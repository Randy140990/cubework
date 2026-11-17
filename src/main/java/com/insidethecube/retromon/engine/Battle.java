package com.insidethecube.retromon.engine;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * A battle to Gold and Silver's rules: you against a wild Pokémon or a trainer's team, one Pokémon out on
 * each side, a turn at a time. Each choice you make plays out a turn and returns what happened, in order,
 * for the screen to play back. Side 0 is you, side 1 the other side.
 *
 * The moves follow their Gen 2 effects: stat changes, status, confusion, flinching, multi-hit, recoil,
 * draining, fixed damage, one-hit KOs, two-turn moves (Fly and Dig dodge), recharging, rampages, trapping,
 * weather, screens, Substitute, Protect, Leech Seed, Toxic, Curse, Perish Song and the rest; the few that
 * need things this game doesn't have yet (Transform, Baton Pass) fail.
 */
public class Battle {
	public enum Kind {
		WILD, TRAINER
	}

	public enum State {
		/** Waiting for what you'll do this turn. */
		CHOOSE,
		/** Your Pokémon fainted: waiting for which one to send out next. */
		SWITCH,
		OVER
	}

	public enum Result {
		NONE, WIN, LOSS, RAN, CAUGHT
	}

	public enum Act {
		/** index: the move slot (-1 to carry on with a move that takes more than one turn). */
		MOVE,
		/** index: the party slot. */
		SWITCH,
		/** item: the item; index: the party slot it's used on. */
		ITEM,
		/** item: the ball. */
		BALL, RUN
	}

	public record Choice(Act act, int index, String item) {
		public Choice(Act act, int index) {
			this(act, index, null);
		}
	}

	/** A move that wants to be learned when there's no room (you choose what to forget, after the battle). */
	public record Learn(int index, String move) {}

	private static final int[] STAT_MULT = {25, 28, 33, 40, 50, 66, 100, 150, 200, 250, 300, 350, 400};
	private static final int[] ACC_MULT = {33, 36, 43, 50, 60, 75, 100, 133, 166, 200, 233, 266, 300};
	private static final int[] CRIT = {17, 32, 64, 85, 128};
	static final int ATK = 0, DEF = 1, SPE = 2, SPA = 3, SPD = 4, ACC = 5, EVA = 6;
	private static final String[] STAT_NAMES = {"ATTACK", "DEFENSE", "SPEED", "SPCL.ATK", "SPCL.DEF", "accuracy", "evasiveness"};
	private static final Type[] HIDDEN_POWER_TYPES = {Type.FIGHTING, Type.FLYING, Type.POISON, Type.GROUND, Type.ROCK, Type.BUG, Type.GHOST, Type.STEEL,
			Type.FIRE, Type.WATER, Type.GRASS, Type.ELECTRIC, Type.PSYCHIC, Type.ICE, Type.DRAGON, Type.DARK};
	private static final int NO_WEATHER = 0, RAIN = 1, SUN = 2, SAND = 3;

	/** What's going on with one side's Pokémon (cleared when it's switched out). */
	private static final class Field {
		final int[] stages = new int[7];
		boolean seeded, flinched, focus, substitute, protecting, enduring, meanLook, lockOn, foresight, nightmare, cursed, destinyBond, minimized, curled,
				attracted, transformed;
		int subHp, protectCount, confusion, toxic, recharge, rampage, trap, trapDamage, disabledSlot = -1, disableTurns, perish, rollout, furyCutter,
				bideTurns, bideDamage;
		String charging, rampageMove, trapMove, lastMove, invulnerable;
		int chargeSlot = -1, rampageSlot = -1, lastDamage;
		boolean lastPhysical, tookHit;
		Type type1Override, type2Override;

		void clear() {
			java.util.Arrays.fill(stages, 0);
			seeded = flinched = focus = substitute = protecting = enduring = meanLook = lockOn = foresight = nightmare = cursed = destinyBond = minimized = curled = attracted = transformed = false;
			subHp = protectCount = confusion = recharge = rampage = trap = trapDamage = disableTurns = perish = rollout = furyCutter = bideTurns = bideDamage = 0;
			disabledSlot = -1;
			charging = rampageMove = trapMove = lastMove = invulnerable = null;
			chargeSlot = rampageSlot = -1;
			lastDamage = 0;
			type1Override = type2Override = null;
		}
	}

	private final Random rng;
	public final List<Mon> mine, theirs;
	public final Kind kind;
	/** The other trainer, as the games show it ("YOUNGSTER JOEY"), or null in the wild. */
	public final String trainer;
	/** What the other trainer pays per level of their last Pokémon. */
	public final int prizeBase;
	public final String playerName;
	public int active, enemy;
	public State state = State.CHOOSE;
	public Result result = Result.NONE;
	/** Money won (when you beat a trainer, and from Pay Day), and the Pokémon you caught. */
	public int prize, payDay;
	public Mon caught;
	/** Your Pokémon that went up a level (they may evolve after the battle), and moves waiting for room. */
	public final Set<Integer> levelled = new LinkedHashSet<>();
	public final List<Learn> learns = new ArrayList<>();
	/** Whether the wild Pokémon got away (Teleport, Roar) rather than being beaten. */
	public boolean fled;
	private final Field[] field = {new Field(), new Field()};
	private final int[] reflect = new int[2], lightScreen = new int[2], safeguard = new int[2], mist = new int[2], spikes = new int[2];
	private final int[] futureTurns = new int[2], futureDamage = new int[2];
	private int weather, weatherTurns;
	private int runAttempts;
	/** The move used last by either side (for Mirror Move): per side. */
	private final String[] lastUsed = new String[2];
	/** Which of your Pokémon fought the one that's out now (they share its EXP). */
	private final Set<Integer> participants = new LinkedHashSet<>();

	public Battle(Random rng, String playerName, List<Mon> mine, List<Mon> theirs, Kind kind, String trainer, int prizeBase) {
		this.rng = rng;
		this.playerName = playerName;
		this.mine = mine;
		this.theirs = theirs;
		this.kind = kind;
		this.trainer = trainer;
		this.prizeBase = prizeBase;
	}

	public Mon me() {
		return mine.get(active);
	}

	public Mon them() {
		return theirs.get(enemy);
	}

	private Mon side(int s) {
		return s == 0 ? me() : them();
	}

	private String who(int s) {
		if (s == 0) return me().name();
		return (kind == Kind.WILD ? "Wild " : "Enemy ") + them().name();
	}

	private static int firstHealthy(List<Mon> team) {
		for (int i = 0; i < team.size(); i++) if (!team.get(i).fainted()) return i;
		return -1;
	}

	private Type type1(int s) {
		return field[s].type1Override != null ? field[s].type1Override : side(s).species.type1();
	}

	private Type type2(int s) {
		return field[s].type1Override != null ? field[s].type2Override : side(s).species.type2();
	}

	private boolean isType(int s, Type t) {
		return type1(s) == t || type2(s) == t;
	}

	/** Whether your Pokémon is in the middle of a move that carries on (charging, recharging, rampaging): it can't choose. */
	public boolean locked() {
		Field f = field[0];
		return state == State.CHOOSE && (f.charging != null || f.recharge > 0 || f.rampage > 0 || f.bideTurns > 0);
	}

	/** Whether you can run (not from trainers, and not when you're trapped or held by Mean Look). */
	public boolean canRun() {
		return kind == Kind.WILD && field[0].trap <= 0 && !field[0].meanLook;
	}

	public List<Event> start() {
		List<Event> out = new ArrayList<>();
		active = Math.max(0, firstHealthy(mine));
		enemy = Math.max(0, firstHealthy(theirs));
		if (kind == Kind.WILD) {
			out.add(Event.sendOut(1, enemy, them().hp));
			out.add(Event.text("Wild " + them().name() + " appeared!"));
		} else {
			out.add(Event.text(trainer + " wants to battle!"));
			out.add(Event.sendOut(1, enemy, them().hp));
			out.add(Event.text(trainer + " sent out " + them().name() + "!"));
		}
		out.add(Event.sendOut(0, active, me().hp));
		out.add(Event.text("Go! " + me().name() + "!"));
		participants.add(active);
		return out;
	}

	// ---------- a turn ----------

