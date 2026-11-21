package com.lumenforge.lens.capture;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;

import java.nio.ByteBuffer;

/**
 * The few OpenGL calls Lightlens makes itself, all on the render thread: copying the finished picture (the window's
 * back buffer, or the game's main picture) into a picture of the output size, and reading it back without waiting
 * for the graphics card (pixel buffers, collected a frame or two later).
 *
 * Everything we bind is put back as it was, so Minecraft's own record of what's bound stays true.
 */
public final class Gl {
	private Gl() {}

	/** What was bound before we started, put back by restore(). */
	private static int prevRead, prevDraw, prevPack;
	private static boolean prevScissor;

	static void save() {
		prevRead = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
		prevDraw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
		prevPack = GL11.glGetInteger(GL21.GL_PIXEL_PACK_BUFFER_BINDING);
		prevScissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
		if (prevScissor) GL11.glDisable(GL11.GL_SCISSOR_TEST);
	}

	static void restore() {
		GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, prevPack);
		GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, prevRead);
		GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, prevDraw);
		if (prevScissor) GL11.glEnable(GL11.GL_SCISSOR_TEST);
	}

	/** A colour picture of our own, with a framebuffer to draw into it and read from it. */
	static final class Target {
		int tex, fbo, w, h;

		void ensure(int width, int height) {
			if (tex != 0 && w == width && h == height) return;
			free();
			w = width;
			h = height;
			int prevTex = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
			tex = GL11.glGenTextures();
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
			GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, w, h, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevTex);
			int prev = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
			fbo = GL30.glGenFramebuffers();
			GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, fbo);
			GL30.glFramebufferTexture2D(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, tex, 0);
			GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, prev);
		}

		void free() {
			if (fbo != 0) GL30.glDeleteFramebuffers(fbo);
			if (tex != 0) GL11.glDeleteTextures(tex);
			fbo = tex = 0;
		}
	}

	/** A framebuffer we point at someone else's picture (the game's main one) to read from it. */
	private static int readFbo;

	/** Binds a texture to read from, as the read framebuffer. Returns that framebuffer. */
	static int readFrom(int texture) {
		if (readFbo == 0) readFbo = GL30.glGenFramebuffers();
		GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFbo);
		GL30.glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, texture, 0);
		GL11.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);
		return readFbo;
	}

	/** The window's back buffer (the finished frame, about to be shown) as the read framebuffer. */
	static void readFromWindow() {
		GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, 0);
		GL11.glReadBuffer(GL11.GL_BACK);
	}

	/**
	 * Copies part of what's bound for reading (x, y, w, h from the top left of a picture `srcH` tall) into the
	 * target, scaled to fill it and turned the right way up (OpenGL counts rows from the bottom).
	 */
	static void blitInto(Target t, int srcH, int x, int y, int w, int h) {
		int y0 = srcH - (y + h);
		GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, t.fbo);
		GL30.glBlitFramebuffer(x, y0, x + w, y0 + h, 0, t.h, t.w, 0, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_LINEAR);
	}

	/** Scales what's bound for reading (w x h) onto the window (ww x wh): Minecraft drawing bigger than the window. */
	static void blitToWindow(int w, int h, int ww, int wh) {
		GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, 0);
		GL30.glBlitFramebuffer(0, 0, w, h, 0, 0, ww, wh, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_LINEAR);
	}

	/** Reads w x h RGBA from what's bound for reading into memory now (waits for the graphics card: photos only). */
	static void readNow(int x, int y, int w, int h, ByteBuffer into) {
		GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, 0);
		packDefaults();
		GL11.glReadPixels(x, y, w, h, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, into);
	}

	static void packDefaults() {
		GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 4);
		GL11.glPixelStorei(GL11.GL_PACK_ROW_LENGTH, 0);
		GL11.glPixelStorei(GL11.GL_PACK_SKIP_ROWS, 0);
		GL11.glPixelStorei(GL11.GL_PACK_SKIP_PIXELS, 0);
	}

	/**
	 * Reading frames back without stalling: each frame is read into one of a few pixel buffers, and a frame or two
	 * later, when the graphics card says it's done, copied out.
	 */
	static final class Readback {
		private final int count;
		private final int[] pbo;
		private final long[] fence;
		private final long[] stamp;
		private int w, h, head, inFlight;

		Readback(int count) {
			this.count = count;
			pbo = new int[count];
			fence = new long[count];
			stamp = new long[count];
		}

		int bytes() {
			return w * h * 4;
		}

		void ensure(int width, int height) {
			if (pbo[0] != 0 && w == width && h == height) return;
			free();
			w = width;
			h = height;
			for (int i = 0; i < count; i++) {
				pbo[i] = GL15.glGenBuffers();
				GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, pbo[i]);
				GL15.glBufferData(GL21.GL_PIXEL_PACK_BUFFER, (long) w * h * 4, GL15.GL_STREAM_READ);
			}
		}

		/** True when every buffer is still waiting on the graphics card. */
		boolean full() {
			return inFlight >= count;
		}

		/** Starts reading the target (bound for reading) into the next buffer. */
		void submit(long time) {
			int i = (head + inFlight) % count;
			GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, pbo[i]);
			packDefaults();
			GL11.glReadPixels(0, 0, w, h, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, 0L);
			fence[i] = GL32.glFenceSync(GL32.GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
			stamp[i] = time;
			inFlight++;
		}

		interface Sink {
			/** Takes a finished frame (it's only valid during the call). */
			void accept(ByteBuffer rgba, long time);
		}

		/** Hands over the frames that are done, oldest first. With `wait`, waits for all of them (stopping). */
		void collect(Sink sink, boolean wait) {
			while (inFlight > 0) {
				int i = head;
				int r = GL32.glClientWaitSync(fence[i], wait ? GL32.GL_SYNC_FLUSH_COMMANDS_BIT : 0, wait ? 2_000_000_000L : 0L);
				if (r != GL32.GL_ALREADY_SIGNALED && r != GL32.GL_CONDITION_SATISFIED) {
					if (!wait) return;
				}
				GL32.glDeleteSync(fence[i]);
				fence[i] = 0;
				GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, pbo[i]);
				ByteBuffer data = GL30.glMapBufferRange(GL21.GL_PIXEL_PACK_BUFFER, 0, (long) w * h * 4, GL30.GL_MAP_READ_BIT);
				if (data != null) {
					try {
						sink.accept(data, stamp[i]);
					} finally {
						GL15.glUnmapBuffer(GL21.GL_PIXEL_PACK_BUFFER);
					}
				}
				head = (head + 1) % count;
				inFlight--;
			}
		}

		void free() {
			for (int i = 0; i < count; i++) {
				if (fence[i] != 0) GL32.glDeleteSync(fence[i]);
				if (pbo[i] != 0) GL15.glDeleteBuffers(pbo[i]);
				fence[i] = 0;
				pbo[i] = 0;
			}
			head = inFlight = 0;
		}
	}

	/** A filled rectangle straight onto the window (x, y from the top left), outside anything recorded. */
	static void windowRect(int winH, int x, int y, int w, int h, float r, float g, float b) {
		GL11.glScissor(x, winH - (y + h), w, h);
		GL11.glClearColor(r, g, b, 1F);
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
	}
}
