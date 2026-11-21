package com.lumenforge.lens.compat;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Which shader pack is on, for the settings screen ("Tempered Light 2.10.5 is on: it'll be in your video").
 * Lightlens records the finished picture, so whatever Iris draws is in it; nothing else is needed from Iris.
 * Read by reflection, so Iris isn't needed.
 */
public final class Shaders {
	private Shaders() {}

	public static boolean irisInstalled() {
		return FabricLoader.getInstance().isModLoaded("iris");
	}

	/** The pack's name when shaders are on, or null. */
	public static String activePack() {
		if (!irisInstalled()) return null;
		try {
			Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
			Object inst = api.getMethod("getInstance").invoke(null);
			boolean on = (Boolean) api.getMethod("isShaderPackInUse").invoke(inst);
			if (!on) return null;
			try {
				Object name = Class.forName("net.irisshaders.iris.Iris").getMethod("getCurrentPackName").invoke(null);
				if (name != null) {
					String n = name.toString();
					if (n.toLowerCase().endsWith(".zip")) n = n.substring(0, n.length() - 4);
					return n.replace('_', ' ');
				}
			} catch (Throwable ignored) {
			}
			return "a shader pack";
		} catch (Throwable t) {
			return null;
		}
	}
}
