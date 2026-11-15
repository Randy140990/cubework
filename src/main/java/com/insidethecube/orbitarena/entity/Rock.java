package com.insidethecube.orbitarena.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

/**
 * A rock orbiting round The Bridge, tumbling as it goes. Like the crystals, rocks only exist on your screen
 * (client.Rocks adds them and moves them round): which rock it is (its shape) and how big.
 */
public class Rock extends Mob {
	public long seed;
	public int size;
	public float spinX, spinY;

	public Rock(EntityType<? extends Mob> type, Level level) {
		super(type, level);
		this.noPhysics = true;
		this.setNoGravity(true);
		this.setSilent(true);
		this.setInvulnerable(true);
		this.noCulling = true;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes();
	}

	@Override
	public void tick() {
		this.tickCount++;
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double distance) {
		return distance < 400 * 400;
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		return false;
	}
}
