package com.insidethecube.orbitarena.block;

import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * A light strip: a glowing white core edged with a line of colour, joined up with the strips beside it so a
 * run of them (along, up, or a panel of them) reads as one light. The coloured line runs only round the
 * outside of the whole shape, never between two strips that touch. Each side knows whether a strip of the
 * same colour is next to it (as glass panes and fences do).
 */
public class LightStripBlock extends Block {
	public static final Map<Direction, BooleanProperty> JOINS = PipeBlock.PROPERTY_BY_DIRECTION;

	public LightStripBlock(Properties props) {
		super(props);
		BlockState s = stateDefinition.any();
		for (BooleanProperty p : JOINS.values()) s = s.setValue(p, false);
		registerDefaultState(s);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		for (BooleanProperty p : JOINS.values()) builder.add(p);
	}

	/** This strip joined to the strips round it in a world. */
	public BlockState joined(BlockGetter level, BlockPos pos) {
		BlockState s = defaultBlockState();
		for (Direction d : Direction.values()) s = s.setValue(JOINS.get(d), level.getBlockState(pos.relative(d)).is(this));
		return s;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return joined(context.getLevel(), context.getClickedPos());
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighbourPos,
			BlockState neighbour, RandomSource random) {
		return state.setValue(JOINS.get(direction), neighbour.is(this));
	}
}
