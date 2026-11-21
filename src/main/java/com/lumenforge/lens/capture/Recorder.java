package com.lumenforge.lens.capture;

import com.lumenforge.lens.Lens;
import com.lumenforge.lens.Settings;
import com.lumenforge.lens.audio.GameSound;
import com.lumenforge.lens.core.Ffmpeg;
import com.lumenforge.lens.core.Mic;
import com.lumenforge.lens.core.MkvWriter;
import com.lumenforge.lens.core.SoundMix;
import com.lumenforge.lens.core.Sizes;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL30;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Recording what you see. Each frame, as it's about to go to the screen (menus, other mods' windows and your
 * shaders all in it), the picture is copied on the graphics card to the video's size, read back without waiting,
 * and passed to FFmpeg with the time it was shown. FFmpeg encodes it on the graphics card as you play, into a
 * working file. When you stop, the game's sound (and your mic) is added and the finished MP4 goes into your folder:
 * a few seconds, not a long export.
 */
public final class Recorder {
	public enum State { IDLE, STARTING, RECORDING, FINISHING }

	private record Frame(byte[] data, long time) {}

	private static final Frame END = new Frame(null, -1);

	private static volatile State state = State.IDLE;
	/** What's happening, for the settings screen ("Finishing: adding the sound"). */
	public static volatile String status = "";
	/** The last finished file and any error, for the settings screen. */
	public static volatile Path lastFile;
	public static volatile String lastError;

	// This take.
	private static int outW, outH, fps;
	private static boolean sharp;
	private static Sizes.Shape shape;
	private static Path workDir, videoFile, micFile;
	private static Process ffmpeg;
	private static Thread writer;
	private static final StringBuilder LOG = new StringBuilder();
	private static ArrayBlockingQueue<Frame> queue;
	private static ArrayBlockingQueue<byte[]> pool;
	private static volatile IOException writeError;
	private static final Gl.Target TARGET = new Gl.Target();
	private static final Gl.Readback READBACK = new Gl.Readback(4);

	private static long startNanos, pausedNanos, pauseStart;
	private static volatile boolean paused;
	private static long lastSlot;
	public static volatile int captured, dropped;
	private static long nextSpaceCheck;
	private static String encoder;

	private Recorder() {}

	public static State state() {
		return state;
	}

	public static boolean recording() {
		return state == State.RECORDING;
	}

	public static boolean busy() {
		return state != State.IDLE;
	}

	public static boolean paused() {
		return paused;
	}

	public static int width() {
		return outW;
	}

	public static int height() {
		return outH;
	}

	public static int fps() {
		return fps;
	}

	public static String encoder() {
		return encoder;
	}

	/** Milliseconds recorded so far (paused time left out). */
	public static long clockMs() {
		if (startNanos == 0) return 0;
		long now = paused ? pauseStart : System.nanoTime();
		return (now - startNanos - pausedNanos) / 1_000_000L;
	}

	public static double clockSeconds() {
		return clockMs() / 1000.0;
	}

	/** F10: start, or stop and save. */
	public static void toggle() {
		if (state == State.RECORDING) stop(null);
		else if (state == State.IDLE) start();
	}

	public static void togglePause() {
		if (state != State.RECORDING) return;
		if (paused) {
			pausedNanos += System.nanoTime() - pauseStart;
			paused = false;
		} else {
			pauseStart = System.nanoTime();
			paused = true;
		}
		Mic.setPaused(paused);
		Minecraft.getInstance().updateTitle();
	}

	// ---------------------------------------------------------------- start

	/** Why recording can't start now, or null if it can. */
	public static String cantStart() {
		if (state != State.IDLE) return "Already recording";
		if (com.lumenforge.lens.compat.Lightreel.exporting()) return "Lightreel is exporting; wait for it to finish";
		if (Ffmpeg.find() == null) return Ffmpeg.searching() ? "Looking for FFmpeg, try again in a moment" : "FFmpeg isn't installed yet (Lightlens settings, Get FFmpeg)";
		if (!Ffmpeg.probed()) {
			Ffmpeg.probeAsync();
			return "Checking your graphics card's video encoder, try again in a moment";
		}
		if (Ffmpeg.h264() == null) return "This FFmpeg can't make video here. Pick another in config/lightlens.json (ffmpegPath)";
		return null;
	}

