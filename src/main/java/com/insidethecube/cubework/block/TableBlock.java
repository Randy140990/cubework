package com.insidethecube.cubework.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
//#if MC >= 1.21.2
import net.minecraft.world.level.ScheduledTickAccess;
//#else
import net.minecraft.world.level.LevelAccessor;
//#endif
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A low table shaped like a bed: a 6-pixel top on 3-pixel legs. Tables with the same legs placed side by
 * side join into one: the top runs on and legs stay only at the outside corners, so two make a bed-sized
 * table, four a square one, and a ring of them a table with a hole in the middle. The top's texture turns
 * with the way you face as you place it (for glazed terracotta's patterns and wood's grain).
 */
public class TableBlock extends Block {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
	public static final BooleanProperty EAST = BlockStateProperties.EAST;
	public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
	public static final BooleanProperty WEST = BlockStateProperties.WEST;

	private static final VoxelShape TOP = Block.box(0, 3, 0, 16, 9, 16);
	private static final VoxelShape LEG_NW = Block.box(0, 0, 0, 3, 3, 3);
	private static final VoxelShape LEG_NE = Block.box(13, 0, 0, 16, 3, 3);
	private static final VoxelShape LEG_SW = Block.box(0, 0, 13, 3, 3, 16);
	private static final VoxelShape LEG_SE = Block.box(13, 0, 13, 16, 3, 16);
	private static final VoxelShape[] SHAPES = new VoxelShape[16];

	static {
		for (int i = 0; i < 16; i++) {
			boolean n = (i & 1) != 0, e = (i & 2) != 0, s = (i & 4) != 0, w = (i & 8) != 0;
			VoxelShape shape = TOP;
			if (!n && !w) shape = Shapes.or(shape, LEG_NW);
			if (!n && !e) shape = Shapes.or(shape, LEG_NE);
			if (!s && !w) shape = Shapes.or(shape, LEG_SW);
			if (!s && !e) shape = Shapes.or(shape, LEG_SE);
			SHAPES[i] = shape.optimize();
		}
	}

	/** Which legs it has (oak, or a concrete colour): tables join only tables with the same legs. */
	private final String legs;

	public TableBlock(String legs, BlockBehaviour.Properties settings) {
		super(settings);
		this.legs = legs;
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, NORTH, EAST, SOUTH, WEST);
	}

	private static BooleanProperty side(Direction d) {
		return switch (d) {
			case NORTH -> NORTH;
			case EAST -> EAST;
			case SOUTH -> SOUTH;
			default -> WEST;
		};
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		int i = (state.getValue(NORTH) ? 1 : 0) | (state.getValue(EAST) ? 2 : 0) | (state.getValue(SOUTH) ? 4 : 0) | (state.getValue(WEST) ? 8 : 0);
		return SHAPES[i];
	}

	private boolean joins(BlockState other) {
		return other.getBlock() instanceof TableBlock t && t.legs.equals(legs);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		BlockState state = defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
		for (Direction d : Direction.Plane.HORIZONTAL) {
			state = state.setValue(side(d), joins(ctx.getLevel().getBlockState(ctx.getClickedPos().relative(d))));
		}
		return state;
	}

	@Override
	//#if MC >= 1.21.2
	protected BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickView, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighborState, RandomSource random) {
	//#else
	protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
		LevelAccessor tickView = world;
		RandomSource random = world.getRandom();
	//#endif
		return direction.getAxis().isHorizontal() ? state.setValue(side(direction), joins(neighborState)) : state;
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		BlockState out = state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
		for (Direction d : Direction.Plane.HORIZONTAL) out = out.setValue(side(rotation.rotate(d)), state.getValue(side(d)));
		return out;
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		BlockState out = state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
		for (Direction d : Direction.Plane.HORIZONTAL) out = out.setValue(side(mirror.mirror(d)), state.getValue(side(d)));
		return out;
	}
}
