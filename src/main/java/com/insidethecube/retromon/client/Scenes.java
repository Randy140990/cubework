package com.insidethecube.retromon.client;

import com.insidethecube.retromon.Retromon;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;

/**
 * The UI Viewer's 3D scenes, drawn in game the same way: the title (its picture moving, frame by frame), the
 * professor with our creatures about him, and you on a stand. Positions are the viewer's (blocks, its camera at
 * its angle with its 34 degree lens), put through the same perspective onto the 1280 by 720 canvas, and the people
 * and creatures drawn there by Figures, sized by how far off they are.
 */
public final class Scenes {
	private Scenes() {}

	// ---------- the camera ----------

	/** A camera as the viewer's: where it is, what it looks at, 34 degrees top to bottom, onto the canvas. */
	public static final class Cam {
		double px, py, pz;
		final double[] f = new double[3], r = new double[3], u = new double[3];
		double focal = V.H / 2 / Math.tan(Math.toRadians(17)), ref = 6;

		public Cam look(double px, double py, double pz, double tx, double ty, double tz) {
			this.px = px;
			this.py = py;
			this.pz = pz;
			double fx = tx - px, fy = ty - py, fz = tz - pz, fl = Math.sqrt(fx * fx + fy * fy + fz * fz);
			ref = fl;
			f[0] = fx / fl;
			f[1] = fy / fl;
			f[2] = fz / fl;
			// Right: forward across world up; up: right across forward.
			double rx = -f[2], rz = f[0], rl = Math.sqrt(rx * rx + rz * rz);
			r[0] = rx / rl;
			r[1] = 0;
			r[2] = rz / rl;
			u[0] = r[1] * f[2] - r[2] * f[1];
			u[1] = r[2] * f[0] - r[0] * f[2];
			u[2] = r[0] * f[1] - r[1] * f[0];
			return this;
		}

		/** A world point on the canvas: {x, y, depth}. */
		public float[] at(double x, double y, double z) {
			double dx = x - px, dy = y - py, dz = z - pz;
			double cz = dx * f[0] + dy * f[1] + dz * f[2], cx = dx * r[0] + dy * r[1] + dz * r[2], cy = dx * u[0] + dy * u[1] + dz * u[2];
			cz = Math.max(0.05, cz);
			return new float[] {(float) (V.W / 2 + cx / cz * focal), (float) (V.H / 2 - cy / cz * focal), (float) cz};
		}

		/** How far the camera is from what it looks at (the middle of the scene's depth). */
		public float refDepth() {
			return (float) ref;
		}

		/** Canvas pixels to a block at a depth. */
		public float perBlock(float depth) {
			return (float) (focal / depth);
		}

		/** How far the camera looks down (degrees). */
		public float pitch() {
			return (float) Math.toDegrees(Math.asin(-f[1]));
		}

		/** Which way something at (x, z) sees the camera from (degrees, round from facing +z towards +x). */
		public float towards(double x, double z) {
			return (float) Math.toDegrees(Math.atan2(px - x, pz - z));
		}
	}

	/**
	 * Something stood in a scene: a person (skin) or a creature (species) at (x, y, z), turned yaw (the viewer's
	 * rotation.y: pi faces the camera).
	 */
	public static Figures.Fig place(Figures.Batch b, Cam cam, Identifier skin, String species, double x, double y, double z, double yaw, float secs) {
		float[] p = cam.at(x, y, z);
		float ppb = cam.perBlock(p[2]) * V.k;
		float facing = (float) Math.toDegrees(yaw - Math.PI) - cam.towards(x, z);
		float sx = V.x(p[0]) + V.bx, sy = V.y(p[1]) + V.by;
		Figures.Fig f = skin != null ? b.person(skin, sx, sy, 1.8F * ppb) : b.mon(species, sx, sy, 1).perBlock(ppb);
		// How near it is, in the same pixels as its size (a block nearer is a block's width nearer), so the figures
		// pass in front of and behind each other as they really stand (the viewer's 3D), not just a little apart.
		float ref = cam.refDepth(), near = (ref - p[2]) * cam.perBlock(ref) * V.k;
		return f.facing(wrap(facing)).pitch(cam.pitch()).depth(near).at(secs);
	}

	private static float wrap(float d) {
		d %= 360;
		if (d > 180) d -= 360;
		if (d < -180) d += 360;
		return d;
	}

