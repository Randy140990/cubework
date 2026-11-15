package com.insidethecube.cubework.block;

import com.insidethecube.cubework.Behaviour;
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
 * Waxing, scraping with an axe and taking wax off: see Copper.
 */
public final class Aging {
	private Aging() {}

	public static void tick(Behaviour behaviour, BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
		if (behaviour != Behaviour.COPPER || !(world.getBlockEntity(pos) instanceof ShapeEntity entity)) return;
		Material material = entity.material();
		if (material == null) return;
		int age = material.copperAge();
		if (age < 0 || age == 3 || random.nextFloat() >= 0.05688889F) return;
		int same = 0, more = 0;
		// Every block within 4 steps (counted along the axes); the order doesn't change the result.
		for (BlockPos p : BlockPos.betweenClosed(pos.offset(-4, -4, -4), pos.offset(4, 4, 4))) {
			if (p.distManhattan(pos) > 4 || p.equals(pos)) continue;
			int a = ageOf(world, p);
			if (a < 0) continue;
			if (a < age) return;
			if (a > age) more++;
			else same++;
		}
		float f = (more + 1f) / (more + same + 1f);
		if (random.nextFloat() < f * f * (age == 0 ? 0.75f : 1f)) {
			Material next = material.nextAge();
			if (next != null) entity.setMaterial(next);
		}
	}

	private static int ageOf(ServerLevel world, BlockPos p) {
		BlockState s = world.getBlockState(p);
		if (s.getBlock() instanceof WeatheringCopper o) return o.getAge().ordinal();
		if (!(s.getBlock() instanceof CwShape shape) || shape.behaviour() != Behaviour.COPPER) return -1;
		Material m = ShapeEntity.at(world, p);
		return m == null ? -1 : m.copperAge();
	}
}
