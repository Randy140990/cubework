package com.insidethecube.cubework.block;

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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A round corner: a quarter of a round tower one block across (outer), or the curved fill for the inside
 * of a corner (inner). It sits in the corner you're looking towards. Drawn in code.
 */
public class RoundCornerBlock extends CwBlock implements SimpleWaterloggedBlock {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

	public final boolean inner;

	public RoundCornerBlock(Behaviour behaviour, boolean inner, BlockBehaviour.Properties settings) {
		super(behaviour, settings);
		this.inner = inner;
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, WATERLOGGED);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return RoundGeometry.corner(state.getValue(FACING), inner);
	}

	@Override
	//#if MC >= 1.21.2
	protected VoxelShape getOcclusionShape(BlockState state) {
	//#else
	protected VoxelShape getOcclusionShape(BlockState state, BlockGetter world, BlockPos pos) {
	//#endif
		return RoundGeometry.cornerSides(state.getValue(FACING));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		return placed(defaultBlockState().setValue(FACING, Corners.looking(ctx)).setValue(WATERLOGGED, waterAt(ctx)), ctx);
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return Corners.rotate(state, FACING, rotation);
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return Corners.mirror(state, FACING, mirror);
	}
}
