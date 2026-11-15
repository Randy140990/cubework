package com.insidethecube.orbitarena.match;

import static com.insidethecube.orbitarena.arena.ArenaLayout.*;

import com.insidethecube.orbitarena.OrbitArena;
import com.insidethecube.orbitarena.entity.Fighter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

/** Who comes out to fight, kitted out for the difficulty, and where they appear. */
final class Spawns {
	private Spawns() {}

	private static final Identifier TOUGHER = Identifier.fromNamespaceAndPath(OrbitArena.ID, "tougher");
	private static final Identifier HARDER = Identifier.fromNamespaceAndPath(OrbitArena.ID, "harder");

	/** Mobs that join the waves as the difficulty rises (each list adds to the ones before). */
	private static final EntityType<?>[][] MOB_TIERS = {
			{EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER, EntityType.HUSK},
			{EntityType.STRAY, EntityType.CAVE_SPIDER, EntityType.ZOMBIE_VILLAGER, EntityType.BOGGED},
			{EntityType.VINDICATOR, EntityType.PILLAGER, EntityType.WITCH, EntityType.BLAZE},
			{EntityType.PIGLIN_BRUTE, EntityType.WITHER_SKELETON, EntityType.EVOKER, EntityType.BREEZE},
			{EntityType.RAVAGER, EntityType.PIGLIN_BRUTE, EntityType.WITHER_SKELETON}};

	static LivingEntity spawn(Match m, ServerLevel level, int index) {
		RandomSource rnd = level.random;
		int d = m.settings.difficulty();
		// The first one stands on the far pedestal; the rest come out round the far side and the tunnels.
		double x, z;
		if (index == 0) {
			x = m.centreX + PEDESTAL_X + 0.5;
			z = 0.5;
		} else {
			// Round the far side of the floor, clear of the dais and its ring of light.
			double a = Math.toRadians(-70 + rnd.nextDouble() * 140), r = 16 + rnd.nextDouble() * 28;
			// Not inside the far pedestal.
			if (Math.abs(Math.cos(a) * r - PEDESTAL_X) < 4.5 && Math.abs(Math.sin(a) * r) < 4.5) r += 9;
			x = m.centreX + Math.cos(a) * r;
			z = Math.sin(a) * r;
		}
		double y = index == 0 ? FLOOR + PEDESTAL_TOP : FLOOR + 1;
		Mob mob = switch (m.settings.opponents()) {
			case FIGHTERS -> fighter(level, d, rnd);
			case MOBS -> mob(level, d, rnd);
			case BOSS -> boss(level, m, rnd);
		};
		if (mob == null) return null;
		if (mob instanceof EnderDragon || mob instanceof WitherBoss) y = FLOOR + 14;
		mob.snapTo(x, y, z, 90, 0);
		mob.setPersistenceRequired();
		if (!(mob instanceof Fighter)) toughen(mob, d);
		level.addFreshEntity(mob);
		return mob;
	}

