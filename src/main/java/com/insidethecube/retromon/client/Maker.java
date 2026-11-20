package com.insidethecube.retromon.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;

/**
 * The UI Viewer's maker, as signed off (20 Nov): a glass panel of soft blue light on the right, YOU (eyes, hair style,
 * hair colour) and CLOTHES as tabs, each in cards by what they're for (HAT, TOP, BOTTOMS, SHOES, BAG), one row
 * under another (a label, then its chips or colour swatches), the row you're on lit in gold; MIX IT UP at the foot.
 * It scrolls (a rounded mint bar) when it's taller than its room. Up and down between rows, left and right to change,
 * TAB between tabs. The new game's maker uses both tabs (your skin is set by the face you picked, so no skin row);
 * CLOTHES uses the clothes alone, with UNDO.
 */
public final class Maker {
	/** Where a click goes (the screen's vhit). */
	public interface Hits {
		void add(float x, float y, float w, float h, Runnable click);
	}

	/** A row: its label, its card, the part it sets, its choices (chips or swatches). */
	private record Row(String label, String group, String part, String[] chips, int[] swatches, int[] values) {
		boolean isSwatch() {
			return swatches != null;
		}

		int count() {
			return isSwatch() ? swatches.length : chips.length;
		}
	}

	/** The caps the maker offers (the viewer's four), as the outfit's cap numbers. */
	private static final int[] CAP_VALUES = {0, 8, 9, 10};
	private static final String[] CAP_LABELS = {"Poké Ball", "Great Ball", "Ultra Ball", "Plain"};

	public Outfit outfit;
	public boolean girl;
	private final String[] tabs;
	private int tab, sel;
	/** The button beside MIX IT UP (UNDO, in CLOTHES), or null for none (the new game's: Continue goes on). */
	private final String done;
	private final Runnable onDone, onBack;
	/** Called when what you look like changes. */
	private final Runnable changed;
	/** Where the panel sits (canvas): x, y, width, the most it's tall. */
	public float px = 650, py = 24, pw = 600, maxH = 528;
	private float scroll, scrollGoal;

	public Maker(Outfit outfit, boolean girl, String[] tabs, String done, Runnable onDone, Runnable onBack, Runnable changed) {
		this.outfit = outfit;
		this.girl = girl;
		this.tabs = tabs;
		this.done = done;
		this.onDone = onDone;
		this.onBack = onBack;
		this.changed = changed;
	}

	private static String[] titled(String[] names) {
		String[] out = new String[names.length];
		for (int i = 0; i < names.length; i++) out[i] = names[i].substring(0, 1).toUpperCase() + names[i].substring(1).toLowerCase();
		return out;
	}

	private static int[] range(int n) {
		int[] v = new int[n];
		for (int i = 0; i < n; i++) v[i] = i;
		return v;
	}

	private List<Row> rows() {
		List<Row> r = new ArrayList<>();
		if (tabs[tab].equals("you")) {
			r.add(new Row("EYES", "YOU", "eyes", null, Outfit.EYE_RGB, range(Outfit.EYE_RGB.length)));
			r.add(new Row("HAIR STYLE", "YOU", "hair", titled(Outfit.HAIRS), null, range(Outfit.HAIRS.length)));
			r.add(new Row("HAIR COLOUR", "YOU", "hairc", null, Outfit.COLOURS, range(Outfit.COLOURS.length)));
		} else {
			r.add(new Row("STYLE", "HAT", "hat", titled(Outfit.HATS), null, range(Outfit.HATS.length)));
			r.add(new Row("CAP", "HAT", "capp", CAP_LABELS, null, CAP_VALUES));
			r.add(new Row("CAP WAY", "HAT", "capb", new String[] {"Forwards", "Backwards"}, null, new int[] {0, 1}));
			r.add(new Row("COLOUR", "HAT", "hatc", null, sub(Outfit.COLOURS, Outfit.CLOTHES), range(Outfit.CLOTHES)));
			r.add(new Row("STYLE", "TOP", "top", titled(Outfit.TOPS), null, range(Outfit.TOPS.length)));
			r.add(new Row("COLOUR", "TOP", "topc", null, sub(Outfit.COLOURS, Outfit.CLOTHES), range(Outfit.CLOTHES)));
			r.add(new Row("STYLE", "BOTTOMS", "bot", titled(Outfit.BOTTOMS), null, range(Outfit.BOTTOMS.length)));
			r.add(new Row("COLOUR", "BOTTOMS", "botc", null, sub(Outfit.COLOURS, Outfit.CLOTHES), range(Outfit.CLOTHES)));
			r.add(new Row("COLOUR", "SHOES", "shoec", null, sub(Outfit.COLOURS, Outfit.CLOTHES), range(Outfit.CLOTHES)));
			r.add(new Row("STYLE", "BAG", "bag", titled(Outfit.BAGS), null, range(Outfit.BAGS.length)));
			r.add(new Row("COLOUR", "BAG", "bagc", null, sub(Outfit.COLOURS, Outfit.CLOTHES), range(Outfit.CLOTHES)));
		}
		return r;
	}

