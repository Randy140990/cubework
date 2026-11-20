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
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.nbt.CompoundTag;

/**
 * The START menu, on your Pokédex: its apps (the Pokédex, your Pokémon and their summaries, the bag, the Town
 * Map, clothes, your trainer card, waiting, saving and options) as round buttons, as the newer games have them.
 */
public class MenuScreen extends DeviceScreen {
	private enum View {
		MENU, PARTY, PARTY_ACTION, SUMMARY, BAG, BAG_ACTION, USE_ON, CARD, SAVE, SAVING, WAIT, OPTION, MESSAGE
	}

	/** Which OPTION row the keys change: 0 text speed, 1 music, 2 the Pokédex voice, 3 nights, 4 wild Pokémon, 5 walking with you, 6 the POKéMON LOOK. */
	private static final String[] ITEMS = {"POKéDEX", "POKéMON", "BAG", "MAP", "CLOTHES", "CARD", "WAIT", "SAVE", "OPTION", "EXIT"};
	/** Each app's picture (textures/gui/icon). */
	private static final String[] ICONS = {"dex", "ball", "backpack", "map", "cap", "card", "clock", "save", "cog", "close"};
	/** The apps in rows of this many. */
	private static final int COLS = 5;
	/** One per menu item (WAIT's was missing, which made opening the menu crash). */
	private static final int[] COLORS = {0xFFE5525C, Ui.GREEN, Ui.AMBER, 0xFF4CB8A0, 0xFFB07CE0, Ui.BLUE, 0xFF7C8CF0, Ui.CYAN, 0xFF8C93A8, Ui.RED};
	private static final String[] POCKETS = {"ITEMS", "BALLS", "KEY ITEMS", "TM & HM"};

