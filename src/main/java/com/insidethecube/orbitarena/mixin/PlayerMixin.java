package com.insidethecube.orbitarena.mixin;

import com.insidethecube.orbitarena.match.Matches;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fighting in Creative (Axiom's editor switches you to it): the arena's fighters, mobs and bosses ignore
 * anyone in Creative, so mid-fight the player counts as not in Creative for them (and is made hurtable in
 * Match). Everywhere else, and on your own screen, Creative is Creative.
 */
@Mixin(Player.class)
public class PlayerMixin {
	@Inject(method = "isCreative", at = @At("HEAD"), cancellable = true)
	private void orbitarena$fightAsSurvival(CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this instanceof ServerPlayer p && Matches.fightingInCreative(p)) cir.setReturnValue(false);
	}
}
