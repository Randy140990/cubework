package com.insidethecube.retromon.client;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Over the world in the adventure: no hotbar, hearts or crosshair, the "!" over a trainer who's spotted you, and your money as it changes. */
public final class Hud {
	private Hud() {}

	public static void register() {
		Identifier[] hidden = {VanillaHudElements.CROSSHAIR, VanillaHudElements.HOTBAR, VanillaHudElements.ARMOR_BAR, VanillaHudElements.HEALTH_BAR,
				VanillaHudElements.FOOD_BAR, VanillaHudElements.AIR_BAR, VanillaHudElements.INFO_BAR, VanillaHudElements.EXPERIENCE_LEVEL,
				VanillaHudElements.HELD_ITEM_TOOLTIP, VanillaHudElements.STATUS_EFFECTS, VanillaHudElements.MOUNT_HEALTH};
		for (Identifier id : hidden) {
			HudElementRegistry.replaceElement(id, old -> (g, delta) -> {
				if (!Adventure.active()) old.render(g, delta);
			});
		}
		HudElementRegistry.addLast(com.insidethecube.retromon.Retromon.id("cinema"), Hud::cinema);
		HudElementRegistry.addLast(com.insidethecube.retromon.Retromon.id("exclaim"), Hud::exclaim);
		HudElementRegistry.addLast(com.insidethecube.retromon.Retromon.id("hints"), Hud::hints);
		HudElementRegistry.addLast(com.insidethecube.retromon.Retromon.id("fade"), Hud::fade);
		HudElementRegistry.addLast(com.insidethecube.retromon.Retromon.id("place"), Hud::place);
		HudElementRegistry.addLast(com.insidethecube.retromon.Retromon.id("speed"), Hud::speed);
		HudElementRegistry.addLast(com.insidethecube.retromon.Retromon.id("money"), Hud::money);
	}

	// ---------- the cinema frame (talking to someone, or a trainer walking up to you), fading in and out ----------

	private static float cine, cineOld;
	/** A fade to black and back (waiting until morning): ticks left, and what to do at the darkest. */
	private static int fade;
	private static final int FADE = 36;
	private static Runnable atDark;

	public static void fadeThrough(Runnable then) {
		fade = FADE;
		atDark = then;
	}

	public static void tick(Minecraft mc) {
		cineOld = cine;
		boolean want = Adventure.active() && !BattleScene.active()
				&& (mc.screen instanceof DialogueScreen || mc.screen instanceof StarterScreen || mc.screen instanceof NicknameScreen && cine > 0
						|| mc.screen == null && Adventure.frozen > 0);
		cine += ((want ? 1 : 0) - cine) * (want ? 0.14F : 0.2F);
		if (Math.abs(cine - (want ? 1 : 0)) < 0.01F) cine = want ? 1 : 0;
		if (fade > 0) {
			fade--;
			if (fade == FADE / 2 && atDark != null) {
				atDark.run();
				atDark = null;
			}
		}
		moneyTick(mc);
	}

	// ---------- your money, sliding in at the top right when it changes (after a battle, say) ----------

	private static int moneySeen, moneyFrom, moneyTo, moneyTicks = -1, moneyQuiet;
	private static final int MONEY_SHOW = 110;

	private static void moneyTick(Minecraft mc) {
		int cash = ClientState.data.money;
		if (!Adventure.active() || mc.level == null) {
			// Loading a game: what it has comes in quietly.
			moneyTicks = -1;
			moneyQuiet = 40;
			return;
		}
		if (moneyQuiet > 0) {
			moneyQuiet--;
			moneySeen = cash;
			return;
		}
		if (mc.screen instanceof MartScreen) moneySeen = cash;
		if (cash != moneySeen) {
			if (moneyTicks < 0) moneyFrom = moneySeen;
			moneyTo = cash;
			moneySeen = cash;
			moneyTicks = 0;
		}
		// It waits for you to be back out in the world (a battle's prize shows once the battle's over).
		if (moneyTicks >= 0 && mc.screen == null && !BattleScene.active() && ++moneyTicks > MONEY_SHOW) moneyTicks = -1;
	}

