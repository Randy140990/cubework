package com.insidethecube.cubework.block;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Shapes that sit in one corner of the block (corner posts, round corners) store it as a facing: the corner
 * between that side and the next one clockwise. North is the north-east corner, east the south-east, south
 * the south-west and west the north-west.
 */
public final class Corners {
	private Corners() {}

	/** The corner's x and z (0 or 1). */
	public static int[] of(Direction f) {
		return switch (f) {
			case EAST -> new int[] {1, 1};
			case SOUTH -> new int[] {0, 1};
			case WEST -> new int[] {0, 0};
			default -> new int[] {1, 0};
		};
	}

	public static Direction facing(int x, int z) {
		if (x == 1) return z == 0 ? Direction.NORTH : Direction.EAST;
		return z == 1 ? Direction.SOUTH : Direction.WEST;
	}

	/** The corner the player is looking towards (the far one, as you face a tower's corner or a room's). */
	public static Direction looking(BlockPlaceContext ctx) {
		double yaw = Math.toRadians(ctx.getRotation());
		double dx = -Math.sin(yaw), dz = Math.cos(yaw);
		return facing(dx > 0 ? 1 : 0, dz > 0 ? 1 : 0);
	}

	/** The corner of the block nearest where the face was clicked. */
	public static Direction clicked(BlockPlaceContext ctx) {
		double x = ctx.getClickLocation().x - ctx.getClickedPos().getX(), z = ctx.getClickLocation().z - ctx.getClickedPos().getZ();
		Direction side = ctx.getClickedFace();
		// On a block's side, the clicked half is the one against that side.
		if (side == Direction.EAST) x = 0;
		if (side == Direction.WEST) x = 1;
		if (side == Direction.SOUTH) z = 0;
		if (side == Direction.NORTH) z = 1;
		return facing(x >= 0.5 ? 1 : 0, z >= 0.5 ? 1 : 0);
	}

	public static BlockState rotate(BlockState state, EnumProperty<Direction> prop, Rotation rotation) {
		return state.setValue(prop, rotation.rotate(state.getValue(prop)));
	}

	public static BlockState mirror(BlockState state, EnumProperty<Direction> prop, Mirror mirror) {
		int[] c = of(state.getValue(prop));
		if (mirror == Mirror.LEFT_RIGHT) c[1] = 1 - c[1];
		if (mirror == Mirror.FRONT_BACK) c[0] = 1 - c[0];
		return state.setValue(prop, facing(c[0], c[1]));
	}
}
