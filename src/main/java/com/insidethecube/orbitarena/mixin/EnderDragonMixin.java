package com.insidethecube.orbitarena.mixin;

import com.insidethecube.orbitarena.OrbitArena;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** In the arena, the Ender Dragon doesn't break the blocks it flies through. */
@Mixin(EnderDragon.class)
public class EnderDragonMixin {
	@Inject(method = "checkWalls", at = @At("HEAD"), cancellable = true)
	private void orbitarena$keepTheArena(ServerLevel level, AABB box, CallbackInfoReturnable<Boolean> cir) {
		if (level.dimension() == OrbitArena.ARENA) cir.setReturnValue(false);
	}
}
