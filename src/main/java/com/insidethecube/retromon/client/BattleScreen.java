package com.insidethecube.retromon.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import com.insidethecube.retromon.engine.Event;
import com.insidethecube.retromon.engine.Item;
import com.insidethecube.retromon.engine.Mon;
import com.insidethecube.retromon.engine.Move;
import com.insidethecube.retromon.engine.Species;
import com.insidethecube.retromon.game.MonDisplay;
import com.insidethecube.retromon.game.MonTag;
import com.insidethecube.retromon.game.Net;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.lwjgl.glfw.GLFW;

/**
 * A battle on its stage: their card top right, yours (with its EXP bar) bottom left, the message bar along
 * the bottom, and the commands stacked on the right as the newer games have them: FIGHT, POKéMON, BAG, RUN.
 * The game on the server decides each turn; this plays back what happened (the lines, the HP running down,
 * each move's effect, hits shaking the screen, faints, Poké Balls sent out, thrown and shaking, EXP filling)
 * and then asks what you'll do next. A first catch shows its Pokédex entry. Keys or mouse; Esc is Minecraft's menu.
 */
public class BattleScreen extends GameScreen {
	/** What's on show for one side (it changes as the turn plays back, not all at once). */
	private static final class Side {
		String species = "", name = "", status = "", gender = "";
		int level, max = 1, hpTarget, index = -1;
		float hp, exp, expTarget;
		boolean shown;
	}

	private record Step(int kind, int side, String text, int a, int b) {}

	private static final int SHAKE = -1, POP = -2, END = -3, NOTICE_DONE = -4, INTO_BALL = -5, GROW = -6, DEX = -7;
	/** The way in (as Let's Go): two quick flashes, a slanted sweep of white from left to right, then the white fades on the stage. */
	private static final int WIPE = 36;
	/** The wipe's ticks left when the stage goes up behind the white. */
	private static final int REVEAL = 16;
	/** Ticks since the stage showed (the cinema frame fades in over them). */
	private int onStage;
	/** Aiming a Poké Ball: which ball, and how long the ring's been closing. */
	private String aimBall;
	private int aimTicks;
	/** The battle that's on (it carries on behind Minecraft's menu). */
	public static BattleScreen current;
	/** The commands as they're shown, top to bottom, and what each one is (0 FIGHT, 1 BAG, 2 POKéMON, 3 RUN). */
	private static final int[] ORDER = {0, 2, 1, 3};
	private boolean newCatch;
	private String caughtSpecies;

	private enum Mode {
		PLAY, MAIN, FIGHT, PARTY, BAG, BAG_TARGET, WAIT, AIM
	}

	private final Side[] sides = {new Side(), new Side()};
	private final Deque<Step> queue = new ArrayDeque<>();
	private List<Mon> mine = new ArrayList<>(), theirs = new ArrayList<>();
	private int active;
	private String state = "CHOOSE", kind = "WILD", result;
	private boolean locked, canRun = true, boss;
	private final List<String> after = new ArrayList<>();
	private boolean afterQueued;

	private Mode mode = Mode.PLAY, noticeReturn = Mode.MAIN;
	private String message;
	private int typed, hold, wait, waited;
	private boolean forced;
	private int mainAt, fightAt, partyAt, bagAt;
	private String bagItem, lastBall = "poke_ball";
	private int wipe = WIPE;

	public BattleScreen(CompoundTag start) {
		super("Battle");
		current = this;
		Music.play(start.getStringOr("music", "wild_battle"));
		feed(start);
		for (Mon m : theirs) caughtSpecies = m.species.id();
		newCatch = caughtSpecies != null && !ClientState.data.caught.contains(caughtSpecies);
	}

	/** A turn from the game: what happened, and where things stand. */
	public void feed(CompoundTag t) {
		mine = MonTag.loadAll(t.getListOrEmpty("mine"));
		theirs = MonTag.loadAll(t.getListOrEmpty("theirs"));
		active = t.getIntOr("active", 0);
		state = t.getStringOr("state", "CHOOSE");
		kind = t.getStringOr("kind", "WILD");
		locked = t.getBooleanOr("locked", false);
		canRun = t.getBooleanOr("canRun", true);
		String r = t.getStringOr("result", "");
		if (!r.isEmpty()) {
			result = r;
			boss = t.getBooleanOr("boss", false);
			for (Tag l : t.getListOrEmpty("after")) l.asString().ifPresent(after::add);
		}
		for (Tag e : t.getListOrEmpty("events")) {
			if (!(e instanceof CompoundTag c)) continue;
			String text = c.getStringOr("t", "");
			queue.add(new Step(c.getIntOr("k", 0), c.getIntOr("s", -1), text.isEmpty() ? null : text, c.getIntOr("a", 0), c.getIntOr("b", 0)));
		}
		mode = Mode.PLAY;
		waited = 0;
	}

	// ---------- playing back ----------

	@Override
	public void tick() {
		super.tick();
		if (wipe > 0) {
			wipe--;
			if (wipe == REVEAL) BattleScene.reveal();
			if (wipe > REVEAL) return;
		}
		onStage++;
		if (mode == Mode.WAIT) waited++;
		boolean moving = false;
		for (Side sd : sides) {
			if (Math.abs(sd.hp - sd.hpTarget) > 0.01F) {
				float step = Math.max(0.35F, sd.max / 40F);
				sd.hp = sd.hp < sd.hpTarget ? Math.min(sd.hpTarget, sd.hp + step) : Math.max(sd.hpTarget, sd.hp - step);
				moving = true;
			}
		}
		Side me = sides[0];
		if (me.exp < me.expTarget) {
			Species sp = Species.ALL.get(me.species);
			int next = sp == null || me.level >= 100 ? Integer.MAX_VALUE : sp.growth().expAt(me.level + 1);
			float cap = Math.min(me.expTarget, next);
			if (me.exp < cap) {
				me.exp = Math.min(cap, me.exp + Math.max(1, (next - (sp == null ? 0 : sp.growth().expAt(me.level))) / 40F));
				moving = true;
			}
		}
		if (moving) return;
		if (wait > 0) {
			wait--;
			return;
		}
		if (message != null) {
			typed = Math.min(message.length(), typed + textSpeed());
			if (typed >= message.length() && ++hold > 30 && mode == Mode.PLAY) message = null;
			return;
		}
		if (!queue.isEmpty()) {
			play(queue.poll());
			return;
		}
		if (mode == Mode.PLAY) done();
	}

