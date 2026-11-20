package com.insidethecube.retromon.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import com.insidethecube.retromon.engine.Event;
import com.insidethecube.retromon.engine.Item;
import com.insidethecube.retromon.engine.Mon;
import com.insidethecube.retromon.engine.Move;
import com.insidethecube.retromon.engine.Species;
import com.insidethecube.retromon.game.MonDisplay;
import com.insidethecube.retromon.game.MonTag;
import com.insidethecube.retromon.game.Net;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.lwjgl.glfw.GLFW;

/**
 * A battle on its stage: their card top right, yours (with its EXP bar) bottom left, the message bar along
 * the bottom, and the commands stacked on the right as the newer games have them: FIGHT, POKéMON, BAG, RUN.
 * The game on the server decides each turn; this plays back what happened (the lines, the HP running down,
 * each move's effect, hits shaking the screen, faints, Poké Balls sent out, thrown and shaking, EXP filling)
 * and then asks what you'll do next. A first catch shows its Pokédex entry. Keys or mouse; Esc is Minecraft's menu.
 */
public class BattleScreen extends GameScreen {
	/** What's on show for one side (it changes as the turn plays back, not all at once). */
	private static final class Side {
		String species = "", name = "", status = "", gender = "";
		int level, max = 1, hpTarget, index = -1;
		float hp, exp, expTarget;
		boolean shown;
	}

	private record Step(int kind, int side, String text, int a, int b) {}

	private static final int SHAKE = -1, POP = -2, END = -3, NOTICE_DONE = -4, INTO_BALL = -5, GROW = -6, DEX = -7, ABSORB = -8, CLOSE_BALL = -9,
			DROP = -10, CHEER = -11;
	/** The way in (as Let's Go): two quick flashes, a slanted sweep of white from left to right, then the white fades on the stage. */
	private static final int WIPE = 36;
	/** The wipe's ticks left when the stage goes up behind the white. */
	private static final int REVEAL = 16;
	/** Ticks since the stage showed (the cinema frame fades in over them). */
	private int onStage;
	/** Going to the Pokédex (ticks into the fade to white, -1 when not), and coming back from it (white fading). */
	private int toDex = -1, fromDex;
	private static final int DEX_FADE = 10;
	/** Aiming a Poké Ball: which ball, and how long the ring's been closing. */
	private String aimBall;
	private int aimTicks;
	/** The battle that's on (it carries on behind Minecraft's menu). */
	public static BattleScreen current;
	/** The commands as they're shown, top to bottom, and what each one is (0 FIGHT, 1 BAG, 2 POKéMON, 3 RUN). */
	private static final int[] ORDER = {0, 2, 1, 3};
	private boolean newCatch;
	private String caughtSpecies;

	private enum Mode {
		PLAY, MAIN, FIGHT, PARTY, BAG, BAG_TARGET, WAIT, AIM, ASK, HOLD
	}

	private final Side[] sides = {new Side(), new Side()};
	private final Deque<Step> queue = new ArrayDeque<>();
	private List<Mon> mine = new ArrayList<>(), theirs = new ArrayList<>();
	private int active;
	private String state = "CHOOSE", kind = "WILD", result;
	private boolean locked, canRun = true, boss;
	private final List<String> after = new ArrayList<>();
	private boolean afterQueued;

	private Mode mode = Mode.PLAY, noticeReturn = Mode.MAIN;
	private String message;
	private int typed, hold, wait, waited;
	private boolean forced;
	/** BATTLE STYLE SWITCH: the trainer's next one (its name) is coming; YES or NO to changing yours, then picking. */
	private String nextFoe = "", trainerName = "";
	private Choice ask;
	private boolean shifting;
	private int mainAt, fightAt, partyAt, bagAt;
	private String bagItem, lastBall = "poke_ball";
	private int wipe = WIPE;
	/** Your money, shown at the top left once it changes (prize money, Pay Day, blacking out): from, to, and ticks since. */
	private int moneyFrom, moneyTo, moneyAt = -1;
	private static final int MONEY_COUNT = 18;
	/** A level gained (the viewer's panel at the right while "grew to Lv." shows): each stat before and after. */
	private int[] levelBefore, levelAfter;
	/** When everything began to fade to white (your last one fainted), or -1. */
	private int whiteAt = -1;
	/** When it was caught ("Gotcha!" pops up), or -1. */
	private int gotchaAt = -1;

	public BattleScreen(CompoundTag start) {
		super("Battle");
		current = this;
		Music.play(start.getStringOr("music", "wild_battle"));
		feed(start);
		for (Mon m : theirs) caughtSpecies = m.species.id();
		newCatch = caughtSpecies != null && !ClientState.data.caught.contains(caughtSpecies);
	}

	/** A turn from the game: what happened, and where things stand. */
	public void feed(CompoundTag t) {
		mine = MonTag.loadAll(t.getListOrEmpty("mine"));
		theirs = MonTag.loadAll(t.getListOrEmpty("theirs"));
		active = t.getIntOr("active", 0);
		state = t.getStringOr("state", "CHOOSE");
		nextFoe = t.getStringOr("next", "");
		trainerName = t.getStringOr("trainer", trainerName);
		kind = t.getStringOr("kind", "WILD");
		locked = t.getBooleanOr("locked", false);
		canRun = t.getBooleanOr("canRun", true);
		String r = t.getStringOr("result", "");
		if (!r.isEmpty()) {
			result = r;
			boss = t.getBooleanOr("boss", false);
			for (Tag l : t.getListOrEmpty("after")) l.asString().ifPresent(after::add);
		}
		for (Tag e : t.getListOrEmpty("events")) {
			if (!(e instanceof CompoundTag c)) continue;
			String text = c.getStringOr("t", "");
			queue.add(new Step(c.getIntOr("k", 0), c.getIntOr("s", -1), text.isEmpty() ? null : text, c.getIntOr("a", 0), c.getIntOr("b", 0)));
		}
		mode = Mode.PLAY;
		waited = 0;
	}

	// ---------- playing back ----------

	@Override
	public void tick() {
		super.tick();
		if (wipe > 0) {
			wipe--;
			if (wipe == REVEAL) BattleScene.reveal();
			if (wipe > REVEAL) return;
		}
		// Sped up (TAB), the battle plays that much faster too: its steps, text, bars and waits.
		for (int i = 0, n = Math.max(1, Adventure.speed); i < n && minecraft != null && minecraft.screen == this; i++) step();
	}

	private void step() {
		onStage++;
		if (moneyAt >= 0) moneyAt++;
		if (fromDex > 0) fromDex--;
		if (toDex >= 0) {
			wait = Math.max(wait, 2);
			if (++toDex >= DEX_FADE) {
				toDex = -1;
				if (minecraft != null && caughtSpecies != null) {
					minecraft.setScreen(new DexScreen(caughtSpecies, () -> {
						current = this;
						fromDex = DEX_FADE;
						minecraft.setScreen(this);
					}));
				}
			}
		}
		if (mode == Mode.WAIT) waited++;
		boolean moving = false;
		for (Side sd : sides) {
			if (Math.abs(sd.hp - sd.hpTarget) > 0.01F) {
				float step = Math.max(0.35F, sd.max / 40F);
				sd.hp = sd.hp < sd.hpTarget ? Math.min(sd.hpTarget, sd.hp + step) : Math.max(sd.hpTarget, sd.hp - step);
				moving = true;
			}
		}
		Side me = sides[0];
		if (me.exp < me.expTarget) {
			Species sp = Species.ALL.get(me.species);
			int next = sp == null || me.level >= 100 ? Integer.MAX_VALUE : sp.growth().expAt(me.level + 1);
			float cap = Math.min(me.expTarget, next);
			if (me.exp < cap) {
				me.exp = Math.min(cap, me.exp + Math.max(1, (next - (sp == null ? 0 : sp.growth().expAt(me.level))) / 40F));
				moving = true;
			}
		}
		if (moving) return;
		if (wait > 0) {
			wait--;
			return;
		}
		if (message != null) {
			typed = Math.min(message.length(), typed + textSpeed());
			if (typed >= message.length() && ++hold > 30 && mode == Mode.PLAY) message = null;
			return;
		}
		if (!queue.isEmpty()) {
			play(queue.poll());
			return;
		}
		if (mode == Mode.PLAY) done();
	}

	private Side side(int s) {
		return sides[s == 1 ? 1 : 0];
	}

