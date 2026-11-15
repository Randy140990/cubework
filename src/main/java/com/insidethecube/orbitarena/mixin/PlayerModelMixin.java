package com.insidethecube.orbitarena.mixin;

import com.insidethecube.orbitarena.client.Emotes;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A player who's celebrating (see client.Emotes) moves as the celebration goes, after the usual pose. */
@Mixin(PlayerModel.class)
public class PlayerModelMixin {
	@Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"))
	private void orbitarena$celebrate(AvatarRenderState state, CallbackInfo ci) {
		Emotes.pose((PlayerModel) (Object) this, state);
	}
}
