package com.insidethecube.cubework.client;

//#if MC >= 1.21.5
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Predicate;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.insidethecube.cubework.block.Joining;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.client.model.loading.v1.BlockStateResolver;
import net.fabricmc.fabric.api.client.model.loading.v1.PreparableModelLoadingPlugin;
import net.minecraft.util.Util;
import net.minecraft.client.renderer.block.model.BlockModelDefinition;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.slf4j.Logger;

/**
 * The models of window frames, tables and partitions (the blocks that join their neighbours without
 * keeping it in their block state, see Joining). Their looks are written like blockstate files, with the
 * joins as if they were properties, in assets/cubework/joined/. Here each is read against a stand-in set of
 * states that has those properties (one set for every block with the same properties, so few are made),
 * and each real state gets a JoinedModel holding its 16 joined looks.
 */
public final class JoinedModels {
	private JoinedModels() {}

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final FileToIdConverter LISTER = FileToIdConverter.json("joined");
	/** The stand-in states, by the block's own properties followed by its join properties. */
	private static final Map<List<Property<?>>, StateDefinition<Block, BlockState>> LOOK_STATES = new ConcurrentHashMap<>();

	/** Blocks drawn in code, which join through Models.registerJoinedLazy instead. */
	private static Predicate<Block> drawnInCode = b -> false;

	public static void register(Predicate<Block> inCode) {
		drawnInCode = inCode;
		PreparableModelLoadingPlugin.register(JoinedModels::load, (looks, ctx) -> {
			for (Block block : BuiltInRegistries.BLOCK) {
				if (!(block instanceof Joining joining) || drawnInCode.test(block)) continue;
				Identifier id = BuiltInRegistries.BLOCK.getKey(block);
				BlockModelDefinition look = looks.get(id);
				if (look == null) {
					LOGGER.warn("Cubework: no joined look for {}", id);
					continue;
				}
				ctx.registerBlockStateResolver(block, resolver -> resolve(resolver, block, joining, look, id));
			}
		});
	}

	/** Reads every joined look, in parallel. */
	private static CompletableFuture<Map<Identifier, BlockModelDefinition>> load(PreparableReloadListener.SharedState shared, Executor executor) {
		ResourceManager resources = shared.resourceManager();
		return CompletableFuture.supplyAsync(() -> LISTER.listMatchingResources(resources), executor).thenCompose(files -> {
			List<CompletableFuture<Map.Entry<Identifier, BlockModelDefinition>>> reads = new ArrayList<>(files.size());
			for (Map.Entry<Identifier, Resource> file : files.entrySet()) {
				reads.add(CompletableFuture.supplyAsync(() -> {
					Identifier id = LISTER.fileToId(file.getKey());
					try (Reader reader = file.getValue().openAsReader()) {
						JsonElement json = StrictJsonParser.parse(reader);
						return Map.entry(id, BlockModelDefinition.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(JsonParseException::new));
					} catch (Exception e) {
						LOGGER.error("Cubework: couldn't read the joined look of {}", id, e);
						return null;
					}
				}, executor));
			}
			return Util.sequence(reads).thenApply(list -> {
				Map<Identifier, BlockModelDefinition> out = new HashMap<>(list.size() * 2);
				for (Map.Entry<Identifier, BlockModelDefinition> e : list) if (e != null) out.put(e.getKey(), e.getValue());
				return out;
			});
		});
	}

	private static void resolve(BlockStateResolver.Context resolver, Block block, Joining joining, BlockModelDefinition look, Identifier id) {
		List<BooleanProperty> joins = joining.joinProperties();
		List<Property<?>> props = new ArrayList<>(block.getStateDefinition().getProperties());
		props.addAll(joins);
		StateDefinition<Block, BlockState> lookStates = LOOK_STATES.computeIfAbsent(props, p -> {
			StateDefinition.Builder<Block, BlockState> builder = new StateDefinition.Builder<>(block);
			builder.add(p.toArray(new Property<?>[0]));
			return builder.create(Block::defaultBlockState, BlockState::new);
		});
		Map<BlockState, BlockStateModel.UnbakedRoot> roots = look.instantiate(lookStates, () -> id + " (joined)");
		for (BlockState state : block.getStateDefinition().getPossibleStates()) {
			BlockState[] looks = new BlockState[16];
			BlockStateModel.UnbakedRoot[] lookRoots = new BlockStateModel.UnbakedRoot[16];
			BlockState base = lookStates.any();
			for (Property<?> p : state.getProperties()) base = copy(base, state, p);
			boolean whole = true;
			for (int mask = 0; mask < 16; mask++) {
				BlockState s = base;
				for (int i = 0; i < joins.size(); i++) s = s.setValue(joins.get(i), (mask >> i & 1) != 0);
				looks[mask] = s;
				lookRoots[mask] = roots.get(s);
				if (lookRoots[mask] == null) whole = false;
			}
			if (whole) resolver.setModel(state, new JoinedRoot(joining, looks, lookRoots));
		}
	}

	private static <T extends Comparable<T>> BlockState copy(BlockState to, BlockState from, Property<T> p) {
		return to.setValue(p, from.getValue(p));
	}

	/** A real state's model: its 16 joined looks, baked together. */
	private record JoinedRoot(Joining joining, BlockState[] looks, BlockStateModel.UnbakedRoot[] roots) implements BlockStateModel.UnbakedRoot {
		@Override
		public void resolveDependencies(ResolvableModel.Resolver resolver) {
			Set<BlockStateModel.UnbakedRoot> seen = Collections.newSetFromMap(new IdentityHashMap<>());
			for (BlockStateModel.UnbakedRoot root : roots) if (seen.add(root)) root.resolveDependencies(resolver);
		}

		@Override
		public Object visualEqualityGroup(BlockState state) {
			return state;
		}

		@Override
		public BlockStateModel bake(BlockState state, ModelBaker baker) {
			BlockStateModel[] baked = new BlockStateModel[16];
			for (int i = 0; i < 16; i++) baked[i] = roots[i].bake(looks[i], baker);
			return new JoinedModel(joining, baked, joining.lone(state));
		}
	}
}
//#else
/** Joined models need the block model system of 1.21.5 and later. */
public final class JoinedModels {
	private JoinedModels() {}

	public static void register(java.util.function.Predicate<net.minecraft.world.level.block.Block> inCode) {
	}
}
//#endif
