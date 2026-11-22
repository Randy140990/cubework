package com.insidethecube.retromon.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Supplier;

import com.insidethecube.retromon.game.Net;
import com.insidethecube.retromon.game.PlayerData;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/**
 * What your screen knows of the game: your data as the game last sent it, and what it's asked to show. Screens
 * that come one after another (a catch's nickname, then an evolution, then a new move) wait their turn.
 */
public final class ClientState {
	private ClientState() {}

	public static PlayerData data = new PlayerData();
	/** The professor's welcome is waiting for the world to be on screen. */
	private static boolean introPending, introStopped, introLab;
	private static int introDelay;
	/** The trainer who's just spotted you, and how long the "!" stays up. */
	public static int exclaimNpc = -1, exclaimTicks;
	/** The trainer whose battle is about to begin (from their text box). */
	private static int battleNpc = -1;
	/** The place you're in: its name for the banner, and how long the banner's been up. */
	public static String placeName = "";
	/** On a SAFARI GAME (15:29): steps and SAFARI BALLs left (steps -1: not on one). */
	public static int safariSteps = -1, safariBalls = 0;
	public static int placeTicks = -1;
	private static final Deque<Supplier<Screen>> QUEUE = new ArrayDeque<>();
	/** The places marked on the map (towns, routes): where each is, for the Town Map. */
	public record Zone(String place, int x1, int z1, int x2, int z2) {}

	public static java.util.List<Zone> zones = java.util.List.of();
	/** Your name, your rival's and boy or girl from the new-game intro, sent once the world's loaded. */
	public static CompoundTag pendingProfile;

	public static void receive(String kind, CompoundTag t) {
		Minecraft mc = Minecraft.getInstance();
		switch (kind) {
			case "mode" -> {
				boolean save = t.getBooleanOr("save", false);
				Adventure.setOn(t.getBooleanOr("on", false), save);
				if (save) {
					Night.send();
					Shaders.enter();
					// The brighter greens are worked into the map as it's drawn: draw it again.
					if (mc.levelRenderer != null) mc.levelRenderer.allChanged();
				}
			}
			case "sync" -> {
				data = PlayerData.load(t);
				if (t.contains("zones")) {
					java.util.List<Zone> zs = new java.util.ArrayList<>();
					for (Tag e : t.getListOrEmpty("zones")) {
						if (!(e instanceof CompoundTag z)) continue;
						zs.add(new Zone(z.getStringOr("place", ""), z.getIntOr("x1", 0), z.getIntOr("z1", 0), z.getIntOr("x2", 0), z.getIntOr("z2", 0)));
					}
					zones = java.util.List.copyOf(zs);
				}
				if (pendingProfile != null) {
					ClientPlayNetworking.send(new Net.Act("profile", pendingProfile));
					pendingProfile = null;
				}
			}
			// Through a door, down the stairs: the screen goes black and comes back (the server moves you at the darkest).
			case "fade" -> Hud.fadeThrough(null, "");
			case "intro" -> {
				introPending = true;
				introStopped = t.getBooleanOr("stop", false);
				introLab = t.getBooleanOr("lab", false);
				introDelay = introStopped || introLab ? 2 : 30;
			}
			case "safari" -> {
				safariSteps = t.getIntOr("steps", -1);
				safariBalls = t.getIntOr("balls", 0);
			}
			case "room" -> {
				// A building's room (or back outside): its music; its name in the banner, coming in from outside.
				String name = t.getStringOr("name", "");
				if (!name.isEmpty()) {
					placeName = name;
					placeTicks = 0;
				}
				// Back outside: the place's name back on the pill (no banner: you've only been indoors).
				String here = t.getStringOr("here", "");
				if (!here.isEmpty() && name.isEmpty()) {
					placeName = here;
					if (placeTicks >= 0 && placeTicks < 100) placeTicks = 100;
				}
				String music = t.getStringOr("music", "");
				if (!music.isEmpty()) Music.setPlace(music);
			}
			case "release" -> {
				// Someone who was coming over won't now (you went through a door): you can move again.
				Adventure.frozen = 0;
				exclaimTicks = 0;
				exclaimNpc = -1;
				Adventure.focus(-1);
			}
			case "place" -> {
				placeName = t.getStringOr("name", "");
				placeTicks = 0;
				Postcards.entered(t.getStringOr("id", ""));
				Music.setPlace(t.getStringOr("music", ""));
			}
			case "dialogue" -> {
				List<String> lines = new ArrayList<>();
				ListTag l = t.getListOrEmpty("lines");
				for (Tag e : l) e.asString().ifPresent(lines::add);
				String mode = t.getStringOr("mode", "talk");
				int npc = t.getIntOr("npc", -1);
				if (mode.equals("battle") || mode.equals("rival")) battleNpc = npc;
				Adventure.frozen = 0;
				exclaimTicks = 0;
				// A message while the menu's open (an item's effect) shows in the menu.
				if (npc < 0 && mode.equals("talk") && mc.screen instanceof MenuScreen menu) {
					menu.message(lines);
					CompoundTag done = new CompoundTag();
					done.putString("mode", "talk");
					ClientPlayNetworking.send(new Net.Act("dialogue_done", done));
					return;
				}
				show(() -> new DialogueScreen(lines, mode, npc, t));
			}
			case "follow" -> {
				// Walking after someone (the GYM's guide in PEWTER): your feet take you to the spot behind him.
				Adventure.followX = t.getDoubleOr("x", 0);
				Adventure.followZ = t.getDoubleOr("z", 0);
				Adventure.followTicks = t.getIntOr("ticks", 8);
			}
			case "hold" -> {
				// Still a cutscene (someone walking off): the black bars stay, and you stand still.
				Adventure.frozen = Math.max(Adventure.frozen, t.getIntOr("ticks", 10));
			}
			case "exclaim" -> {
				exclaimNpc = t.getIntOr("npc", -1);
				exclaimTicks = 40;
				Adventure.frozen = 200;
				Sounds.spotted(t.getStringOr("music", ""));
				// The camera frames the two of you (black bars in), and you turn to face them.
				Adventure.focus(exclaimNpc);
				net.minecraft.world.entity.Entity who = mc.level == null ? null : mc.level.getEntity(exclaimNpc);
				if (who != null && mc.player != null) {
					float yaw = (float) Math.toDegrees(Math.atan2(-(who.getX() - mc.player.getX()), who.getZ() - mc.player.getZ()));
					mc.player.setYRot(yaw);
					mc.player.setYHeadRot(yaw);
					mc.player.yBodyRot = yaw;
				}
			}
			case "mart" -> show(() -> new MartScreen(t.getStringOr("mart", "")));
			case "battle_start" -> {
				List<String> enemies = new ArrayList<>();
				for (Tag e : t.getListOrEmpty("theirs")) if (e instanceof CompoundTag c) enemies.add(c.getStringOr("sp", "pidgey"));
				BattleScene.start(t.getStringOr("kind", "WILD").equals("TRAINER") ? battleNpc : -1, enemies, t.getStringOr("arena", "route"));
				battleNpc = -1;
				GameScreen.resume = null;
				mc.setScreen(new BattleScreen(t));
			}
			case "battle_turn" -> {
				if (BattleScreen.current != null) BattleScreen.current.feed(t);
			}
			case "nickname" -> QUEUE.add(() -> new NicknameScreen(t));
			case "evolve" -> QUEUE.add(() -> new EvolutionScreen(t));
			case "learn" -> QUEUE.add(() -> new LearnScreen(t));
			case "trade" -> QUEUE.add(() -> new TradeScreen(t));
			case "sound" -> {
				switch (t.getStringOr("sound", "")) {
					case "heal" -> Sounds.heal();
					case "caught" -> Sounds.caught();
					case "item" -> Music.jingle("get_item");
					case "key" -> Music.jingle("get_key_item");
					default -> {
					}
				}
			}
			default -> {
			}
		}
	}