	/** A soft round shadow on the ground under something (radius in blocks). */
	public static void blob(GuiGraphics g, Cam cam, double x, double z, double radius, int alpha) {
		float[] c = cam.at(x, 0.01, z), e = cam.at(x + radius, 0.01, z), n = cam.at(x, 0.01, z - radius);
		float rx = Math.abs(e[0] - c[0]), ry = Math.max(2, Math.abs(n[1] - c[1]));
		for (int i = 3; i >= 1; i--) V.ellipse(g, c[0], c[1], rx * (0.6F + i * 0.15F), ry * (0.6F + i * 0.15F), Ui.alpha(0xFF0E2A28, alpha / 255F * 0.28F));
	}

	/** A flat disc on the ground (the stage under the professor, a stand's top), radius in blocks. */
	public static void disc(GuiGraphics g, Cam cam, double x, double y, double z, double radius, int colour) {
		float[] c = cam.at(x, y, z), e = cam.at(x + radius, y, z), n = cam.at(x, y, z - radius), s = cam.at(x, y, z + radius);
		float rx = Math.abs(e[0] - c[0]), ry = Math.abs(s[1] - n[1]) / 2, cy = (s[1] + n[1]) / 2;
		V.ellipse(g, c[0], cy, rx, ry, colour);
	}

	// ---------- the title ----------

	private static final Identifier TITLE_OURS = V.tex("title_ours"), TITLE_THEIRS = V.tex("title_theirs");

	/**
	 * The title's picture: the sky (day: the blocky hill, its trees and clouds and our creatures; night: the stars,
	 * the moon and Charizard), moving at 12 frames a second, pixel art as the viewer draws it.
	 */
	public static void title(GuiGraphics g, float secs, boolean ours) {
		// The moving picture covers the whole window (wider or taller than the canvas, it's cut at the top or the
		// sides, never left with bare strips), its ground kept at the bottom.
		float wx = -V.ox / V.k, wy = -V.oy / V.k, ww = V.gw / V.k, wh = V.gh / V.k;
		float sc = Math.max(ww / V.W, wh / V.H), w = V.W * sc, h = V.H * sc;
		V.img(g, V.tex(ours ? "bg_title_day" : "bg_title_night"), wx, wy, ww, wh, 480, 270, 0xFFFFFFFF);
		int frame = (int) (secs * 12) % 48;
		V.sub(g, ours ? TITLE_OURS : TITLE_THEIRS, wx + (ww - w) / 2, wy + wh - h, w, h, (frame % 8) * 256, (frame / 8) * 144, 256, 144, 2048, 864);
	}

	/** A backdrop picture (textures/gui/v/bg_*.png) filling the whole window. */
	public static void backdrop(GuiGraphics g, String name) {
		V.img(g, V.tex("bg_" + name), -V.ox / V.k, -V.oy / V.k, V.gw / V.k, V.gh / V.k, 480, 270, 0xFFFFFFFF);
	}

	/**
	 * A page's backdrop in a creature's own colour (its first type's, as Let's Go's pages are): the colour, lit from
	 * the top left and shaded to the bottom right, as the UI Viewer's typeBackdrop.
	 */
	public static void typeBackdrop(GuiGraphics g, com.insidethecube.retromon.engine.Type t) {
		g.fill(0, 0, g.guiWidth(), g.guiHeight(), Ui.typeColor(t) | 0xFF000000);
		V.img(g, V.tex("bg_sheen"), -V.ox / V.k, -V.oy / V.k, V.gw / V.k, V.gh / V.k, 480, 270, 0xFFFFFFFF);
	}

	/** A band in a creature's colour across the top of its page, for its name: the colour lit from the top. */
	public static void typeBand(GuiGraphics g, com.insidethecube.retromon.engine.Type t, float x, float y, float w, float h) {
		int c = Ui.typeColor(t) | 0xFF000000;
		V.rr(g, x, y + 4, w, h, 22, 0x33000000);
		V.rr(g, x - 2, y - 2, w + 4, h + 4, 24, 0x66FFFFFF);
		V.rrStops(g, x, y, w, h, 22, new float[] {0, 0.55F, 1}, new int[] {Ui.lighter(c, 0.12F), Ui.darker(c, 0.86F), Ui.darker(c, 0.68F)});
	}

	// ---------- the professor ----------

	private static final Identifier OAK = Retromon.id("textures/entity/npc/oak.png"), RIVAL = Retromon.id("textures/entity/npc/blue.png");

	/** The creatures about him: species, where (x, z), their turn, how high they fly (0 on the ground). */
	private static final Object[][] SPOTS = {{"pikachu", -0.85, 0.95, 0.35, 0.0}, {"cubone", -1.8, 0.35, 0.55, 0.0}, {"oddish", 0.55, 1.35, -0.1, 0.0},
			{"clefairy", -1.3, 1.55, 0.2, 0.0}, {"pidgey", -1.45, -0.55, 0.4, 1.55}};

