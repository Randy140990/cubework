package com.insidethecube.cubework;

import net.minecraft.world.level.block.Block;

/**
 * What a shape's block does that can't change from one placed block to the next: how it lets light and
 * entities through, how slippery it is, whether it glows, burns or weathers, how tall its full height is.
 * Every material belongs to one; each shape has one block per behaviour, and the material of each placed
 * block is kept with it (see block.ShapeEntity). Sound, hardness, the right tool, map colour and blast
 * resistance still come from the material, block by block.
 */
public enum Behaviour {
	/** Everything not listed below: stone, dirt, concrete, terracotta, metals... */
	SOLID("", "stone"),
	/** Dirt path and farmland: a pixel lower than a full block. */
	SHORT("short", "dirt_path"),
	GLASS("glass", "glass"),
	/** Tinted glass: see-through, but blocks light. */
	TINTED("tinted", "tinted_glass"),
	ICE("ice", "ice"),
	PACKED_ICE("packed_ice", "packed_ice"),
	BLUE_ICE("blue_ice", "blue_ice"),
	/** Soul sand: slows you down. */
	SOUL("soul", "soul_sand"),
	MAGMA("magma", "magma_block"),
	CRYING("crying", "crying_obsidian"),
	GLOWING("glowing", "glowstone"),
	/** Wool and wool patterns: catch fire and burn away. */
	WOOL("wool", "white_wool"),
	WALLPAPER("wallpaper", "birch_planks"),
	/** Coal: burns like the coal block. */
	COAL("coal", "coal_block"),
	/** Copper, waxed or not: unwaxed copper weathers. */
	COPPER("copper", "copper_block");

	/** Added to the shape's id (shape_slab_glass): empty for SOLID (shape_slab). */
	public final String suffix;
	/** The vanilla block whose settings the shapes copy. */
	public final String model;

	Behaviour(String suffix, String model) {
		this.suffix = suffix;
		this.model = model;
	}

	public Block modelBlock() {
		return Reg.vanilla(model);
	}

	/**
	 * The block id of a shape in this behaviour: shape_slab, shape_slab_glass, shape_round_window_frame_8_copper.
	 * The "shape_" keeps them apart from the ids before 0.12 (diamond_window_frame_4 was the diamond block's
	 * window frame).
	 */
	public String idOf(Shape shape) {
		return "shape_" + (suffix.isEmpty() ? shape.suffix : shape.suffix + "_" + suffix);
	}

	/** See-through, drawn as the game draws glass and ice. */
	public boolean seeThrough() {
		return this == GLASS || this == TINTED || this == ICE;
	}

	/** Glass: shapes of the same glass hide the faces where they meet. */
	public boolean glass() {
		return this == GLASS || this == TINTED;
	}

	/** How tall the full block is, in pixels: dirt path and farmland are 15. */
	public int height() {
		return this == SHORT ? 15 : 16;
	}

	/** Which behaviour a material has. */
	public static Behaviour of(Material m) {
		if (m.kind() == Material.Kind.GLASS) return m.id().equals("tinted_glass") ? TINTED : GLASS;
		if (m.kind() == Material.Kind.WOOL || (m.patterned() && m.source().endsWith("_wool"))) return WOOL;
		if (m.wallpaper()) return WALLPAPER;
		if (m.copper()) return COPPER;
		return switch (m.id()) {
			case "ice" -> ICE;
			case "packed_ice" -> PACKED_ICE;
			case "blue_ice" -> BLUE_ICE;
			case "dirt_path", "farmland" -> SHORT;
			case "soul_sand" -> SOUL;
			case "magma_block" -> MAGMA;
			case "crying_obsidian" -> CRYING;
			case "glowstone" -> GLOWING;
			case "coal_block" -> COAL;
			default -> SOLID;
		};
	}
}
