package com.lumenforge.lens;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lumenforge.lens.core.Ffmpeg;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Lightlens: names, folders and the settings file. */
public final class Lens {
	public static final String ID = "lightlens";
	public static final String NAME = "Lightlens";
	public static final Logger LOG = LoggerFactory.getLogger(NAME);
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public static Settings settings = new Settings();

	private Lens() {}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(ID, path);
	}

	public static String version() {
		return FabricLoader.getInstance().getModContainer(ID).map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("");
	}

	/** .minecraft/lightlens */
	public static Path root() {
		return FabricLoader.getInstance().getGameDir().resolve("lightlens");
	}

	/** Where a recording's picture is written while you record (on the game's drive, never a synced folder). */
	public static Path work() {
		return root().resolve("work");
	}

	public static Path tools() {
		return root().resolve("tools");
	}

	/** The folder you picked, or .minecraft/lightlens/captures until you pick one. */
	public static Path output() {
		String f = settings.folder;
		if (f != null && !f.isBlank()) {
			try {
				return Path.of(f);
			} catch (Exception ignored) {
			}
		}
		return root().resolve("captures");
	}

	public static Path photos() {
		return settings.photosInSubfolder ? output().resolve("Photos") : output();
	}

	public static Path gifs() {
		return settings.photosInSubfolder ? output().resolve("GIFs") : output();
	}

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("lightlens.json");
	}

	public static void load() {
		try {
			Path f = file();
			if (Files.isRegularFile(f)) {
				Settings s = GSON.fromJson(Files.readString(f, StandardCharsets.UTF_8), Settings.class);
				if (s != null) settings = s;
			}
		} catch (Exception e) {
			LOG.warn("Couldn't read the Lightlens settings, using the defaults: {}", e.toString());
		}
		settings.fix();
		setupFfmpeg();
	}

	public static void save() {
		try {
			Files.createDirectories(file().getParent());
			Files.writeString(file(), GSON.toJson(settings), StandardCharsets.UTF_8);
		} catch (Exception e) {
			LOG.warn("Couldn't save the Lightlens settings: {}", e.toString());
		}
	}

	/** FFmpeg: ours, or the copy Lightreel downloaded, or one on the PATH. */
	public static void setupFfmpeg() {
		Path game = FabricLoader.getInstance().getGameDir();
		Ffmpeg.setup(tools(), settings.ffmpegPath, game.resolve("lightreel").resolve("tools").resolve("ffmpeg"));
	}
}
