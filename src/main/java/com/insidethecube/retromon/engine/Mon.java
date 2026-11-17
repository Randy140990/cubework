package com.insidethecube.retromon.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * One Pokémon: its species, level, EXP, DVs (0 to 15, as in Gold and Silver), stat experience, HP, status
 * and up to four moves with their PP. Stats are worked out from these, as the games do.
 */
public class Mon {
	public enum Status {
		NONE(""), BURN("BRN"), POISON("PSN"), SLEEP("SLP"), PARALYSIS("PAR"), FREEZE("FRZ");

		public final String tag;

		Status(String tag) {
			this.tag = tag;
		}
	}

	public Species species;
	public String nickname;
	public int level, exp, hp;
	/** Attack, Defense, Speed, Special. */
	public int dvAtk, dvDef, dvSpe, dvSpc;
	/** HP, Attack, Defense, Speed, Special (0 to 65535). */
	public int[] statExp = new int[5];
	public Status status = Status.NONE;
	/** Turns of sleep left. */
	public int sleepTurns;
	public final List<String> moves = new ArrayList<>();
	public final List<Integer> pp = new ArrayList<>();
	/** How much it likes you (0 to 255; 70 to start, as in Gold and Silver). */
	public int friendship = 70;
	/** Steps walked with you, towards the next bit of friendship. */
	public int steps;

	public Mon(Species species, int level, Random rng) {
		this.species = species;
		this.level = level;
		this.exp = species.growth().expAt(level);
		this.dvAtk = rng.nextInt(16);
		this.dvDef = rng.nextInt(16);
		this.dvSpe = rng.nextInt(16);
		this.dvSpc = rng.nextInt(16);
		for (String m : species.movesAt(level)) {
			moves.add(m);
			pp.add(Move.get(m).pp());
		}
		this.hp = maxHp();
	}

	/** For loading a saved one: no moves or DVs yet (set them after). */
	public Mon(Species species) {
		this.species = species;
		this.level = 1;
	}

	public String name() {
		return nickname != null && !nickname.isEmpty() ? nickname : species.name();
	}

	/** Gold and Silver's rule: from the Attack and Speed DVs against the species' ratio. 0 male, 1 female, -1 neither. */
	public int gender() {
		int ratio = species.gender();
		if (ratio < 0) return -1;
		if (ratio == 0) return 0;
		if (ratio >= 254) return 1;
		int b = (dvAtk << 4) | dvSpe;
		return b <= ratio ? 1 : 0;
	}

	public String genderMark() {
		return switch (gender()) {
			case 0 -> "\u2642";
			case 1 -> "\u2640";
			default -> "";
		};
	}

	/** Gold and Silver's friendship gain from a level up: more when it doesn't like you much yet. */
	public void levelFriendship() {
		friendship = Math.min(255, friendship + (friendship < 100 ? 5 : friendship < 200 ? 3 : 2));
	}

	/** What it would evolve into now, by the way given ("level", "item" with the item, or "trade"), or null. */
	public Species evolution(String trigger, String item, boolean day) {
		for (Species.Evolution e : species.evolutions()) {
			Species into = Species.ALL.get(e.into());
			if (into == null) continue;
			switch (e.method()) {
				case "level" -> {
					if (trigger.equals("level") && level >= Integer.parseInt(e.param())) return into;
				}
				case "item" -> {
					if (trigger.equals("item") && e.param().equals(item)) return into;
				}
				case "trade" -> {
					// No trading here: a trade evolution happens at level 37, or with its held item used like a stone.
					if (e.param().equals("-") && trigger.equals("level") && level >= 37) return into;
					if (!e.param().equals("-") && trigger.equals("item") && e.param().equals(item)) return into;
				}
				case "happiness" -> {
					if (!trigger.equals("level") || friendship < 220) continue;
					if (e.param().equals("anytime") || (e.param().equals("mornday") && day) || (e.param().equals("nite") && !day)) return into;
				}
				case "stat" -> {
					String[] p = e.param().split(":");
					if (!trigger.equals("level") || level < Integer.parseInt(p[0])) continue;
					int a = attack(), d = defense();
					if ((p[1].equals("gt") && a > d) || (p[1].equals("lt") && a < d) || (p[1].equals("eq") && a == d)) return into;
				}
				default -> {
				}
			}
		}
		return null;
	}

	/** Becomes another species (evolving), keeping its moves, DVs and EXP; HP goes up by what its max went up. */
	public void evolveInto(Species into) {
		int oldMax = maxHp();
		boolean named = nickname != null && !nickname.isEmpty() && !nickname.equals(species.name());
		species = into;
		if (!named) nickname = null;
		hp = Math.max(0, Math.min(maxHp(), hp + maxHp() - oldMax));
	}

	public int dvHp() {
		return ((dvAtk & 1) << 3) | ((dvDef & 1) << 2) | ((dvSpe & 1) << 1) | (dvSpc & 1);
	}

	/** Gold and Silver's shiny rule: Defense, Speed and Special DVs of 10, Attack 2, 3, 6, 7, 10, 11, 14 or 15. */
	public boolean shiny() {
		return dvDef == 10 && dvSpe == 10 && dvSpc == 10 && (dvAtk & 2) == 2;
	}

	private static int expTerm(int se) {
		return (int) Math.floor(Math.ceil(Math.sqrt(se)) / 4);
	}

	private int stat(int base, int dv, int se) {
		return ((base + dv) * 2 + expTerm(se)) * level / 100 + 5;
	}

	public int maxHp() {
		return ((species.hp() + dvHp()) * 2 + expTerm(statExp[0])) * level / 100 + level + 10;
	}

	public int attack() {
		return stat(species.atk(), dvAtk, statExp[1]);
	}

	public int defense() {
		return stat(species.def(), dvDef, statExp[2]);
	}

	public int speed() {
		return stat(species.spe(), dvSpe, statExp[3]);
	}

	public int spAttack() {
		return stat(species.spa(), dvSpc, statExp[4]);
	}

	public int spDefense() {
		return stat(species.spd(), dvSpc, statExp[4]);
	}

	public boolean fainted() {
		return hp <= 0;
	}

	public void heal() {
		hp = maxHp();
		status = Status.NONE;
		sleepTurns = 0;
		for (int i = 0; i < moves.size(); i++) pp.set(i, Move.get(moves.get(i)).pp());
	}

	/** Puts a move in place of the one at this slot (when you choose which to forget). */
	public String replace(int slot, String move) {
		String old = moves.set(slot, move);
		pp.set(slot, Move.get(move).pp());
		return old;
	}

	/** Adds a move: into a free slot, or in place of the oldest. Returns the move it forgot, or null. */
	public String learn(String move) {
		if (moves.contains(move)) return null;
		if (moves.size() < 4) {
			moves.add(move);
			pp.add(Move.get(move).pp());
			return null;
		}
		String old = moves.remove(0);
		pp.remove(0);
		moves.add(move);
		pp.add(Move.get(move).pp());
		return old;
	}

	public int expToNext() {
		return level >= 100 ? 0 : species.growth().expAt(level + 1) - exp;
	}
}
