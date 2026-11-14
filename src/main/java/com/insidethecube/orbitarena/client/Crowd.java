package com.insidethecube.orbitarena.client;

import static com.insidethecube.orbitarena.arena.ArenaLayout.*;

import java.util.ArrayList;
import java.util.List;

import com.insidethecube.orbitarena.OrbitArena;
import com.insidethecube.orbitarena.entity.Fan;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/**
 * The crowd: fans in the seats of the stands, added on your screen only when you're in the arena. How
 * full the stadium is comes from the setup screen. Cheers (from the server: a hit, a kill, the win) make
 * the crowd jump and wave, rippling round the stands; groans (when you take a big hit) put hands on heads.
 */
public final class Crowd {
	private Crowd() {}

	/** How full the stands are: off, a quarter, half, packed. */
	public static float density = 0.5F;
	private static final List<Fan> FANS = new ArrayList<>();
	private static ClientLevel spawnedIn;
	private static int centreX = Integer.MIN_VALUE, spawnedAt = Integer.MIN_VALUE;
	private static float spawnedDensity;
	private static float cheer, groan, idle = 0.15F;
	private static int nextId = -2_000_000;

	public static void setCentre(int x) {
		centreX = x;
	}

	public static void cheer(float strength) {
		if (strength >= 0) cheer = Math.max(cheer, strength);
		else groan = Math.max(groan, -strength);
	}

	/** How excited a fan is (the cheer ripples round, so each fan's beat shifts it a little). */
	public static float excitement(float phase) {
		float wave = 0.85F + 0.15F * Mth.sin((Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getGameTime()) * 0.1F + phase * 6.28F);
		return Math.max(idle * (0.5F + phase), cheer * wave);
	}

	public static float groan() {
		return groan;
	}

	public static void tick(Minecraft mc) {
		cheer *= 0.965F;
		groan *= 0.93F;
		ClientLevel level = mc.level;
		boolean inArena = level != null && level.dimension() == OrbitArena.ARENA && mc.player != null;
		if (!inArena) {
			FANS.clear();
			spawnedIn = null;
			spawnedAt = Integer.MIN_VALUE;
			return;
		}
		int centre = centreX != Integer.MIN_VALUE ? centreX : Math.round((float) mc.player.getX() / SPACING) * SPACING;
		boolean stale = spawnedIn != level || spawnedAt != centre || spawnedDensity != density;
		// Wait until the stands are there (the chunks have arrived).
		if (stale && level.getBlockState(new net.minecraft.core.BlockPos(centre + (int) seatRadius(true, 4), FLOOR + seatY(true, 4) - 1, 0)).isAir()) return;
		if (stale) {
			for (Fan f : FANS) level.removeEntity(f.getId(), Entity.RemovalReason.DISCARDED);
			FANS.clear();
			spawnedIn = level;
			spawnedAt = centre;
			spawnedDensity = density;
			if (density > 0) seat(level, centre);
		}
	}

	private static void seat(ClientLevel level, int centre) {
		for (int pass = 0; pass < 2; pass++) {
			boolean upper = pass == 1;
			int rows = upper ? UPPER_ROWS : LOWER_ROWS;
			for (int row = 0; row < rows; row++) {
				double r = seatRadius(upper, row);
				int y = FLOOR + seatY(upper, row);
				int seats = (int) (Math.PI * 2 * r / 1.25);
				for (int s = 0; s < seats; s++) {
					double a = 360.0 * s / seats;
					if (aisle(a, r) || (upper && box(a))) continue;
					// Keep the tunnel tops clear on the first rows.
					boolean tunnel = false;
					for (double t : TUNNELS) if (!upper && row < 2 && Math.toRadians(angleDiff(a, t)) * r < 3) tunnel = true;
					if (tunnel) continue;
					double h = hash(s, row, pass, 99);
					if (h > density) continue;
					double rad = Math.toRadians(a + (hash(s, row, pass, 98) - 0.5) * 0.4);
					double x = centre + Math.cos(rad) * r, z = Math.sin(rad) * r;
					Fan fan = new Fan(OrbitArena.FAN, level);
					fan.setId(nextId--);
					fan.setPos(x, y, z);
					float yaw = (float) Math.toDegrees(Math.atan2(-z, centre - x)) - 90;
					fan.setYRot(yaw);
					fan.yBodyRot = yaw;
					fan.yHeadRot = yaw + (float) (hash(s, row, pass, 97) - 0.5F) * 40;
					fan.yRotO = yaw;
					fan.yBodyRotO = yaw;
					fan.yHeadRotO = fan.yHeadRot;
					fan.setOldPosAndRot();
					fan.look = (int) (hash(s, row, pass, 96) * FanRenderer.LOOKS);
					fan.sign = hash(s, row, pass, 95) < 0.06 ? (int) (hash(s, row, pass, 94) * FanRenderer.SIGNS) : -1;
					fan.phase = (float) hash(s, row, pass, 93);
					fan.height = 0.86F + (float) hash(s, row, pass, 92) * 0.16F;
					level.addEntity(fan);
					FANS.add(fan);
				}
			}
		}
	}
}
