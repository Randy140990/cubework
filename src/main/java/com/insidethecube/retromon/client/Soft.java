package com.insidethecube.retromon.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import com.insidethecube.retromon.mixin.CubeAccessor;
import com.insidethecube.retromon.mixin.ModelPartAccessor;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;

/**
 * Softened corners, as the Person, Interior and UI viewers draw people: each box of a model has its edges and
 * corners rounded a little (0.45 of a pixel for people, 0.3 for the cap and bag). Each face is cut into three by
 * three, the cuts set in from the edges by the rounding, and every point pushed out onto a rounded box; the
 * skin's pixels stay where they were. Done once per box; apply() swaps a model between its softened and its
 * plain boxes (yours softens only in a Retromon save).
 */
public final class Soft {
	private Soft() {}

	public static final float PEOPLE = 0.45F, GEAR = 0.3F;
	/** Each box's plain faces and its softened ones. */
	private static final Map<ModelPart.Cube, ModelPart.Polygon[][]> MADE = new WeakHashMap<>();
	private static final Map<ModelPart, List<ModelPart.Cube>> CUBES = new WeakHashMap<>();

	/** Softens (on) or restores (off) every box of a model (all its parts). */
	public static void apply(ModelPart root, boolean on, float r) {
		List<ModelPart.Cube> cubes = CUBES.computeIfAbsent(root, Soft::cubesOf);
		for (ModelPart.Cube c : cubes) {
			ModelPart.Polygon[][] pair = MADE.get(c);
			if (pair == null) {
				ModelPart.Polygon[] plain = c.polygons, soft;
				try {
					soft = soften(plain, r);
				} catch (RuntimeException e) {
					soft = plain;
				}
				MADE.put(c, pair = new ModelPart.Polygon[][] {plain, soft});
			}
			ModelPart.Polygon[] want = on ? pair[1] : pair[0];
			if (c.polygons != want) ((CubeAccessor) c).retromon$setPolygons(want);
		}
	}

	private static List<ModelPart.Cube> cubesOf(ModelPart root) {
		List<ModelPart.Cube> out = new ArrayList<>();
		for (ModelPart p : root.getAllParts()) out.addAll(((ModelPartAccessor) (Object) p).retromon$cubes());
		return out;
	}

	/** A box's faces, softened by r (pixels). */
	static ModelPart.Polygon[] soften(ModelPart.Polygon[] faces, float r0) {
		if (faces.length == 0) return faces;
		float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
		for (ModelPart.Polygon f : faces)
			for (ModelPart.Vertex v : f.vertices()) {
				minX = Math.min(minX, v.x());
				minY = Math.min(minY, v.y());
				minZ = Math.min(minZ, v.z());
				maxX = Math.max(maxX, v.x());
				maxY = Math.max(maxY, v.y());
				maxZ = Math.max(maxZ, v.z());
			}
		float hw = (maxX - minX) / 2, hh = (maxY - minY) / 2, hd = (maxZ - minZ) / 2;
		float r = Math.min(r0, Math.min(hw, Math.min(hh, hd)) * 0.45F);
		if (r <= 0.01F) return faces;
		float ix0 = minX + r, ix1 = maxX - r, iy0 = minY + r, iy1 = maxY - r, iz0 = minZ + r, iz1 = maxZ - r;
		List<ModelPart.Polygon> out = new ArrayList<>();
		for (ModelPart.Polygon f : faces) {
			ModelPart.Vertex[] q = f.vertices();
			if (q.length != 4) {
				out.add(f);
				continue;
			}
			ModelPart.Vertex c00 = q[0], c10 = q[1], c11 = q[2], c01 = q[3];
			float la = dist(c00, c10), lb = dist(c00, c01);
			float ta = la <= 0 ? 0 : Math.min(0.5F, r / la), tb = lb <= 0 ? 0 : Math.min(0.5F, r / lb);
			float[] A = {0, ta, 1 - ta, 1}, B = {0, tb, 1 - tb, 1};
			ModelPart.Vertex[][] grid = new ModelPart.Vertex[4][4];
			Vector3f[][] norm = new Vector3f[4][4];
			for (int i = 0; i < 4; i++)
				for (int j = 0; j < 4; j++) {
					float a = A[i], b = B[j];
					float x = bil(c00.x(), c10.x(), c11.x(), c01.x(), a, b), y = bil(c00.y(), c10.y(), c11.y(), c01.y(), a, b),
							z = bil(c00.z(), c10.z(), c11.z(), c01.z(), a, b);
					float u = bil(c00.u(), c10.u(), c11.u(), c01.u(), a, b), v = bil(c00.v(), c10.v(), c11.v(), c01.v(), a, b);
					// Onto the rounded box: from the nearest point of the inner box, out by r.
					float cx = clamp(x, ix0, ix1), cy = clamp(y, iy0, iy1), cz = clamp(z, iz0, iz1);
					Vector3f n = new Vector3f(x - cx, y - cy, z - cz);
					if (n.lengthSquared() < 1e-10F) n.set(f.normal());
					else n.normalize();
					norm[i][j] = n;
					grid[i][j] = new ModelPart.Vertex(cx + n.x * r, cy + n.y * r, cz + n.z * r, u, v);
				}
			for (int i = 0; i < 3; i++)
				for (int j = 0; j < 3; j++) {
					if (A[i + 1] - A[i] < 1e-5F || B[j + 1] - B[j] < 1e-5F) continue;
					Vector3f n = new Vector3f(norm[i][j]).add(norm[i + 1][j]).add(norm[i + 1][j + 1]).add(norm[i][j + 1]);
					if (n.lengthSquared() < 1e-10F) n.set(f.normal());
					else n.normalize();
					out.add(new ModelPart.Polygon(new ModelPart.Vertex[] {grid[i][j], grid[i + 1][j], grid[i + 1][j + 1], grid[i][j + 1]}, n));
				}
		}
		return out.toArray(new ModelPart.Polygon[0]);
	}

	private static float dist(ModelPart.Vertex a, ModelPart.Vertex b) {
		float dx = a.x() - b.x(), dy = a.y() - b.y(), dz = a.z() - b.z();
		return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	private static float bil(float p00, float p10, float p11, float p01, float a, float b) {
		return (1 - a) * (1 - b) * p00 + a * (1 - b) * p10 + a * b * p11 + (1 - a) * b * p01;
	}

	private static float clamp(float v, float lo, float hi) {
		return v < lo ? lo : v > hi ? hi : v;
	}
}
