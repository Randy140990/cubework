package com.insidethecube.retromon.client;

import java.util.ArrayList;
import java.util.List;

import com.insidethecube.retromon.engine.Item;
import com.insidethecube.retromon.engine.Mon;
import com.insidethecube.retromon.engine.Move;
import com.insidethecube.retromon.engine.Species;
import com.insidethecube.retromon.engine.Type;
import com.insidethecube.retromon.game.Net;
import com.insidethecube.retromon.game.PlayerData;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.nbt.CompoundTag;

/**
 * The START menu, on your Pokédex: its apps (the Pokédex, your Pokémon and their summaries, the bag, the Town
 * Map, clothes, your trainer card, waiting, saving and options) as round buttons, as the newer games have them.
 */
public class MenuScreen extends DeviceScreen {
	private enum View {
		MENU, PARTY, PARTY_ACTION, SUMMARY, BAG, BAG_ACTION, USE_ON, CARD, SAVE, WAIT, OPTION, MESSAGE
	}

	/** Which OPTION row the keys change: 0 text speed, 1 music, 2 the Pokédex voice, 3 nights, 4 wild Pokémon, 5 walking with you. */
	private int optionRow;
	private static final String[] ITEMS = {"POKéDEX", "POKéMON", "BAG", "MAP", "CLOTHES", "CARD", "WAIT", "SAVE", "OPTION", "EXIT"};
	/** Each app's picture (textures/gui/icon). */
	private static final String[] ICONS = {"dex", "ball", "backpack", "map", "cap", "card", "clock", "save", "cog", "close"};
	/** The apps in rows of this many. */
	private static final int COLS = 5;
	/** One per menu item (WAIT's was missing, which made opening the menu crash). */
	private static final int[] COLORS = {0xFFE5525C, Ui.GREEN, Ui.AMBER, 0xFF4CB8A0, 0xFFB07CE0, Ui.BLUE, 0xFF7C8CF0, Ui.CYAN, 0xFF8C93A8, Ui.RED};
	private static final String[] POCKETS = {"ITEMS", "BALLS", "KEY ITEMS", "TM & HM"};

	private View view = View.MENU, afterMessage = View.MENU;
	private int menuAt, partyAt, swapFrom = -1, pocket, bagAt, bagTop, actionAt;
	private Choice yesNo, waitChoice;
	private TextBox message;

	public MenuScreen() {
		super("Menu");
	}

	private static String name() {
		if (!ClientState.data.name.isEmpty()) return ClientState.data.name;
		Minecraft mc = Minecraft.getInstance();
		return mc.player == null ? "YOU" : mc.player.getGameProfile().name().toUpperCase();
	}

	@Override
	public void tick() {
		super.tick();
		if (message != null) message.tick();
	}

	/** Lines from the game (an item's effect) shown here, then back to where you were. */
	public void message(List<String> lines) {
		message = new TextBox(lines);
		if (view != View.MESSAGE) afterMessage = view == View.USE_ON || view == View.BAG_ACTION ? View.BAG : view;
		view = View.MESSAGE;
	}

	private void say(View then, String... lines) {
		message = new TextBox(List.of(lines));
		afterMessage = then;
		view = View.MESSAGE;
	}

	private List<String> pocketItems() {
		List<String> out = new ArrayList<>();
		for (String id : ClientState.data.items.keySet()) {
			Item it = Item.find(id);
			if (it == null) continue;
			int p = it.teaches() ? 3 : it.keyItem() ? 2 : it.ball() ? 1 : 0;
			if (p == pocket) out.add(id);
		}
		return out;
	}

	// ---------- choosing ----------

	private void pickMenu(int i) {
		PlayerData d = ClientState.data;
		menuAt = i;
		Sounds.press();
		switch (ITEMS[i]) {
			case "POKéDEX" -> {
				// Oak gives you it, once you've taken him his parcel.
				if (!d.hasDex()) say(View.MENU, "You don't have a POKéDEX yet.");
				else if (minecraft != null) minecraft.setScreen(new PokedexScreen(this));
			}
			case "MAP" -> {
				if (minecraft != null) minecraft.setScreen(new TownMapScreen(this));
			}
			case "CLOTHES" -> {
				if (minecraft != null) minecraft.setScreen(new ClothesScreen(this));
			}
			case "POKéMON" -> {
				if (d.party.isEmpty()) say(View.MENU, "You don't have any POKéMON yet.");
				else {
					partyAt = 0;
					swapFrom = -1;
					view = View.PARTY;
				}
			}
			case "BAG" -> {
				bagAt = 0;
				view = View.BAG;
			}
			case "CARD" -> view = View.CARD;
			case "SAVE" -> {
				yesNo = new Choice(List.of("YES", "NO"));
				view = View.SAVE;
			}
			case "OPTION" -> view = View.OPTION;
			case "WAIT" -> {
				waitChoice = new Choice(List.of("MORNING", "MIDDAY", "EVENING", "NIGHT", "CANCEL"));
				view = View.WAIT;
			}
			default -> onClose();
		}
	}

	private void pickParty(int i) {
		PlayerData d = ClientState.data;
		if (i >= d.party.size()) return;
		if (swapFrom >= 0) {
			if (swapFrom != i) {
				CompoundTag t = new CompoundTag();
				t.putInt("a", swapFrom);
				t.putInt("b", i);
				ClientPlayNetworking.send(new Net.Act("order", t));
				Mon m = d.party.get(swapFrom);
				d.party.set(swapFrom, d.party.get(i));
				d.party.set(i, m);
			}
			swapFrom = -1;
			partyAt = i;
			return;
		}
		partyAt = i;
		actionAt = 0;
		view = View.PARTY_ACTION;
	}