	private Side side(int s) {
		return sides[s == 1 ? 1 : 0];
	}

	private void play(Step e) {
		MonDisplay d = e.side() >= 0 ? BattleScene.display(e.side()) : null;
		switch (e.kind()) {
			case INTO_BALL -> {
				if (BattleScene.display(1) != null) BattleScene.display(1).sizeTarget = 0;
				BattleScene.dropBall();
				wait = 14;
				return;
			}
			case SHAKE -> {
				Sounds.shake();
				BattleScene.shakeBall();
				wait = 18;
				return;
			}
			case POP -> {
				BattleScene.burstBall();
				Sounds.poof();
				if (BattleScene.display(1) != null) BattleScene.display(1).sizeTarget = 1;
				Sounds.cry(sides[1].species, 1F);
				wait = 12;
				return;
			}
			case GROW -> {
				if (d != null) d.sizeTarget = 1;
				Sounds.cry(side(e.side()).species, 1F);
				side(e.side()).shown = true;
				wait = 14;
				return;
			}
			case DEX -> {
				// A first catch: its Pokédex entry, on the stage, then on to the end.
				if (minecraft != null && caughtSpecies != null) {
					minecraft.setScreen(new DexScreen(caughtSpecies, () -> {
						current = this;
						minecraft.setScreen(this);
					}));
				}
				wait = 4;
				return;
			}
			case END -> {
				close();
				return;
			}
			case NOTICE_DONE -> {
				mode = noticeReturn;
				return;
			}
			default -> {
			}
		}
		Event.Kind[] kinds = Event.Kind.values();
		if (e.kind() < 0 || e.kind() >= kinds.length) return;
		switch (kinds[e.kind()]) {
			case TEXT -> say(e.text());
			case HP -> {
				Side sd = side(e.side());
				sd.hpTarget = e.a();
				sd.max = Math.max(1, e.b());
			}
			case ATTACK -> {
				Move mv = e.text() == null ? null : Move.find(e.text());
				boolean close = mv == null || BattleScene.contact(mv);
				if (d != null) {
					d.dash = close;
					d.lunge = MonDisplay.LUNGE;
				}
				if (mv == null) Sounds.attack();
				BattleScene.move(e.side(), e.text());
				// The hit lands as it arrives (up close), or as the move reaches the target (from afar).
				wait = mv == null ? 8 : mv.power() <= 0 ? 16 : close ? 8 : 19;
			}
			case HIT -> {
				int effect = effectAhead();
				if (d != null) d.flash = 14;
				Sounds.hit(effect);
				BattleScene.hit(e.side(), effect);
				wait = 14;
			}
			case FAINT -> {
				BattleScene.faint(e.side());
				Sounds.cry(side(e.side()).species, 0.7F);
				side(e.side()).shown = false;
				wait = 26;
			}
			case SEND_OUT -> {
				List<Mon> list = e.side() == 0 ? mine : theirs;
				Side sd = side(e.side());
				if (e.a() >= 0 && e.a() < list.size()) {
					Mon m = list.get(e.a());
					sd.species = m.species.id();
					sd.name = m.name();
					sd.gender = m.genderMark();
					sd.level = m.level;
					sd.max = m.maxHp();
					sd.status = m.status.tag;
					if (e.side() == 0 && sd.index != e.a()) sd.exp = sd.expTarget = m.exp;
				}
				sd.index = e.a();
				sd.hp = sd.hpTarget = e.b();
				if (d != null) {
					d.species = sd.species;
					d.size = d.sizeOld = 0;
					d.sizeTarget = 0;
				}
				// Yours (and a trainer's) come out of a ball; a wild one is just there.
				if (e.side() == 0 || !kind.equals("WILD")) {
					BattleScene.sendOut(e.side());
					sd.shown = false;
					queue.addFirst(new Step(GROW, e.side(), null, 0, 0));
					wait = 9;
				} else {
					sd.shown = true;
					if (d != null) d.sizeTarget = 1;
					Sounds.cry(sd.species, 1F);
					wait = 18;
				}
			}
			case RECALL -> {
				if (d != null) d.sizeTarget = 0;
				wait = 14;
			}
			case BALL -> {
				BattleScene.throwBall(lastBall);
				wait = 14;
				Step next = queue.peek();
				boolean caught = next != null && next.kind() == Event.Kind.CAUGHT.ordinal();
				if (!caught) queue.addFirst(new Step(POP, 1, null, 0, 0));
				for (int i = 0; i < e.a(); i++) queue.addFirst(new Step(SHAKE, 1, null, 0, 0));
				queue.addFirst(new Step(INTO_BALL, 1, null, 0, 0));
			}
			case CAUGHT -> {
				// The battle music stops for the catch.
				Music.stop();
				Sounds.caught();
				BattleScene.caughtStars();
				sides[1].shown = false;
				wait = 40;
			}
			case ESCAPE -> {
				Sounds.run();
				wait = 12;
			}
			case EXP -> {
				if (e.b() == sides[0].index) sides[0].expTarget += e.a();
			}
			case LEVEL -> {
				if (e.b() == sides[0].index) sides[0].level = e.a();
				Sounds.levelUp();
				wait = 30;
			}
			case STATUS -> {
				Side sd = side(e.side());
				List<Mon> list = e.side() == 0 ? mine : theirs;
				if (sd.index >= 0 && sd.index < list.size()) sd.status = list.get(sd.index).status.tag;
			}
			case STAT -> {
				Sounds.stat(e.a() > 0);
				BattleScene.stat(e.side() == 1 ? 1 : 0, e.a() > 0);
				wait = 8;
			}
			default -> {
			}
		}
	}

