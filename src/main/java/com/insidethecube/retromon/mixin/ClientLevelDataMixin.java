package com.insidethecube.retromon.mixin;

import com.insidethecube.retromon.client.BattleScene;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** On a battle's stage it's always bright midday (on your screen), whatever the time in the world. */
@Mixin(ClientLevel.ClientLevelData.class)
public abstract class ClientLevelDataMixin {
	@Inject(method = "getDayTime", at = @At("HEAD"), cancellable = true)
	private void retromon$noon(CallbackInfoReturnable<Long> cir) {
		if (BattleScene.staged()) cir.setReturnValue(6000L);
	}
}
