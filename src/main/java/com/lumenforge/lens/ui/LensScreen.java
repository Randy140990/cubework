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

import static com.lumenforge.lens.ui.Draw.*;

/**
 * The Lightlens screen, full screen in the LumenForge look: the game stays visible behind a light tint; three
 * cards (Video, Photo, GIF) with segmented controls; a strip for where things are saved and whether FFmpeg, the
 * encoder and shaders are ready; and the actions along the bottom with their keys. It fades and rises in.
 */
public final class LensScreen extends Screen {
	private final Screen parent;
	private String message;
	private final List<Hit> hits = new ArrayList<>();
	private static volatile String install;
	private static volatile float installProgress = -1;
	private static volatile boolean choosing;
	private long opened;
	/** How far the screen has faded in (0..1), set each frame. */
	private float in = 1;

	private record Hit(float x, float y, float w, float h, Runnable action) {
		boolean contains(double mx, double my) {
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
		if (opened == 0) opened = System.currentTimeMillis();
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

	/** A light tint (the game shows through), a little darker at the top and bottom where the words are. No blur. */
	@Override
	public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
		if (minecraft.level == null) {
			g.fill(0, 0, width, height, 0xFF0B0B0E);
			return;
		}
		float t = ease();
		g.fill(0, 0, width, height, fade(0x6E0B0B0E, t));
		int band = Math.max(40, height / 5);
		g.fillGradient(0, 0, width, band, fade(0x900B0B0E, t), 0x000B0B0E);
		g.fillGradient(0, height - band, width, height, 0x000B0B0E, fade(0xA00B0B0E, t));
	}

	private float ease() {
		float t = Math.min(1, (System.currentTimeMillis() - opened) / 220F);
		return 1 - (1 - t) * (1 - t) * (1 - t);
	}

	private int a(int c) {
		return fade(c, in);
	}

	// ---------------------------------------------------------------- layout

	@Override
	public void render(GuiGraphics g, int mx, int my, float partial) {
		super.render(g, mx, my, partial);
		hits.clear();
		in = ease();
		float rise = (1 - in) * 8;
		Settings s = Lens.settings;
		float m = Math.max(8, Math.min(24, width * 0.025F));
		float gap = Math.max(5, Math.min(10, width * 0.012F));
		boolean locked = Recorder.busy();

		// ---- Top bar: the mark, the name with its gradient full stop, what it's for, and the status pill.
		float top = m + rise;
		mark(g, m, top, 18);
		big(g, font, "Lightlens", TITLE, m + 25, top + 9, 1.45F, a(TEXT));
		float nameW = width(font, "Lightlens", TITLE) * 1.45F;
		dot(g, m + 25 + nameW + 2.5F, top + 13.2F, 1.6F, a(TEAL));
		float pillW = statusPill(g, width - m, top + 9);
		String tagline = "Record, photograph and loop what you see";
		float tagX = m + 25 + nameW + 9;
		if (tagX + width(font, tagline, UI) < width - m - pillW - 12) text(g, font, tagline, UI, tagX, top + 10, a(MUTED));
		hairline(g, m, top + 24, width - 2 * m, 0.9F * in);

		// ---- Bottom: the actions, and above them the save and ready strip.
		float barH = 22, stripH = 30;
		float barY = height - m - barH + rise * 0.5F;
		float stripY = barY - gap - stripH;
		float cardsY = top + 30;
		float cardsH = stripY - gap - cardsY;
		if (message != null) {
			float bw = Math.min(width - 2 * m, width(font, message, UI) + 28);
			card(g, (width - bw) / 2, cardsY, bw, 16, 8, a(0xE6241A0A), a(0x66F59E0B));
			text(g, font, fit(font, message, UI, Math.round(bw - 28)), UI, (width - bw) / 2 + 14, cardsY + 8, a(0xFFFCD9A0));
			cardsY += 16 + gap;
			cardsH -= 16 + gap;
		}

		// ---- Three cards.
		float cw = (width - 2 * m - 2 * gap) / 3;
		videoCard(g, s, m, cardsY, cw, cardsH, locked, mx, my);
		photoCard(g, s, m + cw + gap, cardsY, cw, cardsH, mx, my);
		gifCard(g, s, m + 2 * (cw + gap), cardsY, cw, cardsH, locked, mx, my);

		strip(g, s, m, stripY, width - 2 * m, stripH, locked, mx, my);
		actions(g, s, m, barY, width - 2 * m, barH, mx, my);
	}

