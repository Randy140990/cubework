package com.insidethecube.kantojohto.engine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A kind of Pokémon, with Silver's numbers: base stats (HP, Attack, Defense, Speed, Special Attack, Special
 * Defense), catch rate, base EXP, growth rate and the moves it learns by level.
 */
public record Species(String id, String name, int dex, Type type1, Type type2, int hp, int atk, int def, int spe, int spa, int spd, int catchRate, int baseExp,
		Growth growth, List<int[]> learnLevels, List<String> learnMoves) {
	public enum Growth {
		MEDIUM_FAST, MEDIUM_SLOW;

		/** Total EXP needed to reach this level. */
		public int expAt(int level) {
			if (level <= 1) return 0;
			long n = level;
			return switch (this) {
				case MEDIUM_FAST -> (int) (n * n * n);
				case MEDIUM_SLOW -> (int) Math.max(0, (6 * n * n * n) / 5 - 15 * n * n + 100 * n - 140);
			};
		}
	}

	public static final Map<String, Species> ALL = new LinkedHashMap<>();

	/** The moves it knows at this level: the last four it learned on the way up. */
	public List<String> movesAt(int level) {
		List<String> out = new ArrayList<>();
		for (int i = 0; i < learnMoves.size(); i++) {
			if (learnLevels.get(i)[0] > level) continue;
			String m = learnMoves.get(i);
			if (out.contains(m)) continue;
			out.add(m);
			if (out.size() > 4) out.remove(0);
		}
		return out;
	}

	/** The moves it learns at exactly this level. */
	public List<String> learnsAt(int level) {
		List<String> out = new ArrayList<>();
		for (int i = 0; i < learnMoves.size(); i++) if (learnLevels.get(i)[0] == level) out.add(learnMoves.get(i));
		return out;
	}

	private static void add(String id, String name, int dex, Type t1, Type t2, int[] stats, int catchRate, int baseExp, Growth g, Object... learn) {
		List<int[]> lv = new ArrayList<>();
		List<String> mv = new ArrayList<>();
		for (int i = 0; i < learn.length; i += 2) {
			lv.add(new int[] {(Integer) learn[i]});
			mv.add((String) learn[i + 1]);
		}
		ALL.put(id, new Species(id, name, dex, t1, t2, stats[0], stats[1], stats[2], stats[3], stats[4], stats[5], catchRate, baseExp, g, lv, mv));
	}

	static {
		add("bulbasaur", "Bulbasaur", 1, Type.GRASS, Type.POISON, new int[] {45, 49, 49, 45, 65, 65}, 45, 64, Growth.MEDIUM_SLOW,
				1, "tackle", 4, "growl", 7, "leech_seed", 10, "vine_whip", 15, "poison_powder", 15, "sleep_powder", 20, "razor_leaf");
		add("charmander", "Charmander", 4, Type.FIRE, null, new int[] {39, 52, 43, 65, 60, 50}, 45, 65, Growth.MEDIUM_SLOW,
				1, "scratch", 1, "growl", 7, "ember", 13, "smokescreen");
		add("squirtle", "Squirtle", 7, Type.WATER, null, new int[] {44, 48, 65, 43, 50, 64}, 45, 66, Growth.MEDIUM_SLOW,
				1, "tackle", 4, "tail_whip", 7, "bubble", 10, "withdraw", 13, "water_gun");
		add("pidgey", "Pidgey", 16, Type.NORMAL, Type.FLYING, new int[] {40, 45, 40, 56, 35, 35}, 255, 55, Growth.MEDIUM_SLOW,
				1, "tackle", 5, "sand_attack", 9, "gust", 15, "quick_attack");
		add("rattata", "Rattata", 19, Type.NORMAL, null, new int[] {30, 56, 35, 72, 25, 35}, 255, 57, Growth.MEDIUM_FAST,
				1, "tackle", 1, "tail_whip", 7, "quick_attack", 13, "hyper_fang");
		add("geodude", "Geodude", 74, Type.ROCK, Type.GROUND, new int[] {40, 80, 100, 20, 30, 30}, 255, 86, Growth.MEDIUM_SLOW,
				1, "tackle", 6, "defense_curl", 11, "rock_throw");
		add("onix", "Onix", 95, Type.ROCK, Type.GROUND, new int[] {35, 45, 160, 70, 30, 45}, 45, 108, Growth.MEDIUM_FAST,
				1, "tackle", 1, "screech", 14, "rock_throw");
	}

	public static Species get(String id) {
		Species s = ALL.get(id);
		if (s == null) throw new IllegalArgumentException("No species " + id);
		return s;
	}
}
