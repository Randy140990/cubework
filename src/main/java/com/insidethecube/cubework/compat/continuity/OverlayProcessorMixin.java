package com.insidethecube.cubework.compat.continuity;

import me.pepperbell.continuity.api.client.QuadProcessor;
import me.pepperbell.continuity.client.processor.overlay.StandardOverlayQuadProcessor;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Continuity's connected overlays (grass edges onto sand, gravel onto grass): Cubework shapes give their
 * overlays like full blocks do (a grass slope edges the sand slope beside it), and an overlay on a shape
 * follows the shape's face.
 */
@Mixin(value = StandardOverlayQuadProcessor.class, remap = false)
public abstract class OverlayProcessorMixin {
	@Inject(method = "processQuadInner", at = @At("HEAD"))
	private void cubework$begin(MutableQuadView quad, TextureAtlasSprite sprite, BlockAndTintGetter level, BlockPos pos, BlockState appearanceState,
			BlockState state, RandomSource random, int pass, QuadProcessor.ProcessingContext context,
			CallbackInfoReturnable<QuadProcessor.ProcessingResult> cir) {
		OverlayQuads.begin(quad, state);
	}

	@Inject(method = "processQuadInner", at = @At("RETURN"))
	private void cubework$end(MutableQuadView quad, TextureAtlasSprite sprite, BlockAndTintGetter level, BlockPos pos, BlockState appearanceState,
			BlockState state, RandomSource random, int pass, QuadProcessor.ProcessingContext context,
			CallbackInfoReturnable<QuadProcessor.ProcessingResult> cir) {
		OverlayQuads.end();
	}

	@Redirect(method = "appliesOverlay", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/state/BlockState;isCollisionShapeFullBlock(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Z",
			remap = true))
	private boolean cubework$shapesGiveOverlays(BlockState other, BlockGetter level, BlockPos pos) {
		return OverlayQuads.shape(other) || other.isCollisionShapeFullBlock(level, pos);
	}
}