	/** Whether the hit coming up is super effective (1), not very effective (-1) or neither (0): its line follows it. */
	private int effectAhead() {
		int n = 0;
		for (Step st : queue) {
			if (++n > 4) break;
			if (st.text() == null) continue;
			if (st.text().contains("super effective")) return 1;
			if (st.text().contains("not very effective")) return -1;
		}
		return 0;
	}

	private void say(String text) {
		message = text == null ? "" : text;
		typed = 0;
		hold = 0;
	}

	/** The turn's played back: the end of the battle, or your next choice. */
	private void done() {
		if (result != null) {
			if (!afterQueued) {
				afterQueued = true;
				if (result.equals("WIN")) Music.play(kind.equals("WILD") ? "defeated_wild_mon" : boss ? "defeated_gym_leader" : "defeated_trainer");
				if (result.equals("CAUGHT") && newCatch) {
					queue.add(new Step(Event.Kind.TEXT.ordinal(), -1, "New POKéDEX data will be added for " + sides[1].name + "!", 0, 0));
					queue.add(new Step(DEX, -1, null, 0, 0));
				}
				for (String l : after) queue.add(new Step(Event.Kind.TEXT.ordinal(), -1, l, 0, 0));
				queue.add(new Step(END, -1, null, 0, 0));
			}
			return;
		}
		if (state.equals("SWITCH")) {
			forced = true;
			partyAt = firstHealthy();
			mode = Mode.PARTY;
			return;
		}
		// Carrying on with a move that takes more than one turn: no choice to make.
		if (locked) {
			send("MOVE", -1, null);
			return;
		}
		mode = Mode.MAIN;
	}

	private int firstHealthy() {
		for (int i = 0; i < mine.size(); i++) if (!mine.get(i).fainted()) return i;
		return 0;
	}

	private Mon me() {
		return active >= 0 && active < mine.size() ? mine.get(active) : null;
	}

	private void close() {
		current = null;
		BattleScene.end();
		Music.resumePlace();
		if (minecraft != null && minecraft.screen == this) minecraft.setScreen(null);
	}

	// ---------- your choices ----------

	private void send(String act, int i, String item) {
		CompoundTag t = new CompoundTag();
		t.putString("act", act);
		t.putInt("i", i);
		if (item != null) t.putString("item", item);
		ClientPlayNetworking.send(new Net.Act("battle", t));
		mode = Mode.WAIT;
		waited = 0;
		message = null;
	}

	/** A line from the screen itself (no PP left, and so on), then back to the menu it came from. */
	private void notice(String text, Mode back) {
		noticeReturn = back;
		mode = Mode.PLAY;
		say(text);
		queue.addFirst(new Step(NOTICE_DONE, -1, null, 0, 0));
	}

	private void pickMain(int i) {
		mainAt = i;
		switch (i) {
			case 0 -> {
				fightAt = 0;
				mode = Mode.FIGHT;
			}
			case 1 -> {
				bagAt = 0;
				mode = Mode.BAG;
			}
			case 2 -> {
				forced = false;
				partyAt = active;
				mode = Mode.PARTY;
			}
			default -> {
				if (!kind.equals("WILD")) notice("No! There's no running from a trainer battle!", Mode.MAIN);
				else send("RUN", 0, null);
			}
		}
	}

	private void pickMove(int i) {
		Mon m = me();
		if (m == null) return;
		boolean anyPp = m.pp.stream().anyMatch(p -> p > 0);
		if (!anyPp) {
			send("MOVE", -1, null);
			return;
		}
		if (i >= m.moves.size()) return;
		if (m.pp.get(i) <= 0) notice("No PP left for this move!", Mode.FIGHT);
		else send("MOVE", i, null);
	}

	private void pickParty(int i) {
		if (i >= mine.size()) return;
		Mon m = mine.get(i);
		if (mode == Mode.BAG_TARGET) {
			useItemOn(i);
			return;
		}
		if (m.fainted()) notice("There's no will to fight!", Mode.PARTY);
		else if (i == active && !forced) notice(m.name() + " is already out!", Mode.PARTY);
		else send("SWITCH", i, null);
	}

	/** What's in your bag that works in battle: balls first, then medicine and X items. */
	private List<String> battleItems() {
		List<String> out = new ArrayList<>();
		for (String id : ClientState.data.items.keySet()) {
			Item it = Item.find(id);
			if (it != null && it.ball()) out.add(id);
		}
		for (String id : ClientState.data.items.keySet()) {
			Item it = Item.find(id);
			if (it != null && !it.ball() && it.battleUse()) out.add(id);
		}
		return out;
	}

	private void pickItem(int i) {
		List<String> l = battleItems();
		if (i >= l.size()) {
			mode = Mode.MAIN;
			return;
		}
		Item it = Item.find(l.get(i));
		if (it.ball()) {
			if (!kind.equals("WILD")) {
				notice("The trainer blocked the BALL! Don't be a thief!", Mode.BAG);
				return;
			}
			// Aim first: the ring round the wild Pokémon, coloured by how likely it is to stay caught.
			aimBall = it.id();
			aimTicks = 0;
			mode = Mode.AIM;
			return;
		}
		if (it.revives() > 0) {
			bagItem = it.id();
			partyAt = 0;
			mode = Mode.BAG_TARGET;
			return;
		}
		bagItem = it.id();
		useItemOn(active);
	}

	private void useItemOn(int i) {
		Item it = Item.find(bagItem);
		if (it == null || i >= mine.size()) return;
		Mon target = mine.get(i);
		if (it.revives() > 0 ? !target.fainted() : (it.heal() > 0 || it.cures() != null) && it.statBoost() < 0 && !worthIt(it, target)) {
			notice("It won't have any effect.", Mode.BAG);
			return;
		}
		ClientState.data.take(it.id());
		send("ITEM", i, it.id());
	}

	private static boolean worthIt(Item it, Mon m) {
		if (m.fainted()) return false;
		boolean hurt = m.hp < m.maxHp();
		boolean sick = it.cures() != null && m.status != Mon.Status.NONE && (it.cures() == Mon.Status.NONE || it.cures() == m.status);
		return (it.heal() > 0 && hurt) || sick || it.curesConfusion();
	}

