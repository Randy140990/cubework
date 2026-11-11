package com.insidethecube.cubework.mixin;

import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.insidethecube.cubework.ShaderMaps;

/** Shader maps for Cubework's painted doors come from the door each was painted from (see ShaderMaps). */
@Mixin(MultiPackResourceManager.class)
public abstract class ShaderMapsMixin {
	@Inject(method = "getResource", at = @At("HEAD"), cancellable = true)
	private void cubework$doorShaderMaps(Identifier id, CallbackInfoReturnable<Optional<Resource>> cir) {
		Identifier source = ShaderMaps.source(id);
		if (source != null) cir.setReturnValue(((ResourceManager) (Object) this).getResource(source));
	}
}
