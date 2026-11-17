package com.insidethecube.retromon.client;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.insidethecube.retromon.Retromon;
import com.insidethecube.retromon.game.MonDisplay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A battle on the spot: your Pokémon and the other one stand on the ground where you are, yours near and to
 * the left, theirs further off and to the right (as the games lay it out), seen from just behind you. The way
 * it faces is the most open way round you; tall grass, flowers and the like are flattened while you battle
 * (only on your screen) and come back after. Also the Poké Ball's throw, and the evolution showcase.
 */
public final class BattleScene {
	private BattleScene() {}

	private static boolean active, revealed;
	private static Vec3 cam = Vec3.ZERO;
	private static float camYaw, camPitch;
	private static MonDisplay mine, theirs, ball;
	private static int hiddenNpc = -1;
	private static Vec3 origin = Vec3.ZERO, fwd = new Vec3(0, 0, 1), right = new Vec3(-1, 0, 0), enemyAt = Vec3.ZERO, mineAt = Vec3.ZERO;
	private static int nextId = -77000;
	private static ClientLevel level;
	/** Plants flattened for the battle, to put back after. */
	private static final Map<BlockPos, BlockState> CLEARED = new LinkedHashMap<>();

	/** Whether the battle camera is on (from the middle of the wipe into the battle to the end). */
	public static boolean active() {
		return active && revealed;
	}

	public static void reveal() {
		revealed = true;
	}

	public static float fov() {
		return 55F;
	}

	public static boolean hides(Entity e) {
		return active && revealed && e.getId() == hiddenNpc;
	}

	/** 0: yours, 1: theirs (or the one evolving). */
	public static MonDisplay display(int side) {
		return side == 0 ? mine : theirs;
	}