	@Override
	protected boolean key(KeyEvent e) {
		if (wipe > REVEAL) return true;
		// If the game ever stops answering, Delete gets you out.
		if (mode == Mode.WAIT && waited > 100 && e.key() == GLFW.GLFW_KEY_DELETE) {
			ClientPlayNetworking.send(new Net.Act("battle_quit", new CompoundTag()));
			close();
			return true;
		}
		switch (mode) {
			case PLAY, WAIT -> {
				if (message != null && Ui.isA(e)) advance();
			}
			case MAIN -> {
				int row = row(mainAt);
				if (Ui.isUp(e) || Ui.isLeft(e)) mainAt = ORDER[Math.floorMod(row - 1, 4)];
				else if (Ui.isDown(e) || Ui.isRight(e)) mainAt = ORDER[Math.floorMod(row + 1, 4)];
				else if (Ui.isA(e)) {
					Sounds.press();
					pickMain(mainAt);
				}
			}
			case FIGHT -> {
				Mon m = me();
				int n = m == null ? 0 : m.moves.size();
				if (n == 0 || Ui.isB(e)) mode = Mode.MAIN;
				else if (Ui.isUp(e) || Ui.isLeft(e)) fightAt = Math.floorMod(fightAt - 1, n);
				else if (Ui.isDown(e) || Ui.isRight(e)) fightAt = Math.floorMod(fightAt + 1, n);
				else if (Ui.isA(e)) {
					Sounds.press();
					pickMove(fightAt);
				}
			}
			case PARTY, BAG_TARGET -> {
				int n = mine.size();
				if (n == 0) return true;
				if (Ui.isB(e) && !forced) mode = mode == Mode.BAG_TARGET ? Mode.BAG : Mode.MAIN;
				else if (Ui.isUp(e)) partyAt = Math.floorMod(partyAt - 2, n);
				else if (Ui.isDown(e)) partyAt = Math.floorMod(partyAt + 2, n);
				else if (Ui.isLeft(e) || Ui.isRight(e)) partyAt = Math.floorMod(partyAt + (Ui.isLeft(e) ? -1 : 1), n);
				else if (Ui.isA(e)) pickParty(partyAt);
			}
			case AIM -> {
				if (Ui.isA(e)) throwBall();
				else if (Ui.isB(e)) mode = Mode.BAG;
			}
			case BAG -> {
				int n = battleItems().size() + 1;
				if (Ui.isB(e)) mode = Mode.MAIN;
				else if (Ui.isUp(e)) bagAt = Math.floorMod(bagAt - 1, n);
				else if (Ui.isDown(e)) bagAt = Math.floorMod(bagAt + 1, n);
				else if (Ui.isA(e)) pickItem(bagAt);
			}
		}
		return true;
	}

	private static int row(int command) {
		for (int i = 0; i < 4; i++) if (ORDER[i] == command) return i;
		return 0;
	}

	private void advance() {
		if (message == null) return;
		Sounds.press();
		if (typed < message.length()) typed = message.length();
		else message = null;
	}

	@Override
	protected void clickAnywhere() {
		if (mode == Mode.PLAY || mode == Mode.WAIT) advance();
		else if (mode == Mode.AIM) throwBall();
	}

	private void throwBall() {
		if (aimBall == null) return;
		Sounds.press();
		lastBall = aimBall;
		ClientState.data.take(aimBall);
		send("BALL", 0, aimBall);
		aimBall = null;
	}

	/** The chance of a catch with this ball now (Gold and Silver's sums), for the ring's colour. */
	private float catchChance() {
		Species sp = Species.ALL.get(sides[1].species);
		if (sp == null) return 0.5F;
		Item ball = Item.find(aimBall);
		float bonus = ball == null ? 1F : (float) Math.max(1, ball.catchBonus());
		if (aimBall != null && aimBall.equals("master_ball")) return 1;
		float max = Math.max(1, sides[1].max), hp = Math.max(1, sides[1].hp);
		float a = Math.max(1, (3 * max - 2 * hp) * sp.catchRate() * bonus / (3 * max));
		if (!sides[1].status.isEmpty()) a += sides[1].status.equals("SLP") || sides[1].status.equals("FRZ") ? 10 : 5;
		return Math.min(1, (a + 1) / 256F);
	}

	@Override
	protected void back() {
		switch (mode) {
			case FIGHT, BAG -> mode = Mode.MAIN;
			case PARTY -> {
				if (!forced) mode = Mode.MAIN;
			}
			case BAG_TARGET, AIM -> mode = Mode.BAG;
			default -> clickAnywhere();
		}
	}

	// ---------- drawing ----------

	/** Streaks running ahead of the white as it sweeps across: height on screen (0 to 1), length, lead. */
	private static final float[][] STREAKS = {{0.08F, 0.5F, 0.1F}, {0.19F, 0.8F, 0.3F}, {0.27F, 0.35F, 0.05F}, {0.36F, 0.9F, 0.42F}, {0.47F, 0.55F, 0.15F},
			{0.55F, 1F, 0.5F}, {0.63F, 0.4F, 0.2F}, {0.72F, 0.75F, 0.35F}, {0.81F, 0.5F, 0.08F}, {0.9F, 0.85F, 0.45F}, {0.96F, 0.3F, 0.25F}};

	/** Fills everything left of a slanted line (leaning forward: the top further on), lead being where it crosses the middle. */
	private void slanted(GuiGraphics g, float lead, int lean, int color) {
		g.pose().pushMatrix();
		g.pose().translate(0, height / 2F);
		g.pose().mul(new org.joml.Matrix3x2f(1, 0, -lean / (float) Math.max(1, height), 1, 0, 0));
		g.fill(-width, -height / 2 - 2, Math.round(lead), height - height / 2 + 2, color);
		g.pose().popMatrix();
	}

