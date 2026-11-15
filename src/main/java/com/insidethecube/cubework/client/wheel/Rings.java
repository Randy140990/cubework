package com.insidethecube.cubework.client.wheel;

import java.util.Arrays;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import net.minecraft.client.renderer.RenderPipelines;
import org.joml.Matrix3x2f;

/**
 * The wheel's shapes, drawn as coloured quads in the menu like anything else the game draws there: rings,
 * wedges with even gaps between them, discs, glows, lines, triangles and rounded boxes. The game draws its
 * menus without smoothing, so every outer edge here gets a one-screen-pixel fade, which smooths curves and
 * slants the way a vector drawing would be. Sizes are in GUI units; px is one screen pixel in them.
 *
 * Angles are in radians, 0 at the top, going clockwise.
 */
final class Rings {
	private float[] xy = new float[4096];
	private int[] colour = new int[2048];
	private int quads;
	/** One screen pixel, in GUI units: how wide the smoothed edges are. */
	private float px = 1 / 3f;
	/** Everything drawn is faded by this (for the wheel opening). */
	private float alpha = 1;

	/** Colour by angle and by inner (false) or outer (true) edge. */
	interface Paint {
		int at(double angle, boolean outer);
	}

	static Paint flat(int c) {
		return (a, outer) -> c;
	}

	static Paint radial(int in, int out) {
		return (a, outer) -> outer ? out : in;
	}

	void set(float px, float alpha) {
		this.px = px;
		this.alpha = alpha;
	}

	float px() {
		return px;
	}

	// ---------------------------------------------------------------- rings and wedges

	private static float sx(double a) {
		return (float) Math.sin(a);
	}

	private static float sy(double a) {
		return (float) -Math.cos(a);
	}

	/** How far round a straight side, kept off a radius by off, is at radius r. */
	private static double side(double off, double r) {
		if (r <= 1e-4) return 0;
		return Math.asin(Math.max(-1, Math.min(1, off / r)));
	}

	/**
	 * Quads between radius ra (from angle a0 to a1) and radius rb (from b0 to b1): colours from paint at the
	 * inner or outer edge (whichever outerA and outerB say), faded by fa at ra and fb at rb.
	 */
	private void strip(float cx, float cy, float ra, double a0, double a1, float rb, double b0, double b1, Paint p, boolean outerA, boolean outerB,
			float fa, float fb) {
		int steps = Math.max(1, (int) Math.ceil(Math.max(Math.abs(a1 - a0), Math.abs(b1 - b0)) / Math.toRadians(3)));
		for (int i = 0; i < steps; i++) {
			double s = (double) i / steps, t = (double) (i + 1) / steps;
			double as = a0 + (a1 - a0) * s, at = a0 + (a1 - a0) * t, bs = b0 + (b1 - b0) * s, bt = b0 + (b1 - b0) * t;
			quad(cx + sx(as) * ra, cy + sy(as) * ra, fade(p.at(as, outerA), fa), cx + sx(bs) * rb, cy + sy(bs) * rb, fade(p.at(bs, outerB), fb),
					cx + sx(bt) * rb, cy + sy(bt) * rb, fade(p.at(bt, outerB), fb), cx + sx(at) * ra, cy + sy(at) * ra, fade(p.at(at, outerA), fa));
		}
	}

	/**
	 * Part of a ring, from a0 to a1 between radii r0 and r1, its ends cut along radii. The curved edges are
	 * smoothed when aaIn and aaOut say (not where it meets another band of the same ring).
	 */
	void band(float cx, float cy, float r0, float r1, double a0, double a1, Paint p, boolean aaIn, boolean aaOut) {
		float h = px / 2;
		aaIn &= r0 > h;
		float c0 = aaIn ? r0 + h : r0, c1 = aaOut ? r1 - h : r1;
		if (c1 <= c0) {
			// Thinner than a pixel: one faint line.
			float mid = (r0 + r1) / 2, f = Math.max(0, (r1 - r0) / px);
			strip(cx, cy, Math.max(0, mid - h), a0, a1, mid, a0, a1, p, false, false, 0, f);
			strip(cx, cy, mid, a0, a1, mid + h, a0, a1, p, true, true, f, 0);
			return;
		}
		strip(cx, cy, c0, a0, a1, c1, a0, a1, p, false, true, 1, 1);
		if (aaIn) strip(cx, cy, r0 - h, a0, a1, c0, a0, a1, p, false, false, 0, 1);
		if (aaOut) strip(cx, cy, c1, a0, a1, r1 + h, a0, a1, p, true, true, 1, 0);
	}

	void ring(float cx, float cy, float r0, float r1, Paint p) {
		band(cx, cy, r0, r1, 0, Math.PI * 2, p, true, true);
	}

	void ring(float cx, float cy, float r0, float r1, int in, int out) {
		ring(cx, cy, r0, r1, radial(in, out));
	}

