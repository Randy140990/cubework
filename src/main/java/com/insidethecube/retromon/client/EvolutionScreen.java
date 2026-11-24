package com.insidethecube.retromon.client;

import java.util.List;

import com.insidethecube.retromon.engine.Species;
import com.insidethecube.retromon.game.Net;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.nbt.CompoundTag;

/**
 * Evolving, as the UI Viewer's "Evolving" page: "What? BULBASAUR is evolving!" on the bright backdrop with it on its
 * stand. Then it turns to white light; it and its new form swap back and forth, each growing as the other shrinks,
 * slowly at first and then quicker and quicker, until the new one stays and its colours come back through the white,
 * a burst of light as it settles. "Congratulations!" pops up and the text box says what it became. B while it's going
 * stops it, as the old games let you.
 */
public class EvolutionScreen extends GameScreen {
	private enum Phase {
		INTRO, GLOW, GONE, NEW, DONE, STOPPED
	}

	/**
	 * How long each part lasts (seconds): whitening (0.9), the fourteen swaps (from 0.52 s down to a 0.07 s flicker),
	 * the new one's colours coming back (0.9); stopped, the old one's come back (0.6).
	 */
	private static final float WHITE = 0.9F, BACK = 0.9F, UNDO = 0.6F;
	private static final float[] SWAPS = new float[14];
	private static final float SWAP_TOTAL;
	static {
		float t = 0;
		for (int i = 0; i < 14; i++) t += SWAPS[i] = Math.max(0.07F, 0.52F * (float) Math.pow(0.8, i));
		SWAP_TOTAL = t;
	}
	private static final int GLOW = Math.round(WHITE * 20), GONE = Math.round(SWAP_TOTAL * 20), NEW = Math.round(BACK * 20);
	/** When this phase began (ticks), and when it stopped (for the old one's colours coming back). */
	private int phaseAt, stoppedAt = -1;
	private final CompoundTag tag;
	private final String from, into, name;
	private final TextBox box;
	private Phase phase = Phase.INTRO;
	private int at;
	/** How far round it's turned (seconds of turning): held while it glows. */
	private float turn;
	private int calloutAt = -1;
	private float whiteWhenStopped = 1;
	private boolean sent;

	public EvolutionScreen(CompoundTag tag) {
		super("Evolution");
		this.tag = tag;
		this.from = tag.getStringOr("from", "");
		this.into = tag.getStringOr("into", "");
		Species f = Species.get(from);
		this.name = tag.getStringOr("name", f == null ? from.toUpperCase() : f.name());
		box = new TextBox(List.of("What? " + name + " is evolving!"));
		// (0.59.6) In Johto, Crystal's evolution song plays while it evolves; in Kanto, the jingle as before.
		if (Music.johto()) Music.play("evolution");
		else Music.jingle("level_up");
	}

	private String newName() {
		Species s = Species.get(into);
		return s == null ? into.toUpperCase() : s.name();
	}

	@Override
	public void tick() {
		super.tick();
		box.tick();
		boolean turning = phase == Phase.INTRO || phase == Phase.DONE || phase == Phase.STOPPED;
		if (turning) turn += 0.05F;
		if (phase == Phase.GLOW || phase == Phase.GONE || phase == Phase.NEW) {
			at++;
			if (phase == Phase.GLOW && at >= GLOW) {
				phase = Phase.GONE;
				at = 0;
				phaseAt = ticks;
			} else if (phase == Phase.GONE && at >= GONE) {
				phase = Phase.NEW;
				at = 0;
				phaseAt = ticks;
				Sounds.cry(into, 1F);
			} else if (phase == Phase.NEW && at >= NEW) {
				phase = Phase.DONE;
				calloutAt = ticks;
				if (Music.johto()) Music.resumePlace();
				Music.jingle("caught_mon");
				box.set(List.of(name + " evolved into " + newName() + "!"));
				send(true);
			}
		}
	}

	private void send(boolean ok) {
		if (sent) return;
		sent = true;
		CompoundTag t = tag.copy();
		t.putBoolean("ok", ok);
		ClientPlayNetworking.send(new Net.Act("evolve", t));
	}

	/** A press of A on the first line starts it glowing. */
	private void begin() {
		phase = Phase.GLOW;
		at = 0;
		phaseAt = ticks;
	}

	private void stop() {
		if (phase != Phase.GLOW && phase != Phase.GONE) return;
		whiteWhenStopped = phase == Phase.GLOW ? Math.min(1, at / (float) GLOW) : 1;
		phase = Phase.STOPPED;
		stoppedAt = ticks;
		Sounds.press();
		if (Music.johto()) Music.resumePlace();
		box.set(List.of("Huh? " + name + " stopped evolving!"));
		send(false);
	}

