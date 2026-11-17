package com.insidethecube.retromon.client;

import java.util.HashMap;
import java.util.Map;

import com.insidethecube.retromon.Retromon;
import com.insidethecube.retromon.game.Npc;
import com.insidethecube.retromon.game.Trainers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * Draws the people: the nurse, the clerk, trainers, gym leaders and the townsfolk, each in the skin for the
 * sprite the games gave them (a youngster, a lass, a hiker...).
 */
public class NpcRenderer extends HumanoidMobRenderer<Npc, NpcRenderer.State, HumanoidModel<NpcRenderer.State>> {
	private static final Map<String, Identifier> SKINS = new HashMap<>();
	private static final Identifier FALLBACK = Retromon.id("textures/entity/npc/youngster.png");

	public static class State extends HumanoidRenderState {
		String sprite = "youngster";
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
		// The trainer you're battling steps out of the picture.
		if (BattleScene.hides(npc)) return false;
		return super.shouldRender(npc, frustum, x, y, z);
	}

	@Override
	public void extractRenderState(Npc npc, State s, float partial) {
		super.extractRenderState(npc, s, partial);
		s.sprite = sprite(npc);
	}

	static String sprite(Npc npc) {
		return switch (npc.role()) {
			case Npc.NURSE -> "nurse";
			case Npc.CLERK -> "clerk";
			case Npc.PERSON -> {
				Trainers.Person p = Trainers.PEOPLE.get(npc.key());
				yield p == null ? "youngster" : p.sprite();
			}
			default -> {
				Trainers.Trainer t = Trainers.get(npc.key());
				yield t == null ? "youngster" : t.sprite();
			}
		};
	}

	@Override
	public Identifier getTextureLocation(State s) {
		return SKINS.computeIfAbsent(s.sprite, k -> {
			Identifier id = Retromon.id("textures/entity/npc/" + k + ".png");
			return Minecraft.getInstance().getResourceManager().getResource(id).isPresent() ? id : FALLBACK;
		});
	}
}
