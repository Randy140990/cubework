package com.insidethecube.orbitarena.entity;

import java.util.EnumSet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * An arena fighter: an armoured opponent that fights like a player. It's built on the zombie (for its
 * armour, held items and spear poses) but none of the zombie is left in how it behaves: no groans, no
 * burning, no reinforcements, no loot, no fall damage. How well it fights is its difficulty, 1 to 10:
 * <ul>
 * <li>reaction time (12 ticks at 1, 2 at 10) and missed swings (often at 1, never from 8);</li>
 * <li>strafing round you from 3, switching direction more often as it rises;</li>
 * <li>jump critical hits from 4, sprint-reset knockback from 6;</li>
 * <li>raising its shield when you're about to hit, from 3, quicker as it rises;</li>
 * <li>swapping to an axe to break your shield, from 5;</li>
 * <li>mace leaps with a mace; longer reach with a spear;</li>
 * <li>backing off to eat golden apples when hurt, from 4.</li>
 * </ul>
 */
public class Fighter extends Zombie {
	private static final Identifier CRIT_ID = Identifier.fromNamespaceAndPath("orbitarena", "crit");

	private int difficulty = 5;
	private int apples;
	/** An axe to swap to against a shield (kept aside), and the main weapon while it's swapped out. */
	private ItemStack spareAxe = ItemStack.EMPTY, stowed = ItemStack.EMPTY;
	private int eating = -1;
	private ItemStack beforeEating = ItemStack.EMPTY;
	/** A look (0 to 7) for the texture. */
	private int look;
	/** On the bridge: its part in its team's game (null in the stadium), and its sword between blocks and bows. */
	public BridgeGoal.Brain bridge;
	ItemStack bridgeSword = ItemStack.EMPTY;

