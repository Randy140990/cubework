package com.insidethecube.orbitarena.client;

import static com.insidethecube.orbitarena.arena.ArenaLayout.*;

import java.util.ArrayList;
import java.util.List;

import com.insidethecube.orbitarena.ArenaSounds;
import com.insidethecube.orbitarena.OrbitArena;
import com.insidethecube.orbitarena.entity.Fan;
import com.insidethecube.orbitarena.net.Payloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;

/**
 * The crowd: fans in the seats of the three tiers, added on your screen only when you're in the arena. How
 * full the stadium is comes from the setup screen.
 * <p>
 * The crowd is never still: every fan keeps choosing something to do (clapping, waving, cheering, pumping
 * a fist or a sign), more and bigger the more excited the crowd is, and every head follows you round the
 * floor. Now and then a Mexican wave goes round the stands. What happens in the fight (hits, kills, the
 * win, your big hits taken) comes from the server and sets the crowd off, with its sound: a murmur all the
 * time, cheers, a roar for "FIGHT!", an uproar and applause for a win, an "ooh" and an "aww".
 */
public final class Crowd {
	private Crowd() {}

	/** How full the stands are: off, quiet, busy, packed. */
	public static float density = 0.5F;
	private static final List<Fan> FANS = new ArrayList<>();
	private static ClientLevel spawnedIn;
	private static int centreX = Integer.MIN_VALUE, spawnedAt = Integer.MIN_VALUE;
	private static float spawnedDensity;
	private static boolean fighting;
	/** How worked up the crowd is (0 to 1), and how much it groans. */
	private static float cheer, groan;
	/** Ticks the crowd keeps celebrating (after a win). */
	private static int party;
	/** The Mexican wave: the tick it started (or -1) and where. */
	private static long waveStart = -1, nextWave = 600;
	private static float waveFrom;
	private static int nextId = -2_000_000;
	private static long lastSmall, lastOoh;
	private static Murmur murmur;

	public static void setCentre(int x) {
		centreX = x;
	}

	public static void setFighting(boolean on) {
		fighting = on;
	}

	/** Something happened in the fight: the crowd reacts, and you hear it. */
	public static void react(int kind, float strength) {
		Minecraft mc = Minecraft.getInstance();
		long now = mc.level == null ? 0 : mc.level.getGameTime();
		float loud = 0.45F + 0.55F * Math.min(1, density / 0.8F);
		switch (kind) {
			case Payloads.Cheer.HIT -> {
				cheer = Math.max(cheer, 0.3F + 0.3F * strength);
				if (now - lastSmall > 18) {
					lastSmall = now;
					play(ArenaSounds.CHEER_SMALL, loud * (0.35F + 0.35F * strength), 0);
				}
			}
			case Payloads.Cheer.KILL -> {
				cheer = Math.max(cheer, 0.75F + 0.25F * strength);
				play(ArenaSounds.CHEER, loud * (0.65F + 0.35F * strength), 0);
			}
			case Payloads.Cheer.FIGHT -> {
				cheer = 1;
				play(ArenaSounds.ROAR, loud, 0);
			}
			case Payloads.Cheer.WIN -> {
				cheer = 1;
				party = 160;
				play(ArenaSounds.UPROAR, loud, 0);
				play(ArenaSounds.APPLAUSE, loud * 0.8F, 30);
			}
			case Payloads.Cheer.LOSS -> {
				groan = 1;
				play(ArenaSounds.AWW, loud * 0.9F, 0);
				play(ArenaSounds.APPLAUSE_LIGHT, loud * 0.5F, 40);
			}
			case Payloads.Cheer.HURT -> {
				groan = Math.max(groan, 0.5F + 0.5F * strength);
				if (now - lastOoh > 30) {
					lastOoh = now;
					play(ArenaSounds.OOH, loud * (0.5F + 0.4F * strength), 0);
				}
			}
			default -> {
			}
		}
	}

