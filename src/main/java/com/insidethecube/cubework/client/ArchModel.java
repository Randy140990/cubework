package com.insidethecube.cubework.client;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

import com.insidethecube.cubework.Material;
import com.insidethecube.cubework.block.ArchBlock;
import com.insidethecube.cubework.block.ArchCornerBlock;
import com.insidethecube.cubework.block.ArchGeometry;

/**
 * Arches drawn with a smooth curve: the front and back are strips under the curve, the underside is a band
 * of narrow faces following it, using the vanilla block's textures. Like slopes, it goes through the game's
 * ordinary block model, so Voxy and shader mods draw it too.
 */
public final class ArchModel {
	private ArchModel() {}

	public record Unbaked(Material material) implements BlockStateModel.UnbakedRoot {
		@Override
		public void resolveDependencies(ResolvableModel.Resolver resolver) {
		}

		@Override
		public Object visualEqualityGroup(BlockState state) {
			return state;
		}

		@Override
		public BlockStateModel bake(BlockState state, ModelBaker baker) {
			Material.Textures t = material.textures();
			ModelDebugName named = () -> "cubework arch " + material.id();
			TextureAtlasSprite top = sprite(baker, t.top(), named), side = sprite(baker, t.side(), named), bottom = sprite(baker, t.bottom(), named);
			boolean corner = state.getBlock() instanceof ArchCornerBlock;
			int turns = corner ? ArchCornerBlock.turns(state.getValue(ArchCornerBlock.FACING))
					: state.getValue(ArchBlock.AXIS) == Direction.Axis.X ? 1 : 0;
			return new SlopeModel.Baked(new SlopeModel.Part(quads(corner, turns, top, side, bottom), side));
		}

		private static TextureAtlasSprite sprite(ModelBaker baker, String texture, ModelDebugName named) {
			return baker.sprites().get(new net.minecraft.client.resources.model.Material(Uv.BLOCK_ATLAS, Identifier.withDefaultNamespace("block/" + texture)), named);
		}
	}

	/** Built facing north and south (seen from the front, x across, y up), then turned clockwise. */
	private static SlopeModel.Quads quads(boolean corner, int turns, TextureAtlasSprite top, TextureAtlasSprite side, TextureAtlasSprite bottom) {
		Map<Direction, List<BakedQuad>> out = new EnumMap<>(Direction.class);
		List<BakedQuad> free = new ArrayList<>();
		double[][] c = ArchGeometry.curve(corner);
		double cx = ArchGeometry.centre(corner), a = ArchGeometry.width(corner), b = ArchGeometry.HEIGHT;

		for (int i = 0; i + 1 < c.length; i++) {
			double x0 = c[i][0], y0 = c[i][1], x1 = c[i + 1][0], y1 = c[i + 1][1];
			// Front and back: the strip between the curve and the top.
			for (Direction d : new Direction[] {Direction.NORTH, Direction.SOUTH}) {
				double z = d == Direction.NORTH ? 0 : 1;
				double[][] p = {{x0, y0, z}, {x1, y1, z}, {x1, 1, z}, {x0, 1, z}};
				add(out, turn(d, turns), quad(side, p, sideUv(p, d), d, axis(d), turns));
			}
			// Underside: facing into the opening; a ceiling where it's flatter, a wall where it's steeper.
			double mx = (x0 + x1) / 2, my = (y0 + y1) / 2;
			double nx = -(mx - cx) / (a * a), ny = -my / (b * b);
			Direction face = Math.abs(ny) >= Math.abs(nx) ? Direction.DOWN : nx < 0 ? Direction.WEST : Direction.EAST;
			double[][] p = {{x0, y0, 0}, {x1, y1, 0}, {x1, y1, 1}, {x0, y0, 1}};
			float[][] uv = face == Direction.DOWN ? flatUv(p, face) : sideUv(p, face);
			free.add(quad(face == Direction.DOWN ? bottom : side, p, uv, face, new double[] {nx, ny, 0}, turns));
		}

		double[][] topFace = {{0, 1, 0}, {1, 1, 0}, {1, 1, 1}, {0, 1, 1}};
		add(out, turn(Direction.UP, turns), quad(top, topFace, flatUv(topFace, Direction.UP), Direction.UP, axis(Direction.UP), turns));

		// The ends: from the curve up to the top.
		for (Direction d : new Direction[] {Direction.WEST, Direction.EAST}) {
			double x = d == Direction.WEST ? 0 : 1, y = d == Direction.WEST ? c[0][1] : c[c.length - 1][1];
			if (y >= 1) continue;
			double[][] p = {{x, y, 0}, {x, y, 1}, {x, 1, 1}, {x, 1, 0}};
			add(out, turn(d, turns), quad(side, p, sideUv(p, d), d, axis(d), turns));
		}
		return new SlopeModel.Quads(out, free);
	}

