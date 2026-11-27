package com.insidethecube.retromon.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.insidethecube.retromon.engine.Places;
import com.insidethecube.retromon.engine.Species;
import com.insidethecube.retromon.game.Net;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.nbt.CompoundTag;
import org.lwjgl.glfw.GLFW;

/**
 * (0.72) The PLACER (OPTION, John's 18:24: "I'll need a way where I can touch a pixel on the map and place them"):
 * every trainer, person, item ball, hidden item and STRENGTH boulder FireRed has outside and in the caves, place by
 * place in the order you come to them. Pick one, stand where it goes facing the way it looks, and PLACE HERE; or WALK
 * AND PLACE to put a place's down one after another out on the map (P place, N next, U undo, X stop). Each one's saved
 * in the game folder's config/retromon_placed.tsv as you go, and is there in the game straight away.
 */
public class PlacerScreen extends GameScreen {
	private final Screen parent;

	/** One of FireRed's: where (its place), what, its key, what it's called and has, its step on FireRed's map. */
	public record Thing(String place, String kind, String key, String label, String detail, int gx, int gy, String facing, int sight) {}

	private static List<Thing> ALL;
	private static final Map<String, List<Thing>> BY_PLACE = new LinkedHashMap<>();
	private static final Map<String, Thing> BY_KEY = new HashMap<>();
	/** The places in the order you come to them in KANTO (those FireRed has things in). */
	private static final String[] ORDER = {"pallet_town", "route_1", "viridian_city", "route_22", "route_2", "viridian_forest", "pewter_city", "route_3", "mt_moon_1f",
			"mt_moon_b1f", "mt_moon_b2f", "route_4", "cerulean_city", "route_24", "route_25", "route_5", "underground_path_ns", "route_6", "vermilion_city", "route_11",
			"route_9", "route_10", "rock_tunnel_1f", "rock_tunnel_b1f", "power_plant", "lavender_town", "route_8", "underground_path_we", "route_7", "celadon_city",
			"route_16", "route_17", "route_18", "fuchsia_city", "safari_zone_center", "safari_zone_east", "safari_zone_north", "safari_zone_west", "route_15", "route_14",
			"route_13", "route_12", "saffron_city", "route_19", "route_20", "seafoam_islands_1f", "seafoam_islands_b1f", "seafoam_islands_b2f", "seafoam_islands_b3f",
			"seafoam_islands_b4f", "cinnabar_island", "route_21", "route_23", "victory_road_1f", "victory_road_2f", "victory_road_3f", "cerulean_cave_1f",
			"cerulean_cave_2f", "cerulean_cave_b1f"};
	private static final int SHOWN = 12;

	private static int placeAt;
	private int at, top;

	/** WALK AND PLACE: the one you're putting down out on the map now (null: not placing). */
	public static Thing walking;
	/** The last one put down that way (U undoes it). */
	private static Thing lastPlaced;

	public PlacerScreen(Screen parent) {
		super("Placer");
		this.parent = parent;
		load();
		// Where you are, to start with: the place you're in, if FireRed has things there.
		String here = ClientState.data.place;
		List<String> places = places();
		if (walking != null) placeAt = Math.max(0, places.indexOf(walking.place()));
		else if (here != null && places.contains(here)) placeAt = places.indexOf(here);
		placeAt = Math.max(0, Math.min(places.size() - 1, placeAt));
		if (walking != null) at = Math.max(0, things().indexOf(walking));
		if (Minecraft.getInstance().getConnection() != null) ClientPlayNetworking.send(new Net.Act("placer_open", new CompoundTag()));
	}

	private static void load() {
		if (ALL != null) return;
		List<Thing> out = new ArrayList<>();
		for (String[] r : Species.table("placeables.tsv")) {
			if (r.length < 10) continue;
			try {
				out.add(new Thing(r[1], r[2], r[3], r[4], r[5], Integer.parseInt(r[6].trim()), Integer.parseInt(r[7].trim()), r[8], Integer.parseInt(r[9].trim())));
			} catch (NumberFormatException ignored) {}
		}
		ALL = out;
		Map<String, List<Thing>> m = new LinkedHashMap<>();
		for (String p : ORDER) m.put(p, new ArrayList<>());
		for (Thing t : out) {
			m.computeIfAbsent(t.place(), k -> new ArrayList<>()).add(t);
			BY_KEY.putIfAbsent(t.key(), t);
		}
		m.values().removeIf(List::isEmpty);
		BY_PLACE.putAll(m);
	}

