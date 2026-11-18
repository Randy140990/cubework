package com.insidethecube.retromon.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.insidethecube.retromon.engine.Species;
import com.insidethecube.retromon.game.Net;
import com.insidethecube.retromon.game.PlayerData;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.nbt.CompoundTag;

/**
 * The Town Map, for Kanto and Johto: the games' own towns, routes and places where the Pokégear had them,
 * joined by the routes between them. Where you are pulses red; the places you've been are bright, the rest
 * dimmed. Pick a place to see its name; a town you've been to can be flown to (a Pokémon of yours that knows
 * FLY, and the THUNDERBADGE). The Pokédex can open it with a Pokémon's places marked.
 */
public class TownMapScreen extends GameScreen {
	/** A spot on the map: a town, a route or a place (cave, tower, forest...). */
	public record Point(String key, String region, int x, int y, String kind, String name) {}

	private static Map<String, Point> points;
	private static Map<String, String> placeToPoint;
	private static List<String[]> edges;

	private final Screen parent;
	private String region;
	private String at;
	private String highlightTitle;
	private final Set<String> highlight = new HashSet<>();

	public TownMapScreen(Screen parent) {
		super("Town Map");
		this.parent = parent;
		load();
		String here = placeToPoint.get(ClientState.data.place);
		Point p = here == null ? null : points.get(here);
		region = p == null ? "kanto" : p.region();
		at = p == null ? first(region) : p.key();
	}

	private static void load() {
		if (points != null) return;
		points = new LinkedHashMap<>();
		for (String[] r : Species.table("map_points.tsv")) {
			points.put(r[0], new Point(r[0], r[1], Integer.parseInt(r[2].trim()), Integer.parseInt(r[3].trim()), r[4], r[5]));
		}
		placeToPoint = new HashMap<>();
		for (String[] r : Species.table("place_landmarks.tsv")) placeToPoint.put(r[0], r[1]);
		edges = new ArrayList<>();
		for (String[] r : Species.table("map_edges.tsv")) edges.add(new String[] {r[0], r[1]});
	}

	/** Marks a Pokémon's places (from the Pokédex). */
	public void highlight(String title, Set<String> places) {
		highlightTitle = title;
		for (String pl : places) {
			String k = placeToPoint.get(pl);
			if (k != null) highlight.add(k);
		}
		for (String k : highlight) {
			Point p = points.get(k);
			if (p != null) {
				region = p.region();
				at = k;
				break;
			}
		}
	}

	private String first(String reg) {
		for (Point p : points.values()) if (p.region().equals(reg) && p.kind().equals("town")) return p.key();
		return null;
	}

	private List<Point> inRegion() {
		List<Point> out = new ArrayList<>();
		for (Point p : points.values()) if (p.region().equals(region)) out.add(p);
		return out;
	}

	private boolean visited(String key) {
		for (String pl : ClientState.data.visited) if (key.equals(placeToPoint.get(pl))) return true;
		return false;
	}

	/** The place to fly to for a point: a place you've visited that's in this town. */
	private String flyPlace(String key) {
		for (String pl : ClientState.data.visited) if (key.equals(placeToPoint.get(pl)) && !pl.contains("_gym") && !pl.contains("house")) return pl;
		return null;
	}

	private boolean canFly() {
		PlayerData d = ClientState.data;
		return d.badges.contains("THUNDERBADGE") && d.party.stream().anyMatch(m -> m.moves.contains("fly") && !m.fainted());
	}

	/** The nearest point in a direction (dx, dy), for the arrow keys. */
	private void step(int dx, int dy) {
		Point from = points.get(at);
		if (from == null) {
			at = first(region);
			return;
		}
		Point best = null;
		double bestScore = 1e9;
		for (Point p : inRegion()) {
			if (p.key().equals(at)) continue;
			int vx = p.x() - from.x(), vy = p.y() - from.y();
			double along = vx * dx + vy * dy;
			if (along <= 0) continue;
			double across = Math.abs(vx * dy - vy * dx);
			double score = along + across * 2;
			if (score < bestScore) {
				bestScore = score;
				best = p;
			}
		}
		if (best != null) {
			at = best.key();
			Sounds.press();
		}
	}

	private void fly() {
		String pl = flyPlace(at);
		if (pl == null || !canFly() || minecraft == null) return;
		Sounds.press();
		CompoundTag t = new CompoundTag();
		t.putString("place", pl);
		ClientPlayNetworking.send(new Net.Act("fly", t));
		minecraft.setScreen(null);
	}

