package com.lumenforge.lens.mixin;

import com.lumenforge.lens.capture.Recorder;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** While recording, the window's title says so. */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
	@Inject(method = "createTitle", at = @At("RETURN"), cancellable = true, require = 0)
	private void lightlens$title(CallbackInfoReturnable<String> cir) {
		if (Recorder.recording()) cir.setReturnValue((Recorder.paused() ? "❚❚ Paused · " : "● REC · ") + cir.getReturnValue());
	}
}