	public static void start() {
		Minecraft mc = Minecraft.getInstance();
		Settings s = Lens.settings;
		if (!s.folderChosen) {
			mc.setScreen(new com.lumenforge.lens.ui.LensScreen(mc.screen, "Choose where your videos go, then press Start."));
			return;
		}
		String why = cantStart();
		if (why != null) {
			Notify.toast("Can't record", why);
			return;
		}
		Path out = Lens.output();
		try {
			Files.createDirectories(out);
			if (!Files.isWritable(out)) throw new IOException("can't write there");
		} catch (Exception e) {
			Notify.toast("Can't record", "The folder " + out + " can't be used: " + e.getMessage());
			return;
		}

		boolean hevc = s.hevc && Ffmpeg.hevc() != null;
		encoder = hevc ? Ffmpeg.hevc() : Ffmpeg.h264();
		fps = s.fps;
		shape = s.shape;
		int rw = Capture.realWidth(), rh = Capture.realHeight();
		if (rw <= 0 || rh <= 0) {
			Notify.toast("Can't record", "The game window is minimised");
			return;
		}
		int[] size = Sizes.output(s.videoSize, shape, rw, rh);
		outW = size[0];
		outH = size[1];
		int k = s.sharp ? RenderScale.needed(outH, Sizes.crop(shape, rw, rh)[3], 3) : 1;
		sharp = k > 1;

		long perMin = Sizes.bytesPerMinute(s.quality, outW, outH, fps, hevc);
		long free = Capture.freeSpace(out);
		if (free >= 0 && free < perMin * 2 + 500_000_000L) {
			Notify.toast("Can't record", "Not enough space in " + out.getFileName() + " (" + Sizes.bytes(free) + " free)");
			return;
		}

		String stamp = Capture.stamp();
		workDir = Lens.work().resolve(stamp.replace(' ', '_'));
		videoFile = workDir.resolve("video.mkv");
		micFile = workDir.resolve("mic.wav");
		try {
			Files.createDirectories(workDir);
			Files.writeString(workDir.resolve("take.txt"), "name=" + stamp + "\nout=" + out + "\n", StandardCharsets.UTF_8);
		} catch (IOException e) {
			Notify.toast("Can't record", "Couldn't make the working folder: " + e.getMessage());
			return;
		}

		int frameBytes = outW * outH * 4;
		int slots = frameBytes >= 3840 * 2160 * 4 ? 4 : 6;
		try {
			pool = new ArrayBlockingQueue<>(slots);
			for (int i = 0; i < slots; i++) pool.add(new byte[frameBytes]);
		} catch (OutOfMemoryError e) {
			pool = null;
			Notify.toast("Can't record", "Not enough memory for " + outW + "x" + outH + ". Try a smaller size.");
			return;
		}
		queue = new ArrayBlockingQueue<>(slots);
		writeError = null;
		captured = dropped = 0;
		lastError = null;
		synchronized (LOG) {
			LOG.setLength(0);
		}

		List<String> cmd = Ffmpeg.liveCommand(Ffmpeg.find(), encoder, s.quality, outW, outH, fps, videoFile);
		Lens.LOG.info("Recording {}x{} at {} fps with {}: {}", outW, outH, fps, encoder, String.join(" ", cmd));
		try {
			ffmpeg = new ProcessBuilder(cmd).redirectErrorStream(true).start();
		} catch (IOException e) {
			Notify.toast("Can't record", "FFmpeg didn't start: " + e.getMessage());
			pool = null;
			return;
		}
		Process p = ffmpeg;
		Thread logReader = new Thread(() -> readLog(p.getInputStream()), "Lightlens FFmpeg log");
		logReader.setDaemon(true);
		logReader.start();
		OutputStream pipe = new BufferedOutputStream(p.getOutputStream(), 1 << 20);
		MkvWriter mkv = new MkvWriter(pipe, outW, outH);
		writer = new Thread(() -> writeLoop(p, pipe, mkv), "Lightlens video writer");
		writer.setDaemon(true);
		writer.start();

		if (s.mic) {
			try {
				Mic.start(micFile);
			} catch (Exception e) {
				Notify.toast("No microphone", "Recording without it: " + e.getMessage());
			}
		}
		gameSoundOn = s.gameSound;
		if (s.gameSound) GameSound.begin();

		paused = false;
		pausedNanos = 0;
		lastSlot = -1;
		phase = -1;
		nextSpaceCheck = System.currentTimeMillis() + 5000;
		startNanos = System.nanoTime();
		state = State.RECORDING;
		status = "Recording";
		if (sharp) RenderScale.setVideo(k);
		mc.updateTitle();
	}

	private static void readLog(InputStream in) {
		try {
			byte[] b = new byte[4096];
			int n;
			while ((n = in.read(b)) > 0) synchronized (LOG) {
				LOG.append(new String(b, 0, n, StandardCharsets.UTF_8));
				if (LOG.length() > 20000) LOG.delete(0, LOG.length() - 10000);
			}
		} catch (IOException ignored) {
		}
	}

	private static String log() {
		synchronized (LOG) {
			return Ffmpeg.tail(LOG.toString(), 1200);
		}
	}

