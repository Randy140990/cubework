package com.insidethecube.retromon;

import java.util.Optional;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

/**
 * (0.76, John's 28 Nov 12:43: "every time that I save it and provide it you ... you need to ensure that it's version
 * updated as well, because that's the most important thing ... if I continue game, it should probably be updating the
 * map as well") Which map this Retromon is made for, and which map jar is in the mods folder.
 *
 * <p>Each save John sends is a new map version (Retromon-Map-1.0.4.jar and on). This mod needs at least {@link #NEEDS}:
 * an older map jar is refused by name, so a game is never made from an old map without anyone knowing. A game records
 * the map it was made with (its retromon.json's "map"), and continuing a game made with an older map brings the new
 * map's land into it first (IntroScreen), keeping everything that's yours.
 */
public final class MapVersion {
	private MapVersion() {}

	/** The map this Retromon is made for (the oldest map jar it will play). */
	public static final String NEEDS = "1.0.4";
	/** That map jar's file name, as it's sent. */
	public static final String JAR = "Retromon-Map-" + NEEDS + ".jar";

	/** The map jar in the mods folder: Retromon Map's version (1.0.4), or null if there's none. */
	public static String installed() {
		Optional<ModContainer> c = FabricLoader.getInstance().getModContainer("retromon_map");
		return c.map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse(null);
	}

	/** -1, 0 or 1 as version a is older than, the same as, or newer than b (1.0.10 is newer than 1.0.9). */
	public static int compare(String a, String b) {
		String[] x = a.split("[.+-]"), y = b.split("[.+-]");
		for (int i = 0; i < Math.max(x.length, y.length); i++) {
			int p = i < x.length ? num(x[i]) : 0, q = i < y.length ? num(y[i]) : 0;
			if (p != q) return p < q ? -1 : 1;
		}
		return 0;
	}

	private static int num(String s) {
		try {
			return Integer.parseInt(s.replaceAll("\\D", ""));
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	/**
	 * What's wrong with the map jar, in words for the screen, or null when it's the right one (or the map is a map
	 * maker's world folder in .minecraft/retromon, which has no version).
	 */
	public static String problem() {
		String have = installed();
		if (have == null) return null;
		if (compare(have, NEEDS) < 0)
			return "Your mods folder has Retromon-Map-" + have + ".jar, an older map. This Retromon needs " + JAR
					+ ". Take the old one out, put " + JAR + " in its place, then restart.";
		return null;
	}
}
