package com.insidethecube.retromon.game;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.insidethecube.retromon.Retromon;
import com.insidethecube.retromon.engine.Item;
import com.insidethecube.retromon.engine.Species;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;

/**
 * (0.72, John's 18:24 and 18:56: "the placement of absolutely everything. Items across the map, all the caves ... I'll
 * need a way where I can touch a pixel on the map and place them"; "on the next pass, I want obviously everything in
 * there") The PLACER: everything FireRed has outside and in the caves (placeables.tsv: every trainer, person, item
 * ball, hidden item, STRENGTH boulder and floor switch), and where John has put each one down in his map.
 *
 * <p>Where each is (placed.tsv in the mod, then his own in the game folder's config/retromon_placed.tsv, his last
 * word): out in the world (x, y, z) or in a room (its id, and x, y, z from its corner), and the way it faces. A
 * trainer or a person stands there, an item ball lies there (till it's picked up), a hidden item is found with A
 * there; a boulder and a switch are for STRENGTH (Boulders). Only whoever runs the game can place.
 */
public final class Placer {
	private Placer() {}

	/** Something FireRed has: its place, what it is, its key in the mod, what it's called and what it has. */
	public record Thing(String place, String kind, String key, String label, String detail, int gx, int gy, String facing, int sight) {}

	/** Where something's been put down (room null: out in the world); gone: taken away (one the mod had). */
	public record Spot(String key, String kind, String place, String room, double x, double y, double z, float yaw, boolean gone) {}

	public static final List<Thing> CATALOG = new ArrayList<>();
	public static final Map<String, Thing> BY_KEY = new LinkedHashMap<>();
	private static final Map<String, Spot> BAKED = new LinkedHashMap<>(), MINE = new LinkedHashMap<>();
	private static boolean loaded;

	static {
		for (String[] r : Species.table("placeables.tsv")) {
			if (r.length < 10) continue;
			try {
				Thing t = new Thing(r[1], r[2], r[3], r[4], r[5], Integer.parseInt(r[6].trim()), Integer.parseInt(r[7].trim()), r[8], Integer.parseInt(r[9].trim()));
				CATALOG.add(t);
				BY_KEY.putIfAbsent(t.key(), t);
			} catch (NumberFormatException ignored) {}
		}
	}

	private static Path file() {
		return net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("retromon_placed.tsv");
	}

	private static void read(List<String> lines, Map<String, Spot> into) {
		for (String l : lines) {
			if (l.isBlank() || l.startsWith("#")) continue;
			String[] a = l.split("\t", -1);
			if (a.length < 9) continue;
			try {
				into.put(a[0], new Spot(a[0], a[1], a[2], a[3].isEmpty() || a[3].equals("-") ? null : a[3], Double.parseDouble(a[4]), Double.parseDouble(a[5]),
						Double.parseDouble(a[6]), Float.parseFloat(a[7]), a[8].equals("gone")));
			} catch (NumberFormatException ignored) {}
		}
	}