	private void play(Step e) {
		MonDisplay d = e.side() >= 0 ? BattleScene.display(e.side()) : null;
		switch (e.kind()) {
			case INTO_BALL -> {
				// As in the games: the ball bounces up off it, opens, it's drawn in as red light, the ball shuts and drops.
				BattleScene.ballUp();
				wait = 7;
				return;
			}
			case ABSORB -> {
				BattleScene.absorb();
				wait = 14;
				return;
			}
			case CLOSE_BALL -> {
				BattleScene.closeBall();
				wait = 6;
				return;
			}
			case CHEER -> {
				BattleScene.cheer();
				Sounds.cry(sides[0].species, 1.05F);
				wait = 50;
				return;
			}
			case DROP -> {
				BattleScene.dropBall();
				wait = 12;
				return;
			}
			case SHAKE -> {
				Sounds.shake();
				BattleScene.shakeBall();
				wait = 18;
				return;
			}
			case POP -> {
				BattleScene.burstBall();
				Sounds.poof();
				BattleScene.release();
				if (BattleScene.display(1) != null) BattleScene.display(1).act("cry");
				Sounds.cry(sides[1].species, 1F);
				wait = 12;
				return;
			}
			case GROW -> {
				if (d != null) {
					d.sizeTarget = 1;
					d.act("cry");
				}
				Sounds.cry(side(e.side()).species, 1F);
				side(e.side()).shown = true;
				wait = 14;
				return;
			}
			case DEX -> {
				// A first catch: the screen goes white, the Pokédex opens on its entry, then white again back to the battle.
				if (minecraft != null && caughtSpecies != null) toDex = 0;
				wait = 4;
				return;
			}
			case END -> {
				// A win (or a catch) holds on yours celebrating, with your team's EXP showing, till you carry on.
				if (("WIN".equals(result) || "CAUGHT".equals(result)) && sides[0].shown && whiteAt < 0) {
					mode = Mode.HOLD;
					message = null;
					BattleScene.hold();
				} else close();
				return;
			}
			case NOTICE_DONE -> {
				mode = noticeReturn;
				return;
			}
			default -> {
			}
		}
		Event.Kind[] kinds = Event.Kind.values();
		if (e.kind() < 0 || e.kind() >= kinds.length) return;
		switch (kinds[e.kind()]) {
			case TEXT -> say(e.text());
			case HP -> {
				Side sd = side(e.side());
				sd.hpTarget = e.a();
				sd.max = Math.max(1, e.b());
			}
			case ATTACK -> {
				Move mv = e.text() == null ? null : Move.find(e.text());
				boolean close = mv == null || BattleScene.contact(mv);
				if (d != null) {
					d.dash = close;
					d.lunge = MonDisplay.LUNGE;
					if (mv != null && mv.power() <= 0) d.act("cry");
				}
				if (mv == null) Sounds.attack();
				// (BATTLE EFFECTS off in OPTION: no move animations.)
				boolean fx = Settings.effects();
				if (fx) BattleScene.move(e.side(), e.text());
				// The hit lands as it arrives (up close), or as the move reaches the target (from afar).
				wait = mv == null || !fx ? 8 : mv.power() <= 0 ? 16 : close ? 8 : 19;
			}
			case HIT -> {
				int effect = effectAhead();
				if (d != null) {
					d.flash = 14;
					d.act("recoil");
				}
				Sounds.hit(effect);
				if (Settings.effects()) BattleScene.hit(e.side(), effect);
				wait = 14;
			}
			case FAINT -> {
				BattleScene.faint(e.side());
				Sounds.cry(side(e.side()).species, 0.7F);
				side(e.side()).shown = false;
				// It falls and lies there, then its trainer calls it back with a ball.
				wait = 66;
			}
			case SEND_OUT -> {
				List<Mon> list = e.side() == 0 ? mine : theirs;
				Side sd = side(e.side());
				if (e.a() >= 0 && e.a() < list.size()) {
					Mon m = list.get(e.a());
					sd.species = m.species.id();
					sd.name = m.name();
					sd.gender = m.genderMark();
					sd.level = m.level;
					sd.max = m.maxHp();
					sd.status = m.status.tag;
					if (e.side() == 0 && sd.index != e.a()) sd.exp = sd.expTarget = m.exp;
				}
				sd.index = e.a();
				sd.hp = sd.hpTarget = e.b();
				if (d != null) {
					d.species = sd.species;
					d.size = d.sizeOld = 0;
					d.sizeTarget = 0;
				}
				// Yours (and a trainer's) come out of a ball; a wild one is just there.
				if (e.side() == 0 || !kind.equals("WILD")) {
					BattleScene.sendOut(e.side());
					sd.shown = false;
					queue.addFirst(new Step(GROW, e.side(), null, 0, 0));
					wait = 9;
				} else {
					sd.shown = true;
					if (d != null) {
						d.sizeTarget = 1;
						d.act("cry");
					}
					Sounds.cry(sd.species, 1F);
					wait = 18;
				}
			}
			case RECALL -> {
				// Called back: red light streams from it into the ball in its trainer's hand (the Creature Viewer's recall).
				BattleScene.recall(e.side());
				wait = 30;
			}
			case BALL -> {
				BattleScene.throwBall(lastBall);
				wait = 14;
				Step next = queue.peek();
				boolean caught = next != null && next.kind() == Event.Kind.CAUGHT.ordinal();
				if (!caught) queue.addFirst(new Step(POP, 1, null, 0, 0));
				for (int i = 0; i < e.a(); i++) queue.addFirst(new Step(SHAKE, 1, null, 0, 0));
				queue.addFirst(new Step(DROP, 1, null, 0, 0));
				queue.addFirst(new Step(CLOSE_BALL, 1, null, 0, 0));
				queue.addFirst(new Step(ABSORB, 1, null, 0, 0));
				queue.addFirst(new Step(INTO_BALL, 1, null, 0, 0));
			}
			case CAUGHT -> {
				// The battle music stops for the catch.
				Music.stop();
				Sounds.caught();
				BattleScene.caughtStars();
				gotchaAt = ticks;
				sides[1].shown = false;
				wait = 40;
			}
			case ESCAPE -> {
				Sounds.run();
				wait = 12;
			}
			case EXP -> {
				if (e.b() == sides[0].index) sides[0].expTarget += e.a();
			}
			case LEVEL -> {
				if (e.b() == sides[0].index) sides[0].level = e.a();
				if (e.b() >= 0 && e.b() < mine.size()) {
					Mon lm = mine.get(e.b());
					int keep = lm.level;
					lm.level = e.a() - 1;
					levelBefore = stats(lm);
					lm.level = e.a();
					levelAfter = stats(lm);
					lm.level = Math.max(keep, e.a());
				}
				Sounds.levelUp();
				wait = 30;
			}
			case STATUS -> {
				Side sd = side(e.side());
				List<Mon> list = e.side() == 0 ? mine : theirs;
				if (sd.index >= 0 && sd.index < list.size()) sd.status = list.get(sd.index).status.tag;
			}
			case STAT -> {
				Sounds.stat(e.a() > 0);
				BattleScene.stat(e.side() == 1 ? 1 : 0, e.a() > 0);
				wait = 8;
			}
			case MONEY -> {
				// Counting on from what it showed last (a prize, then Pay Day's coins on top).
				moneyFrom = moneyAt >= 0 ? moneyTo : e.b() - e.a();
				moneyTo = e.b();
				moneyAt = 0;
				if (e.a() > 0) Sounds.sfx("purchase");
				wait = 4;
			}
			default -> {
			}
		}
	}

	/** Whether the hit coming up is super effective (1), not very effective (-1) or neither (0): its line follows it. */
	private int effectAhead() {
		int n = 0;
		for (Step st : queue) {
			if (++n > 4) break;
			if (st.text() == null) continue;
			if (st.text().contains("super effective")) return 1;
			if (st.text().contains("not very effective")) return -1;
		}
		return 0;
	}

	private static int[] stats(Mon m) {
		return new int[] {m.maxHp(), m.attack(), m.defense(), m.spAttack(), m.spDefense(), m.speed()};
	}

	private void say(String text) {
		if (text == null || !text.contains(" grew to ")) levelAfter = null;
		if ("... ... ...".equals(text) && whiteAt < 0) whiteAt = ticks;
		// (Our creatures' names in place before it's typed out, so the old name never shows part way.)
		message = text == null ? "" : Look.text(text);
		typed = 0;
		hold = 0;
	}

	/** The turn's played back: the end of the battle, or your next choice. */
	private void done() {
		if (result != null) {
			if (!afterQueued) {
				afterQueued = true;
				if (result.equals("WIN")) {
					Music.play(kind.equals("WILD") ? "defeated_wild_mon" : boss ? "defeated_gym_leader" : "defeated_trainer");
					// Yours celebrates: the camera comes round in front of it as it hops for joy.
					if (sides[0].shown) queue.add(new Step(CHEER, 0, null, 0, 0));
				}
				if (result.equals("CAUGHT") && newCatch) {
					queue.add(new Step(Event.Kind.TEXT.ordinal(), -1, "New POKéDEX data will be added for " + sides[1].name + "!", 0, 0));
					queue.add(new Step(DEX, -1, null, 0, 0));
				}
				for (String l : after) queue.add(new Step(Event.Kind.TEXT.ordinal(), -1, l, 0, 0));
				queue.add(new Step(END, -1, null, 0, 0));
			}
			return;
		}
		if (state.equals("SWITCH")) {
			forced = true;
			partyAt = firstHealthy();
			mode = Mode.PARTY;
			return;
		}
		if (state.equals("SHIFT")) {
			// The trainer's about to send out their next one: on SWITCH you're asked if you'll change yours first.
			if (!Settings.switchStyle()) {
				send("SWITCH", -1, null);
				return;
			}
			ask = new Choice(List.of("YES", "NO"));
			mode = Mode.ASK;
			return;
		}
		// Carrying on with a move that takes more than one turn: no choice to make.
		if (locked) {
			send("MOVE", -1, null);
			return;
		}
		mode = Mode.MAIN;
	}

	private int firstHealthy() {
		for (int i = 0; i < mine.size(); i++) if (!mine.get(i).fainted()) return i;
		return 0;
	}

	private Mon me() {
		return active >= 0 && active < mine.size() ? mine.get(active) : null;
	}

	private void close() {
		current = null;
		BattleScene.end();
		ClientPlayNetworking.send(new Net.Act("battle_closed", new CompoundTag()));
		Music.resumePlace();
		if (minecraft != null && minecraft.screen == this) minecraft.setScreen(null);
	}

	// ---------- your choices ----------

	/** "Will you change POKéMON?": YES to pick one, NO to keep the one that's out. */
	private void askPick(int i) {
		Sounds.select();
		if (i == 0) {
			shifting = true;
			forced = false;
			partyAt = active;
			for (int k = 0; k < mine.size(); k++) if (k != active && !mine.get(k).fainted()) {
				partyAt = k;
				break;
			}
			mode = Mode.PARTY;
		} else send("SWITCH", -1, null);
	}

