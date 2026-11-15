package com.insidethecube.orbitarena.client;

import com.insidethecube.orbitarena.OrbitArena;
import com.insidethecube.orbitarena.net.Payloads;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * The Bridge's score, top left, as on the videos: a banner for each team with a face, the name and the
 * goals (2/5), yours over theirs, and the clock if there's a time limit.
 */
public final class BridgeHud {
	private BridgeHud() {}

	private static Payloads.BridgeScore score;

	public static void set(Payloads.BridgeScore s) {
		score = s;
	}

	public static void clear() {
		score = null;
	}

	public static void render(GuiGraphics g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		if (score == null || mc.player == null || mc.level == null || mc.level.dimension() != OrbitArena.ARENA || !Crowd.onBridge() || Cinematic.active()
				|| mc.options.hideGui) {
			return;
		}
		Font font = mc.font;
		int x = 6, y = 6;
		// Your team's banner on top (with your face), then theirs.
		boolean blue = score.you() == 0;
		int blueFill = 0xE01A2A6B, blueEdge = 0xFF4A7BFF, redFill = 0xE06B1A22, redEdge = 0xFFFF4A5A;
		banner(g, font, x, y, blue ? blueFill : redFill, blue ? blueEdge : redEdge, blue ? score.blueName() : score.redName(), blue ? score.blue() : score.red(),
				score.toWin(), true, 0);
		banner(g, font, x, y + 26, blue ? redFill : blueFill, blue ? redEdge : blueEdge, blue ? score.redName() : score.blueName(), blue ? score.red() : score.blue(),
				score.toWin(), false, score.theirLook());
		if (score.secondsLeft() >= 0) {
			String t = score.secondsLeft() / 60 + ":" + String.format("%02d", score.secondsLeft() % 60);
			int w = font.width(t) + 12, cx = g.guiWidth() / 2;
			g.fill(cx - w / 2, 4, cx + w / 2, 18, 0xC0101018);
			g.drawString(font, t, cx - font.width(t) / 2, 7, score.secondsLeft() <= 30 ? 0xFFFF6060 : 0xFFFFFFFF, true);
		}
	}

	/** A team's banner: dark panel, coloured edge, the face, the name, the goals in green. */
	private static void banner(GuiGraphics g, Font font, int x, int y, int fill, int edge, String name, int goals, int toWin, boolean you, int look) {
		String n = name.toUpperCase(java.util.Locale.ROOT), sc = goals + "/" + toWin;
		int w = 30 + font.width(n) + 10 + font.width(sc) * 2 + 10, h = 22;
		g.fill(x - 1, y - 1, x + w + 1, y + h + 1, edge);
		g.fill(x, y, x + w, y + h, fill);
		g.fill(x, y, x + w, y + 1, 0x40FFFFFF);
		Minecraft mc = Minecraft.getInstance();
		if (you && mc.player != null) {
			PlayerFaceRenderer.draw(g, mc.player.getSkin(), x + 3, y + 3, 16);
		} else {
			Identifier tex = OrbitArena.id("textures/entity/fighter/fighter_" + Math.floorMod(look, FighterRenderer.LOOKS) + ".png");
			g.blit(RenderPipelines.GUI_TEXTURED, tex, x + 3, y + 3, 8, 8, 16, 16, 8, 8, 64, 64);
		}
		g.drawString(font, n, x + 24, y + 7, 0xFFFFFFFF, true);
		g.pose().pushMatrix();
		g.pose().translate(x + 30 + font.width(n), y + 3);
		g.pose().scale(2, 2);
		g.drawString(font, sc, 0, 0, 0xFF3DFF5A, true);
		g.pose().popMatrix();
	}
}
