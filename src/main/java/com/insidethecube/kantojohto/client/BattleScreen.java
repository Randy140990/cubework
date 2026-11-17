package com.insidethecube.kantojohto.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import com.insidethecube.kantojohto.engine.Event;
import com.insidethecube.kantojohto.engine.Mon;
import com.insidethecube.kantojohto.engine.Move;
import com.insidethecube.kantojohto.game.MonDisplay;
import com.insidethecube.kantojohto.game.MonTag;
import com.insidethecube.kantojohto.game.Net;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.lwjgl.glfw.GLFW;

/**
 * A battle as Gold and Silver show it, over the Pokémon standing in the world: their box top left, yours
 * above the text box, the text box, and FIGHT / PKMN / PACK / RUN. The game on the server decides each
 * turn; this plays back what happened (the lines, the HP running down, lunges, flickers, faints, the
 * Poké Ball's shakes) and then asks what you'll do next.
 */
public class BattleScreen extends GameScreen {
	/** What's on show for one side (it changes as the turn plays back, not all at once). */
	private static final class Side {
		String species = "", name = "", status = "";
		int level, max = 1, hpTarget, index;
		float hp;
		boolean shown;
	}

	private record Step(int kind, int side, String text, int a, int b) {}

	private static final int SHAKE = -1, POP = -2, END = -3, NOTICE_DONE = -4;
	private static final int WIPE = 20;

	private enum Mode {
		PLAY, MAIN, FIGHT, PARTY, PACK, WAIT
	}

	private final Side[] sides = {new Side(), new Side()};
	private final Deque<Step> queue = new ArrayDeque<>();
	private List<Mon> mine = new ArrayList<>(), theirs = new ArrayList<>();
	private int active, enemy;
	private String state = "CHOOSE", kind = "WILD", result;
	private final List<String> after = new ArrayList<>();
	private boolean afterQueued;

	private Mode mode = Mode.PLAY, noticeReturn = Mode.MAIN;
	private String message;
	private int typed, hold, wait, waited;
	private boolean forced;
	private int mainAt, fightAt, partyAt, packAt;
	private int wipe = WIPE;

	public BattleScreen(CompoundTag start) {
		super("Battle");
		feed(start);
	}

