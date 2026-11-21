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
	/** When you last came into a world (ms), so Minecraft's pause menu opening as it loads can be closed. */
	private static long joinedAt;

	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(Retromon.NPC, NpcRenderer::new);
		EntityRendererRegistry.register(Retromon.MON_DISPLAY, MonRenderer::new);
		EntityRendererRegistry.register(Retromon.WORLD_MON, WorldMonRenderer::new);
		EntityRendererRegistry.register(Retromon.ROOM_LIGHT, RoomLightRenderer::new);
		Adventure.register();
		Hud.register();
		// The insides of buildings: cut-out (glass, leaves and the clock face show what's behind them).
		net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap.putBlock(Retromon.PROP, net.minecraft.client.renderer.chunk.ChunkSectionLayer.CUTOUT);
		Colours.register();
		// 3D figures on screens (Oak, you, the starters, the Pokédex), drawn without a world.
		net.fabricmc.fabric.api.client.rendering.v1.SpecialGuiElementRegistry.register(ctx -> new Figures.Renderer(ctx.vertexConsumers()));
		net.fabricmc.fabric.api.client.rendering.v1.SpecialGuiElementRegistry.register(ctx -> new Figures.ShadowRenderer(ctx.vertexConsumers()));
		// The Retromon Look pack (Pokémon-style grass, leaves, flowers, paths): in Resource Packs, off until you switch it on.
		net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer(Retromon.ID).ifPresent(c -> net.fabricmc.fabric.api.resource.ResourceManagerHelper
				.registerBuiltinResourcePack(Retromon.id("retromon_look"), c, Component.literal("Retromon Look"),
						net.fabricmc.fabric.api.resource.ResourcePackActivationType.NORMAL));
		ClientPlayNetworking.registerGlobalReceiver(Net.Push.TYPE, (payload, ctx) -> ClientState.receive(payload.kind(), payload.data()));
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			IntroScreen.tickPending();
			Sounds.tick();
			Music.tick(mc);
			Adventure.tick(mc);
			ClientState.tick(mc);
			BattleScene.tick(mc);
			RoomLightRenderer.tick(mc);
			for (int i = 1; i < Adventure.speed && BattleScene.active(); i++) {
				BattleScene.tick(mc);
				BattleScene.tickDisplays();
			}
			Device.tick(mc);
			Hud.tick(mc);
			SlotSnapshot.tick(mc);
			Postcards.tick(mc);
			TownMap.tick(mc);
		});
		// The Town Map keeps up with the world: chunks coming in are looked at for what's changed.
		net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents.CHUNK_LOAD.register((level, chunk) -> TownMap.chunkLoaded(chunk));
		// Your cap, bag and Pokédex on your figure.
		net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback.EVENT.register((type, renderer, helper, ctx) -> {
			if (renderer instanceof net.minecraft.client.renderer.entity.player.AvatarRenderer<?> avatar) helper.register(new GearLayer<>(avatar));
		});
		// Into the world: it fades in from black (the loading screen's black), not all at once.
		ClientPlayConnectionEvents.JOIN.register((handler, sender, mc) -> {
			joinedAt = System.currentTimeMillis();
			Hud.arrive();
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, mc) -> {
			SlotSnapshot.leaving(mc);
			TownMap.leaving();
			Adventure.setOn(false, false);
			ClientState.reset();
			BattleScene.end();
			Music.stop();
			mc.execute(Shaders::leave);
		});
		// Just into the world, Minecraft's own pause menu (it opens when the window wasn't in front as the world
		// loaded) is closed again: you're in the game, not paused.
		ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
			if (screen instanceof net.minecraft.client.gui.screens.PauseScreen && System.currentTimeMillis() - joinedAt < 8000 && client.level != null)
				client.execute(() -> {
					if (client.screen == screen) client.setScreen(null);
				});
		});
		// Retromon on the title screen, top left (clear of the menu and other mods' buttons).
		ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
			if (!(screen instanceof TitleScreen)) return;
			Button button = Button.builder(Component.literal("Retromon"), b -> {
				IntroScreen.minecraftTitle = false;
				Minecraft.getInstance().setScreen(new IntroScreen(screen));
			}).bounds(8, 8, 98, 20)
					.build();
			Screens.getButtons(screen).add(button);
		});
	}
}
