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
			while (RECORD.consumeClick()) Recorder.toggle();
			while (PHOTO.consumeClick()) Photo.take();
			while (PAUSE.consumeClick()) Recorder.togglePause();
			while (MENU.consumeClick()) mc.setScreen(new LensScreen(mc.screen, null));
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

	/** With a menu open, our keys still work (only function keys, so typing is never caught). */
	public static boolean keyInScreen(int action, KeyEvent e) {
		Minecraft mc = Minecraft.getInstance();
		if (action != GLFW.GLFW_PRESS || mc.screen == null || RECORD == null) return false;
		if (mc.screen instanceof net.minecraft.client.gui.screens.options.controls.KeyBindsScreen) return false;
		int key = e.key();
		boolean functionKey = (key >= GLFW.GLFW_KEY_F1 && key <= GLFW.GLFW_KEY_F25) || key == GLFW.GLFW_KEY_PRINT_SCREEN || key == GLFW.GLFW_KEY_PAUSE
				|| key == GLFW.GLFW_KEY_SCROLL_LOCK;
		if (!functionKey) return false;
		if (RECORD.matches(e)) {
			Recorder.toggle();
			return true;
		}
		if (PHOTO.matches(e)) {
			Photo.take();
			return true;
		}
		if (PAUSE.matches(e)) {
			Recorder.togglePause();
			return true;
		}
		return false;
	}
}
