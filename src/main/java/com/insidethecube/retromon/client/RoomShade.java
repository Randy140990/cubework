package com.insidethecube.retromon.client;

import java.io.DataInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.insidethecube.retromon.Retromon;
import com.insidethecube.retromon.game.Interiors;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.model.SpriteFinder;
import net.fabricmc.fabric.api.util.TriState;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;

/**
 * (0.66, John's 13:36: "the lighting just doesn't exist, which is one of the main things that makes the room look
 * amazing ... you said what I see in the Retromon interior viewer UI is what I'm going to see in game, and it's just
 * not.") A room's light, as the Interior Viewer works it out and draws it (tools/interior/shade.js): the sun in from
 * high over the open front with its soft shadows, the room's light round about by which way a face faces, darker in
 * corners and under things, the lamps by the time of day. The viewer works out, at every corner of every piece the
 * game draws, how much sun reaches it (S) and how open it is (AO), and keeps them for the game
 * (retromon/interiors/light/<room>.bin). Here each piece's corners are coloured from those, by the same sums as the
 * viewer's (light, colour), so the room looks as the viewer shows it. With Retromon Light (the shaders) the colour is
 * kept at 1/SCALE (the shader takes it times SCALE: light brighter than the picture, as the viewer has it); without,
 * it's as it is, up to 1.
 */
public final class RoomShade {
	private RoomShade() {}

	private static final int Q = 64, OFF = 4096;
	private static final double SCALE = 2.5, AO_K = 0.6, AMB = 0.1;
	/** The sun's and the room's light by the time of day: day, morning, evening, night (colour, strength). */
	private static final double[][] TOD = {{0xffffff, 0.64, 0xf4f7fb, 0.72}, {0xfff0d8, 0.6, 0xfff4e6, 0.72}, {0xffd6aa, 0.42, 0xffecd0, 0.84},
			{0xffe2b8, 0.38, 0xffefd6, 0.88}};
	private static final int GROUND = 0xa89e92;
	private static final double[][] N = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

	/** A room's corners: their keys (sorted), each one's S and AO (0 to 255), and whether it has windows. */
	record Table(long[] keys, byte[] s, byte[] ao, boolean windows) {
		int find(long key) {
			int lo = 0, hi = keys.length - 1;
			while (lo <= hi) {
				int mid = (lo + hi) >>> 1;
				long k = keys[mid];
				if (k < key) lo = mid + 1;
				else if (k > key) hi = mid - 1;
				else return mid;
			}
			return -1;
		}
	}

	/** The light now, for a room: the sun's way, its colour, the room's light from above and below, the lamps. */
	record Light(double[] sunDir, double[] sun, double[] sky, double[] ground, double lamp) {}

	private static final Map<String, Optional<Table>> TABLES = new ConcurrentHashMap<>();
	private static volatile Map<String, double[]> mats;
	/** The time of day the rooms are coloured for (k, side, m: RoomLightRenderer.sky(), in steps), and the shaders. */
	private static volatile double tk = 0, tSide = 1, tm = 0;
	private static volatile boolean shaders;
	private static int stamp;
	private static long lastCheck;

	private static Table load(String id) {
		try (InputStream raw = RoomShade.class.getResourceAsStream("/retromon/interiors/light/" + id + ".bin")) {
			if (raw == null) return null;
			DataInputStream in = new DataInputStream(new java.io.BufferedInputStream(raw));
			if (in.readByte() != 'R' || in.readByte() != 'L' || in.readByte() != 'T' || in.readByte() != '1') return null;
			boolean windows = in.readByte() != 0;
			int n = in.readInt();
			long[] keys = new long[n];
			long last = 0;
			for (int i = 0; i < n; i++) {
				long d = 0, mul = 1;
				while (true) {
					int b = in.readUnsignedByte();
					d += (b & 127) * mul;
					if (b < 128) break;
					mul *= 128;
				}
				last += d;
				keys[i] = last;
			}
			byte[] s = new byte[n], ao = new byte[n];
			in.readFully(s);
			in.readFully(ao);
			return new Table(keys, s, ao, windows);
		} catch (Exception e) {
			Retromon.LOG.warn("Retromon: couldn't read the light for {}: {}", id, e.toString());
			return null;
		}
	}

	static Table table(String id) {
		return TABLES.computeIfAbsent(id, k -> Optional.ofNullable(load(k))).orElse(null);
	}

