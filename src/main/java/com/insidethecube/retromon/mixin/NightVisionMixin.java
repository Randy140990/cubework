package com.insidethecube.retromon.mixin;

import com.insidethecube.retromon.client.Night;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Nights lifted to a moonlit blue (see Night): the night vision strength the game and shader packs read. */
@Mixin(GameRenderer.class)
public abstract class NightVisionMixin {
	@Inject(method = "getNightVisionScale", at = @At("HEAD"), cancellable = true, require = 0)
	private static void retromon$moonlight(LivingEntity e, float partial, CallbackInfoReturnable<Float> cir) {
		float lift = Night.lift();
		if (lift > 0 && !e.hasEffect(MobEffects.NIGHT_VISION)) cir.setReturnValue(lift);
	}
}
