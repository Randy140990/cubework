package com.insidethecube.cubework.tool;

import java.util.function.Consumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** A tool's settings, kept on the item. */
public final class ToolData {
	private ToolData() {}

	public static CompoundTag get(ItemStack stack) {
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		return data == null ? new CompoundTag() : data.copyTag();
	}

	public static int getInt(ItemStack stack, String key, int fallback) {
		return get(stack).getIntOr(key, fallback);
	}

	public static void update(ItemStack stack, Consumer<CompoundTag> change) {
		CustomData.update(DataComponents.CUSTOM_DATA, stack, change);
	}
}
