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
		/** Hopping for joy (you, on the battle stage, after a win). */
		float hop;
	}

	public NpcRenderer(EntityRendererProvider.Context ctx) {
		super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER)), 0.5F);
		// Your cap and bag, when it's you on the battle stage.
		addLayer(new GearLayer<>(this));
	}

	@Override
	public net.minecraft.world.phys.Vec3 getRenderOffset(State s) {
		return super.getRenderOffset(s).add(0, s.hop, 0);
	}

	/** You as you look in the world: your clothes if you've chosen some, else the boy or the girl. */
	static Identifier hero() {
		boolean girl = "girl".equals(ClientState.data.gender);
		String outfit = ClientState.data.outfit;
		if (outfit != null && !outfit.isEmpty() && Outfit.ensure(outfit, girl)) return Outfit.HERO;
		return Retromon.id(girl ? "textures/entity/player/girl_bare.png" : "textures/entity/player/boy_bare.png");
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
		s.hop = npc.cheer > 0 ? 0.45F * Math.abs(net.minecraft.util.Mth.sin((40 - npc.cheer + partial) / 20F * (float) Math.PI * 2)) : 0;
	}

	static String sprite(Npc npc) {
		String key = npc.key();
		// Your rival (wherever he turns up), and you yourself (on the battle stage).
		if (key.startsWith("rival")) return "blue";
		if (key.equals("hero")) return "hero";
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
		if (s.sprite.equals("hero")) return hero();
		return SKINS.computeIfAbsent(s.sprite, k -> {
			Identifier id = Retromon.id("textures/entity/npc/" + k + ".png");
			return Minecraft.getInstance().getResourceManager().getResource(id).isPresent() ? id : FALLBACK;
		});
	}
}
