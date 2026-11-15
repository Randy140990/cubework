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
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** The vanilla FenceBlock in a Cubework material: placed, connected and used exactly like vanilla's. */
public class CwFence extends FenceBlock implements CwShape {
	private final Behaviour behaviour;

	public CwFence(Behaviour behaviour, BlockBehaviour.Properties settings) {
		super(settings);
		this.behaviour = behaviour;
	}

	@Override
	public Behaviour behaviour() {
		return behaviour;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		return Shared.placed(behaviour, super.getStateForPlacement(ctx), ctx);
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
		//#if MC >= 1.21.2
		return Shared.updated(behaviour, super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random), world, pos);
		//#else
		return Shared.updated(behaviour, super.updateShape(state, direction, neighborState, world, pos, neighborPos), world, pos);
		//#endif
	}


	/** Copper shapes weather like vanilla copper. */
	@Override
	protected void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
		Aging.tick(behaviour, state, world, pos, random);
	}
}
