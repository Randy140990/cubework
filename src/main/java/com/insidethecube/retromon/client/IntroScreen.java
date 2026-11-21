package com.insidethecube.retromon.client;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.insidethecube.retromon.Retromon;
import com.insidethecube.retromon.game.Game;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * The game's start, as the UI Viewer has it (signed off by John, 20 Nov). INSID3 TH3 CUB3 presents; the painted
 * title (our look) or the pixel Charizard (Cobblemon's); your saves as cards with OPTIONS under them; the keys;
 * the professor walking past a parade of POKéMON as he talks, his words on a dark band at the foot; the maker (a
 * starting face for every skin tone, then the glass panel); your name; your rival's; off you go (you floss); and
 * then the Kanto map (from Retromon Map, the second jar) is copied as a new save slot and you're off to Pallet Town.
 */
public class IntroScreen extends GameScreen {
	private static final Identifier ART = Retromon.id("textures/gui/intro.png");
	private static final int ART_W = 480, ART_H = 270;

	private enum Step {
		OPENING, TITLE, MENU, SLOTS, OPTIONS, KEYS, TALK, MAKER, NAME, COPYING, FAILED
	}

	private final Screen parent;
	/** The opening (INSID3 TH3 CUB3 presents, fading in and out) plays each time the title comes up: at the start and after you quit a game. */
	private static boolean opened;
	private volatile Step step = Step.OPENING;
	/** Ticks into the opening, and since the title came up (its white flash, Charizard's roar). */
	private int openTicks, titleTicks = 99;
	private static final int OPEN_END = 70;
	private Choice menu;
	/** The three save slots (Retromon, Retromon 2, Retromon 3): what's in each, read when the menu opens. */
	private SlotInfo[] slotCards = new SlotInfo[SLOTS];
	/** CONTINUE (pick a game, or an empty slot for a new one) or REPLACE (all three full: which to start over in). */
	private boolean replacing;
	/** The slot a new game goes in (-1: the first empty one), and whether what's there is moved aside first. */
	private int targetSlot = -1;
	private boolean replaceTarget;
	/** Asking before a slot's game is replaced. */
	private Choice confirm;
	private int slotAt;
	private volatile String progress = "";
	private volatile String failure = "";
	private volatile boolean copied;
	/** Oak talking: the lines, and what comes after them. */
	private TextBox talk;
	private Runnable afterTalk;
	private boolean girl;
	/** The character maker (you, then your clothes), and how you made yourself (an outfit). */
	private Maker maker;
	private String look = Outfit.defaults(false).write();
	/** Naming: whose name (you, then your rival), the choices, and the letters when it's a new one. */
	private boolean namingRival;
	private Choice names;
	private NameEntry entry;
	/** When the little yellow one jumped for Oak's shoulder (ticks), or -1; when you waved goodbye, or -1. */
	private int hopAt = -1, waveAt = -1;
	private String playerName = "", rivalName = "";
	/** Fading to black before the game loads (ticks), and when the loading screen came up (it fades in from black). */
	private int fadeOutAt = -1, loadingAt = -1;
	private static final int FADE_OUT = 12;
	private String slotName;
	/** Who's on screen while Oak talks: oak, hero (you) or rival. */
	private String scene = "oak";
	/** The professor's scene: his walk past the parade, his grandson, you. */
	private Scenes.ProfWalk prof = new Scenes.ProfWalk();
	private long lastNanos;
	/** After his words: waiting for him to reach the end and turn, then a fade to black (ticks), then the maker. */
	private boolean waitArrive;
	private int arriveAt = -1, fadeAt = -1;
	/** The options page (from OPTIONS under the slots). */
	private final OptionsPage options = new OptionsPage(() -> {
		if (minecraft != null) minecraft.setScreen(new KeyMapScreen(this));
	});
	/** The maker: 1 the starting faces, 2 making it yours (the glass panel); the face picked; each column's scroll. */
	private int makerStage = 1, pickFace = 3;
	private final float[] faceScroll = new float[2];
	private float side, sideGoal;
	/** Off you go: when his words ended (ticks), then the game loads 1.6 seconds on. */
	private int readyEndAt = -1;
	/**
	 * A save to open once this tick is over: opening a world from inside a screen's tick trips Fabric's screen
	 * events (the screen changes under them), so it waits for the end of the tick.
	 */
	private static String pendingLoad;
	private static Screen pendingParent;

	/** Called at the end of each client tick: opens the save that's waiting, if there is one. */
	public static void tickPending() {
		if (pendingLoad == null) return;
		String folder = pendingLoad;
		Screen parent = pendingParent;
		pendingLoad = null;
		pendingParent = null;
		Minecraft mc = Minecraft.getInstance();
		launchedAt = System.currentTimeMillis();
		mc.createWorldOpenFlows().openWorld(folder, () -> mc.setScreen(new IntroScreen(parent)));
	}

	/** Where you're arriving (a new game's PALLET TOWN, or where the save you continue was left). */
	private static String arrivingAt = "PALLET TOWN";

	private static String arrivingPlace() {
		return arrivingAt == null || arrivingAt.isBlank() ? "PALLET TOWN" : arrivingAt.toUpperCase(java.util.Locale.ROOT);
	}

	/** When the game was set going (ms), so Minecraft's own loading screens show ours instead till you're in; or 0. */
	private static long launchedAt;

	/** Whether a new game's just been set going (the page stays up over the world till it's ready: Hud.arrive). */
	public static boolean starting() {
		return launchedAt != 0;
	}

	/** Whether the world's still loading after the new game set it going (a minute at most). */
	public static boolean launching() {
		Minecraft mc = Minecraft.getInstance();
		if (launchedAt == 0) return false;
		if (System.currentTimeMillis() - launchedAt > 60000 || mc.player != null && Adventure.active() && mc.screen == null) {
			launchedAt = 0;
			return false;
		}
		return true;
	}

	/**
	 * The loading page on its own (drawn over Minecraft's own loading screens while the world comes up): the ball
	 * spinning, PALLET TOWN, a full bar and "Arriving...", as the page before it left off (not Minecraft's words).
	 */
	public static void drawArriving(GuiGraphics g) {
		V.fit(g.guiWidth(), g.guiHeight());
		float secs = (System.currentTimeMillis() % 100000) / 1000F;
		Scenes.bg2(g, "loading");
		float cx = 640, top = 720 * 0.44F - (110 + 22 + 60 + 22 + 18) / 2F;
		g.pose().pushMatrix();
		g.pose().translate(V.x(cx), V.y(top + 55));
		g.pose().rotate((float) (secs / 1.1F * Math.PI * 2));
		g.pose().translate(-V.x(cx), -V.y(top + 55));
		V.icon(g, "icon_ball", cx - 55, top, 110);
		g.pose().popMatrix();
		String place = arrivingPlace();
		float pw0 = V.titleTrackedWidth(place, 40, 4);
		V.titleTracked(g, place, cx - pw0 / 2, V.capsTop(top + 110 + 22 + 30, 40), 40, 0xFFFFFFFF, 4);
		float by = top + 110 + 22 + 60 + 22;
		V.pill(g, cx - 260, by, 520, 18, V.TRACK);
		V.rrh(g, cx - 258, by + 2, 516, 14, 7, new float[] {0, 1}, new int[] {0xFF4FB896, 0xFF8FF5C0});
		V.centred(g, Lang.t("Arriving in {0}...", place), cx, by + 30, 15, 0xCCFFFFFF, false);
	}

	public IntroScreen(Screen parent) {
		super("Retromon");
		this.parent = parent;
	}

	private static Path root() {
		return Minecraft.getInstance().gameDirectory.toPath();
	}

	public static Path mapFolder() {
		return root().resolve("retromon");
	}

	@Override
	protected void init() {
		super.init();
		if (ticks == 0 && step != Step.OPENING) Music.play("title_screen");
	}

	@Override
	public void tick() {
		super.tick();
		if (step == Step.OPENING) {
			// Not while Minecraft's own loading screen is still over it.
			if (minecraft == null || minecraft.getOverlay() == null) openTicks++;
			if (openTicks >= OPEN_END) toTitle();
		}
		if (titleTicks < 99) titleTicks++;
		if (talk != null) talk.tick();
		if (waitArrive && prof.arrived()) {
			if (arriveAt < 0) arriveAt = ticks;
			if (ticks - arriveAt >= 10 && fadeAt < 0) fadeAt = ticks;
		}
		if (fadeAt >= 0 && ticks - fadeAt >= 19) {
			fadeAt = -1;
			waitArrive = false;
			arriveAt = -1;
			startMaker();
		}
		if (readyEndAt >= 0 && ticks - readyEndAt >= 32) {
			readyEndAt = -1;
			prof.floss = 0;
			prof.wave = 0;
			loadingAt = ticks;
			newGame();
		}
		// "Off you go, then!": you wave, then the game loads.
		if (waveAt >= 0 && ticks - waveAt > 18 && step == Step.TALK) {
			// Then everything fades to black, and the game loads behind it.
			waveAt = -1;
			fadeOutAt = ticks;
		}
		if (fadeOutAt >= 0 && ticks - fadeOutAt >= FADE_OUT) {
			fadeOutAt = -1;
			loadingAt = ticks;
			newGame();
		}
		if (copied) {
			copied = false;
			load(slotName);
		}
	}

	// ---------- the menu and the save slots ----------

	/** Up to three games: a NEW GAME with all three full asks which one to start over in. */
	public static final int SLOTS = 3;

	/** A slot's save folder name: Retromon, Retromon 2, Retromon 3 (the names new games have always had). */
	static String slotFolder(int k) {
		return k == 0 ? Game.SAVE_NAME : Game.SAVE_NAME + " " + (k + 1);
	}

	/** What a slot holds: its folder (null when empty), its details (SlotSnapshot writes them), its picture. */
	private record SlotInfo(Path folder, com.google.gson.JsonObject info, Identifier picture, int picW, int picH, String when) {
		boolean empty() {
			return folder == null;
		}

		String str(String k, String or) {
			return info.has(k) && info.get(k).isJsonPrimitive() ? info.get(k).getAsString() : or;
		}

		int num(String k) {
			try {
				return info.has(k) ? info.get(k).getAsInt() : 0;
			} catch (RuntimeException e) {
				return 0;
			}
		}
	}

	private void readSlots() {
		Path saves = root().resolve("saves");
		for (int k = 0; k < SLOTS; k++) {
			Path f = saves.resolve(slotFolder(k));
			if (!Files.exists(f.resolve("level.dat")) || !Files.exists(f.resolve(Game.MARKER))) {
				slotCards[k] = new SlotInfo(null, new com.google.gson.JsonObject(), null, 0, 0, "");
				continue;
			}
			com.google.gson.JsonObject info = new com.google.gson.JsonObject();
			try {
				Path j = f.resolve(SlotSnapshot.INFO);
				if (Files.exists(j)) info = com.google.gson.JsonParser.parseString(Files.readString(j, StandardCharsets.UTF_8)).getAsJsonObject();
			} catch (Exception e) {
				Retromon.LOG.warn("Couldn't read slot {}'s details", k + 1, e);
			}
			if (!info.has("name")) {
				// An older save: at least the trainer's name from its marker.
				try {
					java.util.regex.Matcher m = PLAYER.matcher(Files.readString(f.resolve(Game.MARKER), StandardCharsets.UTF_8));
					if (m.find()) info.addProperty("name", m.group(1));
				} catch (IOException ignored) {
					// No name, then.
				}
			}
			String when = "";
			try {
				when = java.time.format.DateTimeFormatter.ofPattern("d MMM, HH:mm").format(
						java.time.LocalDateTime.ofInstant(Files.getLastModifiedTime(f.resolve("level.dat")).toInstant(), java.time.ZoneId.systemDefault()));
			} catch (IOException ignored) {
				// No date, then.
			}
			Identifier pic = null;
			int pw = 0, ph = 0;
			for (String name : new String[] {SlotSnapshot.PICTURE, "icon.png"}) {
				Path img = f.resolve(name);
				if (!Files.exists(img)) continue;
				try (InputStream in = Files.newInputStream(img)) {
					com.mojang.blaze3d.platform.NativeImage n = com.mojang.blaze3d.platform.NativeImage.read(in);
					pw = n.getWidth();
					ph = n.getHeight();
					pic = Retromon.id("dynamic/slot" + k);
					Minecraft.getInstance().getTextureManager().register(pic, new net.minecraft.client.renderer.texture.DynamicTexture(() -> "Retromon slot", n));
					break;
				} catch (Exception e) {
					Retromon.LOG.warn("Couldn't read slot {}'s picture", k + 1, e);
				}
			}
			slotCards[k] = new SlotInfo(f, info, pic, pw, ph, when);
		}
	}

	private boolean anySlot() {
		for (SlotInfo c : slotCards) if (c != null && !c.empty()) return true;
		return false;
	}

	private int firstEmpty() {
		for (int k = 0; k < SLOTS; k++) if (slotCards[k] == null || slotCards[k].empty()) return k;
		return -1;
	}

	/** Your Retromon saves (worlds with Retromon's marker), the last played first. */
	private static List<Path> findSlots() {
		List<Path> out = new ArrayList<>();
		Path saves = root().resolve("saves");
		if (!Files.isDirectory(saves)) return out;
		try (Stream<Path> s = Files.list(saves)) {
			s.filter(p -> Files.exists(p.resolve(Game.MARKER)) && Files.exists(p.resolve("level.dat"))).forEach(out::add);
		} catch (IOException e) {
			Retromon.LOG.warn("Couldn't list your saves", e);
		}
		out.sort(Comparator.comparing((Path p) -> {
			try {
				return Files.getLastModifiedTime(p.resolve("level.dat")).toMillis();
			} catch (IOException e) {
				return 0L;
			}
		}).reversed());
		return out;
	}

	private static final java.util.regex.Pattern PLAYER = java.util.regex.Pattern.compile("\"player\"\\s*:\\s*\"([^\"]*)\"");

	/** What a slot's marker says about it: the trainer's name (if it's there), and when it was last played. */
	private static String slotInfo(Path p) {
		try {
			String json = Files.readString(p.resolve(Game.MARKER), StandardCharsets.UTF_8);
			java.util.regex.Matcher m = PLAYER.matcher(json);
			String who = m.find() ? m.group(1) : "";
			String when = java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm").format(
					java.time.LocalDateTime.ofInstant(Files.getLastModifiedTime(p.resolve("level.dat")).toInstant(), java.time.ZoneId.systemDefault()));
			return (who.isEmpty() ? "" : who + "  ·  ") + when;
		} catch (IOException e) {
			return "";
		}
	}

	/** Showing Minecraft's own title screen (chosen from MINECRAFT); otherwise Retromon's is the start. */
	public static boolean minecraftTitle;

	/** From the opening to the title: a white flash, Charizard's roar, and the title music. */
	private void toTitle() {
		opened = true;
		step = Step.TITLE;
		// Ours comes up as the viewer's does (no flash, no cry); the old one with its flash and Charizard's roar.
		titleTicks = Look.ours() ? 99 : 0;
		titleSince = ticks;
		if (!Look.ours()) Sounds.cry("charizard", 1F);
		Music.play("title_screen");
	}

	/** When the title came up (ticks), for its logo rising in. */
	private int titleSince;

	/** The saves (the viewer's "Continue or new game"): your three slots as cards, OPTIONS under them. */
	private void openMenu() {
		readSlots();
		replacing = false;
		confirm = null;
		slotAt = 0;
		for (int k = 0; k < SLOTS; k++) if (slotCards[k] != null && !slotCards[k].empty()) {
			slotAt = k;
			break;
		}
		step = Step.SLOTS;
	}

	private void menuPick(int i) {
		menu.at = i;
		Sounds.press();
		switch (menu.selected()) {
			case "CONTINUE" -> {
				replacing = false;
				confirm = null;
				slotAt = 0;
				for (int k = 0; k < SLOTS; k++) if (!slotCards[k].empty()) {
					slotAt = k;
					break;
				}
				step = Step.SLOTS;
			}
			case "NEW GAME" -> {
				int free = firstEmpty();
				if (free >= 0) {
					targetSlot = free;
					replaceTarget = false;
					showKeys();
				} else {
					// All three full: which one to start over in.
					replacing = true;
					confirm = null;
					slotAt = 0;
					step = Step.SLOTS;
				}
			}
			case "OPTIONS" -> {
				if (minecraft != null) minecraft.setScreen(new net.minecraft.client.gui.screens.options.OptionsScreen(this, minecraft.options));
			}
			case "MINECRAFT" -> {
				// The usual title screen (other worlds, servers, mods); its Retromon button comes back here.
				minecraftTitle = true;
				if (minecraft != null) minecraft.setScreen(parent instanceof net.minecraft.client.gui.screens.TitleScreen ? parent
						: new net.minecraft.client.gui.screens.TitleScreen());
			}
			case "QUIT" -> {
				if (minecraft != null) minecraft.stop();
			}
			default -> onClose();
		}
	}

	// ---------- Professor Oak's welcome ----------

	private void say(List<String> lines, Runnable then) {
		say(lines, "oak", then);
	}

	private void say(List<String> lines, String who, Runnable then) {
		scene = who;
		talk = new TextBox(lines);
		talk.speaker = "PROF. OAK";
		afterTalk = then;
		step = Step.TALK;
	}

	private void startOak() {
		if (bundledMap() == null) {
			failure = "The map isn't installed. Put Retromon-Map-1.0.0.jar in your mods folder (beside Retromon), then restart.";
			step = Step.FAILED;
			return;
		}
		Music.play("meet_prof_oak");
		hopAt = -1;
		waveAt = -1;
		prof = new Scenes.ProfWalk();
		prof.mode = "intro";
		prof.talking = true;
		say(List.of("Ah, there you are! Welcome. I'm **OAK**. Most people round here call me the **POKéMON PROF**.",
				"This world is full of creatures we call **POKéMON**. You'll find them in the grass, the sea, the caves, even in town.",
				"Some people keep them as friends. Some raise them for battles. Me? I study them.",
				"But that's enough about me. First, tell me a little about yourself."), () -> {
					// He walks on to the end, turns to you, and it fades to black.
					prof.talking = false;
					prof.stopWalk();
					waitArrive = true;
				});
	}

	/** What do you look like? The maker: you (body, skin, eyes, hair), then your clothes. */
	private void startMaker() {
		makerStage = 1;
		side = sideGoal = 0;
		maker = null;
		pickLook(pickFace);
		talk = TextBox.still("What do you look like?");
		step = Step.MAKER;
	}

	/** The viewer's starting looks: a boy and a girl for every skin tone, lightest to darkest, eyes to suit. */
	private static final int[] TONE_ORDER = {8, 4, 9, 0, 1, 5, 10, 2, 6, 3, 7, 11}, LOOK_EYES = {4, 3, 2, 4, 1, 2, 1, 1, 1, 1, 1, 1};
	private static final int[] BOY_HAIR = {1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0}, BOY_HAIRC = {14, 15, 9, 4, 17, 9, 4, 17, 4, 4, 4, 4};
	private static final int[] GIRL_HAIR = {3, 2, 4, 2, 5, 3, 2, 4, 5, 3, 2, 5}, GIRL_HAIRC = {14, 15, 16, 9, 17, 15, 4, 9, 4, 17, 4, 4};
	private static final int PER_SIDE = 12;

	private static Outfit lookOf(int i) {
		boolean gl = i >= PER_SIDE;
		int k = i % PER_SIDE;
		Outfit o = Outfit.defaults(gl);
		o.v.put("skin", TONE_ORDER[k]);
		o.v.put("hair", gl ? GIRL_HAIR[k] : BOY_HAIR[k]);
		o.v.put("hairc", Math.min(Outfit.COLOURS.length - 1, gl ? GIRL_HAIRC[k] : BOY_HAIRC[k]));
		o.v.put("eyes", Math.min(Outfit.EYE_RGB.length - 1, LOOK_EYES[k]));
		return o;
	}

	private void pickLook(int i) {
		pickFace = i;
		girl = i >= PER_SIDE;
		look = lookOf(i).write();
	}

	/** On to making it your own: you glide aside, the glass panel comes in on the right. */
	private void makeItYours() {
		Sounds.press();
		makerStage = 2;
		sideGoal = 2.1F;
		talk = TextBox.still("Make it yours. Change anything you like.");
		maker = new Maker(Outfit.parse(look, girl), girl, new String[] {"you", "clothes"}, null, () -> {}, () -> {
			makerStage = 1;
			sideGoal = 0;
			maker = null;
			talk = TextBox.still("What do you look like?");
		}, () -> {
			girl = maker.girl;
			look = maker.outfit.write();
		});
		maker.px = 650;
		maker.py = 24;
		maker.pw = 600;
		maker.maxH = 528;
	}

	private void startNaming(boolean rival) {
		namingRival = rival;
		entry = null;
		// Oak's question stays up while you pick his grandson's name.
		if (rival) {
			talk = TextBox.still("Now, what was his name again...?");
			talk.speaker = "PROF. OAK";
			prof.talking = false;
		}
		List<String> opts = new ArrayList<>();
		opts.add("NEW NAME");
		if (rival) opts.addAll(List.of("BLUE", "GARY", "JOHN"));
		else if (girl) opts.addAll(List.of("LEAF", "LYRA", "KRIS"));
		else opts.addAll(List.of("RED", "KRIS", "ETHAN"));
		names = new Choice(opts);
		step = Step.NAME;
	}

	private void named(String name) {
		Sounds.press();
		name = name.trim().toUpperCase();
		if (name.isEmpty()) return;
		entry = null;
		if (!namingRival) {
			playerName = name;
			prof = new Scenes.ProfWalk();
			prof.mode = "rival";
			prof.talking = true;
			say(List.of("So you're **" + name + "**! A fine name.", "And this is my grandson. You two have been racing each other since you could walk.",
					"Now, what was his name again...?"), "rival", () -> startNaming(true));
		} else {
			rivalName = name;
			prof.talking = true;
			say(List.of("That's right! I remember now. His name is **" + name + "**!"), "rival", () -> {
				prof = new Scenes.ProfWalk();
				prof.mode = "you";
				prof.talking = true;
				prof.lookUp = 0.22F;
				say(List.of("**" + playerName + "**, your own adventure starts today.",
						"There's a whole region out there, full of POKéMON to meet. I'll be right next door at my lab.", "Off you go, then!"), "hero", () -> {
							prof.talking = false;
							readyEndAt = ticks;
							waveAt = -1;
						});
			});
		}
	}

	@Override
	public boolean charTyped(net.minecraft.client.input.CharacterEvent e) {
		if (step != Step.NAME || entry == null) return false;
		return entry.typed(e);
	}

	@Override
	protected boolean key(KeyEvent e) {
		switch (step) {
			case OPENING -> toTitle();
			case TITLE -> {
				if (e.key() == GLFW.GLFW_KEY_M) toMinecraft();
				else if (Ui.isA(e)) {
					Sounds.press();
					if (!Look.ours()) Sounds.cry("charizard", 1F);
					openMenu();
				} else if (Ui.isB(e)) onClose();
			}
			case OPTIONS -> {
				if (Ui.isB(e)) {
					Sounds.press();
					step = Step.SLOTS;
					slotAt = SLOTS;
				} else options.key(e);
			}
			case MENU -> {
				if (menu.key(e)) return true;
				if (Ui.isB(e)) step = Step.TITLE;
				else if (Ui.isA(e)) menuPick(menu.at);
			}
			case KEYS -> {
				if (keysChoice.key(e)) return true;
				if (Ui.isB(e)) keysPick(2);
				else if (Ui.isA(e)) keysPick(keysChoice.at);
			}
			case SLOTS -> {
				if (busy) return true;
				if (confirm != null) {
					if (confirm.key(e)) return true;
					if (Ui.isB(e)) {
						confirm = null;
						deleting = false;
					} else if (Ui.isA(e)) confirmed(confirm.at == 0);
					return true;
				}
				int n = SLOTS + 2;
				if (e.key() == GLFW.GLFW_KEY_DELETE || e.key() == GLFW.GLFW_KEY_X) {
					askDelete(slotAt);
					return true;
				}
				if (e.key() == GLFW.GLFW_KEY_M) {
					toMinecraft();
					return true;
				}
				if (Ui.isUp(e) || Ui.isLeft(e)) slotAt = Math.floorMod(slotAt - 1, n);
				else if (Ui.isDown(e) || Ui.isRight(e)) slotAt = Math.floorMod(slotAt + 1, n);
				else if (Ui.isB(e)) {
					step = Step.TITLE;
					titleSince = ticks - 40;
				} else if (Ui.isA(e)) pickSlot(slotAt);
			}
			case TALK -> {
				if (waveAt < 0 && readyEndAt < 0 && !waitArrive && Ui.isA(e) && talk.press()) afterTalk.run();
			}
			case MAKER -> {
				if (makerStage == 2) {
					if (Ui.isA(e)) {
						Sounds.press();
						girl = maker.girl;
						look = maker.outfit.write();
						startNaming(false);
						return true;
					}
					return maker.key(e);
				}
				// The faces: up and down a column, left and right between the boys and the girls.
				if (Ui.isUp(e) || Ui.isDown(e)) {
					int sideK = pickFace >= PER_SIDE ? PER_SIDE : 0, rowK = Math.floorMod(pickFace % PER_SIDE + (Ui.isDown(e) ? 1 : -1), PER_SIDE);
					pickLook(sideK + rowK);
					Sounds.select();
				} else if (Ui.isLeft(e) || Ui.isRight(e)) {
					pickLook((pickFace + PER_SIDE) % (PER_SIDE * 2));
					Sounds.select();
				} else if (Ui.isA(e)) makeItYours();
				else if (Ui.isB(e)) startOak();
			}
			case NAME -> {
				if (entry != null) return entry.key(e);
				if (names.key(e)) return true;
				if (Ui.isA(e)) pickName(names.at);
				else if (Ui.isB(e) && !namingRival) {
					startMaker();
					makeItYours();
				}
			}
			case FAILED -> {
				if (Ui.isA(e) || Ui.isB(e)) openMenu();
			}
			default -> {
			}
		}
		return true;
	}

	private void pickName(int i) {
		names.at = i;
		if (i == 0) {
			Sounds.press();
			entry = new NameEntry(namingRival ? "YOUR RIVAL'S NAME" : "YOUR NAME", this::named, () -> entry = null);
		} else named(names.selected());
	}

	/**
	 * A slot picked: CONTINUE plays its game (an empty one starts a new game there); after NEW GAME with all three
	 * full, it asks before starting over in it.
	 */
	private void pickSlot(int i) {
		if (busy) return;
		if (i == SLOTS + 1) {
			toMinecraft();
			return;
		}
		Sounds.press();
		if (i >= SLOTS) {
			// OPTIONS, under the slots.
			options.row = 0;
			step = Step.OPTIONS;
			return;
		}
		SlotInfo c = slotCards[i];
		if (!replacing && !c.empty()) {
			// The loading page names where this save is (not always PALLET TOWN).
			arrivingAt = c.str("place", "PALLET TOWN");
			load(c.folder().getFileName().toString());
			return;
		}
		if (c.empty()) {
			targetSlot = i;
			replaceTarget = false;
			showKeys();
			return;
		}
		slotAt = i;
		confirm = new Choice(List.of("YES, START OVER", "NO"));
		confirm.at = 1;
	}

	/** Asked before a slot's game is deleted (moved aside to retromon-old-saves). */
	private boolean deleting;

	private void askDelete(int i) {
		if (i < 0 || i >= SLOTS || slotCards[i] == null || slotCards[i].empty() || replacing) return;
		Sounds.press();
		slotAt = i;
		deleting = true;
		confirm = new Choice(List.of("YES, DELETE IT", "NO"));
		confirm.at = 1;
	}

	private void confirmed(boolean yes) {
		Sounds.press();
		if (!yes) {
			confirm = null;
			deleting = false;
			return;
		}
		confirm = null;
		if (deleting) {
			deleting = false;
			deleteSlot(slotAt);
			return;
		}
		targetSlot = slotAt;
		replaceTarget = true;
		showKeys();
	}

	// ---------- the keys (the viewer's "Keys check", before the first game) ----------

	private Choice keysChoice;

	private void showKeys() {
		keysChoice = new Choice(List.of("Use these keys", "Show the key map", "Back"));
		step = Step.KEYS;
	}

	private void keysPick(int i) {
		Sounds.press();
		if (i == 0) startOak();
		else if (i == 1) {
			if (minecraft != null) minecraft.setScreen(new KeyMapScreen(this));
		} else step = Step.SLOTS;
	}

	/** "These are your keys" (the viewer's Keys check): walking, talking, the menu (E), speeding up, going back; then go on or see them all. */
	private void drawKeys(GuiGraphics g) {
		Scenes.bg2(g, "keys");
		V.panel(g, 180, 100, 640, 470);
		V.title(g, Lang.t("These are your keys"), 220, V.capsTop(100 + 34 + 27, 36), 36, V.INK);
		String[][] rows = {{"W A S D", "Walk"}, {"SPACE", "Talk, look, choose"}, {"E", "Open the menu"}, {"TAB", "Speed up"}, {"Q X", "Back"}};
		float[] widths = {0, 120, 0, 64, 0};
		float y = 100 + 34 + 54 + 20;
		for (int r = 0; r < rows.length; r++) {
			float x = 220, cy = y + 16;
			if (widths[r] > 0) {
				V.rr(g, x, cy - 15, widths[r], 30, 15, V.INK);
				V.centred(g, rows[r][0], x + widths[r] / 2, V.capsTop(cy, 13), 13, 0xFFFFFFFF, true);
				x += widths[r] + 6;
			} else for (String k : rows[r][0].split(" ")) {
				V.ellipse(g, x + 15, cy, 15, 15, V.INK);
				V.centred(g, k, x + 15, V.capsTop(cy, 13), 13, 0xFFFFFFFF, true);
				x += 30 + 6;
			}
			V.text(g, Lang.t(rows[r][1]), 220 + 166 + 22, V.capsTop(cy, 22), 22, V.INK, false);
			y += 32 + 14;
		}
		List<String> note = V.wrap(Lang.t("Other mods' keys are turned off while you play, so nothing else opens on these (a map mod on M, say)."), 560, 16, false);
		for (int i = 0; i < note.size(); i++) V.text(g, note.get(i), 220, y + 6 + i * 22.4F, 16, V.MUTED, false);
		vchoice(g, keysChoice, 860, 300, 300, this::keysPick);
	}

	/** Deleting a slot (in the background), and a line about it after when it couldn't be. */
	private volatile boolean busy;
	private String notice;
	private int noticeAt;

	/** The slot's game moved aside (DELETE, after YES): off the main thread, the cards read again after. */
	private void deleteSlot(int k) {
		SlotInfo c = k >= 0 && k < SLOTS ? slotCards[k] : null;
		if (c == null || c.folder() == null) return;
		Path folder = c.folder();
		busy = true;
		notice = null;
		Thread t = new Thread(() -> {
			String why = null;
			try {
				moveAside(folder);
			} catch (Exception e) {
				Retromon.LOG.warn("Couldn't delete slot {} ({})", k + 1, folder, e);
				why = e.getMessage() == null ? e.toString() : e.getMessage();
			}
			String fail = why;
			Minecraft.getInstance().execute(() -> {
				busy = false;
				readSlots();
				if (slotCards[k] != null && !slotCards[k].empty()) {
					notice = Lang.t("Couldn't delete SLOT {0}: something still has it open. Try again in a moment.", k + 1)
							+ (fail == null ? "" : " (" + fail + ")");
					noticeAt = ticks;
				}
			});
		}, "Retromon delete");
		t.setDaemon(true);
		t.start();
	}

	/**
	 * The game in a slot, moved aside into .minecraft/retromon-old-saves (the newest three are kept there). Tried
	 * again for a few seconds when something's still letting go of it (a game just left, another mod's files); if
	 * it still can't be moved whole, it's copied there and cleared out instead, so the slot's free either way.
	 */
	private static void moveAside(Path folder) throws IOException {
		// Beside the map folder, not in it (the map folder is searched for a map when the map mod isn't there).
		Path old = root().resolve("retromon-old-saves");
		Files.createDirectories(old);
		String stamp = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HHmmss").format(java.time.LocalDateTime.now());
		Path to = old.resolve(folder.getFileName() + " " + stamp);
		IOException last = null;
		for (int t = 0; t < 12 && Files.exists(folder); t++) {
			try {
				Files.move(folder, to);
				last = null;
				break;
			} catch (IOException e) {
				last = e;
				try {
					Thread.sleep(250);
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					break;
				}
			}
		}
		if (Files.exists(folder)) {
			Retromon.LOG.warn("Couldn't move {} aside whole; copying it and clearing it out instead", folder, last);
			copyAside(folder, to);
			clearOut(folder);
		}
		try {
			// Dated now, so "the newest three" counts from when it was put aside.
			if (Files.exists(to)) Files.setLastModifiedTime(to, java.nio.file.attribute.FileTime.fromMillis(System.currentTimeMillis()));
		} catch (IOException ignored) {
			// Kept either way.
		}
		try {
			pruneOld(old);
		} catch (IOException e) {
			// The slot's free already; the oldest old game is left for now.
			Retromon.LOG.warn("Couldn't tidy retromon-old-saves", e);
		}
	}

	/** A copy of the slot's files kept aside (what can be read; a file still held open is skipped). */
	private static void copyAside(Path from, Path to) {
		try (Stream<Path> st = Files.walk(from)) {
			for (Path f : st.filter(Files::isRegularFile).toList()) {
				if (f.getFileName().toString().equals("session.lock")) continue;
				try {
					Path out = to.resolve(from.relativize(f).toString());
					Files.createDirectories(out.getParent());
					Files.copy(f, out, StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e) {
					Retromon.LOG.warn("Couldn't keep a copy of {}", f, e);
				}
			}
		} catch (IOException | RuntimeException e) {
			Retromon.LOG.warn("Couldn't copy {} aside", from, e);
		}
	}

	/** Everything that can be deleted in the folder, deleted; it's only a failure if the game itself is still there. */
	private static void clearOut(Path dir) throws IOException {
		IOException last = null;
		try (Stream<Path> st = Files.walk(dir)) {
			for (Path p : st.sorted(Comparator.reverseOrder()).toList()) {
				try {
					Files.deleteIfExists(p);
				} catch (IOException e) {
					last = e;
				}
			}
		}
		if (Files.exists(dir.resolve("level.dat")) || Files.exists(dir.resolve(Game.MARKER)))
			throw last != null ? last : new IOException("The save is still there");
	}

	/** Only the newest three old games are kept in retromon-old-saves. */
	private static void pruneOld(Path old) throws IOException {
		List<Path> kept = new ArrayList<>();
		try (Stream<Path> st = Files.list(old)) {
			st.filter(Files::isDirectory).forEach(kept::add);
		}
		kept.sort(Comparator.comparing((Path p) -> {
			try {
				return Files.getLastModifiedTime(p).toMillis();
			} catch (IOException e) {
				return 0L;
			}
		}).reversed());
		for (int i = 3; i < kept.size(); i++) deleteFolder(kept.get(i));
	}

	private static void deleteFolder(Path dir) throws IOException {
		try (Stream<Path> st = Files.walk(dir)) {
			for (Path p : st.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(p);
		}
	}

	@Override
	protected void clickAnywhere() {
		if (step == Step.OPENING) toTitle();
		else if (step == Step.TITLE) {
			if (!Look.ours()) Sounds.cry("charizard", 1F);
			openMenu();
		}
		// A click anywhere moves the words on, as SPACE does.
		else if (step == Step.TALK && waveAt < 0 && readyEndAt < 0 && !waitArrive && talk.press()) afterTalk.run();
		else if (step == Step.FAILED) openMenu();
	}

	@Override
	protected void back() {
		switch (step) {
			case TITLE -> {
				// Retromon is the start: there's nowhere to go back to.
			}
			case MENU, SLOTS -> step = Step.TITLE;
			case FAILED, KEYS -> openMenu();
			case OPTIONS -> step = Step.SLOTS;
			default -> {
			}
		}
	}

	@Override
	public boolean keyPressed(KeyEvent e) {
		// No world yet: Esc goes back to the menu (or out, from the title).
		if (e.key() == GLFW.GLFW_KEY_ESCAPE) {
			if (step == Step.TITLE) onClose();
			else if (step == Step.SLOTS && confirm == null) step = Step.TITLE;
			else if (step == Step.OPTIONS) step = Step.SLOTS;
			else if (step == Step.MAKER && makerStage == 2) maker.key(new KeyEvent(GLFW.GLFW_KEY_BACKSPACE, 0, 0));
			else if (step == Step.SLOTS && confirm != null) {
				confirm = null;
				deleting = false;
			} else if (step != Step.COPYING && readyEndAt < 0) {
				openMenu();
				Music.play("title_screen");
			}
			return true;
		}
		return super.keyPressed(e);
	}

	@Override
	public void onClose() {
		if (step == Step.COPYING) return;
		toMinecraft();
	}

	/**
	 * Out of Retromon to Minecraft's own title screen: your other worlds, servers and mods, played as normal with
	 * Retromon still installed. Its Retromon button (top left) comes back here. (Before, going back from the title
	 * only brought Retromon's title round again.)
	 */
	private void toMinecraft() {
		Sounds.press();
		Music.stop();
		minecraftTitle = true;
		if (minecraft != null) minecraft.setScreen(parent instanceof net.minecraft.client.gui.screens.TitleScreen ? parent
				: new net.minecraft.client.gui.screens.TitleScreen());
	}

	/** A little grass block (MINECRAFT's picture): green on top, earth under it. */
	private static void grassBlock(GuiGraphics g, float x, float y, float s) {
		V.rr(g, x, y, s, s, s * 0.22F, 0xFF8A5A36);
		V.rr(g, x + s * 0.2F, y + s * 0.55F, s * 0.14F, s * 0.14F, s * 0.05F, 0xFF6B4226);
		V.rr(g, x + s * 0.62F, y + s * 0.72F, s * 0.14F, s * 0.14F, s * 0.05F, 0xFF6B4226);
		V.rr(g, x, y, s, s * 0.42F, s * 0.2F, 0xFF62B84A);
		V.rr(g, x + s * 0.12F, y + s * 0.3F, s * 0.16F, s * 0.2F, s * 0.06F, 0xFF62B84A);
		V.rr(g, x + s * 0.55F, y + s * 0.3F, s * 0.2F, s * 0.17F, s * 0.06F, 0xFF62B84A);
	}

	/** MINECRAFT on the title, top right (M, or click it): Minecraft as normal, with your mods. */
	private void drawMinecraftKey(GuiGraphics g) {
		String label = Lang.t("MINECRAFT");
		float kw = V.trackedWidthIn(V.ARCHIVO, "M", 13, 4) + 24, lw = V.trackedWidthIn(V.ARCHIVO, label, 15, 6);
		float total = 26 + 12 + lw + 14 + kw, bx = 1260 - 14 - total - 12, cy = 46, bw = total + 24, bh = 46;
		boolean on = vover(bx, cy - bh / 2, bw, bh);
		V.pill(g, bx, cy - bh / 2, bw, bh, on ? 0x66FFFFFF : 0x40001414);
		float x = bx + 12;
		grassBlock(g, x, cy - 13, 26);
		V.trackedIn(g, V.ARCHIVO, label, x + 38 + 1, V.capsTop(cy, 15) + 2, 15, 0x80001414, 6);
		V.trackedIn(g, V.ARCHIVO, label, x + 38, V.capsTop(cy, 15), 15, 0xFFFFFFFF, 6);
		float kx = x + 38 + lw + 14;
		V.ring(g, kx, cy - 14, kw, 28, 8, 2, 0xD9FFFFFF);
		V.trackedIn(g, V.ARCHIVO, "M", kx + 12, V.capsTop(cy, 13), 13, 0xFFFFFFFF, 4);
		vhit(bx, cy - bh / 2, bw, bh, this::toMinecraft);
	}

	private void load(String folder) {
		pendingLoad = folder;
		pendingParent = parent;
	}

	/**
	 * The map: inside Retromon Map (Retromon's second jar, Retromon-Map-1.0.0.jar; the older retromon-kanto-map jar
	 * still works), or a world or zip in .minecraft/retromon (for map makers).
	 */
	private static Path bundledMap() {
		for (String id : new String[] {"retromon_map", "retromon_kanto"}) {
			Optional<Path> inJar = FabricLoader.getInstance().getModContainer(id).flatMap(c -> c.findPath("retromon/maps/kanto.zip"));
			if (inJar.isPresent()) return inJar.get();
		}
		Path dir = mapFolder();
		if (Files.isDirectory(dir)) {
			try (Stream<Path> s = Files.walk(dir, 4)) {
				Optional<Path> world = s.filter(p -> p.getFileName().toString().equals("level.dat")).map(Path::getParent).findFirst();
				if (world.isPresent()) return world.get();
			} catch (IOException e) {
				Retromon.LOG.warn("Couldn't look in {}", dir, e);
			}
			try (Stream<Path> s = Files.list(dir)) {
				Optional<Path> zip = s.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".zip")).findFirst();
				if (zip.isPresent()) return zip.get();
			} catch (IOException e) {
				Retromon.LOG.warn("Couldn't look in {}", dir, e);
			}
		}
		return null;
	}

	/** The next free slot name: Retromon, Retromon 2, Retromon 3... */
	private static String freeSlot() {
		Path saves = root().resolve("saves");
		if (!Files.exists(saves.resolve(Game.SAVE_NAME))) return Game.SAVE_NAME;
		for (int i = 2;; i++) if (!Files.exists(saves.resolve(Game.SAVE_NAME + " " + i))) return Game.SAVE_NAME + " " + i;
	}

	private void newGame() {
		arrivingAt = "PALLET TOWN";
		Path map = bundledMap();
		if (map == null) return;
		int k = targetSlot >= 0 ? targetSlot : firstEmpty();
		slotName = k >= 0 ? slotFolder(k) : freeSlot();
		boolean aside = replaceTarget;
		replaceTarget = false;
		CompoundTag profile = new CompoundTag();
		profile.putString("name", playerName);
		profile.putString("rival", rivalName);
		profile.putString("gender", girl ? "girl" : "boy");
		profile.putString("outfit", look);
		ClientState.pendingProfile = profile;
		step = Step.COPYING;
		progress = "Getting the map ready...";
		Thread t = new Thread(() -> {
			Path tmp = null;
			try {
				Path to = root().resolve("saves").resolve(slotName);
				// Starting over in a full slot: its old game is moved aside first (kept in old-saves).
				if (aside && Files.exists(to)) {
					progress = "Moving the old game aside...";
					moveAside(to);
				} else if (Files.exists(to)) {
					// An empty slot with something left in its folder (a delete that couldn't finish): cleared first.
					try {
						moveAside(to);
					} catch (IOException e) {
						Retromon.LOG.warn("Couldn't clear what was left in {}", to, e);
					}
				}
				Files.createDirectories(to);
				if (Files.isDirectory(map)) copyFolder(map, to);
				else {
					// A zip inside a jar can't be opened in place: copy it out first.
					Path zip = map;
					if (!map.getFileSystem().equals(java.nio.file.FileSystems.getDefault())) {
						progress = "Getting the map out of the Kanto map mod...";
						tmp = Files.createTempFile("retromon-kanto", ".zip");
						Files.copy(map, tmp, StandardCopyOption.REPLACE_EXISTING);
						zip = tmp;
					}
					unzip(zip, to);
				}
				prepare(to);
				copied = true;
			} catch (Exception e) {
				Retromon.LOG.error("Couldn't make the new game", e);
				failure = e.getMessage() == null ? e.toString() : e.getMessage();
				step = Step.FAILED;
			} finally {
				if (tmp != null) {
					try {
						Files.deleteIfExists(tmp);
					} catch (IOException ignored) {
						// A temporary file: the system clears it in time.
					}
				}
			}
		}, "Retromon new game");
		t.setDaemon(true);
		t.start();
	}

	private static void delete(Path dir) throws IOException {
		try (Stream<Path> s = Files.walk(dir)) {
			List<Path> all = s.sorted(Comparator.reverseOrder()).toList();
			for (Path p : all) Files.deleteIfExists(p);
		}
	}

	private void copyFolder(Path from, Path to) throws IOException {
		List<Path> files;
		try (Stream<Path> s = Files.walk(from)) {
			files = s.filter(Files::isRegularFile).toList();
		}
		long total = 0;
		for (Path f : files) total += Files.size(f);
		AtomicLong done = new AtomicLong();
		for (Path f : files) {
			Path rel = from.relativize(f);
			if (rel.getFileName().toString().equals("session.lock")) continue;
			Path out = to.resolve(rel.toString());
			Files.createDirectories(out.getParent());
			Files.copy(f, out, StandardCopyOption.REPLACE_EXISTING);
			long d = done.addAndGet(Files.size(f));
			progress = "Copying the map... " + (total == 0 ? 100 : d * 100 / total) + "%";
		}
	}

	private void unzip(Path zip, Path to) throws IOException {
		try (ZipFile z = new ZipFile(zip.toFile())) {
			// Where level.dat is inside the zip: that folder is the world.
			String base = null;
			for (ZipEntry e : java.util.Collections.list(z.entries())) {
				String n = e.getName().replace('\\', '/');
				if (n.endsWith("level.dat") && (n.equals("level.dat") || n.endsWith("/level.dat"))) {
					String b = n.substring(0, n.length() - "level.dat".length());
					if (base == null || b.length() < base.length()) base = b;
				}
			}
			if (base == null) throw new IOException("There's no level.dat in " + zip.getFileName());
			List<ZipEntry> entries = new ArrayList<>();
			long total = 0;
			for (ZipEntry e : java.util.Collections.list(z.entries())) {
				String n = e.getName().replace('\\', '/');
				if (!e.isDirectory() && n.startsWith(base)) {
					entries.add(e);
					total += Math.max(0, e.getSize());
				}
			}
			long done = 0;
			Path root = to.toAbsolutePath().normalize();
			for (ZipEntry e : entries) {
				String rel = e.getName().replace('\\', '/').substring(base.length());
				if (rel.isEmpty() || rel.endsWith("session.lock")) continue;
				Path out = root.resolve(rel).normalize();
				if (!out.startsWith(root)) continue;
				Files.createDirectories(out.getParent());
				try (InputStream in = z.getInputStream(e)) {
					Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
				}
				done += Math.max(0, e.getSize());
				progress = "Unpacking the map... " + (total == 0 ? 100 : done * 100 / total) + "%";
			}
		}
	}

	/**
	 * Makes the copy a Retromon save: its name, cheats on (for the /retromon commands), Adventure mode, and
	 * without the map maker's own player (so you start fresh, in Pallet Town). Marks it as ours.
	 */
	private void prepare(Path to) throws IOException {
		progress = "Setting up your save...";
		Path dat = to.resolve("level.dat");
		CompoundTag root = NbtIo.readCompressed(dat, NbtAccounter.unlimitedHeap());
		root.getCompound("Data").ifPresent(d -> {
			d.putString("LevelName", slotName);
			d.putBoolean("allowCommands", true);
			d.putInt("GameType", 2);
			d.remove("Player");
		});
		NbtIo.writeCompressed(root, dat);
		Files.deleteIfExists(to.resolve("level.dat_old"));
		Files.writeString(to.resolve(Game.MARKER), "{\"version\":3,\"player\":\"" + playerName.replace("\"", "") + "\"}", StandardCharsets.UTF_8);
	}

	// ---------- drawing (the UI Viewer's screens, on its 1280 by 720 canvas) ----------

	private static final String[] TIPS = {"Press TAB to speed the game up. Press it again to go back.", "Creatures 1 block tall or less can ride on your shoulder.",
			"Talk to people with SPACE. Some have things to give you.", "Your POKéMON follow you around. Talk to them too.", "In the bag, the KEY ITEMS pocket is its lid.",
			"Nights are darker outside. Change it under OPTIONS, NIGHTS."};
	private final String tip = TIPS[(int) (Math.random() * TIPS.length)];

	private Identifier you() {
		return Scenes.you(look, girl);
	}

	/** The mod's version (0.23.0), as the title shows it. */
	private static String version() {
		return FabricLoader.getInstance().getModContainer("retromon").map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("");
	}

	@Override
	protected void draw(GuiGraphics g, float partial) {
		float secs = (ticks + partial) / 20F;
		long now = System.nanoTime();
		float dt = lastNanos == 0 ? 0 : Math.min(0.1F, (now - lastNanos) / 1e9F);
		lastNanos = now;
		if (step == Step.OPENING) {
			drawOpening(g, openTicks + partial);
			return;
		}
		switch (step) {
			case TITLE, MENU -> drawTitle(g, secs);
			case SLOTS -> drawSlots(g, secs);
			case OPTIONS -> options.draw(g, this::vhit, mx, my);
			case KEYS -> drawKeys(g);
			case TALK -> {
				// His words set things going: on the second line he sets off, on the last he hurries to the end; at "Off
				// you go" you floss and he waves.
				if (prof.mode.equals("intro") && talk.message() >= 1 && !prof.walking) prof.startWalk();
				if (prof.mode.equals("intro") && talk.message() >= 3 && !prof.hurry) prof.stopWalk();
				if (prof.mode.equals("you") && talk.message() >= 2) {
					prof.floss = 1;
					prof.wave = 1;
				}
				prof.tick(dt);
				prof.draw(g, you());
				if (!talk.finished()) talk.renderFilm(g, font, ticks, partial, true);
				if (fadeAt >= 0) g.fill(0, 0, width, height, Ui.alpha(0xFF000000, Math.min(1, (ticks - fadeAt + partial) / 16F)));
			}
			case MAKER -> drawMaker(g, secs, dt, partial);
			case NAME -> drawNames(g, secs, partial, dt);
			case COPYING -> drawLoading(g, secs);
			case FAILED -> {
				Scenes.bg2(g, "loading");
				List<String> lines = V.wrap("The new game couldn't be made: " + failure, 740, 22, false);
				float h = 60 + lines.size() * 32;
				V.panel(g, 240, 360 - h / 2, 800, h);
				for (int i = 0; i < lines.size(); i++) V.text(g, lines.get(i), 270, 360 - h / 2 + 30 + i * 32, 22, V.INK, false);
				V.hints(g, "A", "Back");
			}
			default -> {
			}
		}
		// Into the old title from the opening: a white flash, fading.
		if (titleTicks < 14) g.fill(0, 0, width, height, Ui.alpha(0xFFFFFFFF, 1 - (titleTicks + partial) / 14F));
	}

	/** The opening (the viewer's): INSID3 TH3 CUB3 in spaced Fredoka over "presents", fading in on black and out again. */
	private void drawOpening(GuiGraphics g, float t) {
		g.fill(0, 0, width, height, 0xFF000000);
		float ms = t * 50, a = ms < 2300 ? Math.max(0, Math.min(1, (ms - 80) / 900F)) : Math.max(0, 1 - (ms - 2300) / 900F);
		if (a <= 0) return;
		float w = V.titleTrackedWidth("INSID3 TH3 CUB3", 64, 10);
		V.titleTracked(g, "INSID3 TH3 CUB3", 640 - w / 2, V.capsTop(337, 64), 64, Ui.alpha(0xFFF4F6FF, a), 10);
		String pr = Lang.t("presents");
		V.tracked(g, pr, 640 - V.trackedWidth(pr, 24, 6) / 2, V.capsTop(413, 24), 24, Ui.alpha(V.MUTED, a), 6);
	}

	/**
	 * The title: ours, the painted meadow with your partner (Title); Cobblemon's, the old POKéMON over Charizard at
	 * night with PRESS START.
	 */
	private void drawTitle(GuiGraphics g, float secs) {
		if (Look.ours()) {
			Title.draw(g, (ticks - titleSince) / 20F + (secs - ticks / 20F), "pikachu", version());
			drawMinecraftKey(g);
			return;
		}
		Scenes.title(g, secs, false);
		int ww = 670;
		Identifier mark = V.tex("wordmark_theirs");
		V.img(g, mark, (V.W - ww) / 2, 34 + 10, ww, 152, ww, 152, 0x2E000000);
		V.img(g, mark, (V.W - ww) / 2, 34, ww, 152, ww, 152, 0xFFFFFFFF);
		int foot = 0xCCDDE6FF;
		V.text(g, "INSID3 TH3 CUB3", 28, 680, 15, foot, true);
		V.right(g, "Version " + version(), 1260, 680, 15, foot, true);
		drawMinecraftKey(g);
		if ((secs % 1.1F) < 0.55F) {
			String ps = Lang.t("PRESS START");
			float tw = V.titleTrackedWidth(ps, 30, 6), pw = tw + 68;
			V.pill(g, 640 - pw / 2, 612, pw, 52, 0xFFFFFFFF);
			V.titleTracked(g, ps, 640 - tw / 2, V.capsTop(612 + 26, 30), 30, V.INK, 6);
		}
	}

	/** Your saves as cards (the viewer's): who, where, how far; an empty one for a new game; OPTIONS under them. */
	private final List<Runnable> deletes = new ArrayList<>();

	private void drawSlots(GuiGraphics g, float secs) {
		Scenes.bg2(g, "saves");
		Scenes.stand(g, secs, you(), -1.6, 0, true);
		float x = 70, y = 90, w = 640;
		for (int k = 0; k <= SLOTS; k++) {
			int idx = k;
			if (k == SLOTS) {
				// OPTIONS: a row with the cog, white when you're not on it.
				String ot = Lang.t("OPTIONS");
				float bw = 40 + 26 + 14 + V.width(ot, 21, false) + 22, by = y + 4;
				if (confirm == null && vover(x, by, bw, 46)) slotAt = SLOTS;
				boolean on = slotAt == SLOTS;
				if (on) {
					V.softShadow(g, x, by + 5, bw, 46, 14, 0x59E8961E);
					V.pill(g, x - 2.5F, by - 2.5F, bw + 5, 51, 0xFFFFFFFF);
					V.sel(g, x, by, bw, 46, 23);
					V.tri(g, x + 21, by + 23, 12, 0, V.RED);
				} else V.pill(g, x, by, bw, 46, 0xD9FFFFFF);
				V.icon(g, "icon_cog", x + 40, by + 10, 26);
				V.text(g, ot, x + 40 + 26 + 14, V.capsTop(by + 23, 21), 21, V.INK, false);
				if (confirm == null) vhit(x, by, bw, 46, () -> pickSlot(SLOTS));
				// MINECRAFT beside it: out to Minecraft's own title screen (your other worlds, with your mods).
				String mt = Lang.t("MINECRAFT");
				float mx0 = x + bw + 16, mw = 40 + 26 + 14 + V.width(mt, 21, false) + 22;
				if (confirm == null && vover(mx0, by, mw, 46)) slotAt = SLOTS + 1;
				boolean mon = slotAt == SLOTS + 1;
				if (mon) {
					V.softShadow(g, mx0, by + 5, mw, 46, 14, 0x59E8961E);
					V.pill(g, mx0 - 2.5F, by - 2.5F, mw + 5, 51, 0xFFFFFFFF);
					V.sel(g, mx0, by, mw, 46, 23);
					V.tri(g, mx0 + 21, by + 23, 12, 0, V.RED);
				} else V.pill(g, mx0, by, mw, 46, 0xD9FFFFFF);
				grassBlock(g, mx0 + 40, by + 10, 26);
				V.text(g, mt, mx0 + 40 + 26 + 14, V.capsTop(by + 23, 21), 21, V.INK, false);
				if (confirm == null) vhit(mx0, by, mw, 46, () -> pickSlot(SLOTS + 1));
				break;
			}
			SlotInfo c = slotCards[k];
			boolean used = c != null && !c.empty(), on = slotAt == k;
			float h = used ? 36 + 30 * 1.5F + 6 + 4 * 19 * 1.45F + 6 : 22 * 2 + 28 * 1.5F;
			if (confirm == null && vover(x, y, w, h)) slotAt = k;
			if (on) V.rr(g, x - 5, y - 5, w + 10, h + 10, 27, 0xFFFFC93C);
			V.panel(g, x, y, w, h);
			if (used) {
				// The picture: the save's own (as it was when you left it), or a little Pallet Town.
				float px = x + 3 + 22, py = y + 3 + 18, pw = 150, ph = h - 6 - 36;
				if (c.picture() != null) {
					float k1 = Math.max(pw / c.picW(), ph / c.picH());
					V.rr(g, px, py, pw, ph, 16, 0xFF9ED8F2);
					g.enableScissor(Math.round(V.x(px)), Math.round(V.y(py)), Math.round(V.x(px + pw)), Math.round(V.y(py + ph)));
					V.img(g, c.picture(), px + (pw - c.picW() * k1) / 2, py + (ph - c.picH() * k1) / 2, c.picW() * k1, c.picH() * k1, c.picW(), c.picH(), 0xFFFFFFFF);
					g.disableScissor();
				} else {
					V.rrStops(g, px, py, pw, ph, 16, new float[] {0, 0.55F, 0.56F, 1}, new int[] {0xFF9ED8F2, 0xFFBFEFE0, 0xFF6DB257, 0xFF5FA24C});
					V.rr(g, px + 18, py + ph - 22 - 40, 56, 40, 4, 0xFFF2E8D2);
					V.rr(g, px + 12, py + ph - 58 - 22, 68, 22, 6, 0xFFC9493F);
				}
				float ix = px + pw + 20, ty = py;
				String sl = Lang.t("SLOT {0}", k + 1);
				float tw = V.width(sl, 14, true) + 24;
				V.rrv(g, ix, ty + 45 / 2F - 11, tw, 22, 11, 0xFFFF9473, 0xFFE5525C);
				V.text(g, sl, ix + 12, V.capsTop(ty + 45 / 2F, 14), 14, 0xFFFFFFFF, true);
				V.title(g, c.str("name", "TRAINER"), ix + tw + 12, V.capsTop(ty + 45 / 2F, 30), 30, V.INK);
				String[][] rows = {{"Place", c.str("place", "KANTO")},
						{"Pokédex", c.info().has("dex") && c.info().get("dex").getAsBoolean() ? String.valueOf(c.num("caught")) : "None yet"},
						{"Badges", String.valueOf(c.num("badges"))}, {"Play time", c.num("played") / 3600 + ":" + String.format("%02d", c.num("played") / 60 % 60)}};
				float ry = ty + 45 + 6, lw = 0;
				for (String[] r : rows) lw = Math.max(lw, V.labelWidth(Lang.t(r[0])));
				for (int r = 0; r < rows.length; r++) {
					float rcy = ry + r * (19 * 1.45F + 2) + 19 * 1.45F / 2;
					V.label(g, Lang.t(rows[r][0]), ix, V.capsTop(rcy, 13));
					V.text(g, rows[r][1], ix + lw + 18, V.capsTop(rcy, 19), 19, V.INK, false);
				}
				float dw = V.width(Lang.t("DELETE"), 13, true) + 28, dx = x + w - 16 - dw, dy = y + 14;
				boolean overDel = confirm == null && vover(dx, dy, dw, 24);
				V.pill(g, dx, dy, dw, 24, overDel ? 0xFFE5525C : 0xFFF3A6AC);
				V.pill(g, dx + 2, dy + 2, dw - 4, 20, overDel ? 0xFFE5525C : 0xFFFFFFFF);
				V.text(g, Lang.t("DELETE"), dx + 14, V.capsTop(dy + 12, 13), 13, overDel ? 0xFFFFFFFF : 0xFFD9404A, true);
				// (Its click is added after the card's, so it wins over loading the game.)
				if (confirm == null) deletes.add(() -> vhit(dx, dy, dw, 24, () -> askDelete(idx)));
			} else {
				String sl = Lang.t("SLOT {0}", k + 1);
				float tw = V.width(sl, 14, true) + 24, cy = y + h / 2;
				V.pill(g, x + 3 + 24, cy - 11, tw, 22, 0xFFEEF2FB);
				V.text(g, sl, x + 3 + 24 + 12, V.capsTop(cy, 14), 14, V.MUTED, true);
				V.title(g, Lang.t("NEW GAME"), x + 3 + 24 + tw + 16, V.capsTop(cy, 28), 28, V.INK);
				String em = Lang.t("Empty");
				V.text(g, em, x + w - 3 - 24 - V.width(em, 17, false), V.capsTop(cy, 17), 17, V.MUTED, false);
			}
			if (confirm == null) vhit(x, y, w, h, () -> pickSlot(idx));
			for (Runnable r : deletes) r.run();
			deletes.clear();
			y += h + 18;
		}
		V.actsRight(g, 22, 28, -1, "B", "Back", "A", "Continue");
		String note = busy ? Lang.t("Deleting...") : notice != null && ticks - noticeAt < 160 ? notice : null;
		if (note != null) {
			List<String> nl = V.wrap(note, 560, 18, false);
			float nh = 20 + nl.size() * 24, ny = 700 - nh;
			V.rr(g, 70, ny, 600, nh, 18, busy ? 0xF2FFFFFF : 0xF2FFE9EA);
			for (int i = 0; i < nl.size(); i++) V.text(g, nl.get(i), 90, ny + 12 + i * 24, 18, busy ? V.INK : 0xFFC8323C, false);
		}
		if (confirm != null) {
			SlotInfo c = slotCards[slotAt];
			String q = "Delete SLOT " + (slotAt + 1) + "?";
			String more = c.str("name", "This") + "'s game (" + c.str("place", "KANTO") + ") is moved to retromon-old-saves (the last three are kept there).";
			g.fill(0, 0, width, height, 0x661E2854);
			List<String> lines = V.wrap(more, 580, 20, false);
			float h = 96 + lines.size() * 28;
			V.panel(g, 320, 200, 640, h);
			V.title(g, q, 350, V.capsTop(200 + 46, 28), 28, V.INK);
			for (int i = 0; i < lines.size(); i++) V.text(g, lines.get(i), 350, 272 + i * 28, 20, V.MUTED, false);
			vchoice(g, confirm, 440, 200 + h + 16, 400, i -> confirmed(i == 0));
		}
	}

	/**
	 * The maker (the viewer's): first a starting look, the boys down the left and the girls down the right, a face
	 * on a glossy card for every skin tone, four in view at a time; you stand in the middle on a pool of light.
	 * Continue, and you glide aside for the glass panel to make it yours.
	 */
	private void drawMaker(GuiGraphics g, float secs, float dt, float partial) {
		Scenes.bg2(g, "creator");
		side += (sideGoal - side) * Math.min(1, dt * 5);
		Scenes.stand(g, secs, you(), side, 0, true);
		if (makerStage == 1) drawFaces(g, secs, dt);
		else if (maker != null) maker.draw(g, this::vhit, secs);
		talk.renderV(g, font, 150, 26, 470, ticks, partial, false);
		V.actsRight(g, 22, 28, -1, "B", "Back", "A", "Continue");
	}

	private void drawFaces(GuiGraphics g, float secs, float dt) {
		float top = 34, colH = 516, cardW = 240, cardH = 104, gap = 18, padTop = 14;
		float content = padTop + PER_SIDE * cardH + (PER_SIDE - 1) * gap + 14;
		for (int col = 0; col < 2; col++) {
			float cx = col == 0 ? 40 : 950;
			// Keep the one you're on in view (the nearest edge), smoothly.
			if (pickFace / PER_SIDE == col) {
				float ct = padTop + (pickFace % PER_SIDE) * (cardH + gap), goal = faceScroll[col];
				if (ct - 14 < goal) goal = ct - 14;
				if (ct + cardH + 14 > goal + colH) goal = ct + cardH + 14 - colH;
				goal = Math.max(0, Math.min(content - colH, goal));
				faceScroll[col] += (goal - faceScroll[col]) * Math.min(1, dt * 9);
			}
			g.enableScissor(Math.round(V.x(cx - 30)), Math.round(V.y(top)), Math.round(V.x(cx + 290 + 30)), Math.round(V.y(top + colH)));
			for (int r = 0; r < PER_SIDE; r++) {
				int i = col * PER_SIDE + r;
				float x = cx + 18, y = top + padTop + r * (cardH + gap) - faceScroll[col];
				if (y + cardH < top - 30 || y > top + colH + 30) continue;
				boolean on = i == pickFace;
				g.pose().pushMatrix();
				if (on) {
					float mx0 = V.x(x + cardW / 2), my0 = V.y(y + cardH / 2);
					g.pose().translate(mx0, my0);
					g.pose().scale(1.06F, 1.06F);
					g.pose().translate(-mx0, -my0);
					V.rr(g, x - 22, y - 22, cardW + 44, cardH + 44, 46, 0x38FFFFFF);
					V.rr(g, x - 9, y - 9, cardW + 18, cardH + 18, 33, 0x8CFFFFFF);
				}
				V.shadow(g, x, y, cardW, cardH, 24, 0.8F);
				V.rr(g, x - 3, y - 3, cardW + 6, cardH + 6, 27, on ? 0xFFFFFFFF : 0xD9FFFFFF);
				V.rrh(g, x, y, cardW, cardH, 24, new float[] {0, 0.45F, 1}, new int[] {0xFFFFFFFF, 0xFFF2FBEA, 0xFFD9F3A8});
				// The face: the front of the head and its hat layer, from the skin the look makes.
				float fx = x + (cardW - 80) / 2, fy = y + (cardH - 80) / 2;
				V.rr(g, fx - 1, fy + 6, 82, 80, 14, 0x4D143C5A);
				V.rr(g, fx - 3, fy - 3, 86, 86, 15, 0xFFFFFFFF);
				Identifier skin = Outfit.skinFor("start" + i, lookOf(i));
				if (skin != null) {
					V.sub(g, skin, fx, fy, 80, 80, 8, 8, 8, 8, 64, 64);
					V.sub(g, skin, fx, fy, 80, 80, 40, 8, 8, 8, 64, 64);
				}
				V.rrv(g, x + 10, y + 6, cardW - 20, cardH * 0.36F, 20, 0xBFFFFFFF, 0x00FFFFFF);
				V.rr(g, x + cardW - 14 - 18 - 2, y + cardH - 10 - 18 - 2, 22, 22, 8, 0xFFFFFFFF);
				V.rr(g, x + cardW - 14 - 18, y + cardH - 10 - 18, 18, 18, 6, 0xFF000000 | Outfit.SKIN_RGB[TONE_ORDER[i % PER_SIDE]]);
				g.pose().popMatrix();
				if (vover(x, Math.max(top, y), cardW, Math.min(y + cardH, top + colH) - Math.max(top, y)) && pickFace != i) pickLook(i);
				int ii = i;
				vhit(x, Math.max(top, y), cardW, Math.max(1, Math.min(y + cardH, top + colH) - Math.max(top, y)), () -> {
					pickLook(ii);
					makeItYours();
				});
			}
			g.disableScissor();
			// The scroll bar: rounded, mint.
			float trackTop = top + 22, trackH = colH - 44, thumbH = Math.max(40, trackH * colH / content), thumbY = trackTop + (trackH - thumbH) * (faceScroll[col] / (content - colH));
			V.rrv(g, cx + 290 - 12 + 3, thumbY, 6, thumbH, 3, 0xFF9FF0D8, 0xFF41BF84);
		}
	}

	@Override
	public boolean mouseScrolled(double x, double y, double h, double v) {
		float vx = V.vx(x), vy = V.vy(y);
		if (step == Step.MAKER && makerStage == 2 && maker != null && maker.scrolled(vx, vy, v)) return true;
		if (step == Step.MAKER && makerStage == 1) {
			int col = vx < 640 ? 0 : 1;
			float content = 14 + PER_SIDE * 104 + (PER_SIDE - 1) * 18 + 14;
			faceScroll[col] = Math.max(0, Math.min(content - 516, faceScroll[col] - (float) v * 60));
			return true;
		}
		return super.mouseScrolled(x, y, h, v);
	}

	/** Your name (you on your stand, the ready names, or the letters), or your rival's (beside Oak, his line kept up). */
	private void drawNames(GuiGraphics g, float secs, float partial, float dt) {
		if (namingRival) {
			prof.tick(dt);
			prof.draw(g, you());
			if (entry == null && talk != null) talk.renderFilm(g, font, ticks, partial, false);
			if (entry == null) vchoice(g, names, 980, 140, 250, this::pickName);
		} else {
			Scenes.bg2(g, "name");
			Scenes.stand(g, secs, you(), 0, 0, true);
			V.titleCentred(g, Lang.t("And what's your name?"), 640, V.capsTop(70 + 22, 30), 30, V.INK);
			if (entry == null) vchoice(g, names, 980, 190, 250, this::pickName);
			V.actsRight(g, 22, 28, -1, "B", "Back", "A", "Continue");
		}
		if (entry != null) entry.draw(g, this::vhit, secs);
	}

	/** While the world's made and loads (the viewer's): a ball spinning, where you're going, how far along, and a tip. */
	private void drawLoading(GuiGraphics g, float secs) {
		drawLoadingPage(g, secs);
		float t = loadingAt < 0 ? 99 : ticks - loadingAt;
		if (t < 10) g.fill(0, 0, width, height, Ui.alpha(0xFF000000, 1 - t / 10F));
	}

	private void drawLoadingPage(GuiGraphics g, float secs) {
		Scenes.bg2(g, "loading");
		// Its middle at 44% down: the ball (110), 22, the place (40px), 22, the bar (18).
		float cx = 640, top = 720 * 0.44F - (110 + 22 + 60 + 22 + 18) / 2F;
		g.pose().pushMatrix();
		g.pose().translate(V.x(cx), V.y(top + 55));
		g.pose().rotate((float) (secs / 1.1F * Math.PI * 2));
		g.pose().translate(-V.x(cx), -V.y(top + 55));
		V.icon(g, "icon_ball", cx - 55, top, 110);
		g.pose().popMatrix();
		String place = arrivingPlace();
		float pw0 = V.titleTrackedWidth(place, 40, 4);
		V.titleTracked(g, place, cx - pw0 / 2, V.capsTop(top + 110 + 22 + 30, 40), 40, 0xFFFFFFFF, 4);
		float pct = 0;
		java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)%").matcher(progress);
		if (m.find()) pct = Integer.parseInt(m.group(1)) / 100F;
		else if (progress.startsWith("Setting")) pct = 1;
		float by = top + 110 + 22 + 60 + 22;
		V.pill(g, cx - 260, by, 520, 18, V.TRACK);
		if (pct > 0) V.rrh(g, cx - 258, by + 2, Math.max(14, 516 * pct), 14, 7, new float[] {0, 1}, new int[] {0xFF4FB896, 0xFF8FF5C0});
		V.centred(g, progress, cx, by + 30, 15, 0xCCFFFFFF, false);
		String tp = Lang.t(tip), tl = Lang.t("Tip");
		float tw = V.labelWidth(tl) + 12 + V.width(tp, 20, false), pw = tw + 60;
		V.panel(g, cx - pw / 2, V.H - 60 - 62, pw, 62, 31, true);
		V.label(g, tl, cx - pw / 2 + 30, V.capsTop(V.H - 60 - 31, 13));
		V.text(g, tp, cx - pw / 2 + 30 + V.labelWidth(tl) + 12, V.capsTop(V.H - 60 - 31, 20), 20, V.INK, false);
	}
}
