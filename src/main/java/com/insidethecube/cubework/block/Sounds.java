package com.insidethecube.cubework.block;

import com.insidethecube.cubework.CwBlocks;
import com.insidethecube.cubework.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What a placed shape sounds like: its material's sound (a wool slab muffled, a glass one clinking), not
 * its block's, which every material of the behaviour shares. Used by the mixins where the game plays a
 * block's sound (stepping, falling, placing, hitting, breaking).
 */
public final class Sounds {
	private Sounds() {}

	public static SoundType at(BlockGetter world, BlockPos pos, BlockState state, SoundType fallback) {
		if (!(state.getBlock() instanceof CwShape)) return fallback;
		Material m = ShapeEntity.at(world, pos);
		return of(m, fallback);
	}

	/** Placing: the stack's material. */
	public static SoundType placing(ItemStack stack, BlockState state, SoundType fallback) {
		if (!(state.getBlock() instanceof CwShape)) return fallback;
		return of(CwBlocks.materialOf(stack), fallback);
	}

	private static SoundType of(Material m, SoundType fallback) {
		Block base = m == null ? null : m.base();
		return base == null ? fallback : base.defaultBlockState().getSoundType();
	}
}
