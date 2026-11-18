package com.insidethecube.retromon.mixin;

import com.insidethecube.retromon.client.Night;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Nights lifted to a moonlit blue (see Night): the night vision strength the game and shader packs read. The
 * server gives you an unseen night vision at night (strength 8, so it's told apart from a potion); its strength
 * on screen follows how far into the night it is.
 */
@Mixin(GameRenderer.class)
public abstract class NightVisionMixin {
	@Inject(method = "getNightVisionScale", at = @At("HEAD"), cancellable = true, require = 0)
	private static void retromon$moonlight(LivingEntity e, float partial, CallbackInfoReturnable<Float> cir) {
		var nv = e.getEffect(MobEffects.NIGHT_VISION);
		if (nv != null && nv.getAmplifier() == Night.AMPLIFIER) cir.setReturnValue(Night.lift());
		else if (nv == null) {
			float lift = Night.lift();
			if (lift > 0) cir.setReturnValue(lift);
		}
	}
}
