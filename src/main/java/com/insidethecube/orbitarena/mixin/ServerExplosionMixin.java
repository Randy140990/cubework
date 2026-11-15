package com.insidethecube.orbitarena.mixin;

import java.util.List;

import com.insidethecube.orbitarena.OrbitArena;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ServerExplosion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** In the arena, explosions (the Wither's skulls, its birth, a creeper) don't break blocks or start fires. */
@Mixin(ServerExplosion.class)
public class ServerExplosionMixin {
	@Shadow
	@Final
	private ServerLevel level;

	@Inject(method = "interactsWithBlocks", at = @At("HEAD"), cancellable = true)
	private void orbitarena$keepTheArena(CallbackInfoReturnable<Boolean> cir) {
		if (level.dimension() == OrbitArena.ARENA) cir.setReturnValue(false);
	}

	@Inject(method = "createFire", at = @At("HEAD"), cancellable = true)
	private void orbitarena$noFire(List<BlockPos> list, CallbackInfo ci) {
		if (level.dimension() == OrbitArena.ARENA) ci.cancel();
	}
}
