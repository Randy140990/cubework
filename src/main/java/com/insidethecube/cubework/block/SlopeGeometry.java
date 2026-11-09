package com.insidethecube.cubework.block;

import net.minecraft.block.Block;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.StairShape;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

/**
 * The slope's surface, shared by its hit box (here) and its model (on the client). A straight slope
 * rises from nothing on one edge to full height on the edge it faces. Corners take the lower of two
 * slopes (outer: a point, like a roof's corner) or the higher (inner: where two hillsides meet).
 */
public final class SlopeGeometry {
	private SlopeGeometry() {}

	/** Height (0 to 1) of a slope rising towards d, at x, z (0 to 1) across the block. */
	public static double rise(Direction d, double x, double z) {
		return switch (d) {
			case NORTH -> 1 - z;
			case SOUTH -> z;
			case WEST -> 1 - x;
			default -> x;
		};
	}

	/** Height (0 to 1) of the surface at x, z for a slope facing f (its high side) with this shape. */
	public static double height(Direction f, StairShape shape, double x, double z) {
		double h = rise(f, x, z);
		return switch (shape) {
			case STRAIGHT -> h;
			case OUTER_LEFT -> Math.min(h, rise(f.rotateYCounterclockwise(), x, z));
			case OUTER_RIGHT -> Math.min(h, rise(f.rotateYClockwise(), x, z));
			case INNER_LEFT -> Math.max(h, rise(f.rotateYCounterclockwise(), x, z));
			case INNER_RIGHT -> Math.max(h, rise(f.rotateYClockwise(), x, z));
		};
	}

	private static final VoxelShape[] OUTLINE = new VoxelShape[2 * 4 * 2 * 5];
	private static final VoxelShape[] INSIDE = new VoxelShape[2 * 4 * 2 * 5];

	private static int index(Direction f, BlockHalf half, StairShape shape, int height) {
		return (height == 16 ? 0 : 40) + f.getHorizontalQuarterTurns() * 10 + half.ordinal() * 5 + shape.ordinal();
	}

	/** The hit box and the shape you stand on: steps of 2 pixels, each as high as the middle of its step. */
	public static VoxelShape outline(Direction f, BlockHalf half, StairShape shape, int height) {
		int i = index(f, half, shape, height);
		VoxelShape s = OUTLINE[i];
		if (s == null) OUTLINE[i] = s = build(f, half, shape, false, height);
		return s;
	}

	/**
	 * The part that is certainly solid (steps no higher than the surface), for hiding neighbours' faces:
	 * nothing above the smooth surface may hide a face you can see.
	 */
	public static VoxelShape inside(Direction f, BlockHalf half, StairShape shape, int height) {
		int i = index(f, half, shape, height);
		VoxelShape s = INSIDE[i];
		if (s == null) INSIDE[i] = s = build(f, half, shape, true, height);
		return s;
	}

	/** Heights scale to the block's own height (15 pixels for dirt path and farmland). */
	private static VoxelShape build(Direction f, BlockHalf half, StairShape shape, boolean inside, int height) {
		VoxelShape out = VoxelShapes.empty();
		int cells = 8;
		for (int cz = 0; cz < cells; cz++) {
			int runStart = 0, runPx = -1;
			for (int cx = 0; cx <= cells; cx++) {
				int px = cx == cells ? -1 : (int) Math.round(px(f, shape, cx, cz, cells, inside) * height / 16.0);
				if (px != runPx) {
					if (runPx > 0) out = VoxelShapes.union(out, box(runStart, cx, cz, runPx, half, cells, height));
					runStart = cx;
					runPx = px;
				}
			}
		}
		return out.simplify();
	}

	private static int px(Direction f, StairShape shape, int cx, int cz, int cells, boolean inside) {
		double x0 = cx / (double) cells, x1 = (cx + 1) / (double) cells, z0 = cz / (double) cells, z1 = (cz + 1) / (double) cells;
		double h;
		if (inside) {
			h = Math.min(Math.min(height(f, shape, x0, z0), height(f, shape, x1, z0)), Math.min(height(f, shape, x0, z1), height(f, shape, x1, z1)));
		} else {
			h = height(f, shape, (x0 + x1) / 2, (z0 + z1) / 2);
		}
		return (int) Math.round(h * 16);
	}

	private static VoxelShape box(int x0, int x1, int cz, int px, BlockHalf half, int cells, int height) {
		double step = 16.0 / cells;
		double y0 = half == BlockHalf.BOTTOM ? 0 : height - px, y1 = half == BlockHalf.BOTTOM ? px : height;
		return Block.createCuboidShape(x0 * step, y0, cz * step, x1 * step, y1, (cz + 1) * step);
	}
}
