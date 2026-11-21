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
 * same sums): by day a warm white pool under each window, a soft shaft down to it; low in the morning or evening the
 * sun's golden, its pools longer and further in and to one side (the windows facing away from it in shade), the
 * glass a sunset; at night a faint blue moonlight, the glass dark with stars. The lamps keep the room bright.
 */
public class RoomLightRenderer extends EntityRenderer<RoomLight, RoomLightRenderer.State> {
	private static final int FULL_BRIGHT = 15728880;
	private static final Identifier POOL = Retromon.id("textures/light/pool.png"), POOL_SOFT = Retromon.id("textures/light/pool_soft.png"),
			BEAM = Retromon.id("textures/light/beam.png"), DUSK = Retromon.id("textures/light/window_dusk.png"),
			NIGHT = Retromon.id("textures/light/window_night.png"), DOOR_DUSK = Retromon.id("textures/light/door_dusk.png"),
			DOOR_NIGHT = Retromon.id("textures/light/door_night.png");
	/** How high over the floor the pools lie (over the carpets). */
	private static final double POOL_Y = 0.07;

	public static class State extends EntityRenderState {
		List<double[]> pools = List.of(), panes = List.of();
		double cx, cz;
		float k, side, m;
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
		s.cx = e.cx;
		s.cz = e.cz;
		float[] sky = sky();
		s.k = sky[0];
		s.side = sky[1];
		s.m = sky[2];
	}

