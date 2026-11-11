package com.insidethecube.cubework.client;

import net.minecraft.world.level.block.Block;
//#if MC >= 1.21.6
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
//#else
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.RenderType;
//#endif

/** Which blocks are drawn see-through or with cut-out holes (the API for it changed in 1.21.6). */
final class Layers {
	private Layers() {}

	static void cutout(Block b) {
		//#if MC >= 1.21.6
		BlockRenderLayerMap.putBlock(b, ChunkSectionLayer.CUTOUT);
		//#else
		BlockRenderLayerMap.INSTANCE.putBlock(b, RenderType.cutout());
		//#endif
	}

	static void translucent(Block b) {
		//#if MC >= 1.21.6
		BlockRenderLayerMap.putBlock(b, ChunkSectionLayer.TRANSLUCENT);
		//#else
		BlockRenderLayerMap.INSTANCE.putBlock(b, RenderType.translucent());
		//#endif
	}
}
