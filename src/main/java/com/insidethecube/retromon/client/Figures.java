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
		float facing, pitch, wave, talk, hop, spin, look, nod, seconds, squash = 1;

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
			if (f.talk > 0) {
				// A hand out as they speak, and a nod.
				m.rightArm.xRot = f.talk * (-0.75F - (float) Math.sin(t * 4.5) * 0.18F);
				m.rightArm.zRot += f.talk * 0.12F;
				m.head.xRot += f.talk * (float) Math.sin(t * 5.2) * 0.07F;
			}
			if (f.wave > 0) {
				// The right arm up and waving side to side.
				float w = Math.min(1, f.wave);
				m.rightArm.zRot = (1 - w) * m.rightArm.zRot + w * (2.55F + (float) Math.sin(t * 11) * 0.38F);
				m.rightArm.xRot = (1 - w) * m.rightArm.xRot + w * -0.15F;
				m.head.zRot = w * (float) Math.sin(t * 5.5) * 0.06F;
			}
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
			pose.translate(0, -f.tall / 2F, 0);
			pose.mulPose(Axis.XP.rotationDegrees(f.pitch));
			pose.translate(0, f.tall / 2F, 0);
			pose.mulPose(Axis.YP.rotationDegrees(-(f.facing + f.spin)));
			pose.scale(k, k, k);
			pose.translate(0F, -1.5F, 0F);
			m.renderToBuffer(pose, bufferSource.getBuffer(m.renderType(f.skin)), FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
			bufferSource.endBatch();
		}

		private void drawMon(Fig f, PoseStack pose) {
			BedrockModel m = mons.computeIfAbsent(f.species, BedrockModel::create);
			if (m == null) return;
			float h = Math.max(0.3F, m.def.height);
			float k = f.tall / h;
			pose.translate(0, -f.tall / 2F, 0);
			pose.mulPose(Axis.XP.rotationDegrees(f.pitch));
			pose.translate(0, f.tall / 2F, 0);
			pose.mulPose(Axis.YP.rotationDegrees(-(f.facing + f.spin)));
			pose.scale(k * m.def.scale, k * m.def.scale, k * m.def.scale);
			pose.translate(0F, -1.501F, 0F);
			m.pose(f.seconds);
			m.root.render(pose, bufferSource.getBuffer(RenderTypes.entityCutoutNoCull(m.def.texture)), FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
			for (BedrockModel.Layer l : m.def.layers) {
				m.root.render(pose, bufferSource.getBuffer(RenderTypes.entityTranslucentEmissive(l.frame(f.seconds))), FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
			}
			bufferSource.endBatch();
		}
	}
}
