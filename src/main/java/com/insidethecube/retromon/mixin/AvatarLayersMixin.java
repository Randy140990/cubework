package com.insidethecube.retromon.mixin;

import com.insidethecube.retromon.client.Adventure;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * In a Retromon save your hero's cap, jacket and sleeves always show (they're on the skin's outer layer, which
 * Minecraft's Skin Customization can switch off).
 */
@Mixin(AvatarRenderer.class)
public abstract class AvatarLayersMixin {
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("TAIL"),
			require = 0)
	private void retromon$layers(Avatar avatar, AvatarRenderState state, float partial, CallbackInfo ci) {
		if (!Adventure.inSave || (Object) avatar != Minecraft.getInstance().player) return;
		state.showHat = true;
		state.showJacket = true;
		state.showLeftSleeve = true;
		state.showRightSleeve = true;
		state.showLeftPants = true;
		state.showRightPants = true;
	}
}
