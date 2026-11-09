package com.insidethecube.cubework;

/** Every shape Cubework makes, in the order they appear in each colour's family in the creative menu. */
public enum Shape {
	STAIRS("stairs", "Stairs", 1),
	SLAB("slab", "Slab", 2),
	QUARTER_SLAB("quarter_slab", "Quarter Slab", 4),
	VERTICAL_SLAB("vertical_slab", "Vertical Slab", 2),
	SLOPE("slope", "Slope", 2),
	GENTLE_SLOPE("gentle_slope", "Gentle Slope", 2),
	LAYER("layer", "Layer", 8),
	VERTICAL_LAYER("vertical_layer", "Vertical Layer", 8),
	CARPET("carpet", "Carpet", 4),
	PANEL("panel", "Panel", 4),
	POST("post", "Post", 4),
	ARCH("arch", "Arch", 1),
	ARCH_CORNER("arch_corner", "Arch Corner", 1),
	WALL("wall", "Wall", 1),
	FENCE("fence", "Fence", 1),
	FENCE_GATE("fence_gate", "Fence Gate", 1),
	TRAPDOOR("trapdoor", "Trapdoor", 1),
	BUTTON("button", "Button", 4),
	PRESSURE_PLATE("pressure_plate", "Pressure Plate", 2),
	WINDOW_4("window_frame_4", "Window Frame (4 px)", 4),
	WINDOW_8("window_frame_8", "Window Frame (8 px)", 2),
	WINDOW_12("window_frame_12", "Window Frame (12 px)", 1),
	WINDOW_16("window_frame_16", "Window Frame (16 px)", 1),
	DIAMOND_WINDOW_4("diamond_window_frame_4", "Diamond Window Frame (4 px)", 4),
	DIAMOND_WINDOW_8("diamond_window_frame_8", "Diamond Window Frame (8 px)", 2),
	DIAMOND_WINDOW_12("diamond_window_frame_12", "Diamond Window Frame (12 px)", 1),
	DIAMOND_WINDOW_16("diamond_window_frame_16", "Diamond Window Frame (16 px)", 1);

	public final String suffix;
	public final String title;
	/** How many one block makes in the stonecutter. */
	public final int fromStonecutter;

	Shape(String suffix, String title, int fromStonecutter) {
		this.suffix = suffix;
		this.title = title;
		this.fromStonecutter = fromStonecutter;
	}

	/**
	 * Coloured blocks get every shape (wool skips carpet, which vanilla has). Natural blocks get the
	 * shapes for shaping land: slabs both ways, slopes, layers both ways and carpet.
	 */
	public boolean appliesTo(Material m) {
		if (m.nature()) return switch (this) {
			case SLAB, VERTICAL_SLAB, SLOPE, LAYER, VERTICAL_LAYER, CARPET -> true;
			default -> false;
		};
		if (castle() && !(m.kind() == Material.Kind.WOOL || m.kind() == Material.Kind.TERRACOTTA || m.kind() == Material.Kind.CONCRETE)) return false;
		return !(this == CARPET && m.kind() == Material.Kind.WOOL);
	}

	/** The castle shapes (gentle slopes, quarter slabs, arches, window frames): wool, terracotta and concrete for now. */
	public boolean castle() {
		return switch (this) {
			case QUARTER_SLAB, GENTLE_SLOPE, ARCH, ARCH_CORNER -> true;
			default -> windowDepth() > 0;
		};
	}

	/** A window frame's depth in pixels, or 0. */
	public int windowDepth() {
		return switch (this) {
			case WINDOW_4, DIAMOND_WINDOW_4 -> 4;
			case WINDOW_8, DIAMOND_WINDOW_8 -> 8;
			case WINDOW_12, DIAMOND_WINDOW_12 -> 12;
			case WINDOW_16, DIAMOND_WINDOW_16 -> 16;
			default -> 0;
		};
	}

	public boolean diamond() {
		return name().startsWith("DIAMOND");
	}
}
