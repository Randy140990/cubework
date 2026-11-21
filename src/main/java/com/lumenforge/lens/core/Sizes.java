package com.lumenforge.lens.core;

import java.util.Locale;

/** Output sizes (1K, 2K, 4K), the 16:9 crop, bitrates and file size guesses. */
public final class Sizes {
	private Sizes() {}

	/** A picture height for each size; 0 is the window's own size. */
	public enum Size {
		SCREEN("Screen", 0),
		K1("1K", 1080),
		K2("2K", 1440),
		K4("4K", 2160);

		public final String label;
		public final int height;

		Size(String label, int height) {
			this.label = label;
			this.height = height;
		}

		public static final Size[] VIDEO = {SCREEN, K1, K2, K4};
		public static final Size[] PHOTO = {SCREEN, K1, K2, K4};
	}

	/** The picture's shape. */
	public enum Shape {
		WIDE("16:9"),
		WINDOW("Window");

		public final String label;

		Shape(String label) {
			this.label = label;
		}
	}

	/** The part of a w x h picture that's used: x, y, width, height (the middle, at 16:9, for WIDE). */
	public static int[] crop(Shape shape, int w, int h) {
		if (shape != Shape.WIDE || w <= 0 || h <= 0) return new int[] {0, 0, w, h};
		double want = 16 / 9.0, have = w / (double) h;
		if (Math.abs(have - want) < 0.01) return new int[] {0, 0, w, h};
		if (have > want) {
			int cw = (int) Math.round(h * want);
			return new int[] {(w - cw) / 2, 0, cw, h};
		}
		int ch = (int) Math.round(w / want);
		return new int[] {0, (h - ch) / 2, w, ch};
	}

	/** The output size for a source picture of w x h: width and height, both even (encoders want that). */
	public static int[] output(Size size, Shape shape, int w, int h) {
		int[] c = crop(shape, w, h);
		int cw = c[2], ch = c[3];
		if (size == Size.SCREEN || size.height <= 0) return new int[] {even(cw), even(ch)};
		int oh = size.height;
		int ow = shape == Shape.WIDE ? oh * 16 / 9 : (int) Math.round(oh * (cw / (double) Math.max(1, ch)));
		return new int[] {even(ow), even(oh)};
	}

	static int even(int v) {
		return Math.max(2, v & ~1);
	}

	/**
	 * Megabits a second for a video: about 12 for 1080p at 30 fps on High in H.264, rising a little less than
	 * the pixel count and the frame rate. H.265 needs about 60% of that.
	 */
	public static double mbps(Quality q, int w, int h, int fps, boolean hevc) {
		double px = w * (double) h / (1920.0 * 1080);
		double v = 12 * Math.pow(px, 0.9) * Math.pow(fps / 30.0, 0.6) * q.factor;
		if (hevc) v *= 0.6;
		return Math.max(2, v);
	}

	/** Bytes of video for a minute of recording (picture and sound). */
	public static long bytesPerMinute(Quality q, int w, int h, int fps, boolean hevc) {
		return (long) ((mbps(q, w, h, fps, hevc) + 0.2) * 1_000_000 / 8 * 60);
	}

	/** 1.2 GB, 640 MB, 12 KB. */
	public static String bytes(long b) {
		if (b >= 1_000_000_000L) return String.format(Locale.ROOT, "%.1f GB", b / 1e9);
		if (b >= 1_000_000L) return String.format(Locale.ROOT, "%.0f MB", b / 1e6);
		return String.format(Locale.ROOT, "%.0f KB", Math.max(1, b / 1e3));
	}

	/** 01:02:03 or 02:03. */
	public static String clock(long ms) {
		long s = Math.max(0, ms / 1000);
		long hh = s / 3600, mm = s / 60 % 60, ss = s % 60;
		return hh > 0 ? String.format(Locale.ROOT, "%d:%02d:%02d", hh, mm, ss) : String.format(Locale.ROOT, "%02d:%02d", mm, ss);
	}
}
