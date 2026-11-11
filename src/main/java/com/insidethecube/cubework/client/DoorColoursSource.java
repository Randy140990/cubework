package com.insidethecube.cubework.client;

import java.io.InputStream;
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
import com.insidethecube.cubework.Material;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Makes the coloured doors' textures while the game loads, from whatever door textures are in use (so a
 * texture pack's doors are coloured too). Metal doors (iron) are coloured all over; wooden ones keep their metal. Listed in the block and item atlases; each texture becomes 16
 * sprites, cubework:{prefix}{colour}_{name}, for example cubework:block/red_oak_door_top.
 */
public record DoorColoursSource(List<Identifier> textures, String prefix, boolean metal) implements SpriteSource {
	public static final Identifier ID = Identifier.fromNamespaceAndPath(Cubework.ID, "door_colours");
	public static final MapCodec<DoorColoursSource> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Identifier.CODEC.listOf().fieldOf("textures").forGetter(DoorColoursSource::textures),
			Codec.STRING.fieldOf("prefix").forGetter(DoorColoursSource::prefix),
			Codec.BOOL.optionalFieldOf("metal", false).forGetter(DoorColoursSource::metal)).apply(i, DoorColoursSource::new));

	@Override
	public void run(ResourceManager manager, SpriteSource.Output regions) {
		for (Identifier texture : textures) {
			Optional<Resource> resource = manager.getResource(TEXTURE_ID_CONVERTER.idToFile(texture));
			if (resource.isEmpty()) continue;
			String name = texture.getPath().substring(texture.getPath().lastIndexOf('/') + 1);
			for (String colour : Material.COLOURS) {
				Identifier out = Identifier.fromNamespaceAndPath(Cubework.ID, prefix + colour + "_" + name);
				regions.add(out, new Region(resource.get(), out, Recolour.index(colour), metal));
			}
		}
	}

	@Override
	public MapCodec<? extends SpriteSource> codec() {
		return CODEC;
	}

	/** One coloured sprite, made when the atlas is built. Animated textures use their first frame. */
	private record Region(Resource resource, Identifier id, int colour, boolean metal) implements SpriteSource.DiscardableLoader {
		@Override
		public SpriteContents get(SpriteResourceLoader opener) {
			try (InputStream in = resource.open(); NativeImage source = NativeImage.read(in)) {
				int w = source.getWidth(), h = Math.min(source.getHeight(), w);
				int[] px = new int[w * h];
				for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) px[y * w + x] = source.getPixel(x, y);
				Recolour.apply(px, colour, metal);
				NativeImage out = new NativeImage(w, h, false);
				for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) out.setPixel(x, y, px[y * w + x]);
				return new SpriteContents(id, new FrameSize(w, h), out);
			} catch (Exception e) {
				return null;
			}
		}
	}
}
