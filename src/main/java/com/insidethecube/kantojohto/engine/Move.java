package com.insidethecube.kantojohto.engine;

import java.util.LinkedHashMap;
import java.util.Map;

/** A move, with Silver's numbers: power, accuracy (out of 100, 0 for never-miss), PP, priority and its effect. */
public record Move(String id, String name, Type type, int power, int accuracy, int pp, int priority, Effect effect, int chance, int stages, boolean highCrit) {
	/** What a move does besides damage (or instead of it, for status moves). */
	public enum Effect {
		NONE, ATTACK_DOWN, DEFENSE_DOWN, DEFENSE_DOWN_2, SPEED_DOWN, ACCURACY_DOWN, DEFENSE_UP_SELF, BURN, POISON, SLEEP, PARALYZE, FLINCH, LEECH_SEED
	}

	public boolean status() {
		return power == 0;
	}

	public static final Map<String, Move> ALL = new LinkedHashMap<>();

	private static void add(String id, String name, Type type, int power, int acc, int pp, int priority, Effect effect, int chance, int stages, boolean highCrit) {
		ALL.put(id, new Move(id, name, type, power, acc, pp, priority, effect, chance, stages, highCrit));
	}

	private static void hit(String id, String name, Type type, int power, int acc, int pp) {
		add(id, name, type, power, acc, pp, 0, Effect.NONE, 0, 0, false);
	}

	static {
		hit("tackle", "Tackle", Type.NORMAL, 35, 95, 35);
		hit("scratch", "Scratch", Type.NORMAL, 40, 100, 35);
		add("growl", "Growl", Type.NORMAL, 0, 100, 40, 0, Effect.ATTACK_DOWN, 100, 1, false);
		add("tail_whip", "Tail Whip", Type.NORMAL, 0, 100, 30, 0, Effect.DEFENSE_DOWN, 100, 1, false);
		add("leech_seed", "Leech Seed", Type.GRASS, 0, 90, 10, 0, Effect.LEECH_SEED, 100, 0, false);
		hit("vine_whip", "Vine Whip", Type.GRASS, 35, 100, 10);
		add("poison_powder", "PoisonPowder", Type.POISON, 0, 75, 35, 0, Effect.POISON, 100, 0, false);
		add("sleep_powder", "Sleep Powder", Type.GRASS, 0, 75, 15, 0, Effect.SLEEP, 100, 0, false);
		add("razor_leaf", "Razor Leaf", Type.GRASS, 55, 95, 25, 0, Effect.NONE, 0, 0, true);
		add("ember", "Ember", Type.FIRE, 40, 100, 25, 0, Effect.BURN, 10, 0, false);
		add("smokescreen", "SmokeScreen", Type.NORMAL, 0, 100, 20, 0, Effect.ACCURACY_DOWN, 100, 1, false);
		add("bubble", "Bubble", Type.WATER, 20, 100, 30, 0, Effect.SPEED_DOWN, 10, 1, false);
		hit("water_gun", "Water Gun", Type.WATER, 40, 100, 25);
		add("withdraw", "Withdraw", Type.WATER, 0, 0, 40, 0, Effect.DEFENSE_UP_SELF, 100, 1, false);
		add("sand_attack", "Sand-Attack", Type.GROUND, 0, 100, 15, 0, Effect.ACCURACY_DOWN, 100, 1, false);
		hit("gust", "Gust", Type.FLYING, 40, 100, 35);
		add("quick_attack", "Quick Attack", Type.NORMAL, 40, 100, 30, 1, Effect.NONE, 0, 0, false);
		add("hyper_fang", "Hyper Fang", Type.NORMAL, 80, 90, 15, 0, Effect.FLINCH, 10, 0, false);
		add("defense_curl", "Defense Curl", Type.NORMAL, 0, 0, 40, 0, Effect.DEFENSE_UP_SELF, 100, 1, false);
		hit("rock_throw", "Rock Throw", Type.ROCK, 50, 90, 15);
		add("screech", "Screech", Type.NORMAL, 0, 85, 40, 0, Effect.DEFENSE_DOWN_2, 100, 2, false);
	}

	public static Move get(String id) {
		Move m = ALL.get(id);
		if (m == null) throw new IllegalArgumentException("No move " + id);
		return m;
	}
}
