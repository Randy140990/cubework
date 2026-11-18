package com.insidethecube.retromon.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.insidethecube.retromon.engine.Species;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;

/**
 * A Pokémon's Pokédex entry (shown the first time you catch one), seen on the Pokédex itself: the red case
 * with its lens and lights, and on its screen the Pokémon in 3D turning on a stand, its number, name, kind,
 * types, height and weight, and the entry.
 */
public class DexScreen extends GameScreen {
	private static Map<Integer, String[]> entries;
	private final Species sp;
	private final String[] entry;
	private final Runnable then;

	public DexScreen(String species, Runnable then) {
		super("Pokédex");
		this.sp = Species.ALL.get(species);
		this.entry = sp == null ? null : entries().get(sp.dex());
		this.then = then;
		Music.jingle("dex_page_added");
	}

	/** The entries by number: category, height, weight (lbs), text. */
	public static Map<Integer, String[]> entries() {
		if (entries == null) {
			entries = new HashMap<>();
			for (String[] r : Species.table("dex.tsv")) {
				try {
					entries.put(Integer.parseInt(r[0].trim()), new String[] {r[1], r[2], r[3], r.length > 4 ? r[4] : ""});
				} catch (NumberFormatException ignored) {
					// A header or a stray line.
				}
			}
		}
		return entries;
	}

	/** Closing: ticks into the fade to white (-1 while open). */
	private int closing = -1;
	private static final int FADE = 8;

	private void done() {
		if (closing >= 0) return;
		Sounds.press();
		Sounds.stopVoice();
		closing = 0;
	}

	@Override
	public void tick() {
		super.tick();
		// Its cry, then the Pokédex reads its entry (as on TV).
		if (ticks == 12 && sp != null) Sounds.cry(sp.id(), 1F);
		if (ticks == 30 && sp != null) Sounds.dexVoice(sp.id());
		if (closing >= 0 && ++closing >= FADE) {
			closing = -1;
			then.run();
		}
	}

	@Override
	public void removed() {
		Sounds.stopVoice();
		super.removed();
	}

	@Override
	protected boolean key(KeyEvent e) {
		if (ticks > 10 && (Ui.isA(e) || Ui.isB(e))) done();
		return true;
	}

	@Override
	protected void clickAnywhere() {
		if (ticks > 10) done();
	}

	@Override
	protected void back() {
		clickAnywhere();
	}

