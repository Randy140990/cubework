package com.insidethecube.cubework.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.insidethecube.cubework.CwBlocks;
import com.insidethecube.cubework.block.Shared;

import net.minecraft.block.BlockState;
import net.minecraft.block.TranslucentBlock;
import net.minecraft.util.math.Direction;

/** A glass block hides its face against a glass shape of the same glass that covers it, as it does against glass. */
@Mixin(TranslucentBlock.class)
abstract class TranslucentBlockMixin {
	@Inject(method = "isSideInvisible", at = @At("HEAD"), cancellable = true)
	private void cubework$hideAgainstShapes(BlockState state, BlockState stateFrom, Direction direction, CallbackInfoReturnable<Boolean> cir) {
		if (CwBlocks.info(stateFrom.getBlock()) == null) return;
		if (Shared.hidesFace(CwBlocks.materialOf(state.getBlock()), state, stateFrom, direction)) cir.setReturnValue(true);
	}
}
