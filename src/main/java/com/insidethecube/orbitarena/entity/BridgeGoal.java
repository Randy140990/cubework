package com.insidethecube.orbitarena.entity;

import static com.insidethecube.orbitarena.arena.BridgeLayout.*;

import java.util.EnumSet;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * How a bot plays The Bridge.
 * <p>
 * Attackers go for the other team's goal and keep going: out of their room, across the bridge (building a
 * way of their own round anyone in the way, out over the void), up the other temple's stairs and into the
 * goal. They only stop to fight someone standing in their way or hitting them; anyone they pass gets a
 * swing. Defenders read who's coming: they meet attackers on the bridge (walling them off and knocking them
 * into space), get between them and the stairs on the deck, and guard the goal at the top of the stairs
 * they're coming up. Anyone far off gets an arrow.
 * <p>
 * The way round the map is planned in steps (room, door, the end of the bridge, the foot of the stairs, the
 * top, the goal), so a bot never wanders into its own goal or off the side of a temple.
 * <p>
 * How well it plays is its difficulty, 1 to 10: at 1 it walks, bridges slowly, sometimes walks off the edge
 * and rarely defends; from 3 it defends and eats; from 4 it bridges round you and walls you off; from 6 it
 * plays to knock you off; at 8 to 10 it runs as fast as you, never slips, decides quickly, goes up the stairs
 * you're not guarding, and in a 1 v 1 switches between stopping you and scoring itself.
 */
public final class BridgeGoal extends Goal {
	/** A bot's part in its team's game: who it plays for and how well. */
	public static final class Brain {
		public final BridgeWorld world;
		public final int team, index, difficulty;
		/** Attacking (heading for the other goal) or defending its own. */
		boolean defending;

		public Brain(BridgeWorld world, int team, int index, int difficulty) {
			this.world = world;
			this.team = team;
			this.index = index;
			this.difficulty = Mth.clamp(difficulty, 1, 10);
		}
	}

	private final Fighter f;
	private int placeCool, bowCool = 40, bowDraw = -1, decide, wallCool, stuck, mineTicks, swing, react, strafeTicks, wallsLeft = 2;
	private int stairs = 1;
	private float strafeSide = 1;
	private boolean jumpForCrit;
	private double lane;
	private BlockPos mining;
	private Vec3 lastPos = Vec3.ZERO, heading = Vec3.ZERO;
	private double lastWayDist = 999;
	private LivingEntity bowTarget;
	/** Its plan for getting where it's going (see BridgePath), how far along it is, and when to think again. */
	private List<BridgePath.Step> path;
	private int pathAt, planCool, pathStall;
	private BlockPos pathAim;
	private BridgePath.Habits habits;
	/** Its blocks this round (as many as anyone gets), and which round it last counted them for. */
	private int blocks, blocksRound = -1;
	/** Off to its restock chest. */
	private boolean restocking;
	/** A watch on whether it's getting anywhere: where it was, how long it's been there, how long it'll try anything. */
	private Vec3 watchAt = Vec3.ZERO;
	private int watchTicks, desperate;

	BridgeGoal(Fighter f) {
		this.f = f;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
	}

