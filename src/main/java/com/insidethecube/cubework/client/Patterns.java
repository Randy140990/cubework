package com.insidethecube.cubework.client;

/**
 * Lays a pattern over a block texture (any size, square): tiled (four tiles with grout between), checkered
 * (alternate squares darker, or lighter on dark blocks), herringbone (bricks cut from the texture, laid
 * in a herringbone) and bordered (a framed panel). A texture pack's own textures are used, so packs carry
 * over. Shader maps: herringbone moves pixels, so its normal and specular maps are moved the same way;
 * the others keep every pixel where it was, so they use the block's own maps.
 */
public final class Patterns {
	private Patterns() {}

	/** What the pixels are: the colour texture, a normal map, or another shader map. */
	public enum Kind { COLOUR, NORMAL, OTHER }

	/** Whether a pattern moves pixels (so its shader maps have to be made, not borrowed). */
	public static boolean moves(String pattern) {
		return pattern.equals("herringbone");
	}

	/** The patterned pixels (0xAARRGGBB, rows of w), from a w by w texture. */
	public static int[] apply(int[] src, int w, String pattern, Kind kind) {
		int u = Math.max(1, w / 16);
		int[] out = new int[w * w];
		switch (pattern) {
			case "herringbone" -> herringbone(src, out, w, u, kind);
			case "tiled" -> {
				int t = 8 * u;
				for (int y = 0; y < w; y++) for (int x = 0; x < w; x++) {
					int px = src[y * w + x], ix = x % t, iy = y % t;
					double f = 1;
					if (kind == Kind.COLOUR) {
						if (ix < u || iy < u) f = 0.62;
						else if (ix < 2 * u || iy < 2 * u) f = 1.1;
						else if (ix >= t - u || iy >= t - u) f = 0.88;
					}
					out[y * w + x] = shade(px, f);
				}
			}
			case "checkered" -> {
				int t = 8 * u;
				double f = luminance(src) < 0.3 ? 1.35 : 0.78;
				for (int y = 0; y < w; y++) for (int x = 0; x < w; x++) {
					boolean odd = ((x / t) + (y / t)) % 2 == 1;
					out[y * w + x] = kind == Kind.COLOUR && odd ? shade(src[y * w + x], f) : src[y * w + x];
				}
			}
			default -> {
				// Bordered: a dark edge, then a light bevel inside it.
				for (int y = 0; y < w; y++) for (int x = 0; x < w; x++) {
					int e = Math.min(Math.min(x, y), Math.min(w - 1 - x, w - 1 - y));
					double f = kind != Kind.COLOUR ? 1 : e < u ? 0.6 : e < 2 * u ? 1.12 : 1;
					out[y * w + x] = shade(src[y * w + x], f);
				}
			}
		}
		return out;
	}

	/**
	 * Bricks two cells long and one wide (a cell is a quarter of the texture), laid so each brick's ends butt
	 * against bricks the other way. Cell (cx, cy) is part of an across brick when (cx - cy) mod 4 is 0 or 1
	 * and of an upright one when it's 2 or 3, which repeats every 4 cells both ways.
	 */
	private static void herringbone(int[] src, int[] out, int w, int u, Kind kind) {
		int c = 4 * u;
		for (int y = 0; y < w; y++) for (int x = 0; x < w; x++) {
			int cx = x / c, cy = y / c, s = Math.floorMod(cx - cy, 4);
			boolean across = s < 2;
			int sx = across ? (s == 0 ? cx : cx - 1) : cx;
			int sy = across ? cy : (s == 3 ? cy : cy - 1);
			int bx = sx * c, by = sy * c;
			// Along the brick and across it.
			int along = across ? Math.floorMod(x - bx, w) : Math.floorMod(y - by, w);
			int side = across ? Math.floorMod(y - by, w) : Math.floorMod(x - bx, w);
			// Each brick is cut from its own part of the texture, so they don't all look the same.
			int ox = Math.floorMod((sx * 5 + sy * 3) * c, w), oy = Math.floorMod((sx * 2 + sy * 7) * c, w);
			int px = src[Math.floorMod(oy + side, w) * w + Math.floorMod(ox + along, w)];
			if (!across && kind == Kind.NORMAL) px = swapNormal(px);
			boolean grout = side >= c - u || along >= 2 * c - u;
			out[y * w + x] = kind == Kind.COLOUR && grout ? shade(px, 0.62) : px;
		}
	}

	/** Upright bricks are the texture turned over its diagonal, so a normal map's x and y swap (shader packs use DirectX-style maps). */
	private static int swapNormal(int px) {
		int a = px >>> 24, r = (px >> 16) & 0xFF, g = (px >> 8) & 0xFF, b = px & 0xFF;
		return (a << 24) | (g << 16) | (r << 8) | b;
	}

	private static int shade(int px, double f) {
		if (f == 1) return px;
		int a = px >>> 24;
		int r = clamp(((px >> 16) & 0xFF) * f), g = clamp(((px >> 8) & 0xFF) * f), b = clamp((px & 0xFF) * f);
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	private static int clamp(double v) {
		return (int) Math.max(0, Math.min(255, Math.round(v)));
	}

	private static double luminance(int[] px) {
		double sum = 0;
		for (int p : px) sum += (0.2126 * ((p >> 16) & 0xFF) + 0.7152 * ((p >> 8) & 0xFF) + 0.0722 * (p & 0xFF)) / 255;
		return px.length == 0 ? 0.5 : sum / px.length;
	}
}
