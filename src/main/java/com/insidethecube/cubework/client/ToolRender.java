package com.insidethecube.cubework.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import com.insidethecube.cubework.tool.BuildersGuideItem;
import com.insidethecube.cubework.tool.GuideShapes;
import com.insidethecube.cubework.tool.TerrainBrushItem;
import com.insidethecube.cubework.tool.ToolData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Draws the tools in the world: a builder's guide's outline (held, or pinned anywhere in the inventory) as
 * see-through ghost blocks, and the terrain brush's circle on the ground it would smooth.
 */
public final class ToolRender {
	private ToolRender() {}

	private static final VoxelShape GHOST = Shapes.box(0.12, 0.12, 0.12, 0.88, 0.88, 0.88);
	private static final VoxelShape MARK = Shapes.box(0.3, 0.3, 0.3, 0.7, 0.7, 0.7);
	private static final VoxelShape RING = Shapes.box(0.05, 0, 0.05, 0.95, 0.06, 0.95);
	private static final int GUIDE = 0xC8F2C46A, GUIDE_FAR = 0x80F2C46A, ANCHOR = 0xFF7FD3C7, BRUSH = 0xD07FD3C7;
	/** Ghost blocks drawn a frame at most, nearest first. */
	private static final int MAX_DRAWN = 12000;

	private static CompoundTag lastTag;
	private static List<BlockPos> lastBlocks = List.of();

	public static void register() {
		WorldRenderEvents.AFTER_ENTITIES.register(ToolRender::render);
	}

	private static void render(WorldRenderContext ctx) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) return;
		MultiBufferSource buffers = ctx.consumers();
		if (buffers == null) return;
		Vec3 cam = ctx.worldState().cameraRenderState.pos;
		PoseStack pose = ctx.matrices();
		VertexConsumer lines = buffers.getBuffer(RenderTypes.lines());

		// Guides: the one in hand, and any pinned in the inventory.
		List<ItemStack> guides = new ArrayList<>();
		ItemStack main = mc.player.getMainHandItem(), off = mc.player.getOffhandItem();
		if (main.getItem() instanceof BuildersGuideItem) guides.add(main);
		if (off.getItem() instanceof BuildersGuideItem) guides.add(off);
		for (int i = 0; i < mc.player.getInventory().getContainerSize() && guides.size() < 4; i++) {
			ItemStack s = mc.player.getInventory().getItem(i);
			if (s.getItem() instanceof BuildersGuideItem && s != main && s != off && ToolData.get(s).getBooleanOr(GuideShapes.PIN, false)) guides.add(s);
		}
		for (ItemStack g : guides) {
			CompoundTag tag = ToolData.get(g);
			// Before its middle is set, the held guide previews where you're looking.
			if (g == main && !tag.getBooleanOr("aset", false) && mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
				BlockPos at = hit.getBlockPos().relative(hit.getDirection());
				tag.putInt("ax", at.getX());
				tag.putInt("ay", at.getY());
				tag.putInt("az", at.getZ());
				tag.putBoolean("aset", true);
			}
			guide(pose, lines, cam, tag);
		}

		// The brush's circle, on the ground it would smooth.
		if (main.getItem() instanceof TerrainBrushItem && mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
			brush(pose, lines, cam, mc, hit.getBlockPos(), TerrainBrushItem.radius(main));
		}
	}

	private static void guide(PoseStack pose, VertexConsumer lines, Vec3 cam, CompoundTag tag) {
		List<BlockPos> blocks;
		if (Objects.equals(tag, lastTag)) {
			blocks = lastBlocks;
		} else {
			blocks = GuideShapes.blocks(tag);
			lastTag = tag;
			lastBlocks = blocks;
		}
		if (blocks.isEmpty()) return;
		BlockPos anchor = GuideShapes.anchor(tag, "a");
		double far2 = 48 * 48;
		int drawn = 0;
		// Nearest first, so a huge guide still shows where you're building.
		List<BlockPos> order = blocks.size() > MAX_DRAWN ? nearest(blocks, cam) : blocks;
		for (BlockPos b : order) {
			if (drawn++ >= MAX_DRAWN) break;
			double d2 = b.distToCenterSqr(cam);
			box(pose, lines, cam, b, GHOST, d2 > far2 ? GUIDE_FAR : GUIDE);
		}
		if (anchor != null) box(pose, lines, cam, anchor, MARK, ANCHOR);
		BlockPos end = GuideShapes.anchor(tag, "b");
		if (end != null) box(pose, lines, cam, end, MARK, ANCHOR);
	}

	private static List<BlockPos> nearest(List<BlockPos> blocks, Vec3 cam) {
		List<BlockPos> copy = new ArrayList<>(blocks);
		copy.sort((a, b) -> Double.compare(a.distToCenterSqr(cam), b.distToCenterSqr(cam)));
		return copy;
	}

	private static void brush(PoseStack pose, VertexConsumer lines, Vec3 cam, Minecraft mc, BlockPos centre, int radius) {
		for (int dx = -radius - 1; dx <= radius + 1; dx++) {
			for (int dz = -radius - 1; dz <= radius + 1; dz++) {
				double d = Math.sqrt(dx * dx + dz * dz);
				if (Math.abs(d - radius) > 0.5) continue;
				BlockPos top = surface(mc, centre.offset(dx, 0, dz), radius);
				if (top != null) box(pose, lines, cam, top.above(), RING, BRUSH);
			}
		}
	}

	/** The top solid block of a column near the middle's height. */
	private static BlockPos surface(Minecraft mc, BlockPos p, int radius) {
		for (int y = p.getY() + radius; y >= p.getY() - radius; y--) {
			BlockPos at = new BlockPos(p.getX(), y, p.getZ());
			BlockState s = mc.level.getBlockState(at);
			if (!s.isAir() && !s.canBeReplaced() && s.getFluidState().isEmpty()) return at;
		}
		return null;
	}

	private static void box(PoseStack pose, VertexConsumer lines, Vec3 cam, BlockPos at, VoxelShape shape, int colour) {
		ShapeRenderer.renderShape(pose, lines, shape, at.getX() - cam.x, at.getY() - cam.y, at.getZ() - cam.z, colour, 2.0f);
	}
}
