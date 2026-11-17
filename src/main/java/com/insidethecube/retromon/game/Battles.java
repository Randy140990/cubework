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
		begin(p, new Session(b, null, staticKey), staticKey != null && legendary(foe.species) ? "gym_leader_battle" : "wild_battle");
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
		begin(p, new Session(b, t, null), music);
	}

	private static void begin(ServerPlayer p, Session s, String music) {
		ON.put(p.getUUID(), s);
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
				d.money += b.prize;
				if (s.trainer != null) {
					d.beaten.add(s.trainer.key());
					List<String> said = Game.fill(p, s.trainer.leader() ? s.trainer.defeat() : s.trainer.after());
					if (s.trainer.leader()) {
						after.addAll(said);
						if (d.badges.add(s.trainer.badge()) && after.stream().noneMatch(l -> l.contains("received the " + s.trainer.badge())))
							after.add(name(p) + " received the " + s.trainer.badge() + "!");
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
				d.money /= 2;
				d.healAll();
				Game.blackout(p);
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
