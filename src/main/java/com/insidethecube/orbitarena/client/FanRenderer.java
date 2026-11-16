package com.insidethecube.orbitarena.client;

import com.insidethecube.orbitarena.OrbitArena;
import com.insidethecube.orbitarena.arena.ArenaLayout;
import com.insidethecube.orbitarena.entity.Fan;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Draws the fans: one of the fan looks, with or without a sign, a little taller or shorter. */
public class FanRenderer extends HumanoidMobRenderer<Fan, FanRenderState, FanModel> {
	/** Eight looks, then two in blue's shirts and two in red's (8 to 11); twelve small signs and six big boards. */
	public static final int LOOKS = 12, SIGNS = 18;
	private static final Identifier[][] TEXTURES = new Identifier[LOOKS][SIGNS + 1];

	static {
		for (int l = 0; l < LOOKS; l++) {
			for (int s = 0; s <= SIGNS; s++) TEXTURES[l][s] = OrbitArena.id("textures/entity/fan/fan_" + l + "_" + s + ".png");
		}
	}

	public FanRenderer(EntityRendererProvider.Context ctx) {
		super(ctx, new FanModel(ctx.bakeLayer(FanModel.LAYER)), 0.0F);
	}

	@Override
	public FanRenderState createRenderState() {
		return new FanRenderState();
	}

	@Override
	public void extractRenderState(Fan fan, FanRenderState state, float partial) {
		super.extractRenderState(fan, state, partial);
		state.look = fan.look;
		state.sign = fan.sign;
		state.height = fan.height;
		state.spyglass = fan.spyglass;
		Minecraft mc = Minecraft.getInstance();
		float now = (mc.level == null ? 0 : mc.level.getGameTime()) + partial;
		state.time = now + fan.phase * 997;
		state.energy = Crowd.energy();
		state.groan = Crowd.groan();
		// How its own side's doing: scored (cheer) or scored on (boo); a fan on neither side cheers anything.
		float cheer = Crowd.cheeringFor(fan.team), boo = Crowd.booingFor(fan.team);
		state.boo = boo;
		state.sitBase = fan.sits && state.energy < 0.55F && cheer < 0.3F && boo < 0.3F && Crowd.cheering() < 0.4F;
		// What it's doing: a new choice every 2.5 to 4.5 seconds, its own beat, livelier as the crowd is.
		float slotLength = 50 + fan.phase * 40;
		float at = (now + fan.phase * 1234) / slotLength;
		long slot = (long) Math.floor(at);
		float local = at - slot;
		long seed = (long) (fan.phase * 1_000_003);
		state.action = FanModel.choose(ArenaLayout.hash(seed, slot, fan.sign, 7), ArenaLayout.hash(seed, slot, fan.look, 8), state.energy, cheer, boo, state.groan,
				fan.sign >= 0, fan.snack, fan.flag, fan.sits);
		float edge = Math.min(local, 1 - local) / 0.15F;
		state.blend = edge >= 1 ? 1 : edge * edge * (3 - 2 * edge);
		// The Mexican wave lifts the fans it's passing.
		float wave = Crowd.waveAngle(now);
		if (Float.isNaN(wave)) state.wave = 0;
		else {
			float d = (float) ArenaLayout.angleDiff(fan.seatAngle, wave);
			state.wave = d < 14 ? 0.5F + 0.5F * Mth.cos(d / 14 * Mth.PI) : 0;
		}
		// Every head follows you (each a little off, and drifting).
		if (mc.player != null) {
			double dx = mc.player.getX() - fan.getX(), dz = mc.player.getZ() - fan.getZ(), dy = mc.player.getEyeY() - fan.getEyeY();
			float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
			float off = (fan.phase - 0.5F) * 24 + Mth.sin(state.time * 0.02F) * 8;
			state.yRot = Mth.clamp(Mth.wrapDegrees(yaw - state.bodyRot + off), -70, 70);
			state.xRot = Mth.clamp((float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))), -35, 40);
		}
	}

	@Override
	public Identifier getTextureLocation(FanRenderState state) {
		return TEXTURES[Math.floorMod(state.look, LOOKS)][state.sign < 0 ? 0 : 1 + Math.floorMod(state.sign, SIGNS)];
	}

	@Override
	protected void scale(FanRenderState state, PoseStack pose) {
		pose.scale(state.height, state.height, state.height);
	}
}
