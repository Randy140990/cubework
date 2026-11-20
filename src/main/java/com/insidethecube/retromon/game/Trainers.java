package com.insidethecube.retromon.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.insidethecube.retromon.engine.Species;

/**
 * Everyone you can meet in Kanto, from Yellow's maps: the trainers (each map's in order, keyed like
 * "route_3_0"), with their class, team, sight, prize money and what they say before, on losing and after; the
 * gym leaders and the Elite Four with their badges; and the townsfolk with their lines. Also the one-off
 * Pokémon (Snorlax, the birds, the gifts). Placed in the world with /retromon place.
 */
public final class Trainers {
	private Trainers() {}

	public record Member(String species, int level) {}

	/**
	 * title: as the battle shows it. sprite: which of the games' sprites (for the skin). prizeBase: money per
	 * level of their last Pokémon. badge: what a gym leader gives (or null). sight: how many steps ahead they
	 * watch (0: they only battle if you talk to them).
	 */
	public record Trainer(String key, String place, String title, String sprite, String facing, int sight, int prizeBase, List<Member> team,
			String badge, List<String> before, List<String> defeat, List<String> after) {
		public boolean leader() {
			return badge != null;
		}

		/** Gym leaders, the Elite Four and Giovanni get the big battle music. */
		public boolean boss() {
			return badge != null || title.startsWith("ELITE FOUR") || title.contains("GIOVANNI");
		}
	}

	public record Person(String key, String place, String sprite, String facing, String movement, List<String> lines) {
		/** Some of the games' "people" are Pokémon (a PIDGEY in a house): which one, or null. */
		public String species() {
			if (lines.isEmpty()) return null;
			String first = lines.get(0);
			int colon = first.indexOf(':');
			if (colon <= 0) return null;
			String name = first.substring(0, colon).trim();
			for (Species s : Species.ALL.values()) if (s.name().equals(name)) return s.id();
			return null;
		}
	}

	public record Static(String id, String species, int level, String place, String kind, String note) {}

	public static final Map<String, Trainer> ALL = new LinkedHashMap<>();
	public static final Map<String, Person> PEOPLE = new LinkedHashMap<>();

	/** A sign on the games' maps: where it stands (in steps) and what it says. */
	public record Sign(String key, String place, int x, int y, List<String> lines) {}

	private static List<Sign> signs;

	public static List<Sign> signsIn(String place) {
		if (signs == null) {
			signs = new java.util.ArrayList<>();
			for (String[] r : Species.table("signs.tsv")) {
				if (r.length < 5 || r[0].startsWith("#")) continue;
				try {
					signs.add(new Sign(r[0], r[1], Integer.parseInt(r[2].trim()), Integer.parseInt(r[3].trim()), lines(r[4])));
				} catch (NumberFormatException ignored) {
					// A header or a stray line.
				}
			}
		}
		return signs.stream().filter(sg -> sg.place().equals(place)).toList();
	}
	public static final Map<String, Static> STATICS = new LinkedHashMap<>();
	/** Where each trainer and person stood in the games: their map's x, y in steps. */
	public static final Map<String, int[]> POS = new LinkedHashMap<>();

	/** An item lying in a Poké Ball on the map (the games' item balls). */
	public record FieldItem(String key, String place, String item, int x, int y) {}

	public static final Map<String, FieldItem> FIELD_ITEMS = new LinkedHashMap<>();

	public static List<Person> peopleIn(String place) {
		return PEOPLE.values().stream().filter(p -> p.place().equals(place)).toList();
	}

	public static List<FieldItem> itemsIn(String place) {
		return FIELD_ITEMS.values().stream().filter(i -> i.place().equals(place)).toList();
	}

	private static int num(String[] r, int i) {
		try {
			return i < r.length ? Integer.parseInt(r[i].trim()) : -1;
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	private static List<String> lines(String s) {
		if (s == null || s.equals("-") || s.isEmpty()) return List.of();
		List<String> out = new ArrayList<>();
		for (String l : s.split(" \\| ")) if (!l.isBlank()) out.add(l.trim());
		return List.copyOf(out);
	}

	static {
		// The Trainer House in Viridian (HeartGold and SoulSilver's): today's challenger downstairs (Game.trainerHouse
		// makes who it is and their team each day; this is how they look).
		ALL.put("trainer_house", new Trainer("trainer_house", "viridian_city", "COOLTRAINER", "cooltrainer_m", "down", 0, 40, List.of(new Member("rattata", 5)),
				null, List.of(), List.of(), List.of()));
		// A practice trainer for Route 1 (Gold and Silver's Youngster Joey).
		ALL.put("youngster_joey", new Trainer("youngster_joey", "route_1", "YOUNGSTER JOEY", "youngster", "down", 4, 16, List.of(new Member("rattata", 4)),
				null, List.of("Hey! You have POKéMON!", "Come on! Let's battle 'em!"), List.of("I lost..."),
				List.of("My RATTATA's in the top percentage of RATTATA, though!")));
		for (String[] r : Species.table("trainers.tsv")) {
			List<Member> team = new ArrayList<>();
			for (String m : r[7].split(",")) {
				String[] p = m.split(":");
				if (p.length == 2 && Species.ALL.containsKey(p[0])) team.add(new Member(p[0], Integer.parseInt(p[1])));
			}
			if (team.isEmpty()) continue;
			ALL.put(r[0], new Trainer(r[0], r[1], r[2], r[3], r[4], Integer.parseInt(r[5]), Integer.parseInt(r[6]), List.copyOf(team),
					r[8].equals("-") ? null : r[8], lines(r[9]), lines(r[10]), lines(r[11])));
			POS.put(r[0], new int[] {num(r, 12), num(r, 13)});
		}
		for (String[] r : Species.table("people.tsv")) {
			PEOPLE.put(r[0], new Person(r[0], r[1], r[2], r[3], r[4], lines(r[5])));
			POS.put(r[0], new int[] {num(r, 6), num(r, 7)});
		}
		for (String[] r : Species.table("field_items.tsv")) {
			// The Power Plant's "items" that are really VOLTORB and ELECTRODE stay as they are in the statics.
			if (Species.ALL.containsKey(r[2])) continue;
			FIELD_ITEMS.put(r[0], new FieldItem(r[0], r[1], r[2], num(r, 3), num(r, 4)));
		}
		for (String[] r : Species.table("statics.tsv")) {
			if (Species.ALL.containsKey(r[1])) STATICS.put(r[0], new Static(r[0], r[1], Integer.parseInt(r[2]), r[3], r[4], r[5]));
		}
	}

	public static Trainer get(String key) {
		return key == null ? null : ALL.get(key);
	}

	/** The trainers of a place, in the games' order. */
	public static List<Trainer> in(String place) {
		return ALL.values().stream().filter(t -> t.place().equals(place)).toList();
	}

	public static List<String> sprites() {
		List<String> out = new ArrayList<>();
		for (Trainer t : ALL.values()) if (!out.contains(t.sprite())) out.add(t.sprite());
		for (Person p : PEOPLE.values()) if (!out.contains(p.sprite())) out.add(p.sprite());
		return out;
	}

	@SuppressWarnings("unused")
	private static List<String> list(String... s) {
		return Arrays.asList(s);
	}
}
