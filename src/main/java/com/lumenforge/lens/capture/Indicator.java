package com.lumenforge.lens.capture;

import com.lumenforge.lens.Lens;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

/**
 * The red dot and clock in the window's top right corner while recording. It's drawn onto the window after the
 * frame has been recorded, so you see it and the video never does. A photo taken while recording flashes it white.
 */
public final class Indicator {
	private static long flashUntil;

	private Indicator() {}

	public static void flash() {
		flashUntil = System.currentTimeMillis() + 350;
	}

	/** Segments a to g of each digit 0-9. */
	private static final int[] DIGITS = {0x3F, 0x06, 0x5B, 0x4F, 0x66, 0x6D, 0x7D, 0x07, 0x7F, 0x6F};

	static void draw(int winW, int winH) {
		if (!Recorder.recording() || !Lens.settings.showDot || winW < 200 || winH < 120) return;
		int u = Math.max(2, winH / 270);
		long ms = Recorder.clockMs();
		boolean paused = Recorder.paused();
		boolean flash = System.currentTimeMillis() < flashUntil;
		boolean on = paused || (System.currentTimeMillis() / 600) % 2 == 0 || flash;
		long s = ms / 1000;
		String clock = (s >= 3600 ? (s / 3600) + ":" : "") + two(s / 60 % 60) + ":" + two(s % 60);

		int digitW = 4 * u, gap = 2 * u, colonW = 2 * u;
		int textW = 0;
		for (char c : clock.toCharArray()) textW += (c == ':' ? colonW : digitW) + gap;
		int dot = 7 * u;
		int pad = 3 * u;
		int w = pad + dot + 3 * u + textW + pad - gap, h = dot + 2 * pad;
		int x0 = winW - w - 4 * u, y0 = 4 * u;

		Gl.save();
		float[] clear = new float[4];
		try (MemoryStack st = MemoryStack.stackPush()) {
			FloatBuffer fb = st.mallocFloat(4);
			GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE, fb);
			fb.get(clear);
		}
		org.lwjgl.opengl.GL30.glBindFramebuffer(org.lwjgl.opengl.GL30.GL_DRAW_FRAMEBUFFER, 0);
		GL11.glEnable(GL11.GL_SCISSOR_TEST);
		try {
			Gl.windowRect(winH, x0, y0, w, h, 0.055F, 0.055F, 0.067F);
			// The dot (a rounded square), red, amber when paused, white for a photo.
			float r = flash ? 1F : paused ? 0.96F : 0.94F, g = flash ? 1F : paused ? 0.70F : 0.27F, b = flash ? 1F : paused ? 0.24F : 0.27F;
			int dx = x0 + pad, dy = y0 + pad;
			if (on) {
				Gl.windowRect(winH, dx + u, dy, dot - 2 * u, dot, r, g, b);
				Gl.windowRect(winH, dx, dy + u, dot, dot - 2 * u, r, g, b);
			}
			int x = dx + dot + 3 * u, y = y0 + pad;
			for (char c : clock.toCharArray()) {
				if (c == ':') {
					Gl.windowRect(winH, x, y + 2 * u, u, u, 0.9F, 0.9F, 0.95F);
					Gl.windowRect(winH, x, y + 4 * u, u, u, 0.9F, 0.9F, 0.95F);
					x += colonW + gap;
					continue;
				}
				digit(winH, x, y, u, DIGITS[c - '0']);
				x += digitW + gap;
			}
		} finally {
			GL11.glDisable(GL11.GL_SCISSOR_TEST);
			GL11.glClearColor(clear[0], clear[1], clear[2], clear[3]);
			Gl.restore();
		}
	}

	/** A seven-segment digit, 4u wide and 7u tall, segments one u thick. */
	private static void digit(int winH, int x, int y, int u, int segs) {
		float c = 0.93F;
		if ((segs & 1) != 0) Gl.windowRect(winH, x, y, 4 * u, u, c, c, c);
		if ((segs & 2) != 0) Gl.windowRect(winH, x + 3 * u, y, u, 4 * u, c, c, c);
		if ((segs & 4) != 0) Gl.windowRect(winH, x + 3 * u, y + 3 * u, u, 4 * u, c, c, c);
		if ((segs & 8) != 0) Gl.windowRect(winH, x, y + 6 * u, 4 * u, u, c, c, c);
		if ((segs & 16) != 0) Gl.windowRect(winH, x, y + 3 * u, u, 4 * u, c, c, c);
		if ((segs & 32) != 0) Gl.windowRect(winH, x, y, u, 4 * u, c, c, c);
		if ((segs & 64) != 0) Gl.windowRect(winH, x, y + 3 * u, 4 * u, u, c, c, c);
	}

	private static String two(long v) {
		return v < 10 ? "0" + v : String.valueOf(v);
	}
}
