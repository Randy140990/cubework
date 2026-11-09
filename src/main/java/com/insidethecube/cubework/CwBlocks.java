package com.insidethecube.cubework;

import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import com.insidethecube.cubework.block.CwButton;
import com.insidethecube.cubework.block.CwCarpet;
import com.insidethecube.cubework.block.CwFence;
import com.insidethecube.cubework.block.CwFenceGate;
import com.insidethecube.cubework.block.CwPressurePlate;
import com.insidethecube.cubework.block.CwSlab;
import com.insidethecube.cubework.block.CwStairs;
import com.insidethecube.cubework.block.CwTrapdoor;
import com.insidethecube.cubework.block.CwWall;
import com.insidethecube.cubework.block.LayerBlock;
import com.insidethecube.cubework.block.PanelBlock;
import com.insidethecube.cubework.block.PostBlock;
import com.insidethecube.cubework.block.SlopeBlock;
import com.insidethecube.cubework.block.VerticalLayerBlock;
import com.insidethecube.cubework.block.VerticalSlabBlock;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSetType;
import net.minecraft.block.WoodType;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

/** Every Cubework block, made from the material and shape tables. */
public final class CwBlocks {
	private CwBlocks() {}

	/** Which material and shape a Cubework block is. */
	public record Info(Material material, Shape shape) {}

	private static final Map<Material, Map<Shape, Block>> BY_MATERIAL = new LinkedHashMap<>();
	private static final Map<Block, Info> INFO = new IdentityHashMap<>();
	/** The vanilla blocks, back to their material (for glass faces that meet). */
	private static final Map<Block, Material> BASES = new IdentityHashMap<>();

	public static void register() {
		for (Material m : Material.ALL) {
			Block base = m.base();
			BASES.put(base, m);
			Map<Shape, Block> shapes = new EnumMap<>(Shape.class);
			for (Shape s : Shape.values()) {
				if (!s.appliesTo(m)) continue;
				Block b = block(m.idOf(s), settings(base, s), factory(m, s, base));
				shapes.put(s, b);
				INFO.put(b, new Info(m, s));
				item(m.idOf(s), b);
			}
			BY_MATERIAL.put(m, shapes);
		}
	}

	private static AbstractBlock.Settings settings(Block base, Shape s) {
		AbstractBlock.Settings set = AbstractBlock.Settings.copy(base);
		return switch (s) {
			case BUTTON, PRESSURE_PLATE -> set.noCollision().pistonBehavior(PistonBehavior.DESTROY);
			case TRAPDOOR -> set.nonOpaque().allowsSpawning((state, world, pos, type) -> false);
			case WALL, FENCE, FENCE_GATE -> set.solid();
			default -> set;
		};
	}

	private static Function<AbstractBlock.Settings, Block> factory(Material m, Shape s, Block base) {
		return switch (s) {
			case STAIRS -> set -> new CwStairs(m, base.getDefaultState(), set);
			case SLAB -> set -> new CwSlab(m, set);
			case VERTICAL_SLAB -> set -> new VerticalSlabBlock(m, set);
			case SLOPE -> set -> new SlopeBlock(m, set);
			case LAYER -> set -> new LayerBlock(m, set);
			case VERTICAL_LAYER -> set -> new VerticalLayerBlock(m, set);
			case CARPET -> set -> new CwCarpet(m, set);
			case PANEL -> set -> new PanelBlock(m, set);
			case POST -> set -> new PostBlock(m, set);
			case WALL -> set -> new CwWall(m, set);
			case FENCE -> set -> new CwFence(m, set);
			case FENCE_GATE -> set -> new CwFenceGate(m, WoodType.OAK, set);
			case TRAPDOOR -> set -> new CwTrapdoor(m, BlockSetType.STONE, set);
			case BUTTON -> set -> new CwButton(m, BlockSetType.STONE, 20, set);
			case PRESSURE_PLATE -> set -> new CwPressurePlate(m, BlockSetType.STONE, set);
		};
	}

	private static Block block(String name, AbstractBlock.Settings settings, Function<AbstractBlock.Settings, Block> factory) {
		RegistryKey<Block> key = RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(Cubework.ID, name));
		return Registry.register(Registries.BLOCK, key, factory.apply(settings.registryKey(key)));
	}

	private static void item(String name, Block block) {
		RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Cubework.ID, name));
		Registry.register(Registries.ITEM, key, new BlockItem(block, new Item.Settings().registryKey(key).useBlockPrefixedTranslationKey()));
	}

	public static Block get(Material m, Shape s) {
		Map<Shape, Block> shapes = BY_MATERIAL.get(m);
		return shapes == null ? null : shapes.get(s);
	}

	public static Map<Shape, Block> shapesOf(Material m) {
		return BY_MATERIAL.get(m);
	}

	/** The Cubework block's material and shape, or null for any other block. */
	public static Info info(Block b) {
		return INFO.get(b);
	}

	/** The material of a Cubework shape or of the vanilla block it's made from, or null. */
	public static Material materialOf(Block b) {
		Info i = INFO.get(b);
		return i != null ? i.material() : BASES.get(b);
	}

	public static Iterable<Block> all() {
		return INFO.keySet();
	}
}
