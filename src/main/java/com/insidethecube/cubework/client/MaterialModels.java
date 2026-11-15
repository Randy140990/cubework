package com.insidethecube.cubework.client;

import java.io.Reader;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Predicate;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.insidethecube.cubework.Behaviour;
import com.insidethecube.cubework.CwBlocks;
import com.insidethecube.cubework.Material;
import com.insidethecube.cubework.Shape;
import com.insidethecube.cubework.block.Joining;
import com.insidethecube.cubework.block.Shared;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.client.model.loading.v1.BlockStateResolver;
import net.fabricmc.fabric.api.client.model.loading.v1.PreparableModelLoadingPlugin;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelDefinition;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.util.Util;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

/**
 * How shapes are drawn now that one block serves every material of its behaviour (see CwBlocks).
 * <p>
 * Each shape's look is written once per family of materials (plain, glass, nature...: materials whose shapes
 * are laid out alike, see Family) in assets/cubework/looks/{shape}/{family}.json, a blockstate file whose
 * models use four stand-in textures (cubework:block/slot/top, side, bottom and overlay). As a chunk is drawn,
 * each placed shape's material comes from its block entity, the family's look is drawn, and every quad's
 * texture is moved from the stand-in onto the material's own texture (its top, side...). Textures the looks
 * name outright (a bordered block's white lines, a partition's wooden cap) are left alone. Shapes drawn in
 * code (slopes, arches, round shapes) are made for each material the first time it's drawn.
 */
public final class MaterialModels {
	private MaterialModels() {}

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final FileToIdConverter LISTER = FileToIdConverter.json("looks");

	/** The stand-in textures, in slot order. */
	static final String[] SLOT_NAMES = {"cubework:block/slot/top", "cubework:block/slot/side", "cubework:block/slot/bottom", "cubework:block/slot/overlay"};
	static final int TOP = 0, SIDE = 1, BOTTOM = 2, OVERLAY = 3;

	/** Materials whose shapes are laid out alike: one look each (as tools/gen.py's families). */
	public enum Family {
		PLAIN, GLASS, GLAZED, NATURE, NATURE_GRASS, NATURE_SHORT, SQUEEZE;

		public final String id = name().toLowerCase(java.util.Locale.ROOT);

		/** The family of a material's shape: natural blocks' land shapes follow the ground, and so on. */
		public static Family of(Material m, Shape s) {
			boolean asBuilding = m.nature() && (!s.natureShape() || (m.building() && newNature(s)));
			if (m.nature() && !asBuilding) {
				if (m.textures().overlay() != null) return NATURE_GRASS;
				return m.height() == 15 ? NATURE_SHORT : NATURE;
			}
			if (m.glass()) return GLASS;
			if (s.pillarWidth() > 0) {
				String id = m.id();
				return id.contains("chiseled") || id.contains("smooth") || id.endsWith("pillar") ? SQUEEZE : PLAIN;
			}
			if (m.kind() == Material.Kind.GLAZED) return GLAZED;
			return PLAIN;
		}

		/** Natural blocks that are building blocks too keep the building versions of these (as gen.py's NEW_NATURE). */
		private static boolean newNature(Shape s) {
			return s == Shape.STAIRS || s == Shape.QUARTER_SLAB || s == Shape.SLOPE_2 || s == Shape.SLOPE_3 || s == Shape.GENTLE_SLOPE;
		}

		public static Family byId(String id) {
			for (Family f : values()) if (f.id.equals(id)) return f;
			return null;
		}
	}

	/** Every material's textures, found once each time the game loads its resources. */
	static final class Sprites {
		/** The table for the resources loading now (or loaded last). */
		static volatile Sprites current = new Sprites();

		private volatile boolean loaded;
		final TextureAtlasSprite[] slots = new TextureAtlasSprite[4];
		private final Map<Material, TextureAtlasSprite[]> materials = new HashMap<>();
		private final Map<String, TextureAtlasSprite> byName = new HashMap<>();

