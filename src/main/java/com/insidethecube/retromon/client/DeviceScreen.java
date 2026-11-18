package com.insidethecube.retromon.client;

import net.minecraft.client.gui.GuiGraphics;

/**
 * A screen shown on your Pokédex (the START menu, the Pokédex itself, the Town Map): the device held up in
 * your hands over the frosted world, with this drawn on its screen. Its insides draw from the screen's own
 * top-left corner, width and height being the screen's (below the status bar).
 */
public abstract class DeviceScreen extends GameScreen {
	/** Where the device's screen was last drawn (for the mouse: wheel and drag). */
	protected int screenX, screenY, screenW, screenH;

	protected DeviceScreen(String title) {
		super(title);
	}

	/** What's on the device's screen. */
	protected abstract void drawScreen(GuiGraphics g, float partial);

	@Override
	protected final void draw(GuiGraphics g, float partial) {
		int W = width, H = height, mx0 = mx, my0 = my, s0 = s;
		Device.Frame f = Device.frame(W, H, partial);
		Device.back(g, f, W, H, ticks, partial);
		int cx = f.ix(), cy = f.iy() + f.bar(), cw = f.iw(), ch = f.ih() - f.bar();
		screenX = cx;
		screenY = cy;
		screenW = cw;
		screenH = ch;
		if (cy < H) {
			g.enableScissor(cx, cy, cx + cw, cy + ch);
			g.pose().pushMatrix();
			g.pose().translate(cx, cy);
			width = cw;
			height = ch;
			ox = cx;
			oy = cy;
			mx = mx0 - cx;
			my = my0 - cy;
			s = Device.scale(ch);
			try {
				drawScreen(g, partial);
			} finally {
				g.pose().popMatrix();
				g.disableScissor();
				width = W;
				height = H;
				ox = 0;
				oy = 0;
				mx = mx0;
				my = my0;
				s = s0;
			}
		}
		int[] b = Device.front(g, f, W, H, ticks, partial, font, backLabel(), backOver);
		backOver = b != null && over(b[0], b[1], b[2], b[3]);
		if (b != null) hit(b[0], b[1], b[2], b[3], this::back);
	}

	private boolean backOver;

	/** The status bar's BACK button: what it says (null for none). */
	protected String backLabel() {
		return "BACK";
	}

	@Override
	public boolean keyPressed(net.minecraft.client.input.KeyEvent e) {
		if (Device.closing()) return true;
		return super.keyPressed(e);
	}

	@Override
	public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
		if (Device.closing()) return true;
		return super.mouseClicked(event, doubleClick);
	}

	/** The mouse over the device's screen (its own coordinates), or null. */
	protected double[] onScreen(double x, double y) {
		if (x < screenX || y < screenY || x >= screenX + screenW || y >= screenY + screenH) return null;
		return new double[] {x - screenX, y - screenY};
	}

	/** B at the top: it goes down, then you put it away. */
	@Override
	public void onClose() {
		Device.close();
	}

	/** Going back to another screen on the device (no animation: it stays in your hands). */
	protected void to(net.minecraft.client.gui.screens.Screen next) {
		if (minecraft != null) minecraft.setScreen(next);
	}
}
