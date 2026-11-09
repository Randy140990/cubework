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
 * A slope a quarter as steep: it rises 4 pixels across the block, sitting on the step below, so four in a
 * row (steps 1 to 4) climb one block, for driveways and ramps. Placed in front of another facing the same
 * way, it takes the next step by itself; used with an empty hand, it moves to the next step.
 */
public class GentleSlopeBlock extends SlopeBlock {
	public static final IntProperty STEP = IntProperty.of("step", 1, 4);

	public GentleSlopeBlock(Material material, AbstractBlock.Settings settings) {
		super(material, settings);
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		super.appendProperties(builder);
		builder.add(STEP);
	}

	@Override
	public int base(BlockState state) {
		return (state.get(STEP) - 1) * 4;
	}

	@Override
	public int rise(BlockState state) {
		return 4;
	}

	/** Corners join gentle slopes on the same step. */
	@Override
	protected boolean joins(BlockState state, BlockState other) {
		return super.joins(state, other) && other.get(STEP).equals(state.get(STEP));
	}

	/** The step after the one below it (downhill), or before the one above it (uphill); else the first. */
	@Override
	protected BlockState placing(BlockState state, ItemPlacementContext ctx) {
		Direction facing = state.get(FACING);
		BlockPos pos = ctx.getBlockPos();
		BlockState below = ctx.getWorld().getBlockState(pos.offset(facing.getOpposite()));
		if (below.getBlock() instanceof GentleSlopeBlock && below.get(FACING) == facing && below.get(HALF) == state.get(HALF)) {
			return state.with(STEP, below.get(STEP) % 4 + 1);
		}
		BlockState above = ctx.getWorld().getBlockState(pos.offset(facing));
		if (above.getBlock() instanceof GentleSlopeBlock && above.get(FACING) == facing && above.get(HALF) == state.get(HALF)) {
			return state.with(STEP, above.get(STEP) == 1 ? 4 : above.get(STEP) - 1);
		}
		return state.with(STEP, 1);
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		// Only with an empty hand, so clicking one with a block still places the block.
		if (!player.getMainHandStack().isEmpty()) return ActionResult.PASS;
		BlockState next = state.with(STEP, state.get(STEP) % 4 + 1);
		next = next.with(SHAPE, shapeAt(next, world, pos));
		world.setBlockState(pos, next, Block.NOTIFY_ALL);
		world.playSound(player, pos, getSoundGroup(state).getPlaceSound(), SoundCategory.BLOCKS, 0.6f, 1.2f);
		return ActionResult.SUCCESS;
	}
}
