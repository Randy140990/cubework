package com.insidethecube.orbitarena.match;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;

/**
 * What a match is: who you fight, how well they fight (1 to 10), how many (fighters or bosses; waves of
 * mobs; players a team on the bridge), your kit, the time limit in seconds (0: none), and for the bridge
 * its kit (0 classic, 1 netherite), whether there are obstacles, how wide the bridge is (1 to 3) and which team
 * you're on (0 blue, 1 red), and where the crowd sits (0 in stands along the bridge, 1 in a ring round it all).
 * Intro: the fly-round before it starts.
 */
public record MatchSettings(Opponents opponents, int difficulty, int count, Kit kit, Boss boss, int timeLimit, boolean obstacles, boolean intro,
		int bridgeKit, int bridgeWidth, int bridgeTeam, int bridgeStands) {
	public enum Opponents {
		/** Armoured fighters that fight like players. */
		FIGHTERS,
		/** Waves of hostile mobs. */
		MOBS,
		/** A boss (or a few). */
		BOSS,
		/** The Bridge: two teams, two goals, first to five. */
		BRIDGE
	}

	public enum Boss {
		WITHER, WARDEN, ENDER_DRAGON
	}

	public static final int CLASSIC = 0, NETHERITE = 1, SIDES = 0, RING = 1;

	public static final StreamCodec<ByteBuf, MatchSettings> STREAM_CODEC = StreamCodec.of((buf, s) -> {
		ByteBufCodecs.VAR_INT.encode(buf, s.opponents.ordinal());
		ByteBufCodecs.VAR_INT.encode(buf, s.difficulty);
		ByteBufCodecs.VAR_INT.encode(buf, s.count);
		ByteBufCodecs.VAR_INT.encode(buf, s.kit.ordinal());
		ByteBufCodecs.VAR_INT.encode(buf, s.boss.ordinal());
		ByteBufCodecs.VAR_INT.encode(buf, s.timeLimit);
		ByteBufCodecs.BOOL.encode(buf, s.obstacles);
		ByteBufCodecs.BOOL.encode(buf, s.intro);
		ByteBufCodecs.VAR_INT.encode(buf, s.bridgeKit);
		ByteBufCodecs.VAR_INT.encode(buf, s.bridgeWidth);
		ByteBufCodecs.VAR_INT.encode(buf, s.bridgeTeam);
		ByteBufCodecs.VAR_INT.encode(buf, s.bridgeStands);
	}, buf -> {
		int o = ByteBufCodecs.VAR_INT.decode(buf), d = ByteBufCodecs.VAR_INT.decode(buf), c = ByteBufCodecs.VAR_INT.decode(buf);
		int k = ByteBufCodecs.VAR_INT.decode(buf), b = ByteBufCodecs.VAR_INT.decode(buf), t = ByteBufCodecs.VAR_INT.decode(buf);
		boolean obstacles = ByteBufCodecs.BOOL.decode(buf), intro = ByteBufCodecs.BOOL.decode(buf);
		int bk = ByteBufCodecs.VAR_INT.decode(buf), w = ByteBufCodecs.VAR_INT.decode(buf), team = ByteBufCodecs.VAR_INT.decode(buf);
		int stands = ByteBufCodecs.VAR_INT.decode(buf);
		return new MatchSettings(Opponents.values()[Mth.clamp(o, 0, Opponents.values().length - 1)], d, c, Kit.values()[Mth.clamp(k, 0, Kit.values().length - 1)],
				Boss.values()[Mth.clamp(b, 0, Boss.values().length - 1)], t, obstacles, intro, bk, w, team, stands);
	});

	/** Within the limits (a modified client can send anything). */
	public MatchSettings checked() {
		int max = switch (opponents) {
			case BOSS -> 3;
			case MOBS -> 50;
			case BRIDGE -> 5;
			default -> 100;
		};
		return new MatchSettings(opponents, Mth.clamp(difficulty, 1, 10), Mth.clamp(count, 1, max), kit, boss, Mth.clamp(timeLimit, 0, 3600), obstacles, intro,
				Mth.clamp(bridgeKit, 0, 1), Mth.clamp(bridgeWidth, 1, 3), Mth.clamp(bridgeTeam, 0, 1), Mth.clamp(bridgeStands, 0, 1));
	}

	public boolean bridge() {
		return opponents == Opponents.BRIDGE;
	}
}
