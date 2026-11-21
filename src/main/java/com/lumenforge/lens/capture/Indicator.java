package com.lumenforge.lens.capture;

import com.lumenforge.lens.Lens;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

/**
 * The red dot and clock in the window's top right corner while recording, and a photo's countdown. They're drawn
 * onto the window after the frame has been recorded, so you see them and the video and photos never do. A photo
 * taken while recording flashes the dot white.
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
		boolean rec = Recorder.recording() && Lens.settings.showDot;
		long count = Photo.countdown();
		if ((!rec && count < 0) || winW < 200 || winH < 120) return;
		int u = Math.max(2, winH / 216);
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
			int y = 4 * u;
			if (rec) y = recBox(winW, winH, u, y) + 2 * u;
			if (count >= 0) photoBox(winW, winH, u, y, count);
		} finally {
			GL11.glDisable(GL11.GL_SCISSOR_TEST);
			GL11.glClearColor(clear[0], clear[1], clear[2], clear[3]);
			Gl.restore();
		}
	}

	/** The red dot and the recording's clock. Returns the box's bottom. */
	private static int recBox(int winW, int winH, int u, int y0) {
		long ms = Recorder.clockMs();
		boolean paused = Recorder.paused();
		boolean flash = System.currentTimeMillis() < flashUntil;
		boolean on = paused || (System.currentTimeMillis() / 600) % 2 == 0 || flash;
		long s = ms / 1000;
		String clock = (s >= 3600 ? (s / 3600) + ":" : "") + two(s / 60 % 60) + ":" + two(s % 60);
		int pad = 3 * u, dot = 7 * u;
		int w = pad + dot + 3 * u + textWidth(clock, u) + pad, h = dot + 2 * pad;
		int x0 = winW - w - 4 * u;
		Gl.windowRect(winH, x0, y0, w, h, 0.055F, 0.055F, 0.067F);
		Gl.windowRect(winH, x0, y0 + h - Math.max(1, u / 2), w, Math.max(1, u / 2), 0.94F, 0.27F, 0.27F);
		// The dot (a rounded square), red, amber when paused, white for a photo.
		float r = flash ? 1F : paused ? 0.96F : 0.94F, g = flash ? 1F : paused ? 0.70F : 0.27F, b = flash ? 1F : paused ? 0.24F : 0.27F;
		int dx = x0 + pad, dy = y0 + pad;
		if (on) {
			Gl.windowRect(winH, dx + u, dy, dot - 2 * u, dot, r, g, b);
			Gl.windowRect(winH, dx, dy + u, dot, dot - 2 * u, r, g, b);
		}
		text(winH, clock, dx + dot + 3 * u, y0 + pad, u);
		return y0 + h;
	}

	/** A photo's countdown: a little camera and the seconds left. */
	private static void photoBox(int winW, int winH, int u, int y0, long ms) {
		String secs = String.valueOf((ms + 999) / 1000);
		int pad = 3 * u, cam = 9 * u;
		int w = pad + cam + 3 * u + textWidth(secs, u) + pad, h = 7 * u + 2 * pad;
		int x0 = winW - w - 4 * u;
		Gl.windowRect(winH, x0, y0, w, h, 0.055F, 0.055F, 0.067F);
		Gl.windowRect(winH, x0, y0 + h - Math.max(1, u / 2), w, Math.max(1, u / 2), 0.08F, 0.72F, 0.65F);
		int cx = x0 + pad, cy = y0 + pad + u;
		// Camera: body, lens, and the shutter button on top.
		Gl.windowRect(winH, cx, cy + u, cam, 5 * u, 0.93F, 0.93F, 0.95F);
		Gl.windowRect(winH, cx + 2 * u, cy - u, 2 * u, 2 * u, 0.93F, 0.93F, 0.95F);
		Gl.windowRect(winH, cx + 3 * u, cy + 2 * u, 3 * u, 3 * u, 0.055F, 0.055F, 0.067F);
		text(winH, secs, cx + cam + 3 * u, y0 + pad, u);
	}

	private static int textWidth(String t, int u) {
		int w = 0;
		for (char c : t.toCharArray()) w += (c == ':' ? 2 * u : 4 * u) + 2 * u;
		return w - 2 * u;
	}

	private static void text(int winH, String t, int x, int y, int u) {
		for (char c : t.toCharArray()) {
			if (c == ':') {
				Gl.windowRect(winH, x, y + 2 * u, u, u, 0.9F, 0.9F, 0.95F);
				Gl.windowRect(winH, x, y + 4 * u, u, u, 0.9F, 0.9F, 0.95F);
				x += 4 * u;
				continue;
			}
			digit(winH, x, y, u, DIGITS[c - '0']);
			x += 6 * u;
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