	private static void money(GuiGraphics g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		if (!Adventure.active() || BattleScene.active() || moneyTicks < 0 || mc.screen != null || mc.options.hideGui) return;
		int s = Ui.scale(g.guiHeight());
		float t = moneyTicks + delta.getGameTimeDeltaPartialTick(true);
		float k = Math.max(0, Math.min(1, Math.min(t / 8F, (MONEY_SHOW - t) / 10F)));
		k = 1 - (1 - k) * (1 - k);
		float c = Math.max(0, Math.min(1, (t - 8) / 24F));
		int shown = Math.round(moneyFrom + (moneyTo - moneyFrom) * (1 - (1 - c) * (1 - c)));
		int d = moneyTo - moneyFrom;
		String total = Ui.money(shown), change = (d >= 0 ? "+" : "-") + Ui.money(Math.abs(d));
		int w = Math.max(Ui.width(mc.font, Ui.money(Math.max(moneyFrom, moneyTo)), s), Ui.width(mc.font, change, s)) + 34 * s, h = 30 * s;
		int x = g.guiWidth() - 6 * s - w + Math.round((w + 6 * s) * (1 - k)), y = Adventure.speed > 1 ? 24 * s : 6 * s;
		Ui.tagged(g, x, y, w, h, s, Ui.GOLD);
		int cs = 9 * s, cx = x + 10 * s, cy = y + 5 * s;
		Ui.round(g, cx, cy, cs, cs, 0xFFE0A526, 4 * s);
		Ui.round(g, cx + s, cy + s, cs - 2 * s, cs - 2 * s, 0xFFFFD45A, 4 * s);
		Ui.text(g, mc.font, total, cx + cs + 4 * s, y + 6 * s, Ui.TEXT, s);
		Ui.text(g, mc.font, change, cx + cs + 4 * s, y + 17 * s, d >= 0 ? 0xFF7CF0A8 : 0xFFFF8A8A, s);
	}

	private static void cinema(GuiGraphics g, DeltaTracker delta) {
		float k = cineOld + (cine - cineOld) * delta.getGameTimeDeltaPartialTick(true);
		if (k > 0.005F) Ui.cinema(g, g.guiWidth(), g.guiHeight(), k);
	}

	private static void fade(GuiGraphics g, DeltaTracker delta) {
		if (fade <= 0) return;
		float t = FADE - fade + delta.getGameTimeDeltaPartialTick(true);
		float half = FADE / 2F, a = t < half ? t / half : 1 - (t - half) / half;
		a = Math.max(0, Math.min(1, a * 1.25F));
		g.fill(0, 0, g.guiWidth(), g.guiHeight(), Ui.alpha(0xFF05070F, a));
		if (a > 0.85F) {
			Minecraft mc = Minecraft.getInstance();
			int s = Ui.scale(g.guiHeight());
			Ui.centred(g, mc.font, "Time passes...", g.guiWidth() / 2, g.guiHeight() / 2 - 4 * s, Ui.alpha(0xFFE8ECFF, (a - 0.85F) / 0.15F), s, false);
		}
	}

	// ---------- the buttons and the time, small at the bottom right ----------

	/** A key as it's bound now (SPACE, ENTER, TAB), so the hints follow your controls. */
	private static String key(net.minecraft.client.KeyMapping k) {
		return k == null ? "?" : k.getTranslatedKeyMessage().getString().toUpperCase(java.util.Locale.ROOT);
	}

	/** What's what: SPACE to talk, ENTER for the menu, TAB for speed, and the time of day with the sun or the moon. */
	private static void hints(GuiGraphics g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		if (!Adventure.active() || BattleScene.active() || mc.screen != null || mc.level == null || mc.options.hideGui) return;
		int s = Ui.scale(g.guiHeight()), w = g.guiWidth(), h = g.guiHeight();
		long t = Math.floorMod(mc.level.getDayTime(), 24000L);
		int hour = (int) ((t / 1000 + 6) % 24), minute = (int) ((t % 1000) * 60 / 1000);
		boolean night = Night.nightness() > 0.5F;
		String clock = String.format("%d:%02d %s", hour % 12 == 0 ? 12 : hour % 12, minute, hour < 12 ? "AM" : "PM");
		String[][] keys = {{key(mc.options.keyJump), "Talk"}, {key(Adventure.START), "Menu"}, {key(Adventure.SPEED), "Speed"}};
		int pad = 5 * s, gap = 8 * s, x = w - pad, y = h - pad - 14 * s;
		// The keys, right to left.
		for (int i = keys.length - 1; i >= 0; i--) {
			int lw = Ui.width(mc.font, keys[i][1], s), kw = Ui.boldWidth(mc.font, keys[i][0], s) + 8 * s;
			x -= lw;
			Ui.text(g, mc.font, keys[i][1], x, y + 3 * s, 0xE0E8ECFF, s);
			x -= kw + 3 * s;
			Ui.round(g, x, y, kw, 13 * s, 0xC8141B2E, 2 * s);
			Ui.round(g, x, y + 11 * s, kw, 2 * s, 0x60000000, s);
			Ui.draw(g, mc.font, Ui.bold(keys[i][0]), x + 4 * s, y + 3 * s, Ui.GOLD, s, false);
			x -= gap;
		}
		// The time, with a sun or a moon.
		int cw = Ui.width(mc.font, clock, s) + 22 * s;
		x -= cw;
		Ui.round(g, x, y, cw, 13 * s, 0xC8141B2E, 2 * s);
		Ui.round(g, x + 4 * s, y + 3 * s, 7 * s, 7 * s, night ? 0xFFE8ECFF : 0xFFFFC94A, 3 * s);
		if (night) Ui.round(g, x + 6 * s, y + 2 * s, 6 * s, 6 * s, 0xFF141B2E, 3 * s);
		Ui.text(g, mc.font, clock, x + 15 * s, y + 3 * s, Ui.TEXT, s);
	}

