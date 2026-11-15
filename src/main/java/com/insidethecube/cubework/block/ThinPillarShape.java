package com.insidethecube.cubework.block;

import com.insidethecube.cubework.Behaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** The 12, 8 and 4 pixel pillars of a material. */
public class ThinPillarShape extends ThinPillarBlock implements CwShape {
	private final Behaviour behaviour;

	public ThinPillarShape(Behaviour behaviour, int width, BlockBehaviour.Properties settings) {
		super(width, settings);
		this.behaviour = behaviour;
	}

	@Override
	public Behaviour behaviour() {
		return behaviour;
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
		Aging.tick(behaviour, state, world, pos, random);
	}
}
