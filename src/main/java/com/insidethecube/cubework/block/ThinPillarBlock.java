package com.insidethecube.cubework.block;

import java.util.Map;

import com.insidethecube.cubework.Material;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

/**
 * A narrower pillar of a block (12, 8 or 4 pixels square, the full 16 long) in the middle of the block,
 * placed along the face you click like logs. Thin logs are these too, with no material.
 */
public class ThinPillarBlock extends PillarBlock implements CwShape {
	private final Material material;
	private final int width;
	private final Map<Direction.Axis, VoxelShape> shapes;

	public ThinPillarBlock(Material material, int width, AbstractBlock.Settings settings) {
		super(settings);
		this.material = material;
		this.width = width;
		double a = (16 - width) / 2.0, b = 16 - a;
		shapes = Map.of(
				Direction.Axis.Y, Block.createCuboidShape(a, 0, a, b, 16, b),
				Direction.Axis.X, Block.createCuboidShape(0, a, a, 16, b, b),
				Direction.Axis.Z, Block.createCuboidShape(a, a, 0, b, b, 16));
	}

	@Override
	public Material material() {
		return material;
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return shapes.get(state.get(AXIS));
	}

	@Override
	protected boolean hasSidedTransparency(BlockState state) {
		return width < 16;
	}

	@Override
	protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		Aging.tick(material, state, world, pos, random);
	}
}
