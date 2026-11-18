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
		OPENING, TITLE, MENU, SLOTS, TALK, GENDER, NAME, COPYING, FAILED
	}

	private final Screen parent;
	/** The opening (a shooting star across the night, as the handheld games' logo had) plays once each time the game's started. */
	private static boolean opened;
	private volatile Step step = opened ? Step.TITLE : Step.OPENING;
	/** Ticks into the opening, and since the title came up (its white flash, Charizard's roar). */
	private int openTicks, titleTicks = 99;
	private static final int OPEN_END = 84;
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
	private int genderAt;
	/** Naming: whose name (you, then your rival), the choices, the one typed. */
	private boolean namingRival;
	private Choice names;
	private boolean typing;
	private final StringBuilder typed = new StringBuilder();
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
			if (openTicks == 8) Sounds.sfx("star", 1F, 0.9F);
			if (openTicks >= OPEN_END) toTitle();
		}
		if (titleTicks < 99) titleTicks++;
		if (talk != null) talk.tick();
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
					startOak();
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
		girl = false;
		genderAt = 0;
		say(List.of("Hello there! Welcome to the world of POKéMON!", "My name is OAK! People call me the POKéMON PROF!",
				"This world is inhabited by creatures called POKéMON!",
				"For some people, POKéMON are pets. Others use them for fights. Myself... I study POKéMON as a profession.",
				"Now tell me. Are you a boy? Or are you a girl?"), () -> {
					step = Step.GENDER;
					lastGender = genderAt;
					genderSince = ticks - 20;
				});
	}

	private void pickGender(int i) {
		Sounds.press();
		girl = i == 1;
		say(List.of("First, what is your name?"), () -> startNaming(false));
	}

	private void startNaming(boolean rival) {
		namingRival = rival;
		typing = false;
		typed.setLength(0);
		List<String> opts = new ArrayList<>();
		opts.add("NEW NAME");
		if (rival) opts.addAll(List.of("BLUE", "GARY", "JOHN"));
		else if (girl) opts.addAll(List.of("LEAF", "KRIS", "LYRA"));
		else opts.addAll(List.of("RED", "ASH", "JACK"));
		names = new Choice(opts);
		step = Step.NAME;
	}

	private void named(String name) {
		Sounds.press();
		name = name.trim().toUpperCase();
		if (name.isEmpty()) return;
		if (!namingRival) {
			playerName = name;
			say(List.of("Right! So your name is " + name + "!"), "hero", () -> say(List.of("This is my grandson. He's been your rival since you were a baby.",
					"...Erm, what is his name again?"), "rival", () -> startNaming(true)));
		} else {
			rivalName = name;
			say(List.of("That's right! I remember now! His name is " + name + "!"), "rival", () -> say(List.of(playerName + "!",
					"Your very own POKéMON legend is about to unfold!", "A world of dreams and adventures with POKéMON awaits! Let's go!"), "hero", this::newGame));
		}
	}

	@Override
	public boolean charTyped(net.minecraft.client.input.CharacterEvent e) {
		if (step != Step.NAME || !typing) return false;
		String c = e.codepointAsString();
		if (typed.length() < 10 && c.length() == 1 && (Character.isLetterOrDigit(c.charAt(0)) || " .'-".indexOf(c.charAt(0)) >= 0)) {
			typed.append(Character.toUpperCase(c.charAt(0)));
			Sounds.press();
		}
		return true;
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
			case SLOTS -> {
				if (confirm != null) {
					if (confirm.key(e)) return true;
					if (Ui.isB(e)) confirm = null;
					else if (Ui.isA(e)) confirmed(confirm.at == 0);
					return true;
				}
				int n = SLOTS + 1;
				if (Ui.isUp(e) || Ui.isLeft(e)) slotAt = Math.floorMod(slotAt - 1, n);
				else if (Ui.isDown(e) || Ui.isRight(e)) slotAt = Math.floorMod(slotAt + 1, n);
				else if (Ui.isB(e)) step = Step.MENU;
				else if (Ui.isA(e)) pickSlot(slotAt);
			}
			case TALK -> {
				if ((Ui.isA(e) || Ui.isB(e)) && talk.press()) afterTalk.run();
			}
			case GENDER -> {
				if (Ui.isLeft(e) || Ui.isRight(e) || Ui.isUp(e) || Ui.isDown(e)) genderAt ^= 1;
				else if (Ui.isA(e)) pickGender(genderAt);
			}
			case NAME -> {
				if (typing) {
					if (e.key() == GLFW.GLFW_KEY_ENTER || e.key() == GLFW.GLFW_KEY_KP_ENTER) named(typed.toString());
					else if (e.key() == GLFW.GLFW_KEY_BACKSPACE) {
						if (typed.length() > 0) typed.setLength(typed.length() - 1);
						else typing = false;
					}
					return true;
				}
				if (names.key(e)) return true;
				if (Ui.isA(e)) pickName(names.at);
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
			typing = true;
			typed.setLength(0);
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
			startOak();
			return;
		}
		slotAt = i;
		confirm = new Choice(List.of("YES, START OVER", "NO"));
		confirm.at = 1;
	}

	private void confirmed(boolean yes) {
		Sounds.press();
		if (!yes) {
			confirm = null;
			return;
		}
		confirm = null;
		targetSlot = slotAt;
		replaceTarget = true;
		startOak();
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
		else if (step == Step.TALK && talk.press()) afterTalk.run();
		else if (step == Step.FAILED) step = Step.MENU;
	}

	@Override
	protected void back() {
		switch (step) {
			case TITLE -> {
				// Retromon is the start: there's nowhere to go back to.
			}
			case MENU -> step = Step.TITLE;
			case SLOTS, FAILED -> step = Step.MENU;
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

	// ---------- drawing ----------

	private static final Identifier OAK = Retromon.id("textures/entity/npc/oak.png");
	private static final Identifier RIVAL = Retromon.id("textures/entity/npc/blue.png");
	/** When the picked card last changed (for the wave and the bubble popping up). */
	private int genderSince, lastGender = -1, genderEmote;

	private Identifier hero() {
		return Retromon.id("textures/entity/player/" + (girl ? "girl" : "boy") + ".png");
	}

	@Override
	protected void draw(GuiGraphics g, float partial) {
		if (step == Step.OPENING) {
			drawOpening(g, openTicks + partial);
			return;
		}
		g.fill(0, 0, width, height, 0xFF0A0E24);
		// The picture, filling the screen, crisp (dimmed once Oak's talking).
		float k = Math.max(width / (float) ART_W, height / (float) ART_H);
		int w = Math.round(ART_W * k), h = Math.round(ART_H * k);
		g.blit(RenderPipelines.GUI_TEXTURED, ART, (width - w) / 2, (height - h) / 2, 0, 0, w, h, ART_W, ART_H, ART_W, ART_H);
		boolean story = step == Step.TALK || step == Step.GENDER || step == Step.NAME || step == Step.COPYING;
		if (story) {
			// Oak's lab at night: deep blue, a soft light low down where people stand.
			g.fillGradient(0, 0, width, height, 0xE00A0E24, 0xF0161D3A);
			Ui.glow(g, width / 2, height * 3 / 4, width / 2, height / 4, 0x08B8C8FF);
			Ui.cinema(g, width, height, 1);
		} else {
			int big = Math.max(2, s * 3);
			int tw = Ui.boldWidth(font, "RETROMON", big);
			Ui.draw(g, font, Ui.bold("RETROMON"), (width - tw) / 2, height / 7, Ui.GOLD, big, true);
			// The name's mark: a little twinkling star over the last O (as Pokémon has its accent).
			int ox = (width - tw) / 2 + Ui.boldWidth(font, "RETROM", big) + Ui.boldWidth(font, "O", big) / 2;
			float tw2 = 0.8F + 0.2F * (float) Math.sin((ticks + partial) * 0.25);
			int ss = Math.round(9 * big * tw2);
			Ui.icon(g, "emote/star", ox - ss / 2, height / 7 - ss + big, ss, 0xFFFFFFFF);
			Ui.centred(g, font, "KANTO & JOHTO", width / 2, height / 7 + 12 * big, 0xFFE8ECFF, s, true);
		}
		int m = 8 * s;
		float secs = (ticks + partial) / 20F;
		switch (step) {
			case TITLE -> {
				if ((ticks / 10) % 2 == 0) {
					int ps = Math.max(1, s * 2), pw = Ui.boldWidth(font, "PRESS START", ps);
					Ui.draw(g, font, Ui.bold("PRESS START"), (width - pw) / 2, height * 3 / 4, 0xFFFFFFFF, ps, true);
				}
				Ui.centred(g, font, "Space, Enter or click", width / 2, height * 3 / 4 + 22 * s, 0xFFB8C0E0, s, true);
			}
			case MENU -> {
				int mw = Math.max(menu.width(font, s), 110 * s);
				choice(g, menu, (width - mw) / 2, height * 3 / 5, mw, this::menuPick);
			}
			case SLOTS -> drawSlots(g);
			case TALK -> {
				int bh = boxHeight(), by = height - bh - height / 9;
				drawSpeaker(g, by, secs);
				talk.render(g, font, m * 2, by, width - 4 * m, bh, s, ticks, true);
			}
			case GENDER -> drawGender(g, secs, partial);
			case NAME -> drawNames(g, secs);
			case COPYING -> {
				int tall = (int) (height * 0.42F), feet = height / 9 + 14 * s + tall;
				Figures.Batch b = Figures.batch(0, 0, width, height);
				shadow(g, width / 2, feet, tall);
				// Off you go: waving goodbye, a cheer, a ta-da, by turns.
				int[] bye = {Figures.WAVE_BOTH, Figures.CHEER, Figures.WAVE, Figures.TADA};
				int at = (int) (secs / 2.2F) % bye.length;
				b.person(hero(), width / 2F, feet, tall).emote(bye[at], secs - (int) (secs / 2.2F) * 2.2F).at(secs).facing(8).pitch(-3);
				b.draw(g);
				panel(g, List.of(progress));
			}
			case FAILED -> panel(g, Ui.wrap(font, "The new game couldn't be made: " + failure, 240), true);
		}
		if (!story) Ui.centred(g, font, "A fan game by Insid3 Th3 Cub3.  Pokemon models from Cobblemon (CC BY-NC 3.0).", width / 2, height - 12, 0xFF9098C0, 1, true);
		// Into the title from the opening: a white flash, fading.
		if (titleTicks < 14) g.fill(0, 0, width, height, Ui.alpha(0xFFFFFFFF, 1 - (titleTicks + partial) / 14F));
	}

	/**
	 * The opening: the night sky, stars twinkling; a shooting star streaks across with a trail of sparkles and
	 * lands with a burst; INSID3 TH3 CUB3 fades in where it fell (as the handheld games' logo had its star);
	 * then a white flash into the title. Any key or a click skips it.
	 */
	private void drawOpening(GuiGraphics g, float t) {
		g.fill(0, 0, width, height, 0xFF04050C);
		java.util.Random r = new java.util.Random(11);
		int px = Math.max(1, s);
		for (int i = 0; i < 90; i++) {
			int x = r.nextInt(Math.max(1, width)), y = r.nextInt(Math.max(1, height * 3 / 4));
			float tw = 0.35F + 0.65F * (0.5F + 0.5F * (float) Math.sin(t * (0.08 + r.nextFloat() * 0.12) + r.nextFloat() * 6.3));
			float in = Math.min(1, t / 14F);
			g.fill(x, y, x + px, y + px, Ui.alpha(0xFFE8ECFF, tw * in * (0.4F + r.nextFloat() * 0.6F)));
		}
		// The star: across from the top right, slowing as it comes down.
		float x0 = width * 1.05F, y0 = height * 0.12F, x1 = width * 0.5F, y1 = height * 0.52F;
		float start = 8, land = 36;
		float p = Math.max(0, Math.min(1, (t - start) / (land - start)));
		float e = 1 - (1 - p) * (1 - p) * (1 - p);
		if (t >= start) {
			float hx = x0 + (x1 - x0) * e, hy = y0 + (y1 - y0) * e;
			if (p < 1) {
				// The trail: pixel squares behind it, smaller and fainter, with sparkles dropping off.
				for (int k = 14; k >= 1; k--) {
					float pk = Math.max(0, e - k * 0.028F);
					float tx = x0 + (x1 - x0) * pk, ty = y0 + (y1 - y0) * pk;
					int sz = Math.max(px, (int) ((15 - k) * 0.55F * px));
					g.fill((int) tx - sz / 2, (int) ty - sz / 2, (int) tx + (sz + 1) / 2, (int) ty + (sz + 1) / 2,
							Ui.alpha(k % 3 == 0 ? 0xFFFFE27A : 0xFFFFFFFF, (15 - k) / 15F * 0.85F));
				}
				java.util.Random sp = new java.util.Random((long) (t * 3));
				for (int k = 0; k < 6; k++) {
					float b = Math.max(0, e - sp.nextFloat() * 0.35F);
					float sx = x0 + (x1 - x0) * b + (sp.nextFloat() - 0.5F) * 10 * px, sy = y0 + (y1 - y0) * b + sp.nextFloat() * 14 * px;
					g.fill((int) sx, (int) sy, (int) sx + px, (int) sy + px, Ui.alpha(0xFFFFF4C0, 0.8F));
				}
				int hs = 5 * px;
				Ui.glow(g, (int) hx, (int) hy, 14 * px, 14 * px, 0x18FFF0B0);
				g.fill((int) hx - hs / 2, (int) hy - hs / 2, (int) hx + hs / 2 + 1, (int) hy + hs / 2 + 1, 0xFFFFFFFF);
			} else {
				// Landed: a burst of sparkles, and the star twinkling where it fell.
				float since = t - land;
				for (int k = 0; k < 16; k++) {
					double a = Math.PI * 2 * k / 16;
					float d = Math.min(1, since / 10F) * (22 + (k % 3) * 8) * px;
					float fade = Math.max(0, 1 - since / 18F);
					int sx = (int) (x1 + Math.cos(a) * d), sy = (int) (y1 + Math.sin(a) * d);
					g.fill(sx, sy, sx + px * 2, sy + px * 2, Ui.alpha(k % 2 == 0 ? 0xFFFFE27A : 0xFFFFFFFF, fade));
				}
				float tw = 0.8F + 0.2F * (float) Math.sin(t * 0.35);
				int ss = Math.round(14 * px * tw);
				Ui.icon(g, "emote/star", (int) x1 - ss / 2, (int) y1 - ss / 2, ss, 0xFFFFFFFF);
			}
		}
		// The maker's name under where it fell.
		if (t > land + 2) {
			float a = Math.min(1, (t - land - 2) / 12F);
			int big = Math.max(2, s * 2);
			String name = "INSID3 TH3 CUB3";
			int nw = Ui.boldWidth(font, name, big);
			Ui.draw(g, font, Ui.bold(name), (width - nw) / 2, (int) (y1 + 14 * px), Ui.alpha(0xFFFFFFFF, a), big, true);
			Ui.centred(g, font, "presents", width / 2, (int) (y1 + 14 * px) + 12 * big, Ui.alpha(0xFFB8C0E0, a), s, true);
		}
		// Into white for the title.
		if (t > OPEN_END - 12) g.fill(0, 0, width, height, Ui.alpha(0xFFFFFFFF, Math.min(1, (t - (OPEN_END - 12)) / 12F)));
	}

	/** A soft shadow on the ground under someone's feet. */
	private void shadow(GuiGraphics g, int cx, int feet, int tall) {
		int r = Math.max(6 * s, tall / 5);
		Ui.round(g, cx - r, feet - Math.max(2, r / 6), 2 * r, Math.max(4, r / 3), 0x55000000, Math.max(1, r / 6));
		Ui.round(g, cx - r * 2 / 3, feet - Math.max(1, r / 9), r * 4 / 3, Math.max(2, r / 5), 0x40000000, Math.max(1, r / 8));
	}

	/**
	 * Whoever's in the scene as Oak talks (Oak himself, you, or your rival), in 3D on the right, standing on the
	 * top of the text box (the text starts on the left), turned a little towards it.
	 */
	private void drawSpeaker(GuiGraphics g, int boxTop, float secs) {
		int top = height / 9 + 4 * s, tall = boxTop - top + 2 * s;
		int cx = width - 2 * 8 * s - tall / 3 - 6 * s, feet = boxTop + 2 * s;
		Ui.glow(g, cx, feet - tall / 2, tall * 2 / 5, tall / 2, 0x0AFFF4D8);
		shadow(g, cx, feet, tall);
		boolean speaking = talk != null && !talk.typedOut();
		Identifier who = switch (scene) {
			case "hero" -> hero();
			case "rival" -> RIVAL;
			default -> OAK;
		};
		// They look out at you (turned a little towards the text, the head straight on).
		Figures.Batch b = Figures.batch(0, 0, width, height);
		Figures.Fig f = b.person(who, cx, feet, tall).facing(-22).pitch(-2).look(22, 3).at(secs);
		if (scene.equals("oak")) f.talk(speaking ? 1 : 0.25F);
		else if (scene.equals("hero")) {
			// You, saying hello: a different greeting every couple of seconds.
			int[] hi = {Figures.WAVE, Figures.WAVE_BOTH, Figures.CHEER, Figures.TADA};
			int at = (int) (secs / 2.2F) % hi.length;
			f.emote(hi[at], secs - (int) (secs / 2.2F) * 2.2F).facing(-10);
		} else f.talk(0.2F);
		b.draw(g);
	}

	private void drawGender(GuiGraphics g, float secs, float partial) {
		int top = height / 9 + 22 * s, ch = height - top - height / 9 - 12 * s, cw = Math.min(ch * 2 / 3, width / 3), gap = 16 * s;
		int x0 = (width - 2 * cw - gap) / 2;
		Ui.centred(g, font, "Are you a boy? Or are you a girl?", width / 2, top - 14 * s, Ui.TEXT, s, true);
		String[] label = {"BOY", "GIRL"};
		int[] col = {0xFF4A8FE0, 0xFFE5525C};
		for (int i = 0; i < 2; i++) if (over(x0 + i * (cw + gap), top, cw, ch)) genderAt = i;
		if (genderAt != lastGender) {
			lastGender = genderAt;
			genderSince = ticks;
			genderEmote = (genderEmote + 1 + (int) (Math.random() * (Figures.HAPPY.length - 1))) % Figures.HAPPY.length;
			Sounds.select();
		}
		float since = ticks - genderSince + partial;
		Figures.Batch b = Figures.batch(0, 0, width, height);
		int tall = ch - 44 * s;
		for (int i = 0; i < 2; i++) {
			int x = x0 + i * (cw + gap);
			int idx = i;
			boolean on = genderAt == i;
			// The card: glass, its colour along the top, warmer and lit when it's the one.
			Ui.round(g, x - s, top - s, cw + 2 * s, ch + 2 * s, on ? 0xFFFFFFFF : 0x40FFFFFF, 4 * s);
			g.fillGradient(x, top, x + cw, top + ch, on ? Ui.darker(col[i], 0.55F) | 0xFF000000 : 0xF0141B2E, on ? 0xF0101626 : 0xF00D1222);
			Ui.round(g, x, top, cw, 5 * s, col[i], 2 * s);
			int cx = x + cw / 2, feet = top + 12 * s + tall;
			if (on) Ui.glow(g, cx, feet - tall / 2, cw / 2, tall / 2, 0x0EFFFFFF);
			shadow(g, cx, feet, tall);
			Identifier tex = Retromon.id("textures/entity/player/" + (i == 1 ? "girl" : "boy") + ".png");
			Figures.Fig f = b.person(tex, cx, feet, tall).at(secs + i * 1.3F).pitch(-2);
			if (on) {
				// The one you point at turns to you and does something: waves (one hand or both), cheers, jumps for
				// joy, points, twirls, claps, or flings its arms out (ta-da!).
				f.emote(Figures.HAPPY[genderEmote], since / 20F).facing(0).look(0, 2);
			} else f.facing(i == 0 ? 18 : -18);
			int lw = Ui.boldWidth(font, label[i], s);
			Ui.draw(g, font, Ui.bold(label[i]), x + (cw - lw) / 2, top + ch - 18 * s, 0xFFFFFFFF, s, true);
			hit(x, top, cw, ch, () -> pickGender(idx));
		}
		b.draw(g);
		// A bubble over the one you point at, to go with what they're doing.
		int cx = x0 + genderAt * (cw + gap) + cw / 2;
		String[] bubbles = {"happy", "star", "heart", "exclaim", "note", "happy", "star", "heart"};
		int lift = (int) (Figures.emoteHop(Figures.HAPPY[genderEmote], since / 20F) * tall);
		Ui.bubble(g, bubbles[genderEmote % bubbles.length], cx + tall / 5, top + 12 * s - lift, s, Math.min(1, Math.max(0, since - 3) / 6F));
	}

	private void drawNames(GuiGraphics g, float secs) {
		// Who's being named stands on the left; the names on a card on the right.
		int top = height / 9 + 8 * s, tall = height - top - height / 9 - 16 * s, feet = top + tall;
		int fx = width / 3 - 10 * s;
		Ui.glow(g, fx, feet - tall / 2, tall * 2 / 5, tall / 2, 0x0AFFF4D8);
		shadow(g, fx, feet, tall);
		Figures.Batch b = Figures.batch(0, 0, width, height);
		Figures.Fig who = b.person(namingRival ? RIVAL : hero(), fx, feet, tall).facing(20).pitch(-2).at(secs);
		if (typing) who.emote(Figures.THINK, secs);
		else who.look(0, 3);
		b.draw(g);
		int cw = Math.min(170 * s, width / 2 - 20 * s), cx = width / 2 + 10 * s;
		int listH = names.height(s);
		int ch = 26 * s + listH + (typing ? 30 * s : 0), cy = height / 2 - ch / 2;
		Ui.round(g, cx, cy, cw, ch, 0xF0141B2E, 4 * s);
		Ui.heading(g, font, namingRival ? "YOUR RIVAL'S NAME" : "YOUR NAME", cx + 10 * s, cy + 8 * s, Ui.TEXT, s);
		choice(g, names, cx + 4 * s, cy + 20 * s, cw - 8 * s, this::pickName);
		if (typing) {
			int fy = cy + 22 * s + listH, fw = cw - 20 * s;
			Ui.round(g, cx + 10 * s, fy, fw, 18 * s, Ui.CREAM, 3 * s);
			String shown = typed + ((ticks / 10) % 2 == 0 && typed.length() < 10 ? "_" : "");
			Ui.draw(g, font, Ui.bold(shown.isEmpty() ? "_" : shown), cx + 18 * s, fy + 5 * s, Ui.CREAM_INK, s, false);
			Ui.draw(g, font, Ui.ui("Enter"), cx + 10 * s + fw - 8 * s - Ui.width(font, "Enter", s), fy + 5 * s, 0xFF8A93AA, s, false);
		}
	}

	/**
	 * The three slots as cards side by side: a picture of where you were, your name and where, your badges, your
	 * POKéDEX and time played, your party, and when you last played. An empty slot invites a new game. BACK below.
	 */
	private void drawSlots(GuiGraphics g) {
		int m = 8 * s, gap = 8 * s;
		int cw = Math.min(170 * s, (width - 2 * m - 2 * gap) / 3), x0 = (width - 3 * cw - 2 * gap) / 2;
		int picH = cw * 9 / 16, ch = picH + 92 * s;
		int top = Math.max(m + 22 * s, (height - ch) / 2 - 10 * s);
		String title = replacing ? "ALL THREE SLOTS ARE FULL" : "CONTINUE";
		Ui.heading(g, font, title, (width - Ui.boldWidth(font, title, s)) / 2, top - 20 * s, Ui.GOLD, s);
		if (replacing) Ui.centred(g, font, "Choose a game to start over in.", width / 2, top - 9 * s, Ui.MUTED, s, true);
		for (int k = 0; k < SLOTS; k++) {
			int x = x0 + k * (cw + gap), idx = k;
			if (confirm == null && over(x, top, cw, ch)) slotAt = k;
			boolean on = slotAt == k;
			int y = top - (on ? 3 * s : 0);
			SlotInfo c = slotCards[k];
			Ui.round(g, x - s, y - s, cw + 2 * s, ch + 2 * s, on ? 0xFFFFFFFF : 0x40FFFFFF, 4 * s);
			g.fillGradient(x, y, x + cw, y + ch, 0xF41A2440, 0xF40E1424);
			// The picture (or, empty, a dim field with a plus).
			if (c != null && c.picture() != null) {
				int pw = c.picW(), ph = c.picH();
				// Fills the frame, cut to fit.
				float k1 = Math.max(cw / (float) pw, picH / (float) ph);
				int sw = Math.round(cw / k1), sh = Math.round(picH / k1);
				g.blit(RenderPipelines.GUI_TEXTURED, c.picture(), x, y, (pw - sw) / 2F, (ph - sh) / 2F, cw, picH, sw, sh, pw, ph);
			} else {
				g.fillGradient(x, y, x + cw, y + picH, 0xFF2A4A3A, 0xFF16261E);
				if (c == null || c.empty()) Ui.centred(g, font, "+", x + cw / 2, y + picH / 2 - 8 * s, 0x90FFFFFF, Math.max(2, s * 2), true);
			}
			Ui.round(g, x + 4 * s, y + 4 * s, Ui.boldWidth(font, "SLOT " + (k + 1), s) + 8 * s, 11 * s, 0xC0101626, 2 * s);
			Ui.draw(g, font, Ui.bold("SLOT " + (k + 1)), x + 8 * s, y + 6 * s, 0xFFFFFFFF, s, false);
			int ty = y + picH + 6 * s, tx = x + 7 * s;
			if (c == null || c.empty()) {
				Ui.heading(g, font, "EMPTY", tx, ty, Ui.TEXT, s);
				Ui.text(g, font, replacing ? "" : "Start a new game here.", tx, ty + 12 * s, Ui.MUTED, s);
			} else {
				String who = c.str("name", "TRAINER");
				Ui.heading(g, font, who, tx, ty, "girl".equals(c.str("gender", "")) ? 0xFFFF9FC0 : 0xFF9FD0FF, s);
				Ui.text(g, font, c.str("place", "KANTO"), tx, ty + 11 * s, Ui.TEXT, s);
				// Badges: eight, filled for each one won.
				int badges = c.num("badges"), bs = 7 * s;
				for (int b = 0; b < 8; b++) {
					int bx = tx + b * (bs + 2 * s), by = ty + 24 * s;
					Ui.round(g, bx, by, bs, bs, b < badges ? 0xFFF2C744 : 0x40FFFFFF, Math.max(1, s));
				}
				String dex = c.info().has("dex") && c.info().get("dex").getAsBoolean() ? "POKéDEX " + c.num("caught") : "NO POKéDEX YET";
				Ui.text(g, font, dex, tx, ty + 35 * s, Ui.MUTED, s);
				int secs = c.num("played");
				String time = secs / 3600 + ":" + String.format("%02d", secs / 60 % 60);
				Ui.right(g, font, time, x + cw - 7 * s, ty + 35 * s, Ui.TEXT, s);
				// The party.
				int ps = Math.min(16 * s, (cw - 14 * s) / 6), pi = 0;
				if (c.info().has("party") && c.info().get("party").isJsonArray()) {
					for (com.google.gson.JsonElement e : c.info().getAsJsonArray("party")) {
						if (pi >= 6) break;
						Ui.pic(g, Retromon.id("textures/gui/mon/" + e.getAsString() + ".png"), tx + pi * ps, ty + 46 * s, ps, ps, 128, 128, 0xFFFFFFFF);
						pi++;
					}
				}
				Ui.text(g, font, c.when(), tx, ty + 46 * s + ps + 3 * s, Ui.DIM, s);
			}
			if (confirm == null) hit(x, top, cw, ch, () -> pickSlot(idx));
		}
		// BACK.
		int bw = 60 * s, bx = (width - bw) / 2, byy = top + ch + 10 * s;
		boolean onBack = slotAt == SLOTS || confirm == null && over(bx, byy, bw, 16 * s);
		if (confirm == null && over(bx, byy, bw, 16 * s)) slotAt = SLOTS;
		Ui.pill(g, bx, byy, bw, 16 * s, onBack, s);
		Ui.centred(g, font, "BACK", bx + bw / 2, byy + 4 * s, onBack ? Ui.CREAM_INK : Ui.MUTED, s, false);
		if (confirm == null) hit(bx, byy, bw, 16 * s, () -> pickSlot(SLOTS));
		if (replacing) Ui.centred(g, font, "The game you replace is moved to .minecraft/retromon-old-saves (the last three are kept there).", width / 2,
				byy + 22 * s, Ui.DIM, s, true);
		if (confirm != null) {
			SlotInfo c = slotCards[slotAt];
			List<String> lines = List.of("Start over in SLOT " + (slotAt + 1) + "?", c.str("name", "This") + "'s game (" + c.str("place", "KANTO") + ") is moved",
					"to retromon-old-saves, and a new game begins.");
			int w = 0;
			for (String l : lines) w = Math.max(w, Ui.width(font, l, s));
			w += 24 * s;
			int cwid = confirm.width(font, s), h = lines.size() * 11 * s + 16 * s, px = (width - w) / 2, py = height / 2 - h / 2 - 20 * s;
			g.fill(0, 0, width, height, 0x90000000);
			Ui.round(g, px, py, w, h, 0xF0141B2E, 3 * s);
			for (int i = 0; i < lines.size(); i++) Ui.text(g, font, lines.get(i), px + 12 * s, py + 8 * s + i * 11 * s, Ui.TEXT, s);
			choice(g, confirm, (width - cwid) / 2, py + h + 6 * s, cwid, i -> confirmed(i == 0));
		}
	}

	private void panel(GuiGraphics g, List<String> lines) {
		panel(g, lines, false);
	}

	private void panel(GuiGraphics g, List<String> lines, boolean back) {
		int w = 0;
		for (String l : lines) w = Math.max(w, Ui.width(font, l, s));
		w += 24 * s;
		int h = lines.size() * 11 * s + 14 * s + (back ? 12 * s : 0), x = (width - w) / 2, y = height * 4 / 5 - h / 2;
		Ui.round(g, x, y, w, h, 0xF0141B2E, 3 * s);
		for (int i = 0; i < lines.size(); i++) Ui.text(g, font, lines.get(i), x + 12 * s, y + 8 * s + i * 11 * s, Ui.TEXT, s);
		if (back) Ui.text(g, font, "A: back", x + 12 * s, y + h - 16 * s, Ui.DIM, s);
	}
}
