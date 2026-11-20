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
	/** Where what's drawn now sits on the screen (a device's screen draws its insides from its own corner). */
	protected int ox, oy;

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
		V.HINTS.clear();
		V.fit(width, height);
		draw(g, partial);
		// The key hints work as buttons too (SPACE, ESC, TAB...).
		for (V.Hint h : List.copyOf(V.HINTS)) vhit(h.x(), h.y(), h.w(), h.h(), () -> pressHint(h.key()));
		// Widgets (the nickname box).
		super.render(g, mouseX, mouseY, partial);
	}

	/** Draws the screen (registering what can be clicked with hit()). */
	protected abstract void draw(GuiGraphics g, float partial);

	/** Something clickable, drawn this frame. */
	protected void hit(int x, int y, int w, int h, Runnable click) {
		hits.add(new Hit(x + ox, y + oy, w, h, click));
	}

	/** Something clickable on the viewer's canvas (V): its box in canvas pixels. */
	protected void vhit(float x, float y, float w, float h, Runnable click) {
		hit(Math.round(V.x(x)), Math.round(V.y(y)), Math.round(V.s(w)), Math.round(V.s(h)), click);
	}

	/** Whether the mouse is over a box on the viewer's canvas. */
	protected boolean vover(float x, float y, float w, float h) {
		return V.over(mx, my, x, y, w, h);
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

	/** A key hint clicked: as if its key were pressed (B goes back). */
	protected void pressHint(String key) {
		int code = switch (key) {
			case "A" -> org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE;
			case "TAB" -> org.lwjgl.glfw.GLFW.GLFW_KEY_TAB;
			case "ENTER" -> org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER;
			case "B", "ESC" -> -1;
			default -> key.length() == 1 ? org.lwjgl.glfw.GLFW.GLFW_KEY_A + (Character.toUpperCase(key.charAt(0)) - 'A') : -2;
		};
		if (code == -1) back();
		else if (code >= 0) keyPressed(new KeyEvent(code, 0, 0));
	}

	/** A click on nothing in particular: moves the text on, by default. */
	protected void clickAnywhere() {
	}

	/** Right click: back. */
	protected void back() {
	}

	/** A screen put aside for Minecraft's own menu (Esc), to come back to when that closes. */
	public static GameScreen resume;

	@Override
	public boolean keyPressed(KeyEvent e) {
		// Esc is the hints' ESC: back (as B), a step at a time; from the START menu itself, it closes it. (Minecraft's
		// own menu is Esc out in the world.)
		if (e.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE && minecraft != null && minecraft.level != null) {
			escape();
			return true;
		}
		if (e.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_TAB && wantsTab() && key(e)) return true;
		if (Adventure.SPEED != null && Adventure.SPEED.matches(e) && minecraft != null && minecraft.level != null) {
			Adventure.cycleSpeed();
			return true;
		}
		if (key(e)) return true;
		return super.keyPressed(e);
	}

	/** A key: true if the screen used it. */
	protected abstract boolean key(KeyEvent e);

	/** Esc: as the B button (backspace), or back if the screen didn't take it. */
	protected void escape() {
		if (!key(new KeyEvent(org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE, 0, 0))) back();
	}

	/** Whether TAB is this screen's own (pages, pockets) rather than the game's speed. */
	protected boolean wantsTab() {
		return false;
	}

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
		// A soft glass card with a row of pills; the picked one cream with dark text (the newer games' lists).
		int h = c.height(s);
		Ui.round(g, x, y, w, h, 0xE6123C38, 3 * s);
		for (int i = 0; i < c.options.size(); i++) {
			int ty = y + 6 * s + i * 13 * s;
			int row = i;
			boolean hover = over(x, ty - 2 * s, w, 13 * s);
			if (hover) c.at = i;
			boolean on = i == c.at;
			Ui.pill(g, x + 3 * s, ty - 2 * s, w - 6 * s, 12 * s, on, s);
			Ui.draw(g, font, Ui.bold(c.options.get(i)), x + 10 * s, ty, on ? Ui.CREAM_INK : Ui.TEXT, s, false);
			hit(x, ty - 2 * s, w, 13 * s, () -> pick.accept(row));
		}
	}

	/** A list to pick from as the UI Viewer draws it (V): a panel of rows at x, y (canvas), w wide. Returns its height. */
	protected float vchoice(GuiGraphics g, Choice c, float x, float y, float w, IntConsumer pick) {
		int at = V.choiceAt(c.options.size(), x, y, w, mx, my);
		if (at >= 0) c.at = at;
		float h = V.choice(g, c.options, null, c.at, x, y, w);
		for (int i = 0; i < c.options.size(); i++) {
			int row = i;
			vhit(x + 12, y + 12 + i * 54, w - 24, 46, () -> pick.accept(row));
		}
		return h;
	}

	/** The text box: the lines to say, typed out two lines at a time. */
	public static final class TextBox {
		private final List<String> messages = new ArrayList<>();
		private List<String[]> pages = new ArrayList<>();
		/** Which message each page comes from. */
		private final List<Integer> owners = new ArrayList<>();
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
			owners.clear();
			for (int mi = 0; mi < messages.size(); mi++) {
				List<String> lines = Ui.wrap(f, Look.text(messages.get(mi)), width);
				for (int i = 0; i < lines.size(); i += 2) {
					pages.add(new String[] {lines.get(i), i + 1 < lines.size() ? lines.get(i + 1) : ""});
					owners.add(mi);
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

		/** The message (line given) showing now, or -1 before it's laid out. */
		public int message() {
			return laidOut < 0 || page >= owners.size() ? -1 : owners.get(page);
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
			Sounds.press();
			if (!typedOut()) {
				typed = length();
				return false;
			}
			page++;
			typed = 0;
			return page >= pages.size();
		}

		/**
		 * Drawn as the UI Viewer's text box: on its canvas (V) at x, its bottom edge bottom pixels up from the
		 * canvas's bottom, w wide: teal glass, white words with names in gold, the speaker on a cream tab, the mint
		 * arrow when there's more.
		 */
		public void renderV(GuiGraphics g, Font f, float x, float bottom, float w, int ticks, float partial, boolean arrow) {
			layout(f, (int) ((w - 68) * 8.5F / 25 * 0.9F));
			List<String> lines = new ArrayList<>();
			if (page < pages.size()) {
				String[] p = pages.get(page);
				int left = typed;
				lines.add(p[0].substring(0, Math.min(p[0].length(), Math.max(0, left))));
				left -= p[0].length();
				if (!p[1].isEmpty()) lines.add(left > 0 ? p[1].substring(0, Math.min(p[1].length(), left)) : "");
			}
			V.textBox(g, x, bottom, w, speaker, lines, arrow && typedOut() && page < pages.size(), (ticks + partial) / 20F);
		}

		public void render(GuiGraphics g, Font f, int x, int y, int w, int h, int s, int ticks, boolean arrow) {
			// The speech panel as the newer games have it: warm cream, rounded, dark text, a soft shadow below,
			// and the speaker's name on a dark tab.
			Ui.round(g, x + s, y + 2 * s, w, h, 0x40000000, 4 * s);
			Ui.round(g, x, y, w, h, 0xF8FBF5E6, 4 * s);
			Ui.round(g, x + 2 * s, y + 2 * s, w - 4 * s, h - 4 * s, 0xFFFFF9EC, 3 * s);
			if (speaker != null && !speaker.isEmpty()) {
				int tw = Ui.boldWidth(f, speaker, s) + 16 * s;
				Ui.rr(g, x + 9 * s, y - 8 * s, tw + 2 * s, 15 * s, 7.5F * s, Ui.EDGE);
				Ui.rr(g, x + 10 * s, y - 7 * s, tw, 13 * s, 6.5F * s, 0xFF123C38);
				Ui.draw(g, f, Ui.bold(speaker), x + 18 * s, y - 4 * s, 0xFFFFFFFF, s, false);
			}
			layout(f, (w - 20 * s) / s);
			if (page >= pages.size()) return;
			String[] p = pages.get(page);
			int left = typed;
			String a = p[0].substring(0, Math.min(p[0].length(), Math.max(0, left)));
			left -= p[0].length();
			String b = left > 0 ? p[1].substring(0, Math.min(p[1].length(), left)) : "";
			Ui.plain(g, f, a, x + 14 * s, y + 10 * s, Ui.CREAM_INK, s);
			Ui.plain(g, f, b, x + 14 * s, y + 10 * s + 12 * s, Ui.CREAM_INK, s);
			if (arrow && typedOut()) Ui.triangle(g, x + w - 14 * s, y + h - 11 * s + ((ticks / 6) % 2) * s, 8 * s, 2, 0xFFE5525C);
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
