package com.insidethecube.orbitarena.arena;

import static com.insidethecube.orbitarena.arena.ArenaLayout.hash;
import static com.insidethecube.orbitarena.arena.ArenaLayout.noise;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Bridge's map, shared by the builder, the game and the crowd: two bases floating in space, joined by a
 * bridge five blocks wide, blue to the west and red to the east. Each base has its goal (a glowing pit: drop
 * into the other team's to score), a glass cage above where its team waits between goals, and five pillars
 * along its back, each with a crystal floating over it; one crumbles each time that team is scored on.
 * Grandstands run along both sides of the bridge, and rock spires float all round.
 * <p>
 * Positions are relative to the map's centre: dx along the bridge (east is +), dy from the bridge's top,
 * dz across. Everything in the playing area is either the map's own (worked out here, so it can be put
 * right exactly) or open air the players can build in.
 */
public final class BridgeLayout {
	private BridgeLayout() {}

	/** Bridges sit at this z (the stadiums are at z = 0), each player's at the same x as their stadium. */
	public static final int Z = 1536;
	/** The bridge's top surface (you stand at Y + 1). */
	public static final int Y = ArenaLayout.FLOOR;

	/** The bridge runs between the bases: |dx| up to BRIDGE_END, |dz| up to BRIDGE_HALF, three blocks deep. */
	public static final int BRIDGE_END = 20, BRIDGE_HALF = 2;
	/** Each base: a landing (|dz| up to 6) from BASE_FRONT, the main deck (|dz| up to BASE_HALF) from DECK to BASE_BACK. */
	public static final int BASE_FRONT = 21, DECK = 26, BASE_BACK = 43, BASE_HALF = 10;
	/** The goal: a pit 5 by 5 centred GOAL_X out, five deep, with a coloured rim. */
	public static final int GOAL_X = 30, GOAL_HALF = 2, GOAL_DEPTH = 5;
	/** The cage: centred CAGE_X out, its floor CAGE_FLOOR up, inside 3 along and 7 across, 3 high. */
	public static final int CAGE_X = 37, CAGE_FLOOR = 7, CAGE_HALF_X = 1, CAGE_HALF_Z = 3;
	/** Where you come back after falling or being knocked out: on the deck under the cage. */
	public static final int SPAWN_X = 37;
	/** The pillars: 3 by 3, along the back (PILLAR_X to PILLAR_X + 2 out), at these dz, PILLAR_TOP high. */
	public static final int PILLAR_X = 40, PILLAR_TOP = 13;
	public static final int[] PILLAR_Z = {-8, -4, 0, 4, 8};
	/** Each pillar's crystal floats this high. */
	public static final double CRYSTAL_Y = 16.5;
	/** Below this (dy) you've fallen into space. */
	public static final int VOID = -22;

	/** The playing area: put right exactly between matches (anything not the map's is cleared). */
	public static final int AREA_X = 46, AREA_Z = 14, AREA_BOTTOM = -14, AREA_TOP = 22;

	/** The grandstands along both sides: rows two deep and one up, from STAND_IN out, ROWS of them. */
	public static final int STAND_IN = 17, ROWS = 7, STAND_HALF_X = 20;
	/** Spires and rocks are built out to this far. */
	public static final int SIZE = 170;

	/** Team colours (blue: the player's team, west; red: east). */
	public static final int BLUE = 0, RED = 1;

	/** Which side a team's base is on: -1 west (blue), +1 east (red). */
	public static int side(int team) {
		return team == BLUE ? -1 : 1;
	}

	/** Whether a world position is in or near a bridge map (any player's). */
	public static boolean near(BlockPos pos) {
		return Math.abs(pos.getZ() - Z) < SIZE;
	}

	/** Whether a block position (relative) is in the goal pit of the base on side s (where you score). */
	public static boolean inGoal(double dx, double dy, double dz, int s) {
		return Math.abs(dx - (s * GOAL_X + 0.5)) < GOAL_HALF + 0.75 && Math.abs(dz - 0.5) < GOAL_HALF + 0.75 && dy < 0.6 && dy > -GOAL_DEPTH - 1;
	}

	private static BlockState b(Block block) {
		return block.defaultBlockState();
	}

