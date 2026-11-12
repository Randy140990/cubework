package com.insidethecube.cubework.compat.continuity;

import me.pepperbell.continuity.api.client.QuadProcessor;
import me.pepperbell.continuity.client.processor.overlay.SimpleOverlayQuadProcessor;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Continuity's simple overlays on a Cubework shape follow the shape's face (see QuadUtilMixin). */
@Mixin(value = SimpleOverlayQuadProcessor.class, remap = false)
public abstract class SimpleOverlayMixin {
	@Inject(method = "processQuad", at = @At("HEAD"))
	private void cubework$begin(MutableQuadView quad, TextureAtlasSprite sprite, BlockAndTintGetter level, BlockPos pos, BlockState appearanceState,
			BlockState state, RandomSource random, int pass, QuadProcessor.ProcessingContext context,
			CallbackInfoReturnable<QuadProcessor.ProcessingResult> cir) {
		OverlayQuads.begin(quad, state);
	}

	@Inject(method = "processQuad", at = @At("RETURN"))
	private void cubework$end(MutableQuadView quad, TextureAtlasSprite sprite, BlockAndTintGetter level, BlockPos pos, BlockState appearanceState,
			BlockState state, RandomSource random, int pass, QuadProcessor.ProcessingContext context,
			CallbackInfoReturnable<QuadProcessor.ProcessingResult> cir) {
		OverlayQuads.end();
	}
}
