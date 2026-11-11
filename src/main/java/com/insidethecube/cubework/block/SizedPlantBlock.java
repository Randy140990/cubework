package com.insidethecube.cubework.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A plant at another size: the same plant, drawn smaller or bigger, growing where the original grows. */
public class SizedPlantBlock extends VegetationBlock {
	/** Where it can grow. */
	public enum Soil {
		/** Grass, flowers: dirt, grass, moss and farmland. */
		DIRT,
		/** Short dry grass: also sand and terracotta. */
		DRY,
		/** Mushrooms: any solid block top. */
		SOLID,
		/** Fungi: nylium, soul soil, mycelium and dirt. */
		NETHER
	}

	private final Soil soil;
	private final VoxelShape shape;

	public SizedPlantBlock(Soil soil, double scale, BlockBehaviour.Properties settings) {
		super(settings);
		this.soil = soil;
		double half = Math.min(8, 6 * scale), h = Math.min(16, 13 * scale);
		this.shape = Block.box(8 - half, 0, 8 - half, 8 + half, Math.max(2, h), 8 + half);
	}

	@Override
	protected MapCodec<? extends VegetationBlock> codec() {
		return simpleCodec(settings -> new SizedPlantBlock(Soil.DIRT, 1, settings));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		Vec3 offset = state.getOffset(pos);
		return shape.move(offset.x, offset.y, offset.z);
	}

	@Override
	protected boolean mayPlaceOn(BlockState floor, BlockGetter world, BlockPos pos) {
		return switch (soil) {
			case DIRT -> super.mayPlaceOn(floor, world, pos);
			case DRY -> super.mayPlaceOn(floor, world, pos) || floor.is(BlockTags.SAND) || floor.is(BlockTags.TERRACOTTA);
			case SOLID -> floor.isFaceSturdy(world, pos, Direction.UP);
			case NETHER -> floor.is(BlockTags.NYLIUM) || floor.is(net.minecraft.world.level.block.Blocks.SOUL_SOIL) || floor.is(net.minecraft.world.level.block.Blocks.MYCELIUM)
					|| super.mayPlaceOn(floor, world, pos);
		};
	}
}
