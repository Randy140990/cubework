package com.insidethecube.deepkit.flare;

import com.insidethecube.deepkit.DK;
import com.insidethecube.deepkit.lamp.LightPools;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A thrown (or dropped) burning flare. It bounces off walls, settles on the ground and burns there, lighting
 * about 12 blocks round with sparks and smoke, then falls to ash. It never sets anything alight.
 */
public class FlareEntity extends ThrowableItemProjectile {
	private boolean landed;

	public FlareEntity(EntityType<? extends FlareEntity> type, Level level) {
		super(type, level);
	}

	public FlareEntity(Level level, LivingEntity owner, ItemStack item) {
		super(DK.FLARE, owner, level, item);
	}

	@Override
	protected Item getDefaultItem() {
		return DK.LIT_FLARES[0];
	}

	private FlareColour colour() {
		return getItem().getItem() instanceof LitFlareItem lit ? lit.colour : FlareColour.RED;
	}

	@Override
	public void tick() {
		if (landed && level().getBlockState(blockPosition().below()).isAir() && level().getBlockState(blockPosition()).isAir()) {
			// The ground went: fall again.
			landed = false;
			setNoGravity(false);
		}
		super.tick();
		if (!(level() instanceof ServerLevel server) || isRemoved()) return;
		long t = server.getGameTime();
		long until = getItem().getOrDefault(DK.BURN_UNTIL, t + FlareItem.BURN);
		if (t >= until) {
			server.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.1, getZ(), 6, 0.1, 0.05, 0.1, 0.01);
			server.sendParticles(ParticleTypes.ASH, getX(), getY() + 0.1, getZ(), 12, 0.2, 0.05, 0.2, 0.0);
			server.playSound(null, blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL, 0.6f, 1.2f);
			discard();
			return;
		}
		if (t % 2 == 0) LightPools.light(server, blockPosition(), until - t < 100 && server.getRandom().nextBoolean() ? 11 : 15);
		LitFlareItem.sparks(server, getX(), getY() + 0.12, getZ(), colour(), isInWater());
		if (t % 30 == 0) server.playSound(null, blockPosition(), SoundEvents.FIRE_AMBIENT, SoundSource.NEUTRAL, 0.7f, 1.6f);
	}

	@Override
	protected void onHitBlock(BlockHitResult hit) {
		super.onHitBlock(hit);
		Direction side = hit.getDirection();
		Vec3 v = getDeltaMovement();
		if (side == Direction.UP) {
			landed = true;
			setNoGravity(true);
			setDeltaMovement(Vec3.ZERO);
			setPos(hit.getLocation().x, hit.getLocation().y + 0.01, hit.getLocation().z);
			return;
		}
		// Bounce off walls and ceilings, losing most of its speed.
		double x = v.x * 0.4, y = v.y * 0.4, z = v.z * 0.4;
		switch (side.getAxis()) {
			case X -> x = -x;
			case Y -> y = -y;
			case Z -> z = -z;
		}
		setDeltaMovement(x, y, z);
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		// No harm done: it just drops.
		setDeltaMovement(getDeltaMovement().multiply(-0.1, 0.1, -0.1));
	}
}
