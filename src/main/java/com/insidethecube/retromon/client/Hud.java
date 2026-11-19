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
	/** The words shown at the darkest ("Time passes..." for WAIT; none going through a door). */
	private static String fadeWords = "";

	public static void fadeThrough(Runnable then) {
		fadeThrough(then, "Time passes...");
	}

	public static void fadeThrough(Runnable then, String words) {
		fade = FADE;
		atDark = then;
		fadeWords = words == null ? "" : words;
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

	/** Your money changing (prize money, a sale), as the UI Viewer's notice: a pill popping up at the top middle. */
	private static void money(GuiGraphics g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		if (!Adventure.active() || BattleScene.active() || moneyTicks < 0 || mc.screen != null || mc.options.hideGui) return;
		V.fit(g.guiWidth(), g.guiHeight());
		float t = moneyTicks + delta.getGameTimeDeltaPartialTick(true);
		float k = Math.max(0, Math.min(1, Math.min(t / 8F, (MONEY_SHOW - t) / 10F)));
		float c = Math.max(0, Math.min(1, (t - 8) / 24F));
		int shown = Math.round(moneyFrom + (moneyTo - moneyFrom) * (1 - (1 - c) * (1 - c)));
		int d = moneyTo - moneyFrom;
		String total = Ui.money(shown), change = (d >= 0 ? "+" : "-") + Ui.money(Math.abs(d));
		float tw = V.width(total, 20, true), cw = V.width(change, 18, true), w = 12 + 34 + 12 + tw + 16 + cw + 22, y = 96 - (1 - k) * 20;
		int a = Math.round(k * 255);
		if (a < 4) return;
		g.pose().pushMatrix();
		V.panel(g, 640 - w / 2, y, w, 54, 27, true);
		V.rr(g, 640 - w / 2 + 12, y + 10, 34, 34, 17, Ui.alpha(0xFFE0A526, k));
		V.centred(g, "₽", 640 - w / 2 + 29, y + 15, 20, Ui.alpha(0xFF5A3A00, k), true);
		V.text(g, total, 640 - w / 2 + 58, y + 15, 20, Ui.alpha(V.TEXT, k), true);
		V.text(g, change, 640 - w / 2 + 58 + tw + 16, y + 16, 18, Ui.alpha(d >= 0 ? 0xFF7CF0A8 : 0xFFFF8A8A, k), true);
		g.pose().popMatrix();
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
		if (a > 0.85F && !fadeWords.isEmpty()) {
			Minecraft mc = Minecraft.getInstance();
			int s = Ui.scale(g.guiHeight());
			Ui.centred(g, mc.font, fadeWords, g.guiWidth() / 2, g.guiHeight() / 2 - 4 * s, Ui.alpha(0xFFE8ECFF, (a - 0.85F) / 0.15F), s, false);
		}
	}

	// ---------- the buttons and the time, small at the bottom right ----------

	/** A key as it's bound now (SPACE, ENTER, TAB), so the hints follow your controls. */
	private static String key(net.minecraft.client.KeyMapping k) {
		return k == null ? "?" : k.getTranslatedKeyMessage().getString().toUpperCase(java.util.Locale.ROOT);
	}

	/**
	 * What's over the world (the UI Viewer's HUD): the keys along the bottom right (SPACE Talk, ENTER Menu, TAB
	 * Speed), the time beside them, and what to do next in its panel at the top right.
	 */
	private static void hints(GuiGraphics g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		if (!Adventure.active() || BattleScene.active() || mc.screen != null || mc.level == null || mc.options.hideGui || Device.busy()) return;
		V.fit(g.guiWidth(), g.guiHeight());
		long t = Math.floorMod(mc.level.getDayTime(), 24000L);
		int hour = (int) ((t / 1000 + 6) % 24), minute = (int) ((t % 1000) * 60 / 1000);
		boolean night = Night.nightness() > 0.5F;
		String clock = String.format("%d:%02d %s", hour % 12 == 0 ? 12 : hour % 12, minute, hour < 12 ? "AM" : "PM");
		V.hints(g, key(mc.options.keyJump), "Talk", key(Adventure.START), "Menu", key(Adventure.SPEED), "Speed x" + Adventure.speed);
		// The time, at the bottom left, with a sun or a moon.
		float cw = V.width(clock, 17, true) + 44;
		V.panel(g, 22, V.H - 14 - 36, cw, 36, 18, true);
		V.rr(g, 34, V.H - 14 - 36 + 11, 14, 14, 7, night ? 0xFFE8ECFF : 0xFFFFC94A);
		if (night) V.rr(g, 38, V.H - 14 - 36 + 9, 12, 12, 6, 0xFF123C38);
		V.text(g, clock, 56, V.H - 14 - 36 + 9, 17, V.TEXT, true);
		// What to do next.
		String goal = goal(mc);
		if (goal != null) {
			float w = 380, x = V.W - 24 - w, y = 20;
			V.panel(g, x, y, w, 76, 24, true);
			V.label(g, "Next", x + 18, y + 14);
			V.icon(g, "icon_map", x + 18, y + 36, 28);
			V.text(g, goal, x + 56, y + 38, 19, V.TEXT, true);
			String far = goalDistance(mc);
			if (far != null) V.right(g, far, x + w - 18, y + 41, 16, V.MUTED, true);
		}
	}

	/** What to do next in the story, as far as it goes yet. */
	private static String goal(Minecraft mc) {
		if (!ClientState.data.started) return com.insidethecube.retromon.game.Interiors.in(mc.player) ? null : "Go to PROF. OAK's lab";
		return null;
	}

	/** How far it is to the next thing (the lab's door, before you've a POKéMON), or null. */
	private static String goalDistance(Minecraft mc) {
		if (ClientState.data.started || mc.player == null || com.insidethecube.retromon.game.Interiors.in(mc.player)) return null;
		double d = Math.hypot(mc.player.getX() - LAB_DOOR_X, mc.player.getZ() - LAB_DOOR_Z);
		return Math.round(d) + " m";
	}

	/** Oak's lab's door in the Kanto map (outside it). */
	private static final double LAB_DOOR_X = -782.5, LAB_DOOR_Z = 695.6;

	/**
	 * The place's name, as the UI Viewer's banner: a deep teal band sliding in from the left with mint lines above
	 * and below, the name large and the town's line from its sign under it; then away.
	 */
	private static void place(GuiGraphics g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		int t = ClientState.placeTicks;
		if (!Adventure.active() || BattleScene.active() || t < 0 || t > 90 || ClientState.placeName.isEmpty() || mc.screen != null && !(mc.screen instanceof GameScreen)) return;
		V.fit(g.guiWidth(), g.guiHeight());
		float f = t + delta.getGameTimeDeltaPartialTick(true);
		float in = Math.min(1, f / 10F), e = 1 - (1 - in) * (1 - in) * (1 - in), out = f > 64 ? Math.min(1, (f - 64) / 12F) : 0;
		float slide = (1 - e) * -60 - out * 60, alpha = Math.min(in * 1.4F, 1) * (1 - out);
		String name = ClientState.placeName, line = Adventure.signLine(name);
		float h = line.isEmpty() ? 100 : 128, x = -V.ox / V.k + slide, w = 760 + V.ox / V.k, y = 210;
		// The band: solid on the left, fading out to the right.
		float solid = w * 0.6F;
		g.fill(Math.round(V.x(x)), Math.round(V.y(y)), Math.round(V.x(x + solid)), Math.round(V.y(y + h)), Ui.alpha(0xF2123C38, alpha));
		hband(g, x + solid, y, w - solid, h, Ui.alpha(0xF2123C38, alpha));
		hband(g, x, y, w, 3, Ui.alpha(0xFF8FF5C0, alpha));
		hband(g, x, y + h - 3, w, 3, Ui.alpha(0xFF8FF5C0, alpha));
		V.text(g, name, x + V.ox / V.k + 60, y + 18, 54, Ui.alpha(0xFFF4F6FF, alpha), true);
		if (!line.isEmpty()) V.text(g, line, x + V.ox / V.k + 60, y + 86, 21, Ui.alpha(0xFFCFEDE3, alpha), true);
	}

	/** A strip fading out to the right (canvas pixels). */
	private static void hband(GuiGraphics g, float x, float y, float w, float h, int c) {
		int x0 = Math.round(V.x(x)), x1 = Math.round(V.x(x + w)), y0 = Math.round(V.y(y)), y1 = Math.max(y0 + 1, Math.round(V.y(y + h)));
		int steps = Math.max(1, (x1 - x0) / 3);
		for (int i = 0; i < steps; i++) {
			int a = x0 + (x1 - x0) * i / steps, b = x0 + (x1 - x0) * (i + 1) / steps;
			g.fill(a, y0, b, y1, Ui.alpha(c, 1 - i / (float) steps));
		}
	}

	/** The game speed, when it's sped up. */
	private static void speed(GuiGraphics g, DeltaTracker delta) {
		if (!Adventure.active() || Adventure.speed <= 1) return;
		Minecraft mc = Minecraft.getInstance();
		int s = Ui.scale(g.guiHeight());
		String t = "\u25B6\u25B6 x" + Adventure.speed;
		int w = Ui.width(mc.font, t, s) + 12 * s;
		Ui.rr(g, g.guiWidth() - w - 6 * s, 6 * s, w, 14 * s, 7 * s, 0xD8123C38);
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
