package com.lumenforge.lens.test;

import com.lumenforge.lens.Lens;
import com.lumenforge.lens.capture.Photo;
import com.lumenforge.lens.capture.Recorder;
import com.lumenforge.lens.core.Ffmpeg;
import com.lumenforge.lens.core.Quality;
import com.lumenforge.lens.core.Sizes;
import com.lumenforge.lens.ui.LensScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * A real 1.21.11 client on the build server: records a clip (walking, a menu open, sounds playing), takes photos
 * while recording and not, records again with Sharper on, and checks every file with ffprobe. The middle frame of
 * each video is saved as a picture so it can be looked at.
 */
public final class LensGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext ctx) {
		ctx.getInput().resizeWindow(1280, 720);
		ctx.waitTicks(5);
		Path game = FabricLoader.getInstance().getGameDir();
		Path out = game.resolve("test-captures");
		ctx.runOnClient(mc -> {
			Lens.settings.folder = out.toString();
			Lens.settings.folderChosen = true;
			Lens.settings.videoSize = Sizes.Size.K1;
			Lens.settings.fps = 30;
			Lens.settings.quality = Quality.HIGH;
			Lens.settings.gameSound = true;
			Lens.settings.mic = false;
			Lens.settings.sharp = false;
			Lens.settings.photoSize = Sizes.Size.K4;
		});
		String custom = System.getenv("LIGHTLENS_FFMPEG");
		if (custom != null && !custom.isBlank()) {
			ctx.runOnClient(mc -> {
				Lens.settings.ffmpegPath = custom;
				Lens.setupFfmpeg();
			});
		}
		String exe = Ffmpeg.findNow();
		log("FFmpeg: " + exe);
		check(exe != null, "FFmpeg found");
		Ffmpeg.probe(exe);
		log("encoder H.264: " + Ffmpeg.h264() + ", H.265: " + Ffmpeg.hevc());

