package com.insidethecube.retromon.engine;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A move, all 251 with Gold and Silver's numbers (read from the games' own data): its effect (the games'
 * EFFECT_ name, without the prefix), power, type, accuracy (out of 100), PP, the chance of its side effect,
 * its priority (3 Protect, 2 Quick Attack, 1 most, 0 Counter and Roar) and whether it's likely to crit.
 */
public record Move(String id, String name, String effect, int power, Type type, int accuracy, int pp, int chance, int priority, boolean highCrit) {
	public static final Map<String, Move> ALL = new LinkedHashMap<>();

	/** Moves that do damage (the one-hit KOs have no power listed but still hit). */
	public boolean damaging() {
		return power > 0 || effect.equals("OHKO");
	}

	public boolean status() {
		return !damaging();
	}

	/** Whether it's Gen 2 special (by its type). */
	public boolean special() {
		return type.special();
	}

	public static Move get(String id) {
		Move m = ALL.get(id);
		if (m == null) throw new IllegalArgumentException("No move " + id);
		return m;
	}

	public static Move find(String id) {
		return ALL.get(id);
	}

	static {
		for (String[] r : Species.table("moves.tsv")) {
			Move m = new Move(r[0], r[1], r[2], Integer.parseInt(r[3]), Type.of(r[4]), Integer.parseInt(r[5]), Integer.parseInt(r[6]), Integer.parseInt(r[7]),
					Integer.parseInt(r[8]), r[9].equals("1"));
			ALL.put(m.id, m);
		}
	}
}
