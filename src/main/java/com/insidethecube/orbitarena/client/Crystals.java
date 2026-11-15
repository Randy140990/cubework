package com.insidethecube.orbitarena.client;

import static com.insidethecube.orbitarena.arena.BridgeLayout.*;

import java.util.ArrayList;
import java.util.List;

import com.insidethecube.orbitarena.OrbitArena;
import com.insidethecube.orbitarena.entity.Crystal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;

/**
 * The Bridge's crystals, one over each pillar (green over blue's, light red over red's), on your screen
 * only. Each goal against a team shatters one of its crystals (in a burst of glass and sparks) as its
 * pillar crumbles.
 */
public final class Crystals {
	private Crystals() {}

	private static final List<Crystal> CRYSTALS = new ArrayList<>();
	private static ClientLevel in;
	private static int cx = Integer.MIN_VALUE, cz;
	/** How many goals each team has had against it (so how many of its crystals are gone). */
	private static final int[] against = new int[2];
	private static int nextId = -3_000_000;

	public static void set(int centreX, int centreZ) {
		if (centreX != cx || centreZ != cz) {
			clear();
			against[0] = against[1] = 0;
		}
		cx = centreX;
		cz = centreZ;
	}

	/** The score: blue has had red's goals against it, and red blue's. */
	public static void score(int blue, int red) {
		// A new match (the score back down): all the crystals back.
		if (red < against[BLUE] || blue < against[RED]) clear();
		against[BLUE] = red;
		against[RED] = blue;
		for (Crystal c : new ArrayList<>(CRYSTALS)) if (c.pillar < against[c.team]) remove(c, true);
	}

	public static void shatter(int team, int pillar) {
		if (team < 0 || team > 1) return;
		against[team] = Math.max(against[team], pillar + 1);
		for (Crystal c : new ArrayList<>(CRYSTALS)) if (c.team == team && c.pillar == pillar) remove(c, true);
	}

	public static void clear() {
		for (Crystal c : CRYSTALS) if (in != null) in.removeEntity(c.getId(), Entity.RemovalReason.DISCARDED);
		CRYSTALS.clear();
		in = null;
	}

	private static void remove(Crystal c, boolean effect) {
		CRYSTALS.remove(c);
		ClientLevel level = in;
		if (level == null) return;
		if (effect) {
			double x = c.getX(), y = c.getY() + 1.8, z = c.getZ();
			BlockParticleOption glass = new BlockParticleOption(ParticleTypes.BLOCK, (c.team == BLUE ? Blocks.LIME_STAINED_GLASS : Blocks.PINK_STAINED_GLASS).defaultBlockState());
			for (int i = 0; i < 70; i++) {
				double vx = (Math.random() - 0.5) * 0.9, vy = Math.random() * 0.6, vz = (Math.random() - 0.5) * 0.9;
				level.addParticle(glass, x + vx, y + vy * 2, z + vz, vx, vy, vz);
				if (i % 3 == 0) level.addParticle(ParticleTypes.END_ROD, x, y, z, vx * 0.6, vy * 0.6, vz * 0.6);
				if (i % 5 == 0) level.addParticle(ParticleTypes.ELECTRIC_SPARK, x + vx * 2, y + vy * 2, z + vz * 2, 0, 0, 0);
			}
			level.addParticle(ParticleTypes.EXPLOSION, x, y, z, 0, 0, 0);
			level.playLocalSound(x, y, z, SoundEvents.GLASS_BREAK, SoundSource.AMBIENT, 2.0F, 0.6F, false);
			level.playLocalSound(x, y, z, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.AMBIENT, 2.0F, 0.5F, false);
		}
		level.removeEntity(c.getId(), Entity.RemovalReason.DISCARDED);
	}

	public static void tick(Minecraft mc) {
		ClientLevel level = mc.level;
		boolean onBridge = level != null && mc.player != null && level.dimension() == OrbitArena.ARENA && Crowd.onBridge() && cx != Integer.MIN_VALUE;
		if (!onBridge) {
			if (!CRYSTALS.isEmpty() || in != null) clear();
			return;
		}
		if (in == level && !CRYSTALS.isEmpty()) return;
		if (in == level && against[0] >= PILLAR_Z.length && against[1] >= PILLAR_Z.length) return;
		// Wait until the pillars are there (the chunks have arrived).
		if (level.getBlockState(new BlockPos(cx - PILLAR_X - 1, Y + PILLAR_TOP - 2, cz)).isAir()
				&& level.getBlockState(new BlockPos(cx + PILLAR_X + 1, Y + PILLAR_TOP - 2, cz)).isAir()) {
			return;
		}
		clear();
		in = level;
		for (int team = BLUE; team <= RED; team++) {
			for (int i = against[team]; i < PILLAR_Z.length; i++) {
				Crystal c = new Crystal(OrbitArena.CRYSTAL, level);
				c.setId(nextId--);
				c.team = team;
				c.pillar = i;
				c.phase = (float) Math.random();
				c.setPos(cx + side(team) * (PILLAR_X + 1) + 0.5, Y + CRYSTAL_Y - 1.8, cz + PILLAR_Z[i] + 0.5);
				c.setOldPosAndRot();
				level.addEntity(c);
				CRYSTALS.add(c);
			}
		}
	}
}
