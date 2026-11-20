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

	/** Draws it (the viewer's box at 240, 150, 800 by 430: its label, seven letter boxes, the keys), registering clicks. */
	public void draw(GuiGraphics g, Maker.Hits hits, float secs) {
		float x = 240, y = 150, w = 800, h = 430, px = x + 30, py = y + 24, iw = w - 60;
		V.panel(g, x, y, w, h);
		String lab = Lang.t(label);
		V.label(g, lab, px, V.capsTop(py + 28, 13));
		float bx = px + V.labelWidth(lab) + 18, bw = Math.min(56, (iw - (bx - px) - 6 * 8) / MAX);
		for (int i = 0; i < MAX; i++) {
			float cx = bx + i * (bw + 8);
			boolean filled = i < name.length();
			V.rr(g, cx, py, bw, 56, 14, filled ? 0xFFFFF6CF : 0xFFEEF2FB);
			if (i == name.length()) V.ring(g, cx, py, bw, 56, 14, 3, V.RED);
			if (filled) V.titleCentred(g, String.valueOf(name.charAt(i)), cx + bw / 2, V.capsTop(py + 28, 34), 34, V.INK);
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
			boolean on = i == sel, ok = i == KEYS.length + 1;
			if (ok) {
				if (on) V.rrv(g, kx, ky, kw, 50, 14, 0xFF6BE0A6, 0xFF2FA86C);
				else V.rr(g, kx, ky, kw, 50, 14, 0xFFD9F5E6);
			} else if (on) {
				V.rr(g, kx, ky + 4, kw, 50, 14, 0x59E8961E);
				V.rr(g, kx - 2, ky - 2, kw + 4, 54, 16, 0xFFFFFFFF);
				V.sel(g, kx, ky, kw, 50, 14);
			} else V.rr(g, kx, ky, kw, 50, 14, 0xFFEEF2FB);
			boolean space = i < KEYS.length && KEYS[i].equals(" ");
			int c = ok ? (on ? 0xFFFFFFFF : 0xFF1F8A55) : V.INK;
			if (i < KEYS.length) V.titleCentred(g, space ? "␣" : KEYS[i], kx + kw / 2, V.capsTop(ky + 25, 26), 26, c);
			else {
				String t = i == KEYS.length ? Lang.t("DELETE") : "OK";
				V.centred(g, t, kx + kw / 2, V.capsTop(ky + 25, 18), 18, c, true);
			}
			int ii = i;
			hits.add(kx, ky, kw, 50, () -> {
				sel = ii;
				press();
			});
		}
	}
}
