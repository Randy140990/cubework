package com.insidethecube.kantojohto.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

/**
 * A screen of the game, drawn over the world (which keeps going): no blur, no pause, worked with the keys.
 * Holds the text box at the bottom that types its lines out, two at a time, as the games do.
 */
public abstract class GameScreen extends Screen {
	protected int s = 1;
	protected int ticks;

	protected GameScreen(String title) {
		super(Component.literal(title));
	}

	@Override
	protected void init() {
		s = Ui.scale(height);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partial) {
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	public void tick() {
		ticks++;
	}

	@Override
	public boolean keyPressed(KeyEvent e) {
		if (key(e)) return true;
		return super.keyPressed(e);
	}

	/** A key: true if the screen used it. */
	protected abstract boolean key(KeyEvent e);

	protected int boxHeight() {
		return 10 * s * 2 + 14 * s;
	}

	/** The text box: the lines to say, typed out two lines at a time. */
	public static final class TextBox {
		private final List<String> messages = new ArrayList<>();
		private List<String[]> pages = new ArrayList<>();
		private int page, typed, laidOut = -1;

		public TextBox(List<String> lines) {
			messages.addAll(lines);
		}

		public void add(String line) {
			messages.add(line);
			laidOut = -1;
		}

		public void set(List<String> lines) {
			messages.clear();
			messages.addAll(lines);
			page = 0;
			typed = 0;
			laidOut = -1;
		}

		public boolean empty() {
			return messages.isEmpty();
		}

		private void layout(Font f, int width) {
			if (laidOut == width) return;
			laidOut = width;
			int keepPage = page;
			pages = new ArrayList<>();
			for (String m : messages) {
				List<String> lines = Ui.wrap(f, m, width);
				for (int i = 0; i < lines.size(); i += 2) {
					pages.add(new String[] {lines.get(i), i + 1 < lines.size() ? lines.get(i + 1) : ""});
				}
			}
			page = Math.min(keepPage, Math.max(0, pages.size()));
		}

		private int length() {
			if (page >= pages.size()) return 0;
			String[] p = pages.get(page);
			return p[0].length() + p[1].length();
		}

		public void tick() {
			typed = Math.min(length(), typed + 2);
		}

		/** Whether the page on show is all typed out. */
		public boolean typedOut() {
			return typed >= length();
		}

		/** Whether it's showing its last page, all typed out. */
		public boolean atEnd() {
			return page >= pages.size() - 1 && typedOut();
		}

		public boolean finished() {
			return laidOut >= 0 && page >= pages.size();
		}

		/** A press of A: finishes typing the page, or turns it. Returns true when there's nothing left to show. */
		public boolean press() {
			if (laidOut < 0) return false;
			if (!typedOut()) {
				typed = length();
				return false;
			}
			page++;
			typed = 0;
			return page >= pages.size();
		}

		public void render(GuiGraphics g, Font f, int x, int y, int w, int h, int s, int ticks, boolean arrow) {
			Ui.box(g, x, y, w, h, s);
			layout(f, (w - 16 * s) / s);
			if (page >= pages.size()) return;
			String[] p = pages.get(page);
			int left = typed;
			String a = p[0].substring(0, Math.min(p[0].length(), Math.max(0, left)));
			left -= p[0].length();
			String b = left > 0 ? p[1].substring(0, Math.min(p[1].length(), left)) : "";
			Ui.text(g, f, a, x + 8 * s, y + 7 * s, Ui.INK, s);
			Ui.text(g, f, b, x + 8 * s, y + 7 * s + 10 * s, Ui.INK, s);
			if (arrow && typedOut() && (ticks / 8) % 2 == 0) Ui.text(g, f, "▼", x + w - 12 * s, y + h - 12 * s, Ui.INK, s);
		}
	}

	/** A small list to choose from (YES/NO, the items, the menu), with the cursor. */
	public static final class Choice {
		public final List<String> options;
		public int at;

		public Choice(List<String> options) {
			this.options = new ArrayList<>(options);
		}

		public boolean key(KeyEvent e) {
			if (Ui.isUp(e)) {
				at = Math.floorMod(at - 1, options.size());
				Sounds.select();
				return true;
			}
			if (Ui.isDown(e)) {
				at = Math.floorMod(at + 1, options.size());
				Sounds.select();
				return true;
			}
			return false;
		}

		public int width(Font f, int s) {
			int w = 0;
			for (String o : options) w = Math.max(w, Ui.width(f, o, s));
			return w + 22 * s;
		}

		public int height(int s) {
			return options.size() * 12 * s + 10 * s;
		}

		public void render(GuiGraphics g, Font f, int x, int y, int s) {
			int w = width(f, s), h = height(s);
			Ui.box(g, x, y, w, h, s);
			for (int i = 0; i < options.size(); i++) {
				int ty = y + 6 * s + i * 12 * s;
				if (i == at) Ui.text(g, f, Ui.CURSOR, x + 6 * s, ty, Ui.INK, s);
				Ui.text(g, f, options.get(i), x + 14 * s, ty, Ui.INK, s);
			}
		}

		public String selected() {
			return options.get(at);
		}
	}
}
