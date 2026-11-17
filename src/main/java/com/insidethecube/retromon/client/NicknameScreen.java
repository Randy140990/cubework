package com.insidethecube.retromon.client;

import java.util.List;

import com.insidethecube.retromon.engine.Species;
import com.insidethecube.retromon.game.Net;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** "Give a nickname to RATTATA?" then a box to type it in (up to ten letters, as the games allow). */
public class NicknameScreen extends GameScreen {
	private final String where, species;
	private final int index;
	private Choice yesNo = new Choice(List.of("YES", "NO"));
	private EditBox field;
	private boolean naming, sent;
	private final TextBox box;

	public NicknameScreen(CompoundTag t) {
		super("Nickname");
		this.where = t.getStringOr("where", "party");
		this.index = t.getIntOr("i", 0);
		this.species = t.getStringOr("species", "");
		Species sp = Species.ALL.get(species);
		box = TextBox.still("Give a nickname to " + (sp == null ? "it" : sp.name()) + "?");
	}

	@Override
	protected void init() {
		super.init();
		int w = Math.min(width - 40 * s, 160 * s);
		field = new EditBox(font, (width - w) / 2, height / 2 - 10, w, 20, Component.literal("Nickname"));
		field.setMaxLength(10);
		field.visible = naming;
		addRenderableWidget(field);
		if (naming) setFocused(field);
	}

	private void answer(boolean yes) {
		if (!yes) {
			done("");
			return;
		}
		naming = true;
		field.visible = true;
		setFocused(field);
	}

	private void done(String name) {
		if (sent) return;
		sent = true;
		CompoundTag t = new CompoundTag();
		t.putString("where", where);
		t.putInt("i", index);
		t.putString("name", name);
		ClientPlayNetworking.send(new Net.Act("nickname", t));
		if (minecraft != null && minecraft.screen == this) minecraft.setScreen(null);
	}

	@Override
	protected boolean key(KeyEvent e) {
		if (naming) {
			if (e.key() == GLFW.GLFW_KEY_ENTER || e.key() == GLFW.GLFW_KEY_KP_ENTER) {
				done(field.getValue());
				return true;
			}
			if (e.key() == GLFW.GLFW_KEY_ESCAPE) {
				done("");
				return true;
			}
			return false;
		}
		if (yesNo.key(e)) return true;
		if (Ui.isA(e)) answer(yesNo.at == 0);
		else if (Ui.isB(e)) answer(false);
		return true;
	}

	@Override
	public void onClose() {
		done("");
	}

	@Override
	protected void draw(GuiGraphics g, float partial) {
		int h = boxHeight(), m = 6 * s;
		box.render(g, font, m, height - h - m, width - 2 * m, h, s, ticks, false);
		if (!naming) {
			int w = yesNo.width(font, s);
			choice(g, yesNo, width - w - m, height - h - m - yesNo.height(s) - 4 * s, w, i -> answer(i == 0));
			return;
		}
		int pw = Math.min(width - 20 * s, 200 * s), ph = 60 * s, px = (width - pw) / 2, py = height / 2 - 34 * s;
		Ui.panel(g, px, py, pw, ph, s);
		Ui.centred(g, font, "NICKNAME?", width / 2, py + 6 * s, Ui.MUTED, s, true);
		Ui.centred(g, font, "Enter to keep it, Esc for none", width / 2, py + ph - 12 * s, Ui.DIM, s, true);
		int bw = 40 * s, bx = px + pw - bw - 8 * s, by = py + ph - 26 * s;
		Ui.button(g, font, bx, by, bw, 12 * s, "OK", Ui.GREEN, over(bx, by, bw, 12 * s), s);
		hit(bx, by, bw, 12 * s, () -> done(field.getValue()));
	}
}
