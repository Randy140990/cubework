package com.lumenforge.lens;

import com.lumenforge.lens.audio.GameSound;
import com.lumenforge.lens.capture.Photo;
import com.lumenforge.lens.capture.Recorder;
import com.lumenforge.lens.capture.Rescue;
import com.lumenforge.lens.core.Ffmpeg;
import com.lumenforge.lens.ui.LensScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Keys (F10 record, F12 photo), the button in the pause menu, and the per-tick work. */
public class LensClient implements ClientModInitializer {
	public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Lens.id("lightlens"));
	public static KeyMapping RECORD, PHOTO, PAUSE, MENU;

	@Override
	public void onInitializeClient() {
		Lens.load();
		RECORD = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.lightlens.record", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F10, CATEGORY));
		PHOTO = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.lightlens.photo", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F12, CATEGORY));
		PAUSE = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.lightlens.pause", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, CATEGORY));
		MENU = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.lightlens.menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, CATEGORY));

		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			// Only in a world (not on the title screen or the world list).
			boolean inGame = mc.level != null && mc.player != null;
			// One press, one action: a key held down repeats, which would start and then stop at once.
			if (pressed(RECORD, 0) && inGame) Recorder.toggle();
			if (pressed(PHOTO, 1) && inGame) Photo.take();
			if (pressed(PAUSE, 2) && inGame) Recorder.togglePause();
			while (MENU.consumeClick()) mc.setScreen(new LensScreen(mc.screen, null));
			if (!inGame && Recorder.recording()) Recorder.stop("Stopped and saved: you left the world");
			GameSound.tick();
		});

		// "Lightlens" at the top left of the pause menu.
		ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
			if (!(screen instanceof PauseScreen pause) || !pause.showsPauseMenu()) return;
			Button b = Button.builder(Component.literal(Recorder.recording() ? "● Lightlens" : "Lightlens"), x -> client.setScreen(new LensScreen(screen, null)))
					.bounds(8, 8, 90, 20).build();
			Screens.getButtons(screen).add(b);
		});

		// FFmpeg and the graphics card's encoder are checked in the background as the game starts, so F10 is
		// ready straight away; then anything a crash left unfinished is saved.
		ClientLifecycleEvents.CLIENT_STARTED.register(mc -> {
			Thread t = new Thread(() -> {
				String exe = Ffmpeg.findNow();
				if (exe != null) Ffmpeg.probe(exe);
				Rescue.runAsync();
			}, "Lightlens start-up checks");
			t.setDaemon(true);
			t.start();
		});
		ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> Recorder.onShutdown());
	}

	/** When each of our keys last did something (record, photo, pause). */
	private static final long[] LAST = new long[3];

	/** True once per press: the key's clicks this tick are taken, and repeats within a second are ignored. */
	private static boolean pressed(KeyMapping key, int which) {
		boolean clicked = false;
		while (key.consumeClick()) clicked = true;
		return clicked && fresh(which);
	}

	private static boolean fresh(int which) {
		long now = System.currentTimeMillis();
		if (now - LAST[which] < 1000) return false;
		LAST[which] = now;
		return true;
	}

	/** A short sound you hear (never in the video): higher to start, lower to stop, a click for a photo. */
	public static void cue(float pitch) {
		try {
			Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
					net.minecraft.sounds.SoundEvents.NOTE_BLOCK_PLING.value(), pitch, 0.6F));
		} catch (Throwable ignored) {
		}
	}

	/** With a menu open, our keys still work (only function keys, so typing is never caught). */
	public static boolean keyInScreen(int action, KeyEvent e) {
		Minecraft mc = Minecraft.getInstance();
		if (action != GLFW.GLFW_PRESS || mc.screen == null || RECORD == null || mc.level == null || mc.player == null) return false;
		if (mc.screen instanceof net.minecraft.client.gui.screens.options.controls.KeyBindsScreen) return false;
		int key = e.key();
		boolean functionKey = (key >= GLFW.GLFW_KEY_F1 && key <= GLFW.GLFW_KEY_F25) || key == GLFW.GLFW_KEY_PRINT_SCREEN || key == GLFW.GLFW_KEY_PAUSE
				|| key == GLFW.GLFW_KEY_SCROLL_LOCK;
		if (!functionKey) return false;
		if (RECORD.matches(e)) {
			if (fresh(0)) Recorder.toggle();
			return true;
		}
		if (PHOTO.matches(e)) {
			if (fresh(1)) Photo.take();
			return true;
		}
		if (PAUSE.matches(e)) {
			if (fresh(2)) Recorder.togglePause();
			return true;
		}
		return false;
	}
}
