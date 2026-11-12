package com.insidethecube.deepkit.paint;

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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * A can of glow paint (64 marks). Use it on a block's face to paint the chosen mark; use it in the air to
 * choose the mark. On floors and ceilings arrows point the way you face; on walls they point to the edge
 * of the face nearest where you click.
 */
public class GlowPaintItem extends Item {
	/** Opens the mark picker (set by the client). */
	public static Runnable openPicker = () -> {};

	public final PaintColour colour;

	public GlowPaintItem(PaintColour colour, Item.Properties props) {
		super(props);
		this.colour = colour;
	}

	public static int mark(ItemStack stack) {
		return Marks.clamp(stack.getOrDefault(DK.MARK, 0));
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level.isClientSide()) openPicker.run();
		return InteractionResult.SUCCESS;
	}

	@Override
	public InteractionResult useOn(UseOnContext ctx) {
		Level level = ctx.getLevel();
		BlockPos on = ctx.getClickedPos();
		Direction face = ctx.getClickedFace();
		BlockState clicked = level.getBlockState(on);
		if (clicked.getBlock() instanceof GlowMarkBlock) {
			// Clicking a mark repaints it.
			Direction to = clicked.getValue(GlowMarkBlock.FACING);
			face = to.getOpposite();
			on = on.relative(to);
		}
		BlockPos at = on.relative(face);
		BlockState there = level.getBlockState(at);
		boolean water = there.is(Blocks.WATER) && there.getFluidState().isSource();
		if (!(there.isAir() || water || there.getBlock() instanceof GlowMarkBlock) || !GlowMarkBlock.paintable(level, on, face)) {
			return InteractionResult.FAIL;
		}
		if (!(level instanceof ServerLevel)) return InteractionResult.SUCCESS;
		ItemStack stack = ctx.getItemInHand();
		int rotation = rotation(ctx, face, on);
		BlockState mark = DK.MARKS[colour.ordinal()].defaultBlockState().setValue(GlowMarkBlock.FACING, face.getOpposite())
				.setValue(GlowMarkBlock.ROTATION, rotation).setValue(GlowMarkBlock.SYMBOL, mark(stack)).setValue(GlowMarkBlock.WATERLOGGED, water);
		level.setBlock(at, mark, net.minecraft.world.level.block.Block.UPDATE_ALL);
		level.playSound(null, at, SoundEvents.GLOW_INK_SAC_USE, SoundSource.BLOCKS, 0.8f, 1.2f);
		Player player = ctx.getPlayer();
		if (player != null && !player.hasInfiniteMaterials()) stack.hurtAndBreak(1, player, ctx.getHand());
		return InteractionResult.SUCCESS_SERVER;
	}

	/** Which way the mark is turned (0 up, 1 right, 2 down, 3 left as you look at it; on floors and ceilings, the way you face). */
	private static int rotation(UseOnContext ctx, Direction face, BlockPos on) {
		if (face.getAxis().isVertical()) {
			return switch (ctx.getHorizontalDirection()) {
				case NORTH -> 0;
				case EAST -> 1;
				case SOUTH -> 2;
				default -> 3;
			};
		}
		Vec3 hit = ctx.getClickLocation();
		Direction right = face.getCounterClockWise();
		double rx = hit.x - on.getX(), rz = hit.z - on.getZ();
		double u = Math.max(0, Math.min(1, switch (right) {
			case EAST -> rx;
			case WEST -> 1 - rx;
			case SOUTH -> rz;
			default -> 1 - rz;
		}));
		double v = Math.max(0, Math.min(1, hit.y - on.getY()));
		double up = 1 - v, down = v, left = u, rightEdge = 1 - u;
		double min = Math.min(Math.min(up, down), Math.min(left, rightEdge));
		if (min == up) return 0;
		if (min == rightEdge) return 1;
		if (min == down) return 2;
		return 3;
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.literal("Mark: " + Marks.TITLES[mark(stack)]).withStyle(ChatFormatting.GRAY));
		tooltip.accept(Component.literal("Use on a block to paint; use in the air to choose the mark").withStyle(ChatFormatting.DARK_GRAY));
	}
}
