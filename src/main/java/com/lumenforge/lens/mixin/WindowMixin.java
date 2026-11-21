package com.lumenforge.lens.mixin;

import com.lumenforge.lens.capture.Capture;
import com.lumenforge.lens.capture.LensWindow;
import com.lumenforge.lens.capture.RenderScale;
import com.mojang.blaze3d.TracyFrameCapture;
import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The window: its real size for Lightlens; while the game draws bigger than the window, the bigger size (and GUI
 * scale, so menus keep their size) for the game; and the moment each finished frame is about to be shown.
 */
@Mixin(Window.class)
public abstract class WindowMixin implements LensWindow {
	@Shadow
	private int framebufferWidth;
	@Shadow
	private int framebufferHeight;

	@Override
	public int lightlens$realWidth() {
		return framebufferWidth;
	}

	@Override
	public int lightlens$realHeight() {
		return framebufferHeight;
	}

	@Inject(method = "getWidth", at = @At("RETURN"), cancellable = true)
	private void lightlens$width(CallbackInfoReturnable<Integer> cir) {
		int k = RenderScale.k();
		if (k > 1) cir.setReturnValue(framebufferWidth * k);
	}

	@Inject(method = "getHeight", at = @At("RETURN"), cancellable = true)
	private void lightlens$height(CallbackInfoReturnable<Integer> cir) {
		int k = RenderScale.k();
		if (k > 1) cir.setReturnValue(framebufferHeight * k);
	}

	@Inject(method = "getGuiScale", at = @At("RETURN"), cancellable = true)
	private void lightlens$guiScale(CallbackInfoReturnable<Integer> cir) {
		int k = RenderScale.k();
		if (k > 1) cir.setReturnValue(cir.getReturnValue() * k);
	}

	@Inject(method = "updateDisplay", at = @At("HEAD"))
	private void lightlens$frameEnd(TracyFrameCapture tracy, CallbackInfo ci) {
		Capture.onFrameEnd(framebufferWidth, framebufferHeight);
	}
}
