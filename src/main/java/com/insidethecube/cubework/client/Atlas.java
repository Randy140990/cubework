package com.insidethecube.cubework.client;

import com.insidethecube.cubework.mixin.AtlasSourceManagerAccessor;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.Identifier;
//#if MC < 1.21.10
import net.minecraft.server.packs.resources.ResourceMetadata;
//#endif
//#if MC < 1.21.5
import net.minecraft.client.renderer.texture.atlas.SpriteSourceType;
//#endif

/** The parts of making atlas sprites in code that changed between versions (the painted doors use them). */
final class Atlas {
	private Atlas() {}

	/** A sprite painted when its atlas is built. */
	interface Painted extends
			//#if MC >= 1.21.11
			SpriteSource.DiscardableLoader
			//#else
			SpriteSource.SpriteSupplier
			//#endif
	{
		/** The painted sprite, or null when its textures couldn't be read. */
		SpriteContents paint();

		//#if MC >= 1.21.11
		@Override
		default SpriteContents get(SpriteResourceLoader opener) {
			return paint();
		}
		//#else
		@Override
		default SpriteContents apply(SpriteResourceLoader opener) {
			return paint();
		}
		//#endif
	}

	static SpriteContents contents(Identifier id, NativeImage image) {
		FrameSize size = new FrameSize(image.getWidth(), image.getHeight());
		//#if MC >= 1.21.10
		return new SpriteContents(id, size, image);
		//#else
		return new SpriteContents(id, size, image, ResourceMetadata.EMPTY);
		//#endif
	}

	/** A pixel as 0xAARRGGBB. */
	static int argb(NativeImage image, int x, int y) {
		//#if MC >= 1.21.2
		return image.getPixel(x, y);
		//#else
		int abgr = image.getPixelRGBA(x, y);
		return (abgr & 0xFF00FF00) | ((abgr & 0xFF) << 16) | ((abgr >> 16) & 0xFF);
		//#endif
	}

	static void setArgb(NativeImage image, int x, int y, int argb) {
		//#if MC >= 1.21.2
		image.setPixel(x, y, argb);
		//#else
		image.setPixelRGBA(x, y, (argb & 0xFF00FF00) | ((argb & 0xFF) << 16) | ((argb >> 16) & 0xFF));
		//#endif
	}

	//#if MC >= 1.21.5
	/** Adds a kind of atlas source, named in atlas files by id. */
	static void register(Identifier id, MapCodec<? extends SpriteSource> codec) {
		AtlasSourceManagerAccessor.cubework$idMapper().put(id, codec);
	}
	//#else
	static SpriteSourceType register(Identifier id, MapCodec<? extends SpriteSource> codec) {
		SpriteSourceType type = new SpriteSourceType(codec);
		AtlasSourceManagerAccessor.cubework$types().put(id, type);
		return type;
	}
	//#endif
}
