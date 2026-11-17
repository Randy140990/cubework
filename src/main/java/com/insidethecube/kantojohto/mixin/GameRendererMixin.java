package com.insidethecube.kantojohto.mixin;

import com.insidethecube.kantojohto.client.Adventure;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A narrow field of view in the adventure, so the world looks nearly flat from above, as in the games. */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	@Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
	private void kantojohto$fov(Camera camera, float partial, boolean useSetting, CallbackInfoReturnable<Float> cir) {
		if (Adventure.active()) cir.setReturnValue(com.insidethecube.kantojohto.client.BattleScene.active() ? com.insidethecube.kantojohto.client.BattleScene.fov() : (float) Adventure.FOV);
	}
}
