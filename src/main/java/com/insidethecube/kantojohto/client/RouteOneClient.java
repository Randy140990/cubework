package com.insidethecube.kantojohto.client;

import com.insidethecube.kantojohto.RouteOne;
import com.insidethecube.kantojohto.game.Net;
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

/** Route One on your screen: the camera, the keys, the people and Pokémon, the screens, and the title button. */
public class RouteOneClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(RouteOne.NPC, NpcRenderer::new);
		EntityRendererRegistry.register(RouteOne.MON_DISPLAY, MonRenderer::new);
		Adventure.register();
		Hud.register();
		ClientPlayNetworking.registerGlobalReceiver(Net.Push.TYPE, (payload, ctx) -> ClientState.receive(payload.kind(), payload.data()));
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			Sounds.tick();
			Adventure.tick(mc);
			ClientState.tick(mc);
			BattleScene.tick(mc);
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, mc) -> {
			Adventure.setOn(false, false);
			ClientState.reset();
			BattleScene.end();
		});
		// Route One on the title screen, top left (clear of the menu and other mods' buttons).
		ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
			if (!(screen instanceof TitleScreen)) return;
			Button button = Button.builder(Component.literal("Route One"), b -> Minecraft.getInstance().setScreen(new IntroScreen(screen))).bounds(8, 8, 98, 20)
					.build();
			Screens.getButtons(screen).add(button);
		});
	}
}
