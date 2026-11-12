package com.insidethecube.deepkit.net;

import com.insidethecube.deepkit.DK;
import com.insidethecube.deepkit.lamp.Lamps;
import com.insidethecube.deepkit.paint.GlowPaintItem;
import com.insidethecube.deepkit.paint.Marks;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/** What the client sends: the lamp key, and the mark chosen for a can of glow paint. */
public final class Payloads {
	private Payloads() {}

	public record ToggleLamps(boolean unused) implements CustomPacketPayload {
		public static final Type<ToggleLamps> TYPE = new Type<>(DK.id("toggle_lamps"));
		public static final StreamCodec<RegistryFriendlyByteBuf, ToggleLamps> CODEC = StreamCodec.composite(ByteBufCodecs.BOOL, ToggleLamps::unused,
				ToggleLamps::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record SetMark(int mark) implements CustomPacketPayload {
		public static final Type<SetMark> TYPE = new Type<>(DK.id("set_mark"));
		public static final StreamCodec<RegistryFriendlyByteBuf, SetMark> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, SetMark::mark,
				SetMark::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static void register() {
		PayloadTypeRegistry.playC2S().register(ToggleLamps.TYPE, ToggleLamps.CODEC);
		PayloadTypeRegistry.playC2S().register(SetMark.TYPE, SetMark.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(ToggleLamps.TYPE, (payload, ctx) -> ctx.server().execute(() -> Lamps.toggleWorn(ctx.player())));
		ServerPlayNetworking.registerGlobalReceiver(SetMark.TYPE, (payload, ctx) -> ctx.server().execute(() -> {
			for (InteractionHand hand : InteractionHand.values()) {
				ItemStack s = ctx.player().getItemInHand(hand);
				if (s.getItem() instanceof GlowPaintItem) {
					s.set(DK.MARK, Marks.clamp(payload.mark()));
					return;
				}
			}
		}));
	}
}
