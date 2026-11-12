package com.insidethecube.cubework.tool;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.material.Fluids;

import com.insidethecube.cubework.CwBlocks;
import com.insidethecube.cubework.Material;
import com.insidethecube.cubework.Shape;
import com.insidethecube.cubework.block.CwShape;
import com.insidethecube.cubework.block.GentleSlopeBlock;
import com.insidethecube.cubework.block.SlopeBlock;

/**
 * Smooths stepped ground with Cubework slopes, for the terrain brush and (if switched on) new terrain.
 *
 * The ground's height is taken column by column: the top block that Cubework makes slopes of, with
 * nothing solid on it. Where a column's neighbour is exactly one block higher, a slope of the higher
 * ground's block goes on top of it, rising towards that neighbour (fill), or the higher column's top block
 * is cut into a slope falling towards the lower one (carve). Corners join up by themselves, as slopes do.
 * Where the ground steps up every 2 to 4 blocks, the long slopes (1 in 2, 3 or 4) cover the whole terrace
 * so the hill rises evenly. Under water the slopes are waterlogged; cliffs (2 or more high) are left alone.
 */
public final class Terrain {
	private Terrain() {}

	private static final Direction[] SIDES = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
	private static final int NONE = Integer.MIN_VALUE;

	/** One change, so it can be undone. */
	public record Change(BlockPos pos, BlockState before) {}

	private static final Map<UUID, Deque<List<Change>>> HISTORY = new HashMap<>();

	/** Smooths round a block for a player (the brush); returns how many blocks changed. */
	public static int smooth(LevelAccessor level, BlockPos centre, int radius, int mode, int steep, ServerPlayer player) {
		List<Change> changes = new ArrayList<>();
		run(level, centre.getX() - radius, centre.getZ() - radius, 2 * radius + 1, 2 * radius + 1, centre.getY() - radius - 1,
				centre.getY() + radius + 1, (x, z) -> {
					int dx = x - centre.getX(), dz = z - centre.getZ();
					return dx * dx + dz * dz <= radius * radius + radius;
				}, mode, steep, changes, Block.UPDATE_ALL);
		if (!changes.isEmpty() && player != null) {
			Deque<List<Change>> h = HISTORY.computeIfAbsent(player.getUUID(), k -> new ArrayDeque<>());
			h.push(changes);
			while (h.size() > 10) h.removeLast();
		}
		return changes.size();
	}

	/** Puts back the player's last brush stroke; returns how many blocks went back. */
	public static int undo(LevelAccessor level, ServerPlayer player) {
		Deque<List<Change>> h = HISTORY.get(player.getUUID());
		if (h == null || h.isEmpty()) return -1;
		List<Change> last = h.pop();
		for (int i = last.size() - 1; i >= 0; i--) level.setBlock(last.get(i).pos(), last.get(i).before(), Block.UPDATE_ALL);
		return last.size();
	}

	/** Which columns to change. */
	public interface Area {
		boolean contains(int x, int z);
	}

