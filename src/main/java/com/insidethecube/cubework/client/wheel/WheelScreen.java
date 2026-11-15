package com.insidethecube.cubework.client.wheel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

/**
 * The shape wheel, drawn after the action wheel in Shards of Arcadia: the world blurs and darkens, and a
 * ring of dark wedges on a steel frame opens in the middle, a page of choices at a time. Point anywhere
 * and the wedge that way lights, warm gold, and steps out a little; a pointer on the gold rim follows your
 * hand; the middle says what the lit choice is, what it's made of (or why it can't be made) and its key.
 * Tabs along the top and arrows at the sides name the pages.
 *
 * Hold the wheel's key and let go over a choice to take it, or over the middle for the full block; a quick
 * tap leaves the wheel open to click instead. Scroll steps round the ring, the left and right arrow keys
 * (or the tabs and side arrows) turn the page, Enter takes the lit choice and 1 to 9 take one at once.
 */
public final class WheelScreen extends Screen {
	// Colours (0xAARRGGBB): dark wood, steel and old gold.
	private static final int GOLD = 0xD8B45C, GOLD_HOT = 0xFFF2D27A, GOLD_EDGE = 0xFFE8C46A, CREAM = 0xFFF2E2C0;
	private static final int LABEL = 0xFFB9AD94, LABEL_HOT = 0xFFFFF1C8, LABEL_NO = 0xFF5F574B, ICON = 0xFFCDBF9F, DETAIL = 0xFFA99E8C;
	private static final int RED_EDGE = 0x99E2574A, RED_LIP = 0xB3E2574A, RED_NAME = 0xFFC8A090, RED_DETAIL = 0xFFE07A62;
	private static final int WEDGE_IN = 0xF21C1712, WEDGE_OUT = 0xF20D0B09, HOT_IN = 0xF56A4E22, HOT_MID = 0xF53A2A14, HOT_OUT = 0xF51E160C;
	private static final int NO_FILL = 0xF20C0A08, NO_LINE = 0xFF1D1914, NO_HOT = 0xD93C140E;
	private static final int STEEL_DARK = 0xA82A2B32, STEEL_LIGHT = 0xA85A5C66, FRAME = 0xC8121216;
	private static final int HUB_IN = 0xFC2A2118, HUB_MID = 0xFC0E0B09, HUB_OUT = 0xFC0B0907, KEY_FILL = 0xF20B0907;

	// The wheel's proportions, as shares of R (the outer edge of the wedges' ring).
	private static final float HUB = 0.335f, IN = 0.415f, OUT = 0.985f, SPLIT = 0.776f, FRAME_IN = 0.992f, FRAME_OUT = 1.035f, RIM = 1.055f, TICK = 1.075f,
			HALO = 1.22f, GAP = 0.042f;

	private record Words(Lettering.Face face, String text, float cap, float track, float x, float midY, int colour, int align) {}

	private record Icon(ItemStack stack, float x, float y, float size, float dim) {}

	private record Fit(List<String> lines, float cap) {}

	private final Wheel wheel;
	private final KeyMapping key;
	private final Rings rings = new Rings();
	private final long opened = System.nanoTime();
	private long swapped = opened - 1_000_000_000L, last = opened, popped = opened;
	private int page, hot;
	private boolean hub, sticky, touched;
	private double mouseX = Double.NaN, mouseY = Double.NaN, pointer, pointerTarget;
	private float pointerShown, hubHeat;
	private float[] heat = new float[0];
	private boolean pointerSet;

	// Worked out each frame: the wheel's size and middle, the interface scale, and the opening animation.
	private float R = 120, cx, cy, scale = 3, k = 1, fade = 1, since;
	private double spin;
	private final List<Words> words = new ArrayList<>();
	private final List<Icon> icons = new ArrayList<>();
	/** The tabs and side arrows, for clicks: x0, y0, x1, y1 and the page they turn to. */
	private final List<float[]> hits = new ArrayList<>();

