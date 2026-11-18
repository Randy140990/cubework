package com.insidethecube.retromon.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.insidethecube.retromon.engine.Places;
import com.insidethecube.retromon.engine.Species;
import com.insidethecube.retromon.game.Net;
import com.insidethecube.retromon.game.PlayerData;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;

/**
 * The Town Map, on your Pokédex. MAP: the world itself, drawn from above in the newer games' soft colours (see
 * TownMap), from the whole land down to the houses of a town: the mouse wheel (or Q and E) zooms, dragging (or
 * the arrows) moves it, C comes back to you. The places marked on the map have their names on it; picking one
 * shows its picture (taken the first time you were there) and, for a town you've been to, FLY HERE. You're the
 * arrow, pointing the way you face. KANTO and JOHTO: the games' own Town Map of each region, every place on it.
 * The Pokédex can open it with a Pokémon's places marked.
 */
public class TownMapScreen extends DeviceScreen {
	/** A spot on the games' map: a town, a route or a place (cave, tower, forest...). */
	public record Point(String key, String region, int x, int y, String kind, String name) {}

	private static Map<String, Point> points;
	private static Map<String, String> placeToPoint;
	private static List<String[]> edges;

	private final Screen parent;
	/** MAP (the world), KANTO or JOHTO (the games' maps). */
	private String tab = "map";
	private String region = "kanto";
	private String at;
	private String highlightTitle;
	private final Set<String> highlight = new HashSet<>();
	private final Set<String> highlightPlaces = new HashSet<>();

	// The world map: where it's looking (a world x, z in the middle), and how big a block is (screen pixels).
	private double cx, cz, zoom = 1.6, zoomTo = 1.6;
	private boolean dragging;
	private String picked;
	private int mapW, mapH;