	/** The Lightlens mark: a rounded square in the gradient with a lens and its red dot. */
	private void mark(GuiGraphics g, float x, float y, float size) {
		rrGradient(g, x, y, size, size, size * 0.28F, in);
		dot(g, x + size / 2, y + size / 2, size * 0.34F, a(0xFF16161C));
		dot(g, x + size / 2, y + size / 2, size * 0.17F, a(RED));
		dot(g, x + size * 0.36F, y + size * 0.34F, size * 0.06F, a(0x99FFFFFF));
	}

	/** What's happening, top right. Returns its width. */
	private float statusPill(GuiGraphics g, float right, float cy) {
		String label;
		int colour;
		switch (Recorder.state()) {
			case RECORDING -> {
				if (Recorder.gif()) {
					label = "GIF  " + (Recorder.gifLeft() + 999) / 1000 + " s left";
					colour = TEAL;
				} else {
					label = (Recorder.paused() ? "Paused  " : "Recording  ") + Sizes.clock(Recorder.clockMs());
					colour = Recorder.paused() ? AMBER : RED;
				}
			}
			case FINISHING -> {
				label = Recorder.status;
				colour = BLUE;
			}
			default -> {
				if (Ffmpeg.find() == null) {
					label = Ffmpeg.searching() ? "Looking for FFmpeg" : "FFmpeg needed";
					colour = AMBER;
				} else if (!Ffmpeg.probed()) {
					label = "Checking the encoder";
					colour = BLUE;
				} else {
					label = "Ready";
					colour = GREEN;
				}
			}
		}
		float w = width(font, label, SEMI) + 24, h = 15;
		float x = right - w;
		card(g, x, cy - h / 2, w, h, h / 2, a(fade(colour, 0.16F)), a(fade(colour, 0.55F)));
		boolean blink = Recorder.recording() && !Recorder.paused() && (System.currentTimeMillis() / 600) % 2 == 1;
		if (!blink) dot(g, x + 9, cy, 2.6F, a(colour));
		text(g, font, label, SEMI, x + 16, cy, a(TEXT));
		return w;
	}

	// ---------------------------------------------------------------- cards

	private static final int CARD = 0xD614141A, CARD_EDGE = 0x24FFFFFF;

	/** A card with its title (and a coloured dot). Returns the y where its rows start. */
	private float cardTop(GuiGraphics g, float x, float y, float w, float h, String title, int accent) {
		card(g, x, y, w, h, 7, a(CARD), a(CARD_EDGE));
		dot(g, x + 12, y + 12, 2.8F, a(accent));
		text(g, font, title, SEMI, x + 20, y + 12, a(TEXT));
		return y + 25;
	}

	private float rowH(float cardH) {
		return Math.max(13, Math.min(18, (cardH - 25 - 26 - 6) / 6));
	}

	private void footer(GuiGraphics g, float x, float y, float w, float h, String main, String note, int noteColour) {
		float fy = y + h - 22;
		rect(g, x + 10, fy - 4, w - 20, 1 / gs(), a(0x1AFFFFFF));
		text(g, font, fit(font, main, MONO, Math.round(w - 20)), MONO, x + 10, fy + 4, a(SOFT));
		text(g, font, fit(font, note, UI, Math.round(w - 20)), UI, x + 10, fy + 13, a(noteColour));
	}

