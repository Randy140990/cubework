package com.insidethecube.orbitarena;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/** The crowd's sounds (made by tools/crowd.py): the murmur, cheers, the roar, the uproar, applause, ooh and aww. */
public final class ArenaSounds {
	private ArenaSounds() {}

	public static final SoundEvent MURMUR = register("crowd.murmur");
	public static final SoundEvent CHEER_SMALL = register("crowd.cheer_small");
	public static final SoundEvent CHEER = register("crowd.cheer");
	public static final SoundEvent ROAR = register("crowd.roar");
	public static final SoundEvent UPROAR = register("crowd.uproar");
	public static final SoundEvent APPLAUSE = register("crowd.applause");
	public static final SoundEvent APPLAUSE_LIGHT = register("crowd.applause_light");
	public static final SoundEvent OOH = register("crowd.ooh");
	public static final SoundEvent AWW = register("crowd.aww");
	public static final SoundEvent BOO = register("crowd.boo");

	private static SoundEvent register(String name) {
		Identifier id = OrbitArena.id(name);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}

	/** Loads the class, so the sounds are registered with everything else. */
	public static void register() {
	}
}
