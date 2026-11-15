package com.insidethecube.cubework.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.insidethecube.cubework.block.Sounds;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelEventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

/** Breaking a shape sounds like its material. */
@Mixin(LevelEventHandler.class)
abstract class BreakSoundMixin {
	@WrapOperation(method = "levelEvent", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/state/BlockState;getSoundType()Lnet/minecraft/world/level/block/SoundType;"))
	private SoundType cubework$material(BlockState state, Operation<SoundType> original, @Local(argsOnly = true) BlockPos pos) {
		return Sounds.at(Minecraft.getInstance().level, pos, state, original.call(state));
	}
}
