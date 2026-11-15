package com.insidethecube.orbitarena.client;

import static com.insidethecube.orbitarena.arena.BridgeLayout.Y;

import java.util.ArrayList;
import java.util.List;

import com.insidethecube.orbitarena.OrbitArena;
import com.insidethecube.orbitarena.arena.ArenaLayout;
import com.insidethecube.orbitarena.entity.Rock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;

/**
 * Rocks orbiting round The Bridge, on your screen only: two rings of them, one above the playing area and
 * one below, between it and the spires, going slowly round and tumbling as they go, the nearer ones
 * quicker, so the space round the map moves.
 */
public final class Rocks {
	private Rocks() {}

	private record Orbit(double radius, double height, double period, double start, double bob, double tilt) {}

	private static final int COUNT = 18;
	private static final List<Rock> ROCKS = new ArrayList<>();
	private static final List<Orbit> ORBITS = new ArrayList<>();
	private static ClientLevel in;
	private static int cx = Integer.MIN_VALUE, cz;
	private static int nextId = -3_500_000;

	public static void set(int centreX, int centreZ) {
		if (centreX != cx || centreZ != cz) clear();
		cx = centreX;
		cz = centreZ;
	}

	public static void clear() {
		for (Rock r : ROCKS) if (in != null) in.removeEntity(r.getId(), Entity.RemovalReason.DISCARDED);
		ROCKS.clear();
		ORBITS.clear();
		in = null;
	}

	private static double h(int i, int salt) {
		return ArenaLayout.hash(i, salt, 0, 4242);
	}

	public static void tick(Minecraft mc) {
		ClientLevel level = mc.level;
		boolean onBridge = level != null && mc.player != null && level.dimension() == OrbitArena.ARENA && Crowd.onBridge() && cx != Integer.MIN_VALUE;
		if (!onBridge) {
			if (!ROCKS.isEmpty() || in != null) clear();
			return;
		}
		if (in != level) {
			clear();
			in = level;
			for (int i = 0; i < COUNT; i++) {
				boolean above = i % 2 == 0;
				double radius = 50 + h(i, 1) * 17, height = above ? 36 + h(i, 2) * 20 : -32 - h(i, 2) * 24;
				// Nearer rocks go round quicker (one turn every three to seven minutes).
				double period = 3600 + (radius - 50) * 170 + h(i, 3) * 1800;
				ORBITS.add(new Orbit(radius, height, period, h(i, 4) * Math.PI * 2, h(i, 5) * 3, (h(i, 6) - 0.5) * 0.2));
				Rock r = new Rock(OrbitArena.ROCK, level);
				r.setId(nextId--);
				r.seed = 9001L + i * 31L;
				r.size = 2 + (int) (h(i, 7) * h(i, 7) * 5);
				r.spinX = (float) ((h(i, 8) - 0.5) * 0.6);
				r.spinY = (float) ((h(i, 9) - 0.5) * 0.8);
				place(r, ORBITS.get(i), level.getGameTime(), true);
				level.addEntity(r);
				ROCKS.add(r);
			}
			return;
		}
		long time = level.getGameTime();
		for (int i = 0; i < ROCKS.size(); i++) place(ROCKS.get(i), ORBITS.get(i), time, false);
	}

	private static void place(Rock r, Orbit o, long time, boolean first) {
		double a = o.start() + time / o.period() * Math.PI * 2;
		double x = cx + Math.cos(a) * o.radius() * 1.15, z = cz + Math.sin(a) * o.radius();
		double y = Y + o.height() + Math.sin(a * 3 + o.start()) * o.bob() + Math.cos(a) * o.radius() * o.tilt();
		if (first) {
			r.setPos(x, y, z);
			r.setOldPosAndRot();
			return;
		}
		r.xo = r.getX();
		r.yo = r.getY();
		r.zo = r.getZ();
		r.setPos(x, y, z);
	}
}
