package com.insidethecube.orbitarena.client;

import com.insidethecube.orbitarena.OrbitArena;
import com.insidethecube.orbitarena.entity.Fan;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.Identifier;

/** Draws the fans: one of the fan looks, with or without a sign, a little taller or shorter. */
public class FanRenderer extends HumanoidMobRenderer<Fan, FanRenderState, FanModel> {
	public static final int LOOKS = 8, SIGNS = 6;
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
		state.phase = fan.phase;
		state.height = fan.height;
		state.excitement = Crowd.excitement(fan.phase);
		state.groan = Crowd.groan();
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
