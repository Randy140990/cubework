package com.insidethecube.retromon.client;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

import com.insidethecube.retromon.Retromon;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

/**
 * The shaders, built in. When you start a Retromon game and Iris is installed, Retromon Light (ours, light and
 * quick: the light moves with the day, plants sway, water shows the sky, and rooms inside keep their own light) is
 * put in your shaderpacks folder and switched on, unless OPTION's SHADERS is OFF (Minecraft's own light). Your own
 * choice comes back when you leave. Without Iris, nothing happens and the game runs plain.
 */
public final class Shaders {
	private Shaders() {}

	public static final String RETROMON = "Retromon-Light-1.2.6.zip";
	private static boolean switched;
	private static String previousPack;
	private static boolean previousEnabled;

	/** The pack for a SHADERS setting (1 Retromon Light), or null for OFF. */
	public static String pack(int mode) {
		return mode >= 1 ? RETROMON : null;
	}

	private static Object config() throws Exception {
		Class<?> iris = Class.forName("net.irisshaders.iris.Iris");
		return iris.getMethod("getIrisConfig").invoke(null);
	}

	private static void reload() throws Exception {
		Class.forName("net.irisshaders.iris.Iris").getMethod("reload").invoke(null);
	}

	/** Puts the pack Retromon carries in the shaderpacks folder (again if it's changed). False if it couldn't. */
	private static boolean install(String name) throws Exception {
		Path dir = Minecraft.getInstance().gameDirectory.toPath().resolve("shaderpacks");
		Files.createDirectories(dir);
		Path pack = dir.resolve(name);
		try (InputStream in = Shaders.class.getResourceAsStream("/retromon/shaders/" + name)) {
			if (in == null) return Files.exists(pack);
			byte[] bytes = in.readAllBytes();
			if (Files.exists(pack) && Files.size(pack) == bytes.length) return true;
			Path tmp = dir.resolve(name + ".part");
			Files.write(tmp, bytes);
			Files.move(tmp, pack, StandardCopyOption.REPLACE_EXISTING);
		}
		return true;
	}

	/** OPTION's SHADERS, switched now: 0 off (Minecraft's own light), 1 Retromon Light. */
	public static void set(int mode) {
		if (!FabricLoader.getInstance().isModLoaded("iris")) return;
		try {
			Object cfg = config();
			if (!switched) {
				@SuppressWarnings("unchecked")
				Optional<String> current = (Optional<String>) cfg.getClass().getMethod("getShaderPackName").invoke(cfg);
				previousPack = current.orElse(null);
				previousEnabled = (Boolean) cfg.getClass().getMethod("areShadersEnabled").invoke(cfg);
			}
			String name = pack(mode);
			boolean on = name != null && install(name);
			if (on) cfg.getClass().getMethod("setShaderPackName", String.class).invoke(cfg, name);
			cfg.getClass().getMethod("setShadersEnabled", boolean.class).invoke(cfg, on);
			cfg.getClass().getMethod("save").invoke(cfg);
			reload();
			switched = true;
		} catch (Throwable t) {
			Retromon.LOG.warn("Couldn't switch the shaders to {}: {}", mode, t.toString());
		}
	}

	/** Entering a Retromon save: the pack OPTION names on (if Iris is here and it isn't already), or off. */
	public static void enter() {
		if (switched || !FabricLoader.getInstance().isModLoaded("iris")) return;
		int mode = Settings.shaderMode();
		String name = pack(mode);
		try {
			Object cfg = config();
			@SuppressWarnings("unchecked")
			Optional<String> current = (Optional<String>) cfg.getClass().getMethod("getShaderPackName").invoke(cfg);
			boolean enabled = (Boolean) cfg.getClass().getMethod("areShadersEnabled").invoke(cfg);
			if (name != null && enabled && current.isPresent() && current.get().equals(name)) {
				// Already on; still put in the latest copy of the pack if Retromon's has changed.
				previousPack = name;
				previousEnabled = true;
				Path have = Minecraft.getInstance().gameDirectory.toPath().resolve("shaderpacks").resolve(name);
				long size = Files.exists(have) ? Files.size(have) : -1;
				install(name);
				if (Files.size(have) != size) reload();
				return;
			}
		} catch (Throwable t) {
			Retromon.LOG.warn("Couldn't read the shader settings: {}", t.toString());
		}
		set(mode);
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
