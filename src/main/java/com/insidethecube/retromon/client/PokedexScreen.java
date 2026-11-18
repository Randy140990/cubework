package com.insidethecube.retromon.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.insidethecube.retromon.engine.Places;
import com.insidethecube.retromon.engine.Species;
import com.insidethecube.retromon.game.PlayerData;
import com.insidethecube.retromon.game.Trainers;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;

/**
 * The Pokédex: Kanto's 151 or the National 251, each one you've seen with its picture (a shadow if you
 * haven't caught it), a Poké Ball by the ones you own. The page on the right: number, name, kind, types,
 * height and weight and the entry for the ones you've caught, and for any you've seen, where to find them
 * (which can be shown on the Town Map).
 */
public class PokedexScreen extends DeviceScreen {
	private final Screen parent;
	private boolean national;
	private int at, top;
	private static Map<String, Set<String>> areas;

	public PokedexScreen(Screen parent) {
		super("Pokédex");
		this.parent = parent;
	}

	private int count() {
		return national ? 251 : 151;
	}

	private static Species byDex(int n) {
		for (Species s : Species.ALL.values()) if (s.dex() == n) return s;
		return null;
	}

	/** Where each Pokémon lives: the places of its wild tables and one-off encounters. */
	public static Set<String> areas(String species) {
		if (areas == null) {
			Map<String, Set<String>> m = new LinkedHashMap<>();
			for (String[] r : Species.table("wild.tsv")) {
				if (r.length < 5) continue;
				for (String slot : r[4].split(",")) {
					String sp = slot.split(":")[0];
					m.computeIfAbsent(sp, k -> new LinkedHashSet<>()).add(r[0]);
				}
			}
			for (Trainers.Static st : Trainers.STATICS.values()) m.computeIfAbsent(st.species(), k -> new LinkedHashSet<>()).add(st.place());
			areas = m;
		}
		return areas.getOrDefault(species, Set.of());
	}

	private void move(int d) {
		at = Math.floorMod(at + d, count());
	}

	@Override
	protected boolean key(KeyEvent e) {
		if (Ui.isUp(e)) move(-1);
		else if (Ui.isDown(e)) move(1);
		else if (Ui.isLeft(e)) move(-8);
		else if (Ui.isRight(e)) move(8);
		else if (e.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_TAB) {
			national = !national;
			at = Math.min(at, count() - 1);
			Sounds.press();
		} else if (e.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_V) listen();
		else if (Ui.isA(e)) showArea();
		else if (Ui.isB(e)) back();
		return true;
	}

	@Override
	public boolean mouseScrolled(double x, double y, double dx, double dy) {
		move(dy > 0 ? -3 : 3);
		return true;
	}

	/** V: the Pokédex reads the entry aloud (for one you've caught). */
	private void listen() {
		Species sp = byDex(at + 1);
		if (sp != null && ClientState.data.caught.contains(sp.id())) {
			Settings.set("voice", 1);
			Sounds.dexVoice(sp.id());
		}
	}

	@Override
	public void removed() {
		Sounds.stopVoice();
		super.removed();
	}

	@Override
	protected void back() {
		Sounds.press();
		to(parent);
	}

	private void showArea() {
		Species sp = byDex(at + 1);
		if (sp == null || !ClientState.data.seen.contains(sp.id()) || minecraft == null) return;
		Sounds.press();
		TownMapScreen map = new TownMapScreen(this);
		map.highlight(sp.name(), areas(sp.id()));
		to(map);
	}

