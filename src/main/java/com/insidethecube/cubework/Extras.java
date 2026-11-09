package com.insidethecube.cubework;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import com.insidethecube.cubework.block.ColouredDoorBlock;
import com.insidethecube.cubework.block.SizedCoralBlock;
import com.insidethecube.cubework.block.SizedPlantBlock;
import com.insidethecube.cubework.block.ThinLogBlock;

import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.fabricmc.fabric.api.registry.StrippableBlockRegistry;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSetType;
import net.minecraft.block.BlockState;
import net.minecraft.block.MapColor;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.TallBlockItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EmptyBlockView;

/** Thin logs, plants at other sizes, and doors in every colour. */
public final class Extras {
	private Extras() {}

	/** Log widths, in pixels. 16 is a full log whose ends show bark where other logs join it, to start a tapering trunk. */
	public static final int[] LOG_WIDTHS = {16, 12, 8, 4};
	/** Logs and stems; the ones with a stripped version are stripped by an axe, like full logs. */
	public static final String[] LOGS = {"oak_log", "spruce_log", "birch_log", "jungle_log", "acacia_log", "dark_oak_log", "mangrove_log",
			"cherry_log", "pale_oak_log", "crimson_stem", "warped_stem", "mushroom_stem"};

	/** Plant sizes, in percent. Low plants go a little taller than the rest. */
	public static final int[] LOW_SIZES = {50, 75, 125, 150, 175};
	public static final int[] PLANT_SIZES = {50, 75, 125, 150};
	public static final String[] LOW_PLANTS = {"short_grass", "fern", "short_dry_grass", "bush"};
	public static final String[] PLANTS = {"red_mushroom", "brown_mushroom", "crimson_fungus", "warped_fungus", "dandelion", "poppy", "blue_orchid",
			"allium", "azure_bluet", "red_tulip", "orange_tulip", "white_tulip", "pink_tulip", "oxeye_daisy", "cornflower", "lily_of_the_valley",
			"torchflower", "wither_rose"};

	/** Coral and coral fans (living ones die out of water into the dead ones of the same size). */
	public static final String[] CORALS = {"tube", "brain", "bubble", "fire", "horn"};
	/** Coral sizes, in percent (fans can't be drawn bigger than 150%). */
	public static final int[] CORAL_SIZES = {50, 75, 125, 150};

	/** Metals and gems the copper door's pattern comes in, from their blocks' colours; and what crafts each. */
	public static final String[] METAL_DOORS = {"iron", "gold", "redstone", "emerald", "lapis", "diamond", "netherite"};

	/** Woods whose door pattern comes in the 16 colours. */
	public static final String[] DOOR_WOODS = {"oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "pale_oak", "bamboo",
			"crimson", "warped"};

	public static final List<Block> THIN_LOGS = new ArrayList<>();
	public static final List<Block> SIZED_PLANTS = new ArrayList<>();
	public static final List<Block> CORAL = new ArrayList<>();
	/** Grass, fern and bush sizes, which take the biome's grass colour. */
	public static final List<Block> GRASSY_PLANTS = new ArrayList<>();
	public static final List<Block> DOORS = new ArrayList<>();

	public static void register() {
		logs();
		plants();
		coral();
		doors();
	}

	private static void logs() {
		List<String> all = new ArrayList<>();
		for (String log : LOGS) {
			all.add(log);
			if (!log.equals("mushroom_stem")) all.add("stripped_" + log);
		}
		for (String log : all) {
			Block base = vanilla(log);
			boolean burns = !log.contains("crimson") && !log.contains("warped");
			for (int w : LOG_WIDTHS) {
				Block b = block(log + "_" + w, settings(base), set -> new ThinLogBlock(w, set));
				THIN_LOGS.add(b);
				if (burns) FlammableBlockRegistry.getDefaultInstance().add(b, 5, 5);
			}
		}
		for (String log : LOGS) {
			if (log.equals("mushroom_stem")) continue;
			for (int w : LOG_WIDTHS) StrippableBlockRegistry.register(get(log + "_" + w), get("stripped_" + log + "_" + w));
		}
	}

	private static void plants() {
		for (String p : LOW_PLANTS) for (int size : LOW_SIZES) plant(p, size);
		for (String p : PLANTS) for (int size : PLANT_SIZES) plant(p, size);
	}