	private static synchronized void load() {
		if (loaded) return;
		loaded = true;
		try (InputStream in = Placer.class.getResourceAsStream("/retromon/data/placed.tsv")) {
			if (in != null) read(List.of(new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n")), BAKED);
		} catch (IOException e) {
			Retromon.LOG.warn("Retromon: couldn't read the placed things", e);
		}
		try {
			if (Files.exists(file())) read(Files.readAllLines(file(), StandardCharsets.UTF_8), MINE);
		} catch (IOException e) {
			Retromon.LOG.warn("Retromon: couldn't read {}", file(), e);
		}
	}

	private static void save() {
		StringBuilder b = new StringBuilder("# Retromon's PLACER (OPTION): what you've put down. Send this file to Claude to build it into the mod.\n"
				+ "# key\tkind\tplace\troom\tx\ty\tz\tyaw\tstate\n");
		for (Spot s : MINE.values())
			b.append(s.key()).append('\t').append(s.kind()).append('\t').append(s.place()).append('\t').append(s.room() == null ? "-" : s.room()).append('\t')
					.append(String.format(java.util.Locale.ROOT, "%.2f\t%.2f\t%.2f\t%.0f", s.x(), s.y(), s.z(), s.yaw())).append('\t')
					.append(s.gone() ? "gone" : "placed").append('\n');
		try {
			Files.createDirectories(file().getParent());
			Files.writeString(file(), b.toString(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			Retromon.LOG.warn("Retromon: couldn't save {}", file(), e);
		}
	}

	/** Where something is now (John's, else the mod's), or null; a taken-away one too (gone). */
	public static Spot spot(String key) {
		load();
		Spot s = MINE.get(key);
		return s != null ? s : BAKED.get(key);
	}

	/** Whether something's been put down (and not taken away). */
	public static boolean placed(String key) {
		Spot s = key == null ? null : spot(key);
		return s != null && !s.gone();
	}

	/** Whether something the mod had has been taken away. */
	public static boolean gone(String key) {
		Spot s = key == null ? null : spot(key);
		return s != null && s.gone();
	}

	public static List<Spot> all() {
		load();
		Map<String, Spot> m = new LinkedHashMap<>(BAKED);
		m.putAll(MINE);
		return new ArrayList<>(m.values());
	}

	private static boolean runsGame(ServerPlayer p) {
		MinecraftServer server = p.level().getServer();
		return server != null && (server.isSingleplayer()
				|| p.createCommandSourceStack().permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER));
	}

	/** What's placed, to the PLACER's screen. */
	public static void send(ServerPlayer p) {
		CompoundTag t = new CompoundTag();
		ListTag l = new ListTag();
		for (Spot s : all()) {
			CompoundTag o = new CompoundTag();
			o.putString("key", s.key());
			o.putString("state", s.gone() ? "gone" : MINE.containsKey(s.key()) ? "mine" : "mod");
			l.add(o);
		}
		t.put("placed", l);
		t.putString("file", file().toString());
		Net.send(p, "placer", t);
	}

	/**
	 * From the PLACER: "place" (that key here, where you stand, the way you face: north, east, south or west), "gone"
	 * (taken away), "back" (back as the mod has it). Then it's there for you straight away.
	 */
	public static void act(ServerPlayer p, CompoundTag t) {
		if (!Game.adventure(p)) return;
		if (!runsGame(p)) {
			Game.message(p, List.of("The PLACER is only for whoever runs the game."));
			return;
		}
		load();
		String key = t.getStringOr("key", ""), mode = t.getStringOr("mode", "place");
		Thing th = BY_KEY.get(key);
		if (th == null) return;
		ServerLevel level = (ServerLevel) p.level();
		switch (mode) {
			case "place" -> {
				Interiors.Room r = Interiors.roomOf(p);
				float yaw = Math.round(net.minecraft.util.Mth.wrapDegrees(p.getYRot()) / 90F) * 90F;
				double x = Math.floor(p.getX()) + 0.5, z = Math.floor(p.getZ()) + 0.5, y = Math.floor(p.getY() + 0.01);
				Spot s = r == null ? new Spot(key, th.kind(), th.place(), null, x, y, z, yaw, false)
						: new Spot(key, th.kind(), th.place(), r.id(), x - r.ox(), y - r.oy(), z - r.oz(), yaw, false);
				MINE.put(key, s);
				FLOORS.remove(key);
				OUT.remove(key);
				AT_SEA.remove(key);
				save();
				if (rocky(th.kind())) Boulders.refresh(p);
				else if (!th.kind().equals("hidden")) {
					clear(level, key);
					ensureOne(level, s);
				}
				Retromon.LOG.info("Retromon: PLACER put {} at {}", key, s);
			}
			case "gone" -> {
				Spot was = spot(key);
				MINE.put(key, new Spot(key, th.kind(), th.place(), was == null ? null : was.room(), 0, 0, 0, 0, true));
				save();
				clear(level, key);
				if (rocky(th.kind())) Boulders.refresh(p);
			}
			case "back" -> {
				MINE.remove(key);
				FLOORS.remove(key);
				OUT.remove(key);
				AT_SEA.remove(key);
				save();
				clear(level, key);
				if (rocky(th.kind())) Boulders.refresh(p);
			}
			default -> {
			}
		}
		send(p);
	}

	/** A boulder, a floor switch or a rock barrier: Boulders looks after those, in their rooms. */
	static boolean rocky(String kind) {
		return kind.equals("boulder") || kind.equals("switch") || kind.equals("barrier");
	}

	/** Where a spot is in its level. */
	private static double[] at(Spot s) {
		if (s.room() == null) return new double[] {s.x(), s.y(), s.z()};
		Interiors.Room r = Interiors.room(s.room());
		return r == null ? null : new double[] {r.ox() + s.x(), r.oy() + s.y(), r.oz() + s.z()};
	}

	/** Floors found under spots placed without a height (the caves' baked ones). */
	private static final Map<String, Double> FLOORS = new java.util.HashMap<>();
	/** (0.74) Where the outside ones placed without a height stand (found once each), and who of them is out at sea. */
	private static final Map<String, double[]> OUT = new java.util.HashMap<>();
	private static final java.util.Set<String> AT_SEA = new java.util.HashSet<>();

	/**
	 * (0.74) Saffron's people as FireRed has them: TEAM ROCKET's grunts in its streets till their BOSS is beaten in
	 * SILPH CO., the townsfolk out again after.
	 */
	private static final String SILPH = "beat:silph_co_11f_0";
	private static final Map<String, String> WHEN = new java.util.HashMap<>();
	static {
		for (String k : new String[] {"saffron_city_p0", "saffron_city_p1", "saffron_city_p2", "saffron_city_p3", "saffron_city_p4", "saffron_city_p5", "saffron_city_p6",
				"saffron_city_p13"}) WHEN.put(k, "!" + SILPH);
		for (String k : new String[] {"saffron_city_p7", "saffron_city_p8", "saffron_city_p9", "saffron_city_p10", "saffron_city_p11", "saffron_city_p12"})
			WHEN.put(k, SILPH);
	}

	private static boolean showsFor(ServerPlayer p, Spot s) {
		String w = WHEN.get(s.key());
		if (w == null) return true;
		return w.startsWith("!") ? !Game.meets(p, w.substring(1)) : Game.meets(p, w);
	}

	/**
	 * (0.74) An outside spot baked without a height (John's 10:28: everyone outside at a best guess): the ground there
	 * as the game finds it for anyone (the street, not a roof; off a tree), or for a SWIMMER the sea near it.
	 */
	private static double[] outside(ServerLevel lv, Spot s, double[] a) {
		double[] have = OUT.get(s.key());
		if (have != null) return have;
		int x = (int) Math.floor(a[0]), z = (int) Math.floor(a[2]);
		if (!lv.hasChunkAt(new BlockPos(x, 64, z))) return null;
		Trainers.Trainer t = s.kind().equals("trainer") ? Trainers.get(s.key()) : null;
		if (t != null && Game.swimmer(t)) {
			BlockPos w = sea(lv, x, z);
			if (w != null) {
				AT_SEA.add(s.key());
				have = new double[] {w.getX() + 0.5, w.getY() + 0.9, w.getZ() + 0.5};
			}
		}
		if (have == null) {
			BlockPos g = Game.spot(lv, x, z, 5);
			if (g == null) return null;
			have = new double[] {g.getX() + 0.5, g.getY(), g.getZ() + 0.5};
		}
		OUT.put(s.key(), have);
		return have;
	}

	/** Open sea near a spot (two deep, nothing over it): its top water block; or null. */
	private static BlockPos sea(ServerLevel lv, int x, int z) {
		for (int r = 0; r <= 8; r++) {
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
					int top = lv.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x + dx, z + dz) - 1;
					BlockPos at = new BlockPos(x + dx, top, z + dz);
					if (lv.getBlockState(at).is(net.minecraft.world.level.block.Blocks.WATER) && lv.getBlockState(at.below()).is(net.minecraft.world.level.block.Blocks.WATER)
							&& lv.getBlockState(at.above()).isAir() && lv.getBlockState(at.above(2)).isAir()) return at;
				}
			}
		}
		return null;
	}

