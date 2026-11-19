package com.insidethecube.retromon.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;

/**
 * The UI Viewer's maker: you (body, skin, eyes, hair) and your clothes, as rows of chips and colour swatches in
 * cards, two columns of them, with DONE (or UNDO) and MIX IT UP underneath. The new game's character maker uses
 * both tabs; CLOTHES uses the clothes alone. Up and down between rows, left and right to change, TAB between tabs.
 */
public final class Maker {
	/** Where a click goes (the screen's vhit). */
	public interface Hits {
		void add(float x, float y, float w, float h, Runnable click);
	}

	/** A row: its label, the part it sets, its choices (chips or swatches). */
	private record Row(String label, String group, String part, String[] chips, int[] swatches, int[] values, int newFrom) {
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
	private final String done;
	private final Runnable onDone, onBack;
	/** Called when what you look like changes. */
	private final Runnable changed;

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
			r.add(new Row("BODY", "YOU", "body", new String[] {"Boy", "Girl"}, null, new int[] {0, 1}, -1));
			r.add(new Row("SKIN", "YOU", "skin", null, Outfit.SKIN_RGB, range(Outfit.SKIN_RGB.length), 4));
			r.add(new Row("EYES", "YOU", "eyes", null, Outfit.EYE_RGB, range(Outfit.EYE_RGB.length), 0));
			r.add(new Row("STYLE", "HAIR", "hair", titled(Outfit.HAIRS), null, range(Outfit.HAIRS.length), -1));
			r.add(new Row("COLOUR", "HAIR", "hairc", null, Outfit.COLOURS, range(Outfit.COLOURS.length), -1));
		} else {
			r.add(new Row("STYLE", "HAT", "hat", titled(Outfit.HATS), null, range(Outfit.HATS.length), -1));
			r.add(new Row("CAP", "HAT", "capp", CAP_LABELS, null, CAP_VALUES, -1));
			r.add(new Row("CAP WAY", "HAT", "capb", new String[] {"Forwards", "Backwards"}, null, new int[] {0, 1}, -1));
			r.add(new Row("COLOUR", "HAT", "hatc", null, Outfit.COLOURS, range(Outfit.COLOURS.length), -1));
			r.add(new Row("STYLE", "TOP", "top", titled(Outfit.TOPS), null, range(Outfit.TOPS.length), -1));
			r.add(new Row("COLOUR", "TOP", "topc", null, Outfit.COLOURS, range(Outfit.COLOURS.length), -1));
			r.add(new Row("STYLE", "BOTTOMS", "bot", titled(Outfit.BOTTOMS), null, range(Outfit.BOTTOMS.length), -1));
			r.add(new Row("COLOUR", "BOTTOMS", "botc", null, Outfit.COLOURS, range(Outfit.COLOURS.length), -1));
			r.add(new Row("COLOUR", "SHOES", "shoec", null, Outfit.COLOURS, range(Outfit.COLOURS.length), -1));
			r.add(new Row("STYLE", "BAG", "bag", titled(Outfit.BAGS), null, range(Outfit.BAGS.length), -1));
			r.add(new Row("COLOUR", "BAG", "bagc", null, Outfit.COLOURS, range(Outfit.COLOURS.length), -1));
		}
		return r;
	}

	private int get(Row r) {
		return r.part.equals("body") ? (girl ? 1 : 0) : outfit.get(r.part);
	}

	private void set(Row r, int value) {
		if (r.part.equals("body")) {
			boolean g = value == 1;
			if (g != girl) {
				// A new body: its own clothes and hair, your skin and eyes kept.
				int skin = outfit.get("skin"), eyes = outfit.get("eyes");
				girl = g;
				outfit = Outfit.defaults(girl);
				outfit.v.put("skin", skin);
				outfit.v.put("eyes", eyes);
			}
		} else {
			outfit.v.put(r.part, value);
			if (r.part.equals("capp") || r.part.equals("capb")) outfit.v.put("hat", 1);
		}
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

	public void mix() {
		Random rng = new Random();
		for (int t = 0; t < tabs.length; t++) {
			int keep = tab;
			tab = t;
			for (Row r : rows()) {
				if (r.part.equals("body")) continue;
				outfit.v.put(r.part, r.values[rng.nextInt(r.values.length)]);
			}
			tab = keep;
		}
		Sounds.press();
		changed.run();
	}

	public boolean key(KeyEvent e) {
		List<Row> rows = rows();
		int n = rows.size() + 2;
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
			if (Ui.isLeft(e) || Ui.isRight(e)) {
				sel = sel == rows.size() ? rows.size() + 1 : rows.size();
				Sounds.select();
				return true;
			}
			if (Ui.isA(e)) {
				if (sel == rows.size()) onDone.run();
				else mix();
				return true;
			}
		} else if (Ui.isLeft(e) || Ui.isRight(e)) {
			Row r = rows.get(sel);
			int i = Math.floorMod(index(r) + (Ui.isRight(e) ? 1 : -1), r.values.length);
			set(r, r.values[i]);
			return true;
		}
		if (Ui.isB(e) && onBack != null) {
			onBack.run();
			return true;
		}
		return false;
	}

	// ---------- drawing (the viewer's layout, in canvas pixels) ----------

	private static final float X = 24, Y = 20, PW = 870, PAD_X = 16, PAD_Y = 14, GAP = 12, COL = (PW - 2 * PAD_X - GAP) / 2;
	/** The labels' column is wide enough for COLOUR with its arrow (it ran into the swatches). */
	private static final float LABEL_W = 84, ROW_PAD_X = 10, ROW_PAD_Y = 6, CARD_PAD = 10, TITLE_H = 26, SW = 30, SW_GAP = 8, CHIP = 14, CHIP_H = 26;

	/** The options' lines: where each sits (x, y from the options' top left), and how tall they come to. */
	private static float[][] flow(Row r, float width) {
		float[][] at = new float[r.count()][2];
		float x = 0, y = 0, lineH = r.isSwatch() ? SW : CHIP_H;
		for (int i = 0; i < r.count(); i++) {
			float w = r.isSwatch() ? SW : V.chipWidth(r.chips[i], CHIP);
			if (x > 0 && x + w > width) {
				x = 0;
				y += lineH + SW_GAP;
			}
			at[i][0] = x;
			at[i][1] = y;
			x += w + SW_GAP;
		}
		return at;
	}

	private static float optionsHeight(Row r, float width) {
		float[][] at = flow(r, width);
		return at.length == 0 ? 0 : at[at.length - 1][1] + (r.isSwatch() ? SW : CHIP_H);
	}

	/** Draws the maker, registering clicks. Returns the bottom of its panel (canvas pixels). */
	public float draw(GuiGraphics g, Hits hits, float secs) {
		List<Row> rows = rows();
		float optW = COL - 2 * CARD_PAD + 2 - 2 * ROW_PAD_X - LABEL_W - 8;
		// Cards: the rows grouped by what they're for.
		List<String> groups = new ArrayList<>();
		List<List<Integer>> members = new ArrayList<>();
		for (int i = 0; i < rows.size(); i++) {
			String gname = rows.get(i).group;
			if (groups.isEmpty() || !groups.get(groups.size() - 1).equals(gname)) {
				groups.add(gname);
				members.add(new ArrayList<>());
			}
			members.get(members.size() - 1).add(i);
		}
		float[] rowH = new float[rows.size()];
		for (int i = 0; i < rows.size(); i++) rowH[i] = Math.max(20, optionsHeight(rows.get(i), optW)) + 2 * ROW_PAD_Y;
		float[] cardH = new float[groups.size()];
		float total = 0;
		for (int c = 0; c < groups.size(); c++) {
			float h = CARD_PAD * 2 + TITLE_H;
			for (int i : members.get(c)) h += rowH[i] + 2;
			cardH[c] = h;
			total += h + GAP;
		}
		// Two columns, balanced as the viewer's (cards don't split): the first column takes cards till it's half.
		int split = groups.size();
		float run = 0;
		for (int c = 0; c < groups.size(); c++) {
			if (run > 0 && run + cardH[c] / 2 > total / 2) {
				split = c;
				break;
			}
			run += cardH[c] + GAP;
		}
		float top = Y + PAD_Y + (tabs.length > 1 ? 36 : 0);
		float colH0 = 0, colH1 = 0;
		for (int c = 0; c < groups.size(); c++) if (c < split) colH0 += cardH[c] + GAP;
			else colH1 += cardH[c] + GAP;
		float contentH = Math.max(colH0, colH1) - GAP;
		float ph = (top - Y) + contentH + 10 + 46 + PAD_Y;
		V.panel(g, X, Y, PW, ph);
		if (tabs.length > 1) {
			float tx = X + PAD_X + 10;
			for (int t = 0; t < tabs.length; t++) {
				String name = tabs[t].equals("you") ? "YOU" : "CLOTHES";
				float w = V.chip(g, name, tx, Y + PAD_Y + 2, 14, t == tab, false);
				int tt = t;
				hits.add(tx, Y + PAD_Y + 2, w, 26, () -> {
					tab = tt;
					sel = 0;
				});
				tx += w + 8;
			}
			V.right(g, "TAB switches", X + PW - PAD_X - 4, Y + PAD_Y + 9, 12, V.MUTED, true);
		}
		float[] cy = {top, top};
		for (int c = 0; c < groups.size(); c++) {
			int col = c < split ? 0 : 1;
			float cx = X + PAD_X + col * (COL + GAP), y = cy[col];
			V.rr(g, cx, y, COL, cardH[c], 18, 0x29000000);
			V.tracked(g, groups.get(c), cx + CARD_PAD + 12, y + CARD_PAD + 2, 17, V.EDGE_LIGHT, 2);
			float ry = y + CARD_PAD + TITLE_H;
			for (int i : members.get(c)) {
				Row r = rows.get(i);
				boolean on = i == sel, off = dim(r);
				float rx = cx + 6, rw = COL - 12, rh = rowH[i];
				if (on) {
					V.rr(g, rx, ry, rw, rh, 14, 0x21FFF4D8);
					V.ring(g, rx, ry, rw, rh, 14, 2, 0x8CFFF4D8);
				}
				int la = off ? 0x66000000 : 0xFF000000;
				V.label(g, (on ? "▸ " : "") + r.label, rx + ROW_PAD_X, ry + rh / 2 - 7, Ui.alpha(on ? V.CREAM : V.MUTED, off ? 0.4F : 1));
				float ox = rx + ROW_PAD_X + LABEL_W + 8, oy = ry + ROW_PAD_Y + Math.max(0, (rh - 2 * ROW_PAD_Y - optionsHeight(r, optW)) / 2);
				float[][] at = flow(r, optW);
				int cur = index(r);
				for (int j = 0; j < r.count(); j++) {
					float ax = ox + at[j][0], ay = oy + at[j][1];
					boolean picked = j == cur;
					if (r.isSwatch()) {
						V.swatch(g, r.swatches[j], ax, ay, SW, picked, 9);
						if (r.newFrom >= 0 && j >= r.newFrom) {
							V.rr(g, ax + SW - 6, ay - 4, 10, 10, 5, 0xFF123C38);
							V.rr(g, ax + SW - 5, ay - 3, 8, 8, 4, V.GOLD);
						}
					} else V.chip(g, r.chips[j], ax, ay, CHIP, picked, false);
					if (off) V.rr(g, ax - 2, ay - 2, (r.isSwatch() ? SW : V.chipWidth(r.chips[j], CHIP)) + 4, (r.isSwatch() ? SW : CHIP_H) + 4, 12, 0x99123C38);
					int jj = j, ii = i;
					hits.add(ax, ay, r.isSwatch() ? SW : V.chipWidth(r.chips[j], CHIP), r.isSwatch() ? SW : CHIP_H, () -> {
						sel = ii;
						set(r, r.values[jj]);
					});
				}
				ry += rh + 2;
			}
			cy[col] += cardH[c] + GAP;
		}
		// DONE (or UNDO) and MIX IT UP.
		float by = top + contentH + 10, bx = X + PAD_X + 10;
		String[] buttons = {done, "MIX IT UP"};
		for (int b = 0; b < 2; b++) {
			float w = V.width(buttons[b], 21, true) + 62;
			V.item(g, buttons[b], null, bx, by, w, 46, sel == rows.size() + b, false, 21);
			int bb = b;
			hits.add(bx, by, w, 46, () -> {
				sel = rows.size() + bb;
				if (bb == 0) onDone.run();
				else mix();
			});
			bx += w + 12;
		}
		if (tabs[tab].equals("you")) {
			float nx = X + PW - PAD_X - 10 - V.width("New: four more skin tones, eye colour", 12.5F, true);
			V.rr(g, nx - 14, by + 19, 9, 9, 4.5F, V.GOLD);
			V.text(g, "New: four more skin tones, eye colour", nx, by + 17, 12.5F, V.SOFT_TEXT, true);
		}
		return Y + ph;
	}

	private static String spaced(String s) {
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			b.append(s.charAt(i));
			if (i < s.length() - 1) b.append(' ');
		}
		return b.toString();
	}
}
