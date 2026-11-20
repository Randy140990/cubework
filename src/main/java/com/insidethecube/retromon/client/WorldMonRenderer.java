package com.insidethecube.retromon.client;

import net.minecraft.util.Mth;
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

/**
 * Draws a Pokémon in the world: standing (Snorlax on the road, a gift, a PIDGEY in a house), idling; or out in
 * the grass and walking with you, walking as it moves and flying (Pidgey over the grass) with its own animations.
 */
public class WorldMonRenderer extends EntityRenderer<WorldMon, WorldMonRenderer.State> {
	private static final int FULL_BRIGHT = 15728880;

	public static class State extends EntityRenderState {
		BedrockModel model;
		float yaw, lift;
		String pose;
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
		// (Made again when POKéMON LOOK changes: its model is the look's.)
		if (!(e.model instanceof BedrockModel m) || m.def != BedrockModel.def(e.species())) e.model = BedrockModel.create(e.species());
		s.model = e.model instanceof BedrockModel m ? m : null;
		s.yaw = e.facing();
		s.pose = null;
		s.lift = 0;
		if (e.roams() && s.model != null) {
			// Out in the grass or walking with you: walks when it moves, and a bird takes to the air.
			// Walking when it's been moving these last few ticks (not just this one: the world's updates come in
			// steps, and a tick without one left it gliding along in its standing pose).
			double moved = Math.hypot(e.getX() - e.xo, e.getZ() - e.zo);
			double[] sp = PACE.computeIfAbsent(e, k -> new double[] {0, -1});
			if (sp[1] != e.tickCount) {
				sp[0] = Math.max(moved, sp[0] * 0.75);
				sp[1] = e.tickCount;
			}
			boolean moving = sp[0] > 0.012;
			s.yaw = Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot);
			// Wild birds fly well above the grass (you walk under them; they come down to land now and then).
			float target = e.airborne() ? (e.wild() ? 4.2F : 1.6F) + (float) Math.sin((e.tickCount + partial) * 0.12F) * (e.wild() ? 0.45F : 0.25F) : 0;
			if (e.tickCount != lastTick(e)) {
				e.liftOld = e.liftNow;
				e.liftNow += (target - e.liftNow) * 0.08F;
			}
			s.lift = Mth.lerp(partial, e.liftOld, e.liftNow);
			BedrockModel m = s.model;
			if (s.lift > 0.3F) s.pose = moving && m.def.hasPose("fly") ? "fly" : m.def.hasPose("hover") ? "hover" : m.def.hasPose("fly") ? "fly" : null;
			else if (moving && m.def.hasPose("walk")) s.pose = "walk";
		}
	}

	private static final java.util.WeakHashMap<WorldMon, Integer> TICKED = new java.util.WeakHashMap<>();
	/** How fast each has been going lately (blocks a tick, easing off) and the tick that was worked out on. */
	private static final java.util.WeakHashMap<WorldMon, double[]> PACE = new java.util.WeakHashMap<>();

	/** Eases the flight height once a tick (not once a frame). */
	private static int lastTick(WorldMon e) {
		Integer t = TICKED.put(e, e.tickCount);
		return t == null ? -1 : t;
	}

	@Override
	public void submit(State s, PoseStack pose, SubmitNodeCollector out, CameraRenderState camera) {
		BedrockModel m = s.model;
		if (m == null) return;
		float scale = m.def.scale;
		pose.pushPose();
		pose.translate(0, s.lift, 0);
		pose.mulPose(Axis.YP.rotationDegrees(180F - s.yaw));
		pose.scale(-1F, -1F, 1F);
		pose.scale(scale, scale, scale);
		pose.translate(0F, -1.501F, 0F);
		m.pose(s.ageInTicks / 20.0, s.pose, null, 0);
		out.submitModelPart(m.root, pose, RenderTypes.entityCutoutNoCull(m.def.texture), s.lightCoords, OverlayTexture.NO_OVERLAY, null);
		for (BedrockModel.Layer l : m.def.layers) {
			out.submitModelPart(m.root, pose, RenderTypes.entityTranslucentEmissive(l.frame(s.ageInTicks / 20F)), FULL_BRIGHT, OverlayTexture.NO_OVERLAY, null);
		}
		pose.popPose();
		super.submit(s, pose, out, camera);
	}
}
