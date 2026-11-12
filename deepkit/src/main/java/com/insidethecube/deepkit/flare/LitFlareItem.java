package com.insidethecube.deepkit.flare;

import java.util.function.Consumer;
import com.insidethecube.deepkit.DK;
import com.insidethecube.deepkit.lamp.LightPools;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A burning flare. Held in either hand it lights the area round you, with sparks and smoke. Use throws it;
 * sneak and use drops it at your feet. It goes out after 60 seconds, wherever it is.
 */
public class LitFlareItem extends Item {
	public final FlareColour colour;

	public LitFlareItem(FlareColour colour, Item.Properties props) {
		super(props);
		this.colour = colour;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (level instanceof ServerLevel server) {
			boolean drop = player.isSecondaryUseActive();
			FlareEntity flare = new FlareEntity(server, player, stack.copyWithCount(1));
			if (drop) {
				flare.setPos(player.getX(), player.getY() + 0.1, player.getZ());
				flare.setDeltaMovement(Vec3.ZERO);
			} else {
				flare.shootFromRotation(player, player.getXRot(), player.getYRot(), 0f, 1.1f, 1f);
				level.playSound(null, player.blockPosition(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.6f, 0.6f);
			}
			server.addFreshEntity(flare);
			stack.shrink(1);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
		long until = stack.getOrDefault(DK.BURN_UNTIL, 0L);
		if (level.getGameTime() >= until) {
			level.playSound(null, entity.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5f, 1.2f);
			stack.setCount(0);
			return;
		}
		boolean held = slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND;
		if (!held) return;
		long t = level.getGameTime();
		if (t % 2 == 0) {
			LightPools.light(level, BlockPos.containing(entity.getEyePosition()), 14);
			LightPools.light(level, entity.blockPosition(), 13);
		}
		if (t % 3 == 0) sparks(level, entity.getX(), entity.getEyeY() + 0.1, entity.getZ(), colour, entity.isInWater());
		if (t % 40 == 0) level.playSound(null, entity.blockPosition(), SoundEvents.FIRE_AMBIENT, SoundSource.PLAYERS, 0.5f, 1.6f);
	}

	/** The flare's sparks and smoke (bubbles under water). */
	static void sparks(ServerLevel level, double x, double y, double z, FlareColour colour, boolean water) {
		level.sendParticles(new DustParticleOptions(colour.rgb, 1.3f), x, y, z, 3, 0.06, 0.06, 0.06, 0.0);
		if (water) {
			level.sendParticles(ParticleTypes.BUBBLE, x, y, z, 2, 0.08, 0.05, 0.08, 0.02);
		} else {
			level.sendParticles(ParticleTypes.SMOKE, x, y + 0.1, z, 1, 0.03, 0.02, 0.03, 0.01);
			if (level.getRandom().nextFloat() < 0.3f) level.sendParticles(ParticleTypes.LAVA, x, y, z, 1, 0.02, 0.02, 0.02, 0.0);
		}
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.literal("Use to throw; sneak and use to drop it").withStyle(ChatFormatting.GRAY));
	}
}
