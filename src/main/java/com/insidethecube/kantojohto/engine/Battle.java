package com.insidethecube.kantojohto.engine;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * A battle to Gold and Silver's rules: you against a wild Pokémon or a trainer's team, one Pokémon out on
 * each side, a turn at a time. Each choice you make plays out a turn and returns what happened, in order,
 * for the screen to play back. Side 0 is you, side 1 the other side.
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
		MOVE, SWITCH, POTION, BALL, RUN
	}

	public record Choice(Act act, int index) {}

	private static final int[] STAT_MULT = {25, 28, 33, 40, 50, 66, 100, 150, 200, 250, 300, 350, 400};
	private static final int[] ACC_MULT = {33, 36, 43, 50, 60, 75, 100, 133, 166, 200, 233, 266, 300};
	private static final int ATK = 0, DEF = 1, SPE = 2, SPA = 3, SPD = 4, ACC = 5, EVA = 6;
	private static final String[] STAT_NAMES = {"ATTACK", "DEFENSE", "SPEED", "SPCL.ATK", "SPCL.DEF", "accuracy", "evasiveness"};

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
	/** Money won (when you beat a trainer), and the Pokémon you caught. */
	public int prize;
	public Mon caught;
	private final int[][] stages = new int[2][7];
	private final boolean[] seeded = new boolean[2], flinched = new boolean[2];
	private int runAttempts;
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

	public List<Event> choose(Choice c) {
		List<Event> out = new ArrayList<>();
		if (state == State.OVER) return out;
		if (state == State.SWITCH) {
			if (c.act() != Act.SWITCH || c.index() < 0 || c.index() >= mine.size() || mine.get(c.index()).fainted()) return out;
			sendOut(c.index(), out);
			state = State.CHOOSE;
			return out;
		}
		flinched[0] = flinched[1] = false;
		int enemyMove = enemyChoice();
		switch (c.act()) {
			case RUN -> {
				if (kind == Kind.TRAINER) {
					out.add(Event.text("No! There's no running from a trainer battle!"));
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
				useMove(1, enemyMove, out);
			}
			case BALL -> {
				if (kind == Kind.TRAINER) {
					out.add(Event.text("The trainer blocked the BALL!"));
					out.add(Event.text("Don't be a thief!"));
					return out;
				}
				out.add(Event.text(playerName + " used POKé BALL!"));
				if (throwBall(out)) return out;
				useMove(1, enemyMove, out);
			}
			case POTION -> {
				Mon m = me();
				int before = m.hp;
				m.hp = Math.min(m.maxHp(), m.hp + 20);
				out.add(Event.text(playerName + " used POTION!"));
				out.add(Event.hp(0, m.hp, m.maxHp()));
				out.add(Event.text(m.name() + " recovered " + (m.hp - before) + " HP!"));
				useMove(1, enemyMove, out);
			}
			case SWITCH -> {
				if (c.index() == active || c.index() < 0 || c.index() >= mine.size() || mine.get(c.index()).fainted()) return out;
				out.add(Event.text(me().name() + ", come back!"));
				out.add(Event.of(Event.Kind.RECALL, 0));
				sendOut(c.index(), out);
				useMove(1, enemyMove, out);
			}
			case MOVE -> {
				int mi = c.index();
				if (mi < 0 || mi >= me().moves.size() || me().pp.get(mi) <= 0) return out;
				boolean meFirst = goesFirst(Move.get(me().moves.get(mi)), enemyMove < 0 ? null : Move.get(them().moves.get(enemyMove)));
				if (meFirst) {
					useMove(0, mi, out);
					if (!them().fainted() && !me().fainted()) useMove(1, enemyMove, out);
				} else {
					useMove(1, enemyMove, out);
					if (!them().fainted() && !me().fainted()) useMove(0, mi, out);
				}
			}
		}
		endOfTurn(out);
		return out;
	}

	private void sendOut(int index, List<Event> out) {
		active = index;
		stages[0] = new int[7];
		seeded[0] = false;
		participants.add(active);
		out.add(Event.sendOut(0, active, me().hp));
		out.add(Event.text("Go! " + me().name() + "!"));
	}

	private boolean goesFirst(Move mine, Move theirs) {
		int pa = mine.priority(), pb = theirs == null ? 0 : theirs.priority();
		if (pa != pb) return pa > pb;
		int sa = effSpeed(0), sb = effSpeed(1);
		if (sa != sb) return sa > sb;
		return rng.nextBoolean();
	}

	private int effSpeed(int s) {
		Mon m = side(s);
		int v = m.speed() * STAT_MULT[stages[s][SPE] + 6] / 100;
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

	/** A Poké Ball: catches with the games' chance (better at low HP, asleep or frozen). True if caught. */
	private boolean throwBall(List<Event> out) {
		Mon t = them();
		int max = t.maxHp();
		int a = Math.max(1, (3 * max - 2 * t.hp) * t.species.catchRate() / (3 * max));
		if (t.status == Mon.Status.SLEEP || t.status == Mon.Status.FREEZE) a += 10;
		a = Math.min(255, a);
		// Four checks with the chance per check set so all four pass a/256 of the time; the wobbles shown are the checks passed.
		double p = Math.pow((a + 1) / 256.0, 0.25);
		int passed = 0;
		while (passed < 4 && rng.nextDouble() < p) passed++;
		boolean got = passed == 4;
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
		for (int i = 0; i < t.moves.size(); i++) if (t.pp.get(i) > 0) usable.add(i);
		if (usable.isEmpty()) return -1;
		if (kind == Kind.TRAINER && rng.nextInt(10) < 7) {
			int best = usable.get(0);
			double bestScore = -1;
			for (int i : usable) {
				Move m = Move.get(t.moves.get(i));
				double score = m.status() ? 8 : m.power() * m.type().against(me().species.type1(), me().species.type2()) * (stab(t, m) ? 1.5 : 1);
				if (score > bestScore) {
					bestScore = score;
					best = i;
				}
			}
			return best;
		}
		return usable.get(rng.nextInt(usable.size()));
	}

	private static boolean stab(Mon m, Move mv) {
		return mv.type() == m.species.type1() || mv.type() == m.species.type2();
	}

	private void useMove(int s, int mi, List<Event> out) {
		Mon att = side(s), def = side(1 - s);
		if (att.fainted()) return;
		if (att.status == Mon.Status.SLEEP) {
			if (--att.sleepTurns <= 0) {
				att.status = Mon.Status.NONE;
				out.add(Event.text(who(s) + " woke up!"));
				out.add(Event.of(Event.Kind.STATUS, s));
			} else {
				out.add(Event.text(who(s) + " is fast asleep!"));
			}
			return;
		}
		if (att.status == Mon.Status.FREEZE) {
			if (rng.nextInt(256) < 25) {
				att.status = Mon.Status.NONE;
				out.add(Event.text(who(s) + " thawed out!"));
			} else {
				out.add(Event.text(who(s) + " is frozen solid!"));
				return;
			}
		}
		if (flinched[s]) {
			out.add(Event.text(who(s) + " flinched!"));
			return;
		}
		if (att.status == Mon.Status.PARALYSIS && rng.nextInt(4) == 0) {
			out.add(Event.text(who(s) + " is fully paralyzed!"));
			return;
		}
		Move m;
		if (mi < 0) {
			m = Move.get("tackle");
		} else {
			m = Move.get(att.moves.get(mi));
			att.pp.set(mi, att.pp.get(mi) - 1);
		}
		out.add(Event.text(who(s) + " used " + m.name().toUpperCase() + "!"));
		out.add(Event.of(Event.Kind.ATTACK, s));
		boolean selfMove = m.effect() == Move.Effect.DEFENSE_UP_SELF;
		if (!selfMove && m.accuracy() > 0) {
			int th = m.accuracy() * 255 / 100;
			int st = Math.max(-6, Math.min(6, stages[s][ACC] - stages[1 - s][EVA]));
			th = Math.min(255, th * ACC_MULT[st + 6] / 100);
			if (rng.nextInt(256) >= th) {
				out.add(Event.text(who(s) + "'s attack missed!"));
				return;
			}
		}
		if (m.status()) {
			applyEffect(s, m, out, true);
			return;
		}
		double eff = m.type().against(def.species.type1(), def.species.type2());
		if (eff == 0) {
			out.add(Event.text("It doesn't affect " + who(1 - s) + "..."));
			return;
		}
		boolean crit = rng.nextInt(256) < (m.highCrit() ? 64 : 17);
		int dmg = damage(s, m, crit, eff);
		def.hp = Math.max(0, def.hp - dmg);
		out.add(Event.of(Event.Kind.HIT, 1 - s));
		out.add(Event.hp(1 - s, def.hp, def.maxHp()));
		if (crit) out.add(Event.text("A critical hit!"));
		if (eff > 1) out.add(Event.text("It's super effective!"));
		else if (eff < 1) out.add(Event.text("It's not very effective..."));
		if (def.fainted()) return;
		if (m.effect() != Move.Effect.NONE && rng.nextInt(100) < m.chance()) applyEffect(s, m, out, false);
	}

	private int damage(int s, Move m, boolean crit, double eff) {
		Mon att = side(s), def = side(1 - s);
		boolean sp = m.type().special();
		int ai = sp ? SPA : ATK, di = sp ? SPD : DEF;
		int atkStage = stages[s][ai], defStage = stages[1 - s][di];
		// A critical hit ignores your own drops and their raises.
		if (crit) {
			atkStage = Math.max(0, atkStage);
			defStage = Math.min(0, defStage);
		}
		int a = (sp ? att.spAttack() : att.attack()) * STAT_MULT[atkStage + 6] / 100;
		int d = (sp ? def.spDefense() : def.defense()) * STAT_MULT[defStage + 6] / 100;
		if (!sp && att.status == Mon.Status.BURN) a /= 2;
		if (a > 255 || d > 255) {
			a = Math.max(1, a / 4);
			d = Math.max(1, d / 4);
		}
		d = Math.max(1, d);
		int lv = att.level * 2 / 5 + 2;
		int dmg = lv * m.power() * a / d / 50;
		if (crit) dmg *= 2;
		dmg = Math.min(997, dmg) + 2;
		if (stab(att, m)) dmg = dmg * 3 / 2;
		dmg = (int) (dmg * eff);
		if (dmg > 1) dmg = dmg * (217 + rng.nextInt(39)) / 255;
		return Math.max(1, dmg);
	}

	private void applyEffect(int s, Move m, List<Event> out, boolean statusMove) {
		Mon def = side(1 - s);
		int t = 1 - s;
		switch (m.effect()) {
			case ATTACK_DOWN -> stat(t, ATK, -m.stages(), out);
			case DEFENSE_DOWN, DEFENSE_DOWN_2 -> stat(t, DEF, -m.stages(), out);
			case SPEED_DOWN -> stat(t, SPE, -m.stages(), out);
			case ACCURACY_DOWN -> stat(t, ACC, -m.stages(), out);
			case DEFENSE_UP_SELF -> stat(s, DEF, m.stages(), out);
			case BURN -> {
				if (def.status == Mon.Status.NONE && def.species.type1() != Type.FIRE && def.species.type2() != Type.FIRE) {
					def.status = Mon.Status.BURN;
					out.add(Event.of(Event.Kind.STATUS, t));
					out.add(Event.text(who(t) + " was burned!"));
				}
			}
			case POISON -> {
				boolean immune = def.species.type1() == Type.POISON || def.species.type2() == Type.POISON || def.species.type1() == Type.STEEL || def.species.type2() == Type.STEEL;
				if (immune) out.add(Event.text("It doesn't affect " + who(t) + "..."));
				else if (def.status != Mon.Status.NONE) {
					if (statusMove) out.add(Event.text("But it failed!"));
				} else {
					def.status = Mon.Status.POISON;
					out.add(Event.of(Event.Kind.STATUS, t));
					out.add(Event.text(who(t) + " was poisoned!"));
				}
			}
			case SLEEP -> {
				if (def.status != Mon.Status.NONE) {
					out.add(Event.text("But it failed!"));
				} else {
					def.status = Mon.Status.SLEEP;
					def.sleepTurns = 1 + rng.nextInt(7);
					out.add(Event.of(Event.Kind.STATUS, t));
					out.add(Event.text(who(t) + " fell asleep!"));
				}
			}
			case PARALYZE -> {
				if (def.status == Mon.Status.NONE) {
					def.status = Mon.Status.PARALYSIS;
					out.add(Event.of(Event.Kind.STATUS, t));
					out.add(Event.text(who(t) + " is paralyzed! It may not attack!"));
				}
			}
			case FLINCH -> flinched[t] = true;
			case LEECH_SEED -> {
				if (def.species.type1() == Type.GRASS || def.species.type2() == Type.GRASS) out.add(Event.text("It doesn't affect " + who(t) + "..."));
				else if (seeded[t]) out.add(Event.text("But it failed!"));
				else {
					seeded[t] = true;
					out.add(Event.text(who(t) + " was seeded!"));
				}
			}
			case NONE -> {
			}
		}
	}

	private void stat(int t, int which, int by, List<Event> out) {
		int now = stages[t][which];
		int next = Math.max(-6, Math.min(6, now + by));
		if (next == now) {
			out.add(Event.text("Nothing happened!"));
			return;
		}
		stages[t][which] = next;
		String how = by > 0 ? (by > 1 ? " sharply rose!" : " rose!") : (by < -1 ? " sharply fell!" : " fell!");
		out.add(new Event(Event.Kind.STAT, t, null, by, which));
		out.add(Event.text(who(t) + "'s " + STAT_NAMES[which] + how));
	}

	/** Burn, poison and Leech Seed at the end of the turn, then fainting and what follows. */
	private void endOfTurn(List<Event> out) {
		for (int s = 0; s < 2; s++) {
			Mon m = side(s);
			if (m.fainted()) continue;
			if (m.status == Mon.Status.BURN || m.status == Mon.Status.POISON) {
				int d = Math.max(1, m.maxHp() / 8);
				m.hp = Math.max(0, m.hp - d);
				out.add(Event.text(who(s) + "'s hurt by " + (m.status == Mon.Status.BURN ? "its burn!" : "poison!")));
				out.add(Event.of(Event.Kind.HIT, s));
				out.add(Event.hp(s, m.hp, m.maxHp()));
			}
			if (!m.fainted() && seeded[s]) {
				Mon o = side(1 - s);
				int d = Math.max(1, m.maxHp() / 8);
				d = Math.min(d, m.hp);
				m.hp -= d;
				out.add(Event.text("LEECH SEED saps " + who(s) + "!"));
				out.add(Event.hp(s, m.hp, m.maxHp()));
				if (!o.fainted()) {
					o.hp = Math.min(o.maxHp(), o.hp + d);
					out.add(Event.hp(1 - s, o.hp, o.maxHp()));
				}
			}
		}
		checkFaints(out);
	}

	private void checkFaints(List<Event> out) {
		if (them().fainted()) {
			out.add(Event.of(Event.Kind.FAINT, 1));
			out.add(Event.text(who(1) + " fainted!"));
			giveExp(out);
			int next = firstHealthy(theirs);
			if (next >= 0) {
				enemy = next;
				stages[1] = new int[7];
				seeded[1] = false;
				participants.clear();
				if (!me().fainted()) participants.add(active);
				out.add(Event.sendOut(1, enemy, them().hp));
				out.add(Event.text(trainer + " sent out " + them().name() + "!"));
			} else {
				if (kind == Kind.TRAINER) {
					out.add(Event.text(playerName + " defeated " + trainer + "!"));
					prize = prizeBase * them().level;
					out.add(Event.text(playerName + " got ¥" + prize + " for winning!"));
				}
				end(Result.WIN, out);
				return;
			}
		}
		if (me().fainted()) {
			if (out.stream().noneMatch(e -> e.kind() == Event.Kind.FAINT && e.side() == 0)) {
				out.add(Event.of(Event.Kind.FAINT, 0));
				out.add(Event.text(me().name() + " fainted!"));
			}
			participants.remove(active);
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
		for (int i : participants) if (!mine.get(i).fainted()) who.add(i);
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
				m.hp = Math.min(m.maxHp(), m.hp + (m.maxHp() - oldMax));
				out.add(new Event(Event.Kind.LEVEL, 0, null, m.level, i));
				if (i == active) out.add(Event.hp(0, m.hp, m.maxHp()));
				out.add(Event.text(m.name() + " grew to level " + m.level + "!"));
				for (String mv : m.species.learnsAt(m.level)) {
					String forgot = m.learn(mv);
					if (forgot != null) out.add(Event.text(m.name() + " forgot " + Move.get(forgot).name().toUpperCase() + "..."));
					if (forgot != null || m.moves.contains(mv)) out.add(Event.text(m.name() + " learned " + Move.get(mv).name().toUpperCase() + "!"));
				}
			}
		}
	}

	private void end(Result r, List<Event> out) {
		result = r;
		state = State.OVER;
	}
}
