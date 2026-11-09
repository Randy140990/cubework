package com.insidethecube.cubework.client;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.joml.Vector3f;

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
 * sides and a square underneath, using the vanilla block's texture. Built from the same heights as its
 * hit box. It goes through the game's ordinary block model, so anything that draws blocks (Voxy, shader
 * mods) draws it too.
 */
public final class SlopeModel {
	private SlopeModel() {}

	public record Unbaked(Identifier texture) implements BlockStateModel.UnbakedGrouped {
		@Override
		public void resolve(ResolvableModel.Resolver resolver) {
		}

		@Override
		public Object getEqualityGroup(BlockState state) {
			return state;
		}

		@Override
		public BlockStateModel bake(BlockState state, Baker baker) {
			SimpleModel named = () -> "cubework slope " + texture;
			Sprite sprite = baker.getSpriteGetter().get(new SpriteIdentifier(Uv.BLOCK_ATLAS, texture), named);
			return new Baked(new Part(quads(state, sprite), sprite));
		}
	}

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

	/** Quads by the side they're hidden from (null: never hidden). */
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

	/** Quads hidden by a neighbour on one side, and the sloping surface, which is never hidden. */
	private record Quads(Map<Direction, List<BakedQuad>> sided, List<BakedQuad> free) {}

	private static Quads quads(BlockState state, Sprite sprite) {
		Direction f = state.get(SlopeBlock.FACING);
		StairShape shape = state.get(SlopeBlock.SHAPE);
		boolean top = state.get(SlopeBlock.HALF) == BlockHalf.TOP;
		Map<Direction, List<BakedQuad>> out = new EnumMap<>(Direction.class);
		List<BakedQuad> free = new ArrayList<>();
		Builder b = new Builder(sprite, top);

		// Corner heights: north-west, north-east, south-east, south-west.
		double[][] c = {{0, 0}, {1, 0}, {1, 1}, {0, 1}};
		double[] h = new double[4];
		for (int i = 0; i < 4; i++) h[i] = Math.round(SlopeGeometry.height(f, shape, c[i][0], c[i][1]));

		// Underneath (or on top, upside down).
		Direction under = top ? Direction.UP : Direction.DOWN;
		add(out, under, b.flat());

		// The sloping surface: one quad when it's a single plane, else two triangles split through the odd corner.
		int odd = oddCorner(h);
		if (odd < 0) {
			free.add(b.surface(new double[][] {v(c[0], h[0]), v(c[1], h[1]), v(c[2], h[2]), v(c[3], h[3])}));
		} else {
			int a = odd, o = (odd + 2) % 4, l = (odd + 1) % 4, r = (odd + 3) % 4;
			free.add(b.surface(new double[][] {v(c[a], h[a]), v(c[l], h[l]), v(c[o], h[o]), v(c[o], h[o])}));
			free.add(b.surface(new double[][] {v(c[a], h[a]), v(c[o], h[o]), v(c[r], h[r]), v(c[r], h[r])}));
		}

		// The four sides: a square where both corners are high, a triangle where one is.
		Direction[] sides = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
		for (int i = 0; i < 4; i++) {
			int p = i, q = (i + 1) % 4;
			if (h[p] == 0 && h[q] == 0) continue;
			BakedQuad quad = b.side(sides[i], c[p], h[p], c[q], h[q]);
			add(out, sides[i], quad);
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

	/** Makes quads in block space (0 to 1), turned upside down for the top half. */
	private static final class Builder {
		private final Sprite sprite;
		private final boolean top;

		Builder(Sprite sprite, boolean top) {
			this.sprite = sprite;
			this.top = top;
		}

		private double y(double y) {
			return top ? 1 - y : y;
		}

		BakedQuad flat() {
			double y = top ? 1 : 0;
			Direction face = top ? Direction.UP : Direction.DOWN;
			double[][] p = {{0, y, 0}, {1, y, 0}, {1, y, 1}, {0, y, 1}};
			return quad(p, face, face, true);
		}

		BakedQuad surface(double[][] pts) {
			double[][] p = new double[4][];
			for (int i = 0; i < 4; i++) p[i] = new double[] {pts[i][0], y(pts[i][1]), pts[i][2]};
			Direction face = top ? Direction.DOWN : Direction.UP;
			return quad(p, face, face, true);
		}

		BakedQuad side(Direction d, double[] a, double ha, double[] b, double hb) {
			List<double[]> p = new ArrayList<>();
			p.add(new double[] {a[0], y(0), a[1]});
			p.add(new double[] {b[0], y(0), b[1]});
			if (hb > 0) p.add(new double[] {b[0], y(hb), b[1]});
			if (ha > 0) p.add(new double[] {a[0], y(ha), a[1]});
			while (p.size() < 4) p.add(p.get(p.size() - 1));
			return quad(p.toArray(new double[0][]), d, d, false);
		}

		/** A quad facing out along normal, its corners wound so it shows from outside. */
		private BakedQuad quad(double[][] p, Direction face, Direction normal, boolean flatTexture) {
			if (!facesOut(p, normal)) p = new double[][] {p[0], p[3], p[2], p[1]};
			Vector3f[] pos = new Vector3f[4];
			long[] uv = new long[4];
			for (int i = 0; i < 4; i++) {
				pos[i] = new Vector3f((float) p[i][0], (float) p[i][1], (float) p[i][2]);
				float[] t = flatTexture ? texFlat(p[i], face) : texSide(p[i], face);
				uv[i] = Uv.pack(sprite.getFrameU(t[0]), sprite.getFrameV(t[1]));
			}
			return new BakedQuad(pos[0], pos[1], pos[2], pos[3], uv[0], uv[1], uv[2], uv[3], -1, face, sprite, true, 0);
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

		/** Sides: the texture as on the side of a full block. */
		private static float[] texSide(double[] p, Direction face) {
			float v = (float) (1 - p[1]);
			return switch (face) {
				case NORTH -> new float[] {(float) (1 - p[0]), v};
				case SOUTH -> new float[] {(float) p[0], v};
				case WEST -> new float[] {(float) p[2], v};
				default -> new float[] {(float) (1 - p[2]), v};
			};
		}
	}
}
