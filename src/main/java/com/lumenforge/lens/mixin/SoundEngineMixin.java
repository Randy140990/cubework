package com.lumenforge.lens.mixin;

import com.lumenforge.lens.audio.GameSound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Sounds starting and stopping, noted while recording so the video gets the game's sound. */
@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {
	@Inject(method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)Lnet/minecraft/client/sounds/SoundEngine$PlayResult;", at = @At("RETURN"))
	private void lightlens$played(SoundInstance sound, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
		try {
			GameSound.onPlay((SoundEngine) (Object) this, sound, cir.getReturnValue());
		} catch (Throwable ignored) {
		}
	}

	@Inject(method = "stop(Lnet/minecraft/client/resources/sounds/SoundInstance;)V", at = @At("HEAD"))
	private void lightlens$stopped(SoundInstance sound, CallbackInfo ci) {
		try {
			GameSound.onStop(sound);
		} catch (Throwable ignored) {
		}
	}

	@Inject(method = "stopAll", at = @At("HEAD"))
	private void lightlens$stoppedAll(CallbackInfo ci) {
		try {
			GameSound.onStopAll();
		} catch (Throwable ignored) {
		}
	}
}
