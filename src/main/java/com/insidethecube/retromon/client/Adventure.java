package com.insidethecube.retromon.client;

import com.insidethecube.retromon.Retromon;
import com.insidethecube.retromon.game.Net;
import com.insidethecube.retromon.game.Npc;
import com.insidethecube.retromon.game.WorldMon;
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
 * The adventure on your screen: the fixed top-down camera that follows you and never turns (north up, as in
 * the games), walking with the keys in the camera's directions, and the A, START and speed buttons.
 */
public final class Adventure {
	private Adventure() {}

	public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Retromon.id("retromon"));
	/** A is the jump key (Space), so it follows your controls. START opens the menu; SPEED speeds the game up. */
	public static KeyMapping START, SPEED, TOGGLE;

	/** On in a Retromon save (the game says so), or switched on with the key to try it in any world. */
	private static boolean on;
	public static boolean inSave;
	/** How far back the camera sits (blocks), and where it's easing to. Close to the games' view, a little room either way. */
	public static double distance = 30, distanceTarget = 30;
	public static final double PITCH = 55, FOV = 34, NEAR = 24, FAR = 40;
	/** The camera always looks north, as the games do. */
	public static final float YAW = 180F;
	/** Held still (a trainer has spotted you, or a text box is up). */
	public static int frozen;
	/** The game's speed (1, 2 or 3 times). */
	public static int speed = 1;
	private static CameraType before;
	private static Vec3 follow;
	private static double smoothY = Double.NaN;
	/** Who you're talking to (the camera closes in on the two of you), or -1; and how far in it's come (0 to 1). */
	private static int focusId = -1;
	private static double focusIn;

	public static void focus(int entityId) {
		focusId = entityId;
	}

	/** The camera as last set (x, y, z, yaw, pitch), for putting things on screen over the world. */
	public static double[] lastPose;
	/** Something's between the camera and you (your outline shows through it). */
	public static boolean hidden;

	public static void register() {
		START = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.retromon.start", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_ENTER, CATEGORY));
		SPEED = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.retromon.speed", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY));
		TOGGLE = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.retromon.toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F7, CATEGORY));
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
			smoothY = Double.NaN;
		} else {
			if (before != null) mc.options.setCameraType(before);
			speed = 1;
			Music.stop();
		}
	}

	public static void tick(Minecraft mc) {
		if (mc.player == null) {
			on = false;
			speed = 1;
			return;
		}
		while (TOGGLE.consumeClick()) {
			if (inSave) continue;
			CompoundTag t = new CompoundTag();
			t.putBoolean("on", !on);
			ClientPlayNetworking.send(new Net.Act("adventure", t));
		}
		if (!active()) return;
		if (mc.options.getCameraType() != CameraType.THIRD_PERSON_BACK) mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
		if (frozen > 0) frozen--;
		distance += (distanceTarget - distance) * 0.2;
		while (SPEED.consumeClick()) {
			speed = speed >= 4 ? 1 : speed + 1;
			CompoundTag t = new CompoundTag();
			t.putInt("times", speed);
			ClientPlayNetworking.send(new Net.Act("speed", t));
		}
		hidden = !BattleScene.active() && behind(mc);
		if (mc.screen == null) {
			while (mc.options.keyJump.consumeClick()) talk(mc);
			while (START.consumeClick()) mc.setScreen(new MenuScreen());
		}
	}

	public static void zoom(double scroll) {
		distanceTarget = Math.max(NEAR, Math.min(FAR, distanceTarget - scroll * 2));
	}

	/** Where the camera is and how it's turned: x, y, z, yaw, pitch. */
	public static double[] pose(Entity e, float partial) {
		if (BattleScene.active()) return lastPose = BattleScene.cameraPose(partial);
		Vec3 p = e.getPosition(partial);
		// Across the ground it follows you exactly; up and down (steps, slabs) it eases, so it doesn't bob.
		if (Double.isNaN(smoothY) || Math.abs(smoothY - p.y) > 6) smoothY = p.y;
		smoothY += (p.y - smoothY) * 0.08;
		follow = new Vec3(p.x, smoothY + 0.9, p.z);
		// Talking to someone: ease in towards the middle of the two of you, closer and a little lower.
		Entity other = focusId >= 0 && e.level() != null ? e.level().getEntity(focusId) : null;
		focusIn += ((other != null ? 1 : 0) - focusIn) * 0.12;
		Vec3 centre = follow;
		if (other != null) centre = follow.lerp(other.getPosition(partial).add(0, 0.9, 0), 0.5);
		else focusId = -1;
		Vec3 target = follow.lerp(centre, focusIn);
		double dist = distance + (13 - distance) * focusIn, pitchDeg = PITCH - 12 * focusIn;
		double yaw = Math.toRadians(YAW), pitch = Math.toRadians(pitchDeg);
		double lx = -Math.sin(yaw) * Math.cos(pitch), ly = -Math.sin(pitch), lz = Math.cos(yaw) * Math.cos(pitch);
		return lastPose = new double[] {target.x - lx * dist, target.y - ly * dist, target.z - lz * dist, YAW, (float) pitchDeg};
	}

	/** A: talk to whoever's in front of you (or close by), person or Pokémon. */
	private static void talk(Minecraft mc) {
		Entity best = null;
		double bestScore = 1e9;
		Vec3 me = mc.player.position();
		Vec3 look = Vec3.directionFromRotation(0, mc.player.getYRot());
		for (Entity e : mc.level.entitiesForRendering()) {
			if (!(e instanceof Npc) && !(e instanceof WorldMon)) continue;
			Vec3 to = e.position().subtract(me);
			double d = to.length();
			if (d > 3.5) continue;
			double facingBonus = to.normalize().dot(look);
			double score = d - facingBonus * 1.5;
			if (score < bestScore) {
				bestScore = score;
				best = e;
			}
		}
		if (best == null) {
			readSign(mc);
			return;
		}
		CompoundTag t = new CompoundTag();
		t.putInt("npc", best.getId());
		ClientPlayNetworking.send(new Net.Act("talk", t));
	}

	/** Whether a block stands between the camera and you (checked at your middle and your head). */
	private static boolean behind(Minecraft mc) {
		double[] cam = lastPose;
		if (cam == null || mc.level == null) return false;
		Vec3 from = new Vec3(cam[0], cam[1], cam[2]);
		for (double up : new double[] {0.9, 1.6}) {
			Vec3 to = mc.player.position().add(0, up, 0);
			var hit = mc.level.clip(new net.minecraft.world.level.ClipContext(from, to, net.minecraft.world.level.ClipContext.Block.VISUAL,
					net.minecraft.world.level.ClipContext.Fluid.NONE, mc.player));
			if (hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS || hit.getLocation().distanceToSqr(to) < 0.6) return false;
		}
		return true;
	}

	/** The signs of the games' towns and routes (read from a shelf standing in for a sign). */
	private static final java.util.Map<String, String> SIGNS = java.util.Map.ofEntries(java.util.Map.entry("PALLET TOWN", "Shades of your journey await!"),
			java.util.Map.entry("ROUTE 1", "PALLET TOWN - VIRIDIAN CITY"), java.util.Map.entry("VIRIDIAN CITY", "The Eternally Green Paradise"),
			java.util.Map.entry("ROUTE 22", "POKéMON LEAGUE Front Gate"), java.util.Map.entry("ROUTE 2", "VIRIDIAN CITY - PEWTER CITY"),
			java.util.Map.entry("PEWTER CITY", "A Stone Gray City"), java.util.Map.entry("CERULEAN CITY", "A Mysterious, Blue Aura Surrounds It"),
			java.util.Map.entry("VERMILION CITY", "The Port of Exquisite Sunsets"), java.util.Map.entry("LAVENDER TOWN", "The Noble Purple Town"),
			java.util.Map.entry("CELADON CITY", "The City of Rainbow Dreams"), java.util.Map.entry("FUCHSIA CITY", "Behold! It's Passion Pink!"),
			java.util.Map.entry("SAFFRON CITY", "Shining, Golden Land of Commerce"), java.util.Map.entry("CINNABAR ISLAND", "The Fiery Town of Burning Desire"),
			java.util.Map.entry("INDIGO PLATEAU", "The Ultimate Goal of Trainers!"));

	/** A: read the sign in front of you (its words), or a shelf standing in for one (the place's sign). */
	private static void readSign(Minecraft mc) {
		Vec3 look = Vec3.directionFromRotation(0, mc.player.getYRot());
		for (double d = 0.5; d <= 2.1; d += 0.4) {
			for (int dy = 0; dy <= 1; dy++) {
				net.minecraft.core.BlockPos at = net.minecraft.core.BlockPos.containing(mc.player.getX() + look.x * d, mc.player.getY() + dy + 0.2,
						mc.player.getZ() + look.z * d);
				var state = mc.level.getBlockState(at);
				java.util.List<String> lines = new java.util.ArrayList<>();
				if (mc.level.getBlockEntity(at) instanceof net.minecraft.world.level.block.entity.SignBlockEntity sign) {
					StringBuilder b = new StringBuilder();
					for (var text : new net.minecraft.world.level.block.entity.SignText[] {sign.getFrontText(), sign.getBackText()}) {
						for (int i = 0; i < 4; i++) {
							String l = text.getMessage(i, false).getString().strip();
							if (!l.isEmpty()) b.append(b.isEmpty() ? "" : " ").append(l);
						}
						if (!b.isEmpty()) break;
					}
					if (!b.isEmpty()) lines.add(b.toString());
				} else if (!net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().endsWith("shelf")) continue;
				if (lines.isEmpty()) {
					// A shelf standing in for a sign (or a blank sign): the game reads out the games' own sign for that spot.
					Sounds.press();
					CompoundTag t = new CompoundTag();
					t.putInt("x", at.getX());
					t.putInt("z", at.getZ());
					ClientPlayNetworking.send(new Net.Act("read", t));
					return;
				}
				Sounds.press();
				mc.setScreen(new DialogueScreen(lines, "sign", -1, new CompoundTag()));
				return;
			}
		}
	}

	/** Which way to walk for the keys held: screen up is north. Returns the yaw, or NaN. */
	public static float walkYaw(boolean up, boolean down, boolean left, boolean right) {
		int sx = (right ? 1 : 0) - (left ? 1 : 0), sy = (up ? 1 : 0) - (down ? 1 : 0);
		if (sx == 0 && sy == 0) return Float.NaN;
		return YAW + (float) Math.toDegrees(Math.atan2(sx, sy));
	}
}
