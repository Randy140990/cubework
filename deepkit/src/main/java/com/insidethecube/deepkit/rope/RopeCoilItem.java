package com.insidethecube.deepkit.rope;

import java.util.function.Consumer;
import com.insidethecube.deepkit.DK;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A coil of 32 blocks of rope. Use it on the edge of a drop (the top of the edge block, or its side) and the
 * rope ties on and unrolls straight down. Use it on the bottom of a hanging rope to add another length.
 * Sneak and use it on a rope to wind the rope up.
 */
public class RopeCoilItem extends Item {
	public RopeCoilItem(Item.Properties props) {
		super(props);
	}

	@Override
	public InteractionResult useOn(UseOnContext ctx) {
		Level level = ctx.getLevel();
		BlockPos on = ctx.getClickedPos();
		Player player = ctx.getPlayer();
		BlockState clicked = level.getBlockState(on);
		if (clicked.is(DK.ROPE)) {
			if (player != null && player.isSecondaryUseActive()) {
				RopeBlock.windUp(level, on, player);
				return InteractionResult.SUCCESS;
			}
			if (!(level instanceof ServerLevel)) return InteractionResult.SUCCESS;
			// Add a length to the bottom.
			BlockPos bottom = Ropes.bottom(level, on);
			int n = Ropes.hang(level, bottom.below(), level.getBlockState(bottom).getValue(RopeBlock.FACING), Ropes.COIL, false, false,
					level.getMinY());
			return done(level, ctx, n, bottom.below());
		}
		Direction face = ctx.getClickedFace();
		Direction looking = ctx.getHorizontalDirection();
		BlockPos top;
		Direction wall;
		if (face.getAxis().isHorizontal()) {
			// The side of the edge block: hang in front of it.
			top = on.relative(face);
			wall = face.getOpposite();
		} else if (face == Direction.UP) {
			// The top of the edge block: hang over the side you're facing.
			top = on.relative(looking);
			wall = looking.getOpposite();
		} else {
			// The underside of a block: hang from the ceiling.
			top = on.below();
			wall = looking;
		}
		if (!(level instanceof ServerLevel)) return InteractionResult.SUCCESS;
		int n = Ropes.hang(level, top, wall, Ropes.COIL, true, false, level.getMinY());
		return done(level, ctx, n, top);
	}

	private static InteractionResult done(Level level, UseOnContext ctx, int n, BlockPos at) {
		Player player = ctx.getPlayer();
		if (n == 0) {
			if (player != null) player.displayClientMessage(Component.literal("No room for the rope there: it needs a clear drop."), true);
			return InteractionResult.FAIL;
		}
		level.playSound(null, at, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1f, 1f);
		if (player != null) {
			player.displayClientMessage(Component.literal("Rope: " + n + " blocks down. Sneak and use it to wind it up."), true);
			if (!player.hasInfiniteMaterials()) ctx.getItemInHand().shrink(1);
		}
		return InteractionResult.SUCCESS_SERVER;
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.literal("Use on the edge of a drop to hang 32 blocks of rope").withStyle(ChatFormatting.GRAY));
		tooltip.accept(Component.literal("Sneak and use on a rope to wind it up").withStyle(ChatFormatting.DARK_GRAY));
	}
}
