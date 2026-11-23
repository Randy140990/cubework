package com.insidethecube.retromon.client;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/**
 * (0.53, for John's thumbnails and adverts: to be taken out later.) PHOTO MODE: the camera leaves you and goes where
 * you send it, with every look of the map as it is in the game, for high shots down a route or a town side on.
 * F6 (or OPTIONS, PHOTO MODE) goes in and out. WASD or the arrows move it across the map, SPACE and CTRL up and
 * down, Q and E turn it round, R and F tilt it, the wheel takes it in and out, Z and X narrow and widen the lens,
 * SHIFT for faster; H hides these words. You stay where you were; everything on screen but the world is put away
 * (F2 takes the picture, as ever). The map is drawn as far round you as Minecraft's render distance reaches.
 */
public final class Photo {
	public static boolean on;
	/** What it looks at (on the ground), the way it faces (Minecraft's yaw: 180 is north), its tilt, how far back, its lens. */
	private static double tx, ty, tz, yaw = 180, pitch = 40, dist = 60;
	public static double fov = 30;
	private static long last;
	private static boolean help = true;
	private static final Set<Integer> DOWN = new HashSet<>();
	private static final int[] KEYS = {GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_D, GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_LEFT,
			GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_Q, GLFW.GLFW_KEY_E, GLFW.GLFW_KEY_R, GLFW.GLFW_KEY_F, GLFW.GLFW_KEY_Z, GLFW.GLFW_KEY_X, GLFW.GLFW_KEY_H, GLFW.GLFW_KEY_SPACE,
			GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL, GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT, GLFW.GLFW_KEY_PAGE_UP, GLFW.GLFW_KEY_PAGE_DOWN,
			GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_TAB};

	private Photo() {}

	/** In or out. Going in, it starts where the game's camera is (looking at you), a little higher and further back. */
	public static void set(boolean value) {
		if (on == value) return;
		on = value;
		DOWN.clear();
		last = 0;
		if (on) {
			Minecraft mc = Minecraft.getInstance();
			Vec3 at = mc.player == null ? Vec3.ZERO : mc.player.position();
			tx = at.x;
			ty = at.y;
			tz = at.z;
			yaw = 180;
			pitch = 42;
			dist = 70;
			fov = 30;
			help = true;
		}
	}

	/** A key out in the world while it's on: kept for the camera (true), so you don't walk or open anything. */
	public static boolean key(KeyEvent e, int action) {
		Minecraft mc = Minecraft.getInstance();
		int k = e.key();
		if (k == GLFW.GLFW_KEY_F6 && Adventure.active() && mc.screen == null) {
			if (action == GLFW.GLFW_PRESS) set(!on);
			return true;
		}
		if (!on) return false;
		if (mc.screen != null) {
			DOWN.clear();
			return false;
		}
		for (int c : KEYS)
			if (c == k) {
				if (action == GLFW.GLFW_RELEASE) DOWN.remove(k);
				else {
					if (action == GLFW.GLFW_PRESS && k == GLFW.GLFW_KEY_H) help = !help;
					DOWN.add(k);
				}
				return true;
			}
		return false;
	}

	public static void scroll(double y) {
		dist = Math.max(4, Math.min(900, dist * Math.pow(0.88, y)));
	}

	private static boolean down(int... keys) {
		for (int k : keys) if (DOWN.contains(k)) return true;
		return false;
	}

	/** The camera now: x, y, z, yaw, pitch (moved by the keys held, by the time since last frame). */
	public static double[] pose() {
		long now = System.nanoTime();
		double dt = last == 0 ? 0.016 : Math.max(0, Math.min(0.1, (now - last) / 1e9));
		last = now;
		if (Minecraft.getInstance().screen != null) DOWN.clear();
		double fast = down(GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT) ? 3 : 1;
		double step = Math.max(6, dist) * 0.7 * dt * fast;
		double r = Math.toRadians(yaw), fx = -Math.sin(r), fz = Math.cos(r), rx = -Math.sin(r + Math.PI / 2), rz = Math.cos(r + Math.PI / 2);
		double mf = (down(GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_UP) ? 1 : 0) - (down(GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_DOWN) ? 1 : 0);
		double ms = (down(GLFW.GLFW_KEY_D, GLFW.GLFW_KEY_RIGHT) ? 1 : 0) - (down(GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_LEFT) ? 1 : 0);
		tx += (fx * mf + rx * ms) * step;
		tz += (fz * mf + rz * ms) * step;
		ty += ((down(GLFW.GLFW_KEY_SPACE, GLFW.GLFW_KEY_PAGE_UP) ? 1 : 0) - (down(GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL, GLFW.GLFW_KEY_PAGE_DOWN) ? 1 : 0))
				* step * 0.6;
		yaw += ((down(GLFW.GLFW_KEY_E) ? 1 : 0) - (down(GLFW.GLFW_KEY_Q) ? 1 : 0)) * 55 * dt * fast;
		pitch = Math.max(2, Math.min(89, pitch + ((down(GLFW.GLFW_KEY_R) ? 1 : 0) - (down(GLFW.GLFW_KEY_F) ? 1 : 0)) * 30 * dt * fast));
		fov = Math.max(5, Math.min(90, fov + ((down(GLFW.GLFW_KEY_X) ? 1 : 0) - (down(GLFW.GLFW_KEY_Z) ? 1 : 0)) * 18 * dt * fast));
		double p = Math.toRadians(pitch), back = dist * Math.cos(p), up = dist * Math.sin(p);
		return new double[] {tx - fx * back, ty + up, tz - fz * back, yaw, pitch};
	}

	/** Its few words, top left (H hides them). */
	public static void draw(GuiGraphics g) {
		if (!on || !help) return;
		V.fit(g.guiWidth(), g.guiHeight());
		V.panel(g, 22, 22, 720, 118);
		V.text(g, "PHOTO MODE", 44, 34, 22, V.INK, false);
		V.text(g, "WASD move   SPACE / CTRL up and down   Q / E turn   R / F tilt", 44, 66, 17, V.INK, false);
		V.text(g, "Wheel in and out   Z / X lens   SHIFT faster   H hide this   F6 leave", 44, 92, 17, V.INK, false);
	}
}
