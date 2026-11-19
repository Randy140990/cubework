package com.insidethecube.retromon.client;

import java.util.ArrayList;
import java.util.List;

import com.insidethecube.retromon.engine.Type;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import com.insidethecube.retromon.Retromon;
import org.lwjgl.glfw.GLFW;

/**
 * The look of Retromon's screens: deep navy glass panels with soft rounded corners, clean white text (Poppins,
 * a rounded modern font, as the newer games use), cream highlights for what's picked, colour for what you can
 * press (FIGHT red, BAG amber, POKéMON green, RUN blue), type colours on moves, smooth HP and EXP bars.
 */
public final class Ui {
	private Ui() {}

	/** The battle screen's look, everywhere: deep teal glass with a mint rim. */
	public static final int PANEL = 0xF0123C38, PANEL_SOFT = 0xD0123C38, EDGE = 0xFF4FB896, EDGE_LIGHT = 0xFF8FF5C0, SHINE = 0x22FFFFFF;
	public static final int TEXT = 0xFFF4F6FF, MUTED = 0xFFA9B4D4, DIM = 0xFF6B7697, INK = 0xFF1A1F30;
	public static final int RED = 0xFFE5525C, AMBER = 0xFFF0B43C, GREEN = 0xFF41BF84, BLUE = 0xFF4A8FE0, GOLD = 0xFFFFD45A, CYAN = 0xFF47C6F2;
	public static final int HP_GREEN = 0xFF4CD27E, HP_YELLOW = 0xFFF2C744, HP_RED = 0xFFEF4F4F, TRACK = 0xFF0B1020;
	public static final String CURSOR = "▶";
	/** The cream of a picked row or button (the newer games' highlight), and the dark text on it. */
	public static final int CREAM = 0xFFFFF4D8, CREAM_INK = 0xFF26304A;

	/** How big to draw: 1 on a small window or a big GUI scale, 2 or more when there's room. */
	public static int scale(int screenHeight) {
		return Math.max(1, Math.min(3, screenHeight / 190));
	}

	/** A rounded rectangle in one colour: corners rounded by about one and a half times s (smooth, see rr). */
	public static void round(GuiGraphics g, int x, int y, int w, int h, int color, int s) {
		rr(g, x, y, w, h, Math.max(1, s) * 1.6F, color);
	}

	/** How many screen pixels a GUI pixel is (the GUI scale). */
	private static float gs() {
		return (float) Math.max(1, net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScale());
	}

	/**
	 * A rectangle with round corners (radius r), drawn at the screen's own resolution with soft, anti-aliased
	 * edges (not stepped in GUI pixels): the newer games' smooth panels, buttons and pills. x, y, w, h and r are in
	 * GUI pixels.
	 */
	public static void rr(GuiGraphics g, float x, float y, float w, float h, float r, int color) {
		rrv(g, x, y, w, h, r, color, color);
	}

	/** A colour part way from one to another (each channel, alpha too). */
	public static int mix(int a, int b, float t) {
		t = Math.max(0, Math.min(1, t));
		int out = 0;
		for (int sh = 0; sh <= 24; sh += 8) {
			int ca = (a >>> sh) & 0xFF, cb = (b >>> sh) & 0xFF;
			out |= Math.round(ca + (cb - ca) * t) << sh;
		}
		return out;
	}

	/** A rounded rectangle as rr, shaded from one colour at the top to another at the bottom. */
	public static void rrv(GuiGraphics g, float x, float y, float w, float h, float r, int top, int bottom) {
		rrv(g, x, y, w, h, r, top, bottom, 1F);
	}

	/** White anti-aliased discs (textures/gui/shape/disc_R.png, R the radius in pixels): corners, circles and ellipses. */
	private static final int[] DISCS = {4, 6, 9, 14, 20, 30, 46, 68, 103, 154, 231, 346, 519};
	private static final net.minecraft.resources.Identifier[] DISC_IDS = new net.minecraft.resources.Identifier[DISCS.length];

