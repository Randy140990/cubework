package com.insidethecube.cubework.net;

import com.insidethecube.cubework.Reg;
import com.insidethecube.cubework.ShapeSwap;
import com.insidethecube.cubework.tool.ToolActions;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** What the client sends: a shape picked on the wheel, a tool setting, a tool action (undo, clear). */
public final class Payloads {
	private Payloads() {}

	public record PickShape(String item) implements CustomPacketPayload {
		public static final Type<PickShape> TYPE = new Type<>(Reg.id("pick_shape"));
		public static final StreamCodec<RegistryFriendlyByteBuf, PickShape> CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, PickShape::item,
				PickShape::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record ToolSetting(String key, int value) implements CustomPacketPayload {
		public static final Type<ToolSetting> TYPE = new Type<>(Reg.id("tool_setting"));
		public static final StreamCodec<RegistryFriendlyByteBuf, ToolSetting> CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, ToolSetting::key,
				ByteBufCodecs.VAR_INT, ToolSetting::value, ToolSetting::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record ToolAction(String action) implements CustomPacketPayload {
		public static final Type<ToolAction> TYPE = new Type<>(Reg.id("tool_action"));
		public static final StreamCodec<RegistryFriendlyByteBuf, ToolAction> CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, ToolAction::action,
				ToolAction::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static void register() {
		PayloadTypeRegistry.playC2S().register(PickShape.TYPE, PickShape.CODEC);
		PayloadTypeRegistry.playC2S().register(ToolSetting.TYPE, ToolSetting.CODEC);
		PayloadTypeRegistry.playC2S().register(ToolAction.TYPE, ToolAction.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(PickShape.TYPE, (payload, ctx) -> ctx.server().execute(() -> {
			Identifier id = Identifier.tryParse(payload.item());
			if (id != null) ShapeSwap.pick(ctx.player(), id);
		}));
		ServerPlayNetworking.registerGlobalReceiver(ToolSetting.TYPE, (payload, ctx) -> ctx.server().execute(() ->
				ToolActions.setting(ctx.player(), payload.key(), payload.value())));
		ServerPlayNetworking.registerGlobalReceiver(ToolAction.TYPE, (payload, ctx) -> ctx.server().execute(() ->
				ToolActions.action(ctx.player(), payload.action())));
	}
}
