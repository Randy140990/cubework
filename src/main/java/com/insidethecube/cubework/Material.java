package com.insidethecube.cubework;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

/**
 * A vanilla block that Cubework makes shapes of. Its shapes copy its properties (sound, hardness, tool,
 * light) and use its own textures, so a texture pack that changes the block changes them too.
 */
public record Material(String id, Kind kind, String colour) {

	public enum Kind {
		WOOL, TERRACOTTA, CONCRETE, POWDER, GLAZED, GLASS, NATURE, BUILDING;
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

	/**
	 * The building blocks, in John's order: they get every shape, plus pillars. Stone, deepslate, tuff,
	 * sandstone, prismarine, netherrack, basalt, blackstone and amethyst are natural blocks too (one block,
	 * in both tabs).
	 */
	public static final String[] BUILDING = {
			"stone", "cobblestone", "mossy_cobblestone", "smooth_stone", "stone_bricks", "cracked_stone_bricks", "chiseled_stone_bricks",
			"mossy_stone_bricks", "granite", "polished_granite", "diorite", "polished_diorite", "andesite", "polished_andesite", "deepslate",
			"cobbled_deepslate", "chiseled_deepslate", "polished_deepslate", "deepslate_bricks", "cracked_deepslate_bricks", "deepslate_tiles",
			"cracked_deepslate_tiles", "tuff", "chiseled_tuff", "polished_tuff", "tuff_bricks", "chiseled_tuff_bricks", "bricks", "packed_mud",
			"mud_bricks", "resin_bricks", "chiseled_resin_bricks", "sandstone", "chiseled_sandstone", "smooth_sandstone", "cut_sandstone",
			"red_sandstone", "chiseled_red_sandstone", "smooth_red_sandstone", "cut_red_sandstone", "prismarine", "prismarine_bricks",
			"dark_prismarine", "netherrack", "nether_bricks", "cracked_nether_bricks", "chiseled_nether_bricks", "red_nether_bricks", "basalt",
			"smooth_basalt", "polished_basalt", "blackstone", "gilded_blackstone", "chiseled_polished_blackstone", "polished_blackstone",
			"polished_blackstone_bricks", "cracked_polished_blackstone_bricks", "end_stone_bricks", "purpur_block", "purpur_pillar", "coal_block",
			"iron_block", "gold_block", "redstone_block", "emerald_block", "lapis_block", "diamond_block", "netherite_block", "quartz_block",
			"chiseled_quartz_block", "quartz_bricks", "quartz_pillar", "smooth_quartz", "amethyst_block", "copper_block", "chiseled_copper",
			"cut_copper", "exposed_copper", "exposed_chiseled_copper", "exposed_cut_copper", "weathered_copper", "weathered_chiseled_copper",
			"weathered_cut_copper", "oxidized_copper", "oxidized_chiseled_copper", "oxidized_cut_copper", "waxed_copper_block",
			"waxed_chiseled_copper", "waxed_cut_copper", "waxed_exposed_copper", "waxed_exposed_chiseled_copper", "waxed_exposed_cut_copper",
			"waxed_weathered_copper", "waxed_weathered_chiseled_copper", "waxed_weathered_cut_copper", "waxed_oxidized_copper",
			"waxed_oxidized_chiseled_copper", "waxed_oxidized_cut_copper"};

	private static final java.util.Set<String> BUILDING_SET = java.util.Set.of(BUILDING);

	/** Shorter names for building blocks' shapes: stone_brick_stairs, iron_slab, quartz_wall. */
	private static final Map<String, String> STEMS = Map.ofEntries(
			Map.entry("stone_bricks", "stone_brick"), Map.entry("cracked_stone_bricks", "cracked_stone_brick"),
			Map.entry("chiseled_stone_bricks", "chiseled_stone_brick"), Map.entry("mossy_stone_bricks", "mossy_stone_brick"),
			Map.entry("deepslate_bricks", "deepslate_brick"), Map.entry("cracked_deepslate_bricks", "cracked_deepslate_brick"),
			Map.entry("deepslate_tiles", "deepslate_tile"), Map.entry("cracked_deepslate_tiles", "cracked_deepslate_tile"),
			Map.entry("tuff_bricks", "tuff_brick"), Map.entry("chiseled_tuff_bricks", "chiseled_tuff_brick"), Map.entry("bricks", "brick"),
			Map.entry("mud_bricks", "mud_brick"), Map.entry("resin_bricks", "resin_brick"), Map.entry("chiseled_resin_bricks", "chiseled_resin_brick"),
			Map.entry("prismarine_bricks", "prismarine_brick"), Map.entry("nether_bricks", "nether_brick"),
			Map.entry("cracked_nether_bricks", "cracked_nether_brick"), Map.entry("chiseled_nether_bricks", "chiseled_nether_brick"),
			Map.entry("red_nether_bricks", "red_nether_brick"), Map.entry("polished_blackstone_bricks", "polished_blackstone_brick"),
			Map.entry("cracked_polished_blackstone_bricks", "cracked_polished_blackstone_brick"), Map.entry("end_stone_bricks", "end_stone_brick"),
			Map.entry("purpur_block", "purpur"), Map.entry("coal_block", "coal"), Map.entry("iron_block", "iron"), Map.entry("gold_block", "gold"),
			Map.entry("redstone_block", "redstone"), Map.entry("emerald_block", "emerald"), Map.entry("lapis_block", "lapis"),
			Map.entry("diamond_block", "diamond"), Map.entry("netherite_block", "netherite"), Map.entry("quartz_block", "quartz"),
			Map.entry("chiseled_quartz_block", "chiseled_quartz"), Map.entry("quartz_bricks", "quartz_brick"), Map.entry("copper_block", "copper"),
			Map.entry("waxed_copper_block", "waxed_copper"));

