package com.insidethecube.retromon.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * A Minecraft skin drawn flat from the front, as a figure for screens with no world behind them (the
 * new-game intro): the professor from the waist up as in the games, you and your rival in full. Each part's
 * front face, with its outer layer on top, scaled up crisp.
 */
public final class SkinFigure {
	private SkinFigure() {}

	private static void face(GuiGraphics g, Identifier tex, int x, int y, int u, int v, int w, int h, int k) {
		g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, u, v, w * k, h * k, w, h, 64, 64);
	}

	/**
	 * Draws the skin at (x, y), k screen pixels to a skin pixel: the head, then the body and arms, then (if
	 * full) the legs. The figure is 16 skin pixels wide; 32 tall in full, 20 to the waist. bob moves the head.
	 */
	public static void draw(GuiGraphics g, Identifier tex, int x, int y, int k, boolean full, int bob) {
		// Head and its hat layer.
		face(g, tex, x + 4 * k, y + bob, 8, 8, 8, 8, k);
		face(g, tex, x + 4 * k, y + bob, 40, 8, 8, 8, k);
		int by = y + 8 * k;
		int rows = full ? 12 : 8;
		// Body and jacket layer, arms and sleeves (the right arm on the left of the picture).
		face(g, tex, x + 4 * k, by, 20, 20, 8, rows, k);
		face(g, tex, x + 4 * k, by, 20, 36, 8, rows, k);
		face(g, tex, x, by, 44, 20, 4, rows, k);
		face(g, tex, x, by, 44, 36, 4, rows, k);
		face(g, tex, x + 12 * k, by, 36, 52, 4, rows, k);
		face(g, tex, x + 12 * k, by, 52, 52, 4, rows, k);
		if (!full) return;
		int ly = by + 12 * k;
		face(g, tex, x + 4 * k, ly, 4, 20, 4, 12, k);
		face(g, tex, x + 4 * k, ly, 4, 36, 4, 12, k);
		face(g, tex, x + 8 * k, ly, 20, 52, 4, 12, k);
		face(g, tex, x + 8 * k, ly, 4, 52, 4, 12, k);
	}

	/** A soft oval shadow under a figure standing at (cx, y). */
	public static void shadow(GuiGraphics g, int cx, int y, int w, int s) {
		for (int i = 0; i < 4; i++) {
			int ww = w - i * w / 6;
			g.fill(cx - ww / 2, y + i * s, cx + ww / 2, y + (i + 1) * s, 0x30000000);
		}
	}
}
