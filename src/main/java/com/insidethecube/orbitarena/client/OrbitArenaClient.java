package com.insidethecube.orbitarena.client;

import com.insidethecube.orbitarena.OrbitArena;
import com.insidethecube.orbitarena.net.Payloads;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;

/** The client: the pause-menu button, the setup screen, the crowd, the sky, the fighters and the fades. */
public class OrbitArenaClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(OrbitArena.FIGHTER, FighterRenderer::new);
		EntityRendererRegistry.register(OrbitArena.FAN, FanRenderer::new);
		EntityModelLayerRegistry.registerModelLayer(FanModel.LAYER, FanModel::createLayer);

		ClientPlayNetworking.registerGlobalReceiver(Payloads.State.TYPE, (p, ctx) -> {
			if (p.inArena()) Crowd.setCentre(p.centreX());
		});
		ClientPlayNetworking.registerGlobalReceiver(Payloads.Cheer.TYPE, (p, ctx) -> Crowd.cheer(p.strength()));
		ClientPlayNetworking.registerGlobalReceiver(Payloads.Fade.TYPE, (p, ctx) -> Fade.start(p.out(), p.hold(), p.in()));
		ClientTickEvents.END_CLIENT_TICK.register(Crowd::tick);
		HudElementRegistry.addLast(OrbitArena.id("fade"), Fade::render);

		// PvP Arena on the pause menu (Leave Arena while you're in it).
		ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
			if (!(screen instanceof PauseScreen)) return;
			boolean inArena = client.level != null && client.level.dimension() == OrbitArena.ARENA;
			Button button = Button.builder(Component.literal(inArena ? "Leave Arena" : "PvP Arena"), b -> {
				if (inArena) {
					ClientPlayNetworking.send(new Payloads.Leave());
					Minecraft.getInstance().setScreen(null);
				} else {
					Minecraft.getInstance().setScreen(new ArenaScreen(screen));
				}
			}).bounds(8, 8, 98, 20).build();
			Screens.getButtons(screen).add(button);
		});
	}
}
