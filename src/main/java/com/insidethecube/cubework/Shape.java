package com.insidethecube.cubework;

/** Every shape Cubework makes, in the order they appear in each colour's family in the creative menu. */
public enum Shape {
	STAIRS("stairs", "Stairs", 1),
	STEPS_4("steps_4", "Stairs (4 Steps)", 1),
	STEPS_6("steps_6", "Stairs (6 Steps)", 1),
	STEPS_8("steps_8", "Stairs (8 Steps)", 1),
	STAIRS_2("stairs_2", "Stairs (1 in 2)", 2),
	STAIRS_3("stairs_3", "Stairs (1 in 3)", 2),
	STAIRS_4("stairs_4", "Stairs (1 in 4)", 2),
	STAIR_CURVE("stair_curve", "Curved Stairs", 1),
	SLAB("slab", "Slab", 2),
	QUARTER_SLAB("quarter_slab", "Quarter Slab", 4),
	VERTICAL_SLAB("vertical_slab", "Vertical Slab", 2),
	SLOPE("slope", "Slope", 2),
	SLOPE_2("slope_2", "Slope (1 in 2)", 2),
	SLOPE_3("slope_3", "Slope (1 in 3)", 2),
	GENTLE_SLOPE("gentle_slope", "Slope (1 in 4)", 2),
	LAYER("layer", "Layer", 8),
	VERTICAL_LAYER("vertical_layer", "Vertical Layer", 8),
	CARPET("carpet", "Carpet", 4),
	PANEL("panel", "Panel", 4),
	POST("post", "Post", 4),
	PILLAR_12("pillar_12", "(12 px)", 1),
	PILLAR_8("pillar_8", "(8 px)", 2),
	PILLAR_4("pillar_4", "(4 px)", 4),
	ROUND_COLUMN_16("round_column_16", "Round Column (16 px)", 1),
	ROUND_COLUMN_12("round_column_12", "Round Column (12 px)", 2),
	ROUND_COLUMN_8("round_column_8", "Round Column (8 px)", 4),
	ROUND_COLUMN_4("round_column_4", "Round Column (4 px)", 8),
	CORNER_POST("corner_post", "Corner Post", 4),
	ARCH("arch", "Arch", 1),
	ARCH_CORNER("arch_corner", "Arch Corner", 1),
	ROUND_CORNER("round_corner", "Round Corner", 1),
	ROUND_CORNER_INNER("round_corner_inner", "Inner Round Corner", 4),
	WALL("wall", "Wall", 1),
	FENCE("fence", "Fence", 1),
	FENCE_GATE("fence_gate", "Fence Gate", 1),
	TRAPDOOR("trapdoor", "Trapdoor", 1),
	BUTTON("button", "Button", 4),
	PRESSURE_PLATE("pressure_plate", "Pressure Plate", 2),
	TRIM_2("trim_2", "Trim (2 px)", 16),
	TRIM_4("trim_4", "Trim (4 px)", 8),
	TRIM_8("trim_8", "Trim (8 px)", 4),
	WINDOW_4("window_frame_4", "Window Frame (4 px)", 4),
	WINDOW_8("window_frame_8", "Window Frame (8 px)", 2),
	WINDOW_12("window_frame_12", "Window Frame (12 px)", 1),
	WINDOW_16("window_frame_16", "Window Frame (16 px)", 1),
	DIAMOND_WINDOW_4("diamond_window_frame_4", "Diamond Window Frame (4 px)", 4),
	DIAMOND_WINDOW_8("diamond_window_frame_8", "Diamond Window Frame (8 px)", 2),
	DIAMOND_WINDOW_12("diamond_window_frame_12", "Diamond Window Frame (12 px)", 1),
	DIAMOND_WINDOW_16("diamond_window_frame_16", "Diamond Window Frame (16 px)", 1),
	ROUND_WINDOW_4("round_window_frame_4", "Round Window Frame (4 px)", 4),
	ROUND_WINDOW_8("round_window_frame_8", "Round Window Frame (8 px)", 2),
	ROUND_WINDOW_12("round_window_frame_12", "Round Window Frame (12 px)", 1),
	ROUND_WINDOW_16("round_window_frame_16", "Round Window Frame (16 px)", 1);

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
	 * Building blocks get every shape, and pillars. Coloured blocks get every shape but pillars (wool skips
	 * carpet, which vanilla has). Natural blocks get the shapes for shaping land: slabs, stairs, slopes,
	 * layers and carpet. Patterned blocks get the shapes for floors and walls.
	 */
	public boolean appliesTo(Material m) {
		if (m.patterned()) return patternShape();
		if (pillarWidth() > 0) return m.building();
		if (m.building()) return true;
		if (m.nature()) return natureShape();
		return !(this == CARPET && m.kind() == Material.Kind.WOOL);
	}

	/** The shapes natural blocks get (shown in the Nature tab). */
	public boolean natureShape() {
		return switch (this) {
			case STAIRS, SLAB, QUARTER_SLAB, VERTICAL_SLAB, SLOPE, SLOPE_2, SLOPE_3, GENTLE_SLOPE, LAYER, VERTICAL_LAYER, CARPET -> true;
			default -> false;
		};
	}

