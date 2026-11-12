package com.insidethecube.cubework.block;

import com.insidethecube.cubework.Material;
import net.fabricmc.fabric.api.block.v1.FabricBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** A Cubework shape knows which material it's made of. */
public interface CwShape extends FabricBlock {
	Material material();

	/**
	 * To connected-texture mods (Continuity), every shape looks like the full block it's made from, so a pack's
	 * connected textures, random variants and edge blends for grass, sand or glass apply to their shapes too,
	 * and shapes join up with the full blocks beside them.
	 */
	@Override
	default BlockState getAppearance(BlockState state, BlockAndTintGetter renderView, BlockPos pos, Direction side, BlockState sourceState,
			BlockPos sourcePos) {
		Block base = material().base();
		return base == null || base == state.getBlock() ? state : base.defaultBlockState();
	}
}