	public List<Event> choose(Choice c) {
		List<Event> out = new ArrayList<>();
		if (state == State.OVER) return out;
		if (state == State.SWITCH) {
			if (c.act() != Act.SWITCH || c.index() < 0 || c.index() >= mine.size() || mine.get(c.index()).fainted()) return out;
			sendOut(0, c.index(), out);
			state = State.CHOOSE;
			return out;
		}
		for (Field f : field) {
			f.flinched = false;
			f.protecting = false;
			f.enduring = false;
			f.tookHit = false;
		}
		int enemyMove = enemyChoice();
		Act act = locked() ? Act.MOVE : c.act();
		int index = locked() ? -1 : c.index();
		switch (act) {
			case RUN -> {
				if (kind == Kind.TRAINER) {
					out.add(Event.text("No! There's no running from a trainer battle!"));
					return out;
				}
				if (!canRun()) {
					out.add(Event.text("Can't escape!"));
					return out;
				}
				runAttempts++;
				if (canEscape()) {
					out.add(Event.of(Event.Kind.ESCAPE, 0));
					out.add(Event.text("Got away safely!"));
					end(Result.RAN, out);
					return out;
				}
				out.add(Event.text("Can't escape!"));
				act(1, enemyMove, out);
			}
			case BALL -> {
				if (kind == Kind.TRAINER) {
					out.add(Event.text("The trainer blocked the BALL!"));
					out.add(Event.text("Don't be a thief!"));
					return out;
				}
				Item ball = Item.find(c.item() == null ? "poke_ball" : c.item());
				out.add(Event.text(playerName + " used " + (ball == null ? "POKé BALL" : ball.name()) + "!"));
				if (throwBall(ball, out)) return out;
				act(1, enemyMove, out);
			}
			case ITEM -> {
				Item it = Item.find(c.item());
				if (it == null) return out;
				Mon target = c.index() >= 0 && c.index() < mine.size() ? mine.get(c.index()) : me();
				out.add(Event.text(playerName + " used " + it.name() + "!"));
				if (it.statBoost() >= 0) stat(0, it.statBoost(), 1, out, true);
				else if (it.id().equals("dire_hit")) {
					field[0].focus = true;
					out.add(Event.text(me().name() + "'s getting pumped!"));
				} else if (it.id().equals("guard_spec")) {
					mist[0] = 5;
					out.add(Event.text(me().name() + "'s shrouded in MIST!"));
				} else {
					int before = target.hp;
					String result = it.use(target);
					if (target == me()) {
						out.add(Event.hp(0, target.hp, target.maxHp()));
						if (target.status == Mon.Status.NONE) {
							out.add(Event.of(Event.Kind.STATUS, 0));
							if (it.curesConfusion()) field[0].confusion = 0;
						}
					}
					out.add(Event.text(result != null ? result : target.hp > before ? target.name() + " recovered " + (target.hp - before) + " HP!" : "It had no effect."));
				}
				act(1, enemyMove, out);
			}
			case SWITCH -> {
				if (index == active || index < 0 || index >= mine.size() || mine.get(index).fainted()) return out;
				if (!canRun() && kind == Kind.WILD && field[0].trap > 0) {
					out.add(Event.text("Can't escape!"));
					return out;
				}
				out.add(Event.text(me().name() + ", come back!"));
				out.add(Event.of(Event.Kind.RECALL, 0));
				sendOut(0, index, out);
				act(1, enemyMove, out);
			}
			case MOVE -> {
				int mi = index;
				Mon m = me();
				if (mi >= 0 && (mi >= m.moves.size() || (m.pp.get(mi) <= 0 && anyPp(m)) || mi == field[0].disabledSlot)) return out;
				Move mine = chosenMove(0, mi);
				Move theirs = chosenMove(1, enemyMove);
				boolean meFirst = goesFirst(mine, theirs);
				if (meFirst) {
					act(0, mi, out);
					if (state != State.OVER && !them().fainted() && !me().fainted()) act(1, enemyMove, out);
				} else {
					act(1, enemyMove, out);
					if (state != State.OVER && !them().fainted() && !me().fainted()) act(0, mi, out);
				}
			}
		}
		if (state != State.OVER) endOfTurn(out);
		return out;
	}

	private static boolean anyPp(Mon m) {
		for (int p : m.pp) if (p > 0) return true;
		return false;
	}

	private Move chosenMove(int s, int slot) {
		Field f = field[s];
		if (f.charging != null) return Move.get(f.charging);
		if (f.rampageMove != null && f.rampage > 0) return Move.get(f.rampageMove);
		Mon m = side(s);
		if (slot < 0 || slot >= m.moves.size()) return Move.get("struggle");
		return Move.get(m.moves.get(slot));
	}

	private void sendOut(int s, int index, List<Event> out) {
		if (s == 0) {
			active = index;
			participants.add(active);
		} else {
			enemy = index;
		}
		Mon m = side(s);
		// Toxic turns back into ordinary poison when it comes out again.
		field[s].clear();
		out.add(Event.sendOut(s, index, m.hp));
		out.add(Event.text(s == 0 ? "Go! " + m.name() + "!" : trainer + " sent out " + m.name() + "!"));
		if (spikes[s] > 0 && !isType(s, Type.FLYING)) {
			int d = Math.max(1, m.maxHp() / 8);
			m.hp = Math.max(0, m.hp - d);
			out.add(Event.text(who(s) + "'s hurt by SPIKES!"));
			out.add(Event.of(Event.Kind.HIT, s));
			out.add(Event.hp(s, m.hp, m.maxHp()));
		}
	}

	private boolean goesFirst(Move mine, Move theirs) {
		Field me = field[0], them = field[1];
		int pa = me.recharge > 0 ? 1 : mine.priority(), pb = them.recharge > 0 ? 1 : theirs == null ? 1 : theirs.priority();
		if (pa != pb) return pa > pb;
		int sa = effSpeed(0), sb = effSpeed(1);
		if (sa != sb) return sa > sb;
		return rng.nextBoolean();
	}

	private int effSpeed(int s) {
		Mon m = side(s);
		int v = m.speed() * STAT_MULT[field[s].stages[SPE] + 6] / 100;
		if (m.status == Mon.Status.PARALYSIS) v /= 4;
		return Math.max(1, v);
	}

	/** Gold and Silver's escape check: always if you're faster, otherwise by speed and attempts so far. */
	private boolean canEscape() {
		int a = me().speed(), b = them().speed();
		if (a >= b) return true;
		int d = (b / 4) % 256;
		if (d == 0) return true;
		int f = a * 32 / d + 30 * (runAttempts - 1);
		return f > 255 || rng.nextInt(256) < f;
	}

	/** A Poké Ball: catches with the games' chance (better at low HP, asleep or frozen, and with better balls). */
	private boolean throwBall(Item ball, List<Event> out) {
		Mon t = them();
		int max = t.maxHp();
		double bonus = ball == null ? 1 : ball.catchBonus();
		boolean got;
		int passed;
		if (bonus >= 255) {
			got = true;
			passed = 4;
		} else {
			int rate = (int) Math.min(255, t.species.catchRate() * bonus);
			int a = Math.max(1, (3 * max - 2 * t.hp) * rate / (3 * max));
			if (t.status == Mon.Status.SLEEP || t.status == Mon.Status.FREEZE) a += 10;
			a = Math.min(255, a);
			// Four checks, the chance per check set so all four pass a/256 of the time; the shakes shown are the checks passed.
			double p = Math.pow((a + 1) / 256.0, 0.25);
			passed = 0;
			while (passed < 4 && rng.nextDouble() < p) passed++;
			got = passed == 4;
		}
		out.add(Event.ball(Math.min(3, passed)));
		if (got) {
			out.add(Event.of(Event.Kind.CAUGHT, 1));
			out.add(Event.text("Gotcha! " + t.name() + " was caught!"));
			caught = t;
			end(Result.CAUGHT, out);
			return true;
		}
		out.add(Event.text(passed == 0 ? "Oh no! The POKéMON broke free!" : passed == 1 ? "Aww! It appeared to be caught!" : "Shoot! It was so close too!"));
		return false;
	}

	/** Which move the other side uses: wild Pokémon pick at random; trainers lean to their strongest hit. */
	private int enemyChoice() {
		Mon t = them();
		List<Integer> usable = new ArrayList<>();
		for (int i = 0; i < t.moves.size(); i++) if (t.pp.get(i) > 0 && i != field[1].disabledSlot) usable.add(i);
		if (usable.isEmpty()) return -1;
		if (kind == Kind.TRAINER && rng.nextInt(10) < 7) {
			int best = usable.get(0);
			double bestScore = -1;
			for (int i : usable) {
				Move m = Move.get(t.moves.get(i));
				double score;
				if (m.status()) score = 10 + rng.nextInt(20);
				else score = Math.max(20, m.power()) * m.type().against(type1(0), type2(0)) * (stab(1, m) ? 1.5 : 1);
				if (score > bestScore) {
					bestScore = score;
					best = i;
				}
			}
			return best;
		}
		return usable.get(rng.nextInt(usable.size()));
	}

