package com.insidethecube.cubework.block;

import com.insidethecube.cubework.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * A full block with a 1-pixel white border round its top and bottom edges. Stacked on the same block the
 * borders where they meet go, so a wall of them has one line at its very top and one at its very bottom.
 */
public class BorderedBlock extends CwBlock {
	public static final BooleanProperty BORDER_TOP = BooleanProperty.create("border_top");
	public static final BooleanProperty BORDER_BOTTOM = BooleanProperty.create("border_bottom");

	public BorderedBlock(Material material, BlockBehaviour.Properties settings) {
		super(material, settings);
		registerDefaultState(defaultBlockState().setValue(BORDER_TOP, true).setValue(BORDER_BOTTOM, true));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(BORDER_TOP, BORDER_BOTTOM);
	}

	private BlockState bordered(BlockState state, BlockGetter world, BlockPos pos) {
		return state.setValue(BORDER_TOP, !world.getBlockState(pos.above()).is(this)).setValue(BORDER_BOTTOM, !world.getBlockState(pos.below()).is(this));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		return placed(bordered(defaultBlockState(), ctx.getLevel(), ctx.getClickedPos()), ctx);
	}

	@Override
	protected BlockState shapeUpdate(BlockState state, LevelReader world, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState) {
		return direction.getAxis().isVertical() ? bordered(state, world, pos) : state;
	}
}
