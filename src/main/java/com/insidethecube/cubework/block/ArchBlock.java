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

/**
 * A round arch over one block: its underside curves smoothly from both bottom corners up to 4 pixels below
 * the top. It faces you as you place it, so you walk through it the way you were looking.
 */
public class ArchBlock extends CwBlock implements SimpleWaterloggedBlock {
	/** The way you walk through it. */
	public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;

	private static final VoxelShape ALONG_Z = Profiles.shape(Profiles.arch(), 0, 16, 0);
	private static final VoxelShape ALONG_X = Profiles.shape(Profiles.arch(), 0, 16, 1);
	private static final VoxelShape SOLID_Z = Profiles.shape(ArchGeometry.inside(false), 0, 16, 0);
	private static final VoxelShape SOLID_X = Profiles.shape(ArchGeometry.inside(false), 0, 16, 1);

	public ArchBlock(Material material, BlockBehaviour.Properties settings) {
		super(material, settings);
		registerDefaultState(defaultBlockState().setValue(AXIS, Direction.Axis.Z).setValue(WATERLOGGED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AXIS, WATERLOGGED);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return state.getValue(AXIS) == Direction.Axis.X ? ALONG_X : ALONG_Z;
	}

	/** The part that's solid all over, for hiding neighbours' faces (the model's curve is smooth). */
	@Override
	protected VoxelShape getOcclusionShape(BlockState state) {
		return state.getValue(AXIS) == Direction.Axis.X ? SOLID_X : SOLID_Z;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		return placed(defaultBlockState().setValue(AXIS, ctx.getHorizontalDirection().getAxis()).setValue(WATERLOGGED, waterAt(ctx)), ctx);
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		if (rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90) {
			return state.setValue(AXIS, state.getValue(AXIS) == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X);
		}
		return state;
	}
}
