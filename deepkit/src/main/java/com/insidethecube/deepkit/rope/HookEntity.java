package com.insidethecube.deepkit.rope;

import com.insidethecube.deepkit.DK;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A fired grappling hook. If it hits a block it looks for the top edge (the block itself, or up to three
 * blocks above where it struck a wall) and, if there's a clear drop beside it, hooks on and hangs a rope
 * from the edge down to the shooter (at most 24 blocks). Glass, leaves, ceilings and open air are a miss.
 */
public class HookEntity extends ThrowableItemProjectile {
	private Vec3 start;

	public HookEntity(EntityType<? extends HookEntity> type, Level level) {
		super(type, level);
	}

	public HookEntity(Level level, LivingEntity owner, ItemStack item) {
		super(DK.HOOK, owner, level, item);
		start = owner.getEyePosition();
	}

	@Override
	protected Item getDefaultItem() {
		return DK.GRAPPLING_HOOK;
	}

	@Override
	protected double getDefaultGravity() {
		return 0.02;
	}

	@Override
	public void tick() {
		super.tick();
		if (level() instanceof ServerLevel server && !isRemoved() && (start == null || position().distanceTo(start) > Ropes.HOOK_REACH + 2)) miss(server);
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		if (level() instanceof ServerLevel server) miss(server);
	}

	@Override
	protected void onHitBlock(BlockHitResult hit) {
		super.onHitBlock(hit);
		if (!(level() instanceof ServerLevel server) || isRemoved()) return;
		BlockPos at = hit.getBlockPos();
		Direction side = hit.getDirection();
		BlockState s = server.getBlockState(at);
		if (side == Direction.DOWN || s.getBlock() instanceof TransparentBlock || s.is(BlockTags.LEAVES)) {
			miss(server);
			return;
		}
		Entity owner = getOwner();
		BlockPos edge = null;
		Direction out;
		if (side == Direction.UP) {
			// Landed on top: hook over the side facing the shooter.
			edge = at;
			out = owner == null ? Direction.NORTH : Direction.getApproximateNearest(owner.getX() - (at.getX() + 0.5), 0, owner.getZ() - (at.getZ() + 0.5));
			if (out.getAxis().isVertical()) out = Direction.NORTH;
		} else {
			// Struck a wall: find its top edge, up to three blocks higher.
			out = side;
			for (int dy = 0; dy <= 3; dy++) {
				BlockPos p = at.above(dy);
				BlockState here = server.getBlockState(p), above = server.getBlockState(p.above());
				if (!here.isFaceSturdy(server, p, Direction.UP)) break;
				if (!above.isFaceSturdy(server, p.above(), Direction.DOWN) && above.getCollisionShape(server, p.above()).isEmpty()) {
					edge = p;
					break;
				}
			}
		}
		if (edge == null) {
			miss(server);
			return;
		}
		int bottom = owner == null ? edge.getY() - Ropes.HOOK_REACH : (int) Math.floor(owner.getY());
		int n = Ropes.hang(server, edge.relative(out), out.getOpposite(), Ropes.HOOK_REACH, true, true, Math.min(bottom, edge.getY()));
		if (n == 0) {
			miss(server);
			return;
		}
		server.playSound(null, edge, SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1f, 0.9f);
		discard();
	}

	private void miss(ServerLevel server) {
		server.playSound(null, blockPosition(), SoundEvents.CHAIN_FALL, SoundSource.PLAYERS, 0.6f, 1.2f);
		discard();
	}
}
