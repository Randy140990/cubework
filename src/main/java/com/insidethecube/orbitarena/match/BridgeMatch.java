package com.insidethecube.orbitarena.match;

import static com.insidethecube.orbitarena.arena.BridgeLayout.*;

import java.util.ArrayList;
import java.util.List;

import com.insidethecube.orbitarena.OrbitArena;
import com.insidethecube.orbitarena.arena.BridgeBuilder;
import com.insidethecube.orbitarena.arena.BridgeLayout;
import com.insidethecube.orbitarena.entity.BridgeGoal;
import com.insidethecube.orbitarena.entity.BridgeWorld;
import com.insidethecube.orbitarena.entity.Fighter;
import com.insidethecube.orbitarena.net.Payloads;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Unit;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A game of The Bridge: you and up to four teammates (blue, west) against up to five (red, east), the rest
 * of both teams played by bots. Everyone starts in their team's cage over their base; the cages open after
 * five seconds and you cross the bridge, build round anyone in the way, knock them into space and drop into
 * their goal. A goal sends everyone back to the cages and crumbles one of the other team's pillars (its
 * crystal shatters). First to five wins; if there's a time limit, the most goals when it runs out.
 * <p>
 * Your kit (as on the bridge servers): leather armour in your team's colour (or netherite), a sword, a bow
 * with one arrow that comes back a few seconds after you shoot it, a diamond pickaxe, two stacks of your
 * team's blocks and eight golden apples, all given again whenever you come back. You can place blocks over
 * the void and on the bridge, but not in or over a goal, a cage or the pillars, and you can only break
 * blocks someone placed. Falling into space, or going down, sends you back to your base. No fall damage.
 */
public final class BridgeMatch implements Game, BridgeWorld {
	private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

	enum Phase {
		PREPARING, ENTERING, INTRO, CAGED, PLAY, RESULT, LEAVING, DONE
	}

	static final int TO_WIN = 5, CAGE_TICKS = 100, RESPAWN_TICKS = 30;
	static final String TEAM_TAG = "orbitarena_team_";
	private static final int BLUE_RGB = 0x3C44AA, RED_RGB = 0xB02E26;
	private static final String[] NAMES = {"Nova", "Orion", "Vega", "Comet", "Lyra", "Rigel", "Atlas", "Juno", "Cosmo", "Pulsar", "Sirius", "Tycho"};

	/** Someone playing: the player, or a bot. */
	static final class Member {
		final LivingEntity e;
		final int team;
		final String name;
		/** Ticks until it comes back (after falling or going down), or -1 when it's in play. */
		int respawn = -1;
		Entity lastHitBy;
		long lastHitAt;
		int goals, kills, deaths, noArrow;

		Member(LivingEntity e, int team, String name) {
			this.e = e;
			this.team = team;
			this.name = name;
		}

		boolean inPlay() {
			return respawn < 0 && e.isAlive() && !e.isRemoved();
		}
	}

	/** A pillar crumbling, a layer at a time from the top. */
	private static final class Crumble {
		final int team, pillar;
		int layer = PILLAR_TOP, wait;

		Crumble(int team, int pillar) {
			this.team = team;
			this.pillar = pillar;
		}
	}

	final ServerPlayer player;
	final MatchSettings settings;
	final int slot, cx, cz = Z;
	final boolean again;
	Phase phase = Phase.PREPARING;
	int ticks, playTicks;
	final int[] score = new int[2];
	final List<Member> members = new ArrayList<>();
	private final List<Crumble> crumbles = new ArrayList<>();
	private boolean repairStarted, cagesUp;
	boolean won, draw, forfeit;
	String endReason = "";
	private String summary = "";

	BridgeMatch(ServerPlayer player, MatchSettings settings, int slot, boolean again) {
		this.player = player;
		this.settings = settings;
		this.slot = slot;
		this.cx = slot * com.insidethecube.orbitarena.arena.ArenaLayout.SPACING;
		this.again = again;
	}

	// ---------------------------------------------------------------- Game

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

