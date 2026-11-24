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
		boolean has(BlockPos p, boolean inside) {
			return rooms == inside && p.getX() >= x0 && p.getX() <= x1 && p.getZ() >= z0 && p.getZ() <= z1 && (y0 > y1 || p.getY() >= y0 && p.getY() <= y1);
		}

		/** How big it is (blocks): where two swaps' areas overlap, the smaller one's (the more particular place's) is drawn. */
		long size() {
			return (long) (x1 - x0 + 1) * (z1 - z0 + 1) * (y0 > y1 ? 400 : y1 - y0 + 1);
		}
	}

	/** Whether the world you're in now is the interiors (the buildings' insides and the forest). */
	private static boolean inRooms() {
		var level = net.minecraft.client.Minecraft.getInstance().level;
		return level != null && level.dimension() == com.insidethecube.retromon.game.Interiors.LEVEL;
	}

	/** One look somewhere: its name, its texture swaps ("block/tuff=lamp_tuff", "@4" for laid over 4 blocks), its areas. */
	private record Rule(String look, String swaps, List<Box> boxes, Map<Long, Box[]> cells, List<Box> wide) {
		Rule(String look, String swaps, List<Box> boxes) {
			this(look, swaps, boxes, new java.util.concurrent.ConcurrentHashMap<>(), new ArrayList<>());
		}

		/**
		 * The smallest of its areas a spot's in (Long.MAX_VALUE: none). (0.48: looked up by the 64-block square the spot's
		 * in, not through every area of the look; the chunks are drawn much quicker, so coming out of a door is.)
		 */
		long area(BlockPos p) {
			boolean inside = inRooms();
			long best = Long.MAX_VALUE;
			Box[] here = cells.get(cell(p.getX() >> 6, p.getZ() >> 6));
			if (here != null) for (Box b : here) if (b.has(p, inside)) best = Math.min(best, b.size());
			for (Box b : wide) if (b.has(p, inside)) best = Math.min(best, b.size());
			return best;
		}

		private static long cell(int cx, int cz) {
			return ((long) cx << 32) ^ (cz & 0xFFFFFFFFL);
		}

		void index() {
			{
				Map<Long, List<Box>> m = new HashMap<>();
				for (Box b : boxes) {
					int cx0 = b.x0() >> 6, cx1 = b.x1() >> 6, cz0 = b.z0() >> 6, cz1 = b.z1() >> 6;
					if ((long) (cx1 - cx0 + 1) * (cz1 - cz0 + 1) > 64) {
						wide.add(b);
						continue;
					}
					for (int cx = cx0; cx <= cx1; cx++) for (int cz = cz0; cz <= cz1; cz++) m.computeIfAbsent(cell(cx, cz), k -> new ArrayList<>()).add(b);
				}
				for (Map.Entry<Long, List<Box>> e : m.entrySet()) cells.put(e.getKey(), e.getValue().toArray(new Box[0]));
			}
		}
	}


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
		// Each look's areas filed by the 64-block square they're in (all read in now, before the map's drawn).
		for (Map<String, Rule> m : RULES.values()) for (Rule r : m.values()) r.index();
	}

	public static void register() {
		ModelLoadingPlugin.register(ctx -> {
			for (Direction d : Direction.Plane.HORIZONTAL) {
				for (String part : new String[] {"l", "r", "m", "s"}) {
					for (String row : new String[] {"board", "stand", "short"}) part(ctx, row + "_" + part + "_" + d.getSerializedName(),
							row.equals("short") ? "block/area/sign_short_" + part + "_" + d.getSerializedName() : "block/area/sign_" + row + "_" + part + "_" + d.getSerializedName());
				}
				part(ctx, "wood_" + d.getSerializedName(), "block/area/wood_sign_" + d.getSerializedName());
				// (0.54) The coloured signs (Johto's Route 27: Kanto's orange beside the white): the sign, its board orange.
				for (String part : new String[] {"l", "r", "m", "s"})
					for (String row : new String[] {"board", "stand"}) part(ctx, "o" + row + "_" + part + "_" + d.getSerializedName(), "block/area/osign_" + row + "_" + part + "_" + d.getSerializedName());
				// (0.59.3, 20:35) The blue signs on the beaches: the board blue, on a pale stand.
				for (String part : new String[] {"l", "r", "m", "s"})
					for (String row : new String[] {"board", "stand"}) part(ctx, "b" + row + "_" + part + "_" + d.getSerializedName(), "block/area/bsign_" + row + "_" + part + "_" + d.getSerializedName());
				// The same signs in wood (wood_board: Pewter's oak shelf sign, as our wooden sign).
				for (String part : new String[] {"l", "r", "m", "s"}) {
					for (String row : new String[] {"board", "stand"}) part(ctx, "w" + row + "_" + part + "_" + d.getSerializedName(), "block/area/wsign_" + row + "_" + part + "_" + d.getSerializedName());
				}
			}
			for (String b : new String[] {"boulder_a", "boulder_b", "boulder_small", "mboulder_a", "mboulder_b", "mboulder_small", "iboulder_a", "iboulder_b",
					"iboulder_small", "rboulder_a", "rboulder_b", "rboulder_small", "sboulder_a", "sboulder_b", "sboulder_small", "ttrunk_a", "ttrunk_b", "ttrunk_small"})
				part(ctx, b, "block/area/" + b);
			// (0.50) The leafy trees (20:07): Shards' extra leaves, two ways round, for each kind of leaves.
			for (String t : LEAFY) for (String v : new String[] {"a", "b"}) part(ctx, "leafy_" + t + "_" + v, "block/area/leafy_" + t + "_" + v);
			part(ctx, "prop_bike", "block/area/prop_bike");
			part(ctx, "prop_statue", "block/area/prop_statue");
			part(ctx, "prop_trophy", "block/area/prop_trophy");
			part(ctx, "prop_mon_statue", "block/area/prop_mon_statue");
			// (0.57) The INDIGO PLATEAU's statues as Victory Road's (a stone POKe BALL), the Cinnabar Lab's solar panels.
			part(ctx, "prop_ball_statue", "block/area/prop_ball_statue");
			part(ctx, "prop_solar", "block/area/prop_solar");
			ctx.modifyBlockModelAfterBake().register(ModelModifier.WRAP_PHASE, (model, c) -> {
				Map<String, Rule> looks = RULES.get(c.state().getBlock());
				if (looks == null) return model;
					BlockStateModel out = model;
				// (15:21) Texture swaps don't pile up: where two of a block's swaps cover a spot (all Kanto's dripstone, and
				// Pallet's), the one for the smaller area is drawn there, the other not.
				List<Rule> swaps = new ArrayList<>();
				// ((0.59) The signs too: a shelf that's a sign in a small box is drawn as one even where a planter's or a
				// band's look covers the whole town, and a planter in a small box isn't a sign because the town's are.)
				for (Rule rule : looks.values()) if (rule.look().equals("swap") || rule.look().equals("panel") || rule.look().equals("glass") || rule.look().equals("hedge") || rule.look().equals("leafy") || rule.look().equals("leafy_hedge")
						|| rule.look().equals("sign") || rule.look().equals("sign_o") || rule.look().equals("sign_b") || rule.look().equals("keep") || rule.look().equals("hide") || rule.look().equals("cube")) swaps.add(rule);
				for (Rule rule : looks.values()) {
					Look look = make(rule, c.baker());
					List<Rule> rivals = new ArrayList<>();
					if (swaps.contains(rule)) for (Rule o : swaps) if (o != rule) rivals.add(o);
					if (look != null) out = new Wrapped(out, look, rule, List.copyOf(rivals));
				}
				return out;
			});
		});
	}

	/** The leaves with Shards' extra leaves (0.50): their names without "_leaves". */
	private static final String[] LEAFY = {"oak", "acacia", "birch", "azalea", "flowering_azalea", "dark_oak", "jungle", "mangrove", "cherry", "spruce"};

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
		private final Rule rule;
		private final List<Rule> rivals;

		Wrapped(BlockStateModel wrapped, Look look, Rule rule, List<Rule> rivals) {
			super(wrapped);
			this.look = look;
			this.rule = rule;
			this.rivals = rivals;
		}

		private boolean here(BlockPos pos) {
			if (!Adventure.inSave || pos == null) return false;
			long mine = rule.area(pos);
			if (mine == Long.MAX_VALUE) return false;
			for (Rule o : rivals) {
				long a = o.area(pos);
				if (a < mine || a == mine && o.swaps().compareTo(rule.swaps()) < 0) return false;
			}
			return true;
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
			// (0.57, 15:46: "the chimneys still have the shelf style ... tailor them to be a different style instead of looking
			// too shelfy") John's shelves used as trims, caps, bands and planters: a plain flat board where the shelf's back
			// is (no lip, no recess), in the swapped picture; anything else in the look swapped as usual.
			case "panel" -> panel(baker, rule.swaps());
			// (0.57, 15:46: "the cobblestone and mossy cobblestone ... blend together") The moss's soft edge onto the
			// cobblestone beside it, as the grass's onto the paths.
			case "moss_onto" -> {
				TextureAtlasSprite[] over = new TextureAtlasSprite[18];
				for (int t = 1; t <= 17; t++) over[t] = sprite(baker, "moss_on_cobble_" + t);
				yield new GrassEdge(over, null, false, java.util.Set.of(Blocks.MOSSY_COBBLESTONE, Blocks.MOSSY_COBBLESTONE_SLAB, Blocks.MOSSY_COBBLESTONE_STAIRS,
						Blocks.MOSS_BLOCK), false, null);
			}
			// (0.57, 15:57: "the bone, which is supposed to be steam ... opacity 30% and kind of make them a mist") Cinnabar's
			// volcano's steam: John's bone blocks as soft see-through steam, no faces between two of them.
			case "steam" -> steam(sprite(baker, rule.swaps().equals("-") ? "steam" : rule.swaps()));
			case "roof", "roof_green", "roof_gym", "roof_museum" -> new Roof(sprite(baker, rule.look() + "_tiles"), sprite(baker, rule.look() + "_edge"), sprite(baker, rule.look() + "_under"));
			case "sign" -> AreaLooks::sign;
			case "sign_o" -> (base, e, view, pos, state, random, cull) -> board("o", base, e, view, pos, state, random, cull);
			case "sign_b" -> (base, e, view, pos, state, random, cull) -> board("b", base, e, view, pos, state, random, cull);
			// (0.59.3, 20:47: the ships, "do not texture those") The block as it is, here, whatever the place's looks.
			case "keep" -> (base, e, view, pos, state, random, cull) -> base.emitQuads(e, view, pos, state, random, cull);
			// (0.59.4, 07:31: Nugget Bridge's "buttons on the far steps. Those buttons need removing") Not drawn at all.
			case "hide" -> (base, e, view, pos, state, random, cull) -> {};
			// (0.59.4, 07:31: Route 12's and Route 16's pillars "still using a stripped", their swap set) A full block drawn
			// in our own texture whatever picture it had ("top|side" or one for all): nothing of its old one can show.
			case "cube" -> {
				String[] p = rule.swaps().split("\\|");
				TextureAtlasSprite top = sprite(baker, p[0].trim()), side = p.length > 1 ? sprite(baker, p[1].trim()) : top;
				yield (base, e, view, pos, state, random, cull) -> {
					for (Direction d : Direction.values()) {
						if (cull.test(d)) continue;
						e.square(d, 0, 0, 1, 1, 0);
						float[] ss = new float[4], tt = new float[4];
						for (int i = 0; i < 4; i++) {
							switch (d.getAxis()) {
								case Y -> {
									ss[i] = e.x(i);
									tt[i] = e.z(i);
								}
								case Z -> {
									ss[i] = d == Direction.NORTH ? 1 - e.x(i) : e.x(i);
									tt[i] = 1 - e.y(i);
								}
								default -> {
									ss[i] = d == Direction.EAST ? 1 - e.z(i) : e.z(i);
									tt[i] = 1 - e.y(i);
								}
							}
						}
						TextureAtlasSprite t = d.getAxis() == Direction.Axis.Y ? top : side;
						for (int i = 0; i < 4; i++)
							e.uv(i, t.getU0() + clamp01(ss[i]) * (t.getU1() - t.getU0()), t.getV0() + clamp01(tt[i]) * (t.getV1() - t.getV0()));
						e.color(-1, -1, -1, -1);
						e.tintIndex(-1);
						e.emit();
					}
				};
			}
			// (0.59.4, 07:31: the Museum's "brown band ... behind the sign") A wall sign used as a band: a board in the
			// texture just round the one the game draws (so that never shows), flat against the wall. spec: the texture.
			case "wallboard" -> wallboard(sprite(baker, rule.swaps()));
			// (0.59.3, 20:35) The WHIRL ISLANDS' whirlpools: a turning whirlpool on the water's surface over five blocks,
			// drawn by a rock beside it ("dx,dz,size": where its middle is from the rock).
			case "whirlpool" -> {
				String[] p = rule.swaps().split(",");
				float dx = Float.parseFloat(p[0].trim()), dz = Float.parseFloat(p[1].trim()), size = p.length > 2 ? Float.parseFloat(p[2].trim()) : 5F;
				TextureAtlasSprite sp = sprite(baker, "whirlpool");
				yield (base, e, view, pos, state, random, cull) -> {
					base.emitQuads(e, view, pos, state, random, cull);
					e.square(Direction.UP, 0, 0, 1, 1, 0.07F);
					for (int i = 0; i < 4; i++) {
						float cx = e.x(i), cz = e.z(i);
						e.pos(i, dx + 0.5F + (cx - 0.5F) * size, e.y(i), dz + 0.5F + (cz - 0.5F) * size);
						e.uv(i, sp.getU0() + cx * (sp.getU1() - sp.getU0()), sp.getV0() + cz * (sp.getV1() - sp.getV0()));
					}
					e.color(-1, -1, -1, -1);
					e.tintIndex(-1);
					e.renderLayer(ChunkSectionLayer.TRANSLUCENT);
					e.emit();
				};
			}
			case "wood_board" -> AreaLooks::woodBoard;
			case "wood_sign" -> AreaLooks::woodSign;
			case "boulder" -> boulder(baker, rule.swaps());
			case "glass" -> glass(baker, rule.swaps(), false);
			// (16:17) Leaves as a trimmed hedge: one solid hedge along a row, its edges rounded where it ends.
			case "hedge" -> glass(baker, rule.swaps(), true);
			case "chain" -> AreaLooks::chain;
			// (0.50, 20:07) Trees leafy and rounded, not blocky: Shards' extra leaves standing out of the leaves that show.
			case "leafy" -> AreaLooks::leafy;
			// (0.50.3, 07:38) The hedges as the little trees are (John liked them): all a hedge's leaves the one kind's
			// picture (the azalea's, or the flowering azalea's where it has flowers), with that kind's extra leaves.
			case "leafy_hedge" -> {
				String kind = rule.swaps();
				TextureAtlasSprite leaf = baker.sprites().get(new Material(TextureAtlas.LOCATION_BLOCKS, Identifier.withDefaultNamespace("block/" + kind + "_leaves")),
						() -> "retromon hedges");
				yield (base, e, view, pos, state, random, cull) -> leafyHedge(base, leaf, kind, e, view, pos, state, random, cull);
			}
			// (0.50, 20:12) Something stood on a block (the bikes outside the Bike Shop): its own model over it.
			case "prop" -> {
				String name = "prop_" + rule.swaps();
				yield (base, e, view, pos, state, random, cull) -> {
					base.emitQuads(e, view, pos, state, random, cull);
					BlockStateModel m = extra(name);
					if (m != null) m.emitQuads(e, view, pos, state, random, d -> false);
				};
			}
			// (0.52, 10:41) A model of the mod's in place of the block (ROUTE 23's statues: John's middle block of each),
			// and the rest of what it stands for not drawn at all.
			case "prop_only" -> {
				String name = "prop_" + rule.swaps();
				yield (base, e, view, pos, state, random, cull) -> {
					BlockStateModel m = extra(name);
					if (m != null) m.emitQuads(e, view, pos, state, random, d -> false);
					else base.emitQuads(e, view, pos, state, random, cull);
				};
			}
			case "hide" -> (base, e, view, pos, state, random, cull) -> {
			};
			// (19:06) Lavender Town's low mist: two thin lilac layers of haze over the ground, laid over four blocks.
			case "mist" -> mist(sprite(baker, rule.swaps().equals("-") ? "mist_lav" : rule.swaps()));
			case "grass_onto" -> {
				// (15:29) The grass's soft tufted edge onto what's beside it (Fuchsia's beach sand, its moss, the red path's edge).
				TextureAtlasSprite[] over = new TextureAtlasSprite[18];
				for (int t = 1; t <= 17; t++) over[t] = sprite(baker, "grass_on_drip_" + t);
				java.util.Set<Block> from = rule.swaps().equals("grass") ? java.util.Set.of(Blocks.GRASS_BLOCK) : null;
				// ((0.59.3, 20:35: "That connected texture onto the sand actually should be the moss block") Where the moss
				// meets it, the moss's own soft edge, not the grass's.)
				TextureAtlasSprite[] moss = new TextureAtlasSprite[18];
				for (int t = 1; t <= 17; t++) moss[t] = sprite(baker, "moss_on_cobble_" + t);
				yield new GrassEdge(over, null, false, from, true, from == null ? moss : null);
			}
			case "sand_edge" -> {
				// (The cave's sand running softly onto the floor beside it, as the grass onto the dripstone; 14:15.)
				TextureAtlasSprite[] over = new TextureAtlasSprite[18];
				for (int t = 1; t <= 17; t++) over[t] = sprite(baker, "sand_on_floor_" + t);
				yield new GrassEdge(over, null, false, java.util.Set.of(Blocks.SAND, Blocks.SMOOTH_SANDSTONE), false, null);
			}
			case "grass_edge", "kerb" -> {
				boolean kerb = rule.look().equals("kerb");
				TextureAtlasSprite[] over = new TextureAtlasSprite[18], under = new TextureAtlasSprite[18];
				for (int t = 1; t <= 17; t++) {
					over[t] = sprite(baker, (kerb ? "kerb_grass_" : "grass_on_drip_") + t);
					under[t] = sprite(baker, (kerb ? "kerb_" : "drip_shade_") + t);
				}
				yield new GrassEdge(over, under, !kerb, null, true, null);
			}
			// (Any other roof_ look, Fuchsia's four houses' and the Safari Zone's, 15:29: its own three pictures.)
			default -> rule.look().startsWith("roof_") ? new Roof(sprite(baker, rule.look() + "_tiles"), sprite(baker, rule.look() + "_edge"), sprite(baker, rule.look() + "_under")) : null;
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
		// (0.59.4, 07:31: Route 12's and Route 16's pillars still drawn as stripped logs, their look set) A picture a
		// resource pack draws in place of the block's own (connected or random textures: "block/ctm/stripped_spruce_log/3",
		// "stripped_spruce_log_2") is the same block's, so it's swapped as the block's own is.
		Map<Identifier, Swap> seen = new java.util.concurrent.ConcurrentHashMap<>();
		Swap none = new Swap(null, 0, false, null);
		return (base, e, view, pos, state, random, cull) -> {
			e.pushTransform(q -> {
				TextureAtlasSprite from = finder.find(q);
				Swap sw = from == null ? null : seen.computeIfAbsent(from.contents().name(), id -> {
					Swap own = swaps.get(id);
					return own != null ? own : likeOf(swaps, id, none);
				});
				if (sw == null || sw == none) return true;
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

	/** A wall sign as a plain board against the wall, just round the game's own board (that never shows). */
	private static Look wallboard(TextureAtlasSprite tex) {
		return (base, e, view, pos, state, random, cull) -> {
			Direction facing = state.hasProperty(BlockStateProperties.HORIZONTAL_FACING) ? state.getValue(BlockStateProperties.HORIZONTAL_FACING) : null;
			if (facing == null) {
				base.emitQuads(e, view, pos, state, random, cull);
				return;
			}
			// The board in the block's own frame facing north (against the south side), turned round to its facing.
			float y0 = 4.3F / 16F, y1 = 12.7F / 16F, z0 = 13.75F / 16F, z1 = 1F, x0 = -0.004F, x1 = 1.004F;
			float[][] box = {{x0, y0, z0}, {x1, y1, z1}};
			for (Direction face : Direction.values()) {
				if (face == Direction.SOUTH) continue;
				// Corners of this face (in the north-facing frame), then turned.
				float[][] c = corners(face, box);
				for (int i = 0; i < 4; i++) {
					float[] p = turn(c[i], facing);
					e.pos(i, p[0], p[1], p[2]);
				}
				for (int i = 0; i < 4; i++) {
					float u = face.getAxis() == Direction.Axis.Y ? c[i][0] : face.getAxis() == Direction.Axis.Z ? c[i][0] : c[i][2];
					float v = face.getAxis() == Direction.Axis.Y ? c[i][2] : 1F - c[i][1];
					e.uv(i, tex.getU0() + clamp01(u) * (tex.getU1() - tex.getU0()), tex.getV0() + clamp01(v) * (tex.getV1() - tex.getV0()));
				}
				e.color(-1, -1, -1, -1);
				e.tintIndex(-1);
				e.nominalFace(turnDir(face, facing));
				e.cullFace(null);
				e.emit();
			}
		};
	}

	/** A box's face's four corners (counter-clockwise looking at it from outside). */
	private static float[][] corners(Direction f, float[][] b) {
		float ax = b[0][0], ay = b[0][1], az = b[0][2], bx = b[1][0], by = b[1][1], bz = b[1][2];
		return switch (f) {
			case NORTH -> new float[][] {{bx, by, az}, {bx, ay, az}, {ax, ay, az}, {ax, by, az}};
			case SOUTH -> new float[][] {{ax, by, bz}, {ax, ay, bz}, {bx, ay, bz}, {bx, by, bz}};
			case WEST -> new float[][] {{ax, by, az}, {ax, ay, az}, {ax, ay, bz}, {ax, by, bz}};
			case EAST -> new float[][] {{bx, by, bz}, {bx, ay, bz}, {bx, ay, az}, {bx, by, az}};
			case UP -> new float[][] {{ax, by, az}, {ax, by, bz}, {bx, by, bz}, {bx, by, az}};
			case DOWN -> new float[][] {{ax, ay, bz}, {ax, ay, az}, {bx, ay, az}, {bx, ay, bz}};
		};
	}

	/** A point in a north-facing block turned to face `to` (about the block's middle). */
	private static float[] turn(float[] p, Direction to) {
		float x = p[0] - 0.5F, z = p[2] - 0.5F;
		return switch (to) {
			case SOUTH -> new float[] {0.5F - x, p[1], 0.5F - z};
			case WEST -> new float[] {0.5F + z, p[1], 0.5F - x};
			case EAST -> new float[] {0.5F - z, p[1], 0.5F + x};
			default -> new float[] {p[0], p[1], p[2]};
		};
	}

	private static Direction turnDir(Direction d, Direction to) {
		if (d.getAxis() == Direction.Axis.Y) return d;
		return switch (to) {
			case SOUTH -> d.getOpposite();
			case WEST -> d.getCounterClockWise();
			case EAST -> d.getClockWise();
			default -> d;
		};
	}

	/**
	 * The swap for a picture a resource pack put in place of one of the swapped ones (`none` when there's none): one
	 * whose name has the swapped picture's name as one of its parts ("block/ctm/stripped_spruce_log/3") or as its last
	 * part with a number after it ("stripped_spruce_log_2").
	 */
	private static Swap likeOf(Map<Identifier, Swap> swaps, Identifier id, Swap none) {
		String[] parts = id.getPath().split("/");
		String last = parts[parts.length - 1];
		for (Map.Entry<Identifier, Swap> s : swaps.entrySet()) {
			String p = s.getKey().getPath();
			String name = p.substring(p.lastIndexOf('/') + 1);
			if (id.getPath().equals(p)) return s.getValue();
			for (int i = 0; i < parts.length - 1; i++) if (parts[i].equals(name)) return s.getValue();
			if (last.startsWith(name + "_") && last.substring(name.length() + 1).chars().allMatch(Character::isDigit) && last.length() > name.length() + 1)
				return s.getValue();
		}
		return none;
	}

	private static float clamp01(float v) {
		return v < 0 ? 0 : v > 1 ? 1 : v;
	}

	/** Sets a quad's corners to `sprite` at (s, t) in blocks (one block = the whole texture), from where they are. */
	/** Mist over the ground: the block as it is, then haze at knee and waist height above it, where it's open. */
	private static Look mist(TextureAtlasSprite haze) {
		return (base, e, view, pos, state, random, cull) -> {
			base.emitQuads(e, view, pos, state, random, cull);
			BlockPos up = pos.above();
			if (!view.getBlockState(up).getCollisionShape(view, up).isEmpty()) return;
			for (float h : new float[] {1.35F, 1.85F}) {
				e.square(Direction.UP, 0, 0, 1, 1, 1 - h);
				float shift = h > 1.5F ? 0.5F : 0F;
				float[] ss = new float[4], tt = new float[4];
				for (int i = 0; i < 4; i++) {
					ss[i] = (pos.getX() + e.x(i) + shift * 4) / 4F;
					tt[i] = (pos.getZ() + e.z(i) + shift * 4) / 4F;
				}
				lay(e, haze, ss, tt);
				e.color(-1, -1, -1, -1);
				e.tintIndex(-1);
				e.renderLayer(ChunkSectionLayer.TRANSLUCENT);
				e.emit();
			}
		};
	}

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
		board("", base, e, view, pos, state, random, cull);
	}

	/** Shelves as the sign in wood (our wooden sign's look): Pewter City's TRAINER TIPS. */
	private static void woodBoard(BlockStateModel base, QuadEmitter e, BlockAndTintGetter view, BlockPos pos, BlockState state, RandomSource random, Predicate<Direction> cull) {
		if (state.hasProperty(BlockStateProperties.ROTATION_16)) {
			base.emitQuads(e, view, pos, state, random, cull);
			return;
		}
		board("w", base, e, view, pos, state, random, cull);
	}

	private static void board(String kind, BlockStateModel base, QuadEmitter e, BlockAndTintGetter view, BlockPos pos, BlockState state, RandomSource random, Predicate<Direction> cull) {
		Direction facing = facingOf(state);
		if (facing == null) {
			base.emitQuads(e, view, pos, state, random, cull);
			return;
		}
		// You look at its front from in front of it: your left is its facing turned clockwise (north: east).
		Direction left = facing.getClockWise();
		boolean onLeft = sameSign(view, pos.relative(left), state, facing), onRight = sameSign(view, pos.relative(left.getOpposite()), state, facing);
		String part = onLeft && onRight ? "m" : onRight ? "l" : onLeft ? "r" : "s";
		// ((0.59.3) A pale oak shelf under a warped one is the blue sign's stand.)
		BlockState up = view.getBlockState(pos.above());
		boolean blueOver = state.is(Blocks.PALE_OAK_SHELF) && up.is(Blocks.WARPED_SHELF) && facingOf(up) == facing;
		String row = state.hasProperty(BlockStateProperties.ROTATION_16) ? "short" : sameSign(view, pos.above(), state, facing) || blueOver ? "stand" : "board";
		// ((0.59.4, 07:36: "wherever I had these single ones, the bottom can be made of a wood ... and then the top one is
		// the sign as normal") A sign one block wide stands on a wooden post: the shelf under its board, whichever shelf
		// the board is (Cinnabar's Gym's is a mangrove one on a pale oak one).)
		String kindNow = kind;
		boolean shelfOver = !state.hasProperty(BlockStateProperties.ROTATION_16) && !up.is(Blocks.WARPED_SHELF) && facingOf(up) == facing
				&& BuiltInRegistries.BLOCK.getKey(up.getBlock()).getPath().endsWith("_shelf");
		if (kind.isEmpty() && part.equals("s") && !blueOver && (row.equals("stand") || shelfOver)) {
			kindNow = "w";
			row = "stand";
		}
		BlockStateModel m = extra(kindNow + row + "_" + part + "_" + facing.getSerializedName());
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
	 * A boulder (Diglett's Cave's rocks, John's tuff and cobblestone; Mt. Moon's granite): a round mound of stone in place
	 * of the block, one of two shapes by where it is, and a low one for a slab; the floor under it drawn as the floor
	 * round it is laid (its block below's texture, over four blocks), so no square shows (14:15). spec: "prefix:mboulder"
	 * (which stone's models) and block=texture@N for the floor under it by what's below.
	 */
	private static Look boulder(ModelBaker baker, String spec) {
		String prefix = "boulder";
		Map<Block, Swap> floors = new HashMap<>();
		for (String one : spec.split(",")) {
			one = one.trim();
			if (one.startsWith("prefix:")) {
				prefix = one.substring(7).trim();
				continue;
			}
			int eq = one.indexOf('=');
			if (eq < 0) continue;
			String to = one.substring(eq + 1).trim();
			int period = 1, at = to.indexOf('@');
			if (at >= 0) {
				period = Integer.parseInt(to.substring(at + 1).trim());
				to = to.substring(0, at);
			}
			Block b = BuiltInRegistries.BLOCK.getOptional(Identifier.withDefaultNamespace(one.substring(0, eq).trim())).orElse(null);
			if (b != null && b != Blocks.AIR) floors.put(b, new Swap(sprite(baker, to), period, true, null));
		}
		String pre = prefix;
		return (base, e, view, pos, state, random, cull) -> {
			// (On another rock, a slab on Mt. Moon's granite: one boulder there, the one below. A small tree's trunk goes
			// on up, 17:51.)
			boolean trunk = pre.startsWith("ttrunk");
			if (!trunk && rock(view.getBlockState(pos.below()).getBlock())) return;
			// (A slab's a low one; so is the room's paler grate, 15:21: the rocks are grates in the caves now, hiding nothing.)
			boolean slab = state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.SLAB_TYPE) || state.is(Blocks.WAXED_EXPOSED_COPPER_GRATE);
			String name = pre + (slab ? "_small" : trunk ? (view.getBlockState(pos.below()).is(state.getBlock()) ? "_b" : "_a")
					: Math.floorMod(pos.getX() * 31 + pos.getZ() * 17 + pos.getY() * 7, 2) == 0 ? "_a" : "_b");
			Swap fl = floors.get(view.getBlockState(pos.below()).getBlock());
			if (fl != null) {
				e.square(Direction.UP, 0, 0, 1, 1, 0.998F);
				float per = fl.period();
				float[] ss = new float[4], tt = new float[4];
				for (int i = 0; i < 4; i++) {
					ss[i] = (pos.getX() + e.x(i)) / per;
					tt[i] = (pos.getZ() + e.z(i)) / per;
				}
				lay(e, fl.to(), ss, tt);
				e.color(-1, -1, -1, -1);
				e.tintIndex(-1);
				e.emit();
			}
			BlockStateModel m = extra(name);
			if (m == null) base.emitQuads(e, view, pos, state, random, cull);
			else m.emitQuads(e, view, pos, state, random, cull);
		};
	}

	/**
	 * Glass as one pane (15:21): the window's glass tinted, framed only round its outside, so the panes of a window run
	 * into one another. Each face gets one of sixteen pictures ("name_0" to "name_15", by which of its four edges have no
	 * more of the same glass beyond them: 1 its top, 2 its right, 4 its bottom, 8 its left as the picture lies), drawn see-through.
	 * spec: "block/glass=glass_clear" (old=the pictures' name).
	 */
	private static Look glass(ModelBaker baker, String spec, boolean hedge) {
		SpriteFinder finder = baker.sprites().spriteFinder(TextureAtlas.LOCATION_BLOCKS);
		Map<Identifier, TextureAtlasSprite[]> kinds = new HashMap<>();
		for (String one : spec.split(",")) {
			int eq = one.indexOf('=');
			if (eq < 0) continue;
			String from = one.substring(0, eq).trim(), to = one.substring(eq + 1).trim();
			TextureAtlasSprite[] v = new TextureAtlasSprite[16];
			for (int m = 0; m < 16; m++) v[m] = sprite(baker, to + "_" + m);
			kinds.put(from.contains(":") ? Identifier.parse(from) : Identifier.withDefaultNamespace(from), v);
		}
		return (base, e, view, pos, state, random, cull) -> {
			Block self = state.getBlock();
			e.pushTransform(q -> {
				TextureAtlasSprite from = finder.find(q);
				TextureAtlasSprite[] v = from == null ? null : kinds.get(from.contents().name());
				if (v == null) return true;
				float fu = from.getU1() - from.getU0(), fv = from.getV1() - from.getV0();
				float[] nu = new float[4], nv = new float[4];
				for (int i = 0; i < 4; i++) {
					nu[i] = fu == 0 ? 0 : clamp01((q.u(i) - from.getU0()) / fu);
					nv[i] = fv == 0 ? 0 : clamp01((q.v(i) - from.getV0()) / fv);
				}
				// Which way across the world the picture's u and v run on this face (from its corners).
				Direction du = along(q, nu), dv = along(q, nv);
				int mask = 0;
				if (dv != null && !joins(view, pos, dv.getOpposite(), self, hedge)) mask |= 1;
				if (du != null && !joins(view, pos, du, self, hedge)) mask |= 2;
				if (dv != null && !joins(view, pos, dv, self, hedge)) mask |= 4;
				if (du != null && !joins(view, pos, du.getOpposite(), self, hedge)) mask |= 8;
				TextureAtlasSprite to = v[mask];
				for (int i = 0; i < 4; i++) q.uv(i, to.getU0() + nu[i] * (to.getU1() - to.getU0()), to.getV0() + nv[i] * (to.getV1() - to.getV0()));
				if (hedge) {
					q.renderLayer(ChunkSectionLayer.CUTOUT);
					q.tintIndex(-1);
				} else q.renderLayer(ChunkSectionLayer.TRANSLUCENT);
				return true;
			});
			base.emitQuads(e, view, pos, state, random, cull);
			e.popTransform();
			if (hedge) {
				TextureAtlasSprite[] v = kinds.get(BuiltInRegistries.BLOCK.getKey(self).withPrefix("block/"));
				if (v != null) tufts(e, view, pos, v[0]);
			}
		};
	}

	/** The way across the world a quad's picture coordinate (t, at its corners) grows, or null if it doesn't. */
	private static Direction along(MutableQuadView q, float[] t) {
		int a = 0, b = 0;
		for (int i = 0; i < 4; i++) {
			if (t[i] < t[a]) a = i;
			if (t[i] > t[b]) b = i;
		}
		if (t[b] - t[a] < 1e-4F) return null;
		float dx = q.x(b) - q.x(a), dy = q.y(b) - q.y(a), dz = q.z(b) - q.z(a);
		float ax = Math.abs(dx), ay = Math.abs(dy), az = Math.abs(dz);
		if (ax >= ay && ax >= az) return dx > 0 ? Direction.EAST : Direction.WEST;
		if (ay >= az) return dy > 0 ? Direction.UP : Direction.DOWN;
		return dz > 0 ? Direction.SOUTH : Direction.NORTH;
	}

	/** Whether a face runs on into its neighbour: glass into the same glass, a hedge into any leaves. */
	private static boolean joins(BlockAndTintGetter view, BlockPos pos, Direction d, Block self, boolean hedge) {
		if (!hedge) return same(view, pos, d, self);
		// (17:42) A long hedge is a row of clipped bushes, not one flat block: now and then a seam between two, each
		// rounded off there (the same seam seen from both sides).
		if (d.getAxis().isHorizontal() && seam(pos, d)) return false;
		return view.getBlockState(pos.relative(d)).is(net.minecraft.tags.BlockTags.LEAVES);
	}

	/** Whether there's a seam in a hedge between this block and the next one along (every three or four blocks). */
	private static boolean seam(BlockPos pos, Direction d) {
		int x = pos.getX(), z = pos.getZ();
		if (d == Direction.WEST) x--;
		if (d == Direction.NORTH) z--;
		boolean alongX = d.getAxis() == Direction.Axis.X;
		int along = alongX ? x : z, across = alongX ? z : x;
		int h = (across * 73856093) ^ (Math.floorDiv(along, 7) * 19349663);
		int at = Math.floorMod(h, 7);
		// One or two seams in each seven blocks along, at places that differ row to row.
		return Math.floorMod(along, 7) == at || Math.floorMod(along, 7) == Math.floorMod(at + 3 + (h >>> 8 & 1), 7);
	}

	/**
	 * (17:42) A hedge's leafy depth: little clipped tufts standing up off its top (and out of its open sides), a few
	 * pixels each, so it isn't one flat face; drawn in its own leaves' picture.
	 */
	private static void tufts(QuadEmitter e, BlockAndTintGetter view, BlockPos pos, TextureAtlasSprite sprite) {
		// (0.50, 20:12) In line: two low clipped tufts along the top of each block, in a row with the next block's,
		// a little lower or higher by where they are; now and then one out of an open side, low and close.
		long h = net.minecraft.util.Mth.getSeed(pos.getX(), pos.getY(), pos.getZ());
		boolean top = !view.getBlockState(pos.above()).is(net.minecraft.tags.BlockTags.LEAVES) && !view.getBlockState(pos.above()).isSolidRender();
		boolean alongX = view.getBlockState(pos.east()).is(net.minecraft.tags.BlockTags.LEAVES) || view.getBlockState(pos.west()).is(net.minecraft.tags.BlockTags.LEAVES);
		for (int i = 0; i < 2 && top; i++) {
			float a0 = (i == 0 ? 1.5F : 9.0F) / 16F, w = 5.5F / 16F, hh = (1 + ((h >>> (9 + i * 7)) & 1)) / 16F;
			float b0 = 4.5F / 16F, d = 7F / 16F;
			if (alongX) cube(e, sprite, a0, 1, b0, a0 + w, 1 + hh, b0 + d);
			else cube(e, sprite, b0, 1, a0, b0 + d, 1 + hh, a0 + w);
		}
		for (Direction dd : Direction.Plane.HORIZONTAL) {
			BlockState o = view.getBlockState(pos.relative(dd));
			if (o.is(net.minecraft.tags.BlockTags.LEAVES) || o.isSolidRender()) continue;
			if (((h >>> (40 + dd.get2DDataValue() * 3)) & 3) != 0) continue;
			float a = (4 + ((h >>> (48 + dd.get2DDataValue() * 3)) & 3)) / 16F, w = 5 / 16F, y0 = 6 / 16F;
			float t = 0.75F / 16F;
			switch (dd) {
				case NORTH -> cube(e, sprite, a, y0, -t, a + w, y0 + w, 0);
				case SOUTH -> cube(e, sprite, a, y0, 1, a + w, y0 + w, 1 + t);
				case WEST -> cube(e, sprite, -t, y0, a, 0, y0 + w, a + w);
				default -> cube(e, sprite, 1, y0, a, 1 + t, y0 + w, a + w);
			}
		}
	}

	/**
	 * (0.50, 20:07) Leaves with Shards of Arcadia's extra leaves: three soft planes of leaves standing out of the block,
	 * so a tree's crown is rounded and leafy rather than cubes. Only on leaves that show (with open air or a plant
	 * beside them), so a forest's insides cost nothing; two ways round, by where it is, so no two look the same.
	 */
	private static void leafy(BlockStateModel base, QuadEmitter e, BlockAndTintGetter view, BlockPos pos, BlockState state, RandomSource random, Predicate<Direction> cull) {
		base.emitQuads(e, view, pos, state, random, cull);
		boolean open = false;
		for (Direction d : Direction.values()) {
			BlockState o = view.getBlockState(pos.relative(d));
			if (!o.is(net.minecraft.tags.BlockTags.LEAVES) && !o.isSolidRender()) {
				open = true;
				break;
			}
		}
		if (!open) return;
		String name = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
		if (!name.endsWith("_leaves")) return;
		long h = net.minecraft.util.Mth.getSeed(pos.getX(), pos.getY(), pos.getZ());
		BlockStateModel m = extra("leafy_" + name.substring(0, name.length() - 7) + ((h >>> 5 & 1) == 0 ? "_a" : "_b"));
		if (m != null) m.emitQuads(e, view, pos, state, random, d -> false);
	}

	/** (0.50.3) A hedge's leaves: drawn in one kind's picture, untinted (as the azaleas are), with its extra leaves. */
	private static void leafyHedge(BlockStateModel base, TextureAtlasSprite leaf, String kind, QuadEmitter e, BlockAndTintGetter view, BlockPos pos,
			BlockState state, RandomSource random, Predicate<Direction> cull) {
		e.pushTransform(q -> {
			q.spriteBake(leaf, MutableQuadView.BAKE_LOCK_UV);
			q.tintIndex(-1);
			return true;
		});
		base.emitQuads(e, view, pos, state, random, cull);
		e.popTransform();
		boolean open = false;
		for (Direction d : Direction.values()) {
			BlockState o = view.getBlockState(pos.relative(d));
			if (!o.is(net.minecraft.tags.BlockTags.LEAVES) && !o.isSolidRender()) {
				open = true;
				break;
			}
		}
		if (!open) return;
		long h = net.minecraft.util.Mth.getSeed(pos.getX(), pos.getY(), pos.getZ());
		BlockStateModel m = extra("leafy_" + kind + ((h >>> 5 & 1) == 0 ? "_a" : "_b"));
		if (m != null) m.emitQuads(e, view, pos, state, random, d -> false);
	}

	/**
	 * A shelf as a flat board (0.57): the shelf's back (5 pixels deep, against what it's on) as one box in its swapped
	 * picture, so a band of John's shelves round a chimney or a planter reads as a solid trim; other blocks of the look
	 * are swapped as a swap look swaps them.
	 */
	private static Look panel(ModelBaker baker, String spec) {
		Look other = swap(baker, spec);
		Map<Identifier, TextureAtlasSprite> pics = new HashMap<>();
		for (String one : spec.split(",")) {
			int eq = one.indexOf('=');
			if (eq < 0) continue;
			String from = one.substring(0, eq).trim(), to = one.substring(eq + 1).trim();
			int cut = to.length();
			for (char c : new char[] {'@', '|', '!'}) if (to.indexOf(c) >= 0) cut = Math.min(cut, to.indexOf(c));
			pics.put(from.contains(":") ? Identifier.parse(from) : Identifier.withDefaultNamespace(from), sprite(baker, to.substring(0, cut).trim()));
		}
		return (base, e, view, pos, state, random, cull) -> {
			Identifier key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
			TextureAtlasSprite pic = key.getPath().endsWith("_shelf") ? pics.get(Identifier.fromNamespaceAndPath(key.getNamespace(), "block/" + key.getPath())) : null;
			if (pic == null || !state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
				other.emit(base, e, view, pos, state, random, cull);
				return;
			}
			final float d = 5 / 16F;
			switch (state.getValue(BlockStateProperties.HORIZONTAL_FACING)) {
				case NORTH -> cube(e, pic, 0, 0, 1 - d, 1, 1, 1);
				case SOUTH -> cube(e, pic, 0, 0, 0, 1, 1, d);
				case EAST -> cube(e, pic, 0, 0, 0, d, 1, 1);
				default -> cube(e, pic, 1 - d, 0, 0, 1, 1, 1);
			}
		};
	}

	/** Steam (0.57): each face of the block that isn't against another of the same, see-through, its picture laid over 4 blocks. */
	private static Look steam(TextureAtlasSprite pic) {
		return (base, e, view, pos, state, random, cull) -> {
			for (Direction d : Direction.values()) {
				if (view.getBlockState(pos.relative(d)).is(state.getBlock())) continue;
				e.square(d, 0, 0, 1, 1, 0);
				float[] ss = new float[4], tt = new float[4];
				for (int i = 0; i < 4; i++) {
					float wx = pos.getX() + e.x(i), wy = pos.getY() + e.y(i), wz = pos.getZ() + e.z(i);
					switch (d.getAxis()) {
						case Y -> {
							ss[i] = wx / 4F;
							tt[i] = wz / 4F;
						}
						case Z -> {
							ss[i] = wx / 4F;
							tt[i] = -wy / 4F;
						}
						default -> {
							ss[i] = wz / 4F;
							tt[i] = -wy / 4F;
						}
					}
				}
				lay(e, pic, ss, tt);
				e.color(-1, -1, -1, -1);
				e.tintIndex(-1);
				e.renderLayer(ChunkSectionLayer.TRANSLUCENT);
				e.emit();
			}
		};
	}

	/** A little box (block units), every face in one picture. */
	private static void cube(QuadEmitter e, TextureAtlasSprite sprite, float x0, float y0, float z0, float x1, float y1, float z1) {
		for (Direction d : Direction.values()) {
			switch (d) {
				case UP -> e.square(d, x0, 1 - z1, x1, 1 - z0, 1 - y1);
				case DOWN -> e.square(d, x0, z0, x1, z1, y0);
				case NORTH -> e.square(d, 1 - x1, y0, 1 - x0, y1, z0);
				case SOUTH -> e.square(d, x0, y0, x1, y1, 1 - z1);
				case WEST -> e.square(d, z0, y0, z1, y1, x0);
				default -> e.square(d, 1 - z1, y0, 1 - z0, y1, 1 - x1);
			}
			e.spriteBake(sprite, MutableQuadView.BAKE_LOCK_UV);
			e.color(-1, -1, -1, -1);
			e.tintIndex(-1);
			e.renderLayer(ChunkSectionLayer.SOLID);
			e.emit();
		}
	}

	private static boolean same(BlockAndTintGetter view, BlockPos pos, Direction d, Block self) {
		return view.getBlockState(pos.relative(d)).is(self);
	}

	/**
	 * A chain hung between two walls (Fuchsia's fences, 15:29): kept a chain, but reaching into the posts either side, so
	 * it hangs from them rather than stopping short in the air.
	 */
	private static void chain(BlockStateModel base, QuadEmitter e, BlockAndTintGetter view, BlockPos pos, BlockState state, RandomSource random, Predicate<Direction> cull) {
		Direction.Axis axis = state.hasProperty(BlockStateProperties.AXIS) ? state.getValue(BlockStateProperties.AXIS) : Direction.Axis.Y;
		if (axis == Direction.Axis.Y) {
			base.emitQuads(e, view, pos, state, random, cull);
			return;
		}
		Direction lo = axis == Direction.Axis.X ? Direction.WEST : Direction.NORTH;
		boolean a = post(view, pos.relative(lo)), b = post(view, pos.relative(lo.getOpposite()));
		if (!a && !b) {
			base.emitQuads(e, view, pos, state, random, cull);
			return;
		}
		final float reach = 0.3F;
		e.pushTransform(q -> {
			for (int i = 0; i < 4; i++) {
				float x = q.x(i), y = q.y(i), z = q.z(i);
				if (axis == Direction.Axis.X) {
					if (a && x < 0.01F) x = -reach;
					else if (b && x > 0.99F) x = 1 + reach;
				} else {
					if (a && z < 0.01F) z = -reach;
					else if (b && z > 0.99F) z = 1 + reach;
				}
				q.pos(i, x, y, z);
			}
			return true;
		});
		base.emitQuads(e, view, pos, state, random, cull);
		e.popTransform();
	}

	private static boolean post(BlockAndTintGetter view, BlockPos p) {
		BlockState s = view.getBlockState(p);
		return s.is(net.minecraft.tags.BlockTags.WALLS) || s.is(net.minecraft.tags.BlockTags.FENCES);
	}

	/** Whether a block's a boulder somewhere (a rock look's on it). */
	private static boolean rock(Block b) {
		Map<String, Rule> looks = RULES.get(b);
		if (looks == null) return false;
		for (String k : looks.keySet()) if (k.startsWith("boulder|")) return true;
		return false;
	}

	private static boolean sameSign(BlockAndTintGetter view, BlockPos p, BlockState as, Direction facing) {
		BlockState s = view.getBlockState(p);
		// ((0.54) A white sign beside an orange one (pale oak and acacia shelves) is one sign.)
		boolean family = (s.is(Blocks.PALE_OAK_SHELF) || s.is(Blocks.ACACIA_SHELF)) && (as.is(Blocks.PALE_OAK_SHELF) || as.is(Blocks.ACACIA_SHELF));
		return (s.is(as.getBlock()) || family) && facingOf(s) == facing;
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
	private record GrassEdge(TextureAtlasSprite[] tufts, TextureAtlasSprite[] shade, boolean byWater, java.util.Set<Block> onto, boolean tint, TextureAtlasSprite[] moss) implements Look {
		@Override
		public void emit(BlockStateModel base, QuadEmitter e, BlockAndTintGetter view, BlockPos pos, BlockState state, RandomSource random, Predicate<Direction> cull) {
			base.emitQuads(e, view, pos, state, random, cull);
			if (cull.test(Direction.UP)) return;
			if (byWater && !nearWater(view, pos)) return;
			if (moss != null) {
				// The grass's tufts where the grass meets it, the moss's where the moss does.
				edges(e, view, pos, p -> grassOnly(view, p), tufts, true);
				edges(e, view, pos, p -> view.getBlockState(p).is(Blocks.MOSS_BLOCK), moss, false);
				return;
			}
			edges(e, view, pos, p -> by(view, p), tufts, tint);
		}

		private static boolean grassOnly(BlockAndTintGetter view, BlockPos p) {
			BlockState s = view.getBlockState(p);
			return s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.GREEN_CONCRETE_POWDER);
		}

		private void edges(QuadEmitter e, BlockAndTintGetter view, BlockPos pos, java.util.function.Predicate<BlockPos> by, TextureAtlasSprite[] tufts, boolean tint) {
			boolean n = by.test(pos.north()), s = by.test(pos.south()), w = by.test(pos.west()), ea = by.test(pos.east());
			boolean nw = by.test(pos.north().west()), ne = by.test(pos.north().east()), sw = by.test(pos.south().west()), se = by.test(pos.south().east());
			if (!(n || s || w || ea || nw || ne || sw || se)) return;
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
			if (shade != null) for (int t : tiles) top(e, shade[t], -1);
			// (0.50, 20:07) The grass's green worked out here, so the tufts are green on any block (the sand, the red path,
			// Lavender's flagstones), not only those that take the grass's tint (white dots, before).
			int green = -1;
			if (tint) {
				int c = net.minecraft.client.renderer.BiomeColors.getAverageGrassColor(view, pos);
				green = Adventure.inSave ? Colours.brighten(c) : 0xFF000000 | c;
			}
			for (int t : tiles) top(e, tufts[t], green);
		}

		/** Whether what's beside it runs onto it: grass (onto the dripstone, the kerb), or the sand (onto a cave's floor). */
		private boolean by(BlockAndTintGetter view, BlockPos p) {
			return onto == null ? grass(view, p) : onto.contains(view.getBlockState(p).getBlock()) && view.getBlockState(p.above()).isAir();
		}

		private static void top(QuadEmitter e, TextureAtlasSprite sprite, int colour) {
			e.square(Direction.UP, 0, 0, 1, 1, 0);
			e.spriteBake(sprite, MutableQuadView.BAKE_LOCK_UV);
			e.color(colour, colour, colour, colour);
			e.tintIndex(-1);
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
