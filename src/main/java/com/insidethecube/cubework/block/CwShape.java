package com.insidethecube.cubework.block;

import com.insidethecube.cubework.Behaviour;
import com.insidethecube.cubework.CwBlocks;
import com.insidethecube.cubework.Material;
import net.fabricmc.fabric.api.block.v1.FabricBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A Cubework shape: one block for every material of its behaviour, each placed one keeping its material
 * in a ShapeEntity.
 */
public interface CwShape extends FabricBlock, EntityBlock {
	Behaviour behaviour();

	@Override
	default BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ShapeEntity(pos, state);
	}

	/**
	 * To connected-texture mods (Continuity), every shape looks like the full block it's made from, so a pack's
	 * connected textures, random variants and edge blends for grass, sand or glass apply to their shapes too,
	 * and shapes join up with the full blocks beside them.
	 */
	@Override
	default BlockState getAppearance(BlockState state, BlockAndTintGetter renderView, BlockPos pos, Direction side, BlockState sourceState,
			BlockPos sourcePos) {
		Material m = renderView.getBlockEntityRenderData(pos) instanceof Material r ? r : CwBlocks.defaultMaterial(state.getBlock());
		Block base = m == null ? null : m.base();
		return base == null || base == state.getBlock() ? state : base.defaultBlockState();
	}
}
