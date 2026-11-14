package com.insidethecube.orbitarena.arena;

import static com.insidethecube.orbitarena.arena.ArenaLayout.*;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Builds an arena a slice at a time (a few hundred columns a tick), so the server never stalls: the
 * fighting floor first, then the stands, the roof, the asteroid round it and the floating rocks.
 * Everything is worked out from ArenaLayout, so building again repairs the arena exactly.
 */
public final class ArenaBuilder {
	/** Bump when the arena's design changes, so existing arenas are rebuilt. */
	public static final int VERSION = 1;
	/** The marker under the centre that says which version an arena is. */
	private static final BlockState MARKER = Blocks.REINFORCED_DEEPSLATE.defaultBlockState();

	private static final int SIZE = 112;
	private static final int COLUMNS_PER_TICK = 420;

	private final ServerLevel level;
	private final int cx;
	private final List<int[]> columns = new ArrayList<>();
	private int next;
	private final List<int[]> rocks = new ArrayList<>();
	private int rockNext;

	public ArenaBuilder(ServerLevel level, int centreX) {
		this.level = level;
		this.cx = centreX;
		// Columns from the middle out, so the floor is there first.
		for (int x = -SIZE; x <= SIZE; x++) for (int z = -SIZE; z <= SIZE; z++) if (radius(x, z) <= SIZE) columns.add(new int[] {x, z});
		columns.sort((a, b) -> Double.compare(radius(a[0], a[1]), radius(b[0], b[1])));
		for (int i = 0; i < 14; i++) rocks.add(new int[] {i});
	}

	/** Whether the arena at this centre is built (and up to date). */
	public static boolean built(ServerLevel level, int centreX) {
		return level.getBlockState(new BlockPos(centreX, FLOOR - 3 - VERSION, 0)).is(MARKER.getBlock());
	}

	public boolean done() {
		return next >= columns.size() && rockNext >= rocks.size();
	}

	/** 0 to 1. */
	public float progress() {
		return (next + rockNext * 400f) / (columns.size() + rocks.size() * 400f);
	}

	/** Builds the next slice; true when it's all done. */
	public boolean tick() {
		int end = Math.min(columns.size(), next + COLUMNS_PER_TICK);
		for (; next < end; next++) column(columns.get(next)[0], columns.get(next)[1]);
		if (next >= columns.size() && rockNext < rocks.size()) floatingRock(rockNext++);
		if (done()) set(0, FLOOR - 3 - VERSION, 0, MARKER);
		return done();
	}