	private static List<String> places() {
		load();
		return new ArrayList<>(BY_PLACE.keySet());
	}

	private static List<Thing> things() {
		List<String> ps = places();
		if (ps.isEmpty()) return List.of();
		return BY_PLACE.getOrDefault(ps.get(Math.max(0, Math.min(ps.size() - 1, placeAt))), List.of());
	}

	private Thing thing() {
		List<Thing> l = things();
		return l.isEmpty() ? null : l.get(Math.max(0, Math.min(l.size() - 1, at)));
	}

	/** A place's name as the game has it (a cave floor's from its id). */
	public static String placeName(String id) {
		Places.Place p = Places.get(id);
		if (p != null) return p.name();
		return id.toUpperCase().replace('_', ' ').replace("POKEMON", "POKéMON").replace(" B1F", " B1F");
	}

	/** Where one is now: placed by you, placed in the mod, taken away, or not placed yet. */
	private static String status(Thing t) {
		String s = ClientState.placed.get(t.key());
		return s == null ? "" : s;
	}

	private static int statusColour(String s) {
		return switch (s) {
			case "mine" -> V.GREEN;
			case "mod" -> V.BLUE;
			case "gone" -> V.RED;
			default -> V.DIM;
		};
	}

	private static String statusWord(String s) {
		return switch (s) {
			case "mine" -> "PLACED";
			case "mod" -> "IN THE MOD";
			case "gone" -> "TAKEN AWAY";
			default -> "NOT PLACED";
		};
	}

	private static int kindColour(String kind) {
		return switch (kind) {
			case "trainer" -> V.RED;
			case "person" -> V.BLUE;
			case "item" -> V.AMBER;
			case "hidden" -> 0xFFB07CE0;
			default -> 0xFF8C93A8;
		};
	}

	private static void send(Thing t, String mode) {
		if (t == null || Minecraft.getInstance().getConnection() == null) return;
		CompoundTag c = new CompoundTag();
		c.putString("key", t.key());
		c.putString("mode", mode);
		ClientPlayNetworking.send(new Net.Act("place", c));
	}

	private void move(int step) {
		List<Thing> l = things();
		if (l.isEmpty()) return;
		at = Math.max(0, Math.min(l.size() - 1, at + step));
		if (at < top) top = at;
		if (at >= top + SHOWN) top = at - SHOWN + 1;
		Sounds.select();
	}

	private void place(int step) {
		List<String> ps = places();
		if (ps.isEmpty()) return;
		placeAt = Math.floorMod(placeAt + step, ps.size());
		at = 0;
		top = 0;
		Sounds.press();
	}

	private void walk() {
		Thing t = thing();
		if (t == null) return;
		walking = t;
		Sounds.press();
		if (minecraft != null) minecraft.setScreen(null);
	}