	/**
	 * The professor's scene, as the viewer's: him on a glowing floor, our creatures about him (one wandering in a
	 * loop, the bird hovering, now and then one calling out), and beside him your rival (mode "rival") or you ("you").
	 * hopSecs: seconds since the little yellow one began its jump up onto his shoulder (negative: not yet).
	 */
	public static void prof(GuiGraphics g, float secs, String mode, boolean talking, float wave, float hopSecs, Identifier you) {
		backdrop(g, "dream_sparks");
		double cx = mode.equals("intro") ? -0.3 : 0.2;
		Cam cam = new Cam().look(cx, 2.35, 6.6, cx, 0.62, 0.3);
		disc(g, cam, 0, 0, 0, 4.2, 0x8CDFF6F4);
		disc(g, cam, 0, 0.005, 0, 2.0, 0x73FFFFFF);
		Figures.Batch b = Figures.batch(V.bx, V.by, V.bx + V.gw, V.by + V.gh);
		blob(g, cam, 0, 0, 0.45, 200);
		Figures.Fig oak = place(b, cam, OAK, null, 0, 0, 0, Math.PI, secs);
		if (talking) oak.talk(0.85F);
		else if (wave > 0) oak.wave(wave);
		if (mode.equals("rival")) {
			blob(g, cam, 1.45, 0.45, 0.45, 200);
			place(b, cam, RIVAL, null, 1.45, 0, 0.45, Math.PI - 0.25, secs).look((float) Math.sin(secs * 0.7) * 12);
		} else if (mode.equals("you") && you != null) {
			blob(g, cam, 1.45, 0.45, 0.45, 200);
			Figures.Fig y = place(b, cam, you, null, 1.45, 0, 0.45, Math.PI - 0.25, secs);
			if (wave > 0) y.wave(wave);
		}
		// Now and then one of them calls out.
		int caller = (int) Math.floor(secs / 3.7) % SPOTS.length;
		float callAt = (float) (secs - Math.floor(secs / 3.7) * 3.7);
		for (int i = 0; i < SPOTS.length; i++) {
			String sid = (String) SPOTS[i][0];
			double x = (double) SPOTS[i][1], z = (double) SPOTS[i][2], turn = (double) SPOTS[i][3], fly = (double) SPOTS[i][4];
			double px = x, py = 0, pz = z, yaw = Math.PI + turn + Math.sin(secs * 0.4 + x) * 0.15;
			String pose = null;
			if (sid.equals("cubone")) {
				// Round the professor in a wide loop, clear of the others, facing the way it walks (it walked backwards
				// before: its turn was half a turn out).
				double a = secs * 0.35;
				px = Math.cos(a) * 2.55;
				pz = 0.35 + Math.sin(a) * 2.25;
				yaw = Math.atan2(Math.sin(a) * 2.55, -Math.cos(a) * 2.25);
				pose = "walk";
			} else if (sid.equals("pikachu") && hopSecs >= 0) {
				// Up onto his shoulder in a little arc, then riding there.
				double k = Math.min(1, hopSecs / 0.75), e = k * k * (3 - 2 * k);
				px = x + (0.3 - x) * e;
				pz = z + (0.02 - z) * e;
				py = 1.4 * e + Math.sin(Math.PI * k) * 0.9;
				yaw = Math.PI + turn * (1 - e) + Math.sin(secs * 0.5) * 0.1 * e;
				if (k < 1) pose = "walk";
			} else if (fly > 0) {
				py = fly + Math.sin(secs * 2.2) * 0.12;
				pose = "fly";
			}
			if (py < 0.3) blob(g, cam, px, pz, 0.32, 180);
			Figures.Fig f = place(b, cam, null, sid, px, py, pz, yaw, secs + i * 0.7F).pose(pose);
			if (i == caller && callAt < 1.2F) f.action("cry", callAt);
			if (sid.equals("pikachu") && hopSecs >= 0 && hopSecs < 1.5F) f.action("cry", hopSecs);
		}
		b.draw(g);
	}

	/** A signed-off screen's backdrop (textures/gui/v/bg2_<id>.png, baked from the UI Viewer), covering the whole window. */
	public static void bg2(GuiGraphics g, String id) {
		cover(g, V.tex("bg2_" + id), 640, 360);
	}

