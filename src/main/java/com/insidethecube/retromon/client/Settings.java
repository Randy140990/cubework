package com.insidethecube.retromon.client;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import com.insidethecube.retromon.Retromon;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Your own settings for Retromon on this computer (config/retromon-client.properties): the music volume, whether
 * the Pokédex reads its entries aloud, and how bright nights are.
 */
public final class Settings {
	private Settings() {}

	private static Properties props;

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("retromon-client.properties");
	}

	private static Properties props() {
		if (props == null) {
			props = new Properties();
			try (Reader r = Files.newBufferedReader(file())) {
				props.load(r);
			} catch (Exception ignored) {
				// No settings yet: the defaults.
			}
		}
		return props;
	}

	public static int get(String key, int fallback, int min, int max) {
		try {
			return Math.max(min, Math.min(max, Integer.parseInt(props().getProperty(key, Integer.toString(fallback)).trim())));
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	public static void set(String key, int value) {
		if (key.equals("look")) look = value;
		props().setProperty(key, Integer.toString(value));
		try {
			Files.createDirectories(file().getParent());
			try (Writer w = Files.newBufferedWriter(file())) {
				props().store(w, "Retromon client settings");
			}
		} catch (Exception e) {
			Retromon.LOG.warn("Couldn't save the settings: {}", e.toString());
		}
	}

	private static int look = -1;

	/** POKéMON LOOK: Cobblemon's models (0) or Retromon's own creatures where there's one (1, the default). */
	public static int look() {
		if (look < 0) {
			// Retromon's own creatures are the default now: switched over once, even if an older version saved COBBLEMON.
			if (get("look_ours_default", 0, 0, 1) == 0) {
				set("look", 1);
				set("look_ours_default", 1);
			}
			look = get("look", 1, 0, 1);
		}
		return look;
	}

	/** SOUND: how loud cries and menu sounds are, from OFF (0) to MAX (4, the default). */
	public static int sound() {
		return get("sound", 4, 0, 4);
	}

	/** BATTLE EFFECTS: moves play their animations in battle (1, the default) or not (0). */
	public static boolean effects() {
		return get("effects", 1, 0, 1) == 1;
	}

	/** BATTLE STYLE: SWITCH (0, the default) asks whether you'll change POKéMON when a trainer's about to send out their next; SET (1) doesn't. */
	public static boolean switchStyle() {
		return get("style", 0, 0, 1) == 0;
	}

	/** SHADERS: RETROMON (1, the default: Retromon Light) or OFF (0: Minecraft's own light). */
	public static int shaderMode() {
		return get("shaders", 1, 0, 2) == 0 ? 0 : 1;
	}

	public static boolean shaders() {
		return shaderMode() != 0;
	}

	/** The Pokédex reads entries aloud (1) or not (0). */
	public static boolean voice() {
		return get("voice", 1, 0, 1) == 1;
	}

	/** Wild Pokémon: seen in the grass, walked into (1, the default, as Let's Go), or the classic hidden ones (0). */
	public static boolean seen() {
		return get("seen", 1, 0, 1) == 1;
	}

	/** One of your Pokémon walks with you (1, the default) or not (0). */
	public static boolean follower() {
		return get("follow", 1, 0, 1) == 1;
	}

	/** Tells the server how you like things (it does the nights, the wild Pokémon and your follower). */
	public static void sendPrefs() {
		net.minecraft.nbt.CompoundTag t = new net.minecraft.nbt.CompoundTag();
		t.putInt("nights", nights());
		t.putBoolean("seen", seen());
		t.putBoolean("follow", follower());
		t.putBoolean("cycle", cycle());
		t.putBoolean("battles", battles());
		t.putBoolean("travel", fastTravel());
		net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new com.insidethecube.retromon.game.Net.Act("prefs", t));
	}

	/** BATTLES: on (1, the default), or off (0: no wild POKéMON or trainers battle you; the story's battles still happen). */
	public static boolean battles() {
		return get("battles", 1, 0, 1) == 1;
	}

	/** FAST TRAVEL: off (0, the default), or on (1: the TOWN MAP takes you anywhere on it). */
	public static boolean fastTravel() {
		return get("travel", 0, 0, 1) == 1;
	}

	/** DAY AND NIGHT: the day goes by (1, the default) or it's held at half past ten in the morning (0). */
	public static boolean cycle() {
		return get("cycle", 1, 0, 1) == 1;
	}

	/** Nights: 0 dark (as Minecraft), 1 soft (the default: a blue, moonlit night you can see in), 2 bright. */
	public static int nights() {
		return get("nights", 1, 0, 2);
	}
}
