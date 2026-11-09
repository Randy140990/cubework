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
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

/**
 * A round arch over one block: its underside curves smoothly from both bottom corners up to 4 pixels below
 * the top. It faces you as you place it, so you walk through it the way you were looking.
 */
public class ArchBlock extends CwBlock implements Waterloggable {
	/** The way you walk through it. */
	public static final EnumProperty<Direction.Axis> AXIS = Properties.HORIZONTAL_AXIS;

	private static final VoxelShape ALONG_Z = Profiles.shape(Profiles.arch(), 0, 16, 0);
	private static final VoxelShape ALONG_X = Profiles.shape(Profiles.arch(), 0, 16, 1);
	private static final VoxelShape SOLID_Z = Profiles.shape(ArchGeometry.inside(false), 0, 16, 0);
	private static final VoxelShape SOLID_X = Profiles.shape(ArchGeometry.inside(false), 0, 16, 1);

	public ArchBlock(Material material, AbstractBlock.Settings settings) {
		super(material, settings);
		setDefaultState(getDefaultState().with(AXIS, Direction.Axis.Z).with(WATERLOGGED, false));
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(AXIS, WATERLOGGED);
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return state.get(AXIS) == Direction.Axis.X ? ALONG_X : ALONG_Z;
	}

	/** The part that's solid all over, for hiding neighbours' faces (the model's curve is smooth). */
	@Override
	protected VoxelShape getCullingShape(BlockState state) {
		return state.get(AXIS) == Direction.Axis.X ? SOLID_X : SOLID_Z;
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		return placed(getDefaultState().with(AXIS, ctx.getHorizontalPlayerFacing().getAxis()).with(WATERLOGGED, waterAt(ctx)), ctx);
	}

	@Override
	protected BlockState rotate(BlockState state, BlockRotation rotation) {
		if (rotation == BlockRotation.CLOCKWISE_90 || rotation == BlockRotation.COUNTERCLOCKWISE_90) {
			return state.with(AXIS, state.get(AXIS) == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X);
		}
		return state;
	}
}
