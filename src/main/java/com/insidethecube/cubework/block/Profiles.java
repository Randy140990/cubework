package com.insidethecube.cubework.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The flat outlines (16 x 16 pixels, as seen from the front) of arches and window frames, turned into
 * hit boxes. tools/gen.py draws the same outlines for the models, so the two must match.
 */
public final class Profiles {
	private Profiles() {}

	/** Arch: solid except a round-headed opening from both bottom corners up to 4 pixels below the top. */
	public static boolean[][] arch() {
		boolean[][] m = new boolean[16][16];
		for (int x = 0; x < 16; x++) for (int y = 0; y < 16; y++) {
			double dx = (x + 0.5 - 8) / 8, dy = (y + 0.5) / 12;
			m[x][y] = dx * dx + dy * dy >= 1;
		}
		return m;
	}

	/** Arch corner: the curve rises from the left bottom corner to 4 pixels below the top on the right. */
	public static boolean[][] archCorner() {
		boolean[][] m = new boolean[16][16];
		for (int x = 0; x < 16; x++) for (int y = 0; y < 16; y++) {
			double dx = (x + 0.5 - 16) / 16, dy = (y + 0.5) / 12;
			m[x][y] = dx * dx + dy * dy >= 1;
		}
		return m;
	}

	/**
	 * A window frame: a 4-pixel border round an 8 x 8 opening. A side's border goes where a frame joins
	 * there; corners stay unless both sides beside them are joined. Diamond frames also fill the opening's
	 * corners where both of its sides are closed, so the opening comes to a point.
	 */
	public static boolean[][] window(boolean up, boolean down, boolean left, boolean right, boolean diamond) {
		boolean[][] m = new boolean[16][16];
		for (int x = 0; x < 16; x++) for (int y = 0; y < 16; y++) {
			boolean l = x < 4, r = x >= 12, b = y < 4, t = y >= 12;
			boolean solid;
			if ((l || r) && (b || t)) solid = !((l ? left : right) && (b ? down : up));
			else if (l) solid = !left;
			else if (r) solid = !right;
			else if (b) solid = !down;
			else if (t) solid = !up;
			else solid = diamond && corner(x, y, up, down, left, right);
			m[x][y] = solid;
		}
		return m;
	}

	/** Inside the opening: the triangles that make a diamond's points (only where both sides are closed). */
	private static boolean corner(int x, int y, boolean up, boolean down, boolean left, boolean right) {
		boolean leftHalf = x < 8, bottomHalf = y < 8;
		int i = leftHalf ? x - 4 : 11 - x, j = bottomHalf ? y - 4 : 11 - y;
		boolean closed = !(leftHalf ? left : right) && !(bottomHalf ? down : up);
		return closed && i + j <= 2;
	}

	/**
	 * The outline as a shape: x across (0 = the viewer's left when facing north), y up, extruded from z0
	 * to z1, then turned a number of quarter turns clockwise (seen from above), like a blockstate's y.
	 */
	public static VoxelShape shape(boolean[][] m, double z0, double z1, int quarterTurns) {
		VoxelShape out = Shapes.empty();
		for (int y = 0; y < 16; y++) {
			int start = -1;
			for (int x = 0; x <= 16; x++) {
				boolean solid = x < 16 && m[x][y];
				if (solid && start < 0) start = x;
				if (!solid && start >= 0) {
					out = Shapes.or(out, turned(start, y, z0, x, y + 1, z1, quarterTurns));
					start = -1;
				}
			}
		}
		return out.optimize();
	}

	private static VoxelShape turned(double x0, double y0, double z0, double x1, double y1, double z1, int turns) {
		for (int i = 0; i < (turns & 3); i++) {
			double nx0 = 16 - z1, nx1 = 16 - z0, nz0 = x0, nz1 = x1;
			x0 = nx0;
			x1 = nx1;
			z0 = nz0;
			z1 = nz1;
		}
		return Block.box(x0, y0, z0, x1, y1, z1);
	}
}
