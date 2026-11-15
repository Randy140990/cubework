package com.insidethecube.cubework.client.wheel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.insidethecube.cubework.CwBlocks;
import com.insidethecube.cubework.Material;
import com.insidethecube.cubework.Reg;
import com.insidethecube.cubework.Shape;
import com.insidethecube.cubework.ShapeSwap;
import com.insidethecube.cubework.net.Payloads;
import com.insidethecube.cubework.tool.BuildersGuideItem;
import com.insidethecube.cubework.tool.GuideShapes;
import com.insidethecube.cubework.tool.TerrainBrushItem;
import com.insidethecube.cubework.tool.ToolData;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Builds the wheel for what the player holds (or, empty-handed in creative, looks at). */
public final class WheelModels {
	private WheelModels() {}

	public static Wheel forPlayer(Minecraft mc) {
		if (mc.player == null) return null;
		ItemStack held = mc.player.getMainHandItem();
		if (held.getItem() instanceof TerrainBrushItem) return brush(held);
		if (held.getItem() instanceof BuildersGuideItem) return guide(held);
		ShapeSwap.Kind kind = ShapeSwap.of(held);
		if (kind == null && (held.isEmpty() || mc.player.hasInfiniteMaterials()) && mc.hitResult instanceof BlockHitResult hit
				&& hit.getType() == HitResult.Type.BLOCK && mc.level != null) {
			kind = ShapeSwap.of(mc.level, hit.getBlockPos());
		}
		if (kind == null) return null;
		return shapes(kind, held, mc.player.hasInfiniteMaterials());
	}

	private static Wheel shapes(ShapeSwap.Kind kind, ItemStack held, boolean creative) {
		Material m = kind.material();
		Map<Shape, Block> shapes = CwBlocks.shapesOf(m);
		Block full = m.base();
		List<Wheel.Group> groups = new ArrayList<>();
		int start = 0;
		for (Shape.Group grp : Shape.Group.values()) {
			List<Wheel.Option> options = new ArrayList<>();
			for (Shape s : Shape.values()) {
				Block b = shapes.get(s);
				if (b == null || s.group() != grp) continue;
				if (s == kind.shape()) start = groups.size();
				options.add(option(m, s, s == kind.shape(), note(kind, held, s, creative)));
			}
			if (!options.isEmpty()) groups.add(new Wheel.Group(grp.title, new ItemStack(options.get(0).icon().getItem()), null, options));
		}
		// Patterns: the block and its patterned versions (full blocks).
		List<Wheel.Option> patterns = new ArrayList<>();
		String family = ShapeSwap.family(m);
		for (Material p : Material.ALL) {
			if (!ShapeSwap.family(p).equals(family) || p == m && !m.patterned() || CwBlocks.shapesOf(p).isEmpty()) continue;
			Block b = p.base();
			if (b == null || b.asItem() == null) continue;
			patterns.add(option(p, null, p == m && kind.shape() == null, null));
		}
		Material source = m.patterned() ? Material.byId(m.source()) : null;
		if (source != null && source.exists()) patterns.add(0, option(source, null, false, null));
		if (!patterns.isEmpty()) groups.add(new Wheel.Group("Patterns", new ItemStack(patterns.get(0).icon().getItem()), null, patterns));
		Wheel.Option centre = full == null ? null : option(m, null, kind.shape() == null, note(kind, held, null, creative));
		String title = full == null ? m.id() : full.getName().getString();
		return new Wheel(title, full == null ? ItemStack.EMPTY : new ItemStack(full), centre, groups, start);
	}

	private static Wheel.Option option(Material m, Shape s, boolean current, String note) {
		ItemStack stack = ShapeSwap.stack(m, s);
		String shape = s == null ? "" : s.suffix;
		return Wheel.Option.item(stack.getHoverName().getString(), stack, current, note,
				() -> ClientPlayNetworking.send(new Payloads.PickShape(m.id(), shape)));
	}

	/** In survival: what the stack in hand would make. */
	private static String note(ShapeSwap.Kind kind, ItemStack held, Shape target, boolean creative) {
		if (creative || held.isEmpty()) return null;
		ShapeSwap.Kind have = ShapeSwap.of(held);
		if (have == null || have.material() != kind.material() || have.shape() == target) return null;
		int a = ShapeSwap.perBlock(have.shape()), b = ShapeSwap.perBlock(target), blocks = held.getCount() / a;
		return blocks == 0 ? "Needs " + a + " in hand" : held.getCount() - blocks * a > 0 ? (blocks * a) + " make " + (blocks * b) + " (" + (held.getCount()
				- blocks * a) + " left over)" : held.getCount() + " make " + (blocks * b);
	}

	// ---------- tools ----------

	private static Runnable set(String key, int value) {
		return () -> ClientPlayNetworking.send(new Payloads.ToolSetting(key, value));
	}

