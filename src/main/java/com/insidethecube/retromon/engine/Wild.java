package com.insidethecube.retromon.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Wild encounter tables, from the original games' data: Kanto from Yellow (with what Yellow lacks from Red
 * and Blue, so all 151 can be caught), Johto from Silver (by morning, day and night). For each place and way
 * of meeting them (grass, water, the rods): how often per step (out of 256), and the slots (species, level,
 * weight).
 */
public final class Wild {
	private Wild() {}

	public record Slot(String species, int level, int weight) {}

	public record Table(String place, String method, String time, int ratePer256, List<Slot> slots) {
		public Slot pick(Random rng) {
			int total = 0;
			for (Slot s : slots) total += s.weight();
			int r = rng.nextInt(Math.max(1, total)), acc = 0;
			for (Slot s : slots) {
				acc += s.weight();
				if (r < acc) return s;
			}
			return slots.get(slots.size() - 1);
		}

		public Mon roll(Random rng) {
			Slot s = pick(rng);
			return new Mon(Species.get(s.species()), s.level(), rng);
		}
	}

	private static final Map<String, List<Table>> BY_PLACE = new HashMap<>();

	/** The table for a place, way and time of day ("morn", "day" or "nite"); falls back to "any". Null if there's none. */
	public static Table get(String place, String method, String time) {
		Table t = find(BY_PLACE.get(place), method, time);
		return t != null ? t : find(BY_PLACE.get("*kanto"), method, time);
	}

	private static Table find(List<Table> list, String method, String time) {
		if (list == null) return null;
		Table any = null;
		for (Table t : list) {
			if (!t.method.equals(method)) continue;
			if (t.time.equals(time)) return t;
			if (t.time.equals("any") || any == null) any = t;
		}
		return any;
	}

	public static boolean has(String place) {
		return BY_PLACE.containsKey(place);
	}

	/** The time of day the games use, from Minecraft's (0 is 6am): morning 4 to 10, day 10 to 6pm, night after. */
	public static String timeOfDay(long dayTime) {
		int hour = (int) ((dayTime / 1000 + 6) % 24);
		return hour >= 4 && hour < 10 ? "morn" : hour >= 10 && hour < 18 ? "day" : "nite";
	}

	static {
		for (String[] r : Species.table("wild.tsv")) {
			List<Slot> slots = new ArrayList<>();
			for (String s : r[4].split(",")) {
				String[] p = s.split(":");
				if (p.length == 3 && Species.ALL.containsKey(p[0])) slots.add(new Slot(p[0], Integer.parseInt(p[1]), Integer.parseInt(p[2])));
			}
			if (slots.isEmpty()) continue;
			BY_PLACE.computeIfAbsent(r[0], k -> new ArrayList<>()).add(new Table(r[0], r[1], r[2], Integer.parseInt(r[3]), List.copyOf(slots)));
		}
	}
}
