package com.insidethecube.cubework.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.insidethecube.cubework.block.Sounds;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

/** Placing a shape sounds like its material. */
@Mixin(BlockItem.class)
abstract class PlaceSoundMixin {
	@WrapOperation(method = "place", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/state/BlockState;getSoundType()Lnet/minecraft/world/level/block/SoundType;"))
	private SoundType cubework$materialVolume(BlockState state, Operation<SoundType> original, @Local(argsOnly = true) BlockPlaceContext ctx) {
		return Sounds.placing(ctx.getItemInHand(), state, original.call(state));
	}

	@WrapOperation(method = "place", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/item/BlockItem;getPlaceSound(Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/sounds/SoundEvent;"))
	private SoundEvent cubework$materialSound(BlockItem item, BlockState state, Operation<SoundEvent> original, @Local(argsOnly = true) BlockPlaceContext ctx) {
		SoundEvent sound = original.call(item, state);
		return Sounds.placing(ctx.getItemInHand(), state, null) instanceof net.minecraft.world.level.block.SoundType t ? t.getPlaceSound() : sound;
	}
}