	private boolean stab(int s, Move mv) {
		return mv.type() == type1(s) || mv.type() == type2(s);
	}

	// ---------- using a move ----------

	/** One side's action for the turn: everything that can stop it, then the move. */
	private void act(int s, int slot, List<Event> out) {
		Mon att = side(s);
		Field f = field[s];
		if (att.fainted()) return;
		if (f.recharge > 0) {
			f.recharge = 0;
			out.add(Event.text(who(s) + " must recharge!"));
			return;
		}
		Move chosen = chosenMove(s, slot);
		boolean sleepMove = chosen.effect().equals("SNORE") || chosen.effect().equals("SLEEP_TALK");
		if (att.status == Mon.Status.SLEEP) {
			if (--att.sleepTurns <= 0) {
				att.status = Mon.Status.NONE;
				f.nightmare = false;
				out.add(Event.text(who(s) + " woke up!"));
				out.add(Event.of(Event.Kind.STATUS, s));
			} else {
				out.add(Event.text(who(s) + " is fast asleep!"));
				if (!sleepMove) {
					f.charging = null;
					f.rampage = 0;
					return;
				}
			}
		} else if (sleepMove) {
			usePp(s, slot);
			out.add(Event.text(who(s) + " used " + chosen.name() + "!"));
			out.add(Event.text("But it failed!"));
			return;
		}
		if (att.status == Mon.Status.FREEZE) {
			if (chosen.effect().equals("FLAME_WHEEL") || chosen.effect().equals("SACRED_FIRE")) {
				att.status = Mon.Status.NONE;
				out.add(Event.text(who(s) + " was defrosted!"));
				out.add(Event.of(Event.Kind.STATUS, s));
			} else {
				out.add(Event.text(who(s) + " is frozen solid!"));
				return;
			}
		}
		if (f.flinched) {
			out.add(Event.text(who(s) + " flinched!"));
			f.rampage = 0;
			return;
		}
		if (slot >= 0 && slot == f.disabledSlot && f.charging == null) {
			out.add(Event.text(who(s) + "'s " + chosen.name() + " is DISABLED!"));
			return;
		}
		if (f.confusion > 0) {
			if (--f.confusion <= 0) {
				out.add(Event.text(who(s) + "'s confused no more!"));
			} else {
				out.add(Event.text(who(s) + " is confused!"));
				if (rng.nextBoolean()) {
					int d = confusionDamage(s);
					hurt(s, d, out);
					out.add(Event.text("It hurt itself in its confusion!"));
					f.charging = null;
					f.rampage = 0;
					return;
				}
			}
		}
		if (f.attracted) {
			out.add(Event.text(who(s) + " is in love with " + who(1 - s) + "!"));
			if (rng.nextBoolean()) {
				out.add(Event.text(who(s) + " is immobilized by love!"));
				return;
			}
		}
		if (att.status == Mon.Status.PARALYSIS && rng.nextInt(4) == 0) {
			out.add(Event.text(who(s) + " is fully paralyzed!"));
			f.charging = null;
			f.rampage = 0;
			return;
		}
		// Bide stores up, then hits back double.
		if (f.bideTurns > 0) {
			if (--f.bideTurns > 0) {
				out.add(Event.text(who(s) + " is storing energy!"));
				return;
			}
			out.add(Event.text(who(s) + " unleashed energy!"));
			int d = f.bideDamage * 2;
			f.bideDamage = 0;
			if (d <= 0) out.add(Event.text("But it failed!"));
			else directHit(s, d, out);
			return;
		}
		boolean continuing = f.charging != null || (f.rampage > 0 && f.rampageMove != null);
		if (!continuing) usePp(s, slot);
		use(s, chosen, slot, continuing, out);
	}

	private void usePp(int s, int slot) {
		Mon m = side(s);
		if (slot >= 0 && slot < m.pp.size()) m.pp.set(slot, Math.max(0, m.pp.get(slot) - 1));
	}

	private int confusionDamage(int s) {
		Mon m = side(s);
		int a = m.attack() * STAT_MULT[field[s].stages[ATK] + 6] / 100, d = Math.max(1, m.defense() * STAT_MULT[field[s].stages[DEF] + 6] / 100);
		return Math.max(1, ((m.level * 2 / 5 + 2) * 40 * a / d) / 50 + 2);
	}

	/** Damage straight to one side's Pokémon (its HP and the screen), through a Substitute if it has one. */
	private void hurt(int s, int d, List<Event> out) {
		Mon m = side(s);
		m.hp = Math.max(0, m.hp - d);
		out.add(Event.of(Event.Kind.HIT, s));
		out.add(Event.hp(s, m.hp, m.maxHp()));
	}

	private void heal(int s, int amount, List<Event> out) {
		Mon m = side(s);
		m.hp = Math.min(m.maxHp(), m.hp + amount);
		out.add(Event.hp(s, m.hp, m.maxHp()));
	}

	/** Fixed damage from s to the other side (Bide, Counter, Seismic Toss...), through a Substitute. */
	private void directHit(int s, int d, List<Event> out) {
		int t = 1 - s;
		if (field[t].substitute) {
			hitSubstitute(t, d, out);
			return;
		}
		Mon def = side(t);
		if (field[t].enduring && d >= def.hp) {
			d = def.hp - 1;
			out.add(Event.text(who(t) + " endured the hit!"));
		}
		hurt(t, d, out);
		record(t, d, true);
	}

	private void hitSubstitute(int t, int d, List<Event> out) {
		Field f = field[t];
		f.subHp -= d;
		out.add(Event.text("The SUBSTITUTE took damage for " + who(t) + "!"));
		if (f.subHp <= 0) {
			f.substitute = false;
			out.add(Event.text(who(t) + "'s SUBSTITUTE faded!"));
		}
	}

	private void record(int t, int d, boolean physical) {
		Field f = field[t];
		f.lastDamage = d;
		f.lastPhysical = physical;
		f.tookHit = true;
		if (f.bideTurns > 0) f.bideDamage += d;
	}

