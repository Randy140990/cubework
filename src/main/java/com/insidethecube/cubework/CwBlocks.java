package com.insidethecube.cubework;

import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import com.insidethecube.cubework.block.CwButton;
import com.insidethecube.cubework.block.CwCarpet;
import com.insidethecube.cubework.block.CwFence;
import com.insidethecube.cubework.block.CwFenceGate;
import com.insidethecube.cubework.block.CwPressurePlate;
import com.insidethecube.cubework.block.CwSlab;
import com.insidethecube.cubework.block.CwStairs;
import com.insidethecube.cubework.block.CwTrapdoor;
import com.insidethecube.cubework.block.CwWall;
import com.insidethecube.cubework.block.ArchBlock;
import com.insidethecube.cubework.block.ArchCornerBlock;
import com.insidethecube.cubework.block.GentleSlopeBlock;
import com.insidethecube.cubework.block.QuarterSlabBlock;
import com.insidethecube.cubework.block.WindowFrameBlock;
import com.insidethecube.cubework.block.CornerPostBlock;
import com.insidethecube.cubework.block.PatternBlock;
import com.insidethecube.cubework.block.PitchStairsBlock;
import com.insidethecube.cubework.block.StairCurveBlock;
import com.insidethecube.cubework.block.TrimBlock;
import com.insidethecube.cubework.block.RoundColumnBlock;
import com.insidethecube.cubework.block.RoundCornerBlock;
import com.insidethecube.cubework.block.LayerBlock;
import com.insidethecube.cubework.block.PanelBlock;
import com.insidethecube.cubework.block.PostBlock;
import com.insidethecube.cubework.block.SlopeBlock;
import com.insidethecube.cubework.block.ThinPillarBlock;
import com.insidethecube.cubework.block.VerticalLayerBlock;
import com.insidethecube.cubework.block.VerticalSlabBlock;

import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.fabricmc.fabric.api.registry.OxidizableBlocksRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/** Every Cubework block, made from the material and shape tables. */
public final class CwBlocks {
	private CwBlocks() {}

	/** Which material and shape a Cubework block is. */
	public record Info(Material material, Shape shape) {}

	private static final Map<Material, Map<Shape, Block>> BY_MATERIAL = new LinkedHashMap<>();
	private static final Map<Block, Info> INFO = new IdentityHashMap<>();
	/** The vanilla blocks, back to their material (for glass faces that meet). */
	private static final Map<Block, Material> BASES = new IdentityHashMap<>();

	/** The patterned full blocks, in the order of Material.ALL. */
	public static final java.util.List<Block> PATTERN_BLOCKS = new java.util.ArrayList<>();

