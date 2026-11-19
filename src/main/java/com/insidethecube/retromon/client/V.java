package com.insidethecube.retromon.client;

import java.util.List;

import com.insidethecube.retromon.Retromon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

/**
 * The UI Viewer's look, drawn the same way in game. Every screen is laid out on the viewer's own 1280 by 720
 * canvas, which is fitted to the window (as large as it goes, centred), so a panel at (70, 90) here is where it is
 * in the viewer. Its panels (teal glass, a mint rim and a soft shadow), pills, chips, keys, text sizes and colours
 * are the viewer's, in canvas pixels; the backdrop behind them fills the whole window.
 */
public final class V {
	private V() {}

	public static final float W = 1280, H = 720;
	/** GUI pixels to a canvas pixel, and where the canvas starts in the window. */
	public static float k = 1, ox, oy;
	/** The window in GUI pixels. */
	public static int gw, gh;
	/**
	 * Where the area being drawn in sits on the screen (GUI pixels): 0, 0 for a whole-window screen; the device's
	 * screen's corner when drawing on it (whose drawing is moved there). Pictures drawn apart from the rest (the 3D
	 * figures) need it.
	 */
	public static int bx, by;

	// The viewer's colours.
	public static final int PANEL = 0xFF123C38, PANEL_HI = 0xFF1D5A52, EDGE = 0xFF4FB896, EDGE_LIGHT = 0xFF8FF5C0, TEXT = 0xFFF4F6FF, MUTED = 0xFFA9D4C6,
			DIM = 0xFF6B9A8E, CREAM = 0xFFFFF4D8, INK = 0xFF26304A, GOLD = 0xFFFFD45A, RED = 0xFFE5525C, AMBER = 0xFFF0B43C, GREEN = 0xFF41BF84, BLUE = 0xFF4A8FE0,
			SOFT_TEXT = 0xFFCFEDE3;

	/** Fits the canvas to a window w by h (GUI pixels). Call at the start of drawing. */
	public static void fit(int w, int h) {
		gw = w;
		gh = h;
		k = Math.min(w / W, h / H);
		ox = (w - W * k) / 2;
		oy = (h - H * k) / 2;
	}

	public static float x(float vx) {
		return ox + vx * k;
	}

	public static float y(float vy) {
		return oy + vy * k;
	}

	/** A canvas length in GUI pixels. */
	public static float s(float v) {
		return v * k;
	}

	/** Canvas coordinates of a point in the window (the mouse). */
	public static float vx(double gx) {
		return (float) ((gx - ox) / k);
	}

	public static float vy(double gy) {
		return (float) ((gy - oy) / k);
	}

	// ---------- shapes ----------

	/** A rounded rectangle on the canvas. */
	public static void rr(GuiGraphics g, float x, float y, float w, float h, float r, int c) {
		Ui.rr(g, x(x), y(y), s(w), s(h), s(r), c);
	}

	public static void rrv(GuiGraphics g, float x, float y, float w, float h, float r, int top, int bottom) {
		Ui.rrv(g, x(x), y(y), s(w), s(h), s(r), top, bottom);
	}

	public static void ring(GuiGraphics g, float x, float y, float w, float h, float r, float t, int c) {
		Ui.ring(g, x(x), y(y), s(w), s(h), s(r), s(t), c);
	}

	/** A pill: fully rounded ends. */
	public static void pill(GuiGraphics g, float x, float y, float w, float h, int c) {
		rr(g, x, y, w, h, h / 2, c);
	}

	/** A soft drop shadow under a box (the viewer's 0 14px 36px), as a few widening translucent layers. */
	public static void shadow(GuiGraphics g, float x, float y, float w, float h, float r, float strength) {
		for (int i = 4; i >= 1; i--) {
			float grow = i * 5;
			rr(g, x - grow + 1, y + 8 + grow * 0.6F, w + grow * 2 - 2, h + grow * 0.8F, r + grow, Ui.alpha(0xFF000000, strength * 0.09F));
		}
	}

	/** A panel: teal glass, light at the top, a mint rim, a faint inner line and its shadow. */
	public static void panel(GuiGraphics g, float x, float y, float w, float h) {
		panel(g, x, y, w, h, 24, false);
	}