	/** Copper's ages, as vanilla names them. */
	private static final String[] AGES = {"", "exposed_", "weathered_", "oxidized_"};

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
			Map.entry("magma_block", Textures.all("magma")),
			Map.entry("chiseled_tuff", Textures.column("chiseled_tuff_top", "chiseled_tuff")),
			Map.entry("chiseled_tuff_bricks", Textures.column("chiseled_tuff_bricks_top", "chiseled_tuff_bricks")),
			Map.entry("chiseled_sandstone", Textures.column("sandstone_top", "chiseled_sandstone")),
			Map.entry("smooth_sandstone", Textures.all("sandstone_top")),
			Map.entry("cut_sandstone", Textures.column("sandstone_top", "cut_sandstone")),
			Map.entry("chiseled_red_sandstone", Textures.column("red_sandstone_top", "chiseled_red_sandstone")),
			Map.entry("smooth_red_sandstone", Textures.all("red_sandstone_top")),
			Map.entry("cut_red_sandstone", Textures.column("red_sandstone_top", "cut_red_sandstone")),
			Map.entry("polished_basalt", Textures.column("polished_basalt_top", "polished_basalt_side")),
			Map.entry("purpur_pillar", Textures.column("purpur_pillar_top", "purpur_pillar")),
			Map.entry("quartz_block", Textures.column("quartz_block_top", "quartz_block_side")),
			Map.entry("chiseled_quartz_block", Textures.column("chiseled_quartz_block_top", "chiseled_quartz_block")),
			Map.entry("quartz_pillar", Textures.column("quartz_pillar_top", "quartz_pillar")),
			Map.entry("smooth_quartz", Textures.all("quartz_block_bottom")));

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
		java.util.Set<String> nature = java.util.Set.of(NATURE);
		for (String n : BUILDING) if (!nature.contains(n)) out.add(new Material(n, Kind.BUILDING, null));
		return List.copyOf(out);
	}

	/** The vanilla block. */
	public Block base() {
		return BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(id));
	}

	public boolean glass() {
		return kind == Kind.GLASS;
	}

	public boolean nature() {
		return kind == Kind.NATURE;
	}

	/** A building block: every shape, plus pillars (some natural blocks are building blocks too). */
	public boolean building() {
		return BUILDING_SET.contains(id);
	}

	public static Material byId(String id) {
		for (Material m : ALL) if (m.id.equals(id)) return m;
		return null;
	}

	/** How weathered unwaxed copper is (0 to 3), or -1 for anything else (waxed copper too). */
	public int copperAge() {
		if (id.equals("copper_block") || id.equals("chiseled_copper") || id.equals("cut_copper")) return 0;
		for (int a = 1; a < 4; a++) {
			if (!id.startsWith(AGES[a])) continue;
			String rest = id.substring(AGES[a].length());
			if (rest.equals("copper") || rest.equals("chiseled_copper") || rest.equals("cut_copper")) return a;
		}
		return -1;
	}

	/** Unwaxed copper's next age (exposed after new, and so on), or null. */
	public Material nextAge() {
		int a = copperAge();
		if (a < 0 || a == 3) return null;
		String rest = a == 0 ? (id.equals("copper_block") ? "copper" : id) : id.substring(AGES[a].length());
		return byId(AGES[a + 1] + rest);
	}

	/** Unwaxed copper's waxed version, or null. */
	public Material waxed() {
		return copperAge() < 0 ? null : byId("waxed_" + id);
	}

	/** Copper, waxed or not. */
	public boolean copper() {
		return copperAge() >= 0 || (id.startsWith("waxed_") && byId(id.substring(6)) != null && byId(id.substring(6)).copperAge() >= 0);
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
		// Waxed copper looks just like unwaxed.
		String look = id.startsWith("waxed_") ? id.substring(6) : id;
		Textures t = TEXTURES.get(look);
		return t != null ? t : Textures.all(look);
	}

	/** For concrete powder: the concrete of the same colour, which its shapes turn into when wet. */
	public Material hardened() {
		if (kind != Kind.POWDER) return null;
		for (Material m : ALL) if (m.kind == Kind.CONCRETE && m.colour.equals(colour)) return m;
		return null;
	}

	/** The start of its shapes' ids: the block's id, or a shorter name (stone_brick for stone_bricks). */
	public String stem() {
		return STEMS.getOrDefault(id, id);
	}

	/**
	 * The block's id for one of this material's shapes, for example red_wool_vertical_slab or
	 * stone_brick_stairs. Pillars are named like thin logs: stone_bricks_12.
	 */
	public String idOf(Shape shape) {
		if (shape.pillarWidth() > 0) return id + "_" + shape.pillarWidth();
		return stem() + "_" + shape.suffix;
	}
}
