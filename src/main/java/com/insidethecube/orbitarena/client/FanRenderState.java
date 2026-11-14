package com.insidethecube.orbitarena.client;

import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

/** A fan's look, sign, beat and how excited the crowd is right now. */
public class FanRenderState extends HumanoidRenderState {
	public int look, sign = -1;
	public float phase, height = 1, excitement, groan;
}
