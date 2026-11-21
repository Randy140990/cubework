package com.lumenforge.lens.mixin;

import com.lumenforge.lens.capture.Capture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The game's main picture, finished (world, shaders, hand, HUD and menus), as it's about to be shown. Taken here
 * when the game draws bigger than the window, and then shrunk onto the window by us (the game's own copy would only
 * show a corner of it).
 */
@Mixin(RenderTarget.class)
public abstract class RenderTargetMixin {
	@Inject(method = "blitToScreen", at = @At("HEAD"), cancellable = true)
	private void lightlens$present(CallbackInfo ci) {
		RenderTarget self = (RenderTarget) (Object) this;
		if (self == Minecraft.getInstance().getMainRenderTarget() && Capture.beforePresent(self)) ci.cancel();
	}
}