	/** A circle's outline, w wide (a fainter pixel-wide line if thinner than a pixel). */
	void circle(float cx, float cy, float r, float w, int c) {
		if (w < px) {
			c = fade(c, w / px);
			w = px;
		}
		ring(cx, cy, r - w / 2, r + w / 2, flat(c));
	}

	/** A filled disc, centre colour to edge colour. */
	void disc(float cx, float cy, float r, int centre, int edge) {
		band(cx, cy, 0, r, 0, Math.PI * 2, radial(centre, edge), false, true);
	}

	/** A soft glow: c at radius r0, fading smoothly to nothing at r1 (r0 = 0 for a soft disc). */
	void glow(float cx, float cy, float r0, float r1, int c) {
		int n = 6;
		for (int i = 0; i < n; i++) {
			float t0 = (float) i / n, t1 = (float) (i + 1) / n;
			int ca = fade(c, (1 - t0) * (1 - t0)), cb = fade(c, (1 - t1) * (1 - t1));
			float ra = r0 + (r1 - r0) * t0, rb = r0 + (r1 - r0) * t1;
			strip(cx, cy, ra, 0, Math.PI * 2, rb, 0, Math.PI * 2, radial(ca, cb), false, true, 1, 1);
		}
	}

	/**
	 * A wedge of a ring, like a slice of a pie with its point cut off: a0 to a1 between radii r0 and r1, its
	 * straight sides kept gap / 2 off the radii at a0 and a1, so wedges side by side have even gaps between
	 * them. The curved edges are smoothed when aaIn and aaOut say; the straight sides always are.
	 */
	void slice(float cx, float cy, float r0, float r1, double a0, double a1, float gap, Paint p, boolean aaIn, boolean aaOut) {
		if (a1 - a0 >= Math.PI * 2 - 1e-6) {
			band(cx, cy, r0, r1, a0, a1, p, aaIn, aaOut);
			return;
		}
		float h = px / 2;
		double g = gap / 2.0;
		float c0 = aaIn ? r0 + h : r0, c1 = aaOut ? r1 - h : r1;
		if (c1 <= c0) return;
		double gc = g + h;
		// The middle, inside the smoothed edges.
		strip(cx, cy, c0, a0 + side(gc, c0), a1 - side(gc, c0), c1, a0 + side(gc, c1), a1 - side(gc, c1), p, false, true, 1, 1);
		// The curved edges.
		if (aaIn) strip(cx, cy, r0 - h, a0 + side(g, r0 - h), a1 - side(g, r0 - h), c0, a0 + side(g, c0), a1 - side(g, c0), p, false, false, 0, 1);
		if (aaOut) strip(cx, cy, c1, a0 + side(g, c1), a1 - side(g, c1), r1 + h, a0 + side(g, r1 + h), a1 - side(g, r1 + h), p, true, true, 1, 0);
		// The straight sides: from clear (g - h off the radius) to solid (g + h off it).
		for (int s = 0; s < 2; s++) {
			double a = s == 0 ? a0 : a1, dir = s == 0 ? 1 : -1;
			double oo0 = a + dir * side(g - h, c0), oo1 = a + dir * side(g - h, c1), in0 = a + dir * side(gc, c0), in1 = a + dir * side(gc, c1);
			int p0 = p.at(a, false), p1 = p.at(a, true);
			quad(cx + sx(oo0) * c0, cy + sy(oo0) * c0, fade(p0, 0), cx + sx(oo1) * c1, cy + sy(oo1) * c1, fade(p1, 0), cx + sx(in1) * c1, cy + sy(in1) * c1,
					p1, cx + sx(in0) * c0, cy + sy(in0) * c0, p0);
		}
	}

	/**
	 * Diagonal lines (like /) across a wedge, spacing apart and w wide, kept inside it: the hatching on a
	 * choice that can't be made.
	 */
	void hatch(float cx, float cy, float r0, float r1, double a0, double a1, float gap, float spacing, float w, int c) {
		double dx = Math.sqrt(0.5), dy = -Math.sqrt(0.5), nx = Math.sqrt(0.5), ny = Math.sqrt(0.5);
		boolean sides = a1 - a0 < Math.PI * 2 - 1e-6;
		double g = gap / 2.0 + w / 2.0;
		double v0x = Math.cos(a0), v0y = Math.sin(a0), v1x = Math.cos(a1), v1y = Math.sin(a1);
		int kmax = (int) Math.floor(r1 / spacing);
		for (int k = -kmax; k <= kmax; k++) {
			double o = k * spacing;
			if (o * o >= r1 * r1) continue;
			double t1 = Math.sqrt(r1 * r1 - o * o), lo = -t1, hi = t1;
			if (sides) {
				// v0 . q >= g and v1 . q <= -g, along q = n*o + d*t.
				double[] r = limit(lo, hi, (v0x * dx + v0y * dy), g - (v0x * nx + v0y * ny) * o);
				r = limit(r[0], r[1], -(v1x * dx + v1y * dy), g + (v1x * nx + v1y * ny) * o);
				lo = r[0];
				hi = r[1];
			}
			if (hi <= lo) continue;
			double t0 = o * o < r0 * r0 ? Math.sqrt(r0 * r0 - o * o) : 0;
			if (t0 > 0) {
				segment(cx, cy, o, nx, ny, dx, dy, lo, Math.min(hi, -t0), w, c);
				segment(cx, cy, o, nx, ny, dx, dy, Math.max(lo, t0), hi, w, c);
			} else {
				segment(cx, cy, o, nx, ny, dx, dy, lo, hi, w, c);
			}
		}
	}