	private void drawWipe(GuiGraphics g, float t) {
		int lean = height * 2 / 5;
		if (t < 6) {
			// Two quick flashes, as a wild Pokémon appears.
			float f = (float) Math.abs(Math.sin(t / 6F * Math.PI * 2));
			g.fill(0, 0, width, height, Ui.alpha(0xFFFFFFFF, 0.55F * f));
			return;
		}
		float sweep = Math.min(1, (t - 6) / 12F);
		// Eased: quick through the middle, settling at the end.
		float k = sweep * sweep * (3 - 2 * sweep);
		float lead = -lean + k * (width + 2 * lean + 40 * s);
		// Streaks of light racing ahead of the edge.
		for (int i = 0; i < STREAKS.length; i++) {
			float[] st = STREAKS[i];
			int y = Math.round(height * st[0]);
			float at = lead + (0.5F - st[0]) * lean + st[2] * 70 * s;
			int len = Math.round(st[1] * 90 * s), th = Math.max(1, s + (i % 3 == 0 ? s : 0));
			g.fill(Math.round(at - len), y, Math.round(at), y + th, i % 2 == 0 ? 0xB0FFFFFF : 0x90C8E8FF);
		}
		// A band of pale blue at the edge, then the white.
		slanted(g, lead + 16 * s, lean, 0x70BEE6FF);
		slanted(g, lead + 7 * s, lean, 0xC0E6F6FF);
		slanted(g, lead, lean, 0xFFFFFFFF);
	}

	@Override
	protected void draw(GuiGraphics g, float partial) {
		if (wipe > 0) {
			float t = WIPE - wipe + partial;
			if (wipe > REVEAL) {
				drawWipe(g, t);
				return;
			}
			// The stage is up behind the white: hold, then fade it away.
			float white = t < WIPE - REVEAL + 2 ? 1 : Math.max(0, 1 - (t - (WIPE - REVEAL + 2)) / (REVEAL - 2));
			g.fill(0, 0, width, height, Ui.alpha(0xFFFFFFFF, white));
			if (white > 0.6F) return;
		}
		Ui.cinema(g, width, height, Math.min(1, (onStage + partial) / 14F));
		// A hit's colour glowing round the edges (yellow for a Thunderbolt, orange for fire...); white floods in for the big ones.
		if (BattleScene.flashTicks > 0) {
			float k = Math.max(0, (BattleScene.flashTicks - partial) / BattleScene.flashMax);
			Ui.vignette(g, width, height, BattleScene.flashColor, k);
			if (BattleScene.flashColor == 0xFFFFFF) g.fill(0, 0, width, height, Ui.alpha(0xFFFFFFFF, 0.5F * k));
		}
		drawImpacts(g, partial);
		drawArrows(g, partial);
		int m = 8 * s, bh = 36 * s;
		Side them = sides[1], me = sides[0];
		int by = height - bh - m;
		if (them.shown) foeCard(g, them, width - m - 152 * s, m + height / 12);
		if (me.shown) myCard(g, me, m, by - 46 * s);
		switch (mode) {
			case MAIN -> drawMain(g, m, by, bh);
			case FIGHT -> drawFight(g, m, by, bh);
			case PARTY, BAG_TARGET -> drawParty(g, m, by, bh);
			case BAG -> drawBag(g, m, by, bh);
			case AIM -> drawAim(g, m, by, bh, partial);
			default -> messageBar(g, m, by, width - 2 * m, bh, message == null ? "" : message.substring(0, Math.min(typed, message.length())), true);
		}
	}

	/** Where a side's Pokémon is on the screen (the middle of it), or null. */
	private int[] onScreen(int side, float up) {
		var at = BattleScene.spot(side).add(0, 0.25 + BattleScene.height(side) * up, 0);
		return BattleScene.project(at, width, height);
	}

	/** A bar turned about a point (for the bursts' rays and the arrows). */
	private void ray(GuiGraphics g, float cx, float cy, float angle, float from, float to, float thick, int color) {
		g.pose().pushMatrix();
		g.pose().translate(cx, cy);
		g.pose().rotate(angle);
		g.fill(Math.round(-thick / 2), Math.round(-to), Math.round(thick / 2), Math.round(-from), color);
		g.pose().popMatrix();
	}

	/** Each hit's burst: a white core, rays of light in the move's colour flying out, a ring spreading. */
	private void drawImpacts(GuiGraphics g, float partial) {
		for (BattleScene.Impact im : BattleScene.IMPACTS) {
			int[] c = onScreen(im.side, 0.45F);
			if (c == null) continue;
			float p = Math.min(1, (im.age + partial) / im.life);
			float out = 1 - (1 - p) * (1 - p);
			float big = im.big ? 1.4F : 1F;
			int col = 0xFF000000 | im.color;
			float fade = 1 - p;
			// Core.
			int core = Math.round((10 + 8 * (1 - p)) * s * big);
			Ui.round(g, c[0] - core, c[1] - core, 2 * core, 2 * core, Ui.alpha(0xFFFFFFFF, 0.85F * fade * fade), Math.max(1, core / 2));
			// Rays.
			int n = im.big ? 10 : 8;
			for (int i = 0; i < n; i++) {
				float a = (float) (Math.PI * 2 * i / n + im.side * 0.3 + (i % 2) * 0.12);
				float len = (i % 2 == 0 ? 34 : 24) * s * big;
				ray(g, c[0], c[1], a, out * len * 0.45F, out * len, Math.max(1, (3 - 2 * p) * s), Ui.alpha(i % 2 == 0 ? 0xFFFFFFFF : col, fade));
			}
			// Ring.
			int r = Math.round((12 + 30 * out) * s * big);
			ring(g, c[0], c[1], r, Math.max(1, Math.round(2 * s * fade)), Ui.alpha(col, fade * 0.9F));
		}
	}

