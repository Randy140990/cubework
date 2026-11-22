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
 * Looks that only some places have: a block drawn differently where it is, without the map being changed (the white
 * fences of Pallet Town, Route 1, Viridian City and Route 22; the HeartGold red tiles on your house's and Blue's
 * roofs; the signs as HeartGold's; the grass's tufts onto Pallet's dripstone round the water; Viridian's path, its
 * kerbs and its black lampposts; Route 22's floor as a path; every Pokémon Center's reds and every Mart's blues as
 * Let's Go's). Which look, on which blocks, where: the games' data (area_looks.tsv, from tools/arealooks.py). Only in
 * a Retromon save; anywhere else the blocks look as they always do.
 *
 * Each block with a look keeps its own model, wrapped: outside its areas the wrapper draws the block as it was.
 */
public final class AreaLooks {
	private AreaLooks() {}

	/**
	 * Part of the map: x and z, and the heights it covers (every height when y0 > y1); rooms: in the interiors (the
	 * forest, built there) rather than John's world.
	 */
	private record Box(int x0, int z0, int x1, int z1, int y0, int y1, boolean rooms) {
		boolean has(BlockPos p) {
			return p.getX() >= x0 && p.getX() <= x1 && p.getZ() >= z0 && p.getZ() <= z1 && (y0 > y1 || p.getY() >= y0 && p.getY() <= y1)
					&& rooms == inRooms();
		}
	}

	/** Whether the world you're in now is the interiors (the buildings' insides and the forest). */
	private static boolean inRooms() {
		var level = net.minecraft.client.Minecraft.getInstance().level;
		return level != null && level.dimension() == com.insidethecube.retromon.game.Interiors.LEVEL;
	}

	/** One look somewhere: its name, its texture swaps ("block/tuff=lamp_tuff", "@4" for laid over 4 blocks), its areas. */
	private record Rule(String look, String swaps, List<Box> boxes) {}

	/** For each block with a look somewhere: its looks (by look and swaps). */
	private static final Map<Block, Map<String, Rule>> RULES = new HashMap<>();

	/** The signs' parts, by name ("board_l_north", "short_l_south", "wood_east"): the models put where the signs are. */
	private static final Map<String, ExtraModelKey<BlockStateModel>> SIGN_PARTS = new HashMap<>();

	static {
		for (String[] r : Species.table("area_looks.tsv")) {
			if (r.length < 9) continue;
			int x0 = Integer.parseInt(r[2].trim()), z0 = Integer.parseInt(r[3].trim()), x1 = Integer.parseInt(r[4].trim()), z1 = Integer.parseInt(r[5].trim());
			boolean every = r[6].trim().equals("-") || r[7].trim().equals("-");
			int ya = every ? 1 : Integer.parseInt(r[6].trim()), yb = every ? 0 : Integer.parseInt(r[7].trim());
			Box box = new Box(Math.min(x0, x1), Math.min(z0, z1), Math.max(x0, x1), Math.max(z0, z1), every ? 1 : Math.min(ya, yb), every ? 0 : Math.max(ya, yb),
					r.length > 10 && r[10].trim().equals("interiors"));
			String look = r[0].trim(), swaps = r[8].trim();
			for (String name : r[1].split(",")) {
				Block b = BuiltInRegistries.BLOCK.getOptional(Identifier.withDefaultNamespace(name.trim())).orElse(null);
				if (b == null || b == Blocks.AIR) continue;
				RULES.computeIfAbsent(b, k -> new HashMap<>()).computeIfAbsent(look + "|" + swaps, k -> new Rule(look, swaps, new ArrayList<>())).boxes().add(box);
			}
		}
	}

	public static void register() {
		ModelLoadingPlugin.register(ctx -> {
			for (Direction d : Direction.Plane.HORIZONTAL) {
				for (String part : new String[] {"l", "r", "m", "s"}) {
					for (String row : new String[] {"board", "stand", "short"}) part(ctx, row + "_" + part + "_" + d.getSerializedName(),
							row.equals("short") ? "block/area/sign_short_" + part + "_" + d.getSerializedName() : "block/area/sign_" + row + "_" + part + "_" + d.getSerializedName());
				}
				part(ctx, "wood_" + d.getSerializedName(), "block/area/wood_sign_" + d.getSerializedName());
			}
			for (String b : new String[] {"boulder_a", "boulder_b", "boulder_small"}) part(ctx, b, "block/area/" + b);
			ctx.modifyBlockModelAfterBake().register(ModelModifier.WRAP_PHASE, (model, c) -> {
				Map<String, Rule> looks = RULES.get(c.state().getBlock());
				if (looks == null) return model;
				BlockStateModel out = model;
				for (Rule rule : looks.values()) {
					Look look = make(rule, c.baker());
					if (look != null) out = new Wrapped(out, look, List.copyOf(rule.boxes()));
				}
				return out;
			});
		});
	}

