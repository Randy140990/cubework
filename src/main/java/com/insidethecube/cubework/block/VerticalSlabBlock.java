package com.insidethecube.cubework.block;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
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
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import com.insidethecube.cubework.Material;

/**
 * A slab standing on its edge. It sits against the face you click; clicked on a floor or ceiling, it
 * takes the half furthest from you. A second one in the open half makes a full block, like slabs.
 */
public class VerticalSlabBlock extends CwBlock implements SimpleWaterloggedBlock {
	/** The side it's against. */
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final BooleanProperty DOUBLE = BooleanProperty.create("double");

	private final Map<Direction, VoxelShape> shapes;
	private final VoxelShape full;

	public VerticalSlabBlock(Material material, BlockBehaviour.Properties settings) {
		super(material, settings);
		int h = material.height();
		shapes = Map.of(
				Direction.NORTH, Block.box(0, 0, 0, 16, h, 8),
				Direction.SOUTH, Block.box(0, 0, 8, 16, h, 16),
				Direction.WEST, Block.box(0, 0, 0, 8, h, 16),
				Direction.EAST, Block.box(8, 0, 0, 16, h, 16));
		full = Block.box(0, 0, 0, 16, h, 16);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(DOUBLE, false).setValue(WATERLOGGED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, DOUBLE, WATERLOGGED);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return state.getValue(DOUBLE) ? full : shapes.get(state.getValue(FACING));
	}

	@Override
	protected boolean useShapeForLightOcclusion(BlockState state) {
		return !state.getValue(DOUBLE);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		BlockPos pos = ctx.getClickedPos();
		BlockState existing = ctx.getLevel().getBlockState(pos);
		if (existing.is(this)) return placed(existing.setValue(DOUBLE, true).setValue(WATERLOGGED, false), ctx);
		Direction side = ctx.getClickedFace();
		Direction facing = side.getAxis().isHorizontal() ? side.getOpposite() : ctx.getHorizontalDirection();
		return placed(defaultBlockState().setValue(FACING, facing).setValue(WATERLOGGED, waterAt(ctx)), ctx);
	}

	@Override
	protected boolean canBeReplaced(BlockState state, BlockPlaceContext ctx) {
		if (state.getValue(DOUBLE) || !ctx.getItemInHand().is(asItem())) return false;
		if (!ctx.replacingClickedOnBlock()) return true;
		Direction facing = state.getValue(FACING);
		Direction side = ctx.getClickedFace();
		if (side == facing.getOpposite()) return true;
		if (side.getAxis().isVertical()) {
			Vec3 hit = ctx.getClickLocation();
			BlockPos pos = ctx.getClickedPos();
			double along = facing.getAxis() == Direction.Axis.X ? hit.x - pos.getX() : hit.z - pos.getZ();
			return facing.getAxisDirection() == Direction.AxisDirection.NEGATIVE ? along > 0.5 : along < 0.5;
		}
		return false;
	}

	@Override
	public boolean canPlaceLiquid(LivingEntity filler, BlockGetter world, BlockPos pos, BlockState state, Fluid fluid) {
		return !state.getValue(DOUBLE) && SimpleWaterloggedBlock.super.canPlaceLiquid(filler, world, pos, state, fluid);
	}

	@Override
	public boolean placeLiquid(LevelAccessor world, BlockPos pos, BlockState state, FluidState fluidState) {
		return !state.getValue(DOUBLE) && SimpleWaterloggedBlock.super.placeLiquid(world, pos, state, fluidState);
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
