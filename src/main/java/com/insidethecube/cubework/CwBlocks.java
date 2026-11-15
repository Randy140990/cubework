package com.insidethecube.cubework;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.insidethecube.cubework.block.ArchBlock;
import com.insidethecube.cubework.block.ArchCornerBlock;
import com.insidethecube.cubework.block.BorderedBlock;
import com.insidethecube.cubework.block.CornerPostBlock;
import com.insidethecube.cubework.block.CwButton;
import com.insidethecube.cubework.block.CwCarpet;
import com.insidethecube.cubework.block.CwFence;
import com.insidethecube.cubework.block.CwFenceGate;
import com.insidethecube.cubework.block.CwPressurePlate;
import com.insidethecube.cubework.block.CwSlab;
import com.insidethecube.cubework.block.CwStairs;
import com.insidethecube.cubework.block.CwTrapdoor;
import com.insidethecube.cubework.block.CwWall;
import com.insidethecube.cubework.block.GentleSlopeBlock;
import com.insidethecube.cubework.block.LayerBlock;
import com.insidethecube.cubework.block.PanelBlock;
import com.insidethecube.cubework.block.PartitionBlock;
import com.insidethecube.cubework.block.PatternBlock;
import com.insidethecube.cubework.block.PitchStairsBlock;
import com.insidethecube.cubework.block.PlainBlock;
import com.insidethecube.cubework.block.PostBlock;
import com.insidethecube.cubework.block.QuarterSlabBlock;
import com.insidethecube.cubework.block.RoundColumnBlock;
import com.insidethecube.cubework.block.RoundCornerBlock;
import com.insidethecube.cubework.block.RoundPillarBlock;
import com.insidethecube.cubework.block.ShapeEntity;
import com.insidethecube.cubework.block.SlopeBlock;
import com.insidethecube.cubework.block.StairCurveBlock;
import com.insidethecube.cubework.block.StepStairsBlock;
import com.insidethecube.cubework.block.ThinPillarShape;
import com.insidethecube.cubework.block.TrimBlock;
import com.insidethecube.cubework.block.VerticalLayerBlock;
import com.insidethecube.cubework.block.VerticalSlabBlock;
import com.insidethecube.cubework.block.WindowFrameBlock;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * Every Cubework shape block: one for each shape and behaviour (slab, slab_glass, slab_wool...), each
 * placed one keeping its material (see ShapeEntity), and its item likewise one for every material, the
 * material kept on the stack. Before 0.12 every material had blocks of its own (red_wool_slab), tens of
 * thousands of them, and the game took minutes to start; those ids are converted as worlds load (see
 * OLD and mixin.ConvertMixins).
 */
public final class CwBlocks {
	private CwBlocks() {}

	/** Which shape and behaviour a Cubework shape block is. */
	public record Info(Behaviour behaviour, Shape shape) {}

	/** A block id from before 0.12 (red_wool_slab): the block it is now and its material. */
	public record Old(Block block, Material material) {}

	/** An item's or a placed shape's material. */
	public static DataComponentType<Material> MATERIAL;
	public static BlockEntityType<ShapeEntity> SHAPE_ENTITY;

	private static final Map<Shape, Map<Behaviour, Block>> BLOCKS = new EnumMap<>(Shape.class);
	private static final Map<Block, Info> INFO = new IdentityHashMap<>();
	private static final Map<Block, Material> DEFAULTS = new IdentityHashMap<>();
	/** The full blocks (vanilla, or Cubework's patterned blocks and wallpapers), back to their material. */
	private static final Map<Block, Material> BASES = new IdentityHashMap<>();
	private static final Map<Material, Map<Shape, Block>> BY_MATERIAL = new LinkedHashMap<>();
	public static final Map<String, Old> OLD = new HashMap<>();

	/** The patterned full blocks, in the order of Material.ALL. */
	public static final List<Block> PATTERN_BLOCKS = new ArrayList<>();
	/** The wallpapers' full blocks. */
	public static final List<Block> WALLPAPER_BLOCKS = new ArrayList<>();

	public static void register() {
		MATERIAL = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Reg.id("material"),
				DataComponentType.<Material>builder().persistent(Material.CODEC).networkSynchronized(Material.STREAM_CODEC).build());

