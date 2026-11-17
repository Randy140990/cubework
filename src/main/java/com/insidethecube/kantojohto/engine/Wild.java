package com.insidethecube.kantojohto.engine;

import java.util.List;
import java.util.Random;

/**
 * Wild encounter tables: for each way of meeting Pokémon in an area, how often per step, and the slots
 * (species, level range, chance out of 100).
 */
public final class Wild {
	private Wild() {}

	public record Slot(String species, int min, int max, int chance) {}

	public record Table(String area, int ratePer256, List<Slot> slots) {
		public Slot pick(Random rng) {
			int r = rng.nextInt(100), acc = 0;
			for (Slot s : slots) {
				acc += s.chance();
				if (r < acc) return s;
			}
			return slots.get(slots.size() - 1);
		}

		public Mon roll(Random rng) {
			Slot s = pick(rng);
			int level = s.min() + rng.nextInt(s.max() - s.min() + 1);
			return new Mon(Species.get(s.species()), level, rng);
		}
	}

	/** Route 1 in Red, walking in the grass: Pidgey and Rattata, levels 2 to 5. */
	public static final Table ROUTE_1 = new Table("Route 1", 25, List.of(
			new Slot("pidgey", 3, 3, 20), new Slot("rattata", 3, 3, 20), new Slot("rattata", 3, 3, 15), new Slot("rattata", 2, 2, 10),
			new Slot("pidgey", 2, 2, 10), new Slot("pidgey", 3, 3, 10), new Slot("pidgey", 5, 5, 5), new Slot("rattata", 4, 4, 5),
			new Slot("pidgey", 4, 4, 4), new Slot("pidgey", 5, 5, 1)));
}
