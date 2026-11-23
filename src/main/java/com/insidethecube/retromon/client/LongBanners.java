package com.insidethecube.retromon.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BannerRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.BannerPatterns;
import net.minecraft.world.level.block.state.BlockState;

/**
 * (0.57, 16:23: "it's like 2 banners, one on top of the other to make it longer ... update those and make them look a
 * little bit better") John's banners hung two to a pole (Ecruteak's posts, the NATIONAL PARK's): in a Retromon save,
 * two of the same banner one above the other are one long banner, the top one drawn half as long again and the one under
 * it not drawn, so there's one cloth from the bar down, not two bars and a doubled cloth. (16:35: "make each banner
 * sort of unique, and you can obviously add your own style to it") Each of John's plain banners has a design of its
 * own on it (a border, a band, a disc or a diamond, a fringe), in gold and white on its colour, picked by where it hangs.
 */
public class LongBanners extends BannerRenderer {
	public LongBanners(BlockEntityRendererProvider.Context ctx) {
		super(ctx);
	}

	/** The designs, each three layers: what's laid on, and whether in the first accent (true) or the second. */
	@SuppressWarnings("unchecked")
	private static final ResourceKey<BannerPattern>[][] DESIGNS = new ResourceKey[][] {
			{BannerPatterns.TRIANGLES_BOTTOM, BannerPatterns.CIRCLE_MIDDLE, BannerPatterns.BORDER},
			{BannerPatterns.STRIPE_TOP, BannerPatterns.RHOMBUS_MIDDLE, BannerPatterns.TRIANGLES_BOTTOM},
			{BannerPatterns.GRADIENT_UP, BannerPatterns.FLOWER, BannerPatterns.CURLY_BORDER},
			{BannerPatterns.STRIPE_CENTER, BannerPatterns.STRIPE_BOTTOM, BannerPatterns.BORDER},
			{BannerPatterns.HALF_HORIZONTAL_MIRROR, BannerPatterns.CIRCLE_MIDDLE, BannerPatterns.STRIPE_TOP},
			{BannerPatterns.TRIANGLE_BOTTOM, BannerPatterns.STRIPE_MIDDLE, BannerPatterns.CURLY_BORDER}};
	private static final java.util.Map<Long, BannerPatternLayers> MADE = new java.util.HashMap<>();

	private static BannerPatternLayers design(BlockPos at, DyeColor base) {
		return MADE.computeIfAbsent(at.asLong() * 31 + base.getId(), k -> {
			var level = Minecraft.getInstance().level;
			if (level == null) return BannerPatternLayers.EMPTY;
			var lookup = level.registryAccess().lookupOrThrow(Registries.BANNER_PATTERN);
			DyeColor a = base == DyeColor.YELLOW || base == DyeColor.ORANGE ? DyeColor.WHITE : DyeColor.YELLOW;
			DyeColor b = base == DyeColor.WHITE ? DyeColor.RED : DyeColor.WHITE;
			int pick = Math.floorMod((int) (at.asLong() ^ (at.asLong() >>> 21)) * 7 + at.getX() * 3 + at.getZ(), DESIGNS.length);
			BannerPatternLayers.Builder out = new BannerPatternLayers.Builder();
			ResourceKey<BannerPattern>[] d = DESIGNS[pick];
			for (int i = 0; i < d.length; i++) {
				var h = lookup.get(d[i]);
				if (h.isPresent()) out.add(h.get(), i == 1 ? b : a);
			}
			return out.build();
		});
	}

	private static boolean same(BlockState a, BlockState b) {
		return a.getBlock() == b.getBlock() && a.getValues().equals(b.getValues());
	}

	@Override
	public void submit(BannerRenderState s, PoseStack pose, SubmitNodeCollector out, CameraRenderState camera) {
		var level = Minecraft.getInstance().level;
		if (!Adventure.inSave || level == null || s.blockPos == null || s.blockState == null) {
			super.submit(s, pose, out, camera);
			return;
		}
		BlockPos at = s.blockPos;
		if (s.patterns == null || s.patterns.layers().isEmpty()) s.patterns = design(at, s.baseColor);
		// The lower of the two: its cloth is the top one's.
		if (same(level.getBlockState(at.above()), s.blockState)) return;
		if (!same(level.getBlockState(at.below()), s.blockState)) {
			super.submit(s, pose, out, camera);
			return;
		}
		// The top one: stretched down from its bar over the both of them.
		pose.pushPose();
		pose.translate(0, 1, 0);
		pose.scale(1, 1.55F, 1);
		pose.translate(0, -1, 0);
		super.submit(s, pose, out, camera);
		pose.popPose();
	}
}
