package com.insidethecube.retromon.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.insidethecube.retromon.Retromon;
import com.insidethecube.retromon.engine.Move;
import com.insidethecube.retromon.engine.Type;
import com.insidethecube.retromon.game.MonDisplay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * The battle's stage: a set piece built for the fight, as the games had their battle backgrounds. It's put up
 * in the sky over where you are (on your screen only) and taken down after: a route's grass field ringed with
 * trees, a forest clearing, a cave's rock bowl, the water's edge, a gym floor or a town square, by where you
 * are. It's always bright day there. Your Pokémon stands near and to the left, theirs further off to the
 * right, seen from behind yours. Also the Poké Balls (thrown, shaking, sending out), the moves' effects, the
 * screen shaking on a hit, and the evolution and Pokédex showcase.
 */
public final class BattleScene {
	private BattleScene() {}

	private static boolean active, revealed;
	private static Vec3 cam = Vec3.ZERO, camTarget = Vec3.ZERO;
	private static float camYaw, camPitch;
	private static MonDisplay mine, theirs, ball, sendBall;
	private static int sendSide = -1;
	private static int hiddenNpc = -1;
	private static Vec3 centre = Vec3.ZERO, enemyAt = Vec3.ZERO, mineAt = Vec3.ZERO;
	private static int nextId = -77000;
	private static ClientLevel level;
	private static String kind = "route";
	private static int shake;
	private static final Random RNG = new Random();
	/** What was there before the stage went up, to put back after. */
	private static final Map<BlockPos, BlockState> BEFORE = new LinkedHashMap<>();

	/** Whether the battle camera is on (from the middle of the wipe into the battle to the end). */
	public static boolean active() {
		return active && revealed;
	}

	/** Whether a stage is up (it's day there whatever the time). */
	public static boolean staged() {
		return active;
	}

	public static void reveal() {
		revealed = true;
	}

	public static float fov() {
		return 52F;
	}

	public static boolean hides(Entity e) {
		return active && revealed && e.getId() == hiddenNpc;
	}

	/** 0: yours, 1: theirs (or the one evolving). */
	public static MonDisplay display(int side) {
		return side == 0 ? mine : theirs;
	}

	public static Vec3 spot(int side) {
		return side == 0 ? mineAt : enemyAt;
	}

