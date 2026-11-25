package com.insidethecube.retromon.client;

import java.util.List;

import com.insidethecube.retromon.Retromon;
import com.insidethecube.retromon.game.Interiors;
import com.insidethecube.retromon.game.RoomLight;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;

/**
 * The light through the windows, by the time of day (the Interior Viewer's Morning, Day, Evening and Night are the
 * same sums): the daylight spreads in over the floor from each window, strongest by it and widening and fading the
 * further in it gets, with no edge anywhere (as the sky's light comes into a room in Tempered Light). Low in the
 * morning or evening it's golden and reaches further in, and to one side (the windows facing away from the sun
 * dimmer), the glass a sunset; at night a faint blue moonlight, the glass dark with stars. The lamps keep the room bright.
 */
public class RoomLightRenderer extends EntityRenderer<RoomLight, RoomLightRenderer.State> {
	private static final int FULL_BRIGHT = 15728880;
	private static final Identifier WASH = Retromon.id("textures/light/wash.png"), PANEL = Retromon.id("textures/light/screen_panel.png"),
			SLOT = Retromon.id("textures/light/screen_slot.png"), DOT = Retromon.id("textures/light/screen_dot.png"), DUSK = Retromon.id("textures/light/window_dusk.png"),
			NIGHT = Retromon.id("textures/light/window_night.png"), DOOR_DUSK = Retromon.id("textures/light/door_dusk.png"),
			DOOR_NIGHT = Retromon.id("textures/light/door_night.png");
	/** How high over the floor the light lies (over the carpets). */
	private static final double POOL_Y = 0.07;

	public static class State extends EntityRenderState {
		List<double[]> pools = List.of(), panes = List.of(), clocks = List.of();
		double cx, cz;
		/** The time on the clocks (hours, 0 to 24). */
		float hours;
		/** The Center's big screen while the nurse's machine works: how far along (0 to 1, or -1), and where she stands from here. */
		float heal = -1;
		double nx, ny, nz;
		float secs;
		float k, side, m;
		/** (0.66) The room's lamps are in its own colours (RoomShade): not laid over again here. */
		boolean shaded;
	}