	private void videoCard(GuiGraphics g, Settings s, float x, float y, float w, float h, boolean locked, int mx, int my) {
		float ry = cardTop(g, x, y, w, h, "Video", RED);
		float row = rowH(h);
		float px = x + 10, pw = w - 20;
		ry = seg(g, px, ry, pw, row, "Size", labels(Sizes.Size.VIDEO), index(Sizes.Size.VIDEO, s.videoSize), locked, i -> s.videoSize = Sizes.Size.VIDEO[i], mx, my);
		ry = seg(g, px, ry, pw, row, "FPS", new String[] {"30", "60"}, s.fps == 60 ? 1 : 0, locked, i -> s.fps = i == 1 ? 60 : 30, mx, my);
		ry = seg(g, px, ry, pw, row, "Quality", new String[] {"Good", "High", "Max"}, s.quality.ordinal(), locked, i -> s.quality = Quality.values()[i], mx, my);
		boolean hevcOk = Ffmpeg.hevc() != null || !Ffmpeg.probed();
		ry = seg(g, px, ry, pw, row, "Format", new String[] {"H.264", "H.265"}, s.hevc && hevcOk ? 1 : 0, locked || !hevcOk,
				i -> s.hevc = i == 1 && Ffmpeg.hevc() != null, mx, my);
		ry = seg(g, px, ry, pw, row, "Sharper", new String[] {"Off", "On"}, s.sharp ? 1 : 0, locked, i -> s.sharp = i == 1, mx, my);
		toggles(g, px, ry, pw, row, "Sound", new String[] {"Game", "Mic"}, new boolean[] {s.gameSound, s.mic}, locked, i -> {
			if (i == 0) s.gameSound = !s.gameSound;
			else s.mic = !s.mic;
		}, mx, my);

		int rw = Math.max(2, Capture.realWidth()), rh = Math.max(2, Capture.realHeight());
		int[] out = Sizes.output(s.videoSize, s.shape, rw, rh);
		boolean hevc = s.hevc && Ffmpeg.hevc() != null;
		long per10 = Sizes.bytesPerMinute(s.quality, out[0], out[1], s.fps, hevc) * 10;
		int cropH = Sizes.crop(s.shape, rw, rh)[3];
		boolean bigger = s.videoSize != Sizes.Size.SCREEN && out[1] > cropH * 1.05;
		String note;
		int colour = MUTED;
		if (bigger && s.sharp) {
			int k = (int) Math.max(2, Math.ceil(out[1] / (double) cropH - 0.02));
			note = "Sharper draws " + k + "x the size: fewer fps";
			colour = AMBER;
		} else if (bigger) {
			note = "Enlarged from your " + cropH + "p window";
		} else {
			note = "About " + Sizes.bytes(per10) + " per 10 min";
		}
		footer(g, x, y, w, h, out[0] + " × " + out[1] + " · " + s.fps + " fps", note, colour);
	}

	private void photoCard(GuiGraphics g, Settings s, float x, float y, float w, float h, int mx, int my) {
		float ry = cardTop(g, x, y, w, h, "Photo", TEAL);
		float row = rowH(h);
		float px = x + 10, pw = w - 20;
		boolean busy = Photo.busy();
		ry = seg(g, px, ry, pw, row, "Size", labels(Sizes.Size.PHOTO), index(Sizes.Size.PHOTO, s.photoSize), busy, i -> s.photoSize = Sizes.Size.PHOTO[i], mx, my);
		ry = seg(g, px, ry, pw, row, "Type", new String[] {"PNG", "JPG"}, s.jpg ? 1 : 0, false, i -> s.jpg = i == 1, mx, my);
		ry = seg(g, px, ry, pw, row, "Wait", new String[] {"None", "3 s", "10 s"}, s.photoWait == 10 ? 2 : s.photoWait == 3 ? 1 : 0, false,
				i -> s.photoWait = i == 2 ? 10 : i == 1 ? 3 : 0, mx, my);
		seg(g, px, ry, pw, row, "Shape", new String[] {"16:9", "Window"}, s.shape.ordinal(), Recorder.busy(), i -> s.shape = Sizes.Shape.values()[i], mx, my);

		int rw = Math.max(2, Capture.realWidth()), rh = Math.max(2, Capture.realHeight());
		int[] po = Sizes.output(s.photoSize, s.shape, rw, rh);
		boolean drawn = po[1] > Sizes.crop(s.shape, rw, rh)[3];
		footer(g, x, y, w, h, po[0] + " × " + po[1] + " · " + (s.jpg ? "JPG" : "PNG"),
				drawn ? "Drawn at full size, so it's sharp" : "Exactly as you see it", MUTED);
	}

