package com.lumenforge.lens.capture;

import net.minecraft.client.Minecraft;

/**
 * Drawing the game bigger than its window: 2x draws 3840x2160 in a 1920x1080 window. The game's sizes (and the
 * GUI scale, so menus look the same) are multiplied, and the bigger picture is shrunk onto the window when shown.
 * Used for photos bigger than the window, and for the "Sharper" video option.
 */
public final class RenderScale {
	private static volatile int k = 1;
	private static int video = 1, photo = 1;

	private RenderScale() {}

	/** How many times bigger the game draws now (1 is normal). Read by the Window mixin. */
	public static int k() {
		return k;
	}

	static void setVideo(int v) {
		video = Math.max(1, v);
		apply();
	}

	static void setPhoto(int v) {
		photo = Math.max(1, v);
		apply();
	}

	private static void apply() {
		int want = Math.max(video, photo);
		if (want == k) return;
		k = want;
		Minecraft.getInstance().resizeDisplay();
	}

	/**
	 * The whole-number scale that gets `screenH` (the window's picture height) to at least `wantH`, at most `max`
	 * and within what the graphics card can draw.
	 */
	static int needed(int wantH, int screenH, int max) {
		if (screenH <= 0 || wantH <= screenH) return 1;
		int n = (int) Math.ceil(wantH / (double) screenH - 0.02);
		int w = Capture.realWidth(), limit = Capture.maxTextureSize();
		while (n > 1 && (long) w * n > limit) n--;
		return Math.max(1, Math.min(max, n));
	}
}