		// The patterned blocks and wallpapers: full blocks of their own, made like the block they're drawn on.
		for (Material m : Material.ALL) {
			if (!m.exists()) continue;
			if (m.patterned() || m.wallpaper()) {
				Block vanilla = m.vanillaBlock();
				BlockState def = vanilla.defaultBlockState();
				MapColor colour = def.getMapColor(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
				int light = def.getLightEmission();
				Block base = Reg.block(m.id(), BlockBehaviour.Properties.ofFullCopy(vanilla).mapColor(colour).lightLevel(state -> light),
						set -> new PatternBlock(m, set));
				Reg.blockItem(m.id(), base);
				if (m.patterned()) PATTERN_BLOCKS.add(base);
				else WALLPAPER_BLOCKS.add(base);
				if (m.source().endsWith("_wool")) FlammableBlockRegistry.getDefaultInstance().add(base, 30, 60);
				if (m.wallpaper()) FlammableBlockRegistry.getDefaultInstance().add(base, 5, 20);
				BASES.put(base, m);
			} else {
				BASES.put(m.base(), m);
			}
		}

		// One block for each shape and behaviour that some material has.
		List<Block> all = new ArrayList<>();
		for (Shape s : Shape.values()) {
			for (Behaviour b : Behaviour.values()) {
				Material first = null;
				for (Material m : Material.ALL) {
					if (m.exists() && m.behaviour() == b && s.appliesTo(m)) {
						first = m;
						break;
					}
				}
				if (first == null) continue;
				String name = b.idOf(s);
				Block block = Reg.block(name, settings(b, s), factory(b, s));
				Reg.blockItem(name, block, (bl, props) -> new ShapeItem(bl, s, props));
				BLOCKS.computeIfAbsent(s, k -> new EnumMap<>(Behaviour.class)).put(b, block);
				INFO.put(block, new Info(b, s));
				DEFAULTS.put(block, first);
				all.add(block);
				// Wool shapes catch fire and burn away like wool; coal shapes burn like coal blocks.
				switch (b) {
					case WOOL -> FlammableBlockRegistry.getDefaultInstance().add(block, 30, 60);
					case WALLPAPER -> FlammableBlockRegistry.getDefaultInstance().add(block, 5, 20);
					case COAL -> FlammableBlockRegistry.getDefaultInstance().add(block, 5, 5);
					default -> {
					}
				}
			}
		}
		SHAPE_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Reg.id("shape"),
				FabricBlockEntityTypeBuilder.create(ShapeEntity::new, all.toArray(new Block[0])).build());

