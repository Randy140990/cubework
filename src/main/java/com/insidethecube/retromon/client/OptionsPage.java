package com.insidethecube.retromon.client;

import java.util.List;

import com.insidethecube.retromon.game.Net;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.nbt.CompoundTag;

/**
 * OPTIONS, as the UI Viewer's (signed off 20 Nov), from the save slots and from the menu alike: on its own lime
 * backdrop, what the row you're on does in a panel at the top, then the rows (the one you're on in sunny yellow,
 * the rest ruled apart), each with its value between red arrows; left and right (or a click, or A) change it.
 * CONTROLS opens the key map. The viewer's eight, then the mod's own (walking with you, the creatures' look, the
 * light, the day going by, and for looking round quickly BATTLES and FAST TRAVEL), then CONTROLS.
 */
public final class OptionsPage {
	public static final String[] LABELS = {"TEXT SPEED", "MUSIC", "SOUND", "POKéDEX VOICE", "NIGHTS", "WILD POKéMON", "BATTLE EFFECTS", "BATTLE STYLE", "WALK WITH YOU",
			"POKéMON LOOK", "SHADERS", "DAY AND NIGHT", "BATTLES", "FAST TRAVEL", "CONTROLS"};
	private static final String[] LINES = {"How quickly words appear in text boxes.", "How loud the music plays.", "How loud cries, steps and menu sounds are.",
			"Whether the POKéDEX reads its entries out loud.", "How dark nights get outside.", "Whether wild POKéMON show in the grass, or jump out at you.",
			"Whether moves play their animations in battle.", "SWITCH asks if you want to change POKéMON when a trainer's sends out the next one.",
			"Whether your partner walks with you.", "COBBLEMON shows the POKéMON as Cobblemon draws them; RETROMON shows our own creatures.",
			"RETROMON is our own light: the sun moves with the day, soft shadows, swaying grass, blue nights and glowing lamps. OFF is Minecraft's own.",
			"ON lets the day go by, morning to night. OFF holds it at half past ten in the morning (the day count waits too).",
			"OFF: no wild POKéMON jump out and trainers let you by, to look round quickly. Your rival's and TEAM ROCKET's battles still happen.",
			"ON: the TOWN MAP takes you anywhere. A on any place goes there, or right-click any spot on the map.",
			"See every key: what it does in game, and what's turned off."};
	private static final String[][] VALUES = {{"SLOW", "MID", "FAST"}, {"OFF", "LOW", "MID", "HIGH", "MAX"}, {"OFF", "LOW", "MID", "HIGH", "MAX"}, {"OFF", "ON"},
			{"DARK", "SOFT", "BRIGHT"}, {"HIDDEN", "SEEN"}, {"ON", "OFF"}, {"SWITCH", "SET"}, {"OFF", "ON"}, {"COBBLEMON", "RETROMON"}, {"OFF", "RETROMON"}, {"ON", "OFF"}, {"ON", "OFF"}, {"OFF", "ON"}, {}};
	public int row;
	/** Opens the key map (from CONTROLS). */
	private final Runnable keymap;

	public OptionsPage(Runnable keymap) {
		this.keymap = keymap;
	}

	private static int[] now() {
		return new int[] {ClientState.data.textSpeed, Music.level(), Settings.sound(), Settings.voice() ? 1 : 0, Settings.nights(), Settings.seen() ? 1 : 0,
				Settings.effects() ? 0 : 1, Settings.switchStyle() ? 0 : 1, Settings.follower() ? 1 : 0, Settings.look(), Settings.shaderMode(), Settings.cycle() ? 0 : 1,
				Settings.battles() ? 0 : 1, Settings.fastTravel() ? 1 : 0, 0};
	}

	private static boolean online() {
		return Minecraft.getInstance().getConnection() != null;
	}

