package com.insidethecube.orbitarena.client;

import com.insidethecube.orbitarena.match.Kit;
import com.insidethecube.orbitarena.match.MatchSettings;
import com.insidethecube.orbitarena.match.MatchSettings.Boss;
import com.insidethecube.orbitarena.match.MatchSettings.Opponents;
import com.insidethecube.orbitarena.net.Payloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * The arena's setup screen: who you fight, how well they fight, how many, your kit, a time limit and how
 * full the stands are. Start fades you into the arena.
 */
public class ArenaScreen extends Screen {
	private static Opponents opponents = Opponents.FIGHTERS;
	private static int difficulty = 3, count = 1, bossCount = 1, timeLimit = 0;
	private static Kit kit = Kit.SWORD;
	private static Boss boss = Boss.WITHER;
	private static final int[] TIMES = {0, 120, 300, 600};
	private static final float[] CROWDS = {0, 0.25F, 0.5F, 0.85F};

	private final Screen parent;
	private CycleButton<Boss> bossButton;
	private Slider countSlider;

	public ArenaScreen(Screen parent) {
		super(Component.literal("Orbit Arena"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int w = 220, x = this.width / 2 - w / 2, y = this.height / 2 - 92;
		addRenderableWidget(CycleButton.<Opponents>builder(o -> Component.literal(switch (o) {
					case FIGHTERS -> "Fighters (PvP)";
					case MOBS -> "Mob waves";
					case BOSS -> "Boss";
				}))
				.withValues(Opponents.values()).withInitialValue(opponents)
				.create(x, y, w, 20, Component.literal("Opponents"), (b, v) -> {
					opponents = v;
					rebuildWidgets();
				}));
		y += 24;
		addRenderableWidget(new Slider(x, y, w, (difficulty - 1) / 9.0, v -> difficulty = 1 + (int) Math.round(v * 9),
				() -> "Difficulty: " + difficulty + " · " + describe(difficulty)));
		y += 24;
		if (opponents == Opponents.BOSS) {
			bossButton = addRenderableWidget(CycleButton.<Boss>builder(b -> Component.literal(switch (b) {
						case WITHER -> "Wither";
						case WARDEN -> "Warden";
						case ENDER_DRAGON -> "Ender Dragon (experimental)";
					}))
					.withValues(Boss.values()).withInitialValue(boss)
					.create(x, y, w, 20, Component.literal("Boss"), (b, v) -> boss = v));
			y += 24;
			countSlider = addRenderableWidget(new Slider(x, y, w, (bossCount - 1) / 2.0, v -> bossCount = 1 + (int) Math.round(v * 2),
					() -> "How many: " + bossCount));
		} else {
			countSlider = addRenderableWidget(new Slider(x, y, w, Math.sqrt((count - 1) / 99.0), v -> count = 1 + (int) Math.round(v * v * 99),
					() -> "How many: " + count));
		}
		y += 24;
		addRenderableWidget(CycleButton.<Kit>builder(k -> Component.translatable(k.key()))
				.withValues(Kit.values()).withInitialValue(kit)
				.create(x, y, w, 20, Component.literal("Your kit"), (b, v) -> kit = v));
		y += 24;
		addRenderableWidget(CycleButton.<Integer>builder(t -> Component.literal(t == 0 ? "None" : (t / 60) + " minutes"))
				.withValues(0, 120, 300, 600).withInitialValue(timeLimit)
				.create(x, y, w, 20, Component.literal("Time limit"), (b, v) -> timeLimit = v));
		y += 24;
		addRenderableWidget(CycleButton.<Float>builder(c -> Component.literal(c == 0 ? "Empty" : c < 0.3F ? "Quiet" : c < 0.6F ? "Busy" : "Packed"))
				.withValues(0F, 0.25F, 0.5F, 0.85F).withInitialValue(nearestCrowd())
				.create(x, y, w, 20, Component.literal("Crowd"), (b, v) -> Crowd.density = v));
		y += 30;
		addRenderableWidget(Button.builder(Component.literal("Start"), b -> start()).bounds(x, y, w / 2 - 2, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Back"), b -> onClose()).bounds(x + w / 2 + 2, y, w / 2 - 2, 20).build());
	}

	private static Float nearestCrowd() {
		float best = CROWDS[0];
		for (float c : CROWDS) if (Math.abs(c - Crowd.density) < Math.abs(best - Crowd.density)) best = c;
		return best;
	}

	/** What a fighter does at this level. */
	private static String describe(int d) {
		return switch (d) {
			case 1, 2 -> "beginner";
			case 3 -> "strafes, blocks";
			case 4 -> "+ crits, eats";
			case 5 -> "+ axes your shield";
			case 6, 7 -> "+ sprint resets";
			case 8, 9 -> "netherite, quick";
			default -> "world class";
		};
	}

	private void start() {
		int n = opponents == Opponents.BOSS ? bossCount : count;
		ClientPlayNetworking.send(new Payloads.Start(new MatchSettings(opponents, difficulty, n, kit, boss, timeLimit)));
		this.minecraft.setScreen(null);
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(parent);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
		super.render(g, mouseX, mouseY, partial);
		g.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 112, 0xFFFFFFFF);
		g.drawCenteredString(this.font, Component.literal("Train against fighters, mobs or bosses in a space arena"), this.width / 2, this.height / 2 - 102, 0xFFA0B4FF);
	}

	/** A slider showing a value, with a function that sets it from 0 to 1. */
	private static final class Slider extends AbstractSliderButton {
		private final java.util.function.DoubleConsumer set;
		private final java.util.function.Supplier<String> label;

		Slider(int x, int y, int w, double value, java.util.function.DoubleConsumer set, java.util.function.Supplier<String> label) {
			super(x, y, w, 20, Component.empty(), Mth.clamp(value, 0, 1));
			this.set = set;
			this.label = label;
			updateMessage();
		}

		@Override
		protected void updateMessage() {
			if (label != null) setMessage(Component.literal(label.get()));
		}

		@Override
		protected void applyValue() {
			set.accept(this.value);
		}
	}
}
