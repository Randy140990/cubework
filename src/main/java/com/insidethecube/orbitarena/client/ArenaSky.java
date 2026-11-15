package com.insidethecube.orbitarena.client;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.OptionalDouble;
import java.util.OptionalInt;

import javax.imageio.ImageIO;

import com.insidethecube.orbitarena.OrbitArena;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
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
 * of it hangs a planet (Earth, from Solar System Scope's maps): a real ball, lit from one side by a sun
 * you can't see, so it has a day side and a night side with the cities' lights, its own clouds, and a glow
 * of atmosphere round its edge. The arena dimension uses the End's kind of sky, and this is drawn in place
 * of the End's textured box (see mixin.SkyRendererMixin).
 * <p>
 * The pictures are big, so they're read in the background when the game starts and put on the graphics
 * card the first time the sky is drawn (until then, a second or two after the game starts, space is plain
 * black).
 */
public final class ArenaSky {
	private ArenaSky() {}

	private static final Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
	private static final String DIR = "/assets/orbitarena/textures/environment/";
	private static final int LON = 64, LAT = 32, PLON = 128, PLAT = 64;
	/** One full turn every 20 minutes (24,000 ticks). */
	private static final double TURN_TICKS = 24000.0;
	private static final Vector3f AXIS = new Vector3f(0.25F, 1.0F, 0.35F).normalize();

	/** Where the planet hangs: east by north-east, low over the horizon, about 23 degrees across (radius). */
	private static final float PLANET_AZIMUTH = 34, PLANET_ELEVATION = 13, PLANET_RADIUS_DEG = 23, PLANET_DISTANCE = 70;

	private static final RenderPipeline PLANET = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
			.withLocation(OrbitArena.id("pipeline/planet"))
			.withVertexShader("core/position_tex_color")
			.withFragmentShader("core/position_tex_color")
			.withSampler("Sampler0")
			.withBlend(BlendFunction.TRANSLUCENT)
			.withDepthWrite(false)
			.withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
			.build();
	private static final RenderPipeline PLANET_LIGHTS = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
			.withLocation(OrbitArena.id("pipeline/planet_lights"))
			.withVertexShader("core/position_tex_color")
			.withFragmentShader("core/position_tex_color")
			.withSampler("Sampler0")
			.withBlend(BlendFunction.LIGHTNING)
			.withDepthWrite(false)
			.withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
			.build();
	private static final RenderPipeline GLOW = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
			.withLocation(OrbitArena.id("pipeline/planet_glow"))
			.withVertexShader("core/position_color")
			.withFragmentShader("core/position_color")
			.withBlend(BlendFunction.LIGHTNING)
			.withCull(false)
			.withDepthWrite(false)
			.withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
			.build();

	private static GpuBuffer sky, day, lights, clouds, glow;
	private static int skyIndices, planetIndices, glowIndices;
	/** The pictures once read (stars, day, night, clouds), and on the card. */
	private static volatile NativeImage[] read;
	private static DynamicTexture[] textures;

	/** Starts reading the sky's pictures (in the background). */
	public static void load() {
		Thread t = new Thread(() -> {
			try {
				NativeImage stars = image("stars.jpg", false), dayMap = image("planet_day.jpg", false), night = image("planet_night.jpg", false),
						cloud = image("planet_clouds.jpg", true);
				read = new NativeImage[] {stars, dayMap, night, cloud};
			} catch (Throwable e) {
				LOGGER.warn("Orbit Arena: couldn't read the sky's pictures", e);
			}
		}, "Orbit Arena sky");
		t.setDaemon(true);
		t.start();
	}

	/** A picture from the mod (a JPEG), as an image for the card; clouds: white, with the picture's brightness as how solid. */
	private static NativeImage image(String name, boolean alpha) throws Exception {
		try (InputStream in = ArenaSky.class.getResourceAsStream(DIR + name)) {
			if (in == null) return null;
			BufferedImage img = ImageIO.read(in);
			if (img == null) return null;
			int w = img.getWidth(), h = img.getHeight();
			NativeImage out = new NativeImage(w, h, false);
			int[] row = new int[w];
			for (int y = 0; y < h; y++) {
				img.getRGB(0, y, w, 1, row, 0, w);
				for (int x = 0; x < w; x++) {
					int c = row[x];
					if (alpha) {
						int lum = ((c >> 16 & 255) * 3 + (c >> 8 & 255) * 4 + (c & 255)) >> 3;
						c = lum << 24 | 0xFFFFFF;
					} else {
						c |= 0xFF000000;
					}
					out.setPixel(x, y, c);
				}
			}
			return out;
		}
	}

	public static boolean active() {
		Minecraft mc = Minecraft.getInstance();
		return mc.level != null && mc.level.dimension() == OrbitArena.ARENA;
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

	// ---------------------------------------------------------------- the planet

	private static Vector3f v(double x, double y, double z) {
		return new Vector3f((float) x, (float) y, (float) z);
	}

	private static float smooth(float a, float b, float x) {
		float t = Math.max(0, Math.min(1, (x - a) / (b - a)));
		return t * t * (3 - 2 * t);
	}

	/** The planet's surface (one ball at radius scale, for the day map, the lights or the clouds), and its glow. */
	private static void buildPlanet() {
		double az = Math.toRadians(PLANET_AZIMUTH), el = Math.toRadians(PLANET_ELEVATION);
		// Towards the planet: east (+x), turned north (-z) by the azimuth, raised by the elevation.
		Vector3f dir = v(Math.cos(el) * Math.cos(az), Math.sin(el), -Math.cos(el) * Math.sin(az)).normalize();
		Vector3f centre = new Vector3f(dir).mul(PLANET_DISTANCE);
		float radius = (float) (PLANET_DISTANCE * Math.sin(Math.toRadians(PLANET_RADIUS_DEG)));
		Vector3f up = new Vector3f(0, 1, 0);
		Vector3f toViewer = new Vector3f(dir).negate();
		Vector3f right = new Vector3f(dir).cross(up).normalize();
		Vector3f upOnDisc = new Vector3f(right).cross(dir).normalize();
		// Its axis tipped a little towards the right; its sun off to the right and a little behind you.
		double tilt = Math.toRadians(21);
		Vector3f pole = new Vector3f(upOnDisc).mul((float) Math.cos(tilt)).add(new Vector3f(right).mul((float) Math.sin(tilt))).normalize();
		Vector3f e1 = new Vector3f(toViewer).sub(new Vector3f(pole).mul(toViewer.dot(pole))).normalize();
		Vector3f e2 = new Vector3f(pole).cross(e1).normalize();
		Vector3f sun = new Vector3f(toViewer).mul(0.55F).add(new Vector3f(right).mul(0.82F)).add(new Vector3f(upOnDisc).mul(0.18F)).normalize();
		// Which longitude faces you (Africa and Europe, about 20 degrees east).
		double faceLon = Math.toRadians(20);

		int quads = PLON * PLAT;
		day = sphere("day", centre, radius, pole, e1, e2, faceLon, sun, 0, quads);
		lights = sphere("lights", centre, radius * 1.0005F, pole, e1, e2, faceLon, sun, 1, quads);
		clouds = sphere("clouds", centre, radius * 1.006F, pole, e1, e2, faceLon, sun, 2, quads);
		planetIndices = quads * 6;

		// The glow: a ring round the edge, brightest on the lit side, fading out into space and in over the disc.
		int seg = 128;
		float[] radii = {radius * 0.94F, radius * 0.995F, radius * 1.02F, radius * 1.16F};
		float[] alpha = {0, 0.75F, 0.5F, 0};
		try (ByteBufferBuilder bytes = ByteBufferBuilder.exactlySized(seg * 3 * 4 * DefaultVertexFormat.POSITION_COLOR.getVertexSize())) {
			BufferBuilder b = new BufferBuilder(bytes, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
			for (int i = 0; i < seg; i++) {
				for (int band = 0; band < 3; band++) {
					double a0 = Math.PI * 2 * i / seg, a1 = Math.PI * 2 * (i + 1) / seg;
					double[] as = {a0, a1, a1, a0};
					int[] rs = {band, band, band + 1, band + 1};
					for (int k = 0; k < 4; k++) {
						Vector3f m = new Vector3f(right).mul((float) Math.cos(as[k])).add(new Vector3f(upOnDisc).mul((float) Math.sin(as[k])));
						float lit = 0.12F + 0.88F * smooth(-0.35F, 0.7F, m.dot(sun));
						Vector3f pos = new Vector3f(m).mul(radii[rs[k]]).add(centre);
						int a = (int) (255 * alpha[rs[k]] * lit);
						b.addVertex(pos.x, pos.y, pos.z).setColor(110, 172, 255, a);
					}
				}
			}
			try (MeshData mesh = b.buildOrThrow()) {
				glow = RenderSystem.getDevice().createBuffer(() -> "Orbit Arena planet glow", 40, mesh.vertexBuffer());
			}
		}
		glowIndices = seg * 3 * 6;
	}

	/** A ball of quads round the planet's centre, coloured for one layer (0 day, 1 night lights, 2 clouds). */
	private static GpuBuffer sphere(String name, Vector3f centre, float radius, Vector3f pole, Vector3f e1, Vector3f e2, double faceLon, Vector3f sun, int layer,
			int quads) {
		try (ByteBufferBuilder bytes = ByteBufferBuilder.exactlySized(quads * 4 * DefaultVertexFormat.POSITION_TEX_COLOR.getVertexSize())) {
			BufferBuilder b = new BufferBuilder(bytes, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
			float[] c = {centre.x, centre.y, centre.z};
			for (int j = 0; j < PLAT; j++) {
				for (int i = 0; i < PLON; i++) {
					int[][] corners = {{i, j}, {i, j + 1}, {i + 1, j + 1}, {i + 1, j}};
					float[][] p = new float[4][];
					Vector3f[] n = new Vector3f[4];
					for (int k = 0; k < 4; k++) {
						double lat = corners[k][1] / (double) PLAT * Math.PI;
						// Longitude east from the one facing you (texture u: 0 at 180 west).
						double u = corners[k][0] / (double) PLON, lon = u * Math.PI * 2 - Math.PI - faceLon;
						n[k] = new Vector3f(e1).mul((float) (Math.sin(lat) * Math.cos(lon))).add(new Vector3f(pole).mul((float) Math.cos(lat)))
								.add(new Vector3f(e2).mul((float) (Math.sin(lat) * Math.sin(lon))));
						p[k] = new float[] {centre.x + n[k].x * radius, centre.y + n[k].y * radius, centre.z + n[k].z * radius};
					}
					int[] order = facing(p, c, true) ? new int[] {0, 1, 2, 3} : new int[] {0, 3, 2, 1};
					for (int k : order) {
						float l = n[k].dot(sun);
						int r, g, bl, a;
						if (layer == 0) {
							float shade = 0.014F + 0.986F * smooth(-0.1F, 0.4F, l);
							r = g = bl = (int) (255 * shade);
							a = 255;
						} else if (layer == 1) {
							float dark = 1 - smooth(-0.22F, 0.06F, l);
							r = 255;
							g = 214;
							bl = 160;
							a = (int) (255 * dark * 0.9F);
						} else {
							float shade = 0.02F + 0.98F * smooth(-0.12F, 0.4F, l);
							r = g = bl = (int) (255 * shade);
							a = (int) (255 * (0.15F + 0.85F * smooth(-0.3F, 0.1F, l)));
						}
						b.addVertex(p[k][0], p[k][1], p[k][2]).setUv(corners[k][0] / (float) PLON, corners[k][1] / (float) PLAT).setColor(r, g, bl, a);
					}
				}
			}
			try (MeshData mesh = b.buildOrThrow()) {
				return RenderSystem.getDevice().createBuffer(() -> "Orbit Arena planet " + name, 40, mesh.vertexBuffer());
			}
		}
	}

	// ---------------------------------------------------------------- drawing

	/** The pictures onto the card, once they've been read. */
	private static void upload() {
		NativeImage[] r = read;
		if (r == null || textures != null) return;
		textures = new DynamicTexture[r.length];
		String[] names = {"stars", "planet day", "planet night", "planet clouds"};
		for (int i = 0; i < r.length; i++) {
			if (r[i] == null) continue;
			final String n = names[i];
			textures[i] = new Smooth(() -> "Orbit Arena " + n, r[i]);
		}
	}

	/** A picture on the card, drawn smoothly (not in hard pixels) and wrapping round. */
	private static final class Smooth extends DynamicTexture {
		Smooth(java.util.function.Supplier<String> label, NativeImage image) {
			super(label, image);
			this.sampler = RenderSystem.getSamplerCache().getSampler(AddressMode.REPEAT, AddressMode.REPEAT, FilterMode.LINEAR, FilterMode.LINEAR, false);
		}
	}

	/** Draws the sky (called in place of the End's sky box). */
	public static void render() {
		Minecraft mc = Minecraft.getInstance();
		if (sky == null) buildSky();
		upload();
		DynamicTexture stars = textures == null ? null : textures[0];
		GpuTextureView colour = mc.getMainRenderTarget().getColorTextureView(), depth = mc.getMainRenderTarget().getDepthTextureView();
		double time = mc.level.getGameTime() + mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
		float angle = (float) ((time / TURN_TICKS) % 1.0 * Math.PI * 2);
		Matrix4f still = new Matrix4f(RenderSystem.getModelViewMatrix());
		Matrix4f turning = new Matrix4f(still).rotate(angle, AXIS);
		if (stars != null) draw(RenderPipelines.END_SKY, sky, skyIndices, turning, stars, colour, depth);

		if (textures == null || textures[1] == null) return;
		if (day == null) buildPlanet();
		draw(PLANET, day, planetIndices, still, textures[1], colour, depth);
		if (textures[2] != null) draw(PLANET_LIGHTS, lights, planetIndices, still, textures[2], colour, depth);
		if (textures[3] != null) draw(PLANET, clouds, planetIndices, still, textures[3], colour, depth);
		draw(GLOW, glow, glowIndices, still, null, colour, depth);
	}

	private static void draw(RenderPipeline pipeline, GpuBuffer buffer, int indices, Matrix4f view, AbstractTexture tex, GpuTextureView colour, GpuTextureView depth) {
		RenderSystem.AutoStorageIndexBuffer seq = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
		GpuBuffer ib = seq.getBuffer(indices);
		GpuBufferSlice transform = RenderSystem.getDynamicUniforms().writeTransform(view, new Vector4f(1, 1, 1, 1), new Vector3f(), new Matrix4f());
		try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "Orbit Arena sky", colour, OptionalInt.empty(), depth, OptionalDouble.empty())) {
			pass.setPipeline(pipeline);
			RenderSystem.bindDefaultUniforms(pass);
			pass.setUniform("DynamicTransforms", transform);
			if (tex != null) pass.bindTexture("Sampler0", tex.getTextureView(), tex.getSampler());
			pass.setVertexBuffer(0, buffer);
			pass.setIndexBuffer(ib, seq.type());
			pass.drawIndexed(0, 0, indices, 1);
		}
	}
}
