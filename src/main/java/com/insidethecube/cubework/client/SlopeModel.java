package com.insidethecube.cubework.client;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.joml.Vector3f;

import com.insidethecube.cubework.Material;
import com.insidethecube.cubework.block.GentleSlopeBlock;
import com.insidethecube.cubework.block.SlopeBlock;
import com.insidethecube.cubework.block.SlopeGeometry;

import net.minecraft.block.BlockState;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.StairShape;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.Baker;
import net.minecraft.client.render.model.BlockModelPart;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.client.render.model.ResolvableModel;
import net.minecraft.client.render.model.SimpleModel;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

/**
 * The slope drawn as it really is: one flat sloping surface (two triangles at a corner), triangles on the
 * sides and a square underneath, using the vanilla block's own textures (top, sides, bottom, and grass's
 * tinted overlay). Built from the same heights as its hit box. It goes through the game's ordinary block
 * model, so anything that draws blocks (Voxy, shader mods) draws it too.
 */
public final class SlopeModel {
	private SlopeModel() {}

	public record Unbaked(Material material) implements BlockStateModel.UnbakedGrouped {
		@Override
		public void resolve(ResolvableModel.Resolver resolver) {
		}

		@Override
		public Object getEqualityGroup(BlockState state) {
			return state;
		}

		@Override
		public BlockStateModel bake(BlockState state, Baker baker) {
			Material.Textures t = material.textures();
			SimpleModel named = () -> "cubework slope " + material.id();
			Sprites s = new Sprites(sprite(baker, t.top(), named), sprite(baker, t.side(), named), sprite(baker, t.bottom(), named),
					t.overlay() == null ? null : sprite(baker, t.overlay(), named), material.grassTinted());
			SlopeBlock block = (SlopeBlock) state.getBlock();
			return new Baked(new Part(quads(state, s, block.base(state) / 16.0, block.rise(state) / 16.0, material.height() / 16.0,
					block instanceof GentleSlopeBlock), s.side()));
		}

		private static Sprite sprite(Baker baker, String texture, SimpleModel named) {
			return baker.getSpriteGetter().get(new SpriteIdentifier(Uv.BLOCK_ATLAS, Identifier.ofVanilla("block/" + texture)), named);
		}
	}

	/** The block's textures; tinted means the top and overlay take the biome's grass colour. */
	private record Sprites(Sprite top, Sprite side, Sprite bottom, Sprite overlay, boolean tinted) {}

	private record Baked(Part part) implements BlockStateModel {
		@Override
		public void addParts(Random random, List<BlockModelPart> parts) {
			parts.add(part);
		}

		@Override
		public Sprite particleSprite() {
			return part.sprite;
		}
	}

	/** Quads by the side they're hidden from, and the sloping surface (never hidden). */
	private record Quads(Map<Direction, List<BakedQuad>> sided, List<BakedQuad> free) {}

	private static final class Part implements BlockModelPart {
		private final Map<Direction, List<BakedQuad>> sided;
		private final List<BakedQuad> free;
		final Sprite sprite;

		Part(Quads quads, Sprite sprite) {
			this.free = List.copyOf(quads.free());
			this.sided = quads.sided();
			this.sprite = sprite;
		}

		@Override
		public List<BakedQuad> getQuads(Direction side) {
			if (side == null) return free;
			List<BakedQuad> q = sided.get(side);
			return q == null ? List.of() : q;
		}

		@Override
		public boolean useAmbientOcclusion() {
			return true;
		}

		@Override
		public Sprite particleSprite() {
			return sprite;
		}
	}

