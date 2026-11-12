package com.insidethecube.cubework;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/** Cubework by Inside the Cube: vanilla blocks in every shape. */
public final class Cubework implements ModInitializer {
	public static final String ID = "cubework";

	/** The Inside the Cube block, only used as the first tab's icon. */
	public static Item CUBE;

	@Override
	public void onInitialize() {
		CwBlocks.register();
		Extras.register();
		CUBE = Reg.item("cube", Item::new);

		// Seven tabs: coloured blocks, building blocks, natural blocks, plants, doors, tables and patterns.
		tab("colours", () -> new ItemStack(CUBE), add -> {
			for (Material m : Material.ALL) if (!m.nature() && !m.building() && !m.patterned()) shapes(m, add, false);
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
		tab("patterns", () -> new ItemStack(item("herringbone_stone_bricks")), add -> {
			for (Material m : Material.ALL) {
				if (!m.patterned() || CwBlocks.shapesOf(m).isEmpty()) continue;
				add.accept(m.base());
				shapes(m, add, false);
			}
		});
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
		CreativeModeTab group = FabricItemGroup.builder()
				.title(Component.translatable("itemGroup." + ID + "." + name))
				.icon(icon)
				.displayItems((context, entries) -> fill.accept(entries::accept))
				.build();
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, ResourceKey.create(Registries.CREATIVE_MODE_TAB, Reg.id(name)), group);
	}

	private static Item item(String id) {
		return Reg.item(Reg.id(id));
	}

}
