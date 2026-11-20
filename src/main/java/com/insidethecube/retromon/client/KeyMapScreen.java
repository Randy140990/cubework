package com.insidethecube.retromon.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;

/**
 * The key map, as the UI Viewer's "Key map": the whole keyboard, each key in the colour of what it does (in game,
 * in menus, for building, Minecraft's own, turned off, still to decide). The key you're on (the arrows, or the
 * mouse) is described in the panel below: what it does, and what Retromon turns off. Opened from OPTION's
 * CONTROLS, and from the keys page at the start of a new game.
 */
public class KeyMapScreen extends GameScreen {
	private final Screen parent;
	private int ri = 2, ci = 2;
	private static final float U = 62, X0 = 40, Y0 = 60;

	private static final String[][][] KB = {
			{{"Esc", "1", "Esc"}, {"F1", "1", "F1"}, {"F2", "1", "F2"}, {"F3", "1", "F3"}, {"F4", "1", "F4"}, {"F5", "1", "F5"}, {"F6", "1", "F6"}, {"F7", "1", "F7"}, {"F8", "1", "F8"}, {"F9", "1", "F9"}, {"F10", "1", "F10"}, {"F11", "1", "F11"}, {"F12", "1", "F12"}},
			{{"`", "1", "Backquote"}, {"1", "1", "Digit1"}, {"2", "1", "Digit2"}, {"3", "1", "Digit3"}, {"4", "1", "Digit4"}, {"5", "1", "Digit5"}, {"6", "1", "Digit6"}, {"7", "1", "Digit7"}, {"8", "1", "Digit8"}, {"9", "1", "Digit9"}, {"0", "1", "Digit0"}, {"-", "1", "Minus"}, {"=", "1", "Equal"}, {"Back", "2", "Backspace"}},
			{{"Tab", "1.5", "Tab"}, {"Q", "1", "KeyQ"}, {"W", "1", "KeyW"}, {"E", "1", "KeyE"}, {"R", "1", "KeyR"}, {"T", "1", "KeyT"}, {"Y", "1", "KeyY"}, {"U", "1", "KeyU"}, {"I", "1", "KeyI"}, {"O", "1", "KeyO"}, {"P", "1", "KeyP"}, {"[", "1", "BracketLeft"}, {"]", "1", "BracketRight"}, {"Enter", "1.5", "Enter"}},
			{{"Caps", "1.75", "CapsLock"}, {"A", "1", "KeyA"}, {"S", "1", "KeyS"}, {"D", "1", "KeyD"}, {"F", "1", "KeyF"}, {"G", "1", "KeyG"}, {"H", "1", "KeyH"}, {"J", "1", "KeyJ"}, {"K", "1", "KeyK"}, {"L", "1", "KeyL"}, {";", "1", "Semicolon"}, {"'", "1", "Quote"}, {"#", "1", "Backslash"}, {"", "1.25", "x1"}},
			{{"Shift", "2.25", "ShiftLeft"}, {"Z", "1", "KeyZ"}, {"X", "1", "KeyX"}, {"C", "1", "KeyC"}, {"V", "1", "KeyV"}, {"B", "1", "KeyB"}, {"N", "1", "KeyN"}, {"M", "1", "KeyM"}, {",", "1", "Comma"}, {".", "1", "Period"}, {"/", "1", "Slash"}, {"Shift", "2.75", "ShiftRight"}},
			{{"Ctrl", "1.5", "ControlLeft"}, {"Alt", "1.5", "AltLeft"}, {"Space", "7", "Space"}, {"Alt Gr", "1.5", "AltRight"}, {"Ctrl", "1.5", "ControlRight"}, {"", "0.5", "x2"}, {"←", "1", "ArrowLeft"}, {"↑", "1", "ArrowUp"}, {"↓", "1", "ArrowDown"}, {"→", "1", "ArrowRight"}},
	};
	private static final Map<String, String[]> ROLE = new HashMap<>();
	static {
		ROLE.put("KeyW", new String[] {"game", "Walk north", "In menus: up."});
		ROLE.put("KeyA", new String[] {"game", "Walk west", "In menus: left."});
		ROLE.put("KeyS", new String[] {"game", "Walk south", "In menus: down."});
		ROLE.put("KeyD", new String[] {"game", "Walk east", "In menus: right."});
		ROLE.put("Space", new String[] {"game", "Talk, look at things, choose", "In menus: A."});
		ROLE.put("Enter", new String[] {"menu", "Choose, in menus (A)", "In the world it opens the menu too, as E does."});
		ROLE.put("Tab", new String[] {"game", "Speed up: x1, x2, x3", "In menus: the next tab or pocket. (Moved off R, which reloads shaders.)"});
		ROLE.put("Escape", new String[] {"game", "Pause", "In menus: back."});
		ROLE.put("KeyE", new String[] {"game", "Open the menu (START)", "Where Minecraft opens its inventory, E opens ours. Press it again in the menu to close it."});
		ROLE.put("KeyQ", new String[] {"menu", "Back, in menus (B)", "In the world it would drop what you hold: turned off. On the Town Map, zoom out."});
		ROLE.put("KeyZ", new String[] {"menu", "Choose, in menus (A)", ""});
		ROLE.put("KeyX", new String[] {"menu", "Back, in menus (B)", "On a name, delete a letter."});
		ROLE.put("Backspace", new String[] {"menu", "Back, in menus (B)", "On a name, delete a letter."});
		ROLE.put("KeyC", new String[] {"menu", "Town Map: back to where you are", ""});
		ROLE.put("KeyV", new String[] {"menu", "Pokédex: read the entry out", ""});
		ROLE.put("ArrowUp", new String[] {"menu", "Move, in menus", "In the world, W A S D walk."});
		ROLE.put("ArrowDown", new String[] {"menu", "Move, in menus", ""});
		ROLE.put("ArrowLeft", new String[] {"menu", "Move, in menus", ""});
		ROLE.put("ArrowRight", new String[] {"menu", "Move, in menus", ""});
		ROLE.put("F7", new String[] {"dev", "Retromon on or off", "For building and testing only. Off in the finished game."});
		ROLE.put("F2", new String[] {"free", "Screenshot", "Minecraft's own, kept."});
		ROLE.put("F11", new String[] {"free", "Full screen", "Minecraft's own, kept."});
		ROLE.put("F1", new String[] {"free", "Hide the HUD", "Kept, for screenshots."});
		ROLE.put("F3", new String[] {"off", "Debug screen", "Shows coordinates and more: off in the Retromon game."});
		ROLE.put("F5", new String[] {"off", "Camera view", "The camera is fixed: off."});
		ROLE.put("KeyR", new String[] {"off", "Reload shaders", "Iris's key. Off: the game's look can't be changed."});
		ROLE.put("KeyK", new String[] {"off", "Shaders on or off", "Iris's key. Off: the shaders stay on."});
		ROLE.put("KeyO", new String[] {"off", "Shader packs", "Iris's menu. Off."});
		ROLE.put("KeyM", new String[] {"off", "Other mods' keys", "Every other mod's key is turned off while you play (a map mod on M, for one). The Town Map is in the menu."});
		ROLE.put("KeyJ", new String[] {"off", "Map mods", "Off."});
		ROLE.put("KeyL", new String[] {"off", "Advancements", "Off."});
		ROLE.put("KeyP", new String[] {"off", "Social interactions", "Off."});
		ROLE.put("KeyT", new String[] {"decide", "Chat", "Off in single player. On servers: to decide."});
		ROLE.put("Slash", new String[] {"off", "Commands", "Off, except for whoever runs the server."});
		ROLE.put("KeyF", new String[] {"off", "Swap hands", "Off."});
		ROLE.put("ShiftLeft", new String[] {"decide", "Crouch", "To decide: off, or run."});
		ROLE.put("ShiftRight", new String[] {"decide", "Crouch", "To decide."});
		ROLE.put("ControlLeft", new String[] {"decide", "Sprint", "To decide: run, or off."});
		ROLE.put("Digit1", new String[] {"off", "Hotbar", "1 to 9: off. There's no hotbar."});
		ROLE.put("Digit2", new String[] {"off", "Hotbar", ""});
		ROLE.put("Digit3", new String[] {"off", "Hotbar", ""});
		ROLE.put("Digit4", new String[] {"off", "Hotbar", ""});
		ROLE.put("Digit5", new String[] {"off", "Hotbar", ""});
		ROLE.put("Digit6", new String[] {"off", "Hotbar", ""});
		ROLE.put("Digit7", new String[] {"off", "Hotbar", ""});
		ROLE.put("Digit8", new String[] {"off", "Hotbar", ""});
		ROLE.put("Digit9", new String[] {"off", "Hotbar", ""});
		ROLE.put("Esc", ROLE.get("Escape"));
	}
	private static final String[] ROLES = {"game", "menu", "dev", "free", "off", "decide"};
	private static final int[][] ROLE_COLOURS = {{0xFF41BF84, 0xFFFFFFFF}, {0xFFFFD34E, 0xFF26304A}, {0xFF8E6BC8, 0xFFFFFFFF}, {0xFF7C8A99, 0xFFFFFFFF}, {0xFFE5525C, 0xFFFFFFFF}, {0xFFF0B43C, 0xFF26304A}};
	private static final String[] ROLE_NAMES = {"In game", "In menus", "For building", "Minecraft's, kept", "Turned off", "To decide"};

