package com.insidethecube.retromon.mixin;

import com.insidethecube.retromon.client.IntroScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Retromon takes over the start: wherever the game would show Minecraft's title screen (on launch, after leaving
 * a world), it shows Retromon's instead. Its MINECRAFT option goes to the usual title screen when you want it.
 */
@Mixin(Minecraft.class)
public abstract class TitleMixin {
	@ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true, require = 0)
	private Screen retromon$ourTitle(Screen screen) {
		if (screen instanceof TitleScreen title && !IntroScreen.minecraftTitle) return new IntroScreen(title);
		return screen;
	}
}
