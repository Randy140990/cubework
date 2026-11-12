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
import com.insidethecube.cubework.block.RoundColumnBlock;
import com.insidethecube.cubework.block.RoundCornerBlock;
import com.insidethecube.cubework.block.RoundGeometry;

/**
 * Round columns and round corners, drawn smooth from the vanilla block's own textures: the side texture
 * wraps round the curve (a quarter turn shows about one block's width of it, so bricks stay brick-sized)
 * and the ends use the top texture.
 */
public final class RoundModel {
	private RoundModel() {}

	static Models.Built build(Material material, BlockState state, Function<String, TextureAtlasSprite> sprite) {
		Material.Textures t = material.textures();
		TextureAtlasSprite top = sprite.apply(t.top()), side = sprite.apply(t.side()), bottom = sprite.apply(t.bottom());
		Out out = new Out();
		if (state.getBlock() instanceof RoundColumnBlock column) {
			column(out, column.width / 32.0, state.getValue(RoundColumnBlock.AXIS), top, side, bottom);
		} else {
			RoundCornerBlock corner = (RoundCornerBlock) state.getBlock();
			corner(out, state.getValue(RoundCornerBlock.FACING), corner.inner, top, side, bottom);
		}
		return new Models.Built(new Models.Quads(out.sided, out.free), side);
	}

	/** Quads by the side they're hidden from, and the ones never hidden. */
	private static final class Out {
		final Map<Direction, List<BakedQuad>> sided = new EnumMap<>(Direction.class);
		final List<BakedQuad> free = new ArrayList<>();

		void add(Direction cull, BakedQuad q) {
			if (cull == null) free.add(q);
			else sided.computeIfAbsent(cull, k -> new ArrayList<>()).add(q);
		}
	}

	// ---------- columns ----------

	private static void column(Out out, double r, Direction.Axis axis, TextureAtlasSprite top, TextureAtlasSprite side, TextureAtlasSprite bottom) {
		int n = RoundGeometry.SEGMENTS;
		// A quarter turn's length of texture, centred on the texture.
		double span = Math.min(1, Math.PI * r / 2), u0 = 0.5 - span / 2;
		for (int i = 0; i < n; i++) {
			double a0 = 2 * Math.PI * i / n, a1 = 2 * Math.PI * (i + 1) / n, am = (a0 + a1) / 2;
			double[] p0 = {0.5 + r * Math.cos(a0), 0.5 + r * Math.sin(a0)}, p1 = {0.5 + r * Math.cos(a1), 0.5 + r * Math.sin(a1)};
			int j = i % (n / 4);
			float ua = (float) (u0 + span * j / (n / 4.0)), ub = (float) (u0 + span * (j + 1) / (n / 4.0));
			// Local space: the column runs along y.
			double[][] p = {{p0[0], 0, p0[1]}, {p1[0], 0, p1[1]}, {p1[0], 1, p1[1]}, {p0[0], 1, p0[1]}};
			float[][] uv = {{ua, 1}, {ub, 1}, {ub, 0}, {ua, 0}};
			emit(out, side, p, uv, new double[] {Math.cos(am), 0, Math.sin(am)}, axis, null);
			// The ends, as fans of triangles from the middle.
			double[][] capTop = {{0.5, 1, 0.5}, {p0[0], 1, p0[1]}, {p1[0], 1, p1[1]}, {p1[0], 1, p1[1]}};
			double[][] capBottom = {{0.5, 0, 0.5}, {p0[0], 0, p0[1]}, {p1[0], 0, p1[1]}, {p1[0], 0, p1[1]}};
			emit(out, top, capTop, flatUv(capTop), new double[] {0, 1, 0}, axis, Direction.UP);
			emit(out, axis == Direction.Axis.Y ? bottom : top, capBottom, flatUv(capBottom), new double[] {0, -1, 0}, axis, Direction.DOWN);
		}
	}

	// ---------- corners ----------

	/**
	 * Built for the north-east corner (the facing north), then turned. Outer: a quarter circle round that
	 * corner, radius one block. Inner: the part of the block outside the circle round the opposite corner.
	 */
	private static void corner(Out out, Direction f, boolean inner, TextureAtlasSprite top, TextureAtlasSprite side, TextureAtlasSprite bottom) {
		int turns = switch (f) {
			case EAST -> 1;
			case SOUTH -> 2;
			case WEST -> 3;
			default -> 0;
		};
		int n = 8;
		double[] c = {1, 0};
		// Points along the curve from the north side to the east side.
		double[][] arc = new double[n + 1][];
		for (int i = 0; i <= n; i++) {
			double t = Math.PI / 2 * i / n;
			arc[i] = inner ? new double[] {Math.sin(t), 1 - Math.cos(t)} : new double[] {1 - Math.cos(t), Math.sin(t)};
		}
		for (int i = 0; i < n; i++) {
			double[] a = arc[i], b = arc[i + 1];
			// Two lengths of texture round the curve (about one block each).
			int half = n / 2, j = i % half;
			float ua = (float) j / half, ub = (float) (j + 1) / half;
			double mx = (a[0] + b[0]) / 2, mz = (a[1] + b[1]) / 2;
			// Outer faces out from the corner; inner faces the opposite corner.
			double[] normal = inner ? new double[] {0 - mx, 0, 1 - mz} : new double[] {mx - c[0], 0, mz - c[1]};
			double[][] p = {{a[0], 0, a[1]}, {b[0], 0, b[1]}, {b[0], 1, b[1]}, {a[0], 1, a[1]}};
			float[][] uv = {{ua, 1}, {ub, 1}, {ub, 0}, {ua, 0}};
			emitTurned(out, side, p, uv, normal, turns, null);
			double[][] capTop = {{c[0], 1, c[1]}, {a[0], 1, a[1]}, {b[0], 1, b[1]}, {b[0], 1, b[1]}};
			double[][] capBottom = {{c[0], 0, c[1]}, {a[0], 0, a[1]}, {b[0], 0, b[1]}, {b[0], 0, b[1]}};
			emitTurned(out, top, capTop, null, new double[] {0, 1, 0}, turns, Direction.UP);
			emitTurned(out, bottom, capBottom, null, new double[] {0, -1, 0}, turns, Direction.DOWN);
		}
		// The two flat sides against the corner: whole faces.
		double[][] north = {{0, 0, 0}, {1, 0, 0}, {1, 1, 0}, {0, 1, 0}};
		double[][] east = {{1, 0, 0}, {1, 0, 1}, {1, 1, 1}, {1, 1, 0}};
		emitTurned(out, side, north, null, new double[] {0, 0, -1}, turns, Direction.NORTH);
		emitTurned(out, side, east, null, new double[] {1, 0, 0}, turns, Direction.EAST);
	}