	/** Sets out the field: npc is the trainer you're battling (or -1), enemies the Pokémon you'll face. */
	public static void start(int npc, List<String> enemies) {
		end();
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) return;
		level = mc.level;
		hiddenNpc = npc;
		origin = mc.player.position();
		float prefer = mc.player.getYRot();
		Entity trainer = npc >= 0 ? level.getEntity(npc) : null;
		if (trainer != null) {
			Vec3 d = trainer.position().subtract(origin);
			prefer = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
		}
		// Room for the tallest of them (Onix needs the field set further back).
		float h = 1;
		for (String sp : enemies) {
			BedrockModel.Def def = BedrockModel.def(sp);
			if (def != null) h = Math.max(h, def.height);
		}
		double far = 7.0 + Math.max(0, h - 1.5) * 1.4;
		float yaw = openest(prefer, far);
		double r = Math.toRadians(yaw);
		fwd = new Vec3(-Math.sin(r), 0, Math.cos(r));
		right = new Vec3(-Math.cos(r), 0, -Math.sin(r));
		mineAt = at(2.6, -1.0);
		enemyAt = at(far, 1.5);
		clearPlants(far);
		mine = spawn(mineAt, face(mineAt, enemyAt));
		theirs = spawn(enemyAt, face(enemyAt, mineAt));
		// Just behind and above where you stand (you step out of the picture), looking between the two.
		cam = origin.subtract(fwd.scale(1.2)).add(right.scale(-0.4)).add(0, 1.9 + Math.max(0, h - 2) * 0.35, 0);
		Vec3 look = mineAt.lerp(enemyAt, 0.58).add(0, 0.45 + h * 0.2, 0);
		Vec3 d = look.subtract(cam);
		camYaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
		camPitch = (float) Math.toDegrees(Math.atan2(-d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
		active = true;
		revealed = false;
	}

	/** One Pokémon in front of you (the evolution scene). */
	public static void showcase(String species) {
		end();
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) return;
		level = mc.level;
		origin = mc.player.position();
		float yaw = openest(mc.player.getYRot(), 4);
		double r = Math.toRadians(yaw);
		fwd = new Vec3(-Math.sin(r), 0, Math.cos(r));
		right = new Vec3(-Math.cos(r), 0, -Math.sin(r));
		enemyAt = at(3.2, 0);
		clearPlants(4);
		theirs = spawn(enemyAt, face(enemyAt, origin));
		theirs.species = species;
		theirs.size = theirs.sizeOld = 0;
		theirs.sizeTarget = 1;
		BedrockModel.Def def = BedrockModel.def(species);
		float h = def == null ? 1 : def.height;
		cam = origin.subtract(fwd.scale(0.6)).add(0, 1.2 + h * 0.3, 0);
		Vec3 d = enemyAt.add(0, h * 0.45, 0).subtract(cam);
		camYaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
		camPitch = (float) Math.toDegrees(Math.atan2(-d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
		active = revealed = true;
	}

	// ---------- the Poké Ball ----------

	/** Throws a ball at their Pokémon (texture: which ball). */
	public static void throwBall(String item) {
		if (!active || level == null) return;
		if (ball != null) level.removeEntity(ball.getId(), Entity.RemovalReason.DISCARDED);
		Vec3 from = cam.add(fwd.scale(1.0)).add(0, -0.6, 0);
		ball = spawn(from, 0);
		ball.species = "poke_ball";
		ball.texture = Retromon.ID + ":textures/pokemon/ball_" + (item == null || item.isEmpty() ? "poke_ball" : item) + ".png";
		ball.size = ball.sizeOld = ball.sizeTarget = 1;
		BedrockModel.Def def = BedrockModel.def(theirs == null ? "pidgey" : theirs.species);
		double top = def == null ? 0.8 : Math.min(2.5, def.height * 0.6);
		ball.fly(enemyAt.x, enemyAt.y + top, enemyAt.z, 1.4, 14);
	}

	/** The ball drops to the ground in front of where their Pokémon was. */
	public static void dropBall() {
		if (ball != null) ball.fly(enemyAt.x, enemyAt.y, enemyAt.z, 0.25, 8);
	}

	public static void shakeBall() {
		if (ball != null) ball.wobble = 12;
	}

	/** The ball bursts open (it broke free) or stays shut (caught: it stays until the end). */
	public static void burstBall() {
		if (ball != null && level != null) {
			level.removeEntity(ball.getId(), Entity.RemovalReason.DISCARDED);
			ball = null;
		}
	}

	// ---------- setting out the field ----------

	private static Vec3 at(double ahead, double side) {
		Vec3 p = origin.add(fwd.scale(ahead)).add(right.scale(side));
		return new Vec3(p.x, ground(p.x, origin.y, p.z), p.z);
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

	/** Plants on the field and between you and it (grass, flowers, ferns, bushes, crops), cleared on your screen. */
	private static void clearPlants(double far) {
		CLEARED.clear();
		for (double a = -1.5; a <= far + 1.5; a += 0.5) {
			for (double side = -2.5; side <= 3.0; side += 0.5) {
				Vec3 p = origin.add(fwd.scale(a)).add(right.scale(side));
				for (int dy = -1; dy <= 2; dy++) {
					BlockPos at = BlockPos.containing(p.x, origin.y + dy, p.z);
					if (CLEARED.containsKey(at)) continue;
					BlockState st = level.getBlockState(at);
					if (plant(st)) {
						CLEARED.put(at.immutable(), st);
						level.setBlock(at, Blocks.AIR.defaultBlockState(), 2 | 16, 0);
					}
				}
			}
		}
	}

	private static boolean plant(BlockState s) {
		return s.is(Blocks.SHORT_GRASS) || s.is(Blocks.TALL_GRASS) || s.is(Blocks.FERN) || s.is(Blocks.LARGE_FERN) || s.is(BlockTags.FLOWERS)
				|| s.is(Blocks.DEAD_BUSH) || s.is(Blocks.SWEET_BERRY_BUSH) || s.is(BlockTags.CROPS) || s.is(Blocks.SUGAR_CANE) || s.is(Blocks.BUSH)
				|| s.is(Blocks.SHORT_DRY_GRASS) || s.is(Blocks.TALL_DRY_GRASS) || s.is(Blocks.FIREFLY_BUSH) || s.is(BlockTags.SAPLINGS);
	}

	private static void restorePlants() {
		if (level == null) return;
		for (Map.Entry<BlockPos, BlockState> e : CLEARED.entrySet()) {
			if (level.getBlockState(e.getKey()).isAir()) level.setBlock(e.getKey(), e.getValue(), 2 | 16, 0);
		}
		CLEARED.clear();
	}

	public static void end() {
		if (level != null) {
			for (MonDisplay d : new MonDisplay[] {mine, theirs, ball}) if (d != null) level.removeEntity(d.getId(), Entity.RemovalReason.DISCARDED);
			restorePlants();
		}
		mine = theirs = ball = null;
		hiddenNpc = -1;
		active = revealed = false;
		level = null;
	}

	/** If the world changed under a battle (left the game), forget it. */
	public static void tick(Minecraft mc) {
		if (active && mc.level != level) {
			level = null;
			mine = theirs = ball = null;
			CLEARED.clear();
			active = revealed = false;
		}
	}

	public static double[] cameraPose(float partial) {
		return new double[] {cam.x, cam.y, cam.z, camYaw, camPitch};
	}

	// ---------- the lie of the land ----------

	private static boolean solid(BlockPos p) {
		BlockState s = level.getBlockState(p);
		return !s.getCollisionShape(level, p).isEmpty();
	}

	/** The top of the ground near this height (within a few blocks), so they stand on it, not in it. */
	private static double ground(double x, double y, double z) {
		BlockPos base = BlockPos.containing(x, y, z);
		for (int dy = 3; dy >= -4; dy--) {
			BlockPos p = base.offset(0, dy, 0);
			if (solid(p) && !solid(p.above()) && !solid(p.above(2))) {
				VoxelShape shape = level.getBlockState(p).getCollisionShape(level, p);
				return p.getY() + shape.max(Direction.Axis.Y);
			}
		}
		return y;
	}

	/** Of the eight ways round, the most open one out to the far Pokémon, the way you face first. */
	private static float openest(float prefer, double far) {
		float best = Math.round(prefer / 45F) * 45F;
		int bestScore = Integer.MIN_VALUE;
		for (int i = 0; i < 8; i++) {
			float yaw = Math.round(prefer / 45F) * 45F + (i % 2 == 0 ? 1 : -1) * ((i + 1) / 2) * 45F;
			double r = Math.toRadians(yaw);
			double fx = -Math.sin(r), fz = Math.cos(r), rx = -Math.cos(r), rz = -Math.sin(r);
			int score = 0;
			for (double a = -1.5; a <= far + 1; a += 0.5) {
				for (double side : new double[] {-1.0, 0, 1.5}) {
					double x = origin.x + fx * a + rx * side, z = origin.z + fz * a + rz * side;
					BlockPos feet = BlockPos.containing(x, origin.y + 0.3, z);
					if (!solid(feet) && !solid(feet.above())) score++;
					else score -= 2;
					double gy = ground(x, origin.y, z);
					if (Math.abs(gy - origin.y) > 2.5) score--;
				}
			}
			score = score * 4 - i;
			if (score > bestScore) {
				bestScore = score;
				best = yaw;
			}
		}
		return Mth.wrapDegrees(best);
	}
}
