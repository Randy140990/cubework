package com.insidethecube.orbitarena.net;

import com.insidethecube.orbitarena.OrbitArena;
import com.insidethecube.orbitarena.match.MatchSettings;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** The messages between the arena screen, the server's games, the crowd and the screen effects. */
public final class Payloads {
	private Payloads() {}

	private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> typeOf(String name) {
		return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(OrbitArena.ID, name));
	}

	/** Client to server: start a match with these settings (or, when one has just ended, go again with them). */
	public record Start(MatchSettings settings) implements CustomPacketPayload {
		public static final Type<Start> TYPE = typeOf("start");
		public static final StreamCodec<ByteBuf, Start> CODEC = MatchSettings.STREAM_CODEC.map(Start::new, Start::settings);

		@Override
		public Type<Start> type() {
			return TYPE;
		}
	}

	/** Client to server: leave the arena now (the match ends as a forfeit). */
	public record Leave() implements CustomPacketPayload {
		public static final Type<Leave> TYPE = typeOf("leave");
		public static final StreamCodec<ByteBuf, Leave> CODEC = StreamCodec.unit(new Leave());

		@Override
		public Type<Leave> type() {
			return TYPE;
		}
	}

	/**
	 * Server to client: whether you're in an arena, which kind (the stadium or the bridge), its centre (x and
	 * z; the floor is at ArenaLayout.FLOOR), and whether a match is on.
	 */
	public record State(boolean inArena, int mode, int centreX, int centreZ, boolean fighting) implements CustomPacketPayload {
		public static final int ARENA = 0, BRIDGE = 1, BRIDGE_RING = 2;
		public static final Type<State> TYPE = typeOf("state");
		public static final StreamCodec<ByteBuf, State> CODEC = StreamCodec.composite(
				ByteBufCodecs.BOOL, State::inArena, ByteBufCodecs.VAR_INT, State::mode, ByteBufCodecs.VAR_INT, State::centreX, ByteBufCodecs.VAR_INT,
				State::centreZ, ByteBufCodecs.BOOL, State::fighting, State::new);

		@Override
		public Type<State> type() {
			return TYPE;
		}
	}

	/**
	 * Server to client: something the crowd reacts to (one of the kinds below), and how big it was (0 to 1:
	 * a glancing hit is small, a heavy one big).
	 */
	public record Cheer(int kind, float strength) implements CustomPacketPayload {
		/** GOAL: a team scored (strength: which, 0 blue, 1 red); END: a team won (strength: which, or 2 for a draw). */
		public static final int HIT = 0, KILL = 1, FIGHT = 2, WIN = 3, LOSS = 4, HURT = 5, GOAL = 6, END = 7;
		public static final Type<Cheer> TYPE = typeOf("cheer");
		public static final StreamCodec<ByteBuf, Cheer> CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, Cheer::kind, ByteBufCodecs.FLOAT, Cheer::strength, Cheer::new);

		@Override
		public Type<Cheer> type() {
			return TYPE;
		}
	}

	/** Server to client: fade the screen to black and back (ticks out, held, in), round a teleport. */
	public record Fade(int out, int hold, int in) implements CustomPacketPayload {
		public static final Type<Fade> TYPE = typeOf("fade");
		public static final StreamCodec<ByteBuf, Fade> CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, Fade::out, ByteBufCodecs.VAR_INT, Fade::hold, ByteBufCodecs.VAR_INT, Fade::in, Fade::new);

		@Override
		public Type<Fade> type() {
			return TYPE;
		}
	}

	/**
	 * Server to client: the match is over (won or not, a line of how it went). The client offers to go
	 * again, change the settings or leave.
	 */
	public record Result(boolean won, String reason, String summary) implements CustomPacketPayload {
		public static final Type<Result> TYPE = typeOf("result");
		public static final StreamCodec<ByteBuf, Result> CODEC = StreamCodec.composite(
				ByteBufCodecs.BOOL, Result::won, ByteBufCodecs.STRING_UTF8, Result::reason, ByteBufCodecs.STRING_UTF8, Result::summary, Result::new);

		@Override
		public Type<Result> type() {
			return TYPE;
		}
	}

	/**
	 * Server to client: the fly-round before a match (which arena, its centre, how long, and the title over
	 * it), the camera swooping from out in space past the crowd to where you stand.
	 */
	public record Cinematic(int kind, int centreX, int centreZ, int ticks, String title, String subtitle) implements CustomPacketPayload {
		/** The stadium, or the bridge seen coming in to blue's base or red's. */
		public static final int ARENA = 0, BRIDGE = 1, BRIDGE_RED = 2;
		public static final Type<Cinematic> TYPE = typeOf("cinematic");
		public static final StreamCodec<ByteBuf, Cinematic> CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, Cinematic::kind, ByteBufCodecs.VAR_INT, Cinematic::centreX, ByteBufCodecs.VAR_INT, Cinematic::centreZ,
				ByteBufCodecs.VAR_INT, Cinematic::ticks, ByteBufCodecs.STRING_UTF8, Cinematic::title, ByteBufCodecs.STRING_UTF8, Cinematic::subtitle,
				Cinematic::new);

		@Override
		public Type<Cinematic> type() {
			return TYPE;
		}
	}

	/** Server to client: you won; celebrate (see yourself cheer, from the front) for this many ticks. */
	public record Celebrate(int ticks) implements CustomPacketPayload {
		public static final Type<Celebrate> TYPE = typeOf("celebrate");
		public static final StreamCodec<ByteBuf, Celebrate> CODEC = ByteBufCodecs.VAR_INT.map(Celebrate::new, Celebrate::ticks);

		@Override
		public Type<Celebrate> type() {
			return TYPE;
		}
	}

	/**
	 * Server to client: The Bridge's score (blue, your team; red), goals to win, seconds left (-1: no limit),
	 * who's playing, and which fighter look leads red (for its face on the banner).
	 */
	public record BridgeScore(int blue, int red, int toWin, int secondsLeft, String blueName, String redName, int you, int theirLook) implements CustomPacketPayload {
		public static final Type<BridgeScore> TYPE = typeOf("bridge_score");
		public static final StreamCodec<ByteBuf, BridgeScore> CODEC = StreamCodec.of((buf, s) -> {
			ByteBufCodecs.VAR_INT.encode(buf, s.blue);
			ByteBufCodecs.VAR_INT.encode(buf, s.red);
			ByteBufCodecs.VAR_INT.encode(buf, s.toWin);
			ByteBufCodecs.INT.encode(buf, s.secondsLeft);
			ByteBufCodecs.STRING_UTF8.encode(buf, s.blueName);
			ByteBufCodecs.STRING_UTF8.encode(buf, s.redName);
			ByteBufCodecs.VAR_INT.encode(buf, s.you);
			ByteBufCodecs.VAR_INT.encode(buf, s.theirLook);
		}, buf -> new BridgeScore(ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
				ByteBufCodecs.INT.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
				ByteBufCodecs.VAR_INT.decode(buf)));

		@Override
		public Type<BridgeScore> type() {
			return TYPE;
		}
	}

	/** Server to client: a team was scored on, and that pillar's crystal shatters (team, pillar 0 to 4). */
	public record Shatter(int team, int pillar) implements CustomPacketPayload {
		public static final Type<Shatter> TYPE = typeOf("shatter");
		public static final StreamCodec<ByteBuf, Shatter> CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, Shatter::team, ByteBufCodecs.VAR_INT, Shatter::pillar, Shatter::new);

		@Override
		public Type<Shatter> type() {
			return TYPE;
		}
	}

	/** Client to server: I'm celebrating (one of client.Emotes). */
	public record Emote(int emote) implements CustomPacketPayload {
		public static final Type<Emote> TYPE = typeOf("emote");
		public static final StreamCodec<ByteBuf, Emote> CODEC = ByteBufCodecs.VAR_INT.map(Emote::new, Emote::emote);

		@Override
		public Type<Emote> type() {
			return TYPE;
		}
	}

	/** Server to client: someone near you is celebrating (their entity id, and which). */
	public record EmoteOf(int entityId, int emote) implements CustomPacketPayload {
		public static final Type<EmoteOf> TYPE = typeOf("emote_of");
		public static final StreamCodec<ByteBuf, EmoteOf> CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, EmoteOf::entityId, ByteBufCodecs.VAR_INT, EmoteOf::emote, EmoteOf::new);

		@Override
		public Type<EmoteOf> type() {
			return TYPE;
		}
	}

	public static void register() {
		PayloadTypeRegistry.playC2S().register(Start.TYPE, codec(Start.CODEC));
		PayloadTypeRegistry.playC2S().register(Leave.TYPE, codec(Leave.CODEC));
		PayloadTypeRegistry.playC2S().register(Emote.TYPE, codec(Emote.CODEC));
		PayloadTypeRegistry.playS2C().register(State.TYPE, codec(State.CODEC));
		PayloadTypeRegistry.playS2C().register(Cheer.TYPE, codec(Cheer.CODEC));
		PayloadTypeRegistry.playS2C().register(Fade.TYPE, codec(Fade.CODEC));
		PayloadTypeRegistry.playS2C().register(Result.TYPE, codec(Result.CODEC));
		PayloadTypeRegistry.playS2C().register(Cinematic.TYPE, codec(Cinematic.CODEC));
		PayloadTypeRegistry.playS2C().register(Celebrate.TYPE, codec(Celebrate.CODEC));
		PayloadTypeRegistry.playS2C().register(BridgeScore.TYPE, codec(BridgeScore.CODEC));
		PayloadTypeRegistry.playS2C().register(Shatter.TYPE, codec(Shatter.CODEC));
		PayloadTypeRegistry.playS2C().register(EmoteOf.TYPE, codec(EmoteOf.CODEC));
	}

	@SuppressWarnings("unchecked")
	private static <T> StreamCodec<RegistryFriendlyByteBuf, T> codec(StreamCodec<? super RegistryFriendlyByteBuf, T> c) {
		return (StreamCodec<RegistryFriendlyByteBuf, T>) c;
	}
}
