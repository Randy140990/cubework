package com.insidethecube.cubework.client;

import java.util.ArrayList;
import java.util.List;

import com.insidethecube.cubework.CwBlocks;
import com.insidethecube.cubework.Extras;
import com.insidethecube.cubework.Material;
import com.insidethecube.cubework.Shape;

import com.insidethecube.cubework.client.wheel.Wheel;
import com.insidethecube.cubework.client.wheel.WheelModels;
import com.insidethecube.cubework.client.wheel.WheelScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
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
	/** Hold for the shape wheel (G to start with). */
	public static KeyMapping WHEEL;

	@Override
	public void onInitializeClient() {
		WHEEL = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.cubework.wheel", InputConstants.Type.KEYSYM, 71,
				KeyMapping.Category.register(com.insidethecube.cubework.Reg.id("cubework"))));
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			while (WHEEL.consumeClick()) {
				if (mc.screen != null || mc.player == null) continue;
				Wheel wheel = WheelModels.forPlayer(mc);
				if (wheel == null) {
					net.minecraft.world.item.ItemStack held = mc.player.getMainHandItem();
					mc.player.displayClientMessage(Component.literal(held.isEmpty()
							? "Hold a block to choose its shape (or the terrain brush or builder's guide for their settings)."
							: held.getHoverName().getString() + " has no Cubework shapes."), true);
					continue;
				}
				mc.setScreen(new WheelScreen(wheel, WHEEL));
			}
			ToolHints.tick(mc);
		});
		ToolRender.register();

		// The coloured doors' textures are made from the door textures in use, as the game loads.
		//#if MC >= 1.21.5
		Atlas.register(DoorColoursSource.ID, DoorColoursSource.CODEC);
		Atlas.register(MetalDoorsSource.ID, MetalDoorsSource.CODEC);
		//#else
		DoorColoursSource.TYPE = Atlas.register(DoorColoursSource.ID, DoorColoursSource.CODEC);
		MetalDoorsSource.TYPE = Atlas.register(MetalDoorsSource.ID, MetalDoorsSource.CODEC);
		//#endif
		//#if MC >= 1.21.5
		Atlas.register(PatternSource.ID, PatternSource.CODEC);
		//#else
		PatternSource.TYPE = Atlas.register(PatternSource.ID, PatternSource.CODEC);
		//#endif
		com.insidethecube.cubework.ShaderMaps.painter = PatternMaps::make;

		List<Block> grass = new ArrayList<>(Extras.GRASSY_PLANTS);
		// Glass and ice are see-through, as the game draws them; grass's side overlay is cut out quad by quad
		// (see MaterialModels).
		for (Block b : CwBlocks.all()) {
			CwBlocks.Info info = CwBlocks.info(b);
			if (info.behaviour().seeThrough()) Layers.translucent(b);
			// Only grass's quads ask for a colour.
			grass.add(b);
		}
		for (Block b : Extras.SIZED_PLANTS) Layers.cutout(b);
		for (Block b : Extras.CORAL) Layers.cutout(b);
		for (Block b : Extras.DOORS) Layers.cutout(b);
		for (Block b : Extras.TRAPDOORS) Layers.cutout(b);
		for (Block b : Extras.LIGHT_FITTINGS) Layers.cutout(b);
		for (Block b : Extras.GLASS_TABLES) Layers.translucent(b);

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

		// Tables pick their joined look from their neighbours as they're drawn; the shapes' window frames and
		// partitions do too, through their materials' looks.
		JoinedModels.register(b -> CwBlocks.info(b) != null);
		// Every shape: its material's look, or drawn in code from its material's textures.
		MaterialModels.register();
		net.minecraft.client.renderer.item.ItemModels.ID_MAPPER.put(MaterialItemModel.ID, MaterialItemModel.Unbaked.MAP_CODEC);

		ModelLoadingPlugin.register(ctx -> {
		// Round logs: bark round the sides, rings on the ends.
			for (java.util.Map.Entry<Block, String[]> e : Extras.ROUND_LOG_TEXTURES.entrySet()) {
				String side = e.getValue()[0], end = e.getValue()[1];
				Models.register(ctx, e.getKey(), (state, sprites) -> RoundModel.buildLog(side, end, state, sprites));
			}
		});
	}
}
