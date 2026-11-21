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
		/** How far through a Poké Ball throw (0 for none). */
		float throwK;
		/** Celebrating a win: how (-1 for not), and ticks in. */
		int dance = -1;
		float danceT;
		/** Sitting on a chair. */
		boolean seated;
	}

	/** The figure: the usual walk and stand, with a proper overarm throw and your celebrations on top. */
	static class Figure extends HumanoidModel<State> {
		Figure(net.minecraft.client.model.geom.ModelPart root) {
			super(root);
			// Corners just softened, as the Person Viewer draws everyone.
			Soft.apply(root, true, Soft.PEOPLE);
		}

		@Override
		public void setupAnim(State s) {
			super.setupAnim(s);
			if (s.seated) {
				// As the Interior Viewer seats people: thighs straight out on the seat, a little apart; hands forward
				// on the desk.
				rightLeg.xRot = leftLeg.xRot = -(float) Math.PI / 2 + 0.08F;
				rightLeg.yRot = 0.08F;
				leftLeg.yRot = -0.08F;
				rightLeg.zRot = leftLeg.zRot = 0;
				rightArm.xRot = leftArm.xRot = -0.5F;
			}
			if (s.throwK > 0) throwing(s.throwK);
			switch (s.dance) {
				case 0 -> armsUp(s.danceT);
				case 1 -> floss(s.danceT);
				case 2 -> pump(s.danceT);
				case 3 -> wave(s.danceT);
				default -> {
				}
			}
		}

		private static float ease(float u) {
			u = Math.max(0, Math.min(1, u));
			return u * u * (3 - 2 * u);
		}

		/** Turned at the waist (the arms going round with it). */
		private void twist(float yaw) {
			body.yRot = yaw;
			rightArm.z = net.minecraft.util.Mth.sin(yaw) * 5;
			rightArm.x = -net.minecraft.util.Mth.cos(yaw) * 5;
			leftArm.z = -net.minecraft.util.Mth.sin(yaw) * 5;
			leftArm.x = net.minecraft.util.Mth.cos(yaw) * 5;
			rightArm.yRot = yaw;
			leftArm.yRot = yaw;
		}

		/** Overarm: the arm back over the shoulder, then over and through, then back down to the side. */
		private void throwing(float k) {
			float arm, turn;
			if (k < 0.25F) {
				float u = ease(k / 0.25F);
				arm = -3.0F * u;
				turn = 0.3F * u;
			} else if (k < 0.45F) {
				float u = (k - 0.25F) / 0.2F;
				arm = -3.0F + 2.4F * u;
				turn = 0.3F - 0.55F * u;
			} else {
				float u = ease((k - 0.45F) / 0.55F);
				arm = -0.6F * (1 - u);
				turn = -0.25F * (1 - u);
			}
			twist(turn);
			rightArm.xRot = arm;
			rightArm.zRot = 0.08F;
			leftArm.xRot = -0.6F * (float) Math.sin(Math.min(1, k) * Math.PI);
			leftArm.zRot = -0.1F;
		}

		private static float phase(float t, float perSecond) {
			return t / 20F * (float) Math.PI * 2 * perSecond;
		}

		/** Both arms up, hopping. */
		private void armsUp(float t) {
			float sway = 0.12F * net.minecraft.util.Mth.sin(phase(t, 2.2F));
			rightArm.xRot = -2.8F;
			leftArm.xRot = -2.8F;
			rightArm.zRot = 0.3F + sway;
			leftArm.zRot = -0.3F + sway;
			rightArm.yRot = leftArm.yRot = 0;
			head.xRot = -0.25F;
		}

		/** The floss: both arms straight and swinging side to side, one in front and one behind, the hips the other way. */
		private void floss(float t) {
			float ph = phase(t, 1.8F);
			float side = net.minecraft.util.Mth.sin(ph), front = net.minecraft.util.Mth.cos(ph);
			rightArm.xRot = -0.3F * front;
			leftArm.xRot = 0.3F * front;
			rightArm.yRot = leftArm.yRot = 0;
			rightArm.zRot = 0.6F * side + 0.1F;
			leftArm.zRot = 0.6F * side - 0.1F;
			rightLeg.zRot = -0.16F * side;
			leftLeg.zRot = -0.16F * side;
			body.zRot = -0.06F * side;
			head.zRot = 0.08F * side;
		}

		/** A fist pump: the right fist punching the air, the other hand at the hip. */
		private void pump(float t) {
			float up = Math.abs(net.minecraft.util.Mth.sin(phase(t, 1.6F)));
			rightArm.xRot = -2.4F - 0.55F * up;
			rightArm.yRot = 0;
			rightArm.zRot = -0.1F;
			leftArm.xRot = -0.25F;
			leftArm.yRot = 0;
			leftArm.zRot = -0.65F;
			head.xRot = -0.2F * up;
		}

		/** A wave: the right arm up, the hand going side to side. */
		private void wave(float t) {
			rightArm.xRot = -2.75F;
			rightArm.yRot = 0;
			rightArm.zRot = 0.1F + 0.35F * net.minecraft.util.Mth.sin(phase(t, 1.5F));
			head.zRot = 0.06F * net.minecraft.util.Mth.sin(phase(t, 0.75F));
		}
	}

	public NpcRenderer(EntityRendererProvider.Context ctx) {
		super(ctx, new Figure(ctx.bakeLayer(ModelLayers.PLAYER)), 0.5F);
		// Your cap and bag, when it's you on the battle stage.
		addLayer(new GearLayer<>(this));
	}

	@Override
	public net.minecraft.world.phys.Vec3 getRenderOffset(State s) {
		// Sat down: the hips at the chair's seat (0.56 up, as the Interior Viewer has it) instead of 0.75.
		return super.getRenderOffset(s).add(0, s.hop - (s.seated ? 0.19 : 0), 0);
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
		s.seated = npc.seated();
		float in = Npc.CHEER - npc.cheer + partial;
		s.dance = npc.cheer > 0 ? npc.dance : -1;
		s.danceT = in;
		// Hopping with the arms up and with the fist pump.
		s.hop = npc.cheer > 0 && (npc.dance == 0 || npc.dance == 2) ? 0.4F * Math.abs(net.minecraft.util.Mth.sin(in / 20F * (float) Math.PI * 2)) : 0;
		s.throwK = npc.throwing > 0 ? 1 - Math.max(0, npc.throwing - partial) / Npc.THROW : 0;
	}

	/** Each look's other versions (the Person Viewer's): a family's skins that exist, the look itself first. */
	private static final java.util.Map<String, java.util.List<String>> FAMILIES = new java.util.HashMap<>();
	private static final java.util.Map<String, String[]> NAMED = java.util.Map.of("nurse", new String[] {"nurse", "nurse_2", "nurse_m", "nurse_m2"}, "clerk",
			new String[] {"clerk", "clerk_2", "clerk_f", "clerk_f2"}, "officer_m", new String[] {"officer_m", "officer_m2", "officer_f2"}, "rocket",
			new String[] {"rocket", "rocket_2", "rocket_3", "rocket_4", "rocket_5", "rocket_6", "rocket_7", "rocket_8", "rocket_9", "rocket_f"});

	private static java.util.List<String> family(String base) {
		return FAMILIES.computeIfAbsent(base, b -> {
			java.util.List<String> out = new java.util.ArrayList<>();
			String[] named = NAMED.get(b);
			java.util.List<String> want = new java.util.ArrayList<>();
			if (named != null) want.addAll(java.util.List.of(named));
			else {
				want.add(b);
				for (int i = 2; i <= 9; i++) want.add(b + "_" + i);
			}
			var rm = Minecraft.getInstance().getResourceManager();
			for (String w : want) if (rm.getResource(Retromon.id("textures/entity/npc/" + w + ".png")).isPresent()) out.add(w);
			if (out.isEmpty()) out.add(b);
			return out;
		});
	}

	/** One of a look's versions, the same every time for the same person (and, behind a counter, the same in each town). */
	private static String variant(Npc npc, String base) {
		java.util.List<String> f = family(base);
		if (f.size() <= 1) return base;
		String seed = npc.key() + (npc.role() == Npc.NURSE || npc.role() == Npc.CLERK ? "@" + ClientState.data.place : "");
		return f.get(Math.floorMod(seed.hashCode() * 31 + 7, f.size()));
	}

	static String sprite(Npc npc) {
		String base = baseSprite(npc);
		if (base.equals("blue") || base.equals("hero")) return base;
		return variant(npc, base);
	}

	private static String baseSprite(Npc npc) {
		String key = npc.key();
		// Your rival (wherever he turns up), and you yourself (on the battle stage).
		if (key.startsWith("rival")) return "blue";
		if (key.equals("hero")) return "hero";
		// TEAM ROCKET in Viridian: JESSIE (the one you battle) and JAMES.
		if (key.equals("rocket_viridian")) return "jessie";
		if (key.equals("rocket_james")) return "james";
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
