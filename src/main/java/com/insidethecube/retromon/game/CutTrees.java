package com.insidethecube.retromon.game;

import com.insidethecube.retromon.engine.Mon;
import com.insidethecube.retromon.engine.Species;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * The little trees a POKéMON can CUT down (Route 2's two, John's cherry trees, where FireRed has its two): as in the
 * games, there's no way past one until you can use CUT (the CASCADEBADGE, and one of your team knowing CUT). Each
 * one's way is a box (cut_trees.tsv) you're kept out of; A in front of it says what it is, and once you can, CUT
 * takes it down and you're through to its other side (it grows back, as the games' do, so it's there next time).
 */
public final class CutTrees {
	private CutTrees() {}

	/** A tree's way (blocks x0 to x1, z0 to z1, where you'd stand at y0 to y1), and its two sides (a and b) to come out at. */
	public record Tree(String key, int x0, int z0, int x1, int z1, int y0, int y1, double ax, double az, double bx, double bz) {
		/** Whether someone at x, y, z (pad: half their width) is in its way. */
		boolean has(double x, double y, double z, double pad) {
			return x + pad > x0 && x - pad < x1 + 1 && z + pad > z0 && z - pad < z1 + 1 && y > y0 - 0.5 && y < y1 + 1;
		}
	}

	public static final List<Tree> TREES = new ArrayList<>();

	static {
		for (String[] r : Species.table("cut_trees.tsv")) {
			if (r.length < 11) continue;
			TREES.add(new Tree(r[0].trim(), i(r[1]), i(r[2]), i(r[3]), i(r[4]), i(r[5]), i(r[6]), d(r[7]), d(r[8]), d(r[9]), d(r[10])));
		}
	}

	private static int i(String s) {
		return Integer.parseInt(s.trim());
	}

	private static double d(String s) {
		return Double.parseDouble(s.trim());
	}

	/** Where each player last stood clear of every tree's way. */
	private static final Map<UUID, Vec3> CLEAR = new HashMap<>();

	/** Every tick, outside: in a tree's way, you're put back where you last stood clear of it (so it's a wall). */
	public static void keepOff(ServerPlayer p) {
		if (TREES.isEmpty()) return;
		Vec3 at = p.position();
		for (Tree t : TREES) {
			if (!t.has(at.x, at.y, at.z, 0.3)) continue;
			Vec3 back = CLEAR.get(p.getUUID());
			if (back == null || t.has(back.x, back.y, back.z, 0.3)) {
				// (Never stood clear of it this session: out to its nearer side.)
				boolean a = (at.x - t.ax()) * (at.x - t.ax()) + (at.z - t.az()) * (at.z - t.az()) <= (at.x - t.bx()) * (at.x - t.bx()) + (at.z - t.bz()) * (at.z - t.bz());
				back = new Vec3(a ? t.ax() : t.bx(), at.y, a ? t.az() : t.bz());
			}
			p.teleportTo(back.x, back.y, back.z);
			p.setDeltaMovement(Vec3.ZERO);
			return;
		}
		CLEAR.put(p.getUUID(), at);
	}

	/** The tree whose way is at a block (or right by it), or null. */
	public static Tree at(int x, int y, int z) {
		for (Tree t : TREES) if (x >= t.x0() - 1 && x <= t.x1() + 1 && z >= t.z0() - 1 && z <= t.z1() + 1 && y >= t.y0() - 2 && y <= t.y1() + 3) return t;
		return null;
	}

	/**
	 * (0.71, John's 18:06: "I'll place trees, you hook up") A little tree John's placed anywhere, as Route 2's two are
	 * made (a dark oak fence for its trunk, cherry leaves on it): it can be CUT too. The tree at a block (its leaves
	 * and trunk, all together), with you on one side of it; or null if it's not one.
	 */
	public static Tree placed(net.minecraft.server.level.ServerLevel level, ServerPlayer p, int x, int y, int z) {
		java.util.ArrayDeque<net.minecraft.core.BlockPos> todo = new java.util.ArrayDeque<>();
		java.util.Set<net.minecraft.core.BlockPos> seen = new java.util.HashSet<>();
		net.minecraft.core.BlockPos start = new net.minecraft.core.BlockPos(x, y, z);
		if (!part(level, start)) return null;
		todo.add(start);
		seen.add(start);
		boolean trunk = false, leaves = false;
		int x0 = x, x1 = x, z0 = z, z1 = z, y0 = y, y1 = y, ty = Integer.MAX_VALUE;
		while (!todo.isEmpty()) {
			net.minecraft.core.BlockPos b = todo.poll();
			if (seen.size() > 90) return null;
			boolean fence = level.getBlockState(b).is(net.minecraft.world.level.block.Blocks.DARK_OAK_FENCE);
			if (fence) {
				trunk = true;
				ty = Math.min(ty, b.getY());
			} else leaves = true;
			x0 = Math.min(x0, b.getX());
			x1 = Math.max(x1, b.getX());
			z0 = Math.min(z0, b.getZ());
			z1 = Math.max(z1, b.getZ());
			y0 = Math.min(y0, b.getY());
			y1 = Math.max(y1, b.getY());
			for (int dx = -1; dx <= 1; dx++)
				for (int dy = -1; dy <= 1; dy++)
					for (int dz = -1; dz <= 1; dz++) {
						net.minecraft.core.BlockPos o = b.offset(dx, dy, dz);
						if (!seen.contains(o) && part(level, o)) {
							seen.add(o);
							todo.add(o);
						}
					}
		}
		// (Small, as a CUT tree is: at most 5 across and 6 high.)
		if (!trunk || !leaves || x1 - x0 > 4 || z1 - z0 > 4 || y1 - y0 > 5) return null;
		double cx = (x0 + x1 + 1) / 2.0, cz = (z0 + z1 + 1) / 2.0;
		boolean alongX = Math.abs(p.getX() - cx) >= Math.abs(p.getZ() - cz);
		return alongX ? new Tree("placed", x0, z0, x1, z1, ty, ty + 2, x0 - 0.5, cz, x1 + 1.5, cz)
				: new Tree("placed", x0, z0, x1, z1, ty, ty + 2, cx, z0 - 0.5, cx, z1 + 1.5);
	}

