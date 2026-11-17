package com.insidethecube.kantojohto.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.insidethecube.kantojohto.RouteOne;
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
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
	}

	public static void cry(String species, float pitch) {
		Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(RouteOne.id("cry." + species), SoundSource.NEUTRAL, 0.9F, pitch,
				RandomSource.create(), false, 0, SoundInstance.Attenuation.NONE, 0, 0, 0, true));
	}

	public static void fanfare() {
		tune(SoundEvents.NOTE_BLOCK_BELL, 3, 6, 10, 13, 18, -1, 18, 17, 18, -1, -1, 20, -1, 22);
		tune(SoundEvents.NOTE_BLOCK_BASS, 6, 6, 6, 1, 1, 6, -1, 8);
	}

	public static void heal() {
		tune(SoundEvents.NOTE_BLOCK_BELL, 4, 13, 13, 13, 9, -1, 13, 16, -1, 18);
	}

	public static void spotted() {
		tune(SoundEvents.NOTE_BLOCK_BIT, 2, 18, 22, 18, 22, 18, 22);
	}

	public static void caught() {
		tune(SoundEvents.NOTE_BLOCK_BELL, 3, 13, 17, 20, -1, 25, -1, 20, 25);
	}

	public static void levelUp() {
		tune(SoundEvents.NOTE_BLOCK_BELL, 3, 13, 15, 17, 18, 20, -1, 25);
	}

	public static void win() {
		tune(SoundEvents.NOTE_BLOCK_BELL, 4, 13, 17, 20, 25, -1, 20, 25);
		tune(SoundEvents.NOTE_BLOCK_BASS, 8, 1, 8, 13);
	}

	public static void badge() {
		tune(SoundEvents.NOTE_BLOCK_BELL, 3, 13, 13, 13, 17, -1, 15, 15, 15, 18, -1, 17, 20, 25);
	}

	public static void select() {
		play(SoundEvents.NOTE_BLOCK_HAT, pitch(18), 0.5F);
	}

	public static void attack() {
		play(SoundEvents.NOTE_BLOCK_SNARE, pitch(8), 0.7F);
	}

	public static void hit() {
		play(SoundEvents.NOTE_BLOCK_BASEDRUM, pitch(4), 0.9F);
		play(SoundEvents.NOTE_BLOCK_SNARE, pitch(2), 0.6F);
	}

	public static void stat(boolean up) {
		tune(SoundEvents.NOTE_BLOCK_CHIME, 2, up ? new int[] {10, 14, 18} : new int[] {18, 14, 10});
	}

	public static void shake() {
		play(SoundEvents.NOTE_BLOCK_HAT, pitch(6), 0.8F);
	}

	public static void ball() {
		tune(SoundEvents.NOTE_BLOCK_XYLOPHONE, 2, 18, 13, 6);
	}

	public static void run() {
		tune(SoundEvents.NOTE_BLOCK_HAT, 2, 10, 13, 16, 20);
	}
}