	/** A creature's own backdrop (its first type's swirl, embers, bubbles...): mid true for the middle-centred one. */
	public static void typeBg(GuiGraphics g, com.insidethecube.retromon.engine.Type t, boolean mid) {
		String n = t == null ? "normal" : t.name().toLowerCase(java.util.Locale.ROOT);
		if (n.equals("curse")) n = "ghost";
		cover(g, V.tex("bgt_" + n + (mid ? "_mid" : "")), 640, 360);
	}

	/** A 16:9 picture over the whole window, cut at the sides or the top and bottom rather than stretched. */
	public static void cover(GuiGraphics g, Identifier id, int tw, int th) {
		float wx = -V.ox / V.k, wy = -V.oy / V.k, ww = V.gw / V.k, wh = V.gh / V.k;
		float sc = Math.max(ww / V.W, wh / V.H), w = V.W * sc, h = V.H * sc;
		V.img(g, id, wx + (ww - w) / 2, wy + (wh - h) / 2, w, h, tw, th, 0xFFFFFFFF);
	}

	/**
	 * The professor's opening, as the viewer's (signed off 20 Nov): him on a long pale floor of light with our
	 * creatures about him; then, as Let's Go's, he sets off to the right as he talks, the camera going with him past a
	 * whole parade of POKéMON (big ones at the back, little ones near), each turning to watch him go; the little
	 * yellow one trots at his side, the bone-helmet one follows behind and the bird flies over his shoulder. At the far
	 * end he stops, turns to you, and the camera comes in close. Mode intro, rival (his grandson beside him) or you.
	 */
	public static final class ProfWalk {
		private static final Object[][] SPOTS = {{"pikachu", -0.85, 0.95, 0.35, 0.0}, {"cubone", -1.8, 0.35, 0.55, 0.0}, {"oddish", 0.55, 1.35, -0.1, 0.0},
				{"clefairy", -1.3, 1.55, 0.2, 0.0}, {"pidgey", -1.45, -0.55, 0.4, 1.55}};
		private static final String[] PARADE = {"growlithe", "exeggutor", "psyduck", "ponyta", "jigglypuff", "onix", "meowth", "machoke", "vulpix", "snorlax", "butterfree",
				"nidoking", "eevee", "lapras", "pidgeot"};
		public String mode = "intro";
		public boolean talking, walking, hurry, done;
		public float walkX, turnT, wave, youWave, floss, lookUp;
		private float camX = Float.NaN, t;
		private final double[][] walkers = new double[5][];
		private float nextCry = 2, cryAt = -9;
		private int crier = -1;
		private final java.util.Random rnd = new java.util.Random();
		private final float end = 3.8F + (PARADE.length - 1) * 1.15F + 1.4F;

		public void startWalk() {
			walking = true;
		}

		public void stopWalk() {
			walking = true;
			hurry = true;
		}

		public boolean arrived() {
			return done && turnT >= 1;
		}

		/** Moves it all on by dt seconds. */
		public void tick(float dt) {
			t += dt;
			if (walking && !done) {
				walkX = Math.min(end, walkX + dt * (hurry ? 3.2F : 1.25F));
				if (walkX >= end) {
					done = true;
					turnT = 0;
				}
			}
			if (done) turnT = Math.min(1, turnT + dt * 2.2F);
			if (walking) camX = Float.isNaN(camX) ? walkX : camX + (walkX + 0.4F - camX) * Math.min(1, dt * 2.5F);
			nextCry -= dt;
			if (nextCry < 0) {
				crier = rnd.nextInt(SPOTS.length + (mode.equals("intro") ? PARADE.length : 0));
				cryAt = t;
				nextCry = 2 + rnd.nextFloat() * 2;
			}
		}

