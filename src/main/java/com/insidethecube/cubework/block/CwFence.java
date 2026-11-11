package com.insidethecube.cubework.block;

import com.insidethecube.cubework.Material;
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
	private final Material material;

	public CwFence(Material material, BlockBehaviour.Properties settings) {
		super(settings);
		this.material = material;
	}

	@Override
	public Material material() {
		return material;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		return Shared.placed(material, super.getStateForPlacement(ctx), ctx);
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
		return Shared.updated(material, super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random), world, pos);
		//#else
		return Shared.updated(material, super.updateShape(state, direction, neighborState, world, pos, neighborPos), world, pos);
		//#endif
	}

	@Override
	protected boolean skipRendering(BlockState state, BlockState stateFrom, Direction direction) {
		return Shared.hidesFace(material, state, stateFrom, direction) || super.skipRendering(state, stateFrom, direction);
	}

	/** Copper shapes weather like vanilla copper. */
	@Override
	protected void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
		Aging.tick(material, state, world, pos, random);
	}
}
