package com.insidethecube.retromon.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.insidethecube.retromon.Retromon;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelResource;

/**
 * The Town Map's picture of the world, drawn from the world itself: for every column of blocks, the block on
 * top, how high it is and how deep any water is (baked from the Kanto map into townmap/, then kept up to date
 * from the chunks around you as you play, so what's built since shows too). Painted in the newer games' soft
 * colours: one grass green, cream paths, deep green trees, buildings in their own colours a little cleaner,
 * water paler where it's shallow with a white line along the shore; lit from the north-west, with taller things
 * casting short shadows to the south-east and buildings outlined.
 */
public final class TownMap {
	private TownMap() {}

	public static final int R = 512;
	/** What a block is, on the map. */
	static final byte WATER = 0, FOLIAGE = 1, GROUND = 2, PATH = 3, SAND = 4, BUILT = 5, NOTHING = 6;
	/** The open sea (and anywhere the map knows nothing of). */
	public static final int SEA = 0xFF348CCE;

	static final class Region {
		final int rx, rz;
		final List<String> names = new ArrayList<>();
		final Map<String, Integer> index = new HashMap<>();
		final short[] id = new short[R * R], y = new short[R * R];
		final byte[] depth = new byte[R * R];
		byte[] kinds = new byte[0];
		int[] colours = new int[0];
		/** Painted, ready to show (or null). */
		volatile int[] argb;
		/** Changed since it was painted: the box to paint again (x0, z0, x1, z1 in the region), or null. */
		int[] stale;
		NativeImage image;
		Identifier texture;
		boolean uploaded;

		Region(int rx, int rz) {
			this.rx = rx;
			this.rz = rz;
			name("minecraft:air");
		}

		int name(String n) {
			Integer i = index.get(n);
			if (i != null) return i;
			names.add(n);
			index.put(n, names.size() - 1);
			kinds = java.util.Arrays.copyOf(kinds, names.size());
			colours = java.util.Arrays.copyOf(colours, names.size());
			kinds[names.size() - 1] = kind(n);
			colours[names.size() - 1] = colour(n);
			return names.size() - 1;
		}
	}

	private static final Map<Long, Region> REGIONS = new HashMap<>();
	private static Map<String, Integer> palette;
	/** Where the map opens (the land), and all of it (for the whole-world picture): x0, z0, x1, z1. */
	public static int[] view = {-2288, -48, -46, 1037}, all = {-2937, -48, -46, 1370};
	private static boolean loaded;
	private static CompletableFuture<Void> painting;
	/** The whole world, small (a pixel for 4 by 4 blocks), for the zoomed-out map. */
	public static final int OVER = 4;
	private static int[] overArgb;
	private static int overW, overH;
	private static NativeImage overImage;
	private static boolean overUploaded;
	public static final Identifier OVERVIEW = Retromon.id("dynamic/townmap_overview");
	/** Changes since the bake, by chunk: kept in the save. */
	private static final Map<Long, Edit> EDITS = new HashMap<>();
	private static Path editsLoadedFrom;
	private static final ArrayDeque<long[]> SCAN = new ArrayDeque<>();

	private record Edit(String[] names, short[] y, byte[] depth) {}

	private static long key(int a, int b) {
		return ((long) a << 32) ^ (b & 0xFFFFFFFFL);
	}

	// ---------- what a block looks like ----------

	static byte kind(String name) {
		String n = name.startsWith("minecraft:") ? name.substring(10) : name;
		if (n.equals("air")) return NOTHING;
		if (n.equals("water") || n.equals("bubble_column") || n.startsWith("kelp") || n.endsWith("seagrass")) return WATER;
		if (n.contains("leaves") || n.equals("vine") || n.equals("brown_mushroom_block") || n.equals("red_mushroom_block") || n.equals("mushroom_stem")
				|| n.equals("azalea") || n.equals("flowering_azalea") || n.equals("moss_carpet")) return FOLIAGE;
		if (n.equals("dirt_path") || n.equals("gravel") || n.equals("coarse_dirt") || n.equals("rooted_dirt") || n.equals("packed_mud") || n.equals("mud"))
			return PATH;
		if (n.equals("sand") || n.equals("sandstone") || n.equals("smooth_sandstone") || n.equals("suspicious_sand") || n.equals("red_sand")) return SAND;
		if (n.startsWith("grass_block") || n.equals("dirt") || n.equals("podzol") || n.equals("mycelium") || n.equals("moss_block") || n.equals("clay")
				|| n.equals("snow") || n.equals("snow_block") || n.equals("farmland") || n.equals("lily_pad") || n.equals("poppy") || n.equals("dandelion")
				|| n.equals("bush")) return GROUND;
		return BUILT;
	}

