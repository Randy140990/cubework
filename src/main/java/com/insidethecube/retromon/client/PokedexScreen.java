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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;

/**
 * The Pokédex, as the UI Viewer's "Pokédex" and "Pokédex entry" pages (Let's Go's).
 * <ul>
 * <li>The grid: every POKéMON as a little picture on the POKéDEX's aqua, seven across and four down: the ones you've
 * caught in full colour, the ones you've only seen as shadows, the rest a faint ball. The one you're on gets its number
 * and name on a label. TAB shows only the ones you've caught.</li>
 * <li>The entry (A): it stands big in the middle on its own colour; its number, name, types and height on a clear card
 * top left; how many you've caught and where to find it top right; what it's like across the bottom, read out loud
 * (POKéDEX VOICE). V or A reads it again; M shows where on the Town Map.</li>
 * </ul>
 * Kanto's 151 to begin with; once you've seen one from further afield, the National list.
 */
public class PokedexScreen extends DeviceScreen {
	private final Screen parent;
	private boolean caughtOnly, entry;
	private int at, top;
	/** When the entry was opened (ticks), for reading it out a moment later. */
	private int entryAt = -1;
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
			if (e.key() == GLFW.GLFW_KEY_V || Ui.isA(e)) listen();
			else if (e.key() == GLFW.GLFW_KEY_M) showArea();
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
						entryAt = ticks;
						break;
					}
				}
			}
			return true;
		}
		if (Ui.isUp(e)) move(-COLS);
		else if (Ui.isDown(e)) move(COLS);
		else if (Ui.isLeft(e)) move(-1);
		else if (Ui.isRight(e)) move(1);
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
		entryAt = ticks;
	}

	@Override
	public void tick() {
		super.tick();
		// The entry's read out a moment after it opens (the viewer's), with POKéDEX VOICE on.
		if (entry && entryAt >= 0 && ticks - entryAt == 12) {
			Species sp = current();
			if (sp != null && Settings.voice()) Sounds.dexVoice(sp.id());
		}
	}

	@Override
	public boolean mouseScrolled(double x, double y, double dx, double dy) {
		if (!entry) move(dy > 0 ? -COLS : COLS);
		return true;
	}

	/** V: the Pokédex reads the entry aloud. */
	private void listen() {
		Species sp = current();
		if (sp != null && ClientState.data.seen.contains(sp.id())) {
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

	private static final int COLS = 7, ROWS = 4;

	@Override
	protected void drawScreen(GuiGraphics g, float partial) {
		PlayerData d = ClientState.data;
		float secs = (ticks + partial) / 20F;
		Species sp = current();
		if (entry && sp != null) {
			Scenes.typeBg(g, sp.type1(), true);
			Scenes.modelBare(g, secs, sp.id(), 0);
			page(g, sp, d.caught.contains(sp.id()), secs);
			boolean map = !areas(sp.id()).isEmpty() && d.caught.contains(sp.id());
			if (map) V.hints(g, "V", "Read it out", "M", "On the map", "B", "Back");
			else V.hints(g, "V", "Read it out", "B", "Back");
			return;
		}
		Scenes.bg2(g, "dex");
		// How many you've caught and seen, along the top.
		int seen = 0, caught = 0;
		for (String id : d.seen) if (Species.ALL.containsKey(id)) seen++;
		for (String id : d.caught) if (Species.ALL.containsKey(id)) caught++;
		String[][] heads = {{Lang.t("POKéMON caught"), String.valueOf(caught)}, {Lang.t("POKéMON seen"), String.valueOf(seen)}};
		float[] hw = new float[2];
		float total = 120;
		for (int i = 0; i < 2; i++) total += hw[i] = V.titleWidth(heads[i][0], 22) + 8 + V.titleWidth(heads[i][1], 28);
		float hx = 640 - total / 2;
		for (int i = 0; i < 2; i++) {
			float base = 16 + 22 * 0.95F;
			V.title(g, heads[i][0], hx, base - 22 * 0.95F + 2, 22, 0x2E000000);
			V.title(g, heads[i][0], hx, base - 22 * 0.95F, 22, 0xFFFFFFFF);
			float nx = hx + V.titleWidth(heads[i][0], 22) + 8;
			V.title(g, heads[i][1], nx, base - 28 * 0.95F + 2, 28, 0x2E000000);
			V.title(g, heads[i][1], nx, base - 28 * 0.95F, 28, 0xFFFFFFFF);
			hx += hw[i] + 120;
		}
		// The grid: seven across, four down.
		List<Integer> l = list();
		float gx = 150, gy = 86, gw = 980, cw = (gw - 10 * (COLS - 1)) / COLS, ch = 120;
		if (at < top) top = at / COLS * COLS;
		if (at >= top + COLS * ROWS) top = (at / COLS - ROWS + 1) * COLS;
		top = Math.max(0, top);
		if (l.isEmpty()) V.centred(g, Lang.t("None caught yet."), 640, 300, 24, 0xFFFFFFFF, true);
		int onAt = -1;
		float onX = 0, onY = 0;
		for (int j = 0; j < COLS * ROWS && top + j < l.size(); j++) {
			int i = top + j, n = l.get(i);
			Species s0 = byDex(n);
			if (s0 == null) continue;
			float x = gx + (j % COLS) * (cw + 10), y = gy + (j / COLS) * (ch + 14);
			if (vover(x, y, cw, ch)) at = i;
			boolean on = i == at, seenIt = d.seen.contains(s0.id()), got = d.caught.contains(s0.id());
			if (on) {
				V.rr(g, x + 10 - 3, y + 4 - 3, cw - 20 + 6, ch - 8 + 6, 27, 0xFFFFFFFF);
				V.rr(g, x + 10, y + 4, cw - 20, ch - 8, 24, 0xFF8ADBEA);
				onAt = i;
				onX = x;
				onY = y;
			}
			float mid = x + cw / 2, cy = y + ch / 2;
			if (seenIt) {
				float ps = on ? 96 * 1.15F : 96;
				if (got) V.img(g, Look.portrait(s0.id()), mid - ps / 2, cy - ps / 2 + 4, ps, ps, 128, 128, 0x4D002850);
				V.img(g, Look.portrait(s0.id()), mid - ps / 2, cy - ps / 2, ps, ps, 128, 128, got ? 0xFFFFFFFF : 0x59000000);
			} else {
				float ps = on ? 44 * 1.15F : 44;
				V.icon(g, "picto_ball_aqua", mid - ps / 2, cy - ps / 2, ps, 0x47FFFFFF);
			}
			V.centred(g, String.format("%03d", n), mid, y + ch - 2 - 14 * 1.2F, 14, 0xD9FFFFFF, true);
			vhit(x, y, cw, ch, () -> {
				at = i;
				open();
			});
		}
		// The one you're on: its number and name on a label above it.
		if (onAt >= 0) {
			Species s0 = byDex(l.get(onAt));
			boolean seenIt = s0 != null && d.seen.contains(s0.id());
			String t = "No. " + String.format("%03d", l.get(onAt)) + " " + (seenIt ? s0.name() : "?????");
			float tw = V.width(t, 15, true) + 28, th = 15 * 1.36F + 6, tx = onX + cw / 2 - tw / 2, ty = onY - 12;
			V.softShadow(g, tx, ty + 4, tw, th, 10, 0x40002850);
			V.pill(g, tx, ty, tw, th, 0xFFFFFFFF);
			V.text(g, t, tx + 14, V.capsTop(ty + th / 2, 15), 15, V.INK, true);
		}
		int lines = (l.size() + COLS - 1) / COLS;
		if (top > 0) V.tri(g, 640, gy - 12, 14, 3, 0xD9FFFFFF);
		if (top / COLS + ROWS < lines) V.tri(g, 640, gy + ROWS * (ch + 14) + 2, 14, 1, 0xD9FFFFFF);
		V.hints(g, "A", "Entry", "B", "Back", "TAB", caughtOnly ? "All of them" : "Caught only");
	}

	/** How many of it you have (your team and your box), at least one once it's caught. */
	private static int howMany(Species sp) {
		PlayerData d = ClientState.data;
		int n = 0;
		for (var m : d.party) if (m.species.id().equals(sp.id())) n++;
		for (var m : d.box) if (m.species.id().equals(sp.id())) n++;
		return d.caught.contains(sp.id()) ? Math.max(1, n) : n;
	}

	/** "Stands in for #025 PIKACHU" (our creatures); "No. 025 MOUSE POKéMON" with Cobblemon's. */
	private static String standsFor(Species sp) {
		String no = String.format("%03d", sp.dex());
		if (Look.ours() && Look.has(sp.id())) return "Stands in for #" + no + " " + sp.name();
		String[] e = DexScreen.entries().get(sp.dex());
		return "No. " + no + (e == null || e[0].isEmpty() ? "" : "   " + e[0].toUpperCase() + " POKéMON");
	}

	/** A type's chip (the viewer's g-type): its colour, white capitals spaced a little, at least 92 wide. Returns its width. */
	static float typeChip(GuiGraphics g, com.insidethecube.retromon.engine.Type t, float x, float y) {
		String l = t.label.toUpperCase();
		float tw = V.trackedWidth(l, 13.5F, 1), w = Math.max(92, tw + 24), h = 13.5F * 1.36F + 6;
		V.pill(g, x, y, w, h, Ui.typeColor(t) | 0xFF000000);
		V.tracked(g, l, x + (w - tw) / 2, V.capsTop(y + h / 2, 13.5F) + 1, 13.5F, 0x59000000, 1);
		V.tracked(g, l, x + (w - tw) / 2, V.capsTop(y + h / 2, 13.5F), 13.5F, 0xFFFFFFFF, 1);
		return w;
	}

	/** The viewer's glass card: white mostly see-through, a white edge, a soft shadow. */
	private static void glass(GuiGraphics g, float x, float y, float w, float h) {
		V.shadow(g, x, y, w, h, 22, 0.9F);
		V.rr(g, x, y, w, h, 22, 0xFFFFFFFF);
		V.rr(g, x + 3, y + 3, w - 6, h - 6, 19, 0xDBFFFFFF);
	}

	/** What it's like (its entry, else the viewer's stand-in line). */
	static String about(Species sp) {
		String[] e = DexScreen.entries().get(sp.dex());
		if (e != null && e.length > 3 && !e[3].isEmpty()) return e[3];
		String types = sp.type1().label.toLowerCase() + (sp.type2() != null && sp.type2() != sp.type1() ? " and " + sp.type2().label.toLowerCase() : "");
		return "A " + types + "-type POKéMON. Its entry is still being written.";
	}

	/**
	 * A POKéMON's entry, as the viewer's "Pokédex entry" (the creature is drawn big in the middle by the caller): its
	 * number, name, types and height on a clear card top left; how many you've caught and where to find it top right;
	 * what it's like across the bottom on a band of its own colour, the speaker beside it.
	 */
	static void page(GuiGraphics g, Species sp, boolean got, float secs) {
		int tc = Ui.typeColor(sp.type1()) | 0xFF000000;
		// Top left: number, name, types, height and what it stands in for.
		float x = 40, y = 34, w = 420, px = x + 22;
		BedrockModel.Def def = BedrockModel.def(sp.id());
		String[] e = DexScreen.entries().get(sp.dex());
		String height = def == null ? (e == null ? "?" : e[1]) : String.format("%.2f blocks", def.height);
		boolean ours = Look.ours() && Look.has(sp.id());
		String[][] rows = {{"Height", height}, ours ? new String[] {"Stands in for", sp.name()} : new String[] {"Weight", e == null ? "?" : e[2] + " lb"}};
		float chipH = 13.5F * 1.36F + 6, rowH = 19 * 1.36F;
		float h = 16 + 16 * 1.36F + 6 + 40 + 6 + chipH + 6 + 6 + rows.length * rowH + 2 + 16;
		glass(g, x, y, w, h);
		float cy = y + 16;
		V.tracked(g, "POKéDEX No. " + String.format("%03d", sp.dex()), px, cy + 3, 16, Ui.mix(tc, 0xFF000000, 0.25F), 1);
		cy += 16 * 1.36F + 6;
		V.title(g, sp.name(), px, cy, 40, V.INK);
		cy += 40 + 6;
		float tx = px;
		tx += typeChip(g, sp.type1(), tx, cy) + 8;
		if (sp.type2() != null && sp.type2() != sp.type1()) typeChip(g, sp.type2(), tx, cy);
		cy += chipH + 6 + 6;
		float lw = 0;
		for (String[] r : rows) lw = Math.max(lw, V.width(Lang.t(r[0]), 19, false));
		for (String[] r : rows) {
			V.text(g, Lang.t(r[0]), px, cy + 2, 19, Ui.alpha(V.INK, 0.8F), false);
			if (r[0].equals("Stands in for")) V.draw(g, V.raw(r[1], true), px + lw + 16, cy + 2, 19, V.INK, false);
			else V.text(g, r[1], px + lw + 16, cy + 2, 19, V.INK, false);
			cy += rowH + 2;
		}
		// Top right, the same card as the left's (as tall, its labels in the POKéMON's colour): how many you've caught
		// beside a POKé BALL; a fine line; where to find it, each place a chip tinted its colour.
		float rx = 940, rw = 300, rpx = rx + 22, inner = rw - 44;
		int label = Ui.mix(tc, 0xFF000000, 0.25F), chipBg = Ui.mix(tc, 0xFFFFFFFF, 0.8F), chipInk = Ui.mix(tc, 0xFF000000, 0.55F);
		List<String> places = new java.util.ArrayList<>();
		if (got) for (String n : PokedexScreen.placeNames(sp.id()).split(", ")) if (!n.isBlank()) places.add(n);
		float pillH = 26, chipGap = 6, chipS = 13.5F;
		// The chips, laid out in rows (three at most; any more say how many).
		List<float[]> at = new java.util.ArrayList<>();
		List<String> shown = new java.util.ArrayList<>();
		float cx = 0;
		int row = 0;
		for (int i = 0; i < places.size(); i++) {
			String l = places.get(i);
			float cw = V.trackedWidth(l, chipS, 0.5F) + 22;
			if (cx > 0 && cx + cw > inner) { row++; cx = 0; }
			if (row == 3) {
				// No room for the rest: the last chip says how many more there are.
				float[] last = at.remove(at.size() - 1);
				shown.remove(shown.size() - 1);
				at.add(last); shown.add("+" + (places.size() - shown.size()) + " " + Lang.t("more"));
				break;
			}
			at.add(new float[] {cx, row}); shown.add(l);
			cx += cw + chipGap;
		}
		int nRows = shown.isEmpty() ? 0 : (int) at.get(at.size() - 1)[1] + 1;
		List<String> note = got && places.isEmpty() ? V.wrap(Lang.t("Not in the wild: from people, evolution or trades."), inner, 16, false)
				: !got ? List.of(Lang.t("Not found yet")) : List.of();
		float body = nRows > 0 ? nRows * pillH + (nRows - 1) * chipGap : note.size() * 16 * 1.36F;
		float rh = Math.max(h, 16 + 16 * 1.36F + 6 + 40 + 12 + 2 + 12 + 16 * 1.36F + 8 + body + 16);
		glass(g, rx, y, rw, rh);
		float ry = y + 16;
		V.tracked(g, Lang.t("Number caught"), rpx, ry + 3, 16, label, 1);
		ry += 16 * 1.36F + 6;
		V.icon(g, "ball_pok_ball", rpx, ry + 2, 36);
		V.title(g, String.valueOf(howMany(sp)), rpx + 46, ry - 2, 40, V.INK);
		ry += 40 + 12;
		V.rr(g, rpx, ry, inner, 2, 1, Ui.alpha(V.INK, 0.12F));
		ry += 2 + 12;
		V.tracked(g, Lang.t("Where to find it"), rpx, ry + 3, 16, label, 1);
		ry += 16 * 1.36F + 8;
		for (int i = 0; i < shown.size(); i++) {
			String l = shown.get(i);
			float cw = V.trackedWidth(l, chipS, 0.5F) + 22, ccx = rpx + at.get(i)[0], ccy = ry + at.get(i)[1] * (pillH + chipGap);
			V.pill(g, ccx, ccy, cw, pillH, chipBg);
			V.tracked(g, l, ccx + 11, V.capsTop(ccy + pillH / 2, chipS), chipS, chipInk, 0.5F);
		}
		for (int i = 0; i < note.size(); i++) V.text(g, note.get(i), rpx, ry + i * 16 * 1.36F, 16, Ui.alpha(V.INK, got ? 0.85F : 0.55F), false);
		// The band across the bottom: what it's like, in white, the speaker at its left.
		int c1 = Ui.mix(tc, 0xFF000000, 0.45F), c2 = Ui.mix(tc, 0xFF000000, 0.6F);
		float by = 548, bh = 172;
		int top = Math.round(V.y(by)), mid = Math.round(V.y(by + bh * 0.45F)), bot = Math.round(V.y(720));
		int fullBot = Math.max(bot, Minecraft.getInstance().getWindow().getGuiScaledHeight());
		int gw = Minecraft.getInstance().getWindow().getGuiScaledWidth();
		g.fillGradient(0, top, gw, mid, tc & 0x00FFFFFF, (c1 & 0x00FFFFFF) | 0xCC000000);
		g.fillGradient(0, mid, gw, fullBot, (c1 & 0x00FFFFFF) | 0xCC000000, (c2 & 0x00FFFFFF) | 0xEE000000);
		List<String> lines = V.wrap(about(sp), 1280 - 400, 27, false);
		for (int i = 0; i < Math.min(3, lines.size()); i++) {
			float ly = by + 46 + i * 27 * 1.45F;
			V.centred(g, lines.get(i), 640, ly + 2, 27, 0x59000000, false);
			V.centred(g, lines.get(i), 640, ly, 27, 0xFFFFFFFF, false);
		}
		float sx = 150 + 18, sy = by + 52 + 18, pulse = (secs % 1.2F) / 1.2F;
		V.ellipse(g, sx, sy, 18 + 16 * pulse, 18 + 16 * pulse, Ui.alpha(0xFFFFFFFF, 0.6F * (1 - pulse)));
		V.ellipse(g, sx, sy, 18, 18, 0xFFFFFFFF);
		// The speaker: its box and cone, two waves.
		float u = 22 / 24F, ox = sx - 11, oy = sy - 11;
		V.poly(g, new float[] {ox + 4 * u, oy + 9 * u, ox + 8 * u, oy + 9 * u, ox + 8 * u, oy + 15 * u, ox + 4 * u, oy + 15 * u}, tc);
		V.poly(g, new float[] {ox + 8 * u, oy + 9 * u, ox + 13 * u, oy + 5 * u, ox + 13 * u, oy + 19 * u, ox + 8 * u, oy + 15 * u}, tc);
		for (int wv = 0; wv < 2; wv++) {
			float r = wv == 0 ? 4 : 7.5F, cxw = ox + 15 * u, cyw = oy + 12 * u;
			for (int k = 0; k < 6; k++) {
				double a0 = -Math.PI / 3 + k * (2 * Math.PI / 3) / 6, a1 = a0 + (2 * Math.PI / 3) / 6;
				V.line(g, cxw + (float) Math.cos(a0) * r * u, cyw + (float) Math.sin(a0) * r * u, cxw + (float) Math.cos(a1) * r * u, cyw + (float) Math.sin(a1) * r * u, 2 * u, tc);
			}
		}
	}
}
