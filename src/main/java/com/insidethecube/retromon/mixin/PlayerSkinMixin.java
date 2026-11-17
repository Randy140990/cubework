package com.insidethecube.retromon.mixin;

import com.insidethecube.retromon.Retromon;
import com.insidethecube.retromon.client.Adventure;
import com.insidethecube.retromon.client.ClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.ClientAsset;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** In a Retromon save you look like the games' hero: the boy or the girl you chose at the start. */
@Mixin(AbstractClientPlayer.class)
public abstract class PlayerSkinMixin {
	@org.spongepowered.asm.mixin.Unique
	private static PlayerSkin retromon$boy, retromon$girl;

	@Inject(method = "getSkin", at = @At("HEAD"), cancellable = true)
	private void retromon$hero(CallbackInfoReturnable<PlayerSkin> cir) {
		if (!Adventure.inSave || (Object) this != Minecraft.getInstance().player) return;
		boolean girl = "girl".equals(ClientState.data.gender);
		if (retromon$boy == null) {
			retromon$boy = PlayerSkin.insecure(new ClientAsset.ResourceTexture(Retromon.id("entity/player/boy")), null, null, PlayerModelType.WIDE);
			retromon$girl = PlayerSkin.insecure(new ClientAsset.ResourceTexture(Retromon.id("entity/player/girl")), null, null, PlayerModelType.WIDE);
		}
		cir.setReturnValue(girl ? retromon$girl : retromon$boy);
	}
}
