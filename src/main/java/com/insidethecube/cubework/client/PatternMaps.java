package com.insidethecube.cubework.client;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import com.mojang.blaze3d.platform.NativeImage;

/**
 * Patterned blocks' normal and specular maps, made from the block's own maps: herringbone's cut and laid the
 * same way as its texture; the others as they are. Always one frame and at most Atlas.MAX across, the same
 * as the patterned texture, however a pack draws its maps.
 */
final class PatternMaps {
	private PatternMaps() {}

	/** The patterned map as PNG bytes, or null. */
	static byte[] make(InputStream in, String pattern, boolean normal) {
		try (in; NativeImage source = NativeImage.read(in)) {
			int side = Math.min(source.getWidth(), source.getHeight());
			int[] size = new int[2];
			int[] px = Atlas.pixels(source, side, side, size, false);
			int w = size[0];
			int[] made = Patterns.apply(px, w, pattern, normal ? Patterns.Kind.NORMAL : Patterns.Kind.OTHER);
			Path tmp = Files.createTempFile("cubework-pattern", ".png");
			try (NativeImage out = new NativeImage(w, w, false)) {
				for (int y = 0; y < w; y++) for (int x = 0; x < w; x++) Atlas.setArgb(out, x, y, made[y * w + x]);
				out.writeToFile(tmp);
				return Files.readAllBytes(tmp);
			} finally {
				Files.deleteIfExists(tmp);
			}
		} catch (Throwable e) {
			return null;
		}
	}
}
