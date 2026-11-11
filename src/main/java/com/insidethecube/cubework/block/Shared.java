package com.insidethecube.cubework.block;

import com.insidethecube.cubework.CwBlocks;
import com.insidethecube.cubework.Material;
import com.insidethecube.cubework.Reg;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** What every Cubework shape does the same way, whichever vanilla block it's built on. */
public final class Shared {
	private Shared() {}

	/** A shape just placed: concrete powder touching water sets into concrete straight away. */
	public static BlockState placed(Material m, BlockState state, BlockPlaceContext ctx) {
		if (state == null || m.kind() != Material.Kind.POWDER) return state;
		return wet(state, ctx.getLevel(), ctx.getClickedPos()) ? harden(state) : state;
	}

	/** A neighbour changed: concrete powder sets when water reaches it. */
	public static BlockState updated(Material m, BlockState state, BlockGetter world, BlockPos pos) {
		if (state == null || m.kind() != Material.Kind.POWDER) return state;
		return wet(state, world, pos) ? harden(state) : state;
	}

	private static boolean wet(BlockState state, BlockGetter world, BlockPos pos) {
		if (state.hasProperty(BlockStateProperties.WATERLOGGED) && state.getValue(BlockStateProperties.WATERLOGGED)) return true;
		if (world.getFluidState(pos).is(FluidTags.WATER)) return true;
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
		for (Direction d : Direction.values()) {
			p.setWithOffset(pos, d);
			if (world.getFluidState(p).is(FluidTags.WATER)) return true;
		}
		return false;
	}

	/** The concrete version of a concrete powder shape (or full block), keeping how it's placed. */
	public static BlockState harden(BlockState state) {
		Block b = state.getBlock();
		CwBlocks.Info info = CwBlocks.info(b);
		Block to;
		if (info != null) {
			Material hard = info.material().hardened();
			to = hard == null ? null : CwBlocks.get(hard, info.shape());
		} else {
			String path = Reg.idOf(b).getPath();
			to = path.endsWith("_concrete_powder") ? Reg.vanilla(path.substring(0, path.length() - "_powder".length())) : null;
		}
		return to == null ? state : copy(state, to);
	}

	/** The same placement (facing, layers, half...) on another block with the same properties. */
	public static BlockState copy(BlockState from, Block to) {
		BlockState s = to.defaultBlockState();
		for (Property<?> p : from.getProperties()) if (s.hasProperty(p)) s = with(s, from, p);
		return s;
	}

	private static <T extends Comparable<T>> BlockState with(BlockState to, BlockState from, Property<T> p) {
		return to.setValue(p, from.getValue(p));
	}

	/**
	 * Glass shapes of the same glass hide the faces where they touch, like glass blocks do, so you
	 * don't see the inside walls through them. A face hides only where the neighbour covers all of it.
	 */
	public static boolean hidesFace(Material m, BlockState self, BlockState other, Direction dir) {
		if (m == null || !m.glass()) return false;
		if (CwBlocks.materialOf(other.getBlock()) != m) return false;
		VoxelShape mine = self.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).getFaceShape(dir);
		VoxelShape theirs = other.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).getFaceShape(dir.getOpposite());
		if (theirs.isEmpty()) return false;
		return !Shapes.joinIsNotEmpty(mine, theirs, BooleanOp.ONLY_FIRST);
	}
}
