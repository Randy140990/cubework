package com.insidethecube.cubework.block;

import com.insidethecube.cubework.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Stair Curve: goes under an ordinary flight of stairs (any stairs, any material) to give it a smooth curved
 * underside, flat along the floor at the bottom stair and sweeping up to the top one (see
 * StairCurveGeometry). Fill the blocks under the flight with it: each one finds the stairs above it, works
 * out how long the flight is and where it sits in it, and takes its own slice of the curve. It looks again
 * when the blocks round it change, or when you use it with an empty hand.
 */
public class StairCurveBlock extends CwBlock implements SimpleWaterloggedBlock {
	/** The way the flight climbs (the stairs' facing). */
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final IntegerProperty PIECE = IntegerProperty.create("piece", 0, StairCurveGeometry.PIECES.length - 1);

	/** A piece in the middle of a 4-stair flight, for when there are no stairs above to go by. */
	private static final int DEFAULT_PIECE = StairCurveGeometry.index(4, 2, 0);

	private static final VoxelShape[] SHAPES = new VoxelShape[StairCurveGeometry.PIECES.length * 4];
	private static final VoxelShape[] SOLID = new VoxelShape[StairCurveGeometry.PIECES.length * 4];

	public StairCurveBlock(Material material, BlockBehaviour.Properties settings) {
		super(material, settings);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.EAST).setValue(PIECE, DEFAULT_PIECE).setValue(WATERLOGGED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, PIECE, WATERLOGGED);
	}

	private static int key(BlockState state) {
		return state.getValue(PIECE) * 4 + ArchCornerBlock.turns(state.getValue(FACING));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		int k = key(state);
		VoxelShape s = SHAPES[k];
		if (s == null) SHAPES[k] = s = Profiles.shape(StairCurveGeometry.mask(state.getValue(PIECE)), 0, 16, k & 3);
		return s;
	}

	/** The part that's solid all over, for hiding neighbours' faces (the model's curve is smooth). */
	@Override
	//#if MC >= 1.21.2
	protected VoxelShape getOcclusionShape(BlockState state) {
	//#else
	protected VoxelShape getOcclusionShape(BlockState state, BlockGetter world, BlockPos pos) {
	//#endif
		int k = key(state);
		VoxelShape s = SOLID[k];
		if (s == null) SOLID[k] = s = Profiles.shape(StairCurveGeometry.inside(state.getValue(PIECE)), 0, 16, k & 3);
		return s;
	}

	/**
	 * In the hollow under the curve nothing is placed (the item stays in your hand), so you can fill
	 * every block under a flight and the hollow keeps its shape.
	 */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		BlockState state = defaultBlockState().setValue(FACING, ctx.getHorizontalDirection()).setValue(WATERLOGGED, waterAt(ctx));
		BlockState fit = fitted(state, ctx.getLevel(), ctx.getClickedPos());
		if (fit != state && StairCurveGeometry.hollow(fit.getValue(PIECE))) return null;
		return placed(fit, ctx);
	}

	@Override
	protected BlockState shapeUpdate(BlockState state, LevelReader world, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState) {
		return fitted(state, world, pos);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
		// With an empty hand: look at the stairs above again (after building or changing the flight).
		if (!player.getMainHandItem().isEmpty()) return InteractionResult.PASS;
		BlockState next = fitted(state, world, pos);
		if (next != state) world.setBlock(pos, next, Block.UPDATE_ALL);
		world.playSound(player, pos, getSoundType(state).getPlaceSound(), SoundSource.BLOCKS, 0.6f, 1.2f);
		return InteractionResult.SUCCESS;
	}

	private static boolean isStair(BlockState s) {
		return s.getBlock() instanceof StairBlock && s.getValue(StairBlock.HALF) == Half.BOTTOM;
	}

	private static boolean sameFlight(BlockState s, Direction f) {
		return isStair(s) && s.getValue(StairBlock.FACING) == f;
	}

	/**
	 * The piece for this spot: the first stairs straight above (within six blocks, through anything
	 * else in between) belong to its column; following the flight down and up from there gives the
	 * flight's length, which stair this column is, and so how high above the floor this block sits.
	 * With no stairs above, it keeps what it has.
	 */
	static BlockState fitted(BlockState state, BlockGetter world, BlockPos pos) {
		BlockPos stair = null;
		BlockState st = null;
		for (int k = 1; k <= StairCurveGeometry.MAX; k++) {
			BlockState s = world.getBlockState(pos.above(k));
			if (isStair(s)) {
				stair = pos.above(k);
				st = s;
				break;
			}
		}
		if (stair == null) return state;
		Direction f = st.getValue(StairBlock.FACING);
		int below = 0;
		BlockPos q = stair;
		while (below < 32 && sameFlight(world.getBlockState(q.relative(f.getOpposite()).below()), f)) {
			q = q.relative(f.getOpposite()).below();
			below++;
		}
		int bottomY = q.getY();
		int above = 0;
		q = stair;
		while (above < 32 && sameFlight(world.getBlockState(q.relative(f).above()), f)) {
			q = q.relative(f).above();
			above++;
		}
		int w = below + above + 1, c = below, r = pos.getY() - bottomY;
		// Long flights curve under their top six stairs; lower down they're solid.
		if (w > StairCurveGeometry.MAX) {
			c -= w - StairCurveGeometry.MAX;
			r -= w - StairCurveGeometry.MAX;
			w = StairCurveGeometry.MAX;
			if (c < 1 || r < 0) return state.setValue(FACING, f).setValue(PIECE, StairCurveGeometry.index(StairCurveGeometry.MAX, 2, 1));
		}
		int piece = c >= 1 && r >= 0 && r < c ? StairCurveGeometry.index(w, c, r) : -1;
		if (piece < 0) return state.setValue(FACING, f);
		return state.setValue(FACING, f).setValue(PIECE, piece);
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
