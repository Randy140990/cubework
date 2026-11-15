package com.insidethecube.orbitarena.client;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.OptionalDouble;
import java.util.OptionalInt;

import javax.imageio.ImageIO;

import com.insidethecube.orbitarena.OrbitArena;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.slf4j.Logger;

/**
 * The arena's sky. Behind everything, the real night sky (NASA's Deep Star Maps, made from the Gaia and
 * Hipparcos catalogues) on a sphere round you, turning once every 20 minutes about a tilted axis. In front
 * of it, close, a ringed giant like Saturn: banded cloud belts, lit from one side by a sun you can't see,
 * and its rings. The arena floats in those rings: their plane runs just under you, so they sweep out from
 * under your feet, across the planet's face and round behind it. The planet casts its shadow on the rings
 * and the rings theirs on the planet. The arena dimension uses the End's kind of sky, and this is drawn in
 * place of the End's textured box (see mixin.SkyRendererMixin).
 * <p>
 * The planet's bands and the rings are made in code; the stars are a big picture. All are made or read in
 * the background when the game starts and put on the graphics card in a client tick (until then, a second
 * or two after the game starts, space is plain black).
 */
public final class ArenaSky {
	private ArenaSky() {}

	private static final Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
	private static final String DIR = "/assets/orbitarena/textures/environment/";
	private static final int LON = 64, LAT = 32, PLON = 160, PLAT = 80, RING_SEG = 360, RING_STEPS = 48;
	/** One full turn every 20 minutes (24,000 ticks). */
	private static final double TURN_TICKS = 24000.0;
	private static final Vector3f AXIS = new Vector3f(0.25F, 1.0F, 0.35F).normalize();

	/**
	 * Where the planet hangs: east by north-east, up over the far temple, about 28 degrees across (radius),
	 * in sky units (the stars are 100 out). The rings' plane is HEIGHT under you, tipped ROLL degrees.
	 */
	private static final float PLANET_AZIMUTH = 30, PLANET_ELEVATION = 17, PLANET_RADIUS_DEG = 28.4F, PLANET_DISTANCE = 50, HEIGHT = 5, ROLL = 14;
	/** The rings from their inner edge to their outer, in the planet's radii (as Saturn's: C, B, Cassini, A, F). */
	private static final float RING_IN = 1.24F, RING_OUT = 2.36F;
	/** Pre-blurred copies of the rings for further off (where a pixel spans more of them). */
	private static final int RING_LEVELS = 4, RING_W = 1024, RING_H = 64;

	/**
	 * Everything is drawn with the End sky's own way of drawing (a picture, tinted and faded per corner,
	 * blended over what's behind), so shader packs treat the planet as they treat the sky.
	 */
	private static final RenderPipeline SKY = RenderPipelines.END_SKY;

	private static GpuBuffer sky, planet, glow;
	private static final GpuBuffer[] ringBack = new GpuBuffer[RING_LEVELS], ringFront = new GpuBuffer[RING_LEVELS];
	private static final int[] ringBackIndices = new int[RING_LEVELS], ringFrontIndices = new int[RING_LEVELS];
	private static int skyIndices, planetIndices, glowIndices;
	/** The pictures once made or read (stars, the planet, the rings at each level), and on the card. */
	private static volatile NativeImage[] read;
	private static DynamicTexture[] textures;
	/** Plain white, for the glow (its colour and fade are in its corners). */
	private static DynamicTexture white;

	/** Starts reading and making the sky's pictures (in the background). */
	public static void load() {
		Thread t = new Thread(() -> {
			try {
				NativeImage[] out = new NativeImage[2 + RING_LEVELS];
				out[0] = image("stars.jpg");
				out[1] = planetMap(2048, 1024);
				for (int i = 0; i < RING_LEVELS; i++) out[2 + i] = ringMap(i);
				read = out;
			} catch (Throwable e) {
				LOGGER.warn("Orbit Arena: couldn't make the sky's pictures", e);
			}
		}, "Orbit Arena sky");
		t.setDaemon(true);
		t.start();
	}

