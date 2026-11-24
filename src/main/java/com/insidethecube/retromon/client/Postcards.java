package com.insidethecube.retromon.client;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import com.insidethecube.retromon.Retromon;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.LevelResource;

/**
 * A picture of each place, for the Town Map: the first time you're in a town or on a route, once the place's
 * name has slid away and nothing's on screen, the game keeps a picture of the view (in the save, as
 * retromon_places/&lt;place&gt;.png). Picking the place on the map shows it.
 */
public final class Postcards {
	private Postcards() {}

	private static String pending;
	private static int wait;
	private static final Map<String, Identifier> LOADED = new HashMap<>();

	private static Path folder() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.getSingleplayerServer() == null) return null;
		try {
			Path f = mc.getSingleplayerServer().getWorldPath(LevelResource.ROOT).resolve("retromon_places");
			clearPhotos(f);
			return f;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * (0.61, John 18:06: "I don't want it on the town map ... I will provide those ... to bake them into the solution")
	 * The places' pictures that PHOTO MODE put on the TOWN MAP (0.59.7 to 0.60) taken off it, once a save: nothing else
	 * has put any there since the map's own pictures stopped being taken.
	 */
	private static void clearPhotos(Path f) {
		Path done = f.resolve(".photos_cleared");
		if (!Files.isDirectory(f) || Files.exists(done)) return;
		try (var all = Files.list(f)) {
			for (Path p : all.toList()) if (p.getFileName().toString().endsWith(".png")) Files.deleteIfExists(p);
			Files.writeString(done, "0.61");
		} catch (Exception e) {
			Retromon.LOG.warn("Couldn't take the photos off the TOWN MAP", e);
		}
	}

	private static boolean safe(String place) {
		return place != null && place.matches("[a-z0-9_]+");
	}

	/** You've come into a place: a picture of it soon, if there isn't one yet. */
	public static void entered(String place) {
		Path f = folder();
		if (!safe(place) || f == null || Files.exists(f.resolve(place + ".png"))) return;
		pending = place;
		wait = 140;
	}

	public static void tick(Minecraft mc) {
		if (pending == null) return;
		if (!Adventure.inSave || mc.player == null) {
			pending = null;
			return;
		}
		// A clear view: nothing on screen, nobody talking, not in a battle, the place's name gone.
		if (mc.screen != null || BattleScene.staged() || Adventure.frozen > 0 || Device.busy() || mc.options.hideGui) return;
		if (--wait > 0) return;
		String place = pending;
		pending = null;
		Path f = folder();
		if (f == null) return;
		try {
			Files.createDirectories(f);
			Screenshot.takeScreenshot(mc.getMainRenderTarget(), shot -> java.util.concurrent.CompletableFuture.runAsync(() -> {
				try (shot; NativeImage out = SlotSnapshot.shrink(shot)) {
					out.writeToFile(f.resolve(place + ".png"));
					mc.execute(() -> forget(place));
				} catch (Exception e) {
					Retromon.LOG.warn("Couldn't keep a picture of {}", place, e);
				}
			}));
		} catch (Exception e) {
			Retromon.LOG.warn("Couldn't take a picture of {}", place, e);
		}
	}

	/** The picture of a place (384 by 216), or null if there isn't one yet. */
	public static Identifier picture(String place) {
		if (!safe(place)) return null;
		if (LOADED.containsKey(place)) return LOADED.get(place);
		Identifier id = null;
		Path f = folder();
		if (f != null && Files.exists(f.resolve(place + ".png"))) {
			try (var in = Files.newInputStream(f.resolve(place + ".png"))) {
				NativeImage img = NativeImage.read(in);
				id = Retromon.id("dynamic/place_" + place);
				Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(() -> "Retromon place " + place, img));
			} catch (Exception e) {
				id = null;
			}
		}
		LOADED.put(place, id);
		return id;
	}

	/** (0.59.7, 15:35) A PHOTO MODE picture made the place's Town Map picture (the whole frame, 384 by 216). (0.61: not used.) */
	public static void keep(String place, NativeImage full) {
		Path f = folder();
		if (!safe(place) || f == null) return;
		try (NativeImage out = SlotSnapshot.fit(full, 384, 216)) {
			Files.createDirectories(f);
			out.writeToFile(f.resolve(place + ".png"));
			Minecraft.getInstance().execute(() -> forget(place));
		} catch (Exception e) {
			Retromon.LOG.warn("Couldn't keep the photo of {} for the map", place, e);
		}
	}

	private static void forget(String place) {
		Identifier id = LOADED.remove(place);
		if (id != null) Minecraft.getInstance().getTextureManager().release(id);
	}

	/** Leaving the game: the pictures are let go of (another save has its own). */
	public static void reset() {
		pending = null;
		Minecraft mc = Minecraft.getInstance();
		for (Identifier id : LOADED.values()) if (id != null) mc.getTextureManager().release(id);
		LOADED.clear();
	}
}
