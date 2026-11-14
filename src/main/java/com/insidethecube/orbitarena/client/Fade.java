package com.insidethecube.orbitarena.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** A fade to black and back round going in and out of the arena. */
public final class Fade {
	private Fade() {}

	private static long start = -1;
	private static int out, hold, in;

	public static void start(int outTicks, int holdTicks, int inTicks) {
		start = System.currentTimeMillis();
		out = outTicks;
		hold = holdTicks;
		in = inTicks;
	}

	private static float alpha() {
		if (start < 0) return 0;
		float t = (System.currentTimeMillis() - start) / 50f;
		if (t < out) return t / out;
		if (t < out + hold) return 1;
		if (t < out + hold + in) return 1 - (t - out - hold) / in;
		start = -1;
		return 0;
	}

	public static void render(GuiGraphics g, DeltaTracker delta) {
		float a = alpha();
		if (a <= 0) return;
		Minecraft mc = Minecraft.getInstance();
		int argb = ((int) (a * 255) << 24);
		g.fill(0, 0, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), argb);
	}
}
