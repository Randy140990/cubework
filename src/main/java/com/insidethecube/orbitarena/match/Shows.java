package com.insidethecube.orbitarena.match;

import java.util.List;

import com.insidethecube.orbitarena.net.Payloads;
import it.unimi.dsi.fastutil.ints.IntList;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;

/** What both games show the player: titles, sounds, the crowd's reactions, and fireworks for a win. */
final class Shows {
	private Shows() {}

	static void title(ServerPlayer p, Component title, Component sub, int in, int stay, int out) {
		p.connection.send(new ClientboundSetTitlesAnimationPacket(in, stay, out));
		p.connection.send(new ClientboundSetSubtitleTextPacket(sub));
		p.connection.send(new ClientboundSetTitleTextPacket(title));
	}

	static void sound(ServerPlayer p, SoundEvent sound, float volume, float pitch) {
		p.level().playSound(null, BlockPos.containing(p.position()), sound, SoundSource.PLAYERS, volume, pitch);
	}

	static void cheer(ServerPlayer p, int kind, float strength) {
		ServerPlayNetworking.send(p, new Payloads.Cheer(kind, strength));
	}

	private static final int[][] PALETTES = {
			{0xFFD84A, 0xFFFFFF, 0xFF9A2E}, {0x4AD8FF, 0xFFFFFF, 0x2E6BFF}, {0xFF4A6B, 0xFFC0D0, 0xFFFFFF}, {0x7CFF4A, 0xE0FFD0, 0x4AD8FF},
			{0xC04AFF, 0xFFFFFF, 0xFFD84A}};

	/** One firework from (x, y, z), bursting a second or two up, in one of the palettes (the team's colour first). */
	static void firework(ServerLevel level, double x, double y, double z, int teamColour, RandomSource rnd) {
		int[] pal = PALETTES[rnd.nextInt(PALETTES.length)];
		int first = teamColour >= 0 && rnd.nextFloat() < 0.5F ? teamColour : pal[0];
		FireworkExplosion.Shape[] shapes = FireworkExplosion.Shape.values();
		FireworkExplosion.Shape shape = rnd.nextFloat() < 0.5F ? FireworkExplosion.Shape.LARGE_BALL : shapes[rnd.nextInt(shapes.length)];
		FireworkExplosion burst = new FireworkExplosion(shape, IntList.of(first, pal[1]), IntList.of(pal[2]), rnd.nextBoolean(), rnd.nextBoolean());
		ItemStack rocket = new ItemStack(Items.FIREWORK_ROCKET);
		rocket.set(DataComponents.FIREWORKS, new Fireworks(1 + rnd.nextInt(2), List.of(burst)));
		level.addFreshEntity(new FireworkRocketEntity(level, x, y, z, rocket));
	}
}
