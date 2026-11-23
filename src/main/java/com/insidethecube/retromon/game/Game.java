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
import net.minecraft.world.level.block.Block;
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
		Interiors.serverStarted(s);
		Path m = marker(s);
		ourSave = Files.exists(m);
		Flags.solidSigns = ourSave;
		STREET.clear();
		heldTime = -1;
		frozenWorld = false;
		DEVICES.clear();
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
		// A new adventure starts in Pallet Town, with empty pockets. Moving you to your room waits a few ticks: this
		// runs while the game is still placing you in the world, and taking you to another dimension part way
		// through that leaves you counted in both (which crashes the server the first time you go back outside).
		if (!data(p).started) {
			p.getInventory().clearContent();
			TO_START.put(p.getUUID(), 3);
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
		ServerLevel level = (ServerLevel) p.level();
		// The tree line (the last few blocks of town at its north edge, where the path to Route 1 starts), or the
		// first step into tall grass, wherever it is: he stops you there and then.
		BlockPos feet = p.blockPosition();
		// (Grass counts in the north half of town only: a tuft by a house down south isn't the way out.)
		boolean inGrass = p.getZ() < (town.z1() + town.z2()) / 2.0 && (grass(level.getBlockState(feet)) || grass(level.getBlockState(feet.above())));
		boolean leaving = outOfTown(level, town, p.getX(), p.getY(), p.getZ());
		if (inTown && !leaving) {
			// Where to put you back: only good ground (not the top of a fence, not in the grass).
			if (p.onGround() && p.getZ() > town.z1() + 10 && goodGround(level, feet)) SAFE.put(p.getUUID(), feet);
			return;
		}
		if (!leaving) return;
		// You stop where you are: back only the step you've just taken, to the last place you stood in town.
		Vec3 last = lastWhere(p, v -> town.contains(v.x, v.z) && !outOfTown(level, town, v.x, v.y, v.z));
		if (last != null) stopAt(p, last);
		else {
			BlockPos back = SAFE.get(p.getUUID());
			if (back == null) {
				int x = Math.max(town.x1() + 4, Math.min(town.x2() - 4, p.getBlockX()));
				back = spot(level, x, town.z1() + 14, 8);
			}
			stopAt(p, new Vec3(back.getX() + 0.5, back.getY(), back.getZ() + 0.5));
		}
		BUSY.put(p.getUUID(), 200);
		CompoundTag t = new CompoundTag();
		t.putBoolean("stop", true);
		Net.send(p, "intro", t);
	}

	/** Whether a spot's out of Pallet Town for Oak: the tree line at its north edge, the north half's tall grass, or another place. */
	private static boolean outOfTown(ServerLevel level, Zone town, double x, double y, double z) {
		if (!town.contains(x, z)) return placeAt(x, z) != null;
		BlockPos feet = BlockPos.containing(x, y, z);
		boolean inGrass = z < (town.z1() + town.z2()) / 2.0 && (grass(level.getBlockState(feet)) || grass(level.getBlockState(feet.above())));
		return z < town.z1() + 7 || inGrass;
	}

	/** Where each player has stood lately, newest first (to stop them where they are, not throw them back). */
	private static final Map<UUID, java.util.ArrayDeque<Vec3>> RECENT = new HashMap<>();

	private static void recent(ServerPlayer p) {
		if (!p.onGround()) return;
		java.util.ArrayDeque<Vec3> r = RECENT.computeIfAbsent(p.getUUID(), k -> new java.util.ArrayDeque<>());
		Vec3 here = p.position();
		if (!r.isEmpty()) {
			double d = r.peekFirst().distanceToSqr(here);
			if (d < 0.04) return;
			if (d > 64) r.clear();
		}
		r.addFirst(here);
		while (r.size() > 40) r.removeLast();
	}

	/** The last place you stood that passes the test, or null. */
	private static Vec3 lastWhere(ServerPlayer p, java.util.function.Predicate<Vec3> ok) {
		java.util.ArrayDeque<Vec3> r = RECENT.get(p.getUUID());
		if (r != null) for (Vec3 v : r) if (ok.test(v)) return v;
		return null;
	}

	/** Stopped where you are: put just there, with nothing carrying you on. */
	private static void stopAt(ServerPlayer p, Vec3 at) {
		p.setDeltaMovement(Vec3.ZERO);
		p.teleportTo(at.x, at.y, at.z);
		java.util.ArrayDeque<Vec3> r = RECENT.get(p.getUUID());
		if (r != null) r.clear();
	}

	/** Oak takes you to his lab: inside it, in front of him and the table of balls. */
	public static void toLab(ServerPlayer p) {
		if (data(p).started) return;
		Interiors.Room lab = Interiors.ROOMS.get("lab");
		if (lab != null && Interiors.go(p, lab, 7.6, 6.2, 180F, false)) {
			BUSY.put(p.getUUID(), 200);
			return;
		}
		if (town() == null) return;
		BlockPos[] door = labDoor((ServerLevel) p.level());
		if (door == null) return;
		BlockPos at = door[1];
		float yaw = (float) Math.toDegrees(Math.atan2(-(door[0].getX() - at.getX()), door[0].getZ() - at.getZ()));
		p.teleportTo((ServerLevel) p.level(), at.getX() + 0.5, at.getY(), at.getZ() + 0.5, java.util.Set.of(), yaw, 0F, true);
		BUSY.put(p.getUUID(), 200);
	}

	/**
	 * Oak's lab's door: the door block nearest where the games had it (their Pallet Town, laid over the marked
	 * town), and good ground just outside it. Without a door there, the games' spot itself (on good ground).
	 * Returns {door, outside}, or null.
	 */
	private static BlockPos[] labDoor(ServerLevel level) {
		Zone town = town();
		int[] size = Places.size("pallet_town");
		if (town == null || size == null) return null;
		BlockPos guess = gameSpot(level, town, size, new int[] {12, 11});
		if (guess == null) return null;
		for (int dx = -12; dx <= 12; dx += 12) for (int dz = -12; dz <= 12; dz += 12) if (!level.hasChunkAt(guess.offset(dx, 0, dz))) return null;
		int street = streetY(level, town);
		BlockPos best = null;
		double bestD = 1e9;
		for (int dx = -12; dx <= 12; dx++) {
			for (int dz = -12; dz <= 12; dz++) {
				for (int y = street - 2; y <= street + 3; y++) {
					BlockPos at = new BlockPos(guess.getX() + dx, y, guess.getZ() + dz);
					BlockState b = level.getBlockState(at);
					if (!(b.getBlock() instanceof net.minecraft.world.level.block.DoorBlock)
							|| b.getValue(net.minecraft.world.level.block.DoorBlock.HALF) != net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER)
						continue;
					double d = dx * dx + dz * dz;
					if (d < bestD) {
						bestD = d;
						best = at;
					}
				}
			}
		}
		if (best != null) {
			// Outside: the side of the door with open sky.
			for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.Plane.HORIZONTAL) {
				for (int k = 1; k <= 2; k++) {
					BlockPos o = best.relative(dir, k);
					for (int y = -1; y <= 1; y++) if (goodGround(level, o.above(y))) return new BlockPos[] {best, o.above(y)};
				}
			}
		}
		BlockPos at = spot(level, guess.getX(), guess.getZ() + 1, 8);
		return new BlockPos[] {at.north(2), at};
	}

	/** Your rival's first Pokémon: the one strong against yours (as in Red and Blue), kept from then on. */
	private static String rivalStarter(ServerPlayer p) {
		PlayerData d = data(p);
		for (String k : d.story) if (k.startsWith("rival:")) return k.substring(6);
		String mine = d.party.isEmpty() ? "charmander" : d.party.get(0).species.id();
		String sp = switch (mine) {
			case "bulbasaur" -> "charmander";
			case "squirtle" -> "bulbasaur";
			case "pikachu" -> "eevee";
			default -> "squirtle";
		};
		if (!d.party.isEmpty()) d.story.add("rival:" + sp);
		return sp;
	}

	private static Trainers.Trainer rivalLab(ServerPlayer p) {
		String sp = rivalStarter(p);
		String name = Species.get(sp).name();
		return new Trainers.Trainer("rival_lab", "pallet_town", rival(p), "blue", "none", 0, 35, List.of(new Trainers.Member(sp, 5)), null,
				List.of("{rival}: Heh, I don't need to be greedy like you!", "{rival}: I'll take this one, then!", "{rival} received a " + name + "!",
						"{rival}: Wait, {player}! Let's check out our POKéMON!", "Come on, I'll take you on!"),
				List.of("WHAT? Unbelievable! I picked the wrong POKéMON!"),
				List.of("{rival}: Okay! I'll make my POKéMON fight to toughen it up!", "{rival}: And you'd better get out there and start your own adventure, {player}!",
						"{player}! Gramps! Smell ya later!"));
	}

	/** Route 22, after the POKéDEX: your rival on his way to the League (Red and Blue's first Route 22 battle). */
	private static Trainers.Trainer rival22(ServerPlayer p) {
		String sp = rivalStarter(p);
		return new Trainers.Trainer("rival_22", "route_22", rival(p), "blue", "none", 0, 35,
				List.of(new Trainers.Member("pidgey", 9), new Trainers.Member(sp, 8)), null,
				List.of("{rival}: Hey! {player}!", "You're going to POKéMON LEAGUE?", "Forget it! You probably don't have any BADGEs!",
						"The guard won't let you through!", "By the way, did your POKéMON get any stronger?"),
				List.of("Awww! You just lucked out!"),
				List.of("{rival}: I heard POKéMON LEAGUE has many tough trainers!", "I have to figure out how to get past them!",
						"You should quit dawdling and get a move on!"));
	}

	/** A trainer by key: the games' trainers, or your rival (made up for you, with his Pokémon). */
	static Trainers.Trainer trainerFor(ServerPlayer p, String key) {
		return switch (key) {
			case "rival_lab" -> rivalLab(p);
			case "rival_22" -> rival22(p);
			case "trainer_house" -> trainerHouse(p);
			default -> Trainers.get(key);
		};
	}

	// ---------- the Trainer House (Viridian City) ----------

	/** Who's waiting there each day (a new one each day, going round), and what they give the winner. */
	private static final String[] TH_NAMES = {"GAVEN", "BETH", "JAKE", "MEGAN", "ALLEN", "LOIS", "KEVIN", "CARA", "PAUL", "ROXY"};
	private static final String[] TH_PRIZES = {"rare_candy", "pp_up", "nugget", "max_potion", "ultra_ball", "full_restore", "rare_candy"};
	/** The kinds of POKéMON they bring (ones with our own looks, from early on). */
	private static final String[] TH_POOL = {"pidgeotto", "raticate", "nidorino", "nidorina", "fearow", "primeape", "growlithe", "psyduck", "poliwhirl", "machop",
			"geodude", "ponyta", "vulpix", "gloom", "weepinbell", "sandshrew", "ekans", "meowth", "clefairy", "jigglypuff", "golbat", "venonat", "kadabra", "pikachu",
			"ivysaur", "charmeleon", "wartortle"};

	/** Today (the real day: the Trainer House's challenger changes at midnight). */
	private static long thDay() {
		return java.time.LocalDate.now().toEpochDay();
	}

	private static String thToday() {
		return "trainer_house:" + thDay();
	}

	/** Today's challenger: their name, and a team as strong as your best (two or three of them). */
	static Trainers.Trainer trainerHouse(ServerPlayer p) {
		PlayerData d = data(p);
		long day = thDay();
		java.util.Random r = new java.util.Random(day * 7919L + 17);
		int top = 5;
		for (Mon m : d.party) top = Math.max(top, m.level);
		int n = Math.min(3, Math.max(2, d.party.size()));
		List<Trainers.Member> team = new ArrayList<>();
		List<String> pool = new ArrayList<>();
		for (String s : TH_POOL) if (Species.ALL.containsKey(s)) pool.add(s);
		for (int i = 0; i < n && !pool.isEmpty(); i++)
			team.add(new Trainers.Member(pool.remove(r.nextInt(pool.size())), Math.max(5, Math.min(100, top - (i == n - 1 ? 0 : 1 + r.nextInt(2))))));
		if (team.isEmpty()) team.add(new Trainers.Member("rattata", top));
		String name = TH_NAMES[(int) Math.floorMod(day, TH_NAMES.length)];
		return new Trainers.Trainer("trainer_house", "viridian_city", "COOLTRAINER " + name, "cooltrainer_m", "down", 0, 40, team, null,
				List.of("I'm " + name + ", today's challenger at the TRAINER HOUSE!", "Think you can beat me? Let's find out!"),
				List.of("You're really good!"), List.of("That was a great battle!"));
	}

	/** Today's prize (the same all day). */
	private static String thPrize() {
		return TH_PRIZES[(int) Math.floorMod(thDay(), TH_PRIZES.length)];
	}

	/** You beat today's challenger: the prize, and they're done for the day. */
	static List<String> trainerHouseWon(ServerPlayer p) {
		PlayerData d = data(p);
		d.story.add(thToday());
		String prize = thPrize();
		d.give(prize, 1);
		Item it = Item.find(prize);
		String nm = it == null ? prize.toUpperCase() : it.name();
		return List.of("You won the TRAINER HOUSE's prize!", player(p) + " received a " + nm + "!");
	}

	/** After your first Pokémon gets its name: your rival turns up, takes his, and walks over to battle you. */
	private static void rivalChallenge(ServerPlayer p) {
		PlayerData d = data(p);
		if (d.party.isEmpty() || d.beaten.contains("rival_lab")) return;
		Trainers.Trainer t = rivalLab(p);
		ServerLevel level = (ServerLevel) p.level();
		// He comes from the side of you away from Oak, so he never walks through him.
		Vec3 from = new Vec3(5, 0, 1);
		for (Npc oak : level.getEntitiesOfClass(Npc.class, p.getBoundingBox().inflate(14), o -> o.key().equals(OAK_KEY))) {
			Vec3 away = p.position().subtract(oak.position()).multiply(1, 0, 1);
			if (away.lengthSqr() > 0.01) from = away.normalize().scale(5);
		}
		// Inside (the lab): he comes in through the door and walks over to you.
		Vec3 entry = Interiors.entryOf(p);
		BlockPos at = entry != null ? BlockPos.containing(entry.x, entry.y + 0.1, entry.z)
				: spot(level, (int) Math.floor(p.getX() + from.x), (int) Math.floor(p.getZ() + from.z), 3);
		Npc n = rivalNpc(p, "rival_lab", at);
		if (n == null) talkBattle(p, null, t);
		else walkUp(p, n, t, "meet_rival", null);
	}

	// ---------- your rival in person ----------

	/** Where your rival came from (Route 22: along the route), so he goes back that way. */
	private static final Map<UUID, Vec3> RIVAL_FROM = new HashMap<>();

	/**
	 * Good ground along the way you'd see someone come (dir: -1 0 west), from far to near, on your own level (a step up
	 * or down at most) and in plain sight of you, so they walk up the route rather than down a cliff; or null.
	 */
	private static BlockPos alongRoute(ServerLevel level, ServerPlayer p, int dirX, int dirZ, int far, int near) {
		BlockPos at = p.blockPosition();
		for (int d = far; d >= near; d--)
			for (int side : new int[] {0, 1, -1, 2, -2})
				for (int dy : new int[] {0, -1, 1}) {
					BlockPos c = at.offset(dirX * d + dirZ * side, dy, dirZ * d + dirX * side);
					if (!goodGround(level, c)) continue;
					Vec3 eye = new Vec3(c.getX() + 0.5, c.getY() + 1.5, c.getZ() + 0.5);
					var hit = level.clip(new net.minecraft.world.level.ClipContext(eye, p.getEyePosition(), net.minecraft.world.level.ClipContext.Block.COLLIDER,
							net.minecraft.world.level.ClipContext.Fluid.NONE, p));
					if (hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS) return c;
				}
		return null;
	}

	/** Each player's rival when he's about (in the lab, on Route 22, at Oak's), and ticks left as he heads off. */
	private static final Map<UUID, Npc> RIVALS = new HashMap<>();
	private static final Map<UUID, Integer> RIVAL_LEAVING = new HashMap<>();
	private static final int LEAVE_TICKS = 230, WALK_OFF = LEAVE_TICKS - 8;
	/** What your rival says to you, back in the world after you beat him, before he heads off. */
	private static final Map<UUID, List<String>> RIVAL_SPEECH = new HashMap<>();

	/** Your rival's words after a battle you won: said to you face to face once the battle's closed, then he goes. */
	public static void rivalSpeech(ServerPlayer p, List<String> lines) {
		if (lines != null && !lines.isEmpty()) RIVAL_SPEECH.put(p.getUUID(), List.copyOf(lines));
	}
	/** Who's still looking at their battle's last words (till the battle screen closes, or 30 s). */
	private static final Map<UUID, Integer> AFTER_BATTLE = new HashMap<>();

	/**
	 * The battle's done with on your screen (its cheering, the Pokédex entry, the nickname, any new moves or evolving):
	 * the world goes on (your rival can head off). The wild ones right by you scatter, and a few seconds pass before
	 * another can start a battle, so one never runs straight into the next.
	 */
	public static void battleClosed(ServerPlayer p) {
		AFTER_BATTLE.remove(p.getUUID());
		COOLDOWN.put(p.getUUID(), 100);
		if (p.level() instanceof ServerLevel level) {
			String tag = "wild:" + p.getUUID();
			for (WorldMon w : level.getEntitiesOfClass(WorldMon.class, p.getBoundingBox().inflate(6), w -> tag.equals(w.key()))) vanish(level, w);
		}
	}

	/** When each player last walked (a tick count): a wild one only starts a battle when you walk into it, not by wandering into you. */
	private static final Map<UUID, Integer> WALKING = new HashMap<>();
	private static final Map<UUID, Vec3> WAS_AT = new HashMap<>();

	/** Whether a rival in the world is one the game's using (else he's left over in a save, and goes). */
	static boolean rivalKnown(Npc n) {
		return RIVALS.containsValue(n);
	}

	private static Npc rivalNpc(ServerPlayer p, String key, BlockPos at) {
		dropRival(p);
		Npc n = place((ServerLevel) p.level(), Npc.TRAINER, key, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0F);
		if (n == null) return null;
		n.setCustomName(net.minecraft.network.chat.Component.literal(rival(p)));
		n.setFacing(n.yawTo(p));
		RIVALS.put(p.getUUID(), n);
		return n;
	}

	private static void dropRival(ServerPlayer p) {
		Npc old = RIVALS.remove(p.getUUID());
		if (old != null && !old.isRemoved()) old.discard();
		RIVAL_LEAVING.remove(p.getUUID());
	}

	/** Once you're done with him (the battle, the visit to Oak's), your rival heads off and is gone. */
	private static void rivalsLeave(MinecraftServer s) {
		for (Map.Entry<UUID, Npc> e : List.copyOf(RIVALS.entrySet())) {
			UUID id = e.getKey();
			Npc n = e.getValue();
			ServerPlayer p = s.getPlayerList().getPlayer(id);
			if (p == null || n.isRemoved()) {
				if (n != null && !n.isRemoved()) n.discard();
				RIVALS.remove(id);
				RIVAL_LEAVING.remove(id);
				continue;
			}
			if (engaged(p) || WALKS.containsKey(id) || AFTER_BATTLE.containsKey(id)) continue;
			PlayerData d = data(p);
			boolean done = switch (n.key()) {
				case "rival_lab" -> d.beaten.contains("rival_lab");
				case "rival_22" -> d.beaten.contains("rival_22");
				default -> d.hasDex() && d.beaten.contains("rival_lab");
			};
			if (!done) continue;
			// He turns to you and has his say, then walks straight off, and is gone once he's out of the door.
			Integer left = RIVAL_LEAVING.get(id);
			if (left == null) {
				n.getNavigation().stop();
				n.setFacing(n.yawTo(p));
				RIVAL_LEAVING.put(id, LEAVE_TICKS);
				List<String> say = RIVAL_SPEECH.remove(id);
				if (say != null) dialogue(p, n, "talk", say);
			} else if (left == WALK_OFF) {
				// Inside, he heads out the way he came in, through the door; outside, off away from you.
				Vec3 out = Interiors.exitOf(n);
				Vec3 from = RIVAL_FROM.remove(id);
				if (out != null) n.getNavigation().moveTo(out.x, out.y, out.z, 0.9);
				else if (from != null) {
					// Back the way he came, along the route, and on past it.
					Vec3 away = from.subtract(p.position()).multiply(1, 0, 1);
					away = away.lengthSqr() < 0.01 ? new Vec3(-1, 0, 0) : away.normalize();
					Vec3 to = from.add(away.scale(14));
					n.getNavigation().moveTo(to.x, from.y, to.z, 0.9);
				} else {
					Vec3 away = n.position().subtract(p.position()).multiply(1, 0, 1);
					away = away.lengthSqr() < 0.01 ? new Vec3(-1, 0, 0) : away.normalize();
					Vec3 to = n.position().add(away.scale(24));
					n.getNavigation().moveTo(to.x, n.getY(), to.z, 0.9);
				}
				RIVAL_LEAVING.put(id, left - 1);
			}
			// While he walks off: still a cutscene (the black bars, you stood still) till he's gone.
			if (left != null && left <= WALK_OFF && s.getTickCount() % 5 == 0) {
				CompoundTag hold = new CompoundTag();
				hold.putInt("ticks", 12);
				Net.send(p, "hold", hold);
			}
			if (left == null || left == WALK_OFF) {
			} else if (left < WALK_OFF && Interiors.exitOf(n) != null
					&& (n.position().distanceTo(Interiors.exitOf(n)) < 2.2 || n.getNavigation().isDone() && n.position().distanceTo(Interiors.exitOf(n)) < 4.5)) {
				// Out through the door.
				n.discard();
				RIVALS.remove(id);
				RIVAL_LEAVING.remove(id);
			} else if (left <= 0 || (left < WALK_OFF && n.distanceTo(p) > 22)) {
				n.discard();
				RIVALS.remove(id);
				RIVAL_LEAVING.remove(id);
			} else RIVAL_LEAVING.put(id, left - 1);
		}
	}

	public static Trainers.Trainer rivalTrainer(ServerPlayer p) {
		return rivalLab(p);
	}


	public static void leave(ServerPlayer p) {
		DEVICES.remove(p.getUUID());
		Battles.drop(p);
		LAST.remove(p.getUUID());
		BUSY.remove(p.getUUID());
		RECENT.remove(p.getUUID());
		AFTER_BATTLE.remove(p.getUUID());
		WALKING.remove(p.getUUID());
		WAS_AT.remove(p.getUUID());
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
		CompoundTag t = data(p).save();
		// The marked places too, for the Town Map.
		net.minecraft.nbt.ListTag zs = new net.minecraft.nbt.ListTag();
		for (Zone z : zones) {
			CompoundTag o = new CompoundTag();
			o.putString("place", z.place());
			o.putInt("x1", z.x1());
			o.putInt("z1", z.z1());
			o.putInt("x2", z.x2());
			o.putInt("z2", z.z2());
			zs.add(o);
		}
		t.put("zones", zs);
		Net.send(p, "sync", t);
	}

	/** Everyone's map brought up to date (a place has been marked or cleared). */
	private static void syncAll() {
		if (server == null) return;
		for (ServerPlayer q : server.getPlayerList().getPlayers()) if (adventure(q)) sync(q);
	}

	/** Who has their Pokédex out (the clock stands still while it is, as in a battle). */
	private static final java.util.Set<UUID> DEVICES = new java.util.HashSet<>();

	public static void device(ServerPlayer p, boolean on) {
		if (on && adventure(p)) DEVICES.add(p.getUUID());
		else DEVICES.remove(p.getUUID());
	}

	/** The game's speed: 1 to 4 times (everything, as an emulator's fast-forward does). */
	public static void setSpeed(int times) {
		if (server == null) return;
		int k = Math.max(1, Math.min(4, times));
		server.tickRateManager().setTickRate(20F * k);
		// Minecraft never runs your own side faster than 20 ticks a second, so you'd walk at your usual pace while
		// the world hurried: you walk (and run) faster by the same amount instead.
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			AttributeInstance move = p.getAttribute(Attributes.MOVEMENT_SPEED);
			if (move == null) continue;
			move.removeModifier(SPEED_ID);
			if (k > 1) move.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(SPEED_ID, k - 1,
					net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		}
	}

	private static final net.minecraft.resources.Identifier SPEED_ID = Retromon.id("speed_up");

	/** Map editing (`/retromon build on`): off, nobody can break or change the map in a Retromon save. */
	public static boolean build;

	/** Whether this player is kept from breaking or changing anything (a Retromon save, not in build mode). */
	public static boolean locked(net.minecraft.world.entity.player.Player p) {
		return ourSave && !build && p instanceof ServerPlayer;
	}

	/** Pallet Town, outside your house. */
	public static final int START_X = -806, START_Y = 18, START_Z = 700;

	/** Players waiting to be taken to their room (ticks left), once they're properly in the world. */
	private static final Map<UUID, Integer> TO_START = new HashMap<>();

	/**
	 * Just before you're taken to another dimension (into a building or back out): what this side remembers about
	 * where you've been is dropped, and your partner walking with you goes (it comes back beside you there).
	 */
	public static void leavingLevel(ServerPlayer p) {
		UUID id = p.getUUID();
		LAST.remove(id);
		RECENT.remove(id);
		TRAIL.remove(id);
		Integer f = FOLLOWERS.remove(id);
		if (f != null && p.level().getEntity(f) instanceof WorldMon w) w.discard();
	}

	private static void toStart(ServerPlayer p) {
		// Your room, upstairs at home (an inside of its own; your world's outside is outside its door).
		if (Interiors.start(p)) return;
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
		// (17:51) Not up on a cliff (the map's cliffs and ledges are dripstone, mushroom block and jungle stairs, the
		// garden beds mud), and not in a flower bed (PEWTER's man in the roses).
		if (offGround(level, at)) return false;
		// Not a roof: real ground has more under it (a roof has the room beneath it).
		return !level.getBlockState(at.below(2)).isAir() && !level.getBlockState(at.below(3)).isAir() && level.canSeeSky(at);
	}

	/** Standing on a cliff top or a ledge (rock the map's cliffs are made of), a garden bed, or among flowers. */
	private static boolean offGround(ServerLevel level, BlockPos at) {
		BlockState b = level.getBlockState(at.below());
		if (b.is(net.minecraft.world.level.block.Blocks.DRIPSTONE_BLOCK) || b.is(net.minecraft.world.level.block.Blocks.BROWN_MUSHROOM_BLOCK)
				|| b.is(net.minecraft.world.level.block.Blocks.MUD) || b.is(net.minecraft.world.level.block.Blocks.POINTED_DRIPSTONE)
				|| b.is(net.minecraft.world.level.block.Blocks.JUNGLE_STAIRS) || b.is(net.minecraft.world.level.block.Blocks.JUNGLE_SLAB)
				|| b.is(net.minecraft.world.level.block.Blocks.MUD_BRICKS)) return true;
		BlockState feet = level.getBlockState(at);
		return feet.is(net.minecraft.tags.BlockTags.FLOWERS) || feet.is(net.minecraft.world.level.block.Blocks.ROSE_BUSH)
				|| feet.is(net.minecraft.world.level.block.Blocks.PEONY) || feet.is(net.minecraft.world.level.block.Blocks.LILAC);
	}

	/**
	 * The nearest good spot to stand around x, z (on the ground, not a roof or a treetop), searching outwards
	 * up to `radius` blocks. The ground height comes from the map itself.
	 */
	public static BlockPos spot(ServerLevel level, int x, int z, int radius) {
		if (level.dimension() == Interiors.LEVEL) return Interiors.spot(level, x, z, radius);
		Zone zone = null;
		for (int i = zones.size() - 1; i >= 0 && zone == null; i--) if (zones.get(i).contains(x, z)) zone = zones.get(i);
		// (15:21) On a route the ground where they stand: a route's ground rises and falls (Route 4 up to MT. MOON), so its
		// commonest height put the woman by the POKéMON CENTER on its roof.
		if (zone != null && zone.place().startsWith("route_")) {
			BlockPos own = spotAny(level, x, z, radius);
			if (own != null) return own;
		}
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

	/**
	 * The nearest good ground near a height (searching outwards): first anywhere within a block of it (the
	 * street), and only then two blocks up or down (so it's the pavement in front of an arch, not its top).
	 */
	private static BlockPos spotNear(ServerLevel level, int x, int z, int radius, int y0) {
		for (int[] heights : new int[][] {{0, 1, -1}, {2, -2}}) {
			for (int r = 0; r <= radius; r++) {
				for (int dx = -r; dx <= r; dx++) {
					for (int dz = -r; dz <= r; dz++) {
						if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
						for (int dy : heights) {
							BlockPos at = new BlockPos(x + dx, y0 + dy, z + dz);
							if (goodGround(level, at)) return at;
						}
					}
				}
			}
		}
		return null;
	}

	private static Zone zoneAt(int x, int z) {
		for (int i = zones.size() - 1; i >= 0; i--) if (zones.get(i).contains(x, z)) return zones.get(i);
		return null;
	}

	/**
	 * Someone standing where they shouldn't from an older placing (the nurse on the Pokémon Center's arch, a man
	 * in a pond): moved to good ground nearby, which becomes their spot. Checked once each time they're loaded.
	 */
	private static final java.util.Set<String> RESPOT = java.util.Set.of("pewter_city_p3", "cerulean_city_p7");

	static boolean settle(Npc n) {
		if (!ourSave || n.role() == Npc.LEADER || n.key().startsWith("rival") || !(n.level() instanceof ServerLevel level)) return true;
		BlockPos at = n.blockPosition();
		Zone zone = zoneAt(at.getX(), at.getZ());
		if (zone == null) return true;
		// Not till the place is loaded round them (so nothing's loaded just for this); tried again next tick.
		for (int dx = -9; dx <= 9; dx += 9) for (int dz = -9; dz <= 9; dz += 9) if (!level.hasChunkAt(at.offset(dx, 0, dz))) return false;
		if (!level.hasChunkAt(new BlockPos(zone.x1(), at.getY(), zone.z1())) || !level.hasChunkAt(new BlockPos(zone.x2(), at.getY(), zone.z2()))) return false;
		String pl = zone.place();
		boolean town = pl.endsWith("_city") || pl.endsWith("_town");
		if (!town && !pl.startsWith("route_")) return true;
		boolean wet = n.isInWater() || !level.getFluidState(at).isEmpty() || !level.getFluidState(at.below()).isEmpty();
		boolean high = (town || n.role() == Npc.NURSE || n.role() == Npc.CLERK) && at.getY() > streetY(level, zone) + 1;
		// (17:51) Moved to their own spot since (PEWTER's man out of the roses, CERULEAN CAVE's guard in front of it).
		int[] own = RESPOT.contains(n.key()) ? spotOf(n.key()) : null;
		if (own != null && Math.hypot(own[0] - at.getX(), own[1] - at.getZ()) > 3) {
			BlockPos to = spot(level, own[0], own[1], 6);
			n.teleportTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5);
			n.rehome(to);
			return true;
		}
		// (17:51) Up on a cliff (ROUTE 3, PEWTER) or in a flower bed: down to the ground nearby.
		boolean off = offGround(level, at);
		if (!wet && !high && !off) return true;
		BlockPos to = spot(level, at.getX(), at.getZ(), 8);
		if (!to.equals(at)) n.rehome(to);
		return true;
	}

	private static BlockPos spotAny(ServerLevel level, int x, int z, int radius) {
		for (int r = 0; r <= radius; r++) {
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
					int top = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x + dx, z + dz);
					for (int y = top; y >= top - 12; y--) {
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
			new Zone("viridian_city", -830, 495, -745, 589), new Zone("route_22", -900, 495, -831, 565), new Zone("route_22", -1000, 512, -901, 600), new Zone("route_2", -830, 340, -745, 494),
			new Zone("pewter_city", -850, 255, -741, 339), new Zone("route_3", -740, 285, -580, 332), new Zone("route_4", -628, 238, -410, 284),
			new Zone("fuchsia_city", -560, 733, -441, 860), new Zone("cerulean_city", -401, 220, -311, 305), new Zone("route_24", -372, 140, -332, 219),
			new Zone("route_25", -331, 125, -160, 175), new Zone("route_5", -372, 306, -330, 376), new Zone("route_6", -385, 472, -310, 545),
			new Zone("route_7", -455, 400, -410, 450), new Zone("route_8", -300, 395, -215, 455), new Zone("route_10", -165, 296, -110, 345),
			new Zone("vermilion_city", -402, 546, -300, 614), new Zone("lavender_town", -158, 398, -104, 447),
			// (0.50) Routes 11 to 18: Vermilion east to Lavender's south, round to Fuchsia, and the CYCLING ROAD.
			new Zone("route_11", -299, 556, -142, 612), new Zone("route_12", -142, 448, -100, 720), new Zone("route_13", -300, 688, -143, 728),
			new Zone("route_14", -305, 729, -250, 805), new Zone("route_15", -440, 758, -306, 805), new Zone("route_16", -680, 388, -560, 444),
			new Zone("route_17", -665, 445, -615, 757), new Zone("route_18", -665, 758, -561, 805)};

	private static void loadZones() {
		zones.clear();
		if (marker.has("zones")) {
			for (JsonElement e : marker.getAsJsonArray("zones")) {
				JsonObject o = e.getAsJsonObject();
				zones.add(new Zone(o.get("place").getAsString(), o.get("x1").getAsInt(), o.get("z1").getAsInt(), o.get("x2").getAsInt(), o.get("z2").getAsInt()));
			}
			// Route 22 out to its west end (its sign, the League gate, the Route 28 Gate), in saves from before.
			if (placeAt(-919.5, 550.5) == null) {
				zones.add(0, ROUTE_22_WEST);
				saveMarker();
			}
			// Pewter City, in saves from before it was marked.
			if (!"pewter_city".equals(placeAt(-800.5, 300.5))) {
				zones.add(PEWTER);
				saveMarker();
			}
			// Routes 3 and 4 (13:56).
			if (!"route_3".equals(placeAt(-690.5, 300.5))) {
				zones.add(ROUTE_3);
				saveMarker();
			}
			if (!"route_4".equals(placeAt(-600.5, 262.5))) {
				zones.add(ROUTE_4);
				saveMarker();
			}
			// Fuchsia City (15:29).
			if (!"fuchsia_city".equals(placeAt(-500.5, 800.5))) {
				zones.add(FUCHSIA);
				saveMarker();
			}
			// Cerulean City, Routes 24, 25 and 5, and Routes 6, 7 and 8 (16:17 and 16:20).
			for (Zone cz : CERULEAN_ZONES) {
				double mx = (cz.x1() + cz.x2()) / 2.0 + 0.5, mz = (cz.z1() + cz.z2()) / 2.0 + 0.5;
				if (!cz.place().equals(placeAt(mx, mz))) {
					zones.add(cz);
					saveMarker();
				}
			}
		} else {
			zones.addAll(List.of(DEFAULT_ZONES));
			saveMarker();
		}
	}

	private static final Zone ROUTE_22_WEST = new Zone("route_22", -1000, 512, -901, 600), PEWTER = new Zone("pewter_city", -850, 255, -741, 339),
			ROUTE_3 = new Zone("route_3", -740, 285, -580, 332), ROUTE_4 = new Zone("route_4", -628, 238, -410, 284),
			FUCHSIA = new Zone("fuchsia_city", -560, 733, -441, 860);
	/** Cerulean City and the routes round it (16:17), and the routes the Underground Paths come out on (16:20). */
	private static final Zone[] CERULEAN_ZONES = {new Zone("cerulean_city", -401, 220, -311, 305), new Zone("route_24", -372, 140, -332, 219),
			new Zone("route_25", -331, 125, -160, 175), new Zone("route_5", -372, 306, -330, 376), new Zone("route_6", -385, 472, -310, 545),
			new Zone("route_7", -455, 400, -410, 450), new Zone("route_8", -300, 395, -215, 455), new Zone("route_10", -165, 296, -110, 345),
			// (17:51) Vermilion City.
			new Zone("vermilion_city", -402, 546, -300, 614), new Zone("lavender_town", -158, 398, -104, 447),
			// (0.50) Routes 11 to 18: Vermilion east to Lavender's south, round to Fuchsia, and the CYCLING ROAD.
			new Zone("route_11", -299, 556, -142, 612), new Zone("route_12", -142, 448, -100, 720), new Zone("route_13", -300, 688, -143, 728),
			new Zone("route_14", -305, 729, -250, 805), new Zone("route_15", -440, 758, -306, 805), new Zone("route_16", -680, 388, -560, 444),
			new Zone("route_17", -665, 445, -615, 757), new Zone("route_18", -665, 758, -561, 805)};

	public static String placeAt(double x, double z) {
		for (int i = zones.size() - 1; i >= 0; i--) if (zones.get(i).contains(x, z)) return zones.get(i).place();
		return null;
	}

	/** The Town Map's places (data/map_pins.tsv): key, x, z, and how much nearer a town counts (spots count further). */
	private static List<Object[]> pins;

	/**
	 * Where a spot is for the place pill: its marked place, else the Town Map's place nearest it (a town's pin counts
	 * as a little nearer, a cave's or a tower's a little further, as they cover less), within 300 blocks. A pin with no
	 * place of its own (MT.MOON) is its landmark's first place (MT.MOON 1F).
	 */
	public static String placeNear(double x, double z) {
		String marked = placeAt(x, z);
		if (marked != null) return marked;
		if (pins == null) {
			java.util.Map<String, String> byLandmark = new java.util.HashMap<>();
			for (String[] r : Species.table("place_landmarks.tsv")) if (r.length > 1) byLandmark.putIfAbsent(r[1], r[0]);
			List<Object[]> ps = new ArrayList<>();
			for (String[] r : Species.table("map_pins.tsv")) {
				if (r.length < 5) continue;
				String key = r[1];
				if (Places.get(key) == null) key = byLandmark.getOrDefault(key, key);
				double k = r[4].equals("town") ? 0.8 : r[4].equals("spot") ? 1.35 : 1.0;
				ps.add(new Object[] {key, Double.parseDouble(r[2].trim()), Double.parseDouble(r[3].trim()), k});
			}
			pins = ps;
		}
		String best = null;
		double bd = 300;
		for (Object[] o : pins) {
			double d = Math.hypot((Double) o[1] - x, (Double) o[2] - z) * (Double) o[3];
			if (d < bd) {
				bd = d;
				best = (String) o[0];
			}
		}
		return best;
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
		syncAll();
		return true;
	}

	public static int clearZones(String place) {
		int before = zones.size();
		zones.removeIf(z -> z.place().equals(place));
		saveMarker();
		syncAll();
		return before - zones.size();
	}

	/**
	 * Into a building's room: its own music (Oak's lab has the lab's; homes, their town's), and from outside, its
	 * name in the place banner (PROF. OAK's LAB, YOUR HOUSE, BLUE's HOUSE).
	 */
	public static void roomEntered(ServerPlayer p, Interiors.Room r, boolean banner) {
		PlayerData dd = data(p);
		if (p.level() instanceof ServerLevel lv0) Interiors.people(lv0, r, p);
		if (r.id().equals("lab") && dd.started && (dd.story.contains("parcel") || dd.hasDex()) && !dd.beaten.contains("rival_lab") && !dd.party.isEmpty()
				&& !RIVALS.containsKey(p.getUUID()) && p.level() instanceof ServerLevel lv) {
			Npc rv = rivalNpc(p, "rival_lab", BlockPos.containing(r.ox() + 10.2, r.oy(), r.oz() + 4.6));
			if (rv != null) {
				rv.setFacing(90F);
				rv.rehome(rv.blockPosition());
			}
		}
		// A room that's a place of its own (Viridian Forest, Route 2's gates): you're there now (its wild POKéMON, its name).
		// A Center or Mart is in the town whose door you came in by.
		String rp = Interiors.placeFor(p, r);
		if (rp == null) rp = r.place();
		if (!rp.isEmpty() && !rp.equals(data(p).place)) enter(p, rp);
		Places.Place pl = Places.get(rp);
		CompoundTag t = new CompoundTag();
		String name = fill(p, List.of(r.name().replace("Rival's", "{rival}'s"))).get(0).toUpperCase().replace("POKÉ", "POKé");
		t.putString("name", banner ? name : "");
		String nm = r.name().toLowerCase(java.util.Locale.ROOT);
		t.putString("music", nm.contains("lab") ? "oaks_lab" : nm.contains("center") ? "pokecenter" : pl == null ? "" : pl.music());
		Net.send(p, "room", t);
	}

	/** Back outside: the place's own music again, and its name back on the pill (without its banner). */
	public static void roomLeft(ServerPlayer p) {
		Places.Place pl = Places.get(data(p).place);
		CompoundTag t = new CompoundTag();
		t.putString("name", "");
		t.putString("here", pl == null ? data(p).place.toUpperCase().replace('_', ' ') : pl.name());
		t.putString("music", pl == null ? "" : pl.music());
		Net.send(p, "room", t);
	}

	private static void enter(ServerPlayer p, String place) {
		PlayerData d = data(p);
		if (place == null || place.equals(d.place)) return;
		if (oldManStops(p, place)) return;
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
		// HeartGold's mailboxes by Pallet Town's houses, put in place once that part of the map is loaded.
		if (ourSave && s.getTickCount() % 40 == 0) Mailbox.place(s.overworld());
		for (ServerPlayer p : s.getPlayerList().getPlayers()) {
			if (!adventure(p)) continue;
			UUID id = p.getUUID();
			p.getFoodData().setFoodLevel(20);
			if (data(p).day0 < 0) {
				long now = s.overworld().getDayTime();
				data(p).day0 = now - Math.floorMod(now, 24000L);
				sync(p);
			}
			data(p).played += 1.0 / Math.max(1F, s.tickRateManager().tickrate());
			if (p.getHealth() < p.getMaxHealth()) p.setHealth(p.getMaxHealth());
			COOLDOWN.computeIfPresent(id, (k, v) -> v > 1 ? v - 1 : null);
			BUSY.computeIfPresent(id, (k, v) -> v > 1 ? v - 1 : null);
			AFTER_BATTLE.computeIfPresent(id, (k, v) -> v > 1 ? v - 1 : null);
			Integer ts = TO_START.get(id);
			if (ts != null) {
				if (ts > 1) TO_START.put(id, ts - 1);
				else {
					TO_START.remove(id);
					if (!data(p).started) toStart(p);
				}
				continue;
			}
			recent(p);
			// Taken through a door this tick: nothing else about where you were applies until next tick.
			if (Interiors.tick(p)) continue;
			// Inside a building, the outside's checks (its places, Oak at the town's edge, grass, trainers' sight) don't apply.
			boolean inside = Interiors.in(p);
			if (!inside && s.getTickCount() % 2 == 0) enter(p, placeNear(p.getX(), p.getZ()));
			if (!inside && !data(p).started && !BUSY.containsKey(id)) oakStops(p);
			if (!inside && s.getTickCount() % 2 == 0 && !BUSY.containsKey(id)) gymShut(p);
			if (!inside && !build && !Battles.in(p)) keepAshore(p);
			// Route 2's CUT trees: no way past them until they're cut.
			if (!inside && !build && !Battles.in(p)) CutTrees.keepOff(p);
			// (0.50) The sleeping SNORLAX (Routes 12 and 16) and the S.S. ANNE's sailor: no way past.
			if (!inside && !build && !Battles.in(p)) blockers(p);
			// (The forest's a room you walk about in as outside: its grass and its trainers' eyes apply.)
			boolean walkabout = inside && Interiors.walkabout(p);
			if (s.getTickCount() % 5 == 0) story(p);
			if (s.getTickCount() % 40 == 0) moonlight(p);
			follower(p);
			if (s.getTickCount() % 10 == 0) roamers(p);
			// Kept in Adventure mode (so nothing on the map can be broken, Axiom included) unless map editing is on.
			if (!build && s.getTickCount() % 20 == 0 && p.gameMode.getGameModeForPlayer() != GameType.ADVENTURE) p.setGameMode(GameType.ADVENTURE);
			Vec3 was = WAS_AT.put(id, p.position());
			if (was != null && was.distanceToSqr(p.position().x, was.y, p.position().z) > 0.0004) WALKING.put(id, s.getTickCount());
			// A room with trainers in it (the Pewter Gym's CAMPER LIAM): they watch for you there too.
			if (inside && !walkabout && !Battles.in(p) && !BUSY.containsKey(id) && !AFTER_BATTLE.containsKey(id) && Interiors.watched(p)) watch(p);
			// The Museum's desk: your ticket, the first time you go in by the main door.
			if (inside && !Battles.in(p) && !BUSY.containsKey(id)) museumDesk(p);
			// The SAFARI ZONE (15:29): which of its areas you're in; out of SAFARI BALLs, the game's over.
			if (inside && s.getTickCount() % 10 == 0 && !Battles.in(p)) safariTick(p);
			// Pewter City's east way out before you've beaten BROCK: the boy takes you to the GYM.
			if (!inside && !build && !Battles.in(p) && !BUSY.containsKey(id)) pewterGuide(p);
			if (Battles.in(p) || BUSY.containsKey(id) || inside && !walkabout || AFTER_BATTLE.containsKey(id)) {
				LAST.put(id, p.blockPosition());
				continue;
			}
			BlockPos pos = p.blockPosition();
			BlockPos last = LAST.put(id, pos);
			if (last != null && (last.getX() != pos.getX() || last.getZ() != pos.getZ())) step(p, pos);
			if (touchWild(p)) continue;
			if (inside || s.getTickCount() % 2 == 0) watch(p);
		}
		approach(s);
		escorts();
		mumsGoHome();
		rivalsLeave(s);
		rocketsLeave(s);
		if (s.getTickCount() % 40 == 0) placeTouchPoints(s);
		holdTime(s);
	}

	/** Where each player last stood on dry ground (outside), to be put back to if they step into the water. */
	private static final Map<UUID, Vec3> ASHORE = new HashMap<>();

	/**
	 * As in the games, there's no walking into the water (no SURF here): a step into it (off a pier, down the steps
	 * into the sea, even onto the steps standing in it) puts you back where you last stood on dry ground, so the
	 * water's edge is a wall. In Pallet Town, the dripstone round its pond and along its shore is the town's edge too
	 * (its fences stop short of it, for looks): no walking on it, so nobody goes past the fences and down to the sea.
	 */
	private static void keepAshore(ServerPlayer p) {
		ServerLevel lv = (ServerLevel) p.level();
		BlockPos feet = BlockPos.containing(p.getX(), p.getY() + 0.1, p.getZ());
		// (The water itself, and the steps standing in it: jungle stairs in the water on the dripstone, all over the
		// map. Not a waterlogged slab or step on land, a fountain's say.)
		java.util.function.Predicate<BlockPos> sea = b -> {
			net.minecraft.world.level.block.state.BlockState st = lv.getBlockState(b);
			return st.is(net.minecraft.world.level.block.Blocks.WATER) || st.is(net.minecraft.world.level.block.Blocks.JUNGLE_STAIRS) && !lv.getFluidState(b).isEmpty();
		};
		boolean wet = sea.test(feet) || sea.test(feet.above());
		if (!wet && !p.onGround() && p.getDeltaMovement().y < 0) {
			// Dropping off something over the water: back before the splash.
			for (int d = 1; d <= 3 && !wet; d++) {
				BlockPos b = feet.below(d);
				wet = sea.test(b);
				if (!lv.getBlockState(b).getCollisionShape(lv, b).isEmpty()) break;
			}
		}
		boolean edge = false;
		if (!wet && p.onGround() && "pallet_town".equals(data(p).place)) {
			// Pallet's dripstone round the water (its pond and its shore): not walked on, and no squeezing round a
			// fence's end post over the corner of it (a side of you over it counts, not only your middle).
			for (double[] o : new double[][] {{0, 0}, {-0.2, -0.2}, {0.2, -0.2}, {-0.2, 0.2}, {0.2, 0.2}}) {
				BlockPos under = BlockPos.containing(p.getX() + o[0], p.getY() - 0.2, p.getZ() + o[1]);
				if (lv.getBlockState(under).is(net.minecraft.world.level.block.Blocks.DRIPSTONE_BLOCK)
						&& lv.getBlockState(under.above()).getCollisionShape(lv, under.above()).isEmpty()) {
					edge = true;
					break;
				}
			}
		}
		UUID id = p.getUUID();
		Vec3 dry = ASHORE.get(id);
		if (dry != null && dry.distanceToSqr(p.position()) > 8 * 8) {
			// Moved a long way (FAST TRAVEL, a door, a blackout): not a step into the water.
			ASHORE.remove(id);
			dry = null;
		}
		if (wet || edge) {
			if (dry != null) {
				p.teleportTo(dry.x, dry.y, dry.z);
				p.setDeltaMovement(Vec3.ZERO);
			}
			return;
		}
		if (p.onGround()) ASHORE.put(id, p.position());
	}

	/** Each player's NIGHTS setting (0 dark, 1 soft, 2 bright), sent from their OPTION. */
	private static final Map<UUID, Integer> NIGHTS = new HashMap<>();

	/**
	 * For looking round quickly (OPTIONS): BATTLES OFF (no wild POKéMON jump out, walking into one does nothing,
	 * and trainers don't battle you; the story's battles, your rival's and TEAM ROCKET's, still happen), and FAST
	 * TRAVEL (the TOWN MAP takes you anywhere on it).
	 */
	private static final java.util.Set<UUID> BATTLES_OFF = new java.util.HashSet<>(), FAST_TRAVEL = new java.util.HashSet<>();

	public static boolean battlesOff(ServerPlayer p) {
		return BATTLES_OFF.contains(p.getUUID());
	}

	/** A battle the story needs (your rival's, TEAM ROCKET's): it happens even with BATTLES OFF. */
	static boolean storyBattle(Trainers.Trainer t) {
		return t != null && (t.key().startsWith("rival") || t.key().startsWith("rocket"));
	}

	public static void nights(ServerPlayer p, CompoundTag t) {
		NIGHTS.put(p.getUUID(), Math.max(0, Math.min(2, t.getIntOr("nights", 1))));
		if (t.contains("battles")) { if (t.getBooleanOr("battles", true)) BATTLES_OFF.remove(p.getUUID()); else BATTLES_OFF.add(p.getUUID()); }
		if (t.contains("travel")) { if (t.getBooleanOr("travel", false)) FAST_TRAVEL.add(p.getUUID()); else FAST_TRAVEL.remove(p.getUUID()); }
		if (t.contains("seen")) SEEN.put(p.getUUID(), t.getBooleanOr("seen", true));
		if (t.contains("follow")) FOLLOW_ON.put(p.getUUID(), t.getBooleanOr("follow", true));
		if (t.contains("cycle")) CYCLE.put(p.getUUID(), t.getBooleanOr("cycle", true));
		moonlight(p);
	}

	// ---------- wild Pokémon you can see (as Let's Go), and yours walking with you ----------

	/** The wild Pokémon out in the grass: each one's entity → the Pokémon it is (species, level, moves). */
	private static final Map<Integer, Mon> WILD_MONS = new HashMap<>();
	/** Each player's Pokémon walking with them (its entity). */
	private static final Map<UUID, Integer> FOLLOWERS = new HashMap<>();
	/** Seen in the grass (the default) or the classic hidden ones; and whether one walks with you. */
	private static final Map<UUID, Boolean> SEEN = new HashMap<>(), FOLLOW_ON = new HashMap<>();
	/** DAY AND NIGHT from each player's OPTION: true lets the day go by (the default), false holds it in the morning. */
	private static final Map<UUID, Boolean> CYCLE = new HashMap<>();
	/** Up to this many out at once, appearing this near you, gone when this far. */
	private static final int SEEN_MAX = 4, SEEN_NEAR = 10, SEEN_GONE = 22;

	public static boolean seenMode(ServerPlayer p) {
		return SEEN.getOrDefault(p.getUUID(), true);
	}

	/** Whether a roaming one (wild or following) is still the game's (else it's a leftover and goes). */
	public static boolean roamerKnown(WorldMon w) {
		if (w.wild()) return WILD_MONS.containsKey(w.getId());
		return FOLLOWERS.containsValue(w.getId());
	}

	/** Grass to stand in near a height (or, in a cave, any good ground), or null. */
	private static BlockPos grassAt(ServerLevel level, int x, int z, int nearY, boolean anywhere) {
		for (int y = nearY + 4; y >= nearY - 4; y--) {
			BlockPos at = new BlockPos(x, y, z);
			if (anywhere ? goodGround(level, at) : grass(level.getBlockState(at)) && level.getBlockState(at.below()).isFaceSturdy(level, at.below(),
					net.minecraft.core.Direction.UP)) return at;
		}
		return null;
	}

	private static boolean flier(String species) {
		Species sp = Species.ALL.get(species);
		return sp != null && (sp.type1() == com.insidethecube.retromon.engine.Type.FLYING || sp.type2() == com.insidethecube.retromon.engine.Type.FLYING);
	}

	/** The wild ones near you: new ones appear in the grass (from the place's own wild list), old ones wander or go. */
	private static void roamers(ServerPlayer p) {
		ServerLevel level = (ServerLevel) p.level();
		PlayerData d = data(p);
		String tag = "wild:" + p.getUUID();
		int now = level.getServer().getTickCount();
		List<WorldMon> out = level.getEntitiesOfClass(WorldMon.class, p.getBoundingBox().inflate(48), w -> tag.equals(w.key()));
		boolean grassy = d.place != null && !d.place.isEmpty() && !anywhere(d.place);
		for (WorldMon w : out) {
			double dist = w.distanceTo(p);
			if (dist > SEEN_GONE || now - w.born > 20 * 75 && dist > 9 || !seenMode(p)) {
				vanish(level, w);
				continue;
			}
			if (now >= w.nextMove) {
				w.nextMove = now + 40 + RNG.nextInt(90);
				boolean air = flier(w.species()) && RNG.nextInt(3) == 0 ? !w.airborne() : w.airborne();
				w.setAirborne(air);
				for (int tries = 0; tries < 6; tries++) {
					int x = w.getBlockX() + RNG.nextInt(11) - 5, z = w.getBlockZ() + RNG.nextInt(11) - 5;
					// (In a room's grass, the forest's, it wanders within the grass.)
					BlockPos to = Interiors.in(p) ? Interiors.grassNear(w, p.getRandom()) : grassAt(level, x, z, w.getBlockY(), !grassy);
					if (to == null || p.distanceToSqr(to.getX() + 0.5, p.getY(), to.getZ() + 0.5) < 9) continue;
					w.getNavigation().moveTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5, air ? 1.4 : 1.0);
					break;
				}
			}
		}
		if (!seenMode(p) || !d.started || !d.anyHealthy() || d.place == null || d.place.isEmpty() || out.size() >= SEEN_MAX || Battles.in(p)
				|| AFTER_BATTLE.containsKey(p.getUUID()) || COOLDOWN.containsKey(p.getUUID())) return;
		if (RNG.nextInt(100) >= 40) return;
		Wild.Table table = Wild.get(d.place, "grass", Wild.timeOfDay(level.getDayTime()));
		if (table == null || table.ratePer256() <= 0 || !Wild.has(d.place)) return;
		for (int tries = 0; tries < 14; tries++) {
			double a = RNG.nextDouble() * Math.PI * 2, r = 5 + RNG.nextDouble() * (SEEN_NEAR - 5);
			int x = (int) Math.floor(p.getX() + Math.cos(a) * r), z = (int) Math.floor(p.getZ() + Math.sin(a) * r);
			BlockPos at = Interiors.in(p) ? Interiors.grassNear(p, p.getRandom()) : grassAt(level, x, z, p.getBlockY(), !grassy);
			if (at == null) continue;
			Mon foe = table.roll(RNG);
			// A REPEL keeps away ones weaker than your lead.
			Mon lead = null;
			for (Mon m : d.party) if (!m.fainted()) {
				lead = m;
				break;
			}
			if (d.repel > 0 && lead != null && foe.level < lead.level) return;
			WorldMon w = placeMon(level, foe.species.id(), tag, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, RNG.nextFloat() * 360F);
			if (w == null) return;
			w.born = now;
			w.nextMove = now + 10 + RNG.nextInt(30);
			w.setSpeed(0.2 + RNG.nextDouble() * 0.06);
			if (flier(foe.species.id()) && RNG.nextInt(10) < 7) w.setAirborne(true);
			WILD_MONS.put(w.getId(), foe);
			// It rustles out of the grass.
			level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK,
					Blocks.SHORT_GRASS.defaultBlockState()), at.getX() + 0.5, at.getY() + 0.4, at.getZ() + 0.5, 14, 0.35, 0.25, 0.35, 0.05);
			return;
		}
	}

	private static void vanish(ServerLevel level, WorldMon w) {
		WILD_MONS.remove(w.getId());
		level.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF, w.getX(), w.getY() + 0.4, w.getZ(), 5, 0.2, 0.2, 0.2, 0.01);
		w.discard();
	}

	/** Walking into a wild one you can see: the battle with exactly that one. */
	private static boolean touchWild(ServerPlayer p) {
		if (battlesOff(p) || COOLDOWN.containsKey(p.getUUID()) || AFTER_BATTLE.containsKey(p.getUUID()) || !data(p).anyHealthy()) return false;
		// Only when you walk into it: standing still, one wandering by doesn't drag you into a battle.
		Integer walked = WALKING.get(p.getUUID());
		if (walked == null || p.level().getServer().getTickCount() - walked > 6) return false;
		String tag = "wild:" + p.getUUID();
		ServerLevel level = (ServerLevel) p.level();
		Vec3 ahead = Vec3.directionFromRotation(0, p.getYRot());
		for (WorldMon w : level.getEntitiesOfClass(WorldMon.class, p.getBoundingBox().inflate(0.9, 1.5, 0.9), w -> tag.equals(w.key()) && !w.airborne())) {
			// (15:21) Only one you've walked into: right by you, at your height, and ahead of you (not one just behind or
			// across a ledge, which started a battle with nothing near).
			double dx = w.getX() - p.getX(), dz = w.getZ() - p.getZ(), dh = Math.hypot(dx, dz);
			if (dh > 1.25 || Math.abs(w.getY() - p.getY()) > 0.9 || dh > 0.3 && (dx * ahead.x + dz * ahead.z) / dh < 0.2) continue;
			Mon foe = WILD_MONS.remove(w.getId());
			w.discard();
			if (foe == null) continue;
			Battles.wild(p, foe, null);
			return true;
		}
		return false;
	}

	/** Which of your party walks with you: the one you chose (FOLLOW), or else your first that can. */
	private static Mon followMon(PlayerData d) {
		if (d.follow >= 0 && d.follow < d.party.size() && !d.party.get(d.follow).fainted()) return d.party.get(d.follow);
		for (Mon m : d.party) if (!m.fainted()) return m;
		return null;
	}

	private static Vec3 behind(ServerPlayer p, double by) {
		Vec3 look = Vec3.directionFromRotation(0, p.getYRot());
		return p.position().subtract(look.x * by, 0, look.z * by);
	}

	/** Where each player has walked lately (newest first, a point every fifth of a block): the path yours follows. */
	private static final Map<UUID, java.util.ArrayDeque<Vec3>> TRAIL = new HashMap<>();
	/** How far back along your path your Pokémon walks. */
	private static final double FOLLOW_GAP = 1.7;

	/**
	 * Your Pokémon walking with you (as in Yellow, and the newer games): it walks the path you walked, a step and a
	 * half behind, every tick, so it turns your corners with you and never wanders off the path; it eases to a
	 * stop when you stop, looks up at you, and appears behind you if it's left far behind.
	 */
	private static void follower(ServerPlayer p) {
		ServerLevel level = (ServerLevel) p.level();
		PlayerData d = data(p);
		UUID uid = p.getUUID();
		Integer id = FOLLOWERS.get(uid);
		WorldMon w = id != null && level.getEntity(id) instanceof WorldMon wm && wm.isAlive() ? wm : null;
		Mon lead = followMon(d);
		boolean want = d.started && lead != null && !Battles.in(p) && FOLLOW_ON.getOrDefault(uid, true);
		java.util.ArrayDeque<Vec3> trail = TRAIL.computeIfAbsent(uid, k -> new java.util.ArrayDeque<>());
		Vec3 here = p.position();
		if (trail.isEmpty() || trail.peekFirst().distanceToSqr(here) > 0.04) {
			if (!trail.isEmpty() && trail.peekFirst().distanceToSqr(here) > 36) trail.clear();
			trail.addFirst(here);
			while (trail.size() > 60) trail.removeLast();
		}
		if (!want) {
			if (w != null) w.discard();
			FOLLOWERS.remove(uid);
			return;
		}
		Vec3 target = alongTrail(trail, here, FOLLOW_GAP);
		if (target.distanceTo(here) < FOLLOW_GAP - 0.25) {
			// You've not walked far enough for it to follow your path (it's just come out, or you've just got
			// here): it stands a step and a half off you, where it is if it's that far off already, else behind you.
			Vec3 off = w != null ? w.position().subtract(here).multiply(1, 0, 1) : Vec3.ZERO;
			if (off.lengthSqr() < 0.25) {
				double yaw = Math.toRadians(p.getYRot());
				off = new Vec3(Math.sin(yaw), 0, -Math.cos(yaw));
			}
			double far = Math.max(FOLLOW_GAP - 0.25, Math.min(FOLLOW_GAP, off.length()));
			Vec3 at = here.add(off.normalize().scale(far));
			BlockPos b = BlockPos.containing(at.x, here.y + 0.1, at.z);
			if (level.getBlockState(b).getCollisionShape(level, b).isEmpty()) target = new Vec3(at.x, here.y, at.z);
		}
		if (w == null) {
			FOLLOWERS.remove(uid);
			w = placeMon(level, lead.species.id(), "follow:" + uid, target.x, target.y, target.z, p.getYRot());
			if (w == null) return;
			w.setNoGravity(true);
			w.setNoAi(true);
			FOLLOWERS.put(uid, w.getId());
			return;
		}
		if (!w.species().equals(lead.species.id())) w.setSpecies(lead.species.id());
		w.setDeltaMovement(Vec3.ZERO);
		Vec3 at = w.position();
		double dist = at.distanceTo(target);
		if (dist > 12 || w.distanceTo(p) > 14) {
			w.setPos(target.x, target.y, target.z);
			return;
		}
		if (dist > 0.03) {
			// Eases along: quick when it's fallen behind, gentle as it closes in.
			double step = Math.min(dist, Math.max(0.05, dist * 0.32));
			Vec3 to = at.add(target.subtract(at).scale(step / dist));
			w.setPos(to.x, to.y, to.z);
			if (dist > 0.12) {
				float yaw = (float) Math.toDegrees(Math.atan2(-(target.x - at.x), target.z - at.z));
				float turned = w.getYRot() + net.minecraft.util.Mth.wrapDegrees(yaw - w.getYRot()) * 0.35F;
				w.setYRot(turned);
				w.setYHeadRot(turned);
				w.yBodyRot = turned;
			}
		} else if (w.tickCount % 20 == 0) {
			// Stopped: it looks up at you.
			float yaw = (float) Math.toDegrees(Math.atan2(-(p.getX() - at.x), p.getZ() - at.z));
			w.setYRot(yaw);
			w.setYHeadRot(yaw);
			w.yBodyRot = yaw;
		}
	}

	/** The point so far back along the path you've walked (or as far back as it goes). */
	private static Vec3 alongTrail(java.util.ArrayDeque<Vec3> trail, Vec3 from, double back) {
		Vec3 prev = from;
		double left = back;
		for (Vec3 pt : trail) {
			double seg = prev.distanceTo(pt);
			if (seg >= left && seg > 1e-6) return prev.add(pt.subtract(prev).scale(left / seg));
			left -= seg;
			prev = pt;
		}
		return prev;
	}

	/** A: reading a sign (or a shelf standing in for one): the words of the games' sign nearest that spot. */
	public static void read(ServerPlayer p, CompoundTag t) {
		int bx = t.getIntOr("x", p.getBlockX()), bz = t.getIntOr("z", p.getBlockZ());
		if (t.getBooleanOr("tree", false)) {
			// A little tree (Route 2's cherry trees): one that can be CUT down says so; any other tree says nothing.
			if (Interiors.in(p)) return;
			CutTrees.Tree tree = CutTrees.at(bx, t.getIntOr("y", p.getBlockY()), bz);
			if (tree != null) CutTrees.use(p, tree);
			return;
		}
		if (t.getBooleanOr("mailbox", false)) {
			// A house's mailbox, as FireRed's and HeartGold's: whose house it is.
			Mailbox.Spot sp = Mailbox.at(bx, t.getIntOr("y", p.getBlockY()), bz);
			String line = sp == null ? "It's a mailbox." : sp.owner().equals("rival") ? "{rival}'s house" : sp.owner().equals("player") ? "{player}'s house" : sp.owner();
			dialogue(p, null, "talk", fill(p, List.of(line)));
			return;
		}
		// A sign of its own on John's map (Diglett's Cave's, by each of its mouths): its words.
		for (Object[] sg : SIGN_SPOTS) {
			if (Math.abs(bx - (Integer) sg[0]) <= 1 && Math.abs(bz - (Integer) sg[1]) <= 1) {
				@SuppressWarnings("unchecked")
				List<String> words = (List<String>) sg[2];
				dialogue(p, null, "talk", words);
				return;
			}
		}
		String place = placeAt(bx + 0.5, bz + 0.5);
		if (place == null) return;
		Zone z = null;
		for (Zone zz : zones) if (zz.place().equals(place) && zz.contains(bx + 0.5, bz + 0.5)) z = zz;
		int[] size = Places.size(place);
		Trainers.Sign best = null;
		double bestD = 14 * 14;
		if (z != null && size != null) {
			for (Trainers.Sign sg : Trainers.signsIn(place)) {
				double fx = (sg.x() + 0.5) / size[0], fz = (sg.y() + 0.5) / size[1];
				double wx = z.x1() + fx * (z.x2() - z.x1() + 1), wz = z.z1() + fz * (z.z2() - z.z1() + 1);
				double dd = (wx - bx) * (wx - bx) + (wz - bz) * (wz - bz);
				if (dd < bestD) {
					bestD = dd;
					best = sg;
				}
			}
		}
		if (best == null && Trainers.signsIn(place).size() == 1) best = Trainers.signsIn(place).get(0);
		Places.Place pl = Places.get(place);
		List<String> lines = best != null ? fill(p, best.lines()) : List.of(pl == null ? place.toUpperCase().replace('_', ' ') : pl.name());
		dialogue(p, null, "talk", lines);
	}

	/** FOLLOW from the party: that one walks with you now. */
	/**
	 * BILL's PC: one of your team into the box (never your last one able to battle), or one out of the box onto
	 * your team (when there's room for it).
	 */
	public static void pc(ServerPlayer p, CompoundTag t) {
		PlayerData d = data(p);
		int i = t.getIntOr("i", -1);
		switch (t.getStringOr("op", "")) {
			case "deposit" -> {
				if (i < 0 || i >= d.party.size() || d.party.size() <= 1) return;
				Mon m = d.party.get(i);
				long able = d.party.stream().filter(x -> !x.fainted()).count();
				if (!m.fainted() && able <= 1) return;
				d.party.remove(i);
				d.box.add(m);
				if (d.follow == i || d.follow >= d.party.size()) d.follow = 0;
				else if (d.follow > i) d.follow--;
			}
			case "withdraw" -> {
				if (i < 0 || i >= d.box.size() || d.party.size() >= 6) return;
				d.party.add(d.box.remove(i));
			}
			default -> {
				return;
			}
		}
		sync(p);
	}

	public static void follow(ServerPlayer p, CompoundTag t) {
		PlayerData d = data(p);
		int i = t.getIntOr("i", 0);
		if (i < 0 || i >= d.party.size()) return;
		d.follow = i;
		sync(p);
	}

	/** At night, an unseen night vision (no icon, no swirls) so the screen can lift the dark to a moonlit blue. */
	private static void moonlight(ServerPlayer p) {
		long t = Math.floorMod(p.level().getDayTime(), 24000L);
		// (Viridian Forest is always night under its trees.)
		boolean night = (t >= 12300 && t < 23700 || Interiors.alwaysNight(p)) && NIGHTS.getOrDefault(p.getUUID(), 1) > 0;
		var nv = p.getEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);
		boolean ours = nv != null && nv.getAmplifier() == 7;
		if (night && (nv == null || ours && nv.getDuration() < 400)) {
			p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION, 900, 7, true, false, false));
		} else if (!night && ours) p.removeEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);
	}

	/** The time when a battle began: the world's clock stands still until it's over (so day doesn't turn to night mid-fight). */
	private static long heldTime = -1;
	/** The world's held still (a Pokédex is out). */
	private static boolean frozenWorld;

	private static void holdTime(MinecraftServer s) {
		if (!ourSave) return;
		boolean fighting = false, device = false;
		for (ServerPlayer p : s.getPlayerList().getPlayers()) {
			if (!adventure(p)) continue;
			// The clock waits through the battle and its last words too (the cheering and the EXP after a win, up to
			// Continue): the server's battle is over before your screen is, and the sky jumped on when it closed.
			if (Battles.in(p) || AFTER_BATTLE.containsKey(p.getUUID())) fighting = true;
			if (DEVICES.contains(p.getUUID())) device = true;
		}
		// With your Pokédex out (playing alone), the whole world waits: people and Pokémon stand still too.
		boolean freeze = device && s.isSingleplayer();
		if (freeze != frozenWorld) {
			frozenWorld = freeze;
			s.tickRateManager().setFrozen(freeze);
		}
		fighting |= device;
		ServerLevel level = s.overworld();
		// DAY AND NIGHT off (OPTION): the clock's held at half past ten in the morning (the shadows fall nicely; clouds
		// still drift). The days stop counting meanwhile.
		boolean still = false;
		for (ServerPlayer p : s.getPlayerList().getPlayers()) if (adventure(p) && !CYCLE.getOrDefault(p.getUUID(), true)) still = true;
		if (still) {
			long now = level.getDayTime(), want = now - Math.floorMod(now, 24000L) + 4500;
			if (now != want) level.setDayTime(want);
			if (heldTime != -2) {
				heldTime = -2;
				level.getGameRules().set(GameRules.ADVANCE_TIME, false, s);
			}
			return;
		}
		if (heldTime == -2) {
			heldTime = -1;
			level.getGameRules().set(GameRules.ADVANCE_TIME, true, s);
		}
		if (fighting == heldTime >= 0) return;
		// The clock stops (the game rule, so your screen's clock stops too, with no jumps back) and starts again after.
		heldTime = fighting ? level.getDayTime() : -1;
		level.getGameRules().set(GameRules.ADVANCE_TIME, !fighting, s);
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

	/** The tall grass wild POKéMON live in: the big tufts only (tall grass, large ferns), never the little ones (15:21). */
	private static boolean grass(BlockState s) {
		return s.is(Blocks.TALL_GRASS) || s.is(Blocks.LARGE_FERN);
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
		safariStep(p);
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
		if (d.daycare != null) daycareStep(d.daycare);
		if (!d.started || !d.anyHealthy() || COOLDOWN.containsKey(p.getUUID()) || d.place.isEmpty() || battlesOff(p)) return;
		// Seen in the grass (as Let's Go): you meet them by walking into them, not at random.
		if (seenMode(p)) return;
		boolean inGrass = grass(p.level().getBlockState(pos)) || grass(p.level().getBlockState(pos.above())) || Interiors.inGrass(p);
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
		safariAfterBattle(p);
		COOLDOWN.put(p.getUUID(), 60);
		GRACE.put(p.getUUID(), 4);
		// Till the battle screen closes (the cheering, the EXP, Continue): 5 minutes at most, in case it never says.
		AFTER_BATTLE.put(p.getUUID(), 6000);
	}

	public static void blackout(ServerPlayer p) {
		PlayerData d = data(p);
		if (!d.hasHeal) {
			toStart(p);
			return;
		}
		// Back to where you were last healed, in the world or inside (out of the other first, if that's where you are).
		ServerLevel to = d.healInside ? Interiors.ensure(p.level().getServer()) : p.level().getServer().overworld();
		// (Behind the counter of the Center you were healed at: its door's the way back out, its town where you are.)
		if (d.healInside && !d.healDoor.isEmpty()) {
			d.door = d.healDoor;
			Interiors.Door hd = Interiors.DOORS.get(d.healDoor);
			if (hd != null && hd.place() != null) enter(p, hd.place());
		}
		if (to == null || to == p.level()) p.teleportTo(d.healX, d.healY, d.healZ);
		else {
			leavingLevel(p);
			p.teleportTo(to, d.healX, d.healY, d.healZ, java.util.Set.of(), p.getYRot(), p.getXRot(), true);
		}
	}

	// ---------- the story: Oak's Parcel (Red and Blue's first errand) ----------
	//
	// After your first battle, Oak's back at his lab's door. In Viridian, the Mart's clerk calls you over: Oak's
	// order came in. The Mart won't sell you anything till it's delivered, and the old man lying across the road
	// north won't let you by. Take it to Oak: your rival turns up, and Oak gives you each a POKéDEX, and you five
	// POKé BALLs. Then the old man's had his coffee, the Mart's open, and your rival's waiting on Route 22.

	static final String OAK_KEY = "oaks_lab_p0", OLD_MAN = "viridian_city_p1";
	private static final List<String> OLD_MAN_GRUMPY = List.of("You can't go through here!", "This is private property!",
			"GIRL: Oh Grandpa! Don't be so mean! He hasn't had his coffee yet.");
	/** Where each player last stood on good ground in Viridian (to put them back from the old man). */
	private static final Map<UUID, BlockPos> VIRIDIAN_SAFE = new HashMap<>();

	private static boolean viridianMart(Npc n) {
		return n.role() == Npc.CLERK && n.key().contains("viridian");
	}

	/** Each 5 ticks: the clerk calling you over, Oak at his door, your rival on Route 22. */
	private static void story(ServerPlayer p) {
		PlayerData d = data(p);
		if (!d.started || !ourSave) return;
		ServerLevel level = (ServerLevel) p.level();
		oakAtLab(level);
		if (engaged(p) || WALKS.containsKey(p.getUUID()) || Battles.in(p)) return;
		if ("viridian_city".equals(d.place) && p.onGround() && goodGround(level, p.blockPosition())) VIRIDIAN_SAFE.put(p.getUUID(), p.blockPosition());
		// "Hey! You came from PALLET TOWN?"
		if (!d.hasDex() && !d.story.contains("parcel") && "viridian_city".equals(d.place)) {
			for (Npc n : level.getEntitiesOfClass(Npc.class, p.getBoundingBox().inflate(9), Game::viridianMart)) {
				if (n.distanceTo(p) > 9 || !n.hasLineOfSight(p)) continue;
				walkUp(p, n, null, "", () -> giveParcel(p, n));
				return;
			}
		}
		// MUM outside the lab once you've your first POKéMON, with the TOWN MAP (Let's Go's way). Saves from further on
		// have it already.
		if (!d.story.contains("townmap")) {
			if (d.hasDex() || d.story.contains("parcel") || d.visited.contains("viridian_city")) {
				d.story.add("townmap");
				sync(p);
			} else if ("pallet_town".equals(d.place) && !Interiors.in(p) && !MUMS.containsKey(p.getUUID()) && p.onGround() && clearOfLab(p)) {
				mumComes(p, level);
				return;
			}
		}
		// TEAM ROCKET (JESSIE and JAMES): as in Let's Go, already standing in VIRIDIAN, just up the steps from ROUTE 1 by
		// the town's sign, the first time you come with a POKéMON. You see them as you walk up; when you're close,
		// JESSIE comes over and they stop you.
		if (!d.story.contains("rocket_viridian") && !d.hasDex() && d.anyHealthy() && !Interiors.in(p)) {
			rocketsWait(p, level);
			if (rocketsNear(p) && p.onGround()) {
				rocketsMeet(p);
				return;
			}
		}
		// Your rival, from the League's way, once you have the POKéDEX: as in FireRed, when you walk the top path (the
		// one along to the League's gate) past where it narrows, he comes along it from the gate's end, where he's been
		// standing on it, and you battle. (Never from up the hill: always along the path, and back along it after.)
		if (d.hasDex() && d.anyHealthy() && !d.beaten.contains("rival_22") && "route_22".equals(d.place) && !RIVALS.containsKey(p.getUUID())
				&& p.getX() > RIVAL_22_TRIGGER[0] && p.getX() < RIVAL_22_TRIGGER[1] && p.getZ() > RIVAL_22_TRIGGER[2] && p.getZ() < RIVAL_22_TRIGGER[3]) {
			BlockPos from = spot(level, RIVAL_22_SPOT[0], RIVAL_22_SPOT[1], 3);
			Npc n = rivalNpc(p, "rival_22", from);
			if (n != null) {
				RIVAL_FROM.put(p.getUUID(), Vec3.atBottomCenterOf(from));
				walkUp(p, n, rival22(p), "meet_rival", null);
			}
		}
	}

	/**
	 * Route 22's top path (John's mud brick path, z 533 to 538, from the League gate east): FireRed's triggers for your
	 * rival's battle (its step 33, rows 4 to 6) laid over it at two blocks a step from the gate's door, x0 x1 z0 z1; and
	 * where he stands waiting on it (FireRed's step 25, row 4), x and z.
	 */
	private static final double[] RIVAL_22_TRIGGER = {-871.5, -865.0, 531.5, 539.5};
	private static final int[] RIVAL_22_SPOT = {-885, 535};

	/** Walked up to the Viridian GYM's door: it's locked (its LEADER's away), said once each time. */
	private static final Map<UUID, Boolean> GYM_SHUT = new HashMap<>();

	private static void gymShut(ServerPlayer p) {
		if (!"viridian_city".equals(data(p).place)) return;
		double x = p.getX(), z = p.getZ();
		boolean at = x > -769.4 && x < -764.6 && z > 524.6 && z < 527.2 && Math.abs(net.minecraft.util.Mth.wrapDegrees(p.getYRot() - 180)) < 60;
		if (!at) {
			GYM_SHUT.remove(p.getUUID());
			return;
		}
		if (GYM_SHUT.put(p.getUUID(), true) != null) return;
		message(p, List.of("VIRIDIAN CITY POKéMON GYM", "The GYM's doors are locked...", "It looks like the GYM LEADER isn't here."));
	}

	/** JESSIE and JAMES in Viridian (JESSIE comes up to you; JAMES with her), while they're about. */
	private static final Map<UUID, List<Npc>> ROCKETS = new HashMap<>();

	private static Trainers.Trainer rocketViridian() {
		return new Trainers.Trainer("rocket_viridian", "viridian_city", "TEAM ROCKET", "jessie", "none", 0, 20,
				List.of(new Trainers.Member("ekans", 6), new Trainers.Member("koffing", 6)), null,
				List.of("JESSIE: Well, well! What have we here? A twerp with a brand-new POKéMON!", "JAMES: Fresh out of PALLET TOWN, by the look of it.",
						"MEOWTH: Mrowr!", "JESSIE: We're TEAM ROCKET, twerp. We take the best POKéMON there are...",
						"JAMES: ...and yours looks like it would do very nicely. Hand it over!"),
				List.of("JESSIE: What?! Beaten by a twerp?!", "JAMES: This wasn't in the plan, Jessie!"),
				List.of("JESSIE: You haven't seen the last of TEAM ROCKET!"));
	}

	/** Where they stand in VIRIDIAN (JESSIE, then JAMES): on the paving just up the steps from ROUTE 1, by the sign. */
	private static final int[][] ROCKET_SPOTS = {{-796, 561}, {-793, 561}};

	/** They're put there when you're coming (within 64 blocks), and go again if you go off before you meet them. */
	private static void rocketsWait(ServerPlayer p, ServerLevel level) {
		List<Npc> have = ROCKETS.get(p.getUUID());
		double dx = p.getX() - ROCKET_SPOTS[0][0] - 0.5, dz = p.getZ() - ROCKET_SPOTS[0][1] - 0.5;
		if (dx * dx + dz * dz > 64 * 64) {
			if (have != null && !WALKS.containsKey(p.getUUID())) {
				for (Npc n : have) if (!n.isRemoved()) n.discard();
				ROCKETS.remove(p.getUUID());
			}
			return;
		}
		if (have != null) return;
		List<Npc> both = new ArrayList<>();
		for (int i = 0; i < ROCKET_SPOTS.length; i++) {
			int x = ROCKET_SPOTS[i][0], z = ROCKET_SPOTS[i][1];
			if (!level.hasChunkAt(new BlockPos(x, START_Y, z))) return;
		}
		for (int i = 0; i < ROCKET_SPOTS.length; i++) {
			BlockPos at = spot(level, ROCKET_SPOTS[i][0], ROCKET_SPOTS[i][1], 3);
			if (at == null) continue;
			// Facing south, down the steps: the way you come in.
			Npc n = place(level, i == 0 ? Npc.TRAINER : Npc.PERSON, i == 0 ? "rocket_viridian" : "rocket_james", at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0F);
			if (n == null) continue;
			n.setCustomName(net.minecraft.network.chat.Component.literal(i == 0 ? "JESSIE" : "JAMES"));
			both.add(n);
		}
		if (!both.isEmpty() && both.get(0).key().equals("rocket_viridian")) ROCKETS.put(p.getUUID(), both);
		else for (Npc n : both) n.discard();
	}

	/** Whether you've come up close to them (and nothing else is going on). */
	private static boolean rocketsNear(ServerPlayer p) {
		List<Npc> have = ROCKETS.get(p.getUUID());
		if (have == null || have.isEmpty() || have.get(0).isRemoved() || WALKS.containsKey(p.getUUID()) || engaged(p)) return false;
		Npc jessie = have.get(0);
		return jessie.level() == p.level() && jessie.distanceTo(p) < 6.5;
	}

	/** They've seen you: both turn to you, and JESSIE comes over. */
	private static void rocketsMeet(ServerPlayer p) {
		List<Npc> both = ROCKETS.get(p.getUUID());
		Npc jessie = both.get(0);
		for (Npc n : both) n.setFacing(n.yawTo(p));
		data(p).story.add("rocket_viridian");
		jessie.talker = p;
		walkUp(p, jessie, rocketViridian(), "meet_evil_trainer", null);
	}

	/** Once their battle's over and done with on your screen, JESSIE and JAMES are off ("blasting off", in a puff). */
	private static void rocketsLeave(MinecraftServer s) {
		for (Map.Entry<UUID, List<Npc>> e : List.copyOf(ROCKETS.entrySet())) {
			ServerPlayer p = s.getPlayerList().getPlayer(e.getKey());
			boolean gone = p == null || e.getValue().stream().allMatch(Npc::isRemoved);
			if (!gone && (engaged(p) || WALKS.containsKey(e.getKey()) || AFTER_BATTLE.containsKey(e.getKey()) || !data(p).beaten.contains("rocket_viridian"))) continue;
			for (Npc n : e.getValue()) {
				if (n.isRemoved()) continue;
				if (n.level() instanceof ServerLevel lv)
					lv.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF, n.getX(), n.getY() + 1, n.getZ(), 14, 0.3, 0.5, 0.3, 0.04);
				n.discard();
			}
			ROCKETS.remove(e.getKey());
		}
	}

	/** Whether one of TEAM ROCKET in the world is the game's (else they're left over in a save, and go). */
	static boolean rocketKnown(Npc n) {
		for (List<Npc> l : ROCKETS.values()) if (l.contains(n)) return true;
		return false;
	}

	private static void giveParcel(ServerPlayer p, Npc n) {
		PlayerData d = data(p);
		if (d.story.contains("parcel") || d.hasDex()) return;
		d.story.add("parcel");
		d.give("oaks_parcel", 1);
		sync(p);
		if (n != null) n.talker = p;
		dialogue(p, n, "talk", fill(p, List.of("Hey! You came from PALLET TOWN?", "You know PROF.OAK, right?", "His order came in. Will you take it to him?",
				"{player} got OAK's PARCEL!", "Okay! Say hi to PROF.OAK for me!")), cues(3, "get_key_item"));
	}

	/** Whether you're a few steps out from the lab's door (MUM waits till then). */
	private static boolean clearOfLab(ServerPlayer p) {
		Interiors.Door door = Interiors.DOORS.get("lab");
		if (door == null) return true;
		double dx = p.getX() - door.x(), dz = p.getZ() - door.z();
		return dx * dx + dz * dz > 4.5 * 4.5;
	}

	/** MUM, come out to see you off with the TOWN MAP (and the ticks left till she's home again). */
	private static final Map<UUID, Npc> MUMS = new HashMap<>();
	private static final Map<UUID, Integer> MUM_HOME = new HashMap<>();

	/** MUM's front door (out on the step), and just inside it (where she goes in, home again). */
	private static final Vec3 MUM_DOOR = new Vec3(-802.0, 19.0, 680.6), MUM_IN = new Vec3(-802.0, 19.0, 679.2);
	/** Her front door's two halves (the bottoms), opened as she goes in and shut again behind her. */
	private static final BlockPos[] MUM_DOORS = {new BlockPos(-803, 19, 679), new BlockPos(-802, 19, 679)};

	/** Opens (or shuts) MUM's front door, as anyone going in or out of a house does, with its sound. */
	private static void mumDoor(net.minecraft.world.level.Level level, boolean open) {
		for (BlockPos at : MUM_DOORS) {
			var st = level.getBlockState(at);
			if (st.getBlock() instanceof net.minecraft.world.level.block.DoorBlock door && st.getValue(net.minecraft.world.level.block.DoorBlock.OPEN) != open)
				door.setOpen(null, level, st, at, open);
		}
	}

	/**
	 * Where MUM comes from: out of the town on the way from her house (never from behind the lab, so she doesn't come
	 * through it), on open ground under the sky that you can see from where you are, 7 to 11 blocks off.
	 */
	private static BlockPos mumFrom(ServerLevel level, ServerPlayer p) {
		double dx = MUM_DOOR.x - p.getX(), dz = MUM_DOOR.z - p.getZ(), len = Math.sqrt(dx * dx + dz * dz);
		if (len < 1) return null;
		dx /= len;
		dz /= len;
		for (int d = (int) Math.min(11, len); d >= 7; d--)
			for (double side : new double[] {0, 1.5, -1.5, 3, -3}) {
				int x = (int) Math.floor(p.getX() + dx * d - dz * side), z = (int) Math.floor(p.getZ() + dz * d + dx * side);
				for (int dy : new int[] {0, -1, 1}) {
					BlockPos c = new BlockPos(x, p.getBlockY() + dy, z);
					if (!goodGround(level, c) || !level.canSeeSky(c.above())) continue;
					Vec3 eye = new Vec3(c.getX() + 0.5, c.getY() + 1.5, c.getZ() + 0.5);
					var hit = level.clip(new net.minecraft.world.level.ClipContext(eye, p.getEyePosition(), net.minecraft.world.level.ClipContext.Block.COLLIDER,
							net.minecraft.world.level.ClipContext.Fluid.NONE, p));
					if (hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS) return c;
				}
			}
		return null;
	}

	private static void mumComes(ServerPlayer p, ServerLevel level) {
		BlockPos at = mumFrom(level, p);
		if (at == null) return;
		Npc mum = place(level, Npc.PERSON, "in_house1_0", at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0F);
		if (mum == null) return;
		mum.setCustomName(net.minecraft.network.chat.Component.literal("MUM"));
		MUMS.put(p.getUUID(), mum);
		walkUp(p, mum, null, "", () -> {
			PlayerData d = data(p);
			d.story.add("townmap");
			sync(p);
			mum.talker = p;
			dialogue(p, mum, "mum_map", fill(p, List.of("MUM: {player}! Wait a moment!", "MUM: So that's your very own POKéMON? It's lovely!",
					"MUM: You'll be going a long way from home now. Here, take this with you.", "{player} got the TOWN MAP!",
					"MUM: Look at it from the BAG in your menu, so you never get lost.", "MUM: Take care, love! Come home any time you need a rest.")),
					cues(3, "get_key_item"));
		});
	}

	/** MUM's said her goodbye: back home she goes (and is gone once she's away). */
	private static void mumHeadsHome(ServerPlayer p) {
		Npc mum = MUMS.get(p.getUUID());
		if (mum == null || mum.isRemoved()) {
			MUMS.remove(p.getUUID());
			return;
		}
		mum.talker = null;
		mum.getNavigation().moveTo(MUM_DOOR.x, MUM_DOOR.y, MUM_DOOR.z, 0.8);
		MUM_HOME.put(p.getUUID(), 600);
	}

	/** MUM on her way home: all the way to her door, then in through it (gone). If she can't get there, she only goes
	 * once you're well away, so you never see her vanish. */
	private static void mumsGoHome() {
		for (Map.Entry<UUID, Integer> e : List.copyOf(MUM_HOME.entrySet())) {
			Npc mum = MUMS.get(e.getKey());
			int left = e.getValue() - 1;
			boolean done = mum == null || mum.isRemoved();
			if (!done) {
				Vec3 at = mum.position();
				double toDoor = Math.hypot(at.x - MUM_DOOR.x, at.z - MUM_DOOR.z), toIn = Math.hypot(at.x - MUM_IN.x, at.z - MUM_IN.z);
				ServerPlayer p = mum.level().getServer() == null ? null : mum.level().getServer().getPlayerList().getPlayer(e.getKey());
				boolean seen = p != null && p.level() == mum.level() && p.distanceTo(mum) < 28;
				if (toIn < 0.45) done = true;
				else if (toDoor < 1.2) {
					// On the step: she opens the door and in she goes.
					mumDoor(mum.level(), true);
					mum.getNavigation().stop();
					mum.setFacing(180F);
					mum.getMoveControl().setWantedPosition(MUM_IN.x, MUM_IN.y, MUM_IN.z, 0.6);
					if (left > 40) left = 40;
				} else if (mum.getNavigation().isDone() && left % 20 == 0) mum.getNavigation().moveTo(MUM_DOOR.x, MUM_DOOR.y, MUM_DOOR.z, 0.8);
				if (left <= 0 && (toDoor < 2 || !seen)) done = true;
				if (left <= 0 && !done) left = 20;
			}
			if (done) {
				if (mum != null && !mum.isRemoved()) {
					// The door shut behind her.
					mumDoor(mum.level(), false);
					mum.discard();
				}
				MUMS.remove(e.getKey());
				MUM_HOME.remove(e.getKey());
			} else MUM_HOME.put(e.getKey(), left);
		}
	}

	/** Lines a dialogue plays a jingle on (line index, then the jingle). */
	private static CompoundTag cues(Object... at) {
		CompoundTag t = new CompoundTag();
		ListTag l = new ListTag();
		for (int i = 0; i + 1 < at.length; i += 2) l.add(StringTag.valueOf(at[i] + ":" + at[i + 1]));
		t.put("cues", l);
		return t;
	}

	/** Oak, back at his lab's door once you've your first Pokémon (once per save). */
	private static void oakAtLab(ServerLevel level) {
		// He's inside his lab now (Interiors), not out at the door.
		if (!ourSave || !Interiors.ROOMS.isEmpty()) return;
		JsonArray placed = marker.has("placedKeys") ? marker.getAsJsonArray("placedKeys") : new JsonArray();
		for (JsonElement e : placed) if (e.getAsString().equals("oak:lab")) return;
		BlockPos[] door = labDoor(level);
		if (door == null || !level.hasChunkAt(door[1])) return;
		// Beside the way in, looking out.
		BlockPos out = door[1];
		int sx = Integer.signum(out.getX() - door[0].getX()), sz = Integer.signum(out.getZ() - door[0].getZ());
		BlockPos at = spot(level, out.getX() + sz * 2, out.getZ() + sx * 2, 3);
		Npc oak = place(level, Npc.PERSON, OAK_KEY, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0F);
		if (oak != null) {
			oak.setCustomName(net.minecraft.network.chat.Component.literal("PROF.OAK"));
			oak.setFacing((float) Math.toDegrees(Math.atan2(-(out.getX() + sx * 3 - at.getX()), out.getZ() + sz * 3 - at.getZ())));
		}
		placed.add("oak:lab");
		marker.add("placedKeys", placed);
		saveMarker();
	}

	private static final int[] RATING_AT = {10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120, 130, 140, 150, 999};
	private static final String[] RATINGS = {"You still have lots to do. Look for POKéMON in grassy areas!",
			"You're on the right track! Get a FLASH HM from my AIDE!", "You still need more POKéMON! Try to catch other species!",
			"Good, you're trying hard! Get an ITEMFINDER from my AIDE!", "Looking good! Go find my AIDE when you get 50!",
			"You finally got at least 50 species! Be sure to get EXP.ALL from my AIDE!", "Oh! This is getting even better!",
			"Very good! Go fish for some marine POKéMON!", "Wonderful! Do you like to collect things?", "I'm impressed! It must have been difficult to do!",
			"You finally got at least 100 species! I can't believe how good you are!", "You even have the evolved forms of POKéMON! Super!",
			"Excellent! Trade with friends to get some more!", "Outstanding! You've become a real pro at this!",
			"I have nothing left to say! You're the authority now!", "Your POKéDEX is fully complete! Congratulations!"};

	private static void oakTalk(ServerPlayer p, Npc n) {
		PlayerData d = data(p);
		if (!d.started) {
			CompoundTag t = new CompoundTag();
			t.putBoolean("stop", false);
			t.putBoolean("lab", true);
			Net.send(p, "intro", t);
			return;
		}
		if (d.story.contains("parcel") && !d.hasDex()) {
			deliverParcel(p, n);
			return;
		}
		if (d.hasDex() && !d.beaten.contains("rival_lab") && !d.party.isEmpty() && !RIVALS.containsKey(p.getUUID())) {
			rivalChallenge(p);
			return;
		}
		if (!d.hasDex()) {
			dialogue(p, n, "talk", fill(p, List.of("OAK: If a wild POKéMON appears, your POKéMON can fight against it!", "Afterward, go on to the next town.",
					"VIRIDIAN CITY is just north, past ROUTE 1.")));
			return;
		}
		int own = d.caught.size(), r = 0;
		while (r < RATING_AT.length - 1 && own >= RATING_AT[r]) r++;
		dialogue(p, n, "talk", fill(p, List.of("OAK: Good to see you! How is your POKéDEX coming? Here, let me take a look!",
				"POKéDEX completion is: " + d.seen.size() + " POKéMON seen, " + own + " POKéMON owned.", "PROF.OAK's Rating: " + RATINGS[r])));
	}

	/** The parcel to Oak: your rival turns up, and you each get a POKéDEX; you get five POKé BALLs. */
	private static void deliverParcel(ServerPlayer p, Npc n) {
		PlayerData d = data(p);
		d.take("oaks_parcel");
		d.story.remove("parcel");
		d.story.add("pokedex");
		d.give("poke_ball", 5);
		sync(p);
		ServerLevel level = (ServerLevel) p.level();
		Npc rv = rivalNpc(p, "rival_visit", spot(level, p.getBlockX() + 2, p.getBlockZ() + 2, 3));
		if (rv != null) rv.talker = p;
		List<String> lines = List.of("OAK: Oh, {player}! How is my old POKéMON?", "Well, it seems to like you a lot.",
				"You must be talented as a POKéMON trainer!", "What? You have something for me?", "{player} delivered OAK's PARCEL.",
				"Ah! This is the custom POKé BALL I ordered! Thanks, {player}!", "{rival}: Gramps! I almost forgot! What did you call me for?",
				"OAK: Oh right! I have a request of you two.", "On the desk there is my invention, POKéDEX!",
				"It automatically records data on POKéMON you've seen or caught! It's a hi-tech encyclopedia!",
				"OAK: {player} and {rival}! Take these with you!", "{player} got POKéDEX from OAK!",
				"To make a complete guide on all the POKéMON in the world... That was my dream!", "But, I'm too old! I can't do it!",
				"So, I want you two to fulfill my dream for me!", "Get moving, you two! This is a great undertaking in POKéMON history!",
				"{rival}: Alright Gramps! Leave it all to me!", "{player}, I hate to say it, but I don't need you!",
				"I know! I'll borrow a TOWN MAP from my sis! I'll tell her not to lend you one, {player}! Hahaha!",
				"OAK: You can't get detailed data on POKéMON by just seeing them.", "You must catch them! Use these to capture wild POKéMON.",
				"{player} got 5 POKé BALLs!", "When a wild POKéMON appears, it's fair game. Throw a POKé BALL at it and try to catch it!",
				"This won't always work, though. A healthy POKéMON could escape. You have to be lucky!");
		// Then your rival wants a battle, right there (the first, as Let's Go has it): you can't go on north without it.
		dialogue(p, n, d.beaten.contains("rival_lab") ? "talk" : "parcel", fill(p, lines), cues(4, "get_key_item", 11, "get_key_item", 21, "get_item"));
	}

	/** Oak's done talking (the parcel, the POKéDEX): your rival turns to you and wants a battle. */
	private static void rivalAfterParcel(ServerPlayer p) {
		PlayerData d = data(p);
		if (d.beaten.contains("rival_lab") || d.party.isEmpty()) return;
		Npc rv = RIVALS.get(p.getUUID());
		List<String> ch = fill(p, List.of("{rival}: Wait, {player}! Gramps gave us POKéMON, so let's check them out!", "{rival}: Come on, I'll take you on!"));
		if (rv != null && !rv.isRemoved()) dialogue(p, rv, "rival", ch, new CompoundTag());
		else rivalChallenge(p);
	}

	/** Whether you have what a guarded way needs (a badge by its name, or "CHAMPION"). */
	public static boolean meets(ServerPlayer p, String needs) {
		PlayerData d = data(p);
		if (needs.equals("safari")) return SAFARI.containsKey(p.getUUID());
		if (needs.equals("CHAMPION")) return d.story.contains("champion");
		// (0.50) The CYCLING ROAD's gates: on a BICYCLE only. SAFFRON's: the guards are thirsty (a drink gets you by).
		if (needs.equals("bicycle")) return d.count("bicycle") > 0;
		if (needs.equals("tea")) return d.count("tea") > 0 || d.count("fresh_water") > 0 || d.count("soda_pop") > 0 || d.count("lemonade") > 0;
		return d.badges.contains(needs);
	}

	/** Turned back at a guarded way: the guard says why. */
	public static void gateBlocked(ServerPlayer p, String needs) {
		if (needs.equals("safari")) {
			safariAsk(p);
			return;
		}
		if (needs.equals("bicycle")) {
			dialogue(p, null, "talk", List.of("GUARD: Excuse me! Wait up, please!", "GUARD: No pedestrians are allowed on CYCLING ROAD!", "GUARD: Come back on a BICYCLE."),
					whoTag("GUARD"));
			return;
		}
		if (needs.equals("tea")) {
			dialogue(p, null, "talk", List.of("GUARD: I'm on guard duty. Gee, I'm thirsty, though!", "GUARD: Oh, wait there, the road's closed."), whoTag("GUARD"));
			return;
		}
		String b = needs.equals("CHAMPION") ? "the POKéMON LEAGUE's title" : "the " + needs;
		dialogue(p, null, "talk", List.of("GUARD: Hold it right there!", "GUARD: Only TRAINERS who have " + b + " may go on from here.",
				"GUARD: Come back when you've earned it!"), whoTag("GUARD"));
	}

	private static CompoundTag whoTag(String who) {
		CompoundTag t = new CompoundTag();
		t.putString("who", who);
		return t;
	}

	/** At the Route 28 Gate's west way: where to? ROUTE 28 (under the mountain), or ROUTE 26 and JOHTO. */
	public static void gateChoice(ServerPlayer p, String key) {
		if (!key.equals("johto")) return;
		CompoundTag t = whoTag("GUARD");
		ListTag ch = new ListTag();
		for (String c : List.of("ROUTE 28", "ROUTE 26 / JOHTO", "STAY HERE")) ch.add(StringTag.valueOf(c));
		t.put("choices", ch);
		dialogue(p, null, "gate_choice", List.of("GUARD: The way west goes under the mountain. Which way are you headed?"), t);
	}

	/** North of Viridian before the POKéDEX: the old man won't let you by, and you're put back. */
	private static boolean oldManStops(ServerPlayer p, String place) {
		PlayerData d = data(p);
		if (!ourSave || !d.started || d.hasDex() && d.beaten.contains("rival_lab") || !"viridian_city".equals(d.place)) return false;
		if (!place.matches("route_2(_.*)?") && !place.startsWith("viridian_forest") && !place.startsWith("pewter") && !place.startsWith("diglett")) return false;
		ServerLevel level = (ServerLevel) p.level();
		// You stop where you are: back only the step you've just taken out of Viridian.
		Vec3 last = lastWhere(p, v -> "viridian_city".equals(placeAt(v.x, v.z)));
		if (last != null) stopAt(p, last);
		else {
			BlockPos back = VIRIDIAN_SAFE.get(p.getUUID());
			if (back == null) {
				Vec3 c = centreOf("viridian_city", level);
				if (c == null) return false;
				back = BlockPos.containing(c);
			}
			stopAt(p, new Vec3(back.getX() + 0.5, back.getY(), back.getZ() + 0.5));
		}
		Npc man = null;
		for (Npc n : level.getEntitiesOfClass(Npc.class, p.getBoundingBox().inflate(64), n -> n.key().equals(OLD_MAN)))
			if (man == null || n.distanceTo(p) < man.distanceTo(p)) man = n;
		if (man != null) man.talker = p;
		// The camera turns to him only if he's by you (he's across the road north); else it stays on you.
		boolean near = man != null && man.distanceTo(p) < 12;
		dialogue(p, man, "talk", OLD_MAN_GRUMPY, new CompoundTag(), near ? man.getId() : -1);
		return true;
	}

	// ---------- trainers who spot you ----------

	/** Someone walking up to you: a trainer (then the battle), or anyone with something to say (then that). */
	private record Walk(Npc npc, ServerPlayer player, Trainers.Trainer trainer, Runnable then) {}

	/** The "!" over them, the music, the black bars, and they come to you. */
	private static void walkUp(ServerPlayer p, Npc n, Trainers.Trainer t, String music, Runnable then) {
		CompoundTag e = new CompoundTag();
		e.putInt("npc", n.getId());
		e.putString("music", music == null ? "" : music);
		Net.send(p, "exclaim", e);
		n.getNavigation().stop();
		n.setFacing(n.yawTo(p));
		n.approaching = 160;
		BUSY.put(p.getUUID(), 400);
		WALKS.put(p.getUUID(), new Walk(n, p, t, then));
	}

	private static final Map<UUID, Walk> WALKS = new HashMap<>();

	/**
	 * A walk-up called off (you went through a door before they reached you): you're let go on your screen, and
	 * whoever was coming (MUM with the TOWN MAP, TEAM ROCKET) goes, to come again next time you're out.
	 */
	private static void walkOff(ServerPlayer p, Npc n) {
		BUSY.remove(p.getUUID());
		n.approaching = 0;
		n.talker = null;
		Net.send(p, "release", new CompoundTag());
		if (MUMS.get(p.getUUID()) == n) {
			n.discard();
			MUMS.remove(p.getUUID());
			MUM_HOME.remove(p.getUUID());
		}
		List<Npc> rockets = ROCKETS.get(p.getUUID());
		if (rockets != null && rockets.contains(n)) {
			for (Npc r : rockets) if (!r.isRemoved()) r.discard();
			ROCKETS.remove(p.getUUID());
			data(p).story.remove("rocket_viridian");
		}
	}

	/** Talking to someone or in a battle: people you're with stay put and face you until it's over. */
	static boolean engaged(ServerPlayer p) {
		return BUSY.containsKey(p.getUUID()) || Battles.in(p);
	}

	/**
	 * Any trainer who hasn't battled you yet and sees you walks up to you: you're spotted if you're ahead of them
	 * (within about 70 degrees of the way they're facing as they pace) and within their reach, 5 to 10 blocks
	 * (further for the ones who watched further in the games), or right beside them whichever way they face,
	 * with nothing in between.
	 */
	/** Where each player was when trainers last looked (so walking fast past one, you're still seen; 14:15). */
	private static final Map<UUID, Vec3> SIGHT_PREV = new HashMap<>();

	private static void watch(ServerPlayer p) {
		PlayerData d = data(p);
		Vec3 prev = SIGHT_PREV.put(p.getUUID(), p.position());
		if (!d.started || !d.anyHealthy() || WALKS.containsKey(p.getUUID())) return;
		ServerLevel level = (ServerLevel) p.level();
		for (Npc n : level.getEntitiesOfClass(Npc.class, p.getBoundingBox().inflate(14))) {
			if (n.role() != Npc.TRAINER && n.role() != Npc.LEADER || n.approaching > 0 || n.talker != null) continue;
			Trainers.Trainer t = Trainers.get(n.key());
			if (t == null || t.sight() <= 0 || d.beaten.contains(t.key()) || (battlesOff(p) && !storyBattle(t)) || !spots(n, p, t, prev)) continue;
			walkUp(p, n, t, spottedMusic(t), null);
			return;
		}
	}

	/** Whether a trainer sees you (see watch). */
	static boolean spots(Npc n, ServerPlayer p, Trainers.Trainer t) {
		return spots(n, p, t, null);
	}

	static boolean spots(Npc n, ServerPlayer p, Trainers.Trainer t, Vec3 prev) {
		double dx = p.getX() - n.getX(), dz = p.getZ() - n.getZ(), dy = p.getY() - n.getY();
		if (Math.abs(dy) > 2.5) return false;
		// In the forest, the Gym and Mt. Moon (laid out step for step on FireRed's), as FireRed has it: they see straight
		// ahead, as many of its steps as their sight, in a line as wide as a step, and nothing to the side or behind; and
		// anywhere along the way you've come since they last looked (you can't run past them unseen).
		double[] tile = Interiors.tileOf(n);
		if (tile != null) {
			double yaw = Math.toRadians(n.facing());
			double fx = -Math.sin(yaw), fz = Math.cos(yaw);
			boolean alongX = Math.abs(fx) > Math.abs(fz);
			double step = alongX ? tile[0] : tile[1], width = alongX ? tile[1] : tile[0];
			boolean seen = false;
			int samples = prev != null && prev.distanceToSqr(p.position()) < 64 ? 6 : 0;
			for (int k = 0; k <= samples && !seen; k++) {
				double f = samples == 0 ? 1 : k / (double) samples;
				double px = prev == null || samples == 0 ? p.getX() : prev.x + (p.getX() - prev.x) * f, pz = prev == null || samples == 0 ? p.getZ() : prev.z + (p.getZ() - prev.z) * f;
				double ddx = px - n.getX(), ddz = pz - n.getZ();
				double ahead = ddx * fx + ddz * fz, side = Math.abs(ddx * fz - ddz * fx);
				seen = !(ahead < 0.3 || ahead > t.sight() * step + 0.6 || side > width / 2 + 0.45);
			}
			return seen && n.hasLineOfSight(p);
		}
		double dist = Math.sqrt(dx * dx + dz * dz), reach = Math.max(5, Math.min(10, 3 + t.sight() * 1.5));
		if (dist > reach) return false;
		if (dist > 2.2) {
			double yaw = Math.toRadians(n.facing());
			double ahead = (dx * -Math.sin(yaw) + dz * Math.cos(yaw)) / dist;
			if (ahead < 0.34) return false;
		}
		return n.hasLineOfSight(p);
	}

	/** The "eyes meet" tune: the girls', the boys' or Team Rocket's. */
	static String spottedMusic(Trainers.Trainer t) {
		if (t.key().startsWith("rival")) return "meet_rival";
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
			// You've gone through a door since they saw you (into a room, back out): it's off, and you can move again.
			if (n.level() != p.level()) {
				walkOff(p, n);
				return true;
			}
			n.approaching--;
			double dist = n.distanceTo(p);
			// The "!" (a second), then they come over: round whatever's in the way, not just in a line.
			if (n.approaching > 140) {
				n.getNavigation().stop();
				n.setFacing(n.yawTo(p));
				return false;
			}
			// The clerk behind his counter (Viridian's Mart, calling you over for OAK's PARCEL) and the nurse behind
			// hers don't come round: they talk to you from where they are, after the "!".
			boolean stays = n.role() == Npc.CLERK || n.role() == Npc.NURSE;
			if (dist > 1.9 && n.approaching > 0 && !stays) {
				if (n.approaching % 5 == 0 || n.getNavigation().isDone()) n.getNavigation().moveTo(p, 0.95);
				return false;
			}
			n.getNavigation().stop();
			// Still not with you (something's in their way): they step up beside you, so you're face to face.
			if (dist > 3.5 && !stays && n.level() instanceof ServerLevel lv) {
				int sx = (int) Math.signum(Math.round(n.getX() - p.getX())), sz = (int) Math.signum(Math.round(n.getZ() - p.getZ()));
				BlockPos near = alongRoute(lv, p, sx != 0 ? sx : 0, sx != 0 ? 0 : (sz != 0 ? sz : 1), 2, 2);
				if (near == null) near = spot(lv, p.getBlockX() + sx * 2, p.getBlockZ() + sz * 2, 2);
				if (near != null) {
					n.teleportTo(near.getX() + 0.5, near.getY(), near.getZ() + 0.5);
					n.setFacing(n.yawTo(p));
				}
			}
			n.approaching = 0;
			n.talker = p;
			BUSY.remove(p.getUUID());
			if (w.then() != null) w.then().run();
			else if (w.trainer() != null) talkBattle(p, n, w.trainer());
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
		// How you made yourself in the new game's maker (skin, eyes, hair, clothes).
		String o = t.getStringOr("outfit", "");
		if (!o.isEmpty() && o.length() <= 200 && (!d.started || d.outfit.isEmpty())) d.outfit = o;
		if (d.since == 0) {
			d.since = System.currentTimeMillis();
			// A new adventure begins at six in the morning: day 1.
			ServerLevel lv = p.level().getServer().overworld();
			long now = lv.getDayTime(), base = now - Math.floorMod(now, 24000L);
			lv.setDayTime(base);
			d.day0 = base;
		}
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
		n.getNavigation().stop();
		n.talker = p;
		switch (n.role()) {
			case Npc.NURSE -> dialogue(p, n, "heal", List.of("NURSE: Hello, and welcome to the POKéMON CENTER!",
					"NURSE: We heal tired POKéMON back to full health. Shall we heal yours?"));
			case Npc.CLERK -> {
				// Viridian's Mart before the errand: OAK's PARCEL, and no shopping until it's delivered.
				if (viridianMart(n) && "viridian_city".equals(d.place) && d.started && !d.hasDex()) {
					if (d.story.contains("parcel")) dialogue(p, n, "talk", List.of("Okay! Say hi to PROF.OAK for me!"));
					else giveParcel(p, n);
					return;
				}
				CompoundTag t = new CompoundTag();
				// (The clerk in the room every Mart shares sells the stock of the town you're in.)
				t.putString("mart", n.key().isEmpty() || Interiors.in(p) ? d.place : n.key());
				dialogue(p, n, "mart", List.of("Hi there! May I help you?"), t);
			}
			case Npc.PERSON -> {
				if (n.key().equals(OAK_KEY)) {
					oakTalk(p, n);
					return;
				}
				if (n.key().equals("viridian_school_teacher")) {
					schoolQuestion(p, n);
					return;
				}
				// Route 1's MART clerk: a free POTION, once. PROF. OAK's AIDE in Route 2's east gate: HM05 for 10 kinds caught.
				// Route 2's TRADER: his MR. MIME for your ABRA.
				if (n.key().equals("museum_1f_desk") || n.key().equals("museum_1f_amber")) {
					museumTalk(p, n, d);
					return;
				}
				if (n.key().equals("route_1_clerk") || n.key().equals("route_2_gate_p0") || n.key().equals("route_2_trader")) {
					personGift(p, n, d);
					return;
				}
				// Fuchsia City (15:33): the SAFARI ZONE WARDEN (his GOLD TEETH, HM04), the FISHING GURU's brother (the GOOD
				// ROD), the MOVE DELETER, the SAFARI ZONE's gate.
				if (n.key().equals("wardens_house_p0")) {
					warden(p, n, d);
					return;
				}
				if (n.key().equals("fuchsia_good_rod_house_p0")) {
					goodRodTalk(p, n, d);
					return;
				}
				// Vermilion City (17:51): the FISHING GURU (the OLD ROD), the POKéMON FAN CLUB's CHAIRMAN (the BIKE VOUCHER).
				if (n.key().equals("vermilion_old_rod_house_p0")) {
					oldRodTalk(p, n, d);
					return;
				}
				if (n.key().equals("pokemon_fan_club_p4")) {
					chairman(p, n, d);
					return;
				}
				// Lavender Town (19:06): the NAME RATER.
				if (n.key().equals("name_raters_house_p0")) {
					dialogue(p, n, "rater", List.of("Hello, hello! I am the official NAME RATER!", "Want me to rate the nicknames of your POKéMON?"), yesNo(), n.getId());
					return;
				}
				if (n.key().equals("fuchsia_move_deleter_p0")) {
					deleter(p, n);
					return;
				}
				if (n.key().equals("safari_zone_gate_p0")) {
					if (SAFARI.containsKey(p.getUUID())) dialogue(p, n, "talk", List.of("Good luck! Catch plenty of POKéMON!"));
					else safariAsk(p);
					return;
				}
				if (n.key().equals("mt_moon_pokecenter_salesman")) {
					magikarpMan(p, n, d);
					return;
				}
				// The Underground Path's TRADER and Cerulean's (16:17, 16:20), and the BIKE SHOP's clerk.
				if (TRADERS.containsKey(n.key())) {
					trader(p, n, d);
					return;
				}
				if (n.key().equals("bike_shop_p0")) {
					bikeShop(p, n, d);
					return;
				}
				if (n.key().equals("daycare_p0")) {
					daycare(p, n, d);
					return;
				}
				if (n.key().startsWith("route_4_tutor_")) {
					tutor(p, n, d);
					return;
				}
				if (n.key().equals(OLD_MAN) && !d.hasDex()) {
					dialogue(p, n, "talk", OLD_MAN_GRUMPY);
					return;
				}
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
				// The Trainer House's challenger: one battle a day.
				if (n.key().equals("trainer_house")) {
					if (d.story.contains(thToday())) {
						dialogue(p, n, "talk", List.of("That was a great battle!", "Come back tomorrow: there'll be someone new waiting here."));
						return;
					}
					if (!d.started || !d.anyHealthy()) {
						dialogue(p, n, "talk", List.of("You don't have any POKéMON that can battle!"));
						return;
					}
					d.beaten.remove("trainer_house");
					talkBattle(p, n, trainerHouse(p));
					return;
				}
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
	/**
	 * Whether there's another of the same one (the same key) nearby that came first (a lower id): two of one person or
	 * item ball inside, from a room put in again while the save's were still loading.
	 */
	static boolean twin(net.minecraft.world.entity.Entity e, String key) {
		if (key == null || key.isEmpty()) return false;
		net.minecraft.world.phys.AABB box = e.getBoundingBox().inflate(96, 24, 96);
		for (net.minecraft.world.entity.Entity o : e.level().getEntities(e, box, o -> o.getClass() == e.getClass())) {
			String k = o instanceof Npc n ? n.key() : o instanceof WorldMon m ? m.key() : null;
			if (key.equals(k) && o.getId() < e.getId() && !o.isRemoved()) return true;
		}
		return false;
	}

	private static void pickUp(ServerPlayer p, WorldMon w, String key) {
		PlayerData d = data(p);
		Trainers.FieldItem fi = Trainers.FIELD_ITEMS.get(key);
		if (fi == null || d.statics.contains("item:" + key)) {
			w.discard();
			return;
		}
		if (key.equals("wardens_house_rare_candy") && !(d.count("hm_strength") > 0 && d.badges.contains("SOULBADGE"))) {
			message(p, List.of("A big boulder's in the way.", "A POKéMON may be able to move it with STRENGTH."));
			return;
		}
		d.statics.add("item:" + key);
		// (Mt. Moon's mushrooms grow back: the day it was picked, so it's there again the next day.)
		if (fi.item().contains("mushroom") || key.endsWith("_again")) d.statics.add("picked:" + key + "=" + p.level().getServer().overworld().getDayTime() / 24000L);
		Item it = Item.find(fi.item());
		String name = it != null ? it.name() : fi.item().replace("tm_", "TM ").replace('_', ' ').toUpperCase();
		if (it != null) d.give(fi.item(), 1);
		w.discard();
		// (15:21) And any other of the same ball about (one put in twice), so none is left where it was.
		for (WorldMon o : p.level().getEntitiesOfClass(WorldMon.class, w.getBoundingBox().inflate(96, 24, 96), o -> ("item:" + key).equals(o.key()))) o.discard();
		sync(p);
		CompoundTag s = new CompoundTag();
		s.putString("sound", "item");
		Net.send(p, "sound", s);
		message(p, List.of(player(p) + " found " + name + "!"));
	}


	// ---------- the SAFARI ZONE (15:29, 15:32), as FireRed's ----------

	/** Who's on a SAFARI GAME, and how many steps they've left (FireRed's 600, a step every two blocks). */
	private static final Map<UUID, int[]> SAFARI = new HashMap<>();
	private static final int SAFARI_STEPS = 600, SAFARI_BALLS = 30, SAFARI_FEE = 500;
	/** The areas of the SAFARI ZONE's room (tools/safari.py): place, x0 z0 x1 z1 in the room. */
	private static final Object[][] SAFARI_AREAS = {{"safari_zone_center", 116, 126, 218, 198}, {"safari_zone_west", 8, 108, 104, 180},
			{"safari_zone_north", 106, 8, 220, 88}, {"safari_zone_east", 236, 104, 344, 174}};

	/** At the gate's counter, on the way in: the attendant asks (FireRed's words). */
	private static void safariAsk(ServerPlayer p) {
		Npc n = null;
		for (Npc o : ((ServerLevel) p.level()).getEntitiesOfClass(Npc.class, p.getBoundingBox().inflate(16), o -> o.key().equals("safari_zone_gate_p0"))) n = o;
		CompoundTag t = new CompoundTag();
		ListTag ch = new ListTag();
		for (String c : List.of("YES", "NO")) ch.add(StringTag.valueOf(c));
		t.put("choices", ch);
		dialogue(p, n, "safari_fee", List.of("Welcome to the SAFARI ZONE!", "For just ¥500, you can catch all the POKéMON you want in the park!",
				"Would you like to join the hunt?"), t, n == null ? -1 : n.getId());
	}

	private static void safariFee(ServerPlayer p, Npc n, boolean yes) {
		PlayerData d = data(p);
		if (!yes) {
			dialogue(p, n, "talk", List.of("OK! Please come again!"));
			return;
		}
		if (d.money < SAFARI_FEE) {
			dialogue(p, n, "talk", List.of("Oops! Not enough money!"));
			return;
		}
		d.money -= SAFARI_FEE;
		d.give("safari_ball", SAFARI_BALLS);
		SAFARI.put(p.getUUID(), new int[] {SAFARI_STEPS, 0});
		sync(p);
		safariHud(p);
		dialogue(p, n, "talk", fill(p, List.of("That'll be ¥500, please!", "We only use a special POKé BALL here.", "{player} received 30 SAFARI BALLs!",
				"We'll call you on the PA when you run out of time or SAFARI BALLs!", "Go on through, and good luck!")));
	}

	/** A step in the SAFARI ZONE: one off the steps left every two blocks; none left, the game's over. */
	private static void safariStep(ServerPlayer p) {
		int[] s = SAFARI.get(p.getUUID());
		Interiors.Room r = Interiors.roomOf(p);
		if (s == null || r == null || !r.id().equals("safari_zone")) return;
		if (++s[1] < 2) return;
		s[1] = 0;
		s[0]--;
		if (s[0] % 25 == 0 || s[0] <= 10) safariHud(p);
		if (s[0] <= 0) safariOver(p, "Time's up!");
	}

	/** Every half second in the SAFARI ZONE: the area you're in (its POKéMON, its name), and whether you've balls left. */
	private static void safariTick(ServerPlayer p) {
		Interiors.Room r = Interiors.roomOf(p);
		if (r == null) return;
		if (r.id().equals("safari_gate") && SAFARI.containsKey(p.getUUID()) && Interiors.local(p).z > 5.5) {
			// Back out past the counter (out of the park, or never gone in): the game's over, the balls handed back.
			safariEnd(p);
			dialogue(p, null, "talk", List.of("Did you get a good haul?", "Come again!"), whoTag("ATTENDANT"));
			return;
		}
		if (!r.id().equals("safari_zone")) return;
		Vec3 l = Interiors.local(p);
		String place = null;
		double best = 1e9;
		for (Object[] a : SAFARI_AREAS) {
			int x0 = (Integer) a[1], z0 = (Integer) a[2], x1 = (Integer) a[3], z1 = (Integer) a[4];
			double dx = Math.max(Math.max(x0 - l.x, 0), l.x - x1), dz = Math.max(Math.max(z0 - l.z, 0), l.z - z1), dd = dx * dx + dz * dz;
			if (dd < best) {
				best = dd;
				place = (String) a[0];
			}
		}
		if (place != null) enter(p, place);
		if (SAFARI.containsKey(p.getUUID()) && data(p).count("safari_ball") <= 0 && !AFTER_BATTLE.containsKey(p.getUUID())) safariOver(p, "You have no SAFARI BALLs left!");
	}

	/** The PA calls time: back to the gate, the game over. */
	private static void safariOver(ServerPlayer p, String why) {
		safariEnd(p);
		Interiors.Room gate = Interiors.room("safari_gate");
		if (gate != null) Interiors.go(p, gate, 7.0, 2.6, 0F, true);
		dialogue(p, null, "talk", List.of("PA: Ding-dong!", "PA: " + why, "PA: Your SAFARI GAME is over!", "Did you get a good haul?", "Come again!"), whoTag("PA"));
	}

	private static void safariEnd(ServerPlayer p) {
		if (SAFARI.remove(p.getUUID()) == null) return;
		PlayerData d = data(p);
		int left = d.count("safari_ball");
		if (left > 0) d.give("safari_ball", -left);
		sync(p);
		CompoundTag t = new CompoundTag();
		t.putInt("steps", -1);
		Net.send(p, "safari", t);
	}

	/** The steps and balls left, on your screen (the HUD shows them while you're in the park). */
	private static void safariHud(ServerPlayer p) {
		int[] s = SAFARI.get(p.getUUID());
		CompoundTag t = new CompoundTag();
		t.putInt("steps", s == null ? -1 : s[0]);
		t.putInt("balls", data(p).count("safari_ball"));
		Net.send(p, "safari", t);
	}

	/** After a battle in the park: the balls left on your screen. */
	public static void safariAfterBattle(ServerPlayer p) {
		if (SAFARI.containsKey(p.getUUID())) safariHud(p);
	}

	// ---------- Fuchsia City's houses (15:33) ----------

	/** The SAFARI ZONE WARDEN: without his GOLD TEETH, you can't make him out; bring them, and he gives you HM04. */
	private static void warden(ServerPlayer p, Npc n, PlayerData d) {
		if (d.count("hm_strength") > 0) {
			dialogue(p, n, "talk", List.of("WARDEN: HM04 teaches STRENGTH!", "It lets POKéMON move boulders when you're outside of battle.",
					"Oh yes, did you find any POKéMON you liked in the SAFARI ZONE?"));
			return;
		}
		if (d.count("gold_teeth") <= 0) {
			dialogue(p, n, "talk", List.of("WARDEN: Hif fuff hefifoo!", "Ha lof ha feef ee hafahi ho. Heff hee fwee!"));
			return;
		}
		d.take("gold_teeth");
		d.give("hm_strength", 1);
		sync(p);
		dialogue(p, n, "talk", fill(p, List.of("WARDEN: Ah! Oh! My GOLD TEETH! Thanks, kid!", "No one could understand a word that I said.",
				"I couldn't work that way.", "Let me give you something for your trouble.", "{player} received HM04 from the WARDEN!",
				"HM04 teaches STRENGTH! It lets POKéMON move boulders when you're outside of battle.")));
	}

	private static void goodRodTalk(ServerPlayer p, Npc n, PlayerData d) {
		if (d.count("good_rod") > 0) {
			dialogue(p, n, "talk", fill(p, List.of("Hello there, {player}!", "How are the fish biting?")));
			return;
		}
		CompoundTag t = new CompoundTag();
		ListTag ch = new ListTag();
		for (String c : List.of("YES", "NO")) ch.add(StringTag.valueOf(c));
		t.put("choices", ch);
		dialogue(p, n, "good_rod", List.of("I'm the FISHING GURU's older brother!", "I simply Looove fishing!", "Do you like to fish?"), t, n.getId());
	}

	private static void oldRodTalk(ServerPlayer p, Npc n, PlayerData d) {
		if (d.count("old_rod") > 0) {
			dialogue(p, n, "talk", fill(p, List.of("Hello there, {player}!", "How are the fish biting?")));
			return;
		}
		dialogue(p, n, "old_rod", List.of("I'm the FISHING GURU!", "I simply Looove fishing!", "Do you like to fish?"), yesNo(), n.getId());
	}

	private static void oldRod(ServerPlayer p, Npc n, boolean yes) {
		PlayerData d = data(p);
		if (!yes) {
			dialogue(p, n, "talk", List.of("Oh... That's so disappointing..."));
			return;
		}
		if (d.count("old_rod") > 0) return;
		d.give("old_rod", 1);
		sync(p);
		dialogue(p, n, "talk", fill(p, List.of("Grand! I like your style!", "Take this and fish, young one!", "{player} received an OLD ROD!",
				"Fishing is a way of life! From the seas to rivers, go out and land the big one, young one!")));
	}

	/** The POKéMON FAN CLUB's CHAIRMAN (Vermilion, FireRed's): hear him out, and he gives you the BIKE VOUCHER. */
	private static void chairman(ServerPlayer p, Npc n, PlayerData d) {
		if (d.count("bike_voucher") > 0 || d.statics.contains("bike_voucher")) {
			dialogue(p, n, "talk", fill(p, List.of("Hello, {player}!", "Did you come see me about my POKéMON again?", "No? Too bad!")));
			return;
		}
		dialogue(p, n, "chairman", List.of("I chair the POKéMON FAN CLUB!", "I have collected over 100 POKéMON!", "I'm very fussy when it comes to POKéMON!",
				"So...", "Did you come visit to hear about my POKéMON?"), yesNo(), n.getId());
	}

	private static void chairmanStory(ServerPlayer p, Npc n, boolean yes) {
		PlayerData d = data(p);
		if (!yes) {
			dialogue(p, n, "talk", List.of("Oh. Come back when you want to hear my story!"));
			return;
		}
		if (d.statics.contains("bike_voucher")) return;
		d.statics.add("bike_voucher");
		d.give("bike_voucher", 1);
		sync(p);
		dialogue(p, n, "talk", fill(p, List.of("Good!", "Then listen up!", "My favorite RAPIDASH...", "It... cute... lovely... smart... plus... amazing... you think so?... oh yes... it... stunning... kindly... love it!",
				"Hug it... when... sleeping... warm and cuddly... spectacular... ravishing... ...Oops! Look at the time! I kept you too long!",
				"Thanks for hearing me out!", "I want you to have this!", "{player} received a BIKE VOUCHER!", "Exchange that for a BICYCLE!",
				"Don't worry, my FEAROW will FLY me anywhere!", "So, I don't need a BICYCLE!", "I hope you like cycling!")));
	}

	private static void goodRod(ServerPlayer p, Npc n, boolean yes) {
		PlayerData d = data(p);
		if (!yes) {
			dialogue(p, n, "talk", List.of("Oh... That's so disappointing..."));
			return;
		}
		if (d.count("good_rod") > 0) return;
		d.give("good_rod", 1);
		sync(p);
		dialogue(p, n, "talk", fill(p, List.of("Grand! I like your style!", "Take this and fish, young one!", "{player} received a GOOD ROD!",
				"Fishing is a way of life! From the seas to rivers, go out and land the big one!")));
	}

	/** The MOVE DELETER (FireRed's, in Fuchsia): a POKéMON of yours forgets a move of its choosing. */
	private static void deleter(ServerPlayer p, Npc n) {
		CompoundTag t = new CompoundTag();
		ListTag ch = new ListTag();
		for (String c : List.of("YES", "NO")) ch.add(StringTag.valueOf(c));
		t.put("choices", ch);
		dialogue(p, n, "deleter", List.of("Uh... Oh, yes, I'm the MOVE DELETER.", "I can make POKéMON forget their moves.", "Would you like me to do that?"), t, n.getId());
	}

	private static void deleterAsk(ServerPlayer p, Npc n, boolean yes) {
		PlayerData d = data(p);
		if (!yes || d.party.isEmpty()) {
			dialogue(p, n, "talk", List.of("Come again if there are moves to be forgotten."));
			return;
		}
		CompoundTag t = new CompoundTag();
		ListTag ch = new ListTag();
		for (Mon m : d.party) ch.add(StringTag.valueOf(m.name()));
		ch.add(StringTag.valueOf("CANCEL"));
		t.put("choices", ch);
		dialogue(p, n, "deleter_mon", List.of("Which POKéMON should forget a move?"), t, n == null ? -1 : n.getId());
	}

	private static void deleterMon(ServerPlayer p, Npc n, CompoundTag in) {
		PlayerData d = data(p);
		int i = in.getIntOr("choice", -1);
		if (!in.getBooleanOr("answered", false) || i < 0 || i >= d.party.size()) return;
		Mon m = d.party.get(i);
		if (m.moves.size() <= 1) {
			dialogue(p, n, "talk", List.of(m.name() + " seems to know only one move..."));
			return;
		}
		CompoundTag t = new CompoundTag();
		ListTag ch = new ListTag();
		for (String mv : m.moves) ch.add(StringTag.valueOf(Move.get(mv).name()));
		ch.add(StringTag.valueOf("CANCEL"));
		t.put("choices", ch);
		t.putInt("mon", i);
		dialogue(p, n, "deleter_move", List.of("Which move should be forgotten?"), t, n == null ? -1 : n.getId());
	}

	private static void deleterMove(ServerPlayer p, Npc n, CompoundTag in) {
		PlayerData d = data(p);
		int i = in.getIntOr("mon", -1), k = in.getIntOr("choice", -1);
		if (!in.getBooleanOr("answered", false) || i < 0 || i >= d.party.size()) return;
		Mon m = d.party.get(i);
		if (k < 0 || k >= m.moves.size() || m.moves.size() <= 1) return;
		String name = Move.get(m.moves.get(k)).name();
		m.moves.remove(k);
		m.pp.remove(k);
		sync(p);
		dialogue(p, n, "talk", List.of("Hm! Hm! Hm!", m.name() + " forgot " + name + "!", "Come again if there are other moves to be forgotten."));
	}

	// ---------- Pewter City ----------

	/** Who's been asked at the Museum's desk on this way in (so it isn't asked again until they've stepped back). */
	private static final java.util.Set<UUID> MUSEUM_ASKED = new java.util.HashSet<>();

	/**
	 * The Museum of Science's desk, as FireRed's: going in by the main door, past the desk (the line in front of it), you're
	 * asked for ¥50 for a child's ticket, once (after that you come and go as you like). No, or not enough money: back a
	 * step towards the door.
	 */
	private static void museumDesk(ServerPlayer p) {
		Interiors.Room r = Interiors.roomOf(p);
		if (r == null || !r.id().equals("museum_1f")) return;
		PlayerData d = data(p);
		Vec3 l = Interiors.local(p);
		boolean past = l.x > 10.5 && l.x < 16.4 && l.z < 6.2;
		if (!past) {
			if (l.z > 7.2) MUSEUM_ASKED.remove(p.getUUID());
			return;
		}
		if (d.story.contains("museum_paid") || MUSEUM_ASKED.contains(p.getUUID())) {
			if (!d.story.contains("museum_paid")) museumBack(p, r);
			return;
		}
		MUSEUM_ASKED.add(p.getUUID());
		museumBack(p, r);
		Npc desk = null;
		for (Npc n : ((ServerLevel) p.level()).getEntitiesOfClass(Npc.class, p.getBoundingBox().inflate(12), n -> n.key().equals("museum_1f_desk"))) desk = n;
		CompoundTag t = new CompoundTag();
		ListTag ch = new ListTag();
		for (String c : List.of("YES", "NO")) ch.add(StringTag.valueOf(c));
		t.put("choices", ch);
		dialogue(p, desk, "museum_fee", List.of("It's ¥50 for a child's ticket. Would you like to come in?"), t, desk == null ? -1 : desk.getId());
	}

	/** Back from the desk, a step towards the door. */
	private static void museumBack(ServerPlayer p, Interiors.Room r) {
		p.teleportTo(p.getX(), p.getY(), r.oz() + 6.9);
		p.setDeltaMovement(Vec3.ZERO);
	}

	private static void museumFee(ServerPlayer p, Npc n, boolean yes) {
		PlayerData d = data(p);
		if (!yes) {
			dialogue(p, n, "talk", List.of("Come again!"));
			return;
		}
		if (d.money < 50) {
			dialogue(p, n, "talk", List.of("You don't have enough money.", "Come again!"));
			return;
		}
		d.money -= 50;
		d.story.add("museum_paid");
		sync(p);
		dialogue(p, n, "talk", List.of("Right, ¥50! Thank you!", "Take plenty of time to look!"));
	}

	/** The Museum's desk (in person) and the scientist with the OLD AMBER. */
	private static void museumTalk(ServerPlayer p, Npc n, PlayerData d) {
		if (n.key().equals("museum_1f_desk")) {
			if (d.story.contains("museum_paid")) dialogue(p, n, "talk", List.of("Take plenty of time to look!"));
			else {
				CompoundTag t = new CompoundTag();
				ListTag ch = new ListTag();
				for (String c : List.of("YES", "NO")) ch.add(StringTag.valueOf(c));
				t.put("choices", ch);
				dialogue(p, n, "museum_fee", List.of("It's ¥50 for a child's ticket. Would you like to come in?"), t, n.getId());
			}
			return;
		}
		if (d.statics.add("gift:old_amber")) {
			d.give("old_amber", 1);
			sync(p);
			dialogue(p, n, "talk", fill(p, List.of("Ssh! I think that this chunk of AMBER contains POKéMON DNA!",
					"It would be great if POKéMON could be resurrected from it!", "But, my colleagues just ignore me!",
					"So I have a favor to ask!", "Take this to a POKéMON LAB and get it examined!", "{player} received OLD AMBER!")));
		} else dialogue(p, n, "talk", List.of("Ssh! Get the OLD AMBER checked!"));
	}

	/**
	 * Pewter City's way east (to ROUTE 3), as FireRed's: before you've beaten BROCK, the boy there stops you ("BROCK's
	 * looking for new challengers! Follow me!") and takes you to the GYM's door. The way east's box: x0 x1 z0 z1.
	 */
	private static final double[] PEWTER_EAST = {-752.0, -740.0, 299.0, 314.0};
	private static final Vec3 PEWTER_GYM_FRONT = new Vec3(-807.5, 31.0, 299.5);

	private static void pewterGuide(ServerPlayer p) {
		PlayerData d = data(p);
		if (!ourSave || !d.started || ESCORTS.containsKey(p.getUUID()) || WALKS.containsKey(p.getUUID())) return;
		double x = p.getX(), z = p.getZ();
		if (x < PEWTER_EAST[0] || x > PEWTER_EAST[1] || z < PEWTER_EAST[2] || z > PEWTER_EAST[3]) return;
		boolean badge = d.badges.contains("BOULDERBADGE");
		if (badge && d.story.contains("running_shoes")) return;
		// You stop where you are (a step back, out of the way east).
		Vec3 last = lastWhere(p, v -> v.x < PEWTER_EAST[0] - 0.4);
		stopAt(p, last != null ? last : new Vec3(PEWTER_EAST[0] - 1.2, p.getY(), p.getZ()));
		ServerLevel level = (ServerLevel) p.level();
		if (badge) {
			// PROF. OAK's AIDE comes running with the RUNNING SHOES (FireRed's, on the way to ROUTE 3).
			BlockPos at = spot(level, p.getBlockX() - 4, p.getBlockZ(), 3);
			Npc aide = place(level, Npc.PERSON, "pewter_city_aide", at.getX() + 0.5, at.getY(), at.getZ() + 0.5, yawOf("right", 0F));
			if (aide == null) return;
			aide.setCustomName(net.minecraft.network.chat.Component.literal("AIDE"));
			walkUp(p, aide, null, "", () -> {
				d.story.add("running_shoes");
				d.give("running_shoes", 1);
				sync(p);
				CompoundTag s = new CompoundTag();
				s.putString("sound", "item");
				Net.send(p, "sound", s);
				dialogue(p, aide, "aide_shoes", fill(p, List.of("Oh, {player}! I've been looking for you!", "It's me, one of the ever-present AIDES to PROF. OAK.",
						"PROF. OAK asked me to give you these. Here you go!", "{player} received the RUNNING SHOES!", "{player} switched shoes with the RUNNING SHOES.",
						"Now you can run anywhere, faster than before! (TAB changes your speed.)", "Well, I'd better get back. PROF. OAK will be waiting.")));
			});
			return;
		}
		Npc boy = null;
		for (Npc n : level.getEntitiesOfClass(Npc.class, p.getBoundingBox().inflate(24), n -> n.key().equals("pewter_city_p4"))) boy = n;
		if (boy == null) {
			p.teleportTo(PEWTER_GYM_FRONT.x, PEWTER_GYM_FRONT.y, PEWTER_GYM_FRONT.z);
			dialogue(p, null, "talk", List.of("BROCK's looking for new challengers! Go and take him on at the GYM!"));
			return;
		}
		Npc b = boy;
		walkUp(p, boy, null, "", () -> dialogue(p, b, "guide", List.of("You're a trainer, right? BROCK's looking for new challengers!", "Follow me!")));
	}

	/**
	 * The way from Pewter's east way out to the GYM's door, round the town's beds and lamps (found on John's map): the
	 * boy walks it, you behind him.
	 */
	private static final double[][] PEWTER_TO_GYM = {{-751.5, 30, 306.5}, {-792.5, 30, 306.5}, {-792.5, 30, 290.5}, {-793.5, 30, 290.5}, {-793.5, 30, 289.5},
			{-799.5, 30, 289.5}, {-799.5, 30, 296.5}, {-802.5, 30, 296.5}, {-802.5, 30, 297.5}, {-803.5, 30, 297.5}, {-803.5, 30, 299.5}, {-807.5, 30, 299.5}};

	/** Someone walking you somewhere (Pewter's boy to the GYM): how far along the way they are. */
	private static final class Escort {
		final Npc npc;
		final ServerPlayer player;
		final double[][] path;
		double along;
		int ticks;

		Escort(Npc npc, ServerPlayer player, double[][] path) {
			this.npc = npc;
			this.player = player;
			this.path = path;
		}
	}

	private static final Map<UUID, Escort> ESCORTS = new HashMap<>();
	/** The boy's way home after (his home spot, and when). */
	private static final Map<Npc, Integer> GOING_HOME = new HashMap<>();

	/** A point the way along a path (and the way it heads there, as a yaw). */
	private static double[] along(double[][] path, double s) {
		double left = Math.max(0, s);
		for (int i = 0; i + 1 < path.length; i++) {
			double[] a = path[i], b = path[i + 1];
			double len = Math.hypot(b[0] - a[0], b[2] - a[2]);
			if (left <= len || i + 2 == path.length) {
				double f = len == 0 ? 1 : Math.min(1, left / len);
				return new double[] {a[0] + (b[0] - a[0]) * f, a[1] + (b[1] - a[1]) * f, a[2] + (b[2] - a[2]) * f, Math.toDegrees(Math.atan2(-(b[0] - a[0]), b[2] - a[2]))};
			}
			left -= len;
		}
		double[] e = path[path.length - 1];
		return new double[] {e[0], e[1], e[2], 0};
	}

	private static double pathLength(double[][] path) {
		double l = 0;
		for (int i = 0; i + 1 < path.length; i++) l += Math.hypot(path[i + 1][0] - path[i][0], path[i + 1][2] - path[i][2]);
		return l;
	}

	/** Each tick: the boy walks on along the way, you a step and a half behind him; at the GYM's door, his last word. */
	private static void escorts() {
		ESCORTS.entrySet().removeIf(en -> {
			Escort e = en.getValue();
			ServerPlayer p = e.player;
			if (e.npc.isRemoved() || p.isRemoved() || p.hasDisconnected() || e.npc.level() != p.level()) {
				BUSY.remove(p.getUUID());
				return true;
			}
			double total = pathLength(e.path);
			e.along = Math.min(total, e.along + 0.2);
			e.ticks++;
			double[] at = along(e.path, e.along), me = along(e.path, e.along - 1.5);
			// (17:51) He walks ahead (moved a step at a time, so he's seen walking) and you walk after him on your own
			// feet (your game walks you to the spot behind him), not pulled along.
			e.npc.getNavigation().stop();
			e.npc.setPos(at[0], at[1], at[2]);
			e.npc.setFacing((float) at[3]);
			if (p.position().distanceTo(new Vec3(me[0], me[1], me[2])) > 4.5) p.teleportTo(me[0], me[1], me[2]);
			BUSY.put(p.getUUID(), 40);
			CompoundTag hold = new CompoundTag();
			hold.putInt("ticks", 8);
			Net.send(p, "hold", hold);
			CompoundTag follow = new CompoundTag();
			follow.putDouble("x", me[0]);
			follow.putDouble("z", me[2]);
			follow.putInt("ticks", 8);
			Net.send(p, "follow", follow);
			if (e.along < total && e.ticks < 900) return false;
			e.npc.setFacing(e.npc.yawTo(p));
			BUSY.remove(p.getUUID());
			dialogue(p, e.npc, "talk", List.of("If you have the right stuff, go take on BROCK!"));
			GOING_HOME.put(e.npc, 160);
			return true;
		});
		GOING_HOME.entrySet().removeIf(en -> {
			Npc n = en.getKey();
			if (n.isRemoved()) return true;
			if (n.talker != null) return false;
			int t = en.getValue() - 1;
			en.setValue(t);
			if (t > 0) return false;
			int[] home = spotOf(n.key());
			if (home != null && n.level() instanceof ServerLevel lv) {
				BlockPos at = spot(lv, home[0], home[1], 6);
				n.teleportTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
				n.rehome(at);
			}
			return true;
		});
	}

	/** Mt. Moon's POKéMON CENTER (Route 4): the man selling a MAGIKARP for ¥500, once (FireRed's). */
	private static void magikarpMan(ServerPlayer p, Npc n, PlayerData d) {
		if (d.statics.contains("magikarp_gift")) {
			dialogue(p, n, "talk", List.of("MAN: Well, I don't give refunds!"));
			return;
		}
		CompoundTag t = new CompoundTag();
		ListTag ch = new ListTag();
		for (String c : List.of("YES", "NO")) ch.add(StringTag.valueOf(c));
		t.put("choices", ch);
		dialogue(p, n, "magikarp", List.of("MAN: Hello, there! Have I got a deal just for you!", "I'll let you have a secret POKéMON - a MAGIKARP - for just ¥500!",
				"So, you'll buy it, am I right?"), t, n.getId());
	}

	private static void magikarpSale(ServerPlayer p, Npc n, boolean yes) {
		PlayerData d = data(p);
		if (d.statics.contains("magikarp_gift")) return;
		if (!yes) {
			dialogue(p, n, "talk", List.of("MAN: No? I'm only doing this as a favor to you!"));
			return;
		}
		if (d.money < 500) {
			dialogue(p, n, "talk", List.of("MAN: You'll need more money than that!"));
			return;
		}
		Trainers.Static st = Trainers.STATICS.get("magikarp_gift");
		d.money -= 500;
		Mon m = new Mon(Species.get("magikarp"), st != null ? st.level() : 5, RNG);
		boolean toBox = d.party.size() >= 6;
		d.add(m);
		d.statics.add("magikarp_gift");
		d.seen.add("magikarp");
		d.caught.add("magikarp");
		sync(p);
		CompoundTag s = new CompoundTag();
		s.putString("sound", "caught");
		Net.send(p, "sound", s);
		List<String> lines = new ArrayList<>(List.of(player(p) + " paid an outrageous ¥500 and bought the MAGIKARP!"));
		if (toBox) lines.add("MAGIKARP was sent to BILL's PC.");
		message(p, lines);
		askNickname(p, toBox ? "box" : "party", toBox ? d.box.size() - 1 : d.party.size() - 1);
	}

	/** Route 4's two BLACK BELTs (FireRed's move tutors): MEGA PUNCH and MEGA KICK, each taught once. */
	private static void tutor(ServerPlayer p, Npc n, PlayerData d) {
		String move = n.key().endsWith("punch") ? "mega_punch" : "mega_kick", name = move.equals("mega_punch") ? "MEGA PUNCH" : "MEGA KICK";
		if (d.statics.contains("tutor:" + move)) {
			dialogue(p, n, "talk", List.of("Hah! You've learned " + name + "! Use it well!"));
			return;
		}
		CompoundTag t = new CompoundTag();
		ListTag ch = new ListTag();
		for (Mon m : d.party) ch.add(StringTag.valueOf(m.name()));
		ch.add(StringTag.valueOf("CANCEL"));
		t.put("choices", ch);
		t.putString("move", move);
		dialogue(p, n, "tutor", List.of("Hiyah! My " + name + " is the stuff of legend! I'll teach it to one POKéMON, just once!", "Which POKéMON should learn " + name + "?"), t,
				n.getId());
	}

	private static void tutorChoice(ServerPlayer p, Npc n, CompoundTag t) {
		PlayerData d = data(p);
		String move = t.getStringOr("move", "");
		int i = t.getIntOr("choice", -1);
		if (!t.getBooleanOr("answered", false) || Move.find(move) == null || i < 0 || i >= d.party.size() || d.statics.contains("tutor:" + move)) return;
		Mon m = d.party.get(i);
		if (!m.species.canLearnByMachine(move)) {
			dialogue(p, n, "talk", List.of(m.name() + " can't learn " + Move.get(move).name() + "."));
			return;
		}
		if (m.moves.contains(move)) {
			dialogue(p, n, "talk", List.of(m.name() + " already knows " + Move.get(move).name() + "!"));
			return;
		}
		d.statics.add("tutor:" + move);
		sync(p);
		offerMove(p, i, move);
	}

	/**
	 * Mt. Moon's two fossils (B2F, FireRed's): SUPER NERD MIGUEL won't let you near them until you've beaten him; then you
	 * choose one and he keeps the other.
	 */
	private static void fossil(ServerPlayer p, WorldMon w, String key) {
		PlayerData d = data(p);
		if (!d.beaten.contains("mt_moon_b2f_0")) {
			dialogue(p, null, "talk", List.of("SUPER NERD: Hey, stop! I found these fossils! They're both mine!"), new CompoundTag(), w.getId());
			return;
		}
		String name = key.contains("dome") ? "DOME FOSSIL" : "HELIX FOSSIL";
		CompoundTag t = new CompoundTag();
		ListTag ch = new ListTag();
		for (String c : List.of("YES", "NO")) ch.add(StringTag.valueOf(c));
		t.put("choices", ch);
		t.putString("key", key);
		dialogue(p, null, "fossil", List.of("You want the " + name + "?"), t, w.getId());
	}

	private static void fossilChoice(ServerPlayer p, CompoundTag t, boolean yes) {
		PlayerData d = data(p);
		String key = t.getStringOr("key", "");
		if (!yes || !key.contains("fossil") || d.statics.contains("item:mt_moon_b2f_dome_fossil") || d.statics.contains("item:mt_moon_b2f_helix_fossil")) return;
		String item = key.contains("dome") ? "dome_fossil" : "helix_fossil", other = key.contains("dome") ? "mt_moon_b2f_helix_fossil" : "mt_moon_b2f_dome_fossil";
		d.statics.add("item:" + key);
		d.statics.add("item:" + other);
		d.give(item, 1);
		sync(p);
		for (WorldMon w : ((ServerLevel) p.level()).getEntitiesOfClass(WorldMon.class, p.getBoundingBox().inflate(16),
				w -> w.key().equals("item:" + key) || w.key().equals("item:" + other))) w.discard();
		CompoundTag s = new CompoundTag();
		s.putString("sound", "item");
		Net.send(p, "sound", s);
		message(p, List.of(player(p) + " got the " + (item.startsWith("dome") ? "DOME FOSSIL" : "HELIX FOSSIL") + "!", "SUPER NERD: All right. Then this one is mine!"));
	}

	/** Route 1's clerk, Route 2's AIDE and Route 2's TRADER (see talk). */
	private static void personGift(ServerPlayer p, Npc n, PlayerData d) {
		switch (n.key()) {
			case "route_1_clerk" -> {
				if (d.statics.add("gift:route_1_clerk")) {
					d.give("potion", 1);
					sync(p);
					dialogue(p, n, "talk", fill(p, List.of("Hi! I work at a POKéMON MART.", "It's a convenient shop, so please visit us in VIRIDIAN CITY.",
							"I know, I'll give you a sample. Here you go!", "{player} received a POTION!")));
				} else dialogue(p, n, "talk", List.of("We also carry POKé BALLs for catching POKéMON!", "Please come and see us in VIRIDIAN CITY."));
			}
			case "route_2_gate_p0" -> {
				int kinds = d.caught.size();
				if (d.statics.contains("gift:route_2_gate:hm_flash")) {
					dialogue(p, n, "talk", List.of("The HM FLASH lights even the darkest dungeons."));
					return;
				}
				List<String> lines = new ArrayList<>(List.of("Hi! Remember me? I'm one of PROF. OAK's AIDES.",
						"If you've caught 10 kinds of POKéMON, I'm supposed to give you an HM.", "Let's see... You've caught " + kinds + " kind" + (kinds == 1 ? "" : "s") + "."));
				if (kinds >= 10) {
					lines.add("That's enough! PROF. OAK left this for you.");
					String got = giveGift(p, "route_2_gate");
					if (got != null) lines.add(got);
					lines.add("The HM FLASH lights even the darkest dungeons.");
				} else lines.add("You need 10 kinds for the HM. Come back when you've caught more!");
				dialogue(p, n, "talk", fill(p, lines));
			}
			default -> {
				// The TRADER: as the trade with a creature (the trade screen), his MR. MIME for an ABRA.
				if (d.statics.contains("mr_mime_trade")) {
					dialogue(p, n, "talk", List.of("TRADER: How is my old MR. MIME doing?", "I love the ABRA you gave me!"));
					return;
				}
				CompoundTag t = new CompoundTag();
				t.putString("static", "mr_mime_trade");
				t.putString("want", "abra");
				dialogue(p, n, "trade", List.of("TRADER: I'm looking for an ABRA. Will you trade one for my MR. MIME?"), t, n.getId());
			}
		}
	}

	/** The in-game trades: what each trader wants for theirs (as Red and Blue have them). */
	private static final Map<String, String> TRADE_WANTS = Map.of("mr_mime_trade", "abra", "jynx_trade", "poliwhirl", "farfetch_d_trade", "spearow",
			"lickitung_trade", "slowbro", "nidoran_f_trade", "nidoran_m");
	/** The names the traded POKéMON come with (FireRed's). */
	private static final Map<String, String> TRADE_NICKS = Map.of("nidoran_f_trade", "MS. NIDO", "jynx_trade", "ZYNX", "farfetch_d_trade", "CH'DING");
	/** Traders who stand in a room (by their key): the trade they offer, and what they say. */
	private static final Map<String, String[]> TRADERS = Map.of(
			"underground_path_trader", new String[] {"nidoran_f_trade", "I'm looking for NIDORAN♂! Wanna trade one for NIDORAN♀?", "How is my old NIDORAN♀? My NIDORAN♂ is doing great!"},
			"cerulean_trader", new String[] {"jynx_trade", "I'm looking for POLIWHIRL! Wanna trade one for JYNX?", "Hi! How is ZYNX doing? I'm taking good care of the POLIWHIRL!"},
			"vermilion_trader", new String[] {"farfetch_d_trade", "Hi! Do you have a SPEAROW? Want to trade it for my FARFETCH'D?", "How is my old CH'DING? My SPEAROW is doing great!"});

	private static void trader(ServerPlayer p, Npc n, PlayerData d) {
		String[] tr = TRADERS.get(n.key());
		if (d.statics.contains(tr[0])) {
			dialogue(p, n, "talk", List.of(tr[2]));
			return;
		}
		CompoundTag t = new CompoundTag();
		t.putString("static", tr[0]);
		t.putString("want", TRADE_WANTS.get(tr[0]));
		dialogue(p, n, "trade", List.of(tr[1]), t, n.getId());
	}

	/** The BIKE SHOP (Cerulean, 16:17): the BICYCLE for ¥1,000,000 (the BIKE VOUCHER comes later). */
	private static void bikeShop(ServerPlayer p, Npc n, PlayerData d) {
		if (d.count("bicycle") > 0) {
			dialogue(p, n, "talk", List.of("How do you like your new BICYCLE?", "You can take it on CYCLING ROAD and in caves!"));
			return;
		}
		if (d.count("bike_voucher") > 0) {
			// (17:51) The FAN CLUB CHAIRMAN's BIKE VOUCHER: a BICYCLE for it (FireRed's).
			d.take("bike_voucher");
			d.give("bicycle", 1);
			sync(p);
			dialogue(p, n, "talk", fill(p, List.of("Oh, that's...", "A BIKE VOUCHER!", "OK! Here you go!", "{player} exchanged the BIKE VOUCHER for a BICYCLE.",
					"Thank you! Come back again sometime!")));
			return;
		}
		CompoundTag t = new CompoundTag();
		ListTag ch = new ListTag();
		for (String c : List.of("YES", "NO")) ch.add(StringTag.valueOf(c));
		t.put("choices", ch);
		dialogue(p, n, "bike", List.of("Hi! Welcome to our BIKE SHOP.", "Have we got just the BIKE for you!", "It's a cool BIKE! Do you want it? It's ¥1,000,000."), t, n.getId());
	}

	// ---------- LT.SURGE's GYM (Vermilion, FireRed's): the trash cans and the electric door ----------

	/** Your locks in LT.SURGE's GYM this visit: the can with the first switch, the one with the second, how many found. */
	private static final Map<UUID, int[]> SURGE_LOCKS = new HashMap<>();

	/** Something in a room the game answers (the client says which): LT.SURGE's trash cans. */
	public static void roomUse(ServerPlayer p, String use) {
		Interiors.Room r = Interiors.roomOf(p);
		if (r == null || Battles.in(p) || BUSY.containsKey(p.getUUID())) return;
		if (use.startsWith("can:") && r.id().equals("vermilion_gym")) {
			try {
				trashCan(p, Integer.parseInt(use.substring(4)));
			} catch (NumberFormatException ignored) {
				// Not a can.
			}
		}
	}

	/** Two switches: the first under any can, the second under one right next to it (FireRed's). */
	private static int[] newLocks() {
		int first = RNG.nextInt(15), col = first % 5, row = first / 5;
		List<Integer> next = new ArrayList<>();
		if (col > 0) next.add(first - 1);
		if (col < 4) next.add(first + 1);
		if (row > 0) next.add(first - 5);
		if (row < 2) next.add(first + 5);
		return new int[] {first, next.get(RNG.nextInt(next.size())), 0};
	}

	private static void trashCan(ServerPlayer p, int i) {
		PlayerData d = data(p);
		if (i < 0 || i >= 15) return;
		if (d.statics.contains("vermilion_gym_open")) {
			message(p, List.of("Nope! There's only trash here."));
			return;
		}
		int[] s = SURGE_LOCKS.computeIfAbsent(p.getUUID(), k -> newLocks());
		if (s[2] == 0) {
			if (i == s[0]) {
				s[2] = 1;
				message(p, List.of("Hey! There's a switch under the trash!", "Turn it on!", "The 1st electric lock opened!"));
			} else message(p, List.of("Nope! There's only trash here."));
			return;
		}
		if (i == s[0]) {
			message(p, List.of("The 1st electric lock is open."));
			return;
		}
		if (i == s[1]) {
			d.statics.add("vermilion_gym_open");
			SURGE_LOCKS.remove(p.getUUID());
			sync(p);
			surgeDoor(p);
			message(p, List.of("Hey! There's another switch under the trash!", "Turn it on!", "The 2nd electric lock opened!", "The motorized door opened!"));
			return;
		}
		SURGE_LOCKS.put(p.getUUID(), newLocks());
		message(p, List.of("Nope! There's only trash here.", "Hey! The electric locks were reset!"));
	}

	/**
	 * The electric door in LT.SURGE's GYM: shut (real blocks, drawn as the barrier) till you've found both switches, open
	 * after. Set for you as you come in (a new visit's locks are new, as in FireRed).
	 */
	public static void surgeDoor(ServerPlayer p) {
		if (!(p.level() instanceof ServerLevel level) || level.dimension() != Interiors.LEVEL) return;
		Interiors.Room r = Interiors.ROOMS.get("vermilion_gym");
		if (r == null) return;
		PlayerData d = data(p);
		boolean open = d.statics.contains("vermilion_gym_open") || d.beaten.contains("vermilion_gym_0");
		net.minecraft.world.level.block.state.BlockState b = open ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()
				: net.minecraft.world.level.block.Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();
		for (int x = 6; x <= 7; x++) for (int y = 0; y <= 1; y++) level.setBlock(new BlockPos(r.ox() + x, r.oy() + y, r.oz() + 6), b, 3);
	}

	/** Coming into LT.SURGE's GYM: new locks this visit, and the door as it is for you. */
	public static void surgeEntered(ServerPlayer p) {
		SURGE_LOCKS.remove(p.getUUID());
		surgeDoor(p);
	}

	// ---------- the POKéMON DAY CARE (ROUTE 5, Red and Blue's) ----------

	/** A step while it's at the DAY CARE: a point of experience a step (as in Red and Blue), growing and learning as it goes. */
	private static void daycareStep(Mon m) {
		if (m.level >= 100) return;
		m.exp++;
		while (m.level < 100 && m.exp >= m.species.growth().expAt(m.level + 1)) {
			int hp0 = m.maxHp();
			m.level++;
			m.hp = Math.min(m.maxHp(), m.hp + m.maxHp() - hp0);
			for (String mv : m.species.learnsAt(m.level)) if (Move.find(mv) != null) m.learn(mv);
		}
	}

	private static int daycareFee(PlayerData d) {
		return 100 + 100 * Math.max(0, d.daycare.level - d.daycareLevel);
	}

	private static CompoundTag yesNo() {
		CompoundTag t = new CompoundTag();
		ListTag ch = new ListTag();
		for (String c : List.of("YES", "NO")) ch.add(StringTag.valueOf(c));
		t.put("choices", ch);
		return t;
	}

	private static void daycare(ServerPlayer p, Npc n, PlayerData d) {
		if (d.daycare != null) {
			int grown = d.daycare.level - d.daycareLevel;
			List<String> lines = new ArrayList<>();
			if (grown <= 0) lines.add("Back already? Your " + d.daycare.name() + " needs some more time with me.");
			else lines.add("Your " + d.daycare.name() + " has grown a lot! By level, it's grown by " + grown + "! Aren't I great?");
			lines.add("You owe me ¥" + String.format("%,d", daycareFee(d)) + " for the return of this POKéMON.");
			lines.add("Do you want your POKéMON back?");
			dialogue(p, n, "daycare_back", lines, yesNo(), n.getId());
			return;
		}
		dialogue(p, n, "daycare_ask", List.of("I run a DAY CARE. Would you like me to raise one of your POKéMON?"), yesNo(), n.getId());
	}

	private static void daycareAsk(ServerPlayer p, Npc n, boolean yes) {
		PlayerData d = data(p);
		if (!yes || d.daycare != null) {
			dialogue(p, n, "talk", List.of("All right then, come again."));
			return;
		}
		if (d.party.size() < 2) {
			dialogue(p, n, "talk", List.of("You only have one POKéMON with you.", "All right then, come again."));
			return;
		}
		CompoundTag t = new CompoundTag();
		ListTag ch = new ListTag();
		for (Mon m : d.party) ch.add(StringTag.valueOf(m.name() + "  Lv" + m.level));
		ch.add(StringTag.valueOf("CANCEL"));
		t.put("choices", ch);
		dialogue(p, n, "daycare_pick", List.of("Which POKéMON should I raise?"), t, n.getId());
	}

	private static void daycarePick(ServerPlayer p, Npc n, CompoundTag t) {
		PlayerData d = data(p);
		int i = t.getIntOr("choice", -1);
		if (!t.getBooleanOr("answered", false) || i < 0 || i >= d.party.size() || d.daycare != null) {
			dialogue(p, n, "talk", List.of("All right then, come again."));
			return;
		}
		if (d.party.size() < 2) return;
		boolean healthy = false;
		for (int k = 0; k < d.party.size(); k++) if (k != i && !d.party.get(k).fainted()) healthy = true;
		if (!healthy) {
			dialogue(p, n, "talk", List.of("You'd have no POKéMON able to battle!", "All right then, come again."));
			return;
		}
		Mon m = d.party.remove(i);
		d.daycare = m;
		d.daycareLevel = m.level;
		if (d.follow >= d.party.size()) d.follow = 0;
		sync(p);
		daycarePen(p);
		dialogue(p, n, "talk", List.of("Fine, I'll look after " + m.name() + " for a while.", "Come see me in a while."));
	}

	private static void daycareBack(ServerPlayer p, Npc n, boolean yes) {
		PlayerData d = data(p);
		if (d.daycare == null) return;
		if (!yes) {
			dialogue(p, n, "talk", List.of("All right then, come again."));
			return;
		}
		if (d.party.size() >= 6) {
			dialogue(p, n, "talk", List.of("You have no room for this POKéMON!"));
			return;
		}
		int fee = daycareFee(d);
		if (d.money < fee) {
			dialogue(p, n, "talk", List.of("Hey, you don't have enough money!"));
			return;
		}
		d.money -= fee;
		Mon m = d.daycare;
		d.daycare = null;
		m.heal();
		d.party.add(m);
		sync(p);
		daycarePen(p);
		dialogue(p, n, "talk", fill(p, List.of("Thank you! Here's your POKéMON!", "{player} got " + m.name() + " back!")));
	}

	/** Yours in the DAY CARE's pen while it's there (each time you come in, and when you leave or take it back). */
	public static void daycarePen(ServerPlayer p) {
		if (!(p.level() instanceof ServerLevel level) || level.dimension() != Interiors.LEVEL) return;
		Interiors.Room r = Interiors.ROOMS.get("daycare");
		if (r == null) return;
		String key = "daycare:" + p.getUUID();
		net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(r.ox() - 2, r.oy() - 4, r.oz() - 2, r.ox() + r.w() + 2, r.oy() + 8, r.oz() + r.d() + 2);
		for (WorldMon w : level.getEntitiesOfClass(WorldMon.class, box, w -> w.key().equals(key))) w.discard();
		PlayerData d = data(p);
		if (d.daycare != null) placeMon(level, d.daycare.species.id(), key, r.ox() + 9.9, r.oy(), r.oz() + 6.2, 200F);
	}

	private static void bikeBuy(ServerPlayer p, Npc n, boolean yes) {
		PlayerData d = data(p);
		if (!yes) {
			dialogue(p, n, "talk", List.of("Come back anytime!"));
			return;
		}
		if (d.money < 1000000) {
			dialogue(p, n, "talk", List.of("Sorry! You can't afford it!"));
			return;
		}
		if (d.count("bicycle") > 0) return;
		d.money -= 1000000;
		d.give("bicycle", 1);
		sync(p);
		dialogue(p, n, "talk", fill(p, List.of("Thank you! Here's your BICYCLE!", "{player} got a BICYCLE!", "How do you like your new BICYCLE?")));
	}

	/** A trade agreed on the trade screen: yours (party slot i, the species they want) goes, theirs comes. */
	public static void tradeDone(ServerPlayer p, CompoundTag t) {
		PlayerData d = data(p);
		String key = t.getStringOr("static", "");
		Trainers.Static st = Trainers.STATICS.get(key);
		int i = t.getIntOr("i", -1);
		String want = TRADE_WANTS.get(key);
		if (st == null || want == null || d.statics.contains(key) || i < 0 || i >= d.party.size() || !d.party.get(i).species.id().equals(want)) return;
		Mon got = new Mon(Species.get(st.species()), st.level(), RNG);
		if (TRADE_NICKS.containsKey(key)) got.nickname = TRADE_NICKS.get(key);
		d.party.set(i, got);
		d.seen.add(got.species.id());
		d.caught.add(got.species.id());
		d.statics.add(key);
		Entity e = p.level().getEntity(t.getIntOr("npc", -1));
		if (e instanceof WorldMon w) w.discard();
		sync(p);
	}

	private static void talkMon(ServerPlayer p, WorldMon w) {
		PlayerData d = data(p);
		String key = w.key();
		Species sp = Species.ALL.get(w.species());
		String name = sp == null ? "POKéMON" : sp.name();
		CompoundTag t = new CompoundTag();
		t.putString("cry", w.species());
		if (key.startsWith("item:")) {
			if (key.contains("_fossil")) fossil(p, w, key.substring(5));
			else pickUp(p, w, key.substring(5));
			return;
		}
		if (key.startsWith("follow:")) {
			Mon m = followMon(d);
			String who = m == null ? name : m.name();
			String line = m == null ? who + " is looking at you." : m.hp * 4 < m.maxHp() ? who + " is tired. It needs a rest at a POKéMON CENTER."
					: m.friendship >= 220 ? who + " is overjoyed to be walking with you!" : m.friendship >= 150 ? who + " is happy and bounces along!"
							: m.friendship >= 90 ? who + " is enjoying the walk." : who + " is looking at you, a little unsure.";
			dialogue(p, null, "talk", List.of(line), t, w.getId());
			return;
		}
		if (key.startsWith("wild:")) return;
		if (key.startsWith("daycare:")) {
			String who = d.daycare != null ? d.daycare.name() : name;
			dialogue(p, null, "talk", List.of(who + " is playing happily in the pen."), t, w.getId());
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
		Species want = Species.ALL.get(TRADE_WANTS.getOrDefault(key, ""));
		if (st.kind().equals("trade") && want != null) {
			// An in-game trade (the viewer's "Trading"): what they're after, for theirs.
			t.putString("want", want.id());
			dialogue(p, null, "trade", List.of("TRADER: I'm looking for a " + want.name() + ". Will you trade one for my " + name + "?"), t, w.getId());
		} else if (st.kind().equals("gift") || st.kind().equals("trade")) {
			dialogue(p, null, "gift", List.of(st.note() + ".", "Do you want " + name + "?"), t, w.getId());
		} else if (!d.started || !d.anyHealthy()) {
			dialogue(p, null, "talk", List.of("A wild " + name + "!", "You'd better have a POKéMON that can battle first."), t, w.getId());
		} else {
			// (One posing as an item ball, the Power Plant's ELECTRODE: its own name, not the ball's.)
			Species real = Species.ALL.get(st.species());
			String wild = real == null ? name : real.name();
			if (st.species().equals("snorlax") && d.count("poke_flute") <= 0) {
				// (0.50) Asleep: no battle till it's woken (Mr. Fuji's POKe FLUTE).
				dialogue(p, null, "talk", List.of("A sleeping POKéMON blocks the way!", "It's fast asleep. Maybe something would wake it..."), t, w.getId());
				return;
			}
			dialogue(p, null, "static", st.species().equals("snorlax")
					? List.of(player(p) + " played the POKé FLUTE!", "SNORLAX woke up!", "It attacked in a grumpy rage!") : List.of("The wild " + wild + " attacked!"), t,
					w.getId());
		}
	}

	private static void talkBattle(ServerPlayer p, Npc n, Trainers.Trainer t) {
		CompoundTag extra = new CompoundTag();
		extra.putString("trainer", t.key());
		dialogue(p, n, "battle", fill(p, t.before().isEmpty() ? List.of(t.title() + " wants to battle!") : t.before()), extra);
	}

	static void dialogue(ServerPlayer p, Npc n, String mode, List<String> lines) {
		dialogue(p, n, mode, lines, new CompoundTag());
	}

	private static void dialogue(ServerPlayer p, Npc n, String mode, List<String> lines, CompoundTag t) {
		dialogue(p, n, mode, lines, t, n == null ? -1 : n.getId());
	}

	private static void dialogue(ServerPlayer p, Npc n, String mode, List<String> lines, CompoundTag t, int entityId) {
		// Who's talking, for the tab over the text box (MUM, DAISY...): the name over their head.
		if (n != null && n.hasCustomName() && !t.contains("who")) t.putString("who", n.getCustomName().getString().toUpperCase());
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
	/** The Trainer School's questions (YES or NO), each with what the teacher says after a right or a wrong answer. */
	private record Question(String ask, boolean yes, String right, String wrong) {}

	private static final List<Question> QUESTIONS = List.of(
			new Question("Are FIRE-type moves strong against GRASS-type POKéMON?", true, "That's right! GRASS burns easily.", "Not quite. FIRE is strong against GRASS."),
			new Question("Are WATER-type moves weak against FIRE-type POKéMON?", false, "Right! WATER puts FIRE out: it's strong against it.",
					"Not quite. WATER is strong against FIRE."),
			new Question("Can a POKéMON that's asleep still attack?", false, "Correct! It can't attack until it wakes up.", "Not quite. A sleeping POKéMON can't attack."),
			new Question("Is a POKéMON with low HP easier to catch?", true, "That's right! Weaken it first, then throw your ball.",
					"Not quite. The lower its HP, the easier it is to catch."),
			new Question("Do ELECTRIC-type moves work on GROUND-type POKéMON?", false, "Right! GROUND isn't hurt by ELECTRIC at all.",
					"Not quite. ELECTRIC moves don't affect GROUND-type POKéMON."),
			new Question("Does a POKéMON CENTER heal your POKéMON for free?", true, "Correct! The NURSE will always help.", "Not quite. It's free, every time!"),
			new Question("Is a BURN's damage the only thing it does?", false, "Right! A burnt POKéMON's physical attacks are weaker too.",
					"Not quite. A BURN also weakens its physical attacks."),
			new Question("Can you carry more than six POKéMON with you?", false, "Correct! Six at most. The rest go to the PC.", "Not quite. Six is the most you can carry."));

	private static void schoolQuestion(ServerPlayer p, Npc n) {
		int q = RNG.nextInt(QUESTIONS.size());
		CompoundTag t = new CompoundTag();
		t.putInt("q", q);
		dialogue(p, n, "quiz", List.of("TEACHER: Welcome to the TRAINER SCHOOL! Here's a question for you.", "TEACHER: " + QUESTIONS.get(q).ask()), t);
	}

	public static void dialogueDone(ServerPlayer p, CompoundTag t) {
		BUSY.remove(p.getUUID());
		String mode = t.getStringOr("mode", "talk");
		boolean yes = t.getBooleanOr("yes", false);
		Entity e = p.level().getEntity(t.getIntOr("npc", -1));
		Npc n = e instanceof Npc npc ? npc : null;
		PlayerData d = data(p);
		switch (mode) {
			case "quiz" -> {
				int q = t.getIntOr("q", -1);
				if (q < 0 || q >= QUESTIONS.size() || !t.getBooleanOr("answered", false)) return;
				Question qu = QUESTIONS.get(q);
				dialogue(p, n, "talk", List.of("TEACHER: " + (yes == qu.yes() ? qu.right() : qu.wrong()), "TEACHER: Come back any time for another question!"));
			}
			case "heal" -> {
				if (!yes) {
					dialogue(p, n, "talk", List.of("NURSE: We hope to see you again!"));
					return;
				}
				d.healAll();
				d.hasHeal = true;
				d.healInside = Interiors.in(p);
				d.healDoor = d.healInside ? d.door : "";
				d.healX = p.getX();
				d.healY = p.getY();
				d.healZ = p.getZ();
				sync(p);
				// The viewer's: your balls go into the machine on the first line (the notes play, it lights up), then on.
				CompoundTag h = new CompoundTag();
				h.putInt("healing", 0);
				dialogue(p, n, "talk", List.of("NURSE: OK. I'll take your POKéMON for a few seconds.", "NURSE: Thank you for waiting. Your POKéMON are fighting fit!",
						"NURSE: We hope to see you again!"), h);
			}
			case "mart" -> {
				CompoundTag m = new CompoundTag();
				m.putString("mart", t.getStringOr("mart", d.place));
				Net.send(p, "mart", m);
			}
			case "parcel" -> rivalAfterParcel(p);
			case "museum_fee" -> museumFee(p, n, t.getIntOr("choice", 1) == 0 && t.getBooleanOr("answered", false));
			case "safari_fee" -> safariFee(p, n, t.getIntOr("choice", 1) == 0 && t.getBooleanOr("answered", false));
			case "good_rod" -> goodRod(p, n, t.getIntOr("choice", 1) == 0 && t.getBooleanOr("answered", false));
			case "bike" -> bikeBuy(p, n, t.getIntOr("choice", 1) == 0 && t.getBooleanOr("answered", false));
			case "old_rod" -> oldRod(p, n, t.getIntOr("choice", 1) == 0 && t.getBooleanOr("answered", false));
			case "chairman" -> chairmanStory(p, n, t.getIntOr("choice", 1) == 0 && t.getBooleanOr("answered", false));
			case "rater" -> {
				if (!(t.getIntOr("choice", 1) == 0 && t.getBooleanOr("answered", false)) || d.party.isEmpty()) {
					dialogue(p, n, "talk", List.of("Fine! Come any time you like!"));
					return;
				}
				CompoundTag c = new CompoundTag();
				ListTag ch = new ListTag();
				for (Mon m : d.party) ch.add(StringTag.valueOf(m.name()));
				ch.add(StringTag.valueOf("CANCEL"));
				c.put("choices", ch);
				dialogue(p, n, "rater_pick", List.of("Which POKéMON should I look at?"), c, n.getId());
			}
			case "rater_pick" -> {
				int i = t.getIntOr("choice", -1);
				if (!t.getBooleanOr("answered", false) || i < 0 || i >= d.party.size()) {
					dialogue(p, n, "talk", List.of("Fine! Come any time you like!"));
					return;
				}
				Mon m = d.party.get(i);
				CompoundTag c = yesNo();
				c.putInt("slot", i);
				dialogue(p, n, "rater_yes", List.of(m.name() + ", is it? That is a decent nickname!", "But, would you like me to give it a nicer name?", "How about it?"), c, n.getId());
			}
			case "rater_yes" -> {
				int i = t.getIntOr("slot", -1);
				if (!(t.getIntOr("choice", 1) == 0 && t.getBooleanOr("answered", false)) || i < 0 || i >= d.party.size()) {
					dialogue(p, n, "talk", List.of("Fine! Come any time you like!"));
					return;
				}
				askNickname(p, "party", i);
			}
			case "daycare_ask" -> daycareAsk(p, n, t.getIntOr("choice", 1) == 0 && t.getBooleanOr("answered", false));
			case "daycare_pick" -> daycarePick(p, n, t);
			case "daycare_back" -> daycareBack(p, n, t.getIntOr("choice", 1) == 0 && t.getBooleanOr("answered", false));
			case "deleter" -> deleterAsk(p, n, t.getIntOr("choice", 1) == 0 && t.getBooleanOr("answered", false));
			case "deleter_mon" -> deleterMon(p, n, t);
			case "deleter_move" -> deleterMove(p, n, t);
			case "guide" -> {
				// "Follow me!": he walks you to the GYM (14:18).
				if (n != null && !ESCORTS.containsKey(p.getUUID())) {
					ESCORTS.put(p.getUUID(), new Escort(n, p, PEWTER_TO_GYM));
					BUSY.put(p.getUUID(), 40);
				}
			}
			case "aide_shoes" -> {
				if (n != null && n.key().equals("pewter_city_aide")) n.discard();
			}
			case "magikarp" -> magikarpSale(p, n, t.getIntOr("choice", 1) == 0 && t.getBooleanOr("answered", false));
			case "tutor" -> tutorChoice(p, n, t);
			case "fossil" -> fossilChoice(p, t, t.getIntOr("choice", 1) == 0 && t.getBooleanOr("answered", false));
			case "gate_choice" -> {
				int c = t.getIntOr("choice", -1);
				if (c == 0) Interiors.outThrough(p, "route28");
				else if (c == 1) {
					if (meets(p, "CHAMPION")) Interiors.outThrough(p, "route26");
					else dialogue(p, null, "talk", List.of("GUARD: ROUTE 26 leads to the JOHTO region, and the POKéMON LEAGUE's back way.",
							"GUARD: Only the CHAMPION may go that way. Come back once you've beaten the ELITE FOUR!"), whoTag("GUARD"));
				}
			}
			case "mum_map" -> mumHeadsHome(p);
			case "rival" -> {
				if (!d.beaten.contains("rival_lab") && !d.party.isEmpty()) Battles.trainer(p, rivalLab(p));
			}
			case "battle" -> {
				Trainers.Trainer tr = trainerFor(p, t.getStringOr("trainer", ""));
				if (tr != null && !d.beaten.contains(tr.key()) && d.anyHealthy() && !(battlesOff(p) && !storyBattle(tr))) Battles.trainer(p, tr);
			}
			case "static" -> {
				Trainers.Static st = Trainers.STATICS.get(t.getStringOr("static", ""));
				if (st == null || d.statics.contains(st.id()) || !d.anyHealthy() || battlesOff(p)) return;
				Battles.wild(p, new Mon(Species.get(st.species()), st.level(), RNG), st.id());
			}
			case "trade" -> {
				Trainers.Static st = Trainers.STATICS.get(t.getStringOr("static", ""));
				Species want = Species.ALL.get(t.getStringOr("want", ""));
				if (st == null || want == null || d.statics.contains(st.id())) return;
				int npcId = t.getIntOr("npc", -1);
				if (!yes) {
					dialogue(p, null, "talk", List.of("TRADER: Aww. If you change your mind, I'll be here."), new CompoundTag(), npcId);
					return;
				}
				int idx = -1;
				for (int i = 0; i < d.party.size(); i++) if (d.party.get(i).species.id().equals(want.id()) && idx < 0) idx = i;
				if (idx < 0) {
					dialogue(p, null, "talk", List.of("TRADER: What? That's not a " + want.name() + ". If you get one, come back here!"), new CompoundTag(), npcId);
					return;
				}
				Mon mine = d.party.get(idx);
				CompoundTag o = new CompoundTag();
				o.putString("static", st.id());
				o.putInt("npc", npcId);
				o.putInt("i", idx);
				o.putString("send", want.id());
				o.putString("sendName", mine.name());
				o.putInt("sendLv", mine.level);
				o.putString("get", st.species());
				o.putInt("getLv", st.level());
				o.putString("player", player(p));
				Net.send(p, "trade", o);
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

	/**
	 * FAST TRAVEL (on in OPTIONS): straight to a point on the TOWN MAP (a place's pin, or anywhere you pick on it),
	 * onto good ground there, out of a room if you're in one.
	 */
	public static void travel(ServerPlayer p, CompoundTag t) {
		if (!FAST_TRAVEL.contains(p.getUUID()) || Battles.in(p) || !data(p).started) return;
		ServerLevel world = p.level().getServer().overworld();
		// A town (or a route with one): in front of its POKéMON CENTER, as FLY lands you.
		String pin = t.getStringOr("place", "");
		// (17:51) ROUTE 4's own pin: Route 4 proper, past MT. MOON (its POKéMON CENTER is at the ROUTE 3 end).
		if ("route_4".equals(pin)) {
			t.putInt("x", -520);
			t.putInt("z", 262);
			pin = "";
		}
		for (Interiors.Door d : Interiors.DOORS.values()) {
			if (!"center".equals(d.group()) || !pin.equals(d.place())) continue;
			world.getChunk(BlockPos.containing(d.x(), d.y(), d.z()));
			if (p.level() != world) Interiors.jumpOut(p, world, d.x(), d.y(), d.z());
			else p.teleportTo(d.x(), d.y(), d.z());
			p.setYRot(d.yaw());
			p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
			enter(p, placeNear(d.x(), d.z()));
			return;
		}
		BlockPos at = spot(world, t.getIntOr("x", 0), t.getIntOr("z", 0), 24);
		if (at == null) {
			message(p, List.of("There's nowhere to land there."));
			return;
		}
		if (p.level() != world) Interiors.jumpOut(p, world, at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
		else {
			p.teleportTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
			p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
		}
		enter(p, placeNear(at.getX() + 0.5, at.getZ() + 0.5));
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
				message(p, List.of(m.name() + " grew to Lv. " + m.level + "!"));
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
		// No POKé BALLs yet: Oak gives you some with the POKéDEX (after his parcel).
		d.give("potion", 3);
		d.money = 3000;
		sync(p);
		// (Your rival battles you when you bring Oak's Parcel back, as Let's Go has it, not now.)
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
		// Soft: one ring of faint light round each lamp (brighter rings showed as squares of light on the ground).
		BlockState soft = Blocks.LIGHT.defaultBlockState().setValue(net.minecraft.world.level.block.LightBlock.LEVEL, 7);
		java.util.Set<Long> done = new java.util.HashSet<>();
		for (int x = z.x1(); x <= z.x2(); x++) {
			for (int zz = z.z1(); zz <= z.z2(); zz++) {
				for (int y = street - 2; y <= street + 9; y++) {
					BlockPos at = new BlockPos(x, y, zz);
					BlockState b = level.getBlockState(at);
					if (b.getLightEmission() < 12 || b.is(Blocks.LIGHT) || !level.getFluidState(at).isEmpty() || b.is(Blocks.FIRE) || b.is(Blocks.LAVA)) continue;
					for (int i = 0; i < 6; i++) {
						double a = Math.PI * 2 * i / 6;
						int lx = x + (int) Math.round(Math.cos(a) * 5), lz = zz + (int) Math.round(Math.sin(a) * 5);
						if (!done.add(BlockPos.asLong(lx, 0, lz))) continue;
						int top = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, lx, lz);
						BlockPos to = new BlockPos(lx, top, lz);
						if (Math.abs(top - street) > 4 || !level.getBlockState(to).isAir()) continue;
						level.setBlock(to, soft, 2);
						placed++;
					}
				}
			}
		}
		return placed;
	}

	/**
	 * The games' signs in a place, where the map hasn't got one: two shelves stacked (the look of the signs on the
	 * map), or a sign if shelves aren't in this version, at the sign's spot on the games' map (on good ground near
	 * it), facing the camera. Not where there's already a sign or a shelf within six blocks. Read with SPACE.
	 */
	private static int plantSigns(ServerLevel level, Zone z) {
		String pl = z.place();
		if (!pl.endsWith("_city") && !pl.endsWith("_town") && !pl.startsWith("route_") && !pl.endsWith("_island")) return 0;
		int[] size = Places.size(pl);
		if (size == null) return 0;
		Block shelf = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getOptional(net.minecraft.resources.Identifier.withDefaultNamespace("oak_shelf"))
				.orElse(null);
		int street = streetY(level, z), planted = 0;
		for (Trainers.Sign sg : Trainers.signsIn(pl)) {
			double fx = (sg.x() + 0.5) / size[0], fz = (sg.y() + 0.5) / size[1];
			int wx = z.x1() + (int) Math.floor(fx * (z.x2() - z.x1() + 1)), wz = z.z1() + (int) Math.floor(fz * (z.z2() - z.z1() + 1));
			if (!z.contains(wx + 0.5, wz + 0.5) || !level.hasChunkAt(new BlockPos(wx, street, wz)) || signNear(level, wx, street, wz, 6)) continue;
			BlockPos at = spotNear(level, wx, wz, 4, street);
			if (at == null) continue;
			if (shelf != null) {
				BlockState st = shelf.defaultBlockState().trySetValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING,
						net.minecraft.core.Direction.SOUTH);
				if (!level.getBlockState(at.above()).isAir()) continue;
				level.setBlock(at, st, 3);
				level.setBlock(at.above(), st, 3);
			} else level.setBlock(at, Blocks.OAK_SIGN.defaultBlockState(), 3);
			planted++;
		}
		return planted;
	}

	/**
	 * Takes away the shelf signs plantSigns put down: for each of the games' signs, the spots are tried in the same
	 * order it tried them; the first is where it put its two shelves (facing south, air above), if it put any. Only
	 * where those two are the only shelves near the sign (so your own are never touched). What's taken away is
	 * listed in the world's folder (retromon_removed_signs.txt).
	 */
	private static int unplantSigns(MinecraftServer server, ServerLevel level, Zone z) {
		String pl = z.place();
		int[] size = Places.size(pl);
		Block shelf = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getOptional(net.minecraft.resources.Identifier.withDefaultNamespace("oak_shelf"))
				.orElse(null);
		if (size == null || shelf == null) return 0;
		int street = streetY(level, z), removed = 0;
		StringBuilder log = new StringBuilder();
		for (Trainers.Sign sg : Trainers.signsIn(pl)) {
			double fx = (sg.x() + 0.5) / size[0], fz = (sg.y() + 0.5) / size[1];
			int wx = z.x1() + (int) Math.floor(fx * (z.x2() - z.x1() + 1)), wz = z.z1() + (int) Math.floor(fz * (z.z2() - z.z1() + 1));
			if (!z.contains(wx + 0.5, wz + 0.5) || !level.hasChunkAt(new BlockPos(wx, street, wz))) continue;
			BlockPos found = null;
			search:
			for (int[] heights : new int[][] {{0, 1, -1}, {2, -2}}) {
				for (int r = 0; r <= 4; r++) {
					for (int dx = -r; dx <= r; dx++) {
						for (int dz = -r; dz <= r; dz++) {
							if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
							for (int dy : heights) {
								BlockPos at = new BlockPos(wx + dx, street + dy, wz + dz);
								if (ourShelves(level, at, shelf)) {
									found = at;
									break search;
								}
								if (goodGround(level, at)) break search;
							}
						}
					}
				}
			}
			if (found == null || shelvesNear(level, wx, street, wz, 6) != 2) continue;
			level.setBlock(found.above(), Blocks.AIR.defaultBlockState(), 3);
			level.setBlock(found, Blocks.AIR.defaultBlockState(), 3);
			log.append(pl).append(' ').append(found.getX()).append(' ').append(found.getY()).append(' ').append(found.getZ()).append('\n');
			removed++;
		}
		if (removed > 0) {
			try {
				Path f = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("retromon_removed_signs.txt");
				Files.writeString(f, log.toString(), StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
			} catch (IOException e) {
				Retromon.LOG.warn("Couldn't note the removed signs", e);
			}
			Retromon.LOG.info("Retromon: took away {} shelf signs in {}", removed, pl);
		}
		return removed;
	}

	/** Two shelves stacked, both facing south, with air above: the way plantSigns put them. */
	private static boolean ourShelves(ServerLevel level, BlockPos at, Block shelf) {
		BlockState a = level.getBlockState(at), b = level.getBlockState(at.above());
		return a.is(shelf) && b.is(shelf) && facesSouth(a) && facesSouth(b) && level.getBlockState(at.above(2)).isAir();
	}

	private static boolean facesSouth(BlockState st) {
		return st.getOptionalValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING).orElse(null) == net.minecraft.core.Direction.SOUTH;
	}

	/** How many shelves there are within r of a spot. */
	private static int shelvesNear(ServerLevel level, int x, int y, int z, int r) {
		int n = 0;
		for (int dx = -r; dx <= r; dx++)
			for (int dz = -r; dz <= r; dz++)
				for (int dy = -3; dy <= 4; dy++) {
					BlockState b = level.getBlockState(new BlockPos(x + dx, y + dy, z + dz));
					if (net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(b.getBlock()).getPath().endsWith("shelf")) n++;
				}
		return n;
	}

	/** Takes away the faint light rings lightUp put round lamps (level 7, at the ground). */
	private static void unlightRings(ServerLevel level, Zone z) {
		int street = streetY(level, z);
		for (int x = z.x1(); x <= z.x2(); x++) {
			for (int zz = z.z1(); zz <= z.z2(); zz++) {
				int top = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, zz);
				for (int y = Math.max(street - 4, top - 1); y <= Math.min(street + 5, top + 1); y++) {
					BlockPos at = new BlockPos(x, y, zz);
					BlockState b = level.getBlockState(at);
					if (b.is(Blocks.LIGHT) && b.getValue(net.minecraft.world.level.block.LightBlock.LEVEL) == 7) level.setBlock(at, Blocks.AIR.defaultBlockState(), 2);
				}
			}
		}
	}

	private static boolean signNear(ServerLevel level, int x, int y, int z, int r) {
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				for (int dy = -3; dy <= 4; dy++) {
					BlockState b = level.getBlockState(new BlockPos(x + dx, y + dy, z + dz));
					if (b.getBlock() instanceof net.minecraft.world.level.block.SignBlock
							|| net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(b.getBlock()).getPath().endsWith("shelf")) return true;
				}
			}
		}
		return false;
	}

	/** Takes away the brighter light rings 0.7 and 0.8 put round lamps (levels 12 and 9, just above the ground). */
	private static void unlight(ServerLevel level, Zone z) {
		int street = streetY(level, z);
		for (int x = z.x1(); x <= z.x2(); x++) {
			for (int zz = z.z1(); zz <= z.z2(); zz++) {
				int top = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, zz);
				for (int y = Math.max(street - 4, top - 1); y <= Math.min(street + 5, top + 1); y++) {
					BlockPos at = new BlockPos(x, y, zz);
					BlockState b = level.getBlockState(at);
					if (!b.is(Blocks.LIGHT)) continue;
					int lv = b.getValue(net.minecraft.world.level.block.LightBlock.LEVEL);
					if (lv == 12 || lv == 9) level.setBlock(at, Blocks.AIR.defaultBlockState(), 2);
				}
			}
		}
	}

	/**
	 * The first people on the Kanto map: a practice trainer on Route 1. (Viridian's nurse and clerk are inside the
	 * Pokémon Center and the Mart now; the ones earlier versions stood outside, on the roofs, are taken away.)
	 */
	private static final Object[][] DEFAULTS = {{"joey", Npc.TRAINER, "youngster_joey", -790, 600, 0F}};
	private static final int[][] OLD_OUTSIDE = {{-785, 561}, {-766, 549}};
	/**
	 * People who stand somewhere on John's map other than where the games' grid puts them, each moved there once per
	 * save (SPOTS_DONE says which time: bumped, everyone's moved again). Pallet's, where FireRed and LeafGreen have
	 * them: the girl ("I'm raising POKéMON too!") on the west lawn, left of the flower garden; the man who says
	 * technology is incredible below the lab's fence, by its left end; PROF.OAK's AIDE (not in FireRed) by the lab's
	 * sign, as John asked. Key, then x and z.
	 */
	private static final Object[][] SPOTS = {{"pallet_town_p1", -809, 686}, {"pallet_town_p2", -785, 704}, {"pallet_town_p3", -780, 701},
			// Viridian City's people where FireRed has them (12:44; its map laid over John's at two blocks a step, from its
			// doors): the old man (lying in the road, then his catching lesson and the TEACHY TV) by the way north, his
			// granddaughter below him, the old man by the GYM, the boy with the caterpillars down the west side, the
			// youngster by the POKéMON CENTER, the man who knows DREAM EATER by the pond.
			{"viridian_city_p1", -796, 517}, {"viridian_city_woman", -798, 529}, {"viridian_city_gym_man", -770, 527}, {"viridian_city_boy", -806, 549},
			{"viridian_city_p0", -772, 557}, {"viridian_city_tutor", -822, 557},
			// Route 1's: the POKéMON MART's clerk with a free POTION (low down, by the flowers above the sign), and the boy
			// who tells you about the ledges (up in the grass on the right).
			{"route_1_clerk", -802, 641}, {"route_1_p0", -776, 617},
			// Pewter City's (13:18), FireRed's map over John's at two blocks a step (x = 2c - 836, z = 2r + 265, from its
			// doors): the girl who says CLEFAIRY came from the moon, the man by the museum, the man below the Center, the
			// boy in the rose garden, the boy at the way east who takes you to the GYM.
			{"pewter_city_p0", -824, 295}, {"pewter_city_p2", -770, 299}, {"pewter_city_p1", -794, 321}, {"pewter_city_p3", -776, 320}, {"cerulean_city_p7", -396, 251}, {"vermilion_city_p0", -356, 566}, {"vermilion_city_p1", -366, 562}, {"vermilion_city_p3", -328, 564}, {"vermilion_city_p4", -330, 566}, {"vermilion_city_p5", -338, 603}, {"vermilion_city_p2", -352, 607}, {"lavender_town_p0", -116, 420}, {"lavender_town_p1", -131, 429}, {"lavender_town_p2", -136, 419},
			{"pewter_city_p4", -752, 306},
			// Routes 3 and 4 (13:56), where FireRed has them (x = 2c - 748, z = 2r + 285 on Route 3; x = 2c - 628, z = 2r + 246
			// on Route 4): the youngster resting by the way up to MT. MOON; the woman by the POKéMON CENTER, the boy below it,
			// the two BLACK BELTs who teach MEGA PUNCH and MEGA KICK.
			{"route_3_p0", -608, 311}, {"route_4_p0", -610, 263}, {"route_4_boy", -598, 275}, {"route_4_tutor_punch", -534, 252}, {"route_4_tutor_kick", -528, 252}};
	/** Trainers who stand somewhere on John's map other than where the games' grid puts them (Routes 3 and 4, FireRed's). */
	private static final Object[][] TRAINER_SPOTS = {{"route_10_0", -125, 334},{"route_3_0", -722, 299}, {"route_3_1", -714, 293}, {"route_3_2", -710, 303}, {"route_3_3", -698, 293},
			{"route_3_4", -688, 291}, {"route_3_5", -690, 305}, {"route_3_6", -684, 297}, {"route_3_7", -668, 307}, {"route_4_0", -478, 252}};
	private static final String SPOTS_DONE = "spot7:";
	/**
	 * Item balls that lie somewhere on John's map other than where the games' grid puts them (FireRed's, on routes Red
	 * and Blue had nothing there), each put down once per save. Route 2's PARLYZ HEAL: in the east lane, past the
	 * northern CUT tree, between its flowers (FireRed's spot, a step right of the top flower), clear of them. Key (its
	 * row in field_items.tsv), then x, y and z.
	 */
	/**
	 * (0.50) The POKeMON that wait in one spot out in the world: Route 12's SNORLAX asleep on the pier south of Route
	 * 11's gate, Route 16's outside its gate's west way. Key, x, y, z, facing.
	 */
	private static final Object[][] STATIC_SPOTS = {{"snorlax_route_12", -123.0, 31.0, 599.5, 90.0}, {"snorlax_route_16", -623.5, 34.0, 418.5, 0.0}};

	/** How near a sleeping SNORLAX you can come (blocks from its middle). */
	private static final double SNORLAX_REACH = 2.7;
	private static final Map<UUID, Integer> BLOCKED_SAID = new HashMap<>();

	/**
	 * (0.50) What won't let you by: a sleeping SNORLAX (till you've woken it with the POKe FLUTE and battled it), and the
	 * S.S. ANNE's sailor at the foot of its gangway (Vermilion's harbour: the ship isn't boarding yet). You're put back
	 * where you can stand, and told why once in a while.
	 */
	private static void blockers(ServerPlayer p) {
		PlayerData d = data(p);
		BLOCKED_SAID.computeIfPresent(p.getUUID(), (k, v) -> v > 1 ? v - 1 : null);
		for (Object[] o : STATIC_SPOTS) {
			String key = (String) o[0];
			if (d.statics.contains(key)) continue;
			double x = (Double) o[1], y = (Double) o[2], z = (Double) o[3];
			double dx = p.getX() - x, dz = p.getZ() - z, dist = Math.sqrt(dx * dx + dz * dz);
			if (dist >= SNORLAX_REACH || Math.abs(p.getY() - y) > 3) continue;
			if (dist < 1e-3) {
				dx = 0;
				dz = -1;
				dist = 1;
			}
			p.teleportTo(x + dx / dist * SNORLAX_REACH, p.getY(), z + dz / dist * SNORLAX_REACH);
			if (!BLOCKED_SAID.containsKey(p.getUUID())) {
				BLOCKED_SAID.put(p.getUUID(), 100);
				message(p, List.of("A sleeping POKéMON blocks the way!"));
			}
		}
		// The S.S. ANNE: the sailor at the foot of the gangway (z 612 on): no further.
		if (p.getX() > -366 && p.getX() < -340 && p.getZ() > 612.2 && p.getZ() < 620 && p.getY() > 28 && p.getY() < 36) {
			p.teleportTo(p.getX(), p.getY(), 611.6);
			if (!BLOCKED_SAID.containsKey(p.getUUID())) {
				BLOCKED_SAID.put(p.getUUID(), 100);
				message(p, d.count("s_s_ticket") > 0 ? List.of("SAILOR: Welcome aboard! ...Oh, sorry. The S.S.ANNE isn't boarding yet.", "SAILOR: Come back a little later!")
						: List.of("SAILOR: Welcome to the S.S.ANNE!", "SAILOR: May I see your S.S.TICKET?", "SAILOR: ...Sorry! You need a ticket to come aboard."));
			}
		}
	}

	private static final Object[][] ITEM_SPOTS = {{"route_2_parlyz_heal", -779.0, 26.0, 472.0},
			// Viridian City's POTION: in the fenced corner at the top left, where FireRed has it behind its CUT tree; on the
			// path between the white fences, where you can get to it (14:15).
			{"viridian_city_potion", -804.5, 25.0, 514.5},
			// Route 2's ETHER (14:18): on the east lane below its gate, as FireRed has it (not on the posts and wall by the gate).
			{"route_2_i0", -779.5, 27.0, 452.5},
			// Routes 3 and 4 (13:56): FireRed's ORAN BERRY; its GREAT BALL by the tutors, PERSIM BERRY by the CENTER, RAZZ
			// BERRY and TM05 (ROAR) further east.
			{"route_3_oran_berry", -695.5, 30.0, 303.5}, {"route_4_great_ball", -536.5, 34.0, 252.5}, {"route_4_persim_berry", -617.5, 35.0, 254.5},
			{"route_4_razz_berry", -493.5, 32.0, 280.5}, {"route_4_i0", -493.5, 34.0, 256.5}};
	/** Item balls the games' grid put down that aren't there in FireRed (taken away again where they lie). */
	private static final String[] GONE_ITEMS = {"route_2_i1"};
	/**
	 * Signs on John's map whose words aren't the place's own (the games' grid would read out another of its signs):
	 * the west block of each (x, z), and its words. Diglett's Cave's, by its mouth on Route 2 and by its mouth on Route 11.
	 */
	private static final Object[][] SIGN_SPOTS = {{-788, 369, List.of("DIGLETT's CAVE")}, {-297, 581, List.of("DIGLETT's CAVE")},
			// Pewter City's: its own sign on the rose garden's fence, and the TRAINER TIPS by the Center.
			{-777, 317, List.of("PEWTER CITY", "A Stone Gray City")},
			{-798, 326, List.of("TRAINER TIPS", "Any POKéMON that takes part in battle, however short, earns EXP!")},
			// Route 3's and Route 4's (13:56).
			{-604, 308, List.of("ROUTE 3", "MT. MOON AHEAD")}, {-593, 260, List.of("MT. MOON", "Tunnel Entrance")},
			{-550, 260, List.of("ROUTE 4", "MT. MOON - CERULEAN CITY")}};

	/** Where a trainer in TRAINER_SPOTS stands (x, z), or null. */
	private static int[] trainerSpot(String key) {
		for (Object[] o : TRAINER_SPOTS) if (o[0].equals(key)) return new int[] {(Integer) o[1], (Integer) o[2]};
		return null;
	}

	/** Whether an item ball lies at a spot of its own (ITEM_SPOTS). */
	private static boolean itemSpot(String key) {
		for (Object[] o : ITEM_SPOTS) if (o[0].equals(key)) return true;
		return false;
	}

	/** Where someone in SPOTS stands (x, z), or null. */
	private static int[] spotOf(String key) {
		for (Object[] o : SPOTS) if (o[0].equals(key)) return new int[] {(Integer) o[1], (Integer) o[2]};
		return null;
	}

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
			boolean loaded = level.hasChunkAt(new BlockPos(z.x1(), START_Y, z.z1())) && level.hasChunkAt(new BlockPos(z.x2(), START_Y, z.z2()));
			if (!loaded) continue;
			if (!done.contains("place:" + z.place())) {
				populate(level, z.place(), false);
				placed.add("place:" + z.place());
				done.add("place:" + z.place());
				changed = true;
			}
			// Nothing of ours on the map: the lamp light rings and the shelf signs earlier versions put down are
			// taken away again (once per place), and none are put down any more.
			if (!done.contains("unlight7:" + z.place() + z.x1() + "," + z.z1())) {
				unlight(level, z);
				unlightRings(level, z);
				placed.add("unlight7:" + z.place() + z.x1() + "," + z.z1());
				done.add("unlight7:" + z.place() + z.x1() + "," + z.z1());
				changed = true;
			}
			if (done.contains("signs:" + z.place() + z.x1() + "," + z.z1()) && !done.contains("unsigns:" + z.place() + z.x1() + "," + z.z1())) {
				unplantSigns(s, level, z);
				placed.add("unsigns:" + z.place() + z.x1() + "," + z.z1());
				done.add("unsigns:" + z.place() + z.x1() + "," + z.z1());
				changed = true;
			}
		}
		// The nurse and the clerk who stood outside (on the roofs) before there were insides: gone, and kept gone
		// (looked for every couple of seconds while that part of Viridian is loaded; they're inside now).
		{
			boolean all = true;
			for (int[] o : OLD_OUTSIDE) {
				BlockPos at = new BlockPos(o[0], START_Y, o[1]);
				if (!level.hasChunkAt(at)) {
					all = false;
					continue;
				}
				net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(o[0] - 14, level.getMinY(), o[1] - 14, o[0] + 14, level.getMaxY(), o[1] + 14);
				for (Npc n : level.getEntitiesOfClass(Npc.class, box, n -> n.role() == Npc.NURSE || n.role() == Npc.CLERK)) n.discard();
			}
			if (all && !done.contains("indoors:viridian")) {
				placed.add("indoors:viridian");
				done.add("indoors:viridian");
				changed = true;
			}
		}
		// The lab's aides belong in the lab: one the town had put outside too (by the pond) goes. (PROF.OAK's AIDE out
		// in town is his own person, by the town's sign: pallet_town_p3.)
		if (!done.contains("cleanup:lab_aides") && done.contains("place:pallet_town")) {
			Zone pz = null;
			for (Zone z : zones) if (z.place().equals("pallet_town")) pz = z;
			if (pz != null && level.hasChunkAt(new BlockPos(pz.x1(), START_Y, pz.z1())) && level.hasChunkAt(new BlockPos(pz.x2(), START_Y, pz.z2()))) {
				net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(pz.x1() - 8, level.getMinY(), pz.z1() - 8, pz.x2() + 8, level.getMaxY(), pz.z2() + 8);
				for (Npc n : level.getEntitiesOfClass(Npc.class, box, n -> n.key().startsWith("oaks_lab_") || n.key().startsWith("in_lab_"))) n.discard();
				placed.add("cleanup:lab_aides");
				done.add("cleanup:lab_aides");
				changed = true;
			}
		}
		// Those in SPOTS: moved there (or put there, if they're missing), once per save.
		for (Object[] o : SPOTS) {
			String key = (String) o[0], doneKey = SPOTS_DONE + key;
			int x = (Integer) o[1], z = (Integer) o[2];
			Trainers.Person who = Trainers.PEOPLE.get(key);
			if (done.contains(doneKey) || who == null || !done.contains("place:" + who.place())) continue;
			if (!level.hasChunkAt(new BlockPos(x, START_Y, z))) continue;
			BlockPos at = spot(level, x, z, 6);
			net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(x - 120, level.getMinY(), z - 120, x + 120, level.getMaxY(), z + 120);
			List<Npc> found = level.getEntitiesOfClass(Npc.class, box, n -> key.equals(n.key()));
			if (found.isEmpty()) place(level, Npc.PERSON, key, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, yawOf(who.facing(), 0F));
			else for (Npc n : found) n.rehome(at);
			placed.add(doneKey);
			done.add(doneKey);
			changed = true;
		}
		// Those in ITEM_SPOTS: put down once per save, where their part of the map is loaded (unless picked up already).
		for (Object[] o : STATIC_SPOTS) {
			String key = (String) o[0], doneKey = "staticspot2:" + key;
			double x = (Double) o[1], y = (Double) o[2], z = (Double) o[3];
			Trainers.Static st = Trainers.STATICS.get(key);
			if (done.contains(doneKey) || st == null || takenByAnyone(key)) continue;
			if (!level.hasChunkAt(BlockPos.containing(x, y, z))) continue;
			net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(x - 4, y - 3, z - 4, x + 4, y + 3, z + 4);
			List<WorldMon> there = level.getEntitiesOfClass(WorldMon.class, box, w -> key.equals(w.key()));
			// (0.50.3) Lying down now: one put down before turns to lie across the way.
			if (there.isEmpty()) placeMon(level, st.species(), key, x, y, z, ((Double) o[4]).floatValue());
			else for (WorldMon w : there) {
				w.teleportTo(x, y, z);
				w.setFacing(((Double) o[4]).floatValue());
			}
			placed.add(doneKey);
			done.add(doneKey);
			changed = true;
		}
		for (Object[] o : ITEM_SPOTS) {
			String key = (String) o[0], doneKey = "itemspot2:" + key;
			double x = (Double) o[1], y = (Double) o[2], z = (Double) o[3];
			if (done.contains(doneKey) || !Trainers.FIELD_ITEMS.containsKey(key)) continue;
			if (!level.hasChunkAt(BlockPos.containing(x, y, z))) continue;
			// (A copy the grid put down somewhere else, as on Route 2 by its gate: taken away; 14:18.)
			net.minecraft.world.phys.AABB wide = new net.minecraft.world.phys.AABB(x - 120, level.getMinY(), z - 120, x + 120, level.getMaxY(), z + 120);
			for (WorldMon w : level.getEntitiesOfClass(WorldMon.class, wide, w -> ("item:" + key).equals(w.key()) && w.distanceToSqr(x, y, z) > 9)) w.discard();
			net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(x - 3, y - 3, z - 3, x + 3, y + 3, z + 3);
			boolean there = !level.getEntitiesOfClass(WorldMon.class, box, w -> ("item:" + key).equals(w.key())).isEmpty();
			if (!there && !takenByAnyone("item:" + key)) placeMon(level, "poke_ball", "item:" + key, x, y, z, 0F);
			placed.add(doneKey);
			done.add(doneKey);
			changed = true;
		}
		for (String key : GONE_ITEMS) {
			String doneKey = "itemgone1:" + key;
			Trainers.FieldItem fi = Trainers.FIELD_ITEMS.get(key);
			Zone gz = null;
			if (fi != null) for (Zone zz : zones) if (zz.place().equals(fi.place())) gz = zz;
			if (done.contains(doneKey) || gz == null) continue;
			if (!level.hasChunkAt(new BlockPos(gz.x1(), START_Y, gz.z1())) || !level.hasChunkAt(new BlockPos(gz.x2(), START_Y, gz.z2()))) continue;
			net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(gz.x1() - 4, level.getMinY(), gz.z1() - 4, gz.x2() + 4, level.getMaxY(), gz.z2() + 4);
			for (WorldMon w : level.getEntitiesOfClass(WorldMon.class, box, w -> ("item:" + key).equals(w.key()))) w.discard();
			placed.add(doneKey);
			done.add(doneKey);
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
	/** Places marked only round one spot so far (16:58: Route 10 round the Power Plant): only people with their own spots. */
	private static final java.util.Set<String> OWN_SPOTS_ONLY = java.util.Set.of("route_10", "vermilion_city", "lavender_town");

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
		// (A place only partly marked so far, Route 10 round the Power Plant: only those with their own spots.)
		boolean ownOnly = OWN_SPOTS_ONLY.contains(place);
		for (Trainers.Trainer t : Trainers.in(place)) {
			int[] own = trainerSpot(t.key());
			if (ownOnly && own == null) continue;
			BlockPos at = own != null ? (level.hasChunkAt(new BlockPos(own[0], START_Y, own[1])) ? spot(level, own[0], own[1], 4) : null)
					: gameSpot(level, z, size, Trainers.POS.get(t.key()));
			if (at == null || present.contains(t.key())) continue;
			place(level, t.leader() ? Npc.LEADER : Npc.TRAINER, t.key(), at.getX() + 0.5, at.getY(), at.getZ() + 0.5, yawOf(t.facing(), 0F));
			count++;
		}
		for (Trainers.Person ps : Trainers.peopleIn(place)) {
			// Oak at the edge of Pallet Town is the one who stops you (he comes to you); he doesn't stand there.
			if (ps.key().equals("pallet_town_p0")) continue;
			// (Cerulean's rival comes himself, at the bridge; its second OFFICER JENNY is the first one again.)
			if (ps.key().equals("cerulean_city_p0") || ps.key().equals("cerulean_city_p8")) continue;
			int[] own = spotOf(ps.key());
			if (ownOnly && own == null) continue;
			BlockPos at = own != null ? (level.hasChunkAt(new BlockPos(own[0], START_Y, own[1])) ? spot(level, own[0], own[1], 6) : null) : gameSpot(level, z, size, Trainers.POS.get(ps.key()));
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
			if (ownOnly) break;
			if (present.contains("item:" + fi.key()) || takenByAnyone("item:" + fi.key()) || itemSpot(fi.key()) || java.util.Arrays.asList(GONE_ITEMS).contains(fi.key())) continue;
			BlockPos at = gameSpot(level, z, size, new int[] {fi.x(), fi.y()});
			if (at == null) continue;
			placeMon(level, "poke_ball", "item:" + fi.key(), at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0F);
			count++;
		}
		return count;
	}

	static boolean takenByAnyone(String key) {
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
		Trainers.Person who = t == null ? Trainers.PEOPLE.get(key) : null;
		n.setCustomName(net.minecraft.network.chat.Component.literal(t != null ? t.title() : who != null ? title(who.sprite()) : Npc.ROLES[role].toUpperCase()));
		n.setCustomNameVisible(false);
		level.addFreshEntity(n);
		return n;
	}

	/** What someone's called from what they look like (a youngster: YOUNGSTER; a cooltrainer_f: LASS), as the games label them. */
	static String title(String sprite) {
		String s = sprite == null ? "" : sprite.replaceAll("_\\d+$", "").replaceAll("_[fm]2?$", "");
		return switch (s) {
			case "cooltrainer" -> sprite.contains("_f") ? "LASS" : "COOLTRAINER";
			case "little_boy" -> "BOY";
			case "little_girl" -> "GIRL";
			case "middle_aged_man", "balding_guy", "gentleman" -> "MAN";
			case "middle_aged_woman", "lady" -> "WOMAN";
			case "old_man", "gramps", "grandad" -> "OLD MAN";
			case "" -> "PERSON";
			default -> s.replace('_', ' ').toUpperCase();
		};
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
