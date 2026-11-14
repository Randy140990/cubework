package com.insidethecube.cubework.block;

import com.insidethecube.cubework.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
//#if MC >= 1.21.2
import net.minecraft.world.level.ScheduledTickAccess;
//#else
import net.minecraft.world.level.LevelAccessor;
//#endif
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The vanilla CarpetBlock in a Cubework material, which also turns: it's laid facing you, as glazed
 * terracotta is, and an empty hand turns it a quarter turn (so glazed patterns can be lined up).
 */
public class CwCarpet extends CarpetBlock implements CwShape {
	private final Material material;

	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

	public CwCarpet(Material material, BlockBehaviour.Properties settings) {
		super(settings);
		this.material = material;
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(FACING);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
		if (!player.getMainHandItem().isEmpty()) return InteractionResult.PASS;
		world.setBlock(pos, state.setValue(FACING, state.getValue(FACING).getClockWise()), Block.UPDATE_ALL);
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

	@Override
	public Material material() {
		return material;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		BlockState state = super.getStateForPlacement(ctx);
		if (state != null) state = state.setValue(FACING, ctx.getHorizontalDirection().getOpposite());
		return Shared.placed(material, state, ctx);
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
		return Shared.updated(material, super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random), world, pos);
		//#else
		return Shared.updated(material, super.updateShape(state, direction, neighborState, world, pos, neighborPos), world, pos);
		//#endif
	}

	@Override
	protected boolean skipRendering(BlockState state, BlockState stateFrom, Direction direction) {
		return Shared.hidesFace(material, state, stateFrom, direction) || super.skipRendering(state, stateFrom, direction);
	}

	/** Copper shapes weather like vanilla copper. */
	@Override
	protected void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
		Aging.tick(material, state, world, pos, random);
	}
}
