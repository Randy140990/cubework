package com.insidethecube.retromon.client;

import java.util.ArrayList;
import java.util.List;

import com.insidethecube.retromon.engine.Campaign;
import com.insidethecube.retromon.engine.Mon;
import com.insidethecube.retromon.engine.Move;
import com.insidethecube.retromon.engine.Species;
import com.insidethecube.retromon.game.Net;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.lwjgl.glfw.GLFW;

/**
 * The PARTY PICKER (OPTION, for John's videos, 15:28): on the left, your starter, whether your badges match, the
 * points in KANTO (STARTING OUT, AFTER BROCK ... CHAMPION), CUSTOM and MY OWN PARTY; on the right, the party you'd
 * get, each with its picture, level, types and moves. A uses it; R rolls a slightly different one. CUSTOM starts from
 * the party shown and lets you change each one's species and level. Your own party is kept, and MY OWN PARTY brings it
 * (and your badges) back.
 */
public class PartyPickerScreen extends GameScreen {
	private final Screen parent;
	private static final int STARTER = 0, BADGES = 1, FIRST_STAGE = 2, CUSTOM = FIRST_STAGE + Campaign.STAGES.size(), OWN = CUSTOM + 1, ROWS = OWN + 1;
	private int row = FIRST_STAGE, starter, stage, slot;
	private boolean badges = true, editing;
	/** Each point's roll (R rolls it again). */
	private final long[] seeds = new long[Campaign.STAGES.size()];
	private List<Mon> shown = List.of();
	private String shownKey = "";
	/** CUSTOM's party: species and level, each slot (null: empty). */
	private final String[] customSp = new String[6];
	private final int[] customLv = new int[6];
	private String done = "";
	private int doneAt = -100;
	private static List<Species> ALL_SPECIES;

	public PartyPickerScreen(Screen parent) {
		super("Party picker");
		this.parent = parent;
		java.util.Random r = new java.util.Random();
		for (int i = 0; i < seeds.length; i++) seeds[i] = r.nextInt(1_000_000);
		String mine = mineStarter();
		for (int i = 0; i < Campaign.STARTERS.length; i++) if (Campaign.STARTERS[i].equals(mine)) starter = i;
		if (ClientState.data.picking) {
			for (int i = 0; i < Campaign.STAGES.size(); i++) if (Campaign.STAGES.get(i).title().equals(ClientState.data.pickedAs)) stage = i;
			row = FIRST_STAGE + stage;
		}
	}

	/** Your starter: the first of the starters' lines in your own party (or box). */
	private static String mineStarter() {
		List<Mon> look = new ArrayList<>(ClientState.data.picking ? ClientState.data.ownParty : ClientState.data.party);
		look.addAll(ClientState.data.box);
		for (Mon m : look) {
			String s = Campaign.starterOf(m.species.id());
			if (s != null) return s;
		}
		return "bulbasaur";
	}

	private static List<Species> allSpecies() {
		if (ALL_SPECIES == null) ALL_SPECIES = new ArrayList<>(Species.ALL.values());
		return ALL_SPECIES;
	}

	/** The party shown: the point's (worked out as the game will), or CUSTOM's. */
	private List<Mon> party() {
		String key = editing ? "custom" + java.util.Arrays.toString(customSp) + java.util.Arrays.toString(customLv) + stage
				: stage + ":" + Campaign.STARTERS[starter] + ":" + seeds[stage];
		if (!key.equals(shownKey)) {
			shownKey = key;
			shown = Campaign.make(entries(), stage, seeds[stage]);
		}
		return shown;
	}

	private List<Campaign.Entry> entries() {
		if (editing) {
			List<Campaign.Entry> out = new ArrayList<>();
			for (int i = 0; i < 6; i++) if (customSp[i] != null) out.add(new Campaign.Entry(customSp[i], customLv[i]));
			return out;
		}
		return Campaign.pick(stage, Campaign.STARTERS[starter], seeds[stage]);
	}

	/** CUSTOM begins from the party on show. */
	private void startCustom() {
		List<Campaign.Entry> from = Campaign.pick(stage, Campaign.STARTERS[starter], seeds[stage]);
		for (int i = 0; i < 6; i++) {
			customSp[i] = i < from.size() ? from.get(i).species() : null;
			customLv[i] = i < from.size() ? from.get(i).level() : Campaign.STAGES.get(stage).lo();
		}
		editing = true;
		slot = 0;
	}

