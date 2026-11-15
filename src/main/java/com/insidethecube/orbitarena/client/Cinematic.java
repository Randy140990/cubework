package com.insidethecube.orbitarena.client;

import com.insidethecube.orbitarena.arena.ArenaLayout;
import com.insidethecube.orbitarena.arena.BridgeLayout;
import com.insidethecube.orbitarena.net.Payloads;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;

/**
 * The fly-round before a match: ten seconds with the camera swooping in from out in space, round the
 * arena (or along the bridge), low over the crowd and on to where you stand, the screen letterboxed and the
 * match's name over it, then straight into "3, 2, 1". Space skips it.
 */
public final class Cinematic {
	private Cinematic() {}

	private static boolean active;
	private static long start;
	private static int length;
	private static String title = "", subtitle = "";
	private static ArmorStand cam;
	private static CameraType before;
	private static Vec3[] path, look;
	private static boolean faded;

	public static boolean active() {
		return active;
	}

	public static void start(Payloads.Cinematic p) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null) return;
		boolean bridge = p.kind() == Payloads.Cinematic.BRIDGE || p.kind() == Payloads.Cinematic.BRIDGE_RED;
		Vec3 c = bridge ? new Vec3(p.centreX(), BridgeLayout.Y, p.centreZ()) : new Vec3(p.centreX(), ArenaLayout.FLOOR, p.centreZ());
		if (bridge) {
			// From far out, in over the other team's temple and round behind its monoliths, low over the stands
			// along the bridge, past your temple, and round to look out over the bridge from your deck. (Coming
			// in to red's base, the same mirrored.)
			double m = p.kind() == Payloads.Cinematic.BRIDGE_RED ? -1 : 1;
			path = new Vec3[] {v(c, m * 170, 75, -150), v(c, m * 92, 38, -58), v(c, m * 82, 24, 34), v(c, m * 18, 18, 30), v(c, m * -12, 7, 8),
					v(c, m * -25, 5.5, 9.5)};
			look = new Vec3[] {v(c, 0, 0, 0), v(c, m * 41, 12, 0), v(c, m * 41, 12, 0), v(c, m * -12, 2, 0), v(c, m * -41, 9, 0), v(c, m * 6, 2, 0)};
		} else {
			// From far out, round the stadium, over the crowd, down to your pedestal.
			path = new Vec3[] {v(c, 240, 90, -170), v(c, 30, 75, -200), v(c, -95, 38, -40), v(c, -60, 14, 22), v(c, -27.5, 4.62, 0.5)};
			look = new Vec3[] {v(c, 0, 0, 0), v(c, 0, 8, 0), v(c, 60, 20, 30), v(c, 0, 6, 0), v(c, 20, 4.62, 0.5)};
		}
		title = p.title();
		subtitle = p.subtitle();
		length = Math.max(20, p.ticks() - 4);
		start = mc.level.getGameTime();
		if (cam == null || cam.level() != mc.level) {
			cam = new ArmorStand(EntityType.ARMOR_STAND, mc.level);
			cam.setInvisible(true);
			cam.setNoGravity(true);
		}
		if (!active) before = mc.options.getCameraType();
		mc.options.setCameraType(CameraType.FIRST_PERSON);
		faded = false;
		place(0, true);
		mc.setCameraEntity(cam);
		active = true;
	}

	private static Vec3 v(Vec3 c, double x, double y, double z) {
		return c.add(x, y, z);
	}

	public static void stop() {
		if (!active) return;
		active = false;
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null) mc.setCameraEntity(mc.player);
		if (before != null) mc.options.setCameraType(before);
		before = null;
	}

	public static void tick(Minecraft mc) {
		if (!active) return;
		if (mc.level == null || mc.player == null || cam == null || cam.level() != mc.level) {
			stop();
			return;
		}
		if (mc.options.keyJump.isDown() && mc.screen == null) {
			stop();
			return;
		}
		float t = (mc.level.getGameTime() - start + 1) / (float) length;
		// A quick fade to black at the end, so the cut back to you doesn't jump.
		if (!faded && mc.level.getGameTime() - start + 1 >= length - 8) {
			faded = true;
			Fade.start(8, 3, 12);
		}
		if (t >= 1) {
			stop();
			return;
		}
		place(t, false);
	}

	/** Moves the camera to t (0 to 1) of the way along, eased in and out. */
	private static void place(float t, boolean first) {
		float e = t * t * (3 - 2 * t);
		Vec3 pos = spline(path, e), at = spline(look, Math.min(1, e * 1.05F));
		Vec3 d = at.subtract(pos);
		float yaw = (float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90;
		float pitch = (float) -(Mth.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * Mth.RAD_TO_DEG);
		double eye = cam.getEyeHeight();
		if (first) {
			cam.setPos(pos.x, pos.y - eye, pos.z);
			cam.xo = cam.getX();
			cam.yo = cam.getY();
			cam.zo = cam.getZ();
			cam.setYRot(yaw);
			cam.setXRot(pitch);
			cam.yRotO = yaw;
			cam.xRotO = pitch;
			head(yaw, yaw);
			return;
		}
		cam.xo = cam.getX();
		cam.yo = cam.getY();
		cam.zo = cam.getZ();
		cam.yRotO = cam.getYRot();
		cam.xRotO = cam.getXRot();
		cam.setPos(pos.x, pos.y - eye, pos.z);
		// Keep turning the short way round.
		cam.setYRot(cam.yRotO + Mth.wrapDegrees(yaw - cam.yRotO));
		cam.setXRot(pitch);
		head(cam.yRotO, cam.getYRot());
	}

	/** The camera looks where its head points: keep the head (and body) turned with it. */
	private static void head(float before, float now) {
		cam.yHeadRotO = before;
		cam.yHeadRot = now;
		cam.yBodyRotO = before;
		cam.yBodyRot = now;
	}

	/** A smooth curve through the points (Catmull-Rom), t from 0 to 1. */
	private static Vec3 spline(Vec3[] p, float t) {
		int n = p.length - 1;
		float f = Mth.clamp(t, 0, 1) * n;
		int i = Math.min(n - 1, (int) f);
		float u = f - i;
		Vec3 p0 = p[Math.max(0, i - 1)], p1 = p[i], p2 = p[i + 1], p3 = p[Math.min(n, i + 2)];
		double u2 = u * u, u3 = u2 * u;
		return new Vec3(cr(p0.x, p1.x, p2.x, p3.x, u, u2, u3), cr(p0.y, p1.y, p2.y, p3.y, u, u2, u3), cr(p0.z, p1.z, p2.z, p3.z, u, u2, u3));
	}

	private static double cr(double a, double b, double c, double d, double u, double u2, double u3) {
		return 0.5 * (2 * b + (-a + c) * u + (2 * a - 5 * b + 4 * c - d) * u2 + (-a + 3 * b - 3 * c + d) * u3);
	}

	/** Hides the hotbar, hearts, crosshair and the rest while the fly-round plays. */
	public static void hideHud() {
		Identifier[] hidden = {VanillaHudElements.CROSSHAIR, VanillaHudElements.HOTBAR, VanillaHudElements.ARMOR_BAR, VanillaHudElements.HEALTH_BAR,
				VanillaHudElements.FOOD_BAR, VanillaHudElements.AIR_BAR, VanillaHudElements.INFO_BAR, VanillaHudElements.EXPERIENCE_LEVEL,
				VanillaHudElements.HELD_ITEM_TOOLTIP, VanillaHudElements.STATUS_EFFECTS, VanillaHudElements.BOSS_BAR, VanillaHudElements.SCOREBOARD,
				VanillaHudElements.TITLE_AND_SUBTITLE, VanillaHudElements.OVERLAY_MESSAGE};
		for (Identifier id : hidden) {
			HudElementRegistry.replaceElement(id, old -> (g, delta) -> {
				if (!active) old.render(g, delta);
			});
		}
	}

	/** The letterbox and the title. */
	public static void render(GuiGraphics g, DeltaTracker delta) {
		if (!active) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return;
		float ticks = mc.level.getGameTime() - start + delta.getGameTimeDeltaPartialTick(true);
		float in = Mth.clamp(ticks / 12, 0, 1), out = Mth.clamp((length - ticks) / 12, 0, 1), bars = Math.min(in, out);
		int w = g.guiWidth(), h = g.guiHeight(), bar = (int) (h * 0.11F * bars);
		g.fill(0, 0, w, bar, 0xFF000000);
		g.fill(0, h - bar, w, h, 0xFF000000);
		// The title, in from 1.5 seconds and out at 7.
		float a = Math.min(Mth.clamp((ticks - 30) / 15, 0, 1), Mth.clamp((140 - ticks) / 15, 0, 1));
		if (a <= 0.02F) return;
		Font font = mc.font;
		int alpha = (int) (a * 255) << 24;
		g.pose().pushMatrix();
		g.pose().translate(w / 2f, h * 0.68F);
		g.pose().scale(3.2F, 3.2F);
		Component t = Component.literal(title);
		g.drawString(font, t, -font.width(t) / 2, -4, alpha | 0xFFFFFF, true);
		g.pose().popMatrix();
		Component s = Component.literal(subtitle);
		g.drawString(font, s, w / 2 - font.width(s) / 2, (int) (h * 0.68F) + 12, alpha | 0x9FD8FF, true);
		Component skip = Component.literal("Space to skip");
		g.drawString(font, skip, w - font.width(skip) - 8, h - bar + Math.max(2, (bar - 8) / 2), ((int) (bars * 160) << 24) | 0xA0A0A0, false);
	}
}
