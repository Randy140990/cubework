package com.insidethecube.cubework.block;

import java.util.Map;

import com.insidethecube.cubework.Material;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.Waterloggable;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

/** A carpet standing up: one pixel thick, flat against the face you click (or the far side of a floor). */
public class PanelBlock extends CwBlock implements Waterloggable {
	/** The side it's against. */
	public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;

	private static final Map<Direction, VoxelShape> SHAPES = Map.of(
			Direction.NORTH, Block.createCuboidShape(0, 0, 0, 16, 16, 1),
			Direction.SOUTH, Block.createCuboidShape(0, 0, 15, 16, 16, 16),
			Direction.WEST, Block.createCuboidShape(0, 0, 0, 1, 16, 16),
			Direction.EAST, Block.createCuboidShape(15, 0, 0, 16, 16, 16));

	public PanelBlock(Material material, AbstractBlock.Settings settings) {
		super(material, settings);
		setDefaultState(getDefaultState().with(FACING, Direction.NORTH).with(WATERLOGGED, false));
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING, WATERLOGGED);
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPES.get(state.get(FACING));
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		Direction side = ctx.getSide();
		Direction facing = side.getAxis().isHorizontal() ? side.getOpposite() : ctx.getHorizontalPlayerFacing();
		return placed(getDefaultState().with(FACING, facing).with(WATERLOGGED, waterAt(ctx)), ctx);
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
