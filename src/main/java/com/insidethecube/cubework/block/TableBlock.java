package com.insidethecube.cubework.block;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;

/**
 * A low table shaped like a bed: a 6-pixel top on 3-pixel legs. Tables with the same legs placed side by
 * side join into one: the top runs on and legs stay only at the outside corners, so two make a bed-sized
 * table, four a square one, and a ring of them a table with a hole in the middle. The top's texture turns
 * with the way you face as you place it (for glazed terracotta's patterns and wood's grain).
 */
public class TableBlock extends Block {
	public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;
	public static final BooleanProperty NORTH = Properties.NORTH;
	public static final BooleanProperty EAST = Properties.EAST;
	public static final BooleanProperty SOUTH = Properties.SOUTH;
	public static final BooleanProperty WEST = Properties.WEST;

	private static final VoxelShape TOP = Block.createCuboidShape(0, 3, 0, 16, 9, 16);
	private static final VoxelShape LEG_NW = Block.createCuboidShape(0, 0, 0, 3, 3, 3);
	private static final VoxelShape LEG_NE = Block.createCuboidShape(13, 0, 0, 16, 3, 3);
	private static final VoxelShape LEG_SW = Block.createCuboidShape(0, 0, 13, 3, 3, 16);
	private static final VoxelShape LEG_SE = Block.createCuboidShape(13, 0, 13, 16, 3, 16);
	private static final VoxelShape[] SHAPES = new VoxelShape[16];

	static {
		for (int i = 0; i < 16; i++) {
			boolean n = (i & 1) != 0, e = (i & 2) != 0, s = (i & 4) != 0, w = (i & 8) != 0;
			VoxelShape shape = TOP;
			if (!n && !w) shape = VoxelShapes.union(shape, LEG_NW);
			if (!n && !e) shape = VoxelShapes.union(shape, LEG_NE);
			if (!s && !w) shape = VoxelShapes.union(shape, LEG_SW);
			if (!s && !e) shape = VoxelShapes.union(shape, LEG_SE);
			SHAPES[i] = shape.simplify();
		}
	}

	/** Which legs it has (oak, or a concrete colour): tables join only tables with the same legs. */
	private final String legs;

	public TableBlock(String legs, AbstractBlock.Settings settings) {
		super(settings);
		this.legs = legs;
		setDefaultState(getDefaultState().with(FACING, Direction.NORTH).with(NORTH, false).with(EAST, false).with(SOUTH, false).with(WEST, false));
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
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
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		int i = (state.get(NORTH) ? 1 : 0) | (state.get(EAST) ? 2 : 0) | (state.get(SOUTH) ? 4 : 0) | (state.get(WEST) ? 8 : 0);
		return SHAPES[i];
	}

	private boolean joins(BlockState other) {
		return other.getBlock() instanceof TableBlock t && t.legs.equals(legs);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		BlockState state = getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing());
		for (Direction d : Direction.Type.HORIZONTAL) {
			state = state.with(side(d), joins(ctx.getWorld().getBlockState(ctx.getBlockPos().offset(d))));
		}
		return state;
	}

	@Override
	protected BlockState getStateForNeighborUpdate(BlockState state, WorldView world, ScheduledTickView tickView, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighborState, Random random) {
		return direction.getAxis().isHorizontal() ? state.with(side(direction), joins(neighborState)) : state;
	}

	@Override
	protected BlockState rotate(BlockState state, BlockRotation rotation) {
		BlockState out = state.with(FACING, rotation.rotate(state.get(FACING)));
		for (Direction d : Direction.Type.HORIZONTAL) out = out.with(side(rotation.rotate(d)), state.get(side(d)));
		return out;
	}

	@Override
	protected BlockState mirror(BlockState state, BlockMirror mirror) {
		BlockState out = state.with(FACING, mirror.apply(state.get(FACING)));
		for (Direction d : Direction.Type.HORIZONTAL) out = out.with(side(mirror.apply(d)), state.get(side(d)));
		return out;
	}
}