	private void set(int x, int y, int z, BlockState state) {
		level.setBlock(new BlockPos(cx + x, y, z), state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
	}

	private void fill(int x, int z, int y0, int y1, BlockState state) {
		for (int y = y0; y <= y1; y++) set(x, y, z, state);
	}

	private void clear(int x, int z, int y0, int y1) {
		fill(x, z, y0, y1, Blocks.AIR.defaultBlockState());
	}

	private static BlockState b(Block block) {
		return block.defaultBlockState();
	}

	private void column(int x, int z) {
		double r = radius(x + 0.5, z + 0.5), a = angle(x + 0.5, z + 0.5);
		int f = FLOOR;
		// Clear the space above the floor up to the roof (a rebuild repairs anything broken).
		if (r < ROOF_OUT) clear(x, z, f + 1, f + ROOF + 1);
		// The underside of the asteroid, three blocks thick, under everything.
		double outer = rockOuter(a);
		if (r < outer) {
			int bottom = f + rockBottom(x, z);
			fill(x, z, bottom, bottom + 2, rock(x, bottom, z));
		}
		if (r < CENTRE + 1) centre(x, z, r);
		else if (r < WALL_IN) honeycomb(x, z, r);
		else if (r < WALL_OUT) wall(x, z, a);
		else if (r < CONCOURSE_IN) lowerDeck(x, z, r, a);
		else if (r < CONCOURSE_OUT) concourse(x, z, a);
		else if (r < BACK_WALL) upperDeck(x, z, r, a);
		else if (r < ROOF_OUT) backWall(x, z, a);
		if (r >= ROOF_IN && r < ROOF_OUT) roof(x, z, r, a);
		if (r >= ROOF_OUT && r < outer) rockRim(x, z, r, outer);
		if (r < WALL_OUT) fill(x, z, f - 2, f - 1, b(Blocks.DEEPSLATE));
		// Tunnels at floor level through the wall and the first rows.
		if (r >= WALL_IN - 0.5 && r < 47.5) {
			for (double t : TUNNELS) {
				double arc = Math.toRadians(angleDiff(a, t)) * r;
				if (arc < 2.6) {
					clear(x, z, f + 1, f + 4);
					if (r >= 46.5) fill(x, z, f + 1, f + 4, b(Blocks.BLACK_CONCRETE));
				} else if (arc < 3.6 && r < WALL_OUT + 0.5) {
					fill(x, z, f + 1, f + 5, b(Blocks.ORANGE_CONCRETE));
				}
				if (arc < 3.6 && r < WALL_OUT + 0.5) set(x, f + 5, z, b(Blocks.ORANGE_CONCRETE));
			}
		}
	}

	private void centre(int x, int z, double r) {
		int f = FLOOR;
		BlockState floor;
		if (r < 1.3) floor = b(Blocks.SEA_LANTERN);
		else if (r >= CENTRE) floor = b(Blocks.WHITE_CONCRETE);
		else if (r >= 9.8 && r < 11.3) floor = b(Blocks.WHITE_CONCRETE);
		else if (Math.abs(x) == 0 || Math.abs(z) == 0) floor = b(Blocks.SMOOTH_STONE);
		else floor = hash(x, 0, z, 1) < 0.25 ? b(Blocks.SMOOTH_STONE) : b(Blocks.LIGHT_GRAY_CONCRETE);
		set(x, f, z, floor);
		// The dais: stepped discs, lit in the middle.
		if (r < DAIS) set(x, f + 1, z, b(Blocks.POLISHED_ANDESITE));
		if (r < 4.6) set(x, f + 2, z, b(Blocks.SMOOTH_STONE));
		if (r < 2.6) set(x, f + 3, z, r < 1.3 ? b(Blocks.SEA_LANTERN) : b(Blocks.POLISHED_DIORITE));
		if (r < 2.6) set(x, f + 1, z, b(Blocks.SEA_LANTERN));
	}

	private void honeycomb(int x, int z, double r) {
		int f = FLOOR;
		boolean line = hexLine(x + 0.5, z + 0.5, 11) < 0.62;
		BlockState s = line ? b(Blocks.SEA_LANTERN) : hash(x, 1, z, 2) < 0.15 ? b(Blocks.POLISHED_DEEPSLATE) : b(Blocks.GRAY_CONCRETE);
		if (r >= WALL_IN - 1.4) s = b(Blocks.POLISHED_BLACKSTONE_BRICKS);
		set(x, f, z, s);
		// The pedestals: a dark base, a pale step lit from underneath your feet.
		for (int side : new int[] {-1, 1}) {
			int px = side * PEDESTAL_X;
			if (Math.abs(x - px) <= 2 && Math.abs(z) <= 2) set(x, f, z, b(Blocks.BLACK_CONCRETE));
			if (Math.abs(x - px) <= 1 && Math.abs(z) <= 1) set(x, f + 1, z, x == px && z == 0 ? b(Blocks.SEA_LANTERN) : b(Blocks.SMOOTH_STONE));
		}
	}

	private void wall(int x, int z, double a) {
		int f = FLOOR;
		fill(x, z, f, f + 4, b(Blocks.POLISHED_DEEPSLATE));
		set(x, f + 2, z, b(Blocks.DEEPSLATE_TILES));
		// Lights along the foot of the wall, every 12 degrees.
		if (angleDiff(a, Math.round(a / 12.0) * 12.0) < 0.9) set(x, f + 1, z, b(Blocks.SEA_LANTERN));
		set(x, f + 5, z, b(Blocks.POLISHED_BLACKSTONE_BRICKS));
	}

	/** A row of stands: its top, two below, and the step in front lighter in the aisles. */
	private void row(int x, int z, int top, double r, double a) {
		BlockState s = aisle(a, r) ? b(Blocks.POLISHED_ANDESITE) : b(Blocks.DEEPSLATE_TILES);
		set(x, top, z, s);
		fill(x, z, top - 3, top - 1, b(Blocks.DEEPSLATE_BRICKS));
	}

	private void lowerDeck(int x, int z, double r, double a) {
		int k = (int) ((r - LOWER_IN) / 2);
		row(x, z, FLOOR + LOWER_BASE + k, r, a);
	}

	private void concourse(int x, int z, double a) {
		int f = FLOOR;
		fill(x, z, f + 10, f + 12, b(Blocks.DEEPSLATE_BRICKS));
		set(x, f + 13, z, b(Blocks.LIGHT_GRAY_CONCRETE));
	}

	private void upperDeck(int x, int z, double r, double a) {
		int f = FLOOR;
		if (box(a) && r >= UPPER_IN + 2) {
			box(x, z, r, a);
			return;
		}
		int k = (int) ((r - UPPER_IN) / 2);
		if (r < UPPER_IN + 1) {
			// The face below the upper deck: an orange band with floodlights.
			fill(x, z, f + 13, f + 17, b(Blocks.LIGHT_GRAY_CONCRETE));
			set(x, f + 15, z, b(Blocks.ORANGE_CONCRETE));
			set(x, f + 14, z, b(Blocks.ORANGE_TERRACOTTA));
			if (angleDiff(a, Math.round(a / 9.0) * 9.0) < 0.8) set(x, f + 16, z, b(Blocks.SEA_LANTERN));
			return;
		}
		row(x, z, FLOOR + UPPER_BASE + k, r, a);
	}

	/** One of the boxes in the upper deck: an orange shell, a window, lit eyes along the top. */
	private void box(int x, int z, double r, double a) {
		int f = FLOOR;
		double edge = 7 - Math.min(7, angleDiffAny(a));
		boolean side = edge < 1.0;
		if (r < UPPER_IN + 3.5) {
			fill(x, z, f + 17, f + 19, b(Blocks.ORANGE_CONCRETE));
			if (side) fill(x, z, f + 20, f + 24, b(Blocks.ORANGE_CONCRETE));
			else fill(x, z, f + 20, f + 24, b(Blocks.GRAY_STAINED_GLASS));
			fill(x, z, f + 25, f + 28, b(Blocks.ORANGE_CONCRETE));
			if (!side && hash(x, 3, z, 5) < 0.5 && (int) (angleDiffAny(a) * 3) % 3 == 0) set(x, f + 26, z, b(Blocks.SEA_LANTERN));
		} else {
			set(x, f + 19, z, b(Blocks.GRAY_CONCRETE));
			set(x, f + 28, z, b(Blocks.ORANGE_CONCRETE));
			if (side || r > BACK_WALL - 2) fill(x, z, f + 20, f + 27, b(Blocks.ORANGE_CONCRETE));
		}
	}

	private static double angleDiffAny(double a) {
		double best = 360;
		for (double b : BOXES) best = Math.min(best, angleDiff(a, b));
		return best;
	}

	private void backWall(int x, int z, double a) {
		int f = FLOOR;
		fill(x, z, f + UPPER_BASE + UPPER_ROWS - 3, f + ROOF, b(Blocks.LIGHT_GRAY_CONCRETE));
		set(x, f + ROOF - 3, z, b(Blocks.PURPLE_CONCRETE));
		set(x, f + ROOF - 4, z, b(Blocks.MAGENTA_TERRACOTTA));
	}

	/** The roof ring: a slab overhanging the top deck, with zig-zag light strips under it and a lit edge. */
	private void roof(int x, int z, double r, double a) {
		int f = FLOOR;
		set(x, f + ROOF + 1, z, b(Blocks.LIGHT_GRAY_CONCRETE));
		set(x, f + ROOF, z, b(Blocks.SMOOTH_STONE));
		if (r < ROOF_IN + 1) set(x, f + ROOF, z, b(Blocks.SEA_LANTERN));
		// Strips that zig-zag outwards every 15 degrees, like lightning.
		double zig = Math.sin((r - ROOF_IN) * 0.9) * 2.2;
		for (int i = 0; i < 24; i++) {
			double arc = Math.toRadians(angleDiff(a, i * 15.0 + zig)) * r;
			if (arc < 0.6) set(x, f + ROOF, z, b(Blocks.SEA_LANTERN));
		}
	}

	/** The rock round the stadium: rounded mounds on top, solid at its outer face. */
	private void rockRim(int x, int z, double r, double outer) {
		int f = FLOOR, top = f + rockTop(x, z);
		if (outer - r < 2.5) {
			fill(x, z, f + rockBottom(x, z), top, rock(x, f, z));
		} else {
			for (int y = top - 3; y <= top; y++) set(x, y, z, rock(x, y, z));
		}
		// Orange outcrops here and there on top.
		if (hash(x, 7, z, 8) < 0.012) fill(x, z, top + 1, top + 1 + (int) (hash(x, 8, z, 9) * 3), b(Blocks.ORANGE_TERRACOTTA));
	}

	/** Layered rock: bands of terracotta with granite and red streaks. */
	private static BlockState rock(int x, int y, int z) {
		double h = hash(x, y, z, 3);
		if (h < 0.08) return b(Blocks.GRANITE);
		if (h < 0.14) return b(Blocks.RED_TERRACOTTA);
		return Math.floorMod(y, 3) == 0 ? b(Blocks.BROWN_TERRACOTTA) : b(Blocks.TERRACOTTA);
	}

	/** One of the rocks floating round the asteroid, a lumpy blob. */
	private void floatingRock(int i) {
		double ang = hash(i, 0, 0, 40) * Math.PI * 2, dist = 135 + hash(i, 1, 0, 41) * 70;
		int ox = (int) (Math.cos(ang) * dist), oz = (int) (Math.sin(ang) * dist), oy = FLOOR - 30 + (int) (hash(i, 2, 0, 42) * 70);
		double rad = 3 + hash(i, 3, 0, 43) * 8, stretch = 1.3 + hash(i, 4, 0, 44) * 1.6;
		int rr = (int) Math.ceil(rad * stretch);
		for (int x = -rr; x <= rr; x++) {
			for (int y = -rr; y <= rr; y++) {
				for (int z = -rr; z <= rr; z++) {
					double d = Math.sqrt(x * x + (y / stretch) * (y / stretch) + z * z);
					double lump = rad * (0.75 + 0.5 * noise(x + i * 50, z + y * 3, 3, 45));
					if (d < lump) set(ox + x, oy + y, oz + z, rock(ox + x, oy + y, oz + z));
				}
			}
		}
	}
}
