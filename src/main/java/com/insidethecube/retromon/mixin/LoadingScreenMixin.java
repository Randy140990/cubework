package com.insidethecube.retromon.mixin;

import com.insidethecube.retromon.client.Adventure;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Going through a door in the adventure (into a building's inside, or back out) changes dimension, and Minecraft
 * puts up its "Loading terrain" screen while the other side loads. In the adventure that moment is the darkest of
 * the door's fade, so it's just that dark, with nothing on it, and the fade carries on as you arrive.
 */
@Mixin(LevelLoadingScreen.class)
public abstract class LoadingScreenMixin {
	@Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
	private void retromon$dark(GuiGraphics g, int mouseX, int mouseY, float partial, CallbackInfo ci) {
		// Starting the new game: our loading page carries on over Minecraft's ("Preparing...", "Loading terrain").
		if (com.insidethecube.retromon.client.IntroScreen.launching()) {
			com.insidethecube.retromon.client.IntroScreen.drawArriving(g);
			ci.cancel();
			return;
		}
		if (Adventure.active()) {
			g.fill(0, 0, g.guiWidth(), g.guiHeight(), 0xFF05070F);
			ci.cancel();
		}
	}

	@Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true, require = 0)
	private void retromon$darkBack(GuiGraphics g, int mouseX, int mouseY, float partial, CallbackInfo ci) {
		if (Adventure.active() || com.insidethecube.retromon.client.IntroScreen.launching()) {
			g.fill(0, 0, g.guiWidth(), g.guiHeight(), 0xFF05070F);
			ci.cancel();
		}
	}
}
