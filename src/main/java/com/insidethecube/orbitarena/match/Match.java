package com.insidethecube.orbitarena.match;

import static com.insidethecube.orbitarena.arena.ArenaLayout.*;

import java.util.ArrayList;
import java.util.List;

import com.insidethecube.orbitarena.OrbitArena;
import com.insidethecube.orbitarena.arena.ArenaBuilder;
import com.insidethecube.orbitarena.net.Payloads;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * One player's match: getting the arena ready, the fade in, "3, 2, 1, Fight!", the fight (enemies
 * topped up in waves until all are beaten), the result, and then the player's choice: go again (straight
 * back onto the pedestal, the arena put right first if a mob or boss fight damaged it), change the
 * settings and go again, or fade back home with everything returned.
 * <p>
 * The player always fights in Survival, whatever their game mode was (and if anything switches it during
 * the fight, it's switched back), and is only let go once they're really standing in the arena.
 */
public final class Match {
	private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

	enum Phase {
		PREPARING, ENTERING, COUNTDOWN, FIGHT, RESULT, LEAVING, DONE
	}

	public static final String ENEMY_TAG = "orbitarena_enemy";

	final ServerPlayer player;
	final MatchSettings settings;
	final int slot, centreX;
	/** Another go straight after the last match: the player is already in the arena, their things put away. */
	final boolean again;
	Phase phase = Phase.PREPARING;
	int ticks, fightTicks;
	final List<LivingEntity> alive = new ArrayList<>();
	int spawned, kills;
	float dealt, taken;
	boolean won, forfeit;
	String endReason = "";
	private final ServerBossEvent bar = new ServerBossEvent(Component.literal("Orbit Arena"), BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.PROGRESS);

	Match(ServerPlayer player, MatchSettings settings, int slot, boolean again) {
		this.player = player;
		this.settings = settings;
		this.slot = slot;
		this.centreX = slot * SPACING;
		this.again = again;
	}

	ServerLevel arena() {
		return player.level().getServer().getLevel(OrbitArena.ARENA);
	}

	Vec3 start() {
		return new Vec3(centreX - PEDESTAL_X + 0.5, FLOOR + PEDESTAL_TOP, 0.5);
	}

	AABB box() {
		return new AABB(centreX - WALL_OUT, FLOOR - 4, -WALL_OUT, centreX + WALL_OUT, FLOOR + CEILING + 30, WALL_OUT);
	}

	int maxAlive() {
		int d = settings.difficulty();
		return switch (settings.opponents()) {
			case FIGHTERS -> Math.min(settings.count(), Math.min(30, 4 + 2 * d));
			case MOBS -> Math.min(settings.count(), Math.min(40, 4 + 3 * d));
			case BOSS -> settings.count();
		};
	}

	void tick() {
		ticks++;
		ServerLevel arena = arena();
		if (arena == null) return;
		switch (phase) {
			case PREPARING -> {
				// Going again: wait on the pedestal while the arena's put right.
				if (again) hold(arena);
				// Wait until the arena is built and nothing is building or repairing it (stepping in while it's
				// being rebuilt could leave you with no floor).
				ArenaBuilder running = Matches.building(centreX);
				if (running != null) {
					player.displayClientMessage(Component.literal((again ? "Putting the arena right… " : "Building the arena… ") + (int) (running.progress() * 100) + "%")
							.withStyle(ChatFormatting.AQUA), true);
				} else if (!ArenaBuilder.built(arena, centreX)) {
					Matches.builder(arena, centreX);
				} else if (again) {
					player.getInventory().clearContent();
					arm(arena);
					countdown();
				} else {
					enter();
				}
			}
			case ENTERING -> {
				// Halfway through the fade: put things away, step through, kit up.
				if (ticks == 12) {
					try {
						Loadout.store(player);
					} catch (RuntimeException e) {
						LOGGER.error("Orbit Arena: couldn't put {}'s things away", player.getName().getString(), e);
					}
					arm(arena);
				}
				// Not through yet (the game can refuse a teleport mid-load): try again until you are.
				if (ticks > 12 && player.level() != arena && ticks % 10 == 0) teleportIn(arena);
				if (ticks >= 30) {
					if (player.level() == arena) {
						countdown();
					} else if (ticks > 400) {
						LOGGER.warn("Orbit Arena: couldn't get {} into the arena", player.getName().getString());
						player.displayClientMessage(Component.literal("Couldn't get you into the arena. Try again.").withStyle(ChatFormatting.RED), false);
						phase = Phase.RESULT;
						leave();
					}
				}
			}
			case COUNTDOWN -> {
				keepSurvival();
				hold(arena);
				if (ticks == 1 || ticks == 21 || ticks == 41) {
					int n = 3 - (ticks - 1) / 20;
					title(Component.literal(String.valueOf(n)).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), Component.literal(subtitle()), 0, 20, 2);
					sound(SoundEvents.NOTE_BLOCK_PLING.value(), 0.6F + 0.2F * (3 - n));
				}
				if (ticks >= 60) {
					title(Component.literal("FIGHT!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), Component.empty(), 0, 20, 10);
					sound(SoundEvents.PLAYER_LEVELUP, 1.4F);
					cheer(Payloads.Cheer.FIGHT, 1.0F);
					freezeEnemies(false);
					phase = Phase.FIGHT;
					ticks = 0;
					state(true);
				}
			}
			case FIGHT -> {
				keepSurvival();
				// Somehow out of the arena (another mod's teleport, a command): that ends it.
				if (player.level() != arena) {
					forfeit = true;
					end(false, "You left the arena");
					leave();
				} else {
					fight(arena);
				}
			}
			case RESULT -> {
				// The player picks: go again, change the settings, or leave (see Matches.start). If they don't
				// within five minutes, or they've gone, home they go.
				keepSurvival();
				if (ticks >= 6000 || player.level() != arena) leave();
			}
			case LEAVING -> {
				if (ticks == 12) {
					clearArena(arena);
					Loadout.restore(player);
					state(false);
					bar.removeAllPlayers();
				}
				if (ticks >= 14) {
					phase = Phase.DONE;
					// Bosses and mob fights can leave scorch marks and holes: build it again, ready for next time.
					if (settings.opponents() != MatchSettings.Opponents.FIGHTERS) Matches.builder(arena, centreX);
				}
			}
			default -> {
			}
		}
	}

	private String subtitle() {
		String who = switch (settings.opponents()) {
			case FIGHTERS -> settings.count() + (settings.count() == 1 ? " fighter" : " fighters");
			case MOBS -> settings.count() + " mobs";
			case BOSS -> settings.count() + " × " + bossName();
		};
		return who + " · level " + settings.difficulty();
	}

	String bossName() {
		return switch (settings.boss()) {
			case WITHER -> "Wither";
			case WARDEN -> "Warden";
			case ENDER_DRAGON -> "Ender Dragon";
		};
	}

	/** Into the arena (or back onto the pedestal), in Survival, healed, fed and kitted out. */
	private void arm(ServerLevel arena) {
		teleportIn(arena);
		try {
			player.setGameMode(GameType.SURVIVAL);
			player.removeAllEffects();
			player.setHealth(player.getMaxHealth());
			player.getFoodData().setFoodLevel(20);
			player.getFoodData().setSaturation(10);
			player.setRemainingFireTicks(0);
			settings.kit().give(player);
		} catch (RuntimeException e) {
			LOGGER.error("Orbit Arena: couldn't kit out {}", player.getName().getString(), e);
		}
		state(true);
	}

	private void teleportIn(ServerLevel arena) {
		Vec3 s = start();
		player.teleportTo(arena, s.x, s.y, s.z, java.util.Set.of(), -90, 0, true);
		player.setDeltaMovement(Vec3.ZERO);
		player.fallDistance = 0;
	}

	private void countdown() {
		phase = Phase.COUNTDOWN;
		ticks = 0;
		spawnUpTo(Math.min(maxAlive(), settings.count()));
		freezeEnemies(true);
		bar.addPlayer(player);
	}

	/** Fights are in Survival: if anything switches the player out of it, they're switched back. */
	private void keepSurvival() {
		if (player.gameMode.getGameModeForPlayer() != GameType.SURVIVAL) player.setGameMode(GameType.SURVIVAL);
	}

	private void enter() {
		phase = Phase.ENTERING;
		ticks = 0;
		ServerPlayNetworking.send(player, new Payloads.Fade(12, 6, 20));
		player.level().playSound(null, player.blockPosition(), SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.25F, 1.6F);
	}

	private void fight(ServerLevel arena) {
		fightTicks++;
		alive.removeIf(e -> !e.isAlive() || e.isRemoved());
		// Top up the enemies as they fall, until all of them have come.
		if (alive.size() < maxAlive() && spawned < settings.count() && fightTicks % 10 == 0) spawnUpTo(Math.min(maxAlive(), alive.size() + 2));
		for (LivingEntity e : alive) {
			if (e instanceof Warden w) w.increaseAngerAt(player, 150, false);
			else if (e instanceof Mob m && !(e instanceof EnderDragon) && m.getTarget() != player) m.setTarget(player);
			if (!(e instanceof EnderDragon)) Matches.keepInside(e, centreX);
		}
		Matches.keepInside(player, centreX);
		if (fightTicks % 20 == 0) com.insidethecube.orbitarena.entity.Fighter.clearDrops(arena, box());
		int left = settings.count() - kills;
		float progress = settings.count() == 0 ? 0 : left / (float) settings.count();
		bar.setProgress(Math.max(0, Math.min(1, progress)));
		String time = clock(fightTicks / 20);
		if (settings.timeLimit() > 0) time = clock(Math.max(0, settings.timeLimit() - fightTicks / 20)) + " left";
		bar.setName(Component.literal(left + " to beat · " + time));
		if (kills >= settings.count()) end(true, "All beaten");
		else if (settings.timeLimit() > 0 && fightTicks >= settings.timeLimit() * 20) end(false, "Time's up");
	}

	static String clock(int s) {
		return s / 60 + ":" + String.format("%02d", s % 60);
	}

	private void spawnUpTo(int target) {
		while (alive.size() < target && spawned < settings.count()) {
			LivingEntity e = Spawns.spawn(this, arena(), spawned);
			spawned++;
			if (e == null) continue;
			e.addTag(ENEMY_TAG);
			alive.add(e);
		}
	}

	private void freezeEnemies(boolean frozen) {
		for (LivingEntity e : alive) if (e instanceof Mob m) m.setNoAi(frozen);
	}

	/** Holds the player on the pedestal (during the countdown, and while the arena's put right to go again). */
	private void hold(ServerLevel arena) {
		Vec3 s = start();
		if (player.level() != arena) teleportIn(arena);
		else if (player.position().distanceToSqr(s) > 0.6) player.teleportTo(s.x, s.y, s.z);
		player.fallDistance = 0;
	}

	void onEnemyHurt(float amount) {
		dealt += amount;
		cheer(Payloads.Cheer.HIT, Math.min(1, amount / 8));
	}

	void onEnemyDeath(LivingEntity e) {
		if (!alive.remove(e)) return;
		kills++;
		cheer(Payloads.Cheer.KILL, kills >= settings.count() ? 1 : 0.7F);
	}

	void onPlayerHurt(float amount) {
		taken += amount;
		if (amount >= 4) cheer(Payloads.Cheer.HURT, Math.min(1, amount / 10));
	}

	/** The player would have died: it's a loss instead. */
	void onPlayerDown() {
		player.setHealth(player.getMaxHealth());
		player.removeAllEffects();
		end(false, "You were beaten");
	}

	void end(boolean won, String reason) {
		if (phase.ordinal() >= Phase.RESULT.ordinal()) return;
		boolean fighting = phase == Phase.FIGHT;
		this.won = won;
		this.endReason = reason;
		phase = Phase.RESULT;
		ticks = 0;
		freezeEnemies(true);
		if (!fighting) {
			// Never got going (left while it was building or counting down): straight home.
			if (Loadout.has(player)) leave();
			else phase = Phase.DONE;
			return;
		}
		int secs = fightTicks / 20;
		title(Component.literal(won ? "VICTORY" : forfeit ? "LEFT THE ARENA" : "DEFEAT").withStyle(won ? ChatFormatting.GOLD : ChatFormatting.RED, ChatFormatting.BOLD),
				Component.literal(reason + " · " + clock(secs)), 5, 70, 20);
		sound(won ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE : SoundEvents.BEACON_DEACTIVATE, 1.0F);
		cheer(won ? Payloads.Cheer.WIN : Payloads.Cheer.LOSS, 1.0F);
		player.sendSystemMessage(Component.literal("Orbit Arena · " + (won ? "Victory" : "Defeat") + " · " + subtitle()).withStyle(won ? ChatFormatting.GOLD : ChatFormatting.RED));
		String summary = String.format("Time %s · Beaten %d of %d · Damage dealt %.0f · Damage taken %.0f", clock(secs), kills, settings.count(), dealt, taken);
		player.sendSystemMessage(Component.literal(" " + summary).withStyle(ChatFormatting.GRAY));
		state(true);
		// Go again, change the settings, or leave: the player's choice (not when they chose to leave).
		if (!forfeit) ServerPlayNetworking.send(player, new Payloads.Result(won, reason, summary));
	}

	/** Making way for another go: the enemies cleared, this match's bar gone. */
	void retire() {
		ServerLevel arena = arena();
		if (arena != null) clearArena(arena);
		bar.removeAllPlayers();
		phase = Phase.DONE;
	}

	/** Fade out and go home (from the result, or straight away when leaving). */
	void leave() {
		if (phase == Phase.LEAVING || phase == Phase.DONE) return;
		phase = Phase.LEAVING;
		ticks = 0;
		ServerPlayNetworking.send(player, new Payloads.Fade(12, 6, 20));
	}

	/** When the player has gone (logged off): tidy up; their things come back when they're next in. */
	void abandon() {
		ServerLevel arena = arena();
		if (arena != null) clearArena(arena);
		bar.removeAllPlayers();
		phase = Phase.DONE;
	}

	void clearArena(ServerLevel arena) {
		for (Entity e : arena.getEntities((Entity) null, box().inflate(40), e -> !(e instanceof Player))) e.discard();
		alive.clear();
	}

	boolean done() {
		return phase == Phase.DONE;
	}

	boolean fighting() {
		return phase == Phase.FIGHT;
	}

	boolean inArena() {
		return phase.ordinal() >= Phase.COUNTDOWN.ordinal() || (phase == Phase.ENTERING && ticks >= 12) || (again && phase == Phase.PREPARING);
	}

	private void state(boolean in) {
		ServerPlayNetworking.send(player, new Payloads.State(in, centreX, phase == Phase.FIGHT || phase == Phase.COUNTDOWN));
	}

	private void cheer(int kind, float strength) {
		ServerPlayNetworking.send(player, new Payloads.Cheer(kind, strength));
	}

	private void title(Component title, Component sub, int in, int stay, int out) {
		player.connection.send(new ClientboundSetTitlesAnimationPacket(in, stay, out));
		player.connection.send(new ClientboundSetSubtitleTextPacket(sub));
		player.connection.send(new ClientboundSetTitleTextPacket(title));
	}

	private void sound(SoundEvent sound, float pitch) {
		player.level().playSound(null, BlockPos.containing(player.position()), sound, SoundSource.PLAYERS, 1.0F, pitch);
	}
}