	/** Sets out the stage: npc is the trainer you're battling (or -1), enemies the Pokémon you'll face, arena the kind of place. */
	public static void start(int npc, List<String> enemies, String arena) {
		end();
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) return;
		level = mc.level;
		hiddenNpc = npc;
		kind = arena == null ? "route" : arena;
		float h = 1;
		for (String sp : enemies) {
			BedrockModel.Def def = BedrockModel.def(sp);
			if (def != null) h = Math.max(h, def.height);
		}
		build(mc.player.blockPosition());
		double far = 6.0 + Math.max(0, h - 1.5) * 1.3;
		mineAt = centre.add(-1.3, 0, 2.6);
		enemyAt = centre.add(1.4, 0, 2.6 - far);
		if (kind.equals("water")) enemyAt = new Vec3(enemyAt.x, centre.y - 0.1, enemyAt.z);
		mine = spawn(mineAt, face(mineAt, enemyAt));
		theirs = spawn(enemyAt, face(enemyAt, mineAt));
		// Behind your Pokémon and a little above, looking across to theirs (the newer games' view).
		cam = centre.add(0.3, 2.3 + Math.max(0, h - 2) * 0.35, 8.2 + Math.max(0, h - 2) * 0.8);
		aim(mineAt.lerp(enemyAt, 0.6).add(0, 0.55 + h * 0.18, 0));
		active = true;
		revealed = false;
	}

	/** One Pokémon in front of you on a stage (evolving): builds a stage if there isn't one. */
	public static void showcase(String species) {
		if (!active) {
			Minecraft mc = Minecraft.getInstance();
			if (mc.player == null || mc.level == null) return;
			end();
			level = mc.level;
			kind = "route";
			build(mc.player.blockPosition());
			active = true;
		}
		spotlight(species);
		revealed = true;
	}

	/** The Pokémon caught (or evolving) stands in the middle, close to you, turning slowly. */
	public static void spotlight(String species) {
		if (level == null) return;
		for (MonDisplay d : new MonDisplay[] {mine, ball, sendBall}) if (d != null) level.removeEntity(d.getId(), Entity.RemovalReason.DISCARDED);
		mine = ball = sendBall = null;
		Vec3 at = centre.add(0, 0, -0.5);
		if (theirs == null) theirs = spawn(at, 0);
		theirs.snapTo(at.x, at.y, at.z, 0, 0);
		theirs.facing = 0;
		theirs.species = species;
		theirs.size = theirs.sizeOld = 0;
		theirs.sizeTarget = 1;
		enemyAt = at;
		BedrockModel.Def def = BedrockModel.def(species);
		float h = def == null ? 1 : def.height;
		cam = at.add(0, 0.9 + h * 0.45, 2.6 + h * 1.1);
		aim(at.add(0, h * 0.5, 0));
	}

	private static void aim(Vec3 look) {
		camTarget = look;
		Vec3 d = look.subtract(cam);
		camYaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
		camPitch = (float) Math.toDegrees(Math.atan2(-d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
	}

	// ---------- the Poké Balls ----------

	/** Throws a ball at their Pokémon (item: which ball). */
	public static void throwBall(String item) {
		if (!active || level == null) return;
		if (ball != null) level.removeEntity(ball.getId(), Entity.RemovalReason.DISCARDED);
		ball = ballAt(cam.add(-0.4, -0.7, -0.8), item);
		BedrockModel.Def def = BedrockModel.def(theirs == null ? "pidgey" : theirs.species);
		double top = def == null ? 0.8 : Math.min(2.5, def.height * 0.6);
		ball.fly(enemyAt.x, enemyAt.y + top, enemyAt.z, 1.4, 14);
		Sounds.ball();
	}

	private static MonDisplay ballAt(Vec3 p, String item) {
		MonDisplay b = spawn(p, 0);
		b.species = "poke_ball";
		b.texture = Retromon.ID + ":textures/pokemon/ball_" + (item == null || item.isEmpty() ? "poke_ball" : item) + ".png";
		b.size = b.sizeOld = b.sizeTarget = 1;
		return b;
	}

	/** The ball drops to the ground in front of where their Pokémon was. */
	public static void dropBall() {
		if (ball != null) ball.fly(enemyAt.x, enemyAt.y, enemyAt.z, 0.25, 8);
		burst(enemyAt.add(0, 0.6, 0), ParticleTypes.POOF, 10, 0.25);
	}

	public static void shakeBall() {
		if (ball != null) ball.wobble = 12;
	}

	/** The ball bursts open (it broke free). */
	public static void burstBall() {
		if (ball != null && level != null) {
			burst(ball.position().add(0, 0.3, 0), ParticleTypes.POOF, 12, 0.3);
			level.removeEntity(ball.getId(), Entity.RemovalReason.DISCARDED);
			ball = null;
		}
	}

	/** Caught: little stars over the shut ball. */
	public static void caughtStars() {
		if (ball != null) burst(ball.position().add(0, 0.5, 0), ParticleTypes.HAPPY_VILLAGER, 14, 0.4);
	}

	/** A Pokémon sent out: a ball arcs to its spot and pops (the Pokémon appears as it does). */
	public static void sendOut(int side) {
		if (!active || level == null) return;
		if (sendBall != null) level.removeEntity(sendBall.getId(), Entity.RemovalReason.DISCARDED);
		Vec3 to = spot(side);
		Vec3 from = side == 0 ? cam.add(0.6, -0.8, -0.6) : to.add(0.8, 1.6, -3.5);
		sendBall = ballAt(from, "poke_ball");
		sendBall.fly(to.x, to.y + 0.2, to.z, 1.1, 9);
		sendSide = side;
		Sounds.ball();
	}

	// ---------- the moves' effects ----------

	private record Fx(Vec3 from, Vec3 to, ParticleOptions particle, int life, boolean beam, int[] age, int density) {}

	private static final List<Fx> FX = new ArrayList<>();
	/** A flash of the move's colour over the screen (the battle screen draws it): colour, ticks left, length. */
	public static int flashColor, flashTicks, flashMax = 1;
	/** The camera's move for an attack: behind the attacker looking at the target, then back. */
	private static int shotSide = -1, shotTicks, shotMax = 1;
	/** A lightning bolt to come down on a target (Thunderbolt, Thunder): ticks to wait, and where. */
	private static int boltIn = -1;
	private static Vec3 boltAt = Vec3.ZERO;

	/** The colour of each type, for the flash on a hit. */
	public static int typeColor(Type t) {
		return switch (t) {
			case FIRE -> 0xFF8A2A;
			case WATER -> 0x3C8CFF;
			case GRASS -> 0x5CD65C;
			case ELECTRIC -> 0xFFE23A;
			case PSYCHIC -> 0xFF6FB7;
			case ICE -> 0xA8F0FF;
			case DRAGON -> 0x7A5CFF;
			case DARK -> 0x1A1420;
			case GHOST -> 0x7A4CC8;
			case POISON -> 0xB04CD8;
			case FIGHTING -> 0xE0502A;
			case GROUND -> 0xC8A050;
			case ROCK -> 0xB8A070;
			case BUG -> 0xA8D030;
			case FLYING -> 0xB8D8FF;
			case STEEL -> 0xC8D0E0;
			default -> 0xFFFFFF;
		};
	}

	/** Where a point in the world falls on the screen (width x height), or null if it's behind the camera. */
	public static int[] project(Vec3 p, int width, int height) {
		double[] pose = cameraPose(0);
		double yaw = Math.toRadians(pose[3]), pitch = Math.toRadians(pose[4]);
		double fx = -Math.sin(yaw) * Math.cos(pitch), fy = -Math.sin(pitch), fz = Math.cos(yaw) * Math.cos(pitch);
		double rx = -Math.cos(yaw), rz = -Math.sin(yaw);
		// up = right x forward
		double ux = 0 * fz - rz * fy, uy = rz * fx - rx * fz, uz = rx * fy - 0 * fx;
		double dx = p.x - pose[0], dy = p.y - pose[1], dz = p.z - pose[2];
		double x = dx * rx + dz * rz, y = dx * ux + dy * uy + dz * uz, z = dx * fx + dy * fy + dz * fz;
		if (z <= 0.1) return null;
		double t = Math.tan(Math.toRadians(fov()) / 2);
		return new int[] {(int) (width / 2.0 + x / z / t * height / 2.0), (int) (height / 2.0 - y / z / t * height / 2.0)};
	}

	/** How tall their Pokémon stands (for the catch ring). */
	public static float enemyHeight() {
		BedrockModel.Def def = BedrockModel.def(theirs == null ? "pidgey" : theirs.species);
		return def == null ? 1 : def.height;
	}

	public static Vec3 enemySpot() {
		return enemyAt;
	}

	/**
	 * A move's effect: the camera moves behind the attacker to watch it go, the move's colour flashes as it
	 * lands, and its particles by type: a stream for special moves, a burst for physical ones, sparkles for
	 * stat moves. The big ones have their own: a real bolt of lightning for Thunderbolt and Thunder, beams for
	 * the beam moves, the ground shaking for Earthquake, a fire storm, a wave, an explosion.
	 */
	public static void move(int side, String moveId) {
		if (!active() || moveId == null) return;
		Move m = Move.find(moveId);
		if (m == null) return;
		Vec3 me = spot(side).add(0, 0.6, 0), them = spot(1 - side).add(0, 0.6, 0);
		ParticleOptions p = particle(m.type());
		int density = 3 + Math.max(0, m.power()) / 25;
		if (m.power() <= 0) {
			// Status moves: on itself (raising its stats) or on the other one.
			boolean self = m.effect().contains("UP") || m.effect().contains("HEAL") || m.effect().contains("REFLECT") || m.effect().contains("LIGHT_SCREEN")
					|| m.effect().contains("SUBSTITUTE") || m.effect().contains("FOCUS") || m.effect().contains("REST") || m.effect().contains("PROTECT");
			FX.add(new Fx(self ? me : them, self ? me : them, self ? ParticleTypes.HAPPY_VILLAGER : p, 14, false, new int[1], 4));
			shot(side, 12);
			Sounds.sfx("shrink", self ? 1.2F : 0.9F, 0.6F);
			return;
		}
		boolean special = switch (m.type()) {
			case FIRE, WATER, GRASS, ELECTRIC, PSYCHIC, ICE, DRAGON, DARK -> true;
			default -> false;
		};
		String id = m.id();
		shot(side, special ? 24 : 18);
		if (id.equals("thunderbolt") || id.equals("thunder") || id.equals("zap_cannon")) {
			boltIn = 10;
			boltAt = spot(1 - side);
			FX.add(new Fx(them, them, ParticleTypes.ELECTRIC_SPARK, 20, false, new int[1], 10));
		} else if (id.contains("beam") || id.equals("solarbeam") || id.equals("dragonbreath") || id.equals("aurora_beam")) {
			FX.add(new Fx(me, them, ParticleTypes.END_ROD, 14, true, new int[1], 6));
			FX.add(new Fx(me, them, p, 14, true, new int[1], density + 4));
		} else if (id.equals("earthquake") || id.equals("magnitude") || id.equals("fissure")) {
			shake = 26;
			FX.add(new Fx(them, them, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()), 20, false, new int[1], 14));
			FX.add(new Fx(me, me, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()), 20, false, new int[1], 8));
		} else if (id.equals("explosion") || id.equals("selfdestruct")) {
			shake = 24;
			burst(me, ParticleTypes.EXPLOSION_EMITTER, 1, 0.1);
			flash(0xFFFFFF, 14);
		} else if (id.equals("flamethrower") || id.equals("fire_blast") || id.equals("fire_spin") || id.equals("sacred_fire")) {
			FX.add(new Fx(me, them, ParticleTypes.FLAME, 14, true, new int[1], 12));
			FX.add(new Fx(them, them, ParticleTypes.LAVA, 18, false, new int[1], 6));
		} else if (id.equals("surf") || id.equals("hydro_pump") || id.equals("whirlpool") || id.equals("bubblebeam")) {
			FX.add(new Fx(me, them, ParticleTypes.SPLASH, 14, true, new int[1], 14));
			FX.add(new Fx(them, them, ParticleTypes.FALLING_WATER, 18, false, new int[1], 10));
		} else if (id.equals("blizzard") || id.equals("gust") || id.equals("twister") || id.equals("razor_wind") || id.equals("whirlwind")) {
			FX.add(new Fx(them, them, m.type() == Type.ICE ? ParticleTypes.SNOWFLAKE : ParticleTypes.GUST, 20, false, new int[1], 8));
		} else if (special) {
			FX.add(new Fx(me, them, p, 12, true, new int[1], density));
		} else {
			FX.add(new Fx(them, them, p, 8, false, new int[1], density));
		}
		pendingFlash = typeColor(m.type());
		Sounds.sfx(sound(m), 1F, 0.7F);
	}

	/** The colour to flash when the hit lands (set by the move, used by hit()). */
	private static int pendingFlash = 0xFFFFFF;

	public static void flash(int rgb, int ticks) {
		flashColor = rgb;
		flashTicks = flashMax = ticks;
	}

	private static void shot(int side, int ticks) {
		shotSide = side;
		shotTicks = shotMax = ticks;
	}

	private static String sound(Move m) {
		return switch (m.type()) {
			case FIRE -> "ember";
			case WATER -> "water";
			case GRASS -> "vine";
			case ELECTRIC -> "thunder";
			case PSYCHIC -> "psybeam";
			case ICE -> "ice";
			case FLYING -> "gust";
			case POISON, BUG -> "peck";
			case FIGHTING, ROCK, GROUND, STEEL -> "pound";
			default -> "tackle";
		};
	}

	private static ParticleOptions particle(Type t) {
		return switch (t) {
			case FIRE -> ParticleTypes.FLAME;
			case WATER -> ParticleTypes.SPLASH;
			case GRASS -> ParticleTypes.CHERRY_LEAVES;
			case ELECTRIC -> ParticleTypes.ELECTRIC_SPARK;
			case PSYCHIC -> ParticleTypes.WITCH;
			case ICE -> ParticleTypes.SNOWFLAKE;
			case DRAGON -> ParticleTypes.REVERSE_PORTAL;
			case DARK -> ParticleTypes.SQUID_INK;
			case GHOST -> ParticleTypes.SOUL;
			case POISON -> ParticleTypes.WITCH;
			case FLYING -> ParticleTypes.SMALL_GUST;
			case FIGHTING -> ParticleTypes.ENCHANTED_HIT;
			case GROUND -> new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState());
			case ROCK -> new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState());
			case BUG -> ParticleTypes.SPORE_BLOSSOM_AIR;
			case STEEL -> ParticleTypes.WAX_OFF;
			default -> ParticleTypes.CRIT;
		};
	}

	/** A hit: the screen shakes (harder when it's super effective). */
	public static void hit(int side, int effect) {
		shake = Math.max(shake, effect > 0 ? 12 : 7);
		flash(pendingFlash, effect > 0 ? 12 : 8);
		burst(spot(side).add(0, 0.7, 0), ParticleTypes.CRIT, effect > 0 ? 18 : 8, 0.35);
	}

	/** Fainting: it sinks as it goes. */
	public static void faint(int side) {
		MonDisplay d = display(side);
		if (d == null) return;
		Vec3 p = spot(side);
		d.fly(p.x, p.y - 0.7, p.z, 0, 16);
		d.sizeTarget = 0;
		Sounds.faint();
	}

	private static void burst(Vec3 at, ParticleOptions p, int n, double spread) {
		if (level == null) return;
		for (int i = 0; i < n; i++) {
			level.addParticle(p, at.x + (RNG.nextDouble() - 0.5) * spread * 2, at.y + (RNG.nextDouble() - 0.5) * spread * 2,
					at.z + (RNG.nextDouble() - 0.5) * spread * 2, (RNG.nextDouble() - 0.5) * 0.1, RNG.nextDouble() * 0.08, (RNG.nextDouble() - 0.5) * 0.1);
		}
	}

	// ---------- building the stage ----------

	/** Where the stage goes: over you, clear of the highest ground around, and below the top of the world. */
	private static void build(BlockPos you) {
		BEFORE.clear();
		int top = you.getY();
		for (int dx = -16; dx <= 16; dx += 4) {
			for (int dz = -16; dz <= 16; dz += 4) top = Math.max(top, level.getHeight(Heightmap.Types.WORLD_SURFACE, you.getX() + dx, you.getZ() + dz));
		}
		int y = Math.min(level.getMaxY() - 20, top + 26);
		int cx = you.getX(), cz = you.getZ();
		centre = new Vec3(cx + 0.5, y + 1, cz + 0.5);
		RNG.setSeed(cx * 31L + cz);
		int r = 14;
		for (int dx = -r - 3; dx <= r + 3; dx++) {
			for (int dz = -r - 3; dz <= r + 3; dz++) {
				double d = Math.sqrt(dx * dx + dz * dz);
				if (d > r + 0.5) continue;
				floor(cx + dx, y, cz + dz, dx, dz, d);
			}
		}
		switch (kind) {
			case "forest" -> {
				ring(cx, y, cz, 10.5, 15, 9, true);
				ring(cx, y, cz, 13.5, 18, 11, true);
				plants(cx, y, cz, 6, 12, 0.35, Blocks.FERN.defaultBlockState(), Blocks.SHORT_GRASS.defaultBlockState(), Blocks.BROWN_MUSHROOM.defaultBlockState());
			}
			case "cave" -> rocks(cx, y, cz);
			case "gym" -> gym(cx, y, cz);
			case "water" -> {
				ring(cx, y, cz, 12.5, 9, 7, false);
				plants(cx, y, cz, 4, 9, 0.2, Blocks.SHORT_GRASS.defaultBlockState(), Blocks.SUGAR_CANE.defaultBlockState(), Blocks.LILY_PAD.defaultBlockState());
			}
			case "town" -> {
				ring(cx, y, cz, 12.5, 8, 7, false);
				plants(cx, y, cz, 7, 11, 0.25, Blocks.POPPY.defaultBlockState(), Blocks.DANDELION.defaultBlockState(), Blocks.SHORT_GRASS.defaultBlockState());
				for (int i = 0; i < 6; i++) {
					double a = Math.toRadians(i * 60 + 30);
					int lx = cx + (int) Math.round(Math.sin(a) * 9.5), lz = cz + (int) Math.round(Math.cos(a) * 9.5);
					if (lz - cz > 5) continue;
					set(lx, y + 1, lz, Blocks.OAK_FENCE.defaultBlockState());
					set(lx, y + 2, lz, Blocks.OAK_FENCE.defaultBlockState());
					set(lx, y + 3, lz, Blocks.LANTERN.defaultBlockState());
				}
			}
			default -> {
				ring(cx, y, cz, 11.5, 12, 8, false);
				ring(cx, y, cz, 14.5, 14, 10, false);
				plants(cx, y, cz, 6.5, 11, 0.45, Blocks.TALL_GRASS.defaultBlockState(), Blocks.SHORT_GRASS.defaultBlockState(), Blocks.DANDELION.defaultBlockState(),
						Blocks.POPPY.defaultBlockState(), Blocks.AZURE_BLUET.defaultBlockState());
			}
		}
	}

	private static void floor(int x, int y, int z, int dx, int dz, double d) {
		BlockState top;
		switch (kind) {
			case "cave" -> top = RNG.nextInt(5) == 0 ? Blocks.GRAVEL.defaultBlockState() : RNG.nextInt(4) == 0 ? Blocks.ANDESITE.defaultBlockState() : Blocks.STONE.defaultBlockState();
			case "gym" -> {
				boolean line = Math.abs(d - 7) < 0.5 || Math.abs(dz) == 0 && d < 7;
				top = line ? Blocks.WHITE_CONCRETE.defaultBlockState() : d < 7 ? (dz > 0 ? Blocks.LIGHT_BLUE_TERRACOTTA : Blocks.RED_TERRACOTTA).defaultBlockState()
						: Blocks.POLISHED_ANDESITE.defaultBlockState();
			}
			case "water" -> top = dz < -1 ? Blocks.WATER.defaultBlockState() : dz < 1 ? Blocks.SAND.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState();
			case "forest" -> top = RNG.nextInt(6) == 0 ? Blocks.PODZOL.defaultBlockState() : RNG.nextInt(8) == 0 ? Blocks.MOSS_BLOCK.defaultBlockState()
					: Blocks.GRASS_BLOCK.defaultBlockState();
			case "town" -> top = Math.abs(dx) <= 1 || Math.abs(dz) <= 1 ? Blocks.DIRT_PATH.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState();
			default -> top = Blocks.GRASS_BLOCK.defaultBlockState();
		}
		set(x, y, z, top);
		set(x, y - 1, z, top.is(Blocks.WATER) ? Blocks.SAND.defaultBlockState() : Blocks.DIRT.defaultBlockState());
		set(x, y - 2, z, Blocks.STONE.defaultBlockState());
	}

	/** A ring of trees at a radius (n of them, height h), leaving the space behind the camera open. */
	private static void ring(int cx, int y, int cz, double radius, int n, int h, boolean mixed) {
		for (int i = 0; i < n; i++) {
			double a = Math.toRadians(i * 360.0 / n + RNG.nextInt(10));
			int tx = cx + (int) Math.round(Math.sin(a) * radius), tz = cz + (int) Math.round(Math.cos(a) * radius);
			if (tz - cz > 6 && Math.abs(tx - cx) < 7) continue;
			int kindOf = mixed ? RNG.nextInt(3) : 0;
			BlockState log = kindOf == 1 ? Blocks.SPRUCE_LOG.defaultBlockState() : kindOf == 2 ? Blocks.BIRCH_LOG.defaultBlockState() : Blocks.OAK_LOG.defaultBlockState();
			BlockState leaves = (kindOf == 1 ? Blocks.SPRUCE_LEAVES : kindOf == 2 ? Blocks.BIRCH_LEAVES : Blocks.OAK_LEAVES).defaultBlockState()
					.setValue(LeavesBlock.PERSISTENT, true);
			tree(tx, y + 1, tz, h - 2 + RNG.nextInt(4), log, leaves);
		}
	}

	private static void tree(int x, int y, int z, int h, BlockState log, BlockState leaves) {
		for (int i = 0; i < h; i++) set(x, y + i, z, log);
		int top = y + h;
		for (int dx = -3; dx <= 3; dx++) {
			for (int dy = -3; dy <= 2; dy++) {
				for (int dz = -3; dz <= 3; dz++) {
					double d = Math.sqrt(dx * dx + dy * dy * 1.4 + dz * dz);
					if (d > 3.1 || (dx == 0 && dz == 0 && dy < 1)) continue;
					BlockPos p = new BlockPos(x + dx, top + dy, z + dz);
					if (level.getBlockState(p).isAir()) set(p.getX(), p.getY(), p.getZ(), leaves);
				}
			}
		}
	}

	/** Grass and flowers between two radii (none in the middle where they battle), with a chance per spot. */
	private static void plants(int cx, int y, int cz, double from, double to, double chance, BlockState... kinds) {
		for (int dx = (int) -to; dx <= to; dx++) {
			for (int dz = (int) -to; dz <= to; dz++) {
				double d = Math.sqrt(dx * dx + dz * dz);
				if (d < from || d > to || RNG.nextDouble() > chance) continue;
				BlockPos at = new BlockPos(cx + dx, y + 1, cz + dz);
				if (!level.getBlockState(at).isAir()) continue;
				BlockState below = level.getBlockState(at.below());
				BlockState p = kinds[RNG.nextInt(kinds.length)];
				if (p.is(Blocks.LILY_PAD)) {
					if (below.is(Blocks.WATER)) set(at.getX(), at.getY(), at.getZ(), p);
					continue;
				}
				if (below.is(Blocks.WATER)) continue;
				if (p.is(Blocks.TALL_GRASS)) {
					set(at.getX(), at.getY(), at.getZ(), p);
					set(at.getX(), at.getY() + 1, at.getZ(), p.setValue(net.minecraft.world.level.block.DoublePlantBlock.HALF,
							net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
				} else if (p.is(Blocks.SUGAR_CANE)) {
					if (!below.is(Blocks.SAND) && !below.is(Blocks.GRASS_BLOCK)) continue;
					set(at.getX(), at.getY(), at.getZ(), p);
					set(at.getX(), at.getY() + 1, at.getZ(), p);
				} else {
					set(at.getX(), at.getY(), at.getZ(), p);
				}
			}
		}
	}

	/** A cave: a bowl of rock walls of uneven height around a stone floor, with a few boulders. */
	private static void rocks(int cx, int y, int cz) {
		BlockState[] stone = {Blocks.STONE.defaultBlockState(), Blocks.ANDESITE.defaultBlockState(), Blocks.TUFF.defaultBlockState(),
				Blocks.COBBLESTONE.defaultBlockState()};
		for (int dx = -16; dx <= 16; dx++) {
			for (int dz = -16; dz <= 16; dz++) {
				double d = Math.sqrt(dx * dx + dz * dz);
				if (d < 10.5 || d > 14.5) continue;
				if (dz > 6 && Math.abs(dx) < 8) continue;
				int h = 4 + (int) (Math.abs(Math.sin(dx * 0.7) + Math.cos(dz * 0.6)) * 3) + RNG.nextInt(3);
				for (int i = 1; i <= h; i++) set(cx + dx, y + i, cz + dz, stone[RNG.nextInt(stone.length)]);
			}
		}
		for (int i = 0; i < 6; i++) {
			int bx = cx + RNG.nextInt(17) - 8, bz = cz + RNG.nextInt(9) - 9;
			if (Math.abs(bx - cx) < 3) continue;
			set(bx, y + 1, bz, stone[RNG.nextInt(stone.length)]);
		}
	}

	/** A gym: the battle floor in red and blue, and pillars with lights round the back. */
	private static void gym(int cx, int y, int cz) {
		for (int i = 0; i < 9; i++) {
			double a = Math.toRadians(100 + i * 20);
			int px = cx + (int) Math.round(Math.sin(a) * 12), pz = cz + (int) Math.round(Math.cos(a) * 12);
			for (int k = 1; k <= 6; k++) set(px, y + k, pz, Blocks.QUARTZ_PILLAR.defaultBlockState());
			set(px, y + 7, pz, Blocks.SEA_LANTERN.defaultBlockState());
		}
	}

	private static void set(int x, int y, int z, BlockState s) {
		BlockPos p = new BlockPos(x, y, z);
		if (!BEFORE.containsKey(p)) BEFORE.put(p, level.getBlockState(p));
		level.setBlock(p, s, 2 | 16, 0);
	}

	private static float face(Vec3 from, Vec3 to) {
		Vec3 d = to.subtract(from);
		return (float) Math.toDegrees(Math.atan2(-d.x, d.z));
	}

	private static MonDisplay spawn(Vec3 p, float yaw) {
		MonDisplay d = new MonDisplay(Retromon.MON_DISPLAY, level);
		d.setId(nextId--);
		d.snapTo(p.x, p.y, p.z, yaw, 0);
		d.facing = yaw;
		d.size = d.sizeOld = d.sizeTarget = 0;
		level.addEntity(d);
		return d;
	}

	public static void end() {
		if (level != null) {
			for (MonDisplay d : new MonDisplay[] {mine, theirs, ball, sendBall}) if (d != null) level.removeEntity(d.getId(), Entity.RemovalReason.DISCARDED);
			List<Map.Entry<BlockPos, BlockState>> back = new ArrayList<>(BEFORE.entrySet());
			for (int i = back.size() - 1; i >= 0; i--) level.setBlock(back.get(i).getKey(), back.get(i).getValue(), 2 | 16, 0);
		}
		BEFORE.clear();
		FX.clear();
		shotTicks = flashTicks = 0;
		boltIn = -1;
		mine = theirs = ball = sendBall = null;
		hiddenNpc = -1;
		active = revealed = false;
		level = null;
		shake = 0;
	}

	public static void tick(Minecraft mc) {
		// If the world changed under a battle (left the game), forget it.
		if (active && mc.level != level) {
			level = null;
			mine = theirs = ball = sendBall = null;
			BEFORE.clear();
			FX.clear();
			active = revealed = false;
			return;
		}
		if (!active || level == null) return;
		if (shake > 0) shake--;
		if (flashTicks > 0) flashTicks--;
		if (shotTicks > 0) shotTicks--;
		if (boltIn >= 0 && boltIn-- == 0) {
			// A real bolt of lightning on the target (just on your screen): its flash, its thunder.
			net.minecraft.world.entity.LightningBolt bolt = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(level,
					net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
			if (bolt != null) {
				bolt.setVisualOnly(true);
				bolt.setId(nextId--);
				bolt.snapTo(boltAt.x, boltAt.y, boltAt.z, 0, 0);
				level.addEntity(bolt);
			}
			flash(0xFFE23A, 10);
		}
		if (sendBall != null && !sendBall.flying()) {
			burst(sendBall.position().add(0, 0.4, 0), ParticleTypes.POOF, 12, 0.3);
			Sounds.poof();
			level.removeEntity(sendBall.getId(), Entity.RemovalReason.DISCARDED);
			sendBall = null;
		}
		for (int i = FX.size() - 1; i >= 0; i--) {
			Fx f = FX.get(i);
			int age = ++f.age()[0];
			if (f.beam()) {
				// The stream travels from the attacker to the target, then bursts there.
				double t = Math.min(1, age / (double) f.life());
				Vec3 at = f.from().lerp(f.to(), t);
				for (int k = 0; k < f.density(); k++) {
					level.addParticle(f.particle(), at.x + (RNG.nextDouble() - 0.5) * 0.3, at.y + (RNG.nextDouble() - 0.5) * 0.3, at.z + (RNG.nextDouble() - 0.5) * 0.3, 0, 0.01, 0);
				}
				if (age == f.life()) burst(f.to(), f.particle(), 16, 0.4);
			} else if (age % 2 == 0) {
				burst(f.to(), f.particle(), f.density(), 0.45);
			}
			if (age >= f.life()) FX.remove(i);
		}
	}

	public static double[] cameraPose(float partial) {
		double sx = 0, sy = 0;
		if (shake > 0) {
			double k = shake * 0.012;
			sx = (RNG.nextDouble() - 0.5) * k;
			sy = (RNG.nextDouble() - 0.5) * k;
		}
		double x = cam.x, y = cam.y, z = cam.z, yaw = camYaw, pitch = camPitch;
		if (shotTicks > 0 && shotSide >= 0) {
			// Behind the attacker and a little to the side, watching its move go across; in and out smoothly.
			float t = 1 - (shotTicks - partial) / shotMax;
			double w = Math.sin(Math.PI * Math.max(0, Math.min(1, t)));
			w = w * w * 0.85;
			Vec3 a = spot(shotSide), b = spot(1 - shotSide);
			Vec3 dir = new Vec3(b.x - a.x, 0, b.z - a.z).normalize();
			Vec3 side = new Vec3(-dir.z, 0, dir.x);
			Vec3 at = a.subtract(dir.scale(3.0)).add(side.scale(1.1)).add(0, 1.6, 0);
			Vec3 look = b.add(0, 0.7, 0).subtract(at);
			double sYaw = Math.toDegrees(Math.atan2(-look.x, look.z)), sPitch = Math.toDegrees(Math.atan2(-look.y, Math.sqrt(look.x * look.x + look.z * look.z)));
			double dy = ((sYaw - yaw) % 360 + 540) % 360 - 180;
			x += (at.x - x) * w;
			y += (at.y - y) * w;
			z += (at.z - z) * w;
			yaw += dy * w;
			pitch += (sPitch - pitch) * w;
		}
		return new double[] {x + sx, y + sy, z, yaw, pitch};
	}
}
