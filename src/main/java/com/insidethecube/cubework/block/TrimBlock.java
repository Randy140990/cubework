package com.insidethecube.cubework.block;

import java.util.Locale;

import com.insidethecube.cubework.Behaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Trim: a square strip 2, 4 or 8 pixels thick running the length of one edge of the block, for skirting,
 * cornices, sills, edging and the corners of walls. It lies against the face you click, along that face's
 * edge nearest to where you clicked (so click a wall near the top for a cornice, near a side for an upright).
 */
public class TrimBlock extends CwBlock implements SimpleWaterloggedBlock {
	/** The twelve edges of a block, each named by the two sides it's between. */
	public enum Edge implements StringRepresentable {
		DOWN_NORTH(Direction.DOWN, Direction.NORTH), DOWN_EAST(Direction.DOWN, Direction.EAST), DOWN_SOUTH(Direction.DOWN, Direction.SOUTH),
		DOWN_WEST(Direction.DOWN, Direction.WEST), UP_NORTH(Direction.UP, Direction.NORTH), UP_EAST(Direction.UP, Direction.EAST),
		UP_SOUTH(Direction.UP, Direction.SOUTH), UP_WEST(Direction.UP, Direction.WEST), NORTH_EAST(Direction.NORTH, Direction.EAST),
		SOUTH_EAST(Direction.SOUTH, Direction.EAST), SOUTH_WEST(Direction.SOUTH, Direction.WEST), NORTH_WEST(Direction.NORTH, Direction.WEST);

		public final Direction a, b;

		Edge(Direction a, Direction b) {
			this.a = a;
			this.b = b;
		}

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}

		/** The edge between two sides at right angles. */
		public static Edge of(Direction x, Direction y) {
			for (Edge e : values()) if ((e.a == x && e.b == y) || (e.a == y && e.b == x)) return e;
			return DOWN_NORTH;
		}

		Edge map(java.util.function.UnaryOperator<Direction> f) {
			return of(f.apply(a), f.apply(b));
		}
	}

	public static final EnumProperty<Edge> EDGE = EnumProperty.create("edge", Edge.class);

	private final int width;
	private final VoxelShape[] shapes = new VoxelShape[Edge.values().length];

	public TrimBlock(Behaviour behaviour, int width, BlockBehaviour.Properties settings) {
		super(behaviour, settings);
		this.width = width;
		registerDefaultState(defaultBlockState().setValue(EDGE, Edge.DOWN_NORTH).setValue(WATERLOGGED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(EDGE, WATERLOGGED);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		Edge e = state.getValue(EDGE);
		VoxelShape s = shapes[e.ordinal()];
		if (s == null) {
			double[] lo = {0, 0, 0}, hi = {16, 16, 16};
			for (Direction d : new Direction[] {e.a, e.b}) {
				int i = d.getAxis().ordinal();
				if (d.getAxisDirection() == Direction.AxisDirection.POSITIVE) lo[i] = 16 - width;
				else hi[i] = width;
			}
			shapes[e.ordinal()] = s = Block.box(lo[0], lo[1], lo[2], hi[0], hi[1], hi[2]);
		}
		return s;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		BlockPos pos = ctx.getClickedPos();
		// Against the block clicked (or the floor when placed into a replaceable block from above).
		Direction against = ctx.getClickedFace().getOpposite();
		Vec3 hit = ctx.getClickLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
		Direction best = null;
		double nearest = Double.MAX_VALUE;
		for (Direction d : Direction.values()) {
			if (d.getAxis() == against.getAxis()) continue;
			double c = Math.max(0, Math.min(1, d.getAxis().choose(hit.x, hit.y, hit.z)));
			double away = d.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 - c : c;
			if (away < nearest) {
				nearest = away;
				best = d;
			}
		}
		BlockState state = defaultBlockState().setValue(EDGE, Edge.of(against, best)).setValue(WATERLOGGED, waterAt(ctx));
		return placed(state, ctx);
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(EDGE, state.getValue(EDGE).map(rotation::rotate));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.setValue(EDGE, state.getValue(EDGE).map(mirror::mirror));
	}
}
