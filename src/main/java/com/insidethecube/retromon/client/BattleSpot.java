package com.insidethecube.retromon.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.Vec3;

/**
 * (0.64, John's 07:10) A battle fought on the spot, in John's own build: where it's fought, which way, where the
 * camera stands, and what's cleared out of the way (on your screen only, put back after). The Interior Viewer's
 * previews are laid out by the same rules, block for block (tools/battlespot.py).
 *
 * <ul>
 * <li>The battle's laid out as the stages were: you behind yours, theirs across the field with their trainer behind,
 * the camera behind yours and a little above, looking across. It's laid along one of eight ways: the way to the
 * trainer battling you (or the way you face, for a wild one), unless another's clearly more open.</li>
 * <li>Its field and the camera's view are a corridor of the world: 10 blocks across, from behind their trainer to
 * behind the camera.</li>
 * <li>Never cut: anything solid (the ground, rock, buildings, tree trunks, a cave's walls). Where something solid would
 * be in the field or the camera's way, the battle moves to the nearest open spot (up to 6 blocks outside, 10 in a cave
 * or a forest, twice that if nowhere's open that near) or the camera comes in closer.</li>
 * <li>Cleared from the corridor: grass, flowers, bushes and the like; leaves; fences, walls, signs, lamp posts and the
 * like.</li>
 * </ul>
 */
public final class BattleSpot {
	private BattleSpot() {}

	public static final int AIR = 0, FLUID = 1, FLAT = 2, PLANT = 3, LEAVES = 4, DECO = 5, SOLID = 6;

	private static final double D = Math.sqrt(0.5);
	/** The way from you to them: north, north-east, east ... north-west. */
	public static final double[][] DIRS = {{0, -1}, {D, -D}, {1, 0}, {D, D}, {0, 1}, {-D, D}, {-1, 0}, {-D, -D}};

	private static final Set<String> AIRS = Set.of("air", "cave_air", "void_air", "light", "structure_void", "barrier");
	private static final Set<String> FLUIDS = Set.of("water", "lava", "bubble_column");
	private static final String[] FLAT_END = {"_carpet", "_pressure_plate"};
	private static final Set<String> FLATS = Set.of("snow", "rail", "powered_rail", "detector_rail", "activator_rail", "tripwire", "redstone_wire", "lily_pad",
			"leaf_litter", "pink_petals", "wildflowers", "moss_carpet", "pale_moss_carpet");
	private static final Set<String> PLANTS = Set.of("short_grass", "grass", "tall_grass", "fern", "large_fern", "dead_bush", "bush", "firefly_bush",
			"short_dry_grass", "tall_dry_grass", "sweet_berry_bush", "sugar_cane", "brown_mushroom", "red_mushroom", "vine", "glow_lichen", "hanging_roots",
			"dandelion", "poppy", "blue_orchid", "allium", "azure_bluet", "red_tulip", "orange_tulip", "white_tulip", "pink_tulip", "oxeye_daisy",
			"cornflower", "lily_of_the_valley", "wither_rose", "sunflower", "lilac", "rose_bush", "peony", "torchflower", "pitcher_plant", "open_eyeblossom",
			"closed_eyeblossom", "cactus_flower", "seagrass", "tall_seagrass", "kelp", "kelp_plant", "cobweb", "nether_sprouts", "crimson_roots",
			"warped_roots", "crimson_fungus", "warped_fungus", "spore_blossom", "small_dripleaf", "cave_vines", "cave_vines_plant", "twisting_vines",
			"twisting_vines_plant", "weeping_vines", "weeping_vines_plant", "pale_hanging_moss", "big_dripleaf_stem");
	private static final String[] DECO_END = {"_fence", "_fence_gate", "_wall", "_pane", "_sign", "_wall_sign", "_hanging_sign", "_wall_hanging_sign", "_banner",
			"_wall_banner", "_button", "_candle", "_head", "_wall_head", "_skull", "_wall_skull", "_lantern", "_torch", "_wall_torch", "_shelf", "_chain"};
	private static final Set<String> DECOS = Set.of("lantern", "soul_lantern", "torch", "wall_torch", "soul_torch", "soul_wall_torch", "redstone_torch",
			"redstone_wall_torch", "chain", "iron_chain", "iron_bars", "flower_pot", "end_rod", "lightning_rod", "lever", "scaffolding", "bell", "candle",
			"glass_pane", "copper_bars", "tripwire_hook", "decorated_pot");

