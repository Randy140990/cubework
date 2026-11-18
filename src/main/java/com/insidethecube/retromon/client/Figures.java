package com.insidethecube.retromon.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * Blocky 3D figures on a screen, with no world needed: people (a skin on Minecraft's player model) and Pokémon
 * (their own models), posed and moving, all drawn into one picture over an area of the screen. Professor Oak in
 * the intro, the boy and the girl waving when you point at them, the three starters hopping, the Pokémon on its
 * Pokédex page. One picture for all of them (the game keeps one picture per kind of thing drawn this way).
 */
public final class Figures {
	private Figures() {}

	private static final int FULL_BRIGHT = 15728880;

	/** One figure: where it stands (feet), how tall it is (screen pixels) and what it's doing. */
	public static final class Fig {
		final Identifier skin;
		final String species;
		final float cx, feet, tall;
		float facing, pitch, wave, talk, hop, spin, look, lookDown, nod, seconds, squash = 1;
		/** A gesture (people): one of the EMOTES, and how far into it (seconds). */
		int emote = -1;
		float emoteSecs;
		/** A one-off animation (Pokémon): cry, recoil..., and how far into it (seconds). */
		String action;
		float actionSecs;

		Fig(Identifier skin, String species, float cx, float feet, float tall) {
			this.skin = skin;
			this.species = species;
			this.cx = cx;
			this.feet = feet;
			this.tall = tall;
		}

		/** Turned to face the screen's right (positive) or left (negative), in degrees. */
		public Fig facing(float degrees) {
			facing = degrees;
			return this;
		}

		/** Tipped towards you (positive: you see more of the top). */
		public Fig pitch(float degrees) {
			pitch = degrees;
			return this;
		}

		/** Waving (0 to 1): the right arm up and waving. */
		public Fig wave(float amount) {
			wave = amount;
			return this;
		}

		/** Talking (0 to 1): a hand out, a nod. */
		public Fig talk(float amount) {
			talk = amount;
			return this;
		}

		/** Off the ground by this many screen pixels (a hop). */
		public Fig hop(float px) {
			hop = px;
			return this;
		}

		/** A turn about itself (degrees, on top of facing). */
		public Fig spin(float degrees) {
			spin = degrees;
			return this;
		}

		/** The head turned to look (degrees, positive to the screen's right). */
		public Fig look(float degrees) {
			look = degrees;
			return this;
		}

		/** The head turned to look, across (positive to the screen's right) and down (positive down), in degrees. */
		public Fig look(float across, float down) {
			look = across;
			lookDown = down;
			return this;
		}

		/** A gesture: one of WAVE, CHEER, JUMP, POINT, SPIN, THINK, and how far into it (seconds). */
		public Fig emote(int kind, float secs) {
			emote = kind;
			emoteSecs = secs;
			return this;
		}

		/** A Pokémon's one-off animation (its cry, a flinch), secs in. */
		public Fig action(String name, float secs) {
			action = name;
			actionSecs = secs;
			return this;
		}

		/** Squashed (below 1) or stretched (above 1) up and down, for a landing or a spring. */
		public Fig squash(float k) {
			squash = k;
			return this;
		}

		/** The time (for idling and waving), in seconds. */
		public Fig at(float secs) {
			seconds = secs;
			return this;
		}
	}

	/** People's gestures. */
	public static final int WAVE = 0, CHEER = 1, JUMP = 2, POINT = 3, SPIN = 4, THINK = 5, CLAP = 6, TADA = 7, WAVE_BOTH = 8, EMOTES = 9;
	/** The cheerful ones (for someone you point at, or saying hello). */
	public static final int[] HAPPY = {WAVE, CHEER, JUMP, POINT, SPIN, CLAP, TADA, WAVE_BOTH};

	/** How high a gesture lifts them (screen pixels per block of height), for the ones with a jump. */
	public static float emoteHop(int kind, float secs) {
		return switch (kind) {
			case JUMP -> secs < 0.9F ? (float) Math.abs(Math.sin(secs / 0.45F * Math.PI)) * 0.22F : 0;
			case CHEER -> secs < 1.2F ? (float) Math.abs(Math.sin(secs / 0.3F * Math.PI)) * 0.06F : 0;
			case SPIN -> secs < 0.6F ? (float) Math.sin(secs / 0.6F * Math.PI) * 0.18F : 0;
			case TADA -> secs < 0.4F ? (float) Math.sin(secs / 0.4F * Math.PI) * 0.08F : 0;
			default -> 0;
		};
	}

	/** The figures for one frame over an area of the screen. */
	public record State(List<Fig> figs, int x0, int y0, int x1, int y1, float scale, ScreenRectangle scissorArea, ScreenRectangle bounds)
			implements PictureInPictureRenderState {}

	/** Figures over an area of the screen (usually the whole screen): add them, then draw. */
	public static final class Batch {
		private final List<Fig> figs = new ArrayList<>();
		private final int x0, y0, x1, y1;

		Batch(int x0, int y0, int x1, int y1) {
			this.x0 = x0;
			this.y0 = y0;
			this.x1 = x1;
			this.y1 = y1;
		}

		/** A person in a skin, standing with their feet at (cx, feet), tall pixels high. */
		public Fig person(Identifier skin, float cx, float feet, float tall) {
			Fig f = new Fig(skin, null, cx, feet, tall);
			figs.add(f);
			return f;
		}

		/** A Pokémon standing with its feet at (cx, feet), about tall pixels high. */
		public Fig mon(String species, float cx, float feet, float tall) {
			Fig f = new Fig(null, species, cx, feet, tall);
			figs.add(f);
			return f;
		}

		public boolean isEmpty() {
			return figs.isEmpty();
		}

		/** Puts the figures on the screen (in the drawing order of the screen: what's drawn after goes on top). */
		public void draw(GuiGraphics g) {
			if (figs.isEmpty() || x1 <= x0 || y1 <= y0) return;
			g.guiRenderState.submitPicturesInPictureState(new State(List.copyOf(figs), x0, y0, x1, y1, 1F, null,
					PictureInPictureRenderState.getBounds(x0, y0, x1, y1, null)));
		}
	}

	public static Batch batch(int x0, int y0, int x1, int y1) {
		return new Batch(x0, y0, x1, y1);
	}

	// ---------- drawing ----------

	/** Draws a frame's figures into its picture (registered with Fabric as a special GUI element). */
	public static final class Renderer extends PictureInPictureRenderer<State> {
		private PlayerModel person;
		private boolean nested;
		private final Map<String, BedrockModel> mons = new HashMap<>();

		public Renderer(MultiBufferSource.BufferSource buffers) {
			super(buffers);
		}

		@Override
		public Class<State> getRenderStateClass() {
			return State.class;
		}

		@Override
		protected String getTextureLabel() {
			return "retromon figures";
		}

		@Override
		protected float getTranslateY(int height, int guiScale) {
			return 0;
		}

		@Override
		protected void renderToTexture(State st, PoseStack pose) {
			Minecraft.getInstance().gameRenderer.getLighting().setupFor(Lighting.Entry.ENTITY_IN_UI);
			// Picture space: screen pixels from the area's top left, y down, z towards you.
			pose.translate(-(st.x1() - st.x0()) / 2F, 0, 0);
			for (Fig f : st.figs()) {
				pose.pushPose();
				pose.translate(f.cx - st.x0(), f.feet - st.y0() - f.hop, 0);
				if (f.squash != 1) pose.scale(1 / (float) Math.sqrt(f.squash), f.squash, 1 / (float) Math.sqrt(f.squash));
				if (f.skin != null) drawPerson(f, pose);
				else drawMon(f, pose);
				pose.popPose();
			}
		}

		private PlayerModel person() {
			if (person == null) {
				person = new PlayerModel(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
				try {
					nested = person.rightArm.getChild("right_sleeve") == person.rightSleeve;
				} catch (RuntimeException e) {
					nested = false;
				}
			}
			return person;
		}

		private static void copy(ModelPart from, ModelPart to) {
			to.x = from.x;
			to.y = from.y;
			to.z = from.z;
			to.xRot = from.xRot;
			to.yRot = from.yRot;
			to.zRot = from.zRot;
		}

		private void drawPerson(Fig f, PoseStack pose) {
			PlayerModel m = person();
			for (ModelPart p : new ModelPart[] {m.head, m.hat, m.body, m.jacket, m.rightArm, m.leftArm, m.rightSleeve, m.leftSleeve, m.rightLeg, m.leftLeg,
					m.rightPants, m.leftPants}) p.resetPose();
			float t = f.seconds;
			// Standing easy: arms a touch out and swaying, breathing.
			float sway = (float) Math.sin(t * 1.7) * 0.035F;
			m.rightArm.zRot = 0.06F + sway;
			m.leftArm.zRot = -0.06F - sway;
			m.head.xRot = (float) Math.sin(t * 1.1) * 0.03F;
			m.head.yRot = (float) Math.toRadians(-f.look);
			m.head.xRot += (float) Math.toRadians(f.lookDown);
			if (f.talk > 0) {
				// Talking with their hands, as people do: a hand out, both opened wide, the other hand, a hand to the
				// chin, easing from one to the next; and a nod as they go.
				float cyc = t / 2.6F, into = cyc - (float) Math.floor(cyc);
				int a = Math.floorMod((int) Math.floor(cyc), 4);
				float w = Math.min(1, into / 0.3F);
				w = w * w * (3 - 2 * w);
				float[] from = talkPose(Math.floorMod(a - 1, 4), t), to = talkPose(a, t);
				float[] p = new float[7];
				for (int i = 0; i < 7; i++) p[i] = f.talk * (from[i] + (to[i] - from[i]) * w);
				m.rightArm.xRot += p[0];
				m.rightArm.yRot += p[1];
				m.rightArm.zRot += p[2];
				m.leftArm.xRot += p[3];
				m.leftArm.yRot += p[4];
				m.leftArm.zRot += p[5];
				m.head.zRot += p[6];
				m.head.xRot += f.talk * (float) Math.sin(t * 5.2) * 0.07F;
			}
			if (f.wave > 0) {
				// The right arm up and waving side to side.
				float w = Math.min(1, f.wave);
				m.rightArm.zRot = (1 - w) * m.rightArm.zRot + w * (2.55F + (float) Math.sin(t * 11) * 0.38F);
				m.rightArm.xRot = (1 - w) * m.rightArm.xRot + w * -0.15F;
				m.head.zRot = w * (float) Math.sin(t * 5.5) * 0.06F;
			}
			if (f.emote >= 0) gesture(m, f.emote, f.emoteSecs);
			if (!nested) {
				copy(m.head, m.hat);
				copy(m.body, m.jacket);
				copy(m.rightArm, m.rightSleeve);
				copy(m.leftArm, m.leftSleeve);
				copy(m.rightLeg, m.rightPants);
				copy(m.leftLeg, m.leftPants);
			}
			// The model is two blocks from the top of its head to its feet.
			float k = f.tall / 2F;
			float turn = f.emote == SPIN && f.emoteSecs < 0.6F ? f.emoteSecs / 0.6F * 360F : 0;
			if (f.emote >= 0) pose.translate(0, -emoteHop(f.emote, f.emoteSecs) * f.tall, 0);
			if (turn != 0) pose.mulPose(Axis.YP.rotationDegrees(-turn));
			pose.translate(0, -f.tall / 2F, 0);
			pose.mulPose(Axis.XP.rotationDegrees(f.pitch));
			pose.translate(0, f.tall / 2F, 0);
			pose.mulPose(Axis.YP.rotationDegrees(-(f.facing + f.spin)));
			pose.scale(k, k, k);
			pose.translate(0F, -1.5F, 0F);
			m.renderToBuffer(pose, bufferSource.getBuffer(m.renderType(f.skin)), FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
			bufferSource.endBatch();
		}

		/** The arms (right x, y, z; left x, y, z) and head tilt of each way of talking with the hands. */
		private static float[] talkPose(int i, float t) {
			float s = (float) Math.sin(t * 4.5), c = (float) Math.sin(t * 4 + 1);
			return switch (i) {
				case 0 -> new float[] {-0.75F - s * 0.18F, 0, 0.12F, 0, 0, 0, 0};
				case 1 -> new float[] {-0.62F - s * 0.1F, 0.3F, 0.5F, -0.62F - c * 0.1F, -0.3F, -0.5F, 0};
				case 2 -> new float[] {0, 0, 0, -0.8F - c * 0.18F, 0, -0.12F, -0.05F};
				default -> new float[] {-1.85F, 0.55F, 0, -0.45F, -0.55F, 0, 0.12F};
			};
		}

		/** A gesture on the model: arms (and head) for each kind. */
		private static void gesture(PlayerModel m, int kind, float t) {
			float sw = (float) Math.sin(t * 11);
			switch (kind) {
				case WAVE -> {
					float w = Math.min(1, t / 0.2F);
					m.rightArm.zRot = (1 - w) * m.rightArm.zRot + w * (2.55F + sw * 0.38F);
					m.rightArm.xRot = w * -0.15F;
					m.head.zRot = w * (float) Math.sin(t * 5.5) * 0.06F;
				}
				case CHEER -> {
					// Both arms up, pumping.
					float pump = (float) Math.abs(Math.sin(t / 0.3F * Math.PI));
					m.rightArm.zRot = 2.6F + pump * 0.25F;
					m.leftArm.zRot = -2.6F - pump * 0.25F;
					m.rightArm.xRot = m.leftArm.xRot = -0.1F;
					m.head.xRot -= 0.25F;
				}
				case JUMP -> {
					// A jump for joy: arms flung out and up, legs tucking a little at the top.
					float up = (float) Math.abs(Math.sin(t / 0.45F * Math.PI));
					m.rightArm.zRot = 0.5F + up * 1.6F;
					m.leftArm.zRot = -0.5F - up * 1.6F;
					m.rightLeg.xRot = -up * 0.35F;
					m.leftLeg.xRot = up * 0.2F;
					m.head.xRot -= up * 0.2F;
				}
				case POINT -> {
					// Points out at you, then a little fist pump.
					float w = Math.min(1, t / 0.18F);
					m.rightArm.xRot = w * -1.45F;
					m.rightArm.yRot = w * 0.25F;
					m.leftArm.xRot = w * (-0.9F + (float) Math.abs(Math.sin(t * 7)) * -0.6F);
					m.leftArm.zRot = -0.2F;
				}
				case SPIN -> {
					m.rightArm.zRot = 1.3F;
					m.leftArm.zRot = -1.3F;
				}
				case CLAP -> {
					// Both hands out in front, clapping.
					float c = (float) Math.abs(Math.sin(t * 9));
					m.rightArm.xRot = m.leftArm.xRot = -1.15F;
					m.rightArm.yRot = -0.32F - c * 0.22F;
					m.leftArm.yRot = 0.32F + c * 0.22F;
					m.head.xRot -= 0.1F;
				}
				case TADA -> {
					// Arms flung out wide (ta-da!), with a bounce.
					float w = Math.min(1, t / 0.15F), b = (float) Math.sin(t * 6) * 0.08F;
					m.rightArm.zRot = w * (1.85F + b);
					m.leftArm.zRot = -w * (1.85F + b);
					m.rightArm.xRot = m.leftArm.xRot = -0.25F * w;
					m.head.xRot -= 0.15F * w;
				}
				case WAVE_BOTH -> {
					// Both arms up, waving side to side.
					m.rightArm.zRot = 2.45F + sw * 0.35F;
					m.leftArm.zRot = -2.45F + sw * 0.35F;
					m.rightArm.xRot = m.leftArm.xRot = -0.12F;
					m.head.zRot = (float) Math.sin(t * 5.5) * 0.07F;
				}
				case THINK -> {
					// A hand to the chin, head tipped.
					float w = Math.min(1, t / 0.25F);
					m.rightArm.xRot = w * -2.0F;
					m.rightArm.yRot = w * 0.55F;
					m.leftArm.xRot = w * -0.6F;
					m.leftArm.yRot = w * -0.7F;
					m.head.zRot = w * 0.18F;
					m.head.xRot += w * 0.12F;
				}
				default -> {
				}
			}
		}

		private void drawMon(Fig f, PoseStack pose) {
			BedrockModel m = mons.computeIfAbsent((Look.ours() ? "r:" : "c:") + f.species, k -> BedrockModel.create(f.species));
			if (m == null) return;
			float h = Math.max(0.3F, m.def.height);
			float k = f.tall / h;
			pose.translate(0, -f.tall / 2F, 0);
			pose.mulPose(Axis.XP.rotationDegrees(f.pitch));
			pose.translate(0, f.tall / 2F, 0);
			pose.mulPose(Axis.YP.rotationDegrees(-(f.facing + f.spin)));
			pose.scale(k * m.def.scale, k * m.def.scale, k * m.def.scale);
			pose.translate(0F, -1.501F, 0F);
			m.pose(f.seconds, f.action, f.actionSecs);
			m.root.render(pose, bufferSource.getBuffer(RenderTypes.entityCutoutNoCull(m.def.texture)), FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
			for (BedrockModel.Layer l : m.def.layers) {
				m.root.render(pose, bufferSource.getBuffer(RenderTypes.entityTranslucentEmissive(l.frame(f.seconds))), FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
			}
			bufferSource.endBatch();
		}
	}
}
