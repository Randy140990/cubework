package com.insidethecube.cubework;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.Block;

/**
 * A vanilla block that Cubework makes shapes of. Its shapes copy its properties (sound, hardness, tool,
 * light) and use its own textures, so a texture pack that changes the block changes them too.
 */
public record Material(String id, Kind kind, String colour, String pattern, String source) {

	public Material(String id, Kind kind, String colour) {
		this(id, kind, colour, null, null);
	}

	public enum Kind {
		WOOL, TERRACOTTA, CONCRETE, POWDER, GLAZED, GLASS, NATURE, BUILDING, PATTERN, WALLPAPER;
	}

	/** Patterns laid over a block's own texture (made in the game from the textures in use, see client.Patterns). */
	public static final String[] PATTERNS = {"tiled", "checkered", "herringbone", "bordered"};

	/** The blocks that come in patterns: floors and walls with an even texture. */
	public static final String[] PATTERN_SOURCES = {
			"stone", "smooth_stone", "cobblestone", "stone_bricks", "mossy_stone_bricks", "granite", "polished_granite", "diorite",
			"polished_diorite", "andesite", "polished_andesite", "cobbled_deepslate", "polished_deepslate", "deepslate_bricks", "deepslate_tiles",
			"tuff", "polished_tuff", "tuff_bricks", "calcite", "dripstone_block", "bricks", "mud_bricks", "packed_mud", "resin_bricks",
			"smooth_sandstone", "smooth_red_sandstone", "prismarine", "prismarine_bricks", "dark_prismarine", "nether_bricks", "red_nether_bricks",
			"smooth_basalt", "polished_blackstone", "polished_blackstone_bricks", "end_stone", "end_stone_bricks", "purpur_block", "quartz_bricks",
			"smooth_quartz", "obsidian", "iron_block", "gold_block", "emerald_block", "lapis_block", "diamond_block", "netherite_block",
			"amethyst_block", "terracotta", "white_terracotta", "light_gray_terracotta", "gray_terracotta", "black_terracotta", "brown_terracotta",
			"red_terracotta", "orange_terracotta", "yellow_terracotta", "lime_terracotta", "green_terracotta", "cyan_terracotta",
			"light_blue_terracotta", "blue_terracotta", "purple_terracotta", "magenta_terracotta", "pink_terracotta", "white_concrete",
			"light_gray_concrete", "gray_concrete", "black_concrete", "brown_concrete", "red_concrete", "orange_concrete", "yellow_concrete",
			"lime_concrete", "green_concrete", "cyan_concrete", "light_blue_concrete", "blue_concrete", "purple_concrete", "magenta_concrete",
			"pink_concrete", "white_wool", "light_gray_wool", "gray_wool", "black_wool", "brown_wool", "red_wool", "orange_wool", "yellow_wool",
			"lime_wool", "green_wool", "cyan_wool", "light_blue_wool", "blue_wool", "purple_wool", "magenta_wool", "pink_wool"};

	/** Wallpapers: Cubework's own blocks (textures in cubework:block/wallpaper/), made like birch planks. */
	public static final String[] WALLPAPERS = {"yellow_wallpaper", "yellow_striped_wallpaper", "cream_wallpaper", "cream_striped_wallpaper",
			"sage_wallpaper", "sage_striped_wallpaper", "rose_wallpaper", "rose_striped_wallpaper"};

	/** Building blocks that get partition walls too (with concrete, terracotta and wallpaper): smooth, plain walls. */
	private static final java.util.Set<String> OFFICE_BUILDING = java.util.Set.of("smooth_stone", "stone_bricks", "bricks", "polished_andesite",
			"polished_diorite", "polished_granite", "polished_deepslate", "polished_tuff", "mud_bricks", "smooth_sandstone", "smooth_quartz", "quartz_bricks");

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
	private static final Map<String, Material> BY_ID = new HashMap<>();
	private static final Map<String, Behaviour> BEHAVIOURS = new HashMap<>();

