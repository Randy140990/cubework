package com.insidethecube.retromon.mixin;

import com.insidethecube.retromon.client.Adventure;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * In the Retromon game only the game's own keys work out in the world (walk, run, SPACE, ENTER, TAB, Esc, and
 * Minecraft's screenshot, full screen and hide-the-HUD): E no longer opens the inventory, the hotbar keys, Q, F, T,
 * F3, F5 and every other mod's keys (shaders, maps, schematics...) do nothing. Taken before anything else sees
 * them (a low priority number, so it's first). Screens of the game get their keys as ever.
 */
@Mixin(value = KeyboardHandler.class, priority = 100)
public abstract class KeyboardHandlerMixin {
	@Inject(method = "keyPress", at = @At("HEAD"), cancellable = true, require = 0)
	private void retromon$onlyOurs(long window, int action, KeyEvent event, CallbackInfo ci) {
		if (Adventure.blocks(event)) ci.cancel();
	}
}
