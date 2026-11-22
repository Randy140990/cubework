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
		Vec3 at = p.position();
		boolean fromA = (at.x - t.ax()) * (at.x - t.ax()) + (at.z - t.az()) * (at.z - t.az()) <= (at.x - t.bx()) * (at.x - t.bx()) + (at.z - t.bz()) * (at.z - t.bz());
		double x = fromA ? t.bx() : t.ax(), z = fromA ? t.bz() : t.az();
		float yaw = (float) Math.toDegrees(Math.atan2(-(x - at.x), z - at.z));
		Game.dialogue(p, null, "talk", List.of(m.name().toUpperCase() + " used CUT!"));
		p.teleportTo((net.minecraft.server.level.ServerLevel) p.level(), x, at.y, z, java.util.Set.of(), yaw, p.getXRot(), true);
		p.setDeltaMovement(Vec3.ZERO);
		CLEAR.put(p.getUUID(), new Vec3(x, at.y, z));
	}
}
