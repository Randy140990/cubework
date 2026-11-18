package com.insidethecube.retromon.engine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A kind of Pokémon, all 251 with Gold and Silver's numbers (read from the games' own data): base stats (HP,
 * Attack, Defense, Speed, Special Attack, Special Defense), types, catch rate, base EXP, growth rate, gender
 * ratio, how it evolves, the moves it learns by level and the TMs and HMs it can use.
 */
public record Species(String id, String name, int dex, Type type1, Type type2, int hp, int atk, int def, int spe, int spa, int spd, int catchRate, int baseExp,
		Growth growth, int gender, List<Evolution> evolutions, List<Learn> learnset, Set<String> tmhm) {

	public enum Growth {
		MEDIUM_FAST(1, 1, 0, 0, 0), SLIGHTLY_FAST(3, 4, 10, 0, 30), SLIGHTLY_SLOW(3, 4, 20, 0, 70), MEDIUM_SLOW(6, 5, -15, 100, 140), FAST(4, 5, 0, 0, 0),
		SLOW(5, 4, 0, 0, 0);

		private final int num, den, sq, lin, sub;

		Growth(int num, int den, int sq, int lin, int sub) {
			this.num = num;
			this.den = den;
			this.sq = sq;
			this.lin = lin;
			this.sub = sub;
		}

		/** Total EXP needed to reach this level (the games' formula). */
		public int expAt(int level) {
			if (level <= 1) return 0;
			long n = level;
			return (int) Math.max(0, num * n * n * n / den + sq * n * n + lin * n - sub);
		}
	}

	/** How it evolves: by level, with an item, by trade, when it's happy, or (Tyrogue) by its Attack and Defense. */
	public record Evolution(String method, String param, String into) {}

	public record Learn(int level, String move) {}

	public static final Map<String, Species> ALL = new LinkedHashMap<>();
	private static final Map<Integer, Species> BY_DEX = new LinkedHashMap<>();

	public Type type(int i) {
		return i == 0 ? type1 : type2;
	}

	public boolean is(Type t) {
		return type1 == t || type2 == t;
	}

	/** The name as the games print it in a sentence ("BULBASAUR"). */
	public String upper() {
		return name;
	}

	/** The moves it knows at this level: the last four it learned on the way up. */
	public List<String> movesAt(int level) {
		List<String> out = new ArrayList<>();
		for (Learn l : learnset) {
			if (l.level > level) continue;
			if (out.contains(l.move)) continue;
			out.add(l.move);
			if (out.size() > 4) out.remove(0);
		}
		return out;
	}

	/** The moves it learns at exactly this level. */
	public List<String> learnsAt(int level) {
		List<String> out = new ArrayList<>();
		for (Learn l : learnset) if (l.level == level && !out.contains(l.move)) out.add(l.move);
		return out;
	}

	public static Species get(String id) {
		Species s = ALL.get(id);
		if (s == null) throw new IllegalArgumentException("No species " + id);
		return s;
	}

	public static Species byDex(int dex) {
		return BY_DEX.get(dex);
	}

	private static java.util.Map<String, Set<String>> tmhm1;

	/** Whether a TM or HM of this move can be taught to it (Gold/Silver's lists, plus Yellow's for the first 151). */
	public boolean canLearnByMachine(String move) {
		if (tmhm.contains(move)) return true;
		if (tmhm1 == null) {
			java.util.Map<String, Set<String>> m = new java.util.HashMap<>();
			for (String[] r : table("tmhm1.tsv")) m.put(r[0], new java.util.HashSet<>(java.util.Arrays.asList(r.length > 1 ? r[1].split(",") : new String[0])));
			tmhm1 = m;
		}
		return tmhm1.getOrDefault(id, Set.of()).contains(move);
	}

	public static List<String[]> table(String name) {
		List<String[]> rows = new ArrayList<>();
		try (InputStream in = Species.class.getResourceAsStream("/retromon/data/" + name)) {
			if (in == null) throw new IllegalStateException("Missing data file " + name);
			BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			String line;
			while ((line = r.readLine()) != null) {
				if (line.isEmpty() || line.startsWith("#")) continue;
				rows.add(line.split("\t", -1));
			}
		} catch (IOException e) {
			throw new IllegalStateException("Couldn't read " + name, e);
		}
		return rows;
	}

	static {
		for (String[] r : table("species.tsv")) {
			List<Evolution> evos = new ArrayList<>();
			if (!r[15].equals("-")) {
				for (String e : r[15].split("\\|")) {
					String[] p = e.split(":");
					// stat:20:lt:hitmonchan keeps both the level and the comparison.
					if (p.length == 4) evos.add(new Evolution(p[0], p[1] + ":" + p[2], p[3]));
					else evos.add(new Evolution(p[0], p[1], p[2]));
				}
			}
			List<Learn> learn = new ArrayList<>();
			for (String l : r[16].split(",")) {
				if (l.isEmpty()) continue;
				String[] p = l.split(":");
				learn.add(new Learn(Integer.parseInt(p[0]), p[1]));
			}
			Set<String> tm = new LinkedHashSet<>();
			if (!r[17].equals("-")) Collections.addAll(tm, r[17].split(","));
			Species s = new Species(r[1], r[2], Integer.parseInt(r[0]), Type.of(r[3]), Type.of(r[4]), Integer.parseInt(r[5]), Integer.parseInt(r[6]),
					Integer.parseInt(r[7]), Integer.parseInt(r[8]), Integer.parseInt(r[9]), Integer.parseInt(r[10]), Integer.parseInt(r[11]), Integer.parseInt(r[12]),
					Growth.valueOf(r[13]), Integer.parseInt(r[14]), List.copyOf(evos), List.copyOf(learn), Collections.unmodifiableSet(tm));
			ALL.put(s.id, s);
			BY_DEX.put(s.dex, s);
		}
	}
}