		public void draw(GuiGraphics g, Identifier you) {
			bg2(g, "prof");
			Cam cam;
			if (mode.equals("you")) cam = new Cam().look(0.75, 1.45, 5.6, 0.75, 1.05, 0.3);
			else if (walking && !Float.isNaN(camX)) {
				float close = done ? turnT * turnT * (3 - 2 * turnT) : 0;
				cam = new Cam().look(camX - 0.2 * close, 2.35 - 0.55 * close, 6.6 - 2.1 * close, camX, 0.62 + 0.25 * close, 0.3);
			} else {
				double x = mode.equals("intro") ? -0.3 : 0.2;
				cam = new Cam().look(x, 2.35, 6.6, x, 0.62, 0.3);
			}
			// The long floor of light (40 by 9 blocks, its middle at x 8), and the bright disc where he starts.
			for (int i = 0; i < 24; i++) {
				double z0 = -4.3 + i * 9.0 / 24, z1 = z0 + 9.0 / 24;
				float[] a = cam.at(-12, 0, z1), b = cam.at(28, 0, z1), c = cam.at(-12, 0, z0);
				V.rr(g, a[0], c[1], b[0] - a[0], Math.max(1, a[1] - c[1]) + 0.5F, 0, 0x80DFF6F4);
			}
			disc(g, cam, 0, 0.005, 0, 2.0, 0x73FFFFFF);
			Figures.Batch b = Figures.batch(V.bx, V.by, V.bx + V.gw, V.by + V.gh);
			float px = walking ? walkX : 0;
			double yaw = Math.PI;
			if (walking && !done) yaw = -Math.PI / 2 - 0.55;
			else if (done) {
				float e = turnT * turnT * (3 - 2 * turnT);
				yaw = (-Math.PI / 2 - 0.55) + (-Math.PI - (-Math.PI / 2 - 0.55)) * e;
			}
			blob(g, cam, px, 0, 0.45, 200);
			Figures.Fig oak = place(b, cam, OAK, null, px, 0, 0, yaw, t);
			if (walking && !done) oak.walk(1);
			if (talking) oak.talk(0.85F);
			if (wave > 0) oak.wave(wave);
			if (mode.equals("rival")) {
				blob(g, cam, 1.45, 0.45, 0.45, 200);
				place(b, cam, RIVAL, null, 1.45, 0, 0.45, Math.PI - 0.25, t).look((float) Math.toDegrees(Math.sin(t * 0.7) * 0.2));
			} else if (mode.equals("you") && you != null) {
				blob(g, cam, 1.25, 0.6, 0.45, 200);
				Figures.Fig y = place(b, cam, you, null, 1.25, 0, 0.6, Math.PI + 0.12, t).floss(floss).lookUp(lookUp);
				if (youWave > 0) y.wave(youWave);
			}
			for (int i = 0; i < SPOTS.length; i++) {
				String sid = (String) SPOTS[i][0];
				double x = (double) SPOTS[i][1], z = (double) SPOTS[i][2], turn = (double) SPOTS[i][3], fly = (double) SPOTS[i][4];
				double mx = x, my = fly > 0 ? fly + Math.sin(t * 2.2) * 0.12 : 0, mz = z, myaw = Math.PI + turn + Math.sin(t * 0.4 + x) * 0.15;
				String pose = fly > 0 ? "fly" : null;
				int with = sid.equals("pikachu") ? 0 : sid.equals("cubone") ? 1 : sid.equals("pidgey") ? 2 : -1;
				if (walking && with >= 0) {
					double[][] off = {{0.85, 0.75, 0}, {-1.0, -0.45, 0}, {-0.4, -0.6, 1.7}};
					double tx = px + off[with][0], tz = off[with][1];
					if (walkers[i] == null) walkers[i] = new double[] {x, z};
					walkers[i][0] += (tx - walkers[i][0]) * Math.min(1, 0.05 * 3);
					walkers[i][1] += (tz - walkers[i][1]) * Math.min(1, 0.05 * 3);
					boolean moving = !done || Math.abs(tx - walkers[i][0]) > 0.05;
					mx = walkers[i][0];
					mz = walkers[i][1];
					myaw = moving ? -Math.PI / 2 - 0.25 : Math.PI + (sid.equals("pikachu") ? -0.4 : 0.3);
					my = off[with][2] > 0 ? off[with][2] + Math.sin(t * 2.4) * 0.12 : 0;
					pose = off[with][2] > 0 ? "fly" : moving ? "walk" : null;
				} else if (sid.equals("cubone")) {
					double a = t * 0.35;
					mx = Math.cos(a) * 2.55;
					mz = 0.35 + Math.sin(a) * 2.25;
					myaw = Math.atan2(Math.sin(a) * 2.55, -Math.cos(a) * 2.25);
					pose = "walk";
				}
				if (my < 0.3) blob(g, cam, mx, mz, 0.32, 180);
				Figures.Fig f = place(b, cam, null, sid, mx, my, mz, myaw, t + i * 0.7F).pose(pose);
				if (i == crier && t - cryAt < 1.2F && !(walking && sid.equals("pikachu"))) f.action("cry", t - cryAt);
			}
			if (mode.equals("intro")) {
				for (int i = 0; i < PARADE.length; i++) {
					String sid = PARADE[i];
					BedrockModel.Def def = BedrockModel.def(sid);
					if (def == null) continue;
					double h = Math.max(0.3, def.height), big = h > 2.2 ? 2.2 / h : 1, x = 3.8 + i * 1.15, row = new double[] {1.4, -1.35, -2.4}[i % 3];
					boolean flyer = sid.equals("butterfree") || sid.equals("pidgeot");
					double hh = h * big, z = flyer ? -0.9 : hh > 1.3 ? -2.4 : hh > 0.9 && row > 0 ? -1.35 : row, fly = flyer ? 1.5 + Math.sin(t * 2.1 + x) * 0.14 : 0;
					double look = walking ? Math.max(-0.6, Math.min(0.6, (px - x) * 0.4)) : 0;
					if (!flyer) blob(g, cam, x, z, 0.32 * Math.max(1, hh), 160);
					Figures.Fig f = place(b, cam, null, sid, x, fly, z, Math.PI + (i % 3 - 1) * 0.35 - look, t + i * 0.37F).pose(flyer ? "fly" : null);
					if (big < 1) f.perBlock(f.perBlock * (float) big);
					if (SPOTS.length + i == crier && t - cryAt < 1.2F) f.action("cry", t - cryAt);
				}
			}
			b.draw(g);
		}
	}

