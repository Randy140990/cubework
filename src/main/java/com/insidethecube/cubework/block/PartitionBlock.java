package com.insidethecube.cubework.block;

import java.util.List;

import java.util.Locale;

import com.insidethecube.cubework.Behaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A thin partition wall, 2, 4, 6 or 8 pixels thick, standing in the middle of the block and joining other
 * partitions (any material or thickness) and solid faces, like glass panes, into runs, corners and T's. An
 * empty hand changes its height (full, three quarters, half); a wooden slab puts a 1-pixel cap of that wood
 * along its top, a pixel wider each side; sneaking with an empty hand takes the cap off.
 * <p>
 * Its arms aren't kept in the block state (see Joining): they're worked out from the neighbours. The state
 * keeps only the way a lone partition runs (AXIS, which follows the last straight run it was part of), its
 * height and its cap.
 */
public class PartitionBlock extends CwBlock implements Joining {
	public enum Height implements StringRepresentable {
		FULL(16), THREE_QUARTERS(12), HALF(8);

		public final int pixels;

		Height(int pixels) {
			this.pixels = pixels;
		}

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	/** The cap's wood, or none. */
	public enum Cap implements StringRepresentable {
		NONE, OAK, SPRUCE, BIRCH, JUNGLE, ACACIA, DARK_OAK, MANGROVE, CHERRY, PALE_OAK, BAMBOO, CRIMSON, WARPED;

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	/** The way it runs when nothing is there to join. */
	public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
	/** The arms its model is written with (blockstate under joined/): north, east, south, west (bits 1, 2, 4, 8). */
	public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
	public static final BooleanProperty EAST = BlockStateProperties.EAST;
	public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
	public static final BooleanProperty WEST = BlockStateProperties.WEST;
	public static final EnumProperty<Height> HEIGHT = EnumProperty.create("height", Height.class);
	public static final EnumProperty<Cap> CAP = EnumProperty.create("cap", Cap.class);

	private static final List<BooleanProperty> JOINS = List.of(NORTH, EAST, SOUTH, WEST);
	private static final Direction[] DIRS = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
	private static final int ALONG_X = 2 | 8, ALONG_Z = 1 | 4;

	public final int thickness;
	private final java.util.Map<Integer, VoxelShape> shapes = new java.util.concurrent.ConcurrentHashMap<>();

	public PartitionBlock(Behaviour behaviour, int thickness, BlockBehaviour.Properties settings) {
		super(behaviour, settings.dynamicShape());
		this.thickness = thickness;
		registerDefaultState(defaultBlockState().setValue(AXIS, Direction.Axis.X).setValue(HEIGHT, Height.FULL).setValue(CAP, Cap.NONE));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AXIS, HEIGHT, CAP);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		int arms = joins(world, pos, state);
		int key = arms | state.getValue(HEIGHT).ordinal() << 4 | (state.getValue(CAP) == Cap.NONE ? 0 : 64);
		return shapes.computeIfAbsent(key, k -> build(state, arms));
	}

	private VoxelShape build(BlockState state, int arms) {
		double h = state.getValue(HEIGHT).pixels / 16.0, a = (8 - thickness / 2.0) / 16, b = (8 + thickness / 2.0) / 16;
		boolean cap = state.getValue(CAP) != Cap.NONE;
		double body = cap ? h - 1 / 16.0 : h, c = 1 / 16.0;
		VoxelShape s = Shapes.box(a, 0, a, b, body, b);
		if (cap) s = Shapes.or(s, Shapes.box(a - c, body, a - c, b + c, h, b + c));
		for (int i = 0; i < 4; i++) {
			if ((arms & 1 << i) == 0) continue;
			Direction d = DIRS[i];
			double x0 = d == Direction.WEST ? 0 : d == Direction.EAST ? b : a, x1 = d == Direction.WEST ? a : d == Direction.EAST ? 1 : b;
			double z0 = d == Direction.NORTH ? 0 : d == Direction.SOUTH ? b : a, z1 = d == Direction.NORTH ? a : d == Direction.SOUTH ? 1 : b;
			s = Shapes.or(s, Shapes.box(x0, 0, z0, x1, body, z1));
			if (cap) {
				double cx0 = d.getAxis() == Direction.Axis.X ? x0 : a - c, cx1 = d.getAxis() == Direction.Axis.X ? x1 : b + c;
				double cz0 = d.getAxis() == Direction.Axis.Z ? z0 : a - c, cz1 = d.getAxis() == Direction.Axis.Z ? z1 : b + c;
				s = Shapes.or(s, Shapes.box(cx0, body, cz0, cx1, h, cz1));
			}
		}
		return s.optimize();
	}

