package com.insidethecube.cubework.block;

import com.insidethecube.cubework.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A slab 4 pixels thick, at the bottom or the top of the block, placed like a slab (the top for arch spans). */
public class QuarterSlabBlock extends CwBlock implements SimpleWaterloggedBlock {
	public static final EnumProperty<Half> HALF = BlockStateProperties.HALF;

	private static final VoxelShape BOTTOM = Block.box(0, 0, 0, 16, 4, 16);
	private static final VoxelShape TOP = Block.box(0, 12, 0, 16, 16, 16);

	public QuarterSlabBlock(Material material, BlockBehaviour.Properties settings) {
		super(material, settings);
		registerDefaultState(defaultBlockState().setValue(HALF, Half.BOTTOM).setValue(WATERLOGGED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(HALF, WATERLOGGED);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return state.getValue(HALF) == Half.TOP ? TOP : BOTTOM;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		Direction side = ctx.getClickedFace();
		boolean top = side == Direction.DOWN || (side != Direction.UP && ctx.getClickLocation().y - ctx.getClickedPos().getY() > 0.5);
		return placed(defaultBlockState().setValue(HALF, top ? Half.TOP : Half.BOTTOM).setValue(WATERLOGGED, waterAt(ctx)), ctx);
	}
}
