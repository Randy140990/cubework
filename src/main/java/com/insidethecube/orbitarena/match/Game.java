package com.insidethecube.orbitarena.match;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/** One player's game in the arena dimension: a match in the stadium (Match) or on the bridge (BridgeMatch). */
interface Game {
	ServerPlayer player();

	MatchSettings settings();

	int slot();

	void tick();

	boolean done();

	/** In the arena dimension, as far as the game is concerned (its things put away). */
	boolean inArena();

	/** The fight is on (things can be hurt). */
	boolean fighting();

	/** Over, waiting on the player's choice (go again, change the settings, or leave). */
	boolean awaitingChoice();

	/** Making way for another go straight after: tidy up, without sending the player home. */
	void retire();

	/** The player has gone (logged off): tidy up; their things come back when they're next in. */
	void abandon();

	/** The player chose to leave. */
	void forfeit();

	/** Fade out and go home. */
	void leave();

	/** Would this hurt be allowed (who can hurt whom)? */
	boolean allowDamage(LivingEntity victim, DamageSource source, float amount);

	/** After a hurt went through. */
	void afterDamage(LivingEntity victim, DamageSource source, float taken);

	/** Something of this game's would die: true lets it (false: the game deals with it, as a respawn or a loss). */
	boolean allowDeath(LivingEntity victim, DamageSource source);

	/** Something died. */
	void onDeath(LivingEntity e);

	/** Whether the player may break or place a block here (in survival). */
	boolean mayBreak(BlockPos pos);

	boolean mayPlace(BlockPos pos);

	/** Whether this game owns this position (it's in its arena or on its bridge). */
	boolean owns(BlockPos pos);
}