	/** Where a spot is, its height found (the floor there, from the top) where it was placed without one. */
	private static double[] atIn(ServerLevel lv, Spot s) {
		double[] a = at(s);
		if (a == null || !Double.isNaN(a[1])) return a;
		if (s.room() == null) return outside(lv, s, a);
		Double y = FLOORS.get(s.key());
		if (y == null) {
			Interiors.Room r = Interiors.room(s.room());
			if (r == null || !lv.hasChunkAt(BlockPos.containing(a[0], r.oy(), a[2]))) return null;
			for (int dy = 12; dy >= -8 && y == null; dy--) {
				BlockPos b = BlockPos.containing(a[0], r.oy() + dy, a[2]);
				if (lv.getBlockState(b).getCollisionShape(lv, b).isEmpty() && lv.getBlockState(b.above()).getCollisionShape(lv, b.above()).isEmpty()
						&& !lv.getBlockState(b.below()).getCollisionShape(lv, b.below()).isEmpty())
					y = (double) b.getY();
			}
			if (y == null) return null;
			FLOORS.put(s.key(), y);
		}
		return new double[] {a[0], y, a[2]};
	}

	private static String entityKey(Spot s) {
		return switch (s.kind()) {
			case "item" -> "item:" + s.key();
			case "boulder" -> "boulder:" + s.key();
			case "barrier" -> "barrier:" + s.key();
			default -> s.key();
		};
	}

