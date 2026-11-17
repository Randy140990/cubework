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
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.insidethecube.retromon.Retromon;
import com.insidethecube.retromon.game.Game;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

/**
 * The game's title: the pixel Charizard against the night sky, RETROMON, PRESS START. Then CONTINUE (your
 * save) or NEW GAME, which makes a fresh copy of the Kanto map as your save and loads you into Pallet Town.
 * The map is read from the retromon folder in your Minecraft folder (the world folder, or its zip).
 */
public class IntroScreen extends GameScreen {
	private static final Identifier ART = Retromon.id("textures/gui/intro.png");
	private static final int ART_W = 480, ART_H = 270;

	private enum Step {
		TITLE, MENU, PICK, CONFIRM, COPYING, FAILED
	}

	private final Screen parent;
	private volatile Step step = Step.TITLE;
	private Choice menu;
	private Choice yesNo;
	private List<Path> maps = new ArrayList<>();
	private int pickAt;
	private Path chosen;
	private volatile String progress = "";
	private volatile String failure = "";
	private volatile boolean copied;

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

	private static Path save() {
		return root().resolve("saves").resolve(Game.SAVE_NAME);
	}

	@Override
	protected void init() {
		super.init();
		if (ticks == 0) Music.play("title_screen");
	}

	@Override
	public void tick() {
		super.tick();
		if (copied) {
			copied = false;
			load();
		}
	}

	private void openMenu() {
		List<String> options = new ArrayList<>();
		if (Files.exists(save().resolve("level.dat"))) options.add("CONTINUE");
		options.add("NEW GAME");
		options.add("BACK");
		menu = new Choice(options);
		step = Step.MENU;
	}

	private void menuPick(int i) {
		menu.at = i;
		switch (menu.selected()) {
			case "CONTINUE" -> load();
			case "NEW GAME" -> {
				maps = findMaps();
				pickAt = 0;
				step = Step.PICK;
			}
			default -> onClose();
		}
	}

	private void pickMap(int i) {
		if (i >= maps.size()) {
			step = Step.MENU;
			return;
		}
		chosen = maps.get(i);
		if (Files.exists(save())) {
			yesNo = new Choice(List.of("NO", "YES"));
			step = Step.CONFIRM;
		} else newGame();
	}

	@Override
	protected boolean key(KeyEvent e) {
		switch (step) {
			case TITLE -> {
				if (Ui.isA(e)) {
					Sounds.select();
					openMenu();
				} else if (Ui.isB(e)) onClose();
			}
			case MENU -> {
				if (menu.key(e)) return true;
				if (Ui.isB(e)) step = Step.TITLE;
				else if (Ui.isA(e)) menuPick(menu.at);
			}
			case PICK -> {
				int n = maps.size() + 2;
				if (Ui.isUp(e)) pickAt = Math.floorMod(pickAt - 1, n);
				else if (Ui.isDown(e)) pickAt = Math.floorMod(pickAt + 1, n);
				else if (Ui.isB(e)) step = Step.MENU;
				else if (Ui.isA(e)) {
					if (pickAt == maps.size()) openFolder();
					else pickMap(pickAt);
				}
			}
			case CONFIRM -> {
				if (yesNo.key(e)) return true;
				if (Ui.isB(e) || (Ui.isA(e) && yesNo.at == 0)) step = Step.PICK;
				else if (Ui.isA(e)) newGame();
			}
			case FAILED -> {
				if (Ui.isA(e) || Ui.isB(e)) step = Step.MENU;
			}
			default -> {
			}
		}
		return true;
	}

	@Override
	protected void clickAnywhere() {
		if (step == Step.TITLE) openMenu();
		else if (step == Step.FAILED) step = Step.MENU;
	}

	@Override
	protected void back() {
		switch (step) {
			case TITLE -> onClose();
			case MENU -> step = Step.TITLE;
			case PICK -> step = Step.MENU;
			case CONFIRM -> step = Step.PICK;
			case FAILED -> step = Step.MENU;
			default -> {
			}
		}
	}

