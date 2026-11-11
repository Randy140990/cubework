package com.insidethecube.cubework.block;

import com.insidethecube.cubework.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The vanilla SlabBlock in a Cubework material: placed, connected and used exactly like vanilla's. */
public class CwSlab extends SlabBlock implements CwShape {
	private final Material material;

	public CwSlab(Material material, BlockBehaviour.Properties settings) {
		super(settings);
		this.material = material;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		int h = material.height();
		if (h == 16) return super.getShape(state, world, pos, context);
		return switch (state.getValue(TYPE)) {
			case BOTTOM -> Block.box(0, 0, 0, 16, h / 2.0, 16);
			case TOP -> Block.box(0, h / 2.0, 0, 16, h, 16);
			case DOUBLE -> Block.box(0, 0, 0, 16, h, 16);
		};
	}

	@Override
	public Material material() {
		return material;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		return Shared.placed(material, super.getStateForPlacement(ctx), ctx);
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
