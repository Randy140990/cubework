package com.insidethecube.cubework.block;

import com.insidethecube.cubework.Material;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.Waterloggable;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.StairShape;
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
import net.minecraft.world.WorldView;

/**
 * A smooth ramp: placed like stairs (rising away from you, upside down on a ceiling or the top half of a
 * face), and bending into inner and outer corners by itself where slopes meet, like stairs do.
 */
public class SlopeBlock extends CwBlock implements Waterloggable {
	/** The high side. */
	public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;
	public static final EnumProperty<BlockHalf> HALF = Properties.BLOCK_HALF;
	public static final EnumProperty<StairShape> SHAPE = Properties.STAIR_SHAPE;

	public SlopeBlock(Material material, AbstractBlock.Settings settings) {
		super(material, settings);
		setDefaultState(getDefaultState().with(FACING, Direction.NORTH).with(HALF, BlockHalf.BOTTOM).with(SHAPE, StairShape.STRAIGHT)
				.with(WATERLOGGED, false));
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING, HALF, SHAPE, WATERLOGGED);
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SlopeGeometry.outline(state.get(FACING), state.get(HALF), state.get(SHAPE), base(state), rise(state), material.height());
	}

	@Override
	protected VoxelShape getCullingShape(BlockState state) {
		return SlopeGeometry.inside(state.get(FACING), state.get(HALF), state.get(SHAPE), base(state), rise(state), material.height());
	}

	/** Where the slope starts, in pixels (gentle slopes sit on the step below). */
	public int base(BlockState state) {
		return 0;
	}

	/** How many pixels it rises across the block. */
	public int rise(BlockState state) {
		return material.height();
	}

	/** Which neighbours it turns corners with: slopes of the same kind (any material). */
	protected boolean joins(BlockState state, BlockState other) {
		return other.getBlock().getClass() == getClass();
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		Direction side = ctx.getSide();
		BlockPos pos = ctx.getBlockPos();
		boolean bottom = side != Direction.DOWN && (side == Direction.UP || !(ctx.getHitPos().y - pos.getY() > 0.5));
		BlockState state = getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing()).with(HALF, bottom ? BlockHalf.BOTTOM : BlockHalf.TOP)
				.with(WATERLOGGED, waterAt(ctx));
		state = placing(state, ctx);
		return placed(state.with(SHAPE, shapeAt(state, ctx.getWorld(), pos)), ctx);
	}

	/** A last say over a slope about to be placed (gentle slopes pick their step here). */
	protected BlockState placing(BlockState state, ItemPlacementContext ctx) {
		return state;
	}

	@Override
	protected BlockState shapeUpdate(BlockState state, WorldView world, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState) {
		return direction.getAxis().isHorizontal() ? state.with(SHAPE, shapeAt(state, world, pos)) : state;
	}

	/** The same rule vanilla stairs use to turn corners, applied between slopes. */
	protected StairShape shapeAt(BlockState state, BlockView world, BlockPos pos) {
		Direction facing = state.get(FACING);
		BlockState front = world.getBlockState(pos.offset(facing));
		if (joins(state, front) && state.get(HALF) == front.get(HALF)) {
			Direction d = front.get(FACING);
			if (d.getAxis() != facing.getAxis() && differs(state, world, pos, d.getOpposite())) {
				return d == facing.rotateYCounterclockwise() ? StairShape.OUTER_LEFT : StairShape.OUTER_RIGHT;
			}
		}
		BlockState back = world.getBlockState(pos.offset(facing.getOpposite()));
		if (joins(state, back) && state.get(HALF) == back.get(HALF)) {
			Direction d = back.get(FACING);
			if (d.getAxis() != facing.getAxis() && differs(state, world, pos, d)) {
				return d == facing.rotateYCounterclockwise() ? StairShape.INNER_LEFT : StairShape.INNER_RIGHT;
			}
		}
		return StairShape.STRAIGHT;
	}

	private boolean differs(BlockState state, BlockView world, BlockPos pos, Direction dir) {
		BlockState other = world.getBlockState(pos.offset(dir));
		return !joins(state, other) || other.get(FACING) != state.get(FACING) || other.get(HALF) != state.get(HALF);
	}

	@Override
	protected BlockState rotate(BlockState state, BlockRotation rotation) {
		return state.with(FACING, rotation.rotate(state.get(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, BlockMirror mirror) {
		Direction facing = state.get(FACING);
		StairShape shape = state.get(SHAPE);
		boolean flipsCorners = (mirror == BlockMirror.LEFT_RIGHT && facing.getAxis() == Direction.Axis.Z)
				|| (mirror == BlockMirror.FRONT_BACK && facing.getAxis() == Direction.Axis.X);
		if (flipsCorners) {
			BlockState turned = state.rotate(BlockRotation.CLOCKWISE_180);
			return switch (shape) {
				case INNER_LEFT -> turned.with(SHAPE, StairShape.INNER_RIGHT);
				case INNER_RIGHT -> turned.with(SHAPE, StairShape.INNER_LEFT);
				case OUTER_LEFT -> turned.with(SHAPE, StairShape.OUTER_RIGHT);
				case OUTER_RIGHT -> turned.with(SHAPE, StairShape.OUTER_LEFT);
				default -> turned;
			};
		}
		return state;
	}
}