	private void use() {
		CompoundTag t = new CompoundTag();
		if (row == OWN) {
			if (!ClientState.data.picking) return;
			t.putString("mode", "own");
			say("Your own party is back.");
		} else if (editing || row == CUSTOM) {
			if (!editing) {
				startCustom();
				Sounds.press();
				return;
			}
			ListTag l = new ListTag();
			for (int i = 0; i < 6; i++) {
				if (customSp[i] == null) continue;
				CompoundTag c = new CompoundTag();
				c.putString("sp", customSp[i]);
				c.putInt("lv", customLv[i]);
				l.add(c);
			}
			if (l.isEmpty()) return;
			t.putString("mode", "custom");
			t.put("party", l);
			say("Your party is now your own pick.");
		} else if (row >= FIRST_STAGE && row < CUSTOM) {
			t.putString("mode", "point");
			t.putString("starter", Campaign.STARTERS[starter]);
			say("Your party is now " + Campaign.STAGES.get(stage).title() + "'s.");
		} else return;
		t.putInt("stage", stage);
		t.putLong("seed", seeds[stage]);
		t.putBoolean("badges", badges);
		Sounds.press();
		if (minecraft != null && minecraft.getConnection() != null) ClientPlayNetworking.send(new Net.Act("pick_party", t));
	}

	private void say(String s) {
		done = s;
		doneAt = ticks;
	}

	private void changeSpecies(int step) {
		List<Species> all = allSpecies();
		int at = -1;
		for (int i = 0; i < all.size(); i++) if (all.get(i).id().equals(customSp[slot])) at = i;
		at = at < 0 ? (step > 0 ? 0 : all.size() - 1) : Math.floorMod(at + step, all.size());
		customSp[slot] = all.get(at).id();
		if (customLv[slot] < 2) customLv[slot] = Campaign.STAGES.get(stage).lo();
		Sounds.select();
	}

	private void changeLevel(int step) {
		if (customSp[slot] == null) return;
		customLv[slot] = Math.max(2, Math.min(100, customLv[slot] + step));
		Sounds.select();
	}

