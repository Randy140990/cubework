package com.insidethecube.cubework.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.insidethecube.cubework.world.Convert;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;

/** A chunk from before 0.12: its old shape blocks become the new ones with their material (see world.Convert). */
@Mixin(SerializableChunkData.class)
abstract class ConvertChunkMixin {
	@Inject(method = "parse", at = @At("HEAD"))
	private static void cubework$convert(LevelHeightAccessor height, PalettedContainerFactory containers, CompoundTag tag,
			CallbackInfoReturnable<SerializableChunkData> cir) {
		Convert.chunk(tag);
	}
}
