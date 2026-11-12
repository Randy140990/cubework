package com.insidethecube.deepkit.paint;

/** Glow paint colours. */
public enum PaintColour {
	WHITE("white", "White", 0xF4F4EE, "white_dye"),
	RED("red", "Red", 0xFF4A3D, "red_dye"),
	ORANGE("orange", "Orange", 0xFF9A2E, "orange_dye"),
	YELLOW("yellow", "Yellow", 0xFFE14A, "yellow_dye"),
	LIME("lime", "Lime", 0x8CF04A, "lime_dye"),
	CYAN("cyan", "Cyan", 0x3EE6E6, "cyan_dye"),
	BLUE("blue", "Blue", 0x4F86FF, "blue_dye"),
	PINK("pink", "Pink", 0xFF7BC0, "pink_dye");

	public final String name, title, dye;
	public final int rgb;

	PaintColour(String name, String title, int rgb, String dye) {
		this.name = name;
		this.title = title;
		this.rgb = rgb;
		this.dye = dye;
	}
}
