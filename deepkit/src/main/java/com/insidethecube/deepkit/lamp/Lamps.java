package com.insidethecube.deepkit.lamp;

import com.insidethecube.deepkit.DK;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Every tick, for every player: lamps that are on use battery, and each lights where its beam lands. A hand
 * torch shines while it's held in either hand; a fitted lamp while its armour is worn. Below 10% the light
 * flickers.
 */
public final class Lamps {
	private Lamps() {}

	public static void tick(MinecraftServer server) {
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			if (!(p.level() instanceof ServerLevel level) || p.isSpectator()) continue;
			long t = level.getGameTime();
			for (InteractionHand hand : InteractionHand.values()) {
				ItemStack s = p.getItemInHand(hand);
				if (s.getItem() instanceof LampItem) run(level, p, s, t);
			}
			run(level, p, p.getItemBySlot(EquipmentSlot.HEAD), t);
			run(level, p, p.getItemBySlot(EquipmentSlot.CHEST), t);
		}
	}

	private static void run(ServerLevel level, ServerPlayer p, ItemStack stack, long t) {
		LampData d = stack.get(DK.LAMP);
		if (d == null || !d.on()) return;
		// Battery, a second at a time (so the item isn't re-sent every tick).
		if (!p.hasInfiniteMaterials() && t % 20 == 0) {
			LampData next = d.withCharge(d.charge() - 20);
			stack.set(DK.LAMP, next);
			if (!next.on()) {
				level.playSound(null, p.blockPosition(), SoundEvents.LEVER_CLICK, SoundSource.PLAYERS, 0.4f, 0.6f);
				p.displayClientMessage(Component.literal(d.lamp().title + ": battery flat"), true);
				return;
			}
			if (d.percent() > 10 && next.percent() <= 10) p.displayClientMessage(Component.literal(d.lamp().title + ": battery low"), true);
		}
		if (t % 2 != 0) return;
		boolean low = d.percent() <= 10;
		if (low && level.getRandom().nextFloat() < 0.35f) return;
		beam(level, p, d.lamp());
	}

	/** Lights where the beam lands, and more faintly halfway along it. */
	private static void beam(ServerLevel level, ServerPlayer p, LampKind kind) {
		Vec3 from = p.getEyePosition();
		Vec3 dir;
		if (kind == LampKind.CHEST) {
			// The chest lamp faces where the body faces, a little down.
			double yaw = Math.toRadians(p.yBodyRot), pitch = Math.toRadians(10);
			dir = new Vec3(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
			from = from.add(0, -0.45, 0);
		} else {
			dir = p.getViewVector(1f);
		}
		double range = kind.range * (p.isEyeInFluid(FluidTags.WATER) ? 2 / 3.0 : 1);
		Vec3 end = from.add(dir.scale(range));
		BlockHitResult hit = level.clip(new ClipContext(from, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
		double dist = range;
		if (hit.getType() == HitResult.Type.BLOCK) {
			LightPools.light(level, hit.getBlockPos().relative(hit.getDirection()), kind.light);
			dist = hit.getLocation().distanceTo(from);
		} else {
			LightPools.light(level, BlockPos.containing(end), kind.light - 4);
		}
		if (dist > 6) LightPools.light(level, BlockPos.containing(from.add(dir.scale(dist / 2))), kind.light - 3);
	}

	/** The lamp key: switches the lamps fitted to the helmet and chestplate (all off if any is on, else all on). */
	public static void toggleWorn(ServerPlayer p) {
		ItemStack head = p.getItemBySlot(EquipmentSlot.HEAD), chest = p.getItemBySlot(EquipmentSlot.CHEST);
		LampData h = head.get(DK.LAMP), c = chest.get(DK.LAMP);
		if (h == null && c == null) {
			// Nothing worn: the key switches a hand torch in either hand instead.
			for (InteractionHand hand : InteractionHand.values()) {
				ItemStack s = p.getItemInHand(hand);
				if (s.getItem() instanceof LampItem lamp && lamp.kind == LampKind.TORCH) {
					lamp.use(p.level(), p, hand);
					return;
				}
			}
			p.displayClientMessage(Component.literal("You're not wearing a lamp. Right-click a headlamp or chest lamp to put it on."), true);
			return;
		}
		boolean anyOn = (h != null && h.on()) || (c != null && c.on());
		boolean flat = true;
		if (h != null) {
			head.set(DK.LAMP, h.withOn(!anyOn));
			flat &= h.charge() <= 0;
		}
		if (c != null) {
			chest.set(DK.LAMP, c.withOn(!anyOn));
			flat &= c.charge() <= 0;
		}
		p.level().playSound(null, p.blockPosition(), SoundEvents.LEVER_CLICK, SoundSource.PLAYERS, 0.4f, anyOn ? 0.8f : 1.2f);
		if (!anyOn && flat) {
			p.displayClientMessage(Component.literal("The battery is flat. Charge it with glowstone dust or redstone in a crafting grid."), true);
			return;
		}
		String what = h != null && c != null ? "Lamps" : h != null ? LampKind.HEAD.title : LampKind.CHEST.title;
		int charge = h != null ? h.percent() : c.percent();
		p.displayClientMessage(Component.literal(what + (anyOn ? " off" : " on, battery " + charge + "%")), true);
	}
}
