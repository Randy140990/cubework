package com.insidethecube.orbitarena.arena;

import static com.insidethecube.orbitarena.arena.ArenaLayout.hash;
import static com.insidethecube.orbitarena.arena.ArenaLayout.noise;

import com.insidethecube.orbitarena.block.ArenaBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * The Bridge's map, shared by the builder, the game, the bots and the crowd: two bases floating in space,
 * joined by a bridge 1 to 3 blocks wide, blue to the west and red to the east.
 * <p>
 * Each base is an island with a deck edged in layers (a quartz lip, a band of light set back under it, a
 * lower lip, then a second band), and on it a temple: you come out of its door (where your team waits
 * behind glass before each round), and stairs up both sides take you to the top, where the goal is, a
 * glowing pit under a roof on four columns with a force-field skylight. Behind the temple five monoliths
 * float in a horseshoe, each with a crystal over it; one crumbles each time that team is scored on. The
 * blue base's lights are green, the red base's red. Grandstands run along both sides of the bridge, and red
 * rock spires float all round at different heights.
 * <p>
 * Positions are relative to the map's centre: dx along the bridge (east is +), dy from the bridge's top,
 * dz across. "adx" is how far out from the middle (|dx|): both bases are the same, mirrored.
 */
public final class BridgeLayout {
	private BridgeLayout() {}

	/** Bridges sit at this z (the stadiums are at z = 0), each player's at the same x as their stadium. */
	public static final int Z = 1536;
	/** The bridge's top surface (you stand at Y + 1). */
	public static final int Y = ArenaLayout.FLOOR;

	/** The bridge runs between the bases, |dx| up to BRIDGE_END, three blocks deep. */
	public static final int BRIDGE_END = 20;
	/** Each base's deck: a landing |dz| up to LANDING_HALF from BASE_FRONT, then the main deck to BASE_BACK. */
	public static final int BASE_FRONT = 21, LANDING_HALF = 6, DECK_FROM = 25, BASE_BACK = 52, BASE_HALF = 13;
	/** The temple: its base from TEMPLE_FRONT to TEMPLE_BACK, |dz| up to TEMPLE_HALF, its top at dy = TOP. */
	public static final int TEMPLE_FRONT = 32, TEMPLE_BACK = 47, TEMPLE_HALF = 8, TOP = 7;
	/** The stairs up each side: |dz| from STAIR_IN to STAIR_OUT, one step up a block from STAIR_FROM out. */
	public static final int STAIR_FROM = 33, STAIR_IN = 9, STAIR_OUT = 11;
	/** Where the stairs come out at the top (the landings either side, from here back). */
	public static final int LANDING_FROM = 40;
	/** The goal: a pit 5 by 5 in the top, centred GOAL_X out, five deep, under the roof. */
	public static final int GOAL_X = 41, GOAL_HALF = 2, GOAL_DEPTH = 5;
	/** The room inside the temple's door, where a team waits and comes back. */
	public static final int ROOM_FROM = 33, ROOM_TO = 37, ROOM_HALF = 2, ROOM_TOP = 4, DOOR_HALF = 1, DOOR_TOP = 3;
	/** Where everyone stands in the room (how far out). */
	public static final int SPAWN_X = 35;
	/** The roof over the goal: from PAV_FROM to TEMPLE_BACK, |dz| up to PAV_HALF, at dy = ROOF. */
	public static final int PAV_FROM = 35, PAV_HALF = 6, ROOF = 14;
	/** Below this (dy) you've fallen into space. */
	public static final int VOID = -28;

	/** The playing area: put right exactly between matches (anything not the map's is cleared). */
	public static final int AREA_X = 66, AREA_Z = 25, AREA_BOTTOM = -24, AREA_TOP = 34;

	/** The grandstands along both sides of the bridge: rows two deep and one up, from STAND_IN out, ROWS of them. */
	public static final int STAND_IN = 17, ROWS = 10, STAND_HALF_X = 26;
	/** Spires and rocks are built out to this far. */
	public static final int SIZE = 230;

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

	// ---------------------------------------------------------------- the bridge

	/** The bridge's blocks across (dz), from bridgeLo to bridgeHi, for a width of 1 to 3. */
	public static int bridgeLo(int width) {
		return -(width / 2);
	}

	public static int bridgeHi(int width) {
		return (width - 1) / 2;
	}

