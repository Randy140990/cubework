package com.insidethecube.retromon;

import com.insidethecube.retromon.engine.Mon;
import com.insidethecube.retromon.engine.Species;
import com.insidethecube.retromon.game.Battles;
import com.insidethecube.retromon.game.Game;
import com.insidethecube.retromon.game.MonDisplay;
import com.insidethecube.retromon.game.Net;
import com.insidethecube.retromon.game.Npc;
import com.insidethecube.retromon.game.PlayerData;
import com.insidethecube.retromon.game.Trainers;
import com.insidethecube.retromon.game.WorldMon;
import com.insidethecube.retromon.engine.Item;
import com.insidethecube.retromon.engine.Places;
import net.minecraft.world.phys.Vec3;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Retromon: the original games' mechanics, region by region, as a 2.5D adventure on Insid3 Th3 Cub3's maps (Kanto and Johto first). */
public class Retromon implements ModInitializer {
	public static final String ID = "retromon";
	public static final Logger LOG = LoggerFactory.getLogger("Retromon");
	private static final ResourceKey<EntityType<?>> NPC_KEY = ResourceKey.create(Registries.ENTITY_TYPE, id("npc"));
	private static final ResourceKey<EntityType<?>> MON_KEY = ResourceKey.create(Registries.ENTITY_TYPE, id("mon_display"));
	private static final ResourceKey<EntityType<?>> WORLD_MON_KEY = ResourceKey.create(Registries.ENTITY_TYPE, id("world_mon"));
	private static final ResourceKey<EntityType<?>> ROOM_LIGHT_KEY = ResourceKey.create(Registries.ENTITY_TYPE, id("room_light"));
	public static EntityType<Npc> NPC;
	public static EntityType<MonDisplay> MON_DISPLAY;
	public static EntityType<WorldMon> WORLD_MON;
	/** The light through a room's windows (only on your screen). */
	public static EntityType<com.insidethecube.retromon.game.RoomLight> ROOM_LIGHT;
	public static final ResourceKey<net.minecraft.world.level.block.Block> PROP_KEY = ResourceKey.create(Registries.BLOCK, id("prop"));
	/** The insides of buildings, block by block (see Interiors). */
	public static net.minecraft.world.level.block.Block PROP;
	public static final ResourceKey<net.minecraft.world.level.block.Block> MAILBOX_KEY = ResourceKey.create(Registries.BLOCK, id("mailbox"));
	/** HeartGold's mailboxes by the houses (see Mailbox). */
	public static net.minecraft.world.level.block.Block MAILBOX;

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(ID, path);
	}

	@Override
	public void onInitialize() {
		NPC = Registry.register(BuiltInRegistries.ENTITY_TYPE, NPC_KEY,
				EntityType.Builder.<Npc>of(Npc::new, MobCategory.MISC).sized(0.6F, 1.8F).eyeHeight(1.62F).clientTrackingRange(10).noLootTable().build(NPC_KEY));
		MON_DISPLAY = Registry.register(BuiltInRegistries.ENTITY_TYPE, MON_KEY,
				EntityType.Builder.<MonDisplay>of(MonDisplay::new, MobCategory.MISC).sized(0.8F, 1.0F).clientTrackingRange(0).noLootTable().noSave().noSummon()
						.build(MON_KEY));
		ROOM_LIGHT = Registry.register(BuiltInRegistries.ENTITY_TYPE, ROOM_LIGHT_KEY,
				EntityType.Builder.<com.insidethecube.retromon.game.RoomLight>of(com.insidethecube.retromon.game.RoomLight::new, MobCategory.MISC).sized(0.1F, 0.1F)
						.clientTrackingRange(0).noLootTable().noSave().noSummon().build(ROOM_LIGHT_KEY));
		WORLD_MON = Registry.register(BuiltInRegistries.ENTITY_TYPE, WORLD_MON_KEY,
				EntityType.Builder.<WorldMon>of(WorldMon::new, MobCategory.MISC).sized(1.0F, 1.2F).clientTrackingRange(10).noLootTable().build(WORLD_MON_KEY));
		// (0.67) The rooms' blocks: their looks shared out over many blocks of Prop.SIZE states each (game/Prop).
		for (int k = 0; k < com.insidethecube.retromon.game.Prop.BLOCKS; k++) {
			ResourceKey<net.minecraft.world.level.block.Block> key = k == 0 ? PROP_KEY : ResourceKey.create(Registries.BLOCK, id("prop_" + k));
			com.insidethecube.retromon.game.Prop.ALL[k] = Registry.register(BuiltInRegistries.BLOCK, key, new com.insidethecube.retromon.game.Prop(
					net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().setId(key).noOcclusion().strength(-1F, 3600000F).noLootTable()
							.isViewBlocking((st, g, pos) -> false).isSuffocating((st, g, pos) -> false)
							.pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK).sound(net.minecraft.world.level.block.SoundType.WOOD),
					k * com.insidethecube.retromon.game.Prop.SIZE));
		}
		PROP = com.insidethecube.retromon.game.Prop.ALL[0];
		MAILBOX = Registry.register(BuiltInRegistries.BLOCK, MAILBOX_KEY, new com.insidethecube.retromon.game.Mailbox(
				net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().setId(MAILBOX_KEY).noOcclusion().strength(-1F, 3600000F).noLootTable()
						.isViewBlocking((st, g, pos) -> false).isSuffocating((st, g, pos) -> false)
						.pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK).sound(net.minecraft.world.level.block.SoundType.METAL)));
		FabricDefaultAttributeRegistry.register(NPC, Npc.createAttributes());
		FabricDefaultAttributeRegistry.register(WORLD_MON, WorldMon.createAttributes());
		// Read the games' data now, so a mistake in it shows at once.
		LOG.info("Retromon: {} Pokémon, {} moves, {} items, {} places, {} trainers, {} people", com.insidethecube.retromon.engine.Species.ALL.size(),
				com.insidethecube.retromon.engine.Move.ALL.size(), Item.ALL.size(), Places.ALL.size(), Trainers.ALL.size(), Trainers.PEOPLE.size());
		Game.register();
		Net.register();
		ServerPlayNetworking.registerGlobalReceiver(Net.Act.TYPE, (payload, ctx) -> act(ctx.player(), payload));
		ServerLifecycleEvents.SERVER_STARTED.register(Game::serverStarted);
		ServerTickEvents.END_SERVER_TICK.register(Game::tick);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> Game.join(handler.player));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> Game.leave(handler.player));
		// Nothing on the map can be broken in a Retromon save (whatever the game mode), unless map editing is on.
		net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, be) -> !Game.locked(player));
		net.fabricmc.fabric.api.event.player.AttackBlockCallback.EVENT.register((player, world, hand, pos, dir) ->
				Game.locked(player) ? net.minecraft.world.InteractionResult.FAIL : net.minecraft.world.InteractionResult.PASS);
		commands();
	}

	private static void act(ServerPlayer p, Net.Act a) {
		CompoundTag t = a.data();
		switch (a.kind()) {
			case "adventure" -> Game.setAdventure(p, t.getBooleanOr("on", false));
			case "room_use" -> Game.roomUse(p, t.getStringOr("use", ""));
			case "photo" -> Game.photo(p, t.getBooleanOr("on", false), t.getDoubleOr("x", Double.NaN), t.getDoubleOr("y", Double.NaN),
					t.getDoubleOr("z", Double.NaN));
			case "talk" -> Game.talk(p, t.getIntOr("npc", -1));
			case "dialogue_done" -> Game.dialogueDone(p, t);
			case "battle" -> Battles.choose(p, t);
			case "battle_quit" -> {
				Battles.drop(p);
				Game.afterBattle(p);
			}
			case "battle_closed" -> Game.battleClosed(p);
			case "buy" -> Game.buy(p, t);
			case "sell" -> Game.sell(p, t);
			case "use" -> Game.use(p, t);
			case "starter" -> Game.starter(p, t);
			case "nickname" -> Game.nickname(p, t);
			case "evolve" -> Game.evolve(p, t);
			case "learn" -> Game.learn(p, t);
			case "trade_done" -> Game.tradeDone(p, t);
			case "order" -> Game.order(p, t);
			case "options" -> Game.options(p, t);
			case "speed" -> {
				if (Game.adventure(p) && p.level().getServer().isSingleplayer()) Game.setSpeed(t.getIntOr("times", 1));
			}
			case "save" -> Game.save(p);
			case "profile" -> Game.profile(p, t);
			case "fly" -> Game.fly(p, t);
			case "travel" -> Game.travel(p, t);
			case "dark" -> com.insidethecube.retromon.game.Interiors.dark(p);
			case "outfit" -> Game.outfit(p, t);
			case "lab" -> Game.toLab(p);
			case "wait" -> Game.waitUntil(p, t);
			case "nights", "prefs" -> Game.nights(p, t);
			case "follow" -> Game.follow(p, t);
			case "pc" -> Game.pc(p, t);
			case "read" -> Game.read(p, t);
			case "device" -> Game.device(p, t.getBooleanOr("on", false));
			case "pick_party" -> Game.pickParty(p, t);
			default -> {
			}
		}
	}

	private static int populate(CommandContext<CommandSourceStack> c, boolean redo) {
		String place = StringArgumentType.getString(c, "place");
		int n = Game.populate(c.getSource().getLevel(), place, redo);
		if (n < 0) {
			c.getSource().sendFailure(Component.literal("Mark " + place + " first (/retromon zone), and it has to be one of the games' Kanto maps"));
			return 0;
		}
		c.getSource().sendSuccess(() -> Component.literal(n + " people and items placed in " + place + (redo ? " (everyone put back where the games had them)" : "")), false);
		return 1;
	}

	private static float facingFrom(ServerPlayer p) {
		// Face back the way you're looking from (so they face you as you put them down).
		return Math.round((p.getYRot() + 180) / 90F) * 90F;
	}

	private static int placed(CommandContext<CommandSourceStack> c, String what) {
		c.getSource().sendSuccess(() -> Component.literal("Placed " + what), false);
		return 1;
	}

	private static void commands() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) -> dispatcher.register(Commands.literal("retromon")
				.requires(s -> s.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER))
				.then(Commands.literal("place")
						.then(Commands.literal("nurse").executes(c -> {
							ServerPlayer p = c.getSource().getPlayerOrException();
							Game.place((ServerLevel) p.level(), Npc.NURSE, "", p.getX(), p.getY(), p.getZ(), facingFrom(p));
							return placed(c, "the nurse");
						}))
						.then(Commands.literal("clerk").executes(c -> {
							ServerPlayer p = c.getSource().getPlayerOrException();
							Game.place((ServerLevel) p.level(), Npc.CLERK, Game.data(p).place, p.getX(), p.getY(), p.getZ(), facingFrom(p));
							return placed(c, "a Mart clerk (" + Places.martFor(Game.data(p).place).id() + ")");
						}).then(Commands.argument("mart", StringArgumentType.word()).suggests((c, b) -> {
							for (String m : Places.MARTS.keySet()) b.suggest(m);
							return b.buildFuture();
						}).executes(c -> {
							ServerPlayer p = c.getSource().getPlayerOrException();
							String mart = StringArgumentType.getString(c, "mart");
							Game.place((ServerLevel) p.level(), Npc.CLERK, mart, p.getX(), p.getY(), p.getZ(), facingFrom(p));
							return placed(c, "a Mart clerk (" + mart + ")");
						})))
						.then(Commands.literal("trainer").then(Commands.argument("key", StringArgumentType.word()).suggests((c, b) -> {
							for (String k : Trainers.ALL.keySet()) b.suggest(k);
							return b.buildFuture();
						}).executes(c -> {
							ServerPlayer p = c.getSource().getPlayerOrException();
							Trainers.Trainer t = Trainers.get(StringArgumentType.getString(c, "key"));
							if (t == null) {
								c.getSource().sendFailure(Component.literal("No trainer called that (try /retromon list trainers <place>)"));
								return 0;
							}
							Game.place((ServerLevel) p.level(), t.leader() ? Npc.LEADER : Npc.TRAINER, t.key(), p.getX(), p.getY(), p.getZ(),
									Game.yawOf(t.facing(), facingFrom(p)));
							return placed(c, t.title() + " (" + t.key() + ")");
						})))
						.then(Commands.literal("person").then(Commands.argument("key", StringArgumentType.word()).suggests((c, b) -> {
							for (String k : Trainers.PEOPLE.keySet()) b.suggest(k);
							return b.buildFuture();
						}).executes(c -> {
							ServerPlayer p = c.getSource().getPlayerOrException();
							Trainers.Person person = Trainers.PEOPLE.get(StringArgumentType.getString(c, "key"));
							if (person == null) {
								c.getSource().sendFailure(Component.literal("No one called that (try /retromon list people <place>)"));
								return 0;
							}
							float yaw = Game.yawOf(person.facing(), facingFrom(p));
							String sp = person.species();
							if (sp != null) Game.placeMon((ServerLevel) p.level(), sp, "pet:" + person.key(), p.getX(), p.getY(), p.getZ(), yaw);
							else Game.place((ServerLevel) p.level(), Npc.PERSON, person.key(), p.getX(), p.getY(), p.getZ(), yaw);
							return placed(c, person.key());
						})))
						.then(Commands.literal("pokemon").then(Commands.argument("id", StringArgumentType.word()).suggests((c, b) -> {
							for (String k : Trainers.STATICS.keySet()) b.suggest(k);
							return b.buildFuture();
						}).executes(c -> {
							ServerPlayer p = c.getSource().getPlayerOrException();
							Trainers.Static st = Trainers.STATICS.get(StringArgumentType.getString(c, "id"));
							if (st == null) {
								c.getSource().sendFailure(Component.literal("No one-off Pokémon called that"));
								return 0;
							}
							Game.placeMon((ServerLevel) p.level(), st.species(), st.id(), p.getX(), p.getY(), p.getZ(), facingFrom(p));
							return placed(c, st.id() + " (" + st.kind() + ", L" + st.level() + ")");
						})))
						// (0.67) One of the farm's creatures or a pet, where you stand, facing you: /retromon place farm moobelle.
						.then(Commands.literal("farm").then(Commands.argument("kind", StringArgumentType.word()).suggests((c, b) -> {
							for (String k : com.insidethecube.retromon.game.Farm.ALL.keySet()) b.suggest(k);
							return b.buildFuture();
						}).executes(c -> {
							ServerPlayer p = c.getSource().getPlayerOrException();
							String kind = StringArgumentType.getString(c, "kind");
							com.insidethecube.retromon.game.Farm.Kind f = com.insidethecube.retromon.game.Farm.get(kind);
							if (f == null) {
								c.getSource().sendFailure(Component.literal("No farm creature or pet called that"));
								return 0;
							}
							String key = "farm:" + kind + "@" + p.getBlockX() + "," + p.getBlockY() + "," + p.getBlockZ();
							Game.placeMon((ServerLevel) p.level(), kind, key, p.getX(), p.getY(), p.getZ(), facingFrom(p));
							return placed(c, f.name() + (f.coat().isEmpty() ? "" : " (" + f.coat().split(":")[0] + ")") + " at " + p.getBlockX() + " " + p.getBlockY() + " " + p.getBlockZ());
						}))))
				.then(Commands.literal("remove").executes(c -> {
					ServerPlayer p = c.getSource().getPlayerOrException();
					net.minecraft.world.entity.Entity best = null;
					for (net.minecraft.world.entity.Entity n : p.level().getEntities(p, p.getBoundingBox().inflate(4), e -> e instanceof Npc || e instanceof WorldMon)) {
						if (best == null || n.distanceTo(p) < best.distanceTo(p)) best = n;
					}
					if (best == null) return 0;
					best.discard();
					c.getSource().sendSuccess(() -> Component.literal("Removed"), false);
					return 1;
				}))
				.then(Commands.literal("zone")
						.then(Commands.literal("corner1").executes(c -> {
							Game.corner(c.getSource().getPlayerOrException(), 1);
							c.getSource().sendSuccess(() -> Component.literal("First corner marked. Now go to the opposite corner: /retromon zone corner2"), false);
							return 1;
						}))
						.then(Commands.literal("corner2").executes(c -> {
							Game.corner(c.getSource().getPlayerOrException(), 2);
							c.getSource().sendSuccess(() -> Component.literal("Second corner marked. Now name it: /retromon zone set <place>"), false);
							return 1;
						}))
						.then(Commands.literal("set").then(Commands.argument("place", StringArgumentType.word()).suggests((c, b) -> {
							for (String k : Places.ALL.keySet()) b.suggest(k);
							return b.buildFuture();
						}).executes(c -> {
							ServerPlayer p = c.getSource().getPlayerOrException();
							String place = StringArgumentType.getString(c, "place");
							if (!Places.ALL.containsKey(place)) {
								c.getSource().sendFailure(Component.literal("No place called " + place));
								return 0;
							}
							if (!Game.markZone(p, place)) {
								c.getSource().sendFailure(Component.literal("Mark both corners first (/retromon zone corner1, corner2)"));
								return 0;
							}
							int n = Game.populate((ServerLevel) p.level(), place, false);
							c.getSource().sendSuccess(() -> Component.literal("That area is now " + Places.get(place).name()
									+ (n > 0 ? ", with " + n + " people and items from the games" : "")), false);
							return 1;
						})))
						.then(Commands.literal("clear").then(Commands.argument("place", StringArgumentType.word()).suggests((c, b) -> {
							for (Game.Zone z : Game.zones()) b.suggest(z.place());
							return b.buildFuture();
						}).executes(c -> {
							int n = Game.clearZones(StringArgumentType.getString(c, "place"));
							c.getSource().sendSuccess(() -> Component.literal("Cleared " + n + " area(s)"), false);
							return n;
						})))
						.then(Commands.literal("list").executes(c -> {
							for (Game.Zone z : Game.zones()) {
								c.getSource().sendSuccess(() -> Component.literal(z.place() + ": " + z.x1() + " " + z.z1() + " to " + z.x2() + " " + z.z2()), false);
							}
							return Game.zones().size();
						}))
						.then(Commands.literal("here").executes(c -> {
							ServerPlayer p = c.getSource().getPlayerOrException();
							String place = Game.placeAt(p.getX(), p.getZ());
							c.getSource().sendSuccess(() -> Component.literal(place == null ? "Not in any place yet" : "You're in " + place), false);
							return 1;
						})))
				.then(Commands.literal("go").then(Commands.argument("place", StringArgumentType.word()).suggests((c, b) -> {
					for (Game.Zone z : Game.zones()) b.suggest(z.place());
					return b.buildFuture();
				}).executes(c -> {
					ServerPlayer p = c.getSource().getPlayerOrException();
					Vec3 to = Game.centreOf(StringArgumentType.getString(c, "place"), (ServerLevel) p.level());
					if (to == null) return 0;
					p.teleportTo(to.x, to.y, to.z);
					return 1;
				})))
				.then(Commands.literal("list")
						.then(Commands.literal("trainers").then(Commands.argument("place", StringArgumentType.word()).suggests((c, b) -> {
							for (String k : Places.ALL.keySet()) b.suggest(k);
							return b.buildFuture();
						}).executes(c -> {
							String place = StringArgumentType.getString(c, "place");
							int n = 0;
							for (Trainers.Trainer t : Trainers.ALL.values()) {
								if (!t.place().startsWith(place)) continue;
								n++;
								StringBuilder team = new StringBuilder();
								for (Trainers.Member m : t.team()) team.append(team.isEmpty() ? "" : ", ").append(m.species()).append(" ").append(m.level());
								c.getSource().sendSuccess(() -> Component.literal(t.key() + ": " + t.title() + " (" + team + ")"), false);
							}
							return n;
						})))
						.then(Commands.literal("people").then(Commands.argument("place", StringArgumentType.word()).suggests((c, b) -> {
							for (String k : Places.ALL.keySet()) b.suggest(k);
							return b.buildFuture();
						}).executes(c -> {
							String place = StringArgumentType.getString(c, "place");
							int n = 0;
							for (Trainers.Person person : Trainers.PEOPLE.values()) {
								if (!person.place().startsWith(place)) continue;
								n++;
								String first = person.lines().isEmpty() ? "" : person.lines().get(0);
								c.getSource().sendSuccess(() -> Component.literal(person.key() + " (" + person.sprite() + "): " + first), false);
							}
							return n;
						})))
						.then(Commands.literal("pokemon").executes(c -> {
							for (Trainers.Static st : Trainers.STATICS.values()) {
								c.getSource().sendSuccess(() -> Component.literal(st.id() + ": " + st.species() + " L" + st.level() + ", " + st.place() + " (" + st.kind() + ")"), false);
							}
							return Trainers.STATICS.size();
						})))
				.then(Commands.literal("level").then(Commands.argument("level", IntegerArgumentType.integer(1, 100)).executes(c -> {
					ServerPlayer p = c.getSource().getPlayerOrException();
					int lv = IntegerArgumentType.getInteger(c, "level");
					PlayerData d = Game.data(p);
					for (Mon m : d.party) {
						m.level = lv;
						m.exp = m.species.growth().expAt(lv);
						for (String mv : m.species.movesAt(lv)) m.learn(mv);
						m.heal();
					}
					Game.sync(p);
					c.getSource().sendSuccess(() -> Component.literal("Your POKéMON are now level " + lv), false);
					return 1;
				})))
				.then(Commands.literal("give").then(Commands.argument("species", StringArgumentType.word()).suggests((c, b) -> {
					for (String s : Species.ALL.keySet()) b.suggest(s);
					return b.buildFuture();
				}).then(Commands.argument("level", IntegerArgumentType.integer(1, 100)).executes(c -> {
					ServerPlayer p = c.getSource().getPlayerOrException();
					Species s = Species.ALL.get(StringArgumentType.getString(c, "species"));
					if (s == null) return 0;
					PlayerData d = Game.data(p);
					d.add(new Mon(s, IntegerArgumentType.getInteger(c, "level"), new java.util.Random()));
					d.started = true;
					Game.sync(p);
					return 1;
				}))))
				.then(Commands.literal("item").then(Commands.argument("item", StringArgumentType.word()).suggests((c, b) -> {
					for (String s : Item.ALL.keySet()) b.suggest(s);
					return b.buildFuture();
				}).then(Commands.argument("count", IntegerArgumentType.integer(1, 999)).executes(c -> {
					ServerPlayer p = c.getSource().getPlayerOrException();
					String item = StringArgumentType.getString(c, "item");
					if (Item.find(item) == null) return 0;
					Game.data(p).give(item, IntegerArgumentType.getInteger(c, "count"));
					Game.sync(p);
					return 1;
				}))))
				.then(Commands.literal("money").then(Commands.argument("amount", IntegerArgumentType.integer(0, 999999)).executes(c -> {
					ServerPlayer p = c.getSource().getPlayerOrException();
					Game.data(p).money = IntegerArgumentType.getInteger(c, "amount");
					Game.sync(p);
					return 1;
				})))
				.then(Commands.literal("populate").then(Commands.argument("place", StringArgumentType.word()).suggests((c, b) -> {
					for (Game.Zone z : Game.zones()) b.suggest(z.place());
					return b.buildFuture();
				}).executes(c -> populate(c, false)).then(Commands.literal("redo").executes(c -> populate(c, true)))))
				.then(Commands.literal("build").then(Commands.literal("on").executes(c -> {
					Game.build = true;
					c.getSource().sendSuccess(() -> Component.literal("Map editing on: game modes and breaking blocks are allowed until /retromon build off"), false);
					return 1;
				})).then(Commands.literal("off").executes(c -> {
					Game.build = false;
					c.getSource().sendSuccess(() -> Component.literal("Map editing off: the map is locked again"), false);
					return 1;
				})))
				.then(Commands.literal("badge").then(Commands.argument("badge", StringArgumentType.word()).executes(c -> {
					ServerPlayer p = c.getSource().getPlayerOrException();
					String b = StringArgumentType.getString(c, "badge").toUpperCase();
					PlayerData d = Game.data(p);
					if (b.equals("ALL")) {
						for (Trainers.Trainer t : Trainers.ALL.values()) if (t.badge() != null) d.badges.add(t.badge());
					} else d.badges.add(b.endsWith("BADGE") ? b : b + "BADGE");
					Game.sync(p);
					c.getSource().sendSuccess(() -> Component.literal("Badges: " + String.join(", ", d.badges)), false);
					return 1;
				})))
				.then(Commands.literal("heal").executes(c -> {
					ServerPlayer p = c.getSource().getPlayerOrException();
					Game.data(p).healAll();
					Game.sync(p);
					return 1;
				}))
				.then(Commands.literal("reset").executes(c -> {
					ServerPlayer p = c.getSource().getPlayerOrException();
					p.setAttached(Game.DATA, new PlayerData());
					Game.join(p);
					c.getSource().sendSuccess(() -> Component.literal("Your adventure starts over"), false);
					return 1;
				}))));
	}
}
