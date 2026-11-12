package com.insidethecube.deepkit.flare;

/** Flare colours: red (plain "Flare"), green, blue and white. */
public enum FlareColour {
	RED("flare", "Flare", 0xFF3A2A, "red_dye"),
	GREEN("green_flare", "Green Flare", 0x46E35A, "green_dye"),
	BLUE("blue_flare", "Blue Flare", 0x3D8BFF, "blue_dye"),
	WHITE("white_flare", "White Flare", 0xF6F2E8, "white_dye");

	public final String item, title, dye;
	public final int rgb;

	FlareColour(String item, String title, int rgb, String dye) {
		this.item = item;
		this.title = title;
		this.rgb = rgb;
		this.dye = dye;
	}
}
