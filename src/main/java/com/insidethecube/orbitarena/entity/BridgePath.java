package com.insidethecube.orbitarena.entity;

import static com.insidethecube.orbitarena.arena.BridgeLayout.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
import java.util.function.Predicate;

import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Plans a bot's way across The Bridge the way a player would think it through: walk where it can, drop down
 * where it's safe (there's no fall damage), and where something's in the way, choose between breaking
 * through it (placed blocks only), bridging out round it over the void, building a step and climbing over
 * it, or pillaring straight up, whichever costs this bot least. Each bot has its habits (some would rather
 * dig, some build round, some climb), so a team doesn't all do the same.
 * <p>
 * It searches outward from where the bot stands (A*, steps of one block, up to a set number of places
 * looked at) to wherever it's going, and if it can't get all the way, as close as it can. The plan is a
 * list of steps, each a place to stand and how to get there.
 */
final class BridgePath {
	/** How a step is made. */
	enum Act {
		/** Walk across (on level ground). */
		WALK,
		/** Jump up a block (onto a block or a stair in the way). */
		UP,
		/** Walk off and drop to the floor below. */
		DROP,
		/** Place a block to walk on, over a gap, then walk out onto it. */
		BRIDGE,
		/** Place a block in front as a step, then climb onto it. */
		STAIR,
		/** Jump and place a block under itself (straight up). */
		PILLAR,
		/** Break the placed blocks in the way, then walk through. */
		MINE
	}

	record Step(BlockPos at, Act act) {}

	/** A bot's habits: what each way of getting past something costs it (higher: less keen), and what it can do. */
	record Habits(double place, double mine, double climb, boolean stairs, boolean pillar) {}

