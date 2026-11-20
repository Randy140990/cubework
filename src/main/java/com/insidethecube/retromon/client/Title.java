package com.insidethecube.retromon.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;

/**
 * The title, as the UI Viewer has it (signed off by John, 20 Nov), our look: shot like a film's opening. The
 * Minecraft meadow behind, out of focus (baked from the viewer: the sky, the clouds drifting across it, the meadow
 * in front of them); your partner big and sharp on the right, close enough that its feet are below the frame;
 * grass blades swaying close up across the foot of the frame; the sun off the screen past the top right corner,
 * never seen, only its light: the sky lighter on that side and the lens's faint rings near the top, drifting as if
 * the camera is held; a soft vignette; RETROMON with A FAN-MADE ADVENTURE rising in; PRESS SPACE; the studio and
 * the version at the foot.
 */
public final class Title {
	private Title() {}

	/** The baked clouds: where each starts (canvas pixels), how wide and tall, its middle, its drift (px a second) and its wrap. */
	private static final float[][] CLOUDS = {
			// left, top, w, h, middle x, speed, wrap from, wrap to (its middle's)
			{196, 164, 222, 70, 316.7F, 6.51F, -227.7F, 1507.7F}, {563, 162, 237, 70, 679.1F, 6.91F, -97.4F, 1377.4F},
			{917, 203, 169, 60, 992.2F, 9.25F, -181.9F, 1461.9F}, {489, 167, 173, 58, 578.6F, 8.41F, -1.1F, 1281.1F},
			{1042, 197, 179, 59, 1118.6F, 10.56F, -64.0F, 1344.0F}, {364, 185, 158, 65, 448.3F, 16.62F, -344.9F, 1624.9F},
			{781, 141, 198, 70, 872.6F, 17.04F, -268.7F, 1548.7F}, {117, 177, 220, 62, 238.1F, 13.97F, -37.2F, 1317.2F}, {633, 171, 203, 59, 729.8F, 12.86F, 68.2F, 1211.8F}};
	private static final Identifier SKY = V.tex("title_sky"), MEADOW = V.tex("title_meadow"), LOGO = V.tex("title_logo"), BLADES0 = V.tex("title_blades0"),
			BLADES1 = V.tex("title_blades1"), VEIL = V.tex("title_veil"), RING = V.tex("title_ring"), DISC = V.tex("title_disc"), VIGNETTE = V.tex("title_vignette");
	private static final Identifier[] CLOUD = new Identifier[9];
	/** The lens's ghosts: how far along the line from the sun (0) to near the top left (1), size, ring (true) or disc, colour. */
	private static final Object[][] GHOSTS = {{0.25F, 140, false, 0xFFF0CE}, {0.35F, 40, false, 0xFFFFF4}, {0.45F, 100, true, 0}, {0.55F, 56, false, 0xB4ECDC},
			{0.65F, 26, false, 0xFFF4D2}, {0.78F, 150, true, 0}, {0.9F, 64, false, 0xD2C4FF}};
	private static final float SUN_X = 1360, SUN_Y = -120, TO_X = 300, TO_Y = 230;

	/** The whole window, cover-fitted (its middle the canvas's middle): x, y, w, h on the canvas. */
	private static float[] cover() {
		float wx = -V.ox / V.k, wy = -V.oy / V.k, ww = V.gw / V.k, wh = V.gh / V.k;
		float sc = Math.max(ww / V.W, wh / V.H), w = V.W * sc, h = V.H * sc;
		return new float[] {wx + (ww - w) / 2, wy + (wh - h) / 2, w, h, sc};
	}