	/**
	 * Smooths the columns in an area (x0, z0, w by d), looking from y0 to y1 for the ground. Neighbours up to
	 * 4 blocks outside the area are read (to see how wide terraces are) but never changed.
	 */
	public static void run(LevelAccessor level, int x0, int z0, int w, int d, int y0, int y1, Area area, int mode, int steep, List<Change> changes,
			int flags) {
		int pad = 5, W = w + 2 * pad, D = d + 2 * pad;
		int[][] h = new int[W][D];
		Material[][] top = new Material[W][D];
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
		for (int i = 0; i < W; i++) {
			for (int j = 0; j < D; j++) {
				h[i][j] = NONE;
				int x = x0 - pad + i, z = z0 - pad + j;
				for (int y = y1; y >= y0; y--) {
					p.set(x, y, z);
					BlockState s = level.getBlockState(p);
					if (s.isAir() || s.canBeReplaced() || !s.getFluidState().isEmpty() && !s.isSolid()) continue;
					Material m = ground(s);
					if (m != null && clear(level.getBlockState(p.above()))) {
						h[i][j] = y;
						top[i][j] = m;
					}
					break;
				}
			}
		}
		Map<BlockPos, BlockState> place = new LinkedHashMap<>();
		boolean[][] done = new boolean[W][D];
		for (int i = pad; i < pad + w; i++) {
			for (int j = pad; j < pad + d; j++) {
				int x = x0 - pad + i, z = z0 - pad + j;
				if (h[i][j] == NONE || !area.contains(x, z)) continue;
				if (mode == TerrainBrushItem.CARVE) carve(h, top, i, j, x, z, place);
				else fill(h, top, i, j, x, z, steep, place, done, area, x0 - pad, z0 - pad, pad, w, d);
			}
		}
		// Place everything, then let each slope turn its corners from its new neighbours.
		for (Map.Entry<BlockPos, BlockState> e : place.entrySet()) {
			BlockState before = level.getBlockState(e.getKey());
			BlockState state = e.getValue();
			if (state.hasProperty(BlockStateProperties.WATERLOGGED)) {
				state = state.setValue(BlockStateProperties.WATERLOGGED, level.getFluidState(e.getKey()).is(FluidTags.WATER)
						&& level.getFluidState(e.getKey()).getType() == Fluids.WATER);
			}
			if (level.setBlock(e.getKey(), state, flags)) changes.add(new Change(e.getKey(), before));
		}
		for (BlockPos at : place.keySet()) {
			BlockState s = level.getBlockState(at);
			BlockState turned = Block.updateFromNeighbourShapes(s, level, at);
			if (turned != s) level.setBlock(at, turned, flags);
		}
	}

	/** The material of ground Cubework can slope (a full block that has slopes), or null. */
	private static Material ground(BlockState s) {
		Block b = s.getBlock();
		if (b instanceof CwShape) return null;
		Material m = CwBlocks.materialOf(b);
		if (m == null || CwBlocks.info(b) != null || CwBlocks.get(m, Shape.SLOPE) == null) return null;
		return m;
	}

	/** Air, plants, snow and water can be built into; anything else on top means it isn't the surface. */
	private static boolean clear(BlockState s) {
		return s.isAir() || s.canBeReplaced() || (!s.getFluidState().isEmpty() && !s.isSolid());
	}

	private static int at(int[][] h, int i, int j) {
		return i < 0 || j < 0 || i >= h.length || j >= h[0].length ? NONE : h[i][j];
	}

	private static Material mat(Material[][] top, int i, int j) {
		return i < 0 || j < 0 || i >= top.length || j >= top[0].length ? null : top[i][j];
	}

	/** Cut the top edge of a higher column into a slope falling towards a lower neighbour. */
	private static void carve(int[][] h, Material[][] top, int i, int j, int x, int z, Map<BlockPos, BlockState> place) {
		int y = h[i][j];
		Direction drop = null;
		int drops = 0;
		for (Direction d : SIDES) {
			int n = at(h, i + d.getStepX(), j + d.getStepZ());
			if (n == y - 1) {
				drops++;
				if (drop == null || (drop.getClockWise() == d)) drop = d;
			}
		}
		if (drop == null) {
			// A corner going in: only a diagonal is lower.
			for (Direction d : SIDES) {
				Direction e = d.getClockWise();
				if (at(h, i + d.getStepX() + e.getStepX(), j + d.getStepZ() + e.getStepZ()) == y - 1
						&& at(h, i + d.getStepX(), j + d.getStepZ()) == y && at(h, i + e.getStepX(), j + e.getStepZ()) == y) {
					drop = d;
					break;
				}
			}
			if (drop == null) return;
		} else if (drops > 2) {
			return;
		}
		Block slope = CwBlocks.get(top[i][j], Shape.SLOPE);
		if (slope == null) return;
		place.put(new BlockPos(x, y, z), slope.defaultBlockState().setValue(SlopeBlock.FACING, drop.getOpposite()).setValue(SlopeBlock.HALF, Half.BOTTOM));
	}

