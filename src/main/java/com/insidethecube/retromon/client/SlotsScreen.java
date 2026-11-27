package com.insidethecube.retromon.client;

import com.insidethecube.retromon.game.KantoStory;
import com.insidethecube.retromon.game.Net;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.nbt.CompoundTag;
import org.lwjgl.glfw.GLFW;

/**
 * (0.71) A GAME CORNER slot machine (Red and Blue's way of playing, with symbols of our own): bet 1 to 3 coins (UP
 * and DOWN; 1 coin plays the middle line, 2 the top and bottom too, 3 the diagonals too), A to spin, then A to stop
 * each reel in turn. The server says where they stop; each one slides on to its place after you stop it. Three in a
 * line pay out: SEVENs 300, BARs 100, BELLs, STARs and MOONs 15, CHERRIES 8 (one or two from the left pay 2 or 4).
 */
public class SlotsScreen extends GameScreen {
	private int coins, bet = 1, win = -1, stopping;
	/** Each reel's place (which symbol's in the middle row: fractions as it moves) and where it'll stop (-1: not known yet). */
	private final double[] pos = {0, 5, 10}, aim = {-1, -1, -1};
	private final int[] stop = {-1, -1, -1};
	private boolean waiting, spinning;
	private int resultCoins, shownAt = -100;
	private String said = "Press A to spin!";

	private static final float RX = 420, RY = 196, RW = 132, RH = 84, GAP = 16;
	private static final String[] NAMES = {"SEVEN", "BAR", "CHERRY", "BELL", "STAR", "MOON"};

	public SlotsScreen(int coins) {
		super("Slots");
		this.coins = coins;
		this.resultCoins = coins;
	}

	/** The server's answer to a spin: where each reel stops, what it pays, and your coins after. */
	public void result(CompoundTag t) {
		int[] s = t.getIntArray("stop").orElse(new int[0]);
		if (s.length < 3) {
			waiting = false;
			return;
		}
		for (int i = 0; i < 3; i++) stop[i] = s[i];
		win = t.getIntOr("win", 0);
		resultCoins = t.getIntOr("coins", coins);
		waiting = false;
		spinning = true;
		stopping = 0;
		said = "A to stop each reel!";
	}

	@Override
	public void tick() {
		super.tick();
		boolean any = false;
		for (int i = 0; i < 3; i++) {
			if (!spinning && !waiting) continue;
			if (aim[i] < 0) {
				pos[i] += 0.42;
				any = true;
			} else if (pos[i] < aim[i] - 1e-3) {
				pos[i] = Math.min(aim[i], pos[i] + Math.max(0.06, (aim[i] - pos[i]) * 0.22));
				any = true;
				if (pos[i] >= aim[i] - 1e-3) {
					pos[i] = aim[i];
					Sounds.select();
				}
			}
		}
		// All three in: what it paid.
		if (spinning && stopping >= 3 && !any) {
			spinning = false;
			coins = resultCoins;
			shownAt = ticks;
			if (win > 0) {
				said = "You won " + win + " coin" + (win == 1 ? "" : "s") + "!";
				Sounds.jingle("get_item");
			} else said = "Not this time! Press A to spin again.";
			for (int i = 0; i < 3; i++) aim[i] = -1;
		}
	}

	private void spin() {
		if (waiting || spinning) return;
		if (coins < bet) {
			said = coins <= 0 ? "You're out of coins!" : "Not enough coins for that bet.";
			return;
		}
		coins -= bet;
		win = -1;
		waiting = true;
		for (int i = 0; i < 3; i++) {
			aim[i] = -1;
			stop[i] = -1;
		}
		said = "...";
		CompoundTag t = new CompoundTag();
		t.putInt("bet", bet);
		Sounds.press();
		ClientPlayNetworking.send(new Net.Act("slot_spin", t));
	}

	/** Stops the next reel: it slides on to the server's symbol, at least two places further. */
	private void stopNext() {
		if (!spinning || stopping >= 3) return;
		int i = stopping++;
		int len = KantoStory.REELS[i].length;
		double at = pos[i] + 2;
		double base = Math.ceil(at);
		int k = (int) Math.floorMod((long) base, len);
		double target = base + Math.floorMod(stop[i] - k, len);
		aim[i] = target;
		Sounds.press();
	}

