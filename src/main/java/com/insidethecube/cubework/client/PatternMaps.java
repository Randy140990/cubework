package com.insidethecube.cubework.client;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import com.mojang.blaze3d.platform.NativeImage;

/** Herringbone's normal and specular maps: the block's own maps, cut and laid the same way as its texture. */
final class PatternMaps {
	private PatternMaps() {}

	/** The patterned map as PNG bytes, or null. */
	static byte[] make(InputStream in, String pattern, boolean normal) {
		try (in; NativeImage source = NativeImage.read(in)) {
			int w = Math.min(source.getWidth(), source.getHeight());
			int[] px = new int[w * w];
			for (int y = 0; y < w; y++) for (int x = 0; x < w; x++) px[y * w + x] = Atlas.argb(source, x, y);
			int[] made = Patterns.apply(px, w, pattern, normal ? Patterns.Kind.NORMAL : Patterns.Kind.OTHER);
			Path tmp = Files.createTempFile("cubework-pattern", ".png");
			try (NativeImage out = new NativeImage(w, w, false)) {
				for (int y = 0; y < w; y++) for (int x = 0; x < w; x++) Atlas.setArgb(out, x, y, made[y * w + x]);
				out.writeToFile(tmp);
				return Files.readAllBytes(tmp);
			} finally {
				Files.deleteIfExists(tmp);
			}
		} catch (Exception e) {
			return null;
		}
	}
}
