package com.insidethecube.cubework.block;

import com.insidethecube.cubework.Material;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

/**
 * Builds up from the floor like snow: 1 to 8 layers, 2 pixels each. Using it on the top of a layer adds
 * one; the eighth turns it into the full block.
 */
public class LayerBlock extends CwBlock {
	public static final IntProperty LAYERS = Properties.LAYERS;

	private static final VoxelShape[] SHAPES = new VoxelShape[9];

	static {
		for (int n = 1; n <= 8; n++) SHAPES[n] = Block.createCuboidShape(0, 0, 0, 16, n * 2, 16);
	}

	public LayerBlock(Material material, AbstractBlock.Settings settings) {
		super(material, settings);
		setDefaultState(getDefaultState().with(LAYERS, 1));
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(LAYERS);
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPES[state.get(LAYERS)];
	}

	@Override
	protected boolean hasSidedTransparency(BlockState state) {
		return state.get(LAYERS) < 8;
	}

	@Override
	protected boolean canReplace(BlockState state, ItemPlacementContext ctx) {
		if (!ctx.getStack().isOf(asItem()) || state.get(LAYERS) >= 8) return false;
		return !ctx.canReplaceExisting() || ctx.getSide() == Direction.UP;
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		BlockState existing = ctx.getWorld().getBlockState(ctx.getBlockPos());
		if (existing.isOf(this)) {
			int n = Math.min(8, existing.get(LAYERS) + 1);
			return placed(n == 8 ? material.base().getDefaultState() : existing.with(LAYERS, n), ctx);
		}
		return placed(getDefaultState(), ctx);
	}
}
