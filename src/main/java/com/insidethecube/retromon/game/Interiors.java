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

	/**
	 * A way out of a room. choice: the game asks you where to there (the Route 28 Gate's west way: ROUTE 28, or ROUTE
	 * 26 and JOHTO); needs: what you must have to go through (a badge: the League gate's guard).
	 */
	public record Warp(String kind, double x0, double x1, double z0, double z1, String door, String to, double sx, double sz, float syaw, String choice, String needs,
			double sy) {}

	/** Someone in a room; role: "nurse" (heals), "clerk" (sells) or null (just talks). */
	/**
	 * Someone in a room. group: one of a group's people is in at a time (a Mart's shopper, a different one each time you
	 * come in); from and to: the hours they're about (the Trainer School's, by day), or -1.
	 */
	public record Person(String key, String name, String skin, double x, double z, float yaw, List<String> lines, String role, boolean seated, String group,
			int from, int to, double y) {}

	/** Something to look at in a room; map, a wall map that opens the Town Map. */
	public record Act(double x0, double z0, double x1, double z1, List<String> lines, boolean map, boolean pc, String use) {}

	/** An item ball lying in a room (the forest's): its key in field_items.tsv, and where it lies. */
	public record Ball(String key, double x, double z, double y) {}

	/**
	 * A room. A big one built in Minecraft's own blocks (the forest, from John's schematic) has schem (its blocks, a
	 * resource beside rooms.json), base (how far below the floor the schematic's bottom goes), lights (faint unseen
	 * lamps, for a dark one), tile (how many blocks one of the games' steps is, across and down: its trainers see
	 * that many steps straight ahead) and balls.
	 */
	public record Room(String id, int ox, int oy, int oz, double w, double d, double h, String name, String floor, String place, double sx, double sz,
			float syaw, double ex, double ez, float eyaw, double camX, double camZ, double camDist, int[] cells, List<Warp> warps, List<Person> people,
			List<Act> acts, List<double[]> grass, List<double[]> pools, List<double[]> panes, List<double[]> clocks, String schem, int base, boolean dark,
			double[] tile, List<Ball> balls, List<double[]> lights, double[] haze) {
		/** Big enough to walk about in (the forest): the camera follows you, as it does outside. */
		public boolean big() {
			return w > 26 || d > 26;
		}

		/** How far along x it reaches from its origin (64 blocks for a building's, more for the forest). */
		public double reach() {
			return Math.max(48, w + 16);
		}

		public double cx() {
			return ox + camX;
		}

		public double cz() {
			return oz + camZ;
		}
	}

	/**
	 * A door in the world into a room: its blocks, where you come out (and facing), which way it faces (s, n, e or w:
	 * the side you walk up to it from) and, for a room with more than one way in, where you arrive (x, z, yaw), else null.
	 */
	public record Door(String key, String room, List<BlockPos> blocks, double x, double y, double z, boolean north, float yaw, String face, double[] in,
			String place, String group) {}

	/**
	 * A room's battle arena (Let's Go's: a battle indoors is fought in a round hall built from that room's own
	 * things): its blocks from its corner, as rooms have theirs. The battle scene puts it up over you for the battle.
	 */
	public record Arena(double w, double d, double h, int[] cells, List<double[]> panes) {}

	public static final Map<String, Arena> ARENAS = new LinkedHashMap<>();

	/**
	 * The rooms built in Minecraft's own blocks, each a file of its own beside rooms.json (tools/forest.py). (Before DATA:
	 * the rooms are read as it's loaded.)
	 */
	private static final String[] BLOCK_ROOMS = {"/retromon/interiors/viridian_forest.json", "/retromon/interiors/digletts_cave.json",
			"/retromon/interiors/mt_moon.json", "/retromon/interiors/safari_zone.json", "/retromon/interiors/seafoam_islands.json",
			"/retromon/interiors/rock_tunnel.json"};

	private static final JsonObject DATA = load();
	/** How many looks the prop block has (one state each). */
	public static final int STATES = DATA == null ? 1 : DATA.get("states").getAsInt();
	private static final VoxelShape[] SHAPES = new VoxelShape[STATES];
	public static final Map<String, Room> ROOMS = new LinkedHashMap<>();
	public static final Map<String, Door> DOORS = new LinkedHashMap<>();
	/** POKéMON standing in a room (the Power Plant's ZAPDOS, its ELECTRODE posing as items): a static's key, its look. */
	public record MonSpot(String key, String species, double x, double z, float yaw) {}
	public static final Map<String, List<MonSpot>> MONS = new HashMap<>();
	private static String startRoom = "house2";
	private static double startX = 3.6, startZ = 6.4;
	private static float startYaw;
	private static String version = "0";

	static {
		if (DATA != null) read();
	}

	private static JsonObject load() {
		return json("/retromon/interiors/rooms.json");
	}

	private static JsonObject json(String path) {
		try (InputStream in = Interiors.class.getResourceAsStream(path)) {
			if (in == null) return null;
			return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
		} catch (Exception e) {
			Retromon.LOG.warn("Couldn't read the interiors ({})", path, e);
			return null;
		}
	}

	/**
	 * A list of rows of numbers (a room's pools of light through its windows, and its windows' glass: the client's
	 * RoomLightRenderer draws them by the time of day).
	 */
	private static List<double[]> rows(JsonObject o, String k) {
		List<double[]> out = new ArrayList<>();
		if (o.has(k)) for (JsonElement e : o.getAsJsonArray(k)) {
			JsonArray a = e.getAsJsonArray();
			double[] v = new double[a.size()];
			for (int i = 0; i < v.length; i++) v[i] = a.get(i).getAsDouble();
			out.add(v);
		}
		return out;
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
		for (Map.Entry<String, JsonElement> e : DATA.getAsJsonObject("rooms").entrySet()) room(e.getKey(), e.getValue().getAsJsonObject());
		StringBuilder more = new StringBuilder();
		for (String path : BLOCK_ROOMS) {
			JsonObject extra = json(path);
			if (extra == null) continue;
			for (Map.Entry<String, JsonElement> e : extra.getAsJsonObject("rooms").entrySet()) room(e.getKey(), e.getValue().getAsJsonObject());
			more.append('-').append(extra.toString().length());
		}
		readRest(more.toString());
	}

	/** A room's dust in the air (the cave's): its colour, 0 to 255, and how thick, 0 to 1; or null. */
	private static double[] haze(JsonObject r) {
		if (!r.has("haze") || !r.get("haze").isJsonArray()) return null;
		JsonArray a = r.getAsJsonArray("haze");
		return new double[] {a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble(), a.get(3).getAsDouble()};
	}

	/** One room from its JSON (rooms.json's, or a file of its own). */
	private static void room(String id, JsonObject r) {
		{
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
				boolean north = kind.equals("north");
				// (A ladder or stairs up: across x from x0, to x1 where the room gives one; the cave has two ladders.)
				double x0 = num(w, "x0", 0), x1 = kind.equals("mat") || north ? num(w, "x1", 0) : num(w, "x1", 999);
				// (A way north in a room you walk about in, the SAFARI ZONE's houses: only from just in front of it, its z0.)
				double z0 = kind.equals("mat") ? num(w, "z", 0) : north ? (w.has("z0") ? num(w, "z0", 0) : -999) : num(w, "z0", 0),
						z1 = kind.equals("mat") ? 999 : north ? num(w, "z", 1) : num(w, "z1", 0);
				// Through the east wall (past x, heading east) or the west (short of x, heading west).
				if (kind.equals("east")) {
					x0 = num(w, "x", 0);
					x1 = 999;
				} else if (kind.equals("west")) {
					x0 = -999;
					x1 = num(w, "x", 1);
				}
				warps.add(new Warp(kind, x0, x1, z0, z1, w.has("door") && !w.get("door").isJsonNull() ? w.get("door").getAsString() : null,
						w.has("to") && !w.get("to").isJsonNull() ? w.get("to").getAsString() : null, num(ws, "x", 0), num(ws, "z", 0), (float) num(ws, "yaw", 0),
						w.has("choice") && !w.get("choice").isJsonNull() ? w.get("choice").getAsString() : null,
						w.has("needs") && !w.get("needs").isJsonNull() ? w.get("needs").getAsString() : null, num(ws, "y", 0)));
			}
			List<Person> people = new ArrayList<>();
			for (JsonElement pe : r.getAsJsonArray("people")) {
				JsonObject p = pe.getAsJsonObject();
				people.add(new Person(p.get("key").getAsString(), p.get("name").getAsString(), p.get("skin").getAsString(), num(p, "x", 0), num(p, "z", 0),
						(float) num(p, "yaw", 0), strings(p.get("lines")), p.has("role") && !p.get("role").isJsonNull() ? p.get("role").getAsString() : null,
						p.has("seated") && p.get("seated").getAsBoolean(), p.has("group") && !p.get("group").isJsonNull() ? p.get("group").getAsString() : null,
						p.has("hours") ? p.getAsJsonArray("hours").get(0).getAsInt() : -1, p.has("hours") ? p.getAsJsonArray("hours").get(1).getAsInt() : -1,
						num(p, "y", 0)));
			}
			List<Act> acts = new ArrayList<>();
			for (JsonElement ae : r.getAsJsonArray("acts")) {
				JsonObject a = ae.getAsJsonObject();
				JsonArray rc = a.getAsJsonArray("rect");
				acts.add(new Act(rc.get(0).getAsDouble(), rc.get(1).getAsDouble(), rc.get(2).getAsDouble(), rc.get(3).getAsDouble(), strings(a.get("lines")),
						a.has("map") && a.get("map").getAsBoolean(), a.has("pc") && a.get("pc").getAsBoolean(), a.has("use") ? a.get("use").getAsString() : null));
			}
			List<double[]> grass = new ArrayList<>();
			if (r.has("grass")) for (JsonElement ge : r.getAsJsonArray("grass")) {
				JsonArray g = ge.getAsJsonArray();
				grass.add(new double[] {g.get(0).getAsDouble(), g.get(1).getAsDouble(), g.get(2).getAsDouble(), g.get(3).getAsDouble()});
			}
			List<Ball> balls = new ArrayList<>();
			if (r.has("items")) for (JsonElement be : r.getAsJsonArray("items")) {
				JsonObject b = be.getAsJsonObject();
				balls.add(new Ball(b.get("key").getAsString(), num(b, "x", 0), num(b, "z", 0), num(b, "y", 0)));
			}
			double[] tile = null;
			if (r.has("tile") && r.get("tile").isJsonArray()) tile = new double[] {r.getAsJsonArray("tile").get(0).getAsDouble(), r.getAsJsonArray("tile").get(1).getAsDouble()};
			Room room = new Room(id, o.get(0).getAsInt(), o.get(1).getAsInt(), o.get(2).getAsInt(), num(r, "W", 16), num(r, "D", 14), num(r, "H", 4),
					r.get("name").getAsString(), r.get("floor").getAsString(), r.get("place").getAsString(), num(sp, "x", 0), num(sp, "z", 0),
					(float) num(sp, "yaw", 0), num(en, "x", 0), num(en, "z", 0), (float) num(en, "yaw", 180), num(cam, "x", 8), num(cam, "z", 7),
					num(cam, "dist", 30), cells, warps, people, acts, grass, rows(r, "pools"), rows(r, "panes"), rows(r, "clocks"),
					r.has("schem") && !r.get("schem").isJsonNull() ? r.get("schem").getAsString() : null, (int) num(r, "base", 0),
					r.has("dark") && r.get("dark").getAsBoolean(), tile, balls, rows(r, "lights"), haze(r));
			ROOMS.put(room.id(), room);
			if (r.has("mons")) {
				List<MonSpot> ms = new ArrayList<>();
				for (JsonElement me : r.getAsJsonArray("mons")) {
					JsonObject m = me.getAsJsonObject();
					ms.add(new MonSpot(m.get("key").getAsString(), m.get("species").getAsString(), num(m, "x", 0), num(m, "z", 0), (float) num(m, "yaw", 0)));
				}
				MONS.put(room.id(), ms);
			}
		}
	}

	private static void readRest(String more) {
		if (DATA.has("arenas")) for (Map.Entry<String, JsonElement> e : DATA.getAsJsonObject("arenas").entrySet()) {
			JsonObject a = e.getValue().getAsJsonObject();
			JsonArray c = a.getAsJsonArray("cells");
			int[] cells = new int[c.size()];
			for (int i = 0; i < cells.length; i++) cells[i] = c.get(i).getAsInt();
			ARENAS.put(e.getKey(), new Arena(num(a, "W", 24), num(a, "D", 24), num(a, "H", 7), cells, rows(a, "panes")));
		}
		for (Map.Entry<String, JsonElement> e : DATA.getAsJsonObject("doors").entrySet()) door(e.getKey(), e.getValue().getAsJsonObject());
		for (String path : BLOCK_ROOMS) {
			JsonObject extra = json(path);
			if (extra != null && extra.has("doors")) for (Map.Entry<String, JsonElement> e : extra.getAsJsonObject("doors").entrySet())
				door(e.getKey(), e.getValue().getAsJsonObject());
		}
		readStart(more);
	}

	/** A door in John's world into a room (rooms.json's, or a block room's file's). */
	private static void door(String key, JsonObject d) {
		{
			List<BlockPos> blocks = new ArrayList<>();
			for (JsonElement b : d.getAsJsonArray("door")) {
				JsonArray a = b.getAsJsonArray();
				blocks.add(new BlockPos(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt()));
			}
			JsonArray out = d.getAsJsonArray("out");
			String face = d.has("face") ? d.get("face").getAsString() : "s";
			boolean north = face.equals("n");
			JsonObject in = d.has("in") && d.get("in").isJsonObject() ? d.getAsJsonObject("in") : null;
			DOORS.put(key, new Door(key, d.get("room").getAsString(), blocks, out.get(0).getAsDouble(), out.get(1).getAsDouble(),
					out.get(2).getAsDouble(), north, (float) num(d, "yaw", north ? 180 : face.equals("e") ? -90 : face.equals("w") ? 90 : 0), face,
					in == null ? null : new double[] {num(in, "x", 0), num(in, "z", 0), num(in, "yaw", 0)}, d.has("place") ? d.get("place").getAsString() : null,
					d.has("group") ? d.get("group").getAsString() : null));
		}
	}

	private static void readStart(String more) {
		JsonObject st = DATA.getAsJsonObject("start");
		if (st != null) {
			startRoom = st.get("room").getAsString();
			startX = num(st, "x", startX);
			startZ = num(st, "z", startZ);
			startYaw = (float) num(st, "yaw", 0);
		}
		version = DATA.get("version").getAsString() + "-" + STATES + "-" + DATA.toString().length() + more;
		// The people inside, as the game's people (their skin and what they say), unless the games had them already.
		for (Room r : ROOMS.values())
			for (Person p : r.people()) {
				if (p.role() == null) Trainers.PEOPLE.putIfAbsent(p.key(), new Trainers.Person(p.key(), r.place(), p.skin(), "down", "stay", p.lines()));
				// The lab's aides on their feet potter about between the shelves (the one at his desk sits; the one
				// at the big machine keeps at it).
				// (Under the games' own place for them, the lab, not the town round it: else the town put one outside too.)
				if ("AIDE".equals(p.name()) && !p.seated() && !p.key().equals("oaks_lab_p3")) {
					Trainers.Person old = Trainers.PEOPLE.get(p.key());
					Trainers.PEOPLE.put(p.key(), new Trainers.Person(p.key(), old != null ? old.place() : "oaks_lab", p.skin(), "any_dir", "walk", old != null ? old.lines() : p.lines()));
				}
			}
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
		for (Room r : ROOMS.values()) if (x >= r.ox() - 16 && x < r.ox() + r.reach()) return r;
		return null;
	}

	/** Whether someone's in a room big enough to walk about in (the forest): its grass, its trainers' eyes, as outside. */
	/** A room by its id (the SAFARI ZONE's gate, to be put back in when the game's over). */
	public static Room room(String id) {
		return ROOMS.get(id);
	}

	public static boolean walkabout(Entity e) {
		Room r = roomOf(e);
		return r != null && r.big();
	}

	/** The dust in the air in the room someone's in (Diglett's Cave): colour and thickness, or null. */
	public static double[] hazeOf(Entity e) {
		Room r = roomOf(e);
		return r == null ? null : r.haze();
	}

	/** How many blocks one of the games' steps is in the room someone's in, across (x) and down (z); or null. */
	public static double[] tileOf(Entity e) {
		Room r = roomOf(e);
		return r == null ? null : r.tile();
	}

	/** Whether the room someone's in has trainers who watch for you (the Pewter Gym's CAMPER): their eyes apply in there. */
	public static boolean watched(Entity e) {
		Room r = roomOf(e);
		if (r == null) return false;
		for (Person p : r.people()) if ("trainer".equals(p.role())) return true;
		return false;
	}

	/** Where someone is in the room they're in (room coordinates: x, height above its floor, z), or null outside. */
	public static Vec3 local(Entity e) {
		Room r = roomOf(e);
		return r == null ? null : new Vec3(e.getX() - r.ox(), e.getY() - r.oy(), e.getZ() - r.oz());
	}

	/** Whether someone's in a room that's always night (the forest). */
	public static boolean alwaysNight(Entity e) {
		Room r = roomOf(e);
		return r != null && r.dark();
	}

	/** The box round a room, its black ground and all (everyone in it, and what's built). */
	private static AABB around(Room r) {
		return new AABB(r.ox() - 8, r.oy() - 8, r.oz() - 8, r.ox() + Math.max(40, r.w() + 8), r.oy() + Math.max(8, r.h() + 2), r.oz() + Math.max(40, r.d() + 8));
	}

	public static Room roomOf(Entity e) {
		return in(e) ? roomAt(e.getX()) : null;
	}

	/** Just inside the way in of the room someone's in (where you stand coming through the door), or null outside. */
	public static Vec3 entryOf(Entity e) {
		Room r = roomOf(e);
		return r == null ? null : new Vec3(r.ox() + r.ex(), r.oy(), r.oz() + r.ez());
	}

	/** Out through the way in: a step past the entry, onto the mat (where walking takes you out). */
	public static Vec3 exitOf(Entity e) {
		Room r = roomOf(e);
		if (r == null) return null;
		for (Warp w : r.warps())
			if (w.kind().equals("mat")) return new Vec3(r.ox() + (w.x0() + w.x1()) / 2, r.oy(), r.oz() + w.z0() - 0.2);
		return new Vec3(r.ox() + r.ex(), r.oy(), r.oz() + r.ez() + 1.5);
	}

	// ---------- building them ----------

	/**
	 * The world (its server) the rooms were last checked for. Per world, not once per launch: a second game started
	 * after leaving the first (without restarting Minecraft) had no rooms at all, so you fell into the black.
	 */
	private static MinecraftServer builtFor;

	/** A new world's started: its rooms are checked again, and nobody's left half way through a door. */
	public static void serverStarted(MinecraftServer s) {
		builtFor = null;
		COOL.clear();
		PENDING_T.clear();
		PENDING.clear();
		LAST.clear();
	}

	/** Builds the rooms (once per world, and again when they change, or when one's missing). */
	public static ServerLevel ensure(MinecraftServer s) {
		ServerLevel level = s.getLevel(LEVEL);
		if (level == null) {
			Retromon.LOG.warn("Retromon: the interiors dimension isn't loaded (retromon:interiors)");
			return null;
		}
		if (builtFor == s) return level;
		builtFor = s;
		// ((0.57, 15:46: "probably a 30-second delay" at the lab's door) Each room notes what it was built as, and only
		// the one you're going into is built again when it's changed: the first door after an update had every room
		// rebuilt at once (the forest, the caves, every house), all while you waited.)
		BUILT.clear();
		TRIED.clear();
		markFile = s.getWorldPath(LevelResource.ROOT).resolve("retromon_interiors.txt");
		try {
			if (Files.exists(markFile))
				for (String line : Files.readAllLines(markFile, StandardCharsets.UTF_8)) {
					int eq = line.indexOf('=');
					if (eq > 0) BUILT.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
				}
		} catch (Exception ignored) {
			// Each room built again as it's gone into.
		}
		Retromon.LOG.info("Retromon: interiors {} ({} rooms, {} noted as built)", version, ROOMS.size(), BUILT.size());
		return level;
	}

	/** What each room was last built as (its id: its signature), and the file that's kept in. */
	private static final Map<String, String> BUILT = new HashMap<>();
	private static Path markFile;

	private static final Map<String, String> SIGS = new HashMap<>();
	/** The rooms made ready this game from where you stood (each tried once, so a room that won't build isn't tried every tick). */
	private static final java.util.Set<String> TRIED = new java.util.HashSet<>();

	/** A room's signature: what goes into building it (its blocks, its people and balls, its lights), so a change shows. */
	private static String signature(Room r) {
		return SIGS.computeIfAbsent(r.id(), k -> makeSignature(r));
	}

	private static String makeSignature(Room r) {
		StringBuilder b = new StringBuilder();
		b.append(STATES).append('|').append(java.util.Arrays.hashCode(r.cells())).append('|').append(r.schem()).append('|').append(r.base()).append('|')
				.append(r.dark()).append('|').append(r.people()).append('|').append(r.balls()).append('|').append(MONS.getOrDefault(r.id(), List.of())).append('|');
		for (double[] l : r.lights()) b.append(java.util.Arrays.toString(l));
		return Integer.toHexString(b.toString().hashCode()) + "-" + Math.round(r.w()) + "x" + Math.round(r.d());
	}

	/** Whether a room's been built as it is now (by the note: nothing's looked at in the world). */
	private static boolean upToDate(Room r) {
		return signature(r).equals(BUILT.get(r.id()));
	}

	/**
	 * The room at a spot in the interiors made ready (built again if it's changed): blacking out into a Center, or a
	 * game saved inside a room and loaded after an update.
	 */
	public static void readyAt(ServerLevel level, double x) {
		Room r = roomAt(x);
		if (r != null && !upToDate(r)) ready(level, r);
	}

	/** Builds a room if it's changed since it was built (or isn't there), and notes it. */
	private static void ready(ServerLevel level, Room r) {
		String sig = signature(r);
		if (sig.equals(BUILT.get(r.id())) && !missing(level, r)) return;
		long t0 = System.nanoTime();
		if (!build(level, r)) return;
		BUILT.put(r.id(), sig);
		Retromon.LOG.info("Retromon: {} built ({} ms)", r.id(), (System.nanoTime() - t0) / 1_000_000);
		if (markFile == null) return;
		StringBuilder out = new StringBuilder();
		for (Map.Entry<String, String> e : new java.util.TreeMap<>(BUILT).entrySet()) out.append(e.getKey()).append('=').append(e.getValue()).append('\n');
		try {
			Files.writeString(markFile, out.toString(), StandardCharsets.UTF_8);
		} catch (Exception e) {
			Retromon.LOG.warn("Couldn't note the interiors as built", e);
		}
	}

	/** Whether a room isn't there (its first, middle and last pieces aren't the room's blocks). */
	private static boolean missing(ServerLevel level, Room r) {
		// (A room wholly of blocks, the forest and the caves; one with props and a few blocks of its own, Cerulean Gym's
		// pool, is checked by its props.)
		if (r.schem() != null && r.cells().length == 0) {
			// The forest: its ground (the schematic's second layer, under the floor, is ground all over).
			int y = r.oy() - 1;
			for (int[] xz : new int[][] {{1, 1}, {(int) r.w() / 2, (int) r.d() / 2}, {(int) r.w() - 2, (int) r.d() - 2}}) {
				BlockPos at = new BlockPos(r.ox() + xz[0], y, r.oz() + xz[1]);
				if (!level.hasChunkAt(at)) level.getChunk(at);
				if (level.getBlockState(at).isAir()) return true;
			}
			return false;
		}
		int[] c = r.cells();
		int n = c.length / 4;
		if (n == 0) return false;
		BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
		for (int i : new int[] {0, n / 2, n - 1}) {
			at.set(r.ox() + c[i * 4], r.oy() + c[i * 4 + 1], r.oz() + c[i * 4 + 2]);
			// (Where a room's own blocks are laid over its props, Cerulean Gym's pool, any block there will do.)
			if (!level.getBlockState(at).is(Retromon.PROP) && (r.schem() == null || level.getBlockState(at).isAir())) return true;
		}
		return false;
	}

	/** Builds one room; a problem with one is noted and the rest still get built. */
	private static boolean build(ServerLevel level, Room r) {
		try {
			buildRoom(level, r);
			return true;
		} catch (Exception e) {
			Retromon.LOG.error("Retromon: couldn't build the room {}", r.id(), e);
			return false;
		}
	}

	private static void buildRoom(ServerLevel level, Room r) {
		int W = (int) Math.ceil(r.w()), D = (int) Math.ceil(r.d());
		// Every chunk the room and its black ground touch, loaded.
		for (int cx = (r.ox() - 26) >> 4; cx <= (r.ox() + Math.max(W, 40) + 26) >> 4; cx++)
			for (int cz = (r.oz() - 22) >> 4; cz <= (r.oz() + Math.max(D, 40) + 26) >> 4; cz++) level.getChunk(cx, cz);
		BlockState air = Blocks.AIR.defaultBlockState();
		BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
		// (Cleared well past the room, so a bigger room it was before leaves nothing behind.)
		int xEnd = r.ox() + Math.max(40, W + 8), zEnd = r.oz() + Math.max(40, D + 8), yEnd = r.oy() + Math.max(8, (int) Math.ceil(r.h()) + 2);
		for (int x = r.ox() - 8; x <= xEnd; x++)
			for (int y = r.oy() - 7; y <= yEnd; y++)
				for (int z = r.oz() - 8; z <= zEnd; z++) {
					at.set(x, y, z);
					if (!level.getBlockState(at).isAir()) level.setBlock(at, air, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
				}
		// Black ground far below, so all round the room is black (whatever the sky or the shaders do).
		BlockState black = Blocks.BLACK_CONCRETE.defaultBlockState();
		for (int x = r.ox() - 24; x <= r.ox() + W + 24; x++)
			for (int z = r.oz() - 20; z <= r.oz() + D + 24; z++) level.setBlock(at.set(x, r.oy() - 9, z), black, Block.UPDATE_CLIENTS);
		BlockState prop = Retromon.PROP.defaultBlockState();
		int[] c = r.cells();
		for (int i = 0; i + 3 < c.length; i += 4)
			level.setBlock(at.set(r.ox() + c[i], r.oy() + c[i + 1], r.oz() + c[i + 2]), prop.setValue(Prop.M, Math.min(STATES - 1, c[i + 3])), Block.UPDATE_CLIENTS);
		// The forest: John's own blocks, as he built them (tools/forest.py), the floor where you stand.
		if (r.schem() != null) placeBlocks(level, r);
		if (r.dark()) {
			// Always night in there: no lamps, only faint unseen ones over the open ground (moonlight you can see by).
			BlockState moon = Blocks.LIGHT.defaultBlockState().setValue(net.minecraft.world.level.block.LightBlock.LEVEL, MOONLIGHT);
			for (double[] l : r.lights()) {
				at.set(r.ox() + (int) l[0], r.oy() + 1 + (l.length > 3 ? (int) l[3] : 0), r.oz() + (int) l[1]);
				BlockState lamp = l.length > 2 ? Blocks.LIGHT.defaultBlockState().setValue(net.minecraft.world.level.block.LightBlock.LEVEL, (int) l[2]) : moon;
				if (level.getBlockState(at).isAir()) level.setBlock(at, lamp, Block.UPDATE_CLIENTS);
			}
		} else {
			// Light: unseen lamps over the room, so it's bright inside by day and night.
			BlockState light = Blocks.LIGHT.defaultBlockState().setValue(net.minecraft.world.level.block.LightBlock.LEVEL, 15);
			for (int x = r.ox() + 2; x < r.ox() + W; x += 4)
				for (int z = r.oz() + 2; z < r.oz() + D; z += 4) {
					at.set(x, r.oy() + 3, z);
					if (level.getBlockState(at).isAir()) level.setBlock(at, light, Block.UPDATE_CLIENTS);
				}
		}
		// The people inside, and the item balls nobody's picked up.
		AABB box = around(r);
		for (Npc n : level.getEntitiesOfClass(Npc.class, box, n -> true)) n.discard();
		for (WorldMon w : level.getEntitiesOfClass(WorldMon.class, box, w -> w.key().startsWith("item:"))) w.discard();
		for (Person p : r.people()) if (wanted(level, r, p)) put(level, r, p);
		for (Ball b : r.balls()) if (!Game.takenByAnyone("item:" + b.key())) ball(level, r, b);
		List<MonSpot> mons = MONS.getOrDefault(r.id(), List.of());
		if (!mons.isEmpty()) {
			java.util.Set<String> keys = new java.util.HashSet<>();
			for (MonSpot m : mons) keys.add(m.key());
			for (WorldMon w : level.getEntitiesOfClass(WorldMon.class, box, w -> keys.contains(w.key()))) w.discard();
			for (MonSpot m : mons) if (!Game.takenByAnyone(m.key())) Game.placeMon(level, m.species(), m.key(), r.ox() + m.x(), r.oy(), r.oz() + m.z(), m.yaw());
		}
	}

	/** How bright the forest's moonlight is (a light level). */
	private static final int MOONLIGHT = 6;

	private static void ball(ServerLevel level, Room r, Ball b) {
		Game.placeMon(level, "poke_ball", "item:" + b.key(), r.ox() + b.x(), r.oy() + b.y(), r.oz() + b.z(), 0F);
	}

	/**
	 * Puts a room's own blocks in place (the forest's, from its .bin: "RMF1", the size, the palette of block states,
	 * then runs of (how many, which) over y, then z, then x), the schematic's bottom base blocks below the floor.
	 */
	private static void placeBlocks(ServerLevel level, Room r) {
		try (InputStream raw = Interiors.class.getResourceAsStream("/retromon/interiors/" + r.schem())) {
			if (raw == null) {
				Retromon.LOG.warn("Retromon: {} has no blocks ({})", r.id(), r.schem());
				return;
			}
			java.io.DataInputStream in = new java.io.DataInputStream(new java.io.BufferedInputStream(new java.util.zip.GZIPInputStream(raw)));
			byte[] magic = new byte[4];
			in.readFully(magic);
			if (!new String(magic, StandardCharsets.US_ASCII).equals("RMF1")) throw new IllegalStateException("not a room's blocks: " + r.schem());
			int w = in.readShort(), h = in.readShort(), l = in.readShort(), n = in.readShort();
			BlockState[] palette = new BlockState[n];
			for (int i = 0; i < n; i++) {
				byte[] b = new byte[in.readShort()];
				in.readFully(b);
				palette[i] = state(new String(b, StandardCharsets.UTF_8));
			}
			BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
			long total = (long) w * h * l, i = 0;
			int placed = 0;
			while (i < total) {
				int count = varint(in), which = varint(in);
				BlockState s = palette[which];
				for (int k = 0; k < count; k++, i++) {
					if (s == null || s.isAir()) continue;
					int x = (int) (i % w), z = (int) ((i / w) % l), y = (int) (i / ((long) w * l));
					level.setBlock(at.set(r.ox() + x, r.oy() + r.base() + y, r.oz() + z), s, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
					placed++;
				}
			}
			Retromon.LOG.info("Retromon: {} built ({} blocks)", r.id(), placed);
		} catch (Exception e) {
			Retromon.LOG.error("Retromon: couldn't put {}'s blocks in place", r.id(), e);
		}
	}

	private static int varint(java.io.DataInputStream in) throws java.io.IOException {
		int v = 0, s = 0;
		while (true) {
			int b = in.readUnsignedByte();
			v |= (b & 0x7F) << s;
			if ((b & 0x80) == 0) return v;
			s += 7;
		}
	}

	/** A block state from its name ("minecraft:birch_leaves[distance=1,persistent=true]"); air for one that doesn't exist. */
	private static BlockState state(String text) {
		int br = text.indexOf('[');
		String name = br < 0 ? text : text.substring(0, br);
		Block block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getOptional(net.minecraft.resources.Identifier.parse(name)).orElse(Blocks.AIR);
		BlockState s = block.defaultBlockState();
		if (br >= 0 && text.endsWith("]")) {
			for (String kv : text.substring(br + 1, text.length() - 1).split(",")) {
				int eq = kv.indexOf('=');
				if (eq < 0) continue;
				net.minecraft.world.level.block.state.properties.Property<?> prop = block.getStateDefinition().getProperty(kv.substring(0, eq).trim());
				if (prop != null) s = with(s, prop, kv.substring(eq + 1).trim());
			}
		}
		return s;
	}

	private static <T extends Comparable<T>> BlockState with(BlockState s, net.minecraft.world.level.block.state.properties.Property<T> prop, String value) {
		return prop.getValue(value).map(v -> s.setValue(prop, v)).orElse(s);
	}

	/** Puts someone in a room in their spot. */
	private static void put(ServerLevel level, Room r, Person p) {
		// The nurse heals and the clerk sells, as the games' nurse and clerk do (their own ways of talking).
		int role = "nurse".equals(p.role()) ? Npc.NURSE : "clerk".equals(p.role()) ? Npc.CLERK : "trainer".equals(p.role()) ? Npc.TRAINER
				: "leader".equals(p.role()) ? Npc.LEADER : Npc.PERSON;
		try {
			Npc n = Game.place(level, role, p.key(), r.ox() + p.x(), r.oy() + p.y(), r.oz() + p.z(), p.yaw());
			if (n != null) {
				n.setCustomName(net.minecraft.network.chat.Component.literal(p.name()));
				if (p.seated()) n.setSeated(true);
			}
		} catch (Exception e) {
			Retromon.LOG.error("Retromon: couldn't place {} in {}", p.key(), r.id(), e);
		}
	}

	/** Where the Center's visitors stand: the room's own two, and one more by the tables (Mt. Moon's MAGIKARP man). */
	private static final double[][] CENTER_EXTRA = {{11.6, 6.2, 270}};
	/** The Centers whose people go by another name than their town's (Mt. Moon's is on Route 4, Rock Tunnel's on Route 10). */
	private static final Map<String, String> CENTER_OF = Map.of("route_4", "mt_moon", "route_10", "rock_tunnel");

	private static void centerVisitors(ServerLevel level, Room r, ServerPlayer comer) {
		String place = placeFor(comer, r);
		if (place == null) place = r.place();
		String town = CENTER_OF.getOrDefault(place, place.replaceAll("_(city|town|island)$", ""));
		List<Trainers.Person> visitors = Trainers.peopleIn(town + "_pokecenter");
		List<double[]> slots = new ArrayList<>();
		for (Person p : r.people()) if (p.role() == null) slots.add(new double[] {p.x(), p.z(), p.yaw()});
		slots.addAll(List.of(CENTER_EXTRA));
		AABB box = around(r);
		for (Npc n : level.getEntitiesOfClass(Npc.class, box, n -> n.role() == Npc.PERSON)) n.discard();
		for (WorldMon w : level.getEntitiesOfClass(WorldMon.class, box, w -> w.key().startsWith("pet:"))) w.discard();
		if (visitors.isEmpty()) {
			for (Person p : r.people()) if (p.role() == null) put(level, r, p);
			return;
		}
		for (int i = 0; i < visitors.size() && i < slots.size(); i++) {
			Trainers.Person v = visitors.get(i);
			double[] at = slots.get(i);
			if (v.species() != null) Game.placeMon(level, v.species(), "pet:" + v.key(), r.ox() + at[0], r.oy(), r.oz() + at[1], (float) at[2]);
			else Game.place(level, Npc.PERSON, v.key(), r.ox() + at[0], r.oy(), r.oz() + at[1], (float) at[2]);
		}
	}

	/** Which of each room's groups is in (by room and group): the key of the one about. */
	private static final Map<String, String> CHOSEN = new HashMap<>();

	/** Whether someone's about in a room now: in their hours, and the one of their group who's in. */
	private static boolean wanted(ServerLevel level, Room r, Person p) {
		if (p.from() >= 0) {
			int h = (int) Math.floorMod(level.getServer().overworld().getDayTime() / 1000L + 6, 24L);
			boolean open = p.from() <= p.to() ? h >= p.from() && h < p.to() : h >= p.from() || h < p.to();
			if (!open) return false;
		}
		if (p.group() == null) return true;
		String k = CHOSEN.get(r.id() + "/" + p.group());
		if (k == null) {
			for (Person o : r.people()) if (p.group().equals(o.group())) {
				k = o.key();
				break;
			}
			CHOSEN.put(r.id() + "/" + p.group(), k);
		}
		return p.key().equals(k);
	}

	/**
	 * Someone's come into a room: who's about is settled again (the hours), and, with nobody else in there, a group
	 * (a Mart's shopper) sends in a different one of its people.
	 */
	public static void people(ServerLevel level, Room r, ServerPlayer comer) {
		if (r == null) return;
		boolean alone = level.players().stream().noneMatch(o -> o != comer && r.equals(roomOf(o)));
		// The POKéMON CENTER every town shares: that town's own people in it (14:18), the nurse the same.
		if (alone && r.id().equals("center")) centerVisitors(level, r, comer);
		// The DAY CARE: yours (if you've left one) in the pen with the others.
		if (r.id().equals("daycare")) Game.daycarePen(comer);
		// LT.SURGE's GYM: its electric door, shut or open for you.
		if (r.id().equals("vermilion_gym")) Game.surgeEntered(comer);
		// Mt. Moon's mushrooms (B1F) grow back: one you picked on an earlier day is there again (13:56).
		if (!r.balls().isEmpty()) {
			PlayerData d = Game.data(comer);
			long today = level.getServer().overworld().getDayTime() / 24000L;
			for (Ball b : r.balls()) {
				if (!(b.key().contains("mushroom") || b.key().endsWith("_again")) || !d.statics.contains("item:" + b.key())) continue;
				String picked = null;
				for (String st : d.statics) if (st.startsWith("picked:" + b.key() + "=")) picked = st;
				if (picked == null || !picked.endsWith("=" + today)) {
					d.statics.remove("item:" + b.key());
					if (picked != null) d.statics.remove(picked);
				}
			}
		}
		if (alone) {
			Map<String, List<Person>> groups = new LinkedHashMap<>();
			for (Person p : r.people()) if (p.group() != null) groups.computeIfAbsent(p.group(), g -> new ArrayList<>()).add(p);
			for (Map.Entry<String, List<Person>> g : groups.entrySet()) {
				List<Person> ps = g.getValue();
				if (ps.size() < 2) continue;
				String was = CHOSEN.get(r.id() + "/" + g.getKey());
				Person next;
				do next = ps.get(level.getRandom().nextInt(ps.size()));
				while (next.key().equals(was));
				CHOSEN.put(r.id() + "/" + g.getKey(), next.key());
			}
		}
		AABB box = around(r);
		// The item balls: one you've picked up stays gone; one nobody's picked up is there (a new game in the same world).
		if (!r.balls().isEmpty()) {
			PlayerData d = Game.data(comer);
			List<WorldMon> balls = level.getEntitiesOfClass(WorldMon.class, box, w -> w.key().startsWith("item:"));
			for (Ball b : r.balls()) {
				String k = "item:" + b.key();
				boolean mine = d.statics.contains(k), there = false;
				for (WorldMon w : balls) if (k.equals(w.key())) {
					if (mine || there) w.discard();
					else there = true;
				}
				if (!mine && !there && (alone || !Game.takenByAnyone(k))) ball(level, r, b);
			}
		}
		// The POKéMON standing in here: gone once you've battled them, there (once) while you haven't.
		List<MonSpot> mons = MONS.getOrDefault(r.id(), List.of());
		if (!mons.isEmpty()) {
			PlayerData d = Game.data(comer);
			for (MonSpot m : mons) {
				boolean there = false, mine = d.statics.contains(m.key());
				for (WorldMon w : level.getEntitiesOfClass(WorldMon.class, box, w -> m.key().equals(w.key()))) {
					if (mine || there) w.discard();
					else there = true;
				}
				if (!mine && !there && (alone || !Game.takenByAnyone(m.key())))
					Game.placeMon(level, m.species(), m.key(), r.ox() + m.x(), r.oy(), r.oz() + m.z(), m.yaw());
			}
		}
		List<Npc> here = level.getEntitiesOfClass(Npc.class, box, n -> true);
		for (Person p : r.people()) {
			if (p.group() == null && p.from() < 0) continue;
			boolean want = wanted(level, r, p);
			boolean in = false;
			for (Npc n : here) if (p.key().equals(n.key())) {
				if (want && !in) in = true;
				else if (in || n.talker == null) n.discard();
			}
			if (want && !in) put(level, r, p);
		}
	}

	// ---------- going in and out ----------

	/** Ticks before each player can go through another door or mat (so you don't bounce straight back). */
	private static final Map<UUID, Integer> COOL = new HashMap<>();
	/** (0.51) The warp pad each player last landed on (room coordinates): quiet till they've stepped off it. */
	private static final Map<UUID, double[]> PAD_LOCK = new HashMap<>();
	/** (0.53) An elevator (SILPH CO.'s): the floor each player got in from, and the floor they picked inside. */
	private static final Map<UUID, String> LIFT_FROM = new HashMap<>(), LIFT_TO = new HashMap<>();

	/** Whether a room's an elevator: its way out is to the floor you pick. */
	public static boolean isLift(Room r) {
		for (Warp w : r.warps()) if ("box".equals(w.kind()) && "lift".equals(w.choice())) return true;
		return false;
	}

	/** A floor's doors into an elevator (its way in there), or null. */
	public static Warp liftDoor(Room floor, String lift) {
		for (Warp w : floor.warps()) if ("box".equals(w.kind()) && lift.equals(w.to())) return w;
		return null;
	}

	/** An elevator's floors, top first: the rooms with doors into it, by their floor's name (B4F to 11F, the roof). */
	public static List<Room> liftFloors(String lift) {
		List<Room> out = new ArrayList<>();
		for (Room r : ROOMS.values()) if (liftDoor(r, lift) != null) out.add(r);
		out.sort((a, b) -> Integer.compare(floorNumber(b.floor()), floorNumber(a.floor())));
		return out;
	}

	private static int floorNumber(String f) {
		String u = f == null ? "" : f.toUpperCase(java.util.Locale.ROOT).trim();
		if (u.startsWith("ROOF")) return 1000;
		java.util.regex.Matcher m = java.util.regex.Pattern.compile("^(B?)(\\d+)F").matcher(u);
		if (!m.find()) return 0;
		int n = Integer.parseInt(m.group(2));
		return m.group(1).isEmpty() ? n : -n;
	}

	/** Where you get out: the floor you picked, else the one you got in from, else the lowest. */
	private static Room liftFloor(ServerPlayer p, Room lift) {
		UUID id = p.getUUID();
		Room r = ROOMS.get(LIFT_TO.getOrDefault(id, LIFT_FROM.getOrDefault(id, "")));
		if (r != null && liftDoor(r, lift.id()) != null) return r;
		List<Room> fl = liftFloors(lift.id());
		return fl.isEmpty() ? null : fl.get(fl.size() - 1);
	}

	/** The floor picked on an elevator's buttons. */
	public static void liftPick(ServerPlayer p, String floor) {
		if (ROOMS.containsKey(floor)) LIFT_TO.put(p.getUUID(), floor);
	}

	/** The floor you'd get out on now (the buttons show it lit). */
	public static String liftAt(ServerPlayer p, Room lift) {
		Room r = liftFloor(p, lift);
		return r == null ? null : r.id();
	}
	/** Who's on their way somewhere (after the fade to black): ticks left, and where to. */
	private static final Map<UUID, Integer> PENDING_T = new HashMap<>();
	private static final Map<UUID, Runnable> PENDING = new HashMap<>();
	/**
	 * (After the client's iris has shut right down: it closes over 17 ticks, and says when it's shut, "dark". This is
	 * how long to wait if it never says.)
	 */
	private static final int FADE_TICKS = 15;

	/** How many ticks to wait for the iris at the game's speed now (TAB's x4 runs four times the ticks a second). */
	private static int fadeTicks(ServerPlayer p) {
		float rate = p.level().getServer().tickRateManager().tickrate();
		return Math.round(FADE_TICKS * Math.max(1F, rate / 20F)) + 30;
	}

	/** Your screen's iris has shut: you're moved now (stairs, doors), so you never see the next room before it shuts. */
	public static void dark(ServerPlayer p) {
		if (PENDING_T.containsKey(p.getUUID())) PENDING_T.put(p.getUUID(), 1);
	}
	private static final Map<UUID, Vec3> LAST = new HashMap<>();

	/** A new game: you wake up in your room. */
	public static boolean start(ServerPlayer p) {
		Room r = ROOMS.get(startRoom);
		return r != null && go(p, r, startX, startZ, startYaw, false);
	}

	/** Into a room at a spot in it (room coordinates), facing yaw. With a fade, after the screen's gone black. */
	public static boolean go(ServerPlayer p, Room r, double x, double z, float yaw, boolean fade) {
		return go(p, r, x, 0, z, yaw, fade);
	}

	/** As go, standing that high above the room's floor (Mt. Moon's B2F has its upper level). */
	public static boolean go(ServerPlayer p, Room r, double x, double y, double z, float yaw, boolean fade) {
		MinecraftServer s = p.level().getServer();
		ServerLevel level = ensure(s);
		if (level == null || r == null) return false;
		ready(level, r);
		COOL.put(p.getUUID(), fadeTicks(p) + 20);
		// (0.50, 20:07: a house took a long time to load) The room's chunks loaded while the screen fades, not after.
		preload(level, r.ox() - 2, r.oz() - 2, r.ox() + r.w() + 2, r.oz() + r.d() + 2);
		Runnable move = () -> {
			boolean fromOutside = p.level() != level;
			if (fromOutside) Game.leavingLevel(p);
			p.teleportTo(level, r.ox() + x, r.oy() + y, r.oz() + z, java.util.Set.of(), yaw, 30F, true);
			p.setDeltaMovement(Vec3.ZERO);
			LAST.remove(p.getUUID());
			settle(p);
			Game.roomEntered(p, r, fromOutside);
		};
		if (!fade) {
			move.run();
			return true;
		}
		Net.send(p, "fade", new CompoundTag());
		PENDING_T.put(p.getUUID(), fadeTicks(p));
		PENDING.put(p.getUUID(), move);
		return true;
	}

	/** The rooms' chunks kept loaded (once you've been in one): going in again is quick. At most this many. */
	private static final java.util.LinkedHashSet<Long> HELD = new java.util.LinkedHashSet<>();

	/** Loads (and keeps loaded) the chunks over a part of a level, so a room's there before you arrive. */
	private static void preload(ServerLevel level, double x0, double z0, double x1, double z1) {
		try {
			for (int cx = (int) Math.floor(x0) >> 4; cx <= (int) Math.floor(x1) >> 4; cx++)
				for (int cz = (int) Math.floor(z0) >> 4; cz <= (int) Math.floor(z1) >> 4; cz++) {
					long k = net.minecraft.world.level.ChunkPos.asLong(cx, cz);
					if (level == level.getServer().overworld() || HELD.contains(k)) continue;
					level.setChunkForced(cx, cz, true);
					HELD.add(k);
					if (HELD.size() > 96) {
						long old = HELD.iterator().next();
						HELD.remove(old);
						level.setChunkForced(net.minecraft.world.level.ChunkPos.getX(old), net.minecraft.world.level.ChunkPos.getZ(old), false);
					}
				}
		} catch (RuntimeException e) {
			Retromon.LOG.warn("Retromon: couldn't hold a room's chunks", e);
		}
	}

	/** Out through a building's door: standing just outside it in the world, facing away. */
	public static void out(ServerPlayer p, Door d) {
		MinecraftServer s = p.level().getServer();
		ServerLevel world = s.overworld();
		COOL.put(p.getUUID(), fadeTicks(p) + 20);
		// (The world outside the door loaded while the screen fades: (0.55) the chunks all round it, not just its own, so
		// you're not kept waiting on them once you're out.)
		int dcx = BlockPos.containing(d.x(), d.y(), d.z()).getX() >> 4, dcz = BlockPos.containing(d.x(), d.y(), d.z()).getZ() >> 4;
		for (int cx = dcx - 2; cx <= dcx + 2; cx++)
			for (int cz = dcz - 2; cz <= dcz + 2; cz++) world.getChunk(cx, cz);
		Net.send(p, "fade", new CompoundTag());
		PENDING_T.put(p.getUUID(), fadeTicks(p));
		PENDING.put(p.getUUID(), () -> {
			if (p.level() != world) Game.leavingLevel(p);
			p.teleportTo(world, d.x(), d.y(), d.z(), java.util.Set.of(), d.yaw(), 30F, true);
			p.setDeltaMovement(Vec3.ZERO);
			LAST.remove(p.getUUID());
			settle(p);
			Game.roomLeft(p);
		});
	}

	/**
	 * (0.53, 11:16: "There seems to be a delay going into stuff.") Once you're through a door, only a moment (half a
	 * second) before the next one works; it held you for nearly three seconds more after the screen opened.
	 */
	private static void settle(ServerPlayer p) {
		float rate = p.level().getServer().tickRateManager().tickrate();
		COOL.put(p.getUUID(), Math.round(10 * Math.max(1F, rate / 20F)));
	}

	/** Straight out of a room to a point in the world (FAST TRAVEL), no door or fade. */
	public static void jumpOut(ServerPlayer p, ServerLevel world, double x, double y, double z) {
		Game.leavingLevel(p);
		p.teleportTo(world, x, y, z, java.util.Set.of(), p.getYRot(), 30F, true);
		p.setDeltaMovement(Vec3.ZERO);
		LAST.remove(p.getUUID());
		Game.roomLeft(p);
	}

	/**
	 * The door a room's way out takes you through: its own, or, for a room every Center (or every Mart) shares, the one
	 * you came in by, so you come out of the Pewter Center in Pewter.
	 */
	public static Door doorOut(ServerPlayer p, String own) {
		Door d = own == null ? null : DOORS.get(own);
		Door came = DOORS.get(Game.data(p).door);
		if (d != null && came != null && d.group() != null && d.group().equals(came.group())) return came;
		return d;
	}

	/** The place a shared room is in for you (the town of the Center you went into), or null for the room's own. */
	public static String placeFor(ServerPlayer p, Room r) {
		Door came = DOORS.get(Game.data(p).door);
		return came != null && came.place() != null && came.room().equals(r.id()) ? came.place() : null;
	}

	/** Out through one of the doors by its key (the Route 28 Gate's choice: ROUTE 28's door, or ROUTE 26's). */
	public static void outThrough(ServerPlayer p, String key) {
		Door d = DOORS.get(key);
		if (d != null) out(p, d);
	}

	public static boolean busy(ServerPlayer p) {
		return PENDING.containsKey(p.getUUID());
	}

	/**
	 * Every tick, for each player: the way in at a door, the mats and stairs inside. True when it has just moved
	 * you (through a door, up or down stairs), or you're on your way (the screen gone dark).
	 */
	public static boolean tick(ServerPlayer p) {
		UUID id = p.getUUID();
		Integer pt = PENDING_T.get(id);
		if (pt != null) {
			if (pt > 1) PENDING_T.put(id, pt - 1);
			else {
				PENDING_T.remove(id);
				Runnable r = PENDING.remove(id);
				if (r != null) r.run();
			}
			return true;
		}
		COOL.computeIfPresent(id, (k, v) -> v > 1 ? v - 1 : null);
		Vec3 here = p.position(), last = LAST.put(id, here);
		double dz = last == null ? 0 : here.z - last.z, dx = last == null ? 0 : here.x - last.x;
		if (COOL.containsKey(id)) return false;
		float yaw = Mth.wrapDegrees(p.getYRot());
		boolean facingSouth = Math.abs(yaw) < 55, facingNorth = Math.abs(Mth.wrapDegrees(yaw - 180)) < 55;
		boolean facingEast = Math.abs(Mth.wrapDegrees(yaw + 90)) < 55, facingWest = Math.abs(Mth.wrapDegrees(yaw - 90)) < 55;
		if (in(p)) {
			Room r = roomAt(p.getX());
			if (r == null) return false;
			// (A game saved in here and loaded after an update: the room made as it is now.)
			if (!upToDate(r) && p.level() instanceof ServerLevel lv && TRIED.add(r.id())) {
				ensure(lv.getServer());
				if (!upToDate(r)) ready(lv, r);
			}
			double lx = p.getX() - r.ox(), ly = p.getY() - r.oy(), lz = p.getZ() - r.oz();
			// Fallen out of a room somehow (under a floor, or off its edge: 20:20, at the Underground Path's far stairs):
			// back to where you come in.
			boolean off = lx < -1.5 || lz < -1.5 || lx > r.w() + 1.5 || lz > r.d() + 1.5 || ly < -7;
			if (off && r.schem() == null || ly < -3 && r.warps().stream().noneMatch(w -> w.kind().equals("down"))) {
				go(p, r, r.ex(), r.ez(), r.eyaw(), false);
				return true;
			}
			for (Warp w : r.warps()) {
				switch (w.kind()) {
					case "mat" -> {
						if (lz > w.z0() && lx > w.x0() && lx < w.x1() && (dz > 0.01 || facingSouth)) {
							// (0.51) A guard's way out by the mat (Route 5's gate, south into SAFFRON): only with what it needs.
							if (w.needs() != null && !Game.meets(p, w.needs())) {
								p.teleportTo(p.getX(), p.getY(), p.getZ() - 1.4);
								COOL.put(id, 30);
								Game.gateBlocked(p, w.needs());
								return true;
							}
							// Through to another room (the forest and its gates).
							if (w.to() != null && ROOMS.containsKey(w.to())) {
								go(p, ROOMS.get(w.to()), w.sx(), w.sz(), w.syaw(), true);
								return true;
							}
							Door d = doorOut(p, w.door());
							if (d == null) continue;
							if (r.id().equals("lab") && !Game.data(p).started) {
								// The games' way: Oak won't let you go without a Pokémon of your own.
								p.teleportTo(p.getX(), p.getY(), r.oz() + w.z0() - 1.2);
								COOL.put(id, 20);
								Game.message(p, List.of("OAK: Hey! Don't go away yet!"));
								return false;
							}
							out(p, d);
							return true;
						}
					}
					case "pad" -> {
						// (0.51) A warp pad (the SAFFRON GYM's): onto its partner, in a blink. The pad you land on stays quiet
						// till you've stepped off it.
						double[] lock = PAD_LOCK.get(id);
						if (lock != null && Math.hypot(lx - lock[0], lz - lock[1]) < 0.75) continue;
						if (lock != null) PAD_LOCK.remove(id);
						if (lx > w.x0() && lx < w.x1() && lz > w.z0() && lz < w.z1()) {
							PAD_LOCK.put(id, new double[] {w.sx(), w.sz()});
							// (0.53) SILPH CO.'s: a pad whose partner is on another floor.
							if (w.to() != null && !w.to().equals(r.id()) && ROOMS.containsKey(w.to())) {
								CompoundTag s = new CompoundTag();
								s.putString("sound", "warp");
								Net.send(p, "sound", s);
								go(p, ROOMS.get(w.to()), w.sx(), w.sz(), 0F, true);
								return true;
							}
							p.teleportTo(r.ox() + w.sx(), p.getY(), r.oz() + w.sz());
							p.setDeltaMovement(Vec3.ZERO);
							LAST.remove(id);
							COOL.put(id, 6);
							CompoundTag s = new CompoundTag();
							s.putString("sound", "warp");
							Net.send(p, "sound", s);
							return true;
						}
					}
					case "box" -> {
						// (0.53) Walking into it (SILPH CO.'s stairs, its elevator's doors): to the room it leads to. Out
						// of an elevator ("lift"): onto the floor you picked there, or back where you got in.
						if (lx > w.x0() && lx < w.x1() && lz > w.z0() && lz < w.z1()) {
							if ("lift".equals(w.choice())) {
								Room fl = liftFloor(p, r);
								Warp door = fl == null ? null : liftDoor(fl, r.id());
								if (door == null) continue;
								LIFT_TO.remove(id);
								LIFT_FROM.put(id, fl.id());
								go(p, fl, (door.x0() + door.x1()) / 2, door.z1() + 0.65, 0F, true);
								return true;
							}
							Room to = w.to() == null ? null : ROOMS.get(w.to());
							if (to == null) continue;
							if (isLift(to)) {
								LIFT_FROM.put(id, r.id());
								LIFT_TO.remove(id);
							}
							go(p, to, w.sx(), w.sz(), w.syaw(), true);
							return true;
						}
					}
					case "key_n", "key_s", "key_e", "key_w" -> {
						// (0.53) SILPH CO.'s card-key shutters: walking into one, through to its other side with the CARD KEY;
						// without it you're turned back ("It needs a CARD KEY!").
						char dir = w.kind().charAt(4);
						boolean heading = switch (dir) {
							case 'n' -> dz < -0.01 || facingNorth;
							case 's' -> dz > 0.01 || facingSouth;
							case 'e' -> dx > 0.01 || facingEast;
							default -> dx < -0.01 || facingWest;
						};
						if (heading && lx > w.x0() && lx < w.x1() && lz > w.z0() && lz < w.z1()) {
							if (w.needs() != null && !Game.meets(p, w.needs())) {
								double bx = dir == 'e' ? -0.5 : dir == 'w' ? 0.5 : 0, bz = dir == 's' ? -0.5 : dir == 'n' ? 0.5 : 0;
								p.teleportTo(p.getX() + bx, p.getY(), p.getZ() + bz);
								COOL.put(id, 30);
								Game.gateBlocked(p, w.needs());
								return true;
							}
							Game.shutterOpened(p, w.door());
							p.teleportTo(r.ox() + w.sx(), p.getY(), r.oz() + w.sz());
							p.setYRot(w.syaw());
							p.setDeltaMovement(Vec3.ZERO);
							LAST.remove(id);
							COOL.put(id, 12);
							return true;
						}
					}
					case "east", "west" -> {
						// Through a way in a side wall (a sideways gate's): out to the world, to the next room, or asked where to.
						boolean east = w.kind().equals("east");
						if ((east ? lx > w.x0() : lx < w.x1()) && lz > w.z0() && lz < w.z1() && (east ? dx > 0.01 || facingEast : dx < -0.01 || facingWest)) {
							// (0.51) A sideways gate's guarded way (into SAFFRON, onto the CYCLING ROAD): only with what it needs.
							if (w.needs() != null && !Game.meets(p, w.needs())) {
								p.teleportTo(p.getX() + (east ? -1.4 : 1.4), p.getY(), p.getZ());
								COOL.put(id, 30);
								Game.gateBlocked(p, w.needs());
								return true;
							}
							if (w.choice() != null) {
								p.teleportTo(p.getX() + (east ? -1.3 : 1.3), p.getY(), p.getZ());
								COOL.put(id, 30);
								Game.gateChoice(p, w.choice());
								return true;
							}
							if (w.to() != null && ROOMS.containsKey(w.to())) {
								go(p, ROOMS.get(w.to()), w.sx(), w.sz(), w.syaw(), true);
								return true;
							}
							Door d = doorOut(p, w.door());
							if (d == null) continue;
							out(p, d);
							return true;
						}
					}
					case "north" -> {
						// Through a way in the north wall: to the next room, or out to the world (the north gate).
						if (lz < w.z1() && lz > w.z0() && lx > w.x0() && lx < w.x1() && (dz < -0.01 || facingNorth)) {
							// A guard's way (the League gate's): only with what it needs (a badge); else you're turned back.
							if (w.needs() != null && !Game.meets(p, w.needs())) {
								p.teleportTo(p.getX(), p.getY(), p.getZ() + 1.4);
								COOL.put(id, 30);
								Game.gateBlocked(p, w.needs());
								return true;
							}
							if (w.to() != null && ROOMS.containsKey(w.to())) {
								go(p, ROOMS.get(w.to()), w.sx(), w.sz(), w.syaw(), true);
								return true;
							}
							Door d = doorOut(p, w.door());
							if (d == null) continue;
							out(p, d);
							return true;
						}
					}
					case "up" -> {
						// (A cave's ladder: walk up to it and you're at the top, no climbing; 14:15.)
						boolean cave = r.schem() != null;
						double m = cave ? 0.45 : 0;
						if ((ly >= 1.2 || cave && ly > -0.6) && lx > w.x0() - m - 0.3 && lx < w.x1() + m + 0.6 && lz > w.z0() - 0.6 && lz < w.z1() + 0.6) {
							if (w.to() == null) {
								// Up and out of the cave (Mt. Moon's way out onto Route 4).
								Door d = doorOut(p, w.door());
								if (d == null) continue;
								out(p, d);
								return true;
							}
							go(p, ROOMS.get(w.to()), w.sx(), w.sy(), w.sz(), w.syaw(), true);
							return true;
						}
					}
					case "down" -> {
						// A hole with its own box (Mt. Moon's three on 1F): stepping into it, there; else (stairs) at the foot.
						boolean box = w.z1() > w.z0();
						if (box ? ly <= -0.4 && lx > w.x0() - 0.3 && lx < w.x1() + 0.3 && lz > w.z0() - 0.3 && lz < w.z1() + 0.3 : ly <= -1.45) {
							go(p, ROOMS.get(w.to()), w.sx(), w.sy(), w.sz(), w.syaw(), true);
							return true;
						}
					}
					default -> {
					}
				}
			}
			return false;
		}
		if (p.level().dimension() != Level.OVERWORLD) return false;
		for (Door d : DOORS.values()) {
			double x0 = 1e9, x1 = -1e9, z0 = 1e9, z1 = -1e9, y = 0;
			for (BlockPos b : d.blocks()) {
				x0 = Math.min(x0, b.getX());
				x1 = Math.max(x1, b.getX() + 1);
				z0 = Math.min(z0, b.getZ());
				z1 = Math.max(z1, b.getZ() + 1);
				y = b.getY();
			}
			// Up against the door's outside face, heading in: a south door's from the south heading north; a north
			// door's from the north heading south; an east door's (the Route 28 Gate's) from the east heading west; a
			// west door's from the west heading east.
			boolean acrossX = p.getX() > x0 - 0.2 && p.getX() < x1 + 0.2, acrossZ = p.getZ() > z0 - 0.2 && p.getZ() < z1 + 0.2;
			boolean at = switch (d.face()) {
				case "n" -> acrossX && p.getZ() > z1 - 2.55 && p.getZ() < z1 - 0.95 && (dz > 0.01 || facingSouth);
				case "e" -> acrossZ && p.getX() > x1 - 1.0 && p.getX() < x1 + 0.55 && (dx < -0.01 || facingWest);
				case "w" -> acrossZ && p.getX() > x0 - 0.55 && p.getX() < x0 + 1.0 && (dx > 0.01 || facingEast);
				default -> acrossX && p.getZ() > z1 - 1.0 && p.getZ() < z1 + 0.55 && (dz < -0.01 || facingNorth);
			};
			if (at && Math.abs(p.getY() - y) < 1.2) {
				Room r = ROOMS.get(d.room());
				if (r != null) {
					Game.data(p).door = d.key();
					if (d.in() != null) go(p, r, d.in()[0], d.in()[1], (float) d.in()[2], true);
					else go(p, r, r.ex(), r.ez(), r.eyaw(), true);
				}
				return r != null;
			}
		}
		return false;
	}

	/** Whether you're standing in a room's wild grass (the forest's). */
	public static boolean inGrass(Entity e) {
		Room r = roomOf(e);
		if (r == null) return false;
		double x = e.getX() - r.ox(), z = e.getZ() - r.oz();
		for (double[] g : r.grass()) if (x > g[0] && x < g[2] && z > g[1] && z < g[3]) return true;
		return false;
	}

	/** A spot in a room's wild grass near you (between 4 and 12 blocks away), for a wild one to appear; or null. */
	public static BlockPos grassNear(Entity e, net.minecraft.util.RandomSource rnd) {
		Room r = roomOf(e);
		if (r == null || r.grass().isEmpty()) return null;
		for (int tries = 0; tries < 12; tries++) {
			double[] g = r.grass().get(rnd.nextInt(r.grass().size()));
			double x = r.ox() + g[0] + 0.5 + rnd.nextDouble() * Math.max(0, g[2] - g[0] - 1), z = r.oz() + g[1] + 0.5 + rnd.nextDouble() * Math.max(0, g[3] - g[1] - 1);
			double dist = Math.hypot(x - e.getX(), z - e.getZ());
			if (dist >= 4 && dist <= 12) return BlockPos.containing(x, r.oy(), z);
		}
		return null;
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
		Ahead a = ahead(e);
		return a == null ? null : a.lines();
	}

	/** Something in a room you're facing: what it says, how far ahead it is, and whether it's a map (the Town Map). */
	public record Ahead(List<String> lines, double distance, Vec3 at, boolean map, boolean pc, String use) {
	}

	/** The thing straight ahead of you in a room (0.5 to 2.1 blocks on), or null. */
	public static Ahead ahead(Entity e) {
		Room r = roomOf(e);
		if (r == null) return null;
		Vec3 look = Vec3.directionFromRotation(0, e.getYRot());
		// Only what's just in front of you (a step or so), so things on the walls don't call from across the room.
		for (double d = 0.4; d <= 1.3 + 1e-6; d += 0.3) {
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
			if (best != null && !best.lines().isEmpty())
				return new Ahead(best.lines(), d, new Vec3(r.ox() + (best.x0() + best.x1()) / 2, r.oy() + 1.6, r.oz() + (best.z0() + best.z1()) / 2), best.map(), best.pc(), best.use());
		}
		return null;
	}
}