	/** Narrows [lo, hi] to the t where k * t >= m. */
	private static double[] limit(double lo, double hi, double k, double m) {
		if (Math.abs(k) < 1e-9) return m <= 0 ? new double[] {lo, hi} : new double[] {1, 0};
		double t = m / k;
		return k > 0 ? new double[] {Math.max(lo, t), hi} : new double[] {lo, Math.min(hi, t)};
	}

	private void segment(float cx, float cy, double o, double nx, double ny, double dx, double dy, double t0, double t1, float w, int c) {
		if (t1 - t0 < px) return;
		line((float) (cx + nx * o + dx * t0), (float) (cy + ny * o + dy * t0), (float) (cx + nx * o + dx * t1), (float) (cy + ny * o + dy * t1), w, c);
	}

	// ---------------------------------------------------------------- lines, triangles and boxes

	/** A straight line w wide, its long edges smoothed. */
	void line(float x0, float y0, float x1, float y1, float w, int c) {
		if (w < px) {
			c = fade(c, w / px);
			w = px;
		}
		float dx = x1 - x0, dy = y1 - y0, len = (float) Math.sqrt(dx * dx + dy * dy);
		if (len < 1e-4f) return;
		float nx = -dy / len, ny = dx / len, h = px / 2, in = Math.max(0, w / 2 - h), out = w / 2 + h;
		int clear = fade(c, 0);
		if (in > 0) quad(x0 - nx * in, y0 - ny * in, c, x1 - nx * in, y1 - ny * in, c, x1 + nx * in, y1 + ny * in, c, x0 + nx * in, y0 + ny * in, c);
		quad(x0 + nx * in, y0 + ny * in, c, x1 + nx * in, y1 + ny * in, c, x1 + nx * out, y1 + ny * out, clear, x0 + nx * out, y0 + ny * out, clear);
		quad(x0 - nx * in, y0 - ny * in, c, x1 - nx * in, y1 - ny * in, c, x1 - nx * out, y1 - ny * out, clear, x0 - nx * out, y0 - ny * out, clear);
	}

	/** A filled triangle, its edges smoothed. */
	void tri(float x0, float y0, float x1, float y1, float x2, float y2, int c) {
		quad(x0, y0, c, x1, y1, c, x2, y2, c, x2, y2, c);
		float[] p = {x0, y0, x1, y1, x2, y2};
		float mx = (x0 + x1 + x2) / 3, my = (y0 + y1 + y2) / 3;
		int clear = fade(c, 0);
		for (int i = 0; i < 3; i++) {
			float ax = p[i * 2], ay = p[i * 2 + 1], bx = p[(i + 1) % 3 * 2], by = p[(i + 1) % 3 * 2 + 1];
			float dx = bx - ax, dy = by - ay, len = (float) Math.sqrt(dx * dx + dy * dy);
			if (len < 1e-4f) continue;
			float nx = -dy / len * px, ny = dx / len * px;
			if (nx * (ax - mx) + ny * (ay - my) < 0) {
				nx = -nx;
				ny = -ny;
			}
			quad(ax, ay, c, bx, by, c, bx + nx, by + ny, clear, ax + nx, ay + ny, clear);
		}
	}

	/** A plain box, top colour to bottom colour (its edges sit on whole lines, so they need no smoothing). */
	void rect(float x0, float y0, float x1, float y1, int top, int bottom) {
		quad(x0, y0, top, x0, y1, bottom, x1, y1, bottom, x1, y0, top);
	}