	@Override
	protected void draw(GuiGraphics g, float partial) {
		float t = ticks + partial, secs = t / 20F;
		float in = Math.min(1, t / 8F);
		g.fill(0, 0, width, height, Ui.alpha(0xF00A0E1C, in));
		if (sp == null) return;
		// The Pokédex itself: a red case, the lens and lights along the top, the screen inside.
		int m = 10 * s, x = m, y = m + Math.round((1 - in) * 12 * s), w = width - 2 * m, h = height - 2 * m;
		Ui.round(g, x + 2 * s, y + 3 * s, w, h, 0x60000000, 6 * s);
		Ui.round(g, x, y, w, h, 0xFF7E1820, 6 * s);
		Ui.rrv(g, x + s, y + s, w - 2 * s, h - 2 * s, 8.6F * s, 0xFFE0404A, 0xFFB02630);
		Ui.rr(g, x + 9 * s, y + 2 * s, w - 18 * s, s, s / 2F, 0x50FFFFFF);
		int lens = 16 * s, lx = x + 10 * s, ly = y + 6 * s;
		Ui.round(g, lx - 2 * s, ly - 2 * s, lens + 4 * s, lens + 4 * s, 0xFFF4F6FF, lens / 2);
		Ui.round(g, lx, ly, lens, lens, 0xFF2A7FE0, lens / 2 - s);
		Ui.round(g, lx + 3 * s, ly + 3 * s, lens / 3, lens / 3, 0xC0FFFFFF, lens / 6);
		int[] leds = {0xFFFF5A5A, 0xFFFFD45A, 0xFF5CD65C};
		for (int i = 0; i < 3; i++) {
			boolean lit = (ticks / 6 + i) % 3 == 0;
			Ui.round(g, lx + lens + 10 * s + i * 9 * s, ly + 2 * s, 6 * s, 6 * s, lit ? leds[i] : Ui.darker(leds[i], 0.55F), 3 * s);
		}
		Ui.draw(g, font, Ui.bold("POKéDEX"), x + w - 14 * s - Ui.boldWidth(font, "POKéDEX", s), y + 9 * s, 0xFFFFFFFF, s, true);
		// The screen.
		int sx = x + 10 * s, sy = y + 28 * s, sw = w - 20 * s, sh = h - 38 * s;
		Ui.round(g, sx - 2 * s, sy - 2 * s, sw + 4 * s, sh + 4 * s, 0xFF3A0C10, 3 * s);
		Ui.rrv(g, sx, sy, sw, sh, 3.4F * s, 0xFFEAF6F2, 0xFFC4DED6);
		for (int gx = sx + 12 * s; gx < sx + sw; gx += 12 * s) g.fill(gx, sy, gx + 1, sy + sh, 0x14205050);
		for (int gy = sy + 12 * s; gy < sy + sh; gy += 12 * s) g.fill(sx, gy, sx + sw, gy + 1, 0x14205050);
		// Left: the Pokémon on a turntable, turning slowly.
		int lw = sw * 9 / 20, cx = sx + lw / 2, feet = sy + sh - 22 * s, tall = Math.min(sh - 50 * s, lw * 4 / 5);
		Ui.glow(g, cx, feet - tall / 2, lw / 2 - 6 * s, tall / 2 + 6 * s, 0x10FFFFFF);
		Ui.round(g, cx - lw * 2 / 5, feet - 6 * s, lw * 4 / 5, 12 * s, 0xFF9CBCB4, 6 * s);
		Ui.round(g, cx - lw * 3 / 10, feet - 4 * s, lw * 3 / 5, 8 * s, 0xFFB8D4CC, 4 * s);
		Figures.Batch b = Figures.batch(sx, sy, sx + sw, sy + sh);
		float hop = t < 16 ? (float) Math.abs(Math.sin(t / 8F * Math.PI)) * 8 * s : 0;
		b.mon(sp.id(), cx, feet, tall).at(secs).pitch(8).spin(-25 + (float) Math.sin(secs * 0.6) * 35).hop(hop).action("cry", secs - 0.4F);
		b.draw(g);
		Ui.draw(g, font, Ui.bold("No. " + String.format("%03d", sp.dex())), sx + 8 * s, sy + 7 * s, 0xFF3A4A60, s, false);
		// A scan line down the screen as the entry comes in.
		if (t < 14) {
			int scan = sy + Math.round(sh * t / 14F);
			g.fillGradient(sx, Math.max(sy, scan - 18 * s), sx + sw, scan, 0x0060E0FF, 0x7060E0FF);
			g.fill(sx, scan, sx + sw, scan + Math.max(1, s), 0xE0FFFFFF);
		}
		// Right: the page.
		int px = sx + lw + 6 * s, pw = sw - lw - 14 * s, py = sy + 10 * s;
		Ui.round(g, px, py, pw, 20 * s, 0xFFE0404A, 3 * s);
		Ui.draw(g, font, Ui.bold(sp.name()), px + 8 * s, py + 6 * s, 0xFFFFFFFF, s, true);
		String category = entry == null ? "" : entry[0] + " POKéMON";
		Ui.draw(g, font, Ui.ui(category), px + 2 * s, py + 26 * s, 0xFF3A4A60, s, false);
		int cxp = px + 2 * s;
		cxp += Ui.chip(g, font, sp.type1().label.toUpperCase(), cxp, py + 38 * s, Ui.typeColor(sp.type1()), s) + 3 * s;
		if (sp.type2() != null && sp.type2() != sp.type1()) Ui.chip(g, font, sp.type2().label.toUpperCase(), cxp, py + 38 * s, Ui.typeColor(sp.type2()), s);
		if (entry != null) {
			int ty = py + 54 * s;
			Ui.round(g, px, ty, pw / 2 - 2 * s, 14 * s, 0x30205050, 2 * s);
			Ui.round(g, px + pw / 2 + 2 * s, ty, pw / 2 - 2 * s, 14 * s, 0x30205050, 2 * s);
			Ui.draw(g, font, Ui.ui("HT  " + entry[1]), px + 6 * s, ty + 3 * s, 0xFF26304A, s, false);
			Ui.draw(g, font, Ui.ui("WT  " + entry[2] + " lb"), px + pw / 2 + 8 * s, ty + 3 * s, 0xFF26304A, s, false);
			int ey = ty + 20 * s, eh = sy + sh - 8 * s - ey;
			Ui.round(g, px, ey, pw, eh, 0xF8FFFFFF, 3 * s);
			List<String> lines = Ui.wrap(font, entry[3], (pw - 16 * s) / s);
			for (int i = 0; i < lines.size() && i * 11 * s + 18 * s < eh; i++)
				Ui.draw(g, font, Ui.ui(lines.get(i)), px + 8 * s, ey + 7 * s + i * 11 * s, Ui.CREAM_INK, s, false);
		}
		// Registered: a stamp that drops in.
		if (t > 10) {
			float k = Math.min(1, (t - 10) / 5F);
			String reg = "REGISTERED";
			int rw = Ui.boldWidth(font, reg, s) + 12 * s, rx = sx + lw / 2 - rw / 2, ry = sy + 22 * s;
			Ui.round(g, rx, ry, rw, 14 * s, Ui.alpha(0xFF2A9A5A, k), 3 * s);
			Ui.draw(g, font, Ui.bold(reg), rx + 6 * s, ry + 3 * s, Ui.alpha(0xFFFFFFFF, k), s, false);
		}
		if (ticks > 10 && (ticks / 10) % 2 == 0) Ui.triangle(g, x + w - 12 * s, y + h - 8 * s, 7 * s, 2, 0xFFFFFFFF);
		// In from white, out to white.
		float white = t < FADE ? 1 - t / FADE : 0;
		if (closing >= 0) white = Math.min(1, (closing + partial) / FADE);
		if (white > 0) g.fill(0, 0, width, height, Ui.alpha(0xFFFFFFFF, white));
	}
}
