package com.insidethecube.cubework.block;

import com.insidethecube.cubework.CwBlocks;
import com.insidethecube.cubework.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Copper shapes weather as vanilla copper does, at the same rate and by the same rule: now and then a
 * random tick moves one on an age, more likely when the copper round it is further on, and never while
 * any copper within 4 blocks is younger. Vanilla copper and Cubework copper count for each other.
 * Waxing, scraping with an axe and taking wax off go through the game's own tables.
 */
public final class Aging {
	private Aging() {}

	public static void tick(Material material, BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
		if (material == null) return;
		int age = material.copperAge();
		if (age < 0 || age == 3 || random.nextFloat() >= 0.05688889F) return;
		int same = 0, more = 0;
		for (BlockPos p : BlockPos.withinManhattan(pos, 4, 4, 4)) {
			if (p.distManhattan(pos) > 4) break;
			if (p.equals(pos)) continue;
			int a = ageOf(world.getBlockState(p).getBlock());
			if (a < 0) continue;
			if (a < age) return;
			if (a > age) more++;
			else same++;
		}
		float f = (more + 1f) / (more + same + 1f);
		if (random.nextFloat() < f * f * (age == 0 ? 0.75f : 1f)) {
			WeatheringCopper.getNext(state.getBlock()).ifPresent(next -> world.setBlockAndUpdate(pos, Shared.copy(state, next)));
		}
	}

	private static int ageOf(Block b) {
		if (b instanceof WeatheringCopper o) return o.getAge().ordinal();
		CwBlocks.Info i = CwBlocks.info(b);
		return i == null ? -1 : i.material().copperAge();
	}
}
