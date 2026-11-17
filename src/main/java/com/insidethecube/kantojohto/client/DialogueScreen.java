package com.insidethecube.kantojohto.client;

import java.util.List;

import com.insidethecube.kantojohto.game.Net;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.nbt.CompoundTag;

/**
 * Someone talking to you: their lines in the text box, and for the nurse a YES/NO at the end. When it
 * closes the game hears what you said (heal, open the Mart, start the battle).
 */
public class DialogueScreen extends GameScreen {
	private final TextBox box;
	private final String mode, trainer;
	private final int npc;
	private Choice yesNo;
	private boolean sent;

	public DialogueScreen(List<String> lines, String mode, int npc, String trainer) {
		super("Dialogue");
		this.box = new TextBox(lines);
		this.mode = mode;
		this.npc = npc;
		this.trainer = trainer;
	}

	@Override
	public void tick() {
		super.tick();
		box.tick();
		if (mode.equals("heal") && yesNo == null && box.atEnd()) yesNo = new Choice(List.of("YES", "NO"));
	}

	@Override
	protected boolean key(KeyEvent e) {
		if (yesNo != null) {
			if (yesNo.key(e)) return true;
			if (Ui.isA(e)) {
				Sounds.select();
				done(yesNo.at == 0);
				return true;
			}
			if (Ui.isB(e)) {
				done(false);
				return true;
			}
			return true;
		}
		if (Ui.isA(e) || Ui.isB(e)) {
			if (box.press()) done(false);
			return true;
		}
		return false;
	}

	private void done(boolean yes) {
		if (sent) return;
		sent = true;
		CompoundTag t = new CompoundTag();
		t.putInt("npc", npc);
		t.putString("mode", mode);
		t.putBoolean("yes", yes);
		if (trainer != null) t.putString("trainer", trainer);
		ClientPlayNetworking.send(new Net.Act("dialogue_done", t));
		if (minecraft != null && minecraft.screen == this) minecraft.setScreen(null);
	}

	@Override
	public void onClose() {
		done(false);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
		int h = boxHeight();
		box.render(g, font, 0, height - h, width, h, s, ticks, yesNo == null);
		if (yesNo != null) yesNo.render(g, font, width - yesNo.width(font, s) - 2 * s, height - h - yesNo.height(s), s);
	}
}
