package com.insidethecube.cubework.block;

import com.insidethecube.cubework.CwBlocks;
import com.insidethecube.cubework.Material;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Oxidizable;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

/**
 * Copper shapes weather as vanilla copper does, at the same rate and by the same rule: now and then a
 * random tick moves one on an age, more likely when the copper round it is further on, and never while
 * any copper within 4 blocks is younger. Vanilla copper and Cubework copper count for each other.
 * Waxing, scraping with an axe and taking wax off go through the game's own tables.
 */
public final class Aging {
	private Aging() {}

	public static void tick(Material material, BlockState state, ServerWorld world, BlockPos pos, Random random) {
		if (material == null) return;
		int age = material.copperAge();
		if (age < 0 || age == 3 || random.nextFloat() >= 0.05688889F) return;
		int same = 0, more = 0;
		for (BlockPos p : BlockPos.iterateOutwards(pos, 4, 4, 4)) {
			if (p.getManhattanDistance(pos) > 4) break;
			if (p.equals(pos)) continue;
			int a = ageOf(world.getBlockState(p).getBlock());
			if (a < 0) continue;
			if (a < age) return;
			if (a > age) more++;
			else same++;
		}
		float f = (more + 1f) / (more + same + 1f);
		if (random.nextFloat() < f * f * (age == 0 ? 0.75f : 1f)) {
			Oxidizable.getIncreasedOxidationBlock(state.getBlock()).ifPresent(next -> world.setBlockState(pos, Shared.copy(state, next)));
		}
	}

	private static int ageOf(Block b) {
		if (b instanceof Oxidizable o) return o.getDegradationLevel().ordinal();
		CwBlocks.Info i = CwBlocks.info(b);
		return i == null ? -1 : i.material().copperAge();
	}
}
