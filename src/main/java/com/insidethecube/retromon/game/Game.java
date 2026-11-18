package com.insidethecube.retromon.game;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.insidethecube.retromon.Retromon;
import com.insidethecube.retromon.engine.Item;
import com.insidethecube.retromon.engine.Mon;
import com.insidethecube.retromon.engine.Move;
import com.insidethecube.retromon.engine.Places;
import com.insidethecube.retromon.engine.Species;
import com.insidethecube.retromon.engine.Wild;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;

/**
 * The game on the server: your save and its places, walking in the grass and caves, the people you talk to,
 * trainers who spot you, one-off Pokémon and gifts, healing, the Mart, your bag, your first Pokémon, and what
 * happens after a battle (nicknames, evolving, new moves).
 */
public final class Game {
	private Game() {}

	public static final String SAVE_NAME = "Retromon";
	public static final String MARKER = "retromon.json";
	/** The map is built at twice the games' size: a step in the games is this many blocks (for trainers' sight). */
	public static final int BLOCKS_PER_STEP = 2;
	public static AttachmentType<PlayerData> DATA;
	private static final Random RNG = new Random();
	private static final Map<UUID, BlockPos> LAST = new HashMap<>();
	private static final Map<UUID, Integer> COOLDOWN = new HashMap<>();
	/** Players who switched the adventure on with the key, outside a Retromon save (for trying it out). */
	private static final Map<UUID, Boolean> TRYING = new HashMap<>();
	/** Who's in a text box or being walked up to (no encounters, no other trainers). */
	private static final Map<UUID, Integer> BUSY = new HashMap<>();
	private static boolean ourSave;
	private static MinecraftServer server;
	private static JsonObject marker = new JsonObject();

	public static void register() {
		DATA = AttachmentRegistry.create(Retromon.id("player"), b -> b.persistent(PlayerData.CODEC).initializer(PlayerData::new).copyOnDeath());
	}

	public static PlayerData data(ServerPlayer p) {
		return p.getAttachedOrCreate(DATA);
	}

	// ---------- the save ----------

	public static void serverStarted(MinecraftServer s) {
		server = s;
		Path m = marker(s);
		ourSave = Files.exists(m);
		Flags.solidSigns = ourSave;
		STREET.clear();
		heldTime = -1;
		zones.clear();
		if (!ourSave) return;
		try {
			marker = JsonParser.parseString(Files.readString(m, StandardCharsets.UTF_8)).getAsJsonObject();
		} catch (Exception e) {
			marker = new JsonObject();
		}
		loadZones();
		ServerLevel level = s.overworld();
		GameRules rules = level.getGameRules();
		rules.set(GameRules.ADVANCE_WEATHER, false, s);
		rules.set(GameRules.ADVANCE_TIME, true, s);
		rules.set(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, 0, s);
		rules.set(GameRules.SPAWN_MOBS, false, s);
		rules.set(GameRules.SPAWN_MONSTERS, false, s);
		rules.set(GameRules.SPAWN_PHANTOMS, false, s);
		rules.set(GameRules.SPAWN_PATROLS, false, s);
		rules.set(GameRules.SPAWN_WANDERING_TRADERS, false, s);
		rules.set(GameRules.MOB_GRIEFING, false, s);
		rules.set(GameRules.RANDOM_TICK_SPEED, 0, s);
		rules.set(GameRules.FALL_DAMAGE, false, s);
		rules.set(GameRules.KEEP_INVENTORY, true, s);
		rules.set(GameRules.SHOW_ADVANCEMENT_MESSAGES, false, s);
		level.setWeatherParameters(12000, 0, false, false);
	}

	private static Path marker(MinecraftServer s) {
		return s.getWorldPath(LevelResource.ROOT).resolve(MARKER);
	}

