package com.insidethecube.retromon.mixin;

import com.insidethecube.retromon.client.Adventure;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** When something's between the camera and you, your outline shows through it (so you never lose yourself behind a building). */
@Mixin(Minecraft.class)
public abstract class OutlineMixin {
	@Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true, require = 0)
	private void retromon$behind(Entity e, CallbackInfoReturnable<Boolean> cir) {
		if (Adventure.hidden && e == ((Minecraft) (Object) this).player) cir.setReturnValue(true);
	}
}
