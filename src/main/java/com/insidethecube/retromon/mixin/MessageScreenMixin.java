package com.insidethecube.retromon.mixin;

import com.insidethecube.retromon.client.IntroScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Starting the new game, Minecraft shows "Preparing Resources..." (and the like) on a plain message screen while the
 * world opens. Then it's Retromon's own loading page that shows, carrying on from the one before (no Minecraft words).
 */
@Mixin(GenericMessageScreen.class)
public abstract class MessageScreenMixin {
	@Inject(method = "init", at = @At("HEAD"), cancellable = true, require = 0)
	private void retromon$noWords(CallbackInfo ci) {
		if (IntroScreen.launching()) ci.cancel();
	}

	@Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true, require = 0)
	private void retromon$ours(GuiGraphics g, int mouseX, int mouseY, float partial, CallbackInfo ci) {
		if (IntroScreen.launching()) {
			IntroScreen.drawArriving(g);
			ci.cancel();
		}
	}
}
