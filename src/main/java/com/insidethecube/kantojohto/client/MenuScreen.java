package com.insidethecube.kantojohto.client;

import java.util.ArrayList;
import java.util.List;

import com.insidethecube.kantojohto.engine.Mon;
import com.insidethecube.kantojohto.engine.Move;
import com.insidethecube.kantojohto.game.Net;
import com.insidethecube.kantojohto.game.PlayerData;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.nbt.CompoundTag;

/** The START menu: your Pokémon, your bag, your trainer card, saving, and the camera. */
public class MenuScreen extends GameScreen {
	private enum View {
		MENU, PARTY, SUMMARY, BAG, POTION_ON, CARD, SAVE, OPTION, MESSAGE
	}

	private View view = View.MENU;
	private final Choice menu = new Choice(List.of("POKéMON", "BAG", "CARD", "SAVE", "OPTION", "EXIT"));
	private final Choice bag = new Choice(List.of("POTION", "POKé BALL", "CANCEL"));
	private Choice yesNo;
	private int partyAt;
	private TextBox message;
	private View afterMessage = View.MENU;

	public MenuScreen() {
		super("Menu");
	}

	private static String name() {
		Minecraft mc = Minecraft.getInstance();
		return mc.player == null ? "YOU" : mc.player.getGameProfile().name().toUpperCase();
	}

	@Override
	public void tick() {
		super.tick();
		if (message != null) message.tick();
	}

	private void say(View then, String... lines) {
		message = new TextBox(List.of(lines));
		afterMessage = then;
		view = View.MESSAGE;
	}

	@Override
	protected boolean key(KeyEvent e) {
		PlayerData d = ClientState.data;
		switch (view) {
			case MENU -> {
				if (menu.key(e)) return true;
				if (Ui.isB(e)) {
					onClose();
					return true;
				}
				if (!Ui.isA(e)) return true;
				Sounds.select();
				switch (menu.at) {
					case 0 -> {
						if (d.party.isEmpty()) say(View.MENU, "You don't have any POKéMON yet.");
						else {
							partyAt = 0;
							view = View.PARTY;
						}
					}
					case 1 -> view = View.BAG;
					case 2 -> view = View.CARD;
					case 3 -> {
						yesNo = new Choice(List.of("YES", "NO"));
						view = View.SAVE;
					}
					case 4 -> view = View.OPTION;
					default -> onClose();
				}
			}
			case PARTY -> {
				if (Ui.isUp(e)) partyAt = Math.floorMod(partyAt - 1, d.party.size());
				else if (Ui.isDown(e)) partyAt = Math.floorMod(partyAt + 1, d.party.size());
				else if (Ui.isA(e)) view = View.SUMMARY;
				else if (Ui.isB(e)) view = View.MENU;
			}
			case SUMMARY -> {
				if (Ui.isA(e) || Ui.isB(e)) view = View.PARTY;
			}
			case BAG -> {
				if (bag.key(e)) return true;
				if (Ui.isB(e) || (Ui.isA(e) && bag.at == 2)) view = View.MENU;
				else if (Ui.isA(e) && bag.at == 0) {
					if (d.potions <= 0) say(View.BAG, "You don't have any POTIONS.");
					else if (d.party.isEmpty()) say(View.BAG, "You don't have any POKéMON to use it on.");
					else {
						partyAt = 0;
						view = View.POTION_ON;
					}
				} else if (Ui.isA(e)) say(View.BAG, "Throw a POKé BALL at a wild POKéMON in battle to catch it!");
			}
			case POTION_ON -> {
				if (Ui.isUp(e)) partyAt = Math.floorMod(partyAt - 1, d.party.size());
				else if (Ui.isDown(e)) partyAt = Math.floorMod(partyAt + 1, d.party.size());
				else if (Ui.isB(e)) view = View.BAG;
				else if (Ui.isA(e)) {
					Mon m = d.party.get(partyAt);
					if (m.fainted() || m.hp >= m.maxHp()) {
						say(View.POTION_ON, "It won't have any effect.");
						return true;
					}
					CompoundTag t = new CompoundTag();
					t.putInt("i", partyAt);
					ClientPlayNetworking.send(new Net.Act("potion", t));
					int healed = Math.min(20, m.maxHp() - m.hp);
					m.hp += healed;
					d.potions--;
					Sounds.heal();
					say(View.BAG, m.name() + "'s HP was restored by " + healed + " point(s).");
				}
			}
			case CARD -> {
				if (Ui.isA(e) || Ui.isB(e)) view = View.MENU;
			}
			case SAVE -> {
				if (yesNo.key(e)) return true;
				if (Ui.isB(e) || (Ui.isA(e) && yesNo.at == 1)) view = View.MENU;
				else if (Ui.isA(e)) {
					ClientPlayNetworking.send(new Net.Act("save", new CompoundTag()));
					Sounds.heal();
					say(View.MENU, name() + " saved the game.");
				}
			}
			case OPTION -> {
				if (Ui.isLeft(e) || Ui.isRight(e) || Ui.isA(e)) {
					Adventure.diagonal = !Adventure.diagonal;
					Sounds.select();
				} else if (Ui.isB(e)) view = View.MENU;
			}
			case MESSAGE -> {
				if ((Ui.isA(e) || Ui.isB(e)) && message.press()) view = afterMessage;
			}
		}
		return true;
	}