	private static void plant(String p, int size) {
		Block base = vanilla(p);
		SizedPlantBlock.Soil soil = p.equals("short_dry_grass") ? SizedPlantBlock.Soil.DRY
				: p.endsWith("mushroom") ? SizedPlantBlock.Soil.SOLID
				: p.endsWith("fungus") ? SizedPlantBlock.Soil.NETHER : SizedPlantBlock.Soil.DIRT;
		Block b = block(p + "_" + size, settings(base), set -> new SizedPlantBlock(soil, size / 100.0, set));
		SIZED_PLANTS.add(b);
		if (p.equals("short_grass") || p.equals("fern") || p.equals("bush")) GRASSY_PLANTS.add(b);
		if (soil == SizedPlantBlock.Soil.DIRT || soil == SizedPlantBlock.Soil.DRY) FlammableBlockRegistry.getDefaultInstance().add(b, 60, 100);
	}

	private static void coral() {
		for (String c : CORALS) {
			for (boolean fan : new boolean[] {false, true}) {
				String live = c + "_coral" + (fan ? "_fan" : ""), dead = "dead_" + live;
				for (int size : CORAL_SIZES) {
					String deadId = dead + "_" + size;
					CORAL.add(block(live + "_" + size, settings(vanilla(live)), set -> new SizedCoralBlock(size / 100.0, fan, () -> get(deadId), set)));
				}
				for (int size : CORAL_SIZES) {
					CORAL.add(block(dead + "_" + size, settings(vanilla(dead)), set -> new SizedCoralBlock(size / 100.0, fan, null, set)));
				}
			}
		}
	}

	private static void doors() {
		for (String wood : DOOR_WOODS) {
			Block base = vanilla(wood + "_door");
			BlockSetType type = BlockSetType.stream().filter(t -> t.name().equals(wood)).findFirst().orElse(BlockSetType.OAK);
			for (String colour : Material.COLOURS) {
				MapColor map = DyeColor.valueOf(colour.toUpperCase(Locale.ROOT)).getMapColor();
				DOORS.add(block(colour + "_" + wood + "_door", AbstractBlock.Settings.copy(base).mapColor(map), set -> new ColouredDoorBlock(type, set), true));
			}
		}
		// The iron door in the 16 colours: still opened only by redstone.
		Block iron = vanilla("iron_door");
		for (String colour : Material.COLOURS) {
			MapColor map = DyeColor.valueOf(colour.toUpperCase(Locale.ROOT)).getMapColor();
			DOORS.add(block(colour + "_iron_door", AbstractBlock.Settings.copy(iron).mapColor(map), set -> new ColouredDoorBlock(BlockSetType.IRON, set), true));
		}
		// The copper door's pattern in metals and gems: opened by hand, like copper doors.
		Block copper = vanilla("copper_door");
		for (String metal : METAL_DOORS) {
			MapColor map = vanilla(metal + "_block").getDefaultMapColor();
			DOORS.add(block(metal + "_door", AbstractBlock.Settings.copy(copper).mapColor(map), set -> new ColouredDoorBlock(BlockSetType.COPPER, set), true));
		}
	}

	private static Block vanilla(String id) {
		return Registries.BLOCK.get(Identifier.ofVanilla(id));
	}

	public static Block get(String id) {
		return Registries.BLOCK.get(Identifier.of(Cubework.ID, id));
	}

	/** The vanilla block's settings, with its map colour and light fixed (some work them out from properties). */
	private static AbstractBlock.Settings settings(Block base) {
		BlockState def = base.getDefaultState();
		MapColor colour = def.getMapColor(EmptyBlockView.INSTANCE, BlockPos.ORIGIN);
		int light = def.getLuminance();
		return AbstractBlock.Settings.copy(base).mapColor(colour).luminance(state -> light);
	}

	private static Block block(String name, AbstractBlock.Settings settings, Function<AbstractBlock.Settings, Block> factory) {
		return block(name, settings, factory, false);
	}

	/** Doors use the two-block item, which checks there's room for the top half, like vanilla doors. */
	private static Block block(String name, AbstractBlock.Settings settings, Function<AbstractBlock.Settings, Block> factory, boolean tall) {
		RegistryKey<Block> key = RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(Cubework.ID, name));
		Block b = Registry.register(Registries.BLOCK, key, factory.apply(settings.registryKey(key)));
		RegistryKey<Item> ik = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Cubework.ID, name));
		Item.Settings is = new Item.Settings().registryKey(ik).useBlockPrefixedTranslationKey();
		Registry.register(Registries.ITEM, ik, tall ? new TallBlockItem(b, is) : new BlockItem(b, is));
		return b;
	}
}
