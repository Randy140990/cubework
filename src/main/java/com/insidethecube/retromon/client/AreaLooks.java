package com.insidethecube.retromon.client;

import com.insidethecube.retromon.Retromon;
import com.insidethecube.retromon.engine.Species;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.fabricmc.fabric.api.client.model.loading.v1.FabricBakedModelManager;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.fabricmc.fabric.api.client.model.loading.v1.SimpleUnbakedExtraModel;
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.SpriteFinder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Looks that only some places have: a block drawn differently where it is, without the map being changed (Pallet
 * Town's white fences, the HeartGold red tiles on your house's and Blue's roofs, the shelf signs as HeartGold's
 * signs, the grass's tufts onto the dripstone round the water). Which look, on which block, where: the games' data
 * (area_looks.tsv). Only in a Retromon save; anywhere else the blocks look as they always do.
 *
 * Each block with a look keeps its own model, wrapped: outside its areas the wrapper draws the block as it was.
 */
public final class AreaLooks {
	private AreaLooks() {}

	/** Part of the map (x and z, every height). */
	private record Box(int x0, int z0, int x1, int z1) {
		boolean has(BlockPos p) {
			return p.getX() >= x0 && p.getX() <= x1 && p.getZ() >= z0 && p.getZ() <= z1;
		}
	}

	/** For each block with a look somewhere: each look's areas. */
	private static final Map<Block, Map<String, List<Box>>> RULES = new HashMap<>();

	/** The signs' parts, by name ("board_l_north"): the models the mod puts where the shelves are. */
	private static final Map<String, ExtraModelKey<BlockStateModel>> SIGN_PARTS = new HashMap<>();

	static {
		for (String[] r : Species.table("area_looks.tsv")) {
			if (r.length < 6) continue;
			Block b = BuiltInRegistries.BLOCK.getOptional(Identifier.withDefaultNamespace(r[1].trim())).orElse(null);
			if (b == null || b == Blocks.AIR) continue;
			int x0 = Integer.parseInt(r[2].trim()), z0 = Integer.parseInt(r[3].trim()), x1 = Integer.parseInt(r[4].trim()), z1 = Integer.parseInt(r[5].trim());
			RULES.computeIfAbsent(b, k -> new HashMap<>()).computeIfAbsent(r[0].trim(), k -> new ArrayList<>())
					.add(new Box(Math.min(x0, x1), Math.min(z0, z1), Math.max(x0, x1), Math.max(z0, z1)));
		}
	}

	public static void register() {
		ModelLoadingPlugin.register(ctx -> {
			for (String row : new String[] {"board", "stand"}) {
				for (String part : new String[] {"l", "r", "m", "s"}) {
					for (Direction d : Direction.Plane.HORIZONTAL) {
						String name = row + "_" + part + "_" + d.getSerializedName();
						ExtraModelKey<BlockStateModel> key = ExtraModelKey.create(() -> "retromon sign " + name);
						ctx.addModel(key, SimpleUnbakedExtraModel.blockStateModel(Retromon.id("block/area/sign_" + name)));
						SIGN_PARTS.put(name, key);
					}
				}
			}
			ctx.modifyBlockModelAfterBake().register(ModelModifier.WRAP_PHASE, (model, c) -> {
				Map<String, List<Box>> looks = RULES.get(c.state().getBlock());
				if (looks == null) return model;
				BlockStateModel out = model;
				for (Map.Entry<String, List<Box>> e : looks.entrySet()) {
					Look look = make(e.getKey(), c.baker());
					if (look != null) out = new Wrapped(out, look, List.copyOf(e.getValue()));
				}
				return out;
			});
		});
	}

	/** How a look draws a block that's in one of its areas (`base` is how the block's drawn anywhere else). */
	private interface Look {
		void emit(BlockStateModel base, QuadEmitter e, BlockAndTintGetter view, BlockPos pos, BlockState state, RandomSource random, Predicate<Direction> cull);
	}

	private static final class Wrapped extends WrapperBlockStateModel {
		private final Look look;
		private final List<Box> boxes;

		Wrapped(BlockStateModel wrapped, Look look, List<Box> boxes) {
			super(wrapped);
			this.look = look;
			this.boxes = boxes;
		}

		private boolean here(BlockPos pos) {
			if (!Adventure.inSave || pos == null) return false;
			for (Box b : boxes) if (b.has(pos)) return true;
			return false;
		}

		@Override
		public void emitQuads(QuadEmitter emitter, BlockAndTintGetter view, BlockPos pos, BlockState state, RandomSource random, Predicate<Direction> cull) {
			if (here(pos)) look.emit(wrapped, emitter, view, pos, state, random, cull);
			else wrapped.emitQuads(emitter, view, pos, state, random, cull);
		}

		@Override
		public Object createGeometryKey(BlockAndTintGetter view, BlockPos pos, BlockState state, RandomSource random) {
			return here(pos) ? null : wrapped.createGeometryKey(view, pos, state, random);
		}
	}

	private static TextureAtlasSprite sprite(ModelBaker baker, String path) {
		return baker.sprites().get(new Material(TextureAtlas.LOCATION_BLOCKS, Retromon.id("block/area/" + path)), () -> "retromon area looks");
	}

	private static Look make(String name, ModelBaker baker) {
		return switch (name) {
			case "white_fence" -> retexture(baker.sprites().spriteFinder(TextureAtlas.LOCATION_BLOCKS), sprite(baker, "white_fence"));
			case "roof" -> new Roof(sprite(baker, "roof_tiles"), sprite(baker, "roof_edge"), sprite(baker, "roof_under"));
			case "sign" -> AreaLooks::sign;
			case "grass_edge" -> {
				TextureAtlasSprite[] tufts = new TextureAtlasSprite[18], shade = new TextureAtlasSprite[18];
				for (int t = 1; t <= 17; t++) {
					tufts[t] = sprite(baker, "grass_on_drip_" + t);
					shade[t] = sprite(baker, "drip_shade_" + t);
				}
				yield new GrassEdge(tufts, shade);
			}
			default -> null;
		};
	}

	// ---------- one texture for another, same shape (the white fences) ----------
	private static Look retexture(SpriteFinder finder, TextureAtlasSprite to) {
		return (base, e, view, pos, state, random, cull) -> {
			e.pushTransform(q -> {
				TextureAtlasSprite from = finder.find(q);
				if (from == null) return true;
				float fu = from.getU1() - from.getU0(), fv = from.getV1() - from.getV0();
				for (int i = 0; i < 4; i++) {
					float nu = fu == 0 ? 0 : (q.u(i) - from.getU0()) / fu, nv = fv == 0 ? 0 : (q.v(i) - from.getV0()) / fv;
					q.uv(i, to.getU0() + clamp01(nu) * (to.getU1() - to.getU0()), to.getV0() + clamp01(nv) * (to.getV1() - to.getV0()));
				}
				return true;
			});
			base.emitQuads(e, view, pos, state, random, cull);
			e.popTransform();
		};
	}

	private static float clamp01(float v) {
		return v < 0 ? 0 : v > 1 ? 1 : v;
	}

	/** Sets a quad's corners to `sprite` at (s, t) in blocks (one block = the whole texture), from where they are. */
	private static void lay(MutableQuadView q, TextureAtlasSprite sprite, float[] s, float[] t) {
		float s0 = (float) Math.floor(Math.min(Math.min(s[0], s[1]), Math.min(s[2], s[3])) + 1e-3F);
		float t0 = (float) Math.floor(Math.min(Math.min(t[0], t[1]), Math.min(t[2], t[3])) + 1e-3F);
		for (int i = 0; i < 4; i++) {
			float u = clamp01(s[i] - s0), v = clamp01(t[i] - t0);
			q.uv(i, sprite.getU0() + u * (sprite.getU1() - sprite.getU0()), sprite.getV0() + v * (sprite.getV1() - sprite.getV0()));
		}
	}

	// ---------- the roofs: rows of tiles along the eaves, each row's top uphill ----------
	/**
	 * Each face of the roof's stairs gets the tiles laid from where it is, not from the planks' pattern: the rows
	 * run along the eaves (across the way the stairs face) and go down the roof over the steps and their risers in
	 * one run, so the tops and the fronts of the steps read as rows of tiles. The roof's ends (the faces across the
	 * rows) get its darker trim, the undersides its shadow.
	 */
	private record Roof(TextureAtlasSprite tiles, TextureAtlasSprite edge, TextureAtlasSprite under) implements Look {
		@Override
		public void emit(BlockStateModel base, QuadEmitter e, BlockAndTintGetter view, BlockPos pos, BlockState state, RandomSource random, Predicate<Direction> cull) {
			Direction facing = state.hasProperty(BlockStateProperties.HORIZONTAL_FACING) ? state.getValue(BlockStateProperties.HORIZONTAL_FACING) : Direction.NORTH;
			Direction down = facing.getOpposite();
			Direction.Axis along = facing.getAxis() == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
			e.pushTransform(q -> {
				float[] s = new float[4], t = new float[4];
				Direction f = q.lightFace();
				TextureAtlasSprite sp = tiles;
				for (int i = 0; i < 4; i++) {
					float wx = pos.getX() + q.x(i), wy = pos.getY() + q.y(i), wz = pos.getZ() + q.z(i);
					float a = along == Direction.Axis.X ? wx : wz, d = down.getStepX() * wx + down.getStepZ() * wz;
					if (f == Direction.DOWN) {
						s[i] = a;
						t[i] = d;
					} else if (f.getAxis() == along) {
						s[i] = d;
						t[i] = -wy;
					} else {
						s[i] = a;
						t[i] = d - wy;
					}
				}
				if (f == Direction.DOWN) sp = under;
				else if (f.getAxis() == along) sp = edge;
				lay(q, sp, s, t);
				return true;
			});
			base.emitQuads(e, view, pos, state, random, cull);
			e.popTransform();
		}
	}

	// ---------- the signs ----------
	/**
	 * The shelves stacked as a sign: the top row is HeartGold's white board, the rows under it its grey stand. Each
	 * shelf is the board's (or the stand's) left end, right end or a middle block as you look at its front, or the
	 * whole of it when the sign is one block wide.
	 */
	private static void sign(BlockStateModel base, QuadEmitter e, BlockAndTintGetter view, BlockPos pos, BlockState state, RandomSource random, Predicate<Direction> cull) {
		if (!state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
			base.emitQuads(e, view, pos, state, random, cull);
			return;
		}
		Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
		// You look at its front from in front of it: your left is its facing turned clockwise (north: east).
		Direction left = facing.getClockWise();
		boolean onLeft = sameSign(view, pos.relative(left), state), onRight = sameSign(view, pos.relative(left.getOpposite()), state);
		String part = onLeft && onRight ? "m" : onRight ? "l" : onLeft ? "r" : "s";
		String row = sameSign(view, pos.above(), state) ? "stand" : "board";
		ExtraModelKey<BlockStateModel> key = SIGN_PARTS.get(row + "_" + part + "_" + facing.getSerializedName());
		BlockStateModel m = null;
		if (key != null) {
			try {
				m = ((FabricBakedModelManager) Minecraft.getInstance().getModelManager()).getModel(key);
			} catch (RuntimeException ignored) {
				m = null;
			}
		}
		if (m == null) base.emitQuads(e, view, pos, state, random, cull);
		else m.emitQuads(e, view, pos, state, random, cull);
	}

	private static boolean sameSign(BlockAndTintGetter view, BlockPos p, BlockState as) {
		BlockState s = view.getBlockState(p);
		return s.is(as.getBlock()) && s.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
				&& s.getValue(BlockStateProperties.HORIZONTAL_FACING) == as.getValue(BlockStateProperties.HORIZONTAL_FACING);
	}

	// ---------- the grass's tufts onto the dripstone round the water ----------
	/**
	 * The dripstone that borders water (within two blocks of it) takes the grass's rounded, tufted edge where grass
	 * meets it, as the dirt paths do (the same 17 tiles: an edge, two, three or all four, or a corner), with their
	 * soft shadow on the dripstone. The tufts take the grass's green (Colours tints them).
	 */
	private record GrassEdge(TextureAtlasSprite[] tufts, TextureAtlasSprite[] shade) implements Look {
		@Override
		public void emit(BlockStateModel base, QuadEmitter e, BlockAndTintGetter view, BlockPos pos, BlockState state, RandomSource random, Predicate<Direction> cull) {
			base.emitQuads(e, view, pos, state, random, cull);
			if (cull.test(Direction.UP)) return;
			boolean n = grass(view, pos.north()), s = grass(view, pos.south()), w = grass(view, pos.west()), ea = grass(view, pos.east());
			boolean nw = grass(view, pos.north().west()), ne = grass(view, pos.north().east()), sw = grass(view, pos.south().west()), se = grass(view, pos.south().east());
			if (!(n || s || w || ea || nw || ne || sw || se) || !nearWater(view, pos)) return;
			List<Integer> tiles = new ArrayList<>(4);
			int edges = (n ? 1 : 0) | (ea ? 2 : 0) | (s ? 4 : 0) | (w ? 8 : 0);
			switch (edges) {
				case 1 -> tiles.add(16);
				case 2 -> tiles.add(8);
				case 4 -> tiles.add(2);
				case 8 -> tiles.add(10);
				case 3 -> tiles.add(11);
				case 6 -> tiles.add(4);
				case 12 -> tiles.add(5);
				case 9 -> tiles.add(12);
				case 5 -> {
					tiles.add(16);
					tiles.add(2);
				}
				case 10 -> {
					tiles.add(8);
					tiles.add(10);
				}
				case 14 -> tiles.add(6);
				case 13 -> tiles.add(7);
				case 7 -> tiles.add(13);
				case 11 -> tiles.add(14);
				case 15 -> tiles.add(9);
				default -> {
				}
			}
			// Corners: only where neither side next to them has an edge.
			if (nw && !n && !w) tiles.add(17);
			if (ne && !n && !ea) tiles.add(15);
			if (sw && !s && !w) tiles.add(3);
			if (se && !s && !ea) tiles.add(1);
			for (int t : tiles) top(e, shade[t], -1);
			for (int t : tiles) top(e, tufts[t], 0);
		}

		private static void top(QuadEmitter e, TextureAtlasSprite sprite, int tint) {
			e.square(Direction.UP, 0, 0, 1, 1, 0);
			e.spriteBake(sprite, MutableQuadView.BAKE_LOCK_UV);
			e.color(-1, -1, -1, -1);
			e.tintIndex(tint);
			e.renderLayer(ChunkSectionLayer.CUTOUT);
			e.emit();
		}

		private static boolean grass(BlockAndTintGetter view, BlockPos p) {
			BlockState s = view.getBlockState(p);
			return s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.MOSS_BLOCK) || s.is(Blocks.GREEN_CONCRETE_POWDER);
		}

		private static boolean nearWater(BlockAndTintGetter view, BlockPos pos) {
			BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
			for (int dy = -1; dy <= 1; dy++) {
				for (int dx = -2; dx <= 2; dx++) {
					for (int dz = -2; dz <= 2; dz++) {
						m.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
						if (view.getFluidState(m).is(FluidTags.WATER)) return true;
					}
				}
			}
			return false;
		}
	}
}
