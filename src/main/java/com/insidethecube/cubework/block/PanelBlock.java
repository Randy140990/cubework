package com.insidethecube.cubework.block;

import java.util.Map;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import com.insidethecube.cubework.Material;

/** A carpet standing up: one pixel thick, flat against the face you click (or the far side of a floor). */
public class PanelBlock extends CwBlock implements SimpleWaterloggedBlock {
	/** The side it's against. */
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

	private static final Map<Direction, VoxelShape> SHAPES = Map.of(
			Direction.NORTH, Block.box(0, 0, 0, 16, 16, 1),
			Direction.SOUTH, Block.box(0, 0, 15, 16, 16, 16),
			Direction.WEST, Block.box(0, 0, 0, 1, 16, 16),
			Direction.EAST, Block.box(15, 0, 0, 16, 16, 16));

	public PanelBlock(Material material, BlockBehaviour.Properties settings) {
		super(material, settings);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, WATERLOGGED);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return SHAPES.get(state.getValue(FACING));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		Direction side = ctx.getClickedFace();
		Direction facing = side.getAxis().isHorizontal() ? side.getOpposite() : ctx.getHorizontalDirection();
		return placed(defaultBlockState().setValue(FACING, facing).setValue(WATERLOGGED, waterAt(ctx)), ctx);
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
