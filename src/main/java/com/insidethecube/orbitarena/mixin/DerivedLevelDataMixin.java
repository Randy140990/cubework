package com.insidethecube.orbitarena.mixin;

import com.insidethecube.orbitarena.match.ArenaDifficulty;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.storage.DerivedLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The arena dimension always fights at Normal: on Peaceful the game would delete every enemy the moment it
 * appeared and stop them hurting you. (Each dimension other than the Overworld has its own DerivedLevelData;
 * OrbitArena marks the arena's when it loads.)
 */
@Mixin(DerivedLevelData.class)
public class DerivedLevelDataMixin implements ArenaDifficulty {
	@Unique
	private boolean orbitarena$arena;

	@Override
	public void orbitarena$markArena() {
		this.orbitarena$arena = true;
	}

	@Inject(method = "getDifficulty", at = @At("HEAD"), cancellable = true)
	private void orbitarena$normalInArena(CallbackInfoReturnable<Difficulty> cir) {
		if (this.orbitarena$arena) cir.setReturnValue(Difficulty.NORMAL);
	}
}