	/**
	 * base and rise: where the slope starts and how far it climbs (as parts of a block); top: the block's
	 * height; world: hang side textures from the block's top (gentle steps line up) rather than the slope.
	 */
	private static Quads quads(BlockState state, Sprites s, double base, double rise, double height, boolean world) {
		Direction f = state.get(SlopeBlock.FACING);
		StairShape shape = state.get(SlopeBlock.SHAPE);
		boolean top = state.get(SlopeBlock.HALF) == BlockHalf.TOP;
		Map<Direction, List<BakedQuad>> out = new EnumMap<>(Direction.class);
		List<BakedQuad> free = new ArrayList<>();
		Builder b = new Builder(top, height, base, rise, world);
		int tint = s.tinted() ? 0 : -1;

		// Corner heights (0 or 1): north-west, north-east, south-east, south-west.
		double[][] c = {{0, 0}, {1, 0}, {1, 1}, {0, 1}};
		double[] h = new double[4];
		for (int i = 0; i < 4; i++) h[i] = Math.round(SlopeGeometry.height(f, shape, c[i][0], c[i][1]));

		// The flat face: underneath, or on top when upside down (where it's the block's top).
		Direction flat = top ? Direction.UP : Direction.DOWN;
		add(out, flat, b.flat(top ? s.top() : s.bottom(), top ? tint : -1));

		// The sloping surface: the block's top texture (its bottom texture when upside down).
		Sprite surface = top ? s.bottom() : s.top();
		int surfaceTint = top ? -1 : tint;
		int odd = oddCorner(h);
		if (odd < 0) {
			free.add(b.surface(surface, surfaceTint, new double[][] {v(c[0], h[0]), v(c[1], h[1]), v(c[2], h[2]), v(c[3], h[3])}));
		} else {
			int a = odd, o = (odd + 2) % 4, l = (odd + 1) % 4, r = (odd + 3) % 4;
			free.add(b.surface(surface, surfaceTint, new double[][] {v(c[a], h[a]), v(c[l], h[l]), v(c[o], h[o]), v(c[o], h[o])}));
			free.add(b.surface(surface, surfaceTint, new double[][] {v(c[a], h[a]), v(c[o], h[o]), v(c[r], h[r]), v(c[r], h[r])}));
		}

		// The four sides: a square where both corners are high, a triangle where one is, on top of the
		// solid part a gentle slope's step stands on. Grass gets its tinted overlay on top, so the green edge
		// follows the slope.
		Direction[] sides = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
		for (int i = 0; i < 4; i++) {
			int p = i, q = (i + 1) % 4;
			if (base > 0) add(out, sides[i], b.footing(s.side(), sides[i], c[p], c[q]));
			if (h[p] == 0 && h[q] == 0) continue;
			add(out, sides[i], b.side(s.side(), -1, sides[i], c[p], h[p], c[q], h[q]));
			if (s.overlay() != null) add(out, sides[i], b.side(s.overlay(), tint, sides[i], c[p], h[p], c[q], h[q]));
		}
		return new Quads(out, free);
	}

	private static void add(Map<Direction, List<BakedQuad>> out, Direction d, BakedQuad q) {
		out.computeIfAbsent(d, k -> new ArrayList<>()).add(q);
	}

	/** The corner whose height differs from the other three, or -1 when there are two of each (one plane). */
	private static int oddCorner(double[] h) {
		for (int i = 0; i < 4; i++) {
			int same = 0;
			for (int j = 0; j < 4; j++) if (j != i && h[j] == h[i]) same++;
			if (same == 0) return i;
		}
		return -1;
	}

	private static double[] v(double[] xz, double y) {
		return new double[] {xz[0], y, xz[1]};
	}

	/**
	 * Makes quads in block space. Heights come in as 0 to 1 of the piece; they're scaled to the block's
	 * height (15/16 for dirt path) and turned upside down for the top half.
	 */
	private static final class Builder {
		private final boolean top;
		private final double height, base, rise;
		private final boolean world;

		Builder(boolean top, double height, double base, double rise, boolean world) {
			this.top = top;
			this.height = height;
			this.base = base;
			this.rise = rise;
			this.world = world;
		}

		/** A point on the slope (0 to 1 up its rise) in block space, upside down for the top half. */
		private double y(double t) {
			return at(base + t * rise);
		}

		/** A height in block space, upside down for the top half. */
		private double at(double y) {
			return top ? height - y : y;
		}

		/** The solid part under a gentle slope's step, on one side. */
		BakedQuad footing(Sprite sprite, Direction d, double[] a, double[] b) {
			double[][] p = {{a[0], at(0), a[1]}, {b[0], at(0), b[1]}, {b[0], at(base), b[1]}, {a[0], at(base), a[1]}};
			float[][] uv = new float[4][];
			for (int i = 0; i < 4; i++) uv[i] = new float[] {texU(new double[] {p[i][0], 0, p[i][2]}, d), (float) (1 - p[i][1])};
			return quad(sprite, -1, p, uv, d, d);
		}

