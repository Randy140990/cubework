package com.insidethecube.cubework.client;

import com.insidethecube.cubework.CwBlocks;
import com.insidethecube.cubework.Material;
import com.insidethecube.cubework.Shape;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.util.Identifier;

public final class CubeworkClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Glass shapes draw like their glass: clear glass with hard edges, coloured and tinted glass see-through.
		for (Material m : Material.ALL) {
			if (!m.glass()) continue;
			BlockRenderLayer layer = m.translucent() ? BlockRenderLayer.TRANSLUCENT : BlockRenderLayer.CUTOUT;
			for (Block b : CwBlocks.shapesOf(m).values()) BlockRenderLayerMap.putBlock(b, layer);
		}
		// Slopes have their own model, built from the vanilla block's texture.
		ModelLoadingPlugin.register(ctx -> {
			for (Material m : Material.ALL) {
				Block slope = CwBlocks.get(m, Shape.SLOPE);
				SlopeModel.Unbaked model = new SlopeModel.Unbaked(Identifier.ofVanilla("block/" + m.id()));
				ctx.registerBlockStateResolver(slope, resolver -> {
					for (BlockState state : slope.getStateManager().getStates()) resolver.setModel(state, model);
				});
			}
		});
	}
}