	/** The move itself: charging, protection, hitting or missing, damage and what it does besides. */
	private void use(int s, Move m, int slot, boolean continuing, List<Event> out) {
		int t = 1 - s;
		Mon att = side(s), def = side(t);
		Field f = field[s], g = field[t];
		String e = m.effect();
		// Moves that call another move.
		if (e.equals("METRONOME") || e.equals("MIRROR_MOVE") || e.equals("SLEEP_TALK")) {
			out.add(Event.text(who(s) + " used " + m.name() + "!"));
			Move other = null;
			if (e.equals("METRONOME")) {
				List<Move> all = new ArrayList<>(Move.ALL.values());
				do other = all.get(rng.nextInt(all.size()));
				while (other.effect().equals("METRONOME") || other.id().equals("struggle") || other.effect().equals("SLEEP_TALK")
						|| other.effect().equals("MIRROR_MOVE") || other.effect().equals("PROTECT") || other.effect().equals("ENDURE"));
			} else if (e.equals("MIRROR_MOVE")) {
				other = lastUsed[t] == null ? null : Move.find(lastUsed[t]);
			} else {
				List<String> others = new ArrayList<>();
				for (String id : att.moves) if (!Move.get(id).effect().equals("SLEEP_TALK")) others.add(id);
				if (!others.isEmpty()) other = Move.get(others.get(rng.nextInt(others.size())));
			}
			if (other == null || other.effect().equals("MIRROR_MOVE")) {
				out.add(Event.text("But it failed!"));
				return;
			}
			use(s, other, -1, false, out);
			return;
		}
		if (!continuing || f.charging == null) {
			out.add(Event.text(who(s) + " used " + m.name() + "!"));
		}
		lastUsed[s] = m.id();
		f.lastMove = m.id();
		// Two-turn moves: the first turn charges (Fly and Dig go up or under).
		if (f.charging == null && isTwoTurn(e) && !(e.equals("SOLARBEAM") && weather == SUN)) {
			f.charging = m.id();
			f.chargeSlot = slot;
			switch (e) {
				case "RAZOR_WIND" -> out.add(Event.text(who(s) + " made a whirlwind!"));
				case "SOLARBEAM" -> out.add(Event.text(who(s) + " took in sunlight!"));
				case "SKY_ATTACK" -> out.add(Event.text(who(s) + " is glowing!"));
				case "SKULL_BASH" -> {
					out.add(Event.text(who(s) + " lowered its head!"));
					stat(s, DEF, 1, out, true);
				}
				case "FLY" -> {
					f.invulnerable = m.id().equals("dig") ? "dig" : "fly";
					out.add(Event.of(Event.Kind.RECALL, s));
					out.add(Event.text(who(s) + (f.invulnerable.equals("dig") ? " dug a hole!" : " flew up high!")));
				}
				default -> out.add(Event.text(who(s) + " is charging up!"));
			}
			return;
		}
		boolean wasHidden = f.invulnerable != null;
		f.charging = null;
		f.chargeSlot = -1;
		f.invulnerable = null;
		if (wasHidden) out.add(Event.sendOut(s, s == 0 ? active : enemy, att.hp));
		out.add(new Event(Event.Kind.ATTACK, s, m.id(), 0, 0));
		boolean targetsFoe = targetsFoe(m);
		// Protect, Fly and Dig.
		if (targetsFoe && g.protecting) {
			out.add(Event.text(who(t) + " is protecting itself!"));
			if (e.equals("JUMP_KICK")) crash(s, m, out);
			return;
		}
		if (targetsFoe && g.invulnerable != null && !hitsHidden(m, g.invulnerable)) {
			out.add(Event.text(who(s) + "'s attack missed!"));
			if (e.equals("JUMP_KICK")) crash(s, m, out);
			return;
		}
		// Does it hit?
		if (targetsFoe && !alwaysHits(m, s)) {
			int acc = m.accuracy() * 255 / 100;
			if (e.equals("OHKO")) {
				if (def.level > att.level) {
					out.add(Event.text(who(t) + " is unaffected!"));
					return;
				}
				acc = Math.min(255, acc + (att.level - def.level) * 2);
			} else {
				int st = Math.max(-6, Math.min(6, f.stages[ACC] - (g.foresight ? Math.min(0, g.stages[EVA]) : g.stages[EVA])));
				acc = Math.min(255, acc * ACC_MULT[st + 6] / 100);
			}
			if (e.equals("THUNDER") && weather == RAIN) acc = 255;
			if (e.equals("THUNDER") && weather == SUN) acc = acc / 2;
			if (rng.nextInt(256) >= acc) {
				out.add(Event.text(who(s) + "'s attack missed!"));
				f.rampage = 0;
				f.furyCutter = 0;
				f.rollout = 0;
				if (e.equals("JUMP_KICK")) crash(s, m, out);
				return;
			}
		}
		f.lockOn = false;
		if (m.damaging()) hit(s, m, out);
		else effect(s, m, out);
	}

	private static boolean isTwoTurn(String e) {
		return e.equals("RAZOR_WIND") || e.equals("SOLARBEAM") || e.equals("SKY_ATTACK") || e.equals("SKULL_BASH") || e.equals("FLY");
	}

	private static boolean hitsHidden(Move m, String how) {
		if (how.equals("fly")) return m.id().equals("gust") || m.id().equals("twister") || m.id().equals("thunder") || m.id().equals("whirlwind");
		return m.id().equals("earthquake") || m.id().equals("magnitude");
	}

	private boolean alwaysHits(Move m, int s) {
		if (field[s].lockOn) return true;
		return m.effect().equals("ALWAYS_HIT") || m.accuracy() >= 100 && m.effect().equals("ALWAYS_HIT") || !targetsFoe(m);
	}

	/** Moves aimed at the other Pokémon (rather than at the user or the field). */
	private static boolean targetsFoe(Move m) {
		if (m.damaging()) return true;
		return switch (m.effect()) {
			case "ATTACK_UP", "ATTACK_UP_2", "DEFENSE_UP", "DEFENSE_UP_2", "SPEED_UP_2", "SP_ATK_UP", "SP_DEF_UP_2", "EVASION_UP", "DEFENSE_CURL", "HEAL",
					"MORNING_SUN", "SYNTHESIS", "MOONLIGHT", "FOCUS_ENERGY", "MIST", "REFLECT", "LIGHT_SCREEN", "SAFEGUARD", "SUBSTITUTE", "PROTECT", "ENDURE",
					"BELLY_DRUM", "SPLASH", "TELEPORT", "RAIN_DANCE", "SUNNY_DAY", "SANDSTORM", "HEAL_BELL", "BATON_PASS", "CONVERSION", "RESET_STATS", "BIDE",
					"SPIKES", "DESTINY_BOND", "PERISH_SONG" -> false;
			case "CURSE" -> false;
			default -> true;
		};
	}

	private void crash(int s, Move m, List<Event> out) {
		int d = Math.max(1, damageOf(s, m, false, m.type().against(type1(1 - s), type2(1 - s)), m.power()) / 8);
		out.add(Event.text(who(s) + " kept going and crashed!"));
		hurt(s, d, out);
	}

	// ---------- damaging moves ----------

	private void hit(int s, Move m, List<Event> out) {
		int t = 1 - s;
		Mon att = side(s), def = side(t);
		Field f = field[s], g = field[t];
		String e = m.effect();
		Type type = m.type();
		int power = m.power();
		if (e.equals("HIDDEN_POWER")) {
			type = HIDDEN_POWER_TYPES[((att.dvAtk & 3) << 2) | (att.dvDef & 3)];
			int x = ((att.dvAtk >> 3) << 3) | ((att.dvDef >> 3) << 2) | ((att.dvSpe >> 3) << 1) | (att.dvSpc >> 3);
			power = (5 * x + (att.dvSpc & 3)) / 2 + 31;
		}
		double eff = type.against(type1(t), type2(t));
		if (eff == 0 && g.foresight && type1(t) == Type.GHOST || eff == 0 && g.foresight && type2(t) == Type.GHOST) {
			if (type == Type.NORMAL || type == Type.FIGHTING) eff = Type.NORMAL.against(type1(t) == Type.GHOST ? type2(t) : type1(t), null);
		}
		// Moves whose damage isn't worked out the usual way.
		switch (e) {
			case "STATIC_DAMAGE" -> {
				if (eff == 0) {
					noEffect(t, out);
					return;
				}
				directHit(s, power, out);
				afterHit(s, m, 0, out);
				return;
			}
			case "LEVEL_DAMAGE" -> {
				if (eff == 0) {
					noEffect(t, out);
					return;
				}
				directHit(s, att.level, out);
				return;
			}
			case "PSYWAVE" -> {
				directHit(s, Math.max(1, rng.nextInt(Math.max(1, att.level * 3 / 2)) + 1), out);
				return;
			}
			case "SUPER_FANG" -> {
				if (eff == 0) {
					noEffect(t, out);
					return;
				}
				directHit(s, Math.max(1, def.hp / 2), out);
				return;
			}
			case "OHKO" -> {
				if (eff == 0) {
					noEffect(t, out);
					return;
				}
				directHit(s, def.hp, out);
				out.add(Event.text("It's a one-hit KO!"));
				return;
			}
			case "COUNTER", "MIRROR_COAT" -> {
				boolean wantPhysical = e.equals("COUNTER");
				if (!f.tookHit || f.lastDamage <= 0 || f.lastPhysical != wantPhysical) {
					out.add(Event.text("But it failed!"));
					return;
				}
				directHit(s, f.lastDamage * 2, out);
				return;
			}
			case "DREAM_EATER" -> {
				if (def.status != Mon.Status.SLEEP) {
					out.add(Event.text("But it failed!"));
					return;
				}
			}
			case "SNORE" -> {
				if (att.status != Mon.Status.SLEEP) {
					out.add(Event.text("But it failed!"));
					return;
				}
			}
			case "REVERSAL" -> {
				int p = att.hp * 48 / Math.max(1, att.maxHp());
				power = p <= 1 ? 200 : p <= 4 ? 150 : p <= 9 ? 100 : p <= 16 ? 80 : p <= 32 ? 40 : 20;
			}
			case "MAGNITUDE" -> {
				int r = rng.nextInt(100);
				int mag = r < 5 ? 4 : r < 15 ? 5 : r < 35 ? 6 : r < 65 ? 7 : r < 85 ? 8 : r < 95 ? 9 : 10;
				power = new int[] {10, 30, 50, 70, 90, 110, 150}[mag - 4];
				out.add(Event.text("Magnitude " + mag + "!"));
			}
			case "PRESENT" -> {
				int r = rng.nextInt(256);
				if (r < 52) {
					heal(t, 80, out);
					out.add(Event.text(who(t) + " regained health!"));
					return;
				}
				power = r < 154 ? 40 : r < 232 ? 80 : 120;
			}
			case "RETURN" -> power = Math.max(1, att.friendship * 10 / 25);
			case "FRUSTRATION" -> power = Math.max(1, (255 - att.friendship) * 10 / 25);
			case "ROLLOUT" -> {
				power = power << Math.min(4, f.rollout + (f.curled ? 1 : 0));
				f.rollout = (f.rollout + 1) % 5;
				f.rampageMove = f.rollout == 0 ? null : m.id();
				f.rampage = f.rollout == 0 ? 0 : 1;
			}
			case "FURY_CUTTER" -> {
				power = power << Math.min(4, f.furyCutter);
				f.furyCutter++;
			}
			case "FUTURE_SIGHT" -> {
				if (futureTurns[t] > 0) {
					out.add(Event.text("But it failed!"));
					return;
				}
				futureTurns[t] = 3;
				futureDamage[t] = damageOf(s, m, false, 1, power);
				out.add(Event.text(who(s) + " foresaw an attack!"));
				return;
			}
			case "STOMP" -> {
				if (g.minimized) power *= 2;
			}
			case "GUST", "TWISTER" -> {
				if ("fly".equals(g.invulnerable)) power *= 2;
			}
			case "EARTHQUAKE" -> {
				if ("dig".equals(g.invulnerable)) power *= 2;
			}
			case "BIDE" -> {
				f.bideTurns = 2 + rng.nextInt(2);
				f.bideDamage = 0;
				out.add(Event.text(who(s) + " is storing energy!"));
				return;
			}
			default -> {
			}
		}
		if (eff == 0) {
			noEffect(t, out);
			f.rampage = 0;
			return;
		}
		if (e.equals("SOLARBEAM") && (weather == RAIN || weather == SAND)) power /= 2;
		int hits = switch (e) {
			case "MULTI_HIT" -> {
				int r = rng.nextInt(8);
				yield r < 3 ? 2 : r < 6 ? 3 : r < 7 ? 4 : 5;
			}
			case "DOUBLE_HIT", "POISON_MULTI_HIT" -> 2;
			case "TRIPLE_KICK" -> 1 + rng.nextInt(3);
			default -> 1;
		};
		int total = 0;
		boolean anyCrit = false;
		for (int i = 0; i < hits && !def.fainted(); i++) {
			int p = e.equals("TRIPLE_KICK") ? power * (i + 1) : power;
			int stage = Math.min(4, (f.focus ? 1 : 0) + (m.highCrit() ? 2 : 0));
			boolean crit = !e.equals("FUTURE_SIGHT") && rng.nextInt(256) < CRIT[stage];
			int dmg = damageOf(s, m, crit, eff, p, type);
			if (e.equals("FALSE_SWIPE") && dmg >= def.hp) dmg = def.hp - 1;
			if (g.substitute) {
				hitSubstitute(t, dmg, out);
				total += dmg;
				continue;
			}
			if (g.enduring && dmg >= def.hp) {
				dmg = def.hp - 1;
				out.add(Event.text(who(t) + " endured the hit!"));
			}
			dmg = Math.min(dmg, def.hp);
			hurt(t, dmg, out);
			record(t, dmg, !type.special());
			total += dmg;
			if (crit) {
				anyCrit = true;
				out.add(Event.text("A critical hit!"));
			}
		}
		if (hits > 1) out.add(Event.text("Hit " + hits + " times!"));
		if (eff > 1) out.add(Event.text("It's super effective!"));
		else if (eff < 1) out.add(Event.text("It's not very effective..."));
		afterHit(s, m, total, out);
		if (anyCrit && def.fainted()) {
			// (Nothing extra: kept for the screen's sake.)
		}
	}