	private void partyAction(int a) {
		switch (a) {
			case 0 -> view = View.SUMMARY;
			case 1 -> {
				swapFrom = partyAt;
				view = View.PARTY;
			}
			case 2 -> {
				// This one walks with you now.
				CompoundTag t = new CompoundTag();
				t.putInt("i", partyAt);
				ClientPlayNetworking.send(new Net.Act("follow", t));
				ClientState.data.follow = partyAt;
				Mon m = partyAt < ClientState.data.party.size() ? ClientState.data.party.get(partyAt) : null;
				if (m != null) Sounds.cry(m.species.id(), 1F);
				say(View.PARTY, (m == null ? "It" : m.name()) + " will walk with you.");
			}
			default -> view = View.PARTY;
		}
	}

	private void pickBag(int i) {
		List<String> l = pocketItems();
		if (i >= l.size()) {
			view = View.MENU;
			return;
		}
		bagAt = i;
		actionAt = 0;
		view = View.BAG_ACTION;
	}

	private void bagAction(int a) {
		List<String> l = pocketItems();
		if (bagAt >= l.size() || a == 1) {
			view = View.BAG;
			return;
		}
		Item it = Item.find(l.get(bagAt));
		if (it == null) return;
		if (it.onPokemon()) {
			if (ClientState.data.party.isEmpty()) say(View.BAG, "You don't have any POKéMON to use it on.");
			else {
				partyAt = 0;
				view = View.USE_ON;
			}
			return;
		}
		if (it.repelSteps() > 0 || it.id().equals("escape_rope")) {
			use(it.id(), -1);
			return;
		}
		if (it.ball()) say(View.BAG, "Throw it at a wild POKéMON in battle to catch it!");
		else say(View.BAG, "OAK: " + name() + "! This isn't the time to use that!");
	}

	private void use(String item, int on) {
		CompoundTag t = new CompoundTag();
		t.putString("item", item);
		t.putInt("i", on);
		ClientPlayNetworking.send(new Net.Act("use", t));
		view = View.BAG;
	}

	// ---------- keys ----------

	@Override
	protected boolean key(KeyEvent e) {
		PlayerData d = ClientState.data;
		switch (view) {
			case MENU -> {
				if (Ui.isLeft(e)) menuAt = Math.floorMod(menuAt - 1, ITEMS.length);
				else if (Ui.isRight(e)) menuAt = Math.floorMod(menuAt + 1, ITEMS.length);
				else if (Ui.isUp(e) || Ui.isDown(e)) menuAt = Math.floorMod(menuAt + COLS, ITEMS.length);
				else if (Ui.isA(e)) pickMenu(menuAt);
				else if (Ui.isB(e)) onClose();
				if (Ui.isLeft(e) || Ui.isRight(e) || Ui.isUp(e) || Ui.isDown(e)) Sounds.select();
			}
			case PARTY, USE_ON -> {
				int n = Math.max(1, d.party.size());
				if (Ui.isUp(e)) partyAt = Math.floorMod(partyAt - 2, n);
				else if (Ui.isDown(e)) partyAt = Math.floorMod(partyAt + 2, n);
				else if (Ui.isLeft(e) || Ui.isRight(e)) partyAt = Math.floorMod(partyAt + (Ui.isLeft(e) ? -1 : 1), n);
				else if (Ui.isA(e)) {
					if (view == View.USE_ON) use(pocketItems().get(Math.min(bagAt, pocketItems().size() - 1)), partyAt);
					else pickParty(partyAt);
				} else if (Ui.isB(e)) back();
			}
			case PARTY_ACTION -> {
				if (Ui.isUp(e)) actionAt = Math.floorMod(actionAt - 1, 4);
				else if (Ui.isDown(e)) actionAt = Math.floorMod(actionAt + 1, 4);
				else if (Ui.isA(e)) partyAction(actionAt);
				else if (Ui.isB(e)) view = View.PARTY;
			}
			case SUMMARY -> {
				if (Ui.isLeft(e) || Ui.isUp(e)) partyAt = Math.floorMod(partyAt - 1, d.party.size());
				else if (Ui.isRight(e) || Ui.isDown(e)) partyAt = Math.floorMod(partyAt + 1, d.party.size());
				else if (Ui.isA(e) || Ui.isB(e)) view = View.PARTY;
			}
			case BAG -> {
				int n = pocketItems().size() + 1;
				if (Ui.isLeft(e) || Ui.isRight(e)) {
					pocket = Math.floorMod(pocket + (Ui.isLeft(e) ? -1 : 1), POCKETS.length);
					bagAt = 0;
					bagTop = 0;
				} else if (Ui.isUp(e)) bagAt = Math.floorMod(bagAt - 1, n);
				else if (Ui.isDown(e)) bagAt = Math.floorMod(bagAt + 1, n);
				else if (Ui.isA(e)) pickBag(bagAt);
				else if (Ui.isB(e)) view = View.MENU;
				if (Ui.isLeft(e) || Ui.isRight(e)) Sounds.select();
			}
			case BAG_ACTION -> {
				if (Ui.isUp(e) || Ui.isDown(e)) actionAt ^= 1;
				else if (Ui.isA(e)) bagAction(actionAt);
				else if (Ui.isB(e)) view = View.BAG;
			}
			case CARD -> {
				if (Ui.isA(e) || Ui.isB(e)) view = View.MENU;
			}
			case WAIT -> {
				if (waitChoice.key(e)) return true;
				if (Ui.isB(e)) view = View.MENU;
				else if (Ui.isA(e)) waitUntil(waitChoice.at);
			}
			case SAVE -> {
				if (yesNo.key(e)) return true;
				if (Ui.isB(e) || (Ui.isA(e) && yesNo.at == 1)) view = View.MENU;
				else if (Ui.isA(e)) save();
			}
			case OPTION -> {
				if (Ui.isUp(e) || Ui.isDown(e)) optionRow = Math.floorMod(optionRow + (Ui.isUp(e) ? -1 : 1), 6);
				else if (Ui.isLeft(e) || Ui.isRight(e) || Ui.isA(e)) {
					int step = Ui.isLeft(e) ? -1 : 1;
					switch (optionRow) {
						case 0 -> setSpeed(Math.floorMod(d.textSpeed + step, 3));
						case 1 -> Music.setLevel(Math.floorMod(Music.level() + step, 5));
						case 2 -> Settings.set("voice", Settings.voice() ? 0 : 1);
						case 3 -> {
							Settings.set("nights", Math.floorMod(Settings.nights() + step, 3));
							Night.send();
						}
						case 4 -> {
							Settings.set("seen", Settings.seen() ? 0 : 1);
							Settings.sendPrefs();
						}
						default -> {
							Settings.set("follow", Settings.follower() ? 0 : 1);
							Settings.sendPrefs();
						}
					}
					Sounds.press();
				} else if (Ui.isB(e)) view = View.MENU;
			}
			case MESSAGE -> {
				if ((Ui.isA(e) || Ui.isB(e)) && message.press()) view = afterMessage;
			}
		}
		return true;
	}

