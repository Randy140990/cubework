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
 * and then let it carry on. A song with an intro plays the intro once and then loops the rest. It has its own
 * volume (OPTION in the START menu), so it plays even with Minecraft's music slider at 0, and Minecraft's own
 * music keeps quiet meanwhile.
 */
public final class Music {
	private Music() {}

	private static String current, place;
	private static SoundInstance playing;
	private static boolean inIntro, looping;
	private static String jingle;
	private static SoundInstance jinglePlaying;
	private static final Map<String, Boolean> HAS_INTRO = new HashMap<>();
	/** Retromon's music volume, 0 to 4 (OFF, LOW, MID, HIGH, MAX); saved in config/retromon-client.properties. */
	private static int level = -1;
	private static String logged;

	/**
	 * The sound channel the music plays on: Minecraft's "UI" channel where the game has one (it isn't paused
	 * with the game and players rarely turn it down), otherwise the master channel.
	 */
	private static final SoundSource SOURCE = source();

	private static SoundSource source() {
		try {
			return SoundSource.valueOf("UI");
		} catch (IllegalArgumentException e) {
			return SoundSource.MASTER;
		}
	}

	public static int level() {
		if (level < 0) level = Settings.get("music", 4, 0, 4);
		return level;
	}

	/** Sets the music volume (0 to 4), saves it and carries on the song at the new volume. */
	public static void setLevel(int value) {
		level = Math.max(0, Math.min(4, value));
		Settings.set("music", level);
		String song = current;
		stopSound();
		current = null;
		if (song != null && jingle == null) play(song);
		else current = song;
	}

	private static float volume() {
		return level() / 4F;
	}

	/** Whether Retromon's music is playing (Minecraft's own music stays quiet then). */
	public static boolean active() {
		return level() > 0 && (current != null || jingle != null);
	}

	private static boolean exists(String id) {
		return HAS_INTRO.computeIfAbsent(id, k -> Minecraft.getInstance().getResourceManager()
				.getResource(Identifier.fromNamespaceAndPath(Retromon.ID, "sounds/music/" + k + ".ogg")).isPresent());
	}

	private static SoundInstance sound(String id, boolean loop) {
		return new Song(Retromon.id("music." + id), loop);
	}

	/** How loud the music is let play (1 normally; down to a fifth while the Pokédex reads an entry out), and where it's going. */
	private static float duck = 1, duckTo = 1;

	/** Turns the music down (a fifth) or back up again, over about a second. */
	public static void duck(boolean down) {
		duckTo = down ? 0.2F : 1;
	}

	/** Whether the music's all the way down for a voice to be heard over it. */
	public static boolean ducked() {
		return duck <= 0.25F;
	}

	/** A song: its volume follows the music's volume and the ducking as it plays (Minecraft asks it each tick). */
	private static final class Song extends net.minecraft.client.resources.sounds.AbstractTickableSoundInstance {
		Song(Identifier id, boolean loop) {
			super(net.minecraft.sounds.SoundEvent.createVariableRangeEvent(id), SOURCE, RandomSource.create());
			this.looping = loop;
			this.delay = 0;
			this.attenuation = SoundInstance.Attenuation.NONE;
			this.relative = true;
			this.volume = volume() * duck;
		}

		@Override
		public void tick() {
			volume = volume() * duck;
		}
	}

	/** Plays a song (from its intro, if it has one), replacing what's playing. */
	public static void play(String id) {
		if (id == null || id.isEmpty() || !exists(id)) {
			if (id != null && !id.isEmpty()) Retromon.LOG.warn("Retromon music: no file for {}", id);
			stop();
			return;
		}
		if (id.equals(current) && playing != null) return;
		stopSound();
		current = id;
		if (level() == 0) return;
		if (!id.equals(logged)) {
			logged = id;
			boolean known = Minecraft.getInstance().getSoundManager().getSoundEvent(Retromon.id("music." + id)) != null;
			Retromon.LOG.info("Retromon music: {} on {} at {}%{}", id, SOURCE, level() * 25, known ? "" : " (sound event missing!)");
		}
		inIntro = exists(id + "_intro");
		looping = !inIntro;
		playing = sound(inIntro ? id + "_intro" : id, !inIntro);
		Minecraft.getInstance().getMusicManager().stopPlaying();
		if (jingle == null) Minecraft.getInstance().getSoundManager().play(playing);
	}

	/** A song played once through (a jingle that pauses the music). */
	public static void jingle(String id) {
		if (!exists(id) || level() == 0) return;
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
		// Ducking eases there and back over about a second.
		if (duck != duckTo) duck = duck < duckTo ? Math.min(duckTo, duck + 0.045F) : Math.max(duckTo, duck - 0.045F);
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
		if (current == null || playing == null || level() == 0) return;
		if (inIntro && !mc.getSoundManager().isActive(playing)) {
			inIntro = false;
			looping = true;
			playing = sound(current, true);
			mc.getSoundManager().play(playing);
		} else if (looping && !mc.getSoundManager().isActive(playing)) {
			// Stopped by something else (loading a world stops every sound): start again.
			playing = sound(current, true);
			mc.getSoundManager().play(playing);
		}
	}
}
