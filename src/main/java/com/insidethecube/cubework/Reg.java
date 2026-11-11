package com.insidethecube.cubework;

import java.util.function.BiFunction;
import java.util.function.Function;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Every registry lookup and registration goes through here, because they changed between Minecraft
 * versions (1.21.2 added registry keys to block and item settings and renamed the lookups).
 */
public final class Reg {
	private Reg() {}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(Cubework.ID, path);
	}

	public static Identifier vanillaId(String path) {
		return Identifier.withDefaultNamespace(path);
	}

	/** A vanilla block (air if this Minecraft version doesn't have it). */
	public static Block vanilla(String path) {
		return block(vanillaId(path));
	}

	/** Whether this Minecraft version has the vanilla block (pale oak and resin came in 1.21.4, bushes in 1.21.5). */
	public static boolean exists(String path) {
		return BuiltInRegistries.BLOCK.containsKey(vanillaId(path));
	}

	public static Block block(Identifier id) {
		//#if MC >= 1.21.2
		return BuiltInRegistries.BLOCK.getValue(id);
		//#else
		return BuiltInRegistries.BLOCK.get(id);
		//#endif
	}

	public static Item item(Identifier id) {
		//#if MC >= 1.21.2
		return BuiltInRegistries.ITEM.getValue(id);
		//#else
		return BuiltInRegistries.ITEM.get(id);
		//#endif
	}

	public static Identifier idOf(Block block) {
		return BuiltInRegistries.BLOCK.getKey(block);
	}

	/** Registers a Cubework block made from its settings. */
	public static Block block(String name, BlockBehaviour.Properties settings, Function<BlockBehaviour.Properties, Block> factory) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id(name));
		//#if MC >= 1.21.2
		settings = settings.setId(key);
		//#endif
		return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(settings));
	}

	/** Registers the item that places a block (a BlockItem, or another kind such as the door's two-high item). */
	public static Item blockItem(String name, Block block, BiFunction<Block, Item.Properties, Item> factory) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id(name));
		Item.Properties settings = new Item.Properties();
		//#if MC >= 1.21.2
		settings = settings.setId(key).useBlockDescriptionPrefix();
		//#endif
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(block, settings));
	}

	public static Item blockItem(String name, Block block) {
		return blockItem(name, block, BlockItem::new);
	}

	/** Registers a plain item. */
	public static Item item(String name, Function<Item.Properties, Item> factory) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id(name));
		Item.Properties settings = new Item.Properties();
		//#if MC >= 1.21.2
		settings = settings.setId(key);
		//#endif
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(settings));
	}
}
