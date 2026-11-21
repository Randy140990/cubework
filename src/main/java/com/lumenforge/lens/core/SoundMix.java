package com.lumenforge.lens.core;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * The game's sound for a recording, rebuilt from what played. Minecraft's sound goes straight to your speakers, so
 * while you record each sound that starts is noted (which file, when) and, twenty times a second, how loud it is and
 * where it sits from your ears (left, right, how far). Afterwards those are mixed into one stereo WAV, a second at a
 * time, so a long recording doesn't need much memory.
 */
public final class SoundMix {
	public static final int RATE = 48000;

	private SoundMix() {}

	/** A decoded sound: 16-bit samples, interleaved when stereo. */
	public record Clip(short[] data, int channels, int rate) {
		public int frames() {
			return data.length / channels;
		}
	}

	/** One sound playing: its file, when it started, and how it sounded as it went. */
	public static final class Voice {
		public final String file;
		public final boolean loop;
		public final double start;
		/** When it was stopped (seconds from the start of the recording); infinite while it runs to its end. */
		public volatile double end = Double.POSITIVE_INFINITY;

		private float[] t = new float[8], vol = new float[8], att = new float[8], pan = new float[8], pitch = new float[8];
		private boolean[] playing = new boolean[8];
		private int n;

		// Mixing state.
		private double pos;
		private int k;
		private boolean done;

		public Voice(String file, boolean loop, double start) {
			this.file = file;
			this.loop = loop;
			this.start = start;
		}

		/**
		 * How it sounds at time `time`: `volume` is the game's volume for it (its category and Master included),
		 * `attenuation` 0..1 for distance, `panning` -1 (left) to 1 (right), and whether it's playing (sounds stop
		 * while the game is paused).
		 */
		public synchronized void key(double time, float volume, float attenuation, float panning, float pitchV, boolean isPlaying) {
			if (n > 0 && time <= t[n - 1]) {
				int i = n - 1;
				vol[i] = volume;
				att[i] = attenuation;
				pan[i] = panning;
				pitch[i] = pitchV;
				playing[i] = isPlaying;
				return;
			}
			if (n == t.length) {
				int m = n * 2;
				t = java.util.Arrays.copyOf(t, m);
				vol = java.util.Arrays.copyOf(vol, m);
				att = java.util.Arrays.copyOf(att, m);
				pan = java.util.Arrays.copyOf(pan, m);
				pitch = java.util.Arrays.copyOf(pitch, m);
				playing = java.util.Arrays.copyOf(playing, m);
			}
			t[n] = (float) time;
			vol[n] = volume;
			att[n] = attenuation;
			pan[n] = panning;
			pitch[n] = pitchV;
			playing[n] = isPlaying;
			n++;
		}

		public synchronized int keys() {
			return n;
		}
	}

	/**
	 * Mixes the voices into a stereo WAV of `duration` seconds. `load` gives each file's samples (or null if it
	 * can't be read). Returns false if there was nothing to hear.
	 */
	public static boolean render(List<Voice> all, double duration, Function<String, Clip> load, Path wav, java.util.function.DoubleConsumer progress)
			throws java.io.IOException {
		List<Voice> voices = new ArrayList<>();
		for (Voice v : all) if (v.keys() > 0 && v.start < duration) voices.add(v);
		if (voices.isEmpty()) return false;
		voices.sort(Comparator.comparingDouble(v -> v.start));
		int total = (int) Math.ceil(duration * RATE);
		float[] block = new float[RATE * 2];
		List<Voice> active = new ArrayList<>();
		int nextVoice = 0;
		try (WavWriter out = new WavWriter(wav, 2, RATE)) {
			for (int b0 = 0; b0 < total; b0 += RATE) {
				int len = Math.min(RATE, total - b0);
				java.util.Arrays.fill(block, 0);
				double blockEnd = (b0 + len) / (double) RATE;
				while (nextVoice < voices.size() && voices.get(nextVoice).start < blockEnd) {
					Voice v = voices.get(nextVoice++);
					v.pos = 0;
					v.k = 0;
					v.done = false;
					active.add(v);
				}
				for (int a = active.size() - 1; a >= 0; a--) {
					Voice v = active.get(a);
					Clip c = load.apply(v.file);
					if (c == null || c.frames() < 2) {
						active.remove(a);
						continue;
					}
					mixVoice(v, c, block, b0, len);
					if (v.done) active.remove(a);
				}
				for (int i = 0; i < len * 2; i++) block[i] = soften(block[i]);
				out.write(block, 0, len * 2);
				if (progress != null) progress.accept((b0 + len) / (double) total);
			}
		}
		return true;
	}