	@Override
	protected boolean key(KeyEvent e) {
		if (Ui.isB(e)) {
			if (!spinning && !waiting) back();
			return true;
		}
		if (Ui.isA(e)) {
			if (spinning) stopNext();
			else spin();
		} else if (!spinning && !waiting && (Ui.isUp(e) || Ui.isDown(e))) {
			bet = Math.max(1, Math.min(3, bet + (Ui.isUp(e) ? 1 : -1)));
			Sounds.select();
		} else if (!spinning && !waiting && e.key() >= GLFW.GLFW_KEY_1 && e.key() <= GLFW.GLFW_KEY_3) {
			bet = e.key() - GLFW.GLFW_KEY_1 + 1;
			Sounds.select();
		}
		return true;
	}

	@Override
	protected void back() {
		Sounds.press();
		if (minecraft != null) minecraft.setScreen(null);
	}

	@Override
	public void onClose() {
		if (!spinning && !waiting) back();
	}

	@Override
	protected void draw(GuiGraphics g, float partial) {
		Scenes.bg2(g, "options");
		// The machine: a red cabinet, the reels in their window, the lines lit for the bet.
		float mx0 = 380, my0 = 70, mw = 520, mh = 560;
		V.softShadow(g, mx0, my0 + 10, mw, mh, 24, 0x59000000);
		V.rrv(g, mx0, my0, mw, mh, 28, 0xFFE5525C, 0xFFB8323E);
		V.rr(g, mx0 + 20, my0 + 18, mw - 40, 74, 18, 0xFF2B1B2E);
		V.centred(g, "SLOTS", mx0 + mw / 2, V.capsTop(my0 + 55, 34), 34, V.CREAM, true);
		// The reels.
		float wy = RY - RH * 1.5F, wh = RH * 3;
		V.rr(g, RX - 14, wy - 14, 3 * RW + 2 * GAP + 28, wh + 28, 18, 0xFF2B1B2E);
		for (int i = 0; i < 3; i++) {
			float x = RX + i * (RW + GAP);
			V.rr(g, x, wy, RW, wh, 10, 0xFFFFFBF0);
			double p = pos[i];
			if (spinning || waiting) p += (aim[i] < 0 ? 0.42 : 0) * partial;
			int base = (int) Math.floor(p);
			double frac = p - base;
			for (int r = -2; r <= 2; r++) {
				float cy = (float) (RY + (r - frac) * RH);
				if (cy < wy - RH / 2 + 4 || cy > wy + wh + RH / 2 - 4) continue;
				symbol(g, KantoStory.sym(i, base + r), x + RW / 2, cy, Math.min(RW, RH) * 0.36F);
			}
		}
		// The cabinet over the reels' ends (so they run behind its frame).
		V.rr(g, RX - 14, wy - 60, 3 * RW + 2 * GAP + 28, 46, 0, 0xFFE0464F);
		V.rr(g, RX - 14, wy + wh + 14, 3 * RW + 2 * GAP + 28, 46, 0, 0xFFC63A45);
		V.rr(g, RX - 14, wy - 14, 3 * RW + 2 * GAP + 28, 14, 0, 0xFF2B1B2E);
		V.rr(g, RX - 14, wy + wh, 3 * RW + 2 * GAP + 28, 14, 0, 0xFF2B1B2E);
		// The lines played: the middle, the top and bottom (2), the diagonals (3).
		float lx0 = RX - 6, lx1 = RX + 3 * RW + 2 * GAP + 6;
		int lit = 0xCCFFD34E, off = 0x33FFFFFF;
		V.rr(g, lx0, RY - 2, lx1 - lx0, 4, 2, lit);
		V.rr(g, lx0, RY - RH - 2, lx1 - lx0, 4, 2, bet >= 2 ? lit : off);
		V.rr(g, lx0, RY + RH - 2, lx1 - lx0, 4, 2, bet >= 2 ? lit : off);
		for (int k = 0; k < 2; k++) {
			float sy = k == 0 ? RY - RH : RY + RH, ey = k == 0 ? RY + RH : RY - RH;
			for (int s = 0; s <= 20; s++) {
				float f = s / 20F;
				V.rr(g, lx0 + (lx1 - lx0) * f - 2, sy + (ey - sy) * f - 2, 4, 4, 2, bet >= 3 ? lit : off);
			}
		}
		// Below: your coins, the bet, what it says.
		float by = my0 + mh - 150;
		V.rr(g, mx0 + 30, by, 210, 56, 14, 0xFF2B1B2E);
		V.text(g, "COINS", mx0 + 46, by + 10, 13, V.DIM, true);
		V.text(g, String.valueOf(coins), mx0 + 46, by + 26, 22, V.CREAM, true);
		V.rr(g, mx0 + mw - 240, by, 210, 56, 14, 0xFF2B1B2E);
		V.text(g, "BET", mx0 + mw - 224, by + 10, 13, V.DIM, true);
		V.text(g, bet + " COIN" + (bet == 1 ? "" : "S"), mx0 + mw - 224, by + 26, 22, V.CREAM, true);
		boolean big = ticks - shownAt < 60 && win > 0;
		V.pill(g, mx0 + 30, by + 72, mw - 60, 44, big ? V.AMBER : 0xFFFFFFFF);
		V.centred(g, said, mx0 + mw / 2, V.capsTop(by + 94, 18), 18, big ? V.WHITE : V.INK, true);

		// The prize table, at the side.
		float tx = 950, ty = 150;
		V.panel(g, tx, ty, 290, 330);
		V.title(g, "PAYOUTS", tx + 20, V.capsTop(ty + 30, 20), 20, V.INK);
		int[] order = {0, 1, 3, 4, 5, 2};
		for (int i = 0; i < order.length; i++) {
			int s = order[i];
			float ry = ty + 66 + i * 40;
			for (int k = 0; k < 3; k++) symbol(g, s, tx + 34 + k * 30, ry, 12);
			V.right(g, String.valueOf(KantoStory.PAYS[s]), tx + 270, V.capsTop(ry, 18), 18, V.INK, true);
		}
		V.text(g, "per coin on each line", tx + 20, ty + 66 + 6 * 40 - 4, 13, V.MUTED, false);
		V.panel(g, 40, 150, 310, 230);
		V.title(g, "HOW TO PLAY", 60, V.capsTop(180, 20), 20, V.INK);
		String[] how = {"UP/DOWN: bet 1 to 3 coins.", "1 coin: the middle line.", "2 coins: the top and bottom too.", "3 coins: the diagonals too.",
				"A spins, then A stops", "each reel in turn."};
		for (int i = 0; i < how.length; i++) V.text(g, how[i], 60, 212 + i * 26, 15, V.TEXT, false);
		V.hints(g, "A", spinning ? "Stop" : "Spin", "B", "Leave");
	}

