package com.insidethecube.retromon.mixin;

import com.insidethecube.retromon.client.AxiomQuiet;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** (0.59) Axiom's tips in the chat, not shown in a Retromon save (Axiom's turned off there). */
@Mixin(ChatComponent.class)
public abstract class ChatQuietMixin {
	@Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V", at = @At("HEAD"),
			cancellable = true, require = 0)
	private void retromon$quiet(Component message, MessageSignature signature, GuiMessageTag tag, CallbackInfo ci) {
		if (AxiomQuiet.on() && AxiomQuiet.axiomText(message)) ci.cancel();
	}
}
