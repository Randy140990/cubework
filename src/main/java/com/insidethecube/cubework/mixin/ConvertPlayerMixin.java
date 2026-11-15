package com.insidethecube.cubework.mixin;

import java.util.Optional;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.insidethecube.cubework.world.Convert;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;

/** A player from before 0.12 (single player's from the world, others' from playerdata): their old shape items. */
@Mixin(PlayerList.class)
abstract class ConvertPlayerMixin {
	@Inject(method = "loadPlayerData", at = @At("RETURN"))
	private void cubework$convert(NameAndId player, CallbackInfoReturnable<Optional<CompoundTag>> cir) {
		Optional<CompoundTag> tag = cir.getReturnValue();
		if (tag != null) tag.ifPresent(Convert::items);
	}
}
