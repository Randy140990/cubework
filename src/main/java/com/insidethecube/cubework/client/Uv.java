package com.insidethecube.cubework.client;

import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.util.Identifier;

/** How the game stores a texture position in a quad, and where block textures live. */
final class Uv {
	private Uv() {}

	static final Identifier BLOCK_ATLAS = SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;

	static long pack(float u, float v) {
		return ((long) Float.floatToRawIntBits(u) << 32) | (Float.floatToRawIntBits(v) & 0xFFFFFFFFL);
	}
}