	public RoomLightRenderer(EntityRendererProvider.Context ctx) {
		super(ctx);
		this.shadowRadius = 0;
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public boolean shouldRender(RoomLight e, Frustum frustum, double x, double y, double z) {
		return true;
	}

	@Override
	public void extractRenderState(RoomLight e, State s, float partial) {
		super.extractRenderState(e, s, partial);
		s.pools = e.pools;
		s.panes = e.panes;
		s.clocks = e.clocks;
		boolean healing = healNurse != null && System.currentTimeMillis() - healMs < 300 && healH > 0 && healH < 1;
		s.heal = healing ? healH : -1;
		if (healing) {
			s.nx = healNurse.x - e.getX();
			s.ny = healNurse.y - e.getY();
			s.nz = healNurse.z - e.getZ();
		}
		s.secs = (System.currentTimeMillis() % 100000) / 1000F;
		Minecraft mc = Minecraft.getInstance();
		// (Minecraft's day starts at six in the morning.)
		s.hours = mc.level == null ? 9.25F : (float) ((Math.floorMod(mc.level.getDayTime(), 24000L) / 1000.0 + 6) % 24);
		s.cx = e.cx;
		s.cz = e.cz;
		s.shaded = com.insidethecube.retromon.game.Interiors.in(e) && RoomShade.lit(roomId(e.getX()));
		float[] sky = sky();
		s.k = sky[0];
		s.side = sky[1];
		s.m = sky[2];
	}

	@Override
	public void submit(State s, PoseStack pose, SubmitNodeCollector out, CameraRenderState camera) {
		float[] c = colour(s.k, s.m);
		double cx = s.cx, cz = s.cz;
		// The light from each window over the floor: a fine grid, each point as strong as the light is there.
		for (double[] p : s.pools) {
			double[] g = washGrid(p, s.k, s.side, s.m);
			if (g == null) continue;
			float peak = c[3] * 0.65F * (float) g[6];
			int na = (int) g[4], nd = (int) g[5];
			out.submitCustomGeometry(pose, RenderTypes.entityTranslucentEmissive(WASH), (pp, vc) -> {
				double[] at = new double[(na + 1) * (nd + 1)];
				for (int j = 0; j <= nd; j++)
					for (int i = 0; i <= na; i++) {
						double a = g[0] + (g[1] - g[0]) * i / na, d = g[2] + (g[3] - g[2]) * j / nd;
						at[j * (na + 1) + i] = washAt(p, a, d, s.k, s.side, s.m) * peak;
					}
				for (int j = 0; j < nd; j++)
					for (int i = 0; i < na; i++) {
						double v00 = at[j * (na + 1) + i], v10 = at[j * (na + 1) + i + 1], v01 = at[(j + 1) * (na + 1) + i], v11 = at[(j + 1) * (na + 1) + i + 1];
						if (v00 + v10 + v01 + v11 < 0.004) continue;
						double a0 = g[0] + (g[1] - g[0]) * i / na, a1 = g[0] + (g[1] - g[0]) * (i + 1) / na;
						double d0 = g[2] + (g[3] - g[2]) * j / nd, d1 = g[2] + (g[3] - g[2]) * (j + 1) / nd;
						// (Counter-clockwise seen from above, whichever wall the window's in.)
						double[] p00 = floorAt(p, a0, d0), p10 = floorAt(p, a1, d0), p11 = floorAt(p, a1, d1), p01 = floorAt(p, a0, d1);
						boolean flip = (int) p[4] != 2;
						double[][] quad = flip ? new double[][] {p00, p01, p11, p10} : new double[][] {p00, p10, p11, p01};
						double[] al = flip ? new double[] {v00, v01, v11, v10} : new double[] {v00, v10, v11, v01};
						for (int q = 0; q < 4; q++)
							vert(vc, pp, quad[q][0] - cx, POOL_Y, quad[q][1] - cz, 0.5F, 0.5F, argb(c, (float) al[q]), 0, 1, 0);
					}
			});
		}
		// The wall clocks' hands, keeping the world's time: the hour hand short and broad, the minute hand long.
		if (!s.clocks.isEmpty()) {
			double ha = s.hours % 12 / 12 * Math.PI * 2, ma = s.hours % 1 * Math.PI * 2;
			out.submitCustomGeometry(pose, RenderTypes.entityTranslucentEmissive(WASH), (pp, vc) -> {
				for (double[] k : s.clocks) {
					hand(vc, pp, k[0] - cx, k[1], k[2] - cz + 0.004, ha, 0.15, 0.035, 0xFF2B2420);
					hand(vc, pp, k[0] - cx, k[1], k[2] - cz + 0.008, ma, 0.22, 0.025, 0xFF2B2420);
					hand(vc, pp, k[0] - cx, k[1], k[2] - cz + 0.012, 0, 0.02, 0.04, 0xFFC9A23A);
				}
			});
		}
		if (s.heal >= 0) healScreen(s, pose, out);
		// The windows' glass: a sunset (or sunrise) over the day's, then the night's over that.
		float dusk = s.k * (1 - s.m), night = s.m;
		if (!s.panes.isEmpty()) {
			// Windows' glass, and open ways out's daylight (the League gate's), each with their own skies.
			List<double[]> glass = new java.util.ArrayList<>(), doors = new java.util.ArrayList<>(), lamps = new java.util.ArrayList<>();
			for (double[] g : s.panes) (g.length > 12 && g[12] == 2 ? lamps : g.length > 12 && g[12] == 1 ? doors : glass).add(g);
			// (0.62, John's 19:22: "during like the daytime, would you have the lights off?") The lamps: off by day (morning
			// and day), on in the evening and at night, and on all the time in a room with no windows.
			if (!lamps.isEmpty()) {
				float on = glass.isEmpty() && s.pools.isEmpty() ? 1 : Math.max(night, s.side > 0 ? s.k : 0);
				if (on > 0.01F && !s.shaded) lamps(out, pose, lamps, on, cx, cz);
			}
			if (dusk > 0.01F) {
				if (!glass.isEmpty()) panes(out, pose, glass, DUSK, dusk, cx, cz);
				if (!doors.isEmpty()) panes(out, pose, doors, DOOR_DUSK, dusk, cx, cz);
			}
			if (night > 0.01F) {
				if (!glass.isEmpty()) panes(out, pose, glass, NIGHT, night, cx, cz);
				if (!doors.isEmpty()) panes(out, pose, doors, DOOR_NIGHT, night, cx, cz);
			}
		}
	}

	/** The lamps' shades and panels lit: a warm glow laid over each face, as strong as on is (0 to 1). */
	private static void lamps(SubmitNodeCollector out, PoseStack pose, List<double[]> lamps, float on, double cx, double cz) {
		int col = ((int) (Math.min(1, on) * 0.6F * 255) << 24) | 0xFFE6B4;
		out.submitCustomGeometry(pose, RenderTypes.entityTranslucentEmissive(WASH), (pp, vc) -> {
			for (double[] g : lamps) {
				double ox = g[0] + g[9] * 0.006 - cx, oy = g[1] + g[10] * 0.006, oz = g[2] + g[11] * 0.006 - cz;
				float nx = (float) g[9], ny = (float) g[10], nz = (float) g[11];
				vert(vc, pp, ox, oy, oz, 0.5F, 0.5F, col, nx, ny, nz);
				vert(vc, pp, ox + g[3], oy + g[4], oz + g[5], 0.5F, 0.5F, col, nx, ny, nz);
				vert(vc, pp, ox + g[3] + g[6], oy + g[4] + g[7], oz + g[5] + g[8], 0.5F, 0.5F, col, nx, ny, nz);
				vert(vc, pp, ox + g[6], oy + g[7], oz + g[8], 0.5F, 0.5F, col, nx, ny, nz);
			}
		});
	}

	private static void panes(SubmitNodeCollector out, PoseStack pose, List<double[]> panes, Identifier tex, float a, double cx, double cz) {
		int col = ((int) (Math.min(1, a) * 255) << 24) | 0xFFFFFF;
		out.submitCustomGeometry(pose, RenderTypes.entityTranslucentEmissive(tex), (pp, vc) -> {
			for (double[] g : panes) {
				// [corner, along, up, facing]: laid just in front of the glass, the picture upright.
				double ox = g[0] + g[9] * 0.012 - cx, oy = g[1] + g[10] * 0.012, oz = g[2] + g[11] * 0.012 - cz;
				float nx = (float) g[9], ny = (float) g[10], nz = (float) g[11];
				vert(vc, pp, ox, oy, oz, 0, 1, col, nx, ny, nz);
				vert(vc, pp, ox + g[3], oy + g[4], oz + g[5], 1, 1, col, nx, ny, nz);
				vert(vc, pp, ox + g[3] + g[6], oy + g[4] + g[7], oz + g[5] + g[8], 1, 0, col, nx, ny, nz);
				vert(vc, pp, ox + g[6], oy + g[7], oz + g[8], 0, 0, col, nx, ny, nz);
			}
		});
	}

	// ---------- the Center's big screen while the nurse's machine works ----------

	/** The heal screen is drawn here in the room (not over it as a flat picture). */
	public static final boolean HEAL_IN_ROOM = true;
	private static net.minecraft.world.phys.Vec3 healNurse;
	private static float healH;
	private static long healMs;

	/** Each frame while the machine works (DialogueScreen): where the nurse stands, and how far along it is (0 to 1). */
	public static void healAt(net.minecraft.world.phys.Vec3 nurse, float h) {
		healNurse = nurse;
		healH = h;
		healMs = System.currentTimeMillis();
	}

	/**
	 * The big screen over the machine (2.4 either side of her, 2.55 to 3.85 up, on the back wall 3 behind her), its
	 * picture as the UI Viewer's: pink, your POKéMON's pictures on it, each lit as its ball goes on, a light under each
	 * flashing while the machine works. Laid on the screen's face, so it fits it from any angle.
	 */
	private static void healScreen(State s, PoseStack pose, SubmitNodeCollector out) {
		float h = s.heal, a = Math.min(1, Math.min(h / 0.05F, (1 - h) / 0.05F));
		double x0 = s.nx - 2.4, x1 = s.nx + 2.4, y0 = s.ny + 2.55, y1 = s.ny + 3.85, z = s.nz - 3.0 + 0.132;
		// Design units: 480 across, 130 down from the top (as the viewer's).
		java.util.function.BiFunction<Double, Double, double[]> at = (u, v) -> new double[] {x0 + (x1 - x0) * u / 480, y1 - (y1 - y0) * v / 130};
		rect(out, pose, PANEL, at.apply(0.0, 0.0), at.apply(480.0, 130.0), z + 0.004, Ui.alpha(0xFFFFFFFF, a));
		var party = ClientState.data.party;
		int count = Math.min(6, party.size());
		for (int i = 0; i < 6; i++) {
			double cx = 18 + i * 76, cy = 22, cw = 68, ch = 86;
			boolean on = i < count && h > i * 0.06F + 0.04F;
			rect(out, pose, SLOT, at.apply(cx, cy), at.apply(cx + cw, cy + ch), z + 0.008, Ui.alpha(on ? 0xEBFFFFFF : 0x59FFFFFF, a));
			if (!on) continue;
			double ps = Math.min(cw - 4, ch * 0.7), px = cx + (cw - ps) / 2, py = cy + ch * 0.08;
			rect(out, pose, Look.portrait(party.get(i).species.id()), at.apply(px, py), at.apply(px + ps, py + ps), z + 0.012, Ui.alpha(0xFFFFFFFF, a));
			boolean lit = h > 0.4F && h < 0.78F && Math.sin(s.secs * 14 + i) > 0;
			double dr = cw * 0.06, dx = cx + cw / 2, dy = cy + ch * 0.9;
			rect(out, pose, DOT, at.apply(dx - dr, dy - dr), at.apply(dx + dr, dy + dr), z + 0.012, Ui.alpha(lit ? 0xFF58D3A0 : 0xFFE5525C, a));
		}
	}

	/** A picture on the back wall facing into the room: tl its top-left corner, br its bottom-right ([x, y]). */
	private static void rect(SubmitNodeCollector out, PoseStack pose, Identifier tex, double[] tl, double[] br, double z, int argb) {
		out.submitCustomGeometry(pose, RenderTypes.entityTranslucentEmissive(tex), (pp, vc) -> {
			// (Counter-clockwise seen from the south.)
			vert(vc, pp, tl[0], br[1], z, 0, 1, argb, 0, 0, 1);
			vert(vc, pp, br[0], br[1], z, 1, 1, argb, 0, 0, 1);
			vert(vc, pp, br[0], tl[1], z, 1, 0, argb, 0, 0, 1);
			vert(vc, pp, tl[0], tl[1], z, 0, 0, argb, 0, 0, 1);
		});
	}

	/** A clock's hand from its middle (x, y, z, room-centred, facing south), turned a radians clockwise from 12. */
	private static void hand(VertexConsumer vc, PoseStack.Pose p, double x, double y, double z, double a, double len, double w, int argb) {
		double ux = Math.sin(a), uy = Math.cos(a), sx = uy * w / 2, sy = -ux * w / 2, b = 0.02;
		double[][] q = {{-ux * b - sx, -uy * b - sy}, {-ux * b + sx, -uy * b + sy}, {ux * len + sx, uy * len + sy}, {ux * len - sx, uy * len - sy}};
		// (Counter-clockwise seen from the south, where you look at it from.)
		for (int i : new int[] {0, 1, 2, 3}) vert(vc, p, x + q[i][0], y + q[i][1], z, 0.5F, 0.5F, argb, 0, 0, 1);
	}

	private static void vert(VertexConsumer vc, PoseStack.Pose p, double x, double y, double z, float u, float v, int argb, float nx, float ny, float nz) {
		vc.addVertex(p, (float) x, (float) y, (float) z).setColor(argb).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(p, nx, ny, nz);
	}

	private static int argb(float[] c, float a) {
		int ai = Math.max(0, Math.min(255, Math.round(a * 255)));
		return ai << 24 | Math.round(c[0] * 255) << 16 | Math.round(c[1] * 255) << 8 | Math.round(c[2] * 255);
	}

	private static String roomId(double x) {
		com.insidethecube.retromon.game.Interiors.Room r = com.insidethecube.retromon.game.Interiors.roomAt(x);
		return r == null ? null : r.id();
	}

	// ---------- the sums (the Interior Viewer's world.js has the same) ----------

	/**
	 * The sun now: how low and golden it is (0 high or down, 1 at sunrise and sunset), where it is (-1 in the east, the
	 * morning; 1 in the west, the evening) and how far it's night.
	 */
	public static float[] sky() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return new float[] {0, 1, 0};
		long t = Math.floorMod(mc.level.getDayTime(), 24000L);
		float dusk = bump(t - 12000), dawn = bump(Math.floorMod(t - 23400 + 12000, 24000L) - 12000);
		float k = Math.max(dusk, dawn);
		k = k * k * (3 - 2 * k);
		return new float[] {k, dusk >= dawn ? 1 : -1, Night.nightness()};
	}

