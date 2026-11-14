package com.insidethecube.orbitarena.client;

import com.insidethecube.orbitarena.OrbitArena;
import com.insidethecube.orbitarena.entity.Fighter;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.zombie.ZombieModel;
import net.minecraft.client.renderer.entity.AbstractZombieRenderer;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.Items;

/**
 * Draws the arena fighters: a player-shaped body (the zombie's model, without its stiff arms) in one of
 * eight fighter looks, with armour, weapons, a raised shield when blocking and spear poses.
 */
public class FighterRenderer extends AbstractZombieRenderer<Fighter, FighterRenderer.State, ZombieModel<FighterRenderer.State>> {
	public static final int LOOKS = 8;
	private static final Identifier[] TEXTURES = new Identifier[LOOKS];

	static {
		for (int i = 0; i < LOOKS; i++) TEXTURES[i] = OrbitArena.id("textures/entity/fighter/fighter_" + i + ".png");
	}

	public static class State extends ZombieRenderState {
		public int look;
	}

	public FighterRenderer(EntityRendererProvider.Context ctx) {
		super(ctx, new ZombieModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE)), new ZombieModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE_BABY)),
				ArmorModelSet.bake(ModelLayers.ZOMBIE_ARMOR, ctx.getModelSet(), ZombieModel::new),
				ArmorModelSet.bake(ModelLayers.ZOMBIE_BABY_ARMOR, ctx.getModelSet(), ZombieModel::new));
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(Fighter f, State state, float partial) {
		super.extractRenderState(f, state, partial);
		// Fighters hold their arms like players, not reaching out like zombies.
		state.isAggressive = false;
		state.look = Math.floorMod(f.getUUID().hashCode(), LOOKS);
	}

	@Override
	public Identifier getTextureLocation(State state) {
		return TEXTURES[state.look];
	}

	@Override
	protected HumanoidModel.ArmPose getArmPose(Fighter f, HumanoidArm arm) {
		InteractionHand hand = arm == f.getMainArm() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
		if (f.isUsingItem() && f.getUsedItemHand() == hand && f.getItemInHand(hand).is(Items.SHIELD)) return HumanoidModel.ArmPose.BLOCK;
		HumanoidModel.ArmPose pose = super.getArmPose(f, arm);
		if (pose == HumanoidModel.ArmPose.EMPTY && !f.getItemInHand(hand).isEmpty()) return HumanoidModel.ArmPose.ITEM;
		return pose;
	}
}