	/**
	 * The map's block at a relative position in the playing area, or null for open air (where players can
	 * build). Obstacles: copper pieces on and round the bridge.
	 */
	public static BlockState mapBlock(int dx, int dy, int dz, boolean obstacles) {
		int adx = Math.abs(dx), adz = Math.abs(dz), s = dx < 0 ? -1 : 1;
		// The bridge.
		if (adx <= BRIDGE_END && adz <= BRIDGE_HALF && dy <= 0 && dy >= -2) {
			if (dx == 0) return b(Blocks.LIGHT_GRAY_CONCRETE);
			if (dy == -2 && adz == BRIDGE_HALF) return b(Blocks.SEA_LANTERN);
			return dx < 0 ? b(Blocks.BLUE_TERRACOTTA) : b(Blocks.RED_TERRACOTTA);
		}
		if (obstacles) {
			BlockState o = obstacle(adx, dy, dz);
			if (o != null) return o;
		}
		if (adx < BASE_FRONT || adx > BASE_BACK) return null;
		boolean blue = s < 0;
		// The goal pit and its rims.
		int gx = adx - GOAL_X, ring = Math.max(Math.abs(gx), adz);
		if (ring <= GOAL_HALF && dy <= 0 && dy > -GOAL_DEPTH) return null;
		if (ring <= GOAL_HALF && dy == -GOAL_DEPTH) return b(Blocks.SEA_LANTERN);
		if (ring == GOAL_HALF + 1 && dy < 0 && dy >= -GOAL_DEPTH) return dy == -1 ? b(Blocks.SEA_LANTERN) : b(Blocks.WHITE_CONCRETE);
		// The pillars along the back.
		for (int pz : PILLAR_Z) {
			if (adx >= PILLAR_X && adx <= PILLAR_X + 2 && Math.abs(dz - pz) <= 1 && dy >= 1 && dy <= PILLAR_TOP) return pillar(adx, dy, dz - pz, blue);
		}
		// The deck: the narrow landing, then the main deck with its corners cut.
		boolean landing = adx < DECK && adz <= 6;
		boolean main = adx >= DECK && adz <= BASE_HALF && !(adz >= BASE_HALF - 1 && adx >= BASE_BACK - 1);
		if (!landing && !main) {
			return null;
		}
		boolean edge = landing ? adz == 6 || adx == BASE_FRONT && adz > BRIDGE_HALF : adz == BASE_HALF || adx == BASE_BACK || adx == DECK && adz > 6
				|| adz == BASE_HALF - 1 && adx == BASE_BACK - 1 || adz == BASE_HALF - 2 && adx >= BASE_BACK - 1;
		if (dy == 0) {
			if (ring == GOAL_HALF + 1) return blue ? b(Blocks.BLUE_CONCRETE) : b(Blocks.RED_CONCRETE);
			if (ring == GOAL_HALF + 2) return b(Blocks.QUARTZ_BLOCK);
			if (edge) return b(Blocks.SMOOTH_QUARTZ);
			// A path of lighter stone from the bridge to the goal, then pale deck.
			if (adz <= BRIDGE_HALF && adx < GOAL_X - 3) return b(Blocks.POLISHED_DIORITE);
			return hash(dx, 0, dz, 51) < 0.08 ? b(Blocks.POLISHED_ANDESITE) : b(Blocks.LIGHT_GRAY_CONCRETE);
		}
		if (dy == -1) return edge ? b(Blocks.SEA_LANTERN) : b(Blocks.SMOOTH_STONE);
		if (dy == -2) return edge ? blue ? b(Blocks.BLUE_CONCRETE) : b(Blocks.RED_CONCRETE) : b(Blocks.SMOOTH_STONE);
		// The base's underside, stepping in as it goes down.
		if (dy < -2 && dy >= -9) {
			int in = (-2 - dy);
			boolean under = (landing ? adz <= 6 - in : adz <= BASE_HALF - in) && adx >= BASE_FRONT + in && adx <= BASE_BACK - in;
			if (under) {
				boolean rim = !((landing ? adz <= 5 - in : adz <= BASE_HALF - 1 - in) && adx >= BASE_FRONT + in + 1 && adx <= BASE_BACK - in - 1);
				if (rim) return dy == -5 ? b(Blocks.SEA_LANTERN) : b(Blocks.POLISHED_ANDESITE);
				return b(Blocks.STONE);
			}
		}
		return null;
	}

