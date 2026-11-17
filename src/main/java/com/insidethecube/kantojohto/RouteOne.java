package com.insidethecube.kantojohto;

import com.insidethecube.kantojohto.engine.Mon;
import com.insidethecube.kantojohto.engine.Species;
import com.insidethecube.kantojohto.game.Battles;
import com.insidethecube.kantojohto.game.Game;
import com.insidethecube.kantojohto.game.MonDisplay;
import com.insidethecube.kantojohto.game.Net;
import com.insidethecube.kantojohto.game.Npc;
import com.insidethecube.kantojohto.game.PlayerData;
import com.insidethecube.kantojohto.game.Trainers;
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

/** Route One (working name): Kanto and Johto as a 2.5D adventure on Insid3 Th3 Cub3's map. */
public class RouteOne implements ModInitializer {
	public static final String ID = "kantojohto";
	public static final Logger LOG = LoggerFactory.getLogger("Route One");
	private static final ResourceKey<EntityType<?>> NPC_KEY = ResourceKey.create(Registries.ENTITY_TYPE, id("npc"));
	private static final ResourceKey<EntityType<?>> MON_KEY = ResourceKey.create(Registries.ENTITY_TYPE, id("mon_display"));
	public static EntityType<Npc> NPC;
	public static EntityType<MonDisplay> MON_DISPLAY;

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
		FabricDefaultAttributeRegistry.register(NPC, Npc.createAttributes());
		Game.register();
		Net.register();
		ServerPlayNetworking.registerGlobalReceiver(Net.Act.TYPE, (payload, ctx) -> act(ctx.player(), payload));
		ServerLifecycleEvents.SERVER_STARTED.register(Game::serverStarted);
		ServerTickEvents.END_SERVER_TICK.register(Game::tick);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> Game.join(handler.player));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> Game.leave(handler.player));
		commands();
	}

	private static void act(ServerPlayer p, Net.Act a) {
		switch (a.kind()) {
			case "adventure" -> Game.setAdventure(p, a.data().getBooleanOr("on", false));
			case "talk" -> Game.talk(p, a.data().getIntOr("npc", -1));
			case "dialogue_done" -> Game.dialogueDone(p, a.data());
			case "battle" -> Battles.choose(p, a.data());
			case "battle_quit" -> {
				Battles.drop(p);
				Game.afterBattle(p);
			}
			case "buy" -> Game.buy(p, a.data());
			case "potion" -> Game.usePotion(p, a.data());
			case "starter" -> Game.starter(p, a.data());
			case "save" -> Game.save(p);
			default -> {
			}
		}
	}

	private static void commands() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) -> dispatcher.register(Commands.literal("routeone")
				.requires(s -> s.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER))
				.then(Commands.literal("place").then(Commands.argument("who", StringArgumentType.word()).suggests((c, b) -> {
					for (String s : new String[] {"nurse", "clerk", "youngster_joey", "leader_brock"}) b.suggest(s);
					return b.buildFuture();
				}).executes(c -> {
					ServerPlayer p = c.getSource().getPlayerOrException();
					String who = StringArgumentType.getString(c, "who");
					int role = who.equals("nurse") ? Npc.NURSE : who.equals("clerk") ? Npc.CLERK : who.startsWith("leader") ? Npc.LEADER : Npc.TRAINER;
					String key = role >= Npc.TRAINER ? who : "";
					if (role >= Npc.TRAINER && Trainers.get(key) == null) {
						c.getSource().sendFailure(Component.literal("No trainer called " + who));
						return 0;
					}
					// Face back the way you're looking from (so they face you as you put them down).
					float facing = Math.round((p.getYRot() + 180) / 90F) * 90F;
					Game.place((ServerLevel) p.level(), role, key, p.getX(), p.getY(), p.getZ(), facing);
					c.getSource().sendSuccess(() -> Component.literal("Placed " + who), false);
					return 1;
				})))
				.then(Commands.literal("remove").executes(c -> {
					ServerPlayer p = c.getSource().getPlayerOrException();
					Npc best = null;
					for (Npc n : p.level().getEntitiesOfClass(Npc.class, p.getBoundingBox().inflate(4))) if (best == null || n.distanceTo(p) < best.distanceTo(p)) best = n;
					if (best == null) return 0;
					best.discard();
					c.getSource().sendSuccess(() -> Component.literal("Removed"), false);
					return 1;
				}))
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
