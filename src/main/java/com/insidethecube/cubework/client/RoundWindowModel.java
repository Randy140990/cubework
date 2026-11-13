package com.insidethecube.cubework.client;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Function;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import com.insidethecube.cubework.Material;
import com.insidethecube.cubework.block.RoundWindowGeometry;
import com.insidethecube.cubework.block.WindowFrameBlock;

/**
 * Round window frames drawn smooth: the front and back are the square with the round opening cut out (in
 * upright strips), the inside of the opening is a band of narrow faces following its curve (the sill facing
 * up uses the top texture, the head the bottom texture, the sides the side texture, as arches do), and the
 * outside edges are the block's sides, open where another frame joins. Built facing north, then turned.
 */
public final class RoundWindowModel {
	private RoundWindowModel() {}

	private static final double EPS = 1e-6;

	static Models.Built build(Material material, BlockState state, Function<String, TextureAtlasSprite> sprite) {
		Material.Textures t = material.textures();
		TextureAtlasSprite top = sprite.apply(t.top()), side = sprite.apply(t.side()), bottom = sprite.apply(t.bottom());
		int depth = ((WindowFrameBlock) state.getBlock()).depth();
		boolean u = state.getValue(WindowFrameBlock.UP), d = state.getValue(WindowFrameBlock.DOWN);
		boolean l = state.getValue(WindowFrameBlock.LEFT), r = state.getValue(WindowFrameBlock.RIGHT);
		int turns = state.getValue(WindowFrameBlock.FACING).get2DDataValue() + 2 & 3;
		return new Models.Built(quads(depth / 16.0, u, d, l, r, turns, top, side, bottom), side);
	}

	private static Models.Quads quads(double zf, boolean up, boolean down, boolean left, boolean right, int turns, TextureAtlasSprite top,
			TextureAtlasSprite side, TextureAtlasSprite bottom) {
		Map<Direction, List<BakedQuad>> out = new EnumMap<>(Direction.class);
		List<BakedQuad> free = new ArrayList<>();
		double[][] poly = RoundWindowGeometry.opening(up, down, left, right);
		int n = poly.length;
		for (double[] p : poly) {
			p[0] /= 16;
			p[1] /= 16;
		}
		double minX = 1, maxX = 0;
		for (double[] p : poly) {
			minX = Math.min(minX, p[0]);
			maxX = Math.max(maxX, p[0]);
		}

		// Front and back: upright strips between the corners' x positions, solid below and above the opening.
		TreeSet<Double> xs = new TreeSet<>();
		xs.add(0.0);
		xs.add(1.0);
		for (double[] p : poly) xs.add(p[0]);
		Double prev = null;
		for (double x : xs) {
			if (prev != null && x - prev > EPS) {
				double xa = prev, xb = x, xm = (xa + xb) / 2;
				if (xm < minX || xm > maxX) {
					face(out, free, zf, new double[][] {{xa, 0}, {xb, 0}, {xb, 1}, {xa, 1}}, side, turns);
				} else {
					double[] lo = null, hi = null;
					double loY = Double.MAX_VALUE, hiY = -Double.MAX_VALUE;
					for (int i = 0; i < n; i++) {
						double[] p = poly[i], q = poly[(i + 1) % n];
						if (Math.abs(p[0] - q[0]) < EPS || xm < Math.min(p[0], q[0]) || xm > Math.max(p[0], q[0])) continue;
						double y = at(p, q, xm);
						if (y < loY) {
							loY = y;
							lo = new double[] {at(p, q, xa), at(p, q, xb)};
						}
						if (y > hiY) {
							hiY = y;
							hi = new double[] {at(p, q, xa), at(p, q, xb)};
						}
					}
					if (lo != null && Math.max(lo[0], lo[1]) > EPS) {
						face(out, free, zf, new double[][] {{xa, 0}, {xb, 0}, {xb, lo[1]}, {xa, lo[0]}}, side, turns);
					}
					if (hi != null && Math.min(hi[0], hi[1]) < 1 - EPS) {
						face(out, free, zf, new double[][] {{xa, hi[0]}, {xb, hi[1]}, {xb, 1}, {xa, 1}}, side, turns);
					}
				}
			}
			prev = x;
		}

		// Inside the opening: a band following the outline, facing into it (the outline goes anticlockwise, so
		// the opening is on the left of each edge). Edges along the block's own sides are open to the next frame.
		for (int i = 0; i < n; i++) {
			double[] p = poly[i], q = poly[(i + 1) % n];
			if (onBorder(p, q)) continue;
			double dx = q[0] - p[0], dy = q[1] - p[1], len = Math.sqrt(dx * dx + dy * dy);
			if (len < EPS) continue;
			double nx = -dy / len, ny = dx / len;
			Direction face = Math.abs(ny) >= Math.abs(nx) ? (ny > 0 ? Direction.UP : Direction.DOWN) : nx > 0 ? Direction.EAST : Direction.WEST;
			double[][] pts = {{p[0], p[1], 0}, {q[0], q[1], 0}, {q[0], q[1], zf}, {p[0], p[1], zf}};
			TextureAtlasSprite s = face == Direction.UP ? top : face == Direction.DOWN ? bottom : side;
			float[][] uv = face.getAxis() == Direction.Axis.Y ? flatUv(pts, face) : sideUv(pts, face);
			free.add(quad(s, pts, uv, face, new double[] {nx, ny, 0}, turns));
		}

		// The outside edges: whole, or either side of the opening where it runs out to a joined frame.
		edge(out, Direction.WEST, left, down ? 0 : 0.25, up ? 1 : 0.75, zf, side, top, bottom, turns);
		edge(out, Direction.EAST, right, down ? 0 : 0.25, up ? 1 : 0.75, zf, side, top, bottom, turns);
		edge(out, Direction.DOWN, down, left ? 0 : 0.25, right ? 1 : 0.75, zf, side, top, bottom, turns);
		edge(out, Direction.UP, up, left ? 0 : 0.25, right ? 1 : 0.75, zf, side, top, bottom, turns);
		return new Models.Quads(out, free);
	}