		/** Finds every texture, the first time a model is baked. */
		Sprites load(ModelBaker baker) {
			if (loaded) return this;
			synchronized (this) {
				if (loaded) return this;
				ModelDebugName named = () -> "cubework materials";
				for (int i = 0; i < 4; i++) slots[i] = Models.sprite(baker, SLOT_NAMES[i], named);
				for (Material m : Material.ALL) {
					if (!m.exists()) continue;
					Material.Textures t = m.textures();
					String[] names = {t.top(), t.side(), t.bottom(), t.overlay()};
					TextureAtlasSprite[] s = new TextureAtlasSprite[4];
					for (int i = 0; i < 4; i++) {
						if (names[i] == null) continue;
						String name = names[i];
						s[i] = byName.computeIfAbsent(name, n -> Models.sprite(baker, n, named));
					}
					materials.put(m, s);
				}
				loaded = true;
			}
			return this;
		}

		/** A material's top, side, bottom and overlay (null when it has none). */
		TextureAtlasSprite[] of(Material m) {
			return materials.get(m);
		}

		/** A texture by the name Material.Textures gives it (for the shapes drawn in code). */
		TextureAtlasSprite named(String name) {
			return byName.get(name);
		}

		/** Which stand-in a texture coordinate falls in, or -1. */
		int slotAt(float u, float v) {
			for (int i = 0; i < 4; i++) {
				TextureAtlasSprite s = slots[i];
				if (u >= s.getU0() && u <= s.getU1() && v >= s.getV0() && v <= s.getV1()) return i;
			}
			return -1;
		}

		int slotOf(TextureAtlasSprite sprite) {
			for (int i = 0; i < 4; i++) if (slots[i] == sprite) return i;
			return -1;
		}
	}

	/** Moves texture coordinates from one sprite onto another. */
	static float u(float u, TextureAtlasSprite from, TextureAtlasSprite to) {
		return to.getU((u - from.getU0()) / (from.getU1() - from.getU0()));
	}

	static float v(float v, TextureAtlasSprite from, TextureAtlasSprite to) {
		return to.getV((v - from.getV0()) / (from.getV1() - from.getV0()));
	}

	/** The baked quads of a look in a material (for items, falling blocks and anything else drawn without a world). */
	static List<BakedQuad> retexture(List<BakedQuad> quads, Sprites sprites, TextureAtlasSprite[] mat) {
		List<BakedQuad> out = new ArrayList<>(quads.size());
		for (BakedQuad q : quads) {
			int slot = sprites.slotOf(q.sprite());
			if (slot < 0) {
				out.add(q);
				continue;
			}
			TextureAtlasSprite from = q.sprite(), to = mat[slot];
			if (to == null) continue;
			long[] uv = new long[4];
			for (int i = 0; i < 4; i++) {
				long p = q.packedUV(i);
				uv[i] = UVPair.pack(u(UVPair.unpackU(p), from, to), v(UVPair.unpackV(p), from, to));
			}
			out.add(new BakedQuad(q.position0(), q.position1(), q.position2(), q.position3(), uv[0], uv[1], uv[2], uv[3], q.tintIndex(), q.direction(), to,
					q.shade(), q.lightEmission()));
		}
		return out;
	}

	/** A placed shape's material (its block's first when it has none, or when drawn without a world). */
	static Material material(BlockAndTintGetter world, BlockPos pos, BlockState state) {
		if (world != null && pos != null && world.getBlockEntityRenderData(pos) instanceof Material m) return m;
		return CwBlocks.defaultMaterial(state.getBlock());
	}

	// ---------- loading ----------

	public static void register() {
		PreparableModelLoadingPlugin.register(MaterialModels::load, (looks, ctx) -> {
			Sprites sprites = new Sprites();
			Sprites.current = sprites;
			for (Block block : CwBlocks.all()) {
				CwBlocks.Info info = CwBlocks.info(block);
				Shape shape = info.shape();
				if (shape.drawnInCode()) {
					DrawnRoot root = new DrawnRoot(block, info, sprites, new ConcurrentHashMap<>());
					ctx.registerBlockStateResolver(block, resolver -> {
						for (BlockState state : block.getStateDefinition().getPossibleStates()) resolver.setModel(state, root);
					});
					continue;
				}
				Map<Family, BlockModelDefinition> families = looks.get(shape.suffix);
				if (families == null || families.isEmpty()) {
					LOGGER.warn("Cubework: no looks for {}", shape.suffix);
					continue;
				}
				ctx.registerBlockStateResolver(block, resolver -> resolve(resolver, block, info, families, sprites));
			}
		});
	}

