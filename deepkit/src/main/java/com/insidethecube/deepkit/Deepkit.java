package com.insidethecube.deepkit;

import com.insidethecube.deepkit.flare.FlareColour;
import com.insidethecube.deepkit.lamp.Lamps;
import com.insidethecube.deepkit.net.Payloads;
import com.insidethecube.deepkit.paint.PaintColour;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Deepkit by Inside the Cube: lamps, flares, glow paint, rope and a grappling hook for exploring the dark. */
public final class Deepkit implements ModInitializer {
	public static final String ID = "deepkit";

	@Override
	public void onInitialize() {
		DK.register();
		Payloads.register();
		ServerTickEvents.END_SERVER_TICK.register(Lamps::tick);
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, DK.id("deepkit"), FabricItemGroup.builder()
				.title(Component.translatable("itemGroup.deepkit"))
				.icon(() -> new ItemStack(DK.HEADLAMP))
				.displayItems((context, out) -> {
					out.accept(DK.HAND_TORCH);
					out.accept(DK.HEADLAMP);
					out.accept(DK.CHEST_LAMP);
					for (FlareColour c : FlareColour.values()) out.accept(DK.FLARES[c.ordinal()]);
					for (PaintColour c : PaintColour.values()) out.accept(DK.PAINTS[c.ordinal()]);
					out.accept(DK.ROPE_COIL);
					out.accept(DK.GRAPPLING_HOOK);
				})
				.build());
	}
}