	/** A picture from the mod (a JPEG), as an image for the card. */
	private static NativeImage image(String name) throws Exception {
		try (InputStream in = ArenaSky.class.getResourceAsStream(DIR + name)) {
			if (in == null) return null;
			BufferedImage img = ImageIO.read(in);
			if (img == null) return null;
			int w = img.getWidth(), h = img.getHeight();
			NativeImage out = new NativeImage(w, h, false);
			int[] row = new int[w];
			for (int y = 0; y < h; y++) {
				img.getRGB(0, y, w, 1, row, 0, w);
				for (int x = 0; x < w; x++) out.setPixel(x, y, row[x] | 0xFF000000);
			}
			return out;
		}
	}

	public static boolean active() {
		Minecraft mc = Minecraft.getInstance();
		return mc.level != null && mc.level.dimension() == OrbitArena.ARENA;
	}

	// ---------------------------------------------------------------- noise

	private static double hash(long x, long y, long z, long salt) {
		long h = x * 0x9E3779B97F4A7C15L ^ y * 0xC2B2AE3D27D4EB4FL ^ z * 0x165667B19E3779F9L ^ salt * 0x27D4EB2F165667C5L;
		h ^= h >>> 31;
		h *= 0x7FB5D329728EA185L;
		h ^= h >>> 27;
		h *= 0x81DADEF4BC2DD44DL;
		h ^= h >>> 33;
		return (h >>> 11) * 0x1.0p-53;
	}

	private static double noise(double x, double y, double z, long salt) {
		long x0 = (long) Math.floor(x), y0 = (long) Math.floor(y), z0 = (long) Math.floor(z);
		double fx = x - x0, fy = y - y0, fz = z - z0;
		fx = fx * fx * (3 - 2 * fx);
		fy = fy * fy * (3 - 2 * fy);
		fz = fz * fz * (3 - 2 * fz);
		double a = lerp(fx, hash(x0, y0, z0, salt), hash(x0 + 1, y0, z0, salt)), b = lerp(fx, hash(x0, y0 + 1, z0, salt), hash(x0 + 1, y0 + 1, z0, salt));
		double c = lerp(fx, hash(x0, y0, z0 + 1, salt), hash(x0 + 1, y0, z0 + 1, salt)), d = lerp(fx, hash(x0, y0 + 1, z0 + 1, salt), hash(x0 + 1, y0 + 1, z0 + 1, salt));
		return lerp(fz, lerp(fy, a, b), lerp(fy, c, d));
	}

	private static double fbm(double x, double y, double z, int octaves, long salt) {
		double sum = 0, amp = 0.5, norm = 0;
		for (int i = 0; i < octaves; i++) {
			sum += noise(x, y, z, salt + i) * amp;
			norm += amp;
			x *= 2.03;
			y *= 2.03;
			z *= 2.03;
			amp *= 0.5;
		}
		return sum / norm;
	}

	private static double lerp(double t, double a, double b) {
		return a + (b - a) * t;
	}

	// ---------------------------------------------------------------- the planet's bands

	/** The belts and zones by latitude (degrees, north +), Saturn's pale golds and tans, blue-grey at the poles. */
	private static final double[] BAND_LAT = {-90, -72, -60, -50, -42, -33, -25, -16, -8, 0, 7, 15, 23, 31, 40, 49, 58, 70, 90};
	private static final int[][] BAND_RGB = {{118, 132, 150}, {140, 150, 160}, {176, 162, 136}, {206, 188, 152}, {182, 150, 108}, {224, 204, 164},
			{196, 160, 112}, {226, 206, 160}, {214, 186, 136}, {240, 226, 186}, {222, 196, 146}, {200, 164, 114}, {230, 210, 168}, {188, 154, 110},
			{214, 194, 156}, {178, 162, 136}, {160, 160, 158}, {138, 148, 160}, {116, 130, 148}};

	private static double[] band(double lat) {
		int i = 0;
		while (i < BAND_LAT.length - 2 && lat > BAND_LAT[i + 1]) i++;
		double t = (lat - BAND_LAT[i]) / (BAND_LAT[i + 1] - BAND_LAT[i]);
		t = Math.max(0, Math.min(1, t));
		t = t * t * (3 - 2 * t);
		double[] c = new double[3];
		for (int k = 0; k < 3; k++) c[k] = BAND_RGB[i][k] + (BAND_RGB[i + 1][k] - BAND_RGB[i][k]) * t;
		return c;
	}

