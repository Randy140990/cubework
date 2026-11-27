package com.insidethecube.retromon.client;

import java.util.ArrayList;
import java.util.List;

import com.insidethecube.retromon.engine.Campaign;
import com.insidethecube.retromon.engine.Species;
import com.insidethecube.retromon.game.Net;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.nbt.CompoundTag;
import org.lwjgl.glfw.GLFW;

/**
 * (0.71) The EVENT PICKER (OPTION, John's 18:06: "allow me to select one and I should be transferred to before or in
 * it to experience it"; 18:24: "as an admin for now"): every step of the Kanto Campaign Events sheet down the left,
 * the one you're on on the right (where, what happens, what you need and get), and how to go: BEFORE IT or IN IT,
 * with that point's party (as the PARTY PICKER makes it) or your own, and your starter. A goes. Your own game is kept
 * aside the first time; MY OWN GAME brings it back, and you where you were.
 */
public class EventPickerScreen extends GameScreen {
	private final Screen parent;

	/** A step: its number, its party's point, main or side, where, what happens, what you need and get. */
	private record Row(int n, int stage, boolean main, String stageName, String where, String what, String need, String get) {}

	private static List<Row> ROWS;
	private int at, top;
	private boolean in, keepParty;
	private int starter;
	private static final int SHOWN = 13;

	public EventPickerScreen(Screen parent) {
		super("Event picker");
		this.parent = parent;
		rows();
		// Where you are now, if you're at a step.
		String as = ClientState.data.eventAs;
		if (as != null && as.startsWith("STEP ")) {
			try {
				int n = Integer.parseInt(as.substring(5).split(" ")[0]);
				for (int i = 0; i < ROWS.size(); i++) if (ROWS.get(i).n() == n) at = i;
			} catch (NumberFormatException ignored) {}
		}
		top = Math.max(0, at - SHOWN / 2);
		var mine = new ArrayList<>(ClientState.data.party);
		mine.addAll(ClientState.data.box);
		for (var m : mine) {
			String s = Campaign.starterOf(m.species.id());
			for (int i = 0; i < Campaign.STARTERS.length; i++) if (Campaign.STARTERS[i].equals(s)) {
				starter = i;
				break;
			}
		}
	}

	private static List<Row> rows() {
		if (ROWS == null) {
			List<Row> out = new ArrayList<>();
			for (String[] r : Species.table("kanto_events.tsv")) {
				if (r.length < 11) continue;
				try {
					out.add(new Row(Integer.parseInt(r[0].trim()), Integer.parseInt(r[1].trim()), r[2].trim().equals("Main"), r[6], r[7], r[8], r[9], r[10]));
				} catch (NumberFormatException ignored) {}
			}
			ROWS = out;
		}
		return ROWS;
	}

	private Row row() {
		return ROWS.isEmpty() ? null : ROWS.get(Math.max(0, Math.min(ROWS.size() - 1, at)));
	}

	private void move(int step) {
		if (ROWS.isEmpty()) return;
		at = Math.max(0, Math.min(ROWS.size() - 1, at + step));
		if (at < top) top = at;
		if (at >= top + SHOWN) top = at - SHOWN + 1;
		Sounds.select();
	}

	private void go() {
		Row r = row();
		if (r == null || minecraft == null || minecraft.getConnection() == null) return;
		CompoundTag t = new CompoundTag();
		t.putString("mode", "go");
		t.putInt("step", r.n());
		t.putBoolean("in", in);
		t.putBoolean("keepParty", keepParty);
		t.putString("starter", Campaign.STARTERS[starter]);
		Sounds.press();
		ClientPlayNetworking.send(new Net.Act("event", t));
		minecraft.setScreen(null);
	}

	private void own() {
		if (ClientState.data.ownGame == null || minecraft == null || minecraft.getConnection() == null) return;
		CompoundTag t = new CompoundTag();
		t.putString("mode", "own");
		Sounds.press();
		ClientPlayNetworking.send(new Net.Act("event", t));
		minecraft.setScreen(null);
	}

