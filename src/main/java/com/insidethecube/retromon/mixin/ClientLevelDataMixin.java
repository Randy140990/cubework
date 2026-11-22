package com.insidethecube.retromon.mixin;

import com.insidethecube.retromon.client.BattleScene;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** In a battle the time of day holds where it was when the battle began (the server holds the world's clock too). */
@Mixin(ClientLevel.ClientLevelData.class)
public abstract class ClientLevelDataMixin {
	@Inject(method = "getDayTime", at = @At("HEAD"), cancellable = true, require = 0)
	private void retromon$noon(CallbackInfoReturnable<Long> cir) {
		if (BattleScene.staged() && BattleScene.heldDay >= 0) cir.setReturnValue(BattleScene.heldDay);
	}

}