	private String askLine() {
		String who = trainerName.isEmpty() ? "The trainer" : trainerName;
		return who + " is about to use " + nextFoe + ". Will you change POKéMON?";
	}

	private void send(String act, int i, String item) {
		shifting = false;
		CompoundTag t = new CompoundTag();
		t.putString("act", act);
		t.putInt("i", i);
		if (item != null) t.putString("item", item);
		ClientPlayNetworking.send(new Net.Act("battle", t));
		mode = Mode.WAIT;
		waited = 0;
		message = null;
	}

	/** A line from the screen itself (no PP left, and so on), then back to the menu it came from. */
	private void notice(String text, Mode back) {
		noticeReturn = back;
		mode = Mode.PLAY;
		say(text);
		queue.addFirst(new Step(NOTICE_DONE, -1, null, 0, 0));
	}

	private void pickMain(int i) {
		mainAt = i;
		switch (i) {
			case 0 -> {
				fightAt = 0;
				mode = Mode.FIGHT;
			}
			case 1 -> {
				bagAt = 0;
				mode = Mode.BAG;
			}
			case 2 -> {
				forced = false;
				partyAt = active;
				mode = Mode.PARTY;
			}
			default -> {
				if (!kind.equals("WILD")) notice("No! There's no running from a trainer battle!", Mode.MAIN);
				else send("RUN", 0, null);
			}
		}
	}

	private void pickMove(int i) {
		Mon m = me();
		if (m == null) return;
		boolean anyPp = m.pp.stream().anyMatch(p -> p > 0);
		if (!anyPp) {
			send("MOVE", -1, null);
			return;
		}
		if (i >= m.moves.size()) return;
		if (m.pp.get(i) <= 0) notice("No PP left for this move!", Mode.FIGHT);
		else send("MOVE", i, null);
	}

	private void pickParty(int i) {
		if (i >= mine.size()) return;
		Mon m = mine.get(i);
		if (mode == Mode.BAG_TARGET) {
			useItemOn(i);
			return;
		}
		if (m.fainted()) notice("There's no will to fight!", Mode.PARTY);
		else if (i == active && !forced) notice(m.name() + " is already out!", Mode.PARTY);
		else send("SWITCH", i, null);
	}

	/** What's in your bag that works in battle: balls first, then medicine and X items. */
	/** The bag in battle has two pockets: POKé BALLS (0) and MEDICINE (1). */
	private int bagPocket;

	private List<String> battleItems() {
		List<String> out = new ArrayList<>();
		for (String id : ClientState.data.items.keySet()) {
			Item it = Item.find(id);
			if (it == null || ClientState.data.count(id) <= 0) continue;
			if (bagPocket == 0 ? it.ball() : !it.ball() && it.battleUse()) out.add(id);
		}
		return out;
	}

	private void pocket(int d) {
		bagPocket = Math.floorMod(bagPocket + d, 2);
		bagAt = 0;
		Sounds.press();
	}

	private void pickItem(int i) {
		List<String> l = battleItems();
		if (i >= l.size()) return;
		Item it = Item.find(l.get(i));
		if (it.ball()) {
			if (!kind.equals("WILD")) {
				notice("The trainer blocked the BALL! Don't be a thief!", Mode.BAG);
				return;
			}
			// Aim first: the ring round the wild Pokémon, coloured by how likely it is to stay caught.
			aimBall = it.id();
			aimTicks = 0;
			mode = Mode.AIM;
			return;
		}
		if (it.revives() > 0) {
			bagItem = it.id();
			partyAt = 0;
			mode = Mode.BAG_TARGET;
			return;
		}
		bagItem = it.id();
		useItemOn(active);
	}

	private void useItemOn(int i) {
		Item it = Item.find(bagItem);
		if (it == null || i >= mine.size()) return;
		Mon target = mine.get(i);
		if (it.revives() > 0 ? !target.fainted() : (it.heal() > 0 || it.cures() != null) && it.statBoost() < 0 && !worthIt(it, target)) {
			notice("It won't have any effect.", Mode.BAG);
			return;
		}
		ClientState.data.take(it.id());
		send("ITEM", i, it.id());
	}

	private static boolean worthIt(Item it, Mon m) {
		if (m.fainted()) return false;
		boolean hurt = m.hp < m.maxHp();
		boolean sick = it.cures() != null && m.status != Mon.Status.NONE && (it.cures() == Mon.Status.NONE || it.cures() == m.status);
		return (it.heal() > 0 && hurt) || sick || it.curesConfusion();
	}

	@Override
	protected boolean key(KeyEvent e) {
		if (wipe > REVEAL) return true;
		// If the game ever stops answering, Delete gets you out.
		if (mode == Mode.WAIT && waited > 100 && e.key() == GLFW.GLFW_KEY_DELETE) {
			ClientPlayNetworking.send(new Net.Act("battle_quit", new CompoundTag()));
			close();
			return true;
		}
		switch (mode) {
			case HOLD -> {
				if (Ui.isA(e) || Ui.isB(e)) {
					Sounds.press();
					close();
				}
			}
			case PLAY, WAIT -> {
				if (message != null && Ui.isA(e)) advance();
			}
			case MAIN -> {
				int row = row(mainAt);
				if (Ui.isUp(e) || Ui.isLeft(e)) mainAt = ORDER[Math.floorMod(row - 1, 4)];
				else if (Ui.isDown(e) || Ui.isRight(e)) mainAt = ORDER[Math.floorMod(row + 1, 4)];
				else if (Ui.isA(e)) {
					Sounds.press();
					pickMain(mainAt);
				}
			}
			case FIGHT -> {
				Mon m = me();
				int n = m == null ? 0 : m.moves.size();
				if (n == 0 || Ui.isB(e)) mode = Mode.MAIN;
				else if (Ui.isUp(e) || Ui.isLeft(e)) fightAt = Math.floorMod(fightAt - 1, n);
				else if (Ui.isDown(e) || Ui.isRight(e)) fightAt = Math.floorMod(fightAt + 1, n);
				else if (Ui.isA(e)) {
					Sounds.press();
					pickMove(fightAt);
				}
			}
			case PARTY, BAG_TARGET -> {
				int n = mine.size();
				if (n == 0) return true;
				if (Ui.isB(e) && shifting && mode == Mode.PARTY) mode = Mode.ASK;
				else if (Ui.isB(e) && !forced) mode = mode == Mode.BAG_TARGET ? Mode.BAG : Mode.MAIN;
				else if (Ui.isUp(e) || Ui.isLeft(e)) partyAt = Math.floorMod(partyAt - 1, n);
				else if (Ui.isDown(e) || Ui.isRight(e)) partyAt = Math.floorMod(partyAt + 1, n);
				else if (Ui.isA(e)) pickParty(partyAt);
			}
			case ASK -> {
				if (ask.key(e)) return true;
				if (Ui.isA(e)) askPick(ask.at);
				else if (Ui.isB(e)) askPick(1);
			}
			case AIM -> {
				if (Ui.isA(e)) throwBall();
				else if (Ui.isB(e)) mode = Mode.BAG;
			}
			case BAG -> {
				int n = Math.max(1, battleItems().size());
				if (Ui.isB(e)) mode = Mode.MAIN;
				else if (Ui.isUp(e)) bagAt = Math.floorMod(bagAt - 1, n);
				else if (Ui.isDown(e)) bagAt = Math.floorMod(bagAt + 1, n);
				else if (Ui.isLeft(e) || Ui.isRight(e) || e.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_TAB) pocket(Ui.isLeft(e) ? -1 : 1);
				else if (Ui.isA(e)) pickItem(bagAt);
			}
		}
		return true;
	}

	/** TAB turns the bag's pockets (not the game's speed) while the bag's open. */
	@Override
	protected boolean wantsTab() {
		return mode == Mode.BAG;
	}

	private static int row(int command) {
		for (int i = 0; i < 4; i++) if (ORDER[i] == command) return i;
		return 0;
	}

	private void advance() {
		if (message == null) return;
		Sounds.press();
		if (typed < message.length()) typed = message.length();
		else message = null;
	}

	@Override
	protected void clickAnywhere() {
		if (mode == Mode.HOLD) {
			Sounds.press();
			close();
			return;
		}
		if (mode == Mode.PLAY || mode == Mode.WAIT) advance();
		else if (mode == Mode.AIM) throwBall();
	}

	private void throwBall() {
		if (aimBall == null) return;
		Sounds.press();
		lastBall = aimBall;
		ClientState.data.take(aimBall);
		send("BALL", 0, aimBall);
		aimBall = null;
	}

	/** The chance of a catch with this ball now (Gold and Silver's sums), for the ring's colour. */
	private float catchChance() {
		Species sp = Species.ALL.get(sides[1].species);
		if (sp == null) return 0.5F;
		Item ball = Item.find(aimBall);
		float bonus = ball == null ? 1F : (float) Math.max(1, ball.catchBonus());
		if (aimBall != null && aimBall.equals("master_ball")) return 1;
		float max = Math.max(1, sides[1].max), hp = Math.max(1, sides[1].hp);
		float a = Math.max(1, (3 * max - 2 * hp) * sp.catchRate() * bonus / (3 * max));
		if (!sides[1].status.isEmpty()) a += sides[1].status.equals("SLP") || sides[1].status.equals("FRZ") ? 10 : 5;
		return Math.min(1, (a + 1) / 256F);
	}

	@Override
	protected void back() {
		switch (mode) {
			case FIGHT, BAG -> mode = Mode.MAIN;
			case PARTY -> {
				if (shifting) mode = Mode.ASK;
				else if (!forced) mode = Mode.MAIN;
			}
			case ASK -> askPick(1);
			case BAG_TARGET, AIM -> mode = Mode.BAG;
			default -> clickAnywhere();
		}
	}

