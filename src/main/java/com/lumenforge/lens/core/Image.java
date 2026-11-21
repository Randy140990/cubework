package com.lumenforge.lens.core;

/** Picture resizing for photos: each output pixel is the average of the source pixels it covers (sharp, no shimmer). */
public final class Image {
	private Image() {}

	/** ARGB pixels from sw x sh to dw x dh. Shrinking averages; growing (rare) blends neighbours. */
	public static int[] resize(int[] src, int sw, int sh, int dw, int dh) {
		if (sw == dw && sh == dh) return src;
		float[] tmp = new float[dw * sh * 3];
		// Across.
		for (int y = 0; y < sh; y++) {
			int row = y * sw;
			for (int x = 0; x < dw; x++) {
				double a = x * (double) sw / dw, b = (x + 1) * (double) sw / dw;
				float[] c = sample(src, row, 1, sw, a, b);
				int o = (y * dw + x) * 3;
				tmp[o] = c[0];
				tmp[o + 1] = c[1];
				tmp[o + 2] = c[2];
			}
		}
		// Down.
		int[] out = new int[dw * dh];
		for (int x = 0; x < dw; x++) {
			for (int y = 0; y < dh; y++) {
				double a = y * (double) sh / dh, b = (y + 1) * (double) sh / dh;
				float r = 0, g = 0, bl = 0, wsum = 0;
				if (b - a <= 1) {
					double m = (a + b) / 2 - 0.5;
					int i0 = (int) Math.floor(m);
					double f = m - i0;
					int i1 = Math.min(sh - 1, Math.max(0, i0 + 1));
					i0 = Math.min(sh - 1, Math.max(0, i0));
					int o0 = (i0 * dw + x) * 3, o1 = (i1 * dw + x) * 3;
					r = (float) (tmp[o0] * (1 - f) + tmp[o1] * f);
					g = (float) (tmp[o0 + 1] * (1 - f) + tmp[o1 + 1] * f);
					bl = (float) (tmp[o0 + 2] * (1 - f) + tmp[o1 + 2] * f);
					wsum = 1;
				} else {
					for (int i = (int) Math.floor(a); i < Math.ceil(b) && i < sh; i++) {
						double w = Math.min(b, i + 1) - Math.max(a, i);
						if (w <= 0) continue;
						int o = (i * dw + x) * 3;
						r += tmp[o] * w;
						g += tmp[o + 1] * w;
						bl += tmp[o + 2] * w;
						wsum += w;
					}
				}
				out[y * dw + x] = 0xFF000000 | (clamp(r / wsum) << 16) | (clamp(g / wsum) << 8) | clamp(bl / wsum);
			}
		}
		return out;
	}

	private static float[] sample(int[] src, int row, int stride, int n, double a, double b) {
		float r = 0, g = 0, bl = 0, wsum = 0;
		if (b - a <= 1) {
			double m = (a + b) / 2 - 0.5;
			int i0 = (int) Math.floor(m);
			double f = m - i0;
			int i1 = Math.min(n - 1, Math.max(0, i0 + 1));
			i0 = Math.min(n - 1, Math.max(0, i0));
			int c0 = src[row + i0 * stride], c1 = src[row + i1 * stride];
			r = (float) (((c0 >> 16) & 255) * (1 - f) + ((c1 >> 16) & 255) * f);
			g = (float) (((c0 >> 8) & 255) * (1 - f) + ((c1 >> 8) & 255) * f);
			bl = (float) ((c0 & 255) * (1 - f) + (c1 & 255) * f);
			return new float[] {r, g, bl};
		}
		for (int i = (int) Math.floor(a); i < Math.ceil(b) && i < n; i++) {
			double w = Math.min(b, i + 1) - Math.max(a, i);
			if (w <= 0) continue;
			int c = src[row + i * stride];
			r += ((c >> 16) & 255) * w;
			g += ((c >> 8) & 255) * w;
			bl += (c & 255) * w;
			wsum += w;
		}
		return new float[] {r / wsum, g / wsum, bl / wsum};
	}

	private static int clamp(float v) {
		return Math.max(0, Math.min(255, Math.round(v)));
	}

	/** RGBA bytes read from OpenGL (bottom row first) to ARGB pixels, top row first. */
	public static int[] fromGl(java.nio.ByteBuffer rgba, int w, int h) {
		int[] px = new int[w * h];
		for (int y = 0; y < h; y++) {
			int src = (h - 1 - y) * w * 4, dst = y * w;
			for (int x = 0; x < w; x++) {
				int i = src + x * 4;
				int r = rgba.get(i) & 255, g = rgba.get(i + 1) & 255, b = rgba.get(i + 2) & 255;
				px[dst + x] = 0xFF000000 | (r << 16) | (g << 8) | b;
			}
		}
		return px;
	}
}
