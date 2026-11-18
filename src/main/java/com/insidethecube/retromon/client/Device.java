package com.insidethecube.retromon.client;

import java.util.function.Supplier;

import com.insidethecube.retromon.game.Net;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;

/**
 * Your Pokédex, held in your hands. START in the world: you take it out of your pocket and look down at it
 * (your figure does, out in the world), then it comes up the screen in your hands, over the world frosted
 * behind it, with the menu on it. The Pokédex, the bag, your Pokémon and the Town Map all show on it. Closing
 * it: it goes down, then you put it away. The world's clock stands still while it's out.
 */
public final class Device {
	private Device() {}

	/** How long (ticks): taking it out, it coming up the screen, going down, and putting it away. */
	static final int TAKE = 12, RISE = 11, DROP = 8, PUT = 10;

	private enum Phase {
		NONE, TAKING, OPEN, DROPPING, PUTTING
	}

	private static Phase phase = Phase.NONE;
	private static int t;
	private static float shown, shownOld;
	private static Supplier<Screen> pending;
	private static boolean held;

	/** Taking it out, holding it or putting it away (you can't walk meanwhile). */
	public static boolean busy() {
		return phase != Phase.NONE;
	}

	/** START in the world: take it out, then show this on it. */
	public static void open(Supplier<Screen> screen) {
		if (phase != Phase.NONE) return;
		phase = Phase.TAKING;
		t = 0;
		pending = screen;
		hold(true);
		Sounds.sfx("menu", 1.1F, 0.7F);
	}

	/** Going down (input waits). */
	public static boolean closing() {
		return phase == Phase.DROPPING;
	}

	/** Closing from the device: down it goes, then into your pocket. */
	public static void close() {
		if (phase == Phase.NONE) {
			Minecraft.getInstance().setScreen(null);
			return;
		}
		if (phase != Phase.OPEN) return;
		phase = Phase.DROPPING;
		t = 0;
		Sounds.sfx("menu", 0.85F, 0.6F);
	}

	public static void reset() {
		phase = Phase.NONE;
		t = 0;
		shown = shownOld = 0;
		pending = null;
		held = false;
	}

	/** The world's clock stands still while it's out (singleplayer: the server holds it). */
	private static void hold(boolean on) {
		if (held == on) return;
		held = on;
		CompoundTag tag = new CompoundTag();
		tag.putBoolean("on", on);
		ClientPlayNetworking.send(new Net.Act("device", tag));
	}

	public static void tick(Minecraft mc) {
		shownOld = shown;
		if (!Adventure.active()) {
			if (phase != Phase.NONE && mc.player != null) hold(false);
			phase = Phase.NONE;
			shown = shownOld = 0;
			return;
		}
		// Shown some other way than from START (back from somewhere): it's simply in your hands.
		if (phase == Phase.NONE && mc.screen instanceof DeviceScreen) {
			phase = Phase.OPEN;
			hold(true);
		}
		switch (phase) {
			case TAKING -> {
				if (++t >= TAKE) {
					phase = Phase.OPEN;
					t = 0;
					Screen next = pending == null ? null : pending.get();
					pending = null;
					if (next != null) mc.setScreen(next);
					else phase = Phase.PUTTING;
				}
			}
			case OPEN -> {
				// Something else took the screen (flying off, waiting till morning, someone talking to you): put it away.
				Screen sc = mc.screen;
				if (!(sc instanceof DeviceScreen) && !(sc instanceof ClothesScreen) && !(sc instanceof PauseScreen) && GameScreen.resume == null) {
					phase = Phase.PUTTING;
					t = 0;
				}
			}
			case DROPPING -> {
				if (++t >= DROP) {
					phase = Phase.PUTTING;
					t = 0;
					if (mc.screen instanceof DeviceScreen) mc.setScreen(null);
				}
			}
			case PUTTING -> {
				if (++t >= PUT) {
					phase = Phase.NONE;
					t = 0;
					hold(false);
				}
			}
			default -> {
			}
		}
		float target = phase == Phase.OPEN && mc.screen instanceof DeviceScreen ? 1 : 0;
		if (shown < target) shown = Math.min(target, shown + 1F / RISE);
		else if (shown > target) shown = Math.max(target, shown - 1F / DROP);
	}