	// ---------- drawing ----------

	/** Streaks running ahead of the white as it sweeps across: height on screen (0 to 1), length, lead. */
	private static final float[][] STREAKS = {{0.08F, 0.5F, 0.1F}, {0.19F, 0.8F, 0.3F}, {0.27F, 0.35F, 0.05F}, {0.36F, 0.9F, 0.42F}, {0.47F, 0.55F, 0.15F},
			{0.55F, 1F, 0.5F}, {0.63F, 0.4F, 0.2F}, {0.72F, 0.75F, 0.35F}, {0.81F, 0.5F, 0.08F}, {0.9F, 0.85F, 0.45F}, {0.96F, 0.3F, 0.25F}};

	/** Fills everything left of a slanted line (leaning forward: the top further on), lead being where it crosses the middle. */
	private void slanted(GuiGraphics g, float lead, int lean, int color) {
		g.pose().pushMatrix();
		g.pose().translate(0, height / 2F);
		g.pose().mul(new org.joml.Matrix3x2f(1, 0, -lean / (float) Math.max(1, height), 1, 0, 0));
		g.fill(-width, -height / 2 - 2, Math.round(lead), height - height / 2 + 2, color);
		g.pose().popMatrix();
	}

	private void drawWipe(GuiGraphics g, float t) {
		int lean = height * 2 / 5;
		if (t < 6) {
			// Two quick flashes, as a wild Pokémon appears.
			float f = (float) Math.abs(Math.sin(t / 6F * Math.PI * 2));
			g.fill(0, 0, width, height, Ui.alpha(0xFFFFFFFF, 0.55F * f));
			return;
		}
		float sweep = Math.min(1, (t - 6) / 12F);
		// Eased: quick through the middle, settling at the end.
		float k = sweep * sweep * (3 - 2 * sweep);
		float lead = -lean + k * (width + 2 * lean + 40 * s);
		// Streaks of light racing ahead of the edge.
		for (int i = 0; i < STREAKS.length; i++) {
			float[] st = STREAKS[i];
			int y = Math.round(height * st[0]);
			float at = lead + (0.5F - st[0]) * lean + st[2] * 70 * s;
			int len = Math.round(st[1] * 90 * s), th = Math.max(1, s + (i % 3 == 0 ? s : 0));
			g.fill(Math.round(at - len), y, Math.round(at), y + th, i % 2 == 0 ? 0xB0FFFFFF : 0x90C8E8FF);
		}
		// A band of pale blue at the edge, then the white.
		slanted(g, lead + 16 * s, lean, 0x70BEE6FF);
		slanted(g, lead + 7 * s, lean, 0xC0E6F6FF);
		slanted(g, lead, lean, 0xFFFFFFFF);
	}

	@Override
	protected void draw(GuiGraphics g, float partial) {
		if (wipe > 0) {
			float t = WIPE - wipe + partial;
			if (wipe > REVEAL) {
				drawWipe(g, t);
				return;
			}
			// The stage is up behind the white: hold, then fade it away.
			float white = t < WIPE - REVEAL + 2 ? 1 : Math.max(0, 1 - (t - (WIPE - REVEAL + 2)) / (REVEAL - 2));
			g.fill(0, 0, width, height, Ui.alpha(0xFFFFFFFF, white));
			if (white > 0.6F) return;
		}
		Ui.cinema(g, width, height, Math.min(1, (onStage + partial) / 14F));
		// A hit's colour glowing round the edges (yellow for a Thunderbolt, orange for fire...); white floods in for the big ones.
		if (BattleScene.flashTicks > 0) {
			float k = Math.max(0, (BattleScene.flashTicks - partial) / BattleScene.flashMax);
			Ui.vignette(g, width, height, BattleScene.flashColor, k);
			if (BattleScene.flashColor == 0xFFFFFF) g.fill(0, 0, width, height, Ui.alpha(0xFFFFFFFF, 0.5F * k));
		}
		drawImpacts(g, partial);
		drawArrows(g, partial);
		int m = 8 * s, bh = 36 * s;
		Side them = sides[1], me = sides[0];
		int by = height - bh - m;
		// The plates (the UI Viewer's): theirs at the top right, yours at the bottom left, the party's balls above each.
		// Out of the way while the bag or your party is up.
		boolean plates = mode != Mode.BAG && mode != Mode.PARTY && mode != Mode.BAG_TARGET;
		if (them.shown && plates) plate(g, them, false);
		if (me.shown && plates) plate(g, me, true);
		if (moneyAt >= 0) moneyBox(g, partial);
		if (levelAfter != null && levelBefore != null) levelPanel(g);
		if (gotchaAt >= 0) V.callout(g, "Gotcha!", 640, 150, (ticks - gotchaAt + partial) / 20F);
		if (message != null && message.endsWith(" wants to battle!") && !kind.equals("WILD")) drawChallenge(g, message.substring(0, message.length() - 17), partial);
		else challengeAt = -1;
		// Your last one's fainted: everything fades to white (the words still over it).
		if (whiteAt >= 0) g.fill(0, 0, width, height, Ui.alpha(0xFFFFFFFF, Math.min(1, (ticks - whiteAt + partial) / 24F)));
		switch (mode) {
			case MAIN -> drawMain(g, m, by, bh);
			case FIGHT -> drawFight(g, m, by, bh);
			case PARTY, BAG_TARGET -> drawParty(g, m, by, bh);
			case BAG -> drawBag(g, m, by, bh);
			case AIM -> drawAim(g, m, by, bh, partial);
			case ASK -> {
				say(g, askLine(), false, partial);
				vchoice(g, ask, 520 + 724 - 300, V.H - 26 - 120 - 12 - V.choiceHeight(2), 300, this::askPick);
			}
			case HOLD -> {
				// The win, held: yours and you celebrating; carry on when you like.
				String who = sides[0].name;
				say(g, "WIN".equals(result) ? who + " is overjoyed!" : who + " cheers for the catch!", false, partial);
				V.hints(g, "A", "Continue");
			}
			default -> say(g, message == null ? "" : message.substring(0, Math.min(typed, message.length())), true, partial);
		}
		if (toDex >= 0 || fromDex > 0) {
			float a = toDex >= 0 ? Math.min(1, (toDex + partial) / DEX_FADE) : Math.max(0, (fromDex - partial) / DEX_FADE);
			g.fill(0, 0, width, height, Ui.alpha(0xFFFFFFFF, a));
		}
	}

	/** When the challenge card came in (ticks), or -1. */
	private int challengeAt = -1;

	/**
	 * "BLUE wants to battle!", as the UI Viewer has it: speed lines streaming across, and the trainer's card sliding
	 * in from the right, a red band leaning forward with gold edges, TRAINER and their name on it.
	 */
	private void drawChallenge(GuiGraphics g, String who, float partial) {
		if (challengeAt < 0) challengeAt = ticks;
		float t = (ticks - challengeAt + partial) / 20F;
		// The speed lines: pale slanted streaks sliding right.
		float shift = (t / 0.5F % 1) * 400;
		for (float x = -400 + shift - V.ox / V.k; x < V.W + V.ox / V.k + 400; x += 400) {
			for (int k2 = 0; k2 < 13; k2++) {
				float lx = x + k2 * 30;
				g.pose().pushMatrix();
				g.pose().translate(V.x(lx), V.y(0));
				g.pose().mul(new org.joml.Matrix3x2f(1, 0, -0.2126F, 1, 0, 0));
				g.fill(0, 0, Math.max(1, Math.round(V.s(4))), Math.round(V.s(V.H)), 0x47FFFFFF);
				g.pose().popMatrix();
			}
		}
		float in = Math.min(1, t / 0.5F), e = 1 - (1 - in) * (1 - in) * (1 - in), x = 1280 - 640 * e, y = 180, w = 640 + V.ox / V.k + 60, h = 52 * 1.15F + 13 * 1.3F + 44 + 8;
		g.pose().pushMatrix();
		g.pose().translate(V.x(x), V.y(y));
		g.pose().mul(new org.joml.Matrix3x2f(1, 0, -0.2126F, 1, 0, 0));
		float k = V.k;
		// The band: clear at its left end, red by a quarter of the way.
		int steps = 32;
		for (int i = 0; i < steps; i++) {
			float t0 = i / (float) steps, a = Math.min(1, t0 / 0.25F) * 0.95F;
			g.fill(Math.round(w * k * t0), 0, Math.round(w * k * (i + 1) / steps), Math.round(h * k), Ui.alpha(0xFFE5525C, a));
		}
		g.fill(0, 0, Math.round(w * k), Math.round(4 * k), V.GOLD);
		g.fill(0, Math.round((h - 4) * k), Math.round(w * k), Math.round(h * k), V.GOLD);
		g.pose().popMatrix();
		V.label(g, "Trainer", x + 160, y + 26, 0xFFFFE6C0);
		V.text(g, who, x + 160 - 6, y + 26 + 20, 52, V.TEXT, true);
	}

	/** Where a side's Pokémon is on the screen (the middle of it), or null. */
	private int[] onScreen(int side, float up) {
		var at = BattleScene.spot(side).add(0, 0.25 + BattleScene.height(side) * up, 0);
		return BattleScene.project(at, width, height);
	}

	/** A bar turned about a point (for the bursts' rays and the arrows). */
	private void ray(GuiGraphics g, float cx, float cy, float angle, float from, float to, float thick, int color) {
		g.pose().pushMatrix();
		g.pose().translate(cx, cy);
		g.pose().rotate(angle);
		g.fill(Math.round(-thick / 2), Math.round(-to), Math.round(thick / 2), Math.round(-from), color);
		g.pose().popMatrix();
	}

