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
	/** A line about each of ours (the viewer's STARTERS). */
	private static final String[] LINES = {"A leafy armadillo. It curls up in its bark-hard bands, and the sprout on its head drinks in the sun.",
			"A little black goat with a flame between its horns. The happier it is, the brighter the flame.",
			"A sea tapir. It sprays water from its soft snout, and the curl of wave on its head never dries."};
	/** Looking in a ball: when it began (ticks; -100 for none yet), and which was showing before. */
	private int revealAt = -100, shownBefore = 1;

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
		box.set(List.of("So! You want the " + KINDS[i] + " POKéMON, **" + Species.get(IDS[i]).name() + "**?"));
	}

	private void confirm(boolean yes) {
		if (!yes) {
			step = Step.CHOOSE;
			box.set(List.of("Take your time. Which one will you choose?"));
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
		box.set(List.of("**{N}** received **" + mon + "**!", "Here are some POTIONS to start you off.",
				"If a wild POKéMON appears, your POKéMON can fight against it!",
				"ROUTE 1 is just north of here. Afterward, go on to the next town, VIRIDIAN CITY.", "Press ENTER for your menu. Good luck, " + name + "!"));
	}

	/** Looks in another ball: the light builds up out of it, then its creature's there. */
	private void look(int i) {
		if (i == at) return;
		shownBefore = shown();
		at = i;
		revealAt = ticks;
		Sounds.select();
	}

	/** The one standing big in the middle (the old one until the light's at its brightest). */
	private int shown() {
		return ticks - revealAt < 20 ? shownBefore : at;
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
			box.set(List.of("Three balls, three POKéMON. Look in each one. Which will you choose?"));
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
				if (Ui.isLeft(e) || Ui.isRight(e)) look(Math.floorMod(at + (Ui.isLeft(e) ? -1 : 1), 3));
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
		boolean choosing0 = step == Step.CHOOSE || step == Step.CONFIRM;
		if (step == Step.STOP) g.fillGradient(0, height / 2, width, height, 0x000A0E24, 0x900A0E24);
		else if (!choosing0) g.fillGradient(0, 0, width, height, 0x700A0E24, 0xC00A0E24);
		Figures.Batch b = Figures.batch(0, 0, width, height);
		boolean choosing = choosing0;
		if (choosing) {
			drawChoose(g, partial, secs);
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
		if (choosing) box.renderFilm(g, font, ticks, partial, false);
		else box.renderV(g, font, 140, 26, 1000, ticks, partial, step == Step.STOP || step == Step.WELCOME || step == Step.AFTER);
		if (toLab >= 0) {
			float t = (toLab + partial) / LAB_FADE;
			float a = Math.min(1, (t < 0.5F ? t : 1 - t) * 2.6F);
			g.fill(0, 0, width, height, Ui.alpha(0xFF05070F, a));
		}
		if (step == Step.CONFIRM) vchoice(g, yesNo, V.W - 140 - 300, V.H - 26 - 150 - V.choiceHeight(2), 300, i -> confirm(i == 0));
	}

	/**
	 * Choosing your first, as the UI Viewer's: three balls along the foot; the one you're on rolls forward and pops
	 * open; looking in another, a white light builds up out of it and fills the screen, then fades to show its
	 * creature standing big in the middle on its own backdrop, its plate beside it (what kind, name, type, a line).
	 */
	private void drawChoose(GuiGraphics g, float partial, float secs) {
		float rt = (ticks - revealAt + partial) / 20F;
		boolean revealing = rt < 1.42F + 0.3F;
		int sid = shown();
		Species sp = Species.get(IDS[sid]);
		// Its backdrop and it, big in the middle (hidden while the light builds).
		Scenes.typeBg(g, sp.type1(), true);
		if (!(rt >= 0 && rt < 1)) {
			if (rt >= 1 && rt < 1.42F) V.glow(g, 640, 330, 240, 280, Ui.alpha(0xFFFFFFFF, 0.5F));
			Scenes.modelBare(g, secs, IDS[sid], 0, false, 84);
		}
		// The plate.
		float plateA = !revealing ? 1 : rt < 1.42F ? 0 : Math.min(1, (rt - 1.42F) / 0.3F);
		if (plateA > 0.01F) plate(g, sid, plateA);
		// The three balls along the foot, each with its type under it.
		float[] colW = new float[3];
		float total = 300;
		for (int i = 0; i < 3; i++) total += colW[i] = Math.max(96, V.trackedWidth(KINDS[i], 14, 1) + 28);
		float x = 640 - total / 2;
		float onT = (ticks - Math.max(revealAt, -100) + partial) / 20F;
		for (int i = 0; i < 3; i++) {
			boolean on = i == at;
			float cx = x + colW[i] / 2, lift = 0, sc = 1, wob = 0;
			if (on) {
				float p = Math.min(1, (revealAt < 0 ? 1 : onT) / 0.45F);
				if (p < 0.4F) {
					float q = V.bezier(0, 0, 0.58F, 1, p / 0.4F);
					lift = -22 * q;
					sc = 1 + 0.08F * q;
				} else {
					float q = V.bezier(0, 0, 0.58F, 1, (p - 0.4F) / 0.6F);
					lift = -22 + 8 * q;
					sc = 1.08F - 0.04F * q;
				}
			} else {
				float ph = (((secs - i * 0.4F) % 2.4F) + 2.4F) % 2.4F / 2.4F;
				wob = -8 * (float) Math.sin(ph * Math.PI * 2);
			}
			int tc = Ui.typeColor(Species.get(IDS[i]).type1()) | 0xFF000000;
			ball(g, cx, 486 + 48 + lift, 96 * sc, on, tc, wob);
			String k = KINDS[i];
			float pw = V.trackedWidth(k, 14, 1) + 28, ph2 = 14 * 1.36F + 4, py = 486 + 96 + 6 + lift;
			V.pill(g, cx - pw / 2, py, pw, ph2, on ? 0xFFFFFFFF : 0x66FFFFFF);
			V.tracked(g, k, cx - pw / 2 + 14, V.capsTop(py + ph2 / 2, 14), 14, on ? tc : 0xFFFFFFFF, 1);
			int idx = i;
			if (step == Step.CHOOSE) {
				if (vover(cx - colW[i] / 2, 486, colW[i], 120) && at != i) look(i);
				vhit(cx - colW[i] / 2, 486, colW[i], 120, () -> choose(idx));
			}
			x += colW[i] + 150;
		}
		// The light: building up out of the ball for about a second, then fading off it.
		if (rt >= 0 && rt < 1.5F) {
			float fx = new float[] {0.34F, 0.5F, 0.66F}[at] * 1280, fy = 0.74F * 720;
			float k, scale, white;
			if (rt < 1) {
				k = V.bezier(0.42F, 0, 1, 1, Math.min(1, rt / 0.9F));
				scale = 0.3F + 2.7F * V.bezier(0.6F, 0, 0.9F, 0.6F, Math.min(1, rt / 0.95F));
				white = 0.92F * V.bezier(0.42F, 0, 1, 1, Math.max(0, Math.min(1, (rt - 0.45F) / 0.55F)));
			} else {
				float out = 1 - V.bezier(0, 0, 0.58F, 1, Math.min(1, (rt - 1) / 0.5F));
				k = out;
				scale = 3;
				white = 0.92F * out;
			}
			V.radialSoft(g, fx, fy, 333 * scale, new float[] {0, 0.45F, 1}, new int[] {Ui.alpha(0xFFFFFFFF, k), Ui.alpha(0xFFFFFFFF, 0.95F * k), 0x00FFFFFF});
			if (white > 0) g.fill(0, 0, width, height, Ui.alpha(0xFFFFFFFF, white));
		}
		if (step == Step.CHOOSE) V.hints(g, "A", "Choose");
	}

	/** The plate (the viewer's): THE FIRE POKéMON, its name, its types, a line about it, on clear white. */
	private void plate(GuiGraphics g, int i, float a) {
		Species sp = Species.get(IDS[i]);
		int tc = Ui.typeColor(sp.type1()) | 0xFF000000;
		String line = Look.ours() && Look.has(IDS[i]) ? LINES[i] : PokedexScreen.about(sp);
		float x = 860, y = 150, w = 380, px = x + 24, cw = w - 48;
		List<String> ll = V.wrap(line, cw, 18, true);
		float chipH = 13.5F * 1.36F + 6;
		float h = 18 + 14 * 1.36F + 36 * 1.1F + 8 + chipH + 10 + ll.size() * 18 * 1.45F + 18;
		V.shadow(g, x, y, w, h, 26, 1.3F * a);
		V.rr(g, x, y, w, h, 26, Ui.alpha(0xFFFFFFFF, a));
		V.rr(g, x + 4, y + 4, w - 8, h - 8, 22, Ui.alpha(0xE6FFFFFF, a));
		float cy = y + 18;
		V.tracked(g, "THE " + KINDS[i] + " POKéMON", px, cy + 2, 14, Ui.alpha(Ui.mix(tc, 0xFF000000, 0.2F), a), 1.2F);
		cy += 14 * 1.36F;
		V.title(g, sp.name(), px, cy, 36, Ui.alpha(V.INK, a));
		cy += 36 * 1.1F + 8;
		float tx = px;
		tx += PokedexScreen.typeChip(g, sp.type1(), tx, cy) + 8;
		if (sp.type2() != null && sp.type2() != sp.type1()) PokedexScreen.typeChip(g, sp.type2(), tx, cy);
		cy += chipH + 10;
		for (String l : ll) {
			V.text(g, l, px, cy, 18, Ui.alpha(V.INK, a), true);
			cy += 18 * 1.45F;
		}
	}

	/** A ball (the viewer's svg, 100 across): its shadow, white below, red above (lifted open when on), the band and button. */
	private static void ball(GuiGraphics g, float cx, float cy, float size, boolean on, int tc, float wobDeg) {
		float u = size / 100;
		double wr = Math.toRadians(wobDeg);
		float cw = (float) Math.cos(wr), sw = (float) Math.sin(wr);
		// Ball space (0 to 100, the middle at 50, 50) to the canvas, turned by the wobble.
		java.util.function.BiFunction<Float, Float, float[]> P = (bx, by) -> {
			float dx = (bx - 50) * u, dy = (by - 50) * u;
			return new float[] {cx + dx * cw - dy * sw, cy + dx * sw + dy * cw};
		};
		float[] sh = P.apply(50F, 96F);
		V.ellipse(g, sh[0], sh[1], 34 * u, 7 * u, 0x38000000);
		// Below: white with its dark edge.
		half(g, P, 50, 50, 44.5F, false, 0, 0, 0, V.INK);
		half(g, P, 50, 50, 39.5F, false, 0, 0, 0, 0xFFF4F6F5);
		// The band.
		float[] b0 = P.apply(8F, 46F), b1 = P.apply(92F, 46F), b2 = P.apply(92F, 54F), b3 = P.apply(8F, 54F);
		V.poly(g, new float[] {b0[0], b0[1], b1[0], b1[1], b2[0], b2[1], b3[0], b3[1]}, V.INK);
		// Above: red, lifted open on the one you're on (moved up and turned back about its left end).
		float tx = on ? -6 : 0, ty = on ? -14 : 0, rot = on ? -42 : 0;
		half(g, P, 50, 50, 44.5F, true, tx, ty, rot, V.INK);
		half(g, P, 50, 50, 39.5F, true, tx, ty, rot, 0xFFE5525C);
		// The button.
		float[] c = P.apply(50F, 50F);
		V.ellipse(g, c[0], c[1], 15.5F * u, 15.5F * u, V.INK);
		V.ellipse(g, c[0], c[1], 10.5F * u, 10.5F * u, 0xFFF4F6F5);
		V.ellipse(g, c[0], c[1], 7 * u, 7 * u, V.INK);
		V.ellipse(g, c[0], c[1], 5 * u, 5 * u, on ? tc : 0xFFF4F6F5);
		if (on) {
			float[] gl = P.apply(50F, 40F);
			V.glow(g, gl[0], gl[1], 34 * u, 16 * u, 0xD9FFFFFF);
		}
	}

	/** Half a disc (top or bottom) of radius r about (ox, oy) in ball space; the top one moved by (tx, ty) and turned rot degrees about (8, 50). */
	private static void half(GuiGraphics g, java.util.function.BiFunction<Float, Float, float[]> P, float ox, float oy, float r, boolean top, float tx, float ty, float rot,
			int col) {
		int n = 20;
		float[] pts = new float[(n + 1) * 2];
		double rr = Math.toRadians(rot);
		for (int i = 0; i <= n; i++) {
			double a = top ? Math.PI + Math.PI * i / n : Math.PI * i / n;
			float bx = ox + (float) Math.cos(a) * r, by = oy + (float) Math.sin(a) * r;
			if (top && rot != 0) {
				float dx = bx - 8, dy = by - 50;
				bx = 8 + dx * (float) Math.cos(rr) - dy * (float) Math.sin(rr);
				by = 50 + dx * (float) Math.sin(rr) + dy * (float) Math.cos(rr);
			}
			float[] q = P.apply(bx + tx, by + ty);
			pts[i * 2] = q[0];
			pts[i * 2 + 1] = q[1];
		}
		V.poly(g, pts, col);
	}
}