	@Override
	public ServerLevel level() {
		return arena();
	}

	@Override
	public int centreX() {
		return cx;
	}

	@Override
	public int centreZ() {
		return cz;
	}

	@Override
	public int y() {
		return Y;
	}

	@Override
	public boolean playing() {
		return phase == Phase.PLAY;
	}

	@Override
	public int teamSize() {
		return settings.count();
	}

	@Override
	public List<LivingEntity> enemiesOf(int team) {
		List<LivingEntity> out = new ArrayList<>();
		for (Member m : members) if (m.team != team && m.inPlay()) out.add(m.e);
		return out;
	}

	@Override
	public List<LivingEntity> alliesOf(int team) {
		List<LivingEntity> out = new ArrayList<>();
		for (Member m : members) if (m.team == team && m.inPlay()) out.add(m.e);
		return out;
	}

	@Override
	public boolean buildable(BlockPos pos) {
		int dx = pos.getX() - cx, dy = pos.getY() - Y, dz = pos.getZ() - cz;
		return BridgeLayout.buildable(dx, dy, dz) && mapBlock(dx, dy, dz, settings.obstacles()) == null;
	}

	@Override
	public boolean breakable(BlockPos pos) {
		ServerLevel level = arena();
		return level != null && buildable(pos) && !level.getBlockState(pos).isAir();
	}

	@Override
	public BlockState blockFor(int team) {
		return team == BLUE ? Blocks.BLUE_TERRACOTTA.defaultBlockState() : Blocks.RED_TERRACOTTA.defaultBlockState();
	}

	boolean involves(LivingEntity e) {
		for (Member m : members) if (m.e == e) return true;
		return false;
	}

	private Member memberOf(Entity e) {
		if (e == null) return null;
		for (Member m : members) if (m.e == e) return m;
		return null;
	}

	@Override
	public boolean mayBreak(BlockPos pos) {
		return playing() && breakable(pos);
	}

	@Override
	public boolean mayPlace(BlockPos pos) {
		return playing() && buildable(pos);
	}

	@Override
	public boolean owns(BlockPos pos) {
		return pos.getX() - cx > -SIZE && pos.getX() - cx < SIZE && Math.abs(pos.getZ() - cz) < SIZE;
	}

	// ---------------------------------------------------------------- the game

	private Vec3 spawn(int team) {
		return new Vec3(cx + side(team) * SPAWN_X + 0.5, Y + 1, cz + 0.5);
	}

	/** Facing the bridge from a team's base. */
	private static float yaw(int team) {
		return team == BLUE ? -90 : 90;
	}

