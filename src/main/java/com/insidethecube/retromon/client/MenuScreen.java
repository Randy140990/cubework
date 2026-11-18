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

/** The START menu: your Pokémon (and their summaries), the bag, your trainer card, saving and options. */
public class MenuScreen extends GameScreen {
	private enum View {
		MENU, PARTY, PARTY_ACTION, SUMMARY, BAG, BAG_ACTION, USE_ON, CARD, SAVE, WAIT, OPTION, MESSAGE
	}

	/** Which OPTION row the keys change: 0 text speed, 1 music, 2 the Pokédex voice, 3 nights. */
	private int optionRow;
	private static final String[] ITEMS = {"POKéDEX", "POKéMON", "BAG", "MAP", "CLOTHES", "CARD", "WAIT", "SAVE", "OPTION", "EXIT"};
	private static final int[] COLORS = {0xFFE5525C, Ui.GREEN, Ui.AMBER, 0xFF4CB8A0, 0xFFB07CE0, Ui.BLUE, Ui.CYAN, 0xFF8C93A8, Ui.RED};
	private static final String[] POCKETS = {"ITEMS", "BALLS", "KEY ITEMS", "TM & HM"};

	private View view = View.MENU, afterMessage = View.MENU;
	private int menuAt, partyAt, swapFrom = -1, pocket, bagAt, actionAt;
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
				if (minecraft != null) minecraft.setScreen(new PokedexScreen(this));
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
				if (Ui.isUp(e)) menuAt = Math.floorMod(menuAt - 1, ITEMS.length);
				else if (Ui.isDown(e)) menuAt = Math.floorMod(menuAt + 1, ITEMS.length);
				else if (Ui.isA(e)) pickMenu(menuAt);
				else if (Ui.isB(e)) onClose();
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
				if (Ui.isUp(e)) actionAt = Math.floorMod(actionAt - 1, 3);
				else if (Ui.isDown(e)) actionAt = Math.floorMod(actionAt + 1, 3);
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
				} else if (Ui.isUp(e)) bagAt = Math.floorMod(bagAt - 1, n);
				else if (Ui.isDown(e)) bagAt = Math.floorMod(bagAt + 1, n);
				else if (Ui.isA(e)) pickBag(bagAt);
				else if (Ui.isB(e)) view = View.MENU;
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
				if (Ui.isUp(e) || Ui.isDown(e)) optionRow = Math.floorMod(optionRow + (Ui.isUp(e) ? -1 : 1), 4);
				else if (Ui.isLeft(e) || Ui.isRight(e) || Ui.isA(e)) {
					int step = Ui.isLeft(e) ? -1 : 1;
					switch (optionRow) {
						case 0 -> setSpeed(Math.floorMod(d.textSpeed + step, 3));
						case 1 -> Music.setLevel(Math.floorMod(Music.level() + step, 5));
						case 2 -> Settings.set("voice", Settings.voice() ? 0 : 1);
						default -> {
							Settings.set("nights", Math.floorMod(Settings.nights() + step, 3));
							Night.send();
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

	@Override
	public void onClose() {
		if (minecraft != null) minecraft.setScreen(null);
	}

	// ---------- drawing ----------

	@Override
	protected void draw(GuiGraphics g, float partial) {
		PlayerData d = ClientState.data;
		int m = 6 * s, h = boxHeight();
		switch (view) {
			case MENU, SAVE, WAIT, OPTION, CARD -> drawMenu(g);
			default -> {
			}
		}
		switch (view) {
			case PARTY, PARTY_ACTION, USE_ON -> {
				drawParty(g, d);
				String hint = view == View.USE_ON ? "Use on which POKéMON?" : swapFrom >= 0 ? "Move to where?" : "Choose a POKéMON.";
				TextBox.still(hint).render(g, font, m, height - h - m, width - 2 * m, h, s, ticks, false);
				if (view == View.PARTY_ACTION) {
					Choice c = new Choice(List.of("SUMMARY", "SWITCH", "CANCEL"));
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

	private void drawMenu(GuiGraphics g) {
		int m = 6 * s, w = 96 * s, row = 18 * s, h = ITEMS.length * row + 8 * s, x = width - w - m, y = m;
		Ui.panel(g, x, y, w, h, s);
		for (int i = 0; i < ITEMS.length; i++) {
			int ry = y + 4 * s + i * row;
			int idx = i;
			if (over(x, ry, w, row) && view == View.MENU) menuAt = i;
			boolean on = i == menuAt;
			if (on) Ui.round(g, x + 3 * s, ry + s, w - 6 * s, row - 2 * s, Ui.alpha(COLORS[i], 0.35F), s);
			g.fill(x + 6 * s, ry + 5 * s, x + 9 * s, ry + row - 5 * s, COLORS[i]);
			Ui.text(g, font, ITEMS[i], x + 14 * s, ry + 5 * s, on ? Ui.TEXT : Ui.MUTED, s);
			if (view == View.MENU) hit(x, ry, w, row, () -> pickMenu(idx));
		}
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

	private void drawBag(GuiGraphics g, PlayerData d) {
		int m = 6 * s, w = Math.min(width - 2 * m, 240 * s), x = m, y = m;
		// Pockets as tabs.
		int n = POCKETS.length, tw = (w - 2 * s * (n - 1)) / n;
		for (int i = 0; i < n; i++) {
			int tx = x + i * (tw + 2 * s);
			int idx = i;
			Ui.button(g, font, tx, y, tw, 16 * s, POCKETS[i], i == 0 ? Ui.AMBER : i == 1 ? Ui.RED : i == 2 ? Ui.BLUE : 0xFF9A6CD8, i == pocket, s);
			if (view == View.BAG) hit(tx, y, tw, 16 * s, () -> {
				pocket = idx;
				bagAt = 0;
			});
		}
		List<String> l = pocketItems();
		int row = 14 * s, ly = y + 20 * s, lh = (l.size() + 1) * row + 10 * s;
		Ui.panel(g, x, ly, w, lh, s);
		for (int i = 0; i <= l.size(); i++) {
			int ty = ly + 6 * s + i * row;
			int idx = i;
			if (over(x, ty - 2 * s, w, row) && view == View.BAG) bagAt = i;
			if (i == bagAt) Ui.round(g, x + 4 * s, ty - 2 * s, w - 8 * s, row - s, 0x33F0B43C, s);
			if (i == l.size()) Ui.text(g, font, "CANCEL", x + 12 * s, ty + s, Ui.MUTED, s);
			else {
				Item it = Item.find(l.get(i));
				Ui.text(g, font, it.name(), x + 12 * s, ty + s, Ui.TEXT, s);
				Ui.right(g, font, "x" + d.count(it.id()), x + w - 10 * s, ty + s, Ui.MUTED, s);
			}
			if (view == View.BAG) hit(x, ty - 2 * s, w, row, () -> pickBag(idx));
		}
		if (l.isEmpty()) Ui.text(g, font, "Nothing in this pocket yet.", x + 12 * s, ly + lh + 4 * s, Ui.DIM, s);
	}

	private void drawCard(GuiGraphics g, PlayerData d) {
		int m = 6 * s, w = Math.min(width - 120 * s, 230 * s), h = 120 * s, x = m, y = m;
		Ui.tagged(g, x, y, w, h, s, Ui.BLUE);
		hit(x, y, w, h, () -> view = View.MENU);
		int l = x + 12 * s, ty = y + 10 * s;
		Ui.text(g, font, "TRAINER CARD", l, ty, Ui.MUTED, s);
		ty += 14 * s;
		Ui.text(g, font, "NAME", l, ty, Ui.MUTED, s);
		Ui.right(g, font, name(), x + w - 12 * s, ty, Ui.TEXT, s);
		ty += 12 * s;
		Ui.text(g, font, "MONEY", l, ty, Ui.MUTED, s);
		Ui.right(g, font, "¥" + d.money, x + w - 12 * s, ty, Ui.GOLD, s);
		ty += 12 * s;
		Ui.text(g, font, "POKéDEX", l, ty, Ui.MUTED, s);
		Ui.right(g, font, "SEEN " + d.seen.size() + "   OWN " + d.caught.size(), x + w - 12 * s, ty, Ui.TEXT, s);
		ty += 16 * s;
		String[] badges = {"BOULDERBADGE", "CASCADEBADGE", "THUNDERBADGE", "RAINBOWBADGE", "SOULBADGE", "MARSHBADGE", "VOLCANOBADGE", "EARTHBADGE"};
		int[] colors = {0xFF9A9A9A, 0xFF4FA6E8, 0xFFF0A63C, 0xFF7CCB5C, 0xFFD95A9E, 0xFFE8C340, 0xFFE8553C, 0xFF6BBF59};
		int bw = (w - 24 * s) / 8;
		for (int i = 0; i < 8; i++) {
			boolean got = d.badges.contains(badges[i]);
			int bx = l + i * bw;
			Ui.round(g, bx, ty, bw - 3 * s, bw - 3 * s, got ? colors[i] : 0xFF2A3350, s);
			if (got) Ui.round(g, bx + 2 * s, ty + 2 * s, bw - 7 * s, (bw - 7 * s) / 3, 0x55FFFFFF, s);
		}
		Ui.text(g, font, "BADGES " + d.badges.size(), l, ty + bw + 2 * s, Ui.MUTED, s);
	}

	private void drawOptions(GuiGraphics g, PlayerData d) {
		int m = 6 * s, w = Math.min(width - 120 * s, 240 * s), h = 172 * s, x = m, y = m;
		Ui.panel(g, x, y, w, h, s);
		int l = x + 12 * s;
		optionRow(g, d, l, y + 10 * s, w, 0, "TEXT SPEED", new String[] {"SLOW", "MID", "FAST"}, d.textSpeed, i -> setSpeed(i));
		optionRow(g, d, l, y + 46 * s, w, 1, "MUSIC", new String[] {"OFF", "LOW", "MID", "HIGH", "MAX"}, Music.level(), Music::setLevel);
		optionRow(g, d, l, y + 82 * s, w, 2, "POKéDEX VOICE", new String[] {"OFF", "ON"}, Settings.voice() ? 1 : 0, i -> Settings.set("voice", i));
		optionRow(g, d, l, y + 118 * s, w, 3, "NIGHTS", new String[] {"DARK", "SOFT", "BRIGHT"}, Settings.nights(), i -> {
			Settings.set("nights", i);
			Night.send();
		});
		Ui.text(g, font, "Mouse wheel: zoom.  R: game speed.  Shift: run.", l, y + h - 14 * s, Ui.DIM, s);
	}

	private void optionRow(GuiGraphics g, PlayerData d, int l, int y, int w, int row, String label, String[] values, int now,
			java.util.function.IntConsumer pick) {
		Ui.text(g, font, label, l, y, optionRow == row ? Ui.TEXT : Ui.MUTED, s);
		int n = values.length, bw = (w - 24 * s - (n - 1) * 3 * s) / n;
		for (int i = 0; i < n; i++) {
			int bx = l + i * (bw + 3 * s), by = y + 12 * s;
			int idx = i;
			Ui.button(g, font, bx, by, bw, 16 * s, values[i], Ui.BLUE, now == i, s);
			hit(bx, by, bw, 16 * s, () -> {
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
