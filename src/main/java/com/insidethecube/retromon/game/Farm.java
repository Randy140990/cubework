package com.insidethecube.retromon.game;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.insidethecube.retromon.Retromon;

/**
 * (0.67, John's 16:07: "with these new ones, what we've created, the new creatures, etc., I need those in the actual
 * release as well ... when I tell you about placements, they're already in the mod") Retromon's farm and pet creatures
 * (tools/mons11.py; John's 13:27): the cattle, sheep, pigs, chickens and horses (each young one growing into a she or
 * a he) and the dog and the cat in their coats (14:40). Not caught, not wild, never battled: they stand about where
 * they're put (/retromon place farm, or a place's scenery), and press A and they make their sound and you hear what
 * they're like. Their data is retromon/data/farm.tsv; their models are with the rest (mons/<sid>).
 */
public final class Farm {
	private Farm() {}

	/** One: its id, name, its line (Cattle, Sheep, ... Dog, Cat), stage (Young, Grown, Pet), she/he/either, its coat. */
	public record Kind(String id, String name, String line, String stage, String sex, String grows, String get, String does, String coat) {
		/** What it says when you press A at it. */
		public String sound() {
			return switch (line) {
				case "Cattle" -> stage.equals("Young") ? "Moo?" : id.equals("brawnox") ? "MOOOO!" : "Mooo.";
				case "Sheep" -> stage.equals("Young") ? "Meh-eh!" : "Baaa.";
				case "Pigs" -> stage.equals("Young") ? "Wee wee!" : "Oink oink.";
				case "Chickens" -> stage.equals("Young") ? "Peep peep!" : id.equals("crowcomb") ? "Cock-a-doodle-doo!" : "Bawk bawk.";
				case "Horses" -> stage.equals("Young") ? "Whinny!" : "Neighhh.";
				case "Dog" -> "Woof!";
				case "Cat" -> "Mew.";
				default -> "...";
			};
		}

		/** Minecraft's own sound for its kind of animal, and how high (the young ones' higher). */
		public String noise() {
			return switch (line) {
				case "Cattle" -> "entity.cow.ambient";
				case "Sheep" -> "entity.sheep.ambient";
				case "Pigs" -> "entity.pig.ambient";
				case "Chickens" -> "entity.chicken.ambient";
				case "Horses" -> "entity.horse.ambient";
				case "Dog" -> "entity.wolf.ambient";
				case "Cat" -> "entity.cat.ambient";
				default -> null;
			};
		}

		public float pitch() {
			return stage.equals("Young") ? 1.35F : id.equals("brawnox") || id.equals("tuskhog") ? 0.85F : 1F;
		}

		/** Its lines when you press A: its sound, then what it does (and a pet's coat). */
		public List<String> lines() {
			String first = name + ": " + sound();
			String what = coat != null && !coat.isEmpty() ? "A " + coat.split(":")[0].toLowerCase() + " " + (line.equals("Dog") ? "dog" : "cat") + ". " + does : does;
			return List.of(first, what);
		}
	}

	public static final Map<String, Kind> ALL;

	static {
		Map<String, Kind> m = new LinkedHashMap<>();
		try (InputStream raw = Farm.class.getResourceAsStream("/retromon/data/farm.tsv")) {
			if (raw != null) {
				BufferedReader in = new BufferedReader(new InputStreamReader(raw, StandardCharsets.UTF_8));
				String line;
				while ((line = in.readLine()) != null) {
					if (line.isBlank() || line.startsWith("#")) continue;
					String[] f = line.split("\t", -1);
					if (f.length < 10) continue;
					m.put(f[0], new Kind(f[0], f[1], f[2], f[3], f[4], f[7], f[8], f[9], f.length > 10 ? f[10] : ""));
				}
			}
		} catch (Exception e) {
			Retromon.LOG.warn("Retromon: couldn't read the farm's creatures: {}", e.toString());
		}
		ALL = Collections.unmodifiableMap(m);
	}

	public static Kind get(String id) {
		return id == null ? null : ALL.get(id);
	}

	public static boolean is(String id) {
		return id != null && ALL.containsKey(id);
	}
}
