package com.insidethecube.kantojohto.engine;

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
		return nickname != null && !nickname.isEmpty() ? nickname : species.name().toUpperCase();
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
