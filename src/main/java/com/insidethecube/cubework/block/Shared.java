package com.insidethecube.cubework.block;

import com.insidethecube.cubework.Behaviour;
import com.insidethecube.cubework.CwBlocks;
import com.insidethecube.cubework.Material;
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

	/** A shape about to be placed (its material is set just after, see ShapeEntity.applyImplicitComponents). */
	public static BlockState placed(Behaviour b, BlockState state, BlockPlaceContext ctx) {
		return state;
	}

	/** A neighbour changed: concrete powder sets when water reaches it. */
	public static BlockState updated(Behaviour b, BlockState state, BlockGetter world, BlockPos pos) {
		if (state == null || b != Behaviour.SOLID) return state;
		if (world.getBlockEntity(pos) instanceof ShapeEntity e) settle(world, pos, e);
		return state;
	}

	/** Concrete powder touching water sets into concrete of its colour. */
	static void settle(BlockGetter world, BlockPos pos, ShapeEntity e) {
		Material m = e.material();
		if (m == null || m.kind() != Material.Kind.POWDER) return;
		if (wet(e.getBlockState(), world, pos)) e.setMaterial(m.hardened());
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

	/** Whether the stack in hand is the same material as the shape being added to (slabs, layers). */
	public static boolean sameMaterial(BlockState state, BlockPlaceContext ctx) {
		Material here = ShapeEntity.atOrDefault(ctx.getLevel(), ctx.getClickedPos(), state);
		return here != null && here == CwBlocks.materialOf(ctx.getItemInHand());
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
	 * Whether the neighbour on side dir covers all of this shape's face there (glass shapes of the same glass
	 * hide the faces where they touch, like glass blocks do, so you don't see the inside walls through them).
	 */
	public static boolean covers(BlockState self, BlockState other, Direction dir) {
		VoxelShape mine = self.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).getFaceShape(dir);
		VoxelShape theirs = other.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).getFaceShape(dir.getOpposite());
		if (theirs.isEmpty()) return false;
		return !Shapes.joinIsNotEmpty(mine, theirs, BooleanOp.ONLY_FIRST);
	}
}
