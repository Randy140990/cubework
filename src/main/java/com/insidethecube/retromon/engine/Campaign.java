package com.insidethecube.retromon.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * The PARTY PICKER's parties (John, 15:28: "choose where I would be within the Kanto campaign, and for those Pokemon
 * to have the evolution/level associated with that"): for each point in KANTO, a party you could have had by then.
 * Your starter's line always leads; the rest come from where you could have caught them by then (the game's own wild
 * tables, and the gifts: the MAGIKARP salesman's, the EEVEE in CELADON, SNORLAX, LAPRAS, the fossils...), raised to
 * that point's levels, evolved by level and by the stones you could have bought by then, knowing the moves of their
 * level and the HMs you'd have had (CUT, FLY, STRENGTH, SURF). Each roll is a little different.
 *
 * The same in the screen and the game: a party is worked out from (point, starter, roll) alone, so the picker shows
 * exactly what you'll get.
 */
public final class Campaign {
	private Campaign() {}

	/** A point in the story: its name, the line under it, the levels (your lead's, the rest's), how many, and what's new by then. */
	public record Stage(String title, String line, int leadLo, int leadHi, int lo, int hi, int min, int max, String places, String gifts) {}

	/** One of the party, as picked: its species and level. */
	public record Entry(String species, int level) {}

	public static final String[] BADGES = {"BOULDERBADGE", "CASCADEBADGE", "THUNDERBADGE", "RAINBOWBADGE", "SOULBADGE", "MARSHBADGE", "VOLCANOBADGE",
			"EARTHBADGE"};
	public static final String[] STARTERS = {"bulbasaur", "charmander", "squirtle", "pikachu"};

	// Each point: just after that GYM, so the levels sit round its LEADER's strongest (BROCK's ONIX 14, MISTY's STARMIE
	// 21, LT.SURGE's RAICHU 28, ERIKA's 32, KOGA's WEEZING 43, SABRINA's ALAKAZAM 50, BLAINE's ARCANINE 54, GIOVANNI's
	// RHYDON 55, LANCE's DRAGONITE 62), and the places are the ones you'd have been through by then.
	public static final List<Stage> STAGES = List.of(
			new Stage("STARTING OUT", "Fresh from PALLET TOWN, heading into VIRIDIAN FOREST.", 7, 8, 3, 6, 2, 3, "route_1 route_2 route_22", ""),
			new Stage("AFTER BROCK", "1 badge: the BOULDERBADGE. On to MT. MOON.", 15, 16, 10, 13, 3, 4, "viridian_forest", ""),
			new Stage("AFTER MISTY", "2 badges: the CASCADEBADGE. On to VERMILION.", 22, 23, 16, 20, 4, 5, "route_3 route_4 mt_moon_1f mt_moon_b1f mt_moon_b2f route_24 route_25",
					"magikarp:20 abra:4"),
			new Stage("AFTER LT. SURGE", "3 badges: the THUNDERBADGE. CUT in hand.", 29, 30, 23, 27, 5, 6,
					"route_5 route_6 route_11 digletts_cave vermilion_city *kanto", "farfetch_d:5 jynx:4 mr_mime:4"),
			new Stage("AFTER ERIKA", "4 badges: the RAINBOWBADGE. FLY, and the stones from CELADON.", 34, 35, 28, 32, 6, 6,
					"route_9 route_10 rock_tunnel_1f rock_tunnel_b1f route_7 route_8 celadon_city lavender_town", "eevee:30 porygon:5 dratini:8"),
			new Stage("AFTER KOGA", "5 badges: the SOULBADGE. SURF and STRENGTH from the SAFARI ZONE.", 44, 45, 37, 42, 6, 6,
					"pokemon_tower_3f pokemon_tower_4f pokemon_tower_5f pokemon_tower_6f pokemon_tower_7f route_12 route_13 route_14 route_15 route_16 route_17 route_18 "
							+ "fuchsia_city safari_zone_center safari_zone_east safari_zone_north safari_zone_west",
					"snorlax:25"),
			new Stage("AFTER SABRINA", "6 badges: the MARSHBADGE. SILPH CO.'s LAPRAS, the DOJO's gift.", 50, 51, 43, 48, 6, 6,
					"saffron_city route_19 route_20 seafoam_islands_1f seafoam_islands_b1f seafoam_islands_b2f seafoam_islands_b3f seafoam_islands_b4f",
					"lapras:30 hitmonlee:12 hitmonchan:12"),
			new Stage("AFTER BLAINE", "7 badges: the VOLCANOBADGE. The fossils brought back to life.", 54, 55, 47, 52, 6, 6,
					"cinnabar_island pokemon_mansion_1f pokemon_mansion_2f pokemon_mansion_3f pokemon_mansion_b1f route_21 power_plant",
					"omanyte:9 kabuto:9 aerodactyl:7"),
			new Stage("AFTER GIOVANNI", "All 8 badges. On to VICTORY ROAD and the POKéMON LEAGUE.", 57, 58, 50, 55, 6, 6,
					"route_23 victory_road_1f victory_road_2f victory_road_3f", ""),
			new Stage("CHAMPION", "The ELITE FOUR and your rival beaten: the HALL OF FAME.", 66, 68, 58, 64, 6, 6, "", ""));

