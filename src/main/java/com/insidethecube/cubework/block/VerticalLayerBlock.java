package com.insidethecube.cubework.block;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import com.insidethecube.cubework.Material;

/**
 * Snow-style layers on the side of a block: 1 to 8, each 2 pixels, growing out from the wall towards
 * you. Using it on its outer face adds one; the eighth turns it into the full block.
 */
public class VerticalLayerBlock extends CwBlock {
	/** The way it grows: away from the wall it's on. */
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final IntegerProperty LAYERS = BlockStateProperties.LAYERS;

	private final Map<Direction, VoxelShape[]> shapes = new EnumMap<>(Direction.class);

	/** A slab of this thickness against the wall opposite the way it grows. */
	static VoxelShape shape(Direction grows, double t, double h) {
		return switch (grows) {
			case SOUTH -> Block.box(0, 0, 0, 16, h, t);
			case NORTH -> Block.box(0, 0, 16 - t, 16, h, 16);
			case EAST -> Block.box(0, 0, 0, t, h, 16);
			default -> Block.box(16 - t, 0, 0, 16, h, 16);
		};
	}

	public VerticalLayerBlock(Material material, BlockBehaviour.Properties settings) {
		super(material, settings);
		for (Direction d : Direction.Plane.HORIZONTAL) {
			VoxelShape[] s = new VoxelShape[9];
			for (int n = 1; n <= 8; n++) s[n] = shape(d, n * 2, material.height());
			shapes.put(d, s);
		}
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.SOUTH).setValue(LAYERS, 1));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, LAYERS);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return shapes.get(state.getValue(FACING))[state.getValue(LAYERS)];
	}

	@Override
	protected boolean useShapeForLightOcclusion(BlockState state) {
		return state.getValue(LAYERS) < 8;
	}

	@Override
	protected boolean canBeReplaced(BlockState state, BlockPlaceContext ctx) {
		if (!ctx.getItemInHand().is(asItem()) || state.getValue(LAYERS) >= 8) return false;
		return !ctx.replacingClickedOnBlock() || ctx.getClickedFace() == state.getValue(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		BlockState existing = ctx.getLevel().getBlockState(ctx.getClickedPos());
		if (existing.is(this)) {
			int n = Math.min(8, existing.getValue(LAYERS) + 1);
			return placed(n == 8 ? material.base().defaultBlockState() : existing.setValue(LAYERS, n), ctx);
		}
		Direction side = ctx.getClickedFace();
		Direction grows = side.getAxis().isHorizontal() ? side : ctx.getHorizontalDirection().getOpposite();
		return placed(defaultBlockState().setValue(FACING, grows), ctx);
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
