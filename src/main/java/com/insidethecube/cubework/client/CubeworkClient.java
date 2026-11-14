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

		// Window frames, tables and partitions pick their joined look from their neighbours as they're drawn.
		JoinedModels.register(b -> b instanceof com.insidethecube.cubework.block.SlopeBlock
				|| b instanceof com.insidethecube.cubework.block.WindowFrameBlock w && w.style() == com.insidethecube.cubework.block.WindowFrameBlock.Style.ROUND);

		// Slopes, arches and round shapes have their own models, built from the vanilla block's textures.
		ModelLoadingPlugin.register(ctx -> {
			for (Material m : Material.ALL) {
				for (Shape shape : Shape.values()) {
					if (!shape.drawnInCode()) continue;
					Block block = CwBlocks.get(m, shape);
					if (block == null) continue;
					if (shape == Shape.STAIR_CURVE) {
						// 35 pieces in four facings: quads made when first drawn.
						Material.Textures t = m.textures();
						Models.registerLazy(ctx, block, List.of(t.top(), t.side(), t.bottom()), t.side(), (state, sprites) -> StairCurveModel.build(m, state, sprites));
						continue;
					}
					if (shape.round()) {
						// Round window frames: 16 ways of joining for each state, their quads made when first drawn.
						Material.Textures t = m.textures();
						Models.registerJoinedLazy(ctx, block, List.of(t.top(), t.side(), t.bottom()), t.side(), (state, joins, sprites) -> RoundWindowModel.build(m, state, joins, sprites));
						continue;
					}
					if (shape == Shape.SLOPE || shape == Shape.SLOPE_2 || shape == Shape.SLOPE_3 || shape == Shape.GENTLE_SLOPE) {
						// The corner each slope turns is picked from its neighbours as it's drawn: 5 looks, made when first drawn.
						Material.Textures t = m.textures();
						List<String> textures = new ArrayList<>(List.of(t.top(), t.side(), t.bottom()));
						if (t.overlay() != null) textures.add(t.overlay());
						Models.registerJoinedLazy(ctx, block, textures, t.side(), (state, joins, sprites) -> SlopeModel.build(m, state, joins, sprites));
						continue;
					}
					Models.Maker maker = switch (shape) {
						case ARCH, ARCH_CORNER -> (state, sprites) -> ArchModel.build(m, state, sprites);
						case ROUND_PILLAR_2, ROUND_PILLAR_3, ROUND_PILLAR_4 -> (state, sprites) -> RoundPillarModel.build(m, state, sprites);
						case ROUND_COLUMN_16, ROUND_COLUMN_12, ROUND_COLUMN_8, ROUND_COLUMN_4, ROUND_CORNER, ROUND_CORNER_INNER ->
								(state, sprites) -> RoundModel.build(m, state, sprites);
						default -> (state, sprites) -> SlopeModel.build(m, state, 0, sprites);
					};
					Models.register(ctx, block, maker);
				}
			}
			// Round logs: bark round the sides, rings on the ends.
			for (java.util.Map.Entry<Block, String[]> e : Extras.ROUND_LOG_TEXTURES.entrySet()) {
				String side = e.getValue()[0], end = e.getValue()[1];
				Models.register(ctx, e.getKey(), (state, sprites) -> RoundModel.buildLog(side, end, state, sprites));
			}
		});
	}
}
