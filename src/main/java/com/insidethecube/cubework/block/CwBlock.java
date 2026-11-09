package com.insidethecube.cubework.block;

import com.insidethecube.cubework.Material;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;
import net.minecraft.server.world.ServerWorld;

/** The shapes vanilla doesn't have: what they all share. */
public abstract class CwBlock extends Block implements CwShape {
	public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;

	protected final Material material;

	protected CwBlock(Material material, AbstractBlock.Settings settings) {
		super(settings);
		this.material = material;
	}

	@Override
	public Material material() {
		return material;
	}

	/** Called by subclasses on the state they're about to place. */
	protected BlockState placed(BlockState state, ItemPlacementContext ctx) {
		return Shared.placed(material, state, ctx);
	}

	protected static boolean waterAt(ItemPlacementContext ctx) {
		return ctx.getWorld().getFluidState(ctx.getBlockPos()).getFluid() == Fluids.WATER;
	}

	@Override
	protected BlockState getStateForNeighborUpdate(BlockState state, WorldView world, ScheduledTickView tickView, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighborState, Random random) {
		if (state.contains(WATERLOGGED) && state.get(WATERLOGGED)) tickView.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
		BlockState next = shapeUpdate(state, world, pos, direction, neighborPos, neighborState);
		return Shared.updated(material, next, world, pos);
	}

	/** A neighbour changed: the shape's own update (corners for slopes). */
	protected BlockState shapeUpdate(BlockState state, WorldView world, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState) {
		return state;
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		return state.contains(WATERLOGGED) && state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
	}

	@Override
	protected boolean hasSidedTransparency(BlockState state) {
		return true;
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
