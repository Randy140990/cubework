package com.insidethecube.orbitarena.match;

import static com.insidethecube.orbitarena.arena.ArenaLayout.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import com.insidethecube.orbitarena.OrbitArena;
import com.insidethecube.orbitarena.arena.ArenaBuilder;
import com.insidethecube.orbitarena.arena.BridgeBuilder;
import com.insidethecube.orbitarena.arena.BridgeLayout;
import com.insidethecube.orbitarena.net.Payloads;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorStandItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.EndCrystalItem;
import net.minecraft.world.item.FireChargeItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** All the games going on, the arenas and bridges being built, and the rules of the arena dimension. */
public final class Matches {
	private Matches() {}

	/** Something building an arena or a bridge a slice at a time. */
	public interface Builder {
		/** Builds the next slice; true when it's all done. */
		boolean tick();

		/** 0 to 1. */
		float progress();
	}

	private static final Map<UUID, Game> GAMES = new HashMap<>();
	private static final Map<Long, Builder> BUILDERS = new HashMap<>();

	public static long key(int centreX, int centreZ) {
		return ((long) centreX << 32) | (centreZ & 0xFFFFFFFFL);
	}

	public static void register() {
		ServerPlayNetworking.registerGlobalReceiver(Payloads.Start.TYPE, (payload, ctx) -> start(ctx.player(), payload.settings().checked()));
		ServerPlayNetworking.registerGlobalReceiver(Payloads.Leave.TYPE, (payload, ctx) -> forfeit(ctx.player()));
		// A celebration: everyone near sees it.
		ServerPlayNetworking.registerGlobalReceiver(Payloads.Emote.TYPE, (payload, ctx) -> {
			ServerPlayer p = ctx.player();
			if (!isArena(p.level())) return;
			for (ServerPlayer other : ((ServerLevel) p.level()).players()) {
				if (other != p && other.distanceToSqr(p) < 160 * 160) ServerPlayNetworking.send(other, new Payloads.EmoteOf(p.getId(), payload.emote()));
			}
		});
		ServerTickEvents.END_SERVER_TICK.register(Matches::tick);
		// The arena always fights at Normal, whatever the world's difficulty (Peaceful would delete every enemy).
		ServerWorldEvents.LOAD.register((server, world) -> {
			if (world.dimension() == OrbitArena.ARENA && world.getLevelData() instanceof ArenaDifficulty d) d.orbitarena$markArena();
		});

		// Who can hurt whom, what happens instead of dying, and what the crowd makes of it: the game decides.
		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
			Game g = gameOf(entity);
			return g == null || g.allowDeath(entity, source);
		});
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			Game g = gameOf(entity);
			if (g != null) return g.allowDamage(entity, source, amount);
			// Nothing hurts a player in the arena outside a game.
			return !(entity instanceof ServerPlayer p && isArena(p.level()));
		});
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, taken, blocked) -> {
			Game g = gameOf(entity);
			if (g != null) g.afterDamage(entity, source, taken);
		});
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			for (Game g : GAMES.values()) g.onDeath(entity);
		});

		// The arena and the bridges can't be broken or built on, except what the bridge allows (blocks placed
		// in a bridge game, and building near the bridge). Creative is free to build, but not mid-fight.
		// Only placing things is stopped: pearls, wind charges, shields and bows all work while you're looking
		// at the floor.
		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, be) -> mayBreak(player, pos));
		AttackBlockCallback.EVENT.register((player, level, hand, pos, dir) -> mayBreak(player, pos) ? InteractionResult.PASS : InteractionResult.FAIL);
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			ItemStack stack = player.getItemInHand(hand);
			if (!isArena(level) || !placesSomething(stack) || freeToBuild(player)) return InteractionResult.PASS;
			if (stack.getItem() instanceof BlockItem) {
				BlockPos at = new BlockPlaceContext(player, hand, stack, hit).getClickedPos();
				if (level.isClientSide()) return BridgeLayout.near(at) ? InteractionResult.PASS : InteractionResult.FAIL;
				Game g = player instanceof ServerPlayer sp ? GAMES.get(sp.getUUID()) : null;
				if (g != null && g.fighting() && g.mayPlace(at)) return InteractionResult.PASS;
				String why = g == null ? null : g.whyNotPlace(at);
				if (why != null) player.displayClientMessage(Component.literal(why).withStyle(ChatFormatting.GRAY), true);
				return InteractionResult.FAIL;
			}
			return InteractionResult.FAIL;
		});

		// Back in after leaving mid-fight (or a crash): everything given back.
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer p = handler.getPlayer();
			if (!GAMES.containsKey(p.getUUID()) && (Loadout.has(p) || p.level().dimension() == OrbitArena.ARENA)) {
				server.execute(() -> sendHome(p));
			}
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			Game g = GAMES.remove(handler.getPlayer().getUUID());
			if (g != null) g.abandon();
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			for (Game g : GAMES.values()) {
				g.abandon();
				Loadout.restore(g.player());
			}
			GAMES.clear();
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
							int slot = IntegerArgumentType.getInteger(c, "slot");
							if (arena != null) {
								builder(arena, slot * SPACING);
								bridgeBuilder(arena, slot * SPACING, true, false, 2);
							}
							c.getSource().sendSuccess(() -> Component.literal("Rebuilding the arena and the bridge"), false);
							return 1;
						})))));
	}

	/** Items that would put something into the arena or change it when used on a block. */
	private static boolean placesSomething(ItemStack stack) {
		Item i = stack.getItem();
		return i instanceof BlockItem || i instanceof BucketItem || i instanceof FlintAndSteelItem || i instanceof FireChargeItem || i instanceof SpawnEggItem
				|| i instanceof BoneMealItem || i instanceof EndCrystalItem || i instanceof ArmorStandItem;
	}

	/** Really in Creative (not as mixin.PlayerMixin shows it mid-fight). */
	static boolean realCreative(Player p) {
		return p instanceof ServerPlayer sp ? sp.gameMode.getGameModeForPlayer() == GameType.CREATIVE : p.isCreative();
	}

	/** Creative and not in a fight: free to build and break (Axiom included). */
	private static boolean freeToBuild(Player p) {
		if (!realCreative(p)) return false;
		Game g = p instanceof ServerPlayer sp ? GAMES.get(sp.getUUID()) : null;
		return g == null || !g.fighting();
	}

	private static boolean mayBreak(Player player, BlockPos pos) {
		if (!isArena(player.level()) || freeToBuild(player)) return true;
		if (player.level().isClientSide()) return BridgeLayout.near(pos);
		Game g = player instanceof ServerPlayer sp ? GAMES.get(sp.getUUID()) : null;
		return g != null && g.fighting() && g.mayBreak(pos);
	}

	/**
	 * Fighting in Creative (Axiom's editor): mixin.PlayerMixin shows the player as not Creative for the
	 * fight, so everything still comes for them.
	 */
	public static boolean fightingInCreative(ServerPlayer p) {
		if (GAMES.isEmpty()) return false;
		Game g = GAMES.get(p.getUUID());
		return g != null && g.fighting() && p.gameMode.getGameModeForPlayer() == GameType.CREATIVE;
	}

	/** The game a player, an enemy or a teammate belongs to. */
	static Game gameOf(LivingEntity e) {
		if (GAMES.isEmpty()) return null;
		if (e instanceof ServerPlayer p) {
			Game g = GAMES.get(p.getUUID());
			if (g != null) return g;
		}
		if (!isArena(e.level())) return null;
		for (Game g : GAMES.values()) if (g instanceof BridgeMatch b && b.involves(e) || g instanceof Match m && m.alive.contains(e)) return g;
		return null;
	}

	static void start(ServerPlayer p, MatchSettings settings) {
		Game old = GAMES.get(p.getUUID());
		if (old != null) {
			// Just finished one, still in the arena: go again straight away (in the stadium or on the bridge,
			// whichever the settings say).
			if (old.awaitingChoice() && isArena(p.level())) {
				old.retire();
				GAMES.put(p.getUUID(), settings.bridge() ? new BridgeMatch(p, settings, old.slot(), true) : new Match(p, settings, old.slot(), true));
				return;
			}
			p.displayClientMessage(Component.literal("You're already in a match.").withStyle(ChatFormatting.RED), true);
			return;
		}
		if (Loadout.has(p)) sendHome(p);
		int slot = 0;
		while (slotTaken(slot)) slot++;
		GAMES.put(p.getUUID(), settings.bridge() ? new BridgeMatch(p, settings, slot, false) : new Match(p, settings, slot, false));
	}

	/** The builder at work here, or null. */
	static Builder building(long key) {
		return BUILDERS.get(key);
	}

	private static boolean slotTaken(int slot) {
		for (Game g : GAMES.values()) if (g.slot() == slot) return true;
		return false;
	}

	static void forfeit(ServerPlayer p) {
		Game g = GAMES.get(p.getUUID());
		if (g == null) {
			if (Loadout.has(p) || p.level().dimension() == OrbitArena.ARENA) sendHome(p);
			return;
		}
		g.forfeit();
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
		ServerPlayNetworking.send(p, new Payloads.State(false, Payloads.State.ARENA, 0, 0, false));
	}

	static Builder builder(ServerLevel arena, int centreX) {
		return BUILDERS.computeIfAbsent(key(centreX, 0), k -> new ArenaBuilder(arena, centreX));
	}

	/** Builds the bridge (full: everything; otherwise just the playing area, put right). */
	static Builder bridgeBuilder(ServerLevel arena, int centreX, boolean full, boolean obstacles, int width) {
		return BUILDERS.computeIfAbsent(key(centreX, BridgeLayout.Z), k -> new BridgeBuilder(arena, centreX, full).obstacles(obstacles).width(width));
	}

	private static void tick(MinecraftServer server) {
		Iterator<Map.Entry<Long, Builder>> b = BUILDERS.entrySet().iterator();
		while (b.hasNext()) if (b.next().getValue().tick()) b.remove();
		Iterator<Game> it = GAMES.values().iterator();
		while (it.hasNext()) {
			Game g = it.next();
			if (g.player().isRemoved() && g.player().hasDisconnected()) {
				g.abandon();
				it.remove();
				continue;
			}
			g.tick();
			if (g.done()) it.remove();
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
		return GAMES.containsKey(p.getUUID());
	}

	static boolean isArena(Level level) {
		return level.dimension() == OrbitArena.ARENA;
	}
}
