package com.insidethecube.retromon.mixin;

import com.insidethecube.retromon.client.Music;
import net.minecraft.client.sounds.MusicManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Minecraft's own music keeps quiet while Retromon's plays. */
@Mixin(MusicManager.class)
public abstract class MusicManagerMixin {
	@Inject(method = "tick", at = @At("HEAD"), cancellable = true, require = 0)
	private void retromon$quiet(CallbackInfo ci) {
		if (Music.active()) ci.cancel();
	}
}
