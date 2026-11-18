package com.insidethecube.retromon.mixin;

import com.insidethecube.retromon.game.Flags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** In a Retromon save, signs are solid (you bump into them and read them, as in the games, instead of walking through). */
@Mixin(BlockBehaviour.class)
public abstract class SignCollisionMixin {
	@Inject(method = "getCollisionShape", at = @At("HEAD"), cancellable = true, require = 0)
	private void retromon$solidSigns(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx, CallbackInfoReturnable<VoxelShape> cir) {
		if (Flags.solidSigns && (Object) this instanceof SignBlock) cir.setReturnValue(state.getShape(level, pos));
	}
}
