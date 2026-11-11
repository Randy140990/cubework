package com.insidethecube.cubework.block;

import com.insidethecube.cubework.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;

/** The vanilla FenceGateBlock in a Cubework material: placed, connected and used exactly like vanilla's. */
public class CwFenceGate extends FenceGateBlock implements CwShape {
	private final Material material;

	public CwFenceGate(Material material, WoodType wood, BlockBehaviour.Properties settings) {
		super(wood, settings);
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
	protected BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickView, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		return Shared.updated(material, super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random), world, pos);
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
