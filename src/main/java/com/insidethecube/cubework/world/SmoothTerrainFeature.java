package com.insidethecube.cubework.world;

import java.util.ArrayList;
import com.insidethecube.cubework.tool.Terrain;
import com.insidethecube.cubework.tool.TerrainBrushItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Smooths each new chunk's steps into slopes, last of all (after trees and plants), when smoothNewTerrain
 * is on in config/cubework.json. Off, it does nothing.
 */
public class SmoothTerrainFeature extends Feature<NoneFeatureConfiguration> {
	public SmoothTerrainFeature() {
		super(NoneFeatureConfiguration.CODEC);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
		if (!Config.smoothNewTerrain) return false;
		WorldGenLevel level = ctx.level();
		BlockPos o = ctx.origin();
		int x0 = o.getX() & ~15, z0 = o.getZ() & ~15;
		int low = Integer.MAX_VALUE, high = Integer.MIN_VALUE;
		for (int x = 0; x < 16; x += 3) {
			for (int z = 0; z < 16; z += 3) {
				int y = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x0 + x, z0 + z);
				low = Math.min(low, y);
				high = Math.max(high, y);
			}
		}
		if (low == Integer.MAX_VALUE) return false;
		// Ground under trees isn't seen (leaves are on top), which is fine: it stays as it was.
		Terrain.run(level, x0, z0, 16, 16, low - 8, high + 8, (x, z) -> true, TerrainBrushItem.FILL, Config.smoothSteepness, new ArrayList<>(), 2);
		return true;
	}
}
