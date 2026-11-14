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
 * Builds an arena a slice at a time (as many columns as fit in a few milliseconds a tick), so the server
 * never stalls: the fighting floor first, then the stands, the roof, the asteroid round it and the
 * floating rocks. Everything is worked out from ArenaLayout, so building again repairs the arena exactly;
 * an arena of an older design is cleared as it's rebuilt.
 */
public final class ArenaBuilder {
	/** Bump when the arena's design changes, so existing arenas are rebuilt. */
	public static final int VERSION = 2;
	/** The marker under the centre that says which version an arena is. */
	private static final BlockState MARKER = Blocks.REINFORCED_DEEPSLATE.defaultBlockState();

	/** Time a tick may spend building, in nanoseconds. */
	private static final long BUDGET = 25_000_000L;

	private final ServerLevel level;
	private final int cx;
	private final boolean wipe;
	private final List<int[]> columns = new ArrayList<>();
	private int next;
	private final int rocks = 16;
	private int rockNext;

	public ArenaBuilder(ServerLevel level, int centreX) {
		this.level = level;
		this.cx = centreX;
		// An older arena here: clear each column top to bottom before building it again.
		boolean old = false;
		for (int v = 1; v < VERSION; v++) old |= level.getBlockState(new BlockPos(centreX, FLOOR - 3 - v, 0)).is(MARKER.getBlock());
		this.wipe = old;
		// Columns from the middle out, so the floor is there first.
		for (int x = -SIZE; x <= SIZE; x++) for (int z = -SIZE; z <= SIZE; z++) if (radius(x, z) <= SIZE) columns.add(new int[] {x, z});
		columns.sort((a, b) -> Double.compare(radius(a[0], a[1]), radius(b[0], b[1])));
	}

	/** Whether the arena at this centre is built (and up to date). */
	public static boolean built(ServerLevel level, int centreX) {
		return level.getBlockState(new BlockPos(centreX, FLOOR - 3 - VERSION, 0)).is(MARKER.getBlock());
	}

	public boolean done() {
		return next >= columns.size() && rockNext >= rocks;
	}

	/** 0 to 1. */
	public float progress() {
		return (next + rockNext * 600f) / (columns.size() + rocks * 600f);
	}