	/** Reads every look, in parallel: shape suffix, then family. */
	private static CompletableFuture<Map<String, Map<Family, BlockModelDefinition>>> load(PreparableReloadListener.SharedState shared, Executor executor) {
		ResourceManager resources = shared.resourceManager();
		return CompletableFuture.supplyAsync(() -> LISTER.listMatchingResources(resources), executor).thenCompose(files -> {
			List<CompletableFuture<Object[]>> reads = new ArrayList<>(files.size());
			for (Map.Entry<Identifier, Resource> file : files.entrySet()) {
				reads.add(CompletableFuture.supplyAsync(() -> {
					Identifier id = LISTER.fileToId(file.getKey());
					String path = id.getPath();
					int slash = path.lastIndexOf('/');
					Family family = slash < 0 ? null : Family.byId(path.substring(slash + 1));
					if (family == null) return null;
					try (Reader reader = file.getValue().openAsReader()) {
						JsonElement json = StrictJsonParser.parse(reader);
						return new Object[] {path.substring(0, slash), family, BlockModelDefinition.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(JsonParseException::new)};
					} catch (Exception e) {
						LOGGER.error("Cubework: couldn't read the look {}", id, e);
						return null;
					}
				}, executor));
			}
			return Util.sequence(reads).thenApply(list -> {
				Map<String, Map<Family, BlockModelDefinition>> out = new HashMap<>();
				for (Object[] e : list) {
					if (e == null) continue;
					out.computeIfAbsent((String) e[0], k -> new EnumMap<>(Family.class)).put((Family) e[1], (BlockModelDefinition) e[2]);
				}
				return out;
			});
		});
	}

