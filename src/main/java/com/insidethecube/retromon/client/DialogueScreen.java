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

	/** A line that starts with who's speaking: "OAK: ..." (capitals, before a colon). */
	private static final java.util.regex.Pattern SPEAKER = java.util.regex.Pattern.compile("^([A-Z][A-Z .'é&]{1,20}): (.+)$");

	public DialogueScreen(List<String> lines, String mode, int npc, CompoundTag extra) {
		super("Dialogue");
		// "OAK: Hey! Wait!": the speaker goes on the tab over the box (as the viewer shows it), not in the words.
		String who = null;
		java.util.List<String> said = new java.util.ArrayList<>();
		for (String l : lines) {
			java.util.regex.Matcher m = SPEAKER.matcher(l);
			if (m.matches() && (who == null || who.equals(m.group(1)))) {
				who = m.group(1);
				said.add(m.group(2));
			} else said.add(l);
		}
		this.box = new TextBox(said);
		if (who != null) box.speaker = who;
		this.mode = mode;
		this.npc = npc;
		this.extra = extra;
		String t = extra.getStringOr("trainer", "");
		this.trainer = t.isEmpty() ? null : t;
		Trainers.Trainer tr = Trainers.get(trainer);
		if (tr != null && box.speaker == null) box.speaker = tr.title();
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
		// The UI Viewer's text box (teal glass, the speaker on a cream tab, names in gold), rising into place; a
		// question's answers in its list above the box's right end.
		float in = Math.min(1, (ticks + partial) / 5F);
		float rise = (1 - in * in * (3 - 2 * in)) * 90;
		g.pose().pushMatrix();
		g.pose().translate(0, V.s(rise));
		box.renderV(g, font, 140, 26, 1000, ticks, partial, yesNo == null);
		g.pose().popMatrix();
		if (yesNo != null) {
			float w = 300;
			vchoice(g, yesNo, V.W - 140 - w, V.H - 26 - 150 - V.choiceHeight(yesNo.options.size()), w, i -> done(i == 0));
		}
	}
}