	@Override
	public boolean canUse() {
		return f.bridge != null && f.isAlive();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void stop() {
		f.stopUsingItem();
		f.getNavigation().stop();
		f.setShiftKeyDown(false);
	}

	private void hold(ItemStack stack) {
		if (!ItemStack.isSameItem(f.getMainHandItem(), stack)) f.setItemSlot(EquipmentSlot.MAINHAND, stack);
	}

	private ItemStack sword() {
		return f.bridgeSword;
	}

	// ---------------------------------------------------------------- where things are (relative to the map)

	private double rx(BridgeWorld w, double x) {
		return x - w.centreX();
	}

	private double rz(BridgeWorld w, double z) {
		return z - w.centreZ();
	}

	private double ry(BridgeWorld w, double y) {
		return y - w.y();
	}

	private Vec3 world(BridgeWorld w, double[] rel) {
		return new Vec3(w.centreX() + rel[0], w.y() + rel[1], w.centreZ() + rel[2]);
	}

	private static int sideOf(double rx) {
		return rx < 0 ? -1 : 1;
	}

	private enum Where {
		BRIDGE, DECK, ROOM, STAIRS, TOP, OFF
	}

	private Where where(BridgeWorld w, Vec3 p) {
		double ax = Math.abs(rx(w, p.x)), az = Math.abs(rz(w, p.z)), y = ry(w, p.y);
		if (ax <= BRIDGE_END + 0.5) return Where.BRIDGE;
		if (ax > BASE_BACK + 1.5 || y < -3) return Where.OFF;
		if (ax >= ROOM_FROM - 0.6 && ax <= ROOM_TO + 1 && az <= ROOM_HALF + 1 && y < ROOM_TOP + 0.5) return Where.ROOM;
		if (ax >= STAIR_FROM - 0.6 && ax <= LANDING_FROM + 0.4 && az >= STAIR_IN - 0.7 && az <= STAIR_OUT + 0.7 && y > 1.3) return Where.STAIRS;
		if (y > TOP + 0.5 && ax >= TEMPLE_FRONT - 0.5 && ax <= TEMPLE_BACK + 1 && az <= STAIR_OUT + 1) return Where.TOP;
		return Where.DECK;
	}

	private static double flat(Vec3 a, Vec3 b) {
		return Math.hypot(a.x - b.x, a.z - b.z);
	}

	/**
	 * The next place to head for on the way to a target: through the door, to the end of the bridge, along
	 * it, to the foot of the stairs, up them, round the goal.
	 */
	private Vec3 next(BridgeWorld w, Vec3 p, Vec3 t, int ownSide) {
		Where at = where(w, p), to = where(w, t);
		int sp = sideOf(rx(w, p.x)), st = sideOf(rx(w, t.x));
		int width = w.bridgeWidth();
		int k = Math.abs(rz(w, p.z)) > 1.5 ? (rz(w, p.z) < 0 ? -1 : 1) : stairs;
		boolean sameBase = sp == st && at != Where.BRIDGE && to != Where.BRIDGE && to != Where.OFF;
		boolean goingUp = sameBase && to == Where.TOP;
		switch (at) {
			case ROOM -> {
				return to == Where.ROOM && sameBase ? t : world(w, new double[] {sp * (TEMPLE_FRONT - 2) + 0.5, 1, 0.5});
			}
			case TOP -> {
				if (goingUp) return avoidPit(w, p, t, ownSide);
				// Down the stairs on this side.
				Vec3 top = world(w, stairTop(sp, k));
				return flat(p, top) > 1.2 ? avoidPit(w, p, top, ownSide) : world(w, stairFoot(sp, k));
			}
			case STAIRS -> {
				return goingUp ? world(w, stairTop(sp, k)) : world(w, stairFoot(sp, k));
			}
			case DECK -> {
				double ax = Math.abs(rx(w, p.x)), az = Math.abs(rz(w, p.z));
				// Beside or behind the temple: round to the front of it first.
				boolean besideTemple = ax > TEMPLE_FRONT - 1 && az <= STAIR_OUT + 1.2 && !(goingUp && az >= STAIR_IN - 0.7 && ax < STAIR_FROM + 0.5);
				if (besideTemple && !goingUp) return world(w, new double[] {sp * (TEMPLE_FRONT - 2) + 0.5, 1, k * (STAIR_OUT + 1.5) + 0.5});
				if (goingUp) {
					if (ax > STAIR_FROM && az > STAIR_OUT) return world(w, new double[] {sp * (TEMPLE_FRONT - 1) + 0.5, 1, k * (STAIR_OUT + 1.5) + 0.5});
					Vec3 foot = world(w, stairFoot(sp, k));
					return flat(p, foot) > 1.0 ? foot : world(w, stairTop(sp, k));
				}
				if (sameBase) return t;
				// Off to the bridge: to the front of the deck, then out along it (the far end of it, so it keeps
				// going; on the bridge it heads on from there).
				double mid = bridgeMidZ(width) + lane;
				if (ax < BASE_FRONT + 3.5 && Math.abs(rz(w, p.z) - mid) < 1.6) return world(w, new double[] {-sp * (BASE_FRONT + 2) + 0.5, 1, mid});
				return world(w, new double[] {sp * (BASE_FRONT + 1) + 0.5, 1, mid});
			}
			case BRIDGE -> {
				if (to == Where.BRIDGE) return t;
				return world(w, new double[] {st * (BASE_FRONT + 2) + 0.5, 1, bridgeMidZ(width) + (Math.abs(rx(w, p.x)) < BRIDGE_END - 3 ? lane : 0)});
			}
			default -> {
				return t;
			}
		}
	}

	/** On its own temple's top: round the front of the pit rather than across it. */
	private Vec3 avoidPit(BridgeWorld w, Vec3 p, Vec3 to, int ownSide) {
		if (sideOf(rx(w, p.x)) != ownSide) return to;
		double gx = w.centreX() + ownSide * GOAL_X + 0.5, gz = w.centreZ() + 0.5;
		// Does the line from here to there pass over the pit?
		double vx = to.x - p.x, vz = to.z - p.z, len2 = vx * vx + vz * vz;
		if (len2 < 0.01) return to;
		double t = Mth.clamp(((gx - p.x) * vx + (gz - p.z) * vz) / len2, 0, 1);
		double cxp = p.x + vx * t - gx, czp = p.z + vz * t - gz;
		if (Math.max(Math.abs(cxp), Math.abs(czp)) > GOAL_HALF + 1.3) return to;
		// Round the front of it, on this side.
		double side = p.z - gz >= 0 ? 1 : -1;
		Vec3 corner = new Vec3(gx - ownSide * (GOAL_HALF + 2), p.y, gz + side * (GOAL_HALF + 2));
		if (flat(p, corner) < 1.0) corner = new Vec3(gx - ownSide * (GOAL_HALF + 2), p.y, gz - side * (GOAL_HALF + 2));
		return corner;
	}

	// ---------------------------------------------------------------- the game

	@Override
	public void tick() {
		Brain b = f.bridge;
		if (b == null) return;
		BridgeWorld w = b.world;
		if (!w.playing()) {
			f.getNavigation().stop();
			f.getMoveControl().setWantedPosition(f.getX(), f.getY(), f.getZ(), 0);
			return;
		}
		f.getNavigation().stop();
		int d = b.difficulty;
		ServerLevel level = w.level();
		if (placeCool > 0) placeCool--;
		if (bowCool > 0) bowCool--;
		if (wallCool > 0) wallCool--;
		if (swing > 0) swing--;
		if (react > 0) react--;
		f.setShiftKeyDown(false);

		if (f.eating()) return;
		LivingEntity near = nearest(w.enemiesOf(b.team));
		double nd = near == null ? 999 : f.distanceTo(near);
		if (d >= 3 && f.getHealth() < f.getMaxHealth() * 0.4F && nd > 4) {
			f.eat();
			return;
		}

		if (--decide <= 0) {
			decide = 6 + (10 - d) * 2;
			choose(b, near);
		}

		int own = side(b.team), theirs = -own;
		Vec3 p = f.position();
		Vec3 target = b.defending ? defendTarget(w, b) : world(w, goal(theirs));
		Vec3 way = next(w, p, target, own);
		heading = new Vec3(way.x - p.x, 0, way.z - p.z);

		// Fight: anyone in the way or hitting it (and anyone near, when defending or from 6).
		boolean hitBy = near != null && f.getLastHurtByMob() == near && f.tickCount - f.getLastHurtByMobTimestamp() < 40;
		boolean inWay = near != null && inPath(near);
		if (near != null && nd < 3.6 + 0.08 * d && Math.abs(near.getY() - f.getY()) < 2.5 && f.hasLineOfSight(near)
				&& (b.defending || inWay || hitBy || d >= 6 && nd < 2.6)) {
			bowDraw = -1;
			if (f.isUsingItem()) f.stopUsingItem();
			melee(w, near, d);
			return;
		}
		f.setTarget(null);

		// Drawing a bow: hold still, aim, loose.
		if (bowDraw >= 0) {
			if (bowTarget == null || !bowTarget.isAlive() || bowTarget.isSpectator()) {
				bowDraw = -1;
				f.stopUsingItem();
			} else {
				f.getMoveControl().setWantedPosition(p.x, p.y, p.z, 0);
				f.getLookControl().setLookAt(bowTarget, 30, 30);
				if (++bowDraw >= 22 - d) shoot(level, bowTarget, d);
				return;
			}
		}
		Where at = where(w, p);
		if (near != null && nd > 9 && nd < 30 && bowCool == 0 && d >= 2 && at != Where.STAIRS && at != Where.ROOM && f.onGround() && f.hasLineOfSight(near)
				&& f.getRandom().nextFloat() < (b.defending ? 0.05F : 0.02F) + 0.01F * d) {
			hold(new ItemStack(Items.BOW));
			f.startUsingItem(InteractionHand.MAIN_HAND);
			bowTarget = near;
			bowDraw = 0;
			return;
		}

		// A swing at anyone it passes.
		if (near != null && nd < f.reach() && swing == 0 && f.hasLineOfSight(near)) {
			f.getLookControl().setLookAt(near, 40, 40);
			hit(level, near, d, false);
		}
		// A wall in front of an attacker out on the bridge (defenders, from 4; a couple a life, not a fort).
		if (at == Where.ROOM) wallsLeft = 2;
		if (b.defending && d >= 4 && wallCool == 0 && wallsLeft > 0 && near != null && nd < 8 && Math.abs(rx(w, near.getX())) <= BRIDGE_END - 3
				&& own * (rx(w, near.getX()) - rx(w, p.x)) < 0 && f.getRandom().nextFloat() < 0.04F * (d - 3)) {
			wall(w, near, own, d);
		}
		travel(w, b, way, target, d);
	}

	/** Attack or defend, which stairs, and which way round anyone in the way on the bridge. */
	private void choose(Brain b, LivingEntity near) {
		BridgeWorld w = b.world;
		int d = b.difficulty, own = side(b.team);
		LivingEntity threat = threat(w, b.team);
		double goalX = w.centreX() + own * GOAL_X + 0.5;
		boolean was = b.defending;
		if (w.teamSize() == 1) {
			// On its own: stop anyone nearer its goal than it is and coming (more often and sooner as the
			// difficulty rises), otherwise go and score.
			boolean danger = threat != null && Math.abs(threat.getX() - goalX) < Math.abs(f.getX() - goalX) + 8;
			if (!danger) b.defending = false;
			else if (!was) b.defending = f.getRandom().nextFloat() < 0.12F + 0.088F * d;
		} else {
			// In a team, everyone goes to score while nobody's coming (but for a keeper, in big teams of good
			// bots). When they come, as many bots as there are attackers on our half go back to stop them, the
			// nearest to our goal first, always leaving one to keep attacking where there are two or more; any
			// already on their base, about to score, keeps going.
			List<Fighter> bots = new ArrayList<>();
			for (LivingEntity a : w.alliesOf(b.team)) if (a instanceof Fighter fa && fa.bridge != null) bots.add(fa);
			int coming = 0;
			for (LivingEntity e : w.enemiesOf(b.team)) if (own * rx(w, e.getX()) > -6) coming++;
			if (coming == 0) {
				b.defending = bots.size() >= 3 && d >= 7 && b.index == 1;
			} else {
				int want = Math.min(coming, Math.max(1, bots.size() - 1));
				bots.removeIf(a -> a != f && -own * rx(w, a.getX()) > BASE_FRONT);
				bots.sort((x1, x2) -> Double.compare(Math.abs(x1.getX() - goalX), Math.abs(x2.getX() - goalX)));
				int rank = bots.indexOf(f);
				boolean onTheirBase = -own * rx(w, f.getX()) > BASE_FRONT;
				boolean should = !onTheirBase && rank >= 0 && rank < want;
				// Low levels are slow to see it.
				b.defending = should && (was || d >= 3 || f.getRandom().nextFloat() < 0.3F + 0.15F * d);
			}
		}
		// Which stairs: from 8, the side nobody's guarding; otherwise the side it's on, or either.
		if (!b.defending) {
			if (d >= 8 && near != null && Math.abs(rx(w, near.getX())) > TEMPLE_FRONT - 4) stairs = rz(w, near.getZ()) > 0.5 ? -1 : 1;
			else if (Math.abs(rx(w, f.getX())) <= BRIDGE_END && f.getRandom().nextFloat() < 0.15F) stairs = f.getRandom().nextBoolean() ? 1 : -1;
		}
		// Round whoever's on the bridge ahead (from 4, by bridging out to one side), or along the middle.
		boolean blocked = false;
		if (near != null && !b.defending) {
			double ahead = (near.getX() - f.getX()) * -own;
			blocked = ahead > 1.5 && ahead < 12 && Math.abs(rx(w, near.getX())) <= BRIDGE_END && Math.abs(near.getZ() - f.getZ()) < 2.5;
		}
		if (blocked && d >= 4) {
			if (Math.abs(lane) < 2) lane = f.getRandom().nextBoolean() ? 2.5 : -2.5;
		} else if (f.getRandom().nextFloat() < 0.4F) {
			lane = 0;
		}
	}

	/** Where a defender should be: on the bridge in front of an attacker, between them and the stairs, or by the goal. */
	private Vec3 defendTarget(BridgeWorld w, Brain b) {
		int own = side(b.team), d = b.difficulty;
		LivingEntity threat = threat(w, b.team);
		if (threat == null) {
			// Nobody coming: hold the end of the bridge (good bots), or the goal.
			return d >= 5 ? world(w, front(own, w.bridgeWidth())) : world(w, guard(own, stairs));
		}
		Vec3 t = threat.position();
		Where tw = where(w, t);
		int ts = sideOf(rx(w, t.x));
		if (tw == Where.BRIDGE || ts != own) {
			// Meet them on the bridge, a couple of blocks on our side of them.
			double x = t.x + own * 2.0;
			if (own * rx(w, x) < 2) x = w.centreX() + own * 2.0;
			return new Vec3(x, w.y() + 1, t.z);
		}
		int k = rz(w, t.z) < 0 ? -1 : 1;
		stairs = k;
		if (tw == Where.STAIRS || tw == Where.TOP) return world(w, guard(own, k));
		// On our deck: between them and the stairs they're making for.
		Vec3 foot = world(w, stairFoot(own, k));
		Vec3 dir = foot.subtract(t);
		double len = Math.max(0.1, Math.hypot(dir.x, dir.z));
		return new Vec3(t.x + dir.x / len * 1.8, w.y() + 1, t.z + dir.z / len * 1.8);
	}

	/** The enemy closest to this team's goal, if any is on this half. */
	private LivingEntity threat(BridgeWorld w, int team) {
		int own = side(team);
		double goal = w.centreX() + own * GOAL_X + 0.5;
		LivingEntity best = null;
		double bestD = 999;
		for (LivingEntity e : w.enemiesOf(team)) {
			if (own * rx(w, e.getX()) < -6) continue;
			double dist = Math.abs(e.getX() - goal) + Math.abs(e.getZ() - w.centreZ()) * 0.3;
			if (dist < bestD) {
				bestD = dist;
				best = e;
			}
		}
		return best;
	}

	private LivingEntity nearest(List<LivingEntity> list) {
		LivingEntity best = null;
		double bestD = Double.MAX_VALUE;
		for (LivingEntity e : list) {
			double dist = f.distanceToSqr(e);
			if (dist < bestD) {
				bestD = dist;
				best = e;
			}
		}
		return best;
	}

	/** Whether someone's standing in the way it's going (a few blocks ahead, near its line). */
	private boolean inPath(LivingEntity e) {
		double len = Math.hypot(heading.x, heading.z);
		if (len < 0.1) return false;
		double ux = heading.x / len, uz = heading.z / len, ex = e.getX() - f.getX(), ez = e.getZ() - f.getZ();
		double ahead = ex * ux + ez * uz, side = Math.abs(ex * uz - ez * ux);
		return ahead > 0.3 && ahead < 5.5 && side < 2.0;
	}

	// ---------------------------------------------------------------- fighting

	/** A fight: close in (never off an edge), strafe where there's room, jump for crits, knock them off. */
	private void melee(BridgeWorld w, LivingEntity t, int d) {
		ServerLevel level = w.level();
		f.setTarget(t);
		hold(sword());
		f.getLookControl().setLookAt(t, 40, 40);
		float face = (float) (Mth.atan2(t.getZ() - f.getZ(), t.getX() - f.getX()) * Mth.RAD_TO_DEG) - 90;
		double dist = f.distanceTo(t), reach = f.reach();
		Vec3 p = f.position();
		if (dist > reach - 0.4) {
			Vec3 to = t.position().subtract(p);
			double len = Math.max(0.01, Math.hypot(to.x, to.z));
			if (safe(w, p.x + to.x / len * 0.9, p.y, p.z + to.z / len * 0.9)) {
				f.setSprinting(d >= 3);
				f.getMoveControl().setWantedPosition(t.getX(), p.y, t.getZ(), d >= 3 ? 1.0 : 0.85);
			} else {
				f.getMoveControl().setWantedPosition(p.x, p.y, p.z, 0);
			}
		} else {
			f.setYRot(face);
			f.yBodyRot = face;
			// Strafe round them where there's floor both sides (from 3), keeping to the middle (from 6).
			boolean room = safe(w, p.x + Mth.cos(face * Mth.DEG_TO_RAD) * 1.2, p.y, p.z + Mth.sin(face * Mth.DEG_TO_RAD) * 1.2)
					&& safe(w, p.x - Mth.cos(face * Mth.DEG_TO_RAD) * 1.2, p.y, p.z - Mth.sin(face * Mth.DEG_TO_RAD) * 1.2);
			if (d >= 3 && room) {
				if (--strafeTicks <= 0) {
					strafeSide = f.getRandom().nextBoolean() ? 1 : -1;
					strafeTicks = 8 + f.getRandom().nextInt(Math.max(4, 40 - 3 * d));
				}
				float forward = dist > 2.8 ? 0.35F : dist < 1.8 ? -0.3F : 0;
				f.getMoveControl().strafe(forward, strafeSide * (0.2F + 0.04F * d));
			} else {
				f.getMoveControl().setWantedPosition(p.x, p.y, p.z, 0);
			}
			if (d >= 6) {
				// Edge in towards the middle of the bridge, so it's them who goes off.
				double mz = w.centreZ() + bridgeMidZ(w.bridgeWidth());
				if (Math.abs(rx(w, p.x)) <= BRIDGE_END + 1 && Math.abs(f.getZ() - mz) > 0.35) {
					f.setDeltaMovement(f.getDeltaMovement().add(0, 0, Math.signum(mz - f.getZ()) * 0.02 * (d - 4) / 6.0));
				}
			}
			if (d >= 4 && swing <= 4 && f.onGround() && !jumpForCrit && f.getRandom().nextFloat() < 0.05F * (d - 3)) {
				f.getJumpControl().jump();
				jumpForCrit = true;
			}
		}
		boolean falling = !f.onGround() && f.getDeltaMovement().y < 0;
		boolean waitForDrop = jumpForCrit && !falling && !f.onGround();
		if (swing == 0 && dist <= reach && !waitForDrop && f.hasLineOfSight(t)) hit(level, t, d, d >= 6);
	}

	/** A swing (low levels miss now and then); a sprint hit knocks them further. */
	private void hit(ServerLevel level, LivingEntity t, int d, boolean sprintHit) {
		if (react > 0) return;
		hold(sword());
		f.swing(InteractionHand.MAIN_HAND);
		if (f.getRandom().nextFloat() >= Math.max(0, 0.35F - 0.05F * d)) {
			boolean hit = f.doHurtTarget(level, t);
			if (hit && sprintHit) t.knockback(0.35 + 0.02 * d, f.getX() - t.getX(), f.getZ() - t.getZ());
		}
		swing = f.swingTicks() + f.getRandom().nextInt(Math.max(1, (11 - d) * 2));
		react = Math.max(1, 10 - d);
		jumpForCrit = false;
	}

	// ---------------------------------------------------------------- moving

	private boolean solid(ServerLevel level, BlockPos pos) {
		return !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
	}

	/** How far down to the first floor below a spot (0: there's a floor at that level), or 99 if none near. */
	private int drop(ServerLevel level, BlockPos floor) {
		for (int k = 0; k <= 12; k++) if (solid(level, floor.below(k))) return k;
		return 99;
	}

	/** Whether stepping to (x, z) keeps it on something (a drop of a block or two at most). */
	private boolean safe(BridgeWorld w, double x, double y, double z) {
		return drop(w.level(), BlockPos.containing(x, y - 0.5, z)) <= 2;
	}

	private boolean nearOwnGoal(BridgeWorld w, double x, double z, int s) {
		return Math.abs(x - (w.centreX() + s * GOAL_X + 0.5)) < GOAL_HALF + 1.2 && Math.abs(z - (w.centreZ() + 0.5)) < GOAL_HALF + 1.2;
	}

	/**
	 * Walks towards the next place: bridges a gap over the void (or waits at the edge), walks down steps and
	 * drops a block or two, jumps a block in the way, breaks through placed walls.
	 */
	private void steer(BridgeWorld w, Brain b, Vec3 way, Vec3 target, int d) {
		ServerLevel level = w.level();
		Vec3 p = f.position();
		int own = side(b.team);
		boolean attacking = !b.defending;
		double wx = p.x + Mth.clamp(way.x - p.x, -2.5, 2.5), wz = p.z + Mth.clamp(way.z - p.z, -2.5, 2.5);
		double dx = wx - p.x, dz = wz - p.z, len = Math.sqrt(dx * dx + dz * dz);
		if (len < 0.2) {
			f.getMoveControl().setWantedPosition(p.x, p.y, p.z, 0);
			f.setSprinting(false);
			return;
		}
		dx /= len;
		dz /= len;
		f.getLookControl().setLookAt(wx, f.getEyeY(), wz);

		// Breaking through a wall of placed blocks.
		if (mining != null) {
			hold(new ItemStack(Items.DIAMOND_PICKAXE));
			f.getMoveControl().setWantedPosition(p.x, p.y, p.z, 0);
			if (mineTicks % 4 == 0) f.swing(InteractionHand.MAIN_HAND);
			if (--mineTicks <= 0) {
				if (w.breakable(mining)) level.destroyBlock(mining, false, f);
				mining = null;
			}
			return;
		}

		int groundY = Mth.floor(p.y - 0.5);
		BlockPos ahead = BlockPos.containing(p.x + dx * 0.9, groundY, p.z + dz * 0.9);
		boolean intoGoal = attacking && flat(p, world(w, goal(-own))) < GOAL_HALF + 2.5;
		if (f.onGround() && !intoGoal && !solid(level, ahead)) {
			int down = drop(level, ahead);
			boolean wantDown = way.y < p.y - 1.5 || target.y < p.y - 1.5;
			boolean ownPit = nearOwnGoal(w, ahead.getX() + 0.5, ahead.getZ() + 0.5, own) && ry(w, p.y) > TOP - 1;
			if (ownPit) {
				// Never into its own goal: round it.
				lane = 0;
				f.getMoveControl().setWantedPosition(p.x - dz, p.y, p.z + dx, 0.8);
				return;
			}
			if (!(down <= 2 || wantDown && down <= 12)) {
				// A gap over the void: build across it (slower and with the odd slip at low levels), or wait.
				if (f.getRandom().nextFloat() < Math.max(0, 0.04F - 0.005F * d)) {
					f.getMoveControl().setWantedPosition(wx, p.y, wz, 1.0);
					return;
				}
				f.setShiftKeyDown(true);
				f.setSprinting(false);
				f.getMoveControl().setWantedPosition(p.x, p.y, p.z, 0);
				if (placeCool == 0 && w.buildable(ahead) && level.getBlockState(ahead).isAir()) place(w, b, ahead);
				else if (!w.buildable(ahead)) lane = 0;
				return;
			}
		}

		BlockPos feet = BlockPos.containing(p.x + dx * 0.8, p.y + 0.2, p.z + dz * 0.8), head = feet.above();
		boolean feetBlocked = solid(level, feet), headBlocked = solid(level, head);
		if (headBlocked) {
			// Something placed in the way, at head height or both: break through it (it's only wool).
			BlockPos breakable = w.breakable(head) && solid(level, head) ? head : w.breakable(feet) && feetBlocked ? feet : null;
			if (breakable != null) {
				mine(breakable, d);
				return;
			}
			if (feetBlocked && Math.abs(rx(w, p.x)) <= BRIDGE_END) lane = Mth.clamp(lane + (f.getRandom().nextBoolean() ? 2 : -2), -3, 3);
		} else if (feetBlocked && f.onGround() && !level.getBlockState(feet).is(net.minecraft.tags.BlockTags.STAIRS)) {
			// A step up: jump it, first breaking anything over its head that would stop the jump.
			BlockPos over = BlockPos.containing(p.x, p.y + 2.2, p.z), overStep = feet.above(2);
			if (solid(level, over) && w.breakable(over)) {
				mine(over, d);
				return;
			}
			if (solid(level, overStep) && w.breakable(overStep)) {
				mine(overStep, d);
				return;
			}
			f.getJumpControl().jump();
		}

		// Stuck (getting no nearer where it's going): break whatever placed blocks are round it in the way,
		// then try another way round.
		if (f.tickCount % 20 == 0) {
			double dw = flat(p, way);
			boolean noNearer = dw > 1.5 && dw > lastWayDist - 0.4;
			if (noNearer || p.distanceToSqr(lastPos) < 0.05 && dw > 1.5) {
				stuck++;
				BlockPos clear = stuck >= 1 ? inTheWay(w, p, dx, dz) : null;
				if (clear != null) {
					mine(clear, d);
					lastWayDist = 999;
					lastPos = p;
					return;
				}
				if (stuck >= 2) {
					if (Math.abs(rx(w, p.x)) <= BRIDGE_END) lane = Mth.clamp(lane + (f.getRandom().nextBoolean() ? 2.5 : -2.5), -3, 3);
					else stairs = -stairs;
					if (f.onGround()) f.getJumpControl().jump();
					stuck = 0;
				}
			} else {
				stuck = 0;
			}
			lastWayDist = dw;
			lastPos = p;
		}
		hold(sword());
		boolean sprint = d >= 3 && (attacking || flat(p, target) > 6) && Math.abs(lane) < 2.5;
		f.setSprinting(sprint);
		f.getMoveControl().setWantedPosition(wx, p.y, wz, sprint ? 1.0 : 0.85);
	}

	// ---------------------------------------------------------------- getting there, whatever's in the way

	/** Its habits (from which bot it is): a digger, a builder or a climber, and what its level allows. */
	private BridgePath.Habits habits(int d) {
		if (habits == null) {
			int kind = Math.floorMod(f.getUUID().hashCode(), 3);
			// A digger breaks through; a builder bridges round; a climber builds a step and goes over.
			double place = kind == 1 ? 1.8 : kind == 0 ? 3.2 : 2.2, mine = kind == 0 ? 1.6 : kind == 1 ? 9 : 6, climb = kind == 2 ? 0.4 : 1.6;
			habits = new BridgePath.Habits(place + (10 - d) * 0.15, mine, climb, d >= 3, d >= 5, true);
		}
		// Stuck a while: anything goes (dig, build, climb, all cheap) until it's moving again.
		if (desperate > 0) return new BridgePath.Habits(1.2, 1.0, 0.5, d >= 2 && blocks > 0, d >= 2 && blocks > 0, blocks > 0);
		// With no blocks left it can only walk, drop and dig.
		if (blocks <= 0 && habits.build()) return new BridgePath.Habits(habits.place(), habits.mine(), habits.climb(), false, false, false);
		return habits;
	}

	/**
	 * Heads where it's going as a player would: with a plan (BridgePath) that walks, drops, breaks through,
	 * bridges round, steps up over or pillars up past whatever's in the way, followed step by step and
	 * thought again whenever it goes wrong. Where no plan can be made, it falls back on steering straight.
	 */
	private void travel(BridgeWorld w, Brain b, Vec3 way, Vec3 target, int d) {
		ServerLevel level = w.level();
		Vec3 p = f.position();
		if (blocksRound != w.round()) {
			blocksRound = w.round();
			blocks = w.blocksPerRound();
			restocking = false;
		}
		if (mining != null) {
			hold(new ItemStack(Items.DIAMOND_PICKAXE));
			f.getMoveControl().setWantedPosition(p.x, p.y, p.z, 0);
			f.getLookControl().setLookAt(mining.getX() + 0.5, mining.getY() + 0.5, mining.getZ() + 0.5);
			if (mineTicks % 4 == 0) f.swing(InteractionHand.MAIN_HAND);
			if (--mineTicks <= 0) {
				if (w.breakable(mining)) level.destroyBlock(mining, false, f);
				mining = null;
			}
			return;
		}
		int own = side(b.team), theirs = -own;
		Vec3 goalAt = world(w, goal(theirs));
		// Running low on blocks (from 2): down to a restock chest under the bridge, if there's
		// anything in it and nobody's about to score.
		// (Whichever chest is nearer and has blocks in it: what colour they are makes no odds to a bot.)
		int ct = w.chestBlocks(b.team) == 0 || w.chestBlocks(1 - b.team) > 0 && p.distanceToSqr(Vec3.atCenterOf(w.chest(1 - b.team))) < p.distanceToSqr(Vec3.atCenterOf(w.chest(b.team)))
				? 1 - b.team : b.team;
		BlockPos chest = w.chest(ct);
		if (!restocking && blocks < 16 && d >= 2 && w.chestBlocks(ct) > 0 && (!b.defending || nearestThreatFar(w, b))) restocking = true;
		if (restocking && (blocks >= w.blocksPerRound() / 2 || w.chestBlocks(ct) == 0 || b.defending && !nearestThreatFar(w, b))) restocking = false;
		if (restocking && p.distanceToSqr(chest.getX() + 1.0, chest.getY() + 0.5, chest.getZ() + 0.5) < 9) {
			// At the chest: open it, take what it needs.
			f.getLookControl().setLookAt(chest.getX() + 1.0, chest.getY() + 0.5, chest.getZ() + 0.5);
			f.swing(InteractionHand.MAIN_HAND);
			level.playSound(null, chest, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.5F, 1.0F);
			blocks += w.takeBlocks(ct, w.blocksPerRound() - blocks);
			restocking = false;
			path = null;
			return;
		}
		// Near their goal, straight for it; otherwise to the route's next point (or where it's defending).
		boolean forGoal = !restocking && !b.defending && flat(p, goalAt) < 36;
		Vec3 aim = restocking ? new Vec3(chest.getX() + 1.0, chest.getY() + 1, chest.getZ() + 0.5) : forGoal ? new Vec3(goalAt.x, w.y() + TOP - 2, goalAt.z)
				: b.defending ? target : way;
		BlockPos aimBlock = BlockPos.containing(aim);
		BlockPos feet = BlockPos.containing(p.x, p.y + 0.6, p.z);
		// Not getting anywhere (three seconds in the same spot, with somewhere to be): think again, anything goes.
		if (desperate > 0) desperate--;
		if (f.tickCount % 20 == 0) {
			if (p.distanceToSqr(watchAt) < 0.5 && p.distanceTo(aim) > 2.5) watchTicks += 20;
			else watchTicks = 0;
			watchAt = p;
			if (watchTicks >= 60) {
				watchTicks = 0;
				desperate = 140;
				path = null;
			}
		}
		boolean lost = path != null && pathAt < path.size() && (flatTo(p, path.get(pathAt).at()) > 3.2 || Math.abs(p.y - path.get(pathAt).at().getY()) > 3);
		boolean rethink = path == null || pathAt >= path.size() || --planCool <= 0 || lost || pathAim == null || pathAim.distSqr(aimBlock) > 6;
		// (It thinks again with its feet on something: not halfway through a jump or a drop.)
		if (rethink && f.onGround()) plan(w, b, feet, aim, forGoal, d);
		if (path == null || path.isEmpty()) {
			steer(w, b, way, target, d);
			return;
		}
		follow(w, b, p, d);
	}

	/** Whether nobody on the other team is close to scoring (none on its own half, past the middle of the bridge). */
	private boolean nearestThreatFar(BridgeWorld w, Brain b) {
		int own = side(b.team);
		for (LivingEntity e : w.enemiesOf(b.team)) if (own * rx(w, e.getX()) > 4) return false;
		return true;
	}

	private double flatTo(Vec3 p, BlockPos at) {
		double dx = p.x - (at.getX() + 0.5), dz = p.z - (at.getZ() + 0.5);
		return Math.sqrt(dx * dx + dz * dz);
	}

	private void plan(BridgeWorld w, Brain b, BlockPos feet, Vec3 aim, boolean forGoal, int d) {
		int own = side(b.team), theirs = -own, cx = w.centreX(), cz = w.centreZ(), y0 = w.y();
		// Never into its own goal; their goal is done the moment it's in the pit.
		java.util.function.Predicate<BlockPos> ownPit = q -> Math.max(Math.abs(q.getX() - cx - own * GOAL_X), Math.abs(q.getZ() - cz)) <= GOAL_HALF && q.getY() - y0 <= TOP;
		java.util.function.Predicate<BlockPos> done;
		if (forGoal) {
			done = q -> Math.max(Math.abs(q.getX() - cx - theirs * GOAL_X), Math.abs(q.getZ() - cz)) <= GOAL_HALF && q.getY() - y0 <= TOP;
		} else if (restocking) {
			done = q -> Math.abs(q.getX() + 0.5 - aim.x) < 2.2 && Math.abs(q.getZ() + 0.5 - aim.z) < 2.2 && Math.abs(q.getY() - aim.y) <= 2;
		} else {
			done = q -> Math.abs(q.getX() + 0.5 - aim.x) < 1.2 && Math.abs(q.getZ() + 0.5 - aim.z) < 1.2 && Math.abs(q.getY() - aim.y) <= 1.5;
		}
		path = new BridgePath(w).find(feet, aim, done, ownPit, habits(d), 2200 + 380 * d);
		pathAt = 0;
		pathStall = 0;
		pathAim = BlockPos.containing(aim);
		planCool = 30 + (10 - d) * 3 + f.getRandom().nextInt(8);
	}

	/** Takes the next step of the plan: getting to it, and first whatever has to be done to make it. */
	private void follow(BridgeWorld w, Brain b, Vec3 p, int d) {
		ServerLevel level = w.level();
		BridgePath.Step s = path.get(pathAt);
		BlockPos at = s.at();
		double hd = flatTo(p, at);
		boolean landed = f.onGround() || s.act() == BridgePath.Act.DROP && p.y < at.getY() + 0.3;
		if (hd < 0.42 && Math.abs(p.y - at.getY()) < 0.65 && landed) {
			pathAt++;
			pathStall = 0;
			if (pathAt >= path.size()) {
				f.getMoveControl().setWantedPosition(p.x, p.y, p.z, 0);
				return;
			}
			s = path.get(pathAt);
			at = s.at();
			hd = flatTo(p, at);
		}
		if (!new BridgePath(w).stillGood(s)) {
			path = null;
			return;
		}
		if (++pathStall > 70) {
			// Getting nowhere: think again (a little less keen on what it was trying).
			path = null;
			habits = null;
			return;
		}
		double cx = at.getX() + 0.5, cz = at.getZ() + 0.5;
		f.setShiftKeyDown(false);
		switch (s.act()) {
			case MINE -> {
				for (BlockPos q : new BlockPos[] {at.above(), at}) {
					if (solid(level, q)) {
						if (!w.breakable(q)) {
							path = null;
							return;
						}
						mine(q, d);
						return;
					}
				}
			}
			case BRIDGE, DESCEND -> {
				BlockPos floor = at.below();
				if (!solid(level, floor)) {
					if (blocks <= 0) {
						path = null;
						return;
					}
					// At the edge, crouched, placing the next block out (low levels now and then just step off).
					f.setShiftKeyDown(true);
					f.setSprinting(false);
					f.getLookControl().setLookAt(floor.getX() + 0.5, floor.getY() + 0.5, floor.getZ() + 0.5);
					if (d <= 2 && f.getRandom().nextFloat() < 0.01F) {
						f.getMoveControl().setWantedPosition(cx, at.getY(), cz, 1.0);
						return;
					}
					if (hd > 1.5) {
						BlockPos back = pathAt > 0 ? path.get(pathAt - 1).at() : BlockPos.containing(p);
						f.getMoveControl().setWantedPosition(back.getX() + 0.5, back.getY(), back.getZ() + 0.5, 0.6);
						return;
					}
					f.getMoveControl().setWantedPosition(p.x, p.y, p.z, 0);
					if (placeCool == 0) place(w, b, floor);
					return;
				}
			}
			case STAIR -> {
				BlockPos step = at.below();
				if (!solid(level, step)) {
					if (blocks <= 0) {
						path = null;
						return;
					}
					f.setSprinting(false);
					f.getLookControl().setLookAt(step.getX() + 0.5, step.getY() + 0.5, step.getZ() + 0.5);
					if (!level.getEntities(f, new AABB(step)).isEmpty()) {
						path = null;
						return;
					}
					if (hd > 2.2) break;
					f.getMoveControl().setWantedPosition(p.x, p.y, p.z, 0);
					if (placeCool == 0) place(w, b, step);
					return;
				}
			}
			case PILLAR -> {
				BlockPos under = at.below();
				if (!solid(level, under)) {
					if (blocks <= 0) {
						path = null;
						return;
					}
					f.setSprinting(false);
					f.getMoveControl().setWantedPosition(under.getX() + 0.5, p.y, under.getZ() + 0.5, 0.5);
					f.getLookControl().setLookAt(under.getX() + 0.5, under.getY(), under.getZ() + 0.5);
					if (f.onGround()) f.getJumpControl().jump();
					else if (p.y >= under.getY() + 1.0 && placeCool == 0) place(w, b, under);
					return;
				}
			}
			default -> {
			}
		}
		// On to it: jump up a block, walk, or step off the edge.
		hold(sword());
		f.getLookControl().setLookAt(cx, f.getEyeY(), cz);
		boolean up = (s.act() == BridgePath.Act.UP || s.act() == BridgePath.Act.STAIR) && !level.getBlockState(at.below()).is(net.minecraft.tags.BlockTags.STAIRS);
		if (up && f.onGround() && hd < 1.5 && at.getY() > p.y + 0.4) f.getJumpControl().jump();
		boolean clear = true;
		for (int k = pathAt; k < Math.min(path.size(), pathAt + 4); k++) {
			BridgePath.Act a = path.get(k).act();
			if (a != BridgePath.Act.WALK && a != BridgePath.Act.DROP) clear = false;
		}
		boolean sprint = d >= 3 && clear && (!b.defending || hd > 3);
		f.setSprinting(sprint);
		f.getMoveControl().setWantedPosition(cx, at.getY(), cz, sprint ? 1.0 : 0.85);
	}

	/** Starts breaking a placed block (quicker the better it is). */
	private void mine(BlockPos pos, int d) {
		mining = pos;
		mineTicks = Math.max(3, 12 - d);
	}

	/**
	 * A placed block round it that's in its way (ahead at its feet or head, over its head, or to either side
	 * ahead when it's going slantwise), or null.
	 */
	private BlockPos inTheWay(BridgeWorld w, Vec3 p, double dx, double dz) {
		ServerLevel level = w.level();
		BlockPos at = BlockPos.containing(p.x, p.y + 0.2, p.z);
		BlockPos ahead = BlockPos.containing(p.x + dx * 0.8, p.y + 0.2, p.z + dz * 0.8);
		BlockPos ax = BlockPos.containing(p.x + Math.signum(dx) * 0.8, p.y + 0.2, p.z), az = BlockPos.containing(p.x, p.y + 0.2, p.z + Math.signum(dz) * 0.8);
		BlockPos far = BlockPos.containing(p.x + dx * 1.6, p.y + 0.2, p.z + dz * 1.6);
		BlockPos[] tries = {ahead.above(), ahead, at.above(2), ahead.above(2), ax.above(), ax, az.above(), az, far.above(), far};
		for (BlockPos q : tries) if (solid(level, q) && w.breakable(q)) return q;
		return null;
	}

	/** Places one of its team's blocks (as fast as its difficulty allows). */
	private void place(BridgeWorld w, Brain b, BlockPos pos) {
		if (blocks <= 0) return;
		blocks--;
		BlockState state = w.blockFor(b.team);
		hold(new ItemStack(state.getBlock().asItem()));
		w.level().setBlock(pos, state, 3);
		w.level().playSound(null, pos, state.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 0.8F);
		f.swing(InteractionHand.MAIN_HAND);
		placeCool = Math.max(2, 14 - (int) (1.3 * b.difficulty));
	}

	/** A wall across an attacker's path on the bridge, two high, between them and its base (toward: its side). */
	private void wall(BridgeWorld w, LivingEntity threat, double toward, int d) {
		ServerLevel level = w.level();
		int x = Mth.floor(threat.getX() + toward * 2), y = Mth.floor(threat.getY()), z0 = Mth.floor(threat.getZ());
		// Only across the bridge itself (never a heap on a deck).
		if (Math.abs(rx(w, x + 0.5)) > BRIDGE_END) {
			wallCool = 10;
			return;
		}
		int placed = 0;
		for (int dz = -1; dz <= 1; dz++) {
			for (int dy = 0; dy <= 1; dy++) {
				BlockPos pos = new BlockPos(x, y + dy, z0 + dz);
				if (level.getBlockState(pos).isAir() && w.buildable(pos) && level.getEntities(null, new AABB(pos)).isEmpty()) {
					place(w, f.bridge, pos);
					placed++;
				}
			}
		}
		if (placed > 0) wallsLeft--;
		wallCool = placed > 0 ? 200 - 8 * d : 10;
	}

	/** Looses an arrow, leading a moving target at higher levels. */
	private void shoot(ServerLevel level, LivingEntity t, int d) {
		ItemStack bow = f.getMainHandItem().is(Items.BOW) ? f.getMainHandItem() : new ItemStack(Items.BOW);
		Arrow arrow = new Arrow(level, f, new ItemStack(Items.ARROW), bow);
		double ex = t.getX() - f.getX(), ez = t.getZ() - f.getZ(), h = Math.sqrt(ex * ex + ez * ez);
		float speed = 1.6F + 0.08F * d;
		double lead = (d / 10.0) * h / (speed * 20) * 20;
		ex += t.getDeltaMovement().x * lead;
		ez += t.getDeltaMovement().z * lead;
		double ey = t.getY(0.4) - arrow.getY();
		arrow.shoot(ex, ey + h * 0.18, ez, speed, Math.max(0.4F, 12 - 1.15F * d));
		arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
		level.addFreshEntity(arrow);
		level.playSound(null, f.getX(), f.getY(), f.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.HOSTILE, 1.0F, 1.0F / (f.getRandom().nextFloat() * 0.4F + 1.2F) + 0.5F);
		f.stopUsingItem();
		bowDraw = -1;
		bowCool = 80;
		hold(sword());
	}
}