	private static void resolve(BlockStateResolver.Context resolver, Block block, CwBlocks.Info info, Map<Family, BlockModelDefinition> families, Sprites sprites) {
		Identifier id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block);
		Map<Family, Map<BlockState, BlockStateModel.UnbakedRoot>> roots = new EnumMap<>(Family.class);
		for (Map.Entry<Family, BlockModelDefinition> e : families.entrySet()) {
			String name = id + " (" + e.getKey().id + ")";
			roots.put(e.getKey(), block instanceof Joining joining ? JoinedModels.instantiate(block, joining, e.getValue(), name)
					: e.getValue().instantiate(block.getStateDefinition(), () -> name));
		}
		Family fallback = families.containsKey(Family.PLAIN) ? Family.PLAIN : families.keySet().iterator().next();
		Map<BlockState, MaterialRoot> made = new HashMap<>();
		for (BlockState state : block.getStateDefinition().getPossibleStates()) {
			BlockState look = Models.dry(state);
			MaterialRoot root = made.computeIfAbsent(look, s -> {
				BlockStateModel.UnbakedRoot[] byFamily = new BlockStateModel.UnbakedRoot[Family.values().length];
				for (Map.Entry<Family, Map<BlockState, BlockStateModel.UnbakedRoot>> e : roots.entrySet()) byFamily[e.getKey().ordinal()] = e.getValue().get(s);
				return new MaterialRoot(block, info, s, byFamily, fallback, sprites);
			});
			if (root.looks()[fallback.ordinal()] == null) {
				LOGGER.warn("Cubework: {} has no look for {}", id, state);
				continue;
			}
			resolver.setModel(state, root);
		}
	}

	// ---------- shapes with looks ----------

	/** One state's looks, one for each family (missing ones fall back to plain). */
	private static final class MaterialRoot implements BlockStateModel.UnbakedRoot {
		private final Block block;
		private final CwBlocks.Info info;
		private final BlockState look;
		private final BlockStateModel.UnbakedRoot[] looks;
		private final Family fallback;
		private final Sprites sprites;
		private volatile BlockStateModel baked;

		MaterialRoot(Block block, CwBlocks.Info info, BlockState look, BlockStateModel.UnbakedRoot[] looks, Family fallback, Sprites sprites) {
			this.block = block;
			this.info = info;
			this.look = look;
			this.looks = looks;
			this.fallback = fallback;
			this.sprites = sprites;
		}

		BlockStateModel.UnbakedRoot[] looks() {
			return looks;
		}

		@Override
		public void resolveDependencies(ResolvableModel.Resolver resolver) {
			for (BlockStateModel.UnbakedRoot r : looks) if (r != null) r.resolveDependencies(resolver);
		}

		@Override
		public Object visualEqualityGroup(BlockState state) {
			return this;
		}

		@Override
		public BlockStateModel bake(BlockState state, ModelBaker baker) {
			BlockStateModel b = baked;
			if (b != null) return b;
			synchronized (this) {
				if (baked != null) return baked;
				sprites.load(baker);
				BlockStateModel[] out = new BlockStateModel[looks.length];
				for (int i = 0; i < looks.length; i++) if (looks[i] != null) out[i] = looks[i].bake(look, baker);
				for (int i = 0; i < out.length; i++) if (out[i] == null) out[i] = out[fallback.ordinal()];
				baked = new MaterialModel(block, info, out, sprites);
				return baked;
			}
		}
	}

	/** A shape's model: as it's drawn, the material's family's look in the material's textures. */
	private static final class MaterialModel implements BlockStateModel {
		private final Block block;
		private final CwBlocks.Info info;
		private final BlockStateModel[] looks;
		private final Sprites sprites;

		MaterialModel(Block block, CwBlocks.Info info, BlockStateModel[] looks, Sprites sprites) {
			this.block = block;
			this.info = info;
			this.looks = looks;
			this.sprites = sprites;
		}

		private BlockStateModel look(Material m) {
			return m == null ? looks[0] : looks[Family.of(m, info.shape()).ordinal()];
		}

		@Override
		public void emitQuads(QuadEmitter emitter, BlockAndTintGetter world, BlockPos pos, BlockState state, RandomSource random, Predicate<Direction> cullTest) {
			Material m = material(world, pos, state);
			TextureAtlasSprite[] mat = m == null ? null : sprites.of(m);
			if (mat == null) {
				look(m).emitQuads(emitter, world, pos, state, random, cullTest);
				return;
			}
			int hidden = info.behaviour().glass() ? hiddenFaces(world, pos, state, m) : 0;
			emitter.pushTransform(q -> retexture(q, mat, hidden));
			look(m).emitQuads(emitter, world, pos, state, random, cullTest);
			emitter.popTransform();
		}

		/** One quad onto the material's textures; false drops it (a glass face against the same glass). */
		private boolean retexture(MutableQuadView q, TextureAtlasSprite[] mat, int hidden) {
			Direction cull = q.cullFace();
			if (cull != null && (hidden & 1 << cull.ordinal()) != 0) return false;
			int slot = sprites.slotAt((q.u(0) + q.u(1) + q.u(2) + q.u(3)) / 4, (q.v(0) + q.v(1) + q.v(2) + q.v(3)) / 4);
			if (slot < 0) return true;
			TextureAtlasSprite from = sprites.slots[slot], to = mat[slot];
			if (to == null) return false;
			for (int i = 0; i < 4; i++) q.uv(i, u(q.u(i), from, to), v(q.v(i), from, to));
			// Grass's overlay has see-through pixels: drawn cut out, while the rest stays solid.
			if (slot == OVERLAY) q.renderLayer(ChunkSectionLayer.CUTOUT);
			return true;
		}

		@Override
		public void collectParts(RandomSource random, List<BlockModelPart> parts) {
			Material m = CwBlocks.defaultMaterial(block);
			List<BlockModelPart> inner = new ArrayList<>();
			look(m).collectParts(random, inner);
			TextureAtlasSprite[] mat = m == null ? null : sprites.of(m);
			if (mat == null) {
				parts.addAll(inner);
				return;
			}
			for (BlockModelPart p : inner) parts.add(new Retextured(p, sprites, mat));
		}

		@Override
		public Object createGeometryKey(BlockAndTintGetter world, BlockPos pos, BlockState state, RandomSource random) {
			Material m = material(world, pos, state);
			Object inner = look(m).createGeometryKey(world, pos, state, random);
			return inner == null ? null : new Key(m, inner);
		}

		@Override
		public TextureAtlasSprite particleIcon() {
			Material m = CwBlocks.defaultMaterial(block);
			TextureAtlasSprite[] mat = m == null ? null : sprites.of(m);
			return mat != null ? mat[SIDE] : looks[0].particleIcon();
		}

		@Override
		public TextureAtlasSprite particleSprite(BlockAndTintGetter world, BlockPos pos, BlockState state) {
			Material m = material(world, pos, state);
			TextureAtlasSprite[] mat = m == null ? null : sprites.of(m);
			return mat != null ? mat[SIDE] : particleIcon();
		}
	}

	private record Key(Material material, Object look) {}

	/** Which faces of a glass shape hide against a neighbour of the same glass that covers them (a bit per direction). */
	static int hiddenFaces(BlockAndTintGetter world, BlockPos pos, BlockState state, Material m) {
		if (world == null || pos == null) return 0;
		int mask = 0;
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
		for (Direction d : Direction.values()) {
			p.setWithOffset(pos, d);
			BlockState other = world.getBlockState(p);
			Material theirs;
			if (CwBlocks.info(other.getBlock()) != null) theirs = material(world, p, other);
			else theirs = CwBlocks.materialOf(other.getBlock());
			if (theirs != m) continue;
			if (Shared.covers(state, other, d)) mask |= 1 << d.ordinal();
		}
		return mask;
	}

	/** A part of a look in a material's textures (drawn without a world). */
	private record Retextured(BlockModelPart part, Sprites sprites, TextureAtlasSprite[] mat) implements BlockModelPart {
		@Override
		public List<BakedQuad> getQuads(Direction side) {
			return retexture(part.getQuads(side), sprites, mat);
		}

		@Override
		public boolean useAmbientOcclusion() {
			return part.useAmbientOcclusion();
		}

		@Override
		public TextureAtlasSprite particleIcon() {
			return mat[SIDE];
		}
	}

	// ---------- shapes drawn in code ----------

	/** Every state of a shape drawn in code: its quads for each material (and way of joining) are made when first drawn. */
	private record DrawnRoot(Block block, CwBlocks.Info info, Sprites sprites, Map<BlockState, BlockStateModel> baked) implements BlockStateModel.UnbakedRoot {
		@Override
		public void resolveDependencies(ResolvableModel.Resolver resolver) {
		}

		@Override
		public Object visualEqualityGroup(BlockState state) {
			return Models.dry(state);
		}

		@Override
		public BlockStateModel bake(BlockState state, ModelBaker baker) {
			sprites.load(baker);
			return baked.computeIfAbsent(Models.dry(state), look -> new DrawnModel(block, info, look, sprites, new ConcurrentHashMap<>()));
		}
	}

	/** Made for each material and way of joining when first drawn. */
	private record Drawn(Material material, int joins) {}

	private record DrawnModel(Block block, CwBlocks.Info info, BlockState look, Sprites sprites, Map<Drawn, Models.Built> made) implements BlockStateModel {
		private Models.Built built(Material m, int joins) {
			return made.computeIfAbsent(new Drawn(m, joins), k -> make(k.material(), k.joins()));
		}

		private Models.Built make(Material m, int joins) {
			java.util.function.Function<String, TextureAtlasSprite> named = sprites::named;
			return switch (info.shape()) {
				case STAIR_CURVE -> StairCurveModel.build(m, look, named);
				case ROUND_WINDOW_4, ROUND_WINDOW_8, ROUND_WINDOW_12, ROUND_WINDOW_16 -> RoundWindowModel.build(m, look, joins, named);
				case SLOPE, SLOPE_2, SLOPE_3, GENTLE_SLOPE -> SlopeModel.build(m, look, joins, named);
				case ARCH, ARCH_CORNER -> ArchModel.build(m, look, named);
				case ROUND_PILLAR_2, ROUND_PILLAR_3, ROUND_PILLAR_4 -> RoundPillarModel.build(m, look, named);
				default -> RoundModel.build(m, look, named);
			};
		}

		private int joins(BlockAndTintGetter world, BlockPos pos, BlockState state) {
			if (!(block instanceof Joining j)) return 0;
			return world == null || pos == null ? j.lone(state) : j.joins(world, pos, state) & 15;
		}

		@Override
		public void emitQuads(QuadEmitter emitter, BlockAndTintGetter world, BlockPos pos, BlockState state, RandomSource random, Predicate<Direction> cullTest) {
			Material m = material(world, pos, state);
			if (m == null) return;
			Models.Built b = built(m, joins(world, pos, state));
			TextureAtlasSprite[] mat = sprites.of(m);
			TextureAtlasSprite overlay = mat == null ? null : mat[OVERLAY];
			int hidden = info.behaviour().glass() ? hiddenFaces(world, pos, state, m) : 0;
			if (overlay != null || hidden != 0) {
				emitter.pushTransform(q -> {
					Direction cull = q.cullFace();
					if (cull != null && (hidden & 1 << cull.ordinal()) != 0) return false;
					if (overlay != null) {
						float u = (q.u(0) + q.u(1) + q.u(2) + q.u(3)) / 4, v = (q.v(0) + q.v(1) + q.v(2) + q.v(3)) / 4;
						if (u >= overlay.getU0() && u <= overlay.getU1() && v >= overlay.getV0() && v <= overlay.getV1()) q.renderLayer(ChunkSectionLayer.CUTOUT);
					}
					return true;
				});
				emit(emitter, b, cullTest);
				emitter.popTransform();
			} else {
				emit(emitter, b, cullTest);
			}
		}

		private static void emit(QuadEmitter emitter, Models.Built b, Predicate<Direction> cullTest) {
			for (Map.Entry<Direction, List<BakedQuad>> e : b.quads().sided().entrySet()) {
				if (cullTest.test(e.getKey())) continue;
				for (BakedQuad q : e.getValue()) {
					emitter.fromBakedQuad(q);
					emitter.cullFace(e.getKey());
					emitter.emit();
				}
			}
			for (BakedQuad q : b.quads().free()) {
				emitter.fromBakedQuad(q);
				emitter.emit();
			}
		}

		@Override
		public void collectParts(RandomSource random, List<BlockModelPart> parts) {
			Material m = CwBlocks.defaultMaterial(block);
			if (m == null) return;
			Models.Built b = built(m, joins(null, null, look));
			parts.add(new DrawnPart(b));
		}

		@Override
		public Object createGeometryKey(BlockAndTintGetter world, BlockPos pos, BlockState state, RandomSource random) {
			return new Key(material(world, pos, state), new Drawn(null, joins(world, pos, state)));
		}

		@Override
		public TextureAtlasSprite particleIcon() {
			Material m = CwBlocks.defaultMaterial(block);
			TextureAtlasSprite[] mat = m == null ? null : sprites.of(m);
			return mat != null ? mat[SIDE] : sprites.slots[SIDE];
		}

		@Override
		public TextureAtlasSprite particleSprite(BlockAndTintGetter world, BlockPos pos, BlockState state) {
			Material m = material(world, pos, state);
			TextureAtlasSprite[] mat = m == null ? null : sprites.of(m);
			return mat != null ? mat[SIDE] : particleIcon();
		}
	}

	private record DrawnPart(Models.Built built) implements BlockModelPart {
		@Override
		public List<BakedQuad> getQuads(Direction side) {
			if (side == null) return built.quads().free();
			List<BakedQuad> list = built.quads().sided().get(side);
			return list == null ? List.of() : list;
		}

		@Override
		public boolean useAmbientOcclusion() {
			return true;
		}

		@Override
		public TextureAtlasSprite particleIcon() {
			return built.particle();
		}
	}

	/** The shapes drawn see-through. */
	static boolean translucent(Behaviour b) {
		return b.seeThrough();
	}
}