	/** Builds the next slice; true when it's all done. */
	public boolean tick() {
		long start = System.nanoTime();
		while (next < columns.size() && System.nanoTime() - start < BUDGET) {
			for (int i = 0; i < 40 && next < columns.size(); i++, next++) column(columns.get(next)[0], columns.get(next)[1]);
		}
		if (next >= columns.size() && rockNext < rocks) floatingRock(rockNext++);
		if (done()) {
			for (int v = 1; v < VERSION; v++) set(0, FLOOR - 3 - v, 0, b(Blocks.DEEPSLATE));
			set(0, FLOOR - 3 - VERSION, 0, MARKER);
		}
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

	/** Whether an angle is within d degrees of a multiple of step. */
	private static boolean every(double a, double step, double d) {
		return angleDiff(a, Math.round(a / step) * step) < d;
	}

	private void column(int x, int z) {
		double r = radius(x + 0.5, z + 0.5), a = angle(x + 0.5, z + 0.5);
		int f = FLOOR;
		if (wipe) clear(x, z, f - 45, f + 62);
		// Clear the space above the floor up to the roof (a rebuild repairs anything broken).
		else if (r < ROOF_OUT) clear(x, z, f + 1, f + ROOF + 1);
		// The underside of the asteroid, three blocks thick, under everything.
		double outer = rockOuter(a);
		if (r < outer) {
			int bottom = f + rockBottom(x, z);
			fill(x, z, bottom, bottom + 2, rock(x, bottom, z));
		}
		if (r < WALL_OUT) fill(x, z, f - 2, f - 1, b(Blocks.DEEPSLATE));
		int tier = tierAt(r);
		if (r < CENTRE) centre(x, z, r);
		else if (r < WALL_IN) floor(x, z, r);
		else if (r < WALL_OUT) wall(x, z, a);
		else if (tier >= 0) stands(x, z, r, a, tier);
		else if (r < BACK_WALL) band(x, z, r, a);
		else if (r < ROOF_OUT) backWall(x, z, a);
		if (r >= ROOF_IN && r < ROOF_OUT) roof(x, z, r, a);
		if (r >= ROOF_OUT && r < outer) rockRim(x, z, r, outer);
		tunnels(x, z, r, a);
	}

	/**
	 * The middle: a pale disc with a cross and a ring drawn in it and a white rim, a band of light round
	 * the dais, and the dais itself, stepped up three times to a hexagon of light.
	 */
	private void centre(int x, int z, double r) {
		int f = FLOOR;
		BlockState floor;
		if (r >= LIGHT_RING_IN && r < LIGHT_RING_OUT) floor = b(Blocks.SEA_LANTERN);
		else if (r >= CENTRE - 0.8) floor = b(Blocks.WHITE_CONCRETE);
		else if (r >= 14.4 && r < 15.0) floor = b(Blocks.SMOOTH_STONE);
		else if (r >= LIGHT_RING_OUT && (x == 0 || z == 0 || x == -1 || z == -1)) floor = b(Blocks.SMOOTH_STONE);
		else floor = hash(x, 0, z, 1) < 0.07 ? b(Blocks.SMOOTH_STONE) : b(Blocks.LIGHT_GRAY_CONCRETE);
		set(x, f, z, floor);
		// The dais: three steps, the top one a hexagon edged with light, lit at its heart.
		double hex = hexRadius(x + 0.5, z + 0.5, 9.0);
		if (r < DAIS) set(x, f + 1, z, r >= DAIS - 0.9 ? b(Blocks.POLISHED_ANDESITE) : b(Blocks.STONE_BRICKS));
		if (r < 6.9) set(x, f + 2, z, r >= 6.0 ? b(Blocks.POLISHED_ANDESITE) : b(Blocks.STONE_BRICKS));
		if (hex < 1.0) {
			BlockState top = hex >= 0.76 ? b(Blocks.SEA_LANTERN) : r < 1.3 ? b(Blocks.SEA_LANTERN) : b(Blocks.POLISHED_DIORITE);
			set(x, f + 3, z, top);
		}
	}

	/** The fighting floor: dark hexagons with thin glowing lines, a pale rim, and the two pedestals. */
	private void floor(int x, int z, double r) {
		int f = FLOOR;
		BlockState s;
		if (r >= RIM + 0.8) s = b(Blocks.LIGHT_GRAY_CONCRETE);
		else if (r >= RIM) s = b(Blocks.SMOOTH_STONE);
		else s = hexLine(x + 0.5, z + 0.5, HEX) < 0.5 ? b(Blocks.SEA_LANTERN) : b(Blocks.GRAY_CONCRETE);
		set(x, f, z, s);
		// The pedestals: a black 7x7 in the floor, a 5x5 on it and the 3x3 you stand on, lit in the middle.
		for (int side : new int[] {-1, 1}) {
			int dx = Math.abs(x - side * PEDESTAL_X), dz = Math.abs(z);
			if (dx <= 3 && dz <= 3) set(x, f, z, b(Blocks.BLACK_CONCRETE));
			if (dx <= 2 && dz <= 2) set(x, f + 1, z, dx == 2 || dz == 2 ? b(Blocks.POLISHED_ANDESITE) : b(Blocks.SMOOTH_STONE));
			if (dx <= 1 && dz <= 1) set(x, f + 2, z, dx == 0 && dz == 0 ? b(Blocks.SEA_LANTERN) : b(Blocks.WHITE_CONCRETE));
		}
	}

	/** The low wall round the floor: pale, lit along its foot, a dark rail on top. */
	private void wall(int x, int z, double a) {
		int f = FLOOR;
		fill(x, z, f, f + 3, b(Blocks.SMOOTH_STONE));
		if (every(a, 10, 0.7)) set(x, f + 1, z, b(Blocks.SEA_LANTERN));
		set(x, f + 4, z, b(Blocks.POLISHED_DEEPSLATE));
	}

	/**
	 * A tier of stands: rows two blocks deep and one up, pale steps on stone, aisles every 30 degrees. The
	 * second and third tiers rise from the band below with a face carrying the orange stripe and lights.
	 */
	private void stands(int x, int z, double r, double a, int tier) {
		int f = FLOOR;
		if (tier == 1 && ArenaLayout.box(a) && r >= TIER_IN[1] + 2) {
			box(x, z, r, a);
			return;
		}
		if (tier > 0 && r < TIER_IN[tier] + 1) {
			int walk = TIER_BASE[tier - 1] + TIER_ROWS + 1;
			fill(x, z, f + walk - 3, f + TIER_BASE[tier] - 1, b(Blocks.LIGHT_GRAY_CONCRETE));
			set(x, f + walk + 1, z, b(Blocks.ORANGE_TERRACOTTA));
			set(x, f + walk + 2, z, b(Blocks.ORANGE_CONCRETE));
			if (every(a, 7.5, 0.5)) set(x, f + walk + 4, z, b(Blocks.SEA_LANTERN));
			set(x, f + TIER_BASE[tier], z, b(Blocks.POLISHED_DEEPSLATE));
			return;
		}
		int k = (int) ((r - TIER_IN[tier]) / 2);
		int top = f + TIER_BASE[tier] + k;
		boolean front = (r - TIER_IN[tier]) - 2 * k < 1;
		BlockState step = aisle(a, r) ? b(Blocks.SMOOTH_STONE) : front ? b(Blocks.POLISHED_ANDESITE) : b(Blocks.ANDESITE);
		set(x, top, z, step);
		fill(x, z, top - 3, top - 1, b(Blocks.STONE_BRICKS));
	}

	/** The walkway between two tiers, level with the top row below. */
	private void band(int x, int z, double r, double a) {
		int f = FLOOR;
		for (int t = 0; t + 1 < TIER_IN.length; t++) {
			if (r >= TIER_IN[t] + 2 * TIER_ROWS && r < TIER_IN[t + 1]) {
				int walk = TIER_BASE[t] + TIER_ROWS;
				fill(x, z, f + walk - 3, f + walk - 1, b(Blocks.STONE_BRICKS));
				set(x, f + walk, z, b(Blocks.LIGHT_GRAY_CONCRETE));
			}
		}
	}

	/** One of the boxes in the middle tier: an orange shell, a window, lights along the top. */
	private void box(int x, int z, double r, double a) {
		int f = FLOOR + TIER_BASE[1];
		double edge = 6 - Math.min(6, angleDiffAny(a));
		boolean side = edge < 1.0;
		double end = TIER_IN[1] + 2 * TIER_ROWS;
		if (r < TIER_IN[1] + 3.5) {
			fill(x, z, f, f + 2, b(Blocks.ORANGE_CONCRETE));
			fill(x, z, f + 3, f + 7, side ? b(Blocks.ORANGE_CONCRETE) : b(Blocks.GRAY_STAINED_GLASS));
			fill(x, z, f + 8, f + 11, b(Blocks.ORANGE_CONCRETE));
			if (!side && every(angleDiffAny(a), 2, 0.6)) set(x, f + 9, z, b(Blocks.SEA_LANTERN));
		} else {
			fill(x, z, f - 1, f + 2, b(Blocks.STONE_BRICKS));
			set(x, f + 2, z, b(Blocks.GRAY_CONCRETE));
			set(x, f + 11, z, b(Blocks.ORANGE_CONCRETE));
			if (side || r > end - 2) fill(x, z, f + 3, f + 10, b(Blocks.ORANGE_CONCRETE));
		}
	}

	private static double angleDiffAny(double a) {
		double best = 360;
		for (double b : BOXES) best = Math.min(best, angleDiff(a, b));
		return best;
	}

	private void backWall(int x, int z, double a) {
		int f = FLOOR;
		fill(x, z, f + TIER_BASE[2] + TIER_ROWS - 4, f + ROOF, b(Blocks.LIGHT_GRAY_CONCRETE));
		set(x, f + ROOF - 3, z, b(Blocks.ORANGE_CONCRETE));
		set(x, f + ROOF - 4, z, b(Blocks.ORANGE_TERRACOTTA));
		if (every(a, 7.5, 0.4)) set(x, f + ROOF - 1, z, b(Blocks.SEA_LANTERN));
	}

	/** The roof ring: a slab overhanging the top tier, with zig-zag light strips under it and a lit edge. */
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

	/** Tunnels at floor level through the wall and the first rows, framed in orange. */
	private void tunnels(int x, int z, double r, double a) {
		int f = FLOOR;
		if (r < WALL_IN - 0.5 || r >= 56.5) return;
		for (double t : TUNNELS) {
			double arc = Math.toRadians(angleDiff(a, t)) * r;
			if (arc < 2.6) {
				clear(x, z, f + 1, f + 4);
				set(x, f, z, b(Blocks.GRAY_CONCRETE));
				if (r >= 55.5) fill(x, z, f + 1, f + 4, b(Blocks.BLACK_CONCRETE));
				else set(x, f + 5, z, b(Blocks.ORANGE_CONCRETE));
			} else if (arc < 3.6 && r < WALL_OUT + 0.5) {
				fill(x, z, f + 1, f + 5, b(Blocks.ORANGE_CONCRETE));
			}
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
		double ang = hash(i, 0, 0, 40) * Math.PI * 2, dist = 160 + hash(i, 1, 0, 41) * 80;
		int ox = (int) (Math.cos(ang) * dist), oz = (int) (Math.sin(ang) * dist), oy = FLOOR - 30 + (int) (hash(i, 2, 0, 42) * 80);
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