	private static boolean ends(String b, String[] ends) {
		for (String e : ends) if (b.endsWith(e)) return true;
		return false;
	}

	/** What a block is, for the battle: air, water, flat on the ground, a plant, leaves, a fence (and the like), solid. */
	public static int kind(BlockState s) {
		String b = BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath();
		if (AIRS.contains(b)) return AIR;
		if (FLUIDS.contains(b)) return FLUID;
		if (FLATS.contains(b) || ends(b, FLAT_END)) return FLAT;
		if (PLANTS.contains(b) || b.endsWith("_sapling") && !b.startsWith("potted_")) return PLANT;
		if (b.endsWith("_leaves")) return LEAVES;
		if (DECOS.contains(b) || b.startsWith("potted_") || ends(b, DECO_END)) return DECO;
		return SOLID;
	}

	/** How high a solid block's top is in its block (a bottom slab's at a half, a snow layer's by its layers). */
	static double topOf(BlockState s) {
		if (s.getBlock() instanceof SlabBlock && s.hasProperty(SlabBlock.TYPE) && s.getValue(SlabBlock.TYPE) == SlabType.BOTTOM) return 0.5;
		if (s.getBlock() instanceof SnowLayerBlock && s.hasProperty(SnowLayerBlock.LAYERS)) return s.getValue(SnowLayerBlock.LAYERS) / 8.0;
		return 1.0;
	}

	static final double[] CAM_BACKS = {0.0, -1.4, -2.8, -4.0};
	static final double ACROSS0 = -4.5, ACROSS1 = 5.5;
	static final int UP = 6, COST_UP = 4;
	static final double[][] FOE_OFFSETS = {{2.1, -1.8}, {1.2, -1.6}, {0.3, -1.6}, {2.8, -0.6}, {-0.8, -1.6}};

	/** The layout along the battle (right, back towards the camera) from the field's middle, as the stages had it. */
	static final class Layout {
		final double far, mineR, mineB, enemyR, enemyB, heroR, heroB, foeR, foeB, foeH, camUp, camRight, camBack;

		Layout(double h) {
			far = 6.0 + Math.max(0.0, h - 1.5) * 1.3;
			mineR = -0.6;
			mineB = 2.2;
			enemyR = 1.8;
			enemyB = 2.2 - far;
			heroR = mineR - 2.0;
			heroB = mineB + 1.5;
			foeH = Math.max(0.0, h - 1.5) * 0.4;
			foeR = enemyR + 2.1;
			foeB = enemyB - 1.8 - foeH;
			camUp = 2.2 + Math.max(0.0, h - 2) * 0.4;
			camRight = -1.4;
			camBack = 9.8 + Math.max(0.0, h - 2) * 0.9;
		}
	}

	/** A battle as it's laid out. Spots are where each stands (on the ground's top); wet: on water. */
	public record Result(int dir, double fx, double fz, double rx, double rz, Vec3 centre, Vec3 hero, Vec3 mine, Vec3 enemy, Vec3 foe, boolean mineWet,
			boolean enemyWet, boolean heroWet, boolean foeWet, Vec3 cam, Vec3 look, List<BlockPos> clears, double cost, double camBack) {}

	/** The blocks, read once each. */
	private static final class World {
		final Level level;
		final Map<Long, Integer> kinds = new HashMap<>();
		final Map<Long, BlockState> states = new HashMap<>();
		final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();

		World(Level level) {
			this.level = level;
		}

		int k(int x, int y, int z) {
			long key = BlockPos.asLong(x, y, z);
			Integer v = kinds.get(key);
			if (v == null) {
				BlockState s = level.getBlockState(at.set(x, y, z));
				states.put(key, s);
				v = kind(s);
				kinds.put(key, v);
			}
			return v;
		}

		/** The ground's top at a column near a height (NaN if there's none close); wet[0]: whether it's water. */
		double surface(int x, int z, double yref, boolean[] wet) {
			int top = (int) Math.floor(yref) + 2, bottom = (int) Math.floor(yref) - 4;
			if (wet != null) wet[0] = false;
			for (int y = top; y >= bottom; y--) {
				int k = k(x, y, z);
				if (k == SOLID) return y + topOf(states.get(BlockPos.asLong(x, y, z)));
				if (k == FLUID) {
					if (wet != null) wet[0] = true;
					return y + 0.8;
				}
			}
			return Double.NaN;
		}

