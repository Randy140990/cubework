package com.insidethecube.retromon.client;

import java.util.HashSet;
import java.util.Set;

import com.insidethecube.retromon.game.Net;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/**
 * (0.53, for John's thumbnails and adverts: to be taken out later.) PHOTO MODE: the camera leaves you and goes where
 * you send it, with every look of the map as it is in the game, for high shots down a route or a town side on.
 * F6 (or OPTIONS, PHOTO MODE) goes in and out. WASD or the arrows move it across the map, SPACE and SHIFT up and
 * down (as Minecraft's flying; 15:17), Q and E turn it round, R and F tilt it, the wheel takes it in and out, Z and X narrow
 * and widen the lens, CTRL for faster; H hides these words. You stay where you were; everything on screen but the world is put away
 * (F2 takes the picture, as ever). (0.54) You're kept under the camera on the server (unseen), so the map is drawn
 * round where it looks, as far as Minecraft's render distance reaches; on leaving you're back where you were.
 * (0.59.7, John 15:35: "the ability in f6 to get an image too and also do a 2k output ... location photos for the map")
 * P takes a picture at 2560 by 1440 whatever the window's size: for a moment the game draws at that size (as
 * Minecraft's own panorama pictures do), then it's back. It's saved in the game's folder (retromon_photos/, by
 * place and time), and becomes that place's picture on the TOWN MAP.
 */
