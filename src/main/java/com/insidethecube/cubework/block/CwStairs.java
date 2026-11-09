package com.insidethecube.cubework.block;

import com.insidethecube.cubework.Material;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.StairsBlock;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;

/** The vanilla StairsBlock in a Cubework material: placed, connected and used exactly like vanilla's. */
public class CwStairs extends StairsBlock implements CwShape {
	private final Material material;

	public CwStairs(Material material, BlockState base, AbstractBlock.Settings settings) {
		super(base, settings);
		this.material = material;
	}

	@Override
	public Material material() {
		return material;
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		return Shared.placed(material, super.getPlacementState(ctx), ctx);
	}

	@Override
	protected BlockState getStateForNeighborUpdate(BlockState state, WorldView world, ScheduledTickView tickView, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighborState, Random random) {
		return Shared.updated(material, super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random), world, pos);
	}

	@Override
	protected boolean isSideInvisible(BlockState state, BlockState stateFrom, Direction direction) {
		return Shared.hidesFace(material, state, stateFrom, direction) || super.isSideInvisible(state, stateFrom, direction);
	}
}
