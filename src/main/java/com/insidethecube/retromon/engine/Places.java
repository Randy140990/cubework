package com.insidethecube.retromon.engine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The towns, routes and dungeons of Kanto (from Yellow) and Johto (from Silver): the name the games show,
 * the music that plays there and its wild Pokémon. Marts and what they sell (Yellow's lists) are here too.
 */
public final class Places {
	private Places() {}

	public record Place(String id, String region, String name, String music, String wild) {}

	public record Mart(String id, String place, List<String> items) {}

	public static final Map<String, Place> ALL = new LinkedHashMap<>();
	public static final Map<String, Mart> MARTS = new LinkedHashMap<>();

	public static Place get(String id) {
		return id == null ? null : ALL.get(id);
	}

	/** The Mart for a place (its first counter), or the Viridian one. */
	public static Mart martFor(String place) {
		Mart m = MARTS.get(place);
		if (m != null) return m;
		for (Mart x : MARTS.values()) if (x.place().equals(place)) return x;
		return MARTS.get("viridian_city");
	}

	static {
		for (String[] r : Species.table("places.tsv")) {
			// Johto's maps reuse some Kanto ids (its Kanto half): the first (Yellow's) wins.
			ALL.putIfAbsent(r[0], new Place(r[0], r[1], r[2], r[3], r[4]));
		}
		for (String[] r : Species.table("marts.tsv")) {
			List<String> items = new ArrayList<>();
			for (String i : r[2].split(",")) if (Item.ALL.containsKey(i)) items.add(i);
			MARTS.put(r[0], new Mart(r[0], r[1], List.copyOf(items)));
		}
	}
}
