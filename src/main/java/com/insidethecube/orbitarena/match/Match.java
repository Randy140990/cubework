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
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
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
 * One player's match in the stadium: getting the arena ready, the fade in, the fly-round (if it's on),
 * "3, 2, 1, Fight!", the fight (fighters and bosses topped up until all are beaten; mobs in waves, the next
 * coming in as the last few of one are left), the result, and then the player's choice: go again (straight
 * back onto the pedestal, the arena put right first if a mob or boss fight damaged it), change the settings,
 * or fade back home with everything returned. A win is celebrated for ten seconds first: the crowd goes
 * wild, fireworks go up, and you see yourself celebrate.
 * <p>
 * The fight starts in Survival. Switching to Creative (Axiom's editor) is allowed: you can still be hurt,
 * and everything still comes for you (see mixin.PlayerMixin).
 */
public final class Match implements Game {
	private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

	enum Phase {
		PREPARING, ENTERING, INTRO, COUNTDOWN, FIGHT, RESULT, LEAVING, DONE
	}

	public static final String ENEMY_TAG = "orbitarena_enemy";
	/** How long the fly-round lasts, and how long a win is celebrated before the choice comes up (ticks). */
	static final int INTRO_TICKS = 200, CELEBRATE_TICKS = 200;

	final ServerPlayer player;
	final MatchSettings settings;
	final int slot, centreX;
	/** Another go straight after the last match: the player is already in the arena, their things put away. */
	final boolean again;
	Phase phase = Phase.PREPARING;
	int ticks, fightTicks;
	final List<LivingEntity> alive = new ArrayList<>();
	int spawned, kills;
	/** Mob waves: the wave on now (from 1), and how many of it are still to come out. */
	int wave, waveLeft;
	float dealt, taken;
	boolean won, forfeit;
	String endReason = "";
	private String summary = "";
	private final ServerBossEvent bar = new ServerBossEvent(Component.literal("Orbit Arena"), BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.PROGRESS);

	Match(ServerPlayer player, MatchSettings settings, int slot, boolean again) {
		this.player = player;
		this.settings = settings;
		this.slot = slot;
		this.centreX = slot * SPACING;
		this.again = again;
	}

	@Override
	public ServerPlayer player() {
		return player;
	}

	@Override
	public MatchSettings settings() {
		return settings;
	}

	@Override
	public int slot() {
		return slot;
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

	boolean mobs() {
		return settings.opponents() == MatchSettings.Opponents.MOBS;
	}

	/** How many there are to beat in all (for mob waves, every wave's). */
	int total() {
		if (!mobs()) return settings.count();
		int n = 0;
		for (int w = 1; w <= settings.count(); w++) n += waveSize(w);
		return n;
	}

	/** Each wave a little bigger than the last, and bigger at higher levels. */
	int waveSize(int w) {
		return Math.min(30, 3 + w + settings.difficulty() / 3);
	}

	int maxAlive() {
		int d = settings.difficulty();
		return switch (settings.opponents()) {
			case FIGHTERS -> Math.min(settings.count(), Math.min(30, 4 + 2 * d));
			case MOBS -> Math.min(40, 4 + 3 * d);
			default -> settings.count();
		};
	}

	@Override
	public void tick() {
		ticks++;
		ServerLevel arena = arena();
		if (arena == null) return;
		switch (phase) {
			case PREPARING -> {
				// Going again: wait on the pedestal while the arena's put right.
				if (again) hold(arena);
				// Wait until the arena is built and nothing is building or repairing it (stepping in while it's
				// being rebuilt could leave you with no floor).
				Matches.Builder running = Matches.building(Matches.key(centreX, 0));
				if (running != null) {
					player.displayClientMessage(Component.literal((again ? "Putting the arena right… " : "Building the arena… ") + (int) (running.progress() * 100) + "%")
							.withStyle(ChatFormatting.AQUA), true);
				} else if (!ArenaBuilder.built(arena, centreX)) {
					Matches.builder(arena, centreX);
				} else if (again) {
					player.getInventory().clearContent();
					arm(arena);
					ready(false);
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
						ready(settings.intro());
					} else if (ticks > 400) {
						LOGGER.warn("Orbit Arena: couldn't get {} into the arena", player.getName().getString());
						player.displayClientMessage(Component.literal("Couldn't get you into the arena. Try again.").withStyle(ChatFormatting.RED), false);
						phase = Phase.RESULT;
						leave();
					}
				}
			}
			case INTRO -> {
				hold(arena);
				if (ticks >= INTRO_TICKS) countdown();
			}
			case COUNTDOWN -> {
				hold(arena);
				if (ticks == 1 || ticks == 21 || ticks == 41) {
					int n = 3 - (ticks - 1) / 20;
					Shows.title(player, Component.literal(String.valueOf(n)).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), Component.literal(subtitle()), 0, 20, 2);
					Shows.sound(player, SoundEvents.NOTE_BLOCK_PLING.value(), 1.0F, 0.6F + 0.2F * (3 - n));
				}
				if (ticks >= 60) {
					Shows.title(player, Component.literal("FIGHT!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), Component.empty(), 0, 20, 10);
					Shows.sound(player, SoundEvents.PLAYER_LEVELUP, 1.0F, 1.4F);
					Shows.cheer(player, Payloads.Cheer.FIGHT, 1.0F);
					freezeEnemies(false);
					phase = Phase.FIGHT;
					ticks = 0;
					state(true);
				}
			}
			case FIGHT -> {
				// Somehow out of the arena (another mod's teleport, a command): that ends it.
				if (player.level() != arena) {
					forfeit = true;
					end(false, "You left the arena");
					leave();
				} else {
					creativeFair();
					fight(arena);
				}
			}
			case RESULT -> {
				// A win: ten seconds of fireworks and cheering, then the choice. The player picks: go again, change
				// the settings, or leave (see Matches.start). If they don't within five minutes, or they've gone,
				// home they go.
				if (won && ticks < CELEBRATE_TICKS - 20 && ticks % 7 == 0) {
					double a = arena.random.nextDouble() * Math.PI * 2, r = 20 + arena.random.nextDouble() * 26;
					Shows.firework(arena, centreX + Math.cos(a) * r, FLOOR + 2, Math.sin(a) * r, -1, arena.random);
				}
				if (ticks == (won ? CELEBRATE_TICKS : 30) && !forfeit) ServerPlayNetworking.send(player, new Payloads.Result(won, endReason, summary));
				if (ticks >= 6000 + CELEBRATE_TICKS || player.level() != arena) leave();
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

	/**
	 * In Creative mid-fight (Axiom's editor, say), the fight stays fair: Creative would make you unhurtable
	 * and invisible to everything, so you're made hurtable again (and mixin.PlayerMixin keeps you visible).
	 */
	private void creativeFair() {
		if (player.gameMode.getGameModeForPlayer() == GameType.CREATIVE && player.getAbilities().invulnerable) {
			player.getAbilities().invulnerable = false;
		}
	}

	private String subtitle() {
		String who = switch (settings.opponents()) {
			case FIGHTERS -> settings.count() + (settings.count() == 1 ? " fighter" : " fighters");
			case MOBS -> settings.count() + (settings.count() == 1 ? " wave" : " waves") + " of mobs";
			default -> settings.count() + " × " + bossName();
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

	/** The first enemies out (held still), the bar up, then the fly-round or straight to the countdown. */
	private void ready(boolean intro) {
		if (mobs()) startWave(1, false);
		spawnUpTo(Math.min(maxAlive(), total()));
		freezeEnemies(true);
		bar.addPlayer(player);
		if (intro) {
			phase = Phase.INTRO;
			ticks = 0;
			ServerPlayNetworking.send(player, new Payloads.Cinematic(Payloads.Cinematic.ARENA, centreX, 0, INTRO_TICKS, "ORBIT ARENA", subtitle()));
		} else {
			countdown();
		}
	}

	private void countdown() {
		phase = Phase.COUNTDOWN;
		ticks = 0;
	}

	private void enter() {
		phase = Phase.ENTERING;
		ticks = 0;
		ServerPlayNetworking.send(player, new Payloads.Fade(12, 6, 20));
		player.level().playSound(null, player.blockPosition(), SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.25F, 1.6F);
	}

	private void startWave(int n, boolean announce) {
		wave = n;
		waveLeft = waveSize(n);
		if (announce) {
			Shows.title(player, Component.literal("WAVE " + n).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
					Component.literal(n == settings.count() ? "The last one" : "of " + settings.count()), 0, 30, 10);
			Shows.sound(player, SoundEvents.RAID_HORN.value(), 0.6F, 1.2F);
			Shows.cheer(player, Payloads.Cheer.KILL, 0.8F);
		}
	}

	private void fight(ServerLevel arena) {
		fightTicks++;
		alive.removeIf(e -> !e.isAlive() || e.isRemoved());
		if (mobs()) {
			// Waves without a break: the next comes out while the last few of this one are still fighting.
			if (waveLeft == 0 && alive.size() <= 2 && wave < settings.count()) startWave(wave + 1, true);
			if (waveLeft > 0 && alive.size() < maxAlive() && fightTicks % 8 == 0) spawnUpTo(Math.min(maxAlive(), alive.size() + 2));
		} else if (alive.size() < maxAlive() && spawned < total() && fightTicks % 10 == 0) {
			// Top up the enemies as they fall, until all of them have come.
			spawnUpTo(Math.min(maxAlive(), alive.size() + 2));
		}
		for (LivingEntity e : alive) {
			if (e instanceof Warden w) w.increaseAngerAt(player, 150, false);
			else if (e instanceof Mob m && !(e instanceof EnderDragon) && m.getTarget() != player) m.setTarget(player);
			if (!(e instanceof EnderDragon)) Matches.keepInside(e, centreX);
		}
		Matches.keepInside(player, centreX);
		if (fightTicks % 20 == 0) com.insidethecube.orbitarena.entity.Fighter.clearDrops(arena, box());
		int total = total(), left = total - kills;
		bar.setProgress(Math.max(0, Math.min(1, total == 0 ? 0 : left / (float) total)));
		String time = clock(fightTicks / 20);
		if (settings.timeLimit() > 0) time = clock(Math.max(0, settings.timeLimit() - fightTicks / 20)) + " left";
		if (mobs()) bar.setName(Component.literal("Wave " + wave + " of " + settings.count() + " · " + (alive.size() + waveLeft) + " to beat · " + time));
		else bar.setName(Component.literal(left + " to beat · " + time));
		if (kills >= total) end(true, mobs() ? "Every wave beaten" : "All beaten");
		else if (settings.timeLimit() > 0 && fightTicks >= settings.timeLimit() * 20) end(false, "Time's up");
	}

	static String clock(int s) {
		return s / 60 + ":" + String.format("%02d", s % 60);
	}

	private void spawnUpTo(int target) {
		while (alive.size() < target && spawned < total() && (!mobs() || waveLeft > 0)) {
			LivingEntity e = Spawns.spawn(this, arena(), spawned);
			spawned++;
			if (mobs()) waveLeft--;
			if (e == null) {
				kills++;
				continue;
			}
			e.addTag(ENEMY_TAG);
			alive.add(e);
		}
	}

	private void freezeEnemies(boolean frozen) {
		for (LivingEntity e : alive) if (e instanceof Mob m) m.setNoAi(frozen);
	}

	/** Holds the player on the pedestal (during the fly-round and countdown, and while the arena's put right). */
	private void hold(ServerLevel arena) {
		Vec3 s = start();
		if (player.level() != arena) teleportIn(arena);
		else if (player.position().distanceToSqr(s) > 0.6) player.teleportTo(s.x, s.y, s.z);
		player.fallDistance = 0;
	}

	// ---------------------------------------------------------------- who hurts whom

	@Override
	public boolean allowDamage(LivingEntity victim, DamageSource source, float amount) {
		// Enemies never hurt each other.
		if (victim.getTags().contains(ENEMY_TAG) && source.getEntity() != null && source.getEntity().getTags().contains(ENEMY_TAG)) return false;
		// Nothing hurts the player outside a fight (the fly-round, the countdown, the result).
		if (victim == player) return fighting();
		return true;
	}

	@Override
	public void afterDamage(LivingEntity victim, DamageSource source, float amount) {
		if (!fighting()) return;
		if (victim == player) {
			taken += amount;
			if (amount >= 4) Shows.cheer(player, Payloads.Cheer.HURT, Math.min(1, amount / 10));
		} else if (victim.getTags().contains(ENEMY_TAG) && source.getEntity() == player) {
			dealt += amount;
			Shows.cheer(player, Payloads.Cheer.HIT, Math.min(1, amount / 8));
		}
	}

	@Override
	public boolean allowDeath(LivingEntity victim, DamageSource source) {
		if (victim != player || !inArena()) return true;
		// The player would have died: it's a loss instead.
		player.setHealth(player.getMaxHealth());
		player.removeAllEffects();
		end(false, "You were beaten");
		return false;
	}

	@Override
	public void onDeath(LivingEntity e) {
		if (!alive.remove(e)) return;
		kills++;
		Shows.cheer(player, Payloads.Cheer.KILL, kills >= total() ? 1 : 0.7F);
	}

	@Override
	public boolean mayBreak(BlockPos pos) {
		return false;
	}

	@Override
	public boolean mayPlace(BlockPos pos) {
		return false;
	}

	@Override
	public boolean owns(BlockPos pos) {
		return Math.abs(pos.getX() - centreX) < SIZE + 120 && Math.abs(pos.getZ()) < SIZE + 120;
	}

	// ---------------------------------------------------------------- the end

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
		Shows.title(player, Component.literal(won ? "VICTORY" : forfeit ? "LEFT THE ARENA" : "DEFEAT").withStyle(won ? ChatFormatting.GOLD : ChatFormatting.RED, ChatFormatting.BOLD),
				Component.literal(reason + " · " + clock(secs)), 5, 70, 20);
		Shows.sound(player, won ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE : SoundEvents.BEACON_DEACTIVATE, 1.0F, 1.0F);
		Shows.cheer(player, won ? Payloads.Cheer.WIN : Payloads.Cheer.LOSS, 1.0F);
		player.sendSystemMessage(Component.literal("Orbit Arena · " + (won ? "Victory" : "Defeat") + " · " + subtitle()).withStyle(won ? ChatFormatting.GOLD : ChatFormatting.RED));
		String what = mobs() ? String.format("Waves %d of %d · Beaten %d", Math.max(0, won ? wave : wave - 1), settings.count(), kills)
				: String.format("Beaten %d of %d", kills, total());
		summary = String.format("Time %s · %s · Damage dealt %.0f · Damage taken %.0f", clock(secs), what, dealt, taken);
		player.sendSystemMessage(Component.literal(" " + summary).withStyle(ChatFormatting.GRAY));
		state(true);
		// A win is celebrated: you see yourself cheer (third person) while the fireworks go up.
		if (won && !forfeit) ServerPlayNetworking.send(player, new Payloads.Celebrate(CELEBRATE_TICKS));
	}

	@Override
	public void retire() {
		ServerLevel arena = arena();
		if (arena != null) clearArena(arena);
		bar.removeAllPlayers();
		phase = Phase.DONE;
		// Mob and boss fights can damage the arena: put it right before the next go.
		if (arena != null && settings.opponents() != MatchSettings.Opponents.FIGHTERS) Matches.builder(arena, centreX);
	}

	@Override
	public void forfeit() {
		forfeit = true;
		if (phase != Phase.RESULT) end(false, "You left");
		leave();
	}

	/** Fade out and go home (from the result, or straight away when leaving). */
	@Override
	public void leave() {
		if (phase == Phase.LEAVING || phase == Phase.DONE) return;
		phase = Phase.LEAVING;
		ticks = 0;
		ServerPlayNetworking.send(player, new Payloads.Fade(12, 6, 20));
	}

	@Override
	public void abandon() {
		ServerLevel arena = arena();
		if (arena != null) clearArena(arena);
		bar.removeAllPlayers();
		phase = Phase.DONE;
	}

	void clearArena(ServerLevel arena) {
		for (Entity e : arena.getEntities((Entity) null, box().inflate(40), e -> !(e instanceof Player))) e.discard();
		alive.clear();
	}

	@Override
	public boolean done() {
		return phase == Phase.DONE;
	}

	@Override
	public boolean fighting() {
		return phase == Phase.FIGHT;
	}

	@Override
	public boolean awaitingChoice() {
		return phase == Phase.RESULT;
	}

	@Override
	public boolean inArena() {
		return phase.ordinal() >= Phase.INTRO.ordinal() || (phase == Phase.ENTERING && ticks >= 12) || (again && phase == Phase.PREPARING);
	}

	private void state(boolean in) {
		ServerPlayNetworking.send(player, new Payloads.State(in, Payloads.State.ARENA, centreX, 0, phase == Phase.FIGHT || phase == Phase.COUNTDOWN));
	}
}
