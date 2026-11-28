package com.insidethecube.retromon.mixin;

import com.insidethecube.retromon.client.Adventure;
import com.insidethecube.retromon.client.Device;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Your figure with your Pokédex: the right hand dips to your pocket and brings it up, the left joins it, and you
 * look down at it, both hands holding it in front of you; putting it away, the same the other way.
 */
@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin {
	@Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"), require = 0)
	private void retromon$device(AvatarRenderState state, CallbackInfo ci) {
		// In a Retromon save players' corners are just softened, as the Person Viewer draws everyone.
		com.insidethecube.retromon.client.Soft.apply(((PlayerModel) (Object) this).root(), Adventure.inSave, com.insidethecube.retromon.client.Soft.PEOPLE);
		Minecraft mc = Minecraft.getInstance();
		// ((0.74) John's 10:41: "using surf, when you're moving, the character's legs are moving as if he's running,
		// and obviously he's not running") On your SURF POKéMON's back: sat astride it, legs forward either side, hands
		// down on its back, still as you glide along.
		if (com.insidethecube.retromon.game.Surf.client && mc.player != null && state.id == mc.player.getId()) {
			PlayerModel self = (PlayerModel) (Object) this;
			self.rightLeg.xRot = -1.4137167F;
			self.rightLeg.yRot = (float) (Math.PI / 10);
			self.rightLeg.zRot = 0.07853982F;
			self.leftLeg.xRot = -1.4137167F;
			self.leftLeg.yRot = (float) (-Math.PI / 10);
			self.leftLeg.zRot = -0.07853982F;
			self.rightArm.xRot = -0.62831855F;
			self.rightArm.yRot = 0;
			self.rightArm.zRot = 0.12F;
			self.leftArm.xRot = -0.62831855F;
			self.leftArm.yRot = 0;
			self.leftArm.zRot = -0.12F;
			// (Sat down on its back: the figure lowered by the legs' length, less a little.)
			self.root().y += 9F;
		}
		if (!Adventure.active() || mc.player == null || state.id != mc.player.getId()) return;
		float[] p = Device.pose(mc.getDeltaTracker().getGameTimeDeltaPartialTick(true));
		if (p == null) return;
		PlayerModel self = (PlayerModel) (Object) this;
		float right = p[0], left = p[1], head = p[2], pocket = p[3];
		self.rightArm.xRot = lerp(right, self.rightArm.xRot + pocket * 0.4F, -0.98F);
		self.rightArm.yRot = lerp(right, self.rightArm.yRot, -0.3F);
		self.rightArm.zRot = lerp(right, self.rightArm.zRot + pocket * 0.12F, 0.04F);
		self.leftArm.xRot = lerp(left, self.leftArm.xRot, -0.98F);
		self.leftArm.yRot = lerp(left, self.leftArm.yRot, 0.3F);
		self.leftArm.zRot = lerp(left, self.leftArm.zRot, -0.04F);
		self.head.xRot = lerp(head, self.head.xRot, 0.55F);
		self.head.yRot = lerp(head, self.head.yRot, 0);
	}

	private static float lerp(float k, float a, float b) {
		return a + (b - a) * k;
	}
}
