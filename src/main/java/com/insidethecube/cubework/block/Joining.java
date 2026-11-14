package com.insidethecube.cubework.block;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * A block that joins its neighbours without keeping a note of it in its block state: which sides join is
 * worked out from the blocks round it whenever it's needed (its hit box, and its model as the chunk is
 * drawn). Window frames, tables and partitions do this, so each has a handful of block states instead of
 * one for every way of joining (192 for a frame), and the game starts far quicker with Cubework.
 * <p>
 * Their blocks set dynamicShape, so the game asks for the hit box each time rather than once per state.
 */
public interface Joining {
	/** Which sides it joins now, as bits in the order of joinProperties (bit 0 first). */
	int joins(BlockGetter world, BlockPos pos, BlockState state);

	/** The joins to show when there's no world to look at (in the hand, falling, being pushed). */
	default int lone(BlockState state) {
		return 0;
	}

	/**
	 * The yes/no properties the model's look is written with (its blockstate file, under joined/), in bit
	 * order: true where it joins. They're not properties of the block itself.
	 */
	List<BooleanProperty> joinProperties();
}
