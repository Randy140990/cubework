package com.insidethecube.cubework.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.insidethecube.cubework.block.CwShape;
import com.insidethecube.cubework.block.ShapeEntity;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** A broken shape drops when the tool is right for its material (stone needs a pickaxe; dirt doesn't). */
@Mixin(ServerPlayerGameMode.class)
abstract class HarvestMixin {
	@WrapOperation(method = "destroyBlock", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/server/level/ServerPlayer;hasCorrectToolForDrops(Lnet/minecraft/world/level/block/state/BlockState;)Z"))
	private boolean cubework$materialTool(ServerPlayer player, BlockState state, Operation<Boolean> original, @Local BlockEntity entity) {
		if (state.getBlock() instanceof CwShape && entity instanceof ShapeEntity e && e.material() != null && e.material().base() != null) {
			return original.call(player, e.material().base().defaultBlockState());
		}
		return original.call(player, state);
	}
}
