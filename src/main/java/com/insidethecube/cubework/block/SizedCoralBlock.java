package com.insidethecube.cubework.block;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
//#if MC >= 1.21.2
import net.minecraft.world.level.ScheduledTickAccess;
//#else
import net.minecraft.world.level.LevelAccessor;
//#endif
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Coral or a coral fan at another size, behaving like vanilla's: it stands on the top of a solid block,
 * fills with water when placed in water, and living coral out of water dies after a few seconds, turning
 * into the dead coral of the same size.
 */
public class SizedCoralBlock extends Block implements SimpleWaterloggedBlock {
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

	private final VoxelShape shape;
	/** The dead coral it turns into, or null when it's dead already. */
	private final Supplier<Block> dead;

	public SizedCoralBlock(double scale, boolean fan, Supplier<Block> dead, BlockBehaviour.Properties settings) {
		super(settings);
		this.dead = dead;
		double half = Math.min(8, 6 * scale), h = Math.min(16, (fan ? 4 : 15) * scale);
		this.shape = Block.box(8 - half, 0, 8 - half, 8 + half, Math.max(1, h), 8 + half);
		registerDefaultState(defaultBlockState().setValue(WATERLOGGED, true));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(WATERLOGGED);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return shape;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		FluidState fluid = ctx.getLevel().getFluidState(ctx.getClickedPos());
		return defaultBlockState().setValue(WATERLOGGED, fluid.is(FluidTags.WATER) && fluid.getAmount() == 8);
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
		BlockPos below = pos.below();
		return world.getBlockState(below).isFaceSturdy(world, below, Direction.UP);
	}

	@Override
	protected void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean notify) {
		if (dead != null && !inWater(state, world, pos)) world.scheduleTick(pos, this, 60 + world.getRandom().nextInt(40));
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
		if (direction == Direction.DOWN && !canSurvive(state, world, pos)) return Blocks.AIR.defaultBlockState();
		if (state.getValue(WATERLOGGED)) tickView.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
		if (dead != null && !inWater(state, world, pos)) tickView.scheduleTick(pos, this, 60 + random.nextInt(40));
		return state;
	}

	@Override
	protected void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
		if (dead != null && !inWater(state, world, pos)) world.setBlock(pos, dead.get().defaultBlockState().setValue(WATERLOGGED, false), Block.UPDATE_CLIENTS);
	}

	/** Living coral needs water in its own block or any block touching it. */
	private static boolean inWater(BlockState state, BlockGetter world, BlockPos pos) {
		if (state.getValue(WATERLOGGED)) return true;
		for (Direction d : Direction.values()) if (world.getFluidState(pos.relative(d)).is(FluidTags.WATER)) return true;
		return false;
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}
}
