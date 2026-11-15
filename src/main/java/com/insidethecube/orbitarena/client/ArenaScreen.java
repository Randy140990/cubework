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
 * The arena's setup screen: what to play (fighters, mob waves, a boss, or The Bridge), how well they play,
 * how many (fighters; waves; bosses; players a team on the bridge), your kit, the bridge's obstacles, a time
 * limit, how full the stands are, and whether the fly-round plays first. Start fades you in.
 */
public class ArenaScreen extends Screen {
	private static Opponents opponents = Opponents.FIGHTERS;
	private static int difficulty = 3, count = 1, waves = 10, bossCount = 1, teamSize = 1, timeLimit = 0, bridgeTime = 900, bridgeKit = MatchSettings.CLASSIC;
	private static Kit kit = Kit.SWORD;
	private static Boss boss = Boss.WITHER;
	private static boolean obstacles, intro = true;
	private static final float[] CROWDS = {0, 0.25F, 0.5F, 0.85F};

	private final Screen parent;

	public ArenaScreen(Screen parent) {
		super(Component.literal("Orbit Arena"));
		this.parent = parent;
	}

	private int top() {
		return this.height / 2 - (opponents == Opponents.BRIDGE || opponents == Opponents.BOSS ? 104 : 92);
	}

	@Override
	protected void init() {
		int w = 220, x = this.width / 2 - w / 2, y = top();
		addRenderableWidget(CycleButton.<Opponents>builder(o -> Component.literal(switch (o) {
					case FIGHTERS -> "Fighters (PvP)";
					case MOBS -> "Mob waves";
					case BOSS -> "Boss";
					case BRIDGE -> "The Bridge";
				}), opponents)
				.withValues(Opponents.values())
				.create(x, y, w, 20, Component.literal("Play"), (b, v) -> {
					opponents = v;
					rebuildWidgets();
				}));
		y += 24;
		boolean bridge = opponents == Opponents.BRIDGE;
		addRenderableWidget(new Slider(x, y, w, (difficulty - 1) / 9.0, v -> difficulty = 1 + (int) Math.round(v * 9),
				() -> "Difficulty: " + difficulty + " · " + (bridge ? describeBridge(difficulty) : describe(difficulty))));
		y += 24;
		switch (opponents) {
			case BOSS -> {
				addRenderableWidget(CycleButton.<Boss>builder(b -> Component.literal(switch (b) {
							case WITHER -> "Wither";
							case WARDEN -> "Warden";
							case ENDER_DRAGON -> "Ender Dragon (experimental)";
						}), boss)
						.withValues(Boss.values())
						.create(x, y, w, 20, Component.literal("Boss"), (b, v) -> boss = v));
				y += 24;
				addRenderableWidget(new Slider(x, y, w, (bossCount - 1) / 2.0, v -> bossCount = 1 + (int) Math.round(v * 2), () -> "How many: " + bossCount));
			}
			case MOBS -> addRenderableWidget(new Slider(x, y, w, Math.sqrt((waves - 1) / 49.0), v -> waves = 1 + (int) Math.round(v * v * 49),
					() -> "Waves: " + waves + " (one after another)"));
			case BRIDGE -> addRenderableWidget(new Slider(x, y, w, (teamSize - 1) / 4.0, v -> teamSize = 1 + (int) Math.round(v * 4),
					() -> "Teams: " + teamSize + " v " + teamSize + (teamSize > 1 ? " (bots with you)" : "")));
			default -> addRenderableWidget(new Slider(x, y, w, Math.sqrt((count - 1) / 99.0), v -> count = 1 + (int) Math.round(v * v * 99),
					() -> "How many: " + count));
		}
		y += 24;
		if (bridge) {
			addRenderableWidget(CycleButton.<Integer>builder(k -> Component.literal(k == MatchSettings.CLASSIC ? "Classic (leather, iron sword)" : "Netherite"), bridgeKit)
					.withValues(MatchSettings.CLASSIC, MatchSettings.NETHERITE)
					.create(x, y, w, 20, Component.literal("Kit"), (b, v) -> bridgeKit = v));
			y += 24;
			addRenderableWidget(CycleButton.<Boolean>builder(o -> Component.literal(o ? "Copper" : "None"), obstacles)
					.withValues(false, true)
					.create(x, y, w, 20, Component.literal("Obstacles"), (b, v) -> obstacles = v));
			y += 24;
			addRenderableWidget(CycleButton.<Integer>builder(t -> Component.literal(t == 0 ? "None" : (t / 60) + " minutes"), bridgeTime)
					.withValues(0, 300, 600, 900)
					.create(x, y, w, 20, Component.literal("Time limit"), (b, v) -> bridgeTime = v));
		} else {
			addRenderableWidget(CycleButton.<Kit>builder(k -> Component.translatable(k.key()), kit)
					.withValues(Kit.values())
					.create(x, y, w, 20, Component.literal("Your kit"), (b, v) -> kit = v));
			y += 24;
			addRenderableWidget(CycleButton.<Integer>builder(t -> Component.literal(t == 0 ? "None" : (t / 60) + " minutes"), timeLimit)
					.withValues(0, 120, 300, 600)
					.create(x, y, w, 20, Component.literal("Time limit"), (b, v) -> timeLimit = v));
		}
		y += 24;
		addRenderableWidget(CycleButton.<Float>builder(c -> Component.literal(c == 0 ? "Empty" : c < 0.3F ? "Quiet" : c < 0.6F ? "Busy" : "Packed"), nearestCrowd())
				.withValues(0F, 0.25F, 0.5F, 0.85F)
				.create(x, y, w, 20, Component.literal("Crowd"), (b, v) -> Crowd.density = v));
		y += 24;
		addRenderableWidget(CycleButton.<Boolean>builder(o -> Component.literal(o ? "On" : "Off"), intro)
				.withValues(true, false)
				.create(x, y, w, 20, Component.literal("Fly-round first"), (b, v) -> intro = v));
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

	/** What a bridge bot does at this level. */
	private static String describeBridge(int d) {
		return switch (d) {
			case 1, 2 -> "learning to bridge";
			case 3 -> "+ defends, eats";
			case 4, 5 -> "+ bridges round you, walls";
			case 6, 7 -> "+ knocks you off";
			case 8, 9 -> "fast, reads attacks";
			default -> "pro";
		};
	}

	private void start() {
		ClientPlayNetworking.send(new Payloads.Start(settings()));
		this.minecraft.setScreen(null);
	}

	/** The settings as last chosen (what "Go again" uses). */
	static MatchSettings settings() {
		int n = switch (opponents) {
			case BOSS -> bossCount;
			case MOBS -> waves;
			case BRIDGE -> teamSize;
			default -> count;
		};
		int time = opponents == Opponents.BRIDGE ? bridgeTime : timeLimit;
		return new MatchSettings(opponents, difficulty, n, kit, boss, time, obstacles, intro, bridgeKit);
	}

	/** The settings in a line: "10 fighters · level 10 · Netherite kit". */
	static Component describeSettings() {
		if (opponents == Opponents.BRIDGE) {
			return Component.literal("The Bridge · " + teamSize + " v " + teamSize + " · level " + difficulty + " · "
					+ (bridgeKit == MatchSettings.CLASSIC ? "Classic" : "Netherite") + " kit" + (obstacles ? " · copper obstacles" : ""));
		}
		String who = switch (opponents) {
			case FIGHTERS -> count + (count == 1 ? " fighter" : " fighters");
			case MOBS -> waves + (waves == 1 ? " wave" : " waves") + " of mobs";
			default -> bossCount + " × " + switch (boss) {
				case WITHER -> "Wither";
				case WARDEN -> "Warden";
				case ENDER_DRAGON -> "Ender Dragon";
			};
		};
		return Component.literal(who + " · level " + difficulty + " · ").append(Component.translatable(kit.key())).append(" kit");
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(parent);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
		super.render(g, mouseX, mouseY, partial);
		int y = top();
		g.drawCenteredString(this.font, this.title, this.width / 2, y - 20, 0xFFFFFFFF);
		g.drawCenteredString(this.font, Component.literal(opponents == Opponents.BRIDGE ? "Cross the bridge and drop into their goal: first to five"
				: "Train against fighters, mobs or bosses in a space arena"), this.width / 2, y - 10, 0xFFA0B4FF);
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
