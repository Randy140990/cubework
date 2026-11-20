package com.insidethecube.retromon.client;

import java.util.HashMap;
import java.util.Map;

import com.insidethecube.retromon.engine.Species;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;

/**
 * A Pokémon's Pokédex entry, shown the first time you catch one: the UI Viewer's entry page (number, name, types,
 * height, what it's like, where to find it) with the creature turning on its stand beside it.
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
	protected void clickAnywhere() {
		if (ticks > 10) done();
	}

	@Override
	protected void back() {
		clickAnywhere();
	}

	@Override
	protected boolean key(KeyEvent e) {
		if (e.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_V && sp != null) {
			Settings.set("voice", 1);
			Sounds.dexVoice(sp.id());
			return true;
		}
		if (ticks > 10 && (Ui.isA(e) || Ui.isB(e))) done();
		return true;
	}

	@Override
	protected void draw(GuiGraphics g, float partial) {
		// The UI Viewer's Pokédex entry: the creature turning on its stand at the right, its page on the left, a
		// word at the top right that it's new.
		float t = ticks + partial, secs = t / 20F;
		if (sp == null) {
			Scenes.bg2(g, "dex");
			return;
		}
		// Its page in its own colour, as Let's Go's: a blue one's blue, a fiery one's orange.
		Scenes.typeBg(g, sp.type1(), true);
		Scenes.modelBare(g, secs, sp.id(), 0);
		PokedexScreen.page(g, sp, true, secs);
		float in = Math.min(1, Math.max(0, (t - 8) / 6F)), k = 1 - (1 - in) * (1 - in);
		String word = Lang.t("Added to your POKéDEX!");
		float ww = V.width(word, 20, true) + 50, wy = 500 - (1 - k) * 20;
		V.panel(g, 640 - ww / 2, wy, ww, 46, 23, true);
		V.text(g, word, 640 - ww / 2 + 25, V.capsTop(wy + 23, 20), 20, Ui.alpha(V.GOLD, k), true);
		V.hints(g, "V", "Read it out", "A", "Done");
		// In from white, out to white.
		float white = t < FADE ? 1 - t / FADE : 0;
		if (closing >= 0) white = Math.min(1, (closing + partial) / FADE);
		if (white > 0) g.fill(0, 0, width, height, Ui.alpha(0xFFFFFFFF, white));
	}
}
