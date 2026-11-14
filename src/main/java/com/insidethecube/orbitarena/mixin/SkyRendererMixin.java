package com.insidethecube.orbitarena.mixin;

import com.insidethecube.orbitarena.client.ArenaSky;
import net.minecraft.client.renderer.SkyRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** In the arena, the night-sky panorama is drawn in place of the End's sky box. */
@Mixin(SkyRenderer.class)
public class SkyRendererMixin {
	@Inject(method = "renderEndSky", at = @At("HEAD"), cancellable = true)
	private void orbitarena$arenaSky(CallbackInfo ci) {
		if (ArenaSky.active()) {
			ArenaSky.render();
			ci.cancel();
		}
	}
}
