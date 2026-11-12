package com.insidethecube.deepkit.paint;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A painted mark on a block's face, one colour a block. FACING points at the block it's painted on;
 * ROTATION turns it (which way an arrow points); SYMBOL is which mark (see Marks). It glows faintly,
 * goes when its block does, and comes off with a sponge or by breaking it (it drops nothing).
 */
public class GlowMarkBlock extends Block implements SimpleWaterloggedBlock {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
	public static final IntegerProperty ROTATION = IntegerProperty.create("rotation", 0, 3);
	public static final IntegerProperty SYMBOL = IntegerProperty.create("symbol", 0, Marks.NAMES.length - 1);
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

	private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

	static {
		SHAPES.put(Direction.NORTH, Block.box(0, 0, 0, 16, 16, 0.5));
		SHAPES.put(Direction.SOUTH, Block.box(0, 0, 15.5, 16, 16, 16));
		SHAPES.put(Direction.WEST, Block.box(0, 0, 0, 0.5, 16, 16));
		SHAPES.put(Direction.EAST, Block.box(15.5, 0, 0, 16, 16, 16));
		SHAPES.put(Direction.DOWN, Block.box(0, 0, 0, 16, 0.5, 16));
		SHAPES.put(Direction.UP, Block.box(0, 15.5, 0, 16, 16, 16));
	}

	public final PaintColour colour;

	public GlowMarkBlock(PaintColour colour, BlockBehaviour.Properties props) {
		super(props);
		this.colour = colour;
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(ROTATION, 0).setValue(SYMBOL, 0).setValue(WATERLOGGED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, ROTATION, SYMBOL, WATERLOGGED);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return SHAPES.get(state.getValue(FACING));
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	/** Whether a mark can go on the given face of the block at pos (a solid face, not glass or leaves). */
	public static boolean paintable(LevelReader level, BlockPos pos, Direction face) {
		BlockState s = level.getBlockState(pos);
		if (s.getBlock() instanceof net.minecraft.world.level.block.TransparentBlock || s.is(net.minecraft.tags.BlockTags.LEAVES)) return false;
		return s.isFaceSturdy(level, pos, face);
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		Direction to = state.getValue(FACING);
		return paintable(level, pos.relative(to), to.getOpposite());
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction dir, BlockPos nPos,
			BlockState nState, RandomSource random) {
		if (state.getValue(WATERLOGGED)) ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		if (dir == state.getValue(FACING) && !canSurvive(state, level, pos)) {
			return state.getValue(WATERLOGGED) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
		}
		return state;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		if (!stack.is(Items.SPONGE) && !stack.is(Items.WET_SPONGE)) return InteractionResult.TRY_WITH_EMPTY_HAND;
		if (level instanceof ServerLevel) {
			level.setBlock(pos, state.getValue(WATERLOGGED) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
			level.playSound(null, pos, SoundEvents.SPONGE_ABSORB, SoundSource.BLOCKS, 0.6f, 1.4f);
		}
		return InteractionResult.SUCCESS;
	}
}