	/** Vanilla mobs get more health and hit harder as the difficulty rises. */
	private static void toughen(Mob mob, int d) {
		AttributeInstance hp = mob.getAttribute(Attributes.MAX_HEALTH);
		if (hp != null) {
			hp.addOrReplacePermanentModifier(new AttributeModifier(TOUGHER, 0.1 * (d - 1), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
			mob.setHealth(mob.getMaxHealth());
		}
		AttributeInstance dmg = mob.getAttribute(Attributes.ATTACK_DAMAGE);
		if (dmg != null) dmg.addOrReplacePermanentModifier(new AttributeModifier(HARDER, 0.06 * (d - 1), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
	}

	private static Mob mob(ServerLevel level, int d, RandomSource rnd) {
		int tiers = Math.min(MOB_TIERS.length, 1 + (d - 1) / 2);
		// Lean towards the toughest unlocked tier.
		int tier = Math.min(tiers - 1, (int) (Math.sqrt(rnd.nextDouble()) * tiers));
		EntityType<?>[] pick = MOB_TIERS[tier];
		EntityType<?> type = pick[rnd.nextInt(pick.length)];
		if (!(type.create(level, EntitySpawnReason.MOB_SUMMONED) instanceof Mob mob)) return null;
		mob.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.ZERO), EntitySpawnReason.MOB_SUMMONED, null);
		return mob;
	}

	private static Mob boss(ServerLevel level, Match m, RandomSource rnd) {
		Mob mob = switch (m.settings.boss()) {
			case WITHER -> EntityType.WITHER.create(level, EntitySpawnReason.MOB_SUMMONED);
			case WARDEN -> EntityType.WARDEN.create(level, EntitySpawnReason.MOB_SUMMONED);
			case ENDER_DRAGON -> EntityType.ENDER_DRAGON.create(level, EntitySpawnReason.MOB_SUMMONED);
		};
		if (mob instanceof EnderDragon dragon) dragon.setFightOrigin(new BlockPos(m.centreX, FLOOR, 0));
		return mob;
	}

	/** A fighter, armed and armoured for the difficulty, in one of five fighting styles. */
	private static Fighter fighter(ServerLevel level, int d, RandomSource rnd) {
		Fighter f = OrbitArena.FIGHTER.create(level, EntitySpawnReason.MOB_SUMMONED);
		if (f == null) return null;
		Item[] armour = switch (d) {
			case 1 -> new Item[] {Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS};
			case 2 -> new Item[] {Items.LEATHER_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.CHAINMAIL_BOOTS};
			case 3 -> new Item[] {Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS};
			case 4, 5 -> new Item[] {Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS};
			case 6, 7 -> new Item[] {Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS};
			default -> new Item[] {Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS};
		};
		int protection = d >= 10 ? 4 : d >= 9 ? 2 : d >= 7 ? 1 : 0;
		EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
		for (int i = 0; i < 4; i++) {
			ItemStack s = gear(armour[i]);
			if (protection > 0) ench(level, s, Enchantments.PROTECTION, protection);
			f.setItemSlot(slots[i], s);
		}
		int sharp = d >= 10 ? 5 : Math.max(0, d - 6);
		String tier = d <= 1 ? "wooden" : d == 2 ? "stone" : d <= 4 ? "iron" : d <= 7 ? "diamond" : "netherite";
		// Styles: sword and shield, sword, axe and shield, mace (from 4), spear.
		double roll = rnd.nextDouble();
		ItemStack main, off = ItemStack.EMPTY;
		if (roll < 0.35) {
			main = weapon(tier, "sword");
			off = gear(Items.SHIELD);
		} else if (roll < 0.5) {
			main = weapon(tier, "sword");
		} else if (roll < 0.7) {
			main = weapon(tier, "axe");
			off = gear(Items.SHIELD);
		} else if (roll < 0.82 && d >= 4) {
			main = gear(Items.MACE);
			if (d >= 6) ench(level, main, Enchantments.DENSITY, Math.min(5, d - 5));
		} else {
			main = weapon(tier, "spear");
			off = d >= 3 ? gear(Items.SHIELD) : ItemStack.EMPTY;
		}
		if (sharp > 0 && !main.is(Items.MACE)) ench(level, main, Enchantments.SHARPNESS, sharp);
		f.setItemSlot(EquipmentSlot.MAINHAND, main);
		f.setItemSlot(EquipmentSlot.OFFHAND, off);
		for (EquipmentSlot s : EquipmentSlot.values()) f.setDropChance(s, 0);
		ItemStack axe = d >= 5 && !main.getItem().toString().contains("axe") ? weapon(tier, "axe") : ItemStack.EMPTY;
		int apples = d >= 4 ? (d - 2) / 2 : 0;
		f.setup(d, apples, axe, rnd.nextInt(8));
		AttributeInstance hp = f.getAttribute(Attributes.MAX_HEALTH);
		if (hp != null && d >= 8) {
			hp.addOrReplacePermanentModifier(new AttributeModifier(TOUGHER, 4.0 * (d - 7), AttributeModifier.Operation.ADD_VALUE));
		}
		f.setHealth(f.getMaxHealth());
		return f;
	}

	private static ItemStack weapon(String tier, String kind) {
		Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(tier + "_" + kind));
		return gear(item);
	}

	private static ItemStack gear(Item item) {
		ItemStack s = new ItemStack(item);
		if (s.isDamageableItem()) s.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);
		return s;
	}

	private static void ench(ServerLevel level, ItemStack s, ResourceKey<Enchantment> key, int lvl) {
		Holder<Enchantment> e = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
		s.enchant(e, lvl);
	}
}
