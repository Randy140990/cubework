package com.lumenforge.lens.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Enumeration;
import java.util.function.BiConsumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Downloads with progress, and unzips. Used for FFmpeg. */
public final class Download {
	private Download() {}

	/** Reports (bytesDone, bytesTotal); total is -1 when unknown. */
	public static void file(String url, Path to, BiConsumer<Long, Long> progress) throws IOException, InterruptedException {
		HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).connectTimeout(Duration.ofSeconds(20)).build();
		HttpRequest req = HttpRequest.newBuilder(URI.create(url)).header("User-Agent", "Lightlens").GET().build();
		HttpResponse<InputStream> res = http.send(req, HttpResponse.BodyHandlers.ofInputStream());
		if (res.statusCode() / 100 != 2) throw new IOException("HTTP " + res.statusCode() + " for " + url);
		long total = res.headers().firstValueAsLong("Content-Length").orElse(-1);
		Files.createDirectories(to.getParent());
		Path tmp = to.resolveSibling(to.getFileName() + ".part");
		try (InputStream in = res.body(); OutputStream out = Files.newOutputStream(tmp)) {
			byte[] buf = new byte[1 << 16];
			long done = 0, lastReport = 0;
			int n;
			while ((n = in.read(buf)) > 0) {
				out.write(buf, 0, n);
				done += n;
				if (progress != null && done - lastReport > (1 << 20)) {
					progress.accept(done, total);
					lastReport = done;
				}
			}
			if (progress != null) progress.accept(done, total);
		}
		Files.move(tmp, to, StandardCopyOption.REPLACE_EXISTING);
	}

	public static void unzip(Path zip, Path dir) throws IOException {
		Files.createDirectories(dir);
		try (ZipFile zf = new ZipFile(zip.toFile())) {
			Enumeration<? extends ZipEntry> en = zf.entries();
			while (en.hasMoreElements()) {
				ZipEntry e = en.nextElement();
				Path out = dir.resolve(e.getName()).normalize();
				if (!out.startsWith(dir)) continue; // no escaping the folder
				if (e.isDirectory()) {
					Files.createDirectories(out);
				} else {
					Files.createDirectories(out.getParent());
					try (InputStream in = zf.getInputStream(e)) {
						Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
					}
				}
			}
		}
	}

	/** Finds the first file with one of these names anywhere under dir. */
	public static Path findFile(Path dir, String... names) {
		if (!Files.isDirectory(dir)) return null;
		try (var s = Files.walk(dir, 6)) {
			return s.filter(Files::isRegularFile).filter(p -> {
				String n = p.getFileName().toString();
				for (String want : names) if (n.equalsIgnoreCase(want)) return true;
				return false;
			}).findFirst().orElse(null);
		} catch (IOException e) {
			return null;
		}
	}

	public static boolean windows() {
		return System.getProperty("os.name", "").toLowerCase().contains("win");
	}

	public static boolean linux() {
		return System.getProperty("os.name", "").toLowerCase().contains("linux");
	}

	public static boolean mac() {
		return System.getProperty("os.name", "").toLowerCase().contains("mac");
	}
}
