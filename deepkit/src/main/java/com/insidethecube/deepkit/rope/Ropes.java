package com.insidethecube.deepkit.rope;

import com.insidethecube.deepkit.DK;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Hanging, extending and taking down ropes (a column of RopeBlock, its top tied with a knot or a hook). */
public final class Ropes {
	private Ropes() {}

	/** How much rope a coil holds, and how far a hook's rope reaches. */
	public static final int COIL = 32, HOOK_REACH = 24;

	private static boolean free(BlockState s) {
		return s.isAir() || (s.is(Blocks.WATER) && s.getFluidState().isSource());
	}

	/**
	 * Hangs rope from top downwards, against the wall on side `wall`, stopping at the first block in the way
	 * (or `max`, or above `stopAbove`). The top piece is tied (knot or hook) when `tied`. Returns how much hung.
	 */
	public static int hang(Level level, BlockPos top, Direction wall, int max, boolean tied, boolean hook, int stopAbove) {
		int n = 0;
		BlockPos.MutableBlockPos p = top.mutable();
		while (n < max && p.getY() >= stopAbove) {
			BlockState s = level.getBlockState(p);
			if (!free(s)) break;
			boolean water = !s.isAir();
			BlockState rope = DK.ROPE.defaultBlockState().setValue(RopeBlock.FACING, wall).setValue(RopeBlock.KNOT, tied && n == 0)
					.setValue(RopeBlock.HOOK, hook && n == 0).setValue(RopeBlock.WATERLOGGED, water);
			level.setBlock(p, rope, Block.UPDATE_ALL);
			n++;
			p.move(Direction.DOWN);
		}
		return n;
	}

	/** The top and bottom of the rope column through pos. */
	public static BlockPos top(Level level, BlockPos pos) {
		BlockPos.MutableBlockPos p = pos.mutable();
		while (level.getBlockState(p.above()).is(DK.ROPE)) p.move(Direction.UP);
		return p.immutable();
	}

	public static BlockPos bottom(Level level, BlockPos pos) {
		BlockPos.MutableBlockPos p = pos.mutable();
		while (level.getBlockState(p.below()).is(DK.ROPE)) p.move(Direction.DOWN);
		return p.immutable();
	}

	/**
	 * Takes the whole rope through pos down. A coil's rope gives back its coils (one a 32 blocks, rounded up)
	 * to the player, or drops them; a hook's rope gives nothing (the hook stays in your hand). `keep` is a
	 * position left for the game to remove itself (the block being broken), or null.
	 */
	public static void takeDown(Level level, BlockPos pos, Player player, BlockPos keep) {
		BlockPos top = top(level, pos), bottom = bottom(level, pos);
		boolean hook = level.getBlockState(top).getValue(RopeBlock.HOOK);
		int length = top.getY() - bottom.getY() + 1;
		for (BlockPos p = top; p.getY() >= bottom.getY(); p = p.below()) {
			if (p.equals(keep)) continue;
			BlockState s = level.getBlockState(p);
			level.setBlock(p, s.getValue(RopeBlock.WATERLOGGED) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		}
		if (hook || (player != null && player.hasInfiniteMaterials())) return;
		ItemStack coils = new ItemStack(DK.ROPE_COIL, (length + COIL - 1) / COIL);
		if (player != null) player.getInventory().placeItemBackInInventory(coils);
		else Block.popResource(level, pos, coils);
	}
}
