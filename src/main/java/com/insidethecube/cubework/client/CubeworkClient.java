package com.insidethecube.cubework.client;

import java.util.ArrayList;
import java.util.List;

import com.insidethecube.cubework.CwBlocks;
import com.insidethecube.cubework.Extras;
import com.insidethecube.cubework.Material;
import com.insidethecube.cubework.Shape;
import com.insidethecube.cubework.mixin.AtlasSourceManagerAccessor;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.world.biome.GrassColors;

public final class CubeworkClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// The coloured doors' textures are made from the door textures in use, as the game loads.
		AtlasSourceManagerAccessor.cubework$idMapper().put(DoorColoursSource.ID, DoorColoursSource.CODEC);

		List<Block> grass = new ArrayList<>(Extras.GRASSY_PLANTS);
		for (Material m : Material.ALL) {
			var shapes = CwBlocks.shapesOf(m).values();
			// Glass and ice are see-through, as the game draws them; grass needs cut-out for its side overlay.
			if (m.seeThrough()) for (Block b : shapes) BlockRenderLayerMap.putBlock(b, BlockRenderLayer.TRANSLUCENT);
			else if (m.grassTinted()) for (Block b : shapes) BlockRenderLayerMap.putBlock(b, BlockRenderLayer.CUTOUT);
			if (m.grassTinted()) grass.addAll(shapes);
		}
		for (Block b : Extras.SIZED_PLANTS) BlockRenderLayerMap.putBlock(b, BlockRenderLayer.CUTOUT);
		for (Block b : Extras.DOORS) BlockRenderLayerMap.putBlock(b, BlockRenderLayer.CUTOUT);

		// Grass shapes, grass, ferns and bushes take the biome's grass colour.
		ColorProviderRegistry.BLOCK.register((state, world, pos, tint) -> world != null && pos != null ? BiomeColors.getGrassColor(world, pos)
				: GrassColors.getDefaultColor(), grass.toArray(new Block[0]));

		// Slopes have their own model, built from the vanilla block's textures.
		ModelLoadingPlugin.register(ctx -> {
			for (Material m : Material.ALL) {
				Block slope = CwBlocks.get(m, Shape.SLOPE);
				SlopeModel.Unbaked model = new SlopeModel.Unbaked(m);
				ctx.registerBlockStateResolver(slope, resolver -> {
					for (BlockState state : slope.getStateManager().getStates()) resolver.setModel(state, model);
				});
			}
		});
	}
}
