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
		box = TextBox.still("Do you want to give a nickname to " + (sp == null ? "it" : sp.name()) + "?");
	}

	private void answer(boolean yes) {
		Sounds.press();
		if (!yes) {
			done("");
			return;
		}
		naming = true;
		Species sp = Species.ALL.get(species);
		entry = new NameEntry((sp == null ? "ITS" : Look.text(sp.name()) + "'S") + " NICKNAME", n -> {
			Sounds.press();
			done(n.toUpperCase());
		}, () -> done(""));
	}

	/** The UI Viewer's name entry (seven boxes and the letters), once you've said yes. */
	private NameEntry entry;

	@Override
	public boolean charTyped(net.minecraft.client.input.CharacterEvent e) {
		if (!naming || entry == null) return false;
		return entry.typed(e);
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
			if (entry != null) entry.key(e);
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

	/** Esc: no nickname (while typing one too: it's kept as it is). */
	@Override
	protected void escape() {
		done("");
	}

	/** "Do you want to give a nickname?" in the UI Viewer's text box and list; then its name entry. */
	@Override
	protected void draw(GuiGraphics g, float partial) {
		if (!naming) {
			// The UI Viewer's "Being given a POKéMON": it held up on its stand against the warm light.
			Scenes.backdrop(g, "gift");
			if (Species.ALL.containsKey(species)) Scenes.model(g, (ticks + partial) / 20F, species, 0);
			box.renderV(g, font, 140, 26, 1000, ticks, partial, false);
			vchoice(g, yesNo, V.W - 140 - 300, V.H - 26 - 150 - V.choiceHeight(2), 300, i -> answer(i == 0));
			return;
		}
		g.fill(0, 0, width, height, 0x60000000);
		entry.draw(g, this::vhit, (ticks + partial) / 20F);
		V.hints(g, "A", "Choose", "B", "Delete");
	}
}