	@Override
	public void onClose() {
		if (minecraft != null) minecraft.setScreen(null);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
		PlayerData d = ClientState.data;
		int h = boxHeight();
		switch (view) {
			case MENU, BAG, SAVE, MESSAGE -> {
				if (view == View.MESSAGE && afterMessage != View.MENU && afterMessage != View.BAG) {
					renderParty(g, d, false);
				} else {
					menu.render(g, font, width - menu.width(font, s) - 2 * s, 2 * s, s);
				}
				if (view == View.BAG || (view == View.MESSAGE && afterMessage == View.BAG)) renderBag(g, d);
				if (view == View.SAVE) {
					TextBox ask = new TextBox(List.of("Would you like to save the game?"));
					ask.press();
					ask.render(g, font, 0, height - h, width, h, s, ticks, false);
					yesNo.render(g, font, width - yesNo.width(font, s) - 2 * s, height - h - yesNo.height(s), s);
				}
				if (view == View.MESSAGE) message.render(g, font, 0, height - h, width, h, s, ticks, true);
			}
			case PARTY, POTION_ON -> {
				renderParty(g, d, true);
				TextBox hint = new TextBox(List.of(view == View.PARTY ? "Choose a POKéMON." : "Use on which POKéMON?"));
				hint.press();
				hint.render(g, font, 0, height - h, width, h, s, ticks, false);
			}
			case SUMMARY -> renderSummary(g, d);
			case CARD -> renderCard(g, d);
			case OPTION -> {
				int w = 200 * s, bh = 50 * s, x = (width - w) / 2, y = (height - bh) / 2;
				Ui.box(g, x, y, w, bh, s);
				Ui.text(g, font, "CAMERA", x + 10 * s, y + 10 * s, Ui.INK, s);
				Ui.text(g, font, Ui.CURSOR + " " + (Adventure.diagonal ? "DIAGONAL" : "NORTH UP"), x + 90 * s, y + 10 * s, Ui.INK, s);
				Ui.text(g, font, "Scroll the mouse wheel to zoom.", x + 10 * s, y + 30 * s, Ui.GREY, s);
			}
		}
	}

	private void renderBag(GuiGraphics g, PlayerData d) {
		int w = 150 * s, bh = bag.height(s) + 4 * s, x = 4 * s, y = 4 * s;
		Ui.box(g, x, y, w, bh, s);
		String[] have = {"x" + d.potions, "x" + d.balls, ""};
		for (int i = 0; i < bag.options.size(); i++) {
			int ty = y + 8 * s + i * 12 * s;
			if (i == bag.at && view == View.BAG) Ui.text(g, font, Ui.CURSOR, x + 6 * s, ty, Ui.INK, s);
			Ui.text(g, font, bag.options.get(i), x + 14 * s, ty, Ui.INK, s);
			Ui.text(g, font, have[i], x + w - 8 * s - Ui.width(font, have[i], s), ty, Ui.INK, s);
		}
	}

	/** Your party, one row each: name, level, status, HP. */
	public static void party(GuiGraphics g, Font font, List<Mon> party, int at, boolean cursor, int x, int y, int w, int s) {
		int row = 22 * s;
		Ui.box(g, x, y, w, party.size() * row + 10 * s, s);
		for (int i = 0; i < party.size(); i++) {
			Mon m = party.get(i);
			int ty = y + 6 * s + i * row;
			if (cursor && i == at) Ui.text(g, font, Ui.CURSOR, x + 6 * s, ty + 4 * s, Ui.INK, s);
			Ui.text(g, font, m.name(), x + 16 * s, ty, Ui.INK, s);
			String lv = (m.fainted() ? "FNT" : m.status.tag.isEmpty() ? "" : m.status.tag + " ") + ":L" + m.level;
			Ui.text(g, font, lv, x + 16 * s + 80 * s, ty, Ui.INK, s);
			Ui.hpBar(g, font, x + 16 * s, ty + 10 * s, 90 * s, m.hp, m.maxHp(), s);
			String hp = m.hp + "/" + m.maxHp();
			Ui.text(g, font, hp, x + w - 8 * s - Ui.width(font, hp, s), ty + 10 * s, Ui.INK, s);
		}
	}

