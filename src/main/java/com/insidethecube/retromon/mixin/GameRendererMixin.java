package com.insidethecube.retromon.mixin;

import com.insidethecube.retromon.client.Adventure;
import com.insidethecube.retromon.client.BattleScene;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A narrow field of view in the adventure, so the world looks nearly flat from above as in the games, and no
 * view bobbing (the camera doesn't bounce as you walk).
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	@Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
	private void retromon$fov(Camera camera, float partial, boolean useSetting, CallbackInfoReturnable<Float> cir) {
		if (Adventure.active()) cir.setReturnValue(BattleScene.active() ? BattleScene.fov() : (float) Adventure.FOV);
	}

	@Inject(method = "bobView", at = @At("HEAD"), cancellable = true, require = 0)
	private void retromon$noBob(CallbackInfo ci) {
		if (Adventure.active()) ci.cancel();
	}

	/** No first-person hand in the adventure (see HandMixin; this is the same, a step earlier). */
	@Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true, require = 0)
	private void retromon$noHand(CallbackInfo ci) {
		if (Adventure.active()) ci.cancel();
	}

	@Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true, require = 0)
	private void retromon$noHurtBob(CallbackInfo ci) {
		if (Adventure.active()) ci.cancel();
	}
}