	static {
		for (Material m : ALL) BY_ID.put(m.id, m);
		for (Material m : ALL) BEHAVIOURS.put(m.id, Behaviour.of(m));
	}

	/** A material saved by its id (red_wool, tiled_stone_bricks). */
	public static final Codec<Material> CODEC = Codec.STRING.comapFlatMap(
			id -> {
				Material m = byId(id);
				return m != null ? DataResult.success(m) : DataResult.error(() -> "unknown Cubework material " + id);
			}, Material::id);
	public static final StreamCodec<ByteBuf, Material> STREAM_CODEC = ByteBufCodecs.STRING_UTF8.map(Material::byIdOrStone, Material::id);

	/** What its shapes' blocks do (see Behaviour). */
	public Behaviour behaviour() {
		return BEHAVIOURS.get(id);
	}

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
		for (String src : PATTERN_SOURCES) for (String p : PATTERNS) out.add(new Material(p + "_" + src, Kind.PATTERN, null, p, src));
		for (String w : WALLPAPERS) out.add(new Material(w, Kind.WALLPAPER, null, "wallpaper", "birch_planks"));
		return List.copyOf(out);
	}

	/** The full block: the vanilla block, or for a pattern Cubework's patterned block (once registered). */
	public Block base() {
		return pattern != null ? Reg.block(Reg.id(id)) : Reg.vanilla(id);
	}

	/** The vanilla block it's made from, whose sound, hardness and tool it copies. */
	public Block vanillaBlock() {
		return Reg.vanilla(pattern != null ? source : id);
	}

	/** Whether this Minecraft version has the block (pale moss and resin came in 1.21.4). */
	public boolean exists() {
		return Reg.exists(pattern != null ? source : id);
	}

	/** A patterned block (tiled stone bricks and so on). */
	public boolean patterned() {
		return kind == Kind.PATTERN;
	}

	/** A wallpaper (Cubework's own full block). */
	public boolean wallpaper() {
		return kind == Kind.WALLPAPER;
	}

	/** Gets partition walls: concrete, terracotta, wallpaper and the plain building blocks. */
	public boolean office() {
		return kind == Kind.CONCRETE || kind == Kind.TERRACOTTA || kind == Kind.WALLPAPER || OFFICE_BUILDING.contains(id);
	}

	/** Copper, and the metal and gem blocks (their own tab). */
	public boolean metal() {
		return copper() || java.util.Set.of("coal_block", "iron_block", "gold_block", "redstone_block", "emerald_block", "lapis_block", "diamond_block",
				"netherite_block", "amethyst_block").contains(id);
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
		return BY_ID.get(id);
	}

	/** For the network: a material the other side doesn't know reads as stone rather than failing. */
	private static Material byIdOrStone(String id) {
		Material m = BY_ID.get(id);
		return m != null ? m : BY_ID.get("stone");
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

	/** Copper's age before this one (scraped with an axe), or null. */
	public Material previousAge() {
		int a = copperAge();
		if (a <= 0) return null;
		String rest = id.substring(AGES[a].length());
		if (a == 1) return byId(rest.equals("copper") ? "copper_block" : rest);
		return byId(AGES[a - 1] + rest);
	}

	/** Waxed copper without its wax, or null. */
	public Material unwaxed() {
		if (!id.startsWith("waxed_")) return null;
		Material m = byId(id.substring(6));
		return m != null && m.copperAge() >= 0 ? m : null;
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

	/** The block's textures (paths under minecraft:block/, or full ids such as cubework:block/pattern/tiled/stone). */
	public Textures textures() {
		if (kind == Kind.WALLPAPER) return Textures.all(Cubework.ID + ":block/wallpaper/" + id);
		if (pattern != null) {
			Textures t = TEXTURES.get(source);
			if (t == null) t = Textures.all(source);
			String at = Cubework.ID + ":block/pattern/" + pattern + "/";
			return new Textures(at + t.top(), at + t.side(), at + t.bottom(), null);
		}
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
		if (kind == Kind.WALLPAPER) return id;
		if (pattern != null) return pattern + "_" + STEMS.getOrDefault(source, source);
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
