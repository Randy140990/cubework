package com.insidethecube.retromon.client;

import com.insidethecube.retromon.Retromon;
import com.insidethecube.retromon.game.Net;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

/** Retromon on your screen: the camera, the keys, the people and Pokémon, the screens, the music and the title button. */
public class RetromonClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(Retromon.NPC, NpcRenderer::new);
		EntityRendererRegistry.register(Retromon.MON_DISPLAY, MonRenderer::new);
		EntityRendererRegistry.register(Retromon.WORLD_MON, WorldMonRenderer::new);
		Adventure.register();
		Hud.register();
		Colours.register();
		// The Retromon Look pack (Pokémon-style grass, leaves, flowers, paths): in Resource Packs, off until you switch it on.
		net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer(Retromon.ID).ifPresent(c -> net.fabricmc.fabric.api.resource.ResourceManagerHelper
				.registerBuiltinResourcePack(Retromon.id("retromon_look"), c, Component.literal("Retromon Look"),
						net.fabricmc.fabric.api.resource.ResourcePackActivationType.NORMAL));
		ClientPlayNetworking.registerGlobalReceiver(Net.Push.TYPE, (payload, ctx) -> ClientState.receive(payload.kind(), payload.data()));
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			Sounds.tick();
			Music.tick(mc);
			Adventure.tick(mc);
			ClientState.tick(mc);
			BattleScene.tick(mc);
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, mc) -> {
			Adventure.setOn(false, false);
			ClientState.reset();
			BattleScene.end();
			Music.stop();
			mc.execute(Shaders::leave);
		});
		// Retromon on the title screen, top left (clear of the menu and other mods' buttons).
		ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
			if (!(screen instanceof TitleScreen)) return;
			Button button = Button.builder(Component.literal("Retromon"), b -> Minecraft.getInstance().setScreen(new IntroScreen(screen))).bounds(8, 8, 98, 20)
					.build();
			Screens.getButtons(screen).add(button);
		});
	}
}
