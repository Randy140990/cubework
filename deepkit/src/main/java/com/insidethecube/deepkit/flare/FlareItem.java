package com.insidethecube.deepkit.flare;

import java.util.function.Consumer;
import com.insidethecube.deepkit.DK;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/** An unlit flare: use it to strike it. The lit flare goes in your hand; the rest of the stack back in your bag. */
public class FlareItem extends Item {
	/** How long a flare burns, in ticks. */
	public static final int BURN = 60 * 20;

	public final FlareColour colour;

	public FlareItem(FlareColour colour, Item.Properties props) {
		super(props);
		this.colour = colour;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level instanceof ServerLevel server) {
			ItemStack held = player.getItemInHand(hand);
			ItemStack lit = new ItemStack(DK.LIT_FLARES[colour.ordinal()]);
			lit.set(DK.BURN_UNTIL, server.getGameTime() + BURN);
			ItemStack rest = held.getCount() > 1 ? held.copyWithCount(held.getCount() - 1) : ItemStack.EMPTY;
			if (player.hasInfiniteMaterials()) rest = held.copy();
			player.setItemInHand(hand, lit);
			if (!rest.isEmpty()) player.getInventory().placeItemBackInInventory(rest);
			level.playSound(null, player.blockPosition(), SoundEvents.FLINTANDSTEEL_USE, SoundSource.PLAYERS, 1f, 0.8f);
			level.playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.5f, 1.4f);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.literal("Use to strike; burns for 60 seconds").withStyle(ChatFormatting.GRAY));
	}
}
