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
import net.minecraft.core.particles.DustParticleOptions;
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

	/** Whether a stage is up. */
	public static boolean staged() {
		return active;
	}

	/** The world's time of day when the battle began: it holds there on your screen until it's over (the server holds it too). */
	public static long heldDay = -1;

	public static void reveal() {
		if (active && !built) setUp();
		revealed = true;
	}

	public static float fov() {
		return 52F;
	}

	public static boolean hides(Entity e) {
		if (!active || !revealed) return false;
		if (e.getId() == hiddenNpc) return true;
		// Inside (no stage: the room itself), the room's people and POKéMON step out of the picture too, so no one
		// stands in the way (the stage's own you and them have ids below 0).
		return kind.equals("inside") && e.getId() >= 0 && !(e instanceof net.minecraft.world.entity.player.Player);
	}

	/** 0: yours, 1: theirs (or the one evolving). */
	public static MonDisplay display(int side) {
		return side == 0 ? mine : theirs;
	}

	public static Vec3 spot(int side) {
		return side == 0 ? mineAt : enemyAt;
	}

	/** Where the stage goes up, how tall their tallest is, and who's battling you (their key and role), until it's built. */
	private static BlockPos stageAt = BlockPos.ZERO;
	private static float stageH = 1;
	private static String foeKey;
	private static int foeRole = com.insidethecube.retromon.game.Npc.TRAINER;
	private static boolean built;
	/** You, standing behind yours; their trainer behind theirs (on the stage only). */
	private static com.insidethecube.retromon.game.Npc hero, foe;

	/**
	 * A battle's starting: npc is the trainer you're battling (or -1), enemies the Pokémon you'll face, arena the
	 * kind of place. The stage itself goes up under the white of the wipe (reveal), so nothing shows (or casts a
	 * shader's shadow) before it covers the screen.
	 */
	public static void start(int npc, List<String> enemies, String arena) {
		end();
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) return;
		level = mc.level;
		heldDay = mc.level.getDayTime();
		hiddenNpc = npc;
		kind = arena == null ? "route" : arena;
		float h = 1;
		for (String sp : enemies) {
			BedrockModel.Def def = BedrockModel.def(sp);
			if (def != null) h = Math.max(h, def.height);
		}
		stageH = h;
		stageAt = mc.player.blockPosition();
		Entity t = npc >= 0 ? mc.level.getEntity(npc) : null;
		foeKey = t instanceof com.insidethecube.retromon.game.Npc n ? n.key() : null;
		foeRole = t instanceof com.insidethecube.retromon.game.Npc n ? n.role() : com.insidethecube.retromon.game.Npc.TRAINER;
		built = false;
		active = true;
		revealed = false;
	}

	/** The battle's own directions: to your right, and back behind you (away from them). A stage's are the world's. */
	private static Vec3 axRight = new Vec3(1, 0, 0), axBack = new Vec3(0, 0, 1);

	/** A point offset from another along the battle's directions: right, up, back. */
	private static Vec3 local(Vec3 from, double right, double up, double back) {
		return from.add(axRight.scale(right)).add(0, up, 0).add(axBack.scale(back));
	}

	/**
	 * Battling inside a building (your rival in the lab): no stage. It's fought where you stand, in the room itself
	 * (as Let's Go has it), the two of you where you are, your Pokémon out between, and the camera behind yours,
	 * kept inside the room's walls.
	 */
	private static void setUpInside() {
		Minecraft mc = Minecraft.getInstance();
		com.insidethecube.retromon.game.Interiors.Room room = com.insidethecube.retromon.game.Interiors.roomOf(mc.player);
		com.insidethecube.retromon.game.Interiors.Arena arena = room == null ? null : com.insidethecube.retromon.game.Interiors.ARENAS.get(room.id());
		if (arena != null) {
			setUpArena(arena);
			return;
		}
		float h = stageH;
		Vec3 you = mc.player.position();
		Entity t = hiddenNpc >= 0 ? level.getEntity(hiddenNpc) : null;
		Vec3 look = Vec3.directionFromRotation(0, mc.player.getYRot()).multiply(1, 0, 1);
		Vec3 them = t != null ? t.position() : you.add(look.normalize().scale(6));
		Vec3 f = them.subtract(you).multiply(1, 0, 1);
		if (f.lengthSqr() < 0.25) f = look.lengthSqr() < 1e-4 ? new Vec3(0, 0, -1) : look;
		double dist = f.length();
		// Looking into the room, towards its back wall (the professor's end of the lab), never out through the door
		// at the black outside: the rooms are open to the south for the camera.
		f = roomAxis(you.lerp(them, 0.5), new Vec3(0, 0, -1), Math.max(1.7, dist / 2 - 1.0));
		axBack = f.scale(-1);
		axRight = new Vec3(-f.z, 0, f.x);
		Vec3 mid = you.lerp(them, 0.5);
		double half = Math.max(1.7, dist / 2 - 1.0);
		centre = new Vec3(mid.x, you.y, mid.z);
		mineAt = local(centre, -0.35, 0, half);
		enemyAt = local(centre, 0.5, 0, -half - Math.max(0, h - 1.5) * 0.4);
		mine = spawn(mineAt, face(mineAt, enemyAt));
		theirs = spawn(enemyAt, face(enemyAt, mineAt));
		Vec3 target = mineAt.lerp(enemyAt, 0.5).add(0, 0.8 + Math.max(0, h - 1) * 0.15, 0);
		Vec3 want = local(mineAt, -1.2, 2.0 + Math.max(0, h - 2) * 0.4, 4.6 + Math.max(0, h - 2) * 0.9);
		cam = clear(target, want);
		aim(target);
		introTicks = INTRO;
		Vec3 heroAt = local(mineAt, -1.3, 0, 1.4);
		hero = person("hero", com.insidethecube.retromon.game.Npc.PERSON, heroAt, face(heroAt, enemyAt));
		if (foeKey != null && !foeKey.isEmpty()) {
			Vec3 foeAt = local(enemyAt, 1.3, 0, -1.4);
			foe = person(foeKey, foeRole, foeAt, face(foeAt, mineAt));
		}
	}

	/**
	 * A battle in a room with its own arena (the lab's): the round hall goes up over you (as the outdoor stages do, and
	 * comes down after), lit by unseen lamps, and the battle's fought across its middle facing its back wall: your
	 * Pokémon, theirs, and their trainer behind theirs. You're not in the picture (Let's Go's indoor battles show just
	 * the Pokémon and the trainer you're battling); the throw still comes from where you'd stand.
	 */
	private static void setUpArena(com.insidethecube.retromon.game.Interiors.Arena a) {
		float h = stageH;
		Minecraft mc = Minecraft.getInstance();
		BEFORE.clear();
		BlockPos you = mc.player.blockPosition();
		int y = Math.min(level.getMaxY() - 12, you.getY() + 26);
		int ox = you.getX() - (int) Math.round(a.w() / 2), oz = you.getZ() - (int) Math.round(a.d() / 2);
		// Clear the air it goes into first (nothing's there, but just in case), then its blocks, then the lamps.
		net.minecraft.world.level.block.state.BlockState prop = Retromon.PROP.defaultBlockState();
		int[] c = a.cells();
		int states = com.insidethecube.retromon.game.Interiors.STATES;
		for (int i = 0; i + 3 < c.length; i += 4)
			set(ox + c[i], y + c[i + 1], oz + c[i + 2], prop.setValue(com.insidethecube.retromon.game.Prop.M, Math.min(states - 1, c[i + 3])));
		net.minecraft.world.level.block.state.BlockState lamp = Blocks.LIGHT.defaultBlockState().setValue(net.minecraft.world.level.block.LightBlock.LEVEL, 15);
		double cxr = a.w() / 2, czr = a.d() / 2, rad = Math.min(cxr, czr) - 2;
		for (int dx = 2; dx < a.w(); dx += 4)
			for (int dz = 2; dz < a.d(); dz += 4) {
				if (Math.hypot(dx + 0.5 - cxr, dz + 0.5 - czr) > rad) continue;
				BlockPos lp = new BlockPos(ox + dx, y + 4, oz + dz);
				if (level.getBlockState(lp).isAir()) set(lp.getX(), lp.getY(), lp.getZ(), lamp);
			}
		centre = new Vec3(ox + cxr, y, oz + czr);
		double far = 6.0 + Math.max(0, h - 1.5) * 1.3;
		mineAt = centre.add(-0.6, 0, 2.6);
		enemyAt = centre.add(1.6, 0, 2.6 - far);
		mine = spawn(mineAt, face(mineAt, enemyAt));
		theirs = spawn(enemyAt, face(enemyAt, mineAt));
		// Behind yours and a little above, looking across to theirs and their trainer, the hall's back wall behind them.
		cam = centre.add(-1.2, 2.3 + Math.max(0, h - 2) * 0.4, 9.2 + Math.max(0, h - 2) * 0.6);
		aim(mineAt.lerp(enemyAt, 0.5).add(0, 0.9 + Math.max(0, h - 1) * 0.15, 0));
		introTicks = INTRO;
		Vec3 heroAt = mineAt.add(-1.6, 0, 1.6);
		hero = person("hero", com.insidethecube.retromon.game.Npc.PERSON, heroAt, face(heroAt, enemyAt));
		hero.setInvisible(true);
		if (foeKey != null && !foeKey.isEmpty()) {
			Vec3 foeAt = enemyAt.add(2.0, 0, -1.7 - Math.max(0, h - 1.5) * 0.4);
			foe = person(foeKey, foeRole, foeAt, face(foeAt, mineAt));
		}
	}

	/**
	 * Inside, which way to look across the room: the way from you to them, or the nearest turn of it (in eighths)
	 * that has a wall behind them (a background, not the black outside), space for both of you along it, and
	 * room behind yours for the camera.
	 */
	private static Vec3 roomAxis(Vec3 mid, Vec3 want, double half) {
		Vec3 eye = mid.add(0, 1.9, 0), best = want;
		double bestScore = -1e9, a0 = Math.atan2(want.z, want.x);
		for (int i = 0; i < 8; i++) {
			double a = a0 + i * Math.PI / 4;
			Vec3 d = new Vec3(Math.cos(a), 0, Math.sin(a));
			double ahead = reach(eye, d, 24), behind = reach(eye, d.scale(-1), 24);
			double score = 0;
			if (ahead >= 23.5) score -= 40;
			if (ahead < half + 1.6) score -= 30;
			if (behind < half + 1.2) score -= 30;
			score += Math.min(behind - half, 6.5) * 2 + Math.min(ahead, 9) * 0.5;
			// Rather the way you were facing them, turned as little as can be.
			score -= Math.min(i, 8 - i) * 1.2;
			if (d.z > 0.3) score -= 60;
			if (score > bestScore) {
				bestScore = score;
				best = d;
			}
		}
		return best;
	}

	/** How far you can see from a point along a way before a block (up to max). */
	private static double reach(Vec3 from, Vec3 dir, double max) {
		Vec3 to = from.add(dir.scale(max));
		net.minecraft.world.phys.BlockHitResult hit = level.clip(new net.minecraft.world.level.ClipContext(from, to, net.minecraft.world.level.ClipContext.Block.VISUAL,
				net.minecraft.world.level.ClipContext.Fluid.NONE, Minecraft.getInstance().player));
		if (hit == null || hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS) return max;
		return from.distanceTo(hit.getLocation());
	}

	/** Where a camera can be on the way from what it looks at towards where it would like to be, short of any wall. */
	private static Vec3 clear(Vec3 from, Vec3 to) {
		Minecraft mc = Minecraft.getInstance();
		net.minecraft.world.phys.BlockHitResult hit = level.clip(new net.minecraft.world.level.ClipContext(from, to, net.minecraft.world.level.ClipContext.Block.VISUAL,
				net.minecraft.world.level.ClipContext.Fluid.NONE, mc.player));
		if (hit == null || hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS) return to;
		Vec3 d = to.subtract(from);
		double len = d.length();
		double keep = Math.max(1.5, from.distanceTo(hit.getLocation()) - 0.45);
		return len < 1e-3 ? to : from.add(d.scale(Math.min(1, keep / len)));
	}

	/** Puts up the stage, the two Pokémon (not out yet), you and their trainer, and the camera. */
	private static void setUp() {
		built = true;
		float h = stageH;
		axRight = new Vec3(1, 0, 0);
		axBack = new Vec3(0, 0, 1);
		if (kind.equals("inside")) {
			setUpInside();
			return;
		}
		build(stageAt);
		double far = 6.0 + Math.max(0, h - 1.5) * 1.3;
		mineAt = centre.add(-0.6, 0, 2.2);
		enemyAt = centre.add(1.8, 0, 2.2 - far);
		if (kind.equals("water")) enemyAt = new Vec3(enemyAt.x, centre.y - 0.1, enemyAt.z);
		mine = spawn(mineAt, face(mineAt, enemyAt));
		theirs = spawn(enemyAt, face(enemyAt, mineAt));
		// Behind your Pokémon and a little above, looking across to theirs (the newer games' view): yours low and
		// left of the middle, clear of your card, theirs further off to the right.
		cam = centre.add(-1.4, 2.2 + Math.max(0, h - 2) * 0.4, 9.8 + Math.max(0, h - 2) * 0.9);
		aim(mineAt.lerp(enemyAt, 0.5).add(0, 0.8 + Math.max(0, h - 1) * 0.15, 0));
		introTicks = INTRO;
		// You stand behind yours, off to the left (the newer games' view); their trainer behind theirs.
		Vec3 heroAt = mineAt.add(-2.0, 0, 1.5);
		hero = person("hero", com.insidethecube.retromon.game.Npc.PERSON, heroAt, face(heroAt, enemyAt));
		if (foeKey != null && !foeKey.isEmpty()) {
			Vec3 foeAt = enemyAt.add(2.1, 0, -1.8 - Math.max(0, h - 1.5) * 0.4);
			foe = person(foeKey, foeRole, foeAt, face(foeAt, mineAt));
		}
	}

	private static com.insidethecube.retromon.game.Npc person(String key, int role, Vec3 at, float yaw) {
		com.insidethecube.retromon.game.Npc n = new com.insidethecube.retromon.game.Npc(Retromon.NPC, level);
		n.setId(nextId--);
		n.setRole(role);
		n.setKey(key);
		n.snapTo(at.x, at.y, at.z, yaw, 0);
		n.setFacing(yaw);
		level.addEntity(n);
		return n;
	}

	/** Where the one who sent a side's Pokémon out stands (you, or their trainer), or null for a wild one. */
	private static Vec3 trainerOf(int side) {
		com.insidethecube.retromon.game.Npc n = side == 0 ? hero : foe;
		return n == null ? null : n.position().add(0, 1.1, 0);
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
			built = true;
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

	/** The camera's opening sweep: ticks left (from their side, round to behind you). */
	private static int introTicks;
	private static final int INTRO = 64;

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
		// From your hand (you throw it), or from in front of the camera if you're not on the stage.
		ball = ballAt(hand(hero, cam.add(-0.4, -0.7, -0.8)), item);
		if (hero != null) hero.throwing = com.insidethecube.retromon.game.Npc.THROW;
		BedrockModel.Def def = BedrockModel.def(theirs == null ? "pidgey" : theirs.species);
		double top = def == null ? 0.8 : Math.min(2.5, def.height * 0.6);
		ball.fly(enemyAt.x, enemyAt.y + top, enemyAt.z, 1.4, 14);
		Sounds.ball();
	}

	/** A trainer's throwing hand (on the stage), or the given spot without one. */
	private static Vec3 hand(com.insidethecube.retromon.game.Npc n, Vec3 otherwise) {
		if (n == null) return otherwise;
		double yaw = Math.toRadians(n.getYRot());
		return n.position().add(-Math.cos(yaw) * 0.4 - Math.sin(yaw) * 0.3, 1.45, -Math.sin(yaw) * 0.4 + Math.cos(yaw) * 0.3);
	}

	/** A Poké Ball on its own in the world, out of a battle (the nurse's machine): moved by whoever asked; removeLoose when done. */
	public static MonDisplay looseBall(Vec3 p) {
		ClientLevel lv = net.minecraft.client.Minecraft.getInstance().level;
		if (lv == null) return null;
		MonDisplay b = new MonDisplay(Retromon.MON_DISPLAY, lv);
		b.setId(nextId--);
		b.snapTo(p.x, p.y, p.z, 0, 0);
		b.species = "poke_ball";
		b.texture = Retromon.ID + ":textures/pokemon/ball_poke_ball.png";
		b.size = b.sizeOld = b.sizeTarget = 1;
		lv.addEntity(b);
		return b;
	}

	public static void removeLoose(MonDisplay b) {
		if (b != null && b.level() instanceof ClientLevel lv) lv.removeEntity(b.getId(), Entity.RemovalReason.DISCARDED);
	}

	private static MonDisplay ballAt(Vec3 p, String item) {
		MonDisplay b = spawn(p, 0);
		b.species = "poke_ball";
		b.texture = Retromon.ID + ":textures/pokemon/ball_" + (item == null || item.isEmpty() ? "poke_ball" : item) + ".png";
		b.size = b.sizeOld = b.sizeTarget = 1;
		return b;
	}

	/** The ball bounces up off their Pokémon and hangs there, opening. */
	public static void ballUp() {
		if (ball == null) return;
		Vec3 p = ball.position();
		ball.fly(p.x, p.y + 0.9, p.z + 0.5, 0.35, 7);
		Sounds.sfx("tink", 1.4F, 0.6F);
	}

	/** It's drawn into the open ball as red light: tinted red, shrinking as it streams up into it. */
	public static void absorb() {
		if (ball == null || theirs == null) return;
		Vec3 b = ball.position();
		Vec3 mid = enemyAt.add(0, 0.25 + height(1) * 0.45, 0);
		theirs.red = 40;
		theirs.fly(b.x, b.y - 0.35, b.z, 0, 12);
		theirs.sizeTarget = 0;
		FX.add(new Fx(BEAM, mid, b, dust(0xFF3A3A, 2.0F), dust(0xFFE0E0, 1.3F), 12, 0, new int[1], 7));
		sphere(b, ParticleTypes.END_ROD, 12, 0.14);
		Sounds.sfx("shrink", 1.5F, 0.8F);
	}

	/** The ball snaps shut on it. */
	public static void closeBall() {
		if (ball == null) return;
		sphere(ball.position(), ParticleTypes.CRIT, 10, 0.2);
		Sounds.sfx("tink", 1.1F, 0.8F);
	}

	/** It broke free: back at its spot, red light fading. */
	public static void release() {
		if (theirs == null) return;
		theirs.setPos(enemyAt.x, enemyAt.y, enemyAt.z);
		theirs.fly(enemyAt.x, enemyAt.y, enemyAt.z, 0, 1);
		theirs.red = 6;
		theirs.sizeTarget = 1;
	}

	/** The ball drops to the ground in front of where their Pokémon was. */
	public static void dropBall() {
		if (ball != null) ball.fly(enemyAt.x, enemyAt.y, enemyAt.z + 0.5, 0.25, 8);
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
		com.insidethecube.retromon.game.Npc who = side == 0 ? hero : foe;
		Vec3 from = hand(who, side == 0 ? cam.add(0.6, -0.8, -0.6) : to.add(0.8, 1.6, -3.5));
		if (who != null) who.throwing = com.insidethecube.retromon.game.Npc.THROW;
		sendBall = ballAt(from, "poke_ball");
		sendBall.fly(to.x, to.y + 0.2, to.z, 1.1, 9);
		sendSide = side;
		Sounds.ball();
	}

	// ---------- the moves' effects ----------

	/** Kinds of effect: a stream from one to the other, bursts on the target, a gathering on the attacker, a ring on the ground. */
	private static final int BEAM = 0, BURST = 1, CHARGE = 2, RING = 3, SPIRAL = 4;

	private record Fx(int kind, Vec3 from, Vec3 to, ParticleOptions particle, ParticleOptions trail, int life, int delay, int[] age, int density) {}

	private static final List<Fx> FX = new ArrayList<>();
	/** A flash of the move's colour round the screen's edges (the battle screen draws it): colour, ticks left, length. */
	public static int flashColor, flashTicks, flashMax = 1;
	/** The camera's move for an attack: a push in towards the attacker, and back. */
	private static int shotSide = -1, shotTicks, shotMax = 1;
	/** A lightning bolt to come down on a target (Thunderbolt, Thunder): ticks to wait, and where. */
	private static int boltIn = -1;
	private static Vec3 boltAt = Vec3.ZERO;

	/** A hit's burst drawn on the screen over the one hit (a star of light in the move's colour). */
	public static final class Impact {
		public final int side, color;
		public final boolean big;
		public int age;
		public final int life;

		Impact(int side, int color, boolean big, int life) {
			this.side = side;
			this.color = color;
			this.big = big;
			this.life = life;
		}
	}

	/** Arrows rising (a stat up) or falling (down) over a Pokémon, drawn on the screen. */
	public static final class Arrows {
		public final int side;
		public final boolean up;
		public int age;
		public static final int LIFE = 26;

		Arrows(int side, boolean up) {
			this.side = side;
			this.up = up;
		}
	}

	public static final List<Impact> IMPACTS = new ArrayList<>();
	public static final List<Arrows> ARROWS = new ArrayList<>();

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
			case DARK -> 0x3A2A50;
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

	/** How tall a side's Pokémon stands. */
	public static float height(int side) {
		MonDisplay d = display(side);
		BedrockModel.Def def = d == null ? null : BedrockModel.def(d.species);
		return def == null ? 1 : Math.min(3.5F, def.height);
	}

	public static Vec3 enemySpot() {
		return enemyAt;
	}

	private static ParticleOptions dust(int rgb, float size) {
		return new DustParticleOptions(rgb & 0xFFFFFF, size);
	}

	/** Moves that hit from afar (the attacker rears back rather than dashing in). */
	private static final java.util.Set<String> RANGED = java.util.Set.of("swift", "sonicboom", "dragon_rage", "razor_wind", "gust", "twister", "earthquake",
			"magnitude", "fissure", "rock_slide", "rock_throw", "pin_missile", "spike_cannon", "egg_bomb", "barrage", "bonemerang", "poison_sting", "twineedle",
			"acid", "smog", "sludge", "sludge_bomb", "razor_leaf", "explosion", "selfdestruct", "night_shade", "psywave", "seismic_toss", "sand_attack",
			"string_shot", "spore", "sleep_powder", "stun_spore", "poisonpowder", "present");
	/** Moves of the special types that still hit up close (punches, bites, a waterfall). */
	private static final java.util.Set<String> CLOSE = java.util.Set.of("fire_punch", "ice_punch", "thunderpunch", "bite", "crunch", "flame_wheel", "waterfall",
			"crabhammer", "clamp", "vine_whip", "faint_attack", "thief", "pursuit", "outrage");

	/** Whether a move hits up close (the attacker dashes in) or from afar. */
	public static boolean contact(Move m) {
		if (m == null || m.power() <= 0) return false;
		if (CLOSE.contains(m.id())) return true;
		if (m.id().contains("beam") || RANGED.contains(m.id())) return false;
		return !m.type().special();
	}

	/**
	 * A move's effect: the attacker hops in (or rears back for a move from afar), the camera pushes in a little,
	 * and the move's particles by type: special moves gather on the attacker and stream across in their colour,
	 * bursting on the target; hits up close slash or thump; stat moves spiral up. The big ones have their own:
	 * a real bolt of lightning for Thunderbolt and Thunder, beams, the ground shaking for Earthquake, a fire
	 * storm, a wave, an explosion.
	 */
	public static void move(int side, String moveId) {
		if (!active() || moveId == null) return;
		Move m = Move.find(moveId);
		if (m == null) return;
		Vec3 me = spot(side).add(0, 0.25 + height(side) * 0.4, 0), them = spot(1 - side).add(0, 0.25 + height(1 - side) * 0.4, 0);
		ParticleOptions p = particle(m.type());
		int rgb = typeColor(m.type());
		ParticleOptions tint = dust(rgb, 1.8F);
		int density = 3 + Math.max(0, m.power()) / 25;
		pendingFlash = rgb;
		if (m.power() <= 0) {
			// Status moves: on itself (raising its stats) or on the other one.
			boolean self = m.effect().contains("UP") || m.effect().contains("HEAL") || m.effect().contains("REFLECT") || m.effect().contains("LIGHT_SCREEN")
					|| m.effect().contains("SUBSTITUTE") || m.effect().contains("FOCUS") || m.effect().contains("REST") || m.effect().contains("PROTECT");
			Vec3 at = self ? spot(side) : spot(1 - side);
			FX.add(new Fx(SPIRAL, at, at, self ? ParticleTypes.END_ROD : p, tint, 18, 0, new int[1], 3));
			shot(side, 14);
			Sounds.sfx("shrink", self ? 1.2F : 0.9F, 0.6F);
			return;
		}
		boolean close = contact(m);
		String id = m.id();
		shot(side, close ? 16 : 24);
		if (!close) FX.add(new Fx(CHARGE, me, me, ParticleTypes.END_ROD, tint, 7, 0, new int[1], 4));
		int after = close ? 5 : 7;
		if (id.equals("thunderbolt") || id.equals("thunder") || id.equals("zap_cannon") || id.equals("thundershock") || id.equals("thunderpunch")) {
			boltIn = after + 3;
			boltAt = spot(1 - side);
			FX.add(new Fx(BURST, them, them, ParticleTypes.ELECTRIC_SPARK, tint, 18, after, new int[1], 10));
		} else if (id.contains("beam") || id.equals("solarbeam") || id.equals("dragonbreath") || id.equals("psybeam")) {
			FX.add(new Fx(BEAM, me, them, ParticleTypes.END_ROD, tint, 12, after, new int[1], 8));
			FX.add(new Fx(BEAM, me, them, p, dust(0xFFFFFF, 1.2F), 12, after + 2, new int[1], density + 3));
		} else if (id.equals("earthquake") || id.equals("magnitude") || id.equals("fissure")) {
			shake = 28;
			FX.add(new Fx(RING, spot(1 - side), spot(1 - side), new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()), tint, 20, 2, new int[1],
					16));
			FX.add(new Fx(RING, spot(side), spot(side), ParticleTypes.DUST_PLUME, tint, 16, 0, new int[1], 10));
		} else if (id.equals("explosion") || id.equals("selfdestruct")) {
			shake = 26;
			burst(me, ParticleTypes.EXPLOSION_EMITTER, 1, 0.1);
			flash(0xFFFFFF, 16);
		} else if (id.equals("flamethrower") || id.equals("fire_blast") || id.equals("fire_spin") || id.equals("sacred_fire") || id.equals("ember")) {
			FX.add(new Fx(BEAM, me, them, ParticleTypes.FLAME, dust(0xFF6A1A, 2.2F), 12, after, new int[1], id.equals("ember") ? 6 : 14));
			FX.add(new Fx(BURST, them, them, ParticleTypes.LAVA, dust(0xFFB040, 2F), 16, after + 10, new int[1], 6));
		} else if (id.equals("surf") || id.equals("hydro_pump") || id.equals("whirlpool") || id.equals("bubblebeam") || id.equals("water_gun") || id.equals("bubble")) {
			FX.add(new Fx(BEAM, me, them, id.startsWith("bubble") ? ParticleTypes.BUBBLE_POP : ParticleTypes.SPLASH, dust(0x5AA8FF, 2F), 12, after, new int[1], 14));
			FX.add(new Fx(BURST, them, them, ParticleTypes.FALLING_WATER, dust(0x9AD0FF, 1.6F), 16, after + 10, new int[1], 10));
		} else if (id.equals("blizzard") || id.equals("gust") || id.equals("twister") || id.equals("razor_wind") || id.equals("whirlwind") || id.equals("ice_beam")
				|| id.equals("powder_snow")) {
			FX.add(new Fx(SPIRAL, spot(1 - side), spot(1 - side), m.type() == Type.ICE ? ParticleTypes.SNOWFLAKE : ParticleTypes.GUST, tint, 20, after, new int[1], 6));
		} else if (close) {
			// Up close: a slash for claws and blades, a thump for the rest, as it lands its hop.
			boolean slash = id.contains("scratch") || id.contains("slash") || id.equals("cut") || id.contains("fury") || id.contains("claw") || id.equals("vicegrip")
					|| id.equals("guillotine") || id.equals("karate_chop") || id.equals("wing_attack") || id.equals("peck") || id.equals("drill_peck");
			FX.add(new Fx(BURST, them, them, slash ? ParticleTypes.SWEEP_ATTACK : ParticleTypes.POOF, tint, 4, after, new int[1], slash ? 2 : 6));
			if (m.type() != Type.NORMAL) FX.add(new Fx(BURST, them, them, p, tint, 8, after, new int[1], density));
		} else {
			FX.add(new Fx(BEAM, me, them, p, tint, 12, after, new int[1], density + 3));
		}
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

	/** A hit: the screen shakes (harder when it's super effective), the move's colour glows round the edges, a star of light bursts on it. */
	public static void hit(int side, int effect) {
		shake = Math.max(shake, effect > 0 ? 14 : 8);
		flash(pendingFlash, effect > 0 ? 16 : 10);
		IMPACTS.add(new Impact(side, pendingFlash, effect > 0, effect > 0 ? 12 : 9));
		Vec3 at = spot(side).add(0, 0.25 + height(side) * 0.4, 0);
		sphere(at, ParticleTypes.CRIT, effect > 0 ? 22 : 12, 0.45);
		sphere(at, dust(pendingFlash, 1.6F), effect > 0 ? 18 : 10, 0.3);
		ring(spot(side).add(0, 0.1, 0), dust(pendingFlash, 1.4F), 16, 0.28);
	}

	/** Your Pokémon celebrating a win: the camera comes round in front of it as it hops for joy. */
	private static int cheerTicks, cheerMax = 1;

	/** The win held on: the camera stays in front of yours as it (and you) celebrate, till you carry on. */
	public static boolean holding;

	public static void hold() {
		if (!active) return;
		if (!holding) cheer();
		holding = true;
	}

	/** A win's been celebrated: the camera stays in front of yours from then on (not back to theirs). */
	private static boolean celebrated;

	public static void cheer() {
		if (!active) return;
		cheerTicks = cheerMax = 52;
		celebrated = true;
		MonDisplay d = display(0);
		if (d != null) {
			d.cheer = MonDisplay.CHEER;
			d.act("cry");
		}
		if (hero != null) {
			// A different celebration each time: arms up and hopping, the floss, a fist pump, a wave.
			hero.cheer = com.insidethecube.retromon.game.Npc.CHEER;
			int was = hero.dance;
			hero.dance = (int) (Math.random() * 4);
			if (hero.dance == was) hero.dance = (hero.dance + 1) % 4;
		}
		Vec3 at = spot(0).add(0, 0.3 + height(0) * 0.5, 0);
		sphere(at, ParticleTypes.HAPPY_VILLAGER, 14, 0.2);
		FX.add(new Fx(SPIRAL, spot(0), spot(0), ParticleTypes.NOTE, dust(0xFFE070, 1.4F), 30, 4, new int[1], 3));
	}

	/** A stat raised or lowered: arrows over it (the screen draws them), and sparkles. */
	public static void stat(int side, boolean up) {
		if (!active) return;
		ARROWS.add(new Arrows(side, up));
		Vec3 at = spot(side);
		FX.add(new Fx(SPIRAL, at, at, up ? ParticleTypes.END_ROD : ParticleTypes.SQUID_INK, dust(up ? 0xFF7A3A : 0x4A8FE0, 1.4F), 16, 0, new int[1], 3));
	}

	/**
	 * Fainting: it sags and keels over (its own faint animation, where its model has one) and lies there a moment;
	 * then its trainer throws a ball beside it, the ball opens, red light draws it in, and the ball comes back to
	 * the trainer's hand (a wild one fades away where it lies).
	 */
	public static void faint(int side) {
		MonDisplay d = display(side);
		if (d == null) return;
		d.faint = MonDisplay.FAINT;
		d.down = true;
		d.act("faint");
		Sounds.faint();
		// A trainer's is called back; a wild one just lies there till the battle's over.
		later(30, () -> {
			if (trainerOf(side) != null) recallWithBall(side);
		});
	}

	/**
	 * Called back (switching): as the Creature Viewer's recall: its trainer throws a ball beside it, the ball pops
	 * open, red light streams from it into the ball as it shrinks, tinted red, and the ball arcs back to the hand.
	 */
	public static void recall(int side) {
		if (display(side) == null) return;
		recallWithBall(side);
	}

	/** A ball thrown to call a Pokémon back (one at a time). */
	private static MonDisplay recallBall;

	private static void recallWithBall(int side) {
		MonDisplay d = display(side);
		if (d == null || level == null) return;
		Vec3 at = spot(side), back = trainerOf(side);
		com.insidethecube.retromon.game.Npc who = side == 0 ? hero : foe;
		Vec3 from = hand(who, back == null ? at.add(0, 1.4, 2) : back);
		Vec3 toward = from.subtract(at).multiply(1, 0, 1);
		Vec3 land = at.add(toward.lengthSqr() < 1e-4 ? Vec3.ZERO : toward.normalize().scale(1.1)).add(0, 0.13, 0);
		if (recallBall != null) level.removeEntity(recallBall.getId(), Entity.RemovalReason.DISCARDED);
		MonDisplay b = recallBall = ballAt(from, "poke_ball");
		b.fly(land.x, land.y, land.z, 0.9, 8);
		if (who != null) who.throwing = com.insidethecube.retromon.game.Npc.THROW;
		Sounds.ball();
		later(10, () -> {
			// It opens: the red light, and in it goes.
			Vec3 p = at.add(0, 0.2 + height(side) * 0.35, 0);
			d.red = 16;
			d.sizeTarget = 0;
			FX.add(new Fx(BEAM, p, b.position().add(0, 0.2, 0), dust(0xFF3A30, 1.1F), dust(0xFFFFFF, 0.8F), 9, 0, new int[1], 3));
			flash(0xFF3A30, 5);
			Sounds.sfx("tink", 0.8F, 0.6F);
		});
		later(24, () -> b.fly(from.x, from.y, from.z, 0.7, 8));
		later(33, () -> {
			if (level != null && recallBall == b) {
				level.removeEntity(b.getId(), Entity.RemovalReason.DISCARDED);
				recallBall = null;
			}
		});
	}

	/** Things to do a little later (after so many ticks). */
	private record Later(int[] ticks, Runnable then) {}

	private static final List<Later> LATER = new ArrayList<>();

	private static void later(int ticks, Runnable then) {
		LATER.add(new Later(new int[] {ticks}, then));
	}

	/** One more tick for the Pokémon and balls on the stage (the game sped up: the battle keeps pace). */
	public static void tickDisplays() {
		for (MonDisplay d : new MonDisplay[] {mine, theirs, ball, sendBall, recallBall}) if (d != null) d.tick();
	}

	private static void burst(Vec3 at, ParticleOptions p, int n, double spread) {
		if (level == null) return;
		for (int i = 0; i < n; i++) {
			level.addParticle(p, at.x + (RNG.nextDouble() - 0.5) * spread * 2, at.y + (RNG.nextDouble() - 0.5) * spread * 2,
					at.z + (RNG.nextDouble() - 0.5) * spread * 2, (RNG.nextDouble() - 0.5) * 0.1, RNG.nextDouble() * 0.08, (RNG.nextDouble() - 0.5) * 0.1);
		}
	}

	/** Particles flying out in all directions from a point. */
	private static void sphere(Vec3 at, ParticleOptions p, int n, double speed) {
		if (level == null) return;
		for (int i = 0; i < n; i++) {
			double u = RNG.nextDouble() * 2 - 1, a = RNG.nextDouble() * Math.PI * 2, r = Math.sqrt(1 - u * u);
			double vx = r * Math.cos(a), vy = u, vz = r * Math.sin(a);
			level.addParticle(p, at.x + vx * 0.15, at.y + vy * 0.15, at.z + vz * 0.15, vx * speed, vy * speed, vz * speed);
		}
	}

	/** Particles flying out flat along the ground in a ring. */
	private static void ring(Vec3 at, ParticleOptions p, int n, double speed) {
		if (level == null) return;
		for (int i = 0; i < n; i++) {
			double a = Math.PI * 2 * i / n;
			level.addParticle(p, at.x + Math.cos(a) * 0.3, at.y, at.z + Math.sin(a) * 0.3, Math.cos(a) * speed, 0.01, Math.sin(a) * speed);
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
		int r = 19;
		for (int dx = -r - 3; dx <= r + 3; dx++) {
			for (int dz = -r - 3; dz <= r + 3; dz++) {
				double d = Math.sqrt(dx * dx + dz * dz);
				if (d > r + 0.5) continue;
				floor(cx + dx, y, cz + dz, dx, dz, d);
			}
		}
		switch (kind) {
			case "forest" -> {
				hedges(cx, y, cz, 8.6, 10.0, true);
				ring(cx, y, cz, 10.5, 17, 9, true);
				ring(cx, y, cz, 13.5, 22, 11, true);
				ring(cx, y, cz, 16.8, 28, 13, true);
				plants(cx, y, cz, 6, 9.5, 0.4, Blocks.FERN.defaultBlockState(), Blocks.SHORT_GRASS.defaultBlockState(), Blocks.BROWN_MUSHROOM.defaultBlockState());
			}
			case "cave" -> rocks(cx, y, cz);
			case "gym" -> gym(cx, y, cz);
			case "water" -> {
				ring(cx, y, cz, 12.5, 9, 7, false);
				ring(cx, y, cz, 16.5, 22, 11, false);
				plants(cx, y, cz, 4, 9, 0.2, Blocks.SHORT_GRASS.defaultBlockState(), Blocks.SUGAR_CANE.defaultBlockState(), Blocks.LILY_PAD.defaultBlockState());
			}
			case "town" -> {
				ring(cx, y, cz, 12.5, 8, 7, false);
				ring(cx, y, cz, 16.5, 24, 11, false);
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
				// Let's Go's fields: open grass in the middle, long grass and hedges at its edge, then a deep wall of
				// round trees all the way round (no sky or edge showing between them).
				hedges(cx, y, cz, 8.8, 10.2, false);
				ring(cx, y, cz, 10.8, 16, 7, false);
				ring(cx, y, cz, 13.6, 22, 10, false);
				ring(cx, y, cz, 16.8, 28, 12, false);
				plants(cx, y, cz, 6.2, 9.6, 0.5, Blocks.TALL_GRASS.defaultBlockState(), Blocks.TALL_GRASS.defaultBlockState(), Blocks.SHORT_GRASS.defaultBlockState(),
						Blocks.DANDELION.defaultBlockState(), Blocks.POPPY.defaultBlockState(), Blocks.AZURE_BLUET.defaultBlockState());
			}
		}
		lights(cx, y, cz);
	}

	/** Unseen lights over the stage, so a battle at night is moonlit and clear rather than dark. */
	private static void lights(int cx, int y, int cz) {
		BlockState light = Blocks.LIGHT.defaultBlockState().setValue(net.minecraft.world.level.block.LightBlock.LEVEL, 13);
		for (int i = 0; i < 6; i++) {
			double a = Math.toRadians(i * 60);
			int lx = cx + (int) Math.round(Math.sin(a) * 5), lz = cz + (int) Math.round(Math.cos(a) * 5) - 1;
			set(lx, y + 4, lz, light);
		}
		set(cx, y + 5, cz, light);
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

	/** Where the battle's camera stands on the stage (x, z), so the trees and hedges keep clear of it. */
	private static double[] camSpot(int cx, int cz) {
		return new double[] {cx + 0.5 - 1.4, cz + 0.5 + 9.8 + Math.max(0, stageH - 2) * 0.9};
	}

	/**
	 * A ring of trees at a radius (n of them, height h), all the way round (so there's something behind yours when
	 * the camera comes round to it), but none where the camera itself stands.
	 */
	private static void ring(int cx, int y, int cz, double radius, int n, int h, boolean mixed) {
		double[] cam = camSpot(cx, cz);
		for (int i = 0; i < n; i++) {
			double a = Math.toRadians(i * 360.0 / n + RNG.nextInt(10));
			int tx = cx + (int) Math.round(Math.sin(a) * radius), tz = cz + (int) Math.round(Math.cos(a) * radius);
			if (Math.hypot(tx - cam[0], tz - cam[1]) < 6.5) continue;
			int kindOf = mixed ? RNG.nextInt(3) : 0;
			BlockState log = kindOf == 1 ? Blocks.SPRUCE_LOG.defaultBlockState() : kindOf == 2 ? Blocks.BIRCH_LOG.defaultBlockState() : Blocks.OAK_LOG.defaultBlockState();
			BlockState leaves = (kindOf == 1 ? Blocks.SPRUCE_LEAVES : kindOf == 2 ? Blocks.BIRCH_LEAVES : Blocks.OAK_LEAVES).defaultBlockState()
					.setValue(LeavesBlock.PERSISTENT, true);
			tree(tx, y + 1, tz, h - 2 + RNG.nextInt(4), log, leaves);
		}
	}

	/** Low hedges (leaves a block or two high) round the field's edge, between two radii, clear of the camera. */
	private static void hedges(int cx, int y, int cz, double from, double to, boolean mixed) {
		double[] cam = camSpot(cx, cz);
		for (int dx = (int) -to; dx <= to; dx++) {
			for (int dz = (int) -to; dz <= to; dz++) {
				double d = Math.sqrt(dx * dx + dz * dz);
				if (d < from || d > to || Math.hypot(cx + dx - cam[0], cz + dz - cam[1]) < 4 || RNG.nextInt(10) < 3) continue;
				BlockState leaves = (mixed && RNG.nextInt(3) == 0 ? Blocks.SPRUCE_LEAVES : Blocks.OAK_LEAVES).defaultBlockState()
						.setValue(LeavesBlock.PERSISTENT, true);
				set(cx + dx, y + 1, cz + dz, leaves);
				if (RNG.nextInt(3) > 0) set(cx + dx, y + 2, cz + dz, leaves);
			}
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
				// Nothing between the camera and the Pokémon.
				if (dz > 0 && Math.abs(dx) < 9) continue;
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
				if (dz > 4 && Math.abs(dx) < 9) continue;
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
			for (MonDisplay d : new MonDisplay[] {mine, theirs, ball, sendBall, recallBall}) if (d != null) level.removeEntity(d.getId(), Entity.RemovalReason.DISCARDED);
			for (Entity n : new Entity[] {hero, foe}) if (n != null) level.removeEntity(n.getId(), Entity.RemovalReason.DISCARDED);
			List<Map.Entry<BlockPos, BlockState>> back = new ArrayList<>(BEFORE.entrySet());
			for (int i = back.size() - 1; i >= 0; i--) level.setBlock(back.get(i).getKey(), back.get(i).getValue(), 2 | 16, 0);
		}
		BEFORE.clear();
		FX.clear();
		IMPACTS.clear();
		ARROWS.clear();
		cheerTicks = 0;
		holding = false;
		celebrated = false;
		introTicks = 0;
		LATER.clear();
		hero = foe = null;
		built = false;
		shotTicks = flashTicks = 0;
		boltIn = -1;
		mine = theirs = ball = sendBall = recallBall = null;
		hiddenNpc = -1;
		active = revealed = false;
		level = null;
		shake = 0;
	}

	public static void tick(Minecraft mc) {
		// If the world changed under a battle (left the game), forget it.
		if (active && mc.level != level) {
			level = null;
			mine = theirs = ball = sendBall = recallBall = null;
			BEFORE.clear();
			FX.clear();
			active = revealed = false;
			return;
		}
		if (!active || level == null) return;
		for (int i = LATER.size() - 1; i >= 0; i--) {
			Later l = LATER.get(i);
			if (--l.ticks()[0] <= 0) {
				LATER.remove(i);
				l.then().run();
			}
		}
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
			// The ball pops open in a flash of white light, and out it comes glowing white (the Creature Viewer's).
			burst(sendBall.position().add(0, 0.4, 0), ParticleTypes.POOF, 12, 0.3);
			burst(sendBall.position().add(0, 0.6, 0), dust(0xFFFFFF, 1.3F), 18, 0.45);
			flash(0xFFFFFF, 6);
			MonDisplay popped = display(sendSide);
			if (popped != null) popped.white = MonDisplay.WHITE;
			Sounds.poof();
			level.removeEntity(sendBall.getId(), Entity.RemovalReason.DISCARDED);
			sendBall = null;
		}
		for (int i = FX.size() - 1; i >= 0; i--) {
			Fx f = FX.get(i);
			int age = ++f.age()[0] - f.delay();
			if (age <= 0) continue;
			switch (f.kind()) {
				case BEAM -> {
					// The stream travels from the attacker to the target in a trail of its colour, then bursts there.
					double t = Math.min(1, age / (double) f.life());
					Vec3 d = f.to().subtract(f.from());
					for (int k = 0; k < f.density(); k++) {
						double tt = Math.max(0, t - RNG.nextDouble() * 0.12);
						Vec3 at = f.from().lerp(f.to(), tt);
						double j = 0.12 + 0.1 * Math.sin(age * 0.9 + k);
						level.addParticle(f.particle(), at.x + (RNG.nextDouble() - 0.5) * j, at.y + (RNG.nextDouble() - 0.5) * j, at.z + (RNG.nextDouble() - 0.5) * j,
								d.x * 0.02, d.y * 0.02 + 0.005, d.z * 0.02);
					}
					Vec3 head = f.from().lerp(f.to(), t);
					for (int k = 0; k < 3; k++) level.addParticle(f.trail(), head.x + (RNG.nextDouble() - 0.5) * 0.2, head.y + (RNG.nextDouble() - 0.5) * 0.2,
							head.z + (RNG.nextDouble() - 0.5) * 0.2, 0, 0, 0);
					if (age == f.life()) {
						sphere(f.to(), f.particle(), 18, 0.25);
						sphere(f.to(), f.trail(), 14, 0.2);
					}
				}
				case CHARGE -> {
					// Light gathering on the attacker before the move goes.
					for (int k = 0; k < f.density(); k++) {
						double a = RNG.nextDouble() * Math.PI * 2, u = RNG.nextDouble() * 2 - 1, r = Math.sqrt(1 - u * u);
						Vec3 off = new Vec3(r * Math.cos(a), u, r * Math.sin(a)).scale(0.9);
						Vec3 at = f.to().add(off);
						level.addParticle(k % 2 == 0 ? f.particle() : f.trail(), at.x, at.y, at.z, -off.x * 0.12, -off.y * 0.12, -off.z * 0.12);
					}
				}
				case RING -> {
					if (age % 3 == 1) ring(f.to(), f.particle(), f.density(), 0.18 + age * 0.01);
					if (age % 4 == 1) ring(f.to().add(0, 0.2, 0), f.trail(), 12, 0.22);
				}
				case SPIRAL -> {
					// Rising round it in a spiral (a stat move, a whirlwind).
					for (int k = 0; k < f.density(); k++) {
						double a = age * 0.55 + k * Math.PI * 2 / f.density();
						double y = (age / (double) f.life()) * 1.8;
						Vec3 at = f.to().add(Math.cos(a) * 0.7, y, Math.sin(a) * 0.7);
						level.addParticle(k % 2 == 0 ? f.particle() : f.trail(), at.x, at.y, at.z, 0, 0.02, 0);
					}
				}
				default -> {
					if (age % 2 == 1) {
						burst(f.to(), f.particle(), f.density(), 0.45);
						burst(f.to(), f.trail(), Math.max(2, f.density() / 2), 0.4);
					}
				}
			}
			if (age >= f.life()) FX.remove(i);
		}
		if (cheerTicks > 0) cheerTicks--;
		// Once yours has celebrated, the camera stays round in front of it (half way through its move round).
		if (celebrated) cheerTicks = Math.max(cheerTicks, cheerMax / 2);
		if (holding) {
			// Held in front of yours (half way through the cheer's move round), the two of you still celebrating.
			cheerTicks = Math.max(cheerTicks, cheerMax / 2);
			if (hero != null && hero.cheer <= 0) hero.cheer = com.insidethecube.retromon.game.Npc.CHEER;
			MonDisplay mineNow = display(0);
			if (mineNow != null && mineNow.cheer <= 0) mineNow.cheer = MonDisplay.CHEER;
		}
		if (introTicks > 0) introTicks--;
		for (int i = IMPACTS.size() - 1; i >= 0; i--) if (++IMPACTS.get(i).age > IMPACTS.get(i).life) IMPACTS.remove(i);
		for (int i = ARROWS.size() - 1; i >= 0; i--) if (++ARROWS.get(i).age > Arrows.LIFE) ARROWS.remove(i);
	}

	public static double[] cameraPose(float partial) {
		double sx = 0, sy = 0;
		if (shake > 0) {
			double k = shake * 0.012;
			sx = (RNG.nextDouble() - 0.5) * k;
			sy = (RNG.nextDouble() - 0.5) * k;
		}
		double x = cam.x, y = cam.y, z = cam.z, yaw = camYaw, pitch = camPitch;
		if (introTicks > 0) {
			// The opening: low on their side, looking at them (and their trainer), then round and back to you.
			float t = 1 - (introTicks - partial) / INTRO;
			double w = Math.min(1, Math.max(0, (t - 0.15) / 0.85));
			w = w * w * (3 - 2 * w);
			Vec3 them = foe != null ? enemyAt.lerp(foe.position(), 0.35) : enemyAt;
			Vec3 from = local(enemyAt, -3.6, 1.4 + height(1) * 0.3, 4.6);
			if (kind.equals("inside")) from = clear(enemyAt.add(0, 0.8, 0), from);
			Vec3 lookFrom = them.add(0, 0.6 + height(1) * 0.35, 0);
			x = from.x + (x - from.x) * w;
			y = from.y + (y - from.y) * w;
			z = from.z + (z - from.z) * w;
			Vec3 at = lookFrom.lerp(camTarget, w).subtract(x, y, z);
			yaw = Math.toDegrees(Math.atan2(-at.x, at.z));
			pitch = Math.toDegrees(Math.atan2(-at.y, Math.sqrt(at.x * at.x + at.z * at.z)));
		} else if (cheerTicks > 0) {
			// In front of yours, a little to the side, looking back at it.
			float t = 1 - (cheerTicks - partial) / cheerMax;
			double w = t < 0.2 ? t / 0.2 : t > 0.82 ? (1 - t) / 0.18 : 1;
			w = w * w * (3 - 2 * w);
			Vec3 me = spot(0), toFoe = spot(1).subtract(me);
			toFoe = new Vec3(toFoe.x, 0, toFoe.z).normalize();
			double h = height(0);
			Vec3 front = me.add(toFoe.scale(2.6 + h * 0.9)).add(-toFoe.z * 1.1, 0.9 + h * 0.4, toFoe.x * 1.1);
			if (kind.equals("inside")) front = clear(me.add(0, 0.6, 0), front);
			Vec3 look = me.add(0, 0.25 + h * 0.5, 0);
			x += (front.x - x) * w;
			y += (front.y - y) * w;
			z += (front.z - z) * w;
			Vec3 at = camTarget.lerp(look, w).subtract(x, y, z);
			yaw = Math.toDegrees(Math.atan2(-at.x, at.z));
			pitch = Math.toDegrees(Math.atan2(-at.y, Math.sqrt(at.x * at.x + at.z * at.z)));
		} else if (shotTicks > 0 && shotSide >= 0) {
			// A gentle push in towards the one attacking, the view leaning its way; then back.
			float t = 1 - (shotTicks - partial) / shotMax;
			double w = Math.sin(Math.PI * Math.max(0, Math.min(1, t)));
			w = w * w;
			Vec3 focus = spot(shotSide).add(0, 0.25 + height(shotSide) * 0.45, 0);
			double push = shotSide == 0 ? 0.42 : 0.36;
			x += (focus.x - x) * push * w;
			y += (focus.y - y) * push * 0.5 * w;
			z += (focus.z - z) * push * w;
			Vec3 look = camTarget.lerp(focus, 0.55 * w).subtract(x, y, z);
			yaw = Math.toDegrees(Math.atan2(-look.x, look.z));
			pitch = Math.toDegrees(Math.atan2(-look.y, Math.sqrt(look.x * look.x + look.z * look.z)));
		}
		return new double[] {x + sx, y + sy, z, yaw, pitch};
	}
}
