package com.insidethecube.retromon.client;

import java.util.HashMap;
import java.util.Map;

import com.insidethecube.retromon.Retromon;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * The hero's gear, as models of their own: the cap (forwards or backwards, in a Pokémon's pattern, some with
 * ears, a bulb or a flame), the bag (a backpack, or a satchel on a strap across you) in your colour, and the
 * Pokédex in your hands while it's out. On you in the world (and in the wardrobe's mirror), and on you on the
 * battle stage.
 */
public class GearLayer<S extends HumanoidRenderState, M extends HumanoidModel<S>> extends RenderLayer<S, M> {
	private static ModelPart cap, ears, spikes, bulb, flame, backpack, satchel, deviceHeld, deviceHand;
	private static final Identifier DEVICE = Retromon.id("textures/gear/device.png");
	private static final Map<Integer, Identifier> BAGS = new HashMap<>();

	public GearLayer(RenderLayerParent<S, M> parent) {
		super(parent);
		build();
	}

	private static ModelPart bake(MeshDefinition mesh, int w, int h) {
		return LayerDefinition.create(mesh, w, h).bakeRoot();
	}

	private static void build() {
		if (cap != null) return;
		// The cap, in the head's own space (the head runs from -8 to 0 up and down, -4 to 4 across; the face is towards -z).
		MeshDefinition m = new MeshDefinition();
		PartDefinition r = m.getRoot();
		r.addOrReplaceChild("crown", CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, -8.5F, -4.5F, 9, 4, 9, new CubeDeformation(0.12F)), PartPose.ZERO);
		r.addOrReplaceChild("dome", CubeListBuilder.create().texOffs(36, 0).addBox(-3.5F, -9.45F, -3.5F, 7, 1, 7), PartPose.ZERO);
		r.addOrReplaceChild("button", CubeListBuilder.create().texOffs(28, 14).addBox(-0.5F, -10.2F, -0.5F, 1, 1, 1, new CubeDeformation(-0.1F)), PartPose.ZERO);
		r.addOrReplaceChild("brim", CubeListBuilder.create().texOffs(0, 14).addBox(-4.5F, -0.5F, -5F, 9, 1, 5),
				PartPose.offsetAndRotation(0, -4.9F, -4.6F, 0.14F, 0, 0));
		cap = bake(m, 64, 32);
		// Ears (Pikachu, Eevee), leaning out.
		m = new MeshDefinition();
		r = m.getRoot();
		r.addOrReplaceChild("left", CubeListBuilder.create().texOffs(32, 16).addBox(-1, -4, -0.5F, 2, 4, 1), PartPose.offsetAndRotation(3, -8.7F, 0.5F, 0, 0, 0.32F));
		r.addOrReplaceChild("right", CubeListBuilder.create().texOffs(32, 16).mirror().addBox(-1, -4, -0.5F, 2, 4, 1),
				PartPose.offsetAndRotation(-3, -8.7F, 0.5F, 0, 0, -0.32F));
		ears = bake(m, 64, 32);
		// Spikes (Gengar): two out to the sides and one back.
		m = new MeshDefinition();
		r = m.getRoot();
		r.addOrReplaceChild("left", CubeListBuilder.create().texOffs(32, 16).addBox(-1, -4, -0.5F, 2, 4, 1), PartPose.offsetAndRotation(2.5F, -8.6F, 0.5F, 0, 0, 0.6F));
		r.addOrReplaceChild("right", CubeListBuilder.create().texOffs(32, 16).mirror().addBox(-1, -4, -0.5F, 2, 4, 1),
				PartPose.offsetAndRotation(-2.5F, -8.6F, 0.5F, 0, 0, -0.6F));
		r.addOrReplaceChild("back", CubeListBuilder.create().texOffs(32, 16).addBox(-1, -4, -0.5F, 2, 4, 1), PartPose.offsetAndRotation(0, -8.6F, 2.5F, -0.55F, 0, 0));
		spikes = bake(m, 64, 32);
		// Bulbasaur's bulb, on top towards the back.
		m = new MeshDefinition();
		m.getRoot().addOrReplaceChild("bulb", CubeListBuilder.create().texOffs(40, 16).addBox(-2, -3, -2, 4, 3, 4), PartPose.offset(0, -9.3F, 1.2F));
		bulb = bake(m, 64, 32);
		// Charmander's flame, up the back.
		m = new MeshDefinition();
		m.getRoot().addOrReplaceChild("flame", CubeListBuilder.create().texOffs(32, 24).addBox(-1.5F, -4, 0, 3, 4, 1),
				PartPose.offsetAndRotation(0, -5.6F, 4.5F, -0.35F, 0, 0));
		flame = bake(m, 64, 32);
		// The backpack, in the body's space (shoulders at 0, the waist at 12; the back towards +z): the pack, its
		// lid, a pocket each side and one on the back, straps over the shoulders and down the front.
		m = new MeshDefinition();
		r = m.getRoot();
		r.addOrReplaceChild("main", CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, 0.6F, 2.2F, 7, 9, 4), PartPose.ZERO);
		r.addOrReplaceChild("flap", CubeListBuilder.create().texOffs(0, 13).addBox(-3.5F, 0.3F, 6.0F, 7, 4, 1, new CubeDeformation(0.08F)), PartPose.ZERO);
		r.addOrReplaceChild("side_l", CubeListBuilder.create().texOffs(22, 0).addBox(3.5F, 5.0F, 2.7F, 1, 4, 3), PartPose.ZERO);
		r.addOrReplaceChild("side_r", CubeListBuilder.create().texOffs(22, 0).mirror().addBox(-4.5F, 5.0F, 2.7F, 1, 4, 3), PartPose.ZERO);
		r.addOrReplaceChild("pocket", CubeListBuilder.create().texOffs(16, 13).addBox(-2.5F, 5.6F, 6.05F, 5, 3, 1), PartPose.ZERO);
		r.addOrReplaceChild("top_l", CubeListBuilder.create().texOffs(36, 0).addBox(2.2F, -0.45F, -2.6F, 1, 1, 5), PartPose.ZERO);
		r.addOrReplaceChild("top_r", CubeListBuilder.create().texOffs(36, 0).addBox(-3.2F, -0.45F, -2.6F, 1, 1, 5), PartPose.ZERO);
		r.addOrReplaceChild("strap_l", CubeListBuilder.create().texOffs(32, 0).addBox(2.2F, 0, -2.6F, 1, 8, 1), PartPose.ZERO);
		r.addOrReplaceChild("strap_r", CubeListBuilder.create().texOffs(32, 0).addBox(-3.2F, 0, -2.6F, 1, 8, 1), PartPose.ZERO);
		backpack = bake(m, 64, 32);
		// The satchel: a bag at the back of the left hip, its strap from the right shoulder across the front and back.
		m = new MeshDefinition();
		r = m.getRoot();
		r.addOrReplaceChild("bag", CubeListBuilder.create().texOffs(0, 20).addBox(0.6F, 7.6F, 2.15F, 5, 5, 2), PartPose.ZERO);
		r.addOrReplaceChild("front", CubeListBuilder.create().texOffs(48, 0).addBox(-0.5F, 0, -0.5F, 1, 14, 1), PartPose.offsetAndRotation(-3.2F, -0.2F, -2.35F, 0, 0, -0.62F));
		r.addOrReplaceChild("back", CubeListBuilder.create().texOffs(48, 0).addBox(-0.5F, 0, -0.5F, 1, 12, 1), PartPose.offsetAndRotation(-3.2F, -0.2F, 2.35F, 0, 0, -0.5F));
		r.addOrReplaceChild("shoulder", CubeListBuilder.create().texOffs(36, 0).addBox(-3.7F, -0.45F, -2.6F, 1, 1, 5), PartPose.ZERO);
		satchel = bake(m, 64, 32);
		// The Pokédex: held in both hands in front of you, its face tipped up towards yours; or in the right hand.
		m = new MeshDefinition();
		m.getRoot().addOrReplaceChild("dex", CubeListBuilder.create().texOffs(0, 0).addBox(-3, -0.5F, -2, 6, 1, 4), PartPose.offsetAndRotation(0, 8.2F, -7.6F, -0.8F, 0, 0));
		deviceHeld = bake(m, 32, 16);
		m = new MeshDefinition();
		m.getRoot().addOrReplaceChild("dex", CubeListBuilder.create().texOffs(0, 0).addBox(-3, -0.5F, -2, 6, 1, 4),
				PartPose.offsetAndRotation(-1, 10.6F, -0.5F, 0, (float) (Math.PI / 2), (float) (Math.PI / 2)));
		deviceHand = bake(m, 32, 16);
	}

	/** Whose gear this is: you (in the world or the wardrobe's mirror), or you on the battle stage. Null for anyone else. */
	private static String outfitOf(HumanoidRenderState state) {
		Minecraft mc = Minecraft.getInstance();
		if (!Adventure.inSave || state.isInvisible) return null;
		boolean me = state instanceof AvatarRenderState a && mc.player != null && a.id == mc.player.getId();
		boolean stage = state instanceof NpcRenderer.State n && "hero".equals(n.sprite);
		if (!me && !stage) return null;
		String o = me && ClothesScreen.preview != null ? ClothesScreen.preview : ClientState.data.outfit;
		return o == null ? "" : o;
	}

	@Override
	public void submit(PoseStack pose, SubmitNodeCollector out, int light, S state, float yRot, float xRot) {
		String worn = outfitOf(state);
		if (worn == null) return;
		boolean girl = "girl".equals(ClientState.data.gender);
		Outfit o = worn.isEmpty() ? Outfit.defaults(girl) : Outfit.parse(worn, girl);
		M model = getParentModel();
		int overlay = OverlayTexture.NO_OVERLAY;
		// The cap.
		if (Outfit.HATS[Math.floorMod(o.get("hat"), Outfit.HATS.length)].equals("cap")) {
			String pattern = Outfit.CAPS[Math.floorMod(o.get("capp"), Outfit.CAPS.length)];
			var type = RenderTypes.entityCutoutNoCull(Retromon.id("textures/gear/cap_" + pattern + ".png"));
			int tint = pattern.equals("plain") ? 0xFF000000 | o.colour("hatc") : -1;
			pose.pushPose();
			model.root().translateAndRotate(pose);
			model.getHead().translateAndRotate(pose);
			if (o.get("capb") == 1) pose.mulPose(Axis.YP.rotation((float) Math.PI));
			out.submitModelPart(cap, pose, type, light, overlay, null, false, false, tint, null, 0);
			ModelPart extra = switch (pattern) {
				case "pikachu", "eevee" -> ears;
				case "gengar" -> spikes;
				case "bulbasaur" -> bulb;
				case "charmander" -> flame;
				default -> null;
			};
			if (extra != null) out.submitModelPart(extra, pose, type, light, overlay, null, false, false, -1, null, 0);
			pose.popPose();
		}
		// The bag.
		String bag = Outfit.BAGS[Math.floorMod(o.get("bag"), Outfit.BAGS.length)];
		if (!bag.equals("none")) {
			Identifier tex = bagTexture(o.colour("bagc"));
			if (tex != null) {
				pose.pushPose();
				model.root().translateAndRotate(pose);
				model.body.translateAndRotate(pose);
				out.submitModelPart(bag.equals("satchel") ? satchel : backpack, pose, RenderTypes.entityCutoutNoCull(tex), light, overlay, null, false, false, -1,
						null, 0);
				pose.popPose();
			}
		}
		// The Pokédex in your hands (you only, out in the world).
		if (state instanceof AvatarRenderState) {
			float[] p = Device.pose(Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true));
			if (p != null && p[4] > 0) {
				pose.pushPose();
				model.root().translateAndRotate(pose);
				boolean both = p[1] > 0.6F;
				(both ? model.body : model.rightArm).translateAndRotate(pose);
				out.submitModelPart(both ? deviceHeld : deviceHand, pose, RenderTypes.entityCutoutNoCull(DEVICE), light, overlay, null, false, false, -1, null, 0);
				pose.popPose();
			}
		}
	}

	/** The bag in a colour: its greys tinted, with the buckles, zips and badge over them. */
	private static Identifier bagTexture(int rgb) {
		if (BAGS.containsKey(rgb)) return BAGS.get(rgb);
		Identifier id = null;
		try {
			NativeImage img = new NativeImage(64, 32, true);
			Outfit.layer(img, Retromon.id("textures/gear/bag_base.png"), rgb);
			Outfit.layer(img, Retromon.id("textures/gear/bag_detail.png"), -1);
			id = Retromon.id("dynamic/bag_" + Integer.toHexString(rgb & 0xFFFFFF));
			Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(() -> "Retromon bag", img));
		} catch (Exception e) {
			Retromon.LOG.warn("Couldn't make the bag's texture", e);
		}
		BAGS.put(rgb, id);
		return id;
	}
}
