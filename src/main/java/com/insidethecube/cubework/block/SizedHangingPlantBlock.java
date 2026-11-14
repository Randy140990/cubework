package com.insidethecube.cubework.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
//#if MC >= 1.21.2
import net.minecraft.world.level.ScheduledTickAccess;
//#else
import net.minecraft.world.level.LevelAccessor;
//#endif
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A plant hanging from a ceiling at another size (the spore blossom): it needs a solid block above. */
public class SizedHangingPlantBlock extends Block {
	private final VoxelShape shape;

	public SizedHangingPlantBlock(double scale, BlockBehaviour.Properties settings) {
		super(settings);
		double half = Math.min(8, 6 * scale);
		this.shape = Block.box(8 - half, 13, 8 - half, 8 + half, 16, 8 + half);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return shape;
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
		return Block.canSupportCenter(world, pos.above(), Direction.DOWN);
	}

	@Override
	//#if MC >= 1.21.2
	protected BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickView, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighborState, RandomSource random) {
	//#else
	protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
	//#endif
		return direction == Direction.UP && !canSurvive(state, world, pos) ? Blocks.AIR.defaultBlockState() : state;
	}
}
