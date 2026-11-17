package com.insidethecube.kantojohto.engine;

/** The seventeen types and Silver's (Gen 2) type chart. Special types use Special Attack and Defense. */
public enum Type {
	NORMAL("Normal"), FIRE("Fire"), WATER("Water"), ELECTRIC("Electric"), GRASS("Grass"), ICE("Ice"), FIGHTING("Fighting"), POISON("Poison"),
	GROUND("Ground"), FLYING("Flying"), PSYCHIC("Psychic"), BUG("Bug"), ROCK("Rock"), GHOST("Ghost"), DRAGON("Dragon"), DARK("Dark"), STEEL("Steel");

	public final String label;

	Type(String label) {
		this.label = label;
	}

	/** Fire, Water, Grass, Electric, Ice, Psychic, Dragon and Dark moves are special in Gen 2; the rest physical. */
	public boolean special() {
		return switch (this) {
			case FIRE, WATER, GRASS, ELECTRIC, ICE, PSYCHIC, DRAGON, DARK -> true;
			default -> false;
		};
	}

	private static final double[][] CHART = new double[17][17];

	private static void set(Type a, double m, Type... ds) {
		for (Type d : ds) CHART[a.ordinal()][d.ordinal()] = m;
	}

	static {
		for (double[] row : CHART) java.util.Arrays.fill(row, 1);
		set(NORMAL, 0.5, ROCK, STEEL);
		set(NORMAL, 0, GHOST);
		set(FIRE, 0.5, FIRE, WATER, ROCK, DRAGON);
		set(FIRE, 2, GRASS, ICE, BUG, STEEL);
		set(WATER, 2, FIRE, GROUND, ROCK);
		set(WATER, 0.5, WATER, GRASS, DRAGON);
		set(ELECTRIC, 2, WATER, FLYING);
		set(ELECTRIC, 0.5, ELECTRIC, GRASS, DRAGON);
		set(ELECTRIC, 0, GROUND);
		set(GRASS, 2, WATER, GROUND, ROCK);
		set(GRASS, 0.5, FIRE, GRASS, POISON, FLYING, BUG, DRAGON, STEEL);
		set(ICE, 2, GRASS, GROUND, FLYING, DRAGON);
		set(ICE, 0.5, FIRE, WATER, ICE, STEEL);
		set(FIGHTING, 2, NORMAL, ICE, ROCK, DARK, STEEL);
		set(FIGHTING, 0.5, POISON, FLYING, PSYCHIC, BUG);
		set(FIGHTING, 0, GHOST);
		set(POISON, 2, GRASS);
		set(POISON, 0.5, POISON, GROUND, ROCK, GHOST);
		set(POISON, 0, STEEL);
		set(GROUND, 2, FIRE, ELECTRIC, POISON, ROCK, STEEL);
		set(GROUND, 0.5, GRASS, BUG);
		set(GROUND, 0, FLYING);
		set(FLYING, 2, GRASS, FIGHTING, BUG);
		set(FLYING, 0.5, ELECTRIC, ROCK, STEEL);
		set(PSYCHIC, 2, FIGHTING, POISON);
		set(PSYCHIC, 0.5, PSYCHIC, STEEL);
		set(PSYCHIC, 0, DARK);
		set(BUG, 2, GRASS, PSYCHIC, DARK);
		set(BUG, 0.5, FIRE, FIGHTING, POISON, FLYING, GHOST, STEEL);
		set(ROCK, 2, FIRE, ICE, FLYING, BUG);
		set(ROCK, 0.5, FIGHTING, GROUND, STEEL);
		set(GHOST, 2, PSYCHIC, GHOST);
		set(GHOST, 0.5, DARK, STEEL);
		set(GHOST, 0, NORMAL);
		set(DRAGON, 2, DRAGON);
		set(DRAGON, 0.5, STEEL);
		set(DARK, 2, PSYCHIC, GHOST);
		set(DARK, 0.5, FIGHTING, DARK, STEEL);
		set(STEEL, 2, ICE, ROCK);
		set(STEEL, 0.5, FIRE, WATER, ELECTRIC, STEEL);
	}

	/** How effective an attack of this type is against a defender of these types (0, 0.25 to 4). */
	public double against(Type t1, Type t2) {
		double m = CHART[ordinal()][t1.ordinal()];
		if (t2 != null && t2 != t1) m *= CHART[ordinal()][t2.ordinal()];
		return m;
	}
}
