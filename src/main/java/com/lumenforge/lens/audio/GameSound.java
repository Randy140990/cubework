package com.lumenforge.lens.audio;

import com.lumenforge.lens.Lens;
import com.lumenforge.lens.capture.Recorder;
import com.lumenforge.lens.core.SoundMix;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.libc.LibCStdlib;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The game's sound while you record. Each sound that starts is noted, and twenty times a second (and when it
 * starts) we note how loud it is with your volume settings, how far it is and which side it's on, as you hear it.
 * When you stop, SoundMix turns that into a WAV.
 */
public final class GameSound {
	private static final int MAX_LIVE = 256;
	private static volatile boolean on;
	private static SoundEngine engine;
	private static final Map<SoundInstance, SoundMix.Voice> LIVE = new IdentityHashMap<>();
	private static final Map<SoundMix.Voice, SoundInstance> INSTANCE = new IdentityHashMap<>();
	private static final List<SoundMix.Voice> ALL = new ArrayList<>();

	private GameSound() {}

	public static void begin() {
		synchronized (ALL) {
			LIVE.clear();
			INSTANCE.clear();
			ALL.clear();
		}
		on = true;
	}

	/** Stops noting, and hands over everything heard (ending what's still playing at `end` seconds). */
	public static List<SoundMix.Voice> end(double endSeconds) {
		if (!on) return List.of();
		on = false;
		synchronized (ALL) {
			for (SoundMix.Voice v : LIVE.values()) if (v.end > endSeconds) v.end = endSeconds;
			LIVE.clear();
			INSTANCE.clear();
			List<SoundMix.Voice> out = new ArrayList<>(ALL);
			ALL.clear();
			return out;
		}
	}

	/** From the sound engine, when a sound has started. */
	public static void onPlay(SoundEngine e, SoundInstance s, SoundEngine.PlayResult result) {
		if (!on || result == SoundEngine.PlayResult.NOT_STARTED) return;
		engine = e;
		Sound sound = s.getSound();
		if (sound == null) return;
		double t = Recorder.clockSeconds();
		SoundMix.Voice v = new SoundMix.Voice(sound.getPath().toString(), s.isLooping() && s.getDelay() <= 0, t);
		synchronized (ALL) {
			SoundMix.Voice old = LIVE.put(s, v);
			if (old != null) {
				old.end = Math.min(old.end, t);
				INSTANCE.remove(old);
			}
			INSTANCE.put(v, s);
			ALL.add(v);
			if (LIVE.size() > MAX_LIVE) {
				Iterator<Map.Entry<SoundInstance, SoundMix.Voice>> it = LIVE.entrySet().iterator();
				Map.Entry<SoundInstance, SoundMix.Voice> first = it.next();
				first.getValue().end = Math.min(first.getValue().end, t);
				INSTANCE.remove(first.getValue());
				it.remove();
			}
		}
		key(v, s, t, sound);
	}

	/** From the sound engine, when a sound is stopped. */
	public static void onStop(SoundInstance s) {
		if (!on) return;
		synchronized (ALL) {
			SoundMix.Voice v = LIVE.remove(s);
			if (v != null) {
				v.end = Math.min(v.end, Recorder.clockSeconds());
				INSTANCE.remove(v);
			}
		}
	}

	public static void onStopAll() {
		if (!on) return;
		double t = Recorder.clockSeconds();
		synchronized (ALL) {
			for (SoundMix.Voice v : LIVE.values()) v.end = Math.min(v.end, t);
			LIVE.clear();
			INSTANCE.clear();
		}
	}

