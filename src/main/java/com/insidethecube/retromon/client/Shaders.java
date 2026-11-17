package com.insidethecube.retromon.client;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

import com.insidethecube.retromon.Retromon;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

/**
 * Tempered Light, built in: when you start a Retromon game and Iris is installed, the pack Retromon carries is
 * put in your shaderpacks folder and switched on (once a session), so the lights show at night. Your own
 * choice comes back when you leave. Without Iris, nothing happens and the game runs plain.
 */
public final class Shaders {
	private Shaders() {}

	public static final String PACK = "Tempered-Light-2.10.5.zip";
	private static boolean switched;
	private static String previousPack;
	private static boolean previousEnabled;

	private static Object config() throws Exception {
		Class<?> iris = Class.forName("net.irisshaders.iris.Iris");
		return iris.getMethod("getIrisConfig").invoke(null);
	}

	private static void reload() throws Exception {
		Class.forName("net.irisshaders.iris.Iris").getMethod("reload").invoke(null);
	}

	/** Entering a Retromon save: Tempered Light on (if Iris is here and it isn't already). */
	public static void enter() {
		if (switched || !FabricLoader.getInstance().isModLoaded("iris")) return;
		try {
			Path dir = Minecraft.getInstance().gameDirectory.toPath().resolve("shaderpacks");
			Files.createDirectories(dir);
			Path pack = dir.resolve(PACK);
			if (!Files.exists(pack)) {
				try (InputStream in = Shaders.class.getResourceAsStream("/retromon/shaders/" + PACK)) {
					if (in == null) return;
					Files.copy(in, pack, StandardCopyOption.REPLACE_EXISTING);
				}
			}
			Object cfg = config();
			@SuppressWarnings("unchecked")
			Optional<String> current = (Optional<String>) cfg.getClass().getMethod("getShaderPackName").invoke(cfg);
			boolean enabled = (Boolean) cfg.getClass().getMethod("areShadersEnabled").invoke(cfg);
			if (enabled && current.isPresent() && current.get().startsWith("Tempered-Light")) return;
			previousPack = current.orElse(null);
			previousEnabled = enabled;
			Method setName = cfg.getClass().getMethod("setShaderPackName", String.class);
			setName.invoke(cfg, PACK);
			cfg.getClass().getMethod("setShadersEnabled", boolean.class).invoke(cfg, true);
			cfg.getClass().getMethod("save").invoke(cfg);
			reload();
			switched = true;
		} catch (Throwable t) {
			Retromon.LOG.warn("Couldn't switch Tempered Light on: {}", t.toString());
		}
	}

	/** Leaving: your own shader choice back. */
	public static void leave() {
		if (!switched) return;
		switched = false;
		try {
			Object cfg = config();
			cfg.getClass().getMethod("setShaderPackName", String.class).invoke(cfg, previousPack);
			cfg.getClass().getMethod("setShadersEnabled", boolean.class).invoke(cfg, previousEnabled);
			cfg.getClass().getMethod("save").invoke(cfg);
			reload();
		} catch (Throwable t) {
			Retromon.LOG.warn("Couldn't put your shaders back: {}", t.toString());
		}
	}
}