	private static void part(ModelLoadingPlugin.Context ctx, String name, String model) {
		ExtraModelKey<BlockStateModel> key = ExtraModelKey.create(() -> "retromon sign " + name);
		ctx.addModel(key, SimpleUnbakedExtraModel.blockStateModel(Retromon.id(model)));
		SIGN_PARTS.put(name, key);
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

	private static Look make(Rule rule, ModelBaker baker) {
		return switch (rule.look()) {
			case "swap" -> swap(baker, rule.swaps());
			case "roof", "roof_green", "roof_gym" -> new Roof(sprite(baker, rule.look() + "_tiles"), sprite(baker, rule.look() + "_edge"), sprite(baker, rule.look() + "_under"));
			case "sign" -> AreaLooks::sign;
			case "wood_sign" -> AreaLooks::woodSign;
			case "boulder" -> AreaLooks::boulder;
			case "grass_edge", "kerb" -> {
				boolean kerb = rule.look().equals("kerb");
				TextureAtlasSprite[] over = new TextureAtlasSprite[18], under = new TextureAtlasSprite[18];
				for (int t = 1; t <= 17; t++) {
					over[t] = sprite(baker, (kerb ? "kerb_grass_" : "grass_on_drip_") + t);
					under[t] = sprite(baker, (kerb ? "kerb_" : "drip_shade_") + t);
				}
				yield new GrassEdge(over, under, !kerb);
			}
			default -> null;
		};
	}

	// ---------- textures swapped for others, same shapes (the white fences, the lamps, the roofs' colours, paths) ----------
	/** A sprite to put in place of another, laid as the old one was (period 0) or from where it is, over `period` blocks. */
	/** A texture swapped in: laid every period blocks (0: as the old one was), and drawn without the biome's tint (noTint). */
	private record Swap(TextureAtlasSprite to, int period, boolean noTint, TextureAtlasSprite side) {}

	/**
	 * Each face with one of the swapped textures gets its new one: in the old one's place on it (so a fence's posts and
	 * rails keep their parts of the picture), or laid from where the face is ("@4": a picture four blocks across, as the
	 * dirt paths' is), so neighbours run on.
	 */
	private static Look swap(ModelBaker baker, String spec) {
		SpriteFinder finder = baker.sprites().spriteFinder(TextureAtlas.LOCATION_BLOCKS);
		Map<Identifier, Swap> swaps = new HashMap<>();
		for (String one : spec.split(",")) {
			int eq = one.indexOf('=');
			if (eq < 0) continue;
			String from = one.substring(0, eq).trim(), to = one.substring(eq + 1).trim();
			// ("name!": drawn without the biome's tint, the texture's own colours: the League gate's bushes.)
			boolean noTint = to.endsWith("!");
			if (noTint) to = to.substring(0, to.length() - 1);
			int period = 0, at = to.indexOf('@');
			if (at >= 0) {
				period = Integer.parseInt(to.substring(at + 1).trim());
				to = to.substring(0, at);
			}
			Identifier fromId = from.contains(":") ? Identifier.parse(from) : Identifier.withDefaultNamespace(from);
			// ("top|side": one texture for the tops and bottoms, another for the sides: Diglett's Cave's rock.)
			int bar = to.indexOf('|');
			TextureAtlasSprite side = bar < 0 ? null : sprite(baker, to.substring(bar + 1).trim());
			if (bar >= 0) to = to.substring(0, bar).trim();
			swaps.put(fromId, new Swap(sprite(baker, to), period, noTint, side));
		}
		return (base, e, view, pos, state, random, cull) -> {
			e.pushTransform(q -> {
				TextureAtlasSprite from = finder.find(q);
				Swap sw = from == null ? null : swaps.get(from.contents().name());
				if (sw == null) return true;
				TextureAtlasSprite to = sw.side() != null && q.lightFace().getAxis().isHorizontal() ? sw.side() : sw.to();
				if (sw.noTint()) q.tintIndex(-1);
				if (sw.period() > 0) {
					float[] s = new float[4], t = new float[4];
					Direction f = q.lightFace();
					float per = sw.period();
					for (int i = 0; i < 4; i++) {
						float wx = pos.getX() + q.x(i), wy = pos.getY() + q.y(i), wz = pos.getZ() + q.z(i);
						switch (f.getAxis()) {
							case Y -> {
								s[i] = wx / per;
								t[i] = wz / per;
							}
							case Z -> {
								s[i] = wx / per;
								t[i] = -wy / per;
							}
							default -> {
								s[i] = wz / per;
								t[i] = -wy / per;
							}
						}
					}
					lay(q, to, s, t);
					return true;
				}
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
	 * The way a sign faces: a shelf's facing, or a standing sign's turn when it's square to the map (south, west,
	 * north or east); null for a standing sign at a slant (it keeps its own look).
	 */
	private static Direction facingOf(BlockState s) {
		if (s.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) return s.getValue(BlockStateProperties.HORIZONTAL_FACING);
		if (s.hasProperty(BlockStateProperties.ROTATION_16)) {
			int r = s.getValue(BlockStateProperties.ROTATION_16);
			if (r % 4 != 0) return null;
			return r == 0 ? Direction.SOUTH : r == 4 ? Direction.WEST : r == 8 ? Direction.NORTH : Direction.EAST;
		}
		return null;
	}

	private static BlockStateModel extra(String name) {
		ExtraModelKey<BlockStateModel> key = SIGN_PARTS.get(name);
		if (key == null) return null;
		try {
			return ((FabricBakedModelManager) Minecraft.getInstance().getModelManager()).getModel(key);
		} catch (RuntimeException ignored) {
			return null;
		}
	}

	/**
	 * Signs as HeartGold's: the shelves stacked as a sign (the top row the white board, the rows under it its grey
	 * stand), and standing pale oak signs (a block high: the board on a short stand, built round Minecraft's own
	 * sign, so that never shows). Each block is the sign's left end, right end or a middle as you look at its front,
	 * or the whole of it when the sign is one block wide.
	 */
	private static void sign(BlockStateModel base, QuadEmitter e, BlockAndTintGetter view, BlockPos pos, BlockState state, RandomSource random, Predicate<Direction> cull) {
		Direction facing = facingOf(state);
		if (facing == null) {
			base.emitQuads(e, view, pos, state, random, cull);
			return;
		}
		// You look at its front from in front of it: your left is its facing turned clockwise (north: east).
		Direction left = facing.getClockWise();
		boolean onLeft = sameSign(view, pos.relative(left), state, facing), onRight = sameSign(view, pos.relative(left.getOpposite()), state, facing);
		String part = onLeft && onRight ? "m" : onRight ? "l" : onLeft ? "r" : "s";
		String row = state.hasProperty(BlockStateProperties.ROTATION_16) ? "short" : sameSign(view, pos.above(), state, facing) ? "stand" : "board";
		BlockStateModel m = extra(row + "_" + part + "_" + facing.getSerializedName());
		if (m == null) base.emitQuads(e, view, pos, state, random, cull);
		else m.emitQuads(e, view, pos, state, random, cull);
	}

	/** A standing wooden sign as HeartGold's wooden sign, taller, built round Minecraft's own (that never shows). */
	private static void woodSign(BlockStateModel base, QuadEmitter e, BlockAndTintGetter view, BlockPos pos, BlockState state, RandomSource random, Predicate<Direction> cull) {
		Direction facing = facingOf(state);
		BlockStateModel m = facing == null ? null : extra("wood_" + facing.getSerializedName());
		if (m == null) base.emitQuads(e, view, pos, state, random, cull);
		else m.emitQuads(e, view, pos, state, random, cull);
	}

	/**
	 * A boulder (Diglett's Cave's rocks, John's tuff and cobblestone): a round mound of stone in place of the block, one
	 * of two shapes by where it is, and a low one for a slab.
	 */
	private static void boulder(BlockStateModel base, QuadEmitter e, BlockAndTintGetter view, BlockPos pos, BlockState state, RandomSource random, Predicate<Direction> cull) {
		boolean slab = state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.SLAB_TYPE);
		String name = slab ? "boulder_small" : Math.floorMod(pos.getX() * 31 + pos.getZ() * 17 + pos.getY() * 7, 2) == 0 ? "boulder_a" : "boulder_b";
		BlockStateModel m = extra(name);
		if (m == null) base.emitQuads(e, view, pos, state, random, cull);
		else m.emitQuads(e, view, pos, state, random, cull);
	}

	private static boolean sameSign(BlockAndTintGetter view, BlockPos p, BlockState as, Direction facing) {
		BlockState s = view.getBlockState(p);
		return s.is(as.getBlock()) && facingOf(s) == facing;
	}

	// ---------- the grass's tufts onto the dripstone round the water, and Viridian's kerbs ----------
	/**
	 * (The same for Viridian City's path, "kerb": a light grey kerb along the grass, rounded at the corners, and the
	 * grass's own small tufts onto it, on any of the path's blocks the grass meets.)
	 *
	 * The dripstone that borders water (within two blocks of it) takes the grass's rounded, tufted edge where grass
	 * meets it, as the dirt paths do (the same 17 tiles: an edge, two, three or all four, or a corner), with their
	 * soft shadow on the dripstone. The tufts take the grass's green (Colours tints them).
	 */
	private record GrassEdge(TextureAtlasSprite[] tufts, TextureAtlasSprite[] shade, boolean byWater) implements Look {
		@Override
		public void emit(BlockStateModel base, QuadEmitter e, BlockAndTintGetter view, BlockPos pos, BlockState state, RandomSource random, Predicate<Direction> cull) {
			base.emitQuads(e, view, pos, state, random, cull);
			if (cull.test(Direction.UP)) return;
			boolean n = grass(view, pos.north()), s = grass(view, pos.south()), w = grass(view, pos.west()), ea = grass(view, pos.east());
			boolean nw = grass(view, pos.north().west()), ne = grass(view, pos.north().east()), sw = grass(view, pos.south().west()), se = grass(view, pos.south().east());
			if (!(n || s || w || ea || nw || ne || sw || se) || byWater && !nearWater(view, pos)) return;
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
