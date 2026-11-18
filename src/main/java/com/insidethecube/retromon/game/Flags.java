package com.insidethecube.retromon.game;

/** Switches read from deep inside the game (block collisions): kept in a class of their own with nothing to load. */
public final class Flags {
	private Flags() {}

	/** Signs are solid (a Retromon save is open). */
	public static volatile boolean solidSigns;
}
