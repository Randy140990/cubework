package com.insidethecube.cubework.tool;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** The server's side of the tools' wheel settings and actions. */
public final class ToolActions {
	private ToolActions() {}

	public static void setting(ServerPlayer player, String key, int value) {
		ItemStack held = player.getMainHandItem();
		if (held.getItem() instanceof TerrainBrushItem) {
			switch (key) {
				case TerrainBrushItem.MODE -> ToolData.update(held, t -> t.putInt(key, Math.floorMod(value, 2)));
				case TerrainBrushItem.RADIUS -> ToolData.update(held, t -> t.putInt(key, Math.max(1, Math.min(16, value))));
				case TerrainBrushItem.STEEP -> ToolData.update(held, t -> t.putInt(key, Math.floorMod(value, 3)));
				default -> {
				}
			}
			return;
		}
		if (held.getItem() instanceof BuildersGuideItem) {
			switch (key) {
				case GuideShapes.SHAPE -> ToolData.update(held, t -> t.putInt(key, Math.floorMod(value, GuideShapes.NAMES.length)));
				case GuideShapes.SIZE, GuideShapes.SIZE2 -> ToolData.update(held, t -> t.putInt(key, Math.max(1, Math.min(96, value))));
				case GuideShapes.SIDES -> ToolData.update(held, t -> t.putInt(key, Math.max(3, Math.min(12, value))));
				case GuideShapes.PIN -> ToolData.update(held, t -> t.putBoolean(key, value != 0));
				default -> {
					return;
				}
			}
			int n = GuideShapes.blocks(ToolData.get(held)).size();
			player.displayClientMessage(Component.literal(GuideShapes.describe(ToolData.get(held)) + (n > 0 ? ": " + n + " blocks" : "")), true);
		}
	}

	public static void action(ServerPlayer player, String action) {
		ItemStack held = player.getMainHandItem();
		switch (action) {
			case "undo" -> {
				if (!(held.getItem() instanceof TerrainBrushItem)) return;
				int n = Terrain.undo(player.level(), player);
				player.displayClientMessage(Component.literal(n < 0 ? "Nothing to undo." : "Put back " + n + " blocks."), true);
			}
			case "clear" -> {
				if (!(held.getItem() instanceof BuildersGuideItem)) return;
				ToolData.update(held, t -> {
					t.remove("aset");
					t.remove("bset");
				});
				player.displayClientMessage(Component.literal("Guide cleared."), true);
			}
			default -> {
			}
		}
	}
}
