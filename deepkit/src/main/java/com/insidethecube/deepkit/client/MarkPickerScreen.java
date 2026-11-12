package com.insidethecube.deepkit.client;

import com.insidethecube.deepkit.net.Payloads;
import com.insidethecube.deepkit.paint.Marks;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Choosing what glow paint paints: the marks in a ring (like Cubework's shape wheel). Point at one and
 * click; Esc or right-click closes without changing.
 */
public final class MarkPickerScreen extends Screen {
	private static final int SLATE = 0xE61A1C21, RIM = 0xFFB98D4E, GLOW = 0xF0D9A64C, TEXT = 0xFFF2E6CC, DIM = 0xFFA89A80, CURRENT = 0xFF7FD3C7;

	private final int current;
	private int hover = -1;

	public MarkPickerScreen(int current) {
		super(Component.literal("Glow paint mark"));
		this.current = current;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
		g.fillGradient(0, 0, width, height, 0x50000000, 0x78000000);
	}

	private float radius() {
		return Math.max(50, Math.min(width, height) * 0.3f);
	}

	private int at(double x, double y) {
		double dx = x - width / 2.0, dy = y - height / 2.0;
		if (dx * dx + dy * dy < 20 * 20) return -1;
		int n = Marks.count();
		double a = Math.atan2(dx, -dy), turn = 2 * Math.PI;
		a = ((a + Math.PI / n) % turn + turn) % turn;
		return Math.min(n - 1, (int) (a / turn * n));
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		super.render(g, mouseX, mouseY, delta);
		hover = at(mouseX, mouseY);
		int n = Marks.count();
		float cx = width / 2f, cy = height / 2f, r = radius();
		int box = 11;
		for (int i = 0; i < n; i++) {
			double a = i * 2 * Math.PI / n;
			int x = Math.round(cx + (float) Math.sin(a) * r), y = Math.round(cy - (float) Math.cos(a) * r);
			int edge = i == hover ? GLOW : i == current ? CURRENT : RIM;
			g.fill(x - box - 1, y - box - 1, x + box + 1, y + box + 1, edge);
			g.fill(x - box, y - box, x + box, y + box, i == hover ? 0xF0574124 : SLATE);
			g.pose().pushMatrix();
			g.pose().translate(x, y);
			g.pose().scale(1.6f, 1.6f);
			g.drawCenteredString(font, Marks.GLYPHS[i], 0, -4, i == hover ? 0xFFFFFFFF : TEXT);
			g.pose().popMatrix();
		}
		String name = hover >= 0 ? Marks.TITLES[hover] : "Choose a mark";
		g.drawCenteredString(font, name, (int) cx, (int) cy - 4, TEXT);
		g.drawCenteredString(font, "Click a mark · Esc to cancel", (int) cx, (int) (cy + r + box + 10), DIM);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == 0) {
			int i = at(event.x(), event.y());
			if (i >= 0) {
				ClientPlayNetworking.send(new Payloads.SetMark(i));
				onClose();
			}
			return true;
		}
		if (event.button() == 1) {
			onClose();
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		return super.keyPressed(event);
	}
}