	public WheelScreen(Wheel wheel, KeyMapping key) {
		super(Component.literal(wheel.title()));
		this.wheel = wheel;
		this.key = key;
		this.page = wheel.pages().isEmpty() ? 0 : Math.max(0, Math.min(wheel.pages().size() - 1, wheel.start()));
		this.hot = wheel.hot() >= 0 && wheel.hot() < options().size() ? wheel.hot() : -1;
		if (hot >= 0) {
			pointer = pointerTarget = hot * Math.PI * 2 / options().size();
			pointerSet = true;
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private List<Wheel.Option> options() {
		return wheel.pages().isEmpty() ? List.of() : wheel.pages().get(page).options();
	}

	private boolean blocks() {
		return wheel.centre() != null;
	}

	// ---------------------------------------------------------------- layout and animation

	/** Sized to fit between the tabs and the hints, with room for the side arrows. */
	private void layout() {
		scale = Math.max(1, (float) Math.round(minecraft.getWindow().getGuiScale()));
		float top = 28, bottom = 28, side = 70;
		R = Math.max(56, Math.min(200, Math.min((height - top - bottom) / 2f / 1.13f, (width - 2 * side) / 2f / 1.13f)));
		cx = width / 2f;
		cy = top + (height - top - bottom) / 2f;
	}

	private static float clamp(float x) {
		return Math.max(0, Math.min(1, x));
	}

	/** Eases out with a small overshoot, like the wheel settling into place. */
	private static float settle(float x) {
		if (x <= 0) return 0;
		if (x >= 1) return 1;
		float c = 0.9f, t = x - 1;
		return 1 + (c + 1) * t * t * t + c * t * t;
	}

	private static float easeOut(float x) {
		x = clamp(x);
		return 1 - (1 - x) * (1 - x) * (1 - x);
	}

	private void animate() {
		long now = System.nanoTime();
		float dt = Math.min(0.1f, (now - last) / 1e9f);
		last = now;
		float t = (now - opened) / 1e9f, ts = (now - swapped) / 1e9f;
		float appear = settle(t / 0.32f), turn = settle(ts / 0.3f);
		k = (0.86f + 0.14f * appear) * (0.94f + 0.06f * turn);
		spin = Math.toRadians(-8 * (1 - appear) + 6 * (1 - turn));
		fade = clamp(t / 0.22f) * (0.4f + 0.6f * clamp(ts / 0.3f));
		since = Math.min(t, ts);
		int n = options().size();
		if (heat.length != n) heat = new float[n];
		float ease = 1 - (float) Math.exp(-dt / 0.05f);
		for (int i = 0; i < n; i++) heat[i] += ((i == hot ? 1 : 0) - heat[i]) * ease;
		hubHeat += ((hub && hot < 0 ? 1 : 0) - hubHeat) * ease;
		pointerShown += ((hot >= 0 ? 1 : 0) - pointerShown) * ease;
		if (hot >= 0) {
			if (!pointerSet) {
				pointer = pointerTarget;
				pointerSet = true;
			}
			double d = Math.IEEEremainder(pointerTarget - pointer, Math.PI * 2);
			pointer += d * (1 - Math.exp(-dt / 0.045));
		}
	}

	/** Which wedge of n (0 at the top, clockwise) an angle falls in. */
	private static int segment(double angle, int n) {
		double turn = 2 * Math.PI, a = ((angle + Math.PI / n) % turn + turn) % turn;
		return Math.min(n - 1, (int) (a / turn * n));
	}

	/** Lights whatever the mouse points at, if it has moved (or force): the middle, or the wedge that way. */
	private void point(double mx, double my, boolean force) {
		boolean first = Double.isNaN(mouseX), moved = !first && (Math.abs(mx - mouseX) > 0.5 || Math.abs(my - mouseY) > 0.5);
		mouseX = mx;
		mouseY = my;
		if (!moved && !force) return;
		if (moved) touched = true;
		double dx = mx - cx, dy = my - cy;
		int n = options().size();
		if (Math.sqrt(dx * dx + dy * dy) < HUB * R * k || n == 0) {
			light(-1, blocks());
			return;
		}
		double a = Math.atan2(dx, -dy);
		pointerTarget = a;
		light(segment(a - spin, n), false);
	}

	private void light(int h, boolean middle) {
		if (h == hot && middle == hub) return;
		hot = h;
		hub = middle;
		popped = System.nanoTime();
		if (h >= 0 || middle) sound(SoundEvents.UI_BUTTON_CLICK, 1.9f, 0.06f);
	}

	/** Steps round the ring, for the scroll wheel and the up and down keys. */
	private void step(int dir) {
		int n = options().size();
		if (n == 0) return;
		touched = true;
		int h = hot < 0 ? 0 : Math.floorMod(hot + dir, n);
		pointerTarget = h * Math.PI * 2 / n + spin;
		light(h, false);
	}

	private void turn(int to) {
		int n = wheel.pages().size();
		if (n <= 1) return;
		to = Math.floorMod(to, n);
		if (to == page) return;
		page = to;
		touched = true;
		swapped = System.nanoTime();
		heat = new float[options().size()];
		hot = -1;
		hub = false;
		sound(SoundEvents.BOOK_PAGE_TURN, 1.0f, 0.5f);
		if (!Double.isNaN(mouseX)) point(mouseX, mouseY, true);
	}

	private void sound(SoundEvent e, float pitch, float volume) {
		minecraft.getSoundManager().play(SimpleSoundInstance.forUI(e, pitch, volume));
	}

	private void sound(Holder<SoundEvent> e, float pitch, float volume) {
		sound(e.value(), pitch, volume);
	}

	// ---------------------------------------------------------------- text

	/** Track (letter spacing) in GUI units for a share of an em, at a cap height. */
	private static float track(float cap, float em) {
		return cap / 0.7f * em;
	}

	private float textWidth(Lettering.Face f, String s, float cap, float em) {
		if (f == Lettering.Face.CAPS) s = s.toUpperCase(Locale.ROOT);
		return Lettering.width(f, cap * scale, s, track(cap, em) * scale) / scale;
	}

	private void say(Lettering.Face f, String s, float cap, float em, float x, float midY, int colour, int align) {
		if (s == null || s.isEmpty()) return;
		words.add(new Words(f, f == Lettering.Face.CAPS ? s.toUpperCase(Locale.ROOT) : s, cap, track(cap, em), x, midY, colour, align));
	}

	/**
	 * Lines of s that fit maxW: its own line breaks, or broken at the space that best halves it when a line
	 * is too wide (up to maxLines), then made smaller, down to minScale, if still too wide.
	 */
	private Fit fit(Lettering.Face f, String s, float cap, float em, float maxW, int maxLines, float minScale) {
		String text = f == Lettering.Face.CAPS ? s.toUpperCase(Locale.ROOT) : s;
		List<String> lines = new ArrayList<>(List.of(text.split("\n")));
		while (lines.size() < maxLines) {
			int widest = 0;
			for (int i = 1; i < lines.size(); i++) if (textWidth(f, lines.get(i), cap, em) > textWidth(f, lines.get(widest), cap, em)) widest = i;
			String line = lines.get(widest);
			if (textWidth(f, line, cap, em) <= maxW) break;
			int best = -1;
			float bestW = Float.MAX_VALUE;
			for (int i = line.indexOf(' '); i > 0; i = line.indexOf(' ', i + 1)) {
				float w = Math.max(textWidth(f, line.substring(0, i), cap, em), textWidth(f, line.substring(i + 1), cap, em));
				if (w < bestW) {
					bestW = w;
					best = i;
				}
			}
			if (best < 0) break;
			lines.set(widest, line.substring(best + 1));
			lines.add(widest, line.substring(0, best));
		}
		float w = 0;
		for (String l : lines) w = Math.max(w, textWidth(f, l, cap, em));
		return new Fit(lines, w > maxW ? cap * Math.max(minScale, maxW / w) : cap);
	}

	// ---------------------------------------------------------------- drawing

	/** The world behind blurs and darkens towards the edges, as the wheel opens. */
	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
		renderBlurredBackground(g);
		layout();
		float t = clamp((System.nanoTime() - opened) / 1e9f / 0.25f);
		float d = (float) Math.hypot(Math.max(cx, width - cx), Math.max(cy, height - cy)) + 2;
		rings.set(1f / scale, t);
		rings.band(cx, cy, 0, 0.6f * d, 0, Math.PI * 2, Rings.radial(0x40060508, 0xC0060508), false, false);
		rings.band(cx, cy, 0.6f * d, d * 1.05f, 0, Math.PI * 2, Rings.radial(0xC0060508, 0xE6040306), false, false);
		rings.submit(g);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float delta) {
		super.render(g, mx, my, delta);
		layout();
		point(mx, my, false);
		animate();
		words.clear();
		icons.clear();
		hits.clear();
		List<Wheel.Option> opts = options();
		float px = 1f / scale;
		rings.set(px, fade);
		frame();
		for (int i = 0; i < opts.size(); i++) if (i != hot) wedge(opts, i);
		if (hot >= 0 && hot < opts.size()) wedge(opts, hot);
		rings.set(px, fade);
		middle(opts);
		pointer();
		rings.set(px, Math.min(1, fade * 1.5f));
		tabs();
		arrows();
		hints(opts);
		rings.submit(g);
		// The blocks, over the wheel; then a shade over the ones that can't be made; then the lettering.
		for (Icon ic : icons) {
			g.pose().pushMatrix();
			g.pose().translate(ic.x(), ic.y());
			g.pose().scale(ic.size() / 16f, ic.size() / 16f);
			g.renderItem(ic.stack(), -8, -8);
			g.pose().popMatrix();
		}
		rings.set(px, fade);
		for (Icon ic : icons) if (ic.dim() > 0) rings.disc(ic.x(), ic.y(), ic.size() * 0.74f, Rings.fade(0xA60C0A08, ic.dim()), Rings.fade(0x800C0A08, ic.dim()));
		rings.submit(g);
		for (Words w : words) {
			float tw = Lettering.width(w.face(), w.cap() * scale, w.text(), w.track() * scale) / scale;
			float left = w.align() == 0 ? w.x() - tw / 2 : w.align() > 0 ? w.x() - tw : w.x();
			Lettering.draw(g, w.face(), w.cap() * scale, w.text(), left * scale, (w.midY() + w.cap() / 2) * scale, w.track() * scale, w.colour(), scale);
		}
		Lettering.flush();
	}

	private static float sx(double a) {
		return (float) Math.sin(a);
	}

	private static float sy(double a) {
		return (float) -Math.cos(a);
	}

	/** The halo, the steel the wedges sit in, the dark frame, the gold rim and its ticks. */
	private void frame() {
		float r = R * k, px = rings.px();
		rings.glow(cx, cy, 1.03f * r, HALO * r, 0x24D8B45C);
		final double sp = spin;
		rings.ring(cx, cy, HUB * r, FRAME_OUT * r, (a, outer) -> {
			// Lit from the top left.
			double light = 0.5 + 0.5 * Math.cos(a - sp + Math.PI / 4);
			return Rings.mix(STEEL_DARK, STEEL_LIGHT, outer ? light * 0.75 : light);
		});
		rings.ring(cx, cy, FRAME_IN * r, FRAME_OUT * r, FRAME, FRAME);
		rings.circle(cx, cy, RIM * r, Math.max(px, 0.005f * r), 0x73000000 | GOLD);
		float tw = Math.max(px, 0.0045f * r);
		for (int i = 0; i < 72; i++) {
			double a = i * Math.PI * 2 / 72 + spin;
			float r0 = TICK * r, r1 = (i % 6 == 0 ? 1.125f : 1.097f) * r;
			rings.line(cx + sx(a) * r0, cy + sy(a) * r0, cx + sx(a) * r1, cy + sy(a) * r1, tw, 0x59000000 | GOLD);
		}
		rings.circle(cx, cy, 0.392f * r, Math.max(px, 0.004f * r), 0x40000000 | GOLD);
	}

	/** One wedge and what's on it: its block (or word), its label and its key. */
	private void wedge(List<Wheel.Option> opts, int i) {
		Wheel.Option o = opts.get(i);
		int n = opts.size();
		float appear = easeOut((since - 0.06f - i * 0.022f) / 0.34f);
		if (appear <= 0.001f) return;
		rings.set(rings.px(), fade * appear);
		float e = 0.7f + 0.3f * appear, hi = heat[i], r = R * k;
		boolean no = o.blocked();
		double stepA = Math.PI * 2 / n, mid = i * stepA + spin, a0 = mid - stepA / 2, a1 = mid + stepA / 2;
		float push = n > 1 ? 0.02f * r * hi : 0, wx = cx + sx(mid) * push, wy = cy + sy(mid) * push;
		float r0 = IN * r * e, r1 = OUT * r * e, rs = SPLIT * r * e, gap = n > 1 ? GAP * r : 0;
		float sw = Math.max(rings.px(), 0.0045f * r) * (1 + 0.35f * hi);

		// A warm glow round the lit wedge.
		if (hi > 0.01f && !no) {
			for (int j = 1; j <= 3; j++) {
				float ex = 0.012f * r * j;
				rings.slice(wx, wy, r0 - ex, r1 + ex, a0, a1, gap - 2 * ex, Rings.flat(Rings.fade(0x30E8C46A, hi / j)), true, true);
			}
		}
		// Its edge, then its fill: dark wood, or warm gold when lit; hatched, or dark red when lit, if it can't be made.
		int edge = Rings.mix(no ? 0x1F000000 | GOLD : 0x47000000 | GOLD, no ? RED_EDGE : GOLD_EDGE, hi);
		rings.slice(wx, wy, r0, r1, a0, a1, gap, Rings.flat(edge), true, true);
		float t = (SPLIT - IN) / (OUT - IN);
		int nIn = no ? NO_FILL : WEDGE_IN, nMid = no ? NO_FILL : Rings.mix(WEDGE_IN, WEDGE_OUT, t), nOut = no ? NO_FILL : WEDGE_OUT;
		int hIn = no ? NO_HOT : HOT_IN, hMid = no ? NO_HOT : HOT_MID, hOut = no ? NO_HOT : HOT_OUT;
		int cIn = Rings.mix(nIn, hIn, hi), cMid = Rings.mix(nMid, hMid, hi), cOut = Rings.mix(nOut, hOut, hi);
		rings.slice(wx, wy, r0 + sw, rs, a0, a1, gap + 2 * sw, Rings.radial(cIn, cMid), true, false);
		rings.slice(wx, wy, rs, r1 - sw, a0, a1, gap + 2 * sw, Rings.radial(cMid, cOut), false, true);
		if (no && hi < 0.99f) {
			rings.hatch(wx, wy, r0 + sw, r1 - sw, a0, a1, gap + 2 * sw, 0.026f * r, 0.0086f * r, Rings.fade(NO_LINE, 1 - hi));
		}
		// The lip along its outer edge: gold for the shape in your hand, bright when lit.
		int lip = Rings.mix(o.current() ? 0xFF000000 | GOLD : no ? 0x0F000000 | GOLD : 0x2E000000 | GOLD, no ? RED_LIP : GOLD_HOT, hi);
		rings.slice(wx, wy, r1 - 0.013f * r * e, r1, a0, a1, gap, Rings.flat(lip), false, true);

		// What's on it, centred on the middle of the wedge.
		float alpha = fade * appear;
		float rm = (IN + OUT) / 2 * r * e;
		float chord = n <= 2 ? 0.6f * r : 2 * rm * (float) Math.sin(stepA / 2) - gap;
		float ox = wx + sx(mid) * rm, oy = wy + sy(mid) * rm;
		int labelColour = Rings.fade(Rings.mix(no ? LABEL_NO : LABEL, LABEL_HOT, hi), alpha);
		int keyColour = Rings.fade(Rings.mix(no ? 0x66000000 | GOLD : 0x99000000 | GOLD, GOLD_HOT, hi), alpha);
		String keyText = i < 9 ? String.valueOf(i + 1) : null;
		float keyCap = 0.026f * r;
		if (o.icon().isEmpty()) {
			String word = o.word() == null ? o.name() : o.word();
			Fit f = fit(Lettering.Face.CAPS, word, 0.055f * r, 0.08f, chord * 0.8f, 1, 0.5f);
			say(Lettering.Face.CAPS, f.lines().get(0), f.cap(), 0.08f, ox, oy, Rings.fade(Rings.mix(no ? LABEL_NO : ICON, LABEL_HOT, hi), alpha), 0);
			if (keyText != null) {
				float ww = textWidth(Lettering.Face.CAPS, f.lines().get(0), f.cap(), 0.08f);
				say(Lettering.Face.CAPS, keyText, keyCap, 0.1f, ox + ww / 2 + 0.03f * r, oy - f.cap() * 0.9f, keyColour, 0);
			}
			return;
		}
		float size = Math.max(0.11f * r, Math.min(0.17f * r, chord * 0.42f)) * e;
		Fit f = o.label() == null ? null : fit(Lettering.Face.CAPS, o.label(), 0.032f * r, 0.14f, Math.min(0.5f * r, chord * 0.86f), 2, 0.62f);
		float lineGap = f == null ? 0 : f.cap() * 1.75f, labelH = f == null ? 0 : f.cap() + lineGap * (f.lines().size() - 1), between = f == null ? 0 : 0.03f * r;
		float top = oy - (size + between + labelH) / 2, iy = top + size / 2;
		if (appear > 0.35f) icons.add(new Icon(o.icon(), ox, iy, size, no ? 1 - 0.6f * hi : 0));
		if (f != null) {
			for (int l = 0; l < f.lines().size(); l++) {
				say(Lettering.Face.CAPS, f.lines().get(l), f.cap(), 0.14f, ox, top + size + between + f.cap() / 2 + l * lineGap, labelColour, 0);
			}
		}
		if (keyText != null) say(Lettering.Face.CAPS, keyText, keyCap, 0.1f, ox + size * 0.62f, iy - size * 0.42f, keyColour, 0);
	}

	/** The middle: what the lit choice is, what it's made of or why it can't be made, and its key. */
	private void middle(List<Wheel.Option> opts) {
		float r = R * k, hr = HUB * r, px = rings.px();
		rings.ring(cx, cy, hr, hr + 0.035f * r, 0x59000000, 0x00000000);
		rings.band(cx, cy, 0, 0.7f * hr, 0, Math.PI * 2, Rings.radial(HUB_IN, HUB_MID), false, false);
		rings.band(cx, cy, 0.7f * hr, hr, 0, Math.PI * 2, Rings.radial(HUB_MID, HUB_OUT), false, true);
		rings.glow(cx, cy - 0.12f * r, 0, 0.2f * r, 0x1E3A2C1C);
		rings.circle(cx, cy, hr, Math.max(px, 0.0065f * r) * (1 + hubHeat), Rings.mix(0x80000000 | GOLD, GOLD_EDGE, hubHeat));
		if (hubHeat > 0.01f) rings.glow(cx, cy, hr, hr + 0.05f * r, Rings.fade(0x40E8C46A, hubHeat));

		Wheel.Option shown = hot >= 0 && hot < opts.size() ? opts.get(hot) : hub ? wheel.centre() : null;
		boolean no = shown != null && shown.blocked();
		String caption = shown == null ? wheel.title() : hot >= 0 ? wheel.pages().get(page).name() : "Full block";
		ItemStack icon = shown == null || shown.icon().isEmpty() ? wheel.icon() : shown.icon();
		String name = shown == null ? "Choose" : shown.name();
		String detail = shown == null ? (blocks() ? "Point at a shape" : "Point the way you want") : shown.detail();
		String keyText = hot >= 0 && hot < 9 ? String.valueOf(hot + 1) : null;

		float maxW = 0.58f * r;
		Fit cap = fit(Lettering.Face.CAPS, caption, 0.026f * r, 0.3f, 0.46f * r, 1, 0.6f);
		Fit nm = fit(Lettering.Face.CAPS, name, 0.04f * r, 0.14f, maxW, 2, 0.6f);
		Fit dt = detail == null || detail.isEmpty() ? null : fit(Lettering.Face.ITALIC, detail, 0.034f * r, 0, maxW, 2, 0.75f);
		float size = 0.15f * r, keyH = keyText == null ? 0 : 0.064f * r;
		float nmH = nm.cap() * (1 + 1.45f * (nm.lines().size() - 1)), dtH = dt == null ? 0 : dt.cap() * (1 + 1.5f * (dt.lines().size() - 1));
		float g1 = 0.03f * r, g2 = 0.05f * r, g3 = dt == null ? 0 : 0.035f * r, g4 = keyText == null ? 0 : 0.045f * r;
		float total = cap.cap() + g1 + size + g2 + nmH + g3 + dtH + g4 + keyH;
		if (total > 0.62f * r && keyText != null) {
			total -= g4 + keyH;
			keyText = null;
			keyH = g4 = 0;
		}
		if (total > 0.62f * r) {
			total -= size - 0.11f * r;
			size = 0.11f * r;
		}
		float y = cy - total / 2;
		float alpha = fade;
		say(Lettering.Face.CAPS, cap.lines().get(0), cap.cap(), 0.3f, cx, y + cap.cap() / 2, Rings.fade(0x99000000 | GOLD, alpha), 0);
		y += cap.cap() + g1;
		float pop = 0.7f + 0.3f * settle((System.nanoTime() - popped) / 1e9f / 0.25f), iy = y + size / 2;
		if (shown != null && !no) rings.glow(cx, iy, 0, size * 0.8f, 0x55F2D27A);
		if (!icon.isEmpty()) icons.add(new Icon(icon, cx, iy, size * pop, no ? 0.6f : 0));
		y += size + g2;
		for (int l = 0; l < nm.lines().size(); l++) {
			say(Lettering.Face.CAPS, nm.lines().get(l), nm.cap(), 0.14f, cx, y + nm.cap() / 2 + l * nm.cap() * 1.45f, Rings.fade(no ? RED_NAME : CREAM, alpha), 0);
		}
		y += nmH + g3;
		if (dt != null) {
			for (int l = 0; l < dt.lines().size(); l++) {
				say(Lettering.Face.ITALIC, dt.lines().get(l), dt.cap(), 0, cx, y + dt.cap() / 2 + l * dt.cap() * 1.5f, Rings.fade(no ? RED_DETAIL : DETAIL, alpha), 0);
			}
			y += dtH;
		}
		if (keyText != null) keycap(cx, y + g4 + keyH / 2, keyH, keyText, 0.03f * r);
	}

	/** A key, as a dark cap with a gold edge (deeper at the bottom) and its letter on it. */
	private float keycap(float x, float midY, float h, String text, float cap) {
		float tw = textWidth(Lettering.Face.CAPS, text, cap, 0.05f), w = Math.max(h * 1.05f, tw + cap * 1.3f), bw = Math.max(rings.px(), h * 0.07f);
		float x0 = x - w / 2, y0 = midY - h / 2;
		keyShape(x0, y0, w, h, bw);
		say(Lettering.Face.CAPS, text, cap, 0.05f, x, midY - bw / 2, Rings.fade(CREAM, fade), 0);
		return w;
	}

	private void keyShape(float x0, float y0, float w, float h, float bw) {
		rings.rrect(x0, y0, x0 + w, y0 + h, h * 0.22f, 0xB3000000 | GOLD);
		rings.rrect(x0 + bw, y0 + bw, x0 + w - bw, y0 + h - 2 * bw, h * 0.18f, KEY_FILL);
	}

	/** The pointer on the rim, following your hand to the lit wedge. */
	private void pointer() {
		if (pointerShown < 0.01f) return;
		float r = R * k, s = pointerShown;
		double a = pointer;
		float c = (float) Math.cos(a), d = (float) Math.sin(a);
		float tip = 1.045f * r, base = 1.105f * r, hw = 0.038f * r;
		float tx = cx + sx(a) * tip, ty = cy + sy(a) * tip, bx = cx + sx(a) * base, by = cy + sy(a) * base;
		rings.glow((tx + bx) / 2, (ty + by) / 2, 0, 0.075f * r, Rings.fade(0x80F2D27A, s * 0.7f));
		rings.band(cx, cy, 1.012f * r, 1.032f * r, a - 0.16, a + 0.16, Rings.flat(Rings.fade(0xBFF2D27A, s)), true, true);
		rings.tri(tx, ty, bx + c * hw, by + d * hw, bx - c * hw, by - d * hw, Rings.fade(GOLD_HOT, s));
	}

	/** The pages, along the top: the open one bright, gold diamonds between them. Click one to turn to it. */
	private void tabs() {
		List<Wheel.Page> pages = wheel.pages();
		int np = pages.size();
		if (np < 2) return;
		float cap = Math.max(4.2f, Math.min(6.5f, 0.03f * R)), em = 0.3f, maxW = width - 24;
		float[] w = new float[np];
		float total = 0;
		for (int pass = 0; pass < 3; pass++) {
			float sep = cap * 3.2f;
			total = sep * (np - 1);
			for (int i = 0; i < np; i++) total += w[i] = textWidth(Lettering.Face.CAPS, pages.get(i).name(), cap, em);
			if (total <= maxW) break;
			if (pass == 0) em = 0.15f;
			else cap *= Math.max(0.7f, maxW / total);
		}
		// Still too wide: the pages nearest the open one.
		int first = 0, lastPage = np - 1;
		float sep = cap * 3.2f;
		while (total > maxW && lastPage - first > 1) {
			if (page - first > lastPage - page) {
				total -= w[first] + sep;
				first++;
			} else {
				total -= w[lastPage] + sep;
				lastPage--;
			}
		}
		float x = width / 2f - total / 2, y = 14;
		for (int i = first; i <= lastPage; i++) {
			boolean over = mouseX >= x - 3 && mouseX <= x + w[i] + 3 && mouseY >= y - 9 && mouseY <= y + 9;
			int c = i == page ? GOLD_EDGE : over ? 0xD9000000 | GOLD : 0x8C000000 | GOLD;
			say(Lettering.Face.CAPS, pages.get(i).name(), cap, em, x, y, Rings.fade(c, fade), -1);
			hits.add(new float[] {x - 3, y - 9, x + w[i] + 3, y + 9, i});
			x += w[i];
			if (i < lastPage) {
				float dx = x + sep / 2, s = cap * 0.42f;
				int dc = Rings.fade(0x99000000 | GOLD, fade);
				rings.tri(dx, y - s, dx + s, y, dx, y + s, dc);
				rings.tri(dx, y - s, dx, y + s, dx - s, y, dc);
				x += sep;
			}
		}
	}

	/** Chevrons at the sides, with the page each way named under them. */
	private void arrows() {
		List<Wheel.Page> pages = wheel.pages();
		int np = pages.size();
		if (np < 2) return;
		float ax = Math.min(width / 2f - 36, 1.52f * R), ay = cy - 0.02f * R, hh = 0.07f * R, dd = 0.036f * R, lw = Math.max(rings.px() * 1.5f, 0.014f * R);
		float cap = Math.max(3.8f, Math.min(6f, 0.028f * R)), maxW = Math.max(48, 2 * (ax - 1.16f * R));
		for (int s = -1; s <= 1; s += 2) {
			float x = cx + s * ax;
			int to = Math.floorMod(page + s, np);
			float x0 = x - Math.max(24, maxW / 2), x1 = x + Math.max(24, maxW / 2), y0 = ay - hh - 6, y1 = ay + 0.2f * R;
			boolean over = mouseX >= x0 && mouseX <= x1 && mouseY >= y0 && mouseY <= y1;
			int c = Rings.fade(over ? GOLD_HOT : 0xD9000000 | GOLD, fade);
			rings.line(x - s * dd / 2, ay - hh, x + s * dd / 2, ay, lw, c);
			rings.line(x + s * dd / 2, ay, x - s * dd / 2, ay + hh, lw, c);
			Fit f = fit(Lettering.Face.CAPS, pages.get(to).name(), cap, 0.3f, maxW, 1, 0.6f);
			say(Lettering.Face.CAPS, f.lines().get(0), f.cap(), 0.3f, x, ay + hh + 0.07f * R, Rings.fade(over ? 0xE6000000 | GOLD : 0x99000000 | GOLD, fade), 0);
			hits.add(new float[] {x0, y0, x1, y1, to});
		}
	}

	/** The keys, along the bottom. */
	private void hints(List<Wheel.Option> opts) {
		int np = wheel.pages().size(), n = Math.min(9, opts.size());
		String keyName = key.getTranslatedKeyMessage().getString();
		List<String[]> items = new ArrayList<>();
		items.add(new String[] {keyName, sticky ? "Close" : "Let go to choose"});
		items.add(new String[] {"Enter", "Choose"});
		items.add(new String[] {null, "Scroll to step round"});
		if (np > 1) items.add(new String[] {"<>", np == 2 ? wheel.pages().get((page + 1) % 2).name() : "Turn the page"});
		if (n > 1) items.add(new String[] {"1-" + n, wheel.pages().get(page).name()});
		float cap = Math.max(3.8f, Math.min(5.8f, 0.028f * R)), em = 0.18f;
		float total = 0;
		for (int pass = 0; pass < 4; pass++) {
			total = hintsWidth(items, cap, em);
			if (total <= width - 16) break;
			if (pass == 0) items.remove(2);
			else cap *= Math.max(0.75f, (width - 16) / total);
		}
		float x = width / 2f - total / 2, y = height - 14, kh = cap * 2.3f, gapKey = cap * 0.9f;
		int c = Rings.fade(0x8C000000 | GOLD, fade);
		for (String[] it : items) {
			if (it[0] != null) {
				x += keys(it[0], x, y, kh, cap) + gapKey;
			}
			float tw = textWidth(Lettering.Face.CAPS, it[1], cap, em);
			say(Lettering.Face.CAPS, it[1], cap, em, x, y, c, -1);
			x += tw + cap * 4;
		}
	}

	private float hintsWidth(List<String[]> items, float cap, float em) {
		float kh = cap * 2.3f, total = 0;
		for (String[] it : items) {
			if (it[0] != null) total += keysWidth(it[0], kh, cap) + cap * 0.9f;
			total += textWidth(Lettering.Face.CAPS, it[1], cap, em) + cap * 4;
		}
		return total - cap * 4;
	}

	private float keyWidth(String text, float h, float cap) {
		return Math.max(h * 1.05f, textWidth(Lettering.Face.CAPS, text, cap * 0.85f, 0.05f) + cap * 0.85f * 1.3f);
	}

	/** Key caps for a hint: one key, "1-9" (two keys and a dash between) or "<>" (the arrow keys). */
	private float keysWidth(String spec, float h, float cap) {
		if (spec.equals("<>")) return h * 2 + cap * 0.5f;
		int dash = spec.indexOf('-');
		if (dash > 0) return keyWidth(spec.substring(0, dash), h, cap) + keyWidth(spec.substring(dash + 1), h, cap) + cap * 1.6f;
		return keyWidth(spec, h, cap);
	}

	private float keys(String spec, float x, float y, float h, float cap) {
		float bw = Math.max(rings.px(), h * 0.07f);
		int c = Rings.fade(CREAM, fade);
		if (spec.equals("<>")) {
			for (int s = -1; s <= 1; s += 2) {
				float x0 = s < 0 ? x : x + h + cap * 0.5f, mx = x0 + h / 2, my = y - bw / 2, len = h * 0.26f, head = h * 0.14f, lw = Math.max(rings.px(), h * 0.06f);
				keyShape(x0, y - h / 2, h, h, bw);
				rings.line(mx - len, my, mx + len, my, lw, c);
				rings.line(mx + s * len, my, mx + s * (len - head), my - head, lw, c);
				rings.line(mx + s * len, my, mx + s * (len - head), my + head, lw, c);
			}
			return keysWidth(spec, h, cap);
		}
		int dash = spec.indexOf('-');
		if (dash > 0) {
			float a = keyWidth(spec.substring(0, dash), h, cap);
			keycap(x + a / 2, y, h, spec.substring(0, dash), cap * 0.85f);
			rings.line(x + a + cap * 0.45f, y, x + a + cap * 1.15f, y, Math.max(rings.px(), cap * 0.14f), Rings.fade(0x8C000000 | GOLD, fade));
			float b = keyWidth(spec.substring(dash + 1), h, cap);
			keycap(x + a + cap * 1.6f + b / 2, y, h, spec.substring(dash + 1), cap * 0.85f);
			return keysWidth(spec, h, cap);
		}
		float w = keyWidth(spec, h, cap);
		keycap(x + w / 2, y, h, spec, cap * 0.85f);
		return w;
	}

	// ---------------------------------------------------------------- choosing

	/** Takes the lit choice (or the middle's). One that can't be made stays open, unless closing anyway. */
	private void choose(boolean closeAnyway) {
		List<Wheel.Option> opts = options();
		Wheel.Option o = hot >= 0 && hot < opts.size() ? opts.get(hot) : hub ? wheel.centre() : null;
		if (o != null && o.blocked()) {
			if (closeAnyway) onClose();
			return;
		}
		onClose();
		if (o != null && o.action() != null) o.action().run();
	}

	/** Whether the wheel's key is still held (a key on the keyboard; a mouse button can't be checked). */
	private boolean held() {
		InputConstants.Key bound = KeyBindingHelper.getBoundKeyOf(key);
		if (bound.getType() != InputConstants.Type.KEYSYM) return true;
		return InputConstants.isKeyDown(minecraft.getWindow(), bound.getValue());
	}

	private void released() {
		if (!touched) {
			// A tap stays open to be clicked; held still and let go, it just closes.
			if (System.nanoTime() - opened < 250_000_000L) sticky = true;
			else onClose();
			return;
		}
		choose(true);
	}

	@Override
	public void tick() {
		if (!sticky && !held()) released();
	}

	@Override
	public boolean keyReleased(KeyEvent event) {
		if (!sticky && key.matches(event)) {
			released();
			return true;
		}
		return super.keyReleased(event);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		int k = event.key();
		// The number keys 1 to 9, on the keyboard or the keypad (GLFW 49 to 57, 321 to 329); 0 for the middle.
		if (k >= 49 && k <= 57 || k >= 321 && k <= 329) {
			int i = k >= 321 ? k - 321 : k - 49;
			if (i < options().size()) {
				light(i, false);
				choose(false);
			}
			return true;
		}
		if ((k == 48 || k == 320) && blocks()) {
			light(-1, true);
			choose(false);
			return true;
		}
		switch (k) {
			case 257, 335, 32 -> { // Enter, keypad Enter, space
				touched = true;
				choose(false);
				return true;
			}
			case 262 -> { // right
				turn(page + 1);
				return true;
			}
			case 263 -> { // left
				turn(page - 1);
				return true;
			}
			case 264 -> { // down
				step(1);
				return true;
			}
			case 265 -> { // up
				step(-1);
				return true;
			}
			case 258 -> { // Tab, or Shift and Tab
				turn(page + ((event.modifiers() & 1) != 0 ? -1 : 1));
				return true;
			}
			default -> {}
		}
		if (sticky && key.matches(event)) {
			onClose();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double x = event.x(), y = event.y();
		if (event.button() == 0) {
			for (float[] h : hits) {
				if (x >= h[0] && x <= h[2] && y >= h[1] && y <= h[3]) {
					turn((int) h[4]);
					return true;
				}
			}
			layout();
			point(x, y, true);
			touched = true;
			choose(false);
			return true;
		}
		if (event.button() == 1) {
			onClose();
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
		if (vertical != 0) step(vertical < 0 ? 1 : -1);
		return true;
	}
}
