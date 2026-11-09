package com.insidethecube.cubework.client;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import com.insidethecube.cubework.Cubework;
import com.insidethecube.cubework.Material;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.SpriteContents;
import net.minecraft.client.texture.SpriteDimensions;
import net.minecraft.client.texture.SpriteOpener;
import net.minecraft.client.texture.atlas.AtlasSource;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

/**
 * Makes the coloured doors' textures while the game loads, from whatever door textures are in use (so a
 * texture pack's doors are coloured too). Listed in the block and item atlases; each texture becomes 16
 * sprites, cubework:{prefix}{colour}_{name}, for example cubework:block/red_oak_door_top.
 */
public record DoorColoursSource(List<Identifier> textures, String prefix) implements AtlasSource {
	public static final Identifier ID = Identifier.of(Cubework.ID, "door_colours");
	public static final MapCodec<DoorColoursSource> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Identifier.CODEC.listOf().fieldOf("textures").forGetter(DoorColoursSource::textures),
			Codec.STRING.fieldOf("prefix").forGetter(DoorColoursSource::prefix)).apply(i, DoorColoursSource::new));

	@Override
	public void load(ResourceManager manager, AtlasSource.SpriteRegions regions) {
		for (Identifier texture : textures) {
			Optional<Resource> resource = manager.getResource(RESOURCE_FINDER.toResourcePath(texture));
			if (resource.isEmpty()) continue;
			String name = texture.getPath().substring(texture.getPath().lastIndexOf('/') + 1);
			for (String colour : Material.COLOURS) {
				Identifier out = Identifier.of(Cubework.ID, prefix + colour + "_" + name);
				regions.add(out, new Region(resource.get(), out, Recolour.index(colour)));
			}
		}
	}

	@Override
	public MapCodec<? extends AtlasSource> getCodec() {
		return CODEC;
	}

	/** One coloured sprite, made when the atlas is built. Animated textures use their first frame. */
	private record Region(Resource resource, Identifier id, int colour) implements AtlasSource.SpriteRegion {
		@Override
		public SpriteContents load(SpriteOpener opener) {
			try (InputStream in = resource.getInputStream(); NativeImage source = NativeImage.read(in)) {
				int w = source.getWidth(), h = Math.min(source.getHeight(), w);
				int[] px = new int[w * h];
				for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) px[y * w + x] = source.getColorArgb(x, y);
				Recolour.apply(px, colour);
				NativeImage out = new NativeImage(w, h, false);
				for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) out.setColorArgb(x, y, px[y * w + x]);
				return new SpriteContents(id, new SpriteDimensions(w, h), out);
			} catch (Exception e) {
				return null;
			}
		}
	}
}
