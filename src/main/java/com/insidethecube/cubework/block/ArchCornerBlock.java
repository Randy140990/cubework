package com.insidethecube.cubework.block;

import com.insidethecube.cubework.Material;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.Waterloggable;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

/**
 * Half of a two-wide arch: its underside curves smoothly from the floor at its back up to 4 pixels below the
 * top at its front. Its back goes against the block you click, so it reaches out from that wall; on a floor
 * it backs onto the far side and reaches towards you. Two facing each other across a two-block gap make an
 * arch; top quarter slabs between them make a three- or four-wide one.
 */
public class ArchCornerBlock extends CwBlock implements Waterloggable {
	/** The high side, towards the middle of the arch. */
	public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;

	private static final VoxelShape[] SHAPES = new VoxelShape[4];
	private static final VoxelShape[] SOLID = new VoxelShape[4];

	static {
		for (int t = 0; t < 4; t++) {
			SHAPES[t] = Profiles.shape(Profiles.archCorner(), 0, 16, t);
			SOLID[t] = Profiles.shape(ArchGeometry.inside(true), 0, 16, t);
		}
	}

	public ArchCornerBlock(Material material, AbstractBlock.Settings settings) {
		super(material, settings);
		setDefaultState(getDefaultState().with(FACING, Direction.EAST).with(WATERLOGGED, false));
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING, WATERLOGGED);
	}

	/** The model is drawn rising east, so east is no turn. */
	public static int turns(Direction high) {
		return high.getHorizontalQuarterTurns() + 1 & 3;
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPES[turns(state.get(FACING))];
	}

	/** The part that's solid all over, for hiding neighbours' faces (the model's curve is smooth). */
	@Override
	protected VoxelShape getCullingShape(BlockState state) {
		return SOLID[turns(state.get(FACING))];
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		Direction side = ctx.getSide();
		Direction high = side.getAxis().isHorizontal() ? side : ctx.getHorizontalPlayerFacing().getOpposite();
		return placed(getDefaultState().with(FACING, high).with(WATERLOGGED, waterAt(ctx)), ctx);
	}

	@Override
	protected BlockState rotate(BlockState state, BlockRotation rotation) {
		return state.with(FACING, rotation.rotate(state.get(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, BlockMirror mirror) {
		return state.with(FACING, mirror.apply(state.get(FACING)));
	}
}
