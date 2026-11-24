package com.insidethecube.retromon.game;

import com.insidethecube.retromon.engine.Mon;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * (0.61, John's 18:04: "when I go up to the water ... if I hit space to interact, it should say about using surf")
 * SURF, as FireRed's: facing the water, A asks "The water is dyed a deep blue... Would you like to SURF?" if one of
 * your team knows SURF (and you've the badge for it: the SOULBADGE in Kanto, the FOGBADGE in Johto); YES and it carries
 * you out onto the water, riding on its back. The water's top is then something you stand on (a block's top, just over
 * the water's face); step back onto land and you're off its back.
 */
public final class Surf {
	private Surf() {
	}

	/** Who's surfing (the server's side) and on which POKéMON. */
	public static final Map<UUID, String> ON = new ConcurrentHashMap<>();
	/** The surfing POKéMON shown under each surfer. */
	private static final Map<UUID, Integer> MON = new ConcurrentHashMap<>();
	/** The client's own: whether you're surfing, and who you are there. */
	public static volatile boolean client;
	public static volatile UUID clientId;
	public static volatile String clientSpecies = "";

	/** What the water's top is while you surf: a block just over its face (the face is at 8/9 of a block). */
	public static final VoxelShape TOP = Shapes.box(0, 0, 0, 1, 0.9, 1);

	public static boolean on(Entity e) {
		if (!(e instanceof Player)) return false;
		if (e.level().isClientSide()) return client && e.getUUID().equals(clientId);
		return ON.containsKey(e.getUUID());
	}

	/** The water's top to stand on, for a surfer above a still water block with no water over it; or null. */
	public static VoxelShape shape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		if (!(ctx instanceof EntityCollisionContext ec) || !(ec.getEntity() instanceof Player pl) || !on(pl)) return null;
		var fluid = state.getFluidState();
		if (!fluid.is(FluidTags.WATER) || !fluid.isSource()) return null;
		if (level.getFluidState(pos.above()).is(FluidTags.WATER)) return null;
		return ctx.isAbove(TOP, pos, true) ? TOP : null;
	}

	/** One of your team who can SURF (knows it and isn't fainted), with the badge for it; or null. */
	public static Mon surfer(ServerPlayer p) {
		PlayerData d = Game.data(p);
		if (!d.badges.contains("SOULBADGE") && !d.badges.contains("FOGBADGE")) return null;
		for (Mon m : d.party) if (!m.fainted() && m.moves.contains("surf")) return m;
		return null;
	}

	/** A facing the water. */
	public static void ask(ServerPlayer p) {
		if (on(p) || Battles.in(p)) return;
		Mon m = surfer(p);
		if (m == null) {
			Game.dialogue(p, null, "talk", List.of("The water is dyed a deep blue..."));
			return;
		}
		CompoundTag c = new CompoundTag();
		net.minecraft.nbt.ListTag ch = new net.minecraft.nbt.ListTag();
		for (String s : List.of("YES", "NO")) ch.add(net.minecraft.nbt.StringTag.valueOf(s));
		c.put("choices", ch);
		Game.dialogue(p, null, "surf", List.of("The water is dyed a deep blue...", "Would you like to SURF?"), c);
	}

	/** YES: out onto the water ahead, on its back. */
	public static void start(ServerPlayer p, boolean yes) {
		if (!yes || on(p)) return;
		Mon m = surfer(p);
		if (m == null) return;
		ServerLevel lv = (ServerLevel) p.level();
		double r = Math.toRadians(p.getYRot());
		BlockPos water = null;
		for (double d = 0.8; d <= 2.2 && water == null; d += 0.35) {
			for (int dy = 0; dy >= -2; dy--) {
				BlockPos b = BlockPos.containing(p.getX() - Math.sin(r) * d, p.getY() + 0.1 + dy, p.getZ() + Math.cos(r) * d);
				if (lv.getFluidState(b).is(FluidTags.WATER) && lv.getFluidState(b).isSource() && !lv.getFluidState(b.above()).is(FluidTags.WATER)
						&& lv.getBlockState(b.above()).getCollisionShape(lv, b.above()).isEmpty()) {
					water = b;
					break;
				}
			}
		}
		if (water == null) return;
		ON.put(p.getUUID(), m.species.id());
		// Up onto a bank a block over the water (as a swimmer climbs out): a taller step while on its back.
		step(p, 1.25);
		Game.dialogue(p, null, "talk", List.of(m.name().toUpperCase() + " used SURF!"));
		send(p, true, m.species.id());
		p.teleportTo(lv, water.getX() + 0.5, water.getY() + 0.9, water.getZ() + 0.5, Set.of(), p.getYRot(), p.getXRot(), true);
		p.setDeltaMovement(Vec3.ZERO);
	}

	/** Each tick, for a surfer: the POKéMON under you; off its back once you're standing on land. */
	public static void tick(ServerPlayer p) {
		ServerLevel lv = (ServerLevel) p.level();
		UUID id = p.getUUID();
		String species = ON.get(id);
		if (species == null) return;
		BlockPos under = BlockPos.containing(p.getX(), p.getY() - 0.05, p.getZ());
		boolean onWater = lv.getFluidState(under).is(FluidTags.WATER) || lv.getFluidState(under.above()).is(FluidTags.WATER);
		if (p.onGround() && !onWater || Interiors.in(p) && !Interiors.swims(p) && !onWater) {
			stop(p);
			return;
		}
		Integer mid = MON.get(id);
		WorldMon w = mid != null && lv.getEntity(mid) instanceof WorldMon wm && wm.isAlive() ? wm : null;
		if (w == null) {
			w = Game.placeMon(lv, species, "surf:" + id, p.getX(), p.getY() - 0.9, p.getZ(), p.getYRot());
			if (w == null) return;
			w.setNoGravity(true);
			w.setNoAi(true);
			MON.put(id, w.getId());
		}
		w.setPos(p.getX(), p.getY() - 0.9, p.getZ());
		w.setYRot(p.getYRot());
		w.setYHeadRot(p.getYRot());
		w.yBodyRot = p.getYRot();
		w.setFacing(p.getYRot());
	}

	/** Off its back (on land, through a door, flying off). */
	public static void stop(ServerPlayer p) {
		UUID id = p.getUUID();
		if (ON.remove(id) == null) return;
		step(p, 0.6);
		Integer mid = MON.remove(id);
		if (mid != null && p.level().getEntity(mid) instanceof WorldMon w) w.discard();
		send(p, false, "");
	}

	private static void step(ServerPlayer p, double h) {
		var a = p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.STEP_HEIGHT);
		if (a != null) a.setBaseValue(h);
	}

	private static void send(ServerPlayer p, boolean on, String species) {
		CompoundTag t = new CompoundTag();
		t.putBoolean("on", on);
		t.putString("species", species);
		Net.send(p, "surf", t);
	}
}