	public KeyMapScreen(Screen parent) {
		super("Key map");
		this.parent = parent;
	}

	private static float w(String[] k) {
		return Float.parseFloat(k[1]);
	}

	private static int roleIndex(String role) {
		for (int i = 0; i < ROLES.length; i++) if (ROLES[i].equals(role)) return i;
		return -1;
	}

	@Override
	protected boolean key(KeyEvent e) {
		if (Ui.isB(e)) {
			back();
			return true;
		}
		if (Ui.isLeft(e) || Ui.isRight(e)) {
			ci = Math.max(0, Math.min(KB[ri].length - 1, ci + (Ui.isRight(e) ? 1 : -1)));
			Sounds.press();
		} else if (Ui.isUp(e) || Ui.isDown(e)) {
			// To the key above or below the middle of this one.
			int nr = Math.max(0, Math.min(KB.length - 1, ri + (Ui.isDown(e) ? 1 : -1)));
			float x = 0;
			for (int i = 0; i < ci; i++) x += w(KB[ri][i]);
			x += w(KB[ri][ci]) / 2;
			float acc = 0;
			int best = 0;
			for (int i = 0; i < KB[nr].length; i++) {
				if (acc <= x) best = i;
				acc += w(KB[nr][i]);
			}
			ri = nr;
			ci = best;
			Sounds.press();
		}
		return true;
	}

