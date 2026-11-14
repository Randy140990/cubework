package com.insidethecube.orbitarena.match;

import java.util.ArrayList;
import java.util.List;

import com.insidethecube.orbitarena.OrbitArena;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Everything put away when you enter the arena: your inventory (armour and off hand too), where you were,
 * your game mode, health and hunger. It's saved with the player (so closing the game mid-fight loses
 * nothing) and given back when you leave.
 */
public record Loadout(List<ItemStack> items, ResourceKey<Level> dimension, Vec3 pos, float yRot, float xRot, int gameMode, float health, int food,
		float saturation, int xpLevel, float xpProgress) {
	public static final Codec<Loadout> CODEC = RecordCodecBuilder.create(i -> i.group(
			ItemStack.OPTIONAL_CODEC.listOf().fieldOf("items").forGetter(Loadout::items),
			Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(Loadout::dimension),
			Vec3.CODEC.fieldOf("pos").forGetter(Loadout::pos),
			Codec.FLOAT.fieldOf("yaw").forGetter(Loadout::yRot),
			Codec.FLOAT.fieldOf("pitch").forGetter(Loadout::xRot),
			Codec.INT.fieldOf("game_mode").forGetter(Loadout::gameMode),
			Codec.FLOAT.fieldOf("health").forGetter(Loadout::health),
			Codec.INT.fieldOf("food").forGetter(Loadout::food),
			Codec.FLOAT.fieldOf("saturation").forGetter(Loadout::saturation),
			Codec.INT.optionalFieldOf("xp_level", 0).forGetter(Loadout::xpLevel),
			Codec.FLOAT.optionalFieldOf("xp_progress", 0f).forGetter(Loadout::xpProgress)
	).apply(i, Loadout::new));

	public static final AttachmentType<Loadout> SAVED = AttachmentRegistry.create(Identifier.fromNamespaceAndPath(OrbitArena.ID, "saved_loadout"),
			b -> b.persistent(CODEC));

	/** Puts the player's things away (saved with the player) and empties their inventory. */
	public static void store(ServerPlayer p) {
		Inventory inv = p.getInventory();
		List<ItemStack> items = new ArrayList<>();
		for (int i = 0; i < inv.getContainerSize(); i++) items.add(inv.getItem(i).copy());
		Loadout l = new Loadout(items, p.level().dimension(), p.position(), p.getYRot(), p.getXRot(), p.gameMode.getGameModeForPlayer().getId(),
				p.getHealth(), p.getFoodData().getFoodLevel(), p.getFoodData().getSaturationLevel(), p.experienceLevel, p.experienceProgress);
		p.setAttached(SAVED, l);
		inv.clearContent();
	}

	public static boolean has(ServerPlayer p) {
		return p.getAttached(SAVED) != null;
	}

	/** Gives everything back and sends the player home. */
	public static void restore(ServerPlayer p) {
		Loadout l = p.getAttached(SAVED);
		if (l == null) return;
		Inventory inv = p.getInventory();
		inv.clearContent();
		for (int i = 0; i < Math.min(inv.getContainerSize(), l.items.size()); i++) inv.setItem(i, l.items.get(i));
		p.setGameMode(GameType.byId(l.gameMode));
		p.removeAllEffects();
		p.setHealth(Math.max(1f, l.health));
		p.getFoodData().setFoodLevel(l.food);
		p.getFoodData().setSaturation(l.saturation);
		p.setExperienceLevels(l.xpLevel);
		p.experienceProgress = l.xpProgress;
		p.setRemainingFireTicks(0);
		p.fallDistance = 0;
		MinecraftServer server = p.level().getServer();
		ServerLevel home = server.getLevel(l.dimension);
		if (home == null) home = server.overworld();
		p.teleportTo(home, l.pos.x, l.pos.y, l.pos.z, java.util.Set.of(), l.yRot, l.xRot, true);
		p.removeAttached(SAVED);
	}
}
