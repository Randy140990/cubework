package com.insidethecube.retromon.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import com.insidethecube.retromon.engine.Battle;
import com.insidethecube.retromon.engine.Event;
import com.insidethecube.retromon.engine.Item;
import com.insidethecube.retromon.engine.Mon;
import com.insidethecube.retromon.engine.Places;
import com.insidethecube.retromon.engine.Species;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerPlayer;

/**
 * The battles going on: one per player at most, run by the engine here, played back on the player's screen;
 * then what follows (money, badges, a catch's nickname, evolving, new moves).
 */
public final class Battles {
	private Battles() {}

	private record Session(Battle battle, Trainers.Trainer trainer, String staticKey) {}

	private static final Map<UUID, Session> ON = new HashMap<>();
	private static final Random RNG = new Random();

	public static boolean in(ServerPlayer p) {
		return ON.containsKey(p.getUUID());
	}

	private static String name(ServerPlayer p) {
		String n = Game.data(p).name;
		return n.isEmpty() ? p.getGameProfile().name().toUpperCase() : n;
	}

	/** Which set piece the battle is fought on: a route's field, a forest, a cave, the water's edge, a gym or a town square. */
	private static String arena(ServerPlayer p, Session s) {
		// Inside a building (your rival in the lab): fought in the room itself.
		// (The forest, a room you walk about in, has the forest's set piece, as outside.)
		if (p.level().dimension() == Interiors.LEVEL && !Interiors.walkabout(p)) return "inside";
		String place = Game.data(p).place;
		if (s.trainer != null && s.trainer.boss() && !s.trainer.title().contains("ROCKET")) return "gym";
		if (place.contains("gym") || place.contains("plateau") || place.contains("room") || place.contains("lance") || place.contains("champion")) return "gym";
		if (place.contains("forest") || place.contains("ilex") || place.contains("park")) return "forest";
		for (String k : new String[] {"cave", "mt_moon", "tunnel", "seafoam", "victory_road", "rock", "mortar", "ice_path", "whirl", "ruins", "dragons_den",
				"silver_cave", "dark_cave", "union", "slowpoke", "diglett", "hideout", "mansion", "tower", "power_plant", "well"}) {
			if (place.contains(k)) return "cave";
		}
		net.minecraft.core.BlockPos at = p.blockPosition();
		for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++)
			if (!p.level().getFluidState(at.offset(dx, -1, dz)).isEmpty() || !p.level().getFluidState(at.offset(dx, 0, dz)).isEmpty()) return "water";
		if (place.contains("city") || place.contains("town") || place.contains("island")) return "town";
		return "route";
	}

	/** A wild battle (staticKey: the one-off encounter it is, or null). */
	public static void wild(ServerPlayer p, Mon foe, String staticKey) {
		PlayerData d = Game.data(p);
		d.seen.add(foe.species.id());
		Battle b = new Battle(RNG, name(p), d.party, new ArrayList<>(List.of(foe)), Battle.Kind.WILD, null, 0);
		// ((0.59.5) In Johto a legendary's is Crystal's legendary battle.)
		begin(p, new Session(b, null, staticKey), staticKey != null && legendary(foe.species) ? (johto(p) ? "johto_legend_battle" : "gym_leader_battle") : "wild_battle");
	}

	/** Whether you're in Johto (for Crystal's songs). */
	static boolean johto(ServerPlayer p) {
		Places.Place pl = Places.get(Game.data(p).place);
		return pl != null && "johto".equals(pl.region());
	}

	private static boolean legendary(Species s) {
		return switch (s.id()) {
			case "articuno", "zapdos", "moltres", "mewtwo", "mew", "lugia", "ho_oh", "raikou", "entei", "suicune", "celebi" -> true;
			default -> false;
		};
	}

	public static void trainer(ServerPlayer p, Trainers.Trainer t) {
		PlayerData d = Game.data(p);
		List<Mon> team = new ArrayList<>();
		for (Trainers.Member m : t.team()) {
			Mon mon = new Mon(Species.get(m.species()), m.level(), RNG);
			team.add(mon);
			d.seen.add(m.species());
		}
		Battle b = new Battle(RNG, name(p), d.party, team, Battle.Kind.TRAINER, t.title(), t.prizeBase());
		String music = t.title().startsWith("ELITE FOUR") || t.title().contains("CHAMPION") ? "final_battle" : t.boss() ? "gym_leader_battle" : "trainer_battle";
		// ((0.59.5) In Johto, Crystal's rival and Team Rocket battles; the rest swap themselves on the client.)
		if (johto(p) && !t.boss() && t.key().startsWith("rival")) music = "johto_rival_battle";
		else if (johto(p) && t.title().contains("ROCKET")) music = "johto_rocket_battle";
		begin(p, new Session(b, t, null), music);
	}

	private static void begin(ServerPlayer p, Session s, String music) {
		ON.put(p.getUUID(), s);
		s.battle.purse = Game.data(p).money;
		s.battle.badges = Game.data(p).badges.size();
		s.battle.lenient = s.trainer != null && (s.trainer.key().equals("rival_lab") || s.trainer.key().equals("rival_22") || s.trainer.key().equals("rocket_viridian"));
		List<Event> ev = s.battle.start();
		CompoundTag t = turn(s, ev);
		t.putString("music", music);
		t.putString("arena", arena(p, s));
		Net.send(p, "battle_start", t);
	}

	/** A choice from your battle screen: a move, a switch, an item or running. */
	public static void choose(ServerPlayer p, CompoundTag t) {
		Session s = ON.get(p.getUUID());
		if (s == null) return;
		PlayerData d = Game.data(p);
		Battle.Act act;
		try {
			act = Battle.Act.valueOf(t.getStringOr("act", "MOVE"));
		} catch (IllegalArgumentException e) {
			return;
		}
		int index = t.getIntOr("i", 0);
		String item = t.getStringOr("item", "");
		if (act == Battle.Act.BALL) {
			Item ball = Item.find(item.isEmpty() ? "poke_ball" : item);
			if (ball == null || !ball.ball() || d.count(ball.id()) <= 0) return;
			item = ball.id();
			if (s.battle.kind == Battle.Kind.WILD) d.take(item);
		}
		if (act == Battle.Act.ITEM) {
			Item it = Item.find(item);
			if (it == null || !it.battleUse() || it.ball() || d.count(item) <= 0) return;
			d.take(item);
		}
		List<Event> ev = s.battle.choose(new Battle.Choice(act, index, item.isEmpty() ? null : item));
		if (ev.isEmpty()) return;
		CompoundTag out = turn(s, ev);
		if (s.battle.state == Battle.State.OVER) finish(p, s, out);
		Net.send(p, "battle_turn", out);
		Game.sync(p);
		if (s.battle.state == Battle.State.OVER) followUps(p, s);
	}

	private static void finish(ServerPlayer p, Session s, CompoundTag out) {
		ON.remove(p.getUUID());
		PlayerData d = Game.data(p);
		Battle b = s.battle;
		List<String> after = new ArrayList<>();
		switch (b.result) {
			case WIN -> {
				d.money = Math.min(999999, d.money + b.prize);
				if (s.trainer != null && s.trainer.key().startsWith("rival_")) {
					d.beaten.add(s.trainer.key());
					// (0.60) Your rival beaten as the CHAMPION: the title's yours (the HALL OF FAME's door opens).
					if (s.trainer.key().equals("rival_champion")) d.story.add("champion");
					// The first one, in the lab: Oak sees to your Pokémon after.
					if (s.trainer.key().equals("rival_lab")) d.healAll();
					after.addAll(Game.fill(p, s.trainer.defeat()));
					// What he says after, he says to your face back in the world, before he heads off.
					Game.rivalSpeech(p, Game.fill(p, s.trainer.after()));
				} else if (s.trainer != null && s.trainer.key().equals("trainer_house")) {
					// The Trainer House: the prize (and the challenger's done for today).
					after.addAll(Game.trainerHouseWon(p));
				} else if (s.trainer != null) {
					d.beaten.add(s.trainer.key());
					// (0.60) The ROCKET HIDEOUT: the ROCKET on B4F drops the LIFT KEY; GIOVANNI leaves the SILPH SCOPE.
					if (s.trainer.key().equals("rocket_hideout_b4f_1") && d.count("lift_key") <= 0) {
						d.give("lift_key", 1);
						after.add("Oh no! I dropped the LIFT KEY!");
						after.add(name(p) + " found the LIFT KEY!");
					}
					if (s.trainer.key().equals("rocket_hideout_b4f_0") && d.count("silph_scope") <= 0) {
						d.give("silph_scope", 1);
						after.add(name(p) + " found the SILPH SCOPE!");
					}
					// (0.58) A CINNABAR GYM trainer beaten opens their room's gate.
					if (s.trainer.key().startsWith("cinnabar_gym_")) p.level().getServer().execute(() -> Game.roomGates(p));
					List<String> said = Game.fill(p, s.trainer.leader() ? s.trainer.defeat() : s.trainer.after());
					if (s.trainer.leader()) {
						after.addAll(said);
						if (d.badges.add(s.trainer.badge()) && after.stream().noneMatch(l -> l.contains("received the " + s.trainer.badge())))
							after.add(name(p) + " received the " + s.trainer.badge() + "!");
						String tm = Game.giveGift(p, s.trainer.place());
						if (tm != null) after.add(tm);
					} else if (s.trainer.key().equals("cerulean_city_0")) {
						// The ROCKET behind Cerulean's robbed house gives back the TM he stole (TM28, DIG).
						after.addAll(Game.fill(p, s.trainer.defeat()));
						after.addAll(said);
						String tm = Game.giveGift(p, "cerulean_city");
						if (tm != null) after.add(tm);
					}
				}
				if (s.staticKey != null) staticDone(p, s.staticKey);
			}
			case CAUGHT -> {
				Mon m = b.caught;
				boolean toBox = d.party.size() >= 6;
				d.add(m);
				if (toBox) after.add(m.name() + " was sent to BILL's PC.");
				if (s.staticKey != null) staticDone(p, s.staticKey);
				out.putString("nickname", toBox ? "box" : "party");
				out.putInt("nicknameIndex", toBox ? d.box.size() - 1 : d.party.size() - 1);
			}
			case LOSS -> {
				if (s.trainer != null && s.trainer.key().equals("rival_22")) {
					// Route 22, the first time: no harm done if you lose (as in the games), he goes on his way.
					d.beaten.add("rival_22");
					d.healAll();
					after.addAll(Game.fill(p, List.of("{rival}: What? Why do I have 2 POKéMON? You should catch some more too!",
							"{rival}: I heard POKéMON LEAGUE has many tough trainers! You should quit dawdling and get a move on!")));
				} else if (s.trainer != null && s.trainer.key().equals("rocket_viridian")) {
					// TEAM ROCKET in Viridian: they win, but have no time to take anything (they're wanted at MT. MOON).
					d.beaten.add("rocket_viridian");
					d.healAll();
					after.addAll(List.of("JESSIE: Ha! Too easy! Now, about that POKéMON of yours...", "JAMES: Jessie! The boss wants us at MT. MOON, right now!",
							"JESSIE: Ugh! Fine. Count yourself lucky, twerp!", "(Your POKéMON had a rest while they argued.)"));
				} else if (s.trainer != null && s.trainer.key().equals("rival_lab")) {
					// The first battle with your rival: no harm done if you lose (Oak sees to your Pokémon).
					d.beaten.add("rival_lab");
					d.healAll();
					after.addAll(Game.fill(p, List.of("{rival}: Yeah! Am I great or what?", "{rival}: Okay! I'll make my POKéMON fight to toughen it up!",
							"{rival}: And you'd better get out there and start your own adventure, {player}!",
							"{player}! Gramps! Smell ya later!", "OAK: Your POKéMON look tired. Here, let me heal them for you.")));
				} else {
					d.money = Math.max(0, d.money - b.lost);
					d.healAll();
					Game.blackout(p);
				}
			}
			default -> {
			}
		}
		out.putString("result", b.result.name());
		out.putBoolean("boss", s.trainer != null && s.trainer.boss());
		ListTag lines = new ListTag();
		for (String l : after) lines.add(StringTag.valueOf(l));
		out.put("after", lines);
		Game.afterBattle(p);
	}

	/** The one-off Pokémon's gone (beaten or caught): from your save, and from the world. */
	private static void staticDone(ServerPlayer p, String key) {
		Game.data(p).statics.add(key);
		for (WorldMon w : p.level().getEntitiesOfClass(WorldMon.class, p.getBoundingBox().inflate(32))) if (key.equals(w.key())) w.discard();
	}

	/** After the battle's text: the catch's nickname, then evolutions, then moves that need room (as the games order them). */
	private static void followUps(ServerPlayer p, Session s) {
		Battle b = s.battle;
		PlayerData d = Game.data(p);
		if (b.result == Battle.Result.CAUGHT) {
			boolean toBox = d.party.isEmpty() || !d.party.contains(b.caught);
			Game.askNickname(p, toBox ? "box" : "party", toBox ? d.box.indexOf(b.caught) : d.party.indexOf(b.caught));
		}
		if (b.result == Battle.Result.LOSS) return;
		for (Battle.Learn l : b.learns) Game.offerMove(p, l.index(), l.move());
		for (int i : b.levelled) Game.offerEvolution(p, i, "level", null);
	}

	/** What the screen needs after a turn: what happened, and where things stand now. */
	private static CompoundTag turn(Session s, List<Event> ev) {
		Battle b = s.battle;
		CompoundTag t = new CompoundTag();
		ListTag events = new ListTag();
		for (Event e : ev) {
			CompoundTag c = new CompoundTag();
			c.putInt("k", e.kind().ordinal());
			c.putInt("s", e.side());
			if (e.text() != null) c.putString("t", e.text());
			c.putInt("a", e.a());
			c.putInt("b", e.b());
			events.add(c);
		}
		t.put("events", events);
		t.put("mine", MonTag.saveAll(b.mine));
		t.put("theirs", MonTag.saveAll(b.theirs));
		t.putInt("active", b.active);
		t.putInt("enemy", b.enemy);
		t.putString("state", b.state.name());
		if (b.state == Battle.State.SHIFT && b.pendingFoe >= 0 && b.pendingFoe < b.theirs.size()) t.putString("next", b.theirs.get(b.pendingFoe).name());
		t.putString("kind", b.kind.name());
		t.putBoolean("locked", b.locked());
		t.putBoolean("canRun", b.canRun());
		if (b.trainer != null) t.putString("trainer", b.trainer);
		if (s.trainer != null) {
			t.putString("trainerKey", s.trainer.key());
			t.putString("sprite", s.trainer.sprite());
		}
		return t;
	}

	public static void drop(ServerPlayer p) {
		ON.remove(p.getUUID());
	}
}