	// ---------- you on a stand ----------

	/**
	 * You on a little round stand, turning gently (the maker, clothes, the trainer card): the viewer's camera at
	 * side (so you're off to one side), the stand's top a pale disc.
	 */
	public static void stand(GuiGraphics g, float secs, Identifier you, double side, float wave, boolean turning) {
		Cam cam = new Cam().look(side, 1.35, 6.3, side, 0.92, 0);
		// The stand: its side a band, its top a disc.
		float[] c = cam.at(0, 0, 0), e = cam.at(1.0, 0, 0), lo = cam.at(0, -0.18, 0), n = cam.at(0, 0, -1.0), s = cam.at(0, 0, 1.0);
		float rx = Math.abs(e[0] - c[0]), ry = Math.abs(s[1] - n[1]) / 2, cy = (s[1] + n[1]) / 2, drop = lo[1] - c[1];
		V.ellipse(g, c[0], cy + drop + 6, rx * 1.02F, ry * 1.05F, 0x30000000);
		V.ellipse(g, c[0], cy + drop, rx, ry, 0xFF9FD6C4);
		V.rr(g, c[0] - rx, cy, rx * 2, drop, 0, 0xFFA9DCCB);
		V.ellipse(g, c[0], cy, rx * 0.95F, ry * 0.95F, 0xFFBFEFE0);
		V.ellipse(g, c[0], cy, rx * 0.85F, ry * 0.85F, 0xFFE9FFF6);
		Figures.Batch b = Figures.batch(V.bx, V.by, V.bx + V.gw, V.by + V.gh);
		double turn = turning ? Math.sin(secs * 0.55) * 0.9 : 0;
		Figures.Fig f = place(b, cam, you, null, 0, 0, 0, Math.PI + turn, secs);
		if (wave > 0) f.wave(wave);
		b.draw(g);
	}

	/**
	 * One creature on a round stand, turning gently (the summary, the Pokédex): the viewer's camera at side so it
	 * stands off to one side, framed by its size.
	 */
	public static void model(GuiGraphics g, float secs, String species, double side) {
		model(g, secs, species, side, secs, 0, false);
	}