	/** y on the line through p and q at x. */
	private static double at(double[] p, double[] q, double x) {
		return p[1] + (q[1] - p[1]) * (x - p[0]) / (q[0] - p[0]);
	}

	/** Whether an edge of the outline lies along the block's side (where the opening runs on into the next frame). */
	private static boolean onBorder(double[] p, double[] q) {
		for (int axis = 0; axis < 2; axis++) {
			for (double v : new double[] {0, 1}) {
				if (Math.abs(p[axis] - v) < EPS && Math.abs(q[axis] - v) < EPS) return true;
			}
		}
		return false;
	}

	/** One strip of the front (at zf, facing south) and the back (against the wall, facing north). */
	private static void face(Map<Direction, List<BakedQuad>> out, List<BakedQuad> free, double zf, double[][] xy, TextureAtlasSprite side, int turns) {
		double[][] front = new double[4][], back = new double[4][];
		for (int i = 0; i < 4; i++) {
			front[i] = new double[] {xy[i][0], xy[i][1], zf};
			back[i] = new double[] {xy[i][0], xy[i][1], 0};
		}
		BakedQuad f = quad(side, front, sideUv(front, Direction.SOUTH), Direction.SOUTH, axis(Direction.SOUTH), turns);
		if (zf >= 1 - EPS) add(out, turn(Direction.SOUTH, turns), f);
		else free.add(f);
		add(out, turn(Direction.NORTH, turns), quad(side, back, sideUv(back, Direction.NORTH), Direction.NORTH, axis(Direction.NORTH), turns));
	}

	/** An outside edge: the whole side when closed, else the parts beside the opening (from a to b along it). */
	private static void edge(Map<Direction, List<BakedQuad>> out, Direction d, boolean open, double a, double b, double zf, TextureAtlasSprite side,
			TextureAtlasSprite top, TextureAtlasSprite bottom, int turns) {
		double[][] spans = open ? new double[][] {{0, a}, {b, 1}} : new double[][] {{0, 1}};
		for (double[] s : spans) {
			if (s[1] - s[0] < EPS) continue;
			double[][] p;
			switch (d) {
				case WEST -> p = new double[][] {{0, s[0], 0}, {0, s[1], 0}, {0, s[1], zf}, {0, s[0], zf}};
				case EAST -> p = new double[][] {{1, s[0], 0}, {1, s[1], 0}, {1, s[1], zf}, {1, s[0], zf}};
				case DOWN -> p = new double[][] {{s[0], 0, 0}, {s[1], 0, 0}, {s[1], 0, zf}, {s[0], 0, zf}};
				default -> p = new double[][] {{s[0], 1, 0}, {s[1], 1, 0}, {s[1], 1, zf}, {s[0], 1, zf}};
			}
			TextureAtlasSprite sp = d == Direction.UP ? top : d == Direction.DOWN ? bottom : side;
			float[][] uv = d.getAxis() == Direction.Axis.Y ? flatUv(p, d) : sideUv(p, d);
			add(out, turn(d, turns), quad(sp, p, uv, d, axis(d), turns));
		}
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
		// Wind by the corners that differ (a strip can come to a point, repeating a corner).
		double[] a = p[0], b = p[1], c = p[2];
		if (same(a, b)) {
			b = p[2];
			c = p[3];
		} else if (same(b, c)) {
			c = p[3];
		}
		double ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2];
		double vx = c[0] - a[0], vy = c[1] - a[1], vz = c[2] - a[2];
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
