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
 * texture). Each fan keeps picking something to do (FanRenderer chooses, see choose): standing about,
 * clapping, waving, cheering with both arms up and jumping, pumping a fist, pointing at you, hands on head
 * when you take a big hit. Sign holders pump their signs. The Mexican wave lifts everyone it passes.
 */
public class FanModel extends HumanoidModel<FanRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(OrbitArena.id("fan"), "main");
	public static final int IDLE = 0, CLAP = 1, WAVE = 2, CHEER = 3, PUMP = 4, POINT = 5, HEADS = 6;

	/** Signs from this one up are the big boards (INSID3, TH3, CUB3). */
	public static final int BIG = 6;
	private final ModelPart sign, big;

	public FanModel(ModelPart root) {
		super(root);
		this.sign = this.rightArm.getChild("sign");
		this.big = this.rightArm.getChild("big_sign");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0);
		PartDefinition arm = mesh.getRoot().getChild("right_arm");
		// A board 14 x 8 pixels past the hand, reaching across to the other hand when both arms are up.
		arm.addOrReplaceChild("sign", CubeListBuilder.create().texOffs(0, 32).addBox(-2.0F, 10.0F, -0.5F, 14.0F, 8.0F, 1.0F), PartPose.ZERO);
		// A bigger board, 26 x 10, centred between the hands: three side by side spell the brand.
		arm.addOrReplaceChild("big_sign", CubeListBuilder.create().texOffs(0, 48).addBox(-8.0F, 10.0F, -0.5F, 26.0F, 10.0F, 1.0F), PartPose.ZERO);
		return LayerDefinition.create(mesh, 64, 64);
	}

	/**
	 * What a fan does next, from a random number r (0 to 1): the livelier the crowd (energy), the more it
	 * claps, waves and cheers; when the crowd cheers most of it cheers; when it groans, hands go to heads.
	 */
	public static int choose(double r, float energy, float cheering, float groan, boolean sign) {
		if (groan > 0.35F && r < groan * 0.7F) return HEADS;
		if (cheering > 0.55F && r < cheering * 0.85F) return CHEER;
		if (sign) return IDLE;
		float e = Math.min(1, energy);
		float[] w = {1.1F - e, 0.3F + 0.5F * e, 0.15F + 0.3F * e, 0.03F + 1.1F * e * e, 0.08F + 0.45F * e, 0.07F};
		float total = 0;
		for (float x : w) total += x;
		double pick = (r * 7.31 % 1.0) * total;
		for (int i = 0; i < w.length; i++) {
			pick -= w[i];
			if (pick < 0) return i;
		}
		return IDLE;
	}

	@Override
	public void setupAnim(FanRenderState s) {
		super.setupAnim(s);
		float t = s.time, e = Mth.clamp(s.energy, 0, 1), k = s.blend;
		boolean holding = s.sign >= 0;
		this.sign.visible = holding && s.sign < BIG;
		this.big.visible = holding && s.sign >= BIG;

		// Standing about: a little sway, arms loose.
		this.body.yRot = Mth.sin(t * 0.05F) * 0.08F;
		float rx = Mth.sin(t * 0.07F) * 0.08F, ry = 0, rz = 0.06F;
		float lx = -Mth.sin(t * 0.07F) * 0.08F, ly = 0, lz = -0.06F;
		float bob = 0;

		// The chosen move, eased in and out.
		float ax = rx, ay = ry, az = rz, bx = lx, by = ly, bz = lz, jump = 0;
		if (holding && s.action != HEADS && s.action != CHEER) {
			// The sign held high in both hands, pumped up and down, swaying.
			float pump = Mth.sin(t * 0.3F) * 0.18F * (0.5F + e);
			ax = -2.95F + pump;
			bx = -2.95F + pump;
			az = 0.18F + Mth.sin(t * 0.12F) * 0.06F;
			bz = -0.18F + Mth.sin(t * 0.12F) * 0.06F;
			jump = e > 0.45F ? (e - 0.45F) * 1.6F : 0;
			k = 1;
		} else {
			switch (s.action) {
				case CLAP -> {
					float c = (Mth.sin(t * 1.7F) + 1) / 2;
					ax = -1.25F;
					bx = -1.25F;
					ay = -(0.2F + 0.35F * c);
					by = 0.2F + 0.35F * c;
					az = 0;
					bz = 0;
					jump = e * 0.25F;
				}
				case WAVE -> {
					ax = -2.8F;
					az = -0.3F + Mth.sin(t * 0.9F) * 0.45F;
				}
				case CHEER -> {
					ax = -2.9F + Mth.sin(t * 0.6F) * 0.12F;
					bx = -2.9F - Mth.sin(t * 0.6F) * 0.12F;
					az = -0.38F;
					bz = 0.38F;
					jump = 0.6F + 0.4F * e;
				}
				case PUMP -> {
					ax = -2.35F + Mth.sin(t * 0.8F) * 0.55F;
					az = -0.15F;
					bx = -0.3F;
					jump = 0.25F * e;
				}
				case POINT -> {
					ax = -1.55F;
					ay = this.head.yRot * 0.8F;
					az = 0;
				}
				case HEADS -> {
					float g = Math.max(0.6F, Mth.clamp(s.groan, 0, 1));
					ax = -2.6F;
					bx = -2.6F;
					az = 0.75F * g;
					bz = -0.75F * g;
					this.head.xRot = Mth.lerp(k, this.head.xRot, 0.35F * g);
				}
				default -> {
				}
			}
		}
		rx = Mth.lerp(k, rx, ax);
		ry = Mth.lerp(k, ry, ay);
		rz = Mth.lerp(k, rz, az);
		lx = Mth.lerp(k, lx, bx);
		ly = Mth.lerp(k, ly, by);
		lz = Mth.lerp(k, lz, bz);
		bob = -Math.abs(Mth.sin(t * 0.45F)) * 3.5F * jump * k;

		// The Mexican wave: up on its feet, arms straight up, as it passes.
		if (s.wave > 0 && s.action != HEADS) {
			float w = s.wave;
			rx = Mth.lerp(w, rx, -3.05F);
			lx = Mth.lerp(w, lx, -3.05F);
			rz = Mth.lerp(w, rz, holding ? 0.18F : -0.12F);
			lz = Mth.lerp(w, lz, holding ? -0.18F : 0.12F);
			ry = Mth.lerp(w, ry, 0);
			ly = Mth.lerp(w, ly, 0);
			bob = Math.min(bob, -3.0F * w);
		}

		// Looking through a spyglass: the right hand up to the eye, following the head.
		if (s.spyglass && s.action != HEADS) {
			rx = this.head.xRot - 1.5F;
			ry = this.head.yRot - 0.3F;
			rz = 0;
		}

		this.rightArm.xRot = rx;
		this.rightArm.yRot = ry;
		this.rightArm.zRot = rz;
		this.leftArm.xRot = lx;
		this.leftArm.yRot = ly;
		this.leftArm.zRot = lz;
		for (ModelPart p : new ModelPart[] {head, hat, body, rightArm, leftArm, rightLeg, leftLeg}) p.y += bob;
	}
}