	/**
	 * The same, with the viewer's evolving touches: turnSecs sets how far round it has turned (held still, it stops
	 * turning); glow (0 to 1) brings up the white glow round it, pulsing; hide leaves just the stand.
	 */
	public static void model(GuiGraphics g, float secs, String species, double side, float turnSecs, float glow, boolean hide) {
		BedrockModel.Def def = BedrockModel.def(species);
		double h = Math.max(0.6, def == null ? 1 : def.height), l = Math.max(0.6, h), d = Math.max(Math.max(h * 3.1, l * 2.3), 3.8);
		Cam cam = new Cam().look(side, h * 0.62 + d * 0.16, d, side, h * 0.5, 0);
		double sc = Math.max(0.5, Math.min(3, l * 0.55));
		float[] c = cam.at(0, 0, 0), e = cam.at(1.4 * sc, 0, 0), lo = cam.at(0, -0.16, 0), n = cam.at(0, 0, -1.4 * sc), s = cam.at(0, 0, 1.4 * sc);
		float rx = Math.abs(e[0] - c[0]), ry = Math.abs(s[1] - n[1]) / 2, cy = (s[1] + n[1]) / 2, drop = lo[1] - c[1];
		V.ellipse(g, c[0], cy + drop + 6, rx * 1.1F, ry * 1.1F, 0x30000000);
		V.ellipse(g, c[0], cy + drop, rx * 1.1F, ry * 1.1F, 0xFF9FD6C4);
		V.rr(g, c[0] - rx * 1.1F, cy, rx * 2.2F, drop, 0, 0xFFA9DCCB);
		V.ellipse(g, c[0], cy, rx * 1.1F, ry * 1.1F, 0xFFBFEFE0);
		V.ellipse(g, c[0], cy, rx * 0.93F, ry * 0.93F, 0xFFE9FFF6);
		if (!hide) {
			Figures.Batch b = Figures.batch(V.bx, V.by, V.bx + V.gw, V.by + V.gh);
			place(b, cam, null, species, 0, 0, 0, Math.PI + Math.sin(turnSecs * 0.45) * 0.7, secs);
			b.draw(g);
		}
		if (glow > 0) {
			// The viewer's white glow: a ball of light round it (0.7 of its length across, 0.75 of its height up and
			// down from its middle), pulsing quickly.
			float[] mid = cam.at(0, h / 2, 0), up = cam.at(0, h / 2 + h * 0.75, 0), side2 = cam.at(l * 0.7, h / 2, 0);
			float grx = Math.abs(side2[0] - mid[0]), gry = Math.abs(mid[1] - up[1]);
			float a = Math.max(0, Math.min(1, glow * (0.65F + 0.3F * (float) Math.sin(secs * 12))));
			V.ellipse(g, mid[0], mid[1], grx, gry, Ui.alpha(0xFFFFFFFF, a));
		}
	}

	/**
	 * Evolving (the viewer's morph): the old form and the new on one stand, framed for the old one, each turned to
	 * white light (white 0 to 1) and grown or shrunk (ka, kb: 0 gone, 1 its size), the glow round them.
	 */
	public static void morph(GuiGraphics g, float secs, String a, String b, float ka, float kb, float white, float glow, float turnSecs) {
		BedrockModel.Def def = BedrockModel.def(a);
		double h = Math.max(0.6, def == null ? 1 : def.height), l = Math.max(0.6, h), d = Math.max(Math.max(h * 3.1, l * 2.3), 3.8);
		Cam cam = new Cam().look(0, h * 0.62 + d * 0.16, d, 0, h * 0.5, 0);
		double sc = Math.max(0.5, Math.min(3, l * 0.55));
		float[] c = cam.at(0, 0, 0), e = cam.at(1.4 * sc, 0, 0), lo = cam.at(0, -0.16, 0), n = cam.at(0, 0, -1.4 * sc), s0 = cam.at(0, 0, 1.4 * sc);
		float rx = Math.abs(e[0] - c[0]), ry = Math.abs(s0[1] - n[1]) / 2, cy = (s0[1] + n[1]) / 2, drop = lo[1] - c[1];
		V.ellipse(g, c[0], cy + drop + 6, rx * 1.1F, ry * 1.1F, 0x30000000);
		V.ellipse(g, c[0], cy + drop, rx * 1.1F, ry * 1.1F, 0xFF9FD6C4);
		V.rr(g, c[0] - rx * 1.1F, cy, rx * 2.2F, drop, 0, 0xFFA9DCCB);
		V.ellipse(g, c[0], cy, rx * 1.1F, ry * 1.1F, 0xFFBFEFE0);
		V.ellipse(g, c[0], cy, rx * 0.93F, ry * 0.93F, 0xFFE9FFF6);
		Figures.Batch bt = Figures.batch(V.bx, V.by, V.bx + V.gw, V.by + V.gh);
		double yaw = Math.PI + Math.sin(turnSecs * 0.45) * 0.7;
		if (ka > 0.001F) place(bt, cam, null, a, 0, 0, 0, yaw, secs).white(white).grow(ka);
		if (kb > 0.001F && b != null) place(bt, cam, null, b, 0, 0, 0, yaw, secs).white(white).grow(kb);
		bt.draw(g);
		if (glow > 0) {
			float[] mid = cam.at(0, h / 2, 0), up = cam.at(0, h / 2 + h * 0.75, 0), side2 = cam.at(l * 0.7, h / 2, 0);
			float grx = Math.abs(side2[0] - mid[0]), gry = Math.abs(mid[1] - up[1]);
			float al = Math.max(0, Math.min(1, glow * (0.65F + 0.3F * (float) Math.sin(secs * 12))));
			V.ellipse(g, mid[0], mid[1], grx, gry, Ui.alpha(0xFFFFFFFF, al * 0.55F));
		}
	}