	/** WAIT: rest until a time of day (the screen fades to black while time passes). */
	private void waitUntil(int which) {
		if (which >= 4) {
			view = View.MENU;
			return;
		}
		Sounds.press();
		onClose();
		Hud.fadeThrough(() -> {
			CompoundTag t = new CompoundTag();
			t.putInt("until", which);
			ClientPlayNetworking.send(new Net.Act("wait", t));
		});
	}

	private void save() {
		ClientPlayNetworking.send(new Net.Act("save", new CompoundTag()));
		// The save slot's picture and details (where you are, badges, time) are brought up to date too.
		SlotSnapshot.soon();
		Sounds.jingle("get_key_item");
		say(View.MENU, name() + " saved the game.");
	}

	private void setSpeed(int sp) {
		ClientState.data.textSpeed = sp;
		CompoundTag t = new CompoundTag();
		t.putInt("textSpeed", sp);
		ClientPlayNetworking.send(new Net.Act("options", t));
	}

	@Override
	protected void clickAnywhere() {
		if (view == View.MESSAGE && message.press()) view = afterMessage;
	}

	@Override
	protected void back() {
		switch (view) {
			case MENU -> onClose();
			case PARTY -> {
				if (swapFrom >= 0) swapFrom = -1;
				else view = View.MENU;
			}
			case USE_ON, BAG_ACTION -> view = View.BAG;
			case PARTY_ACTION, SUMMARY -> view = View.PARTY;
			case MESSAGE -> clickAnywhere();
			default -> view = View.MENU;
		}
	}

	// ---------- drawing ----------

	@Override
	protected void drawScreen(GuiGraphics g, float partial) {
		PlayerData d = ClientState.data;
		int m = 6 * s, h = boxHeight();
		switch (view) {
			case MENU, SAVE, WAIT -> drawMenu(g);
			default -> {
			}
		}
		switch (view) {
			case PARTY, PARTY_ACTION, USE_ON -> {
				drawParty(g, d);
				String hint = view == View.USE_ON ? "Use on which POKéMON?" : swapFrom >= 0 ? "Move to where?" : "Choose a POKéMON.";
				TextBox.still(hint).render(g, font, m, height - h - m, width - 2 * m, h, s, ticks, false);
				if (view == View.PARTY_ACTION) {
					Choice c = new Choice(List.of("SUMMARY", "SWITCH", "FOLLOW ME", "CANCEL"));
					c.at = actionAt;
					int w = c.width(font, s);
					choice(g, c, width - w - m, height - h - m - c.height(s) - 4 * s, w, this::partyAction);
					actionAt = c.at;
				}
			}
			case SUMMARY -> drawSummary(g, d);
			case BAG, BAG_ACTION -> {
				drawBag(g, d);
				if (view == View.BAG_ACTION) {
					Choice c = new Choice(List.of("USE", "CANCEL"));
					c.at = actionAt;
					int w = c.width(font, s);
					choice(g, c, width - w - m, height - h - m - c.height(s) - 4 * s, w, this::bagAction);
					actionAt = c.at;
				}
			}
			case CARD -> drawCard(g, d);
			case SAVE -> {
				TextBox.still("Would you like to save the game?").render(g, font, m, height - h - m, width - 2 * m, h, s, ticks, false);
				int w = yesNo.width(font, s);
				choice(g, yesNo, width - w - m, height - h - m - yesNo.height(s) - 4 * s, w, i -> {
					if (i == 0) save();
					else view = View.MENU;
				});
			}
			case OPTION -> drawOptions(g, d);
			case WAIT -> {
				TextBox.still("Wait until when?").render(g, font, m, height - h - m, width - 2 * m, h, s, ticks, false);
				int w = waitChoice.width(font, s);
				choice(g, waitChoice, width - w - m, height - h - m - waitChoice.height(s) - 4 * s, w, this::waitUntil);
			}
			case MESSAGE -> {
				if (afterMessage == View.BAG) drawBag(g, d);
				else if (afterMessage == View.PARTY) drawParty(g, d);
				else drawMenu(g);
				message.render(g, font, m, height - h - m, width - 2 * m, h, s, ticks, true);
			}
			default -> {
			}
		}
	}