	private void gifCard(GuiGraphics g, Settings s, float x, float y, float w, float h, boolean locked, int mx, int my) {
		float ry = cardTop(g, x, y, w, h, "GIF", VIOLET);
		float row = rowH(h);
		float px = x + 10, pw = w - 20;
		ry = seg(g, px, ry, pw, row, "Length", new String[] {"3 s", "5 s", "10 s"}, s.gifSeconds == 10 ? 2 : s.gifSeconds == 3 ? 0 : 1, locked,
				i -> s.gifSeconds = i == 2 ? 10 : i == 0 ? 3 : 5, mx, my);
		ry = seg(g, px, ry, pw, row, "Width", new String[] {"480", "640", "960"}, s.gifWidth == 960 ? 2 : s.gifWidth == 480 ? 0 : 1, locked,
				i -> s.gifWidth = i == 2 ? 960 : i == 0 ? 480 : 640, mx, my);
		seg(g, px, ry, pw, row, "Style", new String[] {"Normal", "Timelapse"}, s.gifTimelapse ? 1 : 0, locked, i -> s.gifTimelapse = i == 1, mx, my);

		int rw = Math.max(2, Capture.realWidth()), rh = Math.max(2, Capture.realHeight());
		int[] c = Sizes.crop(s.shape, rw, rh);
		int gw = s.gifWidth, gh = (int) Math.round(gw * c[3] / (double) Math.max(1, c[2])) & ~1;
		String note = "About " + Sizes.bytes(Ffmpeg.gifBytes(gw, gh, s.gifSeconds, s.gifTimelapse))
				+ (s.gifTimelapse ? " · " + s.gifSeconds * 10 + " s of play" : "");
		footer(g, x, y, w, h, gw + " × " + gh + " · " + (s.gifTimelapse ? "timelapse 10x" : "15 fps"), note, MUTED);
	}

	// ---------------------------------------------------------------- the strip

