package com.insidethecube.retromon.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import com.insidethecube.retromon.Retromon;
import com.insidethecube.retromon.engine.Item;
import com.insidethecube.retromon.engine.Mon;
import com.insidethecube.retromon.engine.Species;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * (0.71, John's 27 Nov 18:06: "build all in for Kanto") FireRed's Kanto story where Retromon didn't have it yet, as
 * the Kanto Campaign Events sheet lists it:
 *
 * <ul>
 * <li>your rival at the foot of NUGGET BRIDGE, on SILPH CO.'s 7F, and on ROUTE 22 the second time (all eight badges);</li>
 * <li>the Viridian GYM shut till you've the other seven badges, the Cinnabar GYM's door locked without the SECRET KEY,
 * and CERULEAN CAVE for the CHAMPION only;</li>
 * <li>MR. FUJI on the POKéMON TOWER's 7F, who takes you home and gives you the POKé FLUTE there;</li>
 * <li>the S.S. ANNE setting sail once you're off with HM01;</li>
 * <li>the NUGGET BRIDGE ROCKET's prize and his offer to join (for when he's standing there);</li>
 * <li>the old man's TEACHY TV, BILL's CELL SEPARATOR, the COIN CASE, the GAME CORNER's coins, slots and prizes, FLASH
 * in ROCK TUNNEL, the AIDEs counting your POKéMON and the old lady's TEA;</li>
 * <li>JESSIE and JAMES where Let's Go has them (MT. MOON, the ROCKET HIDEOUT, the POKéMON TOWER, SILPH CO. and ROUTE
 * 17), as John asked at 18:24.</li>
 * </ul>
 *
 * The EVENT PICKER (OPTION) is at the end: any step of the sheet, before it or in it.
 */
public final class KantoStory {
	private KantoStory() {}

	private static final Random RNG = new Random();

	private static PlayerData data(ServerPlayer p) {
		return Game.data(p);
	}

	private static void sound(ServerPlayer p, String s) {
		CompoundTag t = new CompoundTag();
		t.putString("sound", s);
		Net.send(p, "sound", t);
	}

	// ---------- the doors that need something ----------

	/** What a door into a building needs (null: nothing). */
	public static String doorNeeds(String door) {
		return switch (door) {
			case "viridian_gym" -> "BADGES7";
			case "cinnabar_gym" -> "secret_key";
			case "cerulean_cave" -> "CHAMPION";
			default -> null;
		};
	}

	/** Every badge but GIOVANNI's: the Viridian GYM opens then (FireRed's). */
	public static boolean sevenBadges(PlayerData d) {
		for (String b : com.insidethecube.retromon.engine.Campaign.BADGES) if (!b.equals("EARTHBADGE") && !d.badges.contains(b)) return false;
		return true;
	}

	/** Turned back at a door: why. */
	public static void doorBlocked(ServerPlayer p, String door, String needs) {
		switch (door) {
			// (The GYM's locked doors say so as you walk up to them; this is only if that hasn't.)
			case "viridian_gym" -> {
				if (!Game.BUSY.containsKey(p.getUUID()))
					Game.message(p, List.of("VIRIDIAN CITY POKéMON GYM", "The GYM's doors are locked...", "It looks like the GYM LEADER isn't here."));
			}
			case "cinnabar_gym" -> Game.message(p, List.of("The door is locked..."));
			case "cerulean_cave" -> Game.dialogue(p, null, "talk",
					List.of("This is CERULEAN CAVE! Horribly strong POKéMON live in there!", "The POKéMON LEAGUE champion is the only person who is allowed in!"),
					Game.whoTag("MAN"));
			default -> Game.gateBlocked(p, needs);
		}
	}

	/** Through a door that needed something: the first time, what opened it. */
	public static void doorOpened(ServerPlayer p, String door) {
		PlayerData d = data(p);
		if (door.equals("cinnabar_gym") && d.statics.add("door:cinnabar_gym")) {
			sound(p, "key");
			Game.message(p, List.of("The door is locked...", Game.player(p) + " used the SECRET KEY.", "The door opened!"));
		}
	}

	// ---------- who's in a room ----------

	/** Whether someone of a room's is there for you now (null: the story doesn't say). */
	public static Boolean wants(ServerPlayer p, String key) {
		if (p == null) return null;
		PlayerData d = data(p);
		return switch (key) {
			// MR. FUJI on the TOWER's 7F, till you've freed him.
			case "pokemon_tower_7f_p0" -> !d.statics.contains("fuji_rescued");
			// BILL, once he's himself again (FireRed's CLEFAIRY in his house till then).
			case "bills_house_p0" -> billSeparated(d);
			default -> null;
		};
	}

	static boolean billSeparated(PlayerData d) {
		return d.statics.contains("bill_separated") || d.statics.contains("gift:bills_house:s_s_ticket");
	}

	/** Where MR. FUJI stands at home once he's back (room coordinates), and where you come in with him. */
	private static final double[] FUJI_HOME = {5.5, 3.5}, FUJI_YOU = {5.5, 5.6};
	/** Where BILL steps out of his TELEPORTER (room coordinates; his own spot by the table). */
	private static final double[] BILL_OUT = {8.5, 5.5};

	/** You've come into a room: the story's own people there (or gone), your rival on SILPH CO.'s 7F, FLASH. */
	public static void roomEntered(ServerPlayer p, Interiors.Room r, boolean fromOutside) {
		if (!(p.level() instanceof ServerLevel level)) return;
		PlayerData d = data(p);
		switch (r.id()) {
			case "lavender_volunteer_house" -> keep(level, r, "mr_fujis_house_p4", "MR.FUJI", d.statics.contains("fuji_rescued"), FUJI_HOME[0], FUJI_HOME[1], 0F);
			case "silph_co_7f" -> {
				if (d.started && !d.beaten.contains("rival_silph") && d.anyHealthy()) {
					Npc n = Game.rivalNpc(p, "rival_silph", BlockPos.containing(r.ox() + 2.5, r.oy(), r.oz() + 6.5));
					if (n != null) {
						n.setFacing(180F);
						n.rehome(n.blockPosition());
					}
				}
			}
			default -> {
			}
		}
		for (Jj j : JJ) if (j.room().equals(r.id())) jjPut(p, level, r, j);
		// FLASH (FireRed's): ROCK TUNNEL is pitch dark till one of yours lights it.
		if (r.id().startsWith("rock_tunnel")) {
			if (fromOutside || !d.story.contains("flash_lit")) flashAsk(p);
			else flashOn(p);
		} else if (d.story.remove("flash_lit")) {
			flashOff(p);
			Game.sync(p);
		}
	}

	/** Back outside: FLASH's light is left behind. */
	public static void roomLeft(ServerPlayer p) {
		PlayerData d = data(p);
		if (d.story.remove("flash_lit")) {
			flashOff(p);
			Game.sync(p);
		}
	}

	/** Someone the story puts in a room (or takes out of it): there if wanted, once; gone if not. */
	private static Npc keep(ServerLevel level, Interiors.Room r, String key, String name, boolean want, double x, double z, float yaw) {
		net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(r.ox() - 2, r.oy() - 3, r.oz() - 2, r.ox() + r.w() + 2, r.oy() + 6, r.oz() + r.d() + 2);
		Npc have = null;
		for (Npc n : level.getEntitiesOfClass(Npc.class, box, n -> key.equals(n.key()))) {
			if (!want || have != null) n.discard();
			else have = n;
		}
		if (!want || have != null) return have;
		Npc n = Game.place(level, Npc.PERSON, key, r.ox() + x, r.oy(), r.oz() + z, yaw);
		if (n != null) n.setCustomName(net.minecraft.network.chat.Component.literal(name));
		return n;
	}

	// ---------- JESSIE and JAMES (Let's Go's) ----------

	/**
	 * Where JESSIE and JAMES battle you, as in Let's Go (after Retromon's VIRIDIAN, where they first meet you): the
	 * room, the spot each stands (room coordinates, searched round for a floor), their POKéMON's level and what they
	 * say. ROUTE 17 (out on the CYCLING ROAD) is done apart, as your rival on ROUTE 22 is.
	 */
	record Jj(String where, String room, String until, double jx, double jz, double mx, double mz, int level, String evolved, List<String> before,
			List<String> defeat, List<String> after) {}

