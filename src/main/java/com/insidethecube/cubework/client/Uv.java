package com.insidethecube.cubework.client;

import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;

/** How the game stores a texture position in a quad, and where block textures live. */
final class Uv {
	private Uv() {}

	static final Identifier BLOCK_ATLAS = TextureAtlas.LOCATION_BLOCKS;

}
