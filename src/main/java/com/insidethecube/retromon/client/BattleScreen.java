package com.insidethecube.retromon.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

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

	private static final int SHAKE = -1, POP = -2, END = -3, NOTICE_DONE = -4, INTO_BALL = -5, GROW = -6, DEX = -7, ABSORB = -8, CLOSE_BALL = -9,
			DROP = -10, CHEER = -11;
	/** The way in (as Let's Go): two quick flashes, a slanted sweep of white from left to right, then the white fades on the stage. */
	private static final int WIPE = 36;
	/** The wipe's ticks left when the stage goes up behind the white. */
	private static final int REVEAL = 16;
	/** Ticks since the stage showed (the cinema frame fades in over them). */
	private int onStage;
	/** Going to the Pokédex (ticks into the fade to white, -1 when not), and coming back from it (white fading). */
	private int toDex = -1, fromDex;
	private static final int DEX_FADE = 10;
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
	/** Your money, shown at the top left once it changes (prize money, Pay Day, blacking out): from, to, and ticks since. */
	private int moneyFrom, moneyTo, moneyAt = -1;
	private static final int MONEY_COUNT = 24;

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
		// Sped up (TAB), the battle plays that much faster too: its steps, text, bars and waits.
		for (int i = 0, n = Math.max(1, Adventure.speed); i < n && minecraft != null && minecraft.screen == this; i++) step();
	}

	private void step() {
		onStage++;
		if (moneyAt >= 0) moneyAt++;
		if (fromDex > 0) fromDex--;
		if (toDex >= 0) {
			wait = Math.max(wait, 2);
			if (++toDex >= DEX_FADE) {
				toDex = -1;
				if (minecraft != null && caughtSpecies != null) {
					minecraft.setScreen(new DexScreen(caughtSpecies, () -> {
						current = this;
						fromDex = DEX_FADE;
						minecraft.setScreen(this);
					}));
				}
			}
		}
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
				// As in the games: the ball bounces up off it, opens, it's drawn in as red light, the ball shuts and drops.
				BattleScene.ballUp();
				wait = 7;
				return;
			}
			case ABSORB -> {
				BattleScene.absorb();
				wait = 14;
				return;
			}
			case CLOSE_BALL -> {
				BattleScene.closeBall();
				wait = 6;
				return;
			}
			case CHEER -> {
				BattleScene.cheer();
				Sounds.cry(sides[0].species, 1.05F);
				wait = 50;
				return;
			}
			case DROP -> {
				BattleScene.dropBall();
				wait = 12;
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
				BattleScene.release();
				if (BattleScene.display(1) != null) BattleScene.display(1).act("cry");
				Sounds.cry(sides[1].species, 1F);
				wait = 12;
				return;
			}
			case GROW -> {
				if (d != null) {
					d.sizeTarget = 1;
					d.act("cry");
				}
				Sounds.cry(side(e.side()).species, 1F);
				side(e.side()).shown = true;
				wait = 14;
				return;
			}
			case DEX -> {
				// A first catch: the screen goes white, the Pokédex opens on its entry, then white again back to the battle.
				if (minecraft != null && caughtSpecies != null) toDex = 0;
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
					if (mv != null && mv.power() <= 0) d.act("cry");
				}
				if (mv == null) Sounds.attack();
				BattleScene.move(e.side(), e.text());
				// The hit lands as it arrives (up close), or as the move reaches the target (from afar).
				wait = mv == null ? 8 : mv.power() <= 0 ? 16 : close ? 8 : 19;
			}
			case HIT -> {
				int effect = effectAhead();
				if (d != null) {
					d.flash = 14;
					d.act("recoil");
				}
				Sounds.hit(effect);
				BattleScene.hit(e.side(), effect);
				wait = 14;
			}
			case FAINT -> {
				BattleScene.faint(e.side());
				Sounds.cry(side(e.side()).species, 0.7F);
				side(e.side()).shown = false;
				wait = 34;
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
					if (d != null) {
						d.sizeTarget = 1;
						d.act("cry");
					}
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
				queue.addFirst(new Step(DROP, 1, null, 0, 0));
				queue.addFirst(new Step(CLOSE_BALL, 1, null, 0, 0));
				queue.addFirst(new Step(ABSORB, 1, null, 0, 0));
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
			case MONEY -> {
				// Counting on from what it showed last (a prize, then Pay Day's coins on top).
				moneyFrom = moneyAt >= 0 ? moneyTo : e.b() - e.a();
				moneyTo = e.b();
				moneyAt = 0;
				if (e.a() > 0) Sounds.sfx("purchase");
				wait = 4;
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
				if (result.equals("WIN")) {
					Music.play(kind.equals("WILD") ? "defeated_wild_mon" : boss ? "defeated_gym_leader" : "defeated_trainer");
					// Yours celebrates: the camera comes round in front of it as it hops for joy.
					if (sides[0].shown) queue.add(new Step(CHEER, 0, null, 0, 0));
				}
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
		ClientPlayNetworking.send(new Net.Act("battle_closed", new CompoundTag()));
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
	/** The bag in battle has two pockets: POKé BALLS (0) and MEDICINE (1). */
	private int bagPocket;

	private List<String> battleItems() {
		List<String> out = new ArrayList<>();
		for (String id : ClientState.data.items.keySet()) {
			Item it = Item.find(id);
			if (it == null || ClientState.data.count(id) <= 0) continue;
			if (bagPocket == 0 ? it.ball() : !it.ball() && it.battleUse()) out.add(id);
		}
		return out;
	}

	private void pocket(int d) {
		bagPocket = Math.floorMod(bagPocket + d, 2);
		bagAt = 0;
		Sounds.press();
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
				else if (Ui.isUp(e) || Ui.isLeft(e)) partyAt = Math.floorMod(partyAt - 1, n);
				else if (Ui.isDown(e) || Ui.isRight(e)) partyAt = Math.floorMod(partyAt + 1, n);
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
				else if (Ui.isLeft(e) || Ui.isRight(e) || e.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_TAB) pocket(Ui.isLeft(e) ? -1 : 1);
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
		// The plates: theirs at the top right, yours down at the bottom left (level with the bottom of the buttons).
		// Out of the way while the bag or your party is up.
		boolean plates = mode != Mode.BAG && mode != Mode.PARTY && mode != Mode.BAG_TARGET;
		int mineH = PLATE_W * s * 160 / 600, foeY = m + 13 * s, myY = height - m - mineH;
		if (them.shown && plates) foeCard(g, them, width - m - PLATE_W * s, foeY);
		if (me.shown && plates) myCard(g, me, m, myY);
		if (moneyAt >= 0) moneyChip(g, m, foeY, partial);
		// What's said runs along the bottom beside your plate.
		barX = me.shown && plates ? m + PLATE_W * s + 8 * s : m;
		switch (mode) {
			case MAIN -> drawMain(g, m, by, bh);
			case FIGHT -> drawFight(g, m, by, bh);
			case PARTY, BAG_TARGET -> drawParty(g, m, by, bh);
			case BAG -> drawBag(g, m, by, bh);
			case AIM -> drawAim(g, m, by, bh, partial);
			default -> messageBar(g, barX, by, width - m - barX, bh, message == null ? "" : message.substring(0, Math.min(typed, message.length())), true);
		}
		if (toDex >= 0 || fromDex > 0) {
			float a = toDex >= 0 ? Math.min(1, (toDex + partial) / DEX_FADE) : Math.max(0, (fromDex - partial) / DEX_FADE);
			g.fill(0, 0, width, height, Ui.alpha(0xFFFFFFFF, a));
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

	private static final net.minecraft.resources.Identifier PLATE_MINE = com.insidethecube.retromon.Retromon.id("textures/gui/battle/plate_mine.png"),
			PLATE_FOE = com.insidethecube.retromon.Retromon.id("textures/gui/battle/plate_foe.png"),
			BALL_FULL = com.insidethecube.retromon.Retromon.id("textures/gui/battle/ball_full.png"),
			BALL_DOWN = com.insidethecube.retromon.Retromon.id("textures/gui/battle/ball_down.png"),
			BALL_EMPTY = com.insidethecube.retromon.Retromon.id("textures/gui/battle/ball_empty.png"),
			POINTER = com.insidethecube.retromon.Retromon.id("textures/gui/battle/pointer.png");
	/** The round command buttons, by command (0 FIGHT, 1 BAG, 2 POKéMON, 3 RUN). */
	private static final net.minecraft.resources.Identifier[] BUTTONS = {com.insidethecube.retromon.Retromon.id("textures/gui/battle/fight.png"),
			com.insidethecube.retromon.Retromon.id("textures/gui/battle/bag.png"), com.insidethecube.retromon.Retromon.id("textures/gui/battle/pokemon.png"),
			com.insidethecube.retromon.Retromon.id("textures/gui/battle/run.png")};
	private static final int PLATE_W = 156;
	/** Where the message bar starts (beside your plate). */
	private int barX;

	/**
	 * A Pokémon's plate (Let's Go): dark see-through teal with a bright mint edge; the name, then its level and
	 * whether it's male (blue) or female (pink) on the right; a thick HP bar with a white rim; for yours the HP
	 * numbers and a thin EXP line. The party's Poké Balls sit above it (grey for one that's fainted).
	 */
	private void card(GuiGraphics g, Side sd, int x, int y, int w, boolean mine) {
		int h = mine ? w * 160 / 600 : w * 128 / 600;
		Ui.pic(g, mine ? PLATE_MINE : PLATE_FOE, x, y, w, h, 600, mine ? 160 : 128, 0xFFFFFFFF);
		int tx = x + 10 * s, ty = y + (mine ? 7 : 6) * s, right = x + w - 13 * s;
		Ui.heading(g, font, sd.name, tx, ty, 0xFFFFFFFF, s);
		// Right: the gender mark (bigger, in its colour), and the level before it.
		if (!sd.gender.isEmpty()) {
			boolean she = sd.gender.contains("\u2640");
			Ui.draw(g, font, Ui.bold(sd.gender), right - Ui.boldWidth(font, sd.gender, s), ty, she ? 0xFFFF7FA8 : 0xFF6FB8FF, s, true);
			right -= Ui.boldWidth(font, sd.gender, s) + 3 * s;
		}
		String lv = String.valueOf(sd.level);
		int lvx = right - Ui.boldWidth(font, lv, s);
		Ui.draw(g, font, Ui.bold(lv), lvx, ty, 0xFFFFFFFF, s, true);
		int lw = Ui.width(font, "Lv.", s);
		Ui.text(g, font, "Lv.", lvx - lw - s, ty, 0xFFBFF5DA, s);
		if (!sd.status.isEmpty()) Ui.chip(g, font, sd.status, lvx - lw - 8 * s - Ui.width(font, sd.status, s), ty - s, Ui.statusColor(sd.status), s);
		// The HP bar: a white rim, a dark track, the colour (green, then yellow, then red), lit along its top.
		float frac = Math.max(0, Math.min(1, sd.hp / Math.max(1F, sd.max)));
		int bx = tx, bw = x + w - 13 * s - tx, bh = 6 * s, byy = y + (mine ? 18 : 17) * s;
		Ui.rr(g, bx - s, byy - s, bw + 2 * s, bh + 2 * s, bh / 2F + s, 0xFFEFFFF6);
		Ui.rr(g, bx, byy, bw, bh, bh / 2F, 0xFF0B1A19);
		float fw = bw * frac;
		if (frac > 0 && fw < 1) fw = 1;
		if (fw > 0) {
			int col = frac > 0.5F ? 0xFF46DB78 : frac > 0.2F ? 0xFFF2C53A : 0xFFEF4A4A;
			Ui.rrv(g, bx, byy, fw, bh, Math.min(bh, fw) / 2F, Ui.lighter(col, 0.3F), Ui.darker(col, 0.8F));
			if (fw > 2 * s) Ui.rr(g, bx + s, byy + 0.6F * s, fw - 2 * s, Math.max(1, bh / 4F), bh / 8F, Ui.alpha(0xFFFFFFFF, 0.35F));
		}
		if (mine) {
			Ui.text(g, font, Math.round(sd.hp) + "/" + sd.max, tx, byy + bh + 3 * s, 0xFFFFFFFF, s);
			Species sp = Species.ALL.get(sd.species);
			if (sp != null) {
				int now = sp.growth().expAt(sd.level), next = sd.level >= 100 ? now + 1 : sp.growth().expAt(sd.level + 1);
				float ef = Math.max(0, Math.min(1, (sd.exp - now) / Math.max(1F, next - now)));
				int ex = x + w / 2, ew = x + w - 13 * s - ex, ey = byy + bh + 6 * s;
				Ui.plain(g, font, "EXP", ex - Ui.width(font, "EXP", s) - 3 * s, byy + bh + 3 * s, 0xFF8FE6FF, s);
				Ui.rr(g, ex, ey, ew, 2 * s, s, 0xA00B1A19);
				if (ef > 0) Ui.rr(g, ex, ey, Math.max(2 * s, ew * ef), 2 * s, s, 0xFF5AD2FF);
			}
		}
	}

	/**
	 * Your money, sliding in at the top left when it changes: the total counting up (or down) to what you have now,
	 * with the change beneath it in green (or red).
	 */
	private void moneyChip(GuiGraphics g, int x0, int y, float partial) {
		float t = moneyAt + partial, in = Math.min(1, t / 6F), k = 1 - (1 - in) * (1 - in);
		float c = Math.min(1, t / MONEY_COUNT);
		int shown = Math.round(moneyFrom + (moneyTo - moneyFrom) * (1 - (1 - c) * (1 - c)));
		int delta = moneyTo - moneyFrom;
		String total = Ui.money(shown), change = (delta >= 0 ? "+" : "-") + Ui.money(Math.abs(delta));
		int w = Math.max(Ui.boldWidth(font, total, s), Ui.width(font, "MONEY", s) + Ui.width(font, change, s) + 8 * s) + 22 * s, h = 30 * s;
		int x = (int) (x0 - (w + x0) * (1 - k));
		Ui.round(g, x + s, y + 2 * s, w, h, 0x50000000, 3 * s);
		Ui.round(g, x, y, w, h, 0xFFFFD45A, 3 * s);
		Ui.round(g, x + s, y + s, w - 2 * s, h - 2 * s, 0xF0123C38, 3 * s);
		Ui.rr(g, x + 5 * s, y + 2 * s, w - 10 * s, s, s / 2F, 0x26FFFFFF);
		// A gold coin with the ₽ on it.
		int cx = x + 6 * s, cy = y + 6 * s, cs = 9 * s;
		Ui.round(g, cx, cy, cs, cs, 0xFFE0A526, 4 * s);
		Ui.round(g, cx + s, cy + s, cs - 2 * s, cs - 2 * s, 0xFFFFD45A, 4 * s);
		Ui.text(g, font, "₽", cx + 2 * s, cy + s, 0xFF8A5A10, s);
		Ui.draw(g, font, Ui.bold(total), x + 18 * s, y + 6 * s, 0xFFFFFFFF, s, true);
		Ui.text(g, font, "MONEY", x + 6 * s, y + 18 * s, 0xFF8FE6FF, s);
		float lift = Math.max(0, 1 - t / 10F) * 4 * s;
		Ui.right(g, font, change, x + w - 7 * s, y + 18 * s + Math.round(lift), delta >= 0 ? 0xFF7CF0A8 : 0xFFFF8A8A, s);
	}

	/** The party's six Poké Balls in a row: one for each Pokémon (grey if it's fainted), empty slots after. */
	private void balls(GuiGraphics g, List<Mon> party, int x, int y, boolean fromRight) {
		int b = 9 * s, gap = 2 * s;
		for (int i = 0; i < 6; i++) {
			net.minecraft.resources.Identifier id = i >= party.size() ? BALL_EMPTY : party.get(i).fainted() ? BALL_DOWN : BALL_FULL;
			int bx = fromRight ? x - (i + 1) * (b + gap) + gap : x + i * (b + gap);
			Ui.pic(g, id, bx, y, b, b, 48, 48, 0xFFFFFFFF);
		}
	}

	private void foeCard(GuiGraphics g, Side them, int x, int y) {
		card(g, them, x, y, PLATE_W * s, false);
		// A trainer's party (a wild one's on its own).
		if (!kind.equals("WILD")) balls(g, theirs, x + PLATE_W * s - 6 * s, y - 11 * s, true);
	}

	private void myCard(GuiGraphics g, Side me, int x, int y) {
		card(g, me, x, y, PLATE_W * s, true);
		balls(g, mine, x + 6 * s, y - 11 * s, false);
	}

	/** The message bar along the bottom (the line typing out, the arrow when it's done). */
	private void messageBar(GuiGraphics g, int x, int y, int w, int h, String text, boolean arrow) {
		float r = 8 * s;
		Ui.rr(g, x + s * 0.6F, y + 2 * s, w, h, r, 0x50000000);
		Ui.rr(g, x, y, w, h, r, 0xFF8FF5C0);
		Ui.rr(g, x + 1.5F * s, y + 1.5F * s, w - 3 * s, h - 3 * s, r - 1.5F * s, 0xF0123C38);
		Ui.rr(g, x + 4 * s, y + 2.5F * s, w - 8 * s, h * 0.4F, r - 3 * s, 0x141D5A52);
		Ui.rr(g, x + 8 * s, y + 2.5F * s, w - 16 * s, Math.max(1, s * 0.6F), 1, 0x30FFFFFF);
		List<String> lines = Ui.wrap(font, text, (w - 24 * s) / s);
		for (int i = 0; i < Math.min(2, lines.size()); i++) Ui.text(g, font, lines.get(i), x + 12 * s, y + 8 * s + i * 12 * s, Ui.TEXT, s);
		if (arrow && message != null && typed >= message.length()) Ui.triangle(g, x + w - 12 * s, y + h - 10 * s + ((ticks / 6) % 2) * s, 7 * s, 2, Ui.CREAM);
	}

	/** A full coloured command or move bar (Let's Go): slanted, lit from the top; the picked one slides out with a white edge. */
	private void bigBar(GuiGraphics g, int x, int y, int w, int h, int color, boolean on, boolean dim) {
		int lean = 7 * s;
		int top = dim ? 0xFF4A5068 : Ui.lighter(color, on ? 0.38F : 0.16F), bottom = dim ? 0xFF2E3346 : Ui.darker(color, on ? 0.92F : 0.7F);
		Ui.slab(g, x + 2 * s, y + 2 * s, w, h, 0x60000000, 0x60000000, lean, 0, 0);
		Ui.slab(g, x, y, w, h, top, bottom, lean, on ? 0xFFFFFFFF : 0x40000000, on ? Math.max(1, s) : Math.max(1, s / 2));
	}

	/**
	 * What to do (Let's Go): big round buttons along the bottom right, FIGHT, POKéMON and BAG, rising a little to
	 * the right, with RUN smaller above BAG; the one picked grows, with the white pointer bobbing beside it. The
	 * question sits in a pill beside RUN, clear of them.
	 */
	private void drawMain(GuiGraphics g, int m, int by, int bh) {
		Mon mon = me();
		int d = 52 * s, gap = 7 * s;
		int[] row = {0, 2, 1};
		int[][] at = new int[4][];
		// FIGHT, POKéMON, BAG left to right, each clear of the next (with room to grow when picked), stepping up a little.
		for (int k = 0; k < 3; k++) at[row[k]] = new int[] {width - m - d / 2 - 3 * s - (2 - k) * (d + gap), height - m - d / 2 - 3 * s - k * d / 9, d};
		int rd = d * 3 / 5;
		at[3] = new int[] {at[1][0] + d / 2 - rd / 2, at[1][1] - d / 2 - rd / 2 - 9 * s, rd};
		for (int i = 0; i < 4; i++) if (dist(mx, my, at[i][0], at[i][1]) <= at[i][2] / 2F) mainAt = i;
		// The question, in a pill beside RUN (above FIGHT and POKéMON).
		String q = "What will " + (mon == null ? "you" : mon.name()) + " do?";
		int qh = 16 * s, qw = Ui.width(font, q, s) + 18 * s, qx = at[3][0] - rd / 2 - 6 * s - qw, qy = at[3][1] - qh / 2;
		Ui.rr(g, qx + s * 0.6F, qy + 1.5F * s, qw, qh, qh / 2F, 0x50000000);
		Ui.rr(g, qx, qy, qw, qh, qh / 2F, 0xFF8FF5C0);
		Ui.rr(g, qx + 1.5F * s, qy + 1.5F * s, qw - 3 * s, qh - 3 * s, qh / 2F - 1.5F * s, 0xF0123C38);
		Ui.text(g, font, q, qx + 9 * s, qy + 4 * s, 0xFFFFFFFF, s);
		String[] names = {"FIGHT", "BAG", "POKéMON", "RUN"};
		// The others first, the picked one last (on top).
		for (int pass = 0; pass < 2; pass++) {
			for (int i = 0; i < 4; i++) {
				boolean on = i == mainAt;
				if (on != (pass == 1)) continue;
				boolean dim = i == 3 && !kind.equals("WILD");
				float grow = on ? 1.12F + 0.02F * (float) Math.sin(ticks * 0.3) : 1F;
				int size = Math.round(at[i][2] * grow), cx = at[i][0], cy = at[i][1];
				Ui.pic(g, BUTTONS[i], cx - size / 2, cy - size / 2, size, size, 256, 256, dim ? 0xFF8A8F99 : on ? 0xFFFFFFFF : 0xFFE4E4E4);
				int ts = i == 3 ? Math.max(1, s) : s;
				int tw = Ui.boldWidth(font, names[i], ts);
				Ui.draw(g, font, Ui.bold(names[i]), cx - tw / 2, cy - 4 * ts, dim ? 0xFFD0D0D0 : 0xFFFFFFFF, ts, true);
				int idx = i;
				hit(cx - size / 2, cy - size / 2, size, size, () -> pickMain(idx));
				if (on) {
					int ps = 13 * s, bob = (int) (Math.sin(ticks * 0.35) * 2 * s);
					Ui.pic(g, POINTER, cx - size / 2 - ps - 2 * s + bob, cy - ps / 2, ps, ps, 64, 64, 0xFFFFFFFF);
				}
			}
		}
	}

	private static float dist(double ax, double ay, int bx, int by) {
		return (float) Math.hypot(ax - bx, ay - by);
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
			Ui.rr(g, x + 6 * s, y + 4 * s, tw, th, th / 2F, 0xF0FFFFFF);
			Ui.draw(g, font, Ui.bold(type), x + 6 * s + (tw - Ui.boldWidth(font, type, s)) / 2, y + 4 * s + (th - 8 * s) / 2 + s / 2, Ui.darker(col, 0.8F), s, false);
			Ui.draw(g, font, Ui.bold(mv.name()), x + tw + 12 * s, y + (ph - 8 * s) / 2, 0xFFFFFFFF, s, true);
			String ppText = pp + "/" + mv.pp();
			int ppc = pp == 0 ? 0xFFFF9A9A : pp * 4 <= mv.pp() ? 0xFFFFE08A : 0xFFFFFFFF;
			int px = x + cw + 2 * s - Ui.boldWidth(font, ppText, s);
			Ui.draw(g, font, Ui.bold(ppText), px, y + (ph - 8 * s) / 2, ppc, s, true);
			Ui.draw(g, font, Ui.ui("PP"), px - Ui.width(font, "PP", s) - 3 * s, y + (ph - 8 * s) / 2, 0xD0FFFFFF, s, true);
			hit(x, y, cw + 7 * s, ph, () -> pickMove(idx));
		}
		// What the picked move is, in a panel above the moves: its type and kind, how it'll do, power and accuracy.
		int ih = 36 * s, ix = x0 - 12 * s, iw = cw + 19 * s, iy = y0 - ih - 7 * s;
		Move sel = fightAt < n ? Move.get(mon.moves.get(fightAt)) : null;
		messageBar(g, ix, iy, iw, ih, "", false);
		if (sel != null) {
			String type = sel.type().label.toUpperCase();
			int cx = ix + 9 * s;
			cx += Ui.chip(g, font, type, cx, iy + 6 * s, Ui.typeColor(sel.type()), s) + 4 * s;
			String cat = sel.power() <= 0 ? "STATUS" : sel.type().special() ? "SPECIAL" : "PHYSICAL";
			cx += Ui.chip(g, font, cat, cx, iy + 6 * s, 0xFF4A5578, s) + 6 * s;
			String eff = effectiveness(sel);
			if (eff != null) {
				int ec = eff.equals("Super effective") ? 0xFF5CD65C : eff.equals("No effect") ? 0xFFEF4F4F : eff.equals("Effective") ? Ui.MUTED : 0xFFF2C744;
				Ui.text(g, font, eff, cx, iy + 7 * s, ec, s);
			}
			String stats = "POWER " + (sel.power() <= 1 ? "-" : sel.power()) + "   ACCURACY " + (sel.accuracy() <= 0 ? "-" : sel.accuracy());
			Ui.text(g, font, stats, ix + 9 * s, iy + 21 * s, Ui.MUTED, s);
		}
		int back = 40 * s, bx = ix + iw - back - 6 * s, bby = iy + ih - 17 * s;
		Ui.button(g, font, bx, bby, back, 13 * s, "BACK", 0xFF5A6070, over(bx, bby, back, 13 * s), s);
		hit(bx, bby, back, 13 * s, () -> mode = Mode.MAIN);
	}

	/**
	 * Your Pokémon in battle (Let's Go): a column of slanted plates (picture, name, level, HP, status; the one in
	 * battle marked), the picked one sliding out, and on the right that one in 3D on a stand with its types and moves.
	 */
	private void drawParty(GuiGraphics g, int m, int by, int bh) {
		int top = m + height / 14, bottom = by - 6 * s;
		int n = mine.size(), gap = 3 * s, ph = Math.min(30 * s, (bottom - top - gap * 5) / 6), pw = Math.min(176 * s, (width - 2 * m) / 2);
		partyAt = Math.max(0, Math.min(partyAt, Math.max(0, n - 1)));
		for (int i = 0; i < n; i++) {
			Mon mon = mine.get(i);
			int y = top + i * (ph + gap);
			int idx = i;
			if (over(m, y, pw + 10 * s, ph)) partyAt = i;
			boolean on = i == partyAt, out = i == active, down = mon.fainted();
			int x = m + (on ? 10 * s : 0);
			int c1 = down ? 0xF04A2430 : on ? 0xFFFFF4D8 : 0xF0263252, c2 = down ? 0xF02A141C : on ? 0xFFF0DFB4 : 0xF0121A2E;
			Ui.slab(g, x + s, y + 2 * s, pw, ph, 0x50000000, 0x50000000, 6 * s, 0, 0);
			Ui.slab(g, x, y, pw, ph, c1, c2, 6 * s, on ? 0xFFFFFFFF : 0x40FFFFFF, on ? s : Math.max(1, s / 2));
			Ui.portrait(g, mon.species.id(), x + 5 * s, y + s, ph - 2 * s);
			int tx = x + ph + 6 * s, ink = on ? Ui.CREAM_INK : Ui.TEXT;
			Ui.draw(g, font, Ui.bold(mon.name()), tx, y + 4 * s, ink, s, false);
			String lv = "Lv" + mon.level;
			Ui.draw(g, font, Ui.ui(lv), x + pw - 6 * s - Ui.width(font, lv, s), y + 4 * s, on ? 0xFF7A6A40 : Ui.GOLD, s, false);
			float frac = mon.hp / Math.max(1F, mon.maxHp());
			int hx = tx, hw = pw - (tx - x) - 54 * s;
			Ui.bar(g, hx, y + ph - 10 * s, hw, 4 * s, frac, Ui.hpColor(frac), s);
			String hp = mon.hp + "/" + mon.maxHp();
			Ui.draw(g, font, Ui.ui(hp), x + pw - 6 * s - Ui.width(font, hp, s), y + ph - 12 * s, on ? 0xFF4A5068 : Ui.MUTED, s, false);
			if (!mon.status.tag.isEmpty()) Ui.chip(g, font, mon.status.tag, tx + Ui.boldWidth(font, mon.name(), s) + 4 * s, y + 3 * s, Ui.statusColor(mon.status.tag), s);
			if (out) Ui.chip(g, font, "IN BATTLE", x + pw - 54 * s, y + 3 * s, 0xFF4A8FE0, s);
			hit(m, y, pw + 10 * s, ph, () -> pickParty(idx));
		}
		// The picked one, in 3D on a stand, with its types and moves.
		if (partyAt < n) {
			Mon mon = mine.get(partyAt);
			int rx = m + pw + 22 * s, rw = width - m - rx, cx = rx + rw / 2;
			int stand = Math.min(bottom - top - 60 * s, rw * 3 / 5), feet = top + 14 * s + stand;
			Ui.glow(g, cx, feet - stand / 2, rw / 2, stand / 2 + 6 * s, 0x0CFFFFFF);
			Ui.round(g, cx - rw / 3, feet - 5 * s, rw * 2 / 3, 10 * s, 0x90263252, 5 * s);
			Ui.round(g, cx - rw / 4, feet - 3 * s, rw / 2, 6 * s, 0x60FFFFFF, 3 * s);
			Figures.Batch b = Figures.batch(rx, top, width - m, bottom);
			b.mon(mon.species.id(), cx, feet, stand * 0.8F).at((ticks) / 20F).pitch(8).facing(-20).spin((float) Math.sin(ticks / 30.0) * 12);
			b.draw(g);
			int ty = feet + 10 * s, chx = rx + 6 * s;
			chx += Ui.chip(g, font, mon.species.type1().label.toUpperCase(), chx, ty, Ui.typeColor(mon.species.type1()), s) + 3 * s;
			if (mon.species.type2() != null && mon.species.type2() != mon.species.type1())
				Ui.chip(g, font, mon.species.type2().label.toUpperCase(), chx, ty, Ui.typeColor(mon.species.type2()), s);
			int mw = (rw - 16 * s) / 2;
			for (int i = 0; i < mon.moves.size(); i++) {
				Move mv = Move.get(mon.moves.get(i));
				int mx2 = rx + 6 * s + (i % 2) * (mw + 4 * s), my2 = ty + 14 * s + (i / 2) * 14 * s;
				if (my2 + 12 * s > bottom) break;
				int col = Ui.typeColor(mv.type());
				Ui.slab(g, mx2, my2, mw - 4 * s, 12 * s, Ui.lighter(col, 0.1F), Ui.darker(col, 0.75F), 4 * s, 0, 0);
				Ui.draw(g, font, Ui.ui(mv.name()), mx2 + 6 * s, my2 + 2 * s, 0xFFFFFFFF, s, true);
			}
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
		messageBar(g, barX, by, width - m - barX, bh, "Throw the " + (it == null ? "BALL" : it.name()) + "!   A or click: throw.   B: back.", false);
	}

	private static Map<String, String> itemText;

	/** What an item does, in the games' words. */
	static String describe(String id) {
		if (itemText == null) {
			itemText = new java.util.HashMap<>();
			for (String[] r : Species.table("item_desc.tsv")) if (r.length > 1 && !r[0].startsWith("#")) itemText.put(r[0].trim(), r[1]);
		}
		return itemText.getOrDefault(id, "");
	}

	/** A little picture of an item: a ball in its colours, a bottle for medicine, a gem for the rest. */
	static void itemIcon(GuiGraphics g, Item it, int x, int y, int size, int s) {
		if (it.ball()) {
			int top = switch (it.id()) {
				case "great_ball" -> 0xFF3F7FE0;
				case "ultra_ball" -> 0xFF2A2A2E;
				case "master_ball" -> 0xFF8A4AC8;
				case "safari_ball" -> 0xFF6A9A40;
				default -> 0xFFE5404A;
			};
			Ui.round(g, x, y, size, size, 0xFFF4F4F4, size / 2);
			Ui.round(g, x, y, size, size / 2 + 1, top, size / 2);
			g.fill(x + 1, y + size / 2 - Math.max(1, s / 2), x + size - 1, y + size / 2 + Math.max(1, s / 2), 0xFF1E1E22);
			if (it.id().equals("ultra_ball")) {
				g.fill(x + size / 4, y + 1, x + size / 4 + Math.max(1, s), y + size / 2 - 1, 0xFFF2C230);
				g.fill(x + size * 3 / 4 - Math.max(1, s), y + 1, x + size * 3 / 4, y + size / 2 - 1, 0xFFF2C230);
			}
			Ui.round(g, x + size / 2 - size / 6, y + size / 2 - size / 6, size / 3, size / 3, 0xFFFFFFFF, Math.max(1, size / 6));
			return;
		}
		boolean heal = it.id().contains("potion") || it.id().contains("restore") || it.id().contains("revive") || it.id().contains("water")
				|| it.id().contains("lemonade") || it.id().contains("soda") || it.id().contains("berry") || it.id().contains("heal") || it.id().contains("antidote")
				|| it.id().contains("awakening");
		if (heal) {
			int col = it.id().contains("revive") ? 0xFFF2C230 : it.id().contains("hyper") ? 0xFFE5525C : it.id().contains("super") ? 0xFFF07DB0
					: it.id().contains("max") || it.id().contains("restore") ? 0xFF4A8FE0 : it.id().contains("potion") ? 0xFFB070E0 : 0xFF5CC88A;
			Ui.round(g, x + size / 3, y, size / 3, size / 4, 0xFFDADAE2, Math.max(1, s / 2));
			Ui.round(g, x + size / 6, y + size / 5, size * 2 / 3, size * 4 / 5, col, size / 4);
			Ui.round(g, x + size / 6 + s, y + size / 5 + s, size / 5, size / 3, 0x60FFFFFF, Math.max(1, s / 2));
			return;
		}
		Ui.round(g, x + size / 6, y + size / 6, size * 2 / 3, size * 2 / 3, 0xFF7AD0E0, size / 4);
	}

	/**
	 * The bag in battle: a card over the stage (dimmed behind it), its two pockets as big tabs down the left (POKé
	 * BALLS and MEDICINE, with how many kinds are in each), what's in the picked pocket on the right with how many
	 * you have, what the picked one does underneath, and BACK. An empty pocket says so (and where to get some).
	 */
	private void drawBag(GuiGraphics g, int m, int by, int bh) {
		List<String> l = battleItems();
		bagAt = Math.min(bagAt, l.size());
		g.fill(0, 0, width, height, 0x80060A10);
		int w = Math.min(width - 2 * m, 380 * s), h = Math.min(height - 2 * m, 220 * s), x = (width - w) / 2, y = (height - h) / 2;
		Ui.rr(g, x + s, y + 3 * s, w, h, 10 * s, 0x60000000);
		Ui.rr(g, x, y, w, h, 10 * s, 0xFF8FF5C0);
		Ui.rr(g, x + 1.5F * s, y + 1.5F * s, w - 3 * s, h - 3 * s, 8.5F * s, 0xF8123C38);
		Ui.draw(g, font, Ui.bold("BAG"), x + 12 * s, y + 9 * s, 0xFFFFFFFF, s, true);
		// The pockets, down the left.
		String[] pockets = {"POKé BALLS", "MEDICINE"};
		int[] pc = {0xFFE5525C, 0xFF41BF84};
		int tw = Math.min(110 * s, w / 3), th = 34 * s, tx = x + 10 * s;
		for (int i = 0; i < 2; i++) {
			int ty = y + 26 * s + i * (th + 6 * s);
			boolean on = bagPocket == i;
			int kinds = count(i);
			Ui.rr(g, tx, ty, tw, th, th / 2F, on ? 0xFFFFFFFF : 0x30FFFFFF);
			Ui.rr(g, tx + 1.5F * s, ty + 1.5F * s, tw - 3 * s, th - 3 * s, th / 2F - 1.5F * s, on ? pc[i] : Ui.darker(pc[i], 0.45F));
			// A ball (or a bottle) in a white round, then the pocket's name and how many kinds.
			int ic = th - 10 * s;
			Ui.rr(g, tx + 5 * s, ty + 5 * s, ic, ic, ic / 2F, 0xF0FFFFFF);
			itemIcon(g, Item.find(i == 0 ? "poke_ball" : "potion"), tx + 5 * s + ic / 6, ty + 5 * s + ic / 6, ic * 2 / 3, s);
			Ui.draw(g, font, Ui.bold(pockets[i]), tx + ic + 11 * s, ty + 7 * s, 0xFFFFFFFF, s, true);
			Ui.draw(g, font, Ui.ui(kinds == 0 ? "Empty" : kinds + (kinds == 1 ? " kind" : " kinds")), tx + ic + 11 * s, ty + 18 * s, 0xD0FFFFFF, s, false);
			int idx = i;
			hit(tx, ty, tw, th, () -> {
				if (bagPocket != idx) pocket(idx - bagPocket);
			});
		}
		// What's in it.
		int lx = tx + tw + 10 * s, lw = x + w - 10 * s - lx, ly = y + 26 * s, descH = 34 * s, row = 18 * s;
		int rows = Math.max(1, (y + h - 10 * s - descH - 6 * s - ly) / row);
		int n = l.size() + 1;
		int first = Math.max(0, Math.min(bagAt - rows / 2, n - rows));
		for (int k = 0; k < rows && first + k < n; k++) {
			int i = first + k, ry = ly + k * row;
			int idx = i;
			if (over(lx, ry, lw, row - 3 * s)) bagAt = i;
			boolean on = i == bagAt;
			Ui.pill(g, lx, ry, lw, row - 3 * s, on, s);
			int ink = on ? Ui.CREAM_INK : Ui.TEXT;
			if (i == l.size()) Ui.draw(g, font, Ui.bold("BACK"), lx + 12 * s, ry + 4 * s, on ? Ui.CREAM_INK : Ui.MUTED, s, false);
			else {
				Item it = Item.find(l.get(i));
				itemIcon(g, it, lx + 6 * s, ry + 2 * s, row - 7 * s, s);
				Ui.draw(g, font, Ui.bold(it.name()), lx + row + 6 * s, ry + 4 * s, ink, s, false);
				String count = "×" + ClientState.data.count(it.id());
				Ui.draw(g, font, Ui.bold(count), lx + lw - 10 * s - Ui.boldWidth(font, count, s), ry + 4 * s, on ? Ui.CREAM_INK : Ui.GOLD, s, false);
			}
			hit(lx, ry, lw, row - 3 * s, () -> pickItem(idx));
		}
		if (l.isEmpty()) {
			String none = bagPocket == 0 ? "No POKé BALLS yet. A POKé MART sells them." : "No medicine to use in battle.";
			Ui.text(g, font, none, lx + 4 * s, ly + row + 4 * s, Ui.MUTED, s);
		}
		// What the picked one does.
		int dy = y + h - 10 * s - descH;
		Ui.rr(g, lx, dy, lw, descH, 6 * s, 0xF0FBF5E6);
		String desc = bagAt < l.size() ? describe(l.get(bagAt)) : "Close the bag.";
		List<String> lines = Ui.wrap(font, desc, (lw - 16 * s) / s);
		for (int i = 0; i < Math.min(2, lines.size()); i++) Ui.plain(g, font, lines.get(i), lx + 8 * s, dy + 6 * s + i * 11 * s, Ui.CREAM_INK, s);
		Ui.text(g, font, "Up/down: item   Left/right: pocket   A: use   B: back", x + 12 * s, y + h - 12 * s, 0x90FFFFFF, s);
	}

	/** How many kinds of thing are in a pocket. */
	private int count(int pocket) {
		int keep = bagPocket;
		bagPocket = pocket;
		int n = battleItems().size();
		bagPocket = keep;
		return n;
	}

}