	/** A turn from the game: what happened, and where things stand. */
	public void feed(CompoundTag t) {
		mine = MonTag.loadAll(t.getListOrEmpty("mine"));
		theirs = MonTag.loadAll(t.getListOrEmpty("theirs"));
		active = t.getIntOr("active", 0);
		enemy = t.getIntOr("enemy", 0);
		state = t.getStringOr("state", "CHOOSE");
		kind = t.getStringOr("kind", "WILD");
		String r = t.getStringOr("result", "");
		if (!r.isEmpty()) {
			result = r;
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
		if (moving) return;
		if (wait > 0) {
			wait--;
			return;
		}
		if (message != null) {
			typed = Math.min(message.length(), typed + 2);
			if (typed >= message.length() && ++hold > 34 && mode == Mode.PLAY) message = null;
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
			case SHAKE -> {
				Sounds.shake();
				if (BattleScene.display(1) != null) BattleScene.display(1).lunge = 4;
				wait = 16;
				return;
			}
			case POP -> {
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
					sd.level = m.level;
					sd.max = m.maxHp();
					sd.status = m.status.tag;
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
				side(e.side()).shown = false;
				wait = 14;
			}
			case BALL -> {
				Sounds.ball();
				if (BattleScene.display(1) != null) BattleScene.display(1).sizeTarget = 0;
				wait = 18;
				Step next = queue.peek();
				boolean caught = next != null && next.kind() == Event.Kind.CAUGHT.ordinal();
				if (!caught) queue.addFirst(new Step(POP, 1, null, 0, 0));
				for (int i = 0; i < e.a(); i++) queue.addFirst(new Step(SHAKE, 1, null, 0, 0));
			}
			case CAUGHT -> {
				Sounds.caught();
				sides[1].shown = false;
				wait = 30;
			}
			case ESCAPE -> {
				Sounds.run();
				wait = 12;
			}
			case LEVEL -> {
				if (e.b() == sides[0].index) sides[0].level = e.a();
				Sounds.levelUp();
				wait = 16;
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
				if (result.equals("WIN")) {
					boolean badge = after.stream().anyMatch(l -> l.contains("received the"));
					if (badge) Sounds.badge();
					else if (kind.equals("TRAINER")) Sounds.win();
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
		mode = Mode.MAIN;
		mainAt = Math.min(mainAt, 3);
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
		if (minecraft != null && minecraft.screen == this) minecraft.setScreen(null);
	}

	// ---------- your choices ----------

	private void send(String act, int i) {
		CompoundTag t = new CompoundTag();
		t.putString("act", act);
		t.putInt("i", i);
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
				if (message != null && Ui.isA(e)) {
					if (typed < message.length()) typed = message.length();
					else message = null;
				}
			}
			case MAIN -> {
				if (Ui.isLeft(e) || Ui.isRight(e)) {
					mainAt ^= 1;
					Sounds.select();
				} else if (Ui.isUp(e) || Ui.isDown(e)) {
					mainAt ^= 2;
					Sounds.select();
				} else if (Ui.isA(e)) {
					Sounds.select();
					switch (mainAt) {
						case 0 -> {
							fightAt = 0;
							mode = Mode.FIGHT;
						}
						case 1 -> {
							forced = false;
							partyAt = active;
							mode = Mode.PARTY;
						}
						case 2 -> {
							packAt = 0;
							mode = Mode.PACK;
						}
						default -> send("RUN", 0);
					}
				}
			}
			case FIGHT -> {
				Mon m = me();
				int n = m == null ? 0 : m.moves.size();
				if (n == 0 || Ui.isB(e)) {
					mode = Mode.MAIN;
					return true;
				}
				if (Ui.isUp(e)) {
					fightAt = Math.floorMod(fightAt - 1, n);
					Sounds.select();
				} else if (Ui.isDown(e)) {
					fightAt = Math.floorMod(fightAt + 1, n);
					Sounds.select();
				} else if (Ui.isA(e)) {
					if (m.pp.get(fightAt) <= 0) notice("No PP left for this move!", Mode.FIGHT);
					else send("MOVE", fightAt);
				}
			}
			case PARTY -> {
				int n = mine.size();
				if (n == 0) return true;
				if (Ui.isB(e) && !forced) mode = Mode.MAIN;
				else if (Ui.isUp(e)) partyAt = Math.floorMod(partyAt - 1, n);
				else if (Ui.isDown(e)) partyAt = Math.floorMod(partyAt + 1, n);
				else if (Ui.isA(e)) {
					Mon m = mine.get(partyAt);
					if (m.fainted()) notice("There's no will to fight!", Mode.PARTY);
					else if (partyAt == active && !forced) notice(m.name() + " is already out!", Mode.PARTY);
					else send("SWITCH", partyAt);
				}
			}
			case PACK -> {
				if (Ui.isB(e)) mode = Mode.MAIN;
				else if (Ui.isUp(e)) packAt = Math.floorMod(packAt - 1, 3);
				else if (Ui.isDown(e)) packAt = Math.floorMod(packAt + 1, 3);
				else if (Ui.isA(e)) {
					if (packAt == 2) mode = Mode.MAIN;
					else if (packAt == 0) {
						Mon m = me();
						if (ClientState.data.potions <= 0) notice("You don't have any POTIONS.", Mode.PACK);
						else if (m != null && m.hp >= m.maxHp()) notice("It won't have any effect.", Mode.PACK);
						else send("POTION", 0);
					} else {
						if (ClientState.data.balls <= 0) notice("You don't have any POKé BALLS.", Mode.PACK);
						else send("BALL", 0);
					}
				}
			}
		}
		return true;
	}

	// ---------- drawing ----------

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
		if (wipe > 0) {
			// Into the battle: bars close in from top and bottom, then open on the field.
			float t = (WIPE - wipe + partial) / (WIPE / 2F);
			float cover = t <= 1 ? t : Math.max(0, 2 - t);
			int bar = (int) (height / 2F * Math.min(1, cover));
			g.fill(0, 0, width, bar, 0xFF000000);
			g.fill(0, height - bar, width, height, 0xFF000000);
			if (wipe > WIPE / 2) return;
		}
		int bh = boxHeight();
		Side them = sides[1], me = sides[0];
		if (them.shown) {
			int w = 124 * s, h = 30 * s, x = 4 * s, y = 4 * s;
			Ui.box(g, x, y, w, h, s);
			Ui.text(g, font, them.name, x + 8 * s, y + 7 * s, Ui.INK, s);
			String lv = (them.status.isEmpty() ? "" : them.status + " ") + ":L" + them.level;
			Ui.text(g, font, lv, x + w - 8 * s - Ui.width(font, lv, s), y + 7 * s, Ui.INK, s);
			Ui.hpBar(g, font, x + 8 * s, y + 17 * s, w - 16 * s, them.hp, them.max, s);
		}
		if (me.shown) {
			int w = 134 * s, h = 40 * s, x = width - w - 4 * s, y = height - bh - h - 4 * s;
			Ui.box(g, x, y, w, h, s);
			Ui.text(g, font, me.name, x + 8 * s, y + 7 * s, Ui.INK, s);
			String lv = (me.status.isEmpty() ? "" : me.status + " ") + ":L" + me.level;
			Ui.text(g, font, lv, x + w - 8 * s - Ui.width(font, lv, s), y + 7 * s, Ui.INK, s);
			Ui.hpBar(g, font, x + 8 * s, y + 17 * s, w - 16 * s, me.hp, me.max, s);
			String nums = Math.round(me.hp) + "/ " + me.max;
			Ui.text(g, font, nums, x + w - 8 * s - Ui.width(font, nums, s), y + 27 * s, Ui.INK, s);
		}
		switch (mode) {
			case MAIN -> renderMain(g, bh);
			case FIGHT -> renderFight(g, bh);
			case PARTY -> renderParty(g, bh);
			case PACK -> {
				textBox(g, 0, width, bh, "Use which item?");
				renderPack(g, bh);
			}
			default -> textBox(g, 0, width, bh, message == null ? "" : message.substring(0, Math.min(typed, message.length())));
		}
	}

	private void textBox(GuiGraphics g, int x, int w, int bh, String text) {
		int y = height - bh;
		Ui.box(g, x, y, w, bh, s);
		List<String> lines = Ui.wrap(font, text, (w - 16 * s) / s);
		for (int i = 0; i < Math.min(2, lines.size()); i++) Ui.text(g, font, lines.get(i), x + 8 * s, y + 7 * s + i * 10 * s, Ui.INK, s);
	}

	private void renderMain(GuiGraphics g, int bh) {
		Mon m = me();
		int mw = 118 * s;
		textBox(g, 0, width - mw, bh, "What will " + (m == null ? "you" : m.name()) + " do?");
		int x = width - mw, y = height - bh;
		Ui.box(g, x, y, mw, bh, s);
		String[] items = {"FIGHT", "PKMN", "PACK", "RUN"};
		for (int i = 0; i < 4; i++) {
			int ix = x + 14 * s + (i % 2) * 54 * s, iy = y + 7 * s + (i / 2) * 11 * s;
			if (i == mainAt) Ui.text(g, font, Ui.CURSOR, ix - 9 * s, iy, Ui.INK, s);
			Ui.text(g, font, items[i], ix, iy, Ui.INK, s);
		}
	}

	private void renderFight(GuiGraphics g, int bh) {
		Mon m = me();
		if (m == null) return;
		int lw = Math.max(150 * s, width / 2), lh = m.moves.size() * 11 * s + 12 * s, lx = width - lw, ly = height - lh;
		Ui.box(g, lx, ly, lw, lh, s);
		for (int i = 0; i < m.moves.size(); i++) {
			int iy = ly + 7 * s + i * 11 * s;
			if (i == fightAt) Ui.text(g, font, Ui.CURSOR, lx + 6 * s, iy, Ui.INK, s);
			Ui.text(g, font, Move.get(m.moves.get(i)).name().toUpperCase(), lx + 15 * s, iy, Ui.INK, s);
		}
		if (fightAt < m.moves.size()) {
			Move mv = Move.get(m.moves.get(fightAt));
			int iw = Math.min(lx, 120 * s), ih = 30 * s, ix = lx - iw, iy = height - ih;
			if (ix >= 0) {
				Ui.box(g, ix, iy, iw, ih, s);
				Ui.text(g, font, "TYPE/ " + mv.type().name(), ix + 8 * s, iy + 7 * s, Ui.INK, s);
				int pp = m.pp.get(fightAt);
				Ui.text(g, font, "PP " + pp + "/" + mv.pp(), ix + 8 * s, iy + 17 * s, pp == 0 ? Ui.HP_RED : Ui.INK, s);
			}
		}
	}

	private void renderParty(GuiGraphics g, int bh) {
		int w = Math.min(width - 8 * s, 220 * s);
		MenuScreen.party(g, font, mine, partyAt, true, (width - w) / 2, 4 * s, w, s);
		textBox(g, 0, width, bh, forced ? "Bring out which POKéMON?" : "Choose a POKéMON.");
	}

	private void renderPack(GuiGraphics g, int bh) {
		String[] items = {"POTION", "POKé BALL", "CANCEL"};
		String[] have = {"x" + ClientState.data.potions, "x" + ClientState.data.balls, ""};
		int w = 130 * s, h = 3 * 12 * s + 12 * s, x = width - w - 2 * s, y = height - bh - h - 2 * s;
		Ui.box(g, x, y, w, h, s);
		for (int i = 0; i < 3; i++) {
			int iy = y + 8 * s + i * 12 * s;
			if (i == packAt) Ui.text(g, font, Ui.CURSOR, x + 6 * s, iy, Ui.INK, s);
			Ui.text(g, font, items[i], x + 15 * s, iy, Ui.INK, s);
			Ui.text(g, font, have[i], x + w - 8 * s - Ui.width(font, have[i], s), iy, Ui.INK, s);
		}
	}
}
