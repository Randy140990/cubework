package com.lumenforge.lens.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Writes raw RGBA frames into a Matroska stream, each with the time it was captured. FFmpeg reads this from its
 * input pipe and makes a steady frame rate from the times, so a frame the game was too slow to give is filled by the
 * one before, and picture and sound stay together however the game's frame rate moves.
 *
 * Only what FFmpeg needs is written: an unknown-size segment, one uncompressed video track (V_UNCOMPRESSED with the
 * 'RGBA' colour space), and unknown-size clusters of SimpleBlocks with millisecond times.
 */
public final class MkvWriter {
	private static final long UNKNOWN = 0x01FFFFFFFFFFFFFFL;
	private static final int CLUSTER_MS = 10_000;

	private final OutputStream out;
	private final int width, height;
	private long clusterStart = -1;
	private long lastTime = -1;

	public MkvWriter(OutputStream out, int width, int height) {
		this.out = out;
		this.width = width;
		this.height = height;
	}

	public int frameBytes() {
		return width * height * 4;
	}

	/** The file's header and the video track. */
	public void writeHeader(String app) throws IOException {
		ByteArrayOutputStream ebml = new ByteArrayOutputStream();
		uint(ebml, 0x4286, 1);
		uint(ebml, 0x42F7, 1);
		uint(ebml, 0x42F2, 4);
		uint(ebml, 0x42F3, 8);
		string(ebml, 0x4282, "matroska");
		uint(ebml, 0x4287, 4);
		uint(ebml, 0x4285, 2);
		element(out, 0x1A45DFA3, ebml.toByteArray());

		id(out, 0x18538067);
		out.write(vint8(UNKNOWN));

		ByteArrayOutputStream info = new ByteArrayOutputStream();
		uint(info, 0x2AD7B1, 1_000_000);
		string(info, 0x4D80, app);
		string(info, 0x5741, app);
		element(out, 0x1549A966, info.toByteArray());

		ByteArrayOutputStream video = new ByteArrayOutputStream();
		uint(video, 0xB0, width);
		uint(video, 0xBA, height);
		element(video, 0x2EB524, "RGBA".getBytes(StandardCharsets.US_ASCII));

		ByteArrayOutputStream track = new ByteArrayOutputStream();
		uint(track, 0xD7, 1);
		uint(track, 0x73C5, 1);
		uint(track, 0x83, 1);
		uint(track, 0x9C, 0);
		string(track, 0x86, "V_UNCOMPRESSED");
		element(track, 0xE0, video.toByteArray());

		ByteArrayOutputStream tracks = new ByteArrayOutputStream();
		element(tracks, 0xAE, track.toByteArray());
		element(out, 0x1654AE6B, tracks.toByteArray());
	}

	/** One frame (width x height x 4 bytes, top row first) at a time in milliseconds from the start. */
	public void writeFrame(long timeMs, byte[] rgba, int offset) throws IOException {
		if (timeMs <= lastTime) timeMs = lastTime + 1;
		lastTime = timeMs;
		if (clusterStart < 0 || timeMs - clusterStart >= CLUSTER_MS) {
			clusterStart = timeMs;
			id(out, 0x1F43B675);
			out.write(vint8(UNKNOWN));
			uint(out, 0xE7, timeMs);
		}
		int rel = (int) (timeMs - clusterStart);
		int n = frameBytes();
		id(out, 0xA3);
		out.write(vint8(n + 4));
		out.write(0x81);
		out.write((rel >> 8) & 0xFF);
		out.write(rel & 0xFF);
		out.write(0x80);
		out.write(rgba, offset, n);
	}

	// ---------------------------------------------------------------- EBML

	private static void id(OutputStream o, int id) throws IOException {
		if ((id & 0xFF000000) != 0) o.write(id >>> 24);
		if ((id & 0xFFFF0000) != 0) o.write((id >>> 16) & 0xFF);
		if ((id & 0xFFFFFF00) != 0) o.write((id >>> 8) & 0xFF);
		o.write(id & 0xFF);
	}

	/** A size as an 8-byte EBML number (values up to 2^56 - 2, or UNKNOWN). */
	static byte[] vint8(long v) {
		byte[] b = new byte[8];
		b[0] = 0x01;
		for (int i = 7; i >= 1; i--) {
			b[i] = (byte) (v & 0xFF);
			v >>>= 8;
		}
		return b;
	}

	private static byte[] vintShort(int len) {
		if (len < 0x7F) return new byte[] {(byte) (0x80 | len)};
		if (len < 0x3FFF) return new byte[] {(byte) (0x40 | (len >> 8)), (byte) len};
		return vint8(len);
	}

	private static void element(OutputStream o, int id, byte[] data) throws IOException {
		id(o, id);
		o.write(vintShort(data.length));
		o.write(data);
	}

	private static void uint(OutputStream o, int id, long v) throws IOException {
		int n = 1;
		while (n < 8 && (v >>> (n * 8)) != 0) n++;
		byte[] b = new byte[n];
		for (int i = n - 1; i >= 0; i--) {
			b[i] = (byte) (v & 0xFF);
			v >>>= 8;
		}
		element(o, id, b);
	}

	private static void string(OutputStream o, int id, String s) throws IOException {
		element(o, id, s.getBytes(StandardCharsets.UTF_8));
	}
}