	@Override
	protected boolean key(KeyEvent e) {
		boolean shift = (e.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0;
		if (Ui.isB(e)) back();
		else if (Ui.isUp(e)) move(shift ? -10 : -1);
		else if (Ui.isDown(e)) move(shift ? 10 : 1);
		else if (Ui.isLeft(e)) place(shift ? -5 : -1);
		else if (Ui.isRight(e)) place(shift ? 5 : 1);
		else if (e.key() == GLFW.GLFW_KEY_W) walk();
		else if (e.key() == GLFW.GLFW_KEY_X) {
			send(thing(), "gone");
			Sounds.press();
		} else if (e.key() == GLFW.GLFW_KEY_U) {
			send(thing(), "back");
			Sounds.press();
		} else if (Ui.isA(e)) {
			send(thing(), "place");
			Sounds.press();
		}
		return true;
	}

	@Override
	protected void back() {
		Sounds.press();
		if (minecraft != null) minecraft.setScreen(parent);
	}

	@Override
	public void onClose() {
		back();
	}

	@Override
	public boolean mouseScrolled(double x, double y, double dx, double dy) {
		int n = things().size();
		top = Math.max(0, Math.min(Math.max(0, n - SHOWN), top - (int) Math.signum(dy) * 3));
		return true;
	}

	@Override
	protected void draw(GuiGraphics g, float partial) {
		Scenes.bg2(g, "options");
		List<String> ps = places();
		int placedN = 0;
		for (Thing t : ALL) if (!status(t).isEmpty() && !status(t).equals("gone")) placedN++;
		// The top: what this is, and how many are down.
		V.panel(g, 40, 24, 1200, 80);
		V.title(g, "PLACER", 64, V.capsTop(52, 30), 30, V.INK);
		V.text(g, "Everyone and everything FireRed has outside and in the caves. Stand where one goes, face its way, and place it.", 64, 72, 16, V.MUTED, false);
		String count = placedN + " OF " + ALL.size() + " DOWN";
		float cw = V.width(count, 16, true) + 32;
		V.pill(g, 40 + 1200 - 24 - cw, 46, cw, 34, V.GREEN);
		V.text(g, count, 40 + 1200 - 24 - cw + 16, 54, 16, V.WHITE, true);

		// The left: the place (left and right change it), then its things.
		float lx = 40, ly = 118, lw = 560, rh = 36;
		String pid = ps.isEmpty() ? "" : ps.get(Math.max(0, Math.min(ps.size() - 1, placeAt)));
		List<Thing> l = things();
		int down = 0;
		for (Thing t : l) if (!status(t).isEmpty() && !status(t).equals("gone")) down++;
		V.panel(g, lx, ly, lw, 56);
		V.tri(g, lx + 26, ly + 28, 12, 2, V.RED);
		V.centred(g, placeName(pid), lx + lw / 2, V.capsTop(ly + 24, 21), 21, V.INK, true);
		V.centred(g, (placeAt + 1) + " OF " + ps.size() + "  ·  " + down + " OF " + l.size() + " DOWN", lx + lw / 2, V.capsTop(ly + 44, 12), 12, V.MUTED, true);
		V.tri(g, lx + lw - 26, ly + 28, 12, 0, V.RED);
		vhit(lx, ly, 70, 56, () -> place(-1));
		vhit(lx + lw - 70, ly, 70, 56, () -> place(1));
		float ty = ly + 66;
		V.panel(g, lx, ty, lw, SHOWN * rh + 30);
		for (int i = 0; i < SHOWN && top + i < l.size(); i++) {
			int idx = top + i;
			Thing t = l.get(idx);
			float ry = ty + 15 + i * rh, rx = lx + 14, rw = lw - 28;
			boolean on = idx == at;
			if (V.over(mx, my, rx, ry, rw, rh)) at = idx;
			if (!on && idx != at - 1 && i < SHOWN - 1) V.rr(g, rx, ry + rh - 1.5F, rw, 1.5F, 0, 0xFFEEF1F8);
			V.item(g, "", null, rx, ry, rw, rh, on, false, 17);
			String kind = t.kind().toUpperCase();
			V.pill(g, rx + 8, ry + 7, 86, rh - 14, kindColour(t.kind()));
			V.centred(g, kind, rx + 51, V.capsTop(ry + rh / 2, 12), 12, V.WHITE, true);
			String st = status(t);
			String label = t.label();
			float room = rw - 104 - V.width(statusWord(st), 12, true) - 30;
			while (V.width(label, 16, on) > room && label.length() > 4) label = label.substring(0, label.length() - 2);
			if (!label.equals(t.label())) label = label.trim() + "...";
			V.text(g, label, rx + 104, V.capsTop(ry + rh / 2, 16), 16, V.INK, on);
			V.right(g, statusWord(st), rx + rw - 12, V.capsTop(ry + rh / 2, 12), 12, statusColour(st), true);
			int ii = idx;
			vhit(rx, ry, rw, rh, () -> {
				if (at == ii) {
					send(thing(), "place");
					Sounds.press();
				} else at = ii;
			});
		}
		if (l.size() > SHOWN) {
			float th = SHOWN * rh, bh = Math.max(30, th * SHOWN / l.size()), by = ty + 15 + (th - bh) * top / Math.max(1, l.size() - SHOWN);
			V.rr(g, lx + lw - 8, ty + 15, 4, th, 2, 0xFFEEF1F8);
			V.rr(g, lx + lw - 8, by, 4, bh, 2, V.DIM);
		}

		// The right: the one you're on, and what to do with it.
		float px = 616, py = 118, pw = 624, ph = 66 + SHOWN * rh + 30;
		V.panel(g, px, py, pw, ph);
		Thing t = thing();
		if (t != null) {
			String chip = t.kind().toUpperCase() + "  " + placeName(t.place());
			float chw = V.width(chip, 14, true) + 28;
			V.pill(g, px + 24, py + 20, chw, 28, kindColour(t.kind()));
			V.text(g, chip, px + 38, py + 27, 14, V.WHITE, true);
			String st = status(t);
			float sw = V.width(statusWord(st), 14, true) + 28;
			V.pill(g, px + pw - 24 - sw, py + 20, sw, 28, statusColour(st));
			V.text(g, statusWord(st), px + pw - 24 - sw + 14, py + 27, 14, V.WHITE, true);
			List<String> wl = V.wrap(t.label(), pw - 48, 24, true);
			for (int i = 0; i < Math.min(2, wl.size()); i++) V.title(g, wl.get(i), px + 24, py + 62 + i * 30, 24, V.INK);
			float y = py + 70 + Math.min(2, wl.size()) * 30;
			String what = t.kind().equals("trainer") ? "TEAM: " + t.detail() : t.kind().equals("item") || t.kind().equals("hidden")
					? (t.kind().equals("hidden") ? "Hidden (found with A): " : "In a POKé BALL: ") + t.detail().replace("item=", "").replace('_', ' ').toUpperCase()
					: t.detail().equals("-") ? "" : t.detail();
			List<String> dl = V.wrap(what, pw - 48, 16, false);
			for (int i = 0; i < Math.min(4, dl.size()); i++) V.text(g, dl.get(i), px + 24, y + i * 22, 16, V.TEXT, false);
			y += Math.min(4, dl.size()) * 22 + 12;
			V.text(g, "FIRERED'S SPOT", px + 24, y, 13, V.MUTED, true);
			String spot = "step " + t.gx() + ", " + t.gy() + (t.facing().equals("none") || t.facing().isEmpty() ? "" : "  ·  facing " + t.facing().replace('_', ' '))
					+ (t.sight() > 0 ? "  ·  sees " + t.sight() + (t.sight() == 1 ? " step" : " steps") : "");
			V.text(g, spot, px + 170, y - 1, 15, V.INK, false);
			y += 26;
			V.text(g, "HOW", px + 24, y, 13, V.MUTED, true);
			String how = t.kind().equals("trainer") ? "Face the way they look: they see straight ahead, a step (two blocks) wide."
					: t.kind().equals("boulder") ? "Stand on the boulder's spot (it's two blocks across): STRENGTH pushes it a step at a time."
					: t.kind().equals("switch") ? "Stand on the switch: a boulder pushed onto it opens its rocks."
					: t.kind().equals("barrier") ? "Stand on the rocks' spot: they crumble when a boulder's on their switch."
					: t.kind().equals("hidden") ? "Stand on the spot: A facing it (or the ITEMFINDER) finds it." : "Stand on the spot where it goes.";
			List<String> hl = V.wrap(how, pw - 190, 15, false);
			for (int i = 0; i < Math.min(2, hl.size()); i++) V.text(g, hl.get(i), px + 170, y - 1 + i * 20, 15, V.INK, false);

			// The buttons: PLACE HERE, WALK AND PLACE, TAKE AWAY, PUT BACK.
			float by = py + ph - 24 - 40, bw = (pw - 36 - 3 * 12) / 4F;
			String[] bl = {"PLACE HERE", "WALK AND PLACE", "TAKE AWAY", "PUT BACK"};
			int[] bc = {V.GREEN, V.BLUE, V.RED, V.AMBER};
			Runnable[] br = {() -> {
				send(thing(), "place");
				Sounds.press();
			}, this::walk, () -> {
				send(thing(), "gone");
				Sounds.press();
			}, () -> {
				send(thing(), "back");
				Sounds.press();
			}};
			for (int i = 0; i < 4; i++) {
				float bx = px + 18 + i * (bw + 12);
				V.pill(g, bx, by, bw, 40, bc[i]);
				V.centred(g, bl[i], bx + bw / 2, V.capsTop(by + 20, 14), 14, V.WHITE, true);
				vhit(bx, by, bw, 40, br[i]);
			}
			String file = ClientState.placerFile.isEmpty() ? "config/retromon_placed.tsv" : ClientState.placerFile;
			List<String> fl = V.wrap("Saved as you go to " + file + ". Send it to Claude to build into the mod.", pw - 48, 13, false);
			for (int i = 0; i < Math.min(2, fl.size()); i++) V.text(g, fl.get(i), px + 24, by - 44 + i * 18, 13, V.MUTED, false);
		}
		V.hints(g, "A", "Place here", "W", "Walk and place", "X", "Take away", "U", "Put back", "B", "Back");
	}

	// ---------- WALK AND PLACE: out on the map ----------

	/** The next of the same place not down yet (after this one), or the next one at all; null at the end. */
	private static Thing next(Thing t, boolean undone) {
		List<Thing> l = BY_PLACE.getOrDefault(t.place(), List.of());
		int i = l.indexOf(t);
		for (int k = i + 1; k < l.size(); k++) {
			String s = status(l.get(k));
			if (!undone || s.isEmpty() || s.equals("gone")) return l.get(k);
		}
		return i + 1 < l.size() ? l.get(i + 1) : null;
	}

	/** P, N, U and X out on the map while placing. True if it used the key. */
	public static boolean fieldKey(KeyEvent e, int action) {
		Minecraft mc = Minecraft.getInstance();
		if (walking == null || mc.screen != null || !Adventure.active()) return false;
		int k = e.key();
		if (k != GLFW.GLFW_KEY_P && k != GLFW.GLFW_KEY_N && k != GLFW.GLFW_KEY_U && k != GLFW.GLFW_KEY_X) return false;
		if (action != GLFW.GLFW_PRESS) return true;
		switch (k) {
			case GLFW.GLFW_KEY_P -> {
				send(walking, "place");
				lastPlaced = walking;
				Sounds.press();
				Thing n = next(walking, true);
				if (n != null) walking = n;
			}
			case GLFW.GLFW_KEY_N -> {
				Thing n = next(walking, false);
				Sounds.select();
				if (n != null) walking = n;
			}
			case GLFW.GLFW_KEY_U -> {
				if (lastPlaced != null) {
					send(lastPlaced, "back");
					walking = lastPlaced;
					lastPlaced = null;
					Sounds.press();
				}
			}
			default -> {
				walking = null;
				Sounds.press();
			}
		}
		return true;
	}

	/** While placing: what you're putting down, at the top of the screen. */
	public static void hud(GuiGraphics g) {
		Thing t = walking;
		if (t == null) return;
		String head = "PLACING  " + t.kind().toUpperCase() + "  " + t.label();
		String sub = placeName(t.place()) + "  ·  FireRed's step " + t.gx() + ", " + t.gy()
				+ (t.facing().equals("none") || t.facing().isEmpty() ? "" : "  ·  facing " + t.facing().replace('_', ' '));
		String keys = "P  Place here     N  Next     U  Undo     X  Stop";
		float w = Math.max(V.width(head, 18, true), Math.max(V.width(sub, 14, false), V.width(keys, 14, true))) + 48, x = (V.W - w) / 2, y = 96, h = 92;
		V.rr(g, x - 3, y - 3, w + 6, h + 6, 25, 0x8CFFFFFF);
		V.rr(g, x, y, w, h, 22, 0xFFFFFFFF);
		V.rr(g, x, y, 8, h, 4, kindColour(t.kind()));
		V.text(g, head, x + 24, y + 14, 18, V.INK, true);
		V.text(g, sub, x + 24, y + 42, 14, V.MUTED, false);
		V.text(g, keys, x + 24, y + 66, 14, V.BLUE, true);
	}
}
