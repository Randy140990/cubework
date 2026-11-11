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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A window frame: a 4-pixel border round an 8-wide opening, 4, 8, 12 or 16 pixels deep, square or diamond.
 * It sits against the face you click. Frames facing the same way join, whatever their depth (a shallower
 * one leaves a ledge) and square or diamond: where they meet the border goes, so a stack makes a tall slit
 * (sill at the bottom, lintel at the top), a row makes a long one, and a square of them one big opening.
 */
public class WindowFrameBlock extends CwBlock implements SimpleWaterloggedBlock {
	/** The side it's against. */
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final BooleanProperty UP = BlockStateProperties.UP;
	public static final BooleanProperty DOWN = BlockStateProperties.DOWN;
	public static final BooleanProperty LEFT = BooleanProperty.create("left");
	public static final BooleanProperty RIGHT = BooleanProperty.create("right");

	private final int depth;
	private final boolean diamond;
	private final VoxelShape[] shapes = new VoxelShape[4 * 16];

	public WindowFrameBlock(Material material, int depth, boolean diamond, BlockBehaviour.Properties settings) {
		super(material, settings);
		this.depth = depth;
		this.diamond = diamond;
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(UP, false).setValue(DOWN, false).setValue(LEFT, false).setValue(RIGHT, false)
				.setValue(WATERLOGGED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, UP, DOWN, LEFT, RIGHT, WATERLOGGED);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		boolean u = state.getValue(UP), d = state.getValue(DOWN), l = state.getValue(LEFT), r = state.getValue(RIGHT);
		int turns = state.getValue(FACING).get2DDataValue() + 2 & 3;
		int i = turns * 16 + (u ? 8 : 0) + (d ? 4 : 0) + (l ? 2 : 0) + (r ? 1 : 0);
		VoxelShape s = shapes[i];
		if (s == null) shapes[i] = s = Profiles.shape(Profiles.window(u, d, l, r, diamond), 0, depth, turns);
		return s;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		Direction side = ctx.getClickedFace();
		Direction facing = side.getAxis().isHorizontal() ? side.getOpposite() : ctx.getHorizontalDirection();
		BlockState state = defaultBlockState().setValue(FACING, facing).setValue(WATERLOGGED, waterAt(ctx));
		return placed(joined(state, ctx.getLevel(), ctx.getClickedPos()), ctx);
	}

	@Override
	protected BlockState shapeUpdate(BlockState state, LevelReader world, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState) {
		return joined(state, world, pos);
	}

	/** Which neighbours it joins: any window frame (any material, depth, square or diamond) facing the same way. */
	private BlockState joined(BlockState state, BlockGetter world, BlockPos pos) {
		Direction f = state.getValue(FACING);
		return state.setValue(UP, joins(state, world.getBlockState(pos.above()))).setValue(DOWN, joins(state, world.getBlockState(pos.below())))
				.setValue(LEFT, joins(state, world.getBlockState(pos.relative(f.getCounterClockWise()))))
				.setValue(RIGHT, joins(state, world.getBlockState(pos.relative(f.getClockWise()))));
	}

	private boolean joins(BlockState state, BlockState other) {
		return other.getBlock() instanceof WindowFrameBlock && other.getValue(FACING) == state.getValue(FACING);
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}
}