public final class Photo {
	public static boolean on;
	/** What it looks at (on the ground), the way it faces (Minecraft's yaw: 180 is north), its tilt, how far back, its lens. */
	private static double tx, ty, tz, yaw = 180, pitch = 40, dist = 60;
	public static double fov = 30;
	private static long last;
	private static boolean help = true;
	/** The 2K picture: 0 none; 1 drawing at 2K (taken at the next tick); the window's own size to go back to. */
	private static int shooting, keepW, keepH, shootTicks;
	private static boolean keepHidden;
	/** What the last picture said, and when (ms). */
	private static String said = "";
	private static long saidAt;
	public static final int SHOT_W = 2560, SHOT_H = 1440;
	private static final Set<Integer> DOWN = new HashSet<>();
	/** Where the server last put you (under the camera), and when (nanoseconds). */
	private static double sentX = Double.NaN, sentZ = Double.NaN;
	private static long sentAt;
	private static final int[] KEYS = {GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_D, GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_LEFT,
			GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_Q, GLFW.GLFW_KEY_E, GLFW.GLFW_KEY_R, GLFW.GLFW_KEY_F, GLFW.GLFW_KEY_Z, GLFW.GLFW_KEY_X, GLFW.GLFW_KEY_H, GLFW.GLFW_KEY_SPACE,
			GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL, GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT, GLFW.GLFW_KEY_PAGE_UP, GLFW.GLFW_KEY_PAGE_DOWN,
			GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_TAB, GLFW.GLFW_KEY_P};

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
			sentX = sentZ = Double.NaN;
			send(true);
		} else send(false);
	}

	/** Tells the server: on (and where the camera looks), or off. */
	private static void send(boolean value) {
		if (Minecraft.getInstance().getConnection() == null) return;
		CompoundTag t = new CompoundTag();
		t.putBoolean("on", value);
		if (value) {
			t.putDouble("x", tx);
			t.putDouble("y", ty + 2);
			t.putDouble("z", tz);
			sentX = tx;
			sentZ = tz;
			sentAt = System.nanoTime();
		}
		ClientPlayNetworking.send(new Net.Act("photo", t));
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
					if (action == GLFW.GLFW_PRESS && k == GLFW.GLFW_KEY_P) shoot();
					DOWN.add(k);
				}
				return true;
			}
		return false;
	}

	/** The place the camera's looking at (the marked place round it, or the nearest within 64 blocks), or "kanto". */
	private static String placeUnder() {
		String best = "kanto";
		double bestD = 64;
		for (ClientState.Zone z : ClientState.zones) {
			double dx = Math.max(0, Math.max(z.x1() - tx, tx - (z.x2() + 1))), dz = Math.max(0, Math.max(z.z1() - tz, tz - (z.z2() + 1)));
			double d = Math.hypot(dx, dz);
			if (d < bestD || d == 0 && bestD == 0 && (z.x2() - z.x1()) * (z.z2() - z.z1()) < 1e9) {
				bestD = d;
				best = z.place();
				if (d == 0) break;
			}
		}
		return best;
	}

	/** P: the game drawn at 2K for a moment (the window keeps its size), and the picture taken at the next tick. */
	private static void shoot() {
		if (shooting != 0) return;
		Minecraft mc = Minecraft.getInstance();
		try {
			com.mojang.blaze3d.platform.Window w = mc.getWindow();
			keepW = w.getWidth();
			keepH = w.getHeight();
			keepHidden = mc.options.hideGui;
			mc.options.hideGui = true;
			w.setWidth(SHOT_W);
			w.setHeight(SHOT_H);
			mc.resizeDisplay();
			shooting = 1;
			shootTicks = 0;
		} catch (Throwable e) {
			com.insidethecube.retromon.Retromon.LOG.warn("Retromon photo: couldn't draw at 2K, taking it at the window's size", e);
			back(mc);
			shooting = 1;
			shootTicks = 0;
		}
	}

	private static void back(Minecraft mc) {
		try {
			com.mojang.blaze3d.platform.Window w = mc.getWindow();
			if (keepW > 0 && (w.getWidth() != keepW || w.getHeight() != keepH)) {
				w.setWidth(keepW);
				w.setHeight(keepH);
				mc.resizeDisplay();
			}
		} catch (Throwable e) {
			com.insidethecube.retromon.Retromon.LOG.warn("Retromon photo: couldn't put the window's size back", e);
		}
		mc.options.hideGui = keepHidden;
	}

	/** Each tick: once a frame or two has been drawn at 2K, the picture's taken and the window's size put back. */
	public static void tick(Minecraft mc) {
		if (shooting == 0) return;
		if (++shootTicks < 2) return;
		shooting = 0;
		String place = placeUnder();
		String when = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
		java.nio.file.Path dir = mc.gameDirectory.toPath().resolve("retromon_photos");
		try {
			net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget(), shot -> java.util.concurrent.CompletableFuture.runAsync(() -> {
				int sw = shot.getWidth(), sh = shot.getHeight();
				try (shot; com.mojang.blaze3d.platform.NativeImage out = sw == SHOT_W && sh == SHOT_H ? null : SlotSnapshot.fit(shot, SHOT_W, SHOT_H)) {
					java.nio.file.Files.createDirectories(dir);
					java.nio.file.Path file = dir.resolve(place + "_" + when + ".png");
					(out == null ? shot : out).writeToFile(file);
					Postcards.keep(place, out == null ? shot : out);
					String msg = "Saved " + SHOT_W + " x " + SHOT_H + (out == null ? "" : " (drawn at " + sw + " x " + sh + ")") + ": " + file.getFileName()
							+ ", and on the TOWN MAP for " + place.replace('_', ' ').toUpperCase();
					mc.execute(() -> {
						said = msg;
						saidAt = System.currentTimeMillis();
					});
					com.insidethecube.retromon.Retromon.LOG.info("Retromon photo: {}", file);
				} catch (Exception e) {
					com.insidethecube.retromon.Retromon.LOG.warn("Retromon photo: couldn't save {}", file(dir, place, when), e);
				}
			}));
		} catch (Exception e) {
			com.insidethecube.retromon.Retromon.LOG.warn("Retromon photo: couldn't take the picture", e);
		}
		back(mc);
	}

	private static String file(java.nio.file.Path dir, String place, String when) {
		return dir.resolve(place + "_" + when + ".png").toString();
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
		double fast = down(GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL) ? 3 : 1;
		double step = Math.max(6, dist) * 0.7 * dt * fast;
		double r = Math.toRadians(yaw), fx = -Math.sin(r), fz = Math.cos(r), rx = -Math.sin(r + Math.PI / 2), rz = Math.cos(r + Math.PI / 2);
		double mf = (down(GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_UP) ? 1 : 0) - (down(GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_DOWN) ? 1 : 0);
		double ms = (down(GLFW.GLFW_KEY_D, GLFW.GLFW_KEY_RIGHT) ? 1 : 0) - (down(GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_LEFT) ? 1 : 0);
		tx += (fx * mf + rx * ms) * step;
		tz += (fz * mf + rz * ms) * step;
		ty += ((down(GLFW.GLFW_KEY_SPACE, GLFW.GLFW_KEY_PAGE_UP) ? 1 : 0) - (down(GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT, GLFW.GLFW_KEY_PAGE_DOWN) ? 1 : 0))
				* step * 0.6;
		yaw += ((down(GLFW.GLFW_KEY_E) ? 1 : 0) - (down(GLFW.GLFW_KEY_Q) ? 1 : 0)) * 55 * dt * fast;
		pitch = Math.max(2, Math.min(89, pitch + ((down(GLFW.GLFW_KEY_R) ? 1 : 0) - (down(GLFW.GLFW_KEY_F) ? 1 : 0)) * 30 * dt * fast));
		fov = Math.max(5, Math.min(90, fov + ((down(GLFW.GLFW_KEY_X) ? 1 : 0) - (down(GLFW.GLFW_KEY_Z) ? 1 : 0)) * 18 * dt * fast));
		// Moved on a way (or a while since): the server puts you under the camera again, so the map's drawn there.
		if (Double.isNaN(sentX) || Math.hypot(tx - sentX, tz - sentZ) > 12 && now - sentAt > 250_000_000L) send(true);
		double p = Math.toRadians(pitch), back = dist * Math.cos(p), up = dist * Math.sin(p);
		return new double[] {tx - fx * back, ty + up, tz - fz * back, yaw, pitch};
	}

	/** Its few words, top left (H hides them). */
	public static void draw(GuiGraphics g) {
		if (!on || shooting != 0) return;
		V.fit(g.guiWidth(), g.guiHeight());
		if (System.currentTimeMillis() - saidAt < 4000 && !said.isEmpty()) {
			float w = Math.min(1236, V.width(said, 16, true) + 40);
			V.pill(g, 640 - w / 2, 660, w, 36, V.GREEN);
			V.centred(g, said, 640, 669, 16, V.WHITE, true);
		}
		if (!help) return;
		V.panel(g, 22, 22, 720, 118);
		V.text(g, "PHOTO MODE", 44, 34, 22, V.INK, false);
		V.text(g, "WASD move   SPACE / SHIFT up and down   Q / E turn   R / F tilt", 44, 66, 17, V.INK, false);
		V.text(g, "Wheel in and out   Z / X lens   CTRL faster   P picture (2K)   H hide   F6 leave", 44, 92, 17, V.INK, false);
	}
}