	private View view = View.MENU, afterMessage = View.MENU;
	private final OptionsPage options = new OptionsPage(() -> {
		if (minecraft != null) minecraft.setScreen(new KeyMapScreen(this));
	});
	/** The menu's rows: 0 your partner's emblem, 1 the five tiles, 2 the five pills. */
	private int menuRow = 1;
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
		if (view == View.SAVING && ticks - savingAt >= 28) {
			say(View.MENU, "**{N}** saved the game.");
			closeAfter = true;
		}
	}

	/** When "Saving..." came up (ticks). */
	private int savingAt;
	/** After this message, back to the world (saved). */
	private boolean closeAfter;

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
				yesNo = new Choice(List.of("SAVE YOUR PROGRESS", "BACK TO YOUR ADVENTURE"));
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

	/** Your partner's emblem chosen: its summary. */
	private void openLead() {
		if (ClientState.data.party.isEmpty()) return;
		Sounds.press();
		partyAt = 0;
		summaryPage = 0;
		view = View.SUMMARY;
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

	@Override
	protected String backLabel() {
		return view == View.MENU ? "CLOSE" : "BACK";
	}

	// ---------- keys ----------

	@Override
	protected boolean key(KeyEvent e) {
		PlayerData d = ClientState.data;
		switch (view) {
			case MENU -> {
				// Your partner on top, the five tiles, the five pills: left and right along a row, up and down between them.
				if (e.key() == GLFW.GLFW_KEY_E) {
					onClose();
					return true;
				}
				int[] rc = menuPlace(menuAt);
				int[] row = rc[0] == 0 ? MAIN_ROW : SIDE_ROW;
				if (menuRow > 0 && Ui.isLeft(e)) menuAt = row[Math.floorMod(rc[1] - 1, row.length)];
				else if (menuRow > 0 && Ui.isRight(e)) menuAt = row[Math.floorMod(rc[1] + 1, row.length)];
				else if (Ui.isUp(e)) {
					if (menuRow == 2) {
						menuRow = 1;
						menuAt = MAIN_ROW[rc[1]];
					} else if (menuRow == 1 && !d.party.isEmpty()) menuRow = 0;
				} else if (Ui.isDown(e)) {
					if (menuRow == 0) {
						menuRow = 1;
						menuAt = MAIN_ROW[2];
					} else if (menuRow == 1) {
						menuRow = 2;
						menuAt = SIDE_ROW[rc[1]];
					}
				} else if (Ui.isA(e)) {
					if (menuRow == 0) openLead();
					else pickMenu(menuAt);
				} else if (Ui.isB(e)) onClose();
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
				// As the viewer: TAB or left and right for the page, up and down for the next of your team.
				if (e.key() == GLFW.GLFW_KEY_TAB || Ui.isRight(e)) summaryPage = (summaryPage + 1) % 3;
				else if (Ui.isLeft(e)) summaryPage = (summaryPage + 2) % 3;
				else if ((Ui.isUp(e) || Ui.isDown(e)) && summaryPage == 2) {
					Mon mm = d.party.get(Math.min(partyAt, d.party.size() - 1));
					moveAt = Math.floorMod(moveAt + (Ui.isUp(e) ? -1 : 1), Math.max(1, mm.moves.size()));
				} else if (Ui.isUp(e)) partyAt = Math.floorMod(partyAt - 1, d.party.size());
				else if (Ui.isDown(e)) partyAt = Math.floorMod(partyAt + 1, d.party.size());
				else if (Ui.isB(e)) view = View.PARTY;
			}
			case BAG -> {
				int n = pocketItems().size();
				if (e.key() == GLFW.GLFW_KEY_TAB) {
					pocket = Math.floorMod(pocket + 1, POCKETS.length);
					bagAt = 0;
					bagTop = 0;
					Sounds.select();
				} else if (n > 0 && (Ui.isLeft(e) || Ui.isRight(e) || Ui.isUp(e) || Ui.isDown(e))) {
					int was = bagAt;
					if (Ui.isLeft(e)) bagAt = Math.floorMod(bagAt - 1, n);
					else if (Ui.isRight(e)) bagAt = Math.floorMod(bagAt + 1, n);
					else if (Ui.isUp(e)) bagAt = bagAt - 4 >= 0 ? bagAt - 4 : bagAt;
					else bagAt = bagAt + 4 < n ? bagAt + 4 : bagAt;
					if (was != bagAt) Sounds.select();
				} else if (Ui.isA(e) && n > 0) pickBag(bagAt);
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
				if (Ui.isUp(e) || Ui.isDown(e)) {
					yesNo.at ^= 1;
					Sounds.select();
				} else if (Ui.isB(e) || (Ui.isA(e) && yesNo.at == 1)) view = View.MENU;
				else if (Ui.isA(e)) save();
			}
			case SAVING -> {
			}
			case OPTION -> {
				if (Ui.isB(e)) view = View.MENU;
				else options.key(e);
			}
			case MESSAGE -> {
				if ((Ui.isA(e) || Ui.isB(e)) && message.press()) messageDone();
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
		String[] times = {"morning", "midday", "evening", "night"};
		Hud.fadeThrough(() -> {
			CompoundTag t = new CompoundTag();
			t.putInt("until", which);
			ClientPlayNetworking.send(new Net.Act("wait", t));
		}, "Time passes...", () -> {
			// Then the viewer's word on it.
			net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
			if (mc.screen == null) mc.setScreen(new DialogueScreen(List.of("It's " + times[which] + " now."), "talk", -1, new CompoundTag()));
		});
	}

	private void save() {
		ClientPlayNetworking.send(new Net.Act("save", new CompoundTag()));
		// The save slot's picture and details (where you are, badges, time) are brought up to date too.
		SlotSnapshot.soon();
		Sounds.jingle("get_key_item");
		// The viewer's: "Saving... Don't turn off the power." with the disc spinning, then it's saved.
		view = View.SAVING;
		savingAt = ticks;
	}

	/** The world behind, washed pale (the viewer's g-blur). */
	private void wash(GuiGraphics g) {
		g.fill(0, 0, width, height, 0x38ECF4FF);
	}

	/** Your trainer number (five figures, from your name, the same on the card and the save). */
	private static String idNo() {
		return String.format("%05d", Math.abs(name().hashCode()) % 100000);
	}

	private static String playTime(PlayerData d) {
		return String.format("%d:%02d", (int) d.played / 3600, (int) d.played / 60 % 60);
	}

	/** When you started, as the viewer writes it (19/11/2026). */
	private static String started(PlayerData d) {
		if (d.since <= 0) return "-";
		return new java.text.SimpleDateFormat("dd/MM/yyyy").format(new java.util.Date(d.since));
	}

	/** Your partner's emblem (the four drawn ones, else its portrait in its colour with a white edge). */
	private static void emblem(GuiGraphics g, String lead, float ex, float ey, float ew) {
		if (java.util.Set.of("pikachu", "charmander", "bulbasaur", "squirtle").contains(lead))
			V.img(g, V.tex("emblem_" + lead), ex, ey - (ew - ew * 0.9F) / 2, ew, ew, 256, 256, 0xFFFFFFFF);
		else {
			Species sp = Species.ALL.get(lead);
			int col = sp == null ? 0xFFF6B42A : Ui.typeColor(sp.type1()) | 0xFF000000;
			float e = ew / 58;
			for (int i = 0; i < 8; i++) {
				double an = i * Math.PI / 4;
				V.img(g, Look.portrait(lead), ex + (float) Math.cos(an) * e, ey + (float) Math.sin(an) * e, ew, ew, 128, 128, 0xFFFFFFFF);
			}
			V.img(g, Look.portrait(lead), ex, ey, ew, ew, 128, 128, Ui.mix(col, 0xFFFFFFFF, 0.15F));
		}
	}

	/**
	 * Saving, as the UI Viewer's (Let's Go's): you on the left on a swirl of sky; your card on the right (name, ID,
	 * POKéDEX, money, time, when you started, and your team's faces); two big buttons under it, SAVE YOUR PROGRESS in
	 * yellow and BACK TO YOUR ADVENTURE; then a moment while it writes, the purple save picture spinning.
	 */
	private void drawSave(GuiGraphics g, PlayerData d, float partial) {
		float secs = (ticks + partial) / 20F;
		Scenes.bg2(g, "save");
		Scenes.stand(g, secs, Scenes.you(d.outfit, "girl".equals(d.gender)), 2.1, 0, true);
		// The card: details on the left, your team's faces on the right (two across).
		float x = 560, y = 50, w = 660;
		String[][] rows = {{"Name", name()}, {"ID No.", idNo()}, {"Pokédex", (d.hasDex() ? d.caught.size() : 0) + " POKéMON"}, {"Money", Ui.money(d.money)},
				{"Play time", playTime(d)}, {"Started", started(d)}};
		float rowH = 21 * 1.36F + 4 + 2, gap = 9;
		float listH = rows.length * rowH + (rows.length - 1) * gap;
		int n = Math.min(6, d.party.size());
		float face = (210 - 10) / 2F;
		float facesH = n == 0 ? 0 : ((n + 1) / 2) * face + ((n + 1) / 2 - 1) * 10;
		float h = 22 * 2 + Math.max(listH, facesH);
		V.panel(g, x, y, w, h);
		float lx = x + 30, lw = w - 60 - 210 - 20;
		float labW = 0;
		for (String[] r : rows) labW = Math.max(labW, V.labelWidth(Lang.t(r[0])));
		for (int i = 0; i < rows.length; i++) {
			float ry = y + 22 + i * (rowH + gap);
			V.label(g, Lang.t(rows[i][0]), lx, V.capsTop(ry + rowH / 2 - 1, 13));
			V.right(g, rows[i][1], lx + lw, ry + 2, 21, V.INK, true);
			V.rr(g, lx + labW + 24, ry + rowH - 2, lw - labW - 24, 2, 0, 0xFFEEF1F8);
		}
		float fx = x + w - 30 - 210;
		for (int i = 0; i < n; i++) {
			Mon m = d.party.get(i);
			float cx = fx + (i % 2) * (face + 10) + face / 2, cy = y + 22 + (i / 2) * (face + 10) + face / 2;
			V.shadow(g, cx - face / 2, cy - face / 2, face, face, face / 2, 0.9F);
			V.ellipse(g, cx, cy, face / 2 + 3, face / 2 + 3, 0xFFFFFFFF);
			int tc = Ui.typeColor(m.species.type1()) | 0xFF000000;
			V.rrv(g, cx - face / 2, cy - face / 2, face, face, face / 2, 0xFFFFFFFF, Ui.mix(tc, 0xFFFFFFFF, 0.7F));
			V.img(g, Look.portrait(m.species.id()), cx - 42, cy - 42, 84, 84, 128, 128, 0xFFFFFFFF);
		}
		// The two buttons.
		boolean saving = view == View.SAVING || closeAfter;
		float bx = x + 40, bw = w - 80, by = y + h + 28;
		String[] labels = {"SAVE YOUR PROGRESS", "BACK TO YOUR ADVENTURE"};
		int[][] cols = {{0xFFFFEC8A, 0xFFFFC93C}, {0xFFFFFFFF, 0xFFEEF2FB}};
		float fade = saving ? 0.4F : 1;
		for (int i = 0; i < 2; i++) {
			float yy = by + i * (62 + 18);
			boolean on = !saving && yesNo != null && yesNo.at == i;
			if (!saving && vover(bx, yy, bw, 62) && yesNo != null) yesNo.at = i;
			V.shadow(g, bx, yy, bw, 62, 31, 0.9F * fade);
			if (on) V.pill(g, bx - 7, yy - 7, bw + 14, 62 + 14, 0xFFE5525C);
			V.pill(g, bx - 3, yy - 3, bw + 6, 62 + 6, Ui.alpha(0xFFFFFFFF, fade));
			V.rrv(g, bx, yy, bw, 62, 31, Ui.alpha(cols[i][0], fade), Ui.alpha(cols[i][1], fade));
			V.titleCentred(g, Lang.t(labels[i]), bx + bw / 2, V.capsTop(yy + 31, 24), 24, Ui.alpha(V.INK, fade));
			if (on) V.text(g, "▶", bx - 30 - 3, V.capsTop(yy + 31, 26), 26, 0xFFE5525C, true);
			int idx = i;
			if (view == View.SAVE) vhit(bx, yy, bw, 62, () -> {
				yesNo.at = idx;
				if (idx == 0) save();
				else view = View.MENU;
			});
		}
		if (view == View.SAVING) {
			String t = Lang.t("Saving... don't switch off.");
			float tw = 44 + 14 + V.width(t, 20, true), sx = 600 + (580 - tw) / 2, sy = 656 + 22;
			g.pose().pushMatrix();
			float cx = V.x(sx + 22), cy = V.y(sy);
			g.pose().translate(cx, cy);
			g.pose().rotate(secs * (float) Math.PI * 2);
			g.pose().translate(-cx, -cy);
			V.icon(g, "picto_save_white", sx, sy - 22, 44, 0xFF8F60DE);
			g.pose().popMatrix();
			V.text(g, t, sx + 44 + 14, V.capsTop(sy, 20), 20, V.INK, true);
		}
		if (view == View.SAVE) V.hints(g, "A", "Choose", "B", "Back");
	}

	private void setSpeed(int sp) {
		ClientState.data.textSpeed = sp;
		CompoundTag t = new CompoundTag();
		t.putInt("textSpeed", sp);
		ClientPlayNetworking.send(new Net.Act("options", t));
	}

	@Override
	protected void clickAnywhere() {
		if (view == View.MESSAGE && message.press()) messageDone();
	}

	private void messageDone() {
		view = afterMessage;
		if (closeAfter) {
			closeAfter = false;
			onClose();
		}
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
			case MENU -> drawMenu(g);
			default -> {
			}
		}
		switch (view) {
			case PARTY, PARTY_ACTION, USE_ON -> {
				drawParty(g, d);
				// Moving one, or using an item: a word at the top right (the cards fill the rest).
				if (view == View.USE_ON || swapFrom >= 0) {
					String hint = view == View.USE_ON ? "Use on which POKéMON?" : "Move to where?";
					float hw = V.width(hint, 19, true) + 44;
					V.panel(g, V.W - 30 - hw, 30, hw, 46, 23, true);
					V.text(g, hint, V.W - 30 - hw + 22, 41, 19, V.TEXT, true);
				}
				if (view == View.PARTY_ACTION) {
					Choice c = new Choice(List.of("SUMMARY", "SWITCH", "FOLLOW ME", "CANCEL"));
					c.at = actionAt;
					vchoice(g, c, 950, V.H - 30 - V.choiceHeight(4), 290, this::partyAction);
					V.hints(g, "A", "Choose", "B", "Back");
					actionAt = c.at;
				}
			}
			case SUMMARY -> drawSummary(g, d);
			case BAG, BAG_ACTION -> {
				drawBag(g, d);
				if (view == View.BAG_ACTION) {
					Choice c = new Choice(List.of("USE", "CANCEL"));
					c.at = actionAt;
					vchoice(g, c, 940, 300, 260, this::bagAction);
					V.hints(g, "A", "Choose", "B", "Back");
					actionAt = c.at;
				}
			}
			case CARD -> drawCard(g, d);
			case SAVE, SAVING -> drawSave(g, d, partial);
			case OPTION -> options.draw(g, this::vhit, mx, my);
			case WAIT -> {
				wash(g);
				TextBox.still("Wait until when?").renderV(g, font, 140, 26, 1000, ticks, partial, false);
				vchoice(g, waitChoice, V.W - 140 - 300, V.H - 26 - 150 - V.choiceHeight(waitChoice.options.size()), 300, this::waitUntil);
			}
			case MESSAGE -> {
				if (afterMessage == View.BAG) drawBag(g, d);
				else if (afterMessage == View.PARTY) drawParty(g, d);
				else if (closeAfter) drawSave(g, d, partial);
				else drawMenu(g);
				message.renderV(g, font, 140, 26, 1000, ticks, partial, true);
			}
			default -> {
			}
		}
	}

	/** What each does, in a line (the UI Viewer's words). */
	private static final String[] LINES = {"Every POKéMON you've seen and caught.", "Your team: check them, switch them, pick who follows you.", "Your items, in four pockets.",
			"The whole region, and where you are in it.", "Change your clothes, hat and bag.", "Your trainer card: your name, money and badges.",
			"Let the time pass: morning, midday, evening or night.", "Save your adventure so far.", "Text speed, music, nights and more.", "Close the menu."};
	/** How each shows: POKéMON is PARTY, as Let's Go has it. */
	private static final String[] SHOWN = {"POKéDEX", "PARTY", "BAG", "MAP", "CLOTHES", "CARD", "WAIT", "SAVE", "OPTION", "EXIT"};
	/** The tiles' white pictures (the viewer's PICTO) and the pills' icons. */
	private static final String[] PICTOS = {"picto_dex_white", "picto_ball_white", "picto_bag_white", "picto_map_white", null, null, null, "picto_save_white", null, null};
	private static final String[] TILE_ICONS = {"icon_dex", "icon_ball", "icon_bag", "icon_map", "icon_cap", "icon_card", "icon_clock", "icon_save", "icon_cog", "icon_close"};

	/** The menu's rows (indexes into ITEMS): the five tiles in Let's Go's order (POKéDEX, BAG, PARTY, MAP, SAVE), then the five pills. */
	private static final int[] MAIN_ROW = {0, 2, 1, 3, 7}, SIDE_ROW = {4, 5, 6, 8, 9};
	/** Each tile's colours (light, middle, deep), by its place in the row: red, orange, green, blue, purple. */
	private static final int[][] MAIN_COLS = {{0xFFFF8C8C, 0xFFE5484F, 0xFFB92F3A}, {0xFFFFC566, 0xFFF59A2A, 0xFFD2731A}, {0xFF7FE3A6, 0xFF3DB873, 0xFF228A52},
			{0xFF7CC2FA, 0xFF3C8DE4, 0xFF2763B8}, {0xFFC9A8F7, 0xFF8F60DE, 0xFF6740B6}};

	/** Which row an item's in (0 the tiles, 1 the pills) and where along it. */
	private static int[] menuPlace(int item) {
		for (int i = 0; i < MAIN_ROW.length; i++) if (MAIN_ROW[i] == item) return new int[] {0, i};
		for (int i = 0; i < SIDE_ROW.length; i++) if (SIDE_ROW[i] == item) return new int[] {1, i};
		return new int[] {0, 0};
	}

	/** Words with a thick white edge (the viewer's -webkit-text-stroke: 5px #fff), in Fredoka. */
	private static void stroked(GuiGraphics g, String t, float cx, float y, float size, int ink) {
		float w = V.titleTrackedWidth(t, size, 1), x = cx - w / 2;
		for (int i = 0; i < 12; i++) {
			double an = i * Math.PI / 6;
			V.titleTracked(g, t, x + (float) Math.cos(an) * 2.5F, y + (float) Math.sin(an) * 2.5F, size, 0xFFFFFFFF, 1);
		}
		V.titleTracked(g, t, x, y, size, ink, 1);
	}

	/**
	 * The menu, as the UI Viewer's (Let's Go's player menu, signed off 20 Nov): the world behind washed pale; your
	 * partner's emblem at the top (the shape of its head in its own colour); a row of five rounded tiles, each its own
	 * colour with a white picture (the one you're on lifts and glows in its colour, a big white arrow points at it,
	 * a line in its colour runs under its name); the smaller things as white pills underneath; what it does.
	 */
	private void drawMenu(GuiGraphics g) {
		PlayerData d = ClientState.data;
		float secs = (ticks + Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true)) / 20F;
		// The world washed pale (the viewer's blur over it), whiter towards the foot.
		g.fill(0, 0, width, height, 0x38ECF4FF);
		int top = Math.round(V.y(720 * 0.3F)), mid = Math.round(V.y(720 * 0.62F));
		g.fillGradient(0, top, width, mid, 0x00FFFFFF, 0x8CFFFFFF);
		g.fillGradient(0, mid, width, height, 0x8CFFFFFF, 0xCCFFFFFF);
		boolean menu = view == View.MENU;
		// Your partner's emblem, on its own.
		String lead = d.party.isEmpty() ? null : d.party.get(0).species.id();
		if (lead != null) {
			float ew = 230, ex = 525, ey = 58;
			if (menu && vover(ex, ey, ew, ew * 0.9F)) menuRow = 0;
			boolean on = menu && menuRow == 0;
			g.pose().pushMatrix();
			if (on) {
				float cx0 = V.x(640), cy0 = V.y(ey + ew * 0.45F);
				g.pose().translate(cx0, cy0 - V.s(6));
				g.pose().scale(1.05F, 1.05F);
				g.pose().translate(-cx0, -cy0);
			}
			if (java.util.Set.of("pikachu", "charmander", "bulbasaur", "squirtle").contains(lead))
				V.img(g, V.tex("emblem_" + lead), ex, ey - (ew - ew * 0.9F) / 2, ew, ew, 256, 256, 0xFFFFFFFF);
			else {
				Species sp = Species.ALL.get(lead);
				int col = sp == null ? 0xFFF6B42A : Ui.typeColor(sp.type1()) | 0xFF000000;
				for (int i = 0; i < 8; i++) {
					double an = i * Math.PI / 4;
					V.img(g, Look.portrait(lead), ex + (float) Math.cos(an) * 4, ey + (float) Math.sin(an) * 4, ew, ew, 128, 128, 0xFFFFFFFF);
				}
				V.img(g, Look.portrait(lead), ex, ey, ew, ew, 128, 128, Ui.mix(col, 0xFFFFFFFF, 0.15F));
			}
			g.pose().popMatrix();
			if (menu) vhit(ex, ey, ew, ew * 0.9F, () -> {
				menuRow = 0;
				openLead();
			});
		}
		// The five tiles.
		for (int c = 0; c < MAIN_ROW.length; c++) {
			int i = MAIN_ROW[c];
			int[] col = MAIN_COLS[c];
			float cx = 640 + (c - 2) * 172, tx = cx - 70, ty = 352, sq = 124;
			boolean off = ITEMS[i].equals("POKéDEX") && !d.hasDex() || ITEMS[i].equals("POKéMON") && d.party.isEmpty();
			if (menu && vover(tx, ty, 140, sq + 10 + 32)) {
				menuRow = 1;
				menuAt = i;
			}
			boolean on = menu && menuRow == 1 && i == menuAt;
			float lift = on ? -10 : 0, sc = on ? 1.08F : 1, sx = cx - sq / 2, sy = ty + lift;
			g.pose().pushMatrix();
			float pcx = V.x(cx), pcy = V.y(sy + sq / 2);
			g.pose().translate(pcx, pcy);
			g.pose().scale(sc, sc);
			g.pose().translate(-pcx, -pcy);
			if (on) {
				V.rr(g, sx - 16, sy - 16, sq + 32, sq + 32, 50, Ui.alpha(col[0], 0.45F));
				V.rr(g, sx - 8, sy - 8, sq + 16, sq + 16, 42, Ui.alpha(0xFFFFFFFF, 0.9F));
			}
			V.rr(g, sx, sy + 7, sq, sq, 34, 0x2E1E2854);
			V.shadow(g, sx, sy, sq, sq, 34, 0.9F);
			V.rr(g, sx, sy, sq, sq, 34, 0xFFFFFFFF);
			int a = on ? 0xFF : 0xEB;
			V.rrStops(g, sx + 4, sy + 4, sq - 8, sq - 8, 30, new float[] {0, 0.58F, 1}, new int[] {Ui.alpha(col[0], a / 255F), Ui.alpha(col[1], a / 255F), Ui.alpha(col[2], a / 255F)});
			V.rrv(g, sx + 4 + sq * 0.08F, sy + 4 + sq * 0.05F, sq - 8 - sq * 0.16F, sq * 0.4F, 26, 0x73FFFFFF, 0x00FFFFFF);
			String pic = PICTOS[i] != null ? PICTOS[i] : TILE_ICONS[i];
			V.icon(g, pic, cx - 37, sy + sq / 2 - 37 + 3, 74, 0x24000000);
			V.icon(g, pic, cx - 37, sy + sq / 2 - 37, 74, off ? 0x99FFFFFF : 0xFFFFFFFF);
			g.pose().popMatrix();
			// Its name with a thick white edge, lifted when on, and a line in its colour under it.
			float ny = ty + sq + 10 + (on ? -4 : 0);
			stroked(g, Lang.t(SHOWN[i]), cx, ny + 4, 21, off ? V.DIM : V.INK);
			if (on) V.pill(g, cx - 48, ny + 31.5F - 6 + 2, 96, 6, 0x1F000000);
			if (on) V.pill(g, cx - 48, ny + 31.5F - 6, 96, 6, col[1]);
			// The big white arrow pointing at it from the side.
			if (on) {
				float ax = tx - 26 + 20, ay = ty + 28 + 24;
				V.tri(g, ax + 1, ay + 3, 46, 0, 0x591E2854);
				V.tri(g, ax, ay, 46, 0, 0xFFFFFFFF);
				V.tri(g, ax - 1, ay, 24, 0, 0xFFDDE3EE);
			}
			int idx = i;
			if (menu) vhit(tx, ty, 140, sq + 10 + 32, () -> pickMenu(idx));
		}
		// The five pills.
		for (int c = 0; c < SIDE_ROW.length; c++) {
			int i = SIDE_ROW[c];
			float w = 190, h = 54, x = 640 - (5 * w + 4 * 14) / 2 + c * (w + 14), y = 560;
			if (menu && vover(x, y, w, h)) {
				menuRow = 2;
				menuAt = i;
			}
			boolean on = menu && menuRow == 2 && i == menuAt;
			if (on) {
				y -= 3;
				V.pill(g, x - 6, y - 6, w + 12, h + 12, 0xFF41BF84);
				V.pill(g, x - 3, y - 3, w + 6, h + 6, 0xFFFFFFFF);
				V.sel(g, x, y, w, h, h / 2);
			} else {
				V.pill(g, x - 2, y - 2, w + 4, h + 4, 0xB3FFFFFF);
				V.shadow(g, x, y, w, h, h / 2, 0.6F);
				V.pill(g, x, y, w, h, 0xFFFFFFFF);
			}
			V.ellipse(g, x + 7 + 20, y + h / 2, 20, 20, on ? 0xFFFFFFFF : 0xFFF1F4FB);
			V.icon(g, TILE_ICONS[i], x + 7 + 5, y + h / 2 - 15, 30);
			V.title(g, Lang.t(SHOWN[i]), x + 7 + 40 + 12, V.capsTop(y + h / 2, 19), 19, V.INK);
			int idx = i;
			if (menu) vhit(x, y, w, h, () -> pickMenu(idx));
		}
		String line = menuRow == 0 && lead != null ? Lang.t("See how {0} is doing.", d.party.get(0).name()) : Lang.t(LINES[Math.max(0, Math.min(LINES.length - 1, menuAt))]);
		V.centred(g, line, 640, V.capsTop(640 + 14, 19), 19, V.INK, false);
		if (menu) V.hints(g, "A", "Open", "B", "Close");
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

	/**
	 * Your team as the UI Viewer's (Let's Go's party cards, signed off 20 Nov): six cards, three across, each in its
	 * own type's colour, its picture bobbing (the one you're on bouncing), name, level, types, status, HP on a white
	 * strip; the one you're on lifted with a sunny ring and a red arrow; FOLLOWING YOU on your partner.
	 */
	private void drawParty(GuiGraphics g, PlayerData d) {
		Scenes.bg2(g, "party");
		V.header(g, "picto_ball_green", "POKéMON", null);
		float secs = (ticks + Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true)) / 20F;
		float cw = (1180 - 2 * 28) / 3F, ch = 250;
		for (int i = 0; i < d.party.size(); i++) {
			Mon mon = d.party.get(i);
			float x = 50 + (i % 3) * (cw + 28), y = 104 + (i / 3) * (ch + 24);
			int idx = i;
			if (vover(x, y, cw, ch) && (view == View.PARTY || view == View.USE_ON)) partyAt = i;
			boolean on = i == partyAt, fnt = mon.fainted();
			if (on) y -= 6;
			int tc = Ui.typeColor(mon.species.type1()) | 0xFF000000;
			if (on) V.rr(g, x - 5, y - 5, cw + 10, ch + 10, 33, 0xFFFFC93C);
			V.shadow(g, x, y, cw, ch, 28, on ? 1.3F : 0.9F);
			V.rr(g, x, y, cw, ch, 28, 0xFFFFFFFF);
			int c1 = Ui.mix(tc, 0xFFFFFFFF, 0.78F), c2 = Ui.mix(tc, 0xFFFFFFFF, 0.55F);
			if (fnt) {
				c1 = Ui.mix(c1, 0xFFD8D8D8, 0.7F);
				c2 = Ui.mix(c2, 0xFFC8C8C8, 0.7F);
			}
			V.rrStops(g, x + 4, y + 4, cw - 8, ch - 8, 24, new float[] {0, 0.55F, 1}, new int[] {0xFFFFFFFF, c1, c2});
			if (i == swapFrom) V.ring(g, x, y, cw, ch, 28, 4, V.BLUE);
			if (on) {
				Component ar = V.raw("▶", true);
				V.stroked(g, ar, x - 26, y + 96, 30, V.RED, 0xFFFFFFFF, 6);
			}
			// Its picture, bobbing (the one you're on bouncing).
			float bob = on ? -3 - 5 * (float) Math.sin(secs * Math.PI * 2) : -1.5F + 1.5F * (float) Math.cos(secs * Math.PI * 2 / (2.4F + i * 0.3F));
			V.img(g, Look.portrait(mon.species.id()), x + 8 + 5, y + 4 + 5 + bob, 160, 160, 128, 128, fnt ? 0x99FFFFFF : 0xFFFFFFFF);
			float tx = x + 4 + 168, ty = y + 4 + 14 + 6;
			V.title(g, mon.name(), tx, V.capsTop(ty + 14, 25), 25, V.INK);
			String gm = mon.genderMark();
			if (gm != null && !gm.isEmpty()) V.text(g, gm, tx + V.titleWidth(mon.name(), 25) + 6, V.capsTop(ty + 14, 22), 22, gm.contains("\u2640") ? 0xFFFF8FB1 : 0xFF7FB8FF, true);
			V.text(g, "Lv. " + mon.level, tx, V.capsTop(ty + 28 + 8 + 15, 20), 20, V.INK, true);
			float cx = tx, chy = ty + 28 + 8 + 30 + 8;
			cx += V.type(g, mon.species.type1().label, tc, cx, chy, 0, 13.5F) + 6;
			if (mon.species.type2() != null) V.type(g, mon.species.type2().label, Ui.typeColor(mon.species.type2()) | 0xFF000000, cx, chy, 0, 13.5F);
			float sx = tx, sy = chy + 24 + 8;
			String st = fnt ? "FNT" : mon.status.tag;
			if (!st.isEmpty()) {
				float sw = V.width(st, 13, true) + 16;
				V.rr(g, sx, sy, sw, 21, 7, Ui.statusColor(st));
				V.text(g, st, sx + 8, V.capsTop(sy + 10.5F, 13), 13, 0xFFFFFFFF, true);
				sx += sw + 6;
			}
			if (i == d.follow) {
				String fy = Lang.t("FOLLOWING YOU");
				float fw = V.trackedWidth(fy, 12, 0.8F) + 20;
				V.rrv(g, sx, sy + 1, fw, 19, 9.5F, 0xFF6BE0A6, 0xFF2FA86C);
				V.tracked(g, fy, sx + 10, V.capsTop(sy + 10.5F, 12), 12, 0xFFFFFFFF, 0.8F);
			}
			// HP on a white strip along its foot.
			float frac = mon.hp / (float) Math.max(1, mon.maxHp()), hx = x + 4 + 20, hw = cw - 8 - 40, hh = 14 + 12, hy = y + ch - 4 - 18 - hh;
			V.pill(g, hx, hy, hw, hh, 0xD9FFFFFF);
			V.tracked(g, "HP", hx + 12, V.capsTop(hy + hh / 2, 14), 14, 0xFFE5525C, 1);
			String hpT = mon.hp + "/" + mon.maxHp();
			float nw = V.width(hpT, 19, true), bx = hx + 12 + V.trackedWidth("HP", 14, 1) + 10;
			V.bar(g, bx, hy + 6, hw - (bx - hx) - 10 - nw - 12, 14, frac, V.hpColour(frac));
			V.right(g, hpT, hx + hw - 12, V.capsTop(hy + hh / 2, 19), 19, V.INK, true);
			if (view == View.PARTY) vhit(x, y, cw, ch, () -> pickParty(idx));
			if (view == View.USE_ON) vhit(x, y, cw, ch, () -> use(pocketItems().get(Math.min(bagAt, pocketItems().size() - 1)), idx));
		}
		if (view != View.PARTY_ACTION) V.hints(g, "A", "Choose", "B", "Back");
	}

	/** The summary's page (INFO, STATS or MOVES), and the move picked on MOVES. */
	private int summaryPage = 1, moveAt;

	@Override
	protected boolean wantsTab() {
		return view == View.SUMMARY || view == View.BAG;
	}

	private static final String[] NATURES = {"Hardy", "Lonely", "Brave", "Adamant", "Naughty", "Bold", "Docile", "Relaxed", "Impish", "Lax", "Timid", "Hasty",
			"Serious", "Jolly", "Naive", "Modest", "Mild", "Quiet", "Bashful", "Rash", "Calm", "Gentle", "Sassy", "Careful", "Quirky"};
	private static final String[] LIKES = {"Napping in the sun", "Running about", "Berries", "Splashing in water", "Being brushed", "Sitting on your shoulder",
			"Bright lights", "Quiet places", "Chasing butterflies", "Big meals"};

	/** How good a stat is for its level, in a word (the viewer's statWord). */
	private static String statWord(int v, int lv) {
		float f = v / (lv * 2.6F + 30);
		return f > 0.8F ? "Best" : f > 0.62F ? "Great" : f > 0.45F ? "Good" : f > 0.3F ? "Decent" : "Fair";
	}

	/**
	 * One of your team in full, as the UI Viewer's (Let's Go's STATS page, signed off 20 Nov): it stands on its own
	 * backdrop (its type's swirl and drifting things), INFO / STATS / MOVES along the top; STATS: the judge's stamp
	 * top left and its six stats as a lime hexagon with a word for each; INFO: who caught it and when, in white and
	 * lime, its EXP at the foot; MOVES: each a bar in its type's colour with its mark and PP, what the one you're on
	 * does underneath. Its name plate at the bottom right, its number at the bottom left.
	 */
	private void drawSummary(GuiGraphics g, PlayerData d) {
		if (d.party.isEmpty()) return;
		partyAt = Math.min(partyAt, d.party.size() - 1);
		Mon mon = d.party.get(partyAt);
		Species sp = mon.species;
		float secs = (ticks + Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true)) / 20F;
		Scenes.typeBg(g, sp.type1(), false);
		Scenes.modelBare(g, secs, sp.id(), 1.05);
		int tc = Ui.typeColor(sp.type1()) | 0xFF000000;
		// A soft shade down the right on INFO and MOVES, so white and lime read on any backdrop.
		if (summaryPage != 1) {
			for (int i = 0; i < 24; i++) {
				float k = i / 23F, a = k < 0.3F ? 0.22F * k / 0.3F : 0.22F + 0.2F * (k - 0.3F) / 0.7F;
				V.rr(g, 520 + i * 760 / 24F, -V.oy / V.k, 760 / 24F + 1, V.gh / V.k, 0, Ui.alpha(0xFF181E3C, a));
			}
		}
		// The tabs.
		String[] pages = {"INFO", "STATS", "MOVES"};
		float tw = 0;
		for (String t : pages) tw += V.titleTrackedWidth(t, 17, 1) + 40 + 10;
		float tx = 640 - (tw - 10) / 2;
		for (int j = 0; j < 3; j++) {
			float w = V.titleTrackedWidth(pages[j], 17, 1) + 40, hh = 17 * 1.5F + 10;
			boolean on = j == summaryPage;
			if (on) {
				V.shadow(g, tx, 10, w, hh, hh / 2, 0.6F);
				V.pill(g, tx, 10, w, hh, 0xFFFFFFFF);
			} else V.pill(g, tx, 10, w, hh, 0x47FFFFFF);
			V.titleTracked(g, pages[j], tx + 20, V.capsTop(10 + hh / 2, 17), 17, on ? V.INK : 0xFFFFFFFF, 1);
			int jj = j;
			vhit(tx, 10, w, hh, () -> summaryPage = jj);
			tx += w + 10;
		}
		int[] stats = {mon.maxHp(), mon.attack(), mon.defense(), mon.speed(), mon.spDefense(), mon.spAttack()};
		if (summaryPage == 1) {
			// The judge's stamp, tilted.
			float f = 0;
			for (int v : stats) f += v;
			f = f / 6 / (mon.level * 2.6F + 30);
			String word = f > 0.62F ? "amazing" : f > 0.48F ? "great" : f > 0.36F ? "good" : "decent";
			g.pose().pushMatrix();
			float scx = V.x(24 + 130), scy = V.y(20 + 95);
			g.pose().translate(scx, scy);
			g.pose().rotate((float) Math.toRadians(-8));
			g.pose().translate(-scx, -scy);
			V.img(g, V.tex("stamp_" + word), 24 + (260 - 190) / 2F, 20, 190, 190, 128, 128, 0xEBFFFFFF);
			g.pose().popMatrix();
			statHex(g, stats, mon.level, 720 + 270 - 190, 40 + 20);
		} else if (summaryPage == 0) {
			// Who caught it, as Let's Go's: white Fredoka on its backdrop, what each is in lime, a lime line under the trainer.
			float rx = 720 + 24, ry = 40 + 34;
			int lime = 0xFFC9F55A;
			Component ot = V.titleComp(Lang.t("Original Trainer") + ": " + name());
			V.draw(g, ot, rx + 0.5F, V.capsTop(ry + 22, 30) + 2, 30, 0x40141830, false);
			V.draw(g, ot, rx, V.capsTop(ry + 22, 30), 30, lime, false);
			V.rr(g, rx, ry + 45 + 3, 540 - 24 + 40 - 40, 3, 1.5F, lime);
			int seed = Math.abs((sp.id().hashCode() * 31) ^ (mon.dvAtk * 17 + mon.dvDef * 5 + mon.dvSpe));
			String[][] rows = {{"ID No.", String.format("%05d", Math.abs(name().hashCode()) % 100000)}, {"Nature", NATURES[seed % NATURES.length]},
					{"Met", d.place.isEmpty() ? "KANTO" : d.place.toUpperCase().replace('_', ' ')}, {"Level met", String.valueOf(Math.max(2, mon.level - 3))},
					{"Likes", LIKES[(seed / 7) % LIKES.length]}};
			float yy = ry + 45 + 8 + 10;
			for (String[] r : rows) {
				Component lab = V.titleComp(Lang.t(r[0]) + ": ");
				float lw = V.width(lab, 26);
				V.draw(g, lab, rx + 0.5F, V.capsTop(yy + 19, 26) + 2, 26, 0x40141830, false);
				V.draw(g, lab, rx, V.capsTop(yy + 19, 26), 26, 0xFFFFFFFF, false);
				V.draw(g, V.titleComp(r[1]), rx + lw, V.capsTop(yy + 19, 26), 26, lime, false);
				yy += 39 + 10;
			}
		} else drawMoves(g, mon);
		// The name plate: dark glass, a ball's outline in it, name, level, types, HP.
		float px = 700, py = 512, pw = 560, ph = 14 + 48 + 6 + 24 + 10 + 22 + 14;
		V.shadow(g, px, py, pw, ph, 44, 0.9F);
		V.rr(g, px, py, pw, ph, 44, 0x8CFFFFFF);
		V.rr(g, px + 2, py + 2, pw - 4, ph - 4, 42, 0x6B1C243C);
		g.enableScissor(Math.round(V.x(px + 2)), Math.round(V.y(py + 2)), Math.round(V.x(px + pw - 2)), Math.round(V.y(py + ph - 2)));
		V.icon(g, "icon_ballmark", px - 34, py - 28, 150, 0x59FFFFFF);
		g.disableScissor();
		float nx = px + 92, ny = py + 14 + 24;
		V.title(g, mon.name(), nx, V.capsTop(ny, 32), 32, 0xFFFFFFFF);
		float gx = nx + V.titleWidth(mon.name(), 32) + 10;
		String gm = mon.genderMark();
		if (gm != null && !gm.isEmpty()) {
			V.text(g, gm, gx, V.capsTop(ny, 24), 24, gm.contains("\u2640") ? 0xFFFF8FB1 : 0xFF7FB8FF, true);
			gx += V.width(gm, 24, true) + 10;
		}
		String st = mon.fainted() ? "FNT" : mon.status.tag;
		if (!st.isEmpty()) {
			float sw = V.width(st, 13, true) + 16;
			V.rr(g, gx, ny - 10.5F, sw, 21, 7, Ui.statusColor(st));
			V.text(g, st, gx + 8, V.capsTop(ny, 13), 13, 0xFFFFFFFF, true);
		}
		V.right(g, "Lv. " + mon.level, px + pw - 24, V.capsTop(ny, 22), 22, 0xFFFFFFFF, true);
		float cy = py + 14 + 48 + 6, cx = nx;
		cx += V.type(g, sp.type1().label, tc, cx, cy, 92, 13.5F) + 8;
		if (sp.type2() != null) V.type(g, sp.type2().label, Ui.typeColor(sp.type2()) | 0xFF000000, cx, cy, 92, 13.5F);
		float hy = cy + 24 + 10, frac = mon.hp / (float) Math.max(1, mon.maxHp());
		V.tracked(g, "HP", nx, V.capsTop(hy + 9, 14), 14, 0xFFFFE04A, 1);
		String hpT = mon.hp + "/" + mon.maxHp();
		float nw = V.width(hpT, 19, true), bx = nx + V.trackedWidth("HP", 14, 1) + 10;
		V.pill(g, bx, hy + 2, px + pw - 24 - nw - 10 - bx, 14, 0x4D000000);
		if (frac > 0) V.pill(g, bx + 2, hy + 4, Math.max(10, (px + pw - 24 - nw - 10 - bx - 4) * frac), 10, V.hpColour(frac));
		V.right(g, hpT, px + pw - 24, V.capsTop(hy + 9, 19), 19, 0xFFFFFFFF, true);
		// Its name and number at the bottom left (and its EXP, on INFO).
		boolean info = summaryPage == 0;
		float dw = info ? 440 : 260, dy = info ? 566 : 640, dh = V.H - dy + 40;
		V.rr(g, -40, dy, dw + 40, dh, 18, 0x8CFFFFFF);
		V.rr(g, -40, dy + 2, dw + 38, dh, 16, 0x6B1C243C);
		V.text(g, mon.name(), 22, dy + 10 + 3, 17, 0xFFFFFFFF, false);
		V.text(g, "POKéDEX No. " + String.format("%03d", sp.dex()), 22, dy + 10 + 3 + 24, 17, 0xFFFFFFFF, false);
		if (info) {
			int now = sp.growth().expAt(mon.level), next = mon.level >= 100 ? now : sp.growth().expAt(mon.level + 1);
			float ey = dy + 10 + 3 + 24 + 30;
			V.text(g, Lang.t("EXP points"), 22, ey, 16, 0xFFFFFFFF, false);
			V.right(g, String.format("%,d", mon.exp), dw - 22, ey, 16, 0xFFFFFFFF, false);
			V.text(g, Lang.t("To the next level"), 22, ey + 22, 16, 0xFFFFFFFF, false);
			V.right(g, String.format("%,d", Math.max(0, next - mon.exp)), dw - 22, ey + 22, 16, 0xFFFFFFFF, false);
			float ef = next <= now ? 1 : (mon.exp - now) / (float) (next - now);
			V.pill(g, 22, ey + 22 + 28, dw - 44, 9, 0x4D000000);
			if (ef > 0) V.rrh(g, 22, ey + 22 + 28, Math.max(9, (dw - 44) * ef), 9, 4.5F, new float[] {0, 1}, new int[] {0xFF7CC2FA, 0xFF3C8DE4});
		}
		vhit(0, 600, 300, 120, () -> view = View.PARTY);
		V.hints(g, "TAB", "Page", "B", "Back");
	}

	/** The six stats as Let's Go's hexagon (the viewer's statHex, 380 by 360 at x, y): its rim, its share in lime, a word and a number for each. */
	private static void statHex(GuiGraphics g, int[] st, int lv, float ox, float oy) {
		String[] labels = {"HP", "Attack", "Defense", "Speed", "Sp. Def", "Sp. Atk"};
		float R = 118, cx = ox + 190, cy = oy + 178, mx = lv * 2.6F + 30;
		float[][] pt = new float[6][];
		for (int k = 0; k < 6; k++) {
			double a = -Math.PI / 2 + k * Math.PI / 3;
			pt[k] = new float[] {(float) Math.cos(a), (float) Math.sin(a)};
		}
		java.util.function.BiFunction<Float, Float, float[]> ring = (r, dy) -> {
			float[] q = new float[12];
			for (int k = 0; k < 6; k++) {
				q[k * 2] = cx + pt[k][0] * r;
				q[k * 2 + 1] = cy + pt[k][1] * r + dy;
			}
			return q;
		};
		// Its shadow, the aqua inside, the lime rim (light at the top left, green at the foot right), the spokes, the half ring.
		V.poly(g, ring.apply(R * 1.06F + 8, 4F), 0x2E000000);
		V.poly(g, ring.apply(R + 7, 0F), 0xFF3FD06A);
		V.poly(g, ring.apply(R + 3, 0F), 0xFFA6EE6A);
		V.poly(g, ring.apply(R - 7, 0F), 0xB86ED6BE);
		for (int k = 0; k < 6; k++) V.line(g, cx, cy, cx + pt[k][0] * R, cy + pt[k][1] * R, 1.5F, 0x80FFFFFF);
		float[] half = ring.apply(R * 0.5F, 0F);
		for (int k = 0; k < 6; k++) V.line(g, half[k * 2], half[k * 2 + 1], half[(k + 1) % 6 * 2], half[(k + 1) % 6 * 2 + 1], 1.5F, 0x66FFFFFF);
		// Its own share.
		float[] val = new float[12];
		for (int k = 0; k < 6; k++) {
			float f = Math.max(0.15F, Math.min(1, st[k] / mx));
			val[k * 2] = cx + pt[k][0] * R * f;
			val[k * 2 + 1] = cy + pt[k][1] * R * f;
		}
		V.poly(g, val, 0xD9E9FF8A);
		for (int k = 0; k < 6; k++) V.line(g, val[k * 2], val[k * 2 + 1], val[(k + 1) % 6 * 2], val[(k + 1) % 6 * 2 + 1], 2.5F, 0xFFFFFFFF);
		int ink = 0xFF1F3A2C;
		for (int k = 0; k < 6; k++) {
			float x = cx + pt[k][0] * (R + 40), y = cy + pt[k][1] * (R + 40);
			Component l = V.titleComp(Lang.t(labels[k])), w = V.titleComp(Lang.t(statWord(st[k], lv))), n = V.raw(String.valueOf(st[k]), true);
			V.stroked(g, l, x - V.width(l, 19) / 2, y - 10 - 19 * 0.8F, 19, 0xFFFFFFFF, ink, 4);
			V.stroked(g, w, x - V.width(w, 20) / 2, y + 12 - 20 * 0.8F, 20, 0xFFFFE04A, ink, 4);
			V.stroked(g, n, x - V.width(n, 13) / 2, y + 30 - 13 * 0.8F, 13, 0xFFFFFFFF, ink, 3);
		}
	}

	/** MOVES as Let's Go's: each a bar in its type's colour, its mark in a white circle, its type on a tag, its name big, PP on a dark slanted cap. */
	private void drawMoves(GuiGraphics g, Mon mon) {
		moveAt = Math.max(0, Math.min(mon.moves.size() - 1, moveAt));
		float x0 = 720, y0 = 40 + 34, bw = 540, bh = 64;
		for (int j = 0; j < mon.moves.size(); j++) {
			Move m = Move.get(mon.moves.get(j));
			int c = Ui.typeColor(m.type()) | 0xFF000000;
			float y = y0 + j * (bh + 12);
			if (vover(x0, y, bw, bh)) moveAt = j;
			boolean on = j == moveAt;
			float x = x0 - (on ? 10 : 0);
			if (on) {
				V.rr(g, x - 3, y - 3, bw + 6, bh + 6, 35, 0x66FFFFFF);
				V.tri(g, x - 40 + 19, y + 32, 40, 0, 0xFFFFFFFF);
				V.tri(g, x - 40 + 18, y + 32, 20, 0, 0xFFDDE3EE);
			}
			V.shadow(g, x, y, bw, bh, 32, 0.8F);
			V.rr(g, x, y, bw, bh, 32, on ? 0xFFFFFFFF : 0xBFFFFFFF);
			V.rrStops(g, x + 3, y + 3, bw - 6, bh - 6, 29, new float[] {0, 0.55F, 1}, new int[] {Ui.lighter(c, 0.3F), c, Ui.darker(c, 0.85F)});
			g.enableScissor(Math.round(V.x(x + 3)), Math.round(V.y(y + 3)), Math.round(V.x(x + bw - 3)), Math.round(V.y(y + bh - 3)));
			V.icon(g, "glyph_" + m.type().name().toLowerCase(java.util.Locale.ROOT), x + bw - 118 - 92, y - 14, 92, 0x47FFFFFF);
			// The PP cap: slanted, dark, a white edge.
			float sx = x + bw - 132 + 16, slant = (bh) * (float) Math.tan(Math.toRadians(18));
			V.poly(g, new float[] {sx + slant / 2 - 3, y, x + bw + 20, y, x + bw + 20, y + bh, sx - slant / 2 - 3, y + bh}, 0x8CFFFFFF);
			V.poly(g, new float[] {sx + slant / 2, y, x + bw + 20, y, x + bw + 20, y + bh, sx - slant / 2, y + bh}, 0xBA181E38);
			g.disableScissor();
			int left = j < mon.pp.size() ? mon.pp.get(j) : m.pp();
			String ppn = String.valueOf(left), ppt = "/" + m.pp();
			float rx = x + bw - 18, cy = y + bh / 2;
			V.text(g, ppt, rx - V.width(ppt, 18, true), V.capsTop(cy, 18), 18, 0xD9FFFFFF, true);
			float pw2 = V.width(ppn, 26, true);
			V.text(g, ppn, rx - V.width(ppt, 18, true) - 6 - pw2, V.capsTop(cy, 26), 26, 0xFFFFFFFF, true);
			V.text(g, "PP", rx - V.width(ppt, 18, true) - 6 - pw2 - 6 - V.width("PP", 13, true), V.capsTop(cy, 13), 13, 0xBFFFFFFF, true);
			V.ellipse(g, x + 6 + 26, cy + 2, 26, 26, 0x26000000);
			V.ellipse(g, x + 6 + 26, cy, 26, 26, 0xFFFFFFFF);
			V.icon(g, "glyph_" + m.type().name().toLowerCase(java.util.Locale.ROOT), x + 6 + 26 - 17, cy - 17, 34, Ui.darker(c, 0.9F));
			String tn = m.type().label.toUpperCase();
			float tw = V.trackedWidth(tn, 12, 1.5F) + 20;
			V.rr(g, x + 6 + 52 + 10, cy - 10, tw, 20, 7, Ui.darker(c, 0.7F));
			V.tracked(g, tn, x + 6 + 52 + 10 + 10, V.capsTop(cy, 12), 12, 0xFFFFFFFF, 1.5F);
			V.title(g, m.name(), x + 6 + 52 + 10 + tw + 10, V.capsTop(cy, 26) + 2, 26, 0x38000000);
			V.title(g, m.name(), x + 6 + 52 + 10 + tw + 10, V.capsTop(cy, 26), 26, 0xFFFFFFFF);
			int jj = j;
			vhit(x0, y, bw, bh, () -> moveAt = jj);
		}
		// What the one you're on does, in a glass card underneath.
		if (!mon.moves.isEmpty()) {
			Move m = Move.get(mon.moves.get(moveAt));
			float cy = y0 + mon.moves.size() * (bh + 12) + 4, ch = 14 + 26 + 6 + 2 * 17 * 1.35F + 14;
			V.rr(g, x0, cy, bw, ch, 20, 0x8CFFFFFF);
			V.rr(g, x0 + 2, cy + 2, bw - 4, ch - 4, 18, 0x6B1C243C);
			float tw = V.type(g, m.type().label, Ui.typeColor(m.type()) | 0xFF000000, x0 + 20, cy + 14, 92, 13.5F);
			String cat = !m.damaging() ? "STATUS" : java.util.EnumSet.of(Type.FIRE, Type.WATER, Type.GRASS, Type.ELECTRIC, Type.PSYCHIC, Type.ICE, Type.DRAGON).contains(m.type()) ? "SPECIAL" : "PHYSICAL";
			V.tracked(g, cat, x0 + 20 + tw + 14, V.capsTop(cy + 14 + 12, 14), 14, 0xCCFFFFFF, 1);
			String acc = m.accuracy() > 0 ? String.valueOf(m.accuracy()) : "-", pow = m.power() > 1 ? String.valueOf(m.power()) : "-";
			float ar = x0 + bw - 20;
			V.text(g, acc, ar - V.width(acc, 17, true), V.capsTop(cy + 26, 17), 17, 0xFFFFFFFF, true);
			ar -= V.width(acc, 17, true) + 6;
			V.text(g, "ACCURACY", ar - V.width("ACCURACY", 13, true), V.capsTop(cy + 26, 13), 13, 0xBFFFFFFF, true);
			ar -= V.width("ACCURACY", 13, true) + 14;
			V.text(g, pow, ar - V.width(pow, 17, true), V.capsTop(cy + 26, 17), 17, 0xFFFFFFFF, true);
			ar -= V.width(pow, 17, true) + 6;
			V.text(g, "POWER", ar - V.width("POWER", 13, true), V.capsTop(cy + 26, 13), 13, 0xBFFFFFFF, true);
			List<String> desc = V.wrap(BattleScreen.moveText(m.id()), bw - 40, 17, false);
			for (int i = 0; i < Math.min(2, desc.size()); i++) V.text(g, desc.get(i), x0 + 20, cy + 14 + 26 + 6 + i * 17 * 1.35F, 17, 0xFFFFFFFF, false);
		}
	}

	/** Your bag's colour (from your clothes), or yellow. */
	private static int bagColour() {
		Outfit o = Outfit.parse(ClientState.data.outfit, "girl".equals(ClientState.data.gender));
		return 0xFF000000 | Outfit.COLOURS[Math.floorMod(o.get("bagc"), Outfit.COLOURS.length)];
	}

	/** Each pocket's colour and picture (the viewer's POCKETS). */
	private static final int[] POCKET_COLS = {0xFF3DB873, 0xFFE5484F, 0xFFF59A2A, 0xFF3C8DE4};
	private static final String[] POCKET_ICONS = {"icon_potion", "icon_ball", "icon_key", "icon_disc"};

	/**
	 * The bag as the UI Viewer's (Let's Go's, signed off 20 Nov): one big white page on a sunny backdrop, your money
	 * in its corner; the pockets along the top, each a round picture in its colour (the one you're on lifted, a red
	 * arrow and a green line under its name); what's in it as tiles below, four across, each with its picture and
	 * how many; what the one you're on does along the foot.
	 */
	private void drawBag(GuiGraphics g, PlayerData d) {
		Scenes.bg2(g, "bag");
		float px = 60, py = 40, pw = 1160, ph = 610;
		V.panel(g, px, py, pw, ph, 36, false);
		// Your money.
		String cash = Ui.money(d.money);
		float mw = 10 + V.trackedWidth("MONEY", 12, 1) + 20 + 8 + V.width(cash, 19, true) + 18, mh = 19 * 1.5F + 12;
		V.pill(g, 36 - 3, 18 - 3, mw + 6, mh + 6, 0xFFFFC93C);
		V.shadow(g, 36, 18, mw, mh, mh / 2, 0.6F);
		V.pill(g, 36, 18, mw, mh, 0xFFFFFFFF);
		float tagw = V.trackedWidth("MONEY", 12, 1) + 20;
		V.pill(g, 36 + 10, 18 + mh / 2 - 10, tagw, 20, 0xFFFFC93C);
		V.tracked(g, "MONEY", 36 + 10 + 10, V.capsTop(18 + mh / 2, 12), 12, V.INK, 1);
		V.text(g, cash, 36 + 10 + tagw + 8, V.capsTop(18 + mh / 2, 19), 19, V.INK, true);
		// The pockets.
		float[] colW = new float[POCKETS.length];
		float total = 0;
		for (int i = 0; i < POCKETS.length; i++) {
			colW[i] = Math.max(76, V.titleWidth(Lang.t(POCKETS[i]), 17));
			total += colW[i] + (i > 0 ? 54 : 0);
		}
		float cx = 640 - total / 2, top = py + 3 + 26;
		for (int i = 0; i < POCKETS.length; i++) {
			boolean on = i == pocket;
			float mid = cx + colW[i] / 2, y = top - (on ? 4 : 0);
			int c = POCKET_COLS[i];
			if (on) V.text(g, "▶", mid - 38 - 26, y + 22, 24, V.RED, true);
			V.shadow(g, mid - 38, y, 76, 76, 38, 0.6F);
			V.ellipse(g, mid, y + 38, 42, 42, on ? c : 0xFFE4E9F4);
			V.ellipse(g, mid, y + 38, 38, 38, 0xFFFFFFFF);
			V.icon(g, POCKET_ICONS[i], mid - 24, y + 38 - 24, 48);
			V.titleCentred(g, Lang.t(POCKETS[i]), mid, V.capsTop(y + 76 + 6 + 12.75F, 17), 17, on ? c : 0xFF8A93A6);
			if (on) V.pill(g, mid - 35, y + 76 + 6 + 25.5F + 6, 70, 5, 0xFF41BF84);
			int idx = i;
			if (view == View.BAG) vhit(cx, y, colW[i], 76 + 40, () -> {
				pocket = idx;
				bagAt = 0;
				bagTop = 0;
			});
			cx += colW[i] + 54;
		}
		float dashY = top + 76 + 6 + 25.5F + 6 + 5 + 22;
		for (float x = px + 3 + 44; x < px + pw - 3 - 44; x += 12) V.rr(g, x, dashY, 6, 2, 1, 0xFFE4E9F4);
		// The tiles: four across, three rows in view.
		List<String> l = pocketItems();
		int cols = 4, rows = 3;
		float gx = px + 3 + 50, gy = dashY + 2 + 22, gw = pw - 6 - 100, tw = (gw - 14 * (cols - 1)) / cols, th = 76;
		if (bagAt / cols < bagTop) bagTop = bagAt / cols;
		if (bagAt / cols >= bagTop + rows) bagTop = bagAt / cols - rows + 1;
		for (int k = bagTop * cols; k < Math.min(l.size(), (bagTop + rows) * cols); k++) {
			int idx = k, r = k / cols - bagTop, c0 = k % cols;
			float x = gx + c0 * (tw + 14), y = gy + r * (th + 14);
			if (vover(x, y, tw, th) && view == View.BAG) bagAt = k;
			boolean on = k == bagAt;
			Item it = Item.find(l.get(k));
			if (it == null) continue;
			int[] kc = V.GOODS[V.goodsKind(it.name())];
			if (on) {
				V.rr(g, x, y + 6, tw, th, 22, 0x4DE8961E);
				V.rr(g, x - 6, y - 6, tw + 12, th + 12, 28, 0xFFFFC93C);
				V.rr(g, x - 3, y - 3, tw + 6, th + 6, 25, 0xFFFFFFFF);
				V.sel(g, x, y, tw, th, 22);
				V.text(g, "▶", x - 18 - 6, V.capsTop(y + th / 2, 20), 20, V.RED, true);
			} else {
				V.rr(g, x, y, tw, th, 22, 0xFFE8EDF6);
				V.rr(g, x + 2, y + 2, tw - 4, th - 4, 20, 0xFFF5F8FD);
			}
			float icx = x + 10 + 27, icy = y + th / 2;
			V.ellipse(g, icx, icy, 30, 30, kc[1]);
			V.ellipse(g, icx, icy, 27, 27, 0xFFFFFFFF);
			V.icon(g, V.itemIcon(it.name()), icx - 18, icy - 18, 36);
			String cnt = "x" + d.count(it.id());
			float cw = V.width(cnt, 18, true);
			V.text(g, cnt, x + tw - 16 - cw, V.capsTop(icy, 18), 18, kc[2], true);
			List<String> nl = V.wrap(it.name(), tw - 10 - 54 - 12 - 16 - cw - 8, 17, true);
			float lh = 17 * 1.15F, ny = icy - Math.min(2, nl.size()) * lh / 2;
			for (int q = 0; q < Math.min(2, nl.size()); q++) V.text(g, nl.get(q), x + 10 + 54 + 12, ny + q * lh + 1, 17, V.INK, true);
			if (view == View.BAG) vhit(x, y, tw, th, () -> pickBag(idx));
		}
		if (l.isEmpty()) V.centred(g, Lang.t("Nothing in this pocket yet."), 640, gy + 30, 21, V.MUTED, false);
		int lines = (l.size() + cols - 1) / cols;
		if (bagTop > 0) V.tri(g, 640, gy - 10, 14, 3, V.MUTED);
		if (bagTop + rows < lines) V.tri(g, 640, gy + rows * (th + 14) - 4, 14, 1, V.MUTED);
		// What it does, along the foot.
		float fx = px + 3 + 40, fw = pw - 6 - 80, fh = 62, fy = py + ph - 3 - 26 - fh;
		V.rr(g, fx, fy, fw, fh, 22, 0xFFF5F8FD);
		String desc = bagAt < l.size() ? BattleScreen.describe(l.get(bagAt)) : "";
		List<String> dl = V.wrap(desc, fw - 52, 21, false);
		for (int i = 0; i < Math.min(1, dl.size()); i++) V.text(g, dl.get(i), fx + 26, V.capsTop(fy + fh / 2, 21), 21, V.INK, false);
		if (view == View.BAG) V.hints(g, "A", "Use", "B", "Back", "TAB", "Pocket");
	}

	private void bagPicture(GuiGraphics g, float ox, float oy) {
		int col = bagColour(), dk = Ui.darker(col, 0.78F), dd = Ui.darker(col, 0.62F), lt = Ui.lighter(col, 0.18F);
		float k = 1;
		// The pockets' boxes (in the picture's 400 by 500), by pocket: ITEMS, BALLS, KEY ITEMS, TM & HM.
		float[][] box = {{60, 70, 280, 390}, {100, 262, 200, 160}, {60, 64, 280, 142}, {318, 230, 62, 170}};
		for (int i = 3; i >= 0; i--) {
			int idx = i;
			if (view == View.BAG) vhit(ox + box[i][0], oy + box[i][1], box[i][2], box[i][3], () -> {
				if (pocket != idx) {
					pocket = idx;
					bagAt = 0;
					bagTop = 0;
				}
			});
		}
		java.util.function.IntFunction<Float> lift = p -> p == pocket ? -6F : 0F;
		// The strap loop and the shoulder straps.
		V.ring(g, ox + 150 - 7, oy + 18 - 7, 114, 70, 25, 14, dd);
		V.rr(g, ox + 64, oy + 96, 44, 300, 10, dk);
		V.rr(g, ox + 292, oy + 96, 44, 300, 10, dk);
		// The body (ITEMS).
		float by = oy + lift.apply(0);
		pocketShape(g, ox + 60, by + 70, 280, 390, 30, col, pocket == 0, dd);
		for (int yy = 8; yy < 382; yy += 16) for (int xx = 8 + (yy / 16) % 2 * 8; xx < 272; xx += 16)
			V.rr(g, ox + 60 + xx, by + 70 + yy, 5, 5, 0, Ui.alpha(lt, 0.22F));
		V.ring(g, ox + 76, by + 86, 248, 358, 22, 2.5F, Ui.alpha(dd, 0.6F));
		V.rr(g, ox + 60, by + 430, 280, 30, 14, Ui.alpha(dd, 0.55F));
		// The side pocket (TM & HM), a disc peeking out.
		float sy = oy + lift.apply(3);
		pocketShape(g, ox + 318, sy + 230, 62, 170, 14, dk, pocket == 3, dd);
		V.rr(g, ox + 326, sy + 210, 46, 34, 8, 0xFF5A8EA1);
		V.rr(g, ox + 349 - 12, sy + 227 - 12, 24, 24, 12, 0xFF9FD3E2);
		V.rr(g, ox + 349 - 4, sy + 227 - 4, 8, 8, 4, 0xFF123C38);
		V.rr(g, ox + 324, sy + 300, 50, 6, 3, dd);
		// The front pocket (BALLS), its zip and a ball on it.
		float fy = oy + lift.apply(1);
		pocketShape(g, ox + 100, fy + 262, 200, 160, 26, lt, pocket == 1, dd);
		V.rr(g, ox + 118, fy + 297.5F, 164, 5, 2.5F, dd);
		V.rr(g, ox + 190, fy + 292, 20, 16, 4, 0xFFF4F6F5);
		V.icon(g, "icon_ball", ox + 200 - 32, fy + 362 - 32, 64);
		// The lid (KEY ITEMS) with its buckle.
		float ly = oy + lift.apply(2);
		pocketShape(g, ox + 60, ly + 64, 280, 142, 44, dk, pocket == 2, dd);
		V.rr(g, ox + 176, ly + 176, 48, 46, 9, 0xFFB8860B);
		V.rr(g, ox + 179, ly + 179, 42, 40, 7, V.GOLD);
		V.rr(g, ox + 190, ly + 190, 20, 18, 4, 0xFFB8860B);
	}

	/** One part of the bag: filled, outlined (cream and glowing when it's the pocket picked). */
	private void pocketShape(GuiGraphics g, float x, float y, float w, float h, float r, int fill, boolean on, int edge) {
		if (on) V.rr(g, x - 8, y - 8, w + 16, h + 16, r + 8, 0x66FFF4D8);
		V.rr(g, x, y, w, h, r, fill);
		V.ring(g, x, y, w, h, r, on ? 6 : 3, on ? V.CREAM : edge);
	}

	/** Whether the mouse is over one of the bag's small pockets (they sit on its body). */
	private boolean overAny(int[][] pockets) {
		for (int i = 1; i < pockets.length; i++) if (over(pockets[i][0], pockets[i][1], pockets[i][2], pockets[i][3])) return true;
		return false;
	}

	/** A picked pocket: a white line round it. */
	private void outline(GuiGraphics g, int x, int y, int w, int h, int r) {
		float t = Math.max(1, s) * 1.2F;
		Ui.ring(g, x - t, y - t, w + 2 * t, h + 2 * t, r + t, t, 0xFFFFFFFF);
	}

	/** Your trainer card: your face (from your look), name, money, the Pokédex, time played and the badges. */
	/**
	 * Your TRAINER CARD, as the UI Viewer's: you on a stand at the left, the card at the right with your name, ID,
	 * money, POKéDEX, play time, and the eight badges (our own designs; dark until you've won each).
	 */
	/** The gym badges' names, in order (the card's case). */
	private static final String[] BADGE_NAMES = {"BOULDERBADGE", "CASCADEBADGE", "THUNDERBADGE", "RAINBOWBADGE", "SOULBADGE", "MARSHBADGE", "VOLCANOBADGE", "EARTHBADGE"};

	/**
	 * The trainer card, as the UI Viewer's: you as you look on the left, on a soft swirl of light. The card has a coral
	 * band with your ID, your name big, money, POKéDEX, time and when you started on pale fields, and the badge case: a
	 * dark tray with eight sunken slots, the ones you've won shining in them. A pearly sheen drifts across it now and then.
	 */
	private void drawCard(GuiGraphics g, PlayerData d) {
		float secs = (ticks + Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true)) / 20F;
		Scenes.bg2(g, "card");
		Scenes.stand(g, secs, Scenes.you(d.outfit, "girl".equals(d.gender)), 2.1, 0, true);
		float x = 470, y = 70, w = 770, h = 580, r = 30;
		vhit(x, y, w, h, () -> view = View.MENU);
		// Its edge: a white ring, a softer one round that, and its shadow.
		V.shadow(g, x, y, w, h, r, 1.45F);
		V.rr(g, x - 8, y - 8, w + 16, h + 16, r + 8, 0x73FFFFFF);
		V.rr(g, x - 5, y - 5, w + 10, h + 10, r + 5, 0xFFFFFFFF);
		V.rr(g, x, y, w, h, r, 0xFFFFFFFF);
		// The coral band, its stripes and a ball's outline (cut to the band).
		float bandH = 104;
		g.enableScissor(Math.round(V.x(x)), Math.round(V.y(y)), Math.round(V.x(x + w)), Math.round(V.y(y + bandH)));
		V.rrh(g, x, y, w, bandH + r, r, new float[] {0, 0.55F, 1}, new int[] {0xFFFF8A7A, 0xFFE5525C, 0xFFC93A5A});
		// -45deg stripes, 14 on and 14 off (measured across them: about 19.8 along the band).
		float step = 28 * 1.41421F, on = 14 * 1.41421F;
		for (float sx = x - bandH; sx < x + w + bandH; sx += step)
			V.poly(g, new float[] {sx, y + bandH, sx + on, y + bandH, sx + on + bandH, y, sx + bandH, y}, 0x1FFFFFFF);
		float bcx = x + w - 75, bcy = y + 53, R = 101.2F, t = 16.1F;
		V.ring(g, bcx - R - t / 2, bcy - R - t / 2, 2 * R + t, 2 * R + t, R + t / 2, t, 0x38FFFFFF);
		float r2 = 32.2F;
		V.ring(g, bcx - r2 - t / 2, bcy - r2 - t / 2, 2 * r2 + t, 2 * r2 + t, r2 + t / 2, t, 0x38FFFFFF);
		V.rr(g, bcx - R + t / 2, bcy - t / 2, R - t / 2 - r2 - t / 2, t, 0, 0x38FFFFFF);
		V.rr(g, bcx + r2 + t / 2, bcy - t / 2, R - t / 2 - r2 - t / 2, t, 0, 0x38FFFFFF);
		g.disableScissor();
		// The card's picture in a white box, and its name.
		V.rr(g, x + 30, y + 26 + 3, 52, 52, 16, 0x26000000);
		V.rr(g, x + 30, y + 26, 52, 52, 16, 0xFFFFFFFF);
		V.icon(g, "icon_card", x + 30 + 7, y + 26 + 7, 38);
		String head = Lang.t("TRAINER CARD");
		V.title(g, head, x + 30 + 52 + 14, V.capsTop(y + 52, 36) + 3, 36, 0x4D781428);
		V.title(g, head, x + 30 + 52 + 14, V.capsTop(y + 52, 36), 36, 0xFFFFFFFF);
		// ID No. in a pill at the right.
		String id = idNo();
		float pw = 16 + V.labelWidth("ID No.") + 8 + V.width(id, 18, true) + 16, ph = 36, px = x + w - 28 - pw, py = y + 34;
		V.pill(g, px, py + 3, pw, ph, 0x26000000);
		V.pill(g, px, py, pw, ph, 0xFFFFFFFF);
		V.label(g, "ID No.", px + 16, V.capsTop(py + ph / 2, 13) + 1);
		V.text(g, id, px + 16 + V.labelWidth("ID No.") + 8, V.capsTop(py + ph / 2, 18), 18, V.INK, true);
		// Your name, big, over a dashed line.
		float cx0 = x + 30, cw = w - 60, ny = y + bandH + 22;
		V.label(g, Lang.t("Name"), cx0 + 4, ny + 17.5F);
		V.title(g, name(), cx0 + 4 + V.labelWidth(Lang.t("Name")) + 14, ny, 38, V.INK);
		for (float dx = cx0; dx < cx0 + cw; dx += 18) V.rr(g, dx, ny + 46 + 6, Math.min(9, cx0 + cw - dx), 3, 0, 0xFFEEF1F8);
		// The four fields.
		String[][] fields = {{"Money", Ui.money(d.money)}, {"Pokédex", Lang.t("{0} caught", String.valueOf(d.hasDex() ? d.caught.size() : 0))},
				{"Play time", playTime(d)}, {"Started", started(d)}};
		float fy = ny + 55 + 10, fw = (cw - 14) / 2;
		for (int i = 0; i < 4; i++) {
			float fx = cx0 + (i % 2) * (fw + 14), yy = fy + (i / 2) * (46 + 10);
			V.rr(g, fx, yy, fw, 46, 14, 0xFFE9EDF5);
			V.rr(g, fx, yy + 2, fw, 44, 14, 0xFFF2F5FB);
			V.label(g, Lang.t(fields[i][0]), fx + 18, V.capsTop(yy + 23, 13) + 1);
			V.right(g, fields[i][1], fx + fw - 18, V.capsTop(yy + 23, 21), 21, V.INK, true);
		}
		// The badge case: a dark tray, eight sunken slots.
		float kx = cx0, ky = fy + 46 * 2 + 10 + 18, kw = cw, kh = 14 + 18 + 10 + 64 + 4 + 15 + 12;
		V.rr(g, kx, ky, kw, kh, 22, 0xFF141A33);
		V.rrv(g, kx, ky + 3, kw, kh - 3, 22, 0xFF2A365C, 0xFF1E2746);
		V.label(g, Lang.t("Badges"), kx + 18, ky + 14 + 2, 0xFFC9D3F0);
		String got = d.badges.size() + " / 8";
		V.right(g, got, kx + kw - 18, ky + 14 + 1, 14, 0xFFFFD45A, true);
		float colW = (kw - 36 - 70) / 8F, sy = ky + 14 + 18 + 10;
		for (int i = 0; i < 8; i++) {
			boolean won = d.badges.contains(BADGE_NAMES[i]);
			float scx = kx + 18 + i * (colW + 10) + colW / 2, scy = sy + 32;
			V.ellipse(g, scx, scy + 1, 32, 32, 0x1FFFFFFF);
			V.ellipse(g, scx, scy, 32, 32, 0xFF0B1022);
			V.ellipse(g, scx, scy + 1.5F, 30, 30, 0xFF0E1428);
			V.ellipse(g, scx, scy - 4, 22, 22, 0xFF131A31);
			V.ellipse(g, scx, scy - 6.4F, 14, 14, 0xFF161D36);
			V.icon(g, "badge_" + i, scx - 25, scy - 25, 50, won ? 0xFFFFFFFF : 0x73737373);
			V.centred(g, String.valueOf(i + 1), scx, sy + 64 + 4, 11, 0xFF8E9AC0, true);
		}
		// Your partner, and the Hall of Fame.
		float ry = ky + kh + 16;
		if (!d.party.isEmpty()) {
			Mon lead = d.party.get(0);
			emblem(g, lead.species.id(), cx0, ry - 3, 58);
			V.label(g, Lang.t("Partner"), cx0 + 58 + 14, ry + 5);
			V.text(g, lead.species.name(), cx0 + 58 + 14, ry + 5 + 17, 21, V.INK, true);
		}
		float rx = cx0 + cw;
		V.label(g, Lang.t("Hall of Fame"), rx - V.labelWidth(Lang.t("Hall of Fame")), ry + 5);
		V.right(g, Lang.t("Not yet"), rx, ry + 5 + 18, 18, V.MUTED, true);
		// The pearly sheen, now and then (every 6 seconds, crossing in the last 1.8).
		float cyc = (((secs - 1) % 6) + 6) % 6 / 6F;
		if (cyc > 0.7F) {
			float q = (cyc - 0.7F) / 0.3F;
			q = q * q * (3 - 2 * q);
			float bw = w * 0.3F, left = x + (-1.2F + 3.8F * q) * bw, sk = 0.3249F * h / 2;
			g.enableScissor(Math.round(V.x(x)), Math.round(V.y(y)), Math.round(V.x(x + w)), Math.round(V.y(y + h)));
			float[] at = {0, 1 / 3F, 2 / 3F, 1};
			int[] cs = {0x00FFFFFF, 0x80FFFFFF, 0x40D6BEFF, 0x00D6BEFF};
			for (int k = 0; k < 3; k++) {
				float a0 = left + at[k] * bw, a1 = left + at[k + 1] * bw;
				float[] xy = {V.x(a0 + sk), V.y(y), V.x(a1 + sk), V.y(y), V.x(a1 - sk), V.y(y + h), V.x(a0 + sk), V.y(y), V.x(a1 - sk), V.y(y + h), V.x(a0 - sk), V.y(y + h)};
				Ui.tris(g, xy, new int[] {cs[k], cs[k + 1], cs[k + 1], cs[k], cs[k + 1], cs[k]});
			}
			g.disableScissor();
		}
		V.hints(g, "B", "Back");
	}

	@SuppressWarnings("unused")
	private static Type none() {
		return null;
	}
}
