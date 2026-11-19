package com.insidethecube.retromon.client;

import com.insidethecube.retromon.game.MonDisplay;
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
import net.minecraft.util.Mth;

/** Draws a Pokémon on the battlefield: its model, idling, with its glowing layers, lunging and flickering. */
public class MonRenderer extends EntityRenderer<MonDisplay, MonRenderer.State> {
	private static final int FULL_BRIGHT = 15728880;

	public static class State extends EntityRenderState {
		BedrockModel model;
		float yaw, size, lunge, lift, wobble, actionSecs, spin, keel, white;
		boolean hidden, red;
		String action;
		Identifier texture;
	}

	public MonRenderer(EntityRendererProvider.Context ctx) {
		super(ctx);
		this.shadowRadius = 0.5F;
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public boolean shouldRender(MonDisplay e, Frustum frustum, double x, double y, double z) {
		return true;
	}

	@Override
	public void extractRenderState(MonDisplay e, State s, float partial) {
		super.extractRenderState(e, s, partial);
		if (!(e.model instanceof BedrockModel m) || m.def != BedrockModel.def(e.species)) {
			e.model = BedrockModel.create(e.species);
		}
		s.model = e.model instanceof BedrockModel m ? m : null;
		s.yaw = e.facing;
		s.size = Mth.lerp(partial, e.sizeOld, e.size);
		s.lunge = s.lift = 0;
		if (e.lunge > 0) {
			float p = Math.min(1, (MonDisplay.LUNGE - e.lunge + partial) / MonDisplay.LUNGE);
			if (e.dash) {
				// Draws back, springs in with a hop, lands, then trots back to its place.
				if (p < 0.18F) s.lunge = -0.3F * ease(p / 0.18F);
				else if (p < 0.48F) s.lunge = -0.3F + 1.75F * ease((p - 0.18F) / 0.3F);
				else s.lunge = 1.45F * (1 - ease((p - 0.48F) / 0.52F));
				if (p > 0.16F && p < 0.56F) s.lift = 0.6F * Mth.sin((float) Math.PI * (p - 0.16F) / 0.4F);
			} else {
				// Rears back and hops where it stands as it lets the move go.
				s.lunge = -0.22F * Mth.sin((float) Math.PI * p);
				s.lift = 0.45F * Math.abs(Mth.sin((float) Math.PI * 2 * p)) * (1 - p * 0.4F);
			}
		}
		if (e.cheer > 0) {
			// A win: two hops for joy, a twirl on the first.
			float p = Math.min(1, (MonDisplay.CHEER - e.cheer + partial) / MonDisplay.CHEER);
			if (p < 0.8F) s.lift = 0.55F * Math.abs(Mth.sin((float) Math.PI * p / 0.4F));
			if (p < 0.4F) s.spin = p / 0.4F * 360F;
		} else s.spin = 0;
		s.keel = 0;
		if (e.faint > 0) {
			// Fainting: sags, then keels over onto its side and settles a little lower.
			float p = Math.min(1, (MonDisplay.FAINT - e.faint + partial) / MonDisplay.FAINT);
			float k = ease(Math.max(0, p - 0.12F) / 0.38F);
			s.keel = 82F * k;
			s.lift = -0.12F * k + (p < 0.12F ? -0.05F * Mth.sin((float) Math.PI * p / 0.12F) : 0);
		}
		s.hidden = e.flash > 0 && (e.flash / 2) % 2 == 0;
		s.action = e.action;
		s.actionSecs = (e.actionAge + partial) / 20F;
		s.red = e.red > 0;
		s.white = e.white > 0 ? Math.max(0, Math.min(1, (e.white - partial) / MonDisplay.WHITE)) : 0;
		s.texture = e.texture == null ? null : Identifier.parse(e.texture);
		s.wobble = e.wobble > 0 ? Mth.sin((12 - e.wobble + partial) / 12F * (float) Math.PI * 2) * 25F : 0;
	}

	private static float ease(float t) {
		t = Math.max(0, Math.min(1, t));
		return t * t * (3 - 2 * t);
	}

	@Override
	public void submit(State s, PoseStack pose, SubmitNodeCollector out, CameraRenderState camera) {
		BedrockModel m = s.model;
		if (m == null || s.hidden || s.size < 0.02F) return;
		float scale = m.def.scale * s.size;
		pose.pushPose();
		double yaw = Math.toRadians(s.yaw);
		pose.translate(-Math.sin(yaw) * s.lunge, s.lift, Math.cos(yaw) * s.lunge);
		pose.mulPose(Axis.YP.rotationDegrees(180F - s.yaw - s.spin));
		if (s.wobble != 0) pose.mulPose(Axis.ZP.rotationDegrees(s.wobble));
		if (s.keel != 0) pose.mulPose(Axis.ZP.rotationDegrees(s.keel));
		pose.scale(-1F, -1F, 1F);
		pose.scale(scale, scale, scale);
		pose.translate(0F, -1.501F, 0F);
		m.pose(s.ageInTicks / 20.0, s.action, s.actionSecs);
		// Lit at least a little, so battles at night can still be seen.
		int light = Math.max(s.lightCoords & 0xFFFF, 11 << 4) | (s.lightCoords & 0xFFFF0000);
		// Red light as it's drawn into a ball.
		// Red: being called back into its ball. White: just come out of it, in the white light.
		int overlay = s.red ? OverlayTexture.pack(0, 3) : s.white > 0 ? OverlayTexture.pack(OverlayTexture.u(s.white), OverlayTexture.v(false)) : OverlayTexture.NO_OVERLAY;
		out.submitModelPart(m.root, pose, RenderTypes.entityCutoutNoCull(s.texture != null ? s.texture : m.def.texture), s.red || s.white > 0 ? FULL_BRIGHT : light,
				overlay, null);
		for (BedrockModel.Layer l : m.def.layers) {
			out.submitModelPart(m.root, pose, RenderTypes.entityTranslucentEmissive(l.frame(s.ageInTicks / 20F)), FULL_BRIGHT, OverlayTexture.NO_OVERLAY, null);
		}
		pose.popPose();
		super.submit(s, pose, out, camera);
	}
}
