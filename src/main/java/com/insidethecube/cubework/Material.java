package com.insidethecube.cubework;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/**
 * A vanilla block that Cubework makes shapes of. Its shapes copy its properties (sound, hardness, tool,
 * light) and use its own textures, so a texture pack that changes the block changes them too.
 */
public record Material(String id, Kind kind, String colour) {

	public enum Kind {
		WOOL, TERRACOTTA, CONCRETE, POWDER, GLAZED, GLASS, NATURE;
	}

	/** The 16 dye colours, in the order the game's creative menu uses. */
	public static final String[] COLOURS = {"white", "light_gray", "gray", "black", "brown", "red", "orange", "yellow", "lime", "green", "cyan",
			"light_blue", "blue", "purple", "magenta", "pink"};

	/** The natural blocks, in John's order. */
	public static final String[] NATURE = {"grass_block", "podzol", "mycelium", "dirt_path", "dirt", "coarse_dirt", "rooted_dirt", "farmland",
			"mud", "clay", "gravel", "sand", "sandstone", "red_sand", "red_sandstone", "ice", "packed_ice", "blue_ice", "snow_block", "moss_block",
			"pale_moss_block", "stone", "deepslate", "granite", "diorite", "andesite", "calcite", "tuff", "dripstone_block", "prismarine",
			"magma_block", "obsidian", "crying_obsidian", "netherrack", "crimson_nylium", "warped_nylium", "soul_sand", "soul_soil", "bone_block",
			"blackstone", "basalt", "smooth_basalt", "end_stone", "coal_ore", "deepslate_coal_ore", "iron_ore", "deepslate_iron_ore", "copper_ore",
			"deepslate_copper_ore", "gold_ore", "deepslate_gold_ore", "redstone_ore", "deepslate_redstone_ore", "emerald_ore", "deepslate_emerald_ore",
			"lapis_ore", "deepslate_lapis_ore", "diamond_ore", "deepslate_diamond_ore", "nether_gold_ore", "nether_quartz_ore", "ancient_debris",
			"raw_iron_block", "raw_copper_block", "raw_gold_block", "glowstone", "amethyst_block", "budding_amethyst"};

	/** Textures of a block: top, sides, bottom, and (grass) the tinted overlay on the sides. */
	public record Textures(String top, String side, String bottom, String overlay) {
		static Textures all(String t) {
			return new Textures(t, t, t, null);
		}

		static Textures column(String end, String side) {
			return new Textures(end, side, end, null);
		}
	}

	/** Blocks whose faces differ; everything else uses one texture named like the block. */
	private static final Map<String, Textures> TEXTURES = Map.ofEntries(
			Map.entry("grass_block", new Textures("grass_block_top", "grass_block_side", "dirt", "grass_block_side_overlay")),
			Map.entry("podzol", new Textures("podzol_top", "podzol_side", "dirt", null)),
			Map.entry("mycelium", new Textures("mycelium_top", "mycelium_side", "dirt", null)),
			Map.entry("dirt_path", new Textures("dirt_path_top", "dirt_path_side", "dirt", null)),
			Map.entry("farmland", new Textures("farmland", "dirt", "dirt", null)),
			Map.entry("sandstone", new Textures("sandstone_top", "sandstone", "sandstone_bottom", null)),
			Map.entry("red_sandstone", new Textures("red_sandstone_top", "red_sandstone", "red_sandstone_bottom", null)),
			Map.entry("crimson_nylium", new Textures("crimson_nylium", "crimson_nylium_side", "netherrack", null)),
			Map.entry("warped_nylium", new Textures("warped_nylium", "warped_nylium_side", "netherrack", null)),
			Map.entry("basalt", Textures.column("basalt_top", "basalt_side")),
			Map.entry("bone_block", Textures.column("bone_block_top", "bone_block_side")),
			Map.entry("deepslate", Textures.column("deepslate_top", "deepslate")),
			Map.entry("blackstone", Textures.column("blackstone_top", "blackstone")),
			Map.entry("ancient_debris", Textures.column("ancient_debris_top", "ancient_debris_side")),
			Map.entry("snow_block", Textures.all("snow")),
			Map.entry("magma_block", Textures.all("magma")));

	public static final List<Material> ALL = build();

	private static List<Material> build() {
		List<Material> out = new ArrayList<>();
		for (String c : COLOURS) out.add(new Material(c + "_wool", Kind.WOOL, c));
		out.add(new Material("terracotta", Kind.TERRACOTTA, null));
		for (String c : COLOURS) out.add(new Material(c + "_terracotta", Kind.TERRACOTTA, c));
		for (String c : COLOURS) out.add(new Material(c + "_concrete", Kind.CONCRETE, c));
		for (String c : COLOURS) out.add(new Material(c + "_concrete_powder", Kind.POWDER, c));
		for (String c : COLOURS) out.add(new Material(c + "_glazed_terracotta", Kind.GLAZED, c));
		out.add(new Material("glass", Kind.GLASS, null));
		out.add(new Material("tinted_glass", Kind.GLASS, null));
		for (String c : COLOURS) out.add(new Material(c + "_stained_glass", Kind.GLASS, c));
		for (String n : NATURE) out.add(new Material(n, Kind.NATURE, null));
		return List.copyOf(out);
	}

	/** The vanilla block. */
	public Block base() {
		return Registries.BLOCK.get(Identifier.ofVanilla(id));
	}

	public boolean glass() {
		return kind == Kind.GLASS;
	}

	public boolean nature() {
		return kind == Kind.NATURE;
	}

	/** Drawn see-through: glass and ice. */
	public boolean seeThrough() {
		return kind == Kind.GLASS || id.equals("ice");
	}

	/** Grass: its top and side overlay take the biome's grass colour. */
	public boolean grassTinted() {
		return id.equals("grass_block");
	}

	/** How tall the full block is, in pixels: dirt path and farmland are 15. */
	public int height() {
		return id.equals("dirt_path") || id.equals("farmland") ? 15 : 16;
	}

	/** The block's textures (paths under minecraft:block/). */
	public Textures textures() {
		Textures t = TEXTURES.get(id);
		return t != null ? t : Textures.all(id);
	}

	/** For concrete powder: the concrete of the same colour, which its shapes turn into when wet. */
	public Material hardened() {
		if (kind != Kind.POWDER) return null;
		for (Material m : ALL) if (m.kind == Kind.CONCRETE && m.colour.equals(colour)) return m;
		return null;
	}

	/** The block's id for one of this material's shapes, for example red_wool_vertical_slab. */
	public String idOf(Shape shape) {
		return id + "_" + shape.suffix;
	}
}
