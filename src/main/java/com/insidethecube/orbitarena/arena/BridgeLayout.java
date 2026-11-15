package com.insidethecube.orbitarena.arena;

import static com.insidethecube.orbitarena.arena.ArenaLayout.hash;
import static com.insidethecube.orbitarena.arena.ArenaLayout.noise;

import com.insidethecube.orbitarena.block.ArenaBlocks;
import com.insidethecube.orbitarena.block.ForceFieldBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Bridge's map, shared by the builder, the game, the bots and the crowd: two bases floating in space,
 * joined by a bridge 1 to 3 blocks wide, blue to the west and red to the east.
 * <p>
 * Each base is an island with a deck edged in layers (a quartz lip, a band of light set back under it, a
 * lower lip, then a second band), and on it a temple in two tiers, each edged the same way. You come out of
 * its door (where your team waits behind glass before each round), and ten steps up each side take you to
 * the top, where the goal is: a glowing pit inside a box of force field that rises from a line of light
 * round it and fades out upwards (walk through it; you can't build into it). Round the back of each base
 * five monoliths float in a horseshoe, built as LumenForge's Portal builder builds its monolith slabs
 * (broad, rounded at the corners and the foot, a band above the foot, a capital with a quartz lip, pillars
 * up each side with the face between them sunk, shaded from foot to top), a crystal over each; one
 * crumbles each time that team is scored on. The blue base's lights are green, the red base's red.
 * Grandstands run along both sides of the bridge on jagged red asteroids, behind a wall of force field;
 * red rock spires float all round.
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
	public static final int BASE_FRONT = 21, LANDING_HALF = 6, DECK_FROM = 25, BASE_BACK = 58, BASE_HALF = 14;
	/**
	 * The temple: a lower tier from TEMPLE_FRONT to TEMPLE_BACK up to dy = TIER, an upper tier UPPER_IN in
	 * from its front and back up to dy = TOP, both |dz| up to TEMPLE_HALF (and out to STAIR_OUT where the
	 * stairs come out at the top).
	 */
	public static final int TEMPLE_FRONT = 32, TEMPLE_BACK = 52, TEMPLE_HALF = 9, TIER = 5, UPPER_IN = 2, TOP = 10;
	/** The stairs up each side: |dz| from STAIR_IN to STAIR_OUT, one step up a block from STAIR_FROM out (ten steps). */
	public static final int STAIR_FROM = 33, STAIR_IN = 10, STAIR_OUT = 12;
	/** Where the stairs come out at the top (the landings either side, from here back). */
	public static final int LANDING_FROM = STAIR_FROM + TOP;
	/** The goal: a pit 5 by 5 in the top, centred GOAL_X out, five deep. */
	public static final int GOAL_X = 45, GOAL_HALF = 2, GOAL_DEPTH = 5;
	/** The force field round the goal: a box FIELD_RING out from the pit's middle, FIELD_HIGH tall over the top. */
	public static final int FIELD_RING = 5, FIELD_HIGH = 6;
	/** The room inside the temple's door, where a team waits and comes back. */
	public static final int ROOM_FROM = 33, ROOM_TO = 37, ROOM_HALF = 2, ROOM_TOP = 4, DOOR_HALF = 1, DOOR_TOP = 3;
	/** Where everyone stands in the room (how far out). */
	public static final int SPAWN_X = 35;
	/** Below this (dy) you've fallen into space. */
	public static final int VOID = -32;

	/** The playing area: put right exactly between matches (anything not the map's is cleared). */
	public static final int AREA_X = 78, AREA_Z = 31, AREA_BOTTOM = -27, AREA_TOP = 42;

	/** The grandstands along both sides of the bridge: rows two deep and one up, from STAND_IN out, ROWS of them. */
	public static final int STAND_IN = 22, ROWS = 10, STAND_HALF_X = 26;
	/** The force field between the playing area and the stands (|dz|), and how high it goes. */
	public static final int WALL_Z = STAND_IN - 2, WALL_END = STAND_HALF_X + 3, WALL_TOP = 24;
	/** How far back the stands' asteroids reach (|dz|), for building and clearing them. */
	public static final int STAND_BACK = STAND_IN + 2 * ROWS + 4;
	/** Spires and rocks are built out to this far. */
	public static final int SIZE = 240;

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

	/**
	 * A monolith: where it stands (adx out, dz across), its lowest point (dy, hanging down into space) and
	 * its top; the crystal floats over it.
	 */
	public record Monolith(int x, int z, int tip, int top) {
		/** Where its crystal floats (dy of the crystal's middle). */
		public double crystalY() {
			return top + 6.5;
		}

		/** Which way its broad face (with the seam of light) faces: along x (true), or across. */
		public boolean facesAlongX() {
			return Math.abs(GOAL_X - x) >= Math.abs(z);
		}

		/** Its lowest and highest blocks, for crumbling it. */
		public int base() {
			return tip;
		}
	}

	/**
	 * In a horseshoe round the goal, in the order they crumble: the two either side first, then the two
	 * behind, the middle one (the tallest) last.
	 */
	public static final Monolith[] MONOLITHS = {new Monolith(50, -27, -14, 30), new Monolith(50, 27, -13, 31), new Monolith(64, -17, -16, 33),
			new Monolith(64, 17, -15, 34), new Monolith(74, 0, -17, 37)};
	/** A monolith's half width across its broad face and half its thickness (the main slab), and how far further its lip stands out. */
	private static final int MONO_HW = 6, MONO_HD = 2, MONO_LIP = 2;
	/** How far out from a monolith's middle it reaches (with its lip). */
	private static final int MONO_REACH = MONO_HW + MONO_LIP;

	/** The goal (where you score) of the base on side s: a block position (relative) in the pit. */
	public static boolean inGoal(double dx, double dy, double dz, int s) {
		return Math.abs(dx - (s * GOAL_X + 0.5)) < GOAL_HALF + 0.75 && Math.abs(dz - 0.5) < GOAL_HALF + 0.75 && dy < TOP + 0.6 && dy > TOP - GOAL_DEPTH - 1;
	}

	// ---------------------------------------------------------------- outlines

	/** How far a column is from the edge of a shape (0 on the edge), -1 off it; and which way its edge runs. */
	private static final int W = AREA_X + 2, D = 2 * AREA_Z + 3;
	private static final int[][] DECK_IN = new int[W][D], LOW_IN = new int[W][D], HIGH_IN = new int[W][D];
	private static final boolean[][] DECK_X = new boolean[W][D], LOW_X = new boolean[W][D], HIGH_X = new boolean[W][D];

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

	/** The temple's lower tier, and its upper (in from the front and back), each with the landings at the top of the stairs. */
	public static boolean lowTier(int adx, int dz) {
		int adz = Math.abs(dz);
		return adx >= TEMPLE_FRONT && adx <= TEMPLE_BACK && (adz <= TEMPLE_HALF || adx >= LANDING_FROM && adz <= STAIR_OUT);
	}

	public static boolean highTier(int adx, int dz) {
		int adz = Math.abs(dz);
		return adx >= TEMPLE_FRONT + UPPER_IN && adx <= TEMPLE_BACK - UPPER_IN && (adz <= TEMPLE_HALF || adx >= LANDING_FROM && adz <= STAIR_OUT);
	}

	private interface Shape {
		boolean in(int adx, int dz);
	}

	private static void distances(int[][] out, boolean[][] alongX, Shape shape, Shape alsoInside) {
		Shape inside = (a, z) -> shape.in(a, z) || alsoInside.in(a, z);
		for (int adx = 0; adx < W; adx++) {
			for (int dz = -AREA_Z - 1; dz <= AREA_Z + 1; dz++) {
				int ix = adx, iz = dz + AREA_Z + 1;
				if (!shape.in(adx, dz)) {
					out[ix][iz] = -1;
					continue;
				}
				int found = 14;
				search:
				for (int k = 1; k <= 14; k++) {
					for (int ox = -k; ox <= k; ox++) {
						for (int oz = -k; oz <= k; oz++) {
							if (Math.max(Math.abs(ox), Math.abs(oz)) == k && !inside.in(adx + ox, dz + oz)) {
								found = k - 1;
								break search;
							}
						}
					}
				}
				out[ix][iz] = found;
				// Which way the edge runs: along x where the outside is across (in z), else across. (Lights laid
				// along the edge, so a run of them reads as one band, the coloured lines along its top and bottom.)
				int k = found + 1;
				boolean outZ = !inside.in(adx, dz + k) || !inside.in(adx, dz - k), outX = !inside.in(adx + k, dz) || !inside.in(adx - k, dz);
				alongX[ix][iz] = outZ || !outX;
			}
		}
	}

	static {
		// The bridge counts as deck where it joins (so the deck isn't edged across the end of the bridge).
		distances(DECK_IN, DECK_X, BridgeLayout::onDeck, (adx, dz) -> adx >= BRIDGE_END - 3 && adx <= BRIDGE_END && Math.abs(dz) <= 2);
		distances(LOW_IN, LOW_X, BridgeLayout::lowTier, (adx, dz) -> false);
		distances(HIGH_IN, HIGH_X, BridgeLayout::highTier, (adx, dz) -> false);
	}

	private static int inset(int[][] table, int adx, int dz) {
		if (adx < 0 || adx >= W || dz < -AREA_Z - 1 || dz > AREA_Z + 1) return -1;
		return table[adx][dz + AREA_Z + 1];
	}

	private static Direction.Axis axis(boolean[][] table, int adx, int dz) {
		return table[Math.min(adx, W - 1)][Math.max(0, Math.min(D - 1, dz + AREA_Z + 1))] ? Direction.Axis.X : Direction.Axis.Z;
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

	/** Force field: 0 full, then 1 to 5 fading out a block at a time (5 fades to nothing at its top). */
	private static BlockState field(Block block, int fade) {
		return block.defaultBlockState().setValue(ForceFieldBlock.FADE, Math.max(0, Math.min(5, fade)));
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
			int lx = adx - m.x(), lz = dz - m.z();
			if (Math.abs(lx) <= MONO_REACH && Math.abs(lz) <= MONO_REACH && dy >= m.tip() && dy <= m.top()) {
				BlockState mb = monolithBlock(m, lx, dy, lz, blue, dx, dz);
				if (mb != null) return mb;
			}
		}
		if (adx < BASE_FRONT || adx > BASE_BACK) return null;
		return dy >= 1 ? temple(adx, dy, dz, blue) : deck(adx, dy, dz, blue);
	}

	/** The deck and the island under it. */
	private static BlockState deck(int adx, int dy, int dz, boolean blue) {
		int e = inset(DECK_IN, adx, dz);
		if (e < 0) return null;
		Direction.Axis axis = axis(DECK_X, adx, dz);
		// Where the bridge comes in, a plain abutment under its end (the bands either side stop at it).
		if (adx <= BASE_FRONT + 1 && Math.abs(dz) <= 2 && dy < 0 && dy >= -3) return dy == -3 ? b(Blocks.POLISHED_ANDESITE) : b(Blocks.SMOOTH_STONE);
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
				// The island under it: a rough bowl of rock, deepest in the middle.
				if (dy > 0 || e <= 2) return null;
				int x = blue ? -adx : adx;
				double depth = Math.min(22, 5 + (e - 2) * 1.45 + noise(x, dz, 6, 501) * 4 - 2);
				if (-dy > depth) return null;
				// Down through it, pale to dark grey and into red at the bottom.
				return islandRock(x, dy, dz, Math.min(1, (-dy - 5) / 17.0));
			}
		}
	}

	/**
	 * A tier's edge, from its top (dy = top) down: a quartz lip, a band of light set back under it, a lower
	 * lip; then its walls (e: how far in from the edge; null for air).
	 */
	private static BlockState tierEdge(int e, int dy, int top, boolean blue, Direction.Axis axis) {
		if (dy == top) return e == 0 ? b(Blocks.SMOOTH_QUARTZ) : null;
		if (dy == top - 1) return e == 0 ? b(Blocks.AIR) : e == 1 ? light(blue, axis) : null;
		if (dy == top - 2) return e == 0 ? b(Blocks.POLISHED_ANDESITE) : null;
		return null;
	}

	/** The temple on the deck: the stairs, its two tiers with the room and the goal, the force field over the goal. */
	private static BlockState temple(int adx, int dy, int dz, boolean blue) {
		int adz = Math.abs(dz);
		// The stairs up both sides, filled in underneath.
		if (adx >= STAIR_FROM && adx < LANDING_FROM && adz >= STAIR_IN && adz <= STAIR_OUT) {
			int k = adx - STAIR_FROM;
			if (dy <= k) return adz == STAIR_OUT ? b(Blocks.POLISHED_ANDESITE) : b(Blocks.SMOOTH_STONE);
			if (dy == k + 1) return stairs(adz == STAIR_OUT ? Blocks.POLISHED_ANDESITE_STAIRS : Blocks.SMOOTH_QUARTZ_STAIRS, blue);
			return null;
		}
		int ring = Math.max(Math.abs(adx - GOAL_X), adz);
		// The force field round the goal, rising from a line of light in the top and fading out upwards.
		if (dy > TOP && dy <= TOP + FIELD_HIGH && ring == FIELD_RING) return field(blue ? ArenaBlocks.FIELD_BLUE : ArenaBlocks.FIELD_RED, dy - TOP - 1);
		if (dy == TOP && ring == FIELD_RING) return light(blue, Math.abs(adx - GOAL_X) == FIELD_RING && adz < FIELD_RING ? Direction.Axis.Z : Direction.Axis.X);
		if (dy > TOP) return null;
		// The goal: a glowing pit with a ring of light round its top, the team's colour round it.
		if (ring <= GOAL_HALF && dy >= TOP - GOAL_DEPTH) return dy == TOP - GOAL_DEPTH ? b(ArenaBlocks.LIGHT_WHITE) : null;
		if (ring == GOAL_HALF + 1 && dy >= TOP - GOAL_DEPTH && dy < TOP) {
			return dy == TOP - 1 ? light(blue, Math.abs(adx - GOAL_X) == GOAL_HALF + 1 ? Direction.Axis.Z : Direction.Axis.X) : b(Blocks.WHITE_CONCRETE);
		}
		boolean high = highTier(adx, dz), low = lowTier(adx, dz);
		if (dy > TIER) {
			// The upper tier.
			if (!high) return null;
			int e = inset(HIGH_IN, adx, dz);
			BlockState edge = tierEdge(e, dy, TOP, blue, axis(HIGH_X, adx, dz));
			if (edge != null) return edge.isAir() ? null : edge;
			if (dy == TOP) {
				if (ring == GOAL_HALF + 1) return wool(blue);
				if (ring == GOAL_HALF + 2) return b(Blocks.SMOOTH_QUARTZ);
				if (Math.floorMod(adx, 4) == 0 || Math.floorMod(dz, 4) == 0) return b(Blocks.POLISHED_ANDESITE);
				return b(Blocks.SMOOTH_STONE);
			}
			if (e == 0) {
				// Its walls, with strips of light up the front either side.
				if (adx == TEMPLE_FRONT + UPPER_IN && adz == 5) return light(blue, Direction.Axis.Y);
				return dy == TIER + 1 ? b(Blocks.POLISHED_ANDESITE) : b(Blocks.SMOOTH_STONE);
			}
			return b(Blocks.STONE);
		}
		// The lower tier.
		if (!low) return null;
		int e = inset(LOW_IN, adx, dz);
		// The room inside the door, lit along its ceiling, and the door.
		if (adx >= ROOM_FROM && adx <= ROOM_TO && adz <= ROOM_HALF && dy <= ROOM_TOP) return null;
		if (adx > ROOM_FROM && adx <= ROOM_TO && dz == 0 && dy == ROOM_TOP + 1) return light(blue, Direction.Axis.X);
		if (adx == TEMPLE_FRONT && adz <= DOOR_HALF && dy <= DOOR_TOP) return null;
		if (adx == TEMPLE_FRONT && adz == DOOR_HALF + 1 && dy <= DOOR_TOP + 1) return b(Blocks.SMOOTH_QUARTZ);
		if (adx == TEMPLE_FRONT && adz <= DOOR_HALF && dy == DOOR_TOP + 1) return b(Blocks.SMOOTH_QUARTZ);
		// Where the upper tier stands on it, its own; elsewhere its edge.
		if (!(high && dy == TIER)) {
			BlockState edge = tierEdge(e, dy, TIER, blue, axis(LOW_X, adx, dz));
			if (edge != null) return edge.isAir() ? null : edge;
		}
		if (e == 0) {
			if (adx == TEMPLE_FRONT && adz == 6) return light(blue, Direction.Axis.Y);
			return dy == 1 ? b(Blocks.POLISHED_ANDESITE) : b(Blocks.SMOOTH_STONE);
		}
		if (dy == TIER && !high) return b(Blocks.SMOOTH_STONE);
		return b(Blocks.STONE);
	}

	/** Light grey stone from light to dark, for the monoliths (LumenForge's pale look). */
	private static final Block[] MONO_RAMP = {Blocks.CALCITE, Blocks.POLISHED_DIORITE, Blocks.SMOOTH_STONE, Blocks.DIORITE, Blocks.POLISHED_ANDESITE,
			Blocks.ANDESITE, Blocks.STONE, Blocks.TUFF};

	/** Whether (a across, o through, from the middle) is in a monolith's footprint grown by g: a rounded rectangle. */
	private static boolean monoIn(int a, int o, int g) {
		int ha = MONO_HW + g, ho = MONO_HD + g;
		if (a > ha || o > ho) return false;
		if (a == ha && o == ho) return false;
		return g < 2 || !(a == ha && o == ho - 1 || o == ho && a == ha - 1);
	}

	/**
	 * A monolith (lx, lz from its middle, dy from the bridge), built as LumenForge's Portal builder builds
	 * its monolith slabs: a broad slab with rounded corners, its broad face to the goal, floating; its foot
	 * rounded over; a band a block proud above the foot; a capital at the top, a band a block proud and
	 * over it a lip two proud, in quartz; up each side a pillar, the face between them sunk, a groove where
	 * they meet and a seam down the middle. Shaded as stone is: its own gradient from foot (darker) to top
	 * (lighter), the sunk faces and the grooves' edges darker. A glowing socket on top, under its crystal.
	 */
	private static BlockState monolithBlock(Monolith m, int lx, int dy, int lz, boolean blue, int dx, int dz) {
		boolean alongX = m.facesAlongX();
		int a = Math.abs(alongX ? lz : lx), o = Math.abs(alongX ? lx : lz);
		int tip = m.tip(), top = m.top();
		if (dy < tip || dy > top) return null;
		boolean main = monoIn(a, o, 0), band = monoIn(a, o, 1), lipRing = monoIn(a, o, 2);
		if (!lipRing) return null;
		int footBand = tip + 4, capBand = top - 5;
		boolean isBand = band && (dy == footBand || dy == footBand + 1 || dy == capBand || dy == capBand + 1 || dy == top - 1);
		boolean isLip = lipRing && dy == top;
		if (!main && !isBand && !isLip) return null;
		// Its foot rounded over (seen face on).
		double rb = 4;
		if (dy < tip + rb && Math.hypot(Math.max(a - (MONO_HW + 0.5 - rb), 0), tip + rb - (dy + 0.5)) > rb + (isBand ? 1 : 0)) return null;
		double t = Math.max(0, Math.min(1, 0.55 * (dy - tip) / (double) (top - tip) + 0.45 * face(dx, dy, dz, 750)));
		double v = 0.08 + (1 - t) * 0.62;
		if (dy == top) {
			if (a == 0 && o == 0) return light(blue, Direction.Axis.Y);
			return b(Blocks.SMOOTH_QUARTZ);
		}
		// The bands and the lip stand out in quartz; where they cross the slab itself, its own stone.
		if (isBand && !main) return b(dy == top - 1 ? Blocks.SMOOTH_QUARTZ : Blocks.QUARTZ_BLOCK);
		if (isBand) return b(pick(MONO_RAMP, v - 0.06, dx, dy, dz, 751));
		// Pillars up each side (the outer two across), the face between them sunk a block, a groove where they
		// meet, a seam down the middle of the sunk face; between the bands.
		boolean shaft = dy > footBand + 1 && dy < capBand;
		int front = MONO_HD - o;   // in from the broad face (0: on it)
		if (shaft) {
			boolean mid = a < MONO_HW - 2;
			boolean groove = a == MONO_HW - 2 && dy > footBand + 2 && dy < capBand - 1;
			if (mid && front < 1 || groove && front < 2) return null;
			if (a == 0 && front < 2 && dy > footBand + 3 && dy < capBand - 2) return null;
			double ao = 0;
			if (mid && front == 1) ao = a >= MONO_HW - 3 || a == 1 ? 0.2 : 0.1;
			else if (!mid && (a == MONO_HW || a == MONO_HW - 1 && groove)) ao = 0.12;
			if (groove && front == 2) ao = 0.24;
			return b(pick(MONO_RAMP, v + ao, dx, dy, dz, 752));
		}
		// The rounded corners a shade darker, as worn stone is.
		boolean corner = a == MONO_HW && o >= MONO_HD - 1 || o == MONO_HD && a >= MONO_HW - 1;
		return b(pick(MONO_RAMP, v + (corner ? 0.1 : 0), dx, dy, dz, 754));
	}

	/** Which monolith (0 to 4) a relative block position belongs to, or -1. */
	public static int monolithAt(int dx, int dy, int dz) {
		int adx = Math.abs(dx);
		for (int i = 0; i < MONOLITHS.length; i++) {
			Monolith m = MONOLITHS[i];
			int lx = adx - m.x(), lz = dz - m.z();
			if (Math.abs(lx) <= MONO_REACH && Math.abs(lz) <= MONO_REACH && monolithBlock(m, lx, dy, lz, false, dx, dz) != null) return i;
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
	 * decks, but not on or round the temples (the stairs, the goals, the doors), the monoliths or the stands.
	 */
	public static boolean buildable(int dx, int dy, int dz) {
		int adx = Math.abs(dx), adz = Math.abs(dz);
		if (adx > AREA_X - 3 || adz > AREA_Z - 2 || dy < -14 || dy > 22) return false;
		if (adx <= WALL_END + 2 && adz > WALL_Z - 2) return false;
		if (adx >= TEMPLE_FRONT - 3 && adx <= TEMPLE_BACK + 1 && adz <= STAIR_OUT + 1 && dy >= 0) return false;
		for (Monolith m : MONOLITHS) {
			if (Math.abs(adx - m.x()) <= MONO_REACH + 1 && Math.abs(dz - m.z()) <= MONO_REACH + 1) return false;
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

	/** How deep the asteroid under a stand reaches at a column (dy, below 0), or 0 off it. */
	private static int asteroidDepth(int adx, int adz) {
		double cz = STAND_IN + ROWS, rz = ROWS + 4, rx = STAND_HALF_X + 4;
		double e = (adx / rx) * (adx / rx) + ((adz - cz) / rz) * ((adz - cz) / rz);
		if (e >= 1) return 0;
		double jag = (noise(adx, adz, 4, 811) - 0.5) * 7 + (hash(adx >> 1, 0, adz >> 1, 812) - 0.5) * 3;
		return (int) Math.round(-(3 + 19 * Math.pow(1 - e, 0.65) + jag));
	}

	/**
	 * The stands' block at a relative position, or null (for both sides; dz's sign picks the side): rows of
	 * seats on a jagged asteroid of red rock, and a wall of force field in front, fading out upwards.
	 */
	public static BlockState standBlock(int dx, int dy, int dz) {
		int adx = Math.abs(dx), adz = Math.abs(dz);
		if (adx > STAND_HALF_X + 4) return null;
		int back = STAND_IN + 2 * ROWS;
		// The force field between the playing area and the stands (you can't get to the crowd; nor can you
		// build round its ends).
		if (adz == WALL_Z && adx <= WALL_END) return dy >= -3 && dy <= WALL_TOP ? field(ArenaBlocks.FIELD_WALL, dy - (WALL_TOP - 5)) : null;
		if (adz < STAND_IN - 1 || adz > back + 4) return null;
		// Under it all, an asteroid.
		int floor = asteroidDepth(adx, adz);
		int bottom = adx > STAND_HALF_X + 1 ? -3 : adz >= back ? -2 : adx == STAND_HALF_X + 1 ? -2 : adz == STAND_IN - 1 ? -1 : rowTop(Math.max(0, (adz - STAND_IN) / 2)) - 3;
		if (floor < 0 && dy < bottom && dy >= floor) {
			double up = 1 - (double) (bottom - dy) / Math.max(1, bottom - floor);
			return rock(dx, dy, dz, 0.5, up);
		}
		if (adz > back + 2 || adx > STAND_HALF_X + 1) return null;
		// The ends and the back wall.
		if (adz >= back) {
			if (adx > STAND_HALF_X) return null;
			if (dy == rowTop(ROWS - 1) + 7) return b(Blocks.SEA_LANTERN);
			if (dy == rowTop(ROWS - 1) + 5) return b(Blocks.ORANGE_CONCRETE);
			if (dy >= -2 && dy <= rowTop(ROWS - 1) + 7) return b(Blocks.LIGHT_GRAY_CONCRETE);
			return null;
		}
		if (adx == STAND_HALF_X + 1) {
			int top = adz < STAND_IN ? -1 : rowTop((adz - STAND_IN) / 2) + 2;
			if (dy >= -2 && dy <= top) return dy == top ? b(Blocks.SEA_LANTERN) : b(Blocks.LIGHT_GRAY_CONCRETE);
			return null;
		}
		// The front rail: lit at the foot.
		if (adz == STAND_IN - 1) {
			if (dy == -1) return b(Blocks.SEA_LANTERN);
			if (dy == 0) return b(Blocks.POLISHED_DEEPSLATE);
			return null;
		}
		int row = (adz - STAND_IN) / 2, top = rowTop(row);
		boolean front = (adz - STAND_IN) % 2 == 0;
		if (dy == top) return Math.floorMod(dx, 8) == 0 ? b(Blocks.SMOOTH_STONE) : front ? b(Blocks.POLISHED_ANDESITE) : b(Blocks.ANDESITE);
		if (dy < top && dy >= top - 3) return b(Blocks.STONE_BRICKS);
		return null;
	}

	// ---------------------------------------------------------------- spires and rocks

	/** One of the rock spires round the map: where its foot is (relative), how wide and tall. */
	public record Spire(int x, int y, int z, double radius, int height, long seed) {}

	public static final int SPIRES = 34;

	public static Spire spire(int i) {
		return spire(i, 64, 100);
	}

	/** The spires as the last version of the map had them (closer in), so a rebuild can clear any that have moved. */
	public static Spire previousSpire(int i) {
		return spire(i, 45, 80);
	}

	private static Spire spire(int i, int clearZ, int clearX) {
		// Round the map at all heights, clear of the playing area and the stands.
		double a = (i + hash(i, 0, 0, 70) * 0.7) / SPIRES * Math.PI * 2, dist = 82 + hash(i, 1, 0, 71) * 95;
		int x = (int) (Math.cos(a) * dist * 1.2), z = (int) (Math.sin(a) * dist);
		if (Math.abs(z) < clearZ && Math.abs(x) < clearX) z += z < 0 ? -clearZ : clearZ;
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

	// ---------------------------------------------------------------- rock, in gradients

	/**
	 * Rock from light to dark, as LumenForge's Island Generator paints its rock faces: a block is picked
	 * along one of these by a smooth value (big patches, streaks running down the face, faint layers,
	 * lighter at the top and darker underneath) with a little jitter, so the tones run into each other
	 * instead of standing in stripes. Red for the spires and the monoliths' rocks, grey for the islands.
	 */
	public static final Block[] RED_RAMP = {Blocks.SMOOTH_RED_SANDSTONE, Blocks.POLISHED_GRANITE, Blocks.GRANITE, Blocks.TERRACOTTA, Blocks.ORANGE_TERRACOTTA,
			Blocks.RED_TERRACOTTA, Blocks.RED_TERRACOTTA, Blocks.NETHERRACK, Blocks.NETHERRACK, Blocks.BROWN_TERRACOTTA, Blocks.RED_NETHER_BRICKS};
	public static final Block[] GREY_RAMP = {Blocks.CALCITE, Blocks.DIORITE, Blocks.ANDESITE, Blocks.ANDESITE, Blocks.STONE, Blocks.STONE, Blocks.STONE, Blocks.TUFF,
			Blocks.TUFF, Blocks.DEEPSLATE, Blocks.COBBLED_DEEPSLATE};

	/** Smooth 3D value noise, 0 to 1. */
	public static double noise3(double x, double y, double z, long salt) {
		long x0 = (long) Math.floor(x), y0 = (long) Math.floor(y), z0 = (long) Math.floor(z);
		double fx = x - x0, fy = y - y0, fz = z - z0;
		fx = fx * fx * (3 - 2 * fx);
		fy = fy * fy * (3 - 2 * fy);
		fz = fz * fz * (3 - 2 * fz);
		double c000 = hash(x0, y0, z0, salt), c100 = hash(x0 + 1, y0, z0, salt), c010 = hash(x0, y0 + 1, z0, salt), c110 = hash(x0 + 1, y0 + 1, z0, salt);
		double c001 = hash(x0, y0, z0 + 1, salt), c101 = hash(x0 + 1, y0, z0 + 1, salt), c011 = hash(x0, y0 + 1, z0 + 1, salt), c111 = hash(x0 + 1, y0 + 1, z0 + 1, salt);
		double a = c000 + (c100 - c000) * fx, b = c010 + (c110 - c010) * fx, c = c001 + (c101 - c001) * fx, d = c011 + (c111 - c011) * fx;
		double e = a + (b - a) * fy, f = c + (d - c) * fy;
		return e + (f - e) * fz;
	}

	/** How a rock face reads at a block, about 0 to 1: patches tens of blocks across, streaks down it. */
	public static double face(int x, int y, int z, long salt) {
		double p = noise3(x / 15.0, y / 10.0, z / 15.0, salt) * 0.7 + noise3(x / 5.0, y / 4.0, z / 5.0, salt + 1) * 0.3;
		double s1 = noise((x * 0.8 + z * 0.6) / 3.4, y / 26.0 + 7.1, 1, salt + 2), s2 = noise((x * 0.6 - z * 0.8) / 3.4, y / 26.0 + 3.3, 1, salt + 3);
		return 0.5 + (p - 0.5) * 1.7 + (Math.max(s1, s2) - 0.5) * 0.6;
	}

	/** The block v of the way along a ramp (0 light, 1 dark), jittered a little so neighbouring tones mix. */
	public static Block pick(Block[] ramp, double v, int x, int y, int z, long salt) {
		v += (hash(x, y, z, salt) - 0.5) * 0.12;
		return ramp[Math.max(0, Math.min(ramp.length - 1, (int) Math.floor(v * ramp.length)))];
	}

	/**
	 * Red rock for the spires and the monoliths' rocks. depth: how far in from the surface (inside it's
	 * all netherrack, unseen); up: 0 at the foot to 1 at the top, light above and dark below; soft
	 * wavering layers a few blocks apart.
	 */
	public static BlockState rock(int x, int y, int z, double depth, double up) {
		if (depth > 2.2) return b(Blocks.NETHERRACK);
		double band = Math.sin((y + noise(x, z, 9, 77) * 5) * Math.PI * 2 / 6.5) * 0.07;
		double v = 0.06 + (1 - up) * 0.74 + (face(x, y, z, 700) - 0.5) * 0.5 + band - (depth < 0.8 ? 0.05 : 0);
		return b(pick(RED_RAMP, v, x, y, z, 701));
	}

	/**
	 * Under a base's deck: grey rock, pale just under the deck and darker downwards, running into red
	 * towards the bottom (t: 0 at the top of the rock to 1 at its lowest).
	 */
	public static BlockState islandRock(int x, int y, int z, double t) {
		double f = face(x, y, z, 720);
		double red = Math.max(0, Math.min(1, (t - 0.42) / 0.45));
		red = red * red * (3 - 2 * red) * 0.85;
		if (hash(x, y, z, 721) < red) return b(pick(RED_RAMP, 0.32 + t * 0.5 + (f - 0.5) * 0.6, x, y, z, 722));
		return b(pick(GREY_RAMP, 0.14 + t * 0.66 + (f - 0.5) * 0.7, x, y, z, 723));
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
