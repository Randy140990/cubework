package com.insidethecube.retromon.game;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A person in the world you can talk to: a Pokémon Centre nurse, a Mart clerk, a trainer, a gym leader or one
 * of the townsfolk.
 * They stand facing one way (trainers watch along it), never come to harm and never wander off.
 */
public class Npc extends PathfinderMob {
	public static final int NURSE = 0, CLERK = 1, TRAINER = 2, LEADER = 3, PERSON = 4;
	public static final String[] ROLES = {"nurse", "clerk", "trainer", "leader", "person"};
	private static final EntityDataAccessor<Integer> ROLE = SynchedEntityData.defineId(Npc.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<String> KEY = SynchedEntityData.defineId(Npc.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<Float> FACING = SynchedEntityData.defineId(Npc.class, EntityDataSerializers.FLOAT);
	/** Walking up to someone they've spotted (server only): ticks left, or 0. */
	public int approaching;

	public Npc(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
		setInvulnerable(true);
		setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.3);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder b) {
		super.defineSynchedData(b);
		b.define(ROLE, NURSE);
		b.define(KEY, "");
		b.define(FACING, 0F);
	}

	public int role() {
		return entityData.get(ROLE);
	}

	public void setRole(int r) {
		entityData.set(ROLE, r);
	}

	/** Which trainer this is (for trainers and leaders, e.g. "route_3_0"), which person ("pallet_town_p1") or which Mart (clerks). */
	public String key() {
		return entityData.get(KEY);
	}

	public void setKey(String k) {
		entityData.set(KEY, k);
	}

	public float facing() {
		return entityData.get(FACING);
	}

	public void setFacing(float yaw) {
		entityData.set(FACING, yaw);
		face(yaw);
	}

	public void face(float yaw) {
		setYRot(yaw);
		yBodyRot = yaw;
		yHeadRot = yaw;
	}

	@Override
	public void tick() {
		super.tick();
		if (approaching <= 0 && getNavigation().isDone()) face(facing());
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		return false;
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput out) {
		super.addAdditionalSaveData(out);
		out.putInt("Role", role());
		out.putString("Key", key());
		out.putFloat("Facing", facing());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput in) {
		super.readAdditionalSaveData(in);
		setRole(in.getIntOr("Role", NURSE));
		setKey(in.getStringOr("Key", ""));
		setFacing(in.getFloatOr("Facing", 0F));
	}
}