	/** One of the reels' symbols, centred there, about so big (half its height). */
	private static void symbol(GuiGraphics g, int s, float cx, float cy, float r) {
		switch (s) {
			case 0 -> {
				// SEVEN: a red 7 on a gold disc.
				V.rr(g, cx - r, cy - r, 2 * r, 2 * r, r, 0xFFFFD34E);
				V.centred(g, "7", cx, V.capsTop(cy, r * 1.6F), r * 1.6F, 0xFFD62F3C, true);
			}
			case 1 -> {
				// BAR: a black bar, the word in white.
				V.rr(g, cx - r * 1.25F, cy - r * 0.55F, r * 2.5F, r * 1.1F, r * 0.25F, 0xFF22242C);
				V.centred(g, "BAR", cx, V.capsTop(cy, r * 0.75F), r * 0.75F, 0xFFFFFFFF, true);
			}
			case 2 -> {
				// CHERRIES: two red berries on green stalks.
				V.rr(g, cx - r * 0.12F, cy - r, r * 0.16F, r * 0.9F, r * 0.08F, 0xFF3E9E4E);
				V.rr(g, cx - r * 0.12F, cy - r, r * 0.7F, r * 0.16F, r * 0.08F, 0xFF3E9E4E);
				V.rr(g, cx - r * 0.95F, cy - r * 0.1F, r * 0.95F, r * 0.95F, r * 0.48F, 0xFFD62F3C);
				V.rr(g, cx + 0.05F * r, cy + r * 0.05F, r * 0.95F, r * 0.95F, r * 0.48F, 0xFFE5525C);
			}
			case 3 -> {
				// BELL: a gold bell and its clapper.
				V.rr(g, cx - r * 0.55F, cy - r * 0.95F, r * 1.1F, r * 1.1F, r * 0.55F, 0xFFF0B43C);
				V.rr(g, cx - r * 0.8F, cy - r * 0.35F, r * 1.6F, r * 0.95F, r * 0.25F, 0xFFF0B43C);
				V.rr(g, cx - r * 0.18F, cy + r * 0.5F, r * 0.36F, r * 0.36F, r * 0.18F, 0xFFB27A1C);
			}
			case 4 -> {
				// STAR: a yellow star (two triangles).
				V.tri(g, cx, cy - r * 0.2F, r * 1.7F, 3, 0xFFFFC93C);
				V.tri(g, cx, cy + r * 0.25F, r * 1.7F, 1, 0xFFFFC93C);
			}
			default -> {
				// MOON: a cream crescent.
				V.rr(g, cx - r, cy - r, 2 * r, 2 * r, r, 0xFFFFE7A1);
				V.rr(g, cx - r * 0.45F, cy - r * 1.1F, 2 * r, 2 * r, r, 0xFFFFFBF0);
			}
		}
	}
}
