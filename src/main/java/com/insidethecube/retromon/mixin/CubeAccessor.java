package com.insidethecube.retromon.mixin;

import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** A box's faces, swapped for softened ones (and back). */
@Mixin(ModelPart.Cube.class)
public interface CubeAccessor {
	@Mutable
	@Accessor("polygons")
	void retromon$setPolygons(ModelPart.Polygon[] polygons);
}
