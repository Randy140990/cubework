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
import org.lwjgl.glfw.GLFW;

/**
 * The Pokédex, as the UI Viewer's "Pokédex" and "Pokédex entry" pages.
 * <ul>
 * <li>The list: every POKéMON in order, nine at a time. Those you've caught have their picture and a ball by
 * them, those you've only seen a dark shape, the rest "?????". The one you're on turns on a stand at the right,
 * with its name, types and what it stands in for beneath it. TAB shows only the ones you've caught.</li>
 * <li>The entry (A): its number and name, its types, how tall it is in blocks, what it's like, and where to find
 * it. V reads it out; A shows where on the Town Map.</li>
 * </ul>
 * Kanto's 151 to begin with; once you've seen one from further afield, the National list.
 */
public class PokedexScreen extends DeviceScreen {
	private final Screen parent;
	private boolean caughtOnly, entry;
	private int at, top;
	private static Map<String, Set<String>> areas;

	public PokedexScreen(Screen parent) {
		super("Pokédex");
		this.parent = parent;
	}

	static Species byDex(int n) {
		for (Species s : Species.ALL.values()) if (s.dex() == n) return s;
		return null;
	}

	/** The numbers to show: Kanto's (the National list once you've seen one past 151), or just those caught. */
	private List<Integer> list() {
		PlayerData d = ClientState.data;
		int count = 151;
		for (String id : d.seen) {
			Species sp = Species.ALL.get(id);
			if (sp != null && sp.dex() > 151) count = 251;
		}
		List<Integer> out = new ArrayList<>();
		for (int n = 1; n <= count; n++) {
			Species sp = byDex(n);
			if (sp == null) continue;
			if (!caughtOnly || d.caught.contains(sp.id())) out.add(n);
		}
		return out;
	}

