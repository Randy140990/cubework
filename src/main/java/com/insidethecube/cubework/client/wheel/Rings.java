package com.insidethecube.cubework.client.wheel;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import net.minecraft.client.renderer.RenderPipelines;
import org.joml.Matrix3x2f;

/**
 * Smooth rings and ring segments for the wheel, as flat coloured quads (the game's GUI only draws
 * rectangles; these are drawn the same way, so they sit in the menu like anything else).
 */
final class Rings {
	private float[] xy = new float[2048];
	private int[] colour = new int[1024];
	private int quads;

	/** Colour by angle (0 at the top, clockwise) and by inner (false) or outer (true) edge. */
	interface Paint {
		int at(double angle, boolean outer);
	}

	/** A ring segment from a0 to a1 (radians, 0 at the top, clockwise) between radii r0 and r1. */
	void sector(float cx, float cy, float r0, float r1, double a0, double a1, Paint paint) {
		int steps = Math.max(1, (int) Math.ceil(Math.abs(a1 - a0) / Math.toRadians(4)));
		for (int i = 0; i < steps; i++) {
			double s = a0 + (a1 - a0) * i / steps, t = a0 + (a1 - a0) * (i + 1) / steps;
			float sx = (float) Math.sin(s), sy = (float) -Math.cos(s), tx = (float) Math.sin(t), ty = (float) -Math.cos(t);
			quad(cx + sx * r0, cy + sy * r0, paint.at(s, false), cx + sx * r1, cy + sy * r1, paint.at(s, true),
					cx + tx * r1, cy + ty * r1, paint.at(t, true), cx + tx * r0, cy + ty * r0, paint.at(t, false));
		}
	}

	void ring(float cx, float cy, float r0, float r1, Paint paint) {
		sector(cx, cy, r0, r1, 0, Math.PI * 2, paint);
	}

	/** A line from the middle outwards at an angle, between radii r0 and r1, w wide. */
	void spoke(float cx, float cy, float r0, float r1, double a, float w, int c) {
		float dx = (float) Math.sin(a), dy = (float) -Math.cos(a), nx = -dy * w / 2, ny = dx * w / 2;
		quad(cx + dx * r0 - nx, cy + dy * r0 - ny, c, cx + dx * r1 - nx, cy + dy * r1 - ny, c, cx + dx * r1 + nx, cy + dy * r1 + ny, c,
				cx + dx * r0 + nx, cy + dy * r0 + ny, c);
	}

	void rect(float x0, float y0, float x1, float y1, int top, int bottom) {
		quad(x0, y0, top, x0, y1, bottom, x1, y1, bottom, x1, y0, top);
	}

	private void quad(float x0, float y0, int c0, float x1, float y1, int c1, float x2, float y2, int c2, float x3, float y3, int c3) {
		// Wound the same way as the game's own rectangles.
		double area = (x0 * y1 - x1 * y0) + (x1 * y2 - x2 * y1) + (x2 * y3 - x3 * y2) + (x3 * y0 - x0 * y3);
		if (area > 0) {
			float tx = x1, ty = y1;
			int tc = c1;
			x1 = x3;
			y1 = y3;
			c1 = c3;
			x3 = tx;
			y3 = ty;
			c3 = tc;
		}
		if ((quads + 1) * 8 > xy.length) {
			xy = java.util.Arrays.copyOf(xy, xy.length * 2);
			colour = java.util.Arrays.copyOf(colour, colour.length * 2);
		}
		int i = quads * 8, j = quads * 4;
		xy[i] = x0;
		xy[i + 1] = y0;
		xy[i + 2] = x1;
		xy[i + 3] = y1;
		xy[i + 4] = x2;
		xy[i + 5] = y2;
		xy[i + 6] = x3;
		xy[i + 7] = y3;
		colour[j] = c0;
		colour[j + 1] = c1;
		colour[j + 2] = c2;
		colour[j + 3] = c3;
		quads++;
	}

	/** Hands everything drawn so far to the GUI, then starts again. */
	void submit(GuiGraphics g) {
		if (quads == 0) return;
		g.guiRenderState.submitGuiElement(new Shapes(new Matrix3x2f(g.pose()), java.util.Arrays.copyOf(xy, quads * 8),
				java.util.Arrays.copyOf(colour, quads * 4), quads, new ScreenRectangle(0, 0, g.guiWidth(), g.guiHeight())));
		quads = 0;
	}

	private record Shapes(Matrix3x2f pose, float[] xy, int[] colour, int quads, ScreenRectangle bounds) implements GuiElementRenderState {
		@Override
		public void buildVertices(VertexConsumer out) {
			for (int q = 0; q < quads; q++) {
				for (int k = 0; k < 4; k++) {
					int i = q * 4 + k;
					out.addVertexWith2DPose(pose, xy[i * 2], xy[i * 2 + 1]).setColor(colour[i]);
				}
			}
		}

		@Override
		public RenderPipeline pipeline() {
			return RenderPipelines.GUI;
		}

		@Override
		public TextureSetup textureSetup() {
			return TextureSetup.noTexture();
		}

		@Override
		public ScreenRectangle scissorArea() {
			return null;
		}
	}

	/** Mixes two colours (0xAARRGGBB). */
	static int mix(int a, int b, double t) {
		t = Math.max(0, Math.min(1, t));
		int out = 0;
		for (int s = 0; s < 32; s += 8) {
			int x = (a >>> s) & 0xFF, y = (b >>> s) & 0xFF;
			out |= ((int) Math.round(x + (y - x) * t) & 0xFF) << s;
		}
		return out;
	}

	/** The same colour with its alpha scaled. */
	static int fade(int c, double f) {
		int a = (int) Math.round(((c >>> 24) & 0xFF) * Math.max(0, Math.min(1, f)));
		return (a << 24) | (c & 0xFFFFFF);
	}
}
