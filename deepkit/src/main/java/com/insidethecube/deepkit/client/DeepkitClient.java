package com.insidethecube.deepkit.client;

import java.util.List;
import com.insidethecube.deepkit.DK;
import com.insidethecube.deepkit.lamp.LampData;
import com.insidethecube.deepkit.net.Payloads;
import com.insidethecube.deepkit.paint.GlowMarkBlock;
import com.insidethecube.deepkit.paint.GlowPaintItem;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** The lamp key (H), the glow paint mark picker, how flares and hooks look in flight, and paint colours. */
public final class DeepkitClient implements ClientModInitializer {
	public static KeyMapping LAMPS;

	@Override
	public void onInitializeClient() {
		LAMPS = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.deepkit.lamps", InputConstants.Type.KEYSYM, 72,
				KeyMapping.Category.register(DK.id("deepkit"))));
		Beams.register();
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			while (LAMPS.consumeClick()) {
				if (mc.player != null) ClientPlayNetworking.send(new Payloads.ToggleLamps(true));
			}
		});
		GlowPaintItem.openPicker = () -> {
			Minecraft mc = Minecraft.getInstance();
			if (mc.player == null) return;
			ItemStack held = mc.player.getMainHandItem().getItem() instanceof GlowPaintItem ? mc.player.getMainHandItem() : mc.player.getOffhandItem();
			mc.setScreen(new MarkPickerScreen(GlowPaintItem.mark(held)));
		};

		EntityRendererRegistry.register(DK.FLARE, ThrownItemRenderer::new);
		EntityRendererRegistry.register(DK.HOOK, ThrownItemRenderer::new);

		BlockRenderLayerMap.putBlock(DK.ROPE, ChunkSectionLayer.CUTOUT);
		for (GlowMarkBlock mark : DK.MARKS) {
			BlockRenderLayerMap.putBlock(mark, ChunkSectionLayer.CUTOUT);
			int rgb = 0xFF000000 | mark.colour.rgb;
			ColorProviderRegistry.BLOCK.register((state, view, pos, tint) -> rgb, mark);
		}

		// A helmet or chestplate with a lamp fitted says so, with its battery.
		ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
			LampData d = stack.get(DK.LAMP);
			if (d == null || stack.getItem() instanceof com.insidethecube.deepkit.lamp.LampItem) return;
			addLampLines(lines, d);
		});
	}

	private static void addLampLines(List<Component> lines, LampData d) {
		lines.add(Component.literal(d.lamp().title + " fitted: battery " + d.percent() + "%" + (d.on() ? ", on" : "")).withStyle(ChatFormatting.GOLD));
		lines.add(Component.literal("H switches it; on its own in a crafting grid gives the lamp back").withStyle(ChatFormatting.DARK_GRAY));
	}
}
