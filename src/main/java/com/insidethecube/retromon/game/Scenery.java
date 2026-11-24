package com.insidethecube.retromon.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * (0.59.4, 07:36: "where you've got the water in Fuchsia, add some swimming Pokémon ... and a couple of others that
 * roam around in the entrance area, non-combative") POKéMON that are part of a place's scenery: swimming about its
 * ponds or pottering about its grass, never battled (press A and it's just enjoying itself). Each (scenery.tsv) keeps to
 * its own patch: a swimmer to the water's surface there, a walker to the open ground. They're out while someone's near
 * and gone when nobody is, so a save never piles them up.
 */
public final class Scenery {
	private Scenery() {}

	/**
	 * One: its key ("scene:" + id), its species, whether it swims, its patch (blocks, both ends in) and the surface's
	 * height there; (0.59.6, 13:17: "they probably don't need to be at the top, they can be underneath") whether it
	 * swims under the surface (the fish) rather than on it.
	 */
	public record Def(String key, String species, boolean swims, int x0, int z0, int x1, int z1, int y, boolean dives) {
		double midX() {
			return (x0 + x1) / 2.0 + 0.5;
		}

		double midZ() {
			return (z0 + z1) / 2.0 + 0.5;
		}
	}

	/** Where one's going and how long it rests when it's there. */
	private static final class Plan {
		double tx, tz;
		int rest;
	}

	public static final List<Def> DEFS = new ArrayList<>();
	private static final Map<Integer, Plan> PLANS = new HashMap<>();
	private static final Random RNG = new Random();
	/** Out while you're this near (blocks from its patch's middle), gone once nobody's this near. */
	private static final double NEAR = 56, GONE = 72;
	/** How fast they go (blocks a tick). */
	private static final double SWIM = 0.035, WALK = 0.045;

	static {
		for (String[] r : com.insidethecube.retromon.engine.Species.table("scenery.tsv")) {
			if (r.length < 8) continue;
			try {
				String mode = r[2].trim();
				DEFS.add(new Def("scene:" + r[0].trim(), r[1].trim(), mode.equals("swim") || mode.equals("dive"), Integer.parseInt(r[3].trim()), Integer.parseInt(r[4].trim()),
						Integer.parseInt(r[5].trim()), Integer.parseInt(r[6].trim()), Integer.parseInt(r[7].trim()), mode.equals("dive")));
			} catch (NumberFormatException ignored) {
				// A row that isn't one.
			}
		}
	}

	public static boolean is(String key) {
		return key != null && key.startsWith("scene:");
	}

	public static Def def(String key) {
		for (Def d : DEFS) if (d.key().equals(key)) return d;
		return null;
	}

	/** What it's up to, for when you press A by it. */
	public static String line(String name, String key) {
		Def d = def(key);
		return d != null && d.swims() ? name + " is swimming happily." : name + " is pottering about happily.";
	}

	/** Every tick, in a Retromon save: out where someone's near, moving about; gone where nobody is. */
	public static void tick(MinecraftServer s) {
		ServerLevel level = s.overworld();
		boolean check = s.getTickCount() % 20 == 0;
		for (Def d : DEFS) {
			AABB patch = new AABB(d.x0() - 6, d.y() - 6, d.z0() - 6, d.x1() + 7, d.y() + 8, d.z1() + 7);
			List<WorldMon> there = level.hasChunkAt(BlockPos.containing(d.midX(), d.y(), d.midZ()))
					? level.getEntitiesOfClass(WorldMon.class, patch, w -> d.key().equals(w.key()))
					: List.of();
			if (check) {
				boolean near = false, keep = false;
				for (ServerPlayer p : s.getPlayerList().getPlayers()) {
					if (p.level() != level || !Game.adventure(p)) continue;
					double dist = Math.hypot(p.getX() - d.midX(), p.getZ() - d.midZ());
					near |= dist < NEAR;
					keep |= dist < GONE;
				}
				// (One left in a save from before, or a second one: only one of each.)
				for (int i = keep ? 1 : 0; i < there.size(); i++) {
					PLANS.remove(there.get(i).getId());
					there.get(i).discard();
				}
				if (!keep) continue;
				if (there.isEmpty() && near && level.hasChunkAt(BlockPos.containing(d.midX(), d.y(), d.midZ()))) {
					double[] at = spot(level, d);
					if (at == null) continue;
					WorldMon w = Game.placeMon(level, d.species(), d.key(), at[0], at[1], at[2], RNG.nextFloat() * 360F);
					if (w != null) {
						w.setNoGravity(true);
						w.noPhysics = true;
					}
					continue;
				}
			}
			if (!there.isEmpty()) move(level, d, there.get(0));
		}
	}

	/** How high a swimmer is: on the surface, bobbing; a diver well under it, drifting up and down a little. */
	private static double swimY(Def d, int tick) {
		return d.dives() ? d.y() - 1.1 + Math.sin(tick * 0.05) * 0.15 : d.y() + 0.4 + Math.sin(tick * 0.1) * 0.04;
	}

	/** (0.59.6, 13:17: "there are 2 stacked") Another of the place's POKéMON within this of a spot: not there. */
	private static final double APART = 2.2;

	private static boolean crowded(ServerLevel level, Def d, double x, double z, WorldMon self) {
		AABB near = new AABB(x - APART, d.y() - 4, z - APART, x + APART, d.y() + 4, z + APART);
		for (WorldMon o : level.getEntitiesOfClass(WorldMon.class, near, w -> is(w.key()) && w != self))
			if (Math.hypot(o.getX() - x, o.getZ() - z) < APART) return true;
		return false;
	}

	/** A random good spot in its patch: on the water (a swimmer) or on open ground (a walker), or null. */
	private static double[] spot(ServerLevel level, Def d) {
		return spot(level, d, null);
	}

	private static double[] spot(ServerLevel level, Def d, WorldMon self) {
		for (int tries = 0; tries < 12; tries++) {
			int x = d.x0() + RNG.nextInt(d.x1() - d.x0() + 1), z = d.z0() + RNG.nextInt(d.z1() - d.z0() + 1);
			if (crowded(level, d, x + 0.5, z + 0.5, self)) continue;
			if (d.swims()) {
				BlockPos at = new BlockPos(x, d.y(), z);
				if (level.getBlockState(at).is(Blocks.WATER) && level.getBlockState(at.above()).isAir() && (!d.dives() || level.getBlockState(at.below()).is(Blocks.WATER)))
					return new double[] {x + 0.5, swimY(d, 0), z + 0.5};
			} else {
				Double y = ground(level, x + 0.5, z + 0.5, d.y());
				if (y != null) return new double[] {x + 0.5, y, z + 0.5};
			}
		}
		return null;
	}

	/** The height to stand at over (x, z) near a height: on the top of the ground there with room above, or null. */
	private static Double ground(ServerLevel level, double x, double z, int near) {
		for (int y = near + 2; y >= near - 3; y--) {
			BlockPos at = BlockPos.containing(x, y, z);
			BlockState below = level.getBlockState(at.below()), here = level.getBlockState(at), up = level.getBlockState(at.above());
			if (below.isFaceSturdy(level, at.below(), net.minecraft.core.Direction.UP) && here.getCollisionShape(level, at).isEmpty()
					&& up.getCollisionShape(level, at.above()).isEmpty() && !here.is(Blocks.WATER)) return (double) y;
		}
		return null;
	}

	private static void move(ServerLevel level, Def d, WorldMon w) {
		w.setNoGravity(true);
		w.noPhysics = true;
		Plan plan = PLANS.computeIfAbsent(w.getId(), k -> {
			Plan p = new Plan();
			p.tx = w.getX();
			p.tz = w.getZ();
			p.rest = 20 + RNG.nextInt(60);
			return p;
		});
		double dx = plan.tx - w.getX(), dz = plan.tz - w.getZ(), dist = Math.hypot(dx, dz);
		if (dist < 0.08) {
			if (--plan.rest > 0) {
				// (A swimmer bobs as it rests.)
				if (d.swims()) w.setPos(w.getX(), swimY(d, w.tickCount), w.getZ());
				return;
			}
			double[] next = spot(level, d, w);
			if (next == null) return;
			plan.tx = next[0] + (RNG.nextDouble() - 0.5) * 0.6;
			plan.tz = next[2] + (RNG.nextDouble() - 0.5) * 0.6;
			plan.rest = (d.swims() ? 30 : 50) + RNG.nextInt(d.swims() ? 80 : 140);
			return;
		}
		double step = Math.min(dist, d.swims() ? SWIM : WALK);
		double nx = w.getX() + dx / dist * step, nz = w.getZ() + dz / dist * step, ny;
		if (d.swims()) {
			// Only over the water: anything else in the way and it turns back.
			BlockPos at = BlockPos.containing(nx, d.y(), nz);
			if (!level.getBlockState(at).is(Blocks.WATER) || !level.getBlockState(at.above()).isAir()) {
				plan.tx = w.getX();
				plan.tz = w.getZ();
				return;
			}
			// (Nor through another of the place's: it waits.)
			if (crowded(level, d, nx, nz, w) && !crowded(level, d, w.getX(), w.getZ(), w)) {
				plan.tx = w.getX();
				plan.tz = w.getZ();
				return;
			}
			ny = swimY(d, w.tickCount);
		} else {
			Double g = ground(level, nx, nz, (int) Math.floor(w.getY() + 0.5));
			if (g == null) {
				plan.tx = w.getX();
				plan.tz = w.getZ();
				return;
			}
			ny = g;
		}
		w.setFacing((float) Math.toDegrees(Math.atan2(-dx, dz)));
		w.setPos(nx, ny, nz);
	}
}