		// Each material's shapes, and the ids they had before (red_wool_slab), for converting old worlds.
		for (Material m : Material.ALL) {
			if (!m.exists()) continue;
			Map<Shape, Block> shapes = new EnumMap<>(Shape.class);
			for (Shape s : Shape.values()) {
				if (!s.appliesTo(m)) continue;
				Block b = BLOCKS.get(s).get(m.behaviour());
				shapes.put(s, b);
				OLD.put(Cubework.ID + ":" + m.idOf(s), new Old(b, m));
			}
			BY_MATERIAL.put(m, shapes);
		}
	}

	private static BlockBehaviour.Properties settings(Behaviour b, Shape s) {
		// Map colour and light are fixed from the vanilla block, since some vanilla blocks work them out from
		// properties our shapes don't have. Each placed shape's own map colour comes from its material
		// (mixin.ShapeStateMixin).
		Block vanilla = b.modelBlock();
		BlockState def = vanilla.defaultBlockState();
		MapColor colour = def.getMapColor(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
		int light = def.getLightEmission();
		BlockBehaviour.Properties set = BlockBehaviour.Properties.ofFullCopy(vanilla).mapColor(colour).lightLevel(state -> light);
		// Copper weathers (vanilla copper decides this in code, so it isn't copied).
		if (b == Behaviour.COPPER) set.randomTicks();
		return switch (s) {
			case BUTTON, PRESSURE_PLATE -> set.noCollision().pushReaction(PushReaction.DESTROY);
			case TRAPDOOR -> set.noOcclusion().isValidSpawn((state, world, pos, type) -> false);
			case WALL, FENCE, FENCE_GATE -> set.forceSolidOn();
			default -> set;
		};
	}

	private static Function<BlockBehaviour.Properties, Block> factory(Behaviour b, Shape s) {
		BlockState base = b.modelBlock().defaultBlockState();
		return switch (s) {
			case STAIRS -> set -> new CwStairs(b, base, set);
			case STEPS_4, STEPS_6, STEPS_8 -> set -> new StepStairsBlock(b, base, s.stepCount(), set);
			case SLAB -> set -> new CwSlab(b, set);
			case VERTICAL_SLAB -> set -> new VerticalSlabBlock(b, set);
			case SLOPE -> set -> new SlopeBlock(b, set);
			case LAYER -> set -> new LayerBlock(b, set);
			case VERTICAL_LAYER -> set -> new VerticalLayerBlock(b, set);
			case CARPET -> set -> new CwCarpet(b, set);
			case PANEL -> set -> new PanelBlock(b, set);
			case POST -> set -> new PostBlock(b, set);
			case WALL -> set -> new CwWall(b, set);
			case FENCE -> set -> new CwFence(b, set);
			case FENCE_GATE -> set -> new CwFenceGate(b, WoodType.OAK, set);
			case TRAPDOOR -> set -> new CwTrapdoor(b, setType(b), set);
			case BUTTON -> set -> new CwButton(b, setType(b), 20, set);
			case PRESSURE_PLATE -> set -> new CwPressurePlate(b, setType(b), set);
			case PILLAR_12, PILLAR_8, PILLAR_4 -> set -> new ThinPillarShape(b, s.pillarWidth(), set);
			case QUARTER_SLAB -> set -> new QuarterSlabBlock(b, set);
			case GENTLE_SLOPE -> set -> new GentleSlopeBlock(b, set);
			case SLOPE_3 -> set -> new GentleSlopeBlock.Over3(b, set);
			case SLOPE_2 -> set -> new GentleSlopeBlock.Over2(b, set);
			case ARCH -> set -> new ArchBlock(b, set);
			case ARCH_CORNER -> set -> new ArchCornerBlock(b, set);
			case WINDOW_4, WINDOW_8, WINDOW_12, WINDOW_16, DIAMOND_WINDOW_4, DIAMOND_WINDOW_8, DIAMOND_WINDOW_12, DIAMOND_WINDOW_16,
					ROUND_WINDOW_4, ROUND_WINDOW_8, ROUND_WINDOW_12, ROUND_WINDOW_16 ->
					set -> new WindowFrameBlock(b, s.windowDepth(), s.diamond() ? WindowFrameBlock.Style.DIAMOND
							: s.round() ? WindowFrameBlock.Style.ROUND : WindowFrameBlock.Style.SQUARE, set);
			case STAIRS_2 -> set -> new PitchStairsBlock.Over2(b, set);
			case STAIRS_3 -> set -> new PitchStairsBlock.Over3(b, set);
			case STAIRS_4 -> set -> new PitchStairsBlock(b, set);
			case TRIM_2, TRIM_4, TRIM_8 -> set -> new TrimBlock(b, s.trimWidth(), set);
			case STAIR_CURVE -> set -> new StairCurveBlock(b, set);
			case BORDERED -> set -> new BorderedBlock(b, set);
			case RAISED_FRAME -> set -> new PlainBlock(b, set);
			case PARTITION_2, PARTITION_4, PARTITION_6, PARTITION_8 -> set -> new PartitionBlock(b, s.partitionWidth(), set);
			case ROUND_PILLAR_2, ROUND_PILLAR_3, ROUND_PILLAR_4 -> set -> new RoundPillarBlock(b, s.pillarSize(), set);
			case ROUND_COLUMN_16, ROUND_COLUMN_12, ROUND_COLUMN_8, ROUND_COLUMN_4 -> set -> new RoundColumnBlock(b, s.columnWidth(), set);
			case CORNER_POST -> set -> new CornerPostBlock(b, set);
			case ROUND_CORNER -> set -> new RoundCornerBlock(b, false, set);
			case ROUND_CORNER_INNER -> set -> new RoundCornerBlock(b, true, set);
		};
	}

	/** Copper trapdoors, buttons and plates sound like copper; the rest like stone. All open by hand. */
	private static BlockSetType setType(Behaviour b) {
		return b == Behaviour.COPPER ? BlockSetType.COPPER : BlockSetType.STONE;
	}

	/** The block for a material's shape (null when the material doesn't come in it). */
	public static Block get(Material m, Shape s) {
		Map<Shape, Block> shapes = BY_MATERIAL.get(m);
		return shapes == null ? null : shapes.get(s);
	}

	/** A stack of one of a material's shapes (empty when the material doesn't come in it). */
	public static ItemStack stack(Material m, Shape s) {
		Block b = get(m, s);
		if (b == null) return ItemStack.EMPTY;
		ItemStack stack = new ItemStack(b);
		stack.set(MATERIAL, m);
		return stack;
	}

	/** The material's shapes (none when this Minecraft version doesn't have the block). */
	public static Map<Shape, Block> shapesOf(Material m) {
		return BY_MATERIAL.getOrDefault(m, Map.of());
	}

	/** The Cubework shape block's shape and behaviour, or null for any other block. */
	public static Info info(Block b) {
		return INFO.get(b);
	}

	/** The material of a full block Cubework makes shapes of (vanilla, patterned or wallpaper), or null. */
	public static Material materialOf(Block b) {
		return BASES.get(b);
	}

	/** The material a shape block falls back to when a placed one or a stack has none: its first. */
	public static Material defaultMaterial(Block b) {
		return DEFAULTS.get(b);
	}

	/** A stack's material: a shape's, or the full block's; null for anything else. */
	public static Material materialOf(ItemStack stack) {
		if (stack.getItem() instanceof ShapeItem si) return si.material(stack);
		return stack.getItem() instanceof BlockItem bi ? BASES.get(bi.getBlock()) : null;
	}

	/** The shape of a stack, or null for anything that isn't a Cubework shape. */
	public static Shape shapeOf(ItemStack stack) {
		return stack.getItem() instanceof ShapeItem si ? si.shape() : null;
	}

	/** Places a shape and gives it its material (in the world, or in a chunk still being generated). */
	public static boolean place(LevelAccessor level, BlockPos pos, BlockState state, Material m, int flags) {
		boolean changed = level.setBlock(pos, state, flags);
		if (m != null && level.getBlockEntity(pos) instanceof ShapeEntity e) {
			if (e.material() != m) changed = true;
			e.setMaterial(m);
		}
		return changed;
	}

	public static Iterable<Block> all() {
		return INFO.keySet();
	}
}
