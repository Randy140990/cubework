package com.insidethecube.cubework.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.renderer.texture.atlas.SpriteSources;
import net.minecraft.resources.Identifier;
//#if MC >= 1.21.5
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.util.ExtraCodecs;
//#else
import com.google.common.collect.BiMap;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceType;
//#endif

/** Lets Cubework add its own kind of atlas source (the painted doors' textures). */
@Mixin(SpriteSources.class)
public interface AtlasSourceManagerAccessor {
	//#if MC >= 1.21.5
	@Accessor("ID_MAPPER")
	static ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends SpriteSource>> cubework$idMapper() {
		throw new AssertionError();
	}
	//#else
	@Accessor("TYPES")
	static BiMap<Identifier, SpriteSourceType> cubework$types() {
		throw new AssertionError();
	}
	//#endif
}
