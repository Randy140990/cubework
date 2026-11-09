package com.insidethecube.cubework.block;

import com.insidethecube.cubework.Material;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.Waterloggable;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

/** A slab 4 pixels thick, at the bottom or the top of the block, placed like a slab (the top for arch spans). */
public class QuarterSlabBlock extends CwBlock implements Waterloggable {
	public static final EnumProperty<BlockHalf> HALF = Properties.BLOCK_HALF;

	private static final VoxelShape BOTTOM = Block.createCuboidShape(0, 0, 0, 16, 4, 16);
	private static final VoxelShape TOP = Block.createCuboidShape(0, 12, 0, 16, 16, 16);

	public QuarterSlabBlock(Material material, AbstractBlock.Settings settings) {
		super(material, settings);
		setDefaultState(getDefaultState().with(HALF, BlockHalf.BOTTOM).with(WATERLOGGED, false));
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(HALF, WATERLOGGED);
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return state.get(HALF) == BlockHalf.TOP ? TOP : BOTTOM;
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		Direction side = ctx.getSide();
		boolean top = side == Direction.DOWN || (side != Direction.UP && ctx.getHitPos().y - ctx.getBlockPos().getY() > 0.5);
		return placed(getDefaultState().with(HALF, top ? BlockHalf.TOP : BlockHalf.BOTTOM).with(WATERLOGGED, waterAt(ctx)), ctx);
	}
}