	public static void panel(GuiGraphics g, float x, float y, float w, float h, float r, boolean soft) {
		shadow(g, x, y, w, h, r, 1);
		rr(g, x, y, w, h, r, EDGE);
		float e = 2.5F;
		if (soft) rr(g, x + e, y + e, w - 2 * e, h - 2 * e, r - e, 0xD1123C38);
		else {
			// rgba(29,90,82,.95) to rgba(18,60,56,.95) by 55%, then flat.
			float mid = (h - 2 * e) * 0.55F;
			rr(g, x + e, y + e, w - 2 * e, h - 2 * e, r - e, 0xF2123C38);
			rrv(g, x + e, y + e, w - 2 * e, mid, Math.min(r - e, mid / 2), 0xF21D5A52, 0xF2123C38);
		}
		ring(g, x + e, y + e, w - 2 * e, h - 2 * e, r - e, 1.5F, 0x2E8FF5C0);
	}

	/** A panel picked out: a cream rim and a cream glow round it. */
	public static void panelOn(GuiGraphics g, float x, float y, float w, float h, float r) {
		rr(g, x - 4, y - 4, w + 8, h + 8, r + 4, 0x66FFF4D8);
		panel(g, x, y, w, h, r, false);
		ring(g, x, y, w, h, r, 2.5F, CREAM);
	}

	/** A triangle pointing right (dir 0), down (1), left (2) or up (3), centred at cx, cy. */
	public static void tri(GuiGraphics g, float cx, float cy, float size, int dir, int c) {
		// Ui.triangle: 0 up, 1 right, 2 down, 3 left.
		int d = switch (dir) {
			case 0 -> 1;
			case 1 -> 2;
			case 2 -> 3;
			default -> 0;
		};
		Ui.triangle(g, x(cx), y(cy), s(size), d, c);
	}

	public static void ellipse(GuiGraphics g, float cx, float cy, float rx, float ry, int c) {
		Ui.ellipse(g, x(cx), y(cy), s(rx), s(ry), c);
	}

	/** A soft glow (light falling off from the middle). */
	public static void glow(GuiGraphics g, float cx, float cy, float rx, float ry, int c) {
		Ui.glow(g, Math.round(x(cx)), Math.round(y(cy)), Math.round(s(rx)), Math.round(s(ry)), c);
	}

	/** Fills the whole window (not just the canvas) with a vertical gradient. */
	public static void backdrop(GuiGraphics g, int top, int bottom) {
		g.fillGradient(0, 0, gw, gh, top, bottom);
	}

	/**
	 * Fills the whole window with a radial gradient centred at (cx, cy) on the canvas: stops are pairs of (0 to 1,
	 * colour), out to radius r (canvas pixels). Drawn as rings from the outside in.
	 */
	public static void radial(GuiGraphics g, float cx, float cy, float r, float[] at, int[] cols) {
		g.fill(0, 0, gw, gh, cols[cols.length - 1]);
		int steps = 48;
		for (int i = steps; i >= 1; i--) {
			float t = i / (float) steps;
			int c = stop(t, at, cols);
			float rad = r * t;
			Ui.ellipse(g, x(cx), y(cy), s(rad), s(rad), c);
		}
	}

	private static int stop(float t, float[] at, int[] cols) {
		if (t <= at[0]) return cols[0];
		for (int i = 1; i < at.length; i++) if (t <= at[i]) return Ui.mix(cols[i - 1], cols[i], (t - at[i - 1]) / (at[i] - at[i - 1]));
		return cols[cols.length - 1];
	}

	// ---------- pictures ----------

	public static Identifier tex(String name) {
		return Retromon.id("textures/gui/v/" + name + ".png");
	}

	/** One of the viewer's pictures (icons, badges, command pictures: 128 by 128) in a box. */
	public static void icon(GuiGraphics g, String name, float x, float y, float size) {
		img(g, tex(name), x, y, size, size, 128, 128, 0xFFFFFFFF);
	}

	public static void icon(GuiGraphics g, String name, float x, float y, float size, int tint) {
		img(g, tex(name), x, y, size, size, 128, 128, tint);
	}