	/** The place's name, sliding in at the top left as you arrive (as the later games do), then away. */
	private static void place(GuiGraphics g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		int t = ClientState.placeTicks;
		if (!Adventure.active() || BattleScene.active() || t < 0 || t > 110 || ClientState.placeName.isEmpty() || mc.screen != null && !(mc.screen instanceof GameScreen)) return;
		int s = Ui.scale(g.guiHeight());
		float in = Math.min(1, (t + delta.getGameTimeDeltaPartialTick(true)) / 8F), out = Math.min(1, (110 - t) / 10F), k = Math.min(in, out);
		String name = ClientState.placeName;
		int w = Ui.width(mc.font, name, s) + 24 * s, h = 22 * s;
		int x = (int) (-w + (w + 6 * s) * (1 - (1 - k) * (1 - k))), y = 6 * s;
		Ui.tagged(g, x, y, w, h, s, Ui.GOLD);
		Ui.text(g, mc.font, name, x + 12 * s, y + 7 * s, Ui.TEXT, s);
	}

	/** The game speed, when it's sped up. */
	private static void speed(GuiGraphics g, DeltaTracker delta) {
		if (!Adventure.active() || Adventure.speed <= 1) return;
		Minecraft mc = Minecraft.getInstance();
		int s = Ui.scale(g.guiHeight());
		String t = "\u25B6\u25B6 x" + Adventure.speed;
		int w = Ui.width(mc.font, t, s) + 12 * s;
		Ui.round(g, g.guiWidth() - w - 6 * s, 6 * s, w, 14 * s, 0xC0141B2E, s);
		Ui.text(g, mc.font, t, g.guiWidth() - w, 9 * s, Ui.GOLD, s);
	}

	/** The "!" bubble, over the trainer's head, where the camera sees it. */
	private static void exclaim(GuiGraphics g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		if (!Adventure.active() || BattleScene.active() || ClientState.exclaimTicks <= 0 || mc.level == null) return;
		Entity e = mc.level.getEntity(ClientState.exclaimNpc);
		double[] cam = Adventure.lastPose;
		if (e == null || cam == null) return;
		Vec3 p = e.position().add(0, 2.35, 0);
		double yaw = Math.toRadians(cam[3]), pitch = Math.toRadians(cam[4]);
		Vec3 f = new Vec3(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
		Vec3 r = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
		Vec3 u = r.cross(f);
		Vec3 v = p.subtract(cam[0], cam[1], cam[2]);
		double depth = v.dot(f);
		if (depth <= 0.1) return;
		int w = g.guiWidth(), h = g.guiHeight();
		double t = Math.tan(Math.toRadians(Adventure.FOV) / 2);
		double nx = v.dot(r) / depth / (t * w / h), ny = v.dot(u) / depth / t;
		int sx = (int) ((nx + 1) / 2 * w), sy = (int) ((1 - ny) / 2 * h);
		int s = Ui.scale(h), bw = 12 * s, bh = 14 * s;
		// Pops up, then stays.
		float pop = Math.min(1, (40 - ClientState.exclaimTicks) / 4F);
		int lift = (int) ((1 - pop) * 6 * s);
		Ui.round(g, sx - bw / 2, sy - bh - lift, bw, bh, 0xFFFFFFFF, s);
		Ui.round(g, sx - bw / 2 + s, sy - bh - lift + s, bw - 2 * s, bh - 2 * s, Ui.RED, s);
		Ui.centred(g, mc.font, "!", sx, sy - bh - lift + 3 * s, 0xFFFFFFFF, s, true);
	}
}
