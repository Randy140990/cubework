package com.insidethecube.retromon.client;

import java.util.HashMap;
import java.util.Map;

import com.insidethecube.retromon.Retromon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/**
 * The game's music (Yellow's soundtrack, rendered from the game's own code): each place's tune, the battle
 * themes and victory fanfares, and the short jingles (healing, a catch, a level up), which pause the music
 * and then let it carry on. A song with an intro plays the intro once and then loops the rest. It plays on
 * Minecraft's music volume, and Minecraft's own music keeps quiet meanwhile.
 */
public final class Music {
	private Music() {}

	private static String current, place;
	private static SoundInstance playing;
	private static boolean inIntro, looping;
	private static String jingle;
	private static SoundInstance jinglePlaying;
	private static final Map<String, Boolean> HAS_INTRO = new HashMap<>();

	/** Whether Retromon's music is playing (Minecraft's own music stays quiet then). */
	public static boolean active() {
		return current != null || jingle != null;
	}

	private static boolean exists(String id) {
		return HAS_INTRO.computeIfAbsent(id, k -> Minecraft.getInstance().getResourceManager()
				.getResource(Identifier.fromNamespaceAndPath(Retromon.ID, "sounds/music/" + k + ".ogg")).isPresent());
	}

	private static SoundInstance sound(String id, boolean loop) {
		return new SimpleSoundInstance(Retromon.id("music." + id), SoundSource.MUSIC, 1.0F, 1.0F, RandomSource.create(), loop, 0,
				SoundInstance.Attenuation.NONE, 0, 0, 0, true);
	}

	/** Plays a song (from its intro, if it has one), replacing what's playing. */
	public static void play(String id) {
		if (id == null || id.isEmpty() || !exists(id)) {
			stop();
			return;
		}
		if (id.equals(current) && playing != null) return;
		stopSound();
		current = id;
		inIntro = exists(id + "_intro");
		looping = !inIntro;
		playing = sound(inIntro ? id + "_intro" : id, !inIntro);
		Minecraft.getInstance().getMusicManager().stopPlaying();
		if (jingle == null) Minecraft.getInstance().getSoundManager().play(playing);
	}

	/** A song played once through (a jingle that pauses the music). */
	public static void jingle(String id) {
		if (!exists(id)) return;
		Minecraft mc = Minecraft.getInstance();
		if (jinglePlaying != null) mc.getSoundManager().stop(jinglePlaying);
		if (playing != null) mc.getSoundManager().stop(playing);
		jingle = id;
		jinglePlaying = sound(id, false);
		mc.getSoundManager().play(jinglePlaying);
	}

	/** The place you're in now: its music plays (unless a battle's on). */
	public static void setPlace(String music) {
		place = fallback(music);
		if (!BattleScene.active()) play(place);
	}

	/** Back to the place's music (after a battle or the professor). */
	public static void resumePlace() {
		play(place);
	}

	/** Johto's own music isn't rendered yet: the nearest Kanto tune stands in. */
	private static String fallback(String music) {
		if (music == null || music.isEmpty() || exists(music)) return music;
		if (music.startsWith("route_") || music.startsWith("routes")) return "routes3";
		if (music.contains("cave") || music.contains("tower") || music.contains("ruins") || music.contains("mount") || music.contains("ice_path")
				|| music.contains("dragons") || music.contains("dark")) return "dungeon3";
		if (music.contains("rocket")) return "silph_co";
		if (music.contains("park") || music.contains("contest") || music.contains("lake")) return "routes2";
		if (music.contains("city") || music.contains("town")) return "cities1";
		return "routes1";
	}

	public static void stop() {
		stopSound();
		current = null;
	}

	private static void stopSound() {
		if (playing != null) Minecraft.getInstance().getSoundManager().stop(playing);
		playing = null;
	}

	public static void tick(Minecraft mc) {
		if (mc.level == null && !(mc.screen instanceof IntroScreen)) {
			if (current != null || jingle != null) {
				stop();
				jingle = null;
			}
			return;
		}
		if (jingle != null) {
			if (jinglePlaying != null && mc.getSoundManager().isActive(jinglePlaying)) return;
			jingle = null;
			jinglePlaying = null;
			// Carry on with the music from where it loops.
			if (current != null) {
				inIntro = false;
				looping = true;
				playing = sound(current, true);
				mc.getSoundManager().play(playing);
			}
			return;
		}
		if (current == null || playing == null) return;
		if (inIntro && !mc.getSoundManager().isActive(playing)) {
			inIntro = false;
			looping = true;
			playing = sound(current, true);
			mc.getSoundManager().play(playing);
		} else if (looping && !mc.getSoundManager().isActive(playing) && mc.options.getSoundSourceVolume(SoundSource.MUSIC) > 0) {
			// Stopped by something else (the volume slider): start again.
			playing = sound(current, true);
			mc.getSoundManager().play(playing);
		}
	}
}
