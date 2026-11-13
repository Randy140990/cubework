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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Stairs that climb one block over four (Over3: three, Over2: two). Each block has two steps half a block
 * deep, like vanilla's, but they only rise a quarter (a third, a half) of a block between them, standing on
 * the steps below. So 1 in 4 stairs, then 1 in 3, 1 in 2 and ordinary stairs make a flight that curves up
 * from the floor (and back the other way to curve out onto a landing). Placed in front of one of the same kind
 * facing the same way, it takes the next step by itself; used with an empty hand, it moves to the next step.
 * Like stairs, it climbs away from you, and goes upside down on a ceiling or the top half of a face.
 */
public class PitchStairsBlock extends CwBlock implements SimpleWaterloggedBlock {
	/** The high side. */
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final EnumProperty<Half> HALF = BlockStateProperties.HALF;
	public static final IntegerProperty STEP = IntegerProperty.create("step", 1, 4);

	private final VoxelShape[] shapes = new VoxelShape[4 * 4 * 2];

	public PitchStairsBlock(Material material, BlockBehaviour.Properties settings) {
		super(material, settings);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(HALF, Half.BOTTOM).setValue(step(), 1)
				.setValue(WATERLOGGED, false));
	}

	/** How many blocks it takes to climb one. */
	public int steps() {
		return 4;
	}

	/** Its step property (1 to steps()). Called while the block is being built, so it returns a constant. */
	public IntegerProperty step() {
		return STEP;
	}

	/** One block over three. */
	public static class Over3 extends PitchStairsBlock {
		public static final IntegerProperty STEP_3 = IntegerProperty.create("step", 1, 3);

		public Over3(Material material, BlockBehaviour.Properties settings) {
			super(material, settings);
		}

		@Override
		public int steps() {
			return 3;
		}

		@Override
		public IntegerProperty step() {
			return STEP_3;
		}
	}

	/** One block over two. */
	public static class Over2 extends PitchStairsBlock {
		public static final IntegerProperty STEP_2 = IntegerProperty.create("step", 1, 2);

		public Over2(Material material, BlockBehaviour.Properties settings) {
			super(material, settings);
		}

		@Override
		public int steps() {
			return 2;
		}

		@Override
		public IntegerProperty step() {
			return STEP_2;
		}
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, HALF, step(), WATERLOGGED);
	}

	/** Height of the low step (the front half) and the high step (the back half), in pixels. */
	public static double[] heights(int steps, int step) {
		double base = (step - 1) * 16.0 / steps;
		return new double[] {base + 8.0 / steps, base + 16.0 / steps};
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		Direction f = state.getValue(FACING);
		boolean top = state.getValue(HALF) == Half.TOP;
		int step = state.getValue(step());
		int i = ((step - 1) * 4 + f.get2DDataValue()) * 2 + (top ? 1 : 0);
		VoxelShape s = shapes[i];
		if (s == null) {
			double[] h = heights(steps(), step);
			s = Shapes.or(half(f.getOpposite(), h[0], top), half(f, h[1], top));
			shapes[i] = s = s.optimize();
		}
		return s;
	}

	/** The half of the block on side d, h pixels tall (hanging from the top when upside down). */
	private static VoxelShape half(Direction d, double h, boolean top) {
		double y0 = top ? 16 - h : 0, y1 = top ? 16 : h;
		return switch (d) {
			case NORTH -> Block.box(0, y0, 0, 16, y1, 8);
			case SOUTH -> Block.box(0, y0, 8, 16, y1, 16);
			case WEST -> Block.box(0, y0, 0, 8, y1, 16);
			default -> Block.box(8, y0, 0, 16, y1, 16);
		};
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		Direction side = ctx.getClickedFace();
		BlockPos pos = ctx.getClickedPos();
		boolean bottom = side != Direction.DOWN && (side == Direction.UP || !(ctx.getClickLocation().y - pos.getY() > 0.5));
		BlockState state = defaultBlockState().setValue(FACING, ctx.getHorizontalDirection()).setValue(HALF, bottom ? Half.BOTTOM : Half.TOP)
				.setValue(WATERLOGGED, waterAt(ctx));
		return placed(state.setValue(step(), stepAt(state, ctx.getLevel(), pos)), ctx);
	}

	/** The step after the one below it (downhill), or before the one above it (uphill); else the first. */
	private int stepAt(BlockState state, BlockGetter world, BlockPos pos) {
		Direction facing = state.getValue(FACING);
		int n = steps();
		BlockState below = world.getBlockState(pos.relative(facing.getOpposite()));
		if (sameKind(below) && below.getValue(FACING) == facing && below.getValue(HALF) == state.getValue(HALF)) {
			return below.getValue(step()) % n + 1;
		}
		BlockState above = world.getBlockState(pos.relative(facing));
		if (sameKind(above) && above.getValue(FACING) == facing && above.getValue(HALF) == state.getValue(HALF)) {
			return above.getValue(step()) == 1 ? n : above.getValue(step()) - 1;
		}
		return 1;
	}

	/** Stairs with the same number of steps (any material). */
	private boolean sameKind(BlockState other) {
		return other.getBlock() instanceof PitchStairsBlock p && p.steps() == steps();
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
		// Only with an empty hand, so clicking one with a block still places the block.
		if (!player.getMainHandItem().isEmpty()) return InteractionResult.PASS;
		world.setBlock(pos, state.setValue(step(), state.getValue(step()) % steps() + 1), Block.UPDATE_ALL);
		world.playSound(player, pos, getSoundType(state).getPlaceSound(), SoundSource.BLOCKS, 0.6f, 1.2f);
		return InteractionResult.SUCCESS;
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
