package com.insidethecube.kantojohto.game;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A Pokémon standing on the field in a battle. It only exists on your screen (the battle adds it and takes
 * it away), so it never touches the world: it just stands, idles, lunges, flinches and faints.
 */
public class MonDisplay extends Entity {
	public String species = "pidgey";
	/** Which way it faces (degrees, as entity yaw). */
	public float facing;
	/** Size, 1 normally: grows in when sent out, shrinks away when it faints or goes into a ball. */
	public float size = 1, sizeTarget = 1, sizeOld = 1;
	/** Ticks left of a lunge (attacking) and of flashing (hit). */
	public int lunge, flash;
	public static final int LUNGE = 10;
	/** Its model on your screen (a client object, kept here so each Pokémon poses its own). */
	public Object model;

	public MonDisplay(EntityType<?> type, Level level) {
		super(type, level);
		this.noPhysics = true;
		setNoGravity(true);
		setSilent(true);
		setInvulnerable(true);
	}

	@Override
	public void tick() {
		// (The level counts its ticks and keeps where it was.)
		sizeOld = size;
		size += (sizeTarget - size) * 0.25F;
		if (Math.abs(sizeTarget - size) < 0.01F) size = sizeTarget;
		if (lunge > 0) lunge--;
		if (flash > 0) flash--;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder b) {
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		return false;
	}

	@Override
	protected void readAdditionalSaveData(ValueInput in) {
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput out) {
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double d) {
		return d < 128 * 128;
	}
}