	@Override
	public void tick() {
		ticks++;
		ServerLevel arena = arena();
		if (arena == null) return;
		tickCrumbles(arena);
		switch (phase) {
			case PREPARING -> {
				if (again) holdAt(arena, spawn(BLUE));
				Matches.Builder running = Matches.building(Matches.key(cx, Z));
				if (running != null) {
					player.displayClientMessage(Component.literal((BridgeBuilder.built(arena, cx) ? "Getting the bridge ready… " : "Building the bridge… ")
							+ (int) (running.progress() * 100) + "%").withStyle(ChatFormatting.AQUA), true);
				} else if (!repairStarted || !BridgeBuilder.built(arena, cx) || BridgeBuilder.hasObstacles(arena, cx) != settings.obstacles()) {
					// Built afresh, or put right from the last game (blocks cleared, pillars stood back up).
					repairStarted = true;
					Matches.bridgeBuilder(arena, cx, false, settings.obstacles());
				} else if (again) {
					player.getInventory().clearContent();
					kit(player, BLUE);
					setup(arena, false);
				} else {
					phase = Phase.ENTERING;
					ticks = 0;
					ServerPlayNetworking.send(player, new Payloads.Fade(12, 6, 20));
					player.level().playSound(null, player.blockPosition(), SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.25F, 1.6F);
				}
			}
			case ENTERING -> {
				if (ticks == 12) {
					try {
						Loadout.store(player);
					} catch (RuntimeException e) {
						LOGGER.error("Orbit Arena: couldn't put {}'s things away", player.getName().getString(), e);
					}
					teleport(player, arena, cageSpot(BLUE, 0, 1), yaw(BLUE));
					kit(player, BLUE);
					state(true);
				}
				if (ticks > 12 && player.level() != arena && ticks % 10 == 0) teleport(player, arena, cageSpot(BLUE, 0, 1), yaw(BLUE));
				if (ticks >= 30) {
					if (player.level() == arena) {
						setup(arena, settings.intro());
					} else if (ticks > 400) {
						player.displayClientMessage(Component.literal("Couldn't get you to the bridge. Try again.").withStyle(ChatFormatting.RED), false);
						phase = Phase.RESULT;
						leave();
					}
				}
			}
			case INTRO -> {
				holdCaged();
				if (ticks % 10 == 0) score();
				if (ticks >= Match.INTRO_TICKS) caged();
			}
			case CAGED -> {
				holdCaged();
				int left = (CAGE_TICKS - ticks) / 20;
				if (ticks % 20 == 0 && left > 0) {
					Shows.title(player, Component.literal(String.valueOf(left)).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
							Component.literal("Cages open in " + left), 0, 22, 2);
					Shows.sound(player, SoundEvents.NOTE_BLOCK_PLING.value(), 0.8F, 0.6F + 0.15F * (5 - left));
				}
				if (ticks >= CAGE_TICKS) open(arena);
			}
			case PLAY -> {
				playTicks++;
				play(arena);
			}
			case RESULT -> {
				if (won && ticks < Match.CELEBRATE_TICKS - 20 && ticks % 6 == 0) {
					double x = cx + (arena.random.nextDouble() - 0.5) * 80, z = cz + (arena.random.nextBoolean() ? 1 : -1) * (6 + arena.random.nextDouble() * 10);
					Shows.firework(arena, x, Y - 4, z, BLUE_RGB, arena.random);
				}
				if (ticks == (won ? Match.CELEBRATE_TICKS : 30) && !forfeit) ServerPlayNetworking.send(player, new Payloads.Result(won, endReason, summary));
				if (ticks >= 6000 + Match.CELEBRATE_TICKS || player.level() != arena) leave();
			}
			case LEAVING -> {
				if (ticks == 12) {
					clear(arena);
					Loadout.restore(player);
					state(false);
				}
				if (ticks >= 14) phase = Phase.DONE;
			}
			default -> {
			}
		}
	}

	/** Bots for both teams (you're the first of blue), everyone in their cage, then the fly-round or the countdown. */
	private void setup(ServerLevel arena, boolean intro) {
		clear(arena);
		members.clear();
		members.add(new Member(player, BLUE, player.getName().getString()));
		player.addTag(TEAM_TAG + "blue");
		int n = settings.count(), d = settings.difficulty(), name = arena.random.nextInt(NAMES.length);
		for (int team = BLUE; team <= RED; team++) {
			for (int i = team == BLUE ? 1 : 0; i < n; i++) {
				Fighter f = OrbitArena.FIGHTER.create(arena, EntitySpawnReason.MOB_SUMMONED);
				if (f == null) continue;
				String who = NAMES[name++ % NAMES.length];
				ItemStack sword = item(settings.bridgeKit() == MatchSettings.NETHERITE ? Items.NETHERITE_SWORD : Items.IRON_SWORD);
				f.setupBridge(new BridgeGoal.Brain(this, team, i, d), sword, d >= 3 ? 8 : 0, arena.random.nextInt(8));
				armour(f, team);
				for (EquipmentSlot slot : EquipmentSlot.values()) f.setDropChance(slot, 0);
				f.setCustomName(Component.literal(who).withStyle(team == BLUE ? ChatFormatting.BLUE : ChatFormatting.RED));
				f.setCustomNameVisible(true);
				f.setPersistenceRequired();
				f.addTag(TEAM_TAG + (team == BLUE ? "blue" : "red"));
				Vec3 at = cageSpot(team, i, n);
				f.snapTo(at.x, at.y, at.z, yaw(team), 0);
				f.setNoAi(true);
				arena.addFreshEntity(f);
				members.add(new Member(f, team, who));
			}
		}
		cages(arena, true);
		toCages(arena);
		score();
		if (intro) {
			phase = Phase.INTRO;
			ticks = 0;
			ServerPlayNetworking.send(player, new Payloads.Cinematic(Payloads.Cinematic.BRIDGE, cx, cz, Match.INTRO_TICKS, "THE BRIDGE", subtitle()));
		} else {
			caged();
		}
	}