	private static int[] sub(int[] a, int n) {
		return java.util.Arrays.copyOf(a, Math.min(n, a.length));
	}

	private int get(Row r) {
		return outfit.get(r.part);
	}

	private void set(Row r, int value) {
		outfit.v.put(r.part, value);
		if (r.part.equals("capp") || r.part.equals("capb")) outfit.v.put("hat", 1);
		Sounds.select();
		changed.run();
	}

	private boolean dim(Row r) {
		return (r.part.equals("capp") || r.part.equals("capb")) && outfit.get("hat") != 1;
	}

	private int index(Row r) {
		int v = get(r);
		for (int i = 0; i < r.values.length; i++) if (r.values[i] == v) return i;
		return 0;
	}

	/** MIX IT UP: everything but your skin picked at random. */
	public void mix() {
		Random rng = new Random();
		int keep = tab;
		for (int t = 0; t < tabs.length; t++) {
			tab = t;
			for (Row r : rows()) outfit.v.put(r.part, r.values[rng.nextInt(r.values.length)]);
		}
		tab = keep;
		Sounds.press();
		changed.run();
	}

	private int buttons() {
		return done == null ? 1 : 2;
	}

	public boolean key(KeyEvent e) {
		List<Row> rows = rows();
		int n = rows.size() + buttons();
		if (e.key() == GLFW.GLFW_KEY_TAB && tabs.length > 1) {
			tab = (tab + 1) % tabs.length;
			sel = 0;
			Sounds.select();
			return true;
		}
		if (Ui.isUp(e) || Ui.isDown(e)) {
			sel = Math.floorMod(sel + (Ui.isDown(e) ? 1 : -1), n);
			Sounds.select();
			return true;
		}
		if (sel >= rows.size()) {
			if ((Ui.isLeft(e) || Ui.isRight(e)) && buttons() > 1) {
				sel = sel == rows.size() ? rows.size() + 1 : rows.size();
				Sounds.select();
				return true;
			}
			if (Ui.isA(e)) {
				if (done != null && sel == rows.size()) onDone.run();
				else mix();
				return true;
			}
		} else if (Ui.isLeft(e) || Ui.isRight(e)) {
			Row r = rows.get(sel);
			int i = Math.floorMod(index(r) + (Ui.isRight(e) ? 1 : -1), r.values.length);
			set(r, r.values[i]);
			return true;
		} else if (Ui.isA(e) && done != null) {
			onDone.run();
			return true;
		}
		if (Ui.isB(e) && onBack != null) {
			onBack.run();
			return true;
		}
		return false;
	}

	/** The mouse wheel over the panel: scrolls it. */
	public boolean scrolled(double vx, double vy, double amount) {
		if (vx < px || vx > px + pw || vy < py || vy > py + maxH) return false;
		scrollGoal -= (float) amount * 60;
		return true;
	}

	// ---------- drawing (the viewer's .mk-glass, in canvas pixels) ----------