	/** The planet's map (longitude across, latitude down): bands, wavering and streaked, fine stripes in them, a storm or two. */
	private static NativeImage planetMap(int w, int h) {
		NativeImage out = new NativeImage(w, h, false);
		for (int y = 0; y < h; y++) {
			double lat = 90 - (y + 0.5) / h * 180, la = Math.toRadians(lat);
			for (int x = 0; x < w; x++) {
				double lon = (x + 0.5) / w * Math.PI * 2;
				// Round the planet in 3D, so the map joins up all the way round.
				double px = Math.cos(la) * Math.cos(lon), py = Math.sin(la), pz = Math.cos(la) * Math.sin(lon);
				// Bands wander: more where the winds are strong, streaked out along the lines of latitude.
				double turb = fbm(px * 3.2, py * 22, pz * 3.2, 5, 11) - 0.5;
				double l = lat + turb * 5.5 + (fbm(px * 9, py * 60, pz * 9, 3, 21) - 0.5) * 1.6;
				double[] c = band(l);
				// Fine stripes within the bands, and a soft mottling.
				double stripe = Math.sin(l * 1.9 + turb * 4) * 0.035 + Math.sin(l * 5.3) * 0.02;
				double mottle = (fbm(px * 14, py * 30, pz * 14, 3, 31) - 0.5) * 0.08;
				double k = 1 + stripe + mottle;
				// A pale oval storm in the southern belts, and a smaller one further north.
				k += storm(lat, lon, -27, 1.1, 5.5, 16, 0.16) + storm(lat, lon, 36, 5.6, 3, 8, 0.1);
				int r = clamp(c[0] * k), g = clamp(c[1] * k), b = clamp(c[2] * k * 0.99);
				out.setPixel(x, y, 0xFF000000 | r << 16 | g << 8 | b);
			}
		}
		return out;
	}

	private static double storm(double lat, double lon, double sLat, double sLon, double halfLat, double halfLonDeg, double strength) {
		double dl = Math.toDegrees(Math.atan2(Math.sin(lon - sLon), Math.cos(lon - sLon)));
		double e = (lat - sLat) * (lat - sLat) / (halfLat * halfLat) + dl * dl / (halfLonDeg * halfLonDeg);
		return e >= 1 ? 0 : strength * (1 - e) * (1 - e);
	}

	private static int clamp(double v) {
		return (int) Math.max(0, Math.min(255, Math.round(v)));
	}

	// ---------------------------------------------------------------- the rings

	/** How solid the rings are at x planet radii out (0 to 1): the faint C ring, bright B, the Cassini gap, A with the Encke gap, F. */
	private static double ringOpacity(double x) {
		double o;
		if (x < RING_IN || x > RING_OUT) return 0;
		else if (x < 1.53) o = 0.10 + 0.08 * smooth(1.24, 1.53, x);
		else if (x < 1.95) o = 0.62 + 0.3 * Math.sin((x - 1.53) / 0.42 * Math.PI) + 0.06 * smooth(1.53, 1.6, x);
		else if (x < 2.03) o = 0.05 + 0.04 * smooth(1.99, 2.03, x);
		else if (x < 2.27) o = 0.58 - 0.12 * smooth(2.03, 2.27, x) - (Math.abs(x - 2.215) < 0.007 ? 0.45 : 0);
		else if (x < 2.322) o = 0.02;
		else if (x < 2.338) o = 0.5;
		else o = 0;
		// Ringlets: fine bands within.
		o *= 0.8 + 0.2 * fbm(x * 180, 0.5, 0.5, 3, 41) + 0.1 * (fbm(x * 40, 3.5, 0.5, 2, 43) - 0.5);
		return Math.max(0, Math.min(1, o));
	}

	private static double smooth(double a, double b, double x) {
		double t = Math.max(0, Math.min(1, (x - a) / (b - a)));
		return t * t * (3 - 2 * t);
	}

	/** The rings' colour at x radii out: grey-brown inside, cream in the B ring, paler grey-gold out in A. */
	private static double[] ringColour(double x) {
		if (x < 1.53) return new double[] {168, 156, 142};
		if (x < 1.95) return new double[] {238, 222, 188};
		if (x < 2.03) return new double[] {150, 140, 128};
		return new double[] {214, 204, 184};
	}