	@Override
	public void submit(State s, PoseStack pose, SubmitNodeCollector out, CameraRenderState camera) {
		float[] c = colour(s.k, s.m);
		double cx = s.cx, cz = s.cz;
		// The pools and their shafts.
		for (double[] p : s.pools) {
			double[] q = rect(p, s.k, s.side, s.m);
			if (q == null) continue;
			int pool = argb(c, (float) (c[3] * q[4])), shaft = argb(c, (float) (c[4] * q[4]));
			out.submitCustomGeometry(pose, RenderTypes.entityTranslucentEmissive(p[4] == 3 ? POOL_SOFT : POOL), (pp, vc) -> {
				// (Counter-clockwise seen from above.)
				vert(vc, pp, q[0] - cx, POOL_Y, q[1] - cz, 0, 0, pool, 0, 1, 0);
				vert(vc, pp, q[0] - cx, POOL_Y, q[3] - cz, 0, 1, pool, 0, 1, 0);
				vert(vc, pp, q[2] - cx, POOL_Y, q[3] - cz, 1, 1, pool, 0, 1, 0);
				vert(vc, pp, q[2] - cx, POOL_Y, q[1] - cz, 1, 0, pool, 0, 1, 0);
			});
			double[][][] beam = beam(p, q);
			if (beam.length > 0 && (shaft >>> 24) > 1)
				out.submitCustomGeometry(pose, RenderTypes.entityTranslucentEmissive(BEAM), (pp, vc) -> {
					for (double[][] quad : beam)
						for (double[] v : quad) vert(vc, pp, v[0] - cx, v[1], v[2] - cz, (float) v[3], (float) v[4], shaft, 0, 1, 0);
				});
		}
		// The windows' glass: a sunset (or sunrise) over the day's, then the night's over that.
		float dusk = s.k * (1 - s.m), night = s.m;
		if (!s.panes.isEmpty()) {
			// Windows' glass, and open ways out's daylight (the League gate's), each with their own skies.
			List<double[]> glass = new java.util.ArrayList<>(), doors = new java.util.ArrayList<>();
			for (double[] g : s.panes) (g.length > 12 && g[12] == 1 ? doors : glass).add(g);
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

	private static void vert(VertexConsumer vc, PoseStack.Pose p, double x, double y, double z, float u, float v, int argb, float nx, float ny, float nz) {
		vc.addVertex(p, (float) x, (float) y, (float) z).setColor(argb).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(p, nx, ny, nz);
	}

	private static int argb(float[] c, float a) {
		int ai = Math.max(0, Math.min(255, Math.round(a * 255)));
		return ai << 24 | Math.round(c[0] * 255) << 16 | Math.round(c[1] * 255) << 8 | Math.round(c[2] * 255);
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
		float[] day = {1, 0.96F, 0.82F, 0.55F, 0.1F}, gold = {1, 0.74F, 0.44F, 0.5F, 0.13F}, moon = {0.55F, 0.7F, 1, 0.28F, 0.05F};
		float[] c = new float[5];
		for (int i = 0; i < 5; i++) {
			float a = day[i] + (gold[i] - day[i]) * k;
			c[i] = a + (moon[i] - a) * m;
		}
		return c;
	}

	/**
	 * Where a pool falls now and how strong it is there ([x0, z0, x1, z1, strength], or null). p: [x0, z0, x1, z1] at
	 * midday, the way the light goes in (0 south from the north wall, 1 west from the east wall, 2 east from the west
	 * wall, 3 north in at the door), the rect it's kept in, then its window (its wall's place, from and to along the
	 * wall, its foot and top).
	 */
	static double[] rect(double[] p, float k, float side, float m) {
		double x0 = p[0], z0 = p[1], x1 = p[2], z1 = p[3];
		int dir = (int) p[4];
		double shift = 0.6 * k, scale = 1 + 0.7 * k - 0.15 * m, lat = 0.8 * k * side, s = 1;
		if (dir == 0) {
			double l = (z1 - z0) * scale;
			z0 += shift;
			z1 = z0 + l;
			x0 += lat;
			x1 += lat;
		} else if (dir == 3) z0 = z1 - (z1 - z0) * scale;
		else if (dir == 1) {
			double l = (x1 - x0) * scale;
			x1 -= shift;
			x0 = x1 - l;
			if (side > 0) s = 1 - 0.65 * k;
		} else {
			double l = (x1 - x0) * scale;
			x0 += shift;
			x1 = x0 + l;
			if (side < 0) s = 1 - 0.65 * k;
		}
		x0 = Math.max(x0, p[5]);
		z0 = Math.max(z0, p[6]);
		x1 = Math.min(x1, p[7]);
		z1 = Math.min(z1, p[8]);
		return x1 - x0 > 0.1 && z1 - z0 > 0.1 ? new double[] {x0, z0, x1, z1, s} : null;
	}

	/** The shaft from a pool's window down to it: four sheets, each four corners of [x, y, z, u, v] (v 0 at the window). */
	static double[][][] beam(double[] p, double[] q) {
		int dir = (int) p[4];
		if (dir == 3 || p.length < 14 || p[13] <= p[12]) return new double[0][][];
		double wc = p[9], a0 = p[10], a1 = p[11], wy0 = p[12], wy1 = p[13], y = POOL_Y;
		double la0 = dir == 0 ? q[0] : q[1], la1 = dir == 0 ? q[2] : q[3];
		double near = dir == 0 ? q[1] : dir == 1 ? q[2] : q[0], far = dir == 0 ? q[3] : dir == 1 ? q[0] : q[2];
		boolean n = dir == 0;
		return new double[][][] {
				{pt(n, a0, wy1, wc, 0, 0), pt(n, a1, wy1, wc, 1, 0), pt(n, la1, y, far, 1, 1), pt(n, la0, y, far, 0, 1)},
				{pt(n, a0, wy0, wc, 0, 0), pt(n, a1, wy0, wc, 1, 0), pt(n, la1, y, near, 1, 1), pt(n, la0, y, near, 0, 1)},
				{pt(n, a0, wy0, wc, 0, 0), pt(n, a0, wy1, wc, 1, 0), pt(n, la0, y, far, 1, 1), pt(n, la0, y, near, 0, 1)},
				{pt(n, a1, wy0, wc, 0, 0), pt(n, a1, wy1, wc, 1, 0), pt(n, la1, y, far, 1, 1), pt(n, la1, y, near, 0, 1)}};
	}

	private static double[] pt(boolean north, double a, double y, double d, double u, double v) {
		return north ? new double[] {a, y, d, u, v} : new double[] {d, y, a, u, v};
	}

	// ---------- putting it in the room you're in ----------

	private static final int ROOM_ID = -76000;
	private static RoomLight here;
	private static String hereId;

	/** Each tick: the light in the room you're in (put in as you come in, taken out as you leave). */
	public static void tick(Minecraft mc) {
		ClientLevel lv = mc.level;
		Interiors.Room r = mc.player == null || lv == null ? null : Interiors.roomOf(mc.player);
		if (r != null && r.pools().isEmpty() && r.panes().isEmpty()) r = null;
		if (here != null && (here.level() != lv || here.isRemoved() || r == null || !r.id().equals(hereId))) {
			if (here.level() == lv && !here.isRemoved()) remove(here);
			here = null;
			hereId = null;
		}
		if (r != null && here == null) {
			here = spawn(lv, ROOM_ID, r.ox(), r.oy(), r.oz(), r.w(), r.d(), r.pools(), r.panes());
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
