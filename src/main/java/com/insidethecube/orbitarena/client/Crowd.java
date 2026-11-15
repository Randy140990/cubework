package com.insidethecube.orbitarena.client;

import static com.insidethecube.orbitarena.arena.ArenaLayout.*;

import java.util.ArrayList;
import java.util.List;

import com.insidethecube.orbitarena.ArenaSounds;
import com.insidethecube.orbitarena.OrbitArena;
import com.insidethecube.orbitarena.arena.BridgeLayout;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

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
	private static int centreX = Integer.MIN_VALUE, centreZ, mode, spawnedAt = Integer.MIN_VALUE, spawnedZ, spawnedMode = -1;
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

	/** Which arena you're in (the stadium or the bridge) and its centre. */
	public static void setArena(int kind, int x, int z) {
		mode = kind;
		centreX = x;
		centreZ = z;
	}

	public static boolean onBridge() {
		return mode == com.insidethecube.orbitarena.net.Payloads.State.BRIDGE;
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
				party = 230;
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
		boolean stale = spawnedIn != level || spawnedAt != centre || spawnedZ != centreZ || spawnedMode != mode || spawnedDensity != density;
		// Wait until the stands are there (the chunks have arrived).
		BlockPos check = onBridge() ? new BlockPos(centre, BridgeLayout.Y + BridgeLayout.rowTop(2), centreZ + (int) BridgeLayout.rowZ(2))
				: new BlockPos(centre + (int) seatRadius(0, 4), FLOOR + seatY(0, 4) - 1, 0);
		if (stale && level.getBlockState(check).isAir()) return;
		if (stale) {
			for (Fan f : FANS) level.removeEntity(f.getId(), Entity.RemovalReason.DISCARDED);
			FANS.clear();
			spawnedIn = level;
			spawnedAt = centre;
			spawnedZ = centreZ;
			spawnedMode = mode;
			spawnedDensity = density;
			if (density > 0) {
				if (onBridge()) seatBridge(level, centre, centreZ);
				else seat(level, centre);
			}
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
					// Facing you as you start (east, row 4 of the first tier): three side by side spelling the brand.
					int brand = tier == 0 && row == 4 ? s == seats - 1 ? 0 : s == 0 ? 1 : s == 1 ? 2 : -1 : -1;
					double h = hash(s, row, tier, 99);
					if (h > density && brand < 0) continue;
					double rad = Math.toRadians(a + (brand >= 0 ? 0 : (hash(s, row, tier, 98) - 0.5) * 0.4));
					double x = centre + Math.cos(rad) * r, z = Math.sin(rad) * r;
					Fan fan = add(level, x, y, z, centre, 0, hash(s, row, tier, 96), hash(s, row, tier, 95), hash(s, row, tier, 94), hash(s, row, tier, 93),
							hash(s, row, tier, 92), brand);
					fan.seatAngle = (float) Math.toDegrees(rad);
				}
			}
		}
	}

	/**
	 * A fan at (x, y, z) facing (fx, fz): its look, sign (about one in eleven holds one, GG most often), a
	 * spyglass now and then, its beat and height, from its seat's random numbers. Brand: 0, 1, 2 for the
	 * three boards that spell INSID3 TH3 CUB3, or -1.
	 */
	private static Fan add(ClientLevel level, double x, double y, double z, double fx, double fz, double look, double signRoll, double signPick, double phase,
			double height, int brand) {
		Fan fan = new Fan(OrbitArena.FAN, level);
		fan.setId(nextId--);
		fan.setPos(x, y, z);
		float yaw = (float) Math.toDegrees(Math.atan2(fz - z, fx - x)) - 90;
		fan.setYRot(yaw);
		fan.yBodyRot = yaw;
		fan.yHeadRot = yaw;
		fan.yRotO = yaw;
		fan.yBodyRotO = yaw;
		fan.yHeadRotO = yaw;
		fan.setOldPosAndRot();
		fan.look = (int) (look * FanRenderer.LOOKS);
		if (brand >= 0) fan.sign = FanModel.BIG + brand;
		else if (signRoll < 0.09) fan.sign = signPick < 0.4 ? 2 : (int) ((signPick - 0.4) / 0.6 * FanModel.BIG);
		else fan.sign = -1;
		if (fan.sign < 0 && signRoll > 0.985) {
			fan.spyglass = true;
			fan.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.SPYGLASS));
		}
		fan.phase = (float) phase;
		fan.height = 0.86F + (float) height * 0.16F;
		level.addEntity(fan);
		FANS.add(fan);
		return fan;
	}

	/** The bridge's two grandstands, facing the bridge; a brand trio in the middle of each. */
	private static void seatBridge(ClientLevel level, int centre, int cz) {
		for (int side = -1; side <= 1; side += 2) {
			for (int row = 0; row < BridgeLayout.ROWS; row++) {
				double z = cz + 0.5 + side * BridgeLayout.rowZ(row);
				double y = BridgeLayout.Y + BridgeLayout.rowTop(row) + 1;
				int seats = (int) ((2 * BridgeLayout.STAND_HALF_X) / 1.35);
				for (int s = 0; s <= seats; s++) {
					double x = centre - BridgeLayout.STAND_HALF_X + 0.5 + s * 1.35;
					// Read from the bridge: on the north stand (facing south) left is east, on the south stand west.
					int mid = seats / 2, brand = row == 2 && Math.abs(s - mid) <= 1 ? side < 0 ? s - mid + 1 : 1 - (s - mid) : -1;
					long seed = s * 31L + side;
					if (hash(seed, row, 7, 99) > density && brand < 0) continue;
					if (brand < 0 && Math.floorMod(s, 8) == 0) continue;
					Fan fan = add(level, x + (brand >= 0 ? 0 : (hash(seed, row, 7, 98) - 0.5) * 0.3), y, z, x, cz + 0.5, hash(seed, row, 7, 96),
							hash(seed, row, 7, 95), hash(seed, row, 7, 94), hash(seed, row, 7, 93), hash(seed, row, 7, 92), brand);
					// The Mexican wave runs along each stand and back down the other.
					fan.seatAngle = (float) ((x - centre + BridgeLayout.STAND_HALF_X) * 4 + (side > 0 ? 180 : 0));
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
