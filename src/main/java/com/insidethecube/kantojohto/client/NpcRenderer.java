package com.insidethecube.kantojohto.client;

import com.insidethecube.kantojohto.RouteOne;
import com.insidethecube.kantojohto.game.Npc;
import com.insidethecube.kantojohto.game.Trainers;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/** Draws the people: the nurse, the clerk, trainers and gym leaders, each in their own skin. */
public class NpcRenderer extends HumanoidMobRenderer<Npc, NpcRenderer.State, HumanoidModel<NpcRenderer.State>> {
	/** Skins by number: nurse, clerk, youngster, gym leader. */
	private static final Identifier[] SKINS = {RouteOne.id("textures/entity/npc/nurse.png"), RouteOne.id("textures/entity/npc/clerk.png"),
			RouteOne.id("textures/entity/npc/youngster.png"), RouteOne.id("textures/entity/npc/leader.png")};

	public static class State extends HumanoidRenderState {
		int skin;
	}

	public NpcRenderer(EntityRendererProvider.Context ctx) {
		super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER)), 0.5F);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public boolean shouldRender(Npc npc, Frustum frustum, double x, double y, double z) {
		// The trainer you're battling steps out of the picture (the battle camera is where they stood).
		if (BattleScene.hides(npc)) return false;
		return super.shouldRender(npc, frustum, x, y, z);
	}

	@Override
	public void extractRenderState(Npc npc, State s, float partial) {
		super.extractRenderState(npc, s, partial);
		int skin = switch (npc.role()) {
			case Npc.NURSE -> 0;
			case Npc.CLERK -> 1;
			case Npc.LEADER -> 3;
			default -> 2;
		};
		Trainers.Trainer t = Trainers.get(npc.key());
		if (t != null) skin = t.skin();
		s.skin = skin;
	}

	@Override
	public Identifier getTextureLocation(State s) {
		return SKINS[Math.floorMod(s.skin, SKINS.length)];
	}
}
