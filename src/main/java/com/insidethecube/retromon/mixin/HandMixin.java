package com.insidethecube.retromon.mixin;

import com.insidethecube.retromon.client.Adventure;
import net.minecraft.client.renderer.ItemInHandRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * No first-person hand in the adventure. Out in the world the camera's behind you anyway; in a battle it stands where
 * you were (you step out of the picture), and Minecraft drew your arm in the corner: the dark shape at the bottom right.
 */
@Mixin(ItemInHandRenderer.class)
public abstract class HandMixin {
	@Inject(method = "renderHandsWithItems", at = @At("HEAD"), cancellable = true, require = 0)
	private void retromon$noHand(CallbackInfo ci) {
		if (Adventure.active()) ci.cancel();
	}
}
