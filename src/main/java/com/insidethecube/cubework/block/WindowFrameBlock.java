package com.insidethecube.cubework.block;

import java.util.List;

import com.insidethecube.cubework.Material;
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
 * <p>
 * Which neighbours it joins isn't kept in the block state (see Joining): only the way it faces and whether
 * it's under water, so a frame has 12 block states rather than 192.
 */
public class WindowFrameBlock extends CwBlock implements SimpleWaterloggedBlock, Joining {
	/** The side it's against: north, east, south or west on a wall; up lying on the floor, down against the ceiling. */
	public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
	/** The joins its model is written with (blockstate under joined/, or RoundWindowModel): up, down, left, right (bits 1, 2, 4, 8). */
	public static final BooleanProperty UP = BlockStateProperties.UP;
	public static final BooleanProperty DOWN = BlockStateProperties.DOWN;
	public static final BooleanProperty LEFT = BooleanProperty.create("left");
	public static final BooleanProperty RIGHT = BooleanProperty.create("right");
	public static final int JOIN_UP = 1, JOIN_DOWN = 2, JOIN_LEFT = 4, JOIN_RIGHT = 8;
	private static final List<BooleanProperty> JOINS = List.of(UP, DOWN, LEFT, RIGHT);

	/** The opening's shape. */
	public enum Style {
		SQUARE, DIAMOND, ROUND
	}

	private final int depth;
	private final Style style;
	private final VoxelShape[] shapes = new VoxelShape[3 * 4 * 16];

	/** Upright, lying on the floor or against the ceiling. */
	public static AttachFace face(BlockState state) {
		Direction f = state.getValue(FACING);
		return f == Direction.UP ? AttachFace.FLOOR : f == Direction.DOWN ? AttachFace.CEILING : AttachFace.WALL;
	}

	/** The way it faces round: its facing on a wall; lying frames are built facing north. */
	public static Direction horizontal(BlockState state) {
		Direction f = state.getValue(FACING);
		return f.getAxis().isVertical() ? Direction.NORTH : f;
	}

	public WindowFrameBlock(Material material, int depth, Style style, BlockBehaviour.Properties settings) {
		super(material, settings.dynamicShape());
		this.depth = depth;
		this.style = style;
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, WATERLOGGED);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		int joins = joins(world, pos, state);
		boolean u = (joins & JOIN_UP) != 0, d = (joins & JOIN_DOWN) != 0, l = (joins & JOIN_LEFT) != 0, r = (joins & JOIN_RIGHT) != 0;
		int turns = horizontal(state).get2DDataValue() + 2 & 3;
		AttachFace face = face(state);
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

	/** Square, diamond or round (round ones are drawn in code, see RoundWindowModel). */
	public Style style() {
		return style;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		Direction side = ctx.getClickedFace();
		Direction facing = side.getAxis().isHorizontal() ? side.getOpposite() : ctx.getHorizontalDirection();
		AttachFace face = AttachFace.WALL;
		BlockState against = ctx.getLevel().getBlockState(ctx.getClickedPos().relative(side.getOpposite()));
		if (against.getBlock() instanceof WindowFrameBlock && face(against) != AttachFace.WALL && side.getAxis().isHorizontal()) {
			face = face(against);
		} else if (ctx.getPlayer() != null && ctx.getPlayer().isShiftKeyDown() && !side.getAxis().isHorizontal()) {
			face = side == Direction.UP ? AttachFace.FLOOR : AttachFace.CEILING;
		}
		// Lying frames face up (on the floor) or down (on the ceiling), so any two side by side join.
		if (face != AttachFace.WALL) facing = face == AttachFace.FLOOR ? Direction.UP : Direction.DOWN;
		BlockState state = defaultBlockState().setValue(FACING, facing).setValue(WATERLOGGED, waterAt(ctx));
		return placed(state, ctx);
	}

	@Override
	public List<BooleanProperty> joinProperties() {
		return JOINS;
	}

	/** Which neighbours it joins: any window frame (any material, depth, square or diamond) facing the same way. */
	@Override
	public int joins(BlockGetter world, BlockPos pos, BlockState state) {
		Direction f = horizontal(state);
		AttachFace face = face(state);
		// The frame's own up and down: on a wall, up and down; lying, the way its top was tipped and the opposite.
		Direction up = face == AttachFace.WALL ? Direction.UP : face == AttachFace.FLOOR ? f : f.getOpposite();
		int joins = 0;
		if (joins(state, world.getBlockState(pos.relative(up)))) joins |= JOIN_UP;
		if (joins(state, world.getBlockState(pos.relative(up.getOpposite())))) joins |= JOIN_DOWN;
		if (joins(state, world.getBlockState(pos.relative(f.getCounterClockWise())))) joins |= JOIN_LEFT;
		if (joins(state, world.getBlockState(pos.relative(f.getClockWise())))) joins |= JOIN_RIGHT;
		return joins;
	}

	private boolean joins(BlockState state, BlockState other) {
		return other.getBlock() instanceof WindowFrameBlock && other.getValue(FACING) == state.getValue(FACING);
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.getValue(FACING).getAxis().isVertical() ? state : state.rotate(mirror.getRotation(state.getValue(FACING)));
	}
}
