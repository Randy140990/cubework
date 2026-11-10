package com.insidethecube.cubework;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** Cubework by Inside the Cube: vanilla blocks in every shape. */
public final class Cubework implements ModInitializer {
	public static final String ID = "cubework";

	/** The Inside the Cube block, only used as the first tab's icon. */
	public static Item CUBE;

	@Override
	public void onInitialize() {
		CwBlocks.register();
		Extras.register();
		RegistryKey<Item> cube = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(ID, "cube"));
		CUBE = Registry.register(Registries.ITEM, cube, new Item(new Item.Settings().registryKey(cube)));

		// Six tabs, so there's little paging: coloured blocks, building blocks, natural blocks, plants, doors and tables.
		tab("colours", () -> new ItemStack(CUBE), add -> {
			for (Material m : Material.ALL) if (!m.nature() && !m.building()) shapes(m, add, false);
		});
		tab("building", () -> new ItemStack(item("stone_brick_slope")), add -> {
			for (String id : Material.BUILDING) shapes(Material.byId(id), add, false);
		});
		tab("nature", () -> new ItemStack(item("grass_block_slope")), add -> {
			for (Material m : Material.ALL) if (m.nature()) shapes(m, add, true);
			Extras.THIN_LOGS.forEach(add);
		});
		tab("plants", () -> new ItemStack(item("fern_175")), add -> {
			Extras.SIZED_PLANTS.forEach(add);
			Extras.CORAL.forEach(add);
		});
		tab("doors", () -> new ItemStack(item("light_blue_mangrove_door")), add -> Extras.DOORS.forEach(add));
		tab("tables", () -> new ItemStack(item("cyan_glazed_terracotta_table")), add -> Extras.TABLES.forEach(add));
	}

	/** One material's shapes, always in the same order (in the Nature tab, only the land-shaping ones). */
	private static void shapes(Material m, Consumer<Block> add, boolean natureOnly) {
		Map<Shape, Block> shapes = CwBlocks.shapesOf(m);
		for (Shape s : Shape.values()) {
			Block b = shapes.get(s);
			if (b != null && (!natureOnly || s.natureShape())) add.accept(b);
		}
	}

	private static void tab(String name, Supplier<ItemStack> icon, Consumer<Consumer<Block>> fill) {
		ItemGroup group = FabricItemGroup.builder()
				.displayName(Text.translatable("itemGroup." + ID + "." + name))
				.icon(icon)
				.entries((context, entries) -> fill.accept(entries::add))
				.build();
		Registry.register(Registries.ITEM_GROUP, RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier.of(ID, name)), group);
	}

	private static Item item(String id) {
		return Registries.ITEM.get(Identifier.of(ID, id));
	}

}
