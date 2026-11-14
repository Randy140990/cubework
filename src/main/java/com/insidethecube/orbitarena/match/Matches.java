package com.insidethecube.orbitarena.match;

import static com.insidethecube.orbitarena.arena.ArenaLayout.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import com.insidethecube.orbitarena.OrbitArena;
import com.insidethecube.orbitarena.arena.ArenaBuilder;
import com.insidethecube.orbitarena.net.Payloads;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** All the matches going on, the arenas being built, and the rules of the arena dimension. */
public final class Matches {
	private Matches() {}

	private static final Map<UUID, Match> MATCHES = new HashMap<>();
	private static final Map<Integer, ArenaBuilder> BUILDERS = new HashMap<>();

	public static void register() {
		ServerPlayNetworking.registerGlobalReceiver(Payloads.Start.TYPE, (payload, ctx) -> start(ctx.player(), payload.settings().checked()));
		ServerPlayNetworking.registerGlobalReceiver(Payloads.Leave.TYPE, (payload, ctx) -> forfeit(ctx.player()));
		ServerTickEvents.END_SERVER_TICK.register(Matches::tick);

		// Losing a fight in the arena isn't dying: the match ends instead.
		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
			if (entity instanceof ServerPlayer p) {
				Match m = MATCHES.get(p.getUUID());
				if (m != null && m.inArena()) {
					m.onPlayerDown();
					return false;
				}
			}
			return true;
		});
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			// Enemies never hurt each other.
			if (entity.entityTags().contains(Match.ENEMY_TAG) && source.getEntity() != null && source.getEntity().entityTags().contains(Match.ENEMY_TAG)) return false;
			// Nothing hurts a player in the arena outside a fight (the countdown, the result).
			if (entity instanceof ServerPlayer p && p.level().dimension() == OrbitArena.ARENA) {
				Match m = MATCHES.get(p.getUUID());
				return m != null && m.fighting();
			}
			return true;
		});
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, taken, blocked) -> {
			if (entity instanceof ServerPlayer p) {
				Match m = MATCHES.get(p.getUUID());
				if (m != null && m.fighting()) m.onPlayerHurt(taken);
			} else if (entity.entityTags().contains(Match.ENEMY_TAG) && source.getEntity() instanceof ServerPlayer p) {
				Match m = MATCHES.get(p.getUUID());
				if (m != null && m.fighting()) m.onEnemyHurt(taken);
			}
		});
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (!entity.entityTags().contains(Match.ENEMY_TAG)) return;
			for (Match m : MATCHES.values()) m.onEnemyDeath(entity);
		});

		// The arena itself can't be broken or built on (except in creative).
		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, be) -> !protectedFor(player));
		AttackBlockCallback.EVENT.register((player, level, hand, pos, dir) -> protectedFor(player) ? InteractionResult.FAIL : InteractionResult.PASS);
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> protectedFor(player) ? InteractionResult.FAIL : InteractionResult.PASS);

		// Back in after leaving mid-fight (or a crash): everything given back.
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer p = handler.getPlayer();
			if (!MATCHES.containsKey(p.getUUID()) && (Loadout.has(p) || p.level().dimension() == OrbitArena.ARENA)) {
				server.execute(() -> sendHome(p));
			}
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			Match m = MATCHES.remove(handler.getPlayer().getUUID());
			if (m != null) m.abandon();
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			for (Match m : MATCHES.values()) {
				m.abandon();
				Loadout.restore(m.player);
			}
			MATCHES.clear();
			BUILDERS.clear();
		});

		CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) -> dispatcher.register(Commands.literal("arena")
				.then(Commands.literal("leave").executes(c -> {
					forfeit(c.getSource().getPlayerOrException());
					return 1;
				}))
				.then(Commands.literal("rebuild").requires(s -> s.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER))
						.then(Commands.argument("slot", IntegerArgumentType.integer(0, 63)).executes(c -> {
							ServerLevel arena = c.getSource().getServer().getLevel(OrbitArena.ARENA);
							if (arena != null) builder(arena, IntegerArgumentType.getInteger(c, "slot") * SPACING);
							c.getSource().sendSuccess(() -> Component.literal("Rebuilding the arena"), false);
							return 1;
						})))));
	}

	private static boolean protectedFor(Player player) {
		return player.level().dimension() == OrbitArena.ARENA && !player.isCreative();
	}

	static void start(ServerPlayer p, MatchSettings settings) {
		if (MATCHES.containsKey(p.getUUID())) {
			p.displayClientMessage(Component.literal("You're already in a match.").withStyle(ChatFormatting.RED), true);
			return;
		}
		if (Loadout.has(p)) sendHome(p);
		int slot = 0;
		while (slotTaken(slot)) slot++;
		MATCHES.put(p.getUUID(), new Match(p, settings, slot));
	}

	private static boolean slotTaken(int slot) {
		for (Match m : MATCHES.values()) if (m.slot == slot) return true;
		return false;
	}

	static void forfeit(ServerPlayer p) {
		Match m = MATCHES.get(p.getUUID());
		if (m == null) {
			if (Loadout.has(p) || p.level().dimension() == OrbitArena.ARENA) sendHome(p);
			return;
		}
		m.forfeit = true;
		if (m.phase == Match.Phase.RESULT) m.leave();
		else m.end(false, "You left");
	}

	/** Gives back anything put away and sends the player home (or to their spawn if nothing was saved). */
	private static void sendHome(ServerPlayer p) {
		if (Loadout.has(p)) {
			Loadout.restore(p);
		} else if (p.level().dimension() == OrbitArena.ARENA) {
			ServerLevel world = p.level().getServer().overworld();
			var spawn = world.getRespawnData().pos();
			p.teleportTo(world, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, java.util.Set.of(), 0, 0, true);
		}
		ServerPlayNetworking.send(p, new Payloads.State(false, 0, false));
	}

	static ArenaBuilder builder(ServerLevel arena, int centreX) {
		return BUILDERS.computeIfAbsent(centreX, x -> new ArenaBuilder(arena, x));
	}

	private static void tick(MinecraftServer server) {
		Iterator<Map.Entry<Integer, ArenaBuilder>> b = BUILDERS.entrySet().iterator();
		while (b.hasNext()) if (b.next().getValue().tick()) b.remove();
		Iterator<Match> it = MATCHES.values().iterator();
		while (it.hasNext()) {
			Match m = it.next();
			if (m.player.isRemoved() && m.player.hasDisconnected()) {
				m.abandon();
				it.remove();
				continue;
			}
			m.tick();
			if (m.done()) it.remove();
		}
	}

	/** The invisible barrier: inside the fighting floor and below the ceiling. */
	static void keepInside(Entity e, int centreX) {
		double dx = e.getX() - centreX, dz = e.getZ(), r = Math.sqrt(dx * dx + dz * dz);
		double max = FIGHT_RADIUS - e.getBbWidth() / 2 - 0.05, top = FLOOR + CEILING;
		boolean out = r > max, high = e.getY() > top;
		if (!out && !high) return;
		double nx = out ? centreX + dx / r * max : e.getX(), nz = out ? dz / r * max : e.getZ(), ny = high ? top : e.getY();
		Vec3 v = e.getDeltaMovement();
		if (out) {
			// Stop outward movement.
			double along = (v.x * dx + v.z * dz) / r;
			if (along > 0) v = v.subtract(dx / r * along, 0, dz / r * along);
		}
		if (high && v.y > 0) v = new Vec3(v.x, 0, v.z);
		if (e instanceof ServerPlayer p) {
			p.teleportTo(nx, ny, nz);
			p.setDeltaMovement(v);
			p.hurtMarked = true;
		} else {
			e.setPos(nx, ny, nz);
			e.setDeltaMovement(v);
		}
	}

	public static boolean inMatch(Player p) {
		return MATCHES.containsKey(p.getUUID());
	}

	static boolean isArena(Level level) {
		return level.dimension() == OrbitArena.ARENA;
	}
}
