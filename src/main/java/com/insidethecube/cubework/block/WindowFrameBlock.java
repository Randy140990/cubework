package com.insidethecube.cubework.block;

import com.insidethecube.cubework.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A window frame: a 4-pixel border round an 8-wide opening, 4, 8, 12 or 16 pixels deep, square, diamond or
 * round (a porthole; joined round frames make one big round or oval window, see RoundWindowGeometry).
 * It sits against the face you click. Frames facing the same way join, whatever their depth (a shallower
 * one leaves a ledge) and square or diamond: where they meet the border goes, so a stack makes a tall slit
 * (sill at the bottom, lintel at the top), a row makes a long one, and a square of them one big opening.
 * <p>
 * Placed while sneaking on the top of a block it lies flat on the floor, or on the underside of a block flat
 * against the ceiling: a hatch or skylight, joining other lying frames on the same side into bigger openings
 * (placing against the side of a lying frame lays the new one the same way, sneaking or not).
 */
public class WindowFrameBlock extends CwBlock implements SimpleWaterloggedBlock {
	/** The side it's against. */
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final BooleanProperty UP = BlockStateProperties.UP;
	public static final BooleanProperty DOWN = BlockStateProperties.DOWN;
	public static final BooleanProperty LEFT = BooleanProperty.create("left");
	public static final BooleanProperty RIGHT = BooleanProperty.create("right");
	/** On a wall (upright), or lying flat on the floor or against the ceiling. */
	public static final EnumProperty<AttachFace> FACE = BlockStateProperties.ATTACH_FACE;

	/** The opening's shape. */
	public enum Style {
		SQUARE, DIAMOND, ROUND
	}

	private final int depth;
	private final Style style;
	private final VoxelShape[] shapes = new VoxelShape[3 * 4 * 16];

	public WindowFrameBlock(Material material, int depth, Style style, BlockBehaviour.Properties settings) {
		super(material, settings);
		this.depth = depth;
		this.style = style;
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(UP, false).setValue(DOWN, false).setValue(LEFT, false).setValue(RIGHT, false)
				.setValue(FACE, AttachFace.WALL).setValue(WATERLOGGED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, UP, DOWN, LEFT, RIGHT, FACE, WATERLOGGED);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		boolean u = state.getValue(UP), d = state.getValue(DOWN), l = state.getValue(LEFT), r = state.getValue(RIGHT);
		int turns = state.getValue(FACING).get2DDataValue() + 2 & 3;
		AttachFace face = state.getValue(FACE);
		int i = face.ordinal() * 64 + turns * 16 + (u ? 8 : 0) + (d ? 4 : 0) + (l ? 2 : 0) + (r ? 1 : 0);
		VoxelShape s = shapes[i];
		if (s == null) {
			boolean[][] mask = style == Style.ROUND ? RoundWindowGeometry.mask(u, d, l, r) : Profiles.window(u, d, l, r, style == Style.DIAMOND);
			shapes[i] = s = face == AttachFace.WALL ? Profiles.shape(mask, 0, depth, turns) : lying(mask, depth / 16.0, face == AttachFace.CEILING, turns);
		}
		return s;
	}

	/**
	 * The hit box of a lying frame: the upright frame facing north tipped onto the floor (its top towards
	 * the north) or up against the ceiling (its top towards the south), as the model is, then turned.
	 */
	private static VoxelShape lying(boolean[][] mask, double d, boolean ceiling, int turns) {
		VoxelShape out = Shapes.empty();
		for (int x = 0; x < 16; x++) {
			for (int y = 0; y < 16; y++) {
				if (!mask[x][y]) continue;
				double x0 = x / 16.0, x1 = (x + 1) / 16.0, z0 = ceiling ? y / 16.0 : 1 - (y + 1) / 16.0, z1 = z0 + 1 / 16.0;
				for (int t = 0; t < turns; t++) {
					double nx0 = 1 - z1, nx1 = 1 - z0;
					z0 = x0;
					z1 = x1;
					x0 = nx0;
					x1 = nx1;
				}
				out = Shapes.or(out, ceiling ? Shapes.box(x0, 1 - d, z0, x1, 1, z1) : Shapes.box(x0, 0, z0, x1, d, z1));
			}
		}
		return out.optimize();
	}

	/** How deep the frame is, in pixels. */
	public int depth() {
		return depth;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		Direction side = ctx.getClickedFace();
		Direction facing = side.getAxis().isHorizontal() ? side.getOpposite() : ctx.getHorizontalDirection();
		AttachFace face = AttachFace.WALL;
		BlockState against = ctx.getLevel().getBlockState(ctx.getClickedPos().relative(side.getOpposite()));
		if (against.getBlock() instanceof WindowFrameBlock && against.getValue(FACE) != AttachFace.WALL && side.getAxis().isHorizontal()) {
			face = against.getValue(FACE);
		} else if (ctx.getPlayer() != null && ctx.getPlayer().isShiftKeyDown() && !side.getAxis().isHorizontal()) {
			face = side == Direction.UP ? AttachFace.FLOOR : AttachFace.CEILING;
		}
		// Lying frames all face north, so any two side by side join.
		if (face != AttachFace.WALL) facing = Direction.NORTH;
		BlockState state = defaultBlockState().setValue(FACING, facing).setValue(FACE, face).setValue(WATERLOGGED, waterAt(ctx));
		return placed(joined(state, ctx.getLevel(), ctx.getClickedPos()), ctx);
	}

	@Override
	protected BlockState shapeUpdate(BlockState state, LevelReader world, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState) {
		return joined(state, world, pos);
	}

	/** Which neighbours it joins: any window frame (any material, depth, square or diamond) facing the same way. */
	private BlockState joined(BlockState state, BlockGetter world, BlockPos pos) {
		Direction f = state.getValue(FACING);
		AttachFace face = state.getValue(FACE);
		// The frame's own up and down: on a wall, up and down; lying, the way its top was tipped and the opposite.
		Direction up = face == AttachFace.WALL ? Direction.UP : face == AttachFace.FLOOR ? f : f.getOpposite();
		return state.setValue(UP, joins(state, world.getBlockState(pos.relative(up)))).setValue(DOWN, joins(state, world.getBlockState(pos.relative(up.getOpposite()))))
				.setValue(LEFT, joins(state, world.getBlockState(pos.relative(f.getCounterClockWise()))))
				.setValue(RIGHT, joins(state, world.getBlockState(pos.relative(f.getClockWise()))));
	}

	private boolean joins(BlockState state, BlockState other) {
		return other.getBlock() instanceof WindowFrameBlock && other.getValue(FACING) == state.getValue(FACING) && other.getValue(FACE) == state.getValue(FACE);
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
