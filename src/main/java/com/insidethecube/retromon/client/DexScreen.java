package com.insidethecube.retromon.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.insidethecube.retromon.engine.Species;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;

/**
 * A Pokémon's Pokédex entry (shown the first time you catch one): the Pokémon itself on the stage, and its
 * page beside it: number, name, what kind of Pokémon it is, its types, height and weight, and the entry.
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

	private void done() {
		Sounds.press();
		then.run();
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
		Ui.cinema(g, width, height, Math.min(1, (ticks + partial) / 8F));
		if (sp == null) return;
		float in = Math.min(1, (ticks + partial) / 10F);
		int w = Math.min(width / 2 - 10 * s, 200 * s), h = 150 * s;
		int x = width - w - 12 * s + (int) ((1 - in) * 40 * s), y = (height - h) / 2;
		Ui.round(g, x, y, w, h, Ui.alpha(0xF0F7F1E3, in), 3 * s);
		Ui.round(g, x, y, w, 22 * s, Ui.alpha(0xFFE5525C, in), 3 * s);
		Ui.draw(g, font, Ui.bold("No. " + String.format("%03d", sp.dex())), x + 10 * s, y + 7 * s, 0xFFFFFFFF, s, false);
		Ui.draw(g, font, Ui.bold(sp.name()), x + 50 * s, y + 7 * s, 0xFFFFFFFF, s, false);
		int ty = y + 30 * s;
		String category = entry == null ? "" : entry[0] + " POKéMON";
		Ui.draw(g, font, Ui.ui(category), x + 10 * s, ty, Ui.CREAM_INK, s, false);
		int cx = x + 10 * s;
		cx += Ui.chip(g, font, sp.type1().label.toUpperCase(), cx, ty + 13 * s, Ui.typeColor(sp.type1()), s) + 3 * s;
		if (sp.type2() != null && sp.type2() != sp.type1()) Ui.chip(g, font, sp.type2().label.toUpperCase(), cx, ty + 13 * s, Ui.typeColor(sp.type2()), s);
		if (entry != null) {
			Ui.draw(g, font, Ui.ui("HT  " + entry[1]), x + 10 * s, ty + 29 * s, 0xFF4A5578, s, false);
			Ui.draw(g, font, Ui.ui("WT  " + entry[2] + " lb"), x + w / 2, ty + 29 * s, 0xFF4A5578, s, false);
			g.fill(x + 10 * s, ty + 41 * s, x + w - 10 * s, ty + 41 * s + Math.max(1, s / 2), 0x334A5578);
			List<String> lines = Ui.wrap(font, entry[3], (w - 20 * s) / s);
			for (int i = 0; i < lines.size() && i < 6; i++) Ui.draw(g, font, Ui.ui(lines.get(i)), x + 10 * s, ty + 47 * s + i * 11 * s, Ui.CREAM_INK, s, false);
		}
		if (ticks > 10 && (ticks / 10) % 2 == 0) Ui.draw(g, font, Ui.ui("▼"), x + w - 14 * s, y + h - 12 * s, 0xFFE5525C, s, false);
	}
}
