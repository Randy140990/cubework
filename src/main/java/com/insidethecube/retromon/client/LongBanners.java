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

	/**
	 * (0.59.2, 19:17: "make sure that the flags are unique in the different areas ... different areas might have
	 * different meanings for having festivals and flags") Each place's banners its own: John's cloths in the place's
	 * colours (his colour by colour: the first of his three, the second, the third) and the place's own designs.
	 */
	private record Theme(String name, int x0, int z0, int x1, int z1, DyeColor[] palette, DyeColor a, DyeColor b, ResourceKey<BannerPattern>[][] designs) {
		boolean has(BlockPos p) {
			return p.getX() >= x0 && p.getX() <= x1 && p.getZ() >= z0 && p.getZ() <= z1;
		}
	}

	@SuppressWarnings("unchecked")
	private static final Theme[] THEMES = {
			// Ecruteak: the towers' festival, the rainbow bird's flame: deep red and orange cloths, gold and white.
			new Theme("ecruteak", -2095, 330, -1960, 440, new DyeColor[] {DyeColor.RED, DyeColor.WHITE, DyeColor.ORANGE}, DyeColor.YELLOW, DyeColor.WHITE,
					new ResourceKey[][] {{BannerPatterns.GRADIENT_UP, BannerPatterns.FLOWER, BannerPatterns.CURLY_BORDER},
							{BannerPatterns.STRIPE_TOP, BannerPatterns.CIRCLE_MIDDLE, BannerPatterns.TRIANGLES_BOTTOM}}),
			// The NATIONAL PARK: the Bug-Catching Contest's pennants, leaf green and lime with a gold sun.
			new Theme("park", -2160, 440, -2080, 560, new DyeColor[] {DyeColor.GREEN, DyeColor.WHITE, DyeColor.LIME}, DyeColor.YELLOW, DyeColor.WHITE,
					new ResourceKey[][] {{BannerPatterns.TRIANGLES_BOTTOM, BannerPatterns.CIRCLE_MIDDLE, BannerPatterns.BORDER},
							{BannerPatterns.STRIPE_CENTER, BannerPatterns.RHOMBUS_MIDDLE, BannerPatterns.CURLY_BORDER}}),
			// Olivine: the harbour's signal flags, navy and sky blue, white waves, the lighthouse's gold light.
			new Theme("olivine", -2345, 495, -2225, 640, new DyeColor[] {DyeColor.BLUE, DyeColor.WHITE, DyeColor.LIGHT_BLUE}, DyeColor.WHITE, DyeColor.YELLOW,
					new ResourceKey[][] {{BannerPatterns.STRIPE_TOP, BannerPatterns.CIRCLE_MIDDLE, BannerPatterns.TRIANGLES_BOTTOM},
							{BannerPatterns.CROSS, BannerPatterns.BORDER, BannerPatterns.BORDER},
							{BannerPatterns.STRAIGHT_CROSS, BannerPatterns.STRIPE_BOTTOM, BannerPatterns.BORDER}}),
			// The S.S. AQUA: dressed overall, signal flags in navy, white and red.
			new Theme("aqua", -2306, 662, -2262, 856, new DyeColor[] {DyeColor.RED, DyeColor.WHITE, DyeColor.BLUE}, DyeColor.WHITE, DyeColor.RED,
					new ResourceKey[][] {{BannerPatterns.STRIPE_CENTER, BannerPatterns.STRIPE_MIDDLE, BannerPatterns.BORDER},
							{BannerPatterns.HALF_HORIZONTAL, BannerPatterns.CIRCLE_MIDDLE, BannerPatterns.BORDER},
							{BannerPatterns.CROSS, BannerPatterns.RHOMBUS_MIDDLE, BannerPatterns.BORDER}}),
			// Cianwood: the beach's, sunset orange and sea cyan, the sun, the surf.
			new Theme("cianwood", -2600, 690, -2480, 820, new DyeColor[] {DyeColor.ORANGE, DyeColor.WHITE, DyeColor.CYAN}, DyeColor.YELLOW, DyeColor.WHITE,
					new ResourceKey[][] {{BannerPatterns.GRADIENT_UP, BannerPatterns.CIRCLE_MIDDLE, BannerPatterns.TRIANGLES_BOTTOM},
							{BannerPatterns.HALF_HORIZONTAL, BannerPatterns.STRIPE_BOTTOM, BannerPatterns.CURLY_BORDER}})};

	/** John's banner colours, in the order he uses them: which of a place's colours each becomes. */
	private static int slot(DyeColor c) {
		return switch (c) {
			case ORANGE, RED -> 0;
			case WHITE -> 1;
			case CYAN, GREEN -> 2;
			default -> 1;
		};
	}

	private static Theme themeAt(BlockPos at) {
		for (Theme t : THEMES) if (t.has(at)) return t;
		return null;
	}

	private static BannerPatternLayers themed(Theme t, BlockPos at, DyeColor base) {
		return MADE.computeIfAbsent(at.asLong() * 31 + base.getId() + 7_000_000_000L * (1 + java.util.Arrays.asList(THEMES).indexOf(t)), k -> {
			var level = Minecraft.getInstance().level;
			if (level == null) return BannerPatternLayers.EMPTY;
			var lookup = level.registryAccess().lookupOrThrow(Registries.BANNER_PATTERN);
			DyeColor a = t.a() == base ? DyeColor.WHITE : t.a(), b = t.b() == base ? t.a() : t.b();
			// (A pole's pair of cloths, and the cloths along a cord, each their own of the place's designs.)
			int pick = Math.floorMod(at.getX() * 5 + at.getZ() * 3 + at.getY(), t.designs().length);
			BannerPatternLayers.Builder out = new BannerPatternLayers.Builder();
			ResourceKey<BannerPattern>[] d = t.designs()[pick];
			for (int i = 0; i < d.length; i++) {
				var h = lookup.get(d[i]);
				if (h.isPresent()) out.add(h.get(), i == 1 ? b : a);
			}
			return out.build();
		});
	}

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
		Theme t = themeAt(at);
		if (t != null) {
			DyeColor base = t.palette()[slot(s.baseColor) % t.palette().length];
			s.patterns = themed(t, at, base);
			s.baseColor = base;
		} else if (s.patterns == null || s.patterns.layers().isEmpty()) s.patterns = design(at, s.baseColor);
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