	private void noEffect(int t, List<Event> out) {
		out.add(Event.text("It doesn't affect " + who(t) + "..."));
	}

	/** The usual damage, Gen 2's formula. */
	private int damageOf(int s, Move m, boolean crit, double eff, int power) {
		return damageOf(s, m, crit, eff, power, m.type());
	}

	private int damageOf(int s, Move m, boolean crit, double eff, int power, Type type) {
		int t = 1 - s;
		Mon att = side(s), def = side(t);
		boolean sp = type.special();
		int ai = sp ? SPA : ATK, di = sp ? SPD : DEF;
		int atkStage = field[s].stages[ai], defStage = field[t].stages[di];
		// A critical hit ignores your own drops and their raises (and their screens).
		if (crit) {
			atkStage = Math.max(0, atkStage);
			defStage = Math.min(0, defStage);
		}
		int a = (sp ? att.spAttack() : att.attack()) * STAT_MULT[atkStage + 6] / 100;
		int d = (sp ? def.spDefense() : def.defense()) * STAT_MULT[defStage + 6] / 100;
		if (!sp && att.status == Mon.Status.BURN) a /= 2;
		if (!crit && ((!sp && reflect[t] > 0) || (sp && lightScreen[t] > 0))) d *= 2;
		if (m.effect().equals("SELFDESTRUCT")) d = Math.max(1, d / 2);
		if (a > 255 || d > 255) {
			a = Math.max(1, a / 4);
			d = Math.max(1, d / 4);
		}
		d = Math.max(1, d);
		int lv = att.level * 2 / 5 + 2;
		int dmg = lv * power * a / d / 50;
		if (crit) dmg *= 2;
		dmg = Math.min(997, dmg) + 2;
		if (weather == RAIN) {
			if (type == Type.WATER) dmg = dmg * 3 / 2;
			else if (type == Type.FIRE) dmg /= 2;
		} else if (weather == SUN) {
			if (type == Type.FIRE) dmg = dmg * 3 / 2;
			else if (type == Type.WATER) dmg /= 2;
		}
		if (type == type1(s) || type == type2(s)) dmg = dmg * 3 / 2;
		dmg = (int) (dmg * eff);
		if (dmg > 1) dmg = dmg * (217 + rng.nextInt(39)) / 255;
		return Math.max(1, dmg);
	}

	/** What a damaging move does after it hits: draining, recoil, side effects, recharging, fainting itself. */
	private void afterHit(int s, Move m, int dealt, List<Event> out) {
		int t = 1 - s;
		Mon att = side(s), def = side(t);
		Field f = field[s], g = field[t];
		String e = m.effect();
		switch (e) {
			case "LEECH_HIT", "DREAM_EATER" -> {
				if (dealt > 0) {
					heal(s, Math.max(1, dealt / 2), out);
					out.add(Event.text("Sucked health from " + who(t) + "!"));
				}
			}
			case "RECOIL_HIT" -> {
				int r = m.id().equals("struggle") ? Math.max(1, dealt / 2) : Math.max(1, dealt / 4);
				out.add(Event.text(who(s) + "'s hit with recoil!"));
				hurt(s, r, out);
			}
			case "SELFDESTRUCT" -> {
				att.hp = 0;
				out.add(Event.hp(s, 0, att.maxHp()));
			}
			case "HYPER_BEAM" -> f.recharge = 1;
			case "RAMPAGE" -> {
				if (f.rampage <= 0) {
					f.rampage = 2 + rng.nextInt(2);
					f.rampageMove = m.id();
				}
				if (--f.rampage <= 0) {
					f.rampageMove = null;
					if (f.confusion <= 0) {
						f.confusion = 2 + rng.nextInt(4);
						out.add(Event.text(who(s) + " became confused due to fatigue!"));
					}
				}
			}
			case "TRAP_TARGET" -> {
				if (!def.fainted() && g.trap <= 0 && !g.substitute) {
					g.trap = 2 + rng.nextInt(4);
					g.trapMove = m.name();
					g.trapDamage = Math.max(1, def.maxHp() / 16);
					out.add(Event.text(who(t) + " was trapped!"));
				}
			}
			case "PAY_DAY" -> {
				payDay += att.level * 2;
				out.add(Event.text("Coins scattered everywhere!"));
			}
			case "RAPID_SPIN" -> {
				f.seeded = false;
				f.trap = 0;
				spikes[s] = 0;
			}
			case "THIEF", "PURSUIT", "BEAT_UP", "RAGE" -> {
			}
			default -> {
			}
		}
		if (def.fainted() || g.substitute) {
			if (g.destinyBond && def.fainted() && !att.fainted()) {
				att.hp = 0;
				out.add(Event.text(who(t) + " took its attacker down with it!"));
				out.add(Event.hp(s, 0, att.maxHp()));
			}
			return;
		}
		if (m.chance() > 0 && rng.nextInt(100) >= m.chance()) return;
		switch (e) {
			case "POISON_HIT", "POISON_MULTI_HIT" -> inflict(t, Mon.Status.POISON, out, false);
			case "BURN_HIT", "FLAME_WHEEL", "SACRED_FIRE" -> inflict(t, Mon.Status.BURN, out, false);
			case "FREEZE_HIT" -> {
				if (weather != SUN) inflict(t, Mon.Status.FREEZE, out, false);
			}
			case "PARALYZE_HIT", "THUNDER" -> inflict(t, Mon.Status.PARALYSIS, out, false);
			case "TRI_ATTACK" -> inflict(t, new Mon.Status[] {Mon.Status.BURN, Mon.Status.FREEZE, Mon.Status.PARALYSIS}[rng.nextInt(3)], out, false);
			case "CONFUSE_HIT" -> confuse(t, out, false);
			case "FLINCH_HIT", "STOMP", "TWISTER", "SNORE" -> g.flinched = true;
			case "SPEED_DOWN_HIT" -> stat(t, SPE, -1, out, false);
			case "ATTACK_DOWN_HIT" -> stat(t, ATK, -1, out, false);
			case "DEFENSE_DOWN_HIT" -> stat(t, DEF, -1, out, false);
			case "SP_DEF_DOWN_HIT" -> stat(t, SPD, -1, out, false);
			case "ACCURACY_DOWN_HIT" -> stat(t, ACC, -1, out, false);
			case "DEFENSE_UP_HIT" -> stat(s, DEF, 1, out, true);
			case "ATTACK_UP_HIT" -> stat(s, ATK, 1, out, true);
			case "ALL_UP_HIT" -> {
				for (int st : new int[] {ATK, DEF, SPE, SPA, SPD}) stat(s, st, 1, out, true);
			}
			default -> {
			}
		}
	}

