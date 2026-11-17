package com.insidethecube.retromon.client;

import java.util.List;

import com.insidethecube.retromon.game.Net;
import com.insidethecube.retromon.game.Trainers;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.nbt.CompoundTag;

/**
 * Someone talking to you: their lines in the text box, and a YES/NO at the end for the nurse and for gifts.
 * When it closes the game hears what you said (heal, open the Mart, start the battle, take the gift).
 */
public class DialogueScreen extends GameScreen {
	private final TextBox box;
	private final String mode, trainer;
	private final CompoundTag extra;
	private final int npc;
	private Choice yesNo;
	private boolean sent;

	public DialogueScreen(List<String> lines, String mode, int npc, CompoundTag extra) {
		super("Dialogue");
		this.box = new TextBox(lines);
		this.mode = mode;
		this.npc = npc;
		this.extra = extra;
		String t = extra.getStringOr("trainer", "");
		this.trainer = t.isEmpty() ? null : t;
		Trainers.Trainer tr = Trainers.get(trainer);
		if (tr != null) box.speaker = tr.title();
		String cry = extra.getStringOr("cry", "");
		if (!cry.isEmpty()) Sounds.cry(cry, 1F);
		// The camera closes in on the two of you, and the black bars come in, as in a cutscene.
		Adventure.focus(npc);
	}

	private boolean asks() {
		return mode.equals("heal") || mode.equals("gift");
	}

	@Override
	public void tick() {
		super.tick();
		box.tick();
		if (asks() && yesNo == null && box.atEnd()) yesNo = new Choice(List.of("YES", "NO"));
	}

	@Override
	protected boolean key(KeyEvent e) {
		if (yesNo != null) {
			if (yesNo.key(e)) return true;
			if (Ui.isA(e)) {
				Sounds.select();
				done(yesNo.at == 0);
			} else if (Ui.isB(e)) done(false);
			return true;
		}
		if (Ui.isA(e) || Ui.isB(e)) {
			if (box.press()) done(false);
			return true;
		}
		return false;
	}

	@Override
	protected void clickAnywhere() {
		if (yesNo == null && box.press()) done(false);
	}

	@Override
	protected void back() {
		if (yesNo != null) done(false);
		else clickAnywhere();
	}

	private void done(boolean yes) {
		if (sent) return;
		sent = true;
		CompoundTag t = extra.copy();
		t.putInt("npc", npc);
		t.putString("mode", mode);
		t.putBoolean("yes", yes);
		if (trainer != null) t.putString("trainer", trainer);
		ClientPlayNetworking.send(new Net.Act("dialogue_done", t));
		Adventure.focus(-1);
		if (minecraft != null && minecraft.screen == this) minecraft.setScreen(null);
	}

	@Override
	public void onClose() {
		done(false);
	}

	@Override
	protected void draw(GuiGraphics g, float partial) {
		if (npc >= 0) Ui.cinema(g, width, height, Math.min(1, (ticks + partial) / 6F));
		int h = boxHeight(), m = 6 * s;
		box.render(g, font, m, height - h - m, width - 2 * m, h, s, ticks, yesNo == null);
		if (yesNo != null) {
			int w = yesNo.width(font, s);
			choice(g, yesNo, width - w - m, height - h - m - yesNo.height(s) - 4 * s, w, i -> done(i == 0));
		}
	}
}
