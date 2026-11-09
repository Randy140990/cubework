package com.insidethecube.cubework.block;

import java.util.function.Supplier;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.Waterloggable;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;

/**
 * Coral or a coral fan at another size, behaving like vanilla's: it stands on the top of a solid block,
 * fills with water when placed in water, and living coral out of water dies after a few seconds, turning
 * into the dead coral of the same size.
 */
public class SizedCoralBlock extends Block implements Waterloggable {
	public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;

	private final VoxelShape shape;
	/** The dead coral it turns into, or null when it's dead already. */
	private final Supplier<Block> dead;

	public SizedCoralBlock(double scale, boolean fan, Supplier<Block> dead, AbstractBlock.Settings settings) {
		super(settings);
		this.dead = dead;
		double half = Math.min(8, 6 * scale), h = Math.min(16, (fan ? 4 : 15) * scale);
		this.shape = Block.createCuboidShape(8 - half, 0, 8 - half, 8 + half, Math.max(1, h), 8 + half);
		setDefaultState(getDefaultState().with(WATERLOGGED, true));
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(WATERLOGGED);
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return shape;
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		FluidState fluid = ctx.getWorld().getFluidState(ctx.getBlockPos());
		return getDefaultState().with(WATERLOGGED, fluid.isIn(FluidTags.WATER) && fluid.getLevel() == 8);
	}

	@Override
	protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
		BlockPos below = pos.down();
		return world.getBlockState(below).isSideSolidFullSquare(world, below, Direction.UP);
	}

	@Override
	protected void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
		if (dead != null && !inWater(state, world, pos)) world.scheduleBlockTick(pos, this, 60 + world.getRandom().nextInt(40));
	}

	@Override
	protected BlockState getStateForNeighborUpdate(BlockState state, WorldView world, ScheduledTickView tickView, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighborState, Random random) {
		if (direction == Direction.DOWN && !canPlaceAt(state, world, pos)) return Blocks.AIR.getDefaultState();
		if (state.get(WATERLOGGED)) tickView.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
		if (dead != null && !inWater(state, world, pos)) tickView.scheduleBlockTick(pos, this, 60 + random.nextInt(40));
		return state;
	}

	@Override
	protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		if (dead != null && !inWater(state, world, pos)) world.setBlockState(pos, dead.get().getDefaultState().with(WATERLOGGED, false), Block.NOTIFY_LISTENERS);
	}

	/** Living coral needs water in its own block or any block touching it. */
	private static boolean inWater(BlockState state, BlockView world, BlockPos pos) {
		if (state.get(WATERLOGGED)) return true;
		for (Direction d : Direction.values()) if (world.getFluidState(pos.offset(d)).isIn(FluidTags.WATER)) return true;
		return false;
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
	}
}