	/** Twenty times a second: how every playing sound sounds now. */
	public static void tick() {
		if (!on || Recorder.paused()) return;
		double t = Recorder.clockSeconds();
		synchronized (ALL) {
			Iterator<Map.Entry<SoundInstance, SoundMix.Voice>> it = LIVE.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<SoundInstance, SoundMix.Voice> e = it.next();
				SoundInstance s = e.getKey();
				SoundMix.Voice v = e.getValue();
				if (engine != null && !engine.isActive(s)) {
					v.end = Math.min(v.end, t);
					INSTANCE.remove(v);
					it.remove();
					continue;
				}
				Sound sound = s.getSound();
				if (sound != null) key(v, s, t, sound);
			}
		}
	}

	private static void key(SoundMix.Voice v, SoundInstance s, double t, Sound sound) {
		Minecraft mc = Minecraft.getInstance();
		SoundSource src = s.getSource();
		float vol = clamp(s.getVolume()) * clamp(mc.options.getFinalSoundSourceVolume(src));
		// Paused (single player's menu), only music and the menu's own sounds carry on.
		boolean playing = !(mc.isPaused() && src != SoundSource.MUSIC && src != SoundSource.UI);
		float att = 1, pan = 0;
		var lt = mc.getSoundManager().getListenerTransform();
		Vec3 ear = lt.position();
		Vec3 right = lt.forward().cross(lt.up());
		Vec3 at = new Vec3(s.getX(), s.getY(), s.getZ());
		Vec3 d = s.isRelative() ? at : at.subtract(ear);
		double dist = d.length();
		if (s.getAttenuation() == SoundInstance.Attenuation.LINEAR) {
			double range = Math.max(s.getVolume(), 1.0F) * sound.getAttenuationDistance();
			att = SoundMix.attenuation(dist, range);
		}
		if (dist > 0.05 && right.lengthSqr() > 1e-6) pan = (float) (d.dot(right.normalize()) / dist);
		float pitch = Math.max(0.5F, Math.min(2.0F, s.getPitch()));
		v.key(t, vol, att, pan, pitch, playing);
	}

	private static float clamp(float f) {
		return Math.max(0, Math.min(1, f));
	}

	// ---------------------------------------------------------------- the sound files

	private static final long MAX_CACHED = 48_000_000L;
	private static final LinkedHashMap<String, SoundMix.Clip> CLIPS = new LinkedHashMap<>(64, 0.75F, true);
	private static long cached;

	/** A sound file's samples, decoded once and kept while there's room (for the mixer, on its thread). */
	public static synchronized SoundMix.Clip clip(String file) {
		SoundMix.Clip c = CLIPS.get(file);
		if (c != null || CLIPS.containsKey(file)) return c;
		c = decode(file);
		CLIPS.put(file, c);
		if (c != null) cached += c.data().length;
		Iterator<Map.Entry<String, SoundMix.Clip>> it = CLIPS.entrySet().iterator();
		while (cached > MAX_CACHED && it.hasNext()) {
			Map.Entry<String, SoundMix.Clip> e = it.next();
			if (e.getKey().equals(file)) continue;
			if (e.getValue() != null) cached -= e.getValue().data().length;
			it.remove();
		}
		return c;
	}

	public static synchronized void forgetClips() {
		CLIPS.clear();
		cached = 0;
	}

	private static SoundMix.Clip decode(String file) {
		try {
			Identifier id = Identifier.parse(file);
			Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(id);
			if (res.isEmpty()) return null;
			byte[] bytes;
			try (InputStream in = res.get().open()) {
				bytes = in.readAllBytes();
			}
			ByteBuffer data = MemoryUtil.memAlloc(bytes.length);
			data.put(bytes).flip();
			try (MemoryStack stack = MemoryStack.stackPush()) {
				IntBuffer ch = stack.mallocInt(1), rate = stack.mallocInt(1);
				ShortBuffer pcm = STBVorbis.stb_vorbis_decode_memory(data, ch, rate);
				if (pcm == null) return null;
				try {
					int channels = Math.max(1, Math.min(2, ch.get(0)));
					int srcCh = ch.get(0);
					int frames = pcm.remaining() / srcCh;
					short[] out = new short[frames * channels];
					for (int i = 0; i < frames; i++) for (int c = 0; c < channels; c++) out[i * channels + c] = pcm.get(i * srcCh + c);
					return new SoundMix.Clip(out, channels, rate.get(0));
				} finally {
					LibCStdlib.free(pcm);
				}
			} finally {
				MemoryUtil.memFree(data);
			}
		} catch (Throwable e) {
			Lens.LOG.debug("Couldn't decode {}", file, e);
			return null;
		}
	}
}
