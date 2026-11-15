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

	/** Short page names, for the tabs and the arrows. */
	private static String tab(Shape.Group g) {
		return switch (g) {
			case SLABS -> "Slabs";
			case STAIRS -> "Stairs";
			case PILLARS -> "Pillars";
			case CURVES -> "Curves";
			case WALLS -> "Walls";
			case WINDOWS -> "Windows";
			case SMALL -> "Small parts";
		};
	}

	/** Most pages hold up to 12; longer lists (patterns) go over more pages. */
	private static final int PER_PAGE = 12;

	private static void addPages(List<Wheel.Page> pages, String name, List<Wheel.Option> options) {
		if (options.isEmpty()) return;
		int n = (options.size() + PER_PAGE - 1) / PER_PAGE, each = (options.size() + n - 1) / n;
		for (int i = 0; i < n; i++) {
			List<Wheel.Option> part = options.subList(i * each, Math.min(options.size(), (i + 1) * each));
			pages.add(new Wheel.Page(n == 1 ? name : name + " " + (i + 1), part));
		}
	}

	private static Wheel shapes(ShapeSwap.Kind kind, ItemStack held, boolean creative) {
		Material m = kind.material();
		Map<Shape, Block> shapes = CwBlocks.shapesOf(m);
		Block full = m.base();
		String material = full == null ? m.id() : full.getName().getString();
		List<Wheel.Page> pages = new ArrayList<>();
		int start = 0, hot = -1;
		for (Shape.Group grp : Shape.Group.values()) {
			List<Wheel.Option> options = new ArrayList<>();
			for (Shape s : Shape.values()) {
				Block b = shapes.get(s);
				if (b == null || s.group() != grp) continue;
				if (s == kind.shape()) {
					start = pages.size();
					hot = options.size();
				}
				options.add(option(m, s, s == kind.shape(), note(kind, held, s, creative), material));
			}
			if (!options.isEmpty()) pages.add(new Wheel.Page(tab(grp), options));
		}
		// Patterns: the block and its patterned versions (full blocks).
		List<Wheel.Option> patterns = new ArrayList<>();
		String family = ShapeSwap.family(m);
		Material source = m.patterned() ? Material.byId(m.source()) : null;
		// The plain block's name, which patterns start with.
		String plain = source != null && source.exists() && source.base() != null ? source.base().getName().getString() : material;
		if (source != null && source.exists()) patterns.add(option(source, null, false, null, plain));
		for (Material p : Material.ALL) {
			if (!ShapeSwap.family(p).equals(family) || p == m && !m.patterned() || CwBlocks.shapesOf(p).isEmpty()) continue;
			Block b = p.base();
			if (b == null || b.asItem() == null) continue;
			patterns.add(option(p, null, p == m && kind.shape() == null, null, plain));
		}
		if (!patterns.isEmpty() && kind.shape() == null) {
			for (int i = 0; i < patterns.size(); i++) {
				if (!patterns.get(i).current()) continue;
				int before = pages.size();
				addPages(pages, "Patterns", patterns);
				int each = pages.get(before).options().size();
				start = before + i / each;
				hot = i % each;
				patterns = List.of();
				break;
			}
		}
		addPages(pages, "Patterns", patterns);
		Wheel.Option centre = null;
		if (full != null) {
			Note n = note(kind, held, null, creative);
			String detail = n != null ? n.text() : kind.shape() == null ? "In your hand now" : "Back to the full block";
			centre = Wheel.Option.item(material, material, new ItemStack(full), kind.shape() == null, detail, n != null && n.blocked(),
					() -> ClientPlayNetworking.send(new Payloads.PickShape(m.id(), "")));
		}
		return new Wheel(material, full == null ? ItemStack.EMPTY : new ItemStack(full), centre, pages, start, hot);
	}

	/** A shape's name for the middle: its title ("Pillar (12 px)" where the title is only its size). */
	private static String title(Shape s) {
		return s.title.startsWith("(") ? "Pillar " + s.title : s.title;
	}

	/** A shape's label on the ring: the size goes on a second line, and window frames go by their shape. */
	private static String label(Shape s) {
		String t = title(s);
		int open = t.indexOf(" (");
		String head = open > 0 && t.endsWith(")") ? t.substring(0, open) : t, tail = open > 0 && t.endsWith(")") ? t.substring(open + 2, t.length() - 1) : null;
		if (head.endsWith("Window Frame")) head = head.startsWith("Diamond") ? "Diamond" : head.startsWith("Round") ? "Round" : "Square";
		else if (head.equals("Round Column")) head = "Column";
		else if (head.equals("Round Pillar")) head = "Round";
		return tail == null ? head : head + "\n" + tail;
	}

	/** A choice of a shape (or, with no shape, of a full block: a pattern). */
	private static Wheel.Option option(Material m, Shape s, boolean current, Note note, String material) {
		ItemStack stack = ShapeSwap.stack(m, s);
		String shape = s == null ? "" : s.suffix;
		String name, label;
		if (s != null) {
			name = title(s);
			label = label(s);
		} else {
			name = stack.getHoverName().getString();
			// Patterns are named after their block ("Red Wool Checkers"): the ring shows what's after it.
			label = name.startsWith(material + " ") && name.length() > material.length() + 1 ? name.substring(material.length() + 1) : name;
		}
		String detail = note != null ? note.text() : current ? "In your hand now" : s == null ? "Full block" : material;
		return Wheel.Option.item(name, label, stack, current, detail, note != null && note.blocked(),
				() -> ClientPlayNetworking.send(new Payloads.PickShape(m.id(), shape)));
	}

	private record Note(String text, boolean blocked) {}

	/** In survival: what the stack in hand would make, or why it can't. */
	private static Note note(ShapeSwap.Kind kind, ItemStack held, Shape target, boolean creative) {
		if (creative || held.isEmpty()) return null;
		ShapeSwap.Kind have = ShapeSwap.of(held);
		if (have == null || have.material() != kind.material() || have.shape() == target) return null;
		int a = ShapeSwap.perBlock(have.shape()), b = ShapeSwap.perBlock(target), blocks = held.getCount() / a;
		if (blocks == 0) return new Note("Needs " + a + " in hand", true);
		int left = held.getCount() - blocks * a;
		return new Note(left > 0 ? (blocks * a) + " make " + (blocks * b) + ", " + left + " left over" : held.getCount() + " make " + (blocks * b), false);
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
				Wheel.Option.word("Fill", "Fill", "Slopes on the low side of each step", mode == TerrainBrushItem.FILL,
						set(TerrainBrushItem.MODE, TerrainBrushItem.FILL)),
				Wheel.Option.word("Carve", "Carve", "Cuts the top edge of each step", mode == TerrainBrushItem.CARVE,
						set(TerrainBrushItem.MODE, TerrainBrushItem.CARVE)));
		List<Wheel.Option> sizes = new ArrayList<>();
		for (int r : TerrainBrushItem.RADII) {
			sizes.add(Wheel.Option.word("Radius " + r, String.valueOf(r), r == radius ? "Chosen now" : "Blocks out from the middle", r == radius,
					set(TerrainBrushItem.RADIUS, r)));
		}
		List<Wheel.Option> slopes = new ArrayList<>();
		String[] what = {"Every step a 1 in 1 slope", "Long slopes on terraces 2 to 4 wide", "Long slopes wherever they fit"};
		for (int i = 0; i < 3; i++) {
			slopes.add(Wheel.Option.word(TerrainBrushItem.STEEP_NAMES[i], TerrainBrushItem.STEEP_NAMES[i], what[i], i == steep, set(TerrainBrushItem.STEEP, i)));
		}
		List<Wheel.Option> undo = List.of(Wheel.Option.word("Undo", "Undo", "Takes back the last stroke", false, act("undo")));
		List<Wheel.Page> pages = List.of(new Wheel.Page("Mode", modes), new Wheel.Page("Size", sizes), new Wheel.Page("Slopes", slopes),
				new Wheel.Page("Undo", undo));
		return new Wheel("Terrain Brush", held.copyWithCount(1), null, pages, 0, mode == TerrainBrushItem.CARVE ? 1 : 0);
	}

	private static Wheel guide(ItemStack held) {
		CompoundTag t = ToolData.get(held);
		int shape = GuideShapes.shape(held), r = GuideShapes.size(t), r2 = GuideShapes.size2(t), sides = GuideShapes.sides(t);
		List<Wheel.Option> shapes = new ArrayList<>();
		for (int i = 0; i < GuideShapes.NAMES.length; i++) {
			shapes.add(Wheel.Option.word(GuideShapes.NAMES[i], GuideShapes.NAMES[i], i == shape ? "Chosen now" : "Guide shape", i == shape,
					set(GuideShapes.SHAPE, i)));
		}
		List<Wheel.Option> size = new ArrayList<>();
		for (int d : new int[] {-10, -5, -1, 1, 5, 10}) {
			int to = Math.max(1, r + d);
			size.add(Wheel.Option.word("Radius " + to, (d > 0 ? "+" : "") + d, "From " + r + " to " + to, false, set(GuideShapes.SIZE, r + d)));
		}
		String second = shape == GuideShapes.CYLINDER ? "Height" : "Width";
		List<Wheel.Option> size2 = new ArrayList<>();
		for (int d : new int[] {-10, -5, -1, 1, 5, 10}) {
			int to = Math.max(1, r2 + d);
			size2.add(Wheel.Option.word(second + " " + to, (d > 0 ? "+" : "") + d, "From " + r2 + " to " + to, false, set(GuideShapes.SIZE2, r2 + d)));
		}
		List<Wheel.Option> poly = new ArrayList<>();
		for (int n = 3; n <= 12; n++) {
			poly.add(Wheel.Option.word(n + " sides", String.valueOf(n), n == sides ? "Chosen now" : "Sides of the polygon", n == sides, set(GuideShapes.SIDES, n)));
		}
		boolean pinned = t.getBooleanOr(GuideShapes.PIN, false);
		List<Wheel.Option> guide = List.of(
				Wheel.Option.word(pinned ? "Unpin" : "Pin", pinned ? "Unpin" : "Pin", pinned ? "Stop showing it when not held" : "Keep showing it when not held",
						pinned, set(GuideShapes.PIN, pinned ? 0 : 1)),
				Wheel.Option.word("Clear", "Clear", "Clears the guide", false, act("clear")));
		List<Wheel.Page> pages = new ArrayList<>();
		pages.add(new Wheel.Page("Shape", shapes));
		pages.add(new Wheel.Page("Radius " + r, size));
		if (shape == GuideShapes.ELLIPSE || shape == GuideShapes.CYLINDER) pages.add(new Wheel.Page(second + " " + r2, size2));
		if (shape == GuideShapes.POLYGON) pages.add(new Wheel.Page(sides + " sides", poly));
		pages.add(new Wheel.Page("Guide", guide));
		return new Wheel(GuideShapes.describe(t), held.copyWithCount(1), null, pages, 0, shape);
	}
}
