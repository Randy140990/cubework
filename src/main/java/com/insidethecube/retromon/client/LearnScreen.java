package com.insidethecube.retromon.client;

import java.util.ArrayList;
import java.util.List;

import com.insidethecube.retromon.engine.Mon;
import com.insidethecube.retromon.engine.Move;
import com.insidethecube.retromon.game.Net;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.nbt.CompoundTag;

/**
 * "BULBASAUR is trying to learn RAZOR LEAF. But it can't learn more than four moves! Delete an older move?"
 * Pick the one to forget, or give up on the new one.
 */
public class LearnScreen extends GameScreen {
	private enum Step {
		ASK, PICK, GIVE_UP, DONE
	}

	private final int index;
	private final String move;
	private Step step = Step.ASK;
	private final TextBox box;
	private Choice yesNo;
	private int at;
	private boolean sent;

	public LearnScreen(CompoundTag t) {
		super("Learn");
		this.index = t.getIntOr("i", 0);
		this.move = t.getStringOr("move", "");
		String name = mon() == null ? "It" : mon().name(), mv = Move.find(move) == null ? move : Move.get(move).name();
		// The UI Viewer's words.
		box = new TextBox(List.of(name + " wants to learn the move " + mv + ".", "But " + name + " already knows four moves. Forget one to make room for " + mv + "?"));
	}

	private Mon mon() {
		List<Mon> p = ClientState.data.party;
		return index >= 0 && index < p.size() ? p.get(index) : null;
	}

	@Override
	public void tick() {
		super.tick();
		box.tick();
		if (step == Step.ASK && yesNo == null && box.atEnd()) yesNo = new Choice(List.of("YES", "NO"));
		if (step == Step.GIVE_UP && yesNo == null && box.atEnd()) yesNo = new Choice(List.of("YES", "NO"));
	}

	private void answer(boolean yes) {
		yesNo = null;
		Mon m = mon();
		String mv = Move.find(move) == null ? move : Move.get(move).name();
		if (step == Step.ASK) {
			if (yes) {
				step = Step.PICK;
				box.set(List.of("Which move should be forgotten?"));
			} else finish(-1, List.of((m == null ? "It" : m.name()) + " didn't learn " + mv + "."));
		} else if (step == Step.GIVE_UP) {
			if (yes) finish(-1, List.of((m == null ? "It" : m.name()) + " didn't learn " + mv + "."));
			else {
				step = Step.ASK;
				box.set(List.of("Delete an older move to make room for " + mv + "?"));
			}
		}
	}

	private void forget(int slot) {
		Mon m = mon();
		if (m == null || slot < 0 || slot >= m.moves.size()) return;
		String old = Move.get(m.moves.get(slot)).name(), mv = Move.get(move).name();
		m.replace(slot, move);
		finish(slot, List.of("1, 2 and... Poof!", m.name() + " forgot " + old + "...", "...and learned " + mv + "!"));
		Music.jingle("level_up");
	}

	/** A row of the list: one of its moves to forget, or (the last) the new one, so it doesn't learn it. */
	private void pick(int row) {
		Mon m = mon();
		Sounds.select();
		if (m != null && row < m.moves.size()) forget(row);
		else finish(-1, List.of((m == null ? "It" : m.name()) + " didn't learn " + (Move.find(move) == null ? move : Move.get(move).name()) + "."));
	}

	private void finish(int slot, List<String> lines) {
		if (!sent) {
			sent = true;
			CompoundTag t = new CompoundTag();
			t.putInt("i", index);
			t.putString("move", move);
			t.putInt("slot", slot);
			ClientPlayNetworking.send(new Net.Act("learn", t));
		}
		step = Step.DONE;
		box.set(new ArrayList<>(lines));
	}

	@Override
	protected boolean key(KeyEvent e) {
		switch (step) {
			case ASK, GIVE_UP -> {
				if (yesNo != null) {
					if (yesNo.key(e)) return true;
					if (Ui.isA(e)) answer(yesNo.at == 0);
					else if (Ui.isB(e)) answer(false);
				} else if (Ui.isA(e) || Ui.isB(e)) box.press();
			}
			case PICK -> {
				Mon m = mon();
				int n = (m == null ? 0 : m.moves.size()) + 1;
				if (Ui.isUp(e)) at = Math.floorMod(at - 1, n);
				else if (Ui.isDown(e)) at = Math.floorMod(at + 1, n);
				else if (Ui.isA(e)) pick(at);
				else if (Ui.isB(e)) pick(n - 1);
			}
			case DONE -> {
				if ((Ui.isA(e) || Ui.isB(e)) && box.press() && minecraft != null) minecraft.setScreen(null);
			}
		}
		return true;
	}

	@Override
	protected void clickAnywhere() {
		if (step == Step.DONE) {
			if (box.press() && minecraft != null) minecraft.setScreen(null);
		} else if (yesNo == null && step != Step.PICK) box.press();
	}

	@Override
	public void onClose() {
	}

	@Override
	protected void draw(GuiGraphics g, float partial) {
		// The UI Viewer's text box and YES/NO; the moves to forget in its list at the right, each with its type.
		box.renderV(g, font, 140, 26, 1000, ticks, partial, yesNo == null && step != Step.PICK);
		if (yesNo != null) vchoice(g, yesNo, V.W - 140 - 300, V.H - 26 - 150 - V.choiceHeight(yesNo.options.size()), 300, i -> answer(i == 0));
		if (step == Step.PICK) {
			Mon mon = mon();
			if (mon == null) return;
			int n = mon.moves.size() + 1;
			float x = 760, y = 120, w = 480, pad = 12, rh = 46, gap = 8;
			V.panel(g, x, y, w, V.choiceHeight(n));
			for (int i = 0; i < n; i++) {
				String id = i < mon.moves.size() ? mon.moves.get(i) : move;
				Move mv = Move.get(id);
				float rx = x + pad, ry = y + pad + i * (rh + gap), rw = w - pad * 2;
				int row = i;
				if (vover(rx, ry, rw, rh)) at = i;
				V.item(g, mv.name(), null, rx, ry, rw, rh, i == at, false, 21);
				String t = mv.type().label.toUpperCase();
				float cw = Math.max(70, V.width(t, 12, true) + 12 * 1.8F);
				V.type(g, t, Ui.typeColor(mv.type()), rx + rw - 18 - cw, ry + (rh - 12 * 1.75F) / 2, 70, 12);
				vhit(rx, ry, rw, rh, () -> pick(row));
			}
		}
	}
}