	// ---------- your figure, out in the world ----------

	/**
	 * How your figure holds it now: [right arm up (0 to 1), left arm up, head bowed, hand at the pocket, the
	 * Pokédex showing (0 or 1)]. All zero when it's put away.
	 */
	public static float[] pose(float partial) {
		Minecraft mc = Minecraft.getInstance();
		if (phase == Phase.NONE || mc.screen instanceof ClothesScreen) return null;
		float p;
		switch (phase) {
			case TAKING -> p = Math.min(1, (t + partial) / TAKE);
			case PUTTING -> p = Math.max(0, 1 - (t + partial) / PUT);
			default -> p = 1;
		}
		float right = smooth((p - 0.28F) / 0.5F), left = smooth((p - 0.5F) / 0.45F), head = smooth((p - 0.3F) / 0.6F);
		// Into the pocket first (a dip of the right hand down and back), then up.
		float pocket = p < 0.42F ? (float) Math.sin(Math.min(1, p / 0.42F) * Math.PI) : 0;
		return new float[] {right, left, head, pocket, p > 0.26F ? 1 : 0};
	}

	private static float smooth(float x) {
		x = Math.max(0, Math.min(1, x));
		return x * x * (3 - 2 * x);
	}

	// ---------- on the screen ----------

	/** Where it is on the screen: its outside, its screen, the status bar's height, how far up it's come (0 to 1). */
	public record Frame(int x, int y, int w, int h, int ix, int iy, int iw, int ih, int bar, int bez, float k, int s) {}

	/** How big a device screen's insides draw (1 on a small window, up to 3). */
	public static int scale(int innerHeight) {
		return Math.max(1, Math.min(3, innerHeight / 180));
	}

	public static Frame frame(int W, int H, float partial) {
		float u = shownOld + (shown - shownOld) * partial;
		// Up: quick then settling; down: the same curve the other way.
		float k = 1 - (1 - u) * (1 - u) * (1 - u);
		int s = Ui.scale(H);
		float aspect = 1.62F;
		int h = (int) Math.min(H * 0.86F, W * 0.9F / aspect);
		int w = (int) (h * aspect);
		int x = (W - w) / 2;
		int y = (H - h) / 2 + H / 70;
		y += Math.round((1 - k) * (H - y + 12));
		int bez = Math.max(6, h / 21);
		int ix = x + bez, iy = y + bez, iw = w - 2 * bez, ih = h - 2 * bez;
		int bar = Math.max(10, ih / 17);
		return new Frame(x, y, w, h, ix, iy, iw, ih, bar, bez, k, s);
	}

	private static final int RED = 0xFFDA3A33, RED_DARK = 0xFF9C1F1C, RED_LIGHT = 0xFFF0675A, INK = 0xFF0E1424;

