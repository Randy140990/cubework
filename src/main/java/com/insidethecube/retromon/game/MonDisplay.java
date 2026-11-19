package com.insidethecube.retromon.game;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A Pokémon standing on the field in a battle. It only exists on your screen (the battle adds it and takes
 * it away), so it never touches the world: it just stands, idles, lunges, flinches and faints.
 */
public class MonDisplay extends Entity {
	public String species = "pidgey";
	/** Which way it faces (degrees, as entity yaw). */
	public float facing;
	/** Size, 1 normally: grows in when sent out, shrinks away when it faints or goes into a ball. */
	public float size = 1, sizeTarget = 1, sizeOld = 1;
	/** Ticks left of a lunge (attacking) and of flashing (hit). */
	public int lunge, flash;
	public static final int LUNGE = 14;
	/** The lunge: a dash in with a hop (a move that hits), or a hop where it stands (a move from afar). */
	public boolean dash = true;
	/** A one-off animation playing (cry, recoil, faint...) and ticks into it, or null. */
	public String action;
	public int actionAge;
	/** Tinted red (drawn into a ball as light) for so many ticks, or 0. */
	public int red;
	/** Glowing white (just out of its ball, in the white light) for so many ticks, or 0. */
	public int white;
	public static final int WHITE = 16;
	/** Fainting: ticks left (it sags, keels over and sinks a little), from FAINT. */
	public int faint;
	public static final int FAINT = 34;
	/** Celebrating a win: ticks left (hops, a twirl). */
	public int cheer;
	public static final int CHEER = 44;
	/** A Poké Ball's texture (which ball it is), or null for a Pokémon. */
	public String texture;
	/** Ticks left of a wobble (a ball shaking on the ground). */
	public int wobble;
	/** Flying from one place to another (a thrown ball): where from, where to, how long, how far along. */
	private double fromX, fromY, fromZ, toX, toY, toZ, arc;
	private int flight, flightLength;
	/** Its model on your screen (a client object, kept here so each Pokémon poses its own). */
	public Object model;

	public MonDisplay(EntityType<?> type, Level level) {
		super(type, level);
		this.noPhysics = true;
		setNoGravity(true);
		setSilent(true);
		setInvulnerable(true);
	}

	@Override
	public void tick() {
		// (The level counts its ticks and keeps where it was.)
		if (flight > 0) {
			flight--;
			double t = 1 - flight / (double) flightLength;
			double y = fromY + (toY - fromY) * t + arc * 4 * t * (1 - t);
			setPos(fromX + (toX - fromX) * t, y, fromZ + (toZ - fromZ) * t);
		}
		if (wobble > 0) wobble--;
		sizeOld = size;
		size += (sizeTarget - size) * 0.25F;
		if (Math.abs(sizeTarget - size) < 0.01F) size = sizeTarget;
		if (lunge > 0) lunge--;
		if (flash > 0) flash--;
		if (red > 0) red--;
		if (white > 0) white--;
		if (cheer > 0) cheer--;
		if (faint > 0) faint--;
		if (action != null && ++actionAge > 100) action = null;
	}

	/** Plays one of its one-off animations from the start (if its model has it). */
	public void act(String name) {
		action = name;
		actionAge = 0;
	}

	/** Throws it (a ball) from where it is to there, in an arc this high, over so many ticks. */
	public void fly(double x, double y, double z, double height, int ticks) {
		fromX = getX();
		fromY = getY();
		fromZ = getZ();
		toX = x;
		toY = y;
		toZ = z;
		arc = height;
		flight = flightLength = Math.max(1, ticks);
	}

	public boolean flying() {
		return flight > 0;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder b) {
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		return false;
	}

	@Override
	protected void readAdditionalSaveData(ValueInput in) {
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput out) {
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double d) {
		return d < 128 * 128;
	}
}
