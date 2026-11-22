package com.insidethecube.retromon.mixin;

import java.util.function.LongSupplier;

import com.insidethecube.retromon.client.Adventure;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * (0.50, 20:20) Lavender Town's night: the sky, the light and the stars (Minecraft's timelines, 1.21.11) see the
 * middle of the night as you come to the town, without the clock itself changing (the HUD still says 10:30). Only
 * your screen's world; the server's time is its own.
 */
@Mixin(EnvironmentAttributeSystem.class)
public abstract class EnvironmentTimeMixin {
	@ModifyVariable(method = "addDefaultLayers", at = @At("STORE"), ordinal = 0, require = 0)
	private static LongSupplier retromon$lavender(LongSupplier day, EnvironmentAttributeSystem.Builder builder, Level level) {
		if (level == null || !level.isClientSide()) return day;
		return () -> Adventure.shownDay(day.getAsLong());
	}
}