	@Override
	protected boolean key(KeyEvent e) {
		if (Ui.isUp(e)) step(0, -1);
		else if (Ui.isDown(e)) step(0, 1);
		else if (Ui.isLeft(e)) step(-1, 0);
		else if (Ui.isRight(e)) step(1, 0);
		else if (e.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_TAB) switchRegion();
		else if (Ui.isA(e)) fly();
		else if (Ui.isB(e)) back();
		return true;
	}

	private void switchRegion() {
		region = region.equals("kanto") ? "johto" : "kanto";
		at = first(region);
		Sounds.press();
	}

	@Override
	protected void back() {
		Sounds.press();
		if (minecraft != null) minecraft.setScreen(parent);
	}

	/** A thick line from one point to another (the route between two places). */
	private static void line(GuiGraphics g, int x0, int y0, int x1, int y1, int thick, int color) {
		int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0)) / Math.max(1, thick / 2) + 1;
		for (int i = 0; i <= steps; i++) {
			int x = x0 + (x1 - x0) * i / steps, y = y0 + (y1 - y0) * i / steps;
			g.fill(x - thick / 2, y - thick / 2, x + (thick + 1) / 2, y + (thick + 1) / 2, color);
		}
	}

	@Override
	protected void draw(GuiGraphics g, float partial) {
		g.fill(0, 0, width, height, 0xFF0E2240);
		int m = 8 * s;
		// The sea, with a soft gradient and faint wave lines.
		for (int y = 0; y < height; y += 2 * s) g.fill(0, y, width, y + s, 0x08FFFFFF);
		List<Point> pts = inRegion();
		if (pts.isEmpty()) return;
		int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
		for (Point p : pts) {
			minX = Math.min(minX, p.x());
			minY = Math.min(minY, p.y());
			maxX = Math.max(maxX, p.x());
			maxY = Math.max(maxY, p.y());
		}
		int panelW = 120 * s;
		int areaX = m + 6 * s, areaY = m + 26 * s, areaW = width - 2 * m - panelW - 18 * s, areaH = height - areaY - m - 10 * s;
		float k = Math.min(areaW / (float) Math.max(1, maxX - minX), areaH / (float) Math.max(1, maxY - minY));
		int fMinX = minX, fMinY = minY;
		int offX = areaX + (int) ((areaW - (maxX - minX) * k) / 2), offY = areaY + (int) ((areaH - (maxY - minY) * k) / 2);
		Map<String, int[]> at2 = new HashMap<>();
		for (Point p : pts) at2.put(p.key(), new int[] {offX + Math.round((p.x() - fMinX) * k), offY + Math.round((p.y() - fMinY) * k)});
		// Land first: wide green bands along the routes, then the paths on them.
		for (String[] e : edges) {
			int[] a = at2.get(e[0]), b = at2.get(e[1]);
			if (a == null || b == null) continue;
			line(g, a[0], a[1], b[0], b[1], 14 * s, 0xFF3E8C4A);
		}
		for (Point p : pts) {
			int[] c = at2.get(p.key());
			Ui.round(g, c[0] - 8 * s, c[1] - 8 * s, 16 * s, 16 * s, 0xFF3E8C4A, 6 * s);
		}
		for (String[] e : edges) {
			int[] a = at2.get(e[0]), b = at2.get(e[1]);
			if (a == null || b == null) continue;
			line(g, a[0], a[1], b[0], b[1], 4 * s, 0xFFE9D8A6);
		}
		String here = placeToPoint.get(ClientState.data.place);
		for (Point p : pts) {
			int[] c = at2.get(p.key());
			boolean been = visited(p.key()), sel = p.key().equals(at);
			if (over(c[0] - 8 * s, c[1] - 8 * s, 16 * s, 16 * s)) at = p.key();
			if (highlight.contains(p.key())) {
				float pulse = (float) (0.5 + 0.5 * Math.sin((ticks + partial) * 0.2));
				int r = (int) ((9 + 3 * pulse) * s);
				Ui.round(g, c[0] - r, c[1] - r, 2 * r, 2 * r, Ui.alpha(0xFFFFD34A, 0.45F + 0.3F * pulse), r);
			}
			switch (p.kind()) {
				case "town" -> {
					int z = 7 * s;
					Ui.round(g, c[0] - z, c[1] - z, 2 * z, 2 * z, sel ? Ui.CREAM : 0xFF1C2742, 2 * s);
					Ui.round(g, c[0] - z + s, c[1] - z + s, 2 * z - 2 * s, 2 * z - 2 * s, been ? 0xFFE5525C : 0xFF6C7590, 2 * s);
				}
				case "route" -> {
					int z = 3 * s;
					Ui.round(g, c[0] - z, c[1] - z, 2 * z, 2 * z, sel ? Ui.CREAM : been ? 0xFFF4F0E0 : 0xFF9AA2B8, z);
				}
				default -> {
					int z = 4 * s;
					Ui.round(g, c[0] - z, c[1] - z, 2 * z, 2 * z, sel ? Ui.CREAM : been ? 0xFF8E6CD8 : 0xFF5A6080, s);
				}
			}
			if (p.key().equals(here)) {
				float pulse = (float) (0.5 + 0.5 * Math.sin((ticks + partial) * 0.3));
				int r = (int) ((10 + 2 * pulse) * s);
				int col = Ui.alpha(0xFFFF3B4A, 0.5F + 0.4F * pulse);
				g.fill(c[0] - r, c[1] - r, c[0] + r, c[1] - r + s, col);
				g.fill(c[0] - r, c[1] + r - s, c[0] + r, c[1] + r, col);
				g.fill(c[0] - r, c[1] - r, c[0] - r + s, c[1] + r, col);
				g.fill(c[0] + r - s, c[1] - r, c[0] + r, c[1] + r, col);
			}
			String key = p.key();
			hit(c[0] - 8 * s, c[1] - 8 * s, 16 * s, 16 * s, () -> {
				if (key.equals(at)) fly();
				at = key;
			});
		}
		// The title bar and the region tabs.
		Ui.round(g, m, m, width - 2 * m, 20 * s, 0xE6141B2E, 3 * s);
		Ui.draw(g, font, Ui.bold("TOWN MAP"), m + 10 * s, m + 6 * s, Ui.TEXT, s, true);
		for (int i = 0; i < 2; i++) {
			String r = i == 0 ? "kanto" : "johto";
			int tw = 56 * s, bx = m + 80 * s + i * (tw + 4 * s);
			boolean on = r.equals(region);
			Ui.round(g, bx, m + 3 * s, tw, 14 * s, on ? Ui.CREAM : 0x33FFFFFF, 2 * s);
			Ui.draw(g, font, Ui.bold(r.toUpperCase()), bx + 9 * s, m + 6 * s, on ? Ui.CREAM_INK : Ui.TEXT, s, false);
			hit(bx, m + 3 * s, tw, 14 * s, () -> {
				if (!r.equals(region)) switchRegion();
			});
		}
		if (highlightTitle != null) Ui.right(g, font, "Where " + highlightTitle + " lives", width - m - 10 * s, m + 6 * s, 0xFFFFD34A, s);
		// The panel: the picked place.
		int px = width - m - panelW, py = m + 26 * s, ph = 96 * s;
		Ui.round(g, px, py, panelW, ph, 0xF0141B2E, 3 * s);
		Point sel = points.get(at);
		if (sel != null) {
			List<String> nm = Ui.wrap(font, sel.name(), (panelW - 16 * s) / s);
			for (int i = 0; i < nm.size() && i < 2; i++) Ui.draw(g, font, Ui.bold(nm.get(i)), px + 8 * s, py + 8 * s + i * 11 * s, Ui.TEXT, s, true);
			String kind = sel.kind().equals("town") ? "Town" : sel.kind().equals("route") ? "Route" : "Place";
			Ui.text(g, font, kind + (visited(sel.key()) ? "  ·  Visited" : "  ·  Not yet visited"), px + 8 * s, py + 32 * s, Ui.MUTED, s);
			if (sel.key().equals(here)) Ui.text(g, font, "You are here.", px + 8 * s, py + 44 * s, 0xFFFF6B76, s);
			if (sel.kind().equals("town")) {
				boolean ok = flyPlace(sel.key()) != null && canFly();
				int bw = panelW - 16 * s, bx = px + 8 * s, by = py + ph - 22 * s;
				Ui.button(g, font, bx, by, bw, 14 * s, "FLY HERE", ok ? 0xFF4A8FE0 : 0xFF4A5068, ok && over(bx, by, bw, 14 * s), s);
				if (ok) hit(bx, by, bw, 14 * s, this::fly);
				else Ui.text(g, font, canFly() ? "Visit it first." : "Needs FLY + THUNDERBADGE.", px + 8 * s, py + ph - 34 * s, Ui.DIM, s);
			}
		}
		Ui.text(g, font, "Arrows: move   Tab: KANTO / JOHTO   A: fly   B: back", m, height - m - 6 * s, Ui.DIM, s);
	}
}