	private static final float PAD = 16, CARD_PAD = 12, CARD_GAP = 12, ROW_GAP = 12, ROW_PAD_X = 12, ROW_PAD_Y = 9, OPT_GAP = 6, SW = 29, CHIP = 14,
			CHIP_H = 14 * 1.5F + 6 + 4, LABEL = 12.5F, LABEL_H = 12.5F * 1.5F, TITLE = 17, TITLE_H = 17 * 1.5F + 4;

	private static float optW(Row r, int i) {
		return r.isSwatch() ? SW : V.width(r.chips[i], CHIP, false) + 22 + 4;
	}

	/** The options' places in a row (x, y from their top left), and how tall they come to. */
	private static float[][] flow(Row r, float width) {
		float[][] at = new float[r.count() + 1][2];
		float x = 0, y = 0, lh = r.isSwatch() ? SW : CHIP_H;
		for (int i = 0; i < r.count(); i++) {
			float w = optW(r, i);
			if (x > 0 && x + w > width) {
				x = 0;
				y += lh + OPT_GAP;
			}
			at[i][0] = x;
			at[i][1] = y;
			x += w + OPT_GAP;
		}
		at[r.count()][1] = y + lh;
		return at;
	}

	private float rowH(Row r, float inner) {
		return ROW_PAD_Y * 2 + LABEL_H + 8 + flow(r, inner - ROW_PAD_X * 2)[r.count()][1];
	}