	/** A box with rounded corners of radius rad, its edges smoothed. */
	void rrect(float x0, float y0, float x1, float y1, float rad, int c) {
		rad = Math.max(0, Math.min(rad, Math.min(x1 - x0, y1 - y0) / 2));
		float h = px / 2;
		if (rad < px) {
			rect(x0, y0, x1, y1, c, c);
			return;
		}
		int clear = fade(c, 0);
		rect(x0 + rad, y0 + h, x1 - rad, y1 - h, c, c);
		rect(x0 + h, y0 + rad, x0 + rad, y1 - rad, c, c);
		rect(x1 - rad, y0 + rad, x1 - h, y1 - rad, c, c);
		// Smoothed straight edges.
		quad(x0 + rad, y0 - h, clear, x0 + rad, y0 + h, c, x1 - rad, y0 + h, c, x1 - rad, y0 - h, clear);
		quad(x0 + rad, y1 - h, c, x0 + rad, y1 + h, clear, x1 - rad, y1 + h, clear, x1 - rad, y1 - h, c);
		quad(x0 - h, y0 + rad, clear, x0 - h, y1 - rad, clear, x0 + h, y1 - rad, c, x0 + h, y0 + rad, c);
		quad(x1 - h, y0 + rad, c, x1 - h, y1 - rad, c, x1 + h, y1 - rad, clear, x1 + h, y0 + rad, clear);
		// Corners.
		double q = Math.PI / 2;
		band(x0 + rad, y0 + rad, 0, rad, -q, 0, flat(c), false, true);
		band(x1 - rad, y0 + rad, 0, rad, 0, q, flat(c), false, true);
		band(x1 - rad, y1 - rad, 0, rad, q, 2 * q, flat(c), false, true);
		band(x0 + rad, y1 - rad, 0, rad, 2 * q, 3 * q, flat(c), false, true);
	}

	// ---------------------------------------------------------------- to the screen

	private void quad(float x0, float y0, int c0, float x1, float y1, int c1, float x2, float y2, int c2, float x3, float y3, int c3) {
		// Wound the same way as the game's own rectangles.
		double area = (x0 * y1 - x1 * y0) + (x1 * y2 - x2 * y1) + (x2 * y3 - x3 * y2) + (x3 * y0 - x0 * y3);
		if (area > 0) {
			float tx = x1, ty = y1;
			int tc = c1;
			x1 = x3;
			y1 = y3;
			c1 = c3;
			x3 = tx;
			y3 = ty;
			c3 = tc;
		}
		if ((quads + 1) * 8 > xy.length) {
			xy = Arrays.copyOf(xy, xy.length * 2);
			colour = Arrays.copyOf(colour, colour.length * 2);
		}
		if (alpha < 1) {
			c0 = fade(c0, alpha);
			c1 = fade(c1, alpha);
			c2 = fade(c2, alpha);
			c3 = fade(c3, alpha);
		}
		int i = quads * 8, j = quads * 4;
		xy[i] = x0;
		xy[i + 1] = y0;
		xy[i + 2] = x1;
		xy[i + 3] = y1;
		xy[i + 4] = x2;
		xy[i + 5] = y2;
		xy[i + 6] = x3;
		xy[i + 7] = y3;
		colour[j] = c0;
		colour[j + 1] = c1;
		colour[j + 2] = c2;
		colour[j + 3] = c3;
		quads++;
	}

	/** Hands everything drawn so far to the GUI, then starts again. */
	void submit(GuiGraphics g) {
		if (quads == 0) return;
		g.guiRenderState.submitGuiElement(new Shapes(new Matrix3x2f(g.pose()), Arrays.copyOf(xy, quads * 8), Arrays.copyOf(colour, quads * 4), quads,
				new ScreenRectangle(0, 0, g.guiWidth(), g.guiHeight())));
		quads = 0;
	}

	private record Shapes(Matrix3x2f pose, float[] xy, int[] colour, int quads, ScreenRectangle bounds) implements GuiElementRenderState {
		@Override
		public void buildVertices(VertexConsumer out) {
			for (int q = 0; q < quads; q++) {
				for (int k = 0; k < 4; k++) {
					int i = q * 4 + k;
					out.addVertexWith2DPose(pose, xy[i * 2], xy[i * 2 + 1]).setColor(colour[i]);
				}
			}
		}

		@Override
		public RenderPipeline pipeline() {
			return RenderPipelines.GUI;
		}

		@Override
		public TextureSetup textureSetup() {
			return TextureSetup.noTexture();
		}

		@Override
		public ScreenRectangle scissorArea() {
			return null;
		}
	}

	// ---------------------------------------------------------------- colours

	/** Mixes two colours (0xAARRGGBB). */
	static int mix(int a, int b, double t) {
		t = Math.max(0, Math.min(1, t));
		int out = 0;
		for (int s = 0; s < 32; s += 8) {
			int x = (a >>> s) & 0xFF, y = (b >>> s) & 0xFF;
			out |= ((int) Math.round(x + (y - x) * t) & 0xFF) << s;
		}
		return out;
	}

	/** The same colour with its alpha scaled. */
	static int fade(int c, double f) {
		int a = (int) Math.round(((c >>> 24) & 0xFF) * Math.max(0, Math.min(1, f)));
		return (a << 24) | (c & 0xFFFFFF);
	}
}