	/** Changes a row by a step (left -1, right or A +1); CONTROLS opens the key map. */
	public void change(int r, int step) {
		switch (r) {
			case 0 -> {
				int sp = Math.floorMod(ClientState.data.textSpeed + step, 3);
				ClientState.data.textSpeed = sp;
				Settings.set("textSpeed", sp);
				if (online()) {
					CompoundTag t = new CompoundTag();
					t.putInt("textSpeed", sp);
					ClientPlayNetworking.send(new Net.Act("options", t));
				}
			}
			case 1 -> Music.setLevel(Math.floorMod(Music.level() + step, 5));
			case 2 -> Settings.set("sound", Math.floorMod(Settings.sound() + step, 5));
			case 3 -> Settings.set("voice", Settings.voice() ? 0 : 1);
			case 4 -> {
				Settings.set("nights", Math.floorMod(Settings.nights() + step, 3));
				if (online()) Night.send();
			}
			case 5 -> {
				Settings.set("seen", Settings.seen() ? 0 : 1);
				if (online()) Settings.sendPrefs();
			}
			case 6 -> Settings.set("effects", Settings.effects() ? 0 : 1);
			case 7 -> Settings.set("style", Settings.switchStyle() ? 1 : 0);
			case 8 -> {
				Settings.set("follow", Settings.follower() ? 0 : 1);
				if (online()) Settings.sendPrefs();
			}
			case 9 -> Settings.set("look", Settings.look() == 1 ? 0 : 1);
			case 10 -> {
				Settings.set("shaders", Settings.shaderMode() == 0 ? 1 : 0);
				Shaders.set(Settings.shaderMode());
			}
			case 11 -> {
				Settings.set("cycle", Settings.cycle() ? 0 : 1);
				if (online()) Settings.sendPrefs();
			}
			case 12 -> {
				Settings.set("battles", Settings.battles() ? 0 : 1);
				if (online()) Settings.sendPrefs();
			}
			case 13 -> {
				Settings.set("travel", Settings.fastTravel() ? 0 : 1);
				if (online()) Settings.sendPrefs();
			}
			default -> {
				Sounds.press();
				keymap.run();
				return;
			}
		}
		Sounds.press();
	}

	/** Up and down between rows, left and right (or A) to change. True if it used the key. */
	public boolean key(KeyEvent e) {
		if (Ui.isUp(e) || Ui.isDown(e)) {
			row = Math.floorMod(row + (Ui.isUp(e) ? -1 : 1), LABELS.length);
			Sounds.select();
			return true;
		}
		if (Ui.isLeft(e) || Ui.isRight(e)) {
			if (VALUES[row].length > 0) change(row, Ui.isLeft(e) ? -1 : 1);
			return true;
		}
		if (Ui.isA(e)) {
			change(row, 1);
			return true;
		}
		return false;
	}

	public void draw(GuiGraphics g, Maker.Hits hits, double mx, double my) {
		Scenes.bg2(g, "options");
		int n = LABELS.length;
		row = Math.max(0, Math.min(n - 1, row));
		// What the row does: a panel at the top, 22px.
		V.panel(g, 170, 40, 940, 96);
		List<String> dl = V.wrap(Lang.t(LINES[row]), 880, 22, false);
		float ty = 40 + (96 - Math.min(2, dl.size()) * 22 * 1.45F) / 2;
		for (int i = 0; i < Math.min(2, dl.size()); i++) V.text(g, dl.get(i), 170 + 30, ty + i * 22 * 1.45F + 2, 22, V.INK, false);
		// The rows: 44 tall (40 with this many), ruled apart, in a panel 18 in.
		// (Short enough that its foot stays clear of the key buttons along the bottom: 18px over them at least.)
		float top = 156, rh = Math.min(44, (720 - 14 - 40 - 18 - top - 36) / n);
		V.panel(g, 170, top, 940, n * rh + 36);
		int[] now = now();
		for (int r = 0; r < n; r++) {
			float ry = top + 18 + r * rh, rx = 170 + 18, rw = 940 - 36;
			if (V.over(mx, my, rx, ry, rw, rh)) row = r;
			boolean on = r == row;
			if (!on && r < n - 1 && r + 1 != row) V.rr(g, rx, ry + rh - 1.5F, rw, 1.5F, 0, 0xFFEEF1F8);
			V.item(g, LABELS[r], null, rx, ry, rw, rh, on, false, 21);
			float cy = ry + rh / 2;
			if (VALUES[r].length == 0) V.tri(g, rx + rw - 22 - 6, cy, 12, 0, V.RED);
			else {
				String v = Lang.t(VALUES[r][Math.max(0, Math.min(VALUES[r].length - 1, now[r]))]);
				float vr = rx + rw - 22, vx = vr - 12 - 14 - 120;
				V.tri(g, vx - 14 - 6, cy, 12, 2, V.RED);
				V.centred(g, v, vx + 60, V.capsTop(cy, 21), 21, V.INK, false);
				V.tri(g, vr - 6, cy, 12, 0, V.RED);
			}
			int rr = r;
			hits.add(rx, ry, rw, rh, () -> {
				row = rr;
				change(rr, 1);
			});
		}
		V.hints(g, "A", "Change", "B", "Back");
	}
}
