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
 * One block of an inside: its look is one of the models made from the Interior Viewer's rooms, its collision what's
 * solid there (furniture and walls to 1.5 high, the floor, the stairs). It can't be broken, lets light through and
 * hides nothing behind it.
 * <p>
 * (0.67, John's 16:07: "the mod is getting really slow to start") The rooms' looks, some 37 thousand, are shared out
 * over many blocks of {@link #SIZE} states each (retromon:prop, retromon:prop_1, ...). The time and memory Minecraft
 * takes to make a block's states grow with the square of how many it has: all of them in one block took about a
 * minute and a half and five gigabytes at every start (measured in the CI: 16 thousand took 16 seconds and a
 * gigabyte); in blocks of 256 it's under a second.
 */
public class Prop extends Block {
	/** States per block. */
	public static final int SIZE = 256;
	/**
	 * How many blocks: enough for the looks, and never fewer than this, so the blocks' names stay the same from one
	 * release to the next (the shaders list them: shaderpack's block.properties).
	 */
	public static final int BLOCKS = Math.max(160, (Interiors.STATES + SIZE - 1) / SIZE);
	public static final IntegerProperty M = IntegerProperty.create("m", 0, SIZE - 1);
	/** The blocks, in order. */
	public static final Prop[] ALL = new Prop[BLOCKS];
	/** The look this block's m=0 is (its m=k being look first + k). */
	public final int first;

	public Prop(BlockBehaviour.Properties p, int first) {
		super(p);
		this.first = first;
		registerDefaultState(stateDefinition.any().setValue(M, 0));
	}

	/** The block state for one of the looks. */
	public static BlockState state(int look) {
		int g = Math.max(0, Math.min(Interiors.STATES - 1, look));
		return ALL[g / SIZE].defaultBlockState().setValue(M, g % SIZE);
	}

	/** Which look a state is (-1 for any other block's). */
	public static int look(BlockState s) {
		return s.getBlock() instanceof Prop p ? p.first + s.getValue(M) : -1;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
		b.add(M);
	}

	@Override
	protected VoxelShape getShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext c) {
		return Interiors.shape(first + s.getValue(M));
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext c) {
		return Interiors.shape(first + s.getValue(M));
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
