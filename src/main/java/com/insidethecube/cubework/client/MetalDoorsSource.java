package com.insidethecube.cubework.client;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import com.insidethecube.cubework.Cubework;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Makes the metal doors' textures while the game loads: the copper door in use, repainted with the colours
 * of each metal or gem block in use (a texture pack's gold block gives its gold door). Each door pixel takes
 * the block's colour that is as light, among the block's pixels, as the door pixel is among the door's.
 * Sprites are cubework:{prefix}{metal}_door..., for example cubework:block/gold_door_top.
 */
public record MetalDoorsSource(List<Identifier> textures, List<Identifier> metals, String prefix) implements SpriteSource {
	public static final Identifier ID = Identifier.fromNamespaceAndPath(Cubework.ID, "metal_doors");
	public static final MapCodec<MetalDoorsSource> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Identifier.CODEC.listOf().fieldOf("textures").forGetter(MetalDoorsSource::textures),
			Identifier.CODEC.listOf().fieldOf("metals").forGetter(MetalDoorsSource::metals),
			Codec.STRING.fieldOf("prefix").forGetter(MetalDoorsSource::prefix)).apply(i, MetalDoorsSource::new));

	@Override
	public void run(ResourceManager manager, SpriteSource.Output regions) {
		for (Identifier texture : textures) {
			Optional<Resource> door = manager.getResource(TEXTURE_ID_CONVERTER.idToFile(texture));
			if (door.isEmpty()) continue;
			String name = texture.getPath().substring(texture.getPath().lastIndexOf('/') + 1);
			for (Identifier metalTexture : metals) {
				Optional<Resource> metal = manager.getResource(TEXTURE_ID_CONVERTER.idToFile(metalTexture));
				if (metal.isEmpty()) continue;
				String block = metalTexture.getPath().substring(metalTexture.getPath().lastIndexOf('/') + 1);
				String metalName = block.endsWith("_block") ? block.substring(0, block.length() - 6) : block;
				Identifier out = Identifier.fromNamespaceAndPath(Cubework.ID, prefix + name.replaceFirst("^copper", metalName));
				regions.add(out, new Region(door.get(), metal.get(), out));
			}
		}
	}

	@Override
	public MapCodec<? extends SpriteSource> codec() {
		return CODEC;
	}

	private record Region(Resource door, Resource metal, Identifier id) implements SpriteSource.DiscardableLoader {
		@Override
		public SpriteContents get(SpriteResourceLoader opener) {
			try (InputStream din = door.open(); NativeImage source = NativeImage.read(din);
					InputStream min = metal.open(); NativeImage block = NativeImage.read(min)) {
				int w = source.getWidth(), h = Math.min(source.getHeight(), w);
				int[] px = new int[w * h];
				for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) px[y * w + x] = source.getPixel(x, y);
				int bw = block.getWidth(), bh = Math.min(block.getHeight(), bw);
				List<Integer> palette = new ArrayList<>();
				for (int y = 0; y < bh; y++) for (int x = 0; x < bw; x++) {
					int c = block.getPixel(x, y);
					if ((c >>> 24) != 0) palette.add(c & 0xFFFFFF);
				}
				if (palette.isEmpty()) return null;
				repaint(px, palette);
				NativeImage out = new NativeImage(w, h, false);
				for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) out.setPixel(x, y, px[y * w + x]);
				return new SpriteContents(id, new FrameSize(w, h), out);
			} catch (Exception e) {
				return null;
			}
		}
	}

	/** Each opaque pixel takes the palette colour at the same place in lightness order. */
	static void repaint(int[] argb, List<Integer> palette) {
		palette.sort((a, b) -> Double.compare(lum(a), lum(b)));
		double[] lums = Arrays.stream(argb).filter(c -> (c >>> 24) != 0).mapToDouble(c -> lum(c & 0xFFFFFF)).sorted().toArray();
		if (lums.length == 0) return;
		for (int i = 0; i < argb.length; i++) {
			if ((argb[i] >>> 24) == 0) continue;
			double l = lum(argb[i] & 0xFFFFFF);
			int below = lowerBound(lums, l), upTo = lowerBound(lums, l + 1e-9);
			double rank = lums.length == 1 ? 0.5 : (below + (upTo - below - 1) / 2.0) / (lums.length - 1);
			int pick = (int) Math.round(Math.max(0, Math.min(1, rank)) * (palette.size() - 1));
			argb[i] = (argb[i] & 0xFF000000) | palette.get(pick);
		}
	}

	private static int lowerBound(double[] sorted, double v) {
		int lo = 0, hi = sorted.length;
		while (lo < hi) {
			int mid = (lo + hi) >>> 1;
			if (sorted[mid] < v) lo = mid + 1;
			else hi = mid;
		}
		return lo;
	}

	private static double lum(int rgb) {
		return 0.299 * ((rgb >> 16) & 255) + 0.587 * ((rgb >> 8) & 255) + 0.114 * (rgb & 255);
	}
}
