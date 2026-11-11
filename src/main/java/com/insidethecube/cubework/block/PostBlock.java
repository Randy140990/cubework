package com.insidethecube.cubework.block;

import java.util.Map;
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
import com.insidethecube.cubework.Material;

/** A thin pillar, 6 pixels across, running along the axis of the face you click (like logs). */
public class PostBlock extends CwBlock implements SimpleWaterloggedBlock {
	public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;

	private static final Map<Direction.Axis, VoxelShape> SHAPES = Map.of(
			Direction.Axis.Y, Block.box(5, 0, 5, 11, 16, 11),
			Direction.Axis.X, Block.box(0, 5, 5, 16, 11, 11),
			Direction.Axis.Z, Block.box(5, 5, 0, 11, 11, 16));

	public PostBlock(Material material, BlockBehaviour.Properties settings) {
		super(material, settings);
		registerDefaultState(defaultBlockState().setValue(AXIS, Direction.Axis.Y).setValue(WATERLOGGED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AXIS, WATERLOGGED);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return SHAPES.get(state.getValue(AXIS));
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