	/** The middle of the bridge across (relative z of the line down its middle). */
	public static double bridgeMidZ(int width) {
		return (bridgeLo(width) + bridgeHi(width) + 1) / 2.0;
	}

	// ---------------------------------------------------------------- the monoliths

	/** A monolith: where it stands (adx out, dz across), its foot (dy) and how tall; the crystal floats over it. */
	public record Monolith(int x, int z, int base, int height) {
		public int top() {
			return base + height;
		}

		/** Where its crystal floats (dy of the crystal's middle). */
		public double crystalY() {
			return top() + 6.5;
		}

		/** Which way its strips of light face: along x (true), or across. */
		public boolean facesAlongX() {
			return Math.abs(GOAL_X - x) >= Math.abs(z);
		}
	}

	/** In the order they crumble: the two ends of the horseshoe first, the middle one last. */
	private static final double[] MONO_ANGLES = {-84, 84, -42, 42, 0};
	private static final int[] MONO_BASES = {-2, 1, 2, -1, 0}, MONO_HEIGHTS = {20, 22, 23, 21, 25};
	public static final Monolith[] MONOLITHS = new Monolith[MONO_ANGLES.length];

	static {
		for (int i = 0; i < MONOLITHS.length; i++) {
			double a = Math.toRadians(MONO_ANGLES[i]);
			MONOLITHS[i] = new Monolith((int) Math.round(GOAL_X + 20 * Math.cos(a)), (int) Math.round(20 * Math.sin(a)), MONO_BASES[i], MONO_HEIGHTS[i]);
		}
	}

	/** The goal (where you score) of the base on side s: a block position (relative) in the pit. */
	public static boolean inGoal(double dx, double dy, double dz, int s) {
		return Math.abs(dx - (s * GOAL_X + 0.5)) < GOAL_HALF + 0.75 && Math.abs(dz - 0.5) < GOAL_HALF + 0.75 && dy < TOP + 0.6 && dy > TOP - GOAL_DEPTH - 1;
	}

	// ---------------------------------------------------------------- the deck's outline

	/** How far a deck column is from the deck's edge (0 on the edge), -1 off the deck; and which way its edge runs. */
	private static final int[][] INSET = new int[AREA_X + 2][2 * AREA_Z + 3];
	private static final boolean[][] EDGE_ALONG_X = new boolean[AREA_X + 2][2 * AREA_Z + 3];
	/** The same for the temple's base (so its top can be edged the same way). */
	private static final int[][] TEMPLE_INSET = new int[AREA_X + 2][2 * AREA_Z + 3];
	private static final boolean[][] TEMPLE_ALONG_X = new boolean[AREA_X + 2][2 * AREA_Z + 3];

	/** How wide the deck is this far out (|dz| up to it), or -1 off the deck. */
	public static int deckHalf(int adx) {
		if (adx < BASE_FRONT || adx > BASE_BACK) return -1;
		if (adx < DECK_FROM) return LANDING_HALF;
		int cut = Math.max(0, Math.max(3 - (adx - DECK_FROM), 3 - (BASE_BACK - adx)));
		return BASE_HALF - cut;
	}

	private static boolean onDeck(int adx, int dz) {
		return Math.abs(dz) <= deckHalf(adx);
	}

	/** The temple's solid base: the main block and the landings at the top of the stairs. */
	public static boolean inTemple(int adx, int dz) {
		int adz = Math.abs(dz);
		return adx >= TEMPLE_FRONT && adx <= TEMPLE_BACK && (adz <= TEMPLE_HALF || adx >= LANDING_FROM && adz <= STAIR_OUT);
	}

	private interface Shape {
		boolean in(int adx, int dz);
	}

	private static void distances(int[][] out, boolean[][] alongX, Shape shape, Shape alsoInside) {
		for (int adx = 0; adx <= AREA_X + 1; adx++) {
			for (int dz = -AREA_Z - 1; dz <= AREA_Z + 1; dz++) {
				int ix = adx, iz = dz + AREA_Z + 1;
				if (!shape.in(adx, dz)) {
					out[ix][iz] = -1;
					continue;
				}
				int found = 14;
				boolean along = true;
				search:
				for (int k = 1; k <= 14; k++) {
					for (int ox = -k; ox <= k; ox++) {
						for (int oz = -k; oz <= k; oz++) {
							if (Math.max(Math.abs(ox), Math.abs(oz)) != k) continue;
							int nx = adx + ox, nz = dz + oz;
							if (!shape.in(nx, nz) && !alsoInside.in(nx, nz)) {
								found = k - 1;
								along = Math.abs(oz) >= Math.abs(ox);
								break search;
							}
						}
					}
				}
				out[ix][iz] = found;
				alongX[ix][iz] = along;
			}
		}
	}

