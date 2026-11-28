package com.insidethecube.retromon.client;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * (0.73) The MAGNET TRAIN's ride (game/MagnetTrain.java): the camera follows the train along John's line, at the
 * adventure's own angle and distance, held between the station and the tunnel's mouth (so as the train goes into the
 * tunnel it's left looking at the mouth, and as it comes out of one it's there waiting). The MAGNET TRAIN's tune
 * plays while you ride.
 */
public final class Ride {
	private Ride() {}

	public static boolean on;
	/**
	 * (0.74) The tunnel's dark back (x) at this end, and which way past it the train's cut off (+1 east, -1 west; 0:
	 * none): the train goes into the dark and is gone, never out the other side (client/ClipConsumer).
	 */
	public static double wall;
	public static int dir;
	private static int train = -1;
	private static double x0, x1, start, y, z, follow = Double.NaN;

	public static void set(CompoundTag t) {
		boolean was = on;
		on = t.getBooleanOr("on", false);
		// The MAGNET TRAIN's own tune while you ride; the place's again once you're off.
		if (on && !was) Music.play("magnet_train");
		else if (!on && was) Music.resumePlace();
		train = t.getIntOr("train", -1);
		x0 = t.getDoubleOr("x0", 0);
		x1 = t.getDoubleOr("x1", 0);
		start = t.getDoubleOr("start", 0);
		y = t.getDoubleOr("y", 0);
		z = t.getDoubleOr("z", 0);
		wall = t.getDoubleOr("wall", 0);
		dir = on ? t.getIntOr("dir", 0) : 0;
		follow = Double.NaN;
	}

	/** Where the camera is and which way it looks (as Adventure.pose gives it). */
	static double[] pose(float partial) {
		Minecraft mc = Minecraft.getInstance();
		Entity e = mc.level == null ? null : mc.level.getEntity(train);
		double x = e != null ? Mth.lerp(partial, e.xo, e.getX()) : start;
		x = Math.max(x0, Math.min(x1, x));
		// (Eased a touch, so the world's position updates in steps don't shake it.)
		follow = Double.isNaN(follow) ? x : follow + (x - follow) * 0.35;
		double[] at = Adventure.camAt(new Vec3(follow, y + 1.2, z), Adventure.distance, Adventure.PITCH);
		return new double[] {at[0], at[1], at[2], Adventure.YAW, Adventure.PITCH};
	}
}
