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
		float facing, pitch, wave, talk, hop, spin, look, lookDown, nod, seconds, squash = 1, walk, floss, lookUp;
		/** Turned to white light (0 to 1), and grown or shrunk about its feet (evolving). */
		float white, grow = 1;
		/** A gesture (people): one of the EMOTES, and how far into it (seconds). */
		int emote = -1;
		float emoteSecs;
		/** A one-off animation (Pokémon): cry, recoil..., and how far into it (seconds). */
		String action;
		float actionSecs;
		/** A Pokémon's pose out in the world (walk, fly, hover), or null for its idle. */
		String poseName;
		/** Towards you (picture pixels): who's drawn in front where figures overlap. */
		float depth;
		/** Pixels to a block, if it's sized by the world (a scene seen through a camera), not by tall. */
		float perBlock;

		Fig(Identifier skin, String species, float cx, float feet, float tall) {
			this.skin = skin;
			this.species = species;
			this.cx = cx;
			this.feet = feet;
			this.tall = tall;
		}

		/** Casts its own shadow on the ground (drawn by Batch.drawShadows, before the figures). */
		boolean cast;

		public Fig cast(boolean on) {
			cast = on;
			return this;
		}

		/** A Pokémon walking, flying or hovering (its world pose), or null for standing. */
		public Fig pose(String name) {
			poseName = name;
			return this;
		}

		/** Nearer (positive) or further (negative) than the others, for overlaps. */
		public Fig depth(float z) {
			depth = z;
			return this;
		}

		/** Sized by the world: so many pixels to a block (its height is then the model's own). */
		public Fig perBlock(float px) {
			perBlock = px;
			return this;
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

		/** Walking (0 to 1): legs and arms swinging (people). */
		public Fig walk(float amount) {
			walk = amount;
			return this;
		}

		/** The floss (0 to 1), as the viewer's: both arms swinging to one side and then the other, one in front and one behind, the hips going the other way. */
		public Fig floss(float amount) {
			floss = amount;
			return this;
		}

		/** Looking up (radians), so a face shows under a cap. */
		public Fig lookUp(float amount) {
			lookUp = amount;
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
		public Fig white(float k) {
			white = Math.max(0, Math.min(1, k));
			return this;
		}

		public Fig grow(float k) {
			grow = k;
			return this;
		}

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

	/**
	 * The figures' shadows for one frame: their shapes flattened on the ground along the sun, solid in their own
	 * picture, then laid over the scene see-through (tint), so where two overlap it's no darker. (Their own kind of
	 * picture: Minecraft keeps one picture per kind a frame.)
	 */
	public record ShadowState(List<Fig> figs, int x0, int y0, int x1, int y1, float scale, ScreenRectangle scissorArea, ScreenRectangle bounds, int tint)
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

		/** Their shadows on the ground (the ones that cast them), alpha how dark (0 to 255). Draw before draw(). */
		public void drawShadows(GuiGraphics g, int alpha) {
			List<Fig> cast = new ArrayList<>();
			for (Fig f : figs) if (f.cast) cast.add(f);
			if (cast.isEmpty() || x1 <= x0 || y1 <= y0) return;
			int a = Math.max(0, Math.min(255, alpha));
			g.guiRenderState.submitPicturesInPictureState(new ShadowState(cast, x0, y0, x1, y1, 1F, null, PictureInPictureRenderState.getBounds(x0, y0, x1, y1, null),
					(a << 24) | (a << 16) | (a << 8) | a));
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

	/**
	 * (0.61, John's 18:04: the party's creatures "zoom in like incredibly big" when picked) How tall to draw a creature
	 * (a mon()'s tall) so all of it fits a box w by h, turned to facing: by its height and by how far it reaches across
	 * (a long one, AZURIBBON, by its length), whichever is the tighter.
	 */
	public static float fitTall(String species, float w, float h, float facing) {
		BedrockModel.Def d = BedrockModel.def(species);
		if (d == null) return h * 0.8F;
		float hb = Math.max(0.3F, d.height), u = d.scale / 16F;
		double a = Math.toRadians(facing);
		float across = (float) (Math.abs(Math.cos(a)) * d.spanX + Math.abs(Math.sin(a)) * d.spanZ) * u;
		float up = Math.max(hb, d.spanY * u);
		float k = Math.min(h / up, w / Math.max(0.3F, across));
		return k * hb;
	}

	// ---------- drawing ----------

	/** Draws a frame's figures into its picture (registered with Fabric as a special GUI element). */
	public static final class Renderer extends PictureInPictureRenderer<State> {
		private static final float Z_SQUEEZE = 0.35F;
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
			figures(st.figs(), st.x0(), st.y0(), st.x1(), pose, false);
		}

		/** Drawing shadows (each figure flattened on the ground along the sun, in black). */
		private boolean shadowMode;

		void figures(List<Fig> figs, int x0, int y0, int x1, PoseStack pose, boolean shadows) {
			shadowMode = shadows;
			Minecraft.getInstance().gameRenderer.getLighting().setupFor(Lighting.Entry.ENTITY_IN_UI);
			// Picture space: screen pixels from the area's top left, y down, z away from you (Minecraft turns z round).
			pose.translate(-(x1 - x0) / 2F, 0, 0);
			State st = new State(figs, x0, y0, x1, 0, 1F, null, null);
			for (Fig f : st.figs()) {
				pose.pushPose();
				// Depth squeezed a third (each figure's own depth with it, so what's in front stays in front), so a
				// whole scene fits the picture's depth even on a big screen.
				// (Minecraft's picture in picture turns z round: away from you is +z here, so nearer is -depth.)
				pose.translate(f.cx - st.x0(), f.feet - st.y0() - (shadowMode ? 0 : f.hop), -f.depth * Z_SQUEEZE);
				pose.scale(1, 1, Z_SQUEEZE);
				if (f.squash != 1) pose.scale(1 / (float) Math.sqrt(f.squash), f.squash, 1 / (float) Math.sqrt(f.squash));
				if (f.grow != 1) pose.scale(Math.max(1e-4F, f.grow), Math.max(1e-4F, f.grow), Math.max(1e-4F, f.grow));
				if (f.skin != null) drawPerson(f, pose);
				else drawMon(f, pose);
				pose.popPose();
			}
		}

		private PlayerModel person() {
			if (person == null) {
				person = new PlayerModel(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
				Soft.apply(person.root(), true, Soft.PEOPLE);
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
			if (f.walk > 0) {
				float ph = t * 8, sw = (float) Math.sin(ph) * f.walk;
				m.rightLeg.xRot = sw * 0.62F;
				m.leftLeg.xRot = -sw * 0.62F;
				m.rightArm.xRot += -sw * 0.5F;
				m.leftArm.xRot += sw * 0.5F;
			}
			if (f.floss > 0) {
				float k = Math.min(1, f.floss), w = t * 6.4F, pp = (float) Math.sin(w), hp = (float) Math.sin(w * 2);
				m.rightArm.zRot = 0.55F * pp * k;
				m.leftArm.zRot = 0.55F * pp * k;
				m.rightArm.xRot = -0.55F * pp * k;
				m.leftArm.xRot = 0.55F * pp * k;
				m.body.zRot = 0.08F * hp * k;
				m.rightLeg.x += -1F * hp * k;
				m.leftLeg.x += -1F * hp * k;
			}
			m.head.xRot -= f.lookUp;
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
			if (shadowMode) flatten(pose);
			pose.mulPose(Axis.YP.rotationDegrees(-(f.facing + f.spin)));
			pose.scale(k, k, k);
			pose.translate(0F, -1.5F, 0F);
			if (shadowMode) m.renderToBuffer(pose, bufferSource.getBuffer(m.renderType(f.skin)), FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0xFF000000);
			else m.renderToBuffer(pose, bufferSource.getBuffer(m.renderType(f.skin)), FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
			// You (in your clothes): your cap and bag too, as in the world.
			if (!shadowMode && Outfit.HERO.equals(f.skin)) GearLayer.drawOn(m, pose, bufferSource, Outfit.heroOutfit(), Outfit.heroGirl(), FULL_BRIGHT);
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
			float k = f.perBlock > 0 ? f.perBlock : f.tall / h;
			float tall = f.perBlock > 0 ? h * f.perBlock : f.tall;
			pose.translate(0, -tall / 2F, 0);
			pose.mulPose(Axis.XP.rotationDegrees(f.pitch));
			pose.translate(0, tall / 2F, 0);
			if (shadowMode) flatten(pose);
			pose.mulPose(Axis.YP.rotationDegrees(-(f.facing + f.spin)));
			pose.scale(k * m.def.scale, k * m.def.scale, k * m.def.scale);
			pose.translate(0F, -1.501F, 0F);
			m.pose(f.seconds, f.poseName, f.action, f.actionSecs);
			int overlay = f.white > 0 ? OverlayTexture.pack(OverlayTexture.u(f.white), OverlayTexture.v(false)) : OverlayTexture.NO_OVERLAY;
			if (shadowMode) {
				m.root.render(pose, bufferSource.getBuffer(RenderTypes.entityCutoutNoCull(m.def.texture)), FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0xFF000000);
				bufferSource.endBatch();
				return;
			}
			m.root.render(pose, bufferSource.getBuffer(RenderTypes.entityCutoutNoCull(m.def.texture)), FULL_BRIGHT, overlay);
			for (BedrockModel.Layer l : m.def.layers) {
				m.root.render(pose, bufferSource.getBuffer(RenderTypes.entityTranslucentEmissive(l.frame(f.seconds))), FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
			}
			bufferSource.endBatch();
		}
	}

	/**
	 * Each figure's shadow in its upright frame (feet at 0, y down): everything brought down to the ground, moved
	 * back and a little right the higher it was, as the viewer's sun (up and to the front left) throws it.
	 */
	static void flatten(PoseStack pose) {
		org.joml.Matrix4f m = new org.joml.Matrix4f();
		m.m10(-0.17F);
		m.m11(0F);
		m.m12(-0.46F);
		pose.last().pose().mul(m);
	}

	/** Draws figures' shadows (ShadowState) into their own picture, then lays it over the scene see-through. */
	public static final class ShadowRenderer extends PictureInPictureRenderer<ShadowState> {
		private final Renderer core;

		public ShadowRenderer(MultiBufferSource.BufferSource buffers) {
			super(buffers);
			core = new Renderer(buffers);
		}

		@Override
		public Class<ShadowState> getRenderStateClass() {
			return ShadowState.class;
		}

		@Override
		protected String getTextureLabel() {
			return "retromon shadows";
		}

		@Override
		protected float getTranslateY(int height, int guiScale) {
			return 0;
		}

		@Override
		protected void renderToTexture(ShadowState st, PoseStack pose) {
			core.figures(st.figs(), st.x0(), st.y0(), st.x1(), pose, true);
		}

		@Override
		protected void blitTexture(ShadowState st, net.minecraft.client.gui.render.state.GuiRenderState gs) {
			com.mojang.blaze3d.textures.GpuTextureView view = colourView();
			if (view == null) return;
			gs.submitBlitToCurrentLayer(new net.minecraft.client.gui.render.state.BlitRenderState(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA,
					net.minecraft.client.gui.render.TextureSetup.singleTexture(view,
							com.mojang.blaze3d.systems.RenderSystem.getSamplerCache().getRepeat(com.mojang.blaze3d.textures.FilterMode.NEAREST)),
					st.pose(), st.x0(), st.y0(), st.x1(), st.y1(), 0F, 1F, 1F, 0F, st.tint(), st.scissorArea(), null));
		}

		/** The picture's colour (its view isn't open to us: found among the fields by its kind). */
		private com.mojang.blaze3d.textures.GpuTextureView colourView() {
			try {
				for (java.lang.reflect.Field f : PictureInPictureRenderer.class.getDeclaredFields()) {
					if (f.getType() != com.mojang.blaze3d.textures.GpuTextureView.class) continue;
					f.setAccessible(true);
					com.mojang.blaze3d.textures.GpuTextureView v = (com.mojang.blaze3d.textures.GpuTextureView) f.get(this);
					if (v != null && v.texture().getFormat() == com.mojang.blaze3d.textures.TextureFormat.RGBA8) return v;
				}
			} catch (ReflectiveOperationException | RuntimeException e) {
				return null;
			}
			return null;
		}
	}
}
