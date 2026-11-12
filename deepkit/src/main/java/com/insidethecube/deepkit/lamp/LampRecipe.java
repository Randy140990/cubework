package com.insidethecube.deepkit.lamp;

import com.insidethecube.deepkit.DK;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.Level;

/**
 * Lamps in the crafting grid, in any arrangement:
 * - a helmet and a headlamp (or a chestplate and a chest lamp): the armour with the lamp fitted, keeping
 *   its enchantments and wear;
 * - armour with a lamp, on its own: the armour back, and the lamp left in the grid;
 * - a lamp (or armour with one) with glowstone dust (+50% each) or redstone (+20% each): charged.
 */
public class LampRecipe extends CustomRecipe {
	public LampRecipe(CraftingBookCategory category) {
		super(category);
	}

	/** What the grid makes: the result, and where the lamp comes back when one is taken off (-1 for none). */
	private record Plan(ItemStack result, int lampSlot, ItemStack lamp) {}

	private static EquipmentSlot armourSlot(ItemStack s) {
		Equippable e = s.get(DataComponents.EQUIPPABLE);
		if (e == null) return null;
		return e.slot() == EquipmentSlot.HEAD || e.slot() == EquipmentSlot.CHEST ? e.slot() : null;
	}

	private static Plan plan(CraftingInput in) {
		ItemStack armour = ItemStack.EMPTY, lamp = ItemStack.EMPTY;
		int armourAt = -1, glow = 0, red = 0;
		for (int i = 0; i < in.size(); i++) {
			ItemStack s = in.getItem(i);
			if (s.isEmpty()) continue;
			if (s.getItem() instanceof LampItem) {
				if (!lamp.isEmpty()) return null;
				lamp = s;
			} else if (s.is(Items.GLOWSTONE_DUST)) {
				glow++;
			} else if (s.is(Items.REDSTONE)) {
				red++;
			} else if (armourSlot(s) != null) {
				if (!armour.isEmpty()) return null;
				armour = s;
				armourAt = i;
			} else {
				return null;
			}
		}
		boolean fuel = glow + red > 0;
		if (!armour.isEmpty() && !lamp.isEmpty()) {
			// Fit a lamp.
			if (fuel || armour.has(DK.LAMP)) return null;
			LampKind k = ((LampItem) lamp.getItem()).kind;
			EquipmentSlot slot = armourSlot(armour);
			if (!(k == LampKind.HEAD && slot == EquipmentSlot.HEAD || k == LampKind.CHEST && slot == EquipmentSlot.CHEST)) return null;
			ItemStack out = armour.copyWithCount(1);
			out.set(DK.LAMP, lamp.getOrDefault(DK.LAMP, LampData.full(k)).withOn(false));
			return new Plan(out, -1, ItemStack.EMPTY);
		}
		ItemStack target = !lamp.isEmpty() ? lamp : armour;
		if (target.isEmpty() || !target.has(DK.LAMP)) return null;
		LampData d = target.get(DK.LAMP);
		if (fuel) {
			// Charge.
			if (d.charge() >= d.lamp().battery) return null;
			int add = (glow * 50 + red * 20) * d.lamp().battery / 100;
			ItemStack out = target.copyWithCount(1);
			out.set(DK.LAMP, d.withCharge(d.charge() + add));
			return new Plan(out, -1, ItemStack.EMPTY);
		}
		if (!armour.isEmpty()) {
			// Take a lamp off armour.
			ItemStack out = armour.copyWithCount(1);
			out.remove(DK.LAMP);
			ItemStack back = new ItemStack(d.lamp() == LampKind.HEAD ? DK.HEADLAMP : DK.CHEST_LAMP);
			back.set(DK.LAMP, d.withOn(false));
			return new Plan(out, armourAt, back);
		}
		return null;
	}

	@Override
	public boolean matches(CraftingInput in, Level level) {
		return plan(in) != null;
	}

	@Override
	public ItemStack assemble(CraftingInput in, HolderLookup.Provider registries) {
		Plan p = plan(in);
		return p == null ? ItemStack.EMPTY : p.result();
	}

	@Override
	public NonNullList<ItemStack> getRemainingItems(CraftingInput in) {
		NonNullList<ItemStack> out = NonNullList.withSize(in.size(), ItemStack.EMPTY);
		Plan p = plan(in);
		if (p != null && p.lampSlot() >= 0) out.set(p.lampSlot(), p.lamp());
		return out;
	}

	@Override
	public RecipeSerializer<LampRecipe> getSerializer() {
		return DK.LAMP_RECIPE;
	}
}
