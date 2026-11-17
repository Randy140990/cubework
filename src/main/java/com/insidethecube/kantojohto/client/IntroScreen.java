package com.insidethecube.kantojohto.client;

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

import com.insidethecube.kantojohto.RouteOne;
import com.insidethecube.kantojohto.game.Game;
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
 * The game's title: the pixel Charizard against the night sky, ROUTE ONE, PRESS START. Then CONTINUE (your
 * save) or NEW GAME, which makes a fresh copy of the Kanto map as your save and loads you into Pallet Town.
 * The map is read from the routeone folder in your Minecraft folder (the world folder, or its zip).
 */
public class IntroScreen extends GameScreen {
	private static final Identifier ART = RouteOne.id("textures/gui/intro.png");
	private static final int ART_W = 480, ART_H = 270;

	private enum Step {
		TITLE, MENU, CONFIRM, NO_MAP, COPYING, FAILED
	}

	private final Screen parent;
	private volatile Step step = Step.TITLE;
	private Choice menu;
	private Choice yesNo;
	private volatile String progress = "";
	private volatile String failure = "";
	private volatile boolean copied;

	public IntroScreen(Screen parent) {
		super("Route One");
		this.parent = parent;
	}

	private static Path root() {
		return Minecraft.getInstance().gameDirectory.toPath();
	}

	public static Path mapFolder() {
		return root().resolve("routeone");
	}

	private static Path save() {
		return root().resolve("saves").resolve(Game.SAVE_NAME);
	}

	@Override
	protected void init() {
		super.init();
		if (ticks == 0) Sounds.fanfare();
	}

	@Override
	public void tick() {
		super.tick();
		if (copied) {
			copied = false;
			load();
		}
	}