	/** Whoever (or whatever) of that key is about, round all the players: gone (to be put back where it's placed now). */
	private static void clear(ServerLevel level, String key) {
		MinecraftServer server = level.getServer();
		List<ServerLevel> levels = new ArrayList<>();
		levels.add(server.overworld());
		levels.add(Interiors.ensure(server));
		for (ServerLevel lv : levels) {
			if (lv == null) continue;
			for (ServerPlayer p : lv.players()) {
				AABB box = p.getBoundingBox().inflate(160);
				for (Npc n : lv.getEntitiesOfClass(Npc.class, box, n -> key.equals(n.key()))) n.discard();
				for (WorldMon w : lv.getEntitiesOfClass(WorldMon.class, box, w -> ("item:" + key).equals(w.key()) || ("pet:" + key).equals(w.key())
						|| ("boulder:" + key).equals(w.key()) || ("barrier:" + key).equals(w.key()))) w.discard();
			}
		}
	}

	/** Each couple of seconds: what's placed near each player is there, where it's placed (and nothing taken away is). */
	public static void tick(MinecraftServer server) {
		if (!Game.ourSave()) return;
		load();
		List<Spot> all = all();
		if (all.isEmpty()) return;
		ServerLevel world = server.overworld(), rooms = Interiors.ensure(server);
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			Interiors.Room here = Interiors.roomOf(p);
			for (Spot s : all) {
				if (rocky(s.kind()) || s.kind().equals("hidden")) continue;
				ServerLevel lv = s.room() == null ? world : rooms;
				if (lv == null || p.level() != lv) continue;
				if (s.room() != null && (here == null || !here.id().equals(s.room()))) continue;
				double[] a = atIn(lv, s);
				if (a == null || p.distanceToSqr(a[0], a[1], a[2]) > 96 * 96 || !lv.hasChunkAt(BlockPos.containing(a[0], a[1], a[2]))) continue;
				if (s.gone() || !showsFor(p, s)) clearNear(lv, s, a);
				else ensureOne(lv, s);
			}
		}
	}

	private static void clearNear(ServerLevel lv, Spot s, double[] a) {
		String k = entityKey(s);
		AABB box = new AABB(a[0] - 96, a[1] - 16, a[2] - 96, a[0] + 96, a[1] + 16, a[2] + 96);
		for (Npc n : lv.getEntitiesOfClass(Npc.class, box, n -> k.equals(n.key()))) n.discard();
		for (WorldMon w : lv.getEntitiesOfClass(WorldMon.class, box, w -> k.equals(w.key()) || ("pet:" + s.key()).equals(w.key()))) w.discard();
	}

	/** One placed thing: there (once), where it's placed. */
	private static void ensureOne(ServerLevel lv, Spot s) {
		double[] a = atIn(lv, s);
		if (a == null || s.gone()) return;
		switch (s.kind()) {
			case "trainer", "person" -> {
				Npc have = null;
				AABB box = new AABB(a[0] - 96, a[1] - 16, a[2] - 96, a[0] + 96, a[1] + 16, a[2] + 96);
				for (Npc n : lv.getEntitiesOfClass(Npc.class, box, n -> s.key().equals(n.key()))) {
					if (have != null) n.discard();
					else have = n;
				}
				Trainers.Person ps = Trainers.PEOPLE.get(s.key());
				if (ps != null && ps.species() != null) {
					// (A person's POKéMON, standing in for them.)
					String pk = "pet:" + s.key();
					if (lv.getEntitiesOfClass(WorldMon.class, box, w -> pk.equals(w.key())).isEmpty())
						Game.placeMon(lv, ps.species(), pk, a[0], a[1], a[2], s.yaw());
					return;
				}
				if (have != null) {
					// (Moved by the placer: to the new spot. Walking about near it is theirs.)
					if (have.distanceToSqr(a[0], a[1], a[2]) > 9 && have.approaching <= 0 && have.talker == null) {
						have.teleportTo(a[0], a[1], a[2]);
						have.rehome(BlockPos.containing(a[0], a[1], a[2]));
						have.setFacing(s.yaw());
					}
					return;
				}
				Trainers.Trainer t = Trainers.get(s.key());
				int role = t != null ? (t.leader() ? Npc.LEADER : Npc.TRAINER) : Npc.PERSON;
				Npc n = Game.place(lv, role, s.key(), a[0], a[1], a[2], s.yaw());
				if (n != null) {
					// ((0.74) A SWIMMER out at sea swims there.)
					if (AT_SEA.contains(s.key())) n.setSwimming(true);
					n.setFacing(s.yaw());
					n.rehome(BlockPos.containing(a[0], a[1], a[2]));
				}
			}
			case "item" -> {
				String k = "item:" + s.key();
				if (Game.takenByAnyone(k)) return;
				AABB box = new AABB(a[0] - 96, a[1] - 16, a[2] - 96, a[0] + 96, a[1] + 16, a[2] + 96);
				List<WorldMon> have = lv.getEntitiesOfClass(WorldMon.class, box, w -> k.equals(w.key()));
				for (int i = 1; i < have.size(); i++) have.get(i).discard();
				if (!have.isEmpty()) {
					WorldMon w = have.get(0);
					if (w.distanceToSqr(a[0], a[1], a[2]) > 0.5) w.teleportTo(a[0], a[1], a[2]);
					return;
				}
				Game.placeMon(lv, "poke_ball", k, a[0], a[1], a[2], 0F);
			}
			default -> {
			}
		}
	}

	/** Where a room's own person or ball now stands (room coordinates), if the placer's moved it there; or null. */
	public static double[] roomSpot(String room, String key) {
		Spot s = spot(key);
		if (s == null || s.gone() || s.room() == null || !s.room().equals(room)) return null;
		return new double[] {s.x(), s.y(), s.z(), s.yaw()};
	}

	/**
	 * A in front of nothing in particular: a hidden item there (FireRed's, under a bush or in the ground), found once.
	 * True if there was one.
	 */
	public static boolean hidden(ServerPlayer p, double x, double y, double z) {
		Interiors.Room r = Interiors.roomOf(p);
		PlayerData d = Game.data(p);
		for (Spot s : all()) {
			if (!s.kind().equals("hidden") || s.gone()) continue;
			if (r == null ? s.room() != null : s.room() == null || !s.room().equals(r.id())) continue;
			double[] a = atIn((ServerLevel) p.level(), s);
			if (a == null) continue;
			double dx = a[0] - x, dz = a[2] - z;
			if (dx * dx + dz * dz > 1.6 * 1.6 || Math.abs(a[1] - y) > 2) continue;
			if (d.statics.contains("hidden:" + s.key())) return false;
			Trainers.FieldItem fi = Trainers.FIELD_ITEMS.get(s.key());
			Item it = fi == null ? null : Item.find(fi.item());
			if (it == null) return false;
			d.statics.add("hidden:" + s.key());
			d.give(it.id(), 1);
			Game.sync(p);
			CompoundTag snd = new CompoundTag();
			snd.putString("sound", "item");
			Net.send(p, "sound", snd);
			Game.message(p, List.of(Game.player(p) + " found " + it.name() + "!"));
			return true;
		}
		return false;
	}

	/** The ITEMFINDER: whether something's hidden near you, and which way. */
	public static void itemfinder(ServerPlayer p) {
		Interiors.Room r = Interiors.roomOf(p);
		PlayerData d = Game.data(p);
		double best = 1e9;
		double[] at = null;
		for (Spot s : all()) {
			if (!s.kind().equals("hidden") || s.gone() || d.statics.contains("hidden:" + s.key())) continue;
			if (r == null ? s.room() != null : s.room() == null || !s.room().equals(r.id())) continue;
			double[] a = atIn((ServerLevel) p.level(), s);
			if (a == null) continue;
			double dd = p.distanceToSqr(a[0], a[1], a[2]);
			if (dd < best) {
				best = dd;
				at = a;
			}
		}
		if (at == null || best > 14 * 14) {
			Game.message(p, List.of("...", "Nope! The ITEMFINDER isn't responding."));
			return;
		}
		double dx = at[0] - p.getX(), dz = at[2] - p.getZ();
		String way = Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? "east" : "west") : (dz > 0 ? "south" : "north");
		Game.message(p, List.of("Oh! The ITEMFINDER's responding!", best < 2.5 * 2.5 ? "There's an item buried right here!" : "There's an item buried to the " + way + "!"));
	}
}
