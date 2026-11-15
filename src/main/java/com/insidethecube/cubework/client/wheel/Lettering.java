package com.insidethecube.cubework.client.wheel;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.insidethecube.cubework.Reg;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

/**
 * The wheel's lettering, in two book faces: Cinzel for the carved capitals and Alegreya's italic for the
 * lines under them (both SIL Open Font License, bundled with the mod). The game's own font is a pixel font
 * and can't be scaled smoothly, so every letter is drawn once, smoothed, at the exact size in screen pixels
 * it appears at, and placed on whole pixels (in three sub-pixel positions, so spacing stays even). Nothing
 * is stretched afterwards, so letters stay crisp at every interface scale. Letters share atlas pages.
 */
final class Lettering {
	private Lettering() {}

	private static final Logger LOGGER = LogUtils.getLogger();

	enum Face {
		/** Carved capitals, for names, labels and keys. */
		CAPS("cinzel-semibold.ttf", java.awt.Font.BOLD),
		/** The book's italic, for the line under a name. */
		ITALIC("alegreya-italic.ttf", java.awt.Font.ITALIC);

		final String file;
		/** The style of the system serif used if the face can't be read. */
		final int fallback;

		Face(String file, int fallback) {
			this.file = file;
			this.fallback = fallback;
		}
	}

	private static final int PAGE = 1024, PHASES = 3;

	private record Glyph(int page, int u, int v, int w, int h, int ox, int oy, float adv) {}

	private static final class Page {
		final Identifier id;
		final DynamicTexture tex;
		int penX = 1, penY = 1, rowH;
		boolean dirty = true;

		Page(int n) {
			id = Reg.id("wheel_lettering_" + n);
			tex = new DynamicTexture(() -> "Cubework wheel lettering " + n, PAGE, PAGE, true);
			NativeImage img = tex.getPixels();
			if (img != null) img.fillRect(0, 0, PAGE, PAGE, 0);
			Minecraft.getInstance().getTextureManager().register(id, tex);
		}
	}

	private static final List<Page> pages = new ArrayList<>();
	private static final Map<Long, Glyph> glyphs = new HashMap<>();
	private static final java.awt.Font[] faces = new java.awt.Font[Face.values().length];
	private static final float[] capRatio = new float[Face.values().length];
	private static final Map<Long, java.awt.Font> sized = new HashMap<>();
	private static final FontRenderContext FRC = new FontRenderContext(null, true, true);
	/** Coverage curve: a touch heavier than linear, so small light letters on dark wood stay solid. */
	private static final int[] GAMMA = new int[256];

	static {
		for (int i = 0; i < 256; i++) GAMMA[i] = (int) Math.round(255 * Math.pow(i / 255.0, 0.8));
	}

