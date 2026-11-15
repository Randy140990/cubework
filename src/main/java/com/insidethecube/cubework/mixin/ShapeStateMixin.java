package com.insidethecube.cubework.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.insidethecube.cubework.CwBlocks;
import com.insidethecube.cubework.Material;
import com.insidethecube.cubework.ShapeItem;
import com.insidethecube.cubework.block.CwShape;
import com.insidethecube.cubework.block.ShapeEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * A placed shape mines like its material (a wool slab with shears, a stone one with a pickaxe, at each's
 * speed), shows its material's colour on maps, and picks (middle click) as a stack of its material.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
abstract class ShapeStateMixin {
	@Shadow
	public abstract Block getBlock();

	@Inject(method = "getDestroyProgress", at = @At("HEAD"), cancellable = true)
	private void cubework$mineLikeMaterial(Player player, BlockGetter world, BlockPos pos, CallbackInfoReturnable<Float> cir) {
		if (!(getBlock() instanceof CwShape)) return;
		Material m = ShapeEntity.at(world, pos);
		Block base = m == null ? null : m.base();
		if (base != null) cir.setReturnValue(base.defaultBlockState().getDestroyProgress(player, world, pos));
	}

	@Inject(method = "getMapColor", at = @At("HEAD"), cancellable = true)
	private void cubework$materialColour(BlockGetter world, BlockPos pos, CallbackInfoReturnable<MapColor> cir) {
		if (!(getBlock() instanceof CwShape)) return;
		Material m = ShapeEntity.at(world, pos);
		Block base = m == null ? null : m.base();
		if (base != null) cir.setReturnValue(base.defaultBlockState().getMapColor(world, pos));
	}

	@Inject(method = "getCloneItemStack", at = @At("RETURN"))
	private void cubework$pickMaterial(LevelReader world, BlockPos pos, boolean includeData, CallbackInfoReturnable<ItemStack> cir) {
		if (!(getBlock() instanceof CwShape)) return;
		ItemStack stack = cir.getReturnValue();
		if (stack == null || !(stack.getItem() instanceof ShapeItem)) return;
		Material m = ShapeEntity.at(world, pos);
		if (m != null) stack.set(CwBlocks.MATERIAL, m);
	}
}
