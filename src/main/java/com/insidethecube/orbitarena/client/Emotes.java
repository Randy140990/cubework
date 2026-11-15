package com.insidethecube.orbitarena.client;

import java.util.HashMap;
import java.util.Map;

import com.insidethecube.orbitarena.OrbitArena;
import com.insidethecube.orbitarena.net.Payloads;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;

/**
 * Celebrations, like a footballer's after a goal: both arms up and bouncing, the jump-and-land "Siuu", the
 * aeroplane, a fist pump and a little dance. Press V in the arena (or on the bridge) to celebrate; after a
 * win you celebrate on your own for ten seconds, seen from the front so you can watch. Everyone near sees
 * it (see mixin.PlayerModelMixin).
 */
public final class Emotes {
	private Emotes() {}

	public static final int CHEER = 0, SIUU = 1, AIRPLANE = 2, FIST_PUMP = 3, DANCE = 4, COUNT = 5;
	private static final int[] LENGTH = {60, 46, 70, 48, 80};

	private record Playing(int emote, long start) {}

	private static final Map<Integer, Playing> PLAYING = new HashMap<>();
	public static KeyMapping KEY;
	private static int next, celebrating;
	private static CameraType before;

	public static void register() {
		KEY = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.orbitarena.celebrate", InputConstants.Type.KEYSYM, 86,
				KeyMapping.Category.register(OrbitArena.id("orbitarena"))));
	}

	/** Someone (by entity id) celebrates. */
	public static void play(int entityId, int emote) {
		PLAYING.put(entityId, new Playing(Math.floorMod(emote, COUNT), System.nanoTime()));
	}

	/** You won: ten seconds of celebrating, seen from the front. */
	public static void celebrate(int ticks) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return;
		if (celebrating <= 0) before = mc.options.getCameraType();
		mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
		celebrating = ticks;
		mine(CHEER);
	}

	private static void mine(int emote) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return;
		play(mc.player.getId(), emote);
		ClientPlayNetworking.send(new Payloads.Emote(emote));
	}

	private static boolean busy(int entityId) {
		Playing p = PLAYING.get(entityId);
		return p != null && (System.nanoTime() - p.start) / 50_000_000L < LENGTH[p.emote];
	}

	public static void tick(Minecraft mc) {
		boolean inArena = mc.level != null && mc.player != null && mc.level.dimension() == OrbitArena.ARENA;
		while (KEY.consumeClick()) {
			if (inArena) mine(next++ % COUNT);
		}
		if (celebrating > 0) {
			// One celebration after another until it's time to choose.
			if (mc.player != null && !busy(mc.player.getId())) mine(1 + next++ % (COUNT - 1));
			if (--celebrating == 0 || !inArena) {
				celebrating = 0;
				if (mc.options.getCameraType() == CameraType.THIRD_PERSON_FRONT && before != null) mc.options.setCameraType(before);
				before = null;
			}
		}
		PLAYING.values().removeIf(p -> (System.nanoTime() - p.start) / 50_000_000L > LENGTH[p.emote]);
	}

	/** Poses a player's model for their celebration, if they're celebrating. */
	public static void pose(PlayerModel m, AvatarRenderState s) {
		Playing p = PLAYING.get(s.id);
		if (p == null) return;
		float t = (System.nanoTime() - p.start) / 50_000_000F, len = LENGTH[p.emote];
		if (t > len) return;
		// Eased in and out over a few ticks.
		float k = Math.min(1, Math.min(t / 4, (len - t) / 4));
		float lift = 0;
		ModelPart ra = m.rightArm, la = m.leftArm, rl = m.rightLeg, ll = m.leftLeg;
		switch (p.emote) {
			case CHEER -> {
				set(ra, -2.85F + Mth.sin(t * 0.55F) * 0.15F, 0, 0.3F, k);
				set(la, -2.85F - Mth.sin(t * 0.55F) * 0.15F, 0, -0.3F, k);
				m.head.xRot = Mth.lerp(k, m.head.xRot, -0.35F);
				lift = Math.abs(Mth.sin(t * 0.32F)) * 3.0F;
			}
			case SIUU -> {
				if (t < 12) {
					// The run-up: arms back.
					set(ra, 0.9F, 0, 0.2F, k);
					set(la, 0.9F, 0, -0.2F, k);
					set(rl, -0.4F, 0, 0, k);
					set(ll, 0.4F, 0, 0, k);
				} else if (t < 26) {
					// Up, turning, arms flung wide.
					float u = (t - 12) / 14;
					lift = Mth.sin(u * Mth.PI) * 6;
					set(ra, -0.2F, 0, 1.5F, k);
					set(la, -0.2F, 0, -1.5F, k);
					m.body.yRot = Mth.lerp(k, m.body.yRot, u * 0.8F);
				} else {
					// Landed, feet apart, arms thrown down and out, head back.
					set(ra, 0.1F, 0, 0.75F, k);
					set(la, 0.1F, 0, -0.75F, k);
					set(rl, 0, 0, 0.3F, k);
					set(ll, 0, 0, -0.3F, k);
					m.head.xRot = Mth.lerp(k, m.head.xRot, -0.55F);
					m.body.xRot = Mth.lerp(k, m.body.xRot, -0.1F);
				}
			}
			case AIRPLANE -> {
				float roll = Mth.sin(t * 0.13F) * 0.35F;
				set(ra, 0, 0, 1.55F + roll, k);
				set(la, 0, 0, -1.55F + roll, k);
				m.body.zRot = Mth.lerp(k, m.body.zRot, roll * 0.4F);
				set(rl, Mth.sin(t * 0.7F) * 0.7F, 0, 0, k);
				set(ll, -Mth.sin(t * 0.7F) * 0.7F, 0, 0, k);
				lift = Math.abs(Mth.sin(t * 0.7F)) * 0.8F;
			}
			case FIST_PUMP -> {
				set(ra, -2.55F + Mth.sin(t * 0.7F) * 0.55F, 0, -0.15F, k);
				set(la, 0.15F, 0, -0.65F, k);
				m.head.xRot = Mth.lerp(k, m.head.xRot, Mth.sin(t * 0.7F) * 0.2F - 0.15F);
				lift = Math.max(0, Mth.sin(t * 0.7F)) * 1.2F;
			}
			case DANCE -> {
				float a = Mth.sin(t * 0.45F);
				set(ra, -1.6F - a * 1.1F, 0, 0.35F, k);
				set(la, -1.6F + a * 1.1F, 0, -0.35F, k);
				m.body.zRot = Mth.lerp(k, m.body.zRot, a * 0.12F);
				set(rl, Math.max(0, a) * -0.5F, 0, 0.08F, k);
				set(ll, Math.max(0, -a) * -0.5F, 0, -0.08F, k);
				lift = Math.abs(a) * 1.0F;
			}
			default -> {
			}
		}
		if (lift > 0) {
			float dy = -lift * k;
			for (ModelPart part : new ModelPart[] {m.head, m.body, ra, la, rl, ll}) part.y += dy;
		}
	}

	private static void set(ModelPart part, float x, float y, float z, float k) {
		part.xRot = Mth.lerp(k, part.xRot, x);
		part.yRot = Mth.lerp(k, part.yRot, y);
		part.zRot = Mth.lerp(k, part.zRot, z);
	}
}
