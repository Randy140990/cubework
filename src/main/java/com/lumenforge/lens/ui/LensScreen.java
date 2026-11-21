package com.lumenforge.lens.ui;

import com.lumenforge.lens.Lens;
import com.lumenforge.lens.LensClient;
import com.lumenforge.lens.Settings;
import com.lumenforge.lens.capture.Capture;
import com.lumenforge.lens.capture.Photo;
import com.lumenforge.lens.capture.Recorder;
import com.lumenforge.lens.compat.Shaders;
import com.lumenforge.lens.core.Ffmpeg;
import com.lumenforge.lens.core.Mic;
import com.lumenforge.lens.core.Quality;
import com.lumenforge.lens.core.Sizes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The Lightlens screen, in the LumenForge look: what size and quality to record at, where it goes, photo size, and
 * whether FFmpeg and the graphics card's encoder are ready. One screen, everything on it, nothing hidden.
 */
public final class LensScreen extends Screen {
	// LumenForge colours.
	static final int BG = 0xF00E0E11, PANEL = 0xFF141418, EDGE = 0xFF26262E, TEXT = 0xFFF4F4F6, MUTED = 0xFF9A9AA6, DIM = 0xFF62626E;
	static final int VIOLET = 0xFF8B5CF6, BLUE = 0xFF3B82F6, TEAL = 0xFF14B8A6, RED = 0xFFEF4444, AMBER = 0xFFF59E0B, GREEN = 0xFF22C55E;

	private final Screen parent;
	private String message;
	private final List<Hit> hits = new ArrayList<>();
	private static volatile String install;
	private static volatile boolean choosing;

	private record Hit(int x, int y, int w, int h, Runnable action) {
		boolean in(double mx, double my) {
			return mx >= x && my >= y && mx < x + w && my < y + h;
		}
	}

	public LensScreen(Screen parent, String message) {
		super(Component.literal(Lens.NAME));
		this.parent = parent;
		this.message = message;
	}

	@Override
	protected void init() {
		if (Ffmpeg.find() != null) Ffmpeg.probeAsync();
	}

	@Override
	public void tick() {
		if (Ffmpeg.find() != null && !Ffmpeg.probed()) Ffmpeg.probeAsync();
	}

	@Override
	public void onClose() {
		Lens.save();
		minecraft.setScreen(parent);
	}

	@Override
	public boolean isPauseScreen() {
		return !Recorder.recording();
	}

