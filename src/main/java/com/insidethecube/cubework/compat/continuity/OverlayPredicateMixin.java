package com.insidethecube.cubework.compat.continuity;

import me.pepperbell.continuity.api.client.ProcessingDataProvider;
import me.pepperbell.continuity.client.processor.overlay.OverlayProcessingPredicate;
import me.pepperbell.continuity.client.util.QuadUtil;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadView;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Overlays go on any of a Cubework shape's faces, not only whole block faces (they follow the face, see QuadUtilMixin). */
@Mixin(value = OverlayProcessingPredicate.class, remap = false)
public abstract class OverlayPredicateMixin {
	@Redirect(method = "shouldProcessQuad", at = @At(value = "INVOKE",
			target = "Lme/pepperbell/continuity/client/util/QuadUtil;isQuadUnitSquare(Lnet/fabricmc/fabric/api/renderer/v1/mesh/QuadView;)Z"))
	private boolean cubework$anyShapeFace(QuadView quad, QuadView same, TextureAtlasSprite sprite, BlockAndTintGetter level, BlockPos pos,
			BlockState appearanceState, BlockState state, ProcessingDataProvider dataProvider) {
		return OverlayQuads.shape(state) || QuadUtil.isQuadUnitSquare(quad);
	}
}