	/**
	 * The home screen: a greeting, the apps as round buttons (the picked one grows, with a white ring), and your
	 * party along the bottom with their HP.
	 */
	private void drawMenu(GuiGraphics g) {
		PlayerData d = ClientState.data;
		int m = 8 * s;
		Ui.draw(g, font, Ui.bold("Hello, " + name() + "!"), m + 2 * s, m, Ui.TEXT, s, true);
		Ui.right(g, font, "BADGES " + d.badges.size() + (d.hasDex() ? "   POKéDEX " + d.caught.size() : ""), width - m - 2 * s, m, Ui.MUTED, s);
		int rows = (ITEMS.length + COLS - 1) / COLS;
		int partyH = d.party.isEmpty() ? 0 : 30 * s;
		int top = m + 16 * s, avail = height - top - partyH - 14 * s;
		int cell = Math.min((width - 2 * m) / COLS, Math.max(30 * s, avail / rows));
		int dia = Math.min(cell - 18 * s, 44 * s);
		int gridW = cell * COLS, x0 = (width - gridW) / 2, y0 = top + Math.max(0, (avail - rows * cell) / 2);
		for (int i = 0; i < ITEMS.length; i++) {
			int cx = x0 + (i % COLS) * cell + cell / 2, cy = y0 + (i / COLS) * cell + dia / 2 + 2 * s;
			int idx = i;
			boolean hover = over(cx - cell / 2, cy - dia / 2 - 2 * s, cell, cell);
			if (hover && view == View.MENU) menuAt = i;
			boolean on = i == menuAt && view == View.MENU;
			boolean off = ITEMS[i].equals("POKéDEX") && !d.hasDex() || ITEMS[i].equals("POKéMON") && d.party.isEmpty();
			int r = dia / 2 + (on ? 2 * s : 0);
			int col = off ? 0xFF4A5068 : COLORS[i];
			Device.disc(g, cx, cy + 2 * s, r, r, 0x50000000);
			if (on) Device.disc(g, cx, cy, r + 2 * s, r + 2 * s, 0xFFFFFFFF);
			Device.disc(g, cx, cy, r, r, col);
			Device.disc(g, cx, cy - r / 3, r * 3 / 4, r / 2, Ui.alpha(0xFFFFFFFF, 0.16F));
			int ic = r + r / 4;
			Ui.icon(g, "icon/" + ICONS[i], cx - ic / 2, cy - ic / 2, ic, off ? 0xFF9AA0B4 : 0xFFFFFFFF);
			int ly = cy + r + 4 * s;
			if (on) {
				int lw = Ui.boldWidth(font, ITEMS[i], s) + 10 * s;
				Ui.round(g, cx - lw / 2, ly - 2 * s, lw, 12 * s, Ui.CREAM, 3 * s);
				Ui.draw(g, font, Ui.bold(ITEMS[i]), cx - lw / 2 + 5 * s, ly, Ui.CREAM_INK, s, false);
			} else Ui.centred(g, font, ITEMS[i], cx, ly, off ? Ui.DIM : Ui.TEXT, s, true);
			if (view == View.MENU) hit(cx - cell / 2, cy - dia / 2 - 2 * s, cell, cell, () -> pickMenu(idx));
		}
		// Your party, along the bottom.
		if (!d.party.isEmpty()) {
			int n = d.party.size(), pw = Math.min(54 * s, (width - 2 * m) / 6), px0 = (width - n * pw) / 2, py = height - partyH - 10 * s;
			for (int i = 0; i < n; i++) {
				Mon mon = d.party.get(i);
				int px = px0 + i * pw;
				Ui.round(g, px + 2 * s, py, pw - 4 * s, partyH - 2 * s, mon.fainted() ? 0x802A1A22 : 0x80243050, 3 * s);
				Ui.portrait(g, mon.species.id(), px + 3 * s, py + s, 18 * s);
				Ui.draw(g, font, Ui.ui("Lv" + mon.level), px + 22 * s, py + 4 * s, Ui.GOLD, s, false);
				Ui.bar(g, px + 5 * s, py + partyH - 9 * s, pw - 14 * s, 3 * s, mon.hp / (float) Math.max(1, mon.maxHp()), Ui.hpColor(mon.hp / (float) Math.max(1, mon.maxHp())), s);
			}
		}
		Ui.text(g, font, "Arrows: choose   A: open   B: put it away", m, height - 10 * s, Ui.DIM, s);
	}

	/** One Pokémon as a card: name, level, HP bar and numbers, status and types. */
	public static void card(GuiGraphics g, Font font, Mon mon, int x, int y, int w, int h, boolean on, boolean marked, int s) {
		Ui.round(g, x, y, w, h, on ? Ui.EDGE_LIGHT : Ui.EDGE, s);
		Ui.round(g, x + s, y + s, w - 2 * s, h - 2 * s, mon.fainted() ? 0xEC2A1A22 : on ? 0xEC22305A : Ui.PANEL, s);
		if (marked) Ui.round(g, x + s, y + s, w - 2 * s, h - 2 * s, 0x40F0B43C, s);
		// Its picture on the left, then its details.
		int ph = h - 6 * s;
		Ui.round(g, x + 3 * s, y + 3 * s, ph, ph, on ? 0x40FFF4D8 : 0x30FFFFFF, 2 * s);
		Ui.portrait(g, mon.species.id(), x + 3 * s, y + 3 * s, ph);
		x += ph + 2 * s;
		w -= ph + 2 * s;
		Ui.text(g, font, mon.name() + " " + mon.genderMark(), x + 7 * s, y + 6 * s, Ui.TEXT, s);
		Ui.right(g, font, "Lv" + mon.level, x + w - 7 * s, y + 6 * s, Ui.GOLD, s);
		Ui.hp(g, font, x + 7 * s, y + 17 * s, w - 60 * s, mon.hp, mon.maxHp(), s);
		Ui.right(g, font, mon.hp + "/" + mon.maxHp(), x + w - 7 * s, y + 17 * s, Ui.MUTED, s);
		int cx = x + 7 * s, cy = y + 28 * s;
		String st = mon.fainted() ? "FNT" : mon.status.tag;
		if (!st.isEmpty()) cx += Ui.chip(g, font, st, cx, cy, Ui.statusColor(st), s) + 3 * s;
		cx += Ui.chip(g, font, mon.species.type1().label.toUpperCase(), cx, cy, Ui.typeColor(mon.species.type1()), s) + 3 * s;
		if (mon.species.type2() != null) Ui.chip(g, font, mon.species.type2().label.toUpperCase(), cx, cy, Ui.typeColor(mon.species.type2()), s);
	}

