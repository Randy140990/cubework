package com.insidethecube.orbitarena.client;

import java.util.OptionalDouble;
import java.util.OptionalInt;

import com.insidethecube.orbitarena.OrbitArena;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * The arena's sky: the night-sky panorama (from ambientCG, CC0) on a sphere round you, turning once every
 * 20 minutes about a tilted axis. The arena dimension uses the End's kind of sky, and this is drawn in
 * place of the End's textured box, in the same way (see mixin.SkyRendererMixin).
 */
public final class ArenaSky {
	private ArenaSky() {}

	private static final Identifier TEXTURE = OrbitArena.id("textures/environment/arena_sky.png");
	private static final int LON = 64, LAT = 32;
	/** One full turn every 20 minutes (24,000 ticks). */
	private static final double TURN_TICKS = 24000.0;
	private static final Vector3f AXIS = new Vector3f(0.25F, 1.0F, 0.35F).normalize();

	private static GpuBuffer buffer;
	private static int indices;

	public static boolean active() {
		Minecraft mc = Minecraft.getInstance();
		return mc.level != null && mc.level.dimension() == OrbitArena.ARENA;
	}

	private static float[] point(int i, int j) {
		double lon = i / (double) LON * Math.PI * 2, lat = j / (double) LAT * Math.PI;
		float r = 100;
		return new float[] {(float) (Math.sin(lat) * Math.cos(lon) * r), (float) (Math.cos(lat) * r), (float) (Math.sin(lat) * Math.sin(lon) * r)};
	}

	private static void build() {
		int quads = LON * LAT;
		try (ByteBufferBuilder bytes = ByteBufferBuilder.exactlySized(quads * 4 * DefaultVertexFormat.POSITION_TEX_COLOR.getVertexSize())) {
			BufferBuilder b = new BufferBuilder(bytes, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
			for (int j = 0; j < LAT; j++) {
				for (int i = 0; i < LON; i++) {
					int[][] corners = {{i, j}, {i, j + 1}, {i + 1, j + 1}, {i + 1, j}};
					float[][] p = new float[4][];
					for (int k = 0; k < 4; k++) p[k] = point(corners[k][0], corners[k][1]);
					// Seen from inside (as the End's sky box is): reverse the corners if they face out.
					float ax = p[1][0] - p[0][0], ay = p[1][1] - p[0][1], az = p[1][2] - p[0][2];
					float bx = p[2][0] - p[0][0], by = p[2][1] - p[0][1], bz = p[2][2] - p[0][2];
					if (ax == 0 && ay == 0 && az == 0) {
						ax = p[2][0] - p[1][0]; ay = p[2][1] - p[1][1]; az = p[2][2] - p[1][2];
						bx = p[3][0] - p[1][0]; by = p[3][1] - p[1][1]; bz = p[3][2] - p[1][2];
					}
					float nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx;
					float cx = p[0][0] + p[2][0], cy = p[0][1] + p[2][1], cz = p[0][2] + p[2][2];
					boolean out = nx * cx + ny * cy + nz * cz > 0;
					int[] order = out ? new int[] {0, 3, 2, 1} : new int[] {0, 1, 2, 3};
					for (int k : order) {
						b.addVertex(p[k][0], p[k][1], p[k][2]).setUv(corners[k][0] / (float) LON, corners[k][1] / (float) LAT).setColor(-1);
					}
				}
			}
			try (MeshData mesh = b.buildOrThrow()) {
				buffer = RenderSystem.getDevice().createBuffer(() -> "Orbit Arena sky", 40, mesh.vertexBuffer());
			}
		}
		indices = quads * 6;
	}

	/** Draws the sky (called in place of the End's sky box). */
	public static void render() {
		Minecraft mc = Minecraft.getInstance();
		if (buffer == null) build();
		AbstractTexture tex = mc.getTextureManager().getTexture(TEXTURE);
		RenderSystem.AutoStorageIndexBuffer seq = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
		GpuBuffer ib = seq.getBuffer(indices);
		GpuTextureView colour = mc.getMainRenderTarget().getColorTextureView(), depth = mc.getMainRenderTarget().getDepthTextureView();
		double time = mc.level.getGameTime() + mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
		float angle = (float) ((time / TURN_TICKS) % 1.0 * Math.PI * 2);
		Matrix4f view = new Matrix4f(RenderSystem.getModelViewMatrix()).rotate(angle, AXIS);
		GpuBufferSlice transform = RenderSystem.getDynamicUniforms().writeTransform(view, new Vector4f(1, 1, 1, 1), new Vector3f(), new Matrix4f());
		try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "Orbit Arena sky", colour, OptionalInt.empty(), depth, OptionalDouble.empty())) {
			pass.setPipeline(RenderPipelines.END_SKY);
			RenderSystem.bindDefaultUniforms(pass);
			pass.setUniform("DynamicTransforms", transform);
			pass.bindTexture("Sampler0", tex.getTextureView(), tex.getSampler());
			pass.setVertexBuffer(0, buffer);
			pass.setIndexBuffer(ib, seq.type());
			pass.drawIndexed(0, 0, indices, 1);
		}
	}
}
