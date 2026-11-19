package com.insidethecube.retromon.client;

import java.util.List;

import com.insidethecube.retromon.Retromon;
import com.insidethecube.retromon.engine.Species;
import com.insidethecube.retromon.game.Net;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;

/**
 * Your first Pokémon. As in the games, Oak stops you as you head out of Pallet Town ("Hey! Wait! Don't go
 * out!") and takes you to his lab: Bulbasaur, Charmander and Squirtle stand on the table in 3D, idling, and the
 * one you point at hops and twirls with a heart over it and its cry. Oak stands on the text box as he talks.
 */
public class StarterScreen extends GameScreen {
	private static final String[] IDS = {"bulbasaur", "charmander", "squirtle"};
	private static final String[] KINDS = {"GRASS", "FIRE", "WATER"};
	private static final int[] COLORS = {0xFF5DB850, 0xFFF08436, 0xFF4D90D5};
	private static final Identifier OAK = Retromon.id("textures/entity/npc/oak.png");

	private enum Step {
		STOP, WELCOME, CHOOSE, CONFIRM, AFTER
	}

	private Step step = Step.WELCOME;
	/** Going to the lab: ticks into the fade to black and back (-1 when not). */
	private int toLab = -1;
	private static final int LAB_FADE = 24;
	private List<String> labLines = List.of();
	private final TextBox box;
	private int at = 1, lastAt = -1, atSince;
	private Choice yesNo;
	private final String name;

	/** stopped: Oak caught you heading out of town (the games' way); otherwise his welcome (an old save). */
	public StarterScreen(boolean stopped) {
		super("Starter");
		Minecraft mc = Minecraft.getInstance();
		String n = ClientState.data.name;
		name = n != null && !n.isEmpty() ? n : mc.player == null ? "YOU" : mc.player.getGameProfile().name().toUpperCase();
		if (stopped) {
			step = Step.STOP;
			labLines = List.of("Here, " + name + "! There are 3 POKéMON here! Haha!",
					"They are inside the POKé BALLS. When I was young, I was a serious POKéMON trainer!",
					"In my old age, I have only 3 left, but you can have one! Choose!");
		}
		box = new TextBox(stopped
				? List.of("Hey! Wait! Don't go out!", "It's unsafe! Wild POKéMON live in tall grass!",
						"You need your own POKéMON for your protection. I know! Here, come with me!")
				: List.of("Hello there! Welcome to the world of POKéMON!", "My name is OAK! People call me the POKéMON PROF!",
						name + "! Your very own POKéMON legend is about to unfold!", "But first, you'll need a POKéMON of your own. Choose one!"));
		box.speaker = "PROF. OAK";
		Music.play("meet_prof_oak");
	}

	public StarterScreen() {
		this(false);
	}

	/** Talking to Oak in his lab before you've a Pokémon: the three balls, straight away. */
	public static StarterScreen inLab() {
		StarterScreen s = new StarterScreen(false);
		s.box.set(List.of("Ah, " + s.name + "! There are 3 POKéMON here! Haha!", "They are inside the POKé BALLS. You can have one! Choose!"));
		return s;
	}

	@Override
	public void tick() {
		super.tick();
		box.tick();
		if (toLab >= 0) {
			toLab++;
			// At the darkest, you're at the lab's door and Oak goes on.
			if (toLab == LAB_FADE / 2) {
				// The lab's inside is its own place: Minecraft puts up its loading screen over this one while you
				// go, so this comes back when that's done, and Oak carries on.
				GameScreen.resume = this;
				ClientPlayNetworking.send(new Net.Act("lab", new CompoundTag()));
				step = Step.WELCOME;
				box.set(labLines);
			}
			if (toLab >= LAB_FADE) toLab = -1;
		}
	}

