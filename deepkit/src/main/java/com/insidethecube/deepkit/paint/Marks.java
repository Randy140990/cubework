package com.insidethecube.deepkit.paint;

/** The marks glow paint can paint (their textures are block/mark/{name}). */
public final class Marks {
	private Marks() {}

	public static final String[] NAMES = {"arrow", "cross", "dot", "circle", "exclamation", "question", "tick", "0", "1", "2", "3", "4", "5", "6", "7",
			"8", "9"};
	public static final String[] TITLES = {"Arrow", "Cross", "Dot", "Circle", "Exclamation mark", "Question mark", "Tick", "0", "1", "2", "3", "4", "5",
			"6", "7", "8", "9"};
	/** How each looks on the picker. */
	public static final String[] GLYPHS = {"↑", "✕", "●", "○", "!", "?", "✓", "0", "1", "2", "3", "4", "5", "6", "7", "8",
			"9"};

	public static int count() {
		return NAMES.length;
	}

	public static int clamp(int m) {
		return Math.floorMod(m, NAMES.length);
	}
}
