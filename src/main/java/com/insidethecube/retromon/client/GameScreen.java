package com.insidethecube.retromon.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * A screen of the game, drawn over the world (which keeps going): no blur, no pause, worked with the keys or
 * the mouse (left click to pick or move the text on, right click to go back). Holds the text box at the
 * bottom that types its lines out, two at a time, as the games do.
 */
public abstract class GameScreen extends Screen {
	protected int s = 1;
	protected int ticks;
	protected int mx = -1, my = -1;

	private record Hit(int x, int y, int w, int h, Runnable click) {}

	private final List<Hit> hits = new ArrayList<>();

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
	public final void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
		mx = mouseX;
		my = mouseY;
		hits.clear();
		draw(g, partial);
		// Widgets (the nickname box).
		super.render(g, mouseX, mouseY, partial);
	}

	/** Draws the screen (registering what can be clicked with hit()). */
	protected abstract void draw(GuiGraphics g, float partial);

	/** Something clickable, drawn this frame. */
	protected void hit(int x, int y, int w, int h, Runnable click) {
		hits.add(new Hit(x, y, w, h, click));
	}

	protected boolean over(int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double x = event.x(), y = event.y();
		if (event.button() == 1) {
			back();
			return true;
		}
		if (event.button() != 0) return false;
		for (int i = hits.size() - 1; i >= 0; i--) {
			Hit h = hits.get(i);
			if (x >= h.x && x < h.x + h.w && y >= h.y && y < h.y + h.h) {
				Sounds.select();
				h.click.run();
				return true;
			}
		}
		clickAnywhere();
		return true;
	}

	/** A click on nothing in particular: moves the text on, by default. */
	protected void clickAnywhere() {
	}

	/** Right click: back. */
	protected void back() {
	}

	@Override
	public boolean keyPressed(KeyEvent e) {
		if (key(e)) return true;
		return super.keyPressed(e);
	}

	/** A key: true if the screen used it. */
	protected abstract boolean key(KeyEvent e);

	protected int boxHeight() {
		return 10 * s * 2 + 16 * s;
	}

	/** Characters typed per tick, by the text speed in OPTION. */
	public static int textSpeed() {
		return switch (ClientState.data.textSpeed) {
			case 0 -> 1;
			case 1 -> 2;
			default -> 4;
		};
	}

	/** A list to pick from, drawn as a panel with the cursor (and clickable): YES/NO, items, the menu. */
	protected void choice(GuiGraphics g, Choice c, int x, int y, int w, IntConsumer pick) {
		int h = c.height(s);
		Ui.panel(g, x, y, w, h, s);
		for (int i = 0; i < c.options.size(); i++) {
			int ty = y + 6 * s + i * 13 * s;
			int row = i;
			boolean hover = over(x, ty - 2 * s, w, 13 * s);
			if (hover) c.at = i;
			if (i == c.at) {
				Ui.round(g, x + 3 * s, ty - 2 * s, w - 6 * s, 12 * s, 0x334A8FE0, s);
				Ui.text(g, font, Ui.CURSOR, x + 6 * s, ty, Ui.GOLD, s);
			}
			Ui.text(g, font, c.options.get(i), x + 16 * s, ty, i == c.at ? Ui.TEXT : Ui.MUTED, s);
			hit(x, ty - 2 * s, w, 13 * s, () -> pick.accept(row));
		}
	}

	/** The text box: the lines to say, typed out two lines at a time. */
	public static final class TextBox {
		private final List<String> messages = new ArrayList<>();
		private List<String[]> pages = new ArrayList<>();
		private int page, typed, laidOut = -1;
		/** Who's speaking (shown on a tab above the box), or null. */
		public String speaker;

		public TextBox(List<String> lines) {
			messages.addAll(lines);
		}

		/** Shown all at once (a prompt that sits there while you choose). */
		public static TextBox still(String line) {
			TextBox b = new TextBox(List.of(line));
			b.typed = 1 << 20;
			return b;
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
			if (typed < length()) typed = Math.min(length(), typed + textSpeed());
		}

		public boolean typedOut() {
			return typed >= length();
		}

		public boolean atEnd() {
			return laidOut >= 0 && !pages.isEmpty() && page >= pages.size() - 1 && typedOut();
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
			if (speaker != null && !speaker.isEmpty()) {
				int tw = Ui.width(f, speaker, s) + 12 * s;
				Ui.round(g, x + 6 * s, y - 9 * s, tw, 11 * s, Ui.EDGE_LIGHT, s);
				Ui.text(g, f, speaker, x + 12 * s, y - 7 * s, Ui.TEXT, s);
			}
			Ui.panel(g, x, y, w, h, s);
			layout(f, (w - 20 * s) / s);
			if (page >= pages.size()) return;
			String[] p = pages.get(page);
			int left = typed;
			String a = p[0].substring(0, Math.min(p[0].length(), Math.max(0, left)));
			left -= p[0].length();
			String b = left > 0 ? p[1].substring(0, Math.min(p[1].length(), left)) : "";
			Ui.text(g, f, a, x + 10 * s, y + 8 * s, Ui.TEXT, s);
			Ui.text(g, f, b, x + 10 * s, y + 8 * s + 11 * s, Ui.TEXT, s);
			if (arrow && typedOut() && (ticks / 8) % 2 == 0) Ui.text(g, f, "▼", x + w - 14 * s, y + h - 13 * s, Ui.GOLD, s);
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
			return w + 28 * s;
		}

		public int height(int s) {
			return options.size() * 13 * s + 10 * s;
		}

		public String selected() {
			return options.get(at);
		}
	}
}
