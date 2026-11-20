package com.insidethecube.retromon.client;

import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.block.Blocks;

/**
 * The games' brighter greens: in a Retromon save, grass and leaves take a livelier, sunnier green (more
 * colour, a touch lighter, leaning yellow), so the world feels like the handheld games rather than plain
 * Minecraft. Other worlds keep their own colours.
 */
public final class Colours {
	private Colours() {}

	public static void register() {
		ColorProviderRegistry.BLOCK.register((state, level, pos, tint) -> {
			int c = level != null && pos != null ? BiomeColors.getAverageGrassColor(level, pos) : GrassColor.getDefaultColor();
			return Adventure.inSave ? brighten(c) : c;
		}, Blocks.GRASS_BLOCK, Blocks.SHORT_GRASS, Blocks.TALL_GRASS, Blocks.FERN, Blocks.LARGE_FERN, Blocks.BUSH, Blocks.SUGAR_CANE);
		ColorProviderRegistry.BLOCK.register((state, level, pos, tint) -> {
			int c = level != null && pos != null ? BiomeColors.getAverageFoliageColor(level, pos) : FoliageColor.FOLIAGE_DEFAULT;
			return Adventure.inSave ? brighten(c) : c;
		}, Blocks.OAK_LEAVES, Blocks.JUNGLE_LEAVES, Blocks.ACACIA_LEAVES, Blocks.DARK_OAK_LEAVES, Blocks.MANGROVE_LEAVES, Blocks.VINE);
	}

	/** More saturated, a little lighter, and nudged towards yellow-green. */
	static int brighten(int rgb) {
		float r = ((rgb >> 16) & 255) / 255F, g = ((rgb >> 8) & 255) / 255F, b = (rgb & 255) / 255F;
		float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b)), d = max - min;
		float h = 0;
		if (d > 0) {
			if (max == r) h = ((g - b) / d) % 6;
			else if (max == g) h = (b - r) / d + 2;
			else h = (r - g) / d + 4;
			h /= 6;
			if (h < 0) h += 1;
		}
		float s = max == 0 ? 0 : d / max, v = max;
		// Greens (hue about a third) lean a little towards yellow; everything gets more colour and light.
		if (h > 0.2F && h < 0.45F) h -= 0.018F;
		s = Math.min(1, s * 1.3F + 0.05F);
		// (A touch brighter again: the grass floors read dark under Retromon Light.)
		v = Math.min(1, v * 1.2F + 0.04F);
		int i = (int) Math.floor(h * 6);
		float f = h * 6 - i, p = v * (1 - s), q = v * (1 - f * s), t = v * (1 - (1 - f) * s);
		float rr, gg, bb;
		switch (Math.floorMod(i, 6)) {
			case 0 -> { rr = v; gg = t; bb = p; }
			case 1 -> { rr = q; gg = v; bb = p; }
			case 2 -> { rr = p; gg = v; bb = t; }
			case 3 -> { rr = p; gg = q; bb = v; }
			case 4 -> { rr = t; gg = p; bb = v; }
			default -> { rr = v; gg = p; bb = q; }
		}
		return 0xFF000000 | (Math.round(rr * 255) << 16) | (Math.round(gg * 255) << 8) | Math.round(bb * 255);
	}
}