	private static void add(Map<Direction, List<BakedQuad>> out, Direction d, BakedQuad q) {
		out.computeIfAbsent(d, k -> new ArrayList<>()).add(q);
	}

	private static double[] axis(Direction d) {
		return new double[] {d.getStepX(), d.getStepY(), d.getStepZ()};
	}

	private static Direction turn(Direction d, int turns) {
		for (int i = 0; i < turns; i++) if (d.getAxis().isHorizontal()) d = d.getClockWise();
		return d;
	}

	/** A quad wound to show from the side its normal points to, then turned with the block. */
	private static BakedQuad quad(TextureAtlasSprite sprite, double[][] p, float[][] uv, Direction face, double[] n, int turns) {
		double ux = p[1][0] - p[0][0], uy = p[1][1] - p[0][1], uz = p[1][2] - p[0][2];
		double vx = p[2][0] - p[0][0], vy = p[2][1] - p[0][1], vz = p[2][2] - p[0][2];
		double cx = uy * vz - uz * vy, cy = uz * vx - ux * vz, cz = ux * vy - uy * vx;
		if (cx * n[0] + cy * n[1] + cz * n[2] < 0) {
			p = new double[][] {p[0], p[3], p[2], p[1]};
			uv = new float[][] {uv[0], uv[3], uv[2], uv[1]};
		}
		Vector3f[] pos = new Vector3f[4];
		long[] packed = new long[4];
		for (int i = 0; i < 4; i++) {
			double x = p[i][0], z = p[i][2];
			for (int t = 0; t < turns; t++) {
				double nx = 1 - z, nz = x;
				x = nx;
				z = nz;
			}
			pos[i] = new Vector3f((float) x, (float) p[i][1], (float) z);
			packed[i] = Uv.pack(sprite.getU(uv[i][0]), sprite.getV(uv[i][1]));
		}
		return new BakedQuad(pos[0], pos[1], pos[2], pos[3], packed[0], packed[1], packed[2], packed[3], -1, turn(face, turns), sprite, true, 0);
	}

	/** Sides: across as on a full block's side, top of the texture at the top of the block. */
	private static float[][] sideUv(double[][] p, Direction face) {
		float[][] uv = new float[4][];
		for (int i = 0; i < 4; i++) {
			double u = switch (face) {
				case NORTH -> 1 - p[i][0];
				case SOUTH -> p[i][0];
				case WEST -> p[i][2];
				default -> 1 - p[i][2];
			};
			uv[i] = new float[] {(float) u, (float) (1 - p[i][1])};
		}
		return uv;
	}

	/** Top and underneath: as seen from above or below. */
	private static float[][] flatUv(double[][] p, Direction face) {
		float[][] uv = new float[4][];
		for (int i = 0; i < 4; i++) {
			uv[i] = face == Direction.UP ? new float[] {(float) p[i][0], (float) p[i][2]} : new float[] {(float) p[i][0], (float) (1 - p[i][2])};
		}
		return uv;
	}
}
