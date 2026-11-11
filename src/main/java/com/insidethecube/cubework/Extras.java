package com.insidethecube.cubework;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import com.insidethecube.cubework.block.ColouredDoorBlock;
import com.insidethecube.cubework.block.SizedCoralBlock;
import com.insidethecube.cubework.block.SizedPlantBlock;
import com.insidethecube.cubework.block.TableBlock;
import com.insidethecube.cubework.block.ThinLogBlock;

import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
//#if MC < 26.3
import net.fabricmc.fabric.api.registry.StrippableBlockRegistry;
//#endif
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.material.MapColor;

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

	/**
	 * Table tops: terracotta (plain and 16 colours), glazed terracotta, stripped woods, and smooth stones,
	 * tuff, resin and quartz. Every top comes with oak legs and with legs in each concrete colour.
	 */
	public static final String[] TABLE_TOPS = tableTops();
	public static final List<Block> TABLES = new ArrayList<>();

	private static String[] tableTops() {
		List<String> out = new ArrayList<>();
		out.add("terracotta");
		for (String c : Material.COLOURS) out.add(c + "_terracotta");
		for (String c : Material.COLOURS) out.add(c + "_glazed_terracotta");
		for (String w : new String[] {"oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "pale_oak"}) out.add("stripped_" + w + "_wood");
		out.addAll(List.of("stripped_crimson_hyphae", "stripped_warped_hyphae", "stripped_bamboo_block", "smooth_stone", "polished_granite",
				"polished_diorite", "polished_andesite", "chiseled_deepslate", "tuff", "polished_tuff", "chiseled_tuff", "chiseled_resin_bricks",
				"quartz_block", "chiseled_quartz_block", "quartz_bricks", "smooth_quartz"));
		return out.toArray(String[]::new);
	}

	/** Woods whose door pattern comes in the 16 colours. */
	public static final String[] DOOR_WOODS = {"oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "pale_oak", "bamboo",
			"crimson", "warped"};

	public static final List<Block> THIN_LOGS = new ArrayList<>();
	public static final List<Block> SIZED_PLANTS = new ArrayList<>();
	public static final List<Block> CORAL = new ArrayList<>();
	/** Each thin log's stripped one (an axe strips them as it strips logs). */
	public static final java.util.Map<Block, Block> STRIPPED = new java.util.IdentityHashMap<>();
	/** Grass, fern and bush sizes, which take the biome's grass colour. */
	public static final List<Block> GRASSY_PLANTS = new ArrayList<>();
	public static final List<Block> DOORS = new ArrayList<>();

	public static void register() {
		logs();
		plants();
		coral();
		doors();
		tables();
	}

	private static void tables() {
		for (String top : TABLE_TOPS) {
			if (!Reg.exists(top)) continue;
			Block base = vanilla(top);
			boolean wood = top.startsWith("stripped_");
			for (String legs : tableLegs()) {
				String name = top + "_table" + (legs.equals("oak") ? "" : "_" + legs + "_legs");
				Block b = block(name, settings(base).noOcclusion(), set -> new TableBlock(legs, set));
				TABLES.add(b);
				if (wood && !top.contains("crimson") && !top.contains("warped")) FlammableBlockRegistry.getDefaultInstance().add(b, 5, 20);
			}
		}
	}

	/** Oak legs, then legs in each concrete colour. */
	public static List<String> tableLegs() {
		List<String> out = new ArrayList<>();
		out.add("oak");
		out.addAll(List.of(Material.COLOURS));
		return out;
	}

	private static void logs() {
		List<String> all = new ArrayList<>();
		for (String log : LOGS) {
			all.add(log);
			if (!log.equals("mushroom_stem")) all.add("stripped_" + log);
		}
		for (String log : all) {
			if (!Reg.exists(log)) continue;
			Block base = vanilla(log);
			boolean burns = !log.contains("crimson") && !log.contains("warped");
			for (int w : LOG_WIDTHS) {
				Block b = block(log + "_" + w, settings(base), set -> new ThinLogBlock(w, set));
				THIN_LOGS.add(b);
				if (burns) FlammableBlockRegistry.getDefaultInstance().add(b, 5, 5);
			}
		}
		for (String log : LOGS) {
			if (log.equals("mushroom_stem") || !Reg.exists(log)) continue;
			for (int w : LOG_WIDTHS) {
				STRIPPED.put(get(log + "_" + w), get("stripped_" + log + "_" + w));
				//#if MC < 26.3
				StrippableBlockRegistry.register(get(log + "_" + w), get("stripped_" + log + "_" + w));
				//#endif
			}
		}
	}

	private static void plants() {
		for (String p : LOW_PLANTS) for (int size : LOW_SIZES) plant(p, size);
		for (String p : PLANTS) for (int size : PLANT_SIZES) plant(p, size);
	}

	private static void plant(String p, int size) {
		if (!Reg.exists(p)) return;
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
			if (!Reg.exists(wood + "_door")) continue;
			Block base = vanilla(wood + "_door");
			BlockSetType type = BlockSetType.values().filter(t -> t.name().equals(wood)).findFirst().orElse(BlockSetType.OAK);
			for (String colour : Material.COLOURS) {
				MapColor map = DyeColor.valueOf(colour.toUpperCase(Locale.ROOT)).getMapColor();
				DOORS.add(block(colour + "_" + wood + "_door", BlockBehaviour.Properties.ofFullCopy(base).mapColor(map), set -> new ColouredDoorBlock(type, set), true));
			}
		}
		// The iron door in the 16 colours: still opened only by redstone.
		Block iron = vanilla("iron_door");
		for (String colour : Material.COLOURS) {
			MapColor map = DyeColor.valueOf(colour.toUpperCase(Locale.ROOT)).getMapColor();
			DOORS.add(block(colour + "_iron_door", BlockBehaviour.Properties.ofFullCopy(iron).mapColor(map), set -> new ColouredDoorBlock(BlockSetType.IRON, set), true));
		}
		// The copper door's pattern in metals and gems: opened by hand, like copper doors.
		Block copper = vanilla("copper_door");
		for (String metal : METAL_DOORS) {
			MapColor map = vanilla(metal + "_block").defaultMapColor();
			DOORS.add(block(metal + "_door", BlockBehaviour.Properties.ofFullCopy(copper).mapColor(map), set -> new ColouredDoorBlock(BlockSetType.COPPER, set), true));
		}
	}

	private static Block vanilla(String id) {
		return Reg.vanilla(id);
	}

	public static Block get(String id) {
		return Reg.block(Reg.id(id));
	}

	/** The vanilla block's settings, with its map colour and light fixed (some work them out from properties). */
	private static BlockBehaviour.Properties settings(Block base) {
		BlockState def = base.defaultBlockState();
		MapColor colour = def.getMapColor(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
		int light = def.getLightEmission();
		return BlockBehaviour.Properties.ofFullCopy(base).mapColor(colour).lightLevel(state -> light);
	}

	private static Block block(String name, BlockBehaviour.Properties settings, Function<BlockBehaviour.Properties, Block> factory) {
		return block(name, settings, factory, false);
	}

	/** Doors use the two-block item, which checks there's room for the top half, like vanilla doors. */
	private static Block block(String name, BlockBehaviour.Properties settings, Function<BlockBehaviour.Properties, Block> factory, boolean tall) {
		Block b = Reg.block(name, settings, factory);
		if (tall) Reg.blockItem(name, b, DoubleHighBlockItem::new);
		else Reg.blockItem(name, b);
		return b;
	}
}