	/** Each hit's burst: a white core, rays of light in the move's colour flying out, a ring spreading. */
	private void drawImpacts(GuiGraphics g, float partial) {
		for (BattleScene.Impact im : BattleScene.IMPACTS) {
			int[] c = onScreen(im.side, 0.45F);
			if (c == null) continue;
			float p = Math.min(1, (im.age + partial) / im.life);
			float out = 1 - (1 - p) * (1 - p);
			float big = im.big ? 1.4F : 1F;
			int col = 0xFF000000 | im.color;
			float fade = 1 - p;
			// Core.
			int core = Math.round((10 + 8 * (1 - p)) * s * big);
			Ui.round(g, c[0] - core, c[1] - core, 2 * core, 2 * core, Ui.alpha(0xFFFFFFFF, 0.85F * fade * fade), Math.max(1, core / 2));
			// Rays.
			int n = im.big ? 10 : 8;
			for (int i = 0; i < n; i++) {
				float a = (float) (Math.PI * 2 * i / n + im.side * 0.3 + (i % 2) * 0.12);
				float len = (i % 2 == 0 ? 34 : 24) * s * big;
				ray(g, c[0], c[1], a, out * len * 0.45F, out * len, Math.max(1, (3 - 2 * p) * s), Ui.alpha(i % 2 == 0 ? 0xFFFFFFFF : col, fade));
			}
			// Ring.
			int r = Math.round((12 + 30 * out) * s * big);
			ring(g, c[0], c[1], r, Math.max(1, Math.round(2 * s * fade)), Ui.alpha(col, fade * 0.9F));
		}
	}

	/** Arrows rising over a Pokémon whose stat went up (warm), or falling when it went down (blue). */
	private void drawArrows(GuiGraphics g, float partial) {
		for (BattleScene.Arrows ar : BattleScene.ARROWS) {
			int[] c = onScreen(ar.side, 0.5F);
			if (c == null) continue;
			float p = (ar.age + partial) / BattleScene.Arrows.LIFE;
			float fade = p < 0.2F ? p / 0.2F : p > 0.75F ? (1 - p) / 0.25F : 1;
			int col = Ui.alpha(ar.up ? 0xFFFF7A3A : 0xFF4A8FE0, Math.max(0, fade));
			for (int i = 0; i < 3; i++) {
				for (int j = -1; j <= 1; j += 2) {
					float drift = (p * 28 + i * 9) * s;
					float y = c[1] + (ar.up ? 14 * s - drift : -14 * s + drift);
					float x = c[0] + j * 16 * s;
					// A chevron: two short bars meeting at the point.
					float sz = 6 * s, a = (float) Math.toRadians(ar.up ? 45 : 135);
					ray(g, x, y, a, 0, sz, 2 * s, col);
					ray(g, x, y, -a, 0, sz, 2 * s, col);
				}
			}
		}
	}

	private static final net.minecraft.resources.Identifier PLATE_MINE = com.insidethecube.retromon.Retromon.id("textures/gui/battle/plate_mine.png"),
			PLATE_FOE = com.insidethecube.retromon.Retromon.id("textures/gui/battle/plate_foe.png"),
			BALL_FULL = com.insidethecube.retromon.Retromon.id("textures/gui/battle/ball_full.png"),
			BALL_DOWN = com.insidethecube.retromon.Retromon.id("textures/gui/battle/ball_down.png"),
			BALL_EMPTY = com.insidethecube.retromon.Retromon.id("textures/gui/battle/ball_empty.png"),
			POINTER = com.insidethecube.retromon.Retromon.id("textures/gui/battle/pointer.png");
	/** The round command buttons, by command (0 FIGHT, 1 BAG, 2 POKéMON, 3 RUN). */
	private static final net.minecraft.resources.Identifier[] BUTTONS = {com.insidethecube.retromon.Retromon.id("textures/gui/battle/fight.png"),
			com.insidethecube.retromon.Retromon.id("textures/gui/battle/bag.png"), com.insidethecube.retromon.Retromon.id("textures/gui/battle/pokemon.png"),
			com.insidethecube.retromon.Retromon.id("textures/gui/battle/run.png")};
	private static final int PLATE_W = 156;
	/** Where the message bar starts (beside your plate). */
	private int barX;

	/**
	 * A Pokémon's plate, as the UI Viewer's: a teal glass panel (yours at the bottom left, theirs at the top right);
	 * the name, male or female, any status, and the level; the HP bar (and for yours the numbers and the EXP); the
	 * party's balls above it.
	 */
	private void plate(GuiGraphics g, Side sd, boolean ours) {
		// Yours sits level with the text box beside it (574 to 694), top and bottom.
		float x = ours ? 36 : 820, y = ours ? 574 : 36, w = ours ? 470 : 420, h = ours ? 120 : 92, px = x + 22;
		V.panel(g, x, y, w, h, 22, false);
		float ty = y + (ours ? 15 : 12);
		V.text(g, sd.name, px, ty, 27, V.TEXT, true);
		float nx = px + V.width(sd.name, 27, true) + 10;
		if (!sd.gender.isEmpty()) {
			boolean she = sd.gender.contains("\u2640");
			V.text(g, sd.gender, nx, ty + 6, 20, she ? 0xFFFF8FB8 : 0xFF6FB8FF, true);
			nx += V.width(sd.gender, 20, true) + 10;
		}
		if (!sd.status.isEmpty()) {
			float sw = V.width(sd.status, 13, true) + 16;
			V.rr(g, nx, ty + 7, sw, 22, 7, Ui.statusColor(sd.status));
			V.text(g, sd.status, nx + 8, ty + 10, 13, 0xFFFFFFFF, true);
		}
		V.right(g, "Lv. " + sd.level, x + w - 22, ty + 5, 21, V.TEXT, true);
		float hy = ty + 34 + 8, frac = Math.max(0, Math.min(1, sd.hp / Math.max(1F, sd.max)));
		V.text(g, "HP", px, hy + 2, 14, V.GOLD, true);
		float bw = ours ? 260 : 320;
		V.bar(g, px + 32, hy + 3, bw, 14, frac, V.hpColour(frac));
		if (ours) {
			V.text(g, Math.round(sd.hp) + "/" + sd.max, px + 32 + bw + 12, hy - 1, 20, V.TEXT, true);
			Species sp = Species.ALL.get(sd.species);
			float ef = 0;
			if (sp != null) {
				int now = sp.growth().expAt(sd.level), next = sd.level >= 100 ? now + 1 : sp.growth().expAt(sd.level + 1);
				ef = Math.max(0, Math.min(1, (sd.exp - now) / Math.max(1F, next - now)));
			}
			float ey = hy + 24 + 6;
			V.text(g, "EXP", px, ey - 2, 12, 0xFF6FC3FF, true);
			V.pill(g, px + 42, ey + 1, w - 44 - 42, 7, 0xFF0B2A2C);
			if (ef > 0) V.pill(g, px + 42, ey + 1, Math.max(7, (w - 44 - 42) * ef), 7, 0xFF6FC3FF);
		}
		// The party's balls above the plate: one for each (grey if it's fainted).
		List<Mon> party = ours ? mine : theirs;
		if (!ours && kind.equals("WILD")) return;
		for (int i = 0; i < party.size() && i < 6; i++) {
			float bx = ours ? x + 18 + i * 26 : x + w - 18 - 22 - i * 26;
			V.icon(g, "icon_ball", bx, y - 30, 22, party.get(i).fainted() ? 0x73808890 : 0xFFFFFFFF);
		}
	}

	/** What's said in battle, in the UI Viewer's text box (beside your plate: 520 from the left, 724 wide). */
	private void say(GuiGraphics g, String text, boolean arrow, float partial) {
		List<String> lines = V.wrap(text, 724 - 68, 25, true);
		if (lines.size() > 2) lines = lines.subList(lines.size() - 2, lines.size());
		V.textBox(g, 520, 26, 724, null, lines, arrow && message != null && typed >= message.length(), (ticks + partial) / 20F);
	}

	/** Your money (the viewer's box at the top left) when it changes: counting up, or down, to what you have now. */
	private void moneyBox(GuiGraphics g, float partial) {
		float t = moneyAt + partial, c = Math.min(1, t / MONEY_COUNT), in = Math.min(1, t / 8F);
		int shown = Math.round(moneyFrom + (moneyTo - moneyFrom) * (1 - (1 - c) * (1 - c)));
		float x = 30, y = 30, w = 300, h = 12 + 18 + 36 + 12;
		g.pose().pushMatrix();
		float k = 0.85F + 0.15F * in, px = V.x(x + w / 2), py = V.y(y + h / 2);
		g.pose().translate(px, py);
		g.pose().scale(k, k);
		g.pose().translate(-px, -py);
		V.panel(g, x, y, w, h);
		V.label(g, "Money", x + 22, y + 12);
		V.text(g, Ui.money(shown), x + 22, y + 12 + 18, 30, V.TEXT, true);
		g.pose().popMatrix();
	}

	/** A level gained: the viewer's panel at the right, each stat's new value and what it went up by in green. */
	private void levelPanel(GuiGraphics g) {
		String[] names = {"HP", "ATTACK", "DEFENSE", "SPCL.ATK", "SPCL.DEF", "SPEED"};
		float x = 860, y = 120, w = 380, rh = 24, gap = 8, h = 32 + 6 * rh + 5 * gap;
		V.panel(g, x, y, w, h);
		for (int i = 0; i < 6; i++) {
			float ry = y + 16 + i * (rh + gap);
			V.label(g, names[i], x + 22, ry + 5);
			V.right(g, String.valueOf(levelAfter[i]), x + w - 22 - 70, ry, 20, V.TEXT, true);
			V.right(g, "+" + Math.max(1, levelAfter[i] - levelBefore[i]), x + w - 22, ry, 20, V.EDGE_LIGHT, true);
		}
	}

