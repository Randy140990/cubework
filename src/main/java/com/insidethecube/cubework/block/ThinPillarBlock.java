package com.insidethecube.cubework.block;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import com.insidethecube.cubework.Material;

/**
 * A narrower pillar of a block (12, 8 or 4 pixels square, the full 16 long) in the middle of the block,
 * placed along the face you click like logs. Thin logs are these too, with no material.
 */
public class ThinPillarBlock extends RotatedPillarBlock implements CwShape {
	private final Material material;
	private final int width;
	private final Map<Direction.Axis, VoxelShape> shapes;

	public ThinPillarBlock(Material material, int width, BlockBehaviour.Properties settings) {
		super(settings);
		this.material = material;
		this.width = width;
		double a = (16 - width) / 2.0, b = 16 - a;
		shapes = Map.of(
				Direction.Axis.Y, Block.box(a, 0, a, b, 16, b),
				Direction.Axis.X, Block.box(0, a, a, 16, b, b),
				Direction.Axis.Z, Block.box(a, a, 0, b, b, 16));
	}

	@Override
	public Material material() {
		return material;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return shapes.get(state.getValue(AXIS));
	}

	@Override
	protected boolean useShapeForLightOcclusion(BlockState state) {
		return width < 16;
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
		Aging.tick(material, state, world, pos, random);
	}
}
