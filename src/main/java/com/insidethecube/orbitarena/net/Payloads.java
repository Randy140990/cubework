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

/** The messages between the arena screen, the server's matches and the crowd. */
public final class Payloads {
	private Payloads() {}

	private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> typeOf(String name) {
		return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(OrbitArena.ID, name));
	}

	/** Client to server: start a match with these settings. */
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
	 * Server to client: whether you're in an arena, its centre (x; the floor is at ArenaLayout.FLOOR and
	 * z = 0), and whether a match is on.
	 */
	public record State(boolean inArena, int centreX, boolean fighting) implements CustomPacketPayload {
		public static final Type<State> TYPE = typeOf("state");
		public static final StreamCodec<ByteBuf, State> CODEC = StreamCodec.composite(
				ByteBufCodecs.BOOL, State::inArena, ByteBufCodecs.VAR_INT, State::centreX, ByteBufCodecs.BOOL, State::fighting, State::new);

		@Override
		public Type<State> type() {
			return TYPE;
		}
	}

	/** Server to client: the crowd reacts (0 to 1: a hit, a kill, a win; negative: groans when you're hurt). */
	public record Cheer(float strength) implements CustomPacketPayload {
		public static final Type<Cheer> TYPE = typeOf("cheer");
		public static final StreamCodec<ByteBuf, Cheer> CODEC = ByteBufCodecs.FLOAT.map(Cheer::new, Cheer::strength);

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

	public static void register() {
		PayloadTypeRegistry.playC2S().register(Start.TYPE, codec(Start.CODEC));
		PayloadTypeRegistry.playC2S().register(Leave.TYPE, codec(Leave.CODEC));
		PayloadTypeRegistry.playS2C().register(State.TYPE, codec(State.CODEC));
		PayloadTypeRegistry.playS2C().register(Cheer.TYPE, codec(Cheer.CODEC));
		PayloadTypeRegistry.playS2C().register(Fade.TYPE, codec(Fade.CODEC));
	}

	@SuppressWarnings("unchecked")
	private static <T> StreamCodec<RegistryFriendlyByteBuf, T> codec(StreamCodec<? super RegistryFriendlyByteBuf, T> c) {
		return (StreamCodec<RegistryFriendlyByteBuf, T>) c;
	}
}