	/** Each texture's part in the light: [its colour, the light it gives off, 1 if it's a lamp]. */
	private static Map<String, double[]> mats() {
		Map<String, double[]> m = mats;
		if (m != null) return m;
		Map<String, double[]> out = new HashMap<>();
		try (InputStream in = RoomShade.class.getResourceAsStream("/retromon/interiors/light/mats.json")) {
			if (in != null) {
				JsonObject o = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
				for (Map.Entry<String, JsonElement> e : o.entrySet()) {
					JsonArray a = e.getValue().getAsJsonArray();
					out.put(e.getKey(), new double[] {a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble(), a.size() > 3 ? a.get(3).getAsDouble() : 0});
				}
			}
		} catch (Exception e) {
			Retromon.LOG.warn("Retromon: couldn't read the rooms' materials: {}", e.toString());
		}
		return mats = out;
	}

	// ---------- the sums (shade.js has the very same) ----------

	/** The sun's way (towards it) for a room W by D: from high over the open front, a little west. */
	static double[] sunDir(double w, double d) {
		double x = -0.2 * w, y = 18, z = d / 2 + 9, l = Math.sqrt(x * x + y * y + z * z);
		return new double[] {x / l, y / l, z / l};
	}

	private static double[] rgb(double hex, double times) {
		int h = (int) hex;
		return new double[] {(h >> 16 & 255) / 255.0 * times, (h >> 8 & 255) / 255.0 * times, (h & 255) / 255.0 * times};
	}

	private static double[] lerp(double[] a, double[] b, double t) {
		return new double[] {a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t};
	}

	/** The light at a time of day (k how low the sun is, side -1 the morning's or 1 the evening's, m how far it's night). */
	static Light light(double k, double side, double m, boolean windows, double w, double d) {
		double[] day = TOD[0], lo = side < 0 ? TOD[1] : TOD[2], night = TOD[3];
		double[] sun = lerp(lerp(rgb(day[0], day[1]), rgb(lo[0], lo[1]), k), rgb(night[0], night[1]), m);
		double[] sky = lerp(lerp(rgb(day[2], day[3]), rgb(lo[2], lo[3]), k), rgb(night[2], night[3]), m);
		double hemi = day[3] + (lo[3] - day[3]) * k, hemiI = hemi + (night[3] - hemi) * m;
		double lamp = windows ? Math.max(m, side > 0 ? k : 0) : 1;
		return new Light(sunDir(w, d), sun, sky, rgb(GROUND, hemiI), lamp);
	}

	/** A corner's colour (r, g, b): from its S and AO (0 to 255), which way it faces (0 to 5), its material. */
	static double[] colour(Light L, int dir, int s, int ao, double[] mat) {
		double[] n = N[dir], sd = L.sunDir;
		double w = 0.5 * n[1] + 0.5;
		double nl = Math.max(0, n[0] * sd[0] + n[1] * sd[1] + n[2] * sd[2]) * (s / 255.0);
		double a = 1 - AO_K * (1 - ao / 255.0);
		double mul = mat == null ? 1 : mat[0], glow = mat == null ? 0 : mat[1];
		boolean lamp = mat != null && mat[2] > 0.5;
		double[] out = new double[3];
		for (int c = 0; c < 3; c++) {
			double ind = (AMB + L.ground[c] + (L.sky[c] - L.ground[c]) * w) * a;
			out[c] = mul * (ind + L.sun[c] * nl) + (lamp ? 0.04 + (glow * 1.15 - 0.04) * L.lamp : glow);
		}
		return out;
	}

	static long key(double x, double y, double z, int dir) {
		long qx = Math.round(x * Q) + OFF, qy = Math.round(y * Q) + OFF, qz = Math.round(z * Q) + OFF;
		return ((dir * 16384L + qx) * 16384L + qy) * 16384L + qz;
	}

	private static int dirOf(net.minecraft.core.Direction f) {
		return switch (f) {
			case EAST -> 0;
			case WEST -> 1;
			case UP -> 2;
			case DOWN -> 3;
			case SOUTH -> 4;
			default -> 5;
		};
	}

	// ---------- colouring the pieces ----------

	/** What a block needs to colour its pieces: its room, the room's corners and the light now. */
	record At(Interiors.Room room, Table table, Light light, boolean shaders) {}

	private static volatile At last;

	/** For a block in a room that has its light (in the interiors), else null. */
	static At at(BlockPos pos) {
		Minecraft mc = Minecraft.getInstance();
		ClientLevel level = mc.level;
		if (level == null || level.dimension() != Interiors.LEVEL) return null;
		Interiors.Room r = Interiors.roomAt(pos.getX() + 0.5);
		if (r == null) return null;
		At a = last;
		int st = stamp;
		if (a != null && a.room == r && a.light != null && lastStamp == st) return a;
		Table t = table(r.id());
		if (t == null) return null;
		a = new At(r, t, light(tk, tSide, tm, t.windows(), r.w(), r.d()), shaders);
		last = a;
		lastStamp = st;
		return a;
	}

