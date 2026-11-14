package com.insidethecube.cubework.block;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
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
 * A low table shaped like a bed: a 6-pixel top on 3-pixel legs; or a tall one, its top level with a full
 * block on 10-pixel legs. Glass tops are 2 pixels thick, on longer legs. Tables with the same legs placed side by
 * side join into one: the top runs on and legs stay only at the outside corners, so two make a bed-sized
 * table, four a square one, and a ring of them a table with a hole in the middle. The top's texture turns
 * with the way you face as you place it (for glazed terracotta's patterns and wood's grain).
 * <p>
 * Which neighbours it joins isn't kept in the block state (see Joining): only the way it faces is.
 */
public class TableBlock extends Block implements Joining {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	/** The joins its model is written with (blockstate under joined/): north, east, south, west (bits 1, 2, 4, 8). */
	public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
	public static final BooleanProperty EAST = BlockStateProperties.EAST;
	public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
	public static final BooleanProperty WEST = BlockStateProperties.WEST;
	private static final List<BooleanProperty> JOINS = List.of(NORTH, EAST, SOUTH, WEST);
	private static final Direction[] SIDES = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

	private final VoxelShape[] shapes = new VoxelShape[16];

	/** The top's height and thickness in pixels (the legs reach up to the top). */
	public static int topOf(boolean tall) {
		return tall ? 16 : 9;
	}

	public static int thicknessOf(boolean glass) {
		return glass ? 2 : 6;
	}

	/** Which legs it has (oak, or a concrete colour) and how tall it is: tables join only tables with the same legs and height. */
	private final String legs;

	public TableBlock(String legs, BlockBehaviour.Properties settings) {
		this(legs, false, false, settings);
	}

	public TableBlock(String legs, boolean tall, boolean glass, BlockBehaviour.Properties settings) {
		super(settings.dynamicShape());
		this.legs = legs + (tall ? "/tall" : "");
		int y1 = topOf(tall), y0 = y1 - thicknessOf(glass);
		VoxelShape top = Block.box(0, y0, 0, 16, y1, 16);
		VoxelShape nw = Block.box(0, 0, 0, 3, y0, 3), ne = Block.box(13, 0, 0, 16, y0, 3);
		VoxelShape sw = Block.box(0, 0, 13, 3, y0, 16), se = Block.box(13, 0, 13, 16, y0, 16);
		for (int i = 0; i < 16; i++) {
			boolean n = (i & 1) != 0, e = (i & 2) != 0, s = (i & 4) != 0, w = (i & 8) != 0;
			VoxelShape shape = top;
			if (!n && !w) shape = Shapes.or(shape, nw);
			if (!n && !e) shape = Shapes.or(shape, ne);
			if (!s && !w) shape = Shapes.or(shape, sw);
			if (!s && !e) shape = Shapes.or(shape, se);
			shapes[i] = shape.optimize();
		}
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public List<BooleanProperty> joinProperties() {
		return JOINS;
	}

	@Override
	public int joins(BlockGetter world, BlockPos pos, BlockState state) {
		int mask = 0;
		for (int i = 0; i < 4; i++) if (joins(world.getBlockState(pos.relative(SIDES[i])))) mask |= 1 << i;
		return mask;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return shapes[joins(world, pos, state)];
	}

	private boolean joins(BlockState other) {
		return other.getBlock() instanceof TableBlock t && t.legs.equals(legs);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
	}
}
