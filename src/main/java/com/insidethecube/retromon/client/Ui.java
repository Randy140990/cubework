package com.insidethecube.retromon.client;

import java.util.ArrayList;
import java.util.List;

import com.insidethecube.retromon.engine.Type;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import com.insidethecube.retromon.Retromon;
import org.lwjgl.glfw.GLFW;

/**
 * The look of Retromon's screens: deep navy glass panels with soft rounded corners, clean white text (Poppins,
 * a rounded modern font, as the newer games use), cream highlights for what's picked, colour for what you can
 * press (FIGHT red, BAG amber, POKéMON green, RUN blue), type colours on moves, smooth HP and EXP bars.
 */
public final class Ui {
	private Ui() {}

	public static final int PANEL = 0xEC141B2E, PANEL_SOFT = 0xD0141B2E, EDGE = 0xFF33416A, EDGE_LIGHT = 0xFF5A6DA3, SHINE = 0x22FFFFFF;
	public static final int TEXT = 0xFFF4F6FF, MUTED = 0xFFA9B4D4, DIM = 0xFF6B7697, INK = 0xFF1A1F30;
	public static final int RED = 0xFFE5525C, AMBER = 0xFFF0B43C, GREEN = 0xFF41BF84, BLUE = 0xFF4A8FE0, GOLD = 0xFFFFD45A, CYAN = 0xFF47C6F2;
	public static final int HP_GREEN = 0xFF4CD27E, HP_YELLOW = 0xFFF2C744, HP_RED = 0xFFEF4F4F, TRACK = 0xFF0B1020;
	public static final String CURSOR = "▶";
	/** The cream of a picked row or button (the newer games' highlight), and the dark text on it. */
	public static final int CREAM = 0xFFFFF4D8, CREAM_INK = 0xFF26304A;

	/** How big to draw: 1 on a small window or a big GUI scale, 2 or more when there's room. */
	public static int scale(int screenHeight) {
		return Math.max(1, Math.min(3, screenHeight / 190));
	}

	/** A rounded rectangle (corners cut one or two pixels in), in one colour. */
	public static void round(GuiGraphics g, int x, int y, int w, int h, int color, int s) {
		int r = Math.max(1, s);
		g.fill(x + 2 * r, y, x + w - 2 * r, y + h, color);
		g.fill(x + r, y + r, x + 2 * r, y + h - r, color);
		g.fill(x + w - 2 * r, y + r, x + w - r, y + h - r, color);
		g.fill(x, y + 2 * r, x + r, y + h - 2 * r, color);
		g.fill(x + w - r, y + 2 * r, x + w, y + h - 2 * r, color);
	}

	/** A panel: an edge, the glass, and a faint sheen across the top. */
	public static void panel(GuiGraphics g, int x, int y, int w, int h, int s) {
		round(g, x, y, w, h, EDGE, s);
		round(g, x + s, y + s, w - 2 * s, h - 2 * s, PANEL, s);
		g.fill(x + 3 * s, y + 2 * s, x + w - 3 * s, y + 3 * s, SHINE);
	}

	/** A panel with a coloured strip down its left side (for menus and boxes that belong to something). */
	public static void tagged(GuiGraphics g, int x, int y, int w, int h, int s, int accent) {
		panel(g, x, y, w, h, s);
		g.fill(x + s, y + 3 * s, x + 3 * s, y + h - 3 * s, accent);
	}

	/** A button: its colour (brighter when picked or under the mouse), its label centred. */
	public static void button(GuiGraphics g, Font f, int x, int y, int w, int h, String label, int color, boolean on, int s) {
		round(g, x, y, w, h, on ? CREAM : darker(color, 0.55F), s);
		round(g, x + s, y + s, w - 2 * s, h - 2 * s, on ? color : darker(color, 0.8F), s);
		g.fill(x + 3 * s, y + 2 * s, x + w - 3 * s, y + 3 * s, 0x33FFFFFF);
		int lw = boldWidth(f, label, s);
		draw(g, f, bold(label), x + (w - lw) / 2, y + (h - 8 * s) / 2, TEXT, s, true);
	}

	/** A pill: a rounded row that's cream when picked (dark text) and glass when not, as the newer games' lists. */
	public static void pill(GuiGraphics g, int x, int y, int w, int h, boolean on, int s) {
		round(g, x, y, w, h, on ? CREAM : 0xB0141B2E, s);
		if (!on) g.fill(x + 2 * s, y, x + w - 2 * s, y + Math.max(1, s / 2), 0x26FFFFFF);
	}

