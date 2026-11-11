package com.insidethecube.cubework.block;

import com.insidethecube.cubework.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A smooth ramp: placed like stairs (rising away from you, upside down on a ceiling or the top half of a
 * face), and bending into inner and outer corners by itself where slopes meet, like stairs do.
 */
public class SlopeBlock extends CwBlock implements SimpleWaterloggedBlock {
	/** The high side. */
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final EnumProperty<Half> HALF = BlockStateProperties.HALF;
	public static final EnumProperty<StairsShape> SHAPE = BlockStateProperties.STAIRS_SHAPE;

	public SlopeBlock(Material material, BlockBehaviour.Properties settings) {
		super(material, settings);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(HALF, Half.BOTTOM).setValue(SHAPE, StairsShape.STRAIGHT)
				.setValue(WATERLOGGED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, HALF, SHAPE, WATERLOGGED);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return SlopeGeometry.outline(state.getValue(FACING), state.getValue(HALF), state.getValue(SHAPE), base(state), rise(state), material.height());
	}

	@Override
	protected VoxelShape getOcclusionShape(BlockState state) {
		return SlopeGeometry.inside(state.getValue(FACING), state.getValue(HALF), state.getValue(SHAPE), base(state), rise(state), material.height());
	}

	/** Where the slope starts, in pixels (gentle slopes sit on the step below). */
	public double base(BlockState state) {
		return 0;
	}

	/** How many pixels it rises across the block. */
	public double rise(BlockState state) {
		return material.height();
	}

	/** Which neighbours it turns corners with: slopes of the same kind (any material). */
	protected boolean joins(BlockState state, BlockState other) {
		return other.getBlock().getClass() == getClass();
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		Direction side = ctx.getClickedFace();
		BlockPos pos = ctx.getClickedPos();
		boolean bottom = side != Direction.DOWN && (side == Direction.UP || !(ctx.getClickLocation().y - pos.getY() > 0.5));
		BlockState state = defaultBlockState().setValue(FACING, ctx.getHorizontalDirection()).setValue(HALF, bottom ? Half.BOTTOM : Half.TOP)
				.setValue(WATERLOGGED, waterAt(ctx));
		state = placing(state, ctx);
		return placed(state.setValue(SHAPE, shapeAt(state, ctx.getLevel(), pos)), ctx);
	}

	/** A last say over a slope about to be placed (gentle slopes pick their step here). */
	protected BlockState placing(BlockState state, BlockPlaceContext ctx) {
		return state;
	}

	@Override
	protected BlockState shapeUpdate(BlockState state, LevelReader world, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState) {
		return direction.getAxis().isHorizontal() ? state.setValue(SHAPE, shapeAt(state, world, pos)) : state;
	}

	/** The same rule vanilla stairs use to turn corners, applied between slopes. */
	protected StairsShape shapeAt(BlockState state, BlockGetter world, BlockPos pos) {
		Direction facing = state.getValue(FACING);
		BlockState front = world.getBlockState(pos.relative(facing));
		if (joins(state, front) && state.getValue(HALF) == front.getValue(HALF)) {
			Direction d = front.getValue(FACING);
			if (d.getAxis() != facing.getAxis() && differs(state, world, pos, d.getOpposite())) {
				return d == facing.getCounterClockWise() ? StairsShape.OUTER_LEFT : StairsShape.OUTER_RIGHT;
			}
		}
		BlockState back = world.getBlockState(pos.relative(facing.getOpposite()));
		if (joins(state, back) && state.getValue(HALF) == back.getValue(HALF)) {
			Direction d = back.getValue(FACING);
			if (d.getAxis() != facing.getAxis() && differs(state, world, pos, d)) {
				return d == facing.getCounterClockWise() ? StairsShape.INNER_LEFT : StairsShape.INNER_RIGHT;
			}
		}
		return StairsShape.STRAIGHT;
	}

	private boolean differs(BlockState state, BlockGetter world, BlockPos pos, Direction dir) {
		BlockState other = world.getBlockState(pos.relative(dir));
		return !joins(state, other) || other.getValue(FACING) != state.getValue(FACING) || other.getValue(HALF) != state.getValue(HALF);
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		Direction facing = state.getValue(FACING);
		StairsShape shape = state.getValue(SHAPE);
		boolean flipsCorners = (mirror == Mirror.LEFT_RIGHT && facing.getAxis() == Direction.Axis.Z)
				|| (mirror == Mirror.FRONT_BACK && facing.getAxis() == Direction.Axis.X);
		if (flipsCorners) {
			BlockState turned = state.rotate(Rotation.CLOCKWISE_180);
			return switch (shape) {
				case INNER_LEFT -> turned.setValue(SHAPE, StairsShape.INNER_RIGHT);
				case INNER_RIGHT -> turned.setValue(SHAPE, StairsShape.INNER_LEFT);
				case OUTER_LEFT -> turned.setValue(SHAPE, StairsShape.OUTER_RIGHT);
				case OUTER_RIGHT -> turned.setValue(SHAPE, StairsShape.OUTER_LEFT);
				default -> turned;
			};
		}
		return state;
	}
}
