package com.insidethecube.cubework.tool;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;

/**
 * The terrain brush: use it on the ground and every one-block step within its circle becomes a Cubework
 * slope of the ground's own block. Fill adds slopes on the low side of each step; carve cuts the top edge
 * down instead. Settings and undo are on the shape wheel. In survival it only carves.
 */
public class TerrainBrushItem extends Item {
	public static final String MODE = "mode", RADIUS = "radius", STEEP = "steep";
	public static final int FILL = 0, CARVE = 1;
	/** Steep: every step is a 1 in 1 slope. Natural: steps spaced 2 to 4 apart get the long slopes. Gentle: long slopes wherever they fit. */
	public static final int STEEP_ONLY = 0, NATURAL = 1, GENTLE = 2;
	public static final int[] RADII = {3, 5, 8, 12, 16};
	public static final String[] MODE_NAMES = {"Fill", "Carve"}, STEEP_NAMES = {"Steep", "Natural", "Gentle"};

	public TerrainBrushItem(Item.Properties properties) {
		super(properties);
	}

	public static int mode(ItemStack stack) {
		return Math.floorMod(ToolData.getInt(stack, MODE, FILL), 2);
	}

	public static int radius(ItemStack stack) {
		return Math.max(1, Math.min(16, ToolData.getInt(stack, RADIUS, 5)));
	}

	public static int steep(ItemStack stack) {
		return Math.floorMod(ToolData.getInt(stack, STEEP, NATURAL), 3);
	}

	@Override
	public InteractionResult useOn(UseOnContext ctx) {
		if (!(ctx.getLevel() instanceof ServerLevel level) || !(ctx.getPlayer() instanceof ServerPlayer player)) return InteractionResult.SUCCESS;
		ItemStack stack = ctx.getItemInHand();
		int mode = player.hasInfiniteMaterials() ? mode(stack) : CARVE;
		int changed = Terrain.smooth(level, ctx.getClickedPos(), radius(stack), mode, steep(stack), player);
		player.displayClientMessage(Component.literal(changed == 0 ? "Nothing to smooth here." : "Smoothed " + changed + " blocks. Undo is on the wheel."),
				true);
		return InteractionResult.SUCCESS_SERVER;
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.literal(MODE_NAMES[mode(stack)] + ", radius " + radius(stack) + ", " + STEEP_NAMES[steep(stack)].toLowerCase() + " slopes")
				.withStyle(net.minecraft.ChatFormatting.GRAY));
		tooltip.accept(Component.literal("Right-click the ground to smooth it; hold ").append(Component.keybind("key.cubework.wheel"))
				.append(" for size, mode and undo").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
	}
}
