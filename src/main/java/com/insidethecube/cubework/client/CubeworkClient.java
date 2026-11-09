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
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.world.biome.GrassColors;

public final class CubeworkClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// The coloured doors' textures are made from the door textures in use, as the game loads.
		AtlasSourceManagerAccessor.cubework$idMapper().put(DoorColoursSource.ID, DoorColoursSource.CODEC);
		AtlasSourceManagerAccessor.cubework$idMapper().put(MetalDoorsSource.ID, MetalDoorsSource.CODEC);

		List<Block> grass = new ArrayList<>(Extras.GRASSY_PLANTS);
		for (Material m : Material.ALL) {
			var shapes = CwBlocks.shapesOf(m).values();
			// Glass and ice are see-through, as the game draws them; grass needs cut-out for its side overlay.
			if (m.seeThrough()) for (Block b : shapes) BlockRenderLayerMap.putBlock(b, BlockRenderLayer.TRANSLUCENT);
			else if (m.grassTinted()) for (Block b : shapes) BlockRenderLayerMap.putBlock(b, BlockRenderLayer.CUTOUT);
			if (m.grassTinted()) grass.addAll(shapes);
		}
		for (Block b : Extras.SIZED_PLANTS) BlockRenderLayerMap.putBlock(b, BlockRenderLayer.CUTOUT);
		for (Block b : Extras.CORAL) BlockRenderLayerMap.putBlock(b, BlockRenderLayer.CUTOUT);
		for (Block b : Extras.DOORS) BlockRenderLayerMap.putBlock(b, BlockRenderLayer.CUTOUT);

		// Grass shapes, grass, ferns and bushes take the biome's grass colour.
		ColorProviderRegistry.BLOCK.register((state, world, pos, tint) -> world != null && pos != null ? BiomeColors.getGrassColor(world, pos)
				: GrassColors.getDefaultColor(), grass.toArray(new Block[0]));

		// Slopes and arches have their own models, built from the vanilla block's textures.
		ModelLoadingPlugin.register(ctx -> {
			for (Material m : Material.ALL) {
				SlopeModel.Unbaked slopeModel = new SlopeModel.Unbaked(m);
				ArchModel.Unbaked archModel = new ArchModel.Unbaked(m);
				for (Shape shape : new Shape[] {Shape.SLOPE, Shape.GENTLE_SLOPE, Shape.ARCH, Shape.ARCH_CORNER}) {
					Block block = CwBlocks.get(m, shape);
					if (block == null) continue;
					BlockStateModel.UnbakedGrouped model = shape == Shape.ARCH || shape == Shape.ARCH_CORNER ? archModel : slopeModel;
					ctx.registerBlockStateResolver(block, resolver -> {
						for (BlockState state : block.getStateManager().getStates()) resolver.setModel(state, model);
					});
				}
			}
		});
	}
}
