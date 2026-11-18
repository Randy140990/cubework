package com.insidethecube.retromon.client;

import com.insidethecube.retromon.game.Net;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.nbt.CompoundTag;

/**
 * The wardrobe: you on the left (turning to follow the mouse), and your look on the right, row by row: skin,
 * hair and its colour, hat (the cap in a Pokémon's pattern, worn forwards or backwards) and its colour, top,
 * bottoms, shoes, and your bag (a backpack or a satchel) and its colour. Up and down pick a row, left and right
 * change it, and it shows on you straight away. DONE keeps it (in your save); B puts back what you had.
 */
public class ClothesScreen extends GameScreen {
	/** One a part of Outfit.PARTS, in its order. */
	private static final String[] ROW_NAMES = {"SKIN", "HAIR", "HAIR COLOUR", "HAT", "CAP STYLE", "CAP", "HAT COLOUR", "TOP", "TOP COLOUR", "BOTTOMS",
			"BOTTOMS COLOUR", "SHOES", "BAG", "BAG COLOUR"};
	/** While you choose, the skin shows this (null: what's saved). */
	public static String preview;

	private final Screen parent;
	private final Outfit outfit;
	private final String before;
	private int row;
	private float mouseX, mouseY;

	public ClothesScreen(Screen parent) {
		super("Clothes");
		this.parent = parent;
		boolean girl = "girl".equals(ClientState.data.gender);
		before = ClientState.data.outfit;
		outfit = Outfit.parse(before, girl);
		preview = outfit.write();
	}

	private void change(int r, int d) {
		String part = Outfit.PARTS[r];
		outfit.v.put(part, Math.floorMod(outfit.get(part) + d, Outfit.options(part)));
		preview = outfit.write();
		Sounds.press();
	}

	private void done() {
		Sounds.sfx("save");
		CompoundTag t = new CompoundTag();
		t.putString("outfit", outfit.write());
		ClientPlayNetworking.send(new Net.Act("outfit", t));
		ClientState.data.outfit = outfit.write();
		preview = null;
		if (minecraft != null) minecraft.setScreen(parent);
	}

	@Override
	protected void back() {
		Sounds.press();
		preview = null;
		ClientState.data.outfit = before;
		if (minecraft != null) minecraft.setScreen(parent);
	}

	@Override
	protected boolean key(KeyEvent e) {
		int n = ROW_NAMES.length + 1;
		if (Ui.isUp(e)) row = Math.floorMod(row - 1, n);
		else if (Ui.isDown(e)) row = Math.floorMod(row + 1, n);
		else if (row < ROW_NAMES.length && (Ui.isLeft(e) || Ui.isRight(e))) change(row, Ui.isLeft(e) ? -1 : 1);
		else if (Ui.isA(e)) {
			if (row == ROW_NAMES.length) done();
			else change(row, 1);
		} else if (Ui.isB(e)) back();
		return true;
	}

	@Override
	protected void draw(GuiGraphics g, float partial) {
		// The world behind, frosted.
		if (minecraft != null && minecraft.options.getMenuBackgroundBlurriness() > 0) g.blurBeforeThisStratum();
		g.fill(0, 0, width, height, 0x990D1222);
		if (minecraft != null) {
			mouseX = (float) (minecraft.mouseHandler.xpos() * width / Math.max(1, minecraft.getWindow().getScreenWidth()));
			mouseY = (float) (minecraft.mouseHandler.ypos() * height / Math.max(1, minecraft.getWindow().getScreenHeight()));
		}
		int m = 8 * s;
		// You, on a soft spotlight.
		int pw = Math.min(width / 2 - m, 150 * s), ph = height - 2 * m;
		Ui.round(g, m, m, pw, ph, 0xE6141B2E, 3 * s);
		Ui.round(g, m + pw / 2 - 40 * s, m + ph - 34 * s, 80 * s, 14 * s, 0x40FFF4D8, 7 * s);
		if (minecraft != null && minecraft.player != null) {
			InventoryScreen.renderEntityInInventoryFollowsMouse(g, m + 6 * s, m + 6 * s, m + pw - 6 * s, m + ph - 20 * s, (int) (ph * 0.36F), 0.0625F,
					mouseX, mouseY, minecraft.player);
		}
		Ui.heading(g, font, "CLOTHES", m + 10 * s, m + 8 * s, Ui.TEXT, s);
		// The rows.
		int x = m + pw + 8 * s, w = width - x - m, y0 = m;
		int pitch = Math.max(11 * s, Math.min(17 * s, (height - 2 * m - 40 * s) / ROW_NAMES.length)), rh = pitch - 2 * s;
		boolean cap = Outfit.HATS[Math.floorMod(outfit.get("hat"), Outfit.HATS.length)].equals("cap");
		for (int r = 0; r < ROW_NAMES.length; r++) {
			int y = y0 + r * pitch;
			boolean on = r == row;
			if (over(x, y, w, rh)) row = r;
			Ui.pill(g, x, y, w, rh, on, s);
			String part = Outfit.PARTS[r];
			// The cap's own rows only matter with the cap on.
			boolean idle = !cap && (part.equals("capp") || part.equals("capb"));
			int ty = y + Math.max(s, (rh - 8 * s) / 2);
			Ui.draw(g, font, Ui.bold(ROW_NAMES[r]), x + 8 * s, ty, on ? Ui.CREAM_INK : idle ? Ui.DIM : Ui.TEXT, s, false);
			int val = outfit.get(part);
			String label = Outfit.label(part, Math.floorMod(val, Outfit.options(part)));
			int vx = x + w - 92 * s;
			if (!part.equals("skin") && (part.endsWith("c") || part.equals("shoec"))) {
				int col = 0xFF000000 | Outfit.COLOURS[Math.floorMod(val, Outfit.COLOURS.length)];
				Ui.round(g, vx + 12 * s, ty - s, 9 * s, 9 * s, col, 2 * s);
			}
			Ui.draw(g, font, Ui.ui("◀"), vx, ty, on ? Ui.CREAM_INK : Ui.MUTED, s, false);
			Ui.draw(g, font, Ui.bold(label), vx + 26 * s, ty, on ? Ui.CREAM_INK : idle ? Ui.DIM : Ui.TEXT, s, false);
			Ui.draw(g, font, Ui.ui("▶"), x + w - 10 * s, ty, on ? Ui.CREAM_INK : Ui.MUTED, s, false);
			int rr = r;
			hit(vx - 2 * s, y, 12 * s, rh, () -> change(rr, -1));
			hit(vx + 12 * s, y, w - (vx - x) - 12 * s, rh, () -> change(rr, 1));
		}
		int by = y0 + ROW_NAMES.length * pitch + 4 * s, bw = (w - 4 * s) / 2;
		boolean onDone = row == ROW_NAMES.length;
		Ui.button(g, font, x, by, bw, 16 * s, "DONE", Ui.GREEN, onDone || over(x, by, bw, 16 * s), s);
		hit(x, by, bw, 16 * s, this::done);
		Ui.button(g, font, x + bw + 4 * s, by, bw, 16 * s, "CANCEL", 0xFF5A6070, over(x + bw + 4 * s, by, bw, 16 * s), s);
		hit(x + bw + 4 * s, by, bw, 16 * s, this::back);
		Ui.text(g, font, "Up/down: pick   Left/right: change", x, by + 22 * s, Ui.DIM, s);
	}
}
