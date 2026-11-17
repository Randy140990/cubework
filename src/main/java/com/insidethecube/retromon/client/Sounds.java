package com.insidethecube.retromon.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.insidethecube.retromon.Retromon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/**
 * The game's sounds: Pokémon cries, and little tunes played on note blocks (the title fanfare, healing,
 * a trainer spotting you, a catch, a level up) along with the beeps and thumps of a battle.
 */
public final class Sounds {
	private Sounds() {}

	private record Note(int at, Holder<SoundEvent> sound, float pitch, float volume) {}

	private static final List<Note> QUEUE = new ArrayList<>();
	private static int now;

	/** A note block's pitch for a note: 0 is F#3, 12 is F#4, 24 is F#5. */
	private static float pitch(int note) {
		return (float) Math.pow(2, (note - 12) / 12.0);
	}

	private static void tune(Holder<SoundEvent> sound, int step, int... notes) {
		int at = now;
		for (int n : notes) {
			if (n >= 0) QUEUE.add(new Note(at, sound, pitch(n), 0.6F));
			at += step;
		}
	}

	public static void tick() {
		now++;
		Iterator<Note> it = QUEUE.iterator();
		while (it.hasNext()) {
			Note n = it.next();
			if (n.at > now) continue;
			play(n.sound, n.pitch, n.volume);
			it.remove();
		}
	}

	public static void play(Holder<SoundEvent> sound, float pitch, float volume) {
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound.value(), pitch, volume));
	}

	public static void cry(String species, float pitch) {
		if (species == null || species.isEmpty()) return;
		Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(Retromon.id("cry." + species), SoundSource.NEUTRAL, 0.9F, pitch,
				RandomSource.create(), false, 0, SoundInstance.Attenuation.NONE, 0, 0, 0, true));
	}

	/** One of the game's jingles (healing, a catch, a level up, an item), pausing the music. */
	public static void jingle(String id) {
		Music.jingle(id);
	}

	public static void fanfare() {
		tune(SoundEvents.NOTE_BLOCK_BELL, 3, 6, 10, 13, 18, -1, 18, 17, 18, -1, -1, 20, -1, 22);
		tune(SoundEvents.NOTE_BLOCK_BASS, 6, 6, 6, 1, 1, 6, -1, 8);
	}

	public static void heal() {
		Music.jingle("pkmn_healed");
	}

	public static void spotted(String music) {
		Music.play(music == null || music.isEmpty() ? "meet_male_trainer" : music);
	}

	public static void caught() {
		Music.jingle("caught_mon");
	}

	public static void levelUp() {
		Music.jingle("level_up");
	}

	public static void win() {
		tune(SoundEvents.NOTE_BLOCK_BELL, 4, 13, 17, 20, 25, -1, 20, 25);
		tune(SoundEvents.NOTE_BLOCK_BASS, 8, 1, 8, 13);
	}

	public static void badge() {
		tune(SoundEvents.NOTE_BLOCK_BELL, 3, 13, 13, 13, 17, -1, 15, 15, 15, 18, -1, 17, 20, 25);
	}

	/** One of the games' little sounds (sounds/sfx): the A press, bumps, balls, hits and so on. */
	public static void sfx(String id) {
		sfx(id, 1F, 0.8F);
	}

	public static void sfx(String id, float pitch, float volume) {
		if (id == null || id.isEmpty()) return;
		Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(Retromon.id("sfx." + id), SoundSource.UI, volume, pitch,
				RandomSource.create(), false, 0, SoundInstance.Attenuation.NONE, 0, 0, 0, true));
	}

	/** The A press (the games' click as the text moves on or a choice is made). */
	public static void press() {
		sfx("press", 1F, 0.55F);
	}

	public static void select() {
		press();
	}

	public static void attack() {
		sfx("tackle", 1F, 0.6F);
	}

	public static void hit() {
		sfx("damage");
	}

	/** A hit by how well it went: super effective, not very effective, or plain. */
	public static void hit(int effect) {
		sfx(effect > 0 ? "super" : effect < 0 ? "weak" : "damage");
	}

	public static void stat(boolean up) {
		tune(SoundEvents.NOTE_BLOCK_CHIME, 2, up ? new int[] {10, 14, 18} : new int[] {18, 14, 10});
	}

	public static void shake() {
		sfx("tink");
	}

	public static void ball() {
		sfx("ball_toss");
	}

	public static void poof() {
		sfx("ball_poof");
	}

	public static void run() {
		sfx("run");
	}

	public static void faint() {
		sfx("faint_fall");
	}
}