	// ---------- status and other effects ----------

	private boolean immuneTo(int t, Mon.Status st) {
		return switch (st) {
			case BURN -> isType(t, Type.FIRE);
			case FREEZE -> isType(t, Type.ICE);
			case POISON -> isType(t, Type.POISON) || isType(t, Type.STEEL);
			default -> false;
		};
	}

	private boolean inflict(int t, Mon.Status st, List<Event> out, boolean fromStatusMove) {
		Mon def = side(t);
		if (def.status != Mon.Status.NONE || immuneTo(t, st) || (safeguard[t] > 0) || (fromStatusMove && field[t].substitute)) {
			if (fromStatusMove) out.add(Event.text(immuneTo(t, st) ? "It doesn't affect " + who(t) + "..." : "But it failed!"));
			return false;
		}
		def.status = st;
		if (st == Mon.Status.SLEEP) def.sleepTurns = 1 + rng.nextInt(6);
		out.add(Event.of(Event.Kind.STATUS, t));
		out.add(Event.text(who(t) + switch (st) {
			case BURN -> " was burned!";
			case FREEZE -> " was frozen solid!";
			case POISON -> " was poisoned!";
			case PARALYSIS -> " is paralyzed! It may not attack!";
			case SLEEP -> " fell asleep!";
			default -> "";
		}));
		return true;
	}

	private void confuse(int t, List<Event> out, boolean fromStatusMove) {
		Field g = field[t];
		if (g.confusion > 0 || safeguard[t] > 0 || (fromStatusMove && g.substitute)) {
			if (fromStatusMove) out.add(Event.text(g.confusion > 0 ? who(t) + "'s already confused!" : "But it failed!"));
			return;
		}
		g.confusion = 2 + rng.nextInt(4);
		out.add(Event.text(who(t) + " became confused!"));
	}