		boolean room(int x, int z, double top) {
			int y = (int) Math.floor(top + 0.01);
			return k(x, y, z) != SOLID && k(x, y + 1, z) != SOLID;
		}

		/** The ground's top where someone stands (near the field's height), or NaN if they can't. */
		double stand(double wx, double wz, double ay, boolean[] wet) {
			int x = (int) Math.floor(wx), z = (int) Math.floor(wz);
			double sy = surface(x, z, ay, wet);
			if (Double.isNaN(sy) || sy < ay - 1.6 || sy > ay + 1.1 || !room(x, z, sy)) return Double.NaN;
			return sy;
		}
	}

	/** A way along the battle: the forward and right vectors and the field's middle (you stand at hero). */
	private record Frame(double fx, double fz, double rx, double rz, double cx, double cz) {
		static Frame of(Layout L, int d, int ax, int az) {
			double fx = DIRS[d][0], fz = DIRS[d][1], rx = -fz, rz = fx;
			return new Frame(fx, fz, rx, rz, ax + 0.5 - rx * L.heroR + fx * L.heroB, az + 0.5 - rz * L.heroR + fz * L.heroB);
		}

		double x(double right, double back) {
			return cx + rx * right - fx * back;
		}

		double z(double right, double back) {
			return cz + rz * right - fz * back;
		}
	}

	static double turnCost(int d, int prefer) {
		double diag = d % 2 != 0 ? 1.5 : 0.0;
		if (prefer < 0) return diag;
		int k = Math.min(Math.floorMod(d - prefer, 8), Math.floorMod(prefer - d, 8));
		return diag + new double[] {0.0, 2.5, 4.0, 6.0, 8.0}[k];
	}

	/** Their trainer's spot: behind theirs, the first of FOE_OFFSETS they can stand on (x, y, z, wet 0/1), or null. */
	private static double[] foeSpot(World W, Layout L, Frame F, double ay) {
		boolean[] wet = new boolean[1];
		for (double[] o : FOE_OFFSETS) {
			double r = L.enemyR + o[0], b = L.enemyB + o[1] - L.foeH;
			double wx = F.x(r, b), wz = F.z(r, b);
			double sy = W.stand(wx, wz, ay, wet);
			if (!Double.isNaN(sy)) return new double[] {wx, sy, wz, wet[0] ? 1 : 0};
		}
		return null;
	}

	/** The blocks whose middles are in the corridor (across, along back b0 to b1): {x, z, right, back}. */
	private static List<double[]> cells(Frame F, double b0, double b1) {
		List<double[]> out = new ArrayList<>();
		double[] xs = {F.x(ACROSS0, b0), F.x(ACROSS0, b1), F.x(ACROSS1, b0), F.x(ACROSS1, b1)};
		double[] zs = {F.z(ACROSS0, b0), F.z(ACROSS0, b1), F.z(ACROSS1, b0), F.z(ACROSS1, b1)};
		double minX = Math.min(Math.min(xs[0], xs[1]), Math.min(xs[2], xs[3])), maxX = Math.max(Math.max(xs[0], xs[1]), Math.max(xs[2], xs[3]));
		double minZ = Math.min(Math.min(zs[0], zs[1]), Math.min(zs[2], zs[3])), maxZ = Math.max(Math.max(zs[0], zs[1]), Math.max(zs[2], zs[3]));
		for (int z = (int) Math.floor(minZ) - 1; z < (int) Math.floor(maxZ) + 2; z++) {
			for (int x = (int) Math.floor(minX) - 1; x < (int) Math.floor(maxX) + 2; x++) {
				double ox = x + 0.5 - F.cx, oz = z + 0.5 - F.cz;
				double right = ox * F.rx + oz * F.rz, back = -(ox * F.fx + oz * F.fz);
				if (ACROSS0 <= right && right <= ACROSS1 && b0 <= back && back <= b1) out.add(new double[] {x, z, right, back});
			}
		}
		return out;
	}