	private String subtitle() {
		return settings.count() + " v " + settings.count() + " · level " + settings.difficulty() + " · first to " + TO_WIN;
	}

	private void caged() {
		phase = Phase.CAGED;
		ticks = 0;
		score();
	}

	/** Where a member of a team stands in its cage. */
	private Vec3 cageSpot(int team, int index, int n) {
		double off = n <= 1 ? 0 : (index - (n - 1) / 2.0) * 1.5;
		return new Vec3(cx + side(team) * CAGE_X + 0.5, Y + CAGE_FLOOR + 1, cz + 0.5 + off);
	}

	/** Builds both cages (or takes them away). */
	private void cages(ServerLevel arena, boolean up) {
		cagesUp = up;
		for (int s = -1; s <= 1; s += 2) {
			for (int ax = CAGE_X - CAGE_HALF_X - 1; ax <= CAGE_X + CAGE_HALF_X + 1; ax++) {
				for (int dz = -CAGE_HALF_Z - 1; dz <= CAGE_HALF_Z + 1; dz++) {
					for (int dy = CAGE_FLOOR; dy <= CAGE_FLOOR + 4; dy++) {
						BlockState b = cageBlock(s * ax, dy, dz);
						if (b == null) continue;
						BlockPos pos = new BlockPos(cx + s * ax, Y + dy, cz + dz);
						arena.setBlock(pos, up ? b : Blocks.AIR.defaultBlockState(), 3);
					}
				}
			}
		}
	}

	/** Everyone back into their cage, healed, kit given again. */
	private void toCages(ServerLevel arena) {
		int[] count = new int[2];
		for (Member m : members) {
			int i = count[m.team]++;
			m.respawn = -1;
			Vec3 at = cageSpot(m.team, i, settings.count());
			if (m.e instanceof ServerPlayer p) {
				teleport(p, arena, at, yaw(m.team));
				refresh(p);
			} else {
				m.e.snapTo(at.x, at.y, at.z, yaw(m.team), 0);
				m.e.setDeltaMovement(Vec3.ZERO);
				m.e.setHealth(m.e.getMaxHealth());
				m.e.removeAllEffects();
				if (m.e instanceof Fighter f) {
					f.setNoAi(true);
					f.setInvisible(false);
					f.setCustomNameVisible(true);
				}
			}
		}
	}

	/** Keeps bots still in their cages (players are kept in by the glass). */
	private void holdCaged() {
		for (Member m : members) {
			if (m.e instanceof ServerPlayer p) {
				p.fallDistance = 0;
				if (!cagesUp || p.getY() < Y + CAGE_FLOOR - 0.5) {
					Vec3 at = cageSpot(m.team, 0, 1);
					teleport(p, arena(), at, yaw(m.team));
				}
			}
		}
	}