	/** The writer thread: frames to FFmpeg, in order, with their times. The last frame is held to close the video. */
	private static void writeLoop(Process p, OutputStream pipe, MkvWriter mkv) {
		byte[] held = null;
		try {
			mkv.writeHeader(Lens.NAME + " " + Lens.version());
			while (true) {
				Frame f = queue.take();
				if (f.data == null) {
					// Stopping: the last picture again at the stop time, so the video lasts as long as the sound.
					if (held != null && f.time > 0) mkv.writeFrame(f.time, held, 0);
					break;
				}
				mkv.writeFrame(f.time, f.data, 0);
				if (held != null) pool.offer(held);
				held = f.data;
			}
		} catch (IOException e) {
			writeError = new IOException(p.isAlive() ? e.getMessage() : "FFmpeg stopped. " + log(), e);
		} catch (InterruptedException ignored) {
		} finally {
			// Closing sends what's buffered, then tells FFmpeg that's the end.
			try {
				pipe.close();
			} catch (IOException e) {
				if (writeError == null) writeError = e;
			}
		}
	}

	// ---------------------------------------------------------------- each frame

	/**
	 * The frame is finished and about to be shown (the window's back buffer holds exactly what you see). Records it
	 * (unless the game's drawing bigger than the window: then the main picture was taken just before), collects
	 * frames the graphics card has finished reading, and draws the red dot.
	 */
	static void onFrameEnd(int winW, int winH) {
		if (state != State.RECORDING) return;
		if (writeError != null) {
			stop("Recording stopped: " + writeError.getMessage());
			return;
		}
		Gl.save();
		try {
			READBACK.ensure(outW, outH);
			TARGET.ensure(outW, outH);
			READBACK.collect(Recorder::hand, false);
			// Minimised, the game doesn't show a frame: nothing new to record (the video holds the last picture).
			if (!paused && !sharp && winW > 0 && winH > 0 && !Minecraft.getInstance().getWindow().isMinimized()) {
				Gl.readFromWindow();
				grab(winW, winH);
			}
		} finally {
			Gl.restore();
		}
		long now = System.currentTimeMillis();
		if (now > nextSpaceCheck) {
			nextSpaceCheck = now + 3000;
			checkSpace();
		}
	}

	/** Drawing bigger than the window: the game's main picture, at its full size, just before it's shown. */
	static void onMainPicture(int texture, int w, int h) {
		if (state != State.RECORDING || !sharp || paused) return;
		Gl.save();
		try {
			READBACK.ensure(outW, outH);
			TARGET.ensure(outW, outH);
			Gl.readFrom(texture);
			grab(w, h);
		} finally {
			Gl.restore();
		}
	}