	static {
		// The bridge counts as deck where it joins (so the deck isn't edged across the end of the bridge).
		distances(INSET, EDGE_ALONG_X, BridgeLayout::onDeck, (adx, dz) -> adx >= BRIDGE_END - 2 && adx <= BRIDGE_END && Math.abs(dz) <= 1);
		distances(TEMPLE_INSET, TEMPLE_ALONG_X, BridgeLayout::inTemple, (adx, dz) -> false);
	}

	private static int inset(int[][] table, int adx, int dz) {
		if (adx < 0 || adx > AREA_X + 1 || dz < -AREA_Z - 1 || dz > AREA_Z + 1) return -1;
		return table[adx][dz + AREA_Z + 1];
	}

	private static boolean alongX(boolean[][] table, int adx, int dz) {
		return table[Math.min(adx, AREA_X + 1)][Math.max(0, Math.min(2 * AREA_Z + 2, dz + AREA_Z + 1))];
	}

	// ---------------------------------------------------------------- blocks

	private static BlockState b(Block block) {
		return block.defaultBlockState();
	}

	/** A team side's light (green for blue's base, red for red's), standing up or lying along x or z. */
	public static BlockState light(boolean blue, Direction.Axis axis) {
		return (blue ? ArenaBlocks.LIGHT_GREEN : ArenaBlocks.LIGHT_RED).defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
	}

	public static BlockState wool(boolean blue) {
		return b(blue ? ArenaBlocks.WOOL_BLUE : ArenaBlocks.WOOL_RED);
	}

	private static BlockState stairs(Block block, boolean blue) {
		// Up away from the bridge: west on blue's side, east on red's.
		return block.defaultBlockState().setValue(StairBlock.FACING, blue ? Direction.WEST : Direction.EAST);
	}

	/**
	 * The map's block at a relative position in the playing area, or null for open air (where players can
	 * build). Obstacles: copper pieces on and round the bridge. Width: how wide the bridge is (1 to 3).
	 */
	public static BlockState mapBlock(int dx, int dy, int dz, boolean obstacles, int width) {
		int adx = Math.abs(dx), s = dx < 0 ? -1 : 1;
		if (adx <= BRIDGE_END) {
			if (dy <= 0 && dy >= -2 && dz >= bridgeLo(width) && dz <= bridgeHi(width)) return dx == 0 ? b(Blocks.LIGHT_GRAY_CONCRETE) : wool(s < 0);
			return obstacles ? obstacle(adx, dy, dz, width) : null;
		}
		boolean blue = s < 0;
		for (Monolith m : MONOLITHS) {
			int lx = adx - m.x, lz = dz - m.z;
			if (Math.abs(lx) <= 5 && Math.abs(lz) <= 5) {
				BlockState mb = monolithBlock(m, lx, dy - m.base, lz, blue, dx, dz);
				if (mb != null) return mb;
			}
		}
		if (adx < BASE_FRONT || adx > BASE_BACK) return null;
		return dy >= 1 ? temple(adx, dy, dz, blue) : deck(adx, dy, dz, blue);
	}

