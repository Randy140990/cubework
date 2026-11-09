package com.insidethecube.cubework.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.mojang.serialization.MapCodec;

import net.minecraft.client.texture.atlas.AtlasSource;
import net.minecraft.client.texture.atlas.AtlasSourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.Codecs;

/** Lets Cubework add its own kind of atlas source (the coloured doors' textures). */
@Mixin(AtlasSourceManager.class)
public interface AtlasSourceManagerAccessor {
	@Accessor("ID_MAPPER")
	static Codecs.IdMapper<Identifier, MapCodec<? extends AtlasSource>> cubework$idMapper() {
		throw new AssertionError();
	}
}