	/**
	 * If this frame's due, copies what's bound for reading into the next buffer. The video's frame slots (1/fps of a
	 * second each) are lined up with the first frame recorded, and a frame up to 30% of a slot early counts for it, so
	 * a game running at the video's frame rate gives every slot one frame instead of now and then two and then none.
	 */
	private static void grab(int srcW, int srcH) {
		long ms = clockMs();
		double frameMs = 1000.0 / fps;
		if (phase < 0) phase = ms;
		long slot = (long) Math.floor((ms - phase) / frameMs + 0.3);
		if (slot <= lastSlot) return;
		lastSlot = slot;
		if (READBACK.full()) {
			dropped++;
			return;
		}
		int[] c = Sizes.crop(shape, srcW, srcH);
		Gl.blitInto(TARGET, srcH, c[0], c[1], c[2], c[3]);
		GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, TARGET.fbo);
		READBACK.submit(Math.round(phase + slot * frameMs));
		captured++;
	}

	/** When the first frame was recorded (ms on the recording's clock), or -1 before it. */
	private static double phase = -1;

	/** A frame the graphics card has finished: copied out and queued for the writer, or dropped if it's behind. */
	private static void hand(ByteBuffer rgba, long time) {
		byte[] b = pool.poll();
		if (b == null) {
			dropped++;
			return;
		}
		rgba.get(0, b, 0, Math.min(b.length, rgba.remaining()));
		if (!queue.offer(new Frame(b, time))) {
			pool.offer(b);
			dropped++;
		}
	}

	private static void checkSpace() {
		long a = Capture.freeSpace(Lens.work()), b = Capture.freeSpace(Lens.output());
		long min = Math.min(a < 0 ? Long.MAX_VALUE : a, b < 0 ? Long.MAX_VALUE : b);
		if (min < 1_000_000_000L) stop("Stopped and saved: your disk is nearly full (" + Sizes.bytes(min) + " left)");
	}

	// ---------------------------------------------------------------- stop

	/** Stops and saves. `why` is shown if it's not you stopping it. */
	public static void stop(String why) {
		if (state != State.RECORDING) return;
		Minecraft mc = Minecraft.getInstance();
		long end = clockMs();
		if (paused) togglePause();
		state = State.FINISHING;
		status = "Finishing";
		// What the graphics card still has, then let go of its buffers.
		try {
			Gl.save();
			try {
				READBACK.collect(Recorder::hand, true);
			} finally {
				Gl.restore();
			}
		} catch (Throwable t) {
			Lens.LOG.warn("Collecting the last frames: {}", t.toString());
		}
		READBACK.free();
		TARGET.free();
		if (sharp) RenderScale.setVideo(1);
		List<SoundMix.Voice> voices = GameSound.end(end / 1000.0);
		mc.updateTitle();
		int c = captured, d = dropped;
		Path work = workDir, video = videoFile, mic = Mic.on() ? micFile : null;
		Process p = ffmpeg;
		Thread w = writer;
		String exe = Ffmpeg.find();
		Path outDir = Lens.output();
		String name = work.getFileName().toString().replace('_', ' ');
		double seconds = end / 1000.0;
		if (why != null) lastError = why;
		Thread t = new Thread(() -> finish(why, exe, p, w, end, voices, work, video, mic, outDir, name, seconds, c, d), "Lightlens finishing");
		finishing = t;
		t.start();
	}

	private static volatile Thread finishing;
	private static boolean gameSoundOn;

	private static void finish(String why, String exe, Process p, Thread w, long end, List<SoundMix.Voice> voices, Path work, Path video, Path mic,
			Path outDir, String name, double seconds, int c, int d) {
		Path result = null;
		String error = null;
		try {
			status = "Finishing the picture";
			while (!queue.offer(new Frame(null, end), 1, TimeUnit.SECONDS)) if (!w.isAlive()) break;
			w.join(120_000);
			if (!p.waitFor(180, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				throw new IOException("FFmpeg didn't finish the video");
			}
			if (p.exitValue() != 0 || !Files.isRegularFile(video) || Files.size(video) < 1000) throw new IOException("FFmpeg failed: " + log());
			if (mic != null) Mic.stop();
			Path game = null;
			if (!voices.isEmpty()) {
				status = "Mixing the game's sound";
				Path wav = work.resolve("game.wav");
				if (SoundMix.render(voices, seconds, GameSound::clip, wav, f -> status = "Mixing the game's sound (" + Math.round(f * 100) + "%)")) game = wav;
				GameSound.forgetClips();
			}
			Files.createDirectories(outDir);
			Path mp4 = Capture.unique(outDir, name, ".mp4");
			Path part = mp4.resolveSibling(mp4.getFileName() + ".part");
			status = "Saving the MP4";
			long videoBytes = Files.size(video);
			Thread watch = new Thread(() -> {
				while (true) {
					try {
						Thread.sleep(400);
						if (Files.exists(part)) status = "Saving the MP4 (" + Math.min(99, Files.size(part) * 100 / Math.max(1, videoBytes)) + "%)";
					} catch (Exception e) {
						return;
					}
				}
			}, "Lightlens progress");
			watch.setDaemon(true);
			watch.start();
			try {
				Ffmpeg.run(Ffmpeg.finishCommand(exe, video, game, mic, Lens.settings.micVolume, gameSoundOn ? seconds : 0, part), 3600);
			} finally {
				watch.interrupt();
			}
			Files.move(part, mp4, StandardCopyOption.REPLACE_EXISTING);
			result = mp4;
			Capture.deleteTree(work);
			Lens.LOG.info("Saved {} ({} frames captured, {} dropped)", mp4, c, d);
		} catch (Exception e) {
			error = e.getMessage();
			Lens.LOG.error("Finishing the recording failed; the picture is kept in {}", work, e);
		} finally {
			if (Mic.on()) Mic.stop();
			pool = null;
			queue = null;
			ffmpeg = null;
			startNanos = 0;
		}
		Path saved = result;
		String err = error;
		Minecraft.getInstance().execute(() -> {
			state = State.IDLE;
			status = "";
			if (saved != null) {
				lastFile = saved;
				long size = 0;
				try {
					size = Files.size(saved);
				} catch (IOException ignored) {
				}
				String what = Sizes.clock((long) (seconds * 1000)) + ", " + outW + "x" + outH + ", " + Sizes.bytes(size);
				Notify.saved(why != null ? why : "Video saved", saved, what + (d > c / 20 && c > 0 ? " · some frames were skipped" : ""));
			} else {
				lastError = err;
				Notify.toast("Video not finished", (err == null ? "" : err) + " The picture is kept in .minecraft/lightlens/work.");
			}
		});
	}

	/** Quitting the game mid-recording: stop, and wait (up to two minutes) for the MP4 to be saved. */
	public static void onShutdown() {
		if (state == State.RECORDING) stop(null);
		Thread t = finishing;
		if (t != null) {
			try {
				t.join(120_000);
			} catch (InterruptedException ignored) {
			}
		}
	}
}