	/** The title. secs: since the title came up; partner: the creature that stands on it. */
	public static void draw(GuiGraphics g, float secs, String partner, String version) {
		float[] c = cover();
		// A canvas point to the cover (the backdrop layers are the canvas, scaled to cover the window).
		float sc = c[4], X = c[0], Y = c[1];
		V.img(g, SKY, X, Y, c[2], c[3], 640, 360, 0xFFFFFFFF);
		for (int i = 0; i < CLOUDS.length; i++) {
			float[] cl = CLOUDS[i];
			if (CLOUD[i] == null) CLOUD[i] = V.tex("title_cloud" + i);
			// Drifting right, coming round again from the left (as the viewer's clouds wrap at x 80).
			float span = cl[7] - cl[6], mid = cl[4], m = cl[6] + Math.floorMod((int) ((mid - cl[6] + secs * cl[5]) * 10), (int) (span * 10)) / 10F;
			float left = cl[0] + (m - mid);
			V.img(g, CLOUD[i], X + left * sc, Y + cl[1] * sc, cl[2] * sc, cl[3] * sc, (int) cl[2], (int) cl[3], 0xFFFFFFFF);
		}
		// The meadow (scale 1.02 about its middle, as the viewer's canvas is).
		float mw = c[2] * 1.02F, mh = c[3] * 1.02F;
		V.img(g, MEADOW, X - (mw - c[2]) / 2, Y - (mh - c[3]) / 2, mw, mh, 960, 540, 0xFFFFFFFF);
		// Your partner: 0.95 blocks tall, on the right, the camera low and close (look3(-0.82, 0.66, 2.35, -0.82, 0.6, 0)).
		Scenes.Cam cam = new Scenes.Cam().look(-0.82, 0.66, 2.35, -0.82, 0.6, 0);
		Figures.Batch b = Figures.batch(V.bx, V.by, V.bx + V.gw, V.by + V.gh);
		BedrockModel.Def def = BedrockModel.def(partner);
		double h = def == null ? 1 : Math.max(0.5, def.height);
		// (A touch bigger and higher than the viewer's 0.95: its model's height in the game runs over its body, so it
		// came out small and low.)
		Figures.Fig f = Scenes.place(b, cam, null, partner, 0, 0.05, 0, Math.PI - 0.38, secs);
		f.perBlock(f.perBlock * (float) (0.95 * 1.22 / h));
		b.draw(g);
		// The grass close up, swaying (the viewer's skewX about their foot, 4.2 and 5.5 seconds there and back).
		blades(g, BLADES0, 0, 568, 1280, 152, (float) Math.toRadians(sway(secs, 4.2F, 0)), X, Y, sc);
		blades(g, BLADES1, 0, 473, 1280, 247, (float) Math.toRadians(sway(secs, 5.5F, -2)), X, Y, sc);
		// The sun's light, from past the top right corner.
		float sx = SUN_X + (float) (Math.sin(secs * 0.23) * 14 + Math.sin(secs * 0.61) * 3), sy = SUN_Y + (float) Math.cos(secs * 0.17) * 8;
		float shine = 0.88F + 0.08F * (float) Math.sin(secs * 0.9) + 0.04F * (float) Math.sin(secs * 2.3);
		V.img(g, VEIL, sx - 525, sy - 400, 1050, 800, 420, 320, Ui.alpha(0xFFFFFFFF, shine));
		for (Object[] gh : GHOSTS) {
			float at = (float) gh[0], size = (int) gh[1];
			float gx = sx + (TO_X - sx) * at, gy = sy + (TO_Y - sy) * at, a = (0.7F + 0.3F * (float) Math.sin(secs * 0.8 + at * 5)) * shine;
			if ((boolean) gh[2]) V.img(g, RING, gx - size / 2, gy - size / 2, size, size, 256, 256, Ui.alpha(0xFFFFFFFF, a));
			else V.img(g, DISC, gx - size / 2, gy - size / 2, size, size, 128, 128, Ui.alpha(0xFF000000 | (int) gh[3], a));
		}
		V.img(g, VIGNETTE, X, Y, c[2], c[3], 320, 180, 0xFFFFFFFF);
		// RETROMON and its tag, rising in over 1.4 seconds.
		float k = Math.min(1, secs / 1.4F), e = 1 - (float) Math.pow(1 - k, 3), rise = 18 * (1 - e);
		V.img(g, LOGO, 70 + 17, 60 + 1.5F + rise, 849, 235.5F, 1698, 471, Ui.alpha(0xFFFFFFFF, e));
		// PRESS SPACE, in white spaced capitals, SPACE in its own outlined key, pulsing.
		String press = Lang.t("Press {0}").toUpperCase();
		String[] parts = press.split("\\{0\\}", -1);
		float pulse = 0.675F + 0.275F * (float) Math.cos(secs / 2.4F * Math.PI * 2);
		float kw = V.trackedWidthIn(V.ARCHIVO, "SPACE", 13, 4) + 28, w0 = V.trackedWidthIn(V.ARCHIVO, parts[0].trim(), 17, 7),
				w1 = parts.length > 1 ? V.trackedWidthIn(V.ARCHIVO, parts[1].trim(), 17, 7) : 0;
		float total = w0 + (w0 > 0 ? 10 : 0) + kw + (w1 > 0 ? 10 : 0) + w1, px = 640 - total / 2, cy = 640 + 15;
		int white = Ui.alpha(0xFFFFFFFF, pulse), dark = Ui.alpha(0xB3001414, pulse * 0.6F);
		if (w0 > 0) {
			V.trackedIn(g, V.ARCHIVO, parts[0].trim(), px + 1, V.capsTop(cy, 17) + 2, 17, dark, 7);
			V.trackedIn(g, V.ARCHIVO, parts[0].trim(), px, V.capsTop(cy, 17), 17, white, 7);
			px += w0 + 10;
		}
		V.ring(g, px, cy - 15, kw, 30, 8, 2, Ui.alpha(0xD9FFFFFF, pulse));
		V.trackedIn(g, V.ARCHIVO, "SPACE", px + 14, V.capsTop(cy, 13), 13, white, 4);
		px += kw + 10;
		if (w1 > 0) V.trackedIn(g, V.ARCHIVO, parts[1].trim(), px, V.capsTop(cy, 17), 17, white, 7);
		int foot = 0xCCFFFFFF;
		V.trackedIn(g, V.ARCHIVO, "INSID3 TH3 CUB3", 28, 684, 12, foot, 4);
		String ver = "VERSION " + version;
		V.trackedIn(g, V.ARCHIVO, ver, 1260 - V.trackedWidthIn(V.ARCHIVO, ver, 12, 4), 684, 12, foot, 4);
	}

