package com.insidethecube.retromon.mixin;

import com.insidethecube.retromon.client.Adventure;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Your outline through walls in a soft sky blue. */
@Mixin(Entity.class)
public abstract class OutlineColourMixin {
	@Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true, require = 0)
	private void retromon$colour(CallbackInfoReturnable<Integer> cir) {
		if (Adventure.hidden && (Object) this == Minecraft.getInstance().player) cir.setReturnValue(0x8FDFFF);
	}
}