	@Override
	protected boolean key(KeyEvent e) {
		boolean shift = (e.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0;
		if (editing) {
			if (Ui.isB(e)) {
				editing = false;
				Sounds.press();
			} else if (Ui.isUp(e) || Ui.isDown(e)) {
				slot = Math.floorMod(slot + (Ui.isUp(e) ? -1 : 1), 6);
				Sounds.select();
			} else if (Ui.isLeft(e) || Ui.isRight(e)) changeSpecies((Ui.isLeft(e) ? -1 : 1) * (shift ? 10 : 1));
			else if (e.key() == GLFW.GLFW_KEY_MINUS) changeLevel(shift ? -10 : -1);
			else if (e.key() == GLFW.GLFW_KEY_EQUAL) changeLevel(shift ? 10 : 1);
			else if (e.key() == GLFW.GLFW_KEY_DELETE) {
				customSp[slot] = null;
				Sounds.press();
			} else if (Ui.isA(e)) use();
			return true;
		}
		if (Ui.isB(e)) {
			back();
			return true;
		}
		if (Ui.isUp(e) || Ui.isDown(e)) {
			row = Math.floorMod(row + (Ui.isUp(e) ? -1 : 1), ROWS);
			if (row >= FIRST_STAGE && row < CUSTOM) stage = row - FIRST_STAGE;
			Sounds.select();
		} else if (Ui.isLeft(e) || Ui.isRight(e)) {
			int step = Ui.isLeft(e) ? -1 : 1;
			if (row == STARTER) starter = Math.floorMod(starter + step, Campaign.STARTERS.length);
			else if (row == BADGES) badges = !badges;
			else return true;
			Sounds.press();
		} else if (e.key() == GLFW.GLFW_KEY_R) {
			seeds[stage] = new java.util.Random().nextInt(1_000_000);
			Sounds.press();
		} else if (Ui.isA(e)) {
			if (row == STARTER) starter = Math.floorMod(starter + 1, Campaign.STARTERS.length);
			else if (row == BADGES) badges = !badges;
			else {
				use();
				return true;
			}
			Sounds.press();
		}
		return true;
	}

	@Override
	protected void back() {
		Sounds.press();
		if (minecraft != null) minecraft.setScreen(parent);
	}

	@Override
	public void onClose() {
		back();
	}

	private static String speciesName(String id) {
		Species s = Species.ALL.get(id);
		return s == null ? id.toUpperCase() : Look.text(s.name());
	}

	@Override
	protected void draw(GuiGraphics g, float partial) {
		Scenes.bg2(g, "options");
		// The top: what this is, and what you're using now.
		V.panel(g, 40, 24, 1200, 80);
		V.title(g, "PARTY PICKER", 64, V.capsTop(52, 30), 30, V.INK);
		V.text(g, "For your videos: a party from any point in KANTO, or your own pick. Your own party is kept aside.", 64, 72, 17, V.MUTED, false);
		String now = ClientState.data.picking ? "NOW: " + ClientState.data.pickedAs : "NOW: YOUR OWN PARTY";
		float nw = V.width(now, 16, true) + 32;
		V.pill(g, 40 + 1200 - 24 - nw, 46, nw, 34, ClientState.data.picking ? V.AMBER : V.GREEN);
		V.text(g, now, 40 + 1200 - 24 - nw + 16, 54, 16, V.WHITE, true);

		// The left: the rows.
		float lx = 40, ly = 118, lw = 420, rh = 35;
		V.panel(g, lx, ly, lw, ROWS * rh + 30);
		for (int r = 0; r < ROWS; r++) {
			float ry = ly + 15 + r * rh, rx = lx + 14, rw = lw - 28;
			boolean on = r == row && !editing;
			String label, right = null;
			boolean off = false;
			if (r == STARTER) {
				label = "STARTER";
				right = speciesName(Campaign.STARTERS[starter]);
			} else if (r == BADGES) {
				label = "BADGES";
				right = badges ? "MATCH" : "KEEP MINE";
			} else if (r < CUSTOM) label = Campaign.STAGES.get(r - FIRST_STAGE).title();
			else if (r == CUSTOM) label = "CUSTOM";
			else {
				label = "MY OWN PARTY";
				off = !ClientState.data.picking;
			}
			if (V.over(mx, my, rx, ry, rw, rh) && !editing) {
				row = r;
				if (r >= FIRST_STAGE && r < CUSTOM) stage = r - FIRST_STAGE;
			}
			if (!on && r != row - 1 && r < ROWS - 1) V.rr(g, rx, ry + rh - 1.5F, rw, 1.5F, 0, 0xFFEEF1F8);
			V.item(g, label, null, rx, ry, rw, rh, on, off, 19);
			if (right != null) {
				float cy = ry + rh / 2, vr = rx + rw - 18;
				V.tri(g, vr - 150, cy, 10, 2, V.RED);
				V.centred(g, right, vr - 78, V.capsTop(cy, 17), 17, V.INK, true);
				V.tri(g, vr - 6, cy, 10, 0, V.RED);
			}
			int rr = r;
			vhit(rx, ry, rw, rh, () -> {
				if (editing) return;
				row = rr;
				if (rr == STARTER) starter = Math.floorMod(starter + 1, Campaign.STARTERS.length);
				else if (rr == BADGES) badges = !badges;
				else {
					if (rr >= FIRST_STAGE && rr < CUSTOM) stage = rr - FIRST_STAGE;
					use();
				}
			});
		}

		// The right: the party you'd get.
		float px = 476, py = 118, pw = 764, ph = ROWS * rh + 30;
		V.panel(g, px, py, pw, ph);
		String head, line;
		if (row == OWN) {
			head = "MY OWN PARTY";
			line = ClientState.data.picking ? "A brings back your own party and your own badges. Anything you've caught since comes too."
					: "You're using your own party.";
		} else if (editing || row == CUSTOM) {
			head = "CUSTOM";
			line = editing ? "UP/DOWN a slot, LEFT/RIGHT its POKéMON (SHIFT: 10 at a time), - and = its level, DELETE empties it, A uses it."
					: "A to change each one: it starts from the party of " + Campaign.STAGES.get(stage).title() + ".";
		} else if (row == STARTER || row == BADGES) {
			head = row == STARTER ? "YOUR STARTER" : "BADGES";
			line = row == STARTER ? "The POKéMON whose line leads every party." : "MATCH: the badges you'd have by then (FLY needs the THUNDERBADGE). KEEP MINE: yours.";
		} else {
			Campaign.Stage st = Campaign.STAGES.get(stage);
			head = st.title();
			line = st.line() + " R for a slightly different party.";
		}
		V.title(g, head, px + 24, V.capsTop(py + 30, 24), 24, V.INK);
		List<String> ll = V.wrap(line, pw - 48, 16, false);
		for (int i = 0; i < Math.min(2, ll.size()); i++) V.text(g, ll.get(i), px + 24, py + 50 + i * 21, 16, V.MUTED, false);
		List<Mon> team = row == OWN ? (ClientState.data.picking ? ClientState.data.ownParty : ClientState.data.party) : party();
		float cw = (pw - 48 - 14) / 2F, ch = (ph - 104 - 20) / 3F;
		for (int i = 0; i < 6; i++) {
			float cx = px + 24 + (i % 2) * (cw + 14), cy = py + 98 + (i / 2) * (ch + 10);
			boolean sel = editing && i == slot;
			if (sel) {
				V.softShadow(g, cx, cy + 4, cw, ch, 12, 0x59E8961E);
				V.rrv(g, cx - 3, cy - 3, cw + 6, ch + 6, 18, V.SEL_TOP, V.SEL_BOTTOM);
			}
			Mon m = i < team.size() ? team.get(i) : null;
			if (editing && customSp[i] == null) m = null;
			int tint = m == null ? 0xFFF4F6FB : Ui.mix(Ui.typeColor(m.species.type1()) | 0xFF000000, 0xFFFFFFFF, 0.82F);
			V.rr(g, cx, cy, cw, ch, 16, tint);
			int ii = i;
			if (editing) vhit(cx, cy, cw, ch, () -> slot = ii);
			if (m == null) {
				V.centred(g, editing ? "EMPTY: LEFT/RIGHT to add one" : "-", cx + cw / 2, V.capsTop(cy + ch / 2, 16), 16, V.DIM, false);
				continue;
			}
			float pic = Math.min(ch - 16, 104);
			V.img(g, Look.portrait(m.species.id()), cx + 8, cy + (ch - pic) / 2, pic, pic, 128, 128, 0xFFFFFFFF);
			float tx = cx + 16 + pic;
			V.text(g, Look.text(m.species.name()), tx, cy + 10, 19, V.INK, true);
			V.right(g, "Lv " + m.level, cx + cw - 14, cy + 11, 17, V.INK, true);
			// Its types.
			float tyx = tx;
			for (com.insidethecube.retromon.engine.Type ty : new com.insidethecube.retromon.engine.Type[] {m.species.type1(), m.species.type2()}) {
				if (ty == null) continue;
				String tn = ty.name();
				float w = V.width(tn, 12, true) + 16;
				V.pill(g, tyx, cy + 36, w, 20, Ui.typeColor(ty) | 0xFF000000);
				V.text(g, tn, tyx + 8, cy + 39, 12, V.WHITE, true);
				tyx += w + 6;
			}
			// Its moves, two by two.
			for (int k = 0; k < m.moves.size() && k < 4; k++) {
				Move mv = Move.ALL.get(m.moves.get(k));
				String mn = mv == null ? m.moves.get(k).toUpperCase() : mv.name();
				V.text(g, mn, tx + (k % 2) * ((cx + cw - 14 - tx) / 2), cy + 64 + (k / 2) * 20, 14, Campaign.isHm(m.moves.get(k)) ? V.BLUE : V.TEXT, false);
			}
			if (sel) {
				// Buttons for the mouse: the species back and on, the level down and up.
				float by = cy + ch - 30;
				String[] labels = {"<", ">", "-", "+"};
				Runnable[] acts = {() -> changeSpecies(-1), () -> changeSpecies(1), () -> changeLevel(-1), () -> changeLevel(1)};
				for (int b = 0; b < 4; b++) {
					float bx = tx + b * 40;
					V.pill(g, bx, by, 34, 24, 0xFFFFFFFF);
					V.centred(g, labels[b], bx + 17, by + 4, 16, V.INK, true);
					vhit(bx, by, 34, 24, acts[b]);
				}
			}
		}
		if (ticks - doneAt < 80 && !done.isEmpty()) {
			float w = V.width(done, 18, true) + 40;
			V.pill(g, 640 - w / 2, 610, w, 38, V.GREEN);
			V.centred(g, done, 640, 619, 18, V.WHITE, true);
		}
		if (editing) V.hints(g, "A", "Use this party", "B", "Done");
		else V.hints(g, "A", row == CUSTOM ? "Change" : "Use this party", "R", "Roll again", "B", "Back");
	}
}
