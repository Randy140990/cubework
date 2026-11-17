package com.insidethecube.kantojohto;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;

/** Route One (working name): Kanto and Johto as a 2.5D adventure on Insid3 Th3 Cub3's map. */
public class RouteOne implements ModInitializer {
	public static final String ID = "kantojohto";

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(ID, path);
	}

	@Override
	public void onInitialize() {
	}
}