	private Species current() {
		List<Integer> l = list();
		if (l.isEmpty()) return null;
		at = Math.max(0, Math.min(at, l.size() - 1));
		return byDex(l.get(at));
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

	/** Its places by their names, in a line ("ROUTE 1, ROUTE 2, VIRIDIAN FOREST"), or "" if it's not in the wild. */
	static String placeNames(String species) {
		List<String> names = new ArrayList<>();
		for (String place : areas(species)) {
			Places.Place pl = Places.get(place);
			String n = pl == null ? place.toUpperCase().replace('_', ' ') : pl.name();
			if (!names.contains(n)) names.add(n);
		}
		return String.join(", ", names);
	}

	private void move(int d) {
		int n = list().size();
		if (n == 0) return;
		int was = at;
		at = Math.max(0, Math.min(n - 1, at + d));
		if (at != was) Sounds.press();
	}

	@Override
	protected boolean wantsTab() {
		return true;
	}

	@Override
	protected boolean key(KeyEvent e) {
		Species sp = current();
		if (entry) {
			if (e.key() == GLFW.GLFW_KEY_V) listen();
			else if (Ui.isA(e)) showArea();
			else if (Ui.isB(e)) {
				Sounds.press();
				Sounds.stopVoice();
				entry = false;
			} else if (Ui.isUp(e) || Ui.isDown(e)) {
				// Up and down turn to the next one you've seen.
				List<Integer> l = list();
				for (int i = at + (Ui.isDown(e) ? 1 : -1); i >= 0 && i < l.size(); i += Ui.isDown(e) ? 1 : -1) {
					Species o = byDex(l.get(i));
					if (o != null && ClientState.data.seen.contains(o.id())) {
						Sounds.stopVoice();
						Sounds.press();
						at = i;
						break;
					}
				}
			}
			return true;
		}
		if (Ui.isUp(e)) move(-1);
		else if (Ui.isDown(e)) move(1);
		else if (Ui.isLeft(e)) move(-9);
		else if (Ui.isRight(e)) move(9);
		else if (e.key() == GLFW.GLFW_KEY_TAB) {
			Species was = sp;
			caughtOnly = !caughtOnly;
			Sounds.press();
			// Stay on the same one if it's still in the list.
			List<Integer> l = list();
			at = 0;
			if (was != null) for (int i = 0; i < l.size(); i++) if (l.get(i) == was.dex()) at = i;
		} else if (e.key() == GLFW.GLFW_KEY_V) listen();
		else if (Ui.isA(e)) open();
		else if (Ui.isB(e)) back();
		return true;
	}

	private void open() {
		Species sp = current();
		if (sp == null || !ClientState.data.seen.contains(sp.id())) return;
		Sounds.press();
		entry = true;
	}

	@Override
	public boolean mouseScrolled(double x, double y, double dx, double dy) {
		if (!entry) move(dy > 0 ? -3 : 3);
		return true;
	}

	/** V: the Pokédex reads the entry aloud (for one you've caught). */
	private void listen() {
		Species sp = current();
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
		if (entry) {
			Sounds.stopVoice();
			entry = false;
			return;
		}
		to(parent);
	}

	private void showArea() {
		Species sp = current();
		if (sp == null || !ClientState.data.seen.contains(sp.id()) || areas(sp.id()).isEmpty() || minecraft == null) return;
		Sounds.press();
		TownMapScreen map = new TownMapScreen(this);
		map.highlight(sp.name(), areas(sp.id()));
		to(map);
	}

	@Override
	protected void drawScreen(GuiGraphics g, float partial) {
		PlayerData d = ClientState.data;
		float secs = (ticks + partial) / 20F;
		Scenes.backdrop(g, "dex");
		Species sp = current();
		if (entry && sp != null) {
			Scenes.model(g, secs, sp.id(), -1.05);
			page(g, sp, d.caught.contains(sp.id()));
			boolean got = d.caught.contains(sp.id()), map = !areas(sp.id()).isEmpty();
			if (got && map) V.hints(g, "A", "On the map", "V", "Read it out", "B", "Back");
			else if (got) V.hints(g, "V", "Read it out", "B", "Back");
			else if (map) V.hints(g, "A", "On the map", "B", "Back");
			else V.hints(g, "B", "Back");
			return;
		}
		boolean saw = sp != null && d.seen.contains(sp.id());
		if (saw) Scenes.model(g, secs, sp.id(), -1.05);
		// The header: POKéDEX, and how many you've seen and caught.
		int seen = 0, caught = 0;
		for (String id : d.seen) if (Species.ALL.containsKey(id)) seen++;
		for (String id : d.caught) if (Species.ALL.containsKey(id)) caught++;
		V.header(g, "icon_dex", "POKéDEX", "SEEN " + seen + "   CAUGHT " + caught);
		// The list on the left: nine rows.
		List<Integer> l = list();
		float lx = 28, ly = 110, lw = 520, lh = 560, rh = 54;
		V.panel(g, lx, ly, lw, lh);
		if (at < top) top = at;
		if (at > top + 8) top = at - 8;
		top = Math.max(0, Math.min(top, Math.max(0, l.size() - 9)));
		if (l.isEmpty()) V.text(g, "None caught yet.", lx + 40, ly + 24, 21, V.DIM, true);
		for (int j = 0; j < 9 && top + j < l.size(); j++) {
			int i = top + j, n = l.get(i);
			Species s = byDex(n);
			if (s == null) continue;
			boolean on = i == at, seenIt = d.seen.contains(s.id()), got = d.caught.contains(s.id());
			float rx = lx + 12, ry = ly + 12 + j * rh, rw = lw - 24;
			V.item(g, "", null, rx, ry, rw, rh, on, false, 21);
			int ink = on ? V.INK : V.TEXT;
			float ty = ry + (rh - 21 * 1.15F) / 2;
			V.text(g, String.format("%03d", n), rx + 40, ty, 21, Ui.alpha(ink, 0.75F), true);
			if (seenIt) V.img(g, Look.portrait(s.id()), rx + 112, ry + 3, 48, 48, 128, 128, got ? 0xFFFFFFFF : 0x73000000);
			V.text(g, seenIt ? s.name() : "?????", rx + 174, ty, 21, seenIt ? ink : Ui.alpha(ink, 0.45F), true);
			if (got) V.icon(g, "icon_ball", rx + rw - 22 - 22, ry + 16, 22);
			vhit(rx, ry, rw, rh, () -> {
				if (at == i) open();
				else at = i;
			});
		}
		// The one you're on, under its stand: its name and types, and what it stands in for.
		float ix = 800, iy = 560, iw = 440;
		V.panel(g, ix, iy, iw, 84, 24, true);
		if (saw) {
			V.text(g, sp.name(), ix + 20, iy + 12, 26, V.TEXT, true);
			float tx = ix + 20 + V.width(sp.name(), 26, true) + 10;
			tx += V.type(g, sp.type1().label, Ui.typeColor(sp.type1()), tx, iy + 16, 70, 12) + 6;
			if (sp.type2() != null && sp.type2() != sp.type1()) V.type(g, sp.type2().label, Ui.typeColor(sp.type2()), tx, iy + 16, 70, 12);
			V.draw(g, V.raw(standsFor(sp), true), ix + 20, iy + 52, 15, V.MUTED, false);
		} else V.text(g, "?????", ix + 20, iy + 22, 26, V.TEXT, true);
		V.hints(g, "A", "Entry", "B", "Back", "TAB", caughtOnly ? "All of them" : "Caught only");
	}

	/** "Stands in for #025 PIKACHU" (our creatures); "No. 025 MOUSE POKéMON" with Cobblemon's. */
	private static String standsFor(Species sp) {
		String no = String.format("%03d", sp.dex());
		if (Look.ours() && Look.has(sp.id())) return "Stands in for #" + no + " " + sp.name();
		String[] e = DexScreen.entries().get(sp.dex());
		return "No. " + no + (e == null || e[0].isEmpty() ? "" : "   " + e[0].toUpperCase() + " POKéMON");
	}

	/**
	 * A POKéMON's entry (the viewer's "Pokédex entry"), in the panel on the left: number, name, types, height in
	 * blocks and what it stands in for, what it's like, and where to find it. (The creature on its stand is drawn
	 * beside it.) Shared with the page you see the first time you catch one.
	 */
	static void page(GuiGraphics g, Species sp, boolean got) {
		float x = 28, y = 28, w = 600, h = 640, px = x + 30, pw = w - 60;
		V.panel(g, x, y, w, h);
		String[] e = DexScreen.entries().get(sp.dex());
		float cy = y + 26;
		V.label(g, "No. " + String.format("%03d", sp.dex()), px, cy);
		cy += 18 + 14;
		V.text(g, sp.name(), px, cy, 46, V.TEXT, true);
		cy += 46 + 14;
		float tx = px;
		tx += V.type(g, sp.type1().label, Ui.typeColor(sp.type1()), tx, cy, 92, 13.5F) + 8;
		if (sp.type2() != null && sp.type2() != sp.type1()) V.type(g, sp.type2().label, Ui.typeColor(sp.type2()), tx, cy, 92, 13.5F);
		cy += 24 + 14;
		// Height (in blocks, from its model) and what it stands in for (or its weight, with Cobblemon's).
		float bw = (pw - 10) / 2, bh = 70;
		BedrockModel.Def def = BedrockModel.def(sp.id());
		String height = def == null ? (e == null ? "?" : e[1]) : String.format("%.2f blocks", def.height);
		boolean ours = Look.ours() && Look.has(sp.id());
		String[][] boxes = {{"Height", height}, ours ? new String[] {"Stands in for", sp.name()} : new String[] {"Weight", e == null ? "?" : e[2] + " lb"}};
		for (int i = 0; i < 2; i++) {
			float bx = px + i * (bw + 10);
			V.panel(g, bx, cy, bw, bh, 16, true);
			V.label(g, boxes[i][0], bx + 16, cy + 12);
			if (i == 1 && ours) V.draw(g, V.raw(boxes[i][1], true), bx + 16, cy + 32, 22, V.TEXT, false);
			else V.text(g, boxes[i][1], bx + 16, cy + 32, 22, V.TEXT, true);
		}
		cy += bh + 14;
		// What it's like (once it's caught).
		String about = got && e != null && !e[3].isEmpty() ? e[3] : got ? "Its entry is still being written." : "Catch one to learn more about it.";
		List<String> lines = V.wrap(about, pw, 22, true);
		for (int i = 0; i < lines.size() && i < 6; i++) V.text(g, lines.get(i), px, cy + i * 33 + 4, 22, 0xFFE8F4EF, true);
		cy += Math.min(6, lines.size()) * 33 + 14 + 4;
		V.label(g, "Where to find it", px, cy);
		cy += 18 + 14;
		String where = placeNames(sp.id());
		if (where.isEmpty()) where = "Not in the wild: from people, evolution or trades.";
		List<String> wl = V.wrap(where, pw, 19, true);
		for (int i = 0; i < wl.size() && cy + i * 26 < y + h - 30; i++) V.text(g, wl.get(i), px, cy + i * 26, 19, V.TEXT, true);
	}
}