	public Fighter(EntityType<? extends Zombie> type, Level level) {
		super(type, level);
		this.setCanPickUpLoot(false);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Zombie.createAttributes()
				.add(Attributes.FOLLOW_RANGE, 96.0)
				.add(Attributes.MOVEMENT_SPEED, 0.28)
				.add(Attributes.ATTACK_DAMAGE, 1.0)
				.add(Attributes.ARMOR, 0.0)
				.add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0.0);
	}

	public void setup(int difficulty, int apples, ItemStack spareAxe, int look) {
		this.difficulty = Mth.clamp(difficulty, 1, 10);
		this.apples = apples;
		this.spareAxe = spareAxe;
		this.look = look;
		AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null) speed.setBaseValue(0.22 + 0.012 * this.difficulty);
	}

	/** Sets it up to play The Bridge for a team. */
	public void setupBridge(BridgeGoal.Brain brain, ItemStack sword, int apples, int look) {
		setup(brain.difficulty, apples, ItemStack.EMPTY, look);
		this.bridge = brain;
		this.bridgeSword = sword;
		setItemSlot(EquipmentSlot.MAINHAND, sword);
	}

	public int difficulty() {
		return difficulty;
	}

	public int look() {
		return look;
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new BridgeGoal(this));
		this.goalSelector.addGoal(1, new DuelGoal(this));
	}

	@Override
	protected void addBehaviourGoals() {
	}

	@Override
	protected boolean isSunSensitive() {
		return false;
	}

	@Override
	protected boolean convertsInWater() {
		return false;
	}

	@Override
	public boolean isBaby() {
		return false;
	}

	@Override
	public void setBaby(boolean baby) {
	}

	@Override
	public boolean canHoldItem(ItemStack stack) {
		return false;
	}

	@Override
	public boolean wantsToPickUp(ServerLevel level, ItemStack stack) {
		return false;
	}

	@Override
	protected int getBaseExperienceReward(ServerLevel level) {
		return 0;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return null;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.PLAYER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.PLAYER_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		this.playSound(state.getSoundType().getStepSound(), 0.15F, 1.0F);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (source.is(DamageTypeTags.IS_FALL)) return false;
		return super.hurtServer(level, source, amount);
	}

	/** A hit while falling is a critical hit (half again as hard, with sparks), as a player's is. */
	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		boolean crit = !onGround() && getDeltaMovement().y < 0 && difficulty >= 4;
		AttributeInstance damage = getAttribute(Attributes.ATTACK_DAMAGE);
		if (crit && damage != null) damage.addOrReplacePermanentModifier(new AttributeModifier(CRIT_ID, 0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		boolean hit = super.doHurtTarget(level, target);
		if (damage != null) damage.removeModifier(CRIT_ID);
		if (hit && crit) {
			level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(0.5), target.getZ(), 12, 0.3, 0.4, 0.3, 0.2);
			level.playSound(null, getX(), getY(), getZ(), SoundEvents.PLAYER_ATTACK_CRIT, getSoundSource(), 1.0F, 1.0F);
		}
		return hit;
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (level().isClientSide() || eating < 0) return;
		eating--;
		if (eating % 4 == 0) playSound(SoundEvents.GENERIC_EAT.value(), 0.6F, 0.9F + random.nextFloat() * 0.2F);
		if (eating == 0) {
			playSound(SoundEvents.PLAYER_BURP, 0.6F, 1.0F);
			addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
			addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 2400, 0));
			setItemSlot(EquipmentSlot.MAINHAND, beforeEating);
			eating = -1;
		}
	}

	boolean eating() {
		return eating >= 0;
	}

	void eat() {
		if (apples <= 0 || eating >= 0) return;
		apples--;
		stopUsingItem();
		beforeEating = getMainHandItem();
		setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_APPLE));
		eating = 32;
	}

	/** Swaps to the spare axe (against a raised shield) or back. */
	void axe(boolean on) {
		if (on && !spareAxe.isEmpty() && stowed.isEmpty() && !(getMainHandItem().getItem() instanceof AxeItem)) {
			stowed = getMainHandItem();
			setItemSlot(EquipmentSlot.MAINHAND, spareAxe);
		} else if (!on && !stowed.isEmpty()) {
			setItemSlot(EquipmentSlot.MAINHAND, stowed);
			stowed = ItemStack.EMPTY;
		}
	}

	/** Ticks between swings with what's in hand (a player's full-strength swing). */
	int swingTicks() {
		ItemStack s = getMainHandItem();
		if (s.isEmpty()) return 6;
		if (s.is(Items.MACE)) return 34;
		if (s.getItem() instanceof AxeItem) return 20;
		if (s.is(net.minecraft.tags.ItemTags.SPEARS)) return 18;
		return 13;
	}

	double reach() {
		return (getMainHandItem().is(net.minecraft.tags.ItemTags.SPEARS) ? 4.2 : 3.1) + 0.02 * difficulty;
	}

	boolean hasShield() {
		return getOffhandItem().is(Items.SHIELD);
	}

	/** Fights the target like a player would, as well as its difficulty allows. */
	static final class DuelGoal extends Goal {
		private final Fighter f;
		private int swing, react, strafeTicks, backOff, maceLeap;
		private float side = 1;
		private boolean jumpForCrit, sprintHit;

		DuelGoal(Fighter f) {
			this.f = f;
			this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
		}

		@Override
		public boolean canUse() {
			LivingEntity t = f.getTarget();
			return t != null && t.isAlive() && f.bridge == null;
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void stop() {
			f.stopUsingItem();
			f.getNavigation().stop();
		}

		@Override
		public void tick() {
			LivingEntity t = f.getTarget();
			if (t == null) return;
			int d = f.difficulty;
			ServerLevel level = (ServerLevel) f.level();
			double dist = f.distanceTo(t);
			f.getLookControl().setLookAt(t, 40, 40);
			float face = (float) (Mth.atan2(t.getZ() - f.getZ(), t.getX() - f.getX()) * Mth.RAD_TO_DEG) - 90;
			if (swing > 0) swing--;
			if (react > 0) react--;

			// Hurt: back off and eat.
			if (f.eating()) {
				f.getNavigation().stop();
				f.setYRot(face);
				f.yBodyRot = face;
				f.getMoveControl().strafe(-0.8F, side * 0.6F);
				return;
			}
			if (d >= 4 && f.apples > 0 && f.getHealth() < f.getMaxHealth() * 0.35F && dist > 2.0) {
				f.eat();
				return;
			}

			boolean blocking = t instanceof Player p && p.isBlocking();
			// Against a raised shield, an axe (from 5).
			f.axe(d >= 5 && blocking);

			// Raise its own shield when you're close and ready to swing, if it can't hit first.
			if (f.hasShield() && d >= 3) {
				boolean threat = dist < 3.6 && t instanceof Player p && p.getAttackStrengthScale(0) > 0.85F;
				if (threat && swing > 3 && react == 0 && f.random.nextFloat() < 0.15F + 0.08F * d) {
					f.startUsingItem(InteractionHand.OFF_HAND);
				} else if (!threat || swing <= 1) {
					if (f.isUsingItem() && f.getUsedItemHand() == InteractionHand.OFF_HAND) {
						f.stopUsingItem();
						react = Math.max(1, 12 - d);
					}
				}
			}

			double reach = f.reach();
			boolean mace = f.getMainHandItem().is(Items.MACE);
			if (dist > reach - 0.3 && maceLeap == 0) {
				// Close in, sprinting.
				f.getNavigation().moveTo(t, 1.0 + 0.04 * d);
				sprintHit = d >= 6;
				// A mace leap from a few blocks out: up, then down onto you.
				if (mace && f.onGround() && dist < 6 && dist > 2 && swing == 0 && d >= 3 && f.random.nextFloat() < 0.08F) {
					Vec3 to = t.position().subtract(f.position()).normalize();
					f.setDeltaMovement(to.x * 0.35, 0.95, to.z * 0.35);
					maceLeap = 30;
				}
			} else {
				f.getNavigation().stop();
				f.setYRot(face);
				f.yBodyRot = face;
				if (d >= 3) {
					if (--strafeTicks <= 0) {
						side = f.random.nextBoolean() ? 1 : -1;
						strafeTicks = 10 + f.random.nextInt(Math.max(4, 50 - 4 * d));
					}
					float forward = dist > 2.8 ? 0.4F : dist < 2.0 ? -0.5F : 0;
					if (backOff > 0) {
						forward = -0.7F;
						backOff--;
					}
					f.getMoveControl().strafe(forward, side * (0.25F + 0.05F * d));
				}
				// A jump to land a critical hit (from 4).
				if (d >= 4 && swing <= 4 && f.onGround() && !jumpForCrit && f.random.nextFloat() < 0.05F * (d - 3)) {
					f.getJumpControl().jump();
					jumpForCrit = true;
				}
			}
			if (maceLeap > 0) maceLeap--;
			if (f.onGround() && maceLeap < 25) maceLeap = 0;

			// Swing when ready and in reach (waiting for the drop when jumping for a crit).
			boolean falling = !f.onGround() && f.getDeltaMovement().y < 0;
			boolean waitForDrop = (jumpForCrit || maceLeap > 0) && !falling && !f.onGround();
			if (swing == 0 && dist <= reach && !waitForDrop && f.hasLineOfSight(t)) {
				if (f.isUsingItem()) f.stopUsingItem();
				f.swing(InteractionHand.MAIN_HAND);
				// Low difficulties miss now and then.
				if (f.random.nextFloat() >= Math.max(0, 0.35F - 0.05F * d)) {
					boolean hit = f.doHurtTarget(level, t);
					if (hit && sprintHit) {
						// A sprint hit knocks you back further; then it resets (backs off a moment).
						t.knockback(0.45, f.getX() - t.getX(), f.getZ() - t.getZ());
						backOff = 3 + f.random.nextInt(3);
					}
				}
				swing = f.swingTicks() + f.random.nextInt(Math.max(1, (11 - d) * 2));
				react = Math.max(1, 12 - d);
				jumpForCrit = false;
				maceLeap = 0;
			}
		}
	}

	/** Gets rid of anything it drops (it shouldn't drop anything, but just in case). */
	public static void clearDrops(ServerLevel level, net.minecraft.world.phys.AABB box) {
		for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, box)) e.discard();
	}
}
