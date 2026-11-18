package com.insidethecube.retromon.engine;

/**
 * Something that happens in a battle, in order, for the screen to play back: a line of text, a change of
 * HP, an attack, a hit, a faint, a Pokémon sent out, a Poké Ball's shakes, EXP, level-ups and money.
 * side: 0 is you, 1 the other side.
 */
public record Event(Kind kind, int side, String text, int a, int b) {
	public enum Kind {
		TEXT, HP, ATTACK, HIT, FAINT, SEND_OUT, RECALL, BALL, CAUGHT, ESCAPE, EXP, LEVEL, STATUS, STAT,
		/** Your money changing (a: by how much, b: what you have after). */
		MONEY
	}

	public static Event text(String t) {
		return new Event(Kind.TEXT, -1, t, 0, 0);
	}

	/** a: the new HP; b: the max. */
	public static Event hp(int side, int hp, int max) {
		return new Event(Kind.HP, side, null, hp, max);
	}

	public static Event of(Kind k, int side) {
		return new Event(k, side, null, 0, 0);
	}

	/** a: party index sent out; b: its HP as it comes out. */
	public static Event sendOut(int side, int index, int hp) {
		return new Event(Kind.SEND_OUT, side, null, index, hp);
	}

	/** a: shakes (0 to 3). */
	public static Event ball(int shakes) {
		return new Event(Kind.BALL, 1, null, shakes, 0);
	}
}
