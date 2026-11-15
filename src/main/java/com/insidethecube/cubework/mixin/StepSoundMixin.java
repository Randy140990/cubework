package com.insidethecube.cubework.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.insidethecube.cubework.block.Sounds;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

/** Footsteps on a shape sound like its material. */
@Mixin(Entity.class)
abstract class StepSoundMixin {
	@WrapOperation(method = "playStepSound", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/state/BlockState;getSoundType()Lnet/minecraft/world/level/block/SoundType;"))
	private SoundType cubework$material(BlockState state, Operation<SoundType> original, @Local(argsOnly = true) BlockPos pos) {
		return Sounds.at(((Entity) (Object) this).level(), pos, state, original.call(state));
	}
}
