package com.lumenforge.lens.mixin;

import com.lumenforge.lens.LensClient;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Record and photo keys work with a menu open too (an inventory, Axiom, Litematica...), as F2 does. */
@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
	@Inject(method = "keyPress", at = @At("HEAD"), cancellable = true, require = 0)
	private void lightlens$keys(long window, int action, KeyEvent event, CallbackInfo ci) {
		if (LensClient.keyInScreen(action, event)) ci.cancel();
	}
}
