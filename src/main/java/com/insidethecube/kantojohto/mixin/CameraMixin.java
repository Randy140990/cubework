package com.insidethecube.kantojohto.mixin;

import com.insidethecube.kantojohto.client.Adventure;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The adventure's fixed 2.5D camera (and the battle camera), in place of the game's. */
@Mixin(Camera.class)
public abstract class CameraMixin {
	@Shadow
	private boolean detached;

	@Shadow
	protected abstract void setRotation(float yRot, float xRot);

	@Shadow
	protected abstract void setPosition(double x, double y, double z);

	@Inject(method = "setup", at = @At("TAIL"))
	private void kantojohto$view(Level level, Entity entity, boolean isDetached, boolean mirror, float partial, CallbackInfo ci) {
		if (!Adventure.active() || entity == null) return;
		double[] p = Adventure.pose(entity, partial);
		if (p == null) return;
		this.detached = true;
		setRotation((float) p[3], (float) p[4]);
		setPosition(p[0], p[1], p[2]);
	}
}
