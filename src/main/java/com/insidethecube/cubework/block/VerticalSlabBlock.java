package com.insidethecube.cubework.block;

import java.util.Map;

import com.insidethecube.cubework.Material;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.Waterloggable;
import net.minecraft.entity.LivingEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;

/**
 * A slab standing on its edge. It sits against the face you click; clicked on a floor or ceiling, it
 * takes the half furthest from you. A second one in the open half makes a full block, like slabs.
 */
public class VerticalSlabBlock extends CwBlock implements Waterloggable {
	/** The side it's against. */
	public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;
	public static final BooleanProperty DOUBLE = BooleanProperty.of("double");

	private static final Map<Direction, VoxelShape> SHAPES = Map.of(
			Direction.NORTH, Block.createCuboidShape(0, 0, 0, 16, 16, 8),
			Direction.SOUTH, Block.createCuboidShape(0, 0, 8, 16, 16, 16),
			Direction.WEST, Block.createCuboidShape(0, 0, 0, 8, 16, 16),
			Direction.EAST, Block.createCuboidShape(8, 0, 0, 16, 16, 16));

	public VerticalSlabBlock(Material material, AbstractBlock.Settings settings) {
		super(material, settings);
		setDefaultState(getDefaultState().with(FACING, Direction.NORTH).with(DOUBLE, false).with(WATERLOGGED, false));
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING, DOUBLE, WATERLOGGED);
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return state.get(DOUBLE) ? VoxelShapes.fullCube() : SHAPES.get(state.get(FACING));
	}

	@Override
	protected boolean hasSidedTransparency(BlockState state) {
		return !state.get(DOUBLE);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		BlockPos pos = ctx.getBlockPos();
		BlockState existing = ctx.getWorld().getBlockState(pos);
		if (existing.isOf(this)) return placed(existing.with(DOUBLE, true).with(WATERLOGGED, false), ctx);
		Direction side = ctx.getSide();
		Direction facing = side.getAxis().isHorizontal() ? side.getOpposite() : ctx.getHorizontalPlayerFacing();
		return placed(getDefaultState().with(FACING, facing).with(WATERLOGGED, waterAt(ctx)), ctx);
	}

	@Override
	protected boolean canReplace(BlockState state, ItemPlacementContext ctx) {
		if (state.get(DOUBLE) || !ctx.getStack().isOf(asItem())) return false;
		if (!ctx.canReplaceExisting()) return true;
		Direction facing = state.get(FACING);
		Direction side = ctx.getSide();
		if (side == facing.getOpposite()) return true;
		if (side.getAxis().isVertical()) {
			Vec3d hit = ctx.getHitPos();
			BlockPos pos = ctx.getBlockPos();
			double along = facing.getAxis() == Direction.Axis.X ? hit.x - pos.getX() : hit.z - pos.getZ();
			return facing.getDirection() == Direction.AxisDirection.NEGATIVE ? along > 0.5 : along < 0.5;
		}
		return false;
	}

	@Override
	public boolean canFillWithFluid(LivingEntity filler, BlockView world, BlockPos pos, BlockState state, Fluid fluid) {
		return !state.get(DOUBLE) && Waterloggable.super.canFillWithFluid(filler, world, pos, state, fluid);
	}

	@Override
	public boolean tryFillWithFluid(WorldAccess world, BlockPos pos, BlockState state, FluidState fluidState) {
		return !state.get(DOUBLE) && Waterloggable.super.tryFillWithFluid(world, pos, state, fluidState);
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
