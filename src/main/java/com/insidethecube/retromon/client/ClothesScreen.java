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
 * change it, and it shows on you straight away. DONE or B keeps it (in your save); UNDO puts back what you had.
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
		// Picking the cap's style or which way it's on puts the cap on.
		if (part.equals("capp") || part.equals("capb")) outfit.v.put("hat", java.util.Arrays.asList(Outfit.HATS).indexOf("cap"));
		preview = outfit.write();
		Sounds.press();
	}

	/** UNDO: back to what you had when you came in (you're still choosing). */
	private void undo() {
		boolean girl = "girl".equals(ClientState.data.gender);
		Outfit was = Outfit.parse(before, girl);
		outfit.v.clear();
		outfit.v.putAll(was.v);
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

	/** B (or Esc) keeps what you're wearing, as the newer games' wardrobes do. */
	@Override
	protected void back() {
		done();
	}

	/** The UI Viewer's maker, with the clothes alone: UNDO puts back what you came in with. */
	private Maker maker;

	private Maker maker() {
		if (maker == null) {
			boolean girl = "girl".equals(ClientState.data.gender);
			maker = new Maker(outfit, girl, new String[] {"clothes"}, "UNDO", this::undo, this::back, () -> preview = maker.outfit.write());
			maker.maxH = 600;
		}
		return maker;
	}

	@Override
	protected boolean key(KeyEvent e) {
		maker().key(e);
		return true;
	}

	/** The wheel scrolls the maker's glass panel. */
	@Override
	public boolean mouseScrolled(double x, double y, double h, double v) {
		if (maker().scrolled(V.vx(x), V.vy(y), v)) return true;
		return super.mouseScrolled(x, y, h, v);
	}

	/** As the UI Viewer's: you on your stand at the left, turning gently, on a warm peach light; the glass maker at the right. */
	@Override
	protected void draw(GuiGraphics g, float partial) {
		Scenes.bg2(g, "clothes");
		boolean girl = "girl".equals(ClientState.data.gender);
		Scenes.stand(g, (ticks + partial) / 20F, Scenes.you(preview == null ? outfit.write() : preview, girl), 2.1, 0, true);
		maker().draw(g, this::vhit, (ticks + partial) / 20F);
		V.hints(g, "A", "Choose", "B", "Keep and close");
	}
}
