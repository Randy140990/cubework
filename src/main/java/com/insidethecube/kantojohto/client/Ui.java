package com.insidethecube.kantojohto.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;

/**
 * The look of the screens, plain for now: Gold and Silver's white boxes with a dark frame, black text, the
 * cursor, HP bars. Everything's drawn at a whole-number size so the pixel font stays crisp.
 */
public final class Ui {
	private Ui() {}

	public static final int INK = 0xFF202028, PAPER = 0xFFF8F8F0, FRAME = 0xFF384878, SHADE = 0xFFB8C0D8, GREY = 0xFF888890;
	public static final int HP_GREEN = 0xFF38C850, HP_YELLOW = 0xFFF0C030, HP_RED = 0xFFE83838, HP_BACK = 0xFF485058;
	public static final String CURSOR = "▶";

	/** How big to draw: 1 on a small window or a big GUI scale, 2 or more when there's room. */
	public static int scale(int screenHeight) {
		return Math.max(1, Math.min(3, screenHeight / 190));
	}

	public static void box(GuiGraphics g, int x, int y, int w, int h, int s) {
		g.fill(x, y, x + w, y + h, INK);
		g.fill(x + s, y + s, x + w - s, y + h - s, FRAME);
		g.fill(x + 2 * s, y + 2 * s, x + w - 2 * s, y + h - 2 * s, SHADE);
		g.fill(x + 3 * s, y + 3 * s, x + w - 3 * s, y + h - 3 * s, PAPER);
	}

	public static void text(GuiGraphics g, Font f, String text, int x, int y, int color, int s) {
		if (s == 1) {
			g.drawString(f, text, x, y, color, false);
			return;
		}
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(s, s);
		g.drawString(f, text, 0, 0, color, false);
		g.pose().popMatrix();
	}

	/** Light text with a shadow, for over the world or a picture. */
	public static void shadowed(GuiGraphics g, Font f, String text, int x, int y, int color, int s) {
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(s, s);
		g.drawString(f, text, 0, 0, color, true);
		g.pose().popMatrix();
	}

	public static int width(Font f, String text, int s) {
		return f.width(text) * s;
	}

	public static void centred(GuiGraphics g, Font f, String text, int cx, int y, int color, int s, boolean shadow) {
		int x = cx - width(f, text, s) / 2;
		if (shadow) shadowed(g, f, text, x, y, color, s);
		else text(g, f, text, x, y, color, s);
	}

	/** Splits text into lines that fit a width (in unscaled font pixels). */
	public static List<String> wrap(Font f, String text, int width) {
		List<String> out = new ArrayList<>();
		for (String para : text.split("\n")) {
			StringBuilder line = new StringBuilder();
			for (String word : para.split(" ")) {
				String next = line.isEmpty() ? word : line + " " + word;
				if (f.width(next) > width && !line.isEmpty()) {
					out.add(line.toString());
					line = new StringBuilder(word);
				} else {
					line = new StringBuilder(next);
				}
			}
			out.add(line.toString());
		}
		return out;
	}

	/** An HP bar: green, yellow under half, red under a fifth. */
	public static void hpBar(GuiGraphics g, Font f, int x, int y, int w, float hp, int max, int s) {
		text(g, f, "HP", x, y, INK, s);
		int bx = x + width(f, "HP", s) + 2 * s, bw = w - (bx - x), bh = 3 * s, by = y + 2 * s;
		g.fill(bx - s, by - s, bx + bw + s, by + bh + s, INK);
		g.fill(bx, by, bx + bw, by + bh, HP_BACK);
		float frac = max <= 0 ? 0 : Math.max(0, Math.min(1, hp / max));
		int color = frac > 0.5F ? HP_GREEN : frac > 0.2F ? HP_YELLOW : HP_RED;
		int fw = Math.round(bw * frac);
		if (hp > 0 && fw == 0) fw = 1;
		g.fill(bx, by, bx + fw, by + bh, color);
	}

	// ---------- keys (screens work with the keyboard, like the games) ----------

	public static boolean isA(KeyEvent e) {
		int k = e.key();
		return k == GLFW.GLFW_KEY_SPACE || k == GLFW.GLFW_KEY_ENTER || k == GLFW.GLFW_KEY_KP_ENTER || k == GLFW.GLFW_KEY_Z || k == GLFW.GLFW_KEY_E;
	}

	public static boolean isB(KeyEvent e) {
		int k = e.key();
		return k == GLFW.GLFW_KEY_BACKSPACE || k == GLFW.GLFW_KEY_ESCAPE || k == GLFW.GLFW_KEY_X || k == GLFW.GLFW_KEY_Q;
	}

	public static boolean isUp(KeyEvent e) {
		return e.key() == GLFW.GLFW_KEY_UP || e.key() == GLFW.GLFW_KEY_W;
	}

	public static boolean isDown(KeyEvent e) {
		return e.key() == GLFW.GLFW_KEY_DOWN || e.key() == GLFW.GLFW_KEY_S;
	}

	public static boolean isLeft(KeyEvent e) {
		return e.key() == GLFW.GLFW_KEY_LEFT || e.key() == GLFW.GLFW_KEY_A;
	}

	public static boolean isRight(KeyEvent e) {
		return e.key() == GLFW.GLFW_KEY_RIGHT || e.key() == GLFW.GLFW_KEY_D;
	}
}
