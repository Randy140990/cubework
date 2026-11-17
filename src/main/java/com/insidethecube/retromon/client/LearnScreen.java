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
		box = new TextBox(List.of(name + " is trying to learn " + mv + ".", "But " + name + " can't learn more than four moves!",
				"Delete an older move to make room for " + mv + "?"));
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
			} else {
				step = Step.GIVE_UP;
				box.set(List.of("Stop learning " + mv + "?"));
			}
		} else if (step == Step.GIVE_UP) {
			if (yes) finish(-1, List.of((m == null ? "It" : m.name()) + " did not learn " + mv + "."));
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
		finish(slot, List.of("1, 2 and... Poof!", m.name() + " forgot " + old + ".", "And...", m.name() + " learned " + mv + "!"));
		Music.jingle("level_up");
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
				int n = m == null ? 1 : m.moves.size();
				if (Ui.isUp(e)) at = Math.floorMod(at - 1, n);
				else if (Ui.isDown(e)) at = Math.floorMod(at + 1, n);
				else if (Ui.isA(e)) forget(at);
				else if (Ui.isB(e)) {
					step = Step.GIVE_UP;
					box.set(List.of("Stop learning " + Move.get(move).name() + "?"));
				}
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
		int h = boxHeight(), m = 6 * s;
		box.render(g, font, m, height - h - m, width - 2 * m, h, s, ticks, yesNo == null && step != Step.PICK);
		if (yesNo != null) {
			int w = yesNo.width(font, s);
			choice(g, yesNo, width - w - m, height - h - m - yesNo.height(s) - 4 * s, w, i -> answer(i == 0));
		}
		if (step == Step.PICK) {
			Mon mon = mon();
			if (mon == null) return;
			int w = Math.min(width - 2 * m, 200 * s), row = 16 * s, x = (width - w) / 2, y = m;
			Ui.panel(g, x, y, w, row * (mon.moves.size() + 1) + 8 * s, s);
			for (int i = 0; i <= mon.moves.size(); i++) {
				String id = i < mon.moves.size() ? mon.moves.get(i) : move;
				Move mv = Move.get(id);
				int ry = y + 4 * s + i * row;
				int idx = i;
				if (i < mon.moves.size() && over(x, ry, w, row)) at = i;
				boolean on = i == at && i < mon.moves.size();
				Ui.round(g, x + 4 * s, ry + s, w - 8 * s, row - 2 * s, Ui.alpha(Ui.typeColor(mv.type()), on ? 0.85F : i == mon.moves.size() ? 0.3F : 0.45F), s);
				Ui.text(g, font, (i == mon.moves.size() ? "NEW: " : "") + mv.name(), x + 10 * s, ry + 5 * s, Ui.TEXT, s);
				Ui.right(g, font, mv.type().label.toUpperCase() + "  PP " + mv.pp(), x + w - 10 * s, ry + 5 * s, Ui.TEXT, s);
				if (i < mon.moves.size()) hit(x, ry, w, row, () -> forget(idx));
			}
		}
	}
}