	/** The party's six Poké Balls in a row: one for each Pokémon (grey if it's fainted), empty slots after. */
	private void balls(GuiGraphics g, List<Mon> party, int x, int y, boolean fromRight) {
		int b = 9 * s, gap = 2 * s;
		for (int i = 0; i < 6; i++) {
			net.minecraft.resources.Identifier id = i >= party.size() ? BALL_EMPTY : party.get(i).fainted() ? BALL_DOWN : BALL_FULL;
			int bx = fromRight ? x - (i + 1) * (b + gap) + gap : x + i * (b + gap);
			Ui.pic(g, id, bx, y, b, b, 48, 48, 0xFFFFFFFF);
		}
	}

	/** The message bar along the bottom (the line typing out, the arrow when it's done). */
	private void messageBar(GuiGraphics g, int x, int y, int w, int h, String text, boolean arrow) {
		float r = 8 * s;
		Ui.rr(g, x + s * 0.6F, y + 2 * s, w, h, r, 0x50000000);
		Ui.rr(g, x, y, w, h, r, 0xFF8FF5C0);
		Ui.rr(g, x + 1.5F * s, y + 1.5F * s, w - 3 * s, h - 3 * s, r - 1.5F * s, 0xF0123C38);
		Ui.rr(g, x + 4 * s, y + 2.5F * s, w - 8 * s, h * 0.4F, r - 3 * s, 0x141D5A52);
		Ui.rr(g, x + 8 * s, y + 2.5F * s, w - 16 * s, Math.max(1, s * 0.6F), 1, 0x30FFFFFF);
		List<String> lines = Ui.wrap(font, text, (w - 24 * s) / s);
		for (int i = 0; i < Math.min(2, lines.size()); i++) Ui.text(g, font, lines.get(i), x + 12 * s, y + 8 * s + i * 12 * s, Ui.TEXT, s);
		if (arrow && message != null && typed >= message.length()) Ui.triangle(g, x + w - 12 * s, y + h - 10 * s + ((ticks / 6) % 2) * s, 7 * s, 2, Ui.CREAM);
	}

	/** A full coloured command or move bar (Let's Go): slanted, lit from the top; the picked one slides out with a white edge. */
	private void bigBar(GuiGraphics g, int x, int y, int w, int h, int color, boolean on, boolean dim) {
		int lean = 7 * s;
		int top = dim ? 0xFF4A5068 : Ui.lighter(color, on ? 0.38F : 0.16F), bottom = dim ? 0xFF2E3346 : Ui.darker(color, on ? 0.92F : 0.7F);
		Ui.slab(g, x + 2 * s, y + 2 * s, w, h, 0x60000000, 0x60000000, lean, 0, 0);
		Ui.slab(g, x, y, w, h, top, bottom, lean, on ? 0xFFFFFFFF : 0x40000000, on ? Math.max(1, s) : Math.max(1, s / 2));
	}

	/**
	 * What to do, as the UI Viewer has it: a stack of wide buttons at the bottom right (FIGHT, POKéMON, BAG, RUN),
	 * each in its colour with a sheen and its own picture, the one you're on slid out with a cream rim and an arrow
	 * pointing at it; the question above them.
	 */
	private void drawMain(GuiGraphics g, int m, int by, int bh) {
		Mon mon = me();
		String[] names = {"FIGHT", "BAG", "POKéMON", "RUN"}, icons = {"cmd_fight", "cmd_bag", "cmd_pokemon", "cmd_run"};
		int[] colours = {0xFFE5525C, 0xFFE9A830, 0xFF36B07A, 0xFF4A8FE0};
		float X = 948, W = 300;
		float[][] at = {{404, 80}, {566, 62}, {494, 62}, {638, 62}};
		for (int i = 0; i < 4; i++) if (vover(X - 14, at[i][0], W + 14, at[i][1])) mainAt = i;
		// The question, right-aligned over the stack.
		String q = "What will " + (mon == null ? "you" : Look.text(mon.name())) + " do?";
		float qw = V.width(q, 19, true) + 44;
		V.panel(g, X + W - qw, 330, qw, 46, 23, true);
		V.draw(g, V.rich(q, true, V.TEXT), X + W - qw + 22, 341, 19, V.TEXT, false);
		for (int i = 0; i < 4; i++) {
			boolean on = i == mainAt, big = i == 0, dim = i == 3 && !kind.equals("WILD");
			float y = at[i][0], hh = at[i][1], x = X - (on ? 14 : 0);
			int c = colours[i];
			if (dim) c = Ui.mix(c, 0xFF707680, 0.6F);
			if (!on) c = Ui.mix(c, 0xFF000000, 0.08F);
			V.rr(g, x, y + 6, W, hh, hh / 2, Ui.darker(c, 0.55F));
			V.rr(g, x, y + 12, W, hh, hh / 2, 0x30000000);
			V.rr(g, x, y, W, hh, hh / 2, on ? V.CREAM : 0xE6FFFFFF);
			// The button's face in one piece (as two halves, each rounded, it showed notches at both ends).
			V.rrStops(g, x + 3, y + 3, W - 6, hh - 6, (hh - 6) / 2, new float[] {0, 0.48F, 1}, new int[] {Ui.lighter(c, 0.18F), c, Ui.darker(c, 0.78F)});
			V.rrv(g, x + 10, y + 4, W - 20, (hh - 6) * 0.42F, (hh - 6) * 0.21F, 0x59FFFFFF, 0x00FFFFFF);
			float ir = big ? 56 : 44, ix = x + (big ? 12 : 10);
			V.rr(g, ix, y + (hh - ir) / 2, ir, ir, ir / 2, Ui.darker(c, 0.7F));
			float is = big ? 40 : 30;
			V.icon(g, icons[i], ix + (ir - is) / 2, y + (hh - is) / 2, is);
			float ts = big ? 32 : 24;
			V.text(g, names[i], ix + ir + 14, y + (hh - ts * 1.15F) / 2 + 2, ts, Ui.darker(c, 0.55F), true);
			V.text(g, names[i], ix + ir + 14, y + (hh - ts * 1.15F) / 2, ts, 0xFFFFFFFF, true);
			int idx = i;
			vhit(x, y, W, hh, () -> pickMain(idx));
			if (on) {
				float bob = (float) Math.sin(ticks * 0.45) * 3.5F - 3.5F;
				V.tri(g, X - 54 + 18 + bob, y + hh / 2 + 2, 30, 0, 0x4D000000);
				V.tri(g, X - 54 + 18 + bob, y + hh / 2, 30, 0, V.INK);
				V.tri(g, X - 54 + 18 + bob - 1, y + hh / 2, 22, 0, V.CREAM);
			}
		}
	}

	private static float dist(double ax, double ay, int bx, int by) {
		return (float) Math.hypot(ax - bx, ay - by);
	}

	/** How a move of this type would do against their Pokémon (Gold and Silver's chart). */
	private String effectiveness(Move mv) {
		Species sp = Species.ALL.get(sides[1].species);
		if (sp == null || mv.power() <= 0) return null;
		double k = mv.type().against(sp.type1(), sp.type2());
		return k == 0 ? "No effect" : k > 1 ? "Super effective" : k < 1 ? "Not very effective" : "Effective";
	}

	/** What a move does, in a few words (the Creature Viewer's move library). */
	private static Map<String, String> moveText;

	private static String moveDesc(String id) {
		if (moveText == null) {
			moveText = new java.util.HashMap<>();
			for (String[] r : Species.table("move_desc.tsv")) if (r.length > 1 && !r[0].startsWith("#")) moveText.put(r[0].trim(), r[1]);
		}
		return moveText.getOrDefault(id, "");
	}

