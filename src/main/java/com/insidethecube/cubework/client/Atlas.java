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

	/**
	 * The largest a painted sprite is made. A texture pack's very large textures (512 or 1024 across) are
	 * averaged down to this for the painted doors and patterns, so hundreds of painted sprites can't
	 * overfill the block atlas or run the game out of memory while it loads.
	 */
	static final int MAX = 256;

	/**
	 * The top w by h of an image as 0xAARRGGBB pixels, averaged down to at most MAX across (h keeps its
	 * ratio to w). size gets the width and height made.
	 */
	static int[] pixels(NativeImage image, int w, int h, int[] size) {
		return pixels(image, w, h, size, true);
	}

	/** As above; byAlpha false averages every channel on its own (for shader maps, whose alpha is data). */
	static int[] pixels(NativeImage image, int w, int h, int[] size, boolean byAlpha) {
		int tw = Math.min(w, MAX), th = Math.max(1, (int) ((long) h * tw / w));
		int[] out = new int[tw * th];
		size[0] = tw;
		size[1] = th;
		if (tw == w && th == h) {
			for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) out[y * w + x] = argb(image, x, y);
			return out;
		}
		for (int ty = 0; ty < th; ty++) {
			int y0 = (int) ((long) ty * h / th), y1 = Math.max(y0 + 1, (int) ((long) (ty + 1) * h / th));
			for (int tx = 0; tx < tw; tx++) {
				int x0 = (int) ((long) tx * w / tw), x1 = Math.max(x0 + 1, (int) ((long) (tx + 1) * w / tw));
				// Colours weighted by their alpha, so see-through pixels don't darken the edges.
				long a = 0, r = 0, gr = 0, b = 0, n = 0;
				for (int y = y0; y < y1; y++) for (int x = x0; x < x1; x++) {
					int c = argb(image, x, y), ca = byAlpha ? c >>> 24 : 1;
					a += c >>> 24;
					r += (long) ((c >> 16) & 0xFF) * ca;
					gr += (long) ((c >> 8) & 0xFF) * ca;
					b += (long) (c & 0xFF) * ca;
					n++;
				}
				int pa = (int) (a / n);
				long d = byAlpha ? a : n;
				out[ty * tw + tx] = d == 0 ? 0 : (pa << 24) | (int) (r / d) << 16 | (int) (gr / d) << 8 | (int) (b / d);
			}
		}
		return out;
	}

	/** An image from 0xAARRGGBB pixels. */
	static NativeImage image(int[] px, int w, int h) {
		NativeImage out = new NativeImage(w, h, false);
		for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) setArgb(out, x, y, px[y * w + x]);
		return out;
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
