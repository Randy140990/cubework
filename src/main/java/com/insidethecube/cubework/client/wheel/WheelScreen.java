package com.insidethecube.cubework.client.wheel;

import java.util.List;
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
 * The look follows Shards of Arcadia's book: a dark slate wheel in brass fittings that catch the light at
 * the top, the chosen segment glowing warm, a name plate underneath.
 */
public final class WheelScreen extends Screen {
	private static final int SLATE = 0xE61A1C21, SLATE_EDGE = 0xF0282B32, HUB = 0xF2111215;
	private static final int BRASS_LIGHT = 0xFFF0D08A, BRASS = 0xFFB98D4E, BRASS_DARK = 0xFF6B4E2A;
	private static final int GLOW_IN = 0xF06E4A1C, GLOW_OUT = 0xF0D9A64C, ACTIVE_IN = 0xE0362A1A, ACTIVE_OUT = 0xE0574124;
	private static final int TEXT = 0xFFF2E6CC, TEXT_DIM = 0xFFA89A80, CURRENT = 0xFF7FD3C7;

	private final Wheel wheel;
	private final KeyMapping key;
	private final long opened = System.currentTimeMillis();
	private final Rings rings = new Rings();
	private int active, hoverGroup = -1, hoverOption = -1;
	private boolean hoverCentre, sticky;

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

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
		g.fillGradient(0, 0, width, height, 0x50000000, 0x78000000);
	}

	private float scale() {
		return Math.max(0.55f, Math.min(1.35f, Math.min(width, height) / 320f));
	}

	private float open() {
		float t = Math.min(1, (System.currentTimeMillis() - opened) / 140f);
		return 1 - (1 - t) * (1 - t) * (1 - t);
	}

	// Radii, before the opening animation: the hub, the inner ring and the outer ring.
	private float hub() {
		return 30 * scale();
	}

	private float in0() {
		return 34 * scale();
	}

	private float in1() {
		return 76 * scale();
	}

	private float out0() {
		return 82 * scale();
	}

	private float out1() {
		return 128 * scale();
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
		float cx = width / 2f, cy = height / 2f;
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
		float cx = width / 2f, cy = height / 2f, k = 0.86f + 0.14f * open(), fadeIn = open();
		float hub = hub() * k, in0 = in0() * k, in1 = in1() * k, out0 = out0() * k, out1 = out1() * k;
		int groups = wheel.groups().size();
		List<Wheel.Option> opts = options();

		// Outer ring: the active group's options.
		if (!opts.isEmpty()) {
			int n = opts.size();
			rings.ring(cx, cy, out0, out1, (a, outer) -> Rings.fade(outer ? SLATE_EDGE : SLATE, fadeIn));
			for (int i = 0; i < n; i++) {
				double a0 = (i - 0.5) * 2 * Math.PI / n, a1 = (i + 0.5) * 2 * Math.PI / n;
				if (i == hoverOption) {
					float grow = 4 * scale();
					rings.sector(cx, cy, out0, out1 + grow, a0, a1, (a, outer) -> Rings.fade(outer ? GLOW_OUT : GLOW_IN, fadeIn));
				} else if (opts.get(i).current()) {
					rings.sector(cx, cy, out1 - 3 * scale(), out1, a0, a1, (a, outer) -> Rings.fade(CURRENT, fadeIn * 0.9));
				}
				if (n > 1) rings.spoke(cx, cy, out0, out1, a0, Math.max(1, scale()), Rings.fade(BRASS_DARK, fadeIn));
			}
			brass(cx, cy, out0 - 1.5f * scale(), out0, fadeIn);
			brass(cx, cy, out1, out1 + 2 * scale(), fadeIn);
		}

		// Inner ring: the groups.
		if (groups > 0) {
			rings.ring(cx, cy, in0, in1, (a, outer) -> Rings.fade(outer ? SLATE_EDGE : SLATE, fadeIn));
			for (int i = 0; i < groups; i++) {
				double a0 = (i - 0.5) * 2 * Math.PI / groups, a1 = (i + 0.5) * 2 * Math.PI / groups;
				if (i == hoverGroup) rings.sector(cx, cy, in0, in1, a0, a1, (a, outer) -> Rings.fade(outer ? GLOW_OUT : GLOW_IN, fadeIn));
				else if (i == active) rings.sector(cx, cy, in0, in1, a0, a1, (a, outer) -> Rings.fade(outer ? ACTIVE_OUT : ACTIVE_IN, fadeIn));
				if (groups > 1) rings.spoke(cx, cy, in0, in1, a0, Math.max(1, scale()), Rings.fade(BRASS_DARK, fadeIn));
			}
			brass(cx, cy, in0 - 1.5f * scale(), in0, fadeIn);
			brass(cx, cy, in1, in1 + 2 * scale(), fadeIn);
		}

		// The hub: the block itself.
		rings.ring(cx, cy, 0, hub, (a, outer) -> Rings.fade(hoverCentre ? Rings.mix(HUB, GLOW_IN, outer ? 0.9 : 0.4) : HUB, fadeIn));
		brass(cx, cy, hub - 2 * scale(), hub, fadeIn);

		// Name plate.
		String name = hoverOption >= 0 ? opts.get(hoverOption).name() : hoverCentre ? wheel.centre().name()
				: hoverGroup >= 0 ? wheel.groups().get(hoverGroup).name() : wheel.title();
		String note = hoverOption >= 0 ? opts.get(hoverOption).note() : null;
		int tw = Math.max(font.width(name), note == null ? 0 : font.width(note)) + 16;
		float py = cy + out1 + 10 * scale() + 4;
		int plateH = note == null ? 16 : 26;
		rings.rect(cx - tw / 2f - 1, py - 1, cx + tw / 2f + 1, py + plateH + 1, Rings.fade(BRASS, fadeIn), Rings.fade(BRASS_DARK, fadeIn));
		rings.rect(cx - tw / 2f, py, cx + tw / 2f, py + plateH, Rings.fade(0xF0221F1A, fadeIn), Rings.fade(0xF0141210, fadeIn));
		rings.submit(g);

		// Icons and words.
		for (int i = 0; i < groups; i++) {
			Wheel.Group grp = wheel.groups().get(i);
			double a = i * 2 * Math.PI / groups;
			float r = (in0 + in1) / 2;
			draw(g, grp.icon(), grp.word(), cx + (float) Math.sin(a) * r, cy - (float) Math.cos(a) * r, 1.15f * scale(), i == hoverGroup);
		}
		for (int i = 0; i < opts.size(); i++) {
			Wheel.Option o = opts.get(i);
			double a = i * 2 * Math.PI / opts.size();
			float r = (out0 + out1) / 2 + (i == hoverOption ? 2 * scale() : 0);
			draw(g, o.icon(), o.word(), cx + (float) Math.sin(a) * r, cy - (float) Math.cos(a) * r, (i == hoverOption ? 1.55f : 1.3f) * scale(),
					i == hoverOption);
		}
		if (!wheel.icon().isEmpty()) draw(g, wheel.icon(), null, cx, cy, (hoverCentre ? 2.2f : 2f) * scale(), hoverCentre);
		g.drawCenteredString(font, name, (int) cx, (int) py + 4, TEXT);
		if (note != null) g.drawCenteredString(font, note, (int) cx, (int) py + 15, TEXT_DIM);
		String hint = sticky ? "Click to choose" : "Let go to choose · scroll for groups · 1 to 9";
		g.drawCenteredString(font, hint, (int) cx, (int) (cy - out1 - 16 * scale() - 6), Rings.fade(TEXT_DIM, open()));
	}

	/** A metal rim: bright at the top where it catches the light, darker underneath. */
	private void brass(float cx, float cy, float r0, float r1, float fadeIn) {
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
