package com.lumenforge.lens.core;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.TargetDataLine;
import java.nio.file.Path;

/** Your microphone, straight into a WAV file while you record (none of it is kept in memory). */
public final class Mic {
	public static final int RATE = 48000;
	private static volatile TargetDataLine line;
	private static volatile float level;
	private static volatile boolean paused;
	private static WavWriter wav;
	private static Thread thread;

	private Mic() {}

	public static boolean available() {
		try {
			return AudioSystem.isLineSupported(new javax.sound.sampled.DataLine.Info(TargetDataLine.class, new AudioFormat(RATE, 16, 1, true, false)));
		} catch (Throwable t) {
			return false;
		}
	}

	public static boolean on() {
		return line != null;
	}

	/** Loudness of the last bit, 0 to 1. */
	public static float level() {
		return level;
	}

	/** While paused, what the mic hears isn't kept (the recording's clock is stopped too). */
	public static void setPaused(boolean p) {
		paused = p;
	}

	public static synchronized void start(Path file) throws Exception {
		if (line != null) return;
		AudioFormat fmt = new AudioFormat(RATE, 16, 1, true, false);
		TargetDataLine l = AudioSystem.getTargetDataLine(fmt);
		l.open(fmt, RATE / 10 * 2);
		wav = new WavWriter(file, 1, RATE);
		WavWriter w = wav;
		paused = false;
		l.start();
		line = l;
		Thread t = new Thread(() -> {
			byte[] b = new byte[RATE / 50 * 2];
			try {
				while (line == l) {
					int n = l.read(b, 0, b.length);
					if (n <= 0) continue;
					if (!paused) w.writeRaw(b, 0, n);
					double sum = 0;
					for (int i = 0; i + 1 < n; i += 2) {
						short s = (short) ((b[i] & 255) | (b[i + 1] << 8));
						sum += s * (double) s;
					}
					level = (float) Math.min(1, Math.sqrt(sum / Math.max(1, n / 2)) / 8000.0);
				}
			} catch (Exception ignored) {
			}
		}, "Lightlens mic");
		t.setDaemon(true);
		thread = t;
		t.start();
	}

	/** Stops and closes the file. Returns its length in seconds. */
	public static synchronized double stop() {
		TargetDataLine l = line;
		if (l == null) return 0;
		line = null;
		try {
			l.stop();
			l.close();
			if (thread != null) thread.join(1000);
		} catch (Exception ignored) {
		}
		level = 0;
		double s = 0;
		try {
			s = wav.seconds();
			wav.close();
		} catch (Exception ignored) {
		}
		wav = null;
		return s;
	}
}
