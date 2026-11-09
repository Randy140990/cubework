package com.insidethecube.cubework.client;

import java.util.Arrays;

/**
 * Repaints a wooden door in a dye colour, keeping its pattern: every wood pixel keeps how much lighter or
 * darker it is than the door's usual wood, so grain, panels and shadows stay; metal (hinges, handles,
 * bands), which is greyer than the wood or a different colour, is left exactly as it is.
 */
public final class Recolour {
	private Recolour() {}

	/** Hue (0 to 1), saturation and lightness of each dye's target colour. Greys have no saturation. */
	private static final double[][] TARGET = new double[16][];

	static {
		String[] names = {"white", "light_gray", "gray", "black", "brown", "red", "orange", "yellow", "lime", "green", "cyan", "light_blue", "blue",
				"purple", "magenta", "pink"};
		int[] rgb = {0xF9FFFE, 0x9D9D97, 0x474F52, 0x1D1D21, 0x835432, 0xB02E26, 0xF9801D, 0xFED83D, 0x80C71F, 0x5E7C16, 0x169C9C, 0x3AB3DA, 0x3C44AA,
				0x8932B8, 0xC74EBD, 0xF38BAA};
		for (int i = 0; i < 16; i++) {
			double[] hsl = hsl(rgb[i]);
			switch (names[i]) {
				case "white" -> hsl = new double[] {0, 0, 0.86};
				case "light_gray" -> hsl = new double[] {0, 0, 0.62};
				case "gray" -> hsl = new double[] {0, 0, 0.36};
				case "black" -> hsl = new double[] {0, 0, 0.17};
				default -> hsl[2] = Math.min(0.82, Math.max(0.18, hsl[2]));
			}
			TARGET[i] = hsl;
		}
	}

	public static int index(String colour) {
		String[] names = {"white", "light_gray", "gray", "black", "brown", "red", "orange", "yellow", "lime", "green", "cyan", "light_blue", "blue",
				"purple", "magenta", "pink"};
		for (int i = 0; i < names.length; i++) if (names[i].equals(colour)) return i;
		return 0;
	}

	/** Recolours ARGB pixels in place; all: every pixel is recoloured (a metal door), not only the wood. */
	public static void apply(int[] argb, int colour, boolean all) {
		int n = 0;
		double[][] hsl = new double[argb.length][];
		for (int i = 0; i < argb.length; i++) {
			if ((argb[i] >>> 24) == 0) continue;
			hsl[i] = hsl(argb[i] & 0xFFFFFF);
			n++;
		}
		if (n == 0) return;
		double[] sats = new double[n];
		double sx = 0, sy = 0;
		int k = 0;
		for (double[] p : hsl) {
			if (p == null) continue;
			sats[k++] = p[1];
			sx += p[1] * Math.cos(2 * Math.PI * p[0]);
			sy += p[1] * Math.sin(2 * Math.PI * p[0]);
		}
		double satMedian = median(sats);
		double hue = ((Math.atan2(sy, sx) / (2 * Math.PI)) % 1 + 1) % 1;

		boolean[] wood = new boolean[argb.length];
		double[] woodL = new double[n], woodS = new double[n];
		int w = 0;
		for (int i = 0; i < argb.length; i++) {
			double[] p = hsl[i];
			if (p == null) continue;
			if (all || isWood(p, hue, satMedian)) {
				wood[i] = true;
				woodL[w] = p[2];
				woodS[w] = p[1];
				w++;
			}
		}
		if (w == 0) return;
		double lMedian = median(Arrays.copyOf(woodL, w));
		double sMedian = median(Arrays.copyOf(woodS, w));

		double[] t = TARGET[colour];
		double room = Math.min(t[2], 1 - t[2]) / Math.max(0.05, Math.min(lMedian, 1 - lMedian));
		double contrast = Math.min(1.6, Math.max(0.6, room));
		for (int i = 0; i < argb.length; i++) {
			if (!wood[i]) continue;
			double[] p = hsl[i];
			double l = clamp(t[2] + (p[2] - lMedian) * contrast);
			double s = t[1] > 0.05 && !all ? clamp(t[1] * (0.6 + 0.4 * (p[1] / Math.max(0.01, sMedian)))) : t[1];
			argb[i] = (argb[i] & 0xFF000000) | rgb(t[0], s, l);
		}
	}

	private static boolean isWood(double[] p, double hue, double satMedian) {
		if (satMedian > 0.06 && p[1] < 0.45 * satMedian) return false;
		double d = Math.abs(p[0] - hue);
		d = Math.min(d, 1 - d);
		return !(p[1] > 0.15 && d > 0.12);
	}

	private static double median(double[] v) {
		double[] c = v.clone();
		Arrays.sort(c);
		return c[c.length / 2];
	}

	private static double clamp(double v) {
		return Math.max(0, Math.min(1, v));
	}

	/** RGB to hue, saturation, lightness (all 0 to 1). */
	static double[] hsl(int rgb) {
		double r = ((rgb >> 16) & 255) / 255.0, g = ((rgb >> 8) & 255) / 255.0, b = (rgb & 255) / 255.0;
		double max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
		double l = (max + min) / 2, h = 0, s = 0;
		if (max != min) {
			double d = max - min;
			s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
			if (max == r) h = ((g - b) / d + (g < b ? 6 : 0)) / 6;
			else if (max == g) h = ((b - r) / d + 2) / 6;
			else h = ((r - g) / d + 4) / 6;
		}
		return new double[] {h, s, l};
	}

	static int rgb(double h, double s, double l) {
		double r, g, b;
		if (s == 0) {
			r = g = b = l;
		} else {
			double q = l < 0.5 ? l * (1 + s) : l + s - l * s, p = 2 * l - q;
			r = hue(p, q, h + 1.0 / 3);
			g = hue(p, q, h);
			b = hue(p, q, h - 1.0 / 3);
		}
		return ((int) Math.round(r * 255) << 16) | ((int) Math.round(g * 255) << 8) | (int) Math.round(b * 255);
	}

	private static double hue(double p, double q, double t) {
		if (t < 0) t += 1;
		if (t > 1) t -= 1;
		if (t < 1.0 / 6) return p + (q - p) * 6 * t;
		if (t < 1.0 / 2) return q;
		if (t < 2.0 / 3) return p + (q - p) * (2.0 / 3 - t) * 6;
		return p;
	}
}
