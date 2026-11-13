package com.insidethecube.cubework.client;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
//#if MC >= 1.21.5
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.util.RandomSource;
//#else
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.util.RandomSource;
//#endif
//#if MC >= 1.21.4 && MC < 1.21.5
import net.minecraft.client.renderer.block.model.UnbakedBlockStateModel;
import net.minecraft.client.resources.model.ResolvableModel;
//#endif
//#if MC < 1.21.4
import net.minecraft.client.resources.model.ModelState;
//#endif
//#if MC < 1.21.2
import java.util.Collection;
import net.minecraft.client.renderer.block.model.ItemOverrides;
//#endif

/**
 * Hands the slopes' and arches' own quads to the game's block models. The geometry (SlopeModel,
 * ArchModel) is the same in every version; how a model is registered, baked and asked for its quads
 * changed in 1.21.2, 1.21.4 and 1.21.5, and all of that is here.
 */
final class Models {
	private Models() {}

	/** Quads by the side they're hidden from, and the ones never hidden. */
	record Quads(Map<Direction, List<BakedQuad>> sided, List<BakedQuad> free) {}

	/** One block state's quads, and the texture its breaking particles use. */
	record Built(Quads quads, TextureAtlasSprite particle) {}

	/** Makes a block state's quads, given a way to find vanilla block textures by name. */
	interface Maker {
		Built make(BlockState state, Function<String, TextureAtlasSprite> sprites);
	}

	/** Every state of the block is drawn with the maker's quads. */
	static void register(ModelLoadingPlugin.Context ctx, Block block, Maker maker) {
		ctx.registerBlockStateResolver(block, resolver -> {
			//#if MC >= 1.21.5
			Root root = new Root(maker);
			for (BlockState state : block.getStateDefinition().getPossibleStates()) resolver.setModel(state, root);
			//#else
			for (BlockState state : block.getStateDefinition().getPossibleStates()) resolver.setModel(state, new PerState(state, maker));
			//#endif
		});
	}

	/**
	 * Like register, for blocks with very many states (round window frames: every way of joining, facing and
	 * depth): the textures are found when the game loads its models, but a state's quads are only made the
	 * first time it's drawn, so states nobody builds with take no memory. textures: every texture the maker
	 * uses; particle: the one breaking particles use.
	 */
	static void registerLazy(ModelLoadingPlugin.Context ctx, Block block, List<String> textures, String particle, Maker maker) {
		//#if MC >= 1.21.5
		LazyRoot root = new LazyRoot(maker, textures, particle);
		ctx.registerBlockStateResolver(block, resolver -> {
			for (BlockState state : block.getStateDefinition().getPossibleStates()) resolver.setModel(state, root);
		});
		//#else
		register(ctx, block, maker);
		//#endif
	}

	private static net.minecraft.client.resources.model.Material texture(String name) {
		// Full ids (patterns: cubework:block/pattern/tiled/stone) are used as they are.
		if (name.indexOf(':') >= 0) {
			//#if MC >= 26.1
			return new net.minecraft.client.resources.model.Material(Identifier.parse(name));
			//#else
			return new net.minecraft.client.resources.model.Material(TextureAtlas.LOCATION_BLOCKS, Identifier.parse(name));
			//#endif
		}
		//#if MC >= 26.2
		// 26.2 renamed the pillars' side textures.
		if (name.equals("quartz_pillar") || name.equals("purpur_pillar")) name += "_side";
		//#endif
		//#if MC >= 26.1
		return new net.minecraft.client.resources.model.Material(Identifier.withDefaultNamespace("block/" + name));
		//#else
		return new net.minecraft.client.resources.model.Material(TextureAtlas.LOCATION_BLOCKS, Identifier.withDefaultNamespace("block/" + name));
		//#endif
	}

	private static List<BakedQuad> quads(Quads q, Direction side) {
		if (side == null) return q.free();
		List<BakedQuad> list = q.sided().get(side);
		return list == null ? List.of() : list;
	}