	/** The deck and the island under it. */
	private static BlockState deck(int adx, int dy, int dz, boolean blue) {
		int e = inset(INSET, adx, dz);
		if (e < 0) return null;
		Direction.Axis axis = alongX(EDGE_ALONG_X, adx, dz) ? Direction.Axis.X : Direction.Axis.Z;
		switch (dy) {
			case 0 -> {
				if (e == 0) return b(Blocks.SMOOTH_QUARTZ);
				if (adx < DECK_FROM) return wool(blue);
				if (e == 1) return b(Blocks.QUARTZ_BLOCK);
				// A path from the bridge to the temple's door, then a floor of pale stone with darker lines.
				if (Math.abs(dz) <= 1 && adx < TEMPLE_FRONT) return b(Blocks.POLISHED_DIORITE);
				if (Math.floorMod(adx, 6) == 1 || Math.floorMod(dz, 6) == 0) return b(Blocks.POLISHED_ANDESITE);
				return b(Blocks.SMOOTH_STONE);
			}
			case -1 -> {
				// A band of light set back under the quartz lip.
				if (e == 0) return null;
				if (e == 1) return light(blue, axis);
				return b(Blocks.SMOOTH_STONE);
			}
			case -2 -> {
				return e == 0 ? b(Blocks.POLISHED_ANDESITE) : b(Blocks.STONE);
			}
			case -3 -> {
				if (e == 0) return null;
				return e == 1 ? b(Blocks.POLISHED_ANDESITE) : b(Blocks.STONE);
			}
			case -4 -> {
				// A second band, a step further in.
				if (e <= 1) return null;
				return e == 2 ? light(blue, axis) : b(Blocks.STONE);
			}
			case -5 -> {
				if (e <= 1) return null;
				return e == 2 ? b(Blocks.POLISHED_ANDESITE) : b(Blocks.STONE);
			}
			default -> {
				// The island under it: a rough bowl of rock, deepest in the middle, red streaks near the bottom.
				if (dy > 0 || e <= 2) return null;
				int x = blue ? -adx : adx;
				double depth = 5 + (e - 2) * 1.45 + noise(x, dz, 6, 501) * 4 - 2;
				if (-dy > Math.min(21, depth)) return null;
				double h = hash(x, dy, dz, 502);
				boolean low = -dy > depth - 3;
				if (low && h < 0.25) return h < 0.12 ? b(Blocks.GRANITE) : b(Blocks.RED_TERRACOTTA);
				if (h < 0.18) return b(Blocks.ANDESITE);
				if (h < 0.27) return b(Blocks.TUFF);
				if (-dy > 12 && h < 0.4) return b(Blocks.DEEPSLATE);
				return b(Blocks.STONE);
			}
		}
	}

