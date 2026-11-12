package com.insidethecube.deepkit.lamp;

import java.util.HashMap;
import java.util.Map;
import com.insidethecube.deepkit.DK;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Light round flares, made of invisible light blocks (BeamLightBlock). Each is lit
 * for a short while every time a lamp asks; once nothing asks it switches itself off. Only air and still
 * water are lit (never another block).
 */
public final class LightPools {
	private LightPools() {}

	/**
	 * Lights come and go without telling the blocks round them, so gravel or sand hanging over a cave never
	 * falls because a flare lit it.
	 */
	static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

	/** Ticks a light stays after its last request, and how often it checks. */
	static final int STAY = 6, CHECK = 4;

	private static final Map<ResourceKey<Level>, Long2LongOpenHashMap> UNTIL = new HashMap<>();

	private static Long2LongOpenHashMap until(ServerLevel level) {
		return UNTIL.computeIfAbsent(level.dimension(), k -> new Long2LongOpenHashMap());
	}

	/** Lights a block at a strength (1 to 15) for a moment. */
	public static void light(ServerLevel level, BlockPos pos, int strength) {
		if (strength <= 0 || !level.hasChunkAt(pos)) return;
		strength = Math.min(15, strength);
		BlockState s = level.getBlockState(pos);
		if (s.getBlock() == DK.BEAM_LIGHT) {
			if (s.getValue(BeamLightBlock.LEVEL) < strength) level.setBlock(pos, s.setValue(BeamLightBlock.LEVEL, strength), QUIET);
		} else {
			boolean water = s.is(Blocks.WATER) && s.getFluidState().isSource();
			if (!s.isAir() && !water) return;
			level.setBlock(pos, DK.BEAM_LIGHT.defaultBlockState().setValue(BeamLightBlock.LEVEL, strength).setValue(BeamLightBlock.WATERLOGGED, water),
					QUIET);
			level.scheduleTick(pos, DK.BEAM_LIGHT, CHECK);
		}
		until(level).put(pos.asLong(), level.getGameTime() + STAY);
	}

	/** Whether a light's time is up (it's then forgotten). */
	static boolean expired(ServerLevel level, BlockPos pos) {
		Long2LongOpenHashMap m = until(level);
		long u = m.getOrDefault(pos.asLong(), 0L);
		if (u >= level.getGameTime()) return false;
		m.remove(pos.asLong());
		return true;
	}
}
