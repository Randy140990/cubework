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
	/** Jingles on lines ("got OAK's PARCEL!"): line index to jingle, and the last line one played for. */
	private final java.util.Map<Integer, String> cues = new java.util.HashMap<>();
	private int cued = -1;

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
		for (net.minecraft.nbt.Tag c : extra.getListOrEmpty("cues")) c.asString().ifPresent(v -> {
			int at = v.indexOf(':');
			if (at > 0) {
				try {
					cues.put(Integer.parseInt(v.substring(0, at)), v.substring(at + 1));
				} catch (NumberFormatException ignored) {
					// Not a cue.
				}
			}
		});
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
		int line = box.message();
		if (line >= 0 && line != cued) {
			cued = line;
			String j = cues.get(line);
			if (j != null) Music.jingle(j);
		}
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
		// (The cinema frame fades in and out round this, from the HUD.) The box rises into place.
		int h = boxHeight(), m = 6 * s;
		float in = Math.min(1, (ticks + partial) / 5F);
		int rise = Math.round((1 - in * in * (3 - 2 * in)) * (h + m) * 0.6F);
		box.render(g, font, m, height - h - m + rise, width - 2 * m, h, s, ticks, yesNo == null);
		if (yesNo != null) {
			int w = yesNo.width(font, s);
			choice(g, yesNo, width - w - m, height - h - m - yesNo.height(s) - 4 * s, w, i -> done(i == 0));
		}
	}
}
