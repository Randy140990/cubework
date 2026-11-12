package com.insidethecube.cubework.world;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

/**
 * config/cubework.json. smoothNewTerrain (off by default): new land is generated with its steps smoothed
 * into slopes. smoothSteepness: "steep", "natural" (the default) or "gentle".
 */
public final class Config {
	private Config() {}

	public static boolean smoothNewTerrain = false;
	public static int smoothSteepness = 1;

	private static final String[] STEEPNESS = {"steep", "natural", "gentle"};

	public static void load() {
		Path file = FabricLoader.getInstance().getConfigDir().resolve("cubework.json");
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		if (Files.exists(file)) {
			try (Reader r = Files.newBufferedReader(file)) {
				JsonObject o = gson.fromJson(r, JsonObject.class);
				if (o != null && o.has("smoothNewTerrain")) smoothNewTerrain = o.get("smoothNewTerrain").getAsBoolean();
				if (o != null && o.has("smoothSteepness")) {
					String s = o.get("smoothSteepness").getAsString();
					for (int i = 0; i < STEEPNESS.length; i++) if (STEEPNESS[i].equalsIgnoreCase(s)) smoothSteepness = i;
				}
			} catch (Exception e) {
				// A broken file keeps the defaults.
			}
		}
		JsonObject o = new JsonObject();
		o.addProperty("smoothNewTerrain", smoothNewTerrain);
		o.addProperty("smoothSteepness", STEEPNESS[smoothSteepness]);
		o.addProperty("_help", "smoothNewTerrain: smooth the steps of newly generated land into slopes. smoothSteepness: steep, natural or gentle.");
		try (Writer w = Files.newBufferedWriter(file)) {
			gson.toJson(o, w);
		} catch (IOException e) {
			// Read-only config folder: nothing to do.
		}
	}
}