	/** What you can catch with, from which point: the OLD ROD in VERMILION, the GOOD and SUPER RODs by FUCHSIA, SURF after it. */
	private static int methodFrom(String method) {
		return switch (method) {
			case "grass" -> 0;
			case "old_rod" -> 3;
			case "good_rod", "super_rod" -> 5;
			case "water" -> 6;
			default -> 99;
		};
	}

	/** Which HMs you'd have had: CUT (the S.S.ANNE, before LT. SURGE), FLY (ROUTE 16), STRENGTH and SURF (FUCHSIA). */
	public static List<String> hms(int stage) {
		List<String> out = new ArrayList<>();
		if (stage >= 3) out.add("cut");
		if (stage >= 4) out.add("fly");
		if (stage >= 5) out.add("surf");
		if (stage >= 5) out.add("strength");
		return out;
	}

	/** The badges you'd have had. */
	public static List<String> badges(int stage) {
		return List.of(BADGES).subList(0, Math.min(8, Math.max(0, stage)));
	}

	/** Whether a stone could be had by then: the MOON STONEs in MT. MOON, the rest from CELADON's store. (No SUN STONEs in KANTO.) */
	private static boolean stone(String item, int stage) {
		return switch (item) {
			case "moon_stone" -> stage >= 2;
			case "fire_stone", "water_stone", "thunderstone", "leaf_stone" -> stage >= 4;
			default -> false;
		};
	}

	private static Map<String, String> parent;

	/** Its line's first (KANTO's: PIKACHU, not PICHU). */
	public static String root(String id) {
		if (parent == null) {
			Map<String, String> p = new HashMap<>();
			for (Species s : Species.ALL.values())
				for (Species.Evolution e : s.evolutions()) if (s.dex() <= 151) p.putIfAbsent(e.into(), s.id());
			parent = p;
		}
		String at = id;
		for (int i = 0; i < 4 && parent.containsKey(at); i++) at = parent.get(at);
		return at;
	}

	/** The starter whose line this is, or null. */
	public static String starterOf(String id) {
		String r = root(id);
		for (String s : STARTERS) if (s.equals(r)) return s;
		return null;
	}

