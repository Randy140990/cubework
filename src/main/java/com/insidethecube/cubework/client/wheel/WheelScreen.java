package com.insidethecube.cubework.client.wheel;

import java.util.List;
import java.util.function.IntPredicate;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * The shape wheel. Hold the key: groups sit on the inner ring, and pointing at one opens its options on
 * the outer ring; let go over an option to choose it (or over the middle for the full block). A quick tap
 * leaves it open to click instead. Scroll to step through the groups, or press 1 to 9 for an option.
 *
 * The look follows Shards of Arcadia's book: see-through slate slices with clear gaps between them, a brass
 * rim that catches the light at the top, the slice you point at outlined and glowing warm inside its own
 * edges, and a name plate underneath that says what letting go will do.
 */
public final class WheelScreen extends Screen {
	private static final int SLATE = 0xB41A1C21, SLATE_EDGE = 0xC4282B32, HUB = 0xD8111215;
	private static final int BRASS_LIGHT = 0xFFF0D08A, BRASS = 0xFFB98D4E, BRASS_DARK = 0xFF6B4E2A;
	private static final int GLOW_IN = 0xE06E4A1C, GLOW_OUT = 0xE0D9A64C, ACTIVE_IN = 0xC8362A1A, ACTIVE_OUT = 0xC8574124;
	private static final int TEXT = 0xFFF2E6CC, TEXT_DIM = 0xFFA89A80, CURRENT = 0xFF7FD3C7;
	/** Room kept above the wheel for the hint and below it for the name plate (GUI pixels). */
	private static final int TOP = 22, BOTTOM = 40;

	private final Wheel wheel;
	private final KeyMapping key;
	private final long opened = System.currentTimeMillis();
	private final Rings rings = new Rings();
	private int active, hoverGroup = -1, hoverOption = -1;
	private boolean hoverCentre, sticky;
	/** Size and middle of the wheel, worked out each frame so it always fits the screen with its plate. */
	private float s = 1, cx, cy;

	public WheelScreen(Wheel wheel, KeyMapping key) {
		super(Component.literal(wheel.title()));
		this.wheel = wheel;
		this.key = key;
		this.active = Math.max(0, Math.min(wheel.groups().size() - 1, wheel.start()));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	/** A light shade over the world and no blur, so you can still see what you're building. */
	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
		g.fillGradient(0, 0, width, height, 0x28000000, 0x48000000);
	}

	/** The wheel is 264 units across at size 1; it's sized to fit between the hint and the name plate. */
	private void layout() {
		float room = Math.max(120, height - TOP - BOTTOM);
		s = Math.max(0.5f, Math.min(1.35f, Math.min(room, width - 20) / 264f));
		cx = width / 2f;
		cy = TOP + room / 2f;
	}

	private float open() {
		float t = Math.min(1, (System.currentTimeMillis() - opened) / 140f);
		return 1 - (1 - t) * (1 - t) * (1 - t);
	}

	// Radii, before the opening animation: the hub, the inner ring and the outer ring.
	private float hub() {
		return 30 * s;
	}

	private float in0() {
		return 34 * s;
	}

	private float in1() {
		return 76 * s;
	}

	private float out0() {
		return 82 * s;
	}

	private float out1() {
		return 128 * s;
	}

	private boolean blocks() {
		return wheel.centre() != null;
	}

	private List<Wheel.Option> options() {
		return wheel.groups().isEmpty() ? List.of() : wheel.groups().get(active).options();
	}

	/** Which segment of n (0 at the top, clockwise) an angle falls in. */
	private static int segment(double angle, int n) {
		double turn = 2 * Math.PI, a = ((angle + Math.PI / n) % turn + turn) % turn;
		return Math.min(n - 1, (int) (a / turn * n));
	}

