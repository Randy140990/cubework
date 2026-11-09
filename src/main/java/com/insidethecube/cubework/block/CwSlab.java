package com.insidethecube.cubework.block;

import com.insidethecube.cubework.Material;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.block.BlockState;
import net.minecraft.block.SlabBlock;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;

/** The vanilla SlabBlock in a Cubework material: placed, connected and used exactly like vanilla's. */
public class CwSlab extends SlabBlock implements CwShape {
	private final Material material;

	public CwSlab(Material material, AbstractBlock.Settings settings) {
		super(settings);
		this.material = material;
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		int h = material.height();
		if (h == 16) return super.getOutlineShape(state, world, pos, context);
		return switch (state.get(TYPE)) {
			case BOTTOM -> Block.createCuboidShape(0, 0, 0, 16, h / 2.0, 16);
			case TOP -> Block.createCuboidShape(0, h / 2.0, 0, 16, h, 16);
			case DOUBLE -> Block.createCuboidShape(0, 0, 0, 16, h, 16);
		};
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
}
