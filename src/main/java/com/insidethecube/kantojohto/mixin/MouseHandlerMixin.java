package com.insidethecube.kantojohto.mixin;

import com.insidethecube.kantojohto.client.Adventure;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** In the adventure the mouse doesn't turn you, and the wheel zooms the camera. */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
	@Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
	private void kantojohto$noTurn(double movementTime, CallbackInfo ci) {
		if (Adventure.active()) ci.cancel();
	}

	@Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
	private void kantojohto$zoom(long window, double x, double y, CallbackInfo ci) {
		if (!Adventure.active() || Minecraft.getInstance().screen != null) return;
		Adventure.zoom(y);
		ci.cancel();
	}
}