	/** A creature with no stand under it (the summary, Let's Go's STATS page), turning gently, framed by its size, side over. */
	public static void modelBare(GuiGraphics g, float secs, String species, double side) {
		BedrockModel.Def def = BedrockModel.def(species);
		double h = Math.max(0.6, def == null ? 1 : def.height), l = Math.max(0.6, h), d = Math.max(Math.max(h * 3.1, l * 2.3), 3.8);
		Cam cam = new Cam().look(side, h * 0.62 + d * 0.16, d, side, h * 0.5, 0);
		blob(g, cam, 0, 0, Math.max(0.4, l * 0.4), 150);
		Figures.Batch b = Figures.batch(V.bx, V.by, V.bx + V.gw, V.by + V.gh);
		place(b, cam, null, species, 0, 0, 0, Math.PI + Math.sin(secs * 0.45) * 0.7, secs);
		b.draw(g);
	}

	/** A round stand at (x, 0, 0): its side a band, its top a pale disc (radius in blocks). */
	private static void standAt(GuiGraphics g, Cam cam, double x, double radius) {
		float[] c = cam.at(x, 0, 0), e = cam.at(x + radius, 0, 0), lo = cam.at(x, -0.16, 0), n = cam.at(x, 0, -radius), s = cam.at(x, 0, radius);
		float rx = Math.abs(e[0] - c[0]), ry = Math.abs(s[1] - n[1]) / 2, cy = (s[1] + n[1]) / 2, drop = lo[1] - c[1];
		V.ellipse(g, c[0], cy + drop + 6, rx * 1.08F, ry * 1.08F, 0x30000000);
		V.ellipse(g, c[0], cy + drop, rx * 1.08F, ry * 1.08F, 0xFF9FD6C4);
		V.rr(g, c[0] - rx * 1.08F, cy, rx * 2.16F, drop, 0, 0xFFA9DCCB);
		V.ellipse(g, c[0], cy, rx * 1.08F, ry * 1.08F, 0xFFBFEFE0);
		V.ellipse(g, c[0], cy, rx * 0.95F, ry * 0.95F, 0xFFE9FFF6);
	}

	/**
	 * A trade, as the viewer's: yours on a stand at the left, theirs at the right, facing each other. swap (0 to 1,
	 * or below 0 before it starts): they go into their balls, which cross over in arcs, and come out the other side.
	 */
	public static void trade(GuiGraphics g, float secs, String yours, String theirs, float swap) {
		Cam cam = new Cam().look(0, 2.2, 7.6, 0, 0.8, 0);
		standAt(g, cam, -2.2, 1.2);
		standAt(g, cam, 2.2, 1.2);
		boolean crossed = swap > 0.85F, mons = swap < 0.15F || crossed;
		if (mons) {
			Figures.Batch b = Figures.batch(V.bx, V.by, V.bx + V.gw, V.by + V.gh);
			place(b, cam, null, crossed ? theirs : yours, -2.2, 0, 0, Math.PI * 0.82, secs);
			place(b, cam, null, crossed ? yours : theirs, 2.2, 0, 0, -Math.PI * 0.82, secs + 0.5F);
			b.draw(g);
		} else {
			float m = Math.max(0, Math.min(1, (swap - 0.15F) / 0.7F));
			ball(g, cam, -2.2 + 4.4 * m, 0.8 + Math.sin(m * Math.PI) * 1.6, 0, 0xFFE5525C);
			ball(g, cam, 2.2 - 4.4 * m, 0.8 + Math.sin(m * Math.PI) * 1.2, 0.3, 0xFF4A8FE0);
		}
	}

	/** A ball in the air (0.22 blocks round), lit from above. */
	private static void ball(GuiGraphics g, Cam cam, double x, double y, double z, int colour) {
		float[] p = cam.at(x, y, z), q = cam.at(x + 0.22, y, z);
		float r = Math.max(3, Math.abs(q[0] - p[0]));
		blob(g, cam, x, z, 0.22, 140);
		V.ellipse(g, p[0], p[1], r, r, Ui.mix(colour, 0xFF000000, 0.25F));
		V.ellipse(g, p[0], p[1] - r * 0.06F, r * 0.92F, r * 0.92F, colour);
		V.ellipse(g, p[0] - r * 0.3F, p[1] - r * 0.35F, r * 0.28F, r * 0.22F, 0x8CFFFFFF);
	}

	/** The picture of you as you look now (your outfit's skin, made if need be). */
	public static Identifier you(String outfit, boolean girl) {
		return Outfit.ensure(outfit, girl) ? Outfit.HERO : Retromon.id("textures/entity/player/" + (girl ? "girl" : "boy") + ".png");
	}
}
