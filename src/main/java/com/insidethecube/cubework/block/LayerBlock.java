package com.insidethecube.cubework.block;

import com.insidethecube.cubework.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Builds up from the floor like snow: 1 to 8 layers, 2 pixels each. Using it on the top of a layer adds
 * one; the eighth turns it into the full block.
 */
public class LayerBlock extends CwBlock {
	public static final IntegerProperty LAYERS = BlockStateProperties.LAYERS;

	private static final VoxelShape[] SHAPES = new VoxelShape[9];

	static {
		for (int n = 1; n <= 8; n++) SHAPES[n] = Block.box(0, 0, 0, 16, n * 2, 16);
	}

	public LayerBlock(Material material, BlockBehaviour.Properties settings) {
		super(material, settings);
		registerDefaultState(defaultBlockState().setValue(LAYERS, 1));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LAYERS);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return SHAPES[state.getValue(LAYERS)];
	}

	@Override
	protected boolean useShapeForLightOcclusion(BlockState state) {
		return state.getValue(LAYERS) < 8;
	}

	@Override
	protected boolean canBeReplaced(BlockState state, BlockPlaceContext ctx) {
		if (!ctx.getItemInHand().is(asItem()) || state.getValue(LAYERS) >= 8) return false;
		return !ctx.replacingClickedOnBlock() || ctx.getClickedFace() == Direction.UP;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		BlockState existing = ctx.getLevel().getBlockState(ctx.getClickedPos());
		if (existing.is(this)) {
			int n = Math.min(8, existing.getValue(LAYERS) + 1);
			return placed(n == 8 ? material.base().defaultBlockState() : existing.setValue(LAYERS, n), ctx);
		}
		return placed(defaultBlockState(), ctx);
	}
}
