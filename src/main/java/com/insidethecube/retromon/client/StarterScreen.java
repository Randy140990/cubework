package com.insidethecube.retromon.client;

import java.util.List;

import com.insidethecube.retromon.engine.Species;
import com.insidethecube.retromon.game.Net;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.nbt.CompoundTag;

/** The start of your adventure: the professor's welcome, and choosing your first Pokémon. */
public class StarterScreen extends GameScreen {
	private static final String[] IDS = {"bulbasaur", "charmander", "squirtle"};
	private static final String[] KINDS = {"GRASS", "FIRE", "WATER"};
	private static final int[] COLORS = {0xFF5DB850, 0xFFF08436, 0xFF4D90D5};

	private enum Step {
		WELCOME, CHOOSE, CONFIRM, AFTER
	}

	private Step step = Step.WELCOME;
	private final TextBox box;
	private int at;
	private Choice yesNo;
	private final String name;

	public StarterScreen() {
		super("Starter");
		Minecraft mc = Minecraft.getInstance();
		name = mc.player == null ? "YOU" : mc.player.getGameProfile().name().toUpperCase();
		box = new TextBox(List.of("Hello there! Welcome to the world of POKéMON!", "My name is OAK! People call me the POKéMON PROF!",
				"This world is inhabited by creatures called POKéMON!", "For some people, POKéMON are pets. Others use them for fights.",
				name + "! Your very own POKéMON legend is about to unfold!", "But first, you'll need a POKéMON of your own. Choose one!"));
		box.speaker = "PROF.OAK";
		Music.play("meet_prof_oak");
	}

	@Override
	public void tick() {
		super.tick();
		box.tick();
	}

	private void choose(int i) {
		at = i;
		step = Step.CONFIRM;
		yesNo = new Choice(List.of("YES", "NO"));
		box.set(List.of("So! You want the " + KINDS[i] + " POKéMON, " + Species.get(IDS[i]).name() + "?"));
	}

	private void confirm(boolean yes) {
		if (!yes) {
			step = Step.CHOOSE;
			box.set(List.of("Which POKéMON will you choose?"));
			return;
		}
		CompoundTag t = new CompoundTag();
		t.putString("species", IDS[at]);
		ClientPlayNetworking.send(new Net.Act("starter", t));
		Sounds.jingle("get_key_item");
		step = Step.AFTER;
		String mon = Species.get(IDS[at]).name();
		box.set(List.of(name + " received " + mon + "!", "Here are some POKé BALLS and POTIONS to start you off.",
				"Wild POKéMON live in the tall grass. ROUTE 1 is just north of here.",
				"Heal at the POKéMON CENTER in VIRIDIAN CITY, and shop at the MART.", "Press ENTER for your menu. Good luck, " + name + "!"));
	}

	private void advance() {
		if (step == Step.WELCOME && box.press()) {
			step = Step.CHOOSE;
			box.set(List.of("Which POKéMON will you choose?"));
		} else if (step == Step.AFTER && box.press()) {
			Music.resumePlace();
			if (minecraft != null) minecraft.setScreen(null);
		}
	}

	@Override
	protected boolean key(KeyEvent e) {
		switch (step) {
			case WELCOME, AFTER -> {
				if (Ui.isA(e) || Ui.isB(e)) advance();
			}
			case CHOOSE -> {
				if (Ui.isLeft(e) || Ui.isRight(e) || Ui.isUp(e) || Ui.isDown(e)) {
					at = Math.floorMod(at + (Ui.isLeft(e) || Ui.isUp(e) ? -1 : 1), 3);
					Sounds.select();
					Sounds.cry(IDS[at], 1F);
				} else if (Ui.isA(e)) choose(at);
			}
			case CONFIRM -> {
				if (yesNo.key(e)) return true;
				if (Ui.isB(e)) confirm(false);
				else if (Ui.isA(e)) confirm(yesNo.at == 0);
			}
		}
		return true;
	}

	@Override
	protected void clickAnywhere() {
		advance();
	}

	@Override
	protected void back() {
		if (step == Step.CONFIRM) confirm(false);
		else advance();
	}

	@Override
	public void onClose() {
	}

	@Override
	protected void draw(GuiGraphics g, float partial) {
		int h = boxHeight(), m = 6 * s;
		if (step == Step.CHOOSE || step == Step.CONFIRM) {
			// The three POKé BALLS on the professor's table, with what each is.
			int cw = Math.min(110 * s, (width - 4 * m) / 3), gap = m, total = 3 * cw + 2 * gap, x0 = (width - total) / 2, ch = 64 * s, y = height / 2 - ch;
			for (int i = 0; i < 3; i++) {
				int x = x0 + i * (cw + gap);
				int idx = i;
				if (step == Step.CHOOSE && over(x, y, cw, ch) && at != i) {
					at = i;
					Sounds.cry(IDS[i], 1F);
				}
				boolean on = i == at;
				Ui.round(g, x, y, cw, ch, on ? 0xFFFFFFFF : Ui.EDGE, s);
				Ui.round(g, x + s, y + s, cw - 2 * s, ch - 2 * s, on ? Ui.darker(COLORS[i], 0.75F) : Ui.PANEL, s);
				// A Poké Ball, drawn simply.
				int bx = x + cw / 2, by = y + 18 * s, r = 9 * s;
				g.fill(bx - r, by - r, bx + r, by, 0xFFE5525C);
				g.fill(bx - r, by, bx + r, by + r, 0xFFF4F6FF);
				g.fill(bx - r, by - s, bx + r, by + s, Ui.INK);
				g.fill(bx - 3 * s, by - 3 * s, bx + 3 * s, by + 3 * s, Ui.INK);
				g.fill(bx - 2 * s, by - 2 * s, bx + 2 * s, by + 2 * s, 0xFFF4F6FF);
				Ui.centred(g, font, Species.get(IDS[i]).name(), x + cw / 2, y + 34 * s, Ui.TEXT, s, true);
				Ui.chip(g, font, KINDS[i], x + cw / 2 - (Ui.width(font, KINDS[i], s) + 6 * s) / 2, y + 47 * s, COLORS[i], s);
				if (step == Step.CHOOSE) hit(x, y, cw, ch, () -> choose(idx));
			}
		}
		box.render(g, font, m, height - h - m, width - 2 * m, h, s, ticks, step == Step.WELCOME || step == Step.AFTER);
		if (step == Step.CONFIRM) {
			int w = yesNo.width(font, s);
			choice(g, yesNo, width - w - m, height - h - m - yesNo.height(s) - 4 * s, w, i -> confirm(i == 0));
		}
	}
}
