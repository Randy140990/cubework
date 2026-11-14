package com.insidethecube.cubework;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.insidethecube.cubework.net.Payloads;
import com.insidethecube.cubework.tool.BuildersGuideItem;
import com.insidethecube.cubework.tool.TerrainBrushItem;
import com.insidethecube.cubework.world.Config;
import com.insidethecube.cubework.world.SmoothTerrainFeature;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/** Cubework by Inside the Cube: vanilla blocks in every shape. */
public final class Cubework implements ModInitializer {
	public static final String ID = "cubework";

	/** The Inside the Cube block, only used as the first tab's icon. */
	public static Item CUBE;
	public static Item TERRAIN_BRUSH, BUILDERS_GUIDE;

	@Override
	public void onInitialize() {
		Config.load();
		CwBlocks.register();
		Extras.register();
		CUBE = Reg.item("cube", Item::new);
		TERRAIN_BRUSH = Reg.item("terrain_brush", p -> new TerrainBrushItem(p.stacksTo(1)));
		BUILDERS_GUIDE = Reg.item("builders_guide", p -> new BuildersGuideItem(p.stacksTo(1)));
		Payloads.register();

		// Smoothing new terrain (only when switched on in config/cubework.json).
		Registry.register(BuiltInRegistries.FEATURE, Reg.id("smooth_terrain"), new SmoothTerrainFeature());
		BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Decoration.TOP_LAYER_MODIFICATION,
				ResourceKey.create(Registries.PLACED_FEATURE, Reg.id("smooth_terrain")));

		// Fourteen tabs, so everything is easy to find: the coloured blocks in three, building blocks, copper and
		// metals, nature, logs, plants, doors and trapdoors, tables, patterns, carpets, lights, and office pieces.
		// Carpets and the office shapes (bordered blocks, raised frames, partitions) are only in their own tabs.
		tab("wool_concrete", () -> new ItemStack(item("light_blue_concrete_stairs")), add -> {
			add.accept(null);
			for (Material m : Material.ALL) {
				Material.Kind k = m.kind();
				if (k == Material.Kind.WOOL || k == Material.Kind.CONCRETE || k == Material.Kind.POWDER) shapes(m, add, false);
			}
		});
		tab("terracotta", () -> new ItemStack(item("magenta_glazed_terracotta_stairs")), add -> {
			for (Material m : Material.ALL) if (m.kind() == Material.Kind.TERRACOTTA || m.kind() == Material.Kind.GLAZED) shapes(m, add, false);
		});
		tab("glass", () -> new ItemStack(item("cyan_stained_glass_arch")), add -> {
			for (Material m : Material.ALL) if (m.glass()) shapes(m, add, false);
		});
		tab("building", () -> new ItemStack(item("stone_brick_arch")), add -> {
			for (String id : Material.BUILDING) if (!Material.byId(id).metal()) shapes(Material.byId(id), add, false);
		});
		tab("metals", () -> new ItemStack(item("copper_round_column_16")), add -> {
			for (String id : Material.BUILDING) if (Material.byId(id).metal()) shapes(Material.byId(id), add, false);
		});
		tab("nature", () -> new ItemStack(item("grass_block_stairs")), add -> {
			for (Material m : Material.ALL) if (m.nature()) shapes(m, add, true);
		});
		tab("logs", () -> new ItemStack(item("round_oak_log_12")), add -> {
			Extras.THIN_LOGS.forEach(add);
			Extras.ROUND_LOGS.forEach(add);
		});
		tab("plants", () -> new ItemStack(item("fern_175")), add -> {
			Extras.SIZED_PLANTS.forEach(add);
			Extras.CORAL.forEach(add);
		});
		tab("doors", () -> new ItemStack(item("light_blue_mangrove_door")), add -> {
			Extras.DOORS.forEach(add);
			Extras.TRAPDOORS.forEach(add);
		});
		tab("tables", () -> new ItemStack(item("cyan_glazed_terracotta_table")), add -> Extras.TABLES.forEach(add));
		tab("patterns", () -> new ItemStack(item("herringbone_stone_bricks")), add -> {
			for (Material m : Material.ALL) {
				if (!m.patterned() || CwBlocks.shapesOf(m).isEmpty()) continue;
				add.accept(m.base());
				shapes(m, add, false);
			}
		});
		// Every carpet: coloured, glazed (they turn, to line up the patterns), building, natural and patterned.
		tab("carpets", () -> new ItemStack(item("cyan_glazed_terracotta_carpet")), add -> {
			for (Material m : Material.ALL) {
				Block b = CwBlocks.get(m, Shape.CARPET);
				if (b != null) add.accept(b);
			}
		});
		tab("lights", () -> new ItemStack(item("sea_lantern_panel_light_12_4")), add -> Extras.LIGHT_FITTINGS.forEach(add));
		// Wallpapers and their shapes, then every material's bordered block, raised frame and partitions.
		tab("office", () -> new ItemStack(item("yellow_wallpaper_bordered")), add -> {
			for (Material m : Material.ALL) {
				if (!m.wallpaper() || CwBlocks.shapesOf(m).isEmpty()) continue;
				add.accept(m.base());
				Map<Shape, Block> shapes = CwBlocks.shapesOf(m);
				for (Shape sh : Shape.values()) if (shapes.containsKey(sh)) add.accept(shapes.get(sh));
			}
			for (Shape sh : new Shape[] {Shape.BORDERED, Shape.RAISED_FRAME, Shape.PARTITION_2, Shape.PARTITION_4, Shape.PARTITION_6, Shape.PARTITION_8}) {
				for (Material m : Material.ALL) {
					if (m.wallpaper()) continue;
					Block b = CwBlocks.get(m, sh);
					if (b != null) add.accept(b);
				}
			}
		});
	}

	/** One material's shapes, always in the same order (in the Nature tab, only the land-shaping ones). */
	private static void shapes(Material m, Consumer<Block> add, boolean natureOnly) {
		Map<Shape, Block> shapes = CwBlocks.shapesOf(m);
		for (Shape s : Shape.values()) {
			Block b = shapes.get(s);
			// Carpets and the office shapes have tabs of their own.
			if (b != null && s != Shape.CARPET && !s.officeShape() && (!natureOnly || s.natureShape())) add.accept(b);
		}
	}

	private static void tab(String name, Supplier<ItemStack> icon, Consumer<Consumer<Block>> fill) {
		CreativeModeTab group = FabricItemGroup.builder()
				.title(Component.translatable("itemGroup." + ID + "." + name))
				.icon(icon)
				.displayItems((context, entries) -> fill.accept(b -> {
					// null stands for the tools, at the start of the first tab.
					if (b == null) {
						entries.accept(TERRAIN_BRUSH);
						entries.accept(BUILDERS_GUIDE);
					} else {
						entries.accept(b);
					}
				}))
				.build();
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, ResourceKey.create(Registries.CREATIVE_MODE_TAB, Reg.id(name)), group);
	}

	private static Item item(String id) {
		return Reg.item(Reg.id(id));
	}

}
