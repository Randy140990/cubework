package com.lumenforge.lens.core;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;

/** Writes 16-bit PCM WAV as it goes (nothing held in memory), and fills in the sizes when closed. */
public final class WavWriter implements AutoCloseable {
	private final Path path;
	private final OutputStream out;
	private final int channels, rate;
	private long dataBytes;
	private final byte[] buf = new byte[8192];
	private int fill;

	public WavWriter(Path path, int channels, int rate) throws IOException {
		this.path = path;
		this.channels = channels;
		this.rate = rate;
		Files.createDirectories(path.toAbsolutePath().getParent());
		out = new BufferedOutputStream(Files.newOutputStream(path), 1 << 16);
		out.write(header(0));
	}

	private byte[] header(long data) {
		byte[] h = new byte[44];
		put(h, 0, "RIFF");
		le32(h, 4, (int) Math.min(0xFFFFFFFFL, 36 + data));
		put(h, 8, "WAVE");
		put(h, 12, "fmt ");
		le32(h, 16, 16);
		le16(h, 20, 1);
		le16(h, 22, channels);
		le32(h, 24, rate);
		le32(h, 28, rate * channels * 2);
		le16(h, 32, channels * 2);
		le16(h, 34, 16);
		put(h, 36, "data");
		le32(h, 40, (int) Math.min(0xFFFFFFFFL, data));
		return h;
	}

	/** Raw little-endian 16-bit samples, as a microphone gives them. */
	public void writeRaw(byte[] b, int off, int len) throws IOException {
		flushBuf();
		out.write(b, off, len);
		dataBytes += len;
	}

	/** Float samples (-1..1), interleaved for stereo. */
	public void write(float[] s, int off, int len) throws IOException {
		for (int i = off; i < off + len; i++) {
			float v = s[i];
			int x = (int) Math.round(Math.max(-1, Math.min(1, v)) * 32767);
			if (fill + 2 > buf.length) flushBuf();
			buf[fill++] = (byte) x;
			buf[fill++] = (byte) (x >> 8);
		}
	}

	private void flushBuf() throws IOException {
		if (fill > 0) {
			out.write(buf, 0, fill);
			dataBytes += fill;
			fill = 0;
		}
	}

	public double seconds() {
		return (dataBytes + fill) / (double) (rate * channels * 2);
	}

	@Override
	public void close() throws IOException {
		flushBuf();
		out.close();
		try (RandomAccessFile f = new RandomAccessFile(path.toFile(), "rw")) {
			f.seek(0);
			f.write(header(dataBytes));
		}
	}

	private static void put(byte[] h, int o, String s) {
		for (int i = 0; i < 4; i++) h[o + i] = (byte) s.charAt(i);
	}

	private static void le16(byte[] h, int o, int v) {
		h[o] = (byte) v;
		h[o + 1] = (byte) (v >> 8);
	}

	private static void le32(byte[] h, int o, int v) {
		for (int i = 0; i < 4; i++) h[o + i] = (byte) (v >> (8 * i));
	}
}
