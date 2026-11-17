package com.insidethecube.kantojohto.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import com.insidethecube.kantojohto.engine.Battle;
import com.insidethecube.kantojohto.engine.Event;
import com.insidethecube.kantojohto.engine.Mon;
import com.insidethecube.kantojohto.engine.Species;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;

/** The battles going on: one per player at most, run by the engine here, played back on the player's screen. */
public final class Battles {
	private Battles() {}

	private record Session(Battle battle, Trainers.Trainer trainer) {}

	private static final Map<UUID, Session> ON = new HashMap<>();
	private static final Random RNG = new Random();

	public static boolean in(ServerPlayer p) {
		return ON.containsKey(p.getUUID());
	}

	public static void wild(ServerPlayer p, Mon foe) {
		PlayerData d = Game.data(p);
		d.seen.add(foe.species.id());
		Battle b = new Battle(RNG, p.getGameProfile().name().toUpperCase(), d.party, new ArrayList<>(List.of(foe)), Battle.Kind.WILD, null, 0);
		begin(p, new Session(b, null));
	}

	public static void trainer(ServerPlayer p, Trainers.Trainer t) {
		PlayerData d = Game.data(p);
		List<Mon> team = new ArrayList<>();
		for (Trainers.Member m : t.team()) {
			Mon mon = new Mon(Species.get(m.species()), m.level(), RNG);
			team.add(mon);
			d.seen.add(m.species());
		}
		Battle b = new Battle(RNG, p.getGameProfile().name().toUpperCase(), d.party, team, Battle.Kind.TRAINER, t.title(), t.prizeBase());
		begin(p, new Session(b, t));
	}

	private static void begin(ServerPlayer p, Session s) {
		ON.put(p.getUUID(), s);
		List<Event> ev = s.battle.start();
		Net.send(p, "battle_start", turn(s, ev));
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
		if (act == Battle.Act.BALL) {
			if (d.balls <= 0) return;
			if (s.battle.kind == Battle.Kind.WILD) d.balls--;
		}
		if (act == Battle.Act.POTION) {
			if (d.potions <= 0) return;
			d.potions--;
		}
		List<Event> ev = s.battle.choose(new Battle.Choice(act, index));
		if (ev.isEmpty()) return;
		CompoundTag out = turn(s, ev);
		if (s.battle.state == Battle.State.OVER) finish(p, s, out);
		Net.send(p, "battle_turn", out);
		Game.sync(p);
	}

	private static void finish(ServerPlayer p, Session s, CompoundTag out) {
		ON.remove(p.getUUID());
		PlayerData d = Game.data(p);
		Battle b = s.battle;
		List<String> after = new ArrayList<>();
		switch (b.result) {
			case WIN -> {
				if (s.trainer != null) {
					d.money += b.prize;
					d.beaten.add(s.trainer.key());
					after.addAll(s.trainer.after());
					if (s.trainer.badge() != null && d.badges.add(s.trainer.badge())) {
						after.add(p.getGameProfile().name().toUpperCase() + " received the " + s.trainer.badge() + "!");
					}
				}
			}
			case CAUGHT -> {
				Mon m = b.caught;
				m.status = m.status == Mon.Status.SLEEP ? Mon.Status.NONE : m.status;
				boolean toBox = d.party.size() >= 6;
				d.add(m);
				if (toBox) after.add(m.name() + " was sent to BILL's PC.");
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
		ListTag lines = new ListTag();
		for (String l : after) lines.add(net.minecraft.nbt.StringTag.valueOf(l));
		out.put("after", lines);
		Game.afterBattle(p);
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
		if (b.trainer != null) t.putString("trainer", b.trainer);
		return t;
	}

	public static void drop(ServerPlayer p) {
		ON.remove(p.getUUID());
	}
}
