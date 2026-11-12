package com.insidethecube.cubework.tool;

import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;

/**
 * The builder's guide: see-through outlines of circles, ellipses, spheres, domes, arches, cylinders,
 * polygons and lines, to build along by hand. Use it on a block to set the middle (for a line: the start;
 * sneak and use for the end). Shape and size are on the shape wheel. It never places anything.
 */
public class BuildersGuideItem extends Item {
	public BuildersGuideItem(Item.Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext ctx) {
		if (!(ctx.getLevel() instanceof ServerLevel)) return InteractionResult.SUCCESS;
		BlockPos at = ctx.getClickedPos().relative(ctx.getClickedFace());
		ItemStack stack = ctx.getItemInHand();
		Player player = ctx.getPlayer();
		boolean end = player != null && player.isSecondaryUseActive() && GuideShapes.shape(stack) == GuideShapes.LINE;
		int facing = ctx.getHorizontalDirection().get2DDataValue();
		ToolData.update(stack, tag -> {
			String k = end ? "b" : "a";
			tag.putInt(k + "x", at.getX());
			tag.putInt(k + "y", at.getY());
			tag.putInt(k + "z", at.getZ());
			tag.putBoolean(k + "set", true);
			if (!end) tag.putInt(GuideShapes.FACING, facing);
		});
		if (player != null) {
			int n = GuideShapes.blocks(ToolData.get(stack)).size();
			player.displayClientMessage(Component.literal((end ? "Line end set. " : "Guide set. ") + GuideShapes.describe(ToolData.get(stack)) + ": " + n
					+ " blocks."), true);
		}
		return InteractionResult.SUCCESS_SERVER;
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.literal(GuideShapes.describe(ToolData.get(stack))).withStyle(net.minecraft.ChatFormatting.GRAY));
		tooltip.accept(Component.literal("Use on a block to set the middle; the shape wheel key changes it").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
	}
}
