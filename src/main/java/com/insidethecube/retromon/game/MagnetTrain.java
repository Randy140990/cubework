package com.insidethecube.retromon.game;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * (0.73, John's 28 Nov 09:22) The MAGNET TRAIN between GOLDENROD and SAFFRON: "I know in the interiors in game, I'm
 * pretty sure it'll show you getting on it and it going away, so there might be some sort of cutscene". Talk to the
 * OFFICER at either station's gate (rooms73.js); YES, and the screen fades; then, out on John's line, the train
 * (pokemon/magnet_train, tools/train73.py) waits at the station a moment, pulls out and away along the line, faster and
 * faster, and into the tunnel at its end ("the tram goes in, disappears"); the screen fades again, and at the other
 * city's tunnel it comes gliding out, slows along the line and pulls in under the station's roof.
 *
 * <p>The camera follows the train along the line (client Ride), the bars of a cutscene in, you standing still; while
 * it runs you're kept in the station's hall under the line, out of sight. No PASS is asked for yet: once the story's
 * settled (John: Kanto's champion "starting again" in Johto) it can need the PASS, as HeartGold has it.
 *
 * <p>(0.78, John's 14:54) "we'll soon depart goldenrod ... I'm not in goldenrod, I'm in Saffron, so I should say we're
 * departing Saffron": the OFFICER names both, "We'll soon depart SAFFRON for GOLDENROD." "you should be led onto the
 * train first and foremost": YES, and he steps aside from the gate, and you walk through it, across the platform and
 * in at the train's door, before it goes. "that middle animation ... where you can see like the train's travelling"
 * (his 12:31: "the train is moving from left to right ... the trees either side in our style going past you"): between
 * the tunnels, the ride (the room magnet_train_ride, tools/trainride.py), the train side on, crossing from the left to
 * the right along a stretch of John's own line, the camera going along with it through the middle, our trees rushing
 * past either side. And "seeing it like come into the station and then you walk off it. The ticket guard can maybe be
 * out of the way. You walk to the other side, so that you're on the other side of the barrier now, and then he lets
 * you know": in at the other station you step off the train and walk out through the gate, the OFFICER stood aside;
 * once you're past the barrier he's back at his gate, turned to you: "We have arrived in GOLDENROD."
 */
public final class MagnetTrain {
	private MagnetTrain() {}

	/**
	 * One end of the line, in John's world: its station room, its OFFICER, its city; the line's top (where the train
	 * stands) and middle; the middle of the train stopped in the station; the tunnel's dark back wall and its mouth;
	 * which way the tunnel is from the station (+1 east, -1 west); and the station's hall, where you're kept.
	 */
	record End(String room, String officer, String city, double y, double z, double stopX, double wallX, double mouthX, int toTunnel, Vec3 hall) {}

	/** GOLDENROD's: its line (z 684 to 688, its top at y 28) east from the station to its tunnel (its back at x -2047). */
	static final End GOLDENROD = new End("goldenrod_magnet_train_station", "goldenrod_magnet_train_station_officer", "GOLDENROD", 28.0, 686.5, -2120.5, -2047.0,
			-2055.0, 1, new Vec3(-2120.5, 21.0, 686.5));
	/**
	 * SAFFRON's (John's copy of GOLDENROD's, flipped east to west): its line (z 389 to 393, top y 41) west to its tunnel;
	 * ((0.74) his 10:56 save: the line run on west, the tunnel's mouth at x -430, its dark back at -433).
	 */
	static final End SAFFRON = new End("saffron_magnet_train_station", "saffron_magnet_train_station_officer", "SAFFRON", 41.0, 391.5, -385.5, -433.0, -430.0, -1,
			new Vec3(-385.5, 34.0, 391.5));

	/** Half the train's length (tools/train73.py: 16.5 blocks), and a little more, to be out of sight behind the wall. */
	private static final double HALF = 8.25 + 1.0;
	/** Ticks: standing at the platform first; pulling out to the tunnel; gliding in from the other tunnel; standing after. */
	private static final int WAIT = 30, OUT = 110, IN = 100, REST = 20, GREET = 12;
	/** (0.78) The ride between the tunnels: how long the train takes across its room, and a moment after. */
	private static final int CROSS = 170, AFTER = 14;
	/** (0.78) The ride's line (tools/trainride.py): the middle of its deck, and its top over the room's grass. */
	private static final double RIDE_Z = 21.5, RIDE_Y = 8.0;
	/** (0.78) Walking on and off: how far a tick (a brisk walk). */
	private static final double PACE = 0.16;
	/**
	 * (0.78) In a station (rooms73.js, from its corner): where the OFFICER stands at his gate, and aside (east of it,
	 * facing it); the way through the gate onto the train (in at its middle door), and the way off it and out past the
	 * barrier.
	 */
	private static final double[] POST = {10.0, 7.7}, ASIDE = {12.9, 8.3};
	private static final double[][] ON = {{10.0, 8.6}, {10.0, 6.0}, {11.1, 4.4}, {11.1, 3.2}};
	private static final double[][] OFF_TRAIN = {{11.1, 3.2}, {11.1, 4.4}, {10.0, 6.0}, {10.0, 8.6}, {10.0, 10.2}};

	private enum Phase { BOARD, DEPART, RIDE, ARRIVE, ALIGHT, OFF }

	private static final class Ride {
		final End from, to;
		Phase phase = Phase.BOARD;
		int t, train = -1;
		/** (0.78) How far along the walk on or off you are. */
		double along;
		Ride(End from, End to) {
			this.from = from;
			this.to = to;
		}
	}

	private static final Map<UUID, Ride> RIDES = new HashMap<>();

	public static boolean riding(ServerPlayer p) {
		return RIDES.containsKey(p.getUUID());
	}

	private static End endOf(String officer) {
		if (GOLDENROD.officer().equals(officer)) return GOLDENROD;
		if (SAFFRON.officer().equals(officer)) return SAFFRON;
		return null;
	}

	/** The OFFICER at a station's gate. */
	public static boolean talk(ServerPlayer p, Npc n) {
		End from = endOf(n.key());
		if (from == null) return false;
		End to = from == GOLDENROD ? SAFFRON : GOLDENROD;
		CompoundTag t = Game.yesNo();
		t.putString("who", "OFFICER");
		Game.dialogue(p, n, "magnet_train", List.of("We'll soon depart " + from.city() + " for " + to.city() + ".", "Are you coming aboard?"), t);
		return true;
	}

	/** YES or NO to the OFFICER. */
	public static boolean answer(ServerPlayer p, String mode, CompoundTag t, Npc n) {
		if (!mode.equals("magnet_train")) return false;
		boolean yes = t.getBooleanOr("answered", false) ? t.getIntOr("choice", 1) == 0 : t.getBooleanOr("yes", false);
		End from = n == null ? null : endOf(n.key());
		if (!yes || from == null) {
			Game.dialogue(p, n, "talk", List.of("We hope to see you again."));
			return true;
		}
		Game.dialogue(p, n, "magnet_train_go", List.of("Right this way, please."));
		return true;
	}

	/** "Right this way, please." read: (0.78) he steps aside, and you walk through the gate and onto the train. */
	public static boolean onBoard(ServerPlayer p, String mode, Npc n) {
		if (!mode.equals("magnet_train_go")) return false;
		End from = n == null ? null : endOf(n.key());
		if (from == null || riding(p)) return true;
		Ride r = new Ride(from, from == GOLDENROD ? SAFFRON : GOLDENROD);
		RIDES.put(p.getUUID(), r);
		Interiors.Room st = Interiors.room(from.room());
		if (st != null) stand(n, st, ASIDE[0], ASIDE[1], 90F);
		return true;
	}

	/** Out on the line: the train at its platform, about to pull out (the camera with it). */
	private static void depart(ServerPlayer p, Ride r) {
		Interiors.fadeThen(p, () -> {
			// (The OFFICER back at his gate, behind you.)
			officerHome(p, r.from);
			ServerLevel world = p.level().getServer().overworld();
			if (p.level() != world) Game.leavingLevel(p);
			Vec3 h = r.from.hall();
			p.teleportTo(world, h.x, h.y, h.z, java.util.Set.of(), 0F, 30F, true);
			p.setDeltaMovement(Vec3.ZERO);
			p.setInvisible(true);
			r.phase = Phase.DEPART;
			r.t = 0;
			r.train = spawn(world, r.from, r.from.stopX());
			send(p, r, r.from, r.from.stopX(), r.from.mouthX());
		});
	}

	/** (0.78) Out of the ride: the other city's tunnel, the train coming out of it. */
	private static void arriveOutside(ServerPlayer p, Ride r) {
		Interiors.fadeThen(p, () -> {
			if (train(p, r) != null) train(p, r).discard();
			ServerLevel world = p.level().getServer().overworld();
			if (p.level() != world) Game.leavingLevel(p);
			Vec3 h = r.to.hall();
			p.teleportTo(world, h.x, h.y, h.z, java.util.Set.of(), 0F, 30F, true);
			p.setDeltaMovement(Vec3.ZERO);
			p.setInvisible(true);
			r.phase = Phase.ARRIVE;
			r.t = 0;
			double start = r.to.wallX() + r.to.toTunnel() * HALF;
			r.train = spawn(world, r.to, start);
			send(p, r, r.to, r.to.stopX(), r.to.mouthX());
		});
	}

	/** (0.78) Into the ride between the tunnels: the train coming in from the left, the camera waiting for it. */
	private static void ride(ServerPlayer p, Ride r) {
		Interiors.fadeThen(p, () -> {
			if (train(p, r) != null) train(p, r).discard();
			Interiors.Room ride = Interiors.room("magnet_train_ride");
			if (ride == null || !Interiors.put(p, ride, ride.w() / 2, 0, ride.d() - 2.5, 180F)) {
				// (No ride room: straight on to the other end.)
				r.train = -1;
				arriveOutside(p, r);
				return;
			}
			p.setInvisible(true);
			r.phase = Phase.RIDE;
			r.t = 0;
			ServerLevel lv = (ServerLevel) p.level();
			lv.getChunk(BlockPos.containing(ride.ox() + 4, ride.oy() + RIDE_Y, ride.oz() + RIDE_Z));
			WorldMon w = Game.placeMon(lv, "magnet_train", "train:magnet", ride.ox() - 20, ride.oy() + RIDE_Y, ride.oz() + RIDE_Z, 90F);
			if (w != null) {
				w.setNoGravity(true);
				w.setNoAi(true);
				w.noPhysics = true;
			}
			r.train = w == null ? -1 : w.getId();
			// The camera held at the left while it comes in, along with it through the middle, held at the right as it
			// goes off the other side.
			CompoundTag t = new CompoundTag();
			t.putBoolean("on", true);
			t.putInt("train", r.train);
			t.putDouble("x0", ride.ox() + 70);
			t.putDouble("x1", ride.ox() + ride.w() - 70);
			t.putDouble("start", ride.ox() + 70);
			t.putDouble("y", ride.oy() + RIDE_Y);
			t.putDouble("z", ride.oz() + RIDE_Z);
			t.putDouble("wall", 0);
			t.putInt("dir", 0);
			Net.send(p, "ride", t);
		});
	}

	/** (0.78) Someone in a station standing at a spot (from its corner), facing that way, kept there. */
	private static void stand(Npc n, Interiors.Room r, double x, double z, float yaw) {
		if (n == null) return;
		n.getNavigation().stop();
		n.teleportTo(r.ox() + x, r.oy(), r.oz() + z);
		n.setFacing(yaw);
		n.rehome(BlockPos.containing(r.ox() + x, r.oy(), r.oz() + z));
	}

	/** (0.78) A station's OFFICER, if he's about. */
	private static Npc officer(ServerPlayer p, End e) {
		Npc n = null;
		for (Npc o : p.level().getEntitiesOfClass(Npc.class, p.getBoundingBox().inflate(32), o -> e.officer().equals(o.key()))) n = o;
		return n;
	}

	/** (0.78) A station's OFFICER back at his gate, facing the hall. */
	private static void officerHome(ServerPlayer p, End e) {
		Interiors.Room st = Interiors.room(e.room());
		if (st != null) stand(officer(p, e), st, POST[0], POST[1], 0F);
	}

	/** (0.78) Walking a way through a station (points from its corner), a step a tick; true once you're at its end. */
	private static boolean walk(ServerPlayer p, Ride r, String room, double[][] way) {
		Interiors.Room st = Interiors.room(room);
		if (st == null) return true;
		r.along += PACE;
		double left = r.along;
		for (int i = 0; i + 1 < way.length; i++) {
			double[] a = way[i], b = way[i + 1];
			double len = Math.hypot(b[0] - a[0], b[1] - a[1]);
			if (left <= len) {
				double f = len == 0 ? 1 : left / len;
				float yaw = (float) Math.toDegrees(Math.atan2(-(b[0] - a[0]), b[1] - a[1]));
				if (p.level() instanceof ServerLevel lv)
					p.teleportTo(lv, st.ox() + a[0] + (b[0] - a[0]) * f, st.oy(), st.oz() + a[1] + (b[1] - a[1]) * f, java.util.Set.of(), yaw, 30F, true);
				p.setDeltaMovement(Vec3.ZERO);
				return false;
			}
			left -= len;
		}
		double[] e = way[way.length - 1];
		p.teleportTo(st.ox() + e[0], st.oy(), st.oz() + e[1]);
		return true;
	}

	/** The train on the line at x (its middle), facing along it. */
	private static int spawn(ServerLevel world, End e, double x) {
		// (Its chunks there, whether or not you've come this way yet.)
		world.getChunk(BlockPos.containing(x, e.y(), e.z()));
		WorldMon w = Game.placeMon(world, "magnet_train", "train:magnet", x, e.y(), e.z(), 90F);
		if (w == null) return -1;
		w.setNoGravity(true);
		w.setNoAi(true);
		w.noPhysics = true;
		return w.getId();
	}

	private static WorldMon train(ServerPlayer p, Ride r) {
		return r.train >= 0 && p.level().getEntity(r.train) instanceof WorldMon w ? w : null;
	}

	/** The client's camera: follow the train (its entity), held between the station and the tunnel's mouth. */
	private static void send(ServerPlayer p, Ride r, End e, double a, double b) {
		CompoundTag t = new CompoundTag();
		t.putBoolean("on", true);
		t.putInt("train", r.train);
		t.putDouble("x0", Math.min(a, b));
		t.putDouble("x1", Math.max(a, b));
		t.putDouble("start", a);
		t.putDouble("y", e.y());
		t.putDouble("z", e.z());
		// ((0.74) The tunnel's dark back here: the train's cut off past it, so it's gone into the dark.)
		t.putDouble("wall", e.wallX());
		t.putInt("dir", e.toTunnel());
		Net.send(p, "ride", t);
	}

	/** Every tick while you ride: true (nothing else about where you are applies). */
	public static boolean tick(ServerPlayer p) {
		Ride r = RIDES.get(p.getUUID());
		if (r == null) return false;
		// Still: the bars of a cutscene, you not moving.
		if (p.level().getServer().getTickCount() % 5 == 0) {
			CompoundTag hold = new CompoundTag();
			hold.putInt("ticks", 12);
			Net.send(p, "hold", hold);
		}
		if (Interiors.busy(p)) return true;
		r.t++;
		switch (r.phase) {
			case BOARD -> {
				// (0.78) Through the gate, across the platform and in at the train's door; then out on the line.
				if (r.t > 4 && walk(p, r, r.from.room(), ON) || r.t > 400) {
					r.t = Integer.MIN_VALUE / 2;
					depart(p, r);
				}
			}
			case DEPART -> {
				End e = r.from;
				double end = e.wallX() + e.toTunnel() * HALF, d = Math.abs(end - e.stopX());
				double k = Math.max(0, r.t - WAIT) / (double) OUT;
				WorldMon w = train(p, r);
				if (w != null) w.setPos(e.stopX() + e.toTunnel() * d * k * k, e.y(), e.z());
				if (r.t >= WAIT + OUT) {
					// Into the tunnel and gone: (0.78) the ride between the tunnels next.
					r.t = Integer.MIN_VALUE / 2;
					ride(p, r);
				}
			}
			case RIDE -> {
				// Across, left to right, eased a touch through the middle as the camera goes with it.
				Interiors.Room ride = Interiors.room("magnet_train_ride");
				WorldMon w = train(p, r);
				if (ride != null && w != null) {
					double k = Math.min(1, r.t / (double) CROSS);
					double from = ride.ox() - 20, to = ride.ox() + ride.w() + 20;
					w.setPos(from + (to - from) * (k - Math.sin(2 * Math.PI * k) * 0.06), ride.oy() + RIDE_Y, ride.oz() + RIDE_Z);
				}
				if (r.t >= CROSS + AFTER) {
					r.t = Integer.MIN_VALUE / 2;
					arriveOutside(p, r);
				}
			}
			case ARRIVE -> {
				End e = r.to;
				double start = e.wallX() + e.toTunnel() * HALF, d = Math.abs(e.stopX() - start);
				double k = Math.min(1, r.t / (double) IN);
				WorldMon w = train(p, r);
				if (w != null) w.setPos(start - e.toTunnel() * d * (1 - (1 - k) * (1 - k)), e.y(), e.z());
				if (r.t >= IN + REST) {
					// (0.78) Off the train, inside the station: out of its door, and through the gate.
					r.t = Integer.MIN_VALUE / 2;
					Interiors.fadeThen(p, () -> {
						if (train(p, r) != null) train(p, r).discard();
						p.setInvisible(false);
						CompoundTag off = new CompoundTag();
						off.putBoolean("on", false);
						Net.send(p, "ride", off);
						r.phase = Phase.ALIGHT;
						r.t = 0;
						r.along = 0;
						Interiors.go(p, Interiors.room(e.room()), OFF_TRAIN[0][0], OFF_TRAIN[0][1], 0F, false);
					});
				}
			}
			case ALIGHT -> {
				// The OFFICER out of the way (he's there once the room's made), you off the train and out past the barrier.
				Interiors.Room st = Interiors.room(r.to.room());
				if (st != null && r.t <= 6) stand(officer(p, r.to), st, ASIDE[0], ASIDE[1], 90F);
				if (r.t > 6 && walk(p, r, r.to.room(), OFF_TRAIN)) {
					r.phase = Phase.OFF;
					r.t = 0;
				}
			}
			case OFF -> {
				if (r.t >= GREET) {
					RIDES.remove(p.getUUID());
					Npc n = officer(p, r.to);
					Interiors.Room st = Interiors.room(r.to.room());
					// Back at his gate, turned to you (you to him); then his word.
					if (n != null && st != null) {
						stand(n, st, POST[0], POST[1], 0F);
						n.setFacing(n.yawTo(p));
					}
					if (p.level() instanceof ServerLevel lv) p.teleportTo(lv, p.getX(), p.getY(), p.getZ(), java.util.Set.of(), 180F, 30F, true);
					Game.dialogue(p, n, "talk", List.of("We have arrived in " + r.to.city() + ".", "We hope to see you again."));
				}
			}
		}
		return true;
	}

	/** Gone mid-ride (left the game): the train away, you back to being seen. */
	public static void left(ServerPlayer p) {
		Ride r = RIDES.remove(p.getUUID());
		if (r == null) return;
		WorldMon w = train(p, r);
		if (w != null) w.discard();
		p.setInvisible(false);
	}

	/** A train left on the line by a game closed mid-ride: taken away when no one's riding. */
	public static void sweep(ServerPlayer p) {
		if (!RIDES.isEmpty() || Interiors.in(p)) return;
		AABB box = p.getBoundingBox().inflate(96, 32, 96);
		for (WorldMon w : p.level().getEntitiesOfClass(WorldMon.class, box, w -> w.key().startsWith("train:"))) w.discard();
	}
}