	/** The viewer's alternate sway: skewX from -1.2 to 1.6 degrees and back (ease-in-out), period seconds each way. */
	private static float sway(float secs, float period, float delay) {
		float t = (secs - delay) / period, k = (float) Math.floorMod((long) Math.floor(t), 2L) == 0 ? t - (float) Math.floor(t) : 1 - (t - (float) Math.floor(t));
		float e = k * k * (3 - 2 * k);
		return -1.2F + 2.8F * e;
	}

	/** A band of grass blades (canvas x, y, w, h), leaning by a skew about its foot. */
	private static void blades(GuiGraphics g, Identifier id, float x, float y, float w, float h, float skew, float X, float Y, float sc) {
		float fx = X + x * sc, fy = Y + y * sc, fw = w * sc, fh = h * sc;
		g.pose().pushMatrix();
		// About the foot: x' = x + tan(skew) * (y - foot).
		float footY = V.y(fy + fh), t = (float) Math.tan(skew);
		g.pose().translate(0, footY);
		g.pose().mul(new org.joml.Matrix3x2f(1, 0, -t, 1, 0, 0));
		g.pose().translate(0, -footY);
		V.img(g, id, fx, fy, fw, fh, (int) w, (int) h, 0xFFFFFFFF);
		g.pose().popMatrix();
	}
}
