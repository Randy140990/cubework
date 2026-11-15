package com.insidethecube.orbitarena.mixin;

import com.insidethecube.orbitarena.OrbitArena;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** In the arena, the Wither doesn't break the blocks round it when it's hit (it was tearing up the floor). */
@Mixin(WitherBoss.class)
public class WitherBossMixin {
	@Shadow
	private int destroyBlocksTick;

	@Inject(method = "customServerAiStep", at = @At("HEAD"))
	private void orbitarena$keepTheFloor(ServerLevel level, CallbackInfo ci) {
		if (level.dimension() == OrbitArena.ARENA) destroyBlocksTick = 0;
	}
}