	/** Grown to this level (and point): by level, and by a stone if one could be had (most players use it). */
	public static Species grow(Species s, int level, int stage, Random rng, boolean lead) {
		for (int step = 0; step < 3; step++) {
			List<String> can = new ArrayList<>();
			for (Species.Evolution e : s.evolutions()) {
				Species into = Species.ALL.get(e.into());
				if (into == null || into.dex() > 151) continue;
				if (e.method().equals("level")) {
					try {
						if (level >= Integer.parseInt(e.param())) can.add(e.into());
					} catch (NumberFormatException ignored) {}
				} else if (e.method().equals("item") && stone(e.param(), stage) && !(lead && s.id().equals("pikachu"))) can.add(e.into());
			}
			if (can.isEmpty()) break;
			// (A stone's evolution taken most of the time, not always: some keep theirs as they are.)
			boolean byStone = s.evolutions().stream().anyMatch(e -> e.method().equals("item") && can.contains(e.into()));
			if (byStone && rng.nextInt(100) >= 85) break;
			s = Species.get(can.get(rng.nextInt(can.size())));
		}
		return s;
	}

	private static Map<Integer, Map<String, Double>> pools = new HashMap<>();

	/** What you could have caught by then (each line once, by its first), and how often you'd meet it. */
	public static Map<String, Double> pool(int stage) {
		Map<String, Double> cached = pools.get(stage);
		if (cached != null) return cached;
		Map<String, Integer> placeFrom = new HashMap<>();
		Map<String, Double> gifts = new LinkedHashMap<>();
		for (int i = 0; i <= stage && i < STAGES.size(); i++) {
			Stage s = STAGES.get(i);
			for (String pl : s.places().split(" ")) if (!pl.isEmpty()) placeFrom.putIfAbsent(pl, i);
			for (String g : s.gifts().split(" "))
				if (!g.isEmpty()) gifts.put(g.split(":")[0], Double.parseDouble(g.split(":")[1]));
		}
		Map<String, Double> raw = new HashMap<>();
		for (String[] r : Species.table("wild.tsv")) {
			if (r.length < 5 || !placeFrom.containsKey(r[0]) || methodFrom(r[1]) > stage) continue;
			for (String slot : r[4].split(",")) {
				String[] p = slot.split(":");
				if (p.length < 3) continue;
				Species sp = Species.ALL.get(p[0]);
				if (sp == null || sp.dex() > 151) continue;
				String root = root(sp.id());
				if (starterOf(root) != null) continue;
				raw.merge(root, Double.parseDouble(p[2]), Double::sum);
			}
		}
		Map<String, Double> out = new LinkedHashMap<>();
		// (Flattened: the common ones still most often, the rare ones now and then.)
		for (Map.Entry<String, Double> e : raw.entrySet()) out.put(e.getKey(), Math.sqrt(e.getValue()));
		for (Map.Entry<String, Double> e : gifts.entrySet()) out.merge(root(e.getKey()), e.getValue(), Math::max);
		pools.put(stage, out);
		return out;
	}

	private static String pickFrom(Map<String, Double> pool, Set<String> not, Random rng) {
		double total = 0;
		for (Map.Entry<String, Double> e : pool.entrySet()) if (!not.contains(e.getKey())) total += e.getValue();
		if (total <= 0) return null;
		double at = rng.nextDouble() * total;
		String last = null;
		for (Map.Entry<String, Double> e : pool.entrySet()) {
			if (not.contains(e.getKey())) continue;
			last = e.getKey();
			at -= e.getValue();
			if (at <= 0) return e.getKey();
		}
		return last;
	}

	private static int between(Random rng, int lo, int hi) {
		return lo + rng.nextInt(Math.max(1, hi - lo + 1));
	}

