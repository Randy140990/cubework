package com.lumenforge.lens.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

/**
 * Finds, installs and tests FFmpeg, and builds its commands. FFmpeg runs as its own program next to the game, so
 * encoding never holds the game up. We fetch an LGPL build and encode on the graphics card when we can (NVIDIA
 * NVENC, AMD AMF, Intel Quick Sync, Apple VideoToolbox), so no GPL encoder ships with the mod.
 *
 * If Lightreel has already downloaded FFmpeg, that copy is used, so it isn't fetched twice.
 */
public final class Ffmpeg {
	public static final String WIN_URL = "https://github.com/BtbN/FFmpeg-Builds/releases/download/latest/ffmpeg-master-latest-win64-lgpl.zip";
	public static final String LINUX_URL = "https://github.com/BtbN/FFmpeg-Builds/releases/download/latest/ffmpeg-master-latest-linux64-lgpl.tar.xz";

	/** Tried in order for each codec; the first that runs on this PC is used. */
	static final String[] H264 = {"h264_nvenc", "h264_amf", "h264_qsv", "h264_videotoolbox", "h264_mf", "libx264", "libopenh264", "mpeg4"};
	static final String[] HEVC = {"hevc_nvenc", "hevc_amf", "hevc_qsv", "hevc_videotoolbox", "hevc_mf"};

	private static Path tools;
	private static final List<Path> OTHER_DIRS = new ArrayList<>();
	private static String custom = "";

	private static volatile String found;
	private static volatile long checkedAt;
	private static volatile boolean searching;
	private static volatile String h264, hevc;
	private static volatile boolean probed, probing;
	private static volatile int major = 99;

	private Ffmpeg() {}

	/** Our own tools folder, plus folders where another of our mods may have put FFmpeg already. */
	public static void setup(Path toolsDir, String customPath, Path... otherDirs) {
		tools = toolsDir;
		custom = customPath == null ? "" : customPath.trim();
		OTHER_DIRS.clear();
		OTHER_DIRS.addAll(List.of(otherDirs));
		forget();
	}

	public static Path dir() {
		return tools.resolve("ffmpeg");
	}

	/** The ffmpeg program to run, or null if there isn't one yet. Cheap to call every frame. */
	public static String find() {
		String f = found;
		if (f != null) return f;
		long now = System.currentTimeMillis();
		if (now - checkedAt < 3000 || searching) return null;
		checkedAt = now;
		searching = true;
		Thread t = new Thread(() -> {
			try {
				found = search();
			} finally {
				searching = false;
			}
		}, "Lightlens FFmpeg check");
		t.setDaemon(true);
		t.start();
		return null;
	}

	/** Looks now, on this thread (it can take a moment). */
	public static String findNow() {
		if (found == null) found = search();
		return found;
	}

	public static boolean searching() {
		return searching;
	}

	/** Looks again (after an install or a new path). */
	public static void forget() {
		found = null;
		checkedAt = 0;
		probed = false;
		h264 = hevc = null;
	}

	private static String search() {
		if (!custom.isEmpty() && Files.isRegularFile(Path.of(custom)) && works(custom)) return custom;
		String exe = Download.windows() ? "ffmpeg.exe" : "ffmpeg";
		List<Path> dirs = new ArrayList<>();
		if (tools != null) dirs.add(dir());
		dirs.addAll(OTHER_DIRS);
		for (Path d : dirs) {
			Path p = Download.findFile(d, exe);
			if (p != null) {
				p.toFile().setExecutable(true);
				if (works(p.toString())) return p.toString();
			}
		}
		if (works("ffmpeg")) return "ffmpeg";
		return null;
	}

	public static boolean canInstall() {
		return Download.windows() || Download.linux();
	}

	public static void install(BiConsumer<Long, Long> progress) throws IOException, InterruptedException {
		Path d = dir();
		Files.createDirectories(d);
		if (Download.windows()) {
			Path zip = d.resolve("ffmpeg.zip");
			Download.file(WIN_URL, zip, progress);
			Download.unzip(zip, d);
			Files.deleteIfExists(zip);
		} else {
			Path tar = d.resolve("ffmpeg.tar.xz");
			Download.file(LINUX_URL, tar, progress);
			Process p = new ProcessBuilder("tar", "-xJf", tar.toString(), "-C", d.toString()).redirectErrorStream(true).start();
			p.getInputStream().readAllBytes();
			p.waitFor();
			Files.deleteIfExists(tar);
		}
		forget();
	}

	static boolean works(String exe) {
		try {
			Process p = new ProcessBuilder(exe, "-hide_banner", "-version").redirectErrorStream(true).start();
			String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
			boolean ok = p.waitFor(10, TimeUnit.SECONDS) && p.exitValue() == 0;
			if (ok) major = parseMajor(out);
			return ok;
		} catch (Exception e) {
			return false;
		}
	}

