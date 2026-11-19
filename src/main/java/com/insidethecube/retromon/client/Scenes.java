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
		double focal = V.H / 2 / Math.tan(Math.toRadians(17));

		public Cam look(double px, double py, double pz, double tx, double ty, double tz) {
			this.px = px;
			this.py = py;
			this.pz = pz;
			double fx = tx - px, fy = ty - py, fz = tz - pz, fl = Math.sqrt(fx * fx + fy * fy + fz * fz);
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
		Figures.Fig f = skin != null ? b.person(skin, V.x(p[0]), V.y(p[1]), 1.8F * ppb) : b.mon(species, V.x(p[0]), V.y(p[1]), 1).perBlock(ppb);
		return f.facing(wrap(facing)).pitch(cam.pitch()).depth((9 - p[2]) * 24).at(secs);
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
		V.img(g, V.tex(ours ? "bg_title_day" : "bg_title_night"), -V.ox / V.k, -V.oy / V.k, V.gw / V.k, V.gh / V.k, 480, 270, 0xFFFFFFFF);
		int frame = (int) (secs * 12) % 48;
		V.sub(g, ours ? TITLE_OURS : TITLE_THEIRS, 0, 0, V.W, V.H, (frame % 8) * 256, (frame / 8) * 144, 256, 144, 2048, 864);
	}

	/** A backdrop picture (textures/gui/v/bg_*.png) filling the whole window. */
	public static void backdrop(GuiGraphics g, String name) {
		V.img(g, V.tex("bg_" + name), -V.ox / V.k, -V.oy / V.k, V.gw / V.k, V.gh / V.k, 480, 270, 0xFFFFFFFF);
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
		Figures.Batch b = Figures.batch(0, 0, V.gw, V.gh);
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
				// Wandering round in a slow loop, facing the way it goes.
				double a = secs * 0.45;
				px = -1.25 + Math.cos(a) * 0.95;
				pz = 0.85 + Math.sin(a) * 0.45;
				yaw = Math.atan2(Math.sin(a) * 0.95, -Math.cos(a) * 0.45) + Math.PI;
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
		Figures.Batch b = Figures.batch(0, 0, V.gw, V.gh);
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
		Figures.Batch b = Figures.batch(0, 0, V.gw, V.gh);
		place(b, cam, null, species, 0, 0, 0, Math.PI + Math.sin(secs * 0.45) * 0.7, secs);
		b.draw(g);
	}

	/** The picture of you as you look now (your outfit's skin, made if need be). */
	public static Identifier you(String outfit, boolean girl) {
		return Outfit.ensure(outfit, girl) ? Outfit.HERO : Retromon.id("textures/entity/player/" + (girl ? "girl" : "boy") + ".png");
	}
}
