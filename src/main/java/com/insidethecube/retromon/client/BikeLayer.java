package com.insidethecube.retromon.client;

import com.insidethecube.retromon.Retromon;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * (0.57, 15:51: "is that the bike gang? Do they need to be on physical bikes? ... I would assume so") The BIKERs and
 * CUE BALLs on their motorbikes: a chunky bike under them (two fat wheels, the tank, the seat, the engine, the exhaust,
 * the forks and the bars), drawn in the model's own space (the ground at 24, the front towards -z), the rider sat on it
 * with their hands on the bars (NpcRenderer's pose).
 */
public class BikeLayer extends RenderLayer<NpcRenderer.State, HumanoidModel<NpcRenderer.State>> {
	private static ModelPart bike;
	private static final Identifier TEX = Retromon.id("textures/entity/npc_bike.png");

	public BikeLayer(RenderLayerParent<NpcRenderer.State, HumanoidModel<NpcRenderer.State>> parent) {
		super(parent);
		build();
	}

	/** The texture's four quarters: 0 the dark tyres, 1 the frame's red, 2 the black seat and grips, 3 the chrome. */
	private static CubeListBuilder c(int quarter) {
		return CubeListBuilder.create().texOffs((quarter % 2) * 32, (quarter / 2) * 32);
	}

	private static void build() {
		if (bike != null) return;
		MeshDefinition m = new MeshDefinition();
		PartDefinition r = m.getRoot();
		// The wheels (round enough: three boxes crossed), front at z -9, back at z +9, 10 across.
		for (float wz : new float[] {-9F, 9F}) {
			String w = wz < 0 ? "front" : "back";
			r.addOrReplaceChild(w + "_a", c(0).addBox(-1.5F, 14F, wz - 3F, 3, 10, 6), PartPose.ZERO);
			r.addOrReplaceChild(w + "_b", c(0).addBox(-1.5F, 16F, wz - 5F, 3, 6, 10), PartPose.ZERO);
			r.addOrReplaceChild(w + "_c", c(0).addBox(-1.5F, 15F, wz - 4F, 3, 8, 8), PartPose.ZERO);
			r.addOrReplaceChild(w + "_hub", c(3).addBox(-1.8F, 18F, wz - 1F, 3.6F, 2, 2), PartPose.ZERO);
		}
		// The frame: the tank in front of the seat, the engine under it, the back mudguard, the exhaust.
		r.addOrReplaceChild("tank", c(1).addBox(-2.5F, 10.5F, -5.5F, 5, 4, 7), PartPose.ZERO);
		r.addOrReplaceChild("seat", c(2).addBox(-2.5F, 11F, 1.5F, 5, 2, 7), PartPose.ZERO);
		r.addOrReplaceChild("body", c(1).addBox(-2F, 13F, 1F, 4, 3, 9), PartPose.ZERO);
		r.addOrReplaceChild("engine", c(3).addBox(-2.5F, 14.5F, -4F, 5, 4, 6), PartPose.ZERO);
		r.addOrReplaceChild("guard", c(1).addBox(-2F, 12.5F, 8F, 4, 2, 5), PartPose.ZERO);
		r.addOrReplaceChild("exhaust", c(3).addBox(2.2F, 17F, -2F, 1.5F, 1.5F, 11), PartPose.ZERO);
		// The forks from the hub up to the bars, raked back; the headlamp; the bars across.
		r.addOrReplaceChild("forks", c(3).addBox(-2F, -9F, -0.75F, 4, 10, 1.5F), PartPose.offsetAndRotation(0, 19F, -9F, -0.35F, 0, 0));
		r.addOrReplaceChild("lamp", c(3).addBox(-1.5F, 8.5F, -8.6F, 3, 3, 2), PartPose.ZERO);
		r.addOrReplaceChild("bars", c(2).addBox(-5F, 8F, -6.5F, 10, 1.2F, 1.2F), PartPose.ZERO);
		bike = LayerDefinition.create(m, 64, 64).bakeRoot();
		Soft.apply(bike, true, Soft.GEAR);
	}

	@Override
	public void submit(PoseStack pose, SubmitNodeCollector out, int light, NpcRenderer.State s, float yRot, float xRot) {
		if (!s.bike || s.isInvisible) return;
		pose.pushPose();
		getParentModel().root().translateAndRotate(pose);
		out.submitModelPart(bike, pose, RenderTypes.entityCutoutNoCull(TEX), light, OverlayTexture.NO_OVERLAY, null, false, false, -1, null, 0);
		pose.popPose();
	}
}
