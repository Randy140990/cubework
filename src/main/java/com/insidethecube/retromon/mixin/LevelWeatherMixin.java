package com.insidethecube.retromon.mixin;

import com.insidethecube.retromon.client.BattleScene;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** And clear skies there (no rain or storm on the stage). */
@Mixin(Level.class)
public abstract class LevelWeatherMixin {
	@Inject(method = "getRainLevel", at = @At("HEAD"), cancellable = true)
	private void retromon$noRain(float partial, CallbackInfoReturnable<Float> cir) {
		if ((Object) this instanceof ClientLevel && BattleScene.staged()) cir.setReturnValue(0F);
	}

	@Inject(method = "getThunderLevel", at = @At("HEAD"), cancellable = true)
	private void retromon$noThunder(float partial, CallbackInfoReturnable<Float> cir) {
		if ((Object) this instanceof ClientLevel && BattleScene.staged()) cir.setReturnValue(0F);
	}
}