	/** The shapes patterned blocks get: floors, steps and walls. */
	public boolean patternShape() {
		return switch (this) {
			case STAIRS, SLAB, QUARTER_SLAB, VERTICAL_SLAB, SLOPE, LAYER, VERTICAL_LAYER, CARPET, PANEL -> true;
			default -> false;
		};
	}

	/** A round column's width in pixels, or 0. */
	public int columnWidth() {
		return switch (this) {
			case ROUND_COLUMN_16 -> 16;
			case ROUND_COLUMN_12 -> 12;
			case ROUND_COLUMN_8 -> 8;
			case ROUND_COLUMN_4 -> 4;
			default -> 0;
		};
	}

	/** Shapes drawn in code from the block's textures (smooth slopes, arches, round shapes). */
	public boolean drawnInCode() {
		return switch (this) {
			case SLOPE, SLOPE_2, SLOPE_3, GENTLE_SLOPE, ARCH, ARCH_CORNER, ROUND_COLUMN_16, ROUND_COLUMN_12, ROUND_COLUMN_8, ROUND_COLUMN_4, ROUND_CORNER,
					ROUND_CORNER_INNER, ROUND_WINDOW_4, ROUND_WINDOW_8, ROUND_WINDOW_12, ROUND_WINDOW_16, STAIR_CURVE -> true;
			default -> false;
		};
	}

	/** The groups on the shape wheel's inner ring. */
	public enum Group {
		SLABS("Slabs and layers"), STAIRS("Stairs and slopes"), PILLARS("Pillars and columns"), CURVES("Arches and curves"),
		WALLS("Walls and fences"), WINDOWS("Window frames"), SMALL("Small parts");

		public final String title;

		Group(String title) {
			this.title = title;
		}
	}

	public Group group() {
		return switch (this) {
			case SLAB, QUARTER_SLAB, VERTICAL_SLAB, LAYER, VERTICAL_LAYER, CARPET, PANEL -> Group.SLABS;
			case STAIRS, STEPS_4, STEPS_6, STEPS_8, STAIRS_2, STAIRS_3, STAIRS_4, STAIR_CURVE, SLOPE, SLOPE_2, SLOPE_3, GENTLE_SLOPE -> Group.STAIRS;
			case POST, PILLAR_12, PILLAR_8, PILLAR_4, ROUND_COLUMN_16, ROUND_COLUMN_12, ROUND_COLUMN_8, ROUND_COLUMN_4, CORNER_POST -> Group.PILLARS;
			case ARCH, ARCH_CORNER, ROUND_CORNER, ROUND_CORNER_INNER -> Group.CURVES;
			case WALL, FENCE, FENCE_GATE -> Group.WALLS;
			case TRAPDOOR, BUTTON, PRESSURE_PLATE, TRIM_2, TRIM_4, TRIM_8 -> Group.SMALL;
			default -> Group.WINDOWS;
		};
	}

	/** A pillar's width in pixels (it's 16 tall, in the middle of the block), or 0. */
	public int pillarWidth() {
		return switch (this) {
			case PILLAR_12 -> 12;
			case PILLAR_8 -> 8;
			case PILLAR_4 -> 4;
			default -> 0;
		};
	}

	/** The castle shapes (gentle slopes, quarter slabs, arches, window frames). Every coloured and building block has them. */
	public boolean castle() {
		return switch (this) {
			case QUARTER_SLAB, SLOPE_2, SLOPE_3, GENTLE_SLOPE, ARCH, ARCH_CORNER -> true;
			default -> windowDepth() > 0;
		};
	}

	/** A window frame's depth in pixels, or 0. */
	public int windowDepth() {
		return switch (this) {
			case WINDOW_4, DIAMOND_WINDOW_4, ROUND_WINDOW_4 -> 4;
			case WINDOW_8, DIAMOND_WINDOW_8, ROUND_WINDOW_8 -> 8;
			case WINDOW_12, DIAMOND_WINDOW_12, ROUND_WINDOW_12 -> 12;
			case WINDOW_16, DIAMOND_WINDOW_16, ROUND_WINDOW_16 -> 16;
			default -> 0;
		};
	}

	public boolean diamond() {
		return name().startsWith("DIAMOND");
	}

	/** Round window frames: the opening's corners are rounded, so one makes a porthole and joined ones a big round window. */
	public boolean round() {
		return name().startsWith("ROUND_WINDOW");
	}

	/** How many blocks stairs take to climb one (2, 3 or 4), or 0. */
	public int stairsPitch() {
		return switch (this) {
			case STAIRS_2 -> 2;
			case STAIRS_3 -> 3;
			case STAIRS_4 -> 4;
			default -> 0;
		};
	}

	/** Stairs with more, smaller steps (4, 6 or 8 to the block, still climbing one block per block), or 0. */
	public int stepCount() {
		return switch (this) {
			case STEPS_4 -> 4;
			case STEPS_6 -> 6;
			case STEPS_8 -> 8;
			default -> 0;
		};
	}

	/** A trim's thickness in pixels (a square strip along one edge of the block), or 0. */
	public int trimWidth() {
		return switch (this) {
			case TRIM_2 -> 2;
			case TRIM_4 -> 4;
			case TRIM_8 -> 8;
			default -> 0;
		};
	}
}