		BakedQuad flat(Sprite sprite, int tint) {
			double y = top ? height : 0;
			Direction face = top ? Direction.UP : Direction.DOWN;
			double[][] p = {{0, y, 0}, {1, y, 0}, {1, y, 1}, {0, y, 1}};
			float[][] uv = new float[4][];
			for (int i = 0; i < 4; i++) uv[i] = texFlat(p[i], face);
			return quad(sprite, tint, p, uv, face, face);
		}

		BakedQuad surface(Sprite sprite, int tint, double[][] pts) {
			double[][] p = new double[4][];
			Direction face = top ? Direction.DOWN : Direction.UP;
			float[][] uv = new float[4][];
			for (int i = 0; i < 4; i++) {
				p[i] = new double[] {pts[i][0], y(pts[i][1]), pts[i][2]};
				uv[i] = texFlat(p[i], face);
			}
			return quad(sprite, tint, p, uv, face, face);
		}

		/**
		 * A side between corners a and b (heights ha, hb). The texture is hung from the top of the piece,
		 * so a side texture's top band (grass, a path's edge, sandstone's rim) runs along the slope.
		 */
		BakedQuad side(Sprite sprite, int tint, Direction d, double[] a, double ha, double[] b, double hb) {
			List<double[]> local = new ArrayList<>();
			List<double[]> col = new ArrayList<>();
			local.add(new double[] {a[0], 0, a[1]});
			col.add(new double[] {ha});
			local.add(new double[] {b[0], 0, b[1]});
			col.add(new double[] {hb});
			if (hb > 0) {
				local.add(new double[] {b[0], hb, b[1]});
				col.add(new double[] {hb});
			}
			if (ha > 0) {
				local.add(new double[] {a[0], ha, a[1]});
				col.add(new double[] {ha});
			}
			while (local.size() < 4) {
				local.add(local.get(local.size() - 1));
				col.add(col.get(col.size() - 1));
			}
			double[][] p = new double[4][];
			float[][] uv = new float[4][];
			for (int i = 0; i < 4; i++) {
				double[] l = local.get(i);
				p[i] = new double[] {l[0], y(l[1]), l[2]};
				double fromTop = top ? l[1] * rise : (col.get(i)[0] - l[1]) * rise;
				float v = world ? (float) (1 - p[i][1]) : (float) fromTop;
				uv[i] = new float[] {texU(l, d), v};
			}
			return quad(sprite, tint, p, uv, d, d);
		}

		/** A quad facing out along normal, its corners wound so it shows from outside. */
		private static BakedQuad quad(Sprite sprite, int tint, double[][] p, float[][] uv, Direction face, Direction normal) {
			if (!facesOut(p, normal)) {
				p = new double[][] {p[0], p[3], p[2], p[1]};
				uv = new float[][] {uv[0], uv[3], uv[2], uv[1]};
			}
			Vector3f[] pos = new Vector3f[4];
			long[] packed = new long[4];
			for (int i = 0; i < 4; i++) {
				pos[i] = new Vector3f((float) p[i][0], (float) p[i][1], (float) p[i][2]);
				packed[i] = Uv.pack(sprite.getFrameU(uv[i][0]), sprite.getFrameV(uv[i][1]));
			}
			return new BakedQuad(pos[0], pos[1], pos[2], pos[3], packed[0], packed[1], packed[2], packed[3], tint, face, sprite, true, 0);
		}

		private static boolean same(double[] a, double[] b) {
			return a[0] == b[0] && a[1] == b[1] && a[2] == b[2];
		}

		private static boolean facesOut(double[][] p, Direction normal) {
			double[] a = p[0], b = p[1], c = same(p[2], p[1]) ? p[3] : p[2];
			double ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2];
			double vx = c[0] - a[0], vy = c[1] - a[1], vz = c[2] - a[2];
			double nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
			return nx * normal.getOffsetX() + ny * normal.getOffsetY() + nz * normal.getOffsetZ() > 0;
		}

		/** Top and bottom: the texture seen from above (or below), as on a full block. */
		private static float[] texFlat(double[] p, Direction face) {
			return face == Direction.UP ? new float[] {(float) p[0], (float) p[2]} : new float[] {(float) p[0], (float) (1 - p[2])};
		}

		/** Sides: across, as on the side of a full block. */
		private static float texU(double[] p, Direction face) {
			return switch (face) {
				case NORTH -> (float) (1 - p[0]);
				case SOUTH -> (float) p[0];
				case WEST -> (float) p[2];
				default -> (float) (1 - p[2]);
			};
		}
	}
}