	private static volatile int lastStamp = -1;

	/** A piece's material, by its picture: [its colour, the light it gives off, 1 if a lamp, how see-through], or null. */
	static double[] matOf(MutableQuadView q, SpriteFinder finder) {
		TextureAtlasSprite sprite = finder == null ? null : finder.find(q);
		if (sprite == null) return null;
		String name = sprite.contents().name().getPath();
		if (!name.startsWith("block/interior/")) return null;
		return mats().get(name.substring("block/interior/".length()));
	}

	/** Colours one piece's corners (a Retromon block's, in a room with its light). */
	static void colour(MutableQuadView q, BlockPos pos, At a, double[] mat) {
		int dir = dirOf(q.lightFace());
		Interiors.Room r = a.room;
		for (int i = 0; i < 4; i++) {
			double x = pos.getX() - r.ox() + q.x(i), y = pos.getY() - r.oy() + q.y(i), z = pos.getZ() - r.oz() + q.z(i);
			int s = 255, ao = 255;
			int at = a.table.find(key(x, y, z, dir));
			if (at < 0) {
				// (A corner a hair off the one worked out: the nearest round it.)
				outer:
				for (int dx = -1; dx <= 1; dx++)
					for (int dy = -1; dy <= 1; dy++)
						for (int dz = -1; dz <= 1; dz++) {
							at = a.table.find(key(x + dx / (double) Q, y + dy / (double) Q, z + dz / (double) Q, dir));
							if (at >= 0) break outer;
						}
			}
			if (at >= 0) {
				s = a.table.s[at] & 255;
				ao = a.table.ao[at] & 255;
			}
			double[] c = colour(a.light, dir, s, ao, mat);
			int[] v = new int[3];
			for (int k = 0; k < 3; k++) v[k] = (int) Math.round(Math.max(0, Math.min(1, a.shaders ? c[k] / SCALE : c[k])) * 255);
			q.color(i, 0xFF000000 | v[0] << 16 | v[1] << 8 | v[2]);
		}
		q.emissive(true);
		q.diffuseShade(false);
		q.ambientOcclusion(TriState.FALSE);
	}

	// ---------- keeping up with the time of day ----------

	private static boolean irisOn() {
		if (!FabricLoader.getInstance().isModLoaded("iris")) return false;
		try {
			Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
			Object o = api.getMethod("getInstance").invoke(null);
			return (Boolean) api.getMethod("isShaderPackInUse").invoke(o);
		} catch (Throwable t) {
			return false;
		}
	}

	/**
	 * Each tick: the time of day in steps (a sixth of the way through dawn, dusk and nightfall), and the shaders on or
	 * off; when either changes, the room you're in is coloured again.
	 */
	public static void tick(Minecraft mc) {
		if (mc.level == null || mc.player == null) return;
		float[] sky = RoomLightRenderer.sky();
		double k = Math.round(sky[0] * 6) / 6.0, side = sky[1] < 0 ? -1 : 1, m = Math.round(sky[2] * 6) / 6.0;
		long now = System.currentTimeMillis();
		boolean sh = shaders;
		if (now - lastCheck > 1000) {
			lastCheck = now;
			sh = irisOn();
		}
		if (k == tk && side == tSide && m == tm && sh == shaders) return;
		tk = k;
		tSide = side;
		tm = m;
		shaders = sh;
		stamp++;
		if (mc.level.dimension() != Interiors.LEVEL || mc.levelRenderer == null) return;
		Interiors.Room r = Interiors.roomAt(mc.player.getX());
		if (r == null || table(r.id()) == null) return;
		int x0 = SectionPos.blockToSectionCoord(r.ox() - 8), x1 = SectionPos.blockToSectionCoord(r.ox() + (int) Math.ceil(r.w()) + 8);
		int y0 = SectionPos.blockToSectionCoord(r.oy() - 8), y1 = SectionPos.blockToSectionCoord(r.oy() + (int) Math.ceil(r.h()) + 8);
		int z0 = SectionPos.blockToSectionCoord(r.oz() - 8), z1 = SectionPos.blockToSectionCoord(r.oz() + (int) Math.ceil(r.d()) + 8);
		for (int sx = x0; sx <= x1; sx++)
			for (int sy = y0; sy <= y1; sy++)
				for (int sz = z0; sz <= z1; sz++) mc.levelRenderer.setSectionDirty(sx, sy, sz);
	}

	/** Whether a room is coloured this way (its lamps are in its colours: RoomLightRenderer leaves them be). */
	public static boolean lit(String roomId) {
		return roomId != null && table(roomId) != null;
	}
}
