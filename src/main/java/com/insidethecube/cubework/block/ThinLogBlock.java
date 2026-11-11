package com.insidethecube.cubework.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
//#if MC >= 1.21.2
import net.minecraft.world.level.ScheduledTickAccess;
//#else
import net.minecraft.world.level.LevelAccessor;
//#endif
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * A log or stem, full length but narrower (16, 12, 8 or 4 pixels), in the middle of the block, with the
 * bark and the ring shrunk to fit. An end with another log against it shows bark instead of the ring, so
 * thinner logs stacked on thicker ones look like one trunk tapering (and branches grow out of bark).
 */
public class ThinLogBlock extends ThinPillarBlock {
	/** The model's top and bottom ends, which turn with the axis: up and down, north and south, east and west. */
	public static final BooleanProperty BARK_TOP = BooleanProperty.create("bark_top");
	public static final BooleanProperty BARK_BOTTOM = BooleanProperty.create("bark_bottom");

	public ThinLogBlock(int width, BlockBehaviour.Properties settings) {
		super(null, width, settings);
		registerDefaultState(defaultBlockState().setValue(BARK_TOP, false).setValue(BARK_BOTTOM, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(BARK_TOP, BARK_BOTTOM);
	}

	/** Where the model's top end faces, for each axis (as the blockstate turns it). */
	private static Direction top(Direction.Axis axis) {
		return switch (axis) {
			case Y -> Direction.UP;
			case Z -> Direction.NORTH;
			case X -> Direction.EAST;
		};
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		BlockState state = super.getStateForPlacement(ctx);
		return state == null ? null : ends(state, ctx.getLevel(), ctx.getClickedPos());
	}

	@Override
	//#if MC >= 1.21.2
	protected BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickView, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighborState, RandomSource random) {
	//#else
	protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
		LevelAccessor tickView = world;
		RandomSource random = world.getRandom();
	//#endif
		//#if MC >= 1.21.2
		BlockState next = super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random);
		//#else
		BlockState next = super.updateShape(state, direction, neighborState, world, pos, neighborPos);
		//#endif
		return next.is(this) && direction.getAxis() == next.getValue(AXIS) ? ends(next, world, pos) : next;
	}

	//#if MC >= 26.3
	/**
	 * From 26.3 the game strips logs by data that a mod can't add to, so thin logs strip themselves when an
	 * axe is used on them, as logs do.
	 */
	@Override
	protected net.minecraft.world.InteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState state, net.minecraft.world.level.Level level,
			BlockPos pos, net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
		Block stripped = com.insidethecube.cubework.Extras.STRIPPED.get(this);
		if (stripped == null || !stack.is(net.minecraft.tags.ItemTags.AXES)) return super.useItemOn(stack, state, level, pos, player, hand, hit);
		level.playSound(player, pos, net.minecraft.sounds.SoundEvents.AXE_STRIP, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
		if (!level.isClientSide()) {
			level.setBlock(pos, Shared.copy(state, stripped), Block.UPDATE_ALL_IMMEDIATE);
			stack.hurtAndBreak(1, player, hand);
		}
		return net.minecraft.world.InteractionResult.SUCCESS;
	}
	//#endif

	private static BlockState ends(BlockState state, BlockGetter world, BlockPos pos) {
		Direction t = top(state.getValue(AXIS));
		return state.setValue(BARK_TOP, log(world.getBlockState(pos.relative(t)))).setValue(BARK_BOTTOM, log(world.getBlockState(pos.relative(t.getOpposite()))));
	}

	private static boolean log(BlockState s) {
		return s.getBlock() instanceof ThinLogBlock || s.is(BlockTags.LOGS) || s.is(Blocks.MUSHROOM_STEM);
	}
}