	private void renderParty(GuiGraphics g, PlayerData d, boolean cursor) {
		int w = Math.min(width - 8 * s, 220 * s);
		party(g, font, d.party, partyAt, cursor, (width - w) / 2, 4 * s, w, s);
	}

	private void renderSummary(GuiGraphics g, PlayerData d) {
		if (d.party.isEmpty()) return;
		Mon m = d.party.get(Math.min(partyAt, d.party.size() - 1));
		int w = Math.min(width - 8 * s, 240 * s), bh = 150 * s, x = (width - w) / 2, y = Math.max(4 * s, (height - bh) / 2);
		Ui.box(g, x, y, w, bh, s);
		int l = x + 10 * s, ty = y + 10 * s;
		Ui.text(g, font, m.name() + "   :L" + m.level + "   No." + m.species.dex(), l, ty, Ui.INK, s);
		ty += 12 * s;
		String type = m.species.type1().name() + (m.species.type2() != null && m.species.type2() != m.species.type1() ? "/" + m.species.type2().name() : "");
		Ui.text(g, font, "TYPE/ " + type + (m.status.tag.isEmpty() ? "" : "   STATUS/ " + m.status.tag), l, ty, Ui.INK, s);
		ty += 12 * s;
		Ui.hpBar(g, font, l, ty, 100 * s, m.hp, m.maxHp(), s);
		Ui.text(g, font, m.hp + "/" + m.maxHp(), l + 108 * s, ty, Ui.INK, s);
		ty += 14 * s;
		int next = m.level >= 100 ? 0 : m.species.growth().expAt(m.level + 1) - m.exp;
		Ui.text(g, font, "EXP " + m.exp + "   TO NEXT LEVEL " + next, l, ty, Ui.INK, s);
		ty += 14 * s;
		String[] names = {"ATTACK", "DEFENSE", "SPEED", "SPCL.ATK", "SPCL.DEF"};
		int[] stats = {m.attack(), m.defense(), m.speed(), m.spAttack(), m.spDefense()};
		for (int i = 0; i < 5; i++) {
			int cx = l + (i % 2) * 110 * s, cy = ty + (i / 2) * 10 * s;
			Ui.text(g, font, names[i] + " " + stats[i], cx, cy, Ui.INK, s);
		}
		ty += 34 * s;
		List<String> moves = new ArrayList<>();
		for (int i = 0; i < m.moves.size(); i++) {
			Move mv = Move.get(m.moves.get(i));
			moves.add(mv.name().toUpperCase() + "  " + mv.type().name() + "  PP " + m.pp.get(i) + "/" + mv.pp());
		}
		for (String mv : moves) {
			Ui.text(g, font, mv, l, ty, Ui.INK, s);
			ty += 10 * s;
		}
	}

	private void renderCard(GuiGraphics g, PlayerData d) {
		int w = Math.min(width - 8 * s, 220 * s), bh = 96 * s, x = (width - w) / 2, y = Math.max(4 * s, (height - bh) / 2);
		Ui.box(g, x, y, w, bh, s);
		int l = x + 10 * s, ty = y + 10 * s;
		Ui.text(g, font, "NAME/ " + name(), l, ty, Ui.INK, s);
		Ui.text(g, font, "MONEY/ ¥" + d.money, l, ty + 12 * s, Ui.INK, s);
		Ui.text(g, font, "POKéDEX/ SEEN " + d.seen.size() + "  OWN " + d.caught.size(), l, ty + 24 * s, Ui.INK, s);
		Ui.text(g, font, "BADGES/ " + d.badges.size(), l, ty + 40 * s, Ui.INK, s);
		int bx = l;
		for (String b : d.badges) {
			Ui.text(g, font, b, bx, ty + 52 * s, 0xFF906018, s);
			bx += Ui.width(font, b, s) + 10 * s;
		}
		if (d.badges.isEmpty()) Ui.text(g, font, "None yet. BROCK waits in PALLET TOWN.", l, ty + 52 * s, Ui.GREY, s);
	}
}