	@Override
	protected void drawScreen(GuiGraphics g, float partial) {
		PlayerData d = ClientState.data;
		g.fill(0, 0, width, height, 0xF00D1222);
		int m = 8 * s;
		// The header: the title, the two lists, and how many you've seen and own.
		Ui.round(g, m, m, width - 2 * m, 22 * s, 0xFFE5525C, 3 * s);
		Ui.draw(g, font, Ui.bold("POKéDEX"), m + 10 * s, m + 7 * s, 0xFFFFFFFF, s, true);
		int tx = m + 70 * s;
		for (int i = 0; i < 2; i++) {
			int tw = 64 * s, bx = tx + i * (tw + 4 * s);
			boolean on = (i == 1) == national;
			Ui.round(g, bx, m + 4 * s, tw, 14 * s, on ? Ui.CREAM : 0x55FFFFFF, 2 * s);
			Ui.draw(g, font, Ui.bold(i == 0 ? "KANTO" : "NATIONAL"), bx + 8 * s, m + 7 * s, on ? Ui.CREAM_INK : 0xFFFFFFFF, s, false);
			boolean nat = i == 1;
			hit(bx, m + 4 * s, tw, 14 * s, () -> {
				national = nat;
				at = Math.min(at, count() - 1);
			});
		}
		int seen = 0, own = 0;
		for (int n = 1; n <= count(); n++) {
			Species sp = byDex(n);
			if (sp == null) continue;
			if (d.seen.contains(sp.id())) seen++;
			if (d.caught.contains(sp.id())) own++;
		}
		Ui.right(g, font, "SEEN " + seen + "    OWN " + own, width - m - 10 * s, m + 7 * s, 0xFFFFFFFF, s);
		// The list on the left.
		int ly = m + 28 * s, lw = Math.min(150 * s, width / 2 - m), row = 14 * s, rows = Math.max(3, (height - ly - m - 14 * s) / row);
		if (at < top) top = at;
		if (at >= top + rows) top = at - rows + 1;
		top = Math.max(0, Math.min(top, count() - rows));
		Ui.round(g, m, ly, lw, rows * row + 6 * s, 0xE6141B2E, 3 * s);
		for (int r = 0; r < rows && top + r < count(); r++) {
			int n = top + r + 1, y = ly + 3 * s + r * row;
			Species sp = byDex(n);
			boolean on = n - 1 == at;
			if (over(m, y, lw, row)) at = n - 1;
			Ui.pill(g, m + 3 * s, y, lw - 6 * s, row - 2 * s, on, s);
			int ink = on ? Ui.CREAM_INK : Ui.TEXT;
			Ui.draw(g, font, Ui.ui(String.format("%03d", n)), m + 8 * s, y + 3 * s, on ? 0xFF7A849E : Ui.MUTED, s, false);
			boolean saw = sp != null && d.seen.contains(sp.id()), got = sp != null && d.caught.contains(sp.id());
			if (saw) Ui.portrait(g, sp.id(), m + 26 * s, y - s, row);
			Ui.draw(g, font, Ui.bold(saw ? sp.name() : "-----"), m + 26 * s + row + 3 * s, y + 3 * s, saw ? ink : Ui.DIM, s, false);
			if (got) ball(g, m + lw - 16 * s, y + 2 * s, 8 * s);
			int idx = n - 1;
			hit(m, y, lw, row, () -> {
				if (at == idx) showArea();
				at = idx;
			});
		}
		Ui.text(g, font, "Tab: KANTO / NATIONAL   A: where to find it", m, height - m - 9 * s, Ui.DIM, s);
		page(g, d, m + lw + 8 * s, ly, width - m - (m + lw + 8 * s), height - ly - m - 14 * s);
	}

	/** A little Poké Ball (the ones you own). */
	private void ball(GuiGraphics g, int x, int y, int size) {
		Ui.round(g, x, y, size, size, 0xFFE5525C, size / 2);
		g.fill(x, y + size / 2, x + size, y + size, 0xFFF4F4F4);
		g.fill(x, y + size / 2 - Math.max(1, s / 2), x + size, y + size / 2 + Math.max(1, s / 2), 0xFF222222);
		g.fill(x + size / 2 - s, y + size / 2 - s, x + size / 2 + s, y + size / 2 + s, 0xFFFFFFFF);
	}

