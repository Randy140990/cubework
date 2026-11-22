package com.insidethecube.retromon.client;

import net.minecraft.client.Minecraft;

/**
 * Nights in a Retromon save: as the games are, a blue, moonlit night you can still see in rather than
 * Minecraft's pitch dark. The game's night vision is lifted a little as night falls (shader packs read the same
 * value, so it works with them too); OPTION sets how much (DARK, SOFT, BRIGHT).
 */
public final class Night {
	private Night() {}

	/** The strength the server's unseen night vision has (a potion's is 0 or 1). */
	public static final int AMPLIFIER = 7;

	/** Tells the server how you like nights (and the rest of your settings). */
	public static void send() {
		Settings.sendPrefs();
	}

	/** How far it's night (0 by day, 1 at full night), from the time of day. */
	public static float nightness() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return 0;
		// Viridian Forest: always night under its trees.
		if (mc.player != null && com.insidethecube.retromon.game.Interiors.alwaysNight(mc.player)) return 1;
		long t = Math.floorMod(Adventure.shownDay(mc.level.getDayTime()), 24000L);
		// Dusk from 12300 to 13800, dawn from 22300 to 23700.
		float k;
		if (t < 12300 || t >= 23700) k = 0;
		else if (t < 13800) k = (t - 12300) / 1500F;
		else if (t < 22300) k = 1;
		else k = 1 - (t - 22300) / 1400F;
		return k * k * (3 - 2 * k);
	}

	/** The lift to give: 0 off, up to about a third (SOFT) or a half (BRIGHT) at full night. */
	public static float lift() {
		if (!Adventure.inSave) return 0;
		int setting = Settings.nights();
		if (setting == 0) return 0;
		return nightness() * (setting == 1 ? 0.3F : 0.5F);
	}
}