	private static void mixVoice(Voice v, Clip c, float[] block, int b0, int len) {
		int frames = c.frames();
		boolean stereo = c.channels() >= 2;
		double step0 = c.rate() / (double) RATE;
		short[] d = c.data();
		int ch = c.channels();
		for (int i = 0; i < len; i++) {
			double time = (b0 + i) / (double) RATE;
			if (time < v.start) continue;
			if (time >= v.end) {
				v.done = true;
				return;
			}
			while (v.k + 1 < v.n && v.t[v.k + 1] <= time) v.k++;
			int k = v.k;
			if (!v.playing[k]) continue;
			float vol, att, pan, pitch;
			if (k + 1 < v.n && v.t[k + 1] > v.t[k]) {
				float f = (float) ((time - v.t[k]) / (v.t[k + 1] - v.t[k]));
				f = Math.max(0, Math.min(1, f));
				vol = v.vol[k] + (v.vol[k + 1] - v.vol[k]) * f;
				att = v.att[k] + (v.att[k + 1] - v.att[k]) * f;
				pan = v.pan[k] + (v.pan[k + 1] - v.pan[k]) * f;
				pitch = v.pitch[k];
			} else {
				vol = v.vol[k];
				att = v.att[k];
				pan = v.pan[k];
				pitch = v.pitch[k];
			}
			int a = (int) v.pos;
			if (a + 1 >= frames) {
				if (v.loop) {
					v.pos -= frames - 1;
					a = (int) v.pos;
				} else {
					v.done = true;
					return;
				}
			}
			double f = v.pos - a;
			float l, r;
			if (stereo) {
				l = (float) (d[a * ch] * (1 - f) + d[(a + 1) * ch] * f) / 32768f;
				r = (float) (d[a * ch + 1] * (1 - f) + d[(a + 1) * ch + 1] * f) / 32768f;
				// Stereo sounds (music, some ambience) aren't placed in the world: they play as they are.
				block[i * 2] += l * vol;
				block[i * 2 + 1] += r * vol;
			} else {
				float s = (float) (d[a * ch] * (1 - f) + d[(a + 1) * ch] * f) / 32768f;
				double ang = (Math.max(-1, Math.min(1, pan)) + 1) * Math.PI / 4;
				float gl = (float) Math.min(1, Math.cos(ang) * Math.sqrt(2)), gr = (float) Math.min(1, Math.sin(ang) * Math.sqrt(2));
				float g = vol * att;
				block[i * 2] += s * g * gl;
				block[i * 2 + 1] += s * g * gr;
			}
			v.pos += step0 * pitch;
		}
	}

	/** Leaves quiet sound alone and rounds off peaks, so a busy moment doesn't crackle. */
	static float soften(float x) {
		float ax = Math.abs(x);
		if (ax <= 0.8f) return x;
		float y = 0.8f + 0.2f * (float) Math.tanh((ax - 0.8f) / 0.2f);
		return Math.copySign(y, x);
	}

	/** Linear distance fall-off, as the game sets OpenAL: full at the sound, silent at `range` blocks. */
	public static float attenuation(double distance, double range) {
		if (range <= 0) return 1;
		return (float) Math.max(0, Math.min(1, 1 - distance / range));
	}
}