	private static java.awt.Font face(Face f) {
		int i = f.ordinal();
		if (faces[i] != null) return faces[i];
		java.awt.Font font;
		try (InputStream in = Lettering.class.getResourceAsStream("/assets/cubework/wheel/" + f.file)) {
			if (in == null) throw new IllegalStateException("missing");
			font = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, in);
		} catch (Throwable t) {
			LOGGER.warn("Cubework: couldn't read the wheel's {}, using a system serif", f.file);
			font = new java.awt.Font(java.awt.Font.SERIF, f.fallback, 1);
		}
		// How tall a capital is, as a share of the font size, so sizes are given as cap heights everywhere.
		Rectangle h = font.deriveFont(200f).createGlyphVector(FRC, "H").getPixelBounds(FRC, 0, 0);
		capRatio[i] = h.height > 0 ? h.height / 200f : 0.7f;
		faces[i] = font;
		return font;
	}

	/** Sizes are kept to quarter pixels, so nearby sizes share their letters. */
	private static int key(float capPx) {
		return Math.max(8, Math.min(4000, Math.round(capPx * 4)));
	}

	private static java.awt.Font font(Face f, int key) {
		long k = ((long) f.ordinal() << 32) | key;
		java.awt.Font c = sized.get(k);
		if (c != null) return c;
		java.awt.Font base = face(f);
		c = base.deriveFont(key / 4f / capRatio[f.ordinal()]);
		sized.put(k, c);
		return c;
	}

	private static Glyph glyph(Face f, int key, int phase, char ch) {
		long k = ((long) f.ordinal() << 60) | ((long) phase << 56) | ((long) key << 24) | ch;
		Glyph gl = glyphs.get(k);
		if (gl != null) return gl;
		java.awt.Font jf = font(f, key);
		if (!jf.canDisplay(ch)) jf = new java.awt.Font(java.awt.Font.SERIF, f.fallback, 1).deriveFont(jf.getSize2D());
		GlyphVector gv = jf.createGlyphVector(FRC, new char[] {ch});
		float adv = gv.getGlyphMetrics(0).getAdvanceX();
		float dx = phase / (float) PHASES;
		Rectangle b = gv.getPixelBounds(FRC, dx, 0);
		if (b.width <= 0 || b.height <= 0 || Character.isWhitespace(ch)) {
			gl = new Glyph(-1, 0, 0, 0, 0, 0, 0, adv);
			glyphs.put(k, gl);
			return gl;
		}
		int w = b.width + 2, h = b.height + 2;
		if (w > PAGE - 2 || h > PAGE - 2) {
			gl = new Glyph(-1, 0, 0, 0, 0, 0, 0, adv);
			glyphs.put(k, gl);
			return gl;
		}
		BufferedImage im = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = im.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g.setColor(Color.WHITE);
		g.drawGlyphVector(gv, 1 - b.x + dx, 1 - b.y);
		g.dispose();
		Page p = room(w, h);
		NativeImage img = p.tex.getPixels();
		if (img == null) return new Glyph(-1, 0, 0, 0, 0, 0, 0, adv);
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int a = GAMMA[im.getRGB(x, y) >>> 24];
				img.setPixel(p.penX + x, p.penY + y, (a << 24) | 0xFFFFFF);
			}
		}
		gl = new Glyph(pages.indexOf(p), p.penX, p.penY, w, h, b.x - 1, b.y - 1, adv);
		p.penX += w + 1;
		p.rowH = Math.max(p.rowH, h);
		p.dirty = true;
		glyphs.put(k, gl);
		return gl;
	}

	private static Page room(int w, int h) {
		Page p = pages.isEmpty() ? null : pages.get(pages.size() - 1);
		if (p != null && p.penX + w + 1 > PAGE) {
			p.penX = 1;
			p.penY += p.rowH + 1;
			p.rowH = 0;
		}
		if (p == null || p.penY + h + 1 > PAGE) {
			p = new Page(pages.size());
			pages.add(p);
		}
		return p;
	}

	/** Width in screen pixels, with track pixels added between letters. */
	static float width(Face f, float capPx, String s, float trackPx) {
		if (s == null || s.isEmpty()) return 0;
		int key = key(capPx);
		float w = 0;
		for (int i = 0; i < s.length(); i++) w += glyph(f, key, 0, s.charAt(i)).adv;
		return w + trackPx * (s.length() - 1);
	}

	/**
	 * Draws a line with its left end at x and its baseline at baseline, both in screen pixels; scale is the
	 * interface scale (screen pixels to a GUI unit), so the letters land on screen pixels exactly.
	 */
	static void draw(GuiGraphics g, Face f, float capPx, String s, float x, float baseline, float trackPx, int colour, float scale) {
		if (s == null || s.isEmpty() || (colour >>> 24) == 0) return;
		int key = key(capPx), base = Math.round(baseline);
		g.pose().pushMatrix();
		g.pose().scale(1f / scale, 1f / scale);
		float pen = x;
		for (int i = 0; i < s.length(); i++) {
			int whole = (int) Math.floor(pen), phase = Math.min(PHASES - 1, (int) ((pen - whole) * PHASES));
			Glyph gl = glyph(f, key, phase, s.charAt(i));
			if (gl.page >= 0) {
				g.blit(RenderPipelines.GUI_TEXTURED, pages.get(gl.page).id, whole + gl.ox, base + gl.oy, gl.u, gl.v, gl.w, gl.h, gl.w, gl.h, PAGE, PAGE, colour);
			}
			pen += gl.adv + trackPx;
		}
		g.pose().popMatrix();
	}

	/** Sends new letters to the graphics card; called after a frame's wheel is drawn, before it's shown. */
	static void flush() {
		for (Page p : pages) {
			if (!p.dirty) continue;
			p.dirty = false;
			try {
				p.tex.upload();
			} catch (Throwable t) {
				LOGGER.debug("Cubework: couldn't upload the wheel's lettering", t);
			}
		}
	}
}