	/** A party for this point: your starter's line first, then the rest. */
	public static List<Entry> pick(int stage, String starter, long seed) {
		stage = Math.max(0, Math.min(STAGES.size() - 1, stage));
		Stage st = STAGES.get(stage);
		Random rng = new Random(seed * 31 + stage);
		if (starter == null || Species.ALL.get(starter) == null) starter = "bulbasaur";
		List<Entry> out = new ArrayList<>();
		int lead = between(rng, st.leadLo(), st.leadHi());
		out.add(new Entry(grow(Species.get(starter), lead, stage, rng, true).id(), lead));
		Map<String, Double> pool = pool(stage);
		Set<String> taken = new HashSet<>();
		Map<String, Integer> types = new HashMap<>();
		types.merge(Species.get(out.get(0).species()).type1().name(), 1, Integer::sum);
		int n = between(rng, st.min(), st.max());
		for (int tries = 0; out.size() < n && tries < 60; tries++) {
			String root = pickFrom(pool, taken, rng);
			if (root == null) break;
			int level = between(rng, st.lo(), st.hi());
			Species sp = grow(Species.get(root), level, stage, rng, false);
			// (No more than two of one type: a party, not a gym.)
			if (types.getOrDefault(sp.type1().name(), 0) >= 2) {
				taken.add(root);
				continue;
			}
			taken.add(root);
			types.merge(sp.type1().name(), 1, Integer::sum);
			out.add(new Entry(sp.id(), level));
		}
		// Someone for each HM you'd have had (one of the rest, swapped for one who can, if no one could).
		for (String hm : hms(stage)) {
			boolean someone = out.stream().anyMatch(e -> Species.get(e.species()).canLearnByMachine(hm));
			if (someone || out.size() < 2) continue;
			Map<String, Double> able = new LinkedHashMap<>();
			for (Map.Entry<String, Double> e : pool.entrySet()) {
				if (taken.contains(e.getKey())) continue;
				Species g = grow(Species.get(e.getKey()), st.hi(), stage, new Random(seed + e.getKey().hashCode()), false);
				if (g.canLearnByMachine(hm)) able.put(e.getKey(), e.getValue());
			}
			String root = pickFrom(able, Set.of(), rng);
			if (root == null) continue;
			int level = between(rng, st.lo(), st.hi());
			Species sp = grow(Species.get(root), level, stage, rng, false);
			taken.add(root);
			out.set(out.size() - 1, new Entry(sp.id(), level));
		}
		return out;
	}

	/**
	 * The party itself: each one raised to its level (a trainer's, a little stronger than a wild one's), knowing its
	 * level's moves, and the HMs you'd have had taught to whoever suits them (a WATER type SURFs, a bird FLYs; your
	 * starter only if no one else can). Each is marked as the picker's.
	 */
	public static List<Mon> make(List<Entry> entries, int stage, long seed) {
		Random rng = new Random(seed * 17 + 5 + stage);
		List<Mon> out = new ArrayList<>();
		for (Entry e : entries) {
			Species sp = Species.ALL.get(e.species());
			if (sp == null) continue;
			int level = Math.max(2, Math.min(100, e.level()));
			Mon m = new Mon(sp, level, rng);
			bestMoves(m);
			// Raised, not wild: some stat experience for its level, a little different for each.
			for (int i = 0; i < 5; i++) m.statExp[i] = Math.min(65535, (int) (level * level * (6 + rng.nextInt(5))));
			m.friendship = Math.min(255, 70 + level * 2);
			m.picked = true;
			m.hp = m.maxHp();
			out.add(m);
		}
		for (String hm : hms(stage)) {
			if (out.stream().anyMatch(m -> m.moves.contains(hm))) continue;
			Mon best = null;
			int bestScore = Integer.MIN_VALUE;
			for (int i = 0; i < out.size(); i++) {
				Mon m = out.get(i);
				if (!m.species.canLearnByMachine(hm)) continue;
				int score = i == 0 ? -100 : -m.level;
				if (hm.equals("surf") && (m.species.type1() == Type.WATER || m.species.type2() == Type.WATER)) score += 200;
				if (hm.equals("fly") && (m.species.type1() == Type.FLYING || m.species.type2() == Type.FLYING)) score += 200;
				if (m.moves.stream().filter(Campaign::isHm).count() >= 2) score -= 300;
				if (score > bestScore) {
					bestScore = score;
					best = m;
				}
			}
			if (best == null) continue;
			if (best.moves.size() < 4) {
				best.moves.add(hm);
				best.pp.add(Move.get(hm).pp());
			} else {
				// In place of its weakest move that isn't an HM.
				int worst = -1;
				double low = Double.MAX_VALUE;
				for (int i = 0; i < best.moves.size(); i++) {
					if (isHm(best.moves.get(i))) continue;
					double sc = score(best, Move.get(best.moves.get(i)));
					if (sc < low) {
						low = sc;
						worst = i;
					}
				}
				if (worst >= 0) {
					best.moves.set(worst, hm);
					best.pp.set(worst, Move.get(hm).pp());
				}
			}
		}
		return out;
	}