	/** Arrows rising over a Pokémon whose stat went up (warm), or falling when it went down (blue). */
	private void drawArrows(GuiGraphics g, float partial) {
		for (BattleScene.Arrows ar : BattleScene.ARROWS) {
			int[] c = onScreen(ar.side, 0.5F);
			if (c == null) continue;
			float p = (ar.age + partial) / BattleScene.Arrows.LIFE;
			float fade = p < 0.2F ? p / 0.2F : p > 0.75F ? (1 - p) / 0.25F : 1;
			int col = Ui.alpha(ar.up ? 0xFFFF7A3A : 0xFF4A8FE0, Math.max(0, fade));
			for (int i = 0; i < 3; i++) {
				for (int j = -1; j <= 1; j += 2) {
					float drift = (p * 28 + i * 9) * s;
					float y = c[1] + (ar.up ? 14 * s - drift : -14 * s + drift);
					float x = c[0] + j * 16 * s;
					// A chevron: two short bars meeting at the point.
					float sz = 6 * s, a = (float) Math.toRadians(ar.up ? 45 : 135);
					ray(g, x, y, a, 0, sz, 2 * s, col);
					ray(g, x, y, -a, 0, sz, 2 * s, col);
				}
			}
		}
	}

	/** A Pokémon's card: name, gender, level, status, HP (and for yours, the numbers and EXP), on a slanted dark plate. */
	private void card(GuiGraphics g, Side sd, int x, int y, int w, boolean mine) {
		int h = mine ? 40 * s : 31 * s, lean = 6 * s;
		int accent = mine ? Ui.BLUE : Ui.RED;
		Ui.slab(g, x + s, y + 2 * s, w, h, 0x50000000, 0x50000000, lean, 0, 0);
		Ui.slab(g, x, y, w, h, 0xF4263252, 0xF4121A2E, lean, 0x50FFFFFF, Math.max(1, s / 2));
		Ui.slab(g, x + 2 * s, y + h - 3 * s, w - 4 * s, 2 * s, accent, Ui.darker(accent, 0.7F), 0, 0, 0);
		int tx = x + 10 * s;
		Ui.heading(g, font, sd.name, tx, y + 5 * s, Ui.TEXT, s);
		int nx = tx + Ui.boldWidth(font, sd.name, s) + 2 * s;
		if (!sd.gender.isEmpty()) Ui.text(g, font, sd.gender, nx, y + 5 * s, sd.gender.contains("♀") ? 0xFFFF8FB3 : 0xFF8FC8FF, s);
		// The level: a small gold Lv and the number.
		String lv = String.valueOf(sd.level);
		int lvx = x + w + lean - 10 * s - Ui.boldWidth(font, lv, s);
		Ui.draw(g, font, Ui.bold(lv), lvx, y + 5 * s, Ui.TEXT, s, true);
		Ui.text(g, font, "Lv", lvx - Ui.width(font, "Lv", s) - s, y + 5 * s, Ui.GOLD, s);
		if (!sd.status.isEmpty()) Ui.chip(g, font, sd.status, lvx - Ui.width(font, "Lv", s) - 8 * s - Ui.width(font, sd.status, s), y + 4 * s,
				Ui.statusColor(sd.status), s);
		float frac = sd.hp / Math.max(1F, sd.max);
		int hy = y + 18 * s, hx = tx + Ui.width(font, "HP", s) + 3 * s;
		Ui.plain(g, font, "HP", tx, hy - s, Ui.GOLD, s);
		Ui.bar(g, hx, hy, x + w - 2 * s - hx, 6 * s, frac, Ui.hpColor(frac), s);
		if (mine) {
			Ui.right(g, font, Math.round(sd.hp) + " / " + sd.max, x + w - 2 * s, y + 26 * s, Ui.TEXT, s);
			Species sp = Species.ALL.get(sd.species);
			if (sp != null) {
				int now = sp.growth().expAt(sd.level), next = sd.level >= 100 ? now + 1 : sp.growth().expAt(sd.level + 1);
				float ef = Math.max(0, Math.min(1, (sd.exp - now) / Math.max(1F, next - now)));
				int ex = tx + Ui.width(font, "EXP", s) + 3 * s, ew = x + w - 60 * s - ex;
				Ui.plain(g, font, "EXP", tx, y + 26 * s, Ui.CYAN, s);
				Ui.bar(g, ex, y + 28 * s, ew, 3 * s, ef, Ui.CYAN, s);
			}
		}
	}

	private void foeCard(GuiGraphics g, Side them, int x, int y) {
		card(g, them, x, y, 140 * s, false);
	}

	private void myCard(GuiGraphics g, Side me, int x, int y) {
		card(g, me, x, y, 140 * s, true);
	}

	/** The message bar along the bottom (the line typing out, the arrow when it's done). */
	private void messageBar(GuiGraphics g, int x, int y, int w, int h, String text, boolean arrow) {
		Ui.round(g, x + s, y + 2 * s, w, h, 0x50000000, 3 * s);
		Ui.round(g, x, y, w, h, 0x60FFFFFF, 3 * s);
		g.fillGradient(x + s, y + s, x + w - s, y + h - s, 0xF4202A44, 0xF40E1424);
		g.fill(x + 3 * s, y + s, x + w - 3 * s, y + 2 * s, 0x30FFFFFF);
		List<String> lines = Ui.wrap(font, text, (w - 24 * s) / s);
		for (int i = 0; i < Math.min(2, lines.size()); i++) Ui.text(g, font, lines.get(i), x + 12 * s, y + 8 * s + i * 12 * s, Ui.TEXT, s);
		if (arrow && message != null && typed >= message.length()) Ui.text(g, font, "▼", x + w - 14 * s, y + h - 13 * s + ((ticks / 6) % 2) * s, Ui.CREAM, s);
	}

	/** A full coloured command or move bar (Let's Go): slanted, lit from the top; the picked one slides out with a white edge. */
	private void bigBar(GuiGraphics g, int x, int y, int w, int h, int color, boolean on, boolean dim) {
		int lean = 7 * s;
		int top = dim ? 0xFF4A5068 : Ui.lighter(color, on ? 0.38F : 0.16F), bottom = dim ? 0xFF2E3346 : Ui.darker(color, on ? 0.92F : 0.7F);
		Ui.slab(g, x + 2 * s, y + 2 * s, w, h, 0x60000000, 0x60000000, lean, 0, 0);
		Ui.slab(g, x, y, w, h, top, bottom, lean, on ? 0xFFFFFFFF : 0x40000000, on ? Math.max(1, s) : Math.max(1, s / 2));
	}

