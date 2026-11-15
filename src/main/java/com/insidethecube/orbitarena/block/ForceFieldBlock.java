package com.insidethecube.orbitarena.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A force field: a glowing see-through wash that fades out upwards. FADE 0 is the full field; 1 to 5 each
 * fade a block further, so a column stepping 0, 1, 2, 3, 4, 5 fades smoothly out to nothing at its top.
 * Some fields can be walked through (the boxes round the goals: you can't build into them, but you can go
 * in, and hit and shoot through them); others stop you (the wall in front of the stands).
 */
public class ForceFieldBlock extends TransparentBlock {
	public static final IntegerProperty FADE = IntegerProperty.create("fade", 0, 5);
	private final boolean solid;

	public ForceFieldBlock(Properties props, boolean solid) {
		super(props);
		this.solid = solid;
		registerDefaultState(stateDefinition.any().setValue(FADE, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FADE);
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return solid ? Shapes.block() : Shapes.empty();
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		// One you can walk through can't be aimed at either, so you hit (and see the outline of) what's behind it.
		return solid ? Shapes.block() : Shapes.empty();
	}
}
