package com.insidethecube.cubework.client;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import com.insidethecube.cubework.Material;
import com.insidethecube.cubework.block.RoundPillarBlock;

/**
 * One block of a big round pillar, drawn smooth: its share of the round side (the side texture wraps round,
 * about one block of texture per block of curve) and of the top and bottom (the top texture, laid as on
 * full blocks so it lines up across the layer).
 */
public final class RoundPillarModel {
	private RoundPillarModel() {}

	private static final double EPS = 1e-7;

	static Models.Built build(Material material, BlockState state, Function<String, TextureAtlasSprite> sprite) {
		Material.Textures t = material.textures();
		TextureAtlasSprite top = sprite.apply(t.top()), side = sprite.apply(t.side()), bottom = sprite.apply(t.bottom());
		RoundPillarBlock block = (RoundPillarBlock) state.getBlock();
		int n = block.size, part = state.getValue(RoundPillarBlock.PART);
		double ci = part % n, cj = part / n, r = n / 2.0;
		Map<Direction, List<BakedQuad>> sided = new EnumMap<>(Direction.class);
		List<BakedQuad> free = new ArrayList<>();

		// The circle as a polygon, 16 sides per block across; each side is a fifth of a texture wide.
		int segs = 16 * n;
		double[][] circle = new double[segs][];
		for (int k = 0; k < segs; k++) {
			double a = 2 * Math.PI * k / segs;
			circle[k] = new double[] {r + r * Math.cos(a), r + r * Math.sin(a)};
		}
		for (int k = 0; k < segs; k++) {
			double[] p = circle[k], q = circle[(k + 1) % segs];
			double[] c = clip(p, q, ci, cj);
			if (c == null) continue;
			double u0 = (k % 5) * 0.2, ua = u0 + 0.2 * c[0], ub = u0 + 0.2 * c[1];
			double ax = p[0] + (q[0] - p[0]) * c[0] - ci, az = p[1] + (q[1] - p[1]) * c[0] - cj;
			double bx = p[0] + (q[0] - p[0]) * c[1] - ci, bz = p[1] + (q[1] - p[1]) * c[1] - cj;
			if (Math.abs(ax - bx) < EPS && Math.abs(az - bz) < EPS) continue;
			double am = Math.PI * 2 * (k + 0.5) / segs;
			double[] normal = {Math.cos(am), 0, Math.sin(am)};
			double[][] pts = {{ax, 0, az}, {bx, 0, bz}, {bx, 1, bz}, {ax, 1, az}};
			float[][] uv = {{(float) ua, 1}, {(float) ub, 1}, {(float) ub, 0}, {(float) ua, 0}};
			free.add(quad(side, pts, uv, normal));
		}

		// Top and bottom: the circle cut to this block, as a fan of triangles.
		List<double[]> poly = new ArrayList<>();
		for (double[] p : circle) poly.add(new double[] {p[0] - ci, p[1] - cj});
		poly = cut(poly, 0, 1, false);
		poly = cut(poly, 0, 0, true);
		poly = cut(poly, 1, 1, false);
		poly = cut(poly, 1, 0, true);
		for (int k = 1; k + 1 < poly.size(); k++) {
			double[] a = poly.get(0), b = poly.get(k), c = poly.get(k + 1);
			for (boolean up : new boolean[] {true, false}) {
				double y = up ? 1 : 0;
				double[][] pts = {{a[0], y, a[1]}, {b[0], y, b[1]}, {c[0], y, c[1]}, {c[0], y, c[1]}};
				float[][] uv = new float[4][];
				for (int i = 0; i < 4; i++) uv[i] = up ? new float[] {(float) pts[i][0], (float) pts[i][2]} : new float[] {(float) pts[i][0], (float) (1 - pts[i][2])};
				BakedQuad qd = quad(up ? top : bottom, pts, uv, new double[] {0, up ? 1 : -1, 0});
				sided.computeIfAbsent(up ? Direction.UP : Direction.DOWN, d -> new ArrayList<>()).add(qd);
			}
		}
		return new Models.Built(new Models.Quads(sided, free), side);
	}

	/** The part of the segment p to q inside the block at (ci, cj), as fractions {from, to} along it, or null. */
	private static double[] clip(double[] p, double[] q, double ci, double cj) {
		double t0 = 0, t1 = 1, dx = q[0] - p[0], dz = q[1] - p[1];
		double[][] planes = {{-dx, p[0] - ci}, {dx, ci + 1 - p[0]}, {-dz, p[1] - cj}, {dz, cj + 1 - p[1]}};
		for (double[] pl : planes) {
			double den = pl[0], num = pl[1];
			if (Math.abs(den) < EPS) {
				if (num < -EPS) return null;
				continue;
			}
			double t = num / den;
			if (den < 0) t0 = Math.max(t0, t);
			else t1 = Math.min(t1, t);
			if (t0 > t1 - EPS) return null;
		}
		return new double[] {t0, t1};
	}

	/** Keeps the part of a convex polygon where coordinate axis is below (below true: at least) the value. */
	private static List<double[]> cut(List<double[]> in, int axis, double value, boolean atLeast) {
		List<double[]> out = new ArrayList<>();
		int n = in.size();
		for (int i = 0; i < n; i++) {
			double[] a = in.get(i), b = in.get((i + 1) % n);
			boolean ia = atLeast ? a[axis] >= value : a[axis] <= value, ib = atLeast ? b[axis] >= value : b[axis] <= value;
			if (ia) out.add(a);
			if (ia != ib) {
				double t = (value - a[axis]) / (b[axis] - a[axis]);
				out.add(new double[] {a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t});
			}
		}
		return out;
	}

	/** Winds the quad to show from the side its normal points to. */
	private static BakedQuad quad(TextureAtlasSprite sprite, double[][] p, float[][] uv, double[] n) {
		double[] a = p[0], b = p[1], c = p[2];
		double ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2];
		double vx = c[0] - a[0], vy = c[1] - a[1], vz = c[2] - a[2];
		double cx = uy * vz - uz * vy, cy = uz * vx - ux * vz, cz = ux * vy - uy * vx;
		if (cx * n[0] + cy * n[1] + cz * n[2] < 0) {
			p = new double[][] {p[0], p[3], p[2], p[1]};
			uv = new float[][] {uv[0], uv[3], uv[2], uv[1]};
		}
		Direction face = Math.abs(n[1]) > 0.5 ? (n[1] > 0 ? Direction.UP : Direction.DOWN)
				: Math.abs(n[0]) >= Math.abs(n[2]) ? (n[0] > 0 ? Direction.EAST : Direction.WEST) : (n[2] > 0 ? Direction.SOUTH : Direction.NORTH);
		return QuadMaker.of(sprite, -1, face, p, uv);
	}
}