	@Override
	protected boolean key(KeyEvent e) {
		boolean shift = (e.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0;
		if (Ui.isB(e)) back();
		else if (Ui.isUp(e)) move(shift ? -10 : -1);
		else if (Ui.isDown(e)) move(shift ? 10 : 1);
		else if (e.key() == GLFW.GLFW_KEY_PAGE_UP) move(-SHOWN);
		else if (e.key() == GLFW.GLFW_KEY_PAGE_DOWN) move(SHOWN);
		else if (Ui.isLeft(e) || Ui.isRight(e)) {
			in = !in;
			Sounds.press();
		} else if (e.key() == GLFW.GLFW_KEY_P) {
			keepParty = !keepParty;
			Sounds.press();
		} else if (e.key() == GLFW.GLFW_KEY_S) {
			starter = Math.floorMod(starter + 1, Campaign.STARTERS.length);
			Sounds.press();
		} else if (e.key() == GLFW.GLFW_KEY_O) own();
		else if (Ui.isA(e)) go();
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

	@Override
	public boolean mouseScrolled(double x, double y, double dx, double dy) {
		top = Math.max(0, Math.min(Math.max(0, ROWS.size() - SHOWN), top - (int) Math.signum(dy) * 3));
		return true;
	}

	private static String speciesName(String id) {
		Species s = Species.ALL.get(id);
		return s == null ? id.toUpperCase() : Look.text(s.name());
	}

	@Override
	protected void draw(GuiGraphics g, float partial) {
		Scenes.bg2(g, "options");
		boolean away = ClientState.data.ownGame != null;
		// The top: what this is, and where you are.
		V.panel(g, 40, 24, 1200, 80);
		V.title(g, "EVENT PICKER", 64, V.capsTop(52, 30), 30, V.INK);
		V.text(g, "Go to any step of KANTO's story: just before it, or into it. Your own game is kept aside.", 64, 72, 17, V.MUTED, false);
		String now = away ? "NOW: " + ClientState.data.eventAs : "NOW: YOUR OWN GAME";
		float nw = V.width(now, 16, true) + 32;
		V.pill(g, 40 + 1200 - 24 - nw, 46, nw, 34, away ? V.AMBER : V.GREEN);
		V.text(g, now, 40 + 1200 - 24 - nw + 16, 54, 16, V.WHITE, true);

		// The left: the steps.
		float lx = 40, ly = 118, lw = 560, rh = 38;
		V.panel(g, lx, ly, lw, SHOWN * rh + 30);
		for (int i = 0; i < SHOWN && top + i < ROWS.size(); i++) {
			int idx = top + i;
			Row r = ROWS.get(idx);
			float ry = ly + 15 + i * rh, rx = lx + 14, rw = lw - 28;
			boolean on = idx == at;
			if (V.over(mx, my, rx, ry, rw, rh)) at = idx;
			if (!on && idx != at - 1 && i < SHOWN - 1) V.rr(g, rx, ry + rh - 1.5F, rw, 1.5F, 0, 0xFFEEF1F8);
			V.item(g, "", null, rx, ry, rw, rh, on, false, 18);
			String num = String.valueOf(r.n());
			V.pill(g, rx + 10, ry + 7, 44, rh - 14, r.main() ? V.BLUE : V.DIM);
			V.centred(g, num, rx + 32, V.capsTop(ry + rh / 2, 15), 15, V.WHITE, true);
			String where = r.where();
			while (V.width(where, 17, on) > rw - 190 && where.length() > 4) where = where.substring(0, where.length() - 2);
			if (!where.equals(r.where())) where = where.trim() + "...";
			V.text(g, where, rx + 66, V.capsTop(ry + rh / 2, 17), 17, V.INK, on);
			V.right(g, r.stageName().toUpperCase(), rx + rw - 12, V.capsTop(ry + rh / 2, 13), 13, V.MUTED, true);
			int ii = idx;
			vhit(rx, ry, rw, rh, () -> {
				if (at == ii) go();
				else at = ii;
			});
		}
		// Its scroll bar.
		if (ROWS.size() > SHOWN) {
			float th = SHOWN * rh, bh = Math.max(30, th * SHOWN / ROWS.size()), by = ly + 15 + (th - bh) * top / Math.max(1, ROWS.size() - SHOWN);
			V.rr(g, lx + lw - 8, ly + 15, 4, th, 2, 0xFFEEF1F8);
			V.rr(g, lx + lw - 8, by, 4, bh, 2, V.DIM);
		}

		// The right: the step.
		float px = 616, py = 118, pw = 624, ph = SHOWN * rh + 30;
		V.panel(g, px, py, pw, ph);
		Row r = row();
		if (r != null) {
			String chip = "STEP " + r.n() + "  " + r.stageName().toUpperCase() + (r.main() ? "" : "  SIDE");
			float cw = V.width(chip, 14, true) + 28;
			V.pill(g, px + 24, py + 20, cw, 28, r.main() ? V.BLUE : V.DIM);
			V.text(g, chip, px + 38, py + 27, 14, V.WHITE, true);
			List<String> wl = V.wrap(r.where(), pw - 48, 24, true);
			for (int i = 0; i < Math.min(2, wl.size()); i++) V.title(g, wl.get(i), px + 24, py + 62 + i * 30, 24, V.INK);
			float y = py + 66 + Math.min(2, wl.size()) * 30;
			List<String> dl = V.wrap(r.what(), pw - 48, 16, false);
			for (int i = 0; i < Math.min(4, dl.size()); i++) V.text(g, dl.get(i), px + 24, y + i * 22, 16, V.TEXT, false);
			y += Math.min(4, dl.size()) * 22 + 10;
			if (!r.need().isEmpty() && !r.need().equals("-")) {
				V.text(g, "YOU NEED", px + 24, y, 13, V.MUTED, true);
				V.text(g, cut(r.need(), pw - 160), px + 120, y - 1, 15, V.INK, false);
				y += 24;
			}
			if (!r.get().isEmpty() && !r.get().equals("-")) {
				V.text(g, "YOU GET", px + 24, y, 13, V.MUTED, true);
				V.text(g, cut(r.get(), pw - 160), px + 120, y - 1, 15, V.INK, false);
				y += 24;
			}
			// How you go: three rows, each with its value between red arrows.
			Campaign.Stage st = Campaign.STAGES.get(Math.max(0, Math.min(Campaign.STAGES.size() - 1, r.stage())));
			String[][] opts = {{"GO TO", in ? "IN IT" : "BEFORE IT"}, {"PARTY", keepParty ? "MY OWN" : st.title()}, {"STARTER", speciesName(Campaign.STARTERS[starter])}};
			float oy = py + ph - 24 - 3 * 42 - 52;
			for (int i = 0; i < 3; i++) {
				float ry = oy + i * 42, rx = px + 18, rw = pw - 36;
				V.rr(g, rx, ry, rw, 38, 12, 0xFFF4F6FB);
				V.text(g, opts[i][0], rx + 16, V.capsTop(ry + 19, 16), 16, V.INK, true);
				float vr = rx + rw - 18;
				V.tri(g, vr - 300, ry + 19, 10, 2, V.RED);
				V.centred(g, opts[i][1], vr - 152, V.capsTop(ry + 19, 16), 16, V.INK, true);
				V.tri(g, vr - 6, ry + 19, 10, 0, V.RED);
				int ii = i;
				vhit(rx, ry, rw, 38, () -> {
					if (ii == 0) in = !in;
					else if (ii == 1) keepParty = !keepParty;
					else starter = Math.floorMod(starter + 1, Campaign.STARTERS.length);
					Sounds.press();
				});
			}
			String line = keepParty ? "Your own party comes with you." : st.line() + " Leads at Lv " + st.leadLo() + " to " + st.leadHi() + ".";
			V.text(g, cut(line, pw - 48), px + 24, oy - 26, 14, V.MUTED, false);
			// GO, and your own game back.
			float gy = py + ph - 24 - 40;
			V.pill(g, px + 18, gy, 240, 40, V.GREEN);
			V.centred(g, in ? "GO INTO IT" : "GO TO JUST BEFORE", px + 18 + 120, V.capsTop(gy + 20, 16), 16, V.WHITE, true);
			vhit(px + 18, gy, 240, 40, this::go);
			if (away) {
				V.pill(g, px + pw - 18 - 240, gy, 240, 40, V.AMBER);
				V.centred(g, "MY OWN GAME", px + pw - 18 - 120, V.capsTop(gy + 20, 16), 16, V.WHITE, true);
				vhit(px + pw - 18 - 240, gy, 240, 40, this::own);
			}
		}
		if (away) V.hints(g, "A", "Go", "P", "Party", "S", "Starter", "O", "My own game", "B", "Back");
		else V.hints(g, "A", "Go", "P", "Party", "S", "Starter", "B", "Back");
	}

	private static String cut(String s, float w) {
		if (V.width(s, 15, false) <= w) return s;
		String t = s;
		while (t.length() > 4 && V.width(t + "...", 15, false) > w) t = t.substring(0, t.length() - 1);
		return t.trim() + "...";
	}
}
