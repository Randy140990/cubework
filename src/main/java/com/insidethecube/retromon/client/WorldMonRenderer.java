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
		boolean asleep;
		/** (0.74) The MAGNET TRAIN (cut off at its tunnel's dark back while you ride). */
		boolean train;
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
	public boolean shouldRender(WorldMon e, net.minecraft.client.renderer.culling.Frustum frustum, double x, double y, double z) {
		// Out of the picture in a battle fought in the room itself.
		if (BattleScene.hides(e)) return false;
		// (0.73) The MAGNET TRAIN: sixteen blocks long on an entity a block across, so it's never left out for being
		// off the edge of the picture by its middle.
		if (e.key().startsWith("train:")) return true;
		return super.shouldRender(e, frustum, x, y, z);
	}

	@Override
	public void extractRenderState(WorldMon e, State s, float partial) {
		super.extractRenderState(e, s, partial);
		// (Made again when POKéMON LOOK changes: its model is the look's.)
		if (!(e.model instanceof BedrockModel m) || m.def != BedrockModel.def(e.species())) e.model = BedrockModel.create(e.species());
		s.model = e.model instanceof BedrockModel m ? m : null;
		s.yaw = e.facing();
		s.train = e.key().startsWith("train:");
		// (0.61) Your SURF POKéMON: under you, where you are this frame (not a tick behind), its back at your feet,
		// facing the way you face.
		var me = net.minecraft.client.Minecraft.getInstance().player;
		if (me != null && e.key().equals("surf:" + me.getUUID()) && s.model != null) {
			float back = Math.max(0.35F, Math.min(1.2F, s.model.def.height)) * 0.85F;
			s.x = Mth.lerp(partial, me.xo, me.getX());
			s.y = Mth.lerp(partial, me.yo, me.getY()) - back;
			s.z = Mth.lerp(partial, me.zo, me.getZ());
			s.yaw = Mth.rotLerp(partial, me.yBodyRotO, me.yBodyRot);
			s.pose = s.model.def.hasPose("swim") ? "swim" : null;
			return;
		}
		s.pose = null;
		s.lift = 0;
		s.asleep = e.asleep() && s.model != null;
		if (s.asleep) s.pose = s.model.def.hasPose("sleep") ? "sleep" : s.model.def.hasPose("ground") ? "ground" : null;
		if ((e.roams() || e.scenery() || e.wanders()) && s.model != null) {
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
		// ((0.74) Where the tunnel's dark back is in the drawing's space: the entity's place there, less its place in the
		// world, plus the wall's.)
		float wall = s.train && Ride.on && Ride.dir != 0 ? (float) (Ride.wall - s.x + pose.last().pose().m30()) : Float.NaN;
		pose.pushPose();
		pose.translate(0, s.lift, 0);
		pose.mulPose(Axis.YP.rotationDegrees(180F - s.yaw));
		if (s.asleep) {
			// (0.50.3) Lying on its back (its front turned up, its head the way its back was), raised so its back rests
			// on the ground, and moved along so its middle, not its feet, is on its spot.
			pose.translate(0F, scale * m.def.backZ / 16F, -scale * m.def.topY / 32F);
			pose.mulPose(Axis.XP.rotationDegrees(90F));
		}
		pose.scale(-1F, -1F, 1F);
		pose.scale(scale, scale, scale);
		pose.translate(0F, -1.501F, 0F);
		m.pose(s.ageInTicks / 20.0, s.pose, null, 0);
		if (!Float.isNaN(wall)) {
			// (0.74) The MAGNET TRAIN going into its tunnel (or coming out of it): drawn cut off at the dark back.
			final int light = s.lightCoords, side = Ride.dir;
			out.submitCustomGeometry(pose, RenderTypes.entityCutoutNoCull(m.def.texture), (pp, vc) -> {
				PoseStack ps = new PoseStack();
				ps.last().pose().set(pp.pose());
				ps.last().normal().set(pp.normal());
				m.root.render(ps, new ClipConsumer(vc, wall, side), light, OverlayTexture.NO_OVERLAY);
			});
			pose.popPose();
			super.submit(s, pose, out, camera);
			return;
		}
		out.submitModelPart(m.root, pose, RenderTypes.entityCutoutNoCull(m.def.texture), s.lightCoords, OverlayTexture.NO_OVERLAY, null);
		for (BedrockModel.Layer l : m.def.layers) {
			out.submitModelPart(m.root, pose, RenderTypes.entityTranslucentEmissive(l.frame(s.ageInTicks / 20F)), FULL_BRIGHT, OverlayTexture.NO_OVERLAY, null);
		}
		pose.popPose();
		super.submit(s, pose, out, camera);
	}
}
