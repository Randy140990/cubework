package com.insidethecube.cubework.block;

import java.util.Map;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

/**
 * A log or stem, full length but narrower (12, 8 or 4 pixels), in the middle of the block. Placed along
 * the face you click, like logs, with the bark and the top ring shrunk to fit.
 */
public class ThinLogBlock extends PillarBlock {
	private final Map<Direction.Axis, VoxelShape> shapes;

	public ThinLogBlock(int width, AbstractBlock.Settings settings) {
		super(settings);
		double a = (16 - width) / 2.0, b = 16 - a;
		shapes = Map.of(
				Direction.Axis.Y, Block.createCuboidShape(a, 0, a, b, 16, b),
				Direction.Axis.X, Block.createCuboidShape(0, a, a, 16, b, b),
				Direction.Axis.Z, Block.createCuboidShape(a, a, 0, b, b, 16));
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return shapes.get(state.get(AXIS));
	}

	@Override
	protected boolean hasSidedTransparency(BlockState state) {
		return true;
	}
}
