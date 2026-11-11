package com.insidethecube.cubework.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.insidethecube.cubework.CwBlocks;
import com.insidethecube.cubework.block.Shared;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.state.BlockState;

/** A glass block hides its face against a glass shape of the same glass that covers it, as it does against glass. */
@Mixin(HalfTransparentBlock.class)
abstract class TranslucentBlockMixin {
	@Inject(method = "skipRendering", at = @At("HEAD"), cancellable = true)
	private void cubework$hideAgainstShapes(BlockState state, BlockState stateFrom, Direction direction, CallbackInfoReturnable<Boolean> cir) {
		if (CwBlocks.info(stateFrom.getBlock()) == null) return;
		if (Shared.hidesFace(CwBlocks.materialOf(state.getBlock()), state, stateFrom, direction)) cir.setReturnValue(true);
	}
}
