package com.insidethecube.retromon.client;

import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * (0.74) John's 28 Nov 10:56, the MAGNET TRAIN: "when you do the animation of it going away, it will go towards those
 * tunnels and disappear into it, not clipping out the other side. That's the main thing to get right." What's drawn
 * through this is cut off at a wall across x (the tunnel's dark back): every face of the model is cut where it crosses
 * it (its picture cut with it, not squeezed), and nothing past it is drawn, so the train goes into the dark and is gone,
 * and comes out of it the same way at the other end.
 *
 * <p>The faces come in as quads (four corners, as the model's cubes give them); each is cut against the wall and sent
 * on as one or two quads.
 */
final class ClipConsumer implements VertexConsumer {
	private final VertexConsumer out;
	/** Where the wall is (x, in the drawing's space) and which side of it is cut off (+1: everything past it in +x). */
	private final float wall;
	private final int side;
	// (Each corner: x y z, then the colour, the overlay and the light as their own ints, u v, then the normal.)
	private final float[][] quad = new float[4][11];
	private final int[][] quadI = new int[4][3];
	private int n;
	private final float[] cur = new float[11];
	private final int[] curI = new int[3];
	private boolean open;

	ClipConsumer(VertexConsumer out, float wall, int side) {
		this.out = out;
		this.wall = wall;
		this.side = side;
	}

	// ---- one corner at a time (x y z, colour, u v, overlay, light, normal) ----

	public VertexConsumer addVertex(float x, float y, float z) {
		if (open) commit();
		cur[0] = x;
		cur[1] = y;
		cur[2] = z;
		curI[0] = 0xFFFFFFFF;
		open = true;
		return this;
	}

	public VertexConsumer setColor(int r, int g, int b, int a) {
		curI[0] = a << 24 | r << 16 | g << 8 | b;
		return this;
	}

	public VertexConsumer setColor(int argb) {
		curI[0] = argb;
		return this;
	}

	public VertexConsumer setUv(float u, float v) {
		cur[4] = u;
		cur[5] = v;
		return this;
	}

	public VertexConsumer setUv1(int u, int v) {
		curI[1] = u & 0xFFFF | v << 16;
		return this;
	}

	public VertexConsumer setUv2(int u, int v) {
		curI[2] = u & 0xFFFF | v << 16;
		return this;
	}

	public VertexConsumer setNormal(float x, float y, float z) {
		cur[8] = x;
		cur[9] = y;
		cur[10] = z;
		commit();
		return this;
	}

	public VertexConsumer setLineWidth(float w) {
		return this;
	}

	/** A whole corner at once (as the model's cubes send them). */
	public void addVertex(float x, float y, float z, int color, float u, float v, int overlay, int light, float nx, float ny, float nz) {
		if (open) commit();
		cur[0] = x;
		cur[1] = y;
		cur[2] = z;
		curI[0] = color;
		curI[1] = overlay;
		curI[2] = light;
		cur[4] = u;
		cur[5] = v;
		cur[8] = nx;
		cur[9] = ny;
		cur[10] = nz;
		open = true;
		commit();
	}

	private void commit() {
		open = false;
		System.arraycopy(cur, 0, quad[n], 0, 11);
		System.arraycopy(curI, 0, quadI[n], 0, 3);
		if (++n == 4) {
			n = 0;
			face();
		}
	}

	// ---- a face: cut at the wall ----

	private float past(float[] v) {
		return side * (v[0] - wall);
	}

	private void face() {
		boolean all = true, none = true;
		for (float[] v : quad) {
			if (past(v) > 0) all = false;
			else none = false;
		}
		if (none) return;
		if (all) {
			for (int i = 0; i < 4; i++) emit(quad[i], quadI[i]);
			return;
		}
		float[][] poly = new float[6][];
		int[][] polyI = new int[6][];
		int m = 0;
		for (int i = 0; i < 4; i++) {
			float[] a = quad[i], b = quad[(i + 1) % 4];
			float da = past(a), db = past(b);
			if (da <= 0) {
				polyI[m] = quadI[i];
				poly[m++] = a.clone();
			}
			if ((da <= 0) != (db <= 0)) {
				float t = da / (da - db);
				float[] c = a.clone();
				for (int k : new int[] {0, 1, 2, 4, 5}) c[k] = a[k] + (b[k] - a[k]) * t;
				polyI[m] = quadI[i];
				poly[m++] = c;
			}
		}
		if (m < 3) return;
		int[] order = m == 3 ? new int[] {0, 1, 2, 2} : m == 4 ? new int[] {0, 1, 2, 3} : new int[] {0, 1, 2, 3, 0, 3, 4, 4};
		for (int i : order) emit(poly[i], polyI[i]);
	}

	private void emit(float[] v, int[] w) {
		out.addVertex(v[0], v[1], v[2]).setColor(w[0]).setUv(v[4], v[5]).setOverlay(w[1]).setLight(w[2]).setNormal(v[8], v[9], v[10]);
	}
}