	private void drawParty(GuiGraphics g, PlayerData d) {
		int m = 6 * s, cols = width > 300 * s ? 2 : 1, gap = 4 * s;
		int cw = Math.min(170 * s, (width - 2 * m - gap * (cols - 1)) / cols), ch = 42 * s;
		int total = cols * cw + gap * (cols - 1), x0 = (width - total) / 2, y0 = m;
		for (int i = 0; i < d.party.size(); i++) {
			int x = x0 + (i % cols) * (cw + gap), y = y0 + (i / cols) * (ch + gap);
			int idx = i;
			if (over(x, y, cw, ch) && (view == View.PARTY || view == View.USE_ON)) partyAt = i;
			card(g, font, d.party.get(i), x, y, cw, ch, i == partyAt, i == swapFrom, s);
			if (view == View.PARTY) hit(x, y, cw, ch, () -> pickParty(idx));
			if (view == View.USE_ON) hit(x, y, cw, ch, () -> use(pocketItems().get(Math.min(bagAt, pocketItems().size() - 1)), idx));
		}
	}

	private void drawSummary(GuiGraphics g, PlayerData d) {
		if (d.party.isEmpty()) return;
		Mon mon = d.party.get(Math.min(partyAt, d.party.size() - 1));
		int m = 6 * s, w = Math.min(width - 2 * m, 300 * s), h = Math.min(height - 2 * m, 190 * s), x = (width - w) / 2, y = (height - h) / 2;
		Ui.panel(g, x, y, w, h, s);
		hit(x, y, w, h, () -> view = View.PARTY);
		Species sp = mon.species;
		int l = x + 10 * s, ty = y + 10 * s;
		Ui.text(g, font, mon.name() + " " + mon.genderMark(), l, ty, Ui.TEXT, s);
		Ui.right(g, font, "Lv" + mon.level + "   No." + String.format("%03d", sp.dex()), x + w - 10 * s, ty, Ui.GOLD, s);
		ty += 13 * s;
		int cx = l;
		cx += Ui.chip(g, font, sp.type1().label.toUpperCase(), cx, ty, Ui.typeColor(sp.type1()), s) + 3 * s;
		if (sp.type2() != null) cx += Ui.chip(g, font, sp.type2().label.toUpperCase(), cx, ty, Ui.typeColor(sp.type2()), s) + 3 * s;
		String st = mon.fainted() ? "FNT" : mon.status.tag;
		if (!st.isEmpty()) Ui.chip(g, font, st, cx, ty, Ui.statusColor(st), s);
		if (mon.nickname != null) Ui.right(g, font, sp.name(), x + w - 10 * s, ty + s, Ui.MUTED, s);
		ty += 15 * s;
		int half = (w - 30 * s) / 2;
		Ui.hp(g, font, l, ty, half, mon.hp, mon.maxHp(), s);
		Ui.text(g, font, mon.hp + "/" + mon.maxHp(), l + half + 6 * s, ty, Ui.MUTED, s);
		ty += 11 * s;
		int now = sp.growth().expAt(mon.level), next = mon.level >= 100 ? now : sp.growth().expAt(mon.level + 1);
		Ui.plain(g, font, "EXP", l, ty, Ui.CYAN, s);
		Ui.bar(g, l + Ui.width(font, "EXP", s) + 3 * s, ty + s, half - Ui.width(font, "EXP", s) - 3 * s, 4 * s,
				next <= now ? 1 : (mon.exp - now) / (float) (next - now), Ui.CYAN, s);
		Ui.text(g, font, (next - mon.exp) + " to Lv" + Math.min(100, mon.level + 1), l + half + 6 * s, ty, Ui.MUTED, s);
		ty += 14 * s;
		// Stats on the left, moves on the right.
		String[] names = {"ATTACK", "DEFENSE", "SPCL.ATK", "SPCL.DEF", "SPEED"};
		int[] stats = {mon.attack(), mon.defense(), mon.spAttack(), mon.spDefense(), mon.speed()};
		for (int i = 0; i < 5; i++) {
			Ui.text(g, font, names[i], l, ty + i * 11 * s, Ui.MUTED, s);
			Ui.right(g, font, String.valueOf(stats[i]), l + half - 4 * s, ty + i * 11 * s, Ui.TEXT, s);
		}
		int mxp = l + half + 6 * s, mw = w - (mxp - x) - 10 * s;
		for (int i = 0; i < mon.moves.size(); i++) {
			Move mv = Move.get(mon.moves.get(i));
			int my = ty + i * 14 * s;
			Ui.round(g, mxp, my - 2 * s, mw, 12 * s, Ui.alpha(Ui.typeColor(mv.type()), 0.55F), s);
			Ui.text(g, font, mv.name(), mxp + 4 * s, my, Ui.TEXT, s);
			Ui.right(g, font, mon.pp.get(i) + "/" + mv.pp(), mxp + mw - 4 * s, my, Ui.TEXT, s);
		}
		Ui.text(g, font, "◀ ▶  other POKéMON", l, y + h - 14 * s, Ui.DIM, s);
	}

	/** Your bag's colour (from your clothes), or yellow. */
	private static int bagColour() {
		Outfit o = Outfit.parse(ClientState.data.outfit, "girl".equals(ClientState.data.gender));
		return 0xFF000000 | Outfit.COLOURS[Math.floorMod(o.get("bagc"), Outfit.COLOURS.length)];
	}

