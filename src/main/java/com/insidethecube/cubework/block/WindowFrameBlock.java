package com.insidethecube.cubework.block;

import com.insidethecube.cubework.Material;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.Waterloggable;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
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
 * A window frame: a 4-pixel border round an 8-wide opening, 4, 8, 12 or 16 pixels deep, square or diamond.
 * It sits against the face you click. Frames facing the same way join, whatever their depth (a shallower
 * one leaves a ledge) and square or diamond: where they meet the border goes, so a stack makes a tall slit
 * (sill at the bottom, lintel at the top), a row makes a long one, and a square of them one big opening.
 */
public class WindowFrameBlock extends CwBlock implements Waterloggable {
	/** The side it's against. */
	public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;
	public static final BooleanProperty UP = Properties.UP;
	public static final BooleanProperty DOWN = Properties.DOWN;
	public static final BooleanProperty LEFT = BooleanProperty.of("left");
	public static final BooleanProperty RIGHT = BooleanProperty.of("right");

	private final int depth;
	private final boolean diamond;
	private final VoxelShape[] shapes = new VoxelShape[4 * 16];

	public WindowFrameBlock(Material material, int depth, boolean diamond, AbstractBlock.Settings settings) {
		super(material, settings);
		this.depth = depth;
		this.diamond = diamond;
		setDefaultState(getDefaultState().with(FACING, Direction.NORTH).with(UP, false).with(DOWN, false).with(LEFT, false).with(RIGHT, false)
				.with(WATERLOGGED, false));
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING, UP, DOWN, LEFT, RIGHT, WATERLOGGED);
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		boolean u = state.get(UP), d = state.get(DOWN), l = state.get(LEFT), r = state.get(RIGHT);
		int turns = state.get(FACING).getHorizontalQuarterTurns() + 2 & 3;
		int i = turns * 16 + (u ? 8 : 0) + (d ? 4 : 0) + (l ? 2 : 0) + (r ? 1 : 0);
		VoxelShape s = shapes[i];
		if (s == null) shapes[i] = s = Profiles.shape(Profiles.window(u, d, l, r, diamond), 0, depth, turns);
		return s;
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		Direction side = ctx.getSide();
		Direction facing = side.getAxis().isHorizontal() ? side.getOpposite() : ctx.getHorizontalPlayerFacing();
		BlockState state = getDefaultState().with(FACING, facing).with(WATERLOGGED, waterAt(ctx));
		return placed(joined(state, ctx.getWorld(), ctx.getBlockPos()), ctx);
	}

	@Override
	protected BlockState shapeUpdate(BlockState state, WorldView world, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState) {
		return joined(state, world, pos);
	}

	/** Which neighbours it joins: any window frame (any material, depth, square or diamond) facing the same way. */
	private BlockState joined(BlockState state, BlockView world, BlockPos pos) {
		Direction f = state.get(FACING);
		return state.with(UP, joins(state, world.getBlockState(pos.up()))).with(DOWN, joins(state, world.getBlockState(pos.down())))
				.with(LEFT, joins(state, world.getBlockState(pos.offset(f.rotateYCounterclockwise()))))
				.with(RIGHT, joins(state, world.getBlockState(pos.offset(f.rotateYClockwise()))));
	}

	private boolean joins(BlockState state, BlockState other) {
		return other.getBlock() instanceof WindowFrameBlock && other.get(FACING) == state.get(FACING);
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