	/** A screen now, or after what's waiting (a battle's follow-ups) if something's on screen. */
	private static void show(Supplier<Screen> screen) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.screen == null || mc.screen instanceof MenuScreen || mc.screen instanceof DialogueScreen && QUEUE.isEmpty()) mc.setScreen(screen.get());
		else QUEUE.add(screen);
	}

	/** A battle's screen has closed: the server's told once what follows it (nickname, moves, evolving) is done. */
	private static boolean closing;

	public static void battleClosing() {
		closing = true;
	}

	public static void tick(Minecraft mc) {
		if (closing && mc.player != null && mc.screen == null && QUEUE.isEmpty()) {
			closing = false;
			ClientPlayNetworking.send(new Net.Act("battle_closed", new CompoundTag()));
		}
		if (exclaimTicks > 0) exclaimTicks--;
		if (placeTicks >= 0) placeTicks++;
		if (mc.player == null) {
			introPending = false;
			QUEUE.clear();
			return;
		}
		// Back from Minecraft's menu (Esc) to the screen that was up: a battle, a conversation, the menu.
		if (mc.screen == null && GameScreen.resume != null) {
			GameScreen r = GameScreen.resume;
			GameScreen.resume = null;
			if (!(r instanceof BattleScreen) || BattleScreen.current == r) {
				mc.setScreen(r);
				return;
			}
		}
		if (mc.screen == null && !QUEUE.isEmpty()) {
			mc.setScreen(QUEUE.poll().get());
			return;
		}
		if (introPending && mc.screen == null && --introDelay <= 0) {
			introPending = false;
			mc.setScreen(introLab ? StarterScreen.inLab() : new StarterScreen(introStopped));
		}
	}

	public static void reset() {
		data = new PlayerData();
		introPending = false;
		exclaimTicks = 0;
		battleNpc = -1;
		placeName = "";
		placeTicks = -1;
		QUEUE.clear();
		GameScreen.resume = null;
		BattleScreen.current = null;
		zones = java.util.List.of();
		Device.reset();
		Postcards.reset();
	}
}
