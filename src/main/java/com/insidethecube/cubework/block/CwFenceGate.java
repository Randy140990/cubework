package com.insidethecube.cubework.block;

import com.insidethecube.cubework.Material;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.WoodType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;
import net.minecraft.server.world.ServerWorld;

/** The vanilla FenceGateBlock in a Cubework material: placed, connected and used exactly like vanilla's. */
public class CwFenceGate extends FenceGateBlock implements CwShape {
	private final Material material;

	public CwFenceGate(Material material, WoodType wood, AbstractBlock.Settings settings) {
		super(wood, settings);
		this.material = material;
	}

	@Override
	public Material material() {
		return material;
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		return Shared.placed(material, super.getPlacementState(ctx), ctx);
	}

	@Override
	protected BlockState getStateForNeighborUpdate(BlockState state, WorldView world, ScheduledTickView tickView, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighborState, Random random) {
		return Shared.updated(material, super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random), world, pos);
	}

	@Override
	protected boolean isSideInvisible(BlockState state, BlockState stateFrom, Direction direction) {
		return Shared.hidesFace(material, state, stateFrom, direction) || super.isSideInvisible(state, stateFrom, direction);
	}

	/** Copper shapes weather like vanilla copper. */
	@Override
	protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		Aging.tick(material, state, world, pos, random);
	}
}
