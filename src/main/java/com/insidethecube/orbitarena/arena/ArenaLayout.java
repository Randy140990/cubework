package com.insidethecube.orbitarena.arena;

/**
 * The arena's shape, shared by the builder (server) and the crowd (client), all relative to the arena's
 * centre at floor level. Distances in blocks.
 * <p>
 * From the middle out: a raised, stepped dais topped with a hexagon of light, ringed by a band of light
 * edged with stone; a pale centre disc; a dark honeycomb floor with thin glowing lines and a pale rim (the
 * fighting floor, radius 48); the two pedestals (3x3 on 5x5 on a black 7x7), each in the middle of a
 * hexagon; a low wall; three tiers of stands, each
 * eight rows, with a pale band between them carrying an orange stripe and lights; the back wall and an
 * overhanging roof ring edged with light. Round all of it, an asteroid of rock.
 */
public final class ArenaLayout {
	private ArenaLayout() {}

	/** Floor level (the floor blocks' y; you stand at FLOOR + 1). */
	public static final int FLOOR = 64;
	/** Where the fighting floor ends (the barrier keeps fighters inside it). */
	public static final double FIGHT_RADIUS = 48.5;
	/** How high fighters can go above the floor. */
	public static final int CEILING = 42;
	/** The raised dais, the ring of light round it, and the pale centre disc. */
	public static final double DAIS = 9.2, LIGHT_RING_IN = 9.6, LIGHT_RING_OUT = 11.2, CENTRE = 18;
	/** The pale rim round the floor, and the wall. */
	public static final double RIM = 46.4, WALL_IN = 49.5, WALL_OUT = 51;
	/**
	 * The honeycomb: hexagons this far across (flat to flat), lines one block wide. Sized so the pedestals
	 * (28 out) sit in the middle of a hexagon.
	 */
	public static final double HEX = 28 / Math.sqrt(3);

	/** Three tiers of stands: where each starts (radius), its first row's top (above the floor), and its rows. */
	public static final double[] TIER_IN = {51, 70, 89};
	public static final int[] TIER_BASE = {5, 19, 33};
	public static final int TIER_ROWS = 8;
	/** Each row: two blocks deep, one block up. A tier ends at TIER_IN + 2 * TIER_ROWS. */
	public static final double BAND = 3;
	public static final double BACK_WALL = 105, ROOF_IN = 96, ROOF_OUT = 109;
	public static final int ROOF = 46;
	/** The pedestals: you start on the west one, the first opponent on the east one (on hexagon centres). */
	public static final int PEDESTAL_X = 28;
	/** You stand on the pedestal's top (its 3x3 is two blocks up). */
	public static final int PEDESTAL_TOP = 3;
	/** Arenas for different players sit this far apart along x. */
	public static final int SPACING = 1024;
	/** How far out the builder works (the rock's widest). */
	public static final int SIZE = 138;

	/** Angles (degrees) of the four tunnels at floor level and the four boxes in the middle tier. */
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

	/** Which tier a radius is in (0, 1, 2), or -1 outside the stands' rows. */
	public static int tierAt(double r) {
		for (int t = 0; t < TIER_IN.length; t++) if (r >= TIER_IN[t] && r < TIER_IN[t] + 2 * TIER_ROWS) return t;
		return -1;
	}

	/** Whether this angle falls in an aisle of the stands (steps every 30 degrees, offset 15 so the boxes and tunnels sit between). */
	public static boolean aisle(double angle, double r) {
		double arc = Math.toRadians(angleDiff(angle, Math.round((angle - 15) / 30.0) * 30.0 + 15)) * r;
		return arc < 1.0;
	}

	/** Whether this angle falls in one of the boxes of the middle tier. */
	public static boolean box(double angle) {
		for (double b : BOXES) if (angleDiff(angle, b) < 6) return true;
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

	/**
	 * Whether the floor block at (x, z) (blocks from the centre, the centre block being 0, 0) is on a line of
	 * the honeycomb. Each edge is drawn as a clean line one block wide, as a pixel line is drawn (one block
	 * across the line's narrow way at each step along it), so the hexagons keep crisp straight sides and
	 * sharp corners instead of blobs where three lines meet. Mirror images match, left and right.
	 */
	public static boolean hexLineBlock(int x, int z) {
		double s = HEX, rad = s / Math.sqrt(3);
		// The hexagon this block is in (as hexLine).
		double q = (2.0 / 3 * x) / rad, t = (-1.0 / 3 * x + Math.sqrt(3) / 3 * z) / rad;
		double cx = q, cz = t, cy = -q - t;
		double rx = Math.round(cx), rz = Math.round(cz), ry = Math.round(cy);
		double dx = Math.abs(rx - cx), dz = Math.abs(rz - cz), dy = Math.abs(ry - cy);
		if (dx > dz && dx > dy) rx = -rz - ry;
		else if (dz > dy) rz = -rx - ry;
		double px = x - rad * 1.5 * rx, pz = z - s * (rz + rx / 2.0);
		double[][] v = {{rad, 0}, {rad / 2, s / 2}, {-rad / 2, s / 2}, {-rad, 0}, {-rad / 2, -s / 2}, {rad / 2, -s / 2}};
		for (int i = 0; i < 6; i++) {
			double ax = v[i][0], az = v[i][1], ex = v[(i + 1) % 6][0] - ax, ez = v[(i + 1) % 6][1] - az;
			double len = Math.sqrt(ex * ex + ez * ez), ux = ex / len, uz = ez / len;
			double along = (px - ax) * ux + (pz - az) * uz;
			if (along < -0.35 || along > len + 0.35) continue;
			double off = Math.abs(ux * (pz - az) - uz * (px - ax));
			if (off / Math.max(Math.abs(ux), Math.abs(uz)) <= 0.5) return true;
		}
		return false;
	}

	/** How far (x, z) is from the centre of a flat-topped hexagon, in its own measure (1 at the flats). */
	public static double hexRadius(double x, double z, double across) {
		double px = Math.abs(x), pz = Math.abs(z);
		return Math.max(pz, (px * Math.sqrt(3) + pz) / 2) / (across / 2);
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
		return 121 + 9 * noise(Math.cos(a) * 40, Math.sin(a) * 40, 9, 11) + 5 * noise(Math.cos(a) * 90, Math.sin(a) * 90, 6, 12);
	}

	/** The rock's top surface (relative to the floor) at (x, z) beyond the back wall: rounded mounds. */
	public static int rockTop(double x, double z) {
		double r = radius(x, z), outer = rockOuter(angle(x, z));
		double edge = Math.max(0, Math.min(1, (outer - r) / 10));
		double mound = 7 * noise(x, z, 14, 21) + 3 * noise(x, z, 6, 22);
		return (int) Math.round(ROOF - 3 + mound * edge - (1 - edge) * 10);
	}

	/** The asteroid's underside (relative to the floor) at radius r: a rounded belly. */
	public static int rockBottom(double x, double z) {
		double r = radius(x, z), outer = rockOuter(angle(x, z));
		double t = Math.min(1, r / outer);
		return (int) Math.round(-40 + 36 * t * t + 4 * noise(x, z, 8, 31));
	}

	/** Where a fan stands in a tier's row: the row's top surface + 1 (above the floor), and the row's centre radius. */
	public static int seatY(int tier, int row) {
		return TIER_BASE[tier] + row + 1;
	}

	public static double seatRadius(int tier, int row) {
		return TIER_IN[tier] + 2 * row + 1;
	}
}