	private void hover(int mouseX, int mouseY) {
		layout();
		double dx = mouseX - cx, dy = mouseY - cy, d = Math.sqrt(dx * dx + dy * dy), angle = Math.atan2(dx, -dy);
		hoverCentre = d < hub() && wheel.centre() != null;
		hoverGroup = -1;
		hoverOption = -1;
		int groups = wheel.groups().size();
		if (d >= hub() && d < (in1() + out0()) / 2 && groups > 0) {
			hoverGroup = segment(angle, groups);
			active = hoverGroup;
		} else if (d >= (in1() + out0()) / 2 && !options().isEmpty()) {
			hoverOption = segment(angle, options().size());
		}
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		super.render(g, mouseX, mouseY, delta);
		hover(mouseX, mouseY);
		float k = 0.86f + 0.14f * open(), fadeIn = open();
		float hub = hub() * k, in0 = in0() * k, in1 = in1() * k, out0 = out0() * k, out1 = out1() * k, gap = 2.5f * s;
		int groups = wheel.groups().size();
		List<Wheel.Option> opts = options();

		// Outer ring: the active group's options, each its own slice.
		if (!opts.isEmpty()) {
			slices(out0, out1, opts.size(), hoverOption, -1, i -> opts.get(i).current(), gap, fadeIn);
			brass(out1 + 1.2f * s, out1 + 3.2f * s, fadeIn);
		}

		// Inner ring: the groups.
		if (groups > 0) slices(in0, in1, groups, hoverGroup, active, i -> false, gap, fadeIn);

		// The hub: the block itself.
		rings.ring(cx, cy, 0, hub, (a, outer) -> Rings.fade(hoverCentre ? Rings.mix(HUB, GLOW_IN, outer ? 0.9 : 0.4) : HUB, fadeIn));
		brass(hub - 2 * s, hub, fadeIn);

		// Name plate, under the wheel: what you're pointing at, and what letting go does.
		String name, line;
		String take = sticky ? "Click to take it" : "Let go to take it";
		if (hoverOption >= 0) {
			Wheel.Option o = opts.get(hoverOption);
			name = o.name();
			line = o.note() != null ? o.note() : o.current() ? (blocks() ? "In your hand now" : "Chosen now") : blocks() ? take
					: sticky ? "Click to choose it" : "Let go to choose it";
		} else if (hoverCentre) {
			name = wheel.centre().name();
			line = wheel.centre().note() != null ? wheel.centre().note() : take;
		} else if (hoverGroup >= 0) {
			Wheel.Group grp = wheel.groups().get(hoverGroup);
			int n = grp.options().size();
			name = grp.name();
			line = n + (blocks() ? (n == 1 ? " shape" : " shapes") : (n == 1 ? " choice" : " choices")) + " on the outer ring";
		} else {
			name = wheel.title();
			line = blocks() ? "Point at a shape" : "Point at a setting";
		}
		int tw = Math.max(font.width(name), font.width(line)) + 16, plateH = 26;
		float py = cy + out1() + 6 * s + 2;
		rings.rect(cx - tw / 2f - 1, py - 1, cx + tw / 2f + 1, py + plateH + 1, Rings.fade(BRASS, fadeIn), Rings.fade(BRASS_DARK, fadeIn));
		rings.rect(cx - tw / 2f, py, cx + tw / 2f, py + plateH, Rings.fade(0xE8221F1A, fadeIn), Rings.fade(0xE8141210, fadeIn));
		rings.submit(g);

		// Icons and words.
		for (int i = 0; i < groups; i++) {
			Wheel.Group grp = wheel.groups().get(i);
			double a = i * 2 * Math.PI / groups;
			float r = (in0 + in1) / 2;
			draw(g, grp.icon(), grp.word(), cx + (float) Math.sin(a) * r, cy - (float) Math.cos(a) * r, 1.15f * s, i == hoverGroup);
		}
		for (int i = 0; i < opts.size(); i++) {
			Wheel.Option o = opts.get(i);
			double a = i * 2 * Math.PI / opts.size();
			float r = (out0 + out1) / 2;
			draw(g, o.icon(), o.word(), cx + (float) Math.sin(a) * r, cy - (float) Math.cos(a) * r, (i == hoverOption ? 1.5f : 1.3f) * s, i == hoverOption);
		}
		if (!wheel.icon().isEmpty()) draw(g, wheel.icon(), null, cx, cy, (hoverCentre ? 2.2f : 2f) * s, hoverCentre);
		g.drawCenteredString(font, name, (int) cx, (int) py + 4, TEXT);
		g.drawCenteredString(font, line, (int) cx, (int) py + 15, TEXT_DIM);
		String keyName = key.getTranslatedKeyMessage().getString();
		String hint = sticky ? "Click to choose · right-click or " + keyName + " to close"
				: (blocks() ? "Point at a shape, then let go of " : "Point at a setting, then let go of ") + keyName + " · scroll for groups · 1 to 9";
		g.drawCenteredString(font, hint, (int) cx, 8, Rings.fade(TEXT_DIM, open()));
	}