	/** How many solid blocks a line passes through (looked at every quarter block). */
	private static int blocked(World W, double ax, double ay, double az, double bx, double by, double bz) {
		double len = Math.sqrt((bx - ax) * (bx - ax) + (by - ay) * (by - ay) + (bz - az) * (bz - az));
		int n = Math.max(1, (int) Math.ceil(len / 0.25));
		java.util.HashSet<Long> seen = new java.util.HashSet<>();
		for (int i = 1; i < n; i++) {
			double t = i / (double) n;
			int x = (int) Math.floor(ax + (bx - ax) * t), y = (int) Math.floor(ay + (by - ay) * t), z = (int) Math.floor(az + (bz - az) * t);
			long key = BlockPos.asLong(x, y, z);
			if (!seen.contains(key) && W.k(x, y, z) == SOLID) seen.add(key);
		}
		return seen.size();
	}

	private record Best(double cost, int d, int ax, int az, double ay, double camBack, boolean bad) {}

	private static Best search(World W, Layout L, double px, double py, double pz, int prefer, int reach, boolean water, boolean trainer) {
		int bx = (int) Math.floor(px), bz = (int) Math.floor(pz);
		boolean[] wet = new boolean[1];
		double y0 = W.surface(bx, bz, py, null);
		if (Double.isNaN(y0)) y0 = py;
		Best best = null;
		for (int dz = -reach; dz <= reach; dz++) {
			for (int dx = -reach; dx <= reach; dx++) {
				double dist = Math.hypot(dx, dz);
				if (dist > reach + 0.01) continue;
				int ax = bx + dx, az = bz + dz;
				double ay = W.surface(ax, az, y0, wet);
				if (Double.isNaN(ay) || Math.abs(ay - y0) > 1.5 || !W.room(ax, az, ay)) continue;
				if (wet[0] && !water) continue;
				for (int d = 0; d < 8; d++) {
					Frame F = Frame.of(L, d, ax, az);
					double pen = turnCost(d, prefer);
					double spotCost = 0;
					boolean bad = false;
					if (Double.isNaN(W.stand(F.x(L.mineR, L.mineB), F.z(L.mineR, L.mineB), ay, null))) {
						spotCost += 60;
						bad = true;
					}
					if (Double.isNaN(W.stand(F.x(L.enemyR, L.enemyB), F.z(L.enemyR, L.enemyB), ay, null))) {
						spotCost += 60;
						bad = true;
					}
					if (trainer) {
						double[] f = foeSpot(W, L, F, ay);
						if (f == null) {
							spotCost += 60;
							bad = true;
						} else if (f[3] > 0 && !water) spotCost += 6;
					}
					int yi = (int) Math.floor(ay - 0.01);
					// The field itself on the ground at its height (a bridge's deck, not the water either side of it).
					double ground = 0;
					for (double[] c : cells(F, L.enemyB - 1.0, L.mineB + 1.0)) {
						if (!(-2.5 <= c[2] && c[2] <= 3.5)) continue;
						double gy = W.surface((int) c[0], (int) c[1], ay, wet);
						if (Double.isNaN(gy) || Math.abs(gy - ay) > 1.6) ground += 1.5;
						else if (wet[0] && !water) ground += 0.8;
					}
					for (double cb : CAM_BACKS) {
						double camBack = L.camBack + cb;
						double cost = pen + dist * 1.2 + spotCost + ground + (-cb) * 3.0;
						if (best != null && cost >= best.cost()) continue;
						double camx = F.x(L.camRight, camBack), camz = F.z(L.camRight, camBack), camy = ay + L.camUp;
						if (W.k((int) Math.floor(camx), (int) Math.floor(camy), (int) Math.floor(camz)) == SOLID
								|| W.k((int) Math.floor(camx), (int) Math.floor(camy) + 1, (int) Math.floor(camz)) == SOLID) cost += 200;
						int block = blocked(W, camx, camy, camz, F.x(L.mineR, L.mineB), ay + 0.9, F.z(L.mineR, L.mineB))
								+ blocked(W, camx, camy, camz, F.x(L.enemyR, L.enemyB), ay + 0.9, F.z(L.enemyR, L.enemyB))
								+ (trainer ? blocked(W, camx, camy, camz, F.x(L.foeR, L.foeB), ay + 0.9, F.z(L.foeR, L.foeB)) : 0);
						cost += 40 * block;
						int coreSolid = 0;
						for (double[] c : cells(F, L.foeB - 2.0, camBack + 1.2)) {
							boolean core = -2.5 <= c[2] && c[2] <= 3.5 && L.enemyB - 1.0 <= c[3] && c[3] <= L.mineB + 1.0;
							for (int dy = 1; dy <= COST_UP; dy++) {
								int k = W.k((int) c[0], yi + dy, (int) c[1]);
								if (k == SOLID) {
									if (core && dy <= 2) {
										cost += 10;
										coreSolid++;
									} else cost += 2;
								} else if (k == DECO) cost += core ? 1.5 : 1.0;
								else if (k == LEAVES) cost += core ? 0.4 : 0.3;
							}
							if (best != null && cost >= best.cost()) break;
						}
						if (best == null || cost < best.cost()) best = new Best(cost, d, ax, az, ay, camBack, bad || coreSolid > 0 || block > 0);
					}
				}
			}
		}
		return best;
	}