	/** Draws the panel (and registers its clicks). Returns how tall it is. */
	public float draw(GuiGraphics g, Hits hits, float secs) {
		List<Row> rows = rows();
		float inner = pw - PAD * 2, cardW = inner, rowW = cardW - CARD_PAD * 2;
		// How tall it all is: the tab bar, the cards, the buttons.
		float tabBar = tabs.length > 1 ? 31 + 8 + 2 : 0, contentH = tabBar;
		List<float[]> cards = new ArrayList<>();
		String group = null;
		float cy = 0;
		for (int i = 0; i < rows.size(); i++) {
			Row r = rows.get(i);
			if (!r.group.equals(group)) {
				if (group != null) contentH += CARD_PAD + 2 + CARD_GAP;
				cards.add(new float[] {contentH});
				group = r.group;
				contentH += CARD_PAD + TITLE_H + ROW_GAP;
			} else contentH += ROW_GAP;
			contentH += rowH(r, rowW);
		}
		contentH += CARD_PAD + 2 + CARD_GAP;
		float btnY = contentH, btnH = 22 + 16 + 4;
		contentH += btnH;
		float h = Math.min(maxH, contentH + PAD + 14);
		// The glass: a soft blue wash, a white rim and a faint halo, a deep soft shadow.
		V.shadow(g, px, py + 8, pw, h, 30, 1.1F);
		V.rr(g, px - 6, py - 6, pw + 12, h + 12, 36, 0x24FFFFFF);
		V.rr(g, px - 2, py - 2, pw + 4, h + 4, 32, 0x99FFFFFF);
		V.rrv(g, px, py, pw, h, 30, 0x9E30589A, 0xB0223E7A);
		// Keep the row you're on in view.
		float viewH = h - PAD - 14;
		float selTop = 0, selBottom = 0, y0 = tabBar;
		group = null;
		for (int i = 0; i < rows.size() && i <= sel; i++) {
			Row r = rows.get(i);
			if (!r.group.equals(group)) {
				if (group != null) y0 += CARD_PAD + 2 + CARD_GAP;
				group = r.group;
				y0 += CARD_PAD + TITLE_H + ROW_GAP;
			} else y0 += ROW_GAP;
			selTop = y0;
			y0 += rowH(r, rowW);
			selBottom = y0;
		}
		if (sel >= rows.size()) {
			selTop = btnY;
			selBottom = btnY + btnH;
		}
		float maxScroll = Math.max(0, contentH - viewH);
		if (selTop < scrollGoal) scrollGoal = selTop - 8;
		if (selBottom > scrollGoal + viewH) scrollGoal = selBottom - viewH + 8;
		scrollGoal = Math.max(0, Math.min(maxScroll, scrollGoal));
		scroll += (scrollGoal - scroll) * 0.25F;
		if (Math.abs(scrollGoal - scroll) < 0.5F) scroll = scrollGoal;
		int sx0 = Math.round(V.x(px)), sy0 = Math.round(V.y(py + PAD)), sx1 = Math.round(V.x(px + pw)), sy1 = Math.round(V.y(py + h - 14));
		g.enableScissor(sx0, sy0, sx1, sy1);
		float ox = px + PAD, oy = py + PAD - scroll;
		// The tabs: YOU and CLOTHES as chips, "TAB switches" at the right.
		if (tabs.length > 1) {
			float tx = ox + 10;
			for (int t = 0; t < tabs.length; t++) {
				String name = tabs[t].equals("you") ? "YOU" : "CLOTHES";
				float w = V.chip(g, name, tx, oy + 2, 14, t == tab, false);
				int tt = t;
				if (visible(oy + 2, 31, py + PAD, py + h - 14)) hits.add(tx, oy + 2, w, 31, () -> {
					tab = tt;
					sel = 0;
					Sounds.select();
				});
				tx += w + 8;
			}
			String sw = Lang.t("TAB switches");
			V.text(g, sw, ox + inner - V.width(sw, 12, true), V.capsTop(oy + 2 + 15, 12), 12, 0xD9FFFFFF, true);
		}
		float y = oy + tabBar;
		group = null;
		float cardTop = 0;
		for (int i = 0; i < rows.size(); i++) {
			Row r = rows.get(i);
			if (!r.group.equals(group)) {
				if (group != null) {
					card(g, ox, cardTop, cardW, y + CARD_PAD + 2 - cardTop);
					y += CARD_PAD + 2 + CARD_GAP;
				}
				group = r.group;
				cardTop = y;
				y += CARD_PAD + TITLE_H + ROW_GAP;
			} else y += ROW_GAP;
			y += rowH(r, rowW);
		}
		card(g, ox, cardTop, cardW, y + CARD_PAD + 2 - cardTop);
		// Now the words and choices over the cards.
		y = oy + tabBar;
		group = null;
		for (int i = 0; i < rows.size(); i++) {
			Row r = rows.get(i);
			if (!r.group.equals(group)) {
				if (group != null) y += CARD_PAD + 2 + CARD_GAP;
				group = r.group;
				V.titleTracked(g, r.group, ox + CARD_PAD + 12, V.capsTop(y + CARD_PAD + TITLE_H / 2, TITLE) + 0.5F + 2, TITLE, 0x59001A33, 2);
				V.titleTracked(g, r.group, ox + CARD_PAD + 12, V.capsTop(y + CARD_PAD + TITLE_H / 2, TITLE), TITLE, 0xFFFFFFFF, 2);
				y += CARD_PAD + TITLE_H + ROW_GAP;
			} else y += ROW_GAP;
			float rh = rowH(r, rowW), rx = ox + CARD_PAD;
			boolean on = i == sel;
			int row = i;
			if (on) {
				V.rr(g, rx, y, rowW, rh, 14, 0x52FFEC96);
				V.ring(g, rx, y, rowW, rh, 14, 2.5F, 0xFFFFE07A);
			}
			int alpha = dim(r) ? 0x66 : 0xFF;
			String label = (on ? "▸ " : "") + Lang.t(r.label);
			V.label(g, label, rx + ROW_PAD_X, y + ROW_PAD_Y + 2, (alpha << 24) | (on ? 0xFFF3B0 : 0xFFFFFF));
			float[][] at = flow(r, rowW - ROW_PAD_X * 2);
			float oxx = rx + ROW_PAD_X, oyy = y + ROW_PAD_Y + LABEL_H + 8;
			int cur = index(r);
			for (int j = 0; j < r.count(); j++) {
				float x = oxx + at[j][0], yy = oyy + at[j][1];
				if (r.isSwatch()) {
					boolean pick = j == cur;
					if (pick) V.rr(g, x - 4, yy - 4, SW + 8, SW + 8, 13, 0xFFFFC93C);
					V.rr(g, x, yy + 2, SW, SW, 9, 0x401E2854);
					V.rr(g, x, yy, SW, SW, 9, Ui.alpha(0xFFFFFFFF, alpha / 255F));
					V.rrv(g, x + 3, yy + 3, SW - 6, SW - 6, 6, Ui.alpha(Ui.mix(r.swatches[j] | 0xFF000000, 0xFFFFFFFF, 0.28F), alpha / 255F),
							Ui.alpha(Ui.mix(r.swatches[j] | 0xFF000000, 0xFF000000, 0.12F), alpha / 255F));
				} else V.chip(g, r.chips[j], x, yy, CHIP, j == cur, false);
				int jj = j;
				if (visible(yy, r.isSwatch() ? SW : CHIP_H, py + PAD, py + h - 14)) hits.add(x, yy, optW(r, j), r.isSwatch() ? SW : CHIP_H, () -> {
					sel = row;
					set(r, r.values[jj]);
				});
			}
			if (visible(y, rh, py + PAD, py + h - 14)) hits.add(rx, y, ROW_PAD_X + V.labelWidth(label) + 20, LABEL_H + ROW_PAD_Y + 4, () -> sel = row);
			y += rh;
		}
		// The buttons: UNDO (CLOTHES) and MIX IT UP, glass pills, gold when you're on them.
		float bx = ox + 10, by = oy + btnY;
		if (done != null) bx += glassButton(g, hits, done, null, bx, by, sel == rows.size(), () -> onDone.run(), h) + 12;
		glassButton(g, hits, "MIX IT UP", "icon_mix", bx, by, sel == rows.size() + buttons() - 1, this::mix, h);
		g.disableScissor();
		// The scroll bar: rounded, mint, when it's taller than its room.
		if (maxScroll > 0) {
			float trackTop = py + 22, trackH = h - 44, thumbH = Math.max(40, trackH * viewH / contentH), thumbY = trackTop + (trackH - thumbH) * (scroll / maxScroll);
			V.rrv(g, px + pw - 12 + 3, thumbY, 6, thumbH, 3, 0xFF9FF0D8, 0xFF41BF84);
		}
		return h;
	}

