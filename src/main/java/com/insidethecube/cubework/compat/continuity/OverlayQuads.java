package com.insidethecube.cubework.compat.continuity;

import net.fabricmc.fabric.api.renderer.v1.mesh.QuadView;
import net.minecraft.world.level.block.state.BlockState;
import com.insidethecube.cubework.block.CwShape;

/**
 * Continuity draws an overlay (a grass edge spilling onto sand, say) as a whole block face. On a Cubework
 * shape that would float at the block's edge, so while an overlay is worked out for a shape's quad, the
 * quad is remembered here and the overlay takes its corners instead (the slab's top, the slope's surface).
 */
public final class OverlayQuads {
	private OverlayQuads() {}

	private static final ThreadLocal<QuadView> CURRENT = new ThreadLocal<>();

	public static boolean shape(BlockState state) {
		return state != null && state.getBlock() instanceof CwShape;
	}

	public static void begin(QuadView quad, BlockState state) {
		CURRENT.set(shape(state) ? quad : null);
	}

	public static void end() {
		CURRENT.remove();
	}

	/** The shape's quad being overlaid, or null. */
	public static QuadView current() {
		return CURRENT.get();
	}
}