	/** The disc to draw radius R (screen pixels) from: the smallest at least as big (scaled down at most by a half). */
	private static int disc(float R) {
		for (int i = 0; i < DISCS.length; i++) if (DISCS[i] >= R) return i;
		return DISCS.length - 1;
	}

	private static net.minecraft.resources.Identifier discId(int i) {
		if (DISC_IDS[i] == null) DISC_IDS[i] = com.insidethecube.retromon.Retromon.id("textures/gui/shape/disc_" + DISCS[i] + ".png");
		return DISC_IDS[i];
	}

	/** A quarter of a disc (corner 0 top left, 1 top right, 2 bottom left, 3 bottom right) into an R by R box. */
	private static void quarter(GuiGraphics g, int x, int y, int R, int corner, int color) {
		if (R <= 0 || (color >>> 24) == 0) return;
		int i = disc(R), D = DISCS[i];
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(R / (float) D, R / (float) D);
		g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, discId(i), 0, 0, (corner & 1) * D, (corner >> 1) * D, D, D, 2 * D, 2 * D, color);
		g.pose().popMatrix();
	}

	/**
	 * A rounded rectangle shaded from top (at its top) to bottom (stop of the way down, flat after that). Drawn as
	 * a few plain bands and four textured corners (a handful of quads, not one per pixel), at the screen's own
	 * resolution, its curved edges anti-aliased. Nothing is drawn twice, so see-through colours stay even.
	 */
	public static void rrv(GuiGraphics g, float x, float y, float w, float h, float r, int top, int bottom, float stop) {
		if (w <= 0 || h <= 0 || ((top >>> 24) == 0 && (bottom >>> 24) == 0)) return;
		float k = gs();
		int X = Math.round(x * k), Y = Math.round(y * k), W = Math.round((x + w) * k) - X, H = Math.round((y + h) * k) - Y;
		if (W <= 0 || H <= 0) return;
		int R = (int) Math.max(0, Math.min(Math.round(r * k), Math.min(W, H) / 2));
		g.pose().pushMatrix();
		g.pose().scale(1 / k, 1 / k);
		float st = Math.max(0.001F, stop);
		java.util.function.IntUnaryOperator at = yy -> top == bottom ? top : mix(top, bottom, (yy - Y) / (H * st));
		// The bands: across the top and bottom between the corners, and the whole width between them.
		bands(g, X + R, Y, X + W - R, Y + R, Y, H, st, top, bottom);
		bands(g, X, Y + R, X + W, Y + H - R, Y, H, st, top, bottom);
		bands(g, X + R, Y + H - R, X + W - R, Y + H, Y, H, st, top, bottom);
		quarter(g, X, Y, R, 0, at.applyAsInt(Y + R / 2));
		quarter(g, X + W - R, Y, R, 1, at.applyAsInt(Y + R / 2));
		quarter(g, X, Y + H - R, R, 2, at.applyAsInt(Y + H - R / 2));
		quarter(g, X + W - R, Y + H - R, R, 3, at.applyAsInt(Y + H - R / 2));
		g.pose().popMatrix();
	}

	/** A box from x0, y0 to x1, y1 (screen pixels) in its shading: top to bottom over stop of H from Y, then flat. */
	private static void bands(GuiGraphics g, int x0, int y0, int x1, int y1, int Y, int H, float stop, int top, int bottom) {
		if (x1 <= x0 || y1 <= y0) return;
		if (top == bottom) {
			g.fill(x0, y0, x1, y1, top);
			return;
		}
		int ys = Y + Math.round(H * stop);
		if (y0 < ys) {
			int e = Math.min(y1, ys);
			g.fillGradient(x0, y0, x1, e, mix(top, bottom, (y0 - Y) / (H * stop)), mix(top, bottom, (e - Y) / (H * stop)));
		}
		if (y1 > ys) g.fill(x0, Math.max(y0, ys), x1, y1, bottom);
	}

	/** A rounded ring (an outline): radius r, t thick, smooth like rr. */
	public static void ring(GuiGraphics g, float x, float y, float w, float h, float r, float t, int color) {
		if (w <= 0 || h <= 0 || t <= 0) return;
		float k = gs();
		float X = x * k, Y = y * k, W = w * k, H = h * k, R = Math.min(r * k, Math.min(W, H) / 2), T = t * k;
		float iX = X + T, iY = Y + T, iW = W - 2 * T, iH = H - 2 * T, iR = Math.max(0, R - T);
		g.pose().pushMatrix();
		g.pose().scale(1 / k, 1 / k);
		int midA = (int) Math.ceil(Y + Math.max(R, T)), midB = (int) Math.floor(Y + H - Math.max(R, T));
		for (int row = (int) Math.floor(Y); row < (int) Math.ceil(Y + H); row++) {
			if (row >= midA && row < midB) {
				// The straight sides: one block each (not a row at a time).
				block(g, midA, midB, X, X + T, color);
				block(g, midA, midB, X + W - T, X + W, color);
				row = midB - 1;
				continue;
			}
			float cy = row + 0.5F;
			float[] o = extent(X, Y, W, H, R, cy);
			if (o == null) continue;
			float[] in = iW > 0 && iH > 0 ? extent(iX, iY, iW, iH, iR, cy) : null;
			if (in == null) span(g, row, o[0], o[1], color);
			else {
				span(g, row, o[0], in[0], color);
				span(g, row, in[1], o[1], color);
			}
		}
		g.pose().popMatrix();
	}

	/** Where a rounded rectangle runs across one row (its left and right), or null if the row's outside it. */
	private static float[] extent(float X, float Y, float W, float H, float R, float cy) {
		if (cy < Y || cy > Y + H) return null;
		float dy = 0;
		if (cy < Y + R) dy = Y + R - cy;
		else if (cy > Y + H - R) dy = cy - (Y + H - R);
		float in = dy > 0 ? R - (float) Math.sqrt(Math.max(0, R * R - dy * dy)) : 0;
		return new float[] {X + in, X + W - in};
	}

	/** Rows r0 to r1 from a to b (screen pixels), its ends as much as they're covered. */
	private static void block(GuiGraphics g, int r0, int r1, float a, float b, int color) {
		if (b <= a || r1 <= r0) return;
		int al = color >>> 24, rgb = color & 0xFFFFFF;
		int ia = (int) Math.ceil(a), ib = (int) Math.floor(b);
		if (ib < ia) {
			g.fill(ib, r0, ib + 1, r1, ((int) (al * (b - a)) << 24) | rgb);
			return;
		}
		if (ib > ia) g.fill(ia, r0, ib, r1, color);
		float fl = ia - a, fr = b - ib;
		if (fl > 0) g.fill(ia - 1, r0, ia, r1, ((int) (al * fl) << 24) | rgb);
		if (fr > 0) g.fill(ib, r0, ib + 1, r1, ((int) (al * fr) << 24) | rgb);
	}

	/** One row's run from a to b (screen pixels), its ends as much as they're covered. */
	private static void span(GuiGraphics g, int row, float a, float b, int color) {
		if (b <= a) return;
		int al = color >>> 24, rgb = color & 0xFFFFFF;
		int ia = (int) Math.ceil(a), ib = (int) Math.floor(b);
		if (ib < ia) {
			g.fill(ib, row, ib + 1, row + 1, ((int) (al * (b - a)) << 24) | rgb);
			return;
		}
		if (ib > ia) g.fill(ia, row, ib, row + 1, color);
		float fl = ia - a, fr = b - ib;
		if (fl > 0) g.fill(ia - 1, row, ia, row + 1, ((int) (al * fl) << 24) | rgb);
		if (fr > 0) g.fill(ib, row, ib + 1, row + 1, ((int) (al * fr) << 24) | rgb);
	}

	/**
	 * A small solid triangle pointing a way (0 up, 1 right, 2 down, 3 left), centred on (cx, cy), size across in GUI
	 * pixels, drawn smooth at the screen's resolution (the font's arrows look odd, so they're drawn instead).
	 */
	public static void triangle(GuiGraphics g, float cx, float cy, float size, int dir, int color) {
		float k = gs();
		float S = size * k, CX = cx * k, CY = cy * k;
		int a = color >>> 24, rgb = color & 0xFFFFFF;
		g.pose().pushMatrix();
		g.pose().translate(cx, cy);
		g.pose().rotate((float) (Math.PI / 2 * dir));
		g.pose().scale(1 / k, 1 / k);
		// Pointing up: rows from the tip down, each as wide as the triangle is there, soft at the ends.
		float hgt = S * 0.86F;
		for (int j = 0; j < (int) Math.ceil(hgt); j++) {
			float half = (j + 0.5F) / hgt * S / 2;
			float top = -hgt / 2 + j;
			int x0 = (int) Math.ceil(-half), x1 = (int) Math.floor(half);
			int yy = Math.round(top);
			if (x1 > x0) g.fill(x0, yy, x1, yy + 1, color);
			float f = x0 + half;
			if (f > 0) {
				int c = ((int) (a * Math.min(1, f)) << 24) | rgb;
				g.fill(x0 - 1, yy, x0, yy + 1, c);
				g.fill(x1, yy, x1 + 1, yy + 1, c);
			}
		}
		g.pose().popMatrix();
	}

	/** A filled ellipse centred on (cx, cy), radii rx and ry (GUI pixels), with soft edges: one textured disc. */
	public static void ellipse(GuiGraphics g, float cx, float cy, float rx, float ry, int color) {
		if (rx <= 0 || ry <= 0 || (color >>> 24) == 0) return;
		float k = gs();
		float RX = rx * k, RY = ry * k;
		int i = disc((float) Math.sqrt(RX * RY)), D = DISCS[i];
		g.pose().pushMatrix();
		g.pose().scale(1 / k, 1 / k);
		g.pose().translate(cx * k - RX, cy * k - RY);
		g.pose().scale(RX / D, RY / D);
		g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, discId(i), 0, 0, 0, 0, 2 * D, 2 * D, 2 * D, 2 * D, color);
		g.pose().popMatrix();
	}

	/** A panel, as the battle screen's: a soft shadow, a mint rim, deep teal glass, a faint sheen across the top. */
	public static void panel(GuiGraphics g, int x, int y, int w, int h, int s) {
		float r = 5 * s;
		rr(g, x + s * 0.6F, y + s * 1.4F, w, h, r, 0x50000000);
		rr(g, x, y, w, h, r, EDGE_LIGHT);
		rr(g, x + s, y + s, w - 2 * s, h - 2 * s, r - s, 0xFF123C38);
		rrv(g, x + s, y + s, w - 2 * s, h / 2F - s, r - s, 0x221D5A52, 0x000B2A2C);
		rr(g, x + 4 * s, y + 2 * s, w - 8 * s, Math.max(1, s * 0.6F), 1, SHINE);
	}

	/** A panel with a coloured strip down its left side (for menus and boxes that belong to something). */
	public static void tagged(GuiGraphics g, int x, int y, int w, int h, int s, int accent) {
		panel(g, x, y, w, h, s);
		rr(g, x + 1.5F * s, y + 4 * s, 2 * s, h - 8 * s, s, accent);
	}

	/** A button: its colour (brighter when picked or under the mouse), its label centred. */
	public static void button(GuiGraphics g, Font f, int x, int y, int w, int h, String label, int color, boolean on, int s) {
		float r = Math.min(h / 2F, 6 * s);
		rr(g, x + s * 0.5F, y + s * 1.2F, w, h, r, 0x50000000);
		rr(g, x, y, w, h, r, on ? 0xFFFFFFFF : darker(color, 0.55F));
		rr(g, x + s, y + s, w - 2 * s, h - 2 * s, r - s, on ? color : darker(color, 0.8F));
		rr(g, x + 3 * s, y + 1.6F * s, w - 6 * s, Math.max(1, (h - 2 * s) * 0.4F), r - 2 * s, 0x26FFFFFF);
		int lw = boldWidth(f, label, s);
		draw(g, f, bold(label), x + (w - lw) / 2, y + (h - 8 * s) / 2, TEXT, s, true);
	}

	/** A pill: a rounded row that's cream when picked (dark text) and glass when not, as the newer games' lists. */
	public static void pill(GuiGraphics g, int x, int y, int w, int h, boolean on, int s) {
		float r = Math.min(h / 2F, 5 * s);
		if (on) rr(g, x - s * 0.5F, y - s * 0.5F, w + s, h + s, r + s * 0.5F, 0xFFFFFFFF);
		rr(g, x, y, w, h, r, on ? CREAM : 0xB0123C38);
		if (!on) rr(g, x + 3 * s, y + s * 0.5F, w - 6 * s, Math.max(1, s * 0.5F), 1, 0x26FFFFFF);
	}

	/**
	 * The cinema frame: the top and bottom of the screen darkening smoothly into the picture (no hard bars). t (0 to
	 * 1) is how far it has faded in: the whole frame fades in together, by its opacity.
	 */
	public static void cinema(GuiGraphics g, int width, int height, float t) {
		t = Math.max(0, Math.min(1, t));
		if (t <= 0) return;
		// Ease in, so it arrives softly.
		float k = t * t * (3 - 2 * t);
		int deep = Math.round(height * 0.075F), soft = Math.round(height * 0.12F);
		int a = (int) (0xE6 * k), mid = (int) (0x9C * k);
		g.fillGradient(0, 0, width, deep, a << 24, mid << 24);
		g.fillGradient(0, deep, width, deep + soft, mid << 24, 0);
		g.fillGradient(0, height - deep - soft, width, height - deep, 0, mid << 24);
		g.fillGradient(0, height - deep, width, height, mid << 24, a << 24);
	}

	/** A glow of colour around the edges of the screen (a hit's colour), strength 0 to 1. */
	public static void vignette(GuiGraphics g, int width, int height, int rgb, float strength) {
		if (strength <= 0) return;
		int a = (int) (Math.min(1, strength) * 0xB0);
		int c = (a << 24) | (rgb & 0xFFFFFF), clear = rgb & 0xFFFFFF;
		int e = Math.round(height * 0.22F);
		g.fillGradient(0, 0, width, e, c, clear);
		g.fillGradient(0, height - e, width, height, clear, c);
		// Sides: thin vertical strips stepping down in strength (the GUI only fades top to bottom).
		int side = Math.round(width * 0.12F), steps = 12;
		for (int i = 0; i < steps; i++) {
			int x0 = side * i / steps, x1 = side * (i + 1) / steps;
			int ai = (int) (a * (1 - (i + 0.5F) / steps) * 0.85F);
			int col = (ai << 24) | (rgb & 0xFFFFFF);
			g.fill(x0, 0, x1, height, col);
			g.fill(width - x1, 0, width - x0, height, col);
		}
	}

	/**
	 * A slanted bar (the newer games' buttons): a parallelogram leaning forward by lean pixels, filled top to
	 * bottom from one colour to another, with an outline if edge isn't 0.
	 */
	public static void slab(GuiGraphics g, int x, int y, int w, int h, int top, int bottom, int lean, int edge, int edgeW) {
		g.pose().pushMatrix();
		g.pose().translate(x + lean / 2F, y + h / 2F);
		g.pose().mul(new org.joml.Matrix3x2f(1, 0, -lean / (float) Math.max(1, h), 1, 0, 0));
		int hh = h / 2;
		if (edge != 0 && edgeW > 0) {
			g.fill(-edgeW, -hh - edgeW, w + edgeW, h - hh + edgeW, edge);
		}
		g.fillGradient(0, -hh, w, h - hh, top, bottom);
		// A sheen along the top.
		g.fill(1, -hh, w - 1, -hh + Math.max(1, h / 10), 0x38FFFFFF);
		g.pose().popMatrix();
	}

	/** A whole picture (texW by texH) drawn into a box, tinted with a colour (0xFFFFFFFF for none). */
	public static void pic(GuiGraphics g, net.minecraft.resources.Identifier id, int x, int y, int w, int h, int texW, int texH, int color) {
		g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, id, x, y, 0, 0, w, h, texW, texH, texW, texH, color);
	}

	/** A white picture (an icon) tinted with a colour. */
	public static void icon(GuiGraphics g, String name, int x, int y, int size, int color) {
		g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, Retromon.id("textures/gui/" + name + ".png"), x, y, 0, 0, size, size, 32, 32, 32, 32,
				color);
	}

	/**
	 * A speech bubble with an emote in it (heart, star, exclaim, note, happy), its tail pointing down at (cx,
	 * bottom). pop (0 to 1) is how far it has popped up.
	 */
	public static void bubble(GuiGraphics g, String emote, int cx, int bottom, int s, float pop) {
		if (pop <= 0) return;
		float k = pop < 1 ? (float) (1 + Math.sin(pop * Math.PI) * 0.25) * pop : 1;
		int size = Math.round(22 * s * k), half = size / 2;
		int top = bottom - size - Math.round(5 * s * k);
		round(g, cx - half - s, top - s, size + 2 * s, size + 2 * s, 0x50000000, 3 * s);
		round(g, cx - half, top, size, size, 0xFFFFFFFF, 3 * s);
		// The tail.
		for (int i = 0; i < 5 * s * k; i++) {
			int w = Math.max(0, Math.round(4 * s * k) - i);
			g.fill(cx - w, top + size + i, cx + w, top + size + i + 1, 0xFFFFFFFF);
		}
		int in = Math.round(size * 0.72F);
		icon(g, "emote/" + emote, cx - in / 2, top + (size - in) / 2, in, 0xFFFFFFFF);
	}

	/** A soft round glow (a light behind something), built up from see-through rounded boxes. */
	public static void glow(GuiGraphics g, int cx, int cy, int rx, int ry, int color) {
		for (int i = 0; i < 7; i++) {
			int x = rx - i * rx / 8, y = ry - i * ry / 8;
			round(g, cx - x, cy - y, 2 * x, 2 * y, color, Math.max(1, Math.min(x, y) / 3));
		}
	}

	/** A colour mixed towards white by k (0 to 1). */
	public static int lighter(int argb, float k) {
		int a = argb >>> 24, r = (argb >> 16) & 255, gr = (argb >> 8) & 255, b = argb & 255;
		return (a << 24) | ((int) (r + (255 - r) * k) << 16) | ((int) (gr + (255 - gr) * k) << 8) | (int) (b + (255 - b) * k);
	}

	public static int darker(int argb, float k) {
		int a = argb >>> 24, r = (argb >> 16) & 255, gr = (argb >> 8) & 255, b = argb & 255;
		return (a << 24) | ((int) (r * k) << 16) | ((int) (gr * k) << 8) | (int) (b * k);
	}

	public static int alpha(int argb, float a) {
		return ((int) (Math.max(0, Math.min(1, a)) * (argb >>> 24)) << 24) | (argb & 0xFFFFFF);
	}

	private static final Style FONT = Style.EMPTY.withFont(new FontDescription.Resource(Retromon.id("ui")));
	private static final Style BOLD = Style.EMPTY.withFont(new FontDescription.Resource(Retromon.id("ui_bold")));

	/** A Pokémon's portrait (drawn from its model), as a square of the given size. */
	public static void portrait(GuiGraphics g, String species, int x, int y, int size) {
		if (species == null || species.isEmpty()) return;
		g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, Look.portrait(species), x, y, 0, 0, size, size, 128, 128, 128, 128);
	}

	/** A portrait as a dark silhouette (seen but not caught, in the Pokédex). */
	public static void silhouette(GuiGraphics g, String species, int x, int y, int size) {
		if (species == null || species.isEmpty()) return;
		g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, Look.portrait(species), x, y, 0, 0, size, size, 128, 128, 128, 128, 0xFF1A2038);
	}

	/** Money as the games show it: ₽3,000. */
	public static String money(int n) {
		return com.insidethecube.retromon.engine.Battle.money(n);
	}

	/** Text in the interface font. */
	public static Component ui(String text) {
		return Component.literal(Look.text(text)).withStyle(FONT);
	}

	/** Text in the bold interface font (titles, names, buttons). */
	public static Component bold(String text) {
		return Component.literal(Look.text(text)).withStyle(BOLD);
	}

	public static void text(GuiGraphics g, Font f, String text, int x, int y, int color, int s) {
		draw(g, f, ui(text), x, y, color, s, true);
	}

	public static void plain(GuiGraphics g, Font f, String text, int x, int y, int color, int s) {
		draw(g, f, ui(text), x, y, color, s, false);
	}

	public static void shadowed(GuiGraphics g, Font f, String text, int x, int y, int color, int s) {
		draw(g, f, ui(text), x, y, color, s, true);
	}

	/** Bold text (a title or a name), with a soft shadow. */
	public static void heading(GuiGraphics g, Font f, String text, int x, int y, int color, int s) {
		draw(g, f, bold(text), x, y, color, s, true);
	}

	public static void draw(GuiGraphics g, Font f, Component c, int x, int y, int color, int s, boolean shadow) {
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(s, s);
		g.drawString(f, c, 0, 0, color, shadow);
		g.pose().popMatrix();
	}

	public static int width(Font f, String text, int s) {
		return f.width(ui(text)) * s;
	}

	public static int boldWidth(Font f, String text, int s) {
		return f.width(bold(text)) * s;
	}

	public static void centred(GuiGraphics g, Font f, String text, int cx, int y, int color, int s, boolean shadow) {
		int x = cx - width(f, text, s) / 2;
		if (shadow) shadowed(g, f, text, x, y, color, s);
		else plain(g, f, text, x, y, color, s);
	}

	public static void right(GuiGraphics g, Font f, String text, int rx, int y, int color, int s) {
		shadowed(g, f, text, rx - width(f, text, s), y, color, s);
	}

	/** Splits text into lines that fit a width (in unscaled font pixels). */
	public static List<String> wrap(Font f, String text, int width) {
		List<String> out = new ArrayList<>();
		for (String para : text.split("\n")) {
			StringBuilder line = new StringBuilder();
			for (String word : para.split(" ")) {
				String next = line.isEmpty() ? word : line + " " + word;
				if (f.width(ui(next)) > width && !line.isEmpty()) {
					out.add(line.toString());
					line = new StringBuilder(word);
				} else {
					line = new StringBuilder(next);
				}
			}
			out.add(line.toString());
		}
		return out;
	}

	public static int hpColor(float frac) {
		return frac > 0.5F ? HP_GREEN : frac > 0.2F ? HP_YELLOW : HP_RED;
	}

	/** A rounded bar: a dark track and the part filled. */
	public static void bar(GuiGraphics g, int x, int y, int w, int h, float frac, int color, int s) {
		// A rounded track with a white rim (as the battle's HP bars), the colour inside it lit along its top.
		rr(g, x - s * 0.6F, y - s * 0.6F, w + s * 1.2F, h + s * 1.2F, (h + s * 1.2F) / 2, 0xD0EFFFF6);
		rr(g, x, y, w, h, h / 2F, TRACK);
		float fw = w * Math.max(0, Math.min(1, frac));
		if (frac > 0 && fw < h) fw = Math.min(w, Math.max(fw, h * 0.6F));
		if (fw > 0) {
			rr(g, x, y, fw, h, h / 2F, color);
			rr(g, x + h / 4F, y + h * 0.12F, Math.max(0, fw - h / 2F), Math.max(1, h * 0.3F), h * 0.15F, 0x40FFFFFF);
		}
	}

	/** An HP bar with its label. */
	public static void hp(GuiGraphics g, Font f, int x, int y, int w, float hp, int max, int s) {
		plain(g, f, "HP", x, y, GOLD, s);
		int bx = x + width(f, "HP", s) + 3 * s;
		float frac = max <= 0 ? 0 : hp / max;
		bar(g, bx, y + s, w - (bx - x), 5 * s, frac, hpColor(frac), s);
	}

	/** The colour of a type (as the later games colour their move buttons). */
	public static int typeColor(Type t) {
		if (t == null) return 0xFF8A8FA3;
		return switch (t) {
			case NORMAL -> 0xFF9DA0AA;
			case FIRE -> 0xFFF08436;
			case WATER -> 0xFF4D90D5;
			case ELECTRIC -> 0xFFE9C72E;
			case GRASS -> 0xFF5DB850;
			case ICE -> 0xFF6FCFD0;
			case FIGHTING -> 0xFFCE4069;
			case POISON -> 0xFFAB6AC8;
			case GROUND -> 0xFFD97746;
			case FLYING -> 0xFF8FA8DD;
			case PSYCHIC -> 0xFFF66F71;
			case BUG -> 0xFF90C12C;
			case ROCK -> 0xFFC5B78C;
			case GHOST -> 0xFF5269AC;
			case DRAGON -> 0xFF0A6DC4;
			case DARK -> 0xFF5A5366;
			case STEEL -> 0xFF5A8EA1;
			case CURSE -> 0xFF68A090;
		};
	}

	/** A small rounded tag (a type, a status). */
	public static int chip(GuiGraphics g, Font f, String text, int x, int y, int color, int s) {
		int w = width(f, text, s) + 8 * s;
		rr(g, x, y, w, 10 * s, 5 * s, color);
		rr(g, x + 2 * s, y + s * 0.6F, w - 4 * s, 3 * s, 1.5F * s, 0x2AFFFFFF);
		plain(g, f, text, x + 4 * s, y + s, 0xFFFFFFFF, s);
		return w;
	}

	public static int statusColor(String tag) {
		return switch (tag) {
			case "PSN" -> 0xFFA25BC7;
			case "BRN" -> 0xFFEF7A3C;
			case "FRZ" -> 0xFF5FC9D6;
			case "PAR" -> 0xFFD9B32E;
			case "SLP" -> 0xFF8C93A8;
			case "FNT" -> 0xFFB53A3A;
			default -> 0xFF8C93A8;
		};
	}

	// ---------- keys (every screen also works with the keyboard, like the games) ----------

	public static boolean isA(KeyEvent e) {
		int k = e.key();
		return k == GLFW.GLFW_KEY_SPACE || k == GLFW.GLFW_KEY_ENTER || k == GLFW.GLFW_KEY_KP_ENTER || k == GLFW.GLFW_KEY_Z || k == GLFW.GLFW_KEY_E;
	}

	public static boolean isB(KeyEvent e) {
		int k = e.key();
		return k == GLFW.GLFW_KEY_BACKSPACE || k == GLFW.GLFW_KEY_X || k == GLFW.GLFW_KEY_Q;
	}

	public static boolean isUp(KeyEvent e) {
		return e.key() == GLFW.GLFW_KEY_UP || e.key() == GLFW.GLFW_KEY_W;
	}

	public static boolean isDown(KeyEvent e) {
		return e.key() == GLFW.GLFW_KEY_DOWN || e.key() == GLFW.GLFW_KEY_S;
	}

	public static boolean isLeft(KeyEvent e) {
		return e.key() == GLFW.GLFW_KEY_LEFT || e.key() == GLFW.GLFW_KEY_A;
	}

	public static boolean isRight(KeyEvent e) {
		return e.key() == GLFW.GLFW_KEY_RIGHT || e.key() == GLFW.GLFW_KEY_D;
	}
}
