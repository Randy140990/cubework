package com.insidethecube.cubework.block;

import com.insidethecube.cubework.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A slope that climbs one block over four (1 in 4): it rises 4 pixels across the block, sitting on the
 * step below, so four in a row (steps 1 to 4) climb one block, for driveways, ramps and roofs. Over3 and
 * Over2 climb one block over three (5 1/3 pixels each) and two (8 pixels each). Placed in front of another
 * of the same kind facing the same way, it takes the next step by itself; used with an empty hand, it moves
 * to the next step.
 */
public class GentleSlopeBlock extends SlopeBlock {
	public static final IntegerProperty STEP = IntegerProperty.create("step", 1, 4);

	public GentleSlopeBlock(Material material, BlockBehaviour.Properties settings) {
		super(material, settings);
	}

	/** How many blocks it takes to climb one. */
	public int steps() {
		return 4;
	}

	/** Its step property (1 to steps()). Called while the block is being built, so it returns a constant. */
	public IntegerProperty step() {
		return STEP;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(step());
	}

	@Override
	public double base(BlockState state) {
		return (state.getValue(step()) - 1) * 16.0 / steps();
	}

	@Override
	public double rise(BlockState state) {
		return 16.0 / steps();
	}

	/** One block over three. */
	public static class Over3 extends GentleSlopeBlock {
		public static final IntegerProperty STEP_3 = IntegerProperty.create("step", 1, 3);

		public Over3(Material material, BlockBehaviour.Properties settings) {
			super(material, settings);
		}

		@Override
		public int steps() {
			return 3;
		}

		@Override
		public IntegerProperty step() {
			return STEP_3;
		}
	}

	/** One block over two. */
	public static class Over2 extends GentleSlopeBlock {
		public static final IntegerProperty STEP_2 = IntegerProperty.create("step", 1, 2);

		public Over2(Material material, BlockBehaviour.Properties settings) {
			super(material, settings);
		}

		@Override
		public int steps() {
			return 2;
		}

		@Override
		public IntegerProperty step() {
			return STEP_2;
		}
	}

	/** Corners join slopes of the same kind on the same step. */
	@Override
	protected boolean joins(BlockState state, BlockState other) {
		return super.joins(state, other) && other.getValue(step()).equals(state.getValue(step()));
	}

	/** The step after the one below it (downhill), or before the one above it (uphill); else the first. */
	@Override
	protected BlockState placing(BlockState state, BlockPlaceContext ctx) {
		Direction facing = state.getValue(FACING);
		BlockPos pos = ctx.getClickedPos();
		BlockState below = ctx.getLevel().getBlockState(pos.relative(facing.getOpposite()));
		IntegerProperty step = step();
		int n = steps();
		if (sameKind(below) && below.getValue(FACING) == facing && below.getValue(HALF) == state.getValue(HALF)) {
			return state.setValue(step, below.getValue(step) % n + 1);
		}
		BlockState above = ctx.getLevel().getBlockState(pos.relative(facing));
		if (sameKind(above) && above.getValue(FACING) == facing && above.getValue(HALF) == state.getValue(HALF)) {
			return state.setValue(step, above.getValue(step) == 1 ? n : above.getValue(step) - 1);
		}
		return state.setValue(step, 1);
	}

	/** A slope with the same number of steps (any material). */
	private boolean sameKind(BlockState other) {
		return other.getBlock() instanceof GentleSlopeBlock g && g.steps() == steps();
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
		// Only with an empty hand, so clicking one with a block still places the block.
		if (!player.getMainHandItem().isEmpty()) return InteractionResult.PASS;
		BlockState next = state.setValue(step(), state.getValue(step()) % steps() + 1);
		next = next.setValue(SHAPE, shapeAt(next, world, pos));
		world.setBlock(pos, next, Block.UPDATE_ALL);
		world.playSound(player, pos, getSoundType(state).getPlaceSound(), SoundSource.BLOCKS, 0.6f, 1.2f);
		return InteractionResult.SUCCESS;
	}
}
