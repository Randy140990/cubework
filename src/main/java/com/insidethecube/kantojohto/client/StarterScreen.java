package com.insidethecube.kantojohto.client;

import java.util.List;

import com.insidethecube.kantojohto.game.Net;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.nbt.CompoundTag;

/** The start of your adventure: the professor's welcome, and choosing your first Pokémon. */
public class StarterScreen extends GameScreen {
	private static final String[] IDS = {"bulbasaur", "charmander", "squirtle"};
	private static final String[] NAMES = {"BULBASAUR", "CHARMANDER", "SQUIRTLE"};
	private static final String[] KINDS = {"GRASS", "FIRE", "WATER"};

	private enum Step {
		WELCOME, CHOOSE, CONFIRM, AFTER
	}

	private Step step = Step.WELCOME;
	private final TextBox box;
	private final Choice choose = new Choice(List.of(NAMES));
	private Choice yesNo;
	private final String name;

	public StarterScreen() {
		super("Starter");
		Minecraft mc = Minecraft.getInstance();
		name = mc.player == null ? "YOU" : mc.player.getGameProfile().name().toUpperCase();
		box = new TextBox(List.of("Hello there! Welcome to the world of POKéMON!", "My name is OAK! People call me the POKéMON PROF!",
				"This world is inhabited by creatures called POKéMON!", "For some people, POKéMON are pets. Others use them for fights.",
				name + "! Your very own POKéMON legend is about to unfold!", "But first, you'll need a POKéMON of your own. Choose one!"));
	}

	@Override
	public void tick() {
		super.tick();
		box.tick();
	}

	@Override
	protected boolean key(KeyEvent e) {
		switch (step) {
			case WELCOME -> {
				if ((Ui.isA(e) || Ui.isB(e)) && box.press()) {
					step = Step.CHOOSE;
					box.set(List.of("Which POKéMON will you choose?"));
				}
			}
			case CHOOSE -> {
				if (Ui.isLeft(e) || Ui.isRight(e)) {
					choose.at = Math.floorMod(choose.at + (Ui.isLeft(e) ? -1 : 1), 3);
					Sounds.select();
					Sounds.cry(IDS[choose.at], 1F);
					return true;
				}
				if (choose.key(e)) {
					Sounds.cry(IDS[choose.at], 1F);
					return true;
				}
				if (Ui.isA(e)) {
					step = Step.CONFIRM;
					yesNo = new Choice(List.of("YES", "NO"));
					box.set(List.of("So! You want the " + KINDS[choose.at] + " POKéMON, " + NAMES[choose.at] + "?"));
				}
			}
			case CONFIRM -> {
				if (yesNo.key(e)) return true;
				if (Ui.isB(e) || (Ui.isA(e) && yesNo.at == 1)) {
					step = Step.CHOOSE;
					box.set(List.of("Which POKéMON will you choose?"));
				} else if (Ui.isA(e)) {
					CompoundTag t = new CompoundTag();
					t.putString("species", IDS[choose.at]);
					ClientPlayNetworking.send(new Net.Act("starter", t));
					Sounds.caught();
					step = Step.AFTER;
					box.set(List.of(name + " received " + NAMES[choose.at] + "!", "Here are some POKé BALLS and POTIONS to start you off.",
							"Wild POKéMON live in the tall grass. ROUTE 1 is just north of here.",
							"Heal at the POKéMON CENTER in VIRIDIAN CITY, and shop at the MART.", "Press ENTER for your menu. Good luck, " + name + "!"));
				}
			}
			case AFTER -> {
				if ((Ui.isA(e) || Ui.isB(e)) && box.press()) onClose();
			}
		}
		return true;
	}

	@Override
	public void onClose() {
		if (step == Step.AFTER && minecraft != null) minecraft.setScreen(null);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
		int h = boxHeight();
		if (step == Step.CHOOSE || step == Step.CONFIRM) {
			// The three on the professor's table, with what each is.
			int cw = width / 3;
			for (int i = 0; i < 3; i++) {
				int x = i * cw + cw / 2;
				boolean on = i == choose.at;
				int bw = Math.min(cw - 8 * s, 110 * s), bh = 40 * s, bx = x - bw / 2, by = height / 2 - bh;
				Ui.box(g, bx, by, bw, bh, s);
				Ui.centred(g, font, (on ? Ui.CURSOR + " " : "") + NAMES[i], x, by + 10 * s, Ui.INK, s, false);
				Ui.centred(g, font, KINDS[i], x, by + 24 * s, on ? 0xFFC03028 : Ui.GREY, s, false);
			}
		}
		box.render(g, font, 0, height - h, width, h, s, ticks, step != Step.CHOOSE && step != Step.CONFIRM);
		if (step == Step.CONFIRM) yesNo.render(g, font, width - yesNo.width(font, s) - 2 * s, height - h - yesNo.height(s), s);
	}
}
