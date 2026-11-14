package com.insidethecube.cubework.block;

import java.util.ArrayList;
import java.util.List;

/**
 * Curved Stairs: a flight of stairs with an S-shaped underside, shared by the model
 * (client.StairCurveModel) and the hit box.
 * <p>
 * Seen from the side, with a flight of W stairs climbing to the right one block per block and x measured
 * in blocks from the front of the bottom stair, the steps are ordinary stairs and the underside is a wave
 * (see curve): flaring off the floor, tucking in close under the steps, swelling out and tucking in again,
 * always rising.
 * <p>
 * Pieces: column c (0 to W - 1, the c-th stair from the bottom), row r (0 to c, blocks up from the
 * floor); row c is the stair itself (steps on top, cut by the curve), the rows below are the fill under it.
 * Two extra pieces: an ordinary stair and a solid block, for the lower part of flights longer than six.
 */
public final class StairCurveGeometry {
	private StairCurveGeometry() {}

	/** Longest flight the curve is drawn for (longer flights curve under their top six stairs). */
	public static final int MAX = 6;

	/** Every piece: {flight length, column, row}; PLAIN and SOLID are {-1,..} and {-2,..}. */
	public static final int[][] PIECES;
	public static final int PLAIN, SOLID;

	static {
		List<int[]> list = new ArrayList<>();
		for (int w = 2; w <= MAX; w++) for (int c = 0; c < w; c++) for (int r = 0; r <= c; r++) list.add(new int[] {w, c, r});
		PLAIN = list.size();
		list.add(new int[] {-1, 0, 0});
		SOLID = list.size();
		list.add(new int[] {-2, 0, 0});
		PIECES = list.toArray(new int[0][]);
	}

	public static int index(int w, int c, int r) {
		for (int i = 0; i < PIECES.length; i++) if (PIECES[i][0] == w && PIECES[i][1] == c && PIECES[i][2] == r) return i;
		return -1;
	}

	/** The thinnest the flight gets: the underside's closest approach to the steps' inner corners. */
	private static final double THIN = 0.25;
	/** How round the flare where the underside meets the floor is. */
	private static final double FLARE = 0.35;

	/** The length of one wave (thin, thick, thin) and how far it swings, for a flight of w stairs. */
	private static double[] wave(int w) {
		double len = w - THIN;
		int waves = Math.max(1, (int) Math.round(len / 3.5));
		double period = len / waves;
		// Never so steep the other way that the underside would dip: it always rises.
		double swing = Math.min(0.45, 0.8 * period / (2 * Math.PI));
		return new double[] {period, swing};
	}

	/** The larger of a and b, rounded off where they meet (k: how round). */
	private static double smoothMax(double a, double b, double k) {
		double h = Math.max(k - Math.abs(a - b), 0) / k;
		return Math.max(a, b) + h * h * k * 0.25;
	}

	/**
	 * Height of the underside (blocks above the floor) at x blocks from the front of the bottom stair: the
	 * line through the steps' inner corners, less a depth that waves thin, thick, thin (the underside tucks
	 * in close under the steps, swells out, and tucks in again, as in the curved-stair builds). It always
	 * rises, never sagging; a flight long enough gets more than one wave, always ending tucked in under the
	 * top stair. Where it meets the floor it flares out smoothly.
	 */
	public static double curve(int w, double x) {
		if (x <= 0) return 0;
		double[] wv = wave(w);
		double s = Math.max(0, x - THIN) / wv[0];
		double depth = THIN + wv[1] - wv[1] * Math.cos(2 * Math.PI * s);
		double raw = x - depth;
		return Math.max(0, Math.min(smoothMax(raw, 0, FLARE), x - 0.2));
	}

	public static boolean stairCell(int piece) {
		int[] p = PIECES[piece];
		return p[0] == -1 || (p[0] > 0 && p[1] == p[2]);
	}

	/** The top of the piece at x (0 at its low side, 1 at its high side): a stair's two steps, or the block's top. */
	public static double top(int piece, double x) {
		return stairCell(piece) ? (x < 0.5 ? 0.5 : 1.0) : 1.0;
	}

	/** Whether a fill piece lies (almost) wholly in the hollow under the curve, where nothing belongs. */
	public static boolean hollow(int piece) {
		int[] p = PIECES[piece];
		if (p[0] < 0 || p[1] == p[2]) return false;
		double lowest = Double.MAX_VALUE;
		for (int i = 0; i <= 32; i++) lowest = Math.min(lowest, curve(p[0], p[1] + i / 32.0) - p[2]);
		return lowest >= 0.95;
	}

	/**
	 * Height of the piece's underside at x, 0 to 1 (1: nothing there, where the curve has left the block
	 * through its top). A piece wholly in the hollow (one left there after the flight changed) keeps a thin
	 * top so it can still be seen and broken.
	 */
	public static double under(int piece, double x) {
		int[] p = PIECES[piece];
		if (p[0] < 0) return 0;
		return Math.max(0, Math.min(hollow(piece) ? 0.875 : 1.0, curve(p[0], p[1] + x) - p[2]));
	}

	/** Points along the underside, x from 0 to 1, with every place it touches the floor or the top as a point. */
	public static double[][] points(int piece) {
		int[] p = PIECES[piece];
		List<Double> xs = new ArrayList<>();
		int n = 24;
		for (int i = 0; i <= n; i++) xs.add((double) i / n);
		if (stairCell(piece)) xs.add(0.5);
		if (p[0] > 0) {
			int w = p[0], c = p[1], r = p[2];
			double cap = hollow(piece) ? 0.875 : 1.0;
			for (double level : new double[] {r, r + cap}) {
				for (int i = 0; i < n; i++) {
					double a = (double) i / n, b = (double) (i + 1) / n;
					double fa = curve(w, c + a) - level, fb = curve(w, c + b) - level;
					if ((fa < 0) == (fb < 0)) continue;
					for (int k = 0; k < 30; k++) {
						double m = (a + b) / 2, fm = curve(w, c + m) - level;
						if ((fm < 0) == (fa < 0)) {
							a = m;
							fa = fm;
						} else {
							b = m;
						}
					}
					xs.add((a + b) / 2);
				}
			}
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
			double h = Math.max(under(piece, x / 16.0), Math.max(under(piece, (x + 0.5) / 16.0), under(piece, (x + 1) / 16.0)));
			double t = top(piece, (x + 0.5) / 16.0);
			for (int y = 0; y < 16; y++) m[x][y] = (y + 1) / 16.0 > h + 1e-9 && y / 16.0 < t - 1e-9;
		}
		return m;
	}

	/** The pixels solid all over (for hiding neighbours' faces). */
	public static boolean[][] inside(int piece) {
		boolean[][] m = new boolean[16][16];
		for (int x = 0; x < 16; x++) {
			double h = Math.max(under(piece, x / 16.0), Math.max(under(piece, (x + 0.5) / 16.0), under(piece, (x + 1) / 16.0)));
			double t = top(piece, (x + 0.5) / 16.0);
			for (int y = 0; y < 16; y++) m[x][y] = y / 16.0 >= h - 1e-9 && (y + 1) / 16.0 <= t + 1e-9;
		}
		return m;
	}
}
