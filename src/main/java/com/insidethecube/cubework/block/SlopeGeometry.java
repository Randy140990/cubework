package com.insidethecube.cubework.block;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The slope's surface, shared by its hit box (here) and its model (on the client). A straight slope
 * rises from its base on one edge to base + rise on the edge it faces (a full slope: 0 to 16 pixels; a
 * 1 in 4 slope's steps: 0-4, 4-8, 8-12, 12-16). Corners take the lower of two slopes (outer: a point, like
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
	public static double height(Direction f, StairsShape shape, double x, double z) {
		double h = rise(f, x, z);
		return switch (shape) {
			case STRAIGHT -> h;
			case OUTER_LEFT -> Math.min(h, rise(f.getCounterClockWise(), x, z));
			case OUTER_RIGHT -> Math.min(h, rise(f.getClockWise(), x, z));
			case INNER_LEFT -> Math.max(h, rise(f.getCounterClockWise(), x, z));
			case INNER_RIGHT -> Math.max(h, rise(f.getClockWise(), x, z));
		};
	}

	private record Key(Direction f, Half half, StairsShape shape, double base, double rise, int top) {}

	private static final Map<Key, VoxelShape> OUTLINE = new ConcurrentHashMap<>();
	private static final Map<Key, VoxelShape> INSIDE = new ConcurrentHashMap<>();

	/**
	 * The hit box and the shape you stand on: steps 2 pixels apart, each as high as the middle of its step.
	 * Heights are in pixels: the slope starts at base and rises by rise; top is the block's height (15 for
	 * dirt path), which upside-down slopes hang from.
	 */
	public static VoxelShape outline(Direction f, Half half, StairsShape shape, double base, double rise, int top) {
		return OUTLINE.computeIfAbsent(new Key(f, half, shape, base, rise, top), k -> build(f, half, shape, false, base, rise, top));
	}

	/**
	 * The part that is certainly solid (steps no higher than the surface), for hiding neighbours' faces:
	 * nothing above the smooth surface may hide a face you can see.
	 */
	public static VoxelShape inside(Direction f, Half half, StairsShape shape, double base, double rise, int top) {
		return INSIDE.computeIfAbsent(new Key(f, half, shape, base, rise, top), k -> build(f, half, shape, true, base, rise, top));
	}

	private record AnyKey(Direction f, Half half, double base, double rise, int top) {}

	private static final Map<AnyKey, VoxelShape> INSIDE_ANY = new ConcurrentHashMap<>();

	/**
	 * What the slope fills whatever corner it turns (the inside of its straight and both outer corners
	 * together; inner corners fill more): its light-blocking shape, which can't know the neighbours.
	 */
	public static VoxelShape insideAny(Direction f, Half half, double base, double rise, int top) {
		return INSIDE_ANY.computeIfAbsent(new AnyKey(f, half, base, rise, top), k -> {
			VoxelShape s = inside(f, half, StairsShape.STRAIGHT, base, rise, top);
			s = Shapes.join(s, inside(f, half, StairsShape.OUTER_LEFT, base, rise, top), BooleanOp.AND);
			s = Shapes.join(s, inside(f, half, StairsShape.OUTER_RIGHT, base, rise, top), BooleanOp.AND);
			return s.optimize();
		});
	}

	private static VoxelShape build(Direction f, Half half, StairsShape shape, boolean inside, double base, double rise, int top) {
		VoxelShape out = Shapes.empty();
		int cells = 8;
		for (int cz = 0; cz < cells; cz++) {
			int runStart = 0;
			double runPx = -1;
			for (int cx = 0; cx <= cells; cx++) {
				// Heights to the nearest third of a pixel (slopes over three blocks rise 5 1/3 pixels).
				double px = cx == cells ? -1 : Math.round((base + fraction(f, shape, cx, cz, cells, inside) * rise) * 3) / 3.0;
				if (px != runPx) {
					if (runPx > 0) out = Shapes.or(out, box(runStart, cx, cz, runPx, half, cells, top));
					runStart = cx;
					runPx = px;
				}
			}
		}
		return out.optimize();
	}

	private static double fraction(Direction f, StairsShape shape, int cx, int cz, int cells, boolean inside) {
		double x0 = cx / (double) cells, x1 = (cx + 1) / (double) cells, z0 = cz / (double) cells, z1 = (cz + 1) / (double) cells;
		if (inside) {
			return Math.min(Math.min(height(f, shape, x0, z0), height(f, shape, x1, z0)), Math.min(height(f, shape, x0, z1), height(f, shape, x1, z1)));
		}
		return height(f, shape, (x0 + x1) / 2, (z0 + z1) / 2);
	}

	private static VoxelShape box(int x0, int x1, int cz, double px, Half half, int cells, int top) {
		double step = 16.0 / cells;
		double y0 = half == Half.BOTTOM ? 0 : top - px, y1 = half == Half.BOTTOM ? px : top;
		return Block.box(x0 * step, y0, cz * step, x1 * step, y1, (cz + 1) * step);
	}
}
