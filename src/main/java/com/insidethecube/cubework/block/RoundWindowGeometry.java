package com.insidethecube.cubework.block;

import java.util.ArrayList;
import java.util.List;

/**
 * The opening of a round window frame, shared by the model (client.RoundWindowModel) and the hit box. Seen
 * from the front (x across from the viewer's left when the frame faces north, y up), in pixels, 0 to 16.
 * <p>
 * Like a square frame, the opening is 8 x 8 inside a 4-pixel border and reaches the edge of the block where
 * another frame joins on. A corner whose two sides are both closed is rounded: across half the opening when
 * the far side is closed too, or all of it when the far side joins on. So one frame is a round porthole, two
 * side by side an oval, a square of four one big circle, and a stack of three a tall window with round ends.
 */
public final class RoundWindowGeometry {
	private RoundWindowGeometry() {}

	/** The opening's outline, anticlockwise (seen from the front), in pixels. */
	public static double[][] opening(boolean up, boolean down, boolean left, boolean right) {
		double x0 = left ? 0 : 4, x1 = right ? 16 : 12, y0 = down ? 0 : 4, y1 = up ? 16 : 12;
		double w = x1 - x0, h = y1 - y0;
		List<double[]> out = new ArrayList<>();
		// Bottom left, bottom right, top right, top left; each rounded when both its sides are closed.
		corner(out, !left && !down, x0, y0, right ? w : w / 2, up ? h : h / 2, Math.PI, 1);
		corner(out, !right && !down, x1, y0, left ? w : w / 2, up ? h : h / 2, 1.5 * Math.PI, 2);
		corner(out, !right && !up, x1, y1, left ? w : w / 2, down ? h : h / 2, 0, 3);
		corner(out, !left && !up, x0, y1, right ? w : w / 2, down ? h : h / 2, 0.5 * Math.PI, 4);
		// Drop points that repeat (where one rounded corner runs right into the next).
		List<double[]> clean = new ArrayList<>();
		for (double[] p : out) {
			if (!clean.isEmpty() && same(clean.get(clean.size() - 1), p)) continue;
			clean.add(p);
		}
		while (clean.size() > 1 && same(clean.get(0), clean.get(clean.size() - 1))) clean.remove(clean.size() - 1);
		return clean.toArray(new double[0][]);
	}

	/**
	 * One corner of the opening at (cx, cy): the square corner, or a quarter of an ellipse with radii rx, ry
	 * starting at angle from (anticlockwise). which: 1 bottom left, 2 bottom right, 3 top right, 4 top left.
	 */
	private static void corner(List<double[]> out, boolean round, double cx, double cy, double rx, double ry, double from, int which) {
		if (!round) {
			out.add(new double[] {cx, cy});
			return;
		}
		// The ellipse's centre is inside the opening, rx and ry in from the corner.
		double ex = which == 1 || which == 4 ? cx + rx : cx - rx;
		double ey = which == 1 || which == 2 ? cy + ry : cy - ry;
		int n = Math.max(4, Math.min(12, (int) Math.round(Math.max(rx, ry) * 0.75) + 2));
		for (int i = 0; i <= n; i++) {
			double t = from + 0.5 * Math.PI * i / n;
			double x = ex + rx * Math.cos(t), y = ey + ry * Math.sin(t);
			out.add(new double[] {snap(x), snap(y)});
		}
	}

	private static double snap(double v) {
		double r = Math.rint(v);
		return Math.abs(v - r) < 1e-9 ? r : v;
	}

	private static boolean same(double[] a, double[] b) {
		return Math.abs(a[0] - b[0]) < 1e-6 && Math.abs(a[1] - b[1]) < 1e-6;
	}

	/** Whether a point (pixels) is inside the opening. */
	public static boolean inside(double[][] poly, double x, double y) {
		boolean in = false;
		for (int i = 0, j = poly.length - 1; i < poly.length; j = i++) {
			double xi = poly[i][0], yi = poly[i][1], xj = poly[j][0], yj = poly[j][1];
			if ((yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi) in = !in;
		}
		return in;
	}

	/** The frame's solid pixels (16 x 16, as Profiles), for the hit box. */
	public static boolean[][] mask(boolean up, boolean down, boolean left, boolean right) {
		double[][] poly = opening(up, down, left, right);
		boolean[][] m = new boolean[16][16];
		for (int x = 0; x < 16; x++) for (int y = 0; y < 16; y++) m[x][y] = !inside(poly, x + 0.5, y + 0.5);
		return m;
	}
}
