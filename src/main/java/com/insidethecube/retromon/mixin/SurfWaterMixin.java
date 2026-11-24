package com.insidethecube.retromon.mixin;

import com.insidethecube.retromon.game.Surf;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** (0.61) SURF: while you're on a POKéMON's back, the water's top holds you up (Surf.shape). */
@Mixin(LiquidBlock.class)
public abstract class SurfWaterMixin {
	@Inject(method = "getCollisionShape", at = @At("HEAD"), cancellable = true, require = 0)
	private void retromon$surf(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx, CallbackInfoReturnable<VoxelShape> cir) {
		VoxelShape s = Surf.shape(state, level, pos, ctx);
		if (s != null) cir.setReturnValue(s);
	}
}