	private void close() {
		if (BattleScene.active()) BattleScene.end();
		if (minecraft != null && minecraft.screen == this) minecraft.setScreen(null);
	}

	private void press() {
		switch (phase) {
			case INTRO -> {
				if (box.press()) begin();
			}
			case DONE, STOPPED -> {
				if (box.press()) close();
			}
			default -> {
			}
		}
	}

	@Override
	protected boolean key(KeyEvent e) {
		if (phase == Phase.GLOW || phase == Phase.GONE) {
			if (Ui.isB(e)) stop();
			return true;
		}
		if (Ui.isA(e) || Ui.isB(e)) press();
		return true;
	}

	@Override
	protected void clickAnywhere() {
		press();
	}

	@Override
	protected void back() {
		if (phase == Phase.GLOW || phase == Phase.GONE) stop();
		else press();
	}

	@Override
	public void onClose() {
	}

	@Override
	protected void draw(GuiGraphics g, float partial) {
		float secs = (ticks + partial) / 20F, pt = (ticks - phaseAt + partial) / 20F;
		Scenes.bg2(g, "evolve");
		float turnNow = turn + (phase == Phase.INTRO || phase == Phase.DONE || phase == Phase.STOPPED ? partial * 0.05F : 0);
		switch (phase) {
			case INTRO -> Scenes.morph(g, secs, from, null, 1, 0, 0, 0, turnNow);
			case GLOW -> {
				float w = Math.min(1, pt / WHITE);
				Scenes.morph(g, secs, from, null, 1, 0, w, w, turnNow);
			}
			case GONE -> {
				// The swaps: each growing as the other shrinks, quicker and quicker.
				float t = pt, ka = 1, kb = 0;
				for (int i = 0; i < SWAPS.length; i++) {
					if (t <= SWAPS[i]) {
						float k = Math.max(0, t / SWAPS[i]), e = k * k * (3 - 2 * k);
						boolean toB = i % 2 == 0;
						ka = toB ? 1 - e : e;
						kb = toB ? e : 1 - e;
						break;
					}
					t -= SWAPS[i];
					ka = i % 2 == 0 ? 0 : 1;
					kb = 1 - ka;
				}
				Scenes.morph(g, secs, from, into, ka, kb, 1, 1, turnNow);
			}
			case NEW -> Scenes.morph(g, secs, into, null, 1, 0, 1 - Math.min(1, pt / BACK), 1 - Math.min(1, pt / BACK), turnNow);
			case DONE -> Scenes.morph(g, secs, into, null, 1, 0, 0, 0, turnNow);
			case STOPPED -> {
				float k = stoppedAt < 0 ? 1 : Math.min(1, (ticks - stoppedAt + partial) / 20F / UNDO), w = whiteWhenStopped * (1 - k);
				Scenes.morph(g, secs, from, null, 1, 0, w, w, turnNow);
			}
		}
		if (phase == Phase.GLOW || phase == Phase.GONE) {
			// "Press B to stop it", in a pill over the middle (the viewer's tip at 440, 300).
			float pop = Math.min(1, (ticks - phaseAt + partial + (phase == Phase.GONE ? GLOW : 0)) / 8F);
			float k = pop < 1 ? 0.85F + 0.15F * pop : 1;
			String a = "Press ", b = " to stop it", key = V.keyName("B");
			float kw = V.keyWidth(key, 30), tw = V.width(a, 20, true) + kw + V.width(b, 20, true);
			float pw = Math.max(400, tw + 36) * k, ph = (20 * 1.36F + 24) * k, px = 640 - pw / 2, py = 300;
			V.panel(g, px, py, pw, ph, ph / 2, false);
			float tx = 640 - tw / 2, mid = py + ph / 2;
			V.text(g, a, tx, V.capsTop(mid, 20), 20, V.INK, true);
			tx += V.width(a, 20, true);
			V.key(g, key, tx, mid - 15, 30);
			tx += kw;
			V.text(g, b, tx, V.capsTop(mid, 20), 20, V.INK, true);
		}
		// A burst of light as it settles.
		if (calloutAt >= 0) {
			float bt = (ticks - calloutAt + partial) / 20F;
			if (bt < 0.85F) {
				float a = 1 - Math.min(1, bt / 0.8F);
				V.radialSoft(g, 640, 360, 900, new float[] {0, 0.25F / 0.6F, 1}, new int[] {Ui.alpha(0xFFFFFFFF, a), Ui.alpha(0xFFFFFFFF, 0.7F * a), 0x00FFFFFF});
			}
			V.callout(g, "Congratulations!", 640, 120, bt);
		}
		if (phase == Phase.INTRO || phase == Phase.DONE || phase == Phase.STOPPED) box.renderV(g, font, 140, 26, 1000, ticks, partial, true);
	}
}
