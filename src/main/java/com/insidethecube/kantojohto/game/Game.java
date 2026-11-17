package com.insidethecube.kantojohto.game;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.insidethecube.kantojohto.RouteOne;
import com.insidethecube.kantojohto.engine.Mon;
import com.insidethecube.kantojohto.engine.Species;
import com.insidethecube.kantojohto.engine.Wild;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;

/**
 * The game on the server: your save, walking in the grass, the people you talk to, trainers who spot you,
 * healing, the Mart, your first Pokémon, and what happens after a battle.
 */
public final class Game {
	private Game() {}

	public static final String SAVE_NAME = "Route One";
	public static final String MARKER = "routeone.json";
	public static AttachmentType<PlayerData> DATA;
	private static final Random RNG = new Random();
	private static final Map<UUID, BlockPos> LAST = new HashMap<>();
	private static final Map<UUID, Integer> COOLDOWN = new HashMap<>();
	/** Players who switched the adventure on with the key, outside a Route One save (for trying it out). */
	private static final Map<UUID, Boolean> TRYING = new HashMap<>();
	/** Who's in a text box or being walked up to (no encounters, no other trainers). */
	private static final Map<UUID, Integer> BUSY = new HashMap<>();
	private static boolean ourSave;

	public static final int POKE_BALL_PRICE = 200, POTION_PRICE = 300;

	public static void register() {
		DATA = AttachmentRegistry.create(RouteOne.id("player"), b -> b.persistent(PlayerData.CODEC).initializer(PlayerData::new).copyOnDeath());
	}

	public static PlayerData data(ServerPlayer p) {
		return p.getAttachedOrCreate(DATA);
	}

	// ---------- the save ----------

	public static void serverStarted(MinecraftServer server) {
		ourSave = Files.exists(marker(server));
		if (!ourSave) return;
		ServerLevel level = server.overworld();
		GameRules rules = level.getGameRules();
		rules.set(GameRules.ADVANCE_WEATHER, false, server);
		rules.set(GameRules.ADVANCE_TIME, true, server);
		rules.set(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, 0, server);
		rules.set(GameRules.SPAWN_MOBS, false, server);
		rules.set(GameRules.SPAWN_MONSTERS, false, server);
		rules.set(GameRules.SPAWN_PHANTOMS, false, server);
		rules.set(GameRules.SPAWN_PATROLS, false, server);
		rules.set(GameRules.SPAWN_WANDERING_TRADERS, false, server);
		rules.set(GameRules.MOB_GRIEFING, false, server);
		rules.set(GameRules.RANDOM_TICK_SPEED, 0, server);
		rules.set(GameRules.FALL_DAMAGE, false, server);
		rules.set(GameRules.KEEP_INVENTORY, true, server);
		rules.set(GameRules.SHOW_ADVANCEMENT_MESSAGES, false, server);
		level.setWeatherParameters(12000, 0, false, false);
	}

	private static Path marker(MinecraftServer server) {
		return server.getWorldPath(LevelResource.ROOT).resolve(MARKER);
	}

	public static boolean ourSave() {
		return ourSave;
	}

	public static boolean adventure(ServerPlayer p) {
		return ourSave || TRYING.getOrDefault(p.getUUID(), false);
	}

	public static void join(ServerPlayer p) {
		LAST.remove(p.getUUID());
		if (!ourSave) {
			sync(p);
			return;
		}
		p.setGameMode(GameType.ADVENTURE);
		// A new adventure starts in Pallet Town, with empty pockets.
		if (!data(p).started) {
			p.getInventory().clearContent();
			toStart(p);
		}
		setAdventure(p, true);
		placeTouchPoints(p.level().getServer());
		sync(p);
		if (!data(p).started) Net.send(p, "intro", new CompoundTag());
	}

	public static void leave(ServerPlayer p) {
		Battles.drop(p);
		LAST.remove(p.getUUID());
		BUSY.remove(p.getUUID());
	}

	/** Turns the adventure on or off for a player: the step up, no hunger, no harm, and the camera on their screen. */
	public static void setAdventure(ServerPlayer p, boolean on) {
		if (!ourSave) TRYING.put(p.getUUID(), on);
		AttributeInstance step = p.getAttribute(Attributes.STEP_HEIGHT);
		if (step != null) {
			if (on && adventure(p)) step.addOrUpdateTransientModifier(new AttributeModifier(RouteOne.id("step"), 0.5, AttributeModifier.Operation.ADD_VALUE));
			else step.removeModifier(RouteOne.id("step"));
		}
		CompoundTag t = new CompoundTag();
		t.putBoolean("on", adventure(p));
		t.putBoolean("save", ourSave);
		Net.send(p, "mode", t);
	}