	/**
	 * The cinema frame: the top and bottom of the screen darkening smoothly into the picture (no hard bars). t (0 to
	 * 1) is how far it has faded in: the whole frame fades in together, by its opacity.
	 */
	public static void cinema(GuiGraphics g, int width, int height, float t) {
		t = Math.max(0, Math.min(1, t));
		if (t <= 0) return;
		// Ease in, so it arrives softly.
		float k = t * t * (3 - 2 * t);
		int deep = Math.round(height * 0.075F), soft = Math.round(height * 0.12F);
		int a = (int) (0xE6 * k), mid = (int) (0x9C * k);
		g.fillGradient(0, 0, width, deep, a << 24, mid << 24);
		g.fillGradient(0, deep, width, deep + soft, mid << 24, 0);
		g.fillGradient(0, height - deep - soft, width, height - deep, 0, mid << 24);
		g.fillGradient(0, height - deep, width, height, mid << 24, a << 24);
	}

	/** A glow of colour around the edges of the screen (a hit's colour), strength 0 to 1. */
	public static void vignette(GuiGraphics g, int width, int height, int rgb, float strength) {
		if (strength <= 0) return;
		int a = (int) (Math.min(1, strength) * 0xB0);
		int c = (a << 24) | (rgb & 0xFFFFFF), clear = rgb & 0xFFFFFF;
		int e = Math.round(height * 0.22F);
		g.fillGradient(0, 0, width, e, c, clear);
		g.fillGradient(0, height - e, width, height, clear, c);
		// Sides: thin vertical strips stepping down in strength (the GUI only fades top to bottom).
		int side = Math.round(width * 0.12F), steps = 12;
		for (int i = 0; i < steps; i++) {
			int x0 = side * i / steps, x1 = side * (i + 1) / steps;
			int ai = (int) (a * (1 - (i + 0.5F) / steps) * 0.85F);
			int col = (ai << 24) | (rgb & 0xFFFFFF);
			g.fill(x0, 0, x1, height, col);
			g.fill(width - x1, 0, width - x0, height, col);
		}
	}

	/**
	 * A slanted bar (the newer games' buttons): a parallelogram leaning forward by lean pixels, filled top to
	 * bottom from one colour to another, with an outline if edge isn't 0.
	 */
	public static void slab(GuiGraphics g, int x, int y, int w, int h, int top, int bottom, int lean, int edge, int edgeW) {
		g.pose().pushMatrix();
		g.pose().translate(x + lean / 2F, y + h / 2F);
		g.pose().mul(new org.joml.Matrix3x2f(1, 0, -lean / (float) Math.max(1, h), 1, 0, 0));
		int hh = h / 2;
		if (edge != 0 && edgeW > 0) {
			g.fill(-edgeW, -hh - edgeW, w + edgeW, h - hh + edgeW, edge);
		}
		g.fillGradient(0, -hh, w, h - hh, top, bottom);
		// A sheen along the top.
		g.fill(1, -hh, w - 1, -hh + Math.max(1, h / 10), 0x38FFFFFF);
		g.pose().popMatrix();
	}

