package com.insidethecube.cubework;

import java.util.Map;

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
		RegistryKey<Item> cube = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(ID, "cube"));
		CUBE = Registry.register(Registries.ITEM, cube, new Item(new Item.Settings().registryKey(cube)));
		tabs();
	}

	/** One tab per kind of material; each colour's shapes sit together, always in the same order. */
	private static void tabs() {
		for (Material.Kind kind : Material.Kind.values()) {
			ItemGroup group = FabricItemGroup.builder()
					.displayName(Text.translatable("itemGroup." + ID + "." + kind.tab))
					.icon(() -> new ItemStack(icon(kind)))
					.entries((context, entries) -> {
						for (Material m : Material.ALL) {
							if (m.kind() != kind) continue;
							Map<Shape, Block> shapes = CwBlocks.shapesOf(m);
							for (Shape s : Shape.values()) {
								Block b = shapes.get(s);
								if (b != null) entries.add(b);
							}
						}
					})
					.build();
			Registry.register(Registries.ITEM_GROUP, RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier.of(ID, kind.tab)), group);
		}
	}

	/** The first tab shows the cube; the others show one of their own shapes, so they're easy to tell apart. */
	private static Item icon(Material.Kind kind) {
		String id = switch (kind) {
			case WOOL -> null;
			case TERRACOTTA -> "orange_terracotta_stairs";
			case CONCRETE -> "light_blue_concrete_slope";
			case POWDER -> "pink_concrete_powder_vertical_slab";
			case GLAZED -> "cyan_glazed_terracotta_stairs";
			case GLASS -> "light_blue_stained_glass_slope";
		};
		return id == null ? CUBE : Registries.ITEM.get(Identifier.of(ID, id));
	}
}
