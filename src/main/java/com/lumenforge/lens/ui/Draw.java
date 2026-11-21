package com.lumenforge.lens.ui;

import com.lumenforge.lens.Lens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;

/**
 * The LumenForge look, drawn with the GUI's own fills: rounded shapes with soft (anti-aliased) edges drawn at the
 * screen's own resolution, the violet-blue-teal gradient, and Inter / Geist Mono text (both SIL OFL).
 *
 * Positions and sizes are in GUI units and may be fractions; the shapes are snapped to real pixels.
 */
public final class Draw {
	private Draw() {}

	// LumenForge colours.
	public static final int VIOLET = 0xFF8B5CF6, BLUE = 0xFF3B82F6, TEAL = 0xFF14B8A6;
	public static final int RED = 0xFFEF4444, AMBER = 0xFFF59E0B, GREEN = 0xFF22C55E;
	public static final int TEXT = 0xFFF4F4F6, SOFT = 0xFFC9C9D2, MUTED = 0xFF8E8E9C, DIM = 0xFF5E5E6C;

	public static final Style UI = Style.EMPTY.withFont(new FontDescription.Resource(Lens.id("ui")));
	public static final Style SEMI = Style.EMPTY.withFont(new FontDescription.Resource(Lens.id("semi")));
	public static final Style TITLE = Style.EMPTY.withFont(new FontDescription.Resource(Lens.id("title")));
	public static final Style MONO = Style.EMPTY.withFont(new FontDescription.Resource(Lens.id("mono")));

	/** How many screen pixels make one GUI unit. */
	public static float gs() {
		return Math.max(1, Minecraft.getInstance().getWindow().getGuiScale());
	}

	/** A colour with its alpha multiplied (for fading in). */
	public static int fade(int c, float a) {
		int al = Math.round(((c >>> 24) & 0xFF) * Math.max(0, Math.min(1, a)));
		return (al << 24) | (c & 0xFFFFFF);
	}

	public static int mix(int a, int b, float t) {
		t = Math.max(0, Math.min(1, t));
		int out = 0;
		for (int sh = 0; sh <= 24; sh += 8) {
			int ca = (a >>> sh) & 0xFF, cb = (b >>> sh) & 0xFF;
			out |= Math.round(ca + (cb - ca) * t) << sh;
		}
		return out;
	}

	/** The brand gradient at 0..1 across, with the given alpha. */
	public static int gradient(float t, float alpha) {
		int c = t < 0.5F ? mix(VIOLET, BLUE, t * 2) : mix(BLUE, TEAL, (t - 0.5F) * 2);
		return fade(c, alpha);
	}

	/** Colour across a shape (0..1): one colour, or the gradient. */
	interface Paint {
		int at(float t);
	}

	/** A rounded rectangle in one colour. */
	public static void rr(GuiGraphics g, float x, float y, float w, float h, float r, int color) {
		shape(g, x, y, w, h, r, t -> color, false);
	}

	/** A rounded rectangle in the brand gradient, left to right. */
	public static void rrGradient(GuiGraphics g, float x, float y, float w, float h, float r, float alpha) {
		shape(g, x, y, w, h, r, t -> gradient(t, alpha), true);
	}

	/** A rounded outline, one screen pixel wide, around a rounded rectangle filled with `fill`. */
	public static void card(GuiGraphics g, float x, float y, float w, float h, float r, int fill, int border) {
		float p = 1 / gs();
		rr(g, x, y, w, h, r, border);
		rr(g, x + p, y + p, w - 2 * p, h - 2 * p, Math.max(0, r - p), fill);
	}

	/** A filled circle. */
	public static void dot(GuiGraphics g, float cx, float cy, float radius, int color) {
		rr(g, cx - radius, cy - radius, radius * 2, radius * 2, radius, color);
	}