	private static float bump(double d) {
		return (float) Math.max(0, 1 - Math.abs(d) / 1800.0);
	}

	/** The light's colour, its pools' strength and its shafts': warm white by day, golden low down, a faint blue at night. */
	static float[] colour(float k, float m) {
		float[] day = {1, 0.95F, 0.84F, 0.22F, 0.035F}, gold = {1, 0.8F, 0.56F, 0.22F, 0.045F}, moon = {0.6F, 0.72F, 1, 0.1F, 0.015F};
		float[] c = new float[5];
		for (int i = 0; i < 5; i++) {
			float a = day[i] + (gold[i] - day[i]) * k;
			c[i] = a + (moon[i] - a) * m;
		}
		return c;
	}

	/**
	 * The window's light over the floor now: [a from, a to, d from, d to, columns, rows, strength], or null. a runs along
	 * the window's wall, d in from it. p: the light as the room gives it ([x0, z0, x1, z1] at midday; the way it goes
	 * in: 0 south from the north wall, 1 west from the east wall, 2 east from the west wall; the rect it's kept in,
	 * then its window: its wall's place, from and to along the wall, its foot and top).
	 */
	static double[] washGrid(double[] p, float k, float side, float m) {
		int dir = (int) p[4];
		if (p.length < 14 || p[11] <= p[10]) return null;
		double wc = p[9], L = reach(k, m);
		// Kept inside the room (up to its walls).
		double bx0 = p[5] - 0.3, bz0 = p[6] - 0.3, bx1 = p[7] + 0.3, bz1 = p[8] + 0.3;
		double dMax = Math.min(3.2 * L, dir == 0 ? bz1 - wc : dir == 1 ? wc - bx0 : bx1 - wc);
		if (dMax < 0.3) return null;
		double half = (p[11] - p[10]) / 2, mid = (p[10] + p[11]) / 2 + shift(dir, k, side);
		double reachA = half + 2.6 * spread(dMax, k);
		double a0 = mid - reachA, a1 = mid + reachA;
		if (dir == 0) { a0 = Math.max(a0, bx0); a1 = Math.min(a1, bx1); } else { a0 = Math.max(a0, bz0); a1 = Math.min(a1, bz1); }
		if (a1 - a0 < 0.3) return null;
		double s = 1;
		if (dir == 1 && side > 0) s = 1 - 0.65 * k;
		if (dir == 2 && side < 0) s = 1 - 0.65 * k;
		return new double[] {a0, a1, 0, dMax, Math.max(2, Math.ceil((a1 - a0) / 0.4)), Math.max(2, Math.ceil(dMax / 0.4)), s};
	}