	public static void sync(ServerPlayer p) {
		Net.send(p, "sync", data(p).save());
	}

	// ---------- every tick ----------

	public static void tick(MinecraftServer server) {
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			if (!adventure(p)) continue;
			UUID id = p.getUUID();
			p.getFoodData().setFoodLevel(20);
			if (p.getHealth() < p.getMaxHealth()) p.setHealth(p.getMaxHealth());
			COOLDOWN.computeIfPresent(id, (k, v) -> v > 1 ? v - 1 : null);
			BUSY.computeIfPresent(id, (k, v) -> v > 1 ? v - 1 : null);
			if (Battles.in(p) || BUSY.containsKey(id)) {
				LAST.put(id, p.blockPosition());
				continue;
			}
			BlockPos pos = p.blockPosition();
			BlockPos last = LAST.put(id, pos);
			if (last != null && (last.getX() != pos.getX() || last.getZ() != pos.getZ())) step(p, pos);
			if (server.getTickCount() % 4 == 0) watch(p);
		}
		approach(server);
	}

	private static boolean grass(BlockState s) {
		return s.is(Blocks.SHORT_GRASS) || s.is(Blocks.TALL_GRASS) || s.is(Blocks.FERN) || s.is(Blocks.LARGE_FERN);
	}

	/** A step: in the grass, a wild Pokémon may jump out (as often as on Route 1 in Red). */
	private static void step(ServerPlayer p, BlockPos pos) {
		PlayerData d = data(p);
		if (!d.started || !d.anyHealthy() || COOLDOWN.containsKey(p.getUUID())) return;
		if (!grass(p.level().getBlockState(pos)) && !grass(p.level().getBlockState(pos.above()))) return;
		Wild.Table table = Wild.ROUTE_1;
		if (RNG.nextInt(256) >= table.ratePer256()) return;
		Battles.wild(p, table.roll(RNG));
	}

	/** Pallet Town, outside your house. */
	public static final int START_X = -806, START_Y = 18, START_Z = 700;

	private static void toStart(ServerPlayer p) {
		ServerLevel level = (ServerLevel) p.level();
		BlockPos at = new BlockPos(START_X, START_Y, START_Z);
		if (!level.getBlockState(at).getCollisionShape(level, at).isEmpty() || !level.getBlockState(at.above()).getCollisionShape(level, at.above()).isEmpty()) {
			at = new BlockPos(START_X, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, START_X, START_Z), START_Z);
		}
		p.teleportTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
	}

	public static void afterBattle(ServerPlayer p) {
		COOLDOWN.put(p.getUUID(), 60);
	}

	public static void blackout(ServerPlayer p) {
		PlayerData d = data(p);
		ServerLevel level = (ServerLevel) p.level();
		Vec3 to;
		if (d.hasHeal) to = new Vec3(d.healX, d.healY, d.healZ);
		else {
			BlockPos sp = level.getRespawnData().pos();
			to = Vec3.atBottomCenterOf(sp);
		}
		p.teleportTo(to.x, to.y, to.z);
	}

	// ---------- trainers who spot you ----------

	private record Walk(Npc npc, ServerPlayer player, Trainers.Trainer trainer) {}

	private static final Map<UUID, Walk> WALKS = new HashMap<>();

	/** Any trainer who hasn't battled you yet and sees you along their line walks up to you. */
	private static void watch(ServerPlayer p) {
		PlayerData d = data(p);
		if (!d.started || !d.anyHealthy() || WALKS.containsKey(p.getUUID())) return;
		ServerLevel level = (ServerLevel) p.level();
		for (Npc n : level.getEntitiesOfClass(Npc.class, p.getBoundingBox().inflate(12))) {
			if (n.role() != Npc.TRAINER) continue;
			Trainers.Trainer t = Trainers.get(n.key());
			if (t == null || t.sight() <= 0 || d.beaten.contains(t.key())) continue;
			double yaw = Math.toRadians(n.facing());
			double fx = -Math.sin(yaw), fz = Math.cos(yaw);
			double dx = p.getX() - n.getX(), dz = p.getZ() - n.getZ(), dy = p.getY() - n.getY();
			double along = dx * fx + dz * fz, across = Math.abs(dx * fz - dz * fx);
			if (along < 0.5 || along > t.sight() + 0.5 || across > 0.9 || Math.abs(dy) > 2) continue;
			if (!n.hasLineOfSight(p)) continue;
			CompoundTag e = new CompoundTag();
			e.putInt("npc", n.getId());
			Net.send(p, "exclaim", e);
			n.approaching = 120;
			BUSY.put(p.getUUID(), 400);
			WALKS.put(p.getUUID(), new Walk(n, p, t));
			return;
		}
	}

	private static void approach(MinecraftServer server) {
		WALKS.entrySet().removeIf(en -> {
			Walk w = en.getValue();
			Npc n = w.npc();
			ServerPlayer p = w.player();
			if (n.isRemoved() || p.isRemoved() || p.hasDisconnected()) return true;
			n.approaching--;
			double dist = n.distanceTo(p);
			if (n.approaching > 100) {
				n.getNavigation().stop();
				return false;
			}
			if (dist > 1.8 && n.approaching > 0) {
				n.getNavigation().moveTo(p, 1.0);
				return false;
			}
			n.getNavigation().stop();
			n.approaching = 0;
			n.getLookControl().setLookAt(p);
			talkBattle(p, n, w.trainer());
			return true;
		});
	}

	// ---------- talking ----------

	public static void talk(ServerPlayer p, int entityId) {
		if (Battles.in(p)) return;
		if (!(p.level().getEntity(entityId) instanceof Npc n) || n.distanceTo(p) > 4.5) return;
		PlayerData d = data(p);
		n.getLookControl().setLookAt(p);
		switch (n.role()) {
			case Npc.NURSE -> dialogue(p, n, "heal", List.of("Welcome to our POKéMON CENTER!", "We heal your POKéMON back to perfect health!",
					"Shall we heal your POKéMON?"));
			case Npc.CLERK -> dialogue(p, n, "mart", List.of("Hi there! May I help you?"));
			case Npc.TRAINER, Npc.LEADER -> {
				Trainers.Trainer t = Trainers.get(n.key());
				if (t == null) {
					dialogue(p, n, "talk", List.of("..."));
					return;
				}
				if (d.beaten.contains(t.key())) {
					dialogue(p, n, "talk", t.after());
					return;
				}
				if (!d.started || !d.anyHealthy()) {
					dialogue(p, n, "talk", List.of("You don't have any POKéMON that can battle!"));
					return;
				}
				talkBattle(p, n, t);
			}
			default -> {
			}
		}
	}

	private static void talkBattle(ServerPlayer p, Npc n, Trainers.Trainer t) {
		CompoundTag extra = new CompoundTag();
		extra.putString("trainer", t.key());
		dialogue(p, n, "battle", t.before(), extra);
	}

	private static void dialogue(ServerPlayer p, Npc n, String mode, List<String> lines) {
		dialogue(p, n, mode, lines, new CompoundTag());
	}

	private static void dialogue(ServerPlayer p, Npc n, String mode, List<String> lines, CompoundTag t) {
		t.putInt("npc", n == null ? -1 : n.getId());
		t.putString("mode", mode);
		ListTag l = new ListTag();
		for (String s : lines) l.add(StringTag.valueOf(s));
		t.put("lines", l);
		BUSY.put(p.getUUID(), 1200);
		Net.send(p, "dialogue", t);
	}

	/** A text box closed: heal (if you said yes), open the Mart, start the battle. */
	public static void dialogueDone(ServerPlayer p, CompoundTag t) {
		BUSY.remove(p.getUUID());
		String mode = t.getStringOr("mode", "talk");
		boolean yes = t.getBooleanOr("yes", false);
		Npc n = p.level().getEntity(t.getIntOr("npc", -1)) instanceof Npc npc ? npc : null;
		PlayerData d = data(p);
		switch (mode) {
			case "heal" -> {
				if (!yes) {
					dialogue(p, n, "talk", List.of("We hope to see you again!"));
					return;
				}
				d.healAll();
				d.hasHeal = true;
				d.healX = p.getX();
				d.healY = p.getY();
				d.healZ = p.getZ();
				sync(p);
				CompoundTag s = new CompoundTag();
				s.putString("sound", "heal");
				Net.send(p, "sound", s);
				dialogue(p, n, "talk", List.of("Thank you for waiting.", "We've restored your POKéMON to full health.", "We hope to see you again!"));
			}
			case "mart" -> Net.send(p, "mart", new CompoundTag());
			case "battle" -> {
				Trainers.Trainer tr = Trainers.get(t.getStringOr("trainer", ""));
				if (tr != null && !d.beaten.contains(tr.key()) && d.anyHealthy()) Battles.trainer(p, tr);
			}
			default -> {
			}
		}
	}

	// ---------- the Mart, the bag, your first Pokémon ----------

	public static void buy(ServerPlayer p, CompoundTag t) {
		PlayerData d = data(p);
		String item = t.getStringOr("item", "");
		int price = item.equals("ball") ? POKE_BALL_PRICE : item.equals("potion") ? POTION_PRICE : -1;
		if (price < 0 || d.money < price) return;
		d.money -= price;
		if (item.equals("ball")) d.balls++;
		else d.potions++;
		sync(p);
	}

	public static void usePotion(ServerPlayer p, CompoundTag t) {
		PlayerData d = data(p);
		int i = t.getIntOr("i", -1);
		if (d.potions <= 0 || i < 0 || i >= d.party.size()) return;
		Mon m = d.party.get(i);
		if (m.fainted() || m.hp >= m.maxHp()) return;
		m.hp = Math.min(m.maxHp(), m.hp + 20);
		d.potions--;
		sync(p);
	}

	public static void starter(ServerPlayer p, CompoundTag t) {
		PlayerData d = data(p);
		if (d.started) return;
		String sp = t.getStringOr("species", "");
		if (!sp.equals("bulbasaur") && !sp.equals("charmander") && !sp.equals("squirtle")) return;
		d.add(new Mon(Species.get(sp), 5, RNG));
		d.started = true;
		d.balls = 5;
		d.potions = 3;
		d.money = 3000;
		sync(p);
	}

	public static void save(ServerPlayer p) {
		MinecraftServer server = p.level().getServer();
		server.saveEverything(true, false, true);
	}

	// ---------- touch points ----------

	/** Where the first people stand on the Kanto map: the Viridian City nurse and clerk, a youngster on Route 1, Brock by Oak's lab. */
	private static final Object[][] DEFAULTS = {
			{Npc.NURSE, "", -785, 561, 0F}, {Npc.CLERK, "", -766, 549, 0F}, {Npc.TRAINER, "youngster_joey", -790, 600, 0F},
			{Npc.LEADER, "leader_brock", -798, 706, 90F}};

	private static void placeTouchPoints(MinecraftServer server) {
		Path m = marker(server);
		JsonObject j;
		try {
			j = JsonParser.parseString(Files.readString(m, StandardCharsets.UTF_8)).getAsJsonObject();
		} catch (Exception e) {
			j = new JsonObject();
		}
		if (j.has("placed") && j.get("placed").getAsBoolean()) return;
		ServerLevel level = server.overworld();
		for (Object[] d : DEFAULTS) place(level, (Integer) d[0], (String) d[1], (Integer) d[2], (Integer) d[3], (Float) d[4]);
		j.addProperty("placed", true);
		try {
			Files.writeString(m, j.toString(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			RouteOne.LOG.warn("Couldn't write {}", m, e);
		}
	}

	public static Npc place(ServerLevel level, int role, String key, double x, double z, float facing) {
		int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z));
		return place(level, role, key, Math.floor(x) + 0.5, y, Math.floor(z) + 0.5, facing);
	}

	public static Npc place(ServerLevel level, int role, String key, double x, double y, double z, float facing) {
		Npc n = RouteOne.NPC.create(level, EntitySpawnReason.COMMAND);
		if (n == null) return null;
		n.setRole(role);
		n.setKey(key);
		n.setPos(x, y, z);
		n.setFacing(facing);
		Trainers.Trainer t = Trainers.get(key);
		n.setCustomName(net.minecraft.network.chat.Component.literal(t != null ? t.title() : role == Npc.NURSE ? "NURSE" : role == Npc.CLERK ? "CLERK" : "TRAINER"));
		n.setCustomNameVisible(false);
		level.addFreshEntity(n);
		return n;
	}
}
