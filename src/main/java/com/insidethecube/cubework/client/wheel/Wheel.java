package com.insidethecube.cubework.client.wheel;

import java.util.List;
import net.minecraft.world.item.ItemStack;

/**
 * What the wheel shows: pages of choices, one page on the ring at a time, and one choice in the middle
 * (the full block, on a shape wheel). Start is the page it opens on and hot the choice lit when it opens
 * (-1 for none).
 */
public record Wheel(String title, ItemStack icon, Option centre, List<Page> pages, int start, int hot) {
	/** A page: its name, on the tabs at the top and beside the arrows, and its choices round the ring. */
	public record Page(String name, List<Option> options) {}

	/**
	 * One choice. Name is what the middle calls it; label, shorter, goes under its icon on the ring (a line
	 * break splits it in two); word stands in for an icon when there's no item to show. Detail is the line
	 * under the name in the middle. A blocked choice can't be made now, and detail says why.
	 */
	public record Option(String name, String label, ItemStack icon, String word, boolean current, String detail, boolean blocked, Runnable action) {
		public static Option item(String name, String label, ItemStack icon, boolean current, String detail, boolean blocked, Runnable action) {
			return new Option(name, label, icon, null, current, detail, blocked, action);
		}

		public static Option word(String name, String word, String detail, boolean current, Runnable action) {
			return new Option(name, null, ItemStack.EMPTY, word, current, detail, false, action);
		}
	}
}