		try (TestSingleplayerContext sp = ctx.worldBuilder().create()) {
			sp.getClientWorld().waitForChunksRender();
			for (String cmd : new String[] {"gamemode creative @a", "time set 6000", "weather clear",
					"execute at @p run fill ~3 ~ ~5 ~7 ~4 ~9 minecraft:amethyst_block hollow",
					"execute at @p run fill ~-7 ~ ~5 ~-4 ~3 ~8 minecraft:oak_planks",
					"execute at @p run setblock ~5 ~5 ~7 minecraft:sea_lantern",
					"execute at @p run summon minecraft:pig ~2 ~ ~4"}) {
				sp.getServer().runCommand(cmd);
			}
			ctx.waitTicks(30);
			sp.getClientWorld().waitForChunksRender();
			ctx.takeScreenshot("ll-01-world");

			ctx.runOnClient(mc -> mc.setScreen(new LensScreen(null, null)));
			ctx.waitTicks(10);
			ctx.takeScreenshot("ll-02-settings");
			ctx.runOnClient(mc -> mc.setScreen(null));
			// The same at 1080p (the usual window), then back to 720p for the rest.
			ctx.getInput().resizeWindow(1920, 1080);
			ctx.waitTicks(5);
			ctx.runOnClient(mc -> mc.setScreen(new LensScreen(null, "Choose where your videos go, then press Record.")));
			ctx.waitTicks(10);
			ctx.takeScreenshot("ll-02b-settings-1080p");
			ctx.runOnClient(mc -> mc.setScreen(null));
			ctx.getInput().resizeWindow(1280, 720);
			ctx.waitTicks(5);

			// ---- Recording 1: 1K, as you see it.
			ctx.runOnClient(mc -> Recorder.start());
			ctx.waitTick();
			check(ctx.computeOnClient(mc -> Recorder.recording()), "recording started (" + Recorder.lastError + ")");
			log("recording " + Recorder.width() + "x" + Recorder.height() + " with " + Recorder.encoder());
			ctx.getInput().holdKeyFor(o -> o.keyUp, 30);
			sp.getServer().runCommand("execute at @p run playsound minecraft:entity.experience_orb.pickup master @a ~ ~ ~ 1 1");
			sp.getServer().runCommand("execute at @p run playsound minecraft:block.note_block.bell master @a ~3 ~ ~ 1 1");
			ctx.runOnClient(mc -> mc.player.setYRot(mc.player.getYRot() + 60));
			ctx.getInput().holdKeyFor(o -> o.keyLeft, 20);
			ctx.runOnClient(mc -> mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player)));
			ctx.waitTicks(20);
			ctx.takeScreenshot("ll-03-recording-menu");
			ctx.runOnClient(mc -> mc.setScreen(null));
			ctx.runOnClient(mc -> Photo.take());
			ctx.waitFor(mc -> !Photo.busy(), 20 * 60);
			ctx.getInput().holdKeyFor(o -> o.keyRight, 20);
			ctx.runOnClient(mc -> Recorder.stop(null));
			ctx.waitFor(mc -> Recorder.state() == Recorder.State.IDLE, 20 * 60 * 5);
			Path v1 = Recorder.lastFile;
			check(v1 != null && Files.isRegularFile(v1), "video 1 saved (" + Recorder.lastError + ")");
			probe(exe, v1, "video1");
			log("frames captured " + Recorder.captured + ", dropped " + Recorder.dropped);

			// ---- Photos, not recording: 4K with a 3 s wait (drawn bigger than the window), and window size.
			ctx.runOnClient(mc -> Lens.settings.photoWait = 3);
			ctx.runOnClient(mc -> Photo.take());
			ctx.waitTicks(10);
			check(ctx.computeOnClient(mc -> Photo.countdown()) > 0, "photo countdown running");
			ctx.waitFor(mc -> !Photo.busy(), 20 * 60);
			ctx.runOnClient(mc -> Lens.settings.photoWait = 0);
			ctx.runOnClient(mc -> Lens.settings.photoSize = Sizes.Size.SCREEN);
			ctx.runOnClient(mc -> Photo.take());
			ctx.waitFor(mc -> !Photo.busy(), 20 * 60);
			ctx.takeScreenshot("ll-04-after-photos");

			// ---- Recording 2: 1K with Sharper (the game draws at 2x the 720p window).
			ctx.runOnClient(mc -> Lens.settings.sharp = true);
			ctx.runOnClient(mc -> Recorder.start());
			ctx.waitTick();
			check(ctx.computeOnClient(mc -> Recorder.recording()), "sharp recording started");
			ctx.getInput().holdKeyFor(o -> o.keyUp, 30);
			ctx.takeScreenshot("ll-05-recording-sharp");
			ctx.runOnClient(mc -> Recorder.stop(null));
			ctx.waitFor(mc -> Recorder.state() == Recorder.State.IDLE, 20 * 60 * 5);
			Path v2 = Recorder.lastFile;
			check(v2 != null && !v2.equals(v1) && Files.isRegularFile(v2), "video 2 saved (" + Recorder.lastError + ")");
			probe(exe, v2, "video2");
			ctx.waitTicks(10);
			ctx.takeScreenshot("ll-06-back-to-normal");
			check(ctx.computeOnClient(mc -> mc.getWindow().getWidth()) == 1280, "the game draws at the window's size again");

			// ---- GIFs: 3 s at 480 wide, then a 3 s timelapse (30 s of play).
			for (boolean lapse : new boolean[] {false, true}) {
				ctx.runOnClient(mc -> {
					Lens.settings.gifSeconds = 3;
					Lens.settings.gifWidth = 480;
					Lens.settings.gifTimelapse = lapse;
					Recorder.startGif();
				});
				ctx.waitTick();
				check(ctx.computeOnClient(mc -> Recorder.recording() && Recorder.gif()), "GIF started" + (lapse ? " (timelapse)" : ""));
				ctx.runOnClient(mc -> mc.player.setYRot(mc.player.getYRot() + 30));
				ctx.getInput().holdKeyFor(o -> o.keyLeft, 20);
				ctx.waitFor(mc -> Recorder.state() == Recorder.State.IDLE, 20 * 60 * 3);
				Path g = Recorder.lastFile;
				check(g != null && g.toString().endsWith(".gif") && Files.isRegularFile(g), "GIF saved (" + Recorder.lastError + ")");
				gifInfo(g, lapse ? "gif-timelapse" : "gif");
			}
		}

		// Every photo: its size.
		try (Stream<Path> s = Files.walk(out)) {
			for (Path p : s.filter(f -> f.toString().endsWith(".png") || f.toString().endsWith(".jpg")).toList()) {
				BufferedImage img = ImageIO.read(p.toFile());
				log("photo " + out.relativize(p) + " " + img.getWidth() + "x" + img.getHeight());
			}
		} catch (Exception e) {
			throw new AssertionError("Couldn't read the photos: " + e);
		}
		ctx.waitTicks(5);
	}

	private static void probe(String exe, Path video, String tag) {
		try {
			Process p = new ProcessBuilder("ffprobe", "-v", "error", "-show_entries", "stream=codec_name,width,height,r_frame_rate,nb_frames,duration:format=duration,size",
					"-of", "compact", video.toString()).redirectErrorStream(true).start();
			String info = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
			p.waitFor();
			log(tag + " " + video.getFileName() + ": " + info.replace('\n', ' '));
			check(info.contains("codec_name=aac"), tag + " has a sound track");
			Process vd = new ProcessBuilder(exe, "-hide_banner", "-i", video.toString(), "-map", "0:a", "-af", "volumedetect", "-f", "null", "-").redirectErrorStream(true).start();
			String vol = new String(vd.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
			vd.waitFor();
			for (String line : vol.split("\n")) if (line.contains("max_volume") || line.contains("mean_volume")) log(tag + " sound " + line.substring(line.indexOf(']') + 1).trim());
			Path shots = FabricLoader.getInstance().getGameDir().resolve("screenshots");
			Files.createDirectories(shots);
			List<String> cmd = List.of(exe, "-v", "error", "-y", "-ss", "2", "-i", video.toString(), "-frames:v", "1", shots.resolve("ll-frame-" + tag + ".png").toString());
			new ProcessBuilder(cmd).redirectErrorStream(true).start().waitFor();
		} catch (Exception e) {
			throw new AssertionError("ffprobe failed: " + e);
		}
	}

	private static void gifInfo(Path gif, String tag) {
		try {
			Process p = new ProcessBuilder("ffprobe", "-v", "error", "-count_frames", "-show_entries", "stream=codec_name,width,height,nb_read_frames:format=duration,size",
					"-of", "compact", gif.toString()).redirectErrorStream(true).start();
			String info = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
			p.waitFor();
			log(tag + " " + gif.getFileName() + ": " + info.replace('\n', ' '));
			check(info.contains("codec_name=gif"), tag + " is a GIF");
		} catch (Exception e) {
			throw new AssertionError("ffprobe failed: " + e);
		}
	}

	private static void log(String s) {
		System.out.println("[lightlens-test] " + s);
	}

	private static void check(boolean ok, String what) {
		if (!ok) throw new AssertionError("Failed: " + what);
		log("ok: " + what);
	}
}
