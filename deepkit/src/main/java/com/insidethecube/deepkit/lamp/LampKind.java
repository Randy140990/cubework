package com.insidethecube.deepkit.lamp;

/** The three lamps: how far the beam reaches, its cone, how bright the light it leaves is, and the battery. */
public enum LampKind {
	TORCH("Hand torch", 32, 18, 12, 15 * 60 * 20),
	HEAD("Headlamp", 24, 30, 11, 20 * 60 * 20),
	CHEST("Chest lamp", 20, 40, 10, 30 * 60 * 20);

	public final String title;
	/** Blocks. */
	public final int range;
	/** Degrees across (used by shader packs; the light itself is a pool where the beam lands). */
	public final int cone;
	/** Light level where the beam lands. */
	public final int light;
	/** Battery, in ticks of light. */
	public final int battery;

	LampKind(String title, int range, int cone, int light, int battery) {
		this.title = title;
		this.range = range;
		this.cone = cone;
		this.light = light;
		this.battery = battery;
	}
}
