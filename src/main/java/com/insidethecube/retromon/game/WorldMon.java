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
	/** (0.61) A room's POKéMON that wanders about its pen (the DAY CARE's): drawn walking when it moves. */
	private static final EntityDataAccessor<Boolean> WANDERS = SynchedEntityData.defineId(WorldMon.class, EntityDataSerializers.BOOLEAN);
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
		b.define(WANDERS, false);
	}

	/** Out in the grass (a wild one you can see) or walking with you (your Pokémon): these move about. */
	public boolean roams() {
		String k = key();
		return k.startsWith("wild:") || k.startsWith("follow:");
	}

	/** (0.50.3) Asleep out in the world, lying down: Routes 12 and 16's SNORLAX (statics.tsv), till it's woken. */
	public boolean asleep() {
		return key().startsWith("snorlax_");
	}

	/** (0.59.4) Part of a place's scenery (Fuchsia's ponds): moves about by itself, never battled. */
	public boolean scenery() {
		return key().startsWith("scene:");
	}

	public boolean wanders() {
		return entityData.get(WANDERS);
	}

	public void setWanders(boolean on) {
		entityData.set(WANDERS, on);
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

	/**
	 * (0.74) John's 28 Nov 10:41: "their meow equivalent is not there, which they should be, remember, walking around".
	 * One that goes about with someone (TEAM ROCKET's MEOWTH, by JESSIE): ambling round near them, along with them when
	 * they walk off, and gone with them.
	 */
	public net.minecraft.world.entity.Entity follow;
	private double aimX = Double.NaN, aimZ;
	private int restFor;

	private void followTick() {
		if (follow == null) {
			// (Left over in a save, with no one to go about with: gone.)
			if (tickCount > 40) discard();
			return;
		}
		if (follow.isRemoved() || follow.level() != level()) {
			discard();
			return;
		}
		setNoGravity(true);
		noPhysics = true;
		setWanders(true);
		boolean moving = follow.getDeltaMovement().horizontalDistanceSqr() > 1e-4 || Math.hypot(follow.getX() - follow.xo, follow.getZ() - follow.zo) > 0.01;
		double near = Math.hypot(aimX - follow.getX(), aimZ - follow.getZ());
		if (Double.isNaN(aimX) || moving || near > 3.2) {
			// Keeping up: just behind and beside them.
			double a = Math.toRadians(follow.getYRot());
			aimX = follow.getX() + Math.sin(a) * 1.1 + Math.cos(a) * 0.9;
			aimZ = follow.getZ() - Math.cos(a) * 1.1 + Math.sin(a) * 0.9;
			restFor = 0;
		}
		double dx = aimX - getX(), dz = aimZ - getZ(), dist = Math.hypot(dx, dz);
		if (dist < 0.08) {
			if (--restFor > 0) return;
			// Somewhere else round them, now and then.
			double ang = random.nextDouble() * Math.PI * 2, r = 1.3 + random.nextDouble() * 1.4;
			aimX = follow.getX() + Math.cos(ang) * r;
			aimZ = follow.getZ() + Math.sin(ang) * r;
			restFor = 40 + random.nextInt(90);
			return;
		}
		double step = Math.min(dist, moving ? 0.16 : 0.05);
		setFacing((float) Math.toDegrees(Math.atan2(-dx, dz)));
		setPos(getX() + dx / dist * step, follow.getY(), getZ() + dz / dist * step);
	}

	@Override
	public void tick() {
		super.tick();
		if (!level().isClientSide() && key().startsWith("rocket_meowth:")) followTick();
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
