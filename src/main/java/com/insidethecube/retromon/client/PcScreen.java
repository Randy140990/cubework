package com.insidethecube.retromon.client;

import java.util.List;
import java.util.function.IntConsumer;

import com.insidethecube.retromon.engine.Mon;
import com.insidethecube.retromon.game.Net;
import com.insidethecube.retromon.game.PlayerData;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.nbt.CompoundTag;
import org.lwjgl.glfw.GLFW;

/**
 * BILL's PC (the POKéMON STORAGE SYSTEM at a POKéMON CENTER), as the UI Viewer's: your team down the left as small
 * cards, the box as a grid of round-cornered tiles on one big white page (30 to a box, its name between arrows),
 * what the one you're on is along the foot. A on one: PUT IN BOX (from your team, never your last one able to
 * battle) or TAKE OUT (when there's room for it), CANCEL. The server does the moving and sends your data back.
 */
public class PcScreen extends GameScreen {
	/** 0: your team (the cards at the left); 1: the box (the grid). */
	private int side = 1, sel, page;
	private final TextBox box = new TextBox(List.of(""));
	private boolean talking;
	private Choice ask;
	private IntConsumer then;

	public PcScreen() {
		super("PC");
		Sounds.press();
	}

	private static List<Mon> team() {
		return ClientState.data.party;
	}

	private static List<Mon> boxed() {
		return ClientState.data.box;
	}

	private int pages() {
		return Math.max(1, (boxed().size() + 1 + 29) / 30);
	}

	/** The one you're on, or null for an empty space in the box. */
	private Mon current() {
		if (side == 0) return sel < team().size() ? team().get(sel) : null;
		int i = page * 30 + sel;
		return i < boxed().size() ? boxed().get(i) : null;
	}

	private void say(String line) {
		box.set(List.of(line));
		talking = true;
	}

	private void pick() {
		Mon m = current();
		if (m == null) return;
		Sounds.press();
		boolean out = side == 0;
		ask = new Choice(List.of(out ? "PUT IN BOX" : "TAKE OUT", "CANCEL"));
		then = i -> {
			if (i != 0) return;
			if (out) {
				long able = team().stream().filter(x -> !x.fainted()).count();
				if (team().size() <= 1 || !m.fainted() && able <= 1) {
					say("That's your last POKéMON! It has to stay with you.");
					return;
				}
				send("deposit", sel);
				say("**" + Look.text(m.name()) + "** went into the box.");
				sel = Math.max(0, Math.min(sel, team().size() - 2));
			} else {
				if (team().size() >= 6) {
					say("Your team's full! Put one in the box first.");
					return;
				}
				send("withdraw", page * 30 + sel);
				say("**" + Look.text(m.name()) + "** joined your team.");
			}
		};
	}

	private static void send(String op, int i) {
		CompoundTag t = new CompoundTag();
		t.putString("op", op);
		t.putInt("i", i);
		ClientPlayNetworking.send(new Net.Act("pc", t));
	}

	private void answer(int i) {
		IntConsumer t = then;
		ask = null;
		then = null;
		if (t != null) t.accept(i);
	}

	@Override
	public void tick() {
		super.tick();
		box.tick();
		int n = team().size();
		if (side == 0 && sel >= n) sel = Math.max(0, n - 1);
		if (page >= pages()) page = pages() - 1;
	}

	@Override
	protected boolean wantsTab() {
		return true;
	}

	@Override
	protected boolean key(KeyEvent e) {
		if (talking) {
			if ((Ui.isA(e) || Ui.isB(e)) && box.press()) talking = false;
			return true;
		}
		if (ask != null) {
			if (ask.key(e)) return true;
			if (Ui.isA(e)) {
				Sounds.select();
				answer(ask.at);
			} else if (Ui.isB(e)) answer(ask.options.size() - 1);
			return true;
		}
		if (e.key() == GLFW.GLFW_KEY_TAB) {
			page = (page + 1) % pages();
			Sounds.select();
			return true;
		}
		if (Ui.isA(e)) {
			pick();
			return true;
		}
		if (Ui.isB(e)) {
			onClose();
			return true;
		}
		int was = sel, wasSide = side;
		if (side == 0) {
			int n = Math.max(1, team().size());
			if (Ui.isUp(e)) sel = Math.floorMod(sel - 1, n);
			else if (Ui.isDown(e)) sel = Math.floorMod(sel + 1, n);
			else if (Ui.isRight(e)) {
				side = 1;
				sel = Math.min(29, sel * 5 / 6 * 6);
			}
		} else {
			if (Ui.isLeft(e) && sel % 6 == 0) {
				side = 0;
				sel = Math.max(0, Math.min(team().size() - 1, sel / 6));
			} else if (Ui.isLeft(e)) sel--;
			else if (Ui.isRight(e)) sel = Math.min(29, sel + 1);
			else if (Ui.isUp(e)) sel = sel - 6 >= 0 ? sel - 6 : sel;
			else if (Ui.isDown(e)) sel = sel + 6 < 30 ? sel + 6 : sel;
		}
		if (was != sel || wasSide != side) Sounds.select();
		return true;
	}

