package com.insidethecube.retromon.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.insidethecube.retromon.engine.Mon;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * (0.72) STRENGTH's boulders (FireRed's, John's 18:56: "everything in there"), in VICTORY ROAD to start with and
 * wherever the PLACER puts them: a boulder two blocks across (one of the games' steps), that you can't walk through.
 * A in front of one: "It's a big boulder..." and, with one of yours that knows STRENGTH and the RAINBOWBADGE, "Would
 * you like to use STRENGTH?" Then walk into a boulder and it slides one step that way, if there's floor for it. On a
 * floor switch it stays put for good, and the rock barrier that switch holds shut crumbles away. Leave the floor and
 * the boulders not on a switch are back where they started, as in the games.
 */
public final class Boulders {
	private Boulders() {}

	/** A boulder (or a barrier's rocks) where it is now: its middle (room coordinates) and the floor's height. */
	private static final class Rock {
		final String key, kind;
		double x, z, y;
		/** Sliding: where to, and ticks left. */
		double tx, tz;
		int slide;
		Rock(String key, String kind, double x, double y, double z) {
			this.key = key;
			this.kind = kind;
			this.x = x;
			this.y = y;
			this.z = z;
		}
	}

	/** Each player's boulders and barriers in the room they're in (and which room), and whether STRENGTH is on there. */
	private static final Map<UUID, Map<String, Rock>> NOW = new HashMap<>();
	private static final Map<UUID, String> ROOM = new HashMap<>(), STRENGTH = new HashMap<>();
	private static final Map<UUID, Vec3> CLEAR = new HashMap<>();
	private static final Map<UUID, Integer> COOL = new HashMap<>();

	private static boolean pressed(PlayerData d, String sw) {
		return d.statics.contains("switch:" + sw);
	}

	/** A barrier's switch: its key without the "_b1". */
	private static String switchOf(String barrier) {
		return barrier.replaceAll("_b\\d+$", "");
	}

	/** Into a room: its boulders where they start (or where they've stayed, on a switch), its barriers still shut. */
	public static void entered(ServerPlayer p, Interiors.Room r) {
		if (!(p.level() instanceof ServerLevel level)) return;
		UUID id = p.getUUID();
		String was = ROOM.get(id);
		if (was != null && !was.equals(r.id())) STRENGTH.remove(id);
		ROOM.put(id, r.id());
		PlayerData d = Game.data(p);
		Map<String, Rock> rocks = new LinkedHashMap<>();
		for (Placer.Spot s : Placer.all()) {
			if (s.gone() || s.room() == null || !s.room().equals(r.id())) continue;
			if (!s.kind().equals("boulder") && !s.kind().equals("barrier")) continue;
			if (s.kind().equals("barrier") && pressed(d, switchOf(s.key()))) continue;
			// (The middle of its step: FireRed's two-by-two, where a trainer would stand on its second block.)
			double x = s.x() - 0.5, z = s.z() - 0.5;
			// (A boulder the room was built with, in blocks where FireRed starts it: those are this one now.)
			if (s.kind().equals("boulder")) unbuild(level, r, x, Double.isNaN(s.y()) ? 0 : s.y(), z);
			for (String st : d.statics)
				if (st.startsWith("boulder_at:" + s.key() + "=")) {
					String[] a = st.substring(st.indexOf('=') + 1).split(",");
					try {
						x = Double.parseDouble(a[0]);
						z = Double.parseDouble(a[1]);
					} catch (RuntimeException ignored) {}
				}
			Double y = floor(level, r, x, Double.isNaN(s.y()) ? 0 : s.y(), z);
			if (y == null) continue;
			rocks.put(s.key(), new Rock(s.key(), s.kind(), x, y, z));
		}
		NOW.put(id, rocks);
		show(level, r, rocks);
	}

	/** Out of the rooms (or into one with none): its boulders go back for next time. */
	public static void left(ServerPlayer p) {
		UUID id = p.getUUID();
		NOW.remove(id);
		ROOM.remove(id);
		STRENGTH.remove(id);
		CLEAR.remove(id);
	}

	/** The PLACER's put one down: shown again as they are now. */
	public static void refresh(ServerPlayer p) {
		Interiors.Room r = Interiors.roomOf(p);
		if (r != null) entered(p, r);
	}

	/** The floor's height (above the room's corner) under a boulder's middle, from about there: or null if there's none. */
	private static Double floor(ServerLevel level, Interiors.Room r, double x, double near, double z) {
		BlockPos c = BlockPos.containing(r.ox() + x - 0.5, r.oy() + near, r.oz() + z - 0.5);
		for (int dy : new int[] {0, 1, -1, 2, -2, 3, -3, 4, -4, 5, -5, 6}) {
			BlockPos at = c.offset(0, dy, 0);
			if (empty(level, at) && empty(level, at.above()) && !empty(level, at.below())) return (double) (at.getY() - r.oy());
		}
		return null;
	}

	/** The boulder built into the room's blocks (VICTORY ROAD's, a two-by-two of copper grate) at its start: taken out. */
	private static void unbuild(ServerLevel level, Interiors.Room r, double x, double near, double z) {
		for (int dx : new int[] {-1, 0})
			for (int dz : new int[] {-1, 0})
				for (int dy = 0; dy <= 3; dy++) {
					BlockPos b = BlockPos.containing(r.ox() + x + dx + 0.5, r.oy() + near + dy, r.oz() + z + dz + 0.5);
					if (level.getBlockState(b).is(net.minecraft.world.level.block.Blocks.WAXED_COPPER_GRATE))
						level.setBlock(b, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
				}
	}

	private static boolean empty(ServerLevel level, BlockPos b) {
		return level.getBlockState(b).getCollisionShape(level, b).isEmpty() && level.getFluidState(b).isEmpty();
	}

	/** The boulders' and barriers' shapes in the room (WorldMon "boulder:" and "barrier:"), each where it is. */
	private static void show(ServerLevel level, Interiors.Room r, Map<String, Rock> rocks) {
		AABB box = new AABB(r.ox() - 4, r.oy() - 10, r.oz() - 4, r.ox() + r.w() + 4, r.oy() + 16, r.oz() + r.d() + 4);
		java.util.Set<String> want = new java.util.HashSet<>();
		for (Rock k : rocks.values()) want.add(k.kind + ":" + k.key);
		for (WorldMon w : level.getEntitiesOfClass(WorldMon.class, box, w -> w.key().startsWith("boulder:") || w.key().startsWith("barrier:")))
			if (!want.contains(w.key())) w.discard();
		for (Rock k : rocks.values()) {
			String ek = k.kind + ":" + k.key;
			List<WorldMon> have = level.getEntitiesOfClass(WorldMon.class, box, w -> ek.equals(w.key()));
			for (int i = 1; i < have.size(); i++) have.get(i).discard();
			double wx = r.ox() + k.x, wy = r.oy() + k.y, wz = r.oz() + k.z;
			if (have.isEmpty()) Game.placeMon(level, k.kind.equals("barrier") ? "rock_barrier" : "boulder", ek, wx, wy, wz, 0F);
			else have.get(0).teleportTo(wx, wy, wz);
		}
	}

	/** A on a boulder (or a barrier's rocks). True if it was one. */
	public static boolean talk(ServerPlayer p, WorldMon w) {
		if (!w.key().startsWith("boulder:") && !w.key().startsWith("barrier:")) return false;
		if (w.key().startsWith("barrier:")) {
			Game.message(p, List.of("Rocks block the way.", "There must be a switch somewhere that opens it..."));
			return true;
		}
		UUID id = p.getUUID();
		String room = ROOM.get(id);
		if (room != null && room.equals(STRENGTH.get(id))) return true;
		Mon m = strong(p);
		if (m == null) {
			Game.message(p, List.of("It's a big boulder, but a POKéMON may be able to push it aside."));
			return true;
		}
		Game.dialogue(p, null, "strength", List.of("It's a big boulder, but a POKéMON may be able to push it aside.", "Would you like to use STRENGTH?"), Game.yesNo());
		return true;
	}

	/** One of your team who can use STRENGTH (knows it, not fainted) with the RAINBOWBADGE, as FireRed has it; or null. */
	private static Mon strong(ServerPlayer p) {
		PlayerData d = Game.data(p);
		if (!d.badges.contains("RAINBOWBADGE")) return null;
		for (Mon m : d.party) if (!m.fainted() && m.moves.contains("strength")) return m;
		return null;
	}

	/** YES to STRENGTH: the boulders on this floor can be pushed now. */
	public static void strength(ServerPlayer p, boolean yes) {
		if (!yes) return;
		Mon m = strong(p);
		String room = ROOM.get(p.getUUID());
		if (m == null || room == null) return;
		STRENGTH.put(p.getUUID(), room);
		String n = m.name().toUpperCase();
		Game.message(p, List.of(n + " used STRENGTH!", n + "'s STRENGTH made it possible to move boulders around!"));
	}

	/** Every tick, in a room with boulders: you're kept out of them (and the barriers), and push one with STRENGTH on. */
	public static void tick(ServerPlayer p) {
		UUID id = p.getUUID();
		Map<String, Rock> rocks = NOW.get(id);
		Interiors.Room r = Interiors.roomOf(p);
		if (rocks == null || rocks.isEmpty() || r == null || !r.id().equals(ROOM.get(id)) || !(p.level() instanceof ServerLevel level)) return;
		COOL.computeIfPresent(id, (k, v) -> v > 1 ? v - 1 : null);
		// Sliding ones: a little further each tick.
		for (Rock k : rocks.values()) {
			if (k.slide <= 0) continue;
			k.slide--;
			double f = k.slide == 0 ? 1 : 0.35;
			k.x += (k.tx - k.x) * f;
			k.z += (k.tz - k.z) * f;
			if (k.slide == 0) {
				k.x = k.tx;
				k.z = k.tz;
				landed(p, level, r, rocks, k);
			}
			String ek = k.kind + ":" + k.key;
			for (WorldMon w : level.getEntitiesOfClass(WorldMon.class, p.getBoundingBox().inflate(40), w -> ek.equals(w.key())))
				w.teleportTo(r.ox() + k.x, r.oy() + k.y, r.oz() + k.z);
		}
		double lx = p.getX() - r.ox(), ly = p.getY() - r.oy(), lz = p.getZ() - r.oz();
		Rock in = null;
		for (Rock k : rocks.values()) {
			if (Math.abs(ly - k.y) > 1.6) continue;
			if (Math.abs(lx - k.x) < 1.0 + 0.3 && Math.abs(lz - k.z) < 1.0 + 0.3) {
				in = k;
				break;
			}
		}
		if (in == null) {
			CLEAR.put(id, p.position());
			return;
		}
		Vec3 back = CLEAR.getOrDefault(id, null);
		// Pushing it (STRENGTH on, a boulder, not already moving): one step the way you came at it.
		if (in.kind.equals("boulder") && r.id().equals(STRENGTH.get(id)) && in.slide <= 0 && !COOL.containsKey(id) && back != null) {
			double bx = back.x - r.ox(), bz = back.z - r.oz();
			double ox = bx - in.x, oz = bz - in.z;
			int sx = 0, sz = 0;
			if (Math.abs(ox) > Math.abs(oz)) sx = ox > 0 ? -1 : 1;
			else sz = oz > 0 ? -1 : 1;
			double nx = in.x + sx * 2, nz = in.z + sz * 2;
			if (free(level, r, rocks, in, nx, in.y, nz)) {
				in.tx = nx;
				in.tz = nz;
				in.slide = 6;
				COOL.put(id, 12);
				level.playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.STONE_PLACE, net.minecraft.sounds.SoundSource.BLOCKS, 0.8F, 0.7F);
			}
		}
		// You can't walk through it: back where you last stood clear.
		if (back != null) {
			p.teleportTo(back.x, back.y, back.z);
			p.setDeltaMovement(Vec3.ZERO);
		}
	}

	/** Whether a boulder's two-by-two can go there: floor at its height, nothing in the way, no other boulder. */
	private static boolean free(ServerLevel level, Interiors.Room r, Map<String, Rock> rocks, Rock me, double x, double y, double z) {
		for (Rock k : rocks.values()) if (k != me && Math.abs(k.x - x) < 1.9 && Math.abs(k.z - z) < 1.9) return false;
		for (double dx : new double[] {-0.5, 0.5})
			for (double dz : new double[] {-0.5, 0.5}) {
				BlockPos b = BlockPos.containing(r.ox() + x + dx, r.oy() + y, r.oz() + z + dz);
				if (!empty(level, b) || !empty(level, b.above()) || empty(level, b.below())) return false;
			}
		AABB box = new AABB(r.ox() + x - 1, r.oy() + y, r.oz() + z - 1, r.ox() + x + 1, r.oy() + y + 2, r.oz() + z + 1);
		return level.getEntitiesOfClass(Npc.class, box, n -> true).isEmpty();
	}

	/** A boulder's come to rest: on a floor switch, it stays there for good, and the switch's barrier crumbles. */
	private static void landed(ServerPlayer p, ServerLevel level, Interiors.Room r, Map<String, Rock> rocks, Rock k) {
		PlayerData d = Game.data(p);
		for (Placer.Spot s : Placer.all()) {
			if (!s.kind().equals("switch") || s.gone() || s.room() == null || !s.room().equals(r.id()) || pressed(d, s.key())) continue;
			if (Math.abs(s.x() - 0.5 - k.x) > 1.1 || Math.abs(s.z() - 0.5 - k.z) > 1.1) continue;
			d.statics.add("switch:" + s.key());
			d.statics.removeIf(st -> st.startsWith("boulder_at:" + k.key + "="));
			d.statics.add("boulder_at:" + k.key + "=" + k.x + "," + k.z);
			List<String> open = new ArrayList<>();
			for (Rock b : rocks.values()) if (b.kind.equals("barrier") && switchOf(b.key).equals(s.key())) open.add(b.key);
			for (String b : open) {
				rocks.remove(b);
				for (WorldMon w : level.getEntitiesOfClass(WorldMon.class, p.getBoundingBox().inflate(80), w -> ("barrier:" + b).equals(w.key()))) {
					level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK,
							net.minecraft.world.level.block.Blocks.STONE.defaultBlockState()), w.getX(), w.getY() + 1, w.getZ(), 30, 0.6, 0.6, 0.6, 0.1);
					w.discard();
				}
			}
			level.playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.STONE_BREAK, net.minecraft.sounds.SoundSource.BLOCKS, 1F, 0.8F);
			Game.sync(p);
			Game.message(p, List.of("The boulder pressed the floor switch...", open.isEmpty() ? "Something changed somewhere." : "The rocks blocking the way crumbled away!"));
			return;
		}
	}

	/** Whether a room has boulders or barriers placed in it (so its ticks look after them). */
	public static boolean has(String room) {
		for (Placer.Spot s : Placer.all())
			if (!s.gone() && room.equals(s.room()) && (s.kind().equals("boulder") || s.kind().equals("barrier"))) return true;
		return false;
	}
}