	/**
	 * The bag: your backpack on the left, in its colour, its four pockets to pick (the big pocket for ITEMS,
	 * the front pocket for BALLS, the lid for KEY ITEMS, the side pocket for TMs and HMs); what's in the picked
	 * one on the right, with what the item under the cursor does.
	 */
	private void drawBag(GuiGraphics g, PlayerData d) {
		int m = 8 * s;
		int bw = Math.min(width * 2 / 5, 130 * s), bh = Math.min(height - 2 * m - 12 * s, bw * 6 / 5);
		int bx = m + 4 * s, by = m + (height - 2 * m - bh) / 2;
		int col = bagColour(), dark = Ui.darker(col, 0.72F), light = Ui.lighter(col, 0.22F);
		int r = Math.max(4, bw / 7);
		// The straps peeking up behind, the body, the lid with its buckles, the side pocket and the front pocket.
		Device.rounded(g, bx + bw / 4, by - 6 * s, bw / 2, 16 * s, 6 * s, Ui.darker(col, 0.5F));
		Device.rounded(g, bx + bw / 4 + 3 * s, by - 3 * s, bw / 2 - 6 * s, 10 * s, 4 * s, 0xFF0F1626);
		Device.rounded(g, bx + 3 * s, by + 5 * s, bw, bh, r, 0x50000000);
		int[][] pockets = new int[4][];
		// ITEMS: the main body.
		pockets[0] = new int[] {bx, by, bw, bh};
		// KEY ITEMS: the lid.
		int lidH = bh * 30 / 100;
		pockets[2] = new int[] {bx + 2 * s, by, bw - 4 * s, lidH};
		// BALLS: the front pocket.
		int fpW = bw * 62 / 100, fpH = bh * 30 / 100;
		pockets[1] = new int[] {bx + (bw - fpW) / 2, by + bh - fpH - 8 * s, fpW, fpH};
		// TMs and HMs: the side pocket (right).
		int spW = Math.max(10 * s, bw / 6), spH = bh * 36 / 100;
		pockets[3] = new int[] {bx + bw - spW / 2, by + bh / 2 - spH / 4, spW, spH};
		for (int i = 0; i < 4; i++) {
			int[] pk = pockets[i];
			int idx = i;
			if (over(pk[0], pk[1], pk[2], pk[3]) && view == View.BAG) {
				// (the smaller pockets win over the body they sit on)
				if (i != 0 || !overAny(pockets)) pocket = i;
			}
			if (view == View.BAG) hit(pk[0], pk[1], pk[2], pk[3], () -> {
				if (pocket != idx) {
					pocket = idx;
					bagAt = 0;
					bagTop = 0;
				}
			});
		}
		// The body.
		Device.rounded(g, bx, by, bw, bh, r, dark);
		Device.rounded(g, bx, by, bw, bh - 3 * s, r, col);
		g.fill(bx + r, by + lidH + 2 * s, bx + bw - r, by + lidH + 3 * s, Ui.alpha(0xFF000000, 0.2F));
		if (pocket == 0) outline(g, bx, by, bw, bh, r);
		// The lid, with two buckles and a Poké Ball badge.
		int[] lid = pockets[2];
		Device.rounded(g, lid[0], lid[1], lid[2], lid[3], r, light);
		g.fill(lid[0] + r, lid[1] + 2 * s, lid[0] + lid[2] - r, lid[1] + 3 * s, 0x33FFFFFF);
		for (int k = 0; k < 2; k++) {
			int sx = lid[0] + lid[2] * (k == 0 ? 1 : 3) / 4 - 3 * s;
			g.fill(sx, lid[1] + lid[3] - 8 * s, sx + 6 * s, lid[1] + lid[3] + 6 * s, Ui.darker(col, 0.55F));
			Ui.round(g, sx - s, lid[1] + lid[3] - 2 * s, 8 * s, 6 * s, 0xFFE2C46A, s);
		}
		int bc = lid[0] + lid[2] / 2, bcy = lid[1] + lid[3] / 2;
		Device.disc(g, bc, bcy, 6 * s, 6 * s, 0xFF2A2A2E);
		Device.disc(g, bc, bcy, 5 * s, 5 * s, 0xFFF4F4F0);
		for (int yy = -5 * s; yy < 0; yy++) {
			double v = (yy + 0.5) / (5.0 * s);
			int half = (int) Math.round(5 * s * Math.sqrt(Math.max(0, 1 - v * v)));
			g.fill(bc - half, bcy + yy, bc + half, bcy + yy + 1, 0xFFE2423C);
		}
		g.fill(bc - 5 * s, bcy - Math.max(1, s / 2), bc + 5 * s, bcy + Math.max(1, s / 2), 0xFF2A2A2E);
		Device.disc(g, bc, bcy, 2 * s, 2 * s, 0xFFFFFFFF);
		if (pocket == 2) outline(g, lid[0], lid[1], lid[2], lid[3], r);
		// The side pocket.
		int[] sp = pockets[3];
		Device.rounded(g, sp[0], sp[1], sp[2], sp[3], 3 * s, dark);
		Device.rounded(g, sp[0] + s, sp[1] + s, sp[2] - 2 * s, sp[3] / 3, 2 * s, Ui.darker(col, 0.62F));
		if (pocket == 3) outline(g, sp[0], sp[1], sp[2], sp[3], 3 * s);
		// The front pocket, with its zip.
		int[] fp = pockets[1];
		Device.rounded(g, fp[0], fp[1] + 2 * s, fp[2], fp[3], 4 * s, Ui.darker(col, 0.8F));
		Device.rounded(g, fp[0], fp[1], fp[2], fp[3], 4 * s, light);
		g.fill(fp[0] + 4 * s, fp[1] + 4 * s, fp[0] + fp[2] - 4 * s, fp[1] + 5 * s, 0xFF4A4A50);
		Ui.round(g, fp[0] + fp[2] - 10 * s, fp[1] + 3 * s, 4 * s, 5 * s, 0xFFD0D0D4, s);
		if (pocket == 1) outline(g, fp[0], fp[1], fp[2], fp[3], 4 * s);
		// The picked pocket's name, under the bag.
		Ui.centred(g, font, POCKETS[pocket], bx + bw / 2, by + bh + 4 * s, Ui.GOLD, s, true);
		// What's in it, on the right.
		List<String> l = pocketItems();
		int lx = bx + bw + 16 * s, lw = width - lx - m, row = 14 * s, descH = 30 * s;
		int ly = m, rows = Math.max(2, (height - ly - m - descH - 12 * s) / row);
		int n = l.size() + 1;
		if (bagAt < bagTop) bagTop = bagAt;
		if (bagAt >= bagTop + rows) bagTop = bagAt - rows + 1;
		bagTop = Math.max(0, Math.min(bagTop, Math.max(0, n - rows)));
		Ui.round(g, lx, ly, lw, rows * row + 8 * s, 0xE6141B2E, 3 * s);
		for (int k = 0; k < rows && bagTop + k < n; k++) {
			int i = bagTop + k, ty = ly + 4 * s + k * row;
			int idx = i;
			if (over(lx, ty, lw, row) && view == View.BAG) bagAt = i;
			Ui.pill(g, lx + 3 * s, ty, lw - 6 * s, row - 2 * s, i == bagAt, s);
			int ink = i == bagAt ? Ui.CREAM_INK : Ui.TEXT;
			if (i == l.size()) Ui.draw(g, font, Ui.bold("CLOSE BAG"), lx + 10 * s, ty + 3 * s, i == bagAt ? Ui.CREAM_INK : Ui.MUTED, s, false);
			else {
				Item it = Item.find(l.get(i));
				BattleScreen.itemIcon(g, it, lx + 6 * s, ty + s, 10 * s, s);
				Ui.draw(g, font, Ui.bold(it.name()), lx + 20 * s, ty + 3 * s, ink, s, false);
				Ui.draw(g, font, Ui.ui("x" + d.count(it.id())), lx + lw - 10 * s - Ui.width(font, "x" + d.count(it.id()), s), ty + 3 * s,
						i == bagAt ? Ui.CREAM_INK : Ui.MUTED, s, false);
			}
			if (view == View.BAG) hit(lx, ty, lw, row, () -> pickBag(idx));
		}
		if (bagTop > 0) Ui.centred(g, font, "▲", lx + lw / 2, ly - 2 * s, Ui.MUTED, s, false);
		if (bagTop + rows < n) Ui.centred(g, font, "▼", lx + lw / 2, ly + rows * row + 5 * s, Ui.MUTED, s, false);
		if (l.isEmpty()) Ui.text(g, font, "Nothing in this pocket yet.", lx + 8 * s, ly + row + 8 * s, Ui.DIM, s);
		// What the item does.
		int dy = height - m - descH;
		Ui.round(g, lx, dy, lw, descH, 0xF0FBF5E6, 3 * s);
		String desc = bagAt < l.size() ? BattleScreen.describe(l.get(bagAt)) : "Put the bag away.";
		List<String> dl = Ui.wrap(font, desc, (lw - 14 * s) / s);
		for (int i = 0; i < Math.min(2, dl.size()); i++) Ui.plain(g, font, dl.get(i), lx + 7 * s, dy + 6 * s + i * 11 * s, Ui.CREAM_INK, s);
		Ui.text(g, font, "Left/right: pocket   A: use   B: back", m, height - 9 * s, Ui.DIM, s);
	}