	@Override
	protected void back() {
		if (talking) {
			if (box.press()) talking = false;
		} else if (ask != null) answer(ask.options.size() - 1);
		else onClose();
	}

	@Override
	protected void clickAnywhere() {
		if (talking && box.press()) talking = false;
	}

	@Override
	public void onClose() {
		if (minecraft != null) minecraft.setScreen(null);
	}

	@Override
	protected void draw(GuiGraphics g, float partial) {
		PlayerData d = ClientState.data;
		Scenes.bg2(g, "pc");
		V.header(g, "picto_ball_aqua", "BILL'S PC", null);
		List<Mon> team = d.party, boxed = d.box;
		boolean free = !talking && ask == null;
		// Your team, down the left.
		V.label(g, Lang.t("YOUR TEAM") + " " + team.size() + "/6", 44, 92);
		for (int i = 0; i < team.size(); i++) {
			Mon m = team.get(i);
			float x = 44, y = 116 + i * 90, w = 300, h = 80;
			if (free && vover(x, y, w, h)) {
				side = 0;
				sel = i;
			}
			boolean on = side == 0 && sel == i;
			tile(g, x, y, w, h, 22, on, true);
			if (on) V.text(g, "▶", x - 22, V.capsTop(y + h / 2, 20), 20, V.RED, true);
			pic(g, m, x + 8, y + 8, 64);
			V.title(g, m.name(), x + 82, V.capsTop(y + 30, 21), 21, V.INK);
			String lv = "Lv. " + m.level;
			V.text(g, lv, x + 82, V.capsTop(y + 56, 16), 16, V.INK, true);
			float frac = m.hp / (float) Math.max(1, m.maxHp());
			V.bar(g, x + 82 + V.width(lv, 16, true) + 8, y + 52, 70, 8, frac, V.hpColour(frac));
			int idx = i;
			if (free) vhit(x, y, w, h, () -> {
				side = 0;
				sel = idx;
				pick();
			});
		}
		// The box: a big white page, its name between arrows, the grid.
		V.panel(g, 370, 96, 870, 470, 36, false);
		float x0 = 373, y0 = 99, mid = y0 + 20 + 20;
		float left = x0 + (864 - 266) / 2F;
		V.ellipse(g, left + 20, mid, 20, 20, 0xFFEEF3FA);
		V.tri(g, left + 19, mid, 14, 2, V.MUTED);
		V.ellipse(g, left + 266 - 20, mid, 20, 20, 0xFFEEF3FA);
		V.tri(g, left + 266 - 19, mid, 14, 0, V.MUTED);
		if (free) {
			vhit(left, mid - 20, 40, 40, () -> page = (page - 1 + pages()) % pages());
			vhit(left + 226, mid - 20, 40, 40, () -> page = (page + 1) % pages());
		}
		String title = Lang.t("BOX") + " " + (page + 1);
		V.title(g, title, left + 133 - V.titleWidth(title, 28) / 2, V.capsTop(mid, 28), 28, V.INK);
		String cnt = Math.min(30, Math.max(0, boxed.size() - page * 30)) + "/30";
		V.label(g, cnt, x0 + 864 - 34 - V.labelWidth(cnt), y0 + 34);
		float gx = x0 + 30, gy = y0 + 60 + 18, cw = (804 - 60) / 6F, ch = 64;
		for (int k = 0; k < 30; k++) {
			float x = gx + (k % 6) * (cw + 12), y = gy + (k / 6) * (ch + 12);
			if (free && vover(x, y, cw, ch)) {
				side = 1;
				sel = k;
			}
			boolean on = side == 1 && sel == k;
			int i = page * 30 + k;
			if (i < boxed.size()) {
				Mon m = boxed.get(i);
				tile(g, x, y, cw, ch, 20, on, false);
				pic(g, m, x + 6, y + 6, 52);
				V.right(g, "Lv. " + m.level, x + cw - 12, V.capsTop(y + ch / 2, 15), 15, V.INK, true);
			} else {
				V.rr(g, x, y, cw, ch, 20, on ? 0xFFFFC93C : 0xFFEEF2F8);
				V.rr(g, x + 2, y + 2, cw - 4, ch - 4, 18, 0xFFFAFCFE);
			}
			int kk = k;
			if (free) vhit(x, y, cw, ch, () -> {
				side = 1;
				sel = kk;
				pick();
			});
		}
		// The one you're on, along the foot.
		V.panel(g, 370, 584, 870, 72, 36, true);
		Mon cur = current();
		float fy = 584 + 36;
		if (cur != null) {
			float tx = 373 + 26;
			V.title(g, cur.name(), tx, V.capsTop(fy, 26), 26, V.INK);
			tx += V.titleWidth(cur.name(), 26) + 14;
			String gm = cur.genderMark();
			if (gm != null && !gm.isEmpty()) {
				V.text(g, gm, tx, V.capsTop(fy, 22), 22, gm.contains("♀") ? 0xFFFF8FB1 : 0xFF7FB8FF, true);
				tx += V.width(gm, 22, true) + 14;
			}
			tx += V.type(g, cur.species.type1().label, Ui.typeColor(cur.species.type1()) | 0xFF000000, tx, fy - 12, 0, 13.5F) + 6;
			if (cur.species.type2() != null && cur.species.type2() != cur.species.type1())
				V.type(g, cur.species.type2().label, Ui.typeColor(cur.species.type2()) | 0xFF000000, tx, fy - 12, 0, 13.5F);
			String hint = Lang.t(side == 0 ? "A: put it in the box" : "A: take it out");
			float rx = 1240 - 3 - 30;
			V.right(g, hint, rx, V.capsTop(fy, 16), 16, V.MUTED, true);
			rx -= V.width(hint, 16, true) + 14;
			V.right(g, "Lv. " + cur.level, rx, V.capsTop(fy, 20), 20, V.INK, true);
		} else V.text(g, Lang.t("An empty space in the box."), 373 + 30, V.capsTop(fy, 20), 20, V.MUTED, true);
		V.hints(g, "A", "Choose", "B", "Back", "TAB", "Next box");
		if (ask != null) vchoice(g, ask, 940, 300, 280, this::answer);
		if (talking) box.renderV(g, font, 140, 26, 1000, ticks, partial, true);
	}