	private void openFolder() {
		try {
			Files.createDirectories(mapFolder());
		} catch (IOException ex) {
			Retromon.LOG.warn("Couldn't make {}", mapFolder(), ex);
		}
		Util.getPlatform().openPath(mapFolder());
	}

	@Override
	public void onClose() {
		if (step == Step.COPYING) return;
		Music.stop();
		if (minecraft != null) minecraft.setScreen(parent);
	}

	private void load() {
		Minecraft mc = Minecraft.getInstance();
		mc.createWorldOpenFlows().openWorld(Game.SAVE_NAME, () -> mc.setScreen(new IntroScreen(parent)));
	}

	// ---------- a new game: a fresh copy of the map ----------

	/** The maps you could start from: your worlds (any save with a level.dat, but not Retromon's), and zips or worlds in .minecraft/retromon. */
	private static List<Path> findMaps() {
		List<Path> out = new ArrayList<>();
		Path saves = root().resolve("saves");
		if (Files.isDirectory(saves)) {
			try (Stream<Path> s = Files.list(saves)) {
				s.filter(p -> Files.exists(p.resolve("level.dat")) && !p.getFileName().toString().equals(Game.SAVE_NAME)).sorted().forEach(out::add);
			} catch (IOException e) {
				Retromon.LOG.warn("Couldn't list your worlds", e);
			}
		}
		Path dir = mapFolder();
		if (Files.isDirectory(dir)) {
			try (Stream<Path> s = Files.walk(dir, 4)) {
				s.filter(p -> p.getFileName().toString().equals("level.dat")).map(Path::getParent).forEach(out::add);
			} catch (IOException e) {
				Retromon.LOG.warn("Couldn't look in {}", dir, e);
			}
			try (Stream<Path> s = Files.list(dir)) {
				s.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".zip")).forEach(out::add);
			} catch (IOException e) {
				Retromon.LOG.warn("Couldn't look in {}", dir, e);
			}
		}
		// The Kanto map first, if one's called that.
		out.sort((a, b) -> Boolean.compare(!a.getFileName().toString().toLowerCase().contains("kanto"), !b.getFileName().toString().toLowerCase().contains("kanto")));
		return out;
	}

	private void newGame() {
		Path map = chosen;
		if (map == null) return;
		step = Step.COPYING;
		progress = "Getting the map ready...";
		Thread t = new Thread(() -> {
			try {
				Path to = save();
				if (Files.exists(to)) delete(to);
				Files.createDirectories(to);
				if (Files.isDirectory(map)) copyFolder(map, to);
				else unzip(map, to);
				prepare(to);
				copied = true;
			} catch (Exception e) {
				Retromon.LOG.error("Couldn't make the new game", e);
				failure = e.getMessage() == null ? e.toString() : e.getMessage();
				step = Step.FAILED;
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
			d.putString("LevelName", Game.SAVE_NAME);
			d.putBoolean("allowCommands", true);
			d.putInt("GameType", 2);
			d.remove("Player");
		});
		NbtIo.writeCompressed(root, dat);
		Files.deleteIfExists(to.resolve("level.dat_old"));
		Files.writeString(to.resolve(Game.MARKER), "{\"version\":2}", StandardCharsets.UTF_8);
	}

	// ---------- drawing ----------

	@Override
	protected void draw(GuiGraphics g, float partial) {
		g.fill(0, 0, width, height, 0xFF0A0E24);
		// The picture, filling the screen, crisp.
		float k = Math.max(width / (float) ART_W, height / (float) ART_H);
		int w = Math.round(ART_W * k), h = Math.round(ART_H * k);
		g.blit(RenderPipelines.GUI_TEXTURED, ART, (width - w) / 2, (height - h) / 2, 0, 0, w, h, ART_W, ART_H, ART_W, ART_H);
		int big = Math.max(2, s * 3);
		Ui.centred(g, font, "RETROMON", width / 2, height / 7, Ui.GOLD, big, true);
		Ui.centred(g, font, "KANTO & JOHTO", width / 2, height / 7 + 11 * big, 0xFFE8ECFF, s, true);
		switch (step) {
			case TITLE -> {
				if ((ticks / 10) % 2 == 0) Ui.centred(g, font, "PRESS START", width / 2, height * 3 / 4, 0xFFFFFFFF, Math.max(1, s * 2), true);
				Ui.centred(g, font, "Space, Enter or click", width / 2, height * 3 / 4 + 22 * s, 0xFFB8C0E0, s, true);
			}
			case MENU -> {
				int mw = Math.max(menu.width(font, s), 90 * s);
				choice(g, menu, (width - mw) / 2, height * 3 / 5, mw, this::menuPick);
			}
			case PICK -> drawPick(g);
			case CONFIRM -> {
				panel(g, List.of("Start a new game from " + chosen.getFileName() + "?", "Your Retromon save will be replaced.", "(The map itself isn't changed.)"));
				int yw = yesNo.width(font, s);
				choice(g, yesNo, (width - yw) / 2, height * 3 / 5 + 40 * s, yw, i -> {
					if (i == 1) newGame();
					else step = Step.PICK;
				});
			}
			case COPYING -> panel(g, List.of(progress));
			case FAILED -> panel(g, List.of("The new game couldn't be made:", failure, "A: back."));
		}
		Ui.centred(g, font, "A fan game by Insid3 Th3 Cub3.  Pokemon models from Cobblemon (CC BY-NC 3.0).", width / 2, height - 12, 0xFF9098C0, 1, true);
	}

	private void drawPick(GuiGraphics g) {
		int m = 8 * s, w = Math.min(width - 2 * m, 260 * s), row = 14 * s;
		int n = Math.min(maps.size(), 10);
		int h = (n + 2) * row + 26 * s, x = (width - w) / 2, y = Math.max(m, height * 2 / 5 - 10 * s);
		Ui.panel(g, x, y, w, h, s);
		Ui.text(g, font, "Which map? (it's copied, never changed)", x + 10 * s, y + 7 * s, Ui.MUTED, s);
		for (int i = 0; i < n + 2; i++) {
			int ty = y + 22 * s + i * row;
			int idx = i < n ? i : i == n ? maps.size() : maps.size() + 1;
			if (over(x, ty - 2 * s, w, row)) pickAt = idx;
			boolean on = pickAt == idx;
			if (on) Ui.round(g, x + 4 * s, ty - 2 * s, w - 8 * s, row - s, 0x334A8FE0, s);
			String label;
			if (i < n) {
				Path p = maps.get(i);
				boolean zip = !Files.isDirectory(p);
				label = p.getFileName() + (zip ? "  (zip)" : p.getParent() != null && p.getParent().getFileName().toString().equals("saves") ? "" : "  (retromon folder)");
			} else label = i == n ? "Open the retromon folder..." : "BACK";
			Ui.text(g, font, label, x + 12 * s, ty + s, on ? Ui.TEXT : Ui.MUTED, s);
			hit(x, ty - 2 * s, w, row, () -> {
				if (idx == maps.size()) openFolder();
				else if (idx > maps.size()) step = Step.MENU;
				else pickMap(idx);
			});
		}
		if (maps.isEmpty()) Ui.text(g, font, "No worlds found. Put the Kanto map in your saves.", x + 10 * s, y + h + 4 * s, Ui.GOLD, s);
	}

	private void panel(GuiGraphics g, List<String> lines) {
		int w = 0;
		for (String l : lines) w = Math.max(w, Ui.width(font, l, s));
		w += 24 * s;
		int h = lines.size() * 11 * s + 14 * s, x = (width - w) / 2, y = height * 3 / 5 - h / 2;
		Ui.panel(g, x, y, w, h, s);
		for (int i = 0; i < lines.size(); i++) Ui.text(g, font, lines.get(i), x + 12 * s, y + 8 * s + i * 11 * s, Ui.TEXT, s);
	}
}