	/** Whether the mouse is over one of the bag's small pockets (they sit on its body). */
	private boolean overAny(int[][] pockets) {
		for (int i = 1; i < pockets.length; i++) if (over(pockets[i][0], pockets[i][1], pockets[i][2], pockets[i][3])) return true;
		return false;
	}

	/** A picked pocket: a white line round it. */
	private void outline(GuiGraphics g, int x, int y, int w, int h, int r) {
		int t = Math.max(1, s);
		g.fill(x + r, y - t, x + w - r, y, 0xFFFFFFFF);
		g.fill(x + r, y + h, x + w - r, y + h + t, 0xFFFFFFFF);
		g.fill(x - t, y + r, x, y + h - r, 0xFFFFFFFF);
		g.fill(x + w, y + r, x + w + t, y + h - r, 0xFFFFFFFF);
		g.fill(x + r / 3, y + r / 3 - t, x + r, y + r / 3, 0xFFFFFFFF);
		g.fill(x + w - r, y + r / 3 - t, x + w - r / 3, y + r / 3, 0xFFFFFFFF);
		g.fill(x + r / 3, y + h - r / 3, x + r, y + h - r / 3 + t, 0xFFFFFFFF);
		g.fill(x + w - r, y + h - r / 3, x + w - r / 3, y + h - r / 3 + t, 0xFFFFFFFF);
	}

