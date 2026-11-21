package com.lumenforge.lens.capture;

import com.lumenforge.lens.Lens;
import com.lumenforge.lens.core.Ffmpeg;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * A recording the game didn't get to finish (it was closed or crashed while recording): its picture is still in
 * the working folder, readable, so on the next start it's saved to your folder as "(recovered)", with the mic if
 * there was one. The game's sound can't be brought back.
 */
public final class Rescue {
	private Rescue() {}

	public static void runAsync() {
		Thread t = new Thread(Rescue::run, "Lightlens recovery");
		t.setDaemon(true);
		t.start();
	}

	private static void run() {
		Path work = Lens.work();
		if (!Files.isDirectory(work)) return;
		List<Path> takes = new ArrayList<>();
		try (Stream<Path> s = Files.list(work)) {
			s.filter(Files::isDirectory).forEach(takes::add);
		} catch (Exception e) {
			return;
		}
		for (Path take : takes) {
			if (Recorder.busy()) return;
			Path video = take.resolve("video.mkv");
			try {
				if (!Files.isRegularFile(video) || Files.size(video) < 10_000) {
					Capture.deleteTree(take);
					continue;
				}
				Path out = Lens.output();
				Files.createDirectories(out);
				String name = take.getFileName().toString().replace('_', ' ') + " (recovered)";
				String exe = Ffmpeg.findNow();
				Path mic = take.resolve("mic.wav");
				Path saved;
				if (exe != null) {
					saved = Capture.unique(out, name, ".mp4");
					Path part = saved.resolveSibling(saved.getFileName() + ".part");
					Ffmpeg.run(Ffmpeg.rescueCommand(exe, video, Files.isRegularFile(mic) && Files.size(mic) > 1000 ? mic : null, part), 3600);
					Files.move(part, saved, StandardCopyOption.REPLACE_EXISTING);
				} else {
					saved = Capture.unique(out, name, ".mkv");
					Files.move(video, saved);
				}
				Capture.deleteTree(take);
				Lens.LOG.info("Recovered an unfinished recording: {}", saved);
				Notify.toast("Recovered a recording", saved.getFileName() + " (the game closed while it was recording)");
			} catch (Exception e) {
				Lens.LOG.warn("Couldn't recover {}: {}", take, e.toString());
			}
		}
	}
}
