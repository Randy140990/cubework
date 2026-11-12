package com.insidethecube.cubework.client;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
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

	//#if MC >= 1.21.5
	@Override
	public MapCodec<? extends SpriteSource> codec() {
		return CODEC;
	}
	//#else
	static net.minecraft.client.renderer.texture.atlas.SpriteSourceType TYPE;

	@Override
	public net.minecraft.client.renderer.texture.atlas.SpriteSourceType type() {
		return TYPE;
	}
	//#endif

	/** One coloured sprite, made when the atlas is built. Animated textures use their first frame. */
	private record Region(Resource resource, Identifier id, int colour, boolean metal) implements Atlas.Painted {
		@Override
		public SpriteContents paint() {
			try (InputStream in = resource.open(); NativeImage source = NativeImage.read(in)) {
				int[] size = new int[2];
				int[] px = Atlas.pixels(source, source.getWidth(), Math.min(source.getHeight(), source.getWidth()), size);
				Recolour.apply(px, colour, metal);
				return Atlas.contents(id, Atlas.image(px, size[0], size[1]));
			} catch (Throwable e) {
				return null;
			}
		}
	}
}