	private void open(ServerLevel arena) {
		cages(arena, false);
		for (Member m : members) if (m.e instanceof Fighter f) f.setNoAi(false);
		Shows.title(player, Component.literal("GO!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), Component.empty(), 0, 16, 8);
		Shows.sound(player, SoundEvents.PLAYER_LEVELUP, 1.0F, 1.4F);
		Shows.cheer(player, Payloads.Cheer.FIGHT, playTicks == 0 ? 1.0F : 0.6F);
		phase = Phase.PLAY;
		ticks = 0;
		state(true);
	}

	private void play(ServerLevel arena) {
		for (Member m : members) {
			LivingEntity e = m.e;
			if (m.respawn >= 0) {
				if (--m.respawn <= 0) back(arena, m);
				continue;
			}
			if (!e.isAlive() || e.isRemoved() || e.level() != arena) {
				if (e == player) {
					forfeit = true;
					end(false, false, "You left the bridge");
					leave();
					return;
				}
				continue;
			}
			double dx = e.getX() - cx, dy = e.getY() - Y, dz = e.getZ() - cz;
			// Into the other team's goal: a goal. Into your own: back to your base.
			if (inGoal(dx, dy, dz, -side(m.team))) {
				goal(arena, m);
				return;
			}
			if (inGoal(dx, dy, dz, side(m.team))) {
				back(arena, m);
				continue;
			}
			if (dy < VOID) {
				down(arena, m, null);
				continue;
			}
			if (e instanceof ServerPlayer p) {
				p.getFoodData().setFoodLevel(20);
				// Your arrow comes back three and a half seconds after you've loosed it.
				if (p.getInventory().countItem(Items.ARROW) == 0 && !p.isUsingItem()) {
					if (++m.noArrow >= 70) {
						p.getInventory().add(new ItemStack(Items.ARROW));
						m.noArrow = 0;
					}
				} else {
					m.noArrow = 0;
				}
				if (p.gameMode.getGameModeForPlayer() == GameType.CREATIVE && p.getAbilities().invulnerable) p.getAbilities().invulnerable = false;
			}
		}
		if (playTicks % 20 == 0) {
			AABB area = new AABB(cx - AREA_X - 30, Y - 40, cz - AREA_Z - 30, cx + AREA_X + 30, Y + 40, cz + AREA_Z + 30);
			for (ItemEntity i : arena.getEntitiesOfClass(ItemEntity.class, area)) i.discard();
			for (AbstractArrow a : arena.getEntitiesOfClass(AbstractArrow.class, area, a -> a.onGround() || a.tickCount > 200)) a.discard();
		}
		if (playTicks % 10 == 0) score();
		if (settings.timeLimit() > 0 && playTicks >= settings.timeLimit() * 20) {
			if (score[BLUE] == score[RED]) end(false, true, "Time's up, level on goals");
			else end(score[BLUE] > score[RED], false, "Time's up");
		}
	}

	/** Out of play (into space, or beaten): back at its base in a moment. */
	private void down(ServerLevel arena, Member m, Entity by) {
		if (by == null && m.lastHitBy != null && arena.getGameTime() - m.lastHitAt < 120) by = m.lastHitBy;
		Member killer = memberOf(by);
		boolean fell = m.e.getY() - Y < VOID;
		String how = fell ? killer != null ? " was knocked into space by " : " fell into space" : " was beaten by ";
		Component msg = Component.literal(m.name).withStyle(m.team == BLUE ? ChatFormatting.BLUE : ChatFormatting.RED)
				.append(Component.literal(how).withStyle(ChatFormatting.GRAY));
		if (killer != null) msg = msg.copy().append(Component.literal(killer.name).withStyle(killer.team == BLUE ? ChatFormatting.BLUE : ChatFormatting.RED));
		else if (!fell) msg = msg.copy().append(Component.literal("the void").withStyle(ChatFormatting.GRAY));
		player.sendSystemMessage(msg);
		m.deaths++;
		if (killer != null) {
			killer.kills++;
			if (killer.e == player) Shows.cheer(player, Payloads.Cheer.KILL, 0.8F);
		}
		if (m.e == player) Shows.cheer(player, Payloads.Cheer.HURT, 0.8F);
		m.lastHitBy = null;
		m.respawn = m.e == player ? 1 : RESPAWN_TICKS;
		if (m.e instanceof Fighter f) {
			// Out of sight for a moment, then back at its base.
			f.setNoAi(true);
			f.setInvisible(true);
			f.setCustomNameVisible(false);
			Vec3 at = spawn(m.team);
			f.snapTo(at.x, at.y + 60, at.z, yaw(m.team), 0);
		}
	}

	/** Back at its base: healed, kit given again. */
	private void back(ServerLevel arena, Member m) {
		m.respawn = -1;
		Vec3 at = spawn(m.team);
		if (m.e instanceof ServerPlayer p) {
			teleport(p, arena, at, yaw(m.team));
			refresh(p);
		} else {
			m.e.snapTo(at.x, at.y, at.z, yaw(m.team), 0);
			m.e.setDeltaMovement(Vec3.ZERO);
			m.e.fallDistance = 0;
			m.e.setHealth(m.e.getMaxHealth());
			m.e.removeAllEffects();
			if (m.e instanceof Fighter f) {
				f.setNoAi(false);
				f.setInvisible(false);
				f.setCustomNameVisible(true);
			}
		}
	}

	private void goal(ServerLevel arena, Member m) {
		score[m.team]++;
		m.goals++;
		int against = 1 - m.team;
		crumbles.add(new Crumble(against, Math.min(PILLAR_Z.length - 1, score[m.team] - 1)));
		ServerPlayNetworking.send(player, new Payloads.Shatter(against, score[m.team] - 1));
		boolean ours = m.team == BLUE;
		ChatFormatting colour = ours ? ChatFormatting.BLUE : ChatFormatting.RED;
		Shows.title(player, Component.literal(m.name.toUpperCase(java.util.Locale.ROOT) + " SCORED!").withStyle(colour, ChatFormatting.BOLD),
				Component.literal("Blue " + score[BLUE] + " · Red " + score[RED]), 2, 40, 10);
		player.sendSystemMessage(Component.literal(m.name).withStyle(colour).append(Component.literal(" scored! ").withStyle(ChatFormatting.GRAY))
				.append(Component.literal("Blue " + score[BLUE] + " · Red " + score[RED]).withStyle(ChatFormatting.WHITE)));
		Shows.sound(player, ours ? SoundEvents.PLAYER_LEVELUP : SoundEvents.BEACON_DEACTIVATE, 1.0F, ours ? 1.0F : 0.8F);
		Shows.cheer(player, ours ? Payloads.Cheer.KILL : Payloads.Cheer.LOSS, ours ? 1.0F : 0.6F);
		score();
		if (score[m.team] >= TO_WIN) {
			end(ours, false, ours ? "Your team crossed " + TO_WIN + " times" : "Red crossed " + TO_WIN + " times");
			return;
		}
		cages(arena, true);
		toCages(arena);
		caged();
	}

	/** One layer of each crumbling pillar every other tick, from the top, its pieces falling away into space. */
	private void tickCrumbles(ServerLevel arena) {
		crumbles.removeIf(c -> {
			if (c.wait-- > 0) return false;
			c.wait = 1;
			int s = side(c.team), pz = PILLAR_Z[c.pillar];
			for (int ax = PILLAR_X; ax <= PILLAR_X + 2; ax++) {
				for (int dz = pz - 1; dz <= pz + 1; dz++) {
					BlockPos pos = new BlockPos(cx + s * ax, Y + c.layer, cz + dz);
					BlockState state = arena.getBlockState(pos);
					if (state.isAir()) continue;
					if (arena.random.nextFloat() < 0.4F) {
						FallingBlockEntity fb = FallingBlockEntity.fall(arena, pos, state);
						fb.dropItem = false;
						fb.setDeltaMovement(s * (0.25 + arena.random.nextDouble() * 0.3), 0.15 + arena.random.nextDouble() * 0.2, (arena.random.nextDouble() - 0.5) * 0.3);
					} else {
						arena.destroyBlock(pos, false);
					}
				}
			}
			return --c.layer < 1;
		});
	}

	// ---------------------------------------------------------------- kits

	private static ItemStack item(Item item) {
		ItemStack s = new ItemStack(item);
		if (s.isDamageableItem()) s.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);
		return s;
	}

	private void armour(LivingEntity e, int team) {
		boolean netherite = settings.bridgeKit() == MatchSettings.NETHERITE;
		Item[] items = netherite ? new Item[] {Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS}
				: new Item[] {Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS};
		EquipmentSlot[] slots = {EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
		for (int i = 0; i < 3; i++) {
			ItemStack s = item(items[i]);
			if (!netherite) s.set(DataComponents.DYED_COLOR, new DyedItemColor(team == BLUE ? BLUE_RGB : RED_RGB));
			e.setItemSlot(slots[i], s);
		}
		e.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
	}

	/** Your kit, given again whenever you come back. */
	private void kit(ServerPlayer p, int team) {
		p.getInventory().clearContent();
		p.setGameMode(GameType.SURVIVAL);
		armour(p, team);
		boolean netherite = settings.bridgeKit() == MatchSettings.NETHERITE;
		p.getInventory().setItem(0, item(netherite ? Items.NETHERITE_SWORD : Items.IRON_SWORD));
		p.getInventory().setItem(1, item(Items.BOW));
		ItemStack pick = item(Items.DIAMOND_PICKAXE);
		Holder<Enchantment> eff = p.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.EFFICIENCY);
		pick.enchant(eff, 2);
		p.getInventory().setItem(2, pick);
		Item block = team == BLUE ? Items.BLUE_TERRACOTTA : Items.RED_TERRACOTTA;
		p.getInventory().setItem(3, new ItemStack(block, 64));
		p.getInventory().setItem(4, new ItemStack(block, 64));
		p.getInventory().setItem(5, new ItemStack(Items.GOLDEN_APPLE, 8));
		p.getInventory().setItem(8, new ItemStack(Items.ARROW));
		p.getInventory().setSelectedSlot(0);
	}

	private void refresh(ServerPlayer p) {
		p.setHealth(p.getMaxHealth());
		p.removeAllEffects();
		p.getFoodData().setFoodLevel(20);
		p.getFoodData().setSaturation(10);
		p.setRemainingFireTicks(0);
		p.fallDistance = 0;
		kit(p, BLUE);
	}

	private void teleport(ServerPlayer p, ServerLevel arena, Vec3 at, float yaw) {
		p.teleportTo(arena, at.x, at.y, at.z, java.util.Set.of(), yaw, 0, true);
		p.setDeltaMovement(Vec3.ZERO);
		p.fallDistance = 0;
	}

	private void holdAt(ServerLevel arena, Vec3 at) {
		if (player.level() != arena || player.position().distanceToSqr(at) > 0.8) teleport(player, arena, at, yaw(BLUE));
		player.fallDistance = 0;
	}

	// ---------------------------------------------------------------- who hurts whom

	@Override
	public boolean allowDamage(LivingEntity victim, DamageSource source, float amount) {
		if (source.is(DamageTypeTags.IS_FALL)) return false;
		if (!playing()) return false;
		Member v = memberOf(victim), a = memberOf(source.getEntity());
		if (v == null || v.respawn >= 0) return false;
		return a == null || a.team != v.team;
	}

	@Override
	public void afterDamage(LivingEntity victim, DamageSource source, float amount) {
		Member v = memberOf(victim), a = memberOf(source.getEntity());
		if (v == null || a == null) return;
		v.lastHitBy = a.e;
		v.lastHitAt = arena() == null ? 0 : arena().getGameTime();
		if (a.e == player) Shows.cheer(player, Payloads.Cheer.HIT, Math.min(1, amount / 6));
		else if (v.e == player && amount >= 4) Shows.cheer(player, Payloads.Cheer.HURT, Math.min(1, amount / 8));
	}

	@Override
	public boolean allowDeath(LivingEntity victim, DamageSource source) {
		Member v = memberOf(victim);
		if (v == null) return true;
		victim.setHealth(victim.getMaxHealth());
		if (playing() && v.respawn < 0) down(arena(), v, source.getEntity());
		return false;
	}

	@Override
	public void onDeath(LivingEntity e) {
	}

	// ---------------------------------------------------------------- the end

	private void end(boolean won, boolean draw, String reason) {
		if (phase.ordinal() >= Phase.RESULT.ordinal()) return;
		boolean played = phase == Phase.PLAY || phase == Phase.CAGED;
		this.won = won;
		this.draw = draw;
		this.endReason = reason;
		phase = Phase.RESULT;
		ticks = 0;
		for (Member m : members) if (m.e instanceof Fighter f) f.setNoAi(true);
		if (!played) {
			if (Loadout.has(player)) leave();
			else phase = Phase.DONE;
			return;
		}
		Member me = members.isEmpty() ? null : members.get(0);
		Shows.title(player, Component.literal(won ? "VICTORY" : draw ? "DRAW" : forfeit ? "LEFT THE BRIDGE" : "DEFEAT")
				.withStyle(won ? ChatFormatting.GOLD : draw ? ChatFormatting.AQUA : ChatFormatting.RED, ChatFormatting.BOLD),
				Component.literal("Blue " + score[BLUE] + " · Red " + score[RED] + " · " + reason), 5, 70, 20);
		Shows.sound(player, won ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE : SoundEvents.BEACON_DEACTIVATE, 1.0F, 1.0F);
		Shows.cheer(player, won ? Payloads.Cheer.WIN : Payloads.Cheer.LOSS, 1.0F);
		player.sendSystemMessage(Component.literal("The Bridge · " + (won ? "Victory" : draw ? "Draw" : "Defeat") + " · " + subtitle())
				.withStyle(won ? ChatFormatting.GOLD : ChatFormatting.RED));
		summary = String.format("Goals %d to %d · Your goals %d · Knocked out %d · Lost %d · Time %s", score[BLUE], score[RED], me == null ? 0 : me.goals,
				me == null ? 0 : me.kills, me == null ? 0 : me.deaths, Match.clock(playTicks / 20));
		player.sendSystemMessage(Component.literal(" " + summary).withStyle(ChatFormatting.GRAY));
		score();
		if (won && !forfeit) ServerPlayNetworking.send(player, new Payloads.Celebrate(Match.CELEBRATE_TICKS));
	}

	@Override
	public void retire() {
		ServerLevel arena = arena();
		if (arena != null) clear(arena);
		phase = Phase.DONE;
	}

	@Override
	public void forfeit() {
		forfeit = true;
		if (phase != Phase.RESULT) end(false, false, "You left");
		leave();
	}

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
		if (arena != null) clear(arena);
		phase = Phase.DONE;
	}