	/** "ffmpeg version 7.1..." gives 7; builds named after a date or "N-..." count as new. */
	static int parseMajor(String versionText) {
		String s = versionText.trim();
		int i = s.indexOf("version ");
		if (i < 0) return 99;
		s = s.substring(i + 8);
		if (s.startsWith("n")) s = s.substring(1);
		int j = 0;
		while (j < s.length() && Character.isDigit(s.charAt(j))) j++;
		if (j == 0) return 99;
		try {
			int v = Integer.parseInt(s.substring(0, j));
			return v > 1000 ? 99 : v;
		} catch (NumberFormatException e) {
			return 99;
		}
	}

	// ---------------------------------------------------------------- encoders

	/** Tests the encoders once, in the background. */
	public static void probeAsync() {
		String exe = found;
		if (exe == null || probed || probing) return;
		probing = true;
		Thread t = new Thread(() -> {
			try {
				probe(exe);
			} finally {
				probing = false;
			}
		}, "Lightlens encoder check");
		t.setDaemon(true);
		t.start();
	}

	public static boolean probed() {
		return probed;
	}

	public static boolean probing() {
		return probing;
	}

	public static synchronized void probe(String exe) {
		if (probed) return;
		h264 = first(exe, H264);
		hevc = first(exe, HEVC);
		probed = true;
	}

	private static String first(String exe, String[] list) {
		for (String enc : list) if (test(exe, enc)) return enc;
		return null;
	}

	/** Encodes a tiny clip with this encoder, fed the way a recording feeds it. */
	static boolean test(String exe, String enc) {
		try {
			List<String> cmd = new ArrayList<>(List.of(exe, "-hide_banner", "-loglevel", "error", "-f", "lavfi", "-i",
					"color=c=black:s=640x360:r=30:d=0.3", "-vf", takesRgba(enc) ? "format=rgba" : "format=nv12", "-c:v", enc, "-f", "null", "-"));
			Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
			p.getInputStream().readAllBytes();
			return p.waitFor(25, TimeUnit.SECONDS) && p.exitValue() == 0;
		} catch (Exception e) {
			return false;
		}
	}

	/** The H.264 encoder found (never null once probed: mpeg4 at worst), or null before the test. */
	public static String h264() {
		return h264;
	}

	/** The H.265 encoder found, or null when there's none (only graphics-card ones are fast enough to use live). */
	public static String hevc() {
		return hevc;
	}

	/** True for encoders that run on the graphics card. */
	public static boolean hardware(String enc) {
		return enc != null && (enc.endsWith("_nvenc") || enc.endsWith("_amf") || enc.endsWith("_qsv") || enc.endsWith("_videotoolbox") || enc.endsWith("_mf"));
	}

	/** A name for the settings screen. */
	public static String label(String enc) {
		if (enc == null) return "none";
		if (enc.endsWith("_nvenc")) return "NVIDIA NVENC";
		if (enc.endsWith("_amf")) return "AMD AMF";
		if (enc.endsWith("_qsv")) return "Intel Quick Sync";
		if (enc.endsWith("_videotoolbox")) return "Apple VideoToolbox";
		if (enc.endsWith("_mf")) return "Windows Media Foundation";
		if (enc.equals("libx264")) return "x264 (processor)";
		if (enc.equals("libopenh264")) return "OpenH264 (processor)";
		return "MPEG-4 (processor)";
	}

	/** NVENC takes RGBA as it comes and changes it to YUV on the graphics card; the others get NV12 from FFmpeg. */
	static boolean takesRgba(String enc) {
		return enc.endsWith("_nvenc");
	}

	// ---------------------------------------------------------------- commands

	/**
	 * The live recording: timed RGBA frames come in on stdin as Matroska, a steady frame rate goes out, encoded,
	 * into a Matroska file (it's readable even if the game stops suddenly).
	 */
	public static List<String> liveCommand(String exe, String enc, Quality q, int w, int h, int fps, Path out) {
		List<String> c = new ArrayList<>(List.of(exe, "-hide_banner", "-loglevel", "warning", "-nostdin", "-y",
				"-f", "matroska", "-thread_queue_size", "256", "-i", "pipe:0"));
		c.addAll(major >= 5 ? List.of("-fps_mode", "cfr") : List.of("-vsync", "cfr"));
		c.addAll(List.of("-r", String.valueOf(fps)));
		if (!takesRgba(enc)) c.addAll(List.of("-vf", "scale=out_color_matrix=bt709:out_range=tv,format=nv12"));
		c.addAll(videoArgs(enc, q, w, h, fps));
		if (!takesRgba(enc)) c.addAll(List.of("-colorspace", "bt709", "-color_primaries", "bt709", "-color_trc", "bt709", "-color_range", "tv"));
		c.addAll(List.of("-f", "matroska", out.toString()));
		return c;
	}

