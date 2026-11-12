package com.insidethecube.cubework.client.wheel;

import java.util.List;
import net.minecraft.world.item.ItemStack;

/**
 * What the wheel shows: groups on the inner ring, each with its options on the outer ring, and one choice
 * in the middle. Options and groups show an item, or a short word when there's no item for them.
 */
public record Wheel(String title, ItemStack icon, Option centre, List<Group> groups, int start) {
	public record Group(String name, ItemStack icon, String word, List<Option> options) {}

	public record Option(String name, ItemStack icon, String word, boolean current, String note, Runnable action) {
		public static Option item(String name, ItemStack icon, boolean current, String note, Runnable action) {
			return new Option(name, icon, null, current, note, action);
		}

		public static Option word(String name, String word, boolean current, Runnable action) {
			return new Option(name, ItemStack.EMPTY, word, current, null, action);
		}
	}
}