	private void choose(int i) {
		at = i;
		Sounds.press();
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
		Sounds.cry(IDS[at], 1F);
		atSince = ticks;
		step = Step.AFTER;
		String mon = Species.get(IDS[at]).name();
		box.set(List.of(name + " received " + mon + "!", "Here are some POTIONS to start you off.",
				"If a wild POKéMON appears, your POKéMON can fight against it!",
				"ROUTE 1 is just north of here. Afterward, go on to the next town, VIRIDIAN CITY.", "Press ENTER for your menu. Good luck, " + name + "!"));
	}

	private void advance() {
		if (toLab >= 0) return;
		if (GameScreen.resume == this) GameScreen.resume = null;
		if (step == Step.STOP && box.press()) {
			toLab = 0;
			Sounds.sfx("run");
		} else if (step == Step.WELCOME && box.press()) {
			step = Step.CHOOSE;
			lastAt = -1;
			box.set(List.of("Which POKéMON will you choose?"));
		} else if (step == Step.AFTER && box.press()) {
			Music.resumePlace();
			if (minecraft != null) minecraft.setScreen(null);
		}
	}

	@Override
	protected boolean key(KeyEvent e) {
		switch (step) {
			case STOP, WELCOME, AFTER -> {
				if (Ui.isA(e) || Ui.isB(e)) advance();
			}
			case CHOOSE -> {
				if (Ui.isLeft(e) || Ui.isRight(e) || Ui.isUp(e) || Ui.isDown(e)) at = Math.floorMod(at + (Ui.isLeft(e) || Ui.isUp(e) ? -1 : 1), 3);
				else if (Ui.isA(e)) choose(at);
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
		int h = boxHeight(), m = 6 * s, boxTop = height - h - m - height / 14;
		float secs = (ticks + partial) / 20F;
		if (step == Step.STOP) g.fillGradient(0, height / 2, width, height, 0x000A0E24, 0x900A0E24);
		else g.fillGradient(0, 0, width, height, 0x700A0E24, 0xC00A0E24);
		Figures.Batch b = Figures.batch(0, 0, width, height);
		boolean choosing = step == Step.CHOOSE || step == Step.CONFIRM;
		if (choosing) {
			// The three on the table, each on its card.
			int cw = Math.min(118 * s, (width - 6 * m) / 3), gap = 2 * m, total = 3 * cw + 2 * gap, x0 = (width - total) / 2;
			int ch = Math.min(124 * s, boxTop - height / 9 - 18 * s), y = boxTop - ch - 16 * s;
			if (step == Step.CHOOSE) for (int i = 0; i < 3; i++) if (over(x0 + i * (cw + gap), y, cw, ch)) at = i;
			if (at != lastAt) {
				if (lastAt >= 0 || step == Step.CHOOSE) Sounds.cry(IDS[at], 1F);
				lastAt = at;
				atSince = ticks;
			}
			float since = ticks - atSince + partial;
			int tall = (int) (ch * 0.48F);
			for (int i = 0; i < 3; i++) {
				int x = x0 + i * (cw + gap);
				int idx = i;
				boolean on = i == at;
				int lift = on ? 4 * s : 0;
				Ui.rr(g, x - s, y - s - lift, cw + 2 * s, ch + 2 * s, 9 * s, on ? 0xFFFFFFFF : 0x38FFFFFF);
				Ui.rrv(g, x, y - lift, cw, ch, 8 * s, on ? Ui.darker(COLORS[i], 0.62F) | 0xFF000000 : 0xF0154440, on ? 0xF0103430 : 0xF00A2422);
				// A round pad for it to stand on, in its colour.
				int cx = x + cw / 2, feet = y - lift + 14 * s + tall;
				Ui.glow(g, cx, feet - tall / 2, cw / 2, tall / 2 + 4 * s, on ? 0x10FFFFFF : 0x06FFFFFF);
				Ui.round(g, cx - cw * 3 / 8, feet - 5 * s, cw * 3 / 4, 10 * s, Ui.alpha(COLORS[i], on ? 0.75F : 0.4F), 4 * s);
				Ui.round(g, cx - cw * 3 / 10, feet - 3 * s, cw * 3 / 5, 6 * s, Ui.alpha(0xFFFFFFFF, on ? 0.35F : 0.15F), 3 * s);
				Figures.Fig f = b.mon(IDS[i], cx, feet, tall).at(secs + i * 0.7F).pitch(10).facing(i == 0 ? 14 : i == 2 ? -14 : 0);
				if (on && step == Step.CHOOSE) f.action("cry", since / 20F);
				if (on && step == Step.CHOOSE) {
					// Hops twice, twirling on the first, and lands with a squash.
					if (since < 22) {
						float p = since / 11F;
						float arc = (float) Math.abs(Math.sin(p * Math.PI));
						f.hop(arc * 12 * s).squash(arc < 0.25F ? 0.88F + arc * 0.48F : 1.04F);
						if (since < 11) f.spin(since / 11F * 360F);
					} else f.hop((float) Math.abs(Math.sin((since - 22) / 20F * Math.PI)) * 2 * s);
				}
				String nm = Species.get(IDS[i]).name();
				Ui.draw(g, font, Ui.bold(nm), cx - Ui.boldWidth(font, nm, s) / 2, y - lift + ch - 30 * s, 0xFFFFFFFF, s, true);
				Ui.chip(g, font, KINDS[i], cx - (Ui.width(font, KINDS[i], s) + 6 * s) / 2, y - lift + ch - 17 * s, COLORS[i], s);
				if (step == Step.CHOOSE) hit(x, y - lift, cw, ch, () -> choose(idx));
			}
			b.draw(g);
			int cx = x0 + at * (cw + gap) + cw / 2;
			Ui.bubble(g, "heart", cx + tall / 3, y + 10 * s, s, step == Step.CHOOSE ? Math.min(1, Math.max(0, since - 4) / 6F) : 1);
		} else {
			// Oak on the right, standing on the text box; after you've chosen, yours beside him, pleased.
			int top = height / 9 + 4 * s, tall = boxTop - top + 2 * s, ox = width - 16 * s - tall / 3 - 6 * s;
			Ui.glow(g, ox, boxTop - tall / 2, tall * 2 / 5, tall / 2, 0x0AFFF4D8);
			b.person(OAK, ox, boxTop + 2 * s, tall).facing(-22).pitch(-2).talk(box.typedOut() ? 0.25F : 1).at(secs);
			if (step == Step.AFTER) {
				float since = ticks - atSince + partial;
				int mt = tall * 2 / 5, mxp = width / 2;
				Ui.round(g, mxp - mt / 2, boxTop - 3 * s, mt, 6 * s, 0x50000000, 3 * s);
				float hop = since < 30 ? (float) Math.abs(Math.sin(since / 10F * Math.PI)) * 10 * s : 0;
				b.mon(IDS[at], mxp, boxTop + s, mt).at(secs).pitch(6).facing(10).hop(hop).action("cry", since / 20F);
				b.draw(g);
				Ui.bubble(g, "star", mxp + mt / 3, boxTop - mt - (int) hop, s, Math.min(1, since / 6F));
			} else b.draw(g);
		}
		box.render(g, font, m * 2, boxTop, width - 4 * m, h, s, ticks, step == Step.STOP || step == Step.WELCOME || step == Step.AFTER);
		if (toLab >= 0) {
			float t = (toLab + partial) / LAB_FADE;
			float a = Math.min(1, (t < 0.5F ? t : 1 - t) * 2.6F);
			g.fill(0, 0, width, height, Ui.alpha(0xFF05070F, a));
		}
		if (step == Step.CONFIRM) {
			int w = yesNo.width(font, s);
			choice(g, yesNo, width - w - m * 2, boxTop - yesNo.height(s) - 4 * s, w, i -> confirm(i == 0));
		}
	}
}
