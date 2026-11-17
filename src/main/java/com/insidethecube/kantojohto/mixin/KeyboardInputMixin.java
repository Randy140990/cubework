package com.insidethecube.kantojohto.mixin;

import com.insidethecube.kantojohto.client.Adventure;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Walking in the adventure: the keys move you in the camera's directions (up the screen, down, left, right,
 * and between), turning you to face the way you walk. No jumping (Space is the A button).
 */
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends ClientInput {
	@Inject(method = "tick", at = @At("TAIL"))
	private void kantojohto$walk(CallbackInfo ci) {
		if (!Adventure.active()) return;
		LocalPlayer player = Minecraft.getInstance().player;
		Input k = this.keyPresses;
		if (player == null || Adventure.frozen > 0) {
			this.keyPresses = Input.EMPTY;
			this.moveVector = Vec2.ZERO;
			return;
		}
		float yaw = Adventure.walkYaw(k.forward(), k.backward(), k.left(), k.right());
		if (Float.isNaN(yaw)) {
			this.keyPresses = new Input(false, false, false, false, false, false, false);
			this.moveVector = Vec2.ZERO;
			return;
		}
		float now = player.getYRot();
		float turned = now + Mth.clamp(Mth.wrapDegrees(yaw - now), -45F, 45F);
		player.setYRot(turned);
		player.setYHeadRot(turned);
		player.setXRot(10F);
		this.keyPresses = new Input(true, false, false, false, false, false, k.sprint());
		this.moveVector = new Vec2(0, 1);
	}
}