	/**
	 * The rings' picture: in from left to right, round from top to bottom (with a faint clumping as you go
	 * round), blurred across by level (each level's for further off, so far rings don't flicker).
	 */
	private static NativeImage ringMap(int level) {
		int blur = level == 0 ? 0 : 1 << (2 * level - 1);
		double[] o = new double[RING_W], r = new double[RING_W], g = new double[RING_W], b = new double[RING_W];
		for (int i = 0; i < RING_W; i++) {
			double x = RING_IN + (i + 0.5) / RING_W * (RING_OUT - RING_IN);
			o[i] = ringOpacity(x);
			double[] c = ringColour(x);
			r[i] = c[0];
			g[i] = c[1];
			b[i] = c[2];
		}
		if (blur > 0) {
			o = box(o, blur);
			r = box(r, blur);
			g = box(g, blur);
			b = box(b, blur);
		}
		NativeImage out = new NativeImage(RING_W, RING_H, false);
		for (int y = 0; y < RING_H; y++) {
			double a = y / (double) RING_H * Math.PI * 2;
			for (int i = 0; i < RING_W; i++) {
				double clump = 1 + (fbm(Math.cos(a) * 3 + i * 0.02, Math.sin(a) * 3, i * 0.05, 2, 51) - 0.5) * 0.14 / (1 + level);
				int al = clamp(o[i] * 255 * clump);
				out.setPixel(i, y, al << 24 | clamp(r[i] * clump) << 16 | clamp(g[i] * clump) << 8 | clamp(b[i] * clump));
			}
		}
		return out;
	}

	private static double[] box(double[] v, int radius) {
		double[] out = new double[v.length];
		for (int i = 0; i < v.length; i++) {
			double s = 0;
			int n = 0;
			for (int k = -radius; k <= radius; k++) {
				int j = i + k;
				if (j < 0 || j >= v.length) continue;
				s += v[j];
				n++;
			}
			out[i] = s / n;
		}
		return out;
	}

	// ---------------------------------------------------------------- the star sphere

	private static float[] point(int i, int j) {
		double lon = i / (double) LON * Math.PI * 2, lat = j / (double) LAT * Math.PI;
		float r = 100;
		return new float[] {(float) (Math.sin(lat) * Math.cos(lon) * r), (float) (Math.cos(lat) * r), (float) (Math.sin(lat) * Math.sin(lon) * r)};
	}