	//#if MC >= 1.21.5
	private record Root(Maker maker) implements BlockStateModel.UnbakedRoot {
		@Override
		public void resolveDependencies(ResolvableModel.Resolver resolver) {
		}

		@Override
		public Object visualEqualityGroup(BlockState state) {
			return state;
		}

		@Override
		public BlockStateModel bake(BlockState state, ModelBaker baker) {
			ModelDebugName named = () -> "cubework " + state;
			//#if MC >= 26.1
			Built built = maker.make(state, name -> baker.materials().get(texture(name), named).sprite());
			//#else
			Built built = maker.make(state, name -> baker.sprites().get(texture(name), named));
			//#endif
			return new Baked(new Part(new Quads(built.quads().sided(), List.copyOf(built.quads().free())), built.particle()));
		}
	}

	private static TextureAtlasSprite sprite(ModelBaker baker, String name, ModelDebugName named) {
		//#if MC >= 26.1
		return baker.materials().get(texture(name), named).sprite();
		//#else
		return baker.sprites().get(texture(name), named);
		//#endif
	}

	private record LazyRoot(Maker maker, List<String> textures, String particle) implements BlockStateModel.UnbakedRoot {
		@Override
		public void resolveDependencies(ResolvableModel.Resolver resolver) {
		}

		@Override
		public Object visualEqualityGroup(BlockState state) {
			return state;
		}

		@Override
		public BlockStateModel bake(BlockState state, ModelBaker baker) {
			ModelDebugName named = () -> "cubework " + state;
			Map<String, TextureAtlasSprite> sprites = new java.util.HashMap<>();
			for (String name : textures) sprites.put(name, sprite(baker, name, named));
			return new LazyBaked(new LazyPart(state, maker, sprites, sprites.get(particle)));
		}
	}

	/** One state's quads, made the first time they're asked for (from any thread; making them twice is harmless). */
	private static final class LazyPart implements BlockModelPart {
		private final BlockState state;
		private final Maker maker;
		private final Map<String, TextureAtlasSprite> sprites;
		private final TextureAtlasSprite particle;
		private volatile Quads quads;

		LazyPart(BlockState state, Maker maker, Map<String, TextureAtlasSprite> sprites, TextureAtlasSprite particle) {
			this.state = state;
			this.maker = maker;
			this.sprites = sprites;
			this.particle = particle;
		}

		private Quads quads() {
			Quads q = quads;
			if (q == null) {
				Built built = maker.make(state, sprites::get);
				quads = q = new Quads(built.quads().sided(), List.copyOf(built.quads().free()));
			}
			return q;
		}

		@Override
		public List<BakedQuad> getQuads(Direction side) {
			return Models.quads(quads(), side);
		}

		@Override
		public boolean useAmbientOcclusion() {
			return true;
		}

		//#if MC >= 26.1
		@Override
		public net.minecraft.client.resources.model.Material.Baked particleMaterial() {
			return new net.minecraft.client.resources.model.Material.Baked(particle, false);
		}

		/** From the textures alone, so asking doesn't make the quads. */
		@Override
		public int materialFlags() {
			int flags = 0;
			for (TextureAtlasSprite s : sprites.values()) flags |= QuadMaker.flags(s);
			return flags;
		}
		//#else
		@Override
		public TextureAtlasSprite particleIcon() {
			return particle;
		}
		//#endif
	}

	private record LazyBaked(LazyPart part) implements BlockStateModel {
		@Override
		public void collectParts(RandomSource random, List<BlockModelPart> parts) {
			parts.add(part);
		}

		//#if MC >= 26.1
		@Override
		public net.minecraft.client.resources.model.Material.Baked particleMaterial() {
			return part.particleMaterial();
		}

		@Override
		public int materialFlags() {
			return part.materialFlags();
		}
		//#else
		@Override
		public TextureAtlasSprite particleIcon() {
			return part.particleIcon();
		}
		//#endif
	}

