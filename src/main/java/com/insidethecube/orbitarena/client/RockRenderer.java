package com.insidethecube.orbitarena.client;

import java.util.HashMap;
import java.util.Map;

import com.insidethecube.orbitarena.OrbitArena;
import com.insidethecube.orbitarena.arena.ArenaLayout;
import com.insidethecube.orbitarena.entity.Rock;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * Draws an orbiting rock: a lumpy clump of blocks in red layered rock (netherrack, red and plain terracotta,
 * granite, as the spires are), tumbling slowly. Each rock's shape is made once from its seed and kept.
 */
public class RockRenderer extends EntityRenderer<Rock, RockRenderer.State> {
	private static final Identifier TEXTURE = OrbitArena.id("textures/entity/rock/rock.png");
	private static final Map<Long, float[]> MESHES = new HashMap<>();

	public static class State extends EntityRenderState {
		public long seed;
		public int size;
		public float time, spinX, spinY;
	}

	public RockRenderer(EntityRendererProvider.Context ctx) {
		super(ctx);
		this.shadowRadius = 0;
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(Rock r, State s, float partial) {
		super.extractRenderState(r, s, partial);
		s.seed = r.seed;
		s.size = r.size;
		s.spinX = r.spinX;
		s.spinY = r.spinY;
		s.time = (float) ((r.level().getGameTime() % 240000L) + partial);
	}

	@Override
	public boolean shouldRender(Rock r, Frustum frustum, double x, double y, double z) {
		return true;
	}

	@Override
	public void submit(State s, PoseStack pose, SubmitNodeCollector out, CameraRenderState camera) {
		float[] mesh = MESHES.computeIfAbsent(s.seed * 31 + s.size, k -> build(s.seed, s.size));
		int light = s.lightCoords;
		pose.pushPose();
		pose.mulPose(Axis.YP.rotationDegrees(s.time * s.spinY * 0.25F));
		pose.mulPose(Axis.XP.rotationDegrees(s.time * s.spinX * 0.25F));
		out.submitCustomGeometry(pose, RenderTypes.entityCutout(TEXTURE), (p, vc) -> {
			for (int i = 0; i < mesh.length; i += 8) {
				vc.addVertex(p, mesh[i], mesh[i + 1], mesh[i + 2]).setColor(-1).setUv(mesh[i + 3], mesh[i + 4]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
						.setNormal(p, mesh[i + 5], mesh[i + 6], mesh[i + 7]);
			}
		});
		pose.popPose();
		super.submit(s, pose, out, camera);
	}

	private static final int[][] DIRS = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
	/** For each direction: the corner to start from (as an offset), and the two edges (each wound so the face points out). */
	private static final int[][][] FACES = {
			{{1, 0, 0}, {0, 1, 0}, {0, 0, 1}},
			{{0, 0, 0}, {0, 0, 1}, {0, 1, 0}},
			{{0, 1, 0}, {0, 0, 1}, {1, 0, 0}},
			{{0, 0, 0}, {1, 0, 0}, {0, 0, 1}},
			{{0, 0, 1}, {1, 0, 0}, {0, 1, 0}},
			{{0, 0, 0}, {0, 1, 0}, {1, 0, 0}}};

	/** A rock's shape: a ball of blocks of radius about `size`, lumpy, as quads (x, y, z, u, v, normal x, y, z). */
	private static float[] build(long seed, int size) {
		int r = size + 1;
		int n = 2 * r + 1;
		boolean[][][] solid = new boolean[n][n][n];
		int[][][] tile = new int[n][n][n];
		for (int x = -r; x <= r; x++) {
			for (int y = -r; y <= r; y++) {
				for (int z = -r; z <= r; z++) {
					double lump = 0.72 + 0.5 * ArenaLayout.hash(Math.floorDiv(x, 2), Math.floorDiv(y, 2), Math.floorDiv(z, 2), seed);
					double rad = (size + 0.5) * lump;
					if (x * x + y * y * 1.3 + z * z <= rad * rad) {
						solid[x + r][y + r][z + r] = true;
						double hh = ArenaLayout.hash(x, y, z, seed + 7);
						tile[x + r][y + r][z + r] = hh < 0.1 ? 3 : Math.floorMod(y + (int) (seed % 5), 4) == 0 ? 1 : Math.floorMod(y + (int) (seed % 5), 4) == 2 ? 2 : 0;
					}
				}
			}
		}
		float[] buf = new float[64];
		int len = 0;
		for (int x = 0; x < n; x++) {
			for (int y = 0; y < n; y++) {
				for (int z = 0; z < n; z++) {
					if (!solid[x][y][z]) continue;
					for (int f = 0; f < 6; f++) {
						int[] d = DIRS[f];
						int nx = x + d[0], ny = y + d[1], nz = z + d[2];
						if (nx >= 0 && ny >= 0 && nz >= 0 && nx < n && ny < n && nz < n && solid[nx][ny][nz]) continue;
						int[][] face = FACES[f];
						float bx = x - r - 0.5F + face[0][0], by = y - r - 0.5F + face[0][1], bz = z - r - 0.5F + face[0][2];
						float u0 = tile[x][y][z] * 0.25F, u1 = u0 + 0.25F;
						float[][] corners = {
								{bx, by, bz, u0, 1},
								{bx + face[1][0], by + face[1][1], bz + face[1][2], u1, 1},
								{bx + face[1][0] + face[2][0], by + face[1][1] + face[2][1], bz + face[1][2] + face[2][2], u1, 0},
								{bx + face[2][0], by + face[2][1], bz + face[2][2], u0, 0}};
						if (len + 32 > buf.length) buf = java.util.Arrays.copyOf(buf, buf.length * 2);
						for (float[] c : corners) {
							buf[len++] = c[0];
							buf[len++] = c[1];
							buf[len++] = c[2];
							buf[len++] = c[3];
							buf[len++] = c[4];
							buf[len++] = d[0];
							buf[len++] = d[1];
							buf[len++] = d[2];
						}
					}
				}
			}
		}
		return java.util.Arrays.copyOf(buf, len);
	}
}
