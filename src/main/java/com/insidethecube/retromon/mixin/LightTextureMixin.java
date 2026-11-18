package com.insidethecube.retromon.mixin;

import com.insidethecube.retromon.client.Night;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Without a shader pack the light map only asks for night vision if you have the effect: say yes at night (see Night). */
@Mixin(LightTexture.class)
public abstract class LightTextureMixin {
	@Redirect(method = "updateLightTexture", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/player/LocalPlayer;hasEffect(Lnet/minecraft/core/Holder;)Z"), require = 0)
	private boolean retromon$moonlight(LocalPlayer player, Holder<MobEffect> effect) {
		return player.hasEffect(effect) || effect.equals(MobEffects.NIGHT_VISION) && Night.lift() > 0;
	}
}
