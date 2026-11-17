package com.insidethecube.retromon.mixin;

import com.insidethecube.retromon.client.Adventure;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Walking in the adventure: the keys move you in the camera's directions (up the screen is north, and the
 * diagonals), turning you to face the way you walk. Shift runs. No jumping (Space is the A button), and no
 * walking into water (until there's SURF); if you're ever in it, you swim out.
 */
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends ClientInput {
	@Inject(method = "tick", at = @At("TAIL"))
	private void retromon$walk(CallbackInfo ci) {
		if (!Adventure.active()) return;
		LocalPlayer player = Minecraft.getInstance().player;
		Input k = this.keyPresses;
		if (player == null || Adventure.frozen > 0) {
			this.keyPresses = Input.EMPTY;
			this.moveVector = Vec2.ZERO;
			return;
		}
		boolean run = k.shift() || k.sprint();
		float yaw = Adventure.walkYaw(k.forward(), k.backward(), k.left(), k.right());
		boolean swimming = player.isInWater();
		if (Float.isNaN(yaw)) {
			this.keyPresses = new Input(false, false, false, false, swimming, false, false);
			this.moveVector = Vec2.ZERO;
			return;
		}
		if (!swimming && waterAhead(player, yaw)) {
			this.keyPresses = new Input(false, false, false, false, false, false, false);
			this.moveVector = Vec2.ZERO;
			player.setYRot(yaw);
			player.setYHeadRot(yaw);
			return;
		}
		float now = player.getYRot();
		float turned = now + Mth.clamp(Mth.wrapDegrees(yaw - now), -45F, 45F);
		player.setYRot(turned);
		player.setYHeadRot(turned);
		player.setXRot(10F);
		this.keyPresses = new Input(true, false, false, false, swimming, false, run);
		this.moveVector = new Vec2(0, 1);
	}

	/** Water just ahead, at your feet or where you'd step down to. */
	private static boolean waterAhead(LocalPlayer p, float yaw) {
		Level level = p.level();
		double r = Math.toRadians(yaw);
		double x = p.getX() - Math.sin(r) * 0.75, z = p.getZ() + Math.cos(r) * 0.75;
		BlockPos feet = BlockPos.containing(x, p.getY() + 0.1, z);
		for (BlockPos at : new BlockPos[] {feet, feet.below(), feet.below(2)}) {
			if (level.getFluidState(at).is(FluidTags.WATER)) return true;
			if (!level.getBlockState(at).getCollisionShape(level, at).isEmpty()) return false;
		}
		return false;
	}
}
