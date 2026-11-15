package com.insidethecube.cubework.block;

import java.util.List;

import com.insidethecube.cubework.Behaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A smooth ramp: placed like stairs (rising away from you, upside down on a ceiling or the top half of a
 * face), and bending into inner and outer corners by itself where slopes meet, like stairs do.
 * <p>
 * The corner isn't kept in the block state (see Joining): it's worked out from the neighbours for the hit
 * box and as the model is drawn, so a slope has 16 block states rather than 80.
 */
public class SlopeBlock extends CwBlock implements SimpleWaterloggedBlock, Joining {
	/** The high side. */
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final EnumProperty<Half> HALF = BlockStateProperties.HALF;
	/** The corners, in the order of their join numbers (StairsShape's own order: straight first). */
	private static final StairsShape[] SHAPES = StairsShape.values();

	public SlopeBlock(Behaviour behaviour, BlockBehaviour.Properties settings) {
		super(behaviour, settings.dynamicShape());
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(HALF, Half.BOTTOM).setValue(WATERLOGGED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, HALF, WATERLOGGED);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return SlopeGeometry.outline(state.getValue(FACING), state.getValue(HALF), shapeAt(state, world, pos), base(state), rise(state), behaviour.height());
	}

	@Override
	//#if MC >= 1.21.2
	protected VoxelShape getOcclusionShape(BlockState state) {
	//#else
	protected VoxelShape getOcclusionShape(BlockState state, BlockGetter world, BlockPos pos) {
	//#endif
		// Without the neighbours: only what it fills whatever corner it turns, so nothing next to it is hidden wrongly.
		return SlopeGeometry.insideAny(state.getValue(FACING), state.getValue(HALF), base(state), rise(state), behaviour.height());
	}

	@Override
	public List<BooleanProperty> joinProperties() {
		return List.of();
	}

	/** The corner it turns here, as a join number (see SHAPES); straight with no world to look at. */
	@Override
	public int joins(BlockGetter world, BlockPos pos, BlockState state) {
		return shapeAt(state, world, pos).ordinal();
	}

	/** The corner for a join number. */
	public static StairsShape shape(int joins) {
		return joins >= 0 && joins < SHAPES.length ? SHAPES[joins] : StairsShape.STRAIGHT;
	}

	/** Where the slope starts, in pixels (gentle slopes sit on the step below). */
	public double base(BlockState state) {
		return 0;
	}

	/** How many pixels it rises across the block. */
	public double rise(BlockState state) {
		return behaviour.height();
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
		return placed(placing(state, ctx), ctx);
	}

	/** A last say over a slope about to be placed (gentle slopes pick their step here). */
	protected BlockState placing(BlockState state, BlockPlaceContext ctx) {
		return state;
	}

	/** The same rule vanilla stairs use to turn corners, applied between slopes. */
	public StairsShape shapeAt(BlockState state, BlockGetter world, BlockPos pos) {
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
		boolean flips = (mirror == Mirror.LEFT_RIGHT && facing.getAxis() == Direction.Axis.Z) || (mirror == Mirror.FRONT_BACK && facing.getAxis() == Direction.Axis.X);
		return flips ? state.rotate(Rotation.CLOCKWISE_180) : state;
	}
}
