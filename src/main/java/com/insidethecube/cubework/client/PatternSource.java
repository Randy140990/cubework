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
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Makes the patterned textures while the game loads, from the block textures in use (so a texture pack's
 * blocks get patterned too). Listed in the block atlas; each texture becomes one sprite a pattern,
 * cubework:block/pattern/{pattern}/{name}, for example cubework:block/pattern/tiled/stone_bricks.
 */
public record PatternSource(List<Identifier> textures, List<String> patterns) implements SpriteSource {
	public static final Identifier ID = Identifier.fromNamespaceAndPath(Cubework.ID, "patterns");
	public static final MapCodec<PatternSource> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Identifier.CODEC.listOf().fieldOf("textures").forGetter(PatternSource::textures),
			Codec.STRING.listOf().fieldOf("patterns").forGetter(PatternSource::patterns)).apply(i, PatternSource::new));

	@Override
	public void run(ResourceManager manager, SpriteSource.Output regions) {
		for (Identifier texture : textures) {
			Optional<Resource> resource = manager.getResource(TEXTURE_ID_CONVERTER.idToFile(texture));
			if (resource.isEmpty()) continue;
			String name = texture.getPath().substring(texture.getPath().lastIndexOf('/') + 1);
			for (String pattern : patterns) {
				Identifier out = Identifier.fromNamespaceAndPath(Cubework.ID, "block/pattern/" + pattern + "/" + name);
				regions.add(out, new Region(resource.get(), out, pattern));
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

	/** One patterned sprite, made when the atlas is built. Animated textures use their first frame. */
	private record Region(Resource resource, Identifier id, String pattern) implements Atlas.Painted {
		@Override
		public SpriteContents paint() {
			try (InputStream in = resource.open(); NativeImage source = NativeImage.read(in)) {
				int w = Math.min(source.getWidth(), source.getHeight());
				int[] px = new int[w * w];
				for (int y = 0; y < w; y++) for (int x = 0; x < w; x++) px[y * w + x] = Atlas.argb(source, x, y);
				int[] made = Patterns.apply(px, w, pattern, Patterns.Kind.COLOUR);
				NativeImage out = new NativeImage(w, w, false);
				for (int y = 0; y < w; y++) for (int x = 0; x < w; x++) Atlas.setArgb(out, x, y, made[y * w + x]);
				return Atlas.contents(id, out);
			} catch (Exception e) {
				return null;
			}
		}
	}
}
