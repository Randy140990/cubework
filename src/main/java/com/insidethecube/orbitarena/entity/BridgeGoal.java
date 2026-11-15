package com.insidethecube.orbitarena.entity;

import static com.insidethecube.orbitarena.arena.BridgeLayout.*;

import java.util.EnumSet;
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
import net.minecraft.world.phys.Vec3;

/**
 * How a bot plays The Bridge. Attackers head for the other team's goal: along the bridge, round anyone in
 * the way by building a path of their own out over the void ("bridging"), jumping or breaking through
 * walls, and drop into the goal. Defenders stand between the nearest attacker and their own goal, wall them
 * off and knock them off the edge. Anyone close gets a fight (as DuelGoal fights, keeping to the middle so
 * the knockback sends the other one off); anyone far, an arrow.
 * <p>
 * How well it plays is its difficulty, 1 to 10: at 1 it bridges slowly and sometimes walks off the edge,
 * rarely defends and never walls; at 10 it bridges as fast as a good player, never falls off by mistake,
 * reads who's coming for its goal, walls them off, leads its arrows and fights to knock you into space.
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
	private final Fighter.DuelGoal duel;
	private int placeCool, bowCool = 40, bowDraw = -1, decide, wallCool, stuck, mineTicks;
	private double lane;
	private BlockPos mining;
	private Vec3 lastPos = Vec3.ZERO;
	private LivingEntity bowTarget;

	BridgeGoal(Fighter f) {
		this.f = f;
		this.duel = new Fighter.DuelGoal(f);
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

	@Override
	public void tick() {
		Brain b = f.bridge;
		if (b == null) return;
		BridgeWorld w = b.world;
		if (!w.playing()) {
			f.getNavigation().stop();
			return;
		}
		int d = b.difficulty;
		ServerLevel level = w.level();
		if (placeCool > 0) placeCool--;
		if (bowCool > 0) bowCool--;
		if (wallCool > 0) wallCool--;
		f.setShiftKeyDown(false);

		// Hurt: step back and eat (from 3).
		if (f.eating()) {
			f.getNavigation().stop();
			return;
		}
		LivingEntity near = nearest(w.enemiesOf(b.team));
		double nd = near == null ? 999 : f.distanceTo(near);
		if (d >= 3 && f.getHealth() < f.getMaxHealth() * 0.4F && nd > 4) {
			f.eat();
			return;
		}

		if (--decide <= 0) {
			decide = 20;
			choose(b, near);
		}

		// Close enough to fight: fight, from the middle of the bridge so it's the other one who goes off.
		if (near != null && nd < 4.0 + 0.06 * d && Math.abs(near.getY() - f.getY()) < 2.5) {
			bowDraw = -1;
			if (f.isUsingItem()) f.stopUsingItem();
			hold(sword());
			f.setTarget(near);
			duel.tick();
			if (d >= 6) {
				double cz = w.centreZ() + 0.5, mine = Math.abs(f.getZ() - cz), theirs = Math.abs(near.getZ() - cz);
				if (mine > theirs - 0.3 && mine > 0.4) f.setDeltaMovement(f.getDeltaMovement().add(0, 0, Math.signum(cz - f.getZ()) * 0.025 * (d - 4) / 6.0));
			}
			return;
		}
		f.setTarget(null);

		// Drawing a bow: hold still, aim, loose.
		if (bowDraw >= 0) {
			if (bowTarget == null || !bowTarget.isAlive()) {
				bowDraw = -1;
				f.stopUsingItem();
			} else {
				f.getNavigation().stop();
				f.getLookControl().setLookAt(bowTarget, 30, 30);
				if (++bowDraw >= 22 - d) shoot(level, bowTarget, d);
				return;
			}
		}
		if (near != null && nd > 9 && nd < 32 && bowCool == 0 && d >= 2 && f.hasLineOfSight(near) && f.random.nextFloat() < 0.04F + 0.012F * d) {
			hold(new ItemStack(Items.BOW));
			f.startUsingItem(InteractionHand.MAIN_HAND);
			bowTarget = near;
			bowDraw = 0;
			return;
		}

		int s = side(b.team);
		Vec3 target;
		if (b.defending) {
			LivingEntity threat = threat(w, b.team);
			if (threat != null) {
				// Between them and the goal, and a wall in their way.
				double toGoal = Math.signum(s * GOAL_X + 0.5 + w.centreX() - threat.getX());
				target = new Vec3(threat.getX() + toGoal * 2.5, f.getY(), threat.getZ());
				if (d >= 4 && wallCool == 0 && threat.distanceTo(f) < 9 && f.random.nextFloat() < 0.08F * (d - 3)) wall(w, threat, toGoal, d);
			} else {
				target = new Vec3(w.centreX() + s * (BASE_FRONT + 1) + 0.5, f.getY(), w.centreZ() + 0.5 + lane * 0.5);
			}
		} else {
			// The other team's goal: once close, straight in.
			double gx = w.centreX() - s * GOAL_X + 0.5, gz = w.centreZ() + 0.5;
			target = new Vec3(gx, f.getY(), gz);
		}
		steer(w, b, target, d);
	}

	/** Attack or defend, and which way round anyone in the way. */
	private void choose(Brain b, LivingEntity near) {
		BridgeWorld w = b.world;
		int d = b.difficulty, s = side(b.team);
		LivingEntity threat = threat(w, b.team);
		double myGoal = w.centreX() + s * GOAL_X + 0.5;
		if (w.teamSize() == 1) {
			// On your own: defend when someone's nearer your goal than you are and closing in (better at it as
			// the difficulty rises), otherwise attack.
			boolean danger = threat != null && Math.abs(threat.getX() - myGoal) < 22 && Math.abs(threat.getX() - myGoal) < Math.abs(f.getX() - myGoal) + 4;
			b.defending = danger && f.random.nextFloat() < 0.15F + 0.085F * d;
		} else {
			// In a team: the first always attacks, every other one defends; anyone defends when the goal's in
			// danger and they're the nearest to it.
			b.defending = b.index % 2 == 1;
			if (threat != null && Math.abs(threat.getX() - myGoal) < 16 && d >= 3) {
				LivingEntity nearestAlly = null;
				double best = 999;
				for (LivingEntity a : w.alliesOf(b.team)) {
					double dist = Math.abs(a.getX() - myGoal);
					if (dist < best) {
						best = dist;
						nearestAlly = a;
					}
				}
				if (nearestAlly == f) b.defending = true;
			}
		}
		// Round whoever's on the bridge ahead (from 4, by bridging out to one side), or along the middle.
		boolean blocked = false;
		if (near != null) {
			double ahead = (near.getX() - f.getX()) * -s;
			blocked = ahead > 2 && ahead < 14 && Math.abs(near.getZ() - (w.centreZ() + 0.5)) < 3;
		}
		if (blocked && d >= 4 && !b.defending) lane = f.random.nextBoolean() ? 4.5 : -4.5;
		else if (Math.abs(lane) > 3 && f.random.nextFloat() < 0.3F) lane = 0;
		else if (Math.abs(lane) <= 3) lane = (f.random.nextFloat() - 0.5) * 2.5;
	}

	/** The enemy closest to this team's goal, if any is coming for it. */
	private static LivingEntity threat(BridgeWorld w, int team) {
		double goal = w.centreX() + side(team) * GOAL_X + 0.5;
		LivingEntity best = null;
		double bestD = 30;
		for (LivingEntity e : w.enemiesOf(team)) {
			double dist = Math.abs(e.getX() - goal);
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

	private boolean solid(ServerLevel level, BlockPos pos) {
		return !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
	}

	/**
	 * Walks towards the target along a lane: bridges any gap ahead (or stops at the edge), jumps a block in
	 * the way, breaks through placed walls, steps round the map's own.
	 */
	private void steer(BridgeWorld w, Brain b, Vec3 target, int d) {
		ServerLevel level = w.level();
		Vec3 p = f.position();
		boolean attacking = !b.defending;
		double toGoal = Math.hypot(target.x - p.x, target.z - p.z);
		double tz = toGoal < 7 || b.defending ? target.z : w.centreZ() + 0.5 + lane;
		double wx = p.x + Mth.clamp(target.x - p.x, -2.5, 2.5), wz = p.z + Mth.clamp(tz - p.z, -1.4, 1.4);
		double dx = wx - p.x, dz = wz - p.z, len = Math.sqrt(dx * dx + dz * dz);
		if (len < 0.15) {
			f.getNavigation().stop();
			return;
		}
		dx /= len;
		dz /= len;
		f.getLookControl().setLookAt(wx, f.getEyeY(), wz);

		// Breaking through a wall of placed blocks.
		if (mining != null) {
			hold(new ItemStack(Items.DIAMOND_PICKAXE));
			if (mineTicks % 4 == 0) f.swing(InteractionHand.MAIN_HAND);
			if (--mineTicks <= 0) {
				if (w.breakable(mining)) level.destroyBlock(mining, false, f);
				mining = null;
			}
			return;
		}

		int groundY = Mth.floor(p.y - 0.5);
		BlockPos ahead = BlockPos.containing(p.x + dx * 0.9, groundY, p.z + dz * 0.9);
		boolean intoGoal = attacking && toGoal < 4.5;
		if (f.onGround() && !intoGoal && !solid(level, ahead)) {
			// A gap: build across it (slower and with the odd slip at low levels), or wait at the edge.
			if (f.random.nextFloat() < Math.max(0, 0.05F - 0.005F * d)) {
				f.getMoveControl().setWantedPosition(wx, p.y, wz, 1.0);
				return;
			}
			f.setShiftKeyDown(true);
			f.getNavigation().stop();
			f.getMoveControl().setWantedPosition(p.x, p.y, p.z, 0);
			if (placeCool == 0 && w.buildable(ahead) && level.getBlockState(ahead).isAir()) place(w, b, ahead);
			return;
		}

		BlockPos feet = BlockPos.containing(p.x + dx * 0.8, p.y + 0.2, p.z + dz * 0.8), head = feet.above();
		boolean feetBlocked = solid(level, feet), headBlocked = solid(level, head);
		if (feetBlocked && headBlocked) {
			BlockPos breakable = w.breakable(head) ? head : w.breakable(feet) ? feet : null;
			if (breakable != null && d >= 2) {
				mining = breakable;
				mineTicks = Math.max(3, 13 - d);
				return;
			}
			lane += f.random.nextBoolean() ? 2 : -2;
			lane = Mth.clamp(lane, -5, 5);
		} else if (feetBlocked && f.onGround()) {
			f.getJumpControl().jump();
		}

		// Stuck (pushed against something): try another lane.
		if (f.tickCount % 20 == 0) {
			if (p.distanceToSqr(lastPos) < 0.05) {
				if (++stuck >= 2) {
					lane = Mth.clamp(lane + (f.random.nextBoolean() ? 2.5 : -2.5), -5, 5);
					if (f.onGround()) f.getJumpControl().jump();
					stuck = 0;
				}
			} else {
				stuck = 0;
			}
			lastPos = p;
		}
		hold(sword());
		boolean sprint = attacking && d >= 3 && Math.abs(lane) < 3;
		f.setSprinting(sprint);
		f.getMoveControl().setWantedPosition(wx, p.y, wz, sprint ? 1.25 : 1.0);
	}

	/** Places one of its team's blocks (as fast as its difficulty allows). */
	private void place(BridgeWorld w, Brain b, BlockPos pos) {
		BlockState state = w.blockFor(b.team);
		hold(new ItemStack(state.getBlock().asItem()));
		w.level().setBlock(pos, state, 3);
		w.level().playSound(null, pos, state.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 0.8F);
		f.swing(InteractionHand.MAIN_HAND);
		placeCool = Math.max(2, 14 - (int) (1.3 * b.difficulty));
	}

	/** A wall across an attacker's path, two high, between them and the goal. */
	private void wall(BridgeWorld w, LivingEntity threat, double toGoal, int d) {
		ServerLevel level = w.level();
		int x = Mth.floor(threat.getX() + toGoal * 2), y = Mth.floor(threat.getY()), z0 = Mth.floor(threat.getZ());
		int placed = 0;
		for (int dz = -1; dz <= 1; dz++) {
			for (int dy = 0; dy <= 1; dy++) {
				BlockPos pos = new BlockPos(x, y + dy, z0 + dz);
				if (level.getBlockState(pos).isAir() && w.buildable(pos) && level.getEntities(null, new net.minecraft.world.phys.AABB(pos)).isEmpty()) {
					place(w, f.bridge, pos);
					placed++;
				}
			}
		}
		wallCool = placed > 0 ? 60 - 4 * d : 10;
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
		level.playSound(null, f.getX(), f.getY(), f.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.HOSTILE, 1.0F, 1.0F / (f.random.nextFloat() * 0.4F + 1.2F) + 0.5F);
		f.stopUsingItem();
		bowDraw = -1;
		bowCool = 70;
		hold(sword());
	}
}
