package com.insidethecube.cubework.block;

import java.util.Locale;

import com.insidethecube.cubework.Material;
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
import net.minecraft.world.level.block.Mirror;
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
 */
public class PartitionBlock extends CwBlock {
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

	public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
	public static final BooleanProperty EAST = BlockStateProperties.EAST;
	public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
	public static final BooleanProperty WEST = BlockStateProperties.WEST;
	public static final EnumProperty<Height> HEIGHT = EnumProperty.create("height", Height.class);
	public static final EnumProperty<Cap> CAP = EnumProperty.create("cap", Cap.class);

	public final int thickness;
	private final java.util.Map<Integer, VoxelShape> shapes = new java.util.concurrent.ConcurrentHashMap<>();

	public PartitionBlock(Material material, int thickness, BlockBehaviour.Properties settings) {
		super(material, settings);
		this.thickness = thickness;
		registerDefaultState(defaultBlockState().setValue(NORTH, false).setValue(EAST, true).setValue(SOUTH, false).setValue(WEST, true)
				.setValue(HEIGHT, Height.FULL).setValue(CAP, Cap.NONE));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(NORTH, EAST, SOUTH, WEST, HEIGHT, CAP);
	}

	private static BooleanProperty side(Direction d) {
		return switch (d) {
			case NORTH -> NORTH;
			case EAST -> EAST;
			case SOUTH -> SOUTH;
			default -> WEST;
		};
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		int key = (state.getValue(NORTH) ? 1 : 0) | (state.getValue(EAST) ? 2 : 0) | (state.getValue(SOUTH) ? 4 : 0) | (state.getValue(WEST) ? 8 : 0)
				| state.getValue(HEIGHT).ordinal() << 4 | (state.getValue(CAP) == Cap.NONE ? 0 : 64);
		return shapes.computeIfAbsent(key, k -> build(state));
	}

	private VoxelShape build(BlockState state) {
		double h = state.getValue(HEIGHT).pixels / 16.0, a = (8 - thickness / 2.0) / 16, b = (8 + thickness / 2.0) / 16;
		boolean cap = state.getValue(CAP) != Cap.NONE;
		double body = cap ? h - 1 / 16.0 : h, c = 1 / 16.0;
		VoxelShape s = Shapes.box(a, 0, a, b, body, b);
		if (cap) s = Shapes.or(s, Shapes.box(a - c, body, a - c, b + c, h, b + c));
		for (Direction d : Direction.Plane.HORIZONTAL) {
			if (!state.getValue(side(d))) continue;
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

	/**
	 * Its arms: towards everything it joins; a run's end reaches across its block (the arm opposite too);
	 * with nothing to join it keeps the way it ran, or runs across the way the placer faced.
	 */
	private BlockState joined(BlockState state, BlockGetter world, BlockPos pos, Direction facing) {
		boolean[] arm = new boolean[4];
		int count = 0;
		Direction[] dirs = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
		for (int i = 0; i < 4; i++) {
			arm[i] = joins(world, pos, dirs[i]);
			if (arm[i]) count++;
		}
		if (count == 0) {
			boolean alongX = facing != null ? facing.getAxis() == Direction.Axis.Z : !(state.getValue(NORTH) && state.getValue(SOUTH));
			arm[1] = arm[3] = alongX;
			arm[0] = arm[2] = !alongX;
		} else if (count == 1) {
			for (int i = 0; i < 4; i++) if (arm[i]) arm[(i + 2) % 4] = true;
		}
		for (int i = 0; i < 4; i++) state = state.setValue(side(dirs[i]), arm[i]);
		return state;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		return placed(joined(defaultBlockState(), ctx.getLevel(), ctx.getClickedPos(), ctx.getHorizontalDirection()), ctx);
	}

	@Override
	protected BlockState shapeUpdate(BlockState state, LevelReader world, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState) {
		return direction.getAxis().isHorizontal() ? joined(state, world, pos, null) : state;
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
		BlockState out = state;
		for (Direction d : Direction.Plane.HORIZONTAL) out = out.setValue(side(rotation.rotate(d)), state.getValue(side(d)));
		return out;
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		BlockState out = state;
		for (Direction d : Direction.Plane.HORIZONTAL) out = out.setValue(side(mirror.mirror(d)), state.getValue(side(d)));
		return out;
	}
}