	@Override
	public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
		g.fill(0, 0, width, height, minecraft.level == null ? 0xFF0E0E11 : 0x99000000);
	}

	// ---------------------------------------------------------------- drawing

	@Override
	public void render(GuiGraphics g, int mx, int my, float partial) {
		super.render(g, mx, my, partial);
		hits.clear();
		Settings s = Lens.settings;
		int pw = Math.min(470, width - 16), ph = Math.min(262, height - 12);
		int x0 = (width - pw) / 2, y0 = (height - ph) / 2;
		boolean tight = ph < 250;
		int row = tight ? 13 : 15;

		g.fill(x0, y0, x0 + pw, y0 + ph, BG);
		outline(g, x0, y0, pw, ph, EDGE);
		gradient(g, x0, y0, pw, 2);

		// Title: Lightlens, with the gradient full stop.
		int tx = x0 + 10, ty = y0 + 9;
		g.drawString(font, "Lightlens", tx, ty, TEXT, false);
		int dotX = tx + font.width("Lightlens");
		g.fill(dotX + 1, ty + 6, dotX + 3, ty + 8, TEAL);
		g.drawString(font, "Record and take photos as you play", dotX + 8, ty, MUTED, false);
		String rec = recLine();
		if (rec != null) g.drawString(font, rec, x0 + pw - 10 - font.width(rec), ty, Recorder.recording() ? RED : AMBER, false);
		int y = y0 + 24;
		if (message != null) {
			g.drawString(font, message, tx, y, VIOLET, false);
			y += 12;
		}

		int colW = (pw - 30) / 2;
		int lx = x0 + 10, rx = lx + colW + 10;
		boolean locked = Recorder.busy();

		// ---- Video
		int ly = y;
		heading(g, lx, ly, "VIDEO");
		ly += 12;
		String[] sizes = labels(Sizes.Size.VIDEO);
		ly = seg(g, lx, ly, colW, row, "Size", sizes, index(Sizes.Size.VIDEO, s.videoSize), locked, i -> s.videoSize = Sizes.Size.VIDEO[i], mx, my);
		ly = seg(g, lx, ly, colW, row, "Frame rate", new String[] {"30", "60"}, s.fps == 60 ? 1 : 0, locked, i -> s.fps = i == 1 ? 60 : 30, mx, my);
		ly = seg(g, lx, ly, colW, row, "Quality", new String[] {"Good", "High", "Max"}, s.quality.ordinal(), locked, i -> s.quality = Quality.values()[i], mx, my);
		boolean hevcOk = Ffmpeg.hevc() != null || !Ffmpeg.probed();
		ly = seg(g, lx, ly, colW, row, "Format", new String[] {"H.264", "H.265"}, s.hevc ? 1 : 0, locked, i -> s.hevc = i == 1 && hevcOk, mx, my);
		ly = seg(g, lx, ly, colW, row, "Sharper", new String[] {"Off", "On"}, s.sharp ? 1 : 0, locked, i -> s.sharp = i == 1, mx, my);
		ly = toggles(g, lx, ly, colW, row, "Sound", locked, mx, my);

		int rw = Capture.realWidth(), rh = Capture.realHeight();
		int[] out = Sizes.output(s.videoSize, s.shape, Math.max(2, rw), Math.max(2, rh));
		boolean hevc = s.hevc && Ffmpeg.hevc() != null;
		long per10 = Sizes.bytesPerMinute(s.quality, out[0], out[1], s.fps, hevc) * 10;
		g.drawString(font, out[0] + " x " + out[1] + " · about " + Sizes.bytes(per10) + " per 10 min", lx, ly + 1, MUTED, false);
		ly += 11;
		String hint = videoHint(s, out, rh);
		if (hint != null) {
			for (var line : font.split(Component.literal(hint), colW)) {
				g.drawString(font, line, lx, ly + 1, DIM, false);
				ly += 10;
			}
		}

		// ---- Photo
		int ry = y;
		heading(g, rx, ry, "PHOTO");
		ry += 12;
		ry = seg(g, rx, ry, colW, row, "Size", labels(Sizes.Size.PHOTO), index(Sizes.Size.PHOTO, s.photoSize), Photo.busy(),
				i -> s.photoSize = Sizes.Size.PHOTO[i], mx, my);
		ry = seg(g, rx, ry, colW, row, "Type", new String[] {"PNG", "JPG"}, s.jpg ? 1 : 0, false, i -> s.jpg = i == 1, mx, my);
		ry = seg(g, rx, ry, colW, row, "Shape", new String[] {"16:9", "Window"}, s.shape.ordinal(), locked, i -> s.shape = Sizes.Shape.values()[i], mx, my);
		int[] po = Sizes.output(s.photoSize, s.shape, Math.max(2, rw), Math.max(2, rh));
		g.drawString(font, po[0] + " x " + po[1] + (po[1] > Sizes.crop(s.shape, rw, rh)[3] ? " · drawn at full size" : ""), rx, ry + 1, MUTED, false);
		ry += 15;

		// ---- Save to
		heading(g, rx, ry, "SAVE TO");
		ry += 12;
		Path folder = Lens.output();
		String shown = s.folderChosen ? folder.toString() : "Not chosen yet";
		g.drawString(font, fit(shown, colW), rx, ry + 1, s.folderChosen ? TEXT : AMBER, false);
		ry += 11;
		int bx = rx;
		bx = button(g, bx, ry, choosing ? "Choosing..." : "Choose folder", !choosing && !locked, this::chooseFolder, mx, my) + 4;
		bx = button(g, bx, ry, "Open", true, () -> openFolder(folder), mx, my) + 4;
		long free = Capture.freeSpace(folder);
		if (free >= 0) g.drawString(font, Sizes.bytes(free) + " free", bx + 2, ry + 3, DIM, false);
		ry += 17;

		// ---- Ready?
		heading(g, rx, ry, "READY");
		ry += 12;
		ry = statusLines(g, rx, ry, colW, mx, my);

		// ---- Bottom bar
		int by = y0 + ph - 22;
		g.fill(x0 + 1, by - 5, x0 + pw - 1, by - 4, EDGE);
		int fx = x0 + 10;
		String recKey = LensClient.RECORD.getTranslatedKeyMessage().getString(), photoKey = LensClient.PHOTO.getTranslatedKeyMessage().getString();
		if (Recorder.recording()) {
			fx = primary(g, fx, by, "■ Stop and save (" + recKey + ")", RED, true, () -> {
				Recorder.stop(null);
			}, mx, my) + 6;
			fx = button(g, fx, by, Recorder.paused() ? "Resume" : "Pause", true, Recorder::togglePause, mx, my) + 6;
		} else {
			boolean can = Recorder.state() == Recorder.State.IDLE;
			String label = !s.folderChosen ? "Choose a folder first" : "● Start recording (" + recKey + ")";
			fx = primary(g, fx, by, label, s.folderChosen ? VIOLET : DIM, can, () -> {
				if (!Lens.settings.folderChosen) {
					chooseFolder();
					return;
				}
				String why = Recorder.cantStart();
				if (why != null) {
					message = why;
					return;
				}
				Lens.save();
				minecraft.setScreen(null);
				Recorder.start();
			}, mx, my) + 6;
		}
		fx = button(g, fx, by, "Take photo (" + photoKey + ")", !Photo.busy(), () -> {
			Lens.save();
			minecraft.setScreen(null);
			Photo.take();
		}, mx, my) + 6;
		int doneW = font.width("Done") + 16;
		button(g, x0 + pw - 10 - doneW, by, "Done", true, this::onClose, mx, my);
	}

	private String recLine() {
		return switch (Recorder.state()) {
			case RECORDING -> (Recorder.paused() ? "❚❚ " : "● ") + Sizes.clock(Recorder.clockMs()) + "  " + Recorder.width() + "x" + Recorder.height() + " " + Recorder.fps() + " fps";
			case FINISHING -> Recorder.status;
			case STARTING -> "Starting";
			default -> null;
		};
	}

	private String videoHint(Settings s, int[] out, int screenH) {
		int cropH = Sizes.crop(s.shape, Math.max(2, Capture.realWidth()), Math.max(2, screenH))[3];
		if (s.videoSize != Sizes.Size.SCREEN && out[1] > cropH * 1.05) {
			return s.sharp ? "Sharper: the game draws at the video's size while you record. Expect fewer frames a second with shaders."
					: "Your window is " + cropH + " tall, so the video is enlarged. Turn on Sharper to draw the game at " + out[1] + ".";
		}
		if (Recorder.recording()) return "Settings apply to your next recording.";
		return null;
	}

	private int statusLines(GuiGraphics g, int x, int y, int w, int mx, int my) {
		String exe = Ffmpeg.find();
		boolean failed = install != null && install.startsWith("Download failed");
		if (install != null) {
			for (var line : font.split(Component.literal(install), w)) {
				g.drawString(font, line, x, y + 1, failed ? RED : AMBER, false);
				y += 10;
			}
			y += 2;
			if (failed) {
				button(g, x, y, "Try again", true, this::installFfmpeg, mx, my);
				y += 17;
			}
		} else if (exe == null) {
			g.drawString(font, Ffmpeg.searching() ? "Looking for FFmpeg..." : "FFmpeg is needed to make videos (about 100 MB, once).", x, y + 1, AMBER, false);
			y += 11;
			if (!Ffmpeg.searching() && Ffmpeg.canInstall()) {
				button(g, x, y, "Get FFmpeg", true, this::installFfmpeg, mx, my);
				y += 17;
			}
		} else if (!Ffmpeg.probed()) {
			g.drawString(font, "Checking your graphics card's encoder...", x, y + 1, MUTED, false);
			y += 12;
		} else {
			String h = Ffmpeg.h264(), v = Ffmpeg.hevc();
			boolean hw = Ffmpeg.hardware(h);
			y = wrap(g, (hw ? "✔ " : "! ") + "Encoder: " + Ffmpeg.label(h) + (v != null ? " (H.264, H.265)" : " (H.264)"), x, y, w, hw ? GREEN : AMBER);
			if (!hw) y = wrap(g, "No graphics-card encoder: 1K at 30 fps works best.", x, y, w, DIM);
		}
		String pack = Shaders.activePack();
		y = wrap(g, pack != null ? "✔ Shaders: " + pack + ", in your video" : Shaders.irisInstalled() ? "Shaders: off" : "Shaders: Iris not installed", x, y, w,
				pack != null ? GREEN : MUTED);
		if (Lens.settings.mic && !Mic.available()) y = wrap(g, "! No microphone found", x, y, w, AMBER);
		return wrap(g, "Keys: " + LensClient.RECORD.getTranslatedKeyMessage().getString() + " record, " + LensClient.PHOTO.getTranslatedKeyMessage().getString()
				+ " photo (Controls to change)", x, y, w, DIM);
	}

	// ---------------------------------------------------------------- widgets

	private void heading(GuiGraphics g, int x, int y, String text) {
		g.drawString(font, text, x, y, DIM, false);
	}

	private interface Pick {
		void pick(int i);
	}

	/** Where the pills start, after a row's label. */
	private static final int LABEL_W = 50;

	/** A row: label on the left, then pills (as wide as their words, shrunk to fit), one picked. Returns the next row's y. */
	private int seg(GuiGraphics g, int x, int y, int w, int rowH, String label, String[] opts, int sel, boolean locked, Pick pick, int mx, int my) {
		g.drawString(font, label, x, y + (rowH - 8) / 2, MUTED, false);
		int px = x + LABEL_W;
		int avail = w - LABEL_W, gap = 3;
		int[] pw = new int[opts.length];
		int total = gap * (opts.length - 1);
		for (int i = 0; i < opts.length; i++) {
			pw[i] = Math.max(24, font.width(opts[i]) + 10);
			total += pw[i];
		}
		if (total > avail) {
			double k = (avail - gap * (opts.length - 1)) / (double) (total - gap * (opts.length - 1));
			for (int i = 0; i < opts.length; i++) pw[i] = Math.max(font.width(opts[i]) + 4, (int) Math.floor(pw[i] * k));
		}
		int ph = rowH - 3;
		for (int i = 0; i < opts.length; i++) {
			int pillW = pw[i];
			boolean on = i == sel, hover = !locked && mx >= px && my >= y && mx < px + pillW && my < y + ph;
			if (on) gradient(g, px, y, pillW, ph);
			else g.fill(px, y, px + pillW, y + ph, hover ? 0xFF22222A : PANEL);
			outline(g, px, y, pillW, ph, on ? 0x00000000 : EDGE);
			int tc = locked && !on ? DIM : on ? 0xFFFFFFFF : TEXT;
			g.drawString(font, opts[i], px + (pillW - font.width(opts[i])) / 2, y + (ph - 8) / 2 + 1, tc, false);
			if (!locked) {
				final int idx = i;
				hits.add(new Hit(px, y, pillW, ph, () -> pick.pick(idx)));
			}
			px += pillW + gap;
		}
		return y + rowH;
	}

	/** Lines of text wrapped to the width. Returns the next y. */
	private int wrap(GuiGraphics g, String text, int x, int y, int w, int colour) {
		for (var line : font.split(Component.literal(text), w)) {
			g.drawString(font, line, x, y + 1, colour, false);
			y += 10;
		}
		return y + 1;
	}

	/** Game sound and microphone, each on or off. */
	private int toggles(GuiGraphics g, int x, int y, int w, int rowH, String label, boolean locked, int mx, int my) {
		Settings s = Lens.settings;
		g.drawString(font, label, x, y + (rowH - 8) / 2, MUTED, false);
		int px = x + LABEL_W, ph = rowH - 3;
		String[] names = {"Game", "Mic"};
		boolean[] on = {s.gameSound, s.mic};
		for (int i = 0; i < 2; i++) {
			int pw = font.width(names[i]) + 22;
			if (on[i]) gradient(g, px, y, pw, ph);
			else g.fill(px, y, px + pw, y + ph, PANEL);
			outline(g, px, y, pw, ph, on[i] ? 0x00000000 : EDGE);
			g.drawString(font, (on[i] ? "✔ " : "  ") + names[i], px + 5, y + (ph - 8) / 2 + 1, on[i] ? 0xFFFFFFFF : (locked ? DIM : TEXT), false);
			if (!locked) {
				final int idx = i;
				hits.add(new Hit(px, y, pw, ph, () -> {
					if (idx == 0) s.gameSound = !s.gameSound;
					else s.mic = !s.mic;
				}));
			}
			px += pw + 3;
		}
		return y + rowH;
	}

	private int button(GuiGraphics g, int x, int y, String label, boolean enabled, Runnable action, int mx, int my) {
		int w = font.width(label) + 14, h = 15;
		boolean hover = enabled && mx >= x && my >= y && mx < x + w && my < y + h;
		g.fill(x, y, x + w, y + h, hover ? 0xFF24242C : PANEL);
		outline(g, x, y, w, h, hover ? VIOLET : EDGE);
		g.drawString(font, label, x + 7, y + 4, enabled ? TEXT : DIM, false);
		if (enabled) hits.add(new Hit(x, y, w, h, action));
		return x + w;
	}

	private int primary(GuiGraphics g, int x, int y, String label, int colour, boolean enabled, Runnable action, int mx, int my) {
		int w = font.width(label) + 18, h = 15;
		boolean hover = enabled && mx >= x && my >= y && mx < x + w && my < y + h;
		if (colour == VIOLET) gradient(g, x, y, w, h);
		else g.fill(x, y, x + w, y + h, colour);
		if (hover) outline(g, x, y, w, h, 0xFFFFFFFF);
		g.drawString(font, label, x + 9, y + 4, 0xFFFFFFFF, false);
		if (enabled) hits.add(new Hit(x, y, w, h, action));
		return x + w;
	}

	/** The LumenForge gradient, violet to blue to teal, left to right. */
	static void gradient(GuiGraphics g, int x, int y, int w, int h) {
		int steps = Math.max(1, Math.min(w, 48));
		for (int i = 0; i < steps; i++) {
			int a = x + w * i / steps, b = x + w * (i + 1) / steps;
			float t = (i + 0.5F) / steps;
			int c = t < 0.5F ? mix(VIOLET, BLUE, t * 2) : mix(BLUE, TEAL, (t - 0.5F) * 2);
			g.fill(a, y, b, y + h, c);
		}
	}

	static int mix(int a, int b, float t) {
		int out = 0;
		for (int sh = 0; sh <= 24; sh += 8) {
			int ca = (a >>> sh) & 0xFF, cb = (b >>> sh) & 0xFF;
			out |= Math.round(ca + (cb - ca) * t) << sh;
		}
		return out;
	}

	static void outline(GuiGraphics g, int x, int y, int w, int h, int c) {
		if ((c >>> 24) == 0) return;
		g.fill(x, y, x + w, y + 1, c);
		g.fill(x, y + h - 1, x + w, y + h, c);
		g.fill(x, y, x + 1, y + h, c);
		g.fill(x + w - 1, y, x + w, y + h, c);
	}

	private String fit(String s, int w) {
		if (font.width(s) <= w) return s;
		String dots = "...";
		// Keep the end of a path (the folder's own name), which is what tells folders apart.
		int keep = s.length();
		while (keep > 0 && font.width(dots + s.substring(s.length() - keep)) > w) keep--;
		return dots + s.substring(s.length() - keep);
	}

	private static String[] labels(Sizes.Size[] sizes) {
		String[] l = new String[sizes.length];
		for (int i = 0; i < sizes.length; i++) l[i] = sizes[i].label;
		return l;
	}

	private static int index(Sizes.Size[] sizes, Sizes.Size s) {
		for (int i = 0; i < sizes.length; i++) if (sizes[i] == s) return i;
		return 0;
	}

	// ---------------------------------------------------------------- input

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == 0) {
			for (Hit h : new ArrayList<>(hits)) {
				if (h.in(event.x(), event.y())) {
					h.action().run();
					Lens.save();
					return true;
				}
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean keyPressed(KeyEvent e) {
		if (e.key() == GLFW.GLFW_KEY_ESCAPE) {
			onClose();
			return true;
		}
		return super.keyPressed(e);
	}

	// ---------------------------------------------------------------- actions

	/** The system's folder picker, on its own thread so the game keeps running. */
	private void chooseFolder() {
		if (choosing) return;
		choosing = true;
		String start = Lens.output().toAbsolutePath().toString();
		Thread t = new Thread(() -> {
			String picked = null;
			try {
				picked = org.lwjgl.util.tinyfd.TinyFileDialogs.tinyfd_selectFolderDialog("Where should Lightlens save videos and photos?", start);
			} catch (Throwable e) {
				Lens.LOG.warn("The folder picker didn't open: {}", e.toString());
			}
			String p = picked;
			Minecraft.getInstance().execute(() -> {
				choosing = false;
				if (p != null && !p.isBlank()) {
					Lens.settings.folder = p;
					Lens.settings.folderChosen = true;
					Lens.save();
					message = null;
				} else if (!Lens.settings.folderChosen) {
					message = "No folder picked. You can also use .minecraft/lightlens/captures: press Choose folder and pick it.";
				}
			});
		}, "Lightlens folder picker");
		t.setDaemon(true);
		t.start();
	}

	private static void openFolder(Path p) {
		try {
			java.nio.file.Files.createDirectories(p);
			Util.getPlatform().openPath(p);
		} catch (Exception e) {
			Lens.LOG.warn("Couldn't open {}: {}", p, e.toString());
		}
	}

	private void installFfmpeg() {
		if (install != null && !install.startsWith("Download failed")) return;
		install = "Downloading FFmpeg...";
		Thread t = new Thread(() -> {
			try {
				Ffmpeg.install((done, total) -> install = total > 0 ? "Downloading FFmpeg " + (done * 100 / total) + "%" : "Downloading FFmpeg " + Sizes.bytes(done));
				install = "Checking FFmpeg...";
				String exe = Ffmpeg.findNow();
				if (exe != null) Ffmpeg.probe(exe);
				install = null;
			} catch (Exception e) {
				Lens.LOG.error("Getting FFmpeg failed", e);
				install = "Download failed: " + e.getMessage();
			}
		}, "Lightlens FFmpeg download");
		t.setDaemon(true);
		t.start();
	}
}
