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
 * The crowd is never still: every fan keeps choosing something to do (sitting watching, chatting, eating a
 * snack, clapping, waving, cheering, jumping, pumping a fist or a sign or a flag, now and then a floss or a
 * dab), more and bigger the more excited the crowd is, and every head follows you round the floor. Now and
 * then a Mexican wave goes round the stands. What happens in the fight (hits, kills, goals, the win, your big
 * hits taken) comes from the server and sets the crowd off, with its sound: a murmur all the time, cheers, a
 * roar for "FIGHT!", an uproar and applause for a win, an "ooh", an "aww", and boos.
 * <p>
 * On the bridge the fans take sides: mostly blue's at blue's end, red's at red's, in their team's shirts,
 * with their team's signs and flags. When a team scores its fans go wild and the other side's boo.
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
	/** On the bridge, how much each team's fans are cheering (their side scored) and booing (scored on); in the stadium, booing. */
	private static final float[] teamCheer = new float[2], teamBoo = new float[2];
	private static float homeBoo;
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
		return mode == Payloads.State.BRIDGE || mode == Payloads.State.BRIDGE_RING;
	}

	/** On the bridge with the crowd in the ring stadium round it all. */
	public static boolean ring() {
		return mode == Payloads.State.BRIDGE_RING;
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
				homeBoo = 0.7F;
				play(ArenaSounds.AWW, loud * 0.9F, 0);
				play(ArenaSounds.BOO, loud * 0.5F, 25);
				play(ArenaSounds.APPLAUSE_LIGHT, loud * 0.5F, 60);
			}
			case Payloads.Cheer.GOAL -> {
				// A goal: the scorers' fans go wild, the other side's boo (and you hear both, yours the louder).
				int team = Math.round(strength);
				if (team < 0 || team > 1) return;
				teamCheer[team] = 1;
				teamBoo[1 - team] = 1;
				cheer = Math.max(cheer, 0.7F);
				boolean yours = BridgeHud.you() < 0 || BridgeHud.you() == team;
				play(ArenaSounds.CHEER, loud * (yours ? 1.0F : 0.75F), 0);
				play(ArenaSounds.ROAR, loud * 0.5F, 4);
				play(ArenaSounds.BOO, loud * (yours ? 0.45F : 0.8F), 12);
			}
			case Payloads.Cheer.END -> {
				int team = Math.round(strength);
				cheer = 1;
				party = 230;
				if (team == 0 || team == 1) {
					teamCheer[team] = 1;
					teamBoo[1 - team] = 1;
					play(ArenaSounds.UPROAR, loud, 0);
					play(ArenaSounds.APPLAUSE, loud * 0.8F, 30);
					play(ArenaSounds.BOO, loud * 0.45F, 20);
				} else {
					play(ArenaSounds.APPLAUSE, loud * 0.8F, 0);
				}
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
		return Math.min(1, Math.max(Math.max(base, cheer), Math.max(Math.max(teamCheer[0], teamCheer[1]), Math.max(teamBoo[0], teamBoo[1]) * 0.8F)));
	}

	public static float cheering() {
		return cheer;
	}

	/** How much a fan on this side is cheering (a fan on neither side, or yours in the stadium: anything good). */
	public static float cheeringFor(int team) {
		if (team < 0) return Math.max(cheer, Math.max(teamCheer[0], teamCheer[1]));
		return Math.max(teamCheer[team], BridgeHud.you() == team ? cheer : cheer * 0.25F);
	}

	/** How much a fan on this side is booing. */
	public static float booingFor(int team) {
		return team < 0 ? homeBoo : teamBoo[team];
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
		homeBoo *= 0.97F;
		for (int t = 0; t < 2; t++) {
			teamCheer[t] *= party > 0 ? 0.995F : 0.976F;
			teamBoo[t] *= party > 0 ? 0.99F : 0.972F;
		}
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
		BlockPos check = ring() ? new BlockPos(centre, BridgeLayout.Y + BridgeLayout.ringRowTop(2), centreZ + (int) BridgeLayout.ringRowDist(2))
				: onBridge() ? new BlockPos(centre, BridgeLayout.Y + BridgeLayout.rowTop(2), centreZ + (int) BridgeLayout.rowZ(2))
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
				if (ring()) seatRing(level, centre, centreZ);
				else if (onBridge()) seatBridge(level, centre, centreZ);
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
							hash(s, row, tier, 92), brand, -1, hash(s, row, tier, 91));
					fan.seatAngle = (float) Math.toDegrees(rad);
				}
			}
		}
	}

	/** Snacks a fan might have (and eat now and then). */
	private static final net.minecraft.world.item.Item[] SNACKS = {Items.COOKIE, Items.BREAD, Items.APPLE, Items.COOKED_CHICKEN, Items.MELON_SLICE,
			Items.PUMPKIN_PIE, Items.BAKED_POTATO, Items.HONEY_BOTTLE, Items.GLOW_BERRIES, Items.CARROT};

	/**
	 * A fan at (x, y, z) facing (fx, fz): its look, sign (about one in eleven holds one, GG most often), a
	 * spyglass now and then, its beat and height, from its seat's random numbers. Brand: 0, 1, 2 for the
	 * three boards that spell INSID3 TH3 CUB3, or -1. Team: whose side it's on (0 blue, 1 red, -1 yours or
	 * neither): half of a team's fans wear its shirt, and its signs and flags are the team's. More: one more
	 * random number, for its snack, its flag and whether it likes to sit.
	 */
	private static Fan add(ClientLevel level, double x, double y, double z, double fx, double fz, double look, double signRoll, double signPick, double phase,
			double height, int brand, int team, double more) {
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
		fan.team = team;
		// Its look: one of the eight, or (half of a team's fans) its team's shirt.
		fan.look = team >= 0 && look < 0.5 ? 8 + team * 2 + (int) (look * 4) % 2 : (int) (look * 8);
		if (brand >= 0) fan.sign = FanModel.BIG + brand;
		else if (signRoll < 0.1) {
			if (team >= 0 && signPick < 0.45) {
				// The team's: GO! on its colour, or a big BLUE! or RED!.
				fan.sign = signPick < 0.25 ? (team == 0 ? FanModel.SIGN_GO_BLUE : FanModel.SIGN_GO_RED) : (team == 0 ? FanModel.BIG_BLUE : FanModel.BIG_RED);
			} else if (signPick < 0.5) fan.sign = 2;
			else fan.sign = (int) ((signPick - 0.5) / 0.5 * FanModel.SIGN_GO_BLUE);
		} else fan.sign = -1;
		double m1 = (more * 7.13) % 1.0, m2 = (more * 13.7) % 1.0, m3 = (more * 29.3) % 1.0;
		fan.sits = m1 < 0.55;
		if (fan.sign < 0 && m2 < 0.035) {
			// A flag: its team's banner (or a white or a yellow one, for a fan on neither side).
			fan.flag = true;
			fan.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(team == 0 ? Items.BLUE_BANNER : team == 1 ? Items.RED_BANNER : m3 < 0.5 ? Items.WHITE_BANNER
					: Items.YELLOW_BANNER));
		} else if (fan.sign < 0 && m2 < 0.17) {
			fan.snack = true;
			fan.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(SNACKS[(int) (m3 * SNACKS.length) % SNACKS.length]));
		}
		if (fan.sign < 0 && !fan.snack && !fan.flag && signRoll > 0.985) {
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
							hash(seed, row, 7, 95), hash(seed, row, 7, 94), hash(seed, row, 7, 93), hash(seed, row, 7, 92), brand,
							side(x - centre, hash(seed, row, 7, 90)), hash(seed, row, 7, 91));
					// The Mexican wave runs along each stand and back down the other.
					fan.seatAngle = (float) ((x - centre + BridgeLayout.STAND_HALF_X) * 4 + (side > 0 ? 180 : 0));
				}
			}
		}
	}

	/** Whose side a fan this far along the bridge is on: mostly the team whose end it's at, a few the other's, a few neither. */
	private static int side(double along, double r) {
		int near = along < 0 ? 0 : 1;
		double lean = Math.min(1, Math.abs(along) / 30.0);
		if (r < 0.06) return -1;
		return r < 0.06 + 0.94 * (0.55 + 0.35 * lean) ? near : 1 - near;
	}

	/**
	 * The ring stadium round the whole map: its lower and upper tiers, every row round the straights and the
	 * ends, facing in; a brand trio in the middle of each long side; the wave going round and round.
	 */
	private static void seatRing(ClientLevel level, int centre, int cz) {
		int rows = BridgeLayout.ringRows();
		// (A big ring: fewer to a row than the other stands, so a packed ring stays smooth.)
		double fill = density * 0.55;
		for (int row = 0; row < rows; row++) {
			double r = BridgeLayout.ringRowDist(row);
			double y = BridgeLayout.Y + BridgeLayout.ringRowTop(row) + 1;
			double straight = 2 * BridgeLayout.RING_L, arc = Math.PI * r, perimeter = 2 * straight + 2 * arc;
			int seats = (int) (perimeter / 1.45);
			for (int s = 0; s < seats; s++) {
				double u = s * perimeter / seats;
				double x, z, fx, fz;
				// Along the north side (west to east), round the east end, back along the south side, round the west end.
				if (u < straight) {
					x = -BridgeLayout.RING_L + u;
					z = -r;
					fx = x;
					fz = 0;
				} else if (u < straight + arc) {
					double a = -Math.PI / 2 + (u - straight) / r;
					x = BridgeLayout.RING_L + Math.cos(a) * r;
					z = Math.sin(a) * r;
					fx = BridgeLayout.RING_L;
					fz = 0;
				} else if (u < 2 * straight + arc) {
					x = BridgeLayout.RING_L - (u - straight - arc);
					z = r;
					fx = x;
					fz = 0;
				} else {
					double a = Math.PI / 2 + (u - 2 * straight - arc) / r;
					x = -BridgeLayout.RING_L + Math.cos(a) * r;
					z = Math.sin(a) * r;
					fx = -BridgeLayout.RING_L;
					fz = 0;
				}
				// Aisles: a gap every so often, all the way up.
				if (Math.floorMod((int) (u / 1.45), 28) == 0) continue;
				int mid = (int) (straight / 2 / 1.45);
				// (Read from the bridge, left to right: west to east on the north side, east to west on the south.)
				int brand = row == 3 && Math.abs(s - mid) <= 1 ? s - mid + 1 : -1;
				if (row == 3 && brand < 0 && u > straight + arc && u < 2 * straight + arc) {
					int m2 = (int) ((straight + arc + straight / 2) / (perimeter / seats));
					if (Math.abs(s - m2) <= 1) brand = s - m2 + 1;
				}
				long seed = s * 131L + row * 7L;
				if (hash(seed, row, 11, 99) > fill && brand < 0) continue;
				Fan fan = add(level, centre + x + 0.5 + (brand >= 0 ? 0 : (hash(seed, row, 11, 98) - 0.5) * 0.3), y, cz + z + 0.5, centre + fx + 0.5, cz + fz + 0.5,
						hash(seed, row, 11, 96), hash(seed, row, 11, 95), hash(seed, row, 11, 94), hash(seed, row, 11, 93), hash(seed, row, 11, 92), brand,
						side(x, hash(seed, row, 11, 90)), hash(seed, row, 11, 91));
				fan.seatAngle = (float) (u / perimeter * 360);
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
