package com.insidethecube.deepkit.lamp;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** A lamp, on its own or fitted to armour: which lamp, how much battery is left (ticks), and whether it's on. */
public record LampData(int kind, int charge, boolean on) {
	public static final Codec<LampData> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.INT.fieldOf("kind").forGetter(LampData::kind),
			Codec.INT.fieldOf("charge").forGetter(LampData::charge),
			Codec.BOOL.fieldOf("on").forGetter(LampData::on)).apply(i, LampData::new));
	public static final StreamCodec<ByteBuf, LampData> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, LampData::kind,
			ByteBufCodecs.VAR_INT, LampData::charge, ByteBufCodecs.BOOL, LampData::on, LampData::new);

	public static LampData full(LampKind k) {
		return new LampData(k.ordinal(), k.battery, false);
	}

	public LampKind lamp() {
		LampKind[] all = LampKind.values();
		return all[Math.floorMod(kind, all.length)];
	}

	public LampData withCharge(int c) {
		return new LampData(kind, Math.max(0, Math.min(lamp().battery, c)), c > 0 && on);
	}

	public LampData withOn(boolean o) {
		return new LampData(kind, charge, o && charge > 0);
	}

	public int percent() {
		return (int) Math.round(100.0 * charge / lamp().battery);
	}
}
