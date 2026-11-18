package com.insidethecube.retromon.game;

import net.minecraft.core.BlockPos;
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
 * Trainers pace a short beat, watching the way they face; townsfolk who walk about in the games stroll a few
 * steps round their spot, and the rest look about now and then. Whoever you talk to stops and turns to you.
 * They never come to harm and never wander off.
 */
public class Npc extends PathfinderMob {
	public static final int NURSE = 0, CLERK = 1, TRAINER = 2, LEADER = 3, PERSON = 4;
	public static final String[] ROLES = {"nurse", "clerk", "trainer", "leader", "person"};
	private static final EntityDataAccessor<Integer> ROLE = SynchedEntityData.defineId(Npc.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<String> KEY = SynchedEntityData.defineId(Npc.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<Float> FACING = SynchedEntityData.defineId(Npc.class, EntityDataSerializers.FLOAT);
	/** Walking up to someone they've spotted (server only): ticks left, or 0. */
	public int approaching;
	/** On the battle stage (your screen only): cheering a win, ticks left (hops for joy). */
	public int cheer;

	public Npc(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
		setInvulnerable(true);
		setPersistenceRequired();
		// They keep out of the water (no one wading into a pond on a stroll).
		setPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.WATER, -1F);
		setPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.WATER_BORDER, 8F);
	}

	/** Moves them somewhere new and makes it their spot (settling one who stood somewhere they shouldn't). */
	public void rehome(BlockPos at) {
		setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
		home = at;
		getNavigation().stop();
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

	/**
	 * Where they stand and the way they watch (saved, so a trainer who walked up to you goes back to their spot);
	 * townsfolk who walk about in the games stroll a few steps round it, trainers pace a short beat across it.
	 */
	private BlockPos home;
	private float homeFacing;
	private int nextWander, beat = 1;
	private boolean looked, walking;
	/** The way to face on arriving (NaN: the way they walked). */
	private float faceAfter = Float.NaN;
	private Trainers.Person person;
	private Trainers.Trainer trainer;
	/** Checked once after loading: standing somewhere they shouldn't (a roof, water) gets them moved. */
	private boolean settled;
	/** Who they're talking to or battling (server only): they stop and face them until it's over. */
	public net.minecraft.server.level.ServerPlayer talker;

	@Override
	public void tick() {
		super.tick();
		if (!level().isClientSide()) serverTick();
		else if (cheer > 0) cheer--;
		// FACING is the way they face right now (walking, too), so everyone sees them turn as they go.
		face(facing());
	}

	private void serverTick() {
		// Your rival, left over in a save from when he was about: gone.
		if (key().startsWith("rival") && !Game.rivalKnown(this)) {
			discard();
			return;
		}
		if (!settled) {
			settled = true;
			Game.settle(this);
		}
		if (home == null) {
			home = blockPosition();
			homeFacing = facing();
		}
		if (talker != null && (talker.isRemoved() || talker.distanceTo(this) > 10 || !Game.engaged(talker))) {
			talker = null;
			if (role() == TRAINER || role() == LEADER) entityData.set(FACING, homeFacing);
		}
		if (talker != null) {
			getNavigation().stop();
			walking = false;
			entityData.set(FACING, yawTo(talker));
			return;
		}
		if (!getNavigation().isDone()) {
			entityData.set(FACING, getYRot());
			return;
		}
		if (approaching > 0) return;
		if (walking) {
			walking = false;
			if (!Float.isNaN(faceAfter)) entityData.set(FACING, faceAfter);
			faceAfter = Float.NaN;
		}
		if (role() == PERSON) wander();
		else if (role() == TRAINER) patrol();
		else if (tickCount >= nextWander && blockPosition().distManhattan(home) > 1) {
			nextWander = tickCount + 60;
			walkTo(home, homeFacing);
		}
	}

	/** The yaw that faces another entity. */
	public float yawTo(net.minecraft.world.entity.Entity e) {
		return (float) Math.toDegrees(Math.atan2(-(e.getX() - getX()), e.getZ() - getZ()));
	}

	private void wander() {
		if (!looked) {
			person = Trainers.PEOPLE.get(key());
			looked = true;
		}
		if (person == null || tickCount < nextWander) return;
		nextWander = tickCount + 70 + random.nextInt(130);
		String facing = person.facing();
		if (person.movement().equals("walk")) {
			boolean upDown = facing.equals("up_down"), leftRight = facing.equals("left_right");
			int dx = upDown ? 0 : random.nextInt(7) - 3, dz = leftRight ? 0 : random.nextInt(7) - 3;
			if (dx == 0 && dz == 0) return;
			walkTo(home.offset(dx, 0, dz), Float.NaN);
		} else if (facing.equals("any_dir") || facing.equals("none")) {
			// Stands still, but looks about now and then.
			if (random.nextInt(3) == 0) setFacing(random.nextInt(4) * 90F);
		}
	}

	/**
	 * Trainers who watch for you pace a short beat across the way they look (two or three steps each side of
	 * their spot), facing their way again at each end, and glance aside now and then; they see along the way
	 * they're facing, so a pacing one can catch you anywhere along the beat. The games' spinners turn about.
	 */
	private void patrol() {
		if (!looked) {
			trainer = Trainers.get(key());
			looked = true;
		}
		if (trainer == null || trainer.sight() <= 0 || tickCount < nextWander) return;
		String f = trainer.facing();
		if (f.equals("none")) {
			nextWander = tickCount + 24 + random.nextInt(40);
			setFacing(random.nextInt(4) * 90F);
			return;
		}
		boolean along = f.equals("left_right") || f.equals("up_down");
		boolean alongX = f.equals("left_right") || f.equals("up") || f.equals("down");
		if (!along && Math.abs(net.minecraft.util.Mth.wrapDegrees(facing() - homeFacing)) > 1) {
			// Glanced aside: watching their way again.
			setFacing(homeFacing);
			nextWander = tickCount + 40 + random.nextInt(60);
			return;
		}
		nextWander = tickCount + 60 + random.nextInt(90);
		BlockPos at = blockPosition();
		if (Math.abs(at.getX() - home.getX()) + Math.abs(at.getZ() - home.getZ()) > 1) {
			walkTo(home, along ? Float.NaN : homeFacing);
			return;
		}
		if (!along && random.nextInt(10) < 3) {
			setFacing(homeFacing + (random.nextBoolean() ? 90F : -90F));
			nextWander = tickCount + 30 + random.nextInt(30);
			return;
		}
		beat = -beat;
		int steps = 2 + random.nextInt(2);
		walkTo(alongX ? home.offset(beat * steps, 0, 0) : home.offset(0, 0, beat * steps), along ? Float.NaN : homeFacing);
	}

	private void walkTo(BlockPos to, float thenFace) {
		walking = getNavigation().moveTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5, 0.55);
		faceAfter = thenFace;
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
		if (home != null) {
			out.putInt("HomeX", home.getX());
			out.putInt("HomeY", home.getY());
			out.putInt("HomeZ", home.getZ());
			out.putFloat("HomeFacing", homeFacing);
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput in) {
		super.readAdditionalSaveData(in);
		setRole(in.getIntOr("Role", NURSE));
		setKey(in.getStringOr("Key", ""));
		setFacing(in.getFloatOr("Facing", 0F));
		int hy = in.getIntOr("HomeY", Integer.MIN_VALUE);
		if (hy != Integer.MIN_VALUE) {
			home = new BlockPos(in.getIntOr("HomeX", 0), hy, in.getIntOr("HomeZ", 0));
			homeFacing = in.getFloatOr("HomeFacing", facing());
		}
	}
}
