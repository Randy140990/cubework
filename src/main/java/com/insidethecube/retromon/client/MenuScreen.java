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
		if (view == View.SAVING && ticks - savingAt >= 30) say(View.MENU, name() + " saved the game.");
	}

	/** When "Saving..." came up (ticks). */
	private int savingAt;

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
				else if (Ui.isUp(e)) partyAt = Math.floorMod(partyAt - 1, d.party.size());
				else if (Ui.isDown(e)) partyAt = Math.floorMod(partyAt + 1, d.party.size());
				else if (Ui.isB(e)) view = View.PARTY;
			}
			case BAG -> {
				int n = pocketItems().size() + 1;
				if (Ui.isLeft(e) || Ui.isRight(e) || e.key() == GLFW.GLFW_KEY_TAB) {
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
				if (Ui.isB(e)) view = View.MENU;
				else options.key(e);
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

	/** Your save's details (the viewer's card): you, where you are, badges, the Pokédex and time played. */
	private void saveCard(GuiGraphics g, PlayerData d) {
		float x = 330, y = 70, w = 620, h = 300;
		V.panel(g, x, y, w, h);
		com.insidethecube.retromon.engine.Places.Place pl = com.insidethecube.retromon.engine.Places.get(d.place);
		String[][] rows = {{"Player", name()}, {"Place", pl == null ? d.place.toUpperCase().replace('_', ' ') : pl.name()}, {"Badges", String.valueOf(d.badges.size())},
				{"Pokédex", d.hasDex() ? String.valueOf(d.caught.size()) : "-"}, {"Time", String.format("%d:%02d", (int) d.played / 3600, (int) d.played / 60 % 60)}};
		float lw = 0;
		for (String[] r : rows) lw = Math.max(lw, V.labelWidth(r[0]));
		for (int i = 0; i < rows.length; i++) {
			float ry = y + 24 + i * (h - 48) / rows.length + 8;
			V.label(g, rows[i][0], x + 30, ry + 7);
			V.text(g, rows[i][1], x + 30 + lw + 24, ry, 22, V.TEXT, true);
		}
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
			case MENU, SAVE, SAVING, WAIT -> drawMenu(g);
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
					actionAt = c.at;
				}
			}
			case SUMMARY -> drawSummary(g, d);
			case BAG, BAG_ACTION -> {
				drawBag(g, d);
				if (view == View.BAG_ACTION) {
					Choice c = new Choice(List.of("USE", "CANCEL"));
					c.at = actionAt;
					vchoice(g, c, V.W - 140 - 300, V.H - 26 - 150 - V.choiceHeight(c.options.size()), 300, this::bagAction);
					actionAt = c.at;
				}
			}
			case CARD -> drawCard(g, d);
			case SAVING -> {
				g.fill(0, 0, width, height, 0x80061A1E);
				saveCard(g, d);
				TextBox.still("Saving... Don't turn off the power.").renderV(g, font, 140, 26, 1000, ticks, partial, false);
				g.pose().pushMatrix();
				float cx = V.x(640), cy = V.y(440);
				g.pose().translate(cx, cy);
				g.pose().rotate((ticks + partial) / 20F * (float) Math.PI * 2);
				g.pose().translate(-cx, -cy);
				V.icon(g, "icon_save", 600, 400, 80);
				g.pose().popMatrix();
			}
			case SAVE -> {
				g.fill(0, 0, width, height, 0x80061A1E);
				saveCard(g, d);
				TextBox.still("Would you like to save the game?").renderV(g, font, 140, 26, 1000, ticks, partial, false);
				vchoice(g, yesNo, V.W - 140 - 300, V.H - 26 - 150 - V.choiceHeight(yesNo.options.size()), 300, i -> {
					if (i == 0) save();
					else view = View.MENU;
				});
			}
			case OPTION -> options.draw(g, this::vhit, mx, my);
			case WAIT -> {
				TextBox.still("Wait until when?").renderV(g, font, 140, 26, 1000, ticks, partial, false);
				vchoice(g, waitChoice, V.W - 140 - 300, V.H - 26 - 150 - V.choiceHeight(waitChoice.options.size()), 300, this::waitUntil);
			}
			case MESSAGE -> {
				if (afterMessage == View.BAG) drawBag(g, d);
				else if (afterMessage == View.PARTY) drawParty(g, d);
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
	 * Your team as the UI Viewer has it: six cards, three across, each with a picture of the POKéMON, its name,
	 * level, types, status, HP; the one you're on cream and glowing; FOLLOWING YOU on your partner.
	 */
	private void drawParty(GuiGraphics g, PlayerData d) {
		Scenes.backdrop(g, "party");
		V.header(g, "icon_ball", "POKéMON", null);
		float cw = (1220 - 2 * 22) / 3F, ch = 262;
		for (int i = 0; i < d.party.size(); i++) {
			Mon mon = d.party.get(i);
			float x = 30 + (i % 3) * (cw + 22), y = 100 + (i / 3) * (ch + 22);
			int idx = i;
			if (vover(x, y, cw, ch) && (view == View.PARTY || view == View.USE_ON)) partyAt = i;
			boolean on = i == partyAt, fnt = mon.fainted();
			if (on) {
				V.rr(g, x - 4, y - 4, cw + 8, ch + 8, 30, 0x66FFF4D8);
				V.shadow(g, x, y, cw, ch, 26, 1.2F);
				V.rr(g, x, y, cw, ch, 26, 0xFFFFFFFF);
				V.rrv(g, x + 2.5F, y + 2.5F, cw - 5, ch - 5, 23.5F, 0xFFFFF8E6, 0xFFFFF0CC);
			} else V.panel(g, x, y, cw, ch, 26, false);
			if (i == swapFrom) V.ring(g, x, y, cw, ch, 26, 4, V.GOLD);
			int ink = on ? V.INK : V.TEXT;
			V.glow(g, x + 10 + 78, y + 8 + 97, 70, 70, on ? 0x3AFFD45A : 0x228FF5C0);
			V.img(g, Look.portrait(mon.species.id()), x + 10, y + 8, 156, 156, 128, 128, fnt ? 0x99FFFFFF : 0xFFFFFFFF);
			float tx = x + 18 + 160, ty = y + 18;
			V.text(g, mon.name(), tx, ty, 25, ink, true);
			String gm = mon.genderMark();
			if (gm != null && !gm.isEmpty()) V.text(g, gm, tx + V.width(mon.name(), 25, true) + 6, ty + 4, 20, gm.contains("\u2640") ? 0xFFFF8FB1 : 0xFF7FB8FF, true);
			V.text(g, "Lv. " + mon.level, tx, ty + 38, 20, ink, true);
			float cx = tx;
			cx += V.type(g, mon.species.type1().label, Ui.typeColor(mon.species.type1()), cx, ty + 70, 0, 13.5F) + 6;
			if (mon.species.type2() != null) V.type(g, mon.species.type2().label, Ui.typeColor(mon.species.type2()), cx, ty + 70, 0, 13.5F);
			float sx = tx;
			String st = fnt ? "FNT" : mon.status.tag;
			if (!st.isEmpty()) {
				float sw = V.width(st, 13, true) + 16;
				V.rr(g, sx, ty + 104, sw, 22, 7, Ui.statusColor(st));
				V.text(g, st, sx + 8, ty + 107, 13, 0xFFFFFFFF, true);
				sx += sw + 6;
			}
			if (i == d.follow) {
				float fw = V.width("FOLLOWING YOU", 12, true) + 20;
				V.pill(g, sx, ty + 105, fw, 21, on ? V.INK : V.EDGE_LIGHT);
				V.text(g, "FOLLOWING YOU", sx + 10, ty + 109, 12, on ? V.CREAM : 0xFF123C38, true);
			}
			float frac = mon.hp / (float) Math.max(1, mon.maxHp()), hy = y + ch - 18 - 24;
			V.text(g, "HP", x + 20, hy + 4, 14, on ? 0xFFB8860B : V.GOLD, true);
			String hpT = mon.hp + "/" + mon.maxHp();
			float hw = V.width(hpT, 19, true);
			V.bar(g, x + 20 + 34, hy + 5, cw - 40 - 34 - hw - 12, 14, frac, V.hpColour(frac));
			V.right(g, hpT, x + cw - 20, hy, 19, ink, true);
			if (view == View.PARTY) vhit(x, y, cw, ch, () -> pickParty(idx));
			if (view == View.USE_ON) vhit(x, y, cw, ch, () -> use(pocketItems().get(Math.min(bagAt, pocketItems().size() - 1)), idx));
		}
		V.hints(g, "A", "Choose", "B", "Back");
	}

	/** The summary's page: INFO, STATS or MOVES. */
	private int summaryPage;

	@Override
	protected boolean wantsTab() {
		return view == View.SUMMARY || view == View.BAG;
	}

	/**
	 * One of your team in full, as the UI Viewer's summary: it in 3D on a stand at the right; on the left its name,
	 * level and three pages: INFO (number, types, trainer, EXP), STATS (as bars) and MOVES (with types and PP).
	 */
	private void drawSummary(GuiGraphics g, PlayerData d) {
		if (d.party.isEmpty()) return;
		Mon mon = d.party.get(Math.min(partyAt, d.party.size() - 1));
		Species sp = mon.species;
		// The page in its own colour (its first type's), its name on a band of it.
		Scenes.typeBackdrop(g, sp.type1());
		Scenes.model(g, (ticks) / 20F, sp.id(), -1.05);
		float x = 28, y = 28, w = 640, h = 660, px = x + 28;
		V.panel(g, x, y, w, h);
		Scenes.typeBand(g, sp.type1(), x + 12, y + 12, w - 24, 64);
		float ty = y + 24;
		V.text(g, mon.name(), px, ty + 2, 36, 0x55000000, true);
		V.text(g, mon.name(), px, ty, 36, V.TEXT, true);
		float nx = px + V.width(mon.name(), 36, true) + 12;
		String gm = mon.genderMark();
		if (gm != null && !gm.isEmpty()) {
			V.text(g, gm, nx, ty + 10, 22, gm.contains("\u2640") ? 0xFFFF8FB1 : 0xFF7FB8FF, true);
			nx += V.width(gm, 22, true) + 10;
		}
		String st = mon.fainted() ? "FNT" : mon.status.tag;
		if (!st.isEmpty()) {
			float sw = V.width(st, 13, true) + 16;
			V.rr(g, nx, ty + 12, sw, 22, 7, Ui.statusColor(st));
			V.text(g, st, nx + 8, ty + 15, 13, 0xFFFFFFFF, true);
		}
		V.right(g, "Lv. " + mon.level, x + w - 28, ty + 8 + 2, 24, 0x55000000, true);
		V.right(g, "Lv. " + mon.level, x + w - 28, ty + 8, 24, V.TEXT, true);
		float cx = px, cy = ty + 50 + 14;
		String[] tabs = {"INFO", "STATS", "MOVES"};
		for (int t = 0; t < 3; t++) {
			int tt = t;
			float tw = V.chip(g, tabs[t], cx, cy, 15, t == summaryPage, false);
			vhit(cx, cy, tw, 26, () -> summaryPage = tt);
			cx += tw + 8;
		}
		float by = cy + 26 + 18;
		if (summaryPage == 0) {
			String[][] rows = {{"No.", String.format("%03d", sp.dex())}, {"Types", ""}, {"Trainer", name()}, {"Item", "None"}};
			for (int r = 0; r < rows.length; r++) {
				float ry = by + r * 40;
				V.label(g, rows[r][0], px, ry + 7);
				if (r == 1) {
					float tx = px + 140;
					tx += V.type(g, sp.type1().label, Ui.typeColor(sp.type1()), tx, ry + 2, 92, 13.5F) + 8;
					if (sp.type2() != null) V.type(g, sp.type2().label, Ui.typeColor(sp.type2()), tx, ry + 2, 92, 13.5F);
				} else V.text(g, rows[r][1], px + 140, ry, 21, V.TEXT, true);
			}
			int now = sp.growth().expAt(mon.level), next = mon.level >= 100 ? now : sp.growth().expAt(mon.level + 1);
			float ey = by + rows.length * 40 + 16;
			V.label(g, "EXP to Lv. " + Math.min(100, mon.level + 1) + "  (" + Math.max(0, next - mon.exp) + " more)", px, ey);
			V.pill(g, px, ey + 22, w - 56, 7, 0xFF0B2A2C);
			float ef = next <= now ? 1 : (mon.exp - now) / (float) (next - now);
			if (ef > 0) V.pill(g, px, ey + 22, Math.max(7, (w - 56) * ef), 7, 0xFF6FC3FF);
		} else if (summaryPage == 1) {
			String[] names = {"HP", "ATTACK", "DEFENSE", "SPCL.ATK", "SPCL.DEF", "SPEED"};
			int[] stats = {mon.maxHp(), mon.attack(), mon.defense(), mon.spAttack(), mon.spDefense(), mon.speed()};
			for (int i = 0; i < 6; i++) {
				float ry = by + i * 40;
				V.label(g, names[i], px, ry + 6);
				V.right(g, i == 0 ? mon.hp + "/" + stats[0] : String.valueOf(stats[i]), px + 150 + 70, ry, 19, V.TEXT, true);
				float f = Math.min(1, stats[i] / (mon.level * 2.6F + 30));
				V.pill(g, px + 150 + 82, ry + 3, w - 56 - 232, 16, 0xFF0B2A2C);
				int tc = Ui.typeColor(sp.type1()) | 0xFF000000;
				if (f > 0) V.rrv(g, px + 150 + 84, ry + 5, Math.max(12, (w - 56 - 236) * f), 12, 6, Ui.lighter(tc, 0.45F), tc);
			}
		} else {
			for (int i = 0; i < mon.moves.size(); i++) {
				Move mv = Move.get(mon.moves.get(i));
				float ry = by + i * 62;
				V.panel(g, px, ry, w - 56, 52, 18, true);
				float tw = V.type(g, mv.type().label, Ui.typeColor(mv.type()), px + 16, ry + 13, 92, 13.5F);
				V.text(g, mv.name(), px + 16 + tw + 14, ry + 13, 21, V.TEXT, true);
				V.right(g, "PP " + mon.pp.get(i) + "/" + mv.pp(), px + w - 56 - 16, ry + 15, 18, V.TEXT, true);
			}
		}
		vhit(x, y + h - 60, w, 60, () -> view = View.PARTY);
		V.hints(g, "TAB", "Page", "B", "Back");
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
		Scenes.backdrop(g, "bag");
		V.header(g, "icon_bag", "BAG", null);
		bagPicture(g, 48, 96);
		// The pockets' names under the bag.
		float tw = 0;
		for (String p : POCKETS) tw += V.chipWidth(p, 13) + 6;
		float cx = 48 + 200 - (tw - 6) / 2;
		for (int i = 0; i < POCKETS.length; i++) {
			int idx = i;
			float w = V.chip(g, POCKETS[i], cx, 96 + 500, 13, i == pocket, false);
			if (view == View.BAG) vhit(cx, 596, w, 24, () -> {
				pocket = idx;
				bagAt = 0;
				bagTop = 0;
			});
			cx += w + 6;
		}
		// What's in the picked pocket, with how many.
		List<String> l = pocketItems();
		float lx = 480, ly = 110, lw = 740, lh = 440;
		V.panel(g, lx, ly, lw, lh);
		int rows = 7, n = l.size() + 1;
		if (bagAt < bagTop) bagTop = bagAt;
		if (bagAt >= bagTop + rows) bagTop = bagAt - rows + 1;
		bagTop = Math.max(0, Math.min(bagTop, Math.max(0, n - rows)));
		for (int k = 0; k < rows && bagTop + k < n; k++) {
			int i = bagTop + k, idx = i;
			float ty = ly + 14 + k * 54;
			if (vover(lx + 14, ty, lw - 28, 46) && view == View.BAG) bagAt = i;
			boolean on = i == bagAt;
			if (i == l.size()) V.item(g, "CLOSE BAG", null, lx + 14, ty, lw - 28, 46, on, !on, 21);
			else {
				Item it = Item.find(l.get(i));
				V.goods(g, it.name(), "x" + d.count(it.id()), lx + 14, ty, lw - 28, 46, on, 21);
			}
			if (view == View.BAG) vhit(lx + 14, ty, lw - 28, 46, () -> pickBag(idx));
		}
		if (bagTop > 0) V.tri(g, lx + lw / 2, ly + 6, 14, 3, V.MUTED);
		if (bagTop + rows < n) V.tri(g, lx + lw / 2, ly + lh - 6, 14, 1, V.MUTED);
		if (l.isEmpty()) V.text(g, "Empty", lx + 34, ly + 14 + 54 + 12, 21, V.MUTED, true);
		// What it does.
		V.panel(g, 480, 570, 740, 110, 24, true);
		String desc = bagAt < l.size() ? BattleScreen.describe(l.get(bagAt)) : "Put the bag away.";
		List<String> dl = V.wrap(desc, 740 - 52, 21, true);
		for (int i = 0; i < Math.min(2, dl.size()); i++) V.text(g, dl.get(i), 506, 570 + 20 + i * 30, 21, V.TEXT, true);
		V.hints(g, "A", "Choose", "B", "Back", "TAB", "Pocket");
	}

	/**
	 * Your backpack (the viewer's picture), in its colour: its body (ITEMS), front pocket (BALLS), lid (KEY ITEMS)
	 * and side pocket (TM & HM); the picked one lifted with a cream outline. Clicking a pocket picks it.
	 */
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
	private void drawCard(GuiGraphics g, PlayerData d) {
		Scenes.backdrop(g, "card");
		float secs = (ticks) / 20F;
		boolean girl = "girl".equals(d.gender);
		Scenes.stand(g, secs, Scenes.you(d.outfit, girl), 2.1, 0, true);
		float x = 470, y = 60, w = 770, h = 600;
		V.panel(g, x, y, w, h);
		vhit(x, y, w, h, () -> view = View.MENU);
		V.icon(g, "icon_card", x + 34, y + 28, 46);
		V.text(g, "TRAINER CARD", x + 34 + 46 + 14, y + 34, 34, V.TEXT, true);
		String[][] rows = {{"Name", name()}, {"ID No.", String.format("%05d", Math.abs(name().hashCode()) % 100000)}, {"Money", Ui.money(d.money)},
				{"Pokédex", d.hasDex() ? String.valueOf(d.caught.size()) : "-"}, {"Play time", String.format("%d:%02d", (int) d.played / 3600, (int) d.played / 60 % 60)}};
		float ry = y + 28 + 46 + 16;
		for (String[] r : rows) {
			V.label(g, r[0], x + 34, ry + 7);
			V.text(g, r[1], x + 34 + 220, ry, 23, V.TEXT, true);
			ry += 23 * 1.3F + 12;
		}
		ry += 6;
		V.label(g, "Badges", x + 34, ry);
		String[] badges = {"BOULDERBADGE", "CASCADEBADGE", "THUNDERBADGE", "RAINBOWBADGE", "SOULBADGE", "MARSHBADGE", "VOLCANOBADGE", "EARTHBADGE"};
		float bw = (w - 68 - 7 * 10) / 8, by = ry + 26;
		for (int i = 0; i < 8; i++) {
			boolean got = d.badges.contains(badges[i]);
			float bx = x + 34 + i * (bw + 10);
			V.rr(g, bx, by, bw, bw, 18, 0x33000000);
			V.icon(g, "badge_" + i, bx + (bw - 66) / 2, by + (bw - 66) / 2, 66, got ? 0xFFFFFFFF : 0x8C55595E);
		}
		V.hints(g, "B", "Back");
	}

	/** What each option does (the viewer's top panel). */
	/** OPTION's rows (the UI Viewer's, with ours for your partner and the look), what each does, and their values. */
	private static final String[] OPTION_LABELS = {"TEXT SPEED", "MUSIC", "SOUND", "POKéDEX VOICE", "NIGHTS", "WILD POKéMON", "BATTLE EFFECTS", "BATTLE STYLE",
			"WALK WITH YOU", "POKéMON LOOK", "SHADERS", "CONTROLS"};
	private static final String[] OPTION_LINES = {"How quickly words appear in text boxes.", "How loud the music plays.", "How loud cries and menu sounds are.",
			"Whether the POKéDEX reads its entries out loud.", "How dark nights get outside.", "Whether wild POKéMON show in the grass, or jump out at you.",
			"Whether moves play their animations in battle.", "SWITCH asks if you want to change POKéMON when a trainer is about to send out their next one.",
			"Whether your partner walks with you.",
			"COBBLEMON shows the POKéMON as Cobblemon draws them; RETROMON shows our own creatures.",
			"RETROMON is our own light: the sun moves with the day, soft shadows, swaying grass, blue nights and glowing lamps. OFF is Minecraft's own.", "See every key: what it does in game, and what's turned off."};
	private static final String[][] OPTION_VALUES = {{"SLOW", "MID", "FAST"}, {"OFF", "LOW", "MID", "HIGH", "MAX"}, {"OFF", "LOW", "MID", "HIGH", "MAX"}, {"OFF", "ON"},
			{"DARK", "SOFT", "BRIGHT"}, {"HIDDEN", "SEEN"}, {"OFF", "ON"}, {"SWITCH", "SET"}, {"OFF", "ON"}, {"COBBLEMON", "RETROMON"}, {"OFF", "RETROMON"}, {}};

	/**
	 * OPTION, as the UI Viewer's: what the row does in a panel at the top, then the rows (the one you're on cream),
	 * each with its value between arrows; left and right (or a click) change it. CONTROLS opens the key map.
	 */
	private void drawOptions(GuiGraphics g, PlayerData d) {
		Scenes.backdrop(g, "sky");
		int n = OPTION_LABELS.length;
		optionRow = Math.max(0, Math.min(n - 1, optionRow));
		V.panel(g, 170, 40, 940, 96);
		List<String> dl = V.wrap(OPTION_LINES[optionRow], 880, 22, true);
		for (int i = 0; i < Math.min(2, dl.size()); i++) V.text(g, dl.get(i), 200, 62 + i * 30, 22, V.TEXT, true);
		float rh = n > 11 ? 42 : 46;
		V.panel(g, 170, 160, 940, n * rh + 36);
		int[] now = {d.textSpeed, Music.level(), Settings.sound(), Settings.voice() ? 1 : 0, Settings.nights(), Settings.seen() ? 1 : 0, Settings.effects() ? 1 : 0,
				Settings.switchStyle() ? 0 : 1, Settings.follower() ? 1 : 0, Settings.look(), Settings.shaderMode(), 0};
		for (int r = 0; r < n; r++) {
			float ry = 160 + 18 + r * rh, rx = 188, rw = 904;
			if (vover(rx, ry, rw, rh)) optionRow = r;
			boolean on = r == optionRow;
			V.item(g, OPTION_LABELS[r], null, rx, ry, rw, rh, on, false, 21);
			int c = on ? V.INK : V.TEXT;
			if (OPTION_VALUES[r].length == 0) V.tri(g, rx + rw - 22 - 6, ry + rh / 2, 12, 0, Ui.alpha(c, 0.6F));
			else {
				String v = OPTION_VALUES[r][Math.max(0, Math.min(OPTION_VALUES[r].length - 1, now[r]))];
				float vx = rx + rw - 22 - 130;
				V.tri(g, vx - 14, ry + rh / 2, 12, 2, Ui.alpha(c, 0.6F));
				V.centred(g, v, vx + 65, ry + (rh - 21 * 1.15F) / 2, 21, c, true);
				V.tri(g, vx + 130 + 14, ry + rh / 2, 12, 0, Ui.alpha(c, 0.6F));
			}
			int row = r;
			vhit(rx, ry, rw, rh, () -> {
				optionRow = row;
				changeOption(row, 1);
			});
		}
		V.hints(g, "A", "Change", "B", "Back");
	}

	/** Changes an option by a step (left -1, right or A +1); CONTROLS opens the key map. */
	private void changeOption(int row, int step) {
		PlayerData d = ClientState.data;
		switch (row) {
			case 0 -> setSpeed(Math.floorMod(d.textSpeed + step, 3));
			case 1 -> Music.setLevel(Math.floorMod(Music.level() + step, 5));
			case 2 -> Settings.set("sound", Math.floorMod(Settings.sound() + step, 5));
			case 3 -> Settings.set("voice", Settings.voice() ? 0 : 1);
			case 4 -> {
				Settings.set("nights", Math.floorMod(Settings.nights() + step, 3));
				Night.send();
			}
			case 5 -> {
				Settings.set("seen", Settings.seen() ? 0 : 1);
				Settings.sendPrefs();
			}
			case 6 -> Settings.set("effects", Settings.effects() ? 0 : 1);
			case 7 -> Settings.set("style", Settings.switchStyle() ? 1 : 0);
			case 8 -> {
				Settings.set("follow", Settings.follower() ? 0 : 1);
				Settings.sendPrefs();
			}
			case 9 -> Settings.set("look", Settings.look() == 1 ? 0 : 1);
			case 10 -> {
				Settings.set("shaders", Settings.shaderMode() == 0 ? 1 : 0);
				Shaders.set(Settings.shaderMode());
			}
			default -> {
				Sounds.press();
				if (minecraft != null) minecraft.setScreen(new KeyMapScreen(this));
				return;
			}
		}
		Sounds.press();
	}

	@SuppressWarnings("unused")
	private static Type none() {
		return null;
	}
}
