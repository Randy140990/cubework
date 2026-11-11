package com.insidethecube.cubework.client;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
//#if MC >= 1.21.11
import net.minecraft.client.model.geom.builders.UVPair;
import org.joml.Vector3f;
//#endif

/** Makes the game's quads (how they're stored changed in 1.21.4 and again in 1.21.11). */
final class Quads {
	private Quads() {}

	/**
	 * A quad with corners p (block coordinates, 0 to 1) and texture positions uv (0 to 1 across the
	 * sprite), in the order they're given; tint -1 for none.
	 */
	static BakedQuad of(TextureAtlasSprite sprite, int tint, Direction face, double[][] p, float[][] uv) {
		//#if MC >= 1.21.11
		Vector3f[] pos = new Vector3f[4];
		long[] packed = new long[4];
		for (int i = 0; i < 4; i++) {
			pos[i] = new Vector3f((float) p[i][0], (float) p[i][1], (float) p[i][2]);
			packed[i] = UVPair.pack(sprite.getU(uv[i][0]), sprite.getV(uv[i][1]));
		}
		return new BakedQuad(pos[0], pos[1], pos[2], pos[3], packed[0], packed[1], packed[2], packed[3], tint, face, sprite, true, 0);
		//#else
		// Eight ints a corner: x, y, z, colour, u, v, light, normal (the last two are filled in when drawn).
		int[] data = new int[32];
		for (int i = 0; i < 4; i++) {
			data[i * 8] = Float.floatToRawIntBits((float) p[i][0]);
			data[i * 8 + 1] = Float.floatToRawIntBits((float) p[i][1]);
			data[i * 8 + 2] = Float.floatToRawIntBits((float) p[i][2]);
			data[i * 8 + 3] = -1;
			data[i * 8 + 4] = Float.floatToRawIntBits(sprite.getU(uv[i][0]));
			data[i * 8 + 5] = Float.floatToRawIntBits(sprite.getV(uv[i][1]));
		}
		//#if MC >= 1.21.2
		return new BakedQuad(data, tint, face, sprite, true, 0);
		//#else
		return new BakedQuad(data, tint, face, sprite, true);
		//#endif
		//#endif
	}
}