	/** A pillar: pale stone with a strip of light up its face, capped in its team's colour. */
	private static BlockState pillar(int adx, int dy, int rz, boolean blue) {
		if (dy == PILLAR_TOP) return blue ? b(Blocks.BLUE_CONCRETE) : b(Blocks.RED_CONCRETE);
		if (dy == PILLAR_TOP - 1) return b(Blocks.POLISHED_ANDESITE);
		if (adx == PILLAR_X && rz == 0 && dy >= 3 && dy <= PILLAR_TOP - 3) return b(Blocks.SEA_LANTERN);
		if (dy == 1 || dy == 2) return b(Blocks.POLISHED_ANDESITE);
		return Math.abs(rz) == 1 && adx == PILLAR_X ? b(Blocks.SMOOTH_QUARTZ) : b(Blocks.SMOOTH_STONE);
	}

	/**
	 * Copper obstacles: a block in the middle of the bridge to go round (7 out), low grates either side of
	 * the middle (17 out), and an arch over it (13 out).
	 */
	private static BlockState obstacle(int adx, int dy, int dz) {
		int adz = Math.abs(dz);
		if (adx == 7 && dz == 0 && (dy == 1 || dy == 2)) return dy == 2 ? b(Blocks.WAXED_OXIDIZED_CUT_COPPER) : b(Blocks.WAXED_WEATHERED_COPPER);
		if (adx == 17 && adz == 1 && dy == 1) return b(Blocks.WAXED_EXPOSED_COPPER_GRATE);
		if (adx == 13 && adz == 3 && dy >= -2 && dy <= 4) return b(Blocks.WAXED_OXIDIZED_COPPER);
		if (adx == 13 && adz <= 3 && dy == 5) return b(Blocks.WAXED_OXIDIZED_CUT_COPPER);
		return null;
	}

	/** Whether players may build here (relative): near the bridge, but not in or over a goal, a cage or the pillars. */
	public static boolean buildable(int dx, int dy, int dz) {
		int adx = Math.abs(dx), adz = Math.abs(dz);
		if (adx > AREA_X - 2 || adz > AREA_Z - 1 || dy < -10 || dy > 12) return false;
		if (Math.abs(adx - GOAL_X) <= GOAL_HALF + 1 && adz <= GOAL_HALF + 1 && dy >= -GOAL_DEPTH && dy <= 3) return false;
		if (adx >= CAGE_X - 3 && adx <= CAGE_X + 3 && adz <= CAGE_HALF_Z + 2 && dy >= CAGE_FLOOR - 2) return false;
		if (adx >= SPAWN_X - 2 && adx <= SPAWN_X + 2 && adz <= 2 && dy >= 1 && dy <= 3) return false;
		return adx < PILLAR_X - 1 || dy < 1;
	}

	/** The cage's block at a relative position (glass in the team's colour), or null. */
	public static BlockState cageBlock(int dx, int dy, int dz) {
		int adx = Math.abs(dx), s = dx < 0 ? -1 : 1;
		int rx = adx - CAGE_X, rz = Math.abs(dz);
		if (Math.abs(rx) > CAGE_HALF_X + 1 || rz > CAGE_HALF_Z + 1 || dy < CAGE_FLOOR || dy > CAGE_FLOOR + 4) return null;
		boolean wall = Math.abs(rx) == CAGE_HALF_X + 1 || rz == CAGE_HALF_Z + 1 || dy == CAGE_FLOOR || dy == CAGE_FLOOR + 4;
		if (!wall) return null;
		return s < 0 ? b(Blocks.BLUE_STAINED_GLASS) : b(Blocks.RED_STAINED_GLASS);
	}

	// ---------------------------------------------------------------- the grandstands

	/** The top of a row of the stands (dy), and its middle (|dz|). */
	public static int rowTop(int row) {
		return -1 + row;
	}

	public static double rowZ(int row) {
		return STAND_IN + 2 * row + 1;
	}

