package com.insidethecube.retromon.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BannerRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * (0.57, 16:23: "it's like 2 banners, one on top of the other to make it longer ... update those and make them look a
 * little bit better") John's banners hung two to a pole (Ecruteak's posts, the NATIONAL PARK's): in a Retromon save,
 * two of the same banner one above the other are one long banner, the top one drawn half as long again and the one under
 * it not drawn, so there's one cloth from the bar down, not two bars and a doubled cloth.
 */
public class LongBanners extends BannerRenderer {
	public LongBanners(BlockEntityRendererProvider.Context ctx) {
		super(ctx);
	}

	private static boolean same(BlockState a, BlockState b) {
		return a.getBlock() == b.getBlock() && a.getValues().equals(b.getValues());
	}

	@Override
	public void submit(BannerRenderState s, PoseStack pose, SubmitNodeCollector out, CameraRenderState camera) {
		var level = Minecraft.getInstance().level;
		if (!Adventure.inSave || level == null || s.blockPos == null || s.blockState == null) {
			super.submit(s, pose, out, camera);
			return;
		}
		BlockPos at = s.blockPos;
		// The lower of the two: its cloth is the top one's.
		if (same(level.getBlockState(at.above()), s.blockState)) return;
		if (!same(level.getBlockState(at.below()), s.blockState)) {
			super.submit(s, pose, out, camera);
			return;
		}
		// The top one: stretched down from its bar over the both of them.
		pose.pushPose();
		pose.translate(0, 1, 0);
		pose.scale(1, 1.55F, 1);
		pose.translate(0, -1, 0);
		super.submit(s, pose, out, camera);
		pose.popPose();
	}
}