	private static boolean visible(float y, float hh, float top, float bottom) {
		return y + hh > top && y < bottom;
	}

	/** A card in the glass: a faint white wash and a fine rim. */
	private static void card(GuiGraphics g, float x, float y, float w, float h) {
		V.rr(g, x, y, w, h, 18, 0x1AFFFFFF);
		V.ring(g, x, y, w, h, 18, 1.5F, 0x38FFFFFF);
	}

	/** A glass pill button (the viewer's .mk-btn): its icon, its words in white Fredoka; gold with ink when picked. Returns its width. */
	private float glassButton(GuiGraphics g, Hits hits, String words, String icon, float x, float y, boolean on, Runnable click, float panelH) {
		String w0 = Lang.t(words);
		float tw = V.titleTrackedWidth(w0, 16, 1), w = 14 + (icon == null ? 0 : 22 + 10) + tw + 20, hh = 16 * 1.5F + 16;
		if (on) {
			V.pill(g, x - 3, y - 3, w + 6, hh + 6, 0xFFFFFFFF);
			V.sel(g, x, y, w, hh, hh / 2);
		} else {
			V.pill(g, x, y, w, hh, 0x29FFFFFF);
			V.ring(g, x, y, w, hh, hh / 2, 2, 0x8CFFFFFF);
		}
		int c = on ? V.INK : 0xFFFFFFFF;
		float ix = x + 14;
		if (icon != null) {
			V.icon(g, icon, ix, y + (hh - 22) / 2, 22, c);
			ix += 22 + 10;
		}
		V.titleTracked(g, w0, ix, V.capsTop(y + hh / 2, 16), 16, c, 1);
		if (visible(y, hh, py + PAD, py + panelH - 14)) hits.add(x, y, w, hh, click);
		return w;
	}
}
