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
	/** The way in: three white flashes, then the bars sweep shut and open on the stage. */
	private static final int WIPE = 34;
	/** The battle that's on (it carries on behind Minecraft's menu). */
	public static BattleScreen current;
	/** The commands as they're shown, top to bottom, and what each one is (0 FIGHT, 1 BAG, 2 POKéMON, 3 RUN). */
	private static final int[] ORDER = {0, 2, 1, 3};
	private boolean newCatch;
	private String caughtSpecies;

	private enum Mode {
		PLAY, MAIN, FIGHT, PARTY, BAG, BAG_TARGET, WAIT
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
			if (wipe == WIPE / 3) BattleScene.reveal();
			if (wipe > WIPE / 3) return;
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
					BattleScene.spotlight(caughtSpecies);
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
				if (d != null && (mv == null || mv.power() > 0)) d.lunge = MonDisplay.LUNGE;
				if (mv == null) Sounds.attack();
				BattleScene.move(e.side(), e.text());
				wait = mv != null && mv.power() > 0 ? 14 : 12;
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
			lastBall = it.id();
			ClientState.data.take(it.id());
			send("BALL", 0, it.id());
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
		if (wipe > WIPE / 3) return true;
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
	}

	@Override
	protected void back() {
		switch (mode) {
			case FIGHT, BAG -> mode = Mode.MAIN;
			case PARTY -> {
				if (!forced) mode = Mode.MAIN;
			}
			case BAG_TARGET -> mode = Mode.BAG;
			default -> clickAnywhere();
		}
	}

	// ---------- drawing ----------

	@Override
	protected void draw(GuiGraphics g, float partial) {
		if (wipe > 0) {
			// Into the battle: three white flashes, then bars sweep shut from top and bottom and open on the stage.
			float t = WIPE - wipe + partial;
			if (t < 12) {
				if (((int) t / 2) % 2 == 0) g.fill(0, 0, width, height, 0xCCFFFFFF);
				return;
			}
			float u = (t - 12) / ((WIPE - 12) / 2F);
			float cover = u <= 1 ? u : Math.max(0, 2 - u);
			int bar = (int) (height / 2F * Math.min(1, cover));
			g.fill(0, 0, width, bar, 0xFF000000);
			g.fill(0, height - bar, width, height, 0xFF000000);
			if (wipe > WIPE / 3) return;
		}
		Ui.cinema(g, width, height, 0.55F);
		int m = 8 * s, bh = 34 * s;
		Side them = sides[1], me = sides[0];
		if (them.shown) foeCard(g, them, width - m - 156 * s, m + height / 14);
		if (me.shown) myCard(g, me, m, height - m - bh - 50 * s);
		int by = height - bh - m;
		switch (mode) {
			case MAIN -> drawMain(g, m, by, bh);
			case FIGHT -> drawFight(g, m, by, bh);
			case PARTY, BAG_TARGET -> drawParty(g, m, by, bh);
			case BAG -> drawBag(g, m, by, bh);
			default -> messageBar(g, m, by, width - 2 * m, bh, message == null ? "" : message.substring(0, Math.min(typed, message.length())), true);
		}
	}

	private void foeCard(GuiGraphics g, Side them, int x, int y) {
		int w = 156 * s, h = 30 * s;
		Ui.round(g, x, y, w, h, 0xE6141B2E, 2 * s);
		Ui.round(g, x, y, 4 * s, h, Ui.RED, s);
		Ui.heading(g, font, them.name, x + 10 * s, y + 5 * s, Ui.TEXT, s);
		Ui.text(g, font, them.gender, x + 12 * s + Ui.boldWidth(font, them.name, s), y + 5 * s, them.gender.contains("♀") ? 0xFFFF8FB3 : 0xFF8FC8FF, s);
		Ui.right(g, font, "Lv. " + them.level, x + w - 8 * s, y + 5 * s, Ui.CREAM, s);
		if (!them.status.isEmpty()) Ui.chip(g, font, them.status, x + w - 14 * s - Ui.width(font, "Lv. " + them.level, s) - Ui.width(font, them.status, s),
				y + 4 * s, Ui.statusColor(them.status), s);
		float frac = them.hp / Math.max(1F, them.max);
		Ui.bar(g, x + 10 * s, y + 18 * s, w - 18 * s, 6 * s, frac, Ui.hpColor(frac), s);
	}

	private void myCard(GuiGraphics g, Side me, int x, int y) {
		int w = 170 * s, h = 44 * s;
		Ui.round(g, x, y, w, h, 0xE6141B2E, 2 * s);
		Ui.round(g, x, y, 4 * s, h, Ui.BLUE, s);
		Ui.heading(g, font, me.name, x + 10 * s, y + 5 * s, Ui.TEXT, s);
		Ui.text(g, font, me.gender, x + 12 * s + Ui.boldWidth(font, me.name, s), y + 5 * s, me.gender.contains("♀") ? 0xFFFF8FB3 : 0xFF8FC8FF, s);
		Ui.right(g, font, "Lv. " + me.level, x + w - 8 * s, y + 5 * s, Ui.CREAM, s);
		if (!me.status.isEmpty()) Ui.chip(g, font, me.status, x + w - 14 * s - Ui.width(font, "Lv. " + me.level, s) - Ui.width(font, me.status, s),
				y + 4 * s, Ui.statusColor(me.status), s);
		float frac = me.hp / Math.max(1F, me.max);
		Ui.bar(g, x + 10 * s, y + 18 * s, w - 18 * s, 6 * s, frac, Ui.hpColor(frac), s);
		Ui.right(g, font, Math.round(me.hp) + " / " + me.max, x + w - 8 * s, y + 26 * s, Ui.TEXT, s);
		Species sp = Species.ALL.get(me.species);
		if (sp != null) {
			int now = sp.growth().expAt(me.level), next = me.level >= 100 ? now + 1 : sp.growth().expAt(me.level + 1);
			float ef = Math.max(0, Math.min(1, (me.exp - now) / Math.max(1F, next - now)));
			Ui.plain(g, font, "EXP", x + 10 * s, y + 34 * s, Ui.CYAN, s);
			int ex = x + 12 * s + Ui.width(font, "EXP", s);
			Ui.bar(g, ex, y + 35 * s, x + w - 8 * s - ex, 4 * s, ef, Ui.CYAN, s);
		}
	}

	/** The message bar along the bottom (the line typing out, the arrow when it's done). */
	private void messageBar(GuiGraphics g, int x, int y, int w, int h, String text, boolean arrow) {
		Ui.round(g, x, y, w, h, 0xEE101726, 2 * s);
		g.fill(x + 3 * s, y + s, x + w - 3 * s, y + 2 * s, 0x22FFFFFF);
		List<String> lines = Ui.wrap(font, text, (w - 24 * s) / s);
		for (int i = 0; i < Math.min(2, lines.size()); i++) Ui.text(g, font, lines.get(i), x + 12 * s, y + 7 * s + i * 12 * s, Ui.TEXT, s);
		if (arrow && message != null && typed >= message.length() && (ticks / 8) % 2 == 0) Ui.text(g, font, "▼", x + w - 14 * s, y + h - 13 * s, Ui.CREAM, s);
	}

	private void drawMain(GuiGraphics g, int m, int by, int bh) {
		Mon mon = me();
		int cw = 104 * s, ph = 17 * s, gap = 3 * s;
		messageBar(g, m, by, width - 2 * m - cw - 6 * s, bh, "What will " + (mon == null ? "you" : mon.name()) + " do?", false);
		String[] names = {"FIGHT", "BAG", "POKéMON", "RUN"};
		int[] colors = {Ui.RED, Ui.AMBER, Ui.GREEN, Ui.BLUE};
		int x = width - m - cw, y0 = by + bh - 4 * ph - 3 * gap;
		for (int r = 0; r < 4; r++) {
			int i = ORDER[r];
			int y = y0 + r * (ph + gap);
			if (over(x, y, cw, ph)) mainAt = i;
			boolean on = i == mainAt, dim = i == 3 && !kind.equals("WILD");
			int col = dim ? 0xFF5A6070 : colors[i];
			Ui.round(g, x, y, cw, ph, on ? Ui.CREAM : 0xE6141B2E, 2 * s);
			Ui.round(g, x + 2 * s, y + 2 * s, ph - 4 * s, ph - 4 * s, col, 2 * s);
			Ui.draw(g, font, Ui.bold(names[i]), x + ph + 4 * s, y + (ph - 8 * s) / 2, on ? Ui.CREAM_INK : Ui.TEXT, s, !on);
			if (on) Ui.text(g, font, Ui.CURSOR, x - 9 * s, y + (ph - 8 * s) / 2, Ui.CREAM, s);
			hit(x, y, cw, ph, () -> pickMain(i));
		}
	}

	private void drawFight(GuiGraphics g, int m, int by, int bh) {
		Mon mon = me();
		if (mon == null) return;
		int cw = 176 * s, ph = 19 * s, gap = 3 * s, n = mon.moves.size();
		int x = width - m - cw, y0 = by + bh - 4 * ph - 3 * gap;
		for (int i = 0; i < 4; i++) {
			int y = y0 + i * (ph + gap);
			if (i >= n) {
				Ui.round(g, x, y, cw, ph, 0x80141B2E, 2 * s);
				Ui.centred(g, font, "-", x + cw / 2, y + (ph - 8 * s) / 2, Ui.DIM, s, false);
				continue;
			}
			Move mv = Move.get(mon.moves.get(i));
			int idx = i;
			if (over(x, y, cw, ph)) fightAt = i;
			boolean on = i == fightAt;
			int col = Ui.typeColor(mv.type()), pp = mon.pp.get(i);
			Ui.round(g, x, y, cw, ph, on ? Ui.CREAM : 0xE6141B2E, 2 * s);
			// The type's colour as a tab with its name, then the move, then its PP.
			int tw = 44 * s;
			Ui.round(g, x + 2 * s, y + 2 * s, tw, ph - 4 * s, col, 2 * s);
			Ui.centred(g, font, mv.type().label.toUpperCase(), x + 2 * s + tw / 2, y + (ph - 8 * s) / 2, 0xFFFFFFFF, s, true);
			Ui.draw(g, font, Ui.bold(mv.name()), x + tw + 8 * s, y + (ph - 8 * s) / 2, on ? Ui.CREAM_INK : Ui.TEXT, s, !on);
			String ppText = pp + "/" + mv.pp();
			Ui.draw(g, font, Ui.ui(ppText), x + cw - 8 * s - Ui.width(font, ppText, s), y + (ph - 8 * s) / 2,
					pp == 0 ? Ui.HP_RED : on ? Ui.CREAM_INK : Ui.MUTED, s, false);
			if (on) Ui.text(g, font, Ui.CURSOR, x - 9 * s, y + (ph - 8 * s) / 2, Ui.CREAM, s);
			hit(x, y, cw, ph, () -> pickMove(idx));
		}
		// What the picked move is, in the bar on the left.
		int lw = width - 2 * m - cw - 6 * s;
		Move sel = fightAt < n ? Move.get(mon.moves.get(fightAt)) : null;
		Ui.round(g, m, by, lw, bh, 0xEE101726, 2 * s);
		if (sel != null) {
			String type = sel.type().label.toUpperCase();
			Ui.chip(g, font, type, m + 10 * s, by + 6 * s, Ui.typeColor(sel.type()), s);
			String cat = sel.power() <= 0 ? "STATUS" : switch (sel.type()) {
				case FIRE, WATER, GRASS, ELECTRIC, PSYCHIC, ICE, DRAGON, DARK -> "SPECIAL";
				default -> "PHYSICAL";
			};
			Ui.chip(g, font, cat, m + 20 * s + Ui.width(font, type, s), by + 6 * s, 0xFF4A5578, s);
			String stats = "POWER " + (sel.power() <= 1 ? "-" : sel.power()) + "    ACCURACY " + (sel.accuracy() <= 0 ? "-" : sel.accuracy());
			Ui.text(g, font, stats, m + 10 * s, by + 20 * s, Ui.MUTED, s);
		}
		int back = 40 * s, bx = m + lw - back - 4 * s;
		Ui.button(g, font, bx, by + 4 * s, back, 14 * s, "BACK", 0xFF5A6070, over(bx, by + 4 * s, back, 14 * s), s);
		hit(bx, by + 4 * s, back, 14 * s, () -> mode = Mode.MAIN);
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