	@Override
	protected void back() {
		Sounds.press();
		if (minecraft != null) minecraft.setScreen(parent);
	}

	@Override
	public void onClose() {
		back();
	}

	@Override
	protected void draw(GuiGraphics g, float partial) {
		Scenes.bg2(g, "keymap");
		// What the colours mean, along the top.
		float lx = X0;
		for (int i = 0; i < ROLES.length; i++) {
			V.rr(g, lx, 22, 16, 16, 5, ROLE_COLOURS[i][0]);
			V.text(g, ROLE_NAMES[i], lx + 22, 20, 15, V.TEXT, true);
			lx += 22 + V.width(ROLE_NAMES[i], 15, true) + 16;
		}
		// The keyboard.
		for (int r = 0; r < KB.length; r++) {
			float x = X0;
			for (int c = 0; c < KB[r].length; c++) {
				String[] k = KB[r][c];
				float kw = w(k) * U - 6, ky = Y0 + r * (U + 6);
				boolean spacer = k[0].isEmpty() && k[2].startsWith("x");
				if (!spacer) {
					String[] role = ROLE.get(k[2]);
					int ri2 = role == null ? -1 : roleIndex(role[0]);
					boolean on = r == ri && c == ci;
					float lift = on ? 3 : 0;
					int bg = ri2 < 0 ? 0xFFFFFFFF : ROLE_COLOURS[ri2][0], fg = ri2 < 0 ? V.DIM : ROLE_COLOURS[ri2][1];
					if (on) {
						V.shadow(g, x, ky - lift, kw, U - 6, 12, 1.2F);
						V.rr(g, x - 6, ky - lift - 6, kw + 12, U - 6 + 12, 18, V.RED);
						V.rr(g, x - 3, ky - lift - 3, kw + 6, U - 6 + 6, 15, 0xFFFFFFFF);
					} else V.rr(g, x, ky - lift + 3, kw, U - 6, 12, 0x2E1E2854);
					V.rr(g, x, ky - lift, kw, U - 6, 12, bg);
					// Turned off: striped.
					if (ri2 >= 0 && ROLES[ri2].equals("off"))
						for (float sx = 6; sx < kw - 4; sx += 12) V.rr(g, x + sx, ky - lift + 4, 6, U - 14, 3, 0x26000000);
					float size = k[0].length() > 3 ? 15 : 19;
					V.centred(g, k[0], x + kw / 2, ky - lift + (U - 6 - size * 1.15F) / 2, size, fg, true);
					int rr = r, cc = c;
					if (vover(x, ky, kw, U - 6)) {
						ri = rr;
						ci = cc;
					}
				}
				x += w(k) * U;
			}
		}
		// The key you're on.
		String[] k = KB[ri][ci], role = ROLE.get(k[2]);
		V.panel(g, 40, 470, 760, 220);
		float kw = Math.max(56, V.width(k[0].isEmpty() ? " " : k[0], 18, true) + 20);
		V.rr(g, 64, 488 + 3, kw, 40, 10, 0x2E1E2854);
		V.rr(g, 64, 488, kw, 40, 10, 0xFFDDE3F0);
		V.rr(g, 66, 490, kw - 4, 36, 8, 0xFFFFFFFF);
		V.centred(g, k[0], 64 + kw / 2, 488 + (40 - 18 * 1.15F) / 2, 18, V.INK, true);
		if (role != null) {
			int i = roleIndex(role[0]);
			V.title(g, role[1], 64 + kw + 14, V.capsTop(508, 28), 28, V.INK);
			if (i >= 0) {
				float cw = V.width(ROLE_NAMES[i], 14, true) + 24;
				V.pill(g, 40 + 760 - 24 - cw, 496, cw, 26, ROLE_COLOURS[i][0]);
				V.text(g, ROLE_NAMES[i], 40 + 760 - 24 - cw + 12, 500, 14, ROLE_COLOURS[i][1], true);
			}
			List<String> lines = V.wrap(role[2], 712, 20, true);
			for (int j = 0; j < lines.size() && j < 4; j++) V.text(g, lines.get(j), 64, 542 + j * 30, 20, V.TEXT, true);
		} else {
			V.title(g, "Not used", 64 + kw + 14, V.capsTop(508, 26), 26, V.MUTED);
			V.text(g, "Free for now.", 64, 542, 19, V.MUTED, true);
		}
		// Also turned off.
		V.panel(g, 830, 470, 410, 220);
		V.label(g, "Also turned off", 852, 486);
		String[] off = {"Axiom: its editor and every key it adds", "Mouse: breaking, placing, picking blocks, the hotbar wheel",
				"Minecraft's inventory, recipes and creative menus", "Turning the shaders off, or swapping them"};
		float oy = 512;
		for (String o : off) {
			List<String> ol = V.wrap(o, 350, 17, true);
			V.text(g, "■", 852, oy, 17, 0xFFE5525C, true);
			for (int j = 0; j < ol.size(); j++) V.text(g, ol.get(j), 874, oy + j * 20, 17, V.TEXT, true);
			oy += ol.size() * 20 + 4;
		}
		V.hints(g, "B", "Back");
	}
}