	/**
	 * The battle, laid out from where you stand. prefer: the way you'd face (0 to 7, DIRS), or -1. h: their tallest's
	 * height. reach: how far it may move. water: a battle on the water. trainer: whether there's a trainer to stand
	 * behind theirs. Null if there's nowhere at all (no ground near you).
	 */
	public static Result find(Level level, double px, double py, double pz, int prefer, double h, int reach, boolean water, boolean trainer) {
		Layout L = new Layout(h);
		World W = new World(level);
		Best b = search(W, L, px, py, pz, prefer, reach, water, trainer);
		if (b == null || b.bad() && reach < 16) {
			Best wider = search(W, L, px, py, pz, prefer, Math.min(16, reach * 2), water, trainer);
			if (wider != null && (b == null || wider.cost() < b.cost())) b = wider;
		}
		if (b == null) return null;
		Frame F = Frame.of(L, b.d(), b.ax(), b.az());
		double ay = b.ay();
		boolean[] wm = new boolean[1], we = new boolean[1];
		double my = W.surface((int) Math.floor(F.x(L.mineR, L.mineB)), (int) Math.floor(F.z(L.mineR, L.mineB)), ay, wm);
		double ey = W.surface((int) Math.floor(F.x(L.enemyR, L.enemyB)), (int) Math.floor(F.z(L.enemyR, L.enemyB)), ay, we);
		Vec3 mine = new Vec3(F.x(L.mineR, L.mineB), Double.isNaN(my) ? ay : my, F.z(L.mineR, L.mineB));
		Vec3 enemy = new Vec3(F.x(L.enemyR, L.enemyB), Double.isNaN(ey) ? ay : ey, F.z(L.enemyR, L.enemyB));
		Vec3 hero = new Vec3(b.ax() + 0.5, ay, b.az() + 0.5);
		Vec3 foe = null;
		boolean foeWet = false;
		if (trainer) {
			double[] f = foeSpot(W, L, F, ay);
			if (f == null) foe = new Vec3(F.x(L.foeR, L.foeB), ay, F.z(L.foeR, L.foeB));
			else {
				foe = new Vec3(f[0], f[1], f[2]);
				foeWet = f[3] > 0;
			}
		}
		Vec3 cam = new Vec3(F.x(L.camRight, b.camBack()), ay + L.camUp, F.z(L.camRight, b.camBack()));
		Vec3 look = new Vec3((mine.x + enemy.x) / 2, (mine.y + enemy.y) / 2 + 0.8 + Math.max(0.0, h - 1) * 0.15, (mine.z + enemy.z) / 2);
		// What's cleared: every plant, leaf and fence (and the like) in the corridor, from two below the ground up to UP
		// (and a tall plant's top half just over it).
		int yi = (int) Math.floor(ay - 0.01);
		List<BlockPos> clears = new ArrayList<>();
		for (double[] c : cells(F, L.foeB - 2.0, b.camBack() + 1.2)) {
			for (int dy = -2; dy < UP + 2; dy++) {
				int k = W.k((int) c[0], yi + dy, (int) c[1]);
				if ((k == PLANT || k == LEAVES || k == DECO) && (dy <= UP || k == PLANT)) clears.add(new BlockPos((int) c[0], yi + dy, (int) c[1]));
			}
		}
		return new Result(b.d(), F.fx(), F.fz(), F.rx(), F.rz(), new Vec3(F.cx(), ay, F.cz()), hero, mine, enemy, foe, wm[0], we[0], false, foeWet, cam, look,
				clears, b.cost(), b.camBack());
	}

	/** The nearest of the eight ways to a direction (x, z). */
	public static int nearest(double x, double z) {
		if (x * x + z * z < 1e-6) return 0;
		double a = Math.toDegrees(Math.atan2(x, -z));
		return Math.floorMod((int) Math.round(a / 45.0), 8);
	}
}