	private static void play(SoundEvent sound, float volume, int delay) {
		if (density <= 0) return;
		Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(sound.location(), SoundSource.AMBIENT, volume, 1.0F,
				RandomSource.create(), false, delay, SoundInstance.Attenuation.NONE, 0, 0, 0, true));
	}

	/** How lively the crowd is overall: always a little, more in a fight, a lot when it cheers. */
	public static float energy() {
		float base = fighting ? 0.3F : 0.18F;
		return Math.min(1, Math.max(base, cheer));
	}

	public static float cheering() {
		return cheer;
	}

	public static float groan() {
		return groan;
	}

	/** How far round the Mexican wave is (degrees), or NaN when there isn't one. */
	public static float waveAngle(float time) {
		if (waveStart < 0) return Float.NaN;
		return (waveFrom + (time - waveStart) * 2.4F) % 360;
	}

	public static void tick(Minecraft mc) {
		cheer *= party > 0 ? 0.995F : 0.975F;
		groan *= 0.95F;
		if (party > 0) party--;
		ClientLevel level = mc.level;
		boolean inArena = level != null && level.dimension() == OrbitArena.ARENA && mc.player != null;
		if (!inArena) {
			FANS.clear();
			spawnedIn = null;
			spawnedAt = Integer.MIN_VALUE;
			fighting = false;
			if (murmur != null) murmur.end();
			murmur = null;
			return;
		}
		long now = level.getGameTime();
		// The wave: one lap (150 ticks), every minute or so, when the crowd isn't busy cheering.
		if (waveStart >= 0 && now - waveStart > 150) waveStart = -1;
		if (waveStart < 0 && now > nextWave && cheer < 0.4F) {
			waveStart = now;
			waveFrom = (float) (Math.random() * 360);
			nextWave = now + 900 + (long) (Math.random() * 900);
		}
		// The murmur, under everything, louder as the crowd gets going.
		if (density > 0 && (murmur == null || murmur.isStopped())) {
			murmur = new Murmur();
			mc.getSoundManager().play(murmur);
		}
		int centre = centreX != Integer.MIN_VALUE ? centreX : Math.round((float) mc.player.getX() / SPACING) * SPACING;
		boolean stale = spawnedIn != level || spawnedAt != centre || spawnedDensity != density;
		// Wait until the stands are there (the chunks have arrived).
		if (stale && level.getBlockState(new BlockPos(centre + (int) seatRadius(0, 4), FLOOR + seatY(0, 4) - 1, 0)).isAir()) return;
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
		for (int tier = 0; tier < TIER_IN.length; tier++) {
			for (int row = 0; row < TIER_ROWS; row++) {
				double r = seatRadius(tier, row);
				int y = FLOOR + seatY(tier, row);
				int seats = (int) (Math.PI * 2 * r / 1.35);
				for (int s = 0; s < seats; s++) {
					double a = 360.0 * s / seats;
					if (aisle(a, r) || (tier == 1 && box(a))) continue;
					// Keep the tunnel tops clear on the first rows.
					boolean tunnel = false;
					for (double t : TUNNELS) if (tier == 0 && row < 3 && Math.toRadians(angleDiff(a, t)) * r < 3.6) tunnel = true;
					if (tunnel) continue;
					double h = hash(s, row, tier, 99);
					if (h > density) continue;
					double rad = Math.toRadians(a + (hash(s, row, tier, 98) - 0.5) * 0.4);
					double x = centre + Math.cos(rad) * r, z = Math.sin(rad) * r;
					Fan fan = new Fan(OrbitArena.FAN, level);
					fan.setId(nextId--);
					fan.setPos(x, y, z);
					float yaw = (float) Math.toDegrees(Math.atan2(-z, centre - x)) - 90;
					fan.setYRot(yaw);
					fan.yBodyRot = yaw;
					fan.yHeadRot = yaw;
					fan.yRotO = yaw;
					fan.yBodyRotO = yaw;
					fan.yHeadRotO = yaw;
					fan.setOldPosAndRot();
					fan.look = (int) (hash(s, row, tier, 96) * FanRenderer.LOOKS);
					fan.sign = hash(s, row, tier, 95) < 0.07 ? (int) (hash(s, row, tier, 94) * FanRenderer.SIGNS) : -1;
					fan.phase = (float) hash(s, row, tier, 93);
					fan.height = 0.86F + (float) hash(s, row, tier, 92) * 0.16F;
					fan.seatAngle = (float) Math.toDegrees(rad);
					level.addEntity(fan);
					FANS.add(fan);
				}
			}
		}
	}

	/** The crowd's murmur: loops while you're in the arena, louder as the crowd gets going. */
	private static final class Murmur extends AbstractTickableSoundInstance {
		Murmur() {
			super(ArenaSounds.MURMUR, SoundSource.AMBIENT, RandomSource.create());
			this.looping = true;
			this.delay = 0;
			this.relative = true;
			this.attenuation = SoundInstance.Attenuation.NONE;
			this.volume = 0.01F;
		}

		@Override
		public void tick() {
			float target = density <= 0 ? 0 : (0.3F + 0.45F * Math.min(1, density / 0.8F)) * (0.6F + 0.4F * energy());
			this.volume = Mth.lerp(0.05F, this.volume, target);
			if (density <= 0 && this.volume < 0.02F) stop();
		}

		void end() {
			stop();
		}
	}
}
