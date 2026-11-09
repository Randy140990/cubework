package com.insidethecube.cubework.block;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.block.Block;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.StairShape;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

/**
 * The slope's surface, shared by its hit box (here) and its model (on the client). A straight slope
 * rises from its base on one edge to base + rise on the edge it faces (a full slope: 0 to 16 pixels; a
 * gentle slope's steps: 0-4, 4-8, 8-12, 12-16). Corners take the lower of two slopes (outer: a point, like
 * a roof's corner) or the higher (inner: where two hillsides meet).
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

	/** How far up its rise (0 to 1) the surface is at x, z, for a slope facing f (its high side) with this shape. */
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

	private static final Map<Integer, VoxelShape> OUTLINE = new ConcurrentHashMap<>();
	private static final Map<Integer, VoxelShape> INSIDE = new ConcurrentHashMap<>();

	private static int key(Direction f, BlockHalf half, StairShape shape, int base, int rise, int top) {
		return ((((f.getHorizontalQuarterTurns() * 2 + half.ordinal()) * 5 + shape.ordinal()) * 17 + base) * 17 + rise) * 17 + top;
	}

	/**
	 * The hit box and the shape you stand on: steps 2 pixels apart, each as high as the middle of its step.
	 * Heights are in pixels: the slope starts at base and rises by rise; top is the block's height (15 for
	 * dirt path), which upside-down slopes hang from.
	 */
	public static VoxelShape outline(Direction f, BlockHalf half, StairShape shape, int base, int rise, int top) {
		return OUTLINE.computeIfAbsent(key(f, half, shape, base, rise, top), k -> build(f, half, shape, false, base, rise, top));
	}

	/**
	 * The part that is certainly solid (steps no higher than the surface), for hiding neighbours' faces:
	 * nothing above the smooth surface may hide a face you can see.
	 */
	public static VoxelShape inside(Direction f, BlockHalf half, StairShape shape, int base, int rise, int top) {
		return INSIDE.computeIfAbsent(key(f, half, shape, base, rise, top), k -> build(f, half, shape, true, base, rise, top));
	}

	private static VoxelShape build(Direction f, BlockHalf half, StairShape shape, boolean inside, int base, int rise, int top) {
		VoxelShape out = VoxelShapes.empty();
		int cells = 8;
		for (int cz = 0; cz < cells; cz++) {
			int runStart = 0, runPx = -1;
			for (int cx = 0; cx <= cells; cx++) {
				int px = cx == cells ? -1 : base + (int) Math.round(fraction(f, shape, cx, cz, cells, inside) * rise);
				if (px != runPx) {
					if (runPx > 0) out = VoxelShapes.union(out, box(runStart, cx, cz, runPx, half, cells, top));
					runStart = cx;
					runPx = px;
				}
			}
		}
		return out.simplify();
	}

	private static double fraction(Direction f, StairShape shape, int cx, int cz, int cells, boolean inside) {
		double x0 = cx / (double) cells, x1 = (cx + 1) / (double) cells, z0 = cz / (double) cells, z1 = (cz + 1) / (double) cells;
		if (inside) {
			return Math.min(Math.min(height(f, shape, x0, z0), height(f, shape, x1, z0)), Math.min(height(f, shape, x0, z1), height(f, shape, x1, z1)));
		}
		return height(f, shape, (x0 + x1) / 2, (z0 + z1) / 2);
	}

	private static VoxelShape box(int x0, int x1, int cz, int px, BlockHalf half, int cells, int top) {
		double step = 16.0 / cells;
		double y0 = half == BlockHalf.BOTTOM ? 0 : top - px, y1 = half == BlockHalf.BOTTOM ? px : top;
		return Block.createCuboidShape(x0 * step, y0, cz * step, x1 * step, y1, (cz + 1) * step);
	}
}
