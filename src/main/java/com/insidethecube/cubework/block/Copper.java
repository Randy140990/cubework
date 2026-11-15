package com.insidethecube.cubework.block;

import com.insidethecube.cubework.Behaviour;
import com.insidethecube.cubework.Material;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * Copper shapes take wax from honeycomb, and an axe takes the wax off or scrapes a weathered one back an
 * age, as with vanilla copper: the shape's material changes.
 */
public final class Copper {
	private Copper() {}

	public static void register() {
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (player.isSpectator()) return InteractionResult.PASS;
			BlockPos pos = hit.getBlockPos();
			BlockState state = level.getBlockState(pos);
			if (!(state.getBlock() instanceof CwShape shape) || shape.behaviour() != Behaviour.COPPER) return InteractionResult.PASS;
			if (!(level.getBlockEntity(pos) instanceof ShapeEntity entity) || entity.material() == null) return InteractionResult.PASS;
			ItemStack stack = player.getItemInHand(hand);
			Material m = entity.material(), to = null;
			int event = 0;
			SoundEvent sound = null;
			boolean axe = stack.is(ItemTags.AXES);
			if (stack.is(Items.HONEYCOMB) && m.waxed() != null) {
				to = m.waxed();
				event = 3003;
			} else if (axe && m.unwaxed() != null) {
				to = m.unwaxed();
				event = 3004;
				sound = SoundEvents.AXE_WAX_OFF;
			} else if (axe && m.previousAge() != null) {
				to = m.previousAge();
				event = 3005;
				sound = SoundEvents.AXE_SCRAPE;
			}
			if (to == null) return InteractionResult.PASS;
			if (sound != null) level.playSound(player, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
			level.levelEvent(player, event, pos, 0);
			if (!level.isClientSide()) {
				entity.setMaterial(to);
				level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, state));
				if (axe) stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
				else stack.consume(1, player);
			}
			return InteractionResult.SUCCESS;
		});
	}
}
