package com.insidethecube.retromon.game;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * (0.73, John's 28 Nov 09:22) The MAGNET TRAIN between GOLDENROD and SAFFRON: "I know in the interiors in game, I'm
 * pretty sure it'll show you getting on it and it going away, so there might be some sort of cutscene". Talk to the
 * OFFICER at either station's gate (rooms73.js): "We'll soon depart for SAFFRON. Are you coming aboard?" YES, and the
 * screen fades; then, out on John's line, the train (pokemon/magnet_train, tools/train73.py) waits at the station a
 * moment, pulls out and away along the line, faster and faster, and into the tunnel at its end ("the tram goes in,
 * disappears"); the screen fades again, and at the other city's tunnel it comes gliding out, slows along the line and
 * pulls in under the station's roof. A last fade and you're off the train inside that station, the OFFICER there
 * greeting you: "We have arrived in SAFFRON."
 *
 * The camera follows the train along the line (client Ride), the bars of a cutscene in, you standing still; while it
 * runs you're kept in the station's hall under the line, out of sight. No PASS is asked for yet: once the story's
 * settled (John: Kanto's champion "starting again" in Johto) it can need the PASS, as HeartGold has it.
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
	private static final int WAIT = 30, OUT = 110, IN = 100, REST = 20, GREET = 20;

	private enum Phase { DEPART, ARRIVE, OFF }

	private static final class Ride {
		final End from, to;
		Phase phase = Phase.DEPART;
		int t, train = -1;
		boolean moving;
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
		Game.dialogue(p, n, "magnet_train", List.of("We'll soon depart for " + to.city() + ".", from == GOLDENROD ? "Are you coming aboard?" : "Are you coming on board?"), t);
		return true;
	}

	/** YES or NO to the OFFICER. */
	public static boolean answer(ServerPlayer p, String mode, CompoundTag t, Npc n) {
		if (!mode.equals("magnet_train")) return false;
		boolean yes = t.getBooleanOr("answered", false) ? t.getIntOr("choice", 1) == 0 : t.getBooleanOr("yes", false);
		End from = n == null ? null : endOf(n.key());
		if (!yes || from == null) {
			Game.dialogue(p, n, "talk", List.of(from == SAFFRON ? "We hope to see you again." : "We hope to see you again!"));
			return true;
		}
		Game.dialogue(p, n, "magnet_train_go", List.of("Right this way, please."));
		return true;
	}

	/** "Right this way, please." read: on board. */
	public static boolean onBoard(ServerPlayer p, String mode, Npc n) {
		if (!mode.equals("magnet_train_go")) return false;
		End from = n == null ? null : endOf(n.key());
		if (from == null || riding(p)) return true;
		Ride r = new Ride(from, from == GOLDENROD ? SAFFRON : GOLDENROD);
		RIDES.put(p.getUUID(), r);
		Interiors.fadeThen(p, () -> {
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
		return true;
	}

	/** The train on the line at x (its middle), facing along it. */
	private static int spawn(ServerLevel world, End e, double x) {
		// (Its chunks there, whether or not you've come this way yet.)
		world.getChunk(net.minecraft.core.BlockPos.containing(x, e.y(), e.z()));
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
			case DEPART -> {
				End e = r.from;
				double end = e.wallX() + e.toTunnel() * HALF, d = Math.abs(end - e.stopX());
				double k = Math.max(0, r.t - WAIT) / (double) OUT;
				WorldMon w = train(p, r);
				if (w != null) w.setPos(e.stopX() + e.toTunnel() * d * k * k, e.y(), e.z());
				if (r.t >= WAIT + OUT) {
					// Into the tunnel and gone: the other city's tunnel next.
					Interiors.fadeThen(p, () -> {
						if (train(p, r) != null) train(p, r).discard();
						ServerLevel world = p.level().getServer().overworld();
						Vec3 h = r.to.hall();
						p.teleportTo(world, h.x, h.y, h.z, java.util.Set.of(), 0F, 30F, true);
						p.setDeltaMovement(Vec3.ZERO);
						r.phase = Phase.ARRIVE;
						r.t = 0;
						double start = r.to.wallX() + r.to.toTunnel() * HALF;
						r.train = spawn(world, r.to, start);
						send(p, r, r.to, r.to.stopX(), r.to.mouthX());
					});
				}
			}
			case ARRIVE -> {
				End e = r.to;
				double start = e.wallX() + e.toTunnel() * HALF, d = Math.abs(e.stopX() - start);
				double k = Math.min(1, r.t / (double) IN);
				WorldMon w = train(p, r);
				if (w != null) w.setPos(start - e.toTunnel() * d * (1 - (1 - k) * (1 - k)), e.y(), e.z());
				if (r.t >= IN + REST) {
					// Off the train, inside the station, by the gate.
					Interiors.fadeThen(p, () -> {
						if (train(p, r) != null) train(p, r).discard();
						p.setInvisible(false);
						CompoundTag off = new CompoundTag();
						off.putBoolean("on", false);
						Net.send(p, "ride", off);
						r.phase = Phase.OFF;
						r.t = 0;
						Interiors.go(p, Interiors.room(e.room()), 10.5, 9.0, 0F, false);
					});
				}
			}
			case OFF -> {
				if (r.t >= GREET) {
					RIDES.remove(p.getUUID());
					Npc n = null;
					for (Npc o : p.level().getEntitiesOfClass(Npc.class, p.getBoundingBox().inflate(24), o -> r.to.officer().equals(o.key()))) n = o;
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
