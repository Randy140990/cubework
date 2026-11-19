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
 * The game's start, as the handheld games had it. The title: the pixel Charizard against the night sky,
 * RETROMON, PRESS START. Then CONTINUE (your save slots) or NEW GAME: Professor Oak's welcome, boy or girl,
 * your name and your rival's, and then the Kanto map (from the Kanto map mod, Retromon's second jar) is
 * copied as a new save slot and you're off to Pallet Town.
 */
public class IntroScreen extends GameScreen {
	private static final Identifier ART = Retromon.id("textures/gui/intro.png");
	private static final int ART_W = 480, ART_H = 270;

	private enum Step {
		OPENING, TITLE, MENU, SLOTS, KEYS, TALK, MAKER, NAME, COPYING, FAILED
	}

	private final Screen parent;
	/** The opening (INSID3 TH3 CUB3 presents, fading in and out) plays once each time the game's started. */
	private static boolean opened;
	private volatile Step step = opened ? Step.TITLE : Step.OPENING;
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
	private String slotName;
	/** Who's on screen while Oak talks: oak, hero (you) or rival. */
	private String scene = "oak";
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
		mc.createWorldOpenFlows().openWorld(folder, () -> mc.setScreen(new IntroScreen(parent)));
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
			openTicks++;
			if (openTicks >= OPEN_END) toTitle();
		}
		if (titleTicks < 99) titleTicks++;
		if (talk != null) talk.tick();
		// "Off you go, then!": you wave, then the game loads.
		if (waveAt >= 0 && ticks - waveAt > 18 && step == Step.TALK) {
			waveAt = -1;
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
		titleTicks = 0;
		Sounds.cry("charizard", 1F);
		Music.play("title_screen");
	}

	private void openMenu() {
		readSlots();
		List<String> options = new ArrayList<>();
		if (anySlot()) options.add("CONTINUE");
		options.add("NEW GAME");
		options.add("OPTIONS");
		options.add("MINECRAFT");
		options.add("QUIT");
		menu = new Choice(options);
		step = Step.MENU;
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
			failure = "The Kanto map isn't installed. Put retromon-kanto-map.jar in your mods folder (beside Retromon), then restart.";
			step = Step.FAILED;
			return;
		}
		Music.play("meet_prof_oak");
		hopAt = -1;
		waveAt = -1;
		say(List.of("Ah, there you are! Welcome. I'm OAK. Most people round here call me the POKéMON PROF.",
				"This world is full of creatures we call POKéMON. You'll find them in the grass, the sea, the caves, even in town.",
				"Some people keep them as friends. Some raise them for battles. Me? I study them.",
				"But that's enough about me. First, tell me a little about yourself."), this::startMaker);
	}

	/** What do you look like? The maker: you (body, skin, eyes, hair), then your clothes. */
	private void startMaker() {
		Outfit o = Outfit.parse(look, girl);
		maker = new Maker(o, girl, new String[] {"you", "clothes"}, "THAT'S ME", () -> {
			Sounds.press();
			girl = maker.girl;
			look = maker.outfit.write();
			startNaming(false);
		}, this::startOak, () -> {
			girl = maker.girl;
			look = maker.outfit.write();
		});
		step = Step.MAKER;
	}

	private void startNaming(boolean rival) {
		namingRival = rival;
		entry = null;
		// Oak's question stays up while you pick his grandson's name.
		if (rival) {
			talk = TextBox.still("Now, what was his name again...?");
			talk.speaker = "PROF. OAK";
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
			say(List.of("So you're " + name + "! A fine name.", "And this is my grandson. You two have been racing each other since you could walk.",
					"Now, what was his name again...?"), "rival", () -> startNaming(true));
		} else {
			rivalName = name;
			say(List.of("That's right! I remember now. His name is " + name + "!"), "rival", () -> say(List.of(playerName + ", your own adventure starts today.",
					"There's a whole region out there, full of POKéMON to meet. I'll be right next door at my lab.", "Off you go, then!"), "hero", () -> {
						waveAt = ticks;
						Sounds.jingle("get_key_item");
					}));
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
				if (Ui.isA(e)) {
					// START: Charizard roars, as the title's Pokémon did.
					Sounds.press();
					Sounds.cry("charizard", 1F);
					openMenu();
				} else if (Ui.isB(e)) onClose();
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
				if (confirm != null) {
					if (confirm.key(e)) return true;
					if (Ui.isB(e)) {
						confirm = null;
						deleting = false;
					} else if (Ui.isA(e)) confirmed(confirm.at == 0);
					return true;
				}
				int n = SLOTS + 1;
				if (e.key() == GLFW.GLFW_KEY_DELETE || e.key() == GLFW.GLFW_KEY_X) {
					askDelete(slotAt);
					return true;
				}
				if (Ui.isUp(e) || Ui.isLeft(e)) slotAt = Math.floorMod(slotAt - 1, n);
				else if (Ui.isDown(e) || Ui.isRight(e)) slotAt = Math.floorMod(slotAt + 1, n);
				else if (Ui.isB(e)) step = Step.MENU;
				else if (Ui.isA(e)) pickSlot(slotAt);
			}
			case TALK -> {
				if (waveAt < 0 && (Ui.isA(e) || Ui.isB(e)) && talk.press()) afterTalk.run();
			}
			case MAKER -> maker.key(e);
			case NAME -> {
				if (entry != null) return entry.key(e);
				if (names.key(e)) return true;
				if (Ui.isA(e)) pickName(names.at);
				else if (Ui.isB(e) && !namingRival) startMaker();
			}
			case FAILED -> {
				if (Ui.isA(e) || Ui.isB(e)) step = Step.MENU;
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
		Sounds.press();
		if (i >= SLOTS) {
			step = Step.MENU;
			return;
		}
		SlotInfo c = slotCards[i];
		if (!replacing && !c.empty()) {
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
			SlotInfo c = slotCards[slotAt];
			try {
				if (c != null && c.folder() != null) moveAside(c.folder());
			} catch (IOException e) {
				Retromon.LOG.warn("Couldn't move slot {} aside", slotAt + 1, e);
			}
			readSlots();
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
		} else step = Step.MENU;
	}

	/** "These are your keys": walking, talking, the menu, speeding up and going back, then go on or see them all. */
	private void drawKeys(GuiGraphics g) {
		Scenes.backdrop(g, "sky");
		V.panel(g, 180, 110, 640, 420);
		V.text(g, "These are your keys", 220, 144, 36, V.TEXT, true);
		String[][] rows = {{"W A S D", "Walk"}, {"SPACE", "Talk, look, choose"}, {"ENTER", "Open the menu"}, {"TAB", "Speed up"}, {"Q X", "Back"}};
		int[] widths = {0, 120, 80, 64, 0};
		float y = 144 + 36 + 20 + 20;
		for (int r = 0; r < rows.length; r++) {
			float x = 220;
			if (widths[r] > 0) {
				float w = widths[r];
				V.rr(g, x, y + 2, w, 28, 9, 0x40000000);
				V.rr(g, x, y, w, 28, 9, V.CREAM);
				V.centred(g, rows[r][0], x + w / 2, y + (28 - 14 * 1.1F) / 2, 14, V.INK, true);
				x += w;
			} else for (String k : rows[r][0].split(" ")) x += V.key(g, k, x, y, 28) + 6;
			V.text(g, rows[r][1], 220 + 200, y + 1, 22, V.TEXT, true);
			y += 28 + 14 + 12;
		}
		vchoice(g, keysChoice, 860, 300, 300, this::keysPick);
	}

	/** The game in a slot, moved aside into .minecraft/retromon-old-saves (the newest three are kept there). */
	private static void moveAside(Path folder) throws IOException {
		// Beside the map folder, not in it (the map folder is searched for a map when the map mod isn't there).
		Path old = root().resolve("retromon-old-saves");
		Files.createDirectories(old);
		String stamp = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HHmmss").format(java.time.LocalDateTime.now());
		Files.move(folder, old.resolve(folder.getFileName() + " " + stamp));
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
			Sounds.cry("charizard", 1F);
			openMenu();
		}
		else if (step == Step.TALK && waveAt < 0 && talk.press()) afterTalk.run();
		else if (step == Step.FAILED) step = Step.MENU;
	}

	@Override
	protected void back() {
		switch (step) {
			case TITLE -> {
				// Retromon is the start: there's nowhere to go back to.
			}
			case MENU -> step = Step.TITLE;
			case SLOTS, FAILED, KEYS -> step = Step.MENU;
			case TALK -> clickAnywhere();
			default -> {
			}
		}
	}

	@Override
	public boolean keyPressed(KeyEvent e) {
		// No world yet: Esc goes back to the menu (or out, from the title).
		if (e.key() == GLFW.GLFW_KEY_ESCAPE) {
			if (step == Step.TITLE) onClose();
			else if (step != Step.COPYING) {
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
		Music.stop();
		if (minecraft != null) minecraft.setScreen(parent);
	}

	private void load(String folder) {
		pendingLoad = folder;
		pendingParent = parent;
	}

	/** The Kanto map: inside the Kanto map mod (Retromon's second jar), or a world or zip in .minecraft/retromon (for map makers). */
	private static Path bundledMap() {
		Optional<Path> inJar = FabricLoader.getInstance().getModContainer("retromon_kanto").flatMap(c -> c.findPath("retromon/maps/kanto.zip"));
		if (inJar.isPresent()) return inJar.get();
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

	private static final String[] TIPS = {"Press TAB to speed the game up. Press it again to go back.", "Talk to people with SPACE. Some have things to give you.",
			"Your POKéMON follow you around. Talk to them too.", "In the bag, the KEY ITEMS pocket is its lid.", "Nights are darker outside. Change it under OPTION, NIGHTS."};
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
		if (step == Step.OPENING) {
			drawOpening(g, openTicks + partial);
			return;
		}
		switch (step) {
			case TITLE, MENU -> drawTitle(g, secs);
			case SLOTS -> drawSlots(g, secs);
			case KEYS -> drawKeys(g);
			case TALK -> {
				String mode = scene.equals("rival") ? "rival" : scene.equals("hero") ? "you" : "intro";
				// "Some people keep them as friends": the little yellow one jumps up onto his shoulder.
				if (mode.equals("intro") && hopAt < 0 && talk.message() == 2) hopAt = ticks;
				float hop = hopAt < 0 ? -1 : (ticks - hopAt + partial) / 20F;
				float wave = waveAt < 0 ? 0 : Math.min(1, (ticks - waveAt + partial) / 6F);
				Scenes.prof(g, secs, mode, waveAt < 0, wave, mode.equals("intro") ? hop : -1, you());
				talk.renderV(g, font, 140, 26, 1000, ticks, partial, waveAt < 0);
			}
			case MAKER -> {
				Scenes.backdrop(g, "maker");
				Scenes.stand(g, secs, you(), -1.95, 0, true);
				V.centred(g, "What do you look like?", 760 + 240, 640, 24, 0xFF123C38, true);
				maker.draw(g, this::vhit, secs);
				V.hints(g, "A", "Choose", "B", "Back");
			}
			case NAME -> drawNames(g, secs, partial);
			case COPYING -> drawLoading(g, secs);
			case FAILED -> {
				Scenes.backdrop(g, "night");
				List<String> lines = V.wrap("The new game couldn't be made: " + failure, 740, 22, true);
				float h = 60 + lines.size() * 32;
				V.panel(g, 240, 360 - h / 2, 800, h);
				for (int i = 0; i < lines.size(); i++) V.text(g, lines.get(i), 270, 360 - h / 2 + 30 + i * 32, 22, V.TEXT, true);
				V.hints(g, "A", "Back");
			}
			default -> {
			}
		}
		// Into the title from the opening: a white flash, fading.
		if (titleTicks < 14) g.fill(0, 0, width, height, Ui.alpha(0xFFFFFFFF, 1 - (titleTicks + partial) / 14F));
	}

	/** The opening: INSID3 TH3 CUB3 presents, fading in on black and out again, then a white flash into the title. */
	private void drawOpening(GuiGraphics g, float t) {
		g.fill(0, 0, width, height, 0xFF000000);
		float a = Math.min(1, Math.max(0, (t - 2) / 18F)) * Math.min(1, Math.max(0, (OPEN_END - 14 - t) / 18F));
		V.centred(g, "INSID3 TH3 CUB3", 640, 300, 64, Ui.alpha(0xFFF4F6FF, a), true);
		V.centred(g, "presents", 640, 388, 24, Ui.alpha(0xFFA9D4C6, a), true);
		if (t > OPEN_END - 8) g.fill(0, 0, width, height, Ui.alpha(0xFFFFFFFF, Math.min(1, (t - (OPEN_END - 8)) / 8F)));
	}

	/**
	 * The title: RETROMON in blocks over the blocky hill and our creatures (the Retromon look), or POKéMON over
	 * Charizard at night (the Cobblemon look); PRESS START; then the menu over it.
	 */
	private void drawTitle(GuiGraphics g, float secs) {
		boolean ours = Look.ours();
		Scenes.title(g, secs, ours);
		int ww = ours ? 838 : 670;
		Identifier mark = V.tex(ours ? "wordmark_ours" : "wordmark_theirs");
		V.img(g, mark, (V.W - ww) / 2, 34 + 10, ww, 152, ww, 152, 0x2E000000);
		V.img(g, mark, (V.W - ww) / 2, 34, ww, 152, ww, 152, 0xFFFFFFFF);
		int foot = ours ? 0xCC123C38 : 0xCCDDE6FF;
		V.text(g, "INSID3 TH3 CUB3", 28, 680, 15, foot, true);
		V.right(g, "Version " + version(), 1260, 680, 15, foot, true);
		if (step == Step.TITLE) {
			if ((secs % 1.1F) < 0.55F) {
				float tw = V.width("PRESS START", 30, true) + 6 * 10, pw = tw + 68;
				V.pill(g, 640 - pw / 2, 612 + 6, pw, 52, 0x2E000000);
				V.pill(g, 640 - pw / 2, 612, pw, 52, V.CREAM);
				V.centred(g, "P R E S S   S T A R T", 640, 624, 30, V.INK, true);
			}
			return;
		}
		// The menu: the viewer's list, under the word.
		float w = 360, x = 640 - w / 2, y = 250;
		vchoice(g, menu, x, y, w, this::menuPick);
		V.hints(g, "A", "Choose", "B", "Back");
	}

	/** Your saves as cards (the viewer's): who, where, how far; pick one to carry on, an empty one for a new game. */
	private void drawSlots(GuiGraphics g, float secs) {
		Scenes.backdrop(g, "sky");
		Scenes.stand(g, secs, you(), -1.6, 0, true);
		float x = 70, y = 90 - (replacing ? 0 : 0), w = 640;
		if (replacing) V.text(g, "All three slots are full: choose a game to start over in.", x, 40, 20, 0xFF123C38, true);
		for (int k = 0; k <= SLOTS; k++) {
			int idx = k;
			if (k == SLOTS) {
				// BACK, as the viewer's OPTIONS row sits.
				float bw = V.width("BACK", 21, true) + 62;
				if (confirm == null && vover(x, y + 4, bw, 46)) slotAt = SLOTS;
				V.item(g, "BACK", null, x, y + 4, bw, 46, slotAt == SLOTS, false, 21);
				if (confirm == null) vhit(x, y + 4, bw, 46, () -> pickSlot(SLOTS));
				break;
			}
			SlotInfo c = slotCards[k];
			boolean used = c != null && !c.empty(), on = slotAt == k;
			float h = used ? 160 : 76;
			if (confirm == null && vover(x, y, w, h)) slotAt = k;
			if (on) V.panelOn(g, x, y, w, h, 24);
			else V.panel(g, x, y, w, h);
			if (used) {
				// The picture: the save's own (as it was when you left it), or a little Pallet Town.
				float px = x + 22, py = y + 18, pw = 150, ph = h - 36;
				if (c.picture() != null) {
					float k1 = Math.max(pw / c.picW(), ph / c.picH());
					V.rr(g, px, py, pw, ph, 16, 0xFF1E4A40);
					g.enableScissor(Math.round(V.x(px)), Math.round(V.y(py)), Math.round(V.x(px + pw)), Math.round(V.y(py + ph)));
					V.img(g, c.picture(), px + (pw - c.picW() * k1) / 2, py + (ph - c.picH() * k1) / 2, c.picW() * k1, c.picH() * k1, c.picW(), c.picH(), 0xFFFFFFFF);
					g.disableScissor();
				} else {
					V.rrv(g, px, py, pw, ph * 0.55F, 16, 0xFF9ED8F2, 0xFFBFEFE0);
					V.rrv(g, px, py + ph * 0.55F, pw, ph * 0.45F, 16, 0xFF6DB257, 0xFF5FA24C);
					V.rr(g, px + 18, py + ph - 62, 56, 40, 4, 0xFFF2E8D2);
					V.rr(g, px + 12, py + ph - 80, 68, 22, 6, 0xFFC9493F);
				}
				float ix = px + pw + 20;
				float tw = V.width("SLOT " + (k + 1), 14, true) + 24;
				V.pill(g, ix, y + 22, tw, 24, V.CREAM);
				V.text(g, "SLOT " + (k + 1), ix + 12, y + 26, 14, V.INK, true);
				V.text(g, c.str("name", "TRAINER"), ix + tw + 12, y + 16, 30, V.TEXT, true);
				String[][] rows = {{"Place", c.str("place", "KANTO")},
						{"Pokédex", c.info().has("dex") && c.info().get("dex").getAsBoolean() ? String.valueOf(c.num("caught")) : "None yet"},
						{"Badges", String.valueOf(c.num("badges"))}, {"Play time", c.num("played") / 3600 + ":" + String.format("%02d", c.num("played") / 60 % 60)}};
				for (int r = 0; r < rows.length; r++) {
					V.label(g, rows[r][0], ix, y + 60 + r * 23 + 4);
					V.text(g, rows[r][1], ix + 110, y + 60 + r * 23, 19, V.TEXT, true);
				}
				if (!replacing) {
					float dw = V.width("DELETE", 13, true) + 24, dx = x + w - 16 - dw, dy = y + 14;
					boolean overDel = confirm == null && vover(dx, dy, dw, 24);
					if (overDel) V.pill(g, dx, dy, dw, 24, V.RED);
					V.ring(g, dx, dy, dw, 24, 12, 2, 0xB3E5525C);
					V.text(g, "DELETE", dx + 12, dy + 5, 13, overDel ? 0xFFFFFFFF : 0xFFFFB3B8, true);
					if (confirm == null) vhit(dx, dy, dw, 24, () -> askDelete(idx));
				}
			} else {
				float tw = V.width("SLOT " + (k + 1), 14, true) + 24;
				V.pill(g, x + 24, y + 26, tw, 24, 0x33FFF4D8);
				V.text(g, "SLOT " + (k + 1), x + 36, y + 30, 14, V.TEXT, true);
				V.text(g, "NEW GAME", x + 24 + tw + 16, y + 22, 28, V.TEXT, true);
				V.right(g, "Empty", x + w - 24, y + 30, 17, V.MUTED, true);
			}
			if (confirm == null) vhit(x, y, w, h, () -> pickSlot(idx));
			y += h + 18;
		}
		V.hints(g, "A", "Choose", "B", "Back");
		if (confirm != null) {
			SlotInfo c = slotCards[slotAt];
			String q = deleting ? "Delete SLOT " + (slotAt + 1) + "?" : "Start over in SLOT " + (slotAt + 1) + "?";
			String more = c.str("name", "This") + "'s game (" + c.str("place", "KANTO") + ") is moved to retromon-old-saves (the last three are kept there).";
			g.fill(0, 0, width, height, 0x90000000);
			List<String> lines = V.wrap(more, 620, 20, true);
			float h = 90 + lines.size() * 28;
			V.panel(g, 320, 200, 640, h);
			V.text(g, q, 350, 226, 28, V.TEXT, true);
			for (int i = 0; i < lines.size(); i++) V.text(g, lines.get(i), 350, 270 + i * 28, 20, V.SOFT_TEXT, true);
			vchoice(g, confirm, 440, 200 + h + 16, 400, i -> confirmed(i == 0));
		}
	}

	/** Your name (you on your stand, the ready names, or the letters), or your rival's (beside Oak, his line kept up). */
	private void drawNames(GuiGraphics g, float secs, float partial) {
		if (namingRival) {
			Scenes.prof(g, secs, "rival", false, 0, -1, you());
			if (entry == null && talk != null) talk.renderV(g, font, 140, 26, 1000, ticks, partial, false);
			if (entry == null) vchoice(g, names, 980, 140, 250, this::pickName);
		} else {
			Scenes.backdrop(g, "name");
			Scenes.stand(g, secs, you(), 0, 0, true);
			V.centred(g, "And what's your name?", 640, 70, 30, 0xFF123C38, true);
			if (entry == null) vchoice(g, names, 980, 190, 250, this::pickName);
		}
		if (entry != null) entry.draw(g, this::vhit, secs);
		V.hints(g, "A", "Choose", "B", "Back");
	}

	/** While the world's made and loads: a ball spinning, where you're going, how far along, and a tip. */
	private void drawLoading(GuiGraphics g, float secs) {
		Scenes.backdrop(g, "night");
		float cx = 640, cy = 317;
		g.pose().pushMatrix();
		g.pose().translate(V.x(cx), V.y(cy - 70));
		g.pose().rotate((float) (secs / 1.1F * Math.PI * 2));
		g.pose().translate(-V.x(cx), -V.y(cy - 70));
		V.icon(g, "icon_ball", cx - 55, cy - 125, 110);
		g.pose().popMatrix();
		V.centred(g, "PALLET TOWN", cx, cy + 6, 40, V.TEXT, true);
		float pct = 0;
		java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)%").matcher(progress);
		if (m.find()) pct = Integer.parseInt(m.group(1)) / 100F;
		else if (progress.startsWith("Setting")) pct = 1;
		V.pill(g, cx - 260, cy + 70, 520, 18, 0xFF0B2A2C);
		if (pct > 0) V.rrv(g, cx - 258, cy + 72, Math.max(14, 516 * pct), 14, 7, 0xFF4FB896, 0xFF8FF5C0);
		V.centred(g, progress, cx, cy + 100, 15, V.MUTED, true);
		float tw = V.labelWidth("TIP") + 12 + V.width(tip, 20, true), pw = tw + 60;
		V.panel(g, cx - pw / 2, V.H - 60 - 58, pw, 58, 24, true);
		V.label(g, "TIP", cx - pw / 2 + 30, V.H - 60 - 58 + 22);
		V.text(g, tip, cx - pw / 2 + 30 + V.labelWidth("TIP") + 12, V.H - 60 - 58 + 17, 20, V.TEXT, true);
	}
}