	/** Blocks too small to see from above (tufts of grass, torches, signs...): the map looks under them. */
	static boolean seeThrough(String name) {
		String[] parts = {"minecraft:short_grass", "minecraft:tall_grass", "minecraft:fern", "minecraft:large_fern", "minecraft:dead_bush", "_button",
				"minecraft:torch", "minecraft:wall_torch", "minecraft:lantern", "_pressure_plate", "minecraft:tripwire", "minecraft:redstone_wire", "rail",
				"_sign", "_banner", "minecraft:lever", "minecraft:ladder", "minecraft:glass_pane", "minecraft:string", "minecraft:cobweb", "minecraft:vine",
				"_head", "_skull", "minecraft:flower_pot", "potted_", "minecraft:end_rod", "minecraft:chain", "minecraft:iron_bars", "candle",
				"minecraft:scaffolding", "minecraft:light", "minecraft:barrier", "minecraft:structure_void", "minecraft:cave_air", "minecraft:void_air"};
		for (String p : parts) if (name.contains(p)) return true;
		return false;
	}

	private static int colour(String name) {
		if (palette == null) loadPalette();
		Integer c = palette.get(name);
		if (c == null && name.contains("[")) c = palette.get(name.substring(0, name.indexOf('[')));
		if (c != null) return c;
		// A block the palette doesn't know (from another mod, say): its own map colour.
		try {
			String n = name.contains("[") ? name.substring(0, name.indexOf('[')) : name;
			var block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(n));
			int col = block.defaultBlockState().getMapColor(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, BlockPos.ZERO).col;
			if (col != 0) return col;
		} catch (Exception ignored) {
			// Not a block we can find.
		}
		return 0x8C8C8C;
	}

	private static void loadPalette() {
		palette = new HashMap<>();
		try (InputStream in = Minecraft.getInstance().getResourceManager().open(Retromon.id("townmap/palette.json"))) {
			JsonObject o = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
			for (var e : o.entrySet()) palette.put(e.getKey(), Integer.parseInt(e.getValue().getAsString(), 16));
		} catch (Exception e) {
			Retromon.LOG.warn("Couldn't read the Town Map's colours", e);
		}
	}

	// ---------- loading ----------

	/** Reads the baked map (once), and any changes kept in this save. */
	private static synchronized void load() {
		if (loaded) return;
		loaded = true;
		loadPalette();
		try (InputStream in = Minecraft.getInstance().getResourceManager().open(Retromon.id("townmap/index.json"))) {
			JsonObject o = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
			view = ints(o.getAsJsonArray("view"));
			if (o.has("all")) all = ints(o.getAsJsonArray("all"));
			for (var e : o.getAsJsonArray("regions")) {
				String[] p = e.getAsString().split("\\.");
				readRegion(Integer.parseInt(p[0]), Integer.parseInt(p[1]));
			}
		} catch (Exception e) {
			Retromon.LOG.warn("Couldn't read the Town Map", e);
		}
	}

	private static int[] ints(JsonArray a) {
		int[] out = new int[a.size()];
		for (int i = 0; i < out.length; i++) out[i] = a.get(i).getAsInt();
		return out;
	}

	private static void readRegion(int rx, int rz) {
		Identifier id = Retromon.id("townmap/r." + rx + "." + rz + ".bin");
		try (InputStream raw = Minecraft.getInstance().getResourceManager().open(id);
				DataInputStream in = new DataInputStream(new java.io.BufferedInputStream(new GZIPInputStream(raw)))) {
			byte[] magic = new byte[4];
			in.readFully(magic);
			in.readInt();
			in.readInt();
			in.readInt();
			Region r = new Region(rx, rz);
			int n = in.readShort();
			int[] map = new int[n];
			for (int i = 0; i < n; i++) map[i] = r.name(in.readUTF());
			for (int i = 0; i < R * R; i++) r.id[i] = (short) map[Math.max(0, Math.min(n - 1, in.readShort()))];
			for (int i = 0; i < R * R; i++) r.y[i] = in.readShort();
			in.readFully(r.depth);
			REGIONS.put(key(rx, rz), r);
		} catch (Exception e) {
			Retromon.LOG.warn("Couldn't read the Town Map's {}", id, e);
		}
	}

	private static Region region(int rx, int rz, boolean make) {
		Region r = REGIONS.get(key(rx, rz));
		if (r == null && make) {
			r = new Region(rx, rz);
			REGIONS.put(key(rx, rz), r);
		}
		return r;
	}

	public static Collection<Region> regions() {
		return REGIONS.values();
	}

	// ---------- painting ----------

	/** Whether the map's ready to show (painting starts the first time it's asked for). */
	public static boolean ready() {
		if (painting == null) {
			painting = CompletableFuture.runAsync(() -> {
				load();
				loadEdits();
				for (Region r : new ArrayList<>(REGIONS.values())) paint(r, 0, 0, R, R);
				overview();
			});
		}
		return painting.isDone();
	}

	private static int heightAt(int x, int z) {
		Region r = REGIONS.get(key(Math.floorDiv(x, R), Math.floorDiv(z, R)));
		if (r == null) return -64;
		return r.y[Math.floorMod(z, R) * R + Math.floorMod(x, R)];
	}

	private static byte kindAt(int x, int z) {
		Region r = REGIONS.get(key(Math.floorDiv(x, R), Math.floorDiv(z, R)));
		if (r == null) return NOTHING;
		return r.kinds[r.id[Math.floorMod(z, R) * R + Math.floorMod(x, R)]];
	}

	private static boolean land(byte k) {
		return k != WATER && k != NOTHING;
	}

	/** Paints a box of a region (x0, z0 to x1, z1, in the region). */
	static void paint(Region r, int x0, int z0, int x1, int z1) {
		int[] out = r.argb == null ? new int[R * R] : r.argb;
		int bx = r.rx * R, bz = r.rz * R;
		for (int z = Math.max(0, z0); z < Math.min(R, z1); z++) {
			for (int x = Math.max(0, x0); x < Math.min(R, x1); x++) {
				int i = z * R + x, wx = bx + x, wz = bz + z;
				byte k = r.kinds[r.id[i]];
				int c = r.colours[r.id[i]];
				int h = r.y[i];
				float cr = (c >> 16) & 255, cg = (c >> 8) & 255, cb = c & 255;
				float lum = (cr * 0.3F + cg * 0.59F + cb * 0.11F) / 255F;
				float or, og, ob;
				switch (k) {
					case GROUND -> {
						float f = clamp(1 + 0.21F * (lum / 0.42F - 1), 0.8F, 1.15F);
						or = 128 * f;
						og = 196 * f;
						ob = 96 * f;
					}
					case PATH -> {
						float f = clamp(0.9F + 0.3F * (lum - 0.45F), 0.85F, 1.05F);
						or = 226 * f;
						og = 205 * f;
						ob = 150 * f;
					}
					case SAND -> {
						or = 240;
						og = 226;
						ob = 172;
					}
					case FOLIAGE -> {
						float f = clamp(0.85F + 0.5F * (lum - 0.3F), 0.75F, 1.1F) * 0.6F + 0.4F;
						or = 62 * f;
						og = 140 * f;
						ob = 74 * f;
					}
					case BUILT -> {
						float m = (cr + cg + cb) / 3;
						or = clamp((m + (cr - m) * 1.25F) * 1.08F + 12, 0, 255);
						og = clamp((m + (cg - m) * 1.25F) * 1.08F + 12, 0, 255);
						ob = clamp((m + (cb - m) * 1.25F) * 1.08F + 12, 0, 255);
					}
					case WATER -> {
						float t = clamp((r.depth[i] - 1) / 6F, 0, 1);
						or = 130 + (52 - 130) * t;
						og = 214 + (140 - 214) * t;
						ob = 232 + (206 - 232) * t;
					}
					default -> {
						out[i] = SEA;
						continue;
					}
				}
				boolean isLand = land(k);
				if (isLand) {
					float shade = clamp(1 + 0.07F * (h - heightAt(wx - 1, wz - 1)), 0.7F, 1.25F);
					or *= shade;
					og *= shade;
					ob *= shade;
				}
				// A shadow from anything taller to the north-west.
				for (int s = 1; s <= 4; s++) {
					if (heightAt(wx - s, wz - s) > h + s * 1.1F) {
						float d = isLand ? 0.74F : 0.85F;
						or *= d;
						og *= d;
						ob *= d;
						break;
					}
				}
				if (k == WATER) {
					if (land(kindAt(wx + 1, wz)) || land(kindAt(wx - 1, wz)) || land(kindAt(wx, wz + 1)) || land(kindAt(wx, wz - 1))) {
						or = or * 0.35F + 235 * 0.65F;
						og = og * 0.35F + 250 * 0.65F;
						ob = ob * 0.35F + 248 * 0.65F;
					}
				} else if (k == BUILT) {
					if (h - heightAt(wx + 1, wz) >= 2 || h - heightAt(wx - 1, wz) >= 2 || h - heightAt(wx, wz + 1) >= 2 || h - heightAt(wx, wz - 1) >= 2) {
						or *= 0.7F;
						og *= 0.7F;
						ob *= 0.7F;
					}
				}
				out[i] = 0xFF000000 | (int) clamp(or, 0, 255) << 16 | (int) clamp(og, 0, 255) << 8 | (int) clamp(ob, 0, 255);
			}
		}
		r.argb = out;
		r.uploaded = false;
	}

	private static float clamp(float v, float lo, float hi) {
		return v < lo ? lo : Math.min(hi, v);
	}

	/** The whole world, a pixel for every 4 by 4 blocks (their average). */
	private static void overview() {
		int w = (all[2] - all[0]) / OVER + 1, h = (all[3] - all[1]) / OVER + 1;
		int[] out = new int[w * h];
		for (int oz = 0; oz < h; oz++) {
			for (int ox = 0; ox < w; ox++) out[oz * w + ox] = average(all[0] + ox * OVER, all[1] + oz * OVER);
		}
		overW = w;
		overH = h;
		overArgb = out;
		overUploaded = false;
	}

	private static int average(int x0, int z0) {
		int r = 0, g = 0, b = 0;
		for (int dz = 0; dz < OVER; dz++) {
			for (int dx = 0; dx < OVER; dx++) {
				int x = x0 + dx, z = z0 + dz;
				Region reg = REGIONS.get(key(Math.floorDiv(x, R), Math.floorDiv(z, R)));
				int c = reg == null || reg.argb == null ? SEA : reg.argb[Math.floorMod(z, R) * R + Math.floorMod(x, R)];
				r += (c >> 16) & 255;
				g += (c >> 8) & 255;
				b += c & 255;
			}
		}
		int n = OVER * OVER;
		return 0xFF000000 | (r / n) << 16 | (g / n) << 8 | (b / n);
	}

	public static int overW() {
		return overW;
	}

	public static int overH() {
		return overH;
	}

	/** On the render thread: anything painted since is sent to its texture. Returns the regions to draw. */
	public static void upload() {
		Minecraft mc = Minecraft.getInstance();
		for (Region r : REGIONS.values()) {
			if (r.stale != null) {
				int[] b = r.stale;
				r.stale = null;
				paint(r, b[0], b[1], b[2], b[3]);
				refreshOverview(r, b);
			}
			if (r.argb == null || r.uploaded) continue;
			if (r.image == null) {
				r.image = new NativeImage(R, R, false);
				r.texture = Retromon.id("dynamic/townmap_" + (r.rx < 0 ? "m" + -r.rx : String.valueOf(r.rx)) + "_" + (r.rz < 0 ? "m" + -r.rz : String.valueOf(r.rz)));
				mc.getTextureManager().register(r.texture, new DynamicTexture(() -> "Retromon town map", r.image));
			}
			int[] a = r.argb;
			for (int z = 0; z < R; z++) for (int x = 0; x < R; x++) r.image.setPixel(x, z, a[z * R + x]);
			if (mc.getTextureManager().getTexture(r.texture) instanceof DynamicTexture t) t.upload();
			r.uploaded = true;
		}
		if (overArgb != null && !overUploaded) {
			if (overImage == null || overImage.getWidth() != overW || overImage.getHeight() != overH) {
				overImage = new NativeImage(overW, overH, false);
				mc.getTextureManager().register(OVERVIEW, new DynamicTexture(() -> "Retromon town map overview", overImage));
			}
			for (int z = 0; z < overH; z++) for (int x = 0; x < overW; x++) overImage.setPixel(x, z, overArgb[z * overW + x]);
			if (mc.getTextureManager().getTexture(OVERVIEW) instanceof DynamicTexture t) t.upload();
			overUploaded = true;
		}
	}

	private static void refreshOverview(Region r, int[] b) {
		if (overArgb == null) return;
		int x0 = r.rx * R + b[0], z0 = r.rz * R + b[1], x1 = r.rx * R + b[2], z1 = r.rz * R + b[3];
		for (int oz = Math.max(0, (z0 - all[1]) / OVER); oz <= Math.min(overH - 1, (z1 - all[1]) / OVER); oz++) {
			for (int ox = Math.max(0, (x0 - all[0]) / OVER); ox <= Math.min(overW - 1, (x1 - all[0]) / OVER); ox++) {
				overArgb[oz * overW + ox] = average(all[0] + ox * OVER, all[1] + oz * OVER);
			}
		}
		overUploaded = false;
	}

	public static Identifier texture(Region r) {
		return r.uploaded ? r.texture : null;
	}

	// ---------- keeping up with the world ----------

	/** A chunk has come in near you: looked at (a few a tick) for anything that's changed since the map was made. */
	public static void chunkLoaded(LevelChunk chunk) {
		if (!Adventure.inSave) return;
		SCAN.add(new long[] {chunk.getPos().x, chunk.getPos().z});
	}

	public static void tick(Minecraft mc) {
		if (mc.level == null || !Adventure.inSave) {
			SCAN.clear();
			return;
		}
		// The map's read and painted in the background from the start, so changes are caught from the start too.
		if (!ready()) return;
		for (int n = 0; n < 24 && !SCAN.isEmpty(); n++) {
			long[] c = SCAN.poll();
			if (mc.level.getChunkSource().hasChunk((int) c[0], (int) c[1])) scan(mc.level, mc.level.getChunk((int) c[0], (int) c[1]));
		}
	}

	private static void scan(ClientLevel level, LevelChunk chunk) {
		int cx = chunk.getPos().x, cz = chunk.getPos().z;
		Region r = region(Math.floorDiv(cx * 16, R), Math.floorDiv(cz * 16, R), false);
		String[] names = new String[256];
		short[] ys = new short[256];
		byte[] ds = new byte[256];
		boolean changed = false;
		BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
		for (int lz = 0; lz < 16; lz++) {
			for (int lx = 0; lx < 16; lx++) {
				int x = cx * 16 + lx, z = cz * 16 + lz;
				int top = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, lx, lz);
				String name = "minecraft:air";
				int y = top;
				for (int k = 0; k < 8 && y >= level.getMinY(); k++, y--) {
					BlockState st = chunk.getBlockState(at.set(x, y, z));
					if (st.isAir()) continue;
					String n = BuiltInRegistries.BLOCK.getKey(st.getBlock()).toString();
					if (seeThrough(n)) continue;
					if (st.hasProperty(BlockStateProperties.SNOWY) && st.getValue(BlockStateProperties.SNOWY)) n += "[snowy]";
					name = n;
					break;
				}
				int depth = 0;
				if (name.equals("minecraft:water")) {
					for (int k = 0; k < 24; k++) {
						if (!chunk.getBlockState(at.set(x, y - k, z)).is(Blocks.WATER)) break;
						depth = k + 1;
					}
				}
				int i = lz * 16 + lx;
				names[i] = name;
				ys[i] = (short) (name.equals("minecraft:air") ? -64 : y);
				ds[i] = (byte) depth;
				if (r == null) {
					if (!name.equals("minecraft:air") && !name.equals("minecraft:water")) changed = true;
					continue;
				}
				int ri = Math.floorMod(z, R) * R + Math.floorMod(x, R);
				if (!r.names.get(r.id[ri]).equals(name) || r.y[ri] != ys[i] || (name.equals("minecraft:water") && r.depth[ri] != depth)) changed = true;
			}
		}
		if (!changed) return;
		EDITS.put(key(cx, cz), new Edit(names, ys, ds));
		apply(cx, cz, names, ys, ds);
	}

	private static void apply(int cx, int cz, String[] names, short[] ys, byte[] ds) {
		Region r = region(Math.floorDiv(cx * 16, R), Math.floorDiv(cz * 16, R), true);
		for (int lz = 0; lz < 16; lz++) {
			for (int lx = 0; lx < 16; lx++) {
				int i = lz * 16 + lx, ri = Math.floorMod(cz * 16 + lz, R) * R + Math.floorMod(cx * 16 + lx, R);
				r.id[ri] = (short) r.name(names[i]);
				r.y[ri] = ys[i];
				r.depth[ri] = ds[i];
			}
		}
		// Paint it again (and a little round it, for the shadows and the shore), next time the map's shown.
		int x0 = Math.floorMod(cx * 16, R) - 1, z0 = Math.floorMod(cz * 16, R) - 1;
		int[] box = {Math.max(0, x0), Math.max(0, z0), Math.min(R, x0 + 22), Math.min(R, z0 + 22)};
		if (r.stale != null) box = new int[] {Math.min(box[0], r.stale[0]), Math.min(box[1], r.stale[1]), Math.max(box[2], r.stale[2]), Math.max(box[3], r.stale[3])};
		r.stale = box;
		if (r.argb == null && painting != null && painting.isDone()) r.argb = new int[R * R];
	}

	private static Path editsFile() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.getSingleplayerServer() == null) return null;
		try {
			return mc.getSingleplayerServer().getWorldPath(LevelResource.ROOT).resolve("retromon_townmap.dat");
		} catch (Exception e) {
			return null;
		}
	}

	private static void loadEdits() {
		Path f = editsFile();
		if (f == null || f.equals(editsLoadedFrom)) return;
		editsLoadedFrom = f;
		if (!Files.exists(f)) return;
		try (DataInputStream in = new DataInputStream(new java.io.BufferedInputStream(new GZIPInputStream(Files.newInputStream(f))))) {
			int n = in.readInt();
			for (int k = 0; k < n; k++) {
				int cx = in.readInt(), cz = in.readInt();
				String[] names = new String[256];
				short[] ys = new short[256];
				byte[] ds = new byte[256];
				for (int i = 0; i < 256; i++) names[i] = in.readUTF();
				for (int i = 0; i < 256; i++) ys[i] = in.readShort();
				in.readFully(ds);
				EDITS.put(key(cx, cz), new Edit(names, ys, ds));
				apply(cx, cz, names, ys, ds);
			}
		} catch (Exception e) {
			Retromon.LOG.warn("Couldn't read the Town Map's changes", e);
		}
	}

	/** Leaving the game: what's changed is kept in the save, and the map's let go of (another save may be different). */
	public static void leaving() {
		Path f = editsFile();
		if (f != null && !EDITS.isEmpty() && loaded) {
			try (DataOutputStream out = new DataOutputStream(new java.io.BufferedOutputStream(new GZIPOutputStream(Files.newOutputStream(f))))) {
				out.writeInt(EDITS.size());
				for (var e : EDITS.entrySet()) {
					long k = e.getKey();
					out.writeInt((int) (k >> 32));
					out.writeInt((int) k);
					for (String n : e.getValue().names()) out.writeUTF(n);
					for (short v : e.getValue().y()) out.writeShort(v);
					out.write(e.getValue().depth());
				}
			} catch (Exception e) {
				Retromon.LOG.warn("Couldn't keep the Town Map's changes", e);
			}
		}
		Minecraft mc = Minecraft.getInstance();
		mc.execute(() -> {
			for (Region r : REGIONS.values()) if (r.texture != null) mc.getTextureManager().release(r.texture);
			if (overImage != null) mc.getTextureManager().release(OVERVIEW);
			REGIONS.clear();
			EDITS.clear();
			SCAN.clear();
			overArgb = null;
			overImage = null;
			loaded = false;
			painting = null;
			editsLoadedFrom = null;
		});
	}
}
