package com.insidethecube.cubework.mixin;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
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

/**
 * Shader maps for Cubework's painted doors come from the door each was painted from, and patterned blocks'
 * from the block they're drawn on (see ShaderMaps).
 */
@Mixin(MultiPackResourceManager.class)
public abstract class ShaderMapsMixin {
	@Inject(method = "getResource", at = @At("HEAD"), cancellable = true)
	private void cubework$doorShaderMaps(Identifier id, CallbackInfoReturnable<Optional<Resource>> cir) {
		Identifier source = ShaderMaps.source(id);
		ResourceManager self = (ResourceManager) (Object) this;
		if (source != null) {
			cir.setReturnValue(self.getResource(source));
			return;
		}
		ShaderMaps.PatternMap pattern = ShaderMaps.pattern(id);
		if (pattern == null || ShaderMaps.painter == null) return;
		Optional<Resource> base = self.getResource(pattern.source());
		byte[] made = null;
		if (base.isPresent()) {
			try (InputStream in = base.get().open()) {
				made = ShaderMaps.painter.make(in, pattern.pattern(), pattern.normal());
			} catch (Exception e) {
				made = null;
			}
		}
		if (made == null) {
			cir.setReturnValue(Optional.empty());
			return;
		}
		byte[] bytes = made;
		cir.setReturnValue(Optional.of(new Resource(base.get().source(), () -> new ByteArrayInputStream(bytes))));
	}
}