	private void strip(GuiGraphics g, Settings s, float x, float y, float w, float h, boolean locked, int mx, int my) {
		card(g, x, y, w, h, 7, a(CARD), a(CARD_EDGE));
		float third = w / 3;
		float cy = y + h / 2;

		// Save to.
		Path folder = Lens.output();
		float bx = x + third - 10;
		bx = ghost(g, bx, cy, "Open", true, () -> openFolder(folder), mx, my, true) - 4;
		bx = ghost(g, bx, cy, choosing ? "Choosing…" : "Choose", !choosing && !locked, this::chooseFolder, mx, my, true) - 6;
		text(g, font, "SAVE TO", SEMI, x + 10, cy - 5, a(DIM));
		long free = Capture.freeSpace(folder);
		String where = s.folderChosen ? folder.toString() : "Not chosen yet";
		int roomW = Math.round(bx - (x + 10));
		text(g, font, fitEnd(font, where, MONO, roomW), MONO, x + 10, cy + 5, a(s.folderChosen ? SOFT : AMBER));
		if (free >= 0) {
			String f = Sizes.bytes(free) + " free";
			text(g, font, f, UI, bx - width(font, f, UI), cy - 5, a(DIM));
		}
		rect(g, x + third, y + 6, 1 / gs(), h - 12, a(0x1AFFFFFF));

		// Encoder.
		float ex = x + third + 10, ew = third - 20;
		text(g, font, "ENCODER", SEMI, ex, cy - 5, a(DIM));
		String exe = Ffmpeg.find();
		boolean failed = install != null && install.startsWith("Download failed");
		if (install != null && !failed) {
			text(g, font, fit(font, install, UI, Math.round(ew)), UI, ex, cy + 5, a(AMBER));
			if (installProgress >= 0) {
				rr(g, ex, cy + 10, ew, 2, 1, a(0x22FFFFFF));
				rrGradient(g, ex, cy + 10, Math.max(2, ew * installProgress), 2, 1, in);
			}
		} else if (exe == null) {
			if (Ffmpeg.searching()) text(g, font, "Looking for FFmpeg…", UI, ex, cy + 5, a(MUTED));
			else if (Ffmpeg.canInstall()) {
				float after = text(g, font, failed ? "Download failed" : "FFmpeg needed", UI, ex, cy + 5, a(failed ? RED : AMBER));
				ghost(g, after + 6, cy + 5, failed ? "Try again" : "Get FFmpeg (100 MB)", true, this::installFfmpeg, mx, my, false);
			} else text(g, font, "Install FFmpeg, then reopen", UI, ex, cy + 5, a(AMBER));
		} else if (!Ffmpeg.probed()) {
			text(g, font, "Checking your graphics card…", UI, ex, cy + 5, a(MUTED));
		} else if (Ffmpeg.h264() == null) {
			text(g, font, "Can't encode here", UI, ex, cy + 5, a(RED));
		} else {
			String h264 = Ffmpeg.h264(), hevc = Ffmpeg.hevc();
			boolean hw = Ffmpeg.hardware(h264);
			String line = Ffmpeg.label(h264) + (hevc != null ? " · H.264, H.265" : " · H.264");
			dot(g, ex + 2.5F, cy + 5, 2.2F, a(hw ? GREEN : AMBER));
			text(g, font, fit(font, line, UI, Math.round(ew - 8)), UI, ex + 8, cy + 5, a(hw ? TEXT : AMBER));
		}
		rect(g, x + 2 * third, y + 6, 1 / gs(), h - 12, a(0x1AFFFFFF));

		// Shaders, or the last problem.
		float sx = x + 2 * third + 10, sw = third - 20;
		String err = Recorder.lastError;
		if (err != null && !Recorder.busy()) {
			text(g, font, "LAST TIME", SEMI, sx, cy - 5, a(DIM));
			text(g, font, fit(font, err, UI, Math.round(sw)), UI, sx, cy + 5, a(RED));
		} else {
			text(g, font, "SHADERS", SEMI, sx, cy - 5, a(DIM));
			String pack = Shaders.activePack();
			if (pack != null) {
				dot(g, sx + 2.5F, cy + 5, 2.2F, a(GREEN));
				text(g, font, fit(font, pack + " · in your captures", UI, Math.round(sw - 8)), UI, sx + 8, cy + 5, a(TEXT));
			} else {
				text(g, font, Shaders.irisInstalled() ? "Off" : "Iris not installed", UI, sx, cy + 5, a(MUTED));
			}
			if (s.mic && !Mic.available()) text(g, font, "No mic found", UI, sx + sw - width(font, "No mic found", UI), cy - 5, a(AMBER));
		}
	}

	// ---------------------------------------------------------------- actions

