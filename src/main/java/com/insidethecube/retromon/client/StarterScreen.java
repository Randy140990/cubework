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
	/** Each card's colours, light at the top to deep at the foot: leafy, fiery, sea. */
	private static final int[][] CARD = {{0xFF9BE08A, 0xFF4FB848, 0xFF2A7A34}, {0xFFFFC06E, 0xFFF0782E, 0xFFB8401E}, {0xFF8CCBF6, 0xFF3F8EDC, 0xFF2459A6}};
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
		// The text box is the UI Viewer's (its canvas: 140 from the left, 1000 wide, 26 up from the bottom); the cards and
		// people stand on its top.
		int h = boxHeight(), m = 6 * s, boxTop = Math.round(V.y(V.H - 26 - 120));
		float secs = (ticks + partial) / 20F;
		if (step == Step.STOP) g.fillGradient(0, height / 2, width, height, 0x000A0E24, 0x900A0E24);
		else g.fillGradient(0, 0, width, height, 0x700A0E24, 0xC00A0E24);
		Figures.Batch b = Figures.batch(0, 0, width, height);
		boolean choosing = step == Step.CHOOSE || step == Step.CONFIRM;
		if (choosing) {
			// The three on the table, each on a card in its own type's colours (leafy greens, fiery oranges, sea
			// blues), lit from the top; the one you're on lifts, brighter, with a white rim. Canvas units, as the
			// UI Viewer's.
			float cw = 300, ch = 330, gap = 40, x0 = 640 - (3 * cw + 2 * gap) / 2, y = 214;
			if (step == Step.CHOOSE) for (int i = 0; i < 3; i++) if (vover(x0 + i * (cw + gap), y, cw, ch)) at = i;
			if (at != lastAt) {
				if (lastAt >= 0 || step == Step.CHOOSE) Sounds.cry(IDS[at], 1F);
				lastAt = at;
				atSince = ticks;
			}
			float since = ticks - atSince + partial;
			float tall = ch * 0.5F;
			for (int i = 0; i < 3; i++) {
				float x = x0 + i * (cw + gap), cx = x + cw / 2;
				int idx = i;
				boolean on = i == at;
				float lift = on ? 12 : 0, top = y - lift;
				int[] c = CARD[i];
				int[] cols = on ? c : new int[] {Ui.mix(c[0], 0xFF0E2A28, 0.42F), Ui.mix(c[1], 0xFF0E2A28, 0.5F), Ui.mix(c[2], 0xFF0E2A28, 0.55F)};
				if (on) V.rr(g, x - 9, top - 9, cw + 18, ch + 18, 38, 0x55FFF4D8);
				V.rr(g, x, top + 9, cw, ch, 30, 0x45000000);
				V.rr(g, x - 3, top - 3, cw + 6, ch + 6, 33, on ? 0xFFFFFFFF : 0x50FFFFFF);
				V.rrStops(g, x, top, cw, ch, 30, new float[] {0, 0.5F, 1}, cols);
				// A soft light behind it, and a round pad in the card's deep colour for it to stand on.
				float feet = top + 40 + tall;
				V.ellipse(g, cx, feet - tall * 0.5F, cw * 0.4F, tall * 0.56F, on ? 0x2EFFFFFF : 0x12FFFFFF);
				V.ellipse(g, cx, feet, cw * 0.36F, 16, Ui.alpha(Ui.darker(c[2], 0.7F), on ? 0.9F : 0.6F));
				V.ellipse(g, cx, feet - 2, cw * 0.27F, 9, on ? 0x40FFFFFF : 0x1CFFFFFF);
				Figures.Fig f = b.mon(IDS[i], V.x(cx), V.y(feet), V.s(tall)).at(secs + i * 0.7F).pitch(10).facing(i == 0 ? 14 : i == 2 ? -14 : 0);
				if (on && step == Step.CHOOSE) {
					f.action("cry", since / 20F);
					// Hops twice, twirling on the first, and lands with a squash.
					if (since < 22) {
						float p = since / 11F;
						float arc = (float) Math.abs(Math.sin(p * Math.PI));
						f.hop(V.s(arc * 30)).squash(arc < 0.25F ? 0.88F + arc * 0.48F : 1.04F);
						if (since < 11) f.spin(since / 11F * 360F);
					} else f.hop(V.s((float) Math.abs(Math.sin((since - 22) / 20F * Math.PI)) * 5));
				}
				String nm = Species.get(IDS[i]).name();
				V.centred(g, nm, cx, top + ch - 86 + 2, 32, 0x66000000, true);
				V.centred(g, nm, cx, top + ch - 86, 32, on ? 0xFFFFFFFF : 0xFFE4ECE8, true);
				float pw = V.width(KINDS[i], 17, true) + 34;
				V.pill(g, cx - pw / 2, top + ch - 46, pw, 30, on ? 0xFFFFFFFF : 0x30FFFFFF);
				V.centred(g, KINDS[i], cx, V.capsTop(top + ch - 31, 17), 17, on ? Ui.darker(c[2], 0.85F) | 0xFF000000 : 0xFFFFFFFF, true);
				if (step == Step.CHOOSE) vhit(x, top, cw, ch, () -> choose(idx));
			}
			b.draw(g);
			float hx = x0 + at * (cw + gap) + cw / 2 + tall / 3, hy = y - 12 + 34;
			Ui.bubble(g, "heart", Math.round(V.x(hx)), Math.round(V.y(hy)), s, step == Step.CHOOSE ? Math.min(1, Math.max(0, since - 4) / 6F) : 1);
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
		box.renderV(g, font, 140, 26, 1000, ticks, partial, step == Step.STOP || step == Step.WELCOME || step == Step.AFTER);
		if (toLab >= 0) {
			float t = (toLab + partial) / LAB_FADE;
			float a = Math.min(1, (t < 0.5F ? t : 1 - t) * 2.6F);
			g.fill(0, 0, width, height, Ui.alpha(0xFF05070F, a));
		}
		if (step == Step.CONFIRM) vchoice(g, yesNo, V.W - 140 - 300, V.H - 26 - 150 - V.choiceHeight(2), 300, i -> confirm(i == 0));
	}
}