	private static void buildSky() {
		int quads = LON * LAT;
		try (ByteBufferBuilder bytes = ByteBufferBuilder.exactlySized(quads * 4 * DefaultVertexFormat.POSITION_TEX_COLOR.getVertexSize())) {
			BufferBuilder b = new BufferBuilder(bytes, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
			for (int j = 0; j < LAT; j++) {
				for (int i = 0; i < LON; i++) {
					int[][] corners = {{i, j}, {i, j + 1}, {i + 1, j + 1}, {i + 1, j}};
					float[][] p = new float[4][];
					for (int k = 0; k < 4; k++) p[k] = point(corners[k][0], corners[k][1]);
					// Seen from inside (as the End's sky box is): reverse the corners if they face out.
					int[] order = facing(p, new float[] {0, 0, 0}, false) ? new int[] {0, 1, 2, 3} : new int[] {0, 3, 2, 1};
					for (int k : order) {
						b.addVertex(p[k][0], p[k][1], p[k][2]).setUv(corners[k][0] / (float) LON, corners[k][1] / (float) LAT).setColor(-1);
					}
				}
			}
			try (MeshData mesh = b.buildOrThrow()) {
				sky = RenderSystem.getDevice().createBuffer(() -> "Orbit Arena sky", 40, mesh.vertexBuffer());
			}
		}
		skyIndices = quads * 6;
	}

	/**
	 * Whether a quad (four corners, in order) faces the way wanted: out from (or, for out = false, in
	 * towards) a centre.
	 */
	private static boolean facing(float[][] p, float[] centre, boolean out) {
		float ax = p[1][0] - p[0][0], ay = p[1][1] - p[0][1], az = p[1][2] - p[0][2];
		float bx = p[2][0] - p[0][0], by = p[2][1] - p[0][1], bz = p[2][2] - p[0][2];
		if (ax * ax + ay * ay + az * az < 1e-9F) {
			ax = p[2][0] - p[1][0];
			ay = p[2][1] - p[1][1];
			az = p[2][2] - p[1][2];
			bx = p[3][0] - p[1][0];
			by = p[3][1] - p[1][1];
			bz = p[3][2] - p[1][2];
		}
		float nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx;
		float cx = (p[0][0] + p[2][0]) / 2 - centre[0], cy = (p[0][1] + p[2][1]) / 2 - centre[1], cz = (p[0][2] + p[2][2]) / 2 - centre[2];
		boolean outward = nx * cx + ny * cy + nz * cz > 0;
		return outward == out;
	}

	// ---------------------------------------------------------------- the planet and its rings

	private static Vector3f v(double x, double y, double z) {
		return new Vector3f((float) x, (float) y, (float) z);
	}

	private static float smoothf(float a, float b, float x) {
		float t = Math.max(0, Math.min(1, (x - a) / (b - a)));
		return t * t * (3 - 2 * t);
	}

	/** Where everything is: the planet's centre and radius, its pole (square to the rings), two lines in their plane, and the sun. */
	private record Layout(Vector3f centre, float radius, Vector3f pole, Vector3f a, Vector3f b, Vector3f sun, Vector3f right, Vector3f upOnDisc) {}

	private static Layout layout() {
		double az = Math.toRadians(PLANET_AZIMUTH), el = Math.toRadians(PLANET_ELEVATION);
		// Towards the planet: east (+x), turned north (-z) by the azimuth, raised by the elevation.
		Vector3f dir = v(Math.cos(el) * Math.cos(az), Math.sin(el), -Math.cos(el) * Math.sin(az)).normalize();
		Vector3f centre = new Vector3f(dir).mul(PLANET_DISTANCE);
		float radius = (float) (PLANET_DISTANCE * Math.sin(Math.toRadians(PLANET_RADIUS_DEG)));
		Vector3f toViewer = new Vector3f(dir).negate();
		Vector3f right = new Vector3f(dir).cross(new Vector3f(0, 1, 0)).normalize();
		Vector3f upOnDisc = new Vector3f(right).cross(dir).normalize();
		// The rings' plane: through the planet, tipped ROLL degrees about the line to it, HEIGHT under you.
		double roll = Math.toRadians(ROLL);
		Vector3f tipped = new Vector3f(upOnDisc).mul((float) Math.cos(roll)).add(new Vector3f(right).mul((float) Math.sin(roll)));
		float s = -HEIGHT / PLANET_DISTANCE;
		Vector3f pole = new Vector3f(tipped).mul((float) Math.sqrt(1 - s * s)).add(new Vector3f(dir).mul(s)).normalize();
		Vector3f a = new Vector3f(toViewer).sub(new Vector3f(pole).mul(toViewer.dot(pole))).normalize();
		Vector3f b = new Vector3f(pole).cross(a).normalize();
		// The sun: off to the right, a little behind you and above the rings (so you see their lit side).
		Vector3f sun = new Vector3f(toViewer).mul(0.42F).add(new Vector3f(right).mul(0.8F)).add(new Vector3f(pole).mul(0.42F)).normalize();
		return new Layout(centre, radius, pole, a, b, sun, right, upOnDisc);
	}

	/** How much of the sun reaches a point of the planet through the rings (1: none in the way). */
	private static float ringShadow(Layout L, Vector3f p) {
		Vector3f rel = new Vector3f(p).sub(L.centre());
		float h = rel.dot(L.pole()), sh = L.sun().dot(L.pole());
		if (h * sh >= 0 || Math.abs(sh) < 1e-4F) return 1;
		float t = -h / sh;
		Vector3f q = new Vector3f(rel).add(new Vector3f(L.sun()).mul(t));
		double x = q.length() / L.radius();
		return (float) (1 - ringOpacity(x) * 0.8);
	}

	/** How much of the sun reaches a point of the rings past the planet (0 in its shadow). */
	private static float planetShadow(Layout L, Vector3f q) {
		Vector3f w = new Vector3f(L.centre()).sub(q);
		float along = w.dot(L.sun());
		if (along <= 0) return 1;
		float d = (float) Math.sqrt(Math.max(0, w.lengthSquared() - along * along));
		return 0.06F + 0.94F * smoothf(L.radius() * 0.985F, L.radius() * 1.03F, d);
	}

	private static void buildPlanet() {
		Layout L = layout();
		Vector3f centre = L.centre();
		float radius = L.radius();
		Vector3f toViewer = new Vector3f(centre).normalize().negate();
		int quads = PLON * PLAT;
		try (ByteBufferBuilder bytes = ByteBufferBuilder.exactlySized(quads * 4 * DefaultVertexFormat.POSITION_TEX_COLOR.getVertexSize())) {
			BufferBuilder bb = new BufferBuilder(bytes, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
			float[] c = {centre.x, centre.y, centre.z};
			for (int j = 0; j < PLAT; j++) {
				for (int i = 0; i < PLON; i++) {
					int[][] corners = {{i, j}, {i, j + 1}, {i + 1, j + 1}, {i + 1, j}};
					float[][] p = new float[4][];
					Vector3f[] n = new Vector3f[4];
					for (int k = 0; k < 4; k++) {
						double lat = corners[k][1] / (double) PLAT * Math.PI, lon = corners[k][0] / (double) PLON * Math.PI * 2;
						n[k] = new Vector3f(L.a()).mul((float) (Math.sin(lat) * Math.cos(lon))).add(new Vector3f(L.pole()).mul((float) Math.cos(lat)))
								.add(new Vector3f(L.b()).mul((float) (Math.sin(lat) * Math.sin(lon))));
						p[k] = new float[] {centre.x + n[k].x * radius, centre.y + n[k].y * radius, centre.z + n[k].z * radius};
					}
					int[] order = facing(p, c, true) ? new int[] {0, 1, 2, 3} : new int[] {0, 3, 2, 1};
					for (int k : order) {
						float l = n[k].dot(L.sun());
						float lit = smoothf(-0.06F, 0.42F, l) * ringShadow(L, new Vector3f(p[k][0], p[k][1], p[k][2]));
						// Darker towards its edge (seen through more haze), and a faint glow from the lit rings on the night side.
						float mu = Math.max(0, n[k].dot(toViewer));
						float limb = 0.72F + 0.28F * (float) Math.sqrt(mu);
						float ringshine = 0.035F * (1 - Math.abs(n[k].dot(L.pole())));
						float shade = Math.min(1, (0.006F + ringshine + 0.994F * lit) * limb);
						int g = (int) (255 * shade);
						bb.addVertex(p[k][0], p[k][1], p[k][2]).setUv(corners[k][0] / (float) PLON, corners[k][1] / (float) PLAT).setColor(g, g, g, 255);
					}
				}
			}
			try (MeshData mesh = bb.buildOrThrow()) {
				planet = RenderSystem.getDevice().createBuffer(() -> "Orbit Arena planet", 40, mesh.vertexBuffer());
			}
		}
		planetIndices = quads * 6;

		// A thin haze round its edge, warm, brightest on the lit side.
		int seg = 128;
		float[] radii = {radius * 0.97F, radius * 0.998F, radius * 1.012F, radius * 1.07F};
		float[] alpha = {0, 0.32F, 0.2F, 0};
		try (ByteBufferBuilder bytes = ByteBufferBuilder.exactlySized(seg * 3 * 4 * DefaultVertexFormat.POSITION_TEX_COLOR.getVertexSize())) {
			BufferBuilder bb = new BufferBuilder(bytes, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
			for (int i = 0; i < seg; i++) {
				for (int band = 0; band < 3; band++) {
					double a0 = Math.PI * 2 * i / seg, a1 = Math.PI * 2 * (i + 1) / seg;
					double[] as = {a0, a1, a1, a0};
					int[] rs = {band, band, band + 1, band + 1};
					float[][] p = new float[4][];
					int[] al = new int[4];
					for (int k = 0; k < 4; k++) {
						Vector3f m = new Vector3f(L.right()).mul((float) Math.cos(as[k])).add(new Vector3f(L.upOnDisc()).mul((float) Math.sin(as[k])));
						float lit = 0.05F + 0.95F * smoothf(-0.3F, 0.7F, m.dot(L.sun()));
						Vector3f pos = new Vector3f(m).mul(radii[rs[k]]).add(centre);
						p[k] = new float[] {pos.x, pos.y, pos.z};
						al[k] = (int) (255 * alpha[rs[k]] * lit);
					}
					int[] order = facing(p, new float[] {0, 0, 0}, false) ? new int[] {0, 1, 2, 3} : new int[] {0, 3, 2, 1};
					for (int k : order) bb.addVertex(p[k][0], p[k][1], p[k][2]).setUv(0.5F, 0.5F).setColor(255, 222, 176, al[k]);
				}
			}
			try (MeshData mesh = bb.buildOrThrow()) {
				glow = RenderSystem.getDevice().createBuffer(() -> "Orbit Arena planet haze", 40, mesh.vertexBuffer());
			}
		}
		glowIndices = seg * 3 * 6;

		buildRings(L);
	}

	/**
	 * The rings: an annulus in their plane, in quads, each drawn with the picture blurred to suit how far
	 * off it is (and how slantwise you see it), split into the part behind the planet (drawn before it) and in
	 * front (after). Each corner shaded by the planet's shadow.
	 */
	private static void buildRings(Layout L) {
		Vector3f centre = L.centre();
		float dist = centre.length(), rIn = L.radius() * RING_IN, rOut = L.radius() * RING_OUT;
		int vs = DefaultVertexFormat.POSITION_TEX_COLOR.getVertexSize(), quads = RING_SEG * RING_STEPS;
		ByteBufferBuilder[] backBytes = new ByteBufferBuilder[RING_LEVELS], frontBytes = new ByteBufferBuilder[RING_LEVELS];
		BufferBuilder[] back = new BufferBuilder[RING_LEVELS], front = new BufferBuilder[RING_LEVELS];
		int[] backQuads = new int[RING_LEVELS], frontQuads = new int[RING_LEVELS];
		try {
			for (int l = 0; l < RING_LEVELS; l++) {
				backBytes[l] = ByteBufferBuilder.exactlySized(quads * 4 * vs);
				frontBytes[l] = ByteBufferBuilder.exactlySized(quads * 4 * vs);
				back[l] = new BufferBuilder(backBytes[l], VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
				front[l] = new BufferBuilder(frontBytes[l], VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
			}
			float above = Math.abs(new Vector3f(centre).negate().dot(L.pole()));
			for (int j = 0; j < RING_SEG; j++) {
				for (int i = 0; i < RING_STEPS; i++) {
					float[] rr = {i, i + 1, i + 1, i}, aa = {j, j, j + 1, j + 1};
					float[][] p = new float[4][];
					float[] u = new float[4], vv = new float[4], sh = new float[4];
					Vector3f mid = new Vector3f();
					for (int k = 0; k < 4; k++) {
						float r = rIn + (rOut - rIn) * rr[k] / RING_STEPS;
						double a = Math.PI * 2 * aa[k] / RING_SEG;
						Vector3f q = new Vector3f(L.a()).mul((float) Math.cos(a)).add(new Vector3f(L.b()).mul((float) Math.sin(a))).mul(r).add(centre);
						p[k] = new float[] {q.x, q.y, q.z};
						u[k] = rr[k] / RING_STEPS;
						vv[k] = aa[k] / RING_SEG * 6;
						sh[k] = planetShadow(L, q);
						mid.add(new Vector3f(q).mul(0.25F));
					}
					// How many of the rings' texels one pixel spans here: far off and slantwise, many.
					float d = mid.length();
					float span = (RING_W / (rOut - rIn)) * d * d / (above * 800);
					int level = span < 2 ? 0 : span < 8 ? 1 : span < 32 ? 2 : 3;
					boolean inFront = d < dist;
					BufferBuilder b = inFront ? front[level] : back[level];
					if (inFront) frontQuads[level]++;
					else backQuads[level]++;
					int[] order = facing(p, new float[] {0, 0, 0}, false) ? new int[] {0, 1, 2, 3} : new int[] {0, 3, 2, 1};
					for (int k : order) {
						int g = (int) (255 * (0.9F * sh[k] + 0.03F));
						b.addVertex(p[k][0], p[k][1], p[k][2]).setUv(u[k], vv[k]).setColor(g, g, g, 255);
					}
				}
			}
			for (int l = 0; l < RING_LEVELS; l++) {
				final int lv = l;
				MeshData bm = back[l].build(), fm = front[l].build();
				if (bm != null) {
					ringBack[l] = RenderSystem.getDevice().createBuffer(() -> "Orbit Arena rings behind " + lv, 40, bm.vertexBuffer());
					ringBackIndices[l] = backQuads[l] * 6;
					bm.close();
				}
				if (fm != null) {
					ringFront[l] = RenderSystem.getDevice().createBuffer(() -> "Orbit Arena rings in front " + lv, 40, fm.vertexBuffer());
					ringFrontIndices[l] = frontQuads[l] * 6;
					fm.close();
				}
			}
		} finally {
			for (int l = 0; l < RING_LEVELS; l++) {
				if (backBytes[l] != null) backBytes[l].close();
				if (frontBytes[l] != null) frontBytes[l].close();
			}
		}
	}

	// ---------------------------------------------------------------- drawing

	/** The pictures onto the card, once they've been made (each tick until then; never mid-frame). */
	public static void upload(Minecraft mc) {
		NativeImage[] r = read;
		if (r == null || textures != null) return;
		textures = new DynamicTexture[r.length];
		white = new DynamicTexture(() -> "Orbit Arena white", 1, 1, false);
		if (white.getPixels() != null) {
			white.getPixels().setPixel(0, 0, 0xFFFFFFFF);
			white.upload();
		}
		for (int i = 0; i < r.length; i++) {
			if (r[i] == null) continue;
			final String n = i == 0 ? "stars" : i == 1 ? "planet" : "rings " + (i - 2);
			// The rings stop at their edges (and repeat round); the stars and the planet wrap round.
			textures[i] = new Smooth(() -> "Orbit Arena " + n, r[i], i >= 2 ? AddressMode.CLAMP_TO_EDGE : AddressMode.REPEAT);
		}
	}

	/** A picture on the card, drawn smoothly (not in hard pixels). */
	private static final class Smooth extends DynamicTexture {
		Smooth(java.util.function.Supplier<String> label, NativeImage image, AddressMode across) {
			super(label, image);
			this.sampler = RenderSystem.getSamplerCache().getSampler(across, AddressMode.REPEAT, FilterMode.LINEAR, FilterMode.LINEAR, false);
		}
	}

	/** Draws the sky (called in place of the End's sky box). */
	public static void render() {
		Minecraft mc = Minecraft.getInstance();
		if (sky == null) buildSky();
		DynamicTexture stars = textures == null ? null : textures[0];
		GpuTextureView colour = mc.getMainRenderTarget().getColorTextureView(), depth = mc.getMainRenderTarget().getDepthTextureView();
		double time = mc.level.getGameTime() + mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
		float angle = (float) ((time / TURN_TICKS) % 1.0 * Math.PI * 2);
		Matrix4f still = new Matrix4f(RenderSystem.getModelViewMatrix());
		Matrix4f turning = new Matrix4f(still).rotate(angle, AXIS);
		if (stars != null) draw(SKY, sky, skyIndices, turning, stars, colour, depth);

		if (textures == null || textures[1] == null) return;
		if (planet == null) buildPlanet();
		for (int l = 0; l < RING_LEVELS; l++) if (ringBack[l] != null) draw(SKY, ringBack[l], ringBackIndices[l], still, textures[2 + l], colour, depth);
		draw(SKY, planet, planetIndices, still, textures[1], colour, depth);
		draw(SKY, glow, glowIndices, still, white, colour, depth);
		for (int l = 0; l < RING_LEVELS; l++) if (ringFront[l] != null) draw(SKY, ringFront[l], ringFrontIndices[l], still, textures[2 + l], colour, depth);
	}

	private static void draw(RenderPipeline pipeline, GpuBuffer buffer, int indices, Matrix4f view, AbstractTexture tex, GpuTextureView colour, GpuTextureView depth) {
		if (buffer == null || indices == 0 || tex == null) return;
		RenderSystem.AutoStorageIndexBuffer seq = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
		GpuBuffer ib = seq.getBuffer(indices);
		GpuBufferSlice transform = RenderSystem.getDynamicUniforms().writeTransform(view, new Vector4f(1, 1, 1, 1), new Vector3f(), new Matrix4f());
		try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "Orbit Arena sky", colour, OptionalInt.empty(), depth, OptionalDouble.empty())) {
			pass.setPipeline(pipeline);
			RenderSystem.bindDefaultUniforms(pass);
			pass.setUniform("DynamicTransforms", transform);
			pass.bindTexture("Sampler0", tex.getTextureView(), tex.getSampler());
			pass.setVertexBuffer(0, buffer);
			pass.setIndexBuffer(ib, seq.type());
			pass.drawIndexed(0, 0, indices, 1);
		}
	}
}
