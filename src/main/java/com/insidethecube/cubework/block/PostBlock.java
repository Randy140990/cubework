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
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

/** A thin pillar, 6 pixels across, running along the axis of the face you click (like logs). */
public class PostBlock extends CwBlock implements Waterloggable {
	public static final EnumProperty<Direction.Axis> AXIS = Properties.AXIS;

	private static final Map<Direction.Axis, VoxelShape> SHAPES = Map.of(
			Direction.Axis.Y, Block.createCuboidShape(5, 0, 5, 11, 16, 11),
			Direction.Axis.X, Block.createCuboidShape(0, 5, 5, 16, 11, 11),
			Direction.Axis.Z, Block.createCuboidShape(5, 5, 0, 11, 11, 16));

	public PostBlock(Material material, AbstractBlock.Settings settings) {
		super(material, settings);
		setDefaultState(getDefaultState().with(AXIS, Direction.Axis.Y).with(WATERLOGGED, false));
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(AXIS, WATERLOGGED);
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPES.get(state.get(AXIS));
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		return placed(getDefaultState().with(AXIS, ctx.getSide().getAxis()).with(WATERLOGGED, waterAt(ctx)), ctx);
	}

	@Override
	protected BlockState rotate(BlockState state, BlockRotation rotation) {
		if (rotation == BlockRotation.CLOCKWISE_90 || rotation == BlockRotation.COUNTERCLOCKWISE_90) {
			Direction.Axis a = state.get(AXIS);
			if (a == Direction.Axis.X) return state.with(AXIS, Direction.Axis.Z);
			if (a == Direction.Axis.Z) return state.with(AXIS, Direction.Axis.X);
		}
		return state;
	}
}
