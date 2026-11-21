package com.lumenforge.lens.capture;

import com.lumenforge.lens.Lens;
import com.lumenforge.lens.Settings;
import com.lumenforge.lens.compat.Flawless;
import com.lumenforge.lens.core.Image;
import com.lumenforge.lens.core.Sizes;
import org.lwjgl.system.MemoryUtil;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

/**
 * Photos, like F2 but at the size you pick. Up to the window's size, the finished frame is taken as you see it and
 * shrunk to fit. Bigger than the window, the game draws a few frames at a whole-number multiple of the window (2x
 * for 4K on a 1080p screen), waiting for the land to finish loading and for the shaders to settle, and the picture is
 * taken from those. Either way it's saved as PNG (or JPG) in the background.
 */
public final class Photo {
	private enum Phase { IDLE, WAITING, WINDOW, MAIN, SAVING }

	/** Frames drawn at the bigger size before the picture's taken (shaders' light and blur settle in these). */
	private static final int WARM_FRAMES = 14;

	private static volatile Phase phase = Phase.IDLE;
	private static int warm;
	private static long readyAt;
	private static boolean big;
	private static int outW, outH;
	private static Sizes.Shape shape;
	private static boolean jpg;
	private static String name;
	private static boolean restore;

	private Photo() {}

	public static boolean busy() {
		return phase != Phase.IDLE;
	}

	/** Milliseconds left on the countdown, or -1 when there's none. */
	public static long countdown() {
		return phase == Phase.WAITING ? Math.max(0, readyAt - System.currentTimeMillis()) : -1;
	}

	/** F12 (press again during the countdown to take it now). */
	public static void take() {
		if (phase == Phase.WAITING) {
			readyAt = 0;
			return;
		}
		if (phase != Phase.IDLE) return;
		if (com.lumenforge.lens.compat.Lightreel.exporting()) {
			Notify.toast("Can't take a photo", "Lightreel is exporting");
			return;
		}
		Settings s = Lens.settings;
		int rw = Capture.realWidth(), rh = Capture.realHeight();
		if (rw <= 0 || rh <= 0) return;
		shape = s.shape;
		jpg = s.jpg;
		int[] size = Sizes.output(s.photoSize, shape, rw, rh);
		outW = size[0];
		outH = size[1];
		int screenH = Sizes.crop(shape, rw, rh)[3];
		int k = RenderScale.needed(outH, screenH, 4);
		// Bigger than the window: the game draws at full size from now until the picture's taken, so the
		// countdown is also time for the shaders' light, clouds and shadows to settle at that size.
		big = k > 1 && RenderScale.k() < k;
		if (big) {
			RenderScale.setPhoto(k);
			Flawless.set(true);
		}
		warm = big ? WARM_FRAMES : 0;
		readyAt = System.currentTimeMillis() + Math.max(s.photoWait * 1000L, big ? 300 : 0);
		phase = Phase.WAITING;
	}

	/** From Capture, just before the frame's shown: runs the countdown, or takes a window-sized photo. */
	static void onFrameEnd(int winW, int winH) {
		if (restore) {
			// After the frame's been shown, so the game never shows a just-resized, empty picture.
			restore = false;
			RenderScale.setPhoto(1);
			Flawless.set(false);
		}
		if (phase == Phase.WAITING) {
			if (warm > 0) warm--;
			if (warm > 0 || System.currentTimeMillis() < readyAt) return;
			name = Capture.stamp() + " (" + outW + "x" + outH + ")";
			if (RenderScale.k() > 1) {
				// Taken from the big picture on the next frame, as it's about to be shown.
				phase = Phase.MAIN;
				return;
			}
			phase = Phase.WINDOW;
		}
		if (phase != Phase.WINDOW) return;
		Gl.save();
		try {
			Gl.readFromWindow();
			read(winW, winH);
		} finally {
			Gl.restore();
		}
	}

	/** From Capture, as the main picture (drawn bigger than the window) is about to be shown. */
	static void onMainPicture(int texture, int w, int h) {
		if (phase != Phase.MAIN) return;
		Gl.save();
		try {
			Gl.readFrom(texture);
			read(w, h);
		} finally {
			Gl.restore();
		}
		if (big) restore = true;
	}

	/** Reads the middle (the photo's shape) of what's bound for reading, then saves it on another thread. */
	private static void read(int srcW, int srcH) {
		int[] c = Sizes.crop(shape, srcW, srcH);
		int cw = c[2], ch = c[3];
		ByteBuffer buf;
		try {
			buf = MemoryUtil.memAlloc(cw * ch * 4);
		} catch (OutOfMemoryError e) {
			phase = Phase.IDLE;
			Notify.toast("Photo not taken", "Not enough memory");
			return;
		}
		Gl.readNow(c[0], srcH - (c[1] + ch), cw, ch, buf);
		phase = Phase.SAVING;
		if (Recorder.recording()) Indicator.flash();
		else com.lumenforge.lens.LensClient.cue(2.0F);
		int ow = outW, oh = outH;
		boolean asJpg = jpg;
		String n = name;
		Path dir = Lens.photos();
		Thread t = new Thread(() -> save(buf, cw, ch, ow, oh, asJpg, dir, n), "Lightlens photo");
		t.setDaemon(true);
		t.start();
	}

	private static void save(ByteBuffer buf, int w, int h, int ow, int oh, boolean asJpg, Path dir, String n) {
		try {
			int[] px;
			try {
				px = Image.fromGl(buf, w, h);
			} finally {
				MemoryUtil.memFree(buf);
			}
			px = Image.resize(px, w, h, ow, oh);
			BufferedImage img = new BufferedImage(ow, oh, BufferedImage.TYPE_INT_RGB);
			img.setRGB(0, 0, ow, oh, px, 0, ow);
			Files.createDirectories(dir);
			Path file = Capture.unique(dir, n, asJpg ? ".jpg" : ".png");
			Path part = file.resolveSibling(file.getFileName() + ".part");
			if (asJpg) writeJpg(img, part);
			else ImageIO.write(img, "png", part.toFile());
			Files.move(part, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
			Lens.LOG.info("Photo saved: {} ({}x{})", file, ow, oh);
			// While recording, no toast (it would be in the video): the dot flashed instead.
			if (!Recorder.recording()) Notify.saved("Photo saved", file, ow + "x" + oh);
		} catch (Exception e) {
			Lens.LOG.error("Saving the photo failed", e);
			Notify.toast("Photo not saved", e.getMessage());
		} finally {
			phase = Phase.IDLE;
		}
	}

	private static void writeJpg(BufferedImage img, Path file) throws java.io.IOException {
		Iterator<ImageWriter> it = ImageIO.getImageWritersByFormatName("jpg");
		if (!it.hasNext()) throw new java.io.IOException("no JPG writer");
		ImageWriter w = it.next();
		ImageWriteParam p = w.getDefaultWriteParam();
		p.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		p.setCompressionQuality(0.95F);
		try (ImageOutputStream out = ImageIO.createImageOutputStream(file.toFile())) {
			w.setOutput(out);
			w.write(null, new IIOImage(img, null, null), p);
		} finally {
			w.dispose();
		}
	}
}
