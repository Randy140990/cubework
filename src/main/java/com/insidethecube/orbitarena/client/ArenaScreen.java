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
 * The arena's setup screen, in two tabs that keep the same layout whatever you pick:
 * <ul>
 * <li>Arena: fighters, mob waves or a boss in the stadium, how well they fight, how many (waves; which
 * boss), your kit and a time limit;</li>
 * <li>The Bridge: teams of 1 to 5 (bots fill both), blue or red, how well the bots play, the kit, how wide
 * the bridge is, copper obstacles and a time limit.</li>
 * </ul>
 * Both have the crowd and the fly-round. Start fades you in.
 */
public class ArenaScreen extends Screen {
	private static final int ARENA = 0, BRIDGE = 1;
	private static int tab = ARENA;
	private static Opponents opponents = Opponents.FIGHTERS;
	private static int difficulty = 3, count = 1, waves = 10, bossCount = 1, timeLimit = 0;
	private static int bridgeDifficulty = 5, teamSize = 1, bridgeTime = 900, bridgeKit = MatchSettings.CLASSIC, bridgeWidth = 2, bridgeTeam = 0;
	private static Kit kit = Kit.SWORD;
	private static Boss boss = Boss.WITHER;
	private static boolean obstacles, intro = true;
	private static final float[] CROWDS = {0, 0.25F, 0.5F, 0.85F};
	private static final int COL = 150, GAP = 4, ROW = 24;

	private final Screen parent;
	private int left, top, tabsY;

