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
 * A Pokémon standing in the world: a one-off encounter (Snorlax on the road, the legendary birds), a gift
 * waiting for you, or one of the Pokémon the games have in houses. Walk up and press A.
 */
public class WorldMon extends PathfinderMob {
	private static final EntityDataAccessor<String> SPECIES = SynchedEntityData.defineId(WorldMon.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<String> KEY = SynchedEntityData.defineId(WorldMon.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<Float> FACING = SynchedEntityData.defineId(WorldMon.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Boolean> AIRBORNE = SynchedEntityData.defineId(WorldMon.class, EntityDataSerializers.BOOLEAN);
	/** Its model on your screen (a client object), and how high it's flying there (eased). */
	public Object model;
	public float liftNow, liftOld;
	/** Server: when a wild one appeared (ticks), and where it's headed next. */
	public int born, nextMove;

	public WorldMon(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
		setInvulnerable(true);
		setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder b) {
		super.defineSynchedData(b);
		b.define(SPECIES, "pidgey");
		b.define(KEY, "");
		b.define(FACING, 0F);
		b.define(AIRBORNE, false);
	}

	/** Out in the grass (a wild one you can see) or walking with you (your Pokémon): these move about. */
	public boolean roams() {
		String k = key();
		return k.startsWith("wild:") || k.startsWith("follow:");
	}

	public boolean wild() {
		return key().startsWith("wild:");
	}

	/** In the air (a bird flying over the grass). */
	public boolean airborne() {
		return entityData.get(AIRBORNE);
	}

	public void setAirborne(boolean on) {
		entityData.set(AIRBORNE, on);
	}

	/** How fast it moves when it does (wild ones amble, yours keeps up with you). */
	public void setSpeed(double speed) {
		var a = getAttribute(Attributes.MOVEMENT_SPEED);
		if (a != null) a.setBaseValue(speed);
	}

	public String species() {
		return entityData.get(SPECIES);
	}

	public void setSpecies(String s) {
		entityData.set(SPECIES, s);
	}

	/** The one-off encounter or gift (statics.tsv id), or the person it stands in for ("pet:" + key). */
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
		setYRot(yaw);
		yBodyRot = yaw;
		yHeadRot = yaw;
	}

	@Override
	public void tick() {
		super.tick();
		// (15:21) An item ball inside, put in twice (one left from the save): the second goes, so one you take is gone.
		if (!level().isClientSide() && tickCount % 20 == 1 && key().startsWith("item:") && level().dimension() == Interiors.LEVEL && Game.twin(this, key())) {
			discard();
			return;
		}
		if (roams()) {
			// One the game doesn't know any more (left over in a save): gone.
			if (!level().isClientSide() && !Game.roamerKnown(this)) discard();
			return;
		}
		setYRot(facing());
		yBodyRot = facing();
		yHeadRot = facing();
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
		out.putString("Species", species());
		out.putString("Key", key());
		out.putFloat("Facing", facing());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput in) {
		super.readAdditionalSaveData(in);
		setSpecies(in.getStringOr("Species", "pidgey"));
		setKey(in.getStringOr("Key", ""));
		setFacing(in.getFloatOr("Facing", 0F));
	}
}