	/** A card or tile: white (the team) or pale with a fine rim (the box); picked, sunny with white and gold rings. */
	private static void tile(GuiGraphics g, float x, float y, float w, float h, float r, boolean on, boolean card) {
		if (on) {
			V.softShadow(g, x, y + 8, w, h, 16, 0x4DE8961E);
			V.rr(g, x - 6, y - 6, w + 12, h + 12, r + 6, 0xFFFFC93C);
			V.rr(g, x - 3, y - 3, w + 6, h + 6, r + 3, 0xFFFFFFFF);
			V.sel(g, x, y, w, h, r);
		} else if (card) {
			V.softShadow(g, x, y + 6, w, h, 14, 0x2E1E2854);
			V.rr(g, x, y, w, h, r, 0xFFFFFFFF);
		} else {
			V.rr(g, x, y, w, h, r, 0xFFE8EDF6);
			V.rr(g, x + 2, y + 2, w - 4, h - 4, r - 2, 0xFFF5F8FD);
		}
	}

	/** Its picture, smooth, greyed when it's fainted. */
	private static void pic(GuiGraphics g, Mon m, float x, float y, float size) {
		Ui.blitSmooth(g, Look.portrait(m.species.id()), V.x(x), V.y(y), V.x(x + size), V.y(y + size), 0, 1, 0, 1, m.fainted() ? 0x99FFFFFF : 0xFFFFFFFF);
	}
}
