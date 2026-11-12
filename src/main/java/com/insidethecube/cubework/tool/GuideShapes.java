package com.insidethecube.cubework.tool;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * The blocks a builder's guide outlines, worked out from its settings. Outlines are one block thick and
 * closed (no gaps you'd see through diagonally), the way you'd build them.
 */
public final class GuideShapes {
	private GuideShapes() {}

	public static final int CIRCLE = 0, ELLIPSE = 1, SPHERE = 2, DOME = 3, ARCH = 4, CYLINDER = 5, POLYGON = 6, LINE = 7;
	public static final String[] NAMES = {"Circle", "Ellipse", "Sphere", "Dome", "Arch", "Cylinder", "Polygon", "Line"};
	public static final String SHAPE = "shape", SIZE = "r1", SIZE2 = "r2", SIDES = "sides", FACING = "facing", PIN = "pin";
	/** The most a guide outlines, so a huge sphere doesn't stall the game. */
	public static final int MAX_BLOCKS = 40000;

	public static int shape(ItemStack stack) {
		return Math.floorMod(ToolData.getInt(stack, SHAPE, CIRCLE), NAMES.length);
	}

	public static int size(CompoundTag t) {
		return Math.max(1, Math.min(96, t.getIntOr(SIZE, 8)));
	}

	public static int size2(CompoundTag t) {
		return Math.max(1, Math.min(96, t.getIntOr(SIZE2, 5)));
	}

	public static int sides(CompoundTag t) {
		return Math.max(3, Math.min(12, t.getIntOr(SIDES, 6)));
	}

	/** Words for the guide's settings: "Sphere, radius 12". */
	public static String describe(CompoundTag t) {
		int shape = Math.floorMod(t.getIntOr(SHAPE, CIRCLE), NAMES.length);
		String name = NAMES[shape];
		return switch (shape) {
			case ELLIPSE -> name + ", " + size(t) + " by " + size2(t);
			case CYLINDER -> name + ", radius " + size(t) + ", " + size2(t) + " high";
			case POLYGON -> sides(t) + "-sided polygon, radius " + size(t);
			case LINE -> name + (t.getBooleanOr("bset", false) ? ", " + lineLength(t) + " long" : " (sneak and use to set its end)");
			default -> name + ", radius " + size(t);
		};
	}

	private static int lineLength(CompoundTag t) {
		BlockPos a = anchor(t, "a"), b = anchor(t, "b");
		return a == null || b == null ? 0 : (int) Math.round(Math.sqrt(a.distSqr(b))) + 1;
	}

	public static BlockPos anchor(CompoundTag t, String k) {
		if (!t.getBooleanOr(k + "set", false)) return null;
		return new BlockPos(t.getIntOr(k + "x", 0), t.getIntOr(k + "y", 0), t.getIntOr(k + "z", 0));
	}

	/** The outlined blocks (none until the middle is set). */
	public static List<BlockPos> blocks(CompoundTag t) {
		BlockPos c = anchor(t, "a");
		if (c == null) return List.of();
		int shape = Math.floorMod(t.getIntOr(SHAPE, CIRCLE), NAMES.length);
		int r = size(t), r2 = size2(t);
		Set<BlockPos> out = new LinkedHashSet<>();
		switch (shape) {
			case CIRCLE -> ellipse(out, c, r, r, 0);
			case ELLIPSE -> ellipse(out, c, r, r2, 0);
			case CYLINDER -> {
				for (int y = 0; y < r2 && out.size() < MAX_BLOCKS; y++) ellipse(out, c, r, r, y);
			}
			case SPHERE, DOME -> shell(out, c, r, shape == DOME);
			case ARCH -> arch(out, c, r, t.getIntOr(FACING, 0));
			case POLYGON -> polygon(out, c, r, sides(t));
			default -> {
				BlockPos b = anchor(t, "b");
				line(out, c, b == null ? c : b);
			}
		}
		return new ArrayList<>(out);
	}

	/** The outline of a filled ellipse: cells inside whose neighbour (side by side) is outside. */
	private static void ellipse(Set<BlockPos> out, BlockPos c, int a, int b, int dy) {
		for (int x = -a; x <= a; x++) {
			for (int z = -b; z <= b; z++) {
				if (!inEllipse(x, z, a, b)) continue;
				if (!inEllipse(x + 1, z, a, b) || !inEllipse(x - 1, z, a, b) || !inEllipse(x, z + 1, a, b) || !inEllipse(x, z - 1, a, b)) {
					out.add(c.offset(x, dy, z));
				}
			}
		}
	}

	private static boolean inEllipse(int x, int z, int a, int b) {
		double fx = x / (a + 0.5), fz = z / (b + 0.5);
		return fx * fx + fz * fz <= 1;
	}

	private static void shell(Set<BlockPos> out, BlockPos c, int r, boolean dome) {
		for (int x = -r; x <= r; x++) {
			for (int y = dome ? 0 : -r; y <= r; y++) {
				for (int z = -r; z <= r; z++) {
					if (!inSphere(x, y, z, r)) continue;
					boolean edge = !inSphere(x + 1, y, z, r) || !inSphere(x - 1, y, z, r) || !inSphere(x, y + 1, z, r) || !inSphere(x, y - 1, z, r)
							|| !inSphere(x, y, z + 1, r) || !inSphere(x, y, z - 1, r);
					if (edge) out.add(c.offset(x, y, z));
					if (out.size() >= MAX_BLOCKS) return;
				}
			}
		}
	}

	private static boolean inSphere(int x, int y, int z, int r) {
		double f = r + 0.5;
		return x * x + y * y + z * z <= f * f;
	}

	/** A half circle standing up, across the way you faced when you set it. */
	private static void arch(Set<BlockPos> out, BlockPos c, int r, int facing) {
		boolean alongX = facing == 0 || facing == 2; // facing south or north: the arch runs east to west
		for (int u = -r; u <= r; u++) {
			for (int y = 0; y <= r; y++) {
				if (!inEllipse(u, y, r, r)) continue;
				boolean edge = !inEllipse(u + 1, y, r, r) || !inEllipse(u - 1, y, r, r) || !inEllipse(u, y + 1, r, r) || (y > 0 && !inEllipse(u, y - 1, r, r));
				if (edge) out.add(alongX ? c.offset(u, y, 0) : c.offset(0, y, u));
			}
		}
	}

	private static void polygon(Set<BlockPos> out, BlockPos c, int r, int sides) {
		int[][] v = new int[sides][];
		for (int i = 0; i < sides; i++) {
			double a = 2 * Math.PI * i / sides - Math.PI / 2;
			v[i] = new int[] {(int) Math.round(r * Math.cos(a)), (int) Math.round(r * Math.sin(a))};
		}
		for (int i = 0; i < sides; i++) {
			int[] p = v[i], q = v[(i + 1) % sides];
			line(out, c.offset(p[0], 0, p[1]), c.offset(q[0], 0, q[1]));
		}
	}

	/** A line of blocks, each touching the last by a face. */
	private static void line(Set<BlockPos> out, BlockPos a, BlockPos b) {
		int dx = b.getX() - a.getX(), dy = b.getY() - a.getY(), dz = b.getZ() - a.getZ();
		int n = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
		BlockPos last = a;
		out.add(a);
		for (int i = 1; i <= n && out.size() < MAX_BLOCKS; i++) {
			double t = (double) i / n;
			BlockPos p = new BlockPos((int) Math.round(a.getX() + dx * t), (int) Math.round(a.getY() + dy * t), (int) Math.round(a.getZ() + dz * t));
			// Fill diagonal moves so the line is closed.
			if (p.getX() != last.getX() && (p.getY() != last.getY() || p.getZ() != last.getZ())) out.add(new BlockPos(p.getX(), last.getY(), last.getZ()));
			if (p.getY() != last.getY() && p.getZ() != last.getZ()) out.add(new BlockPos(p.getX(), p.getY(), last.getZ()));
			out.add(p);
			last = p;
		}
	}
}
