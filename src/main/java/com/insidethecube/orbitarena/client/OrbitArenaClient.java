package com.insidethecube.orbitarena.client;

import com.insidethecube.orbitarena.OrbitArena;
import com.insidethecube.orbitarena.block.ArenaBlocks;
import com.insidethecube.orbitarena.net.Payloads;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.network.chat.Component;

/**
 * The client: the pause-menu button, the setup screen, the crowd, the sky, the fighters, the fly-round, the
 * bridge's score and crystals, celebrations and the fades.
 */
public class OrbitArenaClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(OrbitArena.FIGHTER, FighterRenderer::new);
		EntityRendererRegistry.register(OrbitArena.FAN, FanRenderer::new);
		EntityRendererRegistry.register(OrbitArena.CRYSTAL, CrystalRenderer::new);
		EntityRendererRegistry.register(OrbitArena.ROCK, RockRenderer::new);
		for (net.minecraft.world.level.block.Block b : ArenaBlocks.TRANSLUCENT) BlockRenderLayerMap.putBlock(b, ChunkSectionLayer.TRANSLUCENT);
		ArenaSky.load();
		EntityModelLayerRegistry.registerModelLayer(FanModel.LAYER, FanModel::createLayer);
		Emotes.register();

		ClientPlayNetworking.registerGlobalReceiver(Payloads.State.TYPE, (p, ctx) -> {
			if (p.inArena()) {
				Crowd.setArena(p.mode(), p.centreX(), p.centreZ());
				if (p.mode() != Payloads.State.ARENA) {
					Crystals.set(p.centreX(), p.centreZ());
					Rocks.set(p.centreX(), p.centreZ());
				}
				else BridgeHud.clear();
			} else {
				ResultScreen.cancel();
				BridgeHud.clear();
				Cinematic.stop();
			}
			Crowd.setFighting(p.inArena() && p.fighting());
		});
		// A match is over: go again, change the settings or leave.
		ClientPlayNetworking.registerGlobalReceiver(Payloads.Result.TYPE, (p, ctx) -> ResultScreen.show(p));
		ClientPlayNetworking.registerGlobalReceiver(Payloads.Cheer.TYPE, (p, ctx) -> Crowd.react(p.kind(), p.strength()));
		ClientPlayNetworking.registerGlobalReceiver(Payloads.Fade.TYPE, (p, ctx) -> Fade.start(p.out(), p.hold(), p.in()));
		ClientPlayNetworking.registerGlobalReceiver(Payloads.Cinematic.TYPE, (p, ctx) -> Cinematic.start(p));
		ClientPlayNetworking.registerGlobalReceiver(Payloads.Celebrate.TYPE, (p, ctx) -> Emotes.celebrate(p.ticks()));
		ClientPlayNetworking.registerGlobalReceiver(Payloads.EmoteOf.TYPE, (p, ctx) -> Emotes.play(p.entityId(), p.emote()));
		ClientPlayNetworking.registerGlobalReceiver(Payloads.BridgeScore.TYPE, (p, ctx) -> {
			BridgeHud.set(p);
			Crystals.score(p.blue(), p.red());
		});
		ClientPlayNetworking.registerGlobalReceiver(Payloads.Shatter.TYPE, (p, ctx) -> Crystals.shatter(p.team(), p.pillar()));
		ClientTickEvents.END_CLIENT_TICK.register(ResultScreen::tick);
		ClientTickEvents.END_CLIENT_TICK.register(Crowd::tick);
		ClientTickEvents.END_CLIENT_TICK.register(Cinematic::tick);
		ClientTickEvents.END_CLIENT_TICK.register(Emotes::tick);
		ClientTickEvents.END_CLIENT_TICK.register(Crystals::tick);
		ClientTickEvents.END_CLIENT_TICK.register(Rocks::tick);
		ClientTickEvents.END_CLIENT_TICK.register(ArenaSky::upload);
		ClientTickEvents.END_CLIENT_TICK.register(ArenaSky::tick);
		Cinematic.hideHud();
		HudElementRegistry.addLast(OrbitArena.id("bridge_score"), BridgeHud::render);
		HudElementRegistry.addLast(OrbitArena.id("cinematic"), Cinematic::render);
		HudElementRegistry.addLast(OrbitArena.id("fade"), Fade::render);

		// Orbit Arena at the top right of the pause menu (Leave Arena while you're in it), clear of other mods'
		// buttons at the top left.
		ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
			if (!(screen instanceof PauseScreen)) return;
			boolean inArena = client.level != null && client.level.dimension() == OrbitArena.ARENA;
			Button button = Button.builder(Component.literal(inArena ? "Leave Arena" : "Orbit Arena"), b -> {
				if (inArena) {
					ClientPlayNetworking.send(new Payloads.Leave());
					Minecraft.getInstance().setScreen(null);
				} else {
					Minecraft.getInstance().setScreen(new ArenaScreen(screen));
				}
			}).bounds(w - 8 - 98, 8, 98, 20).build();
			Screens.getButtons(screen).add(button);
		});
	}
}
