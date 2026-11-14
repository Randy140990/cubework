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
 * Curved Stairs: a flight of stairs with a smooth S-shaped underside (see StairCurveGeometry). Lay the
 * flight with it like ordinary stairs (it climbs away from you), then fill the blocks under the flight with
 * the same item: a block with the flight right above it becomes the fill under its stair. Every block
 * works out how long the flight is and where it sits in it, and takes its own part of the curve; the
 * whole flight re-fits whenever a piece is placed, and any piece used with an empty hand re-fits too.
 * Fill that would sit in the hollow under the curve isn't placed, so you can fill the whole space under
 * the flight. Ordinary stairs on top work as well (the fill still curves; the steps stay square).
 */
public class StairCurveBlock extends CwBlock implements SimpleWaterloggedBlock {
	/** The way the flight climbs. */
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final IntegerProperty PIECE = IntegerProperty.create("piece", 0, StairCurveGeometry.PIECES.length - 1);

	private static final VoxelShape[] SHAPES = new VoxelShape[StairCurveGeometry.PIECES.length * 4];
	private static final VoxelShape[] SOLID = new VoxelShape[StairCurveGeometry.PIECES.length * 4];

	public StairCurveBlock(Material material, BlockBehaviour.Properties settings) {
		super(material, settings);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.EAST).setValue(PIECE, StairCurveGeometry.PLAIN).setValue(WATERLOGGED, false));
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

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		BlockState state = defaultBlockState().setValue(FACING, ctx.getHorizontalDirection()).setValue(WATERLOGGED, waterAt(ctx));
		BlockState fit = fitted(state, ctx.getLevel(), ctx.getClickedPos());
		if (StairCurveGeometry.hollow(fit.getValue(PIECE))) return null;
		return placed(fit, ctx);
	}

	/** A newly placed piece re-fits the whole flight (a new stair makes it longer, so every piece changes). */
	@Override
	protected void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, world, pos, oldState, movedByPiston);
		if (oldState.getBlock() != this) refit(world, pos);
	}

	@Override
	protected BlockState shapeUpdate(BlockState state, LevelReader world, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState) {
		return fitted(state, world, pos);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
		// With an empty hand: fit the whole flight again (after changing it).
		if (!player.getMainHandItem().isEmpty()) return InteractionResult.PASS;
		refit(world, pos);
		world.playSound(player, pos, getSoundType(state).getPlaceSound(), SoundSource.BLOCKS, 0.6f, 1.2f);
		return InteractionResult.SUCCESS;
	}

	/** Part of a flight climbing towards f: Curved Stairs facing f, or ordinary stairs (right way up) facing f. */
	private static boolean member(BlockState s, Direction f) {
		if (s.getBlock() instanceof StairCurveBlock) return s.getValue(FACING) == f;
		return s.getBlock() instanceof StairBlock && s.getValue(StairBlock.HALF) == Half.BOTTOM && s.getValue(StairBlock.FACING) == f;
	}

	private static Direction facingOf(BlockState s) {
		if (s.getBlock() instanceof StairCurveBlock) return s.getValue(FACING);
		if (s.getBlock() instanceof StairBlock && s.getValue(StairBlock.HALF) == Half.BOTTOM) return s.getValue(StairBlock.FACING);
		return null;
	}

	/** Where a flight is: its bottom stair, the way it climbs, and how many stairs it has. */
	private record Flight(BlockPos bottom, Direction facing, int length) {}

	/**
	 * The flight this spot belongs to. With part of a flight right above, it's fill under that flight's
	 * stair (the top of the column above); otherwise it's a stair itself, climbing the way it faces.
	 */
	private static Flight flightAt(BlockState state, BlockGetter world, BlockPos pos) {
		Direction f = facingOf(world.getBlockState(pos.above()));
		BlockPos stair = pos;
		if (f != null) {
			stair = pos.above();
			for (int k = 0; k < 8 && member(world.getBlockState(stair.above()), f); k++) stair = stair.above();
		} else {
			f = state.getValue(FACING);
		}
		int below = 0;
		BlockPos q = stair;
		while (below < 64 && member(world.getBlockState(q.relative(f.getOpposite()).below()), f)) {
			q = q.relative(f.getOpposite()).below();
			below++;
		}
		BlockPos bottom = q;
		int above = 0;
		q = stair;
		while (above < 64 && member(world.getBlockState(q.relative(f).above()), f)) {
			q = q.relative(f).above();
			above++;
		}
		return new Flight(bottom, f, below + above + 1);
	}

	/** The piece for this spot. */
	static BlockState fitted(BlockState state, BlockGetter world, BlockPos pos) {
		Flight fl = flightAt(state, world, pos);
		Direction f = fl.facing();
		// Column: how far along the flight; row: how high above its floor.
		BlockPos d = pos.subtract(fl.bottom());
		int c = d.getX() * f.getStepX() + d.getZ() * f.getStepZ(), r = d.getY();
		int w = fl.length();
		boolean stair = r == c;
		if (w > StairCurveGeometry.MAX) {
			int shift = w - StairCurveGeometry.MAX;
			c -= shift;
			r -= shift;
			w = StairCurveGeometry.MAX;
		}
		int piece;
		if (c < 0 || r < 0 || c >= w || r > c) piece = stair ? StairCurveGeometry.PLAIN : StairCurveGeometry.SOLID;
		else piece = StairCurveGeometry.index(w, c, r);
		if (piece < 0) piece = StairCurveGeometry.PLAIN;
		return state.setValue(FACING, f).setValue(PIECE, piece);
	}

	/** Fits every Curved Stairs block of the flight at pos again. */
	private static void refit(Level world, BlockPos pos) {
		BlockState here = world.getBlockState(pos);
		if (!(here.getBlock() instanceof StairCurveBlock)) return;
		Flight fl = flightAt(here, world, pos);
		Direction f = fl.facing();
		for (int i = 0; i < fl.length(); i++) {
			BlockPos stair = fl.bottom().relative(f, i).above(i);
			for (int r = 0; r <= i; r++) {
				BlockPos p = stair.below(i - r);
				BlockState s = world.getBlockState(p);
				if (!(s.getBlock() instanceof StairCurveBlock)) continue;
				BlockState next = fitted(s, world, p);
				if (next != s) world.setBlock(p, next, Block.UPDATE_ALL);
			}
		}
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
