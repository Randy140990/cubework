package com.insidethecube.orbitarena.block;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.insidethecube.orbitarena.OrbitArena;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * The arena's own blocks, made for The Bridge (and free to build with: they're in their own creative tab):
 * <ul>
 * <li>light strips, green and red (and white): a glowing white core edged with a line of colour, joined up
 * with the strips beside them, so any run or panel of them reads as one light with the coloured line round its
 * outside only;</li>
 * <li>bridge wool, blue and red: what the teams build with (soft like wool, but quick to mine with a pickaxe,
 * as the bridge servers' clay is);</li>
 * <li>force fields, blue and red: a glowing see-through wash fading out upwards, rising round each goal (you
 * can walk through, but not build into it); and a pale one, the wall in front of the stands, that stops you.</li>
 * </ul>
 */
public final class ArenaBlocks {
	private ArenaBlocks() {}

	public static final List<Block> ALL = new ArrayList<>();
	/** The blocks drawn see-through (the client puts them on the translucent layer). */
	public static final List<Block> TRANSLUCENT = new ArrayList<>();

	public static Block LIGHT_GREEN, LIGHT_RED, LIGHT_WHITE, WOOL_BLUE, WOOL_RED, FIELD_BLUE, FIELD_RED, FIELD_WALL;

	private static BlockBehaviour.Properties light(MapColor colour) {
		return BlockBehaviour.Properties.of().mapColor(colour).strength(0.3F).sound(SoundType.GLASS).lightLevel(s -> 15).emissiveRendering((s, g, p) -> true);
	}

	public static void register() {
		LIGHT_GREEN = block("light_strip_green", light(MapColor.COLOR_LIGHT_GREEN), LightStripBlock::new);
		LIGHT_RED = block("light_strip_red", light(MapColor.COLOR_RED), LightStripBlock::new);
		LIGHT_WHITE = block("light_strip_white", light(MapColor.SNOW), LightStripBlock::new);
		WOOL_BLUE = block("bridge_wool_blue", BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).strength(0.8F).sound(SoundType.WOOL), Block::new);
		WOOL_RED = block("bridge_wool_red", BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(0.8F).sound(SoundType.WOOL), Block::new);
		FIELD_BLUE = field("force_field_blue", MapColor.COLOR_BLUE, false);
		FIELD_RED = field("force_field_red", MapColor.COLOR_RED, false);
		FIELD_WALL = field("force_field_wall", MapColor.COLOR_LIGHT_BLUE, true);

		CreativeModeTab tab = FabricItemGroup.builder()
				.title(Component.translatable("itemGroup.orbitarena.blocks"))
				.icon(() -> new ItemStack(LIGHT_GREEN))
				.displayItems((ctx, entries) -> {
					for (Block b : ALL) entries.accept(new ItemStack(b));
				})
				.build();
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, ResourceKey.create(Registries.CREATIVE_MODE_TAB, OrbitArena.id("blocks")), tab);
	}

	private static Block field(String name, MapColor colour, boolean solid) {
		// Unbreakable (but for creative), as the map is.
		Block b = block(name, BlockBehaviour.Properties.of().mapColor(colour).strength(-1.0F, 3600000.0F).sound(SoundType.GLASS).noOcclusion()
				.lightLevel(s -> 7).emissiveRendering((s, g, p) -> true).isValidSpawn((s, g, p, t) -> false).isRedstoneConductor((s, g, p) -> false)
				.isSuffocating((s, g, p) -> false).isViewBlocking((s, g, p) -> false), p -> new ForceFieldBlock(p, solid));
		TRANSLUCENT.add(b);
		return b;
	}

	private static Block block(String name, BlockBehaviour.Properties props, Function<BlockBehaviour.Properties, Block> factory) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, OrbitArena.id(name));
		Block b = Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(props.setId(key)));
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, OrbitArena.id(name));
		Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(b, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
		ALL.add(b);
		return b;
	}
}
