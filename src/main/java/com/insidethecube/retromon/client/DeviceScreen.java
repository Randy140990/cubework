package com.insidethecube.retromon.client;

import net.minecraft.client.gui.GuiGraphics;

/**
 * A page of the START menu (the menu itself, the Pokédex, the Town Map...), as the UI Viewer shows them: over the
 * whole window, the world frosted behind it. (It used to show on a Pokédex held up in your hands.)
 */
public abstract class DeviceScreen extends GameScreen {
	/** Where the page is (the whole window), for the mouse: wheel and drag. */
	protected int screenX, screenY, screenW, screenH;

	protected DeviceScreen(String title) {
		super(title);
	}

	/** The page. */
	protected abstract void drawScreen(GuiGraphics g, float partial);

	@Override
	protected final void draw(GuiGraphics g, float partial) {
		net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
		if (mc.options.getMenuBackgroundBlurriness() > 0) g.blurBeforeThisStratum();
		g.fill(0, 0, width, height, 0x330A1C20);
		screenX = 0;
		screenY = 0;
		screenW = width;
		screenH = height;
		drawScreen(g, partial);
	}

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