	// The moves a player keeps: the strongest it's learned by its level (its own type's count for more), a spread of
	// types, and one useful move that isn't an attack (sleep, paralysis, a stat up, healing); not just its last four.
	private static final Set<String> GOOD_STATUS = Set.of("SLEEP", "PARALYZE", "TOXIC", "CONFUSE", "HEAL", "ATTACK_UP_2", "SP_ATK_UP_2", "SPEED_UP_2",
			"DEFENSE_UP_2", "SP_DEF_UP_2", "SYNTHESIS", "LEECH_SEED", "SUBSTITUTE", "REST", "BELLY_DRUM", "MEAN_LOOK", "CURSE");

	private static double score(Mon m, Move mv) {
		boolean stab = mv.type() == m.species.type1() || mv.type() == m.species.type2();
		int power = mv.power();
		switch (mv.effect()) {
			case "STATIC_DAMAGE", "LEVEL_DAMAGE", "SUPER_FANG" -> power = Math.max(power, Math.min(90, 40 + m.level));
			case "OHKO" -> power = 70;
			case "SELFDESTRUCT" -> power = power / 3;
			case "RAMPAGE", "RECOIL_HIT" -> power = power * 9 / 10;
			case "MULTI_HIT", "DOUBLE_HIT" -> power = power * (mv.effect().equals("DOUBLE_HIT") ? 2 : 3);
			case "TRAP_TARGET" -> power = power + 10;
			default -> {}
		}
		if (power <= 0) return GOOD_STATUS.contains(mv.effect()) ? 48 : 8;
		double acc = mv.accuracy() <= 0 ? 100 : mv.accuracy();
		return power * (stab ? 1.5 : 1) * acc / 100.0;
	}

	public static void bestMoves(Mon m) {
		List<String> known = new ArrayList<>();
		for (Species.Learn l : m.species.learnset()) if (l.level() <= m.level && !known.contains(l.move()) && Move.ALL.containsKey(l.move())) known.add(l.move());
		if (known.size() <= 4) return;
		List<String> chosen = new ArrayList<>();
		Map<Type, Integer> typesUsed = new HashMap<>();
		int status = 0;
		while (chosen.size() < 4) {
			String best = null;
			double bestScore = -1;
			for (String id : known) {
				if (chosen.contains(id)) continue;
				Move mv = Move.get(id);
				double sc = score(m, mv);
				boolean attack = mv.power() > 0 || sc > 48;
				if (!attack && status >= 1) sc *= 0.2;
				if (attack) sc *= Math.pow(0.55, typesUsed.getOrDefault(mv.type(), 0));
				if (sc > bestScore) {
					bestScore = sc;
					best = id;
				}
			}
			if (best == null) break;
			Move mv = Move.get(best);
			if (mv.power() > 0 || score(m, mv) > 48) typesUsed.merge(mv.type(), 1, Integer::sum);
			else status++;
			chosen.add(best);
		}
		m.moves.clear();
		m.pp.clear();
		for (String id : chosen) {
			m.moves.add(id);
			m.pp.add(Move.get(id).pp());
		}
	}

	public static boolean isHm(String move) {
		return move.equals("cut") || move.equals("fly") || move.equals("surf") || move.equals("strength") || move.equals("flash");
	}
}
