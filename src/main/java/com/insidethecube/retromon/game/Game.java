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
		// Not a roof: real ground has more under it (a roof has the room beneath it).
		return !level.getBlockState(at.below(2)).isAir() && !level.getBlockState(at.below(3)).isAir() && level.canSeeSky(at);
	}

	/**
	 * The nearest good spot to stand around x, z (on the ground, not a roof or a treetop), searching outwards
	 * up to `radius` blocks. The ground height comes from the map itself.
	 */
	public static BlockPos spot(ServerLevel level, int x, int z, int radius) {
		if (level.dimension() == Interiors.LEVEL) return Interiors.spot(level, x, z, radius);
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
		if (!wet && !high) return true;
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
			new Zone("viridian_city", -830, 495, -745, 589), new Zone("route_22", -900, 495, -831, 565), new Zone("route_22", -1000, 512, -901, 600), new Zone("route_2", -830, 340, -745, 494)};

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
		} else {
			zones.addAll(List.of(DEFAULT_ZONES));
			saveMarker();
		}
	}

	private static final Zone ROUTE_22_WEST = new Zone("route_22", -1000, 512, -901, 600);

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
		if (r.id().equals("lab") && dd.started && (dd.story.contains("parcel") || dd.hasDex()) && !dd.beaten.contains("rival_lab") && !dd.party.isEmpty()
				&& !RIVALS.containsKey(p.getUUID()) && p.level() instanceof ServerLevel lv) {
			Npc rv = rivalNpc(p, "rival_lab", BlockPos.containing(r.ox() + 10.2, r.oy(), r.oz() + 4.6));
			if (rv != null) {
				rv.setFacing(90F);
				rv.rehome(rv.blockPosition());
			}
		}
		// A room that's a place of its own (Viridian Forest, Route 2's gates): you're there now (its wild POKéMON, its name).
		if (!r.place().isEmpty() && !r.place().equals(data(p).place)) enter(p, r.place());
		Places.Place pl = Places.get(r.place());
		CompoundTag t = new CompoundTag();
		String name = fill(p, List.of(r.name().replace("Rival's", "{rival}'s"))).get(0).toUpperCase().replace("POKÉ", "POKé");
		t.putString("name", banner ? name : "");
		String nm = r.name().toLowerCase(java.util.Locale.ROOT);
		t.putString("music", nm.contains("lab") ? "oaks_lab" : nm.contains("center") ? "pokecenter" : pl == null ? "" : pl.music());
		Net.send(p, "room", t);
	}

	/** Back outside: the place's own music again. */
	public static void roomLeft(ServerPlayer p) {
		Places.Place pl = Places.get(data(p).place);
		CompoundTag t = new CompoundTag();
		t.putString("name", "");
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
			if (!inside && s.getTickCount() % 2 == 0) enter(p, placeAt(p.getX(), p.getZ()));
			if (!inside && !data(p).started && !BUSY.containsKey(id)) oakStops(p);
			if (!inside && s.getTickCount() % 2 == 0 && !BUSY.containsKey(id)) gymShut(p);
			if (s.getTickCount() % 5 == 0) story(p);
			if (s.getTickCount() % 40 == 0) moonlight(p);
			follower(p);
			if (s.getTickCount() % 10 == 0) roamers(p);
			// Kept in Adventure mode (so nothing on the map can be broken, Axiom included) unless map editing is on.
			if (!build && s.getTickCount() % 20 == 0 && p.gameMode.getGameModeForPlayer() != GameType.ADVENTURE) p.setGameMode(GameType.ADVENTURE);
			Vec3 was = WAS_AT.put(id, p.position());
			if (was != null && was.distanceToSqr(p.position().x, was.y, p.position().z) > 0.0004) WALKING.put(id, s.getTickCount());
			if (Battles.in(p) || BUSY.containsKey(id) || inside || AFTER_BATTLE.containsKey(id)) {
				LAST.put(id, p.blockPosition());
				continue;
			}
			BlockPos pos = p.blockPosition();
			BlockPos last = LAST.put(id, pos);
			if (last != null && (last.getX() != pos.getX() || last.getZ() != pos.getZ())) step(p, pos);
			if (touchWild(p)) continue;
			if (s.getTickCount() % 4 == 0) watch(p);
		}
		approach(s);
		mumsGoHome();
		rivalsLeave(s);
		rocketsLeave(s);
		if (s.getTickCount() % 40 == 0) placeTouchPoints(s);
		holdTime(s);
	}

	/** Each player's NIGHTS setting (0 dark, 1 soft, 2 bright), sent from their OPTION. */
	private static final Map<UUID, Integer> NIGHTS = new HashMap<>();

	public static void nights(ServerPlayer p, CompoundTag t) {
		NIGHTS.put(p.getUUID(), Math.max(0, Math.min(2, t.getIntOr("nights", 1))));
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
		if (COOLDOWN.containsKey(p.getUUID()) || AFTER_BATTLE.containsKey(p.getUUID()) || !data(p).anyHealthy()) return false;
		// Only when you walk into it: standing still, one wandering by doesn't drag you into a battle.
		Integer walked = WALKING.get(p.getUUID());
		if (walked == null || p.level().getServer().getTickCount() - walked > 6) return false;
		String tag = "wild:" + p.getUUID();
		ServerLevel level = (ServerLevel) p.level();
		for (WorldMon w : level.getEntitiesOfClass(WorldMon.class, p.getBoundingBox().inflate(0.9, 1.5, 0.9), w -> tag.equals(w.key()) && !w.airborne())) {
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
		boolean night = t >= 12300 && t < 23700 && NIGHTS.getOrDefault(p.getUUID(), 1) > 0;
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
		if (!d.hasDex() && !d.story.contains("parcel")) {
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
			} else if ("pallet_town".equals(d.place) && !Interiors.in(p) && !MUMS.containsKey(p.getUUID()) && p.onGround()) {
				mumComes(p, level);
				return;
			}
		}
		// TEAM ROCKET (JESSIE and JAMES, and their MEOWTH), the first time you're out in VIRIDIAN with a POKéMON.
		if (!d.story.contains("rocket_viridian") && !d.hasDex() && d.anyHealthy() && "viridian_city".equals(d.place) && !Interiors.in(p) && !ROCKETS.containsKey(p.getUUID())
				&& p.onGround()) {
			rocketsArrive(p, level);
			return;
		}
		// Your rival, from the League's way, once you have the POKéDEX.
		if (d.hasDex() && d.anyHealthy() && !d.beaten.contains("rival_22") && "route_22".equals(d.place) && !RIVALS.containsKey(p.getUUID())) {
			BlockPos from = alongRoute(level, p, -1, 0, 12, 6);
			if (from == null) from = spot(level, p.getBlockX() - 9, p.getBlockZ(), 5);
			Npc n = rivalNpc(p, "rival_22", from);
			if (n != null) {
				RIVAL_FROM.put(p.getUUID(), Vec3.atBottomCenterOf(from));
				walkUp(p, n, rival22(p), "meet_rival", null);
			}
		}
	}

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

	private static void rocketsArrive(ServerPlayer p, ServerLevel level) {
		Vec3 look = Vec3.directionFromRotation(0, p.getYRot()).multiply(1, 0, 1);
		if (look.lengthSqr() < 1e-4) look = new Vec3(0, 0, -1);
		look = look.normalize();
		BlockPos at = spot(level, (int) Math.floor(p.getX() + look.x * 7), (int) Math.floor(p.getZ() + look.z * 7), 4);
		if (at == null) return;
		Npc jessie = place(level, Npc.TRAINER, "rocket_viridian", at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0F);
		if (jessie == null) return;
		jessie.setCustomName(net.minecraft.network.chat.Component.literal("JESSIE"));
		jessie.setFacing(jessie.yawTo(p));
		List<Npc> both = new ArrayList<>(List.of(jessie));
		BlockPos side = spot(level, at.getX() + (int) Math.round(-look.z * 2), at.getZ() + (int) Math.round(look.x * 2), 2);
		Npc james = side == null ? null : place(level, Npc.PERSON, "rocket_james", side.getX() + 0.5, side.getY(), side.getZ() + 0.5, 0F);
		if (james != null) {
			james.setCustomName(net.minecraft.network.chat.Component.literal("JAMES"));
			james.setFacing(james.yawTo(p));
			both.add(james);
		}
		ROCKETS.put(p.getUUID(), both);
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

	/** MUM, come out to see you off with the TOWN MAP (and the ticks left till she's home again). */
	private static final Map<UUID, Npc> MUMS = new HashMap<>();
	private static final Map<UUID, Integer> MUM_HOME = new HashMap<>();

	private static void mumComes(ServerPlayer p, ServerLevel level) {
		BlockPos at = alongRoute(level, p, 0, -1, 9, 5);
		if (at == null) at = spot(level, p.getBlockX(), p.getBlockZ() - 7, 4);
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
		Vec3 home = new Vec3(-802.0, mum.getY(), 680.6);
		mum.getNavigation().moveTo(home.x, home.y, home.z, 0.8);
		MUM_HOME.put(p.getUUID(), 140);
	}

	private static void mumsGoHome() {
		for (Map.Entry<UUID, Integer> e : List.copyOf(MUM_HOME.entrySet())) {
			Npc mum = MUMS.get(e.getKey());
			int left = e.getValue() - 1;
			if (mum == null || mum.isRemoved() || left <= 0 || mum.position().distanceTo(new Vec3(-802.0, mum.getY(), 680.6)) < 1.6) {
				if (mum != null && !mum.isRemoved()) mum.discard();
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
		if (needs.equals("CHAMPION")) return d.story.contains("champion");
		return d.badges.contains(needs);
	}

	/** Turned back at a guarded way: the guard says why. */
	public static void gateBlocked(ServerPlayer p, String needs) {
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
	private static void watch(ServerPlayer p) {
		PlayerData d = data(p);
		if (!d.started || !d.anyHealthy() || WALKS.containsKey(p.getUUID())) return;
		ServerLevel level = (ServerLevel) p.level();
		for (Npc n : level.getEntitiesOfClass(Npc.class, p.getBoundingBox().inflate(11))) {
			if (n.role() != Npc.TRAINER && n.role() != Npc.LEADER || n.approaching > 0 || n.talker != null) continue;
			Trainers.Trainer t = Trainers.get(n.key());
			if (t == null || t.sight() <= 0 || d.beaten.contains(t.key()) || !spots(n, p, t)) continue;
			walkUp(p, n, t, spottedMusic(t), null);
			return;
		}
	}

	/** Whether a trainer sees you (see watch). */
	static boolean spots(Npc n, ServerPlayer p, Trainers.Trainer t) {
		double dx = p.getX() - n.getX(), dz = p.getZ() - n.getZ(), dy = p.getY() - n.getY();
		if (Math.abs(dy) > 2.5) return false;
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
				if (viridianMart(n) && d.started && !d.hasDex()) {
					if (d.story.contains("parcel")) dialogue(p, n, "talk", List.of("Okay! Say hi to PROF.OAK for me!"));
					else giveParcel(p, n);
					return;
				}
				CompoundTag t = new CompoundTag();
				t.putString("mart", n.key().isEmpty() ? d.place : n.key());
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

	/** The in-game trades: what each trader wants for theirs (as Red and Blue have them). */
	private static final Map<String, String> TRADE_WANTS = Map.of("mr_mime_trade", "abra", "jynx_trade", "poliwhirl", "farfetch_d_trade", "spearow",
			"lickitung_trade", "slowbro");

	/** A trade agreed on the trade screen: yours (party slot i, the species they want) goes, theirs comes. */
	public static void tradeDone(ServerPlayer p, CompoundTag t) {
		PlayerData d = data(p);
		String key = t.getStringOr("static", "");
		Trainers.Static st = Trainers.STATICS.get(key);
		int i = t.getIntOr("i", -1);
		String want = TRADE_WANTS.get(key);
		if (st == null || want == null || d.statics.contains(key) || i < 0 || i >= d.party.size() || !d.party.get(i).species.id().equals(want)) return;
		Mon got = new Mon(Species.get(st.species()), st.level(), RNG);
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
			pickUp(p, w, key.substring(5));
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
				if (tr != null && !d.beaten.contains(tr.key()) && d.anyHealthy()) Battles.trainer(p, tr);
			}
			case "static" -> {
				Trainers.Static st = Trainers.STATICS.get(t.getStringOr("static", ""));
				if (st == null || d.statics.contains(st.id()) || !d.anyHealthy()) return;
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
	 * People who stand somewhere on John's map other than where the games' grid puts them: Pallet's girl ("I'm
	 * raising POKéMON too!") by the flower beds and roses in the middle of town, as Let's Go has her, rather than
	 * out at the west edge. Key, then x and z.
	 */
	private static final Object[][] SPOTS = {{"pallet_town_p1", -801, 698}, {"pallet_town_p2", -798, 689}};

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
		// Those in SPOTS: moved there (or put there, if they're missing), once per save.
		for (Object[] o : SPOTS) {
			String key = (String) o[0], doneKey = "spot:" + key;
			int x = (Integer) o[1], z = (Integer) o[2];
			Trainers.Person who = Trainers.PEOPLE.get(key);
			if (done.contains(doneKey) || who == null || !done.contains("place:" + who.place())) continue;
			if (!level.hasChunkAt(new BlockPos(x, START_Y, z))) continue;
			BlockPos at = spot(level, x, z, 6);
			net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(x - 120, level.getMinY(), z - 120, x + 120, level.getMaxY(), z + 120);
			List<Npc> found = level.getEntitiesOfClass(Npc.class, box, n -> key.equals(n.key()));
			if (found.isEmpty()) place(level, Npc.PERSON, key, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0F);
			else for (Npc n : found) n.rehome(at);
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
			int[] own = spotOf(ps.key());
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
