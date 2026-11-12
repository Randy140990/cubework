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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The grappling hook. Hold use to aim and let go to fire: the hook flies up to 24 blocks, catches on the top
 * edge of a block and drops a rope down to you. Hold use while on a rope to winch yourself up (no fall
 * damage while winching). Sneak and use it on its rope to take the rope back.
 */
public class GrapplingHookItem extends Item {
	public GrapplingHookItem(Item.Properties props) {
		super(props);
	}

	/** Whether the player is on (or in) a rope. */
	static boolean onRope(LivingEntity e) {
		Level level = e.level();
		BlockPos feet = e.blockPosition();
		return level.getBlockState(feet).is(DK.ROPE) || level.getBlockState(feet.above()).is(DK.ROPE);
	}

	@Override
	public InteractionResult useOn(UseOnContext ctx) {
		Player player = ctx.getPlayer();
		if (player != null && player.isSecondaryUseActive() && ctx.getLevel().getBlockState(ctx.getClickedPos()).is(DK.ROPE)) {
			RopeBlock.windUp(ctx.getLevel(), ctx.getClickedPos(), player);
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		player.startUsingItem(hand);
		return InteractionResult.CONSUME;
	}

	@Override
	public ItemUseAnimation getUseAnimation(ItemStack stack) {
		return ItemUseAnimation.BOW;
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity entity) {
		return 72000;
	}

	@Override
	public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
		if (!onRope(entity)) return;
		// Winch up the rope.
		Vec3 v = entity.getDeltaMovement();
		entity.setDeltaMovement(v.x * 0.5, 0.22, v.z * 0.5);
		entity.resetFallDistance();
		BlockPos feet = entity.blockPosition();
		if (level.getBlockState(feet).is(DK.ROPE) && !level.getBlockState(feet.above()).is(DK.ROPE)) {
			// At the top: step over the edge onto the ledge.
			Direction wall = level.getBlockState(feet).getValue(RopeBlock.FACING);
			entity.setDeltaMovement(wall.getStepX() * 0.25, 0.45, wall.getStepZ() * 0.25);
		}
		if (!level.isClientSide() && (72000 - remaining) % 8 == 0) {
			level.playSound(null, entity.blockPosition(), SoundEvents.CHAIN_STEP, SoundSource.PLAYERS, 0.5f, 1.2f);
		}
	}

	@Override
	public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remaining) {
		if (onRope(entity) || !(entity instanceof Player player)) return false;
		int held = getUseDuration(stack, entity) - remaining;
		if (held < 8) return false;
		if (level instanceof ServerLevel server) {
			HookEntity hook = new HookEntity(server, player, stack.copyWithCount(1));
			hook.shootFromRotation(player, player.getXRot(), player.getYRot(), 0f, 2.2f, 0.4f);
			server.addFreshEntity(hook);
			level.playSound(null, player.blockPosition(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1f, 0.8f);
		}
		player.getCooldowns().addCooldown(stack, 20);
		return true;
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.literal("Hold use to aim, let go to fire (24 blocks)").withStyle(ChatFormatting.GRAY));
		tooltip.accept(Component.literal("On its rope: hold use to winch up").withStyle(ChatFormatting.GRAY));
		tooltip.accept(Component.literal("Sneak and use on its rope to take it back").withStyle(ChatFormatting.DARK_GRAY));
	}
}
