package com.insidethecube.cubework.block;

import java.util.EnumMap;
import java.util.Map;

import com.insidethecube.cubework.Material;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

/**
 * Snow-style layers on the side of a block: 1 to 8, each 2 pixels, growing out from the wall towards
 * you. Using it on its outer face adds one; the eighth turns it into the full block.
 */
public class VerticalLayerBlock extends CwBlock {
	/** The way it grows: away from the wall it's on. */
	public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;
	public static final IntProperty LAYERS = Properties.LAYERS;

	private static final Map<Direction, VoxelShape[]> SHAPES = new EnumMap<>(Direction.class);

	static {
		for (Direction d : Direction.Type.HORIZONTAL) {
			VoxelShape[] s = new VoxelShape[9];
			for (int n = 1; n <= 8; n++) s[n] = shape(d, n * 2);
			SHAPES.put(d, s);
		}
	}

	/** A slab of this thickness against the wall opposite the way it grows. */
	static VoxelShape shape(Direction grows, double t) {
		return switch (grows) {
			case SOUTH -> Block.createCuboidShape(0, 0, 0, 16, 16, t);
			case NORTH -> Block.createCuboidShape(0, 0, 16 - t, 16, 16, 16);
			case EAST -> Block.createCuboidShape(0, 0, 0, t, 16, 16);
			default -> Block.createCuboidShape(16 - t, 0, 0, 16, 16, 16);
		};
	}

	public VerticalLayerBlock(Material material, AbstractBlock.Settings settings) {
		super(material, settings);
		setDefaultState(getDefaultState().with(FACING, Direction.SOUTH).with(LAYERS, 1));
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING, LAYERS);
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPES.get(state.get(FACING))[state.get(LAYERS)];
	}

	@Override
	protected boolean hasSidedTransparency(BlockState state) {
		return state.get(LAYERS) < 8;
	}

	@Override
	protected boolean canReplace(BlockState state, ItemPlacementContext ctx) {
		if (!ctx.getStack().isOf(asItem()) || state.get(LAYERS) >= 8) return false;
		return !ctx.canReplaceExisting() || ctx.getSide() == state.get(FACING);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		BlockState existing = ctx.getWorld().getBlockState(ctx.getBlockPos());
		if (existing.isOf(this)) {
			int n = Math.min(8, existing.get(LAYERS) + 1);
			return placed(n == 8 ? material.base().getDefaultState() : existing.with(LAYERS, n), ctx);
		}
		Direction side = ctx.getSide();
		Direction grows = side.getAxis().isHorizontal() ? side : ctx.getHorizontalPlayerFacing().getOpposite();
		return placed(getDefaultState().with(FACING, grows), ctx);
	}

	@Override
	protected BlockState rotate(BlockState state, BlockRotation rotation) {
		return state.with(FACING, rotation.rotate(state.get(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, BlockMirror mirror) {
		return state.rotate(mirror.getRotation(state.get(FACING)));
	}
}
