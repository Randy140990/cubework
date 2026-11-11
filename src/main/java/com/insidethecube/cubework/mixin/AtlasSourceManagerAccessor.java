package com.insidethecube.cubework.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSources;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;

/** Lets Cubework add its own kind of atlas source (the coloured doors' textures). */
@Mixin(SpriteSources.class)
public interface AtlasSourceManagerAccessor {
	@Accessor("ID_MAPPER")
	static ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends SpriteSource>> cubework$idMapper() {
		throw new AssertionError();
	}
}
