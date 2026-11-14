package com.insidethecube.orbitarena.client;

import com.insidethecube.orbitarena.OrbitArena;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * A fan: a humanoid with a sign it can hold up (a board on its right hand, its texture in the fan's own
 * texture). Standing about, it sways; when the crowd cheers, arms go up and it jumps; signs go up and
 * down; when you take a big hit, hands go to heads.
 */
public class FanModel extends HumanoidModel<FanRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(OrbitArena.id("fan"), "main");

	private final ModelPart sign;

	public FanModel(ModelPart root) {
		super(root);
		this.sign = this.rightArm.getChild("sign");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0);
		PartDefinition arm = mesh.getRoot().getChild("right_arm");
		// A board 14 x 8 pixels past the hand, reaching across to the other hand when both arms are up.
		arm.addOrReplaceChild("sign", CubeListBuilder.create().texOffs(0, 32).addBox(-2.0F, 10.0F, -0.5F, 14.0F, 8.0F, 1.0F), PartPose.ZERO);
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(FanRenderState s) {
		super.setupAnim(s);
		float t = s.ageInTicks + s.phase * 40;
		float e = Mth.clamp(s.excitement, 0, 1), g = Mth.clamp(s.groan, 0, 1);
		boolean holding = s.sign >= 0;
		this.sign.visible = holding;
		// Jumping with the cheer (a few pixels).
		float bob = -Math.abs(Mth.sin(t * 0.45F)) * 3.5F * e;
		for (ModelPart p : new ModelPart[] {head, hat, body, rightArm, leftArm, rightLeg, leftLeg}) p.y += bob;
		this.body.yRot = Mth.sin(t * 0.05F) * 0.06F;
		this.head.xRot = -0.1F + Mth.sin(t * 0.07F) * 0.05F;
		if (g > 0.05F && !holding) {
			// Hands on heads.
			rightArm.xRot = -2.6F;
			leftArm.xRot = -2.6F;
			rightArm.zRot = 0.75F * g;
			leftArm.zRot = -0.75F * g;
			head.xRot = 0.35F * g;
			return;
		}
		float up = Mth.clamp(e * 1.3F + (holding ? 0.85F : 0), 0, 1);
		float wave = Mth.sin(t * 0.55F) * 0.35F * e;
		if (holding) {
			// Sign held high in both hands, pumped up and down.
			float pump = Mth.sin(t * 0.3F) * 0.15F * (0.4F + e);
			rightArm.xRot = Mth.lerp(up, rightArm.xRot, -2.95F) + pump;
			leftArm.xRot = Mth.lerp(up, leftArm.xRot, -2.95F) + pump;
			rightArm.zRot = 0.18F;
			leftArm.zRot = -0.18F;
			rightArm.yRot = 0.0F;
			leftArm.yRot = 0.0F;
		} else {
			rightArm.xRot = Mth.lerp(up, rightArm.xRot, -2.8F) + wave;
			leftArm.xRot = Mth.lerp(up, leftArm.xRot, -2.8F) - wave;
			rightArm.zRot = Mth.lerp(up, rightArm.zRot, 0.35F) + Mth.cos(t * 0.5F) * 0.2F * e;
			leftArm.zRot = Mth.lerp(up, leftArm.zRot, -0.35F) - Mth.cos(t * 0.5F) * 0.2F * e;
		}
	}
}
