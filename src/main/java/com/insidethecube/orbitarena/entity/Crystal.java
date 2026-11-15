package com.insidethecube.orbitarena.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

/**
 * One of The Bridge's crystals, floating over a pillar: a team's (green for blue, light red for red) and
 * which pillar. Like the fans, they only exist on your screen (client.Crystals adds them), turning slowly,
 * until their pillar crumbles and they shatter.
 */
public class Crystal extends Mob {
	public int team, pillar;
	public float phase;

	public Crystal(EntityType<? extends Mob> type, Level level) {
		super(type, level);
		this.noPhysics = true;
		this.setNoGravity(true);
		this.setSilent(true);
		this.setInvulnerable(true);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes();
	}

	@Override
	public void tick() {
		this.tickCount++;
		this.xo = getX();
		this.yo = getY();
		this.zo = getZ();
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double distance) {
		return distance < 300 * 300;
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