	/**
	 * The moves, as the UI Viewer has them: four pills at the right (the type's colour fading into teal; the one
	 * you're on cream), each with its type, name and PP; above them a panel with what the picked one is: its type and
	 * kind, how it'll do against theirs, its power and accuracy, and what it does.
	 */
	private void drawFight(GuiGraphics g, int m, int by, int bh) {
		Mon mon = me();
		if (mon == null) return;
		int n = mon.moves.size();
		float X = 830, W = 410, Y = 420;
		for (int i = 0; i < 4; i++) {
			float y = Y + i * 58;
			if (i >= n) {
				V.pill(g, X, y, W, 48, 0x60123C38);
				V.centred(g, "-", X + W / 2, y + 12, 21, V.DIM, true);
				continue;
			}
			Move mv = Move.get(mon.moves.get(i));
			if (vover(X, y, W, 48)) fightAt = i;
			boolean on = i == fightAt;
			int col = Ui.typeColor(mv.type()), pp = mon.pp.get(i);
			if (on) {
				V.pill(g, X - 3, y - 3, W + 6, 54, 0x59FFF4D8);
				V.pill(g, X, y, W, 48, 0xFFFFFFFF);
				V.pill(g, X + 3, y + 3, W - 6, 42, V.CREAM);
			} else {
				V.pill(g, X, y, W, 48, 0x668FF5C0);
				// The type's colour fading into teal by the middle.
				V.pill(g, X + 3, y + 3, W - 6, 42, 0xF2123C38);
				float fw = (W - 6) * 0.6F;
				for (int k = 0; k < 24; k++) {
					float t0 = k / 24F;
					V.rr(g, X + 3 + fw * t0, y + 3, fw / 24 + 1, 42, k == 0 ? 21 : 0, Ui.mix(col, 0xF2123C38, t0));
				}
				V.rr(g, X + 3, y + 3, 42, 42, 21, col);
			}
			String type = mv.type().label.toUpperCase();
			String t3 = type.length() > 3 ? type.substring(0, 3) : type;
			float tw = Math.max(44, V.width(t3, 11, true) + 16);
			V.pill(g, X + 10, y + 13, tw, 22, col);
			V.text(g, t3, X + 10 + (tw - V.width(t3, 11, true)) / 2, y + 17, 11, 0xFFFFFFFF, true);
			int tc = on ? V.INK : 0xFFFFFFFF;
			V.text(g, mv.name(), X + 10 + tw + 12, y + 11, 21, tc, true);
			String ppText = pp + "/" + mv.pp();
			V.right(g, ppText, X + W - 18, y + 11, 21, pp == 0 ? 0xFFFF7A7A : on ? V.INK : pp * 4 <= mv.pp() ? 0xFFFFE08A : 0xFFFFFFFF, true);
			int idx = i;
			vhit(X, y, W, 48, () -> pickMove(idx));
		}
		// What the picked move is.
		Move sel = fightAt < n ? Move.get(mon.moves.get(fightAt)) : null;
		if (sel != null) {
			String desc = moveDesc(sel.id());
			List<String> dl = desc.isEmpty() ? List.of() : V.wrap(desc, W - 40, 17, true);
			float ih = 14 + 28 + 8 + 26 + (dl.isEmpty() ? 0 : 6 + dl.size() * 23) + 14, iy = Y - 10 - ih;
			V.panel(g, X, iy, W, ih);
			String type = sel.type().label.toUpperCase();
			float cx = X + 20;
			cx += V.type(g, type, Ui.typeColor(sel.type()), cx, iy + 14, 92, 13.5F) + 10;
			String cat = sel.power() <= 0 ? "STATUS" : sel.type().special() ? "SPECIAL" : "PHYSICAL";
			V.label(g, cat, cx, iy + 20);
			String eff = effectiveness(sel);
			if (eff != null) V.right(g, eff, X + W - 20, iy + 17, 15, eff.equals("Super effective") ? 0xFF8FF5C0 : eff.equals("No effect") || eff.startsWith("Not") ? 0xFFFFB3B8 : V.MUTED, true);
			float sy = iy + 14 + 28 + 8;
			V.label(g, "Power", X + 20, sy + 5);
			V.text(g, sel.power() <= 1 ? "-" : String.valueOf(sel.power()), X + 20 + V.labelWidth("Power") + 8, sy, 18, V.TEXT, true);
			float ax = X + 20 + V.labelWidth("Power") + 8 + V.width("000", 18, true) + 22;
			V.label(g, "Accuracy", ax, sy + 5);
			V.text(g, sel.accuracy() <= 0 ? "-" : String.valueOf(sel.accuracy()), ax + V.labelWidth("Accuracy") + 8, sy, 18, V.TEXT, true);
			for (int i = 0; i < dl.size(); i++) V.text(g, dl.get(i), X + 20, sy + 32 + i * 23, 17, 0xFFE8F4EF, true);
		}
		V.hints(g, "A", "Use", "B", "Back");
	}

	/**
	 * Your Pokémon in battle (Let's Go): a column of slanted plates (picture, name, level, HP, status; the one in
	 * battle marked), the picked one sliding out, and on the right that one in 3D on a stand with its types and moves.
	 */
	private void drawParty(GuiGraphics g, int m, int by, int bh) {
		int top = m + height / 14, bottom = by - 6 * s;
		int n = mine.size(), gap = 3 * s, ph = Math.min(30 * s, (bottom - top - gap * 5) / 6), pw = Math.min(176 * s, (width - 2 * m) / 2);
		partyAt = Math.max(0, Math.min(partyAt, Math.max(0, n - 1)));
		for (int i = 0; i < n; i++) {
			Mon mon = mine.get(i);
			int y = top + i * (ph + gap);
			int idx = i;
			if (over(m, y, pw + 10 * s, ph)) partyAt = i;
			boolean on = i == partyAt, out = i == active, down = mon.fainted();
			int x = m + (on ? 10 * s : 0);
			int c1 = down ? 0xF04A2430 : on ? 0xFFFFF4D8 : 0xF0263252, c2 = down ? 0xF02A141C : on ? 0xFFF0DFB4 : 0xF0121A2E;
			Ui.slab(g, x + s, y + 2 * s, pw, ph, 0x50000000, 0x50000000, 6 * s, 0, 0);
			Ui.slab(g, x, y, pw, ph, c1, c2, 6 * s, on ? 0xFFFFFFFF : 0x40FFFFFF, on ? s : Math.max(1, s / 2));
			Ui.portrait(g, mon.species.id(), x + 5 * s, y + s, ph - 2 * s);
			int tx = x + ph + 6 * s, ink = on ? Ui.CREAM_INK : Ui.TEXT;
			Ui.draw(g, font, Ui.bold(mon.name()), tx, y + 4 * s, ink, s, false);
			String lv = "Lv" + mon.level;
			Ui.draw(g, font, Ui.ui(lv), x + pw - 6 * s - Ui.width(font, lv, s), y + 4 * s, on ? 0xFF7A6A40 : Ui.GOLD, s, false);
			float frac = mon.hp / Math.max(1F, mon.maxHp());
			int hx = tx, hw = pw - (tx - x) - 54 * s;
			Ui.bar(g, hx, y + ph - 10 * s, hw, 4 * s, frac, Ui.hpColor(frac), s);
			String hp = mon.hp + "/" + mon.maxHp();
			Ui.draw(g, font, Ui.ui(hp), x + pw - 6 * s - Ui.width(font, hp, s), y + ph - 12 * s, on ? 0xFF4A5068 : Ui.MUTED, s, false);
			if (!mon.status.tag.isEmpty()) Ui.chip(g, font, mon.status.tag, tx + Ui.boldWidth(font, mon.name(), s) + 4 * s, y + 3 * s, Ui.statusColor(mon.status.tag), s);
			if (out) Ui.chip(g, font, "IN BATTLE", x + pw - 54 * s, y + 3 * s, 0xFF4A8FE0, s);
			hit(m, y, pw + 10 * s, ph, () -> pickParty(idx));
		}
		// The picked one, in 3D on a stand, with its types and moves.
		if (partyAt < n) {
			Mon mon = mine.get(partyAt);
			int rx = m + pw + 22 * s, rw = width - m - rx, cx = rx + rw / 2;
			int stand = Math.min(bottom - top - 60 * s, rw * 3 / 5), feet = top + 14 * s + stand;
			Ui.glow(g, cx, feet - stand / 2, rw / 2, stand / 2 + 6 * s, 0x0CFFFFFF);
			Ui.round(g, cx - rw / 3, feet - 5 * s, rw * 2 / 3, 10 * s, 0x90263252, 5 * s);
			Ui.round(g, cx - rw / 4, feet - 3 * s, rw / 2, 6 * s, 0x60FFFFFF, 3 * s);
			Figures.Batch b = Figures.batch(rx, top, width - m, bottom);
			b.mon(mon.species.id(), cx, feet, stand * 0.8F).at((ticks) / 20F).pitch(8).facing(-20).spin((float) Math.sin(ticks / 30.0) * 12);
			b.draw(g);
			int ty = feet + 10 * s, chx = rx + 6 * s;
			chx += Ui.chip(g, font, mon.species.type1().label.toUpperCase(), chx, ty, Ui.typeColor(mon.species.type1()), s) + 3 * s;
			if (mon.species.type2() != null && mon.species.type2() != mon.species.type1())
				Ui.chip(g, font, mon.species.type2().label.toUpperCase(), chx, ty, Ui.typeColor(mon.species.type2()), s);
			int mw = (rw - 16 * s) / 2;
			for (int i = 0; i < mon.moves.size(); i++) {
				Move mv = Move.get(mon.moves.get(i));
				int mx2 = rx + 6 * s + (i % 2) * (mw + 4 * s), my2 = ty + 14 * s + (i / 2) * 14 * s;
				if (my2 + 12 * s > bottom) break;
				int col = Ui.typeColor(mv.type());
				Ui.slab(g, mx2, my2, mw - 4 * s, 12 * s, Ui.lighter(col, 0.1F), Ui.darker(col, 0.75F), 4 * s, 0, 0);
				Ui.draw(g, font, Ui.ui(mv.name()), mx2 + 6 * s, my2 + 2 * s, 0xFFFFFFFF, s, true);
			}
		}
		String prompt = mode == Mode.BAG_TARGET ? "Use it on which POKéMON?" : forced ? "Bring out which POKéMON?" : "Choose a POKéMON.";
		messageBar(g, m, by, width - 2 * m - (forced ? 0 : 50 * s), bh, prompt, false);
		if (!forced) {
			int back = 44 * s, bx = width - m - back;
			Ui.button(g, font, bx, by, back, bh, "BACK", 0xFF5A6070, over(bx, by, back, bh), s);
			hit(bx, by, back, bh, this::back);
		}
	}

	/** A ring of thick dots (the GUI has no circles). */
	private void ring(GuiGraphics g, int cx, int cy, int r, int thick, int color) {
		int n = Math.max(24, r / Math.max(1, s));
		for (int i = 0; i < n; i++) {
			double a = Math.PI * 2 * i / n;
			int x = cx + (int) Math.round(Math.cos(a) * r), y = cy + (int) Math.round(Math.sin(a) * r);
			g.fill(x - thick / 2, y - thick / 2, x + (thick + 1) / 2, y + (thick + 1) / 2, color);
		}
	}

