package com.insidethecube.retromon.client;

import java.util.List;

import com.insidethecube.retromon.engine.Species;
import com.insidethecube.retromon.game.Net;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** "Give a nickname to RATTATA?" then a box to type it in (up to ten letters, as the games allow). */
public class NicknameScreen extends GameScreen {
	private final String where, species;
	private final int index;
	private Choice yesNo = new Choice(List.of("YES", "NO"));
	/** The name as you type it (our own entry box, drawn in the game's style). */
	private final StringBuilder name = new StringBuilder();
	private boolean naming, sent;
	private final TextBox box;
	private static final int MAX = 10;

	public NicknameScreen(CompoundTag t) {
		super("Nickname");
		this.where = t.getStringOr("where", "party");
		this.index = t.getIntOr("i", 0);
		this.species = t.getStringOr("species", "");
		Species sp = Species.ALL.get(species);
		box = TextBox.still("Give a nickname to " + (sp == null ? "it" : sp.name()) + "?");
	}

	private void answer(boolean yes) {
		Sounds.press();
		if (!yes) {
			done("");
			return;
		}
		naming = true;
	}

	@Override
	public boolean charTyped(net.minecraft.client.input.CharacterEvent e) {
		if (!naming) return false;
		String c = e.codepointAsString();
		if (name.length() < MAX && c.length() == 1 && (Character.isLetterOrDigit(c.charAt(0)) || " .'-♂♀".indexOf(c.charAt(0)) >= 0)) {
			name.append(Character.toUpperCase(c.charAt(0)));
			Sounds.press();
		}
		return true;
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
				Sounds.press();
				done(name.toString().trim());
			} else if (e.key() == GLFW.GLFW_KEY_BACKSPACE && name.length() > 0) {
				name.setLength(name.length() - 1);
			}
			return true;
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
		int pw = Math.min(width - 20 * s, 210 * s), ph = 74 * s, px = (width - pw) / 2, py = height / 2 - 44 * s;
		Ui.round(g, px, py, pw, ph, 0xF0141B2E, 3 * s);
		Species sp = Species.ALL.get(species);
		Ui.heading(g, font, "NICKNAME FOR " + (sp == null ? "IT" : sp.name()), px + 12 * s, py + 9 * s, Ui.TEXT, s);
		// The entry: a cream field with the name and a blinking cursor, then the slots left.
		int fx = px + 12 * s, fy = py + 24 * s, fw = pw - 24 * s, fh = 20 * s;
		Ui.round(g, fx, fy, fw, fh, Ui.CREAM, 2 * s);
		String shown = name + ((ticks / 10) % 2 == 0 && name.length() < MAX ? "_" : "");
		Ui.draw(g, font, Ui.bold(shown), fx + 8 * s, fy + 6 * s, Ui.CREAM_INK, s, false);
		Ui.draw(g, font, Ui.ui((MAX - name.length()) + " left"), fx + fw - 8 * s - Ui.width(font, (MAX - name.length()) + " left", s), fy + 6 * s, 0xFF7A849E, s, false);
		Ui.text(g, font, "Enter: done.  Empty: no name.", px + 12 * s, py + 50 * s, Ui.DIM, s);
		int bw = 46 * s, bx = px + pw - bw - 12 * s, by = py + ph - 18 * s + 2 * s;
		Ui.button(g, font, bx, by - 4 * s, bw, 14 * s, "OK", Ui.GREEN, over(bx, by - 4 * s, bw, 14 * s), s);
		hit(bx, by - 4 * s, bw, 14 * s, () -> done(name.toString().trim()));
	}
}
