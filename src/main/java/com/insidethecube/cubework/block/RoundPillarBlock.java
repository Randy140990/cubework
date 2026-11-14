package com.insidethecube.cubework.block;

import com.insidethecube.cubework.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A big round pillar, 2, 3 or 4 blocks across: one item places a whole round layer (the block you aim at
 * is its corner nearest you; it spreads ahead of you and to your right), and stacking layers makes the
 * pillar. Each block is its piece of the circle (PART: x + size * z within the layer, from its north-west
 * corner). Breaking any piece takes the whole layer, which drops one item. Drawn smooth in code
 * (client.RoundPillarModel); the hit box follows the circle in 2-pixel steps.
 */
public class RoundPillarBlock extends CwBlock {
	public static final IntegerProperty PART = IntegerProperty.create("part", 0, 15);

	public final int size;
	private final VoxelShape[] shapes = new VoxelShape[16];

	public RoundPillarBlock(Material material, int size, BlockBehaviour.Properties settings) {
		super(material, settings);
		this.size = size;
		registerDefaultState(defaultBlockState().setValue(PART, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(PART);
	}

	/** Whether the point (x, z), in blocks from the layer's north-west corner, is inside the circle. */
	public boolean inside(double x, double z) {
		double r = size / 2.0, dx = x - r, dz = z - r;
		return dx * dx + dz * dz <= r * r;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		int part = state.getValue(PART);
		VoxelShape s = shapes[part];
		if (s == null) {
			int i = part % size, j = part / size;
			s = Shapes.empty();
			for (int a = 0; a < 8; a++) {
				for (int b = 0; b < 8; b++) {
					if (inside(i + (a + 0.5) / 8, j + (b + 0.5) / 8)) s = Shapes.or(s, Shapes.box(a / 8.0, 0, b / 8.0, (a + 1) / 8.0, 1, (b + 1) / 8.0));
				}
			}
			shapes[part] = s = s.optimize();
		}
		return s;
	}

	/** The layer's north-west corner when a layer is placed at the clicked spot by someone facing f. */
	private BlockPos corner(BlockPos clicked, Direction f) {
		Direction right = f.getClockWise();
		BlockPos far = clicked.relative(f, size - 1).relative(right, size - 1);
		return new BlockPos(Math.min(clicked.getX(), far.getX()), clicked.getY(), Math.min(clicked.getZ(), far.getZ()));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		BlockPos at = ctx.getClickedPos();
		BlockPos nw = corner(at, ctx.getHorizontalDirection());
		Level world = ctx.getLevel();
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				BlockPos p = nw.offset(i, 0, j);
				if (!world.getBlockState(p).canBeReplaced(ctx) || !world.getWorldBorder().isWithinBounds(p)) return null;
			}
		}
		return placed(defaultBlockState().setValue(PART, (at.getX() - nw.getX()) + size * (at.getZ() - nw.getZ())), ctx);
	}

	/** Places the rest of the layer round the piece just placed. */
	@Override
	public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(world, pos, state, placer, stack);
		if (world.isClientSide()) return;
		int part = state.getValue(PART);
		BlockPos nw = pos.offset(-(part % size), 0, -(part / size));
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				BlockPos p = nw.offset(i, 0, j);
				if (!p.equals(pos)) world.setBlock(p, state.setValue(PART, i + size * j), Block.UPDATE_ALL);
			}
		}
	}

	/** A piece whose neighbour in the layer has gone goes too (so breaking one piece takes the layer, dropping one item). */
	@Override
	protected BlockState shapeUpdate(BlockState state, LevelReader world, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState) {
		if (!direction.getAxis().isHorizontal()) return state;
		int part = state.getValue(PART), i = part % size + direction.getStepX(), j = part / size + direction.getStepZ();
		if (i < 0 || j < 0 || i >= size || j >= size) return state;
		boolean ok = neighborState.is(this) && neighborState.getValue(PART) == i + size * j;
		return ok ? state : Blocks.AIR.defaultBlockState();
	}
}
