package com.insidethecube.orbitarena;

import com.insidethecube.orbitarena.entity.Fan;
import com.insidethecube.orbitarena.entity.Fighter;
import com.insidethecube.orbitarena.match.Loadout;
import com.insidethecube.orbitarena.match.Matches;
import com.insidethecube.orbitarena.net.Payloads;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;

/**
 * Orbit Arena: a space PvP training arena. Press PvP Arena on the pause menu, pick who you fight (fighters,
 * mob waves or a boss), a difficulty from 1 to 10, how many and your kit, and you're taken to an arena in
 * space with a packed crowd. Your own things are put away and given back after.
 */
public class OrbitArena implements ModInitializer {
	public static final String ID = "orbitarena";

	public static final ResourceKey<Level> ARENA = ResourceKey.create(Registries.DIMENSION, id("arena"));

	private static final ResourceKey<EntityType<?>> FIGHTER_KEY = ResourceKey.create(Registries.ENTITY_TYPE, id("fighter"));
	private static final ResourceKey<EntityType<?>> FAN_KEY = ResourceKey.create(Registries.ENTITY_TYPE, id("fan"));

	public static EntityType<Fighter> FIGHTER;
	public static EntityType<Fan> FAN;

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(ID, path);
	}

	@Override
	public void onInitialize() {
		FIGHTER = Registry.register(BuiltInRegistries.ENTITY_TYPE, FIGHTER_KEY,
				EntityType.Builder.<Fighter>of(Fighter::new, MobCategory.MONSTER).sized(0.6F, 1.95F).eyeHeight(1.74F).clientTrackingRange(10).noLootTable().build(FIGHTER_KEY));
		FAN = Registry.register(BuiltInRegistries.ENTITY_TYPE, FAN_KEY,
				EntityType.Builder.<Fan>of(Fan::new, MobCategory.MISC).sized(0.6F, 1.8F).eyeHeight(1.6F).clientTrackingRange(0).noLootTable().noSave().noSummon().build(FAN_KEY));
		FabricDefaultAttributeRegistry.register(FIGHTER, Fighter.createAttributes());
		FabricDefaultAttributeRegistry.register(FAN, Fan.createAttributes());
		// Load the saved-things attachment before any player does.
		Loadout.SAVED.identifier();
		Payloads.register();
		Matches.register();
	}
}