	public TownMapScreen(Screen parent) {
		super("Town Map");
		this.parent = parent;
		load();
		String here = placeToPoint.get(ClientState.data.place);
		Point p = here == null ? null : points.get(here);
		region = p == null ? "kanto" : p.region();
		at = p == null ? first(region) : p.key();
		var me = net.minecraft.client.Minecraft.getInstance().player;
		if (me != null) {
			cx = me.getX();
			cz = me.getZ();
		} else {
			cx = (TownMap.view[0] + TownMap.view[2]) / 2.0;
			cz = (TownMap.view[1] + TownMap.view[3]) / 2.0;
		}
		picked = ClientState.data.place == null || ClientState.data.place.isEmpty() ? null : ClientState.data.place;
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
		highlightPlaces.addAll(places);
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
		// On the world map if one of its places is marked there (centred on it); the games' map otherwise.
		ClientState.Zone z = ClientState.zones.stream().filter(zz -> places.contains(zz.place())).findFirst().orElse(null);
		if (z != null) {
			cx = (z.x1() + z.x2()) / 2.0;
			cz = (z.z1() + z.z2()) / 2.0;
			zoom = zoomTo = 1.0;
		} else tab = region;
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

	private void flyTo(String place) {
		if (place == null || !canFly() || minecraft == null) return;
		Sounds.press();
		CompoundTag t = new CompoundTag();
		t.putString("place", place);
		ClientPlayNetworking.send(new Net.Act("fly", t));
		minecraft.setScreen(null);
	}

	private void fly() {
		flyTo(flyPlace(at));
	}

	/** A place on the world map can be flown to: a town you've been to. */
	private boolean flyable(String place) {
		if (place == null || !ClientState.data.visited.contains(place)) return false;
		Point p = points.get(placeToPoint.getOrDefault(place, ""));
		return p != null && p.kind().equals("town");
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

	// ---------- the world map's view ----------

	/** The least zoom: the whole land in view. */
	private double fit() {
		int[] v = TownMap.view;
		if (mapW <= 0 || mapH <= 0) return 0.2;
		return Math.min(mapW / (double) (v[2] - v[0]), mapH / (double) (v[3] - v[1]));
	}

	private static final double MAX_ZOOM = 6;

	private void zoomBy(double k, double sx, double sy) {
		double before = zoomTo;
		zoomTo = Math.max(fit(), Math.min(MAX_ZOOM, zoomTo * k));
		// Keep the point under the mouse where it is.
		double wx = cx + (sx - mapW / 2.0) / zoom, wz = cz + (sy - mapH / 2.0) / zoom;
		double after = zoomTo;
		cx = wx - (wx - cx) * before / after;
		cz = wz - (wz - cz) * before / after;
		clampView();
	}

	private void clampView() {
		int[] a = TownMap.all;
		cx = Math.max(a[0], Math.min(a[2], cx));
		cz = Math.max(a[1], Math.min(a[3], cz));
	}

	private void centreOnYou() {
		var me = net.minecraft.client.Minecraft.getInstance().player;
		if (me == null) return;
		cx = me.getX();
		cz = me.getZ();
		if (zoomTo < 1.2) zoomTo = 1.6;
		Sounds.press();
	}

	@Override
	public void tick() {
		super.tick();
		zoom += (zoomTo - zoom) * 0.35;
		if (Math.abs(zoom - zoomTo) < 0.001) zoom = zoomTo;
	}

	// ---------- keys and the mouse ----------

	@Override
	protected boolean key(KeyEvent e) {
		int k = e.key();
		if (k == org.lwjgl.glfw.GLFW.GLFW_KEY_TAB) {
			tab = tab.equals("map") ? "kanto" : tab.equals("kanto") ? "johto" : "map";
			if (!tab.equals("map")) {
				region = tab;
				at = first(region);
			}
			Sounds.press();
			return true;
		}
		if (Ui.isB(e)) {
			back();
			return true;
		}
		if (tab.equals("map")) {
			double stepBlocks = 40 / Math.max(0.2, zoom);
			if (Ui.isUp(e)) cz -= stepBlocks;
			else if (Ui.isDown(e)) cz += stepBlocks;
			else if (Ui.isLeft(e)) cx -= stepBlocks;
			else if (Ui.isRight(e)) cx += stepBlocks;
			else if (k == org.lwjgl.glfw.GLFW.GLFW_KEY_E || k == org.lwjgl.glfw.GLFW.GLFW_KEY_EQUAL || k == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ADD)
				zoomBy(1.5, mapW / 2.0, mapH / 2.0);
			else if (k == org.lwjgl.glfw.GLFW.GLFW_KEY_Q || k == org.lwjgl.glfw.GLFW.GLFW_KEY_MINUS || k == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_SUBTRACT)
				zoomBy(1 / 1.5, mapW / 2.0, mapH / 2.0);
			else if (k == org.lwjgl.glfw.GLFW.GLFW_KEY_C) centreOnYou();
			else if (Ui.isA(e) && picked != null && flyable(picked)) flyTo(picked);
			clampView();
			return true;
		}
		if (Ui.isUp(e)) step(0, -1);
		else if (Ui.isDown(e)) step(0, 1);
		else if (Ui.isLeft(e)) step(-1, 0);
		else if (Ui.isRight(e)) step(1, 0);
		else if (Ui.isA(e)) fly();
		return true;
	}

	@Override
	public boolean mouseScrolled(double x, double y, double dx, double dy) {
		if (!tab.equals("map")) return true;
		double[] p = onScreen(x, y);
		if (p == null) return true;
		zoomBy(dy > 0 ? 1.25 : 1 / 1.25, p[0], p[1] - mapTop());
		return true;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (tab.equals("map") && event.button() == 0) {
			double[] p = onScreen(event.x(), event.y());
			dragging = p != null && p[1] > mapTop();
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (dragging && tab.equals("map")) {
			cx -= dx / zoom;
			cz -= dy / zoom;
			clampView();
			return true;
		}
		return super.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		dragging = false;
		return super.mouseReleased(event);
	}

	@Override
	protected void back() {
		Sounds.press();
		to(parent);
	}

	/** A thick line from one point to another (the route between two places). */
	private static void line(GuiGraphics g, int x0, int y0, int x1, int y1, int thick, int color) {
		int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0)) / Math.max(1, thick / 2) + 1;
		for (int i = 0; i <= steps; i++) {
			int x = x0 + (x1 - x0) * i / steps, y = y0 + (y1 - y0) * i / steps;
			g.fill(x - thick / 2, y - thick / 2, x + (thick + 1) / 2, y + (thick + 1) / 2, color);
		}
	}

	// ---------- drawing ----------

	private int mapTop() {
		return 0;
	}

	@Override
	protected void drawScreen(GuiGraphics g, float partial) {
		if (tab.equals("map")) drawWorld(g, partial);
		else drawRegion(g, partial);
		drawTabs(g);
	}

	/** The tabs along the top: MAP, KANTO, JOHTO. */
	private void drawTabs(GuiGraphics g) {
		int m = 6 * s, x = m;
		String[] tabs = {"map", "kanto", "johto"};
		for (String t : tabs) {
			String label = t.toUpperCase();
			int tw = Ui.boldWidth(font, label, s) + 16 * s;
			boolean on = t.equals(tab);
			Ui.round(g, x + s, m + 2 * s, tw, 14 * s, 0x50000000, 4 * s);
			Ui.round(g, x, m, tw, 14 * s, on ? Ui.CREAM : 0xD0123C38, 4 * s);
			Ui.draw(g, font, Ui.bold(label), x + 8 * s, m + 3 * s, on ? Ui.CREAM_INK : Ui.TEXT, s, false);
			hit(x, m, tw, 14 * s, () -> {
				tab = t;
				if (!t.equals("map")) {
					region = t;
					at = first(region);
				}
			});
			x += tw + 4 * s;
		}
		if (highlightTitle != null) {
			String h = "Where " + highlightTitle + " lives";
			int hw = Ui.width(font, h, s) + 12 * s;
			Ui.round(g, x + 4 * s, m, hw, 14 * s, 0xD0123C38, 4 * s);
			Ui.text(g, font, h, x + 10 * s, m + 3 * s, 0xFFFFD34A, s);
		}
	}

	/** The world, from above. */
	private void drawWorld(GuiGraphics g, float partial) {
		mapW = width;
		mapH = height;
		if (zoomTo < fit()) zoomTo = fit();
		g.fill(0, 0, width, height, TownMap.SEA);
		if (!TownMap.ready()) {
			Ui.centred(g, font, "Drawing the map...", width / 2, height / 2 - 4 * s, Ui.TEXT, s, true);
			return;
		}
		TownMap.upload();
		double z = zoom;
		// A pixel for every 4 blocks when it's zoomed right out; the regions themselves closer in.
		if (z < 0.6) {
			int[] a = TownMap.all;
			int x0 = sx(a[0], z), y0 = sy(a[1], z), x1 = sx(a[0] + TownMap.overW() * TownMap.OVER, z), y1 = sy(a[1] + TownMap.overH() * TownMap.OVER, z);
			if (TownMap.overW() > 0) g.blit(RenderPipelines.GUI_TEXTURED, TownMap.OVERVIEW, x0, y0, 0, 0, x1 - x0, y1 - y0, TownMap.overW(), TownMap.overH(),
					TownMap.overW(), TownMap.overH());
		} else {
			for (TownMap.Region r : TownMap.regions()) {
				Identifier tex = TownMap.texture(r);
				if (tex == null) continue;
				int x0 = sx(r.rx * TownMap.R, z), y0 = sy(r.rz * TownMap.R, z), x1 = sx((r.rx + 1) * TownMap.R, z), y1 = sy((r.rz + 1) * TownMap.R, z);
				if (x1 < 0 || y1 < 0 || x0 > width || y0 > height) continue;
				g.blit(RenderPipelines.GUI_TEXTURED, tex, x0, y0, 0, 0, x1 - x0, y1 - y0, TownMap.R, TownMap.R, TownMap.R, TownMap.R);
			}
		}
		// The places: marked ones glow (a Pokémon's, from the Pokédex), then names.
		String hover = null;
		for (ClientState.Zone zn : ClientState.zones) {
			int x0 = sx(zn.x1(), z), y0 = sy(zn.z1(), z), x1 = sx(zn.x2() + 1, z), y1 = sy(zn.z2() + 1, z);
			if (highlightPlaces.contains(zn.place())) {
				float pulse = (float) (0.5 + 0.5 * Math.sin((ticks + partial) * 0.2));
				g.fill(x0, y0, x1, y1, Ui.alpha(0xFFFFD34A, 0.18F + 0.14F * pulse));
				frame(g, x0, y0, x1, y1, 0xFFFFD34A);
			}
			if (zn.place().equals(picked)) frame(g, x0, y0, x1, y1, 0xCCFFFFFF);
			if (over(x0, y0, x1 - x0, y1 - y0)) hover = zn.place();
		}
		// Clicking the map picks the place under the mouse (the names, over it, are clicked first).
		String under = hover;
		hit(0, 0, width, height, () -> pick(under));
		// Names: towns always (bigger), routes once it's close enough to read them.
		List<int[]> taken = new ArrayList<>();
		for (ClientState.Zone zn : ClientState.zones) {
			Point pt = points.get(placeToPoint.getOrDefault(zn.place(), ""));
			boolean town = pt != null && pt.kind().equals("town");
			if (!town && z < 0.9) continue;
			Places.Place pl = Places.get(zn.place());
			String name = pl == null ? zn.place().toUpperCase().replace('_', ' ') : pl.name();
			int lx = sx((zn.x1() + zn.x2()) / 2.0, z), ly = town ? sy(zn.z1(), z) + 6 * s : sy((zn.z1() + zn.z2()) / 2.0, z);
			int lw = (town ? Ui.boldWidth(font, name, s) : Ui.width(font, name, s)) + 12 * s, lh = 13 * s;
			int bx = lx - lw / 2, by = ly - lh / 2;
			boolean clash = false;
			for (int[] t : taken) if (bx < t[0] + t[2] && bx + lw > t[0] && by < t[1] + t[3] && by + lh > t[1]) clash = true;
			if (clash && !zn.place().equals(picked)) continue;
			taken.add(new int[] {bx, by, lw, lh});
			boolean been = ClientState.data.visited.contains(zn.place());
			boolean sel = zn.place().equals(picked);
			Ui.round(g, bx + s, by + 2 * s, lw, lh, 0x50000000, 4 * s);
			Ui.round(g, bx, by, lw, lh, sel ? Ui.CREAM : town ? (been ? 0xF0E5525C : 0xE0485068) : 0xD0123C38, 4 * s);
			int ink = sel ? Ui.CREAM_INK : 0xFFFFFFFF;
			if (town) Ui.draw(g, font, Ui.bold(name), bx + 6 * s, by + 3 * s, ink, s, false);
			else Ui.plain(g, font, name, bx + 6 * s, by + 3 * s, sel ? Ui.CREAM_INK : Ui.TEXT, s);
			String place = zn.place();
			hit(bx, by, lw, lh, () -> pick(place));
		}
		// You: an arrow the way you face, with a ring.
		var me = minecraft == null ? null : minecraft.player;
		if (me != null) {
			double px = me.getX(), pz = me.getZ();
			int mx0 = sx(px, z), my0 = sy(pz, z);
			float pulse = (float) (0.5 + 0.5 * Math.sin((ticks + partial) * 0.25));
			int rr = (int) ((9 + 3 * pulse) * s);
			Device.disc(g, mx0, my0, rr, rr, Ui.alpha(0xFFFF4A5A, 0.25F + 0.15F * pulse));
			Device.disc(g, mx0, my0, 7 * s, 7 * s, 0xFFFFFFFF);
			Device.disc(g, mx0, my0, 6 * s, 6 * s, 0xFFE5394A);
			g.pose().pushMatrix();
			g.pose().translate(mx0, my0);
			g.pose().rotate((float) Math.toRadians(me.getYRot() + 180));
			Ui.icon(g, "icon/arrow", -5 * s, -5 * s, 10 * s, 0xFFFFFFFF);
			g.pose().popMatrix();
		}
		drawCompass(g, z);
		drawCard(g);
		Ui.text(g, font, "Wheel/Q/E: zoom   Drag/arrows: move   C: you   Tab: maps", 6 * s, height - 10 * s, 0xE0FFFFFF, s);
	}

	private void pick(String place) {
		if (place != null && place.equals(picked) && flyable(place) && canFly()) {
			flyTo(place);
			return;
		}
		picked = place;
		if (place != null) Sounds.select();
	}

	private int sx(double wx, double z) {
		return (int) Math.round((wx - cx) * z + mapW / 2.0);
	}

	private int sy(double wz, double z) {
		return (int) Math.round((wz - cz) * z + mapH / 2.0);
	}

	private void frame(GuiGraphics g, int x0, int y0, int x1, int y1, int col) {
		int t = Math.max(1, s);
		g.fill(x0, y0, x1, y0 + t, col);
		g.fill(x0, y1 - t, x1, y1, col);
		g.fill(x0, y0, x0 + t, y1, col);
		g.fill(x1 - t, y0, x1, y1, col);
	}

	/** North, and how far 100 blocks (metres) is, at the bottom right; the zoom on the right. */
	private void drawCompass(GuiGraphics g, double z) {
		int r = 9 * s, cxp = width - r - 8 * s, cyp = 8 * s + r + 18 * s;
		Device.disc(g, cxp, cyp + s, r, r, 0x50000000);
		Device.disc(g, cxp, cyp, r, r, 0xF0123C38);
		for (int i = 0; i < r - 2 * s; i++) g.fill(cxp - Math.max(1, (r - 2 * s - i) / 3), cyp - i, cxp + Math.max(1, (r - 2 * s - i) / 3), cyp - i + 1, 0xFFE5525C);
		Ui.centred(g, font, "N", cxp, cyp - r - 10 * s, 0xFFFFFFFF, s, true);
		// The scale: a bar for a round distance.
		int[] steps = {10, 25, 50, 100, 250, 500, 1000};
		int len = 10;
		for (int st : steps) {
			len = st;
			if (st * z >= 40 * s) break;
		}
		int bw = (int) Math.round(len * z), bx = width - bw - 10 * s, by = height - 24 * s;
		g.fill(bx, by, bx + bw, by + 3 * s, 0xFFFFFFFF);
		g.fill(bx, by - 3 * s, bx + s, by + 3 * s, 0xFFFFFFFF);
		g.fill(bx + bw - s, by - 3 * s, bx + bw, by + 3 * s, 0xFFFFFFFF);
		Ui.right(g, font, len + " m", bx + bw, by - 12 * s, 0xFFFFFFFF, s);
		// The zoom, with + and - to click.
		int zx = width - 18 * s, zy = cyp + r + 12 * s, zh = Math.max(30 * s, height - zy - 46 * s);
		Ui.round(g, zx, zy, 12 * s, zh, 0xD0123C38, 4 * s);
		double f = (Math.log(z) - Math.log(fit())) / Math.max(0.01, Math.log(MAX_ZOOM) - Math.log(fit()));
		int ky = zy + zh - 14 * s - (int) ((zh - 28 * s) * Math.max(0, Math.min(1, f)));
		Ui.round(g, zx + 2 * s, ky + 4 * s, 8 * s, 6 * s, Ui.CREAM, 2 * s);
		Ui.centred(g, font, "+", zx + 6 * s, zy + 2 * s, 0xFFFFFFFF, s, false);
		Ui.centred(g, font, "-", zx + 6 * s, zy + zh - 11 * s, 0xFFFFFFFF, s, false);
		hit(zx, zy, 12 * s, 12 * s, () -> zoomBy(1.5, mapW / 2.0, mapH / 2.0));
		hit(zx, zy + zh - 12 * s, 12 * s, 12 * s, () -> zoomBy(1 / 1.5, mapW / 2.0, mapH / 2.0));
	}

	/** The picked place: its picture, its name, and FLY HERE for a town you've been to. */
	private void drawCard(GuiGraphics g) {
		if (picked == null) return;
		Places.Place pl = Places.get(picked);
		String name = pl == null ? picked.toUpperCase().replace('_', ' ') : pl.name();
		Point pt = points.get(placeToPoint.getOrDefault(picked, ""));
		boolean town = pt != null && pt.kind().equals("town"), been = ClientState.data.visited.contains(picked);
		int cw = Math.min(150 * s, width / 2), pich = cw * 9 / 16, ch = pich + 52 * s;
		int x = 6 * s, y = height - ch - 16 * s;
		Device.rounded(g, x + 2 * s, y + 3 * s, cw, ch, 6 * s, 0x50000000);
		Device.rounded(g, x, y, cw, ch, 6 * s, 0xF8FBF5E6);
		Identifier pic = Postcards.picture(picked);
		int px = x + 4 * s, py = y + 4 * s, pw = cw - 8 * s;
		if (pic != null) g.blit(RenderPipelines.GUI_TEXTURED, pic, px, py, 0, 0, pw, pich - 4 * s, 384, 216, 384, 216);
		else {
			g.fill(px, py, px + pw, py + pich - 4 * s, 0xFF2A3350);
			Ui.centred(g, font, been ? "No picture yet" : "Not been here yet", px + pw / 2, py + (pich - 4 * s) / 2 - 4 * s, Ui.MUTED, s, false);
		}
		int ty = y + pich + 2 * s;
		Ui.draw(g, font, Ui.bold(name), x + 8 * s, ty, Ui.CREAM_INK, s, false);
		Ui.draw(g, font, Ui.ui((town ? "Town" : pt != null && pt.kind().equals("route") ? "Route" : "Place") + (been ? "  ·  Visited" : "")), x + 8 * s,
				ty + 11 * s, 0xFF7A849E, s, false);
		if (picked.equals(ClientState.data.place)) Ui.draw(g, font, Ui.ui("You are here."), x + 8 * s, ty + 22 * s, 0xFFE5394A, s, false);
		if (town) {
			boolean ok = flyable(picked) && canFly();
			int bw = 62 * s, bx = x + cw - bw - 6 * s, by = y + ch - 18 * s;
			Ui.button(g, font, bx, by, bw, 13 * s, "FLY HERE", ok ? 0xFF4A8FE0 : 0xFF8A90A0, ok && over(bx, by, bw, 13 * s), s);
			String place = picked;
			if (ok) hit(bx, by, bw, 13 * s, () -> flyTo(place));
		}
	}

	/** The games' own map of Kanto or Johto: every place, joined by its routes. */
	private void drawRegion(GuiGraphics g, float partial) {
		g.fill(0, 0, width, height, 0xFF0E2240);
		int m = 8 * s;
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
		int panelW = 110 * s;
		int areaX = m + 6 * s, areaY = m + 22 * s, areaW = width - 2 * m - panelW - 18 * s, areaH = height - areaY - m - 10 * s;
		float k = Math.min(areaW / (float) Math.max(1, maxX - minX), areaH / (float) Math.max(1, maxY - minY));
		int fMinX = minX, fMinY = minY;
		int offX = areaX + (int) ((areaW - (maxX - minX) * k) / 2), offY = areaY + (int) ((areaH - (maxY - minY) * k) / 2);
		Map<String, int[]> at2 = new HashMap<>();
		for (Point p : pts) at2.put(p.key(), new int[] {offX + Math.round((p.x() - fMinX) * k), offY + Math.round((p.y() - fMinY) * k)});
		for (String[] e : edges) {
			int[] a = at2.get(e[0]), b = at2.get(e[1]);
			if (a == null || b == null) continue;
			line(g, a[0], a[1], b[0], b[1], 12 * s, 0xFF3E8C4A);
		}
		for (Point p : pts) {
			int[] c = at2.get(p.key());
			Ui.round(g, c[0] - 7 * s, c[1] - 7 * s, 14 * s, 14 * s, 0xFF3E8C4A, 6 * s);
		}
		for (String[] e : edges) {
			int[] a = at2.get(e[0]), b = at2.get(e[1]);
			if (a == null || b == null) continue;
			line(g, a[0], a[1], b[0], b[1], 3 * s, 0xFFE9D8A6);
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
					int z = 6 * s;
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
		// The panel: the picked place.
		int px = width - m - panelW, py = m + 22 * s, ph = 92 * s;
		Ui.round(g, px, py, panelW, ph, 0xF0123C38, 3 * s);
		Point sel = points.get(at);
		if (sel != null) {
			List<String> nm = Ui.wrap(font, sel.name(), (panelW - 16 * s) / s);
			for (int i = 0; i < nm.size() && i < 2; i++) Ui.draw(g, font, Ui.bold(nm.get(i)), px + 8 * s, py + 8 * s + i * 11 * s, Ui.TEXT, s, true);
			String kind = sel.kind().equals("town") ? "Town" : sel.kind().equals("route") ? "Route" : "Place";
			Ui.text(g, font, kind + (visited(sel.key()) ? "  ·  Visited" : "  ·  Not yet visited"), px + 8 * s, py + 32 * s, Ui.MUTED, s);
			if (sel.key().equals(here)) Ui.text(g, font, "You are here.", px + 8 * s, py + 44 * s, 0xFFFF6B76, s);
			if (sel.kind().equals("town")) {
				boolean ok = flyPlace(sel.key()) != null && canFly();
				int bw = panelW - 16 * s, bx = px + 8 * s, by = py + ph - 20 * s;
				Ui.button(g, font, bx, by, bw, 13 * s, "FLY HERE", ok ? 0xFF4A8FE0 : 0xFF4A5068, ok && over(bx, by, bw, 13 * s), s);
				if (ok) hit(bx, by, bw, 13 * s, this::fly);
				else Ui.text(g, font, canFly() ? "Visit it first." : "Needs FLY + THUNDERBADGE.", px + 8 * s, py + ph - 32 * s, Ui.DIM, s);
			}
		}
		Ui.text(g, font, "Arrows: move   Tab: maps   A: fly   B: back", m, height - m - 4 * s, Ui.DIM, s);
	}
}