	/** The temple on the deck: the stairs, its base with the room and the goal, the roof over the goal. */
	private static BlockState temple(int adx, int dy, int dz, boolean blue) {
		int adz = Math.abs(dz);
		// The stairs up both sides, filled in underneath.
		if (adx >= STAIR_FROM && adx < LANDING_FROM && adz >= STAIR_IN && adz <= STAIR_OUT) {
			int k = adx - STAIR_FROM;
			if (dy <= k) return adz == STAIR_OUT ? b(Blocks.POLISHED_ANDESITE) : b(Blocks.SMOOTH_STONE);
			if (dy == k + 1) return stairs(adz == STAIR_OUT ? Blocks.POLISHED_ANDESITE_STAIRS : Blocks.SMOOTH_QUARTZ_STAIRS, blue);
			return null;
		}
		if (dy <= TOP && inTemple(adx, dz)) {
			int ep = inset(TEMPLE_INSET, adx, dz);
			Direction.Axis axis = alongX(TEMPLE_ALONG_X, adx, dz) ? Direction.Axis.X : Direction.Axis.Z;
			// The room inside the door, lit along its ceiling, and the door.
			if (adx >= ROOM_FROM && adx <= ROOM_TO && adz <= ROOM_HALF && dy <= ROOM_TOP) return null;
			if (adx >= ROOM_FROM && adx <= ROOM_TO && dz == 0 && dy == ROOM_TOP + 1) return light(blue, Direction.Axis.X);
			if (adx == TEMPLE_FRONT && adz <= DOOR_HALF && dy <= DOOR_TOP) return null;
			if (adx == TEMPLE_FRONT && adz == DOOR_HALF + 1 && dy <= DOOR_TOP + 1) return b(Blocks.SMOOTH_QUARTZ);
			if (adx == TEMPLE_FRONT && adz <= DOOR_HALF && dy == DOOR_TOP + 1) return b(Blocks.SMOOTH_QUARTZ);
			// The goal: a glowing pit with a ring of light round its top, the team's colour round it.
			int ring = Math.max(Math.abs(adx - GOAL_X), adz);
			if (ring <= GOAL_HALF) {
				if (dy > TOP - GOAL_DEPTH) return null;
				if (dy == TOP - GOAL_DEPTH) return b(ArenaBlocks.LIGHT_WHITE);
			}
			if (ring == GOAL_HALF + 1 && dy >= TOP - GOAL_DEPTH && dy < TOP) {
				return dy == TOP - 1 ? light(blue, Math.abs(adx - GOAL_X) == GOAL_HALF + 1 ? Direction.Axis.Z : Direction.Axis.X) : b(Blocks.WHITE_CONCRETE);
			}
			if (dy == TOP) {
				if (ring == GOAL_HALF + 1) return wool(blue);
				if (ring == GOAL_HALF + 2) return b(Blocks.SMOOTH_QUARTZ);
				if (ep == 0) return b(Blocks.SMOOTH_QUARTZ);
				if (Math.floorMod(adx, 4) == 0 || Math.floorMod(dz, 4) == 0) return b(Blocks.POLISHED_ANDESITE);
				return b(Blocks.SMOOTH_STONE);
			}
			// Its sides: a quartz lip at the top, a band of light set back under it, a lower lip, then walls
			// with strips of light either side of the door.
			if (ep == 0) {
				if (dy == TOP - 1) return null;
				if (dy == TOP - 2) return b(Blocks.POLISHED_ANDESITE);
				if (adx == TEMPLE_FRONT && adz == 5) return light(blue, Direction.Axis.Y);
				return dy == 1 ? b(Blocks.POLISHED_ANDESITE) : b(Blocks.SMOOTH_STONE);
			}
			if (ep == 1 && dy == TOP - 1) return light(blue, axis);
			return b(Blocks.STONE);
		}
		// The roof over the goal, on four columns, with the force field over the pit.
		if (dy > TOP && dy <= ROOF + 1 && adx >= PAV_FROM && adx <= TEMPLE_BACK && adz <= PAV_HALF) {
			boolean rim = adx == PAV_FROM || adx == TEMPLE_BACK || adz == PAV_HALF;
			if (dy < ROOF) {
				boolean column = (adx <= PAV_FROM + 1 || adx >= TEMPLE_BACK - 1) && adz >= PAV_HALF - 1;
				if (!column) return null;
				boolean inner = (adx == PAV_FROM + 1 || adx == TEMPLE_BACK - 1) && adz == PAV_HALF - 1;
				if (inner && dy > TOP + 1 && dy < ROOF - 1) return light(blue, Direction.Axis.Y);
				return b(Blocks.QUARTZ_PILLAR);
			}
			if (dy == ROOF) {
				if (Math.abs(adx - GOAL_X) <= 3 && adz <= 3) return b(blue ? ArenaBlocks.FIELD_BLUE : ArenaBlocks.FIELD_RED);
				if (adz == 4 && Math.abs(adx - GOAL_X) <= 4) return light(blue, Direction.Axis.X);
				return rim ? b(Blocks.SMOOTH_QUARTZ) : b(Blocks.SMOOTH_STONE);
			}
			return rim ? Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM) : null;
		}
		return null;
	}

	/**
	 * A monolith (lx, ly, lz from its foot): a rock floating under it, a plinth, a chamfered shaft with strips
	 * of light up two faces, a capital and a glowing socket on top, under its crystal.
	 */
	private static BlockState monolithBlock(Monolith m, int lx, int ly, int lz, boolean blue, int dx, int dz) {
		int ax = Math.abs(lx), az = Math.abs(lz), h = m.height();
		if (ly < -11 || ly > h) return null;
		if (ly < 0) {
			double t = (-ly - 1) / 10.0, r = 4.3 * Math.pow(Math.max(0, 1 - t), 0.8) + noise(dx * 1.7 + ly, dz * 1.3, 2.2, 611) * 1.2 - 0.4;
			return lx * lx + lz * lz <= r * r ? rock(dx, ly + m.base, dz, 0.5 + r - Math.sqrt(lx * lx + lz * lz)) : null;
		}
		boolean wide = ax <= 3 && az <= 3 && ax + az <= 5;
		if (ly <= 1) return wide ? (ly == 0 ? b(Blocks.POLISHED_ANDESITE) : b(Blocks.SMOOTH_QUARTZ)) : null;
		if (ly >= h - 2 && ly < h) return wide ? (ly == h - 2 ? b(Blocks.POLISHED_ANDESITE) : b(Blocks.SMOOTH_QUARTZ)) : null;
		if (ly == h) {
			if (lx == 0 && lz == 0) return light(blue, Direction.Axis.Y);
			return ax <= 1 && az <= 1 ? b(Blocks.SMOOTH_QUARTZ) : null;
		}
		if (ax > 2 || az > 2 || ax + az > 3) return null;
		boolean alongX = m.facesAlongX();
		int across = alongX ? az : ax, out = alongX ? ax : az;
		if (out == 2 && across == 0 && ly >= 4 && ly <= h - 5) return light(blue, Direction.Axis.Y);
		if (out == 2 && across == 1) return b(Blocks.SMOOTH_QUARTZ);
		if (ly == 2 || ly == h - 4) return b(Blocks.POLISHED_ANDESITE);
		if (ax + az == 3) return b(Blocks.SMOOTH_QUARTZ);
		return b(Blocks.SMOOTH_STONE);
	}

	/** Which monolith (0 to 4) a relative block position belongs to, or -1. */
	public static int monolithAt(int dx, int dy, int dz) {
		int adx = Math.abs(dx);
		for (int i = 0; i < MONOLITHS.length; i++) {
			Monolith m = MONOLITHS[i];
			if (Math.abs(adx - m.x) <= 3 && Math.abs(dz - m.z) <= 3 && dy >= m.base && dy <= m.top()) return i;
		}
		return -1;
	}

	/**
	 * Copper obstacles: a block in the middle of the bridge to get round (7 out), an arch over it (13 out)
	 * and a low grate across it to jump (17 out).
	 */
	private static BlockState obstacle(int adx, int dy, int dz, int width) {
		int lo = bridgeLo(width), hi = bridgeHi(width);
		if (adx == 7 && dz == 0 && (dy == 1 || dy == 2)) return dy == 2 ? b(Blocks.WAXED_OXIDIZED_CUT_COPPER) : b(Blocks.WAXED_WEATHERED_COPPER);
		if (adx == 13 && (dz == lo - 2 || dz == hi + 2) && dy >= -2 && dy <= 4) return b(Blocks.WAXED_OXIDIZED_COPPER);
		if (adx == 13 && dz >= lo - 2 && dz <= hi + 2 && dy == 5) return b(Blocks.WAXED_OXIDIZED_CUT_COPPER);
		if (adx == 17 && dz >= lo && dz <= hi && dy == 1) return b(Blocks.WAXED_EXPOSED_COPPER_GRATE);
		return null;
	}

	/**
	 * Whether players may build here (relative): over the void round the bridge and the bases, and on the
	 * decks, but not on or round the temples (the stairs, the goals, the doors) or the monoliths.
	 */
	public static boolean buildable(int dx, int dy, int dz) {
		int adx = Math.abs(dx), adz = Math.abs(dz);
		if (adx > AREA_X - 3 || adz > AREA_Z - 2 || dy < -14 || dy > 20) return false;
		if (adx <= STAND_HALF_X + 1 && adz > STAND_IN - 3) return false;
		if (adx >= TEMPLE_FRONT - 3 && adx <= TEMPLE_BACK + 1 && adz <= STAIR_OUT + 1 && dy >= 0) return false;
		for (Monolith m : MONOLITHS) {
			int lx = adx - m.x, lz = dz - m.z;
			if (lx * lx + lz * lz <= 25) return false;
		}
		return true;
	}

	/** The cage: glass in the team's colour across the temple's door. */
	public static BlockState cageBlock(int dx, int dy, int dz) {
		int adx = Math.abs(dx);
		if (adx != TEMPLE_FRONT || Math.abs(dz) > DOOR_HALF || dy < 1 || dy > DOOR_TOP) return null;
		return dx < 0 ? b(Blocks.BLUE_STAINED_GLASS) : b(Blocks.RED_STAINED_GLASS);
	}

	/** Where the i-th of a team stands in its room (relative z); the first in the middle. */
	public static int roomZ(int i) {
		return switch (i % 5) {
			case 1 -> -1;
			case 2 -> 1;
			case 3 -> -2;
			case 4 -> 2;
			default -> 0;
		};
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

	/** One of the rock spires round the map: where its foot is (relative), how wide and tall. */
	public record Spire(int x, int y, int z, double radius, int height, long seed) {}

	public static final int SPIRES = 34;

	public static Spire spire(int i) {
		// Round the map at all heights, clear of the playing area and the stands.
		double a = (i + hash(i, 0, 0, 70) * 0.7) / SPIRES * Math.PI * 2, dist = 82 + hash(i, 1, 0, 71) * 95;
		int x = (int) (Math.cos(a) * dist * 1.2), z = (int) (Math.sin(a) * dist);
		if (Math.abs(z) < 45 && Math.abs(x) < 80) z += z < 0 ? -45 : 45;
		double size = hash(i, 5, 0, 75);
		int height = 14 + (int) (size * size * 58);
		int y = -55 + (int) (hash(i, 3, 0, 73) * 70) - height / 3;
		double radius = 3 + height * 0.11 + hash(i, 4, 0, 74) * 3;
		return new Spire(x, y, z, radius, height, i * 7919L + 13);
	}

	/** How wide a spire is t of the way up (0 at the foot, 1 at the tip): a spindle, lumpy. */
	public static double spireRadius(Spire s, double t, double angle) {
		double shape = Math.pow(Math.max(0, 1 - Math.abs(2 * t - 1)), 0.7);
		double lump = 0.8 + 0.4 * noise(Math.cos(angle) * 6 + s.seed() % 97, t * 9, 2.5, s.seed());
		return s.radius() * shape * lump;
	}

	/** The first bridges' spires (before the temples), so a rebuild can clear them away. */
	public static Spire legacySpire(int i) {
		double a = hash(i, 0, 0, 60) * Math.PI * 2, dist = 70 + hash(i, 1, 0, 61) * 90;
		int x = (int) (Math.cos(a) * dist * 1.25), z = (int) (Math.sin(a) * dist);
		if (Math.abs(z) < 40 && Math.abs(x) < 70) z += z < 0 ? -40 : 40;
		int height = 18 + (int) (hash(i, 2, 0, 62) * 44);
		int y = -30 + (int) (hash(i, 3, 0, 63) * 34);
		double radius = 4 + hash(i, 4, 0, 64) * 7;
		return new Spire(x, y, z, radius, height, i * 7919L);
	}

	public static final int LEGACY_SPIRES = 28;

	/**
	 * Red layered rock for the spires and the monoliths' rocks: bands of netherrack, red and plain
	 * terracotta and granite on the outside (depth: how far in from the surface), netherrack inside.
	 */
	public static BlockState rock(int x, int y, int z, double depth) {
		if (depth > 1.6) return b(Blocks.NETHERRACK);
		double h = hash(x, y, z, 3);
		if (h < 0.05) return b(Blocks.POLISHED_GRANITE);
		if (h < 0.09) return b(Blocks.NETHER_WART_BLOCK);
		int band = Math.floorMod(y + (int) (noise(x, z, 11, 77) * 4), 7);
		return switch (band) {
			case 0 -> b(Blocks.RED_TERRACOTTA);
			case 1, 4 -> b(Blocks.NETHERRACK);
			case 2 -> b(Blocks.TERRACOTTA);
			case 3 -> b(Blocks.GRANITE);
			case 5 -> b(Blocks.RED_TERRACOTTA);
			default -> b(Blocks.NETHERRACK);
		};
	}

	// ---------------------------------------------------------------- the way round (for the bots)

	/** Where the bots head (relative x, y to stand at, z), on the base on side s. */
	public static double[] front(int s, int width) {
		return new double[] {s * (BASE_FRONT + 2) + 0.5, 1, bridgeMidZ(width)};
	}

	/** The foot and the top of the stairs on one side (k = -1 or 1) of the temple on side s. */
	public static double[] stairFoot(int s, int k) {
		return new double[] {s * (STAIR_FROM - 1) + 0.5, 1, k * (STAIR_IN + 1) + 0.5};
	}

	public static double[] stairTop(int s, int k) {
		return new double[] {s * (LANDING_FROM + 1) + 0.5, TOP + 1, k * (STAIR_IN + 1) + 0.5};
	}

	/** In the middle of the goal (drop in). */
	public static double[] goal(int s) {
		return new double[] {s * GOAL_X + 0.5, TOP + 1, 0.5};
	}

	/** Beside a team's own goal, on the top, on the side its stairs come up (k = -1 or 1): where defenders stand. */
	public static double[] guard(int s, int k) {
		return new double[] {s * GOAL_X + 0.5, TOP + 1, k * (GOAL_HALF + 2) + 0.5};
	}
}
