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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * A lamp item. The hand torch is used in either hand (use switches it on and off). The headlamp and chest
 * lamp are worn like a helmet or chestplate (use puts them on) and switched with the lamp key (H); to keep
 * your armour, fit one onto a helmet or chestplate in a crafting grid instead. A lamp that's on shimmers.
 */
public class LampItem extends Item {
	public final LampKind kind;

	public LampItem(LampKind kind, Item.Properties props) {
		super(worn(props.stacksTo(1).component(DK.LAMP, LampData.full(kind)), kind));
		this.kind = kind;
	}

	/** The headlamp goes in the helmet slot and the chest lamp in the chestplate slot. */
	private static Item.Properties worn(Item.Properties props, LampKind kind) {
		return switch (kind) {
			case HEAD -> props.equippable(EquipmentSlot.HEAD);
			case CHEST -> props.equippable(EquipmentSlot.CHEST);
			default -> props;
		};
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (kind != LampKind.TORCH) {
			// Put it on, like a helmet or chestplate.
			InteractionResult worn = super.use(level, player, hand);
			if (!level.isClientSide() && worn.consumesAction()) {
				player.displayClientMessage(Component.literal("Wearing the " + kind.title.toLowerCase() + ". Press ").append(Component.keybind("key.deepkit.lamps"))
						.append(" to switch it on and off."), true);
			}
			return worn;
		}
		if (level instanceof ServerLevel) {
			LampData d = stack.getOrDefault(DK.LAMP, LampData.full(kind));
			if (d.charge() <= 0) {
				player.displayClientMessage(Component.literal("The battery is flat. Charge it with glowstone dust or redstone in a crafting grid."), true);
			} else {
				stack.set(DK.LAMP, d.withOn(!d.on()));
				level.playSound(null, player.blockPosition(), SoundEvents.LEVER_CLICK, SoundSource.PLAYERS, 0.4f, d.on() ? 0.8f : 1.2f);
				player.displayClientMessage(Component.literal(kind.title + (d.on() ? " off" : " on, battery " + d.percent() + "%")), true);
			}
		}
		return InteractionResult.SUCCESS;
	}

	/** A lamp that's on shimmers, so you can see it's on in daylight too. */
	@Override
	public boolean isFoil(ItemStack stack) {
		LampData d = stack.get(DK.LAMP);
		return d != null && d.on();
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
		if (kind == LampKind.TORCH) {
			tooltip.accept(Component.literal("Right-click to switch on and off; works in either hand").withStyle(ChatFormatting.DARK_GRAY));
		} else {
			String armour = kind == LampKind.HEAD ? "helmet" : "chestplate";
			tooltip.accept(Component.literal("Right-click to wear it, then press ").append(Component.keybind("key.deepkit.lamps"))
					.append(" to switch it on").withStyle(ChatFormatting.DARK_GRAY));
			tooltip.accept(Component.literal("Or fit it onto a " + armour + " in a crafting grid to keep your armour").withStyle(ChatFormatting.DARK_GRAY));
		}
		tooltip.accept(Component.literal("Charge with glowstone dust (+50%) or redstone (+20%)").withStyle(ChatFormatting.DARK_GRAY));
	}
}