	private static void saveMarker() {
		if (server == null || !ourSave) return;
		JsonArray zs = new JsonArray();
		for (Zone z : zones) {
			JsonObject o = new JsonObject();
			o.addProperty("place", z.place());
			o.addProperty("x1", z.x1());
			o.addProperty("z1", z.z1());
			o.addProperty("x2", z.x2());
			o.addProperty("z2", z.z2());
			zs.add(o);
		}
		marker.add("zones", zs);
		try {
			Files.writeString(marker(server), marker.toString(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			Retromon.LOG.warn("Couldn't write {}", MARKER, e);
		}
	}

	public static boolean ourSave() {
		return ourSave;
	}

	public static boolean adventure(ServerPlayer p) {
		return ourSave || TRYING.getOrDefault(p.getUUID(), false);
	}

	public static void join(ServerPlayer p) {
		LAST.remove(p.getUUID());
		if (!ourSave) {
			sync(p);
			return;
		}
		p.setGameMode(GameType.ADVENTURE);
		// A new adventure starts in Pallet Town, with empty pockets.
		if (!data(p).started) {
			p.getInventory().clearContent();
			toStart(p);
		}
		setAdventure(p, true);
		setSpeed(1);
		sync(p);
		data(p).place = "";
		// Without a Pokémon: Oak stops you as you head out of Pallet Town (below); on a map without Pallet Town
		// marked, his welcome comes straight away.
		if (!data(p).started && town() == null) Net.send(p, "intro", new CompoundTag());
	}

	private static Zone town() {
		for (Zone z : zones) if (z.place().equals("pallet_town")) return z;
		return null;
	}

	/** Where each player last stood well inside Pallet Town (to put them back without clipping into a fence). */
	private static final Map<UUID, BlockPos> SAFE = new HashMap<>();
	/** Who's to meet their rival once their first Pokémon has its name. */
	private static final java.util.Set<UUID> RIVAL_PENDING = new java.util.HashSet<>();

	/**
	 * Before you have a Pokémon, Oak stops you as you step into the path out of Pallet Town (as the games did at
	 * the tall grass): you're put back where you last stood in town, and he takes you to his lab to choose one.
	 */
	private static void oakStops(ServerPlayer p) {
		Zone town = town();
		if (town == null) return;
		boolean inTown = town.contains(p.getX(), p.getZ());
		// The tree line: the last few blocks of town at its north edge, where the path to Route 1 starts.
		boolean leaving = inTown ? p.getZ() < town.z1() + 7 : placeAt(p.getX(), p.getZ()) != null;
		if (inTown && !leaving) {
			if (p.onGround() && p.getZ() > town.z1() + 12) SAFE.put(p.getUUID(), p.blockPosition());
			return;
		}
		if (!leaving) return;
		BlockPos back = SAFE.get(p.getUUID());
		if (back == null) {
			int x = Math.max(town.x1() + 4, Math.min(town.x2() - 4, p.getBlockX()));
			back = spot((ServerLevel) p.level(), x, town.z1() + 14, 8);
		}
		p.teleportTo(back.getX() + 0.5, back.getY(), back.getZ() + 0.5);
		BUSY.put(p.getUUID(), 200);
		CompoundTag t = new CompoundTag();
		t.putBoolean("stop", true);
		Net.send(p, "intro", t);
	}

	/** Oak takes you to his lab: you're put at its door (until it has an inside). */
	public static void toLab(ServerPlayer p) {
		Zone town = town();
		if (town == null || data(p).started) return;
		int[] size = Places.size("pallet_town");
		BlockPos door = size == null ? null : gameSpot((ServerLevel) p.level(), town, size, new int[] {12, 12});
		if (door == null) return;
		p.teleportTo((ServerLevel) p.level(), door.getX() + 0.5, door.getY(), door.getZ() + 0.5, java.util.Set.of(), 180F, 0F, true);
		BUSY.put(p.getUUID(), 200);
	}

	/** Your rival's first Pokémon: the one strong against yours (as in Red and Blue). */
	private static Trainers.Trainer rivalLab(ServerPlayer p) {
		PlayerData d = data(p);
		String mine = d.party.isEmpty() ? "charmander" : d.party.get(0).species.id();
		String sp = switch (mine) {
			case "bulbasaur" -> "charmander";
			case "squirtle" -> "bulbasaur";
			case "pikachu" -> "eevee";
			default -> "squirtle";
		};
		return new Trainers.Trainer("rival_lab", "pallet_town", rival(p), "blue", "none", 0, 35, List.of(new Trainers.Member(sp, 5)), null,
				List.of(), List.of("WHAT? Unbelievable! I picked the wrong POKéMON!"),
				List.of("{rival}: Okay! I'll make my POKéMON fight to toughen it up!", "{player}! Gramps! Smell ya later!"));
	}

	/** After your first Pokémon gets its name: your rival takes his, and wants to battle (as in the games). */
	private static void rivalChallenge(ServerPlayer p) {
		PlayerData d = data(p);
		if (d.party.isEmpty() || d.beaten.contains("rival_lab")) return;
		Trainers.Trainer t = rivalLab(p);
		String sp = Species.get(t.team().get(0).species()).name();
		dialogue(p, null, "rival", fill(p, List.of("{rival}: Heh, I don't need to be greedy like you!", "Go ahead and choose, {player}!",
				"{rival}: I'll take this one, then!", "{rival} received a " + sp + "!", "{rival}: Wait, {player}! Let's check out our POKéMON!",
				"Come on, I'll take you on!")));
	}

	public static Trainers.Trainer rivalTrainer(ServerPlayer p) {
		return rivalLab(p);
	}


	public static void leave(ServerPlayer p) {
		Battles.drop(p);
		LAST.remove(p.getUUID());
		BUSY.remove(p.getUUID());
		setSpeed(1);
	}

	/** Turns the adventure on or off for a player: no hunger, no harm, and the camera on their screen. */
	public static void setAdventure(ServerPlayer p, boolean on) {
		if (!ourSave) TRYING.put(p.getUUID(), on);
		AttributeInstance step = p.getAttribute(Attributes.STEP_HEIGHT);
		// Only stairs and slabs can be climbed, as in the games (the old step-up is taken off).
		if (step != null) step.removeModifier(Retromon.id("step"));
		CompoundTag t = new CompoundTag();
		t.putBoolean("on", adventure(p));
		t.putBoolean("save", ourSave);
		Net.send(p, "mode", t);
	}

	public static void sync(ServerPlayer p) {
		Net.send(p, "sync", data(p).save());
	}

	/** The game's speed: 1 to 4 times (everything, as an emulator's fast-forward does). */
	public static void setSpeed(int times) {
		if (server == null) return;
		server.tickRateManager().setTickRate(20F * Math.max(1, Math.min(4, times)));
	}

	/** Map editing (`/retromon build on`): off, nobody can break or change the map in a Retromon save. */
	public static boolean build;

	/** Whether this player is kept from breaking or changing anything (a Retromon save, not in build mode). */
	public static boolean locked(net.minecraft.world.entity.player.Player p) {
		return ourSave && !build && p instanceof ServerPlayer;
	}

	/** Pallet Town, outside your house. */
	public static final int START_X = -806, START_Y = 18, START_Z = 700;

	private static void toStart(ServerPlayer p) {
		ServerLevel level = (ServerLevel) p.level();
		BlockPos at = spot(level, START_X, START_Z, 6);
		p.teleportTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
	}

	/** Somewhere to stand at x, z: the spot nearest the height given (so it's the street, not a roof). */
	public static BlockPos standable(ServerLevel level, int x, int z, int nearY) {
		BlockPos best = null;
		for (int y = nearY + 12; y >= nearY - 12; y--) {
			BlockPos at = new BlockPos(x, y, z);
			if (!free(level, at) || !free(level, at.above()) || free(level, at.below())) continue;
			if (best == null || Math.abs(y - nearY) < Math.abs(best.getY() - nearY)) best = at;
		}
		return best != null ? best : new BlockPos(x, nearY, z);
	}

	private static boolean free(ServerLevel level, BlockPos at) {
		return level.getBlockState(at).getCollisionShape(level, at).isEmpty();
	}

	/** Good ground to stand on: dry, open above, and on a solid top (not water, a fence, a wall or a tree). */
	private static boolean goodGround(ServerLevel level, BlockPos at) {
		if (!free(level, at) || !free(level, at.above()) || !level.getFluidState(at).isEmpty() || !level.getFluidState(at.above()).isEmpty()) return false;
		BlockPos below = at.below();
		BlockState b = level.getBlockState(below);
		if (!level.getFluidState(below).isEmpty() || b.is(net.minecraft.tags.BlockTags.LEAVES) || b.is(net.minecraft.tags.BlockTags.FENCES)
				|| b.is(net.minecraft.tags.BlockTags.WALLS) || b.is(net.minecraft.tags.BlockTags.FENCE_GATES)) return false;
		if (!b.isFaceSturdy(level, below, net.minecraft.core.Direction.UP)) return false;
		// Not a roof: real ground has more under it (a roof has the room beneath it).
		return !level.getBlockState(at.below(2)).isAir() && !level.getBlockState(at.below(3)).isAir() && level.canSeeSky(at);
	}

	/**
	 * The nearest good spot to stand around x, z (on the ground, not a roof or a treetop), searching outwards
	 * up to `radius` blocks. The ground height comes from the map itself.
	 */
	public static BlockPos spot(ServerLevel level, int x, int z, int radius) {
		Zone zone = null;
		for (int i = zones.size() - 1; i >= 0 && zone == null; i--) if (zones.get(i).contains(x, z)) zone = zones.get(i);
		if (zone != null) {
			// Near the place's street level (so not on a roof, however solid the building is).
			int street = streetY(level, zone);
			BlockPos at = spotNear(level, x, z, radius + 4, street);
			if (at != null) return at;
		}
		return spotAny(level, x, z, radius);
	}

	private static final Map<String, Integer> STREET = new HashMap<>();

	/** The height most of a place's open ground is at (its streets and paths). */
	private static int streetY(ServerLevel level, Zone z) {
		return STREET.computeIfAbsent(z.place() + z.x1() + "," + z.z1(), k -> {
			Map<Integer, Integer> count = new HashMap<>();
			for (int x = z.x1(); x <= z.x2(); x += 3) {
				for (int zz = z.z1(); zz <= z.z2(); zz += 3) {
					int top = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, zz);
					BlockState below = level.getBlockState(new BlockPos(x, top - 1, zz));
					if (below.is(net.minecraft.tags.BlockTags.LEAVES) || !level.getFluidState(new BlockPos(x, top - 1, zz)).isEmpty()) continue;
					count.merge(top, 1, Integer::sum);
				}
			}
			int best = START_Y, n = -1;
			for (Map.Entry<Integer, Integer> e : count.entrySet()) if (e.getValue() > n) {
				n = e.getValue();
				best = e.getKey();
			}
			return best;
		});
	}

	/** The nearest good ground within two blocks of a height (searching outwards), or null. */
	private static BlockPos spotNear(ServerLevel level, int x, int z, int radius, int y0) {
		for (int r = 0; r <= radius; r++) {
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
					for (int y = y0 + 2; y >= y0 - 2; y--) {
						BlockPos at = new BlockPos(x + dx, y, z + dz);
						if (goodGround(level, at)) return at;
					}
				}
			}
		}
		return null;
	}

	private static BlockPos spotAny(ServerLevel level, int x, int z, int radius) {
		for (int r = 0; r <= radius; r++) {
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
					int top = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x + dx, z + dz);
					for (int y = top; y >= top - 6; y--) {
						BlockPos at = new BlockPos(x + dx, y, z + dz);
						if (goodGround(level, at)) return at;
					}
				}
			}
		}
		return standable(level, x, z, START_Y);
	}

	// ---------- places (zones on the map) ----------

	/** A part of the map that's a place in the games: a box from x1, z1 to x2, z2 (top to bottom of the world). */
	public record Zone(String place, int x1, int z1, int x2, int z2) {
		boolean contains(double x, double z) {
			return x >= x1 && x <= x2 + 1 && z >= z1 && z <= z2 + 1;
		}
	}

	private static final List<Zone> zones = new ArrayList<>();
	private static final Map<UUID, int[]> CORNER1 = new HashMap<>(), CORNER2 = new HashMap<>();

	/** Pallet Town to Route 2 on Insid3 Th3 Cub3's Kanto map, to begin with (mark the rest with /retromon zone). */
	private static final Zone[] DEFAULT_ZONES = {new Zone("pallet_town", -845, 655, -745, 735), new Zone("route_1", -845, 590, -745, 654),
			new Zone("viridian_city", -830, 495, -745, 589), new Zone("route_22", -900, 495, -831, 565), new Zone("route_2", -830, 340, -745, 494)};

	private static void loadZones() {
		zones.clear();
		if (marker.has("zones")) {
			for (JsonElement e : marker.getAsJsonArray("zones")) {
				JsonObject o = e.getAsJsonObject();
				zones.add(new Zone(o.get("place").getAsString(), o.get("x1").getAsInt(), o.get("z1").getAsInt(), o.get("x2").getAsInt(), o.get("z2").getAsInt()));
			}
		} else {
			zones.addAll(List.of(DEFAULT_ZONES));
			saveMarker();
		}
	}

	public static String placeAt(double x, double z) {
		for (int i = zones.size() - 1; i >= 0; i--) if (zones.get(i).contains(x, z)) return zones.get(i).place();
		return null;
	}

	public static List<Zone> zones() {
		return zones;
	}

	public static void corner(ServerPlayer p, int which) {
		int[] c = {p.getBlockX(), p.getBlockZ()};
		(which == 1 ? CORNER1 : CORNER2).put(p.getUUID(), c);
	}

	/** Marks the box between your two corners as a place. Returns false if a corner's missing. */
	public static boolean markZone(ServerPlayer p, String place) {
		int[] a = CORNER1.get(p.getUUID()), b = CORNER2.get(p.getUUID());
		if (a == null || b == null) return false;
		zones.add(new Zone(place, Math.min(a[0], b[0]), Math.min(a[1], b[1]), Math.max(a[0], b[0]), Math.max(a[1], b[1])));
		saveMarker();
		return true;
	}

	public static int clearZones(String place) {
		int before = zones.size();
		zones.removeIf(z -> z.place().equals(place));
		saveMarker();
		return before - zones.size();
	}

	private static void enter(ServerPlayer p, String place) {
		PlayerData d = data(p);
		if (place == null || place.equals(d.place)) return;
		d.place = place;
		if (d.visited.add(place)) sync(p);
		Places.Place pl = Places.get(place);
		CompoundTag t = new CompoundTag();
		t.putString("id", place);
		t.putString("name", pl == null ? place.toUpperCase().replace('_', ' ') : pl.name());
		t.putString("music", pl == null ? "" : pl.music());
		t.putString("region", pl == null ? "kanto" : pl.region());
		Net.send(p, "place", t);
	}

	// ---------- every tick ----------

	public static void tick(MinecraftServer s) {
		for (ServerPlayer p : s.getPlayerList().getPlayers()) {
			if (!adventure(p)) continue;
			UUID id = p.getUUID();
			p.getFoodData().setFoodLevel(20);
			if (p.getHealth() < p.getMaxHealth()) p.setHealth(p.getMaxHealth());
			COOLDOWN.computeIfPresent(id, (k, v) -> v > 1 ? v - 1 : null);
			BUSY.computeIfPresent(id, (k, v) -> v > 1 ? v - 1 : null);
			if (s.getTickCount() % 10 == 0) enter(p, placeAt(p.getX(), p.getZ()));
			if (s.getTickCount() % 5 == 0 && !data(p).started && !BUSY.containsKey(id)) oakStops(p);
			if (s.getTickCount() % 40 == 0) moonlight(p);
			// Kept in Adventure mode (so nothing on the map can be broken, Axiom included) unless map editing is on.
			if (!build && s.getTickCount() % 20 == 0 && p.gameMode.getGameModeForPlayer() != GameType.ADVENTURE) p.setGameMode(GameType.ADVENTURE);
			if (Battles.in(p) || BUSY.containsKey(id)) {
				LAST.put(id, p.blockPosition());
				continue;
			}
			BlockPos pos = p.blockPosition();
			BlockPos last = LAST.put(id, pos);
			if (last != null && (last.getX() != pos.getX() || last.getZ() != pos.getZ())) step(p, pos);
			if (s.getTickCount() % 4 == 0) watch(p);
		}
		approach(s);
		if (s.getTickCount() % 40 == 0) placeTouchPoints(s);
		holdTime(s);
	}

	/** Each player's NIGHTS setting (0 dark, 1 soft, 2 bright), sent from their OPTION. */
	private static final Map<UUID, Integer> NIGHTS = new HashMap<>();

	public static void nights(ServerPlayer p, CompoundTag t) {
		NIGHTS.put(p.getUUID(), Math.max(0, Math.min(2, t.getIntOr("nights", 1))));
		moonlight(p);
	}

	/** At night, an unseen night vision (no icon, no swirls) so the screen can lift the dark to a moonlit blue. */
	private static void moonlight(ServerPlayer p) {
		long t = Math.floorMod(p.level().getDayTime(), 24000L);
		boolean night = t >= 12300 && t < 23700 && NIGHTS.getOrDefault(p.getUUID(), 1) > 0;
		var nv = p.getEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);
		boolean ours = nv != null && nv.getAmplifier() == 7;
		if (night && (nv == null || ours && nv.getDuration() < 400)) {
			p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION, 900, 7, true, false, false));
		} else if (!night && ours) p.removeEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);
	}

	/** The time when a battle began: the world's clock stands still until it's over (so day doesn't turn to night mid-fight). */
	private static long heldTime = -1;

	private static void holdTime(MinecraftServer s) {
		boolean fighting = false;
		for (ServerPlayer p : s.getPlayerList().getPlayers()) if (adventure(p) && Battles.in(p)) fighting = true;
		ServerLevel level = s.overworld();
		if (!fighting) {
			heldTime = -1;
			return;
		}
		if (heldTime < 0) heldTime = level.getDayTime();
		else if (level.getDayTime() != heldTime) level.setDayTime(heldTime);
	}

	/** WAIT from the menu: on to the morning (0), midday (1), evening (2) or night (3). */
	public static void waitUntil(ServerPlayer p, CompoundTag t) {
		if (!adventure(p) || Battles.in(p) || !p.level().getServer().isSingleplayer()) return;
		long[] at = {23500, 6000, 11800, 14500};
		long target = at[Math.max(0, Math.min(3, t.getIntOr("until", 0)))];
		ServerLevel level = p.level().getServer().overworld();
		long now = level.getDayTime(), base = now - Math.floorMod(now, 24000), to = base + target;
		if (to <= now) to += 24000;
		level.setDayTime(to);
	}

	private static boolean grass(BlockState s) {
		return s.is(Blocks.SHORT_GRASS) || s.is(Blocks.TALL_GRASS) || s.is(Blocks.FERN) || s.is(Blocks.LARGE_FERN);
	}

	/** Caves, towers and the like: wild Pokémon on any step, not just in grass. */
	private static boolean anywhere(String place) {
		for (String k : new String[] {"cave", "mt_moon", "tunnel", "seafoam", "victory_road", "power_plant", "tower", "mansion", "well", "mt_mortar", "ice_path",
				"whirl_island", "mount_mortar", "ruins_of_alph_inner", "dragons_den", "silver_cave", "rocket", "slowpoke"}) {
			if (place.contains(k)) return true;
		}
		return false;
	}

	/** Blocks walked since the last step (the map is at 4 blocks to the games' step), and steps of peace after a battle. */
	private static final Map<UUID, Integer> WALKED = new HashMap<>(), GRACE = new HashMap<>();
	public static final int BLOCKS_PER_GAME_STEP = 4;

	/** A step: friendship, REPEL, and in the grass (or a cave) a wild Pokémon may jump out, as often as in the games. */
	private static void step(ServerPlayer p, BlockPos pos) {
		int walked = WALKED.merge(p.getUUID(), 1, Integer::sum);
		if (walked < BLOCKS_PER_GAME_STEP) return;
		WALKED.put(p.getUUID(), 0);
		if (GRACE.computeIfPresent(p.getUUID(), (k, v) -> v > 1 ? v - 1 : null) != null) return;
		PlayerData d = data(p);
		for (Mon m : d.party) {
			if (++m.steps >= 512) {
				m.steps = 0;
				m.friendship = Math.min(255, m.friendship + 1);
			}
		}
		if (d.repel > 0 && --d.repel == 0) message(p, List.of("REPEL's effect wore off."));
		if (!d.started || !d.anyHealthy() || COOLDOWN.containsKey(p.getUUID()) || d.place.isEmpty()) return;
		boolean inGrass = grass(p.level().getBlockState(pos)) || grass(p.level().getBlockState(pos.above()));
		if (!inGrass && !anywhere(d.place)) return;
		Wild.Table table = Wild.get(d.place, "grass", Wild.timeOfDay(p.level().getDayTime()));
		if (table == null || table.ratePer256() <= 0 || !Wild.has(d.place)) return;
		if (RNG.nextInt(256) >= table.ratePer256()) return;
		Mon foe = table.roll(RNG);
		// A REPEL keeps away wild Pokémon weaker than your lead.
		Mon lead = null;
		for (Mon m : d.party) if (!m.fainted()) {
			lead = m;
			break;
		}
		if (d.repel > 0 && lead != null && foe.level < lead.level) return;
		Battles.wild(p, foe, null);
	}

	public static void afterBattle(ServerPlayer p) {
		COOLDOWN.put(p.getUUID(), 60);
		GRACE.put(p.getUUID(), 4);
	}

	public static void blackout(ServerPlayer p) {
		PlayerData d = data(p);
		if (d.hasHeal) p.teleportTo(d.healX, d.healY, d.healZ);
		else toStart(p);
	}

	// ---------- trainers who spot you ----------

	private record Walk(Npc npc, ServerPlayer player, Trainers.Trainer trainer) {}

	private static final Map<UUID, Walk> WALKS = new HashMap<>();

	/** Any trainer who hasn't battled you yet and sees you along their line walks up to you. */
	private static void watch(ServerPlayer p) {
		PlayerData d = data(p);
		if (!d.started || !d.anyHealthy() || WALKS.containsKey(p.getUUID())) return;
		ServerLevel level = (ServerLevel) p.level();
		for (Npc n : level.getEntitiesOfClass(Npc.class, p.getBoundingBox().inflate(24))) {
			if (n.role() != Npc.TRAINER && n.role() != Npc.LEADER) continue;
			Trainers.Trainer t = Trainers.get(n.key());
			if (t == null || t.sight() <= 0 || d.beaten.contains(t.key())) continue;
			double yaw = Math.toRadians(n.facing());
			double fx = -Math.sin(yaw), fz = Math.cos(yaw);
			double dx = p.getX() - n.getX(), dz = p.getZ() - n.getZ(), dy = p.getY() - n.getY();
			double along = dx * fx + dz * fz, across = Math.abs(dx * fz - dz * fx);
			if (along < 0.5 || along > t.sight() * BLOCKS_PER_STEP + 0.5 || across > 1.2 || Math.abs(dy) > 2) continue;
			if (!n.hasLineOfSight(p)) continue;
			CompoundTag e = new CompoundTag();
			e.putInt("npc", n.getId());
			e.putString("music", spottedMusic(t));
			Net.send(p, "exclaim", e);
			n.approaching = 120;
			BUSY.put(p.getUUID(), 400);
			WALKS.put(p.getUUID(), new Walk(n, p, t));
			return;
		}
	}

	/** The "eyes meet" tune: the girls', the boys' or Team Rocket's. */
	static String spottedMusic(Trainers.Trainer t) {
		String s = t.sprite();
		if (s.contains("rocket") || s.contains("giovanni") || s.contains("jessie") || s.contains("james")) return "meet_evil_trainer";
		if (s.endsWith("_f") || s.contains("girl") || s.contains("lass") || s.contains("beauty") || s.contains("woman") || s.contains("channeler")
				|| s.contains("lorelei") || s.contains("agatha")) return "meet_female_trainer";
		return "meet_male_trainer";
	}

	private static void approach(MinecraftServer s) {
		WALKS.entrySet().removeIf(en -> {
			Walk w = en.getValue();
			Npc n = w.npc();
			ServerPlayer p = w.player();
			if (n.isRemoved() || p.isRemoved() || p.hasDisconnected()) return true;
			n.approaching--;
			double dist = n.distanceTo(p);
			if (n.approaching > 100) {
				n.getNavigation().stop();
				return false;
			}
			if (dist > 1.8 && n.approaching > 0) {
				n.getNavigation().moveTo(p, 1.0);
				return false;
			}
			n.getNavigation().stop();
			n.approaching = 0;
			n.getLookControl().setLookAt(p);
			talkBattle(p, n, w.trainer());
			return true;
		});
	}

	// ---------- talking ----------

	private static String player(ServerPlayer p) {
		String n = data(p).name;
		return n.isEmpty() ? p.getGameProfile().name().toUpperCase() : n;
	}

	private static String rival(ServerPlayer p) {
		String n = data(p).rival;
		return n.isEmpty() ? "BLUE" : n;
	}

	/** The games' lines with your name (and your rival's) put in. */
	static List<String> fill(ServerPlayer p, List<String> lines) {
		List<String> out = new ArrayList<>();
		for (String l : lines) out.add(l.replace("{player}", player(p)).replace("{rival}", rival(p)));
		return out;
	}

	/** From the new-game intro: name, rival and boy or girl (only before the adventure starts, or to fill in a blank). */
	public static void profile(ServerPlayer p, CompoundTag t) {
		PlayerData d = data(p);
		String name = clean(t.getStringOr("name", "")), rival = clean(t.getStringOr("rival", ""));
		if (!name.isEmpty() && (!d.started || d.name.isEmpty())) d.name = name;
		if (!rival.isEmpty() && (!d.started || d.rival.isEmpty())) d.rival = rival;
		String g = t.getStringOr("gender", "");
		if (g.equals("boy") || g.equals("girl")) d.gender = g;
		sync(p);
	}

	private static String clean(String s) {
		s = s.replaceAll("[^A-Za-z0-9 .'-]", "").trim().toUpperCase();
		return s.length() > 10 ? s.substring(0, 10) : s;
	}

	public static void talk(ServerPlayer p, int entityId) {
		if (Battles.in(p)) return;
		Entity e = p.level().getEntity(entityId);
		if (e == null || e.distanceTo(p) > 4.5) return;
		if (e instanceof WorldMon w) {
			talkMon(p, w);
			return;
		}
		if (!(e instanceof Npc n)) return;
		PlayerData d = data(p);
		n.getLookControl().setLookAt(p);
		switch (n.role()) {
			case Npc.NURSE -> dialogue(p, n, "heal", List.of("Welcome to our POKéMON CENTER!", "We heal your POKéMON back to perfect health!",
					"Shall we heal your POKéMON?"));
			case Npc.CLERK -> {
				CompoundTag t = new CompoundTag();
				t.putString("mart", n.key().isEmpty() ? d.place : n.key());
				dialogue(p, n, "mart", List.of("Hi there! May I help you?"), t);
			}
			case Npc.PERSON -> {
				Trainers.Person person = Trainers.PEOPLE.get(n.key());
				List<String> lines = new ArrayList<>(person == null ? List.of("...") : fill(p, person.lines()));
				// Someone with a TM or HM to give (the HM houses, the TM gifts): it's yours.
				String pl = person == null ? "" : person.place();
				boolean outdoors = pl.endsWith("_city") || pl.endsWith("_town") || pl.startsWith("route_") && !pl.contains("gate") && !pl.contains("house")
						|| pl.endsWith("_island");
				if (person != null && !pl.endsWith("_gym") && !outdoors) {
					String got = giveGift(p, person.place());
					if (got != null) lines.add(got);
				}
				dialogue(p, n, "talk", lines);
			}
			case Npc.TRAINER, Npc.LEADER -> {
				Trainers.Trainer t = Trainers.get(n.key());
				if (t == null) {
					dialogue(p, n, "talk", List.of("..."));
					return;
				}
				if (d.beaten.contains(t.key())) {
					dialogue(p, n, "talk", fill(p, t.after().isEmpty() ? List.of("...") : t.after()));
					return;
				}
				if (!d.started || !d.anyHealthy()) {
					dialogue(p, n, "talk", List.of("You don't have any POKéMON that can battle!"));
					return;
				}
				talkBattle(p, n, t);
			}
			default -> {
			}
		}
	}

	/** An item ball: the item goes in your bag (once), and the ball's gone. */
	private static void pickUp(ServerPlayer p, WorldMon w, String key) {
		PlayerData d = data(p);
		Trainers.FieldItem fi = Trainers.FIELD_ITEMS.get(key);
		if (fi == null || d.statics.contains("item:" + key)) {
			w.discard();
			return;
		}
		d.statics.add("item:" + key);
		Item it = Item.find(fi.item());
		String name = it != null ? it.name() : fi.item().replace("tm_", "TM ").replace('_', ' ').toUpperCase();
		if (it != null) d.give(fi.item(), 1);
		w.discard();
		sync(p);
		CompoundTag s = new CompoundTag();
		s.putString("sound", "item");
		Net.send(p, "sound", s);
		message(p, List.of(player(p) + " found " + name + "!"));
	}

	private static void talkMon(ServerPlayer p, WorldMon w) {
		PlayerData d = data(p);
		String key = w.key();
		Species sp = Species.ALL.get(w.species());
		String name = sp == null ? "POKéMON" : sp.name();
		CompoundTag t = new CompoundTag();
		t.putString("cry", w.species());
		if (key.startsWith("item:")) {
			pickUp(p, w, key.substring(5));
			return;
		}
		if (key.startsWith("pet:")) {
			Trainers.Person person = Trainers.PEOPLE.get(key.substring(4));
			dialogue(p, null, "talk", person == null ? List.of(name + ": ...") : fill(p, person.lines()), t, w.getId());
			return;
		}
		Trainers.Static st = Trainers.STATICS.get(key);
		if (st == null || d.statics.contains(key)) {
			dialogue(p, null, "talk", List.of(name + " is looking at you."), t, w.getId());
			return;
		}
		t.putString("static", key);
		if (st.kind().equals("gift") || st.kind().equals("trade")) {
			dialogue(p, null, "gift", List.of(st.note() + ".", "Do you want " + name + "?"), t, w.getId());
		} else if (!d.started || !d.anyHealthy()) {
			dialogue(p, null, "talk", List.of("A wild " + name + "!", "You'd better have a POKéMON that can battle first."), t, w.getId());
		} else {
			dialogue(p, null, "static", List.of(st.species().equals("snorlax") ? "A sleeping POKéMON blocks the way!" : "The wild " + name + " attacked!"), t,
					w.getId());
		}
	}

	private static void talkBattle(ServerPlayer p, Npc n, Trainers.Trainer t) {
		CompoundTag extra = new CompoundTag();
		extra.putString("trainer", t.key());
		dialogue(p, n, "battle", fill(p, t.before().isEmpty() ? List.of(t.title() + " wants to battle!") : t.before()), extra);
	}

	private static void dialogue(ServerPlayer p, Npc n, String mode, List<String> lines) {
		dialogue(p, n, mode, lines, new CompoundTag());
	}

	private static void dialogue(ServerPlayer p, Npc n, String mode, List<String> lines, CompoundTag t) {
		dialogue(p, n, mode, lines, t, n == null ? -1 : n.getId());
	}

	private static void dialogue(ServerPlayer p, Npc n, String mode, List<String> lines, CompoundTag t, int entityId) {
		t.putInt("npc", entityId);
		t.putString("mode", mode);
		ListTag l = new ListTag();
		for (String s : lines) l.add(StringTag.valueOf(s));
		t.put("lines", l);
		BUSY.put(p.getUUID(), 1200);
		Net.send(p, "dialogue", t);
	}

	/** Lines to show (no one speaking): an item's effect, REPEL wearing off. */
	public static void message(ServerPlayer p, List<String> lines) {
		dialogue(p, null, "talk", lines, new CompoundTag(), -1);
	}

	/** A text box closed: heal (if you said yes), open the Mart, start the battle, take the gift. */
	public static void dialogueDone(ServerPlayer p, CompoundTag t) {
		BUSY.remove(p.getUUID());
		String mode = t.getStringOr("mode", "talk");
		boolean yes = t.getBooleanOr("yes", false);
		Entity e = p.level().getEntity(t.getIntOr("npc", -1));
		Npc n = e instanceof Npc npc ? npc : null;
		PlayerData d = data(p);
		switch (mode) {
			case "heal" -> {
				if (!yes) {
					dialogue(p, n, "talk", List.of("We hope to see you again!"));
					return;
				}
				d.healAll();
				d.hasHeal = true;
				d.healX = p.getX();
				d.healY = p.getY();
				d.healZ = p.getZ();
				sync(p);
				CompoundTag s = new CompoundTag();
				s.putString("sound", "heal");
				Net.send(p, "sound", s);
				dialogue(p, n, "talk", List.of("Thank you for waiting.", "We've restored your POKéMON to full health.", "We hope to see you again!"));
			}
			case "mart" -> {
				CompoundTag m = new CompoundTag();
				m.putString("mart", t.getStringOr("mart", d.place));
				Net.send(p, "mart", m);
			}
			case "rival" -> {
				if (!d.beaten.contains("rival_lab") && !d.party.isEmpty()) Battles.trainer(p, rivalLab(p));
			}
			case "battle" -> {
				Trainers.Trainer tr = Trainers.get(t.getStringOr("trainer", ""));
				if (tr != null && !d.beaten.contains(tr.key()) && d.anyHealthy()) Battles.trainer(p, tr);
			}
			case "static" -> {
				Trainers.Static st = Trainers.STATICS.get(t.getStringOr("static", ""));
				if (st == null || d.statics.contains(st.id()) || !d.anyHealthy()) return;
				Battles.wild(p, new Mon(Species.get(st.species()), st.level(), RNG), st.id());
			}
			case "gift" -> {
				Trainers.Static st = Trainers.STATICS.get(t.getStringOr("static", ""));
				if (!yes || st == null || d.statics.contains(st.id())) return;
				Mon m = new Mon(Species.get(st.species()), st.level(), RNG);
				boolean toBox = d.party.size() >= 6;
				d.add(m);
				d.statics.add(st.id());
				if (e instanceof WorldMon w) w.discard();
				sync(p);
				CompoundTag s = new CompoundTag();
				s.putString("sound", "caught");
				Net.send(p, "sound", s);
				List<String> lines = new ArrayList<>(List.of(player(p) + " got " + m.name() + "!"));
				if (toBox) lines.add(m.name() + " was sent to BILL's PC.");
				message(p, lines);
				askNickname(p, toBox ? "box" : "party", toBox ? d.box.size() - 1 : d.party.size() - 1);
			}
			default -> {
			}
		}
	}

	// ---------- after a battle: nicknames, evolving, new moves ----------

	public static void askNickname(ServerPlayer p, String where, int index) {
		CompoundTag t = new CompoundTag();
		t.putString("where", where);
		t.putInt("i", index);
		Mon m = where.equals("box") ? data(p).box.get(index) : data(p).party.get(index);
		t.putString("species", m.species.id());
		Net.send(p, "nickname", t);
	}

	public static void nickname(ServerPlayer p, CompoundTag t) {
		PlayerData d = data(p);
		List<Mon> list = t.getStringOr("where", "party").equals("box") ? d.box : d.party;
		int i = t.getIntOr("i", -1);
		if (i < 0 || i >= list.size()) return;
		String name = t.getStringOr("name", "").strip().replaceAll("[^\\p{L}\\p{N} .'\\-♂♀]", "").toUpperCase();
		if (name.length() > 10) name = name.substring(0, 10);
		list.get(i).nickname = name.isEmpty() ? null : name;
		sync(p);
		if (RIVAL_PENDING.remove(p.getUUID())) rivalChallenge(p);
	}

	/** Offers an evolution for a party Pokémon if it's ready (by level, or by the item given). */
	public static boolean offerEvolution(ServerPlayer p, int index, String trigger, String item) {
		PlayerData d = data(p);
		if (index < 0 || index >= d.party.size()) return false;
		Mon m = d.party.get(index);
		boolean day = !"nite".equals(Wild.timeOfDay(p.level().getDayTime()));
		Species into = m.evolution(trigger, item, day);
		if (into == null) return false;
		CompoundTag t = new CompoundTag();
		t.putInt("i", index);
		t.putString("from", m.species.id());
		t.putString("into", into.id());
		t.putString("name", m.name());
		t.putString("item", item == null ? "" : item);
		Net.send(p, "evolve", t);
		return true;
	}

	public static void evolve(ServerPlayer p, CompoundTag t) {
		PlayerData d = data(p);
		int i = t.getIntOr("i", -1);
		if (i < 0 || i >= d.party.size()) return;
		Mon m = d.party.get(i);
		String item = t.getStringOr("item", "");
		boolean day = !"nite".equals(Wild.timeOfDay(p.level().getDayTime()));
		Species into = m.evolution(item.isEmpty() ? "level" : "item", item.isEmpty() ? null : item, day);
		if (into == null || !into.id().equals(t.getStringOr("into", ""))) return;
		if (!t.getBooleanOr("ok", false)) return;
		if (!item.isEmpty() && !d.take(item)) return;
		m.evolveInto(into);
		d.seen.add(into.id());
		d.caught.add(into.id());
		sync(p);
		for (String mv : into.learnsAt(m.level)) offerMove(p, i, mv);
	}

	/** A move a party Pokémon can learn now: straight in if there's room, otherwise you choose. */
	/**
	 * A TM or HM: "Booted up a TM! It contained <move>! Teach <move> to <Pokémon>?" If it can learn it, it does
	 * (or you pick a move to forget); a TM's used up, an HM isn't.
	 */
	private static void teach(ServerPlayer p, Item it, int index) {
		PlayerData d = data(p);
		Mon m = d.party.get(index);
		Move mv = Move.find(it.move());
		if (mv == null) return;
		List<String> lines = new ArrayList<>();
		lines.add("Booted up " + (it.hm() ? "an HM" : "a TM") + "! It contained " + mv.name() + "!");
		if (!m.species.canLearnByMachine(mv.id())) {
			lines.add(m.name() + " is not compatible with " + mv.name() + ". It can't learn " + mv.name() + ".");
			message(p, lines);
			return;
		}
		if (m.moves.contains(mv.id())) {
			lines.add(m.name() + " already knows " + mv.name() + "!");
			message(p, lines);
			return;
		}
		if (!it.hm()) d.take(it.id());
		message(p, lines);
		offerMove(p, index, mv.id());
	}

	/** Fly: off to a town you've been to, if one of yours knows FLY and you have the THUNDERBADGE (as in the games). */
	public static void fly(ServerPlayer p, CompoundTag t) {
		PlayerData d = data(p);
		String place = t.getStringOr("place", "");
		if (Battles.in(p) || !d.visited.contains(place)) return;
		boolean knows = d.party.stream().anyMatch(m -> m.moves.contains("fly") && !m.fainted());
		if (!knows || !d.badges.contains("THUNDERBADGE")) {
			message(p, List.of(!knows ? "None of your POKéMON knows FLY." : "You need the THUNDERBADGE to use FLY outside of battle."));
			return;
		}
		Zone z = null;
		for (Zone x : zones) if (x.place().equals(place)) z = x;
		if (z == null) {
			message(p, List.of("There's nowhere to land there yet."));
			return;
		}
		ServerLevel level = (ServerLevel) p.level();
		BlockPos at = spot(level, (z.x1() + z.x2()) / 2, (z.z1() + z.z2()) / 2, 16);
		p.teleportTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
		enter(p, place);
	}

	/** Your clothes, from the wardrobe. */
	public static void outfit(ServerPlayer p, CompoundTag t) {
		PlayerData d = data(p);
		String o = t.getStringOr("outfit", "");
		if (o.length() > 200) return;
		d.outfit = o;
		sync(p);
	}

	/** The TMs and HMs people give, by place (the gym leaders' after their badge, the others when you talk to them). */
	private static java.util.Map<String, List<String>> gifts;

	public static List<String> giftsIn(String place) {
		if (gifts == null) {
			java.util.Map<String, List<String>> m = new java.util.HashMap<>();
			for (String[] r : Species.table("gifts.tsv")) m.computeIfAbsent(r[0], k -> new ArrayList<>()).add(r[1]);
			gifts = m;
		}
		return gifts.getOrDefault(place, List.of());
	}

	/** Gives the next gift from this place you haven't had yet: the line to say, or null. */
	public static String giveGift(ServerPlayer p, String place) {
		PlayerData d = data(p);
		for (String g : giftsIn(place)) {
			if (!d.statics.add("gift:" + place + ":" + g)) continue;
			Item it = Item.find(g);
			if (it == null) continue;
			d.give(g, 1);
			sync(p);
			return player(p) + " received " + it.name() + "!";
		}
		return null;
	}

	public static void offerMove(ServerPlayer p, int index, String move) {
		PlayerData d = data(p);
		if (index < 0 || index >= d.party.size() || Move.find(move) == null) return;
		Mon m = d.party.get(index);
		if (m.moves.contains(move)) return;
		if (m.moves.size() < 4) {
			m.learn(move);
			sync(p);
			message(p, List.of(m.name() + " learned " + Move.get(move).name() + "!"));
			return;
		}
		CompoundTag t = new CompoundTag();
		t.putInt("i", index);
		t.putString("move", move);
		Net.send(p, "learn", t);
	}

	public static void learn(ServerPlayer p, CompoundTag t) {
		PlayerData d = data(p);
		int i = t.getIntOr("i", -1), slot = t.getIntOr("slot", -1);
		String move = t.getStringOr("move", "");
		if (i < 0 || i >= d.party.size() || Move.find(move) == null) return;
		Mon m = d.party.get(i);
		if (slot < 0 || slot >= m.moves.size() || m.moves.contains(move) || !m.species.learnset().stream().anyMatch(l -> l.move().equals(move))) return;
		m.replace(slot, move);
		sync(p);
	}

	// ---------- the Mart and the bag ----------

	public static void buy(ServerPlayer p, CompoundTag t) {
		PlayerData d = data(p);
		Item it = Item.find(t.getStringOr("item", ""));
		int n = Math.max(1, Math.min(99, t.getIntOr("n", 1)));
		if (it == null || it.price() <= 0 || d.money < it.price() * n) return;
		d.money -= it.price() * n;
		d.give(it.id(), n);
		sync(p);
	}

	public static void sell(ServerPlayer p, CompoundTag t) {
		PlayerData d = data(p);
		Item it = Item.find(t.getStringOr("item", ""));
		int n = Math.max(1, Math.min(99, t.getIntOr("n", 1)));
		if (it == null || it.price() <= 0 || it.keyItem() || d.count(it.id()) < n) return;
		d.give(it.id(), -n);
		d.money += it.price() / 2 * n;
		sync(p);
	}

	/** Using an item from the bag outside battle (on one of your Pokémon, or not). */
	public static void use(ServerPlayer p, CompoundTag t) {
		PlayerData d = data(p);
		Item it = Item.find(t.getStringOr("item", ""));
		if (it == null || d.count(it.id()) <= 0 || Battles.in(p)) return;
		int i = t.getIntOr("i", -1);
		if (it.repelSteps() > 0) {
			d.take(it.id());
			d.repel = it.repelSteps();
			message(p, List.of(player(p) + " used " + it.name() + "!"));
		} else if (it.id().equals("escape_rope")) {
			d.take(it.id());
			blackout(p);
			message(p, List.of(player(p) + " used an ESCAPE ROPE!"));
		} else if (it.teaches() && i >= 0 && i < d.party.size()) {
			teach(p, it, i);
		} else if (i >= 0 && i < d.party.size()) {
			Mon m = d.party.get(i);
			if (it.evolves()) {
				if (!offerEvolution(p, i, "item", it.id())) message(p, List.of("It won't have any effect."));
				return;
			}
			if (it.id().equals("rare_candy")) {
				if (m.level >= 100) {
					message(p, List.of("It won't have any effect."));
					return;
				}
				d.take(it.id());
				int oldMax = m.maxHp();
				m.level++;
				m.exp = Math.max(m.exp, m.species.growth().expAt(m.level));
				m.levelFriendship();
				if (!m.fainted()) m.hp = Math.min(m.maxHp(), m.hp + m.maxHp() - oldMax);
				sync(p);
				message(p, List.of(m.name() + " grew to level " + m.level + "!"));
				for (String mv : m.species.learnsAt(m.level)) offerMove(p, i, mv);
				offerEvolution(p, i, "level", null);
				return;
			}
			String said = it.use(m);
			if (said == null) {
				message(p, List.of("It won't have any effect."));
				return;
			}
			d.take(it.id());
			message(p, List.of(said));
		} else {
			message(p, List.of("OAK: " + player(p) + "! This isn't the time to use that!"));
			return;
		}
		sync(p);
	}

	public static void starter(ServerPlayer p, CompoundTag t) {
		PlayerData d = data(p);
		if (d.started) return;
		String sp = t.getStringOr("species", "");
		if (!sp.equals("bulbasaur") && !sp.equals("charmander") && !sp.equals("squirtle") && !sp.equals("pikachu")) return;
		d.add(new Mon(Species.get(sp), 5, RNG));
		d.started = true;
		d.give("poke_ball", 5);
		d.give("potion", 3);
		d.money = 3000;
		sync(p);
		if (town() != null) RIVAL_PENDING.add(p.getUUID());
		askNickname(p, "party", 0);
	}

	public static void save(ServerPlayer p) {
		p.level().getServer().saveEverything(true, false, true);
	}

	public static void options(ServerPlayer p, CompoundTag t) {
		data(p).textSpeed = Math.max(0, Math.min(2, t.getIntOr("textSpeed", 2)));
	}

	/** Moves a party Pokémon to another slot (the menu's SWITCH). */
	public static void order(ServerPlayer p, CompoundTag t) {
		PlayerData d = data(p);
		int a = t.getIntOr("a", -1), b = t.getIntOr("b", -1);
		if (a < 0 || b < 0 || a >= d.party.size() || b >= d.party.size() || Battles.in(p)) return;
		Mon m = d.party.get(a);
		d.party.set(a, d.party.get(b));
		d.party.set(b, m);
		sync(p);
	}

	// ---------- people and Pokémon in the world ----------

	/**
	 * Lamps light a wider circle (the games' towns are bright at night): round each lamp on the ground (sea
	 * lanterns, lanterns, glowstone, froglights, lamps), unseen light blocks are set in two rings on open
	 * ground, so the light carries several blocks further. Done once per place.
	 */
	public static int lightUp(ServerLevel level, Zone z) {
		int street = streetY(level, z), placed = 0;
		BlockState light12 = Blocks.LIGHT.defaultBlockState().setValue(net.minecraft.world.level.block.LightBlock.LEVEL, 12);
		BlockState light9 = Blocks.LIGHT.defaultBlockState().setValue(net.minecraft.world.level.block.LightBlock.LEVEL, 9);
		java.util.Set<Long> done = new java.util.HashSet<>();
		for (int x = z.x1(); x <= z.x2(); x++) {
			for (int zz = z.z1(); zz <= z.z2(); zz++) {
				for (int y = street - 2; y <= street + 9; y++) {
					BlockPos at = new BlockPos(x, y, zz);
					BlockState b = level.getBlockState(at);
					if (b.getLightEmission() < 12 || b.is(Blocks.LIGHT) || !level.getFluidState(at).isEmpty() || b.is(Blocks.FIRE) || b.is(Blocks.LAVA)) continue;
					for (int ring = 0; ring < 2; ring++) {
						int r = ring == 0 ? 5 : 9;
						for (int i = 0; i < 8; i++) {
							double a = Math.PI * 2 * i / 8 + ring * Math.PI / 8;
							int lx = x + (int) Math.round(Math.cos(a) * r), lz = zz + (int) Math.round(Math.sin(a) * r);
							if (!done.add(BlockPos.asLong(lx, 0, lz))) continue;
							int top = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, lx, lz);
							BlockPos to = new BlockPos(lx, top, lz);
							if (Math.abs(top - street) > 4 || !level.getBlockState(to).isAir()) continue;
							level.setBlock(to, ring == 0 ? light12 : light9, 2);
							placed++;
						}
					}
				}
			}
		}
		return placed;
	}

	/** The first people on the Kanto map: the Viridian nurse and clerk (until the interiors), and a practice trainer on Route 1. */
	private static final Object[][] DEFAULTS = {{"nurse", Npc.NURSE, "", -785, 561, 0F}, {"clerk", Npc.CLERK, "viridian_city", -766, 549, 0F},
			{"joey", Npc.TRAINER, "youngster_joey", -790, 600, 0F}};

	/** Puts the first people in place once their part of the map is loaded (each only once per save). */
	private static void placeTouchPoints(MinecraftServer s) {
		if (!ourSave) return;
		JsonArray placed = marker.has("placedKeys") ? marker.getAsJsonArray("placedKeys") : new JsonArray();
		List<String> done = new ArrayList<>();
		for (JsonElement e : placed) done.add(e.getAsString());
		ServerLevel level = s.overworld();
		boolean changed = false;
		// Each marked place gets the games' people, trainers and item balls once its ground is loaded.
		for (Zone z : List.copyOf(zones)) {
			if (done.contains("place:" + z.place())) continue;
			if (!level.hasChunkAt(new BlockPos(z.x1(), START_Y, z.z1())) || !level.hasChunkAt(new BlockPos(z.x2(), START_Y, z.z2()))) continue;
			populate(level, z.place(), false);
			lightUp(level, z);
			placed.add("place:" + z.place());
			done.add("place:" + z.place());
			changed = true;
		}
		for (Object[] d : DEFAULTS) {
			String key = (String) d[0];
			if (done.contains(key)) continue;
			int x = (Integer) d[3], z = (Integer) d[4];
			if (!level.hasChunkAt(new BlockPos(x, START_Y, z))) continue;
			BlockPos at = spot(level, x, z, 6);
			place(level, (Integer) d[1], (String) d[2], at.getX() + 0.5, at.getY(), at.getZ() + 0.5, (Float) d[5]);
			placed.add(key);
			changed = true;
		}
		if (changed) {
			marker.add("placedKeys", placed);
			saveMarker();
		}
	}

	/**
	 * Puts a place's people, trainers and item balls where the games had them: each one's spot on the games'
	 * map is laid over the place's marked box (so mark the box to the place's edges), then moved to the nearest
	 * good ground. Anyone already there stays (or, with redo, is put back where the games had them).
	 */
	public static int populate(ServerLevel level, String place, boolean redo) {
		Zone z = null;
		for (Zone x : zones) if (x.place().equals(place)) {
			z = x;
			break;
		}
		int[] size = Places.size(place);
		if (z == null || size == null) return -1;
		net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(z.x1(), level.getMinY(), z.z1(), z.x2() + 1, level.getMaxY(), z.z2() + 1);
		java.util.Set<String> present = new java.util.HashSet<>();
		for (Npc n : level.getEntitiesOfClass(Npc.class, box)) {
			if (redo && Trainers.POS.containsKey(n.key())) n.discard();
			else present.add(n.key());
		}
		for (WorldMon w : level.getEntitiesOfClass(WorldMon.class, box)) {
			if (redo && (w.key().startsWith("pet:") || w.key().startsWith("item:"))) w.discard();
			else present.add(w.key());
		}
		int count = 0;
		for (Trainers.Trainer t : Trainers.in(place)) {
			BlockPos at = gameSpot(level, z, size, Trainers.POS.get(t.key()));
			if (at == null || present.contains(t.key())) continue;
			place(level, t.leader() ? Npc.LEADER : Npc.TRAINER, t.key(), at.getX() + 0.5, at.getY(), at.getZ() + 0.5, yawOf(t.facing(), 0F));
			count++;
		}
		for (Trainers.Person ps : Trainers.peopleIn(place)) {
			// Oak at the edge of Pallet Town is the one who stops you (he comes to you); he doesn't stand there.
			if (ps.key().equals("pallet_town_p0")) continue;
			BlockPos at = gameSpot(level, z, size, Trainers.POS.get(ps.key()));
			if (at == null) continue;
			String sp = ps.species();
			if (sp != null) {
				if (present.contains("pet:" + ps.key())) continue;
				placeMon(level, sp, "pet:" + ps.key(), at.getX() + 0.5, at.getY(), at.getZ() + 0.5, yawOf(ps.facing(), 0F));
			} else {
				if (present.contains(ps.key())) continue;
				place(level, Npc.PERSON, ps.key(), at.getX() + 0.5, at.getY(), at.getZ() + 0.5, yawOf(ps.facing(), 0F));
			}
			count++;
		}
		for (Trainers.FieldItem fi : Trainers.itemsIn(place)) {
			if (present.contains("item:" + fi.key()) || takenByAnyone("item:" + fi.key())) continue;
			BlockPos at = gameSpot(level, z, size, new int[] {fi.x(), fi.y()});
			if (at == null) continue;
			placeMon(level, "poke_ball", "item:" + fi.key(), at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0F);
			count++;
		}
		return count;
	}

	private static boolean takenByAnyone(String key) {
		if (server == null) return false;
		for (ServerPlayer p : server.getPlayerList().getPlayers()) if (data(p).statics.contains(key)) return true;
		return false;
	}

	/** A spot on the games' map (x, y in steps) laid over a marked place, moved to good ground nearby. */
	private static BlockPos gameSpot(ServerLevel level, Zone z, int[] size, int[] xy) {
		if (xy == null || xy[0] < 0 || xy[1] < 0) return null;
		double fx = (xy[0] + 0.5) / size[0], fz = (xy[1] + 0.5) / size[1];
		int x = z.x1() + (int) Math.floor(fx * (z.x2() - z.x1() + 1)), zz = z.z1() + (int) Math.floor(fz * (z.z2() - z.z1() + 1));
		if (!level.hasChunkAt(new BlockPos(x, START_Y, zz))) return null;
		return spot(level, x, zz, 8);
	}

	/** The way the games had someone facing, as a yaw (north up): down is south. */
	public static float yawOf(String facing, float fallback) {
		return switch (facing) {
			case "down" -> 0F;
			case "left" -> 90F;
			case "up" -> 180F;
			case "right" -> 270F;
			default -> fallback;
		};
	}

	public static Npc place(ServerLevel level, int role, String key, double x, double y, double z, float facing) {
		Npc n = Retromon.NPC.create(level, EntitySpawnReason.COMMAND);
		if (n == null) return null;
		n.setRole(role);
		n.setKey(key);
		n.setPos(x, y, z);
		n.setFacing(facing);
		Trainers.Trainer t = Trainers.get(key);
		n.setCustomName(net.minecraft.network.chat.Component.literal(t != null ? t.title() : Npc.ROLES[role].toUpperCase()));
		n.setCustomNameVisible(false);
		level.addFreshEntity(n);
		return n;
	}

	public static WorldMon placeMon(ServerLevel level, String species, String key, double x, double y, double z, float facing) {
		WorldMon w = Retromon.WORLD_MON.create(level, EntitySpawnReason.COMMAND);
		if (w == null) return null;
		w.setSpecies(species);
		w.setKey(key);
		w.setPos(x, y, z);
		w.setFacing(facing);
		level.addFreshEntity(w);
		return w;
	}

	/** The middle of a place's first zone, to go to (for /retromon go). */
	public static Vec3 centreOf(String place, ServerLevel level) {
		for (Zone z : zones) {
			if (!z.place().equals(place)) continue;
			int x = (z.x1() + z.x2()) / 2, zz = (z.z1() + z.z2()) / 2;
			BlockPos at = standable(level, x, zz, START_Y);
			return Vec3.atBottomCenterOf(at);
		}
		return null;
	}
}
