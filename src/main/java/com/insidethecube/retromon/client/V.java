package com.insidethecube.retromon.client;

import java.util.ArrayList;
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
 * in the viewer. Its look is the one John signed off (20 Nov), after Let's Go: white panels with a pastel, pearly
 * rim and a soft navy shadow; dark ink words in Nunito Sans; titles in Fredoka; names picked out in coral; the one
 * you're on in sunny yellow; keys in their colours; every screen on its own bright backdrop, filling the window.
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

	// The viewer's colours (head.html's --g-...): white panels, ink words, coral names, sunny yellow picks.
	public static final int PANEL = 0xFFFFFFFF, PANEL_HI = 0xFFF4F7FF, EDGE = 0xFFD9DFF0, EDGE_LIGHT = 0xFFF07850, TEXT = 0xFF26304A, MUTED = 0xFF6F7A99,
			DIM = 0xFFA7AFC6, CREAM = 0xFFFFD34E, INK = 0xFF26304A, GOLD = 0xFFE5583E, RED = 0xFFE5525C, AMBER = 0xFFF0B43C, GREEN = 0xFF41BF84, BLUE = 0xFF4A8FE0,
			SOFT_TEXT = 0xFF6F7A99, WHITE = 0xFFFFFFFF, TRACK = 0xFFDDE3F0;
	/** What's picked: the viewer's --g-sel, sunny yellow lit from the top. */
	public static final int SEL_TOP = 0xFFFFEC8A, SEL_BOTTOM = 0xFFFFC93C;
	/** The pearly rim round every panel (--g-holo, pink to lilac to sky to mint to butter). */
	public static final float[] HOLO_AT = {0, 0.26F, 0.5F, 0.74F, 1};
	public static final int[] HOLO = {0xFFFFB8D2, 0xFFD4BEFF, 0xFFA9DCFF, 0xFFA8F0D2, 0xFFFFE7A1};
	/** The text box's sheen along to its right end. */
	private static final float[] SHEEN_AT = {0, 0.6F, 0.78F, 0.9F, 1};
	private static final int[] SHEEN = {0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFF1F8, 0xFFECF4FF, 0xFFEEFFF7};

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

	public static void rrStops(GuiGraphics g, float x, float y, float w, float h, float r, float[] at, int[] cols) {
		Ui.rrStops(g, x(x), y(y), s(w), s(h), s(r), at, cols);
	}

	public static void ring(GuiGraphics g, float x, float y, float w, float h, float r, float t, int c) {
		Ui.ring(g, x(x), y(y), s(w), s(h), s(r), s(t), c);
	}

	/** A pill: fully rounded ends. */
	public static void pill(GuiGraphics g, float x, float y, float w, float h, int c) {
		rr(g, x, y, w, h, h / 2, c);
	}

	/** A soft drop shadow under a box (the viewer's 0 12px 30px rgba(30,40,84,.22)), strength times as dark. */
	public static void shadow(GuiGraphics g, float x, float y, float w, float h, float r, float strength) {
		softShadow(g, x, y + 12, w, h, 30, Ui.alpha(0xFF1E2854, Math.min(1, 0.22F * strength)));
	}

	/**
	 * A shadow as CSS's box-shadow draws one: the box (canvas pixels) blurred by blur, in colour (its see-through how
	 * dark). From one blurred picture (textures/gui/shape/shadow.png: a 128 square blurred in a 256 sheet), its corners
	 * kept and its sides and middle stretched, so it's smooth at any size.
	 */
	public static void softShadow(GuiGraphics g, float x, float y, float w, float h, float blur, int colour) {
		if (w <= 0 || h <= 0 || (colour >>> 24) == 0) return;
		float m = blur * 2, X = x - m, Y = y - m, W = w + 2 * m, H = h + 2 * m;
		float c = 1.5F * m, cw = Math.min(c, W / 2), ch = Math.min(c, H / 2);
		float uc = 96F / 256 * cw / c, vc = 96F / 256 * ch / c, um = 128F / 256;
		float[] xs = {X, X + cw, X + W - cw, X + W}, ys = {Y, Y + ch, Y + H - ch, Y + H};
		float[] us = {0, uc, um, um, 1 - uc, 1}, vs = {0, vc, um, um, 1 - vc, 1};
		for (int j = 0; j < 3; j++) {
			for (int i = 0; i < 3; i++) {
				float x0 = xs[i], x1 = xs[i + 1], y0 = ys[j], y1 = ys[j + 1];
				if (x1 - x0 < 0.01F || y1 - y0 < 0.01F) continue;
				float u0 = i == 0 ? 0 : i == 1 ? um : 1 - uc, u1 = i == 0 ? uc : i == 1 ? um : 1;
				float v0 = j == 0 ? 0 : j == 1 ? um : 1 - vc, v1 = j == 0 ? vc : j == 1 ? um : 1;
				Ui.blitSmooth(g, Ui.SHADOW, x(x0), y(y0), x(x1), y(y1), u0, u1, v0, v1, colour);
			}
		}
	}

	/** Shaded left to right through stops, on the canvas. */
	public static void rrh(GuiGraphics g, float x, float y, float w, float h, float r, float[] at, int[] cols) {
		Ui.rrh(g, x(x), y(y), s(w), s(h), s(r), at, cols);
	}

	/** The sunny yellow of what's picked (--g-sel), as a rounded box. */
	public static void sel(GuiGraphics g, float x, float y, float w, float h, float r) {
		rrv(g, x, y, w, h, r, SEL_TOP, SEL_BOTTOM);
	}

	/** The pearly rim: a rounded box in the holo colours, its inside drawn over it t in. */
	public static void holo(GuiGraphics g, float x, float y, float w, float h, float r) {
		rrh(g, x, y, w, h, r, HOLO_AT, HOLO);
	}

	/** A panel (the viewer's g-panel): its shadow, the pearly rim (3px), white shading to #F7F9FF, a faint line inside its foot. */
	public static void panel(GuiGraphics g, float x, float y, float w, float h) {
		panel(g, x, y, w, h, 22, false);
	}

	public static void panel(GuiGraphics g, float x, float y, float w, float h, float r, boolean soft) {
		shadow(g, x, y, w, h, r, 1);
		holo(g, x, y, w, h, r);
		float e = 3;
		if (soft) rr(g, x + e, y + e, w - 2 * e, h - 2 * e, Math.max(0, r - e), 0xEBFFFFFF);
		else rrv(g, x + e, y + e, w - 2 * e, h - 2 * e, Math.max(0, r - e), 0xFFFFFFFF, 0xFFF7F9FF);
		if (h > 24) rr(g, x + e + r * 0.5F, y + h - e - 3, w - 2 * e - r, 3, 1.5F, 0x1F96A0CD);
	}

	/** A panel picked out: a sunny rim and a warm glow round it. */
	public static void panelOn(GuiGraphics g, float x, float y, float w, float h, float r) {
		rr(g, x - 5, y - 5, w + 10, h + 10, r + 5, 0x59FFC93C);
		panel(g, x, y, w, h, r, false);
		ring(g, x, y, w, h, r, 3, 0xFFFFC93C);
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

	/** A ring (canvas): from radius r0 out to r1, shaded c0 at the inside to c1 at the outside. */
	public static void annulus(GuiGraphics g, float cx, float cy, float r0, float r1, int c0, int c1) {
		int n = 64;
		float[] xy = new float[n * 12];
		int[] cs = new int[n * 6];
		float X = x(cx), Y = y(cy), R0 = s(r0), R1 = s(r1);
		for (int i = 0; i < n; i++) {
			double b0 = i * Math.PI * 2 / n, b1 = (i + 1) * Math.PI * 2 / n;
			float c0x = (float) Math.cos(b0), s0 = (float) Math.sin(b0), c1x = (float) Math.cos(b1), s1 = (float) Math.sin(b1);
			float[] q = {X + c0x * R0, Y + s0 * R0, X + c0x * R1, Y + s0 * R1, X + c1x * R1, Y + s1 * R1, X + c0x * R0, Y + s0 * R0, X + c1x * R1, Y + s1 * R1, X + c1x * R0, Y + s1 * R0};
			System.arraycopy(q, 0, xy, i * 12, 12);
			int[] k = {c0, c1, c1, c0, c1, c0};
			System.arraycopy(k, 0, cs, i * 6, 6);
		}
		Ui.tris(g, xy, cs);
	}

	/**
	 * A light behind something: radial stops (at, 0 to 1 of radius r) of colours with their own see-through, drawn as
	 * rings so the see-through doesn't pile up.
	 */
	public static void radialSoft(GuiGraphics g, float cx, float cy, float r, float[] at, int[] cols) {
		int steps = 24;
		for (int i = 0; i < steps; i++) {
			float t0 = i / (float) steps, t1 = (i + 1) / (float) steps;
			annulus(g, cx, cy, r * t0, r * t1, stop(t0, at, cols), stop(t1, at, cols));
		}
	}

	/**
	 * Rays (the viewer's repeating-conic-gradient behind a won thing): wedges of on degrees every every degrees,
	 * turned by turn (radians), solid out to r0 and fading to nothing at r1 (its mask).
	 */
	public static void rays(GuiGraphics g, float cx, float cy, float r0, float r1, float on, float every, double turn, int col) {
		List<Float> xy = new ArrayList<>();
		List<Integer> cs = new ArrayList<>();
		float X = x(cx), Y = y(cy), R0 = s(r0), R1 = s(r1);
		int clear = col & 0x00FFFFFF;
		for (float a = 0; a < 360; a += every) {
			double a0 = Math.toRadians(a - 90) + turn, a1 = Math.toRadians(Math.min(a + on, 360) - 90) + turn;
			float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0), c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
			float[] q = {X, Y, X + c0 * R0, Y + s0 * R0, X + c1 * R0, Y + s1 * R0, X + c0 * R0, Y + s0 * R0, X + c0 * R1, Y + s0 * R1, X + c1 * R1, Y + s1 * R1, X + c0 * R0,
					Y + s0 * R0, X + c1 * R1, Y + s1 * R1, X + c1 * R0, Y + s1 * R0};
			int[] k = {col, col, col, col, clear, clear, col, clear, col};
			for (float v : q) xy.add(v);
			for (int v : k) cs.add(v);
		}
		float[] a = new float[xy.size()];
		for (int i = 0; i < a.length; i++) a[i] = xy.get(i);
		int[] c = new int[cs.size()];
		for (int i = 0; i < c.length; i++) c[i] = cs.get(i);
		Ui.tris(g, a, c);
	}

	/** CSS's cubic-bezier(x1, y1, x2, y2) at t (0 to 1). */
	public static float bezier(float x1, float y1, float x2, float y2, float t) {
		if (t <= 0) return 0;
		if (t >= 1) return 1;
		double u = t;
		for (int i = 0; i < 8; i++) {
			double x = 3 * (1 - u) * (1 - u) * u * x1 + 3 * (1 - u) * u * u * x2 + u * u * u - t;
			double dx = 3 * (1 - u) * (1 - u) * x1 + 6 * (1 - u) * u * (x2 - x1) + 3 * u * u * (1 - x2);
			if (Math.abs(dx) < 1e-6) break;
			u = Math.max(0, Math.min(1, u - x / dx));
		}
		return (float) (3 * (1 - u) * (1 - u) * u * y1 + 3 * (1 - u) * u * u * y2 + u * u * u);
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

	/** The font's em in GUI pixels at scale 1 (Nunito Sans and Fredoka at 8.5). */
	private static final float EM = 8.5F;

	private static Font font() {
		return Minecraft.getInstance().font;
	}

	public static Component comp(String text, boolean bold) {
		return bold ? Ui.bold(text) : Ui.ui(text);
	}

	private static final Style TITLE = Style.EMPTY.withFont(new net.minecraft.network.chat.FontDescription.Resource(Retromon.id("title")));

	/** Words in the titles' font (the viewer's g-title: Fredoka 600). */
	public static Component titleComp(String text) {
		return Component.literal(Look.text(text)).withStyle(TITLE);
	}

	/** A title (g-title) at a size, its top at y. */
	public static void title(GuiGraphics g, String text, float x, float y, float size, int color) {
		draw(g, titleComp(text), x, y, size, color, false);
	}

	public static float titleWidth(String text, float size) {
		return width(titleComp(text), size);
	}

	public static void titleCentred(GuiGraphics g, String text, float cx, float y, float size, int color) {
		title(g, text, cx - titleWidth(text, size) / 2, y, size, color);
	}

	/** Words with a soft dark shadow under them (white words on a bright backdrop: text-shadow 0 2px 4px). */
	public static void soft(GuiGraphics g, Component c, float x, float y, float size, int color) {
		draw(g, c.copy(), x + 0.6F, y + 2, size, 0x47000000, false);
		draw(g, c, x, y, size, color, false);
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

	/** Where to put words (their top, as text takes it) so their capitals sit centred on cy. */
	public static float capsTop(float cy, float size) {
		return cy - size * 0.6F;
	}

	public static void centred(GuiGraphics g, String text, float cx, float y, float size, int color, boolean bold) {
		text(g, text, cx - width(text, size, bold) / 2, y, size, color, bold);
	}

	public static void right(GuiGraphics g, String text, float rx, float y, float size, int color, boolean bold) {
		text(g, text, rx - width(text, size, bold), y, size, color, bold);
	}

	/** A small capital label (the viewer's g-label: 13px, spaced, muted). */
	/** Words with their letters spaced apart (letter-spacing, in canvas pixels). */
	public static void tracked(GuiGraphics g, String text, float x, float y, float size, int color, float spacing) {
		String t = Look.text(text);
		float cx = x;
		for (int i = 0; i < t.length(); i++) {
			String ch = t.substring(i, i + 1);
			draw(g, comp(ch, true), cx, y, size, color, false);
			cx += width(ch, size, true) + spacing;
		}
	}

	public static float trackedWidth(String text, float size, float spacing) {
		String t = Look.text(text);
		float w = 0;
		for (int i = 0; i < t.length(); i++) w += width(t.substring(i, i + 1), size, true) + spacing;
		return Math.max(0, w - spacing);
	}

	/** The title's other fonts: Archivo (700) for its small spaced words. */
	public static final Style ARCHIVO = Style.EMPTY.withFont(new net.minecraft.network.chat.FontDescription.Resource(Retromon.id("archivo")));

	/** Words in a font of their own (style), their letters spaced apart. */
	public static void trackedIn(GuiGraphics g, Style font, String text, float x, float y, float size, int color, float spacing) {
		String t = Look.text(text);
		float cx = x;
		for (int i = 0; i < t.length(); i++) {
			Component ch = Component.literal(t.substring(i, i + 1)).withStyle(font);
			draw(g, ch, cx, y, size, color, false);
			cx += width(ch, size) + spacing;
		}
	}

	public static float trackedWidthIn(Style font, String text, float size, float spacing) {
		String t = Look.text(text);
		float w = 0;
		for (int i = 0; i < t.length(); i++) w += width(Component.literal(t.substring(i, i + 1)).withStyle(font), size) + spacing;
		return Math.max(0, w - spacing);
	}

	/** Title-font words with their letters spaced apart. */
	public static void titleTracked(GuiGraphics g, String text, float x, float y, float size, int color, float spacing) {
		trackedIn(g, TITLE, text, x, y, size, color, spacing);
	}

	public static float titleTrackedWidth(String text, float size, float spacing) {
		return trackedWidthIn(TITLE, text, size, spacing);
	}

	/** A filled convex shape on the canvas (corners x, y, x, y...). */
	public static void poly(GuiGraphics g, float[] pts, int colour) {
		float[] q = new float[pts.length];
		for (int i = 0; i < pts.length; i += 2) {
			q[i] = x(pts[i]);
			q[i + 1] = y(pts[i + 1]);
		}
		Ui.poly(g, q, colour);
	}

	/** A line on the canvas, w thick. */
	public static void line(GuiGraphics g, float x0, float y0, float x1, float y1, float w, int colour) {
		float dx = x1 - x0, dy = y1 - y0, l = (float) Math.sqrt(dx * dx + dy * dy);
		if (l <= 0) return;
		float nx = -dy / l * w / 2, ny = dx / l * w / 2;
		poly(g, new float[] {x0 + nx, y0 + ny, x1 + nx, y1 + ny, x1 - nx, y1 - ny, x0 - nx, y0 - ny}, colour);
	}

	/** Words with an edge round them (the viewer's paint-order stroke), in a component's own font. */
	public static void stroked(GuiGraphics g, Component c, float x, float y, float size, int fill, int edge, float width) {
		for (int i = 0; i < 12; i++) {
			double an = i * Math.PI / 6;
			draw(g, c, x + (float) Math.cos(an) * width / 2, y + (float) Math.sin(an) * width / 2, size, edge, false);
		}
		draw(g, c, x, y, size, fill, false);
	}

	/** A label (the viewer's g-label): 13px capitals, 1.4px apart. */
	public static void label(GuiGraphics g, String text, float x, float y, int color) {
		String t = Look.text(text.toUpperCase());
		float cx = x;
		for (int i = 0; i < t.length(); i++) {
			String ch = t.substring(i, i + 1);
			draw(g, comp(ch, true), cx, y, 13, color, false);
			cx += width(ch, 13, true) + 1.4F;
		}
	}

	public static void label(GuiGraphics g, String text, float x, float y) {
		label(g, text, x, y, MUTED);
	}

	public static float labelWidth(String text) {
		String t = Look.text(text.toUpperCase());
		float w = 0;
		for (int i = 0; i < t.length(); i++) w += width(t.substring(i, i + 1), 13, true) + 1.4F;
		return Math.max(0, w - 1.4F);
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
		return richIn(text, bold, color, GOLD);
	}

	/** As rich, with the names in a colour of its own (gold on the professor's dark band). */
	public static Component richIn(String text, boolean bold, int color, int nameColour) {
		final int GOLD = nameColour;
		text = Look.text(Lang.t(text));
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

	/** A word in capitals (two or more letters, all upper case: OAK, POKéMON), not "I" or "A". */
	public static boolean capsWord(String w) {
		return caps(w);
	}

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

	/** A key's colours (the viewer's g-key: SPACE green, ESC red, TAB blue, X amber, the rest ink). */
	private static int[] keyCols(String k) {
		return switch (k) {
			case "SPACE", "A" -> new int[] {0xFF5BD69A, 0xFF2FA86C};
			case "ESC", "B" -> new int[] {0xFFFF8A7A, 0xFFE5525C};
			case "TAB" -> new int[] {0xFF7CB8F2, 0xFF4A8FE0};
			case "X" -> new int[] {0xFFFFD27A, 0xFFF0A030};
			default -> new int[] {INK, INK};
		};
	}

	/** A key cap (the viewer's g-key): a pill in its key's colour, white capitals; round for one letter. Returns its width. */
	public static float key(GuiGraphics g, String k, float x, float y, float size) {
		float fs = 13 * size / 30, tw = width(k, fs, true), w = k.length() == 1 ? size : Math.max(size, tw + 18 * size / 30);
		int[] c = keyCols(k);
		rrv(g, x, y, w, size, size / 2, c[0], c[1]);
		text(g, k, x + (w - tw) / 2, capsTop(y + size / 2, fs), fs, 0xFFFFFFFF, true);
		return w;
	}

	public static float keyWidth(String k, float size) {
		float fs = 13 * size / 30;
		return k.length() == 1 ? size : Math.max(size, width(k, fs, true) + 18 * size / 30);
	}

	/** A key hint drawn this frame (its box on the canvas and its key), so it can be clicked like a button. */
	public record Hint(float x, float y, float w, float h, String key) {}

	public static final List<Hint> HINTS = new java.util.ArrayList<>();

	/**
	 * The keys along the bottom right (the viewer's g-hints): A shown as SPACE and B as ESC, as on a keyboard. Each
	 * is also a button: clicking it does what the key does.
	 */
	public static void hints(GuiGraphics g, String... pairs) {
		hintsAt(g, false, pairs);
	}

	/** The key buttons along the bottom, at the left instead (the viewer's hints(list, {left: true})). */
	public static void hintsLeft(GuiGraphics g, String... pairs) {
		hintsAt(g, true, pairs);
	}

	private static void hintsAt(GuiGraphics g, boolean left, String... pairs) {
		// Each: a white pill (5px in, 16px at the right), the key (30), 8px, its words (17px); 10px apart.
		int n = pairs.length / 2;
		float[] ws = new float[n];
		float total = 0;
		for (int i = 0; i < n; i++) {
			String kk = keyName(pairs[i * 2]), l = Lang.t(pairs[i * 2 + 1]);
			ws[i] = 5 + keyWidth(kk, 30) + 8 + width(l, 17, false) + 16;
			total += ws[i] + (i > 0 ? 10 : 0);
		}
		float x = left ? 22 : W - 22 - total, y = H - 14 - 40;
		for (int i = 0; i < n; i++) {
			String kk = keyName(pairs[i * 2]), l = Lang.t(pairs[i * 2 + 1]);
			pill(g, x - 2, y - 2, ws[i] + 4, 44, 0x99FFFFFF);
			softShadow(g, x, y + 4, ws[i], 40, 12, 0x381E2854);
			pill(g, x, y, ws[i], 40, 0xF2FFFFFF);
			float kw = key(g, kk, x + 5, y + 5, 30);
			text(g, l, x + 5 + kw + 8, capsTop(y + 20, 17), 17, INK, false);
			HINTS.add(new Hint(x, y, ws[i], 40, pairs[i * 2]));
			x += ws[i] + 10;
		}
	}

	/** A big button drawn this frame (Continue, Back), so it can be clicked. */
	public static final List<Hint> ACTS = new java.util.ArrayList<>();

	/**
	 * The big buttons on the screens you step through (the viewer's g-acts): [B Back] and [A Continue], centred at
	 * the foot of the screen (bottom 22), 62 tall, at least 230 wide; Continue green with white words. on: which is
	 * picked or under the mouse (lifted). pairs: key, words, key, words...
	 */
	public static void acts(GuiGraphics g, float bottom, int on, String... pairs) {
		int n = pairs.length / 2;
		float[] ws = new float[n];
		float total = 0;
		for (int i = 0; i < n; i++) {
			ws[i] = Math.max(230, 10 + 40 + 12 + titleWidth(Lang.t(pairs[i * 2 + 1]), 24) + 12 + 16 + 26);
			total += ws[i] + (i > 0 ? 18 : 0);
		}
		float x = (W - total) / 2;
		for (int i = 0; i < n; i++) {
			act(g, x, H - bottom - 62, ws[i], pairs[i * 2], Lang.t(pairs[i * 2 + 1]), i == on);
			x += ws[i] + 18;
		}
	}

	/** The big buttons at the right (the viewer's actions(..., {right: true})): right px in from the right edge. */
	public static void actsRight(GuiGraphics g, float bottom, float right, int on, String... pairs) {
		int n = pairs.length / 2;
		float[] ws = new float[n];
		float total = 0;
		for (int i = 0; i < n; i++) {
			ws[i] = Math.max(230, 10 + 40 + 12 + titleWidth(Lang.t(pairs[i * 2 + 1]), 24) + 12 + 16 + 26);
			total += ws[i] + (i > 0 ? 18 : 0);
		}
		float x = W - right - total;
		for (int i = 0; i < n; i++) {
			act(g, x, H - bottom - 62, ws[i], pairs[i * 2], Lang.t(pairs[i * 2 + 1]), i == on);
			x += ws[i] + 18;
		}
	}

	/** One big button at x, y (62 tall). */
	public static void act(GuiGraphics g, float x, float y, float w, String key, String words, boolean on) {
		boolean go = "A".equals(key);
		float lift = on ? 3 : 0, hh = 62;
		y -= lift;
		pill(g, x, y + 6 + lift, w, hh, 0x2E1E2854);
		softShadow(g, x, y + 10 + lift, w, hh, 24, 0x381E2854);
		pill(g, x, y, w, hh, 0xFFFFFFFF);
		if (go) rrv(g, x + 3, y + 3, w - 6, hh - 6, (hh - 6) / 2, 0xFF6BE0A6, 0xFF2FA86C);
		else rrv(g, x + 3, y + 3, w - 6, hh - 6, (hh - 6) / 2, 0xFFFFFFFF, 0xFFEEF2FB);
		// The key: 40 round, white see-through on Continue, red on Back.
		if (go) ellipse(g, x + 10 + 20, y + hh / 2, 20, 20, 0x47FFFFFF);
		else rrv(g, x + 10, y + hh / 2 - 20, 40, 40, 20, 0xFFFF8A7A, 0xFFE5525C);
		String kk = go ? "A" : "B";
		float kw = width(kk, 13, true);
		text(g, kk, x + 30 - kw / 2, capsTop(y + hh / 2, 13), 13, 0xFFFFFFFF, true);
		int c = go ? 0xFFFFFFFF : INK;
		if (go) title(g, words, x + 62, capsTop(y + hh / 2, 24) + 2, 24, 0x2E000000);
		title(g, words, x + 62, capsTop(y + hh / 2, 24), 24, c);
		tri(g, x + w - 26 - 7, y + hh / 2, 14, go ? 0 : 2, Ui.alpha(c, 0.85F));
		ACTS.add(new Hint(x, y, w, hh, key));
	}

	public static String keyName(String k) {
		return switch (k) {
			case "A" -> "SPACE";
			case "B" -> "ESC";
			default -> k;
		};
	}

	/** A chip (the viewer's g-chip): a white pill with a pale rim; sunny yellow when on; a red rim when you're on it. Returns its width. */
	public static float chip(GuiGraphics g, String text, float x, float y, float size, boolean on, boolean sel) {
		float tw = width(text, size, false), w = tw + 28 + 4, h = size * 1.45F + 12 + 4;
		if (on) {
			softShadow(g, x, y + 3, w, h, 8, 0x4DE8961E);
			pill(g, x, y, w, h, 0xFFFFFFFF);
			rrv(g, x + 2, y + 2, w - 4, h - 4, (h - 4) / 2, SEL_TOP, SEL_BOTTOM);
		} else {
			pill(g, x, y, w, h, sel ? RED : 0xFFDDE3F0);
			pill(g, x + 2, y + 2, w - 4, h - 4, 0xFFFFFFFF);
		}
		text(g, text, x + (w - tw) / 2, capsTop(y + h / 2, size), size, INK, false);
		return w;
	}

	public static float chipWidth(String text, float size) {
		return width(text, size, false) + 32;
	}

	/** A type's chip (the viewer's g-type): its colour, white capitals. */
	public static float type(GuiGraphics g, String type, int colour, float x, float y, float minW, float size) {
		String t = type.toUpperCase();
		float tw = width(t, size, true), w = Math.max(minW, tw + size * 1.8F), h = size * 1.75F;
		pill(g, x, y, w, h, colour);
		text(g, t, x + (w - tw) / 2, y + (h - size * 1.1F) / 2, size, 0xFFFFFFFF, true, true);
		return w;
	}

	/** A row in a list (the viewer's g-item): sunny yellow with a white rim, a warm glow and a red arrow when picked. */
	public static void item(GuiGraphics g, String text, String right, float x, float y, float w, float h, boolean on, boolean off, float size) {
		if (on) {
			softShadow(g, x, y + 5, w, h, 14, 0x59E8961E);
			pill(g, x - 2.5F, y - 2.5F, w + 5, h + 5, 0xFFFFFFFF);
			sel(g, x, y, w, h, h / 2);
			tri(g, x + 15 + 6, y + h / 2, 12, 0, RED);
		}
		int c = on ? INK : off ? DIM : TEXT;
		draw(g, comp(Look.text(text), false), x + 40, capsTop(y + h / 2, size), size, c, false);
		if (right != null && !right.isEmpty()) right(g, right, x + w - 22, capsTop(y + h / 2, size), size, c, false);
	}

	/** Each sort of thing's colours in a list (light, middle, deep), as the menu's big buttons: balls, medicine, TMs, key items, the rest. */
	public static final int[][] GOODS = {{0xFFF27682, 0xFFD94452, 0xFFAE2E3C}, {0xFF6BDCA2, 0xFF39B27A, 0xFF22855A}, {0xFF8EC4F6, 0xFF4A8FE0, 0xFF2F64B0},
			{0xFFFFD27A, 0xFFF0A030, 0xFFC87818}, {0xFFC6AEF4, 0xFF8A62D8, 0xFF6440B0}};

	/** Which sort an item is, by its name: 0 a ball, 1 medicine, 2 a TM or HM, 3 a key item, 4 the rest (escape ropes, repels). */
	public static int goodsKind(String name) {
		String n = name.toUpperCase();
		if (n.contains("BALL")) return 0;
		if (n.contains("POTION") || n.contains("HEAL") || n.contains("ANTIDOTE") || n.contains("AWAKENING") || n.contains("REVIVE") || n.contains("ETHER") || n.contains("ELIXIR")) return 1;
		if (n.startsWith("TM") || n.startsWith("HM")) return 2;
		if (n.contains("ROPE") || n.contains("REPEL")) return 4;
		return 3;
	}

	/** The balls drawn each their own way (textures/gui/v/ball_*.png). */
	private static final java.util.Set<String> BALLS = java.util.Set.of("pok_ball", "great_ball", "ultra_ball", "master_ball", "safari_ball", "park_ball", "level_ball",
			"lure_ball", "moon_ball", "friend_ball", "love_ball", "heavy_ball", "fast_ball");

	/** An item's picture (the viewer's): balls, medicine, discs for TMs, the map, a bag for ropes and repels, else a key. */
	/** The items drawn each their own way (textures/gui/v/item_*.png, from the viewer's items.js). */
	private static final java.util.Set<String> ITEMS = java.util.Set.of("antidote", "awakening", "berry", "berry_juice", "bicycle", "big_pearl", "bitter_berry", "blk_apricorn", "blu_apricorn", "bluesky_mail",
			"burn_heal", "burnt_berry", "calcium", "carbos", "dire_hit", "elixer", "eon_mail", "escape_rope", "ether", "everstone",
			"fire_stone", "flower_mail", "fresh_water", "full_heal", "full_restore", "gold_berry", "good_rod", "grn_apricorn", "guard_spec", "hard_stone",
			"hp_up", "hyper_potion", "ice_berry", "ice_heal", "iron", "leaf_stone", "lemonade", "litebluemail", "lovely_mail", "max_elixer",
			"max_ether", "max_potion", "max_repel", "max_revive", "mint_berry", "miracleberry", "mirage_mail", "moomoo_milk", "moon_stone", "morph_mail",
			"music_mail", "mysteryberry", "nugget", "old_rod", "parlyz_heal", "pearl", "pnk_apricorn", "portraitmail", "potion", "pp_up",
			"protein", "przcureberry", "psncureberry", "rare_candy", "red_apricorn", "repel", "revive", "sacred_ash", "secretpotion", "soda_pop",
			"star_piece", "stardust", "sun_stone", "super_potion", "super_repel", "super_rod", "surf_mail", "thunderstone", "water_stone", "wht_apricorn",
			"x_accuracy", "x_attack", "x_defend", "x_special", "x_speed", "ylw_apricorn");

	/** An item's name as its picture's (lower case, other letters as _): POKé DOLL is poke_doll. */
	public static String itemSlug(String name) {
		return name.toLowerCase(java.util.Locale.ROOT).replace("é", "e").replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
	}

	public static String itemIcon(String name) {
		String n = name.toUpperCase();
		if (n.contains("PARCEL")) return "icon_parcel";
		String slug = itemSlug(name);
		if (ITEMS.contains(slug)) return "item_" + slug;
		if (n.contains("BALL")) {
			String b = n.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z]+", "_").replaceAll("^_|_$", "");
			return BALLS.contains(b) ? "ball_" + b : "ball_pok_ball";
		}
		if (n.contains("POTION") || n.contains("HEAL") || n.contains("ANTIDOTE") || n.contains("AWAKENING") || n.contains("REVIVE")) return "icon_potion";
		if (n.startsWith("TM") || n.startsWith("HM")) return "icon_disc";
		if (n.contains("MAP")) return "icon_map";
		if (n.contains("ROPE") || n.contains("REPEL")) return "icon_bag";
		return "icon_key";
	}

	/**
	 * A thing in a list (the bag, the shop) as a coloured bar, Let's Go's way: its sort's colours lit from the top,
	 * its picture in a white circle at the left, a Poké Ball mark at the right end, white words; the one you're on
	 * cream, its circle ringed in its colour.
	 */
	public static void goods(GuiGraphics g, String name, String right, float x, float y, float w, float h, boolean on, float size) {
		int[] c = GOODS[goodsKind(name)];
		if (on) {
			// Cream (--g-cream) with a white rim; a pale halo 3px round it over a soft shadow (0 6px 16px).
			softShadow(g, x, y + 6, w, h, 16, 0x40000000);
			pill(g, x - 3, y - 3, w + 6, h + 6, 0x59FFF4D8);
			pill(g, x, y, w, h, 0xFFFFFFFF);
			pill(g, x + 2, y + 2, w - 4, h - 4, CREAM);
		} else {
			// Its colours lit from the top, under a see-through white rim (2px), a hard shadow 3px under it.
			float[] at = {0, 0.6F, 1};
			pill(g, x, y + 3, w, h, 0x30000000);
			rrStops(g, x, y, w, h, h / 2, at, c);
			pill(g, x, y, w, h, 0x73FFFFFF);
			rrStops(g, x + 2, y + 2, w - 4, h - 4, (h - 4) / 2, at, c);
		}
		float ir = (h - 10) / 2, icx = x + 7 + ir, icy = y + h / 2;
		if (on) ellipse(g, icx, icy, ir + 3, ir + 3, c[1]);
		ellipse(g, icx, icy, ir, ir, 0xFFFFFFFF);
		float is = ir * 1.44F;
		icon(g, itemIcon(name), icx - is / 2, icy - is / 2, is);
		float ms = h - 16;
		icon(g, "icon_ballmark", x + w - 12 - ms, y + 8, ms, on ? Ui.alpha(c[1], 0.55F) : 0x4DFFFFFF);
		int ink = on ? INK : 0xFFFFFFFF;
		float tx = x + 7 + 2 * ir + 14, ty = capsTop(y + h / 2, size), rx = x + w - 12 - ms - 14;
		if (!on) text(g, name, tx, ty + 2, size, 0x40000000, true, false);
		text(g, name, tx, ty, size, ink, true, false);
		if (right != null && !right.isEmpty()) {
			if (!on) right(g, right, rx, ty + 2, size, 0x40000000, true);
			right(g, right, rx, ty, size, ink, true);
		}
	}

	/** A colour swatch (g-swatch): round, a 3px white rim; a sunny ring and a touch bigger when picked. */
	public static void swatch(GuiGraphics g, int colour, float x, float y, float size, boolean on, float radius) {
		if (on) {
			float gr = size * 0.05F;
			x -= gr;
			y -= gr;
			size += gr * 2;
			radius += gr;
			rr(g, x - 4, y - 4, size + 8, size + 8, radius + 4, 0xFFFFC93C);
		}
		rr(g, x, y + 2, size, size, radius, 0x401E2854);
		rr(g, x, y, size, size, radius, 0xFFFFFFFF);
		rr(g, x + 3, y + 3, size - 6, size - 6, Math.max(0, radius - 3), colour | 0xFF000000);
	}

	/** A bar (HP, EXP), the viewer's g-hp: a pale track 2px round the fill, the fill rounded. */
	public static void bar(GuiGraphics g, float x, float y, float w, float h, float frac, int colour) {
		pill(g, x, y, w, h, TRACK);
		frac = Math.max(0, Math.min(1, frac));
		if (frac > 0) pill(g, x + 2, y + 2, Math.max(h - 4, (w - 4) * frac), h - 4, colour);
	}

	/** The EXP bar (g-exp): 7 tall, the pale track, sky blue. */
	public static void expBar(GuiGraphics g, float x, float y, float w, float frac) {
		pill(g, x, y, w, 7, TRACK);
		frac = Math.max(0, Math.min(1, frac));
		if (frac > 0) pill(g, x, y, Math.max(7, w * frac), 7, 0xFF6FC3FF);
	}

	public static int hpColour(float frac) {
		return frac > 0.5F ? 0xFF4CD27E : frac > 0.2F ? 0xFFF2C744 : 0xFFEF4F4F;
	}

	/** The rounded header pill at the top left of a menu page (the viewer's header()): its picture, its name, a label. */
	public static void header(GuiGraphics g, String icon, String text, String right) {
		text = Lang.t(text);
		float tw = titleWidth(text, 28), rw = right == null ? 0 : 18 + labelWidth(right), w = 12 + 42 + 12 + tw + rw + 22, hh = 58;
		panel(g, 28, 22, w, hh, hh / 2, true);
		if (icon != null) icon(g, icon, 28 + 12 + 1, 22 + 8 + 1, 40);
		title(g, text, 28 + 12 + 42 + 12, capsTop(22 + hh / 2, 28), 28, INK);
		if (right != null) label(g, right, 28 + 12 + 42 + 12 + tw + 18, capsTop(22 + hh / 2, 13));
	}

	// ---------- the text box ----------

	/**
	 * The text box (the viewer's g-text), Let's Go's: white with a pearly sheen at its right end and the pastel rim;
	 * the speaker on a coral tab at its top left; the words in ink (25px, 800) with names in coral; our turning
	 * cube bobbing at its foot when there's more. lines: what's typed out so far, one entry a line.
	 */
	public static void textBox(GuiGraphics g, float x, float bottom, float w, String who, List<String> lines, boolean more, float secs) {
		float size = 25, lh = size * 1.45F, h = Math.max(120, 22 + 24 + Math.max(1, lines.size()) * lh), y = H - bottom - h, r = 22;
		shadow(g, x, y, w, h, r, 1);
		holo(g, x, y, w, h, r);
		rrh(g, x + 3, y + 3, w - 6, h - 6, r - 3, SHEEN_AT, SHEEN);
		if (who != null && !who.isEmpty()) {
			String wn = Lang.t(who);
			float tw = titleWidth(wn, 21), tx = x + 3 + 28, ty = y + 3 - 20, th = 36;
			pill(g, tx - 1, ty + 4, tw + 36 + 2, th, 0x401E2854);
			pill(g, tx - 3, ty - 3, tw + 36 + 6, th + 6, 0xFFFFFFFF);
			rrv(g, tx, ty, tw + 36, th, th / 2, 0xFFFF9473, 0xFFE5525C);
			title(g, wn, tx + 18, capsTop(ty + th / 2, 21), 21, 0xFFFFFFFF);
		}
		for (int i = 0; i < lines.size(); i++) draw(g, rich(lines.get(i), false, INK), x + 3 + 34, y + 3 + 22 + i * lh + (lh - size) / 2, size, INK, false);
		if (more) moreCube(g, x + w - 3 - 24 - 13, y + h - 3 - 14 - 13, 26, secs, EDGE_LIGHT);
	}

	/**
	 * The words over the picture (the viewer's g-text.film, the professor's): no box, white on a dark band fading up
	 * from the foot of the screen, the speaker in gold capitals above, names in gold, the white cube when there's more.
	 */
	public static void filmBox(GuiGraphics g, String who, List<String> lines, boolean more, float secs) {
		float size = 25, lh = size * 1.45F, top = 60, h = top + (who != null && !who.isEmpty() ? 19 * 1.45F + 6 : 0) + Math.max(1, lines.size()) * lh + 44, y = H - h;
		// linear-gradient(180deg, rgba(12,22,48,0), rgba(12,22,48,.55) 38%, rgba(12,22,48,.84)): across the whole window.
		float gy = y(y), gb = gh;
		int mid = (int) (gy + (gb - gy) * 0.38F);
		g.fillGradient(0, (int) gy, gw, mid, 0x000C1630, 0x8C0C1630);
		g.fillGradient(0, mid, gw, (int) gb, 0x8C0C1630, 0xD60C1630);
		float ty = y + top;
		if (who != null && !who.isEmpty()) {
			String wn = Lang.t(who).toUpperCase();
			tracked(g, wn, 180 + 0.5F, ty + 2, 19, 0x80000000, 1.6F);
			tracked(g, wn, 180, ty, 19, 0xFFFFD45A, 1.6F);
			ty += 19 * 1.45F + 6;
		}
		for (int i = 0; i < lines.size(); i++) {
			Component c = richIn(lines.get(i), false, 0xFFFFFFFF, 0xFFFFD45A);
			draw(g, c.copy(), 180 + 0.5F, ty + i * lh + (lh - size) / 2 + 2, size, 0x73000000, false);
			draw(g, c, 180, ty + i * lh + (lh - size) / 2, size, 0xFFFFFFFF, false);
		}
		if (more) moreCube(g, W - 180 - 26, H - 26 - 26, 26, secs, 0xFFFFFFFF);
	}

	/**
	 * Our own "more to read" mark (the viewer's MORE_MARK): a little cube, its three faces in one colour at three
	 * strengths, bobbing up 4px and turning half round every 1.6 seconds.
	 */
	public static void moreCube(GuiGraphics g, float x, float y, float size, float secs, int colour) {
		float ph = (secs % 1.6F) / 1.6F, e = (float) (0.5 - 0.5 * Math.cos(ph * Math.PI * 2)), bob = -4 * e, turn = (float) Math.PI * e;
		float cx = x + size / 2, cy = y + size / 2 + bob;
		g.pose().pushMatrix();
		g.pose().translate(x(cx), y(cy));
		g.pose().rotate(turn);
		g.pose().scale(s(size) / 128, s(size) / 128);
		g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, tex("more_cube"), -64, -64, 0, 0, 128, 128, 128, 128, 128, 128, colour);
		g.pose().popMatrix();
	}

	/** A list in a panel (the viewer's choice box): rows of g-items. Returns its height. */
	public static float choice(GuiGraphics g, List<String> items, List<String> rights, int sel, float x, float y, float w) {
		// The panel's rim (3px) and its padding (12px) round the rows.
		float pad = 15, rh = 46, gap = 8, h = choiceHeight(items.size());
		panel(g, x, y, w, h);
		for (int i = 0; i < items.size(); i++)
			item(g, Lang.t(items.get(i)), rights == null ? null : rights.get(i), x + pad, y + pad + i * (rh + gap), w - pad * 2, rh, i == sel, false, 21);
		return h;
	}

	public static float choiceHeight(int n) {
		return 30 + n * 46 + Math.max(0, n - 1) * 8;
	}

	/** A row in a list with a POKéMON's picture before its words (the viewer's g-item with a 36px picture in it). */
	public static void itemPic(GuiGraphics g, String species, String text, String right, float x, float y, float w, float h, boolean on, boolean off, float size) {
		item(g, "", right, x, y, w, h, on, off, size);
		float ps = 36, py = y + (h - ps) / 2;
		Ui.blitSmooth(g, Look.portrait(species), x(x + 32), y(py), x(x + 32 + ps), y(py + ps), 0, 1, 0, 1, 0xFFFFFFFF);
		int c = on ? INK : off ? DIM : TEXT;
		draw(g, comp(Look.text(text), false), x + 82, capsTop(y + h / 2, size), size, c, false);
	}

	/** Which row of a choice box the mouse is over (canvas coordinates), or -1. */
	public static int choiceAt(int n, float x, float y, float w, double mx, double my) {
		float px = vx(mx), py = vy(my);
		if (px < x || px > x + w) return -1;
		for (int i = 0; i < n; i++) {
			float ry = y + 15 + i * 54;
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
