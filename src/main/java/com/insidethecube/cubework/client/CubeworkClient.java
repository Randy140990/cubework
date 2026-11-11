package com.insidethecube.cubework.client;

import java.util.ArrayList;
import java.util.List;

import com.insidethecube.cubework.CwBlocks;
import com.insidethecube.cubework.Extras;
import com.insidethecube.cubework.Material;
import com.insidethecube.cubework.Shape;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
//#if MC >= 26.1
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
import net.minecraft.client.color.block.BlockTintSources;
//#else
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.world.level.GrassColor;
//#endif
import net.minecraft.world.level.block.Block;

public final class CubeworkClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// The coloured doors' textures are made from the door textures in use, as the game loads.
		//#if MC >= 1.21.5
		Atlas.register(DoorColoursSource.ID, DoorColoursSource.CODEC);
		Atlas.register(MetalDoorsSource.ID, MetalDoorsSource.CODEC);
		//#else
		DoorColoursSource.TYPE = Atlas.register(DoorColoursSource.ID, DoorColoursSource.CODEC);
		MetalDoorsSource.TYPE = Atlas.register(MetalDoorsSource.ID, MetalDoorsSource.CODEC);
		//#endif

		List<Block> grass = new ArrayList<>(Extras.GRASSY_PLANTS);
		for (Material m : Material.ALL) {
			var shapes = CwBlocks.shapesOf(m).values();
			// Glass and ice are see-through, as the game draws them; grass needs cut-out for its side overlay.
			if (m.seeThrough()) for (Block b : shapes) Layers.translucent(b);
			else if (m.grassTinted()) for (Block b : shapes) Layers.cutout(b);
			if (m.grassTinted()) grass.addAll(shapes);
		}
		for (Block b : Extras.SIZED_PLANTS) Layers.cutout(b);
		for (Block b : Extras.CORAL) Layers.cutout(b);
		for (Block b : Extras.DOORS) Layers.cutout(b);

		// Grass shapes, grass, ferns and bushes take the biome's grass colour.
		//#if MC >= 26.1
		BlockColorRegistry.register(java.util.List.of(BlockTintSources.grass()), grass.toArray(new Block[0]));
		//#else
		ColorProviderRegistry.BLOCK.register((state, world, pos, tint) -> world != null && pos != null ? BiomeColors.getAverageGrassColor(world, pos)
				: GrassColor.getDefaultColor(), grass.toArray(new Block[0]));
		//#endif
		//#if MC < 1.21.4
		// Before 1.21.4 items take their tints from code too.
		ColorProviderRegistry.ITEM.register((stack, tint) -> GrassColor.getDefaultColor(),
				grass.stream().map(Block::asItem).toArray(net.minecraft.world.level.ItemLike[]::new));
		//#endif

		// Slopes and arches have their own models, built from the vanilla block's textures.
		ModelLoadingPlugin.register(ctx -> {
			for (Material m : Material.ALL) {
				for (Shape shape : new Shape[] {Shape.SLOPE, Shape.SLOPE_2, Shape.SLOPE_3, Shape.GENTLE_SLOPE, Shape.ARCH, Shape.ARCH_CORNER}) {
					Block block = CwBlocks.get(m, shape);
					if (block == null) continue;
					boolean arch = shape == Shape.ARCH || shape == Shape.ARCH_CORNER;
					Models.register(ctx, block, (state, sprites) -> arch ? ArchModel.build(m, state, sprites) : SlopeModel.build(m, state, sprites));
				}
			}
		});
	}
}
