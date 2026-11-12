package com.insidethecube.cubework.block;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Round columns and round corners, shared by the model (client.RoundModel) and the hit box. Hit boxes are
 * the round outline in 2-pixel steps; the model is smooth.
 */
public final class RoundGeometry {
	private RoundGeometry() {}

	/** Segments round a full circle in the model. */
	public static final int SEGMENTS = 16;

	private static final Map<Integer, Map<Direction.Axis, VoxelShape>> COLUMNS = new HashMap<>();
	private static final Map<Boolean, Map<Direction, VoxelShape>> CORNERS = new HashMap<>();

	/** Whether the point (x, z), 0 to 1 across the block, is inside a round corner (solid corner given by its facing). */
	public static boolean inCorner(Direction f, boolean inner, double x, double z) {
		int[] c = Corners.of(f);
		if (!inner) return sq(x - c[0]) + sq(z - c[1]) <= 1;
		// Inner: outside the circle round the opposite corner.
		return sq(x - (1 - c[0])) + sq(z - (1 - c[1])) >= 1;
	}

	private static double sq(double v) {
		return v * v;
	}

	public static synchronized VoxelShape column(int width, Direction.Axis axis) {
		return COLUMNS.computeIfAbsent(width, w -> new EnumMap<>(Direction.Axis.class)).computeIfAbsent(axis, a -> {
			double r = width / 32.0;
			// Rows of 2 pixels across the circle.
			VoxelShape out = Shapes.empty();
			for (int i = 0; i < 8; i++) {
				double v0 = i / 8.0, v1 = (i + 1) / 8.0;
				double v = Math.abs((v0 + v1) / 2 - 0.5);
				if (v >= r) continue;
				double half = Math.sqrt(r * r - v * v);
				double u0 = Math.max(0, 0.5 - half), u1 = Math.min(1, 0.5 + half);
				out = Shapes.or(out, switch (a) {
					case Y -> Shapes.box(u0, 0, v0, u1, 1, v1);
					case X -> Shapes.box(0, u0, v0, 1, u1, v1);
					case Z -> Shapes.box(u0, v0, 0, u1, v1, 1);
				});
			}
			return out.optimize();
		});
	}

	public static synchronized VoxelShape corner(Direction f, boolean inner) {
		return CORNERS.computeIfAbsent(inner, k -> new EnumMap<>(Direction.class)).computeIfAbsent(f, d -> {
			VoxelShape out = Shapes.empty();
			for (int zi = 0; zi < 8; zi++) {
				int start = -1;
				for (int xi = 0; xi <= 8; xi++) {
					boolean solid = xi < 8 && inCorner(f, inner, (xi + 0.5) / 8, (zi + 0.5) / 8);
					if (solid && start < 0) start = xi;
					if (!solid && start >= 0) {
						out = Shapes.or(out, Shapes.box(start / 8.0, 0, zi / 8.0, xi / 8.0, 1, (zi + 1) / 8.0));
						start = -1;
					}
				}
			}
			return out.optimize();
		});
	}

	/** The two flat sides of a round corner (whole faces, which hide the neighbours' faces against them). */
	public static VoxelShape cornerSides(Direction f) {
		int[] c = Corners.of(f);
		VoxelShape xs = c[0] == 1 ? Block.box(15, 0, 0, 16, 16, 16) : Block.box(0, 0, 0, 1, 16, 16);
		VoxelShape zs = c[1] == 1 ? Block.box(0, 0, 15, 16, 16, 16) : Block.box(0, 0, 0, 16, 16, 1);
		return Shapes.or(xs, zs);
	}

	/** A corner post: 8 by 8 pixels, the full height, in its corner. */
	public static VoxelShape post(Direction f) {
		int[] c = Corners.of(f);
		double x0 = c[0] * 8, z0 = c[1] * 8;
		return Block.box(x0, 0, z0, x0 + 8, 16, z0 + 8);
	}
}
