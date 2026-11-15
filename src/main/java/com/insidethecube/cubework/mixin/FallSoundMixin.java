package com.insidethecube.cubework.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.insidethecube.cubework.block.Sounds;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

/** Landing on a shape sounds like its material. */
@Mixin(LivingEntity.class)
abstract class FallSoundMixin {
	@WrapOperation(method = "playBlockFallSound", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/state/BlockState;getSoundType()Lnet/minecraft/world/level/block/SoundType;"))
	private SoundType cubework$material(BlockState state, Operation<SoundType> original) {
		LivingEntity self = (LivingEntity) (Object) this;
		BlockPos pos = BlockPos.containing(self.getX(), self.getY() - 0.2, self.getZ());
		return Sounds.at(self.level(), pos, state, original.call(state));
	}
}
