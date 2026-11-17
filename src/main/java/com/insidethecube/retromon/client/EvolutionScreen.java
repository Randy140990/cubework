package com.insidethecube.retromon.client;

import java.util.List;

import com.insidethecube.retromon.engine.Species;
import com.insidethecube.retromon.game.MonDisplay;
import com.insidethecube.retromon.game.Net;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.nbt.CompoundTag;

/**
 * "What? BULBASAUR is evolving!" The Pokémon stands before you, flickering between its two forms faster and
 * faster, then becomes the new one. Press B while it flickers to stop it, as in the games.
 */
public class EvolutionScreen extends GameScreen {
	private static final int LENGTH = 120;
	private final CompoundTag tag;
	private final String from, into, name;
	private final TextBox box;
	private int t = -20;
	private boolean done, cancelled, sent;

	public EvolutionScreen(CompoundTag tag) {
		super("Evolution");
		this.tag = tag;
		this.from = tag.getStringOr("from", "");
		this.into = tag.getStringOr("into", "");
		this.name = tag.getStringOr("name", Species.get(from).name());
		box = new TextBox(List.of("What? " + name + " is evolving!"));
		BattleScene.showcase(from);
		Music.jingle("level_up");
	}

	@Override
	public void tick() {
		super.tick();
		box.tick();
		MonDisplay d = BattleScene.display(1);
		if (done || cancelled) return;
		t++;
		if (t < 0 || d == null) return;
		// Flicker: slow at first, faster towards the end.
		float p = t / (float) LENGTH;
		int period = Math.max(2, (int) (16 - 14 * p));
		d.species = (t / period) % 2 == 0 ? from : into;
		d.flash = (t % period == 0) ? 2 : 0;
		if (t >= LENGTH) {
			done = true;
			d.species = into;
			d.size = 0.6F;
			d.sizeTarget = 1;
			Sounds.cry(into, 1F);
			Music.jingle("caught_mon");
			box.set(List.of("Congratulations! Your " + name + " evolved into " + Species.get(into).name() + "!"));
			send(true);
		}
	}

	private void send(boolean ok) {
		if (sent) return;
		sent = true;
		CompoundTag t = tag.copy();
		t.putBoolean("ok", ok);
		ClientPlayNetworking.send(new Net.Act("evolve", t));
	}

	private void stop() {
		if (done || cancelled || t < 0) return;
		cancelled = true;
		MonDisplay d = BattleScene.display(1);
		if (d != null) d.species = from;
		box.set(List.of("Huh? " + name + " stopped evolving!"));
		send(false);
	}

	private void close() {
		BattleScene.end();
		if (minecraft != null && minecraft.screen == this) minecraft.setScreen(null);
	}

	@Override
	protected boolean key(KeyEvent e) {
		if (!done && !cancelled) {
			if (Ui.isB(e)) stop();
			return true;
		}
		if ((Ui.isA(e) || Ui.isB(e)) && box.press()) close();
		return true;
	}

	@Override
	protected void clickAnywhere() {
		if ((done || cancelled) && box.press()) close();
	}

	@Override
	protected void back() {
		if (!done && !cancelled) stop();
		else clickAnywhere();
	}

	@Override
	public void onClose() {
	}

	@Override
	protected void draw(GuiGraphics g, float partial) {
		int h = boxHeight(), m = 6 * s;
		box.render(g, font, m, height - h - m, width - 2 * m, h, s, ticks, done || cancelled);
		if (!done && !cancelled && t >= 0) Ui.centred(g, font, "B: stop it evolving", width / 2, m, Ui.DIM, s, true);
	}
}