	private static Runnable act(String action) {
		return () -> ClientPlayNetworking.send(new Payloads.ToolAction(action));
	}

	private static Wheel brush(ItemStack held) {
		int mode = TerrainBrushItem.mode(held), radius = TerrainBrushItem.radius(held), steep = TerrainBrushItem.steep(held);
		List<Wheel.Option> modes = List.of(
				Wheel.Option.word("Fill: slopes on the low side of each step", "Fill", mode == TerrainBrushItem.FILL, set(TerrainBrushItem.MODE, TerrainBrushItem.FILL)),
				Wheel.Option.word("Carve: cut the top edge of each step", "Carve", mode == TerrainBrushItem.CARVE,
						set(TerrainBrushItem.MODE, TerrainBrushItem.CARVE)));
		List<Wheel.Option> sizes = new ArrayList<>();
		for (int r : TerrainBrushItem.RADII) sizes.add(Wheel.Option.word("Radius " + r, String.valueOf(r), r == radius, set(TerrainBrushItem.RADIUS, r)));
		List<Wheel.Option> slopes = new ArrayList<>();
		String[] what = {"Steep: every step a 1 in 1 slope", "Natural: long slopes on terraces 2 to 4 wide", "Gentle: long slopes wherever they fit"};
		for (int i = 0; i < 3; i++) slopes.add(Wheel.Option.word(what[i], TerrainBrushItem.STEEP_NAMES[i], i == steep, set(TerrainBrushItem.STEEP, i)));
		List<Wheel.Option> undo = List.of(Wheel.Option.word("Undo the last stroke", "Undo", false, act("undo")));
		List<Wheel.Group> groups = List.of(new Wheel.Group("Mode", ItemStack.EMPTY, "Mode", modes), new Wheel.Group("Size", ItemStack.EMPTY, "Size", sizes),
				new Wheel.Group("Slopes", ItemStack.EMPTY, "Slopes", slopes), new Wheel.Group("Undo", ItemStack.EMPTY, "Undo", undo));
		return new Wheel("Terrain Brush", held.copyWithCount(1), null, groups, 0);
	}

	private static Wheel guide(ItemStack held) {
		CompoundTag t = ToolData.get(held);
		int shape = GuideShapes.shape(held), r = GuideShapes.size(t), r2 = GuideShapes.size2(t), sides = GuideShapes.sides(t);
		List<Wheel.Option> shapes = new ArrayList<>();
		for (int i = 0; i < GuideShapes.NAMES.length; i++) {
			shapes.add(Wheel.Option.word(GuideShapes.NAMES[i], GuideShapes.NAMES[i], i == shape, set(GuideShapes.SHAPE, i)));
		}
		List<Wheel.Option> size = new ArrayList<>();
		for (int d : new int[] {-10, -5, -1, 1, 5, 10}) {
			size.add(Wheel.Option.word("Radius " + Math.max(1, r + d), (d > 0 ? "+" : "") + d, false, set(GuideShapes.SIZE, r + d)));
		}
		String second = shape == GuideShapes.CYLINDER ? "Height" : "Width";
		List<Wheel.Option> size2 = new ArrayList<>();
		for (int d : new int[] {-10, -5, -1, 1, 5, 10}) {
			size2.add(Wheel.Option.word(second + " " + Math.max(1, r2 + d), (d > 0 ? "+" : "") + d, false, set(GuideShapes.SIZE2, r2 + d)));
		}
		List<Wheel.Option> poly = new ArrayList<>();
		for (int n = 3; n <= 12; n++) poly.add(Wheel.Option.word(n + " sides", String.valueOf(n), n == sides, set(GuideShapes.SIDES, n)));
		boolean pinned = t.getBooleanOr(GuideShapes.PIN, false);
		List<Wheel.Option> guide = List.of(
				Wheel.Option.word(pinned ? "Stop showing it when not held" : "Keep showing it when not held", pinned ? "Unpin" : "Pin", pinned,
						set(GuideShapes.PIN, pinned ? 0 : 1)),
				Wheel.Option.word("Clear the guide", "Clear", false, act("clear")));
		List<Wheel.Group> groups = new ArrayList<>();
		groups.add(new Wheel.Group("Shape", ItemStack.EMPTY, "Shape", shapes));
		groups.add(new Wheel.Group("Radius (" + r + ")", ItemStack.EMPTY, "Size", size));
		if (shape == GuideShapes.ELLIPSE || shape == GuideShapes.CYLINDER) groups.add(new Wheel.Group(second + " (" + r2 + ")", ItemStack.EMPTY, second, size2));
		if (shape == GuideShapes.POLYGON) groups.add(new Wheel.Group("Sides (" + sides + ")", ItemStack.EMPTY, "Sides", poly));
		groups.add(new Wheel.Group("Guide", ItemStack.EMPTY, "Guide", guide));
		return new Wheel(GuideShapes.describe(t), held.copyWithCount(1), null, groups, 0);
	}
}
