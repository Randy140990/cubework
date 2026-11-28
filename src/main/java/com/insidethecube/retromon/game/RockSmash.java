package com.insidethecube.retromon.game;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.insidethecube.retromon.engine.Mon;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * (0.78, John's 28 Nov 14:54: the caves "and obviously understanding the mechanics behind them") ROCK SMASH, as
 * HeartGold has it: A at one of a cave's cracked rocks (the ICE PATH's on B3F, in front of the NEVERMELTICE) and "It's
 * a rugged rock, but a POKéMON may be able to smash it."; with one of yours that knows ROCK SMASH and the ZEPHYRBADGE
 * (HeartGold's; FireRed's first, the BOULDERBADGE, does too), "Would you like to use ROCK SMASH?" YES: it breaks
 * apart. It's back when you come into that floor again, as the games' are.
 */
public final class RockSmash {
	private RockSmash() {}

	/** Which rock you were asked about (its number in the room's list). */
	private static final Map<UUID, Integer> ASKED = new HashMap<>();

	private static BlockState rock() {
		return Blocks.WAXED_WEATHERED_COPPER_GRATE.defaultBlockState();
	}

	/** Whether a rock's blocks are still standing. */
	private static boolean standing(ServerLevel level, Interiors.Room r, int[] b) {
		for (int x = b[0]; x < b[3]; x++)
			for (int y = b[1]; y < b[4]; y++)
				for (int z = b[2]; z < b[5]; z++)
					if (level.getBlockState(new BlockPos(r.ox() + x, r.oy() + y, r.oz() + z)).is(Blocks.WAXED_WEATHERED_COPPER_GRATE)) return true;
		return false;
	}

	private static void set(ServerLevel level, Interiors.Room r, int[] b, BlockState s) {
		for (int x = b[0]; x < b[3]; x++)
			for (int y = b[1]; y < b[4]; y++)
				for (int z = b[2]; z < b[5]; z++) level.setBlock(new BlockPos(r.ox() + x, r.oy() + y, r.oz() + z), s, 2);
	}

	/** One of your team who can use ROCK SMASH (knows it, not fainted) with the ZEPHYRBADGE (or the BOULDERBADGE); or null. */
	private static Mon smasher(ServerPlayer p) {
		PlayerData d = Game.data(p);
		if (!d.badges.contains("ZEPHYRBADGE") && !d.badges.contains("BOULDERBADGE")) return null;
		for (Mon m : d.party) if (!m.fainted() && m.moves.contains("rock_smash")) return m;
		return null;
	}

	/** A at a rock (its act's use, "smash:N"). True if it was one. */
	public static boolean use(ServerPlayer p, String use) {
		if (!use.startsWith("smash:")) return false;
		Interiors.Room r = Interiors.roomOf(p);
		List<int[]> rocks = r == null ? null : Interiors.SMASH.get(r.id());
		int i;
		try {
			i = Integer.parseInt(use.substring(6));
		} catch (NumberFormatException e) {
			return true;
		}
		if (rocks == null || i < 0 || i >= rocks.size() || !(p.level() instanceof ServerLevel level) || !standing(level, r, rocks.get(i))) return true;
		Mon m = smasher(p);
		if (m == null) {
			Game.message(p, List.of("It's a rugged rock, but a POKéMON may be able to smash it."));
			return true;
		}
		ASKED.put(p.getUUID(), i);
		Game.dialogue(p, null, "rock_smash", List.of("This rock appears to be breakable.", "Would you like to use ROCK SMASH?"), Game.yesNo());
		return true;
	}

	/** The answer to "Would you like to use ROCK SMASH?": YES, it breaks apart. True if it was that question. */
	public static boolean answer(ServerPlayer p, String mode, CompoundTag t) {
		if (!mode.equals("rock_smash")) return false;
		boolean yes = t.getBooleanOr("answered", false) ? t.getIntOr("choice", 1) == 0 : t.getBooleanOr("yes", false);
		Integer i = ASKED.remove(p.getUUID());
		Interiors.Room r = Interiors.roomOf(p);
		List<int[]> rocks = r == null ? null : Interiors.SMASH.get(r.id());
		Mon m = smasher(p);
		if (!yes || i == null || rocks == null || i >= rocks.size() || m == null || !(p.level() instanceof ServerLevel level)) return true;
		int[] b = rocks.get(i);
		set(level, r, b, Blocks.AIR.defaultBlockState());
		double cx = r.ox() + (b[0] + b[3]) / 2.0, cy = r.oy() + b[1] + 0.8, cz = r.oz() + (b[2] + b[5]) / 2.0;
		level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK, Blocks.COBBLED_DEEPSLATE.defaultBlockState()),
				cx, cy, cz, 40, 0.6, 0.5, 0.6, 0.12);
		level.playSound(null, BlockPos.containing(cx, cy, cz), net.minecraft.sounds.SoundEvents.STONE_BREAK, net.minecraft.sounds.SoundSource.BLOCKS, 1F, 0.7F);
		Game.message(p, List.of(m.name().toUpperCase() + " used ROCK SMASH!"));
		return true;
	}

	/** Into a floor: its rocks stand again. */
	public static void entered(ServerPlayer p, Interiors.Room r) {
		List<int[]> rocks = Interiors.SMASH.get(r.id());
		if (rocks == null || !(p.level() instanceof ServerLevel level)) return;
		ASKED.remove(p.getUUID());
		for (int[] b : rocks) if (!standing(level, r, b)) set(level, r, b, rock());
	}
}
