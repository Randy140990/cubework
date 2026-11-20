package com.insidethecube.retromon.client;

import java.util.List;

import com.insidethecube.retromon.engine.Species;
import com.insidethecube.retromon.game.Net;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.nbt.CompoundTag;

/**
 * An in-game trade, as the UI Viewer's "Trading": yours and theirs side by side on their stands, a card for each
 * (You send, You get: name, level, types, trainer), TRADE or CANCEL. Trading: they go into their balls, which
 * cross over, and come out the other side. Then who sent what, and "Thanks! Take good care of it!".
 */
public class TradeScreen extends GameScreen {
	private enum Step {
		OFFER, CROSS, TALK
	}

	/** How long the balls take to cross (ticks): the viewer's 2.2 seconds. */
	private static final int CROSS = 44;
	private final CompoundTag tag;
	private final String send, get, sendName, player;
	private final int sendLv, getLv;
	private final Choice choice = new Choice(List.of("TRADE", "CANCEL"));
	private Step step = Step.OFFER;
	private int crossAt;
	private TextBox box;
	private boolean sent;

	public TradeScreen(CompoundTag t) {
		super("Trade");
		this.tag = t;
		this.send = t.getStringOr("send", "");
		this.get = t.getStringOr("get", "");
		this.sendName = t.getStringOr("sendName", name(send));
		this.sendLv = t.getIntOr("sendLv", 1);
		this.getLv = t.getIntOr("getLv", 1);
		this.player = t.getStringOr("player", "You");
	}

	private static String name(String species) {
		Species sp = Species.ALL.get(species);
		return sp == null ? species.toUpperCase() : sp.name();
	}

	private void pick(int i) {
		Sounds.select();
		if (i != 0) {
			close();
			return;
		}
		if (!sent) {
			sent = true;
			CompoundTag t = new CompoundTag();
			t.putString("static", tag.getStringOr("static", ""));
			t.putInt("i", tag.getIntOr("i", -1));
			t.putInt("npc", tag.getIntOr("npc", -1));
			ClientPlayNetworking.send(new Net.Act("trade_done", t));
		}
		step = Step.CROSS;
		crossAt = ticks;
	}

	@Override
	public void tick() {
		super.tick();
		if (box != null) {
			box.tick();
			box.speaker = box.message() == 2 ? "TRADER" : null;
		}
		if (step == Step.CROSS && ticks - crossAt >= CROSS) {
			step = Step.TALK;
			Sounds.cry(get, 1F);
			Music.jingle("caught_mon");
			box = new TextBox(List.of(player + " sent " + sendName + " to TRADER.", "TRADER sent " + name(get) + " to " + player + ".", "Thanks! Take good care of it!"));
		}
	}

	private void close() {
		if (minecraft != null && minecraft.screen == this) minecraft.setScreen(null);
	}

	@Override
	protected boolean key(KeyEvent e) {
		switch (step) {
			case OFFER -> {
				if (choice.key(e)) return true;
				if (Ui.isA(e)) pick(choice.at);
				else if (Ui.isB(e)) pick(1);
			}
			case TALK -> {
				if ((Ui.isA(e) || Ui.isB(e)) && box.press()) close();
			}
			default -> {
			}
		}
		return true;
	}

	@Override
	protected void clickAnywhere() {
		if (step == Step.TALK && box.press()) close();
	}

	@Override
	protected void back() {
		if (step == Step.OFFER) pick(1);
		else clickAnywhere();
	}

	@Override
	public void onClose() {
		if (step == Step.OFFER) close();
	}

	@Override
	protected void draw(GuiGraphics g, float partial) {
		float secs = (ticks + partial) / 20F;
		Scenes.bg2(g, "trade");
		float swap = step == Step.CROSS ? (ticks - crossAt + partial) / CROSS : step == Step.TALK ? 1 : -1;
		Scenes.trade(g, secs, send, get, swap);
		if (step == Step.OFFER) {
			card(g, 60, "You send", send, sendName, sendLv, player);
			card(g, V.W - 60 - 400, "You get", get, name(get), getLv, "TRADER");
			vchoice(g, choice, 490, 360, 300, this::pick);
		}
		if (box != null) box.renderV(g, font, 140, 26, 1000, ticks, partial, true);
	}

	/** One side's card (the viewer's): who, its name and level, its types, its trainer. */
	private void card(GuiGraphics g, float x, String who, String species, String name, int level, String trainer) {
		float y = 40, w = 400, h = 150;
		V.panel(g, x, y, w, h);
		V.label(g, Lang.t(who), x + 22, y + 16);
		V.title(g, name, x + 22, y + 16 + 18 + 4, 28, V.INK);
		V.right(g, "Lv. " + level, x + w - 22, y + 16 + 18 + 4 + 6, 20, V.INK, true);
		Species sp = Species.ALL.get(species);
		if (sp != null) {
			float tx = x + 22;
			tx += PokedexScreen.typeChip(g, sp.type1(), tx, y + 82) + 8;
			if (sp.type2() != null && sp.type2() != sp.type1()) PokedexScreen.typeChip(g, sp.type2(), tx, y + 82);
		}
		V.text(g, Lang.t("Trainer: {0}", trainer), x + 22, y + 116, 16, V.MUTED, true);
	}
}
