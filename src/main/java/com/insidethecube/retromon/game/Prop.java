package com.insidethecube.retromon.game;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * One block of an inside: its look is one of the models made from the Interior Viewer's rooms (state M picks
 * which), its collision what's solid there (furniture and walls to 1.5 high, the floor, the stairs). It can't be
 * broken, lets light through and hides nothing behind it.
 */
public class Prop extends Block {
	public static final IntegerProperty M = IntegerProperty.create("m", 0, Math.max(1, Interiors.STATES - 1));

	public Prop(BlockBehaviour.Properties p) {
		super(p);
		registerDefaultState(stateDefinition.any().setValue(M, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
		b.add(M);
	}

	@Override
	protected VoxelShape getShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext c) {
		return Interiors.shape(s.getValue(M));
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext c) {
		return Interiors.shape(s.getValue(M));
	}

	@Override
	protected VoxelShape getOcclusionShape(BlockState s) {
		return Shapes.empty();
	}

	@Override
	protected boolean propagatesSkylightDown(BlockState s) {
		return true;
	}

	@Override
	protected float getShadeBrightness(BlockState s, BlockGetter level, BlockPos pos) {
		return 1F;
	}
}
