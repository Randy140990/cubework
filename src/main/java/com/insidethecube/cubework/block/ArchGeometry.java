package com.insidethecube.cubework.block;

/**
 * The smooth underside of arches, shared by the model (on the client) and the shape that hides
 * neighbours' faces. Seen from the front, x runs 0 to 1 across and y 0 to 1 up. An arch's underside is
 * half an ellipse from both bottom corners to 4 pixels below the top; an arch corner's is a quarter of
 * one, from its back (x = 0, against the wall) at the floor to 4 pixels below the top at its front (x = 1).
 */
public final class ArchGeometry {
	private ArchGeometry() {}

	/** The underside reaches this high (12 pixels): 4 pixels of solid stay above it. */
	public static final double HEIGHT = 0.75;

	public static double centre(boolean corner) {
		return corner ? 1 : 0.5;
	}

	public static double width(boolean corner) {
		return corner ? 1 : 0.5;
	}

	/** Height of the underside at x. */
	public static double under(boolean corner, double x) {
		double d = (x - centre(corner)) / width(corner);
		return d * d >= 1 ? 0 : HEIGHT * Math.sqrt(1 - d * d);
	}

	/** Points along the underside, x going from 0 to 1, spaced evenly round the curve. */
	public static double[][] curve(boolean corner) {
		int n = corner ? 10 : 16;
		double from = Math.PI, to = corner ? Math.PI / 2 : 0;
		double[][] out = new double[n + 1][];
		for (int i = 0; i <= n; i++) {
			double t = from + (to - from) * i / n;
			double x = centre(corner) + width(corner) * Math.cos(t), y = HEIGHT * Math.sin(t);
			if (i == 0) x = 0;
			if (i == n) x = 1;
			out[i] = new double[] {Math.max(0, Math.min(1, x)), Math.max(0, y)};
		}
		if (!corner) out[n][1] = 0;
		return out;
	}

	/** The pixels (16 x 16, as Profiles) that are solid all over: nothing seen through them may be hidden. */
	public static boolean[][] inside(boolean corner) {
		boolean[][] m = new boolean[16][16];
		for (int x = 0; x < 16; x++) {
			double x0 = x / 16.0, x1 = (x + 1) / 16.0, c = Math.max(x0, Math.min(x1, centre(corner)));
			double highest = Math.max(under(corner, c), Math.max(under(corner, x0), under(corner, x1)));
			for (int y = 0; y < 16; y++) m[x][y] = y / 16.0 >= highest - 1e-9;
		}
		return m;
	}
}