	private void page(GuiGraphics g, PlayerData d, int x, int y, int w, int h) {
		Species sp = byDex(at + 1);
		Ui.round(g, x, y, w, h, 0xF0F7F1E3, 3 * s);
		if (sp == null) return;
		boolean saw = d.seen.contains(sp.id()), got = d.caught.contains(sp.id());
		int ps = Math.min(w / 2, h / 2);
		if (saw) Ui.portrait(g, sp.id(), x + 8 * s, y + 8 * s, ps);
		else {
			Ui.round(g, x + 8 * s, y + 8 * s, ps, ps, 0x22000000, 4 * s);
			Ui.draw(g, font, Ui.bold("?"), x + 8 * s + ps / 2 - 3 * s, y + 8 * s + ps / 2 - 4 * s, 0xFF9AA0B4, s * 2, false);
		}
		int rx = x + 16 * s + ps, rw = w - (rx - x) - 8 * s;
		Ui.draw(g, font, Ui.ui("No. " + String.format("%03d", sp.dex())), rx, y + 10 * s, 0xFF7A849E, s, false);
		Ui.draw(g, font, Ui.bold(saw ? sp.name() : "?????"), rx, y + 22 * s, Ui.CREAM_INK, s, false);
		String[] e = DexScreen.entries().get(sp.dex());
		if (got && e != null) {
			Ui.draw(g, font, Ui.ui(e[0] + " POKéMON"), rx, y + 34 * s, 0xFF4A5578, s, false);
			int cx = rx;
			cx += Ui.chip(g, font, sp.type1().label.toUpperCase(), cx, y + 46 * s, Ui.typeColor(sp.type1()), s) + 3 * s;
			if (sp.type2() != null && sp.type2() != sp.type1()) Ui.chip(g, font, sp.type2().label.toUpperCase(), cx, y + 46 * s, Ui.typeColor(sp.type2()), s);
			Ui.draw(g, font, Ui.ui("HT " + e[1] + "    WT " + e[2] + " lb"), rx, y + 60 * s, 0xFF4A5578, s, false);
		} else if (saw) {
			Ui.draw(g, font, Ui.ui("Seen, not caught yet."), rx, y + 34 * s, 0xFF7A849E, s, false);
		}
		int ty = y + 16 * s + ps;
		if (got && e != null) {
			List<String> lines = Ui.wrap(font, e[3], (w - 16 * s) / s);
			for (int i = 0; i < lines.size() && i < 4; i++) Ui.draw(g, font, Ui.ui(lines.get(i)), x + 8 * s, ty + i * 11 * s, Ui.CREAM_INK, s, false);
			ty += Math.min(4, lines.size()) * 11 * s + 6 * s;
		}
		if (saw) {
			Ui.draw(g, font, Ui.bold("WHERE TO FIND IT"), x + 8 * s, ty, 0xFFE5525C, s, false);
			List<String> names = new ArrayList<>();
			for (String place : areas(sp.id())) {
				Places.Place pl = Places.get(place);
				String n = pl == null ? place.toUpperCase().replace('_', ' ') : pl.name();
				if (!names.contains(n)) names.add(n);
			}
			String where = names.isEmpty() ? "Not in the wild: from people, evolution or trades." : String.join(",  ", names);
			List<String> wl = Ui.wrap(font, where, (w - 16 * s) / s);
			int maxLines = Math.max(1, (y + h - ty - 24 * s) / (11 * s));
			for (int i = 0; i < wl.size() && i < maxLines; i++) Ui.draw(g, font, Ui.ui(wl.get(i)), x + 8 * s, ty + 12 * s + i * 11 * s, Ui.CREAM_INK, s, false);
			if (got) {
				int lw2 = 60 * s, lx = x + w - lw2 - 8 * s - (names.isEmpty() ? 0 : 64 * s), ly2 = y + h - 20 * s;
				Ui.button(g, font, lx, ly2, lw2, 14 * s, "LISTEN (V)", 0xFFE5525C, over(lx, ly2, lw2, 14 * s), s);
				hit(lx, ly2, lw2, 14 * s, this::listen);
			}
			if (!names.isEmpty()) {
				int bw = 60 * s, bx = x + w - bw - 8 * s, by = y + h - 20 * s;
				Ui.button(g, font, bx, by, bw, 14 * s, "ON MAP", 0xFF4CB8A0, over(bx, by, bw, 14 * s), s);
				hit(bx, by, bw, 14 * s, this::showArea);
			}
		}
	}
}