	/** Turns a corner piece clockwise (seen from above) a number of quarter turns; uv null means as on a full block. */
	private static void emitTurned(Out out, TextureAtlasSprite sprite, double[][] p, float[][] uv, double[] normal, int turns, Direction cull) {
		double[][] q = new double[4][];
		for (int i = 0; i < 4; i++) q[i] = turn(p[i], turns);
		double[] nv = turn(new double[] {normal[0] + 0.5, normal[1], normal[2] + 0.5}, turns);
		double[] nn = {nv[0] - 0.5, nv[1], nv[2] - 0.5};
		Direction c = cull == null ? null : turnDir(cull, turns);
		Direction face = nearest(nn[0], nn[1], nn[2]);
		if (uv == null) uv = c != null && c.getAxis().isHorizontal() ? sideUv(q, c) : flatUv(q);
		quad(out, sprite, q, uv, nn, face, c);
	}

	private static double[] turn(double[] p, int turns) {
		double x = p[0], z = p[2];
		for (int i = 0; i < turns; i++) {
			double nx = 1 - z, nz = x;
			x = nx;
			z = nz;
		}
		return new double[] {x, p[1], z};
	}

	private static Direction turnDir(Direction d, int turns) {
		if (!d.getAxis().isHorizontal()) return d;
		for (int i = 0; i < turns; i++) d = d.getClockWise();
		return d;
	}

	// ---------- shared ----------

	/** A column quad: local y becomes the column's axis. */
	private static void emit(Out out, TextureAtlasSprite sprite, double[][] p, float[][] uv, double[] normal, Direction.Axis axis, Direction cull) {
		double[][] q = new double[4][];
		for (int i = 0; i < 4; i++) q[i] = along(p[i], axis);
		double[] n = along(new double[] {normal[0] + 0.5, normal[1] + 0.5, normal[2] + 0.5}, axis);
		double[] nn = {n[0] - 0.5, n[1] - 0.5, n[2] - 0.5};
		Direction c = cull == null ? null : nearest(nn[0], nn[1], nn[2]);
		Direction face = nearest(nn[0], nn[1], nn[2]);
		quad(out, sprite, q, uv, nn, face, c);
	}

	private static double[] along(double[] p, Direction.Axis axis) {
		return switch (axis) {
			case X -> new double[] {p[1], p[0], p[2]};
			case Z -> new double[] {p[0], p[2], p[1]};
			default -> p;
		};
	}

	/** Texture as seen from above (or below), as on a full block's top. */
	private static float[][] flatUv(double[][] p) {
		float[][] uv = new float[4][];
		for (int i = 0; i < 4; i++) uv[i] = new float[] {(float) p[i][0], (float) p[i][2]};
		return uv;
	}

	/** Texture as on a full block's side. */
	private static float[][] sideUv(double[][] p, Direction d) {
		float[][] uv = new float[4][];
		for (int i = 0; i < 4; i++) {
			float u = switch (d) {
				case NORTH -> (float) (1 - p[i][0]);
				case SOUTH -> (float) p[i][0];
				case WEST -> (float) p[i][2];
				default -> (float) (1 - p[i][2]);
			};
			uv[i] = new float[] {u, (float) (1 - p[i][1])};
		}
		return uv;
	}

	/** Winds the quad so it shows from the side its normal points to. */
	private static void quad(Out out, TextureAtlasSprite sprite, double[][] p, float[][] uv, double[] n, Direction face, Direction cull) {
		double[] a = p[0], b = p[1], c = same(p[2], p[1]) ? p[3] : p[2];
		double ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2];
		double vx = c[0] - a[0], vy = c[1] - a[1], vz = c[2] - a[2];
		double nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
		if (nx * n[0] + ny * n[1] + nz * n[2] < 0) {
			p = new double[][] {p[0], p[3], p[2], p[1]};
			uv = new float[][] {uv[0], uv[3], uv[2], uv[1]};
		}
		out.add(cull, QuadMaker.of(sprite, -1, face, p, uv));
	}

	private static boolean same(double[] a, double[] b) {
		return a[0] == b[0] && a[1] == b[1] && a[2] == b[2];
	}

	/** The side a direction points most towards. */
	private static Direction nearest(double x, double y, double z) {
		double ax = Math.abs(x), ay = Math.abs(y), az = Math.abs(z);
		if (ay >= ax && ay >= az) return y >= 0 ? Direction.UP : Direction.DOWN;
		if (ax >= az) return x >= 0 ? Direction.EAST : Direction.WEST;
		return z >= 0 ? Direction.SOUTH : Direction.NORTH;
	}
}
