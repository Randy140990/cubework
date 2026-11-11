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
import net.minecraft.client.resources.model.ModelDebugName;
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

	private static net.minecraft.client.resources.model.Material texture(String name) {
		return new net.minecraft.client.resources.model.Material(TextureAtlas.LOCATION_BLOCKS, Identifier.withDefaultNamespace("block/" + name));
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
			Built built = maker.make(state, name -> baker.sprites().get(texture(name), named));
			return new Baked(new Part(new Quads(built.quads().sided(), List.copyOf(built.quads().free())), built.particle()));
		}
	}

	private record Baked(Part part) implements BlockStateModel {
		@Override
		public void collectParts(RandomSource random, List<BlockModelPart> parts) {
			parts.add(part);
		}

		@Override
		public TextureAtlasSprite particleIcon() {
			return part.particle();
		}
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

		@Override
		public TextureAtlasSprite particleIcon() {
			return particle;
		}
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
			ModelDebugName named = () -> "cubework " + state;
			return new Old(maker.make(state, name -> baker.sprites().get(texture(name), named)));
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