	public ArenaScreen(Screen parent) {
		super(Component.literal("Orbit Arena"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int w = 2 * COL + GAP;
		left = this.width / 2 - w / 2;
		tabsY = Math.max(22, this.height / 2 - 108);
		top = tabsY + 34;
		int half = COL, right = left + COL + GAP;

		addRenderableWidget(Button.builder(Component.literal("Arena"), b -> {
			tab = ARENA;
			rebuildWidgets();
		}).bounds(left, tabsY, half, 20).build());
		addRenderableWidget(Button.builder(Component.literal("The Bridge"), b -> {
			tab = BRIDGE;
			rebuildWidgets();
		}).bounds(right, tabsY, half, 20).build());

		int y = top;
		if (tab == ARENA) {
			if (opponents == Opponents.BRIDGE) opponents = Opponents.FIGHTERS;
			addRenderableWidget(CycleButton.<Opponents>builder(o -> Component.literal(switch (o) {
						case FIGHTERS -> "Fighters";
						case MOBS -> "Mob waves";
						default -> "Boss";
					}), opponents)
					.withValues(Opponents.FIGHTERS, Opponents.MOBS, Opponents.BOSS)
					.create(left, y, w, 20, Component.literal("Play"), (b, v) -> {
						opponents = v;
						rebuildWidgets();
					}));
			y += ROW;
			addRenderableWidget(new Slider(left, y, w, (difficulty - 1) / 9.0, v -> difficulty = 1 + (int) Math.round(v * 9),
					() -> "Difficulty: " + difficulty + " · " + describe(difficulty)));
			y += ROW;
			switch (opponents) {
				case BOSS -> addRenderableWidget(new Slider(left, y, half, (bossCount - 1) / 2.0, v -> bossCount = 1 + (int) Math.round(v * 2), () -> "Bosses: " + bossCount));
				case MOBS -> addRenderableWidget(new Slider(left, y, half, Math.sqrt((waves - 1) / 49.0), v -> waves = 1 + (int) Math.round(v * v * 49),
						() -> "Waves: " + waves));
				default -> addRenderableWidget(new Slider(left, y, half, Math.sqrt((count - 1) / 99.0), v -> count = 1 + (int) Math.round(v * v * 99),
						() -> "Fighters: " + count));
			}
			CycleButton<Boss> bossButton = CycleButton.<Boss>builder(b -> Component.literal(switch (b) {
						case WITHER -> "Wither";
						case WARDEN -> "Warden";
						case ENDER_DRAGON -> "Ender Dragon";
					}), boss)
					.withValues(Boss.values())
					.create(right, y, half, 20, Component.literal("Boss"), (b, v) -> boss = v);
			bossButton.active = opponents == Opponents.BOSS;
			addRenderableWidget(bossButton);
			y += ROW;
			addRenderableWidget(CycleButton.<Kit>builder(k -> Component.translatable(k.key()), kit)
					.withValues(Kit.values())
					.displayOnlyValue()
					.create(left, y, half, 20, Component.literal("Kit"), (b, v) -> kit = v));
			addRenderableWidget(CycleButton.<Integer>builder(t -> Component.literal(t == 0 ? "None" : (t / 60) + " min"), timeLimit)
					.withValues(0, 120, 300, 600)
					.create(right, y, half, 20, Component.literal("Time limit"), (b, v) -> timeLimit = v));
		} else {
			addRenderableWidget(new Slider(left, y, half, (teamSize - 1) / 4.0, v -> teamSize = 1 + (int) Math.round(v * 4),
					() -> "Teams: " + teamSize + " v " + teamSize));
			addRenderableWidget(CycleButton.<Integer>builder(t -> t == 0 ? Component.literal("Blue").withColor(0x6F8BFF) : Component.literal("Red").withColor(0xFF6B6B), bridgeTeam)
					.withValues(0, 1)
					.create(right, y, half, 20, Component.literal("You're on"), (b, v) -> bridgeTeam = v));
			y += ROW;
			addRenderableWidget(new Slider(left, y, w, (bridgeDifficulty - 1) / 9.0, v -> bridgeDifficulty = 1 + (int) Math.round(v * 9),
					() -> "Bots: level " + bridgeDifficulty + " · " + describeBridge(bridgeDifficulty)));
			y += ROW;
			addRenderableWidget(CycleButton.<Integer>builder(k -> Component.literal(k == MatchSettings.CLASSIC ? "Classic" : "Netherite"), bridgeKit)
					.withValues(MatchSettings.CLASSIC, MatchSettings.NETHERITE)
					.create(left, y, half, 20, Component.literal("Kit"), (b, v) -> bridgeKit = v));
			addRenderableWidget(CycleButton.<Integer>builder(n -> Component.literal(n + (n == 1 ? " block" : " blocks")), bridgeWidth)
					.withValues(1, 2, 3)
					.create(right, y, half, 20, Component.literal("Bridge"), (b, v) -> bridgeWidth = v));
			y += ROW;
			addRenderableWidget(CycleButton.<Boolean>builder(o -> Component.literal(o ? "Copper" : "None"), obstacles)
					.withValues(false, true)
					.create(left, y, half, 20, Component.literal("Obstacles"), (b, v) -> obstacles = v));
			addRenderableWidget(CycleButton.<Integer>builder(t -> Component.literal(t == 0 ? "None" : (t / 60) + " min"), bridgeTime)
					.withValues(0, 300, 600, 900)
					.create(right, y, half, 20, Component.literal("Time limit"), (b, v) -> bridgeTime = v));
		}
		y += ROW;
		addRenderableWidget(CycleButton.<Float>builder(c -> Component.literal(c == 0 ? "Empty" : c < 0.3F ? "Quiet" : c < 0.6F ? "Busy" : "Packed"), nearestCrowd())
				.withValues(0F, 0.25F, 0.5F, 0.85F)
				.create(left, y, half, 20, Component.literal("Crowd"), (b, v) -> Crowd.density = v));
		addRenderableWidget(CycleButton.<Boolean>builder(o -> Component.literal(o ? "On" : "Off"), intro)
				.withValues(true, false)
				.create(right, y, half, 20, Component.literal("Fly-round"), (b, v) -> intro = v));
		y += ROW + 6;
		addRenderableWidget(Button.builder(Component.literal("Start"), b -> start()).bounds(left, y, half, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Back"), b -> onClose()).bounds(right, y, half, 20).build());
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
			default -> "pro: attacks and defends";
		};
	}

	private void start() {
		ClientPlayNetworking.send(new Payloads.Start(settings()));
		this.minecraft.setScreen(null);
	}

	/** The settings as last chosen (what "Go again" uses). */
	static MatchSettings settings() {
		if (tab == BRIDGE) {
			return new MatchSettings(Opponents.BRIDGE, bridgeDifficulty, teamSize, kit, boss, bridgeTime, obstacles, intro, bridgeKit, bridgeWidth, bridgeTeam);
		}
		Opponents o = opponents == Opponents.BRIDGE ? Opponents.FIGHTERS : opponents;
		int n = switch (o) {
			case BOSS -> bossCount;
			case MOBS -> waves;
			default -> count;
		};
		return new MatchSettings(o, difficulty, n, kit, boss, timeLimit, false, intro, bridgeKit, bridgeWidth, bridgeTeam);
	}

	/** The settings in a line: "10 fighters · level 10 · Netherite kit". */
	static Component describeSettings() {
		if (tab == BRIDGE) {
			return Component.literal("The Bridge · " + (bridgeTeam == 0 ? "blue" : "red") + " · " + teamSize + " v " + teamSize + " · level " + bridgeDifficulty + " · "
					+ (bridgeKit == MatchSettings.CLASSIC ? "Classic" : "Netherite") + " kit · " + bridgeWidth + " wide" + (obstacles ? " · copper obstacles" : ""));
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
		g.drawCenteredString(this.font, this.title, this.width / 2, tabsY - 14, 0xFFFFFFFF);
		// The chosen tab: a bar of light under it.
		int x = tab == ARENA ? left : left + COL + GAP;
		g.fill(x + 2, tabsY + 21, x + COL - 2, tabsY + 23, tab == ARENA ? 0xFF4AC8FF : 0xFF3DFF6A);
		g.drawCenteredString(this.font, Component.literal(tab == BRIDGE ? "Cross the bridge and drop into their goal: first to five"
				: "Fighters, mob waves or a boss in the stadium"), this.width / 2, tabsY + 25, 0xFFA0B4FF);
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
