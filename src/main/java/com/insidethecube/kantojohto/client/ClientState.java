package com.insidethecube.kantojohto.client;

import java.util.ArrayList;
import java.util.List;

import com.insidethecube.kantojohto.game.PlayerData;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/** What your screen knows of the game: your data as the game last sent it, and what it's asked to show. */
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

	public static void receive(String kind, CompoundTag t) {
		Minecraft mc = Minecraft.getInstance();
		switch (kind) {
			case "mode" -> Adventure.setOn(t.getBooleanOr("on", false), t.getBooleanOr("save", false));
			case "sync" -> data = PlayerData.load(t);
			case "intro" -> {
				introPending = true;
				introDelay = 30;
			}
			case "dialogue" -> {
				List<String> lines = new ArrayList<>();
				ListTag l = t.getListOrEmpty("lines");
				for (Tag e : l) e.asString().ifPresent(lines::add);
				String mode = t.getStringOr("mode", "talk");
				int npc = t.getIntOr("npc", -1);
				String trainer = t.getStringOr("trainer", "");
				if (mode.equals("battle")) battleNpc = npc;
				Adventure.frozen = 0;
				exclaimTicks = 0;
				mc.setScreen(new DialogueScreen(lines, mode, npc, trainer.isEmpty() ? null : trainer));
			}
			case "exclaim" -> {
				exclaimNpc = t.getIntOr("npc", -1);
				exclaimTicks = 40;
				Adventure.frozen = 200;
				Sounds.spotted();
			}
			case "mart" -> mc.setScreen(new MartScreen());
			case "battle_start" -> {
				String enemy = "pidgey";
				ListTag theirs = t.getListOrEmpty("theirs");
				int at = t.getIntOr("enemy", 0);
				if (at < theirs.size() && theirs.get(at) instanceof CompoundTag c) enemy = c.getStringOr("sp", "pidgey");
				BattleScene.start(t.getStringOr("kind", "WILD").equals("TRAINER") ? battleNpc : -1, enemy);
				battleNpc = -1;
				mc.setScreen(new BattleScreen(t));
			}
			case "battle_turn" -> {
				if (mc.screen instanceof BattleScreen b) b.feed(t);
			}
			case "sound" -> {
				if (t.getStringOr("sound", "").equals("heal")) Sounds.heal();
			}
			default -> {
			}
		}
	}

	public static void tick(Minecraft mc) {
		if (exclaimTicks > 0) exclaimTicks--;
		if (mc.player == null) {
			introPending = false;
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
	}
}
