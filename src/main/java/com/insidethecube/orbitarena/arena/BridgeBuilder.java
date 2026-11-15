package com.insidethecube.orbitarena.arena;

import static com.insidethecube.orbitarena.arena.BridgeLayout.*;

import com.insidethecube.orbitarena.match.Matches;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Builds a bridge map a slice at a time, so the server never stalls. In full: the playing area (bridge,
 * bases, goals, pillars), the grandstands, then the spires. Otherwise just the playing area, put right
 * exactly: everything built on it cleared, crumbled pillars stood back up, the obstacles there or not as
 * the next match wants them.
 */
public final class BridgeBuilder implements Matches.Builder {
	/** Bump when the bridge's design changes, so existing bridges are rebuilt. */
	public static final int VERSION = 1;
	private static final BlockState MARKER = Blocks.REINFORCED_DEEPSLATE.defaultBlockState();
	private static final long BUDGET = 25_000_000L;

	private final ServerLevel level;
	private final int cx, cz;
	private final boolean full;
	private boolean obstacles;
	private int column, standColumn, spire;
	private final int areaColumns, standColumns;

	public BridgeBuilder(ServerLevel level, int centreX, boolean full) {
		this.level = level;
		this.cx = centreX;
		this.cz = Z;
		this.full = full || !built(level, centreX);
		this.areaColumns = (2 * AREA_X + 1) * (2 * AREA_Z + 1);
		this.standColumns = this.full ? (2 * STAND_HALF_X + 3) * (2 * (ROWS * 2 + 4)) : 0;
		if (!this.full) this.spire = SPIRES;
	}

	/** The obstacles (copper on the bridge) in or out: set before the playing area is built. */
	public BridgeBuilder obstacles(boolean on) {
		this.obstacles = on;
		return this;
	}

	/** Whether this bridge is built (and up to date). */
	public static boolean built(ServerLevel level, int centreX) {
		return level.getBlockState(new BlockPos(centreX, Y - 20 - VERSION, Z)).is(MARKER.getBlock());
	}

	/** Whether the obstacles are there now. */
	public static boolean hasObstacles(ServerLevel level, int centreX) {
		return level.getBlockState(new BlockPos(centreX + 7, Y + 1, Z)).is(Blocks.WAXED_WEATHERED_COPPER);
	}

	@Override
	public float progress() {
		float total = areaColumns + standColumns + SPIRES * 300f;
		return (column + standColumn + spire * 300f) / total;
	}

	private void set(int dx, int dy, int dz, BlockState state) {
		BlockPos pos = new BlockPos(cx + dx, Y + dy, cz + dz);
		if (level.getBlockState(pos) != state) level.setBlock(pos, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
	}

	@Override
	public boolean tick() {
		long start = System.nanoTime();
		BlockState air = Blocks.AIR.defaultBlockState();
		while (column < areaColumns && System.nanoTime() - start < BUDGET) {
			int dx = column / (2 * AREA_Z + 1) - AREA_X, dz = column % (2 * AREA_Z + 1) - AREA_Z;
			for (int dy = AREA_BOTTOM; dy <= AREA_TOP; dy++) {
				BlockState s = mapBlock(dx, dy, dz, obstacles);
				set(dx, dy, dz, s == null ? air : s);
			}
			column++;
		}
		while (column >= areaColumns && standColumn < standColumns && System.nanoTime() - start < BUDGET) {
			int across = 2 * (ROWS * 2 + 4);
			int dx = standColumn / across - (STAND_HALF_X + 1), k = standColumn % across;
			int dz = k < across / 2 ? STAND_IN - 1 + k : -(STAND_IN - 1 + k - across / 2);
			for (int dy = -8; dy <= rowTop(ROWS - 1) + 8; dy++) {
				BlockState s = standBlock(dx, dy, dz);
				if (s != null) set(dx, dy, dz, s);
			}
			standColumn++;
		}
		if (column >= areaColumns && standColumn >= standColumns && spire < SPIRES && System.nanoTime() - start < BUDGET) spire(spire++);
		boolean done = column >= areaColumns && standColumn >= standColumns && spire >= SPIRES;
		if (done && full) {
			for (int v = 1; v < VERSION; v++) set(0, -20 - v, 0, Blocks.DEEPSLATE.defaultBlockState());
			set(0, -20 - VERSION, 0, MARKER);
		}
		return done;
	}

	/** A rock spire: a lumpy spindle of layered rock, standing on end. */
	private void spire(int i) {
		Spire s = spire(i);
		int r = (int) Math.ceil(s.radius() * 1.3);
		for (int y = 0; y < s.height(); y++) {
			double t = (y + 0.5) / s.height();
			for (int x = -r; x <= r; x++) {
				for (int z = -r; z <= r; z++) {
					double d = Math.sqrt(x * x + z * z), a = Math.atan2(z, x);
					if (d < spireRadius(s, t, a)) {
						int wx = s.x() + x, wy = s.y() + y, wz = s.z() + z;
						set(wx, wy, wz, rock(cx + wx, wy, wz));
					}
				}
			}
		}
	}
}