	/** Whether it joins what's on side d: another partition, or a solid face. */
	private static boolean joins(BlockGetter world, BlockPos pos, Direction d) {
		BlockPos p = pos.relative(d);
		BlockState s = world.getBlockState(p);
		return s.getBlock() instanceof PartitionBlock || s.isFaceSturdy(world, p, d.getOpposite());
	}

	@Override
	public List<BooleanProperty> joinProperties() {
		return JOINS;
	}

	/**
	 * Its arms: towards everything it joins; a run's end reaches across its block (the arm opposite too);
	 * with nothing to join it runs the way its AXIS says.
	 */
	@Override
	public int joins(BlockGetter world, BlockPos pos, BlockState state) {
		int arms = 0, count = 0;
		for (int i = 0; i < 4; i++) {
			if (joins(world, pos, DIRS[i])) {
				arms |= 1 << i;
				count++;
			}
		}
		if (count == 0) return lone(state);
		if (count == 1) arms |= (arms << 2 | arms >> 2) & 15;
		return arms;
	}

	@Override
	public int lone(BlockState state) {
		return state.getValue(AXIS) == Direction.Axis.X ? ALONG_X : ALONG_Z;
	}

	/** Its AXIS follows a straight run it's part of, so it keeps running that way if the rest goes. */
	private BlockState followRun(BlockState state, BlockGetter world, BlockPos pos) {
		int arms = joins(world, pos, state);
		if (arms == ALONG_X) return state.setValue(AXIS, Direction.Axis.X);
		if (arms == ALONG_Z) return state.setValue(AXIS, Direction.Axis.Z);
		return state;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		// Alone, it runs across the way the placer faces.
		Direction.Axis axis = ctx.getHorizontalDirection().getAxis() == Direction.Axis.Z ? Direction.Axis.X : Direction.Axis.Z;
		return placed(followRun(defaultBlockState().setValue(AXIS, axis), ctx.getLevel(), ctx.getClickedPos()), ctx);
	}

	@Override
	protected BlockState shapeUpdate(BlockState state, LevelReader world, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState) {
		return direction.getAxis().isHorizontal() ? followRun(state, world, pos) : state;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
		if (!player.getMainHandItem().isEmpty()) return InteractionResult.PASS;
		BlockState next;
		if (player.isShiftKeyDown()) {
			if (state.getValue(CAP) == Cap.NONE) return InteractionResult.PASS;
			next = state.setValue(CAP, Cap.NONE);
		} else {
			Height[] all = Height.values();
			next = state.setValue(HEIGHT, all[(state.getValue(HEIGHT).ordinal() + 1) % all.length]);
		}
		world.setBlock(pos, next, Block.UPDATE_ALL);
		world.playSound(player, pos, getSoundType(state).getPlaceSound(), SoundSource.BLOCKS, 0.6f, 1.2f);
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		Cap cap = capOf(stack);
		if (cap == null) return super.useItemOn(stack, state, world, pos, player, hand, hit);
		if (state.getValue(CAP) == cap) return InteractionResult.PASS;
		world.setBlock(pos, state.setValue(CAP, cap), Block.UPDATE_ALL);
		world.playSound(player, pos, getSoundType(state).getPlaceSound(), SoundSource.BLOCKS, 0.6f, 1.2f);
		if (!player.getAbilities().instabuild) stack.shrink(1);
		return InteractionResult.SUCCESS;
	}

	/** The cap a wooden slab gives (oak_slab: oak), or null. */
	private static Cap capOf(ItemStack stack) {
		String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
		if (!path.endsWith("_slab")) return null;
		String wood = path.substring(0, path.length() - 5);
		for (Cap c : Cap.values()) if (c != Cap.NONE && c.getSerializedName().equals(wood)) return c;
		return null;
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		boolean turns = rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90;
		return turns ? state.setValue(AXIS, state.getValue(AXIS) == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X) : state;
	}
}
