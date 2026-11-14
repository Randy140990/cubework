package com.insidethecube.orbitarena.match;

import java.util.Locale;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

/** The kits you can fight with. Everything in them is unbreakable, so fights are about skill, not durability. */
public enum Kit {
	SWORD, AXE, MACE, SPEAR, ARCHER, IRON, NETHERITE;

	public String key() {
		return "orbitarena.kit." + name().toLowerCase(Locale.ROOT);
	}

	/** Puts the kit on the player (their own things are already put away). */
	public void give(ServerPlayer p) {
		switch (this) {
			case SWORD -> {
				armour(p, Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS, 1);
				add(p, ench(p, item(Items.DIAMOND_SWORD), Enchantments.SHARPNESS, 2));
				offhand(p, item(Items.SHIELD));
				food(p, 6);
			}
			case AXE -> {
				armour(p, Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS, 1);
				add(p, ench(p, item(Items.DIAMOND_AXE), Enchantments.SHARPNESS, 1));
				add(p, item(Items.DIAMOND_SWORD));
				offhand(p, item(Items.SHIELD));
				food(p, 6);
			}
			case MACE -> {
				armour(p, Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS, 1);
				ItemStack boots = p.getItemBySlot(EquipmentSlot.FEET);
				ench(p, boots, Enchantments.FEATHER_FALLING, 4);
				add(p, ench(p, ench(p, item(Items.MACE), Enchantments.DENSITY, 2), Enchantments.WIND_BURST, 1));
				add(p, item(Items.DIAMOND_SWORD));
				add(p, new ItemStack(Items.WIND_CHARGE, 32));
				offhand(p, item(Items.SHIELD));
				food(p, 6);
			}
			case SPEAR -> {
				armour(p, Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS, 1);
				add(p, item(Items.DIAMOND_SPEAR));
				add(p, item(Items.DIAMOND_SWORD));
				offhand(p, item(Items.SHIELD));
				food(p, 6);
			}
			case ARCHER -> {
				armour(p, Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS, 1);
				add(p, ench(p, ench(p, item(Items.BOW), Enchantments.POWER, 3), Enchantments.PUNCH, 1));
				add(p, ench(p, item(Items.CROSSBOW), Enchantments.QUICK_CHARGE, 2));
				add(p, item(Items.IRON_SWORD));
				add(p, new ItemStack(Items.ARROW, 64));
				add(p, new ItemStack(Items.ARROW, 64));
				offhand(p, item(Items.SHIELD));
				food(p, 4);
			}
			case IRON -> {
				armour(p, Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS, 0);
				add(p, item(Items.IRON_SWORD));
				add(p, item(Items.IRON_AXE));
				offhand(p, item(Items.SHIELD));
				food(p, 3);
			}
			case NETHERITE -> {
				armour(p, Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS, 4);
				add(p, ench(p, item(Items.NETHERITE_SWORD), Enchantments.SHARPNESS, 5));
				add(p, ench(p, item(Items.NETHERITE_AXE), Enchantments.SHARPNESS, 5));
				add(p, ench(p, ench(p, item(Items.MACE), Enchantments.DENSITY, 3), Enchantments.WIND_BURST, 1));
				add(p, item(Items.NETHERITE_SPEAR));
				add(p, new ItemStack(Items.WIND_CHARGE, 32));
				add(p, new ItemStack(Items.ENDER_PEARL, 16));
				add(p, new ItemStack(Items.TOTEM_OF_UNDYING));
				offhand(p, item(Items.SHIELD));
				food(p, 12);
			}
		}
	}

	private static ItemStack item(Item item) {
		ItemStack s = new ItemStack(item);
		if (s.isDamageableItem()) s.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);
		return s;
	}

	static ItemStack ench(ServerPlayer p, ItemStack s, ResourceKey<Enchantment> key, int level) {
		Holder<Enchantment> e = p.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
		s.enchant(e, level);
		return s;
	}

	private static void armour(ServerPlayer p, Item head, Item chest, Item legs, Item feet, int protection) {
		Item[] items = {head, chest, legs, feet};
		EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
		for (int i = 0; i < 4; i++) {
			ItemStack s = item(items[i]);
			if (protection > 0) ench(p, s, Enchantments.PROTECTION, protection);
			p.setItemSlot(slots[i], s);
		}
	}

	private static void add(ServerPlayer p, ItemStack s) {
		p.getInventory().add(s);
	}

	private static void offhand(ServerPlayer p, ItemStack s) {
		p.setItemSlot(EquipmentSlot.OFFHAND, s);
	}

	private static void food(ServerPlayer p, int apples) {
		add(p, new ItemStack(Items.GOLDEN_APPLE, apples));
		add(p, new ItemStack(Items.COOKED_BEEF, 32));
	}
}