	/**
	 * One ring of slices. The slice you point at gets a bright edge with its glow inside it, so the highlight
	 * never spills past its own slice; the open group is warmer; the shape in your hand has a teal edge.
	 */
	private void slices(float r0, float r1, int n, int hovered, int active, IntPredicate current, float gap, float fade) {
		float g = n > 1 ? gap : 0, edge = 1.5f * s;
		for (int i = 0; i < n; i++) {
			double a0 = (i - 0.5) * 2 * Math.PI / n, a1 = (i + 0.5) * 2 * Math.PI / n;
			if (i == hovered) {
				rings.slice(cx, cy, r0, r1, a0, a1, g, (a, outer) -> Rings.fade(outer ? BRASS_LIGHT : BRASS, fade));
				rings.slice(cx, cy, r0 + edge, r1 - edge, a0, a1, g + 2 * edge, (a, outer) -> Rings.fade(outer ? GLOW_OUT : GLOW_IN, fade));
			} else {
				int in = i == active ? ACTIVE_IN : SLATE, out = i == active ? ACTIVE_OUT : SLATE_EDGE;
				rings.slice(cx, cy, r0, r1, a0, a1, g, (a, outer) -> Rings.fade(outer ? out : in, fade));
			}
			if (current.test(i)) rings.slice(cx, cy, r1 - 3 * s, r1, a0, a1, g, (a, outer) -> Rings.fade(CURRENT, fade * 0.9));
		}
	}

	/** A metal rim: bright at the top where it catches the light, darker underneath. */
	private void brass(float r0, float r1, float fadeIn) {
		rings.ring(cx, cy, r0, r1, (a, outer) -> {
			double light = 0.5 + 0.5 * Math.cos(a - 0.5);
			int c = light > 0.5 ? Rings.mix(BRASS, BRASS_LIGHT, (light - 0.5) * 2) : Rings.mix(BRASS_DARK, BRASS, light * 2);
			return Rings.fade(c, fadeIn);
		});
	}

	private void draw(GuiGraphics g, ItemStack icon, String word, float x, float y, float size, boolean lit) {
		if (icon != null && !icon.isEmpty()) {
			g.pose().pushMatrix();
			g.pose().translate(x, y);
			g.pose().scale(size, size);
			g.renderItem(icon, -8, -8);
			g.pose().popMatrix();
		} else if (word != null) {
			g.drawCenteredString(font, word, (int) x, (int) y - 4, lit ? 0xFFFFFFFF : TEXT);
		}
	}

	private void choose() {
		Runnable action = null;
		if (hoverOption >= 0 && hoverOption < options().size()) action = options().get(hoverOption).action();
		else if (hoverCentre && wheel.centre() != null) action = wheel.centre().action();
		onClose();
		if (action != null) action.run();
	}

	/** Whether the wheel's key is still held (a key on the keyboard; a mouse button can't be checked). */
	private boolean held() {
		InputConstants.Key bound = KeyBindingHelper.getBoundKeyOf(key);
		if (bound.getType() != InputConstants.Type.KEYSYM) return true;
		return InputConstants.isKeyDown(minecraft.getWindow(), bound.getValue());
	}

	private void released() {
		boolean quick = System.currentTimeMillis() - opened < 250;
		if (quick && hoverOption < 0 && !hoverCentre) {
			// A tap: stay open to be clicked.
			sticky = true;
			return;
		}
		choose();
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
		// The number keys 1 to 9 (GLFW key codes 49 to 57).
		if (k >= 49 && k <= 57) {
			int i = k - 49;
			if (i < options().size()) {
				hoverOption = i;
				hoverCentre = false;
				choose();
			}
			return true;
		}
		if (sticky && key.matches(event)) {
			onClose();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == 0) {
			hover((int) event.x(), (int) event.y());
			if (hoverGroup >= 0 && hoverOption < 0) {
				active = hoverGroup;
				return true;
			}
			choose();
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
		int n = wheel.groups().size();
		if (n > 0 && vertical != 0) active = Math.floorMod(active + (vertical < 0 ? 1 : -1), n);
		return true;
	}
}
