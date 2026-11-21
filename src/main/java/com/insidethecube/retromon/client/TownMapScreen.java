package com.insidethecube.retromon.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
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
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * The Town Map, as the UI Viewer's (signed off 20 Nov): Kanto, and Johto beside it (the arrow at the side, or left
 * past the westmost place), as they are in your world, drawn from above in soft colours from the blocks themselves
 * (TownMap). Every town, route and place is a square with its name beside it, where the viewer has it
 * (data/map_pins.tsv, from tools/uiviewer/*_points.tsv); the one you're on is framed in a bright box. Its panel says
 * what's there, its gym, how its POKéMON are found and shows them, and FLY HERE for a town you've been to. + and -
 * (or Q, E and the wheel) zoom, drag to look around, C comes back to you, TAB goes town to town. The Pokédex can open
 * it with a POKéMON's places marked.
 */
public class TownMapScreen extends DeviceScreen {
	/** A place on the map: where it is (block x, z), what kind (town, route, spot), its name, its zone if known, how its POKéMON are found, which, what else is there. */
	public record Pin(String region, String key, int x, int z, String kind, String name, int[] zone, List<String> how, List<String> wild, List<String> here) {}

	private static List<Pin> pins;
	private static Map<String, String> placeToPoint;

	/** Each region's box (blocks: x0, z0, x1, z1), as the viewer's (tools/uiviewer.py MAP_BOX, JOHTO_BOX). */
	private static final int[] KANTO_BOX = {-1250, 120, -46, 1000}, JOHTO_BOX = {-2640, 140, -1150, 1000};
	/** West of this you're in Johto. */
	private static final int JOHTO_X = -1180;
	/** The map's window on the canvas. */
	private static final float MX = 20, MY = 20, VW = 900, VH = 680;

	private final Screen parent;
	private String region = "kanto";
	private List<Pin> P = new ArrayList<>();
	private int[] box = KANTO_BOX;
	private int mw, mh;
	private double[] zooms;
	private int zi, sel;
	private double cx, cz, tx, tz;
	private long lastNanos;
	/** A POKéMON's places, from the Pokédex ("Where X lives"). */
	private String highlightTitle;
	private final Set<String> highlight = new HashSet<>();
	/** Flipping between regions: when, and which way the map slides in (-1 from the left, 1 from the right). */
	private float flipAt = -10, slide;
	// Dragging the map.
	private boolean dragging, dragMoved;
	private double dragX, dragY, dragTx, dragTz;
	private boolean overUi;

	public TownMapScreen(Screen parent) {
		super("Town Map");
		this.parent = parent;
		load();
		var me = net.minecraft.client.Minecraft.getInstance().player;
		String reg = me != null && me.getX() < JOHTO_X ? "johto" : "kanto";
		setRegion(reg);
		int at = indexOf(mine());
		pick(Math.max(0, at), false);
	}

	private static void load() {
		if (pins != null) return;
		pins = new ArrayList<>();
		for (String[] r : Species.table("map_pins.tsv")) {
			if (r.length < 10) continue;
			int[] zone = null;
			if (!r[6].equals("-")) zone = Arrays.stream(r[6].split(",")).mapToInt(v -> Integer.parseInt(v.trim())).toArray();
			pins.add(new Pin(r[0], r[1], Integer.parseInt(r[2].trim()), Integer.parseInt(r[3].trim()), r[4], r[5], zone, list(r[7], "\\|"), list(r[8], ","), list(r[9], "\\|")));
		}
		placeToPoint = new HashMap<>();
		for (String[] r : Species.table("place_landmarks.tsv")) placeToPoint.put(r[0], r[1]);
	}

	private static List<String> list(String s, String sep) {
		if (s == null || s.isEmpty() || s.equals("-")) return List.of();
		return List.of(s.split(sep));
	}

	private void setRegion(String reg) {
		region = reg;
		box = reg.equals("johto") ? JOHTO_BOX : KANTO_BOX;
		mw = box[2] - box[0];
		mh = box[3] - box[1];
		P = new ArrayList<>();
		for (Pin p : pins) if (p.region().equals(reg)) P.add(p);
		double fit = Math.min(VW / mw, VH / mh);
		zooms = new double[] {fit, 1.5, 3, 6};
		zi = 0;
		cx = tx = box[0] + mw / 2.0;
		cz = tz = box[1] + mh / 2.0;
		sel = 0;
	}

	/** The pin a place belongs to: its own, one it's a part of (safari_zone_east, safari_zone), or its landmark's. */
	private String claim(String place) {
		if (place == null || place.isEmpty()) return null;
		for (Pin p : pins) if (p.key().equals(place)) return place;
		String best = null;
		for (Pin p : pins) if (place.startsWith(p.key() + "_") && (best == null || p.key().length() > best.length())) best = p.key();
		if (best != null) return best;
		String lm = placeToPoint.getOrDefault(place, place);
		for (Pin p : pins) if (p.key().equals(lm)) return lm;
		return null;
	}

	/** Where you are (its pin), or null. */
	private String mine() {
		return claim(ClientState.data.place);
	}

	private int indexOf(String key) {
		if (key == null) return -1;
		for (int i = 0; i < P.size(); i++) if (P.get(i).key().equals(key)) return i;
		return -1;
	}

	/** Marks a POKéMON's places (from the Pokédex). */
	public void highlight(String title, Set<String> places) {
		highlightTitle = title;
		for (String pl : places) {
			String k = claim(pl);
			if (k != null) highlight.add(k);
		}
		for (Pin p : pins) {
			if (!highlight.contains(p.key())) continue;
			if (!p.region().equals(region)) setRegion(p.region());
			pick(indexOf(p.key()), false);
			break;
		}
	}

	// ---------- where you've been, and flying ----------

	private boolean been(String key) {
		PlayerData d = ClientState.data;
		if (d.visited.contains(key)) return true;
		for (String pl : d.visited) if (key.equals(claim(pl))) return true;
		return false;
	}

	/** The place to fly to for a pin: a place you've visited that's in this town. */
	private String flyPlace(String key) {
		PlayerData d = ClientState.data;
		if (d.visited.contains(key)) return key;
		for (String pl : d.visited) if (key.equals(claim(pl)) && !pl.contains("_gym") && !pl.contains("house")) return pl;
		return null;
	}

	private boolean canFly() {
		PlayerData d = ClientState.data;
		return d.badges.contains("THUNDERBADGE") && d.party.stream().anyMatch(m -> m.moves.contains("fly") && !m.fainted());
	}

	/** FAST TRAVEL (on in OPTIONS): straight to a point in the world (a pin's, or one picked on the map). */
	private void travel(double x, double z) {
		if (!Settings.fastTravel() || minecraft == null) return;
		Sounds.press();
		CompoundTag t = new CompoundTag();
		t.putInt("x", (int) Math.floor(x));
		t.putInt("z", (int) Math.floor(z));
		ClientPlayNetworking.send(new Net.Act("travel", t));
		minecraft.setScreen(null);
	}

	private void fly() {
		if (P.isEmpty()) return;
		Pin p = P.get(sel);
		if (Settings.fastTravel()) {
			travel(p.x() + 0.5, p.z() + 0.5);
			return;
		}
		String place = p.kind().equals("town") ? flyPlace(p.key()) : null;
		if (place == null || !canFly() || minecraft == null) return;
		Sounds.press();
		CompoundTag t = new CompoundTag();
		t.putString("place", place);
		ClientPlayNetworking.send(new Net.Act("fly", t));
		minecraft.setScreen(null);
	}

	// ---------- the view ----------

	private double zoom() {
		return zooms[zi];
	}

	private float sx(double x) {
		return (float) ((x - cx) * zoom() + VW / 2 + MX);
	}

	private float sy(double z) {
		return (float) ((z - cz) * zoom() + VH / 2 + MY);
	}

	private void clampC() {
		double hw = VW / 2 / zoom(), hh = VH / 2 / zoom();
		tx = mw * zoom() <= VW ? box[0] + mw / 2.0 : Math.max(box[0] + hw, Math.min(box[2] - hw, tx));
		tz = mh * zoom() <= VH ? box[1] + mh / 2.0 : Math.max(box[1] + hh, Math.min(box[3] - hh, tz));
	}

	/** Zoomed in, the map follows the place you're on when it nears the edge. */
	private void follow() {
		if (P.isEmpty()) return;
		Pin p = P.get(sel);
		double x = (p.x() - tx) * zoom() + VW / 2, y = (p.z() - tz) * zoom() + VH / 2;
		if (zi > 0 && (x < 120 || x > VW - 120 || y < 90 || y > VH - 90)) {
			tx = p.x();
			tz = p.z();
			clampC();
		}
	}

	private void setZoom(int n, Double ax, Double az) {
		n = Math.max(0, Math.min(zooms.length - 1, n));
		if (n == zi) return;
		zi = n;
		if (ax != null) {
			tx = ax;
			tz = az;
		} else if (!P.isEmpty()) {
			tx = P.get(sel).x();
			tz = P.get(sel).z();
		}
		clampC();
		cx = tx;
		cz = tz;
		Sounds.select();
	}

	private void pick(int i, boolean sound) {
		if (i < 0 || i >= P.size()) return;
		if (sound && i != sel) Sounds.select();
		sel = i;
		follow();
	}

	/** The nearest place in a direction (the viewer's near()). */
	private int near(int dx, int dz) {
		if (P.isEmpty()) return sel;
		Pin p = P.get(sel);
		int best = sel;
		double bd = 1e9;
		for (int i = 0; i < P.size(); i++) {
			if (i == sel) continue;
			Pin q = P.get(i);
			double ddx = q.x() - p.x(), ddz = q.z() - p.z(), along = ddx * dx + ddz * dz;
			if (along <= 4) continue;
			double d = along + Math.abs(ddx * dz - ddz * dx) * 2.2;
			if (d < bd) {
				bd = d;
				best = i;
			}
		}
		return best;
	}

	private void flip() {
		String to = region.equals("kanto") ? "johto" : "kanto";
		slide = region.equals("kanto") ? -1 : 1;
		flipAt = (ticks + 0F) / 20F;
		setRegion(to);
		int at = indexOf(mine());
		pick(Math.max(0, at), false);
		Sounds.press();
	}

	private boolean youHere() {
		var me = minecraft == null ? null : minecraft.player;
		return me != null && me.getX() >= box[0] && me.getX() <= box[2] && me.getZ() >= box[1] && me.getZ() <= box[3];
	}

	private void toYou() {
		if (!youHere()) {
			flip();
			return;
		}
		var me = minecraft.player;
		int at = indexOf(mine());
		if (at >= 0) pick(at, false);
		int n = Math.max(zi, 2);
		if (n == zi) {
			tx = me.getX();
			tz = me.getZ();
			clampC();
		} else setZoom(n, me.getX(), me.getZ());
		Sounds.press();
	}

	// ---------- keys and the mouse ----------

	@Override
	protected boolean key(KeyEvent e) {
		int k = e.key();
		if (Ui.isB(e)) {
			back();
			return true;
		}
		if (k == GLFW.GLFW_KEY_EQUAL || k == GLFW.GLFW_KEY_KP_ADD || k == GLFW.GLFW_KEY_E) setZoom(zi + 1, null, null);
		else if (k == GLFW.GLFW_KEY_MINUS || k == GLFW.GLFW_KEY_KP_SUBTRACT || k == GLFW.GLFW_KEY_Q) setZoom(zi - 1, null, null);
		else if (k == GLFW.GLFW_KEY_C) toYou();
		else if (k == GLFW.GLFW_KEY_TAB) {
			int i = sel;
			for (int n = 0; n < P.size(); n++) {
				i = (i + 1) % P.size();
				if (P.get(i).kind().equals("town")) break;
			}
			pick(i, true);
		} else if (Ui.isA(e)) fly();
		else if (Ui.isLeft(e) || Ui.isRight(e) || Ui.isUp(e) || Ui.isDown(e)) {
			int dx = Ui.isLeft(e) ? -1 : Ui.isRight(e) ? 1 : 0, dz = Ui.isUp(e) ? -1 : Ui.isDown(e) ? 1 : 0;
			int n = near(dx, dz);
			if (n == sel && ((dx < 0 && region.equals("kanto")) || (dx > 0 && region.equals("johto")))) flip();
			else pick(n, true);
		}
		return true;
	}

	@Override
	protected void pressHint(String key) {
		switch (key) {
			case "+" -> setZoom(zi + 1, null, null);
			case "-" -> setZoom(zi - 1, null, null);
			case "C" -> toYou();
			default -> super.pressHint(key);
		}
	}

	private boolean inMap(double gx, double gy) {
		float vx = V.vx(gx), vy = V.vy(gy);
		return vx >= MX && vy >= MY && vx < MX + VW && vy < MY + VH;
	}

	/** The place nearest the mouse (within a finger's width), or -1. */
	private int nearest(double gx, double gy) {
		float vx = V.vx(gx), vy = V.vy(gy);
		int best = -1;
		double bd = 24;
		for (int i = 0; i < P.size(); i++) {
			Pin p = P.get(i);
			double d = Math.hypot(sx(p.x()) - vx, sy(p.z()) - vy);
			if (d < bd) {
				bd = d;
				best = i;
			}
		}
		return best;
	}

	@Override
	public boolean mouseScrolled(double x, double y, double h, double v) {
		if (!inMap(x, y) || v == 0) return true;
		float vx = V.vx(x) - MX, vy = V.vy(y) - MY;
		double wx = cx + (vx - VW / 2) / zoom(), wz = cz + (vy - VH / 2) / zoom();
		setZoom(zi + (v > 0 ? 1 : -1), wx, wz);
		return true;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		// FAST TRAVEL: right-click any spot on the map to go there.
		if (event.button() == 1 && Settings.fastTravel() && inMap(event.x(), event.y())) {
			float vx = V.vx(event.x()) - MX, vy = V.vy(event.y()) - MY;
			travel(cx + (vx - VW / 2) / zoom(), cz + (vy - VH / 2) / zoom());
			return true;
		}
		if (event.button() == 0 && !overUi && inMap(event.x(), event.y())) {
			dragging = true;
			dragMoved = false;
			dragX = event.x();
			dragY = event.y();
			dragTx = tx;
			dragTz = tz;
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (dragging) {
			double ddx = (event.x() - dragX) / V.s(1), ddy = (event.y() - dragY) / V.s(1);
			if (Math.hypot(ddx, ddy) > 4) dragMoved = true;
			tx = dragTx - ddx / zoom();
			tz = dragTz - ddy / zoom();
			clampC();
			cx = tx;
			cz = tz;
			return true;
		}
		return super.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (dragging) {
			dragging = false;
			if (!dragMoved) pick(nearest(event.x(), event.y()), true);
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	protected void back() {
		Sounds.press();
		to(parent);
	}

	// ---------- drawing ----------

	/** A quarter (or any part) of a ring, a0 to a1 (radians), from radius r0 out to r1, in GUI pixels. */
	private static void arc(GuiGraphics g, float cx, float cy, float r0, float r1, double a0, double a1, int col) {
		int n = 10;
		float[] xy = new float[n * 12];
		int[] cs = new int[n * 6];
		for (int i = 0; i < n; i++) {
			double b0 = a0 + (a1 - a0) * i / n, b1 = a0 + (a1 - a0) * (i + 1) / n;
			float c0 = (float) Math.cos(b0), s0 = (float) Math.sin(b0), c1 = (float) Math.cos(b1), s1 = (float) Math.sin(b1);
			float[] q = {cx + c0 * r0, cy + s0 * r0, cx + c0 * r1, cy + s0 * r1, cx + c1 * r1, cy + s1 * r1, cx + c0 * r0, cy + s0 * r0, cx + c1 * r1, cy + s1 * r1, cx + c1 * r0,
					cy + s1 * r0};
			System.arraycopy(q, 0, xy, i * 12, 12);
		}
		Arrays.fill(cs, col);
		Ui.tris(g, xy, cs);
	}

	/** Rounds a box's corners off (canvas), painting outside radius r in each corner's colour (tl, tr, br, bl), smoothly. */
	private static void corners(GuiGraphics g, float x, float y, float w, float h, float r, float spread, int[] cols) {
		Ui.cornersOut(g, V.x(x), V.y(y), V.s(w), V.s(h), V.s(r), cols);
	}

	/** A rounded square with its edge drawn over the line (the viewer's fill then stroke). */
	private static void sq(GuiGraphics g, float x, float y, float r, int fill, int edge, float lw) {
		float rad = Math.max(3, r * 0.42F);
		V.rr(g, x - r - lw / 2, y - r - lw / 2, 2 * r + lw, 2 * r + lw, rad + lw / 2, edge);
		V.rr(g, x - r + lw / 2, y - r + lw / 2, 2 * r - lw, 2 * r - lw, Math.max(0, rad - lw / 2), fill);
	}

	/** A ring centred on a circle's line (radius r, lw wide). */
	private static void circle(GuiGraphics g, float x, float y, float r, float lw, int col) {
		float o = r + lw / 2;
		V.ring(g, x - o, y - o, 2 * o, 2 * o, o, lw, col);
	}

	/** A rounded box's edge, centred on its line. */
	private static void stroke(GuiGraphics g, float x, float y, float w, float h, float r, float lw, int col) {
		V.ring(g, x - lw / 2, y - lw / 2, w + lw, h + lw, r + lw / 2, lw, col);
	}

	/** The baked map (TownMap's) of a box of the world into a box on the screen (GUI pixels), k GUI pixels a block. */
	private void world(GuiGraphics g, double wx0, double wz0, double k, int clipX0, int clipY0, int clipX1, int clipY1) {
		if (k < 0.6) {
			int[] a = TownMap.all;
			if (TownMap.overW() <= 0) return;
			int x0 = (int) Math.round((a[0] - wx0) * k) + clipX0, y0 = (int) Math.round((a[1] - wz0) * k) + clipY0;
			int x1 = (int) Math.round((a[0] + TownMap.overW() * TownMap.OVER - wx0) * k) + clipX0, y1 = (int) Math.round((a[1] + TownMap.overH() * TownMap.OVER - wz0) * k) + clipY0;
			Ui.blitSmooth(g, TownMap.OVERVIEW, x0, y0, x1, y1, 0, 1, 0, 1, 0xFFFFFFFF);
			return;
		}
		for (TownMap.Region r : TownMap.regions()) {
			Identifier tex = TownMap.texture(r);
			if (tex == null) continue;
			int x0 = (int) Math.round((r.rx * TownMap.R - wx0) * k) + clipX0, y0 = (int) Math.round((r.rz * TownMap.R - wz0) * k) + clipY0;
			int x1 = (int) Math.round(((r.rx + 1) * TownMap.R - wx0) * k) + clipX0, y1 = (int) Math.round(((r.rz + 1) * TownMap.R - wz0) * k) + clipY0;
			if (x1 < clipX0 || y1 < clipY0 || x0 > clipX1 || y0 > clipY1) continue;
			// Smoothed only when it's drawn smaller than a pixel a block; at a pixel a block or more, sharp (it blurred).
			if (k < 0.95) Ui.blitSmooth(g, tex, x0, y0, x1, y1, 0, 1, 0, 1, 0xFFFFFFFF);
			else g.blit(RenderPipelines.GUI_TEXTURED, tex, x0, y0, 0, 0, x1 - x0, y1 - y0, TownMap.R, TownMap.R, TownMap.R, TownMap.R);
		}
	}

	/**
	 * The same, cut to a box by hand (for the panel's picture, which is turned a little, so it can't be cut by the
	 * screen's scissor): only the part of each region inside the box is drawn. Box and k in GUI pixels.
	 */
	private void worldCut(GuiGraphics g, double wx0, double wz0, double bw, double bh, float k, float x, float y) {
		double wx1 = wx0 + bw / k, wz1 = wz0 + bh / k;
		for (TownMap.Region r : TownMap.regions()) {
			Identifier tex = TownMap.texture(r);
			if (tex == null) continue;
			int rx0 = r.rx * TownMap.R, rz0 = r.rz * TownMap.R;
			int ax = (int) Math.max(rx0, Math.floor(wx0)), az = (int) Math.max(rz0, Math.floor(wz0));
			int bx = (int) Math.min(rx0 + TownMap.R, Math.ceil(wx1)), bz = (int) Math.min(rz0 + TownMap.R, Math.ceil(wz1));
			if (bx <= ax || bz <= az) continue;
			// Inside the box only (whole blocks: the last may poke a little past its edge, so trim to it).
			float px0 = (float) Math.max(0, (ax - wx0) * k), pz0 = (float) Math.max(0, (az - wz0) * k);
			float px1 = (float) Math.min(bw, (bx - wx0) * k), pz1 = (float) Math.min(bh, (bz - wz0) * k);
			if (px1 <= px0 || pz1 <= pz0) continue;
			float u = (float) (wx0 + px0 / k - rx0), v = (float) (wz0 + pz0 / k - rz0);
			float uw = (px1 - px0) / k, vh = (pz1 - pz0) / k;
			g.pose().pushMatrix();
			g.pose().translate(x + px0, y + pz0);
			float sw = px1 - px0, sh = pz1 - pz0;
			int iw = Math.max(1, Math.round(uw)), ih = Math.max(1, Math.round(vh));
			g.pose().scale(sw / iw, sh / ih);
			g.blit(RenderPipelines.GUI_TEXTURED, tex, 0, 0, u, v, iw, ih, iw, ih, TownMap.R, TownMap.R);
			g.pose().popMatrix();
		}
	}

	@Override
	protected void drawScreen(GuiGraphics g, float partial) {
		long now = System.nanoTime();
		float dt = lastNanos == 0 ? 0 : Math.min(0.1F, (now - lastNanos) / 1e9F);
		lastNanos = now;
		float secs = (ticks + partial) / 20F;
		double ease = 1 - Math.pow(0.8, dt * 60);
		cx += (tx - cx) * ease;
		cz += (tz - cz) * ease;
		g.fill(0, 0, width, height, 0xFF348CCE);
		overUi = false;
		// The map, sliding in when you've just turned to the other region.
		float since = secs - flipAt, slideX = 0, cover = 0;
		if (since < 0.38F) {
			float q = since / 0.38F;
			q = 1 - (1 - q) * (1 - q) * (1 - q);
			slideX = slide * 60 * (1 - q);
			cover = 1 - q;
		}
		g.pose().pushMatrix();
		g.pose().translate(V.s(slideX), 0);
		drawMap(g, secs);
		g.pose().popMatrix();
		if (cover > 0) g.fill(Math.round(V.x(MX - 8)), Math.round(V.y(MY - 8)), Math.round(V.x(MX + VW + 8)), Math.round(V.y(MY + VH + 12)), Ui.alpha(0xFF348CCE, cover));
		drawInfo(g);
		drawSide(g, secs);
		drawBack(g);
		V.hints(g, "+", "Zoom in", "-", "Zoom out", "C", "You", "B", "Back");
	}

	/** The map's window: the world, the zones, the place you're on framed, every place's square and name, you, the scale. */
	private void drawMap(GuiGraphics g, float secs) {
		PlayerData d = ClientState.data;
		V.shadow(g, MX, MY, VW, VH, 22, 1);
		V.holo(g, MX, MY, VW, VH, 22);
		int c0 = Math.round(V.x(MX + 3)), r0 = Math.round(V.y(MY + 3)), c1 = Math.round(V.x(MX + VW - 3)), r1 = Math.round(V.y(MY + VH - 3));
		g.enableScissor(c0, r0, c1, r1);
		g.fill(c0, r0, c1, r1, TownMap.SEA);
		double z = zoom();
		if (!TownMap.ready()) {
			V.centred(g, Lang.t("Drawing the map..."), MX + VW / 2, MY + VH / 2 - 10, 21, 0xFFFFFFFF, true);
		} else {
			TownMap.upload();
			double k = z * V.s(1);
			// The box's top left on the screen: where world x, z of (cx - VW/2/z, cz - VH/2/z) lands.
			int ox = Math.round(V.x(MX)), oy = Math.round(V.y(MY));
			world(g, cx - VW / 2 / z, cz - VH / 2 / z, k, ox, oy, Math.round(V.x(MX + VW)), Math.round(V.y(MY + VH)));
		}
		// The zones the mod knows, faintly, once you're in close.
		if (zi >= 1) for (Pin p : P) {
			int[] zn = zoneOf(p);
			if (zn == null) continue;
			dashed(g, sx(zn[0]), sy(zn[1]), sx(zn[2]) - sx(zn[0]), sy(zn[3]) - sy(zn[1]), 0x8CFFF4D8);
		}
		float pulse = (float) (Math.sin(secs * 1000 / 260) + 1) / 2;
		// The place you're on, framed as the old Town Map frames it: a bright box round its area, the land inside lit a
		// little, the rest of the map dimmed a touch.
		if (!P.isEmpty()) {
			Pin p = P.get(sel);
			int[] zn = zoneOf(p);
			float a1, b1, a2, b2;
			if (zn != null) {
				a1 = sx(zn[0]);
				b1 = sy(zn[1]);
				a2 = sx(zn[2]);
				b2 = sy(zn[3]);
			} else {
				int hw = p.kind().equals("town") ? 34 : p.kind().equals("spot") ? 20 : 26;
				a1 = sx(p.x() - hw);
				b1 = sy(p.z() - hw);
				a2 = sx(p.x() + hw);
				b2 = sy(p.z() + hw);
			}
			float pad = 4, x = Math.min(a1, a2) - pad, y = Math.min(b1, b2) - pad, w = Math.abs(a2 - a1) + pad * 2, hh = Math.abs(b2 - b1) + pad * 2;
			int dim = 0x2E101E3C;
			V.rr(g, MX, MY, VW, Math.max(0, y - MY), 0, dim);
			V.rr(g, MX, y + hh, VW, Math.max(0, MY + VH - y - hh), 0, dim);
			V.rr(g, MX, y, Math.max(0, x - MX), hh, 0, dim);
			V.rr(g, x + w, y, Math.max(0, MX + VW - x - w), hh, 0, dim);
			V.rr(g, x, y, w, hh, 8, 0x24FFF8D6);
			stroke(g, x, y, w, hh, 8, 6, Ui.alpha(0xFFFFD45A, 0.35F + pulse * 0.3F));
			stroke(g, x, y, w, hh, 8, 2.5F, 0xFFFFFFFF);
		}
		// Every place is a square: towns big and filled (red once you've been, grey before), routes and other places
		// smaller, white with a red edge (purple for a place). The one you're on is yellow, with a pulse round it.
		record Spot(Pin p, int i, float sx, float sy, float r, boolean on, boolean town) {}
		List<Spot> spots = new ArrayList<>();
		for (int i = 0; i < P.size(); i++) {
			Pin p = P.get(i);
			float x = sx(p.x()), y = sy(p.z());
			if (x < MX - 40 || y < MY - 40 || x > MX + VW + 40 || y > MY + VH + 40) continue;
			boolean on = i == sel, town = p.kind().equals("town"), seen = been(p.key());
			// (Smaller zoomed right out, so they don't crowd the map; bigger as you zoom in.)
			float r = (town ? new float[] {6.5F, 8.5F, 10, 12}[Math.min(3, zi)] : new float[] {4.5F, 6, 7.5F, 9.5F}[Math.min(3, zi)]) + (on ? 1.5F : 0);
			if (highlight.contains(p.key())) {
				V.glow(g, x, y, r + 16, r + 16, Ui.alpha(0xFFFFD34A, 0.55F + 0.3F * pulse));
				circle(g, x, y, r + 6, 2.5F, Ui.alpha(0xFFFFD34A, 0.8F));
			}
			if (on) circle(g, x, y, r + 9 + pulse * 5, 3, Ui.alpha(0xFFFFD45A, 0.9F - pulse * 0.5F));
			if (town) sq(g, x, y, r, on ? 0xFFFFD45A : seen ? 0xFFE5525C : 0xFFB9C4C0, 0xFFFFFFFF, 3);
			else sq(g, x, y, r, on ? 0xFFFFD45A : 0xFFFFFFFF, p.kind().equals("spot") ? 0xFF8F60DE : 0xFFE5525C, 2.5F);
			if (town) V.rr(g, x - r * 0.42F, y - r * 0.42F, r * 0.84F, r * 0.84F, 2, 0xD9FFFFFF);
			spots.add(new Spot(p, i, x, y, r, on, town));
		}
		// Labels: towns and the one you're on first, then routes and places, each left out only where it would cover another.
		List<float[]> taken = new ArrayList<>();
		spots.sort((a, b) -> a.on() != b.on() ? (a.on() ? -1 : 1) : a.town() != b.town() ? (a.town() ? -1 : 1) : 0);
		for (Spot q : spots) {
			boolean big = q.town() || q.on();
			float size = (big ? 13 : 10.5F) * (zi == 0 ? 0.88F : 1), w = V.width(q.p().name(), size, true) + 12, hh = (big ? 21 : 16) * (zi == 0 ? 0.88F : 1), x = q.sx() - w / 2, y = q.sy() + q.r() + 3;
			boolean clash = false;
			for (float[] b : taken) if (x < b[0] + b[2] && x + w > b[0] && y < b[1] + b[3] && y + hh > b[1]) clash = true;
			if (clash && !q.on()) continue;
			taken.add(new float[] {x, y, w, hh});
			V.pill(g, x - 0.75F, y - 0.75F, w + 1.5F, hh + 1.5F, 0x2E1E2854);
			V.pill(g, x + 0.75F, y + 0.75F, w - 1.5F, hh - 1.5F, 0xF2FFFFFF);
			V.centred(g, q.p().name(), q.sx(), V.capsTop(y + hh / 2 + 0.5F, size), size, V.INK, true);
		}
		// You: the arrow, the way you face.
		var me = minecraft == null ? null : minecraft.player;
		if (me != null && youHere()) {
			float x = sx(me.getX()), y = sy(me.getZ());
			double a = Math.toRadians(me.getYRot() + 180);
			float ca = (float) Math.cos(a), sa = (float) Math.sin(a);
			float[] pts = {0, -11, 8, 8, 0, 4, -8, 8};
			float[] q = new float[8];
			for (int i = 0; i < 4; i++) {
				q[i * 2] = x + pts[i * 2] * ca - pts[i * 2 + 1] * sa;
				q[i * 2 + 1] = y + pts[i * 2] * sa + pts[i * 2 + 1] * ca;
			}
			V.poly(g, new float[] {q[0], q[1], q[2], q[3], q[4], q[5]}, 0xFFFFD45A);
			V.poly(g, new float[] {q[0], q[1], q[4], q[5], q[6], q[7]}, 0xFFFFD45A);
			for (int i = 0; i < 4; i++) {
				int j = (i + 1) % 4;
				V.line(g, q[i * 2], q[i * 2 + 1], q[j * 2], q[j * 2 + 1], 2.5F, V.INK);
				V.ellipse(g, q[i * 2], q[i * 2 + 1], 1.25F, 1.25F, V.INK);
			}
		}
		// The scale.
		int blocks = zi == 0 ? 200 : zi == 1 ? 100 : zi == 2 ? 50 : 20;
		float bw = (float) (blocks * z);
		V.rr(g, MX + 14, MY + VH - 40, bw + 90, 26, 13, 0xE6FFFFFF);
		V.rr(g, MX + 26, MY + VH - 28, bw, 4, 0, V.INK);
		V.text(g, blocks + " " + Lang.t("blocks"), MX + 34 + bw, V.capsTop(MY + VH - 26, 12), 12, V.INK, true);
		// The window's rounded corners: the rim's colours, then the sea round it.
		corners(g, MX + 3, MY + 3, VW - 6, VH - 6, 19, 3, new int[] {0xFFFFB8D2, 0xFFFFE7A1, 0xFFFFE7A1, 0xFFFFB8D2});
		corners(g, MX, MY, VW, VH, 22, 10, new int[] {0xFF348CCE, 0xFF348CCE, 0xFF348CCE, 0xFF348CCE});
		g.disableScissor();
		// Pointing at a place picks it.
		if (!dragging && inMap(mx, my)) {
			int b = nearest(mx, my);
			if (b >= 0 && b != sel) pick(b, false);
		}
	}

	/** A pin's zone: the one you've marked in game, or the one the viewer knows. */
	private int[] zoneOf(Pin p) {
		for (ClientState.Zone z : ClientState.zones) if (z.place().equals(p.key())) return new int[] {z.x1(), z.z1(), z.x2() + 1, z.z2() + 1};
		return p.zone();
	}

	private static void dashed(GuiGraphics g, float x, float y, float w, float h, int col) {
		float lw = 1.5F;
		for (float d = 0; d < w; d += 11) {
			float l = Math.min(6, w - d);
			V.rr(g, x + d, y - lw / 2, l, lw, 0, col);
			V.rr(g, x + d, y + h - lw / 2, l, lw, 0, col);
		}
		for (float d = 0; d < h; d += 11) {
			float l = Math.min(6, h - d);
			V.rr(g, x - lw / 2, y + d, lw, l, 0, col);
			V.rr(g, x + w - lw / 2, y + d, lw, l, 0, col);
		}
	}

	/** The town's buildings, for the panel (the places file only has the outside of a town): the viewer's TOWN_HERE. */
	private static final Map<String, List<String>> TOWN_HERE = Map.ofEntries(Map.entry("PALLET TOWN", List.of("PROF. OAK's LAB", "Your house", "Rival's house")),
			Map.entry("VIRIDIAN CITY", List.of("POKéMON CENTER", "POKé MART", "GYM")), Map.entry("PEWTER CITY", List.of("POKéMON CENTER", "POKé MART", "GYM", "MUSEUM")),
			Map.entry("CERULEAN CITY", List.of("POKéMON CENTER", "POKé MART", "GYM", "BIKE SHOP")),
			Map.entry("SAFFRON CITY", List.of("POKéMON CENTER", "POKé MART", "GYM", "SILPH CO.")),
			Map.entry("VERMILION CITY", List.of("POKéMON CENTER", "POKé MART", "GYM", "FAN CLUB")), Map.entry("LAVENDER TOWN", List.of("POKéMON CENTER", "POKé MART", "TOWER")),
			Map.entry("CELADON CITY", List.of("POKéMON CENTER", "DEPT. STORE", "GYM", "GAME CORNER")),
			Map.entry("FUCHSIA CITY", List.of("POKéMON CENTER", "POKé MART", "GYM", "SAFARI ZONE")),
			Map.entry("CINNABAR ISLAND", List.of("POKéMON CENTER", "POKé MART", "GYM", "LAB")), Map.entry("INDIGO PLATEAU", List.of("POKéMON CENTER", "POKéMON LEAGUE")));
	/** The gyms: leader, type, badge (0 to 7) and their team's levels, by town. */
	private static final Map<String, String[]> GYMS = Map.of("pewter_city", new String[] {"BROCK", "ROCK", "0", "12 to 14"}, "cerulean_city",
			new String[] {"MISTY", "WATER", "1", "18 to 21"}, "vermilion_city", new String[] {"LT. SURGE", "ELECTRIC", "2", "18 to 24"}, "celadon_city",
			new String[] {"ERIKA", "GRASS", "3", "24 to 29"}, "fuchsia_city", new String[] {"KOGA", "POISON", "4", "37 to 43"}, "saffron_city",
			new String[] {"SABRINA", "PSYCHIC", "5", "37 to 43"}, "cinnabar_island", new String[] {"BLAINE", "FIRE", "6", "40 to 47"}, "viridian_city",
			new String[] {"GIOVANNI", "GROUND", "7", "42 to 50"});
	private static final String[] BADGE_NAMES = {"BOULDERBADGE", "CASCADEBADGE", "THUNDERBADGE", "RAINBOWBADGE", "SOULBADGE", "MARSHBADGE", "VOLCANOBADGE", "EARTHBADGE"};
	/** How a place's POKéMON are found, each its own colour. */
	private static final Map<String, Integer> HOW_COLS = Map.of("Grass", 0xFFD9F5C8, "Surfing", 0xFFCFE8FF, "Fishing", 0xFFCFF2F0, "Headbutt", 0xFFF3E2C8, "Rock Smash",
			0xFFE8E2DA, "Swarms", 0xFFFFE3CC, "Bug-Catching Contest", 0xFFEEF6C8);

	/**
	 * The panel down the right, as the viewer's: a picture of the place at the top (yours from when you were there,
	 * else the map close in round it), what kind of place and its name, the line from its sign, what's there, its gym,
	 * how its POKéMON are found and which, and FLY HERE for a town you've been to.
	 */
	private void drawInfo(GuiGraphics g) {
		float x = 940, y = 20, w = 320, h = 626;
		V.panel(g, x, y, w, h);
		vhit(x, y, w, h, () -> {
		});
		if (P.isEmpty()) return;
		Pin p = P.get(sel);
		boolean town = p.kind().equals("town"), seen = been(p.key());
		float px = x + 24, cw = w - 48;
		// The picture, turned a little.
		float pw = cw + 16, ph = 150, picX = px - 8, picY = y + 22 - 8;
		V.shadow(g, picX, picY, pw, ph, 18, 0.9F);
		g.pose().pushMatrix();
		float pcx = V.x(picX + pw / 2), pcy = V.y(picY + ph / 2);
		g.pose().translate(pcx, pcy);
		g.pose().rotate((float) Math.toRadians(-1.2));
		g.pose().translate(-pcx, -pcy);
		V.rr(g, picX - 3, picY - 3, pw + 6, ph + 6, 21, 0xFFFFFFFF);
		Identifier pic = Postcards.picture(p.key());
		int gx = Math.round(V.x(picX)), gy = Math.round(V.y(picY)), gw = Math.round(V.s(pw)), gh = Math.round(V.s(ph));
		g.fill(gx, gy, gx + gw, gy + gh, TownMap.SEA);
		if (pic != null) g.blit(RenderPipelines.GUI_TEXTURED, pic, gx, gy, 0, 8, gw, gh, 384, 200, 384, 216);
		else if (TownMap.ready()) {
			float k = 2.5F * V.s(1);
			worldCut(g, p.x() - gw / 2.0 / k, p.z() - gh / 2.0 / k, gw, gh, k, gx, gy);
		}
		// Its corners rounded off in white (the rim round it, and the panel behind).
		Ui.cornersOut(g, V.x(picX), V.y(picY), V.s(pw), V.s(ph), V.s(18), new int[] {0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF});
		if (pic == null) {
			String t = "PICTURE TO COME";
			float tw = V.trackedWidth(t, 11.5F, 1) + 20, th = 11.5F * 1.36F + 6;
			V.pill(g, picX + 10, picY + ph - 8 - th, tw, th, 0xEBFFFFFF);
			V.tracked(g, t, picX + 20, V.capsTop(picY + ph - 8 - th / 2, 11.5F), 11.5F, V.MUTED, 1);
		}
		g.pose().popMatrix();
		float cy = picY + ph + 14;
		// What kind of place, and where; its name.
		String kind = switch (p.kind()) {
			case "town" -> "Town";
			case "route" -> "Route";
			default -> "Place";
		};
		boolean mineHere = p.key().equals(mine());
		V.label(g, mineHere ? Lang.t("You are here") : Lang.t(kind) + " · " + (region.equals("johto") ? "JOHTO" : "KANTO"), px, cy);
		cy += 18 + 6;
		float ts = p.name().length() > 14 ? 27 : 32;
		for (String l : wrapTitle(p.name(), cw, ts)) {
			V.title(g, l, px, cy, ts, V.INK);
			cy += ts * 1.1F;
		}
		cy += 10;
		String tag = Adventure.signLine(p.name());
		if (!tag.isEmpty()) {
			for (String l : V.wrap("\"" + tag + "\"", cw, 18, false)) {
				V.text(g, l, px, cy, 18, V.TEXT, false);
				cy += 18 * 1.45F;
			}
		}
		// What's there.
		List<String> here = TOWN_HERE.getOrDefault(p.name(), p.here());
		if (!here.isEmpty()) {
			cy += 16;
			V.label(g, Lang.t("Here"), px, cy);
			cy += 18 + 7;
			cy = chips(g, here, px, cy, cw, 14.5F, 11, 4, 6, null, true);
		}
		// Its gym.
		String[] gym = GYMS.get(p.key());
		if (gym != null) {
			cy += 16;
			V.label(g, Lang.t("Gym"), px, cy);
			cy += 18 + 8;
			float gh2 = 10 + 64 + 10;
			V.rr(g, px, cy, cw, gh2, 16, 0xFFF1F4FB);
			int b = Integer.parseInt(gym[2]);
			V.icon(g, "badge_" + b, px + 12, cy + (gh2 - 58) / 2, 58);
			float tx0 = px + 12 + 58 + 12;
			V.text(g, gym[0], tx0, cy + 10, 19, V.INK, true);
			V.text(g, Lang.t("{0} type", gym[1]) + " · " + BADGE_NAMES[b], tx0, cy + 10 + 26, 14, V.MUTED, true);
			V.text(g, Lang.t("Team from level {0}", gym[3]), tx0, cy + 10 + 26 + 19, 14, V.TEXT, false);
			cy += gh2;
		}
		// How its POKéMON are found, and which.
		if (!p.how().isEmpty()) {
			cy += 14;
			cy = chips(g, p.how(), px, cy, cw, 12, 9, 3, 5, HOW_COLS, false);
		}
		List<String> wl = p.wild().subList(0, Math.min(p.wild().size(), gym != null ? 4 : 8));
		float bottom = y + h - 22 - (town ? 43 + 8 + 17 : 17) - 12;
		if (!wl.isEmpty()) {
			cy += 10;
			String lab = Lang.t("Wild POKéMON");
			V.label(g, lab, px, cy);
			V.label(g, "(" + p.wild().size() + ")", px + V.labelWidth(lab) + 6, cy, V.MUTED);
			cy += 18 + 8;
			float cell = (cw - 3 * 6) / 4;
			for (int i = 0; i < wl.size(); i++) {
				float wx = px + (i % 4) * (cell + 6), wy = cy + (i / 4) * (cell + 6);
				if (wy + cell > bottom) break;
				V.rr(g, wx, wy, cell, cell, 12, 0xFFF1F4FB);
				String sid = wl.get(i);
				if (Species.ALL.containsKey(sid)) V.img(g, Look.portrait(sid), wx + (cell - 56) / 2, wy + (cell - 56) / 2, 56, 56, 128, 128, 0xFFFFFFFF);
				else V.centred(g, "?", wx + cell / 2, V.capsTop(wy + cell / 2, 26), 26, V.DIM, true);
			}
		}
		// The foot: FLY HERE, or not been yet; where it is. (With FAST TRAVEL, GO HERE on every place.)
		float fy = bottom + 12;
		if (Settings.fastTravel()) {
			float bh = 43;
			V.softShadow(g, px, fy + 3, cw, bh, 8, 0x4DE8961E);
			V.pill(g, px, fy, cw, bh, 0xFFFFFFFF);
			V.rrv(g, px + 2, fy + 2, cw - 4, bh - 4, (bh - 4) / 2, V.SEL_TOP, V.SEL_BOTTOM);
			V.centred(g, Lang.t("A: GO HERE"), px + cw / 2, V.capsTop(fy + bh / 2, 17), 17, V.INK, false);
			vhit(px, fy, cw, bh, this::fly);
			fy += bh + 8;
		} else if (town) {
			boolean ok = seen && canFly();
			float bh = 43;
			if (ok) {
				V.softShadow(g, px, fy + 3, cw, bh, 8, 0x4DE8961E);
				V.pill(g, px, fy, cw, bh, 0xFFFFFFFF);
				V.rrv(g, px + 2, fy + 2, cw - 4, bh - 4, (bh - 4) / 2, V.SEL_TOP, V.SEL_BOTTOM);
				V.centred(g, Lang.t("A: FLY HERE"), px + cw / 2, V.capsTop(fy + bh / 2, 17), 17, V.INK, false);
				vhit(px, fy, cw, bh, this::fly);
			} else {
				V.pill(g, px, fy, cw, bh, Ui.alpha(0xFFDDE3F0, 0.7F));
				V.pill(g, px + 2, fy + 2, cw - 4, bh - 4, Ui.alpha(0xFFFFFFFF, 0.7F));
				V.centred(g, Lang.t(seen ? "You can't fly yet" : "Not been here yet"), px + cw / 2, V.capsTop(fy + bh / 2, 15), 15, Ui.alpha(V.INK, 0.7F), false);
			}
			fy += bh + 8;
		}
		int[] zn = zoneOf(p);
		String where = zn != null ? Lang.t("Zone in the mod: {0}, {1} to {2}, {3}", zn[0], zn[1], zn[2], zn[3]) : Lang.t("About {0}, {1} on your map", p.x(), p.z());
		V.text(g, where, px, fy, 12.5F, V.MUTED, true);
	}

	/** A name in Fredoka, wrapped to a width. */
	private static List<String> wrapTitle(String t, float w, float size) {
		List<String> out = new ArrayList<>();
		String line = "";
		for (String word : t.split(" ")) {
			String next = line.isEmpty() ? word : line + " " + word;
			if (!line.isEmpty() && V.titleWidth(next, size) > w) {
				out.add(line);
				line = word;
			} else line = next;
		}
		if (!line.isEmpty()) out.add(line);
		return out;
	}

	/** Words in pills, wrapping (gap apart); a red dot before each if dot. Returns the y under them. */
	private float chips(GuiGraphics g, List<String> words, float x, float y, float w, float size, float padX, float padY, float gap, Map<String, Integer> cols, boolean dot) {
		float h = size * 1.36F + padY * 2, cx = x;
		List<String> seenWords = new ArrayList<>();
		for (String t : words) {
			if (seenWords.contains(t)) continue;
			seenWords.add(t);
			String shown = Lang.t(t);
			float tw = dot ? V.width(shown, size, true) : V.trackedWidth(shown, size, 0.5F);
			float cw = padX * 2 + (dot ? 8 + 7 : 0) + tw;
			if (cx > x && cx + cw > x + w) {
				cx = x;
				y += h + gap;
			}
			int bg = cols == null ? 0xFFF1F4FB : cols.getOrDefault(t, 0xFFF1F4FB);
			V.pill(g, cx, y, cw, h, bg);
			float tx0 = cx + padX;
			if (dot) {
				V.rr(g, tx0, y + h / 2 - 4, 8, 8, 3, 0xFFE5525C);
				tx0 += 8 + 7;
				V.text(g, shown, tx0, V.capsTop(y + h / 2, size), size, V.INK, true);
			} else V.tracked(g, shown, tx0, V.capsTop(y + h / 2, size), size, V.INK, 0.5F);
			cx += cw + gap;
		}
		return y + h;
	}

	/** The way to the other region: an arrow tab at the map's edge, bobbing gently, JOHTO on the left, KANTO on the right. */
	private void drawSide(GuiGraphics g, float secs) {
		boolean kanto = region.equals("kanto");
		String word = kanto ? "JOHTO" : "KANTO";
		float tw = V.titleTrackedWidth(word, 20, 2), w = 18 + 26 + 8 + tw + 18, h = 56;
		float bob = (float) (1 - Math.cos(secs / 1.6F * Math.PI * 2)) / 2 * 6;
		float x = kanto ? 34 - bob : MX + VW - 160 + bob, y = MY + VH / 2 - 28;
		boolean on = vover(x, y, w, h);
		if (on) overUi = true;
		V.shadow(g, x, y, w, h, h / 2, 1.3F);
		V.pill(g, x - 3, y - 3, w + 6, h + 6, 0x80FFFFFF);
		if (on) V.rrv(g, x, y, w, h, h / 2, V.SEL_TOP, V.SEL_BOTTOM);
		else V.pill(g, x, y, w, h, 0xFFFFFFFF);
		float ax, tx0;
		if (kanto) {
			ax = x + 18;
			tx0 = ax + 26 + 8;
		} else {
			tx0 = x + 18;
			ax = tx0 + tw + 8;
		}
		float u = 26 / 24F, cy = y + h / 2, lw = 4 * u;
		float[] pts = kanto ? new float[] {16, 4, 7, 12, 16, 20} : new float[] {8, 4, 17, 12, 8, 20};
		for (int i = 0; i < 2; i++) V.line(g, ax + pts[i * 2] * u, cy - 13 + pts[i * 2 + 1] * u, ax + pts[i * 2 + 2] * u, cy - 13 + pts[i * 2 + 3] * u, lw, 0xFFE5525C);
		for (int i = 0; i < 3; i++) V.ellipse(g, ax + pts[i * 2] * u, cy - 13 + pts[i * 2 + 1] * u, lw / 2, lw / 2, 0xFFE5525C);
		V.titleTracked(g, word, tx0, V.capsTop(cy, 20), 20, V.INK, 2);
		vhit(x, y, w, h, this::flip);
	}

	/** A big, plain way out: BACK, top left over the map (and, from the Pokédex, whose places these are). */
	private void drawBack(GuiGraphics g) {
		float x = 38, y = 36, hh = 52, w = 8 + 36 + 12 + V.titleWidth(Lang.t("Back"), 20) + 20;
		boolean on = vover(x, y, w, hh);
		if (on) overUi = true;
		V.pill(g, x, y + 6, w, hh, 0x2E1E2854);
		V.shadow(g, x, y, w, hh, hh / 2, 0.7F);
		V.pill(g, x, y, w, hh, on ? 0xFFFFC93C : 0xFFFFFFFF);
		V.rrv(g, x + 3, y + 3, w - 6, hh - 6, (hh - 6) / 2, on ? V.SEL_TOP : 0xFFFFFFFF, on ? V.SEL_BOTTOM : 0xFFEEF2FB);
		V.rrv(g, x + 8, y + hh / 2 - 18, 36, 36, 18, 0xFFFF8A7A, 0xFFE5525C);
		V.text(g, "B", x + 8 + 18 - V.width("B", 13, true) / 2, V.capsTop(y + hh / 2, 13), 13, 0xFFFFFFFF, true);
		V.title(g, Lang.t("Back"), x + 8 + 36 + 12, V.capsTop(y + hh / 2, 20), 20, V.INK);
		vhit(x, y, w, hh, this::back);
		if (highlightTitle != null) {
			String t = Lang.t("Where {0} lives", highlightTitle);
			float tw = V.width(t, 14, true) + 32, th = 44, hx = 170, hy = 40;
			if (vover(hx, hy, tw, th)) overUi = true;
			V.shadow(g, hx, hy, tw, th, th / 2, 0.8F);
			V.rrv(g, hx, hy, tw, th, th / 2, V.SEL_TOP, V.SEL_BOTTOM);
			V.text(g, t, hx + 16, V.capsTop(hy + th / 2, 14), 14, V.INK, true);
		}
	}
}