	/** Aiming: a white ring round the wild Pokémon and a coloured one closing inside it (green easy, red hard). */
	private void drawAim(GuiGraphics g, int m, int by, int bh, float partial) {
		var at = BattleScene.enemySpot().add(0, BattleScene.enemyHeight() * 0.5, 0);
		int[] c = BattleScene.project(at, width, height);
		if (c != null) {
			int[] top = BattleScene.project(at.add(0, BattleScene.enemyHeight() * 0.6, 0), width, height);
			int r = top == null ? 30 * s : Math.max(18 * s, Math.abs(c[1] - top[1]) + 10 * s);
			float chance = catchChance();
			int col = chance >= 0.5F ? 0xFF5CD65C : chance >= 0.25F ? 0xFFF2C12E : chance >= 0.1F ? 0xFFF07D2A : 0xFFE5525C;
			float phase = ((ticks + partial) % 40) / 40F;
			int inner = (int) (r * (1 - 0.7F * phase));
			ring(g, c[0], c[1], r, 2 * s, 0xE6FFFFFF);
			ring(g, c[0], c[1], inner, 3 * s, col);
			Ui.centred(g, font, chance >= 0.5F ? "Easy" : chance >= 0.25F ? "Fair" : chance >= 0.1F ? "Hard" : "Very hard", c[0], c[1] + r + 6 * s, col, s, true);
		}
		Item it = Item.find(aimBall);
		float tw = V.width("Press  or click to throw!", 22, true) + V.width("SPACE", 14, true) + 14 + 44;
		V.panel(g, 640 - tw / 2, 96, tw, 52, 26, false);
		float tx = 640 - tw / 2 + 22;
		V.text(g, "Press", tx, 108, 22, V.TEXT, true);
		tx += V.width("Press ", 22, true);
		tx += V.key(g, "SPACE", tx, 108, 28) + 6;
		V.text(g, "or click to throw!", tx, 108, 22, V.TEXT, true);
		V.hints(g, "A", "Throw " + (it == null ? "BALL" : it.name()), "B", "Back");
	}

	private static Map<String, String> itemText;

	/** What an item does, in the games' words. */
	static String describe(String id) {
		if (itemText == null) {
			itemText = new java.util.HashMap<>();
			for (String[] r : Species.table("item_desc.tsv")) if (r.length > 1 && !r[0].startsWith("#")) itemText.put(r[0].trim(), r[1]);
		}
		return itemText.getOrDefault(id, "");
	}

	/** A little picture of an item: a ball in its colours, a bottle for medicine, a gem for the rest. */
	static void itemIcon(GuiGraphics g, Item it, int x, int y, int size, int s) {
		if (it.ball()) {
			int top = switch (it.id()) {
				case "great_ball" -> 0xFF3F7FE0;
				case "ultra_ball" -> 0xFF2A2A2E;
				case "master_ball" -> 0xFF8A4AC8;
				case "safari_ball" -> 0xFF6A9A40;
				default -> 0xFFE5404A;
			};
			Ui.round(g, x, y, size, size, 0xFFF4F4F4, size / 2);
			Ui.round(g, x, y, size, size / 2 + 1, top, size / 2);
			g.fill(x + 1, y + size / 2 - Math.max(1, s / 2), x + size - 1, y + size / 2 + Math.max(1, s / 2), 0xFF1E1E22);
			if (it.id().equals("ultra_ball")) {
				g.fill(x + size / 4, y + 1, x + size / 4 + Math.max(1, s), y + size / 2 - 1, 0xFFF2C230);
				g.fill(x + size * 3 / 4 - Math.max(1, s), y + 1, x + size * 3 / 4, y + size / 2 - 1, 0xFFF2C230);
			}
			Ui.round(g, x + size / 2 - size / 6, y + size / 2 - size / 6, size / 3, size / 3, 0xFFFFFFFF, Math.max(1, size / 6));
			return;
		}
		boolean heal = it.id().contains("potion") || it.id().contains("restore") || it.id().contains("revive") || it.id().contains("water")
				|| it.id().contains("lemonade") || it.id().contains("soda") || it.id().contains("berry") || it.id().contains("heal") || it.id().contains("antidote")
				|| it.id().contains("awakening");
		if (heal) {
			int col = it.id().contains("revive") ? 0xFFF2C230 : it.id().contains("hyper") ? 0xFFE5525C : it.id().contains("super") ? 0xFFF07DB0
					: it.id().contains("max") || it.id().contains("restore") ? 0xFF4A8FE0 : it.id().contains("potion") ? 0xFFB070E0 : 0xFF5CC88A;
			Ui.round(g, x + size / 3, y, size / 3, size / 4, 0xFFDADAE2, Math.max(1, s / 2));
			Ui.round(g, x + size / 6, y + size / 5, size * 2 / 3, size * 4 / 5, col, size / 4);
			Ui.round(g, x + size / 6 + s, y + size / 5 + s, size / 5, size / 3, 0x60FFFFFF, Math.max(1, s / 2));
			return;
		}
		Ui.round(g, x + size / 6, y + size / 6, size * 2 / 3, size * 2 / 3, 0xFF7AD0E0, size / 4);
	}

	/**
	 * The bag in battle, in the UI Viewer's look: a panel over the stage (dimmed behind it), its two pockets as big
	 * coloured cards down the left (POKé BALLS red, MEDICINE green, with how many kinds are in each), what's in the
	 * picked pocket on the right with how many you have, what the picked one does underneath, and the keys as buttons
	 * (ESC Back closes it). An empty pocket says so, and where to get some.
	 */
	private void drawBag(GuiGraphics g, int m, int by, int bh) {
		List<String> l = battleItems();
		bagAt = Math.max(0, Math.min(bagAt, l.size() - 1));
		g.fill(0, 0, width, height, 0x8C061A1E);
		float px = 190, py = 70, pw = 900, ph = 540;
		V.panel(g, px, py, pw, ph);
		V.icon(g, "icon_bag", px + 30, py + 20, 44);
		V.text(g, "BAG", px + 86, py + 26, 30, V.TEXT, true);
		String[] pockets = {"POKé BALLS", "MEDICINE"};
		int[][] pc = {{0xFFF27682, 0xFFD94452, 0xFFB02E3C}, {0xFF6BDCA2, 0xFF39B27A, 0xFF22855A}};
		for (int i = 0; i < 2; i++) {
			float tx = px + 30, ty = py + 90 + i * 128, tw = 300, th = 108;
			boolean on = bagPocket == i;
			int kinds = count(i);
			if (on) V.rr(g, tx - 5, ty - 5, tw + 10, th + 10, 31, 0x73FFF4D8);
			V.rr(g, tx, ty + 5, tw, th, 26, 0x40000000);
			V.rrStops(g, tx, ty, tw, th, 26, new float[] {0, 0.5F, 1}, on ? pc[i] : new int[] {Ui.mix(pc[i][0], 0xFF123C38, 0.55F), Ui.mix(pc[i][1], 0xFF123C38, 0.55F),
					Ui.mix(pc[i][2], 0xFF123C38, 0.55F)});
			if (on) V.ring(g, tx, ty, tw, th, 26, 3, V.CREAM);
			V.ellipse(g, tx + 54, ty + th / 2, 36, 36, on ? 0xFFFFFFFF : 0xB3FFFFFF);
			itemIcon(g, Item.find(i == 0 ? "poke_ball" : "potion"), Math.round(V.x(tx + 54 - 22)), Math.round(V.y(ty + th / 2 - 22)), Math.round(V.s(44)), 1);
			V.text(g, pockets[i], tx + 104, ty + 24, 24, on ? 0xFFFFFFFF : 0xFFCFEDE3, true);
			V.text(g, kinds == 0 ? "Empty" : kinds + (kinds == 1 ? " kind" : " kinds"), tx + 104, ty + 58, 19, on ? 0xE6FFFFFF : 0xB3CFEDE3, true);
			int idx = i;
			vhit(tx, ty, tw, th, () -> {
				if (bagPocket != idx) pocket(idx - bagPocket);
			});
		}
		// What's in it.
		float lx = px + 360, ly = py + 90, lw = pw - 390, rowH = 54, gap = 8;
		int rows = 6, first = Math.max(0, Math.min(bagAt - rows / 2, l.size() - rows));
		for (int k = 0; k < rows && first + k < l.size(); k++) {
			int i = first + k;
			float ry = ly + k * (rowH + gap);
			if (vover(lx, ry, lw, rowH)) bagAt = i;
			boolean on = i == bagAt;
			Item it = Item.find(l.get(i));
			V.goods(g, it.name(), "x" + ClientState.data.count(it.id()), lx, ry, lw, rowH, on, 21);
			int idx = i;
			vhit(lx, ry, lw, rowH, () -> pickItem(idx));
		}
		if (l.isEmpty()) {
			String none = bagPocket == 0 ? "No POKé BALLS yet. A POKé MART sells them." : "No medicine to use in battle.";
			for (String line : V.wrap(none, lw - 20, 21, true)) {
				V.text(g, line, lx + 10, ly + 14, 21, V.MUTED, true);
				ly += 30;
			}
		}
		// What the picked one does.
		float dy = py + ph - 30 - 92;
		V.panel(g, lx, dy, lw, 92, 20, true);
		String desc = bagAt < l.size() ? describe(l.get(bagAt)) : bagPocket == 0 ? "Throw one at a wild POKéMON to catch it." : "Heal or help your POKéMON.";
		List<String> lines = V.wrap(desc, lw - 48, 19, true);
		for (int i = 0; i < Math.min(2, lines.size()); i++) V.text(g, lines.get(i), lx + 24, dy + 18 + i * 28, 19, V.SOFT_TEXT, true);
		V.hints(g, "A", "Use", "B", "Back", "TAB", "Pocket");
	}

	/** How many kinds of thing are in a pocket. */
	private int count(int pocket) {
		int keep = bagPocket;
		bagPocket = pocket;
		int n = battleItems().size();
		bagPocket = keep;
		return n;
	}

}
