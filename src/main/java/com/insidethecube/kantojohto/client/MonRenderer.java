package com.insidethecube.kantojohto.client;

import com.insidethecube.kantojohto.game.MonDisplay;
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
import net.minecraft.util.Mth;

/** Draws a Pokémon on the battlefield: its model, idling, with its glowing layers, lunging and flickering. */
public class MonRenderer extends EntityRenderer<MonDisplay, MonRenderer.State> {
	private static final int FULL_BRIGHT = 15728880;

	public static class State extends EntityRenderState {
		BedrockModel model;
		float yaw, size, lunge;
		boolean hidden;
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
		if (!(e.model instanceof BedrockModel m) || !m.def.species.equals(e.species)) {
			e.model = BedrockModel.create(e.species);
		}
		s.model = e.model instanceof BedrockModel m ? m : null;
		s.yaw = e.facing;
		s.size = Mth.lerp(partial, e.sizeOld, e.size);
		s.lunge = e.lunge > 0 ? Mth.sin((float) Math.PI * (MonDisplay.LUNGE - e.lunge + partial) / MonDisplay.LUNGE) : 0;
		s.hidden = e.flash > 0 && (e.flash / 2) % 2 == 0;
	}

	@Override
	public void submit(State s, PoseStack pose, SubmitNodeCollector out, CameraRenderState camera) {
		BedrockModel m = s.model;
		if (m == null || s.hidden || s.size < 0.02F) return;
		float scale = m.def.scale * s.size;
		pose.pushPose();
		double yaw = Math.toRadians(s.yaw);
		pose.translate(-Math.sin(yaw) * s.lunge * 0.7, 0, Math.cos(yaw) * s.lunge * 0.7);
		pose.mulPose(Axis.YP.rotationDegrees(180F - s.yaw));
		pose.scale(-1F, -1F, 1F);
		pose.scale(scale, scale, scale);
		pose.translate(0F, -1.501F, 0F);
		m.pose(s.ageInTicks / 20.0);
		out.submitModelPart(m.root, pose, RenderTypes.entityCutoutNoCull(m.def.texture), s.lightCoords, OverlayTexture.NO_OVERLAY, null);
		for (BedrockModel.Layer l : m.def.layers) {
			out.submitModelPart(m.root, pose, RenderTypes.entityTranslucentEmissive(l.frame(s.ageInTicks / 20F)), FULL_BRIGHT, OverlayTexture.NO_OVERLAY, null);
		}
		pose.popPose();
		super.submit(s, pose, out, camera);
	}
}