	private void actions(GuiGraphics g, Settings s, float x, float y, float w, float h, int mx, int my) {
		String recKey = LensClient.RECORD.getTranslatedKeyMessage().getString();
		String photoKey = LensClient.PHOTO.getTranslatedKeyMessage().getString();
		String gifKey = LensClient.GIF.getTranslatedKeyMessage().getString();
		float bx = x;
		if (Recorder.recording()) {
			bx = button(g, bx, y, h, Recorder.gif() ? "Stop GIF" : "Stop and save", Recorder.gif() ? gifKey : recKey, 2, true, () -> Recorder.stop(null), mx, my) + 6;
			if (!Recorder.gif()) bx = button(g, bx, y, h, Recorder.paused() ? "Resume" : "Pause", null, 0, true, Recorder::togglePause, mx, my) + 6;
		} else {
			boolean can = Recorder.state() == Recorder.State.IDLE;
			bx = button(g, bx, y, h, s.folderChosen ? "Record" : "Choose a folder first", s.folderChosen ? recKey : null, 1, can, () -> {
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
		bx = button(g, bx, y, h, "Photo", photoKey, 0, !Photo.busy(), () -> {
			Lens.save();
			minecraft.setScreen(null);
			Photo.take();
		}, mx, my) + 6;
		if (!Recorder.busy()) {
			button(g, bx, y, h, "GIF", gifKey, 0, true, () -> {
				String why = Recorder.cantStart();
				if (why != null) {
					message = why;
					return;
				}
				Lens.save();
				minecraft.setScreen(null);
				Recorder.startGif();
			}, mx, my);
		}
		float dw = width(font, "Done", SEMI) + width(font, "Esc", MONO) + 36;
		button(g, x + w - dw, y, h, "Done", "Esc", 0, true, this::onClose, mx, my);
	}

	/**
	 * A pill button with its key in a little chip. Kind 1 is the main one (the gradient), 2 is stop (red), 0 is
	 * plain. Returns its right edge.
	 */
	private float button(GuiGraphics g, float x, float y, float h, String label, String key, int kind, boolean enabled, Runnable action, int mx, int my) {
		boolean icon = kind == 1 || kind == 2;
		float labelW = width(font, label, SEMI), keyW = key == null ? 0 : width(font, key, MONO) + 8;
		float w = 14 + (icon ? 10 : 0) + labelW + (key != null ? 6 + keyW : 0) + 14;
		boolean hover = enabled && mx >= x && my >= y && mx < x + w && my < y + h;
		if (kind == 1) {
			rrGradient(g, x, y, w, h, h / 2, enabled ? in : 0.35F * in);
			if (hover) rr(g, x, y, w, h, h / 2, a(0x22FFFFFF));
		} else if (kind == 2) {
			rr(g, x, y, w, h, h / 2, a(hover ? 0xFFF05252 : RED));
		} else {
			card(g, x, y, w, h, h / 2, a(hover ? 0xEE24242C : 0xE61A1A20), a(hover ? fade(VIOLET, 0.8F) : 0x30FFFFFF));
		}
		float tx = x + 14, cy = y + h / 2;
		if (kind == 1) dot(g, tx + 3, cy, 2.8F, a(0xFFFFFFFF));
		if (kind == 2) rr(g, tx + 0.5F, cy - 2.5F, 5, 5, 1, a(0xFFFFFFFF));
		if (icon) tx += 10;
		int tc = enabled ? (kind == 0 ? TEXT : 0xFFFFFFFF) : DIM;
		tx = text(g, font, label, SEMI, tx, cy, a(tc));
		if (key != null) {
			float kx = tx + 6;
			rr(g, kx, cy - 5, keyW, 10, 3, a(kind == 0 ? 0x22FFFFFF : 0x33000000));
			text(g, font, key, MONO, kx + 4, cy, a(kind == 0 ? MUTED : 0xDDFFFFFF));
		}
		if (enabled) hits.add(new Hit(x, y, w, h, action));
		return x + w;
	}

	/** A small outlined button. `fromRight`: x is its right edge. Returns its other edge. */
	private float ghost(GuiGraphics g, float x, float cy, String label, boolean enabled, Runnable action, int mx, int my, boolean fromRight) {
		float w = width(font, label, UI) + 14, h = 13;
		float bx = fromRight ? x - w : x;
		boolean hover = enabled && mx >= bx && my >= cy - h / 2 && mx < bx + w && my < cy + h / 2;
		card(g, bx, cy - h / 2, w, h, h / 2, a(hover ? 0xFF26262E : 0xFF1C1C22), a(hover ? fade(VIOLET, 0.8F) : 0x30FFFFFF));
		text(g, font, label, UI, bx + 7, cy, a(enabled ? TEXT : DIM));
		if (enabled) hits.add(new Hit(bx, cy - h / 2, w, h, action));
		return fromRight ? bx : bx + w;
	}

	// ---------------------------------------------------------------- controls

	private interface Pick {
		void pick(int i);
	}

	/** The width of a row's label column. */
	private static final float LABEL = 40;

	/** A row: its label, then a segmented control (a track with the picked part in the gradient). Returns the next row's y. */
	private float seg(GuiGraphics g, float x, float y, float w, float rowH, String label, String[] opts, int sel, boolean locked, Pick pick, int mx, int my) {
		float cy = y + rowH / 2;
		text(g, font, label, UI, x, cy, a(MUTED));
		float tx = x + LABEL, tw = w - LABEL, th = Math.min(13, rowH - 2);
		rr(g, tx, cy - th / 2, tw, th, th / 2, a(0x14FFFFFF));
		float[] ws = new float[opts.length];
		float total = 0;
		for (int i = 0; i < opts.length; i++) total += ws[i] = width(font, opts[i], SEMI) + 8;
		// Equal parts when they fit (tidier); otherwise as wide as their words.
		boolean equal = tw / opts.length >= max(ws);
		float ox = tx;
		for (int i = 0; i < opts.length; i++) {
			float sw = equal ? tw / opts.length : ws[i] * tw / total;
			boolean on = i == sel, hover = !locked && !on && mx >= ox && my >= cy - th / 2 && mx < ox + sw && my < cy + th / 2;
			if (on) rrGradient(g, ox + 1, cy - th / 2 + 1, sw - 2, th - 2, (th - 2) / 2, locked ? 0.45F * in : in);
			else if (hover) rr(g, ox + 1, cy - th / 2 + 1, sw - 2, th - 2, (th - 2) / 2, a(0x1CFFFFFF));
			int tc = on ? 0xFFFFFFFF : locked ? DIM : SOFT;
			text(g, font, opts[i], on ? SEMI : UI, ox + (sw - width(font, opts[i], on ? SEMI : UI)) / 2, cy, a(tc));
			if (!locked && !on) {
				final int idx = i;
				hits.add(new Hit(ox, cy - th / 2, sw, th, () -> pick.pick(idx)));
			}
			ox += sw;
		}
		return y + rowH;
	}

	/** A row of on/off chips. */
	private float toggles(GuiGraphics g, float x, float y, float w, float rowH, String label, String[] names, boolean[] on, boolean locked, Pick flip, int mx, int my) {
		float cy = y + rowH / 2;
		text(g, font, label, UI, x, cy, a(MUTED));
		float ox = x + LABEL, th = Math.min(13, rowH - 2);
		for (int i = 0; i < names.length; i++) {
			float cw = width(font, names[i], UI) + 22;
			boolean hover = !locked && mx >= ox && my >= cy - th / 2 && mx < ox + cw && my < cy + th / 2;
			if (on[i]) card(g, ox, cy - th / 2, cw, th, th / 2, a(fade(TEAL, 0.22F)), a(fade(TEAL, 0.7F)));
			else card(g, ox, cy - th / 2, cw, th, th / 2, a(hover ? 0x22FFFFFF : 0x12FFFFFF), a(0x26FFFFFF));
			if (on[i]) dot(g, ox + 7, cy, 2.2F, a(TEAL));
			else {
				dot(g, ox + 7, cy, 2.2F, a(0x55FFFFFF));
				dot(g, ox + 7, cy, 1.4F, a(0xFF1A1A20));
			}
			text(g, font, names[i], UI, ox + 13, cy, a(on[i] ? TEXT : locked ? DIM : SOFT));
			if (!locked) {
				final int idx = i;
				hits.add(new Hit(ox, cy - th / 2, cw, th, () -> flip.pick(idx)));
			}
			ox += cw + 4;
		}
		return y + rowH;
	}

	private static float max(float[] v) {
		float m = 0;
		for (float f : v) m = Math.max(m, f);
		return m;
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
				if (h.contains(event.x(), event.y())) {
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
				picked = org.lwjgl.util.tinyfd.TinyFileDialogs.tinyfd_selectFolderDialog("Where should Lightlens save videos, photos and GIFs?", start);
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
					message = "No folder picked. Press Choose to pick one.";
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
		install = "Downloading FFmpeg…";
		installProgress = 0;
		Thread t = new Thread(() -> {
			try {
				Ffmpeg.install((done, total) -> {
					if (total > 0) {
						installProgress = done / (float) total;
						install = "Downloading FFmpeg " + (done * 100 / total) + "%";
					} else install = "Downloading FFmpeg " + Sizes.bytes(done);
				});
				install = "Checking FFmpeg…";
				installProgress = -1;
				String exe = Ffmpeg.findNow();
				if (exe != null) Ffmpeg.probe(exe);
				install = null;
			} catch (Exception e) {
				Lens.LOG.error("Getting FFmpeg failed", e);
				installProgress = -1;
				install = "Download failed: " + e.getMessage();
			}
		}, "Lightlens FFmpeg download");
		t.setDaemon(true);
		t.start();
	}
}