	private static boolean part(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos b) {
		var st = level.getBlockState(b);
		return st.is(net.minecraft.world.level.block.Blocks.CHERRY_LEAVES) || st.is(net.minecraft.world.level.block.Blocks.DARK_OAK_FENCE);
	}

	/** The placed tree each player was last asked about (CUT's YES comes back for it). */
	private static final Map<UUID, Tree> ASKED = new HashMap<>();

	/** One of your team who can CUT it down (knows CUT, and isn't fainted), with the CASCADEBADGE; or null. */
	public static Mon cutter(ServerPlayer p) {
		PlayerData d = Game.data(p);
		if (!d.badges.contains("CASCADEBADGE")) return null;
		for (Mon m : d.party) if (!m.fainted() && m.moves.contains("cut")) return m;
		return null;
	}

	/** A in front of a tree: what it is, or (once you can) CUT, and you're through to its other side. */
	public static void use(ServerPlayer p, Tree t) {
		Mon m = cutter(p);
		if (m == null) {
			Game.dialogue(p, null, "talk", List.of("This tree looks like it can be CUT down!"));
			return;
		}
		// (0.61, John's 18:04) As FireRed asks: "Would you like to CUT it?"
		net.minecraft.nbt.CompoundTag c = new net.minecraft.nbt.CompoundTag();
		net.minecraft.nbt.ListTag ch = new net.minecraft.nbt.ListTag();
		for (String s : List.of("YES", "NO")) ch.add(net.minecraft.nbt.StringTag.valueOf(s));
		c.put("choices", ch);
		int idx = TREES.indexOf(t);
		if (idx < 0) ASKED.put(p.getUUID(), t);
		c.putInt("tree", idx >= 0 ? idx : -2);
		Game.dialogue(p, null, "cut", List.of("This tree looks like it can be CUT down!", "Would you like to CUT it?"), c);
	}

	/** YES to CUT: the tree's cut and you're through to its other side. */
	public static void cut(ServerPlayer p, boolean yes, int i) {
		Tree t = i == -2 ? ASKED.remove(p.getUUID()) : i >= 0 && i < TREES.size() ? TREES.get(i) : null;
		if (!yes || t == null) return;
		Mon m = cutter(p);
		if (m == null) return;
		Vec3 at = p.position();
		boolean fromA = (at.x - t.ax()) * (at.x - t.ax()) + (at.z - t.az()) * (at.z - t.az()) <= (at.x - t.bx()) * (at.x - t.bx()) + (at.z - t.bz()) * (at.z - t.bz());
		double x = fromA ? t.bx() : t.ax(), z = fromA ? t.bz() : t.az();
		double y = at.y;
		if (i == -2 && p.level() instanceof net.minecraft.server.level.ServerLevel lv) {
			// (A placed one: where you come out on its far side, a step up or down at most.)
			Double fy = null;
			for (int dy : new int[] {0, 1, -1}) {
				net.minecraft.core.BlockPos b = net.minecraft.core.BlockPos.containing(x, Math.floor(at.y) + dy, z);
				if (lv.getBlockState(b).getCollisionShape(lv, b).isEmpty() && lv.getBlockState(b.above()).getCollisionShape(lv, b.above()).isEmpty()
						&& !lv.getBlockState(b.below()).getCollisionShape(lv, b.below()).isEmpty()) {
					fy = (double) b.getY();
					break;
				}
			}
			if (fy == null) {
				Game.dialogue(p, null, "talk", List.of(m.name().toUpperCase() + " used CUT!", "...But there's no way through on the other side."));
				return;
			}
			y = fy;
		}
		float yaw = (float) Math.toDegrees(Math.atan2(-(x - at.x), z - at.z));
		Game.dialogue(p, null, "talk", List.of(m.name().toUpperCase() + " used CUT!"));
		p.teleportTo((net.minecraft.server.level.ServerLevel) p.level(), x, y, z, java.util.Set.of(), yaw, p.getXRot(), true);
		p.setDeltaMovement(Vec3.ZERO);
		CLEAR.put(p.getUUID(), new Vec3(x, y, z));
	}
}
