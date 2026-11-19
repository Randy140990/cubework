package com.insidethecube.retromon.client;

import java.util.List;

import com.insidethecube.retromon.engine.Species;
import com.insidethecube.retromon.game.Net;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.nbt.CompoundTag;

/**
 * Evolving, as the UI Viewer's "Evolving" page: "What? BULBASAUR is evolving!" on the bright backdrop with it on
 * its stand. Then it stops turning and glows white, pulsing ("Press B to stop it"), it's gone in the light, and
 * it comes back as its new form. "Congratulations!" pops up and the text box says what it became. B while it
 * glows stops it, as the old games let you.
 */
public class EvolutionScreen extends GameScreen {
	private enum Phase {
		INTRO, GLOW, GONE, NEW, DONE, STOPPED
	}

	/** How long each part lasts (ticks): the glow, gone in the light, back as the new one still glowing. */
	private static final int GLOW = 24, GONE = 18, NEW = 8;
	private final CompoundTag tag;
	private final String from, into, name;
	private final TextBox box;
	private Phase phase = Phase.INTRO;
	private int at;
	/** How far round it's turned (seconds of turning): held while it glows. */
	private float turn;
	private int calloutAt = -1;
	private boolean sent;

	public EvolutionScreen(CompoundTag tag) {
		super("Evolution");
		this.tag = tag;
		this.from = tag.getStringOr("from", "");
		this.into = tag.getStringOr("into", "");
		Species f = Species.get(from);
		this.name = tag.getStringOr("name", f == null ? from.toUpperCase() : f.name());
		box = new TextBox(List.of("What? " + name + " is evolving!"));
		Music.jingle("level_up");
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
			} else if (phase == Phase.GONE && at >= GONE) {
				phase = Phase.NEW;
				at = 0;
				Sounds.cry(into, 1F);
			} else if (phase == Phase.NEW && at >= NEW) {
				phase = Phase.DONE;
				calloutAt = ticks;
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
	}

	private void stop() {
		if (phase != Phase.GLOW && phase != Phase.GONE) return;
		phase = Phase.STOPPED;
		Sounds.press();
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
		float secs = (ticks + partial) / 20F;
		Scenes.backdrop(g, "evolve");
		boolean newForm = phase == Phase.NEW || phase == Phase.DONE;
		float glow = switch (phase) {
			case GLOW -> Math.min(1, (at + partial) / 6F);
			case GONE, NEW -> 1;
			case DONE -> Math.max(0, 1 - (ticks - calloutAt + partial) / 6F);
			default -> 0;
		};
		Scenes.model(g, secs, newForm ? into : from, 0, turn + (phase == Phase.INTRO || phase == Phase.DONE || phase == Phase.STOPPED ? partial * 0.05F : 0), glow,
				phase == Phase.GONE);
		if (phase == Phase.GLOW || phase == Phase.GONE) {
			// "Press B to stop it", in a pill over the middle.
			float pop = Math.min(1, (phase == Phase.GLOW ? at + partial : GLOW) / 8F);
			float k = pop < 1 ? 0.85F + 0.15F * pop : 1;
			float pw = 400 * k, ph = 52 * k, px = 640 - pw / 2, py = 300 + 26 - ph / 2;
			V.panel(g, px, py, pw, ph, ph / 2, false);
			String a = "Press ", b = " to stop it", key = V.keyName("B");
			float kw = Math.max(28, V.width(key, 14, true) + 14), tw = V.width(a, 20, true) + 6 + kw + 6 + V.width(b, 20, true), tx = 640 - tw / 2, ty = 300 + 26 - 13;
			V.text(g, a, tx, ty, 20, V.TEXT, true);
			tx += V.width(a, 20, true) + 6;
			V.key(g, key, tx, ty - 2, 28);
			tx += kw + 6;
			V.text(g, b, tx, ty, 20, V.TEXT, true);
		}
		if (calloutAt >= 0) V.callout(g, "Congratulations!", 640, 120, (ticks - calloutAt + partial) / 20F);
		if (phase == Phase.INTRO || phase == Phase.DONE || phase == Phase.STOPPED)
			box.renderV(g, font, 140, 26, 1000, ticks, partial, true);
	}
}
