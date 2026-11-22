package com.insidethecube.retromon.game;

import com.insidethecube.retromon.Retromon;
import com.insidethecube.retromon.engine.Species;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A HeartGold mailbox: a red box on a silver post, by the front of a house (Pallet Town's two). The mod puts them
 * where the games' data says (mailboxes.tsv), in a Retromon save; it can't be broken.
 */
public class Mailbox extends Block {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	private static final VoxelShape ALONG_Z = Block.box(4, 0, 2, 12, 16, 14), ALONG_X = Block.box(2, 0, 4, 14, 16, 12);

	/** Where the mailboxes go, the way each one's slot faces, and whose house it is ("player", "rival" or words). */
	public record Spot(BlockPos pos, Direction facing, String owner) {}

	public static final List<Spot> SPOTS = new ArrayList<>();

	static {
		for (String[] r : Species.table("mailboxes.tsv")) {
			if (r.length < 4) continue;
			Direction d = Direction.byName(r[3].trim());
			SPOTS.add(new Spot(new BlockPos(Integer.parseInt(r[0].trim()), Integer.parseInt(r[1].trim()), Integer.parseInt(r[2].trim())),
					d == null || d.getAxis().isVertical() ? Direction.SOUTH : d, r.length > 4 ? r[4].trim() : ""));
		}
	}

	public Mailbox(BlockBehaviour.Properties p) {
		super(p);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
		b.add(FACING);
	}

	@Override
	protected VoxelShape getShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext c) {
		return s.getValue(FACING).getAxis() == Direction.Axis.X ? ALONG_X : ALONG_Z;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext c) {
		return getShape(s, level, pos, c);
	}

	/** The mailbox at a spot (within a block of it), or null. */
	public static Spot at(int x, int y, int z) {
		for (Spot sp : SPOTS) {
			if (Math.abs(sp.pos().getX() - x) <= 1 && Math.abs(sp.pos().getY() - y) <= 2 && Math.abs(sp.pos().getZ() - z) <= 1) return sp;
		}
		return null;
	}

	/**
	 * Puts the mailboxes in place where their part of the map is loaded: on empty ground (or where a red block marks
	 * the spot), never over anything else.
	 */
	public static void place(ServerLevel level) {
		for (Spot sp : SPOTS) {
			if (!level.hasChunkAt(sp.pos())) continue;
			BlockState now = level.getBlockState(sp.pos());
			BlockState want = Retromon.MAILBOX.defaultBlockState().setValue(FACING, sp.facing());
			if (now.is(Retromon.MAILBOX)) {
				if (now.getValue(FACING) != sp.facing()) level.setBlock(sp.pos(), want, 3);
				continue;
			}
			if (now.isAir() || now.is(Blocks.RED_WOOL) || now.is(Blocks.RED_CONCRETE) || now.is(Blocks.RED_TERRACOTTA)) level.setBlock(sp.pos(), want, 3);
		}
	}
}
