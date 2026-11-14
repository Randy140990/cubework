package com.insidethecube.cubework.block;

import java.util.ArrayList;
import java.util.List;

/**
 * The curve under a flight of stairs, shared by the model (client.StairCurveModel) and the hit box.
 * <p>
 * A flight of W ordinary stairs climbs one block per block. Seen from the side, with the flight climbing to
 * the right and x measured in blocks from the front of the bottom stair, its underside follows
 * U(x) = (W - 1) * ((x - 1) / (W - 1))^2 from x = 1 to x = W: it runs along the floor under the first
 * stair, then sweeps up ever more steeply to meet the underside of the top stair. The stairs stay whole;
 * the curve lies in the blocks beneath them: column c (1 to W - 1, the column of the c-th stair above the
 * bottom one), row r (0 to c - 1, blocks up from the floor). Each piece is solid above the curve.
 */
public final class StairCurveGeometry {
	private StairCurveGeometry() {}

	/** Longest flight the curve is drawn for (longer flights curve under their top six stairs). */
	public static final int MAX = 6;

	/** Every piece: {flight length, column, row}. */
	public static final int[][] PIECES;

	static {
		List<int[]> list = new ArrayList<>();
		for (int w = 2; w <= MAX; w++) for (int c = 1; c < w; c++) for (int r = 0; r < c; r++) list.add(new int[] {w, c, r});
		PIECES = list.toArray(new int[0][]);
	}

	public static int index(int w, int c, int r) {
		for (int i = 0; i < PIECES.length; i++) if (PIECES[i][0] == w && PIECES[i][1] == c && PIECES[i][2] == r) return i;
		return -1;
	}

	/** The height of the curve (blocks above the floor) at x blocks from the front of the bottom stair. */
	public static double curve(int w, double x) {
		if (x <= 1) return 0;
		double t = (x - 1) / (w - 1);
		return (w - 1) * t * t;
	}

	/**
	 * Height of the piece's underside at x (0 at its low side, 1 at its high side), 0 to 1 (1: nothing
	 * there, where the curve has left the block through its top). A piece wholly in the hollow (one left
	 * there after the flight changed) keeps a thin top so it can still be seen and broken.
	 */
	public static double under(int piece, double x) {
		int[] p = PIECES[piece];
		return Math.max(0, Math.min(hollow(piece) ? 0.875 : 1.0, curve(p[0], p[1] + x) - p[2]));
	}

	/** Whether the piece lies (almost) wholly in the hollow under the curve, where nothing belongs. */
	public static boolean hollow(int piece) {
		int[] p = PIECES[piece];
		return curve(p[0], p[1]) - p[2] >= 0.95;
	}

	/** Points along the underside, x from 0 to 1, including where it leaves the floor and meets the top. */
	public static double[][] points(int piece) {
		int[] p = PIECES[piece];
		int w = p[0], c = p[1], r = p[2];
		List<Double> xs = new ArrayList<>();
		int n = 12;
		for (int i = 0; i <= n; i++) xs.add((double) i / n);
		// Where the curve crosses the bottom and the top of this block.
		for (double level : new double[] {r, r + (hollow(piece) ? 0.875 : 1.0)}) {
			if (level <= 0) continue;
			double x = 1 + (w - 1) * Math.sqrt(level / (w - 1)) - c;
			if (x > 0 && x < 1) xs.add(x);
		}
		xs.sort(Double::compare);
		List<double[]> out = new ArrayList<>();
		for (double x : xs) {
			if (!out.isEmpty() && x - out.get(out.size() - 1)[0] < 1e-6) continue;
			out.add(new double[] {x, under(piece, x)});
		}
		return out.toArray(new double[0][]);
	}

	/** The solid pixels (16 x 16, as Profiles: x across towards the high side, y up), for the hit box. */
	public static boolean[][] mask(int piece) {
		boolean[][] m = new boolean[16][16];
		for (int x = 0; x < 16; x++) {
			double h = Math.max(under(piece, x / 16.0), under(piece, (x + 1) / 16.0));
			for (int y = 0; y < 16; y++) m[x][y] = (y + 1) / 16.0 > h + 1e-9;
		}
		return m;
	}

	/** The pixels solid all over (for hiding neighbours' faces): every pixel the curve doesn't reach into. */
	public static boolean[][] inside(int piece) {
		boolean[][] m = new boolean[16][16];
		for (int x = 0; x < 16; x++) {
			double h = Math.max(under(piece, x / 16.0), under(piece, (x + 1) / 16.0));
			for (int y = 0; y < 16; y++) m[x][y] = y / 16.0 >= h - 1e-9;
		}
		return m;
	}
}
