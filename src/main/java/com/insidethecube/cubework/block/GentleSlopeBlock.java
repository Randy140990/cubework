package com.insidethecube.cubework.block;

import com.insidethecube.cubework.Material;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * A slope that climbs one block over four (1 in 4): it rises 4 pixels across the block, sitting on the
 * step below, so four in a row (steps 1 to 4) climb one block, for driveways, ramps and roofs. Over3 and
 * Over2 climb one block over three (5 1/3 pixels each) and two (8 pixels each). Placed in front of another
 * of the same kind facing the same way, it takes the next step by itself; used with an empty hand, it moves
 * to the next step.
 */
public class GentleSlopeBlock extends SlopeBlock {
	public static final IntProperty STEP = IntProperty.of("step", 1, 4);

	public GentleSlopeBlock(Material material, AbstractBlock.Settings settings) {
		super(material, settings);
	}

	/** How many blocks it takes to climb one. */
	public int steps() {
		return 4;
	}

	/** Its step property (1 to steps()). Called while the block is being built, so it returns a constant. */
	public IntProperty step() {
		return STEP;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		super.appendProperties(builder);
		builder.add(step());
	}

	@Override
	public double base(BlockState state) {
		return (state.get(step()) - 1) * 16.0 / steps();
	}

	@Override
	public double rise(BlockState state) {
		return 16.0 / steps();
	}

	/** One block over three. */
	public static class Over3 extends GentleSlopeBlock {
		public static final IntProperty STEP_3 = IntProperty.of("step", 1, 3);

		public Over3(Material material, AbstractBlock.Settings settings) {
			super(material, settings);
		}

		@Override
		public int steps() {
			return 3;
		}

		@Override
		public IntProperty step() {
			return STEP_3;
		}
	}

	/** One block over two. */
	public static class Over2 extends GentleSlopeBlock {
		public static final IntProperty STEP_2 = IntProperty.of("step", 1, 2);

		public Over2(Material material, AbstractBlock.Settings settings) {
			super(material, settings);
		}

		@Override
		public int steps() {
			return 2;
		}

		@Override
		public IntProperty step() {
			return STEP_2;
		}
	}

	/** Corners join slopes of the same kind on the same step. */
	@Override
	protected boolean joins(BlockState state, BlockState other) {
		return super.joins(state, other) && other.get(step()).equals(state.get(step()));
	}

	/** The step after the one below it (downhill), or before the one above it (uphill); else the first. */
	@Override
	protected BlockState placing(BlockState state, ItemPlacementContext ctx) {
		Direction facing = state.get(FACING);
		BlockPos pos = ctx.getBlockPos();
		BlockState below = ctx.getWorld().getBlockState(pos.offset(facing.getOpposite()));
		IntProperty step = step();
		int n = steps();
		if (sameKind(below) && below.get(FACING) == facing && below.get(HALF) == state.get(HALF)) {
			return state.with(step, below.get(step) % n + 1);
		}
		BlockState above = ctx.getWorld().getBlockState(pos.offset(facing));
		if (sameKind(above) && above.get(FACING) == facing && above.get(HALF) == state.get(HALF)) {
			return state.with(step, above.get(step) == 1 ? n : above.get(step) - 1);
		}
		return state.with(step, 1);
	}

	/** A slope with the same number of steps (any material). */
	private boolean sameKind(BlockState other) {
		return other.getBlock() instanceof GentleSlopeBlock g && g.steps() == steps();
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		// Only with an empty hand, so clicking one with a block still places the block.
		if (!player.getMainHandStack().isEmpty()) return ActionResult.PASS;
		BlockState next = state.with(step(), state.get(step()) % steps() + 1);
		next = next.with(SHAPE, shapeAt(next, world, pos));
		world.setBlockState(pos, next, Block.NOTIFY_ALL);
		world.playSound(player, pos, getSoundGroup(state).getPlaceSound(), SoundCategory.BLOCKS, 0.6f, 1.2f);
		return ActionResult.SUCCESS;
	}
}
