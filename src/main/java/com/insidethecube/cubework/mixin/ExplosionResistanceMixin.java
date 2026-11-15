package com.insidethecube.cubework.mixin;

import java.util.Optional;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.insidethecube.cubework.Material;
import com.insidethecube.cubework.block.CwShape;
import com.insidethecube.cubework.block.ShapeEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

/** A placed shape stands up to explosions like its material (obsidian stairs survive TNT, sand ones don't). */
@Mixin(ExplosionDamageCalculator.class)
abstract class ExplosionResistanceMixin {
	@Inject(method = "getBlockExplosionResistance", at = @At("HEAD"), cancellable = true)
	private void cubework$materialResistance(Explosion explosion, BlockGetter world, BlockPos pos, BlockState state, FluidState fluid,
			CallbackInfoReturnable<Optional<Float>> cir) {
		if (!(state.getBlock() instanceof CwShape)) return;
		Material m = ShapeEntity.at(world, pos);
		Block base = m == null ? null : m.base();
		if (base != null) cir.setReturnValue(Optional.of(Math.max(base.getExplosionResistance(), fluid.getExplosionResistance())));
	}
}