	/** The stands' block at a relative position, or null (for both sides; dz's sign picks the side). */
	public static BlockState standBlock(int dx, int dy, int dz) {
		int adx = Math.abs(dx), adz = Math.abs(dz);
		if (adz < STAND_IN - 1 || adz > STAND_IN + 2 * ROWS + 2 || adx > STAND_HALF_X + 1) return null;
		int back = STAND_IN + 2 * ROWS;
		// The ends and the back wall.
		if (adz >= back) {
			if (adx > STAND_HALF_X) return null;
			if (dy == rowTop(ROWS - 1) + 7) return b(Blocks.SEA_LANTERN);
			if (dy == rowTop(ROWS - 1) + 5) return b(Blocks.ORANGE_CONCRETE);
			if (dy >= -6 && dy <= rowTop(ROWS - 1) + 7) return b(Blocks.LIGHT_GRAY_CONCRETE);
			return null;
		}
		if (adx == STAND_HALF_X + 1) {
			int top = adz < STAND_IN ? -1 : rowTop((adz - STAND_IN) / 2) + 2;
			if (dy >= -6 && dy <= top) return dy == top ? b(Blocks.SEA_LANTERN) : b(Blocks.LIGHT_GRAY_CONCRETE);
			return null;
		}
		// The front rail: lit at the foot.
		if (adz == STAND_IN - 1) {
			if (dy == -1) return b(Blocks.SEA_LANTERN);
			if (dy == 0) return b(Blocks.POLISHED_DEEPSLATE);
			if (dy >= -6 && dy < -1) return b(Blocks.LIGHT_GRAY_CONCRETE);
			return null;
		}
		int row = (adz - STAND_IN) / 2, top = rowTop(row);
		boolean front = (adz - STAND_IN) % 2 == 0;
		if (dy == top) return Math.floorMod(dx, 8) == 0 ? b(Blocks.SMOOTH_STONE) : front ? b(Blocks.POLISHED_ANDESITE) : b(Blocks.ANDESITE);
		if (dy < top && dy >= top - 3) return b(Blocks.STONE_BRICKS);
		// Under the stands: beams down to the frame.
		if (dy == -6) return b(Blocks.LIGHT_GRAY_CONCRETE);
		if (dy < top - 3 && dy > -6 && Math.floorMod(dx, 6) == 0) return b(Blocks.LIGHT_GRAY_CONCRETE);
		return null;
	}

	// ---------------------------------------------------------------- spires and rocks

	/** One of the rock spires round the map: where it is (relative), how wide and tall. */
	public record Spire(int x, int y, int z, double radius, int height, long seed) {}

	public static Spire spire(int i) {
		// Round the map, clear of the playing area and the stands.
		double a = hash(i, 0, 0, 60) * Math.PI * 2, dist = 70 + hash(i, 1, 0, 61) * 90;
		int x = (int) (Math.cos(a) * dist * 1.25), z = (int) (Math.sin(a) * dist);
		if (Math.abs(z) < 40 && Math.abs(x) < 70) z += z < 0 ? -40 : 40;
		int height = 18 + (int) (hash(i, 2, 0, 62) * 44);
		int y = -30 + (int) (hash(i, 3, 0, 63) * 34);
		double radius = 4 + hash(i, 4, 0, 64) * 7;
		return new Spire(x, y, z, radius, height, i * 7919L);
	}

	public static final int SPIRES = 28;

	/** How wide a spire is t of the way up (0 at the foot, 1 at the tip): a spindle, lumpy. */
	public static double spireRadius(Spire s, double t, double angle) {
		double shape = Math.pow(Math.max(0, 1 - Math.abs(2 * t - 1)), 0.7);
		double lump = 0.8 + 0.4 * noise(Math.cos(angle) * 6 + s.seed() % 97, t * 9, 2.5, s.seed());
		return s.radius() * shape * lump;
	}

	/** Layered rock for the spires: terracotta bands with granite and red streaks. */
	public static BlockState rock(int x, int y, int z) {
		double h = hash(x, y, z, 3);
		if (h < 0.08) return b(Blocks.GRANITE);
		if (h < 0.14) return b(Blocks.RED_TERRACOTTA);
		int band = Math.floorMod(y, 5);
		return band == 0 ? b(Blocks.BROWN_TERRACOTTA) : band == 3 ? b(Blocks.ORANGE_TERRACOTTA) : b(Blocks.TERRACOTTA);
	}
}
