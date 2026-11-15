package com.insidethecube.orbitarena.mixin;

import com.insidethecube.orbitarena.client.Cinematic;
import net.minecraft.client.renderer.ItemInHandRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** No hand (or what's in it) in the corner of the screen while the fly-round's camera is up. */
@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {
	@Inject(method = "renderHandsWithItems", at = @At("HEAD"), cancellable = true, require = 0)
	private void orbitarena$noHandInFlyRound(CallbackInfo ci) {
		if (Cinematic.active()) ci.cancel();
	}
}
