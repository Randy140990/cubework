package com.insidethecube.orbitarena.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

/**
 * A fan in the stands. Fans only exist on your screen (the crowd adds them when you're in the arena), so
 * a packed stadium costs the server nothing: they don't think, move or collide, they just stand, cheer
 * and hold up signs. Which fan it is (its look and sign) comes from its seat.
 */
public class Fan extends Mob {
	/** Which of the fan textures, whether it holds a sign, and its own beat for cheering. */
	public int look, sign = -1;
	public float phase, height = 1;
	/** Where its seat is round the stadium, in degrees (for the Mexican wave). */
	public float seatAngle;
	/** Watching through a spyglass (it holds one). */
	public boolean spyglass;

	public Fan(EntityType<? extends Mob> type, Level level) {
		super(type, level);
		this.noPhysics = true;
		this.setNoGravity(true);
		this.setSilent(true);
		this.setInvulnerable(true);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes();
	}

	/** Nothing to do but keep count (for the animation). */
	@Override
	public void tick() {
		this.tickCount++;
		this.xo = getX();
		this.yo = getY();
		this.zo = getZ();
		this.yRotO = getYRot();
		this.yBodyRotO = this.yBodyRot;
		this.yHeadRotO = this.yHeadRot;
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double distance) {
		return distance < 200 * 200;
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
