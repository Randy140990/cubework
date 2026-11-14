package com.insidethecube.orbitarena.arena;

/**
 * The arena's shape, shared by the builder (server) and the crowd (client), all relative to the arena's
 * centre at floor level. Distances in blocks.
 * <p>
 * From the middle out: a stepped, lit dais; a pale centre floor with a white ring; a dark honeycomb floor
 * with glowing lines (the fighting floor, radius 40); a low wall; a lower deck of eight rows of stands;
 * a concourse with an orange band and floodlights; an upper deck of ten rows; the back wall and an
 * overhanging roof ring edged with light strips. Round all of it, an asteroid of rock.
 */
public final class ArenaLayout {
	private ArenaLayout() {}

	/** Floor level (the floor blocks' y; you stand at FLOOR + 1). */
	public static final int FLOOR = 64;
	/** Where the fighting floor ends (the barrier keeps fighters inside it). */
	public static final double FIGHT_RADIUS = 40.5;
	/** How high fighters can go above the floor. */
	public static final int CEILING = 40;
	public static final double DAIS = 6.5, CENTRE = 15, WALL_IN = 41.5, WALL_OUT = 43;
	public static final int LOWER_ROWS = 8, UPPER_ROWS = 10;
	/** Each row of stands: two blocks deep, one block up. */
	public static final int LOWER_BASE = 5, UPPER_BASE = 17;
	public static final double LOWER_IN = 43, CONCOURSE_IN = 59, CONCOURSE_OUT = 62, UPPER_IN = 62;
	public static final double BACK_WALL = 82, ROOF_IN = 70, ROOF_OUT = 85;
	public static final int ROOF = 33;
	/** The pedestals: you start on the west one, the first opponent on the east one. */
	public static final int PEDESTAL_X = 27;
	/** Arenas for different players sit this far apart along x. */
	public static final int SPACING = 1024;

	/** Angles (degrees) of the four tunnels at floor level and the four boxes in the upper deck. */
	public static final double[] TUNNELS = {90, 270, 0, 180};
	public static final double[] BOXES = {45, 135, 225, 315};

	public static double radius(double x, double z) {
		return Math.sqrt(x * x + z * z);
	}

	/** Angle in degrees, 0 to 360, 0 along +x, increasing towards +z. */
	public static double angle(double x, double z) {
		double a = Math.toDegrees(Math.atan2(z, x));
		return a < 0 ? a + 360 : a;
	}

	public static double angleDiff(double a, double b) {
		double d = Math.abs(a - b) % 360;
		return d > 180 ? 360 - d : d;
	}

	/** Whether this angle falls in an aisle of the stands (steps every 30 degrees). */
	public static boolean aisle(double angle, double r) {
		double arc = Math.toRadians(angleDiff(angle, Math.round(angle / 30.0) * 30.0)) * r;
		return arc < 1.0;
	}

	/** Whether this angle falls in one of the boxes of the upper deck. */
	public static boolean box(double angle) {
		for (double b : BOXES) if (angleDiff(angle, b) < 7) return true;
		return false;
	}

	/** Distance from (x, z) to the nearest line of a honeycomb of flat-topped hexagons size across. */
	public static double hexLine(double x, double z, double size) {
		// Axial coordinates of the nearest hexagon centre, then the distance to its edge.
		double r = size / Math.sqrt(3);
		double q = (2.0 / 3 * x) / r, s = (-1.0 / 3 * x + Math.sqrt(3) / 3 * z) / r;
		double cx = q, cz = s, cy = -q - s;
		double rx = Math.round(cx), rz = Math.round(cz), ry = Math.round(cy);
		double dx = Math.abs(rx - cx), dz = Math.abs(rz - cz), dy = Math.abs(ry - cy);
		if (dx > dz && dx > dy) rx = -rz - ry;
		else if (dz > dy) rz = -rx - ry;
		double hx = r * 3.0 / 2 * rx, hz = r * Math.sqrt(3) * (rz + rx / 2.0);
		double px = Math.abs(x - hx), pz = Math.abs(z - hz);
		// Distance from the centre to the edge of a flat-topped hexagon, in the direction of p.
		double edge = Math.max(pz, (px * Math.sqrt(3) + pz) / 2);
		return Math.abs(size / 2 - edge);
	}

	/** A repeatable random number in [0, 1) for a position and a salt. */
	public static double hash(long x, long y, long z, long salt) {
		long h = x * 0x9E3779B97F4A7C15L ^ y * 0xC2B2AE3D27D4EB4FL ^ z * 0x165667B19E3779F9L ^ salt * 0x27D4EB2F165667C5L;
		h ^= h >>> 31;
		h *= 0x94D049BB133111EBL;
		h ^= h >>> 29;
		return (h >>> 11) / (double) (1L << 53);
	}

	/** Smooth value noise in [0, 1), for rock shapes. */
	public static double noise(double x, double z, double scale, long salt) {
		double fx = x / scale, fz = z / scale;
		long ix = (long) Math.floor(fx), iz = (long) Math.floor(fz);
		double tx = fx - ix, tz = fz - iz;
		tx = tx * tx * (3 - 2 * tx);
		tz = tz * tz * (3 - 2 * tz);
		double a = hash(ix, 0, iz, salt), b = hash(ix + 1, 0, iz, salt), c = hash(ix, 0, iz + 1, salt), d = hash(ix + 1, 0, iz + 1, salt);
		return (a + (b - a) * tx) + ((c + (d - c) * tx) - (a + (b - a) * tx)) * tz;
	}

	/** The rock's outer edge at an angle (it bulges and dips round the arena). */
	public static double rockOuter(double angleDeg) {
		double a = Math.toRadians(angleDeg);
		return 98 + 7 * noise(Math.cos(a) * 40, Math.sin(a) * 40, 9, 11) + 4 * noise(Math.cos(a) * 90, Math.sin(a) * 90, 6, 12);
	}

	/** The rock's top surface (relative to the floor) at (x, z) beyond the back wall: rounded mounds. */
	public static int rockTop(double x, double z) {
		double r = radius(x, z), outer = rockOuter(angle(x, z));
		double edge = Math.max(0, Math.min(1, (outer - r) / 10));
		double mound = 6 * noise(x, z, 14, 21) + 3 * noise(x, z, 6, 22);
		return (int) Math.round(ROOF - 2 + mound * edge - (1 - edge) * 8);
	}

	/** The asteroid's underside (relative to the floor) at radius r: a rounded belly. */
	public static int rockBottom(double x, double z) {
		double r = radius(x, z), outer = rockOuter(angle(x, z));
		double t = Math.min(1, r / outer);
		return (int) Math.round(-34 + 30 * t * t + 4 * noise(x, z, 8, 31));
	}

	/** Where a fan stands: row's top surface + 1, and the row's centre radius. */
	public static int seatY(boolean upper, int row) {
		return (upper ? UPPER_BASE : LOWER_BASE) + row + 1;
	}

	public static double seatRadius(boolean upper, int row) {
		return (upper ? UPPER_IN : LOWER_IN) + 2 * row + 1;
	}
}
