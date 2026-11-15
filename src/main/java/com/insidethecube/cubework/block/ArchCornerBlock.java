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
 * Half of a two-wide arch: its underside curves smoothly from the floor at its back up to 4 pixels below the
 * top at its front. Its back goes against the block you click, so it reaches out from that wall; on a floor
 * it backs onto the far side and reaches towards you. Two facing each other across a two-block gap make an
 * arch; top quarter slabs between them make a three- or four-wide one.
 */
public class ArchCornerBlock extends CwBlock implements SimpleWaterloggedBlock {
	/** The high side, towards the middle of the arch. */
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

	private static final VoxelShape[] SHAPES = new VoxelShape[4];
	private static final VoxelShape[] SOLID = new VoxelShape[4];

	static {
		for (int t = 0; t < 4; t++) {
			SHAPES[t] = Profiles.shape(Profiles.archCorner(), 0, 16, t);
			SOLID[t] = Profiles.shape(ArchGeometry.inside(true), 0, 16, t);
		}
	}

	public ArchCornerBlock(Behaviour behaviour, BlockBehaviour.Properties settings) {
		super(behaviour, settings);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.EAST).setValue(WATERLOGGED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, WATERLOGGED);
	}

	/** The model is drawn rising east, so east is no turn. */
	public static int turns(Direction high) {
		return high.get2DDataValue() + 1 & 3;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return SHAPES[turns(state.getValue(FACING))];
	}

	/** The part that's solid all over, for hiding neighbours' faces (the model's curve is smooth). */
	@Override
	//#if MC >= 1.21.2
	protected VoxelShape getOcclusionShape(BlockState state) {
	//#else
	protected VoxelShape getOcclusionShape(BlockState state, BlockGetter world, BlockPos pos) {
	//#endif
		return SOLID[turns(state.getValue(FACING))];
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		Direction side = ctx.getClickedFace();
		Direction high = side.getAxis().isHorizontal() ? side : ctx.getHorizontalDirection().getOpposite();
		return placed(defaultBlockState().setValue(FACING, high).setValue(WATERLOGGED, waterAt(ctx)), ctx);
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
