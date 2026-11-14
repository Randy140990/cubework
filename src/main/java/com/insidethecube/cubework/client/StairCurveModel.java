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
import com.insidethecube.cubework.block.ArchCornerBlock;
import com.insidethecube.cubework.block.StairCurveBlock;
import com.insidethecube.cubework.block.StairCurveGeometry;

/**
 * Curved Stairs drawn with a smooth S-shaped underside: the flight's sides are strips from the curve up
 * to the top (a stair's two steps, or the block's top for the fill under it), the underside is a band of
 * narrow faces following the curve (the bottom texture where it's flatter, the side texture where it's
 * steeper), and the steps, riser and ends are flat. Built climbing east, then turned.
 */
public final class StairCurveModel {
	private StairCurveModel() {}

	private static final double EPS = 1e-6;

	static Models.Built build(Material material, BlockState state, Function<String, TextureAtlasSprite> sprite) {
		Material.Textures t = material.textures();
		TextureAtlasSprite top = sprite.apply(t.top()), side = sprite.apply(t.side()), bottom = sprite.apply(t.bottom());
		int piece = state.getValue(StairCurveBlock.PIECE);
		int turns = ArchCornerBlock.turns(state.getValue(StairCurveBlock.FACING));
		return new Models.Built(quads(piece, turns, top, side, bottom), side);
	}

	private static Models.Quads quads(int piece, int turns, TextureAtlasSprite top, TextureAtlasSprite side, TextureAtlasSprite bottom) {
		Map<Direction, List<BakedQuad>> out = new EnumMap<>(Direction.class);
		List<BakedQuad> free = new ArrayList<>();
		double[][] c = StairCurveGeometry.points(piece);
		boolean stair = StairCurveGeometry.stairCell(piece);
		// Runs of the top that are solid underneath: {from x, to x, height}.
		List<double[]> tops = new ArrayList<>();
		for (int i = 0; i + 1 < c.length; i++) {
			double x0 = c[i][0], x1 = c[i + 1][0];
			double tm = StairCurveGeometry.top(piece, (x0 + x1) / 2);
			double y0 = Math.min(c[i][1], tm), y1 = Math.min(c[i + 1][1], tm);
			if (y0 >= tm - EPS && y1 >= tm - EPS) continue;          // the curve has left through the top here
			if (!tops.isEmpty() && Math.abs(tops.get(tops.size() - 1)[1] - x0) < EPS && tops.get(tops.size() - 1)[2] == tm) tops.get(tops.size() - 1)[1] = x1;
			else tops.add(new double[] {x0, x1, tm});
			// The flight's two sides: the strip between the curve and the top.
			for (Direction d : new Direction[] {Direction.NORTH, Direction.SOUTH}) {
				double z = d == Direction.NORTH ? 0 : 1;
				double[][] p = {{x0, y0, z}, {x1, y1, z}, {x1, tm, z}, {x0, tm, z}};
				add(out, turn(d, turns), quad(side, p, sideUv(p, d), d, axis(d), turns));
			}
			// The underside: on the floor it's the block's bottom face; elsewhere it faces down, back or forward.
			double[][] p = {{x0, y0, 0}, {x1, y1, 0}, {x1, y1, 1}, {x0, y0, 1}};
			if (y0 < EPS && y1 < EPS) {
				add(out, turn(Direction.DOWN, turns), quad(bottom, p, flatUv(p, Direction.DOWN), Direction.DOWN, axis(Direction.DOWN), turns));
				continue;
			}
			double dx = x1 - x0, dy = y1 - y0, len = Math.sqrt(dx * dx + dy * dy);
			if (len < EPS) continue;
			double nx = dy / len, ny = -dx / len;
			Direction face = Math.abs(ny) >= Math.abs(nx) ? Direction.DOWN : nx > 0 ? Direction.EAST : Direction.WEST;
			float[][] uv = face == Direction.DOWN ? flatUv(p, face) : sideUv(p, face);
			free.add(quad(face == Direction.DOWN ? bottom : side, p, uv, face, new double[] {nx, ny, 0}, turns));
		}
		// The top: a stair's lower step (inside the block) and upper step (the block's top), or the block's top.
		for (double[] t : tops) {
			double[][] q = {{t[0], t[2], 0}, {t[1], t[2], 0}, {t[1], t[2], 1}, {t[0], t[2], 1}};
			BakedQuad tq = quad(top, q, flatUv(q, Direction.UP), Direction.UP, axis(Direction.UP), turns);
			if (t[2] >= 1 - EPS) add(out, turn(Direction.UP, turns), tq);
			else free.add(tq);
		}
		if (stair) {
			// The riser between the steps, facing down the flight.
			double[][] q = {{0.5, 0.5, 0}, {0.5, 0.5, 1}, {0.5, 1, 1}, {0.5, 1, 0}};
			free.add(quad(side, q, sideUv(q, Direction.WEST), Direction.WEST, axis(Direction.WEST), turns));
		}
		// The ends: from the curve up to the top.
		for (Direction d : new Direction[] {Direction.WEST, Direction.EAST}) {
			double x = d == Direction.WEST ? 0 : 1, y = d == Direction.WEST ? c[0][1] : c[c.length - 1][1];
			double tm = StairCurveGeometry.top(piece, d == Direction.WEST ? 0.0 : 1.0);
			if (y >= tm - EPS) continue;
			double[][] p = {{x, y, 0}, {x, y, 1}, {x, tm, 1}, {x, tm, 0}};
			add(out, turn(d, turns), quad(side, p, sideUv(p, d), d, axis(d), turns));
		}
		return new Models.Quads(out, free);
	}

