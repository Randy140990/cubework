package com.lumenforge.lens.capture;

import com.lumenforge.lens.Lens;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.stream.Stream;

/** Where the mixins come in each frame, and small helpers shared by video and photos. */
public final class Capture {
	private static int maxTex;

	private Capture() {}

	/** From the Window mixin, just before the finished frame goes to the screen. */
	public static void onFrameEnd(int winW, int winH) {
		try {
			Photo.onFrameEnd(winW, winH);
			Recorder.onFrameEnd(winW, winH);
			Indicator.draw(winW, winH);
		} catch (Throwable t) {
			Lens.LOG.error("Capture failed", t);
			if (Recorder.recording()) Recorder.stop("Recording stopped: " + t);
		}
	}

	/**
	 * From the RenderTarget mixin, as the game's main picture is about to be shown. True when we've shown it
	 * ourselves (shrunk onto the window, because the game's drawing bigger than it), so the game's own copy is skipped.
	 */
	public static boolean beforePresent(RenderTarget main) {
		GpuTexture tex = main.getColorTexture();
		if (!(tex instanceof GlTexture gl)) return false;
		int id = gl.glId();
		try {
			Recorder.onMainPicture(id, main.width, main.height);
			Photo.onMainPicture(id, main.width, main.height);
		} catch (Throwable t) {
			Lens.LOG.error("Capture failed", t);
			if (Recorder.recording()) Recorder.stop("Recording stopped: " + t);
		}
		if (RenderScale.k() <= 1) return false;
		int ww = realWidth(), wh = realHeight();
		if (ww <= 0 || wh <= 0) return false;
		GlStateManager._disableScissorTest();
		GlStateManager._depthMask(true);
		GlStateManager._colorMask(true, true, true, true);
		Gl.save();
		try {
			Gl.readFrom(id);
			Gl.blitToWindow(main.width, main.height, ww, wh);
		} finally {
			Gl.restore();
		}
		return true;
	}

	public static int realWidth() {
		return ((LensWindow) (Object) Minecraft.getInstance().getWindow()).lightlens$realWidth();
	}

	public static int realHeight() {
		return ((LensWindow) (Object) Minecraft.getInstance().getWindow()).lightlens$realHeight();
	}

	/** The biggest picture the graphics card can draw (asked once, on the render thread). */
	static int maxTextureSize() {
		if (maxTex == 0) {
			try {
				maxTex = GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE);
			} catch (Throwable t) {
				maxTex = 8192;
			}
			if (maxTex <= 0) maxTex = 8192;
		}
		return maxTex;
	}

	/** Free bytes where `dir` (or its nearest existing parent) is, or -1 if it can't be told. */
	public static long freeSpace(Path dir) {
		try {
			Path p = dir.toAbsolutePath();
			while (p != null && !Files.exists(p)) p = p.getParent();
			return p == null ? -1 : Files.getFileStore(p).getUsableSpace();
		} catch (Exception e) {
			return -1;
		}
	}

	/** "Lightlens 2026-11-21 at 09.30.12" */
	static String stamp() {
		return Lens.NAME + " " + new SimpleDateFormat("yyyy-MM-dd 'at' HH.mm.ss").format(new Date());
	}

	/** dir/name.ext, or dir/name (2).ext and so on if that's taken. */
	static Path unique(Path dir, String name, String ext) {
		Path p = dir.resolve(name + ext);
		for (int i = 2; Files.exists(p) || Files.exists(p.resolveSibling(p.getFileName() + ".part")); i++) p = dir.resolve(name + " (" + i + ")" + ext);
		return p;
	}

	static void deleteTree(Path dir) {
		if (dir == null || !Files.exists(dir)) return;
		try (Stream<Path> s = Files.walk(dir)) {
			s.sorted(Comparator.reverseOrder()).forEach(p -> {
				try {
					Files.deleteIfExists(p);
				} catch (IOException ignored) {
				}
			});
		} catch (IOException ignored) {
		}
	}
}
