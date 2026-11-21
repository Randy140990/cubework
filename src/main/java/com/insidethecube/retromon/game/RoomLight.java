package com.insidethecube.retromon.game;

import java.util.List;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The light through a room's windows: the pools of sun (or moon) on its floor, the shafts down to them and its windows'
 * glass at dusk and at night, as the time of day has them. It only exists on your screen (put in the middle of the
 * room you're in, or of a battle's hall), and draws itself (see the client's RoomLightRenderer).
 */
public class RoomLight extends Entity {
	/** The room's pools and windows (as Interiors reads them), and where this stands in the room (its middle). */
	public List<double[]> pools = List.of(), panes = List.of();
	public double cx, cz;

	public RoomLight(EntityType<?> type, Level level) {
		super(type, level);
		this.noPhysics = true;
		this.noCulling = true;
		setNoGravity(true);
		setSilent(true);
		setInvulnerable(true);
	}

	@Override
	public void tick() {
		// (It stays where it's put.)
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
		return true;
	}
}
