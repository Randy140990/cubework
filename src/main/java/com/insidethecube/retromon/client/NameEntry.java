package com.insidethecube.retromon.client;

import java.util.function.Consumer;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;

/**
 * The UI Viewer's name entry: a panel with the name so far in seven boxes, and the letters to pick from in a
 * grid (DELETE and OK at the end). Type on the keyboard, or move round the letters and press A.
 */
public final class NameEntry {
	private static final String[] KEYS = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X",
			"Y", "Z", "-", "'", ".", " ", "0", "1", "2", "3", "4", "5"};
	private static final int MAX = 7, COLS = 9;
	private final String label;
	private final Consumer<String> done;
	private final Runnable back;
	private final StringBuilder name = new StringBuilder();
	private int sel;

	public NameEntry(String label, Consumer<String> done, Runnable back) {
		this.label = label;
		this.done = done;
		this.back = back;
	}

	private int count() {
		return KEYS.length + 2;
	}

	private void press() {
		if (sel < KEYS.length) {
			if (name.length() < MAX) name.append(KEYS[sel]);
		} else if (sel == KEYS.length) {
			if (!name.isEmpty()) name.setLength(name.length() - 1);
		} else if (!name.toString().isBlank()) {
			done.accept(name.toString().trim());
			return;
		}
		Sounds.press();
	}

	public boolean typed(CharacterEvent e) {
		String c = e.codepointAsString();
		if (c.length() != 1) return false;
		char ch = Character.toUpperCase(c.charAt(0));
		// SPACE presses the letter picked in the grid (as A does), so it doesn't type a space.
		if (ch == ' ') return true;
		if ((Character.isLetterOrDigit(ch) || "-'.".indexOf(ch) >= 0) && name.length() < MAX) {
			name.append(ch);
			Sounds.press();
		}
		return true;
	}

	public boolean key(KeyEvent e) {
		if (e.key() == GLFW.GLFW_KEY_BACKSPACE) {
			if (!name.isEmpty()) name.setLength(name.length() - 1);
			return true;
		}
		if (e.key() == GLFW.GLFW_KEY_ENTER || e.key() == GLFW.GLFW_KEY_KP_ENTER) {
			if (!name.toString().isBlank()) done.accept(name.toString().trim());
			return true;
		}
		// Letters typed go in (charTyped); only the arrows move round the grid here.
		int n = count();
		switch (e.key()) {
			case GLFW.GLFW_KEY_LEFT -> sel = Math.floorMod(sel - 1, n);
			case GLFW.GLFW_KEY_RIGHT -> sel = Math.floorMod(sel + 1, n);
			case GLFW.GLFW_KEY_UP -> sel = sel - COLS >= 0 ? sel - COLS : sel;
			case GLFW.GLFW_KEY_DOWN -> sel = Math.min(n - 1, sel + COLS);
			case GLFW.GLFW_KEY_SPACE -> press();
			case GLFW.GLFW_KEY_ESCAPE -> {
				if (!name.isEmpty()) name.setLength(name.length() - 1);
				else if (back != null) back.run();
			}
			default -> {
				return false;
			}
		}
		return true;
	}

	/** Draws it (the viewer's box at 240, 150, 800 by 430), registering clicks. */
	public void draw(GuiGraphics g, Maker.Hits hits, float secs) {
		float x = 240, y = 150, w = 800, h = 430, px = x + 30, py = y + 24, iw = w - 60;
		V.panel(g, x, y, w, h);
		V.label(g, label, px, py + 21);
		float bx = px + V.labelWidth(label) + 18, bw = Math.min(56, (iw - (bx - px) - 6 * 8) / MAX);
		for (int i = 0; i < MAX; i++) {
			float cx = bx + i * (bw + 8);
			boolean filled = i < name.length();
			V.rr(g, cx, py, bw, 56, 14, filled ? V.CREAM : 0x14FFFFFF);
			if (i == name.length()) V.ring(g, cx, py, bw, 56, 14, 3, V.EDGE_LIGHT);
			if (filled) V.centred(g, String.valueOf(name.charAt(i)), cx + bw / 2, py + 10, 34, V.INK, true);
		}
		float gy = py + 56 + 18, cw = (iw - 8 * (COLS - 1)) / COLS;
		for (int i = 0; i < count(); i++) {
			int col, row;
			float kw = cw;
			if (i < KEYS.length) {
				col = i % COLS;
				row = i / COLS;
			} else {
				// DELETE and OK, two columns wide each, after the letters.
				int j = KEYS.length + (i - KEYS.length) * 2;
				col = j % COLS;
				row = j / COLS;
				kw = cw * 2 + 8;
			}
			float kx = px + col * (cw + 8), ky = gy + row * 58;
			boolean on = i == sel;
			int bg = i == KEYS.length + 1 ? (on ? V.GREEN : 0x4041BF84) : on ? V.CREAM : 0x12FFFFFF;
			V.rr(g, kx, ky, kw, 50, 14, bg);
			String t = i < KEYS.length ? (KEYS[i].equals(" ") ? "␣" : KEYS[i]) : i == KEYS.length ? "DELETE" : "OK";
			float size = i < KEYS.length ? 26 : 18;
			V.centred(g, t, kx + kw / 2, ky + (50 - size * 1.15F) / 2, size, i == KEYS.length + 1 ? 0xFFFFFFFF : on ? V.INK : V.TEXT, true);
			int ii = i;
			hits.add(kx, ky, kw, 50, () -> {
				sel = ii;
				press();
			});
		}
	}
}