	/** A whole picture (texW by texH) drawn into a box, tinted with a colour (0xFFFFFFFF for none). */
	public static void pic(GuiGraphics g, net.minecraft.resources.Identifier id, int x, int y, int w, int h, int texW, int texH, int color) {
		g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, id, x, y, 0, 0, w, h, texW, texH, texW, texH, color);
	}

	/** A white picture (an icon) tinted with a colour. */
	public static void icon(GuiGraphics g, String name, int x, int y, int size, int color) {
		g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, Retromon.id("textures/gui/" + name + ".png"), x, y, 0, 0, size, size, 32, 32, 32, 32,
				color);
	}

	/**
	 * A speech bubble with an emote in it (heart, star, exclaim, note, happy), its tail pointing down at (cx,
	 * bottom). pop (0 to 1) is how far it has popped up.
	 */
	public static void bubble(GuiGraphics g, String emote, int cx, int bottom, int s, float pop) {
		if (pop <= 0) return;
		float k = pop < 1 ? (float) (1 + Math.sin(pop * Math.PI) * 0.25) * pop : 1;
		int size = Math.round(22 * s * k), half = size / 2;
		int top = bottom - size - Math.round(5 * s * k);
		round(g, cx - half - s, top - s, size + 2 * s, size + 2 * s, 0x50000000, 3 * s);
		round(g, cx - half, top, size, size, 0xFFFFFFFF, 3 * s);
		// The tail.
		for (int i = 0; i < 5 * s * k; i++) {
			int w = Math.max(0, Math.round(4 * s * k) - i);
			g.fill(cx - w, top + size + i, cx + w, top + size + i + 1, 0xFFFFFFFF);
		}
		int in = Math.round(size * 0.72F);
		icon(g, "emote/" + emote, cx - in / 2, top + (size - in) / 2, in, 0xFFFFFFFF);
	}

	/** A soft round glow (a light behind something), built up from see-through rounded boxes. */
	public static void glow(GuiGraphics g, int cx, int cy, int rx, int ry, int color) {
		for (int i = 0; i < 7; i++) {
			int x = rx - i * rx / 8, y = ry - i * ry / 8;
			round(g, cx - x, cy - y, 2 * x, 2 * y, color, Math.max(1, Math.min(x, y) / 3));
		}
	}

	/** A colour mixed towards white by k (0 to 1). */
	public static int lighter(int argb, float k) {
		int a = argb >>> 24, r = (argb >> 16) & 255, gr = (argb >> 8) & 255, b = argb & 255;
		return (a << 24) | ((int) (r + (255 - r) * k) << 16) | ((int) (gr + (255 - gr) * k) << 8) | (int) (b + (255 - b) * k);
	}

	public static int darker(int argb, float k) {
		int a = argb >>> 24, r = (argb >> 16) & 255, gr = (argb >> 8) & 255, b = argb & 255;
		return (a << 24) | ((int) (r * k) << 16) | ((int) (gr * k) << 8) | (int) (b * k);
	}

	public static int alpha(int argb, float a) {
		return ((int) (Math.max(0, Math.min(1, a)) * (argb >>> 24)) << 24) | (argb & 0xFFFFFF);
	}

	private static final Style FONT = Style.EMPTY.withFont(new FontDescription.Resource(Retromon.id("ui")));
	private static final Style BOLD = Style.EMPTY.withFont(new FontDescription.Resource(Retromon.id("ui_bold")));

	/** A Pokémon's portrait (drawn from its model), as a square of the given size. */
	public static void portrait(GuiGraphics g, String species, int x, int y, int size) {
		if (species == null || species.isEmpty()) return;
		g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, Retromon.id("textures/gui/mon/" + species + ".png"), x, y, 0, 0, size, size, 128, 128,
				128, 128);
	}

	/** A portrait as a dark silhouette (seen but not caught, in the Pokédex). */
	public static void silhouette(GuiGraphics g, String species, int x, int y, int size) {
		if (species == null || species.isEmpty()) return;
		g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, Retromon.id("textures/gui/mon/" + species + ".png"), x, y, 0, 0, size, size, 128, 128,
				128, 128, 0xFF1A2038);
	}

	/** Money as the games show it: ₽3,000. */
	public static String money(int n) {
		return com.insidethecube.retromon.engine.Battle.money(n);
	}

	/** Text in the interface font. */
	public static Component ui(String text) {
		return Component.literal(text).withStyle(FONT);
	}

	/** Text in the bold interface font (titles, names, buttons). */
	public static Component bold(String text) {
		return Component.literal(text).withStyle(BOLD);
	}

	public static void text(GuiGraphics g, Font f, String text, int x, int y, int color, int s) {
		draw(g, f, ui(text), x, y, color, s, true);
	}

	public static void plain(GuiGraphics g, Font f, String text, int x, int y, int color, int s) {
		draw(g, f, ui(text), x, y, color, s, false);
	}

	public static void shadowed(GuiGraphics g, Font f, String text, int x, int y, int color, int s) {
		draw(g, f, ui(text), x, y, color, s, true);
	}

	/** Bold text (a title or a name), with a soft shadow. */
	public static void heading(GuiGraphics g, Font f, String text, int x, int y, int color, int s) {
		draw(g, f, bold(text), x, y, color, s, true);
	}

	public static void draw(GuiGraphics g, Font f, Component c, int x, int y, int color, int s, boolean shadow) {
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(s, s);
		g.drawString(f, c, 0, 0, color, shadow);
		g.pose().popMatrix();
	}

	public static int width(Font f, String text, int s) {
		return f.width(ui(text)) * s;
	}

	public static int boldWidth(Font f, String text, int s) {
		return f.width(bold(text)) * s;
	}

	public static void centred(GuiGraphics g, Font f, String text, int cx, int y, int color, int s, boolean shadow) {
		int x = cx - width(f, text, s) / 2;
		if (shadow) shadowed(g, f, text, x, y, color, s);
		else plain(g, f, text, x, y, color, s);
	}

	public static void right(GuiGraphics g, Font f, String text, int rx, int y, int color, int s) {
		shadowed(g, f, text, rx - width(f, text, s), y, color, s);
	}

	/** Splits text into lines that fit a width (in unscaled font pixels). */
	public static List<String> wrap(Font f, String text, int width) {
		List<String> out = new ArrayList<>();
		for (String para : text.split("\n")) {
			StringBuilder line = new StringBuilder();
			for (String word : para.split(" ")) {
				String next = line.isEmpty() ? word : line + " " + word;
				if (f.width(ui(next)) > width && !line.isEmpty()) {
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

	public static int hpColor(float frac) {
		return frac > 0.5F ? HP_GREEN : frac > 0.2F ? HP_YELLOW : HP_RED;
	}

	/** A rounded bar: a dark track and the part filled. */
	public static void bar(GuiGraphics g, int x, int y, int w, int h, float frac, int color, int s) {
		round(g, x, y, w, h, 0xFF05080F, Math.max(1, s / 2));
		g.fill(x + s / 2 + 1, y + 1, x + w - s / 2 - 1, y + h - 1, TRACK);
		int fw = Math.round((w - 2) * Math.max(0, Math.min(1, frac)));
		if (frac > 0 && fw < 1) fw = 1;
		if (fw > 0) {
			g.fill(x + 1, y + 1, x + 1 + fw, y + h - 1, color);
			g.fill(x + 1, y + 1, x + 1 + fw, y + 1 + Math.max(1, (h - 2) / 3), 0x40FFFFFF);
		}
	}

	/** An HP bar with its label. */
	public static void hp(GuiGraphics g, Font f, int x, int y, int w, float hp, int max, int s) {
		plain(g, f, "HP", x, y, GOLD, s);
		int bx = x + width(f, "HP", s) + 3 * s;
		float frac = max <= 0 ? 0 : hp / max;
		bar(g, bx, y + s, w - (bx - x), 5 * s, frac, hpColor(frac), s);
	}

	/** The colour of a type (as the later games colour their move buttons). */
	public static int typeColor(Type t) {
		if (t == null) return 0xFF8A8FA3;
		return switch (t) {
			case NORMAL -> 0xFF9DA0AA;
			case FIRE -> 0xFFF08436;
			case WATER -> 0xFF4D90D5;
			case ELECTRIC -> 0xFFE9C72E;
			case GRASS -> 0xFF5DB850;
			case ICE -> 0xFF6FCFD0;
			case FIGHTING -> 0xFFCE4069;
			case POISON -> 0xFFAB6AC8;
			case GROUND -> 0xFFD97746;
			case FLYING -> 0xFF8FA8DD;
			case PSYCHIC -> 0xFFF66F71;
			case BUG -> 0xFF90C12C;
			case ROCK -> 0xFFC5B78C;
			case GHOST -> 0xFF5269AC;
			case DRAGON -> 0xFF0A6DC4;
			case DARK -> 0xFF5A5366;
			case STEEL -> 0xFF5A8EA1;
			case CURSE -> 0xFF68A090;
		};
	}

	/** A small rounded tag (a type, a status). */
	public static int chip(GuiGraphics g, Font f, String text, int x, int y, int color, int s) {
		int w = width(f, text, s) + 6 * s;
		round(g, x, y, w, 10 * s, color, s);
		plain(g, f, text, x + 3 * s, y + s, 0xFFFFFFFF, s);
		return w;
	}

	public static int statusColor(String tag) {
		return switch (tag) {
			case "PSN" -> 0xFFA25BC7;
			case "BRN" -> 0xFFEF7A3C;
			case "FRZ" -> 0xFF5FC9D6;
			case "PAR" -> 0xFFD9B32E;
			case "SLP" -> 0xFF8C93A8;
			case "FNT" -> 0xFFB53A3A;
			default -> 0xFF8C93A8;
		};
	}

	// ---------- keys (every screen also works with the keyboard, like the games) ----------

	public static boolean isA(KeyEvent e) {
		int k = e.key();
		return k == GLFW.GLFW_KEY_SPACE || k == GLFW.GLFW_KEY_ENTER || k == GLFW.GLFW_KEY_KP_ENTER || k == GLFW.GLFW_KEY_Z || k == GLFW.GLFW_KEY_E;
	}

	public static boolean isB(KeyEvent e) {
		int k = e.key();
		return k == GLFW.GLFW_KEY_BACKSPACE || k == GLFW.GLFW_KEY_X || k == GLFW.GLFW_KEY_Q;
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