	/** Encoder settings for live use: quick presets, quality-led with a bitrate ceiling. */
	static List<String> videoArgs(String enc, Quality q, int w, int h, int fps) {
		double mbps = Sizes.mbps(q, w, h, fps, enc != null && enc.startsWith("hevc"));
		String rate = fmt(mbps), max = fmt(mbps * 1.5), buf = fmt(mbps * 2);
		String gop = String.valueOf(fps * 2);
		List<String> a = new ArrayList<>(List.of("-c:v", enc));
		switch (enc) {
			case "h264_nvenc", "hevc_nvenc" -> a.addAll(List.of("-preset", "p4", "-tune", "hq", "-rc", "vbr", "-cq", String.valueOf(q.cq),
					"-b:v", rate, "-maxrate", max, "-bufsize", buf, "-profile:v", enc.startsWith("h264") ? "high" : "main", "-g", gop));
			case "h264_amf", "hevc_amf" -> a.addAll(List.of("-usage", "transcoding", "-quality", "balanced", "-rc", "vbr_peak", "-b:v", rate,
					"-maxrate", max, "-bufsize", buf, "-g", gop));
			case "h264_qsv", "hevc_qsv" -> a.addAll(List.of("-preset", "faster", "-b:v", rate, "-maxrate", max, "-bufsize", buf, "-g", gop));
			case "h264_videotoolbox", "hevc_videotoolbox" -> a.addAll(List.of("-realtime", "1", "-b:v", rate, "-maxrate", max, "-g", gop));
			case "h264_mf", "hevc_mf" -> a.addAll(List.of("-rate_control", "pc_vbr", "-b:v", rate, "-g", gop));
			case "libx264" -> a.addAll(List.of("-preset", "veryfast", "-tune", "zerolatency", "-crf", String.valueOf(q.cq), "-maxrate", max,
					"-bufsize", buf, "-g", gop));
			case "libopenh264" -> a.addAll(List.of("-b:v", rate, "-maxrate", max, "-g", gop));
			default -> a.addAll(List.of("-q:v", "3", "-g", gop));
		}
		return a;
	}

	/**
	 * The finished file: the recorded picture copied as it is (no second encode), with the game's sound and the
	 * microphone mixed and encoded to AAC. Written as MP4 to a temporary name.
	 */
	public static List<String> finishCommand(String exe, Path video, Path gameWav, Path micWav, double micVolume, Path out) {
		List<String> c = new ArrayList<>(List.of(exe, "-hide_banner", "-loglevel", "warning", "-nostdin", "-y", "-i", video.toString()));
		if (gameWav != null) c.addAll(List.of("-i", gameWav.toString()));
		if (micWav != null) c.addAll(List.of("-i", micWav.toString()));
		c.addAll(List.of("-map", "0:v:0", "-c:v", "copy"));
		if (gameWav != null && micWav != null) {
			c.addAll(List.of("-filter_complex", "[2:a]volume=" + fmt2(micVolume) + "[m];[1:a][m]amix=inputs=2:normalize=0:duration=longest[a]",
					"-map", "[a]"));
		} else if (gameWav != null) {
			c.addAll(List.of("-map", "1:a"));
		} else if (micWav != null) {
			c.addAll(List.of("-filter:a", "volume=" + fmt2(micVolume), "-map", "1:a"));
		}
		if (gameWav != null || micWav != null) c.addAll(List.of("-c:a", "aac", "-b:a", "192k", "-ar", "48000"));
		c.addAll(List.of("-f", "mp4", out.toString()));
		return c;
	}

	/** A recording the game didn't finish (it closed or crashed): its picture, as an MP4. */
	public static List<String> rescueCommand(String exe, Path video, Path micWav, Path out) {
		return finishCommand(exe, video, null, micWav, 1.0, out);
	}

	private static String fmt(double mbps) {
		return String.format(Locale.ROOT, "%.1fM", Math.max(1, mbps));
	}

	private static String fmt2(double v) {
		return String.format(Locale.ROOT, "%.2f", v);
	}

	/** Runs ffmpeg and waits; returns its log. Throws if it fails. */
	public static String run(List<String> cmd, long timeoutSec) throws IOException, InterruptedException {
		Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
		String log = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		if (!p.waitFor(timeoutSec, TimeUnit.SECONDS)) {
			p.destroyForcibly();
			throw new IOException("FFmpeg took too long");
		}
		if (p.exitValue() != 0) throw new IOException("FFmpeg failed: " + tail(log, 800));
		return log;
	}

	public static String tail(String s, int n) {
		s = s.trim();
		return s.length() <= n ? s : s.substring(s.length() - n);
	}
}