	/** The bots gone, the cages down. */
	private void clear(ServerLevel arena) {
		for (Member m : members) if (!(m.e instanceof ServerPlayer)) m.e.discard();
		AABB area = new AABB(cx - SIZE, Y - 60, cz - SIZE, cx + SIZE, Y + 80, cz + SIZE);
		for (Entity e : arena.getEntities((Entity) null, area, e -> e instanceof Fighter || e instanceof ItemEntity || e instanceof AbstractArrow || e instanceof FallingBlockEntity)) {
			e.discard();
		}
		player.removeTag(TEAM_TAG + "blue");
		if (cagesUp) cages(arena, false);
	}

	@Override
	public boolean done() {
		return phase == Phase.DONE;
	}

	@Override
	public boolean fighting() {
		return phase == Phase.PLAY;
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
		ServerPlayNetworking.send(player, new Payloads.State(in, Payloads.State.BRIDGE, cx, cz, phase == Phase.PLAY || phase == Phase.CAGED));
	}

	/** The score, the clock and who's playing, for the banners at the top of the screen. */
	private void score() {
		int left = settings.timeLimit() > 0 ? Math.max(0, settings.timeLimit() - playTicks / 20) : -1;
		String red = settings.count() == 1 && members.size() > 1 ? members.get(1).name : "Red team";
		int look = members.size() > 1 ? Math.floorMod(members.get(1).e.getUUID().hashCode(), 8) : 0;
		String blue = settings.count() == 1 ? player.getName().getString() : player.getName().getString() + " + " + (settings.count() - 1);
		ServerPlayNetworking.send(player, new Payloads.BridgeScore(score[BLUE], score[RED], TO_WIN, left, blue, red, look));
	}
}
