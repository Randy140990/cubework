package com.insidethecube.cubework.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.PlantBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

/** A plant at another size: the same plant, drawn smaller or bigger, growing where the original grows. */
public class SizedPlantBlock extends PlantBlock {
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

	public SizedPlantBlock(Soil soil, double scale, AbstractBlock.Settings settings) {
		super(settings);
		this.soil = soil;
		double half = Math.min(8, 6 * scale), h = Math.min(16, 13 * scale);
		this.shape = Block.createCuboidShape(8 - half, 0, 8 - half, 8 + half, Math.max(2, h), 8 + half);
	}

	@Override
	protected MapCodec<? extends PlantBlock> getCodec() {
		return createCodec(settings -> new SizedPlantBlock(Soil.DIRT, 1, settings));
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		Vec3d offset = state.getModelOffset(pos);
		return shape.offset(offset.x, offset.y, offset.z);
	}

	@Override
	protected boolean canPlantOnTop(BlockState floor, BlockView world, BlockPos pos) {
		return switch (soil) {
			case DIRT -> super.canPlantOnTop(floor, world, pos);
			case DRY -> super.canPlantOnTop(floor, world, pos) || floor.isIn(BlockTags.SAND) || floor.isIn(BlockTags.TERRACOTTA);
			case SOLID -> floor.isSideSolidFullSquare(world, pos, Direction.UP);
			case NETHER -> floor.isIn(BlockTags.NYLIUM) || floor.isOf(net.minecraft.block.Blocks.SOUL_SOIL) || floor.isOf(net.minecraft.block.Blocks.MYCELIUM)
					|| super.canPlantOnTop(floor, world, pos);
		};
	}
}