	static final List<Jj> JJ = List.of(
			new Jj("mtmoon", "mt_moon_b2f", "mt_moon_b2f_0", 22.5, 25.5, 24.5, 25.5, 12, "",
					List.of("JESSIE: There you are, twerp! We told you we'd be back!", "JAMES: The fossils in this mountain are TEAM ROCKET's now!",
							"MEOWTH: Mrowr!", "JESSIE: And this time, your POKéMON's coming with us!"),
					List.of("JESSIE: Not again!", "JAMES: Beaten by the twerp twice!"), List.of("JAMES: This isn't over, twerp! TEAM ROCKET never gives up!")),
			new Jj("hideout", "rocket_hideout_b4f", "rocket_hideout_b4f_0", 18.5, 8.5, 20.5, 8.5, 32, "e",
					List.of("JESSIE: Well, well! The twerp found our HIDEOUT!", "JAMES: The BOSS is right through there.", "JESSIE: You'll have to get past us first!"),
					List.of("JESSIE: How?!", "JAMES: The BOSS won't like this one bit..."), List.of("JESSIE: Go on, then. See how you do against the BOSS!")),
			new Jj("tower", "pokemon_tower_7f", "fuji_rescued", 10.5, 13.5, 13.5, 13.5, 34, "e",
					List.of("JAMES: The twerp's come all the way to the top!", "JESSIE: This tower's spooky enough without you, twerp!",
							"JAMES: Let's send this one packing!"),
					List.of("JESSIE: I'm too scared to fight properly up here!", "JAMES: That's your excuse?!"), List.of("JAMES: We're off! This place gives me the creeps!")),
			new Jj("silph", "silph_co_11f", "silph_co_11f_0", 10.5, 11.5, 10.5, 13.5, 36, "e",
					List.of("JESSIE: Hold it right there, brat!", "JAMES: SILPH CO. belongs to TEAM ROCKET now!", "JESSIE: And we're not letting you near the BOSS!"),
					List.of("JESSIE: No! Not here, not now!", "JAMES: We've been beaten again!"), List.of("JESSIE: Fine! Let the BOSS deal with you!")));

	private static final List<String> R17_BEFORE = List.of("JESSIE: Out of our way, twerp! This road's ours!",
			"JAMES: We've been training day and night for this!", "JESSIE: Time to show you what TEAM ROCKET's really made of!"),
			R17_DEFEAT = List.of("JESSIE: After all that training?!", "JAMES: Looks like TEAM ROCKET's blasting off again..."),
			R17_AFTER = List.of("JESSIE: You haven't heard the last of us, twerp!");

	static String jjKey(String where) {
		return "rocket_jj_" + where;
	}

	private static Jj jj(String key) {
		for (Jj j : JJ) if (jjKey(j.where()).equals(key)) return j;
		return null;
	}

	/** JESSIE and JAMES's team there: JESSIE's EKANS (ARBOK) and JAMES's KOFFING (WEEZING). */
	static Trainers.Trainer jjTrainer(String key) {
		if (key.equals(jjKey("route17"))) return jjMake(key, "route_17", 52, true, R17_BEFORE, R17_DEFEAT, R17_AFTER);
		Jj j = jj(key);
		if (j == null) return null;
		return jjMake(key, j.room(), j.level(), !j.evolved().isEmpty(), j.before(), j.defeat(), j.after());
	}

	private static Trainers.Trainer jjMake(String key, String place, int level, boolean evolved, List<String> before, List<String> defeat, List<String> after) {
		List<Trainers.Member> team = List.of(new Trainers.Member(evolved ? "arbok" : "ekans", level), new Trainers.Member(evolved ? "weezing" : "koffing", level));
		return new Trainers.Trainer(key, place, "TEAM ROCKET", "jessie", "none", 0, 40, team, null, before, defeat, after);
	}

	/** Each player's JESSIE and JAMES where they're waiting (JESSIE first). */
	private static final Map<UUID, List<Npc>> JJ_NOW = new HashMap<>();

	/** Whether one of JESSIE and JAMES in the world is the story's (else they're left over in a save, and go). */
	public static boolean jjKnown(Npc n) {
		for (List<Npc> l : JJ_NOW.values()) if (l.contains(n)) return true;
		return false;
	}

	private static void jjPut(ServerPlayer p, ServerLevel level, Interiors.Room r, Jj j) {
		PlayerData d = data(p);
		String key = jjKey(j.where());
		jjDrop(p);
		// (Not once you're past that part of the story: MIGUEL, the BOSS, MR. FUJI freed.)
		if (!d.started || d.beaten.contains(key) || !d.anyHealthy() || d.beaten.contains(j.until()) || d.statics.contains(j.until())) return;
		BlockPos ja = floorNear(level, r, j.jx(), j.jz()), ma = floorNear(level, r, j.mx(), j.mz());
		if (ja == null) return;
		List<Npc> both = new ArrayList<>();
		Npc jessie = Game.place(level, Npc.TRAINER, key, ja.getX() + 0.5, ja.getY(), ja.getZ() + 0.5, 0F);
		if (jessie == null) return;
		jessie.setCustomName(net.minecraft.network.chat.Component.literal("JESSIE"));
		jessie.setFacing(jessie.yawTo(p));
		jessie.rehome(ja);
		both.add(jessie);
		if (ma != null && !ma.equals(ja)) {
			Npc james = Game.place(level, Npc.PERSON, key + "_james", ma.getX() + 0.5, ma.getY(), ma.getZ() + 0.5, 0F);
			if (james != null) {
				james.setCustomName(net.minecraft.network.chat.Component.literal("JAMES"));
				james.setFacing(james.yawTo(p));
				james.rehome(ma);
				both.add(james);
			}
		}
		JJ_NOW.put(p.getUUID(), both);
		Game.meowth(level, jessie, j.where());
	}

	private static void jjDrop(ServerPlayer p) {
		List<Npc> had = JJ_NOW.remove(p.getUUID());
		if (had != null) for (Npc n : had) if (!n.isRemoved()) n.discard();
	}

