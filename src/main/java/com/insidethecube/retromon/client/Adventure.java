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
	public static final double PITCH = 52, FOV = 34, NEAR = 24, FAR = 40;
	/** The camera always looks north, as the games do. */
	public static final float YAW = 180F;
	/** Held still (a trainer has spotted you, or a text box is up). */
	public static int frozen;
	/** Walking after someone for a few ticks more: where to (x, z). */
	public static int followTicks;
	public static double followX, followZ;
	/** The game's speed (1, 2 or 3 times). */
	public static int speed = 1;
	private static CameraType before;
	private static Vec3 follow;
	private static double smoothY = Double.NaN;
	/** Who you're talking to (the camera closes in on the two of you), or -1; and how far in it's come (0 to 1). */
	private static int focusId = -1;
	private static double focusIn;

	/** Ticks till the talking camera lets go (it holds a moment, so one conversation straight after another doesn't bounce). */
	private static int focusRelease;

	/** The nurse's machine is healing: the camera eases up to see its big screen (and back after). */
	public static boolean healView;
	private static double healIn;

	public static void focus(int entityId) {
		if (entityId < 0) {
			if (focusId >= 0) focusRelease = 12;
			return;
		}
		focusId = entityId;
		focusRelease = 0;
	}

	/** The camera as last set (x, y, z, yaw, pitch), for putting things on screen over the world. */
	public static double[] lastPose;
	/** Something's between the camera and you (your outline shows through it). */
	public static boolean hidden;

	public static void register() {
		// E, as Minecraft's inventory key (a new name, so an old ENTER setting isn't carried over); ENTER still works.
		START = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.retromon.menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_E, CATEGORY));
		// TAB, not R: shader packs reload on R. (A new name, so the old R setting isn't carried over.)
		SPEED = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.retromon.fast", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_TAB, CATEGORY));
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
		if (focusRelease > 0 && --focusRelease == 0) focusId = -1;
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
		if (frozen > 0 && --frozen == 0 && mc.screen == null) focus(-1);
		if (followTicks > 0) followTicks--;
		distance += (distanceTarget - distance) * 0.2;
		while (SPEED.consumeClick()) cycleSpeed();
		hidden = !BattleScene.active() && behind(mc);
		updatePrompt(mc);
		if (mc.screen == null && !Device.busy()) {
			while (mc.options.keyJump.consumeClick()) talk(mc);
			// START: you take your Pokédex out, and the menu comes up on it.
			while (START.consumeClick()) Device.open(MenuScreen::new);
		} else if (mc.screen == null) {
			while (mc.options.keyJump.consumeClick()) {
			}
			while (START.consumeClick()) {
			}
		}
	}

	/**
	 * Whether a key is kept from Minecraft and other mods: out in the world in the Retromon game, everything but
	 * the game's own keys (walking, running, SPACE, START, SPEED, the Retromon switch) and Esc, F1, F2 and F11.
	 * With one of our screens open, keys go to it as ever.
	 */
	public static boolean blocks(net.minecraft.client.input.KeyEvent e) {
		Minecraft mc = Minecraft.getInstance();
		if (!active() || mc.player == null) return false;
		if (mc.screen != null) return false;
		int k = e.key();
		if (k == GLFW.GLFW_KEY_ESCAPE || k == GLFW.GLFW_KEY_F1 || k == GLFW.GLFW_KEY_F2 || k == GLFW.GLFW_KEY_F11) return false;
		// Lightlens (recording and photos: F10, F12 and its GIF key) works in the game too, as do F10 and F12 for any
		// recording mod: Minecraft has nothing on them.
		if (k == GLFW.GLFW_KEY_F10 || k == GLFW.GLFW_KEY_F12) return false;
		for (KeyMapping m : mc.options.keyMappings)
			if (m.getName().startsWith("key.lightlens") && m.matches(e)) return false;
		var o = mc.options;
		for (KeyMapping m : new KeyMapping[] {o.keyUp, o.keyDown, o.keyLeft, o.keyRight, o.keyJump, o.keyShift, o.keySprint, START, SPEED, TOGGLE, o.keyScreenshot,
				o.keyFullscreen})
			if (m != null && m.matches(e)) return false;
		// The arrow keys walk too.
		return k != GLFW.GLFW_KEY_UP && k != GLFW.GLFW_KEY_DOWN && k != GLFW.GLFW_KEY_LEFT && k != GLFW.GLFW_KEY_RIGHT;
	}

	/**
	 * The menu key (E, as Minecraft's inventory; ENTER too) out in the world: it opens the game's menu, and nothing
	 * else sees it (so E never opens the inventory). True when it was the menu key.
	 */
	public static boolean menuKey(net.minecraft.client.input.KeyEvent e, int action) {
		Minecraft mc = Minecraft.getInstance();
		if (!active() || mc.player == null || mc.screen != null) return false;
		boolean menu = (START != null && START.matches(e)) || e.key() == GLFW.GLFW_KEY_ENTER || e.key() == GLFW.GLFW_KEY_KP_ENTER;
		if (!menu) return false;
		if (action == GLFW.GLFW_PRESS && !Device.busy()) Device.open(MenuScreen::new);
		return true;
	}

	/** The game's speed: 1x, 2x, 3x, 4x and round again (the world, and battles with it). */
	public static void cycleSpeed() {
		speed = speed >= 4 ? 1 : speed + 1;
		CompoundTag t = new CompoundTag();
		t.putInt("times", speed);
		ClientPlayNetworking.send(new Net.Act("speed", t));
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
		Vec3 you = new Vec3(p.x, smoothY + 0.9, p.z);
		follow = you;
		// Inside a building the camera holds still on the whole room (as the Interior Viewer shows it), not on you.
		com.insidethecube.retromon.game.Interiors.Room room = com.insidethecube.retromon.game.Interiors.roomOf(e);
		// (A big one, the forest, you walk about in: the camera follows you there, as outside.)
		if (room != null && room.big()) room = null;
		// Inside, closer in than the whole room (as the Interior Viewer's game camera): seven tenths as far back (15 at
		// least), following you up and down the room, and across it when it's wider than the view, held in from the
		// walls so the room fills the screen.
		double roomDist = 0;
		if (room != null) {
			roomDist = Math.max(15, room.camDist() * 0.72);
			double vis = 2 * roomDist * Math.tan(Math.toRadians(FOV / 2)) * (16 / 9.0) * 0.92, hx = Math.min(room.w() / 2, vis / 2 - 0.5);
			double lx = room.w() <= vis - 1 ? room.w() / 2 : Math.max(hx, Math.min(room.w() - hx, p.x - room.ox()));
			double lz = Math.max(Math.min(room.d() / 2, 4.5), Math.min(Math.max(room.d() / 2, room.d() - 4.0), p.z - room.oz() - 0.6));
			follow = new Vec3(room.ox() + lx, room.oy() + 0.9, room.oz() + lz);
		}
		// Talking to someone: ease in towards the middle of the two of you, closer and a little lower.
		Entity other = focusId >= 0 && e.level() != null ? e.level().getEntity(focusId) : null;
		// Eased by time, not by frames (at a high frame rate it snapped in and out).
		long now = System.nanoTime();
		double dt = lastFrame == 0 ? 0.016 : Math.max(0, Math.min(0.1, (now - lastFrame) / 1e9));
		lastFrame = now;
		double ease = 1 - Math.exp(-dt * 3.2), easeRoom = 1 - Math.exp(-dt * 2.4);
		focusIn += ((other != null ? 1 : 0) - focusIn) * ease;
		// While talking, between the two of you; afterwards it eases back from there (it jumped back before).
		if (other != null) focusCentre = you.lerp(other.getPosition(partial).add(0, 0.9, 0), 0.5);
		else focusId = -1;
		Vec3 centre = focusCentre != null && focusIn > 0.002 ? focusCentre : follow;
		if (focusIn <= 0.002) focusCentre = null;
		Vec3 target = follow.lerp(centre, focusIn);
		// Closing in only as far as the view stays clear: if the closer, lower camera would be inside a roof
		// or a wall behind them, it stays up and back (no clipping into buildings). Inside a building the room's
		// open to the camera above, so it just comes in (testing there made it jitter in and out by a wall).
		if (room != null) focusRoom = 1;
		else if (focusIn > 0.01 && e.level() != null) {
			double[] full = camAt(target, 13, PITCH - 12);
			var hit = e.level().clip(new net.minecraft.world.level.ClipContext(target, new Vec3(full[0], full[1], full[2]),
					net.minecraft.world.level.ClipContext.Block.VISUAL, net.minecraft.world.level.ClipContext.Fluid.NONE, e));
			boolean clear = hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS;
			focusRoom += ((clear ? 1 : 0) - focusRoom) * easeRoom;
		} else focusRoom = 1;
		double k = focusIn * focusRoom;
		double far = room != null ? roomDist : distance;
		// (In, never back: in a small room, where the camera's already near, it only comes in a little, and tips up
		// less, so it doesn't end up looking at the floor.)
		double near = room != null ? Math.min(13, far * 0.8) : 13;
		double dist = far + (near - far) * k, pitchDeg = PITCH - (room != null ? 7 : 12) * k;
		// Healing: up and in towards the machine's screen on the back wall (and down again after).
		healIn += ((healView && room != null ? 1 : 0) - healIn) * ease;
		if (healIn > 0.002) {
			target = target.add(0, 1.9 * healIn, -2.0 * healIn);
			pitchDeg -= 12 * healIn;
		}
		// (Where the closer camera would be in something, it stays up and back as it was: focusRoom. It used to pull
		// in in front of whatever was in the way, which put it almost on the ground by a tree in VIRIDIAN.)
		double[] at = camAt(target, dist, pitchDeg);
		return lastPose = new double[] {at[0], at[1], at[2], YAW, (float) pitchDeg};
	}

	/** How far the talking camera has room to close in (0 to 1); and where it closed in on. */
	private static double focusRoom = 1;
	private static Vec3 focusCentre;
	private static long lastFrame;

	// ---------- what SPACE would talk to (the prompt over it) ----------

	/** What SPACE talks to or looks at now: where to show the prompt (over it), and the word for it. */
	public record Prompt(Vec3 at, String word) {}

	private static Prompt prompt;

	public static Prompt prompt() {
		return prompt;
	}

	/** Who you'd talk to: the nearest you're facing (or right by you), person or Pokémon; and how far. */
	private static Entity talkTarget(Minecraft mc, double[] dist) {
		Entity best = null;
		double bestScore = 1e9;
		Vec3 me = mc.player.position();
		Vec3 look = Vec3.directionFromRotation(0, mc.player.getYRot());
		dist[0] = 1e9;
		acrossCounter = false;
		for (Entity e : mc.level.entitiesForRendering()) {
			if (!(e instanceof Npc) && !(e instanceof WorldMon) || e.isInvisible()) continue;
			Vec3 to = e.position().subtract(me).multiply(1, 0, 1);
			double d = to.length();
			double facing = d < 1e-3 ? 1 : to.normalize().dot(look);
			// Someone behind a counter (the nurse, a shop's clerk) you're facing: further, but still to talk to.
			boolean across = e instanceof Npc && facing > 0.8 && d <= 4.2;
			if (d > 3.5 && !across) continue;
			if (facing < 0.35 && d > 1.1) continue;
			double score = d - facing * 1.5 + (e instanceof WorldMon ? 0.8 : 0);
			if (score < bestScore) {
				bestScore = score;
				best = e;
				dist[0] = d;
				acrossCounter = across;
			}
		}
		return best;
	}

	/** Whether the one you'd talk to is a person right in front of you (across a counter, say): they come before what's between. */
	private static boolean acrossCounter;

	/** Works out the prompt (each tick): someone to talk to, or the thing in front of you inside. */
	private static void updatePrompt(Minecraft mc) {
		prompt = null;
		if (mc.player == null || mc.level == null || mc.screen != null || Device.busy() || frozen > 0 || BattleScene.active()) return;
		double[] d = new double[1];
		Entity best = talkTarget(mc, d);
		com.insidethecube.retromon.game.Interiors.Ahead ahead = com.insidethecube.retromon.game.Interiors.ahead(mc.player);
		if (ahead != null && ahead.at() != null && (best == null || !acrossCounter && d[0] > ahead.distance() + 0.35)) {
			prompt = new Prompt(ahead.at(), "Look");
			return;
		}
		// (15:21) An item ball's "Pick up", not "Talk"; over a person, high enough to clear their name.
		if (best instanceof WorldMon w && w.key().startsWith("item:")) prompt = new Prompt(best.position().add(0, best.getBbHeight() + 0.35, 0), "Pick up");
		else if (best != null) prompt = new Prompt(best.position().add(0, best.getBbHeight() + (best instanceof Npc ? 0.75 : 0.35), 0), "Talk");
	}

	/** Where a point in the world falls on the screen (GUI pixels, w by h), or null behind the camera. */
	public static float[] project(Vec3 p, int w, int h) {
		double[] pose = lastPose;
		if (pose == null) return null;
		double yaw = Math.toRadians(pose[3]), pitch = Math.toRadians(pose[4]);
		double fx = -Math.sin(yaw) * Math.cos(pitch), fy = -Math.sin(pitch), fz = Math.cos(yaw) * Math.cos(pitch);
		double rx = -Math.cos(yaw), rz = -Math.sin(yaw);
		double ux = -rz * fy, uy = rz * fx - rx * fz, uz = rx * fy;
		double dx = p.x - pose[0], dy = p.y - pose[1], dz = p.z - pose[2];
		double x = dx * rx + dz * rz, y = dx * ux + dy * uy + dz * uz, z = dx * fx + dy * fy + dz * fz;
		if (z <= 0.1) return null;
		double t = Math.tan(Math.toRadians(FOV) / 2);
		return new float[] {(float) (w / 2.0 + x / z / t * h / 2.0), (float) (h / 2.0 - y / z / t * h / 2.0)};
	}

	private static double[] camAt(Vec3 target, double dist, double pitchDeg) {
		double yaw = Math.toRadians(YAW), pitch = Math.toRadians(pitchDeg);
		double lx = -Math.sin(yaw) * Math.cos(pitch), ly = -Math.sin(pitch), lz = Math.cos(yaw) * Math.cos(pitch);
		return new double[] {target.x - lx * dist, target.y - ly * dist, target.z - lz * dist};
	}

	/** A: talk to whoever's in front of you (or close by), person or Pokémon. */
	private static void talk(Minecraft mc) {
		double[] dd = new double[1];
		Entity best = talkTarget(mc, dd);
		double bestDist = dd[0];
		// Inside, the thing in front of you (the TV, a shelf, the fridge) says what it is, unless someone's nearer
		// in front of it.
		com.insidethecube.retromon.game.Interiors.Ahead ahead = com.insidethecube.retromon.game.Interiors.ahead(mc.player);
		if (ahead != null && (best == null || !acrossCounter && bestDist > ahead.distance() + 0.35)) {
			// A map on the wall (or Daisy's on the table) opens the Town Map; B puts you back in the room.
			if (ahead.map()) {
				Sounds.press();
				mc.setScreen(new TownMapScreen(null));
				return;
			}
			// Something the game itself answers (LT.SURGE's trash cans): it says what you find.
			if (ahead.use() != null) {
				CompoundTag u = new CompoundTag();
				u.putString("use", ahead.use());
				ClientPlayNetworking.send(new Net.Act("use", u));
				return;
			}
			// The storage PC in a POKéMON CENTER: BILL's PC.
			if (ahead.pc()) {
				mc.setScreen(new PcScreen());
				return;
			}
			// (Names in it, the Gym statues' "WINNING TRAINERS: {rival}", as yours.)
			var cd = ClientState.data;
			String rv = cd == null || cd.rival.isEmpty() ? "BLUE" : cd.rival, nm = cd == null || cd.name.isEmpty() ? "RED" : cd.name;
			java.util.List<String> said = new java.util.ArrayList<>();
			for (String l : ahead.lines()) said.add(l.replace("{rival}", rv).replace("{player}", nm));
			mc.setScreen(new DialogueScreen(said, "sign", -1, new CompoundTag()));
			return;
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

	/** A town's or route's line from its sign (Pallet Town: "Shades of your journey await!"), or "". */
	public static String signLine(String place) {
		return SIGNS.getOrDefault(place, "");
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

	/** A: read the sign in front of you (its words), a shelf standing in for one (the place's sign) or a mailbox. */
	private static void readSign(Minecraft mc) {
		Vec3 look = Vec3.directionFromRotation(0, mc.player.getYRot());
		for (double d = 0.5; d <= 2.1; d += 0.4) {
			for (int dy = 0; dy <= 1; dy++) {
				net.minecraft.core.BlockPos at = net.minecraft.core.BlockPos.containing(mc.player.getX() + look.x * d, mc.player.getY() + dy + 0.2,
						mc.player.getZ() + look.z * d);
				var state = mc.level.getBlockState(at);
				if (state.is(net.minecraft.world.level.block.Blocks.CHERRY_LEAVES) || state.is(net.minecraft.world.level.block.Blocks.DARK_OAK_FENCE)) {
					// A little tree (Route 2's cherry trees): the server says whether it can be CUT down.
					CompoundTag t = new CompoundTag();
					t.putInt("x", at.getX());
					t.putInt("y", at.getY());
					t.putInt("z", at.getZ());
					t.putBoolean("tree", true);
					ClientPlayNetworking.send(new Net.Act("read", t));
					return;
				}
				if (state.is(com.insidethecube.retromon.Retromon.MAILBOX)) {
					// A house's mailbox: whose house it is.
					Sounds.press();
					CompoundTag t = new CompoundTag();
					t.putInt("x", at.getX());
					t.putInt("y", at.getY());
					t.putInt("z", at.getZ());
					t.putBoolean("mailbox", true);
					ClientPlayNetworking.send(new Net.Act("read", t));
					return;
				}
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
