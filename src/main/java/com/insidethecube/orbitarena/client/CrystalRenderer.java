package com.insidethecube.orbitarena.client;

import com.insidethecube.orbitarena.OrbitArena;
import com.insidethecube.orbitarena.entity.Crystal;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
import net.minecraft.util.Mth;

/**
 * Draws a crystal: a six-sided double point, glassy and glowing with veins of energy (green, or light red),
 * turning slowly and bobbing, with a swirl of light over it.
 */
public class CrystalRenderer extends EntityRenderer<Crystal, CrystalRenderer.State> {
	private static final Identifier[] TEXTURES = {OrbitArena.id("textures/entity/crystal/crystal_green.png"), OrbitArena.id("textures/entity/crystal/crystal_red.png")};
	private static final int FULL_BRIGHT = 15728880;

	public static class State extends EntityRenderState {
		public int team;
		public float time;
	}

	public CrystalRenderer(EntityRendererProvider.Context ctx) {
		super(ctx);
		this.shadowRadius = 0;
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(Crystal c, State s, float partial) {
		super.extractRenderState(c, s, partial);
		s.team = c.team;
		s.time = c.tickCount + partial + c.phase * 400;
	}

	@Override
	public boolean shouldRender(Crystal c, Frustum frustum, double x, double y, double z) {
		return true;
	}

	@Override
	public void submit(State s, PoseStack pose, SubmitNodeCollector out, CameraRenderState camera) {
		Identifier tex = TEXTURES[Math.floorMod(s.team, 2)];
		pose.pushPose();
		pose.translate(0, Mth.sin(s.time * 0.05F) * 0.25F, 0);
		pose.mulPose(Axis.YP.rotationDegrees(s.time * 0.9F));
		out.submitCustomGeometry(pose, RenderTypes.entityTranslucentEmissive(tex), (p, vc) -> gem(p, vc, 1.0F, 0xE6FFFFFF));
		float scroll = (s.time * 0.006F) % 1.0F;
		pose.scale(1.06F, 1.04F, 1.06F);
		out.submitCustomGeometry(pose, RenderTypes.energySwirl(tex, scroll, scroll * 0.7F), (p, vc) -> gem(p, vc, 1.0F, 0xFF808080));
		pose.popPose();
		super.submit(s, pose, out, camera);
	}

	/**
	 * A six-sided gem: a short prism, a point above and a point below. Radius 1.25, 3.6 tall; the texture
	 * wraps round it (the sides in its middle half, the points above and below).
	 */
	private static void gem(PoseStack.Pose p, VertexConsumer vc, float scale, int colour) {
		float r = 1.25F * scale, h = 0.75F * scale, tip = 1.05F * scale;
		for (int i = 0; i < 6; i++) {
			double a0 = Math.PI / 3 * i, a1 = Math.PI / 3 * (i + 1);
			float x0 = (float) Math.cos(a0) * r, z0 = (float) Math.sin(a0) * r, x1 = (float) Math.cos(a1) * r, z1 = (float) Math.sin(a1) * r;
			float u0 = i / 6F, u1 = (i + 1) / 6F;
			float mx = (float) Math.cos((a0 + a1) / 2), mz = (float) Math.sin((a0 + a1) / 2);
			// The side.
			quad(p, vc, colour, x0, -h, z0, u0, 0.75F, x1, -h, z1, u1, 0.75F, x1, h, z1, u1, 0.25F, x0, h, z0, u0, 0.25F, mx, 0, mz);
			// The points: up to the tip and down to the bottom.
			float up = (float) (r / Math.sqrt(r * r + tip * tip)), out = (float) (tip / Math.sqrt(r * r + tip * tip));
			quad(p, vc, colour, x0, h, z0, u0, 0.25F, x1, h, z1, u1, 0.25F, 0, h + tip, 0, (u0 + u1) / 2, 0, 0, h + tip, 0, (u0 + u1) / 2, 0, mx * out, up, mz * out);
			quad(p, vc, colour, x1, -h, z1, u1, 0.75F, x0, -h, z0, u0, 0.75F, 0, -h - tip, 0, (u0 + u1) / 2, 1, 0, -h - tip, 0, (u0 + u1) / 2, 1, mx * out, -up, mz * out);
		}
	}

	private static void quad(PoseStack.Pose p, VertexConsumer vc, int colour, float ax, float ay, float az, float au, float av, float bx, float by, float bz, float bu,
			float bv, float cx, float cy, float cz, float cu, float cv, float dx, float dy, float dz, float du, float dv, float nx, float ny, float nz) {
		v(p, vc, colour, ax, ay, az, au, av, nx, ny, nz);
		v(p, vc, colour, bx, by, bz, bu, bv, nx, ny, nz);
		v(p, vc, colour, cx, cy, cz, cu, cv, nx, ny, nz);
		v(p, vc, colour, dx, dy, dz, du, dv, nx, ny, nz);
	}

	private static void v(PoseStack.Pose p, VertexConsumer vc, int colour, float x, float y, float z, float u, float v, float nx, float ny, float nz) {
		vc.addVertex(p, x, y, z).setColor(colour).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(p, nx, ny, nz);
	}
}