	private static void add(Map<Direction, List<BakedQuad>> out, Direction d, BakedQuad q) {
		out.computeIfAbsent(d, k -> new ArrayList<>()).add(q);
	}

	private static double[] axis(Direction d) {
		return new double[] {d.getStepX(), d.getStepY(), d.getStepZ()};
	}

	private static Direction turn(Direction d, int turns) {
		for (int i = 0; i < turns; i++) if (d.getAxis().isHorizontal()) d = d.getClockWise();
		return d;
	}

	/** A quad wound to show from the side its normal points to, then turned with the block. */
	private static BakedQuad quad(TextureAtlasSprite sprite, double[][] p, float[][] uv, Direction face, double[] n, int turns) {
		double[] a = p[0], b = p[1], cc = p[2];
		if (same(a, b)) {
			b = p[2];
			cc = p[3];
		} else if (same(b, cc)) {
			cc = p[3];
		}
		double ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2];
		double vx = cc[0] - a[0], vy = cc[1] - a[1], vz = cc[2] - a[2];
		double cx = uy * vz - uz * vy, cy = uz * vx - ux * vz, cz = ux * vy - uy * vx;
		if (cx * n[0] + cy * n[1] + cz * n[2] < 0) {
			p = new double[][] {p[0], p[3], p[2], p[1]};
			uv = new float[][] {uv[0], uv[3], uv[2], uv[1]};
		}
		double[][] turned = new double[4][];
		for (int i = 0; i < 4; i++) {
			double x = p[i][0], z = p[i][2];
			for (int t = 0; t < turns; t++) {
				double nx = 1 - z, nz = x;
				x = nx;
				z = nz;
			}
			turned[i] = new double[] {x, p[i][1], z};
		}
		return QuadMaker.of(sprite, -1, turn(face, turns), turned, uv);
	}

	private static boolean same(double[] a, double[] b) {
		return Math.abs(a[0] - b[0]) < EPS && Math.abs(a[1] - b[1]) < EPS && Math.abs(a[2] - b[2]) < EPS;
	}

	/** Sides: across as on a full block's side, top of the texture at the top of the block. */
	private static float[][] sideUv(double[][] p, Direction face) {
		float[][] uv = new float[4][];
		for (int i = 0; i < 4; i++) {
			double u = switch (face) {
				case NORTH -> 1 - p[i][0];
				case SOUTH -> p[i][0];
				case WEST -> p[i][2];
				default -> 1 - p[i][2];
			};
			uv[i] = new float[] {(float) u, (float) (1 - p[i][1])};
		}
		return uv;
	}

	/** Top and underneath: as seen from above or below. */
	private static float[][] flatUv(double[][] p, Direction face) {
		float[][] uv = new float[4][];
		for (int i = 0; i < 4; i++) {
			uv[i] = face == Direction.UP ? new float[] {(float) p[i][0], (float) p[i][2]} : new float[] {(float) p[i][0], (float) (1 - p[i][2])};
		}
		return uv;
	}
}
