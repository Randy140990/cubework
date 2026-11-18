package com.insidethecube.retromon.client;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.insidethecube.retromon.Retromon;
import com.insidethecube.retromon.engine.Mon;
import com.insidethecube.retromon.engine.Places;
import com.insidethecube.retromon.game.PlayerData;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/**
 * What a save slot shows on CONTINUE: a picture of where you are (taken now and then while you play, and after you
 * SAVE, with no menu up and not in a battle) and its details (your name, where you are, badges, the POKéDEX, time
 * played, your party), written into the save's own folder.
 */
public final class SlotSnapshot {
	private SlotSnapshot() {}

	public static final String PICTURE = "retromon_slot.png", INFO = "retromon_slot.json";
	private static final int W = 384, H = 216;
	private static int ticks;
	private static boolean soon;

	/** After a SAVE: a new picture at the next clear moment. */
	public static void soon() {
		soon = true;
	}

	public static void tick(Minecraft mc) {
		if (!Adventure.inSave || mc.getSingleplayerServer() == null || mc.player == null || mc.level == null) {
			ticks = 0;
			return;
		}
		// The first a few seconds in, then every half minute.
		if (++ticks == 100 || ticks % 600 == 0) soon = true;
		if (!soon || mc.screen != null || BattleScene.staged() || Adventure.frozen > 0 || mc.options.hideGui) return;
		soon = false;
		Path folder = folder(mc);
		if (folder == null) return;
		writeInfo(folder);
		try {
			Screenshot.takeScreenshot(mc.getMainRenderTarget(), shot -> java.util.concurrent.CompletableFuture.runAsync(() -> {
				try (shot; NativeImage out = shrink(shot)) {
					out.writeToFile(folder.resolve(PICTURE));
				} catch (Exception e) {
					Retromon.LOG.warn("Couldn't save the slot's picture", e);
				}
			}));
		} catch (Exception e) {
			Retromon.LOG.warn("Couldn't take the slot's picture", e);
		}
	}

	/** Leaving the game: the details brought up to date (the picture stays the last one taken). */
	public static void leaving(Minecraft mc) {
		if (!Adventure.inSave) return;
		Path folder = folder(mc);
		if (folder != null) writeInfo(folder);
	}

	private static Path folder(Minecraft mc) {
		MinecraftServer server = mc.getSingleplayerServer();
		if (server == null) return null;
		try {
			return server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
		} catch (Exception e) {
			return null;
		}
	}

	private static void writeInfo(Path folder) {
		PlayerData d = ClientState.data;
		JsonObject o = new JsonObject();
		o.addProperty("name", d.name.isEmpty() ? "TRAINER" : d.name);
		o.addProperty("gender", d.gender);
		Places.Place pl = d.place == null || d.place.isEmpty() ? null : Places.get(d.place);
		o.addProperty("place", pl != null ? pl.name() : d.place == null || d.place.isEmpty() ? "PALLET TOWN" : d.place.toUpperCase().replace('_', ' '));
		o.addProperty("badges", d.badges.size());
		o.addProperty("dex", d.hasDex());
		o.addProperty("seen", d.seen.size());
		o.addProperty("caught", d.caught.size());
		o.addProperty("played", (int) d.played);
		JsonArray party = new JsonArray();
		for (Mon m : d.party) party.add(m.species.id());
		o.add("party", party);
		try {
			Files.writeString(folder.resolve(INFO), o.toString(), StandardCharsets.UTF_8);
		} catch (Exception e) {
			Retromon.LOG.warn("Couldn't save the slot's details", e);
		}
	}

	/**
	 * The middle of the screen (clear of the corners, where the place name and the keys are), cut to 16:9 and made
	 * small, each pixel the average of the block it stands for.
	 */
	private static NativeImage shrink(NativeImage in) {
		int sw = in.getWidth(), sh = in.getHeight();
		int cw = Math.min(sw * 3 / 4, sh * 3 / 4 * 16 / 9), ch = cw * 9 / 16;
		int x0 = (sw - cw) / 2, y0 = (sh - ch) / 2;
		NativeImage out = new NativeImage(W, H, false);
		for (int y = 0; y < H; y++) {
			int ya = y0 + y * ch / H, yb = Math.max(ya + 1, y0 + (y + 1) * ch / H);
			for (int x = 0; x < W; x++) {
				int xa = x0 + x * cw / W, xb = Math.max(xa + 1, x0 + (x + 1) * cw / W);
				long r = 0, g = 0, b = 0, n = 0;
				for (int yy = ya; yy < yb; yy++) {
					for (int xx = xa; xx < xb; xx++) {
						int c = in.getPixel(xx, yy);
						r += (c >> 16) & 255;
						g += (c >> 8) & 255;
						b += c & 255;
						n++;
					}
				}
				out.setPixel(x, y, 0xFF000000 | (int) (r / n) << 16 | (int) (g / n) << 8 | (int) (b / n));
			}
		}
		return out;
	}
}