	/** A whole picture (tw by th) into a box on the canvas, tinted (0xFFFFFFFF for none). */
	public static void img(GuiGraphics g, Identifier id, float x, float y, float w, float h, int tw, int th, int tint) {
		g.pose().pushMatrix();
		g.pose().translate(x(x), y(y));
		g.pose().scale(s(w) / tw, s(h) / th);
		g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, id, 0, 0, 0, 0, tw, th, tw, th, tw, th, tint);
		g.pose().popMatrix();
	}

	/** Part of a picture (u, v, uw, vh of a tw by th sheet) into a box on the canvas. */
	public static void sub(GuiGraphics g, Identifier id, float x, float y, float w, float h, int u, int v, int uw, int vh, int tw, int th) {
		g.pose().pushMatrix();
		g.pose().translate(x(x), y(y));
		g.pose().scale(s(w) / uw, s(h) / vh);
		g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, id, 0, 0, u, v, uw, vh, tw, th, 0xFFFFFFFF);
		g.pose().popMatrix();
	}

	// ---------- words ----------

	/** The font's em in GUI pixels at scale 1 (Poppins at 8.5). */
	private static final float EM = 8.5F;

	private static Font font() {
		return Minecraft.getInstance().font;
	}

	public static Component comp(String text, boolean bold) {
		return bold ? Ui.bold(text) : Ui.ui(text);
	}

	/** Words as written, with no names swapped for our creatures' ("Stands in for #025 PIKACHU"). */
	public static Component raw(String text, boolean bold) {
		return Component.literal(text).withStyle((bold ? Ui.bold("") : Ui.ui("")).getStyle());
	}

	/** Words at a canvas font size (the viewer's px), their top at y. */
	public static void text(GuiGraphics g, String text, float x, float y, float size, int color, boolean bold) {
		draw(g, comp(Look.text(text), bold), x, y, size, color, false);
	}

	public static void text(GuiGraphics g, String text, float x, float y, float size, int color, boolean bold, boolean shadow) {
		draw(g, comp(Look.text(text), bold), x, y, size, color, shadow);
	}

	public static void draw(GuiGraphics g, Component c, float x, float y, float size, int color, boolean shadow) {
		float sc = s(size) / EM;
		g.pose().pushMatrix();
		g.pose().translate(x(x), y(y) + s(size) * 0.08F);
		g.pose().scale(sc, sc);
		g.drawString(font(), c, 0, 0, color, shadow);
		g.pose().popMatrix();
	}

	/** How wide words are, in canvas pixels, at a size. */
	public static float width(String text, float size, boolean bold) {
		return font().width(comp(Look.text(text), bold)) * size / EM;
	}

	public static float width(Component c, float size) {
		return font().width(c) * size / EM;
	}

	public static void centred(GuiGraphics g, String text, float cx, float y, float size, int color, boolean bold) {
		text(g, text, cx - width(text, size, bold) / 2, y, size, color, bold);
	}

	public static void right(GuiGraphics g, String text, float rx, float y, float size, int color, boolean bold) {
		text(g, text, rx - width(text, size, bold), y, size, color, bold);
	}

	/** A small capital label (the viewer's g-label: 13px, spaced, muted). */
	public static void label(GuiGraphics g, String text, float x, float y, int color) {
		text(g, spaced(text.toUpperCase()), x, y, 13, color, true);
	}

	public static void label(GuiGraphics g, String text, float x, float y) {
		label(g, text, x, y, MUTED);
	}

	public static float labelWidth(String text) {
		return width(spaced(text.toUpperCase()), 13, true);
	}

	/** Letter spacing, roughly: a hair space between letters. */
	private static String spaced(String s) {
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			b.append(s.charAt(i));
			if (i < s.length() - 1 && s.charAt(i) != ' ') b.append(' ');
		}
		return b.toString();
	}

	/**
	 * A line with its names picked out in gold (the viewer's bold words): runs between ** marks, or, in the games'
	 * own lines, words in capitals (names, places, things).
	 */
	public static Component rich(String text, boolean bold, int color) {
		text = Look.text(text);
		MutableComponent out = Component.empty();
		Style base = (bold ? Ui.bold("") : Ui.ui("")).getStyle();
		if (text.contains("**")) {
			String[] parts = text.split("\\*\\*", -1);
			for (int i = 0; i < parts.length; i++)
				out.append(Component.literal(parts[i]).withStyle(base.withColor(i % 2 == 1 ? GOLD & 0xFFFFFF : color & 0xFFFFFF)));
			return out;
		}
		StringBuilder run = new StringBuilder();
		boolean gold = false;
		int i = 0;
		while (i < text.length()) {
			int j = i;
			while (j < text.length() && !Character.isWhitespace(text.charAt(j))) j++;
			String word = text.substring(i, j);
			boolean caps = caps(word);
			if (caps != gold && run.length() > 0) {
				out.append(Component.literal(run.toString()).withStyle(base.withColor((gold ? GOLD : color) & 0xFFFFFF)));
				run.setLength(0);
			}
			gold = caps;
			run.append(word);
			int e = j;
			while (e < text.length() && Character.isWhitespace(text.charAt(e))) e++;
			run.append(text, j, e);
			i = e;
		}
		if (run.length() > 0) out.append(Component.literal(run.toString()).withStyle(base.withColor((gold ? GOLD : color) & 0xFFFFFF)));
		return out;
	}

	/** A word in capitals (two or more letters, all upper case: OAK, POKéMON, ROUTE 1's ROUTE), not "I" or "A". */
	private static boolean caps(String w) {
		int letters = 0, upper = 0;
		for (int i = 0; i < w.length(); i++) {
			char c = w.charAt(i);
			if (Character.isLetter(c)) {
				letters++;
				if (Character.isUpperCase(c) || c == 'é') upper++;
			}
		}
		return letters >= 2 && upper == letters;
	}

	/** Splits words into lines no wider than w (canvas pixels) at a size. */
	public static List<String> wrap(String text, float w, float size, boolean bold) {
		return Ui.wrap(font(), Look.text(text), Math.max(1, (int) (w * EM / size)));
	}

	// ---------- the viewer's pieces ----------

	/** A key cap (the viewer's g-key): cream, dark letters; round for one letter. Returns its width. */
	public static float key(GuiGraphics g, String k, float x, float y, float size) {
		float tw = width(k, 14 * size / 28, true), w = Math.max(size, tw + 14 * size / 28);
		boolean round = k.length() == 1;
		rr(g, x, y + 2 * size / 28, w, size, round ? size / 2 : 9 * size / 28, 0x40000000);
		rr(g, x, y, w, size, round ? size / 2 : 9 * size / 28, CREAM);
		text(g, k, x + (w - tw) / 2, y + (size - 14 * size / 28 * 1.1F) / 2, 14 * size / 28, INK, true);
		return w;
	}

	/** The keys along the bottom right (the viewer's g-hints): A shown as SPACE and B as ESC, as on a keyboard. */
	public static void hints(GuiGraphics g, String... pairs) {
		float x = W - 22, y = H - 14 - 28;
		for (int i = pairs.length - 2; i >= 0; i -= 2) {
			String kk = keyName(pairs[i]), l = pairs[i + 1];
			float lw = width(l, 17, true);
			x -= lw;
			text(g, l, x, y + 5, 17, TEXT, true, true);
			float kw = Math.max(28, width(kk, 14, true) + 14);
			x -= 7 + kw;
			key(g, kk, x, y, 28);
			x -= 18;
		}
	}

	public static String keyName(String k) {
		return switch (k) {
			case "A" -> "SPACE";
			case "B" -> "ESC";
			default -> k;
		};
	}

	/** A chip (the viewer's g-chip): an outlined pill, cream when on. Returns its width. */
	public static float chip(GuiGraphics g, String text, float x, float y, float size, boolean on, boolean sel) {
		float tw = width(text, size, true), w = tw + size * 1.65F, h = size * 1.75F;
		if (on) pill(g, x, y, w, h, CREAM);
		else {
			Ui.ring(g, x(x), y(y), s(w), s(h), s(h / 2), s(2), sel ? EDGE_LIGHT : 0x598FF5C0);
		}
		text(g, text, x + (w - tw) / 2, y + (h - size * 1.1F) / 2, size, on ? INK : TEXT, true);
		return w;
	}

	public static float chipWidth(String text, float size) {
		return width(text, size, true) + size * 1.65F;
	}

	/** A type's chip (the viewer's g-type): its colour, white capitals. */
	public static float type(GuiGraphics g, String type, int colour, float x, float y, float minW, float size) {
		String t = type.toUpperCase();
		float tw = width(t, size, true), w = Math.max(minW, tw + size * 1.8F), h = size * 1.75F;
		pill(g, x, y, w, h, colour);
		text(g, t, x + (w - tw) / 2, y + (h - size * 1.1F) / 2, size, 0xFFFFFFFF, true, true);
		return w;
	}

	/** A row in a list (the viewer's g-item): cream with a dark arrow when picked. */
	public static void item(GuiGraphics g, String text, String right, float x, float y, float w, float h, boolean on, boolean off, float size) {
		if (on) {
			pill(g, x - 3, y - 3, w + 6, h + 6, 0x59FFF4D8);
			pill(g, x, y + 4, w, h, 0x30000000);
			pill(g, x, y, w, h, CREAM);
			tri(g, x + 21, y + h / 2, 12, 0, INK);
		}
		int c = on ? INK : off ? DIM : TEXT;
		draw(g, rich(text, true, c), x + 40, y + (h - size * 1.15F) / 2, size, c, false);
		if (right != null && !right.isEmpty()) right(g, right, x + w - 22, y + (h - size * 1.15F) / 2, size, c, true);
	}

	/** A colour swatch (round, a white rim; cream and bigger when picked). */
	public static void swatch(GuiGraphics g, int colour, float x, float y, float size, boolean on, float radius) {
		if (on) rr(g, x - 4, y - 4, size + 8, size + 8, radius + 4, 0x73FFF4D8);
		rr(g, x, y, size, size, radius, on ? CREAM : 0x59FFFFFF);
		rrv(g, x + 3, y + 3, size - 6, size - 6, Math.max(0, radius - 3), Ui.mix(colour | 0xFF000000, 0xFFFFFFFF, 0.25F), Ui.mix(colour | 0xFF000000, 0xFF000000, 0.12F));
	}

	/** A bar (HP, EXP): a dark track, the fill rounded. */
	public static void bar(GuiGraphics g, float x, float y, float w, float h, float frac, int colour) {
		pill(g, x, y, w, h, 0xFF0B2A2C);
		frac = Math.max(0, Math.min(1, frac));
		if (frac > 0) pill(g, x + 2, y + 2, Math.max(h - 4, (w - 4) * frac), h - 4, colour);
	}

	public static int hpColour(float frac) {
		return frac > 0.5F ? 0xFF4CD27E : frac > 0.2F ? 0xFFF2C744 : 0xFFEF4F4F;
	}

	/** The rounded header pill at the top left of a menu page: its picture and its name. */
	public static void header(GuiGraphics g, String icon, String text, String right) {
		float tw = width(text, 28, true), rw = right == null ? 0 : labelWidth(right) + 18, w = 12 + 42 + 12 + tw + 22 + rw;
		panel(g, 28, 22, w, 58, 29, true);
		if (icon != null) icon(g, icon, 28 + 12 + 1, 22 + 9, 40);
		text(g, text, 28 + 66, 22 + 13, 28, TEXT, true);
		if (right != null) label(g, right, 28 + 66 + tw + 18, 22 + 24);
	}

	// ---------- the text box ----------

	/**
	 * The text box (the viewer's g-text): a panel along the bottom (x, its bottom edge, width), the speaker on a
	 * cream tab at its top left, the words in white with names in gold, and the mint arrow bobbing when there's more.
	 * lines: what's typed out so far, one entry a line.
	 */
	public static void textBox(GuiGraphics g, float x, float bottom, float w, String who, List<String> lines, boolean more, float secs) {
		float size = 25, lh = size * 1.45F, h = Math.max(120, 22 + 24 + lines.size() * lh), y = H - bottom - h;
		panel(g, x, y, w, h);
		if (who != null && !who.isEmpty()) {
			float tw = width(who, 21, true);
			pill(g, x + 28, y - 20 + 3, tw + 36, 36, 0x40000000);
			pill(g, x + 28, y - 20, tw + 36, 36, CREAM);
			text(g, who, x + 28 + 18, y - 20 + 7, 21, INK, true);
		}
		for (int i = 0; i < lines.size(); i++) draw(g, rich(lines.get(i), true, TEXT), x + 34, y + 22 + i * lh + (lh - size) / 2 - 2, size, TEXT, false);
		if (more) {
			float bob = (float) Math.sin(secs * Math.PI / 0.45F) > 0 ? 4 : 0;
			tri(g, x + w - 26 - 10, y + h - 16 - 13 / 2F + bob, 13, 1, EDGE_LIGHT);
		}
	}

	/** A list in a panel (the viewer's choice box): rows of g-items. Returns its height. */
	public static float choice(GuiGraphics g, List<String> items, List<String> rights, int sel, float x, float y, float w) {
		float pad = 12, rh = 46, gap = 8, h = pad * 2 + items.size() * rh + (items.size() - 1) * gap;
		panel(g, x, y, w, h);
		for (int i = 0; i < items.size(); i++)
			item(g, items.get(i), rights == null ? null : rights.get(i), x + pad, y + pad + i * (rh + gap), w - pad * 2, rh, i == sel, false, 21);
		return h;
	}

	public static float choiceHeight(int n) {
		return 24 + n * 46 + Math.max(0, n - 1) * 8;
	}

	/** Which row of a choice box the mouse is over (canvas coordinates), or -1. */
	public static int choiceAt(int n, float x, float y, float w, double mx, double my) {
		float px = vx(mx), py = vy(my);
		if (px < x || px > x + w) return -1;
		for (int i = 0; i < n; i++) {
			float ry = y + 12 + i * 54;
			if (py >= ry && py <= ry + 46) return i;
		}
		return -1;
	}

	/**
	 * The viewer's callout ("Congratulations!", "Gotcha!"): big gold letters edged in brown, tilted a little, popping
	 * in and floating up as they fade (1.4 seconds). secs: since it came up.
	 */
	public static void callout(GuiGraphics g, String text, float cx, float cy, float secs) {
		if (secs < 0 || secs > 1.4F) return;
		float p = secs / 1.4F, sc, a = 1, lift = 0, size = 64, tw = width(text, size, true), th = size * 1.2F;
		if (p < 0.15F) {
			sc = 0.4F + 0.75F * (p / 0.15F);
			a = p / 0.15F;
		} else if (p < 0.25F) sc = 1.15F - 0.15F * ((p - 0.15F) / 0.1F);
		else sc = 1;
		if (p > 0.8F) {
			a = 1 - (p - 0.8F) / 0.2F;
			lift = th * 0.2F * (p - 0.8F) / 0.2F;
		}
		float px = x(cx), py = y(cy - lift);
		g.pose().pushMatrix();
		g.pose().translate(px, py);
		g.pose().rotate((float) Math.toRadians(-6));
		g.pose().scale(sc, sc);
		g.pose().translate(-px, -py);
		float x0 = cx - tw / 2, y0 = cy - lift - size * 0.55F;
		text(g, text, x0, y0 + 6, size, Ui.alpha(0x40000000, a), true);
		int edge = Ui.alpha(0xFF7A4A00, a);
		for (int i = 0; i < 8; i++) {
			double an = i * Math.PI / 4;
			text(g, text, x0 + (float) Math.cos(an) * 3, y0 + (float) Math.sin(an) * 3, size, edge, true);
		}
		text(g, text, x0, y0, size, Ui.alpha(0xFFFFE07A, a), true);
		g.pose().popMatrix();
	}

	/** Whether the mouse is in a box on the canvas. */
	public static boolean over(double mx, double my, float x, float y, float w, float h) {
		float px = vx(mx), py = vy(my);
		return px >= x && px <= x + w && py >= y && py <= y + h;
	}
}
