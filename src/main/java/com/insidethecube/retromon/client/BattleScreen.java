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
 * A battle, over the Pokémon standing in the world: their box top left, yours (with its EXP bar) at the
 * right, the message panel, and four big buttons: FIGHT, BAG, POKéMON, RUN. The game on the server decides
 * each turn; this plays back what happened (the lines, the HP running down, lunges, flickers, faints, the
 * Poké Ball thrown and shaking, EXP filling) and then asks what you'll do next. Keys or mouse.
 */
public class BattleScreen extends GameScreen {
	/** What's on show for one side (it changes as the turn plays back, not all at once). */
	private static final class Side {
		String species = "", name = "", status = "", gender = "";
		int level, max = 1, hpTarget, index;
		float hp, exp, expTarget;
		boolean shown;
	}

	private record Step(int kind, int side, String text, int a, int b) {}

	private static final int SHAKE = -1, POP = -2, END = -3, NOTICE_DONE = -4, INTO_BALL = -5;
	private static final int WIPE = 20;

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
		Music.play(start.getStringOr("music", "wild_battle"));
		feed(start);
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
			if (wipe == WIPE / 2) BattleScene.reveal();
			if (wipe > WIPE / 2) return;
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
				if (BattleScene.display(1) != null) BattleScene.display(1).sizeTarget = 1;
				Sounds.cry(sides[1].species, 1F);
				wait = 12;
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
				if (d != null) d.lunge = MonDisplay.LUNGE;
				Sounds.attack();
				wait = 10;
			}
			case HIT -> {
				if (d != null) d.flash = 14;
				Sounds.hit();
				wait = 14;
			}
			case FAINT -> {
				if (d != null) d.sizeTarget = 0;
				Sounds.cry(side(e.side()).species, 0.7F);
				side(e.side()).shown = false;
				wait = 24;
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
				sd.shown = true;
				if (d != null) {
					d.species = sd.species;
					d.size = d.sizeOld = 0;
					d.sizeTarget = 1;
				}
				Sounds.cry(sd.species, 1F);
				wait = 18;
			}
			case RECALL -> {
				if (d != null) d.sizeTarget = 0;
				wait = 14;
			}
			case BALL -> {
				Sounds.ball();
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
		if (wipe > WIPE / 2) return true;
		// If the game ever stops answering, Escape gets you out.
		if (mode == Mode.WAIT && waited > 100 && e.key() == GLFW.GLFW_KEY_ESCAPE) {
			ClientPlayNetworking.send(new Net.Act("battle_quit", new CompoundTag()));
			close();
			return true;
		}
		switch (mode) {
			case PLAY, WAIT -> {
				if (message != null && Ui.isA(e)) advance();
			}
			case MAIN -> {
				if (Ui.isLeft(e) || Ui.isRight(e)) mainAt ^= 1;
				else if (Ui.isUp(e) || Ui.isDown(e)) mainAt ^= 2;
				else if (Ui.isA(e)) pickMain(mainAt);
			}
			case FIGHT -> {
				Mon m = me();
				int n = m == null ? 0 : m.moves.size();
				if (n == 0 || Ui.isB(e)) mode = Mode.MAIN;
				else if (Ui.isLeft(e) || Ui.isRight(e)) fightAt = Math.min(n - 1, fightAt ^ 1);
				else if (Ui.isUp(e) || Ui.isDown(e)) fightAt = Math.min(n - 1, fightAt ^ 2);
				else if (Ui.isA(e)) pickMove(fightAt);
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

	private void advance() {
		if (message == null) return;
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
			// Into the battle: bars close in from top and bottom, then open on the field.
			float t = (WIPE - wipe + partial) / (WIPE / 2F);
			float cover = t <= 1 ? t : Math.max(0, 2 - t);
			int bar = (int) (height / 2F * Math.min(1, cover));
			g.fill(0, 0, width, bar, 0xFF000000);
			g.fill(0, height - bar, width, height, 0xFF000000);
			if (wipe > WIPE / 2) return;
		}
		int m = 6 * s, bh = Math.max(boxHeight(), 42 * s);
		Side them = sides[1], me = sides[0];
		if (them.shown) {
			int w = 150 * s, h = 32 * s, x = m, y = m;
			Ui.tagged(g, x, y, w, h, s, Ui.RED);
			Ui.text(g, font, them.name + " " + them.gender, x + 9 * s, y + 6 * s, Ui.TEXT, s);
			Ui.right(g, font, "Lv" + them.level, x + w - 8 * s, y + 6 * s, Ui.GOLD, s);
			if (!them.status.isEmpty()) Ui.chip(g, font, them.status, x + w - 8 * s - Ui.width(font, "Lv" + them.level, s) - Ui.width(font, them.status, s) - 12 * s,
					y + 5 * s, Ui.statusColor(them.status), s);
			Ui.hp(g, font, x + 9 * s, y + 18 * s, w - 18 * s, them.hp, them.max, s);
		}
		if (me.shown) {
			int w = 160 * s, h = 44 * s, x = width - w - m, y = height - bh - h - 2 * m;
			Ui.tagged(g, x, y, w, h, s, Ui.BLUE);
			Ui.text(g, font, me.name + " " + me.gender, x + 9 * s, y + 6 * s, Ui.TEXT, s);
			Ui.right(g, font, "Lv" + me.level, x + w - 8 * s, y + 6 * s, Ui.GOLD, s);
			if (!me.status.isEmpty()) Ui.chip(g, font, me.status, x + w - 8 * s - Ui.width(font, "Lv" + me.level, s) - Ui.width(font, me.status, s) - 12 * s,
					y + 5 * s, Ui.statusColor(me.status), s);
			Ui.hp(g, font, x + 9 * s, y + 18 * s, w - 18 * s, me.hp, me.max, s);
			Ui.right(g, font, Math.round(me.hp) + "/" + me.max, x + w - 8 * s, y + 26 * s, Ui.TEXT, s);
			Species sp = Species.ALL.get(me.species);
			if (sp != null) {
				int now = sp.growth().expAt(me.level), next = me.level >= 100 ? now + 1 : sp.growth().expAt(me.level + 1);
				float frac = (me.exp - now) / Math.max(1F, next - now);
				Ui.plain(g, font, "EXP", x + 9 * s, y + 35 * s, Ui.CYAN, s);
				Ui.bar(g, x + 9 * s + Ui.width(font, "EXP", s) + 3 * s, y + 36 * s, w - 26 * s - Ui.width(font, "EXP", s), 4 * s, frac, Ui.CYAN, s);
			}
		}
		int by = height - bh - m;
		switch (mode) {
			case MAIN -> drawMain(g, m, by, bh);
			case FIGHT -> drawFight(g, m, by, bh);
			case PARTY, BAG_TARGET -> drawParty(g, m, by, bh);
			case BAG -> drawBag(g, m, by, bh);
			default -> messagePanel(g, m, by, width - 2 * m, bh, message == null ? "" : message.substring(0, Math.min(typed, message.length())));
		}
	}

	private void messagePanel(GuiGraphics g, int x, int y, int w, int h, String text) {
		Ui.panel(g, x, y, w, h, s);
		List<String> lines = Ui.wrap(font, text, (w - 20 * s) / s);
		for (int i = 0; i < Math.min(2, lines.size()); i++) Ui.text(g, font, lines.get(i), x + 10 * s, y + 9 * s + i * 11 * s, Ui.TEXT, s);
	}

	private void drawMain(GuiGraphics g, int m, int by, int bh) {
		Mon mon = me();
		int cw = Math.min(170 * s, width / 2), mw = width - 2 * m - cw - 4 * s;
		messagePanel(g, m, by, mw, bh, "What will " + (mon == null ? "you" : mon.name()) + " do?");
		String[] items = {"FIGHT", "BAG", "POKéMON", "RUN"};
		int[] colors = {Ui.RED, Ui.AMBER, Ui.GREEN, Ui.BLUE};
		int bw = (cw - 2 * s) / 2, bhh = (bh - 2 * s) / 2, x0 = width - m - cw;
		for (int i = 0; i < 4; i++) {
			int bx = x0 + (i % 2) * (bw + 2 * s), byy = by + (i / 2) * (bhh + 2 * s);
			int idx = i;
			if (over(bx, byy, bw, bhh)) mainAt = i;
			boolean dim = i == 3 && !kind.equals("WILD");
			Ui.button(g, font, bx, byy, bw, bhh, items[i], dim ? 0xFF5A6070 : colors[i], i == mainAt, s);
			hit(bx, byy, bw, bhh, () -> pickMain(idx));
		}
	}

	private void drawFight(GuiGraphics g, int m, int by, int bh) {
		Mon mon = me();
		if (mon == null) return;
		int bw = (width - 2 * m - 50 * s) / 2, bhh = (bh - 2 * s) / 2;
		for (int i = 0; i < 4; i++) {
			int bx = m + (i % 2) * (bw + 2 * s), byy = by + (i / 2) * (bhh + 2 * s);
			if (i >= mon.moves.size()) {
				Ui.round(g, bx, byy, bw, bhh, 0x80141B2E, s);
				Ui.centred(g, font, "-", bx + bw / 2, byy + (bhh - 8 * s) / 2, Ui.DIM, s, true);
				continue;
			}
			Move mv = Move.get(mon.moves.get(i));
			int idx = i;
			if (over(bx, byy, bw, bhh)) fightAt = i;
			boolean on = i == fightAt;
			int col = Ui.typeColor(mv.type());
			Ui.round(g, bx, byy, bw, bhh, on ? 0xFFFFFFFF : Ui.darker(col, 0.5F), s);
			Ui.round(g, bx + s, byy + s, bw - 2 * s, bhh - 2 * s, on ? col : Ui.darker(col, 0.75F), s);
			Ui.text(g, font, mv.name(), bx + 7 * s, byy + 4 * s, Ui.TEXT, s);
			int pp = mon.pp.get(i);
			Ui.plain(g, font, mv.type().label.toUpperCase(), bx + 7 * s, byy + bhh - 11 * s, 0xDDFFFFFF, s);
			Ui.right(g, font, "PP " + pp + "/" + mv.pp(), bx + bw - 7 * s, byy + bhh - 11 * s, pp == 0 ? Ui.HP_RED : Ui.TEXT, s);
			hit(bx, byy, bw, bhh, () -> pickMove(idx));
		}
		int back = 46 * s, bx = width - m - back;
		Ui.button(g, font, bx, by, back, bh, "BACK", 0xFF5A6070, over(bx, by, back, bh), s);
		hit(bx, by, back, bh, () -> mode = Mode.MAIN);
	}

	private void drawParty(GuiGraphics g, int m, int by, int bh) {
		int cols = width > 300 * s ? 2 : 1, gap = 4 * s;
		int cw = Math.min(170 * s, (width - 2 * m - gap * (cols - 1)) / cols), ch = 42 * s;
		int total = cols * cw + gap * (cols - 1), x0 = (width - total) / 2, y0 = m;
		for (int i = 0; i < mine.size(); i++) {
			int x = x0 + (i % cols) * (cw + gap), y = y0 + (i / cols) * (ch + gap);
			int idx = i;
			if (over(x, y, cw, ch)) partyAt = i;
			MenuScreen.card(g, font, mine.get(i), x, y, cw, ch, i == partyAt, i == active, s);
			hit(x, y, cw, ch, () -> pickParty(idx));
		}
		String prompt = mode == Mode.BAG_TARGET ? "Use it on which POKéMON?" : forced ? "Bring out which POKéMON?" : "Choose a POKéMON.";
		messagePanel(g, m, by, width - 2 * m - (forced ? 0 : 50 * s), bh, prompt);
		if (!forced) {
			int back = 46 * s, bx = width - m - back;
			Ui.button(g, font, bx, by, back, bh, "BACK", 0xFF5A6070, over(bx, by, back, bh), s);
			hit(bx, by, back, bh, this::back);
		}
	}

	private void drawBag(GuiGraphics g, int m, int by, int bh) {
		List<String> l = battleItems();
		int w = Math.min(width - 2 * m, 200 * s), row = 14 * s, h = (l.size() + 1) * row + 10 * s, x = width - w - m, y = Math.max(m, by - h - 4 * s);
		Ui.tagged(g, x, y, w, h, s, Ui.AMBER);
		for (int i = 0; i <= l.size(); i++) {
			int ty = y + 6 * s + i * row;
			int idx = i;
			if (over(x, ty - 2 * s, w, row)) bagAt = i;
			if (i == bagAt) Ui.round(g, x + 4 * s, ty - 2 * s, w - 8 * s, row - s, 0x33F0B43C, s);
			if (i == l.size()) Ui.text(g, font, "CANCEL", x + 12 * s, ty + s, Ui.MUTED, s);
			else {
				Item it = Item.find(l.get(i));
				Ui.text(g, font, it.name(), x + 12 * s, ty + s, Ui.TEXT, s);
				Ui.right(g, font, "x" + ClientState.data.count(it.id()), x + w - 10 * s, ty + s, Ui.MUTED, s);
			}
			hit(x, ty - 2 * s, w, row, () -> pickItem(idx));
		}
		messagePanel(g, m, by, width - 2 * m, bh, l.isEmpty() ? "There's nothing you can use in battle." : "Use which item?");
	}
}
