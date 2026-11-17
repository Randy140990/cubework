package com.insidethecube.retromon.client;

import com.insidethecube.retromon.game.WorldMon;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** Draws a Pokémon standing in the world (Snorlax on the road, a gift, a PIDGEY in a house), idling. */
public class WorldMonRenderer extends EntityRenderer<WorldMon, WorldMonRenderer.State> {
	private static final int FULL_BRIGHT = 15728880;

	public static class State extends EntityRenderState {
		BedrockModel model;
		float yaw;
	}

	public WorldMonRenderer(EntityRendererProvider.Context ctx) {
		super(ctx);
		this.shadowRadius = 0.6F;
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(WorldMon e, State s, float partial) {
		super.extractRenderState(e, s, partial);
		if (!(e.model instanceof BedrockModel m) || !m.def.species.equals(e.species())) e.model = BedrockModel.create(e.species());
		s.model = e.model instanceof BedrockModel m ? m : null;
		s.yaw = e.facing();
	}

	@Override
	public void submit(State s, PoseStack pose, SubmitNodeCollector out, CameraRenderState camera) {
		BedrockModel m = s.model;
		if (m == null) return;
		float scale = m.def.scale;
		pose.pushPose();
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