	private void drawMain(GuiGraphics g, int m, int by, int bh) {
		Mon mon = me();
		int cw = 112 * s, ph = 21 * s, gap = 4 * s;
		messageBar(g, m, by, width - 2 * m - cw - 24 * s, bh, "What will " + (mon == null ? "you" : mon.name()) + " do?", false);
		String[] names = {"FIGHT", "BAG", "POKéMON", "RUN"};
		String[] icons = {"icon/fight", "icon/bag", "icon/ball", "icon/run"};
		int[] colors = {0xFFE5525C, 0xFFF0B43C, 0xFF41BF84, 0xFF4A8FE0};
		int x0 = width - m - cw - 7 * s, y0 = by + bh - 4 * ph - 3 * gap;
		for (int r = 0; r < 4; r++) {
			int i = ORDER[r];
			int y = y0 + r * (ph + gap);
			if (over(x0 - 10 * s, y, cw + 17 * s, ph)) mainAt = i;
			boolean on = i == mainAt, dim = i == 3 && !kind.equals("WILD");
			int x = x0 - (on ? 10 * s : 0);
			bigBar(g, x, y, cw, ph, colors[i], on, dim);
			// The icon in a dark round, then the word.
			int ic = ph - 6 * s;
			Ui.round(g, x + 6 * s, y + 3 * s, ic, ic, 0x40000000, Math.max(1, ic / 3));
			Ui.icon(g, icons[i], x + 6 * s + ic / 6, y + 3 * s + ic / 6, ic * 2 / 3, 0xFFFFFFFF);
			Ui.draw(g, font, Ui.bold(names[i]), x + 12 * s + ic, y + (ph - 8 * s) / 2, 0xFFFFFFFF, s, true);
			hit(x, y, cw + 7 * s, ph, () -> pickMain(i));
		}
	}

	/** How a move of this type would do against their Pokémon (Gold and Silver's chart). */
	private String effectiveness(Move mv) {
		Species sp = Species.ALL.get(sides[1].species);
		if (sp == null || mv.power() <= 0) return null;
		double k = mv.type().against(sp.type1(), sp.type2());
		return k == 0 ? "No effect" : k > 1 ? "Super effective" : k < 1 ? "Not very effective" : "Effective";
	}

	private void drawFight(GuiGraphics g, int m, int by, int bh) {
		Mon mon = me();
		if (mon == null) return;
		int cw = 170 * s, ph = 22 * s, gap = 4 * s, n = mon.moves.size();
		int x0 = width - m - cw - 7 * s, y0 = by + bh - 4 * ph - 3 * gap;
		for (int i = 0; i < 4; i++) {
			int y = y0 + i * (ph + gap);
			if (i >= n) {
				Ui.slab(g, x0, y, cw, ph, 0x60202A40, 0x60101626, 7 * s, 0, 0);
				Ui.centred(g, font, "-", x0 + cw / 2, y + (ph - 8 * s) / 2, Ui.DIM, s, false);
				continue;
			}
			Move mv = Move.get(mon.moves.get(i));
			int idx = i;
			if (over(x0 - 12 * s, y, cw + 19 * s, ph)) fightAt = i;
			boolean on = i == fightAt;
			int x = x0 - (on ? 12 * s : 0);
			int col = Ui.typeColor(mv.type()), pp = mon.pp.get(i);
			bigBar(g, x, y, cw, ph, col, on, pp == 0);
			// The type on a white tag, the move's name, its PP.
			String type = mv.type().label.toUpperCase();
			int tw = Math.max(40 * s, Ui.boldWidth(font, type, s) + 10 * s), th = ph - 8 * s;
			Ui.round(g, x + 6 * s, y + 4 * s, tw, th, 0xF0FFFFFF, 2 * s);
			Ui.draw(g, font, Ui.bold(type), x + 6 * s + (tw - Ui.boldWidth(font, type, s)) / 2, y + 4 * s + (th - 8 * s) / 2 + s / 2, Ui.darker(col, 0.8F), s, false);
			Ui.draw(g, font, Ui.bold(mv.name()), x + tw + 12 * s, y + (ph - 8 * s) / 2, 0xFFFFFFFF, s, true);
			String ppText = pp + "/" + mv.pp();
			int ppc = pp == 0 ? 0xFFFF9A9A : pp * 4 <= mv.pp() ? 0xFFFFE08A : 0xFFFFFFFF;
			int px = x + cw + 2 * s - Ui.boldWidth(font, ppText, s);
			Ui.draw(g, font, Ui.bold(ppText), px, y + (ph - 8 * s) / 2, ppc, s, true);
			Ui.draw(g, font, Ui.ui("PP"), px - Ui.width(font, "PP", s) - 3 * s, y + (ph - 8 * s) / 2, 0xD0FFFFFF, s, true);
			hit(x, y, cw + 7 * s, ph, () -> pickMove(idx));
		}
		// What the picked move is, in the bar on the left: its type and kind, power and accuracy, how it'll do.
		int lw = width - 2 * m - cw - 26 * s;
		Move sel = fightAt < n ? Move.get(mon.moves.get(fightAt)) : null;
		messageBar(g, m, by, lw, bh, "", false);
		if (sel != null) {
			String type = sel.type().label.toUpperCase();
			int cx = m + 10 * s;
			cx += Ui.chip(g, font, type, cx, by + 6 * s, Ui.typeColor(sel.type()), s) + 4 * s;
			String cat = sel.power() <= 0 ? "STATUS" : sel.type().special() ? "SPECIAL" : "PHYSICAL";
			cx += Ui.chip(g, font, cat, cx, by + 6 * s, 0xFF4A5578, s) + 6 * s;
			String eff = effectiveness(sel);
			if (eff != null) {
				int ec = eff.equals("Super effective") ? 0xFF5CD65C : eff.equals("No effect") ? 0xFFEF4F4F : eff.equals("Effective") ? Ui.MUTED : 0xFFF2C744;
				Ui.text(g, font, eff, cx, by + 7 * s, ec, s);
			}
			String stats = "POWER " + (sel.power() <= 1 ? "-" : sel.power()) + "    ACCURACY " + (sel.accuracy() <= 0 ? "-" : sel.accuracy());
			Ui.text(g, font, stats, m + 10 * s, by + 21 * s, Ui.MUTED, s);
		}
		int back = 40 * s, bx = m + lw - back - 6 * s;
		Ui.button(g, font, bx, by + 18 * s, back, 13 * s, "BACK", 0xFF5A6070, over(bx, by + 18 * s, back, 13 * s), s);
		hit(bx, by + 18 * s, back, 13 * s, () -> mode = Mode.MAIN);
	}

