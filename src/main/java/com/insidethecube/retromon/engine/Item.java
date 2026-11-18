package com.insidethecube.retromon.engine;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An item, with Gold and Silver's names, prices and pockets (read from the games' data), and what it does:
 * Poké Balls and their catch bonuses, healing, curing, reviving, X items, evolution stones, Rare Candy,
 * vitamins, PP restorers, Repels and Escape Rope.
 */
public record Item(String id, String name, int price, String pocket) {
	public static final Map<String, Item> ALL = new LinkedHashMap<>();

	public static Item find(String id) {
		return id == null ? null : ALL.get(id);
	}

	public boolean ball() {
		return pocket.equals("BALL") || id.endsWith("_ball");
	}

	public boolean keyItem() {
		return pocket.equals("KEY_ITEM");
	}

	/** How much better than a Poké Ball it catches (255: always). */
	public double catchBonus() {
		return switch (id) {
			case "master_ball" -> 255;
			case "ultra_ball" -> 2;
			case "great_ball", "safari_ball", "park_ball" -> 1.5;
			default -> 1;
		};
	}

	/** HP it restores (9999 for all of it), or 0. */
	public int heal() {
		return switch (id) {
			case "potion", "berry_juice", "ragecandybar" -> 20;
			case "berry" -> 10;
			case "gold_berry" -> 30;
			case "super_potion", "fresh_water", "energypowder" -> 50;
			case "soda_pop" -> 60;
			case "lemonade" -> 80;
			case "moomoo_milk" -> 100;
			case "hyper_potion", "energy_root" -> 200;
			case "max_potion", "full_restore" -> 9999;
			default -> 0;
		};
	}

	/** The status it cures (null for none; NONE for all of them). */
	public Mon.Status cures() {
		return switch (id) {
			case "antidote", "psncureberry" -> Mon.Status.POISON;
			case "burn_heal", "ice_berry" -> Mon.Status.BURN;
			case "ice_heal", "burnt_berry" -> Mon.Status.FREEZE;
			case "awakening", "mint_berry" -> Mon.Status.SLEEP;
			case "parlyz_heal", "przcureberry" -> Mon.Status.PARALYSIS;
			case "full_heal", "full_restore", "heal_powder", "miracleberry" -> Mon.Status.NONE;
			default -> null;
		};
	}

	public boolean curesConfusion() {
		return id.equals("full_heal") || id.equals("full_restore") || id.equals("heal_powder") || id.equals("miracleberry") || id.equals("bitter_berry");
	}

	/** Revives a fainted Pokémon: the share of its HP it comes back with (0 for none). */
	public double revives() {
		return switch (id) {
			case "revive" -> 0.5;
			case "max_revive", "revival_herb" -> 1;
			default -> 0;
		};
	}

	/** The battle stat an X item raises (Battle's ATK, DEF...), or -1. */
	public int statBoost() {
		return switch (id) {
			case "x_attack" -> Battle.ATK;
			case "x_defend" -> Battle.DEF;
			case "x_speed" -> Battle.SPE;
			case "x_special" -> Battle.SPA;
			case "x_accuracy" -> Battle.ACC;
			default -> -1;
		};
	}

	/** Evolution items (the stones, and the held items that evolve by trade, used like a stone here). */
	public boolean evolves() {
		return switch (id) {
			case "fire_stone", "thunderstone", "water_stone", "leaf_stone", "moon_stone", "sun_stone", "kings_rock", "metal_coat", "dragon_scale", "up_grade" -> true;
			default -> false;
		};
	}

	/** Steps a Repel keeps weaker wild Pokémon away for. */
	public int repelSteps() {
		return switch (id) {
			case "repel" -> 100;
			case "super_repel" -> 200;
			case "max_repel" -> 250;
			default -> 0;
		};
	}

	/** The stat-experience index a vitamin raises (Mon.statExp), or -1. */
	public int vitamin() {
		return switch (id) {
			case "hp_up" -> 0;
			case "protein" -> 1;
			case "iron" -> 2;
			case "carbos" -> 3;
			case "calcium" -> 4;
			default -> -1;
		};
	}

