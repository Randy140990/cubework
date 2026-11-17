package com.insidethecube.kantojohto.client;

import java.util.List;

import com.insidethecube.kantojohto.game.Game;
import com.insidethecube.kantojohto.game.Net;
import com.insidethecube.kantojohto.game.PlayerData;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.nbt.CompoundTag;

/** The Poké Mart counter: what's for sale, what you've got, and your money. */
public class MartScreen extends GameScreen {
	private final Choice items = new Choice(List.of("POKé BALL", "POTION", "CANCEL"));
	private final TextBox box = new TextBox(List.of("What would you like?"));
	private boolean leaving;

	public MartScreen() {
		super("Mart");
	}

	@Override
	public void tick() {
		super.tick();
		box.tick();
	}

	@Override
	protected boolean key(KeyEvent e) {
		if (leaving) {
			if ((Ui.isA(e) || Ui.isB(e)) && box.press()) onClose();
			return true;
		}
		if (items.key(e)) return true;
		if (Ui.isB(e) || (Ui.isA(e) && items.at == 2)) {
			leaving = true;
			box.set(List.of("Please come again!"));
			return true;
		}
		if (Ui.isA(e)) {
			PlayerData d = ClientState.data;
			String item = items.at == 0 ? "ball" : "potion";
			int price = items.at == 0 ? Game.POKE_BALL_PRICE : Game.POTION_PRICE;
			if (d.money < price) {
				box.set(List.of("You don't have enough money."));
				return true;
			}
			CompoundTag t = new CompoundTag();
			t.putString("item", item);
			ClientPlayNetworking.send(new Net.Act("buy", t));
			// Shown straight away; the game's count follows.
			d.money -= price;
			if (items.at == 0) d.balls++;
			else d.potions++;
			Sounds.select();
			box.set(List.of("Here you are! Thank you!"));
			return true;
		}
		return true;
	}

	@Override
	public void onClose() {
		if (minecraft != null) minecraft.setScreen(null);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
		PlayerData d = ClientState.data;
		int h = boxHeight();
		box.render(g, font, 0, height - h, width, h, s, ticks, leaving);
		// Money, top right.
		String money = "MONEY  ¥" + d.money;
		int mw = Ui.width(font, money, s) + 16 * s;
		Ui.box(g, width - mw - 4 * s, 4 * s, mw, 20 * s, s);
		Ui.text(g, font, money, width - mw + 4 * s, 10 * s, Ui.INK, s);
		if (leaving) return;
		// What's for sale, with prices and what you have.
		int lw = Ui.width(font, "POKé BALL", s) + 90 * s, lh = items.height(s) + 4 * s, lx = width - lw - 4 * s, ly = height - h - lh - 2 * s;
		Ui.box(g, lx, ly, lw, lh, s);
		String[] price = {"¥" + Game.POKE_BALL_PRICE, "¥" + Game.POTION_PRICE, ""};
		String[] have = {"x" + d.balls, "x" + d.potions, ""};
		for (int i = 0; i < items.options.size(); i++) {
			int ty = ly + 8 * s + i * 12 * s;
			if (i == items.at) Ui.text(g, font, Ui.CURSOR, lx + 6 * s, ty, Ui.INK, s);
			Ui.text(g, font, items.options.get(i), lx + 14 * s, ty, Ui.INK, s);
			Ui.text(g, font, price[i], lx + lw - 30 * s - Ui.width(font, price[i], s), ty, Ui.INK, s);
			Ui.text(g, font, have[i], lx + lw - 8 * s - Ui.width(font, have[i], s), ty, Ui.GREY, s);
		}
	}
}
