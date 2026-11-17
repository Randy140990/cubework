package com.insidethecube.kantojohto.client;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Over the world in the adventure: no hotbar, hearts or crosshair, and the "!" over a trainer who's spotted you. */
public final class Hud {
	private Hud() {}

	public static void register() {
		Identifier[] hidden = {VanillaHudElements.CROSSHAIR, VanillaHudElements.HOTBAR, VanillaHudElements.ARMOR_BAR, VanillaHudElements.HEALTH_BAR,
				VanillaHudElements.FOOD_BAR, VanillaHudElements.AIR_BAR, VanillaHudElements.INFO_BAR, VanillaHudElements.EXPERIENCE_LEVEL,
				VanillaHudElements.HELD_ITEM_TOOLTIP, VanillaHudElements.STATUS_EFFECTS, VanillaHudElements.MOUNT_HEALTH};
		for (Identifier id : hidden) {
			HudElementRegistry.replaceElement(id, old -> (g, delta) -> {
				if (!Adventure.active()) old.render(g, delta);
			});
		}
		HudElementRegistry.addLast(com.insidethecube.kantojohto.RouteOne.id("exclaim"), Hud::exclaim);
	}

	/** The "!" bubble, over the trainer's head, where the camera sees it. */
	private static void exclaim(GuiGraphics g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		if (!Adventure.active() || BattleScene.active() || ClientState.exclaimTicks <= 0 || mc.level == null) return;
		Entity e = mc.level.getEntity(ClientState.exclaimNpc);
		double[] cam = Adventure.lastPose;
		if (e == null || cam == null) return;
		Vec3 p = e.position().add(0, 2.35, 0);
		double yaw = Math.toRadians(cam[3]), pitch = Math.toRadians(cam[4]);
		Vec3 f = new Vec3(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
		Vec3 r = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
		Vec3 u = r.cross(f);
		Vec3 v = p.subtract(cam[0], cam[1], cam[2]);
		double depth = v.dot(f);
		if (depth <= 0.1) return;
		int w = g.guiWidth(), h = g.guiHeight();
		double t = Math.tan(Math.toRadians(Adventure.FOV) / 2);
		double nx = v.dot(r) / depth / (t * w / h), ny = v.dot(u) / depth / t;
		int sx = (int) ((nx + 1) / 2 * w), sy = (int) ((1 - ny) / 2 * h);
		int s = Ui.scale(h), bw = 12 * s, bh = 14 * s;
		// Pops up, then stays.
		float pop = Math.min(1, (40 - ClientState.exclaimTicks) / 4F);
		int lift = (int) ((1 - pop) * 6 * s);
		Ui.box(g, sx - bw / 2, sy - bh - lift, bw, bh, s);
		Ui.centred(g, mc.font, "!", sx, sy - bh - lift + 3 * s, 0xFFD02020, s, false);
	}
}
