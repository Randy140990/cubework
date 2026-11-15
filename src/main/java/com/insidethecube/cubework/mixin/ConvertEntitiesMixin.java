package com.insidethecube.cubework.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.insidethecube.cubework.world.Convert;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.chunk.storage.SimpleRegionStorage;

/** The entities of a chunk from before 0.12 (item frames, dropped items, chest minecarts): their old shape items. */
@Mixin(SimpleRegionStorage.class)
abstract class ConvertEntitiesMixin {
	@Inject(method = "upgradeChunkTag(Lnet/minecraft/nbt/CompoundTag;I)Lnet/minecraft/nbt/CompoundTag;", at = @At("RETURN"))
	private void cubework$convert(CompoundTag tag, int version, CallbackInfoReturnable<CompoundTag> cir) {
		CompoundTag out = cir.getReturnValue();
		if (out != null && out.contains("Entities")) Convert.items(out);
	}
}
