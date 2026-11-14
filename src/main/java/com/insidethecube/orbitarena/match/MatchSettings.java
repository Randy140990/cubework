package com.insidethecube.orbitarena.match;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;

/**
 * What a match is: who you fight, how well they fight (1 to 10), how many of them, your kit, and the time
 * limit in seconds (0: none).
 */
public record MatchSettings(Opponents opponents, int difficulty, int count, Kit kit, Boss boss, int timeLimit) {
	public enum Opponents {
		/** Armoured fighters that fight like players. */
		FIGHTERS,
		/** Waves of hostile mobs. */
		MOBS,
		/** A boss (or a few). */
		BOSS
	}

	public enum Boss {
		WITHER, WARDEN, ENDER_DRAGON
	}

	public static final StreamCodec<ByteBuf, MatchSettings> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, s -> s.opponents.ordinal(),
			ByteBufCodecs.VAR_INT, MatchSettings::difficulty,
			ByteBufCodecs.VAR_INT, MatchSettings::count,
			ByteBufCodecs.VAR_INT, s -> s.kit.ordinal(),
			ByteBufCodecs.VAR_INT, s -> s.boss.ordinal(),
			ByteBufCodecs.VAR_INT, MatchSettings::timeLimit,
			(o, d, c, k, b, t) -> new MatchSettings(Opponents.values()[Mth.clamp(o, 0, Opponents.values().length - 1)], d, c,
					Kit.values()[Mth.clamp(k, 0, Kit.values().length - 1)], Boss.values()[Mth.clamp(b, 0, Boss.values().length - 1)], t));

	/** Within the limits (a modified client can send anything). */
	public MatchSettings checked() {
		int max = opponents == Opponents.BOSS ? 3 : 100;
		return new MatchSettings(opponents, Mth.clamp(difficulty, 1, 10), Mth.clamp(count, 1, max), kit, boss, Mth.clamp(timeLimit, 0, 3600));
	}
}