	/** Your trainer card: your face (from your look), name, money, the Pokédex, time played and the badges. */
	private void drawCard(GuiGraphics g, PlayerData d) {
		int m = 8 * s, w = Math.min(width - 2 * m, 280 * s), h = Math.min(height - 2 * m, 150 * s), x = (width - w) / 2, y = (height - h) / 2;
		boolean girl = "girl".equals(d.gender);
		int accent = girl ? 0xFFE86A9A : 0xFF4A8FE0;
		Device.rounded(g, x + 2 * s, y + 4 * s, w, h, 8 * s, 0x50000000);
		Device.rounded(g, x, y, w, h, 8 * s, 0xFFF6F1E4);
		Device.rounded(g, x, y, w, 22 * s, 8 * s, accent);
		g.fill(x, y + 14 * s, x + w, y + 22 * s, accent);
		hit(x, y, w, h, () -> view = View.MENU);
		Ui.draw(g, font, Ui.bold("TRAINER CARD"), x + 10 * s, y + 7 * s, 0xFFFFFFFF, s, true);
		Ui.right(g, font, "IDNo. " + String.format("%05d", Math.abs(name().hashCode()) % 100000), x + w - 10 * s, y + 7 * s, 0xFFFFFFFF, s);
		// Your face, from your look's head (the front of the head and the hat layer over it).
		int ps = 46 * s, px = x + w - ps - 12 * s, py = y + 30 * s;
		Device.rounded(g, px - 3 * s, py - 3 * s, ps + 6 * s, ps + 6 * s, 5 * s, 0xFFE4DCCB);
		if (minecraft != null && minecraft.player != null) {
			try {
				net.minecraft.resources.Identifier skin = minecraft.player.getSkin().body().texturePath();
				g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, skin, px, py, 8, 8, ps, ps, 8, 8, 64, 64);
				g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, skin, px, py, 40, 8, ps, ps, 8, 8, 64, 64);
			} catch (Exception ignored) {
				// No skin to show.
			}
		}
		int l = x + 12 * s, ty = y + 30 * s, vx = px - 12 * s;
		String[][] rows = {{"NAME", name()}, {"MONEY", Ui.money(d.money)}, {"POKéDEX", d.hasDex() ? "SEEN " + d.seen.size() + "  OWN " + d.caught.size() : "-"},
				{"TIME", String.format("%d:%02d", (int) d.played / 3600, (int) d.played / 60 % 60)}};
		for (String[] r : rows) {
			Ui.draw(g, font, Ui.bold(r[0]), l, ty, 0xFF7A849E, s, false);
			Ui.draw(g, font, Ui.ui(r[1]), vx - Ui.width(font, r[1], s), ty, Ui.CREAM_INK, s, false);
			g.fill(l, ty + 10 * s, vx, ty + 10 * s + 1, 0x22000000);
			ty += 13 * s;
		}
		ty += 4 * s;
		String[] badges = {"BOULDERBADGE", "CASCADEBADGE", "THUNDERBADGE", "RAINBOWBADGE", "SOULBADGE", "MARSHBADGE", "VOLCANOBADGE", "EARTHBADGE"};
		int[] colors = {0xFF9A9A9A, 0xFF4FA6E8, 0xFFF0A63C, 0xFF7CCB5C, 0xFFD95A9E, 0xFFE8C340, 0xFFE8553C, 0xFF6BBF59};
		int bwd = Math.min(16 * s, (w - 24 * s) / 8), bty = Math.min(ty, y + h - bwd - 8 * s);
		for (int i = 0; i < 8; i++) {
			boolean got = d.badges.contains(badges[i]);
			int bx = l + i * (bwd + 3 * s);
			Device.disc(g, bx + bwd / 2, bty + bwd / 2, bwd / 2, bwd / 2, got ? colors[i] : 0xFFD8D0BE);
			if (got) Device.disc(g, bx + bwd / 2 - bwd / 6, bty + bwd / 3, bwd / 6, bwd / 8, 0x88FFFFFF);
		}
	}

	private void drawOptions(GuiGraphics g, PlayerData d) {
		int m = 8 * s, x = m, y = m, w = width - 2 * m;
		Ui.draw(g, font, Ui.bold("OPTION"), x, y, Ui.TEXT, s, true);
		int rh = Math.max(20 * s, Math.min(26 * s, (height - y - 30 * s) / 6)), y0 = y + 14 * s;
		optionRow(g, d, x, y0, w, 0, "TEXT SPEED", new String[] {"SLOW", "MID", "FAST"}, d.textSpeed, i -> setSpeed(i));
		optionRow(g, d, x, y0 + rh, w, 1, "MUSIC", new String[] {"OFF", "LOW", "MID", "HIGH", "MAX"}, Music.level(), Music::setLevel);
		optionRow(g, d, x, y0 + 2 * rh, w, 2, "POKéDEX VOICE", new String[] {"OFF", "ON"}, Settings.voice() ? 1 : 0, i -> Settings.set("voice", i));
		optionRow(g, d, x, y0 + 3 * rh, w, 3, "NIGHTS", new String[] {"DARK", "SOFT", "BRIGHT"}, Settings.nights(), i -> {
			Settings.set("nights", i);
			Night.send();
		});
		optionRow(g, d, x, y0 + 4 * rh, w, 4, "WILD POKéMON", new String[] {"HIDDEN", "SEEN"}, Settings.seen() ? 1 : 0, i -> {
			Settings.set("seen", i);
			Settings.sendPrefs();
		});
		optionRow(g, d, x, y0 + 5 * rh, w, 5, "WALK WITH YOU", new String[] {"OFF", "ON"}, Settings.follower() ? 1 : 0, i -> {
			Settings.set("follow", i);
			Settings.sendPrefs();
		});
		Ui.text(g, font, "Mouse wheel: zoom.  TAB: game speed.  Shift: run.", x, height - 10 * s, Ui.DIM, s);
	}

	private void optionRow(GuiGraphics g, PlayerData d, int l, int y, int w, int row, String label, String[] values, int now,
			java.util.function.IntConsumer pick) {
		boolean on = optionRow == row;
		if (on) Ui.round(g, l - 3 * s, y - 3 * s, w + 6 * s, 20 * s, 0x33F0B43C, 3 * s);
		Ui.draw(g, font, Ui.bold(label), l + 2 * s, y + 3 * s, on ? Ui.TEXT : Ui.MUTED, s, false);
		int bx0 = l + Math.min(w / 3, 96 * s), n = values.length, bw = (l + w - bx0 - (n - 1) * 3 * s) / n;
		for (int i = 0; i < n; i++) {
			int bx = bx0 + i * (bw + 3 * s);
			int idx = i;
			Ui.button(g, font, bx, y, bw, 14 * s, values[i], Ui.BLUE, now == i, s);
			hit(bx, y, bw, 14 * s, () -> {
				optionRow = row;
				pick.accept(idx);
				Sounds.press();
			});
		}
	}

	@SuppressWarnings("unused")
	private static Type none() {
		return null;
	}
}