	/**
	 * Draws the shape at screen resolution: the corner columns one pixel at a time (with a part-covered pixel at
	 * each curved edge, so it's smooth), the straight middle in one go (or in strips, for a gradient).
	 */
	private static void shape(GuiGraphics g, float x, float y, float w, float h, float r, Paint paint, boolean graded) {
		float s = gs();
		int X = Math.round(x * s), Y = Math.round(y * s), W = Math.round(w * s), H = Math.round(h * s);
		if (W <= 0 || H <= 0) return;
		float R = Math.max(0, Math.min(Math.min(W, H) / 2F, r * s));
		int cols = (int) Math.ceil(R);
		g.pose().pushMatrix();
		g.pose().scale(1 / s, 1 / s);
		try {
			for (int i = 0; i < cols && i < W; i++) {
				float dx = R - i - 0.5F;
				float dy = (float) Math.sqrt(Math.max(0, R * R - dx * dx));
				float top = R - dy;
				column(g, X + i, Y, H, top, paint.at((i + 0.5F) / W));
				int j = W - 1 - i;
				if (j > i) column(g, X + j, Y, H, top, paint.at((j + 0.5F) / W));
			}
			int left = X + Math.min(cols, W), right = X + W - Math.min(cols, W);
			if (right > left) {
				if (!graded) g.fill(left, Y, right, Y + H, paint.at(0.5F));
				else {
					int strips = Math.max(1, Math.min(40, (right - left) / 3));
					for (int k = 0; k < strips; k++) {
						int a = left + (right - left) * k / strips, b = left + (right - left) * (k + 1) / strips;
						g.fill(a, Y, b, Y + H, paint.at(((a + b) / 2F - X) / W));
					}
				}
			}
		} finally {
			g.pose().popMatrix();
		}
	}

	/** One pixel column of a rounded shape, `top` pixels in from the top and bottom (a fraction is shaded). */
	private static void column(GuiGraphics g, int x, int y, int h, float top, int color) {
		int full = (int) Math.ceil(top);
		if (h - 2 * full > 0) g.fill(x, y + full, x + 1, y + h - full, color);
		float part = full - top;
		if (part > 0.02F && full > 0) {
			int c = fade(color, part);
			g.fill(x, y + full - 1, x + 1, y + full, c);
			g.fill(x, y + h - full, x + 1, y + h - full + 1, c);
		}
	}

	/** A sharp-edged fill at fractional GUI positions (lines, bars). */
	public static void rect(GuiGraphics g, float x, float y, float w, float h, int color) {
		rr(g, x, y, w, h, 0, color);
	}

	/** A horizontal line in the gradient, fading out at both ends. */
	public static void hairline(GuiGraphics g, float x, float y, float w, float alpha) {
		float s = gs();
		int X = Math.round(x * s), Y = Math.round(y * s), W = Math.round(w * s);
		g.pose().pushMatrix();
		g.pose().scale(1 / s, 1 / s);
		try {
			int strips = Math.max(1, Math.min(80, W / 4));
			for (int k = 0; k < strips; k++) {
				int a = X + W * k / strips, b = X + W * (k + 1) / strips;
				float t = (k + 0.5F) / strips;
				float edge = Math.min(1, Math.min(t, 1 - t) * 6);
				g.fill(a, Y, b, Y + Math.max(1, Math.round(s / 2)), gradient(t, alpha * edge));
			}
		} finally {
			g.pose().popMatrix();
		}
	}

	// ---------------------------------------------------------------- text

	public static Component c(String s, Style st) {
		return Component.literal(s).withStyle(st);
	}

	public static int width(Font f, String s, Style st) {
		return f.width(c(s, st));
	}

	/** Text with its capitals centred on cy. Returns the x after it. */
	public static int text(GuiGraphics g, Font f, String s, Style st, float x, float cy, int color) {
		if (((color >>> 24) & 0xFF) < 10) return Math.round(x) + width(f, s, st);
		int ix = Math.round(x), iy = Math.round(cy - 4);
		g.drawString(f, c(s, st), ix, iy, color, false);
		return ix + width(f, s, st);
	}

	/** Text scaled up (titles). */
	public static void big(GuiGraphics g, Font f, String s, Style st, float x, float cy, float scale, int color) {
		if (((color >>> 24) & 0xFF) < 10) return;
		g.pose().pushMatrix();
		g.pose().translate(x, cy - 4 * scale);
		g.pose().scale(scale, scale);
		g.drawString(f, c(s, st), 0, 0, color, false);
		g.pose().popMatrix();
	}

	/** Cuts text to fit, keeping its end (a folder's own name is at the end of its path). */
	public static String fitEnd(Font f, String s, Style st, int w) {
		if (width(f, s, st) <= w) return s;
		int keep = s.length();
		while (keep > 0 && width(f, "…" + s.substring(s.length() - keep), st) > w) keep--;
		return "…" + s.substring(s.length() - keep);
	}

	/** Cuts text to fit, keeping its start. */
	public static String fit(Font f, String s, Style st, int w) {
		if (width(f, s, st) <= w) return s;
		int keep = s.length();
		while (keep > 0 && width(f, s.substring(0, keep) + "…", st) > w) keep--;
		return s.substring(0, keep) + "…";
	}
}
