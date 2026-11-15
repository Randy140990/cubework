package com.insidethecube.cubework.block;

import com.insidethecube.cubework.Behaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
//#if MC >= 1.21.2
import net.minecraft.world.level.ScheduledTickAccess;
//#else
import net.minecraft.world.level.LevelAccessor;
//#endif
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

/** The shapes vanilla doesn't have: what they all share. */
public abstract class CwBlock extends Block implements CwShape {
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

	protected final Behaviour behaviour;

	protected CwBlock(Behaviour behaviour, BlockBehaviour.Properties settings) {
		super(settings);
		this.behaviour = behaviour;
	}

	@Override
	public Behaviour behaviour() {
		return behaviour;
	}

	/** Called by subclasses on the state they're about to place. */
	protected BlockState placed(BlockState state, BlockPlaceContext ctx) {
		return Shared.placed(behaviour, state, ctx);
	}

	protected static boolean waterAt(BlockPlaceContext ctx) {
		return ctx.getLevel().getFluidState(ctx.getClickedPos()).getType() == Fluids.WATER;
	}

	@Override
	//#if MC >= 1.21.2
	protected BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickView, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighborState, RandomSource random) {
	//#else
	protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
		LevelAccessor tickView = world;
		RandomSource random = world.getRandom();
	//#endif
		if (state.hasProperty(WATERLOGGED) && state.getValue(WATERLOGGED)) tickView.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
		BlockState next = shapeUpdate(state, world, pos, direction, neighborPos, neighborState);
		return Shared.updated(behaviour, next, world, pos);
	}

	/** A neighbour changed: the shape's own update (corners for slopes). */
	protected BlockState shapeUpdate(BlockState state, LevelReader world, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState) {
		return state;
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		return state.hasProperty(WATERLOGGED) && state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	@Override
	protected boolean useShapeForLightOcclusion(BlockState state) {
		return true;
	}


	/** Copper shapes weather like vanilla copper. */
	@Override
	protected void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
		Aging.tick(behaviour, state, world, pos, random);
	}
}