	/** How far in the light reaches (blocks): further with a low sun, less by moonlight. */
	static double reach(float k, float m) {
		return 2.0 + 1.6 * k - 0.4 * m;
	}

	/** How far to one side a low sun throws the light from a window in the north wall (the morning's west, the evening's east). */
	static double shift(int dir, float k, float side) {
		return dir == 0 ? 0.8 * k * side : 0;
	}

	/** How soft the light's sides are, d in from the window: it widens as it goes. */
	static double spread(double d, float k) {
		return 0.55 + 0.3 * d + 0.3 * k;
	}

	/** How strong the window's light is at a point on the floor (a along its wall, d in from it), 0 to 1. */
	static double washAt(double[] p, double a, double d, float k, float side, float m) {
		int dir = (int) p[4];
		double half = (p[11] - p[10]) / 2, mid = (p[10] + p[11]) / 2 + shift(dir, k, side);
		double off = Math.max(0, Math.abs(a - mid) - half), sg = spread(d, k);
		double t = Math.min(1, d / 1.2);
		return Math.exp(-d / reach(k, m)) * (0.6 + 0.4 * t * t * (3 - 2 * t)) * Math.exp(-off * off / (2 * sg * sg));
	}

	/** A point given along the window's wall and in from it, as [x, z] in the room. */
	static double[] floorAt(double[] p, double a, double d) {
		int dir = (int) p[4];
		return dir == 0 ? new double[] {a, p[9] + d} : dir == 1 ? new double[] {p[9] - d, a} : new double[] {p[9] + d, a};
	}