	/** Free floor near a spot in a room (room coordinates): within 3 blocks, at the floor's height or a step up or down. */
	private static BlockPos floorNear(ServerLevel level, Interiors.Room r, double x, double z) {
		BlockPos c = BlockPos.containing(r.ox() + x, r.oy(), r.oz() + z);
		for (int rad = 0; rad <= 3; rad++)
			for (int dx = -rad; dx <= rad; dx++)
				for (int dz = -rad; dz <= rad; dz++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != rad) continue;
					for (int dy : new int[] {0, 1, -1, 2, 3}) {
						BlockPos at = c.offset(dx, dy, dz);
						if (stand(level, at)) return at;
					}
				}
		return null;
	}

	private static boolean stand(ServerLevel level, BlockPos at) {
		return level.getBlockState(at).getCollisionShape(level, at).isEmpty() && level.getBlockState(at.above()).getCollisionShape(level, at.above()).isEmpty()
				&& !level.getBlockState(at.below()).getCollisionShape(level, at.below()).isEmpty() && level.getFluidState(at).isEmpty();
	}

	// ---------- every few ticks ----------

	/** Each 5 ticks, with nothing else going on: who comes up to you, and what's happened since. */
	public static void tick(ServerPlayer p) {
		PlayerData d = data(p);
		if (!d.started || !(p.level() instanceof ServerLevel level)) return;
		UUID id = p.getUUID();
		// JESSIE and JAMES: gone in a puff once you've beaten them; JESSIE comes over when you're close.
		List<Npc> jj = JJ_NOW.get(id);
		if (jj != null && !jj.isEmpty()) {
			Npc jessie = jj.get(0);
			if (jessie.isRemoved() || jessie.level() != p.level()) {
				if (!Game.WALKS.containsKey(id)) jjDrop(p);
			} else if (d.beaten.contains(jessie.key())) {
				// ((0.74) John's 10:41: "they should say something and then walk away, instead of just disappearing")
				// JESSIE's parting words, then they walk off (Game.rocketsOff), and they're gone once they're away.
				if (!Game.engaged(p) && !Game.AFTER_BATTLE.containsKey(id) && !Game.WALKS.containsKey(id)) {
					Trainers.Trainer t = jjTrainer(jessie.key());
					Game.rocketsOff(p, jj, t == null ? List.of() : t.after());
				}
			} else if (!Game.engaged(p) && !Game.WALKS.containsKey(id) && jessie.distanceTo(p) < 5.5 && d.anyHealthy()) {
				for (Npc n : jj) n.setFacing(n.yawTo(p));
				jessie.talker = p;
				Game.walkUp(p, jessie, jjTrainer(jessie.key()), "meet_evil_trainer", null);
				return;
			}
		}
		if (Game.engaged(p) || Game.WALKS.containsKey(id)) return;
		// Your rival on SILPH CO.'s 7F: "What kept you?" as you come near.
		Npc rv = Game.RIVALS.get(id);
		if (rv != null && !rv.isRemoved() && rv.key().equals("rival_silph") && !d.beaten.contains("rival_silph") && rv.level() == p.level()
				&& rv.distanceTo(p) < 4.5 && d.anyHealthy()) {
			Game.walkUp(p, rv, rivalSilph(p), "meet_rival", null);
			return;
		}
		if (Interiors.in(p)) return;
		// The S.S. ANNE sets sail once you're off her with HM01 (FireRed's).
		if (d.statics.contains("gift:ss_anne_captains_room:hm_cut") && !d.statics.contains("ss_anne_sailed") && "vermilion_city".equals(d.place)) {
			d.statics.add("ss_anne_sailed");
			sound(p, "ship_horn");
			Game.message(p, List.of("The S.S.ANNE sounded its horn...", "The S.S.ANNE has set sail!"));
			return;
		}
		// Your rival at the foot of NUGGET BRIDGE (FireRed's third battle with him): he comes down off the bridge.
		if (d.hasDex() && d.anyHealthy() && !d.beaten.contains("rival_cerulean") && !Game.RIVALS.containsKey(id) && p.getX() > CERULEAN_TRIGGER[0]
				&& p.getX() < CERULEAN_TRIGGER[1] && p.getZ() > CERULEAN_TRIGGER[2] && p.getZ() < CERULEAN_TRIGGER[3]) {
			BlockPos from = Game.spot(level, CERULEAN_SPOT[0], CERULEAN_SPOT[1], 3);
			Npc n = from == null ? null : Game.rivalNpc(p, "rival_cerulean", from);
			if (n != null) {
				Game.RIVAL_FROM.put(id, Vec3.atBottomCenterOf(from));
				Game.walkUp(p, n, rivalCerulean(p), "meet_rival", null);
			}
			return;
		}
		// ROUTE 22 again, with all eight badges (FireRed's seventh): along the top path from the LEAGUE's gate, as the first time.
		if (d.badges.contains("EARTHBADGE") && d.beaten.contains("rival_22") && !d.beaten.contains("rival_22b") && d.anyHealthy() && "route_22".equals(d.place)
				&& !Game.RIVALS.containsKey(id) && p.getX() > Game.RIVAL_22_TRIGGER[0] && p.getX() < Game.RIVAL_22_TRIGGER[1] && p.getZ() > Game.RIVAL_22_TRIGGER[2]
				&& p.getZ() < Game.RIVAL_22_TRIGGER[3]) {
			BlockPos from = Game.spot(level, Game.RIVAL_22_SPOT[0], Game.RIVAL_22_SPOT[1], 3);
			Npc n = from == null ? null : Game.rivalNpc(p, "rival_22b", from);
			if (n != null) {
				Game.RIVAL_FROM.put(id, Vec3.atBottomCenterOf(from));
				Game.walkUp(p, n, rival22b(p), "meet_rival", null);
			}
			return;
		}
		// JESSIE and JAMES on ROUTE 17 (the CYCLING ROAD), as Let's Go has them, once you've seven badges.
		String r17 = jjKey("route17");
		if (sevenBadges(d) && !d.beaten.contains(r17) && d.anyHealthy() && "route_17".equals(d.place) && !JJ_NOW.containsKey(id)) {
			BlockPos ja = alongRoad(level, p, 9), ma = ja == null ? null : alongRoad(level, p, 11);
			if (ja != null) {
				Npc jessie = Game.place(level, Npc.TRAINER, r17, ja.getX() + 0.5, ja.getY(), ja.getZ() + 0.5, 0F);
				if (jessie != null) {
					jessie.setCustomName(net.minecraft.network.chat.Component.literal("JESSIE"));
					List<Npc> both = new ArrayList<>(List.of(jessie));
					Npc james = ma == null ? null : Game.place(level, Npc.PERSON, r17 + "_james", ma.getX() + 1.5, ma.getY(), ma.getZ() + 0.5, 0F);
					if (james != null) {
						james.setCustomName(net.minecraft.network.chat.Component.literal("JAMES"));
						both.add(james);
					}
					JJ_NOW.put(id, both);
					Game.meowth(level, jessie, "route17");
					for (Npc n : both) n.setFacing(n.yawTo(p));
					jessie.talker = p;
					Game.walkUp(p, jessie, jjTrainer(r17), "meet_evil_trainer", null);
				}
			}
		}
	}

	/** Good ground ahead of you down the road (the way you're heading), about so far. */
	private static BlockPos alongRoad(ServerLevel level, ServerPlayer p, int far) {
		Vec3 look = Vec3.directionFromRotation(0, p.getYRot());
		return Game.spot(level, (int) Math.floor(p.getX() + look.x * far), (int) Math.floor(p.getZ() + look.z * far), 3);
	}

	/** NUGGET BRIDGE's foot (x0 x1 z0 z1: the grass between the bridge and Cerulean), and where your rival waits on the bridge. */
	private static final double[] CERULEAN_TRIGGER = {-355.6, -349.4, 236.0, 241.5};
	private static final int[] CERULEAN_SPOT = {-352, 226};

	// ---------- your rival (FireRed's teams, by his first POKéMON) ----------

	private static String[] rivalLine(ServerPlayer p) {
		return switch (Game.rivalStarter(p)) {
			case "bulbasaur" -> new String[] {"bulbasaur", "ivysaur", "venusaur"};
			case "charmander" -> new String[] {"charmander", "charmeleon", "charizard"};
			case "eevee" -> new String[] {"eevee", "eevee", "jolteon"};
			default -> new String[] {"squirtle", "wartortle", "blastoise"};
		};
	}

	/** His two by his first POKéMON (the one strong against yours): GYARADOS, GROWLITHE or EXEGGCUTE (EXEGGUTOR later). */
	private static String[] rivalPair(ServerPlayer p, boolean grown) {
		return switch (Game.rivalStarter(p)) {
			case "bulbasaur" -> new String[] {"gyarados", grown ? "arcanine" : "growlithe"};
			case "charmander" -> new String[] {grown ? "exeggutor" : "exeggcute", "gyarados"};
			default -> new String[] {grown ? "arcanine" : "growlithe", grown ? "exeggutor" : "exeggcute"};
		};
	}

	static Trainers.Trainer rivalCerulean(ServerPlayer p) {
		String[] line = rivalLine(p);
		return new Trainers.Trainer("rival_cerulean", "cerulean_city", Game.rival(p), "blue", "none", 0, 35,
				List.of(new Trainers.Member("pidgeotto", 18), new Trainers.Member("abra", 15), new Trainers.Member("rattata", 15),
						new Trainers.Member(line[0], 17)),
				null,
				List.of("{rival}: Yo! {player}!", "You're still struggling along back here?", "I'm doing great!", "I caught a bunch of strong and smart POKéMON!",
						"Here, let me see what you caught, {player}!"),
				List.of("Hey! Take it easy! You won already!"),
				List.of("{rival}: Hey, guess what?", "I went to BILL's and got him to show me his rare POKéMON!", "That added a lot of pages to my POKéDEX!",
						"After all, BILL's world famous as a POKéMANIAC!", "He invented the POKéMON Storage System on PC!", "Since you're using his system, go thank him!",
						"Well, I better get rolling! Smell ya later!"));
	}

	static Trainers.Trainer rivalSilph(ServerPlayer p) {
		String[] line = rivalLine(p), pair = rivalPair(p, false);
		return new Trainers.Trainer("rival_silph", "silph_co_7f", Game.rival(p), "blue", "none", 0, 35,
				List.of(new Trainers.Member("pidgeot", 37), new Trainers.Member(pair[0], 38), new Trainers.Member(pair[1], 35), new Trainers.Member("alakazam", 35),
						new Trainers.Member(line[2], 40)),
				null,
				List.of("{rival}: What kept you, {player}?", "Hahaha! I thought you'd turn up if I waited here!", "I guess TEAM ROCKET slowed you down! Not that I care!",
						"I saw you in SAFFRON, so I decided to see if you got better!"),
				List.of("Oh, man! So, you are ready for BOSS ROCKET!"),
				List.of("{rival}: Well, {player}!", "I'm moving on up and ahead!", "By checking my POKéDEX, I'm starting to see what's strong and how they evolve!",
						"I'm going to the POKéMON LEAGUE to boot out the ELITE FOUR!", "I'll become the world's most powerful trainer!",
						"{player}, well, good luck to you! Don't sweat it! Smell ya!"));
	}

	static Trainers.Trainer rival22b(ServerPlayer p) {
		String[] line = rivalLine(p), pair = rivalPair(p, false);
		return new Trainers.Trainer("rival_22b", "route_22", Game.rival(p), "blue", "none", 0, 35,
				List.of(new Trainers.Member("pidgeot", 47), new Trainers.Member("rhyhorn", 45), new Trainers.Member(pair[0], 45), new Trainers.Member(pair[1], 47),
						new Trainers.Member("alakazam", 50), new Trainers.Member(line[2], 53)),
				null,
				List.of("{rival}: What? {player}! What a surprise to see you here!", "So you're going to the POKéMON LEAGUE?", "You collected all the BADGES, too? That's cool!",
						"Then I'll whip you, {player}, as a warm-up for the POKéMON LEAGUE!", "Come on!"),
				List.of("What!? I was just careless, you!"),
				List.of("{rival}: That loosened me up.", "I'm ready for the POKéMON LEAGUE!", "{player}, you need more practice!", "But hey, you know that!",
						"I'm out of here. Smell ya!"));
	}

	/** The NUGGET BRIDGE ROCKET (FireRed's): the five's prize first, then he asks you to join TEAM ROCKET. */
	static Trainers.Trainer nuggetRocket(ServerPlayer p, Trainers.Trainer t) {
		if (t == null) return null;
		boolean had = data(p).statics.contains("gift:route_24:nugget");
		List<String> before = new ArrayList<>();
		if (!had) before.addAll(List.of("Congratulations! You beat our 5 contest trainers!", "You just earned a fabulous prize!", "{player} received a NUGGET!"));
		before.addAll(List.of("By the way, would you like to join TEAM ROCKET?", "We're a group dedicated to evil using POKéMON!", "Want to join?", "Are you sure?",
				"Come on, join us!", "I'm telling you to join!", "OK, you need convincing!", "I'll make you an offer you can't refuse!"));
		return new Trainers.Trainer(t.key(), t.place(), t.title(), t.sprite(), t.facing(), t.sight(), t.prizeBase(), t.team(), t.badge(), before,
				List.of("Arrgh! You are good!"), List.of("With your ability, you could become a top leader in TEAM ROCKET!"));
	}

	/** A trainer the story makes up (null: not one of ours). */
	static Trainers.Trainer trainer(ServerPlayer p, String key) {
		return switch (key) {
			case "rival_cerulean" -> rivalCerulean(p);
			case "rival_silph" -> rivalSilph(p);
			case "rival_22b" -> rival22b(p);
			case "route_24_0" -> nuggetRocket(p, Trainers.get(key));
			default -> key.startsWith("rocket_jj_") && !key.endsWith("_james") ? jjTrainer(key) : null;
		};
	}

	/** Just before a battle starts: the NUGGET BRIDGE ROCKET's prize goes in your bag. */
	public static void beforeBattle(ServerPlayer p, Trainers.Trainer t) {
		PlayerData d = data(p);
		if (t.key().equals("route_24_0") && d.statics.add("gift:route_24:nugget")) {
			d.give("nugget", 1);
			Game.sync(p);
		}
	}

	// ---------- talking ----------

	/** Someone the story has words for: true if it's dealt with. */
	public static boolean talk(ServerPlayer p, Npc n, PlayerData d) {
		String k = n.key();
		if (k.startsWith("rocket_jj_") && k.endsWith("_james")) {
			Game.dialogue(p, n, "talk", List.of("JAMES: Don't look at me, twerp! JESSIE's the one you want!"));
			return true;
		}
		switch (k) {
			case Game.OLD_MAN -> {
				// The old man's had his coffee: his catching lesson, then the TEACHY TV (FireRed's), once.
				if (!d.hasDex()) return false;
				Trainers.Person who = Trainers.PEOPLE.get(k);
				List<String> lines = new ArrayList<>(who == null ? List.of() : Game.fill(p, who.lines()));
				if (d.statics.add("gift:viridian_old_man:teachy_tv")) {
					d.give("teachy_tv", 1);
					Game.sync(p);
					lines.addAll(List.of("There! That's how you catch POKéMON!", "Oh, and here, take this as well.", Game.player(p) + " received a TEACHY TV!",
							"It's a TV show that teaches you how to raise POKéMON. Watch it from your BAG whenever you like!"));
					Game.dialogue(p, n, "talk", lines, Game.cues(lines.size() - 2, "get_key_item"));
				} else {
					lines.add("Have you been watching your TEACHY TV?");
					Game.dialogue(p, n, "talk", lines);
				}
				return true;
			}
			case "pokemon_tower_7f_p0" -> {
				// MR. FUJI, freed: he takes you home with him.
				Trainers.Person who = Trainers.PEOPLE.get(k);
				CompoundTag t = new CompoundTag();
				Game.dialogue(p, n, "fuji_home", who == null ? List.of("MR.FUJI: Follow me to my home.") : Game.fill(p, who.lines()), t);
				return true;
			}
			case "mr_fujis_house_p4" -> {
				if (d.statics.add("gift:mr_fujis_house:poke_flute")) {
					d.give("poke_flute", 1);
					Game.sync(p);
					List<String> lines = new ArrayList<>(Game.fill(p, List.of("MR.FUJI: {player}.", "Your POKéDEX quest may fail without love for your POKéMON.",
							"I think this may help your quest.")));
					lines.add(Game.player(p) + " received a POKé FLUTE!");
					lines.addAll(List.of("MR.FUJI: Upon hearing the POKé FLUTE, sleeping POKéMON will spring awake.", "It works on all sleeping POKéMON."));
					Game.dialogue(p, n, "talk", lines, Game.cues(3, "get_key_item"));
				} else Game.dialogue(p, n, "talk", List.of("MR.FUJI: Has my POKé FLUTE helped you?"));
				return true;
			}
			case "bills_house_p0" -> {
				if (d.statics.contains("gift:bills_house:s_s_ticket")) {
					Game.dialogue(p, n, "talk", List.of("BILL: Look, bud, just check out some of my rare POKéMON on my PC!"));
					return true;
				}
				List<String> lines = new ArrayList<>(List.of("BILL: Yeehah! Thanks, bud! I owe you one!", "So, did you come to see my POKéMON collection?",
						"You didn't? That's a bummer.", "I've got to thank you... Oh, here, maybe this'll do."));
				String got = Game.giveGift(p, "bills_house");
				if (got != null) lines.add(got);
				lines.addAll(List.of("BILL: That cruise ship, the S.S.ANNE, is in VERMILION CITY.", "Its passengers are all trainers!",
						"They invited me to their party, but I can't stand fancy do's.", "Why don't you go instead of me?"));
				Game.dialogue(p, n, "talk", lines, Game.cues(4, "get_key_item"));
				return true;
			}
			case "celadon_diner_fr_coin_case_man" -> {
				if (d.statics.add("gift:celadon_diner:coin_case")) {
					d.give("coin_case", 1);
					Game.sync(p);
					Game.dialogue(p, n, "talk", List.of("Go ahead! Laugh! I'm flat-out busted!", "No more slots for me! I'm going straight!",
							"Here! I won't be needing this anymore!", Game.player(p) + " received a COIN CASE!"), Game.cues(3, "get_key_item"));
				} else Game.dialogue(p, n, "talk", List.of("I always thought I was going to win it back..."));
				return true;
			}
			case "game_corner_p1" -> {
				CompoundTag t = Game.yesNo();
				Game.dialogue(p, n, "coins_buy", List.of("Welcome to ROCKET GAME CORNER!", "Do you need some game coins?", "It's ¥1000 for 50 coins. Would you like some?"), t);
				return true;
			}
			case "game_corner_prize_room_fr_prize_clerk_mons", "game_corner_prize_room_fr_prize_clerk_tms", "game_corner_prize_room_fr_prize_clerk_items" -> {
				prizeAsk(p, n, k.substring(k.lastIndexOf('_') + 1));
				return true;
			}
			case "celadon_mansion_1f_fr_tea_woman" -> {
				// The TEA Saffron's guards want (FireRed's), from her.
				if (d.statics.add("gift:celadon_mansion_1f:tea")) {
					d.give("tea", 1);
					Game.sync(p);
					Game.dialogue(p, n, "talk", List.of("You shouldn't spend all your money on drinks.", "Try this instead.", Game.player(p) + " received TEA!"),
							Game.cues(2, "get_key_item"));
				} else Game.dialogue(p, n, "talk", List.of("Nothing beats thirst like some hot TEA.", "It really is the best."));
				return true;
			}
			case "route_11_gate_2f_p0" -> {
				aide(p, n, d, 30, "route_11_gate_2f", "ITEMFINDER");
				return true;
			}
			case "route_15_gate_2f_p0" -> {
				aide(p, n, d, 50, "route_15_gate_2f", "EXP.SHARE");
				return true;
			}
			default -> {
				return false;
			}
		}
	}

	/** PROF. OAK's AIDEs (FireRed's): their gift once your POKéDEX has this many kinds caught. */
	private static void aide(ServerPlayer p, Npc n, PlayerData d, int need, String place, String what) {
		Trainers.Person who = Trainers.PEOPLE.get(n.key());
		List<String> after = who == null ? List.of() : Game.fill(p, who.lines());
		if (d.statics.contains("gift:" + place + ":" + (place.startsWith("route_11") ? "itemfinder" : "exp_share"))) {
			Game.dialogue(p, n, "talk", after);
			return;
		}
		int kinds = d.caught.size();
		List<String> lines = new ArrayList<>(List.of("Hi! Remember me? I'm one of PROF. OAK's AIDES.",
				"If your POKéDEX has complete data on " + need + " species, I'm supposed to give you a reward.", "PROF. OAK entrusted me with the " + what + ".",
				"Let's see... You've caught " + kinds + " kind" + (kinds == 1 ? "" : "s") + " of POKéMON."));
		if (kinds >= need) {
			lines.add("Great! Congratulations! Here you go!");
			String got = Game.giveGift(p, place);
			if (got != null) lines.add(got);
			lines.addAll(after);
			Game.dialogue(p, n, "talk", lines, Game.cues(5, "get_key_item"));
		} else {
			lines.add("You need data on at least " + need + " kinds for this reward.");
			Game.dialogue(p, n, "talk", lines);
		}
	}

	/** A POKéMON in a room the story has words for (BILL's CLEFAIRY): true if it's dealt with. */
	public static boolean talkMon(ServerPlayer p, WorldMon w) {
		if (!"pet:bills_house_clefairy".equals(w.key())) return false;
		PlayerData d = data(p);
		if (billSeparated(d) || d.statics.contains("bill_in_pod")) return false;
		CompoundTag t = Game.yesNo();
		t.putString("cry", "clefairy");
		t.putString("who", "BILL");
		Game.dialogue(p, null, "bill_help", List.of("Hiya! I'm a POKéMON...", "...No I'm not!", "Call me BILL! I'm a true-blue POKéMANIAC!",
				"Hey! What's with that skeptical look?", "I'm not joshing you, I screwed up an experiment and got combined with a POKéMON!", "So, how about it?",
				"Help me out here!"), t);
		return true;
	}

	/** What you said to someone the story asked (true if it was ours). */
	public static boolean answer(ServerPlayer p, String mode, CompoundTag t, Npc n) {
		PlayerData d = data(p);
		boolean yes = t.getBooleanOr("answered", false) ? t.getIntOr("choice", 1) == 0 : t.getBooleanOr("yes", false);
		switch (mode) {
			case "fuji_home" -> {
				d.statics.add("fuji_rescued");
				Interiors.Room home = Interiors.room("lavender_volunteer_house");
				if (home != null) {
					d.door = "lavender_volunteer_house";
					Interiors.go(p, home, FUJI_YOU[0], FUJI_YOU[1], 180F, true);
				}
				return true;
			}
			case "bill_help" -> {
				List<String> lines = new ArrayList<>();
				if (!yes) lines.addAll(List.of("No!? Come on, you gotta help a guy in deep trouble!", "What do you say, chief? Please? OK? All right!"));
				lines.addAll(List.of("Wait till I get inside the TELEPORTER.", "When I do, go to my PC and run the CELL SEPARATION SYSTEM."));
				d.statics.add("bill_in_pod");
				// Into the TELEPORTER he goes.
				for (WorldMon w : p.level().getEntitiesOfClass(WorldMon.class, p.getBoundingBox().inflate(16), w -> "pet:bills_house_clefairy".equals(w.key()))) {
					if (p.level() instanceof ServerLevel lv)
						lv.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, w.getX(), w.getY() + 0.5, w.getZ(), 12, 0.3, 0.4, 0.3, 0.02);
					w.discard();
				}
				d.statics.add("pet:bills_house_clefairy");
				CompoundTag w = new CompoundTag();
				w.putString("who", "BILL");
				Game.dialogue(p, null, "talk", lines, w);
				return true;
			}
			case "coins_buy" -> {
				if (!yes) {
					Game.dialogue(p, n, "talk", List.of("No? Please come play sometime!"));
				} else if (d.count("coin_case") <= 0) {
					Game.dialogue(p, n, "talk", List.of("Oh, you don't have a COIN CASE.", "Sorry, but we can't give you any coins without one."));
				} else if (d.coins + 50 > 9999) {
					Game.dialogue(p, n, "talk", List.of("Oops! Your COIN CASE is full."));
				} else if (d.money < 1000) {
					Game.dialogue(p, n, "talk", List.of("You can't afford the coins."));
				} else {
					d.money -= 1000;
					d.coins += 50;
					Game.sync(p);
					sound(p, "money");
					Game.dialogue(p, n, "talk", List.of("Thanks! Here are your 50 coins!", "You have " + d.coins + " COINS now."));
				}
				return true;
			}
			case "prize" -> {
				String counter = t.getStringOr("counter", "");
				List<Prize> list = PRIZES.getOrDefault(counter, List.of());
				int i = t.getIntOr("choice", -1);
				if (!t.getBooleanOr("answered", false) || i < 0 || i >= list.size()) {
					Game.dialogue(p, n, "talk", List.of("Oh, OK. Come back any time!"));
					return true;
				}
				Prize pr = list.get(i);
				if (d.coins < pr.coins()) {
					Game.dialogue(p, n, "talk", List.of("You need more coins to get that."));
					return true;
				}
				CompoundTag c = Game.yesNo();
				c.putString("counter", counter);
				c.putInt("prize", i);
				Game.dialogue(p, n, "prize_yes", List.of("So, you want " + pr.name() + "?"), c);
				return true;
			}
			case "prize_yes" -> {
				List<Prize> list = PRIZES.getOrDefault(t.getStringOr("counter", ""), List.of());
				int i = t.getIntOr("prize", -1);
				if (!yes || i < 0 || i >= list.size()) {
					Game.dialogue(p, n, "talk", List.of("Oh, OK. Come back any time!"));
					return true;
				}
				Prize pr = list.get(i);
				if (d.coins < pr.coins()) return true;
				Species sp = pr.level() > 0 ? Species.ALL.get(pr.id()) : null;
				Item it = pr.level() > 0 ? null : Item.find(pr.id());
				if (sp == null && it == null) return true;
				d.coins -= pr.coins();
				List<String> lines = new ArrayList<>();
				if (sp != null) {
					boolean box = d.party.size() >= 6;
					d.add(new Mon(sp, pr.level(), RNG));
					lines.add(Game.player(p) + " received " + pr.name() + "!");
					if (box) lines.add(pr.name() + " was sent to BILL's PC.");
				} else {
					d.give(it.id(), 1);
					lines.add(Game.player(p) + " received " + pr.name() + "!");
				}
				lines.add("You have " + d.coins + " COINS left.");
				Game.sync(p);
				Game.dialogue(p, n, "talk", lines, Game.cues(0, sp != null ? "caught_mon" : "get_item"));
				return true;
			}
			case "flash" -> {
				if (yes) {
					Mon m = flasher(p);
					if (m == null) return true;
					d.story.add("flash_lit");
					Game.sync(p);
					flashOn(p);
					Game.message(p, List.of(m.name().toUpperCase() + " used FLASH!"));
				}
				return true;
			}
			default -> {
				return false;
			}
		}
	}

	// ---------- the GAME CORNER's prizes (Red and Blue's, and FireRed's third counter) ----------

	/** A prize: a POKéMON (its level) or an item (level 0), and its price in coins. */
	record Prize(String id, String name, int level, int coins) {}

	static final Map<String, List<Prize>> PRIZES = Map.of(
			"mons", List.of(new Prize("abra", "ABRA", 9, 180), new Prize("clefairy", "CLEFAIRY", 8, 500), new Prize("nidorina", "NIDORINA", 17, 1200),
					new Prize("dratini", "DRATINI", 18, 2800), new Prize("scyther", "SCYTHER", 25, 5500), new Prize("porygon", "PORYGON", 26, 9999)),
			"tms", List.of(new Prize("tm_dragon_rage", "TM23 DRAGON RAGE", 0, 3300), new Prize("tm_hyper_beam", "TM15 HYPER BEAM", 0, 5500),
					new Prize("tm_substitute", "TM50 SUBSTITUTE", 0, 7700)),
			"items", List.of(new Prize("smoke_ball", "SMOKE BALL", 0, 800), new Prize("miracle_seed", "MIRACLE SEED", 0, 1000), new Prize("charcoal", "CHARCOAL", 0, 1000),
					new Prize("mystic_water", "MYSTIC WATER", 0, 1000)));

	private static void prizeAsk(ServerPlayer p, Npc n, String counter) {
		PlayerData d = data(p);
		if (d.count("coin_case") <= 0) {
			Game.dialogue(p, n, "talk", List.of("We exchange your coins for prizes.", "A COIN CASE is required..."));
			return;
		}
		List<Prize> list = PRIZES.getOrDefault(counter, List.of());
		CompoundTag t = new CompoundTag();
		ListTag ch = new ListTag();
		for (Prize pr : list) ch.add(StringTag.valueOf(pr.name() + "  " + pr.coins() + "C"));
		ch.add(StringTag.valueOf("NO THANKS"));
		t.put("choices", ch);
		t.putString("counter", counter);
		Game.dialogue(p, n, "prize", List.of("We exchange your coins for prizes.", "You have " + d.coins + " COINS.", "Which prize would you like?"), t);
	}

	// ---------- the slot machines ----------

	/** The reels' symbols, as SlotsScreen draws them: 0 SEVEN, 1 BAR, 2 CHERRY, 3 BELL, 4 STAR, 5 MOON. */
	public static final int[][] REELS = {{0, 2, 3, 4, 1, 5, 2, 3, 4, 5, 2, 1, 3, 4, 5}, {0, 3, 2, 4, 5, 1, 3, 2, 5, 4, 3, 1, 2, 5, 4},
			{0, 4, 5, 2, 3, 1, 4, 5, 3, 2, 4, 1, 5, 3, 2}};
	/** What three in a line pay (per coin bet on it): SEVEN 300, BAR 100, BELL, STAR and MOON 15, CHERRY 8 (one or two from the left: 2 or 4). */
	public static final int[] PAYS = {300, 100, 8, 15, 15, 15};

	/** A slot machine: with a COIN CASE and coins, its screen. */
	public static void slots(ServerPlayer p) {
		PlayerData d = data(p);
		if (d.count("coin_case") <= 0) {
			Game.message(p, List.of("A slot machine!", "Want to play?", "A COIN CASE is required..."));
			return;
		}
		if (d.coins <= 0) {
			Game.message(p, List.of("A slot machine!", "You don't have any coins!"));
			return;
		}
		CompoundTag t = new CompoundTag();
		t.putInt("coins", d.coins);
		Net.send(p, "slots", t);
	}

	/** A spin: its coins in, where each reel stops (the middle row's symbol), and what it pays. */
	public static void spin(ServerPlayer p, CompoundTag t) {
		PlayerData d = data(p);
		int bet = Math.max(1, Math.min(3, t.getIntOr("bet", 1)));
		if (d.count("coin_case") <= 0 || d.coins < bet || Battles.in(p)) return;
		d.coins -= bet;
		int[] stop = new int[3];
		for (int i = 0; i < 3; i++) stop[i] = RNG.nextInt(REELS[i].length);
		int win = payout(stop, bet);
		d.coins = Math.min(9999, d.coins + win);
		Game.sync(p);
		CompoundTag o = new CompoundTag();
		o.putIntArray("stop", stop);
		o.putInt("win", win);
		o.putInt("coins", d.coins);
		Net.send(p, "slot_result", o);
	}

	/** What a spin pays: the middle line for 1 coin; the top and bottom too for 2; the diagonals too for 3. */
	public static int payout(int[] stop, int bet) {
		int[][] lines = bet >= 3 ? new int[][] {{0, 0, 0}, {-1, -1, -1}, {1, 1, 1}, {-1, 0, 1}, {1, 0, -1}}
				: bet == 2 ? new int[][] {{0, 0, 0}, {-1, -1, -1}, {1, 1, 1}} : new int[][] {{0, 0, 0}};
		int win = 0;
		for (int[] l : lines) {
			int a = sym(0, stop[0] + l[0]), b = sym(1, stop[1] + l[1]), c = sym(2, stop[2] + l[2]);
			if (a == b && b == c) win += PAYS[a];
			else if (a == 2 && b == 2) win += 4;
			else if (a == 2) win += 2;
		}
		return win;
	}

	public static int sym(int reel, int at) {
		int[] r = REELS[reel];
		return r[Math.floorMod(at, r.length)];
	}

	// ---------- things in a room you use ----------

	/** Something in a room the story answers (the slot machines, BILL's PC): true if it's dealt with. */
	public static boolean roomUse(ServerPlayer p, String use) {
		PlayerData d = data(p);
		switch (use) {
			case "slots" -> {
				slots(p);
				return true;
			}
			case "bill_pc" -> {
				if (billSeparated(d)) {
					Net.send(p, "pc_open", new CompoundTag());
					return true;
				}
				if (!d.statics.contains("bill_in_pod")) {
					Game.message(p, List.of("BILL's PC: the TELEPORTER's CELL SEPARATION SYSTEM.", "There's nobody in the TELEPORTER."));
					return true;
				}
				// The CELL SEPARATOR: BILL's himself again, out of his TELEPORTER.
				d.statics.add("bill_separated");
				Game.sync(p);
				sound(p, "item");
				Interiors.Room r = Interiors.roomOf(p);
				if (r != null && p.level() instanceof ServerLevel lv) {
					Interiors.people(lv, r, p);
					lv.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, r.ox() + BILL_OUT[0], r.oy() + 1, r.oz() + BILL_OUT[1], 20, 0.3, 0.6, 0.3, 0.03);
				}
				Game.message(p, List.of(Game.player(p) + " initiated the TELEPORTER's CELL SEPARATOR!", "...", "BILL is himself again!"));
				return true;
			}
			default -> {
				return false;
			}
		}
	}

	// ---------- items you use from the BAG ----------

	/** The COIN CASE (your coins) and the TEACHY TV (its lessons): true if it's one of these. */
	public static boolean useItem(ServerPlayer p, String item) {
		PlayerData d = data(p);
		switch (item) {
			case "itemfinder" -> {
				// (0.72) Whether one of FireRed's hidden items is near, and which way.
				Placer.itemfinder(p);
				return true;
			}
			case "coin_case" -> {
				Game.message(p, List.of("Your COINS: " + d.coins + "."));
				return true;
			}
			case "teachy_tv" -> {
				Game.message(p, List.of("TEACHY TV! Today's lesson: catching POKéMON.", "First, battle a wild POKéMON and weaken it. Its HP in the red is best.",
						"Put it to sleep or paralyse it, if you can. That helps too.", "Then throw a POKé BALL! Stronger BALLS work better on tougher POKéMON.",
						"And remember: a POKéMON you've raised with love will do anything for you!"));
				return true;
			}
			default -> {
				return false;
			}
		}
	}

	// ---------- FLASH ----------

	/** One of your team who can light the dark (knows FLASH, and isn't fainted), with the BOULDERBADGE; or null. */
	private static Mon flasher(ServerPlayer p) {
		PlayerData d = data(p);
		if (!d.badges.contains("BOULDERBADGE")) return null;
		for (Mon m : d.party) if (!m.fainted() && m.moves.contains("flash")) return m;
		return null;
	}

	private static void flashAsk(ServerPlayer p) {
		PlayerData d = data(p);
		if (d.story.remove("flash_lit")) Game.sync(p);
		flashOff(p);
		Mon m = flasher(p);
		if (m == null) {
			Game.message(p, List.of("It's pitch black in here...", "A POKéMON that knows FLASH could light the way."));
			return;
		}
		Game.dialogue(p, null, "flash", List.of("It's pitch black in here...", "Would you like to use FLASH?"), Game.yesNo());
	}

	/** FLASH's light: you see all round you (a soft night sight, shown to no one). */
	private static void flashOn(ServerPlayer p) {
		p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION, -1, 0, true, false, false));
	}

	private static void flashOff(ServerPlayer p) {
		p.removeEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);
	}

	// ---------- the EVENT PICKER (OPTION, John's 18:06 and 18:24) ----------

	/** A step of the Kanto Campaign Events sheet: its party's point (Campaign.STAGES), where BEFORE and IN take you, and what's done by its end. */
	public record Step(int n, int stage, String before, String in, List<String> gives) {}

	private static List<Step> steps;

	public static List<Step> steps() {
		if (steps == null) {
			List<Step> out = new ArrayList<>();
			for (String[] r : Species.table("kanto_events.tsv")) {
				if (r.length < 6) continue;
				try {
					List<String> g = new ArrayList<>();
					for (String s : r[5].trim().split(" ")) if (!s.isEmpty() && !s.equals("-")) g.add(s);
					out.add(new Step(Integer.parseInt(r[0].trim()), Integer.parseInt(r[1].trim()), r[3].trim(), r[4].trim(), g));
				} catch (NumberFormatException ignored) {}
			}
			steps = out;
		}
		return steps;
	}

	/** The towns you'd have been to by each point (where you can FLY). */
	private static final String[][] TOWNS = {{"pallet_town", "route_1", "viridian_city", "route_22", "route_2", "viridian_forest"},
			{"pewter_city", "route_3", "mt_moon_1f", "route_4"}, {"cerulean_city", "route_24", "route_25", "route_5", "route_6"},
			{"vermilion_city", "route_11", "route_9", "route_10"}, {"lavender_town", "celadon_city", "route_7", "route_8"},
			{"fuchsia_city", "route_12", "route_13", "route_14", "route_15", "route_16", "route_17", "route_18"}, {"saffron_city", "route_19", "route_20"},
			{"cinnabar_island", "route_21"}, {"route_23", "indigo_plateau"}, {}};
	/** Supplies for each point: what a player usually carries by then. */
	private static final String[][] SUPPLIES = {{"poke_ball:10", "potion:5"}, {"poke_ball:10", "potion:5", "antidote:2"},
			{"great_ball:10", "super_potion:5", "repel:3", "escape_rope:2"}, {"great_ball:10", "super_potion:6", "repel:3", "escape_rope:2"},
			{"great_ball:15", "super_potion:8", "revive:2", "super_repel:3", "escape_rope:2"}, {"ultra_ball:10", "hyper_potion:6", "revive:3", "super_repel:3", "escape_rope:2"},
			{"ultra_ball:15", "hyper_potion:8", "revive:4", "max_repel:3", "escape_rope:2"}, {"ultra_ball:15", "hyper_potion:10", "revive:5", "max_repel:3", "escape_rope:2"},
			{"ultra_ball:20", "full_restore:5", "max_potion:5", "revive:6", "max_repel:5", "escape_rope:3"},
			{"ultra_ball:20", "full_restore:8", "max_potion:5", "max_revive:4", "max_repel:5", "escape_rope:3"}};

	/** Whether whoever this is runs the game (single player, or an operator): the picker's only for them. */
	private static boolean runsGame(ServerPlayer p) {
		net.minecraft.server.MinecraftServer server = p.level().getServer();
		return server != null && (server.isSingleplayer()
				|| p.createCommandSourceStack().permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER));
	}

	/**
	 * Off to a step: "go" (before it, or in it, with its point's party or your own), or "own" (your own game back,
	 * where you were). Your own game is kept aside the first time you jump.
	 */
	public static void event(ServerPlayer p, CompoundTag t) {
		if (!Game.adventure(p) || Battles.in(p)) return;
		if (!runsGame(p)) {
			Game.message(p, List.of("The EVENT PICKER is only for whoever runs the game."));
			return;
		}
		PlayerData d = data(p);
		if (t.getStringOr("mode", "").equals("own")) {
			if (d.ownGame == null) return;
			back(p, d);
			return;
		}
		int n = t.getIntOr("step", 1);
		Step step = null;
		for (Step s : steps()) if (s.n() == n) step = s;
		if (step == null) return;
		boolean in = t.getBooleanOr("in", false), keepParty = t.getBooleanOr("keepParty", false);
		String starter = t.getStringOr("starter", "bulbasaur");
		if (!List.of(com.insidethecube.retromon.engine.Campaign.STARTERS).contains(starter)) starter = "bulbasaur";
		Game.clearAround(p);
		jjDrop(p);
		CompoundTag own = d.ownGame;
		double[] ownAt = d.ownAt;
		String ownRoom = d.ownRoom;
		if (own == null) {
			// Kept aside: your own game, and where you were.
			own = d.save();
			Interiors.Room here = Interiors.roomOf(p);
			ownRoom = here == null ? "" : here.id();
			ownAt = here == null ? new double[] {p.getX(), p.getY(), p.getZ(), p.getYRot()}
					: new double[] {p.getX() - here.ox(), p.getY() - here.oy(), p.getZ() - here.oz(), p.getYRot()};
		}
		PlayerData mine = PlayerData.load(own);
		PlayerData nd = new PlayerData();
		// You: your name, your rival's, your clothes and settings, your time played.
		nd.name = mine.name;
		nd.rival = mine.rival;
		nd.gender = mine.gender;
		nd.outfit = mine.outfit;
		nd.textSpeed = d.textSpeed;
		nd.played = d.played;
		nd.since = mine.since;
		nd.day0 = mine.day0;
		nd.ownGame = own;
		nd.ownAt = ownAt;
		nd.ownRoom = ownRoom;
		nd.eventAs = "STEP " + step.n() + (in ? " (IN IT)" : " (BEFORE IT)");
		int stage = Math.max(0, Math.min(com.insidethecube.retromon.engine.Campaign.STAGES.size() - 1, step.stage()));
		// Everything done before it.
		for (Step s : steps()) {
			if (s.n() >= step.n()) break;
			for (String g : s.gives()) apply(nd, g);
		}
		nd.started = step.n() > 3;
		if (nd.started) {
			for (int i = 0; i <= stage && i < TOWNS.length; i++) for (String town : TOWNS[i]) nd.visited.add(town);
			nd.money = 3000 + 6000 * stage;
			for (String sup : SUPPLIES[Math.min(stage, SUPPLIES.length - 1)]) {
				String[] a = sup.split(":");
				if (Item.find(a[0]) != null) nd.give(a[0], Integer.parseInt(a[1]));
			}
			// Your party: that point's (as the PARTY PICKER makes it), or your own.
			List<Mon> team = new ArrayList<>();
			if (keepParty && !mine.party.isEmpty()) {
				for (Mon m : (mine.picking ? mine.ownParty : mine.party)) team.add(MonTag.load(MonTag.save(m)));
			} else {
				long seed = RNG.nextInt(1_000_000);
				team.addAll(com.insidethecube.retromon.engine.Campaign.make(com.insidethecube.retromon.engine.Campaign.pick(stage, starter, seed), stage, seed));
				for (Mon m : team) m.picked = false;
			}
			for (Mon m : team) {
				m.heal();
				nd.party.add(m);
				nd.seen.add(m.species.id());
				nd.caught.add(m.species.id());
			}
			String first = nd.party.isEmpty() ? starter : com.insidethecube.retromon.engine.Campaign.starterOf(nd.party.get(0).species.id());
			if (first == null) first = starter;
			nd.story.add("rival:" + switch (first) {
				case "bulbasaur" -> "charmander";
				case "squirtle" -> "bulbasaur";
				default -> "squirtle";
			});
			if (nd.hasDex()) {
				// At least the kinds the AIDEs ask for by then, if the step after says so.
				nd.story.add("townmap");
			}
		}
		// Healed at the last town's POKéMON CENTER (a black-out takes you back there).
		healAt(nd, stage);
		p.setAttached(Game.DATA, nd);
		Game.sync(p);
		go(p, nd, in ? step.in() : step.before());
		Retromon.LOG.info("Retromon: EVENT PICKER to step {} ({}), stage {}", step.n(), in ? "in it" : "before it", stage);
	}

	/** One of a step's "done" marks applied to a game. */
	static void apply(PlayerData d, String g) {
		int c = g.indexOf(':');
		String kind = c < 0 ? g : g.substring(0, c), v = c < 0 ? "" : g.substring(c + 1);
		switch (kind) {
			case "item" -> {
				String[] a = v.split(":");
				int k = a.length > 1 ? Integer.parseInt(a[1]) : 1;
				if (d.count(a[0]) < k) d.give(a[0], k - d.count(a[0]));
			}
			case "take" -> {
				while (d.count(v) > 0) d.take(v);
			}
			case "story" -> d.story.add(v);
			case "unstory" -> d.story.remove(v);
			case "static" -> d.statics.add(v);
			case "badge" -> d.badges.add(v);
			case "beat" -> {
				for (String b : v.split(",")) d.beaten.add(b);
			}
			case "visit" -> d.visited.add(v);
			case "caught" -> {
				int want = Integer.parseInt(v);
				for (Species s : Species.ALL.values()) {
					if (d.caught.size() >= want) break;
					if (s.dex() < 1 || s.dex() > 151 || s.id().equals("mew") || s.id().equals("mewtwo")) continue;
					d.caught.add(s.id());
					d.seen.add(s.id());
				}
			}
			default -> {
			}
		}
	}

	/** The POKéMON CENTER of the last town you'd have reached by then. */
	private static final String[] CENTERS = {"viridian_center", "pewter_city_center", "cerulean_city_center", "vermilion_city_center", "celadon_city_center",
			"fuchsia_city_center", "saffron_city_center", "cinnabar_island_center", "viridian_center", "viridian_center"};

	private static void healAt(PlayerData d, int stage) {
		Interiors.Door door = Interiors.DOORS.get(CENTERS[Math.min(stage, CENTERS.length - 1)]);
		Interiors.Room r = door == null ? null : Interiors.room(door.room());
		if (door == null || r == null) return;
		d.hasHeal = true;
		d.healInside = true;
		d.healDoor = door.key();
		d.healX = r.ox() + r.ex();
		d.healY = r.oy();
		d.healZ = r.oz() + r.ez() - 1.5;
	}

	/** Where a step's BEFORE or IN takes you: start, out:door, room:id[@x,z[,yaw]][|door], xz:x,z[,yaw]. */
	private static void go(ServerPlayer p, PlayerData d, String where) {
		try {
			if (where.equals("start")) {
				Game.toStart(p);
				return;
			}
			if (where.startsWith("out:")) {
				Interiors.Door door = Interiors.DOORS.get(where.substring(4));
				if (door != null) {
					d.door = door.key();
					if (door.place() != null) d.place = door.place();
					Interiors.out(p, door);
				}
				return;
			}
			if (where.startsWith("room:")) {
				String spec = where.substring(5), door = null;
				int bar = spec.indexOf('|');
				if (bar >= 0) {
					door = spec.substring(bar + 1);
					spec = spec.substring(0, bar);
				}
				String id = spec;
				double[] at = null;
				int atc = spec.indexOf('@');
				if (atc >= 0) {
					id = spec.substring(0, atc);
					String[] a = spec.substring(atc + 1).split(",");
					at = new double[] {Double.parseDouble(a[0]), Double.parseDouble(a[1]), a.length > 2 ? Double.parseDouble(a[2]) : 180};
				}
				Interiors.Room r = Interiors.room(id);
				if (r == null) return;
				if (door == null) for (Interiors.Door dd : Interiors.DOORS.values()) if (dd.room().equals(id) && dd.group() == null) {
					door = dd.key();
					break;
				}
				if (door != null) d.door = door;
				if (at != null) Interiors.go(p, r, at[0], at[1], (float) at[2], true);
				else Interiors.go(p, r, r.ex(), r.ez(), r.eyaw(), true);
				return;
			}
			if (where.startsWith("xz:")) {
				String[] a = where.substring(3).split(",");
				int x = (int) Math.floor(Double.parseDouble(a[0])), z = (int) Math.floor(Double.parseDouble(a[1]));
				float yaw = a.length > 2 ? Float.parseFloat(a[2]) : 180F;
				ServerLevel world = p.level().getServer().overworld();
				world.getChunk(x >> 4, z >> 4);
				BlockPos at = Game.spot(world, x, z, 4);
				if (at == null) at = Game.standable(world, x, z, 32);
				if (Interiors.in(p) || p.level() != world) Interiors.jumpOut(p, world, at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
				else p.teleportTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
				p.setYRot(yaw);
				p.setDeltaMovement(Vec3.ZERO);
			}
		} catch (RuntimeException e) {
			Retromon.LOG.error("Retromon: EVENT PICKER couldn't go to {}", where, e);
		}
	}

	/** Your own game back, and you where you were. */
	private static void back(ServerPlayer p, PlayerData d) {
		Game.clearAround(p);
		jjDrop(p);
		PlayerData mine = PlayerData.load(d.ownGame);
		mine.played = d.played;
		mine.textSpeed = d.textSpeed;
		double[] at = d.ownAt;
		String room = d.ownRoom;
		p.setAttached(Game.DATA, mine);
		flashOff(p);
		Game.sync(p);
		if (at == null) {
			Game.toStart(p);
			return;
		}
		Interiors.Room r = room == null || room.isEmpty() ? null : Interiors.room(room);
		if (r != null) Interiors.go(p, r, at[0], at[1], at[2], (float) at[3], true);
		else {
			ServerLevel world = p.level().getServer().overworld();
			if (Interiors.in(p) || p.level() != world) Interiors.jumpOut(p, world, at[0], at[1], at[2]);
			else p.teleportTo(at[0], at[1], at[2]);
			p.setYRot((float) at[3]);
		}
		Game.message(p, List.of("Your own game is back."));
	}
}
