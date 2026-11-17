package com.insidethecube.kantojohto.client;

import com.insidethecube.kantojohto.RouteOne;
import com.insidethecube.kantojohto.game.MonDisplay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A battle on the spot: your Pokémon and the other one stand on the ground where you are, yours near and to
 * the left, theirs further off and to the right (as Gold and Silver lay it out), seen from where you stand.
 * The way it faces is the most open way round you, so trees and walls stay out of it.
 */
public final class BattleScene {
	private BattleScene() {}

	private static boolean active, revealed;
	private static Vec3 cam = Vec3.ZERO;
	private static float camYaw, camPitch;
	private static MonDisplay mine, theirs;
	private static int hiddenNpc = -1;
	private static Vec3 origin = Vec3.ZERO, fwd = new Vec3(0, 0, 1), right = new Vec3(-1, 0, 0);
	private static int nextId = -77000;
	private static ClientLevel level;

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

	public static MonDisplay display(int side) {
		return side == 0 ? mine : theirs;
	}

	/** Sets out the field: npc is the trainer you're battling (or -1), enemies the Pokémon you'll face. */
	public static void start(int npc, java.util.List<String> enemies) {
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
		double far = 7.5 + Math.max(0, h - 1.5) * 1.4;
		float yaw = openest(prefer, far);
		double r = Math.toRadians(yaw);
		fwd = new Vec3(-Math.sin(r), 0, Math.cos(r));
		right = new Vec3(-Math.cos(r), 0, -Math.sin(r));
		Vec3 m = at(3.0, -0.9), e = at(far, 1.5);
		mine = spawn(m, face(m, e));
		theirs = spawn(e, face(e, m));
		cam = origin.add(fwd.scale(0.6)).add(0, 1.25 + Math.max(0, h - 2) * 0.35, 0);
		Vec3 look = m.lerp(e, 0.55).add(0, 0.45 + h * 0.2, 0);
		Vec3 d = look.subtract(cam);
		camYaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
		camPitch = (float) Math.toDegrees(Math.atan2(-d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
		active = true;
		revealed = false;
	}

	private static Vec3 at(double ahead, double side) {
		Vec3 p = origin.add(fwd.scale(ahead)).add(right.scale(side));
		return new Vec3(p.x, ground(p.x, origin.y, p.z), p.z);
	}

	private static float face(Vec3 from, Vec3 to) {
		Vec3 d = to.subtract(from);
		return (float) Math.toDegrees(Math.atan2(-d.x, d.z));
	}

	private static MonDisplay spawn(Vec3 p, float yaw) {
		MonDisplay d = new MonDisplay(RouteOne.MON_DISPLAY, level);
		d.setId(nextId--);
		d.snapTo(p.x, p.y, p.z, yaw, 0);
		d.facing = yaw;
		d.size = d.sizeOld = d.sizeTarget = 0;
		level.addEntity(d);
		return d;
	}

	public static void end() {
		if (level != null) {
			for (MonDisplay d : new MonDisplay[] {mine, theirs}) if (d != null) level.removeEntity(d.getId(), Entity.RemovalReason.DISCARDED);
		}
		mine = theirs = null;
		hiddenNpc = -1;
		active = revealed = false;
		level = null;
	}

	/** If the world changed under a battle (left the game), forget it. */
	public static void tick(Minecraft mc) {
		if (active && mc.level != level) {
			level = null;
			mine = theirs = null;
			active = false;
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
		int bestScore = -1;
		for (int i = 0; i < 8; i++) {
			float yaw = Math.round(prefer / 45F) * 45F + (i % 2 == 0 ? 1 : -1) * ((i + 1) / 2) * 45F;
			double r = Math.toRadians(yaw);
			double fx = -Math.sin(r), fz = Math.cos(r), rx = -Math.cos(r), rz = -Math.sin(r);
			int score = 0;
			for (double a = 0.5; a <= far + 1; a += 0.5) {
				for (double side : new double[] {-0.9, 0, 1.5}) {
					double x = origin.x + fx * a + rx * side * Math.min(1, a / far * 1.2), z = origin.z + fz * a + rz * side * Math.min(1, a / far * 1.2);
					BlockPos feet = BlockPos.containing(x, origin.y + 0.3, z);
					if (!solid(feet) && !solid(feet.above())) score++;
					double gy = ground(x, origin.y, z);
					if (Math.abs(gy - origin.y) > 2.5) score--;
				}
			}
			// The way you face wins a tie (and near ties).
			score = score * 4 - i;
			if (score > bestScore) {
				bestScore = score;
				best = yaw;
			}
		}
		return Mth.wrapDegrees(best);
	}
}