	/** A status move (power 0): what it does. */
	private void effect(int s, Move m, List<Event> out) {
		int t = 1 - s;
		Mon att = side(s), def = side(t);
		Field f = field[s], g = field[t];
		String e = m.effect();
		// Status moves don't work through a Substitute, and some types are immune (Thunder Wave and Ground).
		if (targetsFoe(m) && g.substitute && !e.equals("ROAR") && !e.equals("FORCE_SWITCH") && !e.equals("PERISH_SONG") && !e.equals("ATTRACT")) {
			out.add(Event.text("But it failed!"));
			return;
		}
		if (targetsFoe(m) && m.type().against(type1(t), type2(t)) == 0 && (e.equals("PARALYZE") || e.equals("POISON") || e.equals("TOXIC"))) {
			noEffect(t, out);
			return;
		}
		switch (e) {
			case "SLEEP" -> inflict(t, Mon.Status.SLEEP, out, true);
			case "POISON" -> inflict(t, Mon.Status.POISON, out, true);
			case "TOXIC" -> {
				if (inflict(t, Mon.Status.POISON, out, true)) {
					g.toxic = 1;
					out.set(out.size() - 1, Event.text(who(t) + "'s badly poisoned!"));
				}
			}
			case "PARALYZE" -> inflict(t, Mon.Status.PARALYSIS, out, true);
			case "CONFUSE" -> confuse(t, out, true);
			case "SWAGGER" -> {
				stat(t, ATK, 2, out, false);
				confuse(t, out, true);
			}
			case "ATTACK_UP" -> stat(s, ATK, 1, out, true);
			case "ATTACK_UP_2" -> stat(s, ATK, 2, out, true);
			case "DEFENSE_UP" -> stat(s, DEF, 1, out, true);
			case "DEFENSE_UP_2" -> stat(s, DEF, 2, out, true);
			case "DEFENSE_CURL" -> {
				f.curled = true;
				stat(s, DEF, 1, out, true);
			}
			case "SPEED_UP_2" -> stat(s, SPE, 2, out, true);
			case "SP_ATK_UP" -> stat(s, SPA, 1, out, true);
			case "SP_DEF_UP_2" -> stat(s, SPD, 2, out, true);
			case "EVASION_UP" -> {
				if (m.id().equals("minimize")) f.minimized = true;
				stat(s, EVA, 1, out, true);
			}
			case "ATTACK_DOWN" -> stat(t, ATK, -1, out, true);
			case "ATTACK_DOWN_2" -> stat(t, ATK, -2, out, true);
			case "DEFENSE_DOWN" -> stat(t, DEF, -1, out, true);
			case "DEFENSE_DOWN_2" -> stat(t, DEF, -2, out, true);
			case "SPEED_DOWN" -> stat(t, SPE, -1, out, true);
			case "SPEED_DOWN_2" -> stat(t, SPE, -2, out, true);
			case "ACCURACY_DOWN" -> stat(t, ACC, -1, out, true);
			case "EVASION_DOWN" -> stat(t, EVA, -1, out, true);
			case "HEAL", "MORNING_SUN", "SYNTHESIS", "MOONLIGHT" -> {
				if (att.hp >= att.maxHp()) {
					out.add(Event.text("But it failed!"));
					return;
				}
				if (m.id().equals("rest")) {
					att.status = Mon.Status.SLEEP;
					att.sleepTurns = 2;
					heal(s, att.maxHp(), out);
					out.add(Event.of(Event.Kind.STATUS, s));
					out.add(Event.text(who(s) + " fell asleep and became healthy!"));
					return;
				}
				int amount = att.maxHp() / 2;
				if (!e.equals("HEAL")) amount = weather == SUN ? att.maxHp() : weather == NO_WEATHER ? att.maxHp() / 2 : att.maxHp() / 4;
				heal(s, amount, out);
				out.add(Event.text(who(s) + " regained health!"));
			}
			case "LEECH_SEED" -> {
				if (isType(t, Type.GRASS)) noEffect(t, out);
				else if (g.seeded) out.add(Event.text("But it failed!"));
				else {
					g.seeded = true;
					out.add(Event.text(who(t) + " was seeded!"));
				}
			}
			case "FOCUS_ENERGY" -> {
				if (f.focus) out.add(Event.text("But it failed!"));
				else {
					f.focus = true;
					out.add(Event.text(who(s) + "'s getting pumped!"));
				}
			}
			case "MIST" -> {
				mist[s] = 5;
				out.add(Event.text(who(s) + "'s shrouded in MIST!"));
			}
			case "REFLECT" -> {
				if (reflect[s] > 0) out.add(Event.text("But it failed!"));
				else {
					reflect[s] = 5;
					out.add(Event.text(who(s) + "'s DEFENSE rose!"));
				}
			}
			case "LIGHT_SCREEN" -> {
				if (lightScreen[s] > 0) out.add(Event.text("But it failed!"));
				else {
					lightScreen[s] = 5;
					out.add(Event.text(who(s) + "'s SPCL.DEF rose!"));
				}
			}
			case "SAFEGUARD" -> {
				if (safeguard[s] > 0) out.add(Event.text("But it failed!"));
				else {
					safeguard[s] = 5;
					out.add(Event.text(who(s) + "'s covered by a veil!"));
				}
			}
			case "SUBSTITUTE" -> {
				int cost = att.maxHp() / 4;
				if (f.substitute) out.add(Event.text(who(s) + " has a SUBSTITUTE already!"));
				else if (att.hp <= cost) out.add(Event.text("Too weak to make a SUBSTITUTE!"));
				else {
					att.hp -= cost;
					f.substitute = true;
					f.subHp = cost;
					out.add(Event.hp(s, att.hp, att.maxHp()));
					out.add(Event.text(who(s) + " made a SUBSTITUTE!"));
				}
			}
			case "PROTECT", "ENDURE" -> {
				boolean ok = rng.nextInt(256) < (255 >> Math.min(7, f.protectCount));
				if (!ok) {
					f.protectCount = 0;
					out.add(Event.text("But it failed!"));
					return;
				}
				f.protectCount++;
				if (e.equals("PROTECT")) {
					f.protecting = true;
					out.add(Event.text(who(s) + " protected itself!"));
				} else {
					f.enduring = true;
					out.add(Event.text(who(s) + " braced itself!"));
				}
				return;
			}
			case "RESET_STATS" -> {
				java.util.Arrays.fill(f.stages, 0);
				java.util.Arrays.fill(g.stages, 0);
				out.add(Event.text("All stat changes were eliminated!"));
			}
			case "BELLY_DRUM" -> {
				if (att.hp <= att.maxHp() / 2) out.add(Event.text("But it failed!"));
				else {
					att.hp -= att.maxHp() / 2;
					f.stages[ATK] = 6;
					out.add(Event.hp(s, att.hp, att.maxHp()));
					out.add(Event.text(who(s) + " cut its HP and maximized ATTACK!"));
				}
			}
			case "CURSE" -> {
				if (isType(s, Type.GHOST)) {
					if (g.cursed) {
						out.add(Event.text("But it failed!"));
						return;
					}
					att.hp = Math.max(0, att.hp - att.maxHp() / 2);
					out.add(Event.hp(s, att.hp, att.maxHp()));
					g.cursed = true;
					out.add(Event.text(who(s) + " cut its own HP and laid a CURSE on " + who(t) + "!"));
				} else {
					stat(s, SPE, -1, out, true);
					stat(s, ATK, 1, out, true);
					stat(s, DEF, 1, out, true);
				}
			}
			case "DISABLE" -> {
				int slot = g.lastMove == null ? -1 : def.moves.indexOf(g.lastMove);
				if (slot < 0 || g.disableTurns > 0) out.add(Event.text("But it failed!"));
				else {
					g.disabledSlot = slot;
					g.disableTurns = 2 + rng.nextInt(6);
					out.add(Event.text(who(t) + "'s " + Move.get(g.lastMove).name() + " was DISABLED!"));
				}
			}
			case "SPITE" -> {
				int slot = g.lastMove == null ? -1 : def.moves.indexOf(g.lastMove);
				if (slot < 0 || def.pp.get(slot) <= 0) out.add(Event.text("But it failed!"));
				else {
					int by = Math.min(def.pp.get(slot), 2 + rng.nextInt(4));
					def.pp.set(slot, def.pp.get(slot) - by);
					out.add(Event.text("Reduced " + who(t) + "'s " + Move.get(g.lastMove).name() + " by " + by + "!"));
				}
			}
			case "MEAN_LOOK" -> {
				g.meanLook = true;
				out.add(Event.text(who(t) + " can't escape now!"));
			}
			case "FORCE_SWITCH", "TELEPORT" -> {
				if (kind == Kind.WILD) {
					out.add(Event.of(Event.Kind.ESCAPE, e.equals("TELEPORT") ? s : t));
					if (s == 1 || e.equals("TELEPORT")) {
						out.add(Event.text(s == 1 && e.equals("TELEPORT") ? who(1) + " fled!" : s == 0 ? "Got away safely!" : who(0) + " was blown away!"));
					} else out.add(Event.text(who(1) + " fled in fear!"));
					fled = true;
					end(Result.RAN, out);
					return;
				}
				out.add(Event.text("But it failed!"));
			}
			case "LOCK_ON" -> {
				f.lockOn = true;
				out.add(Event.text(who(s) + " took aim at " + who(t) + "!"));
			}
			case "FORESIGHT" -> {
				g.foresight = true;
				out.add(Event.text(who(s) + " identified " + who(t) + "!"));
			}
			case "NIGHTMARE" -> {
				if (def.status != Mon.Status.SLEEP || g.nightmare) out.add(Event.text("But it failed!"));
				else {
					g.nightmare = true;
					out.add(Event.text(who(t) + " started to have a NIGHTMARE!"));
				}
			}
			case "DESTINY_BOND" -> {
				f.destinyBond = true;
				out.add(Event.text(who(s) + " is trying to take its foe with it!"));
			}
			case "PERISH_SONG" -> {
				for (Field x : field) if (x.perish <= 0) x.perish = 4;
				out.add(Event.text("Both POKéMON will faint in 3 turns!"));
			}
			case "SPIKES" -> {
				if (spikes[t] > 0) out.add(Event.text("But it failed!"));
				else {
					spikes[t] = 1;
					out.add(Event.text("SPIKES scattered all around " + who(t) + "!"));
				}
			}
			case "RAIN_DANCE" -> setWeather(RAIN, "A downpour started!", out);
			case "SUNNY_DAY" -> setWeather(SUN, "The sunlight got bright!", out);
			case "SANDSTORM" -> setWeather(SAND, "A sandstorm brewed!", out);
			case "HEAL_BELL" -> {
				for (Mon m2 : s == 0 ? mine : theirs) {
					m2.status = Mon.Status.NONE;
					m2.sleepTurns = 0;
				}
				out.add(Event.of(Event.Kind.STATUS, s));
				out.add(Event.text("A bell chimed!"));
			}
			case "PSYCH_UP" -> {
				System.arraycopy(g.stages, 0, f.stages, 0, 7);
				out.add(Event.text(who(s) + " copied " + who(t) + "'s stat changes!"));
			}
			case "PAIN_SPLIT" -> {
				int avg = (att.hp + def.hp) / 2;
				att.hp = Math.min(att.maxHp(), avg);
				def.hp = Math.min(def.maxHp(), avg);
				out.add(Event.hp(s, att.hp, att.maxHp()));
				out.add(Event.hp(t, def.hp, def.maxHp()));
				out.add(Event.text("The battlers shared their pain!"));
			}
			case "ATTRACT" -> {
				int ga = att.gender(), gd = def.gender();
				if (ga < 0 || gd < 0 || ga == gd || g.attracted) out.add(Event.text("But it failed!"));
				else {
					g.attracted = true;
					out.add(Event.text(who(t) + " fell in love!"));
				}
			}
			case "CONVERSION" -> {
				List<Type> types = new ArrayList<>();
				for (String id : att.moves) {
					Type ty = Move.get(id).type();
					if (ty != Type.CURSE && !isType(s, ty) && !types.contains(ty)) types.add(ty);
				}
				if (types.isEmpty()) out.add(Event.text("But it failed!"));
				else {
					Type ty = types.get(rng.nextInt(types.size()));
					f.type1Override = ty;
					f.type2Override = null;
					out.add(Event.text(who(s) + " transformed into the " + ty.label.toUpperCase() + " type!"));
				}
			}
			case "CONVERSION2" -> {
				Move last = g.lastMove == null ? null : Move.find(g.lastMove);
				if (last == null) {
					out.add(Event.text("But it failed!"));
					return;
				}
				List<Type> resist = new ArrayList<>();
				for (Type ty : Type.values()) if (ty != Type.CURSE && last.type().against(ty, null) < 1) resist.add(ty);
				Type ty = resist.get(rng.nextInt(resist.size()));
				f.type1Override = ty;
				f.type2Override = null;
				out.add(Event.text(who(s) + " transformed into the " + ty.label.toUpperCase() + " type!"));
			}
			case "SPLASH" -> out.add(Event.text("But nothing happened!"));
			default -> out.add(Event.text("But it failed!"));
		}
	}

	private void setWeather(int w, String text, List<Event> out) {
		if (weather == w) {
			out.add(Event.text("But it failed!"));
			return;
		}
		weather = w;
		weatherTurns = 5;
		out.add(Event.text(text));
	}

