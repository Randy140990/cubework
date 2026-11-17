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
		TITLE, MENU, SLOTS, TALK, GENDER, NAME, COPYING, FAILED
	}

	private final Screen parent;
	private volatile Step step = Step.TITLE;
	private Choice menu;
	private List<Path> slots = new ArrayList<>();
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
		if (ticks == 0) Music.play("title_screen");
	}

	@Override
	public void tick() {
		super.tick();
		if (talk != null) talk.tick();
		if (copied) {
			copied = false;
			load(slotName);
		}
	}

	// ---------- the menu and the save slots ----------

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

	private void openMenu() {
		slots = findSlots();
		List<String> options = new ArrayList<>();
		if (!slots.isEmpty()) options.add("CONTINUE");
		options.add("NEW GAME");
		options.add("BACK");
		menu = new Choice(options);
		step = Step.MENU;
	}

	private void menuPick(int i) {
		menu.at = i;
		Sounds.press();
		switch (menu.selected()) {
			case "CONTINUE" -> {
				slotAt = 0;
				step = Step.SLOTS;
			}
			case "NEW GAME" -> startOak();
			default -> onClose();
		}
	}

	// ---------- Professor Oak's welcome ----------

	private void say(List<String> lines, Runnable then) {
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
				"Now tell me. Are you a boy? Or are you a girl?"), () -> step = Step.GENDER);
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
			say(List.of("Right! So your name is " + name + "!", "This is my grandson. He's been your rival since you were a baby.",
					"...Erm, what is his name again?"), () -> startNaming(true));
		} else {
			rivalName = name;
			say(List.of("That's right! I remember now! His name is " + name + "!", playerName + "!", "Your very own POKéMON legend is about to unfold!",
					"A world of dreams and adventures with POKéMON awaits! Let's go!"), this::newGame);
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
			case TITLE -> {
				if (Ui.isA(e)) {
					Sounds.press();
					openMenu();
				} else if (Ui.isB(e)) onClose();
			}
			case MENU -> {
				if (menu.key(e)) return true;
				if (Ui.isB(e)) step = Step.TITLE;
				else if (Ui.isA(e)) menuPick(menu.at);
			}
			case SLOTS -> {
				int n = slots.size() + 1;
				if (Ui.isUp(e)) slotAt = Math.floorMod(slotAt - 1, n);
				else if (Ui.isDown(e)) slotAt = Math.floorMod(slotAt + 1, n);
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

	private void pickSlot(int i) {
		Sounds.press();
		if (i >= slots.size()) {
			step = Step.MENU;
			return;
		}
		load(slots.get(i).getFileName().toString());
	}

	@Override
	protected void clickAnywhere() {
		if (step == Step.TITLE) openMenu();
		else if (step == Step.TALK && talk.press()) afterTalk.run();
		else if (step == Step.FAILED) step = Step.MENU;
	}

	@Override
	protected void back() {
		switch (step) {
			case TITLE -> onClose();
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
		Minecraft mc = Minecraft.getInstance();
		mc.createWorldOpenFlows().openWorld(folder, () -> mc.setScreen(new IntroScreen(parent)));
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
		slotName = freeSlot();
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

	@Override
	protected void draw(GuiGraphics g, float partial) {
		g.fill(0, 0, width, height, 0xFF0A0E24);
		// The picture, filling the screen, crisp (dimmed while Oak talks).
		float k = Math.max(width / (float) ART_W, height / (float) ART_H);
		int w = Math.round(ART_W * k), h = Math.round(ART_H * k);
		g.blit(RenderPipelines.GUI_TEXTURED, ART, (width - w) / 2, (height - h) / 2, 0, 0, w, h, ART_W, ART_H, ART_W, ART_H);
		boolean oak = step == Step.TALK || step == Step.GENDER || step == Step.NAME || step == Step.COPYING;
		if (oak) {
			g.fill(0, 0, width, height, 0xC00A0E24);
			Ui.cinema(g, width, height, 1);
			drawOak(g);
		} else {
			int big = Math.max(2, s * 3);
			int tw = Ui.boldWidth(font, "RETROMON", big);
			Ui.draw(g, font, Ui.bold("RETROMON"), (width - tw) / 2, height / 7, Ui.GOLD, big, true);
			Ui.centred(g, font, "KANTO & JOHTO", width / 2, height / 7 + 12 * big, 0xFFE8ECFF, s, true);
		}
		int m = 8 * s;
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
			case TALK -> talk.render(g, font, m, height - boxHeight() - m - height / 10, width - 2 * m, boxHeight(), s, ticks, true);
			case GENDER -> drawGender(g);
			case NAME -> drawNames(g);
			case COPYING -> panel(g, List.of(progress));
			case FAILED -> panel(g, Ui.wrap(font, "The new game couldn't be made: " + failure, 240), true);
		}
		if (!oak) Ui.centred(g, font, "A fan game by Insid3 Th3 Cub3.  Pokemon models from Cobblemon (CC BY-NC 3.0).", width / 2, height - 12, 0xFF9098C0, 1, true);
	}

	/** The professor, as a nameplate under a soft spotlight (his part of the scene). */
	private void drawOak(GuiGraphics g) {
		int cx = width / 2, cy = height / 3, r = 46 * s;
		for (int i = r; i > 0; i -= Math.max(1, s)) {
			int a = (int) (60 * (1 - i / (float) r));
			g.fill(cx - i, cy - i / 2, cx + i, cy + i / 2, a << 24 | 0xFFF4D8);
		}
		int pw = Ui.boldWidth(font, "PROF. OAK", s * 2);
		Ui.draw(g, font, Ui.bold("PROF. OAK"), cx - pw / 2, cy - 8 * s, Ui.CREAM, s * 2, true);
		Ui.centred(g, font, "POKéMON RESEARCHER  ·  PALLET TOWN", cx, cy + 12 * s, 0xFFB8C0E0, s, true);
	}

	private void drawSlots(GuiGraphics g) {
		int m = 8 * s, w = Math.min(width - 2 * m, 250 * s), row = 26 * s;
		int n = Math.min(slots.size(), 6);
		int h = (n + 1) * row + 26 * s, x = (width - w) / 2, y = Math.max(m, height * 2 / 5 - 20 * s);
		Ui.round(g, x, y, w, h, 0xF0141B2E, 3 * s);
		Ui.heading(g, font, "CONTINUE", x + 12 * s, y + 9 * s, Ui.TEXT, s);
		for (int i = 0; i <= n; i++) {
			int ty = y + 24 * s + i * row;
			int idx = i < n ? i : slots.size();
			if (over(x, ty, w, row - 3 * s)) slotAt = idx;
			boolean on = slotAt == idx;
			Ui.pill(g, x + 6 * s, ty, w - 12 * s, row - 4 * s, on, s);
			int ink = on ? Ui.CREAM_INK : Ui.TEXT;
			if (i < n) {
				Path p = slots.get(i);
				Ui.draw(g, font, Ui.bold(p.getFileName().toString()), x + 14 * s, ty + 4 * s, ink, s, false);
				Ui.draw(g, font, Ui.ui(slotInfo(p)), x + 14 * s, ty + 13 * s, on ? 0xFF5A6480 : Ui.MUTED, s, false);
			} else Ui.draw(g, font, Ui.bold("BACK"), x + 14 * s, ty + 7 * s, on ? Ui.CREAM_INK : Ui.MUTED, s, false);
			hit(x, ty, w, row - 3 * s, () -> pickSlot(idx));
		}
	}

	private void drawGender(GuiGraphics g) {
		int cw = 90 * s, ch = 110 * s, gap = 14 * s, x0 = (width - 2 * cw - gap) / 2, y = height / 2 - ch / 2 + 10 * s;
		String[] label = {"BOY", "GIRL"};
		int[] col = {0xFF4A8FE0, 0xFFE5525C};
		for (int i = 0; i < 2; i++) {
			int x = x0 + i * (cw + gap);
			int idx = i;
			if (over(x, y, cw, ch)) genderAt = i;
			boolean on = genderAt == i;
			Ui.round(g, x, y, cw, ch, on ? Ui.CREAM : 0xE6141B2E, 3 * s);
			// A simple figure: head, body, legs, in the card's colour.
			int fx = x + cw / 2, fy = y + 16 * s;
			Ui.round(g, fx - 9 * s, fy, 18 * s, 18 * s, on ? 0xFFF1C9A5 : 0xFFB89A80, 4 * s);
			Ui.round(g, fx - 12 * s, fy - 3 * s, 24 * s, 8 * s, col[i], 2 * s);
			Ui.round(g, fx - 13 * s, fy + 20 * s, 26 * s, 30 * s, col[i], 3 * s);
			Ui.round(g, fx - 10 * s, fy + 51 * s, 8 * s, 18 * s, on ? 0xFF2C3550 : 0xFF3A4260, s);
			Ui.round(g, fx + 2 * s, fy + 51 * s, 8 * s, 18 * s, on ? 0xFF2C3550 : 0xFF3A4260, s);
			int lw = Ui.boldWidth(font, label[i], s);
			Ui.draw(g, font, Ui.bold(label[i]), fx - lw / 2, y + ch - 16 * s, on ? Ui.CREAM_INK : Ui.TEXT, s, !on);
			hit(x, y, cw, ch, () -> pickGender(idx));
		}
		Ui.centred(g, font, "Are you a boy? Or are you a girl?", width / 2, y - 18 * s, Ui.TEXT, s, true);
	}

	private void drawNames(GuiGraphics g) {
		int w = 150 * s, x = width / 2 - w - 6 * s, y = height / 2 - 30 * s;
		String q = namingRival ? "His name?" : "Your name?";
		Ui.centred(g, font, q, width / 2, y - 18 * s, Ui.TEXT, s, true);
		choice(g, names, x, y, w, this::pickName);
		// The typed name, in a cream field (when NEW NAME's picked).
		int fx = width / 2 + 6 * s, fw = 150 * s, fh = 22 * s;
		Ui.round(g, fx, y, fw, fh, typing ? Ui.CREAM : 0x80141B2E, 2 * s);
		String shown = typing ? typed + ((ticks / 10) % 2 == 0 && typed.length() < 10 ? "_" : "") : "NEW NAME: type it";
		Ui.draw(g, font, Ui.bold(shown), fx + 8 * s, y + 7 * s, typing ? Ui.CREAM_INK : Ui.MUTED, s, false);
		if (typing) Ui.text(g, font, "Enter: done.  Backspace: delete.", fx, y + fh + 5 * s, Ui.DIM, s);
	}

	private void panel(GuiGraphics g, List<String> lines) {
		panel(g, lines, false);
	}

	private void panel(GuiGraphics g, List<String> lines, boolean back) {
		int w = 0;
		for (String l : lines) w = Math.max(w, Ui.width(font, l, s));
		w += 24 * s;
		int h = lines.size() * 11 * s + 14 * s + (back ? 12 * s : 0), x = (width - w) / 2, y = height * 3 / 5 - h / 2;
		Ui.round(g, x, y, w, h, 0xF0141B2E, 3 * s);
		for (int i = 0; i < lines.size(); i++) Ui.text(g, font, lines.get(i), x + 12 * s, y + 8 * s + i * 11 * s, Ui.TEXT, s);
		if (back) Ui.text(g, font, "A: back", x + 12 * s, y + h - 16 * s, Ui.DIM, s);
	}
}
