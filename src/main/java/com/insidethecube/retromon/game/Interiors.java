package com.insidethecube.retromon.game;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.insidethecube.retromon.Retromon;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The insides of buildings. John's world is the whole outside; it has no insides. Each inside is built in a
 * space of its own (the retromon:interiors dimension, black all round), from the Interior Viewer's rooms
 * (tools/interior/export: every face of every piece, cut into blocks, as retromon:prop states). Walk into a
 * building's door in the world and you're inside; walk off the door mat and you're back outside that door.
 * Stairs take you up and down; A on something (the TV, a shelf, the fridge) says what it is.
 */
public final class Interiors {
	private Interiors() {}

	public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION, Retromon.id("interiors"));

	public record Warp(String kind, double x0, double x1, double z0, double z1, String door, String to, double sx, double sz, float syaw) {}

	public record Person(String key, String name, String skin, double x, double z, float yaw, List<String> lines) {}

	public record Act(double x0, double z0, double x1, double z1, List<String> lines) {}

	public record Room(String id, int ox, int oy, int oz, double w, double d, double h, String name, String floor, String place, double sx, double sz,
			float syaw, double ex, double ez, float eyaw, double camX, double camZ, double camDist, int[] cells, List<Warp> warps, List<Person> people,
			List<Act> acts) {
		public double cx() {
			return ox + camX;
		}

		public double cz() {
			return oz + camZ;
		}
	}

	public record Door(String key, String room, List<BlockPos> blocks, double x, double y, double z) {}

	private static final JsonObject DATA = load();
	/** How many looks the prop block has (one state each). */
	public static final int STATES = DATA == null ? 1 : DATA.get("states").getAsInt();
	private static final VoxelShape[] SHAPES = new VoxelShape[STATES];
	public static final Map<String, Room> ROOMS = new LinkedHashMap<>();
	public static final Map<String, Door> DOORS = new LinkedHashMap<>();
	private static String startRoom = "house2";
	private static double startX = 4.2, startZ = 9.5;
	private static float startYaw;
	private static String version = "0";

	static {
		if (DATA != null) read();
	}

	private static JsonObject load() {
		try (InputStream in = Interiors.class.getResourceAsStream("/retromon/interiors/rooms.json")) {
			if (in == null) return null;
			return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
		} catch (Exception e) {
			Retromon.LOG.warn("Couldn't read the interiors", e);
			return null;
		}
	}

	private static double num(JsonObject o, String k, double d) {
		return o != null && o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsDouble() : d;
	}

	private static List<String> strings(JsonElement e) {
		List<String> out = new ArrayList<>();
		if (e != null && e.isJsonArray()) for (JsonElement s : e.getAsJsonArray()) out.add(s.getAsString());
		return out;
	}

	private static void read() {
		JsonArray shapes = DATA.getAsJsonArray("shapes");
		for (int i = 0; i < STATES; i++) {
			VoxelShape s = Shapes.empty();
			for (JsonElement b : shapes.get(i).getAsJsonArray()) {
				JsonArray a = b.getAsJsonArray();
				s = Shapes.or(s, Block.box(a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble(), a.get(3).getAsDouble(), a.get(4).getAsDouble(),
						a.get(5).getAsDouble()));
			}
			SHAPES[i] = s.optimize();
		}
		for (Map.Entry<String, JsonElement> e : DATA.getAsJsonObject("rooms").entrySet()) {
			JsonObject r = e.getValue().getAsJsonObject();
			JsonArray o = r.getAsJsonArray("origin");
			JsonObject sp = r.getAsJsonObject("spawn"), en = r.has("entry") && r.get("entry").isJsonObject() ? r.getAsJsonObject("entry") : sp,
					cam = r.getAsJsonObject("cam");
			JsonArray c = r.getAsJsonArray("cells");
			int[] cells = new int[c.size()];
			for (int i = 0; i < cells.length; i++) cells[i] = c.get(i).getAsInt();
			List<Warp> warps = new ArrayList<>();
			for (JsonElement we : r.getAsJsonArray("warps")) {
				JsonObject w = we.getAsJsonObject();
				JsonObject ws = w.has("spawn") && w.get("spawn").isJsonObject() ? w.getAsJsonObject("spawn") : null;
				String kind = w.get("kind").getAsString();
				double x0 = num(w, "x0", 0), x1 = kind.equals("mat") ? num(w, "x1", 0) : x0 + 4;
				double z0 = kind.equals("mat") ? num(w, "z", 0) : num(w, "z0", 0), z1 = kind.equals("mat") ? 999 : num(w, "z1", 0);
				warps.add(new Warp(kind, x0, x1, z0, z1, w.has("door") && !w.get("door").isJsonNull() ? w.get("door").getAsString() : null,
						w.has("to") && !w.get("to").isJsonNull() ? w.get("to").getAsString() : null, num(ws, "x", 0), num(ws, "z", 0), (float) num(ws, "yaw", 0)));
			}
			List<Person> people = new ArrayList<>();
			for (JsonElement pe : r.getAsJsonArray("people")) {
				JsonObject p = pe.getAsJsonObject();
				people.add(new Person(p.get("key").getAsString(), p.get("name").getAsString(), p.get("skin").getAsString(), num(p, "x", 0), num(p, "z", 0),
						(float) num(p, "yaw", 0), strings(p.get("lines"))));
			}
			List<Act> acts = new ArrayList<>();
			for (JsonElement ae : r.getAsJsonArray("acts")) {
				JsonObject a = ae.getAsJsonObject();
				JsonArray rc = a.getAsJsonArray("rect");
				acts.add(new Act(rc.get(0).getAsDouble(), rc.get(1).getAsDouble(), rc.get(2).getAsDouble(), rc.get(3).getAsDouble(), strings(a.get("lines"))));
			}
			Room room = new Room(e.getKey(), o.get(0).getAsInt(), o.get(1).getAsInt(), o.get(2).getAsInt(), num(r, "W", 16), num(r, "D", 14), num(r, "H", 4),
					r.get("name").getAsString(), r.get("floor").getAsString(), r.get("place").getAsString(), num(sp, "x", 0), num(sp, "z", 0),
					(float) num(sp, "yaw", 0), num(en, "x", 0), num(en, "z", 0), (float) num(en, "yaw", 180), num(cam, "x", 8), num(cam, "z", 7),
					num(cam, "dist", 30), cells, warps, people, acts);
			ROOMS.put(room.id(), room);
		}
		for (Map.Entry<String, JsonElement> e : DATA.getAsJsonObject("doors").entrySet()) {
			JsonObject d = e.getValue().getAsJsonObject();
			List<BlockPos> blocks = new ArrayList<>();
			for (JsonElement b : d.getAsJsonArray("door")) {
				JsonArray a = b.getAsJsonArray();
				blocks.add(new BlockPos(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt()));
			}
			JsonArray out = d.getAsJsonArray("out");
			DOORS.put(e.getKey(), new Door(e.getKey(), d.get("room").getAsString(), blocks, out.get(0).getAsDouble(), out.get(1).getAsDouble(),
					out.get(2).getAsDouble()));
		}
		JsonObject st = DATA.getAsJsonObject("start");
		if (st != null) {
			startRoom = st.get("room").getAsString();
			startX = num(st, "x", startX);
			startZ = num(st, "z", startZ);
			startYaw = (float) num(st, "yaw", 0);
		}
		version = DATA.get("version").getAsString() + "-" + STATES + "-" + DATA.toString().length();
		// The people inside, as the game's people (their skin and what they say), unless the games had them already.
		for (Room r : ROOMS.values())
			for (Person p : r.people())
				Trainers.PEOPLE.putIfAbsent(p.key(), new Trainers.Person(p.key(), r.place(), p.skin(), "down", "stay", p.lines()));
	}

	/** A prop state's collision (and outline) shape. */
	public static VoxelShape shape(int state) {
		return state >= 0 && state < SHAPES.length && SHAPES[state] != null ? SHAPES[state] : Shapes.empty();
	}

	// ---------- where you are ----------

	public static boolean in(Entity e) {
		return e != null && e.level() != null && e.level().dimension() == LEVEL;
	}

	/** The room at a spot in the interiors (each has 64 blocks of its own along x). */
	public static Room roomAt(double x) {
		for (Room r : ROOMS.values()) if (x >= r.ox() - 16 && x < r.ox() + 48) return r;
		return null;
	}

	public static Room roomOf(Entity e) {
		return in(e) ? roomAt(e.getX()) : null;
	}

	// ---------- building them ----------

	private static boolean built;

	/** Builds the rooms (once per world, and again when they change). */
	public static ServerLevel ensure(MinecraftServer s) {
		ServerLevel level = s.getLevel(LEVEL);
		if (level == null) {
			Retromon.LOG.warn("Retromon: the interiors dimension isn't loaded (retromon:interiors)");
			return null;
		}
		if (built) return level;
		built = true;
		Path mark = s.getWorldPath(LevelResource.ROOT).resolve("retromon_interiors.txt");
		try {
			if (Files.exists(mark) && Files.readString(mark).trim().equals(version)) return level;
		} catch (Exception ignored) {
			// Built again.
		}
		for (Room r : ROOMS.values()) build(level, r);
		try {
			Files.writeString(mark, version);
		} catch (Exception e) {
			Retromon.LOG.warn("Couldn't note the interiors as built", e);
		}
		Retromon.LOG.info("Retromon: built {} interiors", ROOMS.size());
		return level;
	}

	private static void build(ServerLevel level, Room r) {
		int W = (int) Math.ceil(r.w()), D = (int) Math.ceil(r.d());
		// Every chunk the room and its black ground touch, loaded.
		for (int cx = (r.ox() - 26) >> 4; cx <= (r.ox() + W + 26) >> 4; cx++)
			for (int cz = (r.oz() - 22) >> 4; cz <= (r.oz() + D + 26) >> 4; cz++) level.getChunk(cx, cz);
		BlockState air = Blocks.AIR.defaultBlockState();
		BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
		for (int x = r.ox() - 3; x <= r.ox() + W + 3; x++)
			for (int y = r.oy() - 7; y <= r.oy() + 8; y++)
				for (int z = r.oz() - 3; z <= r.oz() + D + 3; z++) {
					at.set(x, y, z);
					if (!level.getBlockState(at).isAir()) level.setBlock(at, air, Block.UPDATE_CLIENTS);
				}
		// Black ground far below, so all round the room is black (whatever the sky or the shaders do).
		BlockState black = Blocks.BLACK_CONCRETE.defaultBlockState();
		for (int x = r.ox() - 24; x <= r.ox() + W + 24; x++)
			for (int z = r.oz() - 20; z <= r.oz() + D + 24; z++) level.setBlock(at.set(x, r.oy() - 9, z), black, Block.UPDATE_CLIENTS);
		BlockState prop = Retromon.PROP.defaultBlockState();
		int[] c = r.cells();
		for (int i = 0; i + 3 < c.length; i += 4)
			level.setBlock(at.set(r.ox() + c[i], r.oy() + c[i + 1], r.oz() + c[i + 2]), prop.setValue(Prop.M, Math.min(STATES - 1, c[i + 3])), Block.UPDATE_CLIENTS);
		// Light: unseen lamps over the room, so it's bright inside by day and night.
		BlockState light = Blocks.LIGHT.defaultBlockState().setValue(net.minecraft.world.level.block.LightBlock.LEVEL, 15);
		for (int x = r.ox() + 2; x < r.ox() + W; x += 4)
			for (int z = r.oz() + 2; z < r.oz() + D; z += 4) {
				at.set(x, r.oy() + 3, z);
				if (level.getBlockState(at).isAir()) level.setBlock(at, light, Block.UPDATE_CLIENTS);
			}
		// The people inside.
		AABB box = new AABB(r.ox() - 4, r.oy() - 8, r.oz() - 4, r.ox() + W + 4, r.oy() + 8, r.oz() + D + 4);
		for (Npc n : level.getEntitiesOfClass(Npc.class, box, n -> true)) n.discard();
		for (Person p : r.people()) {
			Npc n = Game.place(level, Npc.PERSON, p.key(), r.ox() + p.x(), r.oy(), r.oz() + p.z(), p.yaw());
			if (n != null) n.setCustomName(net.minecraft.network.chat.Component.literal(p.name()));
		}
	}

	// ---------- going in and out ----------

	/** Ticks before each player can go through another door or mat (so you don't bounce straight back). */
	private static final Map<UUID, Integer> COOL = new HashMap<>();
	/** Who's on their way somewhere (after the fade to black): ticks left, and where to. */
	private static final Map<UUID, Integer> PENDING_T = new HashMap<>();
	private static final Map<UUID, Runnable> PENDING = new HashMap<>();
	private static final int FADE_TICKS = 17;
	private static final Map<UUID, Vec3> LAST = new HashMap<>();

	/** A new game: you wake up in your room. */
	public static boolean start(ServerPlayer p) {
		Room r = ROOMS.get(startRoom);
		return r != null && go(p, r, startX, startZ, startYaw, false);
	}

	/** Into a room at a spot in it (room coordinates), facing yaw. With a fade, after the screen's gone black. */
	public static boolean go(ServerPlayer p, Room r, double x, double z, float yaw, boolean fade) {
		MinecraftServer s = p.level().getServer();
		ServerLevel level = ensure(s);
		if (level == null || r == null) return false;
		COOL.put(p.getUUID(), FADE_TICKS + 20);
		Runnable move = () -> {
			p.teleportTo(level, r.ox() + x, r.oy(), r.oz() + z, java.util.Set.of(), yaw, 30F, true);
			p.setDeltaMovement(Vec3.ZERO);
			LAST.remove(p.getUUID());
		};
		if (!fade) {
			move.run();
			return true;
		}
		Net.send(p, "fade", new CompoundTag());
		PENDING_T.put(p.getUUID(), FADE_TICKS);
		PENDING.put(p.getUUID(), move);
		return true;
	}

	/** Out through a building's door: standing just outside it in the world, facing away. */
	public static void out(ServerPlayer p, Door d) {
		MinecraftServer s = p.level().getServer();
		ServerLevel world = s.overworld();
		COOL.put(p.getUUID(), FADE_TICKS + 20);
		Net.send(p, "fade", new CompoundTag());
		PENDING_T.put(p.getUUID(), FADE_TICKS);
		PENDING.put(p.getUUID(), () -> {
			p.teleportTo(world, d.x(), d.y(), d.z(), java.util.Set.of(), 0F, 30F, true);
			p.setDeltaMovement(Vec3.ZERO);
			LAST.remove(p.getUUID());
		});
	}

	public static boolean busy(ServerPlayer p) {
		return PENDING.containsKey(p.getUUID());
	}

	/** Every tick, for each player: the way in at a door, the mats and stairs inside. */
	public static void tick(ServerPlayer p) {
		UUID id = p.getUUID();
		Integer pt = PENDING_T.get(id);
		if (pt != null) {
			if (pt > 1) PENDING_T.put(id, pt - 1);
			else {
				PENDING_T.remove(id);
				Runnable r = PENDING.remove(id);
				if (r != null) r.run();
			}
			return;
		}
		COOL.computeIfPresent(id, (k, v) -> v > 1 ? v - 1 : null);
		Vec3 here = p.position(), last = LAST.put(id, here);
		double dz = last == null ? 0 : here.z - last.z;
		if (COOL.containsKey(id)) return;
		float yaw = Mth.wrapDegrees(p.getYRot());
		boolean facingSouth = Math.abs(yaw) < 55, facingNorth = Math.abs(Mth.wrapDegrees(yaw - 180)) < 55;
		if (in(p)) {
			Room r = roomAt(p.getX());
			if (r == null) return;
			double lx = p.getX() - r.ox(), ly = p.getY() - r.oy(), lz = p.getZ() - r.oz();
			// Fallen out of a room somehow (under a floor): back to where you come in.
			if (ly < -3 && r.warps().stream().noneMatch(w -> w.kind().equals("down"))) {
				go(p, r, r.ex(), r.ez(), r.eyaw(), false);
				return;
			}
			for (Warp w : r.warps()) {
				switch (w.kind()) {
					case "mat" -> {
						if (lz > w.z0() && lx > w.x0() && lx < w.x1() && (dz > 0.01 || facingSouth)) {
							Door d = w.door() == null ? null : DOORS.get(w.door());
							if (d == null) continue;
							if (r.id().equals("lab") && !Game.data(p).started) {
								// The games' way: Oak won't let you go without a Pokémon of your own.
								p.teleportTo(p.getX(), p.getY(), r.oz() + w.z0() - 1.2);
								COOL.put(id, 20);
								Game.message(p, List.of("OAK: Hey! Don't go away yet!"));
								return;
							}
							out(p, d);
							return;
						}
					}
					case "up" -> {
						if (ly >= 1.45 && lx > w.x0() && lz > w.z0() - 0.5 && lz < w.z1() + 0.5) {
							go(p, ROOMS.get(w.to()), w.sx(), w.sz(), w.syaw(), true);
							return;
						}
					}
					case "down" -> {
						if (ly <= -1.45) {
							go(p, ROOMS.get(w.to()), w.sx(), w.sz(), w.syaw(), true);
							return;
						}
					}
					default -> {
					}
				}
			}
			return;
		}
		if (p.level().dimension() != Level.OVERWORLD) return;
		for (Door d : DOORS.values()) {
			double x0 = 1e9, x1 = -1e9, zf = 0, y = 0;
			for (BlockPos b : d.blocks()) {
				x0 = Math.min(x0, b.getX());
				x1 = Math.max(x1, b.getX() + 1);
				zf = b.getZ() + 1;
				y = b.getY();
			}
			// Up against the door's outside (south) face, heading in.
			if (p.getX() > x0 - 0.2 && p.getX() < x1 + 0.2 && p.getZ() > zf - 1.0 && p.getZ() < zf + 0.55 && Math.abs(p.getY() - y) < 1.2
					&& (dz < -0.01 || facingNorth)) {
				Room r = ROOMS.get(d.room());
				if (r != null) go(p, r, r.ex(), r.ez(), r.eyaw(), true);
				return;
			}
		}
	}

	/** Somewhere free to stand near a spot in a room (for people who come in: your rival, say). */
	public static BlockPos spot(ServerLevel level, int x, int z, int radius) {
		Room r = roomAt(x);
		int y = r != null ? r.oy() : 64;
		for (int rad = 0; rad <= radius + 2; rad++)
			for (int dx = -rad; dx <= rad; dx++)
				for (int dz = -rad; dz <= rad; dz++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != rad) continue;
					BlockPos at = new BlockPos(x + dx, y, z + dz);
					if (level.getBlockState(at).getCollisionShape(level, at).isEmpty() && level.getBlockState(at.above()).getCollisionShape(level, at.above()).isEmpty()
							&& !level.getBlockState(at.below()).getCollisionShape(level, at.below()).isEmpty())
						return at;
				}
		return new BlockPos(x, y, z);
	}

	// ---------- looking at things (the client: A with nobody to talk to) ----------

	/** What's straight ahead of you in a room, 0.5 to 2.1 blocks on (as a sign is read): its lines, or null. */
	public static List<String> lookAt(Entity e) {
		Room r = roomOf(e);
		if (r == null) return null;
		Vec3 look = Vec3.directionFromRotation(0, e.getYRot());
		for (double d = 0.5; d <= 2.1 + 1e-6; d += 0.4) {
			double x = e.getX() + look.x * d - r.ox(), z = e.getZ() + look.z * d - r.oz();
			Act best = null;
			double bd = 1e9;
			for (Act a : r.acts()) {
				if (x > a.x0() && x < a.x1() && z > a.z0() && z < a.z1()) {
					double c = Math.hypot((a.x0() + a.x1()) / 2 - x, (a.z0() + a.z1()) / 2 - z);
					if (c < bd) {
						bd = c;
						best = a;
					}
				}
			}
			if (best != null && !best.lines().isEmpty()) return best.lines();
		}
		return null;
	}
}
