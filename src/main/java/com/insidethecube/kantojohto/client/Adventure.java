package com.insidethecube.kantojohto.client;

import com.insidethecube.kantojohto.RouteOne;
import com.insidethecube.kantojohto.game.Net;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/**
 * The adventure on your screen: the fixed 2.5D camera that follows you and never turns, walking with the
 * keys in the camera's directions, and the A, B and Start buttons.
 */
public final class Adventure {
	private Adventure() {}

	public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(RouteOne.id("route_one"));
	/** A is the jump key (Space), so it follows your controls; START opens the menu. */
	public static KeyMapping START, TOGGLE, DIRECTION;

	/** On in a Route One save (the server says so), or switched on with the key to try it in any world. */
	private static boolean on;
	public static boolean inSave;
	/** North up (like the games) or a fixed diagonal (like Minecraft Dungeons). */
	public static boolean diagonal;
	/** How far back the camera sits (blocks), and where it's easing to. */
	public static double distance = 30, distanceTarget = 30;
	public static final double PITCH = 55, FOV = 34;
	/** Held still (a trainer has spotted you, or a text box is up). */
	public static int frozen;
	private static CameraType before;
	private static Vec3 follow;
	/** The camera as last set (x, y, z, yaw, pitch), for putting things on screen over the world. */
	public static double[] lastPose;

	public static void register() {
		START = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.kantojohto.start", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_ENTER, CATEGORY));
		TOGGLE = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.kantojohto.toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F7, CATEGORY));
		DIRECTION = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.kantojohto.direction", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F8, CATEGORY));
	}

	public static boolean active() {
		return on && Minecraft.getInstance().player != null;
	}

	public static void setOn(boolean value, boolean save) {
		inSave = save;
		if (on == value) return;
		on = value;
		Minecraft mc = Minecraft.getInstance();
		if (on) {
			before = mc.options.getCameraType();
			follow = null;
		} else if (before != null) {
			mc.options.setCameraType(before);
		}
	}

	public static void tick(Minecraft mc) {
		if (mc.player == null) {
			on = false;
			return;
		}
		while (TOGGLE.consumeClick()) {
			if (inSave) continue;
			CompoundTag t = new CompoundTag();
			t.putBoolean("on", !on);
			ClientPlayNetworking.send(new Net.Act("adventure", t));
		}
		while (DIRECTION.consumeClick()) diagonal = !diagonal;
		if (!active()) return;
		if (mc.options.getCameraType() != CameraType.THIRD_PERSON_BACK) mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
		if (frozen > 0) frozen--;
		distance += (distanceTarget - distance) * 0.2;
		if (mc.screen == null) {
			while (mc.options.keyJump.consumeClick()) talk(mc);
			while (START.consumeClick()) mc.setScreen(new MenuScreen());
		}
	}

	/** The camera's yaw: looking north (the games' view), or north-east. */
	public static float cameraYaw() {
		return diagonal ? 225F : 180F;
	}

	public static void zoom(double scroll) {
		distanceTarget = Math.max(16, Math.min(64, distanceTarget - scroll * 3));
	}

	/** Where the camera is and how it's turned: x, y, z, yaw, pitch. */
	public static double[] pose(Entity e, float partial) {
		if (BattleScene.active()) return lastPose = BattleScene.cameraPose(partial);
		Vec3 p = e.getPosition(partial).add(0, 0.9, 0);
		// Follows you closely, but smoothly.
		if (follow == null || follow.distanceToSqr(p) > 400) follow = p;
		follow = follow.lerp(p, 0.35);
		double yaw = Math.toRadians(cameraYaw()), pitch = Math.toRadians(PITCH);
		double lx = -Math.sin(yaw) * Math.cos(pitch), ly = -Math.sin(pitch), lz = Math.cos(yaw) * Math.cos(pitch);
		return lastPose = new double[] {follow.x - lx * distance, follow.y - ly * distance, follow.z - lz * distance, cameraYaw(), PITCH};
	}

	/** A: talk to whoever's in front of you (or close by). */
	private static void talk(Minecraft mc) {
		Entity best = null;
		double bestScore = 1e9;
		Vec3 me = mc.player.position();
		Vec3 look = Vec3.directionFromRotation(0, mc.player.getYRot());
		for (Entity e : mc.level.entitiesForRendering()) {
			if (!(e instanceof com.insidethecube.kantojohto.game.Npc)) continue;
			Vec3 to = e.position().subtract(me);
			double d = to.length();
			if (d > 3.2) continue;
			double facingBonus = to.normalize().dot(look);
			double score = d - facingBonus * 1.5;
			if (score < bestScore) {
				bestScore = score;
				best = e;
			}
		}
		if (best == null) return;
		CompoundTag t = new CompoundTag();
		t.putInt("npc", best.getId());
		ClientPlayNetworking.send(new Net.Act("talk", t));
	}

	/** Which way to walk for the keys held: screen up is the camera's forward. Returns the yaw, or NaN. */
	public static float walkYaw(boolean up, boolean down, boolean left, boolean right) {
		int sx = (right ? 1 : 0) - (left ? 1 : 0), sy = (up ? 1 : 0) - (down ? 1 : 0);
		if (sx == 0 && sy == 0) return Float.NaN;
		return cameraYaw() + (float) Math.toDegrees(Math.atan2(sx, sy));
	}
}
