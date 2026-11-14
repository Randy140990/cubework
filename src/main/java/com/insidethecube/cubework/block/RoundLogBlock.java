package com.insidethecube.cubework.block;

import net.minecraft.world.level.block.state.BlockBehaviour;
//#if MC >= 26.3
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
//#endif

/**
 * A round log or stem, 16, 12, 8 or 4 pixels across, along the axis of the face you click: bark round the
 * sides and the rings shrunk to fit the ends (drawn in code, client.RoundModel.buildLog). For masts, posts
 * and round beams. An axe strips it, as it strips logs.
 */
public class RoundLogBlock extends RoundColumnBlock {
	public RoundLogBlock(int width, BlockBehaviour.Properties settings) {
		super(null, width, settings);
	}

	//#if MC >= 26.3
	/** From 26.3 the game strips logs by data that a mod can't add to, so round logs strip themselves. */
	@Override
	protected net.minecraft.world.InteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState state, net.minecraft.world.level.Level level,
			BlockPos pos, net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
		Block stripped = com.insidethecube.cubework.Extras.STRIPPED.get(this);
		if (stripped == null || !stack.is(net.minecraft.tags.ItemTags.AXES)) return super.useItemOn(stack, state, level, pos, player, hand, hit);
		level.playSound(player, pos, net.minecraft.sounds.SoundEvents.AXE_STRIP.value(), net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
		if (!level.isClientSide()) {
			level.setBlock(pos, Shared.copy(state, stripped), Block.UPDATE_ALL_IMMEDIATE);
			stack.hurtAndBreak(1, player, hand);
		}
		return net.minecraft.world.InteractionResult.SUCCESS;
	}
	//#endif
}
