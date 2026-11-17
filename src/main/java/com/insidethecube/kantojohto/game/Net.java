package com.insidethecube.kantojohto.game;

import com.insidethecube.kantojohto.RouteOne;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * The two messages between the game on the server and the screens: an action from you (talk, a battle
 * choice, buy, the starter you picked...) and something for your screen (your data, a text box, a battle
 * turn...). Each carries a kind and its details.
 */
public final class Net {
	private Net() {}

	/** Client to server. */
	public record Act(String kind, CompoundTag data) implements CustomPacketPayload {
		public static final Type<Act> TYPE = new Type<>(RouteOne.id("act"));
		public static final StreamCodec<ByteBuf, Act> CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, Act::kind, ByteBufCodecs.TRUSTED_COMPOUND_TAG,
				Act::data, Act::new);

		@Override
		public Type<Act> type() {
			return TYPE;
		}
	}

	/** Server to client. */
	public record Push(String kind, CompoundTag data) implements CustomPacketPayload {
		public static final Type<Push> TYPE = new Type<>(RouteOne.id("push"));
		public static final StreamCodec<ByteBuf, Push> CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, Push::kind, ByteBufCodecs.TRUSTED_COMPOUND_TAG,
				Push::data, Push::new);

		@Override
		public Type<Push> type() {
			return TYPE;
		}
	}

	public static void register() {
		PayloadTypeRegistry.playC2S().register(Act.TYPE, codec(Act.CODEC));
		PayloadTypeRegistry.playS2C().register(Push.TYPE, codec(Push.CODEC));
	}

	@SuppressWarnings("unchecked")
	private static <T> StreamCodec<RegistryFriendlyByteBuf, T> codec(StreamCodec<? super RegistryFriendlyByteBuf, T> c) {
		return (StreamCodec<RegistryFriendlyByteBuf, T>) c;
	}

	public static void send(ServerPlayer p, String kind, CompoundTag data) {
		ServerPlayNetworking.send(p, new Push(kind, data));
	}
}