	/** Add slopes on top of a lower column, rising towards a neighbour one block higher. */
	private static void fill(int[][] h, Material[][] top, int i, int j, int x, int z, int steep, Map<BlockPos, BlockState> place, boolean[][] done,
			Area area, int ox, int oz, int pad, int w, int d) {
		if (done[i][j]) return;
		int y = h[i][j];
		List<Direction> up = new ArrayList<>();
		for (Direction s : SIDES) if (at(h, i + s.getStepX(), j + s.getStepZ()) == y + 1) up.add(s);
		Direction rise = null;
		Material m = null;
		if (up.size() == 1) {
			rise = up.get(0);
		} else if (up.size() == 2 && up.get(0).getAxis() != up.get(1).getAxis()) {
			// A corner going in: the slope turns its own inner corner.
			rise = up.get(0).getClockWise() == up.get(1) ? up.get(0) : up.get(1);
		} else if (up.isEmpty()) {
			// A corner going out: only a diagonal is higher.
			for (Direction s : SIDES) {
				Direction e = s.getClockWise();
				if (at(h, i + s.getStepX() + e.getStepX(), j + s.getStepZ() + e.getStepZ()) == y + 1) {
					rise = s;
					m = mat(top, i + s.getStepX() + e.getStepX(), j + s.getStepZ() + e.getStepZ());
					break;
				}
			}
		}
		if (rise == null) return;
		if (m == null) m = mat(top, i + rise.getStepX(), j + rise.getStepZ());
		if (m == null) m = top[i][j];
		// Long slopes across a straight terrace: count the flat columns behind, away from the rise.
		int n = 1;
		if (steep != TerrainBrushItem.STEEP_ONLY && up.size() == 1) {
			Direction back = rise.getOpposite();
			int run = 1;
			while (run < 4) {
				int bi = i + back.getStepX() * run, bj = j + back.getStepZ() * run;
				if (at(h, bi, bj) != y || straightUp(h, bi, bj, y, rise)) break;
				run++;
			}
			int beyond = at(h, i + back.getStepX() * run, j + back.getStepZ() * run);
			boolean terrace = beyond == y - 1;
			n = terrace ? run : steep == TerrainBrushItem.GENTLE ? run : Math.min(run, 3);
			if (!terrace && steep == TerrainBrushItem.NATURAL && run < 4) n = 1;
			if (n < 2) n = 1;
		}
		Shape shape = switch (n) {
			case 2 -> Shape.SLOPE_2;
			case 3 -> Shape.SLOPE_3;
			case 4 -> Shape.GENTLE_SLOPE;
			default -> Shape.SLOPE;
		};
		Block slope = CwBlocks.get(m, shape);
		if (slope == null) {
			slope = CwBlocks.get(m, Shape.SLOPE);
			n = 1;
		}
		if (slope == null) return;
		Direction back = rise.getOpposite();
		for (int k = 0; k < n; k++) {
			int bi = i + back.getStepX() * k, bj = j + back.getStepZ() * k;
			if (bi < pad || bj < pad || bi >= pad + w || bj >= pad + d) break;
			int bx = ox + bi, bz = oz + bj;
			if (k > 0 && (!area.contains(bx, bz) || done[bi][bj])) break;
			BlockState s = slope.defaultBlockState().setValue(SlopeBlock.FACING, rise).setValue(SlopeBlock.HALF, Half.BOTTOM);
			if (slope instanceof GentleSlopeBlock g) s = s.setValue(g.step(), n - k);
			place.put(new BlockPos(bx, y + 1, bz), s);
			done[bi][bj] = true;
		}
	}

	/** Whether a column on a terrace has its own step up to the side (so the terrace isn't straight there). */
	private static boolean straightUp(int[][] h, int i, int j, int y, Direction rise) {
		for (Direction s : SIDES) {
			if (s.getAxis() == rise.getAxis()) continue;
			if (at(h, i + s.getStepX(), j + s.getStepZ()) == y + 1) return true;
		}
		return false;
	}
}
