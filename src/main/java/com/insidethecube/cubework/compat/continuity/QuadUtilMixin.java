package com.insidethecube.cubework.compat.continuity;

import me.pepperbell.continuity.client.util.QuadUtil;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadView;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** An overlay on a Cubework shape takes the shape's own quad's corners rather than the whole block face. */
@Mixin(value = QuadUtil.class, remap = false)
public abstract class QuadUtilMixin {
	@Redirect(method = "emitOverlayQuad", at = @At(value = "INVOKE",
			target = "Lnet/fabricmc/fabric/api/renderer/v1/mesh/QuadEmitter;square(Lnet/minecraft/core/Direction;FFFFF)Lnet/fabricmc/fabric/api/renderer/v1/mesh/QuadEmitter;",
			remap = true))
	private static QuadEmitter cubework$followShape(QuadEmitter emitter, Direction face, float left, float bottom, float right, float top, float depth) {
		QuadView quad = OverlayQuads.current();
		if (quad == null) return emitter.square(face, left, bottom, right, top, depth);
		for (int i = 0; i < 4; i++) emitter.pos(i, quad.x(i), quad.y(i), quad.z(i));
		emitter.nominalFace(face);
		emitter.cullFace(quad.cullFace());
		return emitter;
	}
}
