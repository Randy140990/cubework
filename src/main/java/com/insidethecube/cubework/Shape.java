package com.insidethecube.cubework;

/** Every shape Cubework makes, in the order they appear in each colour's family in the creative menu. */
public enum Shape {
	STAIRS("stairs", "Stairs", 1),
	SLAB("slab", "Slab", 2),
	VERTICAL_SLAB("vertical_slab", "Vertical Slab", 2),
	SLOPE("slope", "Slope", 2),
	LAYER("layer", "Layer", 8),
	VERTICAL_LAYER("vertical_layer", "Vertical Layer", 8),
	CARPET("carpet", "Carpet", 4),
	PANEL("panel", "Panel", 4),
	POST("post", "Post", 4),
	WALL("wall", "Wall", 1),
	FENCE("fence", "Fence", 1),
	FENCE_GATE("fence_gate", "Fence Gate", 1),
	TRAPDOOR("trapdoor", "Trapdoor", 1),
	BUTTON("button", "Button", 4),
	PRESSURE_PLATE("pressure_plate", "Pressure Plate", 2);

	public final String suffix;
	public final String title;
	/** How many one block makes in the stonecutter. */
	public final int fromStonecutter;

	Shape(String suffix, String title, int fromStonecutter) {
		this.suffix = suffix;
		this.title = title;
		this.fromStonecutter = fromStonecutter;
	}

	/** Wool already has carpet, so wool skips it. */
	public boolean appliesTo(Material m) {
		return !(this == CARPET && m.kind() == Material.Kind.WOOL);
	}
}
