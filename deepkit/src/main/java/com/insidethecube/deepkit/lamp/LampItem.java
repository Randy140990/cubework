package com.insidethecube.deepkit.lamp;

import java.util.function.Consumer;
import com.insidethecube.deepkit.DK;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * A lamp item. The hand torch is used in either hand (use switches it on and off); the headlamp and chest
 * lamp are fitted to a helmet or chestplate in a crafting grid, then switched with the lamp key (H).
 */
public class LampItem extends Item {
	public final LampKind kind;

	public LampItem(LampKind kind, Item.Properties props) {
		super(props.stacksTo(1).component(DK.LAMP, LampData.full(kind)));
		this.kind = kind;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (kind != LampKind.TORCH) {
			if (!level.isClientSide()) player.displayClientMessage(Component.literal("Fit it onto a "
					+ (kind == LampKind.HEAD ? "helmet" : "chestplate") + " in a crafting grid, then switch it with the lamp key."), true);
			return InteractionResult.PASS;
		}
		if (level instanceof ServerLevel) {
			LampData d = stack.getOrDefault(DK.LAMP, LampData.full(kind));
			if (d.charge() <= 0) {
				player.displayClientMessage(Component.literal("The battery is flat. Charge it with glowstone dust or redstone."), true);
			} else {
				stack.set(DK.LAMP, d.withOn(!d.on()));
				level.playSound(null, player.blockPosition(), SoundEvents.LEVER_CLICK, SoundSource.PLAYERS, 0.4f, d.on() ? 0.8f : 1.2f);
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return true;
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		LampData d = stack.getOrDefault(DK.LAMP, LampData.full(kind));
		return Math.round(13f * d.charge() / kind.battery);
	}

	@Override
	public int getBarColor(ItemStack stack) {
		LampData d = stack.getOrDefault(DK.LAMP, LampData.full(kind));
		return Mth.hsvToRgb(Math.max(0f, (float) d.charge() / kind.battery) / 3f, 1f, 1f);
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		LampData d = stack.getOrDefault(DK.LAMP, LampData.full(kind));
		tooltip.accept(Component.literal("Battery " + d.percent() + "%" + (d.on() ? ", on" : "")).withStyle(ChatFormatting.GRAY));
		tooltip.accept(Component.literal(kind == LampKind.TORCH ? "Use to switch on and off; works in either hand"
				: "Fit onto a " + (kind == LampKind.HEAD ? "helmet" : "chestplate") + " in a crafting grid").withStyle(ChatFormatting.DARK_GRAY));
		tooltip.accept(Component.literal("Charge with glowstone dust (+50%) or redstone (+20%)").withStyle(ChatFormatting.DARK_GRAY));
	}
}
