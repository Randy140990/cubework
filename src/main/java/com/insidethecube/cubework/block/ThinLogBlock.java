package com.insidethecube.cubework.block;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;

/**
 * A log or stem, full length but narrower (16, 12, 8 or 4 pixels), in the middle of the block, with the
 * bark and the ring shrunk to fit. An end with another log against it shows bark instead of the ring, so
 * thinner logs stacked on thicker ones look like one trunk tapering (and branches grow out of bark).
 */
public class ThinLogBlock extends ThinPillarBlock {
	/** The model's top and bottom ends, which turn with the axis: up and down, north and south, east and west. */
	public static final BooleanProperty BARK_TOP = BooleanProperty.of("bark_top");
	public static final BooleanProperty BARK_BOTTOM = BooleanProperty.of("bark_bottom");

	public ThinLogBlock(int width, AbstractBlock.Settings settings) {
		super(null, width, settings);
		setDefaultState(getDefaultState().with(BARK_TOP, false).with(BARK_BOTTOM, false));
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		super.appendProperties(builder);
		builder.add(BARK_TOP, BARK_BOTTOM);
	}

	/** Where the model's top end faces, for each axis (as the blockstate turns it). */
	private static Direction top(Direction.Axis axis) {
		return switch (axis) {
			case Y -> Direction.UP;
			case Z -> Direction.NORTH;
			case X -> Direction.EAST;
		};
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		BlockState state = super.getPlacementState(ctx);
		return state == null ? null : ends(state, ctx.getWorld(), ctx.getBlockPos());
	}

	@Override
	protected BlockState getStateForNeighborUpdate(BlockState state, WorldView world, ScheduledTickView tickView, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighborState, Random random) {
		BlockState next = super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random);
		return next.isOf(this) && direction.getAxis() == next.get(AXIS) ? ends(next, world, pos) : next;
	}

	private static BlockState ends(BlockState state, BlockView world, BlockPos pos) {
		Direction t = top(state.get(AXIS));
		return state.with(BARK_TOP, log(world.getBlockState(pos.offset(t)))).with(BARK_BOTTOM, log(world.getBlockState(pos.offset(t.getOpposite()))));
	}

	private static boolean log(BlockState s) {
		return s.getBlock() instanceof ThinLogBlock || s.isIn(BlockTags.LOGS) || s.isOf(Blocks.MUSHROOM_STEM);
	}
}
