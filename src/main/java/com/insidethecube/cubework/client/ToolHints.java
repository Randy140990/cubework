package com.insidethecube.cubework.client;

import com.insidethecube.cubework.tool.BuildersGuideItem;
import com.insidethecube.cubework.tool.GuideShapes;
import com.insidethecube.cubework.tool.TerrainBrushItem;
import com.insidethecube.cubework.tool.ToolData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** When you take the terrain brush or builder's guide in hand, a line above the hotbar says how to use it. */
final class ToolHints {
	private ToolHints() {}

	private static Item lastItem;
	private static int lastSlot = -1;

	static void tick(Minecraft mc) {
		if (mc.player == null) {
			lastItem = null;
			return;
		}
		ItemStack held = mc.player.getMainHandItem();
		int slot = mc.player.getInventory().getSelectedSlot();
		if (held.getItem() == lastItem && slot == lastSlot) return;
		lastItem = held.getItem();
		lastSlot = slot;
		String key = CubeworkClient.WHEEL.getTranslatedKeyMessage().getString();
		String hint = null;
		if (held.getItem() instanceof BuildersGuideItem) {
			boolean set = ToolData.get(held).getBooleanOr("aset", false);
			hint = set ? GuideShapes.describe(ToolData.get(held)) + ". Right-click a block to move it; hold " + key + " for shape and size."
					: "The white outline follows where you look. Right-click a block to place it there; hold " + key + " for shape and size.";
		} else if (held.getItem() instanceof TerrainBrushItem) {
			hint = "Right-click the ground to smooth it; hold " + key + " for size, mode and undo.";
		}
		if (hint != null) mc.player.displayClientMessage(Component.literal(hint), true);
	}
}
