package com.insidethecube.retromon.client;

import com.insidethecube.retromon.Retromon;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.SpriteFinder;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.block.model.SingleVariant;
import net.minecraft.client.renderer.block.model.Variant;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * (0.63) The battle halls' planks, walls and rock, laid from where each block stands. A texture that repeats over more
 * than a block (planks over 4, a wall over 4 across by 3 up) would otherwise need a model for every place in its
 * repeat, and the thirty halls would add tens of thousands of models. In a hall's models such a face is marked instead
 * (tools/interior/export/tomod.py: tintindex 100000000 + its repeat across x 10000000 + up x 1000000 + the picture's
 * width x 1000 + its height, in 64ths of the padded texture), and here it gets its picture from the world: across it
 * the way the Interior Viewer lays it, a repeat every so many blocks. The halls go up on a whole number of repeats
 * (BattleScene.setUpArena), so each falls just as the viewer shows it. Faces without the mark are left as they are.
 */
public final class PropTiles {
	private PropTiles() {}

	private static final int MARK = 100000000;

	public static void register() {
		ModelLoadingPlugin.register(ctx -> {
			// (0.67, John's 16:07: "the mod is getting really slow to start") Each state's model given straight to
			// Minecraft (retromon/interiors/prop_models.json, in order), not through a blockstates file: Minecraft matches
			// a blockstates file's every variant against every one of the block's states, and with 37 thousand states that
			// was more than a billion checks each time it started.
			// ((0.67) Over all the rooms' blocks, each state its look's model; looks past the last share the first's.)
			int[] models = propModels();
			Map<Integer, BlockStateModel.UnbakedRoot> roots = new HashMap<>();
			for (com.insidethecube.retromon.game.Prop p : com.insidethecube.retromon.game.Prop.ALL) {
				if (p == null) continue;
				ctx.registerBlockStateResolver(p, r -> {
					for (BlockState st : r.block().getStateDefinition().getPossibleStates()) {
						int g = com.insidethecube.retromon.game.Prop.look(st);
						int model = g >= 0 && g < models.length ? models[g] : models.length > 0 ? models[0] : 0;
						r.setModel(st, roots.computeIfAbsent(model, k -> new SingleVariant.Unbaked(new Variant(Retromon.id("block/prop/" + k))).asRoot()));
					}
				});
			}
			ctx.modifyBlockModelAfterBake().register((model, c) -> {
				if (!(c.state().getBlock() instanceof com.insidethecube.retromon.game.Prop) || model == null) return model;
				return new Tiled(model, c.baker().sprites().spriteFinder(TextureAtlas.LOCATION_BLOCKS));
			});
		});
	}

	/** Each prop state's model number, in order. */
	private static int[] propModels() {
		try (java.io.InputStream in = PropTiles.class.getResourceAsStream("/retromon/interiors/prop_models.json")) {
			if (in == null) throw new java.io.IOException("not found");
			com.google.gson.JsonArray a = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8))
					.getAsJsonObject().getAsJsonArray("models");
			int[] out = new int[a.size()];
			for (int i = 0; i < out.length; i++) out[i] = a.get(i).getAsInt();
			return out;
		} catch (Exception e) {
			Retromon.LOG.error("Retromon: couldn't read the rooms' block models", e);
			return new int[0];
		}
	}

	private static final class Tiled extends WrapperBlockStateModel {
		private final SpriteFinder finder;

		Tiled(BlockStateModel wrapped, SpriteFinder finder) {
			super(wrapped);
			this.finder = finder;
		}

		@Override
		public void emitQuads(QuadEmitter emitter, BlockAndTintGetter view, BlockPos pos, BlockState state, RandomSource random, Predicate<Direction> cull) {
			if (pos == null) {
				wrapped.emitQuads(emitter, view, pos, state, random, cull);
				return;
			}
			// (0.66) In a room that has its light (worked out in the Interior Viewer): each piece's corners coloured with it.
			RoomShade.At lit = RoomShade.at(pos);
			emitter.pushTransform(q -> {
				lay(q, pos, finder);
				double[] mat = RoomShade.matOf(q, finder);
				// (0.66) Glass as the viewer draws it: see-through, over what's behind (not cut out).
				if (mat != null && mat[3] > 0) q.renderLayer(ChunkSectionLayer.TRANSLUCENT);
				if (lit != null) RoomShade.colour(q, pos, lit, mat);
				return true;
			});
			wrapped.emitQuads(emitter, view, pos, state, random, cull);
			emitter.popTransform();
		}

		@Override
		public Object createGeometryKey(BlockAndTintGetter view, BlockPos pos, BlockState state, RandomSource random) {
			return null;
		}
	}

	/** A marked face's picture, from where its corners are in the world (as tomod.py works it out for a room's). */
	private static void lay(MutableQuadView q, BlockPos pos, SpriteFinder finder) {
		int t = q.tintIndex();
		if (t < MARK) return;
		int pu = t / 10000000 % 10, pv = t / 1000000 % 10;
		double sx = (t / 1000 % 1000) / 64.0, sy = (t % 1000) / 64.0;
		q.tintIndex(-1);
		if (pu <= 0 || pv <= 0) return;
		TextureAtlasSprite sprite = finder.find(q);
		if (sprite == null) return;
		// The face's way across (R) and up (U), as the viewer's boxes have them for each side.
		int rx, ry, rz, ux, uy, uz;
		switch (q.lightFace()) {
			case WEST -> { rx = 0; ry = 0; rz = 1; ux = 0; uy = 1; uz = 0; }
			case EAST -> { rx = 0; ry = 0; rz = -1; ux = 0; uy = 1; uz = 0; }
			case NORTH -> { rx = -1; ry = 0; rz = 0; ux = 0; uy = 1; uz = 0; }
			case SOUTH -> { rx = 1; ry = 0; rz = 0; ux = 0; uy = 1; uz = 0; }
			case UP -> { rx = 1; ry = 0; rz = 0; ux = 0; uy = 0; uz = -1; }
			default -> { rx = 1; ry = 0; rz = 0; ux = 0; uy = 0; uz = 1; }
		}
		double[] u = new double[4], v = new double[4];
		double umin = Double.MAX_VALUE, vmin = Double.MAX_VALUE;
		for (int i = 0; i < 4; i++) {
			double wx = pos.getX() + (double) q.x(i), wy = pos.getY() + (double) q.y(i), wz = pos.getZ() + (double) q.z(i);
			u[i] = (wx * rx + wy * ry + wz * rz) / pu;
			v[i] = (wx * ux + wy * uy + wz * uz) / pv;
			umin = Math.min(umin, u[i]);
			vmin = Math.min(vmin, v[i]);
		}
		// The repeat the piece is in (it's never across two: the faces are cut at the repeats).
		double ku = Math.floor(umin + 1e-4), kv = Math.floor(vmin + 1e-4);
		float u0 = sprite.getU0(), u1 = sprite.getU1(), v0 = sprite.getV0(), v1 = sprite.getV1();
		for (int i = 0; i < 4; i++) {
			double fu = Math.max(0, Math.min(1, u[i] - ku)), fv = Math.max(0, Math.min(1, v[i] - kv));
			q.uv(i, (float) (u0 + fu * sx * (u1 - u0)), (float) (v0 + (1 - fv) * sy * (v1 - v0)));
		}
	}
}