	private void stat(int t, int which, int by, List<Event> out, boolean shout) {
		if (by < 0 && mist[t] > 0) {
			out.add(Event.text(who(t) + "'s protected by MIST!"));
			return;
		}
		int now = field[t].stages[which];
		int next = Math.max(-6, Math.min(6, now + by));
		if (next == now) {
			if (shout) out.add(Event.text("Nothing happened!"));
			return;
		}
		field[t].stages[which] = next;
		String how = by > 0 ? (by > 1 ? " went way up!" : " went up!") : (by < -1 ? " sharply fell!" : " fell!");
		out.add(new Event(Event.Kind.STAT, t, null, by, which));
		out.add(Event.text(who(t) + "'s " + STAT_NAMES[which] + how));
	}

	// ---------- the end of the turn ----------

	/** Weather, burn, poison, Leech Seed, Nightmare, Curse, trapping, Future Sight, Perish Song, timers; then fainting. */
	private void endOfTurn(List<Event> out) {
		for (int s = 0; s < 2; s++) {
			if (futureTurns[s] > 0 && --futureTurns[s] == 0 && !side(s).fainted()) {
				out.add(Event.text(who(s) + " took the FUTURE SIGHT attack!"));
				hurt(s, Math.min(side(s).hp, futureDamage[s]), out);
			}
		}
		if (weather != NO_WEATHER) {
			if (--weatherTurns <= 0) {
				out.add(Event.text(weather == RAIN ? "The rain stopped." : weather == SUN ? "The sunlight faded." : "The sandstorm subsided."));
				weather = NO_WEATHER;
			} else if (weather == SAND) {
				out.add(Event.text("The sandstorm rages."));
				for (int s = 0; s < 2; s++) {
					Mon m = side(s);
					if (m.fainted() || isType(s, Type.ROCK) || isType(s, Type.GROUND) || isType(s, Type.STEEL)) continue;
					out.add(Event.text(who(s) + " is buffeted by the sandstorm!"));
					hurt(s, Math.max(1, m.maxHp() / 8), out);
				}
			}
		}
		for (int s = 0; s < 2; s++) {
			Mon m = side(s);
			Field f = field[s];
			if (m.fainted()) continue;
			if (m.status == Mon.Status.BURN || m.status == Mon.Status.POISON) {
				int d = Math.max(1, m.maxHp() / 8);
				if (f.toxic > 0) d = Math.max(1, m.maxHp() * f.toxic++ / 16);
				out.add(Event.text(who(s) + "'s hurt by " + (m.status == Mon.Status.BURN ? "its burn!" : "poison!")));
				hurt(s, Math.min(d, m.hp), out);
			}
			if (!m.fainted() && f.seeded) {
				Mon o = side(1 - s);
				int d = Math.min(Math.max(1, m.maxHp() / 8), m.hp);
				out.add(Event.text("LEECH SEED saps " + who(s) + "!"));
				hurt(s, d, out);
				if (!o.fainted()) heal(1 - s, d, out);
			}
			if (!m.fainted() && f.nightmare && m.status == Mon.Status.SLEEP) {
				out.add(Event.text(who(s) + " is locked in a NIGHTMARE!"));
				hurt(s, Math.min(m.hp, Math.max(1, m.maxHp() / 4)), out);
			}
			if (!m.fainted() && f.cursed) {
				out.add(Event.text(who(s) + "'s afflicted by the CURSE!"));
				hurt(s, Math.min(m.hp, Math.max(1, m.maxHp() / 4)), out);
			}
			if (!m.fainted() && f.trap > 0) {
				f.trap--;
				out.add(Event.text(who(s) + "'s hurt by " + f.trapMove + "!"));
				hurt(s, Math.min(m.hp, f.trapDamage), out);
				if (f.trap == 0) out.add(Event.text(who(s) + " was released from " + f.trapMove + "!"));
			}
			if (!m.fainted() && f.perish > 0) {
				f.perish--;
				out.add(Event.text(who(s) + "'s PERISH count fell to " + f.perish + "!"));
				if (f.perish == 0) {
					m.hp = 0;
					out.add(Event.hp(s, 0, m.maxHp()));
				}
			}
			if (f.disableTurns > 0 && --f.disableTurns == 0) {
				f.disabledSlot = -1;
				out.add(Event.text(who(s) + " is no longer DISABLED!"));
			}
			if (m.status == Mon.Status.FREEZE && rng.nextInt(256) < 25) {
				m.status = Mon.Status.NONE;
				out.add(Event.of(Event.Kind.STATUS, s));
				out.add(Event.text(who(s) + " thawed out!"));
			}
		}
		for (int s = 0; s < 2; s++) {
			if (reflect[s] > 0 && --reflect[s] == 0) out.add(Event.text(who(s) + "'s REFLECT faded!"));
			if (lightScreen[s] > 0 && --lightScreen[s] == 0) out.add(Event.text(who(s) + "'s LIGHT SCREEN fell!"));
			if (safeguard[s] > 0 && --safeguard[s] == 0) out.add(Event.text(who(s) + "'s SAFEGUARD faded!"));
			if (mist[s] > 0) mist[s]--;
		}
		checkFaints(out);
	}

	private void checkFaints(List<Event> out) {
		if (them().fainted()) {
			out.add(Event.of(Event.Kind.FAINT, 1));
			out.add(Event.text(who(1) + " fainted!"));
			giveExp(out);
			int next = firstHealthy(theirs);
			if (next >= 0 && !me().fainted() || next >= 0 && firstHealthy(mine) >= 0) {
				if (next >= 0) {
					participants.clear();
					if (!me().fainted()) participants.add(active);
					sendOut(1, next, out);
				}
			}
			if (next < 0) {
				if (kind == Kind.TRAINER) {
					out.add(Event.text(playerName + " defeated " + trainer + "!"));
					prize = prizeBase * them().level;
				}
				prize += payDay;
				if (prize > 0) out.add(Event.text(playerName + " got ¥" + prize + " for winning!"));
				end(Result.WIN, out);
				return;
			}
		}
		if (me().fainted()) {
			if (out.stream().noneMatch(e -> e.kind() == Event.Kind.FAINT && e.side() == 0 && e.a() == active)) {
				out.add(new Event(Event.Kind.FAINT, 0, null, active, 0));
				out.add(Event.text(me().name() + " fainted!"));
			}
			participants.remove(active);
			field[0].clear();
			if (firstHealthy(mine) < 0) {
				out.add(Event.text(playerName + " is out of usable POKéMON!"));
				out.add(Event.text(playerName + " blacked out!"));
				end(Result.LOSS, out);
			} else {
				state = State.SWITCH;
			}
		}
	}

	private void giveExp(List<Event> out) {
		Mon foe = them();
		List<Integer> who = new ArrayList<>();
		for (int i : participants) if (i < mine.size() && !mine.get(i).fainted()) who.add(i);
		if (who.isEmpty()) return;
		int base = foe.species.baseExp() * foe.level / 7;
		if (kind == Kind.TRAINER) base = base * 3 / 2;
		int each = Math.max(1, base / who.size());
		int[] bases = {foe.species.hp(), foe.species.atk(), foe.species.def(), foe.species.spe(), foe.species.spa()};
		for (int i : who) {
			Mon m = mine.get(i);
			for (int k = 0; k < 5; k++) m.statExp[k] = Math.min(65535, m.statExp[k] + bases[k]);
			if (m.level >= 100) continue;
			m.exp += each;
			out.add(new Event(Event.Kind.EXP, 0, null, each, i));
			out.add(Event.text(m.name() + " gained " + each + " EXP. Points!"));
			while (m.level < 100 && m.exp >= m.species.growth().expAt(m.level + 1)) {
				int oldMax = m.maxHp();
				m.level++;
				m.levelFriendship();
				levelled.add(i);
				m.hp = Math.min(m.maxHp(), m.hp + (m.maxHp() - oldMax));
				out.add(new Event(Event.Kind.LEVEL, 0, null, m.level, i));
				if (i == active) out.add(Event.hp(0, m.hp, m.maxHp()));
				out.add(Event.text(m.name() + " grew to level " + m.level + "!"));
				for (String mv : m.species.learnsAt(m.level)) {
					if (m.moves.contains(mv) || Move.find(mv) == null) continue;
					if (m.moves.size() < 4) {
						m.learn(mv);
						out.add(Event.text(m.name() + " learned " + Move.get(mv).name() + "!"));
					} else {
						learns.add(new Learn(i, mv));
					}
				}
			}
		}
	}

	private void end(Result r, List<Event> out) {
		result = r;
		state = State.OVER;
	}
}
