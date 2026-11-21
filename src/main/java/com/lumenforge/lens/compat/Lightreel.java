package com.lumenforge.lens.compat;

import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Method;

/**
 * Lightreel (our replay editor) also changes the size the game draws at while it exports. The two would fight
 * over it, so while Lightreel's exporting, Lightlens waits. Read by reflection: Lightreel isn't needed.
 */
public final class Lightreel {
	private static Method running;
	private static boolean looked;

	private Lightreel() {}

	public static boolean installed() {
		return FabricLoader.getInstance().isModLoaded("lfcinema");
	}

	public static boolean exporting() {
		if (!installed()) return false;
		try {
			if (!looked) {
				looked = true;
				running = Class.forName("com.lumenforge.cinema.export.Exporter").getMethod("running");
			}
			return running != null && (Boolean) running.invoke(null);
		} catch (Throwable t) {
			return false;
		}
	}
}