	@Override
	protected boolean key(KeyEvent e) {
		switch (step) {
			case TITLE -> {
				if (Ui.isA(e)) {
					Sounds.select();
					List<String> options = new ArrayList<>();
					if (Files.exists(save().resolve("level.dat"))) options.add("CONTINUE");
					options.add("NEW GAME");
					options.add("BACK");
					menu = new Choice(options);
					step = Step.MENU;
				} else if (Ui.isB(e)) onClose();
			}
			case MENU -> {
				if (menu.key(e)) return true;
				if (Ui.isB(e)) step = Step.TITLE;
				else if (Ui.isA(e)) {
					Sounds.select();
					switch (menu.selected()) {
						case "CONTINUE" -> load();
						case "NEW GAME" -> {
							if (findMap() == null) step = Step.NO_MAP;
							else if (Files.exists(save())) {
								yesNo = new Choice(List.of("NO", "YES"));
								step = Step.CONFIRM;
							} else newGame();
						}
						default -> onClose();
					}
				}
			}
			case CONFIRM -> {
				if (yesNo.key(e)) return true;
				if (Ui.isB(e) || (Ui.isA(e) && yesNo.at == 0)) step = Step.MENU;
				else if (Ui.isA(e)) newGame();
			}
			case NO_MAP -> {
				if (Ui.isA(e)) {
					try {
						Files.createDirectories(mapFolder());
					} catch (IOException ex) {
						RouteOne.LOG.warn("Couldn't make {}", mapFolder(), ex);
					}
					Util.getPlatform().openPath(mapFolder());
				} else if (Ui.isB(e)) step = Step.MENU;
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
	public void onClose() {
		if (step == Step.COPYING) return;
		if (minecraft != null) minecraft.setScreen(parent);
	}

	private void load() {
		Minecraft mc = Minecraft.getInstance();
		mc.createWorldOpenFlows().openWorld(Game.SAVE_NAME, () -> mc.setScreen(new IntroScreen(parent)));
	}

	// ---------- a new game: a fresh copy of the map ----------

	/** The map to copy: a folder with level.dat in the routeone folder (a few levels down at most), or a zip of one. */
	private static Object findMap() {
		Path dir = mapFolder();
		if (!Files.isDirectory(dir)) return null;
		try (Stream<Path> s = Files.walk(dir, 4)) {
			Path level = s.filter(p -> p.getFileName().toString().equals("level.dat")).min(Comparator.comparingInt(Path::getNameCount)).orElse(null);
			if (level != null) return level.getParent();
		} catch (IOException e) {
			RouteOne.LOG.warn("Couldn't look in {}", dir, e);
		}
		try (Stream<Path> s = Files.list(dir)) {
			return s.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".zip")).findFirst().orElse(null);
		} catch (IOException e) {
			return null;
		}
	}

	private void newGame() {
		Object map = findMap();
		if (map == null) {
			step = Step.NO_MAP;
			return;
		}
		step = Step.COPYING;
		progress = "Getting the map ready...";
		Thread t = new Thread(() -> {
			try {
				Path to = save();
				if (Files.exists(to)) delete(to);
				Files.createDirectories(to);
				if (map instanceof Path p && Files.isDirectory(p)) copyFolder(p, to);
				else unzip((Path) map, to);
				prepare(to);
				copied = true;
			} catch (Exception e) {
				RouteOne.LOG.error("Couldn't make the new game", e);
				failure = e.getMessage() == null ? e.toString() : e.getMessage();
				step = Step.FAILED;
			}
		}, "Route One new game");
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
	 * Makes the copy a Route One save: its name, cheats on (for the /routeone commands), Adventure mode, and
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
		Files.writeString(to.resolve(Game.MARKER), "{\"version\":1,\"placed\":false}", StandardCharsets.UTF_8);
	}

	// ---------- drawing ----------

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
		g.fill(0, 0, width, height, 0xFF0A0E24);
		// The picture, filling the screen, crisp.
		float k = Math.max(width / (float) ART_W, height / (float) ART_H);
		int w = Math.round(ART_W * k), h = Math.round(ART_H * k);
		g.blit(RenderPipelines.GUI_TEXTURED, ART, (width - w) / 2, (height - h) / 2, 0, 0, w, h, ART_W, ART_H, ART_W, ART_H);
		int big = Math.max(2, s * 3);
		Ui.centred(g, font, "ROUTE ONE", width / 2, height / 7, 0xFFFFD040, big, true);
		Ui.centred(g, font, "KANTO & JOHTO", width / 2, height / 7 + 11 * big, 0xFFE8ECFF, s, true);
		switch (step) {
			case TITLE -> {
				if ((ticks / 10) % 2 == 0) Ui.centred(g, font, "PRESS START", width / 2, height * 3 / 4, 0xFFFFFFFF, Math.max(1, s * 2), true);
				Ui.centred(g, font, "Space or Enter", width / 2, height * 3 / 4 + 22 * s, 0xFFB8C0E0, s, true);
			}
			case MENU -> menu.render(g, font, (width - menu.width(font, s)) / 2, height * 3 / 5, s);
			case CONFIRM -> {
				panel(g, List.of("Start a new game?", "Your saved game will be lost."));
				yesNo.render(g, font, (width - yesNo.width(font, s)) / 2, height * 3 / 5 + 34 * s, s);
			}
			case NO_MAP -> panel(g, List.of("The Kanto map isn't in place yet.", "Put its world folder (or its zip) in", ".minecraft/routeone, then choose NEW GAME.",
					"A: open the folder.   B: back."));
			case COPYING -> panel(g, List.of(progress));
			case FAILED -> panel(g, List.of("The new game couldn't be made:", failure, "A: back."));
		}
		Ui.centred(g, font, "A fan game by Insid3 Th3 Cub3.  Pokemon models from Cobblemon (CC BY-NC 3.0).", width / 2, height - 12, 0xFF9098C0, 1, true);
	}

	private void panel(GuiGraphics g, List<String> lines) {
		int w = 0;
		for (String l : lines) w = Math.max(w, Ui.width(font, l, s));
		w += 20 * s;
		int h = lines.size() * 11 * s + 12 * s, x = (width - w) / 2, y = height * 3 / 5 - h / 2;
		Ui.box(g, x, y, w, h, s);
		for (int i = 0; i < lines.size(); i++) Ui.text(g, font, lines.get(i), x + 10 * s, y + 7 * s + i * 11 * s, Ui.INK, s);
	}
}
