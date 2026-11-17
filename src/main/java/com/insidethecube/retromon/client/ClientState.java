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
	private static boolean introPending;
	private static int introDelay;
	/** The trainer who's just spotted you, and how long the "!" stays up. */
	public static int exclaimNpc = -1, exclaimTicks;
	/** The trainer whose battle is about to begin (from their text box). */
	private static int battleNpc = -1;
	/** The place you're in: its name for the banner, and how long the banner's been up. */
	public static String placeName = "";
	public static int placeTicks = -1;
	private static final Deque<Supplier<Screen>> QUEUE = new ArrayDeque<>();

	public static void receive(String kind, CompoundTag t) {
		Minecraft mc = Minecraft.getInstance();
		switch (kind) {
			case "mode" -> {
				boolean save = t.getBooleanOr("save", false);
				Adventure.setOn(t.getBooleanOr("on", false), save);
				if (save) Shaders.enter();
			}
			case "sync" -> data = PlayerData.load(t);
			case "intro" -> {
				introPending = true;
				introDelay = 30;
			}
			case "place" -> {
				placeName = t.getStringOr("name", "");
				placeTicks = 0;
				Music.setPlace(t.getStringOr("music", ""));
			}
			case "dialogue" -> {
				List<String> lines = new ArrayList<>();
				ListTag l = t.getListOrEmpty("lines");
				for (Tag e : l) e.asString().ifPresent(lines::add);
				String mode = t.getStringOr("mode", "talk");
				int npc = t.getIntOr("npc", -1);
				if (mode.equals("battle")) battleNpc = npc;
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
			case "exclaim" -> {
				exclaimNpc = t.getIntOr("npc", -1);
				exclaimTicks = 40;
				Adventure.frozen = 200;
				Sounds.spotted(t.getStringOr("music", ""));
			}
			case "mart" -> show(() -> new MartScreen(t.getStringOr("mart", "")));
			case "battle_start" -> {
				List<String> enemies = new ArrayList<>();
				for (Tag e : t.getListOrEmpty("theirs")) if (e instanceof CompoundTag c) enemies.add(c.getStringOr("sp", "pidgey"));
				BattleScene.start(t.getStringOr("kind", "WILD").equals("TRAINER") ? battleNpc : -1, enemies);
				battleNpc = -1;
				mc.setScreen(new BattleScreen(t));
			}
			case "battle_turn" -> {
				if (mc.screen instanceof BattleScreen b) b.feed(t);
			}
			case "nickname" -> QUEUE.add(() -> new NicknameScreen(t));
			case "evolve" -> QUEUE.add(() -> new EvolutionScreen(t));
			case "learn" -> QUEUE.add(() -> new LearnScreen(t));
			case "sound" -> {
				switch (t.getStringOr("sound", "")) {
					case "heal" -> Sounds.heal();
					case "caught" -> Sounds.caught();
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

	public static void tick(Minecraft mc) {
		if (exclaimTicks > 0) exclaimTicks--;
		if (placeTicks >= 0) placeTicks++;
		if (mc.player == null) {
			introPending = false;
			QUEUE.clear();
			return;
		}
		if (mc.screen == null && !QUEUE.isEmpty()) {
			mc.setScreen(QUEUE.poll().get());
			return;
		}
		if (introPending && mc.screen == null && --introDelay <= 0) {
			introPending = false;
			mc.setScreen(new StarterScreen());
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
	}
}