	/** Whether it can be used in battle (from the PACK). */
	public boolean battleUse() {
		return ball() || heal() > 0 || cures() != null || revives() > 0 || statBoost() >= 0 || id.equals("dire_hit") || id.equals("guard_spec")
				|| id.equals("bitter_berry");
	}

	/** Whether it's used on one of your Pokémon (out of battle). */
	public boolean onPokemon() {
		return teaches() || heal() > 0 || cures() != null || revives() > 0 || evolves() || id.equals("rare_candy") || vitamin() >= 0 || id.startsWith("ether")
				|| id.startsWith("max_ether") || id.startsWith("elixer") || id.equals("max_elixer") || id.equals("pp_up") || id.equals("bitter_berry");
	}

	/**
	 * Uses a healing, curing or reviving item on a Pokémon (vitamins and PP items too). Returns what the games
	 * say, or null when it would have no effect (and nothing is used up).
	 */
	public String use(Mon m) {
		double rev = revives();
		if (rev > 0) {
			if (!m.fainted()) return null;
			m.hp = Math.max(1, (int) (m.maxHp() * rev));
			m.status = Mon.Status.NONE;
			return m.name() + " is revitalized!";
		}
		if (m.fainted()) return null;
		int heal = heal();
		Mon.Status cure = cures();
		boolean hurt = m.hp < m.maxHp();
		boolean sick = cure != null && m.status != Mon.Status.NONE && (cure == Mon.Status.NONE || cure == m.status);
		if (heal > 0 || cure != null) {
			if (!hurt && !sick) return null;
			if (heal > 0 && !hurt && !(heal == 9999 && sick)) return null;
			String said = null;
			if (sick) {
				Mon.Status was = m.status;
				m.status = Mon.Status.NONE;
				m.sleepTurns = 0;
				said = m.name() + switch (was) {
					case POISON -> " was cured of poison.";
					case BURN -> "'s burn was healed.";
					case FREEZE -> " was defrosted.";
					case SLEEP -> " woke up.";
					case PARALYSIS -> "'s rid of paralysis.";
					default -> " is healthy.";
				};
			}
			if (heal > 0 && hurt) {
				int before = m.hp;
				m.hp = Math.min(m.maxHp(), m.hp + heal);
				said = m.name() + "'s HP was restored by " + (m.hp - before) + " point(s).";
			}
			return said;
		}
		int v = vitamin();
		if (v >= 0) {
			if (m.statExp[v] >= 25600) return null;
			m.statExp[v] = Math.min(25600, m.statExp[v] + 2560);
			return m.name() + "'s " + new String[] {"HP", "ATTACK", "DEFENSE", "SPEED", "SPECIAL"}[v] + " rose.";
		}
		if (id.equals("ether") || id.equals("max_ether") || id.equals("elixer") || id.equals("max_elixer")) {
			boolean all = id.contains("elixer");
			int amount = id.startsWith("max") ? 99 : 10;
			boolean any = false;
			for (int i = 0; i < m.moves.size(); i++) {
				int max = Move.get(m.moves.get(i)).pp();
				if (m.pp.get(i) < max && (all || !any)) {
					m.pp.set(i, Math.min(max, m.pp.get(i) + amount));
					any = true;
				}
			}
			return any ? "PP was restored." : null;
		}
		return null;
	}

	/** Whether this is a TM or HM (it teaches a move: tm_<move>, hm_<move>). */
	public boolean teaches() {
		return pocket.equals("tm");
	}

	/** The move a TM or HM teaches. */
	public String move() {
		return teaches() ? id.substring(3) : null;
	}

	public boolean hm() {
		return id.startsWith("hm_");
	}

	static {
		for (String[] r : Species.table("items.tsv")) {
			Item it = new Item(r[0], r[1], Integer.parseInt(r[2]), r[3]);
			ALL.put(it.id, it);
		}
		// The TMs and HMs of both generations: one per move (named by it), in their own pocket.
		for (String[] r : Species.table("tms.tsv")) {
			Move mv = Move.find(r[2]);
			if (mv == null) continue;
			boolean hm = r[1].equals("hm");
			String num = !r[4].equals("-") ? r[4] : r[3];
			ALL.put(r[0], new Item(r[0], num + " " + mv.name(), hm ? 0 : 3000, "tm"));
		}
	}
}