	public static void register() {
		for (Material m : Material.ALL) {
			if (!m.exists()) continue;
			Block base;
			if (m.patterned()) {
				// A patterned block of its own, made like the block it's drawn on.
				Block vanilla = m.vanillaBlock();
				BlockState def = vanilla.defaultBlockState();
				MapColor colour = def.getMapColor(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
				int light = def.getLightEmission();
				base = block(m.id(), BlockBehaviour.Properties.ofFullCopy(vanilla).mapColor(colour).lightLevel(state -> light),
						set -> new PatternBlock(m, set));
				item(m.id(), base);
				PATTERN_BLOCKS.add(base);
				if (m.source().endsWith("_wool")) FlammableBlockRegistry.getDefaultInstance().add(base, 30, 60);
			} else {
				base = m.base();
			}
			BASES.put(base, m);
			Map<Shape, Block> shapes = new EnumMap<>(Shape.class);
			for (Shape s : Shape.values()) {
				if (!s.appliesTo(m)) continue;
				Block b = block(m.idOf(s), settings(m, base, s), factory(m, s, base));
				shapes.put(s, b);
				INFO.put(b, new Info(m, s));
				item(m.idOf(s), b);
				// Wool shapes catch fire and burn away like wool; coal shapes burn like coal blocks.
				if (m.kind() == Material.Kind.WOOL || (m.patterned() && m.source().endsWith("_wool"))) FlammableBlockRegistry.getDefaultInstance().add(b, 30, 60);
				if (m.id().equals("coal_block")) FlammableBlockRegistry.getDefaultInstance().add(b, 5, 5);
			}
			BY_MATERIAL.put(m, shapes);
		}
		// Copper shapes weather into the next age, take wax from honeycomb and lose it (and age) to an axe.
		for (Material m : Material.ALL) {
			if (!BY_MATERIAL.containsKey(m)) continue;
			Material next = m.nextAge(), waxed = m.waxed();
			for (Map.Entry<Shape, Block> e : BY_MATERIAL.get(m).entrySet()) {
				if (next != null) OxidizableBlocksRegistry.registerOxidizableBlockPair(e.getValue(), get(next, e.getKey()));
				if (waxed != null) OxidizableBlocksRegistry.registerWaxableBlockPair(e.getValue(), get(waxed, e.getKey()));
			}
		}
	}

	private static BlockBehaviour.Properties settings(Material m, Block base, Shape s) {
		// Map colour and light are fixed from the vanilla block, since some vanilla blocks work them out
		// from properties our shapes don't have (a log's axis, redstone ore being lit). Patterns copy the
		// vanilla block they're drawn on.
		Block vanilla = m.patterned() ? m.vanillaBlock() : base;
		BlockState def = vanilla.defaultBlockState();
		MapColor colour = def.getMapColor(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
		int light = def.getLightEmission();
		BlockBehaviour.Properties set = BlockBehaviour.Properties.ofFullCopy(vanilla).mapColor(colour).lightLevel(state -> light);
		// Copper that can still weather gets random ticks (vanilla copper decides this in code, so it isn't copied).
		if (m.copperAge() >= 0 && m.copperAge() < 3) set.randomTicks();
		return switch (s) {
			//#if MC >= 1.21.9
			case BUTTON, PRESSURE_PLATE -> set.noCollision().pushReaction(PushReaction.DESTROY);
			//#else
			case BUTTON, PRESSURE_PLATE -> set.noCollission().pushReaction(PushReaction.DESTROY);
			//#endif
			case TRAPDOOR -> set.noOcclusion().isValidSpawn((state, world, pos, type) -> false);
			case WALL, FENCE, FENCE_GATE -> set.forceSolidOn();
			default -> set;
		};
	}

	private static Function<BlockBehaviour.Properties, Block> factory(Material m, Shape s, Block base) {
		return switch (s) {
			case STAIRS -> set -> new CwStairs(m, base.defaultBlockState(), set);
			case SLAB -> set -> new CwSlab(m, set);
			case VERTICAL_SLAB -> set -> new VerticalSlabBlock(m, set);
			case SLOPE -> set -> new SlopeBlock(m, set);
			case LAYER -> set -> new LayerBlock(m, set);
			case VERTICAL_LAYER -> set -> new VerticalLayerBlock(m, set);
			case CARPET -> set -> new CwCarpet(m, set);
			case PANEL -> set -> new PanelBlock(m, set);
			case POST -> set -> new PostBlock(m, set);
			case WALL -> set -> new CwWall(m, set);
			case FENCE -> set -> new CwFence(m, set);
			case FENCE_GATE -> set -> new CwFenceGate(m, WoodType.OAK, set);
			case TRAPDOOR -> set -> new CwTrapdoor(m, setType(m), set);
			case BUTTON -> set -> new CwButton(m, setType(m), 20, set);
			case PRESSURE_PLATE -> set -> new CwPressurePlate(m, setType(m), set);
			case PILLAR_12, PILLAR_8, PILLAR_4 -> set -> new ThinPillarBlock(m, s.pillarWidth(), set);
			case QUARTER_SLAB -> set -> new QuarterSlabBlock(m, set);
			case GENTLE_SLOPE -> set -> new GentleSlopeBlock(m, set);
			case SLOPE_3 -> set -> new GentleSlopeBlock.Over3(m, set);
			case SLOPE_2 -> set -> new GentleSlopeBlock.Over2(m, set);
			case ARCH -> set -> new ArchBlock(m, set);
			case ARCH_CORNER -> set -> new ArchCornerBlock(m, set);
			case WINDOW_4, WINDOW_8, WINDOW_12, WINDOW_16, DIAMOND_WINDOW_4, DIAMOND_WINDOW_8, DIAMOND_WINDOW_12, DIAMOND_WINDOW_16,
					ROUND_WINDOW_4, ROUND_WINDOW_8, ROUND_WINDOW_12, ROUND_WINDOW_16 ->
					set -> new WindowFrameBlock(m, s.windowDepth(), s.diamond() ? WindowFrameBlock.Style.DIAMOND
							: s.round() ? WindowFrameBlock.Style.ROUND : WindowFrameBlock.Style.SQUARE, set);
			case STAIRS_2 -> set -> new PitchStairsBlock.Over2(m, set);
			case STAIRS_3 -> set -> new PitchStairsBlock.Over3(m, set);
			case STAIRS_4 -> set -> new PitchStairsBlock(m, set);
			case TRIM_2, TRIM_4, TRIM_8 -> set -> new TrimBlock(m, s.trimWidth(), set);
			case STAIR_CURVE -> set -> new StairCurveBlock(m, set);
			case ROUND_COLUMN_16, ROUND_COLUMN_12, ROUND_COLUMN_8 -> set -> new RoundColumnBlock(m, s.columnWidth(), set);
			case CORNER_POST -> set -> new CornerPostBlock(m, set);
			case ROUND_CORNER -> set -> new RoundCornerBlock(m, false, set);
			case ROUND_CORNER_INNER -> set -> new RoundCornerBlock(m, true, set);
		};
	}

	/** Copper trapdoors, buttons and plates sound like copper; the rest like stone. All open by hand. */
	private static BlockSetType setType(Material m) {
		return m.copper() ? BlockSetType.COPPER : BlockSetType.STONE;
	}

	private static Block block(String name, BlockBehaviour.Properties settings, Function<BlockBehaviour.Properties, Block> factory) {
		return Reg.block(name, settings, factory);
	}

	private static void item(String name, Block block) {
		Reg.blockItem(name, block);
	}

	public static Block get(Material m, Shape s) {
		Map<Shape, Block> shapes = BY_MATERIAL.get(m);
		return shapes == null ? null : shapes.get(s);
	}

	/** The material's shapes (none when this Minecraft version doesn't have the block). */
	public static Map<Shape, Block> shapesOf(Material m) {
		return BY_MATERIAL.getOrDefault(m, Map.of());
	}

	/** The Cubework block's material and shape, or null for any other block. */
	public static Info info(Block b) {
		return INFO.get(b);
	}

	/** The material of a Cubework shape or of the vanilla block it's made from, or null. */
	public static Material materialOf(Block b) {
		Info i = INFO.get(b);
		return i != null ? i.material() : BASES.get(b);
	}

	public static Iterable<Block> all() {
		return INFO.keySet();
	}
}