	private static final byte UNKNOWN = -1, PASS = 0, SOFT = 1, HARD = 2, STEP = 3;
	/** How much it leans towards where it's going over the cheapest way (a little: much quicker to think, nearly as good). */
	private static final double GREED = 1.6;
	private static final Direction[] AROUND = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};

	private final ServerLevel level;
	private final BridgeWorld w;
	private final Long2ByteOpenHashMap kinds = new Long2ByteOpenHashMap();

	BridgePath(BridgeWorld w) {
		this.w = w;
		this.level = w.level();
		kinds.defaultReturnValue(UNKNOWN);
	}

	/** What a block is to someone getting about: open, placed (breakable), map (not), or a stair to walk up. */
	private byte kind(BlockPos p) {
		long k = p.asLong();
		byte b = kinds.get(k);
		if (b != UNKNOWN) return b;
		BlockState s = level.getBlockState(p);
		if (s.getCollisionShape(level, p).isEmpty()) b = PASS;
		else if (s.is(BlockTags.STAIRS)) b = STEP;
		else b = w.breakable(p) ? SOFT : HARD;
		kinds.put(k, b);
		return b;
	}

	private boolean open(BlockPos p) {
		return kind(p) == PASS;
	}

	private boolean solid(BlockPos p) {
		return kind(p) != PASS;
	}

	private boolean inBounds(BlockPos p) {
		int dx = p.getX() - w.centreX(), dy = p.getY() - w.y(), dz = p.getZ() - w.centreZ();
		return Math.abs(dx) <= AREA_X - 2 && Math.abs(dz) <= AREA_Z - 1 && dy >= -14 && dy <= BUILD_TOP + 3;
	}

	private static final class Node implements Comparable<Node> {
		final BlockPos pos;
		final double g, f;
		final Node parent;
		final Act act;
		boolean closed;

		Node(BlockPos pos, double g, double f, Node parent, Act act) {
			this.pos = pos;
			this.g = g;
			this.f = f;
			this.parent = parent;
			this.act = act;
		}

		@Override
		public int compareTo(Node o) {
			return Double.compare(f, o.f);
		}
	}

	/**
	 * The way from start (where its feet are) to anywhere done says is there (aim: roughly where that is, for
	 * the search), never through anywhere forbidden. Empty if it can't even get nearer.
	 */
	List<Step> find(BlockPos start, Vec3 aim, Predicate<BlockPos> done, Predicate<BlockPos> forbidden, Habits h, int maxNodes) {
		PriorityQueue<Node> openSet = new PriorityQueue<>();
		Long2ObjectOpenHashMap<Node> best = new Long2ObjectOpenHashMap<>();
		Node first = new Node(start, 0, GREED * dist(start, aim), null, Act.WALK);
		openSet.add(first);
		best.put(start.asLong(), first);
		Node nearest = first;
		double nearestH = dist(start, aim);
		int looked = 0;
		List<Object[]> moves = new ArrayList<>();
		while (!openSet.isEmpty() && looked < maxNodes) {
			Node n = openSet.poll();
			if (n.closed) continue;
			n.closed = true;
			looked++;
			if (done.test(n.pos)) return steps(n);
			double hn = dist(n.pos, aim);
			if (hn < nearestH - 0.01) {
				nearestH = hn;
				nearest = n;
			}
			moves.clear();
			from(n.pos, h, moves);
			for (Object[] mv : moves) {
				BlockPos m = (BlockPos) mv[0];
				if (!inBounds(m) || forbidden.test(m)) continue;
				double g = n.g + (Double) mv[2];
				Node was = best.get(m.asLong());
				if (was != null && (was.closed || was.g <= g + 1e-6)) continue;
				Node next = new Node(m, g, g + GREED * dist(m, aim), n, (Act) mv[1]);
				best.put(m.asLong(), next);
				openSet.add(next);
			}
		}
		return nearest == first ? List.of() : steps(nearest);
	}

	private static double dist(BlockPos p, Vec3 aim) {
		double dx = p.getX() + 0.5 - aim.x, dy = p.getY() - aim.y, dz = p.getZ() + 0.5 - aim.z;
		return Math.sqrt(dx * dx + dy * dy * 1.5 + dz * dz);
	}

	private static List<Step> steps(Node n) {
		List<Step> out = new ArrayList<>();
		for (Node k = n; k.parent != null; k = k.parent) out.add(new Step(k.pos, k.act));
		Collections.reverse(out);
		return out;
	}

	/** Every way on from standing at p (feet), with what each costs. */
	private void from(BlockPos p, Habits h, List<Object[]> out) {
		BlockPos over = p.above(2);
		boolean headroom = open(over);
		for (Direction d : AROUND) {
			BlockPos m = p.relative(d), mHead = m.above(), mFloor = m.below();
			byte kf = kind(m), kh = kind(mHead);
			if (kf == PASS && kh == PASS) {
				if (solid(mFloor)) {
					out.add(new Object[] {m, Act.WALK, 1.0});
				} else {
					// Off an edge: down to a floor not far below (no fall damage here), and/or bridge across.
					for (int k = 2; k <= 7; k++) {
						BlockPos fl = m.below(k);
						if (solid(fl)) {
							out.add(new Object[] {m.below(k - 1), Act.DROP, 1.0 + 0.25 * k});
							break;
						}
					}
					if (w.buildable(mFloor)) out.add(new Object[] {m, Act.BRIDGE, h.place()});
				}
				// A step built in front, to climb onto (on firm ground, with room over it).
				if (h.stairs() && headroom && solid(mFloor) && w.buildable(m) && open(m.above(2))) out.add(new Object[] {m.above(), Act.STAIR, h.place() + h.climb()});
			} else {
				// Up onto whatever's in front (a block, or a stair), with room to stand there and to jump.
				if (kf != PASS && kh == PASS && open(m.above(2)) && headroom) out.add(new Object[] {mHead, Act.UP, kf == STEP ? 1.2 : 1.0 + h.climb()});
				// Through it: break what's in the way, if it's only placed blocks.
				boolean soft = kf == SOFT || kh == SOFT, hard = kf == HARD || kf == STEP || kh == HARD || kh == STEP;
				if (soft && !hard && solid(mFloor)) out.add(new Object[] {m, Act.MINE, 1.0 + h.mine() * ((kf == SOFT ? 1 : 0) + (kh == SOFT ? 1 : 0))});
			}
		}
		// Straight up: jump and place a block underneath.
		if (h.pillar() && headroom && w.buildable(p)) out.add(new Object[] {p.above(), Act.PILLAR, h.place() + h.climb() + 0.5});
	}

	/** Whether a step can still be made as planned (nothing new in the way it can't deal with). */
	boolean stillGood(Step s) {
		kinds.clear();
		BlockPos at = s.at();
		return switch (s.act()) {
			case WALK, DROP -> open(at) && open(at.above());
			case UP -> open(at) && open(at.above()) && solid(at.below());
			case BRIDGE -> open(at) && open(at.above()) && (solid(at.below()) || w.buildable(at.below()));
			case STAIR, PILLAR -> open(at) && open(at.above()) && (solid(at.below()) || w.buildable(at.below()));
			case MINE -> kind(at) != HARD && kind(at.above()) != HARD && kind(at) != STEP;
		};
	}
}
