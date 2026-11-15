package com.insidethecube.orbitarena.entity;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;

/** What a bridge bot needs to know about its game (match.BridgeMatch). */
public interface BridgeWorld {
	ServerLevel level();

	/** The map's centre: x, z, and the bridge's top (y). */
	int centreX();

	int centreZ();

	int y();

	/** The cages are open and play is on. */
	boolean playing();

	/** Players a team, and who's on each team (in play: not waiting to come back). */
	int teamSize();

	List<LivingEntity> enemiesOf(int team);

	List<LivingEntity> alliesOf(int team);

	/** How wide the bridge is (1 to 3). */
	int bridgeWidth();

	/** Whether a block may be placed here, and whether the block here was placed (so may be broken). */
	boolean buildable(BlockPos pos);

	boolean breakable(BlockPos pos);

	/** The block a team builds with. */
	BlockState blockFor(int team);
}