	private void drawParty(GuiGraphics g, int m, int by, int bh) {
		int cols = width > 300 * s ? 2 : 1, gap = 4 * s;
		int cw = Math.min(170 * s, (width - 2 * m - gap * (cols - 1)) / cols), ch = 42 * s;
		int total = cols * cw + gap * (cols - 1), x0 = (width - total) / 2, y0 = m + height / 12;
		for (int i = 0; i < mine.size(); i++) {
			int x = x0 + (i % cols) * (cw + gap), y = y0 + (i / cols) * (ch + gap);
			int idx = i;
			if (over(x, y, cw, ch)) partyAt = i;
			MenuScreen.card(g, font, mine.get(i), x, y, cw, ch, i == partyAt, i == active, s);
			hit(x, y, cw, ch, () -> pickParty(idx));
		}
		String prompt = mode == Mode.BAG_TARGET ? "Use it on which POKéMON?" : forced ? "Bring out which POKéMON?" : "Choose a POKéMON.";
		messageBar(g, m, by, width - 2 * m - (forced ? 0 : 50 * s), bh, prompt, false);
		if (!forced) {
			int back = 44 * s, bx = width - m - back;
			Ui.button(g, font, bx, by, back, bh, "BACK", 0xFF5A6070, over(bx, by, back, bh), s);
			hit(bx, by, back, bh, this::back);
		}
	}

	/** A ring of thick dots (the GUI has no circles). */
	private void ring(GuiGraphics g, int cx, int cy, int r, int thick, int color) {
		int n = Math.max(24, r / Math.max(1, s));
		for (int i = 0; i < n; i++) {
			double a = Math.PI * 2 * i / n;
			int x = cx + (int) Math.round(Math.cos(a) * r), y = cy + (int) Math.round(Math.sin(a) * r);
			g.fill(x - thick / 2, y - thick / 2, x + (thick + 1) / 2, y + (thick + 1) / 2, color);
		}
	}

	/** Aiming: a white ring round the wild Pokémon and a coloured one closing inside it (green easy, red hard). */
	private void drawAim(GuiGraphics g, int m, int by, int bh, float partial) {
		var at = BattleScene.enemySpot().add(0, BattleScene.enemyHeight() * 0.5, 0);
		int[] c = BattleScene.project(at, width, height);
		if (c != null) {
			int[] top = BattleScene.project(at.add(0, BattleScene.enemyHeight() * 0.6, 0), width, height);
			int r = top == null ? 30 * s : Math.max(18 * s, Math.abs(c[1] - top[1]) + 10 * s);
			float chance = catchChance();
			int col = chance >= 0.5F ? 0xFF5CD65C : chance >= 0.25F ? 0xFFF2C12E : chance >= 0.1F ? 0xFFF07D2A : 0xFFE5525C;
			float phase = ((ticks + partial) % 40) / 40F;
			int inner = (int) (r * (1 - 0.7F * phase));
			ring(g, c[0], c[1], r, 2 * s, 0xE6FFFFFF);
			ring(g, c[0], c[1], inner, 3 * s, col);
			Ui.centred(g, font, chance >= 0.5F ? "Easy" : chance >= 0.25F ? "Fair" : chance >= 0.1F ? "Hard" : "Very hard", c[0], c[1] + r + 6 * s, col, s, true);
		}
		Item it = Item.find(aimBall);
		messageBar(g, m, by, width - 2 * m, bh, "Throw the " + (it == null ? "BALL" : it.name()) + "!   A or click: throw.   B: back.", false);
	}

	private void drawBag(GuiGraphics g, int m, int by, int bh) {
		List<String> l = battleItems();
		int w = Math.min(width - 2 * m, 190 * s), row = 16 * s, h = (l.size() + 1) * row + 8 * s, x = width - w - m, y = Math.max(m, by - h - 4 * s);
		Ui.round(g, x, y, w, h, 0xE6141B2E, 2 * s);
		for (int i = 0; i <= l.size(); i++) {
			int ty = y + 4 * s + i * row;
			int idx = i;
			if (over(x, ty, w, row)) bagAt = i;
			boolean on = i == bagAt;
			Ui.pill(g, x + 3 * s, ty, w - 6 * s, row - 2 * s, on, s);
			int tc = on ? Ui.CREAM_INK : Ui.TEXT;
			if (i == l.size()) Ui.draw(g, font, Ui.bold("CANCEL"), x + 12 * s, ty + 4 * s, on ? Ui.CREAM_INK : Ui.MUTED, s, false);
			else {
				Item it = Item.find(l.get(i));
				String count = "x" + ClientState.data.count(it.id());
				Ui.draw(g, font, Ui.bold(it.name()), x + 12 * s, ty + 4 * s, tc, s, false);
				Ui.draw(g, font, Ui.ui(count), x + w - 12 * s - Ui.width(font, count, s), ty + 4 * s, on ? Ui.CREAM_INK : Ui.MUTED, s, false);
			}
			hit(x, ty, w, row, () -> pickItem(idx));
		}
		messageBar(g, m, by, width - 2 * m, bh, l.isEmpty() ? "There's nothing you can use in battle." : "Use which item?", false);
	}
}