	// ---------- putting it in the room you're in ----------

	private static final int ROOM_ID = -76000;
	private static RoomLight here;
	private static String hereId;

	/** Each tick: the light in the room you're in (put in as you come in, taken out as you leave). */
	public static void tick(Minecraft mc) {
		ClientLevel lv = mc.level;
		Interiors.Room r = mc.player == null || lv == null ? null : Interiors.roomOf(mc.player);
		if (here != null && (here.level() != lv || here.isRemoved() || r == null || !r.id().equals(hereId))) {
			if (here.level() == lv && !here.isRemoved()) remove(here);
			here = null;
			hereId = null;
		}
		if (r != null && here == null) {
			here = spawn(lv, ROOM_ID, r.ox(), r.oy(), r.oz(), r.w(), r.d(), r.pools(), r.panes());
			if (here != null) here.clocks = r.clocks();
			hereId = r.id();
		}
	}

	/** A room's (or a battle hall's) light, its corner at ox, oy, oz. */
	public static RoomLight spawn(ClientLevel lv, int id, double ox, double oy, double oz, double w, double d, List<double[]> pools, List<double[]> panes) {
		if (lv == null) return null;
		RoomLight e = new RoomLight(Retromon.ROOM_LIGHT, lv);
		e.setId(id);
		e.cx = w / 2;
		e.cz = d / 2;
		e.snapTo(ox + e.cx, oy, oz + e.cz, 0, 0);
		e.pools = pools;
		e.panes = panes;
		lv.addEntity(e);
		return e;
	}

	public static void remove(RoomLight e) {
		if (e != null && e.level() instanceof ClientLevel lv) lv.removeEntity(e.getId(), Entity.RemovalReason.DISCARDED);
	}
}