	/** Behind what's on its screen: the world frosted and dimmed, the device's body, the dark of its screen. */
	public static void back(GuiGraphics g, Frame f, int W, int H, int ticks, float partial) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.options.getMenuBackgroundBlurriness() > 0) g.blurBeforeThisStratum();
		// The world dimmed, more at the edges.
		float k = Math.max(0, Math.min(1, f.k() * 1.2F));
		g.fill(0, 0, W, H, Ui.alpha(0x660A0E1A, k));
		Ui.vignette(g, W, H, 0x05070F, 0.55F * k);
		if (f.y() >= H) return;
		int x = f.x(), y = f.y(), w = f.w(), h = f.h(), r = Math.max(6, h / 11);
		// A soft shadow, the body (a darker lip along the bottom), lit along the top.
		disc(g, x + w / 2, y + h + h / 10, w / 2, h / 14, 0x30000000);
		rounded(g, x + 2, y + 5, w, h, r, 0x50000000);
		rounded(g, x, y, w, h, r, RED_DARK);
		rounded(g, x, y, w, h - Math.max(3, h / 60), r, RED);
		for (int i = 0; i < r; i++) {
			int in = inset(r, i);
			g.fill(x + in, y + i, x + w - in, y + i + 1, Ui.alpha(0xFFFFFFFF, 0.22F * (1 - i / (float) r)));
		}
		g.fill(x + r, y + h / 2, x + w - r, y + h / 2 + Math.max(1, h / 160), 0x33000000);
		// The lens, top left (white ring, blue glass, a glint), and three lights beside it.
		int lr = Math.max(5, (int) (f.bez() * 1.25F)), lx = x + r + lr / 2, ly = y + Math.max(2, f.bez() / 3);
		disc(g, lx, ly + 1, lr + 2, lr + 2, 0x55000000);
		disc(g, lx, ly, lr + 2, lr + 2, 0xFFF4F2EC);
		disc(g, lx, ly, lr, lr, 0xFF2F7FD8);
		disc(g, lx, ly, lr * 2 / 3, lr * 2 / 3, 0xFF59B2F4);
		disc(g, lx - lr / 3, ly - lr / 3, Math.max(1, lr / 4), Math.max(1, lr / 4), 0xDDFFFFFF);
		int[] leds = {0xFFE8453C, 0xFFF4C53A, 0xFF46C46A};
		int dot = Math.max(2, f.bez() / 3);
		for (int i = 0; i < 3; i++) {
			boolean blink = i == 2 && (ticks / 10) % 4 == 0;
			disc(g, lx + lr + 6 + dot + i * (dot * 2 + 4), y + f.bez() / 2, dot, dot, blink ? 0xFFBFFFD0 : leds[i]);
		}
		// A speaker grille, top right.
		for (int i = 0; i < 4; i++) g.fill(x + w - r - 4 - i * 5, y + f.bez() / 3 + 1, x + w - r - 2 - i * 5, y + f.bez() - 2, 0x55000000);
		// Its screen: set in, dark glass.
		int ir = Math.max(3, r / 2);
		rounded(g, f.ix() - 2, f.iy() - 2, f.iw() + 4, f.ih() + 4, ir + 1, 0xFF3A0E0E);
		rounded(g, f.ix(), f.iy(), f.iw(), f.ih(), ir, INK);
		g.fillGradient(f.ix(), f.iy() + f.bar(), f.ix() + f.iw(), f.iy() + f.ih() - ir, 0xFF15203A, 0xFF0C1222);
	}

	/** Over its screen: the status bar (the time, where you are, your money) and your hands holding it. */
	public static void front(GuiGraphics g, Frame f, int W, int H, int ticks, float partial, Font font) {
		if (f.y() >= H) return;
		Minecraft mc = Minecraft.getInstance();
		int s = Math.max(1, f.bar() / 11);
		int bx = f.ix(), by = f.iy(), bw = f.iw(), bh = f.bar();
		g.fill(bx + 3, by, bx + bw - 3, by + bh, 0xFF0A0F1C);
		g.fill(bx + 3, by + bh - 1, bx + bw - 3, by + bh, 0x40FFFFFF);
		int ty = by + (bh - 8 * s) / 2 + s / 2;
		if (mc.level != null) {
			long tod = Math.floorMod(mc.level.getDayTime(), 24000L);
			int hour = (int) ((tod / 1000 + 6) % 24), minute = (int) ((tod % 1000) * 60 / 1000);
			String clock = String.format("%d:%02d %s", hour % 12 == 0 ? 12 : hour % 12, minute, hour < 12 ? "AM" : "PM");
			boolean night = Night.nightness() > 0.5F;
			int cs = 6 * s;
			Ui.round(g, bx + 8, ty, cs, cs, night ? 0xFFE8ECFF : 0xFFFFC94A, 3 * s);
			Ui.draw(g, font, Ui.bold(clock), bx + 12 + cs, ty, Ui.TEXT, s, false);
		}
		String place = ClientState.placeName.isEmpty() ? "" : ClientState.placeName;
		if (!place.isEmpty()) Ui.centred(g, font, place, bx + bw / 2, ty, Ui.MUTED, s, false);
		String cash = Ui.money(ClientState.data.money);
		Ui.right(g, font, cash, bx + bw - 10, ty, Ui.GOLD, s);
		hands(g, f, H);
	}

	/** Your forearms coming up from the bottom of the screen, a hand at each side of it, thumbs over the front. */
	private static void hands(GuiGraphics g, Frame f, int H) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return;
		Identifier skin;
		try {
			skin = mc.player.getSkin().body().texturePath();
		} catch (Exception e) {
			return;
		}
		float gy = f.y() + f.h() * 0.62F;
		double ang = Math.atan2(0.42, 1);
		// Long enough to run off the bottom of the screen.
		float reach = (float) ((H - gy + 24) / Math.cos(ang));
		int p = Math.max(Math.max(3, Math.round(f.h() * 0.03F)), (int) Math.ceil(reach / 12F));
		arm(g, skin, true, f.x() + f.bez() * 0.35F, gy, (float) ang, p, f);
		arm(g, skin, false, f.x() + f.w() - f.bez() * 0.35F, gy, (float) -ang, p, f);
	}

	private static void arm(GuiGraphics g, Identifier skin, boolean left, float gx, float gy, float angle, int p, Frame f) {
		// Faces of the player skin (64x64): the arm's front, its outer side, and the same of its sleeve.
		int fu = left ? 36 : 44, fv = left ? 52 : 20, su = left ? 40 : 40, sv = left ? 52 : 20;
		int lu = left ? 52 : 44, lv = left ? 52 : 36, lsu = left ? 56 : 40, lsv = left ? 52 : 36;
		int L = 12 * p, w = 4 * p, side = 2 * p;
		g.pose().pushMatrix();
		g.pose().translate(gx, gy);
		g.pose().rotate(angle);
		g.pose().scale(1, -1);
		// The arm's outer side (darker, as it turns away from the light), then its front; the sleeve over both.
		int sx = left ? -w / 2 - side : w / 2;
		g.blit(RenderPipelines.GUI_TEXTURED, skin, sx, -L, su, sv, side, L, 4, 12, 64, 64, 0xFF9AA0A8);
		g.blit(RenderPipelines.GUI_TEXTURED, skin, -w / 2, -L, fu, fv, w, L, 4, 12, 64, 64, 0xFFFFFFFF);
		g.blit(RenderPipelines.GUI_TEXTURED, skin, sx, -L, lsu, lsv, side, L, 4, 12, 64, 64, 0xFF9AA0A8);
		g.blit(RenderPipelines.GUI_TEXTURED, skin, -w / 2, -L, lu, lv, w, L, 4, 12, 64, 64, 0xFFFFFFFF);
		// A soft shade where the hand meets the device.
		g.fill(-w / 2, -p, w / 2, 0, 0x22000000);
		g.pose().popMatrix();
		// The thumb, over the front of the device (the hand's own colour).
		g.pose().pushMatrix();
		float tx = gx + (left ? 1 : -1) * p * 1.1F, ty = gy - p * 0.9F;
		g.pose().translate(tx, ty);
		g.pose().rotate(left ? 0.5F : -0.5F);
		int tw = Math.max(2, Math.round(p * 1.3F)), th = Math.max(3, Math.round(p * 2.1F));
		g.fill(-tw / 2 + 1, -th / 2 + 2, tw / 2 + 1, th / 2 + 2, 0x40000000);
		g.blit(RenderPipelines.GUI_TEXTURED, skin, -tw / 2, -th / 2, fu + 1, fv + 10, tw, th, 2, 2, 64, 64, 0xFFFFFFFF);
		g.pose().popMatrix();
	}

	// ---------- shapes ----------

	/** How far in a rounded corner of radius r is, on row i from its edge. */
	private static int inset(int r, int i) {
		double dy = r - i - 0.5;
		return (int) Math.round(r - Math.sqrt(Math.max(0, r * r - dy * dy)));
	}

	/** A rectangle with properly rounded corners (radius r). */
	public static void rounded(GuiGraphics g, int x, int y, int w, int h, int r, int color) {
		r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
		for (int i = 0; i < r; i++) {
			int in = inset(r, i);
			g.fill(x + in, y + i, x + w - in, y + i + 1, color);
			g.fill(x + in, y + h - i - 1, x + w - in, y + h - i, color);
		}
		g.fill(x, y + r, x + w, y + h - r, color);
	}

	/** A filled ellipse centred on (cx, cy). */
	public static void disc(GuiGraphics g, int cx, int cy, int rx, int ry, int color) {
		if (rx <= 0 || ry <= 0) return;
		for (int i = -ry; i < ry; i++) {
			double v = (i + 0.5) / ry;
			int half = (int) Math.round(rx * Math.sqrt(Math.max(0, 1 - v * v)));
			if (half > 0) g.fill(cx - half, cy + i, cx + half, cy + i + 1, color);
		}
	}
}
