package com.insidethecube.cubework.block;

import com.insidethecube.cubework.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A round column, 16, 12 or 8 pixels across, along the axis of the face you click (like logs). Drawn in code. */
public class RoundColumnBlock extends CwBlock implements SimpleWaterloggedBlock {
	public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;

	public final int width;

	public RoundColumnBlock(Material material, int width, BlockBehaviour.Properties settings) {
		super(material, settings);
		this.width = width;
		registerDefaultState(defaultBlockState().setValue(AXIS, Direction.Axis.Y).setValue(WATERLOGGED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AXIS, WATERLOGGED);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return RoundGeometry.column(width, state.getValue(AXIS));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		return placed(defaultBlockState().setValue(AXIS, ctx.getClickedFace().getAxis()).setValue(WATERLOGGED, waterAt(ctx)), ctx);
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		if (rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90) {
			Direction.Axis a = state.getValue(AXIS);
			if (a == Direction.Axis.X) return state.setValue(AXIS, Direction.Axis.Z);
			if (a == Direction.Axis.Z) return state.setValue(AXIS, Direction.Axis.X);
		}
		return state;
	}
}
