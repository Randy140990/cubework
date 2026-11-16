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
 * texture). Each fan keeps picking something to do (FanRenderer asks choose): sitting watching, chatting to
 * the next fan, eating a snack, clapping, waving, cheering with both arms up, jumping, pumping a fist,
 * pointing, flossing, dabbing, stretching, waving a flag; when its side is scored on, booing, hands on head,
 * a hand over its face. Sign holders pump their signs. Calm fans who like to sit sit; anything lively gets
 * them on their feet, and so does the Mexican wave.
 */
public class FanModel extends HumanoidModel<FanRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(OrbitArena.id("fan"), "main");
	public static final int IDLE = 0, CLAP = 1, WAVE = 2, CHEER = 3, PUMP = 4, POINT = 5, HEADS = 6, SIT = 7, FLOSS = 8, DAB = 9, EAT = 10, BOO = 11, JUMP = 12,
			CHAT = 13, FACEPALM = 14, STRETCH = 15, FLAG = 16;

	/** The small signs (GO!, WOW, GG!, WIN, a heart, a star, BOO, OMG, LOL, 10!, GO! on blue, GO! on red), then the big boards. */
	public static final int SIGN_BOO = 6, SIGN_GO_BLUE = 10, SIGN_GO_RED = 11;
	/** Signs from this one up are the big boards (INSID3, TH3, CUB3, BLUE!, RED!, GOAL!). */
	public static final int BIG = 12, BIG_BLUE = BIG + 3, BIG_RED = BIG + 4, BIG_GOAL = BIG + 5;
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

	/** Whether a move can be done sitting down. */
	public static boolean seated(int action) {
		return action == IDLE || action == SIT || action == CHAT || action == EAT || action == CLAP || action == POINT || action == HEADS || action == FACEPALM
				|| action == BOO;
	}

	private static int pick(double r, float[] weights, int[] moves) {
		float total = 0;
		for (float x : weights) total += x;
		double p = r * total;
		for (int i = 0; i < weights.length; i++) {
			p -= weights[i];
			if (p < 0) return moves[i];
		}
		return moves[0];
	}

	/**
	 * What a fan does next, from two random numbers (0 to 1). Its side scored (cheer): it goes wild, a few
	 * of them silly about it. Its side was scored on (boo): it boos, or holds its head. The crowd groans:
	 * hands to heads. Otherwise as lively as the crowd: sitting (if it likes to), chatting, a snack, clapping,
	 * waving, now and then something daft.
	 */
	public static int choose(double r, double r2, float energy, float cheer, float boo, float groan, boolean sign, boolean snack, boolean flag, boolean sits) {
		if (boo > 0.3F && r < boo * 0.85F) return pick(r2, new float[] {0.55F, 0.25F, 0.2F}, new int[] {BOO, HEADS, FACEPALM});
		if (groan > 0.35F && r < groan * 0.7F) return r2 < 0.8 ? HEADS : FACEPALM;
		if (cheer > 0.5F && r < cheer * 0.9F) {
			if (flag) return FLAG;
			if (sign) return IDLE;
			return pick(r2, new float[] {0.32F, 0.16F, 0.15F, 0.08F, 0.12F, 0.1F, 0.07F}, new int[] {CHEER, JUMP, FLOSS, DAB, PUMP, CLAP, WAVE});
		}
		if (sign) return IDLE;
		if (flag && r2 < 0.5) return FLAG;
		if (snack && r2 < 0.4) return EAT;
		float e = Math.min(1, energy);
		float calm = 1.1F - e;
		return pick(r2, new float[] {sits ? calm * 0.4F : calm, sits ? calm * 0.9F : 0, 0.18F + 0.15F * calm, 0.3F + 0.5F * e, 0.15F + 0.3F * e,
				0.03F + 1.1F * e * e, 0.08F + 0.45F * e, 0.07F, 0.03F, 0.025F, 0.015F, 0.03F},
				new int[] {IDLE, SIT, CHAT, CLAP, WAVE, CHEER, PUMP, POINT, STRETCH, FLOSS, DAB, JUMP});
	}

	@Override
	public void setupAnim(FanRenderState s) {
		super.setupAnim(s);
		float t = s.time, e = Mth.clamp(s.energy, 0, 1), k = s.blend;
		boolean holding = s.sign >= 0;
		this.sign.visible = holding && s.sign < BIG;
		this.big.visible = holding && s.sign >= BIG;

		// Standing (or sitting) about: a little sway, arms loose.
		this.body.yRot = Mth.sin(t * 0.05F) * 0.08F;
		this.body.zRot = 0;
		float rx = Mth.sin(t * 0.07F) * 0.08F, ry = 0, rz = 0.06F;
		float lx = -Mth.sin(t * 0.07F) * 0.08F, ly = 0, lz = -0.06F;
		float headX = this.head.xRot, headY = this.head.yRot;

		// The chosen move, eased in and out.
		float ax = rx, ay = ry, az = rz, bx = lx, by = ly, bz = lz, jump = 0, hx = headX, hy = headY, legSwing = 0, hips = 0;
		if (holding && s.action != HEADS && s.action != BOO && s.action != FACEPALM && s.action != CHEER) {
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
				case JUMP -> {
					// Bouncing on the spot, both fists going.
					ax = -2.6F + Mth.sin(t * 0.9F) * 0.5F;
					bx = -2.6F - Mth.sin(t * 0.9F) * 0.5F;
					az = -0.5F;
					bz = 0.5F;
					jump = 1.3F;
				}
				case PUMP -> {
					ax = -2.35F + Mth.sin(t * 0.8F) * 0.55F;
					az = -0.15F;
					bx = -0.3F;
					jump = 0.25F * e;
				}
				case POINT -> {
					ax = -1.55F;
					ay = headY * 0.8F;
					az = 0;
				}
				case HEADS -> {
					float g = Math.max(0.6F, Mth.clamp(Math.max(s.groan, s.boo), 0, 1));
					ax = -2.6F;
					bx = -2.6F;
					az = 0.75F * g;
					bz = -0.75F * g;
					hx = 0.35F * g;
				}
				case FLOSS -> {
					// Arms swung side to side, one in front and one behind, the hips going the other way.
					float f = Mth.sin(t * 0.85F);
					float c = Mth.cos(t * 0.85F);
					az = 0.25F + 0.55F * f;
					bz = -0.25F + 0.55F * f;
					ax = 0.4F * c;
					bx = -0.4F * c;
					hips = -0.18F * f;
					hy = headY * 0.3F;
				}
				case DAB -> {
					// Head down into one elbow, the other arm thrown out and up: every second or so the other way.
					float side = Mth.sin(t * 0.08F) >= 0 ? 1 : -1;
					if (side > 0) {
						bx = -1.7F;
						by = 0.95F;
						bz = 0;
						ax = -2.3F;
						az = -1.1F;
					} else {
						ax = -1.7F;
						ay = -0.95F;
						az = 0;
						bx = -2.3F;
						bz = 1.1F;
					}
					hx = 0.55F;
					hy = side * -0.55F;
				}
				case EAT -> {
					// The snack to its mouth, chewing.
					float bite = Mth.sin(t * 0.25F) > 0.3F ? 1 : 0;
					ax = -1.75F - 0.25F * bite;
					ay = -0.45F;
					az = 0;
					hx = headX * 0.3F + Mth.sin(t * 1.6F) * 0.06F;
					hy = headY * 0.3F;
				}
				case BOO -> {
					// Hands cupped round its mouth, then both arms jabbing down, head shaking.
					if (Mth.sin(t * 0.09F) > 0) {
						ax = -1.85F;
						ay = -0.55F;
						bx = -1.85F;
						by = 0.55F;
						az = 0;
						bz = 0;
					} else {
						ax = -1.1F + Mth.sin(t * 0.7F) * 0.3F;
						bx = -1.1F + Mth.sin(t * 0.7F) * 0.3F;
						az = 0.1F;
						bz = -0.1F;
					}
					hy = headY * 0.4F + Mth.sin(t * 0.55F) * 0.3F;
				}
				case CHAT -> {
					// Turned to the fan beside it, a hand going as it talks.
					float side = s.look % 2 == 0 ? 1 : -1;
					hy = side * 0.9F + Mth.sin(t * 0.13F) * 0.1F;
					hx = 0.05F;
					ax = -0.55F + Mth.sin(t * 0.35F) * 0.3F;
					ay = side * -0.3F;
				}
				case FACEPALM -> {
					bx = -2.05F;
					by = 0.45F;
					bz = 0;
					hx = 0.3F;
				}
				case STRETCH -> {
					ax = -2.95F;
					bx = -2.95F;
					az = -0.55F;
					bz = 0.55F;
					hx = -0.25F;
				}
				case FLAG -> {
					// The flag held high, waved from side to side.
					ax = -2.75F;
					az = Mth.sin(t * 0.35F) * 0.45F;
					bx = -0.5F;
					jump = 0.35F * e;
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
		this.head.xRot = Mth.lerp(k, headX, hx);
		this.head.yRot = Mth.lerp(k, headY, hy);
		this.body.zRot = hips * k;
		float bob = -Math.abs(Mth.sin(t * 0.45F)) * 3.5F * jump * k;

		// Sitting: down on its seat, legs out in front (fans who like to sit, when it's calm and the move is a
		// sitting one); up on its feet for anything lively.
		float sit = s.sitBase ? (seated(s.action) || holding ? 1 : 1 - k) : 0;

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
			sit *= 1 - w;
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
		if (sit > 0) {
			this.rightLeg.xRot = Mth.lerp(sit, this.rightLeg.xRot, -1.45F);
			this.leftLeg.xRot = Mth.lerp(sit, this.leftLeg.xRot, -1.45F);
			this.rightLeg.yRot = Mth.lerp(sit, this.rightLeg.yRot, 0.12F);
			this.leftLeg.yRot = Mth.lerp(sit, this.leftLeg.yRot, -0.12F);
			bob += 10 * sit;
		}
		if (legSwing != 0) this.rightLeg.zRot = legSwing;
		for (ModelPart p : new ModelPart[] {head, body, rightArm, leftArm, rightLeg, leftLeg}) p.y += bob;
	}
}
