package com.insidethecube.orbitarena.client;

import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

/** A fan's look and sign, and what it's doing this frame (worked out in FanRenderer, drawn by FanModel). */
public class FanRenderState extends HumanoidRenderState {
	public int look, sign = -1;
	public float height = 1;
	/** Time in ticks, shifted by the fan's own beat, so no two move together. */
	public float time;
	/** What it's doing (FanModel.IDLE and so on), and how far into it it is (0 to 1, eased in and out). */
	public int action;
	public float blend;
	/** How lively the crowd is, how much it groans, how much this fan's side is booing, and how high the wave lifts it (0 to 1). */
	public float energy, groan, boo, wave;
	/** Watching the fight through a spyglass. */
	public boolean spyglass;
	/** Sitting down when it's calm (a fan who likes to sit, while nothing much is happening). */
	public boolean sitBase;
}