	private record Baked(Part part) implements BlockStateModel {
		@Override
		public void collectParts(RandomSource random, List<BlockModelPart> parts) {
			parts.add(part);
		}

		//#if MC >= 26.1
		@Override
		public net.minecraft.client.resources.model.Material.Baked particleMaterial() {
			return part.particleMaterial();
		}

		@Override
		public int materialFlags() {
			return part.materialFlags();
		}
		//#else
		@Override
		public TextureAtlasSprite particleIcon() {
			return part.particle();
		}
		//#endif
	}

	private record Part(Quads quads, TextureAtlasSprite particle) implements BlockModelPart {
		@Override
		public List<BakedQuad> getQuads(Direction side) {
			return Models.quads(quads, side);
		}

		@Override
		public boolean useAmbientOcclusion() {
			return true;
		}

		//#if MC >= 26.1
		@Override
		public net.minecraft.client.resources.model.Material.Baked particleMaterial() {
			return new net.minecraft.client.resources.model.Material.Baked(particle, false);
		}

		/** Whether any quad is see-through or animated (the game asks, to sort and redraw). */
		@Override
		public int materialFlags() {
			int flags = 0;
			for (BakedQuad q : quads.free()) flags |= q.materialInfo().flags();
			for (List<BakedQuad> side : quads.sided().values()) for (BakedQuad q : side) flags |= q.materialInfo().flags();
			return flags;
		}
		//#else
		@Override
		public TextureAtlasSprite particleIcon() {
			return particle;
		}
		//#endif
	}
	//#else
	/** Before 1.21.5 a model is baked without being told its block state, so each state gets its own. */
	private record PerState(BlockState state, Maker maker) implements
			//#if MC >= 1.21.4
			UnbakedBlockStateModel
			//#else
			UnbakedModel
			//#endif
	{
		//#if MC >= 1.21.4
		@Override
		public void resolveDependencies(ResolvableModel.Resolver resolver) {
		}

		@Override
		public Object visualEqualityGroup(BlockState other) {
			return state;
		}

		@Override
		public BakedModel bake(ModelBaker baker) {
			return new Old(maker.make(state, name -> baker.sprites().get(texture(name))));
		}
		//#elif MC >= 1.21.2
		@Override
		public void resolveDependencies(UnbakedModel.Resolver resolver) {
		}

		@Override
		public BakedModel bake(ModelBaker baker, Function<net.minecraft.client.resources.model.Material, TextureAtlasSprite> sprites, ModelState settings) {
			return new Old(maker.make(state, name -> sprites.apply(texture(name))));
		}
		//#else
		@Override
		public Collection<Identifier> getDependencies() {
			return List.of();
		}

		@Override
		public void resolveParents(Function<Identifier, UnbakedModel> models) {
		}

		@Override
		public BakedModel bake(ModelBaker baker, Function<net.minecraft.client.resources.model.Material, TextureAtlasSprite> sprites, ModelState settings) {
			return new Old(maker.make(state, name -> sprites.apply(texture(name))));
		}
		//#endif
	}

	private record Old(Built built) implements BakedModel {
		@Override
		public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random) {
			return Models.quads(built.quads(), side);
		}

		@Override
		public boolean useAmbientOcclusion() {
			return true;
		}

		@Override
		public boolean isGui3d() {
			return true;
		}

		@Override
		public boolean usesBlockLight() {
			return true;
		}

		@Override
		public TextureAtlasSprite getParticleIcon() {
			return built.particle();
		}

		@Override
		public ItemTransforms getTransforms() {
			return ItemTransforms.NO_TRANSFORMS;
		}

		//#if MC < 1.21.4
		@Override
		public boolean isCustomRenderer() {
			return false;
		}
		//#endif

		//#if MC < 1.21.2
		@Override
		public ItemOverrides getOverrides() {
			return ItemOverrides.EMPTY;
		}
		//#endif
	}
	//#endif
}
