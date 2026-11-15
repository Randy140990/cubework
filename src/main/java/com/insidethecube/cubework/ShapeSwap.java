package com.insidethecube.cubework;

import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import com.insidethecube.cubework.block.ShapeEntity;

/**
 * The shape wheel's swaps. In creative the held stack becomes the chosen shape. In survival a stack is
 * changed at stonecutter rates, in whole blocks' worth (64 stone bricks make 128 slabs, 128 slabs make 64
 * bricks back), and only between shapes of the same block, so nothing is gained or lost.
 */
public final class ShapeSwap {
	private ShapeSwap() {}

	/** What a block is to the wheel: its material and shape (null for the full block). */
	public record Kind(Material material, Shape shape) {}

	private static Map<Block, Kind> vanillaShapes;

	/** A stack of a Cubework shape, of a full block Cubework makes shapes of, or of a vanilla shape of one (stone brick stairs). */
	public static Kind of(ItemStack stack) {
		if (stack.getItem() instanceof ShapeItem si) return new Kind(si.material(stack), si.shape());
		return stack.getItem() instanceof BlockItem bi ? of(bi.getBlock()) : null;
	}

	/** The block placed at pos, as above. */
	public static Kind of(BlockGetter world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		CwBlocks.Info info = CwBlocks.info(state.getBlock());
		if (info != null) return new Kind(ShapeEntity.atOrDefault(world, pos, state), info.shape());
		return of(state.getBlock());
	}

	private static Kind of(Block b) {
		if (b == null) return null;
		Material m = CwBlocks.materialOf(b);
		if (m != null) return new Kind(m, null);
		return vanilla().get(b);
	}

	/** Vanilla's own stairs, slabs, walls and so on, as the Cubework shapes they match. */
	private static synchronized Map<Block, Kind> vanilla() {
		if (vanillaShapes != null) return vanillaShapes;
		Map<Block, Kind> out = new IdentityHashMap<>();
		Shape[] vanilla = {Shape.STAIRS, Shape.SLAB, Shape.WALL, Shape.FENCE, Shape.BUTTON, Shape.PRESSURE_PLATE, Shape.TRAPDOOR};
		for (Material m : Material.ALL) {
			if (m.patterned() || CwBlocks.shapesOf(m).isEmpty()) continue;
			for (Shape s : vanilla) {
				if (CwBlocks.get(m, s) == null) continue;
				for (String name : new String[] {m.stem() + "_" + s.suffix, m.id() + "_" + s.suffix}) {
					if (!Reg.exists(name)) continue;
					out.putIfAbsent(Reg.vanilla(name), new Kind(m, s));
				}
			}
		}
		vanillaShapes = out;
		return out;
	}

	/** How many of a shape one block makes (1 for the full block). */
	public static int perBlock(Shape s) {
		return s == null ? 1 : s.fromStonecutter;
	}

	/** A stack of a material's shape (the full block for no shape). */
	public static ItemStack stack(Material m, Shape s) {
		if (s == null) return m.base() == null ? ItemStack.EMPTY : new ItemStack(m.base());
		return CwBlocks.stack(m, s);
	}

	/** Patterns and the block they're drawn on are one family: their full blocks swap one for one. */
	public static String family(Material m) {
		return m.patterned() ? m.source() : m.id();
	}

	/** The server's side of a pick from the wheel: a material and a shape (null for the full block). */
	public static void pick(ServerPlayer player, Material material, Shape shape) {
		ItemStack target = stack(material, shape);
		if (target.isEmpty()) return;
		Kind want = new Kind(material, shape);
		ItemStack held = player.getMainHandItem();
		int max = target.getMaxStackSize();
		if (player.hasInfiniteMaterials()) {
			int count = held.isEmpty() ? 1 : Math.min(held.getCount(), max);
			player.setItemInHand(InteractionHand.MAIN_HAND, target.copyWithCount(Math.max(1, count)));
			return;
		}
		Kind have = of(held);
		if (have == null) {
			say(player, "Hold the block you want to change.");
			return;
		}
		boolean same = have.material() == want.material();
		boolean family = have.shape() == null && want.shape() == null && family(have.material()).equals(family(want.material()));
		if (!same && !family) {
			say(player, "In survival a stack can only change into shapes of the same block.");
			return;
		}
		if (same && have.shape() == want.shape()) return;
		int a = perBlock(have.shape()), b = perBlock(want.shape());
		int blocks = held.getCount() / a;
		if (blocks == 0) {
			say(player, "You need " + a + " of these to make one block's worth.");
			return;
		}
		int used = blocks * a, made = blocks * b, left = held.getCount() - used;
		ItemStack spare = left > 0 ? held.copyWithCount(left) : ItemStack.EMPTY;
		player.setItemInHand(InteractionHand.MAIN_HAND, target.copyWithCount(Math.min(made, max)));
		for (int rest = made - max; rest > 0; rest -= max) player.getInventory().placeItemBackInInventory(target.copyWithCount(Math.min(rest, max)));
		if (!spare.isEmpty()) player.getInventory().placeItemBackInInventory(spare);
	}

	private static void say(ServerPlayer player, String text) {
		player.displayClientMessage(Component.literal(text), true);
	}
}
