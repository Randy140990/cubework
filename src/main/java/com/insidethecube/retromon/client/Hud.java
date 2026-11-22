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
		// (Under everything else: the dust in a cave's air over the world.)
		HudElementRegistry.addLast(com.insidethecube.retromon.Retromon.id("haze"), Hud::haze);
		HudElementRegistry.addLast(com.insidethecube.retromon.Retromon.id("cinema"), Hud::cinema);
		HudElementRegistry.addLast(com.insidethecube.retromon.Retromon.id("exclaim"), Hud::exclaim);
		HudElementRegistry.addLast(com.insidethecube.retromon.Retromon.id("hints"), Hud::hints);
		HudElementRegistry.addLast(com.insidethecube.retromon.Retromon.id("fade"), Hud::fade);
		HudElementRegistry.addLast(com.insidethecube.retromon.Retromon.id("place"), Hud::place);
		HudElementRegistry.addLast(com.insidethecube.retromon.Retromon.id("speed"), Hud::speed);
		HudElementRegistry.addLast(com.insidethecube.retromon.Retromon.id("badge"), Hud::badgeNotice);
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

	/** How long this fade lasts (ticks): a door's is quick; WAIT's is the viewer's (in, "Time passes...", out). */
	private static int fadeLen = FADE;
	private static final int WAIT_FADE = 56;
	/**
	 * A door's iris (the viewer's): closing on you over 0.85 s right down to nothing, black while the server moves you,
	 * opening round you over 0.7 s. CLOSED: the tick it's fully shut.
	 */
	private static final int DOOR_FADE = 42, CLOSED = 17;
	/** What to do once it's faded back (WAIT: "It's morning now."). */
	private static Runnable afterFade;

	/** Arriving in the world: black, fading out as it comes in (ticks left). */
	private static int arrive;

	public static void arrive() {
		arrive = 34;
		// A new game: the loading page carries on over your room till it's drawn and running smoothly.
		if (IntroScreen.starting()) {
			page = 1;
			pageTicks = 0;
			smoothFrames = 0;
		}
	}

	/**
	 * The loading page over the world as a new game arrives (1 showing, 2 going to black, 0 gone), how long it's been up
	 * (ticks), and how many frames in a row have come quickly (the world's settled: no more stalls as it's built).
	 */
	private static int page, pageTicks, pageOut, smoothFrames;
	private static long lastFrame;

	private static void arrivingPage(GuiGraphics g) {
		if (page == 0) return;
		long now = System.nanoTime();
		smoothFrames = lastFrame != 0 && now - lastFrame < 45_000_000L ? smoothFrames + 1 : 0;
		lastFrame = now;
		IntroScreen.drawArriving(g);
		if (page == 2) g.fill(0, 0, g.guiWidth(), g.guiHeight(), Ui.alpha(0xFF000000, Math.min(1, pageOut / 10F)));
	}

	private static void arrivingTick(Minecraft mc) {
		if (page == 0) return;
		pageTicks++;
		Adventure.frozen = Math.max(Adventure.frozen, 2);
		arrive = 34;
		boolean ready = mc.player != null && mc.level != null && mc.screen == null && pageTicks >= 30 && smoothFrames >= 40;
		if (page == 1 && (ready || pageTicks > 600)) page = 2;
		if (page == 2 && ++pageOut >= 10) {
			// Black now: the world fades in from it, as it does after any loading.
			page = 0;
			pageOut = 0;
		}
	}

	public static void fadeThrough(Runnable then) {
		fadeThrough(then, "Time passes...");
	}

	public static void fadeThrough(Runnable then, String words) {
		fadeThrough(then, words, null);
	}

	public static void fadeThrough(Runnable then, String words, Runnable after) {
		fadeWords = words == null ? "" : words;
		fadeLen = fadeWords.isEmpty() ? DOOR_FADE : WAIT_FADE;
		fade = fadeLen;
		darkSaid = false;
		atDark = then;
		afterFade = after;
		doorLevel = Minecraft.getInstance().level;
		doorHold = 0;
	}

	/** The level a door's iris began in (an interior is a level of its own), and ticks to stay black once it's changed. */
	private static Object doorLevel;
	private static int doorHold;
	/** Where you were when the iris shut (a door within the same level: a room to the next, up the stairs): it stays shut till you've been moved. */
	private static net.minecraft.world.phys.Vec3 darkAt;
	private static int darkWait;
	/** Whether this door's iris has told the server it's shut. */
	private static boolean darkSaid;

	public static void tick(Minecraft mc) {
		dust(mc);
		cineOld = cine;
		boolean want = Adventure.active() && !BattleScene.active()
				&& (mc.screen instanceof DialogueScreen || mc.screen instanceof StarterScreen || mc.screen instanceof NicknameScreen && cine > 0
						|| mc.screen == null && Adventure.frozen > 0);
		cine += ((want ? 1 : 0) - cine) * (want ? 0.14F : 0.2F);
		if (Math.abs(cine - (want ? 1 : 0)) < 0.01F) cine = want ? 1 : 0;
		boolean hold = false;
		if (fade > 0 && fadeWords.isEmpty()) {
			// Through a door into another level (an interior): the iris stays shut while Minecraft's loading screen
			// is up and a moment after you're in (so the room's there), then opens round you; if the change came
			// after it began to open, it shuts again first.
			if (mc.level != doorLevel) {
				if (doorLevel != null && fadeLen - fade > CLOSED) fade = Math.max(fade, fadeLen - CLOSED - 2);
				doorHold = 6;
				doorLevel = mc.level;
				darkAt = null;
			}
			// Black till you've actually been moved, so you never glimpse where you were (or the next room arriving)
			// as it opens; then a moment for the new room to be drawn.
			if (darkAt != null && fadeLen - fade >= CLOSED + 2) {
				boolean moved = mc.player == null || mc.player.position().distanceToSqr(darkAt) > 2.5 * 2.5;
				if (moved) {
					darkAt = null;
					doorHold = Math.max(doorHold, 4);
				} else if (darkWait-- > 0) hold = true;
				else darkAt = null;
			}
			boolean loading = mc.level == null || mc.screen != null && !(mc.screen instanceof GameScreen) && !(mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen);
			if (fadeLen - fade >= CLOSED + 2 && (loading || doorHold > 0)) {
				hold = true;
				if (!loading) doorHold--;
			}
		}
		// Shut: tell the server (it moves you now), and stay shut till you've been moved.
		if (fade > 0 && fadeWords.isEmpty() && atDark == null && !darkSaid && fadeLen - fade >= CLOSED) {
			darkSaid = true;
			if (mc.player != null && mc.level == doorLevel) {
				darkAt = mc.player.position();
				darkWait = 120;
			}
			net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new com.insidethecube.retromon.game.Net.Act("dark", new net.minecraft.nbt.CompoundTag()));
		}
		if (fade > 0 && !hold) {
			fade--;
			if (fade == fadeLen / 2 && atDark != null) {
				atDark.run();
				atDark = null;
				if (fadeWords.isEmpty() && mc.player != null) {
					darkAt = mc.player.position();
					darkWait = 60;
				}
			}
			if (fade == 0 && afterFade != null) {
				Runnable r = afterFade;
				afterFade = null;
				r.run();
			}
		}
		arrivingTick(mc);
		if (page == 0 && (arrive > 0 && mc.level != null && mc.screen == null || arrive > 0 && mc.screen instanceof DialogueScreen)) arrive--;
		moneyTick(mc);
		if (badgeShown >= 0 && mc.screen == null) badgeTicks++;
	}

	// ---------- a badge won: the UI Viewer's notice ----------

	private static int badgeShown = -1, badgeTicks;
	private static final String[] BADGE_NAMES = {"BOULDERBADGE", "CASCADEBADGE", "THUNDERBADGE", "RAINBOWBADGE", "SOULBADGE", "MARSHBADGE", "VOLCANOBADGE", "EARTHBADGE"};

	/** Shows the notice for badge b (0 to 7) once you're back out in the world. */
	public static void badge(int b) {
		badgeShown = b;
		badgeTicks = 0;
	}

	/** The badge, its name, and the eight slots with the ones you have lit, sliding in at the top right for a few seconds. */
	private static void badgeNotice(GuiGraphics g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		if (badgeShown < 0 || !Adventure.active() || mc.screen != null) return;
		if (badgeTicks > 14 + 76 + 10) {
			badgeShown = -1;
			return;
		}
		V.fit(g.guiWidth(), g.guiHeight());
		float t = badgeTicks + delta.getGameTimeDeltaPartialTick(true);
		// First (the viewer's): the big badge flies up to where the notice will be, fading, as the dim lifts.
		if (t < 14) {
			float q = V.bezier(0.42F, 0, 1, 1, Math.min(1, t / 14F)), dim = Math.max(0, 1 - t / 10F);
			if (dim > 0) g.fill(0, 0, g.guiWidth(), g.guiHeight(), Ui.alpha(0xFF0A181E, 0.3F * dim));
			float x0 = 520, y0 = 100, s0 = 240, x1 = 1018, y1 = 100, s1 = 56;
			float bx = x0 + (x1 - x0) * q, by = y0 + (y1 - y0) * q, bs = s0 + (s1 - s0) * q;
			V.icon(g, "badge_" + badgeShown, bx, by, bs, Ui.alpha(0xFFFFFFFF, 1 - q));
		}
		// Then the notice: the badge, its name, and the eight slots with the ones you have lit.
		float nt = t - 14, in = Math.max(0, Math.min(1, nt / 9F)), out = nt > 76 ? Math.min(1, (nt - 76) / 10F) : 0;
		float a = in * (1 - out), x = 870 + (1 - V.bezier(0, 0, 0.58F, 1, in)) * 40, y = 96;
		if (a < 0.02F) return;
		float h = 12 + 18 + 6 + 21 * 1.36F + 6 + 22 + 12;
		V.panel(g, x, y, 386, h, 22, false);
		V.icon(g, "badge_" + badgeShown, x + 12, y + (h - 58) / 2, 58, Ui.alpha(0xFFFFFFFF, a));
		float tx = x + 12 + 58 + 14;
		V.label(g, Lang.t("Badge"), tx, y + 12, Ui.alpha(V.MUTED, a));
		V.text(g, BADGE_NAMES[badgeShown], tx, y + 12 + 18 + 6, 21, Ui.alpha(V.INK, a), true);
		java.util.Set<String> have = ClientState.data.badges;
		for (int j = 0; j < 8; j++)
			V.icon(g, "badge_" + j, tx + j * 27, y + 12 + 18 + 6 + 21 * 1.36F + 6, 22,
					Ui.alpha(have.contains(BADGE_NAMES[j]) || j == badgeShown ? 0xFFFFFFFF : 0x808C8C8C, a));
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
		V.text(g, total, 640 - w / 2 + 58, y + 15, 20, Ui.alpha(V.INK, k), true);
		V.text(g, change, 640 - w / 2 + 58 + tw + 16, y + 16, 18, Ui.alpha(d >= 0 ? 0xFF2FA86C : 0xFFE5525C, k), true);
		g.pose().popMatrix();
	}


	// ---------- a cave's dusty air (Diglett's Cave, as Let's Go's: misty and brown) ----------

	/** How far into the haze you are (it eases in and out going through a door), last tick and now. */
	private static float hazeK, hazeOld;
	private static int hazeColour;
	private static float hazeThick;

	/** Every tick: the haze eases towards the room's, and motes of dust drift about you. */
	private static void dust(Minecraft mc) {
		hazeOld = hazeK;
		double[] h = mc.player == null || !Adventure.inSave ? null : com.insidethecube.retromon.game.Interiors.hazeOf(mc.player);
		if (h != null) {
			hazeColour = ((int) h[0] << 16) | ((int) h[1] << 8) | (int) h[2];
			hazeThick = (float) h[3];
		}
		hazeK += ((h != null ? 1 : 0) - hazeK) * 0.08F;
		if (h == null || mc.level == null || mc.isPaused()) return;
		var r = mc.level.getRandom();
		for (int i = 0; i < 3; i++) {
			double x = mc.player.getX() + (r.nextDouble() - 0.5) * 18, y = mc.player.getY() + r.nextDouble() * 4, z = mc.player.getZ() + (r.nextDouble() - 0.5) * 14;
			mc.level.addParticle(net.minecraft.core.particles.ParticleTypes.WHITE_ASH, x, y, z, (r.nextDouble() - 0.5) * 0.01, -0.002, (r.nextDouble() - 0.5) * 0.01);
		}
	}

	/** The haze: the dust's colour over the whole picture, thicker towards the edges (as the cave's far end fades). */
	private static void haze(GuiGraphics g, DeltaTracker delta) {
		float k = hazeOld + (hazeK - hazeOld) * delta.getGameTimeDeltaPartialTick(true);
		if (k < 0.01F || BattleScene.active()) return;
		int w = g.guiWidth(), h = g.guiHeight();
		int base = (int) (255 * hazeThick * k) << 24 | hazeColour, edge = (int) (255 * Math.min(0.85F, hazeThick * 2.2F) * k) << 24 | hazeColour, clear = hazeColour;
		g.fill(0, 0, w, h, base);
		g.fillGradient(0, 0, w, h / 3, edge, clear);
		g.fillGradient(0, h - h / 4, w, h, clear, edge);
	}

	private static void cinema(GuiGraphics g, DeltaTracker delta) {
		float k = cineOld + (cine - cineOld) * delta.getGameTimeDeltaPartialTick(true);
		if (k > 0.005F) Ui.cinema(g, g.guiWidth(), g.guiHeight(), k);
	}

	private static void fade(GuiGraphics g, DeltaTracker delta) {
		if (page != 0) {
			arrivingPage(g);
			return;
		}
		if (arrive > 0) g.fill(0, 0, g.guiWidth(), g.guiHeight(), Ui.alpha(0xFF000000, Math.min(1, (arrive - delta.getGameTimeDeltaPartialTick(true)) / 20F)));
		if (fade <= 0) return;
		float t = fadeLen - fade + delta.getGameTimeDeltaPartialTick(true);
		if (fadeWords.isEmpty()) {
			// Through a door, as the games' iris: a circle closes in on you until it's black, the place changes,
			// and the circle opens out again round you (centred on you, wherever you are on the screen).
			float close = CLOSED / 20F, open = 0.7F, dark = (fadeLen / 20F) - close - open;
			float secs = t / 20F, k = secs < close ? 1 - (float) Math.pow(secs / close, 1.5) : secs < close + dark ? 0 : (float) Math.min(1, Math.pow((secs - close - dark) / open, 0.8));
			V.fit(g.guiWidth(), g.guiHeight());
			float ox = 640, oy = 360;
			Minecraft mc = Minecraft.getInstance();
			if (mc.player != null) {
				float[] at = Adventure.project(mc.player.getPosition(delta.getGameTimeDeltaPartialTick(true)).add(0, 1.0, 0), g.guiWidth(), g.guiHeight());
				if (at != null) {
					ox = Math.max(0, Math.min(1280, V.vx(at[0])));
					oy = Math.max(0, Math.min(720, V.vy(at[1])));
				}
			}
			float far = (float) Math.hypot(g.guiWidth(), g.guiHeight()) / V.s(1) + 40, r = k * far;
			if (r < 1) g.fill(0, 0, g.guiWidth(), g.guiHeight(), 0xFF000000);
			else V.annulus(g, ox, oy, r, far * 2, 0xFF000000, 0xFF000000);
			return;
		}
		// WAIT, as the viewer's: the screen dims (0.7 s), "Time passes..." comes up and goes, and it lightens again.
		float a = t < 14 ? t / 14 : t > fadeLen - 14 ? (fadeLen - t) / 14 : 1;
		g.fill(0, 0, g.guiWidth(), g.guiHeight(), Ui.alpha(0xFF000000, Math.max(0, Math.min(1, a)) * 0.92F));
		float w = t < 18 ? 0 : t < 28 ? (t - 18) / 10 : t < 46 ? 1 : Math.max(0, 1 - (t - 46) / 10);
		if (w > 0) {
			V.fit(g.guiWidth(), g.guiHeight());
			V.titleCentred(g, fadeWords, 640, 330, 34, Ui.alpha(0xFFFFFFFF, w));
		}
	}

	// ---------- the prompt over what you can talk to ----------

	/**
	 * Over whoever (or whatever) SPACE would talk to or look at: the Interior Viewer's prompt, a small pill with the
	 * key and the word (Talk, Look), bobbing gently.
	 */
	private static void prompt(GuiGraphics g, Minecraft mc, DeltaTracker delta) {
		Adventure.Prompt pr = Adventure.prompt();
		if (pr == null) return;
		float[] at = Adventure.project(pr.at(), g.guiWidth(), g.guiHeight());
		if (at == null) return;
		float cx = V.vx(at[0]), cy = V.vy(at[1]) - 6 + (float) Math.sin((mc.level.getGameTime() + delta.getGameTimeDeltaPartialTick(true)) / 6.0) * 2;
		String k = key(mc.options.keyJump), word = pr.word();
		word = Lang.t(word);
		float kw = V.keyWidth(k, 30), ww = V.width(word, 17, false), w = 5 + kw + 8 + ww + 16, h = 40, x = cx - w / 2, y = cy - h;
		V.pill(g, x - 2, y - 2, w + 4, h + 4, 0x99FFFFFF);
		V.softShadow(g, x, y + 4, w, h, 12, 0x381E2854);
		V.pill(g, x, y, w, h, 0xF2FFFFFF);
		V.key(g, k, x + 5, y + 5, 30);
		V.text(g, word, x + 5 + kw + 8, V.capsTop(y + 20, 17), 17, V.INK, false);
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
		String clock = "DAY " + day() + "  \u00B7  " + String.format("%d:%02d %s", hour % 12 == 0 ? 12 : hour % 12, minute, hour < 12 ? "AM" : "PM");
		// (On a SAFARI GAME, 15:29: the steps and SAFARI BALLs left, in place of the time.)
		if (ClientState.safariSteps >= 0) clock = "STEPS " + ClientState.safariSteps + "/600  \u00B7  BALLS " + ClientState.safariBalls;
		// (The A key's word is what it'd do now: Talk, Pick up, Look.)
		String aWord = Adventure.prompt() != null ? Adventure.prompt().word() : "Talk";
		V.hints(g, "SPACE".equals(key(mc.options.keyJump)) ? "SPACE" : key(mc.options.keyJump), aWord, key(Adventure.START), "Menu", key(Adventure.SPEED),
				Lang.t("Speed x1").replace("1", String.valueOf(Adventure.speed)));
		V.HINTS.clear();
		prompt(g, mc, delta);
		// Where you are: a white pill, a sunny dot (a moon's at night), the place in Fredoka and the time on a coral tab.
		String place = ClientState.placeName.isEmpty() ? "KANTO" : ClientState.placeName;
		float pw = V.titleWidth(place, 24), tw = V.width(clock, 13, true) + 1 + 24, w0 = 14 + 16 + 12 + pw + 12 + 6 + tw + 8, ph = 24 * 1.5F + 12, px = 24, py = 20;
		V.pill(g, px - 3, py - 3, w0 + 6, ph + 6, 0x8CFFFFFF);
		V.shadow(g, px, py, w0, ph, ph / 2, 0.6F);
		V.pill(g, px, py, w0, ph, 0xFFFFFFFF);
		float dx = px + 14, dcy = py + ph / 2;
		V.ellipse(g, dx + 8, dcy, 11, 11, night ? 0xFFDDE3F0 : 0xFFFFE9A0);
		V.ellipse(g, dx + 8, dcy, 8, 8, night ? 0xFFF4F6FF : 0xFFFFD34E);
		V.title(g, place, dx + 16 + 12, V.capsTop(dcy, 24), 24, V.INK);
		float tx = dx + 16 + 12 + pw + 12 + 6;
		V.rrv(g, tx, dcy - 11.5F, tw, 23, 11.5F, 0xFFFF9473, 0xFFE5525C);
		V.text(g, clock, tx + 12, V.capsTop(dcy, 13), 13, 0xFFFFFFFF, true);
		// What to do next: a white card, a blue NEXT tab, the map's pin, and how far.
		String goal = goal(mc);
		if (goal != null) {
			goal = Lang.t(goal);
			float w = 390, x = V.W - 24 - w, y = 20, hh = 22 + 21 * 1.5F + 12;
			V.rr(g, x - 3, y - 3, w + 6, hh + 6, 25, 0x8CFFFFFF);
			V.shadow(g, x, y, w, hh, 22, 0.6F);
			V.rr(g, x, y, w, hh, 22, 0xFFFFFFFF);
			String nx = Lang.t("NEXT");
			float nw = V.trackedWidth(nx, 13, 1.4F) + 28;
			V.pill(g, x + 16 - 3, y - 11 - 3, nw + 6, 26, 0xFFFFFFFF);
			V.rrv(g, x + 16, y - 11, nw, 20, 10, 0xFF7CC2FA, 0xFF3C8DE4);
			V.tracked(g, nx, x + 16 + 14, V.capsTop(y - 1, 13), 13, 0xFFFFFFFF, 1.4F);
			float cy = y + 22 + 21 * 1.5F / 2;
			V.icon(g, "picto_map", x + 18, cy - 15, 30);
			V.text(g, goal, x + 18 + 30 + 10, V.capsTop(cy, 21), 21, V.INK, true);
			String far = goalDistance(mc);
			if (far != null) V.right(g, far, x + w - 18, V.capsTop(cy, 16), 16, V.MUTED, true);
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
	 * The place's name low on the screen, as Let's Go shows it (and the UI Viewer's banner): two fine white lines
	 * sweep out across the width, a soft brushed streak in the place's own colours wipes in between them, and the
	 * name sits on it in big white letters with a dark edge, the sunny dot before it and the town's line under it.
	 * It holds, then fades as it sinks.
	 */
	private static void place(GuiGraphics g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		int t = ClientState.placeTicks;
		if (!Adventure.active() || BattleScene.active() || t < 0 || t > 90 || ClientState.placeName.isEmpty() || mc.screen != null && !(mc.screen instanceof GameScreen)) return;
		// Low on the screen, so not under someone's words (the text box is there).
		if (mc.screen instanceof DialogueScreen) return;
		V.fit(g.guiWidth(), g.guiHeight());
		float f = t + delta.getGameTimeDeltaPartialTick(true);
		float in = smooth(Math.min(1, f / 10F)), out = f > 64 ? smooth(Math.min(1, (f - 64) / 16F)) : 0, alpha = in * (1 - out);
		if (alpha <= 0.02F) return;
		float lines = easeOut(Math.min(1, f / 15F)), wipe = easeOut(Math.max(0, Math.min(1, (f - 2.4F) / 18F)));
		String name = ClientState.placeName, line = Adventure.signLine(name);
		line = line.isEmpty() ? "" : Lang.t(line);
		float top = 560 + 8 * out;
		// A soft shade between the lines, so the white name reads on bright grass and sand.
		V.rrh(g, 0, top + 8, V.W, 74, 0, new float[] {0, 0.3F, 0.7F, 1},
				new int[] {Ui.alpha(0x00101834, alpha), Ui.alpha(0x33101834, alpha), Ui.alpha(0x33101834, alpha), Ui.alpha(0x00101834, alpha)});
		// The two lines, from the middle outwards, fading at the ends.
		float hw = 640 * lines;
		for (float ly : new float[] {top + 6, top + 82})
			V.rrh(g, 640 - hw, ly, hw * 2, 2, 0, new float[] {0, 0.24F, 0.76F, 1},
					new int[] {Ui.alpha(0x00FFFFFF, alpha), Ui.alpha(0xEBFFFFFF, alpha), Ui.alpha(0xEBFFFFFF, alpha), Ui.alpha(0x00FFFFFF, alpha)});
		// The streak, wiping in from the left: the place's colours, fading at both ends, with a light stroke along
		// its top and a deeper one under it (a brush's marks).
		int[] pal = placeColours(name);
		// (Right up to the two lines: from just under the top one to just over the bottom one.)
		float sx = 240, sw = 800, sy = top + 8, sh = 74;
		if (wipe > 0.01F) {
			g.enableScissor(0, 0, Math.round(V.x(sx + sw * wipe)), g.guiHeight());
			float[] at = new float[pal.length + 2];
			int[] cols = new int[pal.length + 2];
			at[0] = 0;
			cols[0] = Ui.alpha(pal[0] & 0xFFFFFF, alpha);
			for (int k = 0; k < pal.length; k++) {
				at[k + 1] = 0.16F + 0.68F * k / Math.max(1, pal.length - 1);
				cols[k + 1] = Ui.alpha(0xE6000000 | pal[k], alpha);
			}
			at[pal.length + 1] = 1;
			cols[pal.length + 1] = Ui.alpha(pal[pal.length - 1] & 0xFFFFFF, alpha);
			V.rrh(g, sx, sy, sw, sh, 0, at, cols);
			V.rrh(g, sx + 60, sy + 6, sw - 150, 14, 7, new float[] {0, 0.3F, 0.7F, 1},
					new int[] {Ui.alpha(0x00FFFFFF, alpha), Ui.alpha(0x52FFFFFF, alpha), Ui.alpha(0x2EFFFFFF, alpha), Ui.alpha(0x00FFFFFF, alpha)});
			int last = pal[pal.length - 1];
			V.rrh(g, sx + 120, sy + sh - 16, sw - 160, 10, 5, new float[] {0, 0.6F, 1},
					new int[] {Ui.alpha(pal[0] & 0xFFFFFF, alpha), Ui.alpha(0x80000000 | last, alpha), Ui.alpha(last & 0xFFFFFF, alpha)});
			g.disableScissor();
		}
		// The name, with the sunny dot (a moon's at night) before it.
		float size = 44, track = 5, nw = V.titleTrackedWidth(name, size, track), gap = 14, dot = 16, total = dot + gap + nw;
		float rowY = top + 16 + 22, nx = 640 - total / 2 + dot + gap, ny = V.capsTop(rowY, size);
		boolean night = Night.nightness() > 0.5F;
		float dcx = 640 - total / 2 + dot / 2;
		// (Radii: the glow round it, its dark edge, the dot, its shine.)
		V.ellipse(g, dcx, rowY, dot / 2 + 3, dot / 2 + 3, Ui.alpha(night ? 0xB3DDE3F0 : 0xB3FFE9A0, alpha));
		V.ellipse(g, dcx, rowY + 0.5F, dot / 2 + 0.5F, dot / 2 + 0.5F, Ui.alpha(0x99203050, alpha));
		V.ellipse(g, dcx, rowY, dot / 2, dot / 2, Ui.alpha(night ? 0xFFF4F6FF : 0xFFFFC93C, alpha));
		V.ellipse(g, dcx - 2, rowY - 2, dot * 0.2F, dot * 0.2F, Ui.alpha(night ? 0xFFFFFFFF : 0xCCFFF3A6, alpha));
		V.softShadow(g, nx + 10, rowY - size * 0.3F + 5, nw - 20, size * 0.6F, 18, Ui.alpha(0x3A0E1830, alpha));
		int edge = Ui.alpha(0xFF203050, alpha);
		float[][] off = {{-2, 0}, {2, 0}, {0, -2}, {0, 2}, {-1.5F, -1.5F}, {1.5F, -1.5F}, {-1.5F, 1.5F}, {1.5F, 1.5F}};
		for (float[] o : off) V.titleTracked(g, name, nx + o[0], ny + o[1], size, edge, track);
		V.titleTracked(g, name, nx, ny, size, Ui.alpha(0xFFFFFFFF, alpha), track);
		if (!line.isEmpty()) {
			float lw = V.width(line, 16, true), lx = 640 - lw / 2, ly = V.capsTop(top + 60 + 9, 16);
			for (float[] o : new float[][] {{-1, 0}, {1, 0}, {0, -1}, {0, 1}}) V.text(g, line, lx + o[0], ly + o[1], 16, edge, true);
			V.text(g, line, lx, ly, 16, Ui.alpha(0xFFFFF4DC, alpha), true);
		}
	}

	/**
	 * Each place's colours for its banner's streak (left to right, RGB): the towns' own, as their signs and names
	 * have them; routes green into teal; anywhere else (rooms) violet into blue into cyan, as Let's Go's banner.
	 */
	private static int[] placeColours(String place) {
		return switch (place) {
			case "PALLET TOWN" -> new int[] {0xFF6F91, 0xFFB547, 0x4FC3F7};
			case "VIRIDIAN CITY" -> new int[] {0x1E9E5A, 0x3CC47C, 0x9BE05A};
			case "PEWTER CITY" -> new int[] {0x5F6B80, 0x98A2B3, 0x6E7A8C};
			case "CERULEAN CITY" -> new int[] {0x2E7BE0, 0x3FB5F0, 0x7FDBFF};
			case "VERMILION CITY" -> new int[] {0xF04A2C, 0xFF8A3D, 0xFFC45A};
			case "LAVENDER TOWN" -> new int[] {0x7A4FD0, 0xA178E8, 0xC9A8F7};
			case "CELADON CITY" -> new int[] {0xFF5C7F, 0xFFC233, 0x3CC47C, 0x3C9BEB, 0xA06CDB};
			case "FUCHSIA CITY" -> new int[] {0xD02F7E, 0xFF5FA2, 0xFF9BC8};
			case "SAFFRON CITY" -> new int[] {0xE8900F, 0xFFC233, 0xFFE070};
			case "CINNABAR ISLAND" -> new int[] {0xD9302C, 0xFF6A3C, 0xFFA84A};
			case "INDIGO PLATEAU" -> new int[] {0x3A3FA8, 0x6C6FE0, 0xA8A6FF};
			default -> place.contains("ROUTE") || place.contains("FOREST") || place.contains("GATE") ? new int[] {0x2FA864, 0x2FA88E, 0x4FA8F0}
					: new int[] {0x7A4FD0, 0x3C7FE0, 0x36B8F0};
		};
	}

	private static float easeOut(float x) {
		float k = 1 - x;
		return 1 - k * k * k;
	}

	private static float smooth(float x) {
		return x * x * (3 - 2 * x);
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

	/** Which day of your adventure it is (the first is day 1), from the time of day and the day it began. */
	public static long day() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return 1;
		long t = mc.level.getDayTime(), d0 = Math.max(0, ClientState.data.day0);
		return Math.max(1, Math.floorDiv(t - d0, 24000L) + 1);
	}

	/**
	 * The game speed, when it's sped up: a white pill as the place's (the UI Viewer's), a teal disc with the two
	 * arrows and the speed beside it. Under the place's pill out in the world; at the top left in a battle.
	 */
	private static void speed(GuiGraphics g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		if (!Adventure.active() || Adventure.speed <= 1 || mc.options.hideGui) return;
		V.fit(g.guiWidth(), g.guiHeight());
		boolean world = mc.screen == null && !BattleScene.active() && !Device.busy();
		String t = "x" + Adventure.speed;
		float h = 40, tw = V.titleWidth(t, 22), w = 8 + 28 + 10 + V.trackedWidth("SPEED", 12, 1.5F) + 10 + tw + 16, x = 24, y = world ? 20 + 48 + 12 : 20;
		V.pill(g, x - 3, y - 3, w + 6, h + 6, 0x8CFFFFFF);
		V.shadow(g, x, y, w, h, h / 2, 0.6F);
		V.pill(g, x, y, w, h, 0xFFFFFFFF);
		float cx = x + 8 + 14, cy = y + h / 2;
		V.ellipse(g, cx, cy + 1, 14, 14, 0x33123C38);
		V.ellipse(g, cx, cy, 14, 14, 0xFF2BB5A0);
		V.ellipse(g, cx, cy, 12, 12, 0xFF3CCBB2);
		V.tri(g, cx - 3.5F, cy, 9, 0, 0xFFFFFFFF);
		V.tri(g, cx + 3.5F, cy, 9, 0, 0xFFFFFFFF);
		float lx = x + 8 + 28 + 10;
		V.tracked(g, "SPEED", lx, V.capsTop(cy, 12), 12, 0xFF8A93A8, 1.5F);
		V.title(g, t, lx + V.trackedWidth("SPEED", 12, 1.5F) + 10, V.capsTop(cy, 22), 22, V.INK);
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
		int s = Ui.scale(h);
		// Pops up, then stays: the same smooth bubble as every other emote.
		float pop = Math.min(1, (40 - ClientState.exclaimTicks) / 4F);
		Ui.bubble(g, "exclaim", sx, sy - (int) ((1 - pop) * 6 * s), s, pop);
	}
}
