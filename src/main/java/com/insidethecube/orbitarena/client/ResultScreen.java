package com.insidethecube.orbitarena.client;

import com.insidethecube.orbitarena.net.Payloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * After a match: how it went, and what next. Go again with the same settings (straight back onto the
 * pedestal), change the settings first, or leave the arena.
 */
public class ResultScreen extends Screen {
	/** A result waiting to be shown (after the title has had a moment), and how many ticks until then. */
	private static Payloads.Result pending;
	private static int delay;

	private final Payloads.Result result;

	public ResultScreen(Payloads.Result result) {
		super(Component.literal(result.won() ? "Victory" : "Defeat"));
		this.result = result;
	}

	/** The server says the match is over: show this in two seconds. */
	static void show(Payloads.Result result) {
		pending = result;
		delay = 40;
	}

	/** Left the arena: nothing to show. */
	static void cancel() {
		pending = null;
	}

	static void tick(Minecraft mc) {
		if (pending == null || --delay > 0) return;
		Payloads.Result r = pending;
		pending = null;
		if (mc.player != null) mc.setScreen(new ResultScreen(r));
	}

	@Override
	protected void init() {
		int w = 200, x = this.width / 2 - w / 2, y = this.height / 2 + 6;
		addRenderableWidget(Button.builder(Component.literal("Go again"), b -> {
			ClientPlayNetworking.send(new Payloads.Start(ArenaScreen.settings()));
			this.minecraft.setScreen(null);
		}).bounds(x, y, w, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Change settings"), b -> this.minecraft.setScreen(new ArenaScreen(this)))
				.bounds(x, y + 24, w, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Leave arena"), b -> {
			ClientPlayNetworking.send(new Payloads.Leave());
			this.minecraft.setScreen(null);
		}).bounds(x, y + 48, w, 20).build());
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
		super.render(g, mouseX, mouseY, partial);
		int cx = this.width / 2, y = this.height / 2 - 58;
		Component head = Component.literal(result.won() ? "VICTORY" : "DEFEAT").withStyle(result.won() ? ChatFormatting.GOLD : ChatFormatting.RED, ChatFormatting.BOLD);
		g.drawCenteredString(this.font, head, cx, y, 0xFFFFFFFF);
		g.drawCenteredString(this.font, Component.literal(result.reason()), cx, y + 14, 0xFFE0E6FF);
		g.drawCenteredString(this.font, Component.literal(result.summary()), cx, y + 26, 0xFFA0B4FF);
		g.drawCenteredString(this.font, Component.literal("Go again: ").append(ArenaScreen.describeSettings()), cx, y + 44, 0xFF8090B0);
	}

	/** A choice has to be made (Leave arena is one). */
	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	/** The crowd keeps cheering behind it. */
	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
